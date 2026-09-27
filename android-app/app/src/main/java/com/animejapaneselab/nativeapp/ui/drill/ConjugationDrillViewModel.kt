package com.animejapaneselab.nativeapp.ui.drill

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.data.ConjugationLesson
import com.animejapaneselab.nativeapp.data.DrillProgress
import com.animejapaneselab.nativeapp.data.EpisodeContentCache
import com.animejapaneselab.nativeapp.data.FoundationTopic
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.data.loadConjugationLessons
import com.animejapaneselab.nativeapp.ui.foundation.fetchAllFoundationTopics
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class DrillPhase { Idle, Loading, Ready, Error }

/** [Lesson] = one 課's 練習 after its 板書 (marks the 課 learned at the end); [Review] = mixed Leitner set. */
enum class DrillMode { Lesson, Review }

/** How far one 課 has come: never studied, 已学 with lines still weak, or every line mastered. */
enum class LessonStatus { New, Learned, Mastered }

data class ConjugationDrillState(
    val phase: DrillPhase = DrillPhase.Idle,
    val items: List<ConjugationDrillItem> = emptyList(),
    val progress: Map<String, DrillProgress> = emptyMap(),
    val today: Long = LocalDate.now().toEpochDay(),
    /** Review scope: 教科書 group / 语法点 (null = every learned 課). */
    val group: String? = null,
    val pointId: String? = null,
    val session: List<DrillQuestion> = emptyList(),
    val index: Int = 0,
    val answers: Map<Int, String> = emptyMap(),
    val mode: DrillMode = DrillMode.Review,
    /** 基础题库 topics linked from the feedback sheet's 深入讲解, by id. */
    val topics: Map<String, FoundationTopic> = emptyMap(),
    /** 板書 by point id; [lessonOrder] is the curriculum order of the lessons asset. */
    val lessons: Map<String, ConjugationLesson> = emptyMap(),
    val lessonOrder: List<String> = emptyList(),
    /** 課 whose 板書 + 練習 are done; only their lines enter review. */
    val learned: Set<String> = emptySet(),
    /** 目次 of this 教科書 is open. */
    val openBook: String? = null,
    /** 授業 page (板書 + 用例) of this 課 is open. */
    val openLesson: String? = null,
) {
    fun topicFor(item: ConjugationDrillItem): FoundationTopic? =
        ConjugationDrillRules.topicIdFor(item)?.let(topics::get)

    val groups: List<String> get() = items.map { it.group }.distinct()

    private val linesByPoint: Map<String, List<ConjugationDrillItem>> by lazy { items.groupBy { it.pointId } }

    /** Every 課 in curriculum order: the lessons asset order, then points only the items know. */
    val pointOrder: List<String> by lazy {
        (lessonOrder.filter { it in linesByPoint } + items.sortedBy { it.sortOrder }.map { it.pointId }).distinct()
    }

    fun groupOf(pointId: String): String =
        linesByPoint[pointId]?.firstOrNull()?.group ?: lessons[pointId]?.group.orEmpty()

    fun titleOf(pointId: String): String =
        linesByPoint[pointId]?.firstOrNull()?.pointTitle ?: lessons[pointId]?.title.orEmpty()

    fun lessonsIn(group: String): List<String> = pointOrder.filter { groupOf(it) == group }

    /** 「第 3 課」: the 課's number inside its 教科書. */
    fun lessonNumber(pointId: String): Int = lessonsIn(groupOf(pointId)).indexOf(pointId) + 1

    fun linesOf(pointId: String): List<ConjugationDrillItem> = linesByPoint[pointId].orEmpty()

    fun statusOf(pointId: String): LessonStatus {
        if (pointId !in learned) return LessonStatus.New
        val lines = linesOf(pointId)
        return if (lines.isNotEmpty() && lines.all { (progress[it.id]?.box ?: 0) >= ConjugationDrillRules.MasteredBox }) {
            LessonStatus.Mastered
        } else {
            LessonStatus.Learned
        }
    }

    /** The first 課 not yet learned, inside [group] when given; null when all are learned. */
    fun nextLesson(group: String? = null): String? =
        (if (group == null) pointOrder else lessonsIn(group)).firstOrNull { it !in learned }

    /** Review scope: learned 課 only, inside the 教科書 / 语法点 filter. */
    val scoped: List<ConjugationDrillItem>
        get() = items.filter {
            it.pointId in learned && (group == null || it.group == group) && (pointId == null || it.pointId == pointId)
        }

    val current: DrillQuestion? get() = session.getOrNull(index)
    val isComplete: Boolean get() = session.isNotEmpty() && index >= session.size
    val correctCount: Int get() = answers.count { (i, id) -> session.getOrNull(i)?.answerId == id }

    /** Due lines inside the review scope. */
    val dueInScope: Int get() = ConjugationDrillRules.dueCount(scoped, progress, today)

    /** Lines due today, from local progress alone (works before [items] load). */
    val dueToday: Int get() = progress.values.count { it.dueDay <= LocalDate.now().toEpochDay() }
}

