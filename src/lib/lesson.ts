import { buildReZeroShadowingAudio } from './rezeroShadowingAudio'
import type { GrammarPoint, LearningSentence, LinguisticPayload, ProgressItem, LearningExercise, VocabItem } from './types'

export type LessonSourceKind = 'vocab' | 'grammar' | 'sentence'
export type LessonMode = 'mixed' | 'vocab' | 'grammar' | 'shadowing' | 'review' | 'target'
export type LessonTarget =
  | { kind: 'vocab'; id: string }
  | { kind: 'grammar'; id: string }
  | { kind: 'sentence'; id: string }

export type PromptAudio =
  | { kind: 'source'; url: string; autoPlay: boolean; reliability: 'verified' | 'flagged'; label?: string; fallbackTts?: { text: string; label?: string } }
  | { kind: 'tts'; text: string; autoPlay: boolean; label?: string }
  | { kind: 'none' }

export type LessonNodeBase = {
  id: string
  source: {
    kind: LessonSourceKind
    sourceId: string
    workSlug: string
    episode: number
    lineNo?: number
  }
  title: string
  prompt: string
  audio: PromptAudio
  explanation: string
  reviewLabel: string
  quality?: {
    score: number
    flags: string[]
  }
}

export type PairMatchLessonNode = LessonNodeBase & {
  type: 'pair-match'
  pairs: {
    id: string
    left: string
    right: string
    audioText?: string
  }[]
}

export type SingleChoiceLessonNode = LessonNodeBase & {
  type: 'single-choice'
  body?: string
  choices: string[]
  answer: string
}

export type ClozeChoiceLessonNode = LessonNodeBase & {
  type: 'cloze-choice'
  before: string
  after: string
  choices: {
    value: string
    note?: string
  }[]
  answer: string
}

export type TileLessonNode = LessonNodeBase & {
  type: 'audio-tiles' | 'translation-tiles'
  displayText?: string
  targetTiles: string[]
  bankTiles: string[]
  targetText: string
}

export type StudyLessonNode = LessonNodeBase & {
  type: 'study-card'
  studyKind: LessonSourceKind
  jaText: string
  reading?: string
  meaningZh: string
  notes: string[]
  linguisticPayload?: LinguisticPayload
}

export type LessonNode =
  | PairMatchLessonNode
  | SingleChoiceLessonNode
  | ClozeChoiceLessonNode
  | TileLessonNode
  | StudyLessonNode

export type LessonPlan = {
  id: string
  workSlug: string
  episode: number
  mode: LessonMode
  batch: number
  hasNextBatch: boolean
  title: string
  nodes: LessonNode[]
  counts: Record<LessonNode['type'], number>
}

export function buildEpisodeLesson(input: {
  workSlug: string
  episode: number
  vocab: VocabItem[]
  grammar: GrammarPoint[]
  sentences: LearningSentence[]
  mode?: LessonMode
  batch?: number
  target?: LessonTarget
  progressItems?: Record<string, ProgressItem>
}): LessonPlan {
  const { workSlug, episode, target } = input
  const mode = input.target ? 'target' : input.mode ?? 'mixed'
  const batch = Math.max(1, input.batch ?? 1)
  const vocab = scopeVocab(input.vocab, target)
  const grammar = scopeGrammar(input.grammar, target)
  const sentences = scopeSentences(input.sentences, target)
  const pools = {
    vocabPair: buildVocabPairNodes(workSlug, episode, vocab),
    vocabChoice: buildVocabChoiceNodes(workSlug, episode, vocab),
    sentenceAudio: buildSentenceAudioTileNodes(workSlug, episode, sentences),
    sentenceTranslation: buildSentenceTranslationNodes(workSlug, episode, sentences),
    grammarCloze: buildGrammarClozeNodes(workSlug, episode, grammar),
    grammarChoice: buildGrammarChoiceNodes(workSlug, episode, grammar),
    vocabStudy: buildVocabStudyNodes(workSlug, episode, vocab),
    grammarStudy: buildGrammarStudyNodes(workSlug, episode, grammar),
    sentenceStudy: buildSentenceStudyNodes(workSlug, episode, sentences),
  }
  const nodes: LessonNode[] = [
    ...pools.vocabPair,
    ...pools.vocabChoice,
    ...pools.sentenceAudio,
    ...pools.sentenceTranslation,
    ...pools.grammarCloze,
    ...pools.grammarChoice,
  ]

  const balanced = buildModeNodes(pools, nodes, mode, input.progressItems ?? {}, target, batch)
  return {
    id: lessonId(workSlug, episode, mode, target, batch),
    workSlug,
    episode,
    mode,
    batch,
    hasNextBatch: hasNextLessonBatch(pools, mode, target, batch),
    title: lessonTitle(workSlug, episode, mode, target, batch),
    nodes: balanced,
    counts: countNodeTypes(balanced),
  }
}

