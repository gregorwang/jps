#!/usr/bin/env python3
"""Build re-zero S02 (global EP26-50) subtitle_lines.csv from Netflix ja SRT + BDRIP SC.ass zh."""

from __future__ import annotations

import argparse
import csv
import importlib.util
import re
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts" / "video_pipeline"))

from ass_parser import RE_ASS_TAGS, cues_overlap, detect_line_lang, parse_ass_cues, style_lang_hint  # noqa: E402

_merge_path = ROOT / "scripts" / "merge-rezero-zh-subtitles.py"
_spec = importlib.util.spec_from_file_location("merge_rezero_zh", _merge_path)
_merge = importlib.util.module_from_spec(_spec)
sys.modules[_spec.name] = _merge
assert _spec.loader is not None
_spec.loader.exec_module(_merge)

JaLine = _merge.JaLine
Match = _merge.Match
extract_zh_cues = _merge.extract_zh_cues
is_likely_dialogue = _merge.is_likely_dialogue
match_episode = _merge.match_episode
validate_match = _merge.validate_match

OUT_DIR = ROOT / "rezero_s02_ep26_ep50_subtitle_lines_import"
OUT_CSV = OUT_DIR / "subtitle_lines.csv"
REPORT_CSV = OUT_DIR / "zh_merge_review.csv"

S02_ASS_DIR = ROOT.parent / "rezero-subtitles-ass-backup-S00-S03" / "S02"
NETFLIX_DIRS = [
    Path.home() / "Downloads" / "Re_Zero kara Hajimeru Isekai Seikatsu 2nd Season Part 2",
    Path.home() / "Downloads" / "Re_Zero kara Hajimeru Isekai Seikatsu 2nd Season",
    Path.home() / "Downloads",
]

WORK_SLUG = "re-zero"
WORK_NAME = "Re:ゼロから始める異世界生活"
GLOBAL_EP_OFFSET = 25

RE_SRT_TAGS = re.compile(r"<[^>]+>")
RE_KANA = re.compile(r"[\u3040-\u309f\u30a0-\u30ff]")


def srt_time_to_ms(t: str) -> int:
    t = t.strip().replace(",", ".")
    parts = t.split(":")
    if len(parts) == 3:
        h, m, s = parts
        return int(round((int(h) * 3600 + int(m) * 60 + float(s)) * 1000))
    if len(parts) == 2:
        m, s = parts
        return int(round((int(m) * 60 + float(s)) * 1000))
    return 0


