import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const dir = join(__dirname, '..', 'rezero_ep05_ep06_learning_csv_import', '.sql_batches')
const out = join(dir, 'chunks')
mkdirSync(out, { recursive: true })

const files = ['015', '017', '021', '023', '025', '027', '029']

for (const f of files) {
  const sql = readFileSync(join(dir, `${f}.sql`), 'utf8').trim()
  const m = sql.match(/^INSERT INTO ([^(]+)\(([^)]+)\)\s*\nVALUES\s*\n([\s\S]*)$/i)
  if (!m) {
    console.error('parse fail', f)
    continue
  }
  const table = m[1].trim()
  const cols = m[2].trim()
  const rows = m[3].replace(/;\s*$/, '').split(/\),\n/).map((r, i, a) => (i < a.length - 1 ? `${r})` : r))
  const chunkSize = f === '027' || f === '029' ? rows.length : 50
  let ci = 0
  for (let i = 0; i < rows.length; i += chunkSize) {
    const part = rows.slice(i, i + chunkSize).join(',\n')
    const q = `INSERT INTO ${table}(${cols})\nVALUES\n${part};`
    const name = `${f}_${String(ci++).padStart(2, '0')}`
    writeFileSync(join(out, `${name}.sql`), q)
    writeFileSync(join(out, `${name}.json`), JSON.stringify({ name: `import_rezero_${name}`, query: q }))
    console.log(name, rows.slice(i, i + chunkSize).length, q.length)
  }
}