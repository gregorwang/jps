package com.animejapaneselab.nativeapp.ui.words

import android.content.Context
import com.animejapaneselab.nativeapp.data.LevelDict
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.ui.reading.Kana
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/*
 * 練習 → 単語: the 辞書 words of one JLPT level (原作 + 高频补充), tested, spaced.
 *
 * Each day: the words that are due first (test before showing anything), then up to 10 new words
 * (a card, then a question one card later). The question gets harder with the box:
 *   box 1      → Listen: hear the word, pick its meaning
 *   box 2      → Look:   see the word, pick its meaning
 *   box 3      → Look:   see the word, pick its reading (kana words: meaning)
 *   box 4, 5   → Line:   hear an anime line with the word blanked, pick the word (homophones are the decoys)
 * Right moves a box up (gaps 1 / 3 / 7 / 14 / 30 days); right at box 5 = 已掌握 (box 6, never due).
 * Wrong goes back to box 1 (tomorrow) and the word comes back once more at the end of the round.
 * Separate from 帳面's 単語 (Tango), which keeps its own boxes.
 */

/** A blank in [TangoLine.ja]: [cut] at [start], read [cutKana] ("" = the word is kana, show a gap). */
data class WpCloze(val start: Int, val cut: String, val cutKana: String)

data class WpWord(
    val id: String,
    val surface: String,
    val reading: String,
    val meaning: String,
    val pos: String,
    val line: TangoLine?,
    val cloze: WpCloze?,
) {
    val short: String get() = WordPracticeRules.shortMeaning(meaning)
    val hasKanji: Boolean get() = surface.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
    val romaji: String get() = Kana.romaji(reading)
}

data class WpProgress(val box: Int, val due: Long)

enum class WpKind { Listen, Look, LookReading, Line }

sealed interface WpStep {
    val word: WpWord

    data class Card(override val word: WpWord) : WpStep

    /** [retry] = the second go after a wrong answer: it never moves the box. */
    data class Quiz(
        override val word: WpWord,
        val kind: WpKind,
        val options: List<String>,
        val answer: Int,
        val fresh: Boolean,
        val retry: Boolean = false,
    ) : WpStep
}

data class WpSession(val level: String, val steps: List<WpStep>, val right: Int = 0, val asked: Int = 0, val newWords: Int = 0)

data class WpState(
    val progress: Map<String, WpProgress> = emptyMap(),
    val newDay: Long = -1L,
    val newCount: Int = 0,
    /** 再学 10 个: extra new words allowed today. */
    val extra: Int = 0,
) {
    fun newToday(day: Long): Int = if (newDay == day) newCount else 0
    fun extraToday(day: Long): Int = if (newDay == day) extra else 0
}

object WordPracticeRules {
    const val NewPerDay = 10
    const val Mastered = 6
    val Gaps = longArrayOf(1, 3, 7, 14, 30)

    /** The first sense, without its bracketed note: 「最后；最终（时间）」→「最后」. */
    fun shortMeaning(m: String): String {
        val first = m.split('；', ';', '，', ',', '、', '/', '／').map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
        return first.replace(Regex("[（(].*?[）)]"), "").trim().ifEmpty { first }
    }

    /** The level's words, 原作 before 高频补充, one per headword, without 斩 ones and empty meanings. */
    fun pool(
        dict: LevelDict.Loaded,
        lines: Map<String, TangoLine>,
        level: String,
        known: Set<String>,
        homoCut: (String) -> HomoWord?,
    ): List<WpWord> {
        val seen = HashSet<String>()
        return (dict.vocab + dict.freqVocab).asSequence()
            .filter { it.level == level && it.meaningZh.isNotBlank() && it.surface.isNotBlank() }
            .filter { it.surface.trim() !in known }
            .filter { seen.add(it.surface.trim()) }
            .map { word(it, lines[it.id], homoCut(it.surface.trim())) }
            .toList()
    }

    /** How many of the level's words are 斩. */
    fun cutCount(dict: LevelDict.Loaded, level: String, known: Set<String>): Int =
        (dict.vocab + dict.freqVocab).asSequence().filter { it.level == level }.map { it.surface.trim() }.distinct().count { it in known }

    private fun word(item: VocabItem, line: TangoLine?, homo: HomoWord?): WpWord {
        val surface = item.surface.trim()
        val reading = item.reading.trim().ifEmpty { surface }
        return WpWord(item.id, surface, reading, item.meaningZh.trim(), item.partOfSpeech, line, line?.let { cloze(surface, reading, it.ja, homo) })
    }

