const token = process.env.SUPABASE_ACCESS_TOKEN
const ref = 'qoatvdvbuleamyzsaldp'
async function q(sql) {
  const r = await fetch(`https://api.supabase.com/v1/projects/${ref}/database/query`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ query: sql }),
  })
  return JSON.parse(await r.text())
}
const lines = await q(`
  select episode, count(*)::int as lines,
    count(*) filter (where zh_text is not null and zh_text <> '')::int as with_zh
  from subtitle_lines where work_slug='re-zero'
  group by episode order by episode`)
const learning = await q(`
  select min(episode) as ep_min, max(episode) as ep_max, count(*)::int as cnt
  from learning_sentences where work_slug='re-zero'`)
console.log('subtitle_lines by episode:', lines)
console.log('learning_sentences range:', learning[0])
