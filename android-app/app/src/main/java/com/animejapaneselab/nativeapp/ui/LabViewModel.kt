package com.animejapaneselab.nativeapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animejapaneselab.nativeapp.data.AiCoachState
import com.animejapaneselab.nativeapp.data.AuthUser
import com.animejapaneselab.nativeapp.data.EpisodeFocus
import com.animejapaneselab.nativeapp.data.EpisodeOption
import com.animejapaneselab.nativeapp.data.EpisodeSelection
import com.animejapaneselab.nativeapp.data.GrammarPoint
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.LessonNode
import com.animejapaneselab.nativeapp.data.LessonTarget
import com.animejapaneselab.nativeapp.data.LinguisticExercise
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReadAirScene
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.data.ReviewState
import com.animejapaneselab.nativeapp.data.SampleLearningRepository
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.SyncSnapshot
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.data.WorkOption
import com.animejapaneselab.nativeapp.data.buildLinguisticProgressPayload
import com.animejapaneselab.nativeapp.domain.LessonEngine
import com.animejapaneselab.nativeapp.domain.LessonSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant

const val ReadAirAllFilter = "all"
const val ReadAirAiQuestion = "请结合台词解释这道读空气题。"

class LabViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SampleLearningRepository()
    private val store = LocalLabStore(application)
    private val initialWorks = repository.works()
    private val deviceId = store.deviceId()
    private val initialSettings = store.readSettings()
    private val initialSelection = store.readSelection(repository.defaultSelection)
    private val lastEpisodesByWork = store.readLastEpisodesByWork().toMutableMap().apply {
        if (initialSelection.workSlug.isNotBlank() && initialSelection.episode > 0) {
            put(initialSelection.workSlug, initialSelection.episode)
        }
    }
    private val initialEpisodes = repository.episodes(initialSelection.workSlug)
    private val initialFocus = lightweightFocus(initialSelection, initialWorks)
    private val initialScene = lightweightReadAirScene(initialSelection)
    private val initialLessonBatch = 1
    private var readAirCatalogLoadStarted = false
    private var authRefreshStarted = false

    private val _uiState = MutableStateFlow(
        LabUiState(
            deviceId = deviceId,
            settings = initialSettings,
            works = initialWorks,
            episodes = initialEpisodes,
            selection = initialSelection,
            focus = initialFocus,
            vocab = emptyList(),
            grammar = emptyList(),
            shadowing = emptyList(),
            scenes = listOf(initialScene),
            selectedScene = initialScene,
            readAir = ReadAirTrainingState(
                exercises = emptyList(),
                message = "正在准备当前集内容；完整题库会在进入读空气时加载。",
                usingFallback = true,
            ),
            lesson = LessonEngine.start(emptyList()),
            lessonMode = LessonMode.Mixed,
            lessonBatch = initialLessonBatch,
            hasNextLessonBatch = false,
            mistakes = store.readMistakes(),
        ),
    )
    val uiState: StateFlow<LabUiState> = _uiState.asStateFlow()

    init {
        loadInitialEpisodeContent()
        refreshAuthStateOnce()
    }

    private fun loadInitialEpisodeContent() {
        val selection = initialSelection
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.Default) {
                val content = repository.content(selection, LessonMode.Mixed, initialLessonBatch)
                val exercises = repository.readAirExercises(selection)
                val hasNextBatch = repository.hasNextLessonBatch(
                    vocab = content.vocab,
                    grammar = content.grammar,
                    sentences = content.shadowing,
                    mode = LessonMode.Mixed,
                    batch = initialLessonBatch,
                )
                InitialEpisodeContent(content, exercises, hasNextBatch)
            }
            _uiState.update { state ->
                if (state.selection != selection) return@update state
                val exercises = mergeReadAirExercises(state.readAir.exercises, snapshot.readAirExercises)
                state.copy(
                    focus = snapshot.content.focus,
                    vocab = snapshot.content.vocab,
                    grammar = snapshot.content.grammar,
                    shadowing = snapshot.content.shadowing,
                    scenes = snapshot.content.scenes,
                    selectedScene = snapshot.content.scenes.firstOrNull() ?: state.selectedScene,
                    readAir = state.readAir.copy(
                        exercises = exercises,
                        message = "当前集样例题已可用；正在准备完整题库。",
                        selectedAnswers = state.readAir.selectedAnswers.filterKeys { id ->
                            exercises.any { it.id == id }
                        } + persistedReadAirAnswers(exercises, state.progressItems),
                        pinnedExerciseId = state.readAir.pinnedExerciseId?.takeIf { id ->
                            exercises.any { it.id == id }
                        },
                    ),
                    lesson = LessonEngine.start(snapshot.content.lessonNodes),
                    hasNextLessonBatch = snapshot.hasNextLessonBatch,
                )
            }
        }
    }

    private fun ensureFallbackReadAirCatalogLoaded() {
        if (readAirCatalogLoadStarted) return
        readAirCatalogLoadStarted = true
        val fallbackExercises = repository.allReadAirExercises()
        _uiState.update { state ->
            val exercises = mergeReadAirExercises(fallbackExercises, state.readAir.exercises)
            val existingIds = state.readAir.exercises.map { it.id }.toSet()
            val addedFallbackExercises = fallbackExercises.any { it.id !in existingIds }
            state.copy(
                readAir = state.readAir.copy(
                    exercises = exercises,
                    message = "完整题库已准备：${exercises.size} 道；可从云端更新今日练习。",
                    usingFallback = state.readAir.usingFallback || addedFallbackExercises,
                    selectedAnswers = state.readAir.selectedAnswers.filterKeys { id ->
                        exercises.any { it.id == id }
                    } + persistedReadAirAnswers(exercises, state.progressItems),
                    pinnedExerciseId = state.readAir.pinnedExerciseId?.takeIf { id ->
                        exercises.any { it.id == id }
                    },
                ),
            )
        }
    }

    private fun remoteClient(): RemoteLabClient {
        return RemoteLabClient(_uiState.value.settings.apiBaseUrl, store.readSessionCookie())
    }

    private fun fetchRemoteProgressSnapshot(client: RemoteLabClient, deviceId: String): RemoteProgressSnapshot {
        return RemoteProgressSnapshot(
            progress = client.fetchProgress(deviceId),
            review = client.fetchReviewTasks(deviceId),
        )
    }

    fun selectTab(tab: LabTab) {
        _uiState.update { it.copy(selectedTab = tab, activeSession = null) }
        when (tab) {
            LabTab.ReadAir -> ensureFallbackReadAirCatalogLoaded()
            LabTab.Library -> ensureFallbackReadAirCatalogLoaded()
            LabTab.Settings -> refreshAuthStateOnce()
            LabTab.Today,
            LabTab.Lesson,
            LabTab.Review -> Unit
        }
    }

    private fun refreshAuthStateOnce() {
        if (authRefreshStarted) return
        authRefreshStarted = true
        refreshAuthState()
    }

    fun refreshAuthState() {
        authRefreshStarted = true
        _uiState.update { it.copy(auth = it.auth.copy(status = SyncStatus.Loading, message = "正在检查账号状态和云端进度")) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    val user = client.fetchAuthMe()
                    val snapshot = fetchRemoteProgressSnapshot(client, deviceId)
                    user to snapshot
                }
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { (user, snapshot) ->
                        val scope = if (user == null) "deviceId" else "账号"
                        state.withRemoteProgressSnapshot(snapshot).copy(
                            auth = AuthState(
                                status = SyncStatus.Success,
                                user = user,
                                message = if (user == null) {
                                    "未登录，当前使用 deviceId 同步；已读取进度 ${snapshot.progress.size} 条，复习 ${snapshot.review.size} 条。"
                                } else {
                                    "已登录：${user.email}；当前按$scope 读取进度 ${snapshot.progress.size} 条，复习 ${snapshot.review.size} 条。"
                                },
                            ),
                            sync = SyncSnapshot(
                                status = SyncStatus.Success,
                                message = "账号状态刷新完成：$scope 范围进度 ${snapshot.progress.size} 条，复习 ${snapshot.review.size} 条",
                                lastSyncedAt = Instant.now().toString(),
                                remoteReviewCount = snapshot.review.size,
                                catalogUpdated = state.sync.catalogUpdated,
                            ),
                        )
                    },
                    onFailure = { error ->
                        val message = "账号状态读取失败：${error.message ?: "网络不可用"}"
                        state.copy(
                            auth = state.auth.copy(status = SyncStatus.Error, message = message),
                            sync = state.sync.copy(status = SyncStatus.Error, message = message),
                        )
                    },
                )
            }
        }
    }

    fun loginOwner(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(auth = it.auth.copy(status = SyncStatus.Error, message = "请输入邮箱和密码。")) }
            return
        }
        _uiState.update { it.copy(auth = it.auth.copy(status = SyncStatus.Loading, message = "正在登录")) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val login = RemoteLabClient(_uiState.value.settings.apiBaseUrl).loginOwner(trimmedEmail, password, deviceId)
                    store.writeSessionCookie(login.sessionCookie)
                    val snapshot = fetchRemoteProgressSnapshot(remoteClient(), deviceId)
                    login to snapshot
                }
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { (login, snapshot) ->
                        state.withRemoteProgressSnapshot(snapshot).copy(
                            auth = AuthState(
                                status = SyncStatus.Success,
                                user = login.user,
                                message = "已登录：${login.user.email}；已读取云端进度 ${snapshot.progress.size} 条。",
                            ),
                            sync = SyncSnapshot(
                                status = SyncStatus.Success,
                                message = "登录后已同步 ${snapshot.progress.size} 条进度，今日复习 ${snapshot.review.size} 条",
                                lastSyncedAt = Instant.now().toString(),
                                remoteReviewCount = snapshot.review.size,
                                catalogUpdated = state.sync.catalogUpdated,
                            ),
                        )
                    },
                    onFailure = { error ->
                        state.copy(auth = state.auth.copy(status = SyncStatus.Error, message = "登录失败：${error.loginFailureMessage()}"))
                    },
                )
            }
        }
    }

    fun logoutOwner() {
        _uiState.update { it.copy(auth = it.auth.copy(status = SyncStatus.Loading, message = "正在退出登录")) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    remoteClient().logoutOwner()
                }
            }
            store.clearSessionCookie()
            val deviceSnapshot = runCatching {
                withContext(Dispatchers.IO) {
                    fetchRemoteProgressSnapshot(remoteClient(), deviceId)
                }
            }
            _uiState.update { state ->
                val logoutSucceeded = result.isSuccess
                val authMessage = if (logoutSucceeded) {
                    "已退出登录，当前使用 deviceId 同步。"
                } else {
                    "本机登录态已清除；服务端退出失败：${result.exceptionOrNull()?.message.orEmpty()}"
                }
                deviceSnapshot.fold(
                    onSuccess = { snapshot ->
                        state.withRemoteProgressSnapshot(snapshot).copy(
                            auth = AuthState(
                                status = if (logoutSucceeded) SyncStatus.Success else SyncStatus.Error,
                                user = null,
                                message = "$authMessage 已读取设备进度 ${snapshot.progress.size} 条，复习 ${snapshot.review.size} 条。",
                            ),
                            sync = SyncSnapshot(
                                status = if (logoutSucceeded) SyncStatus.Success else SyncStatus.Error,
                                message = "已切回 deviceId 范围：进度 ${snapshot.progress.size} 条，复习 ${snapshot.review.size} 条",
                                lastSyncedAt = Instant.now().toString(),
                                remoteReviewCount = snapshot.review.size,
                                catalogUpdated = state.sync.catalogUpdated,
                            ),
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            auth = AuthState(
                                status = if (logoutSucceeded) SyncStatus.Success else SyncStatus.Error,
                                user = null,
                                message = "$authMessage 设备进度读取失败：${error.message ?: "网络不可用"}",
                            ),
                            sync = state.sync.copy(
                                status = SyncStatus.Error,
                                message = "已退出账号，但设备进度读取失败：${error.message ?: "网络不可用"}",
                            ),
                        )
                    },
                )
            }
        }
    }

    fun claimCurrentDevice() {
        _uiState.update { it.copy(auth = it.auth.copy(status = SyncStatus.Loading, message = "正在合并当前设备进度")) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    val merged = client.claimCurrentDevice(deviceId)
                    val snapshot = fetchRemoteProgressSnapshot(client, deviceId)
                    merged to snapshot
                }
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { (merged, snapshot) ->
                        state.withRemoteProgressSnapshot(snapshot).copy(
                            auth = state.auth.copy(
                                status = SyncStatus.Success,
                                message = "已合并：progress ${merged["progress"] ?: 0}，corrections ${merged["corrections"] ?: 0}，AI ${merged["aiInteractions"] ?: 0}；云端进度 ${snapshot.progress.size} 条。",
                            ),
                            sync = SyncSnapshot(
                                status = SyncStatus.Success,
                                message = "合并后已同步 ${snapshot.progress.size} 条进度，今日复习 ${snapshot.review.size} 条",
                                lastSyncedAt = Instant.now().toString(),
                                remoteReviewCount = snapshot.review.size,
                                catalogUpdated = state.sync.catalogUpdated,
                            ),
                        )
                    },
                    onFailure = { error ->
                        state.copy(auth = state.auth.copy(status = SyncStatus.Error, message = "合并失败：${error.message ?: "请先登录"}"))
                    },
                )
            }
        }
    }

    fun selectWork(workSlug: String) {
        val episodes = repository.episodes(workSlug)
        val rememberedEpisode = lastEpisodesByWork[workSlug]?.takeIf { episode ->
            episodes.any { it.episode == episode }
        }
        val episode = rememberedEpisode ?: episodes.firstOrNull()?.episode ?: 1
        applySelection(EpisodeSelection(workSlug = workSlug, episode = episode))
    }

    fun selectEpisode(episode: Int) {
        applySelection(_uiState.value.selection.copy(episode = episode))
    }

    fun startLesson() {
        _uiState.update {
            it.copy(
                selectedTab = LabTab.Lesson,
                activeSession = TrainingSessionKind.Lesson,
            )
        }
    }

    fun startLessonFromCurrentTab() {
        _uiState.update { state ->
            state.copy(
                activeSession = TrainingSessionKind.Lesson,
                libraryRevealEpisodeActionsRequest = state.libraryRevealEpisodeActionsRequest +
                    if (state.selectedTab == LabTab.Library) 1 else 0,
            )
        }
    }

    fun startLessonModeFromCurrentTab(mode: LessonMode) {
        _uiState.update { state ->
            val batch = 1
            val nodes = repository.buildLessonNodes(
                selection = state.selection,
                focus = state.focus,
                vocab = state.vocab,
                grammar = state.grammar,
                sentences = state.shadowing,
                mode = mode,
                batch = batch,
            )
            state.copy(
                activeSession = TrainingSessionKind.Lesson,
                lessonMode = mode,
                lessonBatch = batch,
                lessonTarget = null,
                hasNextLessonBatch = repository.hasNextLessonBatch(state.vocab, state.grammar, state.shadowing, mode, batch),
                focus = state.focus.copy(lessonTitle = lessonTitle(mode, state.focus, batch)),
                lesson = LessonEngine.start(nodes),
                sessionXp = 0,
                aiCoach = AiCoachState(),
                libraryRevealEpisodeActionsRequest = state.libraryRevealEpisodeActionsRequest +
                    if (state.selectedTab == LabTab.Library) 1 else 0,
            )
        }
    }

    fun selectLessonMode(mode: LessonMode) {
        _uiState.update { state ->
            val batch = 1
            val nodes = repository.buildLessonNodes(
                selection = state.selection,
                focus = state.focus,
                vocab = state.vocab,
                grammar = state.grammar,
                sentences = state.shadowing,
                mode = mode,
                batch = batch,
            )
            state.copy(
                lessonMode = mode,
                lessonBatch = batch,
                lessonTarget = null,
                hasNextLessonBatch = repository.hasNextLessonBatch(state.vocab, state.grammar, state.shadowing, mode, batch),
                focus = state.focus.copy(lessonTitle = lessonTitle(mode, state.focus, batch)),
                lesson = LessonEngine.start(nodes),
                sessionXp = 0,
                aiCoach = AiCoachState(),
            )
        }
    }

    fun startTargetLesson(target: LessonTarget) {
        _uiState.update { state ->
            val nodes = repository.buildLessonNodes(
                selection = state.selection,
                focus = state.focus,
                vocab = state.vocab,
                grammar = state.grammar,
                sentences = state.shadowing,
                mode = state.lessonMode,
                target = target,
            )
            state.copy(
                activeSession = TrainingSessionKind.Lesson,
                lessonTarget = target,
                lessonBatch = 1,
                hasNextLessonBatch = false,
                focus = state.focus.copy(lessonTitle = "单点训练 · ${target.labelFrom(state)}"),
                lesson = LessonEngine.start(nodes),
                sessionXp = 0,
                aiCoach = AiCoachState(),
            )
        }
    }

    fun startReadAirSession() {
        ensureFallbackReadAirCatalogLoaded()
        _uiState.update {
            it.copy(
                selectedTab = LabTab.ReadAir,
                activeSession = TrainingSessionKind.ReadAir,
                sessionXp = 0,
                readAir = it.readAir.copy(
                    reviewFocusExerciseId = null,
                    pinnedExerciseId = null,
                    restoreFiltersAfterSession = null,
                    aiCoach = AiCoachState(question = ReadAirAiQuestion),
                ),
            )
        }
    }

    fun startReadAirForCurrentEpisode() {
        _uiState.update { state ->
            val filters = ReadAirFilters(
                workSlug = state.selection.workSlug,
                episode = state.selection.episode,
            )
            val hasCurrentEpisodeExercises = state.readAir.exercises.any { exercise ->
                normalizeReadAirWorkSlug(exercise.workSlug) == normalizeReadAirWorkSlug(filters.workSlug) &&
                    exercise.episode == filters.episode
            }
            val exercises = if (hasCurrentEpisodeExercises) {
                state.readAir.exercises
            } else {
                mergeReadAirExercises(repository.allReadAirExercises(), state.readAir.exercises)
            }
            val readAir = state.readAir.copy(
                exercises = exercises,
                mode = ReadAirMode.Train,
                filters = filters,
                currentIndex = 0,
                reviewFocusExerciseId = null,
                pinnedExerciseId = null,
                restoreFiltersAfterSession = state.readAir.restoreFiltersAfterSession ?: state.readAir.filters,
                aiCoach = AiCoachState(question = ReadAirAiQuestion),
                usingFallback = state.readAir.usingFallback || !hasCurrentEpisodeExercises,
            )
            state.copy(
                activeSession = TrainingSessionKind.ReadAir,
                sessionXp = 0,
                readAir = readAir,
                libraryRevealEpisodeActionsRequest = state.libraryRevealEpisodeActionsRequest +
                    if (state.selectedTab == LabTab.Library) 1 else 0,
            )
        }
    }

    fun restartReadAirSession() {
        _uiState.update { state ->
            val scopedIds = state.readAir.scopedExercises.map { it.id }.toSet()
            state.copy(
                activeSession = TrainingSessionKind.ReadAir,
                sessionXp = 0,
                readAir = state.readAir.copy(
                    selectedAnswers = state.readAir.selectedAnswers.filterKeys { it !in scopedIds },
                    currentIndex = 0,
                    pinnedExerciseId = null,
                    aiCoach = AiCoachState(question = ReadAirAiQuestion),
                ),
            )
        }
    }

    fun exitTrainingSession() {
        _uiState.update { state ->
            val exitingReadAir = state.activeSession == TrainingSessionKind.ReadAir
            val restoreFilters = state.readAir.restoreFiltersAfterSession
                .takeIf { exitingReadAir }
            state.copy(
                activeSession = null,
                readAir = if (exitingReadAir) {
                    state.readAir.copy(
                        filters = restoreFilters ?: state.readAir.filters,
                        currentIndex = 0,
                        reviewFocusExerciseId = null,
                        pinnedExerciseId = null,
                        restoreFiltersAfterSession = null,
                        aiCoach = AiCoachState(question = ReadAirAiQuestion),
                    )
                } else {
                    state.readAir
                },
            )
        }
    }

    fun startNextLessonBatch() {
        _uiState.update { state ->
            if (!state.hasNextLessonBatch || state.lessonTarget != null) return@update state
            val nextBatch = state.lessonBatch + 1
            val nodes = repository.buildLessonNodes(
                selection = state.selection,
                focus = state.focus,
                vocab = state.vocab,
                grammar = state.grammar,
                sentences = state.shadowing,
                mode = state.lessonMode,
                batch = nextBatch,
            )
            state.copy(
                lessonBatch = nextBatch,
                hasNextLessonBatch = repository.hasNextLessonBatch(
                    vocab = state.vocab,
                    grammar = state.grammar,
                    sentences = state.shadowing,
                    mode = state.lessonMode,
                    batch = nextBatch,
                ),
                focus = state.focus.copy(lessonTitle = lessonTitle(state.lessonMode, state.focus, nextBatch)),
                lesson = LessonEngine.start(nodes),
                sessionXp = 0,
                aiCoach = AiCoachState(),
            )
        }
    }

    fun submitAnswer(selected: String) {
        var syncPayload: SyncAnswer? = null
        _uiState.update { state ->
            val nextLesson = LessonEngine.answer(state.lesson, selected)
            val feedback = nextLesson.feedback ?: return@update state.copy(lesson = nextLesson)
            val answeredNow = nextLesson.answered > state.lesson.answered
            if (!answeredNow) return@update state.copy(lesson = nextLesson)

            val node = state.lesson.currentNode ?: return@update state.copy(lesson = nextLesson)
            val correct = feedback.correct
            val nextMistakes = if (correct) {
                state.mistakes.filterNot { it.itemId == node.id }
            } else {
                upsertMistake(state.mistakes, node, selected, feedback.expected, feedback.explanation, state.selection)
            }
            store.writeMistakes(nextMistakes)
            syncPayload = SyncAnswer(
                itemId = node.id,
                itemType = node.progressType(),
                selection = state.selection,
                state = if (correct) ReviewState.Good else ReviewState.Bad,
                label = node.prompt.take(90),
            )

            state.copy(
                lesson = nextLesson,
                sessionXp = state.sessionXp + if (correct) 12 else 0,
                focus = state.focus.copy(energy = (state.focus.energy + if (correct) 0 else -1).coerceIn(0, 5)),
                mistakes = nextMistakes,
            )
        }
        syncPayload?.let { payload ->
            if (_uiState.value.settings.cloudSync) {
                syncAnswer(payload)
            }
        }
    }

    fun continueLesson() {
        _uiState.update { state ->
            state.copy(lesson = LessonEngine.continueAfterFeedback(state.lesson))
        }
    }

    fun restartLesson() {
        _uiState.update { it.copy(lesson = LessonEngine.restart(it.lesson), sessionXp = 0) }
    }

    fun selectScene(sceneId: String) {
        _uiState.update { state ->
            val selected = state.scenes.firstOrNull { it.id == sceneId } ?: state.selectedScene
            state.copy(selectedScene = selected, readAirAnswer = repository.answerReadAir(state.readAirQuestion, selected))
        }
    }

    fun updateReadAirQuestion(question: String) {
        _uiState.update { it.copy(readAirQuestion = question) }
    }

    fun askReadAir() {
        _uiState.update { state ->
            state.copy(readAirAnswer = repository.answerReadAir(state.readAirQuestion, state.selectedScene))
        }
    }

    fun refreshReadAirExercises() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                readAir = it.readAir.copy(
                    status = SyncStatus.Loading,
                    message = "正在更新读空气练习",
                ),
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    remoteClient().fetchLinguisticExercises()
                }
            }
            val fallback = if (result.isFailure || result.getOrNull().orEmpty().isEmpty()) {
                withContext(Dispatchers.Default) {
                    repository.allReadAirExercises()
                }
            } else {
                emptyList()
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { remoteExercises ->
                        val exercises = remoteExercises.ifEmpty { fallback }
                        current.copy(
                            readAir = current.readAir.copy(
                                status = SyncStatus.Success,
                                message = if (remoteExercises.isEmpty()) {
                                    "当前章节暂无云端练习，继续使用样例题。"
                                } else {
                                    "已更新 ${remoteExercises.size} 道读空气练习。"
                                },
                                exercises = exercises,
                                usingFallback = remoteExercises.isEmpty(),
                                currentIndex = 0,
                                reviewFocusExerciseId = null,
                                selectedAnswers = current.readAir.selectedAnswers.filterKeys { id ->
                                    exercises.any { it.id == id }
                                },
                                pinnedExerciseId = null,
                                aiCoach = AiCoachState(question = ReadAirAiQuestion),
                            ),
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            readAir = current.readAir.copy(
                                status = SyncStatus.Error,
                                message = "练习更新失败：${error.message ?: "网络不可用"}；继续使用现有题目。",
                                exercises = current.readAir.exercises.ifEmpty { fallback },
                                usingFallback = current.readAir.usingFallback || current.readAir.exercises.isEmpty(),
                            ),
                        )
                    },
                )
            }
        }
    }

    fun selectReadAirDomain(domain: String) {
        _uiState.update { state ->
            state.copy(
                readAir = state.readAir.copy(
                    filters = state.readAir.filters.copy(
                        domain = domain,
                        phenomenonKey = ReadAirAllFilter,
                    ),
                    currentIndex = 0,
                    reviewFocusExerciseId = null,
                    restoreFiltersAfterSession = null,
                ),
            )
        }
    }

    fun selectReadAirMode(mode: ReadAirMode) {
        _uiState.update { state ->
            state.copy(
                readAir = state.readAir.copy(
                    mode = mode,
                    currentIndex = 0,
                    reviewFocusExerciseId = null,
                    restoreFiltersAfterSession = null,
                    aiCoach = AiCoachState(question = ReadAirAiQuestion),
                ),
            )
        }
    }

    fun selectReadAirWork(workSlug: String) {
        _uiState.update { state ->
            state.copy(
                readAir = state.readAir.copy(
                    filters = state.readAir.filters.copy(
                        workSlug = workSlug,
                        episode = null,
                    ),
                    currentIndex = 0,
                    reviewFocusExerciseId = null,
                    restoreFiltersAfterSession = null,
                ),
            )
        }
    }

    fun selectReadAirPhenomenon(phenomenonKey: String) {
        _uiState.update { state ->
            state.copy(
                readAir = state.readAir.copy(
                    filters = state.readAir.filters.copy(phenomenonKey = phenomenonKey),
                    currentIndex = 0,
                    reviewFocusExerciseId = null,
                    restoreFiltersAfterSession = null,
                ),
            )
        }
    }

    fun selectReadAirQuestionType(questionType: String) {
        _uiState.update { state ->
            state.copy(
                readAir = state.readAir.copy(
                    filters = state.readAir.filters.copy(questionType = questionType),
                    currentIndex = 0,
                    reviewFocusExerciseId = null,
                    restoreFiltersAfterSession = null,
                ),
            )
        }
    }

    fun selectReadAirDifficulty(difficulty: String) {
        _uiState.update { state ->
            state.copy(
                readAir = state.readAir.copy(
                    filters = state.readAir.filters.copy(difficulty = difficulty),
                    currentIndex = 0,
                    reviewFocusExerciseId = null,
                    restoreFiltersAfterSession = null,
                ),
            )
        }
    }

    fun selectReadAirEpisode(episode: Int?) {
        _uiState.update { state ->
            state.copy(
                readAir = state.readAir.copy(
                    filters = state.readAir.filters.copy(episode = episode),
                    currentIndex = 0,
                    reviewFocusExerciseId = null,
                    restoreFiltersAfterSession = null,
                ),
            )
        }
    }

    fun resetReadAirFilters() {
        _uiState.update { state ->
            state.copy(
                readAir = state.readAir.copy(
                    filters = ReadAirFilters(),
                    currentIndex = 0,
                    reviewFocusExerciseId = null,
                    pinnedExerciseId = null,
                    restoreFiltersAfterSession = null,
                ),
            )
        }
    }

    fun resetReadAirQueue() {
        _uiState.update { state ->
            state.copy(
                readAir = state.readAir.resetScopedQueue(),
            )
        }
    }

    fun selectReadAirAnswer(option: String) {
        var syncPayload: SyncAnswer? = null
        _uiState.update { state ->
            val exercise = state.readAir.currentExercise ?: return@update state
            val correct = exercise.isCorrect(option)
            val answeredBefore = state.readAir.selectedAnswers.containsKey(exercise.id)
            val nextMistakes = if (correct) {
                state.mistakes.filterNot { it.itemId == exercise.id }
            } else {
                upsertReadAirMistake(state.mistakes, exercise, option, selectionForExercise(exercise, state.selection))
            }
            store.writeMistakes(nextMistakes)
            syncPayload = SyncAnswer(
                itemId = exercise.id,
                itemType = "exercise",
                selection = selectionForExercise(exercise, state.selection),
                state = if (correct) ReviewState.Good else ReviewState.Bad,
                label = exercise.prompt.take(90),
                payload = buildLinguisticProgressPayload(exercise, option),
            )
            state.copy(
                readAir = state.readAir.copy(
                    selectedAnswers = state.readAir.selectedAnswers + (exercise.id to option),
                    pinnedExerciseId = exercise.id,
                ),
                sessionXp = state.sessionXp + if (correct && !answeredBefore) 8 else 0,
                focus = state.focus.copy(energy = (state.focus.energy + if (correct || answeredBefore) 0 else -1).coerceIn(0, 5)),
                mistakes = nextMistakes,
            )
        }
        syncPayload?.let { payload ->
            if (_uiState.value.settings.cloudSync) {
                syncAnswer(payload)
            }
        }
    }

    fun selectReadAirBrowseAnswer(exerciseId: String, option: String) {
        _uiState.update { state ->
            if (state.readAir.exercises.none { it.id == exerciseId }) return@update state
            state.copy(
                readAir = state.readAir.copy(
                    browseAnswers = state.readAir.browseAnswers + (exerciseId to option),
                ),
            )
        }
    }

    fun nextReadAirExercise() {
        _uiState.update { state ->
            state.copy(readAir = state.readAir.advanceAfterCurrentAnswer())
        }
    }

    fun askAiAboutReadAirExercise() {
        val state = _uiState.value
        val exercise = state.readAir.currentExercise ?: return
        val selected = state.readAir.selectedAnswerFor(exercise.id)
        _uiState.update {
            it.copy(readAir = it.readAir.copy(aiCoach = it.readAir.aiCoach.copy(status = SyncStatus.Loading, answer = "", result = null)))
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    remoteClient().askAi(
                        deviceId = state.deviceId,
                        model = state.settings.aiModel,
                        reasoningEffort = state.settings.reasoningEffort,
                        kind = "linguistic",
                        text = exercise.jaText.ifBlank { exercise.prompt },
                        context = buildReadAirAiContext(exercise, selected),
                    )
                }
            }
            _uiState.update {
                it.copy(
                    readAir = it.readAir.copy(
                        aiCoach = it.readAir.aiCoach.copy(
                            status = if (result.isSuccess) SyncStatus.Success else SyncStatus.Error,
                            answer = result.fold(
                                onSuccess = { aiResult -> aiResult.text },
                                onFailure = { error -> "AI 请求失败：${error.message ?: "未知错误"}" },
                            ),
                            result = result.getOrNull(),
                        ),
                    ),
                )
            }
        }
    }

    fun updateAiQuestion(question: String) {
        _uiState.update { it.copy(aiCoach = it.aiCoach.copy(question = question)) }
    }

    fun askAiAboutCurrentNode() {
        val state = _uiState.value
        val node = state.lesson.currentNode ?: return
        _uiState.update { it.copy(aiCoach = it.aiCoach.copy(status = SyncStatus.Loading, answer = "", result = null)) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    remoteClient().askAi(
                        deviceId = state.deviceId,
                        model = state.settings.aiModel,
                        reasoningEffort = state.settings.reasoningEffort,
                        kind = node.aiKind(),
                        text = node.aiText(),
                        context = buildAiContext(node, state.aiCoach.question),
                    )
                }
            }
            _uiState.update {
                it.copy(
                    aiCoach = it.aiCoach.copy(
                        status = if (result.isSuccess) SyncStatus.Success else SyncStatus.Error,
                        answer = result.fold(
                            onSuccess = { aiResult -> aiResult.text },
                            onFailure = { error -> "AI 请求失败：${error.message ?: "未知错误"}" },
                        ),
                        result = result.getOrNull(),
                    ),
                )
            }
        }
    }

    fun askAiAboutLibraryItem(targetKey: String, kind: String, text: String, context: String) {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                libraryAiTargetKey = targetKey,
                aiCoach = it.aiCoach.copy(status = SyncStatus.Loading, answer = "", result = null),
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    remoteClient().askAi(
                        deviceId = state.deviceId,
                        model = state.settings.aiModel,
                        reasoningEffort = state.settings.reasoningEffort,
                        kind = kind,
                        text = text,
                        context = context,
                    )
                }
            }
            _uiState.update {
                it.copy(
                    aiCoach = it.aiCoach.copy(
                        status = if (result.isSuccess) SyncStatus.Success else SyncStatus.Error,
                        answer = result.fold(
                            onSuccess = { aiResult -> aiResult.text },
                            onFailure = { error -> "AI 请求失败：${error.message ?: "未知错误"}" },
                        ),
                        result = result.getOrNull(),
                    ),
                )
            }
        }
    }

    fun refreshFromServer() {
        val state = _uiState.value
        _uiState.update {
            it.copy(sync = it.sync.copy(status = SyncStatus.Loading, message = "正在更新课程和当前集资料"))
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    val works = normalizeEpisodeCounts(client.fetchWorks().ifEmpty { repository.works() })
                    val episodes = normalizeEpisodes(
                        workSlug = state.selection.workSlug,
                        episodes = client.fetchEpisodes(state.selection.workSlug).ifEmpty { repository.episodes(state.selection.workSlug) },
                        works = works,
                    )
                    val payload = client.fetchEpisodePayload(state.selection)
                    val readAirExercises = client.fetchLinguisticExercises()
                    val fallbackReadAirExercises = if (readAirExercises.isEmpty()) {
                        repository.allReadAirExercises()
                    } else {
                        emptyList()
                    }
                    val content = repository.contentFromRemote(
                        selection = state.selection,
                        vocab = payload.vocab,
                        grammar = payload.grammar,
                        shadowing = payload.shadowing,
                        mode = state.lessonMode,
                        batch = state.lessonBatch,
                    )
                    RemoteRefresh(works, episodes, content, readAirExercises, fallbackReadAirExercises)
                }
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { remote ->
                        val readAirExercises = remote.readAirExercises.ifEmpty { remote.fallbackReadAirExercises }
                        current.copy(
                            works = remote.works,
                            episodes = remote.episodes,
                            focus = remote.content.focus,
                            vocab = remote.content.vocab,
                            grammar = remote.content.grammar,
                            shadowing = remote.content.shadowing,
                            scenes = remote.content.scenes,
                            selectedScene = remote.content.scenes.first(),
                            readAir = current.readAir.copy(
                                status = SyncStatus.Success,
                                message = if (remote.readAirExercises.isEmpty()) {
                                    "资料已更新；当前集暂无云端读空气题，使用样例题。"
                                } else {
                                    "资料已更新；读空气题库 ${remote.readAirExercises.size} 道。"
                                },
                                exercises = readAirExercises,
                                usingFallback = remote.readAirExercises.isEmpty(),
                                currentIndex = 0,
                                selectedAnswers = current.readAir.selectedAnswers.filterKeys { id ->
                                    readAirExercises.any { it.id == id }
                                },
                                pinnedExerciseId = null,
                                aiCoach = AiCoachState(question = ReadAirAiQuestion),
                            ),
                            lesson = LessonEngine.start(remote.content.lessonNodes),
                            lessonTarget = null,
                            hasNextLessonBatch = repository.hasNextLessonBatch(
                                vocab = remote.content.vocab,
                                grammar = remote.content.grammar,
                                sentences = remote.content.shadowing,
                                mode = current.lessonMode,
                                batch = current.lessonBatch,
                            ),
                            sync = SyncSnapshot(
                                status = SyncStatus.Success,
                                message = "已更新作品目录和 ${remote.content.focus.episodeLabel}",
                                lastSyncedAt = Instant.now().toString(),
                                catalogUpdated = true,
                                remoteReviewCount = current.sync.remoteReviewCount,
                            ),
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            sync = current.sync.copy(
                                status = SyncStatus.Error,
                                message = "更新失败：${error.message ?: "网络不可用"}",
                            ),
                        )
                    },
                )
            }
        }
    }

    fun syncProgressNow() {
        val state = _uiState.value
        _uiState.update { it.copy(sync = it.sync.copy(status = SyncStatus.Loading, message = "正在同步进度和今日复习")) }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    val user = client.fetchAuthMe()
                    user to fetchRemoteProgressSnapshot(client, state.deviceId)
                }
            }
            if (result.isSuccess && result.getOrNull()?.first == null) {
                store.clearSessionCookie()
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { (user, snapshot) ->
                        val scope = if (user == null) "deviceId" else "账号"
                        current.withRemoteProgressSnapshot(snapshot).copy(
                            auth = current.auth.copy(
                                status = SyncStatus.Success,
                                user = user,
                                message = if (user == null) {
                                    "未登录或登录态已过期；当前使用 deviceId 读取进度 ${snapshot.progress.size} 条，复习 ${snapshot.review.size} 条。"
                                } else {
                                    "已登录：${user.email}；当前按账号读取进度 ${snapshot.progress.size} 条，复习 ${snapshot.review.size} 条。"
                                },
                            ),
                            sync = SyncSnapshot(
                                status = SyncStatus.Success,
                                message = "已同步 $scope 范围 ${snapshot.progress.size} 条进度，今日复习 ${snapshot.review.size} 条",
                                lastSyncedAt = Instant.now().toString(),
                                remoteReviewCount = snapshot.review.size,
                                catalogUpdated = current.sync.catalogUpdated,
                            ),
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            sync = current.sync.copy(
                                status = SyncStatus.Error,
                                message = "同步失败：${error.message ?: "网络不可用"}",
                            ),
                        )
                    },
                )
            }
        }
    }

    fun markMistakeReviewed(itemId: String) {
        _uiState.update { state ->
            val nextMistakes = state.mistakes.filterNot { it.itemId == itemId }
            store.writeMistakes(nextMistakes)
            state.copy(mistakes = nextMistakes)
        }
    }

    fun practiceWeakestReviewItem() {
        val state = _uiState.value
        val localMistake = state.mistakes.firstOrNull()
        if (localMistake != null) {
            practiceLocalMistake(localMistake.itemId)
            return
        }
        val remoteTask = state.reviewTasks.firstOrNull()
        if (remoteTask != null) {
            practiceReviewTask(remoteTask)
            return
        }
        selectTab(LabTab.Lesson)
    }

    fun practiceLocalMistake(itemId: String) {
        val mistake = _uiState.value.mistakes.firstOrNull { it.itemId == itemId }
        if (mistake == null) {
            selectTab(LabTab.Lesson)
            return
        }
        if (mistake.typeLabel == "读空气") {
            startReviewReadAir(
                ProgressItem(
                    itemId = mistake.itemId,
                    itemType = "exercise",
                    workSlug = mistake.workSlug,
                    episode = mistake.episode,
                    state = mistake.lastState,
                    label = mistake.prompt,
                ),
            )
            return
        }

        val state = _uiState.value
        val selection = EpisodeSelection(
            workSlug = mistake.workSlug.ifBlank { state.selection.workSlug },
            episode = mistake.episode.takeIf { it > 0 } ?: state.selection.episode,
        )
        val reviewNode = findReviewLessonNode(selection, state, mistake.itemId)
        if (reviewNode == null) {
            _uiState.update {
                it.copy(
                    selectedTab = LabTab.Lesson,
                    sync = it.sync.copy(message = "这条错题还没有本机训练卡；请先更新资料。"),
                )
            }
            return
        }

        val target = when (reviewNode.sourceKind) {
            "vocab" -> LessonTarget.Vocab(reviewNode.sourceId)
            "grammar" -> LessonTarget.Grammar(reviewNode.sourceId)
            "sentence" -> LessonTarget.Sentence(reviewNode.sourceId)
            else -> null
        }
        if (target == null) {
            _uiState.update {
                it.copy(
                    selectedTab = LabTab.Lesson,
                    sync = it.sync.copy(message = "这条错题暂时只能从普通训练里复习。"),
                )
            }
            return
        }
        startReviewLesson(
            ProgressItem(
                itemId = reviewNode.sourceId,
                itemType = reviewNode.sourceKind,
                workSlug = selection.workSlug,
                episode = selection.episode,
                state = mistake.lastState,
                label = mistake.prompt,
            ),
            target,
        )
    }

    fun practiceReviewTask(task: ProgressItem) {
        when (task.itemType) {
            "vocab" -> startReviewLesson(task, LessonTarget.Vocab(task.itemId))
            "grammar" -> startReviewLesson(task, LessonTarget.Grammar(task.itemId))
            "sentence" -> startReviewLesson(task, LessonTarget.Sentence(task.itemId))
            "exercise" -> startReviewReadAir(task)
            else -> selectTab(LabTab.Lesson)
        }
    }

    fun updateSettings(settings: LabSettings) {
        store.writeSettings(settings)
        _uiState.update { it.copy(settings = settings) }
    }

    private fun findReviewLessonNode(
        selection: EpisodeSelection,
        state: LabUiState,
        itemId: String,
    ): LessonNode? {
        val modes = listOf(state.lessonMode, LessonMode.Mixed, LessonMode.Vocab, LessonMode.Grammar, LessonMode.Shadowing).distinct()
        return modes.firstNotNullOfOrNull { mode ->
            val sameSelection = selection == state.selection
            val content = if (sameSelection) {
                EpisodeContentSnapshot(
                    focus = state.focus,
                    vocab = state.vocab,
                    grammar = state.grammar,
                    shadowing = state.shadowing,
                    scenes = state.scenes,
                )
            } else {
                val remoteContent = repository.content(selection, mode)
                EpisodeContentSnapshot(
                    focus = remoteContent.focus,
                    vocab = remoteContent.vocab,
                    grammar = remoteContent.grammar,
                    shadowing = remoteContent.shadowing,
                    scenes = remoteContent.scenes,
                )
            }
            repository.buildLessonNodes(
                selection = selection,
                focus = content.focus,
                vocab = content.vocab,
                grammar = content.grammar,
                sentences = content.shadowing,
                mode = mode,
            ).firstOrNull { it.id == itemId }
        }
    }

    private fun startReviewLesson(task: ProgressItem, target: LessonTarget) {
        _uiState.update { state ->
            val selection = task.selectionOrFallback(state.selection)
            val sameSelection = selection == state.selection
            val content = if (sameSelection) {
                EpisodeContentSnapshot(
                    focus = state.focus,
                    vocab = state.vocab,
                    grammar = state.grammar,
                    shadowing = state.shadowing,
                    scenes = state.scenes,
                )
            } else {
                val remoteContent = repository.content(selection, state.lessonMode)
                EpisodeContentSnapshot(
                    focus = remoteContent.focus,
                    vocab = remoteContent.vocab,
                    grammar = remoteContent.grammar,
                    shadowing = remoteContent.shadowing,
                    scenes = remoteContent.scenes,
                )
            }
            val nodes = repository.buildLessonNodes(
                selection = selection,
                focus = content.focus,
                vocab = content.vocab,
                grammar = content.grammar,
                sentences = content.shadowing,
                mode = state.lessonMode,
                target = target,
            )
            if (nodes.isEmpty()) {
                return@update state.copy(
                    sync = state.sync.copy(message = "这条复习内容还没有本机训练卡；请先同步当前集资料。"),
                )
            }
            state.copy(
                selectedTab = LabTab.Lesson,
                activeSession = TrainingSessionKind.Lesson,
                selection = selection,
                episodes = if (sameSelection) state.episodes else repository.episodes(selection.workSlug),
                focus = content.focus.copy(lessonTitle = "复习训练 · ${task.label.ifBlank { target.labelFromContent(content) }}"),
                vocab = content.vocab,
                grammar = content.grammar,
                shadowing = content.shadowing,
                scenes = content.scenes,
                selectedScene = content.scenes.firstOrNull() ?: state.selectedScene,
                readAir = if (sameSelection) {
                    state.readAir
                } else {
                    state.readAir.copy(
                        exercises = mergeReadAirExercises(
                            state.readAir.exercises,
                            repository.readAirExercises(selection),
                        ),
                        message = "已切到复习所属章节；读空气入口保留全题库筛选。",
                        reviewFocusExerciseId = null,
                        pinnedExerciseId = null,
                    )
                },
                lessonTarget = target,
                lessonBatch = 1,
                hasNextLessonBatch = false,
                lesson = LessonEngine.start(nodes),
                sessionXp = 0,
                aiCoach = AiCoachState(),
            )
        }
    }

    private fun startReviewReadAir(task: ProgressItem) {
        ensureFallbackReadAirCatalogLoaded()
        if (startReviewReadAirFromLoadedCatalog(task, showMissingMessage = false)) return

        _uiState.update { state ->
            state.copy(
                selectedTab = LabTab.Review,
                activeSession = null,
                sync = state.sync.copy(
                    status = SyncStatus.Loading,
                    message = "正在从云端匹配这条读空气复习题。",
                ),
            )
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    remoteClient().fetchLinguisticExercises()
                }
            }
            result.onSuccess { remoteExercises ->
                _uiState.update { state ->
                    state.copy(
                        readAir = state.readAir.copy(
                            exercises = mergeReadAirExercises(remoteExercises, state.readAir.exercises),
                            status = SyncStatus.Success,
                            message = "已补齐云端读空气题库，正在进入复习题。",
                            usingFallback = remoteExercises.isEmpty() && state.readAir.usingFallback,
                        ),
                        sync = state.sync.copy(
                            status = SyncStatus.Success,
                            message = if (remoteExercises.isEmpty()) {
                                "云端暂时没有返回读空气题库；继续使用本机题库匹配。"
                            } else {
                                "已补齐云端读空气题库 ${remoteExercises.size} 道。"
                            },
                        ),
                    )
                }
                startReviewReadAirFromLoadedCatalog(task, showMissingMessage = true)
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        selectedTab = LabTab.Review,
                        activeSession = null,
                        sync = state.sync.copy(
                            status = SyncStatus.Error,
                            message = "这条读空气复习题还没在本机题库里，云端匹配失败：${error.message ?: "网络不可用"}",
                        ),
                    )
                }
            }
        }
    }

    private fun startReviewReadAirFromLoadedCatalog(
        task: ProgressItem,
        showMissingMessage: Boolean,
    ): Boolean {
        val snapshot = _uiState.value
        val fallbackSelection = task.selectionOrFallback(snapshot.selection)
        val catalogExercises = mergeReadAirExercises(
            snapshot.readAir.exercises,
            repository.readAirExercises(fallbackSelection),
        )
        val exercise = findReviewReadAirExercise(task, catalogExercises)
        if (exercise == null) {
            if (showMissingMessage) {
                _uiState.update { state ->
                    state.copy(
                        selectedTab = LabTab.Review,
                        activeSession = null,
                        sync = state.sync.copy(
                            status = SyncStatus.Error,
                            message = "这条读空气复习题还没在本机题库里；请先更新资料后再试。",
                        ),
                    )
                }
            }
            return false
        }

        _uiState.update { state ->
            val catalogExercises = mergeReadAirExercises(
                state.readAir.exercises,
                listOf(exercise),
            )
            val filters = ReadAirFilters(
                workSlug = exercise.workSlug.ifBlank { task.workSlug.ifBlank { ReadAirAllFilter } },
                domain = exercise.domain.ifBlank { ReadAirAllFilter },
                phenomenonKey = exercise.phenomenonKey.ifBlank { ReadAirAllFilter },
                questionType = exercise.questionType.ifBlank { ReadAirAllFilter },
                difficulty = exercise.difficulty.ifBlank { ReadAirAllFilter },
                episode = exercise.episode.takeIf { it > 0 },
            )
            val nextExercises = if (catalogExercises.any { it.id == exercise.id }) {
                catalogExercises
            } else {
                listOf(exercise) + catalogExercises
            }
            val nextReadAir = state.readAir.copy(
                exercises = nextExercises,
                mode = ReadAirMode.Train,
                filters = filters,
                selectedAnswers = state.readAir.selectedAnswers - exercise.id,
                reviewFocusExerciseId = exercise.id,
                pinnedExerciseId = exercise.id,
                restoreFiltersAfterSession = state.readAir.restoreFiltersAfterSession ?: state.readAir.filters,
                currentIndex = 0,
                aiCoach = AiCoachState(question = ReadAirAiQuestion),
                message = "已打开错题复习：${task.label.ifBlank { exercise.prompt }}",
            )
            val pinnedIndex = nextReadAir.filteredExercises.indexOfFirst { it.id == exercise.id }.coerceAtLeast(0)
            state.copy(
                selectedTab = LabTab.Review,
                activeSession = TrainingSessionKind.ReadAir,
                sessionXp = 0,
                readAir = nextReadAir.copy(currentIndex = pinnedIndex),
            )
        }
        return true
    }

    private fun applySelection(selection: EpisodeSelection) {
        val mode = _uiState.value.lessonMode
        val batch = 1
        val content = repository.content(selection, mode, batch)
        val selectedEpisodeReadAirExercises = repository.readAirExercises(selection)
        lastEpisodesByWork[selection.workSlug] = selection.episode
        store.writeSelection(selection)
        store.writeLastEpisodeForWork(selection)
        _uiState.update { state ->
            val readAirExercises = mergeReadAirExercises(
                state.readAir.exercises,
                selectedEpisodeReadAirExercises,
            )
            state.copy(
                selection = selection,
                episodes = repository.episodes(selection.workSlug),
                focus = content.focus,
                vocab = content.vocab,
                grammar = content.grammar,
                shadowing = content.shadowing,
                scenes = content.scenes,
                selectedScene = content.scenes.first(),
                readAir = state.readAir.copy(
                    exercises = readAirExercises,
                message = "已切换章节；读空气题库保留 ${readAirExercises.size} 道，可从云端更新。",
                currentIndex = 0,
                reviewFocusExerciseId = null,
                pinnedExerciseId = null,
                restoreFiltersAfterSession = null,
                aiCoach = AiCoachState(question = ReadAirAiQuestion),
            ),
                lessonMode = mode,
                lessonBatch = batch,
                lessonTarget = null,
                hasNextLessonBatch = repository.hasNextLessonBatch(content.vocab, content.grammar, content.shadowing, mode, batch),
                lesson = LessonEngine.start(content.lessonNodes),
                sessionXp = 0,
                readAirAnswer = "",
                aiCoach = AiCoachState(),
                activeSession = null,
            )
        }
    }

    private fun syncAnswer(payload: SyncAnswer) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    try {
                        ProgressSyncResult(
                            item = client.saveProgress(
                                deviceId = deviceId,
                                itemId = payload.itemId,
                                itemType = payload.itemType,
                                selection = payload.selection,
                                state = payload.state,
                                label = payload.label,
                                payload = payload.payload,
                            ),
                        )
                    } catch (error: Throwable) {
                        if (!error.isProgressDuplicateConflict()) throw error
                        runCatching { client.claimCurrentDevice(deviceId) }
                        val existing = runCatching {
                            client.fetchProgress(deviceId).firstOrNull { it.itemId == payload.itemId }
                        }.getOrNull()
                        ProgressSyncResult(
                            item = existing ?: payload.toProgressItem(),
                            recoveredDuplicate = true,
                        )
                    }
                }
            }
            result.onSuccess { synced ->
                _uiState.update { state ->
                    val nextProgressItems = listOf(synced.item) + state.progressItems.filterNot { it.itemId == synced.item.itemId }
                    val nextAuth = state.auth.user?.let { user ->
                        state.auth.copy(
                            status = SyncStatus.Success,
                            message = "已登录：${user.email}；本机已保存进度 ${nextProgressItems.size} 条，复习 ${state.reviewTasks.size} 条。",
                        )
                    } ?: state.auth
                    state.copy(
                        progressItems = nextProgressItems,
                        auth = nextAuth,
                        sync = state.sync.copy(
                            status = SyncStatus.Success,
                            message = if (synced.recoveredDuplicate) {
                                "进度已存在；已按本机记录继续并尝试合并：${synced.item.label}"
                            } else {
                                "已保存进度：${synced.item.label}"
                            },
                            lastSyncedAt = Instant.now().toString(),
                        ),
                    )
                }
            }
            result.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        sync = state.sync.copy(
                            status = SyncStatus.Error,
                            message = "进度保存失败：${error.message ?: "网络不可用"}",
                        ),
                    )
                }
            }
        }
    }
}

