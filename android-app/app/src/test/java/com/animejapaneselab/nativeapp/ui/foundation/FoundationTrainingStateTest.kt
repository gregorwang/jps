package com.animejapaneselab.nativeapp.ui.foundation

import com.animejapaneselab.nativeapp.data.FoundationDomain
import com.animejapaneselab.nativeapp.data.FoundationExampleSpec
import com.animejapaneselab.nativeapp.data.FoundationLearningObjectives
import com.animejapaneselab.nativeapp.data.FoundationPublicationStatus
import com.animejapaneselab.nativeapp.data.FoundationQuestion
import com.animejapaneselab.nativeapp.data.FoundationQuestionOption
import com.animejapaneselab.nativeapp.data.FoundationQuestionPack
import com.animejapaneselab.nativeapp.data.FoundationQuestionType
import com.animejapaneselab.nativeapp.data.FoundationSourceKind
import com.animejapaneselab.nativeapp.data.FoundationStage
import com.animejapaneselab.nativeapp.data.FoundationStimulus
import com.animejapaneselab.nativeapp.data.FoundationTopic
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FoundationTrainingStateTest {
    private val syntaxTopic = topic(
        id = "topic-syntax",
        domain = FoundationDomain.Syntax,
        sortOrder = 2,
    )
    private val semanticsTopic = topic(
        id = "topic-semantics",
        domain = FoundationDomain.Semantics,
        sortOrder = 1,
    )
    private val packOne = pack("pack-one", batchNo = 1)
    private val packTwo = pack("pack-two", batchNo = 2)

    @Test
    fun catalogSelectsFirstPackAndRequestsQuestions() {
        val state = applyFoundationCatalog(
            state = FoundationTrainingState(),
            topics = listOf(syntaxTopic, semanticsTopic),
            packs = listOf(packTwo, packOne),
        )

        assertEquals(FoundationTrainingPhase.LoadingQuestions, state.phase)
        assertEquals("pack-one", state.filters.packId)
        assertEquals(listOf("pack-one", "pack-two"), state.packOptions.map { it.id })
    }

    @Test
    fun filtersCascadeAcrossDomainTopicAndStage() {
        val ready = readyState()

        val syntaxOnly = selectFoundationDomain(ready, FoundationDomain.Syntax)
        assertEquals(listOf("syntax-f1"), syntaxOnly.filteredQuestions.map { it.id })
        assertEquals(listOf("topic-syntax"), syntaxOnly.topicOptions.map { it.id })
        assertEquals(listOf(FoundationStage.F1), syntaxOnly.stageOptions)

        val semanticsOnly = selectFoundationTopic(
            selectFoundationDomain(ready, FoundationDomain.Semantics),
            "topic-semantics",
        )
        assertEquals(listOf("semantics-f2"), semanticsOnly.filteredQuestions.map { it.id })
        assertEquals(FoundationStage.F2, semanticsOnly.stageOptions.single())

        val f2Only = selectFoundationStage(
            selectFoundationDomain(ready, null),
            FoundationStage.F2,
        )
        assertEquals(listOf("semantics-f2"), f2Only.filteredQuestions.map { it.id })
    }

    @Test
    fun answerFeedbackAdvancesCompletesAndRestartsVisibleQuestions() {
        val syntaxOnly = selectFoundationDomain(readyState(), FoundationDomain.Syntax)
        val answered = answerFoundationQuestion(syntaxOnly, "A")

        assertEquals("A", answered.currentSelectedOptionId)
        assertFalse(answered.isComplete)

        val completed = nextFoundationQuestion(answered)
        assertTrue(completed.isComplete)
        assertNull(completed.currentQuestion)

        val restarted = restartFoundationQuestions(completed)
        assertFalse(restarted.isComplete)
        assertEquals("syntax-f1", restarted.currentQuestion?.id)
        assertNull(restarted.currentSelectedOptionId)
    }

    @Test
    fun previousReturnsFromCompletionAndNeverMovesBeforeFirstQuestion() {
        val ready = readyState()
        assertEquals(0, previousFoundationQuestion(ready).currentIndex)

        val second = ready.copy(currentIndex = 1)
        assertEquals("syntax-f1", previousFoundationQuestion(second).currentQuestion?.id)

        val completed = ready.copy(currentIndex = ready.filteredQuestions.size)
        assertEquals("semantics-f2", previousFoundationQuestion(completed).currentQuestion?.id)
    }

    @Test
    fun restoredAnswersMustMatchTheCurrentQuestionRevision() {
        val current = question(
            "syntax-f1",
            "pack-one",
            syntaxTopic,
            FoundationStage.F1,
        ).copy(contentVersion = 2, contentHash = "a".repeat(64))
        val matching = progress(current.id, selected = "A", contentHash = "a".repeat(64))
        val stale = progress(current.id, selected = "B", contentHash = "b".repeat(64))

        assertEquals(
            mapOf(current.id to "A"),
            restoreFoundationAnswers(listOf(current), listOf(matching)),
        )
        assertTrue(restoreFoundationAnswers(listOf(current), listOf(stale)).isEmpty())
        assertTrue(
            restoreFoundationAnswers(
                listOf(current),
                listOf(progress(current.id, selected = "Z", contentHash = "a".repeat(64))),
            ).isEmpty(),
        )
    }

    @Test
    fun answeredQuestionCannotBeOverwritten() {
        val answered = answerFoundationQuestion(readyState(), "A")
        val secondAttempt = answerFoundationQuestion(answered, "B")

        assertSame(answered, secondAttempt)
        assertEquals("A", secondAttempt.currentSelectedOptionId)
    }

    @Test
    fun changingPackClearsPackSpecificFiltersAndIgnoresStaleResponse() {
        val selectedPackTwo = selectFoundationPack(
            readyState().copy(packs = listOf(packOne, packTwo)),
            "pack-two",
        )

        assertEquals(FoundationTrainingPhase.LoadingQuestions, selectedPackTwo.phase)
        assertEquals("pack-two", selectedPackTwo.filters.packId)
        assertTrue(selectedPackTwo.questions.isEmpty())

        val afterStalePackOneResponse = applyFoundationQuestions(
            state = selectedPackTwo,
            packId = "pack-one",
            questions = listOf(question("stale", "pack-one", syntaxTopic, FoundationStage.F1)),
        )
        assertSame(selectedPackTwo, afterStalePackOneResponse)
    }

    @Test
    fun reviewTargetRestoresTheExactTopicAndStage() {
        val focused = focusFoundationReviewQuestion(
            state = readyState(),
            questionId = "semantics-f2",
            packId = "pack-one",
            topicId = "topic-semantics",
            stage = FoundationStage.F2,
        )

        assertEquals("semantics-f2", focused?.currentQuestion?.id)
        assertEquals(FoundationDomain.Semantics, focused?.filters?.domain)
        assertEquals("topic-semantics", focused?.filters?.topicId)
        assertEquals(FoundationStage.F2, focused?.filters?.stage)
        assertNull(
            focusFoundationReviewQuestion(
                state = readyState(),
                questionId = "semantics-f2",
                packId = "pack-one",
                topicId = "topic-semantics",
                stage = FoundationStage.F3,
            ),
        )
    }

    private fun readyState(): FoundationTrainingState {
        val catalog = applyFoundationCatalog(
            state = FoundationTrainingState(),
            topics = listOf(syntaxTopic, semanticsTopic),
            packs = listOf(packOne),
        )
        return applyFoundationQuestions(
            state = catalog,
            packId = "pack-one",
            questions = listOf(
                question("syntax-f1", "pack-one", syntaxTopic, FoundationStage.F1),
                question("semantics-f2", "pack-one", semanticsTopic, FoundationStage.F2),
            ),
        )
    }

    private fun topic(
        id: String,
        domain: FoundationDomain,
        sortOrder: Int,
    ): FoundationTopic {
        return FoundationTopic(
            id = id,
            curriculumVersion = "foundation-v1",
            domain = domain,
            moduleId = "${domain.wireValue}-core",
            sortOrder = sortOrder,
            titleJa = "基礎",
            titleZh = id,
            shortDefinitionZh = "简短定义",
            beginnerExplanationZh = "入门解释",
            deepExplanationZh = "深入解释",
            cautionNoteZh = "注意事项",
            prerequisiteTopicIds = emptyList(),
            learningObjectives = FoundationLearningObjectives(
                f1Zh = "F1",
                f2Zh = "F2",
                f3Zh = "F3",
                f4Zh = "F4",
            ),
            exampleSpec = FoundationExampleSpec(
                formZh = "形式",
                contrastZh = "对比",
                constraintsZh = "约束",
            ),
            tags = listOf(domain.wireValue),
            status = FoundationPublicationStatus.Published,
            qualityScore = 96,
        )
    }

    private fun pack(id: String, batchNo: Int): FoundationQuestionPack {
        return FoundationQuestionPack(
            id = id,
            curriculumVersion = "foundation-v1",
            batchNo = batchNo,
            titleZh = id,
            descriptionZh = "基础题包",
            topicCount = 2,
            questionCount = 8,
            domainQuotas = mapOf(
                FoundationDomain.Syntax to 1,
                FoundationDomain.Semantics to 1,
            ),
            status = FoundationPublicationStatus.Published,
            qualityScore = 97,
        )
    }

    private fun question(
        id: String,
        packId: String,
        topic: FoundationTopic,
        stage: FoundationStage,
    ): FoundationQuestion {
        return FoundationQuestion(
            id = id,
            packId = packId,
            topicId = topic.id,
            curriculumVersion = "foundation-v1",
            stage = stage,
            questionType = FoundationQuestionType.SingleChoice,
            sourceKind = FoundationSourceKind.Metalinguistic,
            stimulus = FoundationStimulus.Metalinguistic("请选择正确分析。"),
            promptZh = "题目 $id",
            options = listOf(
                FoundationQuestionOption("A", "正确"),
                FoundationQuestionOption("B", "错误一"),
                FoundationQuestionOption("C", "错误二"),
                FoundationQuestionOption("D", "错误三"),
            ),
            answerOptionId = "A",
            hintZh = "提示",
            explanationZh = "解释",
            deepExplanationZh = "深入解释",
            cautionNoteZh = "注意",
            wrongExplanations = mapOf(
                "B" to "错因一",
                "C" to "错因二",
                "D" to "错因三",
            ),
            difficulty = stage.difficulty,
            tags = listOf(topic.domain.wireValue),
            sortOrder = stage.difficulty,
            status = FoundationPublicationStatus.Published,
            qualityScore = 98,
        )
    }

    private fun progress(
        itemId: String,
        selected: String,
        contentHash: String,
    ): ProgressItem {
        return ProgressItem(
            itemId = itemId,
            itemType = "exercise",
            workSlug = "",
            episode = 0,
            state = ReviewState.Good,
            label = itemId,
            payload = mapOf(
                "track" to "foundation",
                "selected" to selected,
                "contentVersion" to "2",
                "contentHash" to contentHash,
            ),
        )
    }
}
