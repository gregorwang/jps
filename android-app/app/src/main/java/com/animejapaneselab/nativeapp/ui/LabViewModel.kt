package com.animejapaneselab.nativeapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animejapaneselab.nativeapp.data.AiCoachState
import com.animejapaneselab.nativeapp.data.AuthUser
import com.animejapaneselab.nativeapp.data.EpisodeFocus
import com.animejapaneselab.nativeapp.data.EpisodeOption
import com.animejapaneselab.nativeapp.data.EpisodePlan
import com.animejapaneselab.nativeapp.data.EpisodeSelection
import com.animejapaneselab.nativeapp.data.GrammarPoint
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LessonExerciseKind
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.LessonNode
import com.animejapaneselab.nativeapp.data.LessonTarget
import com.animejapaneselab.nativeapp.data.LearningExercise
import com.animejapaneselab.nativeapp.data.LinguisticExercise
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.PronunciationApiException
import com.animejapaneselab.nativeapp.data.PronunciationAssessmentStatus
import com.animejapaneselab.nativeapp.data.PronunciationEvaluation
import com.animejapaneselab.nativeapp.data.ReadAirScene
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.data.ReviewState
import com.animejapaneselab.nativeapp.data.SampleLearningRepository
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.SubtitleLine
import com.animejapaneselab.nativeapp.data.SyncSnapshot
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.data.WorkOption
import com.animejapaneselab.nativeapp.data.buildLinguisticProgressPayload
import com.animejapaneselab.nativeapp.domain.LessonEngine
import com.animejapaneselab.nativeapp.domain.LessonSession
import com.animejapaneselab.nativeapp.domain.SmartReviewPlan
import com.animejapaneselab.nativeapp.domain.buildSmartReviewPlan
import com.animejapaneselab.nativeapp.domain.resumeLessonFromProgress
import com.animejapaneselab.nativeapp.platform.DeviceCapabilityReader
import com.animejapaneselab.nativeapp.platform.DeviceCapabilitySnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.UUID

