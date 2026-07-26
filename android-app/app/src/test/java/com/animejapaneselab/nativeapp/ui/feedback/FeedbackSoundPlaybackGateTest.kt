package com.animejapaneselab.nativeapp.ui.feedback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackSoundPlaybackGateTest {
    @Test
    fun repeatedCorrectJudgmentOnlyPlaysOnceUntilNextInteraction() {
        val gate = FeedbackSoundPlaybackGate()

        assertTrue(gate.shouldPlay(FeedbackEvent.AnswerCorrect(xp = 12), soundEnabled = true))
        assertFalse(gate.shouldPlay(FeedbackEvent.AnswerCorrect(xp = 12), soundEnabled = true))
        assertFalse(gate.shouldPlay(FeedbackEvent.AnswerCorrect(xp = 1), soundEnabled = true))

        assertTrue(gate.shouldPlay(FeedbackEvent.TapPrimary, soundEnabled = true))
        assertTrue(gate.shouldPlay(FeedbackEvent.AnswerCorrect(xp = 12), soundEnabled = true))
    }

    @Test
    fun repeatedWrongJudgmentOnlyPlaysOnceUntilNextSelection() {
        val gate = FeedbackSoundPlaybackGate()

        assertTrue(gate.shouldPlay(FeedbackEvent.AnswerWrong, soundEnabled = true))
        assertFalse(gate.shouldPlay(FeedbackEvent.AnswerWrong, soundEnabled = true))

        assertTrue(gate.shouldPlay(FeedbackEvent.OptionSelect, soundEnabled = true))
        assertTrue(gate.shouldPlay(FeedbackEvent.AnswerWrong, soundEnabled = true))
    }

    @Test
    fun consequenceEventsDoNotRearmVisibleJudgment() {
        val gate = FeedbackSoundPlaybackGate()

        assertTrue(gate.shouldPlay(FeedbackEvent.AnswerCorrect(xp = 12), soundEnabled = true))
        assertTrue(gate.shouldPlay(FeedbackEvent.XpGain(amount = 12), soundEnabled = true))
        assertTrue(gate.shouldPlay(FeedbackEvent.Combo(count = 5), soundEnabled = true))
        assertFalse(gate.shouldPlay(FeedbackEvent.AnswerCorrect(xp = 12), soundEnabled = true))
    }

    @Test
    fun mutedJudgmentIsConsumedWithoutPlayingAfterRecreation() {
        val gate = FeedbackSoundPlaybackGate()

        assertFalse(gate.shouldPlay(FeedbackEvent.AnswerCorrect(xp = 12), soundEnabled = false))
        assertFalse(gate.shouldPlay(FeedbackEvent.AnswerCorrect(xp = 12), soundEnabled = true))

        assertFalse(gate.shouldPlay(FeedbackEvent.OptionSelect, soundEnabled = false))
        assertTrue(gate.shouldPlay(FeedbackEvent.AnswerCorrect(xp = 12), soundEnabled = true))
    }

    @Test
    fun changingJudgmentPlaysEachFirstTransition() {
        val gate = FeedbackSoundPlaybackGate()

        assertTrue(gate.shouldPlay(FeedbackEvent.AnswerCorrect(xp = 12), soundEnabled = true))
        assertTrue(gate.shouldPlay(FeedbackEvent.AnswerWrong, soundEnabled = true))
        assertFalse(gate.shouldPlay(FeedbackEvent.AnswerWrong, soundEnabled = true))
    }
}
