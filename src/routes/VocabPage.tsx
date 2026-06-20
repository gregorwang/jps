import { useQuery } from '@tanstack/react-query'
import { useParams } from '@tanstack/react-router'
import { useMemo } from 'react'
import { ChoiceTrainer, type ChoiceQuestion } from '../components/ChoiceTrainer'
import { EpisodeScopeSelector } from '../components/EpisodeScopeSelector'
import { PageHeader } from '../components/PageHeader'
import { AiExplainButton } from '../components/AiExplainButton'
import { TtsButton } from '../components/TtsButton'
import { readEpisodeScope } from '../lib/episodeScope'
import { animeRepository } from '../server/repositories/animeRepository'

export function VocabPage() {
  const { workSlug, episode } = useParams({ strict: false })
  const fallbackScope = readEpisodeScope()
  const selectedWorkSlug = workSlug ?? fallbackScope.workSlug
  const episodeNo = Number(episode ?? fallbackScope.episode)
  const vocabQuery = useQuery({
    queryKey: ['vocab', selectedWorkSlug, episodeNo],
    queryFn: () => animeRepository.listEpisodeVocab(selectedWorkSlug, episodeNo),
  })
  const questions = useMemo<ChoiceQuestion[]>(() => {
    const vocab = vocabQuery.data ?? []
    return vocab.map((item, index) => {
      const distractors = vocab
        .filter((candidate) => candidate.id !== item.id)
        .slice(index, index + 3)
        .map((candidate) => candidate.meaningZh)
      return {
        id: item.id,
        itemType: 'vocab',
        workSlug: selectedWorkSlug,
        episode: episodeNo,
        kicker: item.reading ? `听音选义 · ${item.reading}` : '词汇选择',
        prompt: `「${item.surface}」最接近哪个意思？`,
        body: item.animeToneNote,
        choices: [item.meaningZh, ...distractors, '只是语气停顿，没有实际意义'].slice(0, 4),
        answer: item.meaningZh,
        explanation: `${item.surface} 在这里要记成「${item.meaningZh}」。${item.realWorldNote ?? ''}`,
        reviewLabel: item.surface,
        listenText: item.surface,
        ai: {
          kind: 'vocab',
          text: item.surface,
          context: `${item.meaningZh}\n${item.animeToneNote ?? ''}\n${item.realWorldNote ?? ''}`,
        },
      }
    })
  }, [episodeNo, selectedWorkSlug, vocabQuery.data])

  return (
    <section className="page-stack">
      <PageHeader
        eyebrow="词汇训练"
        title={`EP${String(episodeNo).padStart(2, '0')} 听音选义`}
        description="先做题证明自己真的会。答错的词会自动进入今日回炉，AI 只在需要解释时出现。"
      />
      <EpisodeScopeSelector workSlug={selectedWorkSlug} episode={episodeNo} tool="vocab" />
      <ChoiceTrainer questions={questions} emptyText={vocabQuery.isLoading ? '词汇题加载中。' : '暂无词汇题。'} />
      <section className="source-preview">
        <p className="eyebrow">辅助词表</p>
        <strong>做题后再查资料</strong>
        <span>这里保留词典信息，但主学习动作已经变成选择题。</span>
      </section>
      <div className="card-list">
        {vocabQuery.data?.map((item) => (
          <article className="learning-card" key={item.id}>
            <div>
              <h2>{item.surface}</h2>
              <p className="kana">{item.reading} · {item.romaji}</p>
              <p>{item.meaningZh}</p>
              <small>{item.pos} · {item.jlptLevel} · 出现 {item.totalOccurrences} 次</small>
            </div>
            <div className="card-actions">
              <TtsButton text={item.surface} />
              <AiExplainButton kind="vocab" text={item.surface} context={item.meaningZh} />
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}