data class LabUiState(
    val deviceId: String,
    val settings: LabSettings,
    val selectedTab: LabTab = LabTab.Today,
    val activeSession: TrainingSessionKind? = null,
    val libraryRevealEpisodeActionsRequest: Int = 0,
    val works: List<WorkOption>,
    val episodes: List<EpisodeOption>,
    val selection: EpisodeSelection,
    val focus: EpisodeFocus,
    val vocab: List<VocabItem>,
    val grammar: List<GrammarPoint>,
    val shadowing: List<ShadowingSentence>,
    val scenes: List<ReadAirScene>,
    val selectedScene: ReadAirScene,
    val readAir: ReadAirTrainingState,
    val lesson: LessonSession,
    val lessonMode: LessonMode,
    val lessonBatch: Int = 1,
    val lessonTarget: LessonTarget? = null,
    val hasNextLessonBatch: Boolean = false,
    val sessionXp: Int = 0,
    val readAirQuestion: String = "这句话是在表达字面意思，还是在调整关系和语气？",
    val readAirAnswer: String = "",
    val mistakes: List<MistakeRecord> = emptyList(),
    val progressItems: List<ProgressItem> = emptyList(),
    val reviewTasks: List<ProgressItem> = emptyList(),
    val auth: AuthState = AuthState(),
    val sync: SyncSnapshot = SyncSnapshot(),
    val aiCoach: AiCoachState = AiCoachState(),
    val libraryAiTargetKey: String? = null,
)

