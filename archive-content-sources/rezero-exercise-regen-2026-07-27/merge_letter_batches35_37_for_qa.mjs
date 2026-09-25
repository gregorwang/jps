import fs from 'node:fs'

const baseFiles = [
  'archive-content-sources/rezero-exercise-regen-2026-07-27/batch35_exercises_letter.json',
  'archive-content-sources/rezero-exercise-regen-2026-07-27/batch36_exercises_letter.json',
  'archive-content-sources/rezero-exercise-regen-2026-07-27/batch37_exercises_letter.json',
]
const overlayFile = 'archive-content-sources/rezero-exercise-regen-2026-07-27/generation_overlay_exercises_letter_batches35_37.json'
const outputFile = process.argv[2] ?? 'C:/tmp/letter_batches35_37_merged_for_qa.json'

const baseRows = baseFiles.flatMap((file) => JSON.parse(fs.readFileSync(file, 'utf8')))
const overlayRows = JSON.parse(fs.readFileSync(overlayFile, 'utf8'))
const overlayById = new Map(overlayRows.map((row) => [row.id, row]))
const baseIds = new Set(baseRows.map((row) => row.id))

if (baseRows.length !== 120 || baseIds.size !== 120) {
  throw new Error(`expected 120 unique base rows, got ${baseRows.length}/${baseIds.size}`)
}
if (overlayRows.length !== 2 || overlayById.size !== 2) {
  throw new Error(`expected 2 unique overlay rows, got ${overlayRows.length}/${overlayById.size}`)
}
for (const id of overlayById.keys()) {
  if (!baseIds.has(id)) throw new Error(`overlay ID is not in base selection: ${id}`)
}

let replacements = 0
const mergedRows = baseRows.map((row) => {
  const replacement = overlayById.get(row.id)
  if (!replacement) return row
  replacements += 1
  return replacement
})
if (replacements !== overlayRows.length) {
  throw new Error(`expected ${overlayRows.length} replacements, got ${replacements}`)
}

fs.writeFileSync(outputFile, `${JSON.stringify(mergedRows, null, 2)}\n`, 'utf8')
console.log(JSON.stringify({
  baseRows: baseRows.length,
  overlayRows: overlayRows.length,
  replacements,
  mergedRows: mergedRows.length,
  outputFile,
}, null, 2))
