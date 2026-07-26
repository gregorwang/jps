package com.animejapaneselab.nativeapp.ui.feedback

/**
 * Personal-learning Duolingo-like feedback mapping. Asset names point at the local reference pack;
 * app behavior is implemented in our own Kotlin code.
 */
object LearningFeedbackRegistry {
    fun variantFor(event: FeedbackEvent): FeedbackVariant {
        return when (event) {
            FeedbackEvent.TapPrimary -> FeedbackVariant(
                animationTimeMs = 180,
                sound = SoundAsset("cta_node_press_quiet", volume = 0.30f, rate = 1.04f),
                haptic = HapticAsset("cta_button_v3_l1", "cta_button_v3_l2", "cta_button_v3_l3"),
            )

            FeedbackEvent.TapSecondary -> FeedbackVariant(
                animationTimeMs = 140,
                sound = SoundAsset("cta_node_press_quiet", volume = 0.22f, rate = 1.12f),
                haptic = HapticAsset("cta_node_press_v3_l1", "cta_node_press_v3_l2", "cta_node_press_v3_l3"),
            )

            FeedbackEvent.OptionSelect -> FeedbackVariant(
                animationTimeMs = 180,
                sound = SoundAsset("cta_node_release_popup_quiet", volume = 0.26f, rate = 1.08f),
                haptic = HapticAsset("cta_node_release_v3_l1", "cta_node_release_v3_l2", "cta_node_release_v3_l3"),
            )

            is FeedbackEvent.AnswerCorrect -> FeedbackVariant(
                animationTimeMs = 600,
                sound = SoundAsset("right_answer", volume = 1f, rate = 1f),
                haptic = HapticAsset("flashcards_correct_haptic_l1", "flashcards_correct_haptic_l2", "flashcards_correct_haptic"),
                visual = LearningAssetRegistry.answerCorrect,
            )

            FeedbackEvent.AnswerWrong -> FeedbackVariant(
                animationTimeMs = 500,
                sound = SoundAsset("wrong_answer", volume = 1f, rate = 1f),
                haptic = HapticAsset("flashcards_final_incorrect_haptic_l1", "flashcards_final_incorrect_haptic_l2", "flashcards_final_incorrect_haptic"),
            )

            is FeedbackEvent.Combo -> FeedbackVariant(
                animationTimeMs = 720,
                sound = SoundAsset("correct_answer_5_in_a_row_lightning", volume = 0.66f, rate = 1f),
                haptic = HapticAsset("score_checkpoint_progressbar_medium_v5_l1", "score_checkpoint_progressbar_medium_v5_l2", "score_checkpoint_progressbar_medium_v5_l3"),
                visual = VisualAsset.Rive("streakmidlesson_xinarow_31"),
            )

            FeedbackEvent.LessonStepComplete -> FeedbackVariant(
                animationTimeMs = 260,
                sound = SoundAsset("score_checkpoint_progress_bar_sound_small", volume = 0.36f, rate = 1.15f),
                haptic = HapticAsset("score_checkpoint_progressbar_small_v5_l1", "score_checkpoint_progressbar_small_v5_l2", "score_checkpoint_progressbar_small_v5_l3"),
                visual = VisualAsset.Lottie("checklist_check_gradient"),
            )

            FeedbackEvent.LessonNodeUnlock -> FeedbackVariant(
                animationTimeMs = 760,
                sound = SoundAsset("node_complete", volume = 0.58f, rate = 1.02f),
                haptic = HapticAsset("node_complete_l1", "node_complete_l2", "node_complete_l3"),
                visual = VisualAsset.Rive("node_and_ring_07"),
            )

            is FeedbackEvent.XpGain -> FeedbackVariant(
                animationTimeMs = 260,
                sound = SoundAsset("score_increase_standard_first_half", volume = 0.32f, rate = 1.10f),
                haptic = HapticAsset("score_checkpoint_progressbar_small_v5_l1", "score_checkpoint_progressbar_small_v5_l2", "score_checkpoint_progressbar_small_v5_l3"),
                visual = VisualAsset.Rive("score_increase_v19_deliv"),
            )

            is FeedbackEvent.StreakExtend -> FeedbackVariant(
                animationTimeMs = 900,
                sound = SoundAsset("streak_classic_flame", volume = 0.60f, rate = 1f),
                haptic = HapticAsset("streak_classic_v3_l1", "streak_classic_v3_l2"),
                visual = LearningAssetRegistry.streakFlame,
            )

            FeedbackEvent.LessonComplete -> FeedbackVariant(
                animationTimeMs = 1500,
                sound = SoundAsset("applause_big", volume = 0.68f, rate = 1f),
                haptic = HapticAsset("lessoncomplete_v6_l1", "lessoncomplete_v6_l2", "lessoncomplete_v6_l3"),
                visual = LearningAssetRegistry.lessonCompleteSparkles,
            )

            is FeedbackEvent.ReviewScheduled -> FeedbackVariant(
                animationTimeMs = 620,
                sound = SoundAsset("score_checkpoint_progress_bar_sound_medium", volume = 0.42f, rate = 1f),
                haptic = HapticAsset("score_checkpoint_progressbar_medium_v5_l1", "score_checkpoint_progressbar_medium_v5_l2", "score_checkpoint_progressbar_medium_v5_l3"),
                visual = VisualAsset.Lottie("boosted_energy_widget"),
            )
        }
    }
}

object DuolingoLikeAssetRegistry {
    fun variantFor(event: FeedbackEvent): FeedbackVariant = LearningFeedbackRegistry.variantFor(event)
}
