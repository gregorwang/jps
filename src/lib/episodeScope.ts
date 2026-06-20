const episodeScopeKey = 'anime-japanese-lab-episode-scope'

export type EpisodeScope = {
  workSlug: string
  episode: number
}

export const defaultEpisodeScope: EpisodeScope = {
  workSlug: 'k-on',
  episode: 1,
}

export function readEpisodeScope(): EpisodeScope {
  if (typeof window === 'undefined') return defaultEpisodeScope
  try {
    const raw = window.localStorage.getItem(episodeScopeKey)
    if (!raw) return defaultEpisodeScope
    const parsed = JSON.parse(raw) as Partial<EpisodeScope>
    if (typeof parsed.workSlug !== 'string' || !parsed.workSlug) return defaultEpisodeScope
    if (typeof parsed.episode !== 'number' || !Number.isFinite(parsed.episode)) return defaultEpisodeScope
    return { workSlug: parsed.workSlug, episode: parsed.episode }
  } catch {
    return defaultEpisodeScope
  }
}

export function writeEpisodeScope(scope: EpisodeScope) {
  if (typeof window === 'undefined') return
  const current = readEpisodeScope()
  if (current.workSlug === scope.workSlug && current.episode === scope.episode) return
  window.localStorage.setItem(episodeScopeKey, JSON.stringify(scope))
  window.dispatchEvent(new CustomEvent<EpisodeScope>('episode-scope-change', { detail: scope }))
}
