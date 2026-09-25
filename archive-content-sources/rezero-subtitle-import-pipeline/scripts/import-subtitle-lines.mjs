// Import subtitle_lines.csv into Supabase public.subtitle_lines
import { parse } from 'csv-parse/sync'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const args = process.argv.slice(2)
function arg(name, fallback) {
  const i = args.indexOf(name)
  return i >= 0 ? args[i + 1] : fallback
}

const PACKAGE = arg('--package', 'rezero_s1_ep06_ep20_subtitle_lines_import')
const FROM_EP = Number(arg('--from-ep', '6'))
const TO_EP = Number(arg('--to-ep', '20'))
const CSV_PATH = join(__dirname, '..', PACKAGE, 'subtitle_lines.csv')
const PROJECT_REF = 'qoatvdvbuleamyzsaldp'
const BATCH_SIZE = 150
const run = args.includes('--run')
const token = process.env.SUPABASE_ACCESS_TOKEN || args.find((a) => a.startsWith('sbp_'))

function readRows() {
  let raw = readFileSync(CSV_PATH, 'utf8')
  if (raw.charCodeAt(0) === 0xfeff) raw = raw.slice(1)
  return parse(raw, { columns: true, skip_empty_lines: true, relax_quotes: true })
}

function sqlLiteral(value) {
  if (value === null || value === undefined || value === '') return 'NULL'
  if (typeof value === 'boolean') return value ? 'true' : 'false'
  if (value === 'true') return 'true'
  if (value === 'false') return 'false'
  if (/^\d+$/.test(String(value))) return String(value)
  return `'${String(value).replace(/'/g, "''")}'`
}

function transformRow(r) {
  return {
    work_display_name: r.work_display_name,
    work_slug: r.work_slug,
    episode: Number(r.episode),
    line_no: Number(r.line_no),
    start_time: r.start_time,
    end_time: r.end_time,
    ja_text: r.ja_text,
    zh_text: r.zh_text || null,
    language: r.language,
    source: r.source,
    usable_for_analysis: r.usable_for_analysis === 'true' || r.usable_for_analysis === true,
  }
}

function buildInsert(rows) {
  const cols = Object.keys(rows[0])
  const values = rows
    .map((row) => `(${cols.map((c) => sqlLiteral(row[c])).join(', ')})`)
    .join(',\n')
  return `INSERT INTO public.subtitle_lines (${cols.join(', ')})\nVALUES\n${values};`
}

function chunk(arr, size) {
  const out = []
  for (let i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size))
  return out
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

async function main() {
  const rows = readRows().map(transformRow)
  console.log(`Loaded ${rows.length} rows from ${CSV_PATH}`)

  const statements = [
    `DELETE FROM public.subtitle_lines WHERE work_slug = 're-zero' AND episode BETWEEN ${FROM_EP} AND ${TO_EP};`,
    ...chunk(rows, BATCH_SIZE).map(buildInsert),
    `UPDATE public.episodes e SET ja_lines = sub.ja_cnt, zh_lines = sub.zh_cnt FROM (
      SELECT episode,
        count(*)::int AS ja_cnt,
        count(*) FILTER (WHERE coalesce(zh_text, '') <> '')::int AS zh_cnt
      FROM public.subtitle_lines
      WHERE work_slug = 're-zero' AND episode BETWEEN ${FROM_EP} AND ${TO_EP}
      GROUP BY episode
    ) sub WHERE e.work_slug = 're-zero' AND e.episode = sub.episode;`,
    `SELECT episode, count(*)::int AS rows, count(*) FILTER (WHERE coalesce(zh_text,'') <> '')::int AS with_zh FROM public.subtitle_lines WHERE work_slug = 're-zero' AND episode BETWEEN ${FROM_EP} AND ${TO_EP} GROUP BY episode ORDER BY episode;`,
  ]

  if (!run) {
    console.log(`Dry run: ${statements.length} statements (${statements.length - 2} insert batches)`)
    return
  }

  if (!token) {
    console.error('Missing SUPABASE_ACCESS_TOKEN or sbp_ token argument')
    process.exit(1)
  }

  let i = 0
  for (const sql of statements) {
    i++
    const label = sql.startsWith('INSERT') ? 'insert' : sql.startsWith('DELETE') ? 'delete' : 'verify'
    process.stdout.write(`[${i}/${statements.length}] ${label} (${sql.length}b)... `)
    const t0 = Date.now()
    const result = await runSql(sql)
    console.log(`${Date.now() - t0}ms`)
    if (label === 'verify') console.log(result)
  }
  console.log('Import complete')
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
