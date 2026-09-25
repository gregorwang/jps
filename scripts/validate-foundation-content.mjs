#!/usr/bin/env node

import crypto from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import process from 'node:process'

const TOPIC_COUNT = 60
const PACK_TOPIC_COUNT = 20
const PACK_QUESTION_COUNT = 80
const OPTION_COUNT = 4
const MIN_QUALITY_SCORE = 95

const STAGES = Object.freeze(['F1', 'F2', 'F3', 'F4'])
const STAGE_SET = new Set(STAGES)
const DOMAIN_QUOTAS = Object.freeze({
  phonology_writing: 8,
  morphology: 10,
  syntax: 13,
  semantics: 8,
  pragmatics_discourse: 12,
  sociolinguistics: 5,
  historical_grammaticalization: 4,
})

const BANNED_COUPLING_KEYS = new Set([
  'work',
  'workid',
  'workname',
  'workslug',
  'episode',
  'episodeid',
  'episodeno',
  'sourceline',
  'sourcelineid',
  'sourcelineno',
  'sourceid',
  'anime',
  'animeid',
  'animename',
  'character',
  'characters',
  'characterid',
  'charactername',
])

const PROPER_NAME_PATTERNS = Object.freeze([
  /Re\s*:\s*Zero/giu,
  /リゼロ/gu,
  /从零开始的异世界生活/gu,
  /菜月昴|ナツキ・スバル|スバル|艾米莉亚|爱蜜莉雅|エミリア|雷姆|レム|拉姆|ラム|碧翠丝|ベアトリス/gu,
  /帕克|パック|菲鲁特|フェルト|莱因哈鲁特|ラインハルト|罗兹瓦尔|ロズワール|艾尔莎|エルザ/gu,
  /K-ON!?|けいおん[!！]?|放課後ティータイム/giu,
  /平沢唯|秋山澪|田井中律|琴吹紬|中野梓/gu,
])

const APPROVED_REVIEW_STATUSES = new Set([
  'approved',
  'accepted',
  'pass',
  'passed',
  'keep',
  'revised',
])

const QUESTION_TYPES = new Set([
  'single_choice',
  'morphology_analysis',
  'syntax_relation',
  'contrast_choice',
  'kuuki_yomi',
])

const SOURCE_KINDS = new Set([
  'original_sentence',
  'minimal_pair',
  'constructed_dialogue',
  'metalinguistic',
])

const STIMULUS_KINDS = new Set([
  'sentence',
  'dialogue',
  'contrast',
  'metalinguistic',
])

function usage() {
  return [
    'Usage:',
    '  node scripts/validate-foundation-content.mjs topics <generation.json> [review-overlay.json]',
    '  node scripts/validate-foundation-content.mjs pack <questions.json> [review-overlay.json]',
    '  node scripts/validate-foundation-content.mjs --self-test',
    '',
    'Review overlay object:',
    '  {',
    '    "generator_agent": "...",',
    '    "reviewer_agent": "...",',
    '    "quality_score": 95,',
    '    "reviewed_ids": ["every base ID exactly once"],',
    '    "replacements": [/* complete rows; IDs must already exist */]',
    '  }',
    '',
    'The validator never writes a merged file and never creates teaching content.',
  ].join('\n')
}

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}

function isNonEmptyString(value) {
  return typeof value === 'string' && value.trim().length > 0
}

function normalizeKey(key) {
  return String(key).replace(/[^a-zA-Z0-9]/g, '').toLowerCase()
}

function canonicalize(value) {
  if (Array.isArray(value)) return value.map(canonicalize)
  if (!isPlainObject(value)) return value

  const result = {}
  for (const key of Object.keys(value).sort()) {
    result[key] = canonicalize(value[key])
  }
  return result
}

function canonicalStringify(value) {
  return JSON.stringify(canonicalize(value))
}

function canonicalSha256(value) {
  return crypto.createHash('sha256').update(canonicalStringify(value), 'utf8').digest('hex')
}

function readJson(file, errors) {
  const normalized = path.resolve(file)
  try {
    const raw = fs.readFileSync(normalized, 'utf8')
    return {
      file: normalized,
      value: JSON.parse(raw),
      rawSha256: crypto.createHash('sha256').update(raw, 'utf8').digest('hex'),
    }
  } catch (error) {
    errors.push(`${normalized}: invalid JSON or unreadable file: ${error.message}`)
    return { file: normalized, value: null, rawSha256: null }
  }
}

function identifierOf(row, kind) {
  if (!isPlainObject(row)) return null
  const candidates = kind === 'topics'
    ? [row.topic_key, row.topic_id, row.id]
    : [row.question_id, row.question_key, row.id]
  return candidates.find(isNonEmptyString)?.trim() ?? null
}

function topicKeyOf(question) {
  if (!isPlainObject(question)) return null
  return [question.topic_key, question.topic_id]
    .find(isNonEmptyString)
    ?.trim() ?? null
}

function extractGenerationRows(document, kind, errors, file) {
  if (Array.isArray(document)) return document
  if (!isPlainObject(document)) {
    errors.push(`${file}: top level must be an object or array`)
    return []
  }

  const key = kind === 'topics' ? 'topics' : 'questions'
  if (!Array.isArray(document[key])) {
    errors.push(`${file}: top-level ${key} must be an array`)
    return []
  }
  return document[key]
}

function extractOverlayRows(overlay, kind, errors, file) {
  if (overlay === null || overlay === undefined) return []
  if (Array.isArray(overlay)) return overlay
  if (!isPlainObject(overlay)) {
    errors.push(`${file}: review overlay must be an object or array`)
    return []
  }

  const candidateKeys = kind === 'topics'
    ? ['replacements', 'topics', 'rows']
    : ['replacements', 'questions', 'rows']
  const present = candidateKeys.filter((key) => overlay[key] !== undefined)

  if (present.length > 1) {
    errors.push(`${file}: review overlay may use only one replacement collection (${present.join(', ')})`)
    return []
  }
  if (present.length === 0) return []

  const collection = overlay[present[0]]
  if (Array.isArray(collection)) return collection
  if (isPlainObject(collection)) {
    return Object.entries(collection).map(([id, row]) => {
      if (!isPlainObject(row)) return row
      const rowId = identifierOf(row, kind)
      if (rowId && rowId !== id) {
        errors.push(`${file}: replacement map key ${id} disagrees with row ID ${rowId}`)
      }
      if (rowId) return row
      const idKey = kind === 'topics' ? 'topic_key' : 'id'
      return { ...row, [idKey]: id }
    })
  }

  errors.push(`${file}: ${present[0]} must be an array or ID-keyed object`)
  return []
}

