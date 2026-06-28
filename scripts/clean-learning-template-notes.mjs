const projectRef = process.env.SUPABASE_REF ?? 'qoatvdvbuleamyzsaldp'
const supabaseUrl = process.env.SUPABASE_URL ?? 'https://qoatvdvbuleamyzsaldp.supabase.co'
const accessToken = process.env.SUPABASE_ACCESS_TOKEN
const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY ?? (accessToken ? await getServiceRoleKey(accessToken) : '')
const readKey = serviceRoleKey || process.env.SUPABASE_PUBLISHABLE_KEY || process.env.SUPABASE_ANON_KEY
const apply = process.argv.includes('--apply')

if (!readKey) throw new Error('Set SUPABASE_ACCESS_TOKEN, SUPABASE_SERVICE_ROLE_KEY, or SUPABASE_PUBLISHABLE_KEY.')
if (apply && !serviceRoleKey) throw new Error('Set SUPABASE_ACCESS_TOKEN or SUPABASE_SERVICE_ROLE_KEY before running with --apply.')

const tables = [
  {
    name: 'learning_grammar_points',
    key: 'id',
    fields: [
      'id',
      'work_slug',
      'episode',
      'sort_order',
      'source_line_no',
      'pattern',
      'function_zh',
      'ja_example',
      'explanation_zh',
      'pragmatics_note',
      'real_world_note',
      'difficulty',
    ],
    textFields: ['pattern', 'function_zh', 'ja_example', 'explanation_zh', 'pragmatics_note', 'real_world_note', 'difficulty'],
  },
  {
    name: 'learning_sentences',
    key: 'id',
    fields: ['id', 'work_slug', 'episode', 'sort_order', 'source_line_no', 'ja_text', 'meaning_zh', 'romaji', 'difficulty'],
    textFields: ['ja_text', 'meaning_zh', 'romaji', 'difficulty'],
  },
  {
    name: 'learning_vocab_items',
    key: 'id',
    fields: ['id', 'work_slug', 'surface', 'reading', 'romaji', 'meaning_zh', 'pos', 'jlpt_level', 'anime_tone_note', 'real_world_note'],
    textFields: ['surface', 'reading', 'romaji', 'meaning_zh', 'pos', 'jlpt_level', 'anime_tone_note', 'real_world_note'],
  },
  {
    name: 'learning_exercises',
    key: 'id',
    fields: ['id', 'work_slug', 'episode', 'sort_order', 'prompt', 'answer', 'hint', 'difficulty'],
    textFields: ['prompt', 'answer', 'hint', 'difficulty'],
  },
  {
    name: 'linguistic_exercise_drafts',
    key: 'id',
    fields: [
      'id',
      'work_slug',
      'episode',
      'source_line_no',
      'ja_text',
      'zh_text',
      'prompt',
      'hint',
      'basic_explanation_zh',
      'deep_explanation_zh',
      'anime_context_note_zh',
      'caution_note_zh',
      'difficulty',
    ],
    textFields: [
      'ja_text',
      'zh_text',
      'prompt',
      'hint',
      'basic_explanation_zh',
      'deep_explanation_zh',
      'anime_context_note_zh',
      'caution_note_zh',
      'difficulty',
    ],
  },
]

const findings = []

for (const table of tables) {
  const rows = await fetchAll(table)
  let updated = 0
  for (const row of rows) {
    const patch = {}
    const dirtyFields = []
    for (const field of table.textFields) {
      const value = row[field]
      if (typeof value !== 'string') continue
      const cleaned = cleanTemplateVersionNote(value)
      if (cleaned === value) continue
      patch[field] = cleaned
      dirtyFields.push({ field, before: compact(value), after: compact(cleaned) })
    }
    if (dirtyFields.length === 0) continue
    findings.push({ table: table.name, id: row[table.key], workSlug: row.work_slug, episode: row.episode, sourceLineNo: row.source_line_no, dirtyFields })
    if (apply) {
      await patchRow(table, row[table.key], patch)
      updated += 1
    }
  }
  console.log(`${table.name}: ${rows.length} rows scanned, ${updated} rows updated`)
}

