export type Work = {
  id: string
  slug: string
  displayName: string
  episodeCount: number
}

export type Episode = {
  id: string
  workSlug: string
  workDisplayName: string
  episode: number
  totalCues: number
  jaLines: number
  zhLines: number
  usableJaLines: number
  chunkCount: number
  usableAsMainCorpus: boolean
}

export type VocabItem = {
  id: string
  workSlug: string
  surface: string
  reading?: string
  romaji?: string
  meaningZh: string
  pos?: string
  jlptLevel?: string
  suitableHandwriting: boolean
  suitableShadowing: boolean
  animeToneNote?: string
  realWorldNote?: string
  totalOccurrences: number
  episodeCount: number
}

export type GrammarPoint = {
  id: string
  pattern: string
  functionZh: string
  jaExample: string
  explanationZh: string
  pragmaticsNote: string
  realWorldNote: string
  difficulty: string
  sourceLineNo: number
}

export type LearningSentence = {
  id: string
  jaText: string
  romaji?: string
  meaningZh: string
  toneTags: string[]
  difficulty: string
  sourceLineNo: number
}

export type LearningExercise = {
  id: string
  exerciseType: string
  prompt: string
  answer: string
  hint?: string
  difficulty: string
}

export type LinguisticExerciseAnswer = {
  answerZh: string
  correctIndex?: number
}

export type LinguisticExerciseDraft = {
  id: string
  workSlug: string
  episode?: number
  sourceLineNo?: number
  jaText: string
  zhText?: string
  sceneLines?: {
    lineNo?: number
    speaker?: string
    jaText: string
    zhText?: string
    isTarget?: boolean
  }[]
  targetLineNo?: number
  domain: string
  phenomenonKey: string
  questionType: string
  prompt: string
  options: string[]
  answer: LinguisticExerciseAnswer
  basicExplanationZh: string
  deepExplanationZh?: string
  animeContextNoteZh?: string
  cautionNoteZh?: string
  difficulty: string
  qualityScore: number
}

export type SubtitleLine = {
  lineNo: number
  startTime: string
  endTime: string
  jaText: string
  zhText: string
}

export type AiSection = {
  title: string
  body: string
}

export type StructuredAiResult = {
  title: string
  summary: string
  sections: AiSection[]
  text: string
}

export type ProgressItem = {
  deviceId: string
  itemId: string
  itemType: string
  workSlug?: string
  episode?: number
  state: string
  nextReviewOn: string
  lastReviewedAt: string
  payload: Record<string, unknown>
}

export type ReviewTask = ProgressItem & {
  priority: number
  route: string
  label: string
  due: boolean
}

export type TodayReviewResponse = {
  generatedAt: string
  dueDate: string
  tasks: ReviewTask[]
  groups: Record<string, number>
}

export type HistoryEntry = {
  id: string
  title: string
  summary: string
  model?: string
  workSlug?: string
  episode?: number
  updatedAt?: string
  createdAt?: string
}

export type CorrectionHistoryEntry = HistoryEntry & {
  targetType: string
  targetId: string
  promptText: string
}

export type AiHistoryEntry = HistoryEntry & {
  kind: string
  sourceId: string
}

export type ProfileHistoryEntry = HistoryEntry & {
  characterKey: string
}

export type HistoryResponse = {
  generatedAt: string
  corrections: CorrectionHistoryEntry[]
  ai: AiHistoryEntry[]
  profiles: ProfileHistoryEntry[]
}

export type HistoryDetail = {
  type: 'correction' | 'ai' | 'profile'
  id: string
  title: string
  summary: string
  model?: string
  cacheKind?: string
  cacheStatus?: string
  workSlug?: string
  episode?: number
  sourceId?: string
  promptText?: string
  createdAt?: string
  updatedAt?: string
  result: StructuredAiResult | Record<string, unknown>
}

export type EpisodePlan = {
  id: string
  workSlug: string
  episode: number
  vocabCount: number
  handwritingCount: number
  shadowingCount: number
  grammarCount: number
  exerciseCount: number
  vocabItemIds?: string[]
  handwritingVocabIds?: string[]
  notes: string
}
