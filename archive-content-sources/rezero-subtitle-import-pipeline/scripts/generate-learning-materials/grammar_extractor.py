"""Grammar pattern extraction per episode."""

from __future__ import annotations

import re

from ai_generator import AIGenerator
from data_loader import SubtitleLine
from id_utils import grammar_point_id
from japanese_utils import is_japanese_line

RE_STAFF = re.compile(
    r"(字幕|翻译|校对|时间轴|压制|制作|www\.|https?://|本字幕|仅供学习)",
    re.I,
)

GRAMMAR_PATTERNS: list[tuple[str, str, re.Pattern[str], str]] = [
    ("～ないと", "必须/否则", re.compile(r"ないと"), "N4"),
    ("～てしまう", "完了/遗憾", re.compile(r"(てしま|ちゃっ|じゃっ|てしもう)"), "N4"),
    ("～んだ/のだ", "说明/强调", re.compile(r"(んだ|のだ|んです|のです)(?=[。！？]?$|[\s　])"), "N3"),
    ("～わけじゃない", "并非/不是那个意思", re.compile(r"わけじゃない|わけではない"), "N3"),
    ("～しかない", "只能", re.compile(r"しかない"), "N3"),
    ("～ておく", "事先做好", re.compile(r"(ておく|とく|どく)"), "N3"),
    ("～ように", "为了/以免", re.compile(r"ように"), "N3"),
    ("～みたい", "好像/像", re.compile(r"みたい"), "N4"),
    ("～って", "引用/话题", re.compile(r"って"), "N4"),
    ("句末 よ/ね/かな/だろ", "语气助词", re.compile(r"(よ|ね|かな|だろ)(?=[。！？]?$)"), "N5"),
    ("命令形", "命令", re.compile(r"(ろ|え)(?=[。！？]?$)|しなさい|てください"), "N4"),
    ("请求表达", "请求", re.compile(r"(てくれ|てよ|ちょうだい|お願い)"), "N4"),
]


def extract_grammar_points(
    work_slug: str,
    ep: int,
    episode_id: str,
    lines: list[SubtitleLine],
    ai: AIGenerator,
) -> list[dict]:
    found: list[dict] = []
    seen: set[str] = set()
    sort_order = 0
    for line in lines:
        if not line.usable or not is_japanese_line(line.ja_text):
            continue
        for pattern, func_zh, regex, diff in GRAMMAR_PATTERNS:
            if not regex.search(line.ja_text):
                continue
            key = f"{pattern}|{line.ja_text}"
            if key in seen:
                continue
            seen.add(key)
            sort_order += 1
            enriched = ai.enrich_grammar(pattern, line.ja_text, line.zh_text, func_zh)
            found.append(
                {
                    "id": grammar_point_id(work_slug, ep, pattern, line.ja_text),
                    "work_slug": work_slug,
                    "episode_id": episode_id,
                    "episode": ep,
                    "pattern": pattern,
                    "function_zh": func_zh,
                    "ja_example": line.ja_text,
                    "explanation_zh": enriched.get("explanation_zh", ""),
                    "pragmatics_note": enriched.get("pragmatics_note", ""),
                    "real_world_note": enriched.get("real_world_note", ""),
                    "difficulty": diff,
                    "source_line_no": line.line_no,
                    "sort_order": sort_order,
                }
            )
            if len(found) >= 15:
                return found
    return found
