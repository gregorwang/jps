import { execFileSync } from "node:child_process"
import { resolve } from "node:path"

function fail(message) {
  throw new Error(message)
}

function quote(value) {
  return `'${String(value).replaceAll("'", "''")}'`
}

function prepare(kind, args) {
  const raw = execFileSync(
    process.execPath,
    [resolve("scripts/prepare-foundation-transport.mjs"), kind, ...args],
    { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 },
  )
  return JSON.parse(raw)
}

const topicRecord = `
    id text,
    curriculum_version text,
    domain text,
    module_id text,
    sort_order integer,
    title_ja text,
    title_zh text,
    short_definition_zh text,
    beginner_explanation_zh text,
    deep_explanation_zh text,
    caution_note_zh text,
    prerequisite_topic_ids text[],
    learning_objectives_json jsonb,
    example_spec_json jsonb,
    tags_json jsonb,
    content_version integer,
    quality_score smallint,
    reviewer_note text,
    generator_agent text,
    reviewer_agent text,
    source_file text,
    review_report_file text,
    review_sha256 text
`

const packRecord = `
    id text,
    curriculum_version text,
    batch_no smallint,
    title_zh text,
    description_zh text,
    topic_count integer,
    question_count integer,
    domain_quotas_json jsonb,
    content_version integer,
    quality_score smallint,
    reviewer_note text,
    generator_agent text,
    reviewer_agent text,
    source_file text,
    review_report_file text,
    review_sha256 text
`

const questionRecord = `
    id text,
    pack_id text,
    topic_id text,
    curriculum_version text,
    stage text,
    question_type text,
    source_kind text,
    stimulus_json jsonb,
    prompt_zh text,
    options_json jsonb,
    answer_json jsonb,
    hint_zh text,
    explanation_zh text,
    deep_explanation_zh text,
    caution_note_zh text,
    wrong_explanations_json jsonb,
    transfer_example_ja text,
    transfer_explanation_zh text,
    difficulty smallint,
    tags_json jsonb,
    sort_order integer,
    content_version integer,
    quality_score smallint,
    reviewer_note text,
    generator_agent text,
    reviewer_agent text,
    source_file text,
    review_report_file text,
    review_sha256 text
`

function topicTargetJson(alias) {
  return `jsonb_build_object(
    'id', ${alias}.id,
    'curriculum_version', ${alias}.curriculum_version,
    'domain', ${alias}.domain,
    'module_id', ${alias}.module_id,
    'sort_order', ${alias}.sort_order,
    'title_ja', ${alias}.title_ja,
    'title_zh', ${alias}.title_zh,
    'short_definition_zh', ${alias}.short_definition_zh,
    'beginner_explanation_zh', ${alias}.beginner_explanation_zh,
    'deep_explanation_zh', ${alias}.deep_explanation_zh,
    'caution_note_zh', ${alias}.caution_note_zh,
    'prerequisite_topic_ids', to_jsonb(${alias}.prerequisite_topic_ids),
    'learning_objectives_json', ${alias}.learning_objectives_json,
    'example_spec_json', ${alias}.example_spec_json,
    'tags_json', ${alias}.tags_json,
    'content_version', ${alias}.content_version,
    'quality_score', ${alias}.quality_score,
    'reviewer_note', ${alias}.reviewer_note,
    'generator_agent', ${alias}.generator_agent,
    'reviewer_agent', ${alias}.reviewer_agent,
    'source_file', ${alias}.source_file,
    'review_report_file', ${alias}.review_report_file,
    'review_sha256', ${alias}.review_sha256::text
  )`
}

function packTargetJson(alias) {
  return `jsonb_build_object(
    'id', ${alias}.id,
    'curriculum_version', ${alias}.curriculum_version,
    'batch_no', ${alias}.batch_no,
    'title_zh', ${alias}.title_zh,
    'description_zh', ${alias}.description_zh,
    'topic_count', ${alias}.topic_count,
    'question_count', ${alias}.question_count,
    'domain_quotas_json', ${alias}.domain_quotas_json,
    'content_version', ${alias}.content_version,
    'quality_score', ${alias}.quality_score,
    'reviewer_note', ${alias}.reviewer_note,
    'generator_agent', ${alias}.generator_agent,
    'reviewer_agent', ${alias}.reviewer_agent,
    'source_file', ${alias}.source_file,
    'review_report_file', ${alias}.review_report_file,
    'review_sha256', ${alias}.review_sha256::text
  )`
}

