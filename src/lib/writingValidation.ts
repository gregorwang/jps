export type WritingPoint = {
  x: number
  y: number
  t: number
  pressure?: number
}

export type WritingStroke = WritingPoint[]

export type WritingValidationReason = 'empty' | 'too_short' | 'too_far' | 'low_coverage'

export type WritingValidationMetrics = {
  coverageRatio: number
  offTargetRatio: number
  strokeLength: number
  durationMs: number
}

export type WritingValidationResult = {
  passed: boolean
  reason?: WritingValidationReason
  metrics: WritingValidationMetrics
}

export type WritingValidationOptions = {
  width: number
  height: number
  target: string
  fontFamily?: string
  minStrokeLength?: number
  minCoverageRatio?: number
  maxOffTargetRatio?: number
  targetLineWidth?: number
  userLineWidth?: number
  toleranceRadius?: number
}

type MaskData = {
  mask: Uint8Array
  pixels: number
  width: number
  height: number
}

export function validateWriting(strokes: WritingStroke[], options: WritingValidationOptions): WritingValidationResult {
  const strokeLength = measureStrokeLength(strokes)
  const durationMs = measureDuration(strokes)
  const baseMetrics = { coverageRatio: 0, offTargetRatio: 1, strokeLength, durationMs }

  if (strokes.every((stroke) => stroke.length === 0)) {
    return { passed: false, reason: 'empty', metrics: baseMetrics }
  }

  const minStrokeLength = options.minStrokeLength ?? Math.min(options.width, options.height) * 0.22
  if (strokeLength < minStrokeLength) {
    return { passed: false, reason: 'too_short', metrics: baseMetrics }
  }

  const targetMask = createTargetMask(options)
  const userMask = createUserMask(strokes, options)
  const tolerantTargetMask = dilateMask(targetMask, options.toleranceRadius ?? 18)
  const tolerantUserMask = dilateMask(userMask, options.toleranceRadius ?? 14)

  const metrics = calculateMetrics(targetMask, tolerantTargetMask, userMask, tolerantUserMask, strokeLength, durationMs)
  const minCoverageRatio = options.minCoverageRatio ?? 0.48
  const maxOffTargetRatio = options.maxOffTargetRatio ?? 0.4

  if (metrics.offTargetRatio > maxOffTargetRatio) {
    return { passed: false, reason: 'too_far', metrics }
  }
  if (metrics.coverageRatio < minCoverageRatio) {
    return { passed: false, reason: 'low_coverage', metrics }
  }

  return { passed: true, metrics }
}

export function decideWritingPass(
  metrics: WritingValidationMetrics,
  thresholds: { minCoverageRatio?: number; maxOffTargetRatio?: number; minStrokeLength?: number } = {},
): WritingValidationResult {
  if (metrics.strokeLength <= 0) return { passed: false, reason: 'empty', metrics }
  if (metrics.strokeLength < (thresholds.minStrokeLength ?? 80)) return { passed: false, reason: 'too_short', metrics }
  if (metrics.offTargetRatio > (thresholds.maxOffTargetRatio ?? 0.4)) return { passed: false, reason: 'too_far', metrics }
  if (metrics.coverageRatio < (thresholds.minCoverageRatio ?? 0.48)) return { passed: false, reason: 'low_coverage', metrics }
  return { passed: true, metrics }
}

export function measureStrokeLength(strokes: WritingStroke[]) {
  return strokes.reduce((total, stroke) => {
    let length = total
    for (let index = 1; index < stroke.length; index += 1) {
      length += distance(stroke[index - 1], stroke[index])
    }
    return length
  }, 0)
}

function measureDuration(strokes: WritingStroke[]) {
  const points = strokes.flat()
  if (points.length < 2) return 0
  return Math.max(0, points[points.length - 1].t - points[0].t)
}

function distance(a: WritingPoint, b: WritingPoint) {
  return Math.hypot(a.x - b.x, a.y - b.y)
}

function createTargetMask(options: WritingValidationOptions): MaskData {
  const canvas = document.createElement('canvas')
  const scale = 0.55
  canvas.width = Math.max(1, Math.round(options.width * scale))
  canvas.height = Math.max(1, Math.round(options.height * scale))
  const context = canvas.getContext('2d', { willReadFrequently: true })
  if (!context) return emptyMask(canvas.width, canvas.height)

  const fontSize = targetFontSize(options.target, canvas.width, canvas.height)
  context.clearRect(0, 0, canvas.width, canvas.height)
  context.fillStyle = '#000'
  context.strokeStyle = '#000'
  context.lineWidth = options.targetLineWidth ?? 2
  context.lineJoin = 'round'
  context.lineCap = 'round'
  context.textAlign = 'center'
  context.textBaseline = 'middle'
  context.font = `800 ${fontSize}px ${options.fontFamily ?? 'Hiragino Sans, Yu Gothic, Meiryo, sans-serif'}`
  context.fillText(options.target, canvas.width / 2, canvas.height / 2)
  context.strokeText(options.target, canvas.width / 2, canvas.height / 2)
  return readMask(context, canvas.width, canvas.height)
}