type LessonPools = {
  vocabPair: LessonNode[]
  vocabChoice: SingleChoiceLessonNode[]
  sentenceAudio: TileLessonNode[]
  sentenceTranslation: TileLessonNode[]
  grammarCloze: ClozeChoiceLessonNode[]
  grammarChoice: SingleChoiceLessonNode[]
  vocabStudy: StudyLessonNode[]
  grammarStudy: StudyLessonNode[]
  sentenceStudy: StudyLessonNode[]
}

function buildStudyPracticeSequence(studyNodes: StudyLessonNode[], practiceNodes: LessonNode[], batch: number, batchSize: number) {
  const start = (batch - 1) * batchSize
  const scopedStudy = studyNodes.slice(start, start + batchSize)
  const output: LessonNode[] = []
  for (const study of scopedStudy) {
    output.push(study)
    output.push(...practiceNodes.filter((node) => node.source.sourceId === study.source.sourceId).slice(0, 2))
  }
  return output
}

function hasNextLessonBatch(pools: LessonPools, mode: LessonMode, target: LessonTarget | undefined, batch: number) {
  if (mode === 'target' || target || mode === 'mixed' || mode === 'review') return false
  if (mode === 'vocab') return false
  if (mode === 'grammar') return batch * 6 < pools.grammarStudy.length
  if (mode === 'shadowing') return batch * 6 < pools.sentenceStudy.length
  return false
}

function buildMixedStudyPracticeSequence(pools: LessonPools, batch: number) {
  return [
    ...buildStudyPracticeSequence(pools.vocabStudy, pools.vocabChoice, batch, 2),
    ...buildStudyPracticeSequence(pools.grammarStudy, [...pools.grammarCloze, ...pools.grammarChoice], batch, 1),
    ...buildStudyPracticeSequence(pools.sentenceStudy, prioritizeAudioNodes([...pools.sentenceAudio, ...pools.sentenceTranslation]), batch, 1),
    ...pools.vocabPair.slice(0, 1),
  ].slice(0, 10)
}

function buildTargetStudyPracticeSequence(pools: LessonPools, target?: LessonTarget) {
  if (target?.kind === 'vocab') return buildStudyPracticeSequence(pools.vocabStudy, [...pools.vocabChoice, ...pools.vocabPair], 1, 1)
  if (target?.kind === 'grammar') return buildStudyPracticeSequence(pools.grammarStudy, [...pools.grammarCloze, ...pools.grammarChoice], 1, 1)
  if (target?.kind === 'sentence') return buildStudyPracticeSequence(pools.sentenceStudy, prioritizeAudioNodes([...pools.sentenceAudio, ...pools.sentenceTranslation]), 1, 1)
  return []
}

export function buildPracticeLesson(input: {
  workSlug: string
  episode: number
  exercises: LearningExercise[]
}): LessonPlan {
  const nodes: LessonNode[] = input.exercises
    .filter((exercise) => exercise.prompt && exercise.answer)
    .slice(0, 10)
    .map((exercise, index) => ({
      type: 'single-choice',
      id: `${exercise.id}-practice-choice`,
      source: { kind: 'vocab', sourceId: exercise.id, workSlug: input.workSlug, episode: input.episode },
      title: '普通练习',
      prompt: exercise.prompt,
      body: exercise.hint,
      audio: { kind: 'tts', text: exercise.answer, autoPlay: false },
      explanation: exercise.hint ? `${exercise.answer}。${exercise.hint}` : exercise.answer,
      reviewLabel: exercise.answer,
      choices: buildDistractors(input.exercises.map((candidate) => candidate.answer), exercise.answer, index),
      answer: exercise.answer,
    }))
  return {
    id: lessonId(input.workSlug, input.episode, 'mixed', { kind: 'vocab', id: 'ordinary-exercises' }),
    workSlug: input.workSlug,
    episode: input.episode,
    mode: 'mixed',
    batch: 1,
    hasNextBatch: false,
    title: `${input.workSlug} EP${String(input.episode).padStart(2, '0')} 普通练习`,
    nodes,
    counts: countNodeTypes(nodes),
  }
}