function applyReviewOverlay(baseRows, overlayRows, kind, errors, overlayFile) {
  const baseIds = new Map()
  const effectiveRows = [...baseRows]

  baseRows.forEach((row, index) => {
    const id = identifierOf(row, kind)
    if (!id) return
    if (!baseIds.has(id)) baseIds.set(id, index)
  })

  const seenOverlay = new Set()
  let replacementsApplied = 0

  for (const replacement of overlayRows) {
    if (!isPlainObject(replacement)) {
      errors.push(`${overlayFile}: every replacement must be an object`)
      continue
    }

    const id = identifierOf(replacement, kind)
    if (!id) {
      errors.push(`${overlayFile}: replacement is missing its ID`)
      continue
    }
    if (seenOverlay.has(id)) {
      errors.push(`${overlayFile}: duplicate replacement ID ${id}`)
      continue
    }
    seenOverlay.add(id)

    const baseIndex = baseIds.get(id)
    if (baseIndex === undefined) {
      errors.push(`${overlayFile}: replacement ID ${id} is not in the generation file; overlays cannot append`)
      continue
    }

    effectiveRows[baseIndex] = replacement
    replacementsApplied += 1
  }

  if (effectiveRows.length !== baseRows.length) {
    errors.push(`${overlayFile}: overlay changed row count from ${baseRows.length} to ${effectiveRows.length}`)
  }

  return { effectiveRows, replacementsApplied }
}

function collectReviewCoverage(overlay, overlayRows, kind, errors, overlayFile) {
  if (overlay === null || overlay === undefined) return []
  if (Array.isArray(overlay)) {
    return overlayRows.map((row) => identifierOf(row, kind)).filter(Boolean)
  }
  if (!isPlainObject(overlay)) return []

  if (Array.isArray(overlay.reviewed_ids)) return overlay.reviewed_ids
  if (Array.isArray(overlay.reviewedIds)) return overlay.reviewedIds
  if (Array.isArray(overlay.decisions)) {
    return overlay.decisions.map((decision, index) => {
      if (!isPlainObject(decision)) {
        errors.push(`${overlayFile}: decisions[${index}] must be an object`)
        return null
      }
      return [decision.id, decision.topic_key, decision.question_id, decision.question_key]
        .find(isNonEmptyString)
        ?.trim() ?? null
    }).filter(Boolean)
  }
  if (Array.isArray(overlay.reviews)) {
    const ids = []
    for (const review of overlay.reviews) {
      if (!isPlainObject(review)) {
        errors.push(`${overlayFile}: every reviews item must be an object`)
        continue
      }
      const id = [review.id, review.topic_key, review.question_id, review.question_key]
        .find(isNonEmptyString)
        ?.trim()
      if (!id) {
        errors.push(`${overlayFile}: review item is missing an ID`)
        continue
      }
      const status = [review.status, review.review_status, review.verdict]
        .find(isNonEmptyString)
        ?.trim()
        .toLowerCase()
      if (!status || !APPROVED_REVIEW_STATUSES.has(status)) {
        errors.push(`${overlayFile}: review ${id} has non-approved or missing status`)
      }
      ids.push(id)
    }
    return ids
  }

  if (overlayRows.length > 0) {
    return overlayRows.map((row) => identifierOf(row, kind)).filter(Boolean)
  }
  return []
}

function assertExactReviewCoverage(baseRows, reviewedIds, kind, errors, overlayFile) {
  const baseIds = baseRows.map((row) => identifierOf(row, kind)).filter(Boolean)
  const baseSet = new Set(baseIds)
  const seen = new Set()
  const duplicates = new Set()
  const unknown = new Set()

  for (const rawId of reviewedIds) {
    if (!isNonEmptyString(rawId)) {
      errors.push(`${overlayFile}: reviewed ID must be a non-empty string`)
      continue
    }
    const id = rawId.trim()
    if (seen.has(id)) duplicates.add(id)
    seen.add(id)
    if (!baseSet.has(id)) unknown.add(id)
  }

  if (duplicates.size) {
    errors.push(`${overlayFile}: duplicate reviewed IDs: ${[...duplicates].sort().join(', ')}`)
  }
  if (unknown.size) {
    errors.push(`${overlayFile}: reviewed IDs not found in generation: ${[...unknown].sort().join(', ')}`)
  }

  const missing = [...baseSet].filter((id) => !seen.has(id))
  if (missing.length) {
    errors.push(
      `${overlayFile}: independent review coverage is ${baseSet.size - missing.length}/${baseSet.size}; `
      + `missing ${missing.slice(0, 10).join(', ')}${missing.length > 10 ? ', …' : ''}`,
    )
  }
  if (seen.size !== baseSet.size || unknown.size || duplicates.size) {
    errors.push(
      `${overlayFile}: reviewed ID count must equal base unique ID count `
      + `(${seen.size} reviewed vs ${baseSet.size} base)`,
    )
  }
}

