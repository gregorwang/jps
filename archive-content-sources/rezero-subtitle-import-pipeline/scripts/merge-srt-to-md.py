#!/usr/bin/env python3
"""Merge SRT subtitle files into an AI-friendly Markdown document."""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from datetime import date
from pathlib import Path

RE_EPISODE = re.compile(r"S(\d+)E(\d+)", re.IGNORECASE)
RE_EP_TITLE = re.compile(r"S\d+E\d+\.(.+?)\.WEBRip", re.IGNORECASE)
RE_SPEAKER = re.compile(r"^[（(]((?:[^（）()]|\([^）)]*\))*?)[）)]\s*")
RE_FURIGANA = re.compile(r"\([^)]*\)")
RE_SRT_TAGS = re.compile(r"\{[^}]*\}")


@dataclass
class DialogueLine:
    index: int
    start: str
    end: str
    speaker: str | None
    text: str
    raw: str


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


def ms_to_timestamp(ms: int) -> str:
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


def parse_speaker_name(raw: str) -> str:
    return RE_FURIGANA.sub("", raw).strip()


def is_sound_effect(tag: str) -> bool:
    cleaned = RE_FURIGANA.sub("", tag).strip()
    if not cleaned:
        return True
    if any(kw in cleaned for kw in ("音", "声", "ノック", "チャイム", "笑", "拍手", "ドア")):
        return True
    return cleaned.startswith("♪") or cleaned.endswith("♪")


def split_dialogue_lines(text: str) -> list[tuple[str | None, str]]:
    results: list[tuple[str | None, str]] = []
    pending_speaker: str | None = None
    for line in text.split("\n"):
        line = line.strip()
        if not line:
            continue
        m = RE_SPEAKER.match(line)
        if m:
            tag = m.group(1)
            rest = line[m.end() :].strip()
            if rest:
                if is_sound_effect(tag):
                    results.append((None, line))
                else:
                    results.append((parse_speaker_name(tag), rest))
                pending_speaker = None
            elif is_sound_effect(tag):
                results.append((None, line))
                pending_speaker = None
            else:
                pending_speaker = parse_speaker_name(tag)
        else:
            results.append((pending_speaker, line))
            pending_speaker = None
    return results


def parse_srt(path: Path) -> list[DialogueLine]:
    content = read_text_auto(path)
    blocks = re.split(r"\n\s*\n", content.strip(), flags=re.MULTILINE)
    lines: list[DialogueLine] = []
    idx = 0
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
        idx += 1
        start_ms = srt_time_to_ms(start_raw)
        end_ms = srt_time_to_ms(end_raw)
        start = ms_to_timestamp(start_ms)
        end = ms_to_timestamp(end_ms)
        parts = split_dialogue_lines(text)
        if len(parts) == 1:
            speaker, body = parts[0]
            lines.append(
                DialogueLine(
                    index=idx,
                    start=start,
                    end=end,
                    speaker=speaker,
                    text=body,
                    raw=text,
                )
            )
        else:
            for i, (speaker, body) in enumerate(parts):
                lines.append(
                    DialogueLine(
                        index=idx if i == 0 else idx,
                        start=start,
                        end=end,
                        speaker=speaker,
                        text=body,
                        raw=body,
                    )
                )
    return lines


def episode_sort_key(path: Path) -> tuple[int, int]:
    m = RE_EPISODE.search(path.name)
    if m:
        return int(m.group(1)), int(m.group(2))
    return 0, 0


def episode_title(path: Path) -> str:
    m = RE_EP_TITLE.search(path.name)
    return m.group(1) if m else path.stem


def episode_label(path: Path) -> str:
    m = RE_EPISODE.search(path.name)
    if m:
        return f"S{m.group(1)}E{int(m.group(2)):02d}"
    return path.stem


def format_md_line(line: DialogueLine) -> str:
    if line.speaker:
        body = f"**{line.speaker}**: {line.text}"
    else:
        body = line.text
    return f"- `[{line.start}]` {body}"


def build_markdown(episodes: list[tuple[Path, list[DialogueLine]]], title: str) -> str:
    total_lines = sum(len(lines) for _, lines in episodes)
    parts = [
        f"# {title}",
        "",
        f"> 共 {len(episodes)} 集 · {total_lines} 条字幕 · 生成于 {date.today().isoformat()}",
        "> 格式: `- [时间戳] 说话人: 台词`（无说话人则为旁白/音效）",
        "",
        "---",
        "",
    ]
    for path, lines in episodes:
        label = episode_label(path)
        ep_title = episode_title(path)
        parts.append(f"## {label} {ep_title}")
        parts.append("")
        parts.extend(format_md_line(line) for line in lines)
        parts.append("")
        parts.append("---")
        parts.append("")
    return "\n".join(parts).rstrip() + "\n"


def build_jsonl(episodes: list[tuple[Path, list[DialogueLine]]]) -> str:
    rows = []
    for path, lines in episodes:
        label = episode_label(path)
        ep_title = episode_title(path)
        for line in lines:
            rows.append(
                {
                    "episode": label,
                    "episode_title": ep_title,
                    "index": line.index,
                    "start": line.start,
                    "end": line.end,
                    "speaker": line.speaker,
                    "text": line.text,
                }
            )
    return "\n".join(json.dumps(row, ensure_ascii=False) for row in rows) + "\n"


def collect_srt_files(input_dirs: list[Path]) -> list[Path]:
    seen: set[str] = set()
    files: list[Path] = []
    for input_dir in input_dirs:
        for path in input_dir.glob("*.srt"):
            if path.name in seen:
                continue
            seen.add(path.name)
            files.append(path)
    return sorted(files, key=episode_sort_key)


def main() -> None:
    parser = argparse.ArgumentParser(description="Merge SRT files into Markdown/JSONL")
    parser.add_argument(
        "input_dirs",
        type=Path,
        nargs="+",
        help="One or more directories containing .srt files",
    )
    parser.add_argument(
        "-o",
        "--output",
        type=Path,
        help="Output Markdown path (default: first input_dir/subtitles-merged.md)",
    )
    parser.add_argument("--jsonl", type=Path, help="Optional JSONL output path")
    parser.add_argument("--title", default="字幕合集", help="Document title")
    args = parser.parse_args()

    srt_files = collect_srt_files(args.input_dirs)
    if not srt_files:
        raise SystemExit(f"No .srt files found in {', '.join(str(d) for d in args.input_dirs)}")

    episodes: list[tuple[Path, list[DialogueLine]]] = []
    for path in srt_files:
        lines = parse_srt(path)
        episodes.append((path, lines))
        print(f"  {episode_label(path)}: {len(lines)} lines")

    md_path = args.output or (args.input_dirs[0] / "subtitles-merged.md")
    md_path.write_text(build_markdown(episodes, args.title), encoding="utf-8")
    print(f"\nMarkdown: {md_path} ({md_path.stat().st_size:,} bytes)")

    if args.jsonl:
        args.jsonl.write_text(build_jsonl(episodes), encoding="utf-8")
        print(f"JSONL:    {args.jsonl} ({args.jsonl.stat().st_size:,} bytes)")


if __name__ == "__main__":
    main()
