import { createHash } from "node:crypto"
import { execFileSync } from "node:child_process"
import { readFileSync, writeFileSync } from "node:fs"
import { resolve } from "node:path"

function fail(message) {
  throw new Error(message)
}

function sha256(value) {
  return createHash("sha256").update(value).digest("hex")
}

function idOf(kind, row) {
  return kind === "topics" ? row?.topic_key : row?.id
}

const [kind, generationArg, overlayArg, outputArg] = process.argv.slice(2)
if (!["topics", "pack"].includes(kind) || !generationArg || !overlayArg || !outputArg) {
  fail(
    "usage: node scripts/merge-foundation-reviewed.mjs "
      + "<topics|pack> <generation.json> <review-overlay.json> <output.json>",
  )
}

const generationPath = resolve(generationArg)
const overlayPath = resolve(overlayArg)
const outputPath = resolve(outputArg)

execFileSync(
  process.execPath,
  [
    resolve("scripts/validate-foundation-content.mjs"),
    kind,
    generationPath,
    overlayPath,
  ],
  { stdio: ["ignore", "pipe", "inherit"] },
)

const generationRaw = readFileSync(generationPath, "utf8")
const overlayRaw = readFileSync(overlayPath, "utf8")
const generation = JSON.parse(generationRaw)
const overlay = JSON.parse(overlayRaw)
const rowProperty = kind === "topics" ? "topics" : "questions"
const baseRows = generation[rowProperty]
const replacements = overlay.replacements ?? []

if (!Array.isArray(baseRows) || !Array.isArray(replacements)) {
  fail("validated input unexpectedly lacks rows or replacements")
}

const baseIds = new Set(baseRows.map((row) => idOf(kind, row)))
const replacementById = new Map()
for (const replacement of replacements) {
  const id = idOf(kind, replacement)
  if (!baseIds.has(id)) fail(`replacement cannot append unknown ID: ${id}`)
  if (replacementById.has(id)) fail(`duplicate replacement ID: ${id}`)
  replacementById.set(id, replacement)
}

const effectiveRows = baseRows.map((row) => replacementById.get(idOf(kind, row)) ?? row)
const effective = {
  ...generation,
  review_audit: {
    generator_agent: overlay.generator_agent
      ?? generation.generator_agent
      ?? generation.pack?.generator_agent,
    reviewer_agent: overlay.reviewer_agent,
    quality_score: overlay.quality_score
      ?? Math.min(...overlay.decisions.map((decision) => decision.quality_score)),
    reviewed_count: overlay.reviewed_count,
    generation_sha256: overlay.generation_sha256,
    review_overlay_sha256: sha256(overlayRaw),
    replacements_applied: replacementById.size,
  },
  [rowProperty]: effectiveRows,
}

const serialized = `${JSON.stringify(effective, null, 2)}\n`
writeFileSync(outputPath, serialized, "utf8")

process.stdout.write(
  `${JSON.stringify({
    kind,
    rows: effectiveRows.length,
    replacementsApplied: replacementById.size,
    output: outputPath,
    outputSha256: sha256(serialized),
  }, null, 2)}\n`,
)
