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
  linguisticPayload?: LinguisticPayload
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
  linguisticPayload?: LinguisticPayload
}

export type LearningSentence = {
  id: string
  jaText: string
  romaji?: string
  meaningZh: string
  toneTags: string[]
  difficulty: string
  sourceLineNo: number
  audioUrl?: string
  storagePath?: string
  linguisticPayload?: LinguisticPayload
}

export type LinguisticDomain = {
  titleZh?: string
  explanationZh?: string
  takeawayZh?: string
}

export type LinguisticTerm = {
  termZh?: string
  plainZh?: string
}

export type LinguisticPayload = {
  headlineZh?: string
  domains?: LinguisticDomain[]
  terms?: LinguisticTerm[]
  historicalNoteZh?: string
  cautionZh?: string
}

export type RubySegment = {
  text: string
  reading?: string
}

export type FuriganaResult = {
  ruby_segments: RubySegment[]
  cachedAt?: string
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
  correctKey?: string
  rationaleZh?: string
}

export type LinguisticExerciseOption = {
  key: string
  label: string
}

export type LinguisticExerciseDraft = {
  id: string
  batchId?: string
  workSlug: string
  episode?: number
  sourceId?: string
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
  optionItems?: LinguisticExerciseOption[]
  answer: LinguisticExerciseAnswer
  hint?: string
  basicExplanationZh: string
  deepExplanationZh?: string
  animeContextNoteZh?: string
  cautionNoteZh?: string
  difficulty: string
  qualityScore: number
  status?: string
  phenomenonNameZh?: string
  phenomenonNameJa?: string
  phenomenonDefinitionZh?: string
}

declare const foundationTopicIdBrand: unique symbol
declare const foundationPackIdBrand: unique symbol
declare const foundationQuestionIdBrand: unique symbol

export type FoundationTopicId = string & { readonly [foundationTopicIdBrand]: 'FoundationTopicId' }
export type FoundationPackId = string & { readonly [foundationPackIdBrand]: 'FoundationPackId' }
export type FoundationQuestionId = string & { readonly [foundationQuestionIdBrand]: 'FoundationQuestionId' }

export const foundationDomains = [
  'phonology_writing',
  'morphology',
  'syntax',
  'semantics',
  'pragmatics_discourse',
  'sociolinguistics',
  'historical_grammaticalization',
] as const

export type FoundationDomain = (typeof foundationDomains)[number]

export const foundationStages = ['F1', 'F2', 'F3', 'F4'] as const
export type FoundationStage = (typeof foundationStages)[number]

export const foundationQuestionTypes = [
  'single_choice',
  'morphology_analysis',
  'syntax_relation',
  'contrast_choice',
  'kuuki_yomi',
] as const

export type FoundationQuestionType = (typeof foundationQuestionTypes)[number]

export const foundationSourceKinds = [
  'original_sentence',
  'minimal_pair',
  'constructed_dialogue',
  'metalinguistic',
] as const

export type FoundationSourceKind = (typeof foundationSourceKinds)[number]

export type FoundationLearningObjectives = {
  F1Zh: string
  F2Zh: string
  F3Zh: string
  F4Zh: string
}

export type FoundationExampleSpec = {
  formZh: string
  contrastZh: string
  constraintsZh: string
}

export type FoundationTopic = {
  id: FoundationTopicId
  curriculumVersion: string
  domain: FoundationDomain
  moduleId: string
  sortOrder: number
  titleJa: string
  titleZh: string
  shortDefinitionZh: string
  beginnerExplanationZh: string
  deepExplanationZh: string
  cautionNoteZh: string
  prerequisiteTopicIds: FoundationTopicId[]
  learningObjectives: FoundationLearningObjectives
  exampleSpec: FoundationExampleSpec
  tags: string[]
  status: 'published'
  qualityScore: number
}

export type FoundationQuestionPack = {
  id: FoundationPackId
  curriculumVersion: string
  batchNo: number
  titleZh: string
  descriptionZh: string
  topicCount: number
  questionCount: number
  domainQuotas: Partial<Record<FoundationDomain, number>>
  status: 'published'
  qualityScore: number
}

export type FoundationStimulusTurn = {
  speaker?: string
  jaText: string
  zhText?: string
}

export type FoundationStimulusItem = {
  label?: string
  text: string
  noteZh?: string
}

export type FoundationStimulus =
  | { kind: 'sentence'; jaText: string; zhContext?: string }
  | { kind: 'dialogue'; turns: FoundationStimulusTurn[]; zhContext?: string }
  | { kind: 'contrast'; items: FoundationStimulusItem[]; zhContext?: string }
  | { kind: 'metalinguistic'; form?: string; descriptionZh: string }

export type FoundationQuestionOption = {
  id: string
  text: string
}

export type FoundationQuestion = {
  id: FoundationQuestionId
  packId: FoundationPackId
  topicId: FoundationTopicId
  curriculumVersion: string
  stage: FoundationStage
  questionType: FoundationQuestionType
  sourceKind: FoundationSourceKind
  stimulus: FoundationStimulus
  promptZh: string
  options: FoundationQuestionOption[]
  answer: {
    optionId: string
  }
  hintZh: string
  explanationZh: string
  deepExplanationZh: string
  cautionNoteZh: string
  wrongExplanations: Record<string, string>
  transferExampleJa?: string
  transferExplanationZh?: string
  difficulty: 1 | 2 | 3 | 4
  tags: string[]
  sortOrder: number
  status: 'published'
  qualityScore: number
  contentVersion: number
  contentHash: string
}

export type FoundationQuestionTrack =
  | { track: 'foundation'; question: FoundationQuestion }
  | { track: 'corpus'; exercise: LinguisticExerciseDraft }

export type CursorPage<T> = {
  items: T[]
  page: {
    limit: number
    hasMore: boolean
    nextCursor: string | null
  }
}

export type FoundationTopicListParams = {
  curriculumVersion?: string
  domain?: FoundationDomain
  moduleId?: string
  cursor?: string
  limit?: number
}

export type FoundationPackListParams = {
  curriculumVersion?: string
  cursor?: string
  limit?: number
}

export type FoundationQuestionListParams = {
  curriculumVersion?: string
  packId?: FoundationPackId | string
  topicId?: FoundationTopicId | string
  stage?: FoundationStage
  questionType?: FoundationQuestionType
  difficulty?: 1 | 2 | 3 | 4
  cursor?: string
  limit?: number
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
