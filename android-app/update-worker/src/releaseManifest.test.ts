import { describe, expect, it } from 'vitest'
import { normalizeRange } from './index'
import { parseStoredReleaseManifest, releaseManifestKey } from './releaseManifest'

const validManifest = {
  schemaVersion: 1,
  versionCode: 2,
  versionName: '0.2.0',
  apkObjectKey: 'releases/2/app-localSlim.apk',
  sha256: 'a'.repeat(64),
  sizeBytes: 23_000_000,
  releaseNotes: '新增应用内更新。',
  publishedAt: '2026-07-13T12:00:00.000Z',
}

describe('parseStoredReleaseManifest', () => {
  it('accepts a valid immutable release record', () => {
    expect(parseStoredReleaseManifest(validManifest)).toEqual(validManifest)
    expect(releaseManifestKey(2)).toBe('releases/2/manifest.json')
  })

  it('rejects an APK object key that does not match versionCode', () => {
    expect(() => parseStoredReleaseManifest({
      ...validManifest,
      apkObjectKey: 'releases/3/app-localSlim.apk',
    })).toThrow('does not match versionCode')
  })

  it('rejects a malformed SHA-256 digest', () => {
    expect(() => parseStoredReleaseManifest({
      ...validManifest,
      sha256: 'not-a-digest',
    })).toThrow('Invalid APK SHA-256')
  })
})

describe('normalizeRange', () => {
  it('handles the R2 runtime shape when suffix exists but is undefined', () => {
    const runtimeRange = { offset: 0, length: 1024, suffix: undefined } as unknown as R2Range

    expect(normalizeRange(runtimeRange, 23_309_589)).toEqual({ offset: 0, length: 1024 })
  })

  it('normalizes a suffix range', () => {
    expect(normalizeRange({ suffix: 512 }, 2_048)).toEqual({ offset: 1_536, length: 512 })
  })
})
