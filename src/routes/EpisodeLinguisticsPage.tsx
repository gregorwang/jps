import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { ChevronRight } from 'lucide-react'
import { useMemo, useState } from 'react'
import { EpisodeScopeSelector } from '../components/EpisodeScopeSelector'
import { PageHeader } from '../components/PageHeader'
import { formatEpisodeLabel } from '../lib/episodeLabels'
import type { LinguisticExerciseDraft, LinguisticExerciseOption } from '../lib/types'
import { animeRepository } from '../server/repositories/animeRepository'

const domainFilters = ['all', 'syntax', 'morphology', 'pragmatics', 'historical', 'phonology', 'sociolinguistics'] as const

const domainLabels: Record<string, string> = {
  all: '全部',
  syntax: 'syntax',
  morphology: 'morphology',
  pragmatics: 'pragmatics',
  historical: 'historical',
  phonology: 'phonology',
  sociolinguistics: 'sociolinguistics',
}

export function EpisodeLinguisticsPage() {
  const { workSlug, episode } = useParams({ strict: false })
  const selectedWorkSlug = workSlug ?? 're-zero'
  const episodeNo = Number(episode ?? 1)
  const [domain, setDomain] = useState<(typeof domainFilters)[number]>('all')
  const [index, setIndex] = useState(0)
  const [selectedKey, setSelectedKey] = useState('')
  const [submitted, setSubmitted] = useState(false)

  const episodeQuery = useQuery({
    queryKey: ['episode', selectedWorkSlug, episodeNo],
    queryFn: () => animeRepository.getEpisode(selectedWorkSlug, episodeNo),
  })
  const exercisesQuery = useQuery({
    queryKey: ['episode-linguistics', selectedWorkSlug, episodeNo],
    queryFn: () => animeRepository.listEpisodeLinguisticExercises(selectedWorkSlug, episodeNo),
  })

  const exercises = useMemo(() => sortExercises(exercisesQuery.data ?? []), [exercisesQuery.data])
  const filteredExercises = useMemo(
    () => exercises.filter((exercise) => domain === 'all' || exercise.domain === domain),
    [domain, exercises],
  )
  const currentIndex = Math.min(index, Math.max(filteredExercises.length - 1, 0))
  const exercise = filteredExercises[currentIndex]
  const options = exercise ? getOptions(exercise) : []
  const correctKey = exercise ? getCorrectKey(exercise, options) : ''
  const selectedOption = options.find((option) => option.key === selectedKey)
  const correctOption = options.find((option) => option.key === correctKey)
  const isCorrect = submitted && Boolean(selectedKey) && selectedKey === correctKey
  const draftCount = exercises.filter((item) => item.status === 'draft').length

  function resetFor(nextIndex: number, nextDomain = domain) {
    setIndex(nextIndex)
    setDomain(nextDomain)
    setSelectedKey('')
    setSubmitted(false)
  }

  function goNext() {
    const nextIndex = Math.min(currentIndex + 1, filteredExercises.length - 1)
    resetFor(nextIndex)
  }

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow={episodeQuery.data?.workDisplayName ?? selectedWorkSlug}
        title={`${formatEpisodeLabel(selectedWorkSlug, episodeNo)} 语言学专项训练`}
        description="独立读取 linguistic_exercise_drafts。AI 出题保存后会直接写入数据库题库。"
      />
      <EpisodeScopeSelector workSlug={selectedWorkSlug} episode={episodeNo} tool="linguistics" />

      <section className="linguistics-overview">
        <div>
          <span>本集题目</span>
          <strong>{exercisesQuery.isLoading ? '-' : exercises.length}</strong>
        </div>
        <div>
          <span>当前进度</span>
          <strong>{filteredExercises.length > 0 ? `${currentIndex + 1} / ${filteredExercises.length}` : '0 / 0'}</strong>
        </div>
        <div>
          <span>Draft 可见</span>
          <strong>{draftCount > 0 ? `${draftCount} 题` : '未显示'}</strong>
        </div>
      </section>

      <div className="segmented-control linguistics-domain-filter" aria-label="语言学分类筛选">
        {domainFilters.map((item) => (
          <button
            className={domain === item ? 'selected' : ''}
            type="button"
            key={item}
            onClick={() => resetFor(0, item)}
          >
            {domainLabels[item]}
          </button>
        ))}
      </div>

      {exercisesQuery.isError ? (
        <div className="source-preview error-panel">
          <strong>语言学专项题读取失败。</strong>
          <span>请稍后重试，详细错误已输出到控制台。</span>
        </div>
      ) : null}

      {!exercisesQuery.isLoading && exercises.length === 0 && !exercisesQuery.isError ? (
        <div className="source-preview">本集语言学专项题暂未生成。</div>
      ) : null}

      {!exercisesQuery.isLoading && exercises.length > 0 && filteredExercises.length === 0 ? (
        <div className="source-preview">当前分类下没有语言学专项题。</div>
      ) : null}

      {exercise ? (
        <article className="linguistic-trainer-card episode-linguistics-card">
          <header className="trainer-progress-row">
            <div>
              <p className="eyebrow">语言学专项</p>
              <strong>第 {currentIndex + 1} / {filteredExercises.length} 题</strong>
            </div>
            <span className="review-chip">{exercise.status ?? 'published'}</span>
          </header>

          <div className="linguistic-card-meta">
            <span>{exercise.domain}</span>
            <span>{exercise.phenomenonNameZh || exercise.phenomenonKey}</span>
            <span>{exercise.difficulty}</span>
            <span>{sourceLabel(exercise)}</span>
          </div>

          {exercise.phenomenonDefinitionZh ? (
            <p className="phenomenon-definition">{exercise.phenomenonDefinitionZh}</p>
          ) : null}

          <section className="dialogue-block">
            <p className="eyebrow">日文原句 / 片段</p>
            <p className="episode-linguistics-ja">{exercise.jaText || '未提供日文原句'}</p>
            {exercise.zhText ? <p className="episode-linguistics-zh">{exercise.zhText}</p> : null}
          </section>

          <section className="question-block">
            <h2>{exercise.prompt}</h2>
            {exercise.hint ? <p>{exercise.hint}</p> : null}
          </section>

          <div className="linguistic-options" role="radiogroup" aria-label={exercise.prompt}>
            {options.map((option) => (
              <button
                className={[
                  'choice-option',
                  selectedKey === option.key ? 'selected' : '',
                  submitted && option.key === correctKey ? 'correct' : '',
                  submitted && selectedKey === option.key && option.key !== correctKey ? 'wrong' : '',
                ].filter(Boolean).join(' ')}
                type="button"
                key={option.key}
                disabled={submitted}
                onClick={() => setSelectedKey(option.key)}
              >
                <b>{option.key}</b>
                <span>{option.label}</span>
              </button>
            ))}
          </div>

          {submitted ? (
            <section className={isCorrect ? 'linguistic-answer correct' : 'linguistic-answer wrong'}>
              <div className="answer-status-row">
                <div>
                  <p className="eyebrow">结果</p>
                  <strong>{isCorrect ? '正确' : '错误'}</strong>
                </div>
                <div>
                  <p className="eyebrow">正确答案</p>
                  <strong>{correctOption?.label ?? exercise.answer.answerZh}</strong>
                </div>
              </div>
              <ExplanationLine label="你的选择" value={selectedOption?.label} />
              <ExplanationLine label="答案理由" value={exercise.answer.rationaleZh} />
              <ExplanationLine label="基础说明" value={exercise.basicExplanationZh} />
              <ExplanationLine label="深入解释" value={exercise.deepExplanationZh} />
              <ExplanationLine label="中文参考" value={exercise.zhText} />
              <ExplanationLine label="动画语境" value={exercise.animeContextNoteZh} />
              <ExplanationLine label="注意事项" value={exercise.cautionNoteZh} />
            </section>
          ) : null}

          <footer className="trainer-actions">
            <button className="icon-button secondary" type="button" onClick={() => resetFor(Math.max(currentIndex - 1, 0))} disabled={currentIndex === 0}>
              上一题
            </button>
            <button className="primary-action" type="button" disabled={!selectedKey || submitted} onClick={() => setSubmitted(true)}>
              提交
            </button>
            <button className="icon-button secondary" type="button" disabled={currentIndex >= filteredExercises.length - 1} onClick={goNext}>
              <span>下一题</span>
              <ChevronRight size={18} />
            </button>
          </footer>
        </article>
      ) : null}
    </section>
  )
}

