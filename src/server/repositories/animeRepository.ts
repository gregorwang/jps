import type {
  CursorPage,
  Episode,
  EpisodePlan,
  FoundationDomain,
  FoundationPackListParams,
  FoundationQuestion,
  FoundationQuestionListParams,
  FoundationQuestionPack,
  FoundationStimulus,
  FoundationTopic,
  FoundationTopicListParams,
  HistoryResponse,
  HistoryDetail,
  GrammarPoint,
  LearningExercise,
  LinguisticExerciseDraft,
  LearningSentence,
  StructuredAiResult,
  TodayReviewResponse,
  SubtitleLine,
  VocabItem,
  Work,
} from '../../lib/types'
import {
  foundationDomains,
  foundationQuestionTypes,
  foundationSourceKinds,
  foundationStages,
} from '../../lib/types'

const works: Work[] = [
  { id: 'k-on', slug: 'k-on', displayName: 'K-ON!', episodeCount: 14 },
  { id: 're-zero', slug: 're-zero', displayName: 'Re:ゼロから始める異世界生活', episodeCount: 13 },
]

const episodes: Episode[] = [
  [1, 772, 362, 336, 362, 15],
  [2, 792, 361, 352, 361, 15],
  [3, 764, 351, 334, 351, 14],
  [4, 821, 371, 363, 371, 15],
  [5, 1004, 456, 456, 456, 19],
  [6, 699, 298, 288, 298, 12],
  [7, 855, 380, 377, 380, 15],
  [8, 909, 395, 387, 395, 16],
  [9, 841, 380, 380, 380, 15],
  [10, 888, 397, 395, 397, 16],
  [11, 993, 436, 435, 436, 18],
  [12, 881, 346, 346, 346, 14],
  [13, 698, 313, 313, 313, 13],
  [14, 972, 429, 428, 429, 17],
].map(([episode, totalCues, jaLines, zhLines, usableJaLines, chunkCount]) => ({
  id: `k-on-ep${String(episode).padStart(2, '0')}`,
  workSlug: 'k-on',
  workDisplayName: 'K-ON!',
  episode,
  totalCues,
  jaLines,
  zhLines,
  usableJaLines,
  chunkCount,
  usableAsMainCorpus: true,
}))

episodes.push(
  {
    id: 're-zero-s01e01',
    workSlug: 're-zero',
    workDisplayName: 'Re:ゼロから始める異世界生活',
    episode: 1,
    totalCues: 1034,
    jaLines: 1034,
    zhLines: 862,
    usableJaLines: 989,
    chunkCount: 25,
    usableAsMainCorpus: true,
  },
  {
    id: 're-zero-s01e02',
    workSlug: 're-zero',
    workDisplayName: 'Re:ゼロから始める異世界生活',
    episode: 2,
    totalCues: 484,
    jaLines: 477,
    zhLines: 0,
    usableJaLines: 433,
    chunkCount: 13,
    usableAsMainCorpus: true,
  },
  {
    id: 're-zero-s01e03',
    workSlug: 're-zero',
    workDisplayName: 'Re:ゼロから始める異世界生活',
    episode: 3,
    totalCues: 480,
    jaLines: 480,
    zhLines: 0,
    usableJaLines: 477,
    chunkCount: 12,
    usableAsMainCorpus: true,
  },
  {
    id: 're-zero-s01e04',
    workSlug: 're-zero',
    workDisplayName: 'Re:ゼロから始める異世界生活',
    episode: 4,
    totalCues: 506,
    jaLines: 506,
    zhLines: 0,
    usableJaLines: 506,
    chunkCount: 13,
    usableAsMainCorpus: true,
  },
  {
    id: 're-zero-s01e05',
    workSlug: 're-zero',
    workDisplayName: 'Re:ゼロから始める異世界生活',
    episode: 5,
    totalCues: 474,
    jaLines: 474,
    zhLines: 0,
    usableJaLines: 454,
    chunkCount: 12,
    usableAsMainCorpus: true,
  },
  {
    id: 're-zero-s01e06',
    workSlug: 're-zero',
    workDisplayName: 'Re:ゼロから始める異世界生活',
    episode: 6,
    totalCues: 480,
    jaLines: 480,
    zhLines: 0,
    usableJaLines: 473,
    chunkCount: 12,
    usableAsMainCorpus: true,
  },
)

