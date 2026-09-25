/**
 * Second pass: update remaining placeholder meaning_zh from manifests (direct API).
 */
import { readFileSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')
const AUDIO_ROOT = join(ROOT, 'output', 'audio', 're-zero')
const PROJECT_REF = 'qoatvdvbuleamyzsaldp'
const token = process.env.SUPABASE_ACCESS_TOKEN || process.argv.find((a) => a.startsWith('sbp_'))

const PLACEHOLDER_RE = /^用于(慢读跟读的原句：|本集慢读跟读的完整台词)/

function pickZh(t) {
  const c = String(t || '').replace(/\{[^}]*\}/g, '').trim()
  if (!c) return ''
  const parts = c.split(' / ').map((p) => p.trim()).filter(Boolean)
  return parts[0] || c
}

function cleanJa(t) {
  return String(t || '')
    .split('\n')
    .map((l) => l.replace(/^（[^）)]*）\s*/, '').trim())
    .filter(Boolean)
    .join(' ')
    .trim()
}

function sqlLiteral(v) {
  return `'${String(v).replace(/'/g, "''")}'`
}

async function runSql(query) {
  const res = await fetch(`https://api.supabase.com/v1/projects/${PROJECT_REF}/database/query`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ query }),
  })
  const text = await res.text()
  if (!res.ok) throw new Error(`${res.status} ${text}`)
  return JSON.parse(text)
}

const manifest = new Map()
for (let s2 = 1; s2 <= 25; s2++) {
  const path = join(AUDIO_ROOT, `s02e${String(s2).padStart(2, '0')}`, 'audio_manifest.json')
  if (!existsSync(path)) continue
  for (const row of JSON.parse(readFileSync(path, 'utf8'))) {
    if (!row.db_id || row.status !== 'ok') continue
    const zh = pickZh(row.matched_tc_text)
    if (!zh) continue
    manifest.set(row.db_id, {
      id: row.db_id,
      ja_text: cleanJa(row.ja_text || row.matched_ja_text),
      meaning_zh: zh,
    })
  }
}

const placeholders = await runSql(`
  select id, meaning_zh from learning_sentences
  where work_slug='re-zero' and episode between 26 and 50
    and (meaning_zh like '用于慢读跟读的原句：%' or meaning_zh like '用于本集慢读跟读的完整台词%')
`)
console.log(`Remaining placeholders in DB: ${placeholders.length}`)

const updates = []
const noManifest = []
for (const row of placeholders) {
  const u = manifest.get(row.id)
  if (u) updates.push(u)
  else noManifest.push(row.id)
}
console.log(`Can update from manifest: ${updates.length}, no manifest match: ${noManifest.length}`)

const BATCH = 80
for (let i = 0; i < updates.length; i += BATCH) {
  const batch = updates.slice(i, i + BATCH)
  const values = batch
    .map((r) => `(${sqlLiteral(r.id)}, ${sqlLiteral(r.ja_text)}, ${sqlLiteral(r.meaning_zh)})`)
    .join(',')
  const sql = `UPDATE public.learning_sentences AS ls SET ja_text=v.ja_text, meaning_zh=v.meaning_zh, updated_at=now() FROM (VALUES ${values}) AS v(id,ja_text,meaning_zh) WHERE ls.id=v.id`
  process.stdout.write(`batch ${Math.floor(i / BATCH) + 1}/${Math.ceil(updates.length / BATCH)}... `)
  await runSql(sql)
  console.log('ok')
}

const verify = await runSql(`
  select count(*)::int as ph from learning_sentences
  where work_slug='re-zero' and episode between 26 and 50
    and (meaning_zh like '用于慢读跟读的原句：%' or meaning_zh like '用于本集慢读跟读的完整台词%')
`)
console.log('Placeholders left:', verify[0].ph)
