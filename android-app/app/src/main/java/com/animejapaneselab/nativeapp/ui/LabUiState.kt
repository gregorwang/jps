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

data class LabUiState(
    val deviceId: String,
    val settings: LabSettings,
    val selectedTab: LabTab = LabTab.Today,
    val learnSection: LearnSection = LearnSection.Course,
    val activeSession: TrainingSessionKind? = null,
    val exerciseLabLoading: Boolean = false,
    val isExerciseLabSession: Boolean = false,
    val activeExerciseLabKind: LessonExerciseKind? = null,
    val secondaryScreen: SecondaryScreen? = null,
    val deviceCapabilities: DeviceCapabilitySnapshot? = null,
    val deviceCapabilitiesRefreshing: Boolean = false,
    /** ISO date on which 今日の一句 already played its reveal (persisted). */
    val todayLineRevealedOn: String? = null,
    /** `workSlug:episode` → ISO date of the last full アイキャッチ (persisted, today's entries only). */
    val eyecatchPlayedOn: Map<String, String> = emptyMap(),
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
    val linguisticsTrack: LinguisticsTrack = LinguisticsTrack.AnimeCorpus,
    val foundation: FoundationTrainingState = FoundationTrainingState(),
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

/** Moves to the next unanswered exercise (wrapping); the skipped one stays in the queue. */
internal fun ReadAirTrainingState.skipCurrentExercise(): ReadAirTrainingState {
    val size = filteredExercises.size
    val exercise = currentExercise ?: return this
    if (size <= 1 || selectedAnswerFor(exercise.id).isNotBlank()) return this
    return copy(
        pinnedExerciseId = null,
        currentIndex = (currentIndex.coerceIn(0, size - 1) + 1) % size,
        aiCoach = aiCoach.copy(status = SyncStatus.Idle, answer = "", result = null),
    )
}

internal fun mergeReadAirExercises(
    primary: List<LinguisticExercise>,
    secondary: List<LinguisticExercise>,
): List<LinguisticExercise> {
    return (primary + secondary).distinctBy { it.id }
}

internal fun Throwable.loginFailureMessage(): String {
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

internal fun pronunciationResultMessage(evaluation: PronunciationEvaluation): String {
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

internal fun pronunciationFailureMessage(error: Throwable): String {
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

internal enum class ReadAirFilterField {
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

/** v3 bottom tabs: Japanese label + Chinese caption. */
enum class LabTab(val jp: String, val label: String) {
    Today("今日", "今日"),
    Learn("学ぶ", "学习"),
    Library("辞書", "资料"),
    Review("復習", "复盘"),
}

/** Sub-state of [LabTab.Learn]: the 課程 / 言語学 text tabs. */
enum class LearnSection { Course, Linguistics }

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