data class AuthState(
    val status: SyncStatus = SyncStatus.Idle,
    val user: AuthUser? = null,
    val message: String = "未检查账号状态",
)

data class ReadAirTrainingState(
    val status: SyncStatus = SyncStatus.Idle,
    val message: String = "",
    val exercises: List<LinguisticExercise> = emptyList(),
    val mode: ReadAirMode = ReadAirMode.Train,
    val filters: ReadAirFilters = ReadAirFilters(),
    val restoreFiltersAfterSession: ReadAirFilters? = null,
    val currentIndex: Int = 0,
    val selectedAnswers: Map<String, String> = emptyMap(),
    val browseAnswers: Map<String, String> = emptyMap(),
    val reviewFocusExerciseId: String? = null,
    val pinnedExerciseId: String? = null,
    val aiCoach: AiCoachState = AiCoachState(question = ReadAirAiQuestion),
    val usingFallback: Boolean = false,
) {
    val scopedExercises: List<LinguisticExercise>
        get() = exercises.filter { exercise ->
            val workMatch = filters.workSlug == ReadAirAllFilter ||
                normalizeReadAirWorkSlug(exercise.workSlug) == normalizeReadAirWorkSlug(filters.workSlug)
            val domainMatch = filters.domain == ReadAirAllFilter || exercise.domain == filters.domain
            val phenomenonMatch = filters.phenomenonKey == ReadAirAllFilter || exercise.phenomenonKey == filters.phenomenonKey
            val questionTypeMatch = filters.questionType == ReadAirAllFilter || exercise.questionType == filters.questionType
            val difficultyMatch = filters.difficulty == ReadAirAllFilter || exercise.difficulty == filters.difficulty
            val episodeMatch = filters.episode == null || exercise.episode == filters.episode
            val focusMatch = reviewFocusExerciseId == null || exercise.id == reviewFocusExerciseId
            workMatch && domainMatch && phenomenonMatch && questionTypeMatch && difficultyMatch && episodeMatch && focusMatch
        }

    val filteredExercises: List<LinguisticExercise>
        get() = scopedExercises.filter { exercise ->
            selectedAnswers[exercise.id].isNullOrBlank() || exercise.id == pinnedExerciseId
        }

    val answeredScopedCount: Int
        get() = scopedExercises.count { exercise -> selectedAnswers[exercise.id].orEmpty().isNotBlank() }

    val remainingScopedCount: Int
        get() = (scopedExercises.size - answeredScopedCount).coerceAtLeast(0)

    val currentExercise: LinguisticExercise?
        get() {
            val scoped = filteredExercises
            if (scoped.isEmpty()) return null
            return scoped[currentIndex.coerceIn(0, scoped.lastIndex)]
        }

    val domainOptions: List<String>
        get() = cascadingStringOptions(
            selectedValue = filters.domain,
            field = ReadAirFilterField.Domain,
            value = { it.domain },
        )

    val workOptions: List<String>
        get() {
            val options = exercises
                .map { normalizeReadAirWorkSlug(it.workSlug) }
                .filter { it.isNotBlank() }
                .toMutableSet()
            filters.workSlug.takeUnless { it == ReadAirAllFilter }?.let { options.add(normalizeReadAirWorkSlug(it)) }
            return listOf(ReadAirAllFilter) + options.sorted()
        }

    val phenomenonOptions: List<String>
        get() = cascadingStringOptions(
            selectedValue = filters.phenomenonKey,
            field = ReadAirFilterField.Phenomenon,
            value = { it.phenomenonKey },
        )

    val questionTypeOptions: List<String>
        get() = cascadingStringOptions(
            selectedValue = filters.questionType,
            field = ReadAirFilterField.QuestionType,
            value = { it.questionType },
        )

    val difficultyOptions: List<String>
        get() = cascadingStringOptions(
            selectedValue = filters.difficulty,
            field = ReadAirFilterField.Difficulty,
            value = { it.difficulty },
        )

    val episodeOptions: List<Int>
        get() {
            val options = exercises
                .filter { exercise -> matchesReadAirFilters(exercise, ignore = ReadAirFilterField.Episode) }
                .map { it.episode }
                .filter { it > 0 }
                .toMutableSet()
            filters.episode?.takeIf { it > 0 }?.let(options::add)
            return options.sorted()
        }

    fun selectedAnswerFor(exerciseId: String): String = selectedAnswers[exerciseId].orEmpty()

    fun browseAnswerFor(exerciseId: String): String {
        return browseAnswers[exerciseId] ?: selectedAnswers[exerciseId].orEmpty()
    }

    private fun cascadingStringOptions(
        selectedValue: String,
        field: ReadAirFilterField,
        value: (LinguisticExercise) -> String,
    ): List<String> {
        val options = exercises
            .filter { exercise -> matchesReadAirFilters(exercise, ignore = field) }
            .map(value)
            .filter { it.isNotBlank() }
            .toMutableSet()
        selectedValue.takeUnless { it == ReadAirAllFilter }?.let(options::add)
        return listOf(ReadAirAllFilter) + options.sorted()
    }

    private fun matchesReadAirFilters(
        exercise: LinguisticExercise,
        ignore: ReadAirFilterField,
    ): Boolean {
        val workMatch = ignore == ReadAirFilterField.Work ||
            filters.workSlug == ReadAirAllFilter ||
            normalizeReadAirWorkSlug(exercise.workSlug) == normalizeReadAirWorkSlug(filters.workSlug)
        val domainMatch = ignore == ReadAirFilterField.Domain ||
            filters.domain == ReadAirAllFilter ||
            exercise.domain == filters.domain
        val phenomenonMatch = ignore == ReadAirFilterField.Phenomenon ||
            filters.phenomenonKey == ReadAirAllFilter ||
            exercise.phenomenonKey == filters.phenomenonKey
        val questionTypeMatch = ignore == ReadAirFilterField.QuestionType ||
            filters.questionType == ReadAirAllFilter ||
            exercise.questionType == filters.questionType
        val difficultyMatch = ignore == ReadAirFilterField.Difficulty ||
            filters.difficulty == ReadAirAllFilter ||
            exercise.difficulty == filters.difficulty
        val episodeMatch = ignore == ReadAirFilterField.Episode ||
            filters.episode == null ||
            exercise.episode == filters.episode
        return workMatch && domainMatch && phenomenonMatch && questionTypeMatch && difficultyMatch && episodeMatch
    }
}

