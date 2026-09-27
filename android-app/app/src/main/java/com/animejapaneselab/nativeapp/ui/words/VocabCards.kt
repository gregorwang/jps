package com.animejapaneselab.nativeapp.ui.words

import android.content.Context
import org.json.JSONObject

/**
 * Hand-checked word cards (`assets/vocab_cards.json`, written offline and validated by
 * `archive-content-sources/vocab-cards-v1/check.py`). They replace the vocab rows' unreliable
 * reading / meaning / notes; [keep] = false marks fragments not worth studying on their own.
 */
data class VocabCardFix(
    val keep: Boolean,
    val reading: String,
    val lemma: String,
    val lemmaReading: String,
    val pos: String,
    val meaning: String,
    val note: String,
)

object VocabCards {
    private const val Asset = "vocab_cards.json"

    @Volatile
    private var cards: Map<String, VocabCardFix>? = null

    fun get(context: Context, id: String): VocabCardFix? = load(context)[id]

    fun load(context: Context): Map<String, VocabCardFix> {
        cards?.let { return it }
        synchronized(this) {
            cards?.let { return it }
            val parsed = runCatching {
                val json = context.applicationContext.assets.open(Asset).bufferedReader().use { it.readText() }
                val root = JSONObject(json).getJSONObject("cards")
                root.keys().asSequence().associateWith { id ->
                    val row = root.getJSONArray(id)
                    VocabCardFix(
                        keep = row.optInt(0, 1) == 1,
                        reading = row.optString(1),
                        lemma = row.optString(2),
                        lemmaReading = row.optString(3),
                        pos = row.optString(4),
                        meaning = row.optString(5),
                        note = row.optString(6),
                    )
                }
            }.getOrDefault(emptyMap())
            cards = parsed
            return parsed
        }
    }
}
