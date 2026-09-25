begin;

create table public.linguistic_foundation_topics (
  id text primary key
    check (id ~ '^[a-z0-9]+(?:_[a-z0-9]+)*$'),
  curriculum_version text not null default 'foundation-v1'
    check (btrim(curriculum_version) <> ''),
  domain text not null
    check (
      domain in (
        'phonology_writing',
        'morphology',
        'syntax',
        'semantics',
        'pragmatics_discourse',
        'sociolinguistics',
        'historical_grammaticalization'
      )
    ),
  module_id text not null
    check (module_id ~ '^[a-z0-9]+(?:_[a-z0-9]+)*$'),
  sort_order integer not null
    check (sort_order >= 0),
  title_ja text not null
    check (btrim(title_ja) <> ''),
  title_zh text not null
    check (btrim(title_zh) <> ''),
  short_definition_zh text not null
    check (btrim(short_definition_zh) <> ''),
  beginner_explanation_zh text not null
    check (btrim(beginner_explanation_zh) <> ''),
  deep_explanation_zh text not null
    check (btrim(deep_explanation_zh) <> ''),
  caution_note_zh text not null
    check (btrim(caution_note_zh) <> ''),
  prerequisite_topic_ids text[] not null default '{}'::text[],
  learning_objectives_json jsonb not null,
  example_spec_json jsonb not null,
  tags_json jsonb not null default '[]'::jsonb,
  content_version integer not null default 1
    check (content_version > 0),
  status text not null default 'draft'
    check (status in ('draft', 'reviewing', 'approved', 'published', 'archived')),
  quality_score smallint not null default 0
    check (quality_score between 0 and 100),
  generator_agent text not null
    check (btrim(generator_agent) <> ''),
  reviewer_agent text,
  reviewer_note text,
  source_file text not null
    check (btrim(source_file) <> ''),
  content_sha256 character(64) not null
    check (content_sha256 ~ '^[0-9a-f]{64}$'),
  review_report_file text,
  review_sha256 character(64),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  reviewed_at timestamptz,
  published_at timestamptz,
  archived_at timestamptz,
  constraint linguistic_foundation_topics_prerequisite_not_self
    check (not (id = any(prerequisite_topic_ids))),
  constraint linguistic_foundation_topics_objectives_shape
    check (
      jsonb_typeof(learning_objectives_json) = 'object'
      and learning_objectives_json ?& array['F1_zh', 'F2_zh', 'F3_zh', 'F4_zh']
    ),
  constraint linguistic_foundation_topics_example_spec_shape
    check (jsonb_typeof(example_spec_json) = 'object'),
  constraint linguistic_foundation_topics_tags_shape
    check (jsonb_typeof(tags_json) = 'array'),
  constraint linguistic_foundation_topics_reviewer_is_independent
    check (
      reviewer_agent is null
      or (
        btrim(reviewer_agent) <> ''
        and reviewer_agent <> generator_agent
      )
    ),
  constraint linguistic_foundation_topics_review_audit
    check (
      status not in ('approved', 'published', 'archived')
      or (
        reviewer_agent is not null
        and reviewed_at is not null
        and quality_score >= 95
        and review_report_file is not null
        and btrim(review_report_file) <> ''
        and review_sha256 is not null
        and review_sha256 ~ '^[0-9a-f]{64}$'
      )
    ),
  constraint linguistic_foundation_topics_lifecycle
    check (
      (status in ('draft', 'reviewing', 'approved') and published_at is null and archived_at is null)
      or (status = 'published' and published_at is not null and archived_at is null)
      or (status = 'archived' and published_at is not null and archived_at is not null)
    ),
  unique (curriculum_version, sort_order),
  unique (curriculum_version, domain, title_zh)
);

