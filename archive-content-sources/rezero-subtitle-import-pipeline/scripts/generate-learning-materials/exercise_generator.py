"""Handwriting and listening exercise generation."""

from __future__ import annotations

from id_utils import exercise_id


def generate_exercises(
    work_slug: str,
    ep: int,
    episode_id: str,
    ep_vocab_ids: list[str],
    vocab_by_id: dict[str, dict],
) -> list[dict]:
    rows: list[dict] = []
    idx = 0
    ep_vocab = [vocab_by_id[vid] for vid in ep_vocab_ids if vid in vocab_by_id]
    hw_vocab = [v for v in ep_vocab if v.get("suitable_handwriting")][:15]
    sh_vocab = [v for v in ep_vocab if v.get("suitable_shadowing")][:15]
    all_vocab = ep_vocab[:25]

    for v in hw_vocab:
        if not v.get("reading"):
            continue
        idx += 1
        rows.append(
            {
                "id": exercise_id(work_slug, ep, "kana_to_kanji", idx),
                "work_slug": work_slug,
                "episode_id": episode_id,
                "episode": ep,
                "exercise_type": "kana_to_kanji",
                "prompt": f"假名：{v['reading']}",
                "answer": v["surface"],
                "hint": v.get("meaning_zh", ""),
                "difficulty": v.get("jlpt_level", "N4"),
                "vocab_item_id": v["id"],
                "sort_order": idx,
            }
        )

    for v in all_vocab:
        if not v.get("meaning_zh"):
            continue
        idx += 1
        rows.append(
            {
                "id": exercise_id(work_slug, ep, "meaning_to_japanese", idx),
                "work_slug": work_slug,
                "episode_id": episode_id,
                "episode": ep,
                "exercise_type": "meaning_to_japanese",
                "prompt": f"中文：{v['meaning_zh'][:40]}",
                "answer": v["surface"],
                "hint": v.get("reading", ""),
                "difficulty": v.get("jlpt_level", "N4"),
                "vocab_item_id": v["id"],
                "sort_order": idx,
            }
        )

    for v in sh_vocab:
        if not v.get("reading"):
            continue
        idx += 1
        rows.append(
            {
                "id": exercise_id(work_slug, ep, "listening_to_write", idx),
                "work_slug": work_slug,
                "episode_id": episode_id,
                "episode": ep,
                "exercise_type": "listening_to_write",
                "prompt": f"听音写词（TTS 播放）：{v['reading']}",
                "answer": v["surface"],
                "hint": v.get("romaji", ""),
                "difficulty": v.get("jlpt_level", "N4"),
                "vocab_item_id": v["id"],
                "sort_order": idx,
            }
        )

    return rows[:40]
