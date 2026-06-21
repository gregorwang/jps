import { ArrowLeft, Check, RotateCcw, Volume2, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from '@tanstack/react-router'
import type {
  ClozeChoiceLessonNode,
  LessonNode,
  LessonPlan,
  PairMatchLessonNode,
  PromptAudio,
  SingleChoiceLessonNode,
  StudyLessonNode,
  TileLessonNode,
} from '../lib/lesson'
import { saveReviewState } from '../lib/progress'
import { appendLessonAttempt, clearLessonSession, readLessonSession, saveLessonSession, summarizeLessonAttempts } from '../lib/lessonProgress'
import { usePreferredJapaneseVoice } from '../lib/voicePreferences'
import { preloadJapaneseTts, speakJapanese } from '../server/tts'

type LessonPlayerProps = {
  lesson: LessonPlan
}

type AnswerResult = {
  correct: boolean
  selected: unknown
  answer: unknown
}

export function LessonPlayer({ lesson }: LessonPlayerProps) {
  const initialSession = readLessonSession(lesson.id)
  const [index, setIndex] = useState(initialSession?.completedAt ? lesson.nodes.length : initialSession?.index ?? 0)
  const [feedback, setFeedback] = useState<AnswerResult | null>(null)
  const [stats, setStats] = useState({
    correct: initialSession?.correct ?? 0,
    total: initialSession?.total ?? 0,
  })
  const [summaryVersion, setSummaryVersion] = useState(0)
  const nodeStartedAtRef = useRef(Date.now())
  const node = lesson.nodes[index]
  const complete = !node
  const attemptSummary = useMemo(() => summarizeLessonAttempts(lesson.id), [lesson.id, summaryVersion])
  const nextActions = useMemo(() => buildLessonNextActions(lesson), [lesson])

  useEffect(() => {
    const session = readLessonSession(lesson.id)
    setIndex(session?.completedAt ? lesson.nodes.length : session?.index ?? 0)
    setStats({
      correct: session?.correct ?? 0,
      total: session?.total ?? 0,
    })
    setFeedback(null)
    setSummaryVersion((value) => value + 1)
    nodeStartedAtRef.current = Date.now()
  }, [lesson.id, lesson.nodes.length])

  useEffect(() => {
    preloadNodeAudio(lesson.nodes[index])
    preloadNodeAudio(lesson.nodes[index + 1])
  }, [index, lesson.nodes])

  async function recordAnswer(result: AnswerResult, advanceImmediately = false) {
    if (!node || feedback) return
    if (!advanceImmediately) setFeedback(result)
    const durationMs = Date.now() - nodeStartedAtRef.current
    const nextStats = {
      correct: stats.correct + (result.correct ? 1 : 0),
      total: stats.total + 1,
    }
    setStats(nextStats)
    appendLessonAttempt({
      lessonId: lesson.id,
      nodeId: node.id,
      nodeType: node.type,
      sourceKind: node.source.kind,
      sourceId: node.source.sourceId,
      label: node.reviewLabel,
      correct: result.correct,
      selected: result.selected,
      answer: result.answer,
      audioKind: node.audio.kind,
      durationMs,
    })
    setSummaryVersion((value) => value + 1)
    saveLessonSession({
      lessonId: lesson.id,
      index: Math.min(index + 1, lesson.nodes.length),
      ...nextStats,
    })
    void saveReviewState(node.id, result.correct ? 'good' : 'bad', {
      itemType: node.source.kind,
      workSlug: node.source.workSlug,
      episode: node.source.episode,
      payload: {
        label: node.reviewLabel,
        prompt: node.prompt,
        sourceId: node.source.sourceId,
        exerciseType: node.type,
        lessonId: lesson.id,
        lessonMode: lesson.mode,
        durationMs,
        selected: result.selected,
        answer: result.answer,
        audioKind: node.audio.kind,
      },
    }).catch(() => undefined)
    if (advanceImmediately || result.correct) {
      const nextIndex = Math.min(index + 1, lesson.nodes.length)
      const nextSession = {
        lessonId: lesson.id,
        index: nextIndex,
        ...nextStats,
        completedAt: nextIndex >= lesson.nodes.length ? new Date().toISOString() : undefined,
      }
      window.setTimeout(() => {
        setFeedback(null)
        setIndex(nextIndex)
        saveLessonSession(nextSession)
        nodeStartedAtRef.current = Date.now()
      }, advanceImmediately ? 0 : 520)
    }
  }

  function goNext() {
    const nextIndex = Math.min(index + 1, lesson.nodes.length)
    setFeedback(null)
    setIndex(nextIndex)
    saveLessonSession({
      lessonId: lesson.id,
      index: nextIndex,
      correct: stats.correct,
      total: stats.total,
      completedAt: nextIndex >= lesson.nodes.length ? new Date().toISOString() : undefined,
    })
    nodeStartedAtRef.current = Date.now()
  }

  function restart() {
    clearLessonSession(lesson.id)
    setFeedback(null)
    setStats({ correct: 0, total: 0 })
    setIndex(0)
    setSummaryVersion((value) => value + 1)
    nodeStartedAtRef.current = Date.now()
  }

  return (
    <section className="lesson-shell">
      <header className="lesson-topbar">
        <Link
          className="lesson-exit"
          to="/works/$workSlug/episodes/$episode"
          params={{ workSlug: lesson.workSlug, episode: String(lesson.episode) }}
        >
          <ArrowLeft size={20} />
          <span>退出</span>
        </Link>
        <div className="lesson-progress-track" aria-label="练习进度">
          <span style={{ width: `${lesson.nodes.length ? (Math.min(index, lesson.nodes.length) / lesson.nodes.length) * 100 : 0}%` }} />
        </div>
        <strong className="lesson-score">正确 {stats.correct} / 已答 {stats.total}</strong>
      </header>

      {complete ? (
        <section className="lesson-complete">
          <p className="eyebrow">本轮完成</p>
          <h1>{lesson.title}</h1>
          <strong>正确 {stats.correct} / 已答 {stats.total}</strong>
          <div className={attemptSummary.wrong ? 'lesson-result-summary has-weak' : 'lesson-result-summary'}>
            <span>本轮最后记录 {attemptSummary.total}</span>
            <span>正确 {attemptSummary.correct}</span>
            <span>需回炉 {attemptSummary.wrong}</span>
          </div>
          <div className="lesson-type-summary">
            <span>配对 {lesson.counts['pair-match']}</span>
            <span>听音 {lesson.counts['audio-tiles']}</span>
            <span>填空 {lesson.counts['cloze-choice']}</span>
            <span>拼句 {lesson.counts['translation-tiles']}</span>
            <span>选择 {lesson.counts['single-choice']}</span>
            <span>学习卡 {lesson.counts['study-card']}</span>
          </div>
          <div className="lesson-complete-actions">
            <button className="primary-action" type="button" onClick={restart}>
              <RotateCcw size={18} />
              <span>再练一轮</span>
            </button>
            {attemptSummary.wrong > 0 ? (
              <Link
                className="secondary-action"
                to="/works/$workSlug/episodes/$episode/lesson"
                search={{ mode: 'review' }}
                params={{ workSlug: lesson.workSlug, episode: String(lesson.episode) }}
              >
                错题回炉
              </Link>
            ) : null}
            {nextActions.map((action) => (
              <Link
                className="secondary-action"
                key={action.label}
                to="/works/$workSlug/episodes/$episode/lesson"
                search={action.search}
                params={{ workSlug: lesson.workSlug, episode: String(lesson.episode) }}
              >
                {action.label}
              </Link>
            ))}
            {lesson.hasNextBatch ? (
              <Link
                className="primary-action"
                to="/works/$workSlug/episodes/$episode/lesson"
                search={{ mode: lesson.mode, batch: lesson.batch + 1 }}
                params={{ workSlug: lesson.workSlug, episode: String(lesson.episode) }}
              >
                下一批
              </Link>
            ) : null}
            <Link
              className="secondary-action"
              to="/works/$workSlug/episodes/$episode"
              params={{ workSlug: lesson.workSlug, episode: String(lesson.episode) }}
            >
              返回单集页
            </Link>
          </div>
        </section>
      ) : (
        <article className="lesson-stage">
          <header className="lesson-question-head">
            <div>
              <p className="eyebrow">{node.title}</p>
              <h1>{node.prompt}</h1>
            </div>
            <span className="review-chip">{index + 1} / {lesson.nodes.length}</span>
          </header>

          <AudioPrompt audio={node.audio} />

          <LessonNodeView
            node={node}
            disabled={Boolean(feedback)}
            onAnswer={(result) => void recordAnswer(result)}
            onStudyComplete={(result) => void recordAnswer(result, true)}
          />

          <LessonFeedbackDock
            node={node}
            feedback={feedback}
            isLast={index >= lesson.nodes.length - 1}
            onNext={goNext}
          />
        </article>
      )}
    </section>
  )
}

function LessonNodeView({
  node,
  disabled,
  onAnswer,
  onStudyComplete,
}: {
  node: LessonNode
  disabled: boolean
  onAnswer: (result: AnswerResult) => void
  onStudyComplete: (result: AnswerResult) => void
}) {
  if (node.type === 'study-card') return <StudyCard node={node} disabled={disabled} onStudyComplete={onStudyComplete} />
  if (node.type === 'pair-match') return <PairMatchExercise node={node} disabled={disabled} onAnswer={onAnswer} />
  if (node.type === 'single-choice') return <SingleChoiceExercise node={node} disabled={disabled} onAnswer={onAnswer} />
  if (node.type === 'cloze-choice') return <ClozeChoiceExercise node={node} disabled={disabled} onAnswer={onAnswer} />
  return <TileExercise node={node} disabled={disabled} onAnswer={onAnswer} />
}

function AudioPrompt({ audio }: { audio: PromptAudio }) {
  const [status, setStatus] = useState<Record<string, 'idle' | 'loading' | 'playing' | 'error'>>({})
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const voice = usePreferredJapaneseVoice()

  async function play(cue: PromptAudio | { kind: 'tts'; text: string; autoPlay: boolean; label?: string }, autoAttempt = false) {
    if (cue.kind === 'none') return
    const key = audioCueKey(cue)
    setStatus((current) => ({ ...current, [key]: 'loading' }))
    try {
      if (cue.kind === 'source') {
        audioRef.current?.pause()
        const player = new Audio(cue.url)
        audioRef.current = player
        player.preload = 'auto'
        player.addEventListener('playing', () => setStatus((current) => ({ ...current, [key]: 'playing' })), { once: true })
        player.addEventListener('ended', () => setStatus((current) => ({ ...current, [key]: 'idle' })), { once: true })
        player.addEventListener('error', () => setStatus((current) => ({ ...current, [key]: 'error' })), { once: true })
        await player.play()
      } else {
        await speakJapanese(cue.text, voice)
        setStatus((current) => ({ ...current, [key]: 'idle' }))
      }
    } catch (error) {
      if (autoAttempt && error instanceof DOMException && error.name === 'NotAllowedError') {
        setStatus((current) => ({ ...current, [key]: 'idle' }))
        return
      }
      console.error(error)
      setStatus((current) => ({ ...current, [key]: 'error' }))
    }
  }

  useEffect(() => {
    if (audio.kind === 'none' || !audio.autoPlay) return
    void play(audio, true)
  }, [audio])

  if (audio.kind === 'none') return null

  return (
    <div className={audio.kind === 'source' && audio.reliability === 'flagged' ? 'lesson-audio flagged' : 'lesson-audio'}>
      <AudioButton cue={audio} status={status[audioCueKey(audio)] ?? 'idle'} onPlay={() => void play(audio)} />
      {audio.kind === 'source' && audio.fallbackTts ? <FallbackTtsButton fallback={audio.fallbackTts} status={status} onPlay={play} /> : null}
      {audio.kind === 'source' && audio.reliability === 'flagged' ? <small>原声可能不准，本题不会自动播放。</small> : null}
      {audio.kind === 'tts' ? <small>TTS 是辅助标准音，不代表角色原声语气。</small> : null}
    </div>
  )
}

function FallbackTtsButton({
  fallback,
  status,
  onPlay,
}: {
  fallback: { text: string; label?: string }
  status: Record<string, 'idle' | 'loading' | 'playing' | 'error'>
  onPlay: (cue: { kind: 'tts'; text: string; autoPlay: boolean; label?: string }) => Promise<void>
}) {
  const cue = { kind: 'tts' as const, text: fallback.text, autoPlay: false, label: fallback.label ?? '播放 TTS' }
  return (
    <AudioButton
      cue={cue}
      status={status[audioCueKey(cue)] ?? 'idle'}
      onPlay={() => void onPlay(cue)}
    />
  )
}

function preloadNodeAudio(node?: LessonNode) {
  if (!node) return
  if (node.audio.kind === 'tts') preloadJapaneseTts(node.audio.text)
  if (node.audio.kind === 'source') {
    const audio = new Audio(node.audio.url)
    audio.preload = 'auto'
    if (node.audio.fallbackTts) preloadJapaneseTts(node.audio.fallbackTts.text)
  }
  if (node.type === 'pair-match') {
    for (const pair of node.pairs) {
      if (pair.audioText) preloadJapaneseTts(pair.audioText)
    }
  }
}

function audioCueKey(cue: PromptAudio | { kind: 'tts'; text: string; autoPlay: boolean; label?: string }) {
  if (cue.kind === 'source') return `source:${cue.url}`
  if (cue.kind === 'tts') return `tts:${cue.text}`
  return 'none'
}

function AudioButton({
  cue,
  status,
  onPlay,
}: {
  cue: PromptAudio | { kind: 'tts'; text: string; autoPlay: boolean; label?: string }
  status: 'idle' | 'loading' | 'playing' | 'error'
  onPlay: () => void
}) {
  if (cue.kind === 'none') return null
  return (
    <button className="lesson-audio-button" type="button" onClick={onPlay} disabled={status === 'loading'}>
      <Volume2 size={28} />
      <span>
        {status === 'loading'
          ? '加载中'
          : status === 'playing'
            ? '播放中'
            : status === 'error'
              ? '播放失败'
              : cue.label ?? (cue.kind === 'source' ? '播放原声' : '播放 TTS')}
      </span>
    </button>
  )
}

function StudyCard({
  node,
  disabled,
  onStudyComplete,
}: {
  node: StudyLessonNode
  disabled: boolean
  onStudyComplete: (result: AnswerResult) => void
}) {
  return (
    <div className="study-card-node">
      <div>
        <p className="study-ja">{node.jaText}</p>
        {node.reading ? <p className="study-reading">{node.reading}</p> : null}
        <p className="study-meaning">{node.meaningZh}</p>
      </div>
      {node.notes.length ? (
        <ul>
          {node.notes.map((note) => <li key={note}>{note}</li>)}
        </ul>
      ) : null}
      <LinguisticPerspective payload={node.linguisticPayload} />
      <button
        className="primary-action"
        type="button"
        disabled={disabled}
        onClick={() => onStudyComplete({ correct: true, selected: 'studied', answer: node.reviewLabel })}
      >
        我学完了
      </button>
    </div>
  )
}

function LinguisticPerspective({ payload }: { payload: StudyLessonNode['linguisticPayload'] }) {
  if (!hasLinguisticContent(payload)) return null

  const domains = payload.domains?.filter(hasDomainContent) ?? []
  const terms = payload.terms?.filter(hasTermContent) ?? []

  return (
    <details className="linguistic-perspective">
      <summary>为什么日语会这样说？</summary>
      <div className="linguistic-perspective-body">
        {payload.headlineZh ? <p className="linguistic-headline">{payload.headlineZh}</p> : null}
        {domains.length ? (
          <div className="linguistic-domain-grid">
            {domains.map((domain, index) => (
              <section className="linguistic-domain-card" key={`${domain.titleZh ?? 'domain'}-${index}`}>
                {domain.titleZh ? <h2>{domain.titleZh}</h2> : null}
                {domain.explanationZh ? <p>{domain.explanationZh}</p> : null}
                {domain.takeawayZh ? <strong>{domain.takeawayZh}</strong> : null}
              </section>
            ))}
          </div>
        ) : null}
        {terms.length ? (
          <div className="linguistic-terms" aria-label="语言学术语">
            {terms.map((term, index) => (
              <span className="linguistic-term" key={`${term.termZh ?? 'term'}-${index}`}>
                {term.termZh ? <b>{term.termZh}</b> : null}
                {term.plainZh ? <small>{term.plainZh}</small> : null}
              </span>
            ))}
          </div>
        ) : null}
        {payload.historicalNoteZh ? <p className="linguistic-note">{payload.historicalNoteZh}</p> : null}
        {payload.cautionZh ? <p className="linguistic-caution">{payload.cautionZh}</p> : null}
      </div>
    </details>
  )
}

function hasLinguisticContent(payload: StudyLessonNode['linguisticPayload']): payload is NonNullable<StudyLessonNode['linguisticPayload']> {
  if (!payload) return false
  return Boolean(
    payload.headlineZh ||
    payload.historicalNoteZh ||
    payload.cautionZh ||
    payload.domains?.some(hasDomainContent) ||
    payload.terms?.some(hasTermContent),
  )
}

function hasDomainContent(domain: unknown) {
  if (!domain || typeof domain !== 'object') return false
  const row = domain as { titleZh?: unknown; explanationZh?: unknown; takeawayZh?: unknown }
  return Boolean(row.titleZh || row.explanationZh || row.takeawayZh)
}

function hasTermContent(term: unknown) {
  if (!term || typeof term !== 'object') return false
  const row = term as { termZh?: unknown; plainZh?: unknown }
  return Boolean(row.termZh || row.plainZh)
}

function PairMatchExercise({
  node,
  disabled,
  onAnswer,
}: {
  node: PairMatchLessonNode
  disabled: boolean
  onAnswer: (result: AnswerResult) => void
}) {
  const [selectedLeft, setSelectedLeft] = useState('')
  const [matched, setMatched] = useState<string[]>([])
  const [wrongRight, setWrongRight] = useState('')
  const voice = usePreferredJapaneseVoice()
  const leftItems = useMemo(() => shuffleItems(node.pairs, `${node.id}:left`), [node])
  const rightItems = useMemo(() => shuffleItems(node.pairs, `${node.id}:right`), [node])
  const matchedSet = new Set(matched)

  async function chooseRight(pairId: string, audioText?: string) {
    if (disabled || !selectedLeft || matchedSet.has(pairId)) return
    if (audioText) void speakJapanese(audioText, voice).catch((error) => console.error(error))
    if (selectedLeft === pairId) {
      const next = [...matched, pairId]
      setMatched(next)
      setSelectedLeft('')
      if (next.length === node.pairs.length) {
        onAnswer({ correct: true, selected: next, answer: node.pairs.map((pair) => pair.id) })
      }
      return
    }
    setWrongRight(pairId)
    window.setTimeout(() => {
      setWrongRight('')
      setSelectedLeft('')
    }, 520)
  }

  return (
    <div className="pair-match-grid">
      <div className="pair-column">
        {leftItems.map((pair) => (
          <button
            className={[
              'lesson-choice',
              selectedLeft === pair.id ? 'selected' : '',
              matchedSet.has(pair.id) ? 'correct' : '',
            ].filter(Boolean).join(' ')}
            type="button"
            key={pair.id}
            disabled={disabled || matchedSet.has(pair.id)}
            onClick={() => setSelectedLeft(pair.id)}
          >
            {pair.left}
          </button>
        ))}
      </div>
      <div className="pair-column">
        {rightItems.map((pair) => (
          <button
            className={[
              'lesson-choice',
              matchedSet.has(pair.id) ? 'correct' : '',
              wrongRight === pair.id ? 'wrong' : '',
            ].filter(Boolean).join(' ')}
            type="button"
            key={pair.id}
            disabled={disabled || matchedSet.has(pair.id)}
            onClick={() => void chooseRight(pair.id, pair.audioText)}
          >
            {pair.right}
          </button>
        ))}
      </div>
    </div>
  )
}

function SingleChoiceExercise({
  node,
  disabled,
  onAnswer,
}: {
  node: SingleChoiceLessonNode
  disabled: boolean
  onAnswer: (result: AnswerResult) => void
}) {
  const [selected, setSelected] = useState('')
  const answered = disabled && selected
  return (
    <div className="lesson-single">
      {node.body ? <blockquote>{node.body}</blockquote> : null}
      <div className="lesson-choice-grid">
        {node.choices.map((choice) => (
          <button
            className={[
              'lesson-choice',
              selected === choice ? 'selected' : '',
              answered && choice === node.answer ? 'correct' : '',
              answered && selected === choice && choice !== node.answer ? 'wrong' : '',
            ].filter(Boolean).join(' ')}
            type="button"
            key={choice}
            disabled={disabled}
            onClick={() => {
              setSelected(choice)
              onAnswer({ correct: choice === node.answer, selected: choice, answer: node.answer })
            }}
          >
            {choice}
          </button>
        ))}
      </div>
    </div>
  )
}

function ClozeChoiceExercise({
  node,
  disabled,
  onAnswer,
}: {
  node: ClozeChoiceLessonNode
  disabled: boolean
  onAnswer: (result: AnswerResult) => void
}) {
  const [selected, setSelected] = useState('')
  const answered = disabled && selected
  return (
    <div className="cloze-exercise">
      <p className="cloze-sentence">
        <span>{node.before}</span>
        <b>{answered ? node.answer : selected || '____'}</b>
        <span>{node.after}</span>
      </p>
      <div className="lesson-choice-grid">
        {node.choices.map((choice) => (
          <button
            className={[
              'lesson-choice',
              selected === choice.value ? 'selected' : '',
              answered && choice.value === node.answer ? 'correct' : '',
              answered && selected === choice.value && choice.value !== node.answer ? 'wrong' : '',
            ].filter(Boolean).join(' ')}
            type="button"
            key={choice.value}
            title={choice.note}
            disabled={disabled}
            onClick={() => setSelected(choice.value)}
          >
            <span>{choice.value}</span>
            {choice.note ? <small>{choice.note}</small> : null}
          </button>
        ))}
      </div>
      <button
        className="primary-action"
        type="button"
        disabled={disabled || !selected}
        onClick={() => onAnswer({ correct: selected === node.answer, selected, answer: node.answer })}
      >
        检查
      </button>
    </div>
  )
}

function TileExercise({
  node,
  disabled,
  onAnswer,
}: {
  node: TileLessonNode
  disabled: boolean
  onAnswer: (result: AnswerResult) => void
}) {
  const [selected, setSelected] = useState<string[]>([])
  const selectedCounts = countValues(selected)
  const selectedText = selected.join('')
  const answered = disabled

  function addTile(tile: string) {
    if (disabled) return
    setSelected((current) => [...current, tile])
  }

  function removeTile(index: number) {
    if (disabled) return
    setSelected((current) => current.filter((_, itemIndex) => itemIndex !== index))
  }

  return (
    <div className="tile-exercise">
      {node.displayText ? <p className="tile-display-text">{node.displayText}</p> : null}
      <div className={answered ? 'tile-answer locked' : 'tile-answer'} aria-label="当前答案">
        {selected.length === 0 ? <span className="tile-placeholder">选择下方语块</span> : null}
        {selected.map((tile, index) => (
          <button type="button" key={`${tile}-${index}`} onClick={() => removeTile(index)} disabled={disabled}>
            {tile}
          </button>
        ))}
      </div>
      <div className="tile-bank" aria-label="语块选项">
        {node.bankTiles.map((tile, index) => {
          const used = selectedCounts[tile] ?? 0
          const available = node.bankTiles.filter((candidate) => candidate === tile).length
          const hidden = used >= available
          return (
            <button type="button" key={`${tile}-${index}`} disabled={disabled || hidden} onClick={() => addTile(tile)}>
              {tile}
            </button>
          )
        })}
      </div>
      <button
        className="primary-action"
        type="button"
        disabled={disabled || selected.length === 0}
        onClick={() => onAnswer({ correct: selectedText === node.targetText, selected, answer: node.targetTiles })}
      >
        检查
      </button>
    </div>
  )
}

function LessonFeedbackDock({
  node,
  feedback,
  isLast,
  onNext,
}: {
  node: LessonNode
  feedback: AnswerResult | null
  isLast: boolean
  onNext: () => void
}) {
  if (!feedback) {
    return (
      <footer className="lesson-feedback idle">
        <span>完成当前操作后检查答案。</span>
      </footer>
    )
  }

  return (
    <footer className={feedback.correct ? 'lesson-feedback correct' : 'lesson-feedback wrong'}>
      <div className="lesson-feedback-status">
        {feedback.correct ? <Check size={28} /> : <X size={28} />}
        <div>
          <strong>{feedback.correct ? '正确' : '需要回炉'}</strong>
          <p>{feedback.correct ? node.explanation : `正确答案：${formatAnswer(feedback.answer)}。${node.explanation}`}</p>
        </div>
      </div>
      <button className="primary-action" type="button" onClick={onNext}>
        {isLast ? '完成' : '继续'}
      </button>
    </footer>
  )
}

function buildLessonNextActions(lesson: LessonPlan) {
  const modes = {
    mixed: [
      { label: '词汇专项', search: { mode: 'vocab' } },
      { label: '语法专项', search: { mode: 'grammar' } },
      { label: '跟读前置', search: { mode: 'shadowing' } },
    ],
    vocab: [
      { label: '语法专项', search: { mode: 'grammar' } },
      { label: '综合训练', search: { mode: 'mixed' } },
    ],
    grammar: [
      { label: '跟读前置', search: { mode: 'shadowing' } },
      { label: '综合训练', search: { mode: 'mixed' } },
    ],
    shadowing: [
      { label: '错题回炉', search: { mode: 'review' } },
      { label: '综合训练', search: { mode: 'mixed' } },
    ],
    review: [
      { label: '综合训练', search: { mode: 'mixed' } },
      { label: '跟读前置', search: { mode: 'shadowing' } },
    ],
    target: [
      { label: '综合训练', search: { mode: 'mixed' } },
      { label: '错题回炉', search: { mode: 'review' } },
    ],
  } satisfies Record<LessonPlan['mode'], { label: string; search: { mode: string } }[]>
  return modes[lesson.mode].filter((action) => action.search.mode !== lesson.mode)
}

function formatAnswer(answer: unknown) {
  if (Array.isArray(answer)) return answer.join(' / ')
  if (typeof answer === 'string') return answer
  return JSON.stringify(answer)
}

function countValues(values: string[]) {
  return values.reduce<Record<string, number>>((counts, value) => {
    counts[value] = (counts[value] ?? 0) + 1
    return counts
  }, {})
}

function shuffleItems<T>(items: T[], seed: string) {
  const output = [...items]
  let state = [...seed].reduce((sum, char) => sum + char.charCodeAt(0), 0) || 1
  for (let index = output.length - 1; index > 0; index -= 1) {
    state = (state * 1664525 + 1013904223) % 4294967296
    const swapIndex = state % (index + 1)
    const value = output[index]
    output[index] = output[swapIndex]
    output[swapIndex] = value
  }
  return output
}
