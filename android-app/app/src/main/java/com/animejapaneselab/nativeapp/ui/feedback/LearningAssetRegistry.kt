package com.animejapaneselab.nativeapp.ui.feedback

import androidx.annotation.DrawableRes
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.ui.screens.TrainingPathNodeAction
import com.animejapaneselab.nativeapp.ui.screens.TrainingPathNodeState

internal object LearningAssetRegistry {
    val rewardChest: VisualAsset = VisualAsset.Lottie("chest_open_gold", iterations = Int.MAX_VALUE)
    val completedPathCharacter: VisualAsset = VisualAsset.Lottie("checklist_check_gradient")
    val streakFlame: VisualAsset = VisualAsset.Lottie("streak_calendar_day_flame_pop")
    val answerCorrect: VisualAsset = VisualAsset.Lottie("checkmark_calendar")
    val lessonCompleteSparkles: VisualAsset = VisualAsset.Lottie("session_complete_stats_sparkles", iterations = Int.MAX_VALUE)

    @DrawableRes
    fun courseArtworkFor(workSlug: String): Int {
        return when (workSlug.normalizedWorkSlug()) {
            "k-on" -> R.drawable.course_kon
            "re-zero" -> R.drawable.course_rezero
            else -> R.drawable.course_rezero
        }
    }

    @DrawableRes
    fun courseArtworkFor(workSlug: String, episode: Int): Int {
        return when (workSlug.normalizedWorkSlug()) {
            "k-on" -> R.drawable.course_kon
            "re-zero" -> when (episode.coerceAtLeast(1)) {
                in 1..25 -> R.drawable.course_rezero
                in 26..50 -> R.drawable.course_rezero_s2
                else -> R.drawable.course_rezero_s3
            }
            else -> courseArtworkFor(workSlug)
        }
    }

    @DrawableRes
    fun courseBannerArtworkFor(workSlug: String, episode: Int): Int = courseArtworkFor(workSlug, episode)

    fun soundFor(event: FeedbackEvent): SoundAsset? = LearningFeedbackRegistry.variantFor(event).sound

    fun hapticFor(event: FeedbackEvent): HapticAsset? = LearningFeedbackRegistry.variantFor(event).haptic

    fun visualFor(event: FeedbackEvent): VisualAsset? = LearningFeedbackRegistry.variantFor(event).visual

    fun visualForNode(action: TrainingPathNodeAction, state: TrainingPathNodeState): VisualAsset? {
        return when {
            state == TrainingPathNodeState.Reward -> rewardChest
            action == TrainingPathNodeAction.ReadAir && state != TrainingPathNodeState.Locked -> VisualAsset.Lottie("boosted_energy_widget")
            state == TrainingPathNodeState.Current -> VisualAsset.Rive("node_and_ring_07")
            else -> null
        }
    }
}

private fun String.normalizedWorkSlug(): String {
    return when (this) {
        "rezero" -> "re-zero"
        else -> this
    }
}