internal fun ReadAirTrainingState.resetScopedQueue(): ReadAirTrainingState {
    val scopedIds = scopedExercises.map { it.id }.toSet()
    return copy(
        currentIndex = 0,
        selectedAnswers = selectedAnswers.filterKeys { it !in scopedIds },
        reviewFocusExerciseId = null,
        pinnedExerciseId = null,
        aiCoach = AiCoachState(question = ReadAirAiQuestion),
    )
}

internal fun ReadAirTrainingState.advanceAfterCurrentAnswer(): ReadAirTrainingState {
    val size = filteredExercises.size
    val exercise = currentExercise ?: return this
    val selectedAnswer = selectedAnswerFor(exercise.id)
    if (size == 0 || selectedAnswer.isBlank()) return this
    return copy(
        pinnedExerciseId = null,
        currentIndex = currentIndex.coerceAtMost((size - 2).coerceAtLeast(0)),
        aiCoach = aiCoach.copy(status = SyncStatus.Idle, answer = "", result = null),
    )
}

private fun mergeReadAirExercises(
    primary: List<LinguisticExercise>,
    secondary: List<LinguisticExercise>,
): List<LinguisticExercise> {
    return (primary + secondary).distinctBy { it.id }
}

private fun Throwable.loginFailureMessage(): String {
    val raw = message.orEmpty()
    return when {
        raw.contains("Invalid email or password", ignoreCase = true) ||
            raw.contains("HTTP 401", ignoreCase = true) -> "邮箱或密码不正确，请检查后重试。"
        raw.contains("timeout", ignoreCase = true) -> "网络超时，请稍后再试。"
        raw.contains("API base URL is empty", ignoreCase = true) -> "学习服务地址为空，请在高级连接里检查。"
        raw.isBlank() -> "账号服务暂时不可用，请稍后再试。"
        else -> "账号服务暂时不可用：${raw.take(80)}"
    }
}

