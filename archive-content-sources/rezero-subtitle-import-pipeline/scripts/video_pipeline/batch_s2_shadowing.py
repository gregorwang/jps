#!/usr/bin/env python3
"""Batch shadowing audio for Re:Zero S2 — BDRIP video + BDRIP TC timing axis."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parents[1]
sys.path.insert(0, str(SCRIPT_DIR))

from ass_parser import ass_time_to_ms, read_text_auto
from clipper import ms_to_seconds, probe_video, require_ffmpeg
from matcher import normalize_for_match

PRE_ROLL_MS = 150
POST_ROLL_MS = 200
R2_PATH_TEMPLATE = "shadowing-audio/re-zero/{episode_slug}/{sentence_id}.mp3"

DEFAULT_VIDEO_DIR = ROOT / "Season 2"
DEFAULT_TC_DIR = ROOT.parent / "rezero-subtitles-ass-backup-S00-S03" / "S02"
DEFAULT_SENTENCES = ROOT / "output" / "audio" / "re-zero" / "sentences_s02e26-e50.json"
DEFAULT_OUT_ROOT = ROOT / "output" / "audio" / "re-zero"

DEFAULT_NETFLIX_DIRS = [
    Path.home() / "Downloads" / "Re_Zero kara Hajimeru Isekai Seikatsu 2nd Season Part 2",
    Path.home() / "Downloads" / "Re_Zero kara Hajimeru Isekai Seikatsu 2nd Season",
    Path.home() / "Downloads",
]

TAIL_PAD_MS: dict[str, int] = {}

RE_SRT_TAGS = re.compile(r"<[^>]+>")
RE_ASS_DIALOGUE = re.compile(r"^Dialogue:\s*\d+,([^,]+),([^,]+),", re.I)


@dataclass
class Cue:
    start_ms: int
    end_ms: int
    text: str


def extract_audio(ffmpeg: str, video: Path, out: Path, start_ms: int, end_ms: int) -> tuple[bool, str]:
    out.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        ffmpeg,
        "-y",
        "-ss",
        ms_to_seconds(start_ms),
        "-to",
        ms_to_seconds(end_ms),
        "-i",
        str(video.resolve()),
        "-vn",
        "-acodec",
        "libmp3lame",
        "-q:a",
        "2",
        str(out.resolve()),
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True, check=False)
    if proc.returncode != 0:
        return False, (proc.stderr or proc.stdout or "ffmpeg failed")[-400:]
    if not out.exists() or out.stat().st_size == 0:
        return False, "empty output"
    return True, ""


def episode_slug(episode_id: str) -> str:
    m = re.search(r"s02e(\d+)", episode_id, re.I)
    if not m:
        raise ValueError(f"unexpected episode_id: {episode_id}")
    return f"s02e{m.group(1)}"


def episode_num(episode_id: str) -> int:
    return int(re.search(r"s02e(\d+)", episode_id, re.I).group(1))


def ms_to_ts(ms: int) -> str:
    s = ms / 1000
    m, s = divmod(s, 60)
    h, m = divmod(m, 60)
    return f"{int(h):02d}:{int(m):02d}:{s:05.2f}"


def srt_time_to_ms(raw: str) -> int:
    raw = raw.strip().replace(",", ".")
    hh, mm, rest = raw.split(":")
    ss, _, frac = rest.partition(".")
    ms = int(hh) * 3600000 + int(mm) * 60000 + int(ss) * 1000
    if frac:
        ms += int(frac.ljust(3, "0")[:3])
    return ms


def parse_srt(path: Path) -> dict[int, Cue]:
    content = read_text_auto(path)
    blocks = re.split(r"\n\s*\n", content.strip(), flags=re.MULTILINE)
    cues: dict[int, Cue] = {}
    line_no = 0
    for block in blocks:
        rows = [row.strip() for row in block.splitlines() if row.strip()]
        if len(rows) < 2:
            continue
        time_idx = 1 if rows[0].isdigit() else 0
        if time_idx >= len(rows) or "-->" not in rows[time_idx]:
            continue
        start_raw, end_raw = [part.strip() for part in rows[time_idx].split("-->")]
        text = RE_SRT_TAGS.sub("", "\n".join(rows[time_idx + 1 :])).strip()
        if not text:
            continue
        line_no += 1
        cues[line_no] = Cue(srt_time_to_ms(start_raw), srt_time_to_ms(end_raw), text)
    return cues


def parse_bdrip_tc_dialogues(path: Path) -> list[Cue]:
    cues: list[Cue] = []
    for line in read_text_auto(path).splitlines():
        if not line.startswith("Dialogue:"):
            continue
        m = RE_ASS_DIALOGUE.match(line)
        if not m:
            continue
        parts = line.split(",", 9)
        if len(parts) < 10:
            continue
        text = parts[9].strip()
        cues.append(Cue(ass_time_to_ms(m.group(1)), ass_time_to_ms(m.group(2)), text))
    return cues


def load_sentences(path: Path) -> dict[str, list[dict]]:
    raw = json.loads(path.read_text(encoding="utf-8-sig"))
    grouped: dict[str, list[dict]] = defaultdict(list)
    for item in raw:
        grouped[item["episode_id"]].append(item)
    for ep_id in grouped:
        grouped[ep_id].sort(key=lambda x: (x["sort_order"], x["id"]))
    return grouped


def find_s2_video(video_dir: Path, s2_ep: int) -> Path | None:
    for pat in (f"*S02E{s2_ep:02d}*.mkv", f"*S02E{s2_ep}*.mkv"):
        matches = sorted(video_dir.glob(pat))
        if matches:
            return matches[0]
    return None


def find_bdrip_tc(tc_dir: Path, s2_ep: int) -> Path | None:
    matches = sorted(tc_dir.glob(f"*S02E{s2_ep:02d}*.TC.ass"))
    return matches[0] if matches else None


def find_netflix_ja_srt(netflix_dirs: list[Path], s2_ep: int) -> Path | None:
    """S02E zero-padded only — *S02E1* would wrongly match S02E14."""
    for netflix_dir in netflix_dirs:
        if not netflix_dir.is_dir():
            continue
        matches = sorted(netflix_dir.glob(f"*S02E{s2_ep:02d}*.ja*.srt"))
        if matches:
            return matches[0]
    return None


def resolve_bdrip_timing(
    netflix: dict[int, Cue],
    tc_cues: list[Cue],
    start_line: int,
    end_line: int,
) -> tuple[int, int, str, str]:
    if start_line not in netflix or end_line not in netflix:
        raise ValueError(f"netflix lines missing: {start_line}-{end_line}")

    n_start = netflix[start_line].start_ms
    n_end = netflix[end_line].end_ms

    matched: list[Cue] = []
    for c in tc_cues:
        center = (c.start_ms + c.end_ms) // 2
        if center < n_start - 1200 or center > n_end + 400:
            continue
        overlap = min(c.end_ms, n_end) - max(c.start_ms, n_start)
        if overlap <= 200:
            continue
        matched.append(c)

    if matched:
        start_ms = min(c.start_ms for c in matched)
        end_ms = max(c.end_ms for c in matched)
        matched_text = " / ".join(c.text for c in matched)
        reason = f"bdrip_tc_overlap_netflix_L{start_line}-L{end_line}"
        return start_ms, end_ms, matched_text, reason

    return n_start, n_end, netflix[start_line].text, f"netflix_fallback_L{start_line}-L{end_line}"


def text_overlap_ok(ja_text: str, netflix_text: str) -> bool:
    a = normalize_for_match(ja_text)
    b = normalize_for_match(netflix_text)
    if not a or not b:
        return True
    if a in b or b in a:
        return True
    merged = normalize_for_match(netflix_text.replace("\n", ""))
    return a in merged or merged in a or len(set(a) & set(b)) / max(len(set(a)), 1) > 0.5


def process_episode_bdrip(
    episode_id: str,
    items: list[dict],
    video: Path,
    tc_path: Path,
    netflix_path: Path,
    out_dir: Path,
    ffmpeg: str,
    *,
    dry_run: bool = False,
) -> tuple[list[dict], list[dict]]:
    slug = episode_slug(episode_id)
    netflix = parse_srt(netflix_path)
    tc_cues = parse_bdrip_tc_dialogues(tc_path)
    probe = probe_video(video)
    if not probe.ok:
        raise RuntimeError(f"{episode_id}: {probe.error}")

    manifest: list[dict] = []
    suspicious: list[dict] = []

    for item in items:
        sid = item["id"]
        source_line_no = item.get("source_line_no")
        start_line = source_line_no
        end_line = source_line_no
        if not start_line or not end_line:
            manifest.append({"sentence_id": sid, "status": "failed", "error": "missing_line_range"})
            continue

        row: dict = {
            "sentence_id": sid,
            "episode_id": episode_id,
            "sort_order": item["sort_order"],
            "ja_text": item["ja_text"],
            "source_line_no": source_line_no,
            "start_line": start_line,
            "end_line": end_line,
            "timing_source": "bdrip_tc_ass",
            "file_name": f"{sid}.mp3",
            "storage_path": R2_PATH_TEMPLATE.format(episode_slug=slug, sentence_id=sid),
            "status": "pending",
            "error": "",
        }
        if item.get("db_id"):
            row["db_id"] = item["db_id"]

        try:
            start_ms, end_ms, matched_text, reason = resolve_bdrip_timing(
                netflix, tc_cues, int(start_line), int(end_line)
            )
        except ValueError as e:
            row.update({"status": "failed", "error": str(e), "match_status": "unmatched"})
            manifest.append(row)
            suspicious.append({**row, "suspicious_reasons": ["line_lookup_failed"]})
            continue

        n_end = netflix[int(end_line)].end_ms
        tail_pad = TAIL_PAD_MS.get(sid, 0)
        clip_start = max(0, start_ms - PRE_ROLL_MS)
        clip_end = min(
            probe.duration_ms or n_end + POST_ROLL_MS + tail_pad,
            max(end_ms, n_end) + tail_pad + POST_ROLL_MS,
        )

        netflix_text = "\n".join(netflix[i].text for i in range(int(start_line), int(end_line) + 1) if i in netflix)
        flags: list[str] = []
        if not text_overlap_ok(item["ja_text"], netflix_text):
            flags.append("ja_text_netflix_mismatch")

        row.update(
            {
                "ass_start_ms": start_ms,
                "ass_end_ms": end_ms,
                "matched_ja_text": netflix_text,
                "matched_tc_text": matched_text,
                "match_status": "matched",
                "match_confidence": "high",
                "match_reason": reason + (f";tail_pad_ms={tail_pad}" if tail_pad else ""),
                "clip_start_ms": clip_start,
                "clip_end_ms": clip_end,
                "duration_ms": clip_end - clip_start,
                "ass_time": f"{ms_to_ts(start_ms)}-{ms_to_ts(end_ms)}",
            }
        )

        if flags:
            suspicious.append({**row, "suspicious_reasons": flags})

        out_file = out_dir / f"{sid}.mp3"
        if dry_run:
            row["status"] = "dry_run"
        else:
            ok, err = extract_audio(ffmpeg, video, out_file, clip_start, clip_end)
            row["status"] = "ok" if ok else "failed"
            row["error"] = err
            if not ok:
                suspicious.append({**row, "suspicious_reasons": flags + ["extract_failed"]})

        manifest.append(row)

    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "audio_manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    (out_dir / "suspicious.json").write_text(json.dumps(suspicious, ensure_ascii=False, indent=2), encoding="utf-8")
    return manifest, suspicious


def run(args: argparse.Namespace) -> int:
    video_dir = Path(args.video_dir)
    tc_dir = Path(args.tc_dir)
    netflix_dirs = [Path(p) for p in args.netflix_dir.split(";")] if args.netflix_dir else DEFAULT_NETFLIX_DIRS
    out_root = Path(args.out_dir)
    sentences_path = Path(args.sentences)
    ffmpeg, _ = require_ffmpeg()

    if not sentences_path.exists():
        raise SystemExit(f"missing sentences JSON: {sentences_path}\nRun export_s02_shadowing_sentences.py first")

    grouped = load_sentences(sentences_path)
    ep_filter: set[int] | None = None
    if args.episodes:
        ep_filter = {int(x) for x in args.episodes.split(",")}

    all_suspicious: list[dict] = []
    totals: dict[str, int] = {}

    for episode_id in sorted(grouped, key=episode_num):
        ep = episode_num(episode_id)
        if ep_filter and ep not in ep_filter:
            continue
        if args.from_ep and ep < args.from_ep:
            continue
        if args.to_ep and ep > args.to_ep:
            continue

        video = find_s2_video(video_dir, ep)
        tc_path = find_bdrip_tc(tc_dir, ep)
        netflix_path = find_netflix_ja_srt(netflix_dirs, ep)
        if not video or not tc_path or not netflix_path:
            print(f"[SKIP] {episode_id}: video={bool(video)} tc={bool(tc_path)} netflix={bool(netflix_path)}")
            continue

        out_dir = out_root / episode_slug(episode_id)
        items = grouped[episode_id]
        print(f"\n=== {episode_id} ({len(items)} sentences) ===")
        print(f"  video:   {video.name}")
        print(f"  tc:      {tc_path.name}")
        print(f"  netflix: {netflix_path.name}")

        manifest, suspicious = process_episode_bdrip(
            episode_id, items, video, tc_path, netflix_path, out_dir, ffmpeg, dry_run=args.dry_run
        )
        for row in manifest:
            totals[row.get("status", "unknown")] = totals.get(row.get("status", "unknown"), 0) + 1
        for row in suspicious:
            row["episode_id"] = episode_id
        all_suspicious.extend(suspicious)
        ok_n = sum(1 for r in manifest if r.get("status") == "ok")
        print(f"  -> ok={ok_n}/{len(manifest)} suspicious={len(suspicious)} -> {out_dir}")

    summary = {"totals": totals, "suspicious_count": len(all_suspicious)}
    out_root.mkdir(parents=True, exist_ok=True)
    (out_root / "suspicious_s02_bdrip.json").write_text(
        json.dumps({"summary": summary, "items": all_suspicious}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"\nDone. totals={totals}")
    return 0 if totals.get("failed", 0) == 0 else 1


def main() -> int:
    p = argparse.ArgumentParser(description="Re:Zero S2 shadowing — BDRIP video + BDRIP TC timing")
    p.add_argument("--sentences", default=str(DEFAULT_SENTENCES), help="sentences_s02e26-e50.json from DB export")
    p.add_argument("--video-dir", default=str(DEFAULT_VIDEO_DIR))
    p.add_argument("--tc-dir", default=str(DEFAULT_TC_DIR))
    p.add_argument(
        "--netflix-dir",
        help="Semicolon-separated Netflix SRT search dirs (default: Downloads S2 folders)",
    )
    p.add_argument("--out-dir", default=str(DEFAULT_OUT_ROOT))
    p.add_argument("--from-ep", type=int, default=1, help="S2 animation episode min")
    p.add_argument("--to-ep", type=int, default=25, help="S2 animation episode max")
    p.add_argument("--episodes", help="Comma-separated S2 episode numbers, e.g. 15")
    p.add_argument("--dry-run", action="store_true")
    return run(p.parse_args())


if __name__ == "__main__":
    sys.exit(main())
