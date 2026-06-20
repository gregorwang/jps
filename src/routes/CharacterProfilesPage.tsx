import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Bot, RefreshCw } from 'lucide-react'
import { PageHeader } from '../components/PageHeader'
import { StructuredAiResultView } from '../components/StructuredAiResultView'
import { usePreferredGatewayModel } from '../lib/aiPreferences'
import { usePreferredReasoningEffort } from '../lib/reasoningPreferences'
import type { StructuredAiResult } from '../lib/types'
import { animeRepository } from '../server/repositories/animeRepository'

type CharacterOption = {
  key: string
  name: string
}

const characterPresets: Record<string, CharacterOption[]> = {
  'k-on': [
    { key: 'yui', name: '唯' },
    { key: 'mio', name: '澪' },
    { key: 'ritsu', name: '律' },
    { key: 'mugi', name: '紬' },
    { key: 'azusa', name: '梓' },
  ],
  're-zero': [
    { key: 'subaru', name: 'スバル' },
    { key: 'emilia', name: 'エミリア' },
    { key: 'rem', name: 'レム' },
    { key: 'ram', name: 'ラム' },
    { key: 'beatrice', name: 'ベアトリス' },
  ],
}

const defaultWorkSlug = 'k-on'
const fallbackCharacters: CharacterOption[] = [{ key: 'custom', name: '自定义角色' }]

type ProfileSource = {
  id: string
  score: number
  work: string
  episode: number
  chunkNo: number
  startTime: string
  endTime: string
  text: string
}

type CharacterProfileResult = StructuredAiResult & {
  model?: string
  cachedAt?: string
  cacheWarning?: string
  sources?: ProfileSource[]
}

