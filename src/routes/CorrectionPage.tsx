import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Send } from 'lucide-react'
import { PageHeader } from '../components/PageHeader'
import { StructuredAiResultView } from '../components/StructuredAiResultView'
import { usePreferredGatewayModel } from '../lib/aiPreferences'
import { getDeviceId } from '../lib/progress'
import { usePreferredReasoningEffort } from '../lib/reasoningPreferences'
import type { StructuredAiResult } from '../lib/types'
import { animeRepository } from '../server/repositories/animeRepository'

export function CorrectionPage() {
  const [targetType, setTargetType] = useState<'vocab' | 'grammar' | 'free'>('vocab')
  const [targetId, setTargetId] = useState('')
  const [sentence, setSentence] = useState('大丈夫、今日から頑張る。')
  const model = usePreferredGatewayModel()
  const reasoningEffort = usePreferredReasoningEffort()
  const [result, setResult] = useState<StructuredAiResult | null>(null)
  const [status, setStatus] = useState<'idle' | 'loading' | 'error'>('idle')

  const vocabQuery = useQuery({
    queryKey: ['vocab', 'k-on', 1],
    queryFn: () => animeRepository.listEpisodeVocab('k-on', 1),
  })
  const grammarQuery = useQuery({
    queryKey: ['grammar', 'k-on', 1],
    queryFn: () => animeRepository.listEpisodeGrammar('k-on', 1),
  })

  const targetOptions =
    targetType === 'grammar'
      ? grammarQuery.data?.map((item) => ({ id: item.id, label: item.pattern })) ?? []
      : vocabQuery.data?.map((item) => ({ id: item.id, label: item.surface })) ?? []
  const selectedTarget = targetOptions.find((item) => item.id === targetId) ?? targetOptions[0]

  async function submit() {
    setStatus('loading')
    try {
      const correction = await animeRepository.correctSentence({
        deviceId: getDeviceId(),
        targetType,
        targetId: targetType === 'free' ? undefined : selectedTarget?.id,
        targetLabel: targetType === 'free' ? '自由造句' : selectedTarget?.label,
        sentence,
        workSlug: 'k-on',
        episode: 1,
        model,
        reasoningEffort,
      })
      setResult(correction)
      setStatus('idle')
    } catch (error) {
      console.error(error)
      setStatus('error')
    }
  }

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="造句批改"
        title="词汇 / 语法点造句"
        description="选择一个目标后输入自己的日语句子，AI 返回语法、自然度、语气、改写和评分。"
      />
      <form className="correction-form">
        <div className="segmented-control" aria-label="目标类型">
          {(['vocab', 'grammar', 'free'] as const).map((item) => (
            <button
              className={targetType === item ? 'selected' : ''}
              type="button"
              key={item}
              onClick={() => {
                setTargetType(item)
                setTargetId('')
              }}
            >
              {item === 'vocab' ? '词' : item === 'grammar' ? '语法' : '自由'}
            </button>
          ))}
        </div>
        {targetType !== 'free' ? (
          <select className="model-select" value={selectedTarget?.id ?? ''} onChange={(event) => setTargetId(event.target.value)}>
            {targetOptions.map((item) => (
              <option key={item.id} value={item.id}>
                {item.label}
              </option>
            ))}
          </select>
        ) : null}
        <textarea rows={5} value={sentence} onChange={(event) => setSentence(event.target.value)} />
        <button className="primary-action" type="button" disabled={status === 'loading' || !sentence.trim()} onClick={submit}>
          <Send size={18} />
          <span>{status === 'loading' ? '批改中' : status === 'error' ? '批改失败' : '提交批改'}</span>
        </button>
      </form>
      {result ? <StructuredAiResultView result={result} /> : null}
    </section>
  )
}