function buildVocabStudyNodes(workSlug: string, episode: number, vocab: VocabItem[]): StudyLessonNode[] {
  return vocab
    .filter((item) => item.surface && item.meaningZh)
    .map((item) => ({
      type: 'study-card',
      id: `${item.id}-study`,
      source: { kind: 'vocab', sourceId: item.id, workSlug, episode },
      title: '先学这个词',
      prompt: item.surface,
      audio: { kind: 'tts', text: item.surface, autoPlay: false, label: '播放读音' },
      explanation: `${item.surface} = ${item.meaningZh}`,
      reviewLabel: item.surface,
      studyKind: 'vocab',
      jaText: item.surface,
      reading: item.reading,
      meaningZh: item.meaningZh,
      linguisticPayload: item.linguisticPayload,
      notes: [
        item.pos ? `词性：${item.pos}` : '',
        item.jlptLevel ? `难度：${item.jlptLevel}` : '',
        item.animeToneNote ?? '',
        item.realWorldNote ?? '',
      ].filter(isUsefulStudyNote),
    }))
}

function buildGrammarStudyNodes(workSlug: string, episode: number, grammar: GrammarPoint[]): StudyLessonNode[] {
  return grammar
    .filter((point) => point.pattern && point.functionZh)
    .map((point) => ({
      type: 'study-card',
      id: `${point.id}-study`,
      source: { kind: 'grammar', sourceId: point.id, workSlug, episode, lineNo: point.sourceLineNo },
      title: '先学这个语法',
      prompt: point.pattern,
      audio: { kind: 'tts', text: point.jaExample || point.pattern, autoPlay: false, label: '播放例句' },
      explanation: `${point.pattern}：${point.functionZh}`,
      reviewLabel: point.pattern,
      studyKind: 'grammar',
      jaText: point.jaExample || point.pattern,
      meaningZh: point.functionZh,
      linguisticPayload: point.linguisticPayload,
      notes: [
        point.explanationZh,
        point.pragmaticsNote,
        point.realWorldNote,
        point.difficulty ? `难度：${point.difficulty}` : '',
      ].filter(isUsefulStudyNote),
    }))
}

function buildSentenceStudyNodes(workSlug: string, episode: number, sentences: LearningSentence[]): StudyLessonNode[] {
  return sentences
    .filter((sentence) => sentence.jaText && isUsableChineseMeaning(sentence.meaningZh))
    .map((sentence) => {
      const audio = sentenceAudio(workSlug, sentence, false)
      return {
        type: 'study-card',
        id: `${sentence.id}-study`,
        source: { kind: 'sentence', sourceId: sentence.id, workSlug, episode, lineNo: sentence.sourceLineNo },
        title: '先听懂这句',
        prompt: sentence.jaText,
        audio,
        explanation: `${sentence.jaText} / ${sentence.meaningZh}`,
        reviewLabel: sentence.jaText,
        quality: lessonQuality([audioQualityFlag(audio)]),
        studyKind: 'sentence',
        jaText: sentence.jaText,
        meaningZh: sentence.meaningZh,
        linguisticPayload: sentence.linguisticPayload,
        notes: [],
      } satisfies StudyLessonNode
    })
}

function buildVocabPairNodes(workSlug: string, episode: number, vocab: VocabItem[]): PairMatchLessonNode[] {
  const usable = vocab.filter((item) => item.surface && item.meaningZh).slice(0, 8)
  if (usable.length < 4) return []
  return [{
    type: 'pair-match',
    id: `${workSlug}-${episode}-vocab-pair-core`,
    source: { kind: 'vocab', sourceId: usable.map((item) => item.id).join(','), workSlug, episode },
    title: '选择配对',
    prompt: '把中文意思和日文表达配起来',
    audio: { kind: 'none' },
    explanation: '配对时点击日文会播放辅助读音。失败会撤销本次选择，答对的组合会保留。',
    reviewLabel: '词义配对',
    pairs: usable.slice(0, 5).map((item) => ({
      id: item.id,
      left: item.meaningZh,
      right: item.surface,
      audioText: item.surface,
    })),
  }]
}

function buildVocabChoiceNodes(workSlug: string, episode: number, vocab: VocabItem[]): SingleChoiceLessonNode[] {
  return vocab
    .filter((item) => item.surface && item.meaningZh)
    .map((item, index) => {
      const choices = buildVocabDistractors(vocab, item, index)
      return {
        type: 'single-choice',
        id: `${item.id}-meaning-to-ja`,
        source: { kind: 'vocab', sourceId: item.id, workSlug, episode },
        title: '选择正确的日文',
        prompt: `「${item.meaningZh}」对应哪个日文？`,
        body: item.reading ? `读音：${item.reading}` : undefined,
        audio: { kind: 'tts', text: item.surface, autoPlay: false, label: '听答案' },
        explanation: `${item.surface} = ${item.meaningZh}。${item.realWorldNote ?? ''}`,
        reviewLabel: item.surface,
        choices,
        answer: item.surface,
      } satisfies SingleChoiceLessonNode
    })
}

