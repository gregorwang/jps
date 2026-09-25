import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { BookOpen, ChevronLeft, ChevronRight, RotateCcw } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { saveReviewState, useProgressItemsStore } from '../lib/progress'
import type {
  FoundationDomain,
  FoundationQuestion,
  FoundationStage,
  FoundationStimulus,
  FoundationTopic,
} from '../lib/types'
import { animeRepository } from '../server/repositories/animeRepository'

const foundationDomainLabels: Record<FoundationDomain, string> = {
  phonology_writing: '语音与文字',
  morphology: '形态学',
  syntax: '句法学',
  semantics: '语义学',
  pragmatics_discourse: '语用与话语',
  sociolinguistics: '社会语言学',
  historical_grammaticalization: '历史与语法化',
}

const foundationStageLabels: Record<FoundationStage, string> = {
  F1: 'F1 · 识别',
  F2: 'F2 · 解析',
  F3: 'F3 · 对比诊断',
  F4: 'F4 · 迁移应用',
}

type FoundationMode = 'train' | 'catalog'

export function FoundationLinguisticTraining({
  initialPackId,
  initialTopicId,
  initialStage,
  initialQuestionId,
}: {
  initialPackId?: string
  initialTopicId?: string
  initialStage?: FoundationStage
  initialQuestionId?: string
}) {
  const [mode, setMode] = useState<FoundationMode>('train')
  const [packId, setPackId] = useState(initialPackId ?? '')
  const [domain, setDomain] = useState<FoundationDomain | 'all'>('all')
  const [topicId, setTopicId] = useState(initialTopicId ?? '')
  const [stage, setStage] = useState<FoundationStage | 'all'>(initialStage ?? 'all')
  const [pendingQuestionId, setPendingQuestionId] = useState(initialQuestionId ?? '')
  const [currentIndex, setCurrentIndex] = useState(0)
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const progressItems = useProgressItemsStore()

  const topicsQuery = useQuery({
    queryKey: ['foundation-topics', 'foundation-v1'],
    queryFn: () => animeRepository.listFoundationTopics({
      curriculumVersion: 'foundation-v1',
      limit: 100,
    }),
  })
  const packsQuery = useQuery({
    queryKey: ['foundation-packs', 'foundation-v1'],
    queryFn: () => animeRepository.listFoundationPacks({
      curriculumVersion: 'foundation-v1',
      limit: 100,
    }),
  })

  const topics = topicsQuery.data?.items ?? []
  const packs = packsQuery.data?.items ?? []
  const topicById = useMemo(
    () => new Map(topics.map((topic) => [topic.id as string, topic])),
    [topics],
  )
  useEffect(() => {
    if (packs.length === 0) return
    if (!packId || !packs.some((pack) => pack.id === packId)) {
      const requested = initialPackId
        ? packs.find((pack) => pack.id === initialPackId)
        : undefined
      setPackId(requested?.id ?? packs[0].id)
    }
  }, [initialPackId, packId, packs])

  const questionsQuery = useInfiniteQuery({
    queryKey: ['foundation-questions', packId],
    initialPageParam: null as string | null,
    enabled: Boolean(packId),
    queryFn: ({ pageParam }) => animeRepository.listFoundationQuestions({
      curriculumVersion: 'foundation-v1',
      packId,
      cursor: pageParam ?? undefined,
      limit: 40,
    }),
    getNextPageParam: (lastPage) => lastPage.page.nextCursor,
  })

  const loadedQuestions = useMemo(
    () => questionsQuery.data?.pages.flatMap((page) => page.items) ?? [],
    [questionsQuery.data],
  )
  const packTopicIds = useMemo(
    () => new Set(loadedQuestions.map((question) => question.topicId as string)),
    [loadedQuestions],
  )
  const filteredTopics = useMemo(
    () => topics.filter((topic) => (
      packTopicIds.has(topic.id)
      && (domain === 'all' || topic.domain === domain)
    )),
    [domain, packTopicIds, topics],
  )
  const questions = useMemo(
    () => loadedQuestions.filter((question) => {
      if (domain !== 'all' && topicById.get(question.topicId)?.domain !== domain) return false
      if (topicId && question.topicId !== topicId) return false
      return stage === 'all' || question.stage === stage
    }),
    [domain, loadedQuestions, stage, topicById, topicId],
  )
  const persistedAnswers = useMemo(() => Object.fromEntries(
    Object.entries(progressItems)
      .filter(([, item]) => item.payload?.track === 'foundation')
      .map(([id, item]) => [
        id,
        typeof item.payload?.selected === 'string' ? item.payload.selected : '',
      ])
      .filter(([, selected]) => selected),
  ) as Record<string, string>, [progressItems])

  const currentQuestion = questions[currentIndex]
  const currentSelected = currentQuestion
    ? answers[currentQuestion.id] ?? persistedAnswers[currentQuestion.id] ?? ''
    : ''

  useEffect(() => {
    setCurrentIndex(0)
  }, [domain, packId, stage, topicId])

  useEffect(() => {
    if (!pendingQuestionId) return
    const targetIndex = questions.findIndex((question) => question.id === pendingQuestionId)
    if (targetIndex < 0) return
    setCurrentIndex(targetIndex)
    setPendingQuestionId('')
  }, [pendingQuestionId, questions])

  useEffect(() => {
    if (currentIndex >= questions.length) {
      setCurrentIndex(Math.max(questions.length - 1, 0))
    }
  }, [currentIndex, questions.length])

  useEffect(() => {
    if (
      questionsQuery.hasNextPage
      && !questionsQuery.isFetchingNextPage
    ) {
      void questionsQuery.fetchNextPage()
    }
  }, [
    questionsQuery.fetchNextPage,
    questionsQuery.hasNextPage,
    questionsQuery.isFetchingNextPage,
  ])

  async function answerQuestion(question: FoundationQuestion, selected: string) {
    setAnswers((current) => ({ ...current, [question.id]: selected }))
    await saveReviewState(
      question.id,
      selected === question.answer.optionId ? 'good' : 'bad',
      {
        itemType: 'exercise',
        payload: {
          track: 'foundation',
          packId: question.packId,
          topicId: question.topicId,
          label: question.promptZh,
          selected,
          answer: question.answer.optionId,
          domain: topicById.get(question.topicId)?.domain,
          stage: question.stage,
          questionType: question.questionType,
        },
      },
    )
  }

  async function goNext() {
    if (currentIndex < questions.length - 1) {
      setCurrentIndex((index) => index + 1)
      return
    }
    if (questionsQuery.hasNextPage && !questionsQuery.isFetchingNextPage) {
      const previousLength = questions.length
      await questionsQuery.fetchNextPage()
      setCurrentIndex(previousLength)
    }
  }

  function resetFilters() {
    setDomain('all')
    setTopicId('')
    setStage('all')
    setCurrentIndex(0)
  }

  return (
    <section className="page-stack">
      <section className="training-control-bar">
        <div className="segmented-control" aria-label="基础语言学视图">
          <button
            className={mode === 'train' ? 'selected' : ''}
            type="button"
            onClick={() => setMode('train')}
          >
            四阶训练
          </button>
          <button
            className={mode === 'catalog' ? 'selected' : ''}
            type="button"
            onClick={() => setMode('catalog')}
          >
            主题目录
          </button>
        </div>
        <div className="training-queue-summary">
          <span>基础语言学 V1</span>
          <strong>
            {topicsQuery.isLoading || packsQuery.isLoading
              ? '读取中'
              : topicsQuery.isError || packsQuery.isError
                ? '目录读取失败'
              : `${topics.length} 个主题 · ${packs.reduce((sum, pack) => sum + pack.questionCount, 0)} 题`}
          </strong>
        </div>
        <button className="icon-button secondary" type="button" onClick={resetFilters}>
          <RotateCcw size={18} />
          <span>重置筛选</span>
        </button>
      </section>

      {mode === 'catalog' ? (
        <FoundationTopicCatalog
          topics={topics}
          isLoading={topicsQuery.isLoading}
          isError={topicsQuery.isError}
          onRetry={() => void topicsQuery.refetch()}
        />
      ) : (
        <>
          <section className="linguistic-filter-panel training-filter-panel">
            <FoundationSelect
              id="foundation-pack"
              label="题包"
              value={packId}
              onChange={(value) => {
                setPackId(value)
                setTopicId('')
                setPendingQuestionId('')
              }}
              options={packs.map((pack) => ({
                value: pack.id,
                label: `${pack.batchNo}. ${pack.titleZh}`,
              }))}
              emptyLabel={packsQuery.isLoading ? '读取中' : '尚无已发布题包'}
            />
            <FoundationSelect
              id="foundation-domain"
              label="领域"
              value={domain}
              onChange={(value) => {
                setDomain(value as FoundationDomain | 'all')
                setTopicId('')
              }}
              options={[
                { value: 'all', label: '全部领域' },
                ...Object.entries(foundationDomainLabels).map(([value, label]) => ({ value, label })),
              ]}
            />
            <FoundationSelect
              id="foundation-topic"
              label="主题"
              value={topicId}
              onChange={setTopicId}
              options={[
                { value: '', label: '全部主题' },
                ...filteredTopics.map((topic) => ({ value: topic.id, label: topic.titleZh })),
              ]}
            />
            <FoundationSelect
              id="foundation-stage"
              label="阶段"
              value={stage}
              onChange={(value) => setStage(value as FoundationStage | 'all')}
              options={[
                { value: 'all', label: 'F1–F4 全部' },
                ...Object.entries(foundationStageLabels).map(([value, label]) => ({ value, label })),
              ]}
            />
          </section>

          {topicsQuery.isError || packsQuery.isError ? (
            <div className="source-preview">
              基础语言学目录读取失败。
              <button type="button" onClick={() => {
                void topicsQuery.refetch()
                void packsQuery.refetch()
              }}>
                重试
              </button>
            </div>
          ) : null}
          {questionsQuery.isError ? (
            <div className="source-preview">
              基础语言学题目读取失败。
              <button type="button" onClick={() => void questionsQuery.refetch()}>重试</button>
            </div>
          ) : null}
          {questionsQuery.isLoading ? <div className="source-preview">正在读取基础语言学题目…</div> : null}
          {!packsQuery.isError && !questionsQuery.isLoading && packs.length === 0 ? (
            <div className="source-preview">
              主题目录已经发布；题包只有在完成独立逐题复核后才会出现在这里。
            </div>
          ) : null}
          {!questionsQuery.isError
            && !questionsQuery.isLoading
            && !questionsQuery.isFetchingNextPage
            && !questionsQuery.hasNextPage
            && packId
            && questions.length === 0 ? (
            <div className="source-preview">当前筛选下没有已发布题目。</div>
          ) : null}

          {currentQuestion ? (
            <FoundationQuestionCard
              question={currentQuestion}
              topic={topicById.get(currentQuestion.topicId)}
              selectedOptionId={currentSelected}
              currentIndex={currentIndex}
              loadedTotal={questions.length}
              hasMore={Boolean(questionsQuery.hasNextPage)}
              isFetchingMore={questionsQuery.isFetchingNextPage}
              onSelect={(optionId) => void answerQuestion(currentQuestion, optionId)}
              onPrevious={() => setCurrentIndex((index) => Math.max(0, index - 1))}
              onNext={() => void goNext()}
              onRetry={() => setAnswers((current) => ({ ...current, [currentQuestion.id]: '' }))}
            />
          ) : null}
        </>
      )}
    </section>
  )
}