private enum class ReadAirFilterField {
    Work,
    Domain,
    Phenomenon,
    QuestionType,
    Difficulty,
    Episode,
}

data class ReadAirFilters(
    val workSlug: String = ReadAirAllFilter,
    val domain: String = ReadAirAllFilter,
    val phenomenonKey: String = ReadAirAllFilter,
    val questionType: String = ReadAirAllFilter,
    val difficulty: String = ReadAirAllFilter,
    val episode: Int? = null,
)

enum class LabTab(val label: String) {
    Today("今日"),
    Lesson("训练"),
    Library("资料"),
    ReadAir("读空气"),
    Review("错题"),
    Settings("设置"),
}

enum class TrainingSessionKind {
    Lesson,
    ReadAir,
}

enum class ReadAirMode(val label: String) {
    Train("单题训练"),
    Browse("浏览全部题目"),
}

private data class SyncAnswer(
    val itemId: String,
    val itemType: String,
    val selection: EpisodeSelection,
    val state: ReviewState,
    val label: String,
    val payload: JSONObject? = null,
)

private data class RemoteRefresh(
    val works: List<WorkOption>,
    val episodes: List<EpisodeOption>,
    val content: com.animejapaneselab.nativeapp.data.EpisodeContent,
    val readAirExercises: List<LinguisticExercise>,
    val fallbackReadAirExercises: List<LinguisticExercise>,
)

