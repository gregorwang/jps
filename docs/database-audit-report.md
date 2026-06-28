# Database Audit Report

Audit date: 2026-06-28
Project ref: `qoatvdvbuleamyzsaldp`
Method: Direct read-only Supabase Management API query via `database/query`; MCP was not used.

## Scope

The audit scanned public database tables for:

- high-repeat fields and dominant values
- likely duplicated content
- natural-key duplicate records
- inconsistent `work_slug` values
- high-null columns
- leftover seed/probe tables

No database writes were performed.

## Summary

The main data-quality risks are not ordinary enum-style repetition. The highest-priority issues are a small set of likely sentence text/translation mismatches, inconsistent `work_slug` values, and many small seed/probe tables left in `public`.

Core content tables did not show duplicated natural keys, so there is no broad evidence of repeated full-table import duplication.

## High Priority Findings

### 1. `learning_sentences` contains likely Japanese/Chinese mismatch groups

Query pattern:

- grouped by identical `ja_text`
- required at least 3 rows
- required at least 3 distinct `meaning_zh` values

Result:

- 3 suspicious groups
- 21 rows involved
- 18 extra repeated rows beyond the first row per group
- largest group size: 12

Examples:

| `ja_text` | Rows | Distinct Chinese meanings | Notes |
|---|---:|---:|---|
| `死んじゃえ 死んじゃえ 死んじゃえ 死んじゃえ` | 12 | 7 | Same Japanese text maps to meanings such as `再继续下去的话 就太强迫你了`, `很可惜 我不会死`, and `去死吧 去死吧 去死吧 去死吧`. This looks misaligned. |
| `断じて 呪いなどという 訳の分からないものに―` | 5 | 3 | Several rows map to unrelated Chinese lines around the same episode. |
| `それは とてもうれしいです` | 4 | 4 | Appears in multiple episode/line positions with different meanings. Some may be legitimate repeats, but this needs source-line verification. |

Recommended action:

1. Rebuild these rows from `subtitle_lines` using `work_slug + episode + source_line_no`.
2. Add a validation script that flags identical `ja_text` with high `meaning_zh` variance inside the same episode.
3. Review whether `learning_sentences` should prefer source-line identity over text identity for dedupe decisions.

### 2. `work_slug` values are inconsistent

Expected canonical value appears to be `re-zero`, but `rezero` also exists.

Observed distribution:

| Table | Value | Count |
|---|---|---:|
| `ai_interaction_history` | `rezero` | 27 |
| `ai_interaction_history` | `re-zero` | 1 |
| `ai_interaction_history` | `NULL` | 18 |
| `ai_result_cache` | `rezero` | 16 |
| `ai_result_cache` | `re-zero` | 1 |
| `ai_result_cache` | `NULL` | 111 |
| `user_progress` | `rezero` | 3 |
| `user_progress` | `re-zero` | 213 |

Risk:

- cache misses
- split user progress
- incomplete history filtering
- confusing analytics

Recommended action:

1. Normalize `rezero` to `re-zero`.
2. Add a foreign key or check constraint where practical.
3. Add application-level normalization before writes.

## Medium Priority Findings

### 3. Many seed/probe tables remain in `public`

There are many tiny tables matching names such as:

- `linguistic_exercise_seed_v1_part*`
- `linguistic_phenomena_seed_v*`
- `linguistic_seed_probe`

Most contain only 1-6 rows. Examples:

| Table pattern | Typical row count |
|---|---:|
| `linguistic_exercise_seed_v1_part*` | 1-6 |
| `linguistic_phenomena_seed_v*` | 2-7 |
| `linguistic_seed_probe` | 1 |

Risk:

- schema noise
- accidental API exposure
- confusing future migrations and audits

Recommended action:

1. Move seed/probe tables to an internal/archive schema, or export then drop them.
2. Keep production-facing tables in `public` only when they are intended API surface.

### 4. `updated_at` values look like batch import timestamps

Suspicious one-value timestamp fields:

| Table | Column | Rows | Distinct values | Top value |
|---|---|---:|---:|---|
| `learning_exercises` | `updated_at` | 14,427 | 1 | `2026-06-27 23:54:53.799885+00` |
| `learning_vocab_items` | `updated_at` | 3,770 | 1 | `2026-06-27 23:56:53.127368+00` |

Risk:

- `updated_at` is not useful for row-level freshness
- downstream sync or cache invalidation may treat a batch import as individual updates

