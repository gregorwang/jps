package com.animejapaneselab.nativeapp.ui.review

import android.content.Context
import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.NotebookEntry
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillState
import com.animejapaneselab.nativeapp.ui.knowledge.KnowMark
import com.animejapaneselab.nativeapp.ui.knowledge.Knowledge
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeCard
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeRules
import com.animejapaneselab.nativeapp.ui.knowledge.VocabWord
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
 * 復習 → 知識: an endless vertical feed of knowledge cards (the reading documents, one deck each,
 * plus words from 資料), with everything due — 活用 lines of learned 課, 收藏 cards, local mistakes
 * (merged with their server review task) — woven in. A verdict on a due card is written back to the
 * scheduler it came from (drill Leitner, 栞 box, 错题本, server progress); ♥ on a knowledge card
 * goes to [Knowledge].
 */

enum class FeedKind { Conj, Word, Listen, Mistake, Weak, Know, Vocab }

enum class FeedSource(val label: String) { Conj("活用"), Shiori("收藏"), Mistake("错题") }

/** 上划 = [Remembered], 再来 = [Again], ♥ = [Mastered]. */
enum class Verdict { Remembered, Again, Mastered }

data class FeedCard(
    /**
     * `conj:<itemId>`, `shiori:<notebookKey>`, `mistake:<itemId>`, `weak:<name>`, `know:<cardId>`,
     * `vocab:<vocabId>`; a requeued copy adds `#n`.
     */
    val key: String,
    val kind: FeedKind,
    val eyebrow: String = "",
    val dueLabel: String? = null,
    val line: ConjugationDrillItem? = null,
    val entry: NotebookEntry? = null,
    val mistake: MistakeRecord? = null,
    val task: ProgressItem? = null,
    val weak: ReviewRules.WeakSpot? = null,
    val know: KnowledgeCard? = null,
    val vocab: VocabWord? = null,
) {
    val source: FeedSource?
        get() = when (kind) {
            FeedKind.Conj -> FeedSource.Conj
            FeedKind.Word, FeedKind.Listen -> FeedSource.Shiori
            FeedKind.Mistake -> FeedSource.Mistake
            else -> null
        }

    /** Came from a schedule and takes a verdict (上划 remembered, 再来, ♥ mastered). */
    val isDue: Boolean get() = kind == FeedKind.Conj || kind == FeedKind.Word || kind == FeedKind.Listen || kind == FeedKind.Mistake
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
    val know: List<KnowledgeCard> = emptyList(),
    val marks: Map<String, KnowMark> = emptyMap(),
    val words: List<VocabWord> = emptyList(),
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
    /** 帳面 → one deck: only that document's cards. */
    val deck: String? = null,
    /** Knowledge / word cards scrolled past today. */
    val read: Int = 0,
    /** Today's square in 最近 12 週 is lit (once a day). */
    val finished: Boolean = false,
) {
    /** Due cards of today's feed not graded yet (for 今日 三限). */
    val remaining: Int get() = keys.count { FeedRules.isDueKey(it) && it !in graded }
}

// ---------------------------------------------------------------------------------- rules

object FeedRules {
    const val MaxCards = 40
    const val WeakEvery = 8
    const val AgainGap = 4
    /** Stream cards added at a time; more are added when the reader gets near the end. */
    const val Batch = 9
    /** Read this many stream cards (or clear the due ones) and today's square lights up. */
    const val ReadToFinish = 10

    fun baseKey(key: String): String = key.substringBefore('#')

    fun isDueKey(key: String): Boolean = key.startsWith("conj:") || key.startsWith("shiori:") || key.startsWith("mistake:")

    fun dueLabel(dueDay: Long, today: Long): String = if (dueDay < today) "逾期 ${today - dueDay} 天" else "今天"

    fun knowCard(card: KnowledgeCard): FeedCard = FeedCard(
        key = "know:${card.id}",
        kind = FeedKind.Know,
        eyebrow = listOf(card.deckTitle, card.topic).filter { it.isNotBlank() }.joinToString(" · "),
        know = card,
    )

    fun vocabCard(word: VocabWord): FeedCard = FeedCard(key = "vocab:${word.id}", kind = FeedKind.Vocab, eyebrow = "資料 · 単語", vocab = word)

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
            NotebookKind.Vocab -> "收藏 · 词汇"
            NotebookKind.Grammar -> "收藏 · 文型"
            NotebookKind.Line -> "收藏 · 听辨"
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