const vocabItems: VocabItem[] = [
  {
    id: 'k-on-vocab-大丈夫',
    workSlug: 'k-on',
    surface: '大丈夫',
    reading: 'だいじょうぶ',
    romaji: 'daijoubu',
    meaningZh: '没事吧 / 没关系',
    pos: '名詞/表現',
    jlptLevel: 'N5',
    suitableHandwriting: true,
    suitableShadowing: true,
    animeToneNote: '日常校园口语，语气自然。',
    realWorldNote: '现实中可用。',
    totalOccurrences: 131,
    episodeCount: 13,
  },
  {
    id: 'k-on-vocab-って',
    workSlug: 'k-on',
    surface: 'って',
    reading: 'って',
    romaji: 'tte',
    meaningZh: '引用、话题提示、口语转述',
    pos: '助詞/表現',
    jlptLevel: 'N4',
    suitableHandwriting: false,
    suitableShadowing: true,
    animeToneNote: '角色对话里非常高频，常带轻松语气。',
    realWorldNote: '现实口语中常用。',
    totalOccurrences: 96,
    episodeCount: 14,
  },
  {
    id: 'k-on-vocab-軽音部',
    workSlug: 'k-on',
    surface: '軽音部',
    reading: 'けいおんぶ',
    romaji: 'keionbu',
    meaningZh: '轻音部',
    pos: '名詞',
    jlptLevel: 'N5',
    suitableHandwriting: true,
    suitableShadowing: true,
    animeToneNote: '作品核心词。',
    realWorldNote: '校园社团语境可用。',
    totalOccurrences: 44,
    episodeCount: 11,
  },
  {
    id: 'k-on-vocab-頑張',
    workSlug: 'k-on',
    surface: '頑張る',
    reading: 'がんばる',
    romaji: 'ganbaru',
    meaningZh: '努力、加油',
    pos: '動詞',
    jlptLevel: 'N5',
    suitableHandwriting: true,
    suitableShadowing: true,
    animeToneNote: '鼓励、决心场景常见。',
    realWorldNote: '现实中高频可用。',
    totalOccurrences: 35,
    episodeCount: 12,
  },
]

const grammarPoints: GrammarPoint[] = [
  {
    id: 'k-on-ep01-grammar-ないと-7f6892b171',
    pattern: '～ないと',
    functionZh: '必须 / 否则',
    jaExample: 'お姉ちゃん そろそろ起きないと...',
    explanationZh: '这里用「～ないと」表示如果不做就不行，后半常被省略。',
    pragmaticsNote: '语气偏口语，适合家人或熟人提醒。',
    realWorldNote: '日常对话中可用。',
    difficulty: 'N4',
    sourceLineNo: 1,
  },
  {
    id: 'k-on-ep01-grammar-って-50a14458d1',
    pattern: '～って',
    functionZh: '引用 / 话题',
    jaExample: '何うなってるのよ 唯',
    explanationZh: '这里接近把对方状态拿出来当话题追问。',
    pragmaticsNote: '口语感强，比正式表达更贴近日常吐槽。',
    realWorldNote: '现实口语中可用。',
    difficulty: 'N4',
    sourceLineNo: 18,
  },
  {
    id: 'k-on-ep01-grammar-句末-よねかなだろ',
    pattern: '句末语气',
    functionZh: '确认 / 推量 / 共感',
    jaExample: '正確には廃部寸前ね',
    explanationZh: '句末「ね」把判断包装成轻微确认或共享信息。',
    pragmaticsNote: '让说明听起来不生硬，符合轻松社团对话。',
    realWorldNote: '现实中可用。',
    difficulty: 'N5',
    sourceLineNo: 46,
  },
]

