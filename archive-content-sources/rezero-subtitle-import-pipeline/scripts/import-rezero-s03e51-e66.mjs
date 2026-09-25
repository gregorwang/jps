/**
 * Import Re:Zero S03E51-E66 learning CSV packages into Supabase (jps).
 * Usage:
 *   set SUPABASE_ACCESS_TOKEN=sbp_...
 *   set DRY_RUN=1&& node scripts/import-rezero-s03e51-e66.mjs
 *   set DRY_RUN=0&& node scripts/import-rezero-s03e51-e66.mjs
 */
import { parse } from 'csv-parse/sync'
import { readFileSync, mkdirSync, writeFileSync, readdirSync, statSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '..')
const PROJECT_REF = 'qoatvdvbuleamyzsaldp'
const WORK_SLUG = 're-zero'
const EP_MIN = 51
const EP_MAX = 66
const BATCH_SIZE = 200
const PACKAGES = [
  join(ROOT, 'rezero_s03e51_e60_learning_csv_import_v9_all'),
  join(ROOT, 'rezero_s03e61_e66_learning_csv_import_v10_all'),
]
const TABLE_ORDER = [
  'episodes', 'subtitle_chunks', 'learning_vocab_items', 'learning_vocab_occurrences',
  'learning_grammar_points', 'learning_sentences', 'learning_exercises', 'episode_learning_plans',
]
const CSV_NAMES = Object.fromEntries(TABLE_ORDER.map((t) => [t, `${t}.csv`]))
const JSONB_COLS = new Set([
  'example_line_nos', 'tone_tags', 'vocab_item_ids', 'handwriting_vocab_ids',
  'shadowing_sentence_ids', 'grammar_point_ids', 'exercise_ids',
])
const BOOL_COLS = new Set([
  'usable_as_main_corpus', 'suitable_handwriting', 'suitable_shadowing', 'recommended_shadowing',
])
const INT_COLS = new Set([
  'episode', 'total_cues', 'ja_lines', 'zh_lines', 'usable_ja_lines', 'chunk_count', 'chunk_no',
  'start_line', 'end_line', 'line_count', 'occurrence_count', 'source_line_no', 'sort_order',
  'plan_slot', 'total_occurrences', 'episode_count',
])
const DELETE_ORDER = [
  'learning_exercises', 'episode_learning_plans', 'learning_grammar_points', 'learning_sentences',
  'learning_vocab_occurrences', 'subtitle_chunks', 'episodes',
]

function dryRun() {
  const v = process.env.DRY_RUN ?? '1'
  return !['0', 'false', 'False'].includes(v)
}

function readCsv(path) {
  let raw = readFileSync(path, 'utf8')
  if (raw.charCodeAt(0) === 0xfeff) raw = raw.slice(1)
  const rows = parse(raw, { columns: true, skip_empty_lines: true, relax_quotes: true })
  const headers = rows.length ? Object.keys(rows[0]) : parse(raw.split('\n')[0])
  return { headers, rows }
}

function discoverEpisodes() {
  const bundles = []
  for (const pkg of PACKAGES) {
    for (const name of readdirSync(pkg).sort()) {
      const dir = join(pkg, name)
      if (!statSync(dir).isDirectory()) continue
      const epPath = join(dir, CSV_NAMES.episodes)
      try { statSync(epPath) } catch { continue }
      const { rows } = readCsv(epPath)
      if (!rows.length) throw new Error(`Empty episodes.csv in ${dir}`)
      const episode = Number(rows[0].episode)
      if (episode < EP_MIN || episode > EP_MAX) throw new Error(`Episode ${episode} out of range in ${dir}`)
      const data = { episode, directory: dir, rows: {} }
      for (const table of TABLE_ORDER) {
        const p = join(dir, CSV_NAMES[table])
        const parsed = readCsv(p)
        data.rows[table] = parsed.rows
        data.headers = data.headers || {}
        data.headers[table] = parsed.headers
      }
      bundles.push(data)
    }
  }
  bundles.sort((a, b) => a.episode - b.episode)
  const missing = []
  for (let ep = EP_MIN; ep <= EP_MAX; ep++) if (!bundles.some((b) => b.episode === ep)) missing.push(ep)
  if (missing.length) throw new Error(`Missing episodes: ${missing.join(', ')}`)
  return bundles
}

