#!/usr/bin/env python3
"""Convert Japanese SRT subtitle files to Vectorize-ready chunks.jsonl."""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path

RE_SRT_TAGS = re.compile(r"\{[^}]*\}")
RE_KANA = re.compile(r"[\u3040-\u309f\u30a0-\u30ff]")
RE_CJK = re.compile(r"[\u4e00-\u9fff]")
RE_CHINESE_INDICATORS = re.compile(
    r"[的吗嘛吧呢]|动画|感谢|工作人员|轻音|就此|完结|特此|华盟|字幕|制作|翻译|校对|压制|片源|招募|[，；]"
)
RE_EPISODE = re.compile(r"S\d+E(\d+)", re.IGNORECASE)

STAFF_META_KEYWORDS = (
    "字幕", "字幕组", "制作", "工作人员", "感谢", "翻译", "校对",
    "时间轴", "压制", "片源", "招募",
)

CHUNK_SIZE = 30
CHUNK_STEP = 25
TARGET_CHARS = 1200
MAX_CHARS = 1800
MIN_CHARS = 280
MAX_GAP_MS = 8_000
OVERLAP_LINES = 6


@dataclass
class SubtitleLine:
    line_no: int
    start_ms: int
    end_ms: int
    text: str


def read_text_auto(path: Path) -> str:
    for enc in ("utf-8-sig", "utf-8", "utf-16", "cp932", "shift_jis"):
        try:
            return path.read_text(encoding=enc)
        except (UnicodeDecodeError, UnicodeError):
            continue
    return path.read_text(encoding="utf-8", errors="replace")


def srt_time_to_ms(raw: str) -> int:
    raw = raw.strip().replace(",", ".")
    hh, mm, rest = raw.split(":")
    ss, _, frac = rest.partition(".")
    ms = int(hh) * 3_600_000 + int(mm) * 60_000 + int(ss) * 1_000
    if frac:
        ms += int(frac.ljust(3, "0")[:3])
    return ms


def ms_to_hhmmss(ms: int) -> str:
    total = max(ms, 0) // 1000
    hh = total // 3600
    mm = (total % 3600) // 60
    ss = total % 60
    return f"{hh:02d}:{mm:02d}:{ss:02d}"


def clean_srt_text(raw: str) -> str:
    text = RE_SRT_TAGS.sub("", raw)
    text = re.sub(r"<[^>]+>", "", text)
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    text = text.replace("\\N", "\n")
    lines = [ln.strip() for ln in text.split("\n") if ln.strip()]
    return "\n".join(lines)


def is_staff_meta_line(text: str) -> bool:
    return any(kw in text for kw in STAFF_META_KEYWORDS)


def is_mainly_chinese_low_kana(text: str) -> bool:
    if not RE_CHINESE_INDICATORS.search(text):
        return False
    kana_count = len(RE_KANA.findall(text))
    cjk_count = len(RE_CJK.findall(text))
    if cjk_count < 4:
        return False
    if kana_count == 0:
        return True
    if cjk_count >= 6 and kana_count <= 2:
        return True
    total = cjk_count + kana_count
    return total > 0 and kana_count / total < 0.08


def is_usable_ja_line(text: str) -> bool:
    if not text.strip():
        return False
    if "♪" in text:
        return False
    if not RE_KANA.search(text):
        return False
    if is_staff_meta_line(text):
        return False
    if is_mainly_chinese_low_kana(text):
        return False
    if is_pure_caption_effect(text):
        return False
    return True


def is_pure_caption_effect(text: str) -> bool:
    stripped = re.sub(r"\s+", "", text)
    stripped = re.sub(r"^-+", "", stripped)
    if not stripped:
        return True
    without_parens = re.sub(r"[（(][^（）()]*[）)]", "", stripped)
    if without_parens and RE_KANA.search(without_parens):
        return False
    # Keep speaker-tagged dialogue like （スバル）んっ, but drop pure effects like （拍手）.
    return bool(re.fullmatch(r"[（(][^（）()]{1,20}[）)]", stripped))


def parse_srt(path: Path) -> list[SubtitleLine]:
    content = read_text_auto(path)
    blocks = re.split(r"\n\s*\n", content.strip(), flags=re.MULTILINE)
    lines: list[SubtitleLine] = []
    line_no = 0
    for block in blocks:
        rows = [row.strip() for row in block.splitlines() if row.strip()]
        if len(rows) < 2:
            continue
        time_idx = 1 if rows[0].isdigit() else 0
        if time_idx >= len(rows) or "-->" not in rows[time_idx]:
            continue
        start_raw, end_raw = [part.strip() for part in rows[time_idx].split("-->")]
        text = clean_srt_text("\n".join(rows[time_idx + 1 :]))
        if not text:
            continue
        line_no += 1
        lines.append(SubtitleLine(line_no=line_no, start_ms=srt_time_to_ms(start_raw), end_ms=srt_time_to_ms(end_raw), text=text))
    return dedupe_contiguous_captions(lines)


def normalize_for_dedupe(text: str) -> str:
    return re.sub(r"\s+", "", text)


