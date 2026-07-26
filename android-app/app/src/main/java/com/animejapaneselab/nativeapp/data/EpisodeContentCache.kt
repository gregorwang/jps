package com.animejapaneselab.nativeapp.data

import java.io.File
import java.security.MessageDigest

/**
 * Disk cache for read-only catalog/content GET responses so an episode that was
 * opened once keeps working offline. Stores the raw response JSON per request
 * path (query included); parsing stays in [RemoteLabClient], so cached and live
 * payloads run through the same code path. Never used for auth, progress, or
 * AI endpoints — those must stay live.
 */
class EpisodeContentCache(rootDirectory: File) {
    private val directory = File(rootDirectory, "content-cache")

    fun read(path: String): String? {
        val file = entryFile(path)
        return runCatching {
            if (file.isFile) file.readText(Charsets.UTF_8).ifBlank { null } else null
        }.getOrNull()
    }

    fun write(path: String, body: String) {
        if (body.isBlank()) return
        runCatching {
            if (!directory.isDirectory) directory.mkdirs()
            val temp = File(directory, entryName(path) + ".tmp")
            temp.writeText(body, Charsets.UTF_8)
            val target = entryFile(path)
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                target.writeText(body, Charsets.UTF_8)
                temp.delete()
            }
            prune()
        }
    }

    private fun prune() {
        val files = directory.listFiles { file -> file.isFile && file.name.endsWith(".json") } ?: return
        if (files.size <= MaxEntries) return
        files.sortedBy(File::lastModified)
            .take(files.size - MaxEntries)
            .forEach { stale -> runCatching { stale.delete() } }
    }

    private fun entryFile(path: String): File = File(directory, entryName(path) + ".json")

    private fun entryName(path: String): String = sha1(path)

    private companion object {
        const val MaxEntries = 400

        fun sha1(value: String): String {
            return MessageDigest.getInstance("SHA-1")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString(separator = "") { "%02x".format(it) }
        }
    }
}
