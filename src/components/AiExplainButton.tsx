import { Bot } from 'lucide-react'
import type { ReactNode } from 'react'
import { useState } from 'react'
import { usePreferredGatewayModel } from '../lib/aiPreferences'
import { getDeviceId } from '../lib/progress'
import { usePreferredReasoningEffort } from '../lib/reasoningPreferences'
import type { StructuredAiResult } from '../lib/types'
import { StructuredAiResultView } from './StructuredAiResultView'

type AiExplainButtonProps = {
  kind: 'vocab' | 'sentence' | 'grammar'
  text: string
  context?: string
  label?: string
  icon?: ReactNode
}

export function AiExplainButton({ kind, text, context, label = '展开讲解', icon }: AiExplainButtonProps) {
  const [status, setStatus] = useState<'idle' | 'loading' | 'error'>('idle')
  const [result, setResult] = useState<StructuredAiResult | null>(null)
  const model = usePreferredGatewayModel()
  const reasoningEffort = usePreferredReasoningEffort()

  async function explain() {
    setStatus('loading')
    try {
      const response = await fetch('/api/ai/explain', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ kind, text, context, model, reasoningEffort, deviceId: getDeviceId() }),
      })
      if (!response.ok) throw new Error(await response.text())
      setResult((await response.json()) as StructuredAiResult)
      setStatus('idle')
    } catch (error) {
      console.error(error)
      setStatus('error')
    }
  }

  return (
    <div className="ai-explain">
      {result ? null : (
        <button className="icon-button secondary" type="button" onClick={explain} disabled={status === 'loading'}>
          {icon ?? <Bot size={18} />}
          <span>{status === 'loading' ? '分析中' : status === 'error' ? '失败' : label}</span>
        </button>
      )}
      {result ? <StructuredAiResultView result={result} /> : null}
    </div>
  )
}
