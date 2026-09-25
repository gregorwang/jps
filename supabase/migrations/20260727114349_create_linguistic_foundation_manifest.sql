begin;

create schema if not exists maintenance;

create table maintenance.linguistic_foundation_v1_manifest (
  entity_type text not null
    check (entity_type in ('topic', 'pack', 'question', 'phenomenon_map')),
  entity_id text not null
    check (btrim(entity_id) <> ''),
  curriculum_version text not null
    check (btrim(curriculum_version) <> ''),
  pack_id text,
  candidate_values jsonb not null
    check (jsonb_typeof(candidate_values) = 'object'),
  candidate_sha256 character(64) not null
    check (candidate_sha256 ~ '^[0-9a-f]{64}$'),
  generator_agent text not null
    check (btrim(generator_agent) <> ''),
  reviewer_agent text not null
    check (btrim(reviewer_agent) <> '' and reviewer_agent <> generator_agent),
  quality_score smallint not null
    check (quality_score between 95 and 100),
  source_file text not null
    check (btrim(source_file) <> ''),
  review_report_file text not null
    check (btrim(review_report_file) <> ''),
  review_sha256 character(64) not null
    check (review_sha256 ~ '^[0-9a-f]{64}$'),
  status text not null default 'approved'
    check (status in ('approved', 'applied', 'rolled_back')),
  created_at timestamptz not null default now(),
  applied_at timestamptz,
  rolled_back_at timestamptz,
  error_message text,
  primary key (entity_type, entity_id),
  constraint linguistic_foundation_manifest_pack_reference_shape
    check (
      (entity_type in ('pack', 'question') and pack_id is not null and btrim(pack_id) <> '')
      or (entity_type in ('topic', 'phenomenon_map') and pack_id is null)
    ),
  constraint linguistic_foundation_manifest_lifecycle
    check (
      (status = 'approved' and applied_at is null and rolled_back_at is null)
      or (status = 'applied' and applied_at is not null and rolled_back_at is null)
      or (status = 'rolled_back' and rolled_back_at is not null)
    )
);

create index linguistic_foundation_manifest_pack_idx
  on maintenance.linguistic_foundation_v1_manifest (pack_id, entity_type, entity_id);

revoke all privileges on table maintenance.linguistic_foundation_v1_manifest
  from public, anon, authenticated, service_role;

grant select, insert, update, delete
  on table maintenance.linguistic_foundation_v1_manifest
  to service_role;

comment on table maintenance.linguistic_foundation_v1_manifest is
  'Immutable candidate snapshots and review provenance for atomic foundation curriculum publication.';

commit;
