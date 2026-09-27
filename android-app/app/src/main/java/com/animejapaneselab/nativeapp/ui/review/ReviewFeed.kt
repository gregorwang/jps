package com.animejapaneselab.nativeapp.ui.review

import android.content.Context
import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.NotebookEntry
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillState
import com.animejapaneselab.nativeapp.ui.notebook.Notebook
import com.animejapaneselab.nativeapp.ui.screens.jishu.splitTitle
import com.animejapaneselab.nativeapp.ui.screens.review.ReviewRules
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import com.animejapaneselab.nativeapp.ui.words.KnownWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/*
 * 復習 刷卡流: every due thing — 活用 lines of learned 課, 栞 cards, local mistakes (merged with
 * their server review task) — becomes one full-screen card in one vertical feed. A verdict on a
 * card is written back to the scheduler it came from (drill Leitner, 栞 box, 错题本, server progress).
 */

enum class FeedKind { Conj, Word, Listen, Mistake, Weak, End }

enum class FeedSource(val label: String) { Conj("活用"), Shiori("栞"), Mistake("错题") }

/** 上划 = [Remembered], 再来 = [Again], 双击盖章 = [Mastered]. */
enum class Verdict { Remembered, Again, Mastered }

data class FeedCard(
    /** `conj:<itemId>`, `shiori:<notebookKey>`, `mistake:<itemId>`, `weak:<name>`, `end`; a requeued copy adds `#n`. */
    val key: String,
    val kind: FeedKind,
    val eyebrow: String = "",
    val dueLabel: String? = null,
    val line: ConjugationDrillItem? = null,
    val entry: NotebookEntry? = null,
    val mistake: MistakeRecord? = null,
    val task: ProgressItem? = null,
    val weak: ReviewRules.WeakSpot? = null,
) {
    val source: FeedSource?
        get() = when (kind) {
            FeedKind.Conj -> FeedSource.Conj
            FeedKind.Word, FeedKind.Listen -> FeedSource.Shiori
            FeedKind.Mistake -> FeedSource.Mistake
            else -> null
        }

    val isKnowledge: Boolean get() = kind != FeedKind.Weak && kind != FeedKind.End
}

/** Everything the feed is built from; read by the screen from the existing holders. */
class FeedSources(
    val drill: ConjugationDrillState,
    val notebook: List<NotebookEntry>,
    val mistakes: List<MistakeRecord>,
    val reviewTasks: List<ProgressItem>,
    val progressItems: List<ProgressItem>,
    val known: Set<String>,
    val mistakeDue: Map<String, Long>,
)

/** Where verdicts go outside the feed: the drill ViewModel and the main ViewModel. */
class ReviewSinks(
    val gradeLine: (itemId: String, remembered: Boolean, mastered: Boolean) -> Unit,
    val markMistakeReviewed: (itemId: String) -> Unit,
    val gradeTask: (task: ProgressItem, remembered: Boolean) -> Unit,
)

data class FeedSession(
    val day: Long,
    val keys: List<String>,
    val index: Int = 0,
    val remembered: Int = 0,
    val again: Int = 0,
    val mastered: Int = 0,
    /** Exact keys (with `#n`) already graded, so scrolling back and forth never counts twice. */
    val graded: Set<String> = emptySet(),
    val filter: FeedSource? = null,
    /** おかわり round: cards not due; a remembered verdict leaves their schedule alone. */
    val extra: Boolean = false,
    val finished: Boolean = false,
) {
    val knowledgeKeys: List<String> get() = keys.filter(FeedRules::isKnowledgeKey)

    /** Cards of today's round not graded yet (for 今日 三限). */
    val remaining: Int get() = keys.count { FeedRules.isKnowledgeKey(it) && it !in graded }

    /** 1-based position among knowledge cards (the one on screen or the next one). */
    val position: Int get() = keys.take(index + 1).count(FeedRules::isKnowledgeKey).coerceAtLeast(1)
}

// ---------------------------------------------------------------------------------- rules

object FeedRules {
    const val MaxCards = 40
    const val WeakEvery = 8
    const val AgainGap = 4
    const val ExtraSize = 10
    const val EndKey = "end"

