import { execFileSync } from "node:child_process";

const projectRoot = new URL("../", import.meta.url);

const configs = {
  meta9: {
    kind: "exercise",
    chunkSize: 20,
    tableName: "public.learning_exercises",
    placeholderSql: "e.answer like '用于%'",
    generatorAgent: "/root/frozen_generator_meta_batch9",
    reviewerAgent: "/root/review_meta_batch9",
    batchId: (chunk) => `regen-20260727-exercises-meta-b9-p${chunk}`,
  },
  meta10: {
    kind: "exercise",
    chunkSize: 21,
    tableName: "public.learning_exercises",
    placeholderSql: "e.answer like '用于%'",
    generatorAgent: "/root/review_linguistic_batch11",
    reviewerAgent: "/root/review_meta_batch10",
    batchId: (chunk) => `regen-20260727-exercises-meta-b10-p${chunk}`,
  },
  linguistic11: {
    kind: "linguistic",
    chunkSize: 10,
    tableName: "public.learning_card_enrichments",
    generatorAgent: "/root/frozen_generator_linguistic_batch11",
    reviewerAgent: "/root/review_linguistic_batch11",
    batchId: (chunk) => `regen-20260727-linguistic-b11-p${chunk}`,
  },
  linguistic12: {
    kind: "linguistic",
    chunkSize: 10,
    tableName: "public.learning_card_enrichments",
    generatorAgent: "/root/review_letter_batches35_37",
    reviewerAgent: "/root/review_linguistic_batch12",
    batchId: (chunk) => `regen-20260727-linguistic-b12-p${chunk}`,
  },
  letter35_37: {
    kind: "exercise",
    chunkSize: 40,
    tableName: "public.learning_exercises",
    placeholderSql: "e.answer ~ '^[A-D]$'",
    generatorAgent: "/root/frozen_generator_letter_batches35_37",
    reviewerAgent: "/root/review_letter_batches35_37",
    batchId: (chunk) => `regen-20260727-exercises-letter-b${34 + chunk}`,
  },
  letter38_40: {
    kind: "exercise",
    chunkSize: 40,
    tableName: "public.learning_exercises",
    placeholderSql: "e.answer ~ '^[A-D]$'",
    generatorAgent: "/root/review_meta_batch9",
    reviewerAgent: "/root/review_letter_batches38_40",
    batchId: (chunk) => `regen-20260727-exercises-letter-b${37 + chunk}`,
  },
};

function fail(message) {
  throw new Error(message);
}

function quote(value) {
  return `'${String(value).replaceAll("'", "''")}'`;
}

const [group, chunkText] = process.argv.slice(2);
const config = configs[group];
const chunk = Number(chunkText);
if (!config || !Number.isInteger(chunk) || chunk < 1) {
  fail(
    `usage: node archive-content-sources/rezero-exercise-regen-2026-07-27/build_guarded_apply_sql.mjs <${Object.keys(configs).join("|")}> <chunk-number>`,
  );
}
if (config.generatorAgent === config.reviewerAgent) {
  fail(`${group}: generator and reviewer agents must be different`);
}

const preparedRaw = execFileSync(
  process.execPath,
  ["archive-content-sources/rezero-exercise-regen-2026-07-27/prepare_reviewed_transport.mjs", group, "--chunk", String(chunk)],
  { cwd: projectRoot, encoding: "utf8", maxBuffer: 16 * 1024 * 1024 },
);
const prepared = JSON.parse(preparedRaw);
const expected = prepared.rows;
if (
  prepared.group !== group ||
  prepared.chunk !== chunk ||
  expected !== config.chunkSize ||
  typeof prepared.base64 !== "string" ||
  prepared.base64.length === 0
) {
  fail(`${group}: prepared chunk metadata failed integrity checks`);
}
const batchId = config.batchId(chunk);
const payloadSql = `convert_from(decode(${quote(prepared.base64)}, 'base64'), 'utf8')::jsonb`;

const commonHeader = `
do $guarded$
declare
  candidate_doc jsonb := ${payloadSql};
  expected integer := ${expected};
  actual integer;
begin
  perform set_config('lock_timeout', '10s', true);
  lock table maintenance.regen_20260727_manifest in share row exclusive mode;

  select count(*) into actual
  from jsonb_to_recordset(candidate_doc) as x(
    id text,
    prompt text,
    answer text,
    hint text,
    source_type text,
    source_id text,
    linguistic_payload jsonb,
    source_file text
  );
  if actual <> expected then
    raise exception 'candidate count guard failed: expected %, got %', expected, actual;
  end if;

  select count(distinct x.id) into actual
  from jsonb_to_recordset(candidate_doc) as x(id text);
  if actual <> expected then
    raise exception 'unique ID guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from maintenance.regen_20260727_manifest m
  where m.batch_id = ${quote(batchId)};
  if actual <> 0 then
    raise exception 'batch ID history overlap guard failed for ${batchId}: got %', actual;
  end if;
`;