async function runSql(token, query) {
  const res = await fetch(`https://api.supabase.com/v1/projects/${PROJECT_REF}/database/query`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ query }),
  })
  const text = await res.text()
  if (!res.ok) throw new Error(`${res.status} ${text}`)
  return text ? JSON.parse(text) : []
}

async function fetchLiveSchema(token) {
  const sql = `select table_name, column_name from information_schema.columns where table_schema = 'public' and table_name = any(array['episodes','subtitle_chunks','learning_vocab_items','learning_vocab_occurrences','learning_grammar_points','learning_sentences','learning_exercises','episode_learning_plans']) order by table_name, ordinal_position`
  const rows = await runSql(token, sql)
  const schema = {}
  for (const r of rows) (schema[r.table_name] ||= []).push(r.column_name)
  return schema
}

async function fetchExistingVocab(token) {
  const rows = await runSql(token, `select id, surface from public.learning_vocab_items where work_slug = '${WORK_SLUG}'`)
  const bySurface = {}
  for (const r of rows) bySurface[r.surface] = r.id
  return bySurface
}

async function fetchExistingCounts(token) {
  const scope = `work_slug = '${WORK_SLUG}' and episode between ${EP_MIN} and ${EP_MAX}`
  const counts = {}
  for (const table of DELETE_ORDER) {
    const rows = await runSql(token, `select count(*)::int as cnt from public.${table} where ${scope}`)
    counts[table] = rows[0].cnt
  }
  return counts
}

function validateHeaders(bundles, schema) {
  const errors = []
  for (const bundle of bundles) {
    for (const table of TABLE_ORDER) {
      const headers = bundle.headers[table]
      const dbCols = schema[table] || []
      const extra = headers.filter((h) => !dbCols.includes(h))
      const missing = dbCols.filter((c) => !headers.includes(c))
      if (extra.length) errors.push(`EP${String(bundle.episode).padStart(2, '0')} ${table}: CSV columns not in DB: ${extra.join(', ')}`)
      if (missing.length) errors.push(`EP${String(bundle.episode).padStart(2, '0')} ${table}: DB columns missing from CSV: ${missing.join(', ')}`)
      for (const row of bundle.rows[table]) {
        if (row.work_slug && row.work_slug !== WORK_SLUG) errors.push(`EP${bundle.episode} ${table} id=${row.id}: work_slug=${row.work_slug}`)
        if (row.episode && Number(row.episode) !== bundle.episode) errors.push(`EP${bundle.episode} ${table} id=${row.id}: episode=${row.episode}`)
      }
    }
  }
  return errors
}

function buildVocabMaps(bundles, existingBySurface) {
  const surfaceCsvIds = {}
  const csvVocabById = {}
  for (const bundle of bundles) {
    for (const row of bundle.rows.learning_vocab_items) {
      ;(surfaceCsvIds[row.surface] ||= new Set()).add(row.id)
      csvVocabById[row.id] = row
    }
  }
  const idRemap = {}
  const keptRows = {}
  for (const surface of Object.keys(surfaceCsvIds).sort()) {
    const ids = [...surfaceCsvIds[surface]].sort()
    let canonical = existingBySurface[surface]
    if (!canonical) {
      canonical = ids[0]
      keptRows[canonical] = csvVocabById[canonical]
    }
    for (const vid of ids) if (vid !== canonical) idRemap[vid] = canonical
  }
  return { idRemap, vocabRows: Object.values(keptRows), surfaceCsvIds }
}

