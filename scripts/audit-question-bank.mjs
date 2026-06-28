const SUPABASE_URL = process.env.SUPABASE_URL ?? 'https://qoatvdvbuleamyzsaldp.supabase.co'
const SUPABASE_KEY = process.env.SUPABASE_PUBLISHABLE_KEY ?? process.env.SUPABASE_ANON_KEY
  ?? 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFvYXR2ZHZidWxlYW15enNhbGRwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE2OTA2NzEsImV4cCI6MjA5NzI2NjY3MX0.pj_MGoAST7khzKxolz29jeAbMFLRCUvaNE4t8ed3I-8'

const tables = [
  {
    name: 'learning_sentences',
    select: 'id,work_slug,episode,sort_order,source_line_no,ja_text,meaning_zh,romaji,tone_tags,difficulty',
    order: 'work_slug.asc,episode.asc,sort_order.asc',
    audit: auditSentence,
  },
  {
    name: 'learning_grammar_points',
    select: 'id,work_slug,episode,sort_order,source_line_no,pattern,function_zh,ja_example,explanation_zh,pragmatics_note,real_world_note,difficulty',
    order: 'work_slug.asc,episode.asc,sort_order.asc',
    audit: auditGrammar,
  },
  {
    name: 'learning_vocab_items',
    select: 'id,work_slug,surface,reading,romaji,meaning_zh,pos,jlpt_level,anime_tone_note,real_world_note,total_occurrences',
    order: 'work_slug.asc,total_occurrences.desc',
    audit: auditVocab,
  },
  {
    name: 'learning_exercises',
    select: 'id,work_slug,episode,sort_order,exercise_type,prompt,answer,hint,difficulty',
    order: 'work_slug.asc,episode.asc,sort_order.asc',
    audit: auditExercise,
  },
]

const allFindings = []

for (const table of tables) {
  const rows = await fetchAll(table)
  const findings = rows.flatMap((row) => table.audit(row).map((finding) => ({
    table: table.name,
    id: row.id,
    workSlug: row.work_slug,
    episode: row.episode,
    sourceLineNo: row.source_line_no,
    ...finding,
    sample: compactSample(row),
  })))
  allFindings.push(...findings)
  console.log(`${table.name}: ${rows.length} rows, ${findings.length} findings`)
}

const grouped = groupBy(allFindings, (finding) => `${finding.severity}:${finding.code}`)
console.log('\nFinding summary:')
for (const [key, items] of [...grouped.entries()].sort((left, right) => right[1].length - left[1].length)) {
  console.log(`- ${key}: ${items.length}`)
}

const severe = allFindings
  .filter((finding) => finding.severity !== 'info')
  .slice(0, 200)

console.log('\nTop findings:')
for (const finding of severe) {
  const location = [
    finding.table,
    finding.workSlug,
    finding.episode ? `EP${finding.episode}` : '',
    finding.sourceLineNo ? `line ${finding.sourceLineNo}` : '',
    finding.id,
  ].filter(Boolean).join(' / ')
  console.log(`\n[${finding.severity}] ${finding.code}: ${location}`)
  console.log(`  ${finding.message}`)
  console.log(`  ${finding.sample}`)
}

async function fetchAll(table) {
  const output = []
  const pageSize = 1000
  for (let offset = 0; ; offset += pageSize) {
    const params = new URLSearchParams({
      select: table.select,
      order: table.order,
      limit: String(pageSize),
      offset: String(offset),
    })
    const response = await fetch(`${SUPABASE_URL}/rest/v1/${table.name}?${params.toString()}`, {
      headers: {
        apikey: SUPABASE_KEY,
        Authorization: `Bearer ${SUPABASE_KEY}`,
      },
    })
    if (!response.ok) throw new Error(`${table.name} fetch failed: ${response.status} ${await response.text()}`)
    const rows = await response.json()
    output.push(...rows)
    if (rows.length < pageSize) return output
  }
}

function auditSentence(row) {
  const findings = []
  const ja = stringValue(row.ja_text)
  const zh = stringValue(row.meaning_zh)
  if (!ja) findings.push(error('missing_ja_text', '日文句子为空，不能生成听音/跟读题。'))
  if (!zh) findings.push(warn('missing_meaning_zh', '中文释义为空，不能生成中文拼句题和学习卡释义。'))
  if (hasPrivateUseOrReplacement(zh)) findings.push(error('corrupt_meaning_zh', '中文释义含私用区或替换字符，通常是字幕编码/人名乱码。'))
  if (hasKana(zh)) findings.push(error('mixed_language_meaning_zh', '中文释义混入日文假名，会污染中文拼句题。'))
  if (zh && !hasHan(zh)) findings.push(warn('non_chinese_meaning_zh', '中文释义里没有汉字，需确认是否是自然中文。'))
  if (ja && !hasKana(ja) && !/[一-龯々〆〤]/u.test(ja)) findings.push(warn('non_japanese_ja_text', '日文字段不像日文，需确认字段是否错位。'))
  if (ja.length > 80) findings.push(warn('long_ja_text', '日文句子过长，不适合作为拼块题。'))
  if (zh.length > 60) findings.push(warn('long_meaning_zh', '中文释义过长，不适合作为拼块题。'))
  return findings
}

function auditGrammar(row) {
  const findings = []
  const pattern = stringValue(row.pattern)
  const fn = stringValue(row.function_zh)
  const example = stringValue(row.ja_example)
  if (!pattern) findings.push(error('missing_pattern', '语法 pattern 为空。'))
  if (!fn) findings.push(error('missing_function_zh', '语法中文功能为空，选择题无法成立。'))
  if (!example) findings.push(warn('missing_ja_example', '语法例句为空，填空题无法成立。'))
  for (const [field, value] of Object.entries(row)) {
    if (typeof value === 'string' && hasTemplateVersionNote(value)) {
      findings.push(error('template_version_note', `字段 ${field} 混入生成模板版本说明，应从学习卡内容中删除。`))
    }
  }
  if (hasPrivateUseOrReplacement(fn)) findings.push(error('corrupt_function_zh', '语法中文功能含乱码。'))
  if (hasKana(fn)) findings.push(warn('mixed_language_function_zh', '语法中文功能混入日文假名，需确认是否应拆到例句字段。'))
  if (example && pattern && !grammarPatternCanMatch(pattern, example)) {
    findings.push(warn('pattern_not_in_example', '语法 pattern 很可能无法从例句中挖空，填空题会被跳过或答案不自然。'))
  }
  return findings
}

function auditVocab(row) {
  const findings = []
  const surface = stringValue(row.surface)
  const meaning = stringValue(row.meaning_zh)
  if (!surface) findings.push(error('missing_surface', '词条 surface 为空。'))
  if (!meaning) findings.push(error('missing_meaning_zh', '词条中文释义为空，词义选择/配对题无法成立。'))
  if (hasPrivateUseOrReplacement(meaning)) findings.push(error('corrupt_meaning_zh', '词条中文释义含乱码。'))
  if (hasKana(meaning)) findings.push(warn('mixed_language_meaning_zh', '词条中文释义混入日文假名，需确认是否合理。'))
  return findings
}

function auditExercise(row) {
  const findings = []
  const prompt = stringValue(row.prompt)
  const answer = stringValue(row.answer)
  const hint = stringValue(row.hint)
  if (!prompt) findings.push(error('missing_prompt', '练习题 prompt 为空。'))
  if (!answer) findings.push(error('missing_answer', '练习题 answer 为空。'))
  if (hasPrivateUseOrReplacement(`${prompt}${answer}${hint}`)) findings.push(error('corrupt_exercise_text', '练习题含乱码。'))
  if (/中文[:：]/u.test(prompt) && !hasJapanese(answer)) findings.push(warn('meaning_to_japanese_answer_not_japanese', '中文到日文题的答案不像日文。'))
  if (/假名[:：]/u.test(prompt) && !/[一-龯々〆〤]/u.test(answer)) findings.push(warn('kana_to_kanji_answer_not_kanji', '假名转汉字题的答案不含汉字。'))
  return findings
}

function grammarPatternCanMatch(pattern, example) {
  const normalized = pattern.replace(/^～/u, '').replace(/[「」]/gu, '').trim()
  if (!normalized) return false
  if (example.includes(normalized)) return true
  if (/句末/u.test(pattern)) return /(?:よね|かな|だろ|よ|ね)$/u.test(example)
  return normalized.split(/[/／]/u).some((part) => part.trim() && example.includes(part.trim()))
}

function compactSample(row) {
  const fields = ['ja_text', 'meaning_zh', 'surface', 'pattern', 'function_zh', 'ja_example', 'prompt', 'answer', 'hint']
  return fields
    .filter((field) => row[field] !== undefined && row[field] !== null && String(row[field]).trim())
    .map((field) => `${field}=${JSON.stringify(String(row[field]).slice(0, 120))}`)
    .join(' | ')
}

function error(code, message) {
  return { severity: 'error', code, message }
}

function warn(code, message) {
  return { severity: 'warn', code, message }
}

function stringValue(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function hasPrivateUseOrReplacement(value) {
  return /[\uE000-\uF8FF\uFFFD]/u.test(value)
}

function hasTemplateVersionNote(value) {
  return /V\d+\s*[：:][^。]*(?:读完整集后|单句精读|基础语法理解题)|适合单句精读和基础语法理解题/u.test(value)
}

function hasKana(value) {
  return /[ぁ-んァ-ヶ]/u.test(value)
}

function hasHan(value) {
  return /[\p{Script=Han}]/u.test(value)
}

function hasJapanese(value) {
  return /[ぁ-んァ-ヶー一-龯々〆〤]/u.test(value)
}

function groupBy(items, keyFn) {
  const groups = new Map()
  for (const item of items) {
    const key = keyFn(item)
    groups.set(key, [...(groups.get(key) ?? []), item])
  }
  return groups
}