create table public.linguistic_foundation_question_packs (
  id text primary key
    check (id ~ '^[a-z0-9]+(?:[-_][a-z0-9]+)*$'),
  curriculum_version text not null default 'foundation-v1'
    check (btrim(curriculum_version) <> ''),
  batch_no smallint not null
    check (batch_no > 0),
  title_zh text not null
    check (btrim(title_zh) <> ''),
  description_zh text not null
    check (btrim(description_zh) <> ''),
  topic_count integer not null
    check (topic_count > 0),
  question_count integer not null
    check (question_count > 0 and question_count = topic_count * 4),
  domain_quotas_json jsonb not null
    check (jsonb_typeof(domain_quotas_json) = 'object'),
  content_version integer not null default 1
    check (content_version > 0),
  status text not null default 'draft'
    check (status in ('draft', 'reviewing', 'approved', 'published', 'archived')),
  quality_score smallint not null default 0
    check (quality_score between 0 and 100),
  generator_agent text not null
    check (btrim(generator_agent) <> ''),
  reviewer_agent text,
  reviewer_note text,
  source_file text not null
    check (btrim(source_file) <> ''),
  content_sha256 character(64) not null
    check (content_sha256 ~ '^[0-9a-f]{64}$'),
  review_report_file text,
  review_sha256 character(64),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  reviewed_at timestamptz,
  published_at timestamptz,
  archived_at timestamptz,
  constraint linguistic_foundation_packs_reviewer_is_independent
    check (
      reviewer_agent is null
      or (
        btrim(reviewer_agent) <> ''
        and reviewer_agent <> generator_agent
      )
    ),
  constraint linguistic_foundation_packs_review_audit
    check (
      status not in ('approved', 'published', 'archived')
      or (
        reviewer_agent is not null
        and reviewed_at is not null
        and quality_score >= 95
        and review_report_file is not null
        and btrim(review_report_file) <> ''
        and review_sha256 is not null
        and review_sha256 ~ '^[0-9a-f]{64}$'
      )
    ),
  constraint linguistic_foundation_packs_lifecycle
    check (
      (status in ('draft', 'reviewing', 'approved') and published_at is null and archived_at is null)
      or (status = 'published' and published_at is not null and archived_at is null)
      or (status = 'archived' and published_at is not null and archived_at is not null)
    ),
  unique (curriculum_version, batch_no)
);

create table public.linguistic_foundation_questions (
  id text primary key
    check (id ~ '^[a-z0-9]+(?:[-_][a-z0-9]+)*$'),
  pack_id text not null
    references public.linguistic_foundation_question_packs(id)
    on update cascade
    on delete restrict,
  topic_id text not null
    references public.linguistic_foundation_topics(id)
    on update cascade
    on delete restrict,
  curriculum_version text not null default 'foundation-v1'
    check (btrim(curriculum_version) <> ''),
  stage text not null
    check (stage in ('F1', 'F2', 'F3', 'F4')),
  question_type text not null
    check (
      question_type in (
        'single_choice',
        'morphology_analysis',
        'syntax_relation',
        'contrast_choice',
        'kuuki_yomi'
      )
    ),
  source_kind text not null
    check (
      source_kind in (
        'original_sentence',
        'minimal_pair',
        'constructed_dialogue',
        'metalinguistic'
      )
    ),
  stimulus_json jsonb not null,
  prompt_zh text not null
    check (btrim(prompt_zh) <> ''),
  options_json jsonb not null,
  answer_json jsonb not null,
  hint_zh text not null
    check (btrim(hint_zh) <> ''),
  explanation_zh text not null
    check (btrim(explanation_zh) <> ''),
  deep_explanation_zh text not null
    check (btrim(deep_explanation_zh) <> ''),
  caution_note_zh text not null
    check (btrim(caution_note_zh) <> ''),
  wrong_explanations_json jsonb not null,
  transfer_example_ja text,
  transfer_explanation_zh text,
  difficulty smallint not null
    check (difficulty between 1 and 4),
  tags_json jsonb not null default '[]'::jsonb,
  sort_order integer not null
    check (sort_order >= 0),
  content_version integer not null default 1
    check (content_version > 0),
  status text not null default 'draft'
    check (status in ('draft', 'reviewing', 'approved', 'published', 'archived')),
  quality_score smallint not null default 0
    check (quality_score between 0 and 100),
  generator_agent text not null
    check (btrim(generator_agent) <> ''),
  reviewer_agent text,
  reviewer_note text,
  source_file text not null
    check (btrim(source_file) <> ''),
  content_sha256 character(64) not null
    check (content_sha256 ~ '^[0-9a-f]{64}$'),
  review_report_file text,
  review_sha256 character(64),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  reviewed_at timestamptz,
  published_at timestamptz,
  archived_at timestamptz,
  constraint linguistic_foundation_questions_stimulus_shape
    check (
      jsonb_typeof(stimulus_json) = 'object'
      and stimulus_json ? 'kind'
      and jsonb_typeof(stimulus_json -> 'kind') = 'string'
      and stimulus_json ->> 'kind' in ('sentence', 'dialogue', 'contrast', 'metalinguistic')
    ),
  constraint linguistic_foundation_questions_options_shape
    check (jsonb_typeof(options_json) = 'array' and jsonb_array_length(options_json) = 4),
  constraint linguistic_foundation_questions_answer_shape
    check (
      jsonb_typeof(answer_json) = 'object'
      and answer_json ? 'option_id'
      and jsonb_typeof(answer_json -> 'option_id') = 'string'
    ),
  constraint linguistic_foundation_questions_wrong_explanations_shape
    check (jsonb_typeof(wrong_explanations_json) = 'object'),
  constraint linguistic_foundation_questions_tags_shape
    check (jsonb_typeof(tags_json) = 'array'),
  constraint linguistic_foundation_questions_stage_difficulty_match
    check (
      (stage = 'F1' and difficulty = 1)
      or (stage = 'F2' and difficulty = 2)
      or (stage = 'F3' and difficulty = 3)
      or (stage = 'F4' and difficulty = 4)
    ),
  constraint linguistic_foundation_questions_transfer_pair
    check (
      (transfer_example_ja is null and transfer_explanation_zh is null)
      or (
        transfer_example_ja is not null
        and btrim(transfer_example_ja) <> ''
        and transfer_explanation_zh is not null
        and btrim(transfer_explanation_zh) <> ''
      )
    ),
  constraint linguistic_foundation_questions_reviewer_is_independent
    check (
      reviewer_agent is null
      or (
        btrim(reviewer_agent) <> ''
        and reviewer_agent <> generator_agent
      )
    ),
  constraint linguistic_foundation_questions_review_audit
    check (
      status not in ('approved', 'published', 'archived')
      or (
        reviewer_agent is not null
        and reviewed_at is not null
        and quality_score >= 95
        and review_report_file is not null
        and btrim(review_report_file) <> ''
        and review_sha256 is not null
        and review_sha256 ~ '^[0-9a-f]{64}$'
      )
    ),
  constraint linguistic_foundation_questions_lifecycle
    check (
      (status in ('draft', 'reviewing', 'approved') and published_at is null and archived_at is null)
      or (status = 'published' and published_at is not null and archived_at is null)
      or (status = 'archived' and published_at is not null and archived_at is not null)
    ),
  unique (pack_id, topic_id, stage),
  unique (pack_id, sort_order)
);

