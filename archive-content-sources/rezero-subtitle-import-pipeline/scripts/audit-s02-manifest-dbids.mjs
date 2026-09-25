import { readFileSync, existsSync } from 'node:fs'
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

const dbIds = []
const audioRoot = join(ROOT, 'output', 'audio', 're-zero')
for (let s2 = 1; s2 <= 25; s2++) {
  const path = join(audioRoot, `s02e${String(s2).padStart(2, '0')}`, 'audio_manifest.json')
  if (!existsSync(path)) continue
  for (const row of JSON.parse(readFileSync(path, 'utf8'))) {
    if (row.db_id && row.sentence_id && row.status === 'ok') dbIds.push(row.db_id)
  }
}

const idList = dbIds.map((id) => `'${id.replace(/'/g, "''")}'`).join(', ')
const rows = await q(`
  select
    count(*)::int as total,
    count(*) filter (where recommended_shadowing = true)::int as shadow,
    count(*) filter (where id ~ '^[0-9a-f]{8}-')::int as uuid_fmt,
    count(*) filter (where id like 're-zero-sentence-ep%')::int as legacy_ep
  from public.learning_sentences
  where id in (${idList})
`)

const ep40 = await q(`
  select id, recommended_shadowing, sort_order, left(ja_text,25) ja
  from public.learning_sentences
  where work_slug='re-zero' and episode=40 and id='d3fbeb1e-6ad9-5e3f-8c07-56459ec09d73'
`)

const ep40slug = await q(`
  select id, recommended_shadowing, sort_order, left(ja_text,25) ja
  from public.learning_sentences
  where work_slug='re-zero' and episode=40 and ja_text like '%ババアはどうした%'
`)

console.log('Manifest db_id rows in DB:', rows[0])
console.log('EP40 uuid row:', ep40)
console.log('EP40 all ババア rows:', ep40slug)