function validateReviewOverlayProtocol({
  overlay,
  overlayRows,
  baseRows,
  kind,
  generationRawSha256,
  errors,
  overlayFile,
}) {
  const decisionMap = new Map()
  if (overlay === null || overlay === undefined) return decisionMap
  if (!isPlainObject(overlay)) {
    errors.push(`${overlayFile}: review overlay must use the documented object format`)
    return decisionMap
  }

  const reviewer = metadataValue(overlay, [
    'reviewer_agent',
    'reviewerAgent',
    'reviewed_by',
    'reviewedBy',
  ])
  if (!isNonEmptyString(reviewer)) {
    errors.push(`${overlayFile}: missing non-empty reviewer_agent`)
  }

  if (!Number.isInteger(overlay.reviewed_count) || overlay.reviewed_count !== baseRows.length) {
    errors.push(
      `${overlayFile}: reviewed_count must equal base row count `
      + `${baseRows.length}, found ${JSON.stringify(overlay.reviewed_count)}`,
    )
  }

  if (!isNonEmptyString(overlay.generation_sha256)
      || !/^[0-9a-f]{64}$/i.test(overlay.generation_sha256.trim())) {
    errors.push(`${overlayFile}: generation_sha256 must be a 64-character hexadecimal SHA-256`)
  } else if (overlay.generation_sha256.trim().toLowerCase() !== generationRawSha256) {
    errors.push(
      `${overlayFile}: generation_sha256 does not match the raw generation JSON `
      + `(${overlay.generation_sha256.trim().toLowerCase()} vs ${generationRawSha256})`,
    )
  }

  if (!Array.isArray(overlay.decisions)) {
    errors.push(`${overlayFile}: decisions must be an array covering every base ID`)
    return decisionMap
  }

  const replacementIds = new Set()
  for (const row of overlayRows) {
    const id = identifierOf(row, kind)
    if (id) replacementIds.add(id)
  }

  overlay.decisions.forEach((decision, index) => {
    if (!isPlainObject(decision)) return
    const id = [
      decision.id,
      decision.topic_key,
      decision.question_id,
      decision.question_key,
    ].find(isNonEmptyString)?.trim()
    if (!id) return
    if (decisionMap.has(id)) {
      errors.push(`${overlayFile}: duplicate decision ID ${id}`)
      return
    }
    decisionMap.set(id, decision)

    const verdict = isNonEmptyString(decision.decision)
      ? decision.decision.trim().toLowerCase()
      : null
    if (!['keep', 'revise', 'reject'].includes(verdict)) {
      errors.push(`${overlayFile}: decision ${id} must be keep, revise, or reject`)
    }
    if (!isNonEmptyString(decision.note)) {
      errors.push(`${overlayFile}: decision ${id} must include a non-empty review note`)
    }

    const score = Number(decision.quality_score)
    if (!Number.isFinite(score) || score < MIN_QUALITY_SCORE || score > 100) {
      errors.push(`${overlayFile}: decision ${id} quality_score must be ${MIN_QUALITY_SCORE}–100`)
    }

    if (verdict === 'keep' && replacementIds.has(id)) {
      errors.push(`${overlayFile}: keep decision ${id} must not have a replacement`)
    }
    if (verdict === 'revise' && !replacementIds.has(id)) {
      errors.push(`${overlayFile}: revise decision ${id} must have a complete same-ID replacement`)
    }
    if (verdict === 'reject') {
      errors.push(`${overlayFile}: reject decision ${id} blocks publication`)
      if (replacementIds.has(id)) {
        errors.push(`${overlayFile}: reject decision ${id} must not have a replacement`)
      }
    }

    if (index >= baseRows.length) {
      errors.push(`${overlayFile}: decisions contains more rows than the generation file`)
    }
  })

  for (const replacementId of replacementIds) {
    const decision = decisionMap.get(replacementId)
    const verdict = isPlainObject(decision) && isNonEmptyString(decision.decision)
      ? decision.decision.trim().toLowerCase()
      : null
    if (verdict !== 'revise') {
      errors.push(`${overlayFile}: replacement ${replacementId} requires a revise decision`)
    }
  }

  return decisionMap
}

function metadataValue(document, names) {
  if (!isPlainObject(document)) return undefined
  for (const name of names) {
    if (document[name] !== undefined) return document[name]
  }
  for (const containerName of ['audit', 'metadata', 'review', 'generation', 'pack']) {
    const container = document[containerName]
    if (!isPlainObject(container)) continue
    for (const name of names) {
      if (container[name] !== undefined) return container[name]
    }
  }
  return undefined
}

function validateAudit(rows, generation, overlay, decisionMap, errors) {
  const globalGenerator = metadataValue(generation, [
    'generator_agent',
    'generatorAgent',
    'generated_by',
    'generatedBy',
  ]) ?? metadataValue(overlay, [
    'generator_agent',
    'generatorAgent',
    'generated_by',
    'generatedBy',
  ])
  const globalReviewer = metadataValue(overlay, [
    'reviewer_agent',
    'reviewerAgent',
    'reviewed_by',
    'reviewedBy',
  ]) ?? metadataValue(generation, [
    'reviewer_agent',
    'reviewerAgent',
    'reviewed_by',
    'reviewedBy',
  ])
  const globalQuality = metadataValue(overlay, [
    'quality_score',
    'qualityScore',
    'review_quality_score',
  ]) ?? metadataValue(generation, [
    'quality_score',
    'qualityScore',
    'review_quality_score',
  ])

  const missingGenerator = []
  const missingReviewer = []
  const sameAgent = []
  const lowQuality = []

  for (const row of rows) {
    const id = identifierOf(row, 'topics') ?? identifierOf(row, 'pack') ?? '<unknown>'
    const decision = decisionMap.get(id)
    const generator = [
      row.generator_agent,
      row.generatorAgent,
      row.generated_by,
      row.generatedBy,
      globalGenerator,
    ].find(isNonEmptyString)
    const reviewer = [
      row.reviewer_agent,
      row.reviewerAgent,
      row.reviewed_by,
      row.reviewedBy,
      globalReviewer,
    ].find(isNonEmptyString)
    const qualityCandidate = [
      row.quality_score,
      row.qualityScore,
      row.review_quality_score,
      decision?.quality_score,
      decision?.qualityScore,
      globalQuality,
    ].find((value) => value !== undefined && value !== null && value !== '')
    const quality = Number(qualityCandidate)

    if (!generator) missingGenerator.push(id)
    if (!reviewer) missingReviewer.push(id)
    if (generator && reviewer && generator.trim() === reviewer.trim()) sameAgent.push(id)
    if (!Number.isFinite(quality) || quality < MIN_QUALITY_SCORE || quality > 100) {
      lowQuality.push(id)
    }
  }

  const summarize = (label, ids) => {
    if (!ids.length) return
    errors.push(`${label} for ${ids.length}/${rows.length} rows: ${ids.slice(0, 10).join(', ')}${ids.length > 10 ? ', …' : ''}`)
  }
  summarize('missing generator agent', missingGenerator)
  summarize('missing reviewer agent', missingReviewer)
  summarize('generator and reviewer must be different', sameAgent)
  summarize(`quality_score must be ${MIN_QUALITY_SCORE}–100`, lowQuality)

  return {
    generatorAgent: isNonEmptyString(globalGenerator) ? globalGenerator.trim() : null,
    reviewerAgent: isNonEmptyString(globalReviewer) ? globalReviewer.trim() : null,
    qualityScore: Number.isFinite(Number(globalQuality)) ? Number(globalQuality) : null,
  }
}