create table public.linguistic_phenomenon_topic_map (
  phenomenon_key text not null
    references public.linguistic_phenomena(phenomenon_key)
    on update cascade
    on delete restrict,
  topic_id text not null
    references public.linguistic_foundation_topics(id)
    on update cascade
    on delete restrict,
  relation_type text not null
    check (relation_type in ('direct', 'broader', 'narrower', 'related', 'contrastive')),
  mapping_note_zh text,
  confidence_score smallint not null
    check (confidence_score between 0 and 100),
  status text not null default 'draft'
    check (status in ('draft', 'reviewing', 'approved', 'published', 'archived')),
  quality_score smallint not null default 0
    check (quality_score between 0 and 100),
  generator_agent text not null
    check (btrim(generator_agent) <> ''),
  reviewer_agent text,
  reviewer_note text,
  source_file text not null
    check (btrim(source_file) <> ''),
  content_sha256 character(64) not null
    check (content_sha256 ~ '^[0-9a-f]{64}$'),
  review_report_file text,
  review_sha256 character(64),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  reviewed_at timestamptz,
  published_at timestamptz,
  archived_at timestamptz,
  constraint linguistic_phenomenon_topic_map_pkey
    primary key (phenomenon_key, topic_id),
  constraint linguistic_phenomenon_topic_map_reviewer_is_independent
    check (
      reviewer_agent is null
      or (
        btrim(reviewer_agent) <> ''
        and reviewer_agent <> generator_agent
      )
    ),
  constraint linguistic_phenomenon_topic_map_review_audit
    check (
      status not in ('approved', 'published', 'archived')
      or (
        reviewer_agent is not null
        and reviewed_at is not null
        and quality_score >= 95
        and review_report_file is not null
        and btrim(review_report_file) <> ''
        and review_sha256 is not null
        and review_sha256 ~ '^[0-9a-f]{64}$'
      )
    ),
  constraint linguistic_phenomenon_topic_map_lifecycle
    check (
      (status in ('draft', 'reviewing', 'approved') and published_at is null and archived_at is null)
      or (status = 'published' and published_at is not null and archived_at is null)
      or (status = 'archived' and published_at is not null and archived_at is not null)
    )
);