console.log(`template-note findings: ${findings.length}`)
for (const finding of findings.slice(0, 80)) {
  const location = [
    finding.table,
    finding.workSlug,
    Number.isFinite(finding.episode) ? `EP${finding.episode}` : '',
    finding.sourceLineNo ? `line ${finding.sourceLineNo}` : '',
    finding.id,
  ].filter(Boolean).join(' / ')
  console.log(`\n${location}`)
  for (const dirty of finding.dirtyFields) {
    console.log(`  ${dirty.field}: ${JSON.stringify(dirty.before)} -> ${JSON.stringify(dirty.after)}`)
  }
}

if (!apply) console.log('\ndry run only; rerun with --apply to write cleaned values')

async function getServiceRoleKey(token) {
  const response = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/api-keys`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) throw new Error(`api-keys failed: ${response.status} ${await response.text()}`)
  const keys = await response.json()
  const service = keys.find((key) => key.name === 'service_role' || key.id === 'service_role')
  if (!service?.api_key) throw new Error('service_role key not found')
  return service.api_key
}

async function fetchAll(table) {
  const output = []
  const pageSize = 1000
  for (let offset = 0; ; offset += pageSize) {
    const params = new URLSearchParams({
      select: table.fields.join(','),
      limit: String(pageSize),
      offset: String(offset),
    })
    const rows = await request(readKey, 'GET', `/rest/v1/${table.name}?${params.toString()}`)
    output.push(...rows)
    if (rows.length < pageSize) return output
  }
}

async function patchRow(table, id, patch) {
  if (table.name === 'learning_grammar_points' && accessToken) {
    await patchGrammarPointViaSql(id, patch)
    return
  }
  await request(serviceRoleKey, 'PATCH', `/rest/v1/${table.name}?${table.key}=eq.${encodeURIComponent(id)}`, patch)
}

async function patchGrammarPointViaSql(id, patch) {
  const assignments = Object.entries(patch)
    .map(([field, value]) => `${quoteIdent(field)} = ${quoteSql(value)}`)
    .join(', ')
  if (!assignments) return
  await databaseQuery(`
alter table public.learning_grammar_points disable trigger trg_learning_grammar_points_updated_at;
update public.learning_grammar_points set ${assignments} where id = ${quoteSql(id)};
alter table public.learning_grammar_points enable trigger trg_learning_grammar_points_updated_at;
`)
}

async function databaseQuery(query) {
  const response = await fetch(`https://api.supabase.com/v1/projects/${projectRef}/database/query`, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ query }),
  })
  if (!response.ok) throw new Error(`database query failed: ${response.status} ${await response.text()}`)
  const text = await response.text()
  return text ? JSON.parse(text) : []
}

async function request(key, method, pathName, body) {
  const init = {
    method,
    headers: {
      apikey: key,
      Authorization: `Bearer ${key}`,
      ...(body ? { 'Content-Type': 'application/json', Prefer: 'return=minimal' } : {}),
    },
  }
  if (body) init.body = JSON.stringify(body)
  const response = await fetch(`${supabaseUrl}${pathName}`, init)
  if (!response.ok) throw new Error(`${method} ${pathName} failed: ${response.status} ${await response.text()}`)
  if (response.status === 204) return []
  const text = await response.text()
  return text ? JSON.parse(text) : []
}

function cleanTemplateVersionNote(value) {
  if (!hasTemplateVersionNote(value)) return value
  return value
    .replace(/[（(]?\s*V\d+\s*[：:][^）)\n]*(?:读完整集后|单句精读|基础语法理解题)[^）)\n]*[）)]?/gu, ' ')
    .replace(/适合单句精读和基础语法理解题[。.]?/gu, ' ')
    .replace(/\s+/gu, ' ')
    .trim()
}

function hasTemplateVersionNote(value) {
  return /V\d+\s*[：:][^。]*(?:读完整集后|单句精读|基础语法理解题)|适合单句精读和基础语法理解题/u.test(value)
}

function compact(value) {
  return String(value).replace(/\s+/gu, ' ').trim().slice(0, 180)
}

function quoteIdent(value) {
  if (!/^[a-z_][a-z0-9_]*$/u.test(value)) throw new Error(`Unsafe SQL identifier: ${value}`)
  return value
}

function quoteSql(value) {
  return `'${String(value).replace(/'/gu, "''")}'`
}
