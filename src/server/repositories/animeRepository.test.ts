import { describe, expect, it } from 'vitest'
import { animeRepository } from './animeRepository'

describe('animeRepository', () => {
  it('loads the K-ON! work and episode plan fixture', async () => {
    const works = await animeRepository.listWorks()
    const plan = await animeRepository.getEpisodePlan('k-on', 1)

    expect(works[0]?.displayName).toBe('K-ON!')
    expect(plan?.vocabCount).toBe(20)
    expect(plan?.shadowingCount).toBe(5)
  })

  it('loads EP01 learning material lists', async () => {
    const [vocab, grammar, sentences, exercises, subtitles] = await Promise.all([
      animeRepository.listEpisodeVocab('k-on', 1),
      animeRepository.listEpisodeGrammar('k-on', 1),
      animeRepository.listEpisodeSentences('k-on', 1),
      animeRepository.listEpisodeExercises('k-on', 1),
      animeRepository.listSubtitleLines('k-on', 1),
    ])

    expect(vocab.length).toBeGreaterThan(0)
    expect(grammar.length).toBeGreaterThan(0)
    expect(sentences.length).toBeGreaterThan(0)
    expect(exercises.length).toBeGreaterThan(0)
    expect(subtitles[0]?.jaText).toContain('起きないと')
  })
})
