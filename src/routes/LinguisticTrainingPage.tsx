import { useQuery } from '@tanstack/react-query'
import { Bot, Check, ChevronLeft, ChevronRight, Copy, RotateCcw, Search } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { PageHeader } from '../components/PageHeader'
import { StructuredAiResultView } from '../components/StructuredAiResultView'
import { TtsButton } from '../components/TtsButton'
import { usePreferredGatewayModel } from '../lib/aiPreferences'
import { getDeviceId, saveReviewState, useProgressItemsStore, useProgressStore } from '../lib/progress'
import { usePreferredReasoningEffort } from '../lib/reasoningPreferences'
import type { LinguisticExerciseDraft, StructuredAiResult } from '../lib/types'
import { animeRepository } from '../server/repositories/animeRepository'

const domainLabels: Record<string, string> = {
  phonology: '音系学',
  morphology: '形态学',
  syntax: '句法学',
  pragmatics: '语用学',
  historical: '历史语言学',
  sociolinguistics: '社会语言学',
}

const questionTypeLabels: Record<string, string> = {
  kuuki_yomi: '读空气',
  listening_reasoning: '听感解释',
  morphology_analysis: '形态分析',
  syntax_relation: '句法关系',
  multiple_choice: '选择题',
  contrast_choice: '对比判断',
}

const workLabels: Record<string, string> = {
  'k-on': 'K-ON!',
}

const airDraftKey = 'anime-japanese-lab-air-question-drafts'

type AirQuestionCandidate = {
  sceneJa: string
  sceneZh: string
  sceneLines?: {
    lineNo?: number
    speaker?: string
    jaText: string
    zhText?: string
    isTarget?: boolean
  }[]
  targetLineNo?: number
  question: string
  options: string[]
  answer: string
  explanation: string
  evidence: string[]
  source: {
    work: string
    episode: number
    chunkNo: number
    startTime: string
    endTime: string
    sourceId?: string
  }
}

type FilterState = {
  workSlug: string
  domain: string
  phenomenonKey: string
  questionType: string
  difficulty: string
  episode: string
}

type Mode = 'train' | 'browse'

