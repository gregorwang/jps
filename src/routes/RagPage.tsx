import { Link } from '@tanstack/react-router'
import { useQueryClient } from '@tanstack/react-query'
import { Bot, Save, Search, Wand2 } from 'lucide-react'
import { useState } from 'react'
import { PageHeader } from '../components/PageHeader'
import { usePreferredGatewayModel } from '../lib/aiPreferences'
import { getDeviceId } from '../lib/progress'
import { usePreferredReasoningEffort } from '../lib/reasoningPreferences'
import { readEpisodeScope } from '../lib/episodeScope'

type RagWorkSlug = 'rezero' | 'k-on'
type RagScopeMode = 'all' | 'season' | 'episode'

type RagSeason = {
  season: number
  label: string
  startEpisode: number
  endEpisode: number
}

type RagLine = {
  lineNo: number
  startTime: string
  endTime: string
  jaText: string
  zhText: string
}

type RagSource = {
  id: string
  score: number
  work: string
  episode: number
  chunkNo: number
  startTime: string
  endTime: string
  text: string
  lines: RagLine[]
}

type RagResponse = {
  query: string
  sources: RagSource[]
  analysis: {
    title: string
    summary: string
    bullets: string[]
  }
}

type AirQuestionCandidate = {
  sceneJa: string
  sceneZh: string
  sceneLines?: {
    lineNo?: number
    speaker?: string
    jaText: string
    zhText?: string
    isTarget?: boolean
  }[]
  targetLineNo?: number
  question: string
  options: string[]
  answer: string
  explanation: string
  evidence: string[]
  source: {
    work: string
    episode: number
    chunkNo: number
    startTime: string
    endTime: string
    sourceId?: string
  }
}

type RagTrainingSuggestion = {
  query: string
  focus: string
  reason: string
}

const ragWorks: { slug: RagWorkSlug; label: string; seasons: RagSeason[]; coverageNote: string }[] = [
  {
    slug: 'rezero',
    label: 'Re:Zero',
    seasons: [
      { season: 1, label: 'S1', startEpisode: 1, endEpisode: 25 },
      { season: 2, label: 'S2', startEpisode: 26, endEpisode: 50 },
      { season: 3, label: 'S3', startEpisode: 51, endEpisode: 66 },
    ],
    coverageNote: '当前向量库已索引 S1-S3，EP01-EP66。',
  },
  {
    slug: 'k-on',
    label: 'K-ON!',
    seasons: [
      { season: 1, label: 'TV', startEpisode: 1, endEpisode: 14 },
    ],
    coverageNote: '当前向量库索引到 EP01-EP14。',
  },
]

function seasonForEpisode(work: { seasons: RagSeason[] } | undefined, episode: number) {
  return work?.seasons.find((season) => episode >= season.startEpisode && episode <= season.endEpisode) ?? work?.seasons[0]
}

function formatEpisodeOption(work: { slug: RagWorkSlug; seasons: RagSeason[] } | undefined, episode: number) {
  const season = seasonForEpisode(work, episode)
  if (!work || !season || work.slug !== 'rezero') return `EP${String(episode).padStart(2, '0')}`
  const seasonEpisode = episode - season.startEpisode + 1
  return `${season.label} EP${String(seasonEpisode).padStart(2, '0')}（全局 EP${String(episode).padStart(2, '0')}）`
}

function formatSourceEpisode(workSlug: string, episode: number) {
  const work = ragWorks.find((item) => item.slug === workSlug)
  return formatEpisodeOption(work, episode)
}

function episodeRange(season: RagSeason | undefined) {
  if (!season) return []
  return Array.from({ length: season.endEpisode - season.startEpisode + 1 }, (_, index) => season.startEpisode + index)
}

function initialRagWork(): RagWorkSlug | '' {
  const scope = readEpisodeScope()
  if (scope.workSlug === 'k-on') return 'k-on'
  if (scope.workSlug === 'rezero' || scope.workSlug === 're-zero') return 'rezero'
  return ''
}

function draftSignature(text: string | undefined) {
  return text?.trim() ?? ''
}

