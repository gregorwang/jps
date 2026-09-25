import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'

const baseFiles = [
  'regen/batch35_exercises_letter.json',
  'regen/batch36_exercises_letter.json',
  'regen/batch37_exercises_letter.json',
]
const overlayFile = 'regen/generation_overlay_exercises_letter_batches35_37.json'
const expectedHash = 'eedf6b899655ddd940abeaa95b17ad29'
const exactKeys = ['answer', 'hint', 'id', 'prompt', 'review_note']
const issues = []
const warnings = []

function readArray(file) {
  const raw = fs.readFileSync(file, 'utf8')
  if (raw.charCodeAt(0) === 0xfeff || /[\u200b-\u200f\u2060\ufeff]/u.test(raw)) {
    issues.push(`${file}: contains BOM or zero-width characters`)
  }
  const value = JSON.parse(raw)
  if (!Array.isArray(value)) throw new Error(`${file}: top level is not an array`)
  return value
}

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

const baseRows = baseFiles.flatMap((file) => readArray(file).map((row) => ({ ...row, __file: file })))
const overlayRows = readArray(overlayFile)
const overlayById = new Map(overlayRows.map((row) => [row.id, row]))
const baseById = new Map(baseRows.map((row) => [row.id, row]))

if (baseRows.length !== 120 || baseById.size !== 120) {
  issues.push(`base cardinality expected 120/120, got ${baseRows.length}/${baseById.size}`)
}
if (overlayRows.length !== 2 || overlayById.size !== 2) {
  issues.push(`overlay cardinality expected 2/2, got ${overlayRows.length}/${overlayById.size}`)
}
for (const overlay of overlayRows) {
  const base = baseById.get(overlay.id)
  if (!base) {
    issues.push(`overlay ID absent from base: ${overlay.id}`)
    continue
  }
  for (const field of ['id', 'prompt', 'hint', 'review_note']) {
    if (overlay[field] !== base[field]) issues.push(`${overlay.id}: overlay unexpectedly changes ${field}`)
  }
  if (overlay.answer === base.answer) issues.push(`${overlay.id}: overlay answer is unchanged`)
}

let replacementCount = 0
const mergedRows = baseRows.map((base) => {
  const overlay = overlayById.get(base.id)
  if (!overlay) return base
  replacementCount += 1
  return { ...overlay, __file: overlayFile }
})
if (replacementCount !== overlayRows.length) {
  issues.push(`replacement count ${replacementCount}, overlay rows ${overlayRows.length}`)
}

for (const file of baseFiles) {
  const count = baseRows.filter((row) => row.__file === file).length
  if (count !== 40) issues.push(`${file}: expected 40 rows, found ${count}`)
}

const mergedIds = new Set(mergedRows.map((row) => row.id))
if (mergedRows.length !== 120 || mergedIds.size !== 120) {
  issues.push(`merged cardinality expected 120/120, got ${mergedRows.length}/${mergedIds.size}`)
}

for (const row of mergedRows) {
  const keys = Object.keys(row).filter((key) => key !== '__file').sort()
  if (JSON.stringify(keys) !== JSON.stringify(exactKeys)) {
    issues.push(`${row.id}: unexpected schema keys ${keys.join(',')}`)
  }
  for (const field of ['prompt', 'answer', 'hint']) {
    const value = row[field] ?? ''
    if (/(正确答案|选项[A-D]|答案是|用于\d|数据库|manifest|内部ID|原始选项|模板生成|source[_ ]?id|re-zero-)/iu.test(value)) {
      issues.push(`${row.id}.${field}: learner-facing metadata or template leakage`)
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
  for (const row of mergedRows) {
    const value = normalize(row[field])
    if (!value) continue
    if (seen.has(value)) issues.push(`${field} duplicate: ${seen.get(value)} and ${row.id}`)
    else seen.set(value, row.id)
  }
}

const orderedIds = mergedRows.map((row) => row.id)
const orderedHash = crypto.createHash('md5').update(orderedIds.join(',')).digest('hex')
if (orderedHash !== expectedHash) issues.push(`ordered comma-MD5 ${orderedHash}, expected ${expectedHash}`)

const episode21Count = mergedRows.filter((row) => row.id.includes('s01e21')).length
const episode22Count = mergedRows.filter((row) => row.id.includes('s01e22')).length
if (episode21Count !== 56 || episode22Count !== 64) {
  issues.push(`episode split expected 56/64, got ${episode21Count}/${episode22Count}`)
}

const excluded = new Set([...baseFiles, overlayFile].map(path.normalize))
const overlaps = []
for (const entry of fs.readdirSync('regen', { withFileTypes: true })) {
  if (!entry.isFile() || !entry.name.endsWith('.json')) continue
  const file = path.join('regen', entry.name)
  if (excluded.has(path.normalize(file))) continue
  let rows
  try {
    rows = readArray(file)
  } catch {
    continue
  }
  for (const row of rows) {
    if (row && mergedIds.has(row.id)) overlaps.push(`${row.id} in ${file}`)
  }
}
if (overlaps.length) issues.push(`target IDs overlap other candidate files: ${overlaps.join('; ')}`)

console.log(JSON.stringify({
  baseFiles: baseFiles.length,
  baseRows: baseRows.length,
  overlayRows: overlayRows.length,
  replacements: replacementCount,
  mergedRows: mergedRows.length,
  uniqueIds: mergedIds.size,
  episode21Count,
  episode22Count,
  orderedCommaMd5: orderedHash,
  expectedOrderedCommaMd5: expectedHash,
  otherCandidateIdOverlaps: overlaps.length,
  issues,
  warnings,
}, null, 2))

if (issues.length) process.exitCode = 1