export function LinguisticTrainingPage() {
  const [mode, setMode] = useState<Mode>('train')
  const [localAirExercises, setLocalAirExercises] = useState<LinguisticExerciseDraft[]>([])
  const [filters, setFilters] = useState<FilterState>({
    workSlug: 'all',
    domain: 'all',
    phenomenonKey: 'all',
    questionType: 'all',
    difficulty: 'all',
    episode: 'all',
  })
  const [currentIndex, setCurrentIndex] = useState(0)
  const [pinnedExerciseId, setPinnedExerciseId] = useState<string | null>(null)
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const [aiResults, setAiResults] = useState<Record<string, StructuredAiResult>>({})
  const [aiStatus, setAiStatus] = useState<Record<string, 'idle' | 'loading' | 'error'>>({})
  const model = usePreferredGatewayModel()
  const reasoningEffort = usePreferredReasoningEffort()
  const progressStore = useProgressStore()
  const progressItems = useProgressItemsStore()
  const exercisesQuery = useQuery({
    queryKey: ['linguistic-exercises'],
    queryFn: () => animeRepository.listLinguisticExercises(),
  })

  const publishedExercises = exercisesQuery.data ?? []
  const exercises = useMemo(() => [...localAirExercises, ...publishedExercises], [localAirExercises, publishedExercises])
  const filterOptions = useMemo(() => buildFilterOptions(exercises), [exercises])
  const scopedExercises = useMemo(() => filterExercises(exercises, filters), [exercises, filters])
  const filteredExercises = useMemo(
    () => scopedExercises.filter((exercise) => !progressStore[exercise.id] || exercise.id === pinnedExerciseId),
    [pinnedExerciseId, progressStore, scopedExercises],
  )
  const persistedAnswers = useMemo(() => {
    return Object.fromEntries(Object.entries(progressItems)
      .map(([id, item]) => [id, typeof item.payload?.selected === 'string' ? item.payload.selected : ''])
      .filter(([, selected]) => selected)) as Record<string, string>
  }, [progressItems])
  const currentExercise = filteredExercises[currentIndex]
  const currentSelectedOption = currentExercise ? answers[currentExercise.id] ?? persistedAnswers[currentExercise.id] ?? '' : ''

  useEffect(() => {
    setCurrentIndex(0)
  }, [filters])

  useEffect(() => {
    function loadLocalAirExercises() {
      setLocalAirExercises(readLocalAirExercises())
    }

    loadLocalAirExercises()
    window.addEventListener('storage', loadLocalAirExercises)
    window.addEventListener('focus', loadLocalAirExercises)
    return () => {
      window.removeEventListener('storage', loadLocalAirExercises)
      window.removeEventListener('focus', loadLocalAirExercises)
    }
  }, [])

  useEffect(() => {
    if (currentIndex >= filteredExercises.length) {
      setCurrentIndex(Math.max(filteredExercises.length - 1, 0))
    }
  }, [currentIndex, filteredExercises.length])

  function updateFilter(key: keyof FilterState, value: string) {
    setFilters((current) => ({
      ...current,
      [key]: value,
      ...(key === 'domain' ? { phenomenonKey: 'all' } : {}),
    }))
  }

  function resetQueue() {
    setCurrentIndex(0)
    setPinnedExerciseId(null)
    setAnswers({})
    setAiResults({})
    setAiStatus({})
  }

  function resetFilters() {
    setFilters({
      workSlug: 'all',
      domain: 'all',
      phenomenonKey: 'all',
      questionType: 'all',
      difficulty: 'all',
      episode: 'all',
    })
    resetQueue()
  }

  async function explainAnswer(exercise: LinguisticExerciseDraft, selectedOption: string) {
    const correctOption = getCorrectOption(exercise)
    const cacheKey = `${exercise.id}:${selectedOption}`
    setAiStatus((current) => ({ ...current, [cacheKey]: 'loading' }))
    try {
      const response = await fetch('/api/ai/explain', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          kind: 'linguistic',
          text: exercise.jaText,
          context: buildAiContext(exercise, selectedOption, correctOption),
          model,
          reasoningEffort,
          deviceId: getDeviceId(),
        }),
      })
      if (!response.ok) throw new Error(await response.text())
      const result = await response.json() as StructuredAiResult
      setAiResults((current) => ({ ...current, [cacheKey]: result }))
      setAiStatus((current) => ({ ...current, [cacheKey]: 'idle' }))
    } catch (error) {
      console.error(error)
      setAiStatus((current) => ({ ...current, [cacheKey]: 'error' }))
    }
  }

  async function answerExercise(exercise: LinguisticExerciseDraft, selectedOption: string) {
    const correctOption = getCorrectOption(exercise)
    setPinnedExerciseId(exercise.id)
    setAnswers((current) => ({ ...current, [exercise.id]: selectedOption }))
    await saveReviewState(exercise.id, selectedOption === correctOption ? 'good' : 'bad', {
      itemType: 'exercise',
      workSlug: exercise.workSlug,
      episode: exercise.episode,
      payload: {
        label: exercise.prompt,
        selected: selectedOption,
        answer: correctOption,
        domain: exercise.domain,
        phenomenonKey: exercise.phenomenonKey,
        questionType: exercise.questionType,
      },
    })
  }

  function goNext() {
    if (currentSelectedOption) {
      setPinnedExerciseId(null)
      setCurrentIndex((index) => Math.min(index, Math.max(filteredExercises.length - 2, 0)))
      return
    }
    setCurrentIndex((index) => Math.min(index + 1, filteredExercises.length - 1))
  }

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="学习作答"
        title="语言现象与读空气训练"
        description="默认单题推进。这里会同时显示正式发布题，以及你在读空气 RAG 页预览保存的本地草稿。"
      />

      <section className="linguistic-mode-bar">
        <div className="segmented-control">
          <button className={mode === 'train' ? 'selected' : ''} type="button" onClick={() => setMode('train')}>单题训练</button>
          <button className={mode === 'browse' ? 'selected' : ''} type="button" onClick={() => setMode('browse')}>浏览全部题目</button>
        </div>
        <div className="card-actions">
          <button className="icon-button secondary" type="button" onClick={resetQueue}>
            <RotateCcw size={18} />
            <span>重置队列</span>
          </button>
          <button className="icon-button secondary" type="button" onClick={resetFilters}>
            <Search size={18} />
            <span>清空筛选</span>
          </button>
        </div>
      </section>

      <section className="linguistic-filter-panel training-filter-panel">
        <FilterSelect
          id="linguistic-work"
          label="Work"
          value={filters.workSlug}
          onChange={(value) => updateFilter('workSlug', value)}
          options={filterOptions.works.map((workSlug) => ({ value: workSlug, label: workLabel(workSlug) }))}
          allLabel="全部作品"
        />
        <FilterSelect
          id="linguistic-domain"
          label="Domain"
          value={filters.domain}
          onChange={(value) => updateFilter('domain', value)}
          options={filterOptions.domains.map((domain) => ({ value: domain, label: domainLabel(domain) }))}
          allLabel="全部领域"
        />
        <FilterSelect
          id="linguistic-phenomenon"
          label="Phenomenon"
          value={filters.phenomenonKey}
          onChange={(value) => updateFilter('phenomenonKey', value)}
          options={filterOptions.phenomena
            .filter((phenomenon) => filters.domain === 'all' || phenomenon.domain === filters.domain)
            .map((phenomenon) => ({ value: phenomenon.key, label: phenomenon.key }))}
          allLabel="全部语言现象"
        />
        <FilterSelect
          id="linguistic-question-type"
          label="Question Type"
          value={filters.questionType}
          onChange={(value) => updateFilter('questionType', value)}
          options={filterOptions.questionTypes.map((questionType) => ({ value: questionType, label: questionTypeLabel(questionType) }))}
          allLabel="全部题型"
        />
        <FilterSelect
          id="linguistic-difficulty"
          label="Difficulty"
          value={filters.difficulty}
          onChange={(value) => updateFilter('difficulty', value)}
          options={filterOptions.difficulties.map((difficulty) => ({ value: difficulty, label: difficulty }))}
          allLabel="全部难度"
        />
      </section>

      <details className="advanced-filter-panel">
        <summary>高级筛选</summary>
        <FilterSelect
          id="linguistic-episode"
          label="Episode"
          value={filters.episode}
          onChange={(value) => updateFilter('episode', value)}
          options={filterOptions.episodes.map((episode) => ({ value: episode, label: `EP${episode.padStart(2, '0')}` }))}
          allLabel="全部集数"
        />
      </details>

      <section className="source-preview">
        <p className="eyebrow">当前训练队列</p>
        <strong>{exercisesQuery.isLoading ? '读取中' : `${filteredExercises.length} / ${scopedExercises.length} 题`}</strong>
        <span>{filterSummary(filters)} · 本地草稿 {localAirExercises.length} 题</span>
      </section>

      {!exercisesQuery.isLoading && scopedExercises.length === 0 ? (
        <div className="source-preview">暂无语言学训练题</div>
      ) : null}

      {!exercisesQuery.isLoading && scopedExercises.length > 0 && filteredExercises.length === 0 && mode === 'train' ? (
        <div className="source-preview">这一组题都已保存进度，请到今日复习继续回炉。</div>
      ) : null}

      {mode === 'train' && currentExercise ? (
        <TrainingCard
          currentIndex={currentIndex}
          total={filteredExercises.length}
          exercise={currentExercise}
          selectedOption={currentSelectedOption}
          aiResult={aiResults[`${currentExercise.id}:${currentSelectedOption}`]}
          aiStatus={aiStatus[`${currentExercise.id}:${currentSelectedOption}`] ?? 'idle'}
          onSelect={(option) => void answerExercise(currentExercise, option)}
          onPrevious={() => setCurrentIndex((index) => Math.max(index - 1, 0))}
          onNext={goNext}
          onRetry={() => {
            setPinnedExerciseId(currentExercise.id)
            setAnswers((current) => ({ ...current, [currentExercise.id]: '' }))
          }}
          onExplainAnswer={(selectedOption) => void explainAnswer(currentExercise, selectedOption)}
        />
      ) : null}

      {mode === 'browse' ? (
        <BrowseList
          exercises={scopedExercises}
          selectedAnswers={{ ...persistedAnswers, ...answers }}
          onSelect={(exercise, option) => void answerExercise(exercise, option)}
        />
      ) : null}
    </section>
  )
}

