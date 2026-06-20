import { Eraser } from 'lucide-react'
import { type PointerEvent, useEffect, useImperativeHandle, useRef, useState } from 'react'
import { drawStrokePath, type WritingStroke } from '../lib/writingValidation'

export type HandwritingCanvasHandle = {
  clear: () => void
  getStrokes: () => WritingStroke[]
  getSize: () => { width: number; height: number }
}

type HandwritingCanvasProps = {
  target?: string
  onClear?: () => void
  ref?: React.Ref<HandwritingCanvasHandle>
}

export function HandwritingCanvas({ target, onClear, ref }: HandwritingCanvasProps) {
  const backgroundRef = useRef<HTMLCanvasElement | null>(null)
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const drawingRef = useRef(false)
  const activeStrokeRef = useRef<WritingStroke>([])
  const strokesRef = useRef<WritingStroke[]>([])
  const [canvasSize, setCanvasSize] = useState({ width: 0, height: 0 })

  useImperativeHandle(ref, () => ({
    clear,
    getStrokes: () => strokesRef.current.map((stroke) => [...stroke]),
    getSize: () => {
      const rect = canvasRef.current?.getBoundingClientRect()
      return { width: rect?.width ?? 0, height: rect?.height ?? 0 }
    },
  }))

  useEffect(() => {
    const foreground = canvasRef.current
    const background = backgroundRef.current
    if (!foreground || !background) return

    function resize() {
      const rect = foreground!.getBoundingClientRect()
      const scale = window.devicePixelRatio || 1
      for (const canvas of [foreground!, background!]) {
        canvas.width = Math.max(1, Math.floor(rect.width * scale))
        canvas.height = Math.max(1, Math.floor(rect.height * scale))
        const context = canvas.getContext('2d')
        if (!context) continue
        context.setTransform(scale, 0, 0, scale, 0, 0)
        context.lineCap = 'round'
        context.lineJoin = 'round'
      }
      setCanvasSize({ width: rect.width, height: rect.height })
      redrawForeground()
    }

    resize()
    window.addEventListener('resize', resize)
    return () => window.removeEventListener('resize', resize)
  }, [])

  useEffect(() => {
    drawTarget()
  }, [target, canvasSize.width, canvasSize.height])

  function drawTarget() {
    const canvas = backgroundRef.current
    const context = canvas?.getContext('2d')
    if (!canvas || !context) return
    context.clearRect(0, 0, canvas.width, canvas.height)
    if (!target) return

    const rect = canvas.getBoundingClientRect()
    const visualLength = Math.max(1, Array.from(target).length)
    const fontSize = Math.floor(Math.min(rect.height * 0.72, (rect.width * 0.78) / Math.max(1, visualLength * 0.72)))
    context.fillStyle = 'rgba(32, 32, 29, 0.16)'
    context.strokeStyle = 'rgba(32, 32, 29, 0.09)'
    context.lineWidth = 2
    context.textAlign = 'center'
    context.textBaseline = 'middle'
    context.font = `800 ${fontSize}px Hiragino Sans, Yu Gothic, Meiryo, sans-serif`
    context.fillText(target, rect.width / 2, rect.height / 2)
    context.strokeText(target, rect.width / 2, rect.height / 2)
  }

  function redrawForeground() {
    const context = canvasRef.current?.getContext('2d')
    const canvas = canvasRef.current
    if (!context || !canvas) return
    context.clearRect(0, 0, canvas.width, canvas.height)
    configurePen(context)
    for (const stroke of strokesRef.current) {
      drawStrokePath(context, stroke)
    }
  }

  function configurePen(context: CanvasRenderingContext2D) {
    context.lineCap = 'round'
    context.lineJoin = 'round'
    context.lineWidth = 8
    context.strokeStyle = '#20201d'
  }

  function getPoint(event: PointerEvent<HTMLCanvasElement>) {
    const canvas = canvasRef.current!
    const rect = canvas.getBoundingClientRect()
    return {
      x: event.clientX - rect.left,
      y: event.clientY - rect.top,
      t: performance.now(),
      pressure: event.pressure,
    }
  }

  function handlePointerDown(event: PointerEvent<HTMLCanvasElement>) {
    event.preventDefault()
    const context = canvasRef.current?.getContext('2d')
    if (!context) return
    const point = getPoint(event)
    drawingRef.current = true
    activeStrokeRef.current = [point]
    event.currentTarget.setPointerCapture(event.pointerId)
    configurePen(context)
    context.beginPath()
    context.moveTo(point.x, point.y)
  }

  function handlePointerMove(event: PointerEvent<HTMLCanvasElement>) {
    if (!drawingRef.current) return
    event.preventDefault()
    const context = canvasRef.current?.getContext('2d')
    if (!context) return
    const point = getPoint(event)
    const stroke = activeStrokeRef.current
    const previous = stroke[stroke.length - 1]
    stroke.push(point)
    configurePen(context)
    if (previous) {
      const midpoint = { x: (previous.x + point.x) / 2, y: (previous.y + point.y) / 2 }
      context.quadraticCurveTo(previous.x, previous.y, midpoint.x, midpoint.y)
      context.stroke()
    }
  }

  function handlePointerUp(event: PointerEvent<HTMLCanvasElement>) {
    if (!drawingRef.current) return
    event.preventDefault()
    drawingRef.current = false
    if (activeStrokeRef.current.length > 0) {
      strokesRef.current = [...strokesRef.current, activeStrokeRef.current]
    }
    activeStrokeRef.current = []
    redrawForeground()
  }

  function clear() {
    const canvas = canvasRef.current
    const context = canvas?.getContext('2d')
    if (!canvas || !context) return
    strokesRef.current = []
    activeStrokeRef.current = []
    drawingRef.current = false
    context.clearRect(0, 0, canvas.width, canvas.height)
    onClear?.()
  }

  return (
    <div className="canvas-panel writing-canvas-panel">
      <div className="writing-canvas-stage">
        <canvas ref={backgroundRef} className="handwriting-canvas-layer handwriting-canvas-bg" aria-hidden="true" />
        <canvas
          ref={canvasRef}
          className="handwriting-canvas handwriting-canvas-fg"
          aria-label="手写练习区"
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
        />
      </div>
      <button className="icon-button secondary" type="button" onClick={clear}>
        <Eraser size={18} />
        <span>清空</span>
      </button>
    </div>
  )
}
