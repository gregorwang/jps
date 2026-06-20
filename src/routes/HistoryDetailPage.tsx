import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from '@tanstack/react-router'
import { RefreshCw } from 'lucide-react'
import { PageHeader } from '../components/PageHeader'
import { StructuredAiResultView } from '../components/StructuredAiResultView'
import { getDeviceId } from '../lib/progress'
import type { StructuredAiResult } from '../lib/types'
import { animeRepository } from '../server/repositories/animeRepository'

export function HistoryDetailPage() {
  const { kind, id } = useParams({ strict: false })
  const decodedId = decodeURIComponent(id ?? '')
  const detailQuery = useQuery({
    queryKey: ['history-detail', kind, decodedId],
    queryFn: () => animeRepository.getHistoryDetail(kind ?? '', decodedId, getDeviceId()),
  })
  const detail = detailQuery.data

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="历史详情"
        title={detail?.title ?? '记录详情'}
        description={detail?.summary ?? '查看来源、模型、缓存状态和结构化结果。'}
        actions={
          <Link className="icon-button secondary" to="/history">
            <RefreshCw size={18} />
            <span>返回历史</span>
          </Link>
        }
      />

      {detail ? (
        <>
          <section className="source-preview">
            <p className="eyebrow">{detail.cacheKind} · {detail.cacheStatus}</p>
            <strong>{detail.workSlug || 'global'} {detail.episode ? `· EP${String(detail.episode).padStart(2, '0')}` : ''}</strong>
            <span>source: {detail.sourceId || detail.promptText || detail.id}</span>
            <span>model: {detail.model || 'unknown'}</span>
            <span>created: {formatDate(detail.createdAt)}</span>
          </section>
          {isStructuredResult(detail.result) ? <StructuredAiResultView result={detail.result} /> : <pre className="json-preview">{JSON.stringify(detail.result, null, 2)}</pre>}
        </>
      ) : (
        <p className="muted-text">{detailQuery.isLoading ? '读取中...' : '没有找到这条记录。'}</p>
      )}
    </section>
  )
}

function isStructuredResult(value: unknown): value is StructuredAiResult {
  return Boolean(value && typeof value === 'object' && Array.isArray((value as StructuredAiResult).sections))
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