function FilterSelect({
  id,
  label,
  value,
  options,
  allLabel,
  onChange,
}: {
  id: string
  label: string
  value: string
  options: { value: string; label: string }[]
  allLabel: string
  onChange: (value: string) => void
}) {
  return (
    <div className="filter-field">
      <label htmlFor={id}>{label}</label>
      <select id={id} value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="all">{allLabel}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
    </div>
  )
}

function TrainingCard({
  currentIndex,
  total,
  exercise,
  selectedOption,
  aiResult,
  aiStatus,
  onSelect,
  onPrevious,
  onNext,
  onRetry,
  onExplainAnswer,
}: {
  currentIndex: number
  total: number
  exercise: LinguisticExerciseDraft
  selectedOption: string
  aiResult?: StructuredAiResult
  aiStatus: 'idle' | 'loading' | 'error'
  onSelect: (option: string) => void
  onPrevious: () => void
  onNext: () => void
  onRetry: () => void
  onExplainAnswer: (selectedOption: string) => void
}) {
  const correctOption = getCorrectOption(exercise)
  const answered = Boolean(selectedOption)
  const isCorrect = selectedOption === correctOption

  return (
    <article className="linguistic-trainer-card">
      <header className="trainer-progress-row">
        <div>
          <p className="eyebrow">单题训练</p>
          <strong>第 {currentIndex + 1} / {total} 题</strong>
        </div>
        <span className="review-chip">{sourceLabel(exercise)}</span>
      </header>

      <div className="linguistic-card-meta">
        <span>{domainLabel(exercise.domain)}</span>
        <span>{exercise.phenomenonKey}</span>
        <span>{questionTypeLabel(exercise.questionType)}</span>
        <span>{exercise.difficulty}</span>
      </div>

      <div className="dialogue-block">
        <DialogueEvidence exercise={exercise} />
      </div>

      {shouldShowTts(exercise) ? (
        <div className="tts-disclaimer">
          <TtsButton text={exercise.jaText} label="辅助朗读" />
          <span>TTS 仅辅助朗读，音系学判断以原声/真人发音为准。</span>
        </div>
      ) : null}

      <h2>{exercise.prompt}</h2>
      <OptionList exercise={exercise} selectedOption={selectedOption} onSelect={onSelect} />

      {answered ? (
        <AnswerSummary
          exercise={exercise}
          selectedOption={selectedOption}
          correctOption={correctOption}
          aiResult={aiResult}
          aiStatus={aiStatus}
          onExplainAnswer={() => onExplainAnswer(selectedOption)}
        />
      ) : null}

      <footer className="trainer-actions">
        <button className="icon-button secondary" type="button" onClick={onPrevious} disabled={currentIndex === 0}>
          <ChevronLeft size={18} />
          <span>上一题</span>
        </button>
        <button className="icon-button secondary" type="button" onClick={onRetry} disabled={!answered}>
          <RotateCcw size={18} />
          <span>重做本题</span>
        </button>
        <button className="primary-action" type="button" onClick={onNext} disabled={!answered && currentIndex >= total - 1}>
          <span>{answered && currentIndex >= total - 1 ? '完成' : '下一题'}</span>
          <ChevronRight size={18} />
        </button>
      </footer>
    </article>
  )
}

