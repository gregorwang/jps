import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { RotateCcw, Send, SkipForward } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { EpisodeScopeSelector } from '../components/EpisodeScopeSelector'
import { HandwritingCanvas, type HandwritingCanvasHandle } from '../components/HandwritingCanvas'
import { PageHeader } from '../components/PageHeader'
import { readEpisodeScope } from '../lib/episodeScope'
import { buildWritingItems, fetchWritingStats, readLocalWritingStats, skipWritingPractice, submitWritingPractice } from '../lib/writingPractice'
import { validateWriting, type WritingValidationResult } from '../lib/writingValidation'
import { animeRepository } from '../server/repositories/animeRepository'

const defaultWorkSlug = 'k-on'
const defaultEpisode = 1

export function WritingPracticePage() {
  const { workSlug, episode } = useParams({ strict: false })
  const fallbackScope = readEpisodeScope()
  const selectedWorkSlug = workSlug ?? fallbackScope.workSlug ?? defaultWorkSlug
  const selectedEpisode = Number(episode ?? fallbackScope.episode ?? defaultEpisode)
  const canvasRef = useRef<HandwritingCanvasHandle>(null)
  const [index, setIndex] = useState(0)
  const [localStatsTick, setLocalStatsTick] = useState(0)
  const [lastResult, setLastResult] = useState<WritingValidationResult | { passed: false; reason: 'skipped' } | null>(null)

  const vocabQuery = useQuery({
    queryKey: ['writing-vocab', selectedWorkSlug, selectedEpisode],
    queryFn: () => animeRepository.listEpisodeHandwritingVocab(selectedWorkSlug, selectedEpisode),
  })
  const statsQuery = useQuery({
    queryKey: ['writing-stats'],
    queryFn: fetchWritingStats,
  })

  const items = useMemo(
    () => buildWritingItems(vocabQuery.data ?? [], selectedWorkSlug, selectedEpisode),
    [selectedEpisode, selectedWorkSlug, vocabQuery.data],
  )
  const current = items[index] ?? items[0]
  const localStats = useMemo(() => readLocalWritingStats(), [localStatsTick])
  const remoteStats = statsQuery.data ?? []
  const currentStats = remoteStats.find((item) => item.itemId === current?.id) ?? localStats[current?.id ?? '']
  const todayCompleted = remoteStats.reduce((count, item) => {
    const lastPracticed = item.lastPracticedAt?.slice(0, 10)
    return lastPracticed === new Date().toISOString().slice(0, 10) ? count + item.completedCount : count
  }, 0)

  useEffect(() => {
    canvasRef.current?.clear()
    setIndex(0)
    setLastResult(null)
  }, [selectedEpisode, selectedWorkSlug])

  function moveNext() {
    canvasRef.current?.clear()
    setIndex((currentIndex) => (items.length ? (currentIndex + 1) % items.length : 0))
  }

  async function handleSubmit() {
    if (!current) return
    const size = canvasRef.current?.getSize() ?? { width: 0, height: 0 }
    const strokes = canvasRef.current?.getStrokes() ?? []
    const result = validateWriting(strokes, {
      width: size.width,
      height: size.height,
      target: current.text,
    })
    setLastResult(result)
    await submitWritingPractice({
      item: current,
      passed: result.passed,
      reason: result.reason,
      metrics: result.metrics,
    })
    setLocalStatsTick((value) => value + 1)
    void statsQuery.refetch()
    if (result.passed) moveNext()
  }

  async function handleSkip() {
    if (!current) return
    setLastResult({ passed: false, reason: 'skipped' })
    await skipWritingPractice(current)
    moveNext()
  }

  function handleRestart() {
    canvasRef.current?.clear()
    setIndex(0)
    setLastResult(null)
  }

  function resultLabel() {
    if (!lastResult) return '尚未提交'
    if (lastResult.passed) return '完成'
    if (lastResult.reason === 'empty' || lastResult.reason === 'too_short') return '写得太少，请重写'
    if (lastResult.reason === 'skipped') return '已跳过'
    return '偏离太多，清除后再写一次'
  }

  return (
    <section className="page-stack writing-page">
      <PageHeader
        eyebrow="Writing Practice"
        title={`${selectedWorkSlug} EP${String(selectedEpisode).padStart(2, '0')} 手写`}
        description="沿着本集手写词描红，提交后通过就自动进入下一个。"
      />
      <EpisodeScopeSelector workSlug={selectedWorkSlug} episode={selectedEpisode} tool="writing" />

      <div className="writing-layout">
        <aside className="prompt-panel writing-target-panel">
          <div className="trainer-progress-row">
            <p className="eyebrow">当前目标</p>
            <span className="review-chip">{items.length ? index + 1 : 0} / {items.length}</span>
          </div>
          <strong className="writing-target-text">{current?.text ?? '暂无'}</strong>
          <div className="mini-lines">
            {current?.reading ? <p><b>读音</b>{current.reading}{current.romaji ? ` / ${current.romaji}` : ''}</p> : null}
            {current?.meaningZh ? <p><b>释义</b>{current.meaningZh}</p> : null}
            <p><b>来源</b>{current ? '本集手写词' : '等待词库'}</p>
          </div>

          <div className="writing-stat-grid">
            <div>
              <small>当前练习次数</small>
              <strong>{currentStats?.completedCount ?? 0}</strong>
            </div>
            <div>
              <small>今日已完成</small>
              <strong>{todayCompleted}</strong>
            </div>
          </div>

          <div className={`writing-result ${lastResult?.passed ? 'passed' : lastResult ? 'failed' : ''}`}>
            <span>最近提交</span>
            <strong>{resultLabel()}</strong>
          </div>

          <div className="card-actions writing-actions">
            <button className="primary-action" type="button" disabled={!current} onClick={() => void handleSubmit()}>
              <Send size={18} />
              <span>提交</span>
            </button>
            <button className="icon-button secondary" type="button" onClick={() => canvasRef.current?.clear()}>
              清除 / 重写
            </button>
            <button className="icon-button secondary" type="button" disabled={!current} onClick={() => void handleSkip()}>
              <SkipForward size={18} />
              <span>跳过当前</span>
            </button>
            <button className="icon-button secondary" type="button" onClick={handleRestart}>
              <RotateCcw size={18} />
              <span>重新开始本轮</span>
            </button>
          </div>
        </aside>

        <HandwritingCanvas
          ref={canvasRef}
          target={current?.text}
          onClear={() => setLastResult(null)}
        />
      </div>
    </section>
  )
}