const sentences: LearningSentence[] = [
  {
    id: 'k-on-ep01-sent-00025',
    jaText: 'こうやってニートが出来上がっていくのね',
    romaji: 'kou yatte niito ga dekiagatte iku no ne',
    meaningZh: '这样下去就会变成家里蹲了。',
    toneTags: ['语气助词', '日常体'],
    difficulty: 'N4',
    sourceLineNo: 25,
  },
  {
    id: 'k-on-ep01-sent-00046',
    jaText: '正確には廃部寸前ね',
    romaji: 'seikaku ni wa haibu sunzen ne',
    meaningZh: '准确来说是即将废部。',
    toneTags: ['语气助词', '日常体'],
    difficulty: 'N5',
    sourceLineNo: 46,
  },
  {
    id: 'k-on-ep01-sent-00056',
    jaText: 'このプリントをみんなに配っておいてね',
    romaji: 'kono purinto o minna ni kubatte oite ne',
    meaningZh: '去把这些资料发给大家。',
    toneTags: ['请求表达', '日常体'],
    difficulty: 'N4',
    sourceLineNo: 56,
  },
]

const exercises: LearningExercise[] = [
  {
    id: 'k-on-ep01-ex-kana_to_kanji-001',
    exerciseType: 'kana_to_kanji',
    prompt: '假名：だいじょうぶ',
    answer: '大丈夫',
    hint: '没事吧 / 没关系',
    difficulty: 'N5',
  },
  {
    id: 'k-on-ep01-ex-kana_to_kanji-002',
    exerciseType: 'kana_to_kanji',
    prompt: '假名：けいおんぶ',
    answer: '軽音部',
    hint: '轻音部',
    difficulty: 'N5',
  },
  {
    id: 'k-on-ep01-ex-meaning_to_japanese-016',
    exerciseType: 'meaning_to_japanese',
    prompt: '中文：从今天开始',
    answer: '今日から',
    hint: 'から 表示起点',
    difficulty: 'N5',
  },
]

const subtitleLines: SubtitleLine[] = [
  {
    lineNo: 1,
    startTime: '0:00:31.29',
    endTime: '0:00:33.78',
    jaText: 'お姉ちゃん そろそろ起きないと...',
    zhText: '姐姐 该起床了...',
  },
  {
    lineNo: 18,
    startTime: '0:01:16.76',
    endTime: '0:01:18.80',
    jaText: '何うなってるのよ 唯',
    zhText: '唯 你在嘟哝什么呢',
  },
  {
    lineNo: 25,
    startTime: '0:01:37.60',
    endTime: '0:01:40.18',
    jaText: 'こうやってニートが出来上がっていくのね',
    zhText: '这样下去就会变成家里蹲了',
  },
  {
    lineNo: 46,
    startTime: '0:03:12.42',
    endTime: '0:03:14.58',
    jaText: '正確には廃部寸前ね',
    zhText: '准确来说是即将废部',
  },
]

const plan: EpisodePlan = {
  id: 'k-on-ep01-plan',
  workSlug: 'k-on',
  episode: 1,
  vocabCount: 20,
  handwritingCount: 10,
  shadowingCount: 5,
  grammarCount: 5,
  exerciseCount: 20,
  notes: '第1集学习计划：高频词20、手写10、跟读5、语法5',
}

async function apiGet<T>(path: string, fallback: T): Promise<T> {
  if (typeof window === 'undefined') {
    return fallback
  }

  try {
    const response = await fetch(path)
    if (!response.ok) {
      return fallback
    }
    return (await response.json()) as T
  } catch {
    return fallback
  }
}

const emptyFoundationPageLimit = 40
const foundationDomainSet = new Set<string>(foundationDomains)
const foundationStageSet = new Set<string>(foundationStages)
const foundationQuestionTypeSet = new Set<string>(foundationQuestionTypes)
const foundationSourceKindSet = new Set<string>(foundationSourceKinds)

type FoundationCommonListParams = {
  curriculumVersion?: string
  cursor?: string
  limit?: number
}

function addFoundationCommonParams(searchParams: URLSearchParams, params: FoundationCommonListParams) {
  if (params.curriculumVersion) searchParams.set('curriculumVersion', params.curriculumVersion)
  if (params.cursor) searchParams.set('cursor', params.cursor)
  if (params.limit !== undefined) searchParams.set('limit', String(params.limit))
}

export function buildFoundationTopicsPath(params: FoundationTopicListParams = {}) {
  const searchParams = new URLSearchParams()
  addFoundationCommonParams(searchParams, params)
  if (params.domain) searchParams.set('domain', params.domain)
  if (params.moduleId) searchParams.set('moduleId', params.moduleId)
  return withSearchParams('/api/linguistics/foundation/topics', searchParams)
}

