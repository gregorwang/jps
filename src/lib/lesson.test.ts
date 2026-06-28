import { describe, expect, it } from 'vitest'
import { buildEpisodeLesson } from './lesson'
import type { GrammarPoint, LearningSentence, VocabItem } from './types'

const vocab: VocabItem[] = [
  makeVocab('v1', '大丈夫', '没事吧'),
  makeVocab('v2', '今日', '今天'),
  makeVocab('v3', '先生', '老师'),
  makeVocab('v4', '軽音部', '轻音部'),
  makeVocab('v5', '本当', '真的'),
  makeVocab('v6', '朝', '早上'),
  makeVocab('v7', '学校', '学校'),
  makeVocab('v8', '部活', '社团活动'),
  makeVocab('v9', '友達', '朋友'),
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
    expect(lesson.nodes[0]?.type).toBe('study-card')
    expect(lesson.counts['study-card']).toBeGreaterThan(0)
    expect(lesson.counts['audio-tiles']).toBeGreaterThan(0)
    expect(lesson.counts['cloze-choice']).toBeGreaterThan(0)
    expect(lesson.nodes.every((node) => node.source.kind !== 'sentence' || node.audio.kind !== 'source')).toBe(true)
  })

  it('builds mode-specific lesson queues', () => {
    const vocabLesson = buildEpisodeLesson({ workSlug: 'k-on', episode: 1, vocab, grammar, sentences, mode: 'vocab' })
    const grammarLesson = buildEpisodeLesson({ workSlug: 'k-on', episode: 1, vocab, grammar, sentences, mode: 'grammar' })
    const shadowingLesson = buildEpisodeLesson({ workSlug: 'k-on', episode: 1, vocab, grammar, sentences, mode: 'shadowing' })

    expect(vocabLesson.nodes.every((node) => node.source.kind === 'vocab')).toBe(true)
    expect(vocabLesson.counts['study-card']).toBeGreaterThan(0)
    expect(vocabLesson.nodes[0]?.type).toBe('study-card')
    expect(grammarLesson.nodes.every((node) => node.source.kind === 'grammar')).toBe(true)
    expect(grammarLesson.nodes[0]?.type).toBe('study-card')
    expect(grammarLesson.counts['cloze-choice']).toBeGreaterThan(0)
    expect(shadowingLesson.nodes.every((node) => node.source.kind === 'sentence')).toBe(true)
    expect(shadowingLesson.nodes[0]?.type).toBe('study-card')
  })

  it('covers all vocab items in the vocab specialty lesson', () => {
    const lesson = buildEpisodeLesson({ workSlug: 'k-on', episode: 1, vocab, grammar, sentences, mode: 'vocab' })
    const studiedIds = lesson.nodes
      .filter((node) => node.type === 'study-card')
      .map((node) => node.source.sourceId)
    const choiceIds = lesson.nodes
      .filter((node) => node.type === 'single-choice')
      .map((node) => node.source.sourceId)

    expect(lesson.hasNextBatch).toBe(false)
    expect(studiedIds).toEqual(vocab.map((item) => item.id))
    expect(choiceIds).toEqual(vocab.map((item) => item.id))
  })

  it('removes editorial batch notes from study cards', () => {
    const lesson = buildEpisodeLesson({
      workSlug: 're-zero',
      episode: 14,
      vocab: [
        makeVocab(
          'v-note',
          '絶望',
          '绝望',
          'EP14筛选：学习价值优先于机械词频；围绕绝望、求援失败。放回 EP14 原句跟读。',
        ),
      ],
      grammar,
      sentences,
      mode: 'vocab',
    })
    const card = lesson.nodes.find((node) => node.type === 'study-card')

    expect(card?.type).toBe('study-card')
    if (card?.type !== 'study-card') return
    expect(card.notes).not.toContain('EP14筛选：学习价值优先于机械词频；围绕绝望、求援失败。放回 EP14 原句跟读。')
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
    expect(node.targetText).toBe('こうやってニートが出来上がっていくのね')
    expect(node.explanation).toBe('意思：这样下去就会变成家里蹲')
  })

  it('keeps translation tile answers as Chinese only', () => {
    const lesson = buildEpisodeLesson({ workSlug: 'k-on', episode: 1, vocab, grammar, sentences, mode: 'shadowing' })
    const node = lesson.nodes.find((item) => item.type === 'translation-tiles' && item.source.sourceId === 's1')

    expect(node?.type).toBe('translation-tiles')
    if (node?.type !== 'translation-tiles') return
    expect(node.targetText).toBe('这样下去就会变成家里蹲')
    expect(node.targetTiles.join(' / ')).not.toContain('こうやって')
    expect(node.explanation).toBe('原句：こうやってニートが出来上がっていくのね')
  })

  it('does not build Chinese translation tiles from corrupted or mixed-language meanings', () => {
    const badSentences = [
      makeSentence('bad-private-use', 'ムギちゃんはキーボードうまいよね', '的键盘很不错啊'),
      makeSentence('bad-kana', 'これはテストです', 'これは测试です'),
      makeSentence('good', '本当に助かったんだよ', '真的帮了大忙'),
    ]
    const lesson = buildEpisodeLesson({ workSlug: 'k-on', episode: 1, vocab, grammar, sentences: badSentences, mode: 'shadowing' })
    const translationIds = lesson.nodes
      .filter((item) => item.type === 'translation-tiles')
      .map((item) => item.source.sourceId)

    expect(translationIds).toEqual(['good'])
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

  it('keeps source audio and TTS fallback together for Re:Zero flagged audio', () => {
    const lesson = buildEpisodeLesson({
      workSlug: 're-zero',
      episode: 7,
      vocab,
      grammar,
      sentences: [makeSentence('re-zero-s01e07-sentence-001', 'これはテストです', '这是测试')],
      mode: 'shadowing',
    })
    const node = lesson.nodes.find((item) => item.type === 'audio-tiles')

    expect(node?.audio.kind).toBe('source')
    if (node?.audio.kind !== 'source') return
    expect(node.audio.fallbackTts?.text).toBe('これはテストです')
  })
})

function makeVocab(id: string, surface: string, meaningZh: string, realWorldNote = ''): VocabItem {
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
    realWorldNote,
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
