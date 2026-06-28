import { useQuery } from '@tanstack/react-query'
import { useParams, useSearch } from '@tanstack/react-router'
import { useState } from 'react'
import { Bot } from 'lucide-react'
import { PageHeader } from '../components/PageHeader'
import { StructuredAiResultView } from '../components/StructuredAiResultView'
import { TtsButton } from '../components/TtsButton'
import { usePreferredGatewayModel } from '../lib/aiPreferences'
import { formatEpisodeLabel } from '../lib/episodeLabels'
import { getDeviceId } from '../lib/progress'
import { usePreferredReasoningEffort } from '../lib/reasoningPreferences'
import { animeRepository } from '../server/repositories/animeRepository'

export function SentenceDeepDivePage() {
  const { workSlug, episode } = useParams({ strict: false })
  const search = useSearch({ strict: false }) as { lineNo?: string }
  const episodeNo = Number(episode ?? 1)
  const lineNo = Number(search.lineNo ?? 1)
  const model = usePreferredGatewayModel()
  const reasoningEffort = usePreferredReasoningEffort()
  const [enabled, setEnabled] = useState(false)

  const lineQuery = useQuery({
    queryKey: ['subtitle-line', workSlug, episodeNo, lineNo],
    queryFn: () => animeRepository.getSubtitleLine(workSlug ?? 'k-on', episodeNo, lineNo),
  })

  const analysisQuery = useQuery({
    queryKey: ['sentence-deep-dive', workSlug, episodeNo, lineNo, model, reasoningEffort],
    enabled: enabled && Boolean(lineQuery.data),
    queryFn: () =>
      animeRepository.deepDiveSentence({
        workSlug: workSlug ?? 'k-on',
        episode: episodeNo,
        lineNo,
        jaText: lineQuery.data?.jaText ?? '',
        zhText: lineQuery.data?.zhText,
        deviceId: getDeviceId(),
        model,
        reasoningEffort,
      }),
  })

  const line = lineQuery.data

  return (
    <section className="page-stack">
      <PageHeader eyebrow="单句精读" title={`${formatEpisodeLabel(workSlug, episodeNo)} · line ${lineNo}`} />
      <article className="sentence-focus">
        <time>{line?.startTime} - {line?.endTime}</time>
        <h1>{line?.jaText ?? '未找到这句台词'}</h1>
        <p>{line?.zhText ?? '请从台词浏览页重新选择一句。'}</p>
        <div className="card-actions">
          {line?.jaText ? <TtsButton text={line.jaText} /> : null}
          <button className="primary-action" type="button" disabled={!line || analysisQuery.isFetching} onClick={() => setEnabled(true)}>
            <Bot size={18} />
            <span>{analysisQuery.isFetching ? '精读中' : '生成精读'}</span>
          </button>
        </div>
      </article>
      {analysisQuery.data ? <StructuredAiResultView result={analysisQuery.data} /> : null}
      {analysisQuery.isError ? <p className="error-text">AI 精读失败，请稍后重试。</p> : null}
    </section>
  )
}