private fun normalizeEpisodeCounts(works: List<WorkOption>): List<WorkOption> {
    return works.map { work ->
        val knownMax = knownEpisodeCount(work.slug)
        if (knownMax != null && work.episodeCount > knownMax) {
            work.copy(episodeCount = knownMax)
        } else {
            work
        }
    }
}

private fun normalizeEpisodes(
    workSlug: String,
    episodes: List<EpisodeOption>,
    works: List<WorkOption>,
): List<EpisodeOption> {
    val maxEpisode = works.firstOrNull { it.slug == workSlug }?.episodeCount
        ?: knownEpisodeCount(workSlug)
        ?: episodes.maxOfOrNull { it.episode }
        ?: return episodes
    return episodes.filter { it.episode in 1..maxEpisode }
}

private fun knownEpisodeCount(workSlug: String): Int? {
    return when (workSlug) {
        "k-on" -> 14
        "re-zero", "rezero" -> 66
        else -> null
    }
}

private data class InitialEpisodeContent(
    val content: com.animejapaneselab.nativeapp.data.EpisodeContent,
    val readAirExercises: List<LinguisticExercise>,
    val hasNextLessonBatch: Boolean,
)

private data class RemoteProgressSnapshot(
    val progress: List<ProgressItem>,
    val review: List<ProgressItem>,
)

