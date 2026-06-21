const ttsBaseUrl =
  import.meta.env.VITE_TTS_WORKER_URL ??
  'https://cloudflare-edge-tts.ishallnotwant123.workers.dev'

export const japaneseVoices = [
  { id: 'ja-JP-NanamiNeural', label: '七海' },
  { id: 'ja-JP-KeitaNeural', label: '圭太' },
] as const

export type JapaneseVoice = (typeof japaneseVoices)[number]['id']

export const defaultJapaneseVoice: JapaneseVoice = 'ja-JP-NanamiNeural'

const remoteAudioCache = new Map<string, string>()
const pendingRemoteAudio = new Map<string, Promise<string>>()
let browserVoiceReady: Promise<void> | null = null

export async function speakJapanese(text: string, voice: JapaneseVoice = defaultJapaneseVoice, options: { preferLocal?: boolean } = {}) {
  if (options.preferLocal !== false && speakJapaneseLocally(text)) return
  const url = await getRemoteTtsUrl(text, voice)
  const audio = new Audio(url)
  await audio.play()
}

export function preloadJapaneseTts(text: string, voice: JapaneseVoice = defaultJapaneseVoice) {
  if (typeof window === 'undefined' || !text) return
  void getRemoteTtsUrl(text, voice).catch(() => undefined)
}

async function getRemoteTtsUrl(text: string, voice: JapaneseVoice = defaultJapaneseVoice) {
  const key = `${voice}:${text}`
  const cached = remoteAudioCache.get(key)
  if (cached) return cached
  const pending = pendingRemoteAudio.get(key)
  if (pending) return pending

  const request = fetch(`${ttsBaseUrl}/tts`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, voice }),
  })
    .then(async (response) => {
      if (!response.ok) {
        const message = await response.text()
        throw new Error(message || `TTS failed with HTTP ${response.status}`)
      }
      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      remoteAudioCache.set(key, url)
      trimRemoteAudioCache()
      return url
    })
    .finally(() => pendingRemoteAudio.delete(key))

  pendingRemoteAudio.set(key, request)
  return request
}

function speakJapaneseLocally(text: string) {
  if (typeof window === 'undefined' || !('speechSynthesis' in window) || !('SpeechSynthesisUtterance' in window)) return false
  const voice = pickLocalJapaneseVoice()
  if (!voice) return false

  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'ja-JP'
  utterance.voice = voice
  utterance.rate = 0.96
  window.speechSynthesis.speak(utterance)
  return true
}

function pickLocalJapaneseVoice() {
  const voices = window.speechSynthesis.getVoices()
  const voice = voices.find((candidate) => candidate.lang.toLowerCase().startsWith('ja'))
  if (voice || browserVoiceReady) return voice
  browserVoiceReady = new Promise<void>((resolve) => {
    window.speechSynthesis.addEventListener('voiceschanged', () => resolve(), { once: true })
    window.setTimeout(resolve, 900)
  })
  void browserVoiceReady.then(() => {
    browserVoiceReady = null
  })
  return null
}

function trimRemoteAudioCache() {
  const maxEntries = 80
  if (remoteAudioCache.size <= maxEntries) return
  const removable = [...remoteAudioCache.keys()].slice(0, remoteAudioCache.size - maxEntries)
  for (const key of removable) {
    const url = remoteAudioCache.get(key)
    if (url) URL.revokeObjectURL(url)
    remoteAudioCache.delete(key)
  }
}
