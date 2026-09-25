import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')
const AUDIO_ROOT = join(ROOT, 'output', 'audio', 're-zero')
const OUT = join(ROOT, 'output', 'tmp', 's02-dialogue-update-batches.json')
const placeholdersOnly = !process.argv.includes('--all')

function pickZh(matchedTcText) {
  const cleaned = String(matchedTcText || '').replace(/\{[^}]*\}/g, '').trim()
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
  return `'${String(value).replace(/'/g, "''")}'`
}

const updates = []
for (let s2 = 1; s2 <= 25; s2++) {
  const path = join(AUDIO_ROOT, `s02e${String(s2).padStart(2, '0')}`, 'audio_manifest.json')
  if (!existsSync(path)) continue
  for (const row of JSON.parse(readFileSync(path, 'utf8'))) {
    if (!row.db_id || row.status !== 'ok') continue
    const meaning_zh = pickZh(row.matched_tc_text)
    if (!meaning_zh) continue
    updates.push({
      id: row.db_id,
      ja_text: cleanJa(row.ja_text || row.matched_ja_text),
      meaning_zh,
    })
  }
}

const placeholderFilter = placeholdersOnly
  ? ` AND (ls.meaning_zh LIKE '用于慢读跟读的原句：%' OR ls.meaning_zh LIKE '用于本集慢读跟读的完整台词%')`
  : ''

const BATCH_SIZE = 80
const batches = []
for (let i = 0; i < updates.length; i += BATCH_SIZE) {
  const batch = updates.slice(i, i + BATCH_SIZE)
  const values = batch
    .map((r) => `(${sqlLiteral(r.id)}, ${sqlLiteral(r.ja_text)}, ${sqlLiteral(r.meaning_zh)})`)
    .join(',')
  batches.push(
    `UPDATE public.learning_sentences AS ls SET ja_text = v.ja_text, meaning_zh = v.meaning_zh, updated_at = now() FROM (VALUES ${values}) AS v(id, ja_text, meaning_zh) WHERE ls.id = v.id${placeholderFilter};`,
  )
}

mkdirSync(dirname(OUT), { recursive: true })
writeFileSync(OUT, JSON.stringify({ mode: placeholdersOnly ? 'placeholders' : 'all', count: updates.length, batches }))
console.log(`Wrote ${batches.length} batches (${updates.length} manifest rows, mode=${placeholdersOnly ? 'placeholders' : 'all'})`)