    /** Where the word sits in its line: the hand-checked 同音 cut first, then the whole word, then its stem (助け|る). */
    fun cloze(surface: String, reading: String, ja: String, homo: HomoWord?): WpCloze? {
        if (homo != null && homo.cut.isNotEmpty()) {
            val at = ja.indexOf(homo.cut)
            if (at >= 0) return WpCloze(at, homo.cut, homo.cutKana)
        }
        val kanji = surface.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
        ja.indexOf(surface).takeIf { it >= 0 }?.let { return WpCloze(it, surface, if (kanji) reading else "") }
        if (kanji && surface.length > 1 && reading.length > 1 && surface.last() == reading.last()) {
            val stem = surface.dropLast(1)
            if (stem.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }) {
                ja.indexOf(stem).takeIf { it >= 0 }?.let { return WpCloze(it, stem, reading.dropLast(1)) }
            }
        }
        return null
    }

    /** New words in the order they come: with a line first, the ones the shows say most first. */
    fun newOrder(pool: List<WpWord>, progress: Map<String, WpProgress>): List<WpWord> =
        pool.filter { it.id !in progress }.sortedWith(compareBy<WpWord> { if (it.line == null) 1 else 0 }.thenByDescending { it.line?.freq ?: 0 })

    fun due(pool: List<WpWord>, progress: Map<String, WpProgress>, today: Long): List<WpWord> =
        pool.filter { w -> progress[w.id]?.let { it.box in 1 until Mastered && it.due <= today } == true }
            .sortedBy { progress[it.id]?.due ?: 0L }

    fun newLeft(state: WpState, today: Long): Int =
        (NewPerDay + state.extraToday(today) - state.newToday(today)).coerceAtLeast(0)

    fun kindFor(box: Int, word: WpWord): WpKind = when {
        box <= 1 -> WpKind.Listen
        box == 2 -> WpKind.Look
        box == 3 -> if (word.hasKanji) WpKind.LookReading else WpKind.Look
        word.cloze != null -> WpKind.Line
        else -> if (word.hasKanji) WpKind.LookReading else WpKind.Look
    }

    /** Due ones first; then the new ones, each card followed by its question one card later. */
    fun session(level: String, pool: List<WpWord>, state: WpState, today: Long, seed: Long): WpSession {
        val rnd = Random(seed)
        val steps = mutableListOf<WpStep>()
        due(pool, state.progress, today).forEach { w ->
            steps += quiz(w, kindFor(state.progress[w.id]?.box ?: 1, w), pool, rnd, fresh = false)
        }
        val fresh = newOrder(pool, state.progress).take(newLeft(state, today))
        fresh.forEachIndexed { i, w ->
            steps += WpStep.Card(w)
            if (i > 0) steps += quiz(fresh[i - 1], WpKind.Listen, pool, rnd, fresh = true)
        }
        fresh.lastOrNull()?.let { steps += quiz(it, WpKind.Listen, pool, rnd, fresh = true) }
        return WpSession(level, steps)
    }

    fun quiz(w: WpWord, kind: WpKind, pool: List<WpWord>, rnd: Random, fresh: Boolean, retry: Boolean = false): WpStep.Quiz {
        val (right, decoys) = when (kind) {
            WpKind.Listen, WpKind.Look -> w.short to meaningDecoys(w, pool, rnd)
            WpKind.LookReading -> w.reading to readingDecoys(w, pool, rnd)
            WpKind.Line -> w.surface to wordDecoys(w, pool, rnd)
        }
        val options = (decoys.take(3) + right).shuffled(rnd)
        return WpStep.Quiz(w, kind, options, options.indexOf(right), fresh, retry)
    }

    private fun meaningDecoys(w: WpWord, pool: List<WpWord>, rnd: Random): List<String> {
        val target = w.short
        val samePos = pool.filter { it.pos == w.pos }.map { it.short }
        return (samePos.shuffled(rnd) + pool.map { it.short }.shuffled(rnd))
            .filter { it.isNotEmpty() && it != target && !it.contains(target) && !target.contains(it) }
            .distinct().take(3)
    }

    private fun readingDecoys(w: WpWord, pool: List<WpWord>, rnd: Random): List<String> {
        val near = pool.filter { it.reading != w.reading && kotlin.math.abs(it.reading.length - w.reading.length) <= 1 }
        // Same first kana first: さいご → さいしょ is a real decoy, くるま is not.
        val (first, rest) = near.partition { it.reading.firstOrNull() == w.reading.firstOrNull() }
        return (first.shuffled(rnd) + rest.shuffled(rnd)).map { it.reading }.distinct().take(3)
    }

    /** Homophones (not the same word in another kanji), then words sharing a kanji, then same kind of word. */
    private fun wordDecoys(w: WpWord, pool: List<WpWord>, rnd: Random): List<String> {
        val group = Homophones.groupOf(w.surface)
        val cluster = group?.words?.firstOrNull { it.surface == w.surface }?.cluster ?: 0
        val homo = group?.words.orEmpty().filter { it.surface != w.surface && (cluster == 0 || it.cluster != cluster) }.map { it.surface }
        val kanji = w.surface.filter { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }.toSet()
        val share = pool.filter { o -> o.surface != w.surface && o.surface.any { it in kanji } }.shuffled(rnd).map { it.surface }
        val kind = pool.filter { it.surface != w.surface && it.hasKanji == w.hasKanji && it.pos == w.pos }.shuffled(rnd).map { it.surface }
        val any = pool.filter { it.surface != w.surface && it.hasKanji == w.hasKanji }.shuffled(rnd).map { it.surface }
        return (homo + share + kind + any).distinct().take(3)
    }

    fun afterAnswer(p: WpProgress?, right: Boolean, today: Long): WpProgress {
        if (p == null || !right) return WpProgress(1, today + 1)
        val box = (p.box + 1).coerceAtMost(Mastered)
        return WpProgress(box, if (box >= Mastered) Long.MAX_VALUE else today + Gaps[box - 1])
    }
}