    fun baseKey(key: String): String = key.substringBefore('#')

    fun isKnowledgeKey(key: String): Boolean = key != EndKey && !key.startsWith("weak:")

    fun dueLabel(dueDay: Long, today: Long): String = if (dueDay < today) "逾期 ${today - dueDay} 天" else "今天"

    fun conjCard(item: ConjugationDrillItem, drill: ConjugationDrillState, dueLabel: String?): FeedCard {
        val (jp, _) = splitTitle(drill.titleOf(item.pointId))
        val number = drill.lessonNumber(item.pointId)
        return FeedCard(
            key = "conj:${item.id}",
            kind = FeedKind.Conj,
            eyebrow = listOfNotNull("活用", number.takeIf { it > 0 }?.let { "第 $it 課 $jp" } ?: jp.takeIf { it.isNotBlank() }).joinToString(" · "),
            dueLabel = dueLabel,
            line = item,
        )
    }

    fun shioriCard(entry: NotebookEntry, dueLabel: String?): FeedCard = FeedCard(
        key = "shiori:${entry.key}",
        kind = if (entry.kind == NotebookKind.Line) FeedKind.Listen else FeedKind.Word,
        eyebrow = when (entry.kind) {
            NotebookKind.Vocab -> "栞 · 词汇"
            NotebookKind.Grammar -> "栞 · 文型"
            NotebookKind.Line -> "栞 · 听辨"
        },
        dueLabel = dueLabel,
        entry = entry,
    )

    fun mistakeCard(mistake: MistakeRecord, task: ProgressItem?, dueLabel: String?): FeedCard = FeedCard(
        key = "mistake:${mistake.itemId}",
        kind = FeedKind.Mistake,
        eyebrow = "错题 · " + ReviewRules.cardMeta(mistake.typeLabel, 0, mistake.attempts),
        dueLabel = dueLabel,
        mistake = mistake,
        task = task,
    )

    fun taskFor(mistake: MistakeRecord, tasks: List<ProgressItem>): ProgressItem? =
        tasks.firstOrNull { it.itemId.isNotBlank() && it.itemId == mistake.itemId }

    /** Server tasks with no local mistake behind them: only a label, so they stay in 帳面. */
    fun remoteOnly(sources: FeedSources): List<ProgressItem> {
        val ids = sources.mistakes.map { it.itemId }.toSet()
        return sources.reviewTasks.filterNot { it.itemId in ids }.distinctBy(ReviewRules::taskIdentity)
    }

    fun conjDue(sources: FeedSources, today: Long): List<FeedCard> {
        val drill = sources.drill
        return drill.items.asSequence()
            .filter { it.pointId in drill.learned }
            .mapNotNull { item -> drill.progress[item.id]?.takeIf { it.dueDay <= today }?.let { item to it } }
            .sortedWith(compareBy({ it.second.dueDay }, { it.second.box }))
            .map { (item, progress) -> conjCard(item, drill, dueLabel(progress.dueDay, today)) }
            .toList()
    }

    fun shioriDue(sources: FeedSources, today: Long): List<FeedCard> =
        sources.notebook
            .filter { it.dueDay <= today && !(it.kind == NotebookKind.Vocab && it.headline.trim() in sources.known) }
            .sortedWith(compareBy<NotebookEntry> { it.dueDay }.thenByDescending { it.lapses }.thenBy { it.savedAtMillis })
            .map { shioriCard(it, if (it.dueDay == 0L) "今天" else dueLabel(it.dueDay, today)) }

    fun mistakesDue(sources: FeedSources, today: Long): List<FeedCard> =
        sources.mistakes
            .distinctBy(ReviewRules::mistakeIdentity)
            .filter { (sources.mistakeDue[it.itemId] ?: 0L) <= today }
            .map { mistake ->
                val task = taskFor(mistake, sources.reviewTasks)
                mistakeCard(mistake, task, ReviewRules.dueLabel(task?.nextReviewOn, java.time.LocalDate.ofEpochDay(today)) ?: "今天")
            }

