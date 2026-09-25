"""ASS subtitle parsing for video pipeline."""

from __future__ import annotations

import csv
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path

RE_ASS_TAGS = re.compile(r"\{[^}]*\}")
RE_ASS_BREAK = re.compile(r"\\[Nn]")
RE_HIRAGANA = re.compile(r"[\u3040-\u309f]")
RE_KATAKANA = re.compile(r"[\u30a0-\u30ff]")
RE_KANA = re.compile(r"[\u3040-\u309f\u30a0-\u30ff]")
RE_CJK = re.compile(r"[\u4e00-\u9fff]")
RE_CHINESE_INDICATORS = re.compile(
    r"[的了是在有不这那吗嘛吧呢]"
    r"|[，；。！？]"
    r"|什麼|什麼|沒有|這個|那個|為什麼|怎麼|謝謝|對不起"
)


@dataclass
class RawCue:
    start_ms: int
    end_ms: int
    style: str
    text: str


@dataclass
class SubtitleRow:
    episode_id: str
    subtitle_index: int
    start_ms: int
    end_ms: int
    ja_text: str
    zh_text: str
    raw_text: str


def read_text_auto(path: Path) -> str:
    for enc in ("utf-8-sig", "utf-8", "utf-16", "cp932", "shift_jis"):
        try:
            return path.read_text(encoding=enc)
        except (UnicodeDecodeError, UnicodeError):
            continue
    return path.read_text(encoding="utf-8", errors="replace")


def clean_ass_text(raw: str) -> str:
    text = RE_ASS_TAGS.sub("", raw)
    text = RE_ASS_BREAK.sub("\n", text)
    text = text.replace("\\h", " ")
    text = re.sub(r"[ \t　]+", " ", text)
    return text.strip()


def ass_time_to_ms(t: str) -> int:
    t = t.strip().replace(",", ".")
    parts = t.split(":")
    if len(parts) == 3:
        h, m, s = parts
        return int(round((int(h) * 3600 + int(m) * 60 + float(s)) * 1000))
    if len(parts) == 2:
        m, s = parts
        return int(round((int(m) * 60 + float(s)) * 1000))
    return 0


def style_lang_hint(style: str) -> str | None:
    lower = style.strip().lower()
    if lower.startswith("jp_"):
        return "ja"
    if lower.startswith("zh_"):
        return "zh"
    if lower.endswith("jp") or lower in ("opj", "edj"):
        return "ja"
    if lower.endswith("c") or lower in ("opc", "edc", "zhengwen"):
        return "zh"
    return None


def detect_line_lang(text: str) -> str:
    text = text.strip()
    if not text:
        return "empty"
    kana = len(RE_KANA.findall(text))
    cjk = len(RE_CJK.findall(text))
    if kana > 0:
        return "ja"
    if RE_CHINESE_INDICATORS.search(text) and kana == 0 and cjk >= 1:
        return "zh"
    if cjk >= 2 and kana == 0:
        return "zh"
    if cjk >= 1 and kana >= 1:
        return "ja"
    return "other"


def split_ja_zh_text(text: str) -> tuple[str, str]:
    lines = [ln.strip() for ln in text.split("\n") if ln.strip()]
    if not lines:
        return "", ""
    if len(lines) == 1:
        lang = detect_line_lang(lines[0])
        if lang == "zh":
            return "", lines[0]
        return lines[0], ""
    ja_parts: list[str] = []
    zh_parts: list[str] = []
    for ln in lines:
        lang = detect_line_lang(ln)
        if lang == "zh":
            zh_parts.append(ln)
        else:
            ja_parts.append(ln)
    return " ".join(ja_parts).strip(), " ".join(zh_parts).strip()


def parse_ass_cues(path: Path) -> list[RawCue]:
    cues: list[RawCue] = []
    in_events = False
    for line in read_text_auto(path).splitlines():
        stripped = line.strip()
        if stripped == "[Events]":
            in_events = True
            continue
        if not in_events or not stripped.startswith("Dialogue:"):
            continue
        body = stripped[len("Dialogue:") :].lstrip()
        parts = body.split(",", 9)
        if len(parts) < 10:
            continue
        start_ms = ass_time_to_ms(parts[1])
        end_ms = ass_time_to_ms(parts[2])
        style = parts[3].strip()
        text = clean_ass_text(parts[9])
        if not text:
            continue
        cues.append(RawCue(start_ms=start_ms, end_ms=end_ms, style=style, text=text))
    return cues


def cues_overlap(a_start: int, a_end: int, b_start: int, b_end: int, slack_ms: int = 800) -> bool:
    return not (a_end + slack_ms < b_start or b_end + slack_ms < a_start)


def find_zh_for_ja(ja: RawCue, zh_cues: list[RawCue]) -> str:
    best = ""
    best_overlap = -1
    for zh in zh_cues:
        if not cues_overlap(ja.start_ms, ja.end_ms, zh.start_ms, zh.end_ms):
            continue
        overlap = min(ja.end_ms, zh.end_ms) - max(ja.start_ms, zh.start_ms)
        if overlap > best_overlap:
            best_overlap = overlap
            _, zh_text = split_ja_zh_text(zh.text)
            best = zh_text or zh.text
    return best


def build_subtitle_index(episode_id: str, cues: list[RawCue]) -> list[SubtitleRow]:
    ja_entries: list[tuple[RawCue, str, str]] = []
    zh_pool: list[RawCue] = []

    for cue in cues:
        hint = style_lang_hint(cue.style)
        ja_text, zh_text = split_ja_zh_text(cue.text)
        if hint == "zh" and not ja_text:
            zh_pool.append(cue)
            continue
        if hint == "ja" and ja_text:
            ja_entries.append((cue, ja_text, zh_text))
            continue
        if ja_text and not zh_text:
            ja_entries.append((cue, ja_text, ""))
            continue
        if zh_text and not ja_text:
            zh_pool.append(cue)
            continue
        if ja_text:
            ja_entries.append((cue, ja_text, zh_text))
        elif zh_text:
            zh_pool.append(cue)

    rows: list[SubtitleRow] = []
    for idx, (cue, ja_text, inline_zh) in enumerate(ja_entries, start=1):
        zh_text = inline_zh or find_zh_for_ja(cue, zh_pool)
        rows.append(
            SubtitleRow(
                episode_id=episode_id,
                subtitle_index=idx,
                start_ms=cue.start_ms,
                end_ms=cue.end_ms,
                ja_text=ja_text,
                zh_text=zh_text,
                raw_text=cue.text.replace("\n", "\\N"),
            )
        )
    return rows


def parse_ass_to_index(episode_id: str, subtitle_path: Path) -> list[SubtitleRow]:
    cues = parse_ass_cues(subtitle_path)
    return build_subtitle_index(episode_id, cues)


def write_subtitle_index(path: Path, rows: list[SubtitleRow]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(
            f,
            fieldnames=[
                "episode_id",
                "subtitle_index",
                "start_ms",
                "end_ms",
                "ja_text",
                "zh_text",
                "raw_text",
            ],
        )
        writer.writeheader()
        for row in rows:
            writer.writerow(
                {
                    "episode_id": row.episode_id,
                    "subtitle_index": row.subtitle_index,
                    "start_ms": row.start_ms,
                    "end_ms": row.end_ms,
                    "ja_text": row.ja_text,
                    "zh_text": row.zh_text,
                    "raw_text": row.raw_text,
                }
            )
