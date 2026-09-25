"""Vocabulary extraction from subtitle lines."""

from __future__ import annotations

import json
import re
from collections import defaultdict
from dataclasses import dataclass, field

from data_loader import SubtitleLine
from id_utils import vocab_item_id, vocab_occurrence_id
from japanese_utils import (
    estimate_jlpt,
    guess_reading,
    has_kanji,
    infer_pos,
    is_japanese_line,
    is_mostly_katakana,
    normalize_ja,
    to_romaji,
)

RE_NAME_SUFFIX = re.compile(r"(ちゃん|くん|さん|せんせい|先輩|後輩)$")
RE_ONOMATOPOEIA = re.compile(r"^[ァ-ヶー]{2,6}$|^[ぁ-ん]{2,4}[ッ〜～]+$")
RE_STAFF = re.compile(
    r"(字幕|翻译|校对|时间轴|压制|制作|www\.|https?://|本字幕|仅供学习)",
    re.I,
)
RE_KANA_ONLY = re.compile(r"[\u3040-\u309f\u30a0-\u30ffー]+")

STOP_SURFACES = {
    "の", "は", "が", "を", "に", "で", "と", "も", "へ", "や", "か", "ね", "よ", "わ",
    "さ", "な", "ぞ", "ぜ", "て", "た", "だ", "です", "ます", "ない", "ある", "いる",
    "する", "これ", "それ", "あれ", "ここ", "そこ", "あそこ", "私", "僕", "俺",
    "ちゃん", "くん", "さん", "せんせい",
}


@dataclass
class VocabCandidate:
    surface: str
    total_count: int = 0
    episode_counts: dict[int, int] = field(default_factory=dict)
    line_nos: dict[int, list[int]] = field(default_factory=lambda: defaultdict(list))
    zh_hints: list[str] = field(default_factory=list)
    contexts: list[str] = field(default_factory=list)


def tokenize_ja(text: str) -> list[str]:
    text = normalize_ja(text)
    if not text:
        return []
    tokens: list[str] = []
    for m in re.finditer(r"[\u4e00-\u9fff]{2,6}", text):
        tokens.append(m.group(0))
    for m in re.finditer(r"[\u30a0-\u30ffー]{2,}", text):
        tokens.append(m.group(0))
    for m in re.finditer(r"[\u3040-\u309f]{2,}", text):
        w = m.group(0)
        if w not in STOP_SURFACES:
            tokens.append(w)
    for m in re.finditer(
        r"[\u4e00-\u9fff]+(?:する|した|して|しない|できる|いる|ある|ない|たい)", text
    ):
        tokens.append(m.group(0))
    for m in re.finditer(
        r"(そろそろ|ちょっと|ぜひ|おはよう|ありがとう|ごめん|だめ|やばい|まじ|"
        r"遅刻|部活|高校|入学|今日|明日|昨日|大丈夫|本当|全然|結構|"
        r"みたい|らしい|かも|しかない|ないと|てしまう)",
        text,
    ):
        tokens.append(m.group(0))
    return tokens


def is_digit_only(surface: str) -> bool:
    return bool(re.fullmatch(r"[\d０-９]+", surface))


def is_skip_token(surface: str, total_count: int, episode_count: int) -> bool:
    if not surface or surface in STOP_SURFACES or len(surface) == 1:
        return True
    if len(surface) > 12 or is_digit_only(surface):
        return True
    if RE_ONOMATOPOEIA.match(surface) and total_count < 3:
        return True
    if RE_NAME_SUFFIX.search(surface) and total_count <= 2:
        return True
    if is_mostly_katakana(surface) and len(surface) <= 3 and total_count < 2:
        return True
    if total_count == 1 and len(surface) >= 5 and has_kanji(surface):
        return True
    return False


def extract_vocab_candidates(lines: list[SubtitleLine]) -> dict[str, VocabCandidate]:
    cands: dict[str, VocabCandidate] = {}
    for line in lines:
        if not line.usable or not is_japanese_line(line.ja_text):
            continue
        if RE_STAFF.search(line.ja_text):
            continue
        for tok in tokenize_ja(line.ja_text):
            tok = normalize_ja(tok)
            if not tok:
                continue
            if tok not in cands:
                cands[tok] = VocabCandidate(surface=tok)
            c = cands[tok]
            c.total_count += 1
            c.episode_counts[line.episode] = c.episode_counts.get(line.episode, 0) + 1
            c.line_nos[line.episode].append(line.line_no)
            if len(c.contexts) < 5:
                c.contexts.append(line.ja_text)
            if line.zh_text and len(c.zh_hints) < 3:
                c.zh_hints.append(line.zh_text)
    return cands


def select_vocab_items(
    work_slug: str, cands: dict[str, VocabCandidate], min_count: int = 2
) -> list[dict]:
    scored: list[tuple[float, VocabCandidate]] = []
    for c in cands.values():
        ep_count = len(c.episode_counts)
        if is_skip_token(c.surface, c.total_count, ep_count):
            continue
        if c.total_count < min_count and ep_count < 2:
            continue
        score = c.total_count * 2 + ep_count * 3
        if has_kanji(c.surface):
            score += 2
        if 2 <= len(c.surface) <= 6:
            score += 1
        if c.surface.endswith(("よ", "ね", "か", "な")):
            score += 1
        if is_mostly_katakana(c.surface) and c.total_count < 3:
            score -= 3
        scored.append((score, c))
    scored.sort(key=lambda x: (-x[0], -x[1].total_count, x[1].surface))
    items: list[dict] = []
    for _, c in scored[:500]:
        reading = guess_reading(c.surface, c.contexts)
        if not reading and RE_KANA_ONLY.fullmatch(c.surface):
            reading = c.surface
        romaji = to_romaji(reading) if reading else ""
        meaning = c.zh_hints[0] if c.zh_hints else ""
        suitable_hw = has_kanji(c.surface) and len(c.surface) <= 8
        suitable_sh = 2 <= len(c.surface) <= 10 and c.total_count >= 2
        items.append(
            {
                "id": vocab_item_id(work_slug, c.surface),
                "work_slug": work_slug,
                "surface": c.surface,
                "reading": reading,
                "romaji": romaji,
                "meaning_zh": meaning,
                "pos": infer_pos(c.surface),
                "jlpt_level": estimate_jlpt(c.surface, c.total_count),
                "suitable_handwriting": suitable_hw,
                "suitable_shadowing": suitable_sh,
                "anime_tone_note": "日常校园口语，动漫语气自然" if suitable_sh else "偏叙述或专有表达",
                "real_world_note": "现实中可用" if suitable_sh and not is_mostly_katakana(c.surface) else "偏动漫语境",
                "total_occurrences": c.total_count,
                "episode_count": len(c.episode_counts),
                "_candidate": c,
            }
        )
    return items


def build_vocab_occurrences(
    work_slug: str, episode_meta: dict[int, object], vocab_items: list[dict]
) -> list[dict]:
    rows: list[dict] = []
    for item in vocab_items:
        c: VocabCandidate = item.pop("_candidate")
        for ep, count in sorted(c.episode_counts.items()):
            meta = episode_meta[ep]
            line_nos = sorted(set(c.line_nos[ep]))[:10]
            rows.append(
                {
                    "id": vocab_occurrence_id(work_slug, ep, item["id"]),
                    "work_slug": work_slug,
                    "episode_id": meta.id,
                    "episode": ep,
                    "vocab_item_id": item["id"],
                    "occurrence_count": count,
                    "example_line_nos": json.dumps(line_nos, ensure_ascii=False),
                }
            )
    return rows