function questionTargetJson(alias) {
  return `jsonb_build_object(
    'id', ${alias}.id,
    'pack_id', ${alias}.pack_id,
    'topic_id', ${alias}.topic_id,
    'curriculum_version', ${alias}.curriculum_version,
    'stage', ${alias}.stage,
    'question_type', ${alias}.question_type,
    'source_kind', ${alias}.source_kind,
    'stimulus_json', ${alias}.stimulus_json,
    'prompt_zh', ${alias}.prompt_zh,
    'options_json', ${alias}.options_json,
    'answer_json', ${alias}.answer_json,
    'hint_zh', ${alias}.hint_zh,
    'explanation_zh', ${alias}.explanation_zh,
    'deep_explanation_zh', ${alias}.deep_explanation_zh,
    'caution_note_zh', ${alias}.caution_note_zh,
    'wrong_explanations_json', ${alias}.wrong_explanations_json,
    'transfer_example_ja', ${alias}.transfer_example_ja,
    'transfer_explanation_zh', ${alias}.transfer_explanation_zh,
    'difficulty', ${alias}.difficulty,
    'tags_json', ${alias}.tags_json,
    'sort_order', ${alias}.sort_order,
    'content_version', ${alias}.content_version,
    'quality_score', ${alias}.quality_score,
    'reviewer_note', ${alias}.reviewer_note,
    'generator_agent', ${alias}.generator_agent,
    'reviewer_agent', ${alias}.reviewer_agent,
    'source_file', ${alias}.source_file,
    'review_report_file', ${alias}.review_report_file,
    'review_sha256', ${alias}.review_sha256::text
  )`
}

function forbiddenKeyGuard(documentSql) {
  const keys = [
    "workSlug",
    "work_slug",
    "work_id",
    "episode",
    "episode_id",
    "source_line",
    "source_line_no",
    "source_id",
    "anime",
    "character",
    "character_id",
  ]
  return keys
    .map((key) => `${documentSql} @? '$.**.${key}'`)
    .join("\n      or ")
}

