import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from '@tanstack/react-router'
import { EpisodeScopeSelector } from '../components/EpisodeScopeSelector'
import { PageHeader } from '../components/PageHeader'
import { readEpisodeScope } from '../lib/episodeScope'
import { animeRepository } from '../server/repositories/animeRepository'

export function SubtitlesPage() {
  const { workSlug, episode } = useParams({ strict: false })
  const fallbackScope = readEpisodeScope()
  const selectedWorkSlug = workSlug ?? fallbackScope.workSlug
  const episodeNo = Number(episode ?? fallbackScope.episode)
  const subtitlesQuery = useQuery({
    queryKey: ['subtitles', selectedWorkSlug, episodeNo],
    queryFn: () => animeRepository.listSubtitleLines(selectedWorkSlug, episodeNo),
  })

  return (
    <section className="page-stack">
      <PageHeader eyebrow="台词浏览" title={`EP${String(episodeNo).padStart(2, '0')} 日文台词`} />
      <EpisodeScopeSelector workSlug={selectedWorkSlug} episode={episodeNo} tool="subtitles" />
      <div className="timeline">
        {subtitlesQuery.data?.map((line) => (
          <Link
            className="timeline-row"
            key={line.lineNo}
            to="/works/$workSlug/episodes/$episode/sentence"
            params={{ workSlug: selectedWorkSlug, episode: String(episodeNo) }}
            search={{ lineNo: String(line.lineNo) }}
          >
            <time>{line.startTime} - {line.endTime}</time>
            <div>
              <strong>{line.jaText}</strong>
              <span>{line.zhText}</span>
            </div>
          </Link>
        ))}
      </div>
    </section>
  )
}