    fun weakCards(sources: FeedSources, today: Long): List<FeedCard> =
        ReviewRules.weakSpots(sources.progressItems, java.time.LocalDate.ofEpochDay(today), limit = 2)
            .filter { it.accuracy < 0.7f }
            .map { FeedCard(key = "weak:${it.name}", kind = FeedKind.Weak, eyebrow = "苦手 · 最近 7 天", weak = it) }

    /**
     * Today's round: mistakes, 活用 and 栞 taken in turns (so one kind never runs long while the
     * others still have cards), most overdue first inside each; a 苦手 card after every 8.
     */
    fun build(sources: FeedSources, today: Long): List<FeedCard> {
        val queues = listOf(mistakesDue(sources, today), conjDue(sources, today), shioriDue(sources, today)).map { ArrayDeque(it) }
        val cards = mutableListOf<FeedCard>()
        while (cards.size < MaxCards && queues.any { it.isNotEmpty() }) {
            for (queue in queues) {
                if (cards.size >= MaxCards) break
                queue.removeFirstOrNull()?.let(cards::add)
            }
        }
        return withWeak(cards, weakCards(sources, today))
    }

    fun withWeak(cards: List<FeedCard>, weak: List<FeedCard>): List<FeedCard> {
        if (weak.isEmpty() || cards.size < 3) return cards
        val out = mutableListOf<FeedCard>()
        var used = 0
        cards.forEachIndexed { i, card ->
            out += card
            if ((i + 1) % WeakEvery == 0 && used < weak.size && i < cards.lastIndex) out += weak[used++]
        }
        if (used == 0) out.add(minOf(4, out.size), weak.first())
        return out
    }

    /** おかわり: learned lines and 栞 cards that are not due, shuffled. */
    fun extra(sources: FeedSources, today: Long, size: Int = ExtraSize): List<FeedCard> {
        val drill = sources.drill
        val lines = drill.items
            .filter { it.pointId in drill.learned && (drill.progress[it.id]?.dueDay ?: 0L) > today }
            .map { conjCard(it, drill, "おかわり") }
        val shiori = sources.notebook
            .filter { it.dueDay > today && !(it.kind == NotebookKind.Vocab && it.headline.trim() in sources.known) }
            .map { shioriCard(it, "おかわり") }
        return (lines + shiori).shuffled().take(size)
    }

    /** A persisted key back to its card, from the current sources; null when it no longer exists. */
    fun resolve(key: String, sources: FeedSources, today: Long): FeedCard? {
        val base = baseKey(key)
        val card = when {
            base == EndKey -> FeedCard(EndKey, FeedKind.End)
            base.startsWith("conj:") -> sources.drill.items.firstOrNull { it.id == base.removePrefix("conj:") }
                ?.let { item -> conjCard(item, sources.drill, sources.drill.progress[item.id]?.let { dueLabel(it.dueDay, today) }) }
            base.startsWith("shiori:") -> sources.notebook.firstOrNull { it.key == base.removePrefix("shiori:") }?.let { shioriCard(it, null) }
            base.startsWith("mistake:") -> sources.mistakes.firstOrNull { it.itemId == base.removePrefix("mistake:") }
                ?.let { mistakeCard(it, taskFor(it, sources.reviewTasks), null) }
            base.startsWith("weak:") -> weakCards(sources, today).firstOrNull { it.key == base }
            else -> null
        }
        return card?.copy(key = key, dueLabel = if (key != base) "もう一回" else card.dueLabel)
    }

    fun encode(s: FeedSession): String = JSONObject()
        .put("day", s.day)
        .put("keys", JSONArray(s.keys))
        .put("index", s.index)
        .put("ok", s.remembered)
        .put("again", s.again)
        .put("master", s.mastered)
        .put("graded", JSONArray(s.graded.toList()))
        .put("filter", s.filter?.name ?: "")
        .put("extra", s.extra)
        .put("finished", s.finished)
        .toString()

    fun decode(raw: String?): FeedSession? = runCatching {
        val o = JSONObject(raw ?: return null)
        fun strings(name: String): List<String> = o.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
        FeedSession(
            day = o.getLong("day"),
            keys = strings("keys"),
            index = o.optInt("index"),
            remembered = o.optInt("ok"),
            again = o.optInt("again"),
            mastered = o.optInt("master"),
            graded = strings("graded").toSet(),
            filter = FeedSource.entries.firstOrNull { it.name == o.optString("filter") },
            extra = o.optBoolean("extra"),
            finished = o.optBoolean("finished"),
        )
    }.getOrNull()
}

