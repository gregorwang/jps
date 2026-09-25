/**
 * Remap S02 learning_sentences.id to R2-compatible slugs (re-zero-s02eNN-sentence-NNN).
 * Uses audio_manifest.json db_id -> sentence_id mapping from the shadowing pipeline.
 *
 * Usage:
 *   set SUPABASE_ACCESS_TOKEN=sbp_...
 *   node scripts/remap-s02-learning-sentence-ids.mjs           # dry-run
 *   node scripts/remap-s02-learning-sentence-ids.mjs --run
 */
import { mkdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')
const AUDIO_ROOT = join(ROOT, 'output', 'audio', 're-zero')
const PROJECT_REF = 'qoatvdvbuleamyzsaldp'
const EP_MIN = 26
const EP_MAX = 50
const BATCH_SIZE = 80
const run = process.argv.includes('--run')
const token = process.env.SUPABASE_ACCESS_TOKEN || process.argv.find((a) => a.startsWith('sbp_'))

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`
}

function loadRemapPairs() {
  const pairs = []
  const seenNew = new Set()
  for (let s2 = 1; s2 <= 25; s2++) {
    const path = join(AUDIO_ROOT, `s02e${String(s2).padStart(2, '0')}`, 'audio_manifest.json')
    if (!existsSync(path)) continue
    for (const row of JSON.parse(readFileSync(path, 'utf8'))) {
      if (!row.db_id || !row.sentence_id || row.status !== 'ok') continue
      if (seenNew.has(row.sentence_id)) {
        throw new Error(`duplicate target id ${row.sentence_id}`)
      }
      seenNew.add(row.sentence_id)
      pairs.push({ old_id: row.db_id, new_id: row.sentence_id, episode: EP_MIN + s2 - 1 })
    }
  }
  return pairs
}

async function runSql(query) {
  const res = await fetch(`https://api.supabase.com/v1/projects/${PROJECT_REF}/database/query`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ query }),
  })
  const text = await res.text()
  if (!res.ok) throw new Error(`${res.status} ${text}`)
  return text ? JSON.parse(text) : []
}

function chunk(arr, size) {
  const out = []
  for (let i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size))
  return out
}

function buildIdUpdateSql(batch) {
  const values = batch.map((p) => `(${sqlLiteral(p.old_id)}, ${sqlLiteral(p.new_id)})`).join(',\n')
  return `
UPDATE public.learning_sentences AS ls
SET id = v.new_id, updated_at = now()
FROM (VALUES ${values}) AS v(old_id, new_id)
WHERE ls.id = v.old_id AND ls.id <> v.new_id;
`.trim()
}

function buildRebuildPlansSql() {
  return `
ALTER TABLE public.episode_learning_plans DISABLE TRIGGER USER;
UPDATE public.episode_learning_plans AS p
SET shadowing_sentence_ids = sub.ids
FROM (
  SELECT
    episode,
    jsonb_agg(id ORDER BY sort_order, id) AS ids
  FROM public.learning_sentences
  WHERE work_slug = 're-zero'
    AND episode BETWEEN ${EP_MIN} AND ${EP_MAX}
    AND recommended_shadowing = true
  GROUP BY episode
) sub
WHERE p.work_slug = 're-zero'
  AND p.episode = sub.episode;
ALTER TABLE public.episode_learning_plans ENABLE TRIGGER USER;
`.trim()
}

async function main() {
  const pairs = loadRemapPairs()
  const noop = pairs.filter((p) => p.old_id === p.new_id)
  const work = pairs.filter((p) => p.old_id !== p.new_id)

  console.log(`manifest pairs: ${pairs.length}, already aligned: ${noop.length}, to remap: ${work.length}`)

  if (!token) {
    console.error('Missing SUPABASE_ACCESS_TOKEN')
    process.exit(1)
  }

  const oldList = work.map((p) => sqlLiteral(p.old_id)).join(', ')
  const newList = work.map((p) => sqlLiteral(p.new_id)).join(', ')
  const preflight = await runSql(`
    select
      (select count(*)::int from public.learning_sentences where id in (${oldList})) as old_present,
      (select count(*)::int from public.learning_sentences where id in (${newList})) as new_taken,
      (select count(*)::int from public.learning_sentences where work_slug='re-zero' and episode between ${EP_MIN} and ${EP_MAX} and id ~ '^re-zero-s02e[0-9]{2}-sentence-[0-9]{3}$') as already_s02
  `)
  console.log('preflight:', preflight[0])

  if (preflight[0].new_taken > 0) {
    throw new Error(`target ids already taken: ${preflight[0].new_taken}`)
  }
  if (preflight[0].old_present !== work.length) {
    console.warn(`warning: expected ${work.length} old ids present, found ${preflight[0].old_present}`)
  }

  if (!run) {
    console.log('Dry run only. Pass --run to apply.')
    console.log('sample:', work.slice(0, 5))
    return
  }

  const ts = new Date().toISOString().replace(/[-:]/g, '').replace(/\..+/, 'Z')
  const backupDir = join(ROOT, 'backups', `rezero_s02_sentence_id_remap_${ts}`)
  mkdirSync(backupDir, { recursive: true })

  const sentences = await runSql(`
    select * from public.learning_sentences
    where work_slug='re-zero' and episode between ${EP_MIN} and ${EP_MAX}
      and id in (${oldList})
  `)
  writeFileSync(join(backupDir, 'learning_sentences_before_remap.json'), JSON.stringify(sentences, null, 2))

  const plans = await runSql(`
    select * from public.episode_learning_plans
    where work_slug='re-zero' and episode between ${EP_MIN} and ${EP_MAX}
  `)
  writeFileSync(join(backupDir, 'episode_learning_plans_before_remap.json'), JSON.stringify(plans, null, 2))
  console.log(`backup: ${backupDir}`)

  const idStatements = chunk(work, BATCH_SIZE).map(buildIdUpdateSql)
  for (let i = 0; i < idStatements.length; i++) {
    process.stdout.write(`[id ${i + 1}/${idStatements.length}] `)
    const t0 = Date.now()
    await runSql(idStatements[i])
    console.log(`${Date.now() - t0}ms`)
  }

  process.stdout.write('[plans] ')
  const t0 = Date.now()
  await runSql(buildRebuildPlansSql())
  console.log(`${Date.now() - t0}ms`)

  const verify = await runSql(`
    select
      count(*) filter (where id ~ '^re-zero-s02e[0-9]{2}-sentence-[0-9]{3}$')::int as s02_slug,
      count(*) filter (where recommended_shadowing = true and id ~ '^re-zero-s02e[0-9]{2}-sentence-[0-9]{3}$')::int as shadow_s02,
      count(*) filter (where recommended_shadowing = true)::int as shadow_total
    from public.learning_sentences
    where work_slug='re-zero' and episode between ${EP_MIN} and ${EP_MAX}
  `)
  console.log('verify:', verify[0])

  const ep40 = await runSql(`
    select id, sort_order, left(ja_text, 30) as ja
    from public.learning_sentences
    where work_slug='re-zero' and episode=40 and sort_order <= 3
    order by sort_order
  `)
  console.log('EP40 sample:', ep40)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
