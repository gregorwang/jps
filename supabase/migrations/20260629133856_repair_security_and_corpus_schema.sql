-- Repair public API privileges, corpus table keys, and known Re:Zero sentence data issues.
-- The bulk subtitle re-import is intentionally kept in scripts/import-rezero-missing-subtitles.mjs.

do $$
declare
  r record;
begin
  for r in
    select c.relname
    from pg_class c
    join pg_namespace n on n.oid = c.relnamespace
    join information_schema.columns cols
      on cols.table_schema = n.nspname
     and cols.table_name = c.relname
     and cols.column_name = 'work_slug'
    where n.nspname = 'public' and c.relkind in ('r', 'p')
  loop
    execute format('update public.%I set work_slug = $1 where work_slug = $2', r.relname)
      using 're-zero', 'rezero';
  end loop;
end $$;

update public.subtitle_lines set language = 'ja' where language is null;

alter table public.episode_learning_plans add column if not exists updated_at timestamptz default now();

with suspect_texts as (
  select ja_text
  from public.learning_sentences
  where work_slug = 're-zero'
  group by ja_text
  having count(*) >= 3 and count(distinct meaning_zh) >= 3
), bad_rows as (
  select s.id
  from public.learning_sentences s
  join suspect_texts x using (ja_text)
  left join public.subtitle_lines l
    on l.work_slug = s.work_slug
   and l.episode = s.episode
   and l.line_no = s.source_line_no
  where s.work_slug = 're-zero'
    and coalesce(s.ja_text, '') <> coalesce(l.ja_text, '')
), changed_plans as (
  update public.episode_learning_plans p
  set shadowing_sentence_ids = coalesce((
    select jsonb_agg(elem.value order by elem.ordinality)
    from jsonb_array_elements_text(coalesce(p.shadowing_sentence_ids, '[]'::jsonb)) with ordinality as elem(value, ordinality)
    where not exists (select 1 from bad_rows b where b.id = elem.value)
  ), '[]'::jsonb)
  where exists (select 1 from bad_rows b where p.shadowing_sentence_ids @> to_jsonb(array[b.id]))
  returning p.id
)
delete from public.learning_sentences s
using bad_rows b
where s.id = b.id;

alter table public.works alter column id set not null;
alter table public.works alter column slug set not null;
do $$
begin
  if not exists (select 1 from pg_constraint where conrelid = 'public.works'::regclass and conname = 'works_pkey') then
    alter table public.works add constraint works_pkey primary key (id);
  end if;
  if not exists (select 1 from pg_constraint where conrelid = 'public.works'::regclass and conname = 'works_slug_key') then
    alter table public.works add constraint works_slug_key unique (slug);
  end if;
end $$;

alter table public.episodes alter column id set not null;
alter table public.episodes alter column work_slug set not null;
alter table public.episodes alter column episode set not null;
do $$
begin
  if not exists (select 1 from pg_constraint where conrelid = 'public.episodes'::regclass and conname = 'episodes_pkey') then
    alter table public.episodes add constraint episodes_pkey primary key (id);
  end if;
  if not exists (select 1 from pg_constraint where conrelid = 'public.episodes'::regclass and conname = 'episodes_work_slug_episode_key') then
    alter table public.episodes add constraint episodes_work_slug_episode_key unique (work_slug, episode);
  end if;
end $$;

alter table public.subtitle_chunks alter column id set not null;
alter table public.subtitle_chunks alter column work_slug set not null;
alter table public.subtitle_chunks alter column episode set not null;
alter table public.subtitle_chunks alter column chunk_no set not null;
alter table public.subtitle_chunks alter column language set not null;
do $$
begin
  if not exists (select 1 from pg_constraint where conrelid = 'public.subtitle_chunks'::regclass and conname = 'subtitle_chunks_pkey') then
    alter table public.subtitle_chunks add constraint subtitle_chunks_pkey primary key (id);
  end if;
  if not exists (select 1 from pg_constraint where conrelid = 'public.subtitle_chunks'::regclass and conname = 'subtitle_chunks_work_episode_chunk_language_key') then
    alter table public.subtitle_chunks add constraint subtitle_chunks_work_episode_chunk_language_key unique (work_slug, episode, chunk_no, language);
  end if;
