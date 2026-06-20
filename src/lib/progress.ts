import { useEffect, useSyncExternalStore } from 'react'
import type { ProgressItem } from './types'

export type ReviewState = 'known' | 'fuzzy' | 'unknown' | 'good' | 'ok' | 'bad'

const key = 'anime-japanese-lab-progress'
const itemKey = 'anime-japanese-lab-progress-items'
const deviceKey = 'anime-japanese-lab-device-id'

type ProgressStore = Record<string, ReviewState>
type ProgressItemStore = Record<string, ProgressItem>

const listeners = new Set<() => void>()
const emptyProgressStore: ProgressStore = {}
const emptyProgressItemStore: ProgressItemStore = {}
let cachedRaw: string | null = null
let cachedStore: ProgressStore = emptyProgressStore
let cachedItemsRaw: string | null = null
let cachedItemsStore: ProgressItemStore = emptyProgressItemStore

function readStore(): ProgressStore {
  if (typeof window === 'undefined') return emptyProgressStore
  const raw = window.localStorage.getItem(key) ?? '{}'
  if (raw === cachedRaw) return cachedStore

  try {
    cachedRaw = raw
    cachedStore = JSON.parse(raw) as ProgressStore
    return cachedStore
  } catch {
    cachedRaw = raw
    cachedStore = emptyProgressStore
    return cachedStore
  }
}

function emptyStore(): ProgressStore {
  return emptyProgressStore
}

function readItemStore(): ProgressItemStore {
  if (typeof window === 'undefined') return emptyProgressItemStore
  const raw = window.localStorage.getItem(itemKey) ?? '{}'
  if (raw === cachedItemsRaw) return cachedItemsStore

  try {
    cachedItemsRaw = raw
    cachedItemsStore = JSON.parse(raw) as ProgressItemStore
    return cachedItemsStore
  } catch {
    cachedItemsRaw = raw
    cachedItemsStore = emptyProgressItemStore
    return cachedItemsStore
  }
}

function emptyItemStore(): ProgressItemStore {
  return emptyProgressItemStore
}

function writeStore(store: ProgressStore) {
  const raw = JSON.stringify(store)
  if (raw === cachedRaw) return
  cachedRaw = raw
  cachedStore = store
  window.localStorage.setItem(key, raw)
  listeners.forEach((listener) => listener())
}

function writeItemStore(store: ProgressItemStore) {
  const raw = JSON.stringify(store)
  if (raw === cachedItemsRaw) return
  cachedItemsRaw = raw
  cachedItemsStore = store
  window.localStorage.setItem(itemKey, raw)
  listeners.forEach((listener) => listener())
}

function mergeProgressItems(items: ProgressItem[]) {
  const remoteStore = items.reduce<ProgressStore>((store, item) => {
    if (item.itemId && isReviewState(item.state)) {
      store[item.itemId] = item.state
    }
    return store
  }, {})
  const remoteItems = items.reduce<ProgressItemStore>((store, item) => {
    if (item.itemId) store[item.itemId] = item
    return store
  }, {})
  writeStore({ ...readStore(), ...remoteStore })
  writeItemStore({ ...readItemStore(), ...remoteItems })
}

function isReviewState(value: string): value is ReviewState {
  return ['known', 'fuzzy', 'unknown', 'good', 'ok', 'bad'].includes(value)
}

export function setReviewState(itemId: string, state: ReviewState) {
  const current = readStore()
  if (current[itemId] === state) return
  writeStore({ ...current, [itemId]: state })
}

export function getDeviceId() {
  if (typeof window === 'undefined') return 'server'
  const existing = window.localStorage.getItem(deviceKey)
  if (existing) return existing
  const generated = `device-${crypto.randomUUID()}`
  window.localStorage.setItem(deviceKey, generated)
  return generated
}

export async function saveReviewState(
  itemId: string,
  state: ReviewState,
  metadata: {
    itemType?: string
    workSlug?: string
    episode?: number
    payload?: Record<string, unknown>
  } = {},
) {
  setReviewState(itemId, state)
  if (typeof window === 'undefined') return
  const optimisticItem: ProgressItem = {
    deviceId: getDeviceId(),
    itemId,
    itemType: metadata.itemType ?? 'unknown',
    workSlug: metadata.workSlug,
    episode: metadata.episode,
    state,
    nextReviewOn: new Date().toISOString().slice(0, 10),
    lastReviewedAt: new Date().toISOString(),
    payload: metadata.payload ?? {},
  }
  writeItemStore({ ...readItemStore(), [itemId]: optimisticItem })

  const response = await fetch('/api/progress', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      deviceId: getDeviceId(),
      itemId,
      itemType: metadata.itemType ?? 'unknown',
      workSlug: metadata.workSlug,
      episode: metadata.episode,
      state,
      payload: metadata.payload ?? {},
    }),
  }).catch(() => undefined)
  if (!response?.ok) return
  const saved = (await response.json().catch(() => null)) as ProgressItem | null
  if (saved?.itemId) writeItemStore({ ...readItemStore(), [saved.itemId]: saved })
}

export async function syncProgressFromServer() {
  if (typeof window === 'undefined') return
  const response = await fetch(`/api/progress?deviceId=${encodeURIComponent(getDeviceId())}`)
  if (!response.ok) return
  const items = (await response.json()) as ProgressItem[]
  mergeProgressItems(items)
}

export async function getTodayReviewTasks() {
  if (typeof window === 'undefined') return []
  const response = await fetch(`/api/review/today?deviceId=${encodeURIComponent(getDeviceId())}`)
  if (!response.ok) return []
  const data = (await response.json()) as { tasks?: unknown[] }
  return data.tasks ?? []
}

export function useProgressSync() {
  useEffect(() => {
    void syncProgressFromServer().catch(() => undefined)
  }, [])
}

export function useProgressStore() {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    readStore,
    emptyStore,
  )
}

export function useProgressItemsStore() {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    readItemStore,
    emptyItemStore,
  )
}

export function useReviewState(itemId: string) {
  const store = useProgressStore()
  return store[itemId]
}
