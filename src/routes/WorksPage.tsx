import { useEffect, useMemo, useState } from 'react'
import { Link } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { PageHeader } from '../components/PageHeader'
import { animeRepository } from '../server/repositories/animeRepository'
import type { Episode } from '../lib/types'

export function WorksPage() {
  const [selectedWorkSlug, setSelectedWorkSlug] = useState('')
  const worksQuery = useQuery({
    queryKey: ['works'],
    queryFn: () => animeRepository.listWorks(),
  })
  const works = worksQuery.data ?? []
  const selectedWork = useMemo(
    () => works.find((work) => work.slug === selectedWorkSlug) ?? works[0],
    [selectedWorkSlug, works],
  )
  const episodesQuery = useQuery({
    queryKey: ['episodes', selectedWork?.slug],
    queryFn: () => animeRepository.listEpisodes(selectedWork?.slug ?? ''),
    enabled: Boolean(selectedWork),
  })
  const episodes = episodesQuery.data ?? []
  const firstEpisode = episodes[0]?.episode ?? 1

  useEffect(() => {
    if (!selectedWorkSlug && works[0]) {
      setSelectedWorkSlug(works[0].slug)
      return
    }

    if (selectedWorkSlug && works.length > 0 && !works.some((work) => work.slug === selectedWorkSlug)) {
      setSelectedWorkSlug(works[0].slug)
    }
  }, [selectedWorkSlug, works])

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="作品库"
        title="已导入作品"
        description="按作品查看已导入集数，进入单集后可继续看词表、语法、跟读和台词。"
      />
      <div className="work-tabs segmented-control" aria-label="作品切换">
        {works.map((work) => (
          <button
            key={work.id}
            className={work.slug === selectedWork?.slug ? 'selected' : undefined}
            type="button"
            onClick={() => setSelectedWorkSlug(work.slug)}
          >
            {work.displayName}
          </button>
        ))}
      </div>

      {selectedWork ? (
        <section className="work-section">
          <div className="content-grid">
            <Link className="work-card" to="/works/$workSlug/episodes/$episode" params={{ workSlug: selectedWork.slug, episode: String(firstEpisode) }}>
              <strong>{selectedWork.displayName}</strong>
              <span>{episodes.length || selectedWork.episodeCount} 集已导入</span>
              <small>点击进入 EP{String(firstEpisode).padStart(2, '0')}</small>
            </Link>
          </div>
          <div className="episode-grid">
            {episodes.map((episode) => (
              <EpisodeLink key={episode.id} episode={episode} />
            ))}
          </div>
        </section>
      ) : null}
    </section>
  )
}

function EpisodeLink({ episode }: { episode: Episode }) {
  return (
    <Link
      className="episode-pill"
      to="/works/$workSlug/episodes/$episode"
      params={{ workSlug: episode.workSlug, episode: String(episode.episode) }}
    >
      <strong>EP{String(episode.episode).padStart(2, '0')}</strong>
      <span>{episode.jaLines} 日文行 · {episode.chunkCount} chunks</span>
    </Link>
  )
}