Recommended action:

Clarify whether these fields mean "last imported at" or actual row update time. If they mean import time, consider renaming or adding a separate `imported_at`.

### 5. High-null columns

Columns with at least 20 rows and at least 50% nulls:

| Table | Column | Rows | Null rows | Null ratio |
|---|---|---:|---:|---:|
| `learning_sentences` | `romaji` | 8,586 | 8,174 | 95.20% |
| `ai_result_cache` | `episode` | 140 | 125 | 89.29% |
| `ai_result_cache` | `work_slug` | 140 | 111 | 79.29% |
| `learning_sentences` | `difficulty` | 8,586 | 5,581 | 65.00% |
| `learning_exercises` | `vocab_item_id` | 14,427 | 8,917 | 61.81% |
| `ai_interaction_history` | `source_id` | 57 | 34 | 59.65% |

Recommended action:

Decide whether each field is optional by design. If yes, document it in code/schema. If not, backfill and add write-time validation.

## Lower Priority / Expected Repetition

Several fields repeat heavily but appear expected for enum, status, source, or corpus-scope columns:

| Table | Column | Dominant value | Ratio |
|---|---|---|---:|
| `subtitle_lines` | `language` | `ja` | 100% of non-null |
| `learning_card_enrichments` | `model` | `gpt-5.5-thinking` | 100% |
| `learning_card_enrichments` | `linguistic_prompt_version` | `linguistic-card-v1` | 100% |
| `linguistic_exercise_drafts` | `status` | `published` | 100% |
| `subtitle_line_phenomena` | `status` | `draft` | 100% |
| `episodes` | `usable_as_main_corpus` | `true` | 100% |

These are not suspicious by themselves, but they are candidates for constraints or enums.

## Repeated Template Content

The audit found template-like repeated content in learning fields:

| Table | Column | Example | Count |
|---|---|---|---:|
| `learning_grammar_points` | `real_world_note` | `适合做单句精读和语法理解题。` | 240 |
| `learning_grammar_points` | `pragmatics_note` | `V9：语法例句来自原始 SRT 短句，避免长段拼接。` | 240 |
| `learning_exercises` | `hint` | `V10慢读：先听情绪，再确认句意。` | 436 |
| `learning_exercises` | `answer` | `用于本集慢读跟读的完整台词。` | 144 |
| `learning_vocab_items` | `anime_tone_note` | episode-level repeated tone notes | 100+ per group |

Risk:

- lower content quality
- repeated explanations reduce learning value
- version/template notes may leak into user-facing copy

Recommended action:

Review whether these fields should contain item-specific content. If not, move generic template notes to metadata.

## Natural-Key Duplicate Checks

No duplicate natural keys were found in the core checks below:

| Check | Duplicate groups | Extra duplicate rows |
|---|---:|---:|
| `subtitle_lines`: `work_slug + episode + line_no + language` | 0 | 0 |
| `learning_sentences`: `work_slug + episode + source_line_no + ja_text + meaning_zh` | 0 | 0 |
| `learning_vocab_items`: `work_slug + surface + reading + meaning_zh` | 0 | 0 |
| `learning_grammar_points`: `work_slug + episode + pattern + ja_example` | 0 | 0 |
| `learning_exercises`: `work_slug + episode + exercise_type + prompt + answer` | 0 | 0 |

## Table Size Snapshot

Largest public tables:

| Table | Rows |
|---|---:|
| `subtitle_lines` | 38,571 |
| `learning_exercises` | 14,427 |
| `learning_sentences` | 8,586 |
| `learning_vocab_occurrences` | 7,765 |
| `learning_vocab_items` | 3,770 |
| `learning_grammar_points` | 2,966 |
| `learning_card_enrichments` | 1,585 |
| `subtitle_chunks` | 1,030 |
| `linguistic_exercise_drafts` | 494 |
| `linguistic_phenomena` | 426 |

## Recommended Fix Order

1. Fix the 3 suspicious `learning_sentences` mismatch groups from `subtitle_lines`.
2. Normalize `work_slug` values and enforce canonical values.
3. Archive or drop leftover seed/probe tables from `public`.
4. Decide whether high-null fields are intentionally optional; backfill or validate accordingly.
5. Rename or reinterpret batch-style `updated_at` fields if they are not row update timestamps.
6. Remove template/version notes from user-facing learning content where appropriate.

## Security Note

The database audit used an access token supplied for this task. Rotate that token after use.