private data class EpisodeContentSnapshot(
    val focus: EpisodeFocus,
    val vocab: List<VocabItem>,
    val grammar: List<GrammarPoint>,
    val shadowing: List<ShadowingSentence>,
    val scenes: List<ReadAirScene>,
)

private fun ProgressItem.selectionOrFallback(fallback: EpisodeSelection): EpisodeSelection {
    return EpisodeSelection(
        workSlug = workSlug.ifBlank { fallback.workSlug },
        episode = episode.takeIf { it > 0 } ?: fallback.episode,
    )
}

internal fun findReviewReadAirExercise(
    task: ProgressItem,
    exercises: List<LinguisticExercise>,
): LinguisticExercise? {
    if (exercises.isEmpty()) return null

    exercises.firstOrNull { it.id == task.itemId }?.let { return it }

    val scopedExercises = exercises.filter { exercise ->
        val workMatches = task.workSlug.isBlank() ||
            normalizeReadAirWorkSlug(exercise.workSlug) == normalizeReadAirWorkSlug(task.workSlug)
        val episodeMatches = task.episode <= 0 || exercise.episode == task.episode
        workMatches && episodeMatches
    }.ifEmpty { exercises }

    scopedExercises.firstOrNull { it.id == task.itemId }?.let { return it }

    val sourceIds = listOf(
        task.payload["sourceId"],
        task.payload["source_id"],
        task.payload["source"],
    ).mapNotNull { it?.takeIf(String::isNotBlank) }
    scopedExercises.firstOrNull { exercise ->
        sourceIds.any { sourceId ->
            exercise.sourceId == sourceId || exercise.id == sourceId
        }
    }?.let { return it }

    val reviewLineNumbers = reviewLineNumbers(task)
    if (reviewLineNumbers.isNotEmpty()) {
        scopedExercises.firstOrNull { exercise ->
            val exerciseLines = buildSet {
                if (exercise.sourceLineNo > 0) add(exercise.sourceLineNo)
                if (exercise.targetLineNo > 0) add(exercise.targetLineNo)
                exercise.sceneLines.forEach { line ->
                    if (line.lineNo > 0) add(line.lineNo)
                }
            }
            reviewLineNumbers.any { it in exerciseLines }
        }?.let { return it }
    }

    val reviewTexts = listOf(
        task.label,
        task.payload["label"].orEmpty(),
        task.payload["prompt"].orEmpty(),
        task.payload["jaText"].orEmpty(),
        task.payload["ja_text"].orEmpty(),
    ).map(::normalizeReviewText)
        .filter { it.length >= 8 }
        .distinct()
    if (reviewTexts.isNotEmpty()) {
        scopedExercises.firstOrNull { exercise ->
            val exerciseText = normalizeReviewText(
                listOf(exercise.prompt, exercise.jaText, exercise.zhText, exercise.hint).joinToString(" "),
            )
            val exercisePrefix = exerciseText.take(24).takeIf { it.length >= 8 }
            reviewTexts.any { reviewText ->
                exerciseText.contains(reviewText) || (exercisePrefix != null && reviewText.contains(exercisePrefix))
            }
        }?.let { return it }
    }

    return null
}

private fun reviewLineNumbers(task: ProgressItem): Set<Int> {
    val source = listOf(
        task.label,
        task.payload["label"].orEmpty(),
        task.payload["sourceLabel"].orEmpty(),
        task.payload["source_label"].orEmpty(),
    ).joinToString(" ")
    val patterns = listOf(
        Regex("""第\s*(\d+)\s*行"""),
        Regex("""\bline\s*(\d+)\b""", RegexOption.IGNORE_CASE),
    )
    return patterns
        .flatMap { pattern -> pattern.findAll(source).mapNotNull { it.groupValues.getOrNull(1)?.toIntOrNull() } }
        .filter { it > 0 }
        .toSet()
}

private fun normalizeReviewText(value: String): String {
    return value
        .lowercase()
        .filter { it.isLetterOrDigit() || Character.UnicodeScript.of(it.code) in reviewTextScripts }
}

