import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { EpisodeScopeSelector } from '../components/EpisodeScopeSelector'
import { PageHeader } from '../components/PageHeader'
import { AiExplainButton } from '../components/AiExplainButton'
import { ReviewButton } from '../components/ReviewButton'
import { TtsButton } from '../components/TtsButton'
import { readEpisodeScope } from '../lib/episodeScope'
import { animeRepository } from '../server/repositories/animeRepository'

export function SentencesPage() {
  const { workSlug, episode } = useParams({ strict: false })
  const fallbackScope = readEpisodeScope()
  const selectedWorkSlug = workSlug ?? fallbackScope.workSlug
  const episodeNo = Number(episode ?? fallbackScope.episode)
  const sentencesQuery = useQuery({
    queryKey: ['sentences', selectedWorkSlug, episodeNo],
    queryFn: () => animeRepository.listEpisodeSentences(selectedWorkSlug, episodeNo),
  })

  return (
    <section className="page-stack">
      <PageHeader eyebrow="跟读" title={`EP${String(episodeNo).padStart(2, '0')} 跟读句`} />
      <EpisodeScopeSelector workSlug={selectedWorkSlug} episode={episodeNo} tool="sentences" />
      <div className="card-list">
        {sentencesQuery.data?.map((sentence) => (
          <article className="learning-card" key={sentence.id}>
            <div>
              <h2>{sentence.jaText}</h2>
              <p className="kana">{sentence.romaji}</p>
              <p>{sentence.meaningZh}</p>
              <small>{sentence.toneTags.join(' / ')} · {sentence.difficulty}</small>
            </div>
            <div className="card-actions">
              <TtsButton text={sentence.jaText} />
              <AiExplainButton kind="sentence" text={sentence.jaText} context={sentence.meaningZh} />
              <ReviewButton itemId={sentence.id} state="good" itemType="sentence" workSlug={selectedWorkSlug} episode={episodeNo} payload={{ label: sentence.jaText }}>像</ReviewButton>
              <ReviewButton itemId={sentence.id} state="ok" itemType="sentence" workSlug={selectedWorkSlug} episode={episodeNo} payload={{ label: sentence.jaText }}>一般</ReviewButton>
              <ReviewButton itemId={sentence.id} state="bad" itemType="sentence" workSlug={selectedWorkSlug} episode={episodeNo} payload={{ label: sentence.jaText }}>不像</ReviewButton>
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}
