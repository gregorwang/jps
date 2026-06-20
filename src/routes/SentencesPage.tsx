import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { AudioButton } from '../components/AudioButton'
import { EpisodeScopeSelector } from '../components/EpisodeScopeSelector'
import { PageHeader } from '../components/PageHeader'
import { AiExplainButton } from '../components/AiExplainButton'
import { ReviewButton } from '../components/ReviewButton'
import { TtsButton } from '../components/TtsButton'
import { readEpisodeScope } from '../lib/episodeScope'
import { buildReZeroShadowingAudio } from '../lib/rezeroShadowingAudio'
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
        {sentencesQuery.data?.map((sentence) => {
          const sourceAudio =
            selectedWorkSlug === 're-zero'
              ? buildReZeroShadowingAudio({
                  sentenceId: sentence.id,
                  audioUrl: sentence.audioUrl,
                  storagePath: sentence.storagePath,
                })
              : null

          return (
            <article className="learning-card" key={sentence.id}>
              <div>
                <h2>{sentence.jaText}</h2>
                <p className="kana">{sentence.romaji}</p>
                <p>{sentence.meaningZh}</p>
                <small>{sentence.toneTags.join(' / ')} · {sentence.difficulty}</small>
                {sourceAudio?.isFlagged ? (
                  <div className="audio-warning">
                    <b>unmatch</b>
                    <span>原声可能不准确，建议优先 TTS 跟读</span>
                  </div>
                ) : null}
              </div>
              <div className="card-actions">
                {sourceAudio?.isFlagged ? (
                  <>
                    <TtsButton text={sentence.jaText} label="TTS" />
                    <AudioButton src={sourceAudio.url} label="原声" variant="secondary" />
                  </>
                ) : (
                  <>
                    {sourceAudio ? <AudioButton src={sourceAudio.url} label="原声" /> : null}
                    <TtsButton text={sentence.jaText} label="TTS" variant={sourceAudio ? 'secondary' : 'primary'} />
                  </>
                )}
                <AiExplainButton kind="sentence" text={sentence.jaText} context={sentence.meaningZh} />
                <ReviewButton itemId={sentence.id} state="good" itemType="sentence" workSlug={selectedWorkSlug} episode={episodeNo} payload={{ label: sentence.jaText }}>像</ReviewButton>
                <ReviewButton itemId={sentence.id} state="ok" itemType="sentence" workSlug={selectedWorkSlug} episode={episodeNo} payload={{ label: sentence.jaText }}>一般</ReviewButton>
                <ReviewButton itemId={sentence.id} state="bad" itemType="sentence" workSlug={selectedWorkSlug} episode={episodeNo} payload={{ label: sentence.jaText }}>不像</ReviewButton>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
