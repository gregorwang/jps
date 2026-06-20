import { useSyncExternalStore } from 'react'
import { aiGatewayModels, type GatewayModel } from './aiModels'

const key = 'anime-japanese-lab-ai-model'
const defaultModel: GatewayModel = 'gemini-3.1-flash-lite'
const listeners = new Set<() => void>()

function isGatewayModel(value: unknown): value is GatewayModel {
  return aiGatewayModels.some((model) => model.id === value)
}

export function getPreferredGatewayModel(): GatewayModel {
  if (typeof window === 'undefined') return defaultModel
  const stored = window.localStorage.getItem(key)
  return isGatewayModel(stored) ? stored : defaultModel
}

export function setPreferredGatewayModel(model: GatewayModel) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(key, model)
  listeners.forEach((listener) => listener())
}

export function usePreferredGatewayModel() {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener)
      return () => listeners.delete(listener)
    },
    getPreferredGatewayModel,
    () => defaultModel,
  )
}