private val reviewTextScripts = setOf(
    Character.UnicodeScript.HIRAGANA,
    Character.UnicodeScript.KATAKANA,
    Character.UnicodeScript.HAN,
)

private fun LabUiState.withRemoteProgressSnapshot(snapshot: RemoteProgressSnapshot): LabUiState {
    val persistedReadAirAnswers = persistedReadAirAnswers(readAir.exercises, snapshot.progress)
    return copy(
        progressItems = snapshot.progress,
        reviewTasks = snapshot.review,
        readAir = readAir.copy(
            selectedAnswers = readAir.selectedAnswers + persistedReadAirAnswers,
            pinnedExerciseId = readAir.pinnedExerciseId?.takeIf { id ->
                readAir.exercises.any { it.id == id }
            },
        ),
    )
}

private fun persistedReadAirAnswers(
    exercises: List<LinguisticExercise>,
    progressItems: List<ProgressItem>,
): Map<String, String> {
    val exerciseIds = exercises.map { it.id }.toSet()
    return progressItems
        .filter { it.itemType == "exercise" && it.itemId in exerciseIds }
        .mapNotNull { item ->
            val selected = item.payload["selected"].orEmpty()
            if (selected.isBlank()) null else item.itemId to selected
        }
        .toMap()
}

private fun LessonTarget.labelFrom(state: LabUiState): String {
    return when (this) {
        is LessonTarget.Vocab -> state.vocab.firstOrNull { it.id == id }?.surface ?: "词汇"
        is LessonTarget.Grammar -> state.grammar.firstOrNull { it.id == id }?.pattern ?: "语法"
        is LessonTarget.Sentence -> state.shadowing.firstOrNull { it.id == id }?.ja ?: "跟读句"
    }
}

private fun LessonTarget.labelFromContent(content: EpisodeContentSnapshot): String {
    return when (this) {
        is LessonTarget.Vocab -> content.vocab.firstOrNull { it.id == id }?.surface ?: "词汇"
        is LessonTarget.Grammar -> content.grammar.firstOrNull { it.id == id }?.pattern ?: "语法"
        is LessonTarget.Sentence -> content.shadowing.firstOrNull { it.id == id }?.ja ?: "跟读句"
    }
}

private fun lessonTitle(mode: LessonMode, focus: EpisodeFocus, batch: Int): String {
    val batchPart = if (batch > 1) " 第 $batch 批" else ""
    return "${mode.titleLabel}$batchPart · ${focus.episodeLabel}"
}

private fun lightweightFocus(selection: EpisodeSelection, works: List<WorkOption>): EpisodeFocus {
    val work = works.firstOrNull { it.slug == selection.workSlug } ?: works.first()
    val episodeLabel = "${work.displayName} EP${selection.episode.toString().padStart(2, '0')}"
    return EpisodeFocus(
        workSlug = work.slug,
        episodeNumber = selection.episode,
        workTitle = work.displayName,
        episodeLabel = episodeLabel,
        lessonTitle = "正在准备 · $episodeLabel",
        sectionTitle = "课程内容加载中",
        guidebook = "正在准备本集词汇、语法、跟读和读空气题库。",
        dailyGoal = 8,
        xp = 0,
        streakDays = 0,
        energy = 5,
    )
}

private fun lightweightReadAirScene(selection: EpisodeSelection): ReadAirScene {
    val episodeLabel = "EP${selection.episode.toString().padStart(2, '0')}"
    return ReadAirScene(
        id = "${selection.workSlug}-$episodeLabel-loading",
        title = "题库准备中",
        context = "正在准备当前集读空气练习。",
        lines = listOf(
            com.animejapaneselab.nativeapp.data.DialogueLine(
                speaker = "系统",
                ja = "準備中です。",
                zh = "正在准备题库。",
            ),
        ),
        subtext = "稍后即可开始训练。",
        evidence = listOf("课程内容会在后台加载完成后自动刷新。"),
        learningPoint = "先显示可操作首页，再补齐完整题库。",
    )
}

private fun upsertMistake(
    mistakes: List<MistakeRecord>,
    node: LessonNode,
    selected: String,
    expected: String,
    explanation: String,
    selection: EpisodeSelection,
): List<MistakeRecord> {
    val existing = mistakes.firstOrNull { it.itemId == node.id }
    val next = MistakeRecord(
        itemId = node.id,
        typeLabel = node.typeLabel,
        prompt = node.prompt,
        selected = selected,
        expected = expected,
        explanation = explanation,
        sourceLabel = node.sourceLabel,
        attempts = (existing?.attempts ?: 0) + 1,
        lastState = ReviewState.Bad,
        workSlug = selection.workSlug,
        episode = selection.episode,
    )
    return (listOf(next) + mistakes.filterNot { it.itemId == node.id }).take(80)
}

private fun LessonNode.progressType(): String {
    return sourceKind
}

private fun LessonNode.aiKind(): String {
    return when (progressType()) {
        "vocab" -> "vocab"
        "grammar" -> "grammar"
        "sentence" -> "sentence"
        else -> "linguistic"
    }
}

private fun LessonNode.aiText(): String = prompt.ifBlank { expectedAnswer }

private fun buildAiContext(node: LessonNode, question: String): String {
    return buildString {
        append("用户问题：")
        append(question)
        append("\n题型：")
        append(node.typeLabel)
        append("\n题目：")
        append(node.prompt)
        append("\n正确答案：")
        append(node.expectedAnswer)
        append("\n解释：")
        append(node.explanation)
        append("\n来源：")
        append(node.sourceLabel)
    }
}

private fun upsertReadAirMistake(
    mistakes: List<MistakeRecord>,
    exercise: LinguisticExercise,
    selected: String,
    selection: EpisodeSelection,
): List<MistakeRecord> {
    val existing = mistakes.firstOrNull { it.itemId == exercise.id }
    val explanation = listOf(
        exercise.basicExplanationZh,
        exercise.deepExplanationZh,
        exercise.answer.rationaleZh,
        exercise.hint,
    ).filter { it.isNotBlank() }.joinToString(" ")
    val sourceLabel = buildString {
        append(selection.workSlug)
        append(" EP")
        append(selection.episode.toString().padStart(2, '0'))
        if (exercise.sourceLineNo > 0) {
            append(" line ")
            append(exercise.sourceLineNo)
        }
    }
    val next = MistakeRecord(
        itemId = exercise.id,
        typeLabel = "读空气",
        prompt = exercise.prompt,
        selected = selected,
        expected = exercise.correctOption,
        explanation = explanation.ifBlank { exercise.correctOption },
        sourceLabel = sourceLabel,
        attempts = (existing?.attempts ?: 0) + 1,
        lastState = ReviewState.Bad,
        workSlug = selection.workSlug,
        episode = selection.episode,
    )
    return (listOf(next) + mistakes.filterNot { it.itemId == exercise.id }).take(80)
}

private fun selectionForExercise(exercise: LinguisticExercise, fallback: EpisodeSelection): EpisodeSelection {
    return EpisodeSelection(
        workSlug = exercise.workSlug.ifBlank { fallback.workSlug },
        episode = exercise.episode.takeIf { it > 0 } ?: fallback.episode,
    )
}

private fun normalizeReadAirWorkSlug(workSlug: String): String {
    return when (workSlug) {
        "rezero" -> "re-zero"
        else -> workSlug
    }
}

private data class ProgressSyncResult(
    val item: ProgressItem,
    val recoveredDuplicate: Boolean = false,
)

private fun Throwable.isProgressDuplicateConflict(): Boolean {
    val raw = message.orEmpty()
    return raw.contains("23505") ||
        raw.contains("duplicate key", ignoreCase = true) ||
        raw.contains("Key (device_id, item_id)", ignoreCase = true) ||
        (raw.contains("HTTP 500") && raw.contains("409"))
}

private fun SyncAnswer.toProgressItem(): ProgressItem {
    return ProgressItem(
        itemId = itemId,
        itemType = itemType,
        workSlug = selection.workSlug,
        episode = selection.episode,
        state = state,
        label = label,
        lastReviewedAt = Instant.now().toString(),
        payload = payload?.toFlatStringMap().orEmpty(),
    )
}

private fun JSONObject.toFlatStringMap(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val names = keys()
    while (names.hasNext()) {
        val key = names.next()
        result[key] = optString(key)
    }
    return result
}

private fun buildReadAirAiContext(exercise: LinguisticExercise, selected: String): String {
    return buildString {
        append("用户问题：")
        append(ReadAirAiQuestion)
        append("\n题型：")
        append(exercise.questionType)
        append("\n领域：")
        append(exercise.domain)
        append("\n现象：")
        append(exercise.phenomenonKey)
        if (exercise.phenomenonNameZh.isNotBlank()) {
            append(" / ")
            append(exercise.phenomenonNameZh)
        }
        append("\n题目：")
        append(exercise.prompt)
        append("\n日文：")
        append(exercise.jaText)
        if (exercise.zhText.isNotBlank()) {
            append("\n中文：")
            append(exercise.zhText)
        }
        if (exercise.sceneLines.isNotEmpty()) {
            append("\n场景台词：")
            exercise.sceneLines.forEach { line ->
                append("\n")
                if (line.speaker.isNotBlank()) {
                    append(line.speaker)
                    append("：")
                }
                append(line.jaText)
                if (line.zhText.isNotBlank()) {
                    append(" / ")
                    append(line.zhText)
                }
            }
        }
        append("\n选项：")
        append(exercise.options.joinToString(" / "))
        append("\n用户选择：")
        append(selected.ifBlank { "尚未选择" })
        append("\n正确答案：")
        append(exercise.correctOption)
        listOf(
            "提示" to exercise.hint,
            "基础解释" to exercise.basicExplanationZh,
            "深入解释" to exercise.deepExplanationZh,
            "动画语境" to exercise.animeContextNoteZh,
            "注意事项" to exercise.cautionNoteZh,
            "答案依据" to exercise.answer.rationaleZh,
        ).forEach { (label, value) ->
            if (value.isNotBlank()) {
                append("\n")
                append(label)
                append("：")
                append(value)
            }
        }
    }
}