function BrowseList({
  exercises,
  selectedAnswers,
  onSelect,
}: {
  exercises: LinguisticExerciseDraft[]
  selectedAnswers: Record<string, string>
  onSelect: (exercise: LinguisticExerciseDraft, option: string) => void
}) {
  return (
    <div className="linguistic-card-grid">
      {exercises.map((exercise) => (
        <article className="linguistic-card" key={exercise.id}>
          <div className="linguistic-card-meta">
            <span>{sourceLabel(exercise)}</span>
            <span>{domainLabel(exercise.domain)}</span>
            <span>{questionTypeLabel(exercise.questionType)}</span>
          </div>
          <div className="dialogue-block">
            <DialogueEvidence exercise={exercise} compact />
          </div>
          <h2>{exercise.prompt}</h2>
          <OptionList
            exercise={exercise}
            selectedOption={selectedAnswers[exercise.id] ?? ''}
            onSelect={(option) => onSelect(exercise, option)}
          />
          {selectedAnswers[exercise.id] ? (
            <AnswerSummary
              exercise={exercise}
              selectedOption={selectedAnswers[exercise.id]}
              correctOption={getCorrectOption(exercise)}
            />
          ) : null}
        </article>
      ))}
    </div>
  )
}

function DialogueEvidence({ exercise, compact = false }: { exercise: LinguisticExerciseDraft; compact?: boolean }) {
  const [showChinese, setShowChinese] = useState(false)
  const allLines = buildDialogueLines(exercise)
  const quote = extractQuestionQuote(exercise.prompt)
  const hasTranslations = allLines.some((line) => line.zhText)

  return (
    <div className="dialogue-evidence">
      <header className="dialogue-evidence-header">
        <span>全文语境 / 日文原文</span>
      </header>
      <div className={compact ? 'dialogue-lines compact' : 'dialogue-lines'}>
        {allLines.map((line, index) => (
          <div className="dialogue-line" key={`${line.speaker ?? 'narration'}-${index}-${line.jaText.slice(0, 20)}`}>
            {line.speaker ? <b>{line.speaker}</b> : <b className="muted-speaker">未标注</b>}
            <strong><HighlightedText text={line.jaText} highlight={quote} /></strong>
          </div>
        ))}
      </div>
      {hasTranslations ? (
        <>
          <button className="text-button" type="button" onClick={() => setShowChinese((value) => !value)}>
            {showChinese ? '收起中文参考' : '展开中文参考'}
          </button>
          {showChinese ? (
            <div className="translation-lines">
              {allLines.map((line, index) => line.zhText ? (
                <p key={`${line.speaker ?? 'narration'}-${index}-zh`}>
                  {line.speaker ? <b>{line.speaker}</b> : <b className="muted-speaker">未标注</b>}
                  <span>{line.zhText}</span>
                </p>
              ) : null)}
            </div>
          ) : null}
        </>
      ) : null}
    </div>
  )
}

