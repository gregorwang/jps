package com.animejapaneselab.nativeapp.ui.kyoka

import android.content.Context
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.ui.katsuyou.KatsuyouBook
import com.animejapaneselab.nativeapp.ui.katsuyou.KyLesson
import com.animejapaneselab.nativeapp.ui.katsuyou.KyStep
import com.animejapaneselab.nativeapp.ui.katsuyou.KyTable
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/*
 * The 教科書 made from the later documents (第五巻 助詞, 第六巻 口語, 第七巻 類義). They play exactly like the
 * rebuilt 活用 books (課前の一眼 → steps → つづく) but are not 活用 points, so 已学 lives here, not in
 * ConjugationDrillViewModel. `assets/kyoka_books.json` is written by
 * archive-content-sources/kyoka/build_kyoka.py (see KYOKA_HANDOFF.md).
 */

data class KkSection(val no: String, val title: String, val zh: String)

data class KkLesson(val id: String, val number: Int, val section: Int, val title: String, val gloss: String, val play: KyLesson)

data class KkBook(
    val id: String,
    val volume: String,
    val title: String,
    val sub: String,
    val play: String,
    val sections: List<KkSection>,
    val lessons: List<KkLesson>,
    val table: KyTable?,
) {
    fun lesson(id: String): KkLesson? = lessons.firstOrNull { it.id == id }
}

data class KkData(val books: List<KkBook>) {
    fun book(id: String): KkBook? = books.firstOrNull { it.id == id }
}

object KyokaBooks {
    private const val Asset = "kyoka_books.json"
    @Volatile private var cached: KkData? = null

    fun load(context: Context): KkData {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: runCatching {
                parse(context.applicationContext.assets.open(Asset).bufferedReader().use { it.readText() })
            }.getOrElse { KkData(emptyList()) }.also { cached = it }
        }
    }

    internal fun parse(raw: String): KkData {
        val top = JSONObject(raw)
        val base = top.optString("audioBase")
        return KkData(top.getJSONArray("books").objects().map { b ->
            KkBook(
                id = b.getString("id"),
                volume = b.getString("volume"),
                title = b.getString("title"),
                sub = b.optString("sub"),
                play = b.optString("play"),
                sections = b.optJSONArray("sections")?.objects()?.map { KkSection(it.optString("no"), it.getString("title"), it.optString("zh")) }.orEmpty(),
                lessons = b.getJSONArray("lessons").objects().mapIndexed { i, o ->
                    KkLesson(
                        id = o.getString("point"),
                        number = i + 1,
                        section = o.optInt("section"),
                        title = o.getString("title"),
                        gloss = o.optString("gloss"),
                        play = KatsuyouBook.lessonOf(o, base),
                    )
                },
                table = b.optJSONObject("table")?.let { KatsuyouBook.tableOf(it, base) },
            )
        })
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}

data class KyokaState(
    /** 課 played through to つづく. */
    val learned: Set<String> = emptySet(),
    /** 自習: the open 教科書 (目次). */
    val book: String? = null,
    /** 自習: the 課 being played. */
    val lesson: String? = null,
    /** 自習: the book's まとめ is open. */
    val summary: Boolean = false,
    /** 練習: the practice being played (null = none), and which book it is over (null = all). */
    val practice: List<KyStep>? = null,
    val practiceBook: String? = null,
)

object KyokaRules {
    const val PracticeSize = 12

    /**
     * 練習: items drawn from the 課 learned in 自習 only, across [books]. Each picked item keeps the play
     * it came from (the step's title, the swipe's two sides), so it is asked exactly as in the 課; items
     * from the same step are asked together, the steps in random order. Whole-step games (翻牌, 叠积木,
     * 连线) are not drawn.
     */
    fun practice(books: List<KkBook>, learned: Set<String>, seed: Long = System.nanoTime()): List<KyStep> {
        val rnd = Random(seed)
        val steps = books.flatMap { b -> b.lessons.filter { it.id in learned }.flatMap { it.play.steps } }
        val pool = steps.indices.flatMap { s -> (0 until askable(steps[s])).map { s to it } }
        return pool.shuffled(rnd).take(PracticeSize)
            .groupBy({ it.first }, { it.second })
            .map { (s, items) -> subset(steps[s], items.shuffled(rnd)) }
            .shuffled(rnd)
    }

    /** How many items of [step] can be asked one by one. */
    fun askable(step: KyStep): Int = when (step) {
        is KyStep.Fuse, is KyStep.Back, is KyStep.Pick, is KyStep.Speed, is KyStep.Dial, is KyStep.Swipe, is KyStep.Spot -> step.count
        is KyStep.Flip, is KyStep.Stack, is KyStep.Connect -> 0
    }

    private fun subset(step: KyStep, idx: List<Int>): KyStep = when (step) {
        is KyStep.Fuse -> step.copy(items = idx.map(step.items::get))
        is KyStep.Back -> step.copy(items = idx.map(step.items::get))
        is KyStep.Pick -> step.copy(items = idx.map(step.items::get))
        is KyStep.Speed -> step.copy(items = idx.map(step.items::get))
        is KyStep.Dial -> step.copy(items = idx.map(step.items::get))
        is KyStep.Swipe -> step.copy(items = idx.map(step.items::get))
        is KyStep.Spot -> step.copy(items = idx.map(step.items::get))
        is KyStep.Flip, is KyStep.Stack, is KyStep.Connect -> step
    }
}

object Kyoka {
    private var store: LocalLabStore? = null
    private val _state = MutableStateFlow(KyokaState())
    val state: StateFlow<KyokaState> = _state.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        val s = LocalLabStore(context.applicationContext)
        store = s
        _state.value = KyokaState(learned = decode(s.readKyoka()))
    }

    fun openBook(id: String) = mutate { it.copy(book = id, lesson = null, summary = false) }

    fun closeBook() = mutate { it.copy(book = null, lesson = null, summary = false) }

    fun start(lessonId: String) = mutate { it.copy(lesson = lessonId, summary = false) }

    fun exitLesson() = mutate { it.copy(lesson = null) }

    fun openSummary() = mutate { it.copy(summary = true) }

    fun closeSummary() = mutate { it.copy(summary = false) }

    @Synchronized
    fun markLearned(context: Context, lessonId: String) {
        init(context)
        val s = _state.value
        if (lessonId in s.learned) return
        val next = s.copy(learned = s.learned + lessonId)
        _state.value = next
        store?.writeKyoka(JSONObject().put("learned", JSONArray(next.learned.toList())).toString())
    }

    /** 練習 over the learned 課 of one book ([bookId]) or of all of them (null); false = nothing learned yet. */
    fun startPractice(context: Context, bookId: String?): Boolean {
        init(context)
        val data = KyokaBooks.load(context)
        val books = if (bookId == null) data.books else listOfNotNull(data.book(bookId))
        val qs = KyokaRules.practice(books, _state.value.learned)
        if (qs.isEmpty()) return false
        mutate { it.copy(practice = qs, practiceBook = bookId) }
        return true
    }

    fun endPractice() = mutate { it.copy(practice = null, practiceBook = null) }

    fun answer(context: Context, right: Boolean) = StudyLog.record(context, 1, if (right) 1 else 0)

    private inline fun mutate(f: (KyokaState) -> KyokaState) {
        synchronized(this) { _state.value = f(_state.value) }
    }

    private fun decode(raw: String?): Set<String> = runCatching {
        JSONObject(raw.orEmpty()).optJSONArray("learned")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
    }.getOrNull().orEmpty()
}
