import { describe, expect, it } from 'vitest'
import wave1Document from '../archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave1_effective.json'
import wave2Document from '../archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave2_effective.json'
import wave3Document from '../archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave3_effective.json'
import {
  buildFoundationListPlan,
  decodeFoundationCursor,
  encodeFoundationCursor,
  mapFoundationPack,
  mapFoundationQuestion,
  mapFoundationTopic,
  matchFoundationApiRoute,
  reviewRoute,
} from './worker'

type EffectiveQuestion = {
  id: string
  topic_id: string
  stage: string
  question_type: string
  source_kind: string
  stimulus: unknown
  prompt_zh: string
  options: unknown
  answer: unknown
  hint_zh: string
  explanation_zh: string
  deep_explanation_zh: string
  caution_note_zh: string
  wrong_explanations: unknown
  transfer_example_ja?: string
  transfer_explanation_zh?: string
  difficulty: number
  tags: string[]
  sort_order: number
}

function loadEffectiveQuestions(wave: number): EffectiveQuestion[] {
  const documents = [wave1Document, wave2Document, wave3Document] as unknown as {
    questions: EffectiveQuestion[]
  }[]
  return documents[wave - 1]?.questions ?? []
}

function effectiveQuestionRow(question: EffectiveQuestion, wave: number) {
  return {
    id: question.id,
    pack_id: `foundation-v1-wave-${wave}`,
    topic_id: question.topic_id,
    curriculum_version: 'foundation-v1',
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
    status: 'published',
    quality_score: 96,
    content_version: 1,
    content_sha256: '1'.repeat(64),
  }
}

const publishedTopicRow = {
  id: 'syn_case_particles',
  curriculum_version: 'foundation-v1',
  domain: 'syntax',
  module_id: 'case_system',
  sort_order: 19,
  title_ja: '格助詞',
  title_zh: '格助词',
  short_definition_zh: '标示名词短语与谓词之间的格关系。',
  beginner_explanation_zh: '先判断谓词需要哪些参与者，再判断助词怎样标示它们。',
  deep_explanation_zh: '格助词把论元结构与表层名词短语联系起来。',
  caution_note_zh: '不能把一个助词机械地对应成一个中文介词。',
  prerequisite_topic_ids: ['syn_constituent_order'],
  learning_objectives_json: {
    F1_zh: '识别格助词。',
    F2_zh: '分析格关系。',
    F3_zh: '诊断格标记差异。',
    F4_zh: '迁移到新句。',
  },
  example_spec_json: {
    form_zh: '使用中性原创句。',
    contrast_zh: '比较两个格标记。',
    constraints_zh: '不借用动漫台词。',
  },
  tags_json: ['case', 'particles'],
  status: 'published',
  quality_score: 98,
}

const publishedPackRow = {
  id: 'foundation-v1-wave-1',
  curriculum_version: 'foundation-v1',
  batch_no: 1,
  title_zh: '语音、文字与词形基础',
  description_zh: '基础题包。',
  topic_count: 20,
  question_count: 80,
  domain_quotas_json: {
    phonology_writing: 8,
    morphology: 10,
    syntax: 2,
  },
  status: 'published',
  quality_score: 97,
}

const publishedQuestionRow = {
  id: 'foundation-v1-syn-case-particles-f1',
  pack_id: 'foundation-v1-wave-1',
  topic_id: 'syn_case_particles',
  curriculum_version: 'foundation-v1',
  stage: 'F1',
  question_type: 'single_choice',
  source_kind: 'original_sentence',
  stimulus_json: {
    kind: 'sentence',
    ja_text: '先生が学生に本を渡した。',
    zh_context: '老师把书递给了学生。',
  },
  prompt_zh: '哪个助词标示接受者？',
  options_json: [
    { id: 'A', text: 'が' },
    { id: 'B', text: 'に' },
    { id: 'C', text: 'を' },
    { id: 'D', text: 'は' },
  ],
  answer_json: { option_id: 'B' },
  hint_zh: '先找出谁得到书。',
  explanation_zh: '「に」标示接受者。',
  deep_explanation_zh: '授受动词的目标论元在这里由「に」标示。',
  caution_note_zh: '「に」还有位置、时间等其他用法。',
  wrong_explanations_json: {
    A: '「が」标示主语。',
    C: '「を」标示被转移的对象。',
    D: '原句中没有「は」。',
  },
  transfer_example_ja: null,
  transfer_explanation_zh: null,
  difficulty: 1,
  tags_json: ['case', 'ni'],
  sort_order: 76,
  status: 'published',
  quality_score: 99,
  content_version: 3,
  content_sha256: 'a'.repeat(64),
}

