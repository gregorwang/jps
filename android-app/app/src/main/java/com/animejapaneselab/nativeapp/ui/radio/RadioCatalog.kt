package com.animejapaneselab.nativeapp.ui.radio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** z = Chinese narration, j = Japanese (generated), o = a line from the anime with its own audio. */
enum class SegKind { Zh, Ja, Orig }

data class RadioSeg(
    val kind: SegKind,
    val text: String,
    /** Full URL of the original clip ([SegKind.Orig]); empty otherwise. */
    val audioUrl: String,
    /** Chinese of a Japanese line, or the reading of a word. */
    val caption: String,
)

data class RadioTrack(
    val id: String,
    val no: String,
    val title: String,
    val gloss: String,
    val shelf: String,
    val segs: List<RadioSeg>,
    /** (first segment, label) — the progress line is cut into these. */
    val chapters: List<Pair<Int, String>>,
) {
    /** Rough length: Chinese ~4.5 chars/s, Japanese ~7 chars/s, plus the gaps. */
    val seconds: Int by lazy {
        segs.sumOf { s ->
            val chars = s.text.length.toDouble()
            (if (s.kind == SegKind.Zh) chars / 4.5 else chars / 7.0 + 0.6) + 0.4
        }.toInt()
    }
    val voiceCount: Int get() = segs.count { it.kind == SegKind.Orig }

    fun chapterAt(seg: Int): Int = chapters.indexOfLast { it.first <= seg }.coerceAtLeast(0)
}

data class RadioShelf(val id: String, val word: Boolean, val name: String, val tracks: List<RadioTrack>)

/**
 * 知識电台的节目单: `assets/radio_tracks.json`, built by archive-content-sources/radio/build_radio.py
 * (see RADIO_HANDOFF.md). Parsed once, off the main thread.
 */
object RadioCatalog {
    @Volatile private var shelves: List<RadioShelf>? = null

    fun peek(): List<RadioShelf>? = shelves

    @Synchronized
    fun load(context: Context): List<RadioShelf> {
        shelves?.let { return it }
        val root = JSONObject(context.assets.open("radio_tracks.json").bufferedReader().use { it.readText() })
        val base = root.optString("audioBase")
        val out = root.getJSONArray("shelves").objects().map { sh ->
            val name = sh.getString("name")
            RadioShelf(
                id = sh.getString("id"),
                word = sh.optString("tab") == "word",
                name = name,
                tracks = sh.getJSONArray("tracks").objects().map { t ->
                    RadioTrack(
                        id = t.getString("id"),
                        no = t.getString("no"),
                        title = t.getString("title"),
                        gloss = t.optString("gloss"),
                        shelf = name,
                        segs = t.getJSONArray("segs").let { a ->
                            List(a.length()) { i ->
                                val s = a.getJSONArray(i)
                                val audio = s.optString(2)
                                RadioSeg(
                                    kind = when (s.getString(0)) { "z" -> SegKind.Zh; "o" -> SegKind.Orig; else -> SegKind.Ja },
                                    text = s.getString(1),
                                    audioUrl = if (audio.isEmpty() || audio.startsWith("http")) audio else base + audio,
                                    caption = s.optString(3),
                                )
                            }
                        },
                        chapters = t.optJSONArray("chapters")?.let { a ->
                            List(a.length()) { i -> a.getJSONArray(i).let { it.getInt(0) to it.getString(1) } }
                        }.orEmpty(),
                    )
                },
            )
        }
        shelves = out
        return out
    }

    fun track(id: String): RadioTrack? = shelves?.firstNotNullOfOrNull { sh -> sh.tracks.firstOrNull { it.id == id } }

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
}