function HighlightedText({ text, highlight }: { text: string; highlight?: string }) {
  if (!highlight) return <>{text}</>
  const index = text.indexOf(highlight)
  if (index < 0) return <>{text}</>
  return (
    <>
      {text.slice(0, index)}
      <mark>{highlight}</mark>
      {text.slice(index + highlight.length)}
    </>
  )
}

function OptionList({
  exercise,
  selectedOption,
  onSelect,
}: {
  exercise: LinguisticExerciseDraft
  selectedOption: string
  onSelect: (option: string) => void
}) {
  const correctOption = getCorrectOption(exercise)
  const answered = Boolean(selectedOption)

  return (
    <div className="linguistic-options" role="radiogroup" aria-label={exercise.prompt}>
      {exercise.options.map((option, index) => {
        const isSelected = selectedOption === option
        const isAnswer = answered && option === correctOption
        const isWrong = answered && isSelected && option !== correctOption
        return (
          <button
            className={[
              'choice-option',
              isSelected ? 'selected' : '',
              isAnswer ? 'correct' : '',
              isWrong ? 'wrong' : '',
            ].filter(Boolean).join(' ')}
            type="button"
            key={`${exercise.id}-${index}`}
            disabled={answered}
            onClick={() => onSelect(option)}
          >
            {option}
          </button>
        )
      })}
    </div>
  )
}

function AnswerSummary({
  exercise,
  selectedOption,
  correctOption,
  aiResult,
  aiStatus,
  onExplainAnswer,
}: {
  exercise: LinguisticExerciseDraft
  selectedOption: string
  correctOption: string
  aiResult?: StructuredAiResult
  aiStatus?: 'idle' | 'loading' | 'error'
  onExplainAnswer?: () => void
}) {
  const isCorrect = selectedOption === correctOption
  const [copyStatus, setCopyStatus] = useState<'idle' | 'copied' | 'error'>('idle')

  async function copyQuestionPrompt() {
    try {
      await navigator.clipboard.writeText(buildExternalQuestionPrompt(exercise, selectedOption, correctOption))
      setCopyStatus('copied')
      window.setTimeout(() => setCopyStatus('idle'), 1600)
    } catch (error) {
      console.error(error)
      setCopyStatus('error')
      window.setTimeout(() => setCopyStatus('idle'), 2200)
    }
  }

  return (
    <section className={isCorrect ? 'linguistic-answer correct' : 'linguistic-answer wrong'}>
      {onExplainAnswer ? (
        <div className="mistake-ai-callout">
          <div>
            <p className="eyebrow">AI 讲解</p>
            <strong>{isCorrect ? '解释为什么这个判断成立' : '针对你选错的选项讲解'}</strong>
          </div>
          {aiResult ? null : (
            <button className="primary-action" type="button" onClick={onExplainAnswer} disabled={aiStatus === 'loading'}>
              <Bot size={18} />
              <span>{aiStatus === 'loading' ? '分析中' : aiStatus === 'error' ? '重新分析' : isCorrect ? 'AI讲解这题' : 'AI解释我为什么错'}</span>
            </button>
          )}
        </div>
      ) : null}
      <div className="answer-status-row">
        <div>
          <p className="eyebrow">你的选择</p>
          <strong>{selectedOption}</strong>
        </div>
        <div>
          <p className="eyebrow">正确答案（数据库）</p>
          <strong>{exercise.answer.answerZh || correctOption}</strong>
          <small>来自 Supabase linguistic_exercise_drafts.answer</small>
        </div>
      </div>
      <ExplanationLine label="基础说明" value={exercise.basicExplanationZh} />
      <ExplanationLine label="深入解释" value={exercise.deepExplanationZh} />
      <ExplanationLine label="动画语境" value={exercise.animeContextNoteZh} />
      <ExplanationLine label="注意事项" value={exercise.cautionNoteZh} />

      <div className="card-actions">
        <button className="icon-button secondary" type="button" onClick={() => void copyQuestionPrompt()}>
          {copyStatus === 'copied' ? <Check size={18} /> : <Copy size={18} />}
          <span>{copyStatus === 'copied' ? '已复制' : copyStatus === 'error' ? '复制失败' : '复制当前题目'}</span>
        </button>
      </div>

      {onExplainAnswer ? (
        <div className="ai-explain">
          {aiResult ? <StructuredAiResultView result={aiResult} /> : null}
        </div>
      ) : null}
    </section>
  )
}

