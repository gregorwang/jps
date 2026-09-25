import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'

const targetFiles = [
  'archive-content-sources/rezero-exercise-regen-2026-07-27/batch35_exercises_letter.json',
  'archive-content-sources/rezero-exercise-regen-2026-07-27/batch36_exercises_letter.json',
  'archive-content-sources/rezero-exercise-regen-2026-07-27/batch37_exercises_letter.json',
]
const expectedHash = 'eedf6b899655ddd940abeaa95b17ad29'
const exactKeys = ['answer', 'hint', 'id', 'prompt', 'review_note']
const issues = []
const warnings = []

function normalize(value) {
  return String(value ?? '')
    .normalize('NFKC')
    .toLowerCase()
    .replace(/[\s\p{P}\p{S}]+/gu, '')
}

function longestCommonSubstring(left, right) {
  const a = [...left]
  const b = [...right]
  let best = 0
  let previous = new Uint16Array(b.length + 1)
  for (let i = 1; i <= a.length; i += 1) {
    const current = new Uint16Array(b.length + 1)
    for (let j = 1; j <= b.length; j += 1) {
      if (a[i - 1] === b[j - 1]) {
        current[j] = previous[j - 1] + 1
        if (current[j] > best) best = current[j]
      }
    }
    previous = current
  }
  return best
}

function readArray(file) {
  const raw = fs.readFileSync(file, 'utf8')
  if (raw.charCodeAt(0) === 0xfeff || /[\u200b-\u200f\u2060\ufeff]/u.test(raw)) {
    issues.push(`${file}: contains BOM or zero-width characters`)
  }
  const value = JSON.parse(raw)
  if (!Array.isArray(value)) throw new Error(`${file}: top level is not an array`)
  return value
}

const targetRows = targetFiles.flatMap((file) =>
  readArray(file).map((row) => ({ ...row, __file: file })),
)
const targetIds = new Set(targetRows.map((row) => row.id))

for (const file of targetFiles) {
  const count = targetRows.filter((row) => row.__file === file).length
  if (count !== 40) issues.push(`${file}: expected 40 rows, found ${count}`)
}
if (targetRows.length !== 120) issues.push(`expected 120 rows, found ${targetRows.length}`)
if (targetIds.size !== targetRows.length) issues.push('duplicate target IDs')

for (const row of targetRows) {
  const keys = Object.keys(row).filter((key) => key !== '__file').sort()
  if (JSON.stringify(keys) !== JSON.stringify(exactKeys)) {
    issues.push(`${row.id}: unexpected schema keys ${keys.join(',')}`)
  }
  for (const field of ['prompt', 'answer', 'hint']) {
    const value = row[field] ?? ''
    if (/数据库|manifest|内部ID|原始选项|模板生成|source[_ ]?id|re-zero-/iu.test(value)) {
      issues.push(`${row.id}.${field}: learner-facing metadata leakage`)
    }
  }
  const answer = normalize(row.answer)
  const hint = normalize(row.hint)
  if (answer && hint.includes(answer)) issues.push(`${row.id}: hint contains full answer`)
  const common = longestCommonSubstring(answer, hint)
  if (common >= 18) warnings.push(`${row.id}: answer/hint common substring length ${common}`)
}

for (const field of ['prompt', 'answer', 'hint']) {
  const seen = new Map()
  for (const row of targetRows) {
    const value = normalize(row[field])
    if (!value) continue
    if (seen.has(value)) issues.push(`${field} duplicate: ${seen.get(value)} and ${row.id}`)
    else seen.set(value, row.id)
  }
}

const orderedIds = targetRows.map((row) => row.id)
const orderedHash = crypto.createHash('md5').update(orderedIds.join(',')).digest('hex')
if (orderedHash !== expectedHash) issues.push(`ordered comma-MD5 ${orderedHash}, expected ${expectedHash}`)

const otherOccurrences = []
for (const entry of fs.readdirSync('regen', { withFileTypes: true })) {
  if (!entry.isFile() || !entry.name.endsWith('.json')) continue
  const file = path.join('regen', entry.name)
  if (targetFiles.map(path.normalize).includes(path.normalize(file))) continue
  let rows
  try {
    rows = readArray(file)
  } catch {
    continue
  }
  for (const row of rows) {
    if (row && targetIds.has(row.id)) otherOccurrences.push(`${row.id} in ${file}`)
  }
}
if (otherOccurrences.length) issues.push(`target IDs overlap other candidate files: ${otherOccurrences.join('; ')}`)

console.log(JSON.stringify({
  files: targetFiles.length,
  rows: targetRows.length,
  uniqueIds: targetIds.size,
  orderedCommaMd5: orderedHash,
  expectedOrderedCommaMd5: expectedHash,
  otherCandidateIdOverlaps: otherOccurrences.length,
  issues,
  warnings,
}, null, 2))

if (issues.length) process.exitCode = 1
