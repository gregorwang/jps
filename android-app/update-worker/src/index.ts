import {
  latestManifestKey,
  parseStoredReleaseManifest,
  releaseManifestKey,
  type StoredReleaseManifest,
} from './releaseManifest'

type Env = {
  ANDROID_RELEASES: R2Bucket
}

const apkContentType = 'application/vnd.android.package-archive'
const releasePathPattern = /^\/v1\/releases\/([1-9]\d*)\/apk$/

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url)

    try {
      if (url.pathname === '/health' && request.method === 'GET') {
        return json({ ok: true }, 200, 'no-store')
      }

      if (url.pathname === '/v1/latest' && request.method === 'GET') {
        const manifest = await readManifest(env.ANDROID_RELEASES, latestManifestKey)
        return json(
          {
            schemaVersion: manifest.schemaVersion,
            versionCode: manifest.versionCode,
            versionName: manifest.versionName,
            downloadUrl: `${url.origin}/v1/releases/${manifest.versionCode}/apk`,
            sha256: manifest.sha256,
            sizeBytes: manifest.sizeBytes,
            releaseNotes: manifest.releaseNotes,
            publishedAt: manifest.publishedAt,
          },
          200,
          'public, max-age=60, must-revalidate',
        )
      }

      const releaseMatch = url.pathname.match(releasePathPattern)
      if (releaseMatch && (request.method === 'GET' || request.method === 'HEAD')) {
        const versionCode = Number(releaseMatch[1])
        const manifest = await readManifest(env.ANDROID_RELEASES, releaseManifestKey(versionCode))
        if (manifest.versionCode !== versionCode) throw new HttpError(409, 'Release manifest mismatch')
        return serveApk(request, env.ANDROID_RELEASES, manifest)
      }

      return json({ error: { message: 'Not found' } }, 404, 'no-store')
    } catch (error) {
      const status = error instanceof HttpError ? error.status : 500
      const message = error instanceof Error ? error.message : 'Unknown update service error'
      return json({ error: { message } }, status, 'no-store')
    }
  },
} satisfies ExportedHandler<Env>

class HttpError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
  }
}

async function readManifest(bucket: R2Bucket, key: string): Promise<StoredReleaseManifest> {
  const object = await bucket.get(key)
  if (!object) throw new HttpError(404, 'No Android release has been published')

  let raw: unknown
  try {
    raw = await object.json<unknown>()
  } catch {
    throw new HttpError(500, 'Stored Android release manifest is not valid JSON')
  }

  try {
    return parseStoredReleaseManifest(raw)
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Invalid Android release manifest'
    throw new HttpError(500, message)
  }
}

async function serveApk(
  request: Request,
  bucket: R2Bucket,
  manifest: StoredReleaseManifest,
): Promise<Response> {
  if (request.method === 'HEAD') {
    const object = await bucket.head(manifest.apkObjectKey)
    if (!object) throw new HttpError(404, 'Android APK not found')
    assertObjectSize(object.size, manifest.sizeBytes)
    return new Response(null, {
      status: 200,
      headers: apkHeaders(object, manifest, object.size),
    })
  }

  const object = await bucket.get(manifest.apkObjectKey, {
    onlyIf: request.headers,
    range: request.headers,
  })
  if (!object) throw new HttpError(404, 'Android APK not found')
  assertObjectSize(object.size, manifest.sizeBytes)

  if (!('body' in object)) {
    return new Response(null, {
      status: 412,
      headers: apkHeaders(object, manifest, object.size),
    })
  }

  const headers = apkHeaders(object, manifest, object.size)
  const normalizedRange = normalizeRange(object.range, object.size)
  if (normalizedRange) {
    headers.set('Content-Range', `bytes ${normalizedRange.offset}-${normalizedRange.offset + normalizedRange.length - 1}/${object.size}`)
    headers.set('Content-Length', String(normalizedRange.length))
  }

  return new Response(object.body, {
    status: normalizedRange ? 206 : 200,
    headers,
  })
}

function apkHeaders(object: R2Object, manifest: StoredReleaseManifest, contentLength: number): Headers {
  const headers = new Headers()
  object.writeHttpMetadata(headers)
  headers.set('Content-Type', apkContentType)
  headers.set('Content-Disposition', `attachment; filename="anime-japanese-lab-${manifest.versionCode}.apk"`)
  headers.set('Cache-Control', 'public, max-age=31536000, immutable')
  headers.set('Accept-Ranges', 'bytes')
  headers.set('ETag', object.httpEtag)
  headers.set('Content-Length', String(contentLength))
  headers.set('X-Content-Type-Options', 'nosniff')
  return headers
}

export function normalizeRange(
  range: R2Range | undefined,
  totalSize: number,
): { offset: number; length: number } | null {
  if (!range) return null
  const values = range as { offset?: number; length?: number; suffix?: number }
  if (typeof values.suffix === 'number') {
    const length = Math.min(values.suffix, totalSize)
    return { offset: totalSize - length, length }
  }
  const offset = values.offset ?? 0
  const length = Math.min(values.length ?? totalSize - offset, totalSize - offset)
  return { offset, length }
}

function assertObjectSize(actual: number, expected: number): void {
  if (actual !== expected) throw new HttpError(409, 'APK size does not match its release manifest')
}

function json(value: unknown, status: number, cacheControl: string): Response {
  return Response.json(value, {
    status,
    headers: {
      'Cache-Control': cacheControl,
      'X-Content-Type-Options': 'nosniff',
    },
  })
}