function isUsefulStudyNote(note: string | undefined): note is string {
  if (!note) return false
  return !isEditorialBatchNote(note)
}

function isEditorialBatchNote(note: string) {
  return /EP\d+\s*筛选/u.test(note) ||
    /学习价值优先于机械词频/u.test(note) ||
    /放回\s*EP\d+\s*原句跟读/u.test(note)
}

function buildSentenceAudioTileNodes(workSlug: string, episode: number, sentences: LearningSentence[]): TileLessonNode[] {
  return sentences
    .filter((sentence) => {
      const tiles = splitJapaneseTiles(sentence.jaText)
      return sentence.jaText && tiles.length >= 2 && !hasBadTileFragments(tiles)
    })
    .slice(0, 6)
    .map((sentence) => {
      const targetTiles = splitJapaneseTiles(sentence.jaText)
      const audio = sentenceAudio(workSlug, sentence, true)
      return {
        type: 'audio-tiles',
        id: `${sentence.id}-audio-tiles`,
        source: { kind: 'sentence', sourceId: sentence.id, workSlug, episode, lineNo: sentence.sourceLineNo },
        title: '选择听到的内容',
        prompt: '听句子，把下面的语块按顺序拼起来',
        audio,
        explanation: isUsableChineseMeaning(sentence.meaningZh) ? `意思：${sentence.meaningZh}` : sentence.jaText,
        reviewLabel: sentence.jaText,
        quality: lessonQuality([audioQualityFlag(audio), ...tileQualityFlags(targetTiles)]),
        targetTiles,
        bankTiles: shuffleStable([...targetTiles, ...sentenceDistractorTiles(sentences, sentence.id)], sentence.id).slice(0, Math.max(6, targetTiles.length)),
        targetText: targetTiles.join(''),
      } satisfies TileLessonNode
    })
}

function buildSentenceTranslationNodes(workSlug: string, episode: number, sentences: LearningSentence[]): TileLessonNode[] {
  return sentences
    .filter((sentence) => {
      const tiles = splitChineseTiles(sentence.meaningZh)
      return sentence.jaText && isUsableChineseMeaning(sentence.meaningZh) && tiles.length >= 2 && !hasBadTileFragments(tiles)
    })
    .slice(0, 6)
    .map((sentence) => {
      const targetTiles = splitChineseTiles(sentence.meaningZh)
      const audio = sentenceAudio(workSlug, sentence, false)
      return {
        type: 'translation-tiles',
        id: `${sentence.id}-translation-tiles`,
        source: { kind: 'sentence', sourceId: sentence.id, workSlug, episode, lineNo: sentence.sourceLineNo },
        title: '用中文拼出这句话',
        prompt: '理解日文句子，再拼出自然中文意思',
        displayText: sentence.jaText,
        audio,
        explanation: `原句：${sentence.jaText}`,
        reviewLabel: sentence.jaText,
        quality: lessonQuality([audioQualityFlag(audio), ...tileQualityFlags(targetTiles)]),
        targetTiles,
        bankTiles: shuffleStable([...targetTiles, ...translationDistractorTiles(sentences, sentence.id)], sentence.id).slice(0, Math.max(6, targetTiles.length)),
        targetText: targetTiles.join(''),
      } satisfies TileLessonNode
    })
}

function buildGrammarClozeNodes(workSlug: string, episode: number, grammar: GrammarPoint[]): ClozeChoiceLessonNode[] {
  return grammar
    .filter((point) => point.pattern && point.jaExample)
    .slice(0, 8)
    .map((point, index): ClozeChoiceLessonNode | null => {
      const cloze = buildGrammarCloze(point, grammar, index)
      if (!cloze) return null
      return {
        type: 'cloze-choice',
        id: `${point.id}-cloze`,
        source: { kind: 'grammar', sourceId: point.id, workSlug, episode, lineNo: point.sourceLineNo },
        title: '选词填空',
        prompt: `选择最自然的表达：${point.functionZh}`,
        audio: { kind: 'tts', text: point.jaExample, autoPlay: false },
        explanation: `${point.explanationZh} ${point.pragmaticsNote}`,
        reviewLabel: point.pattern,
        quality: lessonQuality(cloze.flags),
        before: cloze.before,
        after: cloze.after,
        choices: cloze.values.map((value) => ({
          value,
          note: value === cloze.answer ? point.functionZh : '干扰项：注意语气、结构或意义是否匹配。',
        })),
        answer: cloze.answer,
      } satisfies ClozeChoiceLessonNode
    })
    .filter((node): node is ClozeChoiceLessonNode => Boolean(node))
}

