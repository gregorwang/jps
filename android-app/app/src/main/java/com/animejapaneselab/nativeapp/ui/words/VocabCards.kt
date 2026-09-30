package com.animejapaneselab.nativeapp.ui.words

import android.content.Context
import org.json.JSONObject

/**
 * Hand-checked word cards (`assets/vocab_cards.json`, written offline and validated by
 * `archive-content-sources/vocab-cards-v1/check.py`). They replace the vocab rows' unreliable
 * reading / meaning / notes; [keep] = false marks fragments not worth studying on their own;
 * [easy] (optional 8th column) marks words every anime viewer already knows.
 */
data class VocabCardFix(
    val keep: Boolean,
    val reading: String,
    val lemma: String,
    val lemmaReading: String,
    val pos: String,
    val meaning: String,
    val note: String,
    /** Anime viewers know it already (なるほど, ありがとう): 辞書 offers to 斩 these in one go. */
    val easy: Boolean = false,
    /** The word itself, for ids that don't end in it (uuid / numbered ids). */
    val head: String = "",
)

object VocabCards {
    private const val Asset = "vocab_cards.json"

    /** 「台词「…」，」 at the start of a note repeats the line the card already shows under 原作里. */
    private val QuotedLine = Regex("""^台词[里中]?[：:]?「[^」]*」(?:中|里)?[，,、。：:]?\s*""")

    fun tidyNote(note: String): String = note.trim().replace(QuotedLine, "").trim()

    @Volatile
    private var cards: Map<String, VocabCardFix>? = null

    fun get(context: Context, id: String): VocabCardFix? = load(context)[id]

    fun load(context: Context): Map<String, VocabCardFix> {
        cards?.let { return it }
        synchronized(this) {
            cards?.let { return it }
            val parsed = runCatching {
                val json = context.applicationContext.assets.open(Asset).bufferedReader().use { it.readText() }
                val top = JSONObject(json)
                val root = top.getJSONObject("cards")
                val heads = top.optJSONObject("heads")
                root.keys().asSequence().associateWith { id ->
                    val row = root.getJSONArray(id)
                    VocabCardFix(
                        keep = row.optInt(0, 1) == 1,
                        reading = row.optString(1),
                        lemma = row.optString(2),
                        lemmaReading = row.optString(3),
                        pos = row.optString(4),
                        meaning = row.optString(5),
                        note = tidyNote(row.optString(6)),
                        easy = row.optInt(7, 0) == 1,
                        head = heads?.optString(id).orEmpty(),
                    )
                }
            }.getOrDefault(emptyMap())
            cards = parsed
            return parsed
        }
    }
}