export function RagPage() {
  const queryClient = useQueryClient()
  const [query, setQuery] = useState('')
  const [selectedWork, setSelectedWork] = useState<RagWorkSlug | ''>(() => initialRagWork())
  const [scopeMode, setScopeMode] = useState<RagScopeMode>(() => initialRagWork() === 'rezero' ? 'season' : 'all')
  const [selectedSeason, setSelectedSeason] = useState(1)
  const [selectedEpisode, setSelectedEpisode] = useState(1)
  const [suggestion, setSuggestion] = useState<RagTrainingSuggestion | null>(null)
  const model = usePreferredGatewayModel()
  const reasoningEffort = usePreferredReasoningEffort()
  const [result, setResult] = useState<RagResponse | null>(null)
  const [status, setStatus] = useState<'idle' | 'planning' | 'loading' | 'error'>('idle')
  const [generatingSourceId, setGeneratingSourceId] = useState('')
  const [batchGenerating, setBatchGenerating] = useState(false)
  const [draftTexts, setDraftTexts] = useState<Record<string, string>>({})
  const [savedDraftSignatures, setSavedDraftSignatures] = useState<Record<string, string>>({})
  const [saveMessage, setSaveMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const selectedRagWork = ragWorks.find((work) => work.slug === selectedWork)
  const supportsSeasonScope = selectedRagWork?.slug === 'rezero'
  const selectedSeasonConfig = selectedRagWork?.seasons.find((season) => season.season === selectedSeason) ?? selectedRagWork?.seasons[0]
  const selectedEpisodeOptions = episodeRange(selectedSeasonConfig)
  const draftEntries = Object.entries(draftTexts)
  const hasDrafts = draftEntries.length > 0
  const hasUnsavedDrafts = draftEntries.some(([sourceId, text]) => savedDraftSignatures[sourceId] !== draftSignature(text))
  const isBusy = saving || batchGenerating || Boolean(generatingSourceId)

  function resetScopeForWork(nextWork: RagWorkSlug | '') {
    const nextRagWork = ragWorks.find((work) => work.slug === nextWork)
    const firstSeason = nextRagWork?.seasons[0]
    setScopeMode(nextRagWork?.slug === 'rezero' ? 'season' : 'all')
    setSelectedSeason(firstSeason?.season ?? 1)
    setSelectedEpisode(firstSeason?.startEpisode ?? 1)
    setResult(null)
    setDraftTexts({})
    setSavedDraftSignatures({})
  }

  function requestScope() {
    if (!selectedRagWork) return {}
    if (scopeMode === 'episode') return { episode: selectedEpisode }
    if (scopeMode === 'season' && supportsSeasonScope) return { season: selectedSeasonConfig?.season }
    return {}
  }

  async function search(nextQuery = query) {
    const trimmedQuery = nextQuery.trim()
    if (!trimmedQuery) return
    if (!selectedWork) {
      setSaveMessage('请先选择作品，再检索字幕。')
      return
    }
    setStatus('loading')
    setSaveMessage('')
    try {
      const scope = requestScope()
      const response = await fetch('/api/rag/search', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: trimmedQuery, workSlug: selectedWork, ...scope, topK: 5, model, reasoningEffort, deviceId: getDeviceId() }),
      })
      if (!response.ok) {
        throw new Error(await response.text())
      }
      setResult((await response.json()) as RagResponse)
      setDraftTexts({})
      setSavedDraftSignatures({})
      setStatus('idle')
    } catch (error) {
      console.error(error)
      setSaveMessage('检索失败，请确认作品/集数后重试。')
      setStatus('error')
    }
  }

  async function autoFindTrainingScenes() {
    if (!selectedWork) {
      setSaveMessage('请先选择作品，再让 AI 自动找场景。')
      return
    }
    setStatus('planning')
    setSaveMessage('')
    try {
      const response = await fetch('/api/rag/suggest-training-query', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ workSlug: selectedWork, model, reasoningEffort, deviceId: getDeviceId() }),
      })
      if (!response.ok) throw new Error(await response.text())
      const nextSuggestion = (await response.json()) as RagTrainingSuggestion
      setSuggestion(nextSuggestion)
      setQuery(nextSuggestion.query)
      await search(nextSuggestion.query)
    } catch (error) {
      console.error(error)
      const fallback = {
        query: '找一个角色没有直接说出口、但语气或反应里有潜台词的日常对话场景。',
        focus: '潜台词判断',
        reason: 'AI 规划失败时使用默认训练方向。',
      }
      setSuggestion(fallback)
      setQuery(fallback.query)
      await search(fallback.query)
    }
  }

  async function generateQuestion(source: RagSource) {
    if (saving || batchGenerating || generatingSourceId) return
    setGeneratingSourceId(source.id)
    setSaveMessage('')
    try {
      const response = await fetch('/api/rag/generate-question', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ source, model, reasoningEffort, deviceId: getDeviceId() }),
      })
      if (!response.ok) throw new Error(await response.text())
      const candidate = (await response.json()) as AirQuestionCandidate
      setDraftTexts((current) => ({ ...current, [source.id]: JSON.stringify(candidate, null, 2) }))
      setSavedDraftSignatures((current) => {
        const next = { ...current }
        delete next[source.id]
        return next
      })
      setSaveMessage('已重新生成候选题，请确认后写入数据库。')
    } catch (error) {
      console.error(error)
      setSaveMessage('生成失败，请换一个场景或稍后重试。')
    } finally {
      setGeneratingSourceId('')
    }
  }

  async function generateAllQuestions() {
    if (saving || batchGenerating) return
    if (!result?.sources.length) return
    setBatchGenerating(true)
    setSaveMessage('')
    try {
      const sources = result.sources.slice(0, 5)
      const response = await fetch('/api/rag/generate-questions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sources, model, reasoningEffort, deviceId: getDeviceId() }),
      })
      if (!response.ok) throw new Error(await response.text())
      const data = (await response.json()) as { candidates?: AirQuestionCandidate[] }
      const nextDraftTexts = Object.fromEntries(
        sources.map((source, index) => [source.id, JSON.stringify(data.candidates?.[index] ?? {}, null, 2)]),
      )
      setDraftTexts(nextDraftTexts)
      setSavedDraftSignatures({})
      setSaveMessage(`已生成 ${Object.keys(nextDraftTexts).length} 道候选题，可编辑后保存全部。`)
    } catch (error) {
      console.error(error)
      setSaveMessage('批量生成失败，请稍后重试或单独生成题目。')
    } finally {
      setBatchGenerating(false)
    }
  }

  async function saveDraft(sourceId: string) {
    if (saving) return
    try {
      setSaving(true)
      const signature = draftSignature(draftTexts[sourceId])
      if (savedDraftSignatures[sourceId] === signature) {
        setSaveMessage('这道题已经写入数据库，没有重复提交。')
        return
      }
      const parsed = JSON.parse(draftTexts[sourceId] ?? '') as AirQuestionCandidate
      const response = await fetch('/api/rag/save-question', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ candidate: parsed }),
      })
      if (!response.ok) throw new Error(await readApiError(response))
      await queryClient.invalidateQueries({ queryKey: ['linguistic-exercises'] })
      await queryClient.invalidateQueries({ queryKey: ['episode-linguistics'] })
      setSavedDraftSignatures((current) => ({ ...current, [sourceId]: signature }))
      setSaveMessage('已写入数据库题库。去读空气作答或单集语言学题库即可看到。')
    } catch (error) {
      console.error(error)
      setSaveMessage(error instanceof Error ? error.message : '保存失败，请检查 JSON 或登录状态。')
    } finally {
      setSaving(false)
    }
  }

  async function saveAllDrafts() {
    if (saving) return
    try {
      setSaving(true)
      const entries = Object.entries(draftTexts)
        .filter(([sourceId, text]) => savedDraftSignatures[sourceId] !== draftSignature(text))
      const parsedEntries = entries
        .map(([sourceId, text]) => [sourceId, JSON.parse(text) as AirQuestionCandidate] as const)
        .filter(([, draft]) => draft.question && draft.options?.length)
      if (parsedEntries.length === 0) {
        setSaveMessage('当前候选题都已经写入数据库，没有重复提交。')
        return
      }
      const response = await fetch('/api/rag/save-questions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ candidates: parsedEntries.map(([, draft]) => draft) }),
      })
      if (!response.ok) throw new Error(await readApiError(response))
      await queryClient.invalidateQueries({ queryKey: ['linguistic-exercises'] })
      await queryClient.invalidateQueries({ queryKey: ['episode-linguistics'] })
      setSavedDraftSignatures((current) => ({
        ...current,
        ...Object.fromEntries(parsedEntries.map(([sourceId]) => [sourceId, draftSignature(draftTexts[sourceId])])),
      }))
      setSaveMessage(`已写入数据库题库 ${parsedEntries.length} 道。`)
    } catch (error) {
      console.error(error)
      setSaveMessage(error instanceof Error ? error.message : '批量保存失败，请检查 JSON 或登录状态。')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="读空气训练"
        title="AI 找场景，人工预览成题"
        description="不用先想搜索词。让 AI 选择适合练习的语用目标，再用 RAG 找字幕片段，预览后直接写入数据库题库。"
        actions={
          <Link className="icon-button secondary" to="/linguistic-training">
            <Bot size={18} />
            <span>去读空气作答</span>
          </Link>
        }
      />
      <section className="source-preview">
        <p className="eyebrow">生成流程</p>
        <strong>RAG 场景检索 → AI 生成题 → 人工预览 → 写入数据库题库</strong>
        <span>主流程由 AI 决定训练方向和检索意图。手动搜索只作为高级入口保留，用来补查指定场景。</span>
        <div className="episode-scope-panel">
          <label className="filter-field compact-field">
            <span>Work</span>
            <select
              value={selectedWork}
              onChange={(event) => {
                const nextWork = event.target.value as RagWorkSlug | ''
                setSelectedWork(nextWork)
                resetScopeForWork(nextWork)
              }}
            >
              <option value="">先选择作品</option>
              {ragWorks.map((work) => (
                <option key={work.slug} value={work.slug}>{work.label}</option>
              ))}
            </select>
          </label>
          <label className="filter-field compact-field">
            <span>召回范围</span>
            <select
              value={scopeMode}
              onChange={(event) => {
                setScopeMode(event.target.value as RagScopeMode)
                setResult(null)
              }}
              disabled={!selectedWork}
            >
              <option value="all">全作品</option>
              {supportsSeasonScope ? <option value="season">按季</option> : null}
              <option value="episode">单集</option>
            </select>
          </label>
          {scopeMode !== 'all' && (supportsSeasonScope || scopeMode === 'episode') ? (
            <label className="filter-field compact-field">
              <span>Season</span>
              <select
                value={selectedSeason}
                onChange={(event) => {
                  const nextSeason = Number(event.target.value)
                  const season = selectedRagWork?.seasons.find((item) => item.season === nextSeason)
                  setSelectedSeason(nextSeason)
                  setSelectedEpisode(season?.startEpisode ?? 1)
                  setResult(null)
                }}
                disabled={!selectedWork}
              >
                {selectedRagWork?.seasons.map((season) => (
                  <option key={season.season} value={season.season}>{season.label}</option>
                ))}
              </select>
            </label>
          ) : null}
          {scopeMode === 'episode' ? (
            <label className="filter-field compact-field">
              <span>Episode</span>
              <select
                value={selectedEpisode}
                onChange={(event) => {
                  setSelectedEpisode(Number(event.target.value))
                  setResult(null)
                }}
                disabled={!selectedWork}
              >
                {selectedEpisodeOptions.map((episode) => (
                  <option key={episode} value={episode}>{formatEpisodeOption(selectedRagWork, episode)}</option>
                ))}
              </select>
            </label>
          ) : null}
          {scopeMode === 'season' && supportsSeasonScope && selectedSeasonConfig ? (
            <label className="filter-field compact-field">
              <span>范围</span>
              <select value={`${selectedSeasonConfig.startEpisode}-${selectedSeasonConfig.endEpisode}`} disabled>
                <option>{selectedSeasonConfig.label} · EP{String(selectedSeasonConfig.startEpisode).padStart(2, '0')}-EP{String(selectedSeasonConfig.endEpisode).padStart(2, '0')}</option>
              </select>
            </label>
          ) : null}
          {selectedRagWork ? <span className="muted-text">{selectedRagWork.coverageNote}</span> : null}
        </div>
        <div className="rag-auto-panel">
          <button className="primary-action" type="button" onClick={() => void autoFindTrainingScenes()} disabled={!selectedWork || status === 'planning' || status === 'loading'}>
            <Wand2 size={18} />
            <span>{status === 'planning' ? 'AI 选题中' : status === 'loading' ? '检索中' : 'AI 自动找可练片段'}</span>
          </button>
          {suggestion ? (
            <div className="rag-suggestion">
              <p className="eyebrow">AI 训练意图</p>
              <strong>{suggestion.focus}</strong>
              <span>{suggestion.reason}</span>
              <small>{suggestion.query}</small>
            </div>
          ) : null}
        </div>
        <details className="advanced-filter-panel">
          <summary>手动补查</summary>
        <form className="rag-box">
          <label htmlFor="rag-question">搜索问题</label>
          <textarea
            id="rag-question"
            rows={5}
            placeholder="例如：找一个角色表面答应、实际有点为难的场景。"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <button className="primary-action" type="button" onClick={() => void search()} disabled={!selectedWork || status === 'loading' || !query.trim()}>
            <Search size={18} />
            <span>{status === 'loading' ? '检索中' : status === 'error' ? '检索失败' : '检索相关场景'}</span>
          </button>
        </form>
        </details>
        {saveMessage ? <span className="muted-text">{saveMessage}</span> : null}
      </section>
      {result ? (
        <div className="card-list">
          <div className="source-preview">
            <p className="eyebrow">{result.analysis.title}</p>
            <strong>{result.analysis.summary}</strong>
            {result.analysis.bullets.map((item) => (
              <span key={item}>{item}</span>
            ))}
            <div className="card-actions">
              <button
                className="primary-action"
                type="button"
                disabled={isBusy || result.sources.length === 0}
                onClick={() => void generateAllQuestions()}
              >
                <Bot size={18} />
                <span>
                  {batchGenerating
                    ? '批量生成中'
                    : hasDrafts
                      ? `重新生成 ${Math.min(result.sources.length, 5)} 道题`
                      : `一键生成 ${Math.min(result.sources.length, 5)} 道题`}
                </span>
              </button>
              <button
                className="icon-button secondary"
                type="button"
                disabled={saving || batchGenerating || !hasUnsavedDrafts}
                onClick={() => void saveAllDrafts()}
              >
                <Save size={18} />
                <span>{saving ? '写入中' : hasDrafts && !hasUnsavedDrafts ? '已写入数据库' : '写入数据库'}</span>
              </button>
            </div>
          </div>
          {result.sources.map((source) => {
              const sourceDraft = draftTexts[source.id]
              const sourceSaved = Boolean(sourceDraft) && savedDraftSignatures[source.id] === draftSignature(sourceDraft)
              return (
              <article className="source-preview" key={source.id}>
              <p className="eyebrow">score {source.score.toFixed(4)}</p>
              <strong>
                {source.work} · {formatSourceEpisode(source.work, source.episode)} · chunk {source.chunkNo} · {source.startTime}-{source.endTime}
              </strong>
              <span>{source.text.slice(0, 240)}</span>
              <div className="mini-lines">
                {source.lines.slice(0, 6).map((line) => (
                  <p key={line.lineNo}>
                    <b>{line.lineNo}</b> {line.jaText}
                    <small>{line.zhText}</small>
                  </p>
                ))}
              </div>
              <div className="card-actions">
                <button
                  className="icon-button secondary"
                  type="button"
                  disabled={isBusy}
                  onClick={() => void generateQuestion(source)}
                >
                  <Bot size={18} />
                  <span>{generatingSourceId === source.id ? '生成中' : sourceDraft ? '重新生成题目' : 'AI 生成题目'}</span>
                </button>
              </div>
              {sourceDraft ? (
                <section className="question-draft-panel">
                  <p className="eyebrow">候选题预览</p>
                  <textarea
                    value={sourceDraft}
                    rows={16}
                    onChange={(event) => setDraftTexts((current) => ({ ...current, [source.id]: event.target.value }))}
                  />
                  <div className="card-actions">
                    <button className="primary-action" type="button" disabled={saving || batchGenerating || sourceSaved} onClick={() => void saveDraft(source.id)}>
                      <Save size={18} />
                      <span>{saving ? '写入中' : sourceSaved ? '已写入数据库' : '写入数据库'}</span>
                    </button>
                    <Link className="icon-button secondary" to="/linguistic-training">
                      <Bot size={18} />
                      <span>去作答</span>
                    </Link>
                  </div>
                  {saveMessage ? <span className="muted-text">{saveMessage}</span> : null}
                </section>
              ) : null}
              </article>
              )
            })}
        </div>
      ) : (
        <div className="source-preview">
          <p className="eyebrow">来源显示契约</p>
          <strong>K-ON! · EP01 · chunk 001 · 0:00:31.29-0:03:14.58</strong>
          <span>返回结果必须展示作品、集数、chunk 编号、时间范围和日文摘录。</span>
        </div>
      )}
    </section>
  )
}

async function readApiError(response: Response) {
  const text = await response.text()
  try {
    const data = JSON.parse(text) as { error?: { message?: string } }
    return data.error?.message ?? text
  } catch {
    return text
  }
}
