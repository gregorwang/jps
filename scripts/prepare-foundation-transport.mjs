import { createHash } from "node:crypto"
import { execFileSync } from "node:child_process"
import { readFileSync } from "node:fs"
import { relative, resolve } from "node:path"

function fail(message) {
  throw new Error(message)
}

function sha256(value) {
  return createHash("sha256").update(value).digest("hex")
}

function readJson(path) {
  return JSON.parse(readFileSync(path, "utf8"))
}

function workspaceRelative(path) {
  return relative(resolve("."), path).replaceAll("\\", "/")
}

function validate(mode, generationPath, overlayPath) {
  execFileSync(
    process.execPath,
    [
      resolve("scripts/validate-foundation-content.mjs"),
      mode,
      generationPath,
      overlayPath,
    ],
    { stdio: ["ignore", "pipe", "inherit"] },
  )
}

function decisionMap(overlay) {
  return new Map(overlay.decisions.map((decision) => [decision.id, decision]))
}

function mergeRows(baseRows, replacements, idOf) {
  const replacementById = new Map(replacements.map((row) => [idOf(row), row]))
  return baseRows.map((row) => replacementById.get(idOf(row)) ?? row)
}

function auditMetadata({ generation, overlay, generationPath, reviewReportPath }) {
  const generatorAgent = overlay.generator_agent
    ?? generation.generator_agent
    ?? generation.pack?.generator_agent
  const reviewerAgent = overlay.reviewer_agent
  if (!generatorAgent || !reviewerAgent || generatorAgent === reviewerAgent) {
    fail("generator/reviewer audit metadata is missing or not independent")
  }
  const reviewReportRaw = readFileSync(reviewReportPath)
  return {
    generatorAgent,
    reviewerAgent,
    sourceFile: workspaceRelative(generationPath),
    reviewReportFile: workspaceRelative(reviewReportPath),
    reviewSha256: sha256(reviewReportRaw),
  }
}

function prepareTopics(generationArg, overlayArg, reviewReportArg) {
  const generationPath = resolve(generationArg)
  const overlayPath = resolve(overlayArg)
  const reviewReportPath = resolve(reviewReportArg)
  validate("topics", generationPath, overlayPath)
  const generation = readJson(generationPath)
  const overlay = readJson(overlayPath)
  const decisions = decisionMap(overlay)
  const effectiveRows = mergeRows(
    generation.topics,
    overlay.replacements ?? [],
    (row) => row.topic_key,
  )
  const audit = auditMetadata({ generation, overlay, generationPath, reviewReportPath })

  const topics = effectiveRows.map((topic, index) => {
    const decision = decisions.get(topic.topic_key)
    if (!decision) fail(`missing reviewed decision for ${topic.topic_key}`)
    return {
      id: topic.topic_key,
      curriculum_version: "foundation-v1",
      domain: topic.domain,
      module_id: topic.module_key,
      sort_order: index,
      title_ja: topic.name_ja,
      title_zh: topic.name_zh,
      short_definition_zh: topic.short_definition_zh,
      beginner_explanation_zh: topic.beginner_explanation_zh,
      deep_explanation_zh: topic.deep_explanation_zh,
      caution_note_zh: topic.caution_zh,
      prerequisite_topic_ids: topic.prerequisite_topic_keys,
      learning_objectives_json: topic.objectives,
      example_spec_json: topic.original_example_spec,
      tags_json: topic.tags,
      content_version: 1,
      quality_score: decision.quality_score,
      reviewer_note: decision.note,
      generator_agent: audit.generatorAgent,
      reviewer_agent: audit.reviewerAgent,
      source_file: audit.sourceFile,
      review_report_file: audit.reviewReportFile,
      review_sha256: audit.reviewSha256,
    }
  })

  return {
    kind: "topics",
    expected: 60,
    generationSha256: sha256(readFileSync(generationPath)),
    overlaySha256: sha256(readFileSync(overlayPath)),
    ...audit,
    candidateDocument: topics,
  }
}