function buildGrammarChoiceNodes(workSlug: string, episode: number, grammar: GrammarPoint[]): SingleChoiceLessonNode[] {
  return grammar
    .filter((point) => point.pattern && point.functionZh)
    .slice(0, 6)
    .map((point, index) => {
      const choices = buildDistractors(grammar.map((candidate) => candidate.functionZh), point.functionZh, index)
      return {
        type: 'single-choice',
        id: `${point.id}-function-choice`,
        source: { kind: 'grammar', sourceId: point.id, workSlug, episode, lineNo: point.sourceLineNo },
        title: '判断语法功能',
        prompt: `这句里的「${point.pattern}」主要表达什么？`,
        body: point.jaExample,
        audio: { kind: 'tts', text: point.jaExample, autoPlay: false },
        explanation: `${point.explanationZh} ${point.pragmaticsNote}`,
        reviewLabel: point.pattern,
        choices,
        answer: point.functionZh,
      } satisfies SingleChoiceLessonNode
    })
}

function sentenceAudio(workSlug: string, sentence: LearningSentence, autoPlay: boolean): PromptAudio {
  if (workSlug === 're-zero') {
    const source = buildReZeroShadowingAudio({
      sentenceId: sentence.id,
      audioUrl: sentence.audioUrl,
      storagePath: sentence.storagePath,
    })
    if (source) {
      return {
        kind: 'source',
        url: source.url,
        autoPlay: autoPlay && !source.isFlagged,
        reliability: source.isFlagged ? 'flagged' : 'verified',
        label: source.isFlagged ? '原声可能不准' : '播放原声',
        fallbackTts: { text: sentence.jaText, label: '播放 TTS' },
      }
    }
  }
  return { kind: 'tts', text: sentence.jaText, autoPlay, label: '播放 TTS' }
}

function buildModeNodes(
  pools: LessonPools,
  nodes: LessonNode[],
  mode: LessonMode,
  progressItems: Record<string, ProgressItem>,
  target?: LessonTarget,
  batch = 1,
) {
  if (mode === 'review') {
    const weakStates = new Set(['bad', 'ok', 'fuzzy', 'unknown'])
    const weakSourceIds = new Set(Object.values(progressItems)
      .filter((item) => weakStates.has(item.state))
      .map((item) => typeof item.payload?.sourceId === 'string' ? item.payload.sourceId : item.itemId))
    const weakNodes = nodes.filter((node) => weakStates.has(progressItems[node.id]?.state) || weakSourceIds.has(node.source.sourceId))
    return balanceLessonNodes(prioritizeReviewNodes(weakNodes.length ? weakNodes : nodes, progressItems), {
      'pair-match': 1,
      'single-choice': 2,
      'audio-tiles': 2,
      'translation-tiles': 2,
      'cloze-choice': 3,
    })
  }

  if (mode === 'vocab') {
    return buildStudyPracticeSequence(pools.vocabStudy, pools.vocabChoice, 1, pools.vocabStudy.length)
  }

  if (mode === 'grammar') {
    return buildStudyPracticeSequence(pools.grammarStudy, [...pools.grammarCloze, ...pools.grammarChoice], batch, 6)
  }

  if (mode === 'shadowing') {
    return buildStudyPracticeSequence(
      pools.sentenceStudy,
      prioritizeAudioNodes([...pools.sentenceAudio, ...pools.sentenceTranslation]),
      batch,
      6,
    )
  }

  if (mode === 'target' || target) {
    const targetNodes = buildTargetStudyPracticeSequence(pools, target)
    if (targetNodes.length) return targetNodes
    return balanceLessonNodes(nodes, {
      'pair-match': target?.kind === 'vocab' ? 1 : 0,
      'single-choice': 3,
      'audio-tiles': target?.kind === 'sentence' ? 3 : 0,
      'translation-tiles': target?.kind === 'sentence' || target?.kind === 'grammar' ? 2 : 0,
      'cloze-choice': target?.kind === 'grammar' ? 3 : 0,
    })
  }

  return buildMixedStudyPracticeSequence(pools, batch)
}

function balanceLessonNodes(nodes: LessonNode[], quota: Partial<Record<LessonNode['type'], number>>) {
  const output: LessonNode[] = []
  const order: LessonNode['type'][] = ['study-card', 'pair-match', 'audio-tiles', 'cloze-choice', 'translation-tiles', 'single-choice']

  for (const type of order) {
    output.push(...nodes.filter((node) => node.type === type).slice(0, quota[type] ?? 0))
  }
  return output.slice(0, 10)
}

function prioritizeReviewNodes(nodes: LessonNode[], progressItems: Record<string, ProgressItem>) {
  return [...nodes].sort((left, right) => reviewPriority(left, progressItems) - reviewPriority(right, progressItems))
}

