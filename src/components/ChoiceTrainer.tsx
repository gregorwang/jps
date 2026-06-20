import { CheckCircle2, HelpCircle, RotateCcw, XCircle } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { AiExplainButton } from './AiExplainButton'
import { TtsButton } from './TtsButton'
import { saveReviewState, useProgressStore } from '../lib/progress'

export type ChoiceQuestion = {
  id: string
  itemType: 'vocab' | 'grammar' | 'sentence' | 'exercise'
  workSlug?: string
  episode?: number
  kicker: string
  prompt: string
  body?: string
  choices: string[]
  answer: string
  explanation: string
  reviewLabel: string
  listenText?: string
  ai?: {
    kind: 'vocab' | 'grammar' | 'sentence'
    text: string
    context?: string
  }
}

type ChoiceTrainerProps = {
  questions: ChoiceQuestion[]
  emptyText?: string
}

export function ChoiceTrainer({ questions, emptyText = '暂无可训练题目。' }: ChoiceTrainerProps) {
  const [index, setIndex] = useState(0)
  const [selected, setSelected] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [pinnedQuestionId, setPinnedQuestionId] = useState<string | null>(null)
  const [stats, setStats] = useState({ correct: 0, total: 0 })
  const progressStore = useProgressStore()
  const activeQuestions = useMemo(
    () => questions.filter((question) => !progressStore[question.id] || question.id === pinnedQuestionId),
    [pinnedQuestionId, progressStore, questions],
  )
  const question = activeQuestions[index]

  useEffect(() => {
    setIndex((current) => Math.min(current, Math.max(activeQuestions.length - 1, 0)))
  }, [activeQuestions.length])

  const choices = useMemo(() => {
    if (!question) return []
    return stableShuffle(question.choices, question.id)
  }, [question])

  if (!question) {
    return <div className="source-preview">{questions.length > 0 ? '这一组题都已标记为学过。' : emptyText}</div>
  }

  const isCorrect = selected === question.answer
  const complete = stats.total > 0 && index >= activeQuestions.length - 1 && submitted

  async function submit() {
    if (!question || !selected) return
    const correct = selected === question.answer
    setPinnedQuestionId(question.id)
    setSubmitted(true)
    setStats((current) => ({
      correct: current.correct + (correct ? 1 : 0),
      total: current.total + 1,
    }))
    await saveReviewState(question.id, correct ? 'good' : 'bad', {
      itemType: question.itemType,
      workSlug: question.workSlug,
      episode: question.episode,
      payload: {
        label: question.reviewLabel,
        prompt: question.prompt,
        answer: question.answer,
        selected,
      },
    })
  }

  function next() {
    setSelected('')
    setSubmitted(false)
    setPinnedQuestionId(null)
    setIndex((current) => Math.min(current, Math.max(activeQuestions.length - 2, 0)))
  }

  async function markKnown() {
    if (!question) return
    await saveReviewState(question.id, 'known', {
      itemType: question.itemType,
      workSlug: question.workSlug,
      episode: question.episode,
      payload: {
        label: question.reviewLabel,
        prompt: question.prompt,
        answer: question.answer,
      },
    })
    setSelected('')
    setSubmitted(false)
    setPinnedQuestionId(null)
    setIndex((current) => Math.min(current, Math.max(activeQuestions.length - 2, 0)))
  }

  function restartWeak() {
    setSelected('')
    setSubmitted(false)
    setPinnedQuestionId(null)
    setIndex(0)
    setStats({ correct: 0, total: 0 })
  }

  return (
    <article className="quiz-card">
      <div className="quiz-progress">
        <span>{index + 1} / {activeQuestions.length}</span>
        <strong>正确 {stats.correct} / 已答 {stats.total}</strong>
      </div>
      <p className="eyebrow">{question.kicker}</p>
      <h2>{question.prompt}</h2>
      {question.body ? <blockquote>{question.body}</blockquote> : null}
      {question.listenText ? <TtsButton text={question.listenText} label="听题" /> : null}
      <div className="choice-grid" role="radiogroup" aria-label="选项">
        {choices.map((choice) => {
          const isAnswer = submitted && choice === question.answer
          const isWrongPick = submitted && choice === selected && choice !== question.answer
          return (
            <button
              className={[
                'choice-option',
                selected === choice ? 'selected' : '',
                isAnswer ? 'correct' : '',
                isWrongPick ? 'wrong' : '',
              ].filter(Boolean).join(' ')}
              type="button"
              key={choice}
              disabled={submitted}
              onClick={() => setSelected(choice)}
            >
              {choice}
            </button>
          )
        })}
      </div>
      <div className="quiz-actions">
        {!submitted ? (
          <button className="primary-action" type="button" disabled={!selected} onClick={() => void submit()}>
            提交答案
          </button>
        ) : (
          <button className="primary-action" type="button" disabled={complete} onClick={next}>
            下一题
          </button>
        )}
        {complete ? (
          <button className="icon-button secondary" type="button" onClick={restartWeak}>
            <RotateCcw size={18} />
            <span>再练一轮</span>
          </button>
        ) : null}
        <button className="icon-button secondary" type="button" onClick={() => void markKnown()}>
          我学过了
        </button>
      </div>
      {submitted ? (
        <section className={isCorrect ? 'quiz-feedback correct' : 'quiz-feedback wrong'}>
          <div className="quiz-feedback-heading">
            {isCorrect ? <CheckCircle2 size={20} /> : <XCircle size={20} />}
            <strong>{isCorrect ? '答对了' : '答错了，已进入回炉队列'}</strong>
          </div>
          <p>{question.explanation}</p>
          {!isCorrect && question.ai ? (
            <AiExplainButton
              kind={question.ai.kind}
              text={question.ai.text}
              context={`${question.ai.context ?? ''}\n题目：${question.prompt}\n你的答案：${selected}\n正确答案：${question.answer}`}
              label="为什么错"
              icon={<HelpCircle size={18} />}
            />
          ) : null}
        </section>
      ) : null}
    </article>
  )
}

function stableShuffle<T>(items: T[], seed: string) {
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
