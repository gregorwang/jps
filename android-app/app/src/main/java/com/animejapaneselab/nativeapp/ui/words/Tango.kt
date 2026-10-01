package com.animejapaneselab.nativeapp.ui.words

import android.content.Context
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeCard
import com.animejapaneselab.nativeapp.ui.knowledge.VocabWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/*
 * 単語 tab (0.19): words come five at a time. Think first, then look (front / back of the card),
 * and at the end of the group one quiz — hear the line, pick the meaning. The quiz result moves
 * the word along its Leitner box (1 / 3 / 7 / 14 / 30 days); a wrong answer sends it back to box 1.
 * ♥ 掌握 = 斩 (same data as the 辞書 already-cut list), so a known word leaves for good.
 */

/** One anime line that contains a word, picked offline (`vocab_lines.json`, script: vocab-cards-v1/build_lines.py). */
data class TangoLine(val ja: String, val audioUrl: String, val freq: Int)

data class TangoProgress(val box: Int = 0, val due: Long = 0L, val last: Long = -1L)

/** The group being worked on: [results] holds the quiz answers so far (true = right). */
data class TangoGroup(val ids: List<String>, val results: Map<String, Boolean> = emptyMap()) {
    val finished: Boolean get() = ids.isNotEmpty() && results.size >= ids.size
}

data class TangoState(
    val progress: Map<String, TangoProgress> = emptyMap(),
    val group: TangoGroup? = null,
    /** Words quizzed on [todayDay] (for the header). */
    val todayCount: Int = 0,
    val todayDay: Long = -1L,
) {
    val learnedCount: Int get() = progress.values.count { it.box >= TangoRules.LearnedBox }
    val learningCount: Int get() = progress.values.count { it.box in 1 until TangoRules.LearnedBox }
    fun today(day: Long): Int = if (todayDay == day) todayCount else 0
}

object TangoRules {
    const val GroupSize = 5
    const val LearnedBox = 3
    private val Gaps = longArrayOf(1, 3, 7, 14, 30)
    private const val MaxDueInGroup = 3

    /** Words worth a card that also have a line from the anime to hang them on. */
    fun pool(words: List<VocabWord>, lines: Map<String, TangoLine>): List<VocabWord> = words.filter { it.id in lines }

    fun nextDue(box: Int, today: Long): Long = today + Gaps[(box - 1).coerceIn(0, Gaps.lastIndex)]

    fun afterAnswer(p: TangoProgress, right: Boolean, today: Long): TangoProgress {
        val box = if (right) (p.box + 1).coerceAtMost(Gaps.size) else 1
        return TangoProgress(box, nextDue(box, today), today)
    }

    /**
     * Order for words never seen: the ones in a 知識 card first, then the ones in a lesson the user
     * has finished, then the rest — inside a tier, words with the original voice, then the most
     * often heard.
     */
    fun rank(pool: List<VocabWord>, lines: Map<String, TangoLine>, knowText: String, lessonText: String): List<VocabWord> =
        pool.sortedWith(
            compareBy<VocabWord>(
                { if (it.head in knowText) 0 else if (it.head in lessonText) 1 else 2 },
                { if (lines[it.id]?.audioUrl.isNullOrEmpty()) 1 else 0 },
                { -(lines[it.id]?.freq ?: 0) },
                { it.id },
            ),
        )

    /** Next five: up to three that are due for review, then new words, then more reviews if new ones run out. */
    fun nextGroup(ranked: List<VocabWord>, progress: Map<String, TangoProgress>, today: Long): List<String> {
        val due = ranked.filter { w -> progress[w.id]?.let { it.due <= today } == true }.sortedBy { progress[it.id]!!.due }
        val fresh = ranked.filter { it.id !in progress }
        val out = LinkedHashSet<String>()
        due.take(MaxDueInGroup).forEach { out.add(it.id) }
        fresh.forEach { if (out.size < GroupSize) out.add(it.id) }
        due.forEach { if (out.size < GroupSize) out.add(it.id) }
        return out.toList()
    }

    /** The meaning question: four options, the right one always among them, wrong ones from the same part of speech first. */
    fun options(word: VocabWord, pool: List<VocabWord>): Pair<List<String>, Int> {
        val right = word.fix.meaning
        val rnd = Random(word.id.hashCode().toLong())
        val others = pool.filter { it.id != word.id && it.fix.meaning.isNotBlank() && it.fix.meaning != right }
        val same = others.filter { it.fix.pos == word.fix.pos }.shuffled(rnd)
        val rest = others.filter { it.fix.pos != word.fix.pos }.shuffled(rnd)
        val wrong = (same + rest).map { it.fix.meaning }.distinct().take(3)
        val all = (wrong + right).shuffled(rnd)
        return all to all.indexOf(right)
    }

    private val Quoted = Regex("「([^」]{2,14})」")

