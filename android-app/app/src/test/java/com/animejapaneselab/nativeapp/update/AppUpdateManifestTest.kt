package com.animejapaneselab.nativeapp.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManifestTest {
    @Test
    fun validManifestRoundTripsWithoutLosingSecurityFields() {
        val release = release(versionCode = 7L)

        val restored = AppUpdateManifestCodec.decode(AppUpdateManifestCodec.encode(release))

        assertEquals(release, restored)
    }

    @Test
    fun malformedDigestIsRejected() {
        val raw = AppUpdateManifestCodec.encode(release()).replace("a".repeat(64), "unsafe")

        val error = assertThrows(AppUpdateException::class.java) {
            AppUpdateManifestCodec.decode(raw)
        }

        assertEquals(AppUpdateError.InvalidManifest, error.reason)
    }

    @Test
    fun malformedPublicationTimeIsRejected() {
        val raw = AppUpdateManifestCodec.encode(release()).replace(
            "2026-07-13T12:00:00.000Z",
            "not-a-time",
        )

        val error = assertThrows(AppUpdateException::class.java) {
            AppUpdateManifestCodec.decode(raw)
        }

        assertEquals(AppUpdateError.InvalidManifest, error.reason)
    }

    @Test
    fun onlyHigherVersionCodeCountsAsAnUpdate() {
        assertTrue(isNewerRelease(currentVersionCode = 2L, release(versionCode = 3L)))
        assertFalse(isNewerRelease(currentVersionCode = 2L, release(versionCode = 2L)))
        assertFalse(isNewerRelease(currentVersionCode = 2L, release(versionCode = 1L)))
    }

    private fun release(versionCode: Long = 2L) = AppUpdateRelease(
        schemaVersion = 1,
        versionCode = versionCode,
        versionName = "0.$versionCode.0",
        downloadUrl = "https://updates.example.test/v1/releases/$versionCode/apk",
        sha256 = "a".repeat(64),
        sizeBytes = 23_000_000L,
        releaseNotes = "应用内更新测试",
        publishedAt = "2026-07-13T12:00:00.000Z",
    )
}