function findBannedKeys(value, errors, file, id, currentPath = '$') {
  if (Array.isArray(value)) {
    value.forEach((child, index) => findBannedKeys(child, errors, file, id, `${currentPath}[${index}]`))
    return
  }
  if (!isPlainObject(value)) return

  for (const [key, child] of Object.entries(value)) {
    const childPath = `${currentPath}.${key}`
    if (BANNED_COUPLING_KEYS.has(normalizeKey(key))) {
      errors.push(`${file} [${id ?? '<unknown>'}] ${childPath}: forbidden corpus/anime coupling key ${key}`)
    }
    findBannedKeys(child, errors, file, id, childPath)
  }
}

function findProperNames(value, warnings, file, id, currentPath = '$') {
  if (typeof value === 'string') {
    const matches = new Set()
    for (const pattern of PROPER_NAME_PATTERNS) {
      pattern.lastIndex = 0
      for (const match of value.matchAll(pattern)) matches.add(match[0])
    }
    if (matches.size) {
      warnings.push(
        `${file} [${id ?? '<unknown>'}] ${currentPath}: possible work/character proper name(s): `
        + [...matches].join(', '),
      )
    }
    return
  }
  if (Array.isArray(value)) {
    value.forEach((child, index) => findProperNames(child, warnings, file, id, `${currentPath}[${index}]`))
    return
  }
  if (!isPlainObject(value)) return
  for (const [key, child] of Object.entries(value)) {
    findProperNames(child, warnings, file, id, `${currentPath}.${key}`)
  }
}

function validateUniqueIds(rows, kind, errors, file) {
  const seen = new Map()
  rows.forEach((row, index) => {
    if (!isPlainObject(row)) {
      errors.push(`${file} [index ${index}]: row must be an object`)
      return
    }
    const id = identifierOf(row, kind)
    if (!id) {
      errors.push(`${file} [index ${index}]: missing non-empty ${kind === 'topics' ? 'topic_key' : 'question ID'}`)
      return
    }
    if (seen.has(id)) {
      errors.push(`${file} [${id}]: duplicate ID also used at index ${seen.get(id)}`)
    } else {
      seen.set(id, index)
    }
  })
  return seen
}

function requireStrings(row, fields, errors, file, id) {
  for (const field of fields) {
    if (!isNonEmptyString(row[field])) {
      errors.push(`${file} [${id}]: missing non-empty ${field}`)
    }
  }
}

function validateStringArray(value, field, errors, file, id, { allowEmpty = true } = {}) {
  if (!Array.isArray(value)) {
    errors.push(`${file} [${id}]: ${field} must be an array`)
    return
  }
  if (!allowEmpty && value.length === 0) {
    errors.push(`${file} [${id}]: ${field} must not be empty`)
  }
  if (value.some((item) => !isNonEmptyString(item))) {
    errors.push(`${file} [${id}]: every ${field} item must be a non-empty string`)
  }
  const normalized = value.filter(isNonEmptyString).map((item) => item.trim())
  if (new Set(normalized).size !== normalized.length) {
    errors.push(`${file} [${id}]: ${field} contains duplicates`)
  }
}

function validateTopic(row, errors, file) {
  const id = identifierOf(row, 'topics')
  if (!id || !isPlainObject(row)) return

  requireStrings(row, [
    'topic_key',
    'domain',
    'module_key',
    'name_ja',
    'name_zh',
    'short_definition_zh',
    'beginner_explanation_zh',
    'deep_explanation_zh',
    'caution_zh',
  ], errors, file, id)

  validateStringArray(row.prerequisite_topic_keys, 'prerequisite_topic_keys', errors, file, id)
  validateStringArray(row.tags, 'tags', errors, file, id, { allowEmpty: false })

  if (!isPlainObject(row.objectives)) {
    errors.push(`${file} [${id}]: objectives must be an object`)
  } else {
    const expected = STAGES.map((stage) => `${stage}_zh`)
    for (const key of expected) {
      if (!isNonEmptyString(row.objectives[key])) {
        errors.push(`${file} [${id}]: objectives.${key} must be non-empty`)
      }
    }
    const unexpected = Object.keys(row.objectives).filter((key) => !expected.includes(key))
    if (unexpected.length) {
      errors.push(`${file} [${id}]: unexpected objective keys: ${unexpected.join(', ')}`)
    }
  }

  if (!isPlainObject(row.original_example_spec)) {
    errors.push(`${file} [${id}]: original_example_spec must be an object`)
  } else {
    requireStrings(
      row.original_example_spec,
      ['form_zh', 'contrast_zh', 'constraints_zh'],
      errors,
      file,
      `${id}.original_example_spec`,
    )
  }
}

function detectPrerequisiteCycles(rows, errors, file) {
  const topicIds = new Set(rows.map((row) => identifierOf(row, 'topics')).filter(Boolean))
  const graph = new Map()

  for (const row of rows) {
    const id = identifierOf(row, 'topics')
    if (!id) continue
    const prerequisites = Array.isArray(row.prerequisite_topic_keys)
      ? row.prerequisite_topic_keys.filter(isNonEmptyString).map((value) => value.trim())
      : []
    for (const prerequisite of prerequisites) {
      if (prerequisite === id) {
        errors.push(`${file} [${id}]: topic cannot require itself`)
      } else if (!topicIds.has(prerequisite)) {
        errors.push(`${file} [${id}]: unknown prerequisite topic ${prerequisite}`)
      }
    }
    graph.set(id, prerequisites.filter((value) => topicIds.has(value)))
  }

  const state = new Map()
  const stack = []
  const reported = new Set()
  function visit(id) {
    const prior = state.get(id)
    if (prior === 'done') return
    if (prior === 'visiting') {
      const start = stack.indexOf(id)
      const cycle = [...stack.slice(start), id]
      const key = cycle.join(' -> ')
      if (!reported.has(key)) {
        errors.push(`${file}: prerequisite cycle detected: ${key}`)
        reported.add(key)
      }
      return
    }
    state.set(id, 'visiting')
    stack.push(id)
    for (const prerequisite of graph.get(id) ?? []) visit(prerequisite)
    stack.pop()
    state.set(id, 'done')
  }
  for (const id of graph.keys()) visit(id)
}