function reviewPriority(node: LessonNode, progressItems: Record<string, ProgressItem>) {
  const state = progressItems[node.id]?.state
  if (state === 'bad' || state === 'fuzzy') return 0
  if (state === 'ok' || state === 'unknown') return 1
  return 2
}

function prioritizeAudioNodes(nodes: LessonNode[]) {
  return [...nodes].sort((left, right) => audioPriority(left.audio) - audioPriority(right.audio))
}

function audioPriority(audio: PromptAudio) {
  if (audio.kind === 'source' && audio.reliability === 'verified') return 0
  if (audio.kind === 'source') return 1
  if (audio.kind === 'tts') return 2
  return 3
}

function lessonId(workSlug: string, episode: number, mode: LessonMode, target?: LessonTarget, batch = 1) {
  const targetPart = target ? `-${target.kind}-${target.id}` : ''
  const batchPart = batch > 1 ? `-batch-${batch}` : ''
  return `${workSlug}-ep${String(episode).padStart(2, '0')}-${mode}${targetPart}${batchPart}-lesson`
}

function lessonTitle(workSlug: string, episode: number, mode: LessonMode, target?: LessonTarget, batch = 1) {
  const prefix = `${workSlug} EP${String(episode).padStart(2, '0')}`
  if (target) return `${prefix} 单点训练`
  const batchPart = batch > 1 ? ` 第 ${batch} 批` : ''
  if (mode === 'vocab') return `${prefix} 词汇训练${batchPart}`
  if (mode === 'grammar') return `${prefix} 语法训练${batchPart}`
  if (mode === 'shadowing') return `${prefix} 跟读训练${batchPart}`
  if (mode === 'review') return `${prefix} 回炉复习`
  return `${prefix} 综合训练`
}

function countNodeTypes(nodes: LessonNode[]): Record<LessonNode['type'], number> {
  return {
    'pair-match': nodes.filter((node) => node.type === 'pair-match').length,
    'single-choice': nodes.filter((node) => node.type === 'single-choice').length,
    'audio-tiles': nodes.filter((node) => node.type === 'audio-tiles').length,
    'translation-tiles': nodes.filter((node) => node.type === 'translation-tiles').length,
    'cloze-choice': nodes.filter((node) => node.type === 'cloze-choice').length,
    'study-card': nodes.filter((node) => node.type === 'study-card').length,
  }
}

function buildDistractors(values: string[], answer: string, offset: number) {
  const normalizedAnswer = compactText(answer)
  const unique = [...new Set(values
    .map((value) => value.trim())
    .filter((value) => value && compactText(value) !== normalizedAnswer))]
  const ranked = unique.sort((left, right) => distractorScore(answer, right) - distractorScore(answer, left))
  const rotated = [...ranked.slice(offset), ...ranked.slice(0, offset)]
  return shuffleStable([answer, ...rotated.slice(0, 3)], answer)
}

function buildVocabDistractors(vocab: VocabItem[], item: VocabItem, offset: number) {
  const ranked = vocab
    .filter((candidate) => candidate.id !== item.id && candidate.surface)
    .sort((left, right) => vocabDistractorScore(item, right) - vocabDistractorScore(item, left))
    .map((candidate) => candidate.surface)
  return buildDistractors(ranked, item.surface, offset)
}

function vocabDistractorScore(item: VocabItem, candidate: VocabItem) {
  let score = 0
  if (item.pos && candidate.pos === item.pos) score += 4
  if (item.jlptLevel && candidate.jlptLevel === item.jlptLevel) score += 2
  score -= Math.abs(item.surface.length - candidate.surface.length)
  return score
}

function scopeVocab(vocab: VocabItem[], target?: LessonTarget) {
  if (target?.kind !== 'vocab') return vocab
  const index = vocab.findIndex((item) => item.id === target.id)
  if (index < 0) return vocab
  return [vocab[index], ...vocab.filter((_, itemIndex) => itemIndex !== index)]
}

function scopeGrammar(grammar: GrammarPoint[], target?: LessonTarget) {
  if (target?.kind !== 'grammar') return grammar
  const index = grammar.findIndex((item) => item.id === target.id)
  if (index < 0) return grammar
  return [grammar[index], ...grammar.filter((_, itemIndex) => itemIndex !== index)]
}

function scopeSentences(sentences: LearningSentence[], target?: LessonTarget) {
  if (target?.kind !== 'sentence') return sentences
  const index = sentences.findIndex((item) => item.id === target.id)
  if (index < 0) return sentences
  return [sentences[index], ...sentences.filter((_, itemIndex) => itemIndex !== index)]
}

