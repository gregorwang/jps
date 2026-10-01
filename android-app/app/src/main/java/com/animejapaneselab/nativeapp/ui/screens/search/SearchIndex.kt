package com.animejapaneselab.nativeapp.ui.screens.search

import com.animejapaneselab.nativeapp.data.GrammarPoint
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeCard
import com.animejapaneselab.nativeapp.ui.screens.library.DictTarget
import com.animejapaneselab.nativeapp.ui.screens.library.Gojuon
import org.json.JSONArray
import org.json.JSONObject

/** What a search box looks in: 今日 everything, 辞書 its entries, 原作 the lines, 知識 the cards. */
enum class SearchScope(val title: String, val placeholder: String) {
    All("全局搜索", "搜单词、语法、知识点，或描述一个场景"),
    Dict("辞書内搜索", "搜单词、语法 · 假名、罗马音、中文都行"),
    Scenes("原作台词搜索", "描述一个场景，或输入台词"),
    Knowledge("知识点搜索", "搜知识点 · 如 ように、連用形"),
    ;

    val dict: Boolean get() = this == All || this == Dict
    val know: Boolean get() = this == All || this == Knowledge
    val scenes: Boolean get() = this == All || this == Scenes
}

internal class Indexed<T>(val value: T, val primary: List<String>, val secondary: List<String>)

/** Lower-case, katakana folded to hiragana: こんにちは matches コンニチハ, Arigatou matches arigatou. */
internal fun fold(text: String): String = buildString(text.length) {
    text.lowercase().forEach { append(Gojuon.toHiragana(it)) }
}

/**
 * The whole dictionary (every level) plus the words of the episode on screen, one entry per
 * headword. Primary fields are the headword / reading / pattern; the Chinese gloss is secondary.
 */
internal fun indexDict(vocab: List<VocabItem>, grammar: List<GrammarPoint>): List<Indexed<DictTarget>> {
    val seen = HashSet<String>()
    val words = vocab.filter { seen.add("v|${it.surface.trim()}|${it.reading.trim()}") }.map { v ->
        Indexed<DictTarget>(
            DictTarget.Word(v),
            primary = listOf(v.surface, v.reading, v.romanization).filter { it.isNotBlank() }.map(::fold),
            secondary = listOf(fold(v.meaningZh)),
        )
    }
    val patterns = grammar.filter { seen.add("g|${it.pattern.trim()}") }.map { g ->
        Indexed<DictTarget>(
            DictTarget.Pattern(g),
            primary = listOf(fold(g.pattern), fold(g.pattern.trim('〜', '～'))),
            secondary = listOf(fold(g.titleZh)),
        )
    }
    return words + patterns
}

/** Knowledge cards: title / topic first, then every text on the card (examples, rows, notes). */
internal fun indexKnowledge(cards: List<KnowledgeCard>): List<Indexed<KnowledgeCard>> {
    val seen = HashSet<String>()
    return cards.filter { seen.add(it.id) }.map { card ->
        val texts = buildList {
            addAll(listOf(card.rule, card.a, card.b))
            card.examples.forEach { add(it.ja); add(it.zh) }
            card.rows.forEach { add(it.label); add(it.a); add(it.b) }
            card.notes.forEach { add(it.h); add(it.b) }
            card.parts.forEach { add(it.w) }
            if (card.v2) collectStrings(card.data, this)
        }
        Indexed(
            card,
            primary = listOf(card.title, card.topic).filter { it.isNotBlank() }.map(::fold),
            secondary = texts.filter { it.isNotBlank() }.map(::fold),
        )
    }
}

private val SkipKeys = setOf("id", "kind", "tests", "deck", "audio")

private fun collectStrings(node: Any?, out: MutableList<String>) {
    when (node) {
        is JSONObject -> node.keys().forEach { k -> if (k !in SkipKeys) collectStrings(node.opt(k), out) }
        is JSONArray -> for (i in 0 until node.length()) collectStrings(node.opt(i), out)
        is String -> out += node.replace("【", "").replace("】", "")
    }
}

/** Best first: exact headword, headword prefix, headword substring, then gloss / body matches. */
internal fun <T> rank(index: List<Indexed<T>>, query: String, limit: Int): List<T> {
    val q = fold(query.trim())
    if (q.isEmpty()) return emptyList()
    return index.asSequence()
        .mapNotNull { e ->
            val score = when {
                e.primary.any { it == q } -> 0
                e.primary.any { it.startsWith(q) } -> 1
                e.primary.any { it.contains(q) } -> 2
                e.secondary.any { it == q } -> 3
                e.secondary.any { it.contains(q) } -> 4
                else -> return@mapNotNull null
            }
            score to e
        }
        .sortedWith(compareBy({ it.first }, { it.second.primary.firstOrNull()?.length ?: 0 }))
        .take(limit)
        .map { it.second.value }
        .toList()
}