const val ReadAirAllFilter = "all"
const val ReadAirCognitiveTopic = "cognitive_linguistics"
const val ReadAirAiQuestion = "请结合台词解释这道语言学训练题。"

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
    private var exerciseLabJob: Job? = null
    private var remoteRefreshJob: Job? = null
    private var reviewContentJob: Job? = null
    private var pronunciationEvaluationJob: Job? = null
    private var pendingPronunciationAttempt: PendingPronunciationAttempt? = null

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
                message = "正在从数据库加载语言学训练题库。",
                usingFallback = false,
            ),
            lesson = LessonEngine.start(emptyList()),
            lessonMode = LessonMode.Mixed,
            lessonBatch = initialLessonBatch,
            hasNextLessonBatch = false,
            mistakes = store.readMistakes(),
            progressItems = store.readProgress(),
        ),
    )
    val uiState: StateFlow<LabUiState> = _uiState.asStateFlow()

    init {
        loadInitialEpisodeContent()
        refreshDeviceCapabilities()
        refreshAuthStateOnce()
    }

    private fun loadInitialEpisodeContent() {
        val selection = initialSelection
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.Default) {
                val content = repository.content(selection, LessonMode.Mixed, initialLessonBatch)
                val hasNextBatch = repository.hasNextLessonBatch(
                    vocab = content.vocab,
                    grammar = content.grammar,
                    sentences = content.shadowing,
                    mode = LessonMode.Mixed,
                    batch = initialLessonBatch,
                )
                InitialEpisodeContent(content, hasNextBatch)
            }
            _uiState.update { state ->
                if (state.selection != selection) return@update state
                state.copy(
                    focus = snapshot.content.focus.copy(
                        streakDays = learningStreakDays(state.progressItems),
                        xp = learningXp(state.progressItems),
                    ),
                    vocab = snapshot.content.vocab,
                    grammar = snapshot.content.grammar,
                    shadowing = snapshot.content.shadowing,
                    exercises = snapshot.content.exercises,
                    scenes = snapshot.content.scenes,
                    selectedScene = snapshot.content.scenes.firstOrNull() ?: state.selectedScene,
                    readAir = state.readAir.copy(
                        message = "正在从数据库加载语言学训练题库。",
                        usingFallback = false,
                    ),
                    lesson = resumeLessonFromProgress(snapshot.content.lessonNodes, state.progressItems),
                    hasNextLessonBatch = snapshot.hasNextLessonBatch,
                )
            }
        }
    }

    private fun ensureFallbackReadAirCatalogLoaded() {
        if (readAirCatalogLoadStarted) return
        readAirCatalogLoadStarted = true
        _uiState.update { state ->
            state.copy(
                readAir = state.readAir.copy(
                    message = "正在从数据库加载语言学训练题库。",
                    usingFallback = false,
                ),
            )
        }
        refreshReadAirExercises()
    }

    private fun remoteClient(): RemoteLabClient {
        return RemoteLabClient(_uiState.value.settings.apiBaseUrl, store.readSessionCookie())
    }

    private fun fetchRemoteProgressSnapshot(client: RemoteLabClient): RemoteProgressSnapshot {
        val localProgress = store.readProgress()
        if (!_uiState.value.settings.cloudSync) {
            return RemoteProgressSnapshot(progress = localProgress)
        }
        val mergedProgress = mergeProgressItems(localProgress, client.fetchProgress(deviceId))
        store.writeProgress(mergedProgress)
        return RemoteProgressSnapshot(
            progress = mergedProgress,
            review = client.fetchReviewTasks(deviceId),
        )
    }

    fun selectTab(tab: LabTab) {
        _uiState.update { it.copy(selectedTab = tab, activeSession = null, secondaryScreen = null) }
        when (tab) {
            LabTab.Library,
            LabTab.Linguistics -> ensureFallbackReadAirCatalogLoaded()
            LabTab.Today,
            LabTab.Lesson,
            LabTab.Review -> Unit
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(activeSession = null, secondaryScreen = SecondaryScreen.Settings) }
        refreshDeviceCapabilities()
        refreshAuthStateOnce()
    }

    fun refreshDeviceCapabilities() {
        _uiState.update { it.copy(deviceCapabilitiesRefreshing = true) }
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                DeviceCapabilityReader.read(getApplication())
            }
            _uiState.update {
                it.copy(
                    deviceCapabilities = snapshot,
                    deviceCapabilitiesRefreshing = false,
                )
            }
        }
    }

    fun openSubtitles() {
        _uiState.update { it.copy(activeSession = null, secondaryScreen = SecondaryScreen.Subtitles) }
        refreshSubtitleLines()
    }

    fun openAiHistory() {
        _uiState.update { it.copy(activeSession = null, secondaryScreen = SecondaryScreen.AiHistory) }
    }

    fun openSearch() {
        _uiState.update { it.copy(activeSession = null, secondaryScreen = SecondaryScreen.Search) }
    }

    /**
     * Jumps to the subtitle browser at [workSlug]/[episode] and asks it to scroll to
     * [lineNo] (0 keeps the current scroll position). Used by search hits and mistake
     * cards; the browser reports back via [clearSubtitleFocus] once it has scrolled.
     */
    fun openSubtitlesAt(workSlug: String, episode: Int, lineNo: Int) {
        val current = _uiState.value.selection
        if (workSlug.isNotBlank() && workSlug != current.workSlug) {
            selectWork(workSlug)
        }
        if (episode > 0 && episode != _uiState.value.selection.episode) {
            selectEpisode(episode)
        }
        _uiState.update {
            it.copy(
                activeSession = null,
                secondaryScreen = SecondaryScreen.Subtitles,
                subtitleFocusLineNo = lineNo.takeIf { line -> line > 0 },
            )
        }
        refreshSubtitleLines()
    }

    fun clearSubtitleFocus() {
        _uiState.update { it.copy(subtitleFocusLineNo = null) }
    }

    fun openSmartReviewQueue() {
        _uiState.update { state ->
            val plan = buildSmartReviewPlan(
                reviewTasks = state.reviewTasks,
                mistakes = state.mistakes,
            )
            if (plan.entries.isEmpty()) {
                state.copy(selectedTab = LabTab.Lesson, activeSession = null, secondaryScreen = null)
            } else {
                state.copy(
                    selectedTab = LabTab.Review,
                    activeSession = null,
                    secondaryScreen = SecondaryScreen.SmartReviewQueue,
                    smartReviewPlan = plan,
                )
            }
        }
    }

    fun startSmartReviewItem(entryKey: String) {
        val entry = _uiState.value.smartReviewPlan.entries.firstOrNull { it.key == entryKey } ?: return
        _uiState.update { it.copy(secondaryScreen = null, selectedTab = LabTab.Review) }
        val localMistakeId = entry.localMistakeId
        if (localMistakeId != null) {
            practiceLocalMistake(localMistakeId)
        } else {
            entry.remoteTask?.let(::practiceReviewTask)
        }
    }

    fun closeSecondaryScreen() {
        _uiState.update { it.copy(secondaryScreen = null) }
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
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    val user = client.fetchAuthMe()
                    val snapshot = if (user == null) RemoteProgressSnapshot() else fetchRemoteProgressSnapshot(client)
                    user to snapshot
                }
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { (user, snapshot) ->
                        state.withRemoteProgressSnapshot(snapshot).copy(
                            auth = AuthState(
                                status = SyncStatus.Success,
                                user = user,
                                message = if (user == null) {
                                    "未登录。请先登录，学习进度只按账号保存。"
                                } else {
                                    "已登录：${user.email}；已读取账号进度 ${snapshot.progress.size} 条，复习 ${snapshot.review.size} 条。"
                                },
                            ),
                            sync = SyncSnapshot(
                                status = SyncStatus.Success,
                                message = if (user == null) "未登录，等待账号登录。" else "账号状态刷新完成：进度 ${snapshot.progress.size} 条，复习 ${snapshot.review.size} 条",
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
            if (result.getOrNull()?.first != null) {
                flushPendingProgress()
                refreshFromServer()
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
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    val login = RemoteLabClient(_uiState.value.settings.apiBaseUrl).loginOwner(trimmedEmail, password, deviceId)
                    store.writeSessionCookie(login.sessionCookie)
                    val snapshot = fetchRemoteProgressSnapshot(remoteClient())
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
            if (result.isSuccess) {
                flushPendingProgress()
                refreshFromServer()
            }
        }
    }

    fun logoutOwner() {
        _uiState.update { it.copy(auth = it.auth.copy(status = SyncStatus.Loading, message = "正在退出登录")) }
        viewModelScope.launch {
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    remoteClient().logoutOwner()
                }
            }
            store.clearSessionCookie()
            _uiState.update { state ->
                val logoutSucceeded = result.isSuccess
                val authMessage = if (logoutSucceeded) {
                    "已退出登录。请重新登录后继续学习。"
                } else {
                    "本机登录态已清除；服务端退出失败：${result.exceptionOrNull()?.message.orEmpty()}"
                }
                state.withRemoteProgressSnapshot(RemoteProgressSnapshot()).copy(
                    auth = AuthState(
                        status = if (logoutSucceeded) SyncStatus.Success else SyncStatus.Error,
                        user = null,
                        message = authMessage,
                    ),
                    sync = SyncSnapshot(
                        status = if (logoutSucceeded) SyncStatus.Success else SyncStatus.Error,
                        message = "已清除账号进度视图。",
                        lastSyncedAt = Instant.now().toString(),
                        remoteReviewCount = 0,
                        catalogUpdated = state.sync.catalogUpdated,
                    ),
                )
            }
        }
    }

    fun claimCurrentDevice() {
        _uiState.update { it.copy(auth = it.auth.copy(status = SyncStatus.Loading, message = "正在合并当前设备进度")) }
        viewModelScope.launch {
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    val merged = client.claimCurrentDevice(deviceId)
                    val snapshot = fetchRemoteProgressSnapshot(client)
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
        val shouldRefreshSubtitles = _uiState.value.secondaryScreen == SecondaryScreen.Subtitles
        val episodes = repository.episodes(workSlug)
        val rememberedEpisode = lastEpisodesByWork[workSlug]?.takeIf { episode ->
            episodes.any { it.episode == episode }
        }
        val episode = rememberedEpisode ?: episodes.firstOrNull()?.episode ?: 1
        applySelection(EpisodeSelection(workSlug = workSlug, episode = episode))
        if (shouldRefreshSubtitles) refreshSubtitleLines()
        refreshFromServerIfSignedIn()
    }

    fun selectEpisode(episode: Int) {
        val shouldRefreshSubtitles = _uiState.value.secondaryScreen == SecondaryScreen.Subtitles
        applySelection(_uiState.value.selection.copy(episode = episode))
        if (shouldRefreshSubtitles) refreshSubtitleLines()
        refreshFromServerIfSignedIn()
    }

    private fun refreshFromServerIfSignedIn() {
        if (_uiState.value.auth.user != null) {
            refreshFromServer()
        }
    }

    fun startLesson() {
        clearPronunciationAttempt()
        _uiState.update {
            it.copy(
                selectedTab = LabTab.Lesson,
                activeSession = TrainingSessionKind.Lesson,
                isExerciseLabSession = false,
                activeExerciseLabKind = null,
                activeLessonPathKey = null,
                pronunciationEvaluation = PronunciationEvaluationState(),
            )
        }
    }

    fun startLessonFromCurrentTab() {
        clearPronunciationAttempt()
        _uiState.update { state ->
            state.copy(
                activeSession = TrainingSessionKind.Lesson,
                isExerciseLabSession = false,
                activeExerciseLabKind = null,
                activeLessonPathKey = null,
                pronunciationEvaluation = PronunciationEvaluationState(),
                libraryRevealEpisodeActionsRequest = state.libraryRevealEpisodeActionsRequest +
                    if (state.selectedTab == LabTab.Library) 1 else 0,
            )
        }
    }

    fun startLessonModeFromCurrentTab(mode: LessonMode) {
        startLessonModeFromCurrentTab(mode, 1)
    }

    fun startLessonModeFromCurrentTab(mode: LessonMode, batch: Int, pathNodeKey: String? = null) {
        clearPronunciationAttempt()
        _uiState.update { state ->
            val safeBatch = batch.coerceAtLeast(1)
            val nodes = repository.buildLessonNodes(
                selection = state.selection,
                focus = state.focus,
                vocab = state.vocab,
                grammar = state.grammar,
                sentences = state.shadowing,
                mode = mode,
                exercises = state.exercises,
                batch = safeBatch,
            )
            state.copy(
                activeSession = TrainingSessionKind.Lesson,
                isExerciseLabSession = false,
                activeExerciseLabKind = null,
                lessonMode = mode,
                lessonBatch = safeBatch,
                lessonTarget = null,
                activeLessonPathKey = pathNodeKey,
                hasNextLessonBatch = pathNodeKey == null &&
                    repository.hasNextLessonBatch(state.vocab, state.grammar, state.shadowing, mode, safeBatch),
                focus = state.focus.copy(lessonTitle = lessonTitle(mode, state.focus, safeBatch)),
                lesson = if (pathNodeKey != null) LessonEngine.start(nodes) else resumeLessonFromProgress(nodes, state.progressItems),
                sessionXp = 0,
                aiCoach = AiCoachState(),
                pronunciationEvaluation = PronunciationEvaluationState(),
                libraryRevealEpisodeActionsRequest = state.libraryRevealEpisodeActionsRequest +
                    if (state.selectedTab == LabTab.Library) 1 else 0,
            )
        }
    }

    fun startExerciseLab(kind: LessonExerciseKind) {
        startExerciseLabSession(kind)
    }

    fun startExerciseLabMix() {
        startExerciseLabSession(kind = null)
    }

    private fun startExerciseLabSession(
        kind: LessonExerciseKind?,
        continueCurrentLab: Boolean = false,
    ) {
        if (exerciseLabJob?.isActive == true) return
        val request = _uiState.value
        val deckNumber = if (continueCurrentLab && request.isExerciseLabSession) {
            request.lessonBatch + 1
        } else {
            1
        }
        _uiState.update { state -> state.copy(exerciseLabLoading = true) }
        exerciseLabJob = viewModelScope.launch {
            try {
                val nodes = withContext(Dispatchers.Default) {
                    fun nodesFrom(
                        focus: EpisodeFocus,
                        vocab: List<VocabItem>,
                        grammar: List<GrammarPoint>,
                        sentences: List<ShadowingSentence>,
                    ): List<LessonNode> {
                        return if (kind == null) {
                            repository.buildExerciseLabMix(
                                selection = request.selection,
                                focus = focus,
                                vocab = vocab,
                                grammar = grammar,
                                sentences = sentences,
                                exercises = request.exercises,
                                progressItems = request.progressItems,
                            )
                        } else {
                            repository.buildExerciseKindNodes(
                                selection = request.selection,
                                focus = focus,
                                vocab = vocab,
                                grammar = grammar,
                                sentences = sentences,
                                kind = kind,
                                exercises = request.exercises,
                                progressItems = request.progressItems,
                            )
                        }
                    }

                    val liveNodes = nodesFrom(request.focus, request.vocab, request.grammar, request.shadowing)
                    val fallbackContent = if (liveNodes.isEmpty()) {
                        repository.content(request.selection, kind?.defaultLessonMode() ?: LessonMode.Mixed)
                    } else {
                        null
                    }
                    liveNodes.ifEmpty {
                        fallbackContent?.let { content ->
                            nodesFrom(content.focus, content.vocab, content.grammar, content.shadowing)
                        }.orEmpty()
                    }
                }
                _uiState.update { state ->
                    if (state.selection != request.selection) return@update state
                    if (nodes.isEmpty()) {
                        return@update state.copy(
                            exerciseLabLoading = false,
                            sync = state.sync.copy(message = "当前集暂时没有${kind?.label ?: "混合"}题，换一集再试。"),
                        )
                    }
                    val title = kind?.let { "题型实验室 · ${it.label}" } ?: "题型实验室 · 六类快练"
                    state.copy(
                        exerciseLabLoading = false,
                        activeSession = TrainingSessionKind.Lesson,
                        isExerciseLabSession = true,
                        activeExerciseLabKind = kind,
                        lessonMode = kind?.defaultLessonMode() ?: LessonMode.Mixed,
                        lessonBatch = deckNumber,
                        lessonTarget = null,
                        activeLessonPathKey = null,
                        // The practice lab is an open queue. The next deck is rebuilt from the
                        // latest progress so unseen material rotates in before completed material.
                        hasNextLessonBatch = true,
                        focus = state.focus.copy(
                            lessonTitle = if (deckNumber > 1) "$title · 第 $deckNumber 组" else title,
                        ),
                        // Practice lab decks deliberately rotate back to old material after all
                        // unseen material is covered, so they must always start as a fresh session.
                        lesson = LessonEngine.start(nodes),
                        sessionXp = 0,
                        aiCoach = AiCoachState(),
                        pronunciationEvaluation = PronunciationEvaluationState(),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { state ->
                    state.copy(
                        exerciseLabLoading = false,
                        sync = state.sync.copy(
                            status = SyncStatus.Error,
                            message = "题型训练准备失败：${error.message ?: "请稍后重试"}",
                        ),
                    )
                }
            } finally {
                _uiState.update { state -> state.copy(exerciseLabLoading = false) }
                exerciseLabJob = null
            }
        }
    }

    fun selectLessonMode(mode: LessonMode) {
        clearPronunciationAttempt()
        _uiState.update { state ->
            val batch = 1
            val nodes = repository.buildLessonNodes(
                selection = state.selection,
                focus = state.focus,
                vocab = state.vocab,
                grammar = state.grammar,
                sentences = state.shadowing,
                mode = mode,
                exercises = state.exercises,
                batch = batch,
            )
            state.copy(
                isExerciseLabSession = false,
                activeExerciseLabKind = null,
                lessonMode = mode,
                lessonBatch = batch,
                lessonTarget = null,
                activeLessonPathKey = null,
                hasNextLessonBatch = repository.hasNextLessonBatch(state.vocab, state.grammar, state.shadowing, mode, batch),
                focus = state.focus.copy(lessonTitle = lessonTitle(mode, state.focus, batch)),
                lesson = resumeLessonFromProgress(nodes, state.progressItems),
                sessionXp = 0,
                aiCoach = AiCoachState(),
                pronunciationEvaluation = PronunciationEvaluationState(),
            )
        }
    }

    fun startTargetLesson(target: LessonTarget) {
        clearPronunciationAttempt()
        _uiState.update { state ->
            val nodes = repository.buildLessonNodes(
                selection = state.selection,
                focus = state.focus,
                vocab = state.vocab,
                grammar = state.grammar,
                sentences = state.shadowing,
                mode = state.lessonMode,
                exercises = state.exercises,
                target = target,
            )
            state.copy(
                activeSession = TrainingSessionKind.Lesson,
                isExerciseLabSession = false,
                activeExerciseLabKind = null,
                lessonTarget = target,
                activeLessonPathKey = null,
                lessonBatch = 1,
                hasNextLessonBatch = false,
                focus = state.focus.copy(lessonTitle = "单点训练 · ${target.labelFrom(state)}"),
                lesson = LessonEngine.start(nodes),
                sessionXp = 0,
                aiCoach = AiCoachState(),
                pronunciationEvaluation = PronunciationEvaluationState(),
            )
        }
    }

    fun startReadAirSession() {
        ensureFallbackReadAirCatalogLoaded()
        _uiState.update { state ->
            state.copy(
                activeSession = TrainingSessionKind.ReadAir,
                activeLessonPathKey = null,
                sessionXp = 0,
                readAir = state.readAir.copy(
                    mode = ReadAirMode.Train,
                    currentIndex = 0,
                    reviewFocusExerciseId = null,
                    pinnedExerciseId = null,
                    sessionExerciseIds = emptySet(),
                    sessionBatch = null,
                    restoreFiltersAfterSession = state.readAir.restoreFiltersAfterSession ?: state.readAir.filters,
                    aiCoach = AiCoachState(question = ReadAirAiQuestion),
                ),
            )
        }
    }

    fun startReadAirForCurrentEpisode() {
        startReadAirForCurrentEpisode(pathBatch = null)
    }

    fun startReadAirPathBatch(batch: Int) {
        startReadAirForCurrentEpisode(pathBatch = batch.coerceAtLeast(1))
    }

    private fun startReadAirForCurrentEpisode(pathBatch: Int?) {
        if (_uiState.value.readAir.exercises.isEmpty()) {
            refreshReadAirExercises()
        }
        _uiState.update { state ->
            val filters = ReadAirFilters(
                workSlug = state.selection.workSlug,
                episode = state.selection.episode,
            )
            val readAir = state.readAir.copy(
                mode = ReadAirMode.Train,
                filters = filters,
                currentIndex = 0,
                reviewFocusExerciseId = null,
                pinnedExerciseId = null,
                restoreFiltersAfterSession = state.readAir.restoreFiltersAfterSession ?: state.readAir.filters,
                aiCoach = AiCoachState(question = ReadAirAiQuestion),
                usingFallback = false,
                sessionExerciseIds = pathBatch?.let { batch ->
                    state.readAir.exercises
                        .filter { exercise ->
                            normalizeReadAirWorkSlug(exercise.workSlug) == normalizeReadAirWorkSlug(state.selection.workSlug) &&
                                exercise.episode == state.selection.episode
                        }
                        .drop((batch - 1) * 7)
                        .take(7)
                        .map(LinguisticExercise::id)
                        .toSet()
                }.orEmpty(),
                sessionBatch = pathBatch,
            )
            state.copy(
                activeSession = TrainingSessionKind.ReadAir,
                activeLessonPathKey = null,
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
                activeLessonPathKey = null,
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
        clearPronunciationAttempt()
        _uiState.update { state ->
            val exitingReadAir = state.activeSession == TrainingSessionKind.ReadAir
            val restoreFilters = state.readAir.restoreFiltersAfterSession
                .takeIf { exitingReadAir }
            state.copy(
                activeSession = null,
                isExerciseLabSession = false,
                activeExerciseLabKind = null,
                activeLessonPathKey = null,
                pronunciationEvaluation = PronunciationEvaluationState(),
                readAir = if (exitingReadAir) {
                    state.readAir.copy(
                        filters = restoreFilters ?: state.readAir.filters,
                        currentIndex = 0,
                        reviewFocusExerciseId = null,
                        pinnedExerciseId = null,
                        sessionExerciseIds = emptySet(),
                        sessionBatch = null,
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
        clearPronunciationAttempt()
        val current = _uiState.value
        if (current.isExerciseLabSession) {
            startExerciseLabSession(
                kind = current.activeExerciseLabKind,
                continueCurrentLab = true,
            )
            return
        }
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
                exercises = state.exercises,
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
                lesson = resumeLessonFromProgress(nodes, state.progressItems),
                sessionXp = 0,
                aiCoach = AiCoachState(),
                pronunciationEvaluation = PronunciationEvaluationState(),
            )
        }
    }

    fun submitAnswer(selected: String) {
        var committedEffects: AnswerCommitEffects? = null
        while (committedEffects == null) {
            val state = _uiState.value
            val nextLesson = LessonEngine.answer(state.lesson, selected)
            val feedback = nextLesson.feedback
            if (feedback == null) {
                if (_uiState.compareAndSet(state, state.copy(lesson = nextLesson))) return
                continue
            }
            val answeredNow = nextLesson.answered > state.lesson.answered
            if (!answeredNow) {
                if (_uiState.compareAndSet(state, state.copy(lesson = nextLesson))) return
                continue
            }

            val node = state.lesson.currentNode
            if (node == null) {
                if (_uiState.compareAndSet(state, state.copy(lesson = nextLesson))) return
                continue
            }
            val correct = feedback.correct
            val nextMistakes = if (correct) {
                state.mistakes.filterNot { it.itemId == node.id }
            } else {
                upsertMistake(state.mistakes, node, selected, feedback.expected, feedback.explanation, state.selection)
            }
            val answerPayload = SyncAnswer(
                itemId = node.progressItemId(),
                itemType = node.progressType(),
                selection = state.selection,
                state = if (correct) ReviewState.Good else ReviewState.Bad,
                label = node.prompt.take(90),
                payload = node.buildLessonProgressPayload(selected, feedback.expected),
            )
            val syncPayloads = mutableListOf(answerPayload)
            val optimisticItems = mutableListOf(answerPayload.toProgressItem())
            val finishingPathNode = state.activeLessonPathKey?.takeIf {
                state.lesson.index == state.lesson.nodes.lastIndex
            }
            if (finishingPathNode != null) {
                val pathPayload = SyncAnswer(
                    itemId = pathNodeProgressId(state.selection, finishingPathNode),
                    // The backend accepts the shared progress types only; the payload carries
                    // the Android path-node discriminator without changing the transport type.
                    itemType = "unknown",
                    selection = state.selection,
                    state = ReviewState.Good,
                    label = "${state.focus.episodeLabel} · $finishingPathNode",
                    payload = JSONObject()
                        .put("pathNodeKey", finishingPathNode)
                        .put("lessonMode", state.lessonMode.name)
                        .put("batch", state.lessonBatch),
                )
                syncPayloads += pathPayload
                optimisticItems += pathPayload.toProgressItem()
            }
            val nextProgressItems = optimisticItems + state.progressItems.filterNot { existing ->
                optimisticItems.any { optimistic -> existing.sameProgressIdentity(optimistic) }
            }

            val nextState = state.copy(
                lesson = nextLesson,
                sessionXp = state.sessionXp + if (correct) 12 else 0,
                focus = state.focus.copy(energy = (state.focus.energy + if (correct) 0 else -1).coerceIn(0, 5)),
                mistakes = nextMistakes,
                progressItems = nextProgressItems,
            )
            if (_uiState.compareAndSet(state, nextState)) {
                committedEffects = AnswerCommitEffects(
                    mistakes = nextMistakes,
                    progressItems = nextProgressItems,
                    syncPayloads = syncPayloads,
                )
            }
        }
        val effects = checkNotNull(committedEffects)
        store.writeMistakes(effects.mistakes)
        persistOptimisticProgress(effects.progressItems, effects.syncPayloads)
        effects.syncPayloads.forEach(::syncAnswer)
    }

    fun continueLesson() {
        clearPronunciationAttempt()
        _uiState.update { state ->
            state.copy(
                lesson = LessonEngine.continueAfterFeedback(state.lesson),
                pronunciationEvaluation = PronunciationEvaluationState(),
            )
        }
    }

    fun restartLesson() {
        clearPronunciationAttempt()
        _uiState.update {
            it.copy(
                lesson = LessonEngine.restart(it.lesson),
                sessionXp = 0,
                pronunciationEvaluation = PronunciationEvaluationState(),
            )
        }
    }

    fun evaluatePronunciation(
        nodeId: String,
        sentenceId: String,
        wavBytes: ByteArray,
        durationMs: Long,
    ) {
        if (durationMs < 400L) {
            _uiState.update { state ->
                state.copy(
                    pronunciationEvaluation = PronunciationEvaluationState(
                        nodeId = nodeId,
                        phase = PronunciationEvaluationPhase.Complete,
                        message = "录音太短，请完整读完这句话。",
                    ),
                )
            }
            return
        }
        if (durationMs > 15_000L || wavBytes.size > 1_500_000) {
            _uiState.update { state ->
                state.copy(
                    pronunciationEvaluation = PronunciationEvaluationState(
                        nodeId = nodeId,
                        phase = PronunciationEvaluationPhase.Complete,
                        message = "录音超过 15 秒，请缩短后重新录制。",
                    ),
                )
            }
            return
        }

        val attempt = PendingPronunciationAttempt(
            nodeId = nodeId,
            sentenceId = sentenceId,
            attemptId = UUID.randomUUID().toString(),
            wavBytes = wavBytes,
        )
        pendingPronunciationAttempt = attempt
        runPronunciationEvaluation(attempt)
    }

    fun retryPronunciationEvaluation() {
        pendingPronunciationAttempt?.let(::runPronunciationEvaluation)
    }

    fun resetPronunciationEvaluation() {
        clearPronunciationAttempt()
        _uiState.update { state ->
            state.copy(pronunciationEvaluation = PronunciationEvaluationState())
        }
    }

    private fun runPronunciationEvaluation(attempt: PendingPronunciationAttempt) {
        pronunciationEvaluationJob?.cancel()
        _uiState.update { state ->
            state.copy(
                pronunciationEvaluation = PronunciationEvaluationState(
                    nodeId = attempt.nodeId,
                    phase = PronunciationEvaluationPhase.Loading,
                    message = "正在识别、对齐并生成体验评分…",
                ),
            )
        }
        pronunciationEvaluationJob = viewModelScope.launch {
            val result = try {
                Result.success(withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    fun evaluateWithFreshTicket(): PronunciationEvaluation {
                        val ticket = client.createPronunciationTicket(attempt.sentenceId)
                        return client.evaluatePronunciation(
                            ticket = ticket,
                            sentenceId = attempt.sentenceId,
                            attemptId = attempt.attemptId,
                            wavBytes = attempt.wavBytes,
                        )
                    }

                    try {
                        evaluateWithFreshTicket()
                    } catch (error: PronunciationApiException) {
                        if (error.httpStatus == 401 && error.code == "ticket_expired") {
                            evaluateWithFreshTicket()
                        } else {
                            throw error
                        }
                    }
                })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Result.failure(error)
            }
            val failure = result.exceptionOrNull()
            val retryable = failure?.let { error ->
                error !is PronunciationApiException ||
                    error.httpStatus == 429 || error.httpStatus == 502 ||
                    (error.httpStatus == 401 && error.code == "ticket_expired")
            } ?: false
            if (result.isSuccess || !retryable) pendingPronunciationAttempt = null
            _uiState.update { state ->
                if (state.lesson.currentNode?.id != attempt.nodeId ||
                    state.pronunciationEvaluation.nodeId != attempt.nodeId
                ) {
                    return@update state
                }
                result.fold(
                    onSuccess = { evaluation ->
                        state.copy(
                            pronunciationEvaluation = PronunciationEvaluationState(
                                nodeId = attempt.nodeId,
                                phase = PronunciationEvaluationPhase.Complete,
                                result = evaluation,
                                message = pronunciationResultMessage(evaluation),
                            ),
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            pronunciationEvaluation = PronunciationEvaluationState(
                                nodeId = attempt.nodeId,
                                phase = PronunciationEvaluationPhase.Error,
                                message = pronunciationFailureMessage(error),
                                canRetry = retryable,
                            ),
                        )
                    },
                )
            }
        }
    }

    private fun clearPronunciationAttempt() {
        pronunciationEvaluationJob?.cancel()
        pronunciationEvaluationJob = null
        pendingPronunciationAttempt = null
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
        _uiState.update {
            it.copy(
                readAir = it.readAir.copy(
                    status = SyncStatus.Loading,
                    message = "正在更新语言学题库",
                ),
            )
        }
        viewModelScope.launch {
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    remoteClient().fetchLinguisticExercises()
                }
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { remoteExercises ->
                        val selectedAnswers = restoreReadAirAnswers(
                            exercises = remoteExercises,
                            progressItems = current.progressItems,
                            inMemoryAnswers = current.readAir.selectedAnswers,
                        )
                        current.copy(
                            readAir = current.readAir.copy(
                                status = SyncStatus.Success,
                                message = if (remoteExercises.isEmpty()) {
                                    "数据库暂时没有返回语言学题。"
                                } else {
                                    "已更新 ${remoteExercises.size} 道语言学题。"
                                },
                                exercises = remoteExercises,
                                usingFallback = false,
                                currentIndex = 0,
                                reviewFocusExerciseId = null,
                                selectedAnswers = selectedAnswers,
                                pinnedExerciseId = null,
                                aiCoach = AiCoachState(question = ReadAirAiQuestion),
                            ),
                        )
                    },
                    onFailure = { error ->
                        current.copy(
                            readAir = current.readAir.copy(
                                status = SyncStatus.Error,
                                message = "练习更新失败：${error.message ?: "网络不可用"}",
                                usingFallback = false,
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

    fun selectReadAirTopic(topic: String) {
        _uiState.update { state ->
            state.copy(
                readAir = state.readAir.copy(
                    filters = state.readAir.filters.copy(topic = topic),
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
        val exerciseId = _uiState.value.readAir.currentExercise?.id ?: return
        commitReadAirAnswer(exerciseId, option, fromBrowse = false)
    }

    fun selectReadAirBrowseAnswer(exerciseId: String, option: String) {
        commitReadAirAnswer(exerciseId, option, fromBrowse = true)
    }

    private fun commitReadAirAnswer(
        exerciseId: String,
        option: String,
        fromBrowse: Boolean,
    ) {
        var committedEffects: AnswerCommitEffects? = null
        while (committedEffects == null) {
            val state = _uiState.value
            val exercise = state.readAir.exercises.firstOrNull { it.id == exerciseId } ?: return
            val correct = exercise.isCorrect(option)
            val answeredBefore = state.readAir.selectedAnswers.containsKey(exercise.id)
            val nextMistakes = if (correct) {
                state.mistakes.filterNot { it.itemId == exercise.id }
            } else {
                upsertReadAirMistake(state.mistakes, exercise, option, selectionForExercise(exercise, state.selection))
            }
            val syncPayload = SyncAnswer(
                itemId = exercise.id,
                itemType = "exercise",
                selection = selectionForExercise(exercise, state.selection),
                state = if (correct) ReviewState.Good else ReviewState.Bad,
                label = exercise.prompt.take(90),
                payload = buildLinguisticProgressPayload(exercise, option),
            )
            val progressItem = syncPayload.toProgressItem()
            val nextProgressItems = listOf(progressItem) + state.progressItems.filterNot {
                it.sameProgressIdentity(progressItem)
            }
            val nextState = state.copy(
                readAir = state.readAir.copy(
                    selectedAnswers = state.readAir.selectedAnswers + (exercise.id to option),
                    browseAnswers = if (fromBrowse) {
                        state.readAir.browseAnswers + (exercise.id to option)
                    } else {
                        state.readAir.browseAnswers
                    },
                    pinnedExerciseId = if (fromBrowse) state.readAir.pinnedExerciseId else exercise.id,
                ),
                sessionXp = state.sessionXp + if (!fromBrowse && correct && !answeredBefore) 8 else 0,
                focus = if (fromBrowse) {
                    state.focus
                } else {
                    state.focus.copy(
                        energy = (state.focus.energy + if (correct || answeredBefore) 0 else -1).coerceIn(0, 5),
                    )
                },
                mistakes = nextMistakes,
                progressItems = nextProgressItems,
            )
            if (_uiState.compareAndSet(state, nextState)) {
                committedEffects = AnswerCommitEffects(
                    mistakes = nextMistakes,
                    progressItems = nextProgressItems,
                    syncPayloads = listOf(syncPayload),
                )
            }
        }
        val effects = checkNotNull(committedEffects)
        store.writeMistakes(effects.mistakes)
        persistOptimisticProgress(effects.progressItems, effects.syncPayloads)
        effects.syncPayloads.forEach(::syncAnswer)
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
            val result = runSuspendCatching {
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
            val result = runSuspendCatching {
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
            val result = runSuspendCatching {
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

    fun askAiAboutMistake(itemId: String) {
        val state = _uiState.value
        val mistake = state.mistakes.firstOrNull { it.itemId == itemId } ?: return
        _uiState.update {
            it.copy(
                reviewAiTargetId = itemId,
                aiCoach = it.aiCoach.copy(status = SyncStatus.Loading, answer = "", result = null),
            )
        }
        viewModelScope.launch {
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    remoteClient().askAi(
                        deviceId = state.deviceId,
                        model = state.settings.aiModel,
                        reasoningEffort = state.settings.reasoningEffort,
                        kind = if (mistake.typeLabel == "语言学题" || mistake.typeLabel == "读空气") "linguistic" else "exercise",
                        text = mistake.prompt,
                        context = buildString {
                            append("请用简体中文讲解这道错题。\n")
                            append("题目：").append(mistake.prompt)
                            append("\n我的答案：").append(mistake.selected)
                            append("\n正确答案：").append(mistake.expected)
                            if (mistake.explanation.isNotBlank()) append("\n站内说明：").append(mistake.explanation)
                            append("\n来源：").append(mistake.workSlug).append(" EP").append(mistake.episode)
                            append("\n请按“语境线索 -> 错因 -> 正确判断 -> 下次判断方法”讲解。")
                        },
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
        val request = RemoteRefreshRequest(
            selection = state.selection,
            lessonMode = state.lessonMode,
            lessonBatch = state.lessonBatch,
        )
        remoteRefreshJob?.cancel()
        _uiState.update {
            it.copy(sync = it.sync.copy(status = SyncStatus.Loading, message = "正在更新课程和当前集资料"))
        }
        remoteRefreshJob = viewModelScope.launch {
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    val works = normalizeEpisodeCounts(client.fetchWorks().ifEmpty { repository.works() })
                    val episodes = normalizeEpisodes(
                        workSlug = request.selection.workSlug,
                        episodes = client.fetchEpisodes(request.selection.workSlug).ifEmpty { repository.episodes(request.selection.workSlug) },
                        works = works,
                    )
                    val payload = client.fetchEpisodePayload(request.selection)
                    val episodePlan = runSuspendCatching { client.fetchEpisodePlan(request.selection) }.getOrNull()
                    val readAirExercises = client.fetchLinguisticExercises()
                    val content = repository.contentFromRemote(
                        selection = request.selection,
                        vocab = prioritizeCoreVocab(payload.vocab, episodePlan),
                        grammar = payload.grammar,
                        shadowing = payload.shadowing,
                        exercises = payload.exercises,
                        mode = request.lessonMode,
                        batch = request.lessonBatch,
                    )
                    RemoteRefresh(works, episodes, content, readAirExercises, episodePlan)
                }
            }
            _uiState.update { current ->
                if (!request.matches(current)) return@update current
                result.fold(
                    onSuccess = { remote ->
                        val selectedAnswers = restoreReadAirAnswers(
                            exercises = remote.readAirExercises,
                            progressItems = current.progressItems,
                            inMemoryAnswers = current.readAir.selectedAnswers,
                        )
                        current.copy(
                            works = remote.works,
                            episodes = remote.episodes,
                            focus = remote.content.focus.copy(
                                streakDays = learningStreakDays(current.progressItems),
                                xp = learningXp(current.progressItems),
                            ),
                            vocab = remote.content.vocab,
                            grammar = remote.content.grammar,
                            shadowing = remote.content.shadowing,
                            exercises = remote.content.exercises,
                            episodePlan = remote.episodePlan,
                            scenes = remote.content.scenes,
                            selectedScene = remote.content.scenes.first(),
                            readAir = current.readAir.copy(
                                status = SyncStatus.Success,
                                message = if (remote.readAirExercises.isEmpty()) {
                                    "资料已更新；数据库暂时没有返回语言学题。"
                                } else {
                                    "资料已更新；语言学题库 ${remote.readAirExercises.size} 道。"
                                },
                                exercises = remote.readAirExercises,
                                usingFallback = false,
                                currentIndex = 0,
                                selectedAnswers = selectedAnswers,
                                pinnedExerciseId = null,
                                aiCoach = AiCoachState(question = ReadAirAiQuestion),
                            ),
                            lesson = resumeLessonFromProgress(remote.content.lessonNodes, current.progressItems),
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
                                message = "已更新 ${remote.content.focus.episodeLabel} · 数据库题 ${remote.content.exercises.size} 道",
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

    fun refreshSubtitleLines() {
        val selection = _uiState.value.selection
        _uiState.update {
            it.copy(
                subtitleStatus = SyncStatus.Loading,
                subtitleMessage = "正在读取 ${selection.workSlug} EP${selection.episode.toString().padStart(2, '0')} 台词",
            )
        }
        viewModelScope.launch {
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    remoteClient().fetchSubtitleLines(selection)
                }
            }
            _uiState.update { state ->
                if (state.selection != selection) return@update state
                result.fold(
                    onSuccess = { lines ->
                        state.copy(
                            subtitles = lines,
                            subtitleStatus = SyncStatus.Success,
                            subtitleMessage = if (lines.isEmpty()) "这一集暂时没有可浏览台词。" else "已读取 ${lines.size} 行台词。",
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            subtitles = emptyList(),
                            subtitleStatus = SyncStatus.Error,
                            subtitleMessage = "台词读取失败：${error.message ?: "网络不可用"}",
                        )
                    },
                )
            }
        }
    }

    fun syncProgressNow() {
        flushPendingProgress()
        _uiState.update { it.copy(sync = it.sync.copy(status = SyncStatus.Loading, message = "正在同步进度和今日复习")) }
        viewModelScope.launch {
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    val user = client.fetchAuthMe()
                    if (user == null) error("请先登录账号。")
                    user to fetchRemoteProgressSnapshot(client)
                }
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { (user, snapshot) ->
                        current.withRemoteProgressSnapshot(snapshot).copy(
                            auth = current.auth.copy(
                                status = SyncStatus.Success,
                                user = user,
                                message = "已登录：${user.email}；已读取账号进度 ${snapshot.progress.size} 条，复习 ${snapshot.review.size} 条。",
                            ),
                            sync = SyncSnapshot(
                                status = SyncStatus.Success,
                                message = "已同步账号进度 ${snapshot.progress.size} 条，今日复习 ${snapshot.review.size} 条",
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
        var committedMistakes: List<MistakeRecord>? = null
        while (committedMistakes == null) {
            val state = _uiState.value
            val nextMistakes = state.mistakes.filterNot { it.itemId == itemId }
            if (_uiState.compareAndSet(state, state.copy(mistakes = nextMistakes))) {
                committedMistakes = nextMistakes
            }
        }
        store.writeMistakes(checkNotNull(committedMistakes))
    }

    fun practiceLocalMistake(itemId: String) {
        val mistake = _uiState.value.mistakes.firstOrNull { it.itemId == itemId }
        if (mistake == null) {
            selectTab(LabTab.Lesson)
            return
        }
        if (mistake.typeLabel == "语言学题" || mistake.typeLabel == "读空气") {
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
        if (reviewNode.sourceKind == "exercise") {
            startOrdinaryExerciseReview(
                task = ProgressItem(
                    itemId = reviewNode.id,
                    itemType = "exercise",
                    workSlug = selection.workSlug,
                    episode = selection.episode,
                    state = mistake.lastState,
                    label = mistake.prompt,
                ),
                node = reviewNode,
            )
            return
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
        val targetSelection = task.selectionOrFallback(_uiState.value.selection)
        if (targetSelection != _uiState.value.selection) {
            loadRemoteReviewContent(task, targetSelection)
            return
        }
        when (task.itemType) {
            "vocab",
            "grammar",
            "sentence" -> {
                val state = _uiState.value
                val selection = task.selectionOrFallback(state.selection)
                val exactNode = findReviewLessonNode(selection, state, task.itemId)
                if (exactNode != null) {
                    startOrdinaryExerciseReview(task, exactNode)
                } else {
                    val sourceId = task.primarySourceId()
                    when (task.itemType) {
                        "vocab" -> startReviewLesson(task, LessonTarget.Vocab(sourceId))
                        "grammar" -> startReviewLesson(task, LessonTarget.Grammar(sourceId))
                        else -> startReviewLesson(task, LessonTarget.Sentence(sourceId))
                    }
                }
            }
            "exercise" -> {
                val state = _uiState.value
                val selection = task.selectionOrFallback(state.selection)
                val ordinaryNode = findReviewLessonNode(selection, state, task.itemId)
                    ?.takeIf { it.sourceKind == "exercise" }
                if (ordinaryNode != null) {
                    startOrdinaryExerciseReview(task, ordinaryNode)
                } else {
                    startReviewReadAir(task)
                }
            }
            else -> selectTab(LabTab.Lesson)
        }
    }

    private fun loadRemoteReviewContent(task: ProgressItem, selection: EpisodeSelection) {
        reviewContentJob?.cancel()
        _uiState.update { state ->
            state.copy(
                selectedTab = LabTab.Review,
                activeSession = null,
                sync = state.sync.copy(
                    status = SyncStatus.Loading,
                    message = "正在加载 ${selection.workSlug} EP${selection.episode} 的复习材料。",
                ),
            )
        }
        reviewContentJob = viewModelScope.launch {
            val mode = _uiState.value.lessonMode
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    val payload = client.fetchEpisodePayload(selection)
                    val episodePlan = runSuspendCatching { client.fetchEpisodePlan(selection) }.getOrNull()
                    val episodes = client.fetchEpisodes(selection.workSlug)
                    val content = repository.contentFromRemote(
                        selection = selection,
                        vocab = prioritizeCoreVocab(payload.vocab, episodePlan),
                        grammar = payload.grammar,
                        shadowing = payload.shadowing,
                        exercises = payload.exercises,
                        mode = mode,
                        batch = 1,
                    )
                    LoadedReviewContent(content, episodePlan, episodes)
                }
            }
            result.onSuccess { loaded ->
                _uiState.update { state ->
                    state.copy(
                        selection = selection,
                        episodes = normalizeEpisodes(selection.workSlug, loaded.episodes, state.works),
                        focus = loaded.content.focus.copy(
                            streakDays = learningStreakDays(state.progressItems),
                            xp = learningXp(state.progressItems),
                        ),
                        vocab = loaded.content.vocab,
                        grammar = loaded.content.grammar,
                        shadowing = loaded.content.shadowing,
                        exercises = loaded.content.exercises,
                        episodePlan = loaded.episodePlan,
                        scenes = loaded.content.scenes,
                        selectedScene = loaded.content.scenes.firstOrNull() ?: state.selectedScene,
                        sync = state.sync.copy(status = SyncStatus.Success, message = "复习材料已加载。"),
                    )
                }
                practiceReviewTask(task)
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        selectedTab = LabTab.Review,
                        activeSession = null,
                        sync = state.sync.copy(
                            status = SyncStatus.Error,
                            message = "复习材料加载失败：${error.message ?: "网络不可用"}",
                        ),
                    )
                }
            }
        }
    }

    fun updateSettings(settings: LabSettings) {
        store.writeSettings(settings)
        _uiState.update { it.copy(settings = settings) }
        if (settings.cloudSync) {
            val progress = store.readProgress()
            store.writePendingProgress(mergeProgressItems(progress, store.readPendingProgress()))
            progress.map(ProgressItem::toSyncAnswer).forEach(::syncAnswer)
        }
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
                    exercises = state.exercises,
                    scenes = state.scenes,
                )
            } else {
                val remoteContent = repository.content(selection, mode)
                EpisodeContentSnapshot(
                    focus = remoteContent.focus,
                    vocab = remoteContent.vocab,
                    grammar = remoteContent.grammar,
                    shadowing = remoteContent.shadowing,
                    exercises = remoteContent.exercises,
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
                exercises = content.exercises,
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
                    exercises = state.exercises,
                    scenes = state.scenes,
                )
            } else {
                val remoteContent = repository.content(selection, state.lessonMode)
                EpisodeContentSnapshot(
                    focus = remoteContent.focus,
                    vocab = remoteContent.vocab,
                    grammar = remoteContent.grammar,
                    shadowing = remoteContent.shadowing,
                    exercises = remoteContent.exercises,
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
                exercises = content.exercises,
                target = target,
            )
            if (nodes.isEmpty()) {
                return@update state.copy(
                    sync = state.sync.copy(message = "这条复习内容还没有本机训练卡；请先同步当前集资料。"),
                )
            }
            state.copy(
                selectedTab = LabTab.Review,
                activeSession = TrainingSessionKind.Lesson,
                isExerciseLabSession = false,
                activeExerciseLabKind = null,
                selection = selection,
                episodePlan = null,
                episodes = if (sameSelection) state.episodes else repository.episodes(selection.workSlug),
                focus = content.focus.copy(
                    lessonTitle = "复习训练 · ${task.label.ifBlank { target.labelFromContent(content) }}",
                    streakDays = learningStreakDays(state.progressItems),
                    xp = learningXp(state.progressItems),
                ),
                vocab = content.vocab,
                grammar = content.grammar,
                shadowing = content.shadowing,
                exercises = content.exercises,
                scenes = content.scenes,
                selectedScene = content.scenes.firstOrNull() ?: state.selectedScene,
                readAir = if (sameSelection) {
                    state.readAir
                } else {
                    state.readAir.copy(
                        message = "已切到复习所属章节；语言学题库仍按数据库数据筛选。",
                        reviewFocusExerciseId = null,
                        pinnedExerciseId = null,
                    )
                },
                lessonTarget = target,
                activeLessonPathKey = null,
                lessonBatch = 1,
                hasNextLessonBatch = false,
                lesson = LessonEngine.start(nodes),
                sessionXp = 0,
                aiCoach = AiCoachState(),
            )
        }
    }

    private fun startOrdinaryExerciseReview(task: ProgressItem, node: LessonNode) {
        _uiState.update { state ->
            val selection = task.selectionOrFallback(state.selection)
            state.copy(
                selectedTab = LabTab.Review,
                activeSession = TrainingSessionKind.Lesson,
                isExerciseLabSession = false,
                activeExerciseLabKind = null,
                selection = selection,
                focus = state.focus.copy(lessonTitle = "数据库错题复习"),
                lessonTarget = null,
                activeLessonPathKey = null,
                lessonBatch = 1,
                hasNextLessonBatch = false,
                lesson = LessonEngine.start(listOf(node)),
                sessionXp = 0,
                aiCoach = AiCoachState(),
                sync = state.sync.copy(message = "已打开数据库错题：${task.label.ifBlank { node.prompt }}"),
            )
        }
    }

    private fun startReviewReadAir(task: ProgressItem) {
        if (startReviewReadAirFromLoadedCatalog(task, showMissingMessage = false)) return

        _uiState.update { state ->
            state.copy(
                selectedTab = LabTab.Review,
                activeSession = null,
                sync = state.sync.copy(
                    status = SyncStatus.Loading,
                    message = "正在从云端匹配这条语言学复习题。",
                ),
            )
        }
        viewModelScope.launch {
            val result = runSuspendCatching {
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
                            message = "已补齐云端语言学题库，正在进入复习题。",
                            usingFallback = remoteExercises.isEmpty() && state.readAir.usingFallback,
                        ),
                        sync = state.sync.copy(
                            status = SyncStatus.Success,
                            message = if (remoteExercises.isEmpty()) {
                                "数据库暂时没有返回语言学题库。"
                            } else {
                                "已补齐云端语言学题库 ${remoteExercises.size} 道。"
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
                            message = "这条语言学复习题还没在本机题库里，云端匹配失败：${error.message ?: "网络不可用"}",
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
        val exercise = findReviewReadAirExercise(task, snapshot.readAir.exercises)
        if (exercise == null) {
            if (showMissingMessage) {
                _uiState.update { state ->
                    state.copy(
                        selectedTab = LabTab.Review,
                        activeSession = null,
                        sync = state.sync.copy(
                            status = SyncStatus.Error,
                            message = "这条语言学复习题还没在本机题库里；请先更新资料后再试。",
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
        clearPronunciationAttempt()
        reviewContentJob?.cancel()
        val mode = _uiState.value.lessonMode
        val batch = 1
        val content = repository.content(selection, mode, batch)
        lastEpisodesByWork[selection.workSlug] = selection.episode
        store.writeSelection(selection)
        store.writeLastEpisodeForWork(selection)
        _uiState.update { state ->
            state.copy(
                selection = selection,
                episodes = repository.episodes(selection.workSlug),
                focus = content.focus.copy(
                    streakDays = learningStreakDays(state.progressItems),
                    xp = learningXp(state.progressItems),
                ),
                vocab = content.vocab,
                grammar = content.grammar,
                shadowing = content.shadowing,
                exercises = content.exercises,
                episodePlan = null,
                scenes = content.scenes,
                selectedScene = content.scenes.first(),
                readAir = state.readAir.copy(
                    message = "已切换章节；正在按数据库刷新语言学题库。",
                    currentIndex = 0,
                    reviewFocusExerciseId = null,
                    pinnedExerciseId = null,
                    restoreFiltersAfterSession = null,
                    aiCoach = AiCoachState(question = ReadAirAiQuestion),
                ),
                lessonMode = mode,
                lessonBatch = batch,
                lessonTarget = null,
                isExerciseLabSession = false,
                activeExerciseLabKind = null,
                activeLessonPathKey = null,
                hasNextLessonBatch = repository.hasNextLessonBatch(content.vocab, content.grammar, content.shadowing, mode, batch),
                lesson = resumeLessonFromProgress(content.lessonNodes, state.progressItems),
                sessionXp = 0,
                readAirAnswer = "",
                aiCoach = AiCoachState(),
                activeSession = null,
                pronunciationEvaluation = PronunciationEvaluationState(),
            )
        }
    }

    private fun persistOptimisticProgress(
        progressItems: List<ProgressItem>,
        syncPayloads: List<SyncAnswer>,
    ) {
        store.writeProgress(progressItems)
        if (!_uiState.value.settings.cloudSync) return
        val pending = mergeProgressItems(
            syncPayloads.map(SyncAnswer::toProgressItem),
            store.readPendingProgress(),
        )
        store.writePendingProgress(pending)
    }

    private fun syncAnswer(payload: SyncAnswer) {
        if (!_uiState.value.settings.cloudSync) {
            _uiState.update { state ->
                state.copy(sync = state.sync.copy(status = SyncStatus.Success, message = "进度已保存在本机；云端同步已关闭。"))
            }
            return
        }
        val localItem = payload.toProgressItem()
        viewModelScope.launch {
            val result = runSuspendCatching {
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
                        val existing = runSuspendCatching {
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
                val durableSyncedItem = mergeSyncedProgressItem(
                    serverItem = synced.item,
                    localItem = localItem,
                )
                var persistedProgress: List<ProgressItem>? = null
                while (persistedProgress == null) {
                    val state = _uiState.value
                    val nextProgressItems = listOf(durableSyncedItem) + state.progressItems.filterNot {
                        it.sameProgressIdentity(durableSyncedItem)
                    }
                    val nextAuth = state.auth.user?.let { user ->
                        state.auth.copy(
                            status = SyncStatus.Success,
                            message = "已登录：${user.email}；本机已保存进度 ${nextProgressItems.size} 条，复习 ${state.reviewTasks.size} 条。",
                        )
                    } ?: state.auth
                    val nextState = state.copy(
                        progressItems = nextProgressItems,
                        focus = state.focus.copy(
                            streakDays = learningStreakDays(nextProgressItems),
                            xp = learningXp(nextProgressItems),
                        ),
                        auth = nextAuth,
                        sync = state.sync.copy(
                            status = SyncStatus.Success,
                            message = if (synced.recoveredDuplicate) {
                                "进度已存在；已按账号记录继续：${durableSyncedItem.label}"
                            } else {
                                "已保存进度：${durableSyncedItem.label}"
                            },
                            lastSyncedAt = Instant.now().toString(),
                        ),
                    )
                    if (_uiState.compareAndSet(state, nextState)) {
                        persistedProgress = nextProgressItems
                    }
                }
                store.writeProgress(checkNotNull(persistedProgress))
                store.writePendingProgress(store.readPendingProgress().filterNot {
                    it.sameProgressIdentity(durableSyncedItem)
                })
            }
            result.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        sync = state.sync.copy(
                            status = SyncStatus.Error,
                            message = "进度已保存在本机；云端同步失败：${error.message ?: "网络不可用"}",
                        ),
                    )
                }
            }
        }
    }

    private fun flushPendingProgress() {
        val state = _uiState.value
        if (!state.settings.cloudSync || state.auth.user == null) return
        store.readPendingProgress().map(ProgressItem::toSyncAnswer).forEach(::syncAnswer)
    }
}

data class LabUiState(
    val deviceId: String,
    val settings: LabSettings,
    val selectedTab: LabTab = LabTab.Today,
    val activeSession: TrainingSessionKind? = null,
    val exerciseLabLoading: Boolean = false,
    val isExerciseLabSession: Boolean = false,
    val activeExerciseLabKind: LessonExerciseKind? = null,
    val secondaryScreen: SecondaryScreen? = null,
    val deviceCapabilities: DeviceCapabilitySnapshot? = null,
    val deviceCapabilitiesRefreshing: Boolean = false,
    val libraryRevealEpisodeActionsRequest: Int = 0,
    val works: List<WorkOption>,
    val episodes: List<EpisodeOption>,
    val selection: EpisodeSelection,
    val focus: EpisodeFocus,
    val vocab: List<VocabItem>,
    val grammar: List<GrammarPoint>,
    val shadowing: List<ShadowingSentence>,
    val exercises: List<LearningExercise> = emptyList(),
    val episodePlan: EpisodePlan? = null,
    val subtitles: List<SubtitleLine> = emptyList(),
    val subtitleStatus: SyncStatus = SyncStatus.Idle,
    val subtitleMessage: String = "",
    val subtitleFocusLineNo: Int? = null,
    val scenes: List<ReadAirScene>,
    val selectedScene: ReadAirScene,
    val readAir: ReadAirTrainingState,
    val lesson: LessonSession,
    val lessonMode: LessonMode,
    val lessonBatch: Int = 1,
    val lessonTarget: LessonTarget? = null,
    val activeLessonPathKey: String? = null,
    val hasNextLessonBatch: Boolean = false,
    val sessionXp: Int = 0,
    val readAirQuestion: String = "这句话是在表达字面意思，还是在调整关系和语气？",
    val readAirAnswer: String = "",
    val mistakes: List<MistakeRecord> = emptyList(),
    val progressItems: List<ProgressItem> = emptyList(),
    val reviewTasks: List<ProgressItem> = emptyList(),
    val smartReviewPlan: SmartReviewPlan = SmartReviewPlan(),
    val auth: AuthState = AuthState(),
    val sync: SyncSnapshot = SyncSnapshot(),
    val aiCoach: AiCoachState = AiCoachState(),
    val libraryAiTargetKey: String? = null,
    val reviewAiTargetId: String? = null,
    val pronunciationEvaluation: PronunciationEvaluationState = PronunciationEvaluationState(),
)

enum class PronunciationEvaluationPhase {
    Idle,
    Loading,
    Complete,
    Error,
}

data class PronunciationEvaluationState(
    val nodeId: String? = null,
    val phase: PronunciationEvaluationPhase = PronunciationEvaluationPhase.Idle,
    val result: PronunciationEvaluation? = null,
    val message: String = "",
    val canRetry: Boolean = false,
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
    val sessionExerciseIds: Set<String> = emptySet(),
    val sessionBatch: Int? = null,
    val aiCoach: AiCoachState = AiCoachState(question = ReadAirAiQuestion),
    val usingFallback: Boolean = false,
) {
    val scopedExercises: List<LinguisticExercise>
        get() {
            val filterMatched = exercises.filter { exercise ->
            val workMatch = filters.workSlug == ReadAirAllFilter ||
                normalizeReadAirWorkSlug(exercise.workSlug) == normalizeReadAirWorkSlug(filters.workSlug)
            val domainMatch = filters.domain == ReadAirAllFilter || exercise.domain == filters.domain
            val questionTypeMatch = filters.questionType == ReadAirAllFilter || exercise.questionType == filters.questionType
            val difficultyMatch = filters.difficulty == ReadAirAllFilter || exercise.difficulty == filters.difficulty
            val topicMatch = filters.topic == ReadAirAllFilter || exercise.matchesReadAirTopic(filters.topic)
            val episodeMatch = filters.episode == null || exercise.episode == filters.episode
            val focusMatch = reviewFocusExerciseId == null || exercise.id == reviewFocusExerciseId
            val sessionMatch = sessionExerciseIds.isEmpty() || exercise.id in sessionExerciseIds
            workMatch && domainMatch && questionTypeMatch && difficultyMatch && topicMatch && episodeMatch && focusMatch && sessionMatch
            }
            if (sessionExerciseIds.isNotEmpty()) return filterMatched
            val batch = sessionBatch ?: return filterMatched
            return filterMatched.drop((batch - 1) * 7).take(7)
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

    val domainCounts: Map<String, Int>
        get() = exercises
            .filter { exercise -> matchesReadAirFilters(exercise, ignore = ReadAirFilterField.Domain) }
            .groupingBy { it.domain }
            .eachCount()

    val workOptions: List<String>
        get() {
            val options = exercises
                .map { normalizeReadAirWorkSlug(it.workSlug) }
                .filter { it.isNotBlank() }
                .toMutableSet()
            filters.workSlug.takeUnless { it == ReadAirAllFilter }?.let { options.add(normalizeReadAirWorkSlug(it)) }
            return listOf(ReadAirAllFilter) + options.sorted()
        }

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

    val topicOptions: List<String>
        get() = buildList {
            add(ReadAirAllFilter)
            if (exercises.any { it.matchesReadAirTopic(ReadAirCognitiveTopic) }) {
                add(ReadAirCognitiveTopic)
            }
        }

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
        val questionTypeMatch = ignore == ReadAirFilterField.QuestionType ||
            filters.questionType == ReadAirAllFilter ||
            exercise.questionType == filters.questionType
        val difficultyMatch = ignore == ReadAirFilterField.Difficulty ||
            filters.difficulty == ReadAirAllFilter ||
            exercise.difficulty == filters.difficulty
        val topicMatch = ignore == ReadAirFilterField.Topic ||
            filters.topic == ReadAirAllFilter ||
            exercise.matchesReadAirTopic(filters.topic)
        val episodeMatch = ignore == ReadAirFilterField.Episode ||
            filters.episode == null ||
            exercise.episode == filters.episode
        return workMatch && domainMatch && questionTypeMatch && difficultyMatch && topicMatch && episodeMatch
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

private fun pronunciationResultMessage(evaluation: PronunciationEvaluation): String {
    return when (evaluation.assessmentStatus) {
        PronunciationAssessmentStatus.Scored -> "真实测评完成。分数为体验评分 Beta，请结合可靠度一起看。"
        PronunciationAssessmentStatus.Uncertain -> "本次无法稳定判断，请在更安静的环境里再读一次。"
        PronunciationAssessmentStatus.ReRecord -> when (evaluation.audioQuality.status) {
            "too_short" -> "录音太短，请读完整句子。"
            "too_long" -> "录音超过 15 秒，请重新录制。"
            "silent" -> "没有检测到有效语音，请确认麦克风后再录。"
            "too_quiet" -> "音量太小，请靠近麦克风。"
            "clipped" -> "音量过大出现爆音，请稍微远离麦克风。"
            "noisy" -> "环境噪声太大，请换到安静位置。"
            else -> "这段录音暂时无法评分，请重新录制。"
        }
    }
}

private fun pronunciationFailureMessage(error: Throwable): String {
    if (error !is PronunciationApiException) {
        return "发音测评连接失败，录音仍保留在内存中，可以直接重试上传。"
    }
    return when (error.httpStatus) {
        401 -> "评测票据已失效，请直接重试上传。"
        403 -> "评测票据与当前句子不匹配，请重新录音后再试。"
        404 -> "这句话暂未开放真实测评。"
        409 -> "本次录音标识冲突，请重新录音。"
        413 -> "录音文件过大，请缩短后重新录制。"
        429 -> "请求过于频繁，录音仍保留，可以稍后重试上传。"
        502 -> "识别服务暂时不可用，录音仍保留，可以稍后重试上传。"
        503 -> "主站尚未配置发音测评票据，请完成服务端配置后重试。"
        else -> "发音测评暂时失败：${error.message.orEmpty().take(100)}"
    }
}

private enum class ReadAirFilterField {
    Work,
    Domain,
    QuestionType,
    Difficulty,
    Topic,
    Episode,
}

data class ReadAirFilters(
    val workSlug: String = ReadAirAllFilter,
    val domain: String = ReadAirAllFilter,
    val questionType: String = ReadAirAllFilter,
    val difficulty: String = ReadAirAllFilter,
    val topic: String = ReadAirAllFilter,
    val episode: Int? = null,
)

enum class LabTab(val label: String) {
    Today("今日"),
    Lesson("训练"),
    Linguistics("语言学"),
    Library("资料"),
    Review("复盘"),
}

enum class TrainingSessionKind {
    Lesson,
    ReadAir,
}

enum class SecondaryScreen {
    Settings,
    Subtitles,
    SmartReviewQueue,
    AiHistory,
    Search,
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

private data class AnswerCommitEffects(
    val mistakes: List<MistakeRecord>,
    val progressItems: List<ProgressItem>,
    val syncPayloads: List<SyncAnswer>,
)

private data class PendingPronunciationAttempt(
    val nodeId: String,
    val sentenceId: String,
    val attemptId: String,
    val wavBytes: ByteArray,
)

private data class RemoteRefresh(
    val works: List<WorkOption>,
    val episodes: List<EpisodeOption>,
    val content: com.animejapaneselab.nativeapp.data.EpisodeContent,
    val readAirExercises: List<LinguisticExercise>,
    val episodePlan: EpisodePlan?,
)

private data class LoadedReviewContent(
    val content: com.animejapaneselab.nativeapp.data.EpisodeContent,
    val episodePlan: EpisodePlan?,
    val episodes: List<EpisodeOption>,
)

private data class RemoteRefreshRequest(
    val selection: EpisodeSelection,
    val lessonMode: LessonMode,
    val lessonBatch: Int,
) {
    fun matches(state: LabUiState): Boolean {
        return selection == state.selection &&
            lessonMode == state.lessonMode &&
            lessonBatch == state.lessonBatch
    }
}

private fun normalizeEpisodeCounts(works: List<WorkOption>): List<WorkOption> {
    return works.map { work ->
        val knownMax = knownEpisodeCount(work.slug)
        if (knownMax != null && work.episodeCount != knownMax) {
            work.copy(episodeCount = knownMax)
        } else {
            work
        }
    }
}

private fun prioritizeCoreVocab(vocab: List<VocabItem>, episodePlan: EpisodePlan?): List<VocabItem> {
    if (vocab.isEmpty()) return emptyList()
    val byId = vocab.associateBy(VocabItem::id)
    val prioritized = episodePlan?.vocabItemIds.orEmpty().mapNotNull(byId::get).distinctBy(VocabItem::id)
    val prioritizedIds = prioritized.map(VocabItem::id).toSet()
    return prioritized + vocab.filterNot { it.id in prioritizedIds }
}

private fun normalizeEpisodes(
    workSlug: String,
    episodes: List<EpisodeOption>,
    works: List<WorkOption>,
): List<EpisodeOption> {
    val maxEpisode = listOfNotNull(
        works.firstOrNull { it.slug == workSlug }?.episodeCount,
        knownEpisodeCount(workSlug),
        episodes.maxOfOrNull { it.episode },
    ).maxOrNull() ?: return episodes
    val remoteByEpisode = episodes.associateBy { it.episode }
    val fallbackByEpisode = SampleLearningRepository().episodes(workSlug).associateBy { it.episode }
    return (1..maxEpisode).mapNotNull { episode ->
        remoteByEpisode[episode] ?: fallbackByEpisode[episode]
    }
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
    val hasNextLessonBatch: Boolean,
)

private data class RemoteProgressSnapshot(
    val progress: List<ProgressItem> = emptyList(),
    val review: List<ProgressItem> = emptyList(),
)

private data class EpisodeContentSnapshot(
    val focus: EpisodeFocus,
    val vocab: List<VocabItem>,
    val grammar: List<GrammarPoint>,
    val shadowing: List<ShadowingSentence>,
    val exercises: List<LearningExercise>,
    val scenes: List<ReadAirScene>,
)

private fun ProgressItem.selectionOrFallback(fallback: EpisodeSelection): EpisodeSelection {
    return EpisodeSelection(
        workSlug = normalizeReadAirWorkSlug(workSlug.ifBlank { fallback.workSlug }),
        episode = episode.takeIf { it > 0 } ?: fallback.episode,
    )
}

private fun ProgressItem.primarySourceId(): String {
    return listOf(payload["sourceId"], payload["source_id"], payload["source"])
        .firstOrNull { !it.isNullOrBlank() }
        ?.substringBefore(',')
        ?.trim()
        .orEmpty()
        .ifBlank { itemId }
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
    val persistedReadAirAnswers = restoreReadAirAnswers(
        exercises = readAir.exercises,
        progressItems = snapshot.progress,
        inMemoryAnswers = readAir.selectedAnswers,
    )
    return copy(
        progressItems = snapshot.progress,
        reviewTasks = snapshot.review,
        focus = focus.copy(
            streakDays = learningStreakDays(snapshot.progress),
            xp = learningXp(snapshot.progress),
        ),
        readAir = readAir.copy(
            selectedAnswers = persistedReadAirAnswers,
            pinnedExerciseId = readAir.pinnedExerciseId?.takeIf { id ->
                readAir.exercises.any { it.id == id }
            },
        ),
    )
}

/**
 * Total XP derived from every recorded answer: mastered items earn the most,
 * struggling ones still credit the attempt. Applied wherever streak is (see
 * the focus.copy call sites) so the HUD numbers stay honest instead of the
 * repository's placeholder.
 */
internal fun learningXp(progressItems: List<ProgressItem>): Int {
    return progressItems.sumOf { item ->
        when (item.state) {
            ReviewState.Good, ReviewState.Known -> 10
            ReviewState.Ok -> 6
            ReviewState.Fuzzy -> 4
            ReviewState.Bad, ReviewState.Unknown -> 2
        }.toInt()
    }
}

private fun learningStreakDays(
    progressItems: List<ProgressItem>,
    zoneId: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zoneId),
): Int {
    val activityDates = progressItems
        .mapNotNull { item -> item.lastReviewedAt.toActivityDate(zoneId) }
        .toSet()
    if (activityDates.isEmpty()) return 0

    var cursor = when {
        today in activityDates -> today
        today.minusDays(1) in activityDates -> today.minusDays(1)
        else -> return 0
    }
    var streak = 0
    while (cursor in activityDates) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}

private fun String.toActivityDate(zoneId: ZoneId): LocalDate? {
    val raw = trim()
    if (raw.isBlank()) return null
    return try {
        Instant.parse(raw).atZone(zoneId).toLocalDate()
    } catch (_: DateTimeParseException) {
        runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
    }
}

internal fun restoreReadAirAnswers(
    exercises: List<LinguisticExercise>,
    progressItems: List<ProgressItem>,
    inMemoryAnswers: Map<String, String> = emptyMap(),
): Map<String, String> {
    val exerciseIds = exercises.map { it.id }.toSet()
    val persistedAnswers = progressItems
        .filter { it.itemType == "exercise" && it.itemId in exerciseIds }
        .mapNotNull { item ->
            val selected = item.payload["selected"].orEmpty()
            if (selected.isBlank()) null else item.itemId to selected
        }
        .toMap()
    val matchingInMemoryAnswers = inMemoryAnswers.filterKeys { it in exerciseIds }
    return persistedAnswers + matchingInMemoryAnswers
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

private fun LessonExerciseKind.defaultLessonMode(): LessonMode {
    return when (this) {
        LessonExerciseKind.PairMatch,
        LessonExerciseKind.SingleChoice -> LessonMode.Vocab
        LessonExerciseKind.Cloze -> LessonMode.Grammar
        LessonExerciseKind.TranslationOrder,
        LessonExerciseKind.AudioOrder,
        LessonExerciseKind.Shadowing -> LessonMode.Shadowing
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
        guidebook = "正在准备本集词汇、语法、跟读和语言学题库。",
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
        context = "正在准备当前集语言学练习。",
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

private fun LessonNode.progressItemId(): String {
    // Each Android interaction node owns its progress record. sourceId remains in the payload
    // so material-level path progress can still aggregate it.
    return id
}

private fun LessonNode.buildLessonProgressPayload(selected: String, expected: String): JSONObject {
    return JSONObject()
        .put("nodeId", id)
        .put("sourceId", sourceId)
        .put("sourceKind", sourceKind)
        .put("typeLabel", typeLabel)
        .put("sourceLabel", sourceLabel)
        .put("label", prompt.take(90))
        .put("prompt", prompt)
        .put("selected", selected)
        .put("expected", expected)
}

private fun LessonNode.aiKind(): String {
    return when (progressType()) {
        "vocab" -> "vocab"
        "grammar" -> "grammar"
        "sentence" -> "sentence"
        "exercise" -> "exercise"
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
        typeLabel = "语言学题",
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

internal fun LinguisticExercise.matchesReadAirTopic(topic: String): Boolean {
    if (topic == ReadAirAllFilter) return true
    if (topic != ReadAirCognitiveTopic) return false
    val searchable = listOf(
        phenomenonKey,
        phenomenonNameZh,
        phenomenonDefinitionZh,
        prompt,
    ).joinToString(" ").lowercase()
    return listOf("认知", "隐喻", "metaphor", "框架").any(searchable::contains)
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

private fun ProgressItem.toSyncAnswer(): SyncAnswer {
    val json = JSONObject()
    payload.forEach(json::put)
    return SyncAnswer(
        itemId = itemId,
        itemType = itemType,
        selection = EpisodeSelection(workSlug = workSlug, episode = episode),
        state = state,
        label = label,
        payload = json,
    )
}

internal fun mergeProgressItems(vararg sources: List<ProgressItem>): List<ProgressItem> {
    val merged = linkedMapOf<String, ProgressItem>()
    sources.asSequence().flatten().forEach { candidate ->
        val key = "${candidate.itemType}\u0000${candidate.workSlug}\u0000${candidate.episode}\u0000${candidate.itemId}"
        val current = merged[key]
        merged[key] = if (current == null) candidate else mergeProgressVersions(current, candidate)
    }
    return merged.values.sortedByDescending(ProgressItem::lastReviewedAt)
}

internal fun mergeSyncedProgressItem(
    serverItem: ProgressItem,
    localItem: ProgressItem,
): ProgressItem {
    return serverItem.copy(
        workSlug = serverItem.workSlug.ifBlank { localItem.workSlug },
        episode = serverItem.episode.takeIf { it > 0 } ?: localItem.episode,
        label = serverItem.label.ifBlank { localItem.label },
        lastReviewedAt = serverItem.lastReviewedAt.ifBlank { localItem.lastReviewedAt },
        payload = localItem.payload + serverItem.payload,
    )
}

private fun mergeProgressVersions(
    left: ProgressItem,
    right: ProgressItem,
): ProgressItem {
    val (older, newer) = if (right.lastReviewedAt >= left.lastReviewedAt) {
        left to right
    } else {
        right to left
    }
    return newer.copy(
        label = newer.label.ifBlank { older.label },
        payload = older.payload + newer.payload,
    )
}

private suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

private fun pathNodeProgressId(selection: EpisodeSelection, pathNodeKey: String): String {
    return "path-node:${selection.workSlug}:${selection.episode}:$pathNodeKey"
}

private fun ProgressItem.sameProgressIdentity(other: ProgressItem): Boolean {
    return itemId == other.itemId &&
        itemType == other.itemType &&
        workSlug == other.workSlug &&
        episode == other.episode
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
