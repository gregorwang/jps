import { Volume2 } from 'lucide-react'
import { useState } from 'react'
import { usePreferredJapaneseVoice } from '../lib/voicePreferences'
import { speakJapanese } from '../server/tts'

type TtsButtonProps = {
  text: string
  label?: string
}

export function TtsButton({ text, label = '播放' }: TtsButtonProps) {
  const [status, setStatus] = useState<'idle' | 'loading' | 'error'>('idle')
  const voice = usePreferredJapaneseVoice()

  async function handleClick() {
    setStatus('loading')
    try {
      await speakJapanese(text, voice)
      setStatus('idle')
    } catch (error) {
      console.error(error)
      setStatus('error')
    }
  }

  return (
    <div className="tts-control">
      <button className="icon-button" type="button" onClick={handleClick} disabled={status === 'loading'}>
        <Volume2 size={18} />
        <span>{status === 'loading' ? '生成中' : status === 'error' ? '失败' : label}</span>
      </button>
    </div>
  )
}