function ExplanationLine({ label, value }: { label: string; value?: string }) {
  if (!value) return null
  return (
    <p>
      <b>{label}</b>
      <span>{value}</span>
    </p>
  )
}

type DialogueLine = {
  speaker?: string
  jaText: string
  zhText?: string
}

function buildDialogueLines(exercise: LinguisticExerciseDraft): DialogueLine[] {
  const markerTurns = parseSpeakerMarkedTranscript(exercise.jaText, exercise.zhText)
  if (markerTurns.length > 0) return markerTurns

  if (exercise.sceneLines?.length) {
    const grouped: DialogueLine[] = []
    exercise.sceneLines
      .filter((line) => line.jaText)
      .forEach((line) => {
        const speaker = line.speaker?.trim()
        const previous = grouped[grouped.length - 1]
        if (previous && previous.speaker === speaker) {
          previous.jaText = joinUtterance(previous.jaText, line.jaText)
          previous.zhText = joinUtterance(previous.zhText ?? '', line.zhText ?? '')
          return
        }
        grouped.push({ speaker, jaText: line.jaText, zhText: line.zhText })
      })
    if (grouped.length > 0) return grouped
  }

  const jaLines = splitSceneLines(exercise.jaText)
  const zhLines = splitSceneLines(exercise.zhText ?? '')
  return [{
    jaText: jaLines.join(' '),
    zhText: zhLines.join(' '),
  }]
}

function splitSceneLines(text: string) {
  return text
    .split(/\n+/)
    .map((line) => line.trim())
    .filter(Boolean)
}

function parseSpeakerMarkedTranscript(jaText: string, zhText?: string): DialogueLine[] {
  const jaTurns = parseSpeakerTurns(jaText)
  if (jaTurns.length === 0) return []
  const zhTurns = parseSpeakerTurns(zhText ?? '')
  return jaTurns.map((turn, index) => ({
    speaker: turn.speaker,
    jaText: turn.text,
    zhText: zhTurns[index]?.text,
  }))
}

function parseSpeakerTurns(text: string) {
  const markerPattern = /（([^）]+)）/gu
  const matches = [...text.matchAll(markerPattern)]
  if (matches.length === 0) return []

  return matches
    .map((match, index) => {
      const speaker = match[1].trim()
      const start = (match.index ?? 0) + match[0].length
      const end = index + 1 < matches.length ? matches[index + 1].index ?? text.length : text.length
      return {
        speaker,
        text: normalizeTranscriptText(text.slice(start, end)),
      }
    })
    .filter((turn) => turn.text)
}

function normalizeTranscriptText(text: string) {
  return text.replace(/\s+/gu, ' ').trim()
}

function joinUtterance(current: string, next: string) {
  const cleanNext = next.trim()
  if (!current) return cleanNext
  if (!cleanNext) return current
  return `${current.trim()} ${cleanNext}`
}

function extractQuestionQuote(question: string) {
  return question.match(/「([^」]{2,80})」/u)?.[1]?.trim()
}