end $$;

alter table public.subtitle_lines alter column work_slug set not null;
alter table public.subtitle_lines alter column episode set not null;
alter table public.subtitle_lines alter column line_no set not null;
alter table public.subtitle_lines alter column language set not null;
do $$
begin
  if not exists (select 1 from pg_constraint where conrelid = 'public.subtitle_lines'::regclass and conname = 'subtitle_lines_pkey') then
    alter table public.subtitle_lines add constraint subtitle_lines_pkey primary key (work_slug, episode, line_no, language);
  end if;
end $$;

create index if not exists learning_exercises_vocab_item_id_idx on public.learning_exercises (vocab_item_id);
create index if not exists learning_vocab_occurrences_vocab_item_id_idx on public.learning_vocab_occurrences (vocab_item_id);
create index if not exists subtitle_line_phenomena_phenomenon_key_idx on public.subtitle_line_phenomena (phenomenon_key);

do $$
begin
  if exists (
    select 1
    from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.proname = 'learning_set_updated_at'
  ) then
    alter function public.learning_set_updated_at() set search_path = public, pg_temp;
  end if;

  if exists (
    select 1
    from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.proname = 'set_learning_card_enrichments_updated_at'
  ) then
    alter function public.set_learning_card_enrichments_updated_at() set search_path = public, pg_temp;
  end if;

  if exists (
    select 1
    from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public' and p.proname = 'writing_practice_set_updated_at'
  ) then
    alter function public.writing_practice_set_updated_at() set search_path = public, pg_temp;
  end if;
end $$;

alter table public.works enable row level security;
alter table public.episodes enable row level security;
alter table public.subtitle_lines enable row level security;
alter table public.subtitle_chunks enable row level security;
alter table public.episode_learning_plans enable row level security;
alter table public.learning_vocab_items enable row level security;
alter table public.learning_vocab_occurrences enable row level security;
alter table public.learning_grammar_points enable row level security;
alter table public.learning_sentences enable row level security;
alter table public.learning_exercises enable row level security;
alter table public.learning_card_enrichments enable row level security;
alter table public.linguistic_exercise_drafts enable row level security;
alter table public.linguistic_phenomena enable row level security;
alter table public.character_language_profiles enable row level security;
alter table public.ai_interaction_history enable row level security;
alter table public.ai_result_cache enable row level security;
alter table public.app_sessions enable row level security;
alter table public.app_users enable row level security;
alter table public.linguistic_generation_batches enable row level security;
alter table public.sentence_correction_history enable row level security;
alter table public.subtitle_line_phenomena enable row level security;
alter table public.user_progress enable row level security;
alter table public.writing_practice_stats enable row level security;
alter table public.writing_practice_submissions enable row level security;

do $$
declare
  r record;
begin
  for r in
    select c.relname, c.relkind
    from pg_class c
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public'
      and c.relkind in ('r', 'p', 'v', 'm')
      and (c.relname like 'linguistic%seed%' or c.relname = 'linguistic_seed_probe')
    order by case when c.relkind in ('v', 'm') then 0 else 1 end, c.relname
  loop
    if r.relkind = 'v' then
      execute format('drop view if exists public.%I cascade', r.relname);
    elsif r.relkind = 'm' then
      execute format('drop materialized view if exists public.%I cascade', r.relname);
    else
      execute format('drop table if exists public.%I cascade', r.relname);
    end if;
  end loop;
end $$;

