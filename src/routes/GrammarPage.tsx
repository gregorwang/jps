import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { useMemo } from 'react'
import { AiExplainButton } from '../components/AiExplainButton'
import { ChoiceTrainer, type ChoiceQuestion } from '../components/ChoiceTrainer'
import { EpisodeScopeSelector } from '../components/EpisodeScopeSelector'
import { PageHeader } from '../components/PageHeader'
import { readEpisodeScope } from '../lib/episodeScope'
import { animeRepository } from '../server/repositories/animeRepository'

export function GrammarPage() {
  const { workSlug, episode } = useParams({ strict: false })
  const fallbackScope = readEpisodeScope()
  const selectedWorkSlug = workSlug ?? fallbackScope.workSlug
  const episodeNo = Number(episode ?? fallbackScope.episode)
  const grammarQuery = useQuery({
    queryKey: ['grammar', selectedWorkSlug, episodeNo],
    queryFn: () => animeRepository.listEpisodeGrammar(selectedWorkSlug, episodeNo),
  })
  const questions = useMemo<ChoiceQuestion[]>(() => {
    const grammar = grammarQuery.data ?? []
    return grammar.map((point, index) => {
      const distractors = grammar
        .filter((candidate) => candidate.id !== point.id)
        .slice(index, index + 3)
        .map((candidate) => candidate.functionZh)
      return {
        id: point.id,
        itemType: 'grammar',
        workSlug: selectedWorkSlug,
        episode: episodeNo,
        kicker: `${point.difficulty} · 字幕语境判断`,
        prompt: `这句里的「${point.pattern}」主要表达什么？`,
        body: point.jaExample,
        choices: [point.functionZh, ...distractors, '单纯书面连接，没有语气'].slice(0, 4),
        answer: point.functionZh,
        explanation: `${point.explanationZh} ${point.pragmaticsNote}`,
        reviewLabel: point.pattern,
        listenText: point.jaExample,
        ai: {
          kind: 'grammar',
          text: point.pattern,
          context: `${point.jaExample}\n${point.explanationZh}\n${point.pragmaticsNote}`,
        },
      }
    })
  }, [episodeNo, grammarQuery.data, selectedWorkSlug])

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="语法训练"
        title={`EP${String(episodeNo).padStart(2, '0')} 字幕语法题`}
        description="用本集台词判断语法在当前场景里的意思和语气，而不是先背说明。"
      />
      <EpisodeScopeSelector workSlug={selectedWorkSlug} episode={episodeNo} tool="grammar" />
      <ChoiceTrainer questions={questions} emptyText={grammarQuery.isLoading ? '语法题加载中。' : '暂无语法题。'} />
      <section className="source-preview">
        <p className="eyebrow">辅助语法卡</p>
        <strong>答题后查看说明</strong>
        <span>语法资料保留在下方，主路径改成先判断、再反馈。</span>
      </section>
      <div className="card-list">
        {grammarQuery.data?.map((point) => (
          <article className="learning-card" key={point.id}>
            <div>
              <h2>{point.pattern}</h2>
              <p className="kana">{point.functionZh} · {point.difficulty}</p>
              <blockquote>{point.jaExample}</blockquote>
              <p>{point.explanationZh}</p>
              <small>{point.pragmaticsNote} {point.realWorldNote}</small>
            </div>
            <div className="card-actions">
              <AiExplainButton kind="grammar" text={point.pattern} context={`${point.jaExample}\n${point.explanationZh}`} />
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}
