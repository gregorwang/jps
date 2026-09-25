import { readFileSync, readdirSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const GROUP_DIR = join(__dirname, '..', 'output', 'tmp', 's02-dialogue-groups')
const PROJECT_REF = 'qoatvdvbuleamyzsaldp'
const token = process.env.SUPABASE_ACCESS_TOKEN || process.argv.find((a) => a.startsWith('sbp_'))

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
  if (!token) {
    console.error('Set SUPABASE_ACCESS_TOKEN or pass sbp_ token')
    process.exit(1)
  }
  const files = readdirSync(GROUP_DIR).filter((f) => f.endsWith('.sql')).sort()
  for (let i = 0; i < files.length; i++) {
    const sql = readFileSync(join(GROUP_DIR, files[i]), 'utf8')
    process.stdout.write(`group ${i + 1}/${files.length} ${files[i]}... `)
    await runSql(sql)
    console.log('ok')
  }
  const verify = JSON.parse(await runSql(`
    select
      count(*) filter (where meaning_zh like '用于慢读跟读的原句：%' or meaning_zh like '用于本集慢读跟读的完整台词%')::int as placeholders,
      count(*)::int as total
    from public.learning_sentences
    where work_slug = 're-zero' and episode between 26 and 50
  `))
  console.log('verify', verify[0])
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
