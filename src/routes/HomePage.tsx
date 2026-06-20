import { Link } from '@tanstack/react-router'
import { BookOpen, Brain, ClipboardList, History, Mic2, PenLine, PlayCircle, Sparkles } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { PageHeader } from '../components/PageHeader'
import { StatCard } from '../components/StatCard'
import { readEpisodeScope } from '../lib/episodeScope'
import { getDeviceId } from '../lib/progress'
import { animeRepository } from '../server/repositories/animeRepository'

export function HomePage() {
  const [episodeScope, setEpisodeScope] = useState(readEpisodeScope)
  const planQuery = useQuery({
    queryKey: ['episode-plan', episodeScope.workSlug, episodeScope.episode],
    queryFn: () => animeRepository.getEpisodePlan(episodeScope.workSlug, episodeScope.episode),
  })
  const reviewQuery = useQuery({
    queryKey: ['today-review'],
    queryFn: () => animeRepository.getTodayReviewTasks(getDeviceId()),
  })

  const plan = planQuery.data
  const reviewTasks = reviewQuery.data?.tasks ?? []
  const groups = reviewQuery.data?.groups ?? {}
  const episodeParams = { workSlug: episodeScope.workSlug, episode: String(episodeScope.episode) }

  useEffect(() => {
    function handleEpisodeScopeChange(event: Event) {
      const detail = (event as CustomEvent<typeof episodeScope>).detail
      if (detail?.workSlug && detail.episode) setEpisodeScope(detail)
    }

    window.addEventListener('episode-scope-change', handleEpisodeScopeChange)
    return () => window.removeEventListener('episode-scope-change', handleEpisodeScopeChange)
  }, [])

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="今日学习"
        title={`${episodeScope.workSlug} EP${String(episodeScope.episode).padStart(2, '0')}`}
        description="今天先做题：词汇选择、语法判断、读空气场景题和错题回炉。资料放在题后，需要时再看。"
        actions={
          <Link className="primary-action" to="/works/$workSlug/episodes/$episode/vocab" params={episodeParams}>
            <BookOpen size={18} />
            <span>开始今日训练</span>
          </Link>
        }
      />

      <div className="stat-grid">
        <StatCard label="高频词" value={plan?.vocabCount ?? 20} icon={<Sparkles size={20} />} />
        <StatCard label="手写词" value={plan?.handwritingCount ?? 10} icon={<PenLine size={20} />} />
        <StatCard label="跟读句" value={plan?.shadowingCount ?? 5} icon={<Mic2 size={20} />} />
        <StatCard label="今日回炉" value={reviewTasks.length} icon={<Brain size={20} />} />
      </div>

      <section className="source-preview">
        <div className="section-heading-row">
          <div>
            <p className="eyebrow">复习 / 错题回炉</p>
            <strong>{reviewTasks.length > 0 ? `今日队列 ${reviewTasks.length} 项` : '今天暂无弱项任务'}</strong>
          </div>
          <Link className="icon-button secondary" to="/history">
            <History size={18} />
            <span>历史</span>
          </Link>
        </div>
        <div className="review-chip-list" aria-label="复习分组">
          <span className="review-chip">词 {groups.vocab ?? 0}</span>
          <span className="review-chip">语法 {groups.grammar ?? 0}</span>
          <span className="review-chip">跟读 {groups.sentence ?? 0}</span>
          <span className="review-chip">手写 {groups.exercise ?? 0}</span>
        </div>
        <div className="review-task-list">
          {reviewTasks.slice(0, 6).map((task, index) => (
            <a className="review-task-row" href={task.route} key={task.itemId}>
              <ClipboardList size={18} />
              <span>{index + 1}</span>
              <strong>{task.label}</strong>
              <small>{task.itemType} · {task.state} · {task.due ? '今日到期' : task.nextReviewOn}</small>
            </a>
          ))}
          {reviewTasks.length === 0 ? <span className="muted-text">把词、句子或手写题标成模糊/不会/不像后，会自动进入这里。</span> : null}
        </div>
      </section>

      <div className="task-grid">
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/vocab" params={episodeParams}>
          <Sparkles size={22} />
          <strong>今日词汇练习</strong>
          <span>听音选义，答错自动回炉。</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/grammar" params={episodeParams}>
          <Brain size={22} />
          <strong>今日语法练习</strong>
          <span>用字幕例句判断语法功能。</span>
        </Link>
        <Link className="task-card" to="/rag">
          <Brain size={22} />
          <strong>AI 生成读空气题</strong>
          <span>自动找字幕片段，预览后保存为本地草稿题。</span>
        </Link>
        <Link className="task-card" to="/linguistic-training">
          <PlayCircle size={22} />
          <strong>学习作答</strong>
          <span>做正式发布题，也可练习 RAG 页保存的本地草稿。</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/writing" params={episodeParams}>
          <PenLine size={22} />
          <strong>手写练习</strong>
          <span>iPad 描红书写，提交后自动切到下一个。</span>
        </Link>
        <Link className="task-card" to="/works/$workSlug/episodes/$episode/sentences" params={episodeParams}>
          <Mic2 size={22} />
          <strong>跟读任务</strong>
          <span>播放 TTS 标准音，先做自评。</span>
        </Link>
      </div>
    </section>
  )
}
