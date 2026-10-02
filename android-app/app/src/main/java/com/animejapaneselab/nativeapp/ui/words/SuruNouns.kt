package com.animejapaneselab.nativeapp.ui.words

import android.content.Context
import com.animejapaneselab.nativeapp.data.ConjugationTable
import com.animejapaneselab.nativeapp.data.Conjugator

/**
 * 名词 in the 辞書 that take する (演奏 → 演奏する / 演奏した / 演奏しない). The list is the dictionary's
 * nouns that JMdict tags `vs`, built offline into `assets/suru_nouns.txt`. Plain nouns (学校, 先生)
 * don't conjugate, so they still get no 活用 table.
 */
object SuruNouns {
    private const val Asset = "suru_nouns.txt"
    const val TypeLabel = "サ变名词（＋する）"

    @Volatile
    private var words: Set<String>? = null

    fun contains(context: Context, surface: String): Boolean = load(context).contains(surface.trim())

    /** The word's own table, or the する table when it is a noun that takes する. */
    fun tableFor(context: Context, surface: String, reading: String, partOfSpeech: String): ConjugationTable? {
        Conjugator.tableFor(surface, reading, partOfSpeech)?.let { return it }
        val pos = partOfSpeech.trim()
        if (!pos.startsWith("名") || !contains(context, surface)) return null
        return Conjugator.tableFor(surface, reading, "サ変")?.copy(typeLabel = TypeLabel)
    }

    private fun load(context: Context): Set<String> {
        words?.let { return it }
        synchronized(this) {
            words?.let { return it }
            val parsed = runCatching {
                context.applicationContext.assets.open(Asset).bufferedReader().useLines { lines ->
                    lines.map { it.trim() }.filter { it.isNotEmpty() }.toHashSet()
                }
            }.getOrDefault(emptySet())
            words = parsed
            return parsed
        }
    }
}
