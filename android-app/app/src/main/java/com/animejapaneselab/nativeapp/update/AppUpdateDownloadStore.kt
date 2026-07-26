package com.animejapaneselab.nativeapp.update

import android.content.Context
import androidx.core.content.edit

internal data class StoredAppUpdateDownload(
    val downloadId: Long,
    val fileName: String,
    val release: AppUpdateRelease,
)

internal class AppUpdateDownloadStore(context: Context) {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    fun read(): StoredAppUpdateDownload? {
        val downloadId = preferences.getLong(DownloadIdKey, MissingDownloadId)
        val fileName = preferences.getString(FileNameKey, null)?.trim().orEmpty()
        val releaseJson = preferences.getString(ReleaseKey, null)?.trim().orEmpty()
        if (downloadId == MissingDownloadId || fileName.isBlank() || releaseJson.isBlank()) return null

        val release = runCatching { AppUpdateManifestCodec.decode(releaseJson) }.getOrNull()
            ?: return null
        return StoredAppUpdateDownload(downloadId, fileName, release)
    }

    fun write(download: StoredAppUpdateDownload) {
        preferences.edit(commit = true) {
            putLong(DownloadIdKey, download.downloadId)
            putString(FileNameKey, download.fileName)
            putString(ReleaseKey, AppUpdateManifestCodec.encode(download.release))
        }
    }

    fun clear() {
        preferences.edit(commit = true) { clear() }
    }

    private companion object {
        const val PreferencesName = "anime-japanese-lab-app-updates"
        const val DownloadIdKey = "download-id"
        const val FileNameKey = "file-name"
        const val ReleaseKey = "release"
        const val MissingDownloadId = -1L
    }
}
