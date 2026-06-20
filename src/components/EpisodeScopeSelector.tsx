import { useQuery } from '@tanstack/react-query'
import { useNavigate } from '@tanstack/react-router'
import { useEffect, useMemo } from 'react'
import { writeEpisodeScope } from '../lib/episodeScope'
import { animeRepository } from '../server/repositories/animeRepository'

export type EpisodeTool = 'vocab' | 'grammar' | 'sentences' | 'writing' | 'subtitles'

const toolPaths: Record<EpisodeTool, string> = {
  vocab: '/works/$workSlug/episodes/$episode/vocab',
  grammar: '/works/$workSlug/episodes/$episode/grammar',
  sentences: '/works/$workSlug/episodes/$episode/sentences',
  writing: '/works/$workSlug/episodes/$episode/writing',
  subtitles: '/works/$workSlug/episodes/$episode/subtitles',
}

export function EpisodeScopeSelector({
  workSlug,
  episode,
  tool,
}: {
  workSlug: string
  episode: number
  tool: EpisodeTool
}) {
  const navigate = useNavigate()
  const worksQuery = useQuery({
    queryKey: ['works'],
    queryFn: () => animeRepository.listWorks(),
  })
  const works = worksQuery.data ?? []
  const selectedWork = useMemo(
    () => works.find((work) => work.slug === workSlug) ?? works[0],
    [workSlug, works],
  )
  const selectedWorkSlug = selectedWork?.slug ?? workSlug
  const episodesQuery = useQuery({
    queryKey: ['episodes', selectedWorkSlug],
    queryFn: () => animeRepository.listEpisodes(selectedWorkSlug),
    enabled: Boolean(selectedWorkSlug),
  })
  const episodes = episodesQuery.data ?? []
  const selectedEpisode = episodes.some((item) => item.episode === episode)
    ? episode
    : episodes[0]?.episode ?? episode

  useEffect(() => {
    if (!selectedWorkSlug || !selectedEpisode) return
    writeEpisodeScope({ workSlug: selectedWorkSlug, episode: selectedEpisode })
  }, [selectedEpisode, selectedWorkSlug])

  async function goTo(nextWorkSlug: string, nextEpisode: number) {
    writeEpisodeScope({ workSlug: nextWorkSlug, episode: nextEpisode })
    await navigate({
      to: toolPaths[tool],
      params: { workSlug: nextWorkSlug, episode: String(nextEpisode) },
    })
  }

  async function changeWork(nextWorkSlug: string) {
    const nextEpisodes = nextWorkSlug === selectedWorkSlug
      ? episodes
      : await animeRepository.listEpisodes(nextWorkSlug)
    const nextEpisode = nextEpisodes.find((item) => item.episode === selectedEpisode)?.episode
      ?? nextEpisodes[0]?.episode
      ?? 1
    await goTo(nextWorkSlug, nextEpisode)
  }

  return (
    <section className="episode-scope-panel">
      <label className="filter-field compact-field">
        <span>Work</span>
        <select value={selectedWorkSlug} onChange={(event) => void changeWork(event.target.value)} disabled={!works.length}>
          {works.map((work) => (
            <option key={work.id} value={work.slug}>{work.displayName}</option>
          ))}
        </select>
      </label>
      <label className="filter-field compact-field">
        <span>Episode</span>
        <select
          value={String(selectedEpisode)}
          onChange={(event) => void goTo(selectedWorkSlug, Number(event.target.value))}
          disabled={episodesQuery.isLoading || !episodes.length}
        >
          {episodes.map((item) => (
            <option key={item.id} value={item.episode}>EP{String(item.episode).padStart(2, '0')}</option>
          ))}
        </select>
      </label>
    </section>
  )
}
