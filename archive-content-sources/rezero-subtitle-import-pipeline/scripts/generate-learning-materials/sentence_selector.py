"""Shadowing sentence selection per episode."""

from __future__ import annotations

import json
import re

from ai_generator import AIGenerator
from data_loader import SubtitleLine
from id_utils import sentence_id
from japanese_utils import guess_reading, is_japanese_line, kana_ratio, to_romaji

RE_STAFF = re.compile(
    r"(字幕|翻译|校对|时间轴|压制|制作|www\.|https?://|本字幕|仅供学习)",
    re.I,
)
RE_CHINESE_MAIN = re.compile(r"[的吗嘛吧呢，；]|动画|感谢|工作人员|字幕")


def score_sentence(line: SubtitleLine) -> float:
    ja = line.ja_text
    if not line.usable or not is_japanese_line(ja):
        return -1
    if RE_STAFF.search(ja):
        return -1
    if RE_CHINESE_MAIN.search(ja) and kana_ratio(ja) < 0.1:
        return -1
    n = len(ja)
    if n < 3 or n > 45:
        return -1
    score = 10.0
    if 8 <= n <= 28:
        score += 5
    if re.search(r"(よ|ね|かな|だろ|です|ます)(?=[。！？]?$)", ja):
        score += 4
    if line.zh_text:
        score += 1
    if "♪" in ja or "♫" in ja:
        score -= 10
    if n > 35:
        score -= 3
    return score


def select_sentences(
    work_slug: str, ep: int, episode_id: str, lines: list[SubtitleLine], ai: AIGenerator
) -> list[dict]:
    scored = [(score_sentence(l), l) for l in lines]
    scored = [(s, l) for s, l in scored if s >= 0]
    scored.sort(key=lambda x: (-x[0], x[1].line_no))
    rows: list[dict] = []
    for i, (_, line) in enumerate(scored[:30]):
        enriched = ai.enrich_sentence(line.ja_text, line.zh_text)
        reading = enriched.get("reading") or guess_reading(line.ja_text, [line.ja_text])
        rows.append(
            {
                "id": sentence_id(work_slug, ep, line.line_no),
                "work_slug": work_slug,
                "episode_id": episode_id,
                "episode": ep,
                "ja_text": line.ja_text,
                "reading": reading,
                "romaji": to_romaji(reading) if reading else to_romaji(line.ja_text),
                "meaning_zh": enriched.get("meaning_zh") or line.zh_text,
                "tone_tags": json.dumps(enriched.get("tone_tags", ["日常"]), ensure_ascii=False),
                "difficulty": enriched.get("difficulty", "N4"),
                "recommended_shadowing": True,
                "source_line_no": line.line_no,
                "sort_order": i + 1,
            }
        )
    return rows