function buildTopicsSql(prepared) {
  if (
    prepared.kind !== "topics"
    || prepared.expected !== 60
    || prepared.generatorAgent === prepared.reviewerAgent
  ) {
    fail("prepared topics metadata failed integrity checks")
  }
  const payloadSql = `convert_from(decode(${quote(prepared.base64)}, 'base64'), 'utf8')::jsonb`
  const expectedQuotas = JSON.stringify({
    phonology_writing: 8,
    morphology: 10,
    syntax: 13,
    semantics: 8,
    pragmatics_discourse: 12,
    sociolinguistics: 5,
    historical_grammaticalization: 4,
  })

  return `
do $guarded$
declare
  candidate_doc jsonb := ${payloadSql};
  expected integer := 60;
  actual integer;
  publish_time timestamptz := clock_timestamp();
begin
  perform set_config('lock_timeout', '10s', true);
  perform pg_advisory_xact_lock(hashtextextended('foundation-v1-topics', 0));
  lock table maintenance.linguistic_foundation_v1_manifest in share row exclusive mode;
  lock table public.linguistic_foundation_topics in share row exclusive mode;

  if jsonb_typeof(candidate_doc) <> 'array'
      or jsonb_array_length(candidate_doc) <> expected then
    raise exception 'topic candidate count guard failed';
  end if;

  select count(distinct x.id) into actual
  from jsonb_to_recordset(candidate_doc) as x(${topicRecord});
  if actual <> expected then
    raise exception 'topic unique ID guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from jsonb_to_recordset(candidate_doc) as x(${topicRecord})
  where x.generator_agent <> ${quote(prepared.generatorAgent)}
     or x.reviewer_agent <> ${quote(prepared.reviewerAgent)}
     or x.generator_agent = x.reviewer_agent
     or x.quality_score < 95
     or x.source_file <> ${quote(prepared.sourceFile)}
     or x.review_report_file <> ${quote(prepared.reviewReportFile)}
     or x.review_sha256 <> ${quote(prepared.reviewSha256)};
  if actual <> 0 then
    raise exception 'topic review provenance guard failed for % rows', actual;
  end if;

  select count(*) into actual
  from (
    select x.domain, count(*) as domain_count
    from jsonb_to_recordset(candidate_doc) as x(${topicRecord})
    group by x.domain
  ) q
  full join jsonb_each_text(${quote(expectedQuotas)}::jsonb) expected_domain
    on expected_domain.key = q.domain
  where q.domain_count is distinct from expected_domain.value::integer;
  if actual <> 0 then
    raise exception 'topic domain quota guard failed for % domains', actual;
  end if;

  select count(*) into actual
  from jsonb_to_recordset(candidate_doc) as x(${topicRecord})
  cross join lateral unnest(x.prerequisite_topic_ids) prerequisite(id)
  where not exists (
    select 1
    from jsonb_to_recordset(candidate_doc) as y(${topicRecord})
    where y.id = prerequisite.id
  );
  if actual <> 0 then
    raise exception 'topic prerequisite existence guard failed for % references', actual;
  end if;

  with recursive
  topic_rows as (
    select x.id, x.prerequisite_topic_ids
    from jsonb_to_recordset(candidate_doc) as x(${topicRecord})
  ),
  edges as (
    select t.id, prerequisite.id as dependency_id
    from topic_rows t
    cross join lateral unnest(t.prerequisite_topic_ids) prerequisite(id)
  ),
  walk(root_id, node_id, path, has_cycle) as (
    select e.id, e.dependency_id, array[e.id, e.dependency_id], e.id = e.dependency_id
    from edges e
    union all
    select w.root_id,
           e.dependency_id,
           w.path || e.dependency_id,
           e.dependency_id = any(w.path)
    from walk w
    join edges e on e.id = w.node_id
    where not w.has_cycle
  )
  select count(*) into actual from walk where has_cycle;
  if actual <> 0 then
    raise exception 'topic prerequisite DAG guard failed: % cycles', actual;
  end if;

  if ${forbiddenKeyGuard("candidate_doc")} then
    raise exception 'topic corpus-coupling key guard failed';
  end if;

  select count(*) into actual
  from public.linguistic_foundation_topics t
  join jsonb_to_recordset(candidate_doc) as x(${topicRecord}) on x.id = t.id;
  if actual <> 0 then
    raise exception 'topic target history overlap guard failed for % IDs', actual;
  end if;

  select count(*) into actual
  from maintenance.linguistic_foundation_v1_manifest m
  join jsonb_to_recordset(candidate_doc) as x(${topicRecord})
    on m.entity_type = 'topic' and m.entity_id = x.id;
  if actual <> 0 then
    raise exception 'topic manifest history overlap guard failed for % IDs', actual;
  end if;

  insert into public.linguistic_foundation_topics (
    id, curriculum_version, domain, module_id, sort_order,
    title_ja, title_zh, short_definition_zh,
    beginner_explanation_zh, deep_explanation_zh, caution_note_zh,
    prerequisite_topic_ids, learning_objectives_json, example_spec_json, tags_json,
    content_version, status, quality_score,
    generator_agent, reviewer_agent, reviewer_note,
    source_file, content_sha256, review_report_file, review_sha256, reviewed_at
  )
  select
    x.id, x.curriculum_version, x.domain, x.module_id, x.sort_order,
    x.title_ja, x.title_zh, x.short_definition_zh,
    x.beginner_explanation_zh, x.deep_explanation_zh, x.caution_note_zh,
    x.prerequisite_topic_ids, x.learning_objectives_json, x.example_spec_json, x.tags_json,
    x.content_version, 'approved', x.quality_score,
    x.generator_agent, x.reviewer_agent, x.reviewer_note,
    x.source_file,
    encode(extensions.digest(convert_to(to_jsonb(x)::text, 'UTF8'), 'sha256'), 'hex'),
    x.review_report_file, x.review_sha256, publish_time
  from jsonb_to_recordset(candidate_doc) as x(${topicRecord});

  insert into maintenance.linguistic_foundation_v1_manifest (
    entity_type, entity_id, curriculum_version, pack_id,
    candidate_values, candidate_sha256,
    generator_agent, reviewer_agent, quality_score,
    source_file, review_report_file, review_sha256, status
  )
  select
    'topic', x.id, x.curriculum_version, null,
    to_jsonb(x),
    encode(extensions.digest(convert_to(to_jsonb(x)::text, 'UTF8'), 'sha256'), 'hex'),
    x.generator_agent, x.reviewer_agent, x.quality_score,
    x.source_file, x.review_report_file, x.review_sha256, 'approved'
  from jsonb_to_recordset(candidate_doc) as x(${topicRecord});

  select count(*) into actual
  from maintenance.linguistic_foundation_v1_manifest m
  join public.linguistic_foundation_topics t
    on m.entity_type = 'topic' and m.entity_id = t.id
  join jsonb_to_recordset(candidate_doc) as x(${topicRecord}) on x.id = t.id
  where m.candidate_values = ${topicTargetJson("t")}
    and m.candidate_sha256 = t.content_sha256;
  if actual <> expected then
    raise exception 'topic approved exact-match guard failed: expected %, got %', expected, actual;
  end if;

  update public.linguistic_foundation_topics t
  set status = 'published', published_at = publish_time
  from jsonb_to_recordset(candidate_doc) as x(${topicRecord})
  where t.id = x.id;

  update maintenance.linguistic_foundation_v1_manifest m
  set status = 'applied', applied_at = publish_time
  from jsonb_to_recordset(candidate_doc) as x(${topicRecord})
  where m.entity_type = 'topic'
    and m.entity_id = x.id
    and m.status = 'approved';

  select count(*) into actual
  from maintenance.linguistic_foundation_v1_manifest m
  join public.linguistic_foundation_topics t
    on m.entity_type = 'topic' and m.entity_id = t.id
  join jsonb_to_recordset(candidate_doc) as x(${topicRecord}) on x.id = t.id
  where m.status = 'applied'
    and t.status = 'published'
    and m.candidate_values = ${topicTargetJson("t")}
    and m.candidate_sha256 = t.content_sha256;
  if actual <> expected then
    raise exception 'topic applied exact-match guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from maintenance.linguistic_foundation_v1_manifest m
  left join public.linguistic_foundation_topics t
    on m.entity_type = 'topic' and m.entity_id = t.id
  where m.entity_type = 'topic'
    and m.status = 'applied'
    and (
      t.id is null
      or t.status <> 'published'
      or m.candidate_values is distinct from ${topicTargetJson("t")}
      or m.candidate_sha256 is distinct from t.content_sha256
    );
  if actual <> 0 then
    raise exception 'global applied topic drift guard failed for % rows', actual;
  end if;
end
$guarded$;
`.trim()
}

