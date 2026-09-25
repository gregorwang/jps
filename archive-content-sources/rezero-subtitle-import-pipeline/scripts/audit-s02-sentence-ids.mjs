import { readFileSync, existsSync, readdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')
const PROJECT_REF = 'qoatvdvbuleamyzsaldp'
const token = process.env.SUPABASE_ACCESS_TOKEN

async function q(sql) {
  const res = await fetch(`https://api.supabase.com/v1/projects/${PROJECT_REF}/database/query`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ query: sql }),
  })
  const text = await res.text()
  if (!res.ok) throw new Error(text)
  return JSON.parse(text)
}

function loadManifestMap() {
  const map = new Map()
  const audioRoot = join(ROOT, 'output', 'audio', 're-zero')
  for (let s2 = 1; s2 <= 25; s2++) {
    const path = join(audioRoot, `s02e${String(s2).padStart(2, '0')}`, 'audio_manifest.json')
    if (!existsSync(path)) continue
    for (const row of JSON.parse(readFileSync(path, 'utf8'))) {
      if (row.db_id && row.sentence_id && row.status === 'ok') {
        map.set(row.db_id, row.sentence_id)
      }
    }
  }
  return map
}

const idStats = await q(`
  select
    count(*)::int as total,
    count(*) filter (where id ~ '^re-zero-s02e[0-9]{2}-sentence-[0-9]{3}$')::int as s02_slug,
    count(*) filter (where id ~ '^re-zero-sentence-ep')::int as legacy_sentence_ep,
    count(*) filter (where id ~ '^re-zero-sent-ep')::int as legacy_sent_ep,
    count(*) filter (where id ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$')::int as uuid
  from public.learning_sentences
  where work_slug = 're-zero' and episode between 26 and 50
`)

const shadowStats = await q(`
  select
    count(*)::int as shadow_total,
    count(*) filter (where id ~ '^re-zero-s02e[0-9]{2}-sentence-[0-9]{3}$')::int as shadow_s02_slug,
    count(*) filter (where id ~ '^[0-9a-f]{8}-')::int as shadow_uuid
  from public.learning_sentences
  where work_slug = 're-zero' and episode between 26 and 50 and recommended_shadowing = true
`)

const sampleUuid = await q(`
  select id, episode, sort_order, left(ja_text, 30) as ja
  from public.learning_sentences
  where work_slug = 're-zero' and episode = 40 and recommended_shadowing = true
  order by sort_order
  limit 5
`)

const fks = await q(`
  select tc.table_name, kcu.column_name, ccu.table_name as foreign_table, ccu.column_name as foreign_column
  from information_schema.table_constraints tc
  join information_schema.key_column_usage kcu on tc.constraint_name = kcu.constraint_name
  join information_schema.constraint_column_usage ccu on ccu.constraint_name = tc.constraint_name
  where tc.constraint_type = 'FOREIGN KEY' and ccu.table_name = 'learning_sentences'
`)

const manifest = loadManifestMap()
let mappable = 0
for (const row of sampleUuid) {
  if (manifest.has(row.id)) mappable++
}

const ids = [...manifest.keys()]
const idList = ids.map((id) => `'${id.replace(/'/g, "''")}'`).join(', ')
const uuidInDb = await q(`select count(*)::int as c from public.learning_sentences where id in (${idList})`)
const legacyShadow = await q(`
  select count(*)::int as c from public.learning_sentences
  where work_slug='re-zero' and episode between 26 and 50
    and recommended_shadowing=true and id like 're-zero-sentence-ep%'
`)
const targetTaken = await q(`
  select count(*)::int as c from public.learning_sentences ls
  where work_slug='re-zero' and episode between 26 and 50
    and id in (select unnest(array[${[...manifest.values()].slice(0, 3).map((id) => `'${id}'`).join(', ')}]))
`)

const plans = await q(`
  select episode,
    count(*) filter (where shadowing_sentence_ids::text like '%re-zero-s02e%')::int as s02_refs,
    count(*) filter (where shadowing_sentence_ids::text ~ '[0-9a-f]{8}-[0-9a-f]{4}')::int as uuid_refs
  from public.episode_learning_plans
  where work_slug='re-zero' and episode between 26 and 50
  group by episode order by episode limit 3
`)

console.log('ID stats EP26-50:', idStats[0])
console.log('Shadowing stats:', shadowStats[0])
console.log('Manifest mappings:', manifest.size)
console.log('Manifest db_ids still in DB:', uuidInDb[0].c)
console.log('Legacy shadow (re-zero-sentence-ep):', legacyShadow[0].c)
console.log('FK refs to learning_sentences:', fks)
console.log('EP40 shadow sample:', sampleUuid)
console.log('Sample mappable in manifest:', mappable, '/', sampleUuid.length)
console.log('Plans sample:', plans)
