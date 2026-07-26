package com.animejapaneselab.nativeapp.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

internal class AppUpdateClient {
    suspend fun fetchLatest(baseUrl: String): AppUpdateRelease = withContext(Dispatchers.IO) {
        val normalizedBase = baseUrl.trim().trimEnd('/')
        val baseUri = parseSecureUri(normalizedBase, AppUpdateError.InvalidManifest)
        val connection = try {
            URL("$normalizedBase/v1/latest").openConnection() as HttpURLConnection
        } catch (error: IOException) {
            throw AppUpdateException(AppUpdateError.Network, "Unable to open update service", error)
        }

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = ConnectTimeoutMillis
            connection.readTimeout = ReadTimeoutMillis
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-cache")

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use(::readLimitedUtf8).orEmpty()
            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                throw AppUpdateException(AppUpdateError.NoRelease, "No Android update is published")
            }
            if (status !in 200..299) {
                throw AppUpdateException(AppUpdateError.Network, "Update service returned HTTP $status")
            }

            val release = AppUpdateManifestCodec.decode(body)
            validateDownloadUri(baseUri, release)
            release
        } catch (error: AppUpdateException) {
            throw error
        } catch (error: IOException) {
            throw AppUpdateException(AppUpdateError.Network, "Unable to read update service", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun validateDownloadUri(baseUri: URI, release: AppUpdateRelease) {
        val downloadUri = parseSecureUri(release.downloadUrl, AppUpdateError.InvalidManifest)
        val expectedPath = "/v1/releases/${release.versionCode}/apk"
        if (
            !downloadUri.host.equals(baseUri.host, ignoreCase = true) ||
            effectivePort(downloadUri) != effectivePort(baseUri) ||
            downloadUri.path != expectedPath ||
            downloadUri.rawQuery != null ||
            downloadUri.rawFragment != null
        ) {
            throw AppUpdateException(AppUpdateError.InvalidManifest, "Update download URL is outside the configured service")
        }
    }

    private fun parseSecureUri(raw: String, errorType: AppUpdateError): URI {
        val uri = try {
            URI(raw)
        } catch (error: Exception) {
            throw AppUpdateException(errorType, "Invalid update service URL", error)
        }
        if (
            !uri.isAbsolute ||
            !uri.scheme.equals("https", ignoreCase = true) ||
            uri.host.isNullOrBlank() ||
            uri.rawUserInfo != null
        ) {
            throw AppUpdateException(errorType, "Update service must use HTTPS")
        }
        return uri
    }

    private fun effectivePort(uri: URI): Int = if (uri.port >= 0) uri.port else 443

    private fun readLimitedUtf8(input: InputStream): String {
        val buffer = ByteArray(ReadBufferSize)
        val output = ByteArrayOutputStream()
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MaxManifestBytes) {
                throw AppUpdateException(AppUpdateError.InvalidManifest, "Update manifest is too large")
            }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private companion object {
        const val ConnectTimeoutMillis = 15_000
        const val ReadTimeoutMillis = 25_000
        const val MaxManifestBytes = 64 * 1024
        const val ReadBufferSize = 4 * 1024
    }
}
