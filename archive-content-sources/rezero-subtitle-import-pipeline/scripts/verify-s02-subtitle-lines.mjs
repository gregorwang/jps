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

const queries = [
  `SELECT episode, ja_lines, zh_lines FROM public.episodes WHERE work_slug='re-zero' AND episode BETWEEN 40 AND 42 ORDER BY episode`,
  `SELECT line_no, left(ja_text,40) AS ja, left(zh_text,30) AS zh FROM public.subtitle_lines WHERE work_slug='re-zero' AND episode=40 AND ja_text LIKE '%ババア%' LIMIT 5`,
  `SELECT count(*)::int AS total FROM public.subtitle_lines WHERE work_slug='re-zero' AND episode BETWEEN 26 AND 50`,
]

for (const sql of queries) {
  console.log('\n--', sql.slice(0, 80), '...')
  console.log(JSON.stringify(await q(sql), null, 2))
}
