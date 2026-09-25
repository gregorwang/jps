/**
 * Update learning_sentences ja_text / meaning_zh from S02 audio manifests.
 * Source: output/audio/re-zero/s02e{01-25}/audio_manifest.json
 *
 * Usage:
 *   set SUPABASE_ACCESS_TOKEN=sbp_...
 *   node scripts/update-s02-dialogue-from-audio-manifest.mjs          # placeholders only
 *   node scripts/update-s02-dialogue-from-audio-manifest.mjs --all  # all manifest rows
 *   node scripts/update-s02-dialogue-from-audio-manifest.mjs --dry-run
 */
import { readFileSync, readdirSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')
const AUDIO_ROOT = join(ROOT, 'output', 'audio', 're-zero')
const PROJECT_REF = 'qoatvdvbuleamyzsaldp'
const BATCH_SIZE = 100

const args = process.argv.slice(2)
const dryRun = args.includes('--dry-run')
const updateAll = args.includes('--all')
const token = process.env.SUPABASE_ACCESS_TOKEN || args.find((a) => a.startsWith('sbp_'))

const PLACEHOLDER_RE = /^用于(慢读跟读的原句：|本集慢读跟读的完整台词)/

function cleanAssTags(text) {
  return String(text || '').replace(/\{[^}]*\}/g, '').trim()
}

function pickZh(matchedTcText) {
  const cleaned = cleanAssTags(matchedTcText)
  if (!cleaned) return ''
  const parts = cleaned.split(' / ').map((p) => p.trim()).filter(Boolean)
  return parts[0] || cleaned
}

function cleanJa(jaText) {
  return String(jaText || '')
    .split('\n')
    .map((line) => line.replace(/^（[^）)]*）\s*/, '').trim())
    .filter(Boolean)
    .join(' ')
    .trim()
}

function sqlLiteral(value) {
  if (value === null || value === undefined || value === '') return 'NULL'
  return `'${String(value).replace(/'/g, "''")}'`
}

function loadManifestUpdates() {
  const updates = new Map()
  for (let s2 = 1; s2 <= 25; s2++) {
    const slug = `s02e${String(s2).padStart(2, '0')}`
    const path = join(AUDIO_ROOT, slug, 'audio_manifest.json')
    if (!existsSync(path)) continue
    const rows = JSON.parse(readFileSync(path, 'utf8'))
    for (const row of rows) {
      if (!row.db_id || row.status !== 'ok') continue
      const zh = pickZh(row.matched_tc_text)
      if (!zh) continue
      const ja = cleanJa(row.ja_text || row.matched_ja_text)
      updates.set(row.db_id, { id: row.db_id, ja_text: ja, meaning_zh: zh })
    }
  }
  return [...updates.values()]
}

function buildBatchSql(batch) {
  const values = batch
    .map((r) => `(${sqlLiteral(r.id)}, ${sqlLiteral(r.ja_text)}, ${sqlLiteral(r.meaning_zh)})`)
    .join(',\n')
  return `
UPDATE public.learning_sentences AS ls
SET
  ja_text = v.ja_text,
  meaning_zh = v.meaning_zh,
  updated_at = now()
FROM (VALUES
${values}
) AS v(id, ja_text, meaning_zh)
WHERE ls.id = v.id;
`.trim()
}

async function runSql(query) {
  const res = await fetch(`https://api.supabase.com/v1/projects/${PROJECT_REF}/database/query`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ query }),
  })
  const text = await res.text()
  if (!res.ok) throw new Error(`${res.status} ${text}`)
  return text
}

function chunk(arr, size) {
  const out = []
  for (let i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size))
  return out
}

async function main() {
  const allUpdates = loadManifestUpdates()
  console.log(`Loaded ${allUpdates.length} manifest dialogue rows`)

  let updates = allUpdates
  if (!updateAll) {
    const ids = allUpdates.map((r) => sqlLiteral(r.id)).join(',')
    const rows = JSON.parse(await runSql(`
      select id, meaning_zh
      from public.learning_sentences
      where id in (${ids})
    `))
    const placeholderIds = new Set(
      rows
        .filter((r) => PLACEHOLDER_RE.test(r.meaning_zh || ''))
        .map((r) => r.id),
    )
    updates = allUpdates.filter((r) => placeholderIds.has(r.id))
    console.log(`Placeholder-only mode: ${updates.length} rows to update`)
  } else {
    console.log(`--all mode: ${updates.length} rows to update`)
  }

  if (dryRun) {
    console.log('Sample updates:')
    for (const row of updates.slice(0, 5)) {
      console.log(`  ${row.id}: ${row.ja_text} => ${row.meaning_zh}`)
    }
    return
  }

  if (!token) {
    console.error('Missing SUPABASE_ACCESS_TOKEN or sbp_ token argument')
    process.exit(1)
  }

  const batches = chunk(updates, BATCH_SIZE)
  let i = 0
  for (const batch of batches) {
    i++
    process.stdout.write(`batch ${i}/${batches.length}... `)
    await runSql(buildBatchSql(batch))
    console.log('ok')
  }

  const verify = JSON.parse(await runSql(`
    select
      count(*) filter (where meaning_zh like '用于慢读跟读的原句：%' or meaning_zh like '用于本集慢读跟读的完整台词%')::int as placeholders,
      count(*)::int as total
    from public.learning_sentences
    where work_slug = 're-zero' and episode between 26 and 50
  `))
  console.log('Verify EP26-50:', verify[0])
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
