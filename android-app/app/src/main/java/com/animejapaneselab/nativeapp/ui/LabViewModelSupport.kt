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

internal data class SyncAnswer(
    val itemId: String,
    val itemType: String,
    val selection: EpisodeSelection?,
    val state: ReviewState,
    val label: String,
    val payload: JSONObject? = null,
)

internal data class FoundationCatalogSnapshot(
    val topics: List<FoundationTopic>,
    val packs: List<FoundationQuestionPack>,
)

internal data class AnswerCommitEffects(
    val mistakes: List<MistakeRecord>,
    val progressItems: List<ProgressItem>,
    val syncPayloads: List<SyncAnswer>,
)

internal data class PendingPronunciationAttempt(
    val nodeId: String,
    val sentenceId: String,
    val attemptId: String,
    val wavBytes: ByteArray,
)

internal data class RemoteRefresh(
    val works: List<WorkOption>,
    val episodes: List<EpisodeOption>,
    val content: com.animejapaneselab.nativeapp.data.EpisodeContent,
    val readAirExercises: List<LinguisticExercise>,
    val episodePlan: EpisodePlan?,
)

internal data class LoadedReviewContent(
    val content: com.animejapaneselab.nativeapp.data.EpisodeContent,
    val episodePlan: EpisodePlan?,
    val episodes: List<EpisodeOption>,
)

