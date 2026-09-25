const token = process.env.SUPABASE_ACCESS_TOKEN?.trim()
if (!token) {
  console.error('Set SUPABASE_ACCESS_TOKEN')
  process.exit(1)
}
const ref = 'qoatvdvbuleamyzsaldp'
const scope = "work_slug = 're-zero' and episode between 26 and 50"

async function q(sql) {
  for (let i = 0; i < 5; i++) {
    const r = await fetch(`https://api.supabase.com/v1/projects/${ref}/database/query`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: sql }),
    })
    const t = await r.text()
    if (r.status === 429) {
      await new Promise((res) => setTimeout(res, 3000 * (i + 1)))
      continue
    }
    if (!r.ok) throw new Error(`${r.status} ${t}`)
    return JSON.parse(t)
  }
  throw new Error('rate limited')
}

for (const t of [
  'episodes', 'subtitle_chunks', 'learning_vocab_occurrences', 'learning_grammar_points',
  'learning_sentences', 'learning_exercises', 'episode_learning_plans',
]) {
  const rows = await q(`select count(*)::int as c from public.${t} where ${scope}`)
  console.log(`${t}: ${rows[0].c}`)
}
const shadow = await q(`select count(*)::int as c from public.learning_sentences where ${scope} and recommended_shadowing = true`)
console.log(`recommended_shadowing: ${shadow[0].c}`)
const badFk = await q(`
  select count(*)::int as c from public.learning_exercises e
  left join public.learning_vocab_items v on v.id = e.vocab_item_id
  where e.work_slug='re-zero' and e.episode between 26 and 50
    and e.exercise_type in ('vocab_meaning','vocab_reading') and v.id is null`)
console.log(`bad_vocab_fk: ${badFk[0].c}`)
const ep125 = await q(`select count(*)::int as c from public.episodes where work_slug='re-zero' and episode between 1 and 25`)
console.log(`ep01_25_preserved: ${ep125[0].c}`)
