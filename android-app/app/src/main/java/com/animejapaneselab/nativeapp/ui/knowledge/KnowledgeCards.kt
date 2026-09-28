package com.animejapaneselab.nativeapp.ui.knowledge

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/*
 * 知識 cards: one knowledge point per card, read straight off the card (no sheets, no jumps).
 * Written offline from the reading documents the user collects (`assets/knowledge_cards.json`,
 * format and checks in `archive-content-sources/knowledge-cards/`), one deck per document.
 */

enum class KnowKind(val id: String, val label: String) {
    Pattern("pattern", "句型"),
    Contrast("contrast", "对比"),
    Trap("trap", "陷阱"),
    Origin("origin", "为什么"),
    Map("map", "总览"),
    Quiz("quiz", "自测"),
}

data class KnowPart(val w: String, val l1: String, val l2: String, val hl: Boolean)

data class KnowExample(val ja: String, val target: String, val romaji: String, val zh: String) {
    val targetRange: IntRange?
        get() = ja.indexOf(target).takeIf { target.isNotEmpty() && it >= 0 }?.let { it until it + target.length }
}

data class KnowRow(val label: String, val a: String, val b: String)

data class KnowStep(val head: String, val text: String)

data class KnowNote(val h: String, val b: String)

data class KnowLimit(val mark: String, val ja: String, val why: String)

data class KnowGlyph(val text: String, val reading: String, val meaning: String)

data class KnowQuiz(
    val before: String,
    val after: String,
    val zh: String,
    val options: List<String>,
    val answer: Int,
    val why: String,
    /** Id of the card this question checks: a wrong answer takes that card's ♥ away. */
    val tests: String,
)

data class KnowledgeCard(
    val id: String,
    val deckId: String,
    val deckTitle: String,
    val kind: KnowKind,
    val topic: String,
    val en: String,
    val title: String,
    val parts: List<KnowPart> = emptyList(),
    val examples: List<KnowExample> = emptyList(),
    val rule: String = "",
    val limit: KnowLimit? = null,
    /** Contrast: the two sides; rows compare them. Map: rows are label / form / example. */
    val a: String = "",
    val b: String = "",
    val rows: List<KnowRow> = emptyList(),
    val glyph: KnowGlyph? = null,
    val steps: List<KnowStep> = emptyList(),
    val quiz: KnowQuiz? = null,
    val notes: List<KnowNote> = emptyList(),
)

data class KnowledgeDeck(val id: String, val title: String, val cards: List<KnowledgeCard>)

object KnowledgeCards {
    private const val Asset = "knowledge_cards.json"

    @Volatile
    private var decks: List<KnowledgeDeck>? = null

    @Volatile
    private var byId: Map<String, KnowledgeCard> = emptyMap()

    fun decks(context: Context): List<KnowledgeDeck> {
        decks?.let { return it }
        synchronized(this) {
            decks?.let { return it }
            val parsed = runCatching {
                parse(context.applicationContext.assets.open(Asset).bufferedReader().use { it.readText() })
            }.getOrDefault(emptyList())
            byId = parsed.flatMap { it.cards }.associateBy { it.id }
            decks = parsed
            return parsed
        }
    }

    fun all(context: Context): List<KnowledgeCard> = decks(context).flatMap { it.cards }

    fun get(context: Context, id: String): KnowledgeCard? {
        decks(context)
        return byId[id]
    }

    fun parse(json: String): List<KnowledgeDeck> {
        val root = JSONObject(json)
        return root.optJSONArray("decks").objects().mapNotNull { d ->
            val deckId = d.optString("id").ifBlank { return@mapNotNull null }
            val title = d.optString("title")
            val cards = d.optJSONArray("cards").objects().mapNotNull { card(it, deckId, title) }
            KnowledgeDeck(deckId, title, cards).takeIf { cards.isNotEmpty() }
        }
    }

    private fun card(o: JSONObject, deckId: String, deckTitle: String): KnowledgeCard? {
        val id = o.optString("id").ifBlank { return null }
        val kind = KnowKind.entries.firstOrNull { it.id == o.optString("kind") } ?: return null
        val quiz = o.optJSONObject("quiz")?.let { q ->
            val options = q.optJSONArray("options").strings()
            val answer = q.optInt("answer", -1)
            if (answer !in options.indices) return null
            KnowQuiz(q.optString("before"), q.optString("after"), q.optString("zh"), options, answer, q.optString("why"), q.optString("tests"))
        }
        if (kind == KnowKind.Quiz && quiz == null) return null
        return KnowledgeCard(
            id = id,
            deckId = deckId,
            deckTitle = deckTitle,
            kind = kind,
            topic = o.optString("topic"),
            en = o.optString("en"),
            title = o.optString("title"),
            parts = o.optJSONArray("parts").objects().map { KnowPart(it.optString("w"), it.optString("l1"), it.optString("l2"), it.optBoolean("hl")) },
            examples = o.optJSONArray("examples").objects().map { KnowExample(it.optString("ja"), it.optString("target"), it.optString("romaji"), it.optString("zh")) },
            rule = o.optString("rule"),
            limit = o.optJSONObject("limit")?.let { KnowLimit(it.optString("mark", "✕"), it.optString("ja"), it.optString("why")) },
            a = o.optString("a"),
            b = o.optString("b"),
            rows = o.optJSONArray("rows").objects().map { KnowRow(it.optString("label"), it.optString("a"), it.optString("b")) },
            glyph = o.optJSONObject("glyph")?.let { KnowGlyph(it.optString("text"), it.optString("reading"), it.optString("meaning")) },
            steps = o.optJSONArray("steps").objects().map { KnowStep(it.optString("head"), it.optString("text")) },
            quiz = quiz,
            notes = o.optJSONArray("notes").objects().map { KnowNote(it.optString("h"), it.optString("b")) },
        )
    }

    private fun JSONArray?.objects(): List<JSONObject> = this?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }.orEmpty()

    private fun JSONArray?.strings(): List<String> = this?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
}