function validateTopics(document, rows, errors, warnings, file) {
  if (rows.length !== TOPIC_COUNT) {
    errors.push(`${file}: topics must contain exactly ${TOPIC_COUNT} rows, found ${rows.length}`)
  }
  if (isPlainObject(document) && document.topic_count !== undefined && document.topic_count !== TOPIC_COUNT) {
    errors.push(`${file}: topic_count must be ${TOPIC_COUNT}, found ${JSON.stringify(document.topic_count)}`)
  }

  const ids = validateUniqueIds(rows, 'topics', errors, file)
  if (ids.size !== rows.length) {
    errors.push(`${file}: unique topic ID count ${ids.size} does not equal row count ${rows.length}`)
  }

  const domainCounts = Object.fromEntries(Object.keys(DOMAIN_QUOTAS).map((domain) => [domain, 0]))
  for (const row of rows) {
    if (!isPlainObject(row)) continue
    const id = identifierOf(row, 'topics')
    validateTopic(row, errors, file)
    if (Object.hasOwn(DOMAIN_QUOTAS, row.domain)) {
      domainCounts[row.domain] += 1
    } else {
      errors.push(`${file} [${id ?? '<unknown>'}]: invalid foundation domain ${JSON.stringify(row.domain)}`)
    }
    findBannedKeys(row, errors, file, id)
    findProperNames(row, warnings, file, id)
  }

  for (const [domain, quota] of Object.entries(DOMAIN_QUOTAS)) {
    if (domainCounts[domain] !== quota) {
      errors.push(`${file}: domain ${domain} must contain ${quota} topics, found ${domainCounts[domain]}`)
    }
  }

  detectPrerequisiteCycles(rows, errors, file)
  return { domainCounts, uniqueIds: ids.size }
}

function stageOf(question) {
  const raw = [question.stage, question.foundation_stage, question.level]
    .find(isNonEmptyString)
  return raw?.trim().toUpperCase() ?? null
}

function difficultyMatchesStage(difficulty, stage) {
  if (!stage || !STAGE_SET.has(stage)) return false
  const number = Number(stage.slice(1))
  if (typeof difficulty === 'number') return difficulty === number
  if (typeof difficulty === 'string') {
    const normalized = difficulty.trim().toUpperCase()
    return normalized === stage || Number(normalized) === number
  }
  return false
}

function optionIdOf(option) {
  if (!isPlainObject(option)) return null
  return [option.id, option.option_id, option.optionId, option.key]
    .find(isNonEmptyString)
    ?.trim() ?? null
}

function optionTextOf(option) {
  if (!isPlainObject(option)) return null
  return [
    option.text,
    option.text_zh,
    option.label,
    option.label_zh,
    option.content,
    option.content_zh,
  ].find(isNonEmptyString)?.trim() ?? null
}

function optionsOf(question) {
  return [
    question.options,
    question.options_json,
    question.option_list,
  ].find(Array.isArray) ?? null
}

function correctOptionIdOf(question) {
  const direct = [
    question.correct_option_id,
    question.correctOptionId,
    question.answer,
  ].find(isNonEmptyString)
  if (direct) return direct.trim()

  const answer = [question.answer_json, question.answer]
    .find(isPlainObject)
  if (!answer) return null
  return [
    answer.option_id,
    answer.optionId,
    answer.correct_option_id,
    answer.correctOptionId,
  ].find(isNonEmptyString)?.trim() ?? null
}

function wrongExplanationsOf(question) {
  return [
    question.wrong_explanations,
    question.wrong_explanations_json,
    question.distractor_explanations,
  ].find(isPlainObject) ?? null
}

function validateStimulus(stimulus, errors, file, id) {
  if (!isPlainObject(stimulus)) {
    errors.push(`${file} [${id}]: stimulus must be an object`)
    return
  }
  if (!isNonEmptyString(stimulus.kind) || !STIMULUS_KINDS.has(stimulus.kind.trim())) {
    errors.push(
      `${file} [${id}]: stimulus.kind must be one of `
      + `${[...STIMULUS_KINDS].join(', ')}`,
    )
    return
  }

  const kind = stimulus.kind.trim()
  if (kind === 'sentence' && !isNonEmptyString(stimulus.ja_text)) {
    errors.push(`${file} [${id}]: sentence stimulus must include non-empty ja_text`)
  }
  if (kind === 'dialogue') {
    if (!Array.isArray(stimulus.turns) || stimulus.turns.length < 2) {
      errors.push(`${file} [${id}]: dialogue stimulus must include at least two turns`)
    } else {
      stimulus.turns.forEach((turn, index) => {
        if (!isPlainObject(turn)) {
          errors.push(`${file} [${id}]: stimulus.turns[${index}] must be an object`)
          return
        }
        const speaker = [
          turn.speaker,
          turn.speaker_label,
          turn.speaker_zh,
        ].find(isNonEmptyString)
        const text = [turn.ja_text, turn.text].find(isNonEmptyString)
        if (!speaker) {
          errors.push(`${file} [${id}]: stimulus.turns[${index}] needs a non-empty speaker label`)
        }
        if (!text) {
          errors.push(`${file} [${id}]: stimulus.turns[${index}] needs non-empty Japanese text`)
        }
      })
    }
  }
  if (kind === 'contrast') {
    if (!Array.isArray(stimulus.items) || stimulus.items.length < 2) {
      errors.push(`${file} [${id}]: contrast stimulus must include at least two items`)
    } else {
      stimulus.items.forEach((item, index) => {
        const text = typeof item === 'string'
          ? item
          : isPlainObject(item)
            ? [item.ja_text, item.text, item.form].find(isNonEmptyString)
            : null
        if (!isNonEmptyString(text)) {
          errors.push(`${file} [${id}]: stimulus.items[${index}] needs non-empty text`)
        }
      })
    }
  }
  if (kind === 'metalinguistic') {
    const material = [
      stimulus.ja_text,
      stimulus.text,
      stimulus.form,
      stimulus.description_zh,
    ].find(isNonEmptyString)
    if (!material) {
      errors.push(`${file} [${id}]: metalinguistic stimulus needs non-empty material`)
    }
  }
}