let body;
if (config.kind === "exercise") {
  body = `
  perform e.id
  from public.learning_exercises e
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = e.id
  order by e.id
  for update of e;

  perform b.id
  from maintenance.regen_20260727_learning_exercises_backup b
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = b.id
  order by b.id
  for share of b;

  select count(*) into actual
  from public.learning_exercises e
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = e.id;
  if actual <> expected then
    raise exception 'target existence guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from maintenance.regen_20260727_learning_exercises_backup b
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = b.id;
  if actual <> expected then
    raise exception 'backup existence guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from public.learning_exercises e
  join maintenance.regen_20260727_learning_exercises_backup b using (id)
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = e.id
  where row(
    e.id, e.work_slug, e.episode_id, e.episode, e.exercise_type,
    e.prompt, e.answer, e.hint, e.difficulty, e.vocab_item_id,
    e.sort_order, e.updated_at
  ) is not distinct from row(
    b.id, b.work_slug, b.episode_id, b.episode, b.exercise_type,
    b.prompt, b.answer, b.hint, b.difficulty, b.vocab_item_id,
    b.sort_order, b.updated_at
  );
  if actual <> expected then
    raise exception 'full-row drift guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from public.learning_exercises e
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = e.id
  where ${config.placeholderSql};
  if actual <> expected then
    raise exception 'placeholder guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from maintenance.regen_20260727_manifest m
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = m.row_id
  where m.table_name = ${quote(config.tableName)};
  if actual <> 0 then
    raise exception 'manifest history overlap guard failed: got %', actual;
  end if;

  insert into maintenance.regen_20260727_manifest (
    batch_id, table_name, row_id, source_file, generator_agent,
    reviewer_agent, review_status, candidate_values, reviewed_at
  )
  select
    ${quote(batchId)},
    ${quote(config.tableName)},
    x.id,
    x.source_file,
    ${quote(config.generatorAgent)},
    ${quote(config.reviewerAgent)},
    'approved',
    jsonb_build_object('prompt', x.prompt, 'answer', x.answer, 'hint', x.hint),
    transaction_timestamp()
  from jsonb_to_recordset(candidate_doc) as x(
    id text, prompt text, answer text, hint text, source_file text
  );
  get diagnostics actual = row_count;
  if actual <> expected then
    raise exception 'manifest insert guard failed: expected %, got %', expected, actual;
  end if;

  update public.learning_exercises e
  set prompt = x.prompt,
      answer = x.answer,
      hint = x.hint,
      updated_at = transaction_timestamp()
  from jsonb_to_recordset(candidate_doc) as x(
    id text, prompt text, answer text, hint text
  )
  where e.id = x.id;
  get diagnostics actual = row_count;
  if actual <> expected then
    raise exception 'target update guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from public.learning_exercises e
  join jsonb_to_recordset(candidate_doc) as x(
    id text, prompt text, answer text, hint text
  ) on x.id = e.id
  where e.prompt is not distinct from x.prompt
    and e.answer is not distinct from x.answer
    and e.hint is not distinct from x.hint;
  if actual <> expected then
    raise exception 'post-update field verification failed: expected %, got %', expected, actual;
  end if;

  update maintenance.regen_20260727_manifest m
  set review_status = 'applied',
      applied_at = transaction_timestamp()
  where m.batch_id = ${quote(batchId)}
    and m.table_name = ${quote(config.tableName)}
    and m.review_status = 'approved'
    and exists (
      select 1
      from jsonb_to_recordset(candidate_doc) as x(id text)
      where x.id = m.row_id
    );
  get diagnostics actual = row_count;
  if actual <> expected then
    raise exception 'manifest applied guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from maintenance.regen_20260727_manifest m
  join jsonb_to_recordset(candidate_doc) as x(id text)
    on x.id = m.row_id
  join public.learning_exercises e
    on e.id = m.row_id
   and m.table_name = ${quote(config.tableName)}
  where m.batch_id = ${quote(batchId)}
    and m.review_status = 'applied'
    and e.prompt is not distinct from m.candidate_values ->> 'prompt'
    and e.answer is not distinct from m.candidate_values ->> 'answer'
    and e.hint is not distinct from m.candidate_values ->> 'hint';
  if actual <> expected then
    raise exception 'manifest candidate verification failed: expected %, got %', expected, actual;
  end if;
`;
} else {
  body = `
  perform e.id
  from public.learning_card_enrichments e
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = e.id
  order by e.id
  for update of e;

  perform b.id
  from maintenance.regen_20260727_card_enrichments_backup b
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = b.id
  order by b.id
  for share of b;

  select count(*) into actual
  from public.learning_card_enrichments e
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = e.id;
  if actual <> expected then
    raise exception 'target existence guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from maintenance.regen_20260727_card_enrichments_backup b
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = b.id;
  if actual <> expected then
    raise exception 'backup existence guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from public.learning_card_enrichments e
  join maintenance.regen_20260727_card_enrichments_backup b using (id)
  join jsonb_to_recordset(candidate_doc) as x(
    id text, source_type text, source_id text
  )
    on x.id = e.id
   and x.source_type = e.source_type
   and x.source_id = e.source_id
  where row(
    e.id, e.source_type, e.source_id, e.work_slug, e.episode, e.model,
    e.prompt_version, e.quality_score, e.status, e.payload, e.created_at,
    e.updated_at, e.linguistic_payload, e.linguistic_prompt_version,
    e.linguistic_quality_score, e.linguistic_status
  ) is not distinct from row(
    b.id, b.source_type, b.source_id, b.work_slug, b.episode, b.model,
    b.prompt_version, b.quality_score, b.status, b.payload, b.created_at,
    b.updated_at, b.linguistic_payload, b.linguistic_prompt_version,
    b.linguistic_quality_score, b.linguistic_status
  );
  if actual <> expected then
    raise exception 'full-row drift/source guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from public.learning_card_enrichments e
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = e.id
  where e.linguistic_prompt_version = 'linguistic-card-v1'
    and e.linguistic_payload is not null;
  if actual <> expected then
    raise exception 'old-linguistic-placeholder guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from maintenance.regen_20260727_manifest m
  join jsonb_to_recordset(candidate_doc) as x(id text) on x.id = m.row_id
  where m.table_name = ${quote(config.tableName)};
  if actual <> 0 then
    raise exception 'manifest history overlap guard failed: got %', actual;
  end if;

  insert into maintenance.regen_20260727_manifest (
    batch_id, table_name, row_id, source_file, generator_agent,
    reviewer_agent, review_status, candidate_values, reviewed_at
  )
  select
    ${quote(batchId)},
    ${quote(config.tableName)},
    x.id,
    x.source_file,
    ${quote(config.generatorAgent)},
    ${quote(config.reviewerAgent)},
    'approved',
    jsonb_build_object(
      'linguistic_payload', x.linguistic_payload,
      'linguistic_prompt_version', 'agent-jp-regenerated-v1',
      'linguistic_quality_score', 95,
      'linguistic_status', 'ready'
    ),
    transaction_timestamp()
  from jsonb_to_recordset(candidate_doc) as x(
    id text, linguistic_payload jsonb, source_file text
  );
  get diagnostics actual = row_count;
  if actual <> expected then
    raise exception 'manifest insert guard failed: expected %, got %', expected, actual;
  end if;

  update public.learning_card_enrichments e
  set linguistic_payload = x.linguistic_payload,
      linguistic_prompt_version = 'agent-jp-regenerated-v1',
      linguistic_quality_score = 95,
      linguistic_status = 'ready',
      updated_at = transaction_timestamp()
  from jsonb_to_recordset(candidate_doc) as x(id text, linguistic_payload jsonb)
  where e.id = x.id;
  get diagnostics actual = row_count;
  if actual <> expected then
    raise exception 'target update guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from public.learning_card_enrichments e
  join jsonb_to_recordset(candidate_doc) as x(id text, linguistic_payload jsonb)
    on x.id = e.id
  where e.linguistic_payload is not distinct from x.linguistic_payload
    and e.linguistic_prompt_version = 'agent-jp-regenerated-v1'
    and e.linguistic_quality_score = 95
    and e.linguistic_status = 'ready';
  if actual <> expected then
    raise exception 'post-update field verification failed: expected %, got %', expected, actual;
  end if;

  update maintenance.regen_20260727_manifest m
  set review_status = 'applied',
      applied_at = transaction_timestamp()
  where m.batch_id = ${quote(batchId)}
    and m.table_name = ${quote(config.tableName)}
    and m.review_status = 'approved'
    and exists (
      select 1
      from jsonb_to_recordset(candidate_doc) as x(id text)
      where x.id = m.row_id
    );
  get diagnostics actual = row_count;
  if actual <> expected then
    raise exception 'manifest applied guard failed: expected %, got %', expected, actual;
  end if;

  select count(*) into actual
  from maintenance.regen_20260727_manifest m
  join jsonb_to_recordset(candidate_doc) as x(id text)
    on x.id = m.row_id
  join public.learning_card_enrichments e
    on e.id = m.row_id
   and m.table_name = ${quote(config.tableName)}
  where m.batch_id = ${quote(batchId)}
    and m.review_status = 'applied'
    and e.linguistic_payload
          is not distinct from m.candidate_values -> 'linguistic_payload'
    and e.linguistic_prompt_version
          is not distinct from m.candidate_values ->> 'linguistic_prompt_version'
    and e.linguistic_quality_score
          is not distinct from (m.candidate_values ->> 'linguistic_quality_score')::integer
    and e.linguistic_status
          is not distinct from m.candidate_values ->> 'linguistic_status';
  if actual <> expected then
    raise exception 'manifest candidate verification failed: expected %, got %', expected, actual;
  end if;
`;
}

