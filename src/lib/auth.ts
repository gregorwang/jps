import { useQuery } from '@tanstack/react-query'
import { getDeviceId } from './progress'

export type AuthUser = {
  id: string
  email: string
}

export type AuthMe = {
  user: AuthUser | null
}

async function authJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })
  if (!response.ok) throw new Error(await response.text())
  return (await response.json()) as T
}

export function useAuthMe() {
  return useQuery({
    queryKey: ['auth-me'],
    queryFn: () => authJson<AuthMe>('/api/auth/me'),
    retry: false,
    staleTime: 60_000,
  })
}

export async function loginOwner(email: string, password: string) {
  return authJson<AuthMe>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password, deviceHint: getDeviceId() }),
  })
}

export async function registerOwner(email: string, password: string) {
  return authJson<AuthMe>('/api/auth/register-owner', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  })
}

export async function logoutOwner() {
  return authJson<{ ok: boolean }>('/api/auth/logout', { method: 'POST', body: '{}' })
}

export async function claimCurrentDevice() {
  return authJson<{ ok: boolean; merged: Record<string, number> }>('/api/auth/claim-device', {
    method: 'POST',
    body: JSON.stringify({ deviceId: getDeviceId() }),
  })
}

export async function changeOwnerPassword(oldPassword: string, newPassword: string) {
  return authJson<{ ok: boolean }>('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify({ oldPassword, newPassword }),
  })
}
