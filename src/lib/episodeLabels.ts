export type EpisodeSeason = {
  season: number
  label: string
  startEpisode: number
  endEpisode: number
}

export const reZeroSeasons: EpisodeSeason[] = [
  { season: 1, label: 'S1', startEpisode: 1, endEpisode: 25 },
  { season: 2, label: 'S2', startEpisode: 26, endEpisode: 50 },
  { season: 3, label: 'S3', startEpisode: 51, endEpisode: 66 },
]

export function isReZeroSlug(workSlug?: string) {
  return workSlug === 're-zero' || workSlug === 'rezero'
}

export function getReZeroSeason(episode: number) {
  return reZeroSeasons.find((season) => episode >= season.startEpisode && episode <= season.endEpisode)
}

export function formatEpisodeLabel(workSlug: string | undefined, episode: number, options?: { includeGlobal?: boolean }) {
  if (!isReZeroSlug(workSlug)) return `EP${String(episode).padStart(2, '0')}`
  const season = getReZeroSeason(episode)
  if (!season) return `EP${String(episode).padStart(2, '0')}`
  const seasonEpisode = episode - season.startEpisode + 1
  const label = `${season.label} EP${String(seasonEpisode).padStart(2, '0')}`
  return options?.includeGlobal === false ? label : `${label}（全局 EP${String(episode).padStart(2, '0')}）`
}