function FoundationSelect({
  id,
  label,
  value,
  options,
  emptyLabel,
  onChange,
}: {
  id: string
  label: string
  value: string
  options: { value: string; label: string }[]
  emptyLabel?: string
  onChange: (value: string) => void
}) {
  return (
    <div className="filter-field">
      <label htmlFor={id}>{label}</label>
      <select id={id} value={value} onChange={(event) => onChange(event.target.value)}>
        {options.length === 0 ? <option value="">{emptyLabel ?? '暂无选项'}</option> : null}
        {options.map((option) => (
          <option key={option.value || 'all'} value={option.value}>{option.label}</option>
        ))}
      </select>
    </div>
  )
}

function FoundationTopicCatalog({
  topics,
  isLoading,
  isError,
  onRetry,
}: {
  topics: FoundationTopic[]
  isLoading: boolean
  isError: boolean
  onRetry: () => void
}) {
  if (isLoading) return <div className="source-preview">正在读取主题目录…</div>
  if (isError) {
    return (
      <div className="source-preview">
        基础语言学主题目录读取失败。
        <button type="button" onClick={onRetry}>重试</button>
      </div>
    )
  }
  return (
    <div className="linguistic-card-grid">
      {topics.map((topic) => (
        <article className="linguistic-card" key={topic.id}>
          <div className="linguistic-card-meta">
            <span>{foundationDomainLabels[topic.domain]}</span>
            <span>{topic.titleJa}</span>
            <span>质量 {topic.qualityScore}</span>
          </div>
          <h2>{topic.titleZh}</h2>
          <p>{topic.shortDefinitionZh}</p>
          <details>
            <summary>学习说明与四阶目标</summary>
            <p>{topic.beginnerExplanationZh}</p>
            <p>{topic.deepExplanationZh}</p>
            <p><strong>注意：</strong>{topic.cautionNoteZh}</p>
            <ol>
              <li>{topic.learningObjectives.F1Zh}</li>
              <li>{topic.learningObjectives.F2Zh}</li>
              <li>{topic.learningObjectives.F3Zh}</li>
              <li>{topic.learningObjectives.F4Zh}</li>
            </ol>
          </details>
        </article>
      ))}
    </div>
  )
}

