const token = process.env.SUPABASE_ACCESS_TOKEN
const ref = 'qoatvdvbuleamyzsaldp'

async function q(sql) {
  const r = await fetch(`https://api.supabase.com/v1/projects/${ref}/database/query`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ query: sql }),
  })
  const text = await r.text()
  if (!r.ok) throw new Error(`${r.status} ${text}`)
  return JSON.parse(text)
}

const ep40 = await q(`select id, ja_text, meaning_zh from learning_sentences where id='d3fbeb1e-6ad9-5e3f-8c07-56459ec09d73'`)
const byEp = await q(`
  select episode,
    count(*) filter (where meaning_zh like '用于慢读跟读的原句：%' or meaning_zh like '用于本集慢读跟读的完整台词%')::int as ph,
    count(*)::int as total
  from learning_sentences
  where work_slug='re-zero' and episode between 38 and 42
  group by episode order by episode`)
const total = await q(`
  select count(*) filter (where meaning_zh like '用于慢读跟读的原句：%' or meaning_zh like '用于本集慢读跟读的完整台词%')::int as ph
  from learning_sentences where work_slug='re-zero' and episode between 26 and 50`)
console.log('EP40:', ep40[0])
console.log('EP38-42:', byEp)
console.log('Total placeholders EP26-50:', total[0])
