"""Stable deterministic IDs for learning materials."""

from __future__ import annotations

import hashlib
import re
import unicodedata


def _slug_part(text: str, max_len: int = 40) -> str:
    text = unicodedata.normalize("NFKC", text or "").strip()
    text = re.sub(r"\s+", "-", text)
    text = re.sub(r"[^\w\u3040-\u309f\u30a0-\u30ff\u4e00-\u9fff-]", "", text)
    if not text:
        text = hashlib.sha256((text or "empty").encode()).hexdigest()[:8]
    return text[:max_len]


def vocab_item_id(work_slug: str, surface: str) -> str:
    return f"{work_slug}-vocab-{_slug_part(surface)}"


def vocab_occurrence_id(work_slug: str, episode: int, vocab_item_id: str) -> str:
    return f"{work_slug}-ep{episode:02d}-occ-{_slug_part(vocab_item_id.split('-vocab-')[-1], 30)}"


def grammar_point_id(work_slug: str, episode: int, pattern: str, example: str) -> str:
    digest = hashlib.sha256(f"{pattern}|{example}".encode()).hexdigest()[:10]
    pat = _slug_part(pattern, 20)
    return f"{work_slug}-ep{episode:02d}-grammar-{pat}-{digest}"


def sentence_id(work_slug: str, episode: int, line_no: int) -> str:
    return f"{work_slug}-ep{episode:02d}-sent-{line_no:05d}"


def exercise_id(work_slug: str, episode: int, ex_type: str, index: int) -> str:
    return f"{work_slug}-ep{episode:02d}-ex-{ex_type}-{index:03d}"


def plan_id(work_slug: str, episode: int) -> str:
    return f"{work_slug}-ep{episode:02d}-plan"
