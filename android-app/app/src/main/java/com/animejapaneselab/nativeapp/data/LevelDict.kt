package com.animejapaneselab.nativeapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * The whole dictionary grouped by JLPT level (辞書 with 按话浏览 off): `assets/dict_vocab.json` (3407
 * words, one per lemma) and `assets/dict_grammar.json` (1677 patterns, one per pattern with up to
 * eight episode lines). Same JSON shape as the Worker's rows, so the same parsers build the items;
 * bundled so the level view works offline. Built from the Supabase tables by
 * archive-content-sources/level-dict/build_assets.py (merge rules are in the header there).
 *
 * 高频补充 (`assets/dict_freq_vocab.json` 2116 words, `dict_freq_grammar.json` 342 patterns): JLPT N5–N2
 * entries that are frequent in anime (Jiten) but missing from the two works, kept apart from the works'
 * own entries; built by archive-content-sources/jlpt-freq (sources and licences in docs/assets-license.md).
 */
object LevelDict {
    /** Level key of proper nouns, plot terms and words with no level. */
    const val Outside = "级外"

    data class Loaded(
        val vocab: List<VocabItem>,
        val grammar: List<GrammarPoint>,
        val freqVocab: List<VocabItem> = emptyList(),
        val freqGrammar: List<GrammarPoint> = emptyList(),
    )

    @Volatile
    private var cached: Loaded? = null

    fun peek(): Loaded? = cached

    fun load(context: Context): Loaded {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val app = context.applicationContext
            fun items(name: String): JSONArray =
                JSONObject(app.assets.open(name).bufferedReader().use { it.readText() }).getJSONArray("items")

            val vocab = items("dict_vocab.json").let { a -> (0 until a.length()).map { vocabFromJson(a.getJSONObject(it), "dict") } }
            val grammar = items("dict_grammar.json").let { a -> (0 until a.length()).map { grammarFromJson(a.getJSONObject(it), "dict") } }
            val freqVocab = runCatching {
                items("dict_freq_vocab.json").let { a -> (0 until a.length()).map { vocabFromJson(a.getJSONObject(it), "freq") } }
            }.getOrDefault(emptyList())
            val freqGrammar = runCatching {
                items("dict_freq_grammar.json").let { a -> (0 until a.length()).map { grammarFromJson(a.getJSONObject(it), "freq") } }
            }.getOrDefault(emptyList())
            return Loaded(vocab, grammar, freqVocab, freqGrammar).also { cached = it }
        }
    }

    private const val PrefsName = "ajl-dict"
    private const val LevelKey = "level"

    /** The level chosen last time; the first visit opens N5. */
    fun lastLevel(context: Context): String =
        context.applicationContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).getString(LevelKey, null) ?: "N5"

    fun setLastLevel(context: Context, level: String) {
        context.applicationContext.getSharedPreferences(PrefsName, Context.MODE_PRIVATE).edit().putString(LevelKey, level).apply()
    }
}
