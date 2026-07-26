package com.animejapaneselab.nativeapp.update

import android.app.DownloadManager
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

internal sealed interface AppUpdateDownloadSnapshot {
    data object None : AppUpdateDownloadSnapshot

    data class InProgress(
        val release: AppUpdateRelease,
        val progressPercent: Int?,
    ) : AppUpdateDownloadSnapshot

    data class Ready(val release: AppUpdateRelease) : AppUpdateDownloadSnapshot

    data class Failed(
        val release: AppUpdateRelease,
        val error: AppUpdateError,
    ) : AppUpdateDownloadSnapshot
}

internal class AppUpdateManager(context: Context) {
    private val appContext = context.applicationContext
    private val downloadManager = requireNotNull(appContext.getSystemService(DownloadManager::class.java))
    private val store = AppUpdateDownloadStore(appContext)

    suspend fun startDownload(release: AppUpdateRelease): AppUpdateDownloadSnapshot = withContext(Dispatchers.IO) {
        discardStoredDownload()
        val fileName = fileNameFor(release)
        val destination = updateFile(fileName)
        if (destination.exists() && !destination.delete()) {
            throw AppUpdateException(AppUpdateError.StorageUnavailable, "Unable to replace previous update file")
        }

        val request = DownloadManager.Request(Uri.parse(release.downloadUrl)).apply {
            setTitle("Nihongo Lab ${release.versionName}")
            setDescription("正在下载应用更新")
            setMimeType(ApkMimeType)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                "$UpdateDirectoryName/$fileName",
            )
        }