function buildPackSql(prepared) {
  if (
    prepared.kind !== "pack"
    || prepared.expected !== 80
    || prepared.generatorAgent === prepared.reviewerAgent
  ) {
    fail("prepared pack metadata failed integrity checks")
  }
  const payloadSql = `convert_from(decode(${quote(prepared.base64)}, 'base64'), 'utf8')::jsonb`

  return `
do $guarded$
declare
  candidate_doc jsonb := ${payloadSql};
  expected_questions integer := 80;
  expected_topics integer := 20;
  actual integer;
  actual_domain_quotas jsonb;
  candidate_pack_id text;
  publish_time timestamptz := clock_timestamp();
begin
  perform set_config('lock_timeout', '10s', true);
  candidate_pack_id := candidate_doc #>> '{pack,id}';
  if candidate_pack_id is null or btrim(candidate_pack_id) = '' then
    raise exception 'pack ID guard failed';
  end if;
  perform pg_advisory_xact_lock(hashtextextended(candidate_pack_id, 0));
  lock table maintenance.linguistic_foundation_v1_manifest in share row exclusive mode;
  lock table public.linguistic_foundation_question_packs in share row exclusive mode;
  lock table public.linguistic_foundation_questions in share row exclusive mode;

  if jsonb_typeof(candidate_doc) <> 'object'
      or jsonb_typeof(candidate_doc -> 'pack') <> 'object'
      or jsonb_typeof(candidate_doc -> 'topic_ids') <> 'array'
      or jsonb_array_length(candidate_doc -> 'topic_ids') <> expected_topics
      or jsonb_typeof(candidate_doc -> 'questions') <> 'array'
      or jsonb_array_length(candidate_doc -> 'questions') <> expected_questions then
    raise exception 'pack candidate shape/count guard failed';
  end if;

  select count(distinct q.id) into actual
  from jsonb_to_recordset(candidate_doc -> 'questions') as q(${questionRecord});
  if actual <> expected_questions then
    raise exception 'question unique ID guard failed: expected %, got %', expected_questions, actual;
  end if;

  select count(distinct topic_id) into actual
  from jsonb_array_elements_text(candidate_doc -> 'topic_ids') topic_id;
  if actual <> expected_topics then
    raise exception 'pack topic unique ID guard failed: expected %, got %', expected_topics, actual;
  end if;

  select count(*) into actual
  from jsonb_to_record(candidate_doc -> 'pack') as p(${packRecord})
  where p.id <> candidate_pack_id
     or p.curriculum_version <> 'foundation-v1'
     or p.batch_no not between 1 and 3
     or p.id <> 'foundation-v1-wave-' || p.batch_no::text
     or p.topic_count <> expected_topics
     or p.question_count <> expected_questions
     or p.generator_agent <> ${quote(prepared.generatorAgent)}
     or p.reviewer_agent <> ${quote(prepared.reviewerAgent)}
     or p.generator_agent = p.reviewer_agent
     or p.quality_score < 95
     or p.source_file <> ${quote(prepared.sourceFile)}
     or p.review_report_file <> ${quote(prepared.reviewReportFile)}
     or p.review_sha256 <> ${quote(prepared.reviewSha256)};
  if actual <> 0 then
    raise exception 'pack review provenance guard failed';
  end if;

  select count(*) into actual
  from jsonb_to_recordset(candidate_doc -> 'questions') as q(${questionRecord})
  where q.pack_id <> candidate_pack_id
     or q.curriculum_version <> candidate_doc #>> '{pack,curriculum_version}'
     or q.generator_agent <> ${quote(prepared.generatorAgent)}
     or q.reviewer_agent <> ${quote(prepared.reviewerAgent)}
     or q.generator_agent = q.reviewer_agent
     or q.quality_score < 95
     or q.source_file <> ${quote(prepared.sourceFile)}
     or q.review_report_file <> ${quote(prepared.reviewReportFile)}
     or q.review_sha256 <> ${quote(prepared.reviewSha256)};
  if actual <> 0 then
    raise exception 'question review provenance guard failed for % rows', actual;
  end if;

  select count(*) into actual
  from jsonb_to_recordset(candidate_doc -> 'questions') as q(${questionRecord})
  where not exists (
    select 1
    from jsonb_array_elements_text(candidate_doc -> 'topic_ids') topic_id
    where topic_id = q.topic_id
  );
  if actual <> 0 then
    raise exception 'question outside pack topic set guard failed for % rows', actual;
  end if;

  perform t.id
  from public.linguistic_foundation_topics t
  join jsonb_array_elements_text(candidate_doc -> 'topic_ids') topic_id
    on topic_id = t.id
  order by t.id
  for share of t;

  select count(*) into actual
  from public.linguistic_foundation_topics t
  join jsonb_array_elements_text(candidate_doc -> 'topic_ids') topic_id
    on topic_id = t.id
  where t.status = 'published'
    and t.curriculum_version = candidate_doc #>> '{pack,curriculum_version}';
  if actual <> expected_topics then
    raise exception
      'published topic curriculum/existence guard failed: expected %, got %',
      expected_topics, actual;
  end if;

  select coalesce(
    jsonb_object_agg(domain_count.domain, domain_count.topic_count),
    '{}'::jsonb
  )
  into actual_domain_quotas
  from (
    select t.domain, count(*) as topic_count
    from public.linguistic_foundation_topics t
    join jsonb_array_elements_text(candidate_doc -> 'topic_ids') topic_id
      on topic_id = t.id
    where t.status = 'published'
      and t.curriculum_version = candidate_doc #>> '{pack,curriculum_version}'
    group by t.domain
  ) domain_count;
  if actual_domain_quotas
      is distinct from candidate_doc #> '{pack,domain_quotas_json}' then
    raise exception
      'pack domain quota guard failed: declared %, database %',
      candidate_doc #> '{pack,domain_quotas_json}', actual_domain_quotas;
  end if;

  select count(*) into actual
  from (
    select q.topic_id,
           count(*) as question_count,
           count(distinct q.stage) as stage_count,
           array_agg(q.stage order by q.stage) as stages
    from jsonb_to_recordset(candidate_doc -> 'questions') as q(${questionRecord})
    group by q.topic_id
  ) topic_questions
  where question_count <> 4
     or stage_count <> 4
     or stages <> array['F1', 'F2', 'F3', 'F4']::text[];
  if actual <> 0 then
    raise exception 'F1-F4 topic coverage guard failed for % topics', actual;
  end if;

  select count(*) into actual
  from (
    select q.answer_json ->> 'option_id' as answer_id, count(*) as answer_count
    from jsonb_to_recordset(candidate_doc -> 'questions') as q(${questionRecord})
    group by q.answer_json ->> 'option_id'
  ) answer_counts
  full join (
    values ('A', 20), ('B', 20), ('C', 20), ('D', 20)
  ) expected(answer_id, answer_count)
    using (answer_id)
  where answer_counts.answer_count is distinct from expected.answer_count;
  if actual <> 0 then
    raise exception 'A/B/C/D answer balance guard failed for % positions', actual;
  end if;

  select count(*) into actual
  from jsonb_to_recordset(candidate_doc -> 'questions') as q(${questionRecord})
  where (
    select count(*)
    from jsonb_array_elements(q.options_json) option_item
    where jsonb_typeof(option_item) = 'object'
      and option_item ? 'id'
      and jsonb_typeof(option_item -> 'id') = 'string'
      and btrim(option_item ->> 'id') <> ''
      and option_item ? 'text'
      and jsonb_typeof(option_item -> 'text') = 'string'
      and btrim(option_item ->> 'text') <> ''
  ) <> 4
  or (
    select count(distinct option_item ->> 'id')
    from jsonb_array_elements(q.options_json) option_item
  ) <> 4
  or (
    select array_agg(option_item ->> 'id' order by option_item ->> 'id')
    from jsonb_array_elements(q.options_json) option_item
  ) <> array['A', 'B', 'C', 'D']::text[]
  or not exists (
    select 1
    from jsonb_array_elements(q.options_json) option_item
    where option_item ->> 'id' = q.answer_json ->> 'option_id'
  )
  or (
    select count(*)
    from jsonb_each(q.wrong_explanations_json) wrong_item
    where wrong_item.key in ('A', 'B', 'C', 'D')
      and wrong_item.key <> q.answer_json ->> 'option_id'
      and jsonb_typeof(wrong_item.value) = 'string'
      and btrim(wrong_item.value #>> '{}') <> ''
  ) <> 3
  or exists (
    select 1
    from jsonb_array_elements(q.options_json) option_item
    where option_item ->> 'id' <> q.answer_json ->> 'option_id'
      and not (q.wrong_explanations_json ? (option_item ->> 'id'))
  );
  if actual <> 0 then
    raise exception 'question option/answer/explanation guard failed for % rows', actual;
  end if;

  if ${forbiddenKeyGuard("candidate_doc")} then
    raise exception 'pack corpus-coupling key guard failed';
  end if;

  select count(*) into actual
  from public.linguistic_foundation_question_packs p
  where p.id = candidate_pack_id;
  if actual <> 0 then
    raise exception 'pack target history overlap guard failed';
  end if;

  select count(*) into actual
  from public.linguistic_foundation_questions q
  join jsonb_to_recordset(candidate_doc -> 'questions') as x(${questionRecord})
    on x.id = q.id;
  if actual <> 0 then
    raise exception 'question target history overlap guard failed for % IDs', actual;
  end if;

  select count(*) into actual
  from maintenance.linguistic_foundation_v1_manifest m
  where (m.entity_type = 'pack' and m.entity_id = candidate_pack_id)
     or (
       m.entity_type = 'question'
       and exists (
         select 1
         from jsonb_to_recordset(candidate_doc -> 'questions') as x(${questionRecord})
         where x.id = m.entity_id
       )
     );
  if actual <> 0 then
    raise exception 'pack/question manifest history overlap guard failed for % rows', actual;
  end if;

  insert into public.linguistic_foundation_question_packs (
    id, curriculum_version, batch_no, title_zh, description_zh,
    topic_count, question_count, domain_quotas_json,
    content_version, status, quality_score,
    generator_agent, reviewer_agent, reviewer_note,
    source_file, content_sha256, review_report_file, review_sha256, reviewed_at
  )
  select
    p.id, p.curriculum_version, p.batch_no, p.title_zh, p.description_zh,
    p.topic_count, p.question_count, p.domain_quotas_json,
    p.content_version, 'approved', p.quality_score,
    p.generator_agent, p.reviewer_agent, p.reviewer_note,
    p.source_file,
    encode(extensions.digest(convert_to(to_jsonb(p)::text, 'UTF8'), 'sha256'), 'hex'),
    p.review_report_file, p.review_sha256, publish_time
  from jsonb_to_record(candidate_doc -> 'pack') as p(${packRecord});

  insert into public.linguistic_foundation_questions (
    id, pack_id, topic_id, curriculum_version, stage,
    question_type, source_kind, stimulus_json, prompt_zh,
    options_json, answer_json, hint_zh, explanation_zh,
    deep_explanation_zh, caution_note_zh, wrong_explanations_json,
    transfer_example_ja, transfer_explanation_zh,
    difficulty, tags_json, sort_order, content_version,
    status, quality_score,
    generator_agent, reviewer_agent, reviewer_note,
    source_file, content_sha256, review_report_file, review_sha256, reviewed_at
  )
  select
    q.id, q.pack_id, q.topic_id, q.curriculum_version, q.stage,
    q.question_type, q.source_kind, q.stimulus_json, q.prompt_zh,
    q.options_json, q.answer_json, q.hint_zh, q.explanation_zh,
    q.deep_explanation_zh, q.caution_note_zh, q.wrong_explanations_json,
    q.transfer_example_ja, q.transfer_explanation_zh,
    q.difficulty, q.tags_json, q.sort_order, q.content_version,
    'approved', q.quality_score,
    q.generator_agent, q.reviewer_agent, q.reviewer_note,
    q.source_file,
    encode(extensions.digest(convert_to(to_jsonb(q)::text, 'UTF8'), 'sha256'), 'hex'),
    q.review_report_file, q.review_sha256, publish_time
  from jsonb_to_recordset(candidate_doc -> 'questions') as q(${questionRecord});

  insert into maintenance.linguistic_foundation_v1_manifest (
    entity_type, entity_id, curriculum_version, pack_id,
    candidate_values, candidate_sha256,
    generator_agent, reviewer_agent, quality_score,
    source_file, review_report_file, review_sha256, status
  )
  select
    'pack', p.id, p.curriculum_version, p.id,
    to_jsonb(p),
    encode(extensions.digest(convert_to(to_jsonb(p)::text, 'UTF8'), 'sha256'), 'hex'),
    p.generator_agent, p.reviewer_agent, p.quality_score,
    p.source_file, p.review_report_file, p.review_sha256, 'approved'
  from jsonb_to_record(candidate_doc -> 'pack') as p(${packRecord});

  insert into maintenance.linguistic_foundation_v1_manifest (
    entity_type, entity_id, curriculum_version, pack_id,
    candidate_values, candidate_sha256,
    generator_agent, reviewer_agent, quality_score,
    source_file, review_report_file, review_sha256, status
  )
  select
    'question', q.id, q.curriculum_version, q.pack_id,
    to_jsonb(q),
    encode(extensions.digest(convert_to(to_jsonb(q)::text, 'UTF8'), 'sha256'), 'hex'),
    q.generator_agent, q.reviewer_agent, q.quality_score,
    q.source_file, q.review_report_file, q.review_sha256, 'approved'
  from jsonb_to_recordset(candidate_doc -> 'questions') as q(${questionRecord});

  select count(*) into actual
  from maintenance.linguistic_foundation_v1_manifest m
  join public.linguistic_foundation_question_packs p
    on m.entity_type = 'pack' and m.entity_id = p.id
  where p.id = candidate_pack_id
    and m.candidate_values = ${packTargetJson("p")}
    and m.candidate_sha256 = p.content_sha256;
  if actual <> 1 then
    raise exception 'pack approved exact-match guard failed';
  end if;

  select count(*) into actual
  from maintenance.linguistic_foundation_v1_manifest m
  join public.linguistic_foundation_questions q
    on m.entity_type = 'question' and m.entity_id = q.id
  join jsonb_to_recordset(candidate_doc -> 'questions') as x(${questionRecord}) on x.id = q.id
  where m.candidate_values = ${questionTargetJson("q")}
    and m.candidate_sha256 = q.content_sha256;
  if actual <> expected_questions then
    raise exception 'question approved exact-match guard failed: expected %, got %',
      expected_questions, actual;
  end if;

  update public.linguistic_foundation_questions q
  set status = 'published', published_at = publish_time
  from jsonb_to_recordset(candidate_doc -> 'questions') as x(${questionRecord})
  where q.id = x.id;

  update public.linguistic_foundation_question_packs p
  set status = 'published', published_at = publish_time
  where p.id = candidate_pack_id;

  update maintenance.linguistic_foundation_v1_manifest m
  set status = 'applied', applied_at = publish_time
  where m.pack_id = candidate_pack_id
    and m.entity_type in ('pack', 'question')
    and m.status = 'approved';

  select count(*) into actual
  from public.linguistic_foundation_questions q
  join maintenance.linguistic_foundation_v1_manifest m
    on m.entity_type = 'question' and m.entity_id = q.id
  where q.pack_id = candidate_pack_id
    and q.status = 'published'
    and m.status = 'applied'
    and m.candidate_values = ${questionTargetJson("q")}
    and m.candidate_sha256 = q.content_sha256;
  if actual <> expected_questions then
    raise exception 'question applied exact-match guard failed: expected %, got %',
      expected_questions, actual;
  end if;

  select count(*) into actual
  from public.linguistic_foundation_question_packs p
  join maintenance.linguistic_foundation_v1_manifest m
    on m.entity_type = 'pack' and m.entity_id = p.id
  where p.id = candidate_pack_id
    and p.status = 'published'
    and m.status = 'applied'
    and m.candidate_values = ${packTargetJson("p")}
    and m.candidate_sha256 = p.content_sha256;
  if actual <> 1 then
    raise exception 'pack applied exact-match guard failed';
  end if;

  select count(*) into actual
  from maintenance.linguistic_foundation_v1_manifest m
  left join public.linguistic_foundation_question_packs p
    on m.entity_type = 'pack' and m.entity_id = p.id
  where m.entity_type = 'pack'
    and m.status = 'applied'
    and (
      p.id is null
      or p.status <> 'published'
      or m.candidate_values is distinct from ${packTargetJson("p")}
      or m.candidate_sha256 is distinct from p.content_sha256
    );
  if actual <> 0 then
    raise exception 'global applied pack drift guard failed for % rows', actual;
  end if;

  select count(*) into actual
  from maintenance.linguistic_foundation_v1_manifest m
  left join public.linguistic_foundation_questions q
    on m.entity_type = 'question' and m.entity_id = q.id
  where m.entity_type = 'question'
    and m.status = 'applied'
    and (
      q.id is null
      or q.status <> 'published'
      or m.candidate_values is distinct from ${questionTargetJson("q")}
      or m.candidate_sha256 is distinct from q.content_sha256
    );
  if actual <> 0 then
    raise exception 'global applied question drift guard failed for % rows', actual;
  end if;
end
$guarded$;
`.trim()
}

const [kind, ...args] = process.argv.slice(2)
let sql
if (kind === "topics" && args.length === 3) {
  sql = buildTopicsSql(prepare("topics", args))
} else if (kind === "pack" && args.length === 4) {
  sql = buildPackSql(prepare("pack", args))
} else {
  fail(
    "usage:\n"
      + "  node scripts/build-foundation-publish-sql.mjs topics "
      + "<generation.json> <review-overlay.json> <cross-review.md>\n"
      + "  node scripts/build-foundation-publish-sql.mjs pack "
      + "<generation.json> <review-overlay.json> <cross-review.md> <topics-effective.json>",
  )
}

process.stdout.write(`${sql}\n`)