function remapId(vid, idRemap) {
  if (!vid) return vid
  let cur = vid
  const seen = new Set()
  while (idRemap[cur] && !seen.has(cur)) {
    seen.add(cur)
    cur = idRemap[cur]
  }
  return cur
}

function parseRow(table, row, idRemap) {
  const out = {}
  for (const [key, val] of Object.entries(row)) {
    if (val === '' || val === undefined || val === null) { out[key] = null; continue }
    if (JSONB_COLS.has(key)) {
      let parsed = JSON.parse(val)
      if ((key.endsWith('_ids') || key === 'example_line_nos') && Array.isArray(parsed)) {
        parsed = parsed.map((x) => remapId(x, idRemap) || x)
      }
      out[key] = parsed
    } else if (BOOL_COLS.has(key)) {
      out[key] = ['true', 't', '1', 'yes'].includes(String(val).toLowerCase())
    } else if (INT_COLS.has(key)) {
      out[key] = Number(val)
    } else {
      out[key] = val
    }
  }
  if (table === 'learning_vocab_occurrences' || table === 'learning_exercises') {
    out.vocab_item_id = remapId(out.vocab_item_id, idRemap)
  }
  if (table === 'episode_learning_plans') {
    for (const col of ['vocab_item_ids', 'handwriting_vocab_ids', 'shadowing_sentence_ids', 'grammar_point_ids', 'exercise_ids']) {
      if (Array.isArray(out[col])) out[col] = out[col].map((x) => remapId(x, idRemap) || x)
    }
  }
  return out
}

function mergeOccurrences(rows) {
  const groups = new Map()
  for (const row of rows) {
    const key = `${row.episode}\0${row.vocab_item_id}`
    if (!groups.has(key)) groups.set(key, { ...row, occurrence_count: 0, example_line_nos: [] })
    const g = groups.get(key)
    g.occurrence_count += Number(row.occurrence_count || 0)
    g.example_line_nos = [...new Set([...(g.example_line_nos || []), ...(row.example_line_nos || [])])].sort((a, b) => a - b)
  }
  return [...groups.values()]
}

function validateReferences(bundles, idRemap) {
  const errors = []
  const episodeIds = new Set(bundles.map((b) => `re-zero-ep${b.episode}`))
  const vocabIds = new Set()
  const sentenceIds = new Set()
  const grammarIds = new Set()
  const exerciseIds = new Set()
  for (const bundle of bundles) {
    for (const row of bundle.rows.learning_vocab_items) vocabIds.add(remapId(row.id, idRemap))
    for (const row of bundle.rows.learning_sentences) sentenceIds.add(row.id)
    for (const row of bundle.rows.learning_grammar_points) grammarIds.add(row.id)
    for (const row of bundle.rows.learning_exercises) exerciseIds.add(row.id)
  }
  const refSets = {
    vocab_item_ids: vocabIds,
    handwriting_vocab_ids: vocabIds,
    shadowing_sentence_ids: sentenceIds,
    grammar_point_ids: grammarIds,
    exercise_ids: exerciseIds,
  }
  for (const bundle of bundles) {
    const epId = `re-zero-ep${bundle.episode}`
    for (const table of ['learning_vocab_occurrences', 'learning_grammar_points', 'learning_sentences', 'learning_exercises', 'episode_learning_plans']) {
      for (const row of bundle.rows[table]) {
        if (row.episode_id && row.episode_id !== epId) errors.push(`EP${bundle.episode} ${table} id=${row.id}: bad episode_id=${row.episode_id}`)
        if (row.episode_id && !episodeIds.has(row.episode_id)) errors.push(`EP${bundle.episode} ${table} id=${row.id}: unknown episode_id=${row.episode_id}`)
      }
    }
    for (const row of bundle.rows.learning_vocab_occurrences) {
      const vid = remapId(row.vocab_item_id, idRemap)
      if (!vocabIds.has(vid)) errors.push(`EP${bundle.episode} occ id=${row.id}: missing vocab_item_id=${row.vocab_item_id}`)
    }
    for (const row of bundle.rows.learning_exercises) {
      if (['vocab_meaning', 'vocab_reading'].includes(row.exercise_type)) {
        const vid = remapId(row.vocab_item_id, idRemap)
        if (vid && !vocabIds.has(vid)) errors.push(`EP${bundle.episode} ex id=${row.id}: missing vocab_item_id=${row.vocab_item_id}`)
      }
    }
    for (const row of bundle.rows.episode_learning_plans) {
      for (const col of Object.keys(refSets)) {
        if (!row[col]) continue
        const ids = JSON.parse(row[col])
        for (const ref of ids) {
          const mapped = remapId(ref, idRemap) || ref
          if (!refSets[col].has(mapped)) errors.push(`EP${bundle.episode} plan id=${row.id}: ${col} missing ref=${ref}`)
        }
      }
    }
  }
  return errors
}

