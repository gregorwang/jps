package com.animejapaneselab.nativeapp.ui.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuolingoLikeAssetRegistryTest {
    @Test
    fun everyFeedbackEventHasAConfiguredVariant() {
        val events = listOf(
            FeedbackEvent.TapPrimary,
            FeedbackEvent.TapSecondary,
            FeedbackEvent.OptionSelect,
            FeedbackEvent.AnswerCorrect(xp = 12),
            FeedbackEvent.AnswerWrong,
            FeedbackEvent.Combo(count = 5),
            FeedbackEvent.LessonStepComplete,
            FeedbackEvent.LessonNodeUnlock,
            FeedbackEvent.XpGain(amount = 10),
            FeedbackEvent.StreakExtend(days = 3),
            FeedbackEvent.LessonComplete,
            FeedbackEvent.ReviewScheduled(count = 2),
        )

        events.forEach { event ->
            val variant = DuolingoLikeAssetRegistry.variantFor(event)
            assertTrue("animation duration for $event", variant.animationTimeMs > 0)
            assertNotNull("sound for $event", variant.sound)
            assertNotNull("haptic for $event", variant.haptic)
        }
    }

    @Test
    fun highValueEventsIncludeVisualAssets() {
        listOf(
            FeedbackEvent.AnswerCorrect(xp = 12),
            FeedbackEvent.LessonNodeUnlock,
            FeedbackEvent.LessonComplete,
        ).forEach { event ->
            assertNotNull("visual for $event", DuolingoLikeAssetRegistry.variantFor(event).visual)
        }
    }

    @Test
    fun answerEventsUseExactMirrorJudgmentSounds() {
        val correct = DuolingoLikeAssetRegistry.variantFor(FeedbackEvent.AnswerCorrect(xp = 12)).sound
        val wrong = DuolingoLikeAssetRegistry.variantFor(FeedbackEvent.AnswerWrong).sound

        assertEquals(SoundAsset(rawName = "right_answer", volume = 1f, rate = 1f), correct)
        assertEquals(SoundAsset(rawName = "wrong_answer", volume = 1f, rate = 1f), wrong)
    }
}
