/**
 * Re:Zero EP11+ CSV schema -> Supabase (v2 format)
 */
import { parse } from 'csv-parse/sync'
import { createHash } from 'node:crypto'
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
  console.error('Usage: node scripts/import-rezero-episode-v2.mjs --episode N --package rezero_epNN_learning_csv_import [--write-batches] [--run]')
  process.exit(1)
}

const CSV_DIR = join(__dirname, '..', PACKAGE)
const BATCH_DIR = join(CSV_DIR, '.sql_batches')
const PROJECT_REF = 'qoatvdvbuleamyzsaldp'
const EPISODE_ID = `re-zero-s01e${String(EPISODE).padStart(2, '0')}`
const BATCH_SIZE = 200

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

const JSONB_COLS = new Set([
  'example_line_nos',
  'tone_tags',
  'vocab_item_ids',
  'handwriting_vocab_ids',
  'shadowing_sentence_ids',
  'grammar_point_ids',
  'exercise_ids',
])

function readCsv(table) {
  let raw = readFileSync(join(CSV_DIR, FILE_MAP[table]), 'utf8')
  if (raw.charCodeAt(0) === 0xfeff) raw = raw.slice(1)
  return parse(raw, { columns: true, skip_empty_lines: true, relax_quotes: true })
}

function emptyToNull(v) {
  if (v === undefined || v === null || v === '') return null
  return v
}

function parseTags(tags) {
  if (!tags) return null
  return String(tags).split(',').map((t) => t.trim()).filter(Boolean)
}

function parseJsonArray(s) {
  if (!s) return null
  return JSON.parse(s)
}

function resolveChoiceAnswer(row) {
  if (!row.correct_answer) return null
  if (!row.choices_json) return row.correct_answer
  try {
    const choices = JSON.parse(row.choices_json)
    const hit = choices.find((c) => c.id === row.correct_answer)
    return hit?.text ?? row.correct_answer
  } catch {
    return row.correct_answer
  }
}

function exerciseSortOrder(id) {
  const m = String(id).match(/-(\d+)$/)
  return m ? Number(m[1]) : null
}

function loadEpisodeMeta() {
  const row = readCsv('episodes')[0]
  const chunkCount = readCsv('subtitle_chunks').length
  return {
    workDisplayName: row.work_title,
    sourceLang: row.source_lang || 'ja',
    chunkCount,
    row,
  }
}

function derivedVocabId(workSlug, surface) {
  const hash = createHash('sha256').update(`${workSlug}:${surface}`).digest('hex').slice(0, 10)
  return `vocab-re-zero-${hash}`
}

function vocabRowFromCsv(r, id) {
  return {
    id,
    work_slug: r.work_slug,
    surface: r.surface,
    reading: r.reading,
    romaji: r.romaji,
    meaning_zh: r.meaning_zh,
    pos: r.part_of_speech,
    jlpt_level: r.jlpt_level,
    suitable_handwriting: Boolean(r.handwriting_hint_zh),
    suitable_shadowing: Boolean(r.shadowing_hint_zh),
    anime_tone_note: [r.notes_zh, r.shadowing_hint_zh].filter(Boolean).join(' ') || null,
    real_world_note: [r.sentence_goal_zh, r.review_prompt_zh].filter(Boolean).join(' ') || null,
  }
}

function prepareVocabImport(bySurface, byId) {
  const idRemap = {}
  const rows = []
  const localSurface = { ...bySurface }
  const localId = { ...byId }

  for (const r of readCsv('learning_vocab_items')) {
    const existingIdForSurface = localSurface[r.surface]
    if (existingIdForSurface) {
      if (existingIdForSurface !== r.id) idRemap[r.id] = existingIdForSurface
      continue
    }

    let insertId = r.id
    if (localId[insertId] && localId[insertId] !== r.surface) {
      insertId = derivedVocabId(r.work_slug, r.surface)
      if (localId[insertId] && localId[insertId] !== r.surface) {
        insertId = derivedVocabId(r.work_slug, `${r.surface}:alt`)
      }
      idRemap[r.id] = insertId
    }

    rows.push(vocabRowFromCsv(r, insertId))
    localSurface[r.surface] = insertId
    localId[insertId] = r.surface
  }

  return { idRemap, rows }
}

