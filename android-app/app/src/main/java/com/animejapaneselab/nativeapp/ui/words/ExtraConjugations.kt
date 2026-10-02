package com.animejapaneselab.nativeapp.ui.words

import android.content.Context
import com.animejapaneselab.nativeapp.data.ConjugationTable
import com.animejapaneselab.nativeapp.data.Conjugator

/**
 * 辞書 words whose part of speech hides that they conjugate, typed from JMdict offline into
 * `assets/conj_extra.tsv` (surface \t kind, see [Conjugator.tableAs]):
 * - 名词 that take する (演奏 → 演奏する / 演奏した / 演奏しない), JMdict `vs`;
 * - 名词 also used as な形容词 (安全な / 安全じゃない), JMdict `adj-na`, hand-pruned;
 * - 连语 ending in a verb or adjective (足を引っ張る → 足を引っ張った), only the end moves.
 * Plain nouns (学校, 先生) don't conjugate, so they still get no table.
 */
object ExtraConjugations {
    private const val Asset = "conj_extra.tsv"

    @Volatile
    private var kinds: Map<String, String>? = null

    /** The word's own table, or the one its JMdict type gives. */
    fun tableFor(context: Context, surface: String, reading: String, partOfSpeech: String): ConjugationTable? {
        Conjugator.tableFor(surface, reading, partOfSpeech)?.let { return it }
        val kind = load(context)[surface.trim()] ?: return null
        val table = Conjugator.tableAs(surface, kind) ?: return null
        val pos = partOfSpeech.trim()
        val label = when {
            pos.startsWith("名") && kind == "suru" -> "サ变名词（＋する）"
            pos.startsWith("名") -> "名词 · 也作な形容词"
            kind == "suru" -> "サ变 · 连语"
            else -> "${table.typeLabel} · 连语"
        }
        return table.copy(typeLabel = label)
    }

    private fun load(context: Context): Map<String, String> {
        kinds?.let { return it }
        synchronized(this) {
            kinds?.let { return it }
            val parsed = runCatching {
                context.applicationContext.assets.open(Asset).bufferedReader().useLines { lines ->
                    lines.mapNotNull { line ->
                        val cols = line.split('\t')
                        if (cols.size == 2 && cols[0].isNotBlank()) cols[0].trim() to cols[1].trim() else null
                    }.toMap()
                }
            }.getOrDefault(emptyMap())
            kinds = parsed
            return parsed
        }
    }
}