// ---------------------------------------------------------------------------------- holder

object ReviewFeed {
    private var store: LocalLabStore? = null
    private val _session = MutableStateFlow<FeedSession?>(null)
    val session: StateFlow<FeedSession?> = _session.asStateFlow()

    /** The session's cards in order (snapshots, so a card mastered mid-round stays on screen). */
    private val _deck = MutableStateFlow<List<FeedCard>>(emptyList())
    val deck: StateFlow<List<FeedCard>> = _deck.asStateFlow()

    private val _mistakeDue = MutableStateFlow<Map<String, Long>>(emptyMap())
    val mistakeDue: StateFlow<Map<String, Long>> = _mistakeDue.asStateFlow()

    private val cards = mutableMapOf<String, FeedCard>()

    fun init(context: Context) {
        if (store != null) return
        val s = LocalLabStore(context.applicationContext)
        store = s
        _session.value = FeedRules.decode(s.readReviewFeedSession())
        _mistakeDue.value = runCatching {
            val o = JSONObject(s.readReviewMistakeDue() ?: "{}")
            o.keys().asSequence().associateWith { o.getLong(it) }
        }.getOrDefault(emptyMap())
    }

    /**
     * Keeps today's round in step with the sources: a new day starts a new round; keys restored
     * after a restart are resolved again (gone ones dropped); cards that fell due meanwhile join
     * before the おわり card.
     */
    @Synchronized
    fun sync(context: Context, sources: FeedSources, today: Long) {
        init(context)
        val s = _session.value
        if (s == null || s.day != today) {
            cards.clear()
            start(FeedSession(day = today, keys = emptyList()), FeedRules.build(sources, today))
            return
        }
        var index = s.index
        val kept = s.keys.filterIndexed { i, key ->
            val alive = key in cards || FeedRules.resolve(key, sources, today)?.also { cards[key] = it } != null
            if (!alive && i < s.index) index--
            alive
        }
        val present = kept.map(FeedRules::baseKey).toSet()
        val added = if (s.extra) emptyList() else FeedRules.build(sources, today)
            .filter { it.isKnowledge && it.key !in present && (s.filter == null || it.source == s.filter) }
        added.forEach { cards[it.key] = it }
        val body = kept.filter { it != FeedRules.EndKey } + added.map { it.key }
        publish(s.copy(keys = withEnd(body), index = index.coerceIn(0, (body.size).coerceAtLeast(0))))
    }

    /** The pager settled on [index]: cards scrolled past without a verdict count as remembered. */
    @Synchronized
    fun settle(context: Context, index: Int, sinks: ReviewSinks) {
        val s = _session.value ?: return
        if (index == s.index || index !in s.keys.indices) return
        if (index > s.index) {
            for (i in s.index until index) {
                val key = s.keys[i]
                if (FeedRules.isKnowledgeKey(key) && key !in (_session.value?.graded ?: emptySet())) grade(context, key, Verdict.Remembered, sinks)
            }
        }
        val now = _session.value ?: return
        val atEnd = now.keys.getOrNull(index) == FeedRules.EndKey
        if (atEnd && !now.finished && now.graded.isNotEmpty()) StudyLog.finishSession(context)
        publish(now.copy(index = index, finished = now.finished || atEnd))
    }