function normalizePattern(pattern: string) {
  return pattern.replace(/^～/u, '').replace(/[「」]/gu, '').trim()
}

function buildGrammarCloze(point: GrammarPoint, grammar: GrammarPoint[], index: number) {
  const candidate = resolveGrammarAnswer(point.pattern, point.jaExample)
  if (!candidate) return null
  const split = splitExampleForPattern(point.jaExample, candidate.answer)
  if (!split || split.before + split.after === point.jaExample) return null
  const values = buildDistractors(
    grammar.flatMap((item) => grammarAnswerChoices(item.pattern, item.jaExample)),
    candidate.answer,
    index,
  )
  if (values.length < 2) return null
  return {
    ...split,
    answer: candidate.answer,
    values,
    flags: candidate.flags,
  }
}

function resolveGrammarAnswer(pattern: string, example: string) {
  const normalized = normalizePattern(pattern)
  const candidates = grammarAnswerChoices(pattern, example)
  const direct = candidates.find((candidate) => example.includes(candidate))
  if (direct) return { answer: direct, flags: direct === normalized ? ['grammar:direct-pattern'] : ['grammar:inferred-pattern'] }
  if (normalized && example.includes(normalized)) return { answer: normalized, flags: ['grammar:direct-pattern'] }
  return null
}

function grammarAnswerChoices(pattern: string, example: string) {
  const normalized = normalizePattern(pattern)
  const slashParts = normalized.split(/[/／]/u).map((part) => part.trim()).filter(Boolean)
  const choices = slashParts.length > 1 ? slashParts : [normalized]

  if (/句末/u.test(pattern) || choices.some((choice) => /^(よ|ね|かな|だろ|よね)$/u.test(choice))) {
    const endings = ['よね', 'かな', 'だろ', 'よ', 'ね']
    const matched = endings.filter((ending) => example.includes(ending))
    return [...matched, ...endings]
  }

  return choices
    .map((choice) => choice.replace(/^～/u, '').trim())
    .filter((choice) => choice && !/句末/u.test(choice))
}

function splitExampleForPattern(example: string, pattern: string) {
  const index = example.lastIndexOf(pattern)
  if (index >= 0) {
    return {
      before: example.slice(0, index),
      after: example.slice(index + pattern.length),
    }
  }
  return null
}

function splitJapaneseTiles(text: string) {
  const chunks = text
    .replace(/[。！？!?]/gu, '')
    .split(/[\s、，]+/u)
    .flatMap((chunk) => chunk.length > 8 ? splitJapaneseLongChunk(chunk) : [chunk])
    .map((chunk) => chunk.trim())
    .filter(Boolean)
  return chunks.length > 1 ? mergeShortTiles(chunks) : mergeShortTiles(splitJapaneseLongChunk(text))
}

function splitChineseTiles(text: string) {
  const clean = text.replace(/[。！？!?]/gu, '')
  const pieces = clean.split(/[\s，,、]+/u).filter(Boolean)
  if (pieces.length > 1) return pieces
  return splitChineseLongChunk(clean)
}

function isUsableChineseMeaning(value: string | undefined) {
  if (!value) return false
  const text = value.trim()
  if (!text) return false
  if (/[\uE000-\uF8FF\uFFFD]/u.test(text)) return false
  if (/[ぁ-んァ-ヶ]/u.test(text)) return false
  if (!/[\p{Script=Han}]/u.test(text)) return false
  return true
}

function sentenceDistractorTiles(sentences: LearningSentence[], sourceId: string) {
  return sentences
    .filter((sentence) => sentence.id !== sourceId)
    .flatMap((sentence) => splitJapaneseTiles(sentence.jaText))
    .filter((tile) => tile.length <= 6)
    .slice(0, 4)
}

function translationDistractorTiles(sentences: LearningSentence[], sourceId: string) {
  return sentences
    .filter((sentence) => sentence.id !== sourceId)
    .flatMap((sentence) => splitChineseTiles(sentence.meaningZh))
    .filter((tile) => tile.length <= 6)
    .slice(0, 4)
}

function shuffleStable<T>(items: T[], seed: string) {
  const output = [...new Set(items)]
  let state = [...seed].reduce((sum, char) => sum + char.charCodeAt(0), 0) || 1
  for (let index = output.length - 1; index > 0; index -= 1) {
    state = (state * 1664525 + 1013904223) % 4294967296
    const swapIndex = state % (index + 1)
    const value = output[index]
    output[index] = output[swapIndex]
    output[swapIndex] = value
  }
  return output
}

