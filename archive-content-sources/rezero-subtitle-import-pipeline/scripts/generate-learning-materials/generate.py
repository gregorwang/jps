#!/usr/bin/env python3
"""
Anime Japanese Lab — 学习材料 CSV 生成器

从 output/supabase/*.csv 读取日文台词，生成可导入 Supabase 的学习材料。

运行: python scripts/generate-learning-materials/generate.py
"""

from __future__ import annotations

import csv
import os
import sys
from collections import defaultdict
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from ai_generator import OpenAIGenerator, RuleBasedGenerator  # noqa: E402
from data_loader import load_episodes, load_subtitle_lines  # noqa: E402
from exercise_generator import generate_exercises  # noqa: E402
from grammar_extractor import extract_grammar_points  # noqa: E402
from plan_generator import build_episode_plan  # noqa: E402
from report import EpisodeStats, generate_report  # noqa: E402
from sentence_selector import select_sentences  # noqa: E402
from vocab_extractor import (  # noqa: E402
    build_vocab_occurrences,
    extract_vocab_candidates,
    select_vocab_items,
)

ROOT = SCRIPT_DIR.parent.parent
SUPABASE_DIR = ROOT / "output" / "supabase"
OUTPUT_DIR = ROOT / "output" / "learning"


def write_csv(path: Path, headers: list[str], rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    normalized: list[dict] = []
    for row in rows:
        out = {}
        for k, v in row.items():
            if isinstance(v, bool):
                out[k] = "true" if v else "false"
            else:
                out[k] = v
        normalized.append(out)
    with path.open("w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=headers, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(normalized)


def check_duplicates(rows: list[dict], key: str = "id") -> list[str]:
    seen: set[str] = set()
    dups: list[str] = []
    for r in rows:
        k = r[key]
        if k in seen:
            dups.append(k)
        seen.add(k)
    return dups


def get_ai_generator():
    api_key = os.environ.get("OPENAI_API_KEY", "")
    if api_key:
        return OpenAIGenerator(api_key=api_key), "OpenAI"
    return RuleBasedGenerator(), "rule-based (no AI key)"


def main() -> None:
    if not SUPABASE_DIR.exists():
        print(f"Missing {SUPABASE_DIR}. Run preprocess.py first.")
        sys.exit(1)

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    ai, ai_mode = get_ai_generator()

    lines = load_subtitle_lines(SUPABASE_DIR)
    episodes = load_episodes(SUPABASE_DIR)
    if not lines:
        print("No subtitle lines found.")
        sys.exit(1)

    work_slug = lines[0].work_slug
    work_name = lines[0].work_display_name
    episode_meta = {e.episode: e for e in episodes if e.work_slug == work_slug}

    by_episode: dict[int, list] = defaultdict(list)
    work_lines = [l for l in lines if l.work_slug == work_slug]
    for line in work_lines:
        by_episode[line.episode].append(line)

    cands = extract_vocab_candidates(work_lines)
    vocab_items = select_vocab_items(work_slug, cands)
    vocab_by_id = {v["id"]: v for v in vocab_items}
    vocab_occurrences = build_vocab_occurrences(work_slug, episode_meta, vocab_items)

    grammar_all: list[dict] = []
    sentences_all: list[dict] = []
    exercises_all: list[dict] = []
    plans_all: list[dict] = []
    episode_stats: list[EpisodeStats] = []
    global_warnings: list[str] = []

    for ep in sorted(by_episode.keys()):
        ep_lines = by_episode[ep]
        meta = episode_meta.get(ep)
        if not meta:
            global_warnings.append(f"Episode {ep} missing from episodes.csv")
            continue

        st = EpisodeStats(episode=ep)
        ep_occ = [o for o in vocab_occurrences if o["episode"] == ep]
        ep_vocab_ids = sorted(
            [o["vocab_item_id"] for o in ep_occ],
            key=lambda vid: -vocab_by_id.get(vid, {}).get("total_occurrences", 0),
        )
        st.vocab_count = len(ep_occ)

        grammar_rows = extract_grammar_points(work_slug, ep, meta.id, ep_lines, ai)
        grammar_all.extend(grammar_rows)
        st.grammar_count = len(grammar_rows)
        if st.grammar_count < 8:
            st.warnings.append("语法点过少")

        sentence_rows = select_sentences(work_slug, ep, meta.id, ep_lines, ai)
        sentences_all.extend(sentence_rows)
        st.sentence_count = len(sentence_rows)
        if st.sentence_count < 15:
            st.warnings.append("跟读句过少")

        exercise_rows = generate_exercises(
            work_slug, ep, meta.id, ep_vocab_ids, vocab_by_id
        )
        exercises_all.extend(exercise_rows)
        st.exercise_count = len(exercise_rows)
        if st.exercise_count < 20:
            st.warnings.append("练习题过少")

        plans_all.append(
            build_episode_plan(
                work_slug,
                ep,
                meta.id,
                ep_vocab_ids,
                grammar_rows,
                sentence_rows,
                exercise_rows,
                vocab_by_id,
            )
        )
        episode_stats.append(st)

    vocab_csv = [{k: v for k, v in item.items() if not k.startswith("_")} for item in vocab_items]

    write_csv(
        OUTPUT_DIR / "learning_vocab_items.csv",
        [
            "id", "work_slug", "surface", "reading", "romaji", "meaning_zh", "pos",
            "jlpt_level", "suitable_handwriting", "suitable_shadowing",
            "anime_tone_note", "real_world_note", "total_occurrences", "episode_count",
        ],
        vocab_csv,
    )
    write_csv(
        OUTPUT_DIR / "learning_vocab_occurrences.csv",
        [
            "id", "work_slug", "episode_id", "episode", "vocab_item_id",
            "occurrence_count", "example_line_nos",
        ],
        vocab_occurrences,
    )
    write_csv(
        OUTPUT_DIR / "learning_grammar_points.csv",
        [
            "id", "work_slug", "episode_id", "episode", "pattern", "function_zh",
            "ja_example", "explanation_zh", "pragmatics_note", "real_world_note",
            "difficulty", "source_line_no", "sort_order",
        ],
        grammar_all,
    )
    write_csv(
        OUTPUT_DIR / "learning_sentences.csv",
        [
            "id", "work_slug", "episode_id", "episode", "ja_text", "reading", "romaji",
            "meaning_zh", "tone_tags", "difficulty", "recommended_shadowing",
            "source_line_no", "sort_order",
        ],
        sentences_all,
    )
    write_csv(
        OUTPUT_DIR / "learning_exercises.csv",
        [
            "id", "work_slug", "episode_id", "episode", "exercise_type", "prompt",
            "answer", "hint", "difficulty", "vocab_item_id", "sort_order",
        ],
        exercises_all,
    )
    write_csv(
        OUTPUT_DIR / "episode_learning_plans.csv",
        [
            "id", "work_slug", "episode_id", "episode", "vocab_item_ids",
            "handwriting_vocab_ids", "shadowing_sentence_ids", "grammar_point_ids",
            "exercise_ids", "notes",
        ],
        plans_all,
    )

    all_rows = vocab_csv + vocab_occurrences + grammar_all + sentences_all + exercises_all + plans_all
    dup_ids = check_duplicates(all_rows)

    generate_report(
        OUTPUT_DIR / "import_report.md",
        work_slug,
        work_name,
        episode_stats,
        vocab_csv,
        global_warnings,
        ai_mode,
        dup_ids,
    )

    print(f"Done. Output: {OUTPUT_DIR}")
    print(f"  vocab items: {len(vocab_csv)}")
    print(f"  episodes: {len(episode_stats)}")
    print(f"  grammar: {len(grammar_all)}")
    print(f"  sentences: {len(sentences_all)}")
    print(f"  exercises: {len(exercises_all)}")


if __name__ == "__main__":
    main()
