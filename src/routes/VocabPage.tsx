import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from '@tanstack/react-router'
import { EpisodeScopeSelector } from '../components/EpisodeScopeSelector'
import { PageHeader } from '../components/PageHeader'
import { AiExplainButton } from '../components/AiExplainButton'
import { TtsButton } from '../components/TtsButton'
import { formatEpisodeLabel } from '../lib/episodeLabels'
import { readEpisodeScope } from '../lib/episodeScope'
import { animeRepository } from '../server/repositories/animeRepository'

export function VocabPage() {
  const { workSlug, episode } = useParams({ strict: false })
  const fallbackScope = readEpisodeScope()
  const selectedWorkSlug = workSlug ?? fallbackScope.workSlug
  const episodeNo = Number(episode ?? fallbackScope.episode)
  const vocabQuery = useQuery({
    queryKey: ['vocab', selectedWorkSlug, episodeNo],
    queryFn: () => animeRepository.listEpisodeVocab(selectedWorkSlug, episodeNo),
  })

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="词汇资料"
        title={`${formatEpisodeLabel(selectedWorkSlug, episodeNo)} 本集词汇`}
        description="这里用于查词义、读音、出现次数和讲解。正式训练进入综合训练队列完成。"
        actions={(
          <Link className="primary-action" to="/works/$workSlug/episodes/$episode/lesson" params={{ workSlug: selectedWorkSlug, episode: String(episodeNo) }}>
            开始综合训练
          </Link>
        )}
      />
      <EpisodeScopeSelector workSlug={selectedWorkSlug} episode={episodeNo} tool="vocab" />
      <div className="card-list">
        {vocabQuery.data?.map((item) => (
          <article className="learning-card" key={item.id}>
            <div>
              <h2>{item.surface}</h2>
              <p className="kana">{item.reading} · {item.romaji}</p>
              <p>{item.meaningZh}</p>
              <small>{item.pos} · {item.jlptLevel} · 出现 {item.totalOccurrences} 次</small>
            </div>
            <div className="card-actions">
              <TtsButton text={item.surface} />
              <AiExplainButton kind="vocab" text={item.surface} context={item.meaningZh} />
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}
