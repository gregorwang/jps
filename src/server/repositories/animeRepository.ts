import type {
  Episode,
  EpisodePlan,
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
