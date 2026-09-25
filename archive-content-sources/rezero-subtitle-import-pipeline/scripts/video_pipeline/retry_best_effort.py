#!/usr/bin/env python3
"""Best-effort re-cut for failed sentences + export long-clip review list."""

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


from batch_kamigami_shadowing import find_ass, timing_source_label


def load_sentences(path: Path) -> dict[str, list[dict]]:
    raw = json.loads(path.read_text(encoding="utf-8-sig"))
    grouped: dict[str, list[dict]] = defaultdict(list)
    for item in raw:
        grouped[item["episode_id"]].append(item)
    for ep_id in grouped:
        grouped[ep_id].sort(key=lambda x: (x["sort_order"], x["id"]))
    return grouped


def load_retry_ids(path: Path) -> list[str]:
    data = json.loads(path.read_text(encoding="utf-8-sig"))
    items = data["items"] if isinstance(data, dict) else data
    return [item["sentence_id"] for item in items]


def export_long_clips(out_root: Path, min_ms: int) -> Path:
    long_items: list[dict] = []
    for ep_dir in sorted(out_root.glob("s01e*")):
        manifest_path = ep_dir / "audio_manifest.json"
        if not manifest_path.exists():
            continue
        for row in json.loads(manifest_path.read_text(encoding="utf-8-sig")):
            if row.get("status") != "ok":
                continue
            duration = int(row.get("duration_ms") or 0)
            if duration < min_ms:
                continue
            mp3 = ep_dir / row["file_name"]
            long_items.append(
                {
                    "sentence_id": row["sentence_id"],
                    "episode_id": row["episode_id"],
                    "sort_order": row.get("sort_order"),
                    "ja_text": row.get("ja_text"),
                    "matched_ja_text": row.get("matched_ja_text", ""),
                    "duration_ms": duration,
                    "duration_sec": round(duration / 1000, 2),
                    "ass_time": row.get("ass_time", ""),
                    "match_reason": row.get("match_reason", ""),
                    "file_name": row["file_name"],
                    "audio_path": str(mp3.resolve()) if mp3.exists() else "",
                    "suggested_trim": "note trim_start_sec e.g. 5 to drop buildup",
                    "trim_start_sec": "",
                    "review_status": "",
                    "review_notes": "",
                }
            )
    long_items.sort(key=lambda x: (x.get("episode_id") or "", x.get("sort_order") or 0))
    out_path = out_root / "review_long_clips.json"
    out_path.write_text(
        json.dumps(
            {
                "description": "Long clips for manual listen — fill trim_start_sec / review_notes",
                "min_duration_ms": min_ms,
                "total": len(long_items),
                "items": long_items,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    return out_path


def ms_to_ts(ms: int) -> str:
    s = ms / 1000
    m, s = divmod(s, 60)
    h, m = divmod(m, 60)
    return f"{int(h):02d}:{int(m):02d}:{s:05.2f}"


def run(args: argparse.Namespace) -> int:
    out_root = Path(args.out_dir)
    if args.export_long_only:
        path = export_long_clips(out_root, args.long_min_ms)
        print(f"Exported {path} ({json.loads(path.read_text(encoding='utf-8'))['total']} items)")
        return 0

    retry_ids = set(load_retry_ids(Path(args.retry_file)))
    grouped = load_sentences(Path(args.sentences))
    id_to_item = {
        item["id"]: item
        for items in grouped.values()
        for item in items
        if item["id"] in retry_ids
    }
    ffmpeg, _ = require_ffmpeg()
    results: list[dict] = []
    ok_n = fail_n = 0
    by_ep: dict[str, list[str]] = {}
    for sid in retry_ids:
        item = id_to_item.get(sid)
        if item:
            by_ep.setdefault(item["episode_id"], []).append(sid)

    for episode_id in sorted(by_ep.keys()):
        ep = episode_num(episode_id)
        if ep < args.from_ep or ep > args.to_ep:
            continue
        video = find_jpv_video(Path(args.jpv_dir), ep)
        ass_path = find_ass(Path(args.ass_dir), ep, args.ass_source)
        if not video or not ass_path:
            print(f"[SKIP] {episode_id}")
            continue
        out_dir = out_root / episode_slug(episode_id)
        ass_rows = parse_ass_to_index(episode_id, ass_path)
        probe = probe_video(video)
        if not probe.ok:
            print(f"[SKIP] {episode_id}: {probe.error}")
            continue
        manifest_path = out_dir / "audio_manifest.json"
        manifest_by_id = {}
        if manifest_path.exists():
            manifest_by_id = {r["sentence_id"]: r for r in json.loads(manifest_path.read_text(encoding="utf-8-sig"))}
        print(f"\n=== retry {episode_id}: {len(by_ep[episode_id])} ===")
        slug = episode_slug(episode_id)

        for sid in sorted(by_ep[episode_id], key=lambda s: id_to_item[s]["sort_order"]):
            item = id_to_item[sid]
            sent = LearningSentence(sid, int(item["sort_order"]), item["ja_text"], "", item.get("source_line_no"))
            match = match_sentence_chronological(sent, ass_rows, min_start_ms=0, best_effort=True)
            row = {
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
                "timing_source": f"{timing_source_label(args.ass_source)};best_effort",
                "file_name": f"{sid}.mp3",
                "storage_path": R2_PATH_TEMPLATE.format(episode_slug=slug, sentence_id=sid),
                "status": "pending",
                "error": "",
            }
            review = {**row, "suspicious_reasons": list(match.suspicious_flags), "review_status": "", "review_notes": ""}
            if not match.clip_eligible or match.subtitle_start_ms is None:
                row["status"] = "failed"
                row["error"] = match.match_reason
                fail_n += 1
                print(f"  FAIL {sid}")
            else:
                clip_start = max(0, match.subtitle_start_ms - PRE_ROLL_MS)
                clip_end = min(probe.duration_ms or match.subtitle_end_ms + POST_ROLL_MS, match.subtitle_end_ms + POST_ROLL_MS)
                duration_ms = clip_end - clip_start
                row.update({"clip_start_ms": clip_start, "clip_end_ms": clip_end, "duration_ms": duration_ms, "ass_time": f"{ms_to_ts(match.subtitle_start_ms)}-{ms_to_ts(match.subtitle_end_ms)}"})
                review.update({"clip_start_ms": clip_start, "clip_end_ms": clip_end, "duration_ms": duration_ms, "ass_time": row["ass_time"], "audio_path": str((out_dir / row["file_name"]).resolve())})
                if args.dry_run:
                    row["status"] = "dry_run"
                else:
                    ok, err = extract_audio(ffmpeg, video, out_dir / row["file_name"], clip_start, clip_end)
                    row["status"] = "ok" if ok else "failed"
                    row["error"] = err
                    if ok:
                        ok_n += 1
                        print(f"  OK {sid} {duration_ms}ms")
                    else:
                        fail_n += 1
            manifest_by_id[sid] = row
            results.append(review)

        out_dir.mkdir(parents=True, exist_ok=True)
        merged = sorted(manifest_by_id.values(), key=lambda r: (r.get("sort_order") or 0, r["sentence_id"]))
        manifest_path.write_text(json.dumps(merged, ensure_ascii=False, indent=2), encoding="utf-8")

    retry_out = Path(args.retry_out) if args.retry_out else out_root / "review_best_effort_retry.json"
    retry_out.write_text(json.dumps({"total": len(results), "ok": ok_n, "failed": fail_n, "items": results}, ensure_ascii=False, indent=2), encoding="utf-8")
    long_path = export_long_clips(out_root, args.long_min_ms)
    print(f"\nRetry list: {retry_out}")
    print(f"Long clips: {long_path}")
    print(f"Done ok={ok_n} failed={fail_n}")
    return 0 if fail_n == 0 else 1


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--retry-file", default=str(DEFAULT_OUT_ROOT / "review_unmatched_ineligible.json"))
    p.add_argument("--sentences", default=str(DEFAULT_SENTENCES))
    p.add_argument("--jpv-dir", default=str(DEFAULT_JPV_DIR))
    p.add_argument("--ass-dir", default=str(DEFAULT_ASS_DIR))
    p.add_argument("--out-dir", default=str(DEFAULT_OUT_ROOT))
    p.add_argument("--ass-source", choices=("kamigami", "moozzi2"), default="kamigami")
    p.add_argument("--from-ep", type=int, default=1)
    p.add_argument("--to-ep", type=int, default=99)
    p.add_argument("--long-min-ms", type=int, default=8000)
    p.add_argument("--retry-out", default="", help="Output JSON path (default: review_best_effort_retry.json)")
    p.add_argument("--dry-run", action="store_true")
    p.add_argument("--export-long-only", action="store_true")
    return run(p.parse_args())


if __name__ == "__main__":
    sys.exit(main())

