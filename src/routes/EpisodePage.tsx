import { Link, useParams } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { BookOpen, Brain, Captions, Mic2, PenLine, Sparkles } from 'lucide-react'
import { PageHeader } from '../components/PageHeader'
import { StatCard } from '../components/StatCard'
import { animeRepository } from '../server/repositories/animeRepository'

export function EpisodePage() {
  const { workSlug, episode } = useParams({ strict: false })
  const episodeNo = Number(episode ?? 1)
  const episodeQuery = useQuery({
    queryKey: ['episode', workSlug, episodeNo],
    queryFn: () => animeRepository.getEpisode(workSlug ?? 'k-on', episodeNo),
  })
  const planQuery = useQuery({
    queryKey: ['episode-plan', workSlug, episodeNo],
    queryFn: () => animeRepository.getEpisodePlan(workSlug ?? 'k-on', episodeNo),
  })

  const current = episodeQuery.data
  const plan = planQuery.data

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="单集学习中心"
        title={`${current?.workDisplayName ?? 'K-ON!'} EP${String(episodeNo).padStart(2, '0')}`}
        description={plan?.notes ? `${plan.notes}。优先完成训练题，再查看词表、语法说明和台词。` : '该集学习材料等待从 Supabase 读取。'}
      />

      <div className="stat-grid">
        <StatCard label="日文台词" value={current?.jaLines ?? '-'} />
        <StatCard label="中文对照" value={current?.zhLines ?? '-'} />
        <StatCard label="场景 chunks" value={current?.chunkCount ?? '-'} />
        <StatCard label="学习练习" value={plan?.exerciseCount ?? '-'} />
      </div>

      <div className="task-grid">
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/vocab" params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <Sparkles size={22} />
          <strong>词汇训练</strong>
          <span>选择题 + 即时反馈</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/grammar" params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <Brain size={22} />
          <strong>语法训练</strong>
          <span>字幕例句判断题</span>
        </Link>
        <Link className="task-card" to="/rag">
          <BookOpen size={22} />
          <strong>读空气训练</strong>
          <span>场景语气判断题</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/sentences" params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <Mic2 size={22} />
          <strong>本集跟读句</strong>
          <span>{plan?.shadowingCount ?? 0} 个句子</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/writing" params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <PenLine size={22} />
          <strong>手写练习</strong>
          <span>{plan?.handwritingCount ?? 0} 个词，连续描红</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/subtitles" params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <Captions size={22} />
          <strong>台词浏览</strong>
          <span>按时间线看日文原文。</span>
        </Link>
      </div>
    </section>
  )
}