export function CharacterProfilesPage() {
  const worksQuery = useQuery({
    queryKey: ['works'],
    queryFn: () => animeRepository.listWorks(),
  })
  const [workSlug, setWorkSlug] = useState(defaultWorkSlug)
  const [characterKey, setCharacterKey] = useState(characterPresets[defaultWorkSlug][0].key)
  const [customCharacterName, setCustomCharacterName] = useState('')
  const model = usePreferredGatewayModel()
  const reasoningEffort = usePreferredReasoningEffort()
  const [enabled, setEnabled] = useState(false)
  const [regenerateCount, setRegenerateCount] = useState(0)
  const works = worksQuery.data ?? []
  const selectedWork = works.find((work) => work.slug === workSlug) ?? works[0]
  const selectedWorkSlug = selectedWork?.slug ?? workSlug
  const selectedWorkName = selectedWork?.displayName ?? selectedWorkSlug
  const characters = characterPresets[selectedWorkSlug] ?? fallbackCharacters
  const selectedCharacter = characters.find((item) => item.key === characterKey) ?? characters[0]
  const isCustomCharacter = selectedCharacter.key === 'custom'
  const characterName = isCustomCharacter ? customCharacterName.trim() : selectedCharacter.name
  const resolvedCharacterKey = isCustomCharacter ? normalizeCharacterKey(characterName) : selectedCharacter.key
  const canGenerate = Boolean(characterName)

  useEffect(() => {
    if (!selectedWork && works[0]) {
      setWorkSlug(works[0].slug)
    }
  }, [selectedWork, works])

  const profileQuery = useQuery({
    queryKey: ['character-profile', selectedWorkSlug, resolvedCharacterKey, characterName, model, reasoningEffort, regenerateCount],
    enabled: enabled && canGenerate,
    queryFn: async () =>
      animeRepository.getCharacterProfile({
        workSlug: selectedWorkSlug,
        characterKey: resolvedCharacterKey,
        characterName,
        model,
        reasoningEffort,
        regenerate: regenerateCount > 0,
      }) as Promise<CharacterProfileResult>,
  })
  const profile = profileQuery.data
  const profileError = profileQuery.error instanceof Error ? profileQuery.error.message : ''

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="角色语言画像"
        title={`${selectedWorkName} 角色说话习惯`}
        description="基于所选番剧字幕 RAG 和 AI 生成缓存画像；新番剧可以先输入角色名做占位分析。"
      />
      <div className="control-strip">
        <label className="filter-field compact-field">
          <span>番剧</span>
          <select
            value={selectedWorkSlug}
            onChange={(event) => {
              const nextWorkSlug = event.target.value
              const nextCharacters = characterPresets[nextWorkSlug] ?? fallbackCharacters
              setWorkSlug(nextWorkSlug)
              setCharacterKey(nextCharacters[0].key)
              setCustomCharacterName('')
              setEnabled(false)
              setRegenerateCount(0)
            }}
          >
            {works.map((work) => (
              <option key={work.slug} value={work.slug}>
                {work.displayName}
              </option>
            ))}
          </select>
        </label>
        <div className="segmented-control" aria-label="角色">
          {characters.map((item) => (
            <button
              className={item.key === selectedCharacter.key ? 'selected' : ''}
              type="button"
              key={item.key}
              onClick={() => {
                setCharacterKey(item.key)
                setEnabled(false)
                setRegenerateCount(0)
              }}
            >
              {item.name}
            </button>
          ))}
        </div>
        {isCustomCharacter ? (
          <label className="filter-field compact-field">
            <span>角色名</span>
            <input
              type="text"
              value={customCharacterName}
              onChange={(event) => {
                setCustomCharacterName(event.target.value)
                setEnabled(false)
                setRegenerateCount(0)
              }}
              placeholder="例如：スバル / 爱蜜莉雅"
            />
          </label>
        ) : null}
        <button className="primary-action" type="button" disabled={profileQuery.isFetching || !canGenerate} onClick={() => setEnabled(true)}>
          <Bot size={18} />
          <span>{profileQuery.isFetching ? '生成中' : '生成画像'}</span>
        </button>
        <button
          className="icon-button secondary"
          type="button"
          disabled={profileQuery.isFetching || !canGenerate}
          onClick={() => {
            setEnabled(true)
            setRegenerateCount((count) => count + 1)
          }}
        >
          <RefreshCw size={18} />
          <span>重新生成</span>
        </button>
      </div>
      {profileError ? (
        <div className="source-preview error-panel" role="alert">
          <p className="eyebrow">生成失败</p>
          <strong>角色画像暂时不可用</strong>
          <span>{profileError}</span>
        </div>
      ) : null}
      {profile ? (
        <>
          {profile.cacheWarning ? (
            <div className="source-preview error-panel" role="status">
              <p className="eyebrow">缓存提醒</p>
              <strong>本次画像已生成</strong>
              <span>{profile.cacheWarning}</span>
            </div>
          ) : null}
          <div className="source-preview">
            <p className="eyebrow">缓存信息</p>
            <strong>{profile.model ?? model}</strong>
            <span>{profile.cachedAt ? `缓存时间：${new Date(profile.cachedAt).toLocaleString('zh-Hans')}` : '缓存时间：当前返回未记录'}</span>
          </div>
          <StructuredAiResultView result={profile} />
          <section className="history-section">
            <p className="eyebrow">RAG 来源</p>
            <div className="history-list">
              {profile.sources?.slice(0, 5).map((source) => (
                <article className="history-row" key={source.id}>
                  <strong>
                    {source.work || selectedWorkSlug} · EP{String(source.episode).padStart(2, '0')} · chunk {source.chunkNo}
                  </strong>
                  <span>{source.text?.slice(0, 260) || '该来源没有文本摘要。'}</span>
                  <small>score {source.score.toFixed(4)} · {source.startTime}-{source.endTime}</small>
                </article>
              ))}
              {profile.sources?.length === 0 ? <span className="muted-text">缓存里没有记录来源，可点重新生成。</span> : null}
            </div>
          </section>
        </>
      ) : null}
      {!profile ? (
        <div className="source-preview">
          <p className="eyebrow">占位说明</p>
          <strong>{characterName || '所选角色'} 的画像会按番剧缓存到 Supabase</strong>
          <span>{selectedWorkName} 和其他番剧会使用各自的 work_slug，不再混在 K-ON! 下面。</span>
        </div>
      ) : null}
    </section>
  )
}

function normalizeCharacterKey(name: string) {
  const normalized = name.trim().toLowerCase().replace(/\s+/g, '-')
  return normalized || 'custom'
}