function buildFilterOptions(exercises: LinguisticExerciseDraft[]) {
  const works = [...new Set(exercises.map((exercise) => exercise.workSlug).filter(Boolean))].sort()
  const episodes = [...new Set(exercises.map((exercise) => String(exercise.episode ?? '')).filter(Boolean))].sort(
    (a, b) => Number(a) - Number(b),
  )
  const domains = [...new Set(exercises.map((exercise) => exercise.domain).filter(Boolean))].sort()
  const questionTypes = [...new Set(exercises.map((exercise) => exercise.questionType).filter(Boolean))].sort()
  const difficulties = [...new Set(exercises.map((exercise) => exercise.difficulty).filter(Boolean))].sort()
  const phenomena = [...new Map(exercises.map((exercise) => [
    exercise.phenomenonKey,
    { key: exercise.phenomenonKey, domain: exercise.domain },
  ])).values()].sort((a, b) => a.key.localeCompare(b.key))
  return { works, episodes, domains, questionTypes, difficulties, phenomena }
}

function readLocalAirExercises(): LinguisticExerciseDraft[] {
  try {
    const drafts = JSON.parse(window.localStorage.getItem(airDraftKey) ?? '[]') as AirQuestionCandidate[]
    return drafts
      .filter((draft) => draft?.question && Array.isArray(draft.options) && draft.options.length > 0)
      .map((draft, index) => {
        const correctIndex = draft.options.findIndex((option) => option === draft.answer)
        const sourceId = draft.source?.sourceId || `${draft.source?.work ?? 'air'}-${draft.source?.episode ?? 0}-${draft.source?.chunkNo ?? index}`
        return {
          id: `local-air-${sourceId}-${index}`,
          workSlug: draft.source?.work || 'k-on',
          episode: draft.source?.episode,
          sourceLineNo: undefined,
          jaText: draft.sceneJa,
          zhText: draft.sceneZh,
          sceneLines: draft.sceneLines,
          targetLineNo: draft.targetLineNo,
          domain: 'pragmatics',
          phenomenonKey: 'ai_saved_air_question',
          questionType: 'kuuki_yomi',
          prompt: draft.question,
          options: draft.options,
          answer: {
            answerZh: draft.answer,
            correctIndex: correctIndex >= 0 ? correctIndex : undefined,
          },
          basicExplanationZh: draft.explanation || '基于保存的 AI 草稿题作答。',
          deepExplanationZh: draft.evidence?.join(' / '),
          animeContextNoteZh: sourceTimeLabel(draft),
          cautionNoteZh: '本题是 RAG 页预览保存的本地草稿，确认发布后才会进入正式题库。',
          difficulty: 'AI候选',
          qualityScore: 0,
        }
      })
  } catch {
    return []
  }
}

function sourceTimeLabel(draft: AirQuestionCandidate) {
  const source = draft.source
  if (!source) return ''
  return `${workLabel(source.work)} · EP${String(source.episode).padStart(2, '0')} · chunk ${source.chunkNo} · ${source.startTime}-${source.endTime}`
}

function filterExercises(exercises: LinguisticExerciseDraft[], filters: FilterState) {
  return exercises.filter((exercise) => {
    const workMatch = filters.workSlug === 'all' || exercise.workSlug === filters.workSlug
    const episodeMatch = filters.episode === 'all' || String(exercise.episode ?? '') === filters.episode
    const domainMatch = filters.domain === 'all' || exercise.domain === filters.domain
    const phenomenonMatch = filters.phenomenonKey === 'all' || exercise.phenomenonKey === filters.phenomenonKey
    const questionTypeMatch = filters.questionType === 'all' || exercise.questionType === filters.questionType
    const difficultyMatch = filters.difficulty === 'all' || exercise.difficulty === filters.difficulty
    return workMatch && episodeMatch && domainMatch && phenomenonMatch && questionTypeMatch && difficultyMatch
  })
}

function domainLabel(domain: string) {
  return domainLabels[domain] ?? domain
}

function questionTypeLabel(questionType: string) {
  return questionTypeLabels[questionType] ?? questionType
}

function workLabel(workSlug: string) {
  return workLabels[workSlug] ?? workSlug
}

function sourceLabel(exercise: LinguisticExerciseDraft) {
  const episode = exercise.episode ? `EP${String(exercise.episode).padStart(2, '0')}` : 'EP--'
  const line = exercise.sourceLineNo ? `Line ${exercise.sourceLineNo}` : 'Line --'
  return `${workLabel(exercise.workSlug)} · ${episode} · ${line}`
}