internal data class RemoteRefreshRequest(
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

internal fun normalizeEpisodeCounts(works: List<WorkOption>): List<WorkOption> {
    return works.map { work ->
        val knownMax = knownEpisodeCount(work.slug)
        if (knownMax != null && work.episodeCount != knownMax) {
            work.copy(episodeCount = knownMax)
        } else {
            work
        }
    }
}

internal fun prioritizeCoreVocab(vocab: List<VocabItem>, episodePlan: EpisodePlan?): List<VocabItem> {
    if (vocab.isEmpty()) return emptyList()
    val byId = vocab.associateBy(VocabItem::id)
    val prioritized = episodePlan?.vocabItemIds.orEmpty().mapNotNull(byId::get).distinctBy(VocabItem::id)
    val prioritizedIds = prioritized.map(VocabItem::id).toSet()
    return prioritized + vocab.filterNot { it.id in prioritizedIds }
}

internal fun normalizeEpisodes(
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

internal fun knownEpisodeCount(workSlug: String): Int? {
    return when (workSlug) {
        "k-on" -> 14
        "re-zero", "rezero" -> 66
        else -> null
    }
}

internal data class InitialEpisodeContent(
    val content: com.animejapaneselab.nativeapp.data.EpisodeContent,
    val hasNextLessonBatch: Boolean,
)

internal data class RemoteProgressSnapshot(
    val progress: List<ProgressItem> = emptyList(),
    val review: List<ProgressItem> = emptyList(),
)

internal data class EpisodeContentSnapshot(
    val focus: EpisodeFocus,
    val vocab: List<VocabItem>,
    val grammar: List<GrammarPoint>,
    val shadowing: List<ShadowingSentence>,
    val exercises: List<LearningExercise>,
    val scenes: List<ReadAirScene>,
)

internal fun ProgressItem.selectionOrFallback(fallback: EpisodeSelection): EpisodeSelection {
    return EpisodeSelection(
        workSlug = normalizeReadAirWorkSlug(workSlug.ifBlank { fallback.workSlug }),
        episode = episode.takeIf { it > 0 } ?: fallback.episode,
    )
}

internal fun ProgressItem.primarySourceId(): String {
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

internal fun reviewLineNumbers(task: ProgressItem): Set<Int> {
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

internal fun normalizeReviewText(value: String): String {
    return value
        .lowercase()
        .filter { it.isLetterOrDigit() || Character.UnicodeScript.of(it.code) in reviewTextScripts }
}

internal val reviewTextScripts = setOf(
    Character.UnicodeScript.HIRAGANA,
    Character.UnicodeScript.KATAKANA,
    Character.UnicodeScript.HAN,
)

internal fun LabUiState.withRemoteProgressSnapshot(snapshot: RemoteProgressSnapshot): LabUiState {
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

internal fun learningStreakDays(
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

internal fun String.toActivityDate(zoneId: ZoneId): LocalDate? {
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

internal fun LessonTarget.labelFrom(state: LabUiState): String {
    return when (this) {
        is LessonTarget.Vocab -> state.vocab.firstOrNull { it.id == id }?.surface ?: "词汇"
        is LessonTarget.Grammar -> state.grammar.firstOrNull { it.id == id }?.pattern ?: "语法"
        is LessonTarget.Sentence -> state.shadowing.firstOrNull { it.id == id }?.ja ?: "跟读句"
    }
}

internal fun LessonTarget.labelFromContent(content: EpisodeContentSnapshot): String {
    return when (this) {
        is LessonTarget.Vocab -> content.vocab.firstOrNull { it.id == id }?.surface ?: "词汇"
        is LessonTarget.Grammar -> content.grammar.firstOrNull { it.id == id }?.pattern ?: "语法"
        is LessonTarget.Sentence -> content.shadowing.firstOrNull { it.id == id }?.ja ?: "跟读句"
    }
}

internal fun LessonExerciseKind.defaultLessonMode(): LessonMode {
    return when (this) {
        LessonExerciseKind.PairMatch,
        LessonExerciseKind.SingleChoice -> LessonMode.Vocab
        LessonExerciseKind.Cloze -> LessonMode.Grammar
        LessonExerciseKind.TranslationOrder,
        LessonExerciseKind.AudioOrder,
        LessonExerciseKind.Shadowing -> LessonMode.Shadowing
    }
}

internal fun lessonTitle(mode: LessonMode, focus: EpisodeFocus, batch: Int): String {
    val batchPart = if (batch > 1) " 第 $batch 批" else ""
    return "${mode.titleLabel}$batchPart · ${focus.episodeLabel}"
}

internal fun lightweightFocus(selection: EpisodeSelection, works: List<WorkOption>): EpisodeFocus {
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

internal fun lightweightReadAirScene(selection: EpisodeSelection): ReadAirScene {
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

internal fun upsertMistake(
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

internal fun LessonNode.progressType(): String {
    return sourceKind
}

internal fun LessonNode.progressItemId(): String {
    // Each Android interaction node owns its progress record. sourceId remains in the payload
    // so material-level path progress can still aggregate it.
    return id
}

internal fun LessonNode.buildLessonProgressPayload(selected: String, expected: String): JSONObject {
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

internal fun LessonNode.aiKind(): String {
    return when (progressType()) {
        "vocab" -> "vocab"
        "grammar" -> "grammar"
        "sentence" -> "sentence"
        "exercise" -> "exercise"
        else -> "linguistic"
    }
}

internal fun LessonNode.aiText(): String = prompt.ifBlank { expectedAnswer }

internal fun buildAiContext(node: LessonNode, question: String): String {
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

internal fun upsertReadAirMistake(
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

internal fun selectionForExercise(exercise: LinguisticExercise, fallback: EpisodeSelection): EpisodeSelection {
    return EpisodeSelection(
        workSlug = exercise.workSlug.ifBlank { fallback.workSlug },
        episode = exercise.episode.takeIf { it > 0 } ?: fallback.episode,
    )
}

internal fun normalizeReadAirWorkSlug(workSlug: String): String {
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

internal data class ProgressSyncResult(
    val item: ProgressItem,
    val recoveredDuplicate: Boolean = false,
)

internal fun Throwable.isProgressDuplicateConflict(): Boolean {
    val raw = message.orEmpty()
    return raw.contains("23505") ||
        raw.contains("duplicate key", ignoreCase = true) ||
        raw.contains("Key (device_id, item_id)", ignoreCase = true) ||
        (raw.contains("HTTP 500") && raw.contains("409"))
}

internal fun SyncAnswer.toProgressItem(): ProgressItem {
    return ProgressItem(
        itemId = itemId,
        itemType = itemType,
        workSlug = selection?.workSlug.orEmpty(),
        episode = selection?.episode ?: 0,
        state = state,
        label = label,
        lastReviewedAt = Instant.now().toString(),
        payload = payload?.toFlatStringMap().orEmpty(),
    )
}

internal fun ProgressItem.toSyncAnswer(): SyncAnswer {
    val json = JSONObject()
    payload.forEach(json::put)
    return SyncAnswer(
        itemId = itemId,
        itemType = itemType,
        selection = if (payload["track"] == "foundation") {
            null
        } else {
            EpisodeSelection(workSlug = workSlug, episode = episode)
        },
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

internal fun mergeProgressVersions(
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

internal suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

internal fun Throwable.toFoundationTrainingError(): FoundationTrainingError {
    return if (this is IllegalArgumentException) {
        FoundationTrainingError.MalformedData
    } else {
        FoundationTrainingError.Network
    }
}

internal fun pathNodeProgressId(selection: EpisodeSelection, pathNodeKey: String): String {
    return "path-node:${selection.workSlug}:${selection.episode}:$pathNodeKey"
}

internal fun ProgressItem.sameProgressIdentity(other: ProgressItem): Boolean {
    return itemId == other.itemId &&
        itemType == other.itemType &&
        workSlug == other.workSlug &&
        episode == other.episode
}

internal fun JSONObject.toFlatStringMap(): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val names = keys()
    while (names.hasNext()) {
        val key = names.next()
        result[key] = optString(key)
    }
    return result
}

internal fun buildReadAirAiContext(exercise: LinguisticExercise, selected: String): String {
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