export function buildFoundationPacksPath(params: FoundationPackListParams = {}) {
  const searchParams = new URLSearchParams()
  addFoundationCommonParams(searchParams, params)
  return withSearchParams('/api/linguistics/foundation/packs', searchParams)
}

export function buildFoundationQuestionsPath(params: FoundationQuestionListParams = {}) {
  const searchParams = new URLSearchParams()
  addFoundationCommonParams(searchParams, params)
  if (params.packId) searchParams.set('packId', params.packId)
  if (params.topicId) searchParams.set('topicId', params.topicId)
  if (params.stage) searchParams.set('stage', params.stage)
  if (params.questionType) searchParams.set('questionType', params.questionType)
  if (params.difficulty !== undefined) searchParams.set('difficulty', String(params.difficulty))
  return withSearchParams('/api/linguistics/foundation/questions', searchParams)
}

function withSearchParams(path: string, searchParams: URLSearchParams) {
  const query = searchParams.toString()
  return query ? `${path}?${query}` : path
}

async function apiGetFoundationPage<T>(
  path: string,
  parser: (input: unknown) => CursorPage<T>,
  requestedLimit?: number,
): Promise<CursorPage<T>> {
  if (typeof window === 'undefined') {
    return {
      items: [],
      page: {
        limit: requestedLimit ?? emptyFoundationPageLimit,
        hasMore: false,
        nextCursor: null,
      },
    }
  }

  const response = await fetch(path)
  if (!response.ok) {
    throw new Error(`Foundation API failed: ${response.status} ${await response.text()}`)
  }
  const payload: unknown = await response.json()
  return parser(payload)
}

export function parseFoundationTopicPage(input: unknown) {
  return parseFoundationPage(input, isFoundationTopic)
}

export function parseFoundationPackPage(input: unknown) {
  return parseFoundationPage(input, isFoundationPack)
}

export function parseFoundationQuestionPage(input: unknown) {
  return parseFoundationPage(input, isFoundationQuestion)
}

function parseFoundationPage<T>(
  input: unknown,
  itemGuard: (value: unknown) => value is T,
): CursorPage<T> {
  if (!isRecord(input) || !Array.isArray(input.items) || !isRecord(input.page)) {
    throw new Error('Invalid foundation page response')
  }
  if (!input.items.every(itemGuard)) throw new Error('Invalid foundation page item')
  const limit = input.page.limit
  const hasMore = input.page.hasMore
  const nextCursor = input.page.nextCursor
  if (
    typeof limit !== 'number'
    || !Number.isSafeInteger(limit)
    || limit < 1
    || limit > 100
    || typeof hasMore !== 'boolean'
    || (nextCursor !== null && (typeof nextCursor !== 'string' || !nextCursor))
    || (hasMore && typeof nextCursor !== 'string')
    || (!hasMore && nextCursor !== null)
    || input.items.length > limit
  ) {
    throw new Error('Invalid foundation page metadata')
  }
  return {
    items: input.items,
    page: {
      limit,
      hasMore,
      nextCursor,
    },
  }
}

function isFoundationTopic(value: unknown): value is FoundationTopic {
  if (!isRecord(value)) return false
  return isNonEmptyString(value.id)
    && isNonEmptyString(value.curriculumVersion)
    && isFoundationDomain(value.domain)
    && isNonEmptyString(value.moduleId)
    && isNonNegativeInteger(value.sortOrder)
    && isNonEmptyString(value.titleJa)
    && isNonEmptyString(value.titleZh)
    && isNonEmptyString(value.shortDefinitionZh)
    && isNonEmptyString(value.beginnerExplanationZh)
    && isNonEmptyString(value.deepExplanationZh)
    && isNonEmptyString(value.cautionNoteZh)
    && isStringArray(value.prerequisiteTopicIds)
    && isFoundationObjectives(value.learningObjectives)
    && isFoundationExampleSpec(value.exampleSpec)
    && isStringArray(value.tags)
    && value.status === 'published'
    && isQualityScore(value.qualityScore)
}

