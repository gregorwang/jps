package com.animejapaneselab.nativeapp.update

import org.json.JSONException
import org.json.JSONObject
import java.time.DateTimeException
import java.time.Instant

data class AppUpdateRelease(
    val schemaVersion: Int,
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long,
    val releaseNotes: String,
    val publishedAt: String,
)

enum class AppUpdateError {
    NoRelease,
    Network,
    InvalidManifest,
    DownloadFailed,
    StorageUnavailable,
    IntegrityCheckFailed,
    PackageMismatch,
    SignatureMismatch,
    InstallPermissionRequired,
    InstallerUnavailable,
    Unknown,
}

internal class AppUpdateException(
    val reason: AppUpdateError,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

internal object AppUpdateManifestCodec {
    private val sha256Pattern = Regex("^[a-fA-F0-9]{64}$")

    fun decode(raw: String): AppUpdateRelease {
        val json = try {
            JSONObject(raw)
        } catch (error: JSONException) {
            throw AppUpdateException(AppUpdateError.InvalidManifest, "Update manifest is not valid JSON", error)
        }

        val schemaVersion = json.optInt("schemaVersion", -1)
        val versionCode = json.optLong("versionCode", -1L)
        val versionName = json.optString("versionName").trim()
        val downloadUrl = json.optString("downloadUrl").trim()
        val sha256 = json.optString("sha256").trim().lowercase()
        val sizeBytes = json.optLong("sizeBytes", -1L)
        val releaseNotes = json.optString("releaseNotes").trim()
        val publishedAt = json.optString("publishedAt").trim()

        if (schemaVersion != 1) invalid("Unsupported update manifest schema")
        if (versionCode <= 0L) invalid("Invalid update versionCode")
        if (versionName.isBlank() || versionName.length > MaxVersionNameLength) invalid("Invalid update versionName")
        if (downloadUrl.isBlank() || downloadUrl.length > MaxUrlLength) invalid("Invalid update download URL")
        if (!sha256Pattern.matches(sha256)) invalid("Invalid update SHA-256")
        if (sizeBytes <= 0L || sizeBytes > MaxApkSizeBytes) invalid("Invalid update APK size")
        if (releaseNotes.length > MaxReleaseNotesLength) invalid("Update release notes are too long")
        if (publishedAt.isBlank()) invalid("Missing update publication time")
        try {
            Instant.parse(publishedAt)
        } catch (_: DateTimeException) {
            invalid("Invalid update publication time")
        }

        return AppUpdateRelease(
            schemaVersion = schemaVersion,
            versionCode = versionCode,
            versionName = versionName,
            downloadUrl = downloadUrl,
            sha256 = sha256,
            sizeBytes = sizeBytes,
            releaseNotes = releaseNotes,
            publishedAt = publishedAt,
        )
    }

    fun encode(release: AppUpdateRelease): String = JSONObject()
        .put("schemaVersion", release.schemaVersion)
        .put("versionCode", release.versionCode)
        .put("versionName", release.versionName)
        .put("downloadUrl", release.downloadUrl)
        .put("sha256", release.sha256)
        .put("sizeBytes", release.sizeBytes)
        .put("releaseNotes", release.releaseNotes)
        .put("publishedAt", release.publishedAt)
        .toString()

    private fun invalid(message: String): Nothing {
        throw AppUpdateException(AppUpdateError.InvalidManifest, message)
    }

    private const val MaxVersionNameLength = 80
    private const val MaxUrlLength = 2_048
    private const val MaxReleaseNotesLength = 8_000
    private const val MaxApkSizeBytes = 512L * 1024L * 1024L
}

internal fun isNewerRelease(currentVersionCode: Long, release: AppUpdateRelease): Boolean =
    release.versionCode > currentVersionCode
