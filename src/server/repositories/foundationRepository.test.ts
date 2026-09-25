import { describe, expect, it } from 'vitest'
import {
  buildFoundationPacksPath,
  buildFoundationQuestionsPath,
  buildFoundationTopicsPath,
  parseFoundationPackPage,
  parseFoundationQuestionPage,
  parseFoundationTopicPage,
} from './animeRepository'

const topicDto = {
  id: 'syn_case_particles',
  curriculumVersion: 'foundation-v1',
  domain: 'syntax',
  moduleId: 'case_system',
  sortOrder: 19,
  titleJa: '格助詞',
  titleZh: '格助词',
  shortDefinitionZh: '标示格关系。',
  beginnerExplanationZh: '先寻找谓词的参与者。',
  deepExplanationZh: '格标记连接论元结构和表层形式。',
  cautionNoteZh: '助词并非一对一翻译。',
  prerequisiteTopicIds: ['syn_constituent_order'],
  learningObjectives: {
    F1Zh: '识别。',
    F2Zh: '解析。',
    F3Zh: '诊断。',
    F4Zh: '迁移。',
  },
  exampleSpec: {
    formZh: '原创中性句。',
    contrastZh: '比较格标记。',
    constraintsZh: '不使用作品语料。',
  },
  tags: ['case'],
  status: 'published',
  qualityScore: 98,
}

const packDto = {
  id: 'foundation-v1-wave-1',
  curriculumVersion: 'foundation-v1',
  batchNo: 1,
  titleZh: '第一题包',
  descriptionZh: '基础题包。',
  topicCount: 20,
  questionCount: 80,
  domainQuotas: {
    syntax: 2,
    morphology: 10,
  },
  status: 'published',
  qualityScore: 97,
}

const questionDto = {
  id: 'foundation-v1-syn-case-particles-f1',
  packId: 'foundation-v1-wave-1',
  topicId: 'syn_case_particles',
  curriculumVersion: 'foundation-v1',
  stage: 'F1',
  questionType: 'single_choice',
  sourceKind: 'original_sentence',
  stimulus: {
    kind: 'sentence',
    jaText: '先生が学生に本を渡した。',
    zhContext: '老师把书递给了学生。',
  },
  promptZh: '哪个助词标示接受者？',
  options: [
    { id: 'A', text: 'が' },
    { id: 'B', text: 'に' },
    { id: 'C', text: 'を' },
    { id: 'D', text: 'は' },
  ],
  answer: { optionId: 'B' },
  hintZh: '先找接受者。',
  explanationZh: '「に」标示接受者。',
  deepExplanationZh: '目标论元由「に」标示。',
  cautionNoteZh: '「に」也有其他用法。',
  wrongExplanations: {
    A: '主语标记。',
    C: '对象标记。',
    D: '原句中没有。',
  },
  difficulty: 1,
  tags: ['case'],
  sortOrder: 76,
  status: 'published',
  qualityScore: 99,
}

describe('foundation repository path builders', () => {
  it('builds independent topics, packs, and questions URLs', () => {
    expect(buildFoundationTopicsPath({
      curriculumVersion: 'foundation-v1',
      domain: 'syntax',
      moduleId: 'case_system',
      limit: 20,
    })).toBe(
      '/api/linguistics/foundation/topics?curriculumVersion=foundation-v1&limit=20&domain=syntax&moduleId=case_system',
    )
    expect(buildFoundationPacksPath()).toBe('/api/linguistics/foundation/packs')
    expect(buildFoundationQuestionsPath({
      packId: 'foundation-v1-wave-1',
      topicId: 'syn_case_particles',
      stage: 'F1',
      difficulty: 1,
      cursor: 'opaque-cursor',
    })).toBe(
      '/api/linguistics/foundation/questions?cursor=opaque-cursor&packId=foundation-v1-wave-1&topicId=syn_case_particles&stage=F1&difficulty=1',
    )
  })
})

describe('foundation repository response validation', () => {
  it('accepts valid pages for all three independent resources', () => {
    expect(parseFoundationTopicPage({
      items: [topicDto],
      page: { limit: 40, hasMore: false, nextCursor: null },
    }).items[0]?.titleZh).toBe('格助词')
    expect(parseFoundationPackPage({
      items: [packDto],
      page: { limit: 40, hasMore: false, nextCursor: null },
    }).items[0]?.questionCount).toBe(80)
    expect(parseFoundationQuestionPage({
      items: [questionDto],
      page: { limit: 40, hasMore: false, nextCursor: null },
    }).items[0]?.answer.optionId).toBe('B')

    const contrastQuestion = {
      ...questionDto,
      stimulus: {
        kind: 'contrast',
        items: [
          { label: '甲', text: '書かせられなかった。', noteZh: '使役被动—否定—过去' },
          { label: '乙', text: '書かなさせられた。', noteZh: '错误的形态顺序' },
        ],
        zhContext: '保持目标意义不变。',
      },
    }
    expect(parseFoundationQuestionPage({
      items: [contrastQuestion],
      page: { limit: 40, hasMore: false, nextCursor: null },
    }).items[0]?.stimulus).toEqual(contrastQuestion.stimulus)

    const metalinguisticQuestion = {
      ...questionDto,
      stimulus: {
        kind: 'metalinguistic',
        form: 'ほん＋たな → ほんだな',
        descriptionZh: '分析复合词中的连浊。',
      },
    }
    expect(parseFoundationQuestionPage({
      items: [metalinguisticQuestion],
      page: { limit: 40, hasMore: false, nextCursor: null },
    }).items[0]?.stimulus).toEqual(metalinguisticQuestion.stimulus)
  })

  it('rejects malformed page metadata', () => {
    expect(() => parseFoundationTopicPage({
      items: [topicDto],
      page: { limit: 0, hasMore: false, nextCursor: null },
    })).toThrow('metadata')
    expect(() => parseFoundationTopicPage({
      items: [topicDto],
      page: { limit: 40, hasMore: true, nextCursor: null },
    })).toThrow('metadata')
  })

  it('rejects malformed foundation items at the Web boundary', () => {
    expect(() => parseFoundationPackPage({
      items: [{ ...packDto, questionCount: 79 }],
      page: { limit: 40, hasMore: false, nextCursor: null },
    })).toThrow('item')
    expect(() => parseFoundationQuestionPage({
      items: [{ ...questionDto, answer: { optionId: 'Z' } }],
      page: { limit: 40, hasMore: false, nextCursor: null },
    })).toThrow('item')
    expect(() => parseFoundationQuestionPage({
      items: [{ ...questionDto, stimulus: { kind: 'dialogue', turns: [] } }],
      page: { limit: 40, hasMore: false, nextCursor: null },
    })).toThrow('item')
  })
})