function isFoundationPack(value: unknown): value is FoundationQuestionPack {
  if (!isRecord(value)) return false
  return isNonEmptyString(value.id)
    && isNonEmptyString(value.curriculumVersion)
    && isPositiveInteger(value.batchNo)
    && isNonEmptyString(value.titleZh)
    && isNonEmptyString(value.descriptionZh)
    && isPositiveInteger(value.topicCount)
    && isPositiveInteger(value.questionCount)
    && value.questionCount === value.topicCount * 4
    && isFoundationDomainQuotas(value.domainQuotas)
    && value.status === 'published'
    && isQualityScore(value.qualityScore)
}

function isFoundationQuestion(value: unknown): value is FoundationQuestion {
  if (!isRecord(value)) return false
  const options = value.options
  const answer = value.answer
  if (
    !isFoundationOptions(options)
    || !isRecord(answer)
    || !isNonEmptyString(answer.optionId)
    || !options.some((option) => option.id === answer.optionId)
  ) {
    return false
  }
  if (
    !isNonEmptyString(value.id)
    || !isNonEmptyString(value.packId)
    || !isNonEmptyString(value.topicId)
    || !isNonEmptyString(value.curriculumVersion)
    || !isFoundationStage(value.stage)
    || !isFoundationQuestionType(value.questionType)
    || !isFoundationSourceKind(value.sourceKind)
    || !isFoundationStimulus(value.stimulus)
    || !isNonEmptyString(value.promptZh)
    || !isNonEmptyString(value.hintZh)
    || !isNonEmptyString(value.explanationZh)
    || !isNonEmptyString(value.deepExplanationZh)
    || !isNonEmptyString(value.cautionNoteZh)
    || !isWrongExplanations(value.wrongExplanations, options, answer.optionId)
    || !isFoundationDifficulty(value.difficulty)
    || foundationStages.indexOf(value.stage) + 1 !== value.difficulty
    || !isStringArray(value.tags)
    || !isNonNegativeInteger(value.sortOrder)
    || value.status !== 'published'
    || !isQualityScore(value.qualityScore)
  ) {
    return false
  }
  const hasTransferExample = value.transferExampleJa !== undefined
  const hasTransferExplanation = value.transferExplanationZh !== undefined
  return hasTransferExample === hasTransferExplanation
    && (!hasTransferExample || (
      isNonEmptyString(value.transferExampleJa)
      && isNonEmptyString(value.transferExplanationZh)
    ))
}

function isFoundationObjectives(value: unknown) {
  return isRecord(value)
    && isNonEmptyString(value.F1Zh)
    && isNonEmptyString(value.F2Zh)
    && isNonEmptyString(value.F3Zh)
    && isNonEmptyString(value.F4Zh)
}

function isFoundationExampleSpec(value: unknown) {
  return isRecord(value)
    && isNonEmptyString(value.formZh)
    && isNonEmptyString(value.contrastZh)
    && isNonEmptyString(value.constraintsZh)
}

function isFoundationDomainQuotas(value: unknown): value is Partial<Record<FoundationDomain, number>> {
  if (!isRecord(value)) return false
  return Object.entries(value).every(([key, count]) => isFoundationDomain(key) && isNonNegativeInteger(count))
}

function isFoundationStimulus(value: unknown): value is FoundationStimulus {
  if (!isRecord(value)) return false
  if (value.kind === 'sentence') {
    return isNonEmptyString(value.jaText)
      && (value.zhContext === undefined || isNonEmptyString(value.zhContext))
  }
  if (value.kind === 'dialogue') {
    return Array.isArray(value.turns)
      && value.turns.length > 0
      && value.turns.every((turn) => isRecord(turn)
        && isNonEmptyString(turn.jaText)
        && (turn.speaker === undefined || isNonEmptyString(turn.speaker))
        && (turn.zhText === undefined || isNonEmptyString(turn.zhText)))
      && (value.zhContext === undefined || isNonEmptyString(value.zhContext))
  }
  if (value.kind === 'contrast') {
    return Array.isArray(value.items)
      && value.items.length >= 2
      && value.items.every((item) => isRecord(item)
        && isNonEmptyString(item.text)
        && (item.label === undefined || isNonEmptyString(item.label))
        && (item.noteZh === undefined || isNonEmptyString(item.noteZh)))
      && (value.zhContext === undefined || isNonEmptyString(value.zhContext))
  }
  return value.kind === 'metalinguistic'
    && isNonEmptyString(value.descriptionZh)
    && (value.form === undefined || isNonEmptyString(value.form))
}

