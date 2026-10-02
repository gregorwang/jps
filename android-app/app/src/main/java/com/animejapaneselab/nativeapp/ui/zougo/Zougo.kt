package com.animejapaneselab.nativeapp.ui.zougo

import android.content.Context
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

/** Quiz record of one word: right / wrong answers and the day of the last one. */
data class ZgRecord(val right: Int = 0, val wrong: Int = 0, val last: Long = -1L)

data class ZougoState(
    /** 課 finished in 自習 — only these go into 練習. */
    val learned: Set<String> = emptySet(),
    /** Matrix cells opened, `"<lesson>:<row>:<col>"`. */
    val seen: Set<String> = emptySet(),
    /** Per word. */
    val records: Map<String, ZgRecord> = emptyMap(),
    /** 自習: the 造語 教科書 is open (目次). */
    val bookOpen: Boolean = false,
    /** 自習: the 課 being studied. */
    val lesson: String? = null,
    /** 練習: the questions of the running practice, null when none. */
    val practice: List<ZgQuestion>? = null,
) {
    fun seenIn(lesson: ZgLesson): Int = lesson.matrix?.words?.count { (r, c, _) -> cellKey(lesson.id, r, c) in seen } ?: 0

    companion object {
        fun cellKey(lesson: String, row: Int, col: Int) = "$lesson:$row:$col"
    }
}

sealed interface ZgQuestion {
    val word: String

    /** 拼合台 again: two parts, how does the compound read. */
    data class Reading(val item: ZgFuse, val options: List<String>, val answer: Int) : ZgQuestion {
        override val word: String get() = item.word
    }

    /** A matrix word: what does it mean. */
    data class Meaning(val cell: ZgCell.Word, val options: List<String>, val answer: Int) : ZgQuestion {
        override val word: String get() = cell.word
    }
}

object ZougoRules {
    const val LessonQuizSize = 6
    const val PracticeSize = 10

    /** The 小テスト after a matrix 課: up to six of its words, meaning questions. */
    fun lessonQuiz(lesson: ZgLesson, seed: Long = System.nanoTime()): List<ZgQuestion> {
        val m = lesson.matrix ?: return emptyList()
        val rnd = Random(seed)
        val words = m.words.map { it.third }
        return words.shuffled(rnd).take(LessonQuizSize).map { meaning(it, words, rnd) }
    }

    /**
     * 練習: words from learned 課 only. The ones missed most (and never asked) first, then the ones
     * asked longest ago; options are reshuffled every time.
     */
    fun practice(book: ZgBookData, state: ZougoState, seed: Long = System.nanoTime()): List<ZgQuestion> {
        val rnd = Random(seed)
        val learned = book.lessons.filter { it.id in state.learned }
        val pool = learned.flatMap { lesson ->
            if (lesson.kind == ZgKind.Fuse) {
                lesson.fuse.map { item -> item.word to { reading(item, rnd) } }
            } else {
                val words = lesson.matrix?.words?.map { it.third }.orEmpty()
                words.map { w -> w.word to { meaning(w, words, rnd) } }
            }
        }.distinctBy { it.first }
        val ranked = pool.shuffled(rnd).sortedWith(
            compareBy<Pair<String, () -> ZgQuestion>>(
                { state.records[it.first]?.let { r -> if (r.right + r.wrong == 0) 0 else 1 - r.wrong.coerceAtMost(3) } ?: -1 },
                { state.records[it.first]?.last ?: -1L },
            ),
        )
        return ranked.take(PracticeSize).map { it.second() }.shuffled(rnd)
    }

    fun reading(item: ZgFuse, rnd: Random): ZgQuestion.Reading {
        val shuffled = item.options.shuffled(rnd)
        return ZgQuestion.Reading(item, shuffled, shuffled.indexOf(item.options[item.answer]))
    }

    fun meaning(word: ZgCell.Word, pool: List<ZgCell.Word>, rnd: Random): ZgQuestion.Meaning {
        val wrong = pool.filter { it.word != word.word && it.meaning != word.meaning }.shuffled(rnd).map { it.meaning }.distinct().take(3)
        val all = (wrong + word.meaning).shuffled(rnd)
        return ZgQuestion.Meaning(word, all, all.indexOf(word.meaning))
    }

    /** Where to continue in the book: the first 課 not learned, else none. */
    fun next(book: ZgBookData, state: ZougoState): ZgLesson? = book.lessons.firstOrNull { it.id !in state.learned }
}

object Zougo {
    private var store: LocalLabStore? = null
    private val _state = MutableStateFlow(ZougoState())
    val state: StateFlow<ZougoState> = _state.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        val s = LocalLabStore(context.applicationContext)
        store = s
        _state.value = decode(s.readZougo())
    }

    fun openBook() = _state.mutate { it.copy(bookOpen = true) }

    fun closeBook() = _state.mutate { it.copy(bookOpen = false, lesson = null) }

    fun start(lessonId: String) = _state.mutate { it.copy(bookOpen = true, lesson = lessonId) }

    fun exitLesson() = _state.mutate { it.copy(lesson = null) }

    @Synchronized
    fun markLearned(context: Context, lessonId: String) {
        init(context)
        val s = _state.value
        if (lessonId !in s.learned) write(s.copy(learned = s.learned + lessonId))
    }

    @Synchronized
    fun see(context: Context, lessonId: String, row: Int, col: Int) {
        init(context)
        val key = ZougoState.cellKey(lessonId, row, col)
        val s = _state.value
        if (key !in s.seen) write(s.copy(seen = s.seen + key))
    }

    /** One judged answer: the word's record, and today's 学習記録. */
    @Synchronized
    fun answer(context: Context, word: String, right: Boolean) {
        init(context)
        val s = _state.value
        val r = s.records[word] ?: ZgRecord()
        val today = LocalDate.now().toEpochDay()
        val next = if (right) r.copy(right = r.right + 1, last = today) else r.copy(wrong = r.wrong + 1, last = today)
        write(s.copy(records = s.records + (word to next)))
        StudyLog.record(context, 1, if (right) 1 else 0)
    }

    fun startPractice(context: Context): Boolean {
        init(context)
        val qs = ZougoRules.practice(ZougoBook.load(context), _state.value)
        if (qs.isEmpty()) return false
        _state.mutate { it.copy(practice = qs) }
        return true
    }

    fun endPractice() = _state.mutate { it.copy(practice = null) }

    private inline fun MutableStateFlow<ZougoState>.mutate(f: (ZougoState) -> ZougoState) {
        synchronized(this@Zougo) { value = f(value) }
    }

    private fun write(s: ZougoState) {
        _state.value = s
        store?.writeZougo(encode(s))
    }

    private fun encode(s: ZougoState): String = JSONObject()
        .put("learned", JSONArray(s.learned.toList()))
        .put("seen", JSONArray(s.seen.toList()))
        .put("rec", JSONObject().also { o -> s.records.forEach { (k, r) -> o.put(k, JSONArray(listOf(r.right, r.wrong, r.last))) } })
        .toString()

    private fun decode(raw: String?): ZougoState {
        if (raw.isNullOrBlank()) return ZougoState()
        return runCatching {
            val o = JSONObject(raw)
            fun set(name: String) = o.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }.orEmpty()
            val rec = o.optJSONObject("rec")
            ZougoState(
                learned = set("learned"),
                seen = set("seen"),
                records = rec?.keys()?.asSequence()?.associateWith { k ->
                    rec.getJSONArray(k).let { ZgRecord(it.optInt(0), it.optInt(1), it.optLong(2, -1L)) }
                }.orEmpty(),
            )
        }.getOrDefault(ZougoState())
    }
}