describe('foundation Worker routes and keyset plans', () => {
  it('routes foundation review items back to the foundation track without fake anime provenance', () => {
    expect(reviewRoute({
      deviceId: 'device',
      itemId: 'foundation-v1-question-1',
      itemType: 'exercise',
      workSlug: '',
      episode: 0,
      state: 'bad',
      nextReviewOn: '',
      lastReviewedAt: '',
      payload: {
        track: 'foundation',
        packId: 'foundation-v1-wave-1',
        topicId: 'syn_case_particles',
        stage: 'F3',
        questionType: 'syntax_relation',
      },
    })).toBe(
      '/linguistic-training?track=foundation&packId=foundation-v1-wave-1&topicId=syn_case_particles'
        + '&stage=F3&questionType=syntax_relation&questionId=foundation-v1-question-1',
    )
  })

  it('matches only the three exact GET routes', () => {
    expect(matchFoundationApiRoute('GET', '/api/linguistics/foundation/topics')).toBe('topics')
    expect(matchFoundationApiRoute('GET', '/api/linguistics/foundation/packs')).toBe('packs')
    expect(matchFoundationApiRoute('GET', '/api/linguistics/foundation/questions')).toBe('questions')
    expect(matchFoundationApiRoute('GET', '/api/linguistic-exercises')).toBeNull()
    expect(() => matchFoundationApiRoute('POST', '/api/linguistics/foundation/topics')).toThrow('Method not allowed')
  })

  it('builds a published-only topics query with limit plus one', () => {
    const plan = buildFoundationListPlan(
      'topics',
      new URL('https://example.test/api/linguistics/foundation/topics?domain=syntax&limit=2'),
    )
    const query = new URL(plan.path, 'https://supabase.test').searchParams

    expect(plan.limit).toBe(2)
    expect(plan.path).toContain('/rest/v1/linguistic_foundation_topics?')
    expect(query.get('status')).toBe('eq.published')
    expect(query.get('curriculum_version')).toBe('eq.foundation-v1')
    expect(query.get('domain')).toBe('eq.syntax')
    expect(query.get('order')).toBe('sort_order.asc,id.asc')
    expect(query.get('limit')).toBe('3')
  })

  it('binds question cursors to the resource and normalized filters', () => {
    const firstPlan = buildFoundationListPlan(
      'questions',
      new URL('https://example.test/api/linguistics/foundation/questions?packId=foundation-v1-wave-1&limit=2'),
    )
    const cursor = encodeFoundationCursor({
      version: 1,
      resource: 'questions',
      filterKey: firstPlan.filterKey,
      packId: 'foundation-v1-wave-1',
      sortOrder: 5,
      id: 'foundation-v1-question-5',
    })
    const nextPlan = buildFoundationListPlan(
      'questions',
      new URL(`https://example.test/api/linguistics/foundation/questions?packId=foundation-v1-wave-1&limit=2&cursor=${cursor}`),
    )
    const query = new URL(nextPlan.path, 'https://supabase.test').searchParams

    expect(query.get('pack_id')).toBe('eq.foundation-v1-wave-1')
    expect(query.get('limit')).toBe('3')
    expect(query.get('order')).toBe('pack_id.asc,sort_order.asc,id.asc')
    expect(query.get('or')).toContain('sort_order.gt.5')

    expect(() => buildFoundationListPlan(
      'questions',
      new URL(`https://example.test/api/linguistics/foundation/questions?packId=foundation-v1-wave-2&cursor=${cursor}`),
    )).toThrow('cursor does not match')
  })

  it('rejects malformed paging inputs and cursors', () => {
    expect(() => buildFoundationListPlan(
      'topics',
      new URL('https://example.test/api/linguistics/foundation/topics?limit=0'),
    )).toThrow('limit must be')
    expect(() => buildFoundationListPlan(
      'topics',
      new URL('https://example.test/api/linguistics/foundation/topics?limit=2&limit=3'),
    )).toThrow('Duplicate query parameter')
    expect(() => buildFoundationListPlan(
      'topics',
      new URL('https://example.test/api/linguistics/foundation/topics?offset=1'),
    )).toThrow('Unknown query parameter')
    expect(() => decodeFoundationCursor('not.a.cursor')).toThrow('cursor is invalid')
  })
})