    /** 「布石を打つ」-style phrases the checked note already gives; only ones that contain the word. */
    fun collocations(word: VocabWord): List<String> {
        val forms = listOf(word.head, word.fix.lemma).filter { it.isNotBlank() }
        return Quoted.findAll(word.fix.note).map { it.groupValues[1] }
            .filter { q -> forms.any { it in q } && q != word.head }
            .distinct().take(3).toList()
    }

    /** Every string a 知識 card carries, for "does this word appear in one". */
    fun textOf(cards: List<KnowledgeCard>): String = buildString {
        cards.forEach { c ->
            append(c.title).append('\n')
            c.examples.forEach { append(it.ja).append('\n') }
            append(c.data.toString()).append('\n')
        }
    }
}

object Tango {
    private var store: LocalLabStore? = null
    private val _state = MutableStateFlow(TangoState())
    val state: StateFlow<TangoState> = _state.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        val s = LocalLabStore(context.applicationContext)
        store = s
        _state.value = decode(s.readTango())
    }

    /** Makes sure there is a group to work on: the unfinished one stays; a finished one waits for 次の 5 個. */
    @Synchronized
    fun ensureGroup(context: Context, ranked: List<VocabWord>, today: Long) {
        init(context)
        val s = _state.value
        if (s.group != null) return
        val ids = TangoRules.nextGroup(ranked, s.progress, today)
        if (ids.isNotEmpty()) write(s.copy(group = TangoGroup(ids)))
    }

    /** 次の 5 個. */
    @Synchronized
    fun nextGroup(context: Context, ranked: List<VocabWord>, today: Long) {
        init(context)
        val s = _state.value
        val ids = TangoRules.nextGroup(ranked, s.progress, today)
        write(s.copy(group = ids.takeIf { it.isNotEmpty() }?.let { TangoGroup(it) }))
    }

    /** A quiz answer: moves the word along its box and counts it for today. */
    @Synchronized
    fun answer(context: Context, id: String, right: Boolean, today: Long) {
        init(context)
        val s = _state.value
        val g = s.group ?: return
        if (id in g.results) return
        val moved = TangoRules.afterAnswer(s.progress[id] ?: TangoProgress(), right, today)
        write(
            s.copy(
                progress = s.progress + (id to moved),
                group = g.copy(results = g.results + (id to right)),
                todayCount = s.today(today) + 1,
                todayDay = today,
            ),
        )
    }

    private fun write(next: TangoState) {
        _state.value = next
        store?.writeTango(encode(next))
    }

    fun encode(s: TangoState): String {
        val o = JSONObject()
        val p = JSONObject()
        s.progress.forEach { (id, v) -> p.put(id, JSONArray(listOf(v.box, v.due, v.last))) }
        o.put("p", p)
        s.group?.let { g ->
            val r = JSONObject()
            g.results.forEach { (id, ok) -> r.put(id, if (ok) 1 else 0) }
            o.put("g", JSONObject().put("ids", JSONArray(g.ids)).put("r", r))
        }
        o.put("n", s.todayCount).put("d", s.todayDay)
        return o.toString()
    }

    fun decode(raw: String?): TangoState = runCatching {
        val o = JSONObject(raw ?: return TangoState())
        val p = o.optJSONObject("p")
        val progress = p?.keys()?.asSequence()?.associateWith { id ->
            val a = p.getJSONArray(id)
            TangoProgress(a.optInt(0), a.optLong(1), a.optLong(2, -1L))
        }.orEmpty()
        val group = o.optJSONObject("g")?.let { g ->
            val ids = g.getJSONArray("ids").let { a -> (0 until a.length()).map { a.getString(it) } }
            val r = g.optJSONObject("r")
            TangoGroup(ids, r?.keys()?.asSequence()?.associateWith { r.optInt(it) == 1 }.orEmpty())
        }
        TangoState(progress, group, o.optInt("n"), o.optLong("d", -1L))
    }.getOrDefault(TangoState())
}

/** `vocab_lines.json`: id → one line of the anime (+ its original voice when the show has it). */
object TangoLines {
    private const val Asset = "vocab_lines.json"

    @Volatile
    private var lines: Map<String, TangoLine>? = null

    /** Already parsed (the app preloads it at startup), or null. */
    fun peek(): Map<String, TangoLine>? = lines

    fun load(context: Context): Map<String, TangoLine> {
        lines?.let { return it }
        synchronized(this) {
            lines?.let { return it }
            val parsed = runCatching {
                val top = JSONObject(context.applicationContext.assets.open(Asset).bufferedReader().use { it.readText() })
                val base = top.optString("audioBase")
                val root = top.getJSONObject("lines")
                root.keys().asSequence().associateWith { id ->
                    val a = root.getJSONArray(id)
                    val path = a.optString(1)
                    TangoLine(a.getString(0), if (path.isEmpty() || path.startsWith("http")) path else base + path, a.optInt(2))
                }
            }.getOrDefault(emptyMap())
            lines = parsed
            return parsed
        }
    }
}
