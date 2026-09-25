#!/usr/bin/env python3
"""Batch shadowing audio extraction using ASS timing on jpv BDRIP video (Kamigami E01-11, Moozzi2 BD E12-25)."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parents[1]
sys.path.insert(0, str(SCRIPT_DIR))

from ass_parser import parse_ass_to_index
from clipper import ms_to_seconds, probe_video, require_ffmpeg
from matcher import LearningSentence, match_sentence_chronological

PRE_ROLL_MS = 150
POST_ROLL_MS = 200
R2_PATH_TEMPLATE = "shadowing-audio/re-zero/{episode_slug}/{sentence_id}.mp3"

DEFAULT_JPV_DIR = ROOT / "jpv"
DEFAULT_ASS_DIR = ROOT / "Re_Zero kara Hajimeru Isekai Seikatsu (1)"
DEFAULT_SENTENCES = ROOT / "output" / "audio" / "re-zero" / "sentences_s01e01-e11.json"
DEFAULT_SENTENCES_E12 = ROOT / "output" / "audio" / "re-zero" / "sentences_s01e12-e25.json"
DEFAULT_OUT_ROOT = ROOT / "output" / "audio" / "re-zero"


def extract_audio(ffmpeg: str, video: Path, out: Path, start_ms: int, end_ms: int) -> tuple[bool, str]:
    out.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        ffmpeg, "-y",
        "-ss", ms_to_seconds(start_ms),
        "-to", ms_to_seconds(end_ms),
        "-i", str(video.resolve()),
        "-vn", "-acodec", "libmp3lame", "-q:a", "2",
        str(out.resolve()),
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True, check=False)
    if proc.returncode != 0:
        return False, (proc.stderr or proc.stdout or "ffmpeg failed")[-400:]
    if not out.exists() or out.stat().st_size == 0:
        return False, "empty output"
    return True, ""


def episode_slug(episode_id: str) -> str:
    m = re.search(r"s01e(\d+)", episode_id, re.I)
    if not m:
        raise ValueError(f"unexpected episode_id: {episode_id}")
    return f"s01e{m.group(1)}"


def episode_num(episode_id: str) -> int:
    m = re.search(r"s01e(\d+)", episode_id, re.I)
    if not m:
        raise ValueError(f"unexpected episode_id: {episode_id}")
    return int(m.group(1))


def find_jpv_video(jpv_dir: Path, ep: int) -> Path | None:
    matches = sorted(jpv_dir.glob(f"*.S01E{ep:02d}*.mkv"))
    return matches[0] if matches else None


def find_kamigami_ass(ass_dir: Path, ep: int) -> Path | None:
    matches: list[Path] = []
    for path in ass_dir.glob("*.ass"):
        name = path.name
        if "Kamigami" not in name or "[JPN]" not in name:
            continue
        if ep == 1:
            if " - 01v3 " in name:
                matches.append(path)
            continue
        if f" - {ep:02d} " in name:
            matches.append(path)
    return sorted(matches)[0] if matches else None


def find_moozzi2_ass(ass_dir: Path, ep: int) -> Path | None:
    matches: list[Path] = []
    for path in ass_dir.glob("*.jp.ass"):
        name = path.name
        if "Moozzi2" not in name:
            continue
        if ep == 25:
            if " - 25 " in name or "25 END" in name:
                matches.append(path)
            continue
        if f" - {ep} " in name:
            matches.append(path)
    return sorted(matches)[0] if matches else None


def find_ass(ass_dir: Path, ep: int, source: str) -> Path | None:
    if source == "moozzi2":
        return find_moozzi2_ass(ass_dir, ep)
    return find_kamigami_ass(ass_dir, ep)


def timing_source_label(source: str) -> str:
    return "moozzi2_bd_ass" if source == "moozzi2" else "kamigami_ass"


def load_sentences(path: Path) -> dict[str, list[dict]]:
    raw = json.loads(path.read_text(encoding="utf-8-sig"))
    grouped: dict[str, list[dict]] = defaultdict(list)
    for item in raw:
        grouped[item["episode_id"]].append(item)
    for ep_id in grouped:
        grouped[ep_id].sort(key=lambda x: (x["sort_order"], x["id"]))
    return grouped


def ms_to_ts(ms: int) -> str:
    s = ms / 1000
    m, s = divmod(s, 60)
    h, m = divmod(m, 60)
    return f"{int(h):02d}:{int(m):02d}:{s:05.2f}"


def process_episode(
    episode_id: str,
    items: list[dict],
    video: Path,
    ass_path: Path,
    out_dir: Path,
    ffmpeg: str,
    *,
    ass_source: str = "kamigami",
    dry_run: bool = False,
    skip_existing: bool = False,
) -> tuple[list[dict], list[dict]]:
    slug = episode_slug(episode_id)
    ass_rows = parse_ass_to_index(episode_id, ass_path)
    probe = probe_video(video)
    if not probe.ok:
        raise RuntimeError(f"{episode_id}: {probe.error}")

    manifest: list[dict] = []
    suspicious: list[dict] = []
    min_start_ms = 0

    for item in items:
        sid = item["id"]
        sent = LearningSentence(
            sentence_id=sid,
            sort_order=int(item["sort_order"]),
            ja_text=item["ja_text"],
            zh_text="",
            source_line_no=int(item["source_line_no"]) if item.get("source_line_no") else None,
        )
        match = match_sentence_chronological(sent, ass_rows, min_start_ms=min_start_ms)
        row: dict = {
            "sentence_id": sid,
            "episode_id": episode_id,
            "sort_order": sent.sort_order,
            "ja_text": sent.ja_text,
            "source_line_no": sent.source_line_no,
            "ass_subtitle_index": match.subtitle_index,
            "ass_start_ms": match.subtitle_start_ms,
            "ass_end_ms": match.subtitle_end_ms,
            "matched_ja_text": match.matched_ja_text,
            "match_status": match.match_status,
            "match_confidence": match.match_confidence,
            "match_reason": match.match_reason,
            "candidate_count": match.candidate_count,
            "timing_source": timing_source_label(ass_source),
            "file_name": f"{sid}.mp3",
            "storage_path": R2_PATH_TEMPLATE.format(episode_slug=slug, sentence_id=sid),
            "status": "pending",
            "error": "",
        }

        if not match.clip_eligible or match.subtitle_start_ms is None or match.subtitle_end_ms is None:
            row["status"] = "failed"
            row["error"] = match.match_reason
            manifest.append(row)
            suspicious.append(
                {
                    **row,
                    "suspicious_reasons": list(match.suspicious_flags) + ["unmatched_or_ineligible"],
                    "ass_time": "",
                }
            )
            continue

        clip_start = max(0, match.subtitle_start_ms - PRE_ROLL_MS)
        clip_end = min(probe.duration_ms or match.subtitle_end_ms + POST_ROLL_MS, match.subtitle_end_ms + POST_ROLL_MS)
        duration_ms = clip_end - clip_start
        row.update(
            {
                "clip_start_ms": clip_start,
                "clip_end_ms": clip_end,
                "duration_ms": duration_ms,
                "ass_time": f"{ms_to_ts(match.subtitle_start_ms)}-{ms_to_ts(match.subtitle_end_ms)}",
            }
        )

        flags = list(match.suspicious_flags)
        if match.subtitle_start_ms < min_start_ms:
            flags.append("time_regression")

        out_file = out_dir / f"{sid}.mp3"
        if flags:
            suspicious.append({**row, "suspicious_reasons": flags})

        if dry_run:
            row["status"] = "dry_run"
        elif skip_existing and out_file.exists() and out_file.stat().st_size > 0:
            row["status"] = "skipped_existing"
        else:
            ok, err = extract_audio(ffmpeg, video, out_file, clip_start, clip_end)
            row["status"] = "ok" if ok else "failed"
            row["error"] = err
            if not ok:
                suspicious.append({**row, "suspicious_reasons": flags + ["extract_failed"]})

        manifest.append(row)
        min_start_ms = max(min_start_ms, clip_end - POST_ROLL_MS)

    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "audio_manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    (out_dir / "suspicious.json").write_text(json.dumps(suspicious, ensure_ascii=False, indent=2), encoding="utf-8")
    return manifest, suspicious


def run(args: argparse.Namespace) -> int:
    grouped = load_sentences(Path(args.sentences))
    jpv_dir = Path(args.jpv_dir)
    ass_dir = Path(args.ass_dir)
    out_root = Path(args.out_dir)
    ffmpeg, _ = require_ffmpeg()

    ep_filter: set[str] | None = None
    if args.episodes:
        ep_filter = {f"re-zero-s01e{int(x):02d}" for x in args.episodes.split(",")}

    all_suspicious: list[dict] = []
    totals: dict[str, int] = {}

    for episode_id in sorted(grouped.keys()):
        if ep_filter and episode_id not in ep_filter:
            continue
        ep = episode_num(episode_id)
        if ep < args.from_ep or ep > args.to_ep:
            continue

        video = find_jpv_video(jpv_dir, ep)
        ass_path = find_ass(ass_dir, ep, args.ass_source)
        if not video or not ass_path:
            print(f"[SKIP] {episode_id}: missing video={bool(video)} ass={bool(ass_path)}")
            continue

        out_dir = out_root / episode_slug(episode_id)
        print(f"\n=== {episode_id} ({len(grouped[episode_id])} sentences) ===")
        print(f"  video: {video.name}")
        print(f"  ass:   {ass_path.name} [{args.ass_source}]")

        manifest, suspicious = process_episode(
            episode_id,
            grouped[episode_id],
            video,
            ass_path,
            out_dir,
            ffmpeg,
            ass_source=args.ass_source,
            dry_run=args.dry_run,
            skip_existing=args.skip_existing,
        )
        for row in manifest:
            totals[row["status"]] = totals.get(row["status"], 0) + 1
        for row in suspicious:
            row["episode_id"] = episode_id
        all_suspicious.extend(suspicious)
        ok_n = sum(1 for r in manifest if r["status"] == "ok")
        print(f"  -> ok={ok_n}/{len(manifest)} suspicious={len(suspicious)} -> {out_dir}")

    summary = {"totals": totals, "suspicious_count": len(all_suspicious)}
    out_root.mkdir(parents=True, exist_ok=True)
    (out_root / "suspicious_all.json").write_text(
        json.dumps({"summary": summary, "items": all_suspicious}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"\nDone. totals={totals}")
    print(f"Suspicious report: {out_root / 'suspicious_all.json'}")
    return 0 if totals.get("failed", 0) == 0 else 1


def main() -> int:
    p = argparse.ArgumentParser(description="Batch ASS shadowing audio for Re:Zero S1")
    p.add_argument("--sentences", default=str(DEFAULT_SENTENCES))
    p.add_argument("--jpv-dir", default=str(DEFAULT_JPV_DIR))
    p.add_argument("--ass-dir", default=str(DEFAULT_ASS_DIR))
    p.add_argument("--out-dir", default=str(DEFAULT_OUT_ROOT))
    p.add_argument(
        "--ass-source",
        choices=("kamigami", "moozzi2"),
        default="kamigami",
        help="kamigami for E01-11, moozzi2 BD .jp.ass for E12-25",
    )
    p.add_argument("--from-ep", type=int, default=1)
    p.add_argument("--to-ep", type=int, default=11)
    p.add_argument("--episodes", help="Comma-separated episode numbers, e.g. 1,2,3")
    p.add_argument("--dry-run", action="store_true")
    p.add_argument("--skip-existing", action="store_true")
    return run(p.parse_args())


if __name__ == "__main__":
    sys.exit(main())

