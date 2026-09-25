/**
 * Re:Zero episode CSV -> Supabase SQL + Management API executor
 * Usage:
 *   node scripts/import-rezero-episode.mjs --episode 8 --package rezero_ep08_learning_csv_import --write-batches --run
 */
import { parse } from 'csv-parse/sync'
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const args = process.argv.slice(2)
function arg(name, fallback) {
  const i = args.indexOf(name)
  return i >= 0 ? args[i + 1] : fallback
}

const EPISODE = Number(arg('--episode'))
const PACKAGE = arg('--package')
if (!EPISODE || !PACKAGE) {
  console.error('Usage: node scripts/import-rezero-episode.mjs --episode N --package rezero_epNN_learning_csv_import [--write-batches] [--run]')
  process.exit(1)
}

const CSV_DIR = join(__dirname, '..', PACKAGE)
const BATCH_DIR = join(CSV_DIR, '.sql_batches')
const PROJECT_REF = 'qoatvdvbuleamyzsaldp'
const BATCH_SIZE = 200

const JSON_FIELDS = {
  learning_vocab_occurrences: ['example_line_nos'],
  learning_sentences: ['tone_tags'],
  episode_learning_plans: [
    'vocab_item_ids',
    'handwriting_vocab_ids',
    'shadowing_sentence_ids',
    'grammar_point_ids',
    'exercise_ids',
  ],
}

const BOOLEAN_FIELDS = new Set([
  'suitable_handwriting',
  'suitable_shadowing',
  'recommended_shadowing',
  'usable_as_main_corpus',
])

const COLUMN_MAP = {
  subtitle_chunks: {
    start_line_no: 'start_line',
    end_line_no: 'end_line',
    cue_count: 'line_count',
  },
}

const TABLE_ORDER = [
  'episodes',
  'subtitle_chunks',
  'learning_vocab_items',
  'learning_vocab_occurrences',
  'learning_grammar_points',
  'learning_sentences',
  'learning_exercises',
  'episode_learning_plans',
]

const FILE_MAP = {
  episodes: 'episodes.csv',
  subtitle_chunks: 'subtitle_chunks.csv',
  learning_vocab_items: 'learning_vocab_items.csv',
  learning_vocab_occurrences: 'learning_vocab_occurrences.csv',
  learning_grammar_points: 'learning_grammar_points.csv',
  learning_sentences: 'learning_sentences.csv',
  learning_exercises: 'learning_exercises.csv',
  episode_learning_plans: 'episode_learning_plans.csv',
}

function readCsv(table) {
  let raw = readFileSync(join(CSV_DIR, FILE_MAP[table]), 'utf8')
  if (raw.charCodeAt(0) === 0xfeff) raw = raw.slice(1)
  return parse(raw, { columns: true, skip_empty_lines: true, relax_quotes: true })
}

function emptyToNull(v) {
  if (v === undefined || v === null || v === '') return null
  return v
}

function transformRow(table, row) {
  const map = COLUMN_MAP[table] ?? {}
  const out = {}
  for (const [key, val] of Object.entries(row)) {
    const col = map[key.replace(/^\uFEFF/, '')] ?? key.replace(/^\uFEFF/, '')
    out[col] = emptyToNull(val)
  }
  if (table === 'episodes' && out.source_files != null) JSON.parse(out.source_files)
  for (const field of JSON_FIELDS[table] ?? []) {
    const v = out[field]
    if (v == null) { out[field] = null; continue }
    out[field] = JSON.parse(v)
  }
  for (const [key, val] of Object.entries(out)) {
    if (BOOLEAN_FIELDS.has(key) && val != null) out[key] = val === 'true' || val === true
    if (val != null && /^\d+$/.test(String(val))) {
      if ([
        'episode', 'total_cues', 'ja_lines', 'zh_lines', 'usable_ja_lines',
        'chunk_count', 'chunk_no', 'start_line', 'end_line', 'line_count',
        'occurrence_count', 'source_line_no', 'sort_order', 'total_occurrences', 'episode_count',
      ].includes(key)) out[key] = Number(val)
    }
  }
  return out
}

function sqlLiteral(value, { jsonb = false } = {}) {
  if (value === null || value === undefined) return 'NULL'
  if (typeof value === 'boolean') return value ? 'true' : 'false'
  if (typeof value === 'number') return String(value)
  if (jsonb) return `'${JSON.stringify(value).replace(/'/g, "''")}'::jsonb`
  return `'${String(value).replace(/'/g, "''")}'`
}

function buildInsert(table, rows) {
  if (!rows.length) return ''
  const cols = Object.keys(rows[0])
  const jsonCols = new Set(JSON_FIELDS[table] ?? [])
  const values = rows.map((row) => {
    const parts = cols.map((c) => jsonCols.has(c) ? sqlLiteral(row[c], { jsonb: true }) : sqlLiteral(row[c]))
    return `(${parts.join(', ')})`
  }).join(',\n')
  return `INSERT INTO ${table} (${cols.join(', ')})\nVALUES\n${values};`
}

function buildVocabItemsImport(rows) {
  const ids = rows.map((r) => sqlLiteral(r.id)).join(', ')
  return [
    `DELETE FROM learning_vocab_items WHERE id IN (${ids});`,
    buildInsert('learning_vocab_items', rows),
  ]
}

function chunk(arr, size) {
  const out = []
  for (let i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size))
  return out
}

function generateImportSql() {
  const statements = []
  for (const table of [
    'learning_exercises', 'learning_sentences', 'learning_grammar_points',
    'learning_vocab_occurrences', 'episode_learning_plans', 'subtitle_chunks', 'episodes',
  ]) {
    statements.push(`DELETE FROM ${table} WHERE work_slug = 're-zero' AND episode = ${EPISODE};`)
  }
  for (const table of TABLE_ORDER) {
    const rows = readCsv(table).map((r) => transformRow(table, r))
    for (const batch of chunk(rows, BATCH_SIZE)) {
      if (table === 'learning_vocab_items') statements.push(...buildVocabItemsImport(batch))
      else statements.push(buildInsert(table, batch))
    }
  }
  return statements
}

async function runSql(token, query) {
  const res = await fetch(`https://api.supabase.com/v1/projects/${PROJECT_REF}/database/query`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ query }),
  })
  const text = await res.text()
  if (!res.ok) throw new Error(`${res.status} ${text}`)
  return text
}

const writeBatches = args.includes('--write-batches')
const run = args.includes('--run')
const statements = generateImportSql()

if (writeBatches) {
  mkdirSync(BATCH_DIR, { recursive: true })
  statements.forEach((sql, i) => writeFileSync(join(BATCH_DIR, `${String(i + 1).padStart(3, '0')}.sql`), sql + '\n', 'utf8'))
  console.log(`EP${EPISODE}: wrote ${statements.length} SQL files`)
}

if (run) {
  const token = process.env.SUPABASE_ACCESS_TOKEN || args.find((a) => a.startsWith('sbp_'))
  if (!token) { console.error('Missing SUPABASE_ACCESS_TOKEN'); process.exit(1) }
  console.log(`EP${EPISODE}: running ${statements.length} statements...`)
  let i = 0
  for (const sql of statements) {
    i++
    process.stdout.write(`  [${i}/${statements.length}] ${sql.length}b... `)
    const t0 = Date.now()
    await runSql(token, sql)
    console.log(`${Date.now() - t0}ms`)
  }
  console.log(`EP${EPISODE}: import complete`)
}