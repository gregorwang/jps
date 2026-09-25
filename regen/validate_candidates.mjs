import fs from 'node:fs'
import path from 'node:path'

const files = process.argv.slice(2)

if (!files.length) {
  console.error('Usage: node regen/validate_candidates.mjs <candidate.json> [...]')
  process.exit(2)
}

const errors = []
const warnings = []
const seenIds = new Map()
const pedagogicalStrings = new Map()

function addError(file, id, message) {
  errors.push(`${file}${id ? ` [${id}]` : ''}: ${message}`)
}

function walkPedagogicalStrings(value) {
  if (typeof value === 'string') {
    const normalized = value.trim()
    if (normalized.length >= 12) {
      pedagogicalStrings.set(normalized, (pedagogicalStrings.get(normalized) ?? 0) + 1)
    }
    return
  }
  if (Array.isArray(value)) {
    value.forEach(walkPedagogicalStrings)
    return
  }
  if (value && typeof value === 'object') {
    for (const [key, child] of Object.entries(value)) {
      if (!['domain', 'level'].includes(key)) walkPedagogicalStrings(child)
    }
  }
}

function validateExercise(file, row) {
  for (const key of ['id', 'prompt', 'answer', 'hint', 'review_note']) {
    if (typeof row[key] !== 'string' || !row[key].trim()) {
      addError(file, row.id, `missing non-empty ${key}`)
    }
  }

  if (typeof row.answer === 'string' && (/^[A-D]$/i.test(row.answer.trim()) || row.answer.startsWith('用于'))) {
    addError(file, row.id, `invalid placeholder answer: ${row.answer}`)
  }

  const learningText = [row.prompt, row.answer, row.hint].filter(Boolean).join(' ')
  if (/(正确答案|选项[A-D]|答案是|用于\d|source[_ ]?id|re-zero-|模板生成)/i.test(learningText)) {
    addError(file, row.id, 'learner-facing content contains metadata or template language')
  }
}

function validateLinguistic(file, row) {
  for (const key of ['id', 'source_type', 'source_id', 'review_note']) {
    if (typeof row[key] !== 'string' || !row[key].trim()) {
      addError(file, row.id, `missing non-empty ${key}`)
    }
  }

  if (!['vocab', 'grammar', 'sentence'].includes(row.source_type)) {
    addError(file, row.id, `invalid source_type: ${row.source_type}`)
  }

  const payload = row.linguistic_payload
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    addError(file, row.id, 'linguistic_payload must be an object')
    return
  }

  const allowedTopLevel = new Set([
    'level',
    'headlineZh',
    'domains',
    'terms',
    'historicalNoteZh',
    'cautionZh',
  ])
  for (const key of Object.keys(payload)) {
    if (!allowedTopLevel.has(key)) addError(file, row.id, `unexpected payload field: ${key}`)
  }

  if (typeof payload.headlineZh !== 'string' || !payload.headlineZh.trim()) {
    addError(file, row.id, 'missing headlineZh')
  }
  if (!Array.isArray(payload.domains) || payload.domains.length < 2 || payload.domains.length > 4) {
    addError(file, row.id, 'domains must contain 2–4 analyses')
  } else {
    payload.domains.forEach((domain, index) => {
      for (const key of ['domain', 'titleZh', 'takeawayZh', 'explanationZh']) {
        if (typeof domain?.[key] !== 'string' || !domain[key].trim()) {
          addError(file, row.id, `domains[${index}] missing non-empty ${key}`)
        }
      }
    })
  }
  if (!Array.isArray(payload.terms) || payload.terms.length < 1 || payload.terms.length > 5) {
    addError(file, row.id, 'terms must contain 1–5 items')
  } else {
    payload.terms.forEach((term, index) => {
      for (const key of ['termZh', 'plainZh']) {
        if (typeof term?.[key] !== 'string' || !term[key].trim()) {
          addError(file, row.id, `terms[${index}] missing non-empty ${key}`)
        }
      }
    })
  }

  const serialized = JSON.stringify(payload)
  if (/(re-zero-|source[_ ]?id|\b(?:grammar|sentence|vocab)-\d{3}\b)/i.test(serialized)) {
    addError(file, row.id, 'payload leaks an internal source identifier')
  }
  walkPedagogicalStrings(payload)
}

let totalRows = 0
for (const input of files) {
  const file = path.normalize(input)
  let rows
  try {
    rows = JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch (error) {
    addError(file, '', `invalid JSON: ${error.message}`)
    continue
  }
  if (!Array.isArray(rows)) {
    addError(file, '', 'top level must be an array')
    continue
  }

  totalRows += rows.length
  for (const row of rows) {
    if (!row || typeof row !== 'object' || Array.isArray(row)) {
      addError(file, '', 'every item must be an object')
      continue
    }
    if (typeof row.id === 'string') {
      const prior = seenIds.get(row.id)
      if (prior) addError(file, row.id, `duplicate ID also found in ${prior}`)
      else seenIds.set(row.id, file)
    }

    if ('linguistic_payload' in row) validateLinguistic(file, row)
    else validateExercise(file, row)
  }
}

for (const [text, count] of pedagogicalStrings) {
  if (count >= 3) warnings.push(`repeated pedagogical string ${count}×: ${JSON.stringify(text)}`)
}

console.log(JSON.stringify({
  files: files.length,
  rows: totalRows,
  uniqueIds: seenIds.size,
  errors,
  warnings,
}, null, 2))

if (errors.length) process.exit(1)