    /** Everything due today: mistakes, 活用 and 收藏 taken in turns, a 苦手 card after every 8. */
    fun due(sources: FeedSources, today: Long, only: FeedSource? = null): List<FeedCard> {
        val queues = listOf(mistakesDue(sources, today), conjDue(sources, today), shioriDue(sources, today))
            .map { q -> ArrayDeque(q.filter { only == null || it.source == only }) }
        val cards = mutableListOf<FeedCard>()
        while (cards.size < MaxCards && queues.any { it.isNotEmpty() }) {
            for (queue in queues) {
                if (cards.size >= MaxCards) break
                queue.removeFirstOrNull()?.let(cards::add)
            }
        }
        return if (only == null) withWeak(cards, weakCards(sources, today)) else cards
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

    /**
     * The next [Batch] stream cards after [recentKeys]: two knowledge cards, then a word from 資料
     * (knowledge only, inside one deck). Words keep it going once every document is read and ♥.
     */
    fun stream(sources: FeedSources, today: Long, recentKeys: List<String>, deck: String?, seed: Long): List<FeedCard> {
        val recent = recentKeys.map(::baseKey).toSet()
        val cards = if (deck == null) sources.know else sources.know.filter { it.deckId == deck }
        val knowRecent = recent.filter { it.startsWith("know:") }.map { it.removePrefix("know:") }.toSet()
        var know = KnowledgeRules.pick(cards, sources.marks, today, knowRecent, if (deck == null) Batch * 2 / 3 else Batch)
        // A small deck read through: let the least recent cards come round again.
        if (know.isEmpty() && deck != null) know = KnowledgeRules.pick(cards, sources.marks, today, emptySet(), Batch)
        if (deck != null) return know.map(::knowCard)
        val vocabRecent = recent.filter { it.startsWith("vocab:") }.map { it.removePrefix("vocab:") }.toSet()
        val words = ArrayDeque(KnowledgeRules.pickVocab(sources.words, vocabRecent, Batch - know.size, seed))
        val knowQueue = ArrayDeque(know)
        val out = mutableListOf<FeedCard>()
        while (knowQueue.isNotEmpty() || words.isNotEmpty()) {
            repeat(2) { knowQueue.removeFirstOrNull()?.let { out += knowCard(it) } }
            words.removeFirstOrNull()?.let { out += vocabCard(it) }
        }
        return out
    }

    /** Due cards woven into the stream: one after every two stream cards. */
    fun weave(stream: List<FeedCard>, due: List<FeedCard>): List<FeedCard> {
        val d = ArrayDeque(due)
        val out = mutableListOf<FeedCard>()
        stream.forEachIndexed { i, card ->
            out += card
            if (i % 2 == 1) d.removeFirstOrNull()?.let { out += it }
        }
        return out + d
    }

    /** A persisted key back to its card, from the current sources; null when it no longer exists. */
    fun resolve(key: String, sources: FeedSources, today: Long): FeedCard? {
        val base = baseKey(key)
        val card = when {
            base.startsWith("conj:") -> sources.drill.items.firstOrNull { it.id == base.removePrefix("conj:") }
                ?.let { item -> conjCard(item, sources.drill, sources.drill.progress[item.id]?.let { dueLabel(it.dueDay, today) }) }
            base.startsWith("shiori:") -> sources.notebook.firstOrNull { it.key == base.removePrefix("shiori:") }?.let { shioriCard(it, null) }
            base.startsWith("mistake:") -> sources.mistakes.firstOrNull { it.itemId == base.removePrefix("mistake:") }
                ?.let { mistakeCard(it, taskFor(it, sources.reviewTasks), null) }
            base.startsWith("weak:") -> weakCards(sources, today).firstOrNull { it.key == base }
            base.startsWith("know:") -> sources.know.firstOrNull { it.id == base.removePrefix("know:") }?.let(::knowCard)
            base.startsWith("vocab:") -> sources.words.firstOrNull { it.id == base.removePrefix("vocab:") }?.let(::vocabCard)
            else -> null
        }
        return card?.copy(key = key, dueLabel = if (key != base && isDueKey(base)) "もう一回" else card.dueLabel)
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
        .put("deck", s.deck ?: "")
        .put("read", s.read)
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
            deck = o.optString("deck").ifBlank { null },
            read = o.optInt("read"),
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
     * Keeps today's feed in step with the sources: a new day starts a new one; keys restored after
     * a restart are resolved again (gone ones dropped); cards that fell due meanwhile are woven in
     * just after the card on screen.
     */
    @Synchronized
    fun sync(context: Context, sources: FeedSources, today: Long) {
        init(context)
        val s = _session.value
        if (s == null || s.day != today) {
            cards.clear()
            start(FeedSession(day = today, keys = emptyList()), fresh(sources, today, null, null, emptyList()))
            return
        }
        var index = s.index
        val kept = s.keys.filterIndexed { i, key ->
            val alive = key in cards || FeedRules.resolve(key, sources, today)?.also { cards[key] = it } != null
            if (!alive && i < s.index) index--
            alive
        }
        val present = kept.map(FeedRules::baseKey).toSet()
        val added = if (s.deck != null) emptyList() else FeedRules.due(sources, today, s.filter).filter { it.isDue && it.key !in present }
        added.forEach { cards[it.key] = it }
        var keys = kept
        if (added.isNotEmpty()) {
            val at = (index + 2).coerceAtMost(keys.size)
            keys = keys.take(at) + added.map { it.key } + keys.drop(at)
        }
        if (keys.size - index <= 4) keys = keys + more(sources, today, s.deck, keys)
        publish(s.copy(keys = keys, index = index.coerceAtLeast(0)))
    }

    /**
     * The pager settled on [index]: due cards scrolled past without a verdict count as remembered,
     * knowledge cards scrolled past count as read, and more cards are added near the end.
     */
    @Synchronized
    fun settle(context: Context, index: Int, sinks: ReviewSinks, sources: FeedSources) {
        val s = _session.value ?: return
        if (index == s.index || index !in s.keys.indices) return
        var read = s.read
        if (index > s.index) {
            for (i in s.index until index) {
                val key = s.keys[i]
                when {
                    FeedRules.isDueKey(key) && key !in (_session.value?.graded ?: emptySet()) -> grade(context, key, Verdict.Remembered, sinks)
                    key.startsWith("know:") -> {
                        Knowledge.seen(context, FeedRules.baseKey(key).removePrefix("know:"), s.day)
                        read++
                    }
                    key.startsWith("vocab:") -> read++
                }
            }
        }
        val now = _session.value ?: return
        val dueLeft = now.keys.any { FeedRules.isDueKey(it) && it !in now.graded }
        val lit = !now.finished && (read >= FeedRules.ReadToFinish || (!dueLeft && now.graded.isNotEmpty()))
        if (lit) StudyLog.finishSession(context)
        var keys = now.keys
        if (keys.size - index <= 4) keys = keys + more(sources, now.day, now.deck, keys)
        publish(now.copy(keys = keys, index = index, read = read, finished = now.finished || lit))
    }

    /** One verdict on a due card: counted in StudyLog, written back to where the card came from. */
    @Synchronized
    fun grade(context: Context, key: String, verdict: Verdict, sinks: ReviewSinks) {
        val s = _session.value ?: return
        val card = cards[key] ?: return
        if (!card.isDue || key in s.graded) return
        StudyLog.record(context, answers = 1, correct = if (verdict == Verdict.Again) 0 else 1)
        val today = s.day
        card.line?.let { sinks.gradeLine(it.id, verdict != Verdict.Again, verdict == Verdict.Mastered) }
        card.entry?.let { entry ->
            when (verdict) {
                Verdict.Mastered -> Notebook.master(context, entry.key)
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
            keys = keys.toMutableList().apply { add(minOf(at + FeedRules.AgainGap + 1, size), copy) }
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

    /** 斩 from a 收藏 word card: the headword is known, its card goes, and it counts as mastered. */
    @Synchronized
    fun cut(context: Context, key: String) {
        val s = _session.value ?: return
        val entry = cards[key]?.entry ?: return
        if (key in s.graded) return
        StudyLog.record(context, answers = 1, correct = 1)
        KnownWords.cutHeadword(context, entry.headline, entry.key)
        publish(s.copy(graded = s.graded + key, mastered = s.mastered + 1))
    }

    /** 帳面 → only one kind of due card (then the stream); null goes back to everything. */
    @Synchronized
    fun filter(context: Context, source: FeedSource?, sources: FeedSources, today: Long) {
        init(context)
        val s = _session.value?.takeIf { it.day == today } ?: FeedSession(day = today, keys = emptyList())
        start(s.copy(filter = source, deck = null), fresh(sources, today, source, null, s.keys))
    }

    /** 帳面 → one document's cards only; null goes back to everything. */
    @Synchronized
    fun deck(context: Context, deck: String?, sources: FeedSources, today: Long) {
        init(context)
        val s = _session.value?.takeIf { it.day == today } ?: FeedSession(day = today, keys = emptyList())
        start(s.copy(filter = null, deck = deck), fresh(sources, today, null, deck, s.keys))
    }

    private fun fresh(sources: FeedSources, today: Long, only: FeedSource?, deck: String?, recent: List<String>): List<FeedCard> {
        val stream = FeedRules.stream(sources, today, recent.takeLast(60), deck, seed(today, recent.size))
        if (deck != null) return stream
        val due = FeedRules.due(sources, today, only)
        return if (only != null) due + stream else FeedRules.weave(stream, due)
    }

    private fun more(sources: FeedSources, today: Long, deck: String?, keys: List<String>): List<String> {
        val taken = keys.toMutableSet()
        // A card coming round again gets `#n`: pager keys have to stay unique.
        return FeedRules.stream(sources, today, keys.takeLast(60), deck, seed(today, keys.size)).map { card ->
            var key = card.key
            var n = 1
            while (key in taken) key = "${card.key}#${++n}"
            taken += key
            cards[key] = card.copy(key = key)
            key
        }
    }

    private fun seed(today: Long, n: Int): Long = today * 1_000L + n

    private fun start(base: FeedSession, fresh: List<FeedCard>) {
        fresh.forEach { cards[it.key] = it }
        publish(base.copy(keys = fresh.map { it.key }, index = 0))
    }

    private fun publish(next: FeedSession) {
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
