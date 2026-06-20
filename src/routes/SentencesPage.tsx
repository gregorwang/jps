import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { Bot } from 'lucide-react'
import { useState } from 'react'
import { AudioButton } from '../components/AudioButton'
import { EpisodeScopeSelector } from '../components/EpisodeScopeSelector'
import { PageHeader } from '../components/PageHeader'
import { AiExplainButton } from '../components/AiExplainButton'
import { FuriganaText } from '../components/JapaneseRubyText'
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
  const [furiganaStatus, setFuriganaStatus] = useState<'idle' | 'generating' | 'error'>('idle')
  const [furiganaProgress, setFuriganaProgress] = useState({ done: 0, total: 0 })
  const [furiganaRefreshKey, setFuriganaRefreshKey] = useState(0)
  const sentences = sentencesQuery.data ?? []

  async function generatePageFurigana() {
    if (sentences.length === 0) return
    setFuriganaStatus('generating')
    setFuriganaProgress({ done: 0, total: sentences.length })

    try {
      for (const sentence of sentences) {
        const response = await fetch('/api/ai/furigana', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            targetType: 'learning_sentence',
            targetId: sentence.id,
            text: sentence.jaText,
          }),
        })
        if (!response.ok) throw new Error(await response.text())
        setFuriganaProgress((current) => ({ ...current, done: current.done + 1 }))
      }
      setFuriganaRefreshKey((value) => value + 1)
      setFuriganaStatus('idle')
    } catch (error) {
      console.error(error)
      setFuriganaStatus('error')
      setFuriganaRefreshKey((value) => value + 1)
    }
  }

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="跟读"
        title={`EP${String(episodeNo).padStart(2, '0')} 跟读句`}
        actions={(
          <div className="furigana-batch-control">
            <button
              className="icon-button secondary compact-action"
              type="button"
              onClick={() => void generatePageFurigana()}
              disabled={furiganaStatus === 'generating' || sentences.length === 0}
            >
              <Bot size={15} />
              <span>{furiganaStatus === 'generating' ? `生成 ${furiganaProgress.done}/${furiganaProgress.total}` : '批量生成假名'}</span>
            </button>
            {furiganaStatus === 'error' ? <small>有句子生成失败，可重试</small> : null}
          </div>
        )}
      />
      <EpisodeScopeSelector workSlug={selectedWorkSlug} episode={episodeNo} tool="sentences" />
      <div className="card-list">
        {sentences.map((sentence) => {
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
                <h2>
                  <FuriganaText text={sentence.jaText} targetId={sentence.id} refreshKey={furiganaRefreshKey} />
                </h2>
                <p className="kana">{sentence.romaji}</p>
                <p>{sentence.meaningZh}</p>
                <small>{sentence.toneTags.join(' / ')} · {sentence.difficulty}</small>
                {sourceAudio?.isFlagged ? (
                  <div className="audio-warning">
                    <b>unmatch</b>
                    <span>原声可能不准，建议 TTS</span>
                  </div>
                ) : null}
              </div>
              <div className="card-actions">
                {sourceAudio ? <AudioButton src={sourceAudio.url} label="原声" /> : null}
                <TtsButton text={sentence.jaText} label="TTS" variant={sourceAudio ? 'secondary' : 'primary'} />
                <AiExplainButton kind="sentence" text={sentence.jaText} context={sentence.meaningZh} />
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
