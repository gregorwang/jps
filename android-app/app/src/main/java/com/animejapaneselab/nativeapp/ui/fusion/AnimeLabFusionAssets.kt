package com.animejapaneselab.nativeapp.ui.fusion

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieConstants
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.BuildConfig

data class FusionRolloutPolicy(
    val useFusionSessionComplete: Boolean,
    val useFusionTodayHero: Boolean,
    val useFusionLessonVisuals: Boolean,
    val useFusionTrainingPathVisuals: Boolean,
    val allowInternalReferenceAssets: Boolean,
)

object AnimeLabFusionRollout {
    /**
     * One local kill switch for the first vertical slice. This remains separate from motion
     * fallback so the whole section can be returned to the legacy implementation immediately.
     */
    val current = FusionRolloutPolicy(
        useFusionSessionComplete = true,
        useFusionTodayHero = true,
        useFusionLessonVisuals = true,
        useFusionTrainingPathVisuals = true,
        allowInternalReferenceAssets = BuildConfig.ALLOW_INTERNAL_REFERENCE_ASSETS,
    )
}

object AnimeLabFusionAssetResolver : FusionAssetResolver {
    override fun resolve(key: FusionVisualKey): FusionVisualResolution {
        val policy = AnimeLabFusionRollout.current
        if (!policy.allowInternalReferenceAssets) {
            return FusionVisualResolution.Unavailable(FusionUnavailableReason.LicenseBlocked)
        }
        return when (key) {
            FusionVisualKey.SessionCompleteCelebration -> if (!policy.useFusionSessionComplete) {
                FusionVisualResolution.Unavailable(FusionUnavailableReason.FeatureDisabled)
            } else FusionVisualResolution.Available(
                primary = FusionMotionSpec.LayeredLottie(
                    layers = listOf(
                        FusionLottieLayer(
                            rawRes = R.raw.complete_particles_loop,
                            width = 310.dp,
                            height = 210.dp,
                            alignment = Alignment.TopCenter,
                            alpha = 0.38f,
                        ),
                        FusionLottieLayer(
                            rawRes = R.raw.complete_stars_loop,
                            width = 280.dp,
                            height = 230.dp,
                            alpha = 0.5f,
                        ),
                    ),
                ),
                fallbacks = listOf(
                    FusionMotionSpec.Lottie(
                        rawRes = R.raw.session_complete_stats_sparkles,
                        iterations = LottieConstants.IterateForever,
                    ),
                ),
                metadata = FusionVisualCatalog.metadataFor(key),
            )

            FusionVisualKey.TrainingPathAmbientCompanion -> if (!policy.useFusionTrainingPathVisuals) {
                FusionVisualResolution.Unavailable(FusionUnavailableReason.FeatureDisabled)
            } else FusionVisualResolution.Available(
                primary = FusionMotionSpec.Lottie(
                    rawRes = R.raw.path_bea_smores,
                    iterations = LottieConstants.IterateForever,
                ),
                fallbacks = emptyList(),
                metadata = FusionVisualCatalog.metadataFor(key),
            )

            FusionVisualKey.TodayHeroCompanion -> if (!policy.useFusionTodayHero) {
                FusionVisualResolution.Unavailable(FusionUnavailableReason.FeatureDisabled)
            } else FusionVisualResolution.Available(
                primary = FusionMotionSpec.Lottie(
                    rawRes = R.raw.path_bea_smores,
                    iterations = LottieConstants.IterateForever,
                ),
                fallbacks = emptyList(),
                metadata = FusionVisualCatalog.metadataFor(key),
            )

            FusionVisualKey.LessonPromptCompanion -> if (!policy.useFusionLessonVisuals) {
                FusionVisualResolution.Unavailable(FusionUnavailableReason.FeatureDisabled)
            } else FusionVisualResolution.Available(
                primary = FusionMotionSpec.Lottie(
                    rawRes = R.raw.duo_normal_mid_lesson,
                    iterations = LottieConstants.IterateForever,
                ),
                fallbacks = emptyList(),
                metadata = FusionVisualCatalog.metadataFor(key),
            )

            FusionVisualKey.LessonAudioSpeaker -> if (!policy.useFusionLessonVisuals) {
                FusionVisualResolution.Unavailable(FusionUnavailableReason.FeatureDisabled)
            } else FusionVisualResolution.Available(
                primary = FusionMotionSpec.Lottie(
                    rawRes = R.raw.speaker_normal_ocean,
                    iterations = LottieConstants.IterateForever,
                ),
                fallbacks = emptyList(),
                metadata = FusionVisualCatalog.metadataFor(key),
            )

            FusionVisualKey.LessonAnswerCorrectIcon,
            FusionVisualKey.LessonAnswerWrongIcon,
            FusionVisualKey.TrainingPathLockedCompanionSmores,
            FusionVisualKey.TrainingPathLockedCompanionTennis,
            FusionVisualKey.TrainingPathLockedRewardChest,
            FusionVisualKey.TrainingPathGuidebookIcon -> FusionVisualResolution.Unavailable(
                FusionUnavailableReason.PackNotInstalled,
            )
        }
    }
}

object AnimeLabFusionDrawableResolver : FusionDrawableResolver {
    override fun resolveDrawable(key: FusionVisualKey): FusionDrawableResolution {
        val policy = AnimeLabFusionRollout.current
        if (!policy.allowInternalReferenceAssets) {
            return FusionDrawableResolution.Unavailable(FusionUnavailableReason.LicenseBlocked)
        }
        val pathKey = key == FusionVisualKey.TrainingPathLockedCompanionSmores ||
            key == FusionVisualKey.TrainingPathLockedCompanionTennis ||
            key == FusionVisualKey.TrainingPathLockedRewardChest ||
            key == FusionVisualKey.TrainingPathGuidebookIcon
        val lessonKey = key == FusionVisualKey.LessonAnswerCorrectIcon ||
            key == FusionVisualKey.LessonAnswerWrongIcon
        if (pathKey && !policy.useFusionTrainingPathVisuals) {
            return FusionDrawableResolution.Unavailable(FusionUnavailableReason.FeatureDisabled)
        }
        if (lessonKey && !policy.useFusionLessonVisuals) {
            return FusionDrawableResolution.Unavailable(FusionUnavailableReason.FeatureDisabled)
        }
        val drawableRes = when (key) {
            FusionVisualKey.LessonAnswerCorrectIcon -> R.drawable.duo_radio_check_correct
            FusionVisualKey.LessonAnswerWrongIcon -> R.drawable.duo_radio_x_incorrect
            FusionVisualKey.TrainingPathLockedCompanionSmores -> R.drawable.path_bea_smores_locked
            FusionVisualKey.TrainingPathLockedCompanionTennis -> R.drawable.path_bea_tennis_locked
            FusionVisualKey.TrainingPathLockedRewardChest -> R.drawable.common_level_chest_locked_v2
            FusionVisualKey.TrainingPathGuidebookIcon -> R.drawable.guidebook_white
            FusionVisualKey.SessionCompleteCelebration,
            FusionVisualKey.TodayHeroCompanion,
            FusionVisualKey.LessonPromptCompanion,
            FusionVisualKey.LessonAudioSpeaker,
            FusionVisualKey.TrainingPathAmbientCompanion -> return FusionDrawableResolution.Unavailable(
                FusionUnavailableReason.PackNotInstalled,
            )
        }
        return FusionDrawableResolution.Available(
            drawableRes = drawableRes,
            metadata = FusionVisualCatalog.metadataFor(key),
        )
    }
}