/**
 * 活用道場 state holder: loads items once, bundles the 板書 lessons, schedules locally (Leitner
 * and 已学 in [LocalLabStore]). Learning goes 目次 → 授業 (板書 + 用例) → 練習; review draws
 * only from learned 課.
 */
class ConjugationDrillViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalLabStore(application)
    private val contentCache = EpisodeContentCache(application.filesDir)
    private val _state = MutableStateFlow(
        ConjugationDrillState(progress = store.readDrillProgress(), learned = store.readLearnedPoints().orEmpty()),
    )
    val state: StateFlow<ConjugationDrillState> = _state.asStateFlow()
    private var startWhenReady = false

    init {
        viewModelScope.launch {
            val lessons = withContext(Dispatchers.IO) { loadConjugationLessons(application) }
            _state.update { s -> s.copy(lessons = lessons.associateBy(ConjugationLesson::pointId), lessonOrder = lessons.map { it.pointId }) }
        }
    }

    fun ensureLoaded() {
        if (_state.value.phase == DrillPhase.Idle || _state.value.phase == DrillPhase.Error) refresh()
    }

    fun refresh() {
        _state.update { it.copy(phase = DrillPhase.Loading) }
        viewModelScope.launch {
            val client = RemoteLabClient(store.readSettings().apiBaseUrl, store.readSessionCookie(), contentCache = contentCache)
            val result = withContext(Dispatchers.IO) { runCatching { client.fetchConjugationDrillItems() } }
            _state.update { s ->
                result.fold(
                    onSuccess = { items -> s.copy(phase = DrillPhase.Ready, items = items, today = LocalDate.now().toEpochDay()) },
                    onFailure = { s.copy(phase = DrillPhase.Error) },
                )
            }
            if (result.isSuccess) migrateLearned()
            if (result.isSuccess && startWhenReady) startSession()
            startWhenReady = false
            if (result.isSuccess && _state.value.topics.isEmpty()) {
                val linked = ConjugationDrillRules.linkedTopicIds
                withContext(Dispatchers.IO) { runCatching { client.fetchAllFoundationTopics() } }
                    .onSuccess { all -> _state.update { it.copy(topics = all.filter { t -> t.id in linked }.associateBy(FoundationTopic::id)) } }
            }
        }
    }

    /** Before 学习台 existed every point was drilled directly: points already practised count as learned. */
    private fun migrateLearned() {
        if (store.readLearnedPoints() != null) return
        val s = _state.value
        val practised = s.items.filter { it.id in s.progress }.map { it.pointId }.toSet()
        store.writeLearnedPoints(practised)
        _state.update { it.copy(learned = practised) }
    }

    // ------------------------------------------------------------ pages

    fun openBook(group: String) = _state.update { it.copy(openBook = group, group = group, pointId = null) }

    fun closeBook() = _state.update { it.copy(openBook = null, group = null, pointId = null) }

    fun openLesson(pointId: String) = _state.update {
        it.copy(openLesson = pointId, openBook = it.openBook ?: it.groupOf(pointId).ifBlank { null })
    }

    fun closeLesson() = _state.update { it.copy(openLesson = null) }

    /** Leaving 第三巻 altogether (volume switch): drop pages and any set in progress. */
    fun leave() = _state.update {
        it.copy(openBook = null, openLesson = null, group = null, pointId = null, session = emptyList(), index = 0, answers = emptyMap())
    }

    fun selectPoint(pointId: String?) = _state.update { it.copy(pointId = pointId) }

    fun resetFilters() = _state.update { it.copy(group = it.openBook, pointId = null) }

    // ------------------------------------------------------------ sets

    /** 復習 entry: a set over every learned 課 (due lines first); waits for the items if still loading. */
    fun startReview() {
        _state.update { it.copy(openBook = null, openLesson = null, group = null, pointId = null) }
        if (_state.value.phase == DrillPhase.Ready) startSession() else {
            startWhenReady = true
            ensureLoaded()
        }
    }

    /** Review set inside the current scope (learned 課 only). */
    fun startSession() = _state.update { s ->
        val today = LocalDate.now().toEpochDay()
        val picked = ConjugationDrillRules.pickSession(s.scoped, s.progress, today)
        s.copy(
            today = today,
            mode = DrillMode.Review,
            session = picked.map { ConjugationDrillRules.question(it, s.items, s.progress[it.id]?.seen ?: 0) },
            index = 0,
            answers = emptyMap(),
        )
    }

    /** 練習 of the open 課: its 板書 practice questions, then a few of its anime lines. */
    fun startLesson() = _state.update { s ->
        val point = s.openLesson ?: return@update s
        val practice = s.lessons[point]?.practice.orEmpty().map { ConjugationDrillRules.practice(point, it) }
        val lines = ConjugationDrillRules.pickLesson(s.linesOf(point), s.progress)
            .map { ConjugationDrillRules.question(it, s.items, s.progress[it.id]?.seen ?: 0) }
        s.copy(today = LocalDate.now().toEpochDay(), mode = DrillMode.Lesson, session = practice + lines, index = 0, answers = emptyMap())
    }

    fun answer(optionId: String) {
        val s = _state.value
        val question = s.current ?: return
        if (s.answers.containsKey(s.index)) return
        val correct = question.answerId == optionId
        val item = question.item
        val updated = if (item == null) s.progress else {
            s.progress + (item.id to ConjugationDrillRules.schedule(s.progress[item.id], correct, s.today))
        }
        if (item != null) store.writeDrillProgress(updated)
        StudyLog.record(getApplication(), answers = 1, correct = if (correct) 1 else 0)
        _state.update { it.copy(progress = updated, answers = it.answers + (it.index to optionId)) }
    }

    fun next() {
        _state.update { it.copy(index = (it.index + 1).coerceAtMost(it.session.size)) }
        val s = _state.value
        val point = s.openLesson
        if (s.mode == DrillMode.Lesson && s.isComplete && point != null && point !in s.learned) {
            val learned = s.learned + point
            store.writeLearnedPoints(learned)
            _state.update { it.copy(learned = learned) }
        }
    }

    /** つづく after a 課 → the next unlearned 課 of the same 教科書 (else of the whole volume). */
    fun openNextLesson() = _state.update { s ->
        val next = s.nextLesson(s.openBook) ?: s.nextLesson()
        s.copy(openLesson = next, openBook = next?.let { s.groupOf(it) } ?: s.openBook, session = emptyList(), index = 0, answers = emptyMap())
    }

    /** Leaves the set. A finished 課 goes back to its 目次; a half-done one back to its 板書. */
    fun exitSession() = _state.update { s ->
        val done = s.mode == DrillMode.Lesson && s.isComplete
        s.copy(session = emptyList(), index = 0, answers = emptyMap(), openLesson = if (done) null else s.openLesson)
    }
}