function FoundationQuestionCard({
  question,
  topic,
  selectedOptionId,
  currentIndex,
  loadedTotal,
  hasMore,
  isFetchingMore,
  onSelect,
  onPrevious,
  onNext,
  onRetry,
}: {
  question: FoundationQuestion
  topic?: FoundationTopic
  selectedOptionId: string
  currentIndex: number
  loadedTotal: number
  hasMore: boolean
  isFetchingMore: boolean
  onSelect: (optionId: string) => void
  onPrevious: () => void
  onNext: () => void
  onRetry: () => void
}) {
  const answered = Boolean(selectedOptionId)
  const isCorrect = selectedOptionId === question.answer.optionId
  const correctOption = question.options.find((option) => option.id === question.answer.optionId)
  return (
    <article className="linguistic-trainer-card">
      <header className="trainer-progress-row">
        <div>
          <p className="eyebrow">{foundationStageLabels[question.stage]}</p>
          <strong>已加载第 {currentIndex + 1} / {loadedTotal} 题{hasMore ? ' · 后续可继续加载' : ''}</strong>
        </div>
        <span className="review-chip">{topic?.titleZh ?? question.topicId}</span>
      </header>

      <div className="linguistic-card-meta">
        <span>{topic ? foundationDomainLabels[topic.domain] : '基础语言学'}</span>
        <span>{question.questionType}</span>
        <span>质量 {question.qualityScore}</span>
      </div>

      <div className="trainer-workspace">
        <section className="trainer-context-panel">
          <FoundationStimulusView stimulus={question.stimulus} />
          {!answered ? <p className="source-preview"><strong>提示：</strong>{question.hintZh}</p> : null}
        </section>
        <section className="trainer-question-panel">
          <h2>{question.promptZh}</h2>
          <div className="linguistic-options" role="radiogroup" aria-label={question.promptZh}>
            {question.options.map((option) => {
              const isSelected = selectedOptionId === option.id
              const isAnswer = answered && option.id === question.answer.optionId
              const isWrong = answered && isSelected && !isAnswer
              return (
                <button
                  className={[
                    'choice-option',
                    isSelected ? 'selected' : '',
                    isAnswer ? 'correct' : '',
                    isWrong ? 'wrong' : '',
                  ].filter(Boolean).join(' ')}
                  type="button"
                  key={option.id}
                  disabled={answered}
                  onClick={() => onSelect(option.id)}
                >
                  <strong>{option.id}</strong>　{option.text}
                </button>
              )
            })}
          </div>

          {answered ? (
            <section className={isCorrect ? 'linguistic-answer correct' : 'linguistic-answer wrong'}>
              <div className="answer-status-row">
                <div>
                  <p className="eyebrow">你的选择</p>
                  <strong>{selectedOptionId}</strong>
                </div>
                <div>
                  <p className="eyebrow">正确答案</p>
                  <strong>{question.answer.optionId} · {correctOption?.text}</strong>
                </div>
              </div>
              {!isCorrect ? (
                <p><strong>本项为什么不成立：</strong>{question.wrongExplanations[selectedOptionId]}</p>
              ) : null}
              <p><strong>基础说明：</strong>{question.explanationZh}</p>
              <p><strong>深入解释：</strong>{question.deepExplanationZh}</p>
              <p><strong>注意：</strong>{question.cautionNoteZh}</p>
              {question.transferExampleJa ? (
                <p>
                  <strong>迁移例：</strong>{question.transferExampleJa}
                  <br />
                  {question.transferExplanationZh}
                </p>
              ) : null}
            </section>
          ) : null}
        </section>
      </div>

      <footer className="trainer-actions">
        <button className="icon-button secondary" type="button" onClick={onPrevious} disabled={currentIndex === 0}>
          <ChevronLeft size={18} />
          <span>上一题</span>
        </button>
        <button className="icon-button secondary" type="button" onClick={onRetry} disabled={!answered}>
          <RotateCcw size={18} />
          <span>重做本题</span>
        </button>
        <button
          className="primary-action"
          type="button"
          onClick={onNext}
          disabled={!answered || (currentIndex >= loadedTotal - 1 && !hasMore) || isFetchingMore}
        >
          <span>{isFetchingMore ? '加载中' : currentIndex >= loadedTotal - 1 && hasMore ? '加载下一页' : '下一题'}</span>
          <ChevronRight size={18} />
        </button>
      </footer>
    </article>
  )
}

