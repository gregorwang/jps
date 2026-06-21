import { describe, expect, it } from 'vitest'
import { buildEpisodeLesson } from './lesson'
import type { GrammarPoint, LearningSentence, VocabItem } from './types'

const vocab: VocabItem[] = [
  makeVocab('v1', '大丈夫', '没事吧'),
  makeVocab('v2', '今日', '今天'),
  makeVocab('v3', '先生', '老师'),
  makeVocab('v4', '軽音部', '轻音部'),
  makeVocab('v5', '本当', '真的'),
]

const grammar: GrammarPoint[] = [
  makeGrammar('g1', '～ないと', '必须 / 否则', 'そろそろ起きないと'),
  makeGrammar('g2', '～って', '引用 / 话题', '唯って面白いね'),
  makeGrammar('g3', 'ね', '确认 / 共感', '楽しそうですね'),
  makeGrammar('g4', '句末 よ/ね/かな/だろ', '句末语气', 'こうやってニートが出来上がっていくのね'),
]

const sentences: LearningSentence[] = [
  makeSentence('s1', 'こうやってニートが出来上がっていくのね', '这样下去就会变成家里蹲'),
  makeSentence('s2', 'このプリントを みんなに 配っておいてね', '把 这些 资料 发给 大家'),
]

describe('buildEpisodeLesson', () => {
  it('builds a mixed ordinary lesson without linguistic high-level exercises', () => {
    const lesson = buildEpisodeLesson({ workSlug: 'k-on', episode: 1, vocab, grammar, sentences })

    expect(lesson.nodes.length).toBeGreaterThan(5)
    expect(lesson.counts['pair-match']).toBe(1)
    expect(lesson.counts['audio-tiles']).toBeGreaterThan(0)
    expect(lesson.counts['cloze-choice']).toBeGreaterThan(0)
    expect(lesson.nodes.every((node) => node.source.kind !== 'sentence' || node.audio.kind !== 'source')).toBe(true)
  })

  it('builds mode-specific lesson queues', () => {
    const vocabLesson = buildEpisodeLesson({ workSlug: 'k-on', episode: 1, vocab, grammar, sentences, mode: 'vocab' })
    const grammarLesson = buildEpisodeLesson({ workSlug: 'k-on', episode: 1, vocab, grammar, sentences, mode: 'grammar' })
    const shadowingLesson = buildEpisodeLesson({ workSlug: 'k-on', episode: 1, vocab, grammar, sentences, mode: 'shadowing' })

    expect(vocabLesson.nodes.every((node) => node.source.kind === 'vocab')).toBe(true)
    expect(grammarLesson.nodes.every((node) => node.source.kind === 'grammar')).toBe(true)
    expect(grammarLesson.counts['cloze-choice']).toBeGreaterThan(0)
    expect(shadowingLesson.nodes.every((node) => node.source.kind === 'sentence')).toBe(true)
  })

  it('builds target practice around the requested source', () => {
    const lesson = buildEpisodeLesson({
      workSlug: 'k-on',
      episode: 1,
      vocab,
      grammar,
      sentences,
      target: { kind: 'grammar', id: 'g2' },
    })

    expect(lesson.mode).toBe('target')
    expect(lesson.id).toContain('grammar-g2')
    expect(lesson.nodes[0]?.source.sourceId).toBe('g2')
  })

  it('splits natural Japanese sentence tiles without fixed-width fragments', () => {
    const lesson = buildEpisodeLesson({ workSlug: 'k-on', episode: 1, vocab, grammar, sentences, mode: 'shadowing' })
    const node = lesson.nodes.find((item) => item.type === 'audio-tiles' && item.source.sourceId === 's1')

    expect(node?.type).toBe('audio-tiles')
    if (node?.type !== 'audio-tiles') return
    expect(node.targetTiles).toEqual(['こうやって', 'ニートが', '出来上がっていくのね'])
    expect(node.targetTiles).not.toContain('がってい')
    expect(node.targetTiles).not.toContain('くのね')
  })

  it('infers sentence-final grammar cloze answers instead of midpoint blanks', () => {
    const lesson = buildEpisodeLesson({
      workSlug: 'k-on',
      episode: 1,
      vocab,
      grammar,
      sentences,
      target: { kind: 'grammar', id: 'g4' },
    })
    const node = lesson.nodes.find((item) => item.type === 'cloze-choice')

    expect(node?.type).toBe('cloze-choice')
    if (node?.type !== 'cloze-choice') return
    expect(node.answer).toBe('ね')
    expect(`${node.before}[${node.answer}]${node.after}`).toBe('こうやってニートが出来上がっていくの[ね]')
    expect(node.choices.map((choice) => choice.value)).toContain('ね')
  })
})

function makeVocab(id: string, surface: string, meaningZh: string): VocabItem {
  return {
    id,
    workSlug: 'k-on',
    surface,
    reading: surface,
    romaji: surface,
    meaningZh,
    pos: '名詞',
    jlptLevel: 'N5',
    suitableHandwriting: true,
    suitableShadowing: true,
    totalOccurrences: 1,
    episodeCount: 1,
  }
}

function makeGrammar(id: string, pattern: string, functionZh: string, jaExample: string): GrammarPoint {
  return {
    id,
    pattern,
    functionZh,
    jaExample,
    explanationZh: `${pattern} explanation`,
    pragmaticsNote: `${pattern} pragmatics`,
    realWorldNote: '',
    difficulty: 'N5',
    sourceLineNo: 1,
  }
}

function makeSentence(id: string, jaText: string, meaningZh: string): LearningSentence {
  return {
    id,
    jaText,
    meaningZh,
    toneTags: [],
    difficulty: 'N5',
    sourceLineNo: 1,
  }
}
