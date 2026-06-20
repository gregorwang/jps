import { useSyncExternalStore } from 'react'
import { defaultJapaneseVoice, japaneseVoices, type JapaneseVoice } from '../server/tts'

const key = 'anime-japanese-lab-tts-voice'
const listeners = new Set<() => void>()

function isJapaneseVoice(value: unknown): value is JapaneseVoice {
  return japaneseVoices.some((voice) => voice.id === value)
}

export function getPreferredJapaneseVoice(): JapaneseVoice {
  if (typeof window === 'undefined') return defaultJapaneseVoice
  const stored = window.localStorage.getItem(key)
  return isJapaneseVoice(stored) ? stored : defaultJapaneseVoice
}

export function setPreferredJapaneseVoice(voice: JapaneseVoice) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(key, voice)
  listeners.forEach((listener) => listener())
}

export function usePreferredJapaneseVoice() {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    getPreferredJapaneseVoice,
    () => defaultJapaneseVoice,
  )
}