function remapId(id, idRemap) {
  if (!id) return id
  return idRemap[id] ?? id
}

function remapIdList(ids, idRemap) {
  if (!ids) return ids
  return ids.map((id) => remapId(id, idRemap))
}

function transformEpisodes(meta) {
  const r = meta.row
  const sourceFiles = JSON.stringify({
    ja_file: r.source_filename,
    source_lang: r.source_lang,
    episode_code: r.episode_code,
    title_ja: r.title_ja,
    title_zh: r.title_zh,
    theme_summary_zh: r.theme_summary_zh,
    note: r.notes_zh,
  })
  return [{
    id: r.id,
    work_slug: r.work_slug,
    work_display_name: r.work_title,
    episode: Number(r.episode_number),
    source_files: sourceFiles,
    total_cues: Number(r.cue_count),
    ja_lines: Number(r.cue_count),
    zh_lines: 0,
    usable_ja_lines: Number(r.usable_cue_count),
    chunk_count: meta.chunkCount,
    usable_as_main_corpus: true,
  }]
}

function transformSubtitleChunks(meta) {
  return readCsv('subtitle_chunks').map((r) => ({
    id: r.id,
    work_slug: 're-zero',
    work_display_name: meta.workDisplayName,
    episode: EPISODE,
    chunk_no: Number(r.chunk_index),
    start_line: Number(r.start_cue_index),
    end_line: Number(r.end_cue_index),
    start_time: r.start_time,
    end_time: r.end_time,
    line_count: Number(r.line_count),
    language: 'ja',
    source: meta.sourceLang.includes('netflix') ? 'netflix-ja-cc-srt' : meta.sourceLang,
  }))
}

function transformVocabOccurrences(idRemap) {
  const groups = new Map()
  for (const r of readCsv('learning_vocab_occurrences')) {
    const vocabItemId = remapId(r.vocab_item_id, idRemap)
    if (!groups.has(vocabItemId)) {
      groups.set(vocabItemId, {
        id: r.id,
        work_slug: 're-zero',
        episode_id: r.episode_id,
        episode: EPISODE,
        vocab_item_id: vocabItemId,
        occurrence_count: 0,
        example_line_nos: [],
      })
    }
    const g = groups.get(vocabItemId)
    g.occurrence_count++
    g.example_line_nos.push(Number(r.cue_index))
  }
  return [...groups.values()].map((g) => ({
    ...g,
    example_line_nos: [...new Set(g.example_line_nos)].sort((a, b) => a - b),
  }))
}

function transformGrammarPoints() {
  const groups = new Map()
  const idRemap = {}
  for (const r of readCsv('learning_grammar_points')) {
    const key = `${r.pattern}\0${r.ja_example}`
    if (groups.has(key)) {
      idRemap[r.id] = groups.get(key).id
      continue
    }
    groups.set(key, {
      id: r.id,
      work_slug: 're-zero',
      episode_id: r.episode_id,
      episode: EPISODE,
      pattern: r.pattern,
      function_zh: r.title_zh,
      ja_example: r.example_ja,
      explanation_zh: r.explanation_zh,
      pragmatics_note: r.training_note_zh,
      real_world_note: r.grammar_tags,
      difficulty: r.jlpt_level,
      source_line_no: Number(r.cue_index),
      sort_order: Number(r.priority),
    })
  }
  return { rows: [...groups.values()], idRemap }
}