function getCorrectOption(exercise: LinguisticExerciseDraft) {
  const correctIndex = exercise.answer.correctIndex
  if (typeof correctIndex === 'number' && exercise.options[correctIndex]) return exercise.options[correctIndex]
  return exercise.answer.answerZh
}

function shouldShowTts(exercise: LinguisticExerciseDraft) {
  return exercise.domain === 'phonology' || exercise.questionType === 'listening_reasoning'
}

function filterSummary(filters: FilterState) {
  const summary = [
    filters.workSlug === 'all' ? '全部作品' : workLabel(filters.workSlug),
    filters.domain === 'all' ? '全部领域' : domainLabel(filters.domain),
    filters.phenomenonKey === 'all' ? '全部语言现象' : filters.phenomenonKey,
    filters.questionType === 'all' ? '全部题型' : questionTypeLabel(filters.questionType),
    filters.difficulty === 'all' ? '全部难度' : filters.difficulty,
  ]
  if (filters.episode !== 'all') summary.push(`EP${filters.episode.padStart(2, '0')}`)
  return summary.join(' / ')
}

function buildAiContext(exercise: LinguisticExerciseDraft, selectedOption: string, correctOption: string) {
  const isCorrect = selectedOption === correctOption
  return [
    `task: 请用简体中文讲解这道日语读空气/语言现象题。用户${isCorrect ? '答对了' : '答错了'}。`,
    isCorrect
      ? 'focus: 说明为什么正确答案成立，并补充这题最容易混淆的点。'
      : 'focus: 说明用户所选项为什么不成立，以及正确答案为什么更符合语境。',
    `ja_text: ${exercise.jaText}`,
    `zh_text: ${exercise.zhText ?? ''}`,
    `prompt: ${exercise.prompt}`,
    `options: ${exercise.options.map((option, index) => `${index + 1}. ${option}`).join(' | ')}`,
    `correct answer: ${exercise.answer.answerZh || correctOption}`,
    `user selected answer: ${selectedOption}`,
    `basic_explanation_zh: ${exercise.basicExplanationZh}`,
    `deep_explanation_zh: ${exercise.deepExplanationZh ?? ''}`,
    `phenomenon_key: ${exercise.phenomenonKey}`,
    `domain: ${exercise.domain}`,
  ].join('\n')
}

function buildExternalQuestionPrompt(exercise: LinguisticExerciseDraft, selectedOption: string, correctOption: string) {
  const isCorrect = selectedOption === correctOption
  return [
    '请作为严谨的日语老师，用简体中文重新讲解下面这道日语读空气/语言现象题。',
    isCorrect
      ? '我答对了，但我想听到更深入、更不同角度的解释。请说明正确答案为什么成立，并指出其他选项为什么不如它。'
      : '我答错了。请先指出我选错的原因，再解释正确答案为什么更自然，并总结我下次该如何判断。',
    '',
    `【作答结果】${isCorrect ? '答对' : '答错'}`,
    `【我的答案】${selectedOption}`,
    `【正确答案】${exercise.answer.answerZh || correctOption}`,
    '',
    `【日文场景】${exercise.jaText}`,
    exercise.zhText ? `【中文参考】${exercise.zhText}` : '',
    `【题目】${exercise.prompt}`,
    '【选项】',
    ...exercise.options.map((option, index) => `${index + 1}. ${option}`),
    '',
    `【领域】${domainLabel(exercise.domain)}`,
    `【语言现象】${exercise.phenomenonKey}`,
    `【题型】${questionTypeLabel(exercise.questionType)}`,
    `【难度】${exercise.difficulty}`,
    `【来源】${sourceLabel(exercise)}`,
    '',
    exercise.basicExplanationZh ? `【站内基础说明】${exercise.basicExplanationZh}` : '',
    exercise.deepExplanationZh ? `【站内深入解释】${exercise.deepExplanationZh}` : '',
    exercise.animeContextNoteZh ? `【动画语境】${exercise.animeContextNoteZh}` : '',
    exercise.cautionNoteZh ? `【注意事项】${exercise.cautionNoteZh}` : '',
    '',
    '请不要只重复答案。请按“语境线索 → 选项对比 → 正确判断 → 可迁移判断方法”的结构解释。',
  ].filter(Boolean).join('\n')
}
