-- Read-only, reproducible audit for the 2026-07-27 regeneration run.

with applied as (
  select *
  from maintenance.regen_20260727_manifest
  where review_status = 'applied'
),
exercise_matches as (
  select m.row_id
  from applied m
  join public.learning_exercises e
    on m.table_name = 'public.learning_exercises'
   and e.id = m.row_id
  where e.prompt is not distinct from m.candidate_values ->> 'prompt'
    and e.answer is not distinct from m.candidate_values ->> 'answer'
    and e.hint is not distinct from m.candidate_values ->> 'hint'
),
linguistic_matches as (
  select m.row_id
  from applied m
  join public.learning_card_enrichments e
    on m.table_name = 'public.learning_card_enrichments'
   and e.id = m.row_id
  where e.linguistic_payload
          is not distinct from m.candidate_values -> 'linguistic_payload'
    and e.linguistic_prompt_version
          is not distinct from m.candidate_values ->> 'linguistic_prompt_version'
    and e.linguistic_quality_score
          is not distinct from (m.candidate_values ->> 'linguistic_quality_score')::integer
    and e.linguistic_status
          is not distinct from m.candidate_values ->> 'linguistic_status'
)
select
  (select count(*) from maintenance.regen_20260727_manifest)
    as manifest_total,
  (select count(*) from applied) as applied_total,
  (select count(*) from applied where table_name = 'public.learning_exercises')
    as applied_exercises,
  (select count(*) from exercise_matches) as exact_exercises,
  (select count(*) from applied where table_name = 'public.learning_card_enrichments')
    as applied_linguistic,
  (select count(*) from linguistic_matches) as exact_linguistic,
  (select count(*) from public.learning_exercises where answer ~ '^[A-D]$')
    as remaining_letter_answers,
  (select count(*) from public.learning_exercises where answer like '用于%')
    as remaining_meta_answers,
  (select count(*) from maintenance.regen_20260727_manifest where review_status = 'rolled_back')
    as rolled_back_manifest_rows,
  (select count(*) from maintenance.regen_20260727_manifest
   where review_status not in ('applied', 'rolled_back'))
    as unfinished_manifest_rows,
  (select count(*) from maintenance.regen_20260727_learning_exercises_backup)
    as exercise_backup_rows,
  (select count(*) from maintenance.regen_20260727_card_enrichments_backup)
    as linguistic_backup_rows,
  (
    select count(*)
    from applied m
    join public.learning_exercises e
      on m.table_name = 'public.learning_exercises'
     and e.id = m.row_id
    where e.answer ~ '^[A-D]$'
       or e.answer like '用于%'
  ) as applied_placeholder_residue;

select kind, episode, count(*) as remaining
from (
  select 'letter'::text as kind, episode
  from public.learning_exercises
  where answer ~ '^[A-D]$'
  union all
  select 'meta'::text as kind, episode
  from public.learning_exercises
  where answer like '用于%'
) remaining
group by kind, episode
order by kind, episode;

-- This intentionally broad audit is reproducible. It counts old cards containing
-- a string leaf of at least 12 characters that occurs on at least three old cards.
-- It is broader than the historical 1,228-card fixed cohort and is not directly
-- comparable with that cohort's arithmetic remainder.
with old_cards as (
  select e.id, e.linguistic_payload
  from public.learning_card_enrichments e
  where e.linguistic_prompt_version = 'linguistic-card-v1'
    and e.linguistic_payload is not null
    and not exists (
      select 1
      from maintenance.regen_20260727_manifest m
      where m.table_name = 'public.learning_card_enrichments'
        and m.row_id = e.id
        and m.review_status = 'applied'
    )
),
leaves as (
  select o.id, value #>> '{}' as leaf
  from old_cards o
  cross join lateral jsonb_path_query(
    o.linguistic_payload,
    '$.** ? (@.type() == "string")'
  ) value
),
repeated as (
  select leaf
  from leaves
  where char_length(leaf) >= 12
  group by leaf
  having count(distinct id) >= 3
)
select
  (select count(*) from old_cards) as remaining_old_prompt_cards,
  count(distinct l.id) as broad_old_template_cards,
  count(distinct r.leaf) as repeated_long_leaves
from leaves l
join repeated r using (leaf);
