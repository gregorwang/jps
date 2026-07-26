package com.animejapaneselab.nativeapp.domain

import com.animejapaneselab.nativeapp.data.ClozeChoice
import com.animejapaneselab.nativeapp.data.ClozeNode
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReviewState
import com.animejapaneselab.nativeapp.data.ShadowingNode
import com.animejapaneselab.nativeapp.data.SingleChoiceNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonEngineTest {
    private val nodes = listOf(
        SingleChoiceNode(
            id = "choice",
            title = "选择",
            prompt = "逃脱、摆脱",
            explanation = "逃れる 偏向从危险中脱身。",
            sourceLabel = "词汇",
            body = null,
            choices = listOf("挑む", "逃れる", "譲る"),
            answer = "逃れる",
        ),
        ClozeNode(
            id = "cloze",
            title = "填空",
            prompt = "选出自然语气",
            explanation = "んじゃない 带阻止和纠正。",
            sourceLabel = "语法",
            before = "逃がす",
            after = "！",
            choices = listOf(ClozeChoice("んじゃない", "阻止"), ClozeChoice("かもしれない", "可能")),
            answer = "んじゃない",
        ),
    )

    @Test
    fun resumeUsesPersistedNodeIdsAndStartsAtFirstUnfinishedItem() {
        val sixNodes = (1..6).map { number ->
            SingleChoiceNode(
                id = "shadowing-$number",
                title = "跟读",
                prompt = "句子 $number",
                explanation = "",
                sourceLabel = "",
                body = null,
                choices = listOf("完成", "重练"),
                answer = "完成",
                sourceKind = "sentence",
                sourceId = "sentence-$number",
            )
        }
        val progress = sixNodes.take(4).map { node ->
            ProgressItem(
                itemId = node.sourceId,
                itemType = node.sourceKind,
                workSlug = "re-zero",
                episode = 1,
                state = ReviewState.Good,
                label = node.prompt,
                payload = mapOf("nodeId" to node.id),
            )
        }

        val resumed = resumeLessonFromProgress(sixNodes, progress)

        assertEquals(4, resumed.index)
        assertEquals(4, resumed.answered)
        assertEquals(4, resumed.correct)
        assertEquals("shadowing-5", resumed.currentNode?.id)
        assertEquals(6, resumed.nodes.size)
    }

    @Test
    fun resumeKeepsIncorrectItemsInTheRemainingQueue() {
        val node = nodes.first()
        val progress = ProgressItem(
            itemId = node.id,
            itemType = node.sourceKind,
            workSlug = "k-on",
            episode = 1,
            state = ReviewState.Bad,
            label = node.prompt,
            payload = mapOf("nodeId" to node.id),
        )

        val resumed = resumeLessonFromProgress(nodes, listOf(progress))

        assertEquals(0, resumed.index)
        assertEquals(node.id, resumed.currentNode?.id)
    }

    @Test
    fun modernNodeProgressDoesNotSkipSiblingMechanicsForTheSameMaterial() {
        val study = (nodes.first() as SingleChoiceNode).copy(
            id = "vocab-1-study",
            sourceKind = "vocab",
            sourceId = "vocab-1",
        )
        val choice = study.copy(id = "vocab-1-ja-to-meaning")
        val progress = ProgressItem(
            itemId = study.id,
            itemType = "vocab",
            workSlug = "re-zero",
            episode = 1,
            state = ReviewState.Good,
            label = study.prompt,
            payload = mapOf("nodeId" to study.id, "sourceId" to study.sourceId),
        )

        val resumed = resumeLessonFromProgress(listOf(study, choice), listOf(progress))

        assertEquals(1, resumed.index)
        assertEquals(choice.id, resumed.currentNode?.id)
    }

    @Test
    fun correctAnswerRecordsFeedbackAndScore() {
        val answered = LessonEngine.answer(LessonEngine.start(nodes), "逃れる")

        assertEquals(1, answered.correct)
        assertEquals(1, answered.answered)
        assertEquals(1, answered.currentStreak)
        assertEquals(1, answered.bestStreak)
        assertNotNull(answered.feedback)
        assertTrue(answered.feedback!!.correct)
        assertEquals("逃れる", answered.feedback!!.expected)
    }

    @Test
    fun wrongAnswerKeepsExpectedAnswerForReview() {
        val answered = LessonEngine.answer(LessonEngine.start(nodes), "挑む")

        assertEquals(0, answered.correct)
        assertEquals(1, answered.answered)
        assertEquals(0, answered.currentStreak)
        assertEquals(0, answered.bestStreak)
        assertFalse(answered.feedback!!.correct)
        assertEquals("逃れる", answered.feedback!!.expected)
    }

    @Test
    fun continueAdvancesAndClearsFeedback() {
        val answered = LessonEngine.answer(LessonEngine.start(nodes), "逃れる")
        val next = LessonEngine.continueAfterFeedback(answered)

        assertEquals(1, next.index)
        assertNull(next.feedback)
        assertEquals("cloze", next.currentNode!!.id)
    }

    @Test
    fun restartKeepsNodesAndResetsProgress() {
        val answered = LessonEngine.answer(LessonEngine.start(nodes), "逃れる")
        val restarted = LessonEngine.restart(answered)

        assertEquals(nodes.size, restarted.nodes.size)
        assertEquals(0, restarted.index)
        assertEquals(0, restarted.correct)
        assertEquals(0, restarted.answered)
        assertEquals(0, restarted.currentStreak)
        assertEquals(0, restarted.bestStreak)
        assertFalse(restarted.isComplete)
    }

    @Test
    fun streakUsesConsecutiveCorrectAnswersAndKeepsSessionBest() {
        val threeNodes = nodes + (nodes.first() as SingleChoiceNode).copy(id = "choice-2")
        val firstCorrect = LessonEngine.answer(LessonEngine.start(threeNodes), "逃れる")
        val secondCorrect = LessonEngine.answer(
            LessonEngine.continueAfterFeedback(firstCorrect),
            "んじゃない",
        )
        val finalWrong = LessonEngine.answer(
            LessonEngine.continueAfterFeedback(secondCorrect),
            "挑む",
        )

        assertEquals(2, secondCorrect.currentStreak)
        assertEquals(2, secondCorrect.bestStreak)
        assertEquals(0, finalWrong.currentStreak)
        assertEquals(2, finalWrong.bestStreak)
    }

    @Test
    fun shadowingRetryRatingRemainsIncorrect() {
        val node = ShadowingNode(
            id = "shadowing",
            title = "跟读",
            prompt = "もう一度",
            explanation = "",
            sourceLabel = "EP01",
            sentence = com.animejapaneselab.nativeapp.data.ShadowingSentence(
                id = "sentence-1",
                ja = "もう一度",
                reading = "mou ichido",
                meaningZh = "再来一次",
                sourceLabel = "EP01",
                audioKind = com.animejapaneselab.nativeapp.data.AudioKind.Tts,
            ),
            ratings = listOf("像原声", "大致跟上", "还要再练"),
        )

        val retry = LessonEngine.answer(LessonEngine.start(listOf(node)), "还要再练")
        val fair = LessonEngine.answer(LessonEngine.start(listOf(node)), "大致跟上")

        assertFalse(retry.feedback!!.correct)
        assertTrue(fair.feedback!!.correct)
    }
}