/** Process-wide 単語 practice: boxes (persisted) and the round being played (memory only). */
object WordPractice {
    private var store: LocalLabStore? = null
    private val _state = MutableStateFlow(WpState())
    val state: StateFlow<WpState> = _state.asStateFlow()
    private val _session = MutableStateFlow<WpSession?>(null)
    val session: StateFlow<WpSession?> = _session.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        val s = LocalLabStore(context.applicationContext)
        store = s
        _state.value = decode(s.readWordPractice())
    }

    fun start(session: WpSession) {
        if (session.steps.isNotEmpty()) _session.value = session
    }

    fun close() {
        _session.value = null
    }

    /** 再学 10 个 after today's are done. */
    @Synchronized
    fun moreToday(today: Long) {
        val s = _state.value
        write(s.copy(newDay = today, newCount = s.newToday(today), extra = s.extraToday(today) + WordPracticeRules.NewPerDay))
    }

    /** Grades [step]; a wrong first go comes back once at the end of the round, as a Listen question. */
    @Synchronized
    fun answer(step: WpStep.Quiz, right: Boolean, pool: List<WpWord>, today: Long) {
        val sess = _session.value ?: return
        var steps = sess.steps
        if (!step.retry) {
            val s = _state.value
            val moved = WordPracticeRules.afterAnswer(if (step.fresh) null else s.progress[step.word.id], right, today)
            write(
                s.copy(
                    progress = s.progress + (step.word.id to moved),
                    newDay = today,
                    newCount = s.newToday(today) + if (step.fresh) 1 else 0,
                    extra = s.extraToday(today),
                ),
            )
            if (!right) steps = steps + WordPracticeRules.quiz(step.word, WpKind.Listen, pool, Random(today + steps.size), fresh = false, retry = true)
        }
        _session.value = sess.copy(
            steps = steps,
            right = sess.right + if (right) 1 else 0,
            asked = sess.asked + 1,
            newWords = sess.newWords + if (step.fresh && !step.retry) 1 else 0,
        )
    }

    private fun write(next: WpState) {
        _state.value = next
        store?.writeWordPractice(encode(next))
    }

    fun encode(s: WpState): String {
        val p = JSONObject()
        s.progress.forEach { (id, v) -> p.put(id, JSONArray(listOf(v.box, v.due))) }
        return JSONObject().put("p", p).put("d", s.newDay).put("n", s.newCount).put("x", s.extra).toString()
    }

    fun decode(raw: String?): WpState = runCatching {
        val o = JSONObject(raw ?: return WpState())
        val p = o.optJSONObject("p")
        val progress = p?.keys()?.asSequence()?.associateWith { id ->
            val a = p.getJSONArray(id)
            WpProgress(a.optInt(0), a.optLong(1))
        }.orEmpty()
        WpState(progress, o.optLong("d", -1L), o.optInt("n"), o.optInt("x"))
    }.getOrDefault(WpState())
}