function transformSentences() {
  return readCsv('learning_sentences').map((r) => ({
    id: r.id,
    work_slug: 're-zero',
    episode_id: r.episode_id,
    episode: EPISODE,
    ja_text: r.ja_text,
    reading: emptyToNull(r.reading),
    romaji: emptyToNull(r.romaji),
    meaning_zh: r.meaning_zh,
    tone_tags: parseTags(r.tags),
    difficulty: null,
    recommended_shadowing: Number(r.shadowing_level) >= 2,
    source_line_no: Number(r.cue_index),
    sort_order: Number(r.sentence_index),
  }))
}

function transformExercises(vocabIdRemap) {
  const groups = new Map()
  const idRemap = {}
  for (const r of readCsv('learning_exercises')) {
    const prompt = r.prompt_ja ? `${r.prompt_zh}\n${r.prompt_ja}` : r.prompt_zh
    const key = `${r.exercise_type}\0${prompt}`
    if (groups.has(key)) {
      idRemap[r.id] = groups.get(key).id
      continue
    }
    groups.set(key, {
      id: r.id,
      work_slug: 're-zero',
      episode_id: r.episode_id,
      episode: EPISODE,
      exercise_type: r.exercise_type,
      prompt,
      answer: resolveChoiceAnswer(r),
      hint: r.explanation_zh,
      difficulty: r.difficulty,
      vocab_item_id: r.source_type === 'vocab_item' ? remapId(r.source_id, vocabIdRemap) : null,
      sort_order: exerciseSortOrder(r.id),
    })
  }
  return { rows: [...groups.values()], idRemap }
}

function transformPlans(idRemap) {
  return readCsv('episode_learning_plans').map((r) => {
    const targetIds = parseJsonArray(r.target_ids_json) ?? []
    const notes = [
      r.section_title_zh,
      r.goal_zh,
      r.method_zh,
      r.estimated_minutes ? `${r.estimated_minutes}分钟` : null,
      r.review_hint_zh ? `复习：${r.review_hint_zh}` : null,
      r.target_type === 'chunks' ? `chunks=${r.target_ids_json}` : null,
    ].filter(Boolean).join(' | ')

    const out = {
      id: r.id,
      work_slug: 're-zero',
      episode_id: r.episode_id,
      episode: EPISODE,
      plan_slot: Number(r.plan_order),
      vocab_item_ids: null,
      handwriting_vocab_ids: null,
      shadowing_sentence_ids: null,
      grammar_point_ids: null,
      exercise_ids: null,
      notes,
    }

    if (r.target_type === 'vocab_tags' || r.target_type === 'vocab_item') {
      out.vocab_item_ids = remapIdList(targetIds, idRemap)
      out.handwriting_vocab_ids = remapIdList(targetIds, idRemap)
    } else if (r.target_type === 'sentences') {
      out.shadowing_sentence_ids = remapIdList(targetIds, idRemap)
    } else if (r.target_type === 'exercises') {
      out.exercise_ids = remapIdList(targetIds, idRemap)
    } else if (r.target_type === 'grammar' || r.target_type === 'grammar_points') {
      out.grammar_point_ids = remapIdList(targetIds, idRemap)
    }
    return out
  })
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
  const values = rows.map((row) => {
    const parts = cols.map((c) => (JSONB_COLS.has(c) ? sqlLiteral(row[c], { jsonb: true }) : sqlLiteral(row[c])))
    return `(${parts.join(', ')})`
  }).join(',\n')
  return `INSERT INTO ${table} (${cols.join(', ')})\nVALUES\n${values};`
}

function buildVocabItemsImport(rows) {
  if (!rows.length) return []
  const cols = Object.keys(rows[0])
  const valueRows = rows
    .map((row) => `(${cols.map((c) => sqlLiteral(row[c])).join(', ')})`)
    .join(',\n')
  const colList = cols.join(', ')
  return [
    `INSERT INTO learning_vocab_items (${colList}) SELECT ${cols.map((c) => `v.${c}`).join(', ')} FROM (VALUES ${valueRows}) AS v(${colList}) WHERE NOT EXISTS (SELECT 1 FROM learning_vocab_items t WHERE t.work_slug = v.work_slug AND t.surface = v.surface);`,
  ]
}