function validateQuestion(row, errors, file) {
  const id = identifierOf(row, 'pack')
  if (!id || !isPlainObject(row)) return

  const topicKey = topicKeyOf(row)
  if (!topicKey) errors.push(`${file} [${id}]: missing non-empty topic_key/topic_id`)

  const stage = stageOf(row)
  if (!stage || !STAGE_SET.has(stage)) {
    errors.push(`${file} [${id}]: stage must be one of ${STAGES.join(', ')}`)
  }
  if (!difficultyMatchesStage(row.difficulty, stage)) {
    errors.push(`${file} [${id}]: difficulty ${JSON.stringify(row.difficulty)} must match stage ${stage ?? '<missing>'}`)
  }

  if (!isNonEmptyString(row.question_type) || !QUESTION_TYPES.has(row.question_type.trim())) {
    errors.push(`${file} [${id}]: invalid question_type ${JSON.stringify(row.question_type)}`)
  }
  if (!isNonEmptyString(row.source_kind) || !SOURCE_KINDS.has(row.source_kind.trim())) {
    errors.push(`${file} [${id}]: invalid source_kind ${JSON.stringify(row.source_kind)}`)
  }
  validateStimulus(row.stimulus, errors, file, id)
  requireStrings(row, [
    'prompt_zh',
    'hint_zh',
    'explanation_zh',
    'deep_explanation_zh',
    'caution_note_zh',
  ], errors, file, id)
  validateStringArray(row.tags, 'tags', errors, file, id, { allowEmpty: false })

  if (!Number.isInteger(row.sort_order) || row.sort_order < 0) {
    errors.push(`${file} [${id}]: sort_order must be a non-negative integer`)
  }
  const hasTransferExample = row.transfer_example_ja !== undefined && row.transfer_example_ja !== null
  const hasTransferExplanation = row.transfer_explanation_zh !== undefined && row.transfer_explanation_zh !== null
  if (hasTransferExample !== hasTransferExplanation
      || (hasTransferExample && !isNonEmptyString(row.transfer_example_ja))
      || (hasTransferExplanation && !isNonEmptyString(row.transfer_explanation_zh))) {
    errors.push(`${file} [${id}]: transfer example and explanation must be a non-empty pair or both absent`)
  }

  const options = optionsOf(row)
  if (!options) {
    errors.push(`${file} [${id}]: options/options_json must be an array`)
    return
  }
  if (options.length !== OPTION_COUNT) {
    errors.push(`${file} [${id}]: options must contain exactly ${OPTION_COUNT} items, found ${options.length}`)
  }

  const optionIds = []
  options.forEach((option, index) => {
    const optionId = optionIdOf(option)
    if (!optionId) {
      errors.push(`${file} [${id}]: options[${index}] is missing a non-empty option ID`)
    } else {
      optionIds.push(optionId)
    }
    if (!optionTextOf(option)) {
      errors.push(`${file} [${id}]: options[${index}] is missing non-empty learner-facing text`)
    }
  })
  if (new Set(optionIds).size !== optionIds.length) {
    errors.push(`${file} [${id}]: option IDs must be unique`)
  }
  const canonicalOptionIds = ['A', 'B', 'C', 'D']
  const normalizedOptionIds = [...new Set(optionIds)].sort()
  if (normalizedOptionIds.length !== canonicalOptionIds.length
      || normalizedOptionIds.some((optionId, index) => optionId !== canonicalOptionIds[index])) {
    errors.push(`${file} [${id}]: option IDs must be exactly A, B, C, D`)
  }

  const correctId = correctOptionIdOf(row)
  if (!correctId) {
    errors.push(`${file} [${id}]: missing single correct option ID`)
  } else if (!optionIds.includes(correctId)) {
    errors.push(`${file} [${id}]: correct option ${correctId} is not one of the four option IDs`)
  }

  const wrongExplanations = wrongExplanationsOf(row)
  if (!wrongExplanations) {
    errors.push(`${file} [${id}]: wrong_explanations must be an object keyed by wrong option ID`)
  } else if (correctId && optionIds.includes(correctId)) {
    const expectedWrongIds = optionIds.filter((optionId) => optionId !== correctId).sort()
    const actualKeys = Object.keys(wrongExplanations).sort()
    const missing = expectedWrongIds.filter((optionId) => !isNonEmptyString(wrongExplanations[optionId]))
    const unexpected = actualKeys.filter((optionId) => !expectedWrongIds.includes(optionId))
    if (expectedWrongIds.length !== OPTION_COUNT - 1) {
      errors.push(`${file} [${id}]: exactly three wrong option IDs are required`)
    }
    if (missing.length) {
      errors.push(`${file} [${id}]: wrong_explanations missing non-empty entries for ${missing.join(', ')}`)
    }
    if (unexpected.length) {
      errors.push(`${file} [${id}]: wrong_explanations contains unexpected/correct IDs: ${unexpected.join(', ')}`)
    }
    if (actualKeys.length !== OPTION_COUNT - 1) {
      errors.push(`${file} [${id}]: wrong_explanations must contain exactly three keys, found ${actualKeys.length}`)
    }
  }
}

