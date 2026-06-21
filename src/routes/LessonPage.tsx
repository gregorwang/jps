import { useQuery } from '@tanstack/react-query'
import { Link, useParams, useRouterState } from '@tanstack/react-router'
import { useMemo } from 'react'
import { LessonPlayer } from '../components/LessonPlayer'
import { buildEpisodeLesson, type LessonMode, type LessonTarget } from '../lib/lesson'
import { useProgressItemsStore } from '../lib/progress'
import { animeRepository } from '../server/repositories/animeRepository'

export function LessonPage() {
  const { workSlug, episode } = useParams({ strict: false })
  const selectedWorkSlug = workSlug ?? 'k-on'
  const episodeNo = Number(episode ?? 1)
  const search = useRouterState({ select: (state) => state.location.search })
  const progressItems = useProgressItemsStore()
  const { mode, target, batch } = parseLessonSearch(search)

  const materialsQuery = useQuery({
    queryKey: ['episode-lesson-materials', selectedWorkSlug, episodeNo],
    queryFn: async () => {
      const [vocab, grammar, sentences] = await Promise.all([
        animeRepository.listEpisodeVocab(selectedWorkSlug, episodeNo),
        animeRepository.listEpisodeGrammar(selectedWorkSlug, episodeNo),
        animeRepository.listEpisodeSentences(selectedWorkSlug, episodeNo),
      ])
      return { vocab, grammar, sentences }
    },
  })

  const lesson = useMemo(() => {
    if (!materialsQuery.data) return null
    return buildEpisodeLesson({
      workSlug: selectedWorkSlug,
      episode: episodeNo,
      mode,
      batch,
      target,
      progressItems,
      ...materialsQuery.data,
    })
  }, [batch, episodeNo, materialsQuery.data, mode, progressItems, selectedWorkSlug, target])

  if (materialsQuery.isLoading) {
    return (
      <section className="lesson-shell">
        <div className="lesson-loading">训练队列加载中。</div>
      </section>
    )
  }

  if (materialsQuery.isError || !lesson || lesson.nodes.length === 0) {
    const fallbackActions = buildFallbackActions(mode, Boolean(target))
    return (
      <section className="lesson-shell">
        <div className="lesson-complete">
          <p className="eyebrow">暂无训练队列</p>
          <h1>本集暂时没有足够材料生成普通练习。</h1>
          <div className="lesson-complete-actions">
            {fallbackActions.map((action) => (
              <Link
                className="secondary-action"
                key={action.label}
                to="/works/$workSlug/episodes/$episode/lesson"
                search={action.search}
                params={{ workSlug: selectedWorkSlug, episode: String(episodeNo) }}
              >
                {action.label}
              </Link>
            ))}
          </div>
          <Link
            className="primary-action"
            to="/works/$workSlug/episodes/$episode"
            params={{ workSlug: selectedWorkSlug, episode: String(episodeNo) }}
          >
            返回单集页
          </Link>
        </div>
      </section>
    )
  }

  return <LessonPlayer lesson={lesson} />
}

function parseLessonSearch(search: Record<string, unknown>): { mode: LessonMode; batch: number; target?: LessonTarget } {
  const modeValue = typeof search.mode === 'string' ? search.mode : ''
  const mode = isLessonMode(modeValue) ? modeValue : 'mixed'
  const rawBatch = typeof search.batch === 'string' ? Number(search.batch) : typeof search.batch === 'number' ? search.batch : 1
  const batch = Number.isFinite(rawBatch) && rawBatch > 0 ? Math.floor(rawBatch) : 1
  const targetKind = typeof search.targetKind === 'string' ? search.targetKind : ''
  const targetId = typeof search.targetId === 'string' ? search.targetId : ''
  const target = targetId && ['vocab', 'grammar', 'sentence'].includes(targetKind)
    ? { kind: targetKind as LessonTarget['kind'], id: targetId }
    : undefined
  return { mode: target ? 'target' : mode, batch, target }
}

function isLessonMode(value: string): value is LessonMode {
  return ['mixed', 'vocab', 'grammar', 'shadowing', 'review', 'target'].includes(value)
}

function buildFallbackActions(mode: LessonMode, hasTarget: boolean) {
  if (hasTarget) return [
    { label: '综合训练', search: { mode: 'mixed' } },
    { label: '词汇专项', search: { mode: 'vocab' } },
    { label: '语法专项', search: { mode: 'grammar' } },
  ]
  return [
    { label: mode === 'mixed' ? '词汇专项' : '综合训练', search: { mode: mode === 'mixed' ? 'vocab' : 'mixed' } },
    { label: '语法专项', search: { mode: 'grammar' } },
    { label: '跟读前置', search: { mode: 'shadowing' } },
  ].filter((action) => action.search.mode !== mode)
}
