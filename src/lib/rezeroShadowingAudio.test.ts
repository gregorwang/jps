import { describe, expect, it } from 'vitest'
import { buildReZeroShadowingAudio } from './rezeroShadowingAudio'

describe('buildReZeroShadowingAudio', () => {
  it('builds season 1 urls from sentence id', () => {
    const audio = buildReZeroShadowingAudio({
      sentenceId: 're-zero-s01e07-sentence-021',
    })

    expect(audio).toEqual({
      url: 'https://cdn.xn--cckl9nsb.com/rezeroS1/s01e07/re-zero-s01e07-sentence-021.unmatched_or_ineligible.mp3',
      isFlagged: true,
    })
  })

  it('builds season 2 urls from sentence id', () => {
    expect(buildReZeroShadowingAudio({
      sentenceId: 're-zero-s02e25-sentence-001',
    })).toEqual({
      url: 'https://cdn.xn--cckl9nsb.com/rezeroS2/s02e25/re-zero-s02e25-sentence-001.mp3',
      isFlagged: false,
    })

    expect(buildReZeroShadowingAudio({
      sentenceId: 're-zero-s02e15-sentence-168',
    })).toEqual({
      url: 'https://cdn.xn--cckl9nsb.com/rezeroS2/s02e15/re-zero-s02e15-sentence-168.mp3',
      isFlagged: false,
    })
  })

  it('builds season 3 urls from production sentence ids', () => {
    const audio = buildReZeroShadowingAudio({
      sentenceId: 'rezero_s03e52_v9_sent_028',
    })

    expect(audio).toEqual({
      url: 'https://cdn.xn--cckl9nsb.com/rezeroS3/s03e02/rezero_s03e52_v9_sent_028.mp3',
      isFlagged: false,
    })
  })

  it('builds season 3 urls from legacy sentence ids', () => {
    const audio = buildReZeroShadowingAudio({
      sentenceId: 're-zero-s03e02-sentence-028',
    })

    expect(audio).toEqual({
      url: 'https://cdn.xn--cckl9nsb.com/rezeroS3/s03e02/rezero_s03e52_v9_sent_028.mp3',
      isFlagged: false,
    })
  })

  it('prefers explicit database audio urls', () => {
    const audio = buildReZeroShadowingAudio({
      sentenceId: 're-zero-s03e02-sentence-028',
      audioUrl: 'https://cdn.xn--cckl9nsb.com/rezeroS3/s03e02/custom.mp3',
    })

    expect(audio).toEqual({
      url: 'https://cdn.xn--cckl9nsb.com/rezeroS3/s03e02/custom.mp3',
      isFlagged: false,
    })
  })

  it('returns null for unsupported legacy season ids', () => {
    expect(
      buildReZeroShadowingAudio({
        sentenceId: 're-zero-s04e01-sentence-001',
      }),
    ).toBeNull()
  })
})
