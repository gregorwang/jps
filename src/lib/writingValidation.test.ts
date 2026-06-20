import { describe, expect, it } from 'vitest'
import { decideWritingPass, measureStrokeLength, type WritingStroke } from './writingValidation'
import { buildWritingItems } from './writingPractice'

describe('writing validation helpers', () => {
  it('rejects short or off-target submissions with coarse reasons', () => {
    const metrics = { coverageRatio: 0.7, offTargetRatio: 0.1, strokeLength: 12, durationMs: 300 }
    expect(decideWritingPass(metrics, { minStrokeLength: 80 }).reason).toBe('too_short')

    expect(decideWritingPass({ ...metrics, strokeLength: 120, offTargetRatio: 0.6 }).reason).toBe('too_far')
    expect(decideWritingPass({ ...metrics, strokeLength: 120, coverageRatio: 0.2 }).reason).toBe('low_coverage')
    expect(decideWritingPass({ ...metrics, strokeLength: 120 }).passed).toBe(true)
  })

  it('measures stroke length across strokes', () => {
    const strokes: WritingStroke[] = [
      [
        { x: 0, y: 0, t: 0 },
        { x: 3, y: 4, t: 10 },
      ],
      [
        { x: 10, y: 10, t: 20 },
        { x: 10, y: 16, t: 30 },
      ],
    ]
    expect(measureStrokeLength(strokes)).toBe(11)
  })

  it('builds writing items from suitable high-frequency vocab only', () => {
    const items = buildWritingItems([
      {
        id: 'vocab-1',
        workSlug: 'k-on',
        surface: '大丈夫',
        reading: 'だいじょうぶ',
        romaji: 'daijoubu',
        meaningZh: '没关系',
        suitableHandwriting: true,
        suitableShadowing: true,
        totalOccurrences: 10,
        episodeCount: 1,
      },
      {
        id: 'vocab-2',
        workSlug: 'k-on',
        surface: 'って',
        reading: 'って',
        romaji: 'tte',
        meaningZh: '引用',
        suitableHandwriting: false,
        suitableShadowing: true,
        totalOccurrences: 20,
        episodeCount: 1,
      },
    ], 'k-on', 1)

    expect(items).toHaveLength(1)
    expect(items[0]).toMatchObject({ text: '大丈夫', source: 'vocab' })
  })
})
