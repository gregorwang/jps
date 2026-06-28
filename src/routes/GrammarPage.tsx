import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from '@tanstack/react-router'
import { AiExplainButton } from '../components/AiExplainButton'
import { EpisodeScopeSelector } from '../components/EpisodeScopeSelector'
import { PageHeader } from '../components/PageHeader'
import { formatEpisodeLabel } from '../lib/episodeLabels'
import { readEpisodeScope } from '../lib/episodeScope'
import { animeRepository } from '../server/repositories/animeRepository'

export function GrammarPage() {
  const { workSlug, episode } = useParams({ strict: false })
  const fallbackScope = readEpisodeScope()
  const selectedWorkSlug = workSlug ?? fallbackScope.workSlug
  const episodeNo = Number(episode ?? fallbackScope.episode)
  const grammarQuery = useQuery({
    queryKey: ['grammar', selectedWorkSlug, episodeNo],
    queryFn: () => animeRepository.listEpisodeGrammar(selectedWorkSlug, episodeNo),
  })

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="语法资料"
        title={`${formatEpisodeLabel(selectedWorkSlug, episodeNo)} 本集语法`}
        description="这里用于查看语法点、字幕例句、语气说明和 AI 精讲。正式训练进入综合训练队列完成。"
        actions={(
          <Link className="primary-action" to="/works/$workSlug/episodes/$episode/lesson" params={{ workSlug: selectedWorkSlug, episode: String(episodeNo) }}>
            开始综合训练
          </Link>
        )}
      />
      <EpisodeScopeSelector workSlug={selectedWorkSlug} episode={episodeNo} tool="grammar" />
      <div className="card-list">
        {grammarQuery.data?.map((point) => (
          <article className="learning-card" key={point.id}>
            <div>
              <h2>{point.pattern}</h2>
              <p className="kana">{[point.functionZh, point.difficulty].filter(Boolean).join(' · ')}</p>
              <blockquote>{point.jaExample}</blockquote>
              <p>{point.explanationZh}</p>
              <small>{point.pragmaticsNote} {point.realWorldNote}</small>
            </div>
            <div className="card-actions">
              <AiExplainButton kind="grammar" text={point.pattern} context={`${point.jaExample}\n${point.explanationZh}`} />
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}
