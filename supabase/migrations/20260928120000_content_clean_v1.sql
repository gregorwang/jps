-- Rewritten grammar points / line translations (archive-content-sources/content-clean-v1, written back by push_cloud.py,
-- which also derives the exercise answers from them). Pre-rewrite rows are kept in the *_backup_20260928 tables.
-- is_active = false retires a row: the worker keeps its first-30-by-sort_order window and drops retired rows inside it.
create table if not exists public.learning_grammar_points_backup_20260928 as table public.learning_grammar_points;
create table if not exists public.learning_sentences_backup_20260928 as table public.learning_sentences;
create table if not exists public.learning_exercises_backup_20260928 as table public.learning_exercises;
alter table public.learning_grammar_points_backup_20260928 enable row level security;
alter table public.learning_sentences_backup_20260928 enable row level security;
alter table public.learning_exercises_backup_20260928 enable row level security;

alter table public.learning_grammar_points
  add column if not exists example_zh text not null default '',
  add column if not exists is_active boolean not null default true,
  -- learning_set_updated_at() was already attached to this table, which had no updated_at: every update failed.
  add column if not exists updated_at timestamptz not null default now();
alter table public.learning_sentences add column if not exists is_active boolean not null default true;
alter table public.learning_exercises add column if not exists is_active boolean not null default true;

-- Pipeline metadata objects that ended up in tone_tags.
update public.learning_sentences set tone_tags = '[]'::jsonb where jsonb_typeof(tone_tags) = 'object';