const globalVerification = `
  select count(*) into actual
  from maintenance.regen_20260727_manifest m
  left join public.learning_exercises ex
    on m.table_name = 'public.learning_exercises'
   and ex.id = m.row_id
  left join public.learning_card_enrichments card
    on m.table_name = 'public.learning_card_enrichments'
   and card.id = m.row_id
  where m.review_status = 'applied'
    and (
      m.applied_at is null
      or m.table_name not in (
        'public.learning_exercises',
        'public.learning_card_enrichments'
      )
      or (
        m.table_name = 'public.learning_exercises'
        and (
          ex.id is null
          or ex.prompt is distinct from m.candidate_values ->> 'prompt'
          or ex.answer is distinct from m.candidate_values ->> 'answer'
          or ex.hint is distinct from m.candidate_values ->> 'hint'
        )
      )
      or (
        m.table_name = 'public.learning_card_enrichments'
        and (
          card.id is null
          or card.linguistic_payload
               is distinct from m.candidate_values -> 'linguistic_payload'
          or card.linguistic_prompt_version
               is distinct from m.candidate_values ->> 'linguistic_prompt_version'
          or to_jsonb(card.linguistic_quality_score)
               is distinct from m.candidate_values -> 'linguistic_quality_score'
          or card.linguistic_status
               is distinct from m.candidate_values ->> 'linguistic_status'
        )
      )
    );
  if actual <> 0 then
    raise exception 'global applied-manifest verification failed: % mismatched rows', actual;
  end if;
`;

