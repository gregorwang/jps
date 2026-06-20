const reZeroShadowingCdnBase = 'https://cdn.xn--cckl9nsb.com'
const reZeroShadowingPrefix = 'rezeroS1'
const flaggedSuffix = '.unmatched_or_ineligible.mp3'

const flaggedSentenceIds = new Set([
  're-zero-s01e03-sentence-003',
  're-zero-s01e03-sentence-004',
  're-zero-s01e03-sentence-010',
  're-zero-s01e03-sentence-021',
  're-zero-s01e03-sentence-023',
  're-zero-s01e03-sentence-025',
  're-zero-s01e04-sentence-016',
  're-zero-s01e04-sentence-028',
  're-zero-s01e04-sentence-036',
  're-zero-s01e05-sentence-002',
  're-zero-s01e05-sentence-007',
  're-zero-s01e05-sentence-009',
  're-zero-s01e05-sentence-017',
  're-zero-s01e05-sentence-023',
  're-zero-s01e05-sentence-033',
  're-zero-s01e05-sentence-041',
  're-zero-s01e05-sentence-054',
  're-zero-s01e06-sentence-007',
  're-zero-s01e06-sentence-009',
  're-zero-s01e06-sentence-010',
  're-zero-s01e06-sentence-023',
  're-zero-s01e06-sentence-047',
  're-zero-s01e06-sentence-050',
  're-zero-s01e06-sentence-054',
  're-zero-s01e06-sentence-059',
  're-zero-s01e06-sentence-062',
  're-zero-s01e07-sentence-001',
  're-zero-s01e07-sentence-021',
  're-zero-s01e07-sentence-025',
  're-zero-s01e07-sentence-030',
  're-zero-s01e07-sentence-038',
  're-zero-s01e07-sentence-044',
  're-zero-s01e07-sentence-056',
  're-zero-s01e07-sentence-058',
  're-zero-s01e08-sentence-010',
  're-zero-s01e08-sentence-015',
  're-zero-s01e08-sentence-018',
  're-zero-s01e08-sentence-024',
  're-zero-s01e08-sentence-025',
  're-zero-s01e08-sentence-031',
  're-zero-s01e08-sentence-035',
  're-zero-s01e08-sentence-036',
  're-zero-s01e08-sentence-037',
  're-zero-s01e08-sentence-047',
  're-zero-s01e08-sentence-056',
  're-zero-s01e08-sentence-063',
  're-zero-s01e08-sentence-070',
  're-zero-s01e08-sentence-072',
  're-zero-s01e08-sentence-073',
  're-zero-s01e08-sentence-076',
  're-zero-s01e08-sentence-081',
  're-zero-s01e08-sentence-084',
  're-zero-s01e08-sentence-085',
  're-zero-s01e08-sentence-089',
  're-zero-s01e08-sentence-091',
  're-zero-s01e08-sentence-093',
  're-zero-s01e08-sentence-094',
  're-zero-s01e09-sentence-002',
  're-zero-s01e09-sentence-008',
  're-zero-s01e09-sentence-050',
  're-zero-s01e09-sentence-055',
  're-zero-s01e09-sentence-056',
  're-zero-s01e09-sentence-059',
  're-zero-s01e09-sentence-063',
  're-zero-s01e10-sentence-002',
  're-zero-s01e10-sentence-012',
  're-zero-s01e10-sentence-013',
  're-zero-s01e10-sentence-038',
  're-zero-s01e10-sentence-063',
  're-zero-s01e10-sentence-073',
  're-zero-s01e10-sentence-074',
  're-zero-s01e10-sentence-082',
  're-zero-s01e10-sentence-092',
  're-zero-s01e11-sentence-023',
  're-zero-s01e11-sentence-029',
  're-zero-s01e11-sentence-030',
  're-zero-s01e11-sentence-059',
  're-zero-s01e11-sentence-061',
  're-zero-s01e11-sentence-062',
  're-zero-s01e11-sentence-084',
  're-zero-s01e11-sentence-086',
  're-zero-s01e11-sentence-096',
])

export type ShadowingAudio = {
  url: string
  isFlagged: boolean
}

type BuildReZeroShadowingAudioInput = {
  sentenceId: string
  audioUrl?: string
  storagePath?: string
}

export function buildReZeroShadowingAudio({
  sentenceId,
  audioUrl,
  storagePath,
}: BuildReZeroShadowingAudioInput): ShadowingAudio | null {
  const dbUrl = normalizeDbAudioValue(audioUrl) ?? normalizeDbAudioValue(storagePath)
  if (dbUrl) {
    return {
      url: dbUrl,
      isFlagged: dbUrl.includes(flaggedSuffix),
    }
  }

  const episodeSlug = sentenceId.split('-')[2]
  if (!episodeSlug) return null

  const isFlagged = flaggedSentenceIds.has(sentenceId)
  const filename = `${sentenceId}${isFlagged ? '.unmatched_or_ineligible' : ''}.mp3`
  return {
    url: `${reZeroShadowingCdnBase}/${reZeroShadowingPrefix}/${episodeSlug}/${filename}`,
    isFlagged,
  }
}

function normalizeDbAudioValue(value?: string) {
  const trimmed = value?.trim()
  if (!trimmed) return undefined
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) return trimmed

  const path = trimmed.replace(/^\/+/, '')
  return `${reZeroShadowingCdnBase}/${path}`
}
