"""Episode learning plan builder."""

from __future__ import annotations

import json

from id_utils import plan_id


def build_episode_plan(
    work_slug: str,
    ep: int,
    episode_id: str,
    ep_vocab_ids: list[str],
    grammar_rows: list[dict],
    sentence_rows: list[dict],
    exercise_rows: list[dict],
    vocab_by_id: dict[str, dict],
) -> dict:
    vocab_top = ep_vocab_ids[:20]
    hw = [
        vid for vid in ep_vocab_ids if vocab_by_id.get(vid, {}).get("suitable_handwriting")
    ][:10]
    if len(hw) < 10:
        hw = vocab_top[:10]
    shadow = [r["id"] for r in sentence_rows[:5]]
    grammar = [r["id"] for r in grammar_rows[:5]]
    exercises = [r["id"] for r in exercise_rows[:20]]
    return {
        "id": plan_id(work_slug, ep),
        "work_slug": work_slug,
        "episode_id": episode_id,
        "episode": ep,
        "vocab_item_ids": json.dumps(vocab_top, ensure_ascii=False),
        "handwriting_vocab_ids": json.dumps(hw, ensure_ascii=False),
        "shadowing_sentence_ids": json.dumps(shadow, ensure_ascii=False),
        "grammar_point_ids": json.dumps(grammar, ensure_ascii=False),
        "exercise_ids": json.dumps(exercises, ensure_ascii=False),
        "notes": f"第{ep}集学习计划：高频词20、手写10、跟读5、语法{len(grammar)}",
    }
