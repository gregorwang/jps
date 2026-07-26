import { describe, expect, it, vi } from 'vitest'
import {
  createEvaluationTicket,
  normalizePronunciationSentenceId,
  pronunciationSentenceLookupPath,
} from './pronunciationTicket'

describe('createEvaluationTicket', () => {
  it('creates a signed short-lived ticket scoped to one sentence and subject', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-10T12:00:00.000Z'))
    try {
      const ticket = await createEvaluationTicket({
        subject: 'user-123',
        sentenceId: 'kon-ep01-046',
        secret: '0123456789abcdef0123456789abcdef',
        ttlSeconds: 60,
      })
      const [payload, signature] = ticket.split('.')
      const payloadBytes = Uint8Array.from(
        atob(payload.replaceAll('-', '+').replaceAll('_', '/')),
        (character) => character.charCodeAt(0),
      )
      const decoded = JSON.parse(
        new TextDecoder().decode(payloadBytes),
      ) as Record<string, unknown>

      expect(signature).toMatch(/^[A-Za-z0-9_-]+$/u)
      expect(decoded).toMatchObject({
        v: 1,
        sub: 'user-123',
        sentenceId: 'kon-ep01-046',
        iat: 1_783_684_800,
        exp: 1_783_684_860,
      })
      expect(decoded.jti).toMatch(/^[a-f0-9]{32}$/u)
    } finally {
      vi.useRealTimers()
    }
  })

  it('rejects secrets that cannot safely sign production tickets', async () => {
    await expect(
      createEvaluationTicket({ subject: 'user', sentenceId: 'sentence', secret: 'short' }),
    ).rejects.toThrow('HMAC secret is too short')
  })

  it('accepts every stable course sentence ID and rejects unsafe IDs', () => {
    expect(normalizePronunciationSentenceId('k-on-ep01-sent-00046')).toBe('k-on-ep01-sent-00046')
    expect(normalizePronunciationSentenceId('re-zero-s02e18-sentence-094')).toBe('re-zero-s02e18-sentence-094')
    expect(normalizePronunciationSentenceId(' sentence/id ')).toBeNull()
    expect(normalizePronunciationSentenceId('')).toBeNull()
  })

  it('builds a PostgREST lookup restricted to a real course sentence', () => {
    expect(pronunciationSentenceLookupPath('k-on-ep01-sent-00046')).toBe(
      '/rest/v1/learning_sentences?select=id&id=eq.k-on-ep01-sent-00046&limit=1',
    )
  })
})
