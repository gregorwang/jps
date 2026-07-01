package com.animejapaneselab.nativeapp.domain

import com.animejapaneselab.nativeapp.data.ClozeChoice
import com.animejapaneselab.nativeapp.data.ClozeNode
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
    fun correctAnswerRecordsFeedbackAndScore() {
        val answered = LessonEngine.answer(LessonEngine.start(nodes), "逃れる")

        assertEquals(1, answered.correct)
        assertEquals(1, answered.answered)
        assertNotNull(answered.feedback)
        assertTrue(answered.feedback!!.correct)
        assertEquals("逃れる", answered.feedback!!.expected)
    }

    @Test
    fun wrongAnswerKeepsExpectedAnswerForReview() {
        val answered = LessonEngine.answer(LessonEngine.start(nodes), "挑む")

        assertEquals(0, answered.correct)
        assertEquals(1, answered.answered)
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
        assertFalse(restarted.isComplete)
    }
}