def ms_to_srt_time(ms: int) -> str:
    s = ms / 1000
    h = int(s // 3600)
    m = int((s % 3600) // 60)
    sec = s % 60
    whole = int(sec)
    frac = int(round((sec - whole) * 1000))
    return f"{h:02d}:{m:02d}:{whole:02d},{frac:03d}"


def find_netflix_srt(s2_ep: int) -> Path | None:
    for d in NETFLIX_DIRS:
        if not d.is_dir():
            continue
        hits = sorted(d.glob(f"*S02E{s2_ep:02d}*.ja*.srt"))
        if hits:
            return hits[0]
    return None


def find_sc_ass(s2_ep: int) -> Path | None:
    hits = sorted(S02_ASS_DIR.glob(f"*S02E{s2_ep:02d}*.SC.ass"))
    return hits[0] if hits else None


def parse_netflix_srt(path: Path, global_ep: int) -> list[JaLine]:
    content = path.read_text(encoding="utf-8-sig")
    blocks = re.split(r"\n\s*\n", content.strip(), flags=re.MULTILINE)
    rows: list[JaLine] = []
    line_no = 0
    for block in blocks:
        lines = [ln.strip() for ln in block.splitlines() if ln.strip()]
        if len(lines) < 2:
            continue
        time_idx = 1 if lines[0].isdigit() else 0
        if time_idx >= len(lines) or "-->" not in lines[time_idx]:
            continue
        start_raw, end_raw = [p.strip() for p in lines[time_idx].split("-->")]
        text = RE_SRT_TAGS.sub("", RE_ASS_TAGS.sub("", "\n".join(lines[time_idx + 1 :]))).strip()
        if not text:
            continue
        line_no += 1
        start_ms = srt_time_to_ms(start_raw)
        end_ms = srt_time_to_ms(end_raw)
        rows.append(
            JaLine(
                episode=global_ep,
                line_no=line_no,
                start_ms=start_ms,
                end_ms=end_ms,
                mid_ms=(start_ms + end_ms) // 2,
                ja_text=text.replace("\n", " ").strip(),
                usable=bool(RE_KANA.search(text)) or len(text) >= 4,
            )
        )
    return rows


def write_report(all_matches: list[Match], ja_by_key: dict[tuple[int, int], JaLine], unmatched: list[JaLine]) -> None:
    with REPORT_CSV.open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(["episode", "line_no", "confidence", "delta_ms", "ja_text", "zh_text", "status"])
        for m in sorted(all_matches, key=lambda x: (x.episode, x.line_no)):
            ja = ja_by_key[(m.episode, m.line_no)]
            w.writerow([m.episode, m.line_no, m.confidence, m.delta_ms, ja.ja_text, m.zh_text, "matched"])
        for ja in sorted(unmatched, key=lambda x: (x.episode, x.line_no)):
            status = "sfx_or_no_zh" if not is_likely_dialogue(ja) else "needs_review"
            w.writerow([ja.episode, ja.line_no, "", "", ja.ja_text, "", status])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--from-s2", type=int, default=1)
    parser.add_argument("--to-s2", type=int, default=25)
    args = parser.parse_args()

    all_ja: list[JaLine] = []
    zh_by_key: dict[tuple[int, int], str] = {}
    all_matches: list[Match] = []
    all_unmatched: list[JaLine] = []

    for s2_ep in range(args.from_s2, args.to_s2 + 1):
        global_ep = GLOBAL_EP_OFFSET + s2_ep
        srt_path = find_netflix_srt(s2_ep)
        ass_path = find_sc_ass(s2_ep)
        if not srt_path:
            print(f"S02E{s2_ep:02d} (EP{global_ep}): missing Netflix ja SRT", file=sys.stderr)
            continue
        if not ass_path:
            print(f"S02E{s2_ep:02d} (EP{global_ep}): missing SC.ass", file=sys.stderr)
            continue

        ep_ja = parse_netflix_srt(srt_path, global_ep)
        all_ja.extend(ep_ja)
        zh_cues = extract_zh_cues(ass_path)
        matches, unmatched = match_episode(ep_ja, zh_cues)
        ja_by_ep = {(j.episode, j.line_no): j for j in ep_ja}
        matches = [m for m in matches if validate_match(m, ja_by_ep[(m.episode, m.line_no)])]
        matched_nos = {m.line_no for m in matches}
        unmatched = [j for j in ep_ja if j.line_no not in matched_nos]
        for m in matches:
            zh_by_key[(m.episode, m.line_no)] = m.zh_text
        all_matches.extend(matches)
        all_unmatched.extend(unmatched)

        dialogue = sum(1 for j in ep_ja if is_likely_dialogue(j))
        matched_d = sum(1 for m in matches if is_likely_dialogue(ja_by_ep[(m.episode, m.line_no)]))
        print(
            f"EP{global_ep} (S02E{s2_ep:02d}): ja={len(ep_ja)} zh_matched={len(matches)} "
            f"dialogue_zh={matched_d}/{dialogue} cues={len(zh_cues)}"
        )

    ja_by_key = {(j.episode, j.line_no): j for j in all_ja}
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    write_report(all_matches, ja_by_key, all_unmatched)
    with OUT_CSV.open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(
            f,
            fieldnames=[
                "work_display_name",
                "work_slug",
                "episode",
                "line_no",
                "start_time",
                "end_time",
                "ja_text",
                "zh_text",
                "language",
                "source",
                "usable_for_analysis",
            ],
        )
        w.writeheader()
        for ja in sorted(all_ja, key=lambda x: (x.episode, x.line_no)):
            zh = zh_by_key.get((ja.episode, ja.line_no), "")
            w.writerow(
                {
                    "work_display_name": WORK_NAME,
                    "work_slug": WORK_SLUG,
                    "episode": ja.episode,
                    "line_no": ja.line_no,
                    "start_time": ms_to_srt_time(ja.start_ms),
                    "end_time": ms_to_srt_time(ja.end_ms),
                    "ja_text": ja.ja_text,
                    "zh_text": zh,
                    "language": "ja",
                    "source": "bilingual_subtitle" if zh else "netflix_ja",
                    "usable_for_analysis": "true" if ja.usable else "false",
                }
            )

    with_zh = sum(1 for k in ja_by_key if zh_by_key.get(k))
    print(f"Wrote {OUT_CSV} ({len(all_ja)} lines, {with_zh} with zh)")
    print(f"Report: {REPORT_CSV}")
    needs = [u for u in all_unmatched if is_likely_dialogue(u)]
    print(f"Dialogue still without zh: {len(needs)}")


if __name__ == "__main__":
    main()
