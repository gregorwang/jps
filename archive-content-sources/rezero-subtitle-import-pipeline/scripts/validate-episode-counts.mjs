import { readFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const PROJECT = 'qoatvdvbuleamyzsaldp'
const token = process.env.SUPABASE_ACCESS_TOKEN
if (!token) {
  console.error('Missing SUPABASE_ACCESS_TOKEN')
  process.exit(1)
}

const packages = {
  17: 'rezero_ep17_learning_csv_import',
  18: 'rezero_ep18_learning_csv_import',
  19: 'rezero_ep19_learning_csv_import',
  20: 'rezero_ep20_learning_csv_import(1)',
  21: 'rezero_ep21_learning_csv_import',
  22: 'rezero_ep22_learning_csv_import(2)',
  23: 'rezero_ep23_learning_csv_import(2)',
  24: 'rezero_ep24_learning_csv_import',
  25: 'rezero_ep25_learning_csv_import',
}

async function q(sql) {
  const res = await fetch(`https://api.supabase.com/v1/projects/${PROJECT}/database/query`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ query: sql }),
  })
  const text = await res.text()
  if (!res.ok) throw new Error(text)
  return JSON.parse(text)
}

function expectedCounts(ep) {
  const raw = readFileSync(join(__dirname, '..', packages[ep], 'counts.json'), 'utf8')
  const c = JSON.parse(raw)
  return {
    chunks: c.chunks_count,
    vocab_occ: c.vocab_occurrences_count ?? c.learning_vocab_occurrences_count,
    grammar: c.grammar_points_count ?? c.learning_grammar_points_count,
    sentences: c.sentences_count ?? c.learning_sentences_count,
    exercises: c.exercises_count ?? c.learning_exercises_count,
    plans: c.episode_learning_plans_count,
  }
}

const eps = Object.keys(packages).map(Number)
const dbRows = await q(`
SELECT e.episode,
  (SELECT COUNT(*)::int FROM subtitle_chunks c WHERE c.work_slug='re-zero' AND c.episode=e.episode) AS chunks,
  (SELECT COUNT(*)::int FROM learning_vocab_occurrences o WHERE o.episode=e.episode) AS vocab_occ,
  (SELECT COUNT(*)::int FROM learning_grammar_points g WHERE g.episode=e.episode) AS grammar,
  (SELECT COUNT(*)::int FROM learning_sentences s WHERE s.episode=e.episode) AS sentences,
  (SELECT COUNT(*)::int FROM learning_exercises x WHERE x.episode=e.episode) AS exercises,
  (SELECT COUNT(*)::int FROM episode_learning_plans p WHERE p.episode=e.episode) AS plans
FROM episodes e
WHERE e.work_slug='re-zero' AND e.episode IN (${eps.join(',')})
ORDER BY e.episode
`)

let ok = true
console.log('ep\tfield\texpected\tactual\tstatus')
for (const ep of eps) {
  const exp = expectedCounts(ep)
  const act = dbRows.find((r) => r.episode === ep)
  if (!act) {
    console.log(`${ep}\tALL\t-\tMISSING\tFAIL`)
    ok = false
    continue
  }
  for (const field of ['chunks', 'vocab_occ', 'grammar', 'sentences', 'exercises', 'plans']) {
    const match = exp[field] === act[field]
    if (!match) ok = false
    console.log(`${ep}\t${field}\t${exp[field]}\t${act[field]}\t${match ? 'OK' : 'DIFF'}`)
  }
}
process.exit(ok ? 0 : 2)
