package com.animejapaneselab.nativeapp.ui.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackEventTest {
    @Test
    fun answerCorrectCarriesXpPayload() {
        val event: FeedbackEvent = FeedbackEvent.AnswerCorrect(xp = 12)

        assertTrue(event is FeedbackEvent.AnswerCorrect)
        assertEquals(12, (event as FeedbackEvent.AnswerCorrect).xp)
    }

    @Test
    fun reviewScheduledCarriesCountPayload() {
        val event: FeedbackEvent = FeedbackEvent.ReviewScheduled(count = 3)

        assertTrue(event is FeedbackEvent.ReviewScheduled)
        assertEquals(3, (event as FeedbackEvent.ReviewScheduled).count)
    }
}
