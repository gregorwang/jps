import { useQuery } from '@tanstack/react-query'
import { Link } from '@tanstack/react-router'
import { PageHeader } from '../components/PageHeader'
import { getDeviceId } from '../lib/progress'
import { animeRepository } from '../server/repositories/animeRepository'

export function HistoryPage() {
  const historyQuery = useQuery({
    queryKey: ['history'],
    queryFn: () => animeRepository.getHistory(getDeviceId()),
  })
  const history = historyQuery.data

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="历史记录"
        title="AI 与练习记录"
        description="匿名设备级记录：造句批改按本设备保存，AI 精读/读空气/角色画像使用 Supabase 缓存。"
      />

      <section className="history-section">
        <p className="eyebrow">造句批改</p>
        <div className="history-list">
          {history?.corrections.map((item) => (
            <Link className="history-row" to="/history/$kind/$id" params={{ kind: 'correction', id: item.id }} key={item.id}>
              <strong>{item.promptText}</strong>
              <span>{item.summary}</span>
              <small>{item.workSlug ?? 'global'} · EP{item.episode ?? '-'} · {item.targetType} · {item.model} · {formatDate(item.createdAt)}</small>
            </Link>
          ))}
          {history?.corrections.length === 0 ? <span className="muted-text">还没有本设备的批改记录。</span> : null}
        </div>
      </section>

      <section className="history-section">
        <p className="eyebrow">AI 精读 / 读空气缓存</p>
        <div className="history-list">
          {history?.ai.map((item) => (
            <Link className="history-row" to="/history/$kind/$id" params={{ kind: 'ai', id: item.id }} key={item.id}>
              <strong>{item.title}</strong>
              <span>{item.summary}</span>
              <small>{item.workSlug ?? 'global'} · EP{item.episode ?? '-'} · {item.kind} · {item.model} · {formatDate(item.updatedAt)}</small>
            </Link>
          ))}
          {history?.ai.length === 0 ? <span className="muted-text">还没有 AI 缓存记录。</span> : null}
        </div>
      </section>

      <section className="history-section">
        <p className="eyebrow">角色画像缓存</p>
        <div className="history-list">
          {history?.profiles.map((item) => (
            <Link className="history-row" to="/history/$kind/$id" params={{ kind: 'profile', id: item.id }} key={item.id}>
              <strong>{item.title}</strong>
              <span>{item.summary}</span>
              <small>{item.workSlug} · {item.characterKey} · {item.model} · {formatDate(item.updatedAt)}</small>
            </Link>
          ))}
          {history?.profiles.length === 0 ? <span className="muted-text">还没有角色画像缓存。</span> : null}
        </div>
      </section>
    </section>
  )
}

function formatDate(value?: string) {
  if (!value) return '未记录时间'
  return new Intl.DateTimeFormat('zh-Hans', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
