package com.animejapaneselab.nativeapp.ui.knowledge

import android.content.Context
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.ui.words.VocabCardFix
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/** What happened to one 知識 card on this phone. */
data class KnowMark(
    val seen: Int = 0,
    /** Epoch day it was last scrolled past; -1 = never. */
    val lastDay: Long = -1L,
    /** Epoch day of ♥ 掌握; -1 = not mastered. */
    val heartDay: Long = -1L,
    /** How many of the 30 / 90-day checks after ♥ it has passed. */
    val checks: Int = 0,
    /** Epoch day of 收藏 (important: keep bringing it back); -1 = not starred. */
    val starDay: Long = -1L,
) {
    val hearted: Boolean get() = heartDay >= 0
    val starred: Boolean get() = starDay >= 0
}

/** A word from 資料 (`vocab_cards.json`) shown as a 知識 card. */
data class VocabWord(val id: String, val head: String, val fix: VocabCardFix)

/*
 * Order of the endless feed, all on the phone (one user: nothing to learn from other people):
 * unread cards in document order, so a point's contrast / origin / quiz cards follow it; read ones
 * come back after 1, 3, 7, 14, 30 days; ♥ 掌握 takes a card out, except a check at 30 and 90 days
 * (its quiz when it has one) — a wrong answer there takes the ♥ back; 收藏 brings a card back every
 * day until it is un-starred. ♥ and 收藏 exclude each other.
 */
object KnowledgeRules {
    /** 帳面 → only the 收藏 cards / only words from 資料: pseudo deck ids of the feed. */
    const val StarDeck = "@star"
    const val VocabDeck = "@vocab"

    private val Gaps = longArrayOf(1, 3, 7, 14, 30)
    private val CheckAfter = longArrayOf(30, 90)

    fun reviewDue(m: KnowMark, today: Long): Boolean =
        !m.hearted && m.seen > 0 && today - m.lastDay >= Gaps[(m.seen - 1).coerceIn(0, Gaps.lastIndex)]

    fun starDue(m: KnowMark, today: Long): Boolean = m.starred && m.lastDay < today

    fun checkDue(m: KnowMark, today: Long): Boolean =
        m.hearted && m.checks < CheckAfter.size && today - m.heartDay >= CheckAfter[m.checks]

    /** The next [count] cards, none of them in [recent]; empty only when every card is ♥ or recent. */
    fun pick(cards: List<KnowledgeCard>, marks: Map<String, KnowMark>, today: Long, recent: Set<String>, count: Int): List<KnowledgeCard> {
        fun mark(c: KnowledgeCard) = marks[c.id] ?: KnowMark()
        val pool = cards.filter { it.id !in recent }
        val checks = ArrayDeque(
            cards.filter { checkDue(mark(it), today) }.mapNotNull { held ->
                pool.firstOrNull { it.testsId == held.id } ?: pool.firstOrNull { it.id == held.id }
            },
        )
        val unread = ArrayDeque(pool.filter { mark(it).seen == 0 && !mark(it).hearted })
        val stars = ArrayDeque(pool.filter { starDue(mark(it), today) }.sortedBy { mark(it).lastDay })
        val reviews = ArrayDeque(pool.filter { reviewDue(mark(it), today) }.sortedBy { mark(it).lastDay })
        val rest = ArrayDeque(pool.filter { !mark(it).hearted && mark(it).seen > 0 }.sortedWith(compareBy({ mark(it).lastDay }, { it.id })))
        val out = LinkedHashMap<String, KnowledgeCard>()
        var slot = 0
        while (out.size < count) {
            val next = when {
                slot == 1 && checks.isNotEmpty() -> checks.removeFirst()
                slot % 4 == 0 && stars.isNotEmpty() -> stars.removeFirst()
                slot % 3 == 2 && reviews.isNotEmpty() -> reviews.removeFirst()
                else -> unread.removeFirstOrNull() ?: reviews.removeFirstOrNull() ?: stars.removeFirstOrNull() ?: rest.removeFirstOrNull() ?: checks.removeFirstOrNull()
            } ?: break
            out.putIfAbsent(next.id, next)
            slot++
        }
        return out.values.toList()
    }

    /** The word: from the asset's heads, else the id's tail (`k-on-vocab-大丈夫`); blank when neither is Japanese. */
    fun headOf(vocabId: String, fix: VocabCardFix): String =
        fix.head.ifBlank { vocabId.substringAfter("-vocab-", "") }.trim().takeIf { h -> h.any { it.code in 0x3040..0x30FF || it.code in 0x4E00..0x9FFF } }.orEmpty()