function collectParsedRows(bundles, idRemap) {
  const out = Object.fromEntries(TABLE_ORDER.map((t) => [t, []]))
  for (const bundle of bundles) {
    for (const table of TABLE_ORDER) {
      let rows = bundle.rows[table].map((r) => parseRow(table, r, idRemap))
      if (table === 'learning_vocab_occurrences') rows = mergeOccurrences(rows)
      out[table].push(...rows)
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
  const cols = Object.keys(rows[0])
  const values = rows.map((row) => `(${cols.map((c) => sqlLiteral(row[c], { jsonb: JSONB_COLS.has(c) })).join(', ')})`).join(',\n')
  return `INSERT INTO public.${table} (${cols.join(', ')})\nVALUES\n${values};`
}

function buildVocabInsert(rows) {
  if (!rows.length) return ''
  const cols = Object.keys(rows[0])
  const valueRows = rows.map((row) => `(${cols.map((c) => sqlLiteral(row[c])).join(', ')})`).join(',\n')
  const colList = cols.join(', ')
  return `INSERT INTO public.learning_vocab_items (${colList})\nSELECT ${cols.map((c) => `v.${c}`).join(', ')} FROM (VALUES\n${valueRows}\n) AS v(${colList})\nWHERE NOT EXISTS (SELECT 1 FROM public.learning_vocab_items t WHERE t.work_slug = v.work_slug AND t.surface = v.surface);`
}

function generateSqlPreview(parsed, vocabRows) {
  const scope = `work_slug = '${WORK_SLUG}' and episode between ${EP_MIN} and ${EP_MAX}`
  const preview = DELETE_ORDER.map((table) => `-- delete\nDELETE FROM public.${table} WHERE ${scope};`)
  for (const table of TABLE_ORDER) {
    const rows = table === 'learning_vocab_items' ? vocabRows : parsed[table]
    if (!rows.length) continue
    preview.push(table === 'learning_vocab_items' ? buildVocabInsert(rows.slice(0, 2)) : buildInsert(table, rows.slice(0, 2)))
  }
  return preview
}

function printReport(ctx) {
  const {
    bundles, schema, headerErrors, refErrors, idRemap, vocabRows, surfaceCsvIds,
    existingBySurface, existingDeleteCounts, parsed, sqlPreview,
  } = ctx
  const incomingSurfaces = new Set(Object.keys(surfaceCsvIds))
  const existingSurfaces = new Set(Object.keys(existingBySurface))
  const overlap = [...incomingSurfaces].filter((s) => existingSurfaces.has(s)).sort()
  const dupInside = [...incomingSurfaces].filter((s) => surfaceCsvIds[s].size > 1).sort()
  const rawVocab = bundles.reduce((n, b) => n + b.rows.learning_vocab_items.length, 0)

  console.log('='.repeat(72))
  console.log('DRY_RUN REPORT - Re:Zero S03E51-E66 learning CSV import')
  console.log('='.repeat(72))
  console.log(`mode: ${dryRun() ? 'DRY_RUN (no writes)' : 'LIVE IMPORT'}`)
  console.log(`work_slug: ${WORK_SLUG}`)
  console.log(`episodes: ${EP_MIN}-${EP_MAX} (${bundles.length} folders)`)
  console.log(`packages: ${PACKAGES.map((p) => p.split(/[/\\]/).pop()).join(', ')}`)
  console.log()

  console.log('--- schema / header validation ---')
  if (headerErrors.length) headerErrors.forEach((e) => console.log(`  ERROR: ${e}`))
  else TABLE_ORDER.forEach((table) => console.log(`  OK ${table}: ${(schema[table] || []).length} DB columns match CSV headers`))
  console.log()

  console.log('--- row counts (raw CSV) ---')
  for (const bundle of bundles) {
    const parts = [`EP${String(bundle.episode).padStart(2, '0')}`]
    for (const table of TABLE_ORDER) parts.push(`${table}=${bundle.rows[table].length}`)
    console.log('  ' + parts.join(' | '))
  }
  console.log()

  console.log('--- surface overlap analysis ---')
  console.log(`incoming_vocab_surface_count: ${incomingSurfaces.size}`)
  console.log(`existing_vocab_surface_count: ${existingSurfaces.size}`)
  console.log(`overlap_surface_count: ${overlap.length}`)
  console.log('overlap_surface_sample (up to 30):')
  for (const s of overlap.slice(0, 30)) {
    console.log(`  ${JSON.stringify(s)} -> existing_id=${existingBySurface[s]} | csv_ids=${[...surfaceCsvIds[s]].slice(0, 3).join(', ')}`)
  }
  console.log(`duplicate_surface_inside_import_count: ${dupInside.length}`)
  console.log('duplicate_surface_inside_import_sample (up to 30):')
  for (const s of dupInside.slice(0, 30)) {
    console.log(`  ${JSON.stringify(s)} -> csv_ids=${[...surfaceCsvIds[s]].join(', ')}`)
  }
  console.log()

  console.log('--- vocab dedup ---')
  console.log(`raw_vocab_rows: ${rawVocab}`)
  console.log(`vocab_id_remap_count: ${Object.keys(idRemap).length}`)
  console.log(`deduped_vocab_insert_rows: ${vocabRows.length}`)
  if (Object.keys(idRemap).length) {
    console.log('id_remap sample:')
    for (const [oldId, newId] of Object.entries(idRemap).slice(0, 10)) console.log(`  ${oldId} -> ${newId}`)
  }
  console.log()

  console.log('--- reference validation ---')
  if (refErrors.length) {
    refErrors.slice(0, 50).forEach((e) => console.log(`  ERROR: ${e}`))
    if (refErrors.length > 50) console.log(`  ... and ${refErrors.length - 50} more`)
  } else console.log('  OK: episode_id / vocab_item_id / plan JSON refs')
  console.log()

  console.log('--- existing rows to delete (EP51-66) ---')
  for (const table of DELETE_ORDER) console.log(`  ${table}: ${existingDeleteCounts[table] ?? 0}`)
  console.log()

  console.log('--- rows to write (after transform) ---')
  for (const table of TABLE_ORDER) {
    const n = table === 'learning_vocab_items' ? vocabRows.length : parsed[table].length
    console.log(`  ${table}: ${n}`)
  }
  console.log()

  console.log('--- SQL preview (first statements, truncated) ---')
  sqlPreview.slice(0, 6).forEach((stmt, i) => {
    console.log(`-- statement ${i + 1}`)
    console.log(stmt.replace(/\s+/g, ' ').slice(0, 240) + (stmt.length > 240 ? ' ...' : ''))
  })
  console.log()

  console.log('--- per-episode summary (post-merge occ / shadowing) ---')
  for (const bundle of bundles) {
    const merged = mergeOccurrences(bundle.rows.learning_vocab_occurrences.map((r) => parseRow('learning_vocab_occurrences', r, idRemap)))
    const shadow = bundle.rows.learning_sentences.filter((r) => ['true', 't', '1'].includes(String(r.recommended_shadowing).toLowerCase())).length
    console.log(`  EP${String(bundle.episode).padStart(2, '0')}: occ=${merged.length} grammar=${bundle.rows.learning_grammar_points.length} sent=${bundle.rows.learning_sentences.length} shadow=${shadow} ex=${bundle.rows.learning_exercises.length} plans=${bundle.rows.episode_learning_plans.length} chunks=${bundle.rows.subtitle_chunks.length}`)
  }
  console.log()

  if (headerErrors.length || refErrors.length) {
    console.log('RESULT: FAILED - fix errors before DRY_RUN=0')
    process.exit(1)
  }
  if (dryRun()) console.log('RESULT: PASSED - ready for DRY_RUN=0 after user review')
}

const SCOPE = `work_slug = '${WORK_SLUG}' and episode between ${EP_MIN} and ${EP_MAX}`

async function generateImportReport(token, backupDir, expected) {
  const lines = []
  const w = (s = '') => lines.push(s)

  w('# Re:Zero S03E51-E66 Import Report')
  w('')
  w(`Generated: ${new Date().toISOString()}`)
  w(`Backup: ${backupDir}`)
  w(`Scope: ${SCOPE}`)
  w('')

  const tableCounts = {}
  for (const table of TABLE_ORDER) {
    if (table === 'learning_vocab_items') {
      const rows = await runSql(token, `
        select count(distinct vocab_item_id)::int as cnt
        from public.learning_vocab_occurrences
        where ${SCOPE}
      `)
      tableCounts[table] = rows[0].cnt
    } else {
      const rows = await runSql(token, `select count(*)::int as cnt from public.${table} where ${SCOPE}`)
      tableCounts[table] = rows[0].cnt
    }
  }

  w('## Table row counts (EP51-66, work_slug=re-zero)')
  w('')
  w('| Table | Rows | Expected |')
  w('|-------|-----:|---------:|')
  for (const table of TABLE_ORDER) {
    const exp = table === 'learning_vocab_items' ? `${expected.vocabInsert} new inserts (+ ${expected.overlapReuse} reused ids)` : (expected.parsed[table]?.length ?? '-')
    w(`| ${table} | ${tableCounts[table]} | ${exp} |`)
  }
  w('')

  const epRows = await runSql(token, `
    select e.episode,
      (select count(*)::int from public.subtitle_chunks c where c.work_slug='${WORK_SLUG}' and c.episode=e.episode) as chunks,
      (select count(*)::int from public.learning_vocab_occurrences o where o.work_slug='${WORK_SLUG}' and o.episode=e.episode) as occurrences,
      (select count(*)::int from public.learning_grammar_points g where g.work_slug='${WORK_SLUG}' and g.episode=e.episode) as grammar,
      (select count(*)::int from public.learning_sentences s where s.work_slug='${WORK_SLUG}' and s.episode=e.episode) as sentences,
      (select count(*)::int from public.learning_exercises x where x.work_slug='${WORK_SLUG}' and x.episode=e.episode) as exercises,
      (select count(*)::int from public.episode_learning_plans p where p.work_slug='${WORK_SLUG}' and p.episode=e.episode) as plans
    from public.episodes e
    where e.work_slug='${WORK_SLUG}' and e.episode between ${EP_MIN} and ${EP_MAX}
    order by e.episode
  `)

  w('## Per-episode counts')
  w('')
  w('| EP | episodes | chunks | occ | grammar | sentences | exercises | plans |')
  w('|---:|---------:|-------:|----:|--------:|----------:|----------:|------:|')
  for (const r of epRows) {
    w(`| ${r.episode} | 1 | ${r.chunks} | ${r.occurrences} | ${r.grammar} | ${r.sentences} | ${r.exercises} | ${r.plans} |`)
  }
  w('')

  const exTypes = await runSql(token, `
    select exercise_type, count(*)::int as cnt
    from public.learning_exercises
    where ${SCOPE}
    group by exercise_type
    order by exercise_type
  `)
  w('## exercise_type distribution')
  w('')
  for (const r of exTypes) w(`- ${r.exercise_type}: ${r.cnt}`)
  w('')

  const badFk = await runSql(token, `
    select count(*)::int as cnt
    from public.learning_exercises e
    left join public.learning_vocab_items v on v.id = e.vocab_item_id
    where e.work_slug = '${WORK_SLUG}' and e.episode between ${EP_MIN} and ${EP_MAX}
      and e.exercise_type in ('vocab_meaning', 'vocab_reading')
      and v.id is null
  `)
  w(`## bad_vocab_fk: ${badFk[0].cnt}`)
  w('')

  const shadow = await runSql(token, `
    select episode, count(*)::int as cnt
    from public.learning_sentences
    where ${SCOPE} and recommended_shadowing = true
    group by episode
    order by episode
  `)
  const shadowTotal = shadow.reduce((n, r) => n + r.cnt, 0)
  w(`## recommended_shadowing=true: ${shadowTotal} total`)
  w('')
  for (const r of shadow) w(`- EP${String(r.episode).padStart(2, '0')}: ${r.cnt}`)
  w('')

  const longShadow = await runSql(token, `
    select episode, source_line_no, length(ja_text)::int as len, ja_text
    from public.learning_sentences
    where ${SCOPE} and recommended_shadowing = true and length(ja_text) > 90
    order by episode, len desc
  `)
  w(`## recommended_shadowing > 90 chars: ${longShadow.length} rows`)
  w('')
  if (longShadow.length) {
    for (const r of longShadow.slice(0, 20)) {
      w(`- EP${r.episode} L${r.source_line_no} (${r.len}): ${r.ja_text}`)
    }
  } else {
    w('None.')
  }
  w('')

  const epRange = await runSql(token, `
    select min(episode)::int as min_ep, max(episode)::int as max_ep, count(*)::int as cnt
    from public.episodes where ${SCOPE}
  `)
  w('## Episode range check')
  w('')
  w(`- min episode: ${epRange[0].min_ep} (expected ${EP_MIN})`)
  w(`- max episode: ${epRange[0].max_ep} (expected ${EP_MAX})`)
  w(`- episode rows: ${epRange[0].cnt} (expected ${EP_MAX - EP_MIN + 1})`)
  w('')

  const rezeroCounts = {}
  let rezeroTotal = 0
  for (const table of [...TABLE_ORDER, 'learning_vocab_items']) {
    const rows = await runSql(token, `select count(*)::int as cnt from public.${table} where work_slug = 'rezero'`)
    rezeroCounts[table] = rows[0].cnt
    rezeroTotal += rows[0].cnt
  }
  w('## work_slug=rezero row counts (must all be 0)')
  w('')
  for (const [table, cnt] of Object.entries(rezeroCounts)) w(`- ${table}: ${cnt}`)
  w(`- **total: ${rezeroTotal}**`)
  w('')

  const ep525 = await runSql(token, `select count(*)::int as cnt from public.episodes where work_slug='${WORK_SLUG}' and episode between 5 and 25`)
  w(`## EP05-25 episodes preserved: ${ep525[0].cnt} rows (unchanged baseline)`)
  w('')

  const reportPath = join(ROOT, 'output', 'learning', 'import_report_s03e51_e66.md')
  writeFileSync(reportPath, lines.join('\n') + '\n', 'utf8')
  console.log(`Import report written: ${reportPath}`)
  return reportPath
}

async function runLiveImport(token, parsed, vocabRows, meta) {
  const ts = new Date().toISOString().replace(/[-:]/g, '').replace(/\..+/, 'Z')
  const backupDir = join(ROOT, 'backups', `rezero_s03e51_e66_before_import_${ts}`)
  mkdirSync(backupDir, { recursive: true })
  for (const table of DELETE_ORDER) {
    const rows = await runSql(token, `select * from public.${table} where ${SCOPE}`)
    writeFileSync(join(backupDir, `${table}.json`), JSON.stringify(rows, null, 2), 'utf8')
  }

  const oldVocabRows = await runSql(token, `select distinct vocab_item_id as id from public.learning_vocab_occurrences where ${SCOPE}`)
  const oldVocabIds = oldVocabRows.map((r) => r.id)

  for (const table of DELETE_ORDER) await runSql(token, `delete from public.${table} where ${SCOPE}`)

  if (oldVocabIds.length) {
    const idsSql = oldVocabIds.map((id) => `'${id.replace(/'/g, "''")}'`).join(', ')
    await runSql(token, `delete from public.learning_vocab_items v where v.work_slug = '${WORK_SLUG}' and v.id in (${idsSql}) and not exists (select 1 from public.learning_vocab_occurrences o where o.vocab_item_id = v.id)`)
  }

  const statements = []
  for (const table of TABLE_ORDER) {
    const rows = table === 'learning_vocab_items' ? vocabRows : parsed[table]
    if (table === 'learning_vocab_items') {
      for (let i = 0; i < rows.length; i += BATCH_SIZE) {
        const stmt = buildVocabInsert(rows.slice(i, i + BATCH_SIZE))
        if (stmt) statements.push(stmt)
      }
      continue
    }
    for (let i = 0; i < rows.length; i += BATCH_SIZE) statements.push(buildInsert(table, rows.slice(i, i + BATCH_SIZE)))
  }

  for (let i = 0; i < statements.length; i++) {
    process.stdout.write(`[${i + 1}/${statements.length}] running batch (${statements[i].length} bytes)... `)
    const t0 = Date.now()
    await runSql(token, statements[i])
    console.log(`${Date.now() - t0}ms`)
  }
  console.log(`Import complete. Backup: ${backupDir}`)
  await generateImportReport(token, backupDir, meta)
  return backupDir
}

async function main() {
  const token = process.env.SUPABASE_ACCESS_TOKEN?.trim()
  if (!token) {
    console.error('ERROR: set SUPABASE_ACCESS_TOKEN in environment (do not pass on CLI)')
    process.exit(1)
  }

  const bundles = discoverEpisodes()
  const schema = await fetchLiveSchema(token)
  const headerErrors = validateHeaders(bundles, schema)
  const existingBySurface = await fetchExistingVocab(token)
  const existingDeleteCounts = await fetchExistingCounts(token)
  const { idRemap, vocabRows: rawVocabRows, surfaceCsvIds } = buildVocabMaps(bundles, existingBySurface)
  const vocabRows = rawVocabRows.map((r) => parseRow('learning_vocab_items', r, idRemap))
  const parsed = collectParsedRows(bundles, idRemap)
  const refErrors = validateReferences(bundles, idRemap)
  const sqlPreview = generateSqlPreview(parsed, vocabRows)

  printReport({ bundles, schema, headerErrors, refErrors, idRemap, vocabRows, surfaceCsvIds, existingBySurface, existingDeleteCounts, parsed, sqlPreview })

  if (!dryRun()) {
    const overlapCount = Object.keys(surfaceCsvIds).filter((s) => existingBySurface[s]).length
    await runLiveImport(token, parsed, vocabRows, {
      parsed,
      vocabInsert: vocabRows.length,
      overlapReuse: overlapCount,
    })
  }
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
