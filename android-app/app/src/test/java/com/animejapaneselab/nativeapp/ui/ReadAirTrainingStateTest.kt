package com.animejapaneselab.nativeapp.ui

import com.animejapaneselab.nativeapp.data.LinguisticExercise
import com.animejapaneselab.nativeapp.data.LinguisticExerciseAnswer
import com.animejapaneselab.nativeapp.data.AiCoachState
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReviewState
import com.animejapaneselab.nativeapp.data.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadAirTrainingStateTest {
    @Test
    fun answeredExerciseStaysInQueueOnlyWhilePinned() {
        val first = exercise("exercise-1", "答え1")
        val second = exercise("exercise-2", "答え2")
        val pinned = ReadAirTrainingState(
            exercises = listOf(first, second),
            selectedAnswers = mapOf(first.id to "答え1"),
            pinnedExerciseId = first.id,
        )

        assertEquals(listOf("exercise-1", "exercise-2"), pinned.filteredExercises.map { it.id })

        val unpinned = pinned.copy(pinnedExerciseId = null)

        assertEquals(listOf("exercise-2"), unpinned.filteredExercises.map { it.id })
    }

    @Test
    fun browseModeKeepsAnsweredExercisesInScopedLibrary() {
        val first = exercise("exercise-1", "答え1")
        val second = exercise("exercise-2", "答え2")
        val state = ReadAirTrainingState(
            exercises = listOf(first, second),
            mode = ReadAirMode.Browse,
            selectedAnswers = mapOf(first.id to "答え1"),
        )

        assertEquals(listOf("exercise-1", "exercise-2"), state.scopedExercises.map { it.id })
        assertEquals(listOf("exercise-2"), state.filteredExercises.map { it.id })
    }

    @Test
    fun browseAnswersRevealExplanationsWithoutConsumingTrainingQueue() {
        val first = exercise("exercise-1", "答え1")
        val second = exercise("exercise-2", "答え2")
        val state = ReadAirTrainingState(
            exercises = listOf(first, second),
            mode = ReadAirMode.Browse,
            browseAnswers = mapOf(first.id to "答え1"),
        )

        assertEquals("答え1", state.browseAnswerFor(first.id))
        assertEquals(0, state.answeredScopedCount)
        assertEquals(2, state.remainingScopedCount)
        assertEquals(listOf(first.id, second.id), state.filteredExercises.map { it.id })
    }

    @Test
    fun scopedCountsSeparateBrowseLibraryFromRemainingTrainingQueue() {
        val first = exercise("exercise-1", "答え1", episode = 1)
        val second = exercise("exercise-2", "答え2", episode = 1)
        val outside = exercise("exercise-3", "答え3", episode = 2)
        val state = ReadAirTrainingState(
            exercises = listOf(first, second, outside),
            filters = ReadAirFilters(episode = 1),
            selectedAnswers = mapOf(first.id to "答え1", outside.id to "答え3"),
        )

        assertEquals(2, state.scopedExercises.size)
        assertEquals(1, state.answeredScopedCount)
        assertEquals(1, state.remainingScopedCount)
        assertEquals(listOf(second.id), state.filteredExercises.map { it.id })
    }

    @Test
    fun workFilterNormalizesRezeroSlug() {
        val state = ReadAirTrainingState(
            exercises = listOf(
                exercise(id = "legacy-rezero", answer = "答え1", workSlug = "rezero"),
                exercise(id = "k-on", answer = "答え2", workSlug = "k-on"),
            ),
            filters = ReadAirFilters(workSlug = "re-zero"),
        )

        assertEquals(listOf("legacy-rezero"), state.scopedExercises.map { it.id })
    }

    @Test
    fun combinedFiltersNarrowQueueAcrossWorkEpisodePhenomenonQuestionTypeAndDifficulty() {
        val target = exercise(
            id = "target",
            answer = "答え1",
            workSlug = "re-zero",
            episode = 2,
            domain = "sociolinguistics",
            phenomenonKey = "topic_shift",
            questionType = "implicit_intent",
            difficulty = "intermediate",
        )
        val state = ReadAirTrainingState(
            exercises = listOf(
                target,
                target.copy(id = "wrong-work", workSlug = "k-on"),
                target.copy(id = "wrong-episode", episode = 1),
                target.copy(id = "wrong-domain", domain = "pragmatics"),
                target.copy(id = "wrong-phenomenon", phenomenonKey = "ellipsis"),
                target.copy(id = "wrong-type", questionType = "relationship_reading"),
                target.copy(id = "wrong-difficulty", difficulty = "advanced"),
            ),
            filters = ReadAirFilters(
                workSlug = "rezero",
                domain = "sociolinguistics",
                phenomenonKey = "topic_shift",
                questionType = "implicit_intent",
                difficulty = "intermediate",
                episode = 2,
            ),
            currentIndex = 99,
        )

        assertEquals(listOf("target"), state.scopedExercises.map { it.id })
        assertEquals("target", state.currentExercise?.id)
    }

    @Test
    fun clearingFiltersRestoresFullLibraryScope() {
        val exercises = listOf(
            exercise(id = "k-on-1", answer = "答え1", workSlug = "k-on", episode = 1),
            exercise(id = "k-on-2", answer = "答え2", workSlug = "k-on", episode = 2, difficulty = "intermediate"),
            exercise(id = "re-zero-1", answer = "答え3", workSlug = "re-zero", episode = 1, domain = "sociolinguistics"),
        )
        val filtered = ReadAirTrainingState(
            exercises = exercises,
            filters = ReadAirFilters(workSlug = "k-on", episode = 1),
        )

        assertEquals(listOf("k-on-1"), filtered.scopedExercises.map { it.id })

        val cleared = filtered.copy(filters = ReadAirFilters())

        assertEquals(listOf("k-on-1", "k-on-2", "re-zero-1"), cleared.scopedExercises.map { it.id })
    }

    @Test
    fun resettingQueueKeepsFiltersButRestoresAnsweredScopedExercises() {
        val first = exercise("exercise-1", "答え1", episode = 2)
        val second = exercise("exercise-2", "答え2", episode = 2)
        val outsideFilter = exercise("exercise-3", "答え3", episode = 1)
        val answered = ReadAirTrainingState(
            exercises = listOf(first, second, outsideFilter),
            filters = ReadAirFilters(episode = 2),
            selectedAnswers = mapOf(first.id to "答え1", second.id to "答え2"),
        )

        assertEquals(emptyList<String>(), answered.filteredExercises.map { it.id })

        val reset = answered.copy(selectedAnswers = emptyMap(), currentIndex = 0, pinnedExerciseId = null)

        assertEquals(listOf("exercise-1", "exercise-2"), reset.filteredExercises.map { it.id })
    }

    @Test
    fun filterOptionsExposeDistinctWorksEpisodesPhenomenaTypesAndDifficulties() {
        val state = ReadAirTrainingState(
            exercises = listOf(
                exercise(
                    id = "k-on-basic",
                    answer = "答え1",
                    workSlug = "k-on",
                    episode = 1,
                    domain = "pragmatics",
                    phenomenonKey = "ellipsis",
                    questionType = "kuuki_yomi",
                    difficulty = "intro",
                ),
                exercise(
                    id = "re-zero-advanced",
                    answer = "答え2",
                    workSlug = "re-zero",
                    episode = 2,
                    domain = "sociolinguistics",
                    phenomenonKey = "topic_shift",
                    questionType = "implicit_intent",
                    difficulty = "advanced",
                ),
            ),
        )

        assertEquals(listOf(ReadAirAllFilter, "k-on", "re-zero"), state.workOptions)
        assertEquals(listOf(ReadAirAllFilter, "pragmatics", "sociolinguistics"), state.domainOptions)
        assertEquals(listOf(ReadAirAllFilter, "ellipsis", "topic_shift"), state.phenomenonOptions)
        assertEquals(listOf(ReadAirAllFilter, "implicit_intent", "kuuki_yomi"), state.questionTypeOptions)
        assertEquals(listOf(ReadAirAllFilter, "advanced", "intro"), state.difficultyOptions)
        assertEquals(listOf(1, 2), state.episodeOptions)
    }

    @Test
    fun filterOptionsCascadeFromOtherSelectedFilters() {
        val state = ReadAirTrainingState(
            exercises = listOf(
                exercise(
                    id = "k-on-ep1",
                    answer = "答え1",
                    workSlug = "k-on",
                    episode = 1,
                    domain = "pragmatics",
                    phenomenonKey = "ellipsis",
                    questionType = "kuuki_yomi",
                    difficulty = "intro",
                ),
                exercise(
                    id = "k-on-ep2",
                    answer = "答え2",
                    workSlug = "k-on",
                    episode = 2,
                    domain = "pragmatics",
                    phenomenonKey = "soft_obligation",
                    questionType = "relationship_reading",
                    difficulty = "intermediate",
                ),
                exercise(
                    id = "re-zero-ep2",
                    answer = "答え3",
                    workSlug = "re-zero",
                    episode = 2,
                    domain = "sociolinguistics",
                    phenomenonKey = "topic_shift",
                    questionType = "implicit_intent",
                    difficulty = "advanced",
                ),
            ),
            filters = ReadAirFilters(workSlug = "k-on", domain = "pragmatics", episode = 2),
        )

        assertEquals(listOf(ReadAirAllFilter, "k-on", "re-zero"), state.workOptions)
        assertEquals(listOf(ReadAirAllFilter, "pragmatics"), state.domainOptions)
        assertEquals(listOf(ReadAirAllFilter, "soft_obligation"), state.phenomenonOptions)
        assertEquals(listOf(ReadAirAllFilter, "relationship_reading"), state.questionTypeOptions)
        assertEquals(listOf(ReadAirAllFilter, "intermediate"), state.difficultyOptions)
        assertEquals(listOf(1, 2), state.episodeOptions)
    }

    @Test
    fun workOptionsRemainAvailableWhenEpisodeFilterWouldOtherwiseHideAWork() {
        val state = ReadAirTrainingState(
            exercises = listOf(
                exercise(id = "k-on-ep1", answer = "答え1", workSlug = "k-on", episode = 1),
                exercise(id = "re-zero-ep56", answer = "答え2", workSlug = "re-zero", episode = 56),
            ),
            filters = ReadAirFilters(workSlug = "re-zero", episode = 56),
        )

        assertEquals(listOf(ReadAirAllFilter, "k-on", "re-zero"), state.workOptions)
        assertEquals(listOf("re-zero-ep56"), state.scopedExercises.map { it.id })
    }

    @Test
    fun cascadingOptionsKeepCurrentInvalidSelectionVisibleSoItCanBeCleared() {
        val state = ReadAirTrainingState(
            exercises = listOf(
                exercise(
                    id = "k-on-ep1",
                    answer = "答え1",
                    workSlug = "k-on",
                    episode = 1,
                    domain = "pragmatics",
                    phenomenonKey = "ellipsis",
                ),
            ),
            filters = ReadAirFilters(workSlug = "k-on", phenomenonKey = "topic_shift"),
        )

        assertEquals(listOf(ReadAirAllFilter, "ellipsis", "topic_shift"), state.phenomenonOptions)
        assertEquals(emptyList<LinguisticExercise>(), state.scopedExercises)
    }

    @Test
    fun resetScopedQueueKeepsAnswersOutsideCurrentFilters() {
        val scoped = exercise("scoped", "答え1", workSlug = "k-on", episode = 2)
        val outside = exercise("outside", "答え2", workSlug = "re-zero", episode = 2)
        val state = ReadAirTrainingState(
            exercises = listOf(scoped, outside),
            filters = ReadAirFilters(workSlug = "k-on", episode = 2),
            selectedAnswers = mapOf(
                scoped.id to "答え1",
                outside.id to "答え2",
            ),
            currentIndex = 4,
            pinnedExerciseId = scoped.id,
        )

        val reset = state.resetScopedQueue()

        assertEquals(mapOf(outside.id to "答え2"), reset.selectedAnswers)
        assertEquals(0, reset.currentIndex)
        assertEquals(null, reset.pinnedExerciseId)
        assertEquals(listOf(scoped.id), reset.filteredExercises.map { it.id })
    }

    @Test
    fun advanceAfterCurrentAnswerDoesNotSkipUnansweredQuestion() {
        val first = exercise("first", "答え1")
        val second = exercise("second", "答え2")
        val state = ReadAirTrainingState(
            exercises = listOf(first, second),
            currentIndex = 0,
        )

        val next = state.advanceAfterCurrentAnswer()

        assertEquals("first", next.currentExercise?.id)
        assertEquals(0, next.currentIndex)
        assertEquals(null, next.pinnedExerciseId)
    }

    @Test
    fun advanceAfterCurrentAnswerClearsFeedbackAndMovesToNextQueuedQuestion() {
        val first = exercise("first", "答え1")
        val second = exercise("second", "答え2")
        val state = ReadAirTrainingState(
            exercises = listOf(first, second),
            currentIndex = 0,
            selectedAnswers = mapOf(first.id to "答え1"),
            pinnedExerciseId = first.id,
            aiCoach = AiCoachState(
                status = SyncStatus.Success,
                answer = "AI explanation",
            ),
        )

        val next = state.advanceAfterCurrentAnswer()

        assertEquals("second", next.currentExercise?.id)
        assertEquals(0, next.currentIndex)
        assertEquals(null, next.pinnedExerciseId)
        assertEquals(SyncStatus.Idle, next.aiCoach.status)
        assertEquals("", next.aiCoach.answer)
        assertEquals(null, next.aiCoach.result)
    }

    @Test
    fun reviewReadAirMatcherUsesSourceIdWhenRemoteProgressIdDiffers() {
        val target = exercise(
            id = "local-read-air-1",
            answer = "答え1",
            sourceId = "cue-25",
        )
        val task = ProgressItem(
            itemId = "remote-progress-1",
            itemType = "exercise",
            workSlug = "k-on",
            episode = 1,
            state = ReviewState.Bad,
            label = "读空气 第 25 行",
            payload = mapOf("sourceId" to "cue-25"),
        )

        assertEquals(target.id, findReviewReadAirExercise(task, listOf(target))?.id)
    }

    @Test
    fun reviewReadAirMatcherUsesLocalizedLineLabelInsideEpisode() {
        val wrongEpisode = exercise(
            id = "wrong-episode",
            answer = "答え1",
            episode = 2,
            sourceLineNo = 25,
        )
        val target = exercise(
            id = "k-on-ep1-line25",
            answer = "答え2",
            episode = 1,
            sourceLineNo = 25,
        )
        val task = ProgressItem(
            itemId = "line-25-review",
            itemType = "exercise",
            workSlug = "k-on",
            episode = 1,
            state = ReviewState.Bad,
            label = "读空气 第 25 行",
        )

        assertEquals(target.id, findReviewReadAirExercise(task, listOf(wrongEpisode, target))?.id)
    }
}

private fun exercise(
    id: String,
    answer: String,
    workSlug: String = "k-on",
    episode: Int = 1,
    domain: String = "pragmatics",
    phenomenonKey: String = "soft_obligation_ellipsis",
    questionType: String = "kuuki_yomi",
    difficulty: String = "intro",
    sourceId: String = "",
    sourceLineNo: Int = 0,
    targetLineNo: Int = 0,
    jaText: String = "そろそろ起きないと。",
): LinguisticExercise {
    return LinguisticExercise(
        id = id,
        workSlug = workSlug,
        episode = episode,
        sourceId = sourceId,
        sourceLineNo = sourceLineNo,
        targetLineNo = targetLineNo,
        jaText = jaText,
        domain = domain,
        phenomenonKey = phenomenonKey,
        questionType = questionType,
        prompt = "この空気は？",
        options = listOf(answer, "違う答え"),
        answer = LinguisticExerciseAnswer(answerZh = answer, correctIndex = 0),
        difficulty = difficulty,
    )
}