function FoundationStimulusView({ stimulus }: { stimulus: FoundationStimulus }) {
  if (stimulus.kind === 'sentence') {
    return (
      <div className="dialogue-evidence">
        <header className="dialogue-evidence-header"><span>原创例句</span></header>
        <div className="dialogue-lines"><strong lang="ja">{stimulus.jaText}</strong></div>
        {stimulus.zhContext ? <p>{stimulus.zhContext}</p> : null}
      </div>
    )
  }
  if (stimulus.kind === 'dialogue') {
    return (
      <div className="dialogue-evidence">
        <header className="dialogue-evidence-header"><span>构造对话</span></header>
        <div className="dialogue-lines">
          {stimulus.turns.map((turn, index) => (
            <div className="dialogue-line" key={`${turn.speaker ?? 'turn'}-${index}`}>
              <b>{turn.speaker ?? `话轮 ${index + 1}`}</b>
              <strong lang="ja">{turn.jaText}</strong>
              {turn.zhText ? <span>{turn.zhText}</span> : null}
            </div>
          ))}
        </div>
        {stimulus.zhContext ? <p>{stimulus.zhContext}</p> : null}
      </div>
    )
  }
  if (stimulus.kind === 'contrast') {
    return (
      <div className="dialogue-evidence">
        <header className="dialogue-evidence-header"><span>对照材料</span></header>
        <div className="dialogue-lines">
          {stimulus.items.map((item, index) => (
            <div className="dialogue-line" key={`${item.label ?? 'item'}-${index}`}>
              <b>{item.label ?? String(index + 1)}</b>
              <strong>{item.text}</strong>
              {item.noteZh ? <span>{item.noteZh}</span> : null}
            </div>
          ))}
        </div>
        {stimulus.zhContext ? <p>{stimulus.zhContext}</p> : null}
      </div>
    )
  }
  return (
    <div className="source-preview">
      <BookOpen size={18} aria-hidden="true" />
      <span>
        {stimulus.form ? <><strong>{stimulus.form}</strong><br /></> : null}
        {stimulus.descriptionZh}
      </span>
    </div>
  )
}
