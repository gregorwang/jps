export const latestManifestKey = 'releases/latest.json'

const sha256Pattern = /^[a-f0-9]{64}$/
const apkObjectKeyPattern = /^releases\/[1-9]\d*\/app-localSlim\.apk$/

export type StoredReleaseManifest = {
  schemaVersion: 1
  versionCode: number
  versionName: string
  apkObjectKey: string
  sha256: string
  sizeBytes: number
  releaseNotes: string
  publishedAt: string
}

export function releaseManifestKey(versionCode: number): string {
  return `releases/${versionCode}/manifest.json`
}

export function parseStoredReleaseManifest(input: unknown): StoredReleaseManifest {
  if (!input || typeof input !== 'object') {
    throw new Error('Release manifest must be a JSON object')
  }

  const value = input as Record<string, unknown>
  const schemaVersion = value.schemaVersion
  const versionCode = value.versionCode
  const versionName = typeof value.versionName === 'string' ? value.versionName.trim() : ''
  const apkObjectKey = typeof value.apkObjectKey === 'string' ? value.apkObjectKey.trim() : ''
  const sha256 = typeof value.sha256 === 'string' ? value.sha256.trim().toLowerCase() : ''
  const sizeBytes = value.sizeBytes
  const releaseNotes = typeof value.releaseNotes === 'string' ? value.releaseNotes.trim() : ''
  const publishedAt = typeof value.publishedAt === 'string' ? value.publishedAt.trim() : ''

  if (schemaVersion !== 1) throw new Error('Unsupported release manifest schema')
  if (!Number.isSafeInteger(versionCode) || (versionCode as number) <= 0) {
    throw new Error('Invalid Android versionCode')
  }
  if (!versionName || versionName.length > 80) throw new Error('Invalid Android versionName')
  if (!apkObjectKeyPattern.test(apkObjectKey)) throw new Error('Invalid APK object key')
  if (apkObjectKey !== `releases/${versionCode}/app-localSlim.apk`) {
    throw new Error('APK object key does not match versionCode')
  }
  if (!sha256Pattern.test(sha256)) throw new Error('Invalid APK SHA-256')
  if (!Number.isSafeInteger(sizeBytes) || (sizeBytes as number) <= 0 || (sizeBytes as number) > 512 * 1024 * 1024) {
    throw new Error('Invalid APK size')
  }
  if (releaseNotes.length > 8_000) throw new Error('Release notes are too long')
  if (!publishedAt || !Number.isFinite(Date.parse(publishedAt))) throw new Error('Invalid publication time')

  return {
    schemaVersion: 1,
    versionCode: versionCode as number,
    versionName,
    apkObjectKey,
    sha256,
    sizeBytes: sizeBytes as number,
    releaseNotes,
    publishedAt,
  }
}