function validatePack(document, rows, errors, warnings, file) {
  if (rows.length !== PACK_QUESTION_COUNT) {
    errors.push(`${file}: pack must contain exactly ${PACK_QUESTION_COUNT} questions, found ${rows.length}`)
  }
  if (!isPlainObject(document?.pack)) {
    errors.push(`${file}: top-level pack must be an object`)
  } else {
    const pack = document.pack
    requireStrings(
      pack,
      ['id', 'curriculum_version', 'title_zh', 'description_zh', 'generator_agent'],
      errors,
      file,
      'pack',
    )
    if (isNonEmptyString(pack.id) && !/^[a-z0-9]+(?:[-_][a-z0-9]+)*$/.test(pack.id)) {
      errors.push(`${file} [pack]: id has an invalid format`)
    }
    if (pack.curriculum_version !== 'foundation-v1') {
      errors.push(`${file} [pack]: curriculum_version must be foundation-v1`)
    }
    if (!Number.isInteger(pack.batch_no) || pack.batch_no < 1 || pack.batch_no > 3) {
      errors.push(`${file} [pack]: batch_no must be an integer from 1 to 3`)
    }
    if (pack.question_count !== PACK_QUESTION_COUNT) {
      errors.push(`${file} [pack]: question_count must be ${PACK_QUESTION_COUNT}`)
    }
    if (pack.topic_count !== PACK_TOPIC_COUNT) {
      errors.push(`${file} [pack]: topic_count must be ${PACK_TOPIC_COUNT}`)
    }
    findBannedKeys(pack, errors, file, 'pack')
    findProperNames(pack, warnings, file, 'pack')
  }

  const ids = validateUniqueIds(rows, 'pack', errors, file)
  if (ids.size !== rows.length) {
    errors.push(`${file}: unique question ID count ${ids.size} does not equal row count ${rows.length}`)
  }

  const topicStages = new Map()
  const answerCounts = Object.fromEntries(['A', 'B', 'C', 'D'].map((id) => [id, 0]))
  const sortOrders = new Map()
  for (const row of rows) {
    if (!isPlainObject(row)) continue
    const id = identifierOf(row, 'pack')
    validateQuestion(row, errors, file)
    const topicKey = topicKeyOf(row)
    const stage = stageOf(row)
    if (topicKey) {
      if (!topicStages.has(topicKey)) topicStages.set(topicKey, [])
      topicStages.get(topicKey).push(stage)
    }
    const answerId = correctOptionIdOf(row)
    if (Object.hasOwn(answerCounts, answerId)) answerCounts[answerId] += 1
    if (Number.isInteger(row.sort_order)) {
      if (sortOrders.has(row.sort_order)) {
        errors.push(
          `${file} [${id ?? '<unknown>'}]: duplicate sort_order ${row.sort_order} `
          + `also used by ${sortOrders.get(row.sort_order)}`,
        )
      } else {
        sortOrders.set(row.sort_order, id)
      }
    }
    findBannedKeys(row, errors, file, id)
    findProperNames(row, warnings, file, id)
  }

  if (topicStages.size !== PACK_TOPIC_COUNT) {
    errors.push(`${file}: pack must cover exactly ${PACK_TOPIC_COUNT} unique topics, found ${topicStages.size}`)
  }
  for (const [topicKey, stages] of topicStages) {
    const validStages = stages.filter((stage) => stage && STAGE_SET.has(stage))
    const missing = STAGES.filter((stage) => !validStages.includes(stage))
    const duplicates = STAGES.filter((stage) => validStages.filter((value) => value === stage).length > 1)
    if (stages.length !== STAGES.length || missing.length || duplicates.length) {
      errors.push(
        `${file} [${topicKey}]: must contain exactly one question for each F1–F4; `
        + `found ${JSON.stringify(stages)}, missing ${missing.join(', ') || 'none'}, `
        + `duplicates ${duplicates.join(', ') || 'none'}`,
      )
    }
  }

  for (const [optionId, count] of Object.entries(answerCounts)) {
    if (count !== PACK_QUESTION_COUNT / OPTION_COUNT) {
      errors.push(
        `${file}: correct-answer quota for ${optionId} must be `
        + `${PACK_QUESTION_COUNT / OPTION_COUNT}, found ${count}`,
      )
    }
  }
  if (sortOrders.size === PACK_QUESTION_COUNT) {
    const expectedSortOrders = Array.from({ length: PACK_QUESTION_COUNT }, (_, index) => index)
    const missingSortOrders = expectedSortOrders.filter((index) => !sortOrders.has(index))
    const outOfRange = [...sortOrders.keys()].filter(
      (index) => index < 0 || index >= PACK_QUESTION_COUNT,
    )
    if (missingSortOrders.length || outOfRange.length) {
      errors.push(
        `${file}: sort_order must be exactly 0–${PACK_QUESTION_COUNT - 1}; `
        + `missing ${missingSortOrders.join(', ') || 'none'}, `
        + `out of range ${outOfRange.join(', ') || 'none'}`,
      )
    }
  }

  if (isPlainObject(document) && Array.isArray(document.topic_ids)) {
    const declared = document.topic_ids.filter(isNonEmptyString).map((value) => value.trim())
    const declaredSet = new Set(declared)
    if (declared.length !== PACK_TOPIC_COUNT || declaredSet.size !== PACK_TOPIC_COUNT) {
      errors.push(`${file}: topic_ids must contain exactly ${PACK_TOPIC_COUNT} unique IDs`)
    }
    const derivedSet = new Set(topicStages.keys())
    const missing = [...derivedSet].filter((id) => !declaredSet.has(id))
    const extra = [...declaredSet].filter((id) => !derivedSet.has(id))
    if (missing.length || extra.length) {
      errors.push(`${file}: declared topic_ids disagree with questions (missing ${missing.join(', ') || 'none'}; extra ${extra.join(', ') || 'none'})`)
    }
  } else {
    errors.push(`${file}: top-level topic_ids must be an array of exactly ${PACK_TOPIC_COUNT} IDs`)
  }

  return {
    topicCount: topicStages.size,
    uniqueIds: ids.size,
    answerCounts,
  }
}