function chunk(arr, size) {
  const out = []
  for (let i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size))
  return out
}

function generateImportSql(vocabRows = [], idRemap = {}) {
  const meta = loadEpisodeMeta()
  const statements = []
  const { rows: grammarRows, idRemap: grammarIdRemap } = transformGrammarPoints()
  const { rows: exerciseRows, idRemap: exerciseIdRemap } = transformExercises(idRemap)
  const mergedRemap = { ...idRemap, ...grammarIdRemap, ...exerciseIdRemap }

  statements.push(`DELETE FROM learning_exercises WHERE episode_id = '${EPISODE_ID}';`)
  statements.push(`DELETE FROM learning_sentences WHERE episode_id = '${EPISODE_ID}';`)
  statements.push(`DELETE FROM learning_grammar_points WHERE episode_id = '${EPISODE_ID}';`)
  statements.push(`DELETE FROM learning_vocab_occurrences WHERE episode_id = '${EPISODE_ID}';`)
  statements.push(`DELETE FROM episode_learning_plans WHERE episode_id = '${EPISODE_ID}';`)
  statements.push(`DELETE FROM subtitle_chunks WHERE work_slug = 're-zero' AND episode = ${EPISODE};`)
  statements.push(`DELETE FROM episodes WHERE id = '${EPISODE_ID}';`)

  const tables = [
    ['episodes', transformEpisodes(meta)],
    ['subtitle_chunks', transformSubtitleChunks(meta)],
    ['learning_vocab_items', vocabRows],
    ['learning_vocab_occurrences', transformVocabOccurrences(idRemap)],
    ['learning_grammar_points', grammarRows],
    ['learning_sentences', transformSentences()],
    ['learning_exercises', exerciseRows],
    ['episode_learning_plans', transformPlans(mergedRemap)],
  ]

  for (const [table, rows] of tables) {
    for (const batch of chunk(rows, BATCH_SIZE)) {
      if (table === 'learning_vocab_items') statements.push(...buildVocabItemsImport(batch))
      else if (batch.length) statements.push(buildInsert(table, batch))
    }
  }
  return statements
}

async function fetchExistingVocab(token) {
  const text = await runSql(token, `SELECT id, surface FROM learning_vocab_items WHERE work_slug = 're-zero'`)
  const rows = JSON.parse(text)
  const bySurface = {}
  const byId = {}
  for (const r of rows) {
    bySurface[r.surface] = r.id
    byId[r.id] = r.surface
  }
  return { bySurface, byId }
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

async function main() {
  let vocabRows = []
  let idRemap = {}
  const token = process.env.SUPABASE_ACCESS_TOKEN || args.find((a) => a.startsWith('sbp_'))
  if (run) {
    if (!token) {
      console.error('Missing SUPABASE_ACCESS_TOKEN')
      process.exit(1)
    }
    const existing = await fetchExistingVocab(token)
    const prepared = prepareVocabImport(existing.bySurface, existing.byId)
    idRemap = prepared.idRemap
    vocabRows = prepared.rows
    const remapped = Object.keys(idRemap).length
    const inserting = vocabRows.length
    console.log(`EP${EPISODE}: ${remapped} vocab id remap(s), ${inserting} new vocab row(s)`)
  } else {
    const prepared = prepareVocabImport({}, {})
    idRemap = prepared.idRemap
    vocabRows = prepared.rows
  }

  const statements = generateImportSql(vocabRows, idRemap)

  if (writeBatches) {
    mkdirSync(BATCH_DIR, { recursive: true })
    statements.forEach((sql, i) => writeFileSync(join(BATCH_DIR, `${String(i + 1).padStart(3, '0')}.sql`), sql + '\n', 'utf8'))
    console.log(`EP${EPISODE}: wrote ${statements.length} SQL files`)
  }

  if (run) {
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
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
