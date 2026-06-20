import { CirclePlay } from 'lucide-react'
import { useRef, useState } from 'react'

type AudioButtonProps = {
  src: string
  label: string
  variant?: 'primary' | 'secondary'
}

export function AudioButton({ src, label, variant = 'primary' }: AudioButtonProps) {
  const [status, setStatus] = useState<'idle' | 'loading' | 'playing' | 'error'>('idle')
  const audioRef = useRef<HTMLAudioElement | null>(null)

  function handleClick() {
    audioRef.current?.pause()
    const audio = new Audio(src)
    audioRef.current = audio
    setStatus('loading')
    audio.addEventListener('playing', () => setStatus('playing'), { once: true })
    audio.addEventListener('ended', () => setStatus('idle'), { once: true })
    audio.addEventListener('error', () => setStatus('error'), { once: true })
    void audio.play().catch((error) => {
      console.error(error)
      setStatus('error')
    })
  }

  return (
    <button
      className={`icon-button${variant === 'secondary' ? ' secondary' : ''}`}
      type="button"
      onClick={handleClick}
      disabled={status === 'loading'}
    >
      <CirclePlay size={18} />
      <span>{status === 'loading' ? '加载中' : status === 'playing' ? '播放中' : status === 'error' ? '失败' : label}</span>
    </button>
  )
}
