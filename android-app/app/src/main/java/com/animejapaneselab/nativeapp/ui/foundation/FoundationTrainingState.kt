package com.animejapaneselab.nativeapp.ui.foundation

import com.animejapaneselab.nativeapp.data.FoundationDomain
import com.animejapaneselab.nativeapp.data.FoundationQuestion
import com.animejapaneselab.nativeapp.data.FoundationQuestionPack
import com.animejapaneselab.nativeapp.data.FoundationStage
import com.animejapaneselab.nativeapp.data.FoundationTopic
import com.animejapaneselab.nativeapp.data.ProgressItem

enum class LinguisticsTrack {
    AnimeCorpus,
    Foundation,
    Conjugation,
}

enum class FoundationTrainingPhase {
    Idle,
    LoadingCatalog,
    LoadingQuestions,
    Ready,
    Error,
}

enum class FoundationTrainingError {
    Network,
    MalformedData,
    EmptyCatalog,
}

data class FoundationFilters(
    val packId: String? = null,
    val domain: FoundationDomain? = null,
    val topicId: String? = null,
    val stage: FoundationStage? = null,
)

data class FoundationTrainingState(
    val phase: FoundationTrainingPhase = FoundationTrainingPhase.Idle,
    val topics: List<FoundationTopic> = emptyList(),
    val packs: List<FoundationQuestionPack> = emptyList(),
    val questions: List<FoundationQuestion> = emptyList(),
    val filters: FoundationFilters = FoundationFilters(),
    val currentIndex: Int = 0,
    val selectedAnswers: Map<String, String> = emptyMap(),
    val error: FoundationTrainingError? = null,
) {
    private val topicById: Map<String, FoundationTopic> = topics.associateBy(FoundationTopic::id)

    val packOptions: List<FoundationQuestionPack> =
        packs.sortedWith(compareBy(FoundationQuestionPack::batchNo, FoundationQuestionPack::id))

    val domainOptions: List<FoundationDomain> = run {
        val questionTopicIds = questions.asSequence()
            .filter { filters.packId == null || it.packId == filters.packId }
            .map(FoundationQuestion::topicId)
            .toSet()
        topics.asSequence()
            .filter { questionTopicIds.isEmpty() || it.id in questionTopicIds }
            .map(FoundationTopic::domain)
            .distinct()
            .sortedBy(FoundationDomain::ordinal)
            .toList()
    }

    val topicOptions: List<FoundationTopic> = run {
        val questionTopicIds = questions.asSequence()
            .filter { filters.packId == null || it.packId == filters.packId }
            .filter { question ->
                filters.domain == null || topicById[question.topicId]?.domain == filters.domain
            }
            .map(FoundationQuestion::topicId)
            .toSet()
        topics.asSequence()
            .filter { filters.domain == null || it.domain == filters.domain }
            .filter { questionTopicIds.isEmpty() || it.id in questionTopicIds }
            .sortedWith(compareBy(FoundationTopic::sortOrder, FoundationTopic::id))
            .toList()
    }

    val stageOptions: List<FoundationStage> = FoundationStage.entries.filter { stage ->
        questions.any { question ->
            question.stage == stage &&
                (filters.packId == null || question.packId == filters.packId) &&
                (filters.domain == null || topicById[question.topicId]?.domain == filters.domain) &&
                (filters.topicId == null || question.topicId == filters.topicId)
        }
    }

    val filteredQuestions: List<FoundationQuestion> = questions.asSequence()
        .filter { filters.packId == null || it.packId == filters.packId }
        .filter { filters.domain == null || topicById[it.topicId]?.domain == filters.domain }
        .filter { filters.topicId == null || it.topicId == filters.topicId }
        .filter { filters.stage == null || it.stage == filters.stage }
        .sortedWith(compareBy(FoundationQuestion::sortOrder, FoundationQuestion::id))
        .toList()

    val currentQuestion: FoundationQuestion?
        get() = filteredQuestions.getOrNull(currentIndex)

    val currentSelectedOptionId: String?
        get() = currentQuestion?.let { selectedAnswers[it.id] }

    val answeredCount: Int
        get() = filteredQuestions.count { selectedAnswers.containsKey(it.id) }

    val isComplete: Boolean
        get() = filteredQuestions.isNotEmpty() && currentIndex >= filteredQuestions.size
}

