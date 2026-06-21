import type { LessonNode } from './lesson'

const sessionKey = 'anime-japanese-lab-lesson-sessions'
const attemptsKey = 'anime-japanese-lab-lesson-attempts'

export type LessonSession = {
  lessonId: string
  index: number
  correct: number
  total: number
  completedAt?: string
  updatedAt: string
}

export type LessonAttempt = {
  lessonId: string
  nodeId: string
  nodeType: LessonNode['type']
  sourceKind: LessonNode['source']['kind']
  sourceId: string
  correct: boolean
  selected: unknown
  answer: unknown
  audioKind: LessonNode['audio']['kind']
  durationMs: number
  createdAt: string
}

type SessionStore = Record<string, LessonSession>
export type LessonAttemptSummary = {
  total: number
  correct: number
  wrong: number
  weakNodeIds: string[]
  weakSourceIds: string[]
}

function readJson<T>(key: string, fallback: T): T {
  if (typeof window === 'undefined') return fallback
  try {
    return JSON.parse(window.localStorage.getItem(key) ?? JSON.stringify(fallback)) as T
  } catch {
    return fallback
  }
}

function writeJson<T>(key: string, value: T) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(key, JSON.stringify(value))
}

export function readLessonSession(lessonId: string): LessonSession | null {
  return readJson<SessionStore>(sessionKey, {})[lessonId] ?? null
}

export function saveLessonSession(session: Omit<LessonSession, 'updatedAt'>) {
  const store = readJson<SessionStore>(sessionKey, {})
  store[session.lessonId] = {
    ...session,
    updatedAt: new Date().toISOString(),
  }
  writeJson(sessionKey, store)
}

export function clearLessonSession(lessonId: string) {
  const store = readJson<SessionStore>(sessionKey, {})
  delete store[lessonId]
  writeJson(sessionKey, store)
}

export function appendLessonAttempt(attempt: Omit<LessonAttempt, 'createdAt'>) {
  const attempts = readJson<LessonAttempt[]>(attemptsKey, [])
  attempts.unshift({
    ...attempt,
    createdAt: new Date().toISOString(),
  })
  writeJson(attemptsKey, attempts.slice(0, 500))
}

export function readLessonAttempts(lessonId: string) {
  return readJson<LessonAttempt[]>(attemptsKey, []).filter((attempt) => attempt.lessonId === lessonId)
}

export function summarizeLessonAttempts(lessonId: string): LessonAttemptSummary {
  const attempts = readLessonAttempts(lessonId)
  const latestByNode = new Map<string, LessonAttempt>()
  for (const attempt of attempts) {
    if (!latestByNode.has(attempt.nodeId)) latestByNode.set(attempt.nodeId, attempt)
  }
  const latest = [...latestByNode.values()]
  const wrongAttempts = latest.filter((attempt) => !attempt.correct)
  return {
    total: latest.length,
    correct: latest.filter((attempt) => attempt.correct).length,
    wrong: wrongAttempts.length,
    weakNodeIds: wrongAttempts.map((attempt) => attempt.nodeId),
    weakSourceIds: [...new Set(wrongAttempts.map((attempt) => attempt.sourceId))],
  }
}
