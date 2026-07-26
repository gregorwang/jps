package com.animejapaneselab.nativeapp.ui.feedback

/**
 * Keeps answer sounds aligned with a judgment state transition instead of a composition pass.
 *
 * The process-scoped instance survives Activity recreation, while explicit user interactions clear
 * the latched judgment before the next answer attempt. Consequence events such as XP and combo
 * feedback deliberately do not clear it, so rotating while a result is visible cannot replay the
 * same right/wrong sound.
 */
internal class FeedbackSoundPlaybackGate {
    private var activeJudgment: Judgment? = null

    @Synchronized
    fun shouldPlay(event: FeedbackEvent, soundEnabled: Boolean): Boolean {
        val firstTransition = when (event) {
            is FeedbackEvent.AnswerCorrect -> latch(Judgment.Correct)
            FeedbackEvent.AnswerWrong -> latch(Judgment.Wrong)
            FeedbackEvent.TapPrimary,
            FeedbackEvent.TapSecondary,
            FeedbackEvent.OptionSelect,
            FeedbackEvent.LessonStepComplete,
            FeedbackEvent.LessonNodeUnlock,
            is FeedbackEvent.ReviewScheduled -> {
                activeJudgment = null
                true
            }

            is FeedbackEvent.Combo,
            is FeedbackEvent.XpGain,
            is FeedbackEvent.StreakExtend,
            FeedbackEvent.LessonComplete -> true
        }
        return soundEnabled && firstTransition
    }

    private fun latch(judgment: Judgment): Boolean {
        if (activeJudgment == judgment) return false
        activeJudgment = judgment
        return true
    }

    private enum class Judgment {
        Correct,
        Wrong,
    }
}

internal val processFeedbackSoundPlaybackGate = FeedbackSoundPlaybackGate()