internal fun beginFoundationCatalogLoad(
    state: FoundationTrainingState,
): FoundationTrainingState {
    return state.copy(
        phase = FoundationTrainingPhase.LoadingCatalog,
        error = null,
    )
}

internal fun applyFoundationCatalog(
    state: FoundationTrainingState,
    topics: List<FoundationTopic>,
    packs: List<FoundationQuestionPack>,
): FoundationTrainingState {
    if (topics.isEmpty() || packs.isEmpty()) {
        return state.copy(
            phase = FoundationTrainingPhase.Error,
            topics = topics,
            packs = packs,
            questions = emptyList(),
            currentIndex = 0,
            error = FoundationTrainingError.EmptyCatalog,
        )
    }
    val selectedPackId = state.filters.packId
        ?.takeIf { candidate -> packs.any { it.id == candidate } }
        ?: packs.minWithOrNull(compareBy(FoundationQuestionPack::batchNo, FoundationQuestionPack::id))?.id
    return state.copy(
        phase = FoundationTrainingPhase.LoadingQuestions,
        topics = topics.distinctBy(FoundationTopic::id),
        packs = packs.distinctBy(FoundationQuestionPack::id),
        questions = emptyList(),
        filters = FoundationFilters(packId = selectedPackId),
        currentIndex = 0,
        error = null,
    )
}

internal fun applyFoundationQuestions(
    state: FoundationTrainingState,
    packId: String,
    questions: List<FoundationQuestion>,
): FoundationTrainingState {
    if (state.filters.packId != packId) return state
    val packQuestions = questions
        .filter { it.packId == packId }
        .distinctBy(FoundationQuestion::id)
    return state.copy(
        phase = FoundationTrainingPhase.Ready,
        questions = packQuestions,
        filters = state.filters.normalizeFor(state.topics, packQuestions),
        currentIndex = 0,
        error = null,
    )
}

internal fun failFoundationLoad(
    state: FoundationTrainingState,
    error: FoundationTrainingError,
): FoundationTrainingState {
    return state.copy(
        phase = FoundationTrainingPhase.Error,
        error = error,
    )
}

internal fun selectFoundationPack(
    state: FoundationTrainingState,
    packId: String,
): FoundationTrainingState {
    require(state.packs.any { it.id == packId }) { "Unknown foundation pack: $packId" }
    if (state.filters.packId == packId && state.questions.isNotEmpty()) return state
    return state.copy(
        phase = FoundationTrainingPhase.LoadingQuestions,
        questions = emptyList(),
        filters = FoundationFilters(packId = packId),
        currentIndex = 0,
        error = null,
    )
}

internal fun selectFoundationDomain(
    state: FoundationTrainingState,
    domain: FoundationDomain?,
): FoundationTrainingState {
    return state.copy(
        filters = state.filters.copy(
            domain = domain,
            topicId = null,
            stage = null,
        ),
        currentIndex = 0,
    )
}

internal fun selectFoundationTopic(
    state: FoundationTrainingState,
    topicId: String?,
): FoundationTrainingState {
    require(topicId == null || state.topicOptions.any { it.id == topicId }) {
        "Unknown foundation topic: $topicId"
    }
    return state.copy(
        filters = state.filters.copy(
            topicId = topicId,
            stage = null,
        ),
        currentIndex = 0,
    )
}

internal fun selectFoundationStage(
    state: FoundationTrainingState,
    stage: FoundationStage?,
): FoundationTrainingState {
    return state.copy(
        filters = state.filters.copy(stage = stage),
        currentIndex = 0,
    )
}