        val downloadId = try {
            downloadManager.enqueue(request)
        } catch (error: Exception) {
            throw AppUpdateException(AppUpdateError.DownloadFailed, "Unable to enqueue Android update", error)
        }
        store.write(StoredAppUpdateDownload(downloadId, fileName, release))
        AppUpdateDownloadSnapshot.InProgress(release, progressPercent = 0)
    }

    suspend fun readDownloadSnapshot(currentVersionCode: Long): AppUpdateDownloadSnapshot =
        withContext(Dispatchers.IO) {
            val stored = store.read() ?: return@withContext AppUpdateDownloadSnapshot.None
            if (stored.fileName != fileNameFor(stored.release) || stored.release.versionCode <= currentVersionCode) {
                discardStoredDownload(stored)
                return@withContext AppUpdateDownloadSnapshot.None
            }

            val cursor = try {
                downloadManager.query(DownloadManager.Query().setFilterById(stored.downloadId))
            } catch (error: Exception) {
                discardStoredDownload(stored)
                return@withContext AppUpdateDownloadSnapshot.Failed(stored.release, AppUpdateError.DownloadFailed)
            }

            cursor.use {
                if (!it.moveToFirst()) {
                    discardStoredDownload(stored)
                    return@withContext AppUpdateDownloadSnapshot.Failed(stored.release, AppUpdateError.DownloadFailed)
                }

                return@withContext when (it.intColumn(DownloadManager.COLUMN_STATUS)) {
                    DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_RUNNING,
                    DownloadManager.STATUS_PAUSED,
                    -> AppUpdateDownloadSnapshot.InProgress(
                        release = stored.release,
                        progressPercent = downloadProgress(
                            downloadedBytes = it.longColumn(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                            totalBytes = it.longColumn(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                .takeIf { total -> total > 0L }
                                ?: stored.release.sizeBytes,
                        ),
                    )

                    DownloadManager.STATUS_SUCCESSFUL -> verifyCompletedDownload(stored)
                    DownloadManager.STATUS_FAILED -> {
                        discardStoredDownload(stored)
                        AppUpdateDownloadSnapshot.Failed(stored.release, AppUpdateError.DownloadFailed)
                    }

                    else -> {
                        discardStoredDownload(stored)
                        AppUpdateDownloadSnapshot.Failed(stored.release, AppUpdateError.DownloadFailed)
                    }
                }
            }
        }

    fun canRequestPackageInstalls(): Boolean = appContext.packageManager.canRequestPackageInstalls()

    fun buildUnknownSourcesIntent(): Intent {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${appContext.packageName}"),
        )
        return makeSystemActivityExplicit(intent)
    }

    suspend fun buildInstallIntent(release: AppUpdateRelease): Intent = withContext(Dispatchers.IO) {
        val stored = store.read()
            ?.takeIf { it.release.versionCode == release.versionCode && it.fileName == fileNameFor(release) }
            ?: throw AppUpdateException(AppUpdateError.IntegrityCheckFailed, "Downloaded update record is missing")
        val verified = verifyCompletedDownload(stored)
        if (verified !is AppUpdateDownloadSnapshot.Ready) {
            val error = (verified as? AppUpdateDownloadSnapshot.Failed)?.error
                ?: AppUpdateError.IntegrityCheckFailed
            throw AppUpdateException(error, "Downloaded update is no longer valid")
        }

        val file = updateFile(stored.fileName)
        val contentUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, ApkMimeType)
            clipData = ClipData.newRawUri("Nihongo Lab update", contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        makeSystemActivityExplicit(intent)
    }

    private fun verifyCompletedDownload(stored: StoredAppUpdateDownload): AppUpdateDownloadSnapshot {
        val file = try {
            updateFile(stored.fileName)
        } catch (error: AppUpdateException) {
            discardStoredDownload(stored)
            return AppUpdateDownloadSnapshot.Failed(stored.release, error.reason)
        }
        val failure = try {
            when {
                !file.isFile || file.length() != stored.release.sizeBytes -> AppUpdateError.IntegrityCheckFailed
                !sha256(file).equals(stored.release.sha256, ignoreCase = true) -> AppUpdateError.IntegrityCheckFailed
                else -> verifyPackage(file, stored.release)
            }
        } catch (_: Exception) {
            AppUpdateError.IntegrityCheckFailed
        }
        if (failure != null) {
            discardStoredDownload(stored)
            return AppUpdateDownloadSnapshot.Failed(stored.release, failure)
        }
        return AppUpdateDownloadSnapshot.Ready(stored.release)
    }

    private fun verifyPackage(file: File, release: AppUpdateRelease): AppUpdateError? {
        val packageManager = appContext.packageManager
        val archiveInfo = packageArchiveInfo(packageManager, file)
            ?: return AppUpdateError.PackageMismatch
        if (archiveInfo.packageName != appContext.packageName) return AppUpdateError.PackageMismatch
        if (PackageInfoCompat.getLongVersionCode(archiveInfo) != release.versionCode) {
            return AppUpdateError.PackageMismatch
        }

        val installedInfo = installedPackageInfo(packageManager)
            ?: return AppUpdateError.PackageMismatch
        val installedSigners = signerDigests(installedInfo)
        val archiveSigners = signerDigests(archiveInfo)
        return if (installedSigners.isNotEmpty() && installedSigners == archiveSigners) {
            null
        } else {
            AppUpdateError.SignatureMismatch
        }
    }

    @Suppress("DEPRECATION")
    private fun packageArchiveInfo(packageManager: PackageManager, file: File): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            packageManager.getPackageArchiveInfo(file.absolutePath, flags)
        }
    }

    @Suppress("DEPRECATION")
    private fun installedPackageInfo(packageManager: PackageManager): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(appContext.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                packageManager.getPackageInfo(appContext.packageName, flags)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun signerDigests(packageInfo: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners.orEmpty()
        } else {
            packageInfo.signatures.orEmpty()
        }
        return signatures.mapTo(mutableSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .toHexString()
        }
    }

    private fun makeSystemActivityExplicit(intent: Intent): Intent {
        val packageManager = appContext.packageManager
        val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        val target = matches
            .map { it.activityInfo }
            .firstOrNull { activity ->
                val flags = activity.applicationInfo.flags
                flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            }
            ?: throw AppUpdateException(AppUpdateError.InstallerUnavailable, "No trusted system installer is available")
        return Intent(intent).setComponent(ComponentName(target.packageName, target.name))
    }

    private fun updateFile(fileName: String): File {
        val downloads = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw AppUpdateException(AppUpdateError.StorageUnavailable, "External files directory is unavailable")
        val updateDirectory = File(downloads, UpdateDirectoryName)
        if ((!updateDirectory.exists() && !updateDirectory.mkdirs()) || !updateDirectory.isDirectory) {
            throw AppUpdateException(AppUpdateError.StorageUnavailable, "Unable to create update directory")
        }
        val canonicalDirectory = try {
            updateDirectory.canonicalFile
        } catch (error: IOException) {
            throw AppUpdateException(AppUpdateError.StorageUnavailable, "Unable to resolve update directory", error)
        }
        val file = try {
            File(canonicalDirectory, fileName).canonicalFile
        } catch (error: IOException) {
            throw AppUpdateException(AppUpdateError.StorageUnavailable, "Unable to resolve update file", error)
        }
        if (file.parentFile != canonicalDirectory) {
            throw AppUpdateException(AppUpdateError.StorageUnavailable, "Unsafe update file path")
        }
        return file
    }

    private fun discardStoredDownload(stored: StoredAppUpdateDownload? = store.read()) {
        if (stored != null) {
            runCatching { downloadManager.remove(stored.downloadId) }
            runCatching { updateFile(stored.fileName).delete() }
        }
        store.clear()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(HashBufferSize)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().toHexString()
    }

    private fun fileNameFor(release: AppUpdateRelease): String =
        "app-localSlim-${release.versionCode}.apk"

    private fun android.database.Cursor.intColumn(name: String): Int = getInt(getColumnIndexOrThrow(name))

    private fun android.database.Cursor.longColumn(name: String): Long = getLong(getColumnIndexOrThrow(name))

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte ->
        (byte.toInt() and 0xff).toString(radix = 16).padStart(length = 2, padChar = '0')
    }

    private fun downloadProgress(downloadedBytes: Long, totalBytes: Long): Int? {
        if (downloadedBytes < 0L || totalBytes <= 0L) return null
        return ((downloadedBytes.coerceAtMost(totalBytes) * 100L) / totalBytes).toInt()
    }

    private companion object {
        const val ApkMimeType = "application/vnd.android.package-archive"
        const val UpdateDirectoryName = "app-updates"
        const val HashBufferSize = 64 * 1024
    }
}
