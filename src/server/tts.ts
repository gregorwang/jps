const ttsBaseUrl =
  import.meta.env.VITE_TTS_WORKER_URL ??
  'https://cloudflare-edge-tts.ishallnotwant123.workers.dev'

export const japaneseVoices = [
  { id: 'ja-JP-NanamiNeural', label: '七海' },
  { id: 'ja-JP-KeitaNeural', label: '圭太' },
] as const

export type JapaneseVoice = (typeof japaneseVoices)[number]['id']

export const defaultJapaneseVoice: JapaneseVoice = 'ja-JP-NanamiNeural'

export async function speakJapanese(text: string, voice: JapaneseVoice = defaultJapaneseVoice) {
  const response = await fetch(`${ttsBaseUrl}/tts`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text, voice }),
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `TTS failed with HTTP ${response.status}`)
  }

  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const audio = new Audio(url)
  audio.addEventListener('ended', () => URL.revokeObjectURL(url), { once: true })
  await audio.play()
}