def dedupe_contiguous_captions(lines: list[SubtitleLine]) -> list[SubtitleLine]:
    deduped: list[SubtitleLine] = []
    for line in lines:
        current_key = normalize_for_dedupe(line.text)
        previous = deduped[-1] if deduped else None
        if previous:
            previous_key = normalize_for_dedupe(previous.text)
            gap = line.start_ms - previous.end_ms
            if current_key == previous_key and -250 <= gap <= 1_500:
                deduped[-1] = SubtitleLine(
                    line_no=previous.line_no,
                    start_ms=previous.start_ms,
                    end_ms=max(previous.end_ms, line.end_ms),
                    text=previous.text,
                )
                continue
        deduped.append(line)
    return [
        SubtitleLine(line_no=index + 1, start_ms=line.start_ms, end_ms=line.end_ms, text=line.text)
        for index, line in enumerate(deduped)
    ]


def parse_episode(path: Path) -> int:
    match = RE_EPISODE.search(path.name)
    if not match:
        raise ValueError(f"Cannot parse episode number from {path.name}")
    return int(match.group(1))


def season_scope_for_global_episode(episode: int) -> tuple[int, int]:
    if 1 <= episode <= 25:
        return 1, episode
    if 26 <= episode <= 50:
        return 2, episode - 25
    if 51 <= episode <= 66:
        return 3, episode - 50
    return 0, episode


def chunk_windows(usable: list[SubtitleLine]) -> list[list[SubtitleLine]]:
    if not usable:
        return []

    windows: list[list[SubtitleLine]] = []
    current: list[SubtitleLine] = []
    current_chars = 0

    for line in usable:
        gap = line.start_ms - current[-1].end_ms if current else 0
        line_chars = len(line.text)
        should_close = bool(current) and (
            gap > MAX_GAP_MS
            or current_chars + line_chars > MAX_CHARS
            or (current_chars >= TARGET_CHARS and len(current) >= 12)
        )
        if should_close:
            windows.append(current)
            overlap = current[-OVERLAP_LINES:] if len(current) > OVERLAP_LINES else current[-2:]
            current = list(overlap)
            current_chars = sum(len(item.text) for item in current)
        current.append(line)
        current_chars += line_chars

    if current:
        if windows and sum(len(item.text) for item in current) < MIN_CHARS:
            seen = {item.line_no for item in windows[-1]}
            windows[-1].extend(item for item in current if item.line_no not in seen)
        else:
            windows.append(current)
    return windows


def build_chunks(lines: list[SubtitleLine], *, work: str, episode: int, source: str) -> list[dict]:
    usable = [ln for ln in lines if is_usable_ja_line(ln.text)]
    if not usable:
        return []
    chunks: list[dict] = []
    season, season_episode = season_scope_for_global_episode(episode)
    for chunk_no, window in enumerate(chunk_windows(usable), start=1):
        chunks.append({
            "id": f"{work}-ep{episode:02d}-ja-chunk-{chunk_no:04d}",
            "text": "\n".join(ln.text for ln in window),
            "metadata": {
                "work": work,
                "episode": episode,
                "season": season,
                "season_episode": season_episode,
                "chunk_no": chunk_no,
                "start_time": ms_to_hhmmss(window[0].start_ms),
                "end_time": ms_to_hhmmss(window[-1].end_ms),
                "language": "ja",
                "source": source,
                "start_line": window[0].line_no,
                "end_line": window[-1].line_no,
            },
        })
    return chunks


def iter_srt_files(input_dir: Path) -> list[Path]:
    return sorted(input_dir.glob("*.srt"), key=lambda p: parse_episode(p))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input-dir", type=Path, default=Path("Re_Zero kara Hajimeru Isekai Seikatsu"))
    parser.add_argument("--output", type=Path, default=Path("output/vectorize/rezero_s1_chunks.jsonl"))
    parser.add_argument("--work", default="rezero")
    parser.add_argument("--source", default="ja_subtitle")
    parser.add_argument("--episode-offset", type=int, default=0)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    if not args.input_dir.is_dir():
        print(f"Input directory not found: {args.input_dir}", file=sys.stderr)
        return 1
    srt_files = iter_srt_files(args.input_dir)
    if not srt_files:
        print(f"No .srt files found in {args.input_dir}", file=sys.stderr)
        return 1
    all_chunks: list[dict] = []
    report_rows: list[dict] = []
    for path in srt_files:
        source_episode = parse_episode(path)
        episode = source_episode + args.episode_offset
        raw_lines = parse_srt(path)
        usable_count = sum(1 for ln in raw_lines if is_usable_ja_line(ln.text))
        chunks = build_chunks(raw_lines, work=args.work, episode=episode, source=args.source)
        all_chunks.extend(chunks)
        report_rows.append({
            "file": str(path),
            "source_episode": source_episode,
            "episode": episode,
            "cues": len(raw_lines),
            "usable": usable_count,
            "chunks": len(chunks),
            "first_time": chunks[0]["metadata"]["start_time"] if chunks else "",
            "last_time": chunks[-1]["metadata"]["end_time"] if chunks else "",
        })
        print(f"EP{episode:02d}: source_ep={source_episode:02d} cues={len(raw_lines)} usable={usable_count} chunks={len(chunks)}")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="\n") as f:
        for chunk in all_chunks:
            f.write(json.dumps(chunk, ensure_ascii=False) + "\n")
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        with args.report.open("w", encoding="utf-8", newline="\n") as f:
            json.dump(report_rows, f, ensure_ascii=False, indent=2)
    print(f"Wrote {len(all_chunks)} chunks -> {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
