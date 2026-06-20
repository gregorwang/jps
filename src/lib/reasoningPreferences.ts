import { useSyncExternalStore } from 'react'

export type ReasoningEffort = 'low' | 'medium' | 'high'

export const reasoningEfforts: { id: ReasoningEffort; label: string }[] = [
  { id: 'low', label: '低' },
  { id: 'medium', label: '中' },
  { id: 'high', label: '高' },
]

const key = 'anime-japanese-lab-grok-reasoning-effort'
const defaultReasoningEffort: ReasoningEffort = 'high'
const listeners = new Set<() => void>()

function isReasoningEffort(value: unknown): value is ReasoningEffort {
  return value === 'low' || value === 'medium' || value === 'high'
}

export function getPreferredReasoningEffort(): ReasoningEffort {
  if (typeof window === 'undefined') return defaultReasoningEffort
  const stored = window.localStorage.getItem(key)
  return isReasoningEffort(stored) ? stored : defaultReasoningEffort
}

export function setPreferredReasoningEffort(effort: ReasoningEffort) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(key, effort)
  listeners.forEach((listener) => listener())
}

export function usePreferredReasoningEffort() {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    getPreferredReasoningEffort,
    () => defaultReasoningEffort,
  )
}
