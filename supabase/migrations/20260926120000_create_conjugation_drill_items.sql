-- 活用道場: anime-line drill items for basic conjugation / auxiliaries / te-form helpers.
-- One row = one grammar point exemplified by one audio-backed subtitle line.
-- Questions are generated on the client from these annotations.
create table if not exists public.conjugation_drill_items (
  id text primary key,
  work_slug text not null default 're-zero',
  point_id text not null,
  point_title_zh text not null,
  group_zh text not null,
  sentence_id text not null,
  episode_label text not null default '',
  start_time text,
  ja_text text not null,
  reading text,
  span_start integer not null check (span_start >= 0),
  span_end integer not null check (span_end > span_start),
  target text not null,
  head_json jsonb not null default '{}'::jsonb,
  zh text not null default '',
  formula text not null default '',
  sense text not null default '',
  note text not null default '',
  audio_url text not null,
  sort_order integer not null default 0,
  status text not null default 'published' check (status in ('draft', 'published', 'archived')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists conjugation_drill_items_point_idx
  on public.conjugation_drill_items (point_id, sort_order);

alter table public.conjugation_drill_items enable row level security;

drop policy if exists conjugation_drill_items_public_read on public.conjugation_drill_items;
create policy conjugation_drill_items_public_read
  on public.conjugation_drill_items
  for select
  to anon, authenticated
  using (status = 'published');