const footer = `
end
$guarded$;

select
  ${quote(batchId)} as batch_id,
  count(*) as applied_rows,
  count(*) filter (
    where review_status = 'applied'
      and applied_at is not null
  ) as verified_applied_rows,
  (
    select count(*)
    from maintenance.regen_20260727_manifest
    where review_status = 'applied'
  ) as global_applied_rows,
  (
    select count(*)
    from maintenance.regen_20260727_manifest m
    join public.learning_exercises e
      on m.table_name = 'public.learning_exercises'
     and e.id = m.row_id
    where m.review_status = 'applied'
      and e.prompt is not distinct from m.candidate_values ->> 'prompt'
      and e.answer is not distinct from m.candidate_values ->> 'answer'
      and e.hint is not distinct from m.candidate_values ->> 'hint'
  ) as global_exact_exercises,
  (
    select count(*)
    from maintenance.regen_20260727_manifest m
    join public.learning_card_enrichments e
      on m.table_name = 'public.learning_card_enrichments'
     and e.id = m.row_id
    where m.review_status = 'applied'
      and e.linguistic_payload
            is not distinct from m.candidate_values -> 'linguistic_payload'
      and e.linguistic_prompt_version
            is not distinct from m.candidate_values ->> 'linguistic_prompt_version'
      and to_jsonb(e.linguistic_quality_score)
            is not distinct from m.candidate_values -> 'linguistic_quality_score'
      and e.linguistic_status
            is not distinct from m.candidate_values ->> 'linguistic_status'
  ) as global_exact_linguistic
from maintenance.regen_20260727_manifest
where batch_id = ${quote(batchId)}
  and table_name = ${quote(config.tableName)};
`;

process.stdout.write(`${commonHeader}${body}${globalVerification}${footer}`);