function preparePack(
  generationArg,
  overlayArg,
  reviewReportArg,
  topicsEffectiveArg,
) {
  const generationPath = resolve(generationArg)
  const overlayPath = resolve(overlayArg)
  const reviewReportPath = resolve(reviewReportArg)
  const topicsEffectivePath = resolve(topicsEffectiveArg)
  validate("pack", generationPath, overlayPath)
  const generation = readJson(generationPath)
  const overlay = readJson(overlayPath)
  const topicsEffective = readJson(topicsEffectivePath)
  const decisions = decisionMap(overlay)
  const effectiveRows = mergeRows(
    generation.questions,
    overlay.replacements ?? [],
    (row) => row.id,
  )
  const audit = auditMetadata({ generation, overlay, generationPath, reviewReportPath })
  const topicById = new Map(topicsEffective.topics.map((topic) => [topic.topic_key, topic]))
  const domainQuotas = {}
  for (const topicId of generation.topic_ids) {
    const topic = topicById.get(topicId)
    if (!topic) fail(`pack references topic missing from reviewed catalog: ${topicId}`)
    domainQuotas[topic.domain] = (domainQuotas[topic.domain] ?? 0) + 1
  }
  const minimumQuality = Math.min(
    ...overlay.decisions.map((decision) => decision.quality_score),
  )

  const pack = {
    id: generation.pack.id,
    curriculum_version: generation.pack.curriculum_version,
    batch_no: generation.pack.batch_no,
    title_zh: generation.pack.title_zh,
    description_zh: generation.pack.description_zh,
    topic_count: generation.pack.topic_count,
    question_count: generation.pack.question_count,
    domain_quotas_json: domainQuotas,
    content_version: 1,
    quality_score: minimumQuality,
    reviewer_note: `独立逐题复核 ${overlay.reviewed_count}/${generation.pack.question_count}；`
      + `修订 ${(overlay.replacements ?? []).length} 条。`,
    generator_agent: audit.generatorAgent,
    reviewer_agent: audit.reviewerAgent,
    source_file: audit.sourceFile,
    review_report_file: audit.reviewReportFile,
    review_sha256: audit.reviewSha256,
  }

  const questions = effectiveRows.map((question) => {
    const decision = decisions.get(question.id)
    if (!decision) fail(`missing reviewed decision for ${question.id}`)
    return {
      id: question.id,
      pack_id: generation.pack.id,
      topic_id: question.topic_id,
      curriculum_version: generation.pack.curriculum_version,
      stage: question.stage,
      question_type: question.question_type,
      source_kind: question.source_kind,
      stimulus_json: question.stimulus,
      prompt_zh: question.prompt_zh,
      options_json: question.options,
      answer_json: question.answer,
      hint_zh: question.hint_zh,
      explanation_zh: question.explanation_zh,
      deep_explanation_zh: question.deep_explanation_zh,
      caution_note_zh: question.caution_note_zh,
      wrong_explanations_json: question.wrong_explanations,
      transfer_example_ja: question.transfer_example_ja ?? null,
      transfer_explanation_zh: question.transfer_explanation_zh ?? null,
      difficulty: question.difficulty,
      tags_json: question.tags,
      sort_order: question.sort_order,
      content_version: 1,
      quality_score: decision.quality_score,
      reviewer_note: decision.note,
      generator_agent: audit.generatorAgent,
      reviewer_agent: audit.reviewerAgent,
      source_file: audit.sourceFile,
      review_report_file: audit.reviewReportFile,
      review_sha256: audit.reviewSha256,
    }
  })

  return {
    kind: "pack",
    expected: 80,
    generationSha256: sha256(readFileSync(generationPath)),
    overlaySha256: sha256(readFileSync(overlayPath)),
    ...audit,
    candidateDocument: { pack, topic_ids: generation.topic_ids, questions },
  }
}

const [kind, ...args] = process.argv.slice(2)
let prepared
if (kind === "topics" && args.length === 3) {
  prepared = prepareTopics(...args)
} else if (kind === "pack" && args.length === 4) {
  prepared = preparePack(...args)
} else {
  fail(
    "usage:\n"
      + "  node scripts/prepare-foundation-transport.mjs topics "
      + "<generation.json> <review-overlay.json> <cross-review.md>\n"
      + "  node scripts/prepare-foundation-transport.mjs pack "
      + "<generation.json> <review-overlay.json> <cross-review.md> <topics-effective.json>",
  )
}

const candidateJson = JSON.stringify(prepared.candidateDocument)
const result = {
  kind: prepared.kind,
  expected: prepared.expected,
  generationSha256: prepared.generationSha256,
  overlaySha256: prepared.overlaySha256,
  generatorAgent: prepared.generatorAgent,
  reviewerAgent: prepared.reviewerAgent,
  sourceFile: prepared.sourceFile,
  reviewReportFile: prepared.reviewReportFile,
  reviewSha256: prepared.reviewSha256,
  candidateSha256: sha256(candidateJson),
  base64: Buffer.from(candidateJson, "utf8").toString("base64"),
}

process.stdout.write(`${JSON.stringify(result)}\n`)