function sortExercises(exercises: LinguisticExerciseDraft[]) {
  return [...exercises].sort((a, b) => {
    const lineA = a.sourceLineNo ?? Number.MAX_SAFE_INTEGER
    const lineB = b.sourceLineNo ?? Number.MAX_SAFE_INTEGER
    if (lineA !== lineB) return lineA - lineB
    return a.id.localeCompare(b.id)
  })
}

function getOptions(exercise: LinguisticExerciseDraft): LinguisticExerciseOption[] {
  if (exercise.optionItems?.length) return exercise.optionItems
  return exercise.options.map((label, index) => ({ key: String(index), label }))
}

function getCorrectKey(exercise: LinguisticExerciseDraft, options: LinguisticExerciseOption[]) {
  if (exercise.answer.correctKey) return exercise.answer.correctKey
  if (typeof exercise.answer.correctIndex === 'number') return String(exercise.answer.correctIndex)
  return options.find((option) => option.label === exercise.answer.answerZh)?.key ?? ''
}

function sourceLabel(exercise: LinguisticExerciseDraft) {
  const parts = [
    exercise.sourceLineNo ? `line ${exercise.sourceLineNo}` : '',
    exercise.sourceId || '',
    exercise.batchId || '',
  ].filter(Boolean)
  return parts.join(' · ') || 'source --'
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