    /** One verdict: counted in StudyLog, written back to where the card came from. */
    @Synchronized
    fun grade(context: Context, key: String, verdict: Verdict, sinks: ReviewSinks) {
        val s = _session.value ?: return
        val card = cards[key] ?: return
        if (!card.isKnowledge || key in s.graded) return
        StudyLog.record(context, answers = 1, correct = if (verdict == Verdict.Again) 0 else 1)
        val today = s.day
        val keepSchedule = s.extra && verdict == Verdict.Remembered
        card.line?.let { if (!keepSchedule) sinks.gradeLine(it.id, verdict != Verdict.Again, verdict == Verdict.Mastered) }
        card.entry?.let { entry ->
            when {
                keepSchedule -> Unit
                verdict == Verdict.Mastered -> Notebook.master(context, entry.key)
                else -> Notebook.grade(context, entry.key, verdict == Verdict.Remembered)
            }
        }
        card.mistake?.let { mistake ->
            val due = _mistakeDue.value
            when {
                verdict == Verdict.Mastered || (verdict == Verdict.Remembered && mistake.itemId in due) -> {
                    sinks.markMistakeReviewed(mistake.itemId)
                    writeMistakeDue(due - mistake.itemId)
                }
                verdict == Verdict.Remembered -> writeMistakeDue(due + (mistake.itemId to today + 2))
                else -> writeMistakeDue(due - mistake.itemId)
            }
            card.task?.let { sinks.gradeTask(it, verdict != Verdict.Again) }
        }
        var keys = s.keys
        if (verdict == Verdict.Again) {
            val base = FeedRules.baseKey(key)
            val copy = "$base#${keys.count { FeedRules.baseKey(it) == base } + 1}"
            cards[copy] = card.copy(key = copy, dueLabel = "もう一回")
            val at = keys.indexOf(key)
            val end = keys.indexOf(FeedRules.EndKey).takeIf { it >= 0 } ?: keys.size
            keys = keys.toMutableList().apply { add(minOf(at + FeedRules.AgainGap + 1, end), copy) }
        }
        publish(
            s.copy(
                keys = keys,
                graded = s.graded + key,
                remembered = s.remembered + if (verdict == Verdict.Remembered) 1 else 0,
                again = s.again + if (verdict == Verdict.Again) 1 else 0,
                mastered = s.mastered + if (verdict == Verdict.Mastered) 1 else 0,
            ),
        )
    }

    /** 斩 from a word card: the headword is known, its 栞 card goes, and it counts as mastered. */
    @Synchronized
    fun cut(context: Context, key: String) {
        val s = _session.value ?: return
        val entry = cards[key]?.entry ?: return
        if (key in s.graded) return
        StudyLog.record(context, answers = 1, correct = 1)
        KnownWords.cutHeadword(context, entry.headline, entry.key)
        publish(s.copy(graded = s.graded + key, mastered = s.mastered + 1))
    }

    /** 帳面 → only one kind; null goes back to everything due. */
    @Synchronized
    fun filter(context: Context, source: FeedSource?, sources: FeedSources, today: Long) {
        init(context)
        val s = _session.value ?: FeedSession(day = today, keys = emptyList())
        val fresh = FeedRules.build(sources, today).filter { source == null || it.source == source }
        start(s.copy(filter = source, extra = false, finished = false, index = 0), fresh)
    }

    /** おかわり after the round: cards that are not due. */
    @Synchronized
    fun extra(context: Context, sources: FeedSources, today: Long) {
        init(context)
        val s = _session.value ?: FeedSession(day = today, keys = emptyList())
        start(s.copy(filter = null, extra = true, finished = false, index = 0), FeedRules.extra(sources, today))
    }

    private fun start(base: FeedSession, fresh: List<FeedCard>) {
        fresh.forEach { cards[it.key] = it }
        publish(base.copy(keys = withEnd(fresh.map { it.key }), index = 0))
    }

    private fun withEnd(body: List<String>): List<String> {
        val cleaned = body.filter { it != FeedRules.EndKey }
        return if (cleaned.none(FeedRules::isKnowledgeKey)) emptyList() else cleaned + FeedRules.EndKey
    }

    private fun publish(next: FeedSession) {
        cards.getOrPut(FeedRules.EndKey) { FeedCard(FeedRules.EndKey, FeedKind.End) }
        val clamped = next.copy(index = next.index.coerceIn(0, (next.keys.size - 1).coerceAtLeast(0)))
        _session.value = clamped
        _deck.value = clamped.keys.mapNotNull(cards::get)
        store?.writeReviewFeedSession(FeedRules.encode(clamped))
    }

    private fun writeMistakeDue(next: Map<String, Long>) {
        _mistakeDue.value = next
        store?.writeReviewMistakeDue(JSONObject(next as Map<*, *>).toString())
    }
}
