package com.animejapaneselab.nativeapp.ui.drill

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.data.DrillProgress
import com.animejapaneselab.nativeapp.data.EpisodeContentCache
import com.animejapaneselab.nativeapp.data.FoundationTopic
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.ui.foundation.fetchAllFoundationTopics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class DrillPhase { Idle, Loading, Ready, Error }

data class ConjugationDrillState(
    val phase: DrillPhase = DrillPhase.Idle,
    val items: List<ConjugationDrillItem> = emptyList(),
    val progress: Map<String, DrillProgress> = emptyMap(),
    val today: Long = LocalDate.now().toEpochDay(),
    val group: String? = null,
    val pointId: String? = null,
    val session: List<DrillQuestion> = emptyList(),
    val index: Int = 0,
    val answers: Map<Int, String> = emptyMap(),
    /** 基础题库 topics linked from the feedback sheet's 深入讲解, by id. */
    val topics: Map<String, FoundationTopic> = emptyMap(),
) {
    fun topicFor(item: ConjugationDrillItem): FoundationTopic? =
        ConjugationDrillRules.topicIdFor(item)?.let(topics::get)

    val groups: List<String> get() = items.map { it.group }.distinct()

    /** Items inside the current 教科書 / 语法点 filter. */
    val scoped: List<ConjugationDrillItem>
        get() = items.filter { (group == null || it.group == group) && (pointId == null || it.pointId == pointId) }

    val points: List<Pair<String, String>>
        get() = items.filter { group == null || it.group == group }.map { it.pointId to it.pointTitle }.distinct()

    val current: DrillQuestion? get() = session.getOrNull(index)
    val isComplete: Boolean get() = session.isNotEmpty() && index >= session.size
    val correctCount: Int get() = answers.count { (i, id) -> session.getOrNull(i)?.answerId == id }
    val nextSessionSize: Int get() = ConjugationDrillRules.pickSession(scoped, progress, today).size

    /** Lines due today, from local progress alone (works before [items] load). */
    val dueToday: Int get() = progress.values.count { it.dueDay <= LocalDate.now().toEpochDay() }
}

/** 活用道場 state holder: loads items once, schedules locally (Leitner in [LocalLabStore]). */
class ConjugationDrillViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalLabStore(application)
    private val contentCache = EpisodeContentCache(application.filesDir)
    private val _state = MutableStateFlow(ConjugationDrillState(progress = store.readDrillProgress()))
    val state: StateFlow<ConjugationDrillState> = _state.asStateFlow()
    private var startWhenReady = false

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
            if (result.isSuccess && startWhenReady) startSession()
            startWhenReady = false
            if (result.isSuccess && _state.value.topics.isEmpty()) {
                val linked = ConjugationDrillRules.linkedTopicIds
                withContext(Dispatchers.IO) { runCatching { client.fetchAllFoundationTopics() } }
                    .onSuccess { all -> _state.update { it.copy(topics = all.filter { t -> t.id in linked }.associateBy(FoundationTopic::id)) } }
            }
        }
    }

    fun selectGroup(group: String?) = _state.update {
        it.copy(group = if (group == it.group) null else group, pointId = null)
    }

    fun selectPoint(pointId: String?) = _state.update { it.copy(pointId = pointId) }

    fun resetFilters() = _state.update { it.copy(group = null, pointId = null) }

    /** 復習 entry: a full-scope set (due lines first); waits for the items if they are still loading. */
    fun startReview() {
        resetFilters()
        if (_state.value.phase == DrillPhase.Ready) startSession() else {
            startWhenReady = true
            ensureLoaded()
        }
    }

    fun startSession() = _state.update { s ->
        val today = LocalDate.now().toEpochDay()
        val picked = ConjugationDrillRules.pickSession(s.scoped, s.progress, today)
        val pool = s.items
        s.copy(
            today = today,
            session = picked.map { ConjugationDrillRules.question(it, pool, s.progress[it.id]?.seen ?: 0) },
            index = 0,
            answers = emptyMap(),
        )
    }

    fun answer(optionId: String) {
        val s = _state.value
        val question = s.current ?: return
        if (s.answers.containsKey(s.index)) return
        val correct = question.answerId == optionId
        val updated = s.progress + (question.item.id to ConjugationDrillRules.schedule(s.progress[question.item.id], correct, s.today))
        store.writeDrillProgress(updated)
        _state.update { it.copy(progress = updated, answers = it.answers + (it.index to optionId)) }
    }

    fun next() = _state.update { it.copy(index = (it.index + 1).coerceAtMost(it.session.size)) }

    fun exitSession() = _state.update { it.copy(session = emptyList(), index = 0, answers = emptyMap()) }
}
