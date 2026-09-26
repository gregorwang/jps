package com.animejapaneselab.nativeapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animejapaneselab.nativeapp.data.AiCoachState
import com.animejapaneselab.nativeapp.data.AuthUser
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import com.animejapaneselab.nativeapp.data.EpisodeContentCache
import com.animejapaneselab.nativeapp.data.EpisodeFocus
import com.animejapaneselab.nativeapp.data.EpisodeOption
import com.animejapaneselab.nativeapp.data.EpisodePlan
import com.animejapaneselab.nativeapp.data.EpisodeSelection
import com.animejapaneselab.nativeapp.data.FoundationDomain
import com.animejapaneselab.nativeapp.data.FoundationPackQuery
import com.animejapaneselab.nativeapp.data.FoundationQuestion
import com.animejapaneselab.nativeapp.data.FoundationQuestionPack
import com.animejapaneselab.nativeapp.data.FoundationQuestionQuery
import com.animejapaneselab.nativeapp.data.FoundationStage
import com.animejapaneselab.nativeapp.data.FoundationTopic
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
import com.animejapaneselab.nativeapp.data.buildFoundationProgressPayload
import com.animejapaneselab.nativeapp.data.buildLinguisticProgressPayload
import com.animejapaneselab.nativeapp.domain.LessonEngine
import com.animejapaneselab.nativeapp.domain.LessonSession
import com.animejapaneselab.nativeapp.domain.SmartReviewPlan
import com.animejapaneselab.nativeapp.domain.buildSmartReviewPlan
import com.animejapaneselab.nativeapp.domain.resumeLessonFromProgress
import com.animejapaneselab.nativeapp.platform.DeviceCapabilityReader
import com.animejapaneselab.nativeapp.platform.StudyReminder
import com.animejapaneselab.nativeapp.platform.DeviceCapabilitySnapshot
import com.animejapaneselab.nativeapp.ui.foundation.FoundationTrainingError
import com.animejapaneselab.nativeapp.ui.foundation.fetchAllFoundationTopics
import com.animejapaneselab.nativeapp.ui.foundation.FoundationTrainingPhase
import com.animejapaneselab.nativeapp.ui.foundation.FoundationTrainingState
import com.animejapaneselab.nativeapp.ui.foundation.LinguisticsTrack
import com.animejapaneselab.nativeapp.ui.foundation.answerFoundationQuestion
import com.animejapaneselab.nativeapp.ui.foundation.applyFoundationCatalog
import com.animejapaneselab.nativeapp.ui.foundation.applyFoundationQuestions
import com.animejapaneselab.nativeapp.ui.foundation.beginFoundationCatalogLoad
import com.animejapaneselab.nativeapp.ui.foundation.failFoundationLoad
import com.animejapaneselab.nativeapp.ui.foundation.focusFoundationReviewQuestion
import com.animejapaneselab.nativeapp.ui.foundation.nextFoundationQuestion as reduceNextFoundationQuestion
import com.animejapaneselab.nativeapp.ui.foundation.previousFoundationQuestion as reducePreviousFoundationQuestion
import com.animejapaneselab.nativeapp.ui.foundation.restartFoundationQuestions as reduceRestartFoundationQuestions
import com.animejapaneselab.nativeapp.ui.foundation.restoreFoundationAnswers
import com.animejapaneselab.nativeapp.ui.foundation.selectFoundationDomain as reduceFoundationDomain
import com.animejapaneselab.nativeapp.ui.foundation.selectFoundationPack as reduceFoundationPack
import com.animejapaneselab.nativeapp.ui.foundation.selectFoundationStage as reduceFoundationStage
import com.animejapaneselab.nativeapp.ui.foundation.selectFoundationTopic as reduceFoundationTopic
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
    private val contentCache = EpisodeContentCache(application.filesDir)
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
    private var foundationCatalogJob: Job? = null
    private var foundationQuestionJob: Job? = null
    private var pendingFoundationReviewTask: ProgressItem? = null
    private var pronunciationEvaluationJob: Job? = null
    private var pendingPronunciationAttempt: PendingPronunciationAttempt? = null

    private val _uiState = MutableStateFlow(
        LabUiState(
            deviceId = deviceId,
            settings = initialSettings,
            // A cached, server-confirmed user skips the login gate on launch; the
            // background refreshAuthState() below re-validates and signs out on 401.
            auth = AuthState(user = store.readCachedUser()),
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
            todayLineRevealedOn = store.readTodayLineRevealedOn(),
            eyecatchPlayedOn = store.readEyecatchPlayedOn(),
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
        return RemoteLabClient(
            _uiState.value.settings.apiBaseUrl,
            store.readSessionCookie(),
            contentCache = contentCache,
        )
    }

    private fun ensureFoundationCatalogLoaded() {
        when (_uiState.value.foundation.phase) {
            FoundationTrainingPhase.Idle,
            FoundationTrainingPhase.Error -> refreshFoundationCatalog()
            FoundationTrainingPhase.LoadingCatalog,
            FoundationTrainingPhase.LoadingQuestions,
            FoundationTrainingPhase.Ready -> Unit
        }
    }

    fun refreshFoundationCatalog() {
        foundationCatalogJob?.cancel()
        foundationQuestionJob?.cancel()
        _uiState.update { state ->
            state.copy(foundation = beginFoundationCatalogLoad(state.foundation))
        }
        foundationCatalogJob = viewModelScope.launch {
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    val client = remoteClient()
                    FoundationCatalogSnapshot(
                        topics = client.fetchAllFoundationTopics(),
                        packs = client.fetchAllFoundationPacks(),
                    )
                }
            }
            result.fold(
                onSuccess = { snapshot ->
                    var selectedPackId: String? = null
                    _uiState.update { state ->
                        val foundation = applyFoundationCatalog(
                            state = state.foundation,
                            topics = snapshot.topics,
                            packs = snapshot.packs,
                        )
                        selectedPackId = foundation.filters.packId
                        state.copy(foundation = foundation)
                    }
                    if (pendingFoundationReviewTask != null) {
                        resolvePendingFoundationReviewTask()
                    } else {
                        selectedPackId?.let(::loadFoundationQuestions)
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            foundation = failFoundationLoad(
                                state.foundation,
                                error.toFoundationTrainingError(),
                            ),
                        )
                    }
                },
            )
        }
    }

    fun selectFoundationPack(packId: String) {
        var shouldLoad = false
        _uiState.update { state ->
            val foundation = reduceFoundationPack(state.foundation, packId)
            shouldLoad = foundation !== state.foundation
            state.copy(foundation = foundation)
        }
        if (shouldLoad) loadFoundationQuestions(packId)
    }

    fun selectFoundationDomain(domain: FoundationDomain?) {
        _uiState.update { state ->
            state.copy(foundation = reduceFoundationDomain(state.foundation, domain))
        }
    }

    fun selectFoundationTopic(topicId: String?) {
        _uiState.update { state ->
            state.copy(foundation = reduceFoundationTopic(state.foundation, topicId))
        }
    }

    fun selectFoundationStage(stage: FoundationStage?) {
        _uiState.update { state ->
            state.copy(foundation = reduceFoundationStage(state.foundation, stage))
        }
    }

    fun submitFoundationAnswer(optionId: String) {
        var syncPayload: SyncAnswer? = null
        var persistedProgress: List<ProgressItem>? = null
        while (syncPayload == null) {
            val state = _uiState.value
            val question = state.foundation.currentQuestion ?: return
            if (state.foundation.selectedAnswers.containsKey(question.id)) return
            val nextFoundation = answerFoundationQuestion(state.foundation, optionId)
            val correct = question.isCorrect(optionId)
            val candidate = SyncAnswer(
                itemId = question.id,
                itemType = "exercise",
                selection = null,
                state = if (correct) ReviewState.Good else ReviewState.Bad,
                label = question.promptZh.take(90),
                payload = buildFoundationProgressPayload(question, optionId),
            )
            val progressItem = candidate.toProgressItem()
            val nextProgressItems = listOf(progressItem) + state.progressItems.filterNot {
                it.sameProgressIdentity(progressItem)
            }
            val nextState = state.copy(
                foundation = nextFoundation,
                sessionXp = state.sessionXp + if (correct) 8 else 0,
                progressItems = nextProgressItems,
            )
            if (_uiState.compareAndSet(state, nextState)) {
                syncPayload = candidate
                persistedProgress = nextProgressItems
            }
        }
        val committedPayload = checkNotNull(syncPayload)
        val committedProgress = checkNotNull(persistedProgress)
        persistOptimisticProgress(committedProgress, listOf(committedPayload))
        syncAnswer(committedPayload)
    }

    fun nextFoundationQuestion() {
        _uiState.update { state ->
            state.copy(foundation = reduceNextFoundationQuestion(state.foundation))
        }
    }

    fun previousFoundationQuestion() {
        _uiState.update { state ->
            state.copy(foundation = reducePreviousFoundationQuestion(state.foundation))
        }
    }

    fun restartFoundationQuestions() {
        _uiState.update { state ->
            state.copy(foundation = reduceRestartFoundationQuestions(state.foundation))
        }
    }

    private fun loadFoundationQuestions(packId: String) {
        val catalogState = _uiState.value.foundation
        val pack = catalogState.packs.firstOrNull { it.id == packId } ?: return
        val knownTopicIds = catalogState.topics.mapTo(mutableSetOf(), FoundationTopic::id)
        foundationQuestionJob?.cancel()
        foundationQuestionJob = viewModelScope.launch {
            val result = runSuspendCatching {
                withContext(Dispatchers.IO) {
                    remoteClient().fetchAllFoundationQuestions(packId).also { questions ->
                        require(questions.size == pack.questionCount) {
                            "Foundation question count does not match its pack"
                        }
                        require(questions.map(FoundationQuestion::topicId).distinct().size == pack.topicCount) {
                            "Foundation topic count does not match its pack"
                        }
                        require(questions.all { it.topicId in knownTopicIds }) {
                            "Foundation question references an unknown topic"
                        }
                    }
                }
            }
            _uiState.update { state ->
                if (state.foundation.filters.packId != packId) return@update state
                result.fold(
                    onSuccess = { questions ->
                        val restoredAnswers = restoreFoundationAnswers(
                            questions = questions,
                            progressItems = state.progressItems,
                        )
                        val applied = applyFoundationQuestions(
                            state = state.foundation,
                            packId = packId,
                            questions = questions,
                        )
                        state.copy(
                            foundation = applied.copy(
                                selectedAnswers = applied.selectedAnswers + restoredAnswers,
                            ),
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            foundation = failFoundationLoad(
                                state.foundation,
                                error.toFoundationTrainingError(),
                            ),
                        )
                    },
                )
            }
            if (result.isSuccess) resolvePendingFoundationReviewTask()
        }
    }

    private fun resolvePendingFoundationReviewTask() {
        val task = pendingFoundationReviewTask ?: return
        val packId = task.payload["packId"].orEmpty()
        val topicId = task.payload["topicId"].orEmpty()
        val stage = FoundationStage.entries.firstOrNull {
            it.wireValue == task.payload["stage"]
        }
        val state = _uiState.value
        if (packId.isBlank() || topicId.isBlank() || stage == null) {
            pendingFoundationReviewTask = null
            _uiState.update {
                it.copy(sync = it.sync.copy(message = "基础语言学复习记录缺少题包、主题或阶段。"))
            }
            return
        }
        if (state.foundation.packs.isEmpty()) return
        if (state.foundation.packs.none { it.id == packId }) {
            pendingFoundationReviewTask = null
            _uiState.update {
                it.copy(sync = it.sync.copy(message = "对应的基础语言学题包尚未发布。"))
            }
            return
        }
        if (
            state.foundation.filters.packId != packId ||
            state.foundation.questions.none { it.packId == packId }
        ) {
            val nextFoundation = if (state.foundation.filters.packId == packId) {
                state.foundation
            } else {
                reduceFoundationPack(state.foundation, packId)
            }
            _uiState.update { it.copy(foundation = nextFoundation) }
            loadFoundationQuestions(packId)
            return
        }
        val focused = focusFoundationReviewQuestion(
            state = state.foundation,
            questionId = task.itemId,
            packId = packId,
            topicId = topicId,
            stage = stage,
        )
        pendingFoundationReviewTask = null
        _uiState.update {
            if (focused == null) {
                it.copy(sync = it.sync.copy(message = "对应的基础语言学复习题已不存在或身份不一致。"))
            } else {
                it.copy(
                    selectedTab = LabTab.Learn,
                    learnSection = LearnSection.Linguistics,
                    linguisticsTrack = LinguisticsTrack.Foundation,
                    foundation = focused,
                )
            }
        }
    }

    private fun RemoteLabClient.fetchAllFoundationPacks(): List<FoundationQuestionPack> {
        val result = mutableListOf<FoundationQuestionPack>()
        val seenCursors = mutableSetOf<String>()
        var cursor: String? = null
        do {
            val page = fetchFoundationPacks(FoundationPackQuery(cursor = cursor))
            result += page.items
            cursor = page.page.nextCursor
            if (page.page.hasMore) {
                require(cursor != null && seenCursors.add(cursor)) {
                    "Foundation pack pagination returned an invalid cursor"
                }
            }
        } while (page.page.hasMore)
        require(result.distinctBy(FoundationQuestionPack::id).size == result.size) {
            "Foundation pack pagination returned duplicate ids"
        }
        return result
    }

    private fun RemoteLabClient.fetchAllFoundationQuestions(packId: String): List<FoundationQuestion> {
        val result = mutableListOf<FoundationQuestion>()
        val seenCursors = mutableSetOf<String>()
        var cursor: String? = null
        do {
            val page = fetchFoundationQuestions(
                FoundationQuestionQuery(
                    packId = packId,
                    cursor = cursor,
                ),
            )
            result += page.items
            cursor = page.page.nextCursor
            if (page.page.hasMore) {
                require(cursor != null && seenCursors.add(cursor)) {
                    "Foundation question pagination returned an invalid cursor"
                }
            }
        } while (page.page.hasMore)
        require(result.distinctBy(FoundationQuestion::id).size == result.size) {
            "Foundation question pagination returned duplicate ids"
        }
        return result
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
            LabTab.Library -> ensureFallbackReadAirCatalogLoaded()
            LabTab.Learn -> if (_uiState.value.learnSection == LearnSection.Linguistics) {
                ensureLinguisticsTrackLoaded(_uiState.value.linguisticsTrack)
            }
            LabTab.Today,
            LabTab.Review -> Unit
        }
    }

    /** 学ぶ top text tabs: 課程 / 言語学. Also switches to the 学ぶ tab. */
    fun selectLearnSection(section: LearnSection) {
        _uiState.update {
            it.copy(selectedTab = LabTab.Learn, learnSection = section, activeSession = null, secondaryScreen = null)
        }
        if (section == LearnSection.Linguistics) ensureLinguisticsTrackLoaded(_uiState.value.linguisticsTrack)
    }

    private fun ensureLinguisticsTrackLoaded(track: LinguisticsTrack) {
        when (track) {
            LinguisticsTrack.AnimeCorpus -> ensureFallbackReadAirCatalogLoaded()
            LinguisticsTrack.Foundation -> ensureFoundationCatalogLoaded()
            LinguisticsTrack.Conjugation -> Unit
        }
    }

    fun selectLinguisticsTrack(track: LinguisticsTrack) {
        _uiState.update { it.copy(linguisticsTrack = track) }
        when (track) {
            LinguisticsTrack.AnimeCorpus -> ensureFallbackReadAirCatalogLoaded()
            LinguisticsTrack.Foundation -> ensureFoundationCatalogLoaded()
            LinguisticsTrack.Conjugation -> Unit
        }
    }

    /** 今日の一句 played its once-a-day reveal on [date] (ISO yyyy-MM-dd). */
    fun markTodayLineRevealed(date: String) {
        if (_uiState.value.todayLineRevealedOn == date) return
        store.writeTodayLineRevealedOn(date)
        _uiState.update { it.copy(todayLineRevealedOn = date) }
    }

    /** The full アイキャッチ for [workSlug]/[episode] played on [date]; older days are dropped. */
    fun markEyecatchPlayed(workSlug: String, episode: Int, date: String) {
        val key = "$workSlug:$episode"
        var written: Map<String, String>? = null
        _uiState.update { state ->
            if (state.eyecatchPlayedOn[key] == date) return@update state
            val next = state.eyecatchPlayedOn.filterValues { it == date } + (key to date)
            written = next
            state.copy(eyecatchPlayedOn = next)
        }
        written?.let(store::writeEyecatchPlayedOn)
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
                state.copy(selectedTab = LabTab.Learn, learnSection = LearnSection.Course, activeSession = null, secondaryScreen = null)
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
            val sessionRejected = result.exceptionOrNull()?.message?.startsWith("HTTP 401") == true
            when {
                result.isSuccess -> store.writeCachedUser(result.getOrNull()?.first)
                sessionRejected -> store.writeCachedUser(null)
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
                            auth = state.auth.copy(
                                status = SyncStatus.Error,
                                user = if (sessionRejected) null else state.auth.user,
                                message = message,
                            ),
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
                    store.writeCachedUser(login.user)
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
                selectedTab = LabTab.Learn,
                learnSection = LearnSection.Course,
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

    /** 跳过: leave the current question unanswered and move on (nothing is recorded). */
    fun skipLessonNode() {
        clearPronunciationAttempt()
        _uiState.update { state ->
            val lesson = state.lesson
            if (lesson.feedback != null || lesson.isComplete) return@update state
            state.copy(
                lesson = lesson.copy(index = (lesson.index + 1).coerceAtMost(lesson.nodes.size)),
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

    /** 跳过: leave the current read-air question unanswered and show the next one in the queue. */
    fun skipReadAirExercise() {
        _uiState.update { state ->
            state.copy(readAir = state.readAir.skipCurrentExercise())
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
            selectLearnSection(LearnSection.Course)
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
                    selectedTab = LabTab.Learn,
                learnSection = LearnSection.Course,
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
                    selectedTab = LabTab.Learn,
                learnSection = LearnSection.Course,
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
        if (task.payload["track"] == "foundation") {
            pendingFoundationReviewTask = task
            _uiState.update {
                it.copy(
                    selectedTab = LabTab.Learn,
                    learnSection = LearnSection.Linguistics,
                    linguisticsTrack = LinguisticsTrack.Foundation,
                    activeSession = null,
                    secondaryScreen = null,
                )
            }
            when (_uiState.value.foundation.phase) {
                FoundationTrainingPhase.Idle,
                FoundationTrainingPhase.Error -> refreshFoundationCatalog()
                FoundationTrainingPhase.LoadingCatalog,
                FoundationTrainingPhase.LoadingQuestions -> Unit
                FoundationTrainingPhase.Ready -> resolvePendingFoundationReviewTask()
            }
            return
        }
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
            else -> selectLearnSection(LearnSection.Course)
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
        val reminderChanged = settings.studyReminder != _uiState.value.settings.studyReminder ||
            settings.studyReminderHour != _uiState.value.settings.studyReminderHour
        store.writeSettings(settings)
        if (reminderChanged) StudyReminder.sync(getApplication())
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
        if (syncPayloads.isNotEmpty()) {
            val wrong = syncPayloads.any { it.state == ReviewState.Bad || it.state == ReviewState.Unknown }
            StudyLog.record(getApplication(), answers = 1, correct = if (wrong) 0 else 1)
        }
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