function splitJapaneseLongChunk(text: string) {
  const clean = text.replace(/[。！？!?、，\s]/gu, '')
  if (!clean) return []

  const tiles: string[] = []
  let start = 0
  while (start < clean.length) {
    const remaining = clean.slice(start)
    const boundary = findJapaneseBoundary(remaining)
    const size = boundary > 0 ? boundary : Math.min(remaining.length, 7)
    tiles.push(remaining.slice(0, size))
    start += size
  }
  return mergeShortTiles(tiles)
}

function findJapaneseBoundary(text: string) {
  const earlyParticles = ['を', 'が', 'は', 'に', 'で', 'と', 'も', 'へ', 'の']
  for (const particle of earlyParticles) {
    const index = text.indexOf(particle)
    const boundary = index + particle.length
    const nextChar = text[boundary]
    if (index >= 1 && boundary >= 3 && boundary <= 6 && text.length - boundary >= 4 && /[一-龯ァ-ヶ]/u.test(nextChar)) return boundary
  }

  const endings = [
    'おいてね',
    'っていくのね',
    'っている',
    'っていく',
    'ないと',
    'ました',
    'ません',
    'ます',
    'です',
    'だろ',
    'かな',
    'よね',
    'って',
    'のね',
    'よ',
    'ね',
  ]
  for (const ending of endings) {
    const index = text.indexOf(ending)
    if (index >= 1 && index + ending.length <= 10) return index + ending.length
  }

  const particles = ['から', 'まで', 'ので', 'けど', 'ても', 'なら', 'って', 'を', 'が', 'は', 'に', 'で', 'と', 'も', 'へ', 'の']
  for (const particle of particles) {
    const index = text.indexOf(particle)
    const boundary = index + particle.length
    if (index >= 1 && boundary >= 3 && boundary <= 9 && text.length - boundary !== 1) return boundary
  }

  if (text.length <= 8) return text.length
  return 0
}

function splitChineseLongChunk(text: string) {
  const clean = text.replace(/[。！？!?，,、\s]/gu, '')
  if (!clean) return []
  const markers = ['这样下去', '然后', '但是', '所以', '因为', '如果', '已经', '就是', '可以', '应该', '不会', '就会', '变成', '大家', '资料']
  const tiles: string[] = []
  let remaining = clean

  while (remaining) {
    const marker = markers.find((item) => remaining.startsWith(item))
    if (marker) {
      tiles.push(marker)
      remaining = remaining.slice(marker.length)
      continue
    }
    const nextMarkerIndex = markers
      .map((item) => remaining.indexOf(item, 1))
      .filter((index) => index > 0)
      .sort((left, right) => left - right)[0]
    const size = nextMarkerIndex && nextMarkerIndex <= 5 ? nextMarkerIndex : Math.min(remaining.length, 4)
    tiles.push(remaining.slice(0, size))
    remaining = remaining.slice(size)
  }

  return mergeShortTiles(tiles)
}

function mergeShortTiles(tiles: string[]) {
  const output: string[] = []
  for (const tile of tiles.filter(Boolean)) {
    const previous = output[output.length - 1]
    if (tile.length === 1 && previous) {
      output[output.length - 1] = `${previous}${tile}`
    } else if (previous?.length === 1) {
      output[output.length - 1] = `${previous}${tile}`
    } else {
      output.push(tile)
    }
  }
  return output
}

function hasBadTileFragments(tiles: string[]) {
  return tiles.some((tile) => tile.length === 1 || /^(くのね|がってい|ってい)$/u.test(tile))
}

function tileQualityFlags(tiles: string[]) {
  const flags = ['tiles:natural']
  if (tiles.some((tile) => tile.length >= 9)) flags.push('tiles:long')
  if (hasBadTileFragments(tiles)) flags.push('tiles:fragment-risk')
  return flags
}

function audioQualityFlag(audio: PromptAudio) {
  if (audio.kind === 'source') return audio.reliability === 'verified' ? 'audio:source-verified' : 'audio:source-flagged'
  if (audio.kind === 'tts') return 'audio:tts'
  return 'audio:none'
}

function lessonQuality(flags: string[]) {
  const penalties = flags.filter((flag) => flag.includes('risk') || flag.includes('flagged') || flag.includes('tts')).length
  return {
    score: Math.max(0, 100 - penalties * 15),
    flags,
  }
}

function compactText(value: string) {
  return value.replace(/\s+/gu, '').trim()
}

function distractorScore(answer: string, candidate: string) {
  let score = 0
  score -= Math.abs(compactText(answer).length - compactText(candidate).length)
  if (/[ぁ-んァ-ヶー]/u.test(answer) === /[ぁ-んァ-ヶー]/u.test(candidate)) score += 2
  if (/[一-龯]/u.test(answer) === /[一-龯]/u.test(candidate)) score += 1
  return score
}
