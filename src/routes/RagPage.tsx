import { Link } from '@tanstack/react-router'
import { Bot, Save, Search, Wand2 } from 'lucide-react'
import { useState } from 'react'
import { PageHeader } from '../components/PageHeader'
import { usePreferredGatewayModel } from '../lib/aiPreferences'
import { getDeviceId } from '../lib/progress'
import { usePreferredReasoningEffort } from '../lib/reasoningPreferences'
import { readEpisodeScope } from '../lib/episodeScope'

type RagWorkSlug = 'rezero' | 'k-on'

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

const airDraftKey = 'anime-japanese-lab-air-question-drafts'
const ragWorks: { slug: RagWorkSlug; label: string; episodeCount: number }[] = [
  { slug: 'rezero', label: 'Re:Zero S1 新编（25 集）', episodeCount: 25 },
  { slug: 'k-on', label: 'K-ON!', episodeCount: 14 },
]

function initialRagWork(): RagWorkSlug | '' {
  const scope = readEpisodeScope()
  if (scope.workSlug === 'k-on') return 'k-on'
  if (scope.workSlug === 'rezero' || scope.workSlug === 're-zero') return 'rezero'
  return ''
}

export function RagPage() {
  const [query, setQuery] = useState('')
  const [selectedWork, setSelectedWork] = useState<RagWorkSlug | ''>(() => initialRagWork())
  const [selectedEpisode, setSelectedEpisode] = useState('all')
  const [suggestion, setSuggestion] = useState<RagTrainingSuggestion | null>(null)
  const model = usePreferredGatewayModel()
  const reasoningEffort = usePreferredReasoningEffort()
  const [result, setResult] = useState<RagResponse | null>(null)
  const [status, setStatus] = useState<'idle' | 'planning' | 'loading' | 'error'>('idle')
  const [generatingSourceId, setGeneratingSourceId] = useState('')
  const [batchGenerating, setBatchGenerating] = useState(false)
  const [draftTexts, setDraftTexts] = useState<Record<string, string>>({})
  const [saveMessage, setSaveMessage] = useState('')

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
      const episode = selectedEpisode === 'all' ? undefined : Number(selectedEpisode)
      const response = await fetch('/api/rag/search', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: trimmedQuery, workSlug: selectedWork, episode, topK: 5, model, reasoningEffort, deviceId: getDeviceId() }),
      })
      if (!response.ok) {
        throw new Error(await response.text())
      }
      setResult((await response.json()) as RagResponse)
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
    } catch (error) {
      console.error(error)
      setSaveMessage('生成失败，请换一个场景或稍后重试。')
    } finally {
      setGeneratingSourceId('')
    }
  }

  async function generateAllQuestions() {
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
      setSaveMessage(`已生成 ${Object.keys(nextDraftTexts).length} 道候选题，可编辑后保存全部。`)
    } catch (error) {
      console.error(error)
      setSaveMessage('批量生成失败，请稍后重试或单独生成题目。')
    } finally {
      setBatchGenerating(false)
    }
  }

  function saveDraft(sourceId: string) {
    try {
      const parsed = JSON.parse(draftTexts[sourceId] ?? '') as AirQuestionCandidate
      const existing = JSON.parse(window.localStorage.getItem(airDraftKey) ?? '[]') as AirQuestionCandidate[]
      window.localStorage.setItem(airDraftKey, JSON.stringify([parsed, ...existing].slice(0, 50)))
      setSaveMessage('已保存为本地草稿题。现在可以去学习页作答，确认质量后再发布到正式题库。')
    } catch {
      setSaveMessage('JSON 格式不对，先修正预览内容再保存。')
    }
  }

  function saveAllDrafts() {
    try {
      const parsed = Object.values(draftTexts)
        .map((text) => JSON.parse(text) as AirQuestionCandidate)
        .filter((draft) => draft.question && draft.options?.length)
      const existing = JSON.parse(window.localStorage.getItem(airDraftKey) ?? '[]') as AirQuestionCandidate[]
      window.localStorage.setItem(airDraftKey, JSON.stringify([...parsed, ...existing].slice(0, 50)))
      setSaveMessage(`已保存 ${parsed.length} 道本地草稿题。`)
    } catch {
      setSaveMessage('有候选题 JSON 格式不对，先修正预览内容再保存。')
    }
  }

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="读空气训练"
        title="AI 找场景，人工预览成题"
        description="不用先想搜索词。让 AI 选择适合练习的语用目标，再用 RAG 找字幕片段，生成草稿题后保存到学习页作答。"
        actions={
          <Link className="icon-button secondary" to="/linguistic-training">
            <Bot size={18} />
            <span>去读空气作答</span>
          </Link>
        }
      />
      <section className="source-preview">
        <p className="eyebrow">生成流程</p>
        <strong>RAG 场景检索 → AI 草稿题 → 人工预览保存 → 学习页作答</strong>
        <span>主流程由 AI 决定训练方向和检索意图。手动搜索只作为高级入口保留，用来补查指定场景。</span>
        <div className="episode-scope-panel">
          <label className="filter-field compact-field">
            <span>Work</span>
            <select
              value={selectedWork}
              onChange={(event) => {
                setSelectedWork(event.target.value as RagWorkSlug | '')
                setSelectedEpisode('all')
                setResult(null)
              }}
            >
              <option value="">先选择作品</option>
              {ragWorks.map((work) => (
                <option key={work.slug} value={work.slug}>{work.label}</option>
              ))}
            </select>
          </label>
          <label className="filter-field compact-field">
            <span>Episode</span>
            <select
              value={selectedEpisode}
              onChange={(event) => {
                setSelectedEpisode(event.target.value)
                setResult(null)
              }}
              disabled={!selectedWork}
            >
              <option value="all">全系列</option>
              {Array.from({ length: ragWorks.find((work) => work.slug === selectedWork)?.episodeCount ?? 0 }, (_, index) => index + 1).map((episode) => (
                <option key={episode} value={episode}>EP{String(episode).padStart(2, '0')}</option>
              ))}
            </select>
          </label>
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
                disabled={batchGenerating || result.sources.length === 0}
                onClick={() => void generateAllQuestions()}
              >
                <Bot size={18} />
                <span>{batchGenerating ? '批量生成中' : `一键生成 ${Math.min(result.sources.length, 5)} 道题`}</span>
              </button>
              <button
                className="icon-button secondary"
                type="button"
                disabled={Object.keys(draftTexts).length === 0}
                onClick={saveAllDrafts}
              >
                <Save size={18} />
                <span>保存全部草稿</span>
              </button>
            </div>
          </div>
          {result.sources.map((source) => (
            <article className="source-preview" key={source.id}>
              <p className="eyebrow">score {source.score.toFixed(4)}</p>
              <strong>
                {source.work} · EP{String(source.episode).padStart(2, '0')} · chunk {source.chunkNo} · {source.startTime}-{source.endTime}
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
                  disabled={generatingSourceId === source.id}
                  onClick={() => void generateQuestion(source)}
                >
                  <Bot size={18} />
                  <span>{generatingSourceId === source.id ? '生成中' : 'AI 生成题目'}</span>
                </button>
              </div>
              {draftTexts[source.id] ? (
                <section className="question-draft-panel">
                  <p className="eyebrow">候选题预览</p>
                  <textarea
                    value={draftTexts[source.id]}
                    rows={16}
                    onChange={(event) => setDraftTexts((current) => ({ ...current, [source.id]: event.target.value }))}
                  />
                  <div className="card-actions">
                    <button className="primary-action" type="button" onClick={() => saveDraft(source.id)}>
                      <Save size={18} />
                      <span>保存为本地草稿</span>
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
          ))}
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