function isFoundationOptions(value: unknown): value is FoundationQuestion['options'] {
  if (!Array.isArray(value) || value.length !== 4) return false
  if (!value.every((option) => isRecord(option) && isNonEmptyString(option.id) && isNonEmptyString(option.text))) {
    return false
  }
  return new Set(value.map((option) => option.id)).size === value.length
}

function isWrongExplanations(
  value: unknown,
  options: FoundationQuestion['options'],
  correctOptionId: string,
) {
  if (!isRecord(value)) return false
  const expectedIds = options.filter((option) => option.id !== correctOptionId).map((option) => option.id)
  const actualIds = Object.keys(value)
  return actualIds.length === expectedIds.length
    && expectedIds.every((optionId) => isNonEmptyString(value[optionId]))
    && !actualIds.includes(correctOptionId)
}

function isFoundationDomain(value: unknown): value is FoundationDomain {
  return typeof value === 'string' && foundationDomainSet.has(value)
}

function isFoundationStage(value: unknown): value is FoundationQuestion['stage'] {
  return typeof value === 'string' && foundationStageSet.has(value)
}

function isFoundationQuestionType(value: unknown): value is FoundationQuestion['questionType'] {
  return typeof value === 'string' && foundationQuestionTypeSet.has(value)
}

function isFoundationSourceKind(value: unknown): value is FoundationQuestion['sourceKind'] {
  return typeof value === 'string' && foundationSourceKindSet.has(value)
}

function isFoundationDifficulty(value: unknown): value is FoundationQuestion['difficulty'] {
  return value === 1 || value === 2 || value === 3 || value === 4
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && Boolean(value.trim())
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every(isNonEmptyString)
}

function isNonNegativeInteger(value: unknown): value is number {
  return typeof value === 'number' && Number.isSafeInteger(value) && value >= 0
}

function isPositiveInteger(value: unknown): value is number {
  return isNonNegativeInteger(value) && value > 0
}

function isQualityScore(value: unknown): value is number {
  return isNonNegativeInteger(value) && value <= 100
}

