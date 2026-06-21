import { Link, useParams } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { BookOpen, Brain, Captions, GraduationCap, Mic2, PenLine, PlayCircle, Sparkles } from 'lucide-react'
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
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/lesson" params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <PlayCircle size={22} />
          <strong>综合训练队列</strong>
          <span>词汇配对、听音拼句、语法填空和选择题混合推进。</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/lesson" search={{ mode: 'vocab' }} params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <Sparkles size={22} />
          <strong>词汇专项训练</strong>
          <span>配对和中文到日文选择为主。</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/lesson" search={{ mode: 'grammar' }} params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <Brain size={22} />
          <strong>语法专项训练</strong>
          <span>语法填空、功能判断和句意理解。</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/lesson" search={{ mode: 'shadowing' }} params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <Mic2 size={22} />
          <strong>跟读前置训练</strong>
          <span>优先原声，听音拼句和中文理解。</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/lesson" search={{ mode: 'review' }} params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <Brain size={22} />
          <strong>错题回炉</strong>
          <span>按本集弱项生成复习队列。</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/vocab" params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <Sparkles size={22} />
          <strong>词汇资料</strong>
          <span>查看词义、读音、出现次数和讲解。</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/grammar" params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <Brain size={22} />
          <strong>语法资料</strong>
          <span>查看本集语法点、例句和语气说明。</span>
        </Link>
        <Link className="task-card" to="/rag">
          <BookOpen size={22} />
          <strong>读空气训练</strong>
          <span>场景语气判断题</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/linguistics" params={{ workSlug: workSlug ?? 'k-on', episode: String(episodeNo) }}>
          <GraduationCap size={22} />
          <strong>语言学专项</strong>
          <span>本集专项题训练</span>
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
