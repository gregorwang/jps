import type { VocabItem } from './types'
import { getDeviceId } from './progress'
import type { WritingValidationMetrics, WritingValidationReason } from './writingValidation'

export type WritingPracticeItem = {
  id: string
  text: string
  reading?: string
  romaji?: string
  meaningZh?: string
  source: 'vocab'
  workSlug?: string
  episode?: number
}

export type WritingPracticeStats = {
  itemId: string
  completedCount: number
  lastPracticedAt?: string
}

export type WritingSubmissionInput = {
  item: WritingPracticeItem
  passed: boolean
  reason?: WritingValidationReason | 'skipped'
  metrics: WritingValidationMetrics
}

const localStatsKey = 'anime-japanese-lab-writing-stats'

export function buildWritingItems(vocabItems: VocabItem[], workSlug?: string, episode?: number): WritingPracticeItem[] {
  const suitableVocab = vocabItems
    .filter((item) => item.suitableHandwriting && isWritableJapanese(item.surface))
    .slice(0, 20)
    .map((item) => ({
      id: `vocab-${item.id}`,
      text: item.surface,
      reading: item.reading,
      romaji: item.romaji,
      meaningZh: item.meaningZh,
      source: 'vocab' as const,
      workSlug: item.workSlug,
      episode,
    }))

  const seen = new Set<string>()
  return suitableVocab.filter((item) => {
    const key = item.text
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

export function readLocalWritingStats(): Record<string, WritingPracticeStats> {
  if (typeof window === 'undefined') return {}
  try {
    const raw = window.localStorage.getItem(localStatsKey)
    return raw ? JSON.parse(raw) as Record<string, WritingPracticeStats> : {}
  } catch {
    return {}
  }
}

export function updateLocalWritingStats(itemId: string) {
  if (typeof window === 'undefined') return
  const stats = readLocalWritingStats()
  const current = stats[itemId] ?? { itemId, completedCount: 0 }
  stats[itemId] = {
    itemId,
    completedCount: current.completedCount + 1,
    lastPracticedAt: new Date().toISOString(),
  }
  window.localStorage.setItem(localStatsKey, JSON.stringify(stats))
}

export async function fetchWritingStats() {
  const response = await fetch(`/api/writing/stats?deviceId=${encodeURIComponent(getDeviceId())}`)
  if (!response.ok) return [] as WritingPracticeStats[]
  return (await response.json()) as WritingPracticeStats[]
}

export async function submitWritingPractice(input: WritingSubmissionInput) {
  if (input.passed) updateLocalWritingStats(input.item.id)
  const response = await fetch('/api/writing/submit', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      deviceId: getDeviceId(),
      itemId: input.item.id,
      itemText: input.item.text,
      itemType: input.item.source,
      reading: input.item.reading,
      romaji: input.item.romaji,
      meaningZh: input.item.meaningZh,
      workSlug: input.item.workSlug,
      episode: input.item.episode,
      passed: input.passed,
      reason: input.reason,
      metrics: input.metrics,
    }),
  }).catch(() => undefined)

  if (!response?.ok) return null
  return (await response.json()) as WritingPracticeStats | null
}

export async function skipWritingPractice(item: WritingPracticeItem) {
  return submitWritingPractice({
    item,
    passed: false,
    reason: 'skipped',
    metrics: { coverageRatio: 0, offTargetRatio: 0, strokeLength: 0, durationMs: 0 },
  })
}

function isWritableJapanese(text: string) {
  return /^[\p{Script=Hiragana}\p{Script=Katakana}\p{Script=Han}\u30fc々〆〤]+$/u.test(text)
}