    /** Words worth a card: checked, not a fragment, not obvious, not 斩. */
    fun vocabPool(cards: Map<String, VocabCardFix>, known: Set<String>): List<VocabWord> =
        cards.entries
            .map { (id, fix) -> VocabWord(id, headOf(id, fix), fix) }
            .filter { w -> w.fix.keep && !w.fix.easy && w.fix.meaning.isNotBlank() && w.head.isNotBlank() && w.head !in known }
            .distinctBy { it.head }
            .sortedBy { it.id }

    fun pickVocab(pool: List<VocabWord>, recent: Set<String>, count: Int, seed: Long): List<VocabWord> =
        pool.filter { it.id !in recent }.shuffled(Random(seed)).take(count)
}

object Knowledge {
    private var store: LocalLabStore? = null
    private val _marks = MutableStateFlow<Map<String, KnowMark>>(emptyMap())
    val marks: StateFlow<Map<String, KnowMark>> = _marks.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        val s = LocalLabStore(context.applicationContext)
        store = s
        _marks.value = decode(s.readKnowledgeMarks())
    }

    /** Scrolled past: counts as read; a ♥ card shown for its check counts as checked. */
    @Synchronized
    fun seen(context: Context, id: String, today: Long) {
        init(context)
        val m = _marks.value[id] ?: KnowMark()
        if (m.lastDay == today && !KnowledgeRules.checkDue(m, today)) return
        val checks = if (KnowledgeRules.checkDue(m, today)) m.checks + 1 else m.checks
        write(_marks.value + (id to m.copy(seen = m.seen + if (m.lastDay == today) 0 else 1, lastDay = today, checks = checks)))
    }

    @Synchronized
    fun heart(context: Context, id: String, on: Boolean, today: Long) {
        init(context)
        val m = _marks.value[id] ?: KnowMark()
        write(_marks.value + (id to m.copy(heartDay = if (on) today else -1L, checks = 0, starDay = if (on) -1L else m.starDay)))
    }

    /** 收藏: comes back every day until taken off; takes the ♥ away (a starred card is not 掌握). */
    @Synchronized
    fun star(context: Context, id: String, on: Boolean, today: Long) {
        init(context)
        val m = _marks.value[id] ?: KnowMark()
        write(_marks.value + (id to m.copy(starDay = if (on) today else -1L, heartDay = if (on) -1L else m.heartDay, checks = if (on) 0 else m.checks)))
    }

    /** Before 0.18 收藏 on a knowledge card went to the 收藏 notebook: take those over as stars. */
    @Synchronized
    fun adoptStars(context: Context, ids: List<String>, today: Long) {
        init(context)
        if (ids.isEmpty()) return
        write(_marks.value + ids.associateWith { id -> (_marks.value[id] ?: KnowMark()).copy(starDay = today, heartDay = -1L, checks = 0) })
    }

    /** A quiz answer; returns true when it took the ♥ away from the card it checks. */
    @Synchronized
    fun answer(context: Context, quiz: KnowQuiz, right: Boolean, today: Long): Boolean {
        init(context)
        val tested = _marks.value[quiz.tests] ?: return false
        if (!tested.hearted) return false
        return if (right) {
            if (KnowledgeRules.checkDue(tested, today)) write(_marks.value + (quiz.tests to tested.copy(checks = tested.checks + 1)))
            false
        } else {
            write(_marks.value + (quiz.tests to tested.copy(heartDay = -1L, checks = 0, lastDay = today)))
            true
        }
    }

    private fun write(next: Map<String, KnowMark>) {
        _marks.value = next
        store?.writeKnowledgeMarks(encode(next))
    }

    fun encode(marks: Map<String, KnowMark>): String {
        val o = JSONObject()
        marks.forEach { (id, m) -> o.put(id, JSONArray(listOf(m.seen, m.lastDay, m.heartDay, m.checks, m.starDay))) }
        return o.toString()
    }

    fun decode(raw: String?): Map<String, KnowMark> = runCatching {
        val o = JSONObject(raw ?: return emptyMap())
        o.keys().asSequence().associateWith { id ->
            val a = o.getJSONArray(id)
            KnowMark(a.optInt(0), a.optLong(1, -1L), a.optLong(2, -1L), a.optInt(3), a.optLong(4, -1L))
        }
    }.getOrDefault(emptyMap())
}