describe('foundation Worker row validation', () => {
  it('maps all 240 reviewed stimuli without dropping reviewed context', () => {
    const mapped = [1, 2, 3].flatMap((wave) => (
      loadEffectiveQuestions(wave).map((question) => (
        mapFoundationQuestion(effectiveQuestionRow(question, wave))
      ))
    ))

    expect(mapped).toHaveLength(240)
    expect(new Set(mapped.map((question) => question.id)).size).toBe(240)
    expect(mapped.filter((question) => question.stimulus.kind === 'sentence')).toHaveLength(69)
    expect(mapped.filter((question) => question.stimulus.kind === 'dialogue')).toHaveLength(30)
    expect(mapped.filter((question) => question.stimulus.kind === 'contrast')).toHaveLength(58)
    expect(mapped.filter((question) => question.stimulus.kind === 'metalinguistic')).toHaveLength(83)

    const dialogue = mapped.find((question) => question.id === 'foundation-v1-prag-deixis-f2')
    expect(dialogue?.stimulus).toMatchObject({
      kind: 'dialogue',
      zhContext: expect.any(String),
    })

    const rawStringContrast = mapped.find(
      (question) => question.id === 'foundation-v1-syn-te-clause-linking-f3',
    )
    expect(rawStringContrast?.stimulus).toEqual({
      kind: 'contrast',
      items: [
        { text: '係員が説明して、利用者が操作した。' },
        { text: '係員が説明して、操作した。' },
      ],
      zhContext: '目标意义是“负责人说明，使用者操作”，两人不同。',
    })

    const annotatedContrast = mapped.find(
      (question) => question.id === 'foundation-v1-morph_auxiliary_chain-f3',
    )
    expect(annotatedContrast?.stimulus).toMatchObject({
      kind: 'contrast',
      items: [
        { noteZh: '[書く-使役-被动]-否定-过去' },
        { noteZh: '[書く-否定]-使役-被动-过去' },
      ],
    })

    const metalinguistic = mapped.find(
      (question) => question.id === 'foundation-v1-soc-contact-borrowing-f1',
    )
    expect(metalinguistic?.stimulus).toMatchObject({
      kind: 'metalinguistic',
      form: expect.stringContaining('strike'),
      descriptionZh: expect.stringContaining('日语音系'),
    })
  })

  it('maps published topic and pack rows to camel-case DTOs', () => {
    const topic = mapFoundationTopic(publishedTopicRow)
    const pack = mapFoundationPack(publishedPackRow)

    expect(topic.id).toBe('syn_case_particles')
    expect(topic.learningObjectives.F4Zh).toBe('迁移到新句。')
    expect(topic.exampleSpec.constraintsZh).toBe('不借用动漫台词。')
    expect(pack.id).toBe('foundation-v1-wave-1')
    expect(pack.domainQuotas.morphology).toBe(10)
  })

  it('maps a valid question and validates its answer and stage', () => {
    const question = mapFoundationQuestion(publishedQuestionRow)

    expect(question.stimulus).toEqual({
      kind: 'sentence',
      jaText: '先生が学生に本を渡した。',
      zhContext: '老师把书递给了学生。',
    })
    expect(question.answer.optionId).toBe('B')
    expect(question.wrongExplanations.C).toContain('转移')
    expect(question.contentVersion).toBe(3)
    expect(question.contentHash).toBe('a'.repeat(64))
  })

  it('rejects malformed nested teaching payloads instead of defaulting them', () => {
    expect(() => mapFoundationTopic({
      ...publishedTopicRow,
      learning_objectives_json: { F1_zh: '只有一个目标' },
    })).toThrow('F2_zh')
    expect(() => mapFoundationQuestion({
      ...publishedQuestionRow,
      answer_json: { option_id: 'Z' },
    })).toThrow('answer option_id')
    expect(() => mapFoundationQuestion({
      ...publishedQuestionRow,
      difficulty: 2,
    })).toThrow('difficulty does not match stage')
    expect(() => mapFoundationPack({
      ...publishedPackRow,
      question_count: 79,
    })).toThrow('question_count')
  })
})
