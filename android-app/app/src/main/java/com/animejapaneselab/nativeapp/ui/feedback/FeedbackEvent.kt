package com.animejapaneselab.nativeapp.ui.feedback

sealed interface FeedbackEvent {
    data object TapPrimary : FeedbackEvent
    data object TapSecondary : FeedbackEvent
    data object OptionSelect : FeedbackEvent
    data class AnswerCorrect(val xp: Int) : FeedbackEvent
    data object AnswerWrong : FeedbackEvent
    data class Combo(val count: Int) : FeedbackEvent
    data object LessonStepComplete : FeedbackEvent
    data object LessonNodeUnlock : FeedbackEvent
    data class XpGain(val amount: Int) : FeedbackEvent
    data class StreakExtend(val days: Int) : FeedbackEvent
    data object LessonComplete : FeedbackEvent
    data class ReviewScheduled(val count: Int) : FeedbackEvent
}
