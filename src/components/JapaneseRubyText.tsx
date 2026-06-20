import { useEffect, useState } from 'react'
import type { FuriganaResult, RubySegment } from '../lib/types'

type JapaneseRubyTextProps = {
  text: string
  segments?: RubySegment[]
}

type FuriganaTextProps = {
  text: string
  targetId: string
  targetType?: string
  className?: string
  refreshKey?: number
}

export function JapaneseRubyText({ text, segments }: JapaneseRubyTextProps) {
  if (!segments?.length) return <>{text}</>

  return (
    <>
      {segments.map((segment, index) => segment.reading ? (
        <ruby key={`${segment.text}-${segment.reading}-${index}`}>
          {segment.text}
          <rt>{segment.reading}</rt>
        </ruby>
      ) : (
        <span key={`${segment.text}-${index}`}>{segment.text}</span>
      ))}
    </>
  )
}

export function FuriganaText({
  text,
  targetId,
  targetType = 'learning_sentence',
  className,
  refreshKey = 0,
}: FuriganaTextProps) {
  const [segments, setSegments] = useState<RubySegment[] | null>(null)
  const [status, setStatus] = useState<'loading' | 'idle' | 'missing' | 'error'>('loading')

  useEffect(() => {
    let cancelled = false
    setSegments(null)
    setStatus('loading')

    const params = new URLSearchParams({ targetType, targetId, text })
    fetch(`/api/ai/furigana?${params.toString()}`)
      .then(async (response) => {
        if (response.status === 404) return null
        if (!response.ok) throw new Error(await response.text())
        return (await response.json()) as FuriganaResult
      })
      .then((result) => {
        if (cancelled) return
        if (result?.ruby_segments?.length) {
          setSegments(result.ruby_segments)
          setStatus('idle')
          return
        }
        setStatus('missing')
      })
      .catch((error) => {
        console.error(error)
        if (!cancelled) setStatus('error')
      })

    return () => {
      cancelled = true
    }
  }, [refreshKey, targetId, targetType, text])

  return (
    <div className={['furigana-text', className].filter(Boolean).join(' ')}>
      <span className="ruby-line">
        <JapaneseRubyText text={text} segments={segments ?? undefined} />
      </span>
      {status === 'missing' ? <small className="furigana-status">假名未生成</small> : null}
      {status === 'error' ? <small className="furigana-status">读音读取失败</small> : null}
    </div>
  )
}
