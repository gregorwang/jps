package com.animejapaneselab.nativeapp.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Local furigana cache so each sentence is annotated at most once per install.
 * Lives in its own SharedPreferences file: backup rules already exclude all
 * SharedPreferences, and the main store stays small.
 */
class FuriganaCache(context: Context) {
    private val preferences = context.getSharedPreferences("ajl-furigana-cache", Context.MODE_PRIVATE)

    fun read(text: String): FuriganaResult? {
        val raw = preferences.getString(entryKey(text), null) ?: return null
        return decode(raw)
    }

    fun write(text: String, result: FuriganaResult) {
        if (result.segments.isEmpty()) return
        val key = entryKey(text)
        val order = readOrder().filterNot { it == key } + key
        val overflow = (order.size - MaxEntries).coerceAtLeast(0)
        val evicted = order.take(overflow)
        preferences.edit {
            putString(key, encode(result))
            evicted.forEach { remove(it) }
            putString(OrderKey, JSONArray(order.drop(overflow)).toString())
        }
    }

    private fun readOrder(): List<String> {
        val raw = preferences.getString(OrderKey, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun entryKey(text: String): String = "f-" + sha1(text)

    private companion object {
        const val OrderKey = "entry-order"
        const val MaxEntries = 600

        fun sha1(value: String): String {
            return MessageDigest.getInstance("SHA-1")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString(separator = "") { "%02x".format(it) }
        }

        fun encode(result: FuriganaResult): String {
            val segments = JSONArray()
            result.segments.forEach { segment ->
                segments.put(JSONObject().put("t", segment.text).put("r", segment.reading))
            }
            return JSONObject().put("segments", segments).toString()
        }

        fun decode(raw: String): FuriganaResult? {
            return runCatching {
                val array = JSONObject(raw).optJSONArray("segments") ?: JSONArray()
                val segments = buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        val text = item.optString("t")
                        if (text.isNotEmpty()) {
                            add(FuriganaSegment(text = text, reading = item.optString("r")))
                        }
                    }
                }
                segments.takeIf { it.isNotEmpty() }?.let(::FuriganaResult)
            }.getOrNull()
        }
    }
}