internal fun answerFoundationQuestion(
    state: FoundationTrainingState,
    optionId: String,
): FoundationTrainingState {
    val question = state.currentQuestion ?: return state
    if (state.selectedAnswers.containsKey(question.id)) return state
    require(question.options.any { it.id == optionId }) {
        "Unknown foundation answer option: $optionId"
    }
    return state.copy(
        selectedAnswers = state.selectedAnswers + (question.id to optionId),
    )
}

internal fun nextFoundationQuestion(
    state: FoundationTrainingState,
): FoundationTrainingState {
    val question = state.currentQuestion ?: return state
    if (!state.selectedAnswers.containsKey(question.id)) return state
    return state.copy(currentIndex = state.currentIndex + 1)
}

internal fun previousFoundationQuestion(
    state: FoundationTrainingState,
): FoundationTrainingState {
    return state.copy(currentIndex = (state.currentIndex - 1).coerceAtLeast(0))
}

internal fun restartFoundationQuestions(
    state: FoundationTrainingState,
): FoundationTrainingState {
    val visibleQuestionIds = state.filteredQuestions.mapTo(mutableSetOf(), FoundationQuestion::id)
    return state.copy(
        currentIndex = 0,
        selectedAnswers = state.selectedAnswers - visibleQuestionIds,
    )
}

internal fun restoreFoundationAnswers(
    questions: List<FoundationQuestion>,
    progressItems: List<ProgressItem>,
): Map<String, String> {
    val questionsById = questions.associateBy(FoundationQuestion::id)
    return buildMap {
        progressItems.forEach { progress ->
            if (containsKey(progress.itemId) || progress.payload["track"] != "foundation") {
                return@forEach
            }
            val question = questionsById[progress.itemId] ?: return@forEach
            val selected = progress.payload["selected"]
                ?.takeIf(String::isNotBlank)
                ?.takeIf { candidate -> question.options.any { it.id == candidate } }
                ?: return@forEach
            val persistedHash = progress.payload["contentHash"]
            val sameRevision = when {
                question.contentHash != null -> persistedHash == question.contentHash
                question.contentVersion > 1 ->
                    progress.payload["contentVersion"] == question.contentVersion.toString()
                else -> true
            }
            if (sameRevision) put(question.id, selected)
        }
    }
}

internal fun focusFoundationReviewQuestion(
    state: FoundationTrainingState,
    questionId: String,
    packId: String,
    topicId: String,
    stage: FoundationStage,
): FoundationTrainingState? {
    if (state.filters.packId != packId || state.questions.none { it.packId == packId }) return null
    val target = state.questions.firstOrNull { question ->
        question.id == questionId &&
            question.packId == packId &&
            question.topicId == topicId &&
            question.stage == stage
    } ?: return null
    val focused = state.copy(
        filters = FoundationFilters(
            packId = packId,
            domain = state.topics.firstOrNull { it.id == target.topicId }?.domain,
            topicId = target.topicId,
            stage = target.stage,
        ),
        currentIndex = 0,
    )
    val targetIndex = focused.filteredQuestions.indexOfFirst { it.id == questionId }
    return targetIndex.takeIf { it >= 0 }?.let { focused.copy(currentIndex = it) }
}

private fun FoundationFilters.normalizeFor(
    topics: List<FoundationTopic>,
    questions: List<FoundationQuestion>,
): FoundationFilters {
    val topicById = topics.associateBy(FoundationTopic::id)
    val normalizedDomain = domain?.takeIf { selectedDomain ->
        questions.any { topicById[it.topicId]?.domain == selectedDomain }
    }
    val normalizedTopicId = topicId?.takeIf { selectedTopicId ->
        questions.any { question ->
            question.topicId == selectedTopicId &&
                (normalizedDomain == null || topicById[question.topicId]?.domain == normalizedDomain)
        }
    }
    val normalizedStage = stage?.takeIf { selectedStage ->
        questions.any { question ->
            question.stage == selectedStage &&
                (normalizedDomain == null || topicById[question.topicId]?.domain == normalizedDomain) &&
                (normalizedTopicId == null || question.topicId == normalizedTopicId)
        }
    }
    return copy(
        domain = normalizedDomain,
        topicId = normalizedTopicId,
        stage = normalizedStage,
    )
}
