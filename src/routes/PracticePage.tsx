import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { useEffect, useMemo, useState } from 'react'
import { HandwritingCanvas } from '../components/HandwritingCanvas'
import { PageHeader } from '../components/PageHeader'
import { ReviewButton } from '../components/ReviewButton'
import { TtsButton } from '../components/TtsButton'
import { formatEpisodeLabel } from '../lib/episodeLabels'
import { saveReviewState, useProgressStore } from '../lib/progress'
import { animeRepository } from '../server/repositories/animeRepository'

export function PracticePage() {
  const { workSlug, episode } = useParams({ strict: false })
  const episodeNo = Number(episode ?? 1)
  const [showAnswer, setShowAnswer] = useState(false)
  const exercisesQuery = useQuery({
    queryKey: ['exercises', workSlug, episodeNo],
    queryFn: () => animeRepository.listEpisodeExercises(workSlug ?? 'k-on', episodeNo),
  })
  const progressStore = useProgressStore()
  const exercises = useMemo(
    () => (exercisesQuery.data ?? []).filter((item) => !progressStore[item.id]),
    [exercisesQuery.data, progressStore],
  )
  const [index, setIndex] = useState(0)
  const exercise = exercises[index]

  useEffect(() => {
    setIndex((current) => Math.min(current, Math.max(exercises.length - 1, 0)))
  }, [exercises.length])

  function next() {
    setShowAnswer(false)
    setIndex((current) => Math.min(current + 1, exercises.length - 1))
  }

  async function markKnown() {
    if (!exercise) return
    await saveReviewState(exercise.id, 'known', {
      itemType: 'exercise',
      workSlug,
      episode: episodeNo,
      payload: { label: exercise.answer, prompt: exercise.prompt },
    })
    setShowAnswer(false)
    setIndex((current) => Math.min(current, Math.max(exercises.length - 2, 0)))
  }

  return (
    <section className="page-stack">
      <PageHeader eyebrow="手写练习" title={`${formatEpisodeLabel(workSlug, episodeNo)} Canvas`} />
      <div className="practice-layout">
        <aside className="prompt-panel">
          <p className="eyebrow">题目</p>
          {exercise ? <span className="review-chip">{index + 1} / {exercises.length}</span> : null}
          <h2>{exercise?.prompt ?? '暂无题目'}</h2>
          <p>{exercise?.hint}</p>
          {showAnswer ? <strong className="answer-text">{exercise?.answer}</strong> : null}
          <div className="card-actions">
            {exercise?.answer ? <TtsButton text={exercise.answer} /> : null}
            <button className="icon-button secondary" type="button" onClick={() => setShowAnswer((value) => !value)}>
              查看答案
            </button>
            {exercise ? (
              <>
                <button className="icon-button secondary" type="button" disabled={index >= exercises.length - 1} onClick={next}>
                  下一题
                </button>
                <button className="icon-button secondary" type="button" onClick={() => void markKnown()}>
                  我学过了
                </button>
              </>
            ) : null}
          </div>
          <div className="self-check">
            {exercise ? (
              <>
                <ReviewButton itemId={exercise.id} state="good" itemType="exercise" workSlug={workSlug} episode={episodeNo} payload={{ label: exercise.answer }}>写对了</ReviewButton>
                <ReviewButton itemId={exercise.id} state="ok" itemType="exercise" workSlug={workSlug} episode={episodeNo} payload={{ label: exercise.answer }}>模糊</ReviewButton>
                <ReviewButton itemId={exercise.id} state="bad" itemType="exercise" workSlug={workSlug} episode={episodeNo} payload={{ label: exercise.answer }}>写错了</ReviewButton>
              </>
            ) : null}
          </div>
        </aside>
        <HandwritingCanvas />
      </div>
    </section>
  )
}
