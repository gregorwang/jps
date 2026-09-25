#!/usr/bin/env python3
"""Anime Japanese Lab — Video Pipeline."""

from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from ass_parser import parse_ass_to_index, write_subtitle_index
from clipper import EncodeOptions, generate_clips, probe_video, require_ffmpeg
from matcher import build_manifest_rows, load_learning_sentences

MANIFEST_FIELDS = [
    "sentence_id", "episode_id", "sort_order", "ja_text", "zh_text",
    "subtitle_start_ms", "subtitle_end_ms", "clip_start_ms", "clip_end_ms",
    "clip_duration_ms", "clip_path", "match_status", "match_confidence", "match_reason",
]


def write_manifest_csv(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=MANIFEST_FIELDS, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def write_manifest_json(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")


def run_pipeline(args: argparse.Namespace) -> int:
    episode_id = args.episode_id
    video_path = Path(args.video)
    subtitle_path = Path(args.subtitle)
    learning_path = Path(args.learning_sentences)
    out_dir = Path(args.out_dir)
    dry_run = args.dry_run
    burn_subtitles = not args.no_burn_subtitles

    for label, p in [("subtitle", subtitle_path), ("learning sentences", learning_path)]:
        if not p.exists():
            print(f"Error: {label} not found: {p}")
            return 1

    if not dry_run and not video_path.exists():
        print(f"Error: video not found: {video_path}")
        return 1

    video_duration_ms = None
    if not dry_run:
        try:
            require_ffmpeg()
        except RuntimeError as exc:
            print(exc)
            return 1
        probe = probe_video(video_path)
        if not probe.ok:
            print(f"Error: ffprobe failed: {probe.error}")
            return 1
        video_duration_ms = probe.duration_ms
        print(f"Video duration: {video_duration_ms} ms ({video_path.name})")

    print(f"Parsing ASS: {subtitle_path}")
    subtitles = parse_ass_to_index(episode_id, subtitle_path)
    write_subtitle_index(out_dir / "subtitle_index.csv", subtitles)
    print(f"  subtitle_index.csv: {len(subtitles)} rows")

    sentences = load_learning_sentences(learning_path)
    print(f"Learning sentences: {len(sentences)}")

    manifest = build_manifest_rows(
        episode_id, sentences, subtitles,
        args.pre_roll_ms, args.post_roll_ms, video_duration_ms,
    )

    matched = sum(1 for r in manifest if r["match_status"] == "matched")
    unmatched = sum(1 for r in manifest if r["match_status"] == "unmatched")
    skipped = sum(1 for r in manifest if r["match_status"] == "skipped")
    clips_generated = clips_failed = 0

    if dry_run:
        print("Dry-run: skipping ffmpeg clip extraction")
    else:
        encode = EncodeOptions(
            preset=args.encode_preset,
            crf=args.crf,
            scale_height=0 if args.full_res else args.scale_height,
            video_codec=args.video_codec,
            workers=args.workers,
        )
        mode = "with hardsub" if burn_subtitles else "no burn"
        print(f"Extracting clips from: {video_path} ({mode})")
        print(f"  encode: {encode.summary()}")
        clips_generated, clips_failed, manifest = generate_clips(
            video_path, out_dir, manifest, burn_subtitles=burn_subtitles, encode=encode,
        )

    write_manifest_csv(out_dir / "sentence_clip_manifest.csv", manifest)
    write_manifest_json(out_dir / "sentence_clip_manifest.json", manifest)

    print("")
    print(f"Episode: {episode_id}")
    print(f"Subtitle rows: {len(subtitles)}")
    print(f"Learning sentences: {len(sentences)}")
    print(f"Matched: {matched}")
    print(f"Unmatched: {unmatched}")
    print(f"Skipped (low confidence): {skipped}")
    print(f"Clips generated: {clips_generated}")
    print(f"Clips failed: {clips_failed}")
    print(f"Hardsub burn: {'yes' if burn_subtitles and not dry_run else 'no'}")
    print(f"Output: {out_dir.resolve()}")
    return 0


def main() -> None:
    parser = argparse.ArgumentParser(description="Anime Japanese Lab video pipeline")
    sub = parser.add_subparsers(dest="command", required=True)
    run_p = sub.add_parser("run")
    run_p.add_argument("--episode-id", required=True)
    run_p.add_argument("--video", required=True)
    run_p.add_argument("--subtitle", required=True)
    run_p.add_argument("--learning-sentences", default="output/learning/learning_sentences.csv")
    run_p.add_argument("--out-dir", required=True)
    run_p.add_argument("--pre-roll-ms", type=int, default=300)
    run_p.add_argument("--post-roll-ms", type=int, default=300)
    run_p.add_argument("--dry-run", action="store_true")
    run_p.add_argument(
        "--no-burn-subtitles",
        action="store_true",
        help="Do not burn JP/ZH subtitles into clips",
    )
    run_p.add_argument(
        "--encode-preset",
        default="veryfast",
        help="libx264 preset or h264_nvenc preset (default: veryfast)",
    )
    run_p.add_argument("--crf", type=int, default=23, help="Quality (default: 23)")
    run_p.add_argument(
        "--scale-height",
        type=int,
        default=1080,
        help="Output height in px; 0 = keep source resolution (default: 1080)",
    )
    run_p.add_argument(
        "--full-res",
        action="store_true",
        help="Keep source resolution (same as --scale-height 0)",
    )
    run_p.add_argument(
        "--video-codec",
        choices=["libx264", "h264_nvenc"],
        default="libx264",
        help="Video encoder (default: libx264)",
    )
    run_p.add_argument(
        "--workers",
        type=int,
        default=3,
        help="Parallel ffmpeg jobs (default: 3)",
    )
    run_p.set_defaults(func=run_pipeline)
    args = parser.parse_args()
    sys.exit(args.func(args))


if __name__ == "__main__":
    main()