create index linguistic_foundation_topics_published_order_idx
  on public.linguistic_foundation_topics (domain, sort_order, id)
  where status = 'published';

create index linguistic_foundation_topics_module_idx
  on public.linguistic_foundation_topics (module_id, sort_order, id);

create index linguistic_foundation_packs_published_order_idx
  on public.linguistic_foundation_question_packs (curriculum_version, batch_no, id)
  where status = 'published';

create index linguistic_foundation_questions_published_pack_order_idx
  on public.linguistic_foundation_questions (pack_id, sort_order, id)
  where status = 'published';

create index linguistic_foundation_questions_topic_stage_idx
  on public.linguistic_foundation_questions (topic_id, stage);

create index linguistic_phenomenon_topic_map_topic_idx
  on public.linguistic_phenomenon_topic_map (topic_id, relation_type, phenomenon_key)
  where status = 'published';

create trigger linguistic_foundation_topics_set_updated_at
before update on public.linguistic_foundation_topics
for each row execute function public.learning_set_updated_at();

create trigger linguistic_foundation_packs_set_updated_at
before update on public.linguistic_foundation_question_packs
for each row execute function public.learning_set_updated_at();

create trigger linguistic_foundation_questions_set_updated_at
before update on public.linguistic_foundation_questions
for each row execute function public.learning_set_updated_at();

create trigger linguistic_phenomenon_topic_map_set_updated_at
before update on public.linguistic_phenomenon_topic_map
for each row execute function public.learning_set_updated_at();

alter table public.linguistic_foundation_topics enable row level security;
alter table public.linguistic_foundation_topics force row level security;
alter table public.linguistic_foundation_question_packs enable row level security;
alter table public.linguistic_foundation_question_packs force row level security;
alter table public.linguistic_foundation_questions enable row level security;
alter table public.linguistic_foundation_questions force row level security;
alter table public.linguistic_phenomenon_topic_map enable row level security;
alter table public.linguistic_phenomenon_topic_map force row level security;

revoke all privileges on table public.linguistic_foundation_topics
  from public, anon, authenticated, service_role;
revoke all privileges on table public.linguistic_foundation_question_packs
  from public, anon, authenticated, service_role;
revoke all privileges on table public.linguistic_foundation_questions
  from public, anon, authenticated, service_role;
revoke all privileges on table public.linguistic_phenomenon_topic_map
  from public, anon, authenticated, service_role;

grant select on table
  public.linguistic_foundation_topics,
  public.linguistic_foundation_question_packs,
  public.linguistic_foundation_questions,
  public.linguistic_phenomenon_topic_map
to anon, authenticated;

grant select, insert, update, delete on table
  public.linguistic_foundation_topics,
  public.linguistic_foundation_question_packs,
  public.linguistic_foundation_questions,
  public.linguistic_phenomenon_topic_map
to service_role;

create policy linguistic_foundation_topics_public_read
on public.linguistic_foundation_topics
for select
to anon, authenticated
using (status = 'published');

create policy linguistic_foundation_packs_public_read
on public.linguistic_foundation_question_packs
for select
to anon, authenticated
using (status = 'published');

create policy linguistic_foundation_questions_public_read
on public.linguistic_foundation_questions
for select
to anon, authenticated
using (
  status = 'published'
  and exists (
    select 1
    from public.linguistic_foundation_question_packs p
    where p.id = linguistic_foundation_questions.pack_id
      and p.status = 'published'
  )
  and exists (
    select 1
    from public.linguistic_foundation_topics t
    where t.id = linguistic_foundation_questions.topic_id
      and t.status = 'published'
  )
);

create policy linguistic_phenomenon_topic_map_public_read
on public.linguistic_phenomenon_topic_map
for select
to anon, authenticated
using (
  status = 'published'
  and exists (
    select 1
    from public.linguistic_foundation_topics t
    where t.id = linguistic_phenomenon_topic_map.topic_id
      and t.status = 'published'
  )
);

comment on table public.linguistic_foundation_topics is
  'Canonical, corpus-independent Japanese linguistics curriculum topics.';
comment on table public.linguistic_foundation_question_packs is
  'Independently generated and reviewed publication units for the foundation curriculum.';
comment on table public.linguistic_foundation_questions is
  'Corpus-independent Japanese linguistics questions; deliberately has no anime or subtitle provenance columns.';
comment on table public.linguistic_phenomenon_topic_map is
  'Reviewed bridge from corpus-specific linguistic phenomena to canonical foundation topics.';

commit;