export const animeRepository = {
  async listWorks() {
    return apiGet('/api/works', works)
  },
  async listEpisodes(workSlug: string) {
    const fallback = episodes.filter((episode) => episode.workSlug === workSlug)
    return apiGet(`/api/works/${workSlug}/episodes`, fallback)
  },
  async getEpisode(workSlug: string, episodeNo: number) {
    const fallback = episodes.find(
      (episode) => episode.workSlug === workSlug && episode.episode === episodeNo,
    )
    return apiGet(`/api/works/${workSlug}/episodes/${episodeNo}`, fallback)
  },
  async getEpisodePlan(workSlug: string, episodeNo: number) {
    const fallback = workSlug === 'k-on' && episodeNo === 1 ? plan : undefined
    return apiGet(`/api/works/${workSlug}/episodes/${episodeNo}/plan`, fallback)
  },
  async listEpisodeVocab(workSlug: string, _episodeNo: number) {
    const fallback = vocabItems.filter((item) => item.workSlug === workSlug)
    return apiGet(`/api/works/${workSlug}/episodes/${_episodeNo}/vocab`, fallback)
  },
  async listEpisodeHandwritingVocab(workSlug: string, episodeNo: number) {
    const fallback = vocabItems.filter((item) => item.workSlug === workSlug && item.suitableHandwriting)
    return apiGet(`/api/works/${workSlug}/episodes/${episodeNo}/vocab?mode=handwriting`, fallback)
  },
  async listEpisodeGrammar(workSlug: string, episodeNo: number) {
    const fallback = workSlug === 'k-on' && episodeNo === 1 ? grammarPoints : []
    return apiGet(`/api/works/${workSlug}/episodes/${episodeNo}/grammar`, fallback)
  },
  async listEpisodeSentences(workSlug: string, episodeNo: number) {
    const fallback = workSlug === 'k-on' && episodeNo === 1 ? sentences : []
    return apiGet(`/api/works/${workSlug}/episodes/${episodeNo}/sentences`, fallback)
  },
  async listEpisodeExercises(workSlug: string, episodeNo: number) {
    const fallback = workSlug === 'k-on' && episodeNo === 1 ? exercises : []
    return apiGet(`/api/works/${workSlug}/episodes/${episodeNo}/exercises`, fallback)
  },
  async listLinguisticExercises() {
    return apiGet('/api/linguistic-exercises', [] as LinguisticExerciseDraft[])
  },
  async listEpisodeLinguisticExercises(workSlug: string, episodeNo: number) {
    const params = new URLSearchParams({
      workSlug,
      episode: String(episodeNo),
      status: 'all',
    })
    return apiGet(`/api/linguistic-exercises?${params.toString()}`, [] as LinguisticExerciseDraft[])
  },
  async listFoundationTopics(params: FoundationTopicListParams = {}) {
    return apiGetFoundationPage(
      buildFoundationTopicsPath(params),
      parseFoundationTopicPage,
      params.limit,
    )
  },
  async listFoundationPacks(params: FoundationPackListParams = {}) {
    return apiGetFoundationPage(
      buildFoundationPacksPath(params),
      parseFoundationPackPage,
      params.limit,
    )
  },
  async listFoundationQuestions(params: FoundationQuestionListParams = {}) {
    return apiGetFoundationPage(
      buildFoundationQuestionsPath(params),
      parseFoundationQuestionPage,
      params.limit,
    )
  },
  async listSubtitleLines(workSlug: string, episodeNo: number) {
    const fallback = workSlug === 'k-on' && episodeNo === 1 ? subtitleLines : []
    return apiGet(`/api/works/${workSlug}/episodes/${episodeNo}/subtitles`, fallback)
  },
  async getSubtitleLine(workSlug: string, episodeNo: number, lineNo: number) {
    const lines = await this.listSubtitleLines(workSlug, episodeNo)
    return lines.find((line) => line.lineNo === lineNo)
  },
  async getTodayReviewTasks(deviceId: string) {
    return apiGet(`/api/review/today?deviceId=${encodeURIComponent(deviceId)}`, {
      generatedAt: '',
      dueDate: '',
      tasks: [],
      groups: {},
    } as TodayReviewResponse)
  },
  async getHistory(deviceId: string) {
    return apiGet(`/api/history?deviceId=${encodeURIComponent(deviceId)}`, {
      generatedAt: '',
      corrections: [],
      ai: [],
      profiles: [],
    } as HistoryResponse)
  },
  async getHistoryDetail(type: string, id: string, deviceId: string) {
    return apiGet(
      `/api/history/detail?type=${encodeURIComponent(type)}&id=${encodeURIComponent(id)}&deviceId=${encodeURIComponent(deviceId)}`,
      null as HistoryDetail | null,
    )
  },
  async deepDiveSentence(input: {
    workSlug: string
    episode: number
    lineNo: number
    jaText: string
    zhText?: string
    deviceId?: string
    model: string
    reasoningEffort?: string
  }) {
    const response = await fetch('/api/ai/sentence-deep-dive', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
    if (!response.ok) throw new Error(await response.text())
    return (await response.json()) as StructuredAiResult
  },
  async getCharacterProfile(input: {
    workSlug: string
    characterKey: string
    characterName: string
    model: string
    reasoningEffort?: string
    regenerate?: boolean
  }) {
    const response = await fetch('/api/ai/character-profile', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
    if (!response.ok) throw new Error(await readApiError(response))
    return (await response.json()) as StructuredAiResult
  },
  async correctSentence(input: {
    deviceId: string
    targetType: string
    targetId?: string
    targetLabel?: string
    sentence: string
    workSlug?: string
    episode?: number
    model: string
    reasoningEffort?: string
  }) {
    const response = await fetch('/api/ai/correct-sentence', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
    if (!response.ok) throw new Error(await response.text())
    return (await response.json()) as StructuredAiResult
  },
}

async function readApiError(response: Response) {
  const text = await response.text()
  try {
    const data = JSON.parse(text) as { error?: { message?: string } }
    return data.error?.message ?? text
  } catch {
    return text
  }
}