function createUserMask(strokes: WritingStroke[], options: WritingValidationOptions): MaskData {
  const canvas = document.createElement('canvas')
  const scale = 0.55
  canvas.width = Math.max(1, Math.round(options.width * scale))
  canvas.height = Math.max(1, Math.round(options.height * scale))
  const context = canvas.getContext('2d', { willReadFrequently: true })
  if (!context) return emptyMask(canvas.width, canvas.height)

  context.scale(scale, scale)
  context.lineCap = 'round'
  context.lineJoin = 'round'
  context.lineWidth = options.userLineWidth ?? 22
  context.strokeStyle = '#000'
  for (const stroke of strokes) {
    drawStrokePath(context, stroke)
  }
  return readMask(context, canvas.width, canvas.height)
}

export function drawStrokePath(context: CanvasRenderingContext2D, stroke: WritingStroke) {
  if (stroke.length === 0) return
  context.beginPath()
  context.moveTo(stroke[0].x, stroke[0].y)
  if (stroke.length === 1) {
    context.lineTo(stroke[0].x + 0.1, stroke[0].y + 0.1)
  } else {
    for (let index = 1; index < stroke.length - 1; index += 1) {
      const current = stroke[index]
      const next = stroke[index + 1]
      context.quadraticCurveTo(current.x, current.y, (current.x + next.x) / 2, (current.y + next.y) / 2)
    }
    const last = stroke[stroke.length - 1]
    context.lineTo(last.x, last.y)
  }
  context.stroke()
}

function targetFontSize(target: string, width: number, height: number) {
  const visualLength = Math.max(1, Array.from(target).length)
  const widthBound = (width * 0.78) / Math.max(1, visualLength * 0.72)
  return Math.floor(Math.min(height * 0.72, widthBound))
}

function readMask(context: CanvasRenderingContext2D, width: number, height: number): MaskData {
  const image = context.getImageData(0, 0, width, height)
  const mask = new Uint8Array(width * height)
  let pixels = 0
  for (let index = 0; index < mask.length; index += 1) {
    if (image.data[index * 4 + 3] > 24) {
      mask[index] = 1
      pixels += 1
    }
  }
  return { mask, pixels, width, height }
}

function emptyMask(width: number, height: number): MaskData {
  return { mask: new Uint8Array(width * height), pixels: 0, width, height }
}

function dilateMask(data: MaskData, radiusCssPx: number): MaskData {
  const radius = Math.max(1, Math.round(radiusCssPx * 0.55))
  const result = new Uint8Array(data.mask.length)
  let pixels = 0
  for (let y = 0; y < data.height; y += 1) {
    for (let x = 0; x < data.width; x += 1) {
      const index = y * data.width + x
      if (!data.mask[index]) continue
      for (let dy = -radius; dy <= radius; dy += 1) {
        for (let dx = -radius; dx <= radius; dx += 1) {
          if (dx * dx + dy * dy > radius * radius) continue
          const nx = x + dx
          const ny = y + dy
          if (nx < 0 || ny < 0 || nx >= data.width || ny >= data.height) continue
          const nextIndex = ny * data.width + nx
          if (!result[nextIndex]) {
            result[nextIndex] = 1
            pixels += 1
          }
        }
      }
    }
  }
  return { mask: result, pixels, width: data.width, height: data.height }
}

function calculateMetrics(
  targetMask: MaskData,
  tolerantTargetMask: MaskData,
  userMask: MaskData,
  tolerantUserMask: MaskData,
  strokeLength: number,
  durationMs: number,
): WritingValidationMetrics {
  let coveredTarget = 0
  let onTargetUser = 0

  for (let index = 0; index < targetMask.mask.length; index += 1) {
    if (targetMask.mask[index] && tolerantUserMask.mask[index]) coveredTarget += 1
    if (userMask.mask[index] && tolerantTargetMask.mask[index]) onTargetUser += 1
  }

  const userPixels = Math.max(1, userMask.pixels)
  return {
    coverageRatio: targetMask.pixels ? coveredTarget / targetMask.pixels : 0,
    offTargetRatio: 1 - onTargetUser / userPixels,
    strokeLength,
    durationMs,
  }
}