drop policy if exists "ai interaction public read" on public.ai_interaction_history;
drop policy if exists "ai interaction public write" on public.ai_interaction_history;
drop policy if exists "ai interaction public update" on public.ai_interaction_history;
drop policy if exists "ai cache public read" on public.ai_result_cache;
drop policy if exists "ai cache public write" on public.ai_result_cache;
drop policy if exists "ai cache public update" on public.ai_result_cache;
drop policy if exists "corrections public read" on public.sentence_correction_history;
drop policy if exists "corrections public write" on public.sentence_correction_history;
drop policy if exists "progress public read" on public.user_progress;
drop policy if exists "progress public write" on public.user_progress;
drop policy if exists "progress public update" on public.user_progress;
drop policy if exists "profiles public write" on public.character_language_profiles;
drop policy if exists "profiles public update" on public.character_language_profiles;

drop policy if exists "Public corpus read access" on public.works;
create policy "Public corpus read access" on public.works for select to anon, authenticated using (true);
drop policy if exists "Public corpus read access" on public.episodes;
create policy "Public corpus read access" on public.episodes for select to anon, authenticated using (true);
drop policy if exists "Public corpus read access" on public.subtitle_lines;
create policy "Public corpus read access" on public.subtitle_lines for select to anon, authenticated using (true);
drop policy if exists "Public corpus read access" on public.subtitle_chunks;
create policy "Public corpus read access" on public.subtitle_chunks for select to anon, authenticated using (true);

drop policy if exists "Public learning read access" on public.episode_learning_plans;
create policy "Public learning read access" on public.episode_learning_plans for select to anon, authenticated using (true);
drop policy if exists "Public learning read access" on public.learning_vocab_items;
create policy "Public learning read access" on public.learning_vocab_items for select to anon, authenticated using (true);
drop policy if exists "Public learning read access" on public.learning_vocab_occurrences;
create policy "Public learning read access" on public.learning_vocab_occurrences for select to anon, authenticated using (true);
drop policy if exists "Public learning read access" on public.learning_grammar_points;
create policy "Public learning read access" on public.learning_grammar_points for select to anon, authenticated using (true);
drop policy if exists "Public learning read access" on public.learning_sentences;
create policy "Public learning read access" on public.learning_sentences for select to anon, authenticated using (true);
drop policy if exists "Public learning read access" on public.learning_exercises;
create policy "Public learning read access" on public.learning_exercises for select to anon, authenticated using (true);

drop policy if exists "learning_card_enrichments_public_read_ready" on public.learning_card_enrichments;
create policy "learning_card_enrichments_public_read_ready" on public.learning_card_enrichments
  for select to anon, authenticated using (status = 'ready');
drop policy if exists "Published linguistic exercises are readable" on public.linguistic_exercise_drafts;
create policy "Published linguistic exercises are readable" on public.linguistic_exercise_drafts
  for select to anon, authenticated using (status = 'published');
drop policy if exists "read_linguistic_phenomena" on public.linguistic_phenomena;
create policy "read_linguistic_phenomena" on public.linguistic_phenomena for select to public using (true);
drop policy if exists "profiles public read" on public.character_language_profiles;
create policy "profiles public read" on public.character_language_profiles for select to anon, authenticated using (true);

revoke all privileges on all tables in schema public from anon, authenticated;
grant select on table
  public.works,
  public.episodes,
  public.subtitle_lines,
  public.subtitle_chunks,
  public.episode_learning_plans,
  public.learning_vocab_items,
  public.learning_vocab_occurrences,
  public.learning_grammar_points,
  public.learning_sentences,
  public.learning_exercises,
  public.learning_card_enrichments,
  public.linguistic_exercise_drafts,
  public.linguistic_phenomena,
  public.character_language_profiles
  to anon, authenticated;

do $$
declare
  table_name text;
begin
  foreach table_name in array array[
    'ai_interaction_history',
    'ai_result_cache',
    'app_sessions',
    'app_users',
    'linguistic_generation_batches',
    'sentence_correction_history',
    'subtitle_line_phenomena',
    'user_progress',
    'writing_practice_stats',
    'writing_practice_submissions'
  ] loop
    execute format('drop policy if exists "deny public access" on public.%I', table_name);
    execute format('create policy "deny public access" on public.%I for all to anon, authenticated using (false) with check (false)', table_name);
  end loop;
end $$;