function runValidation(kind, generationFile, overlayFile) {
  const errors = []
  const warnings = []
  const generationRead = readJson(generationFile, errors)
  const overlayRead = overlayFile
    ? readJson(overlayFile, errors)
    : { file: null, value: null, rawSha256: null }

  if (errors.length || generationRead.value === null || (overlayFile && overlayRead.value === null)) {
    return {
      mode: kind,
      generationFile: generationRead.file,
      reviewOverlayFile: overlayRead.file,
      errors,
      warnings,
      canonicalSha256: null,
    }
  }

  const baseRows = extractGenerationRows(
    generationRead.value,
    kind,
    errors,
    generationRead.file,
  )
  const overlayRows = extractOverlayRows(
    overlayRead.value,
    kind,
    errors,
    overlayRead.file ?? '<no overlay>',
  )
  const { effectiveRows, replacementsApplied } = applyReviewOverlay(
    baseRows,
    overlayRows,
    kind,
    errors,
    overlayRead.file ?? '<no overlay>',
  )

  const baseIdMap = validateUniqueIds(baseRows, kind, errors, generationRead.file)
  if (baseIdMap.size !== baseRows.length) {
    errors.push(`${generationRead.file}: base candidate count and unique ID count must match exactly`)
  }

  if (overlayFile) {
    const reviewedIds = collectReviewCoverage(
      overlayRead.value,
      overlayRows,
      kind,
      errors,
      overlayRead.file,
    )
    assertExactReviewCoverage(
      baseRows,
      reviewedIds,
      kind,
      errors,
      overlayRead.file,
    )
  }

  const decisionMap = validateReviewOverlayProtocol({
    overlay: overlayRead.value,
    overlayRows,
    baseRows,
    kind,
    generationRawSha256: generationRead.rawSha256,
    errors,
    overlayFile: overlayRead.file ?? '<no overlay>',
  })

  const audit = validateAudit(
    effectiveRows,
    generationRead.value,
    overlayRead.value,
    decisionMap,
    errors,
  )
  const details = kind === 'topics'
    ? validateTopics(generationRead.value, effectiveRows, errors, warnings, generationRead.file)
    : validatePack(generationRead.value, effectiveRows, errors, warnings, generationRead.file)

  return {
    mode: kind,
    generationFile: generationRead.file,
    reviewOverlayFile: overlayRead.file,
    rows: effectiveRows.length,
    replacementsApplied,
    generationFileSha256: generationRead.rawSha256,
    reviewOverlayFileSha256: overlayRead.rawSha256,
    audit,
    ...details,
    errors,
    warnings,
    generationCanonicalSha256: canonicalSha256(baseRows),
    canonicalSha256: canonicalSha256(effectiveRows),
  }
}

function runSelfTest() {
  const failures = []
  function assert(condition, message) {
    if (!condition) failures.push(message)
  }

  assert(
    canonicalStringify({ b: 2, a: { d: 4, c: 3 } })
      === canonicalStringify({ a: { c: 3, d: 4 }, b: 2 }),
    'canonical object-key ordering is unstable',
  )
  assert(
    canonicalSha256({ b: 2, a: 1 }) === canonicalSha256({ a: 1, b: 2 }),
    'canonical SHA-256 changes with object key order',
  )

  const overlayErrors = []
  const { effectiveRows, replacementsApplied } = applyReviewOverlay(
    [{ id: 'q-1', marker: 'base-1' }, { id: 'q-2', marker: 'base-2' }],
    [{ id: 'q-2', marker: 'reviewed-2' }, { id: 'q-3', marker: 'must-not-append' }],
    'pack',
    overlayErrors,
    '<self-test>',
  )
  assert(effectiveRows.length === 2, 'overlay appended a row')
  assert(effectiveRows[1].marker === 'reviewed-2', 'known overlay replacement was not applied')
  assert(replacementsApplied === 1, 'replacement count should exclude unknown IDs')
  assert(overlayErrors.some((message) => message.includes('cannot append')), 'unknown overlay ID was not rejected')

  const protocolErrors = []
  const protocolBase = [{ id: 'q-1' }, { id: 'q-2' }]
  const protocolOverlayRows = [{ id: 'q-2' }]
  const protocolHash = 'a'.repeat(64)
  const protocolDecisionMap = validateReviewOverlayProtocol({
    overlay: {
      reviewer_agent: 'review-agent',
      reviewed_count: 2,
      generation_sha256: protocolHash,
      decisions: [
        { id: 'q-1', decision: 'keep', quality_score: 95, note: 'opaque-review-note' },
        { id: 'q-2', decision: 'revise', quality_score: 96, note: 'opaque-review-note' },
      ],
      replacements: protocolOverlayRows,
    },
    overlayRows: protocolOverlayRows,
    baseRows: protocolBase,
    kind: 'pack',
    generationRawSha256: protocolHash,
    errors: protocolErrors,
    overlayFile: '<self-test>',
  })
  assert(protocolErrors.length === 0, `valid review protocol failed: ${protocolErrors.join('; ')}`)
  assert(protocolDecisionMap.size === 2, 'review decision map did not cover every base ID')

  const rejectErrors = []
  validateReviewOverlayProtocol({
    overlay: {
      reviewer_agent: 'review-agent',
      reviewed_count: 1,
      generation_sha256: protocolHash,
      decisions: [
        { id: 'q-1', decision: 'reject', quality_score: 99, note: 'opaque-review-note' },
      ],
      replacements: [],
    },
    overlayRows: [],
    baseRows: [{ id: 'q-1' }],
    kind: 'pack',
    generationRawSha256: protocolHash,
    errors: rejectErrors,
    overlayFile: '<self-test>',
  })
  assert(rejectErrors.some((message) => message.includes('blocks publication')), 'reject did not block publication')

  const bannedErrors = []
  findBannedKeys(
    { nested: [{ workSlug: 'opaque-test-value' }] },
    bannedErrors,
    '<self-test>',
    'opaque-id',
  )
  assert(bannedErrors.length === 1, 'recursive banned-key detection failed')

  const result = {
    selfTest: true,
    errors: failures,
    warnings: [],
    canonicalSha256: canonicalSha256({
      tests: [
        'canonical-order',
        'overlay-no-append',
        'review-protocol',
        'reject-blocks',
        'recursive-banned-key',
      ],
    }),
  }
  console.log(JSON.stringify(result, null, 2))
  return failures.length === 0 ? 0 : 1
}

const [command, generationFile, overlayFile, ...extra] = process.argv.slice(2)

if (command === '--self-test') {
  process.exitCode = runSelfTest()
} else if (!['topics', 'pack'].includes(command) || !generationFile || extra.length) {
  console.error(usage())
  process.exitCode = 2
} else {
  const result = runValidation(command, generationFile, overlayFile)
  console.log(JSON.stringify(result, null, 2))
  process.exitCode = result.errors.length ? 1 : 0
}
