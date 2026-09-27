-- Hand-checked vocab cards (archive-content-sources/vocab-cards-v1, written back by push_cloud.py).
-- The pre-rewrite rows are kept in learning_vocab_items_backup_20260927.
create table if not exists public.learning_vocab_items_backup_20260927 as table public.learning_vocab_items;

alter table public.learning_vocab_items
  add column if not exists lemma text not null default '',
  add column if not exists lemma_reading text not null default '',
  add column if not exists is_study_word boolean not null default true;
alter table public.learning_vocab_items_backup_20260927 enable row level security;
