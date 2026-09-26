package com.animejapaneselab.nativeapp.ui.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

class FeedbackEngine(
    private val settings: FeedbackSettings,
    private val soundFx: SoundFx,
    private val haptics: Haptics,
) {
    fun emit(event: FeedbackEvent) {
        if (processFeedbackSoundPlaybackGate.shouldPlay(event, settings.soundEnabled)) {
            event.sound()?.let(soundFx::play)
        }
        if (settings.hapticsEnabled) haptics.perform(event.hapticKind())
    }
}

val LocalFeedbackEngine = staticCompositionLocalOf<FeedbackEngine?> { null }

@Composable
fun ProvideFeedbackEngine(
    settings: FeedbackSettings,
    content: @Composable () -> Unit,
) {
    val soundFx = rememberSoundFx()
    val haptics = rememberHaptics()
    val engine = remember(settings, soundFx, haptics) {
        FeedbackEngine(settings, soundFx, haptics)
    }
    CompositionLocalProvider(
        LocalFeedbackEngine provides engine,
        content = content,
    )
}

/** v3 sound map (MOTION_SPEC section 4): only judgments make a sound, using the Kenney CC0 pack. */
private fun FeedbackEvent.sound(): SoundAsset? {
    return when (this) {
        is FeedbackEvent.AnswerCorrect -> SoundAsset(SoundAsset.Success, volume = 0.8f)
        FeedbackEvent.AnswerWrong -> SoundAsset(SoundAsset.Error, volume = 0.8f)
        FeedbackEvent.LessonComplete -> SoundAsset(SoundAsset.Success, volume = 0.6f)
        else -> null
    }
}

private fun FeedbackEvent.hapticKind(): HapticKind? {
    return when (this) {
        FeedbackEvent.TapPrimary,
        FeedbackEvent.TapSecondary -> null
        FeedbackEvent.OptionSelect,
        FeedbackEvent.LessonStepComplete,
        FeedbackEvent.LessonNodeUnlock,
        is FeedbackEvent.XpGain,
        is FeedbackEvent.ReviewScheduled -> HapticKind.Light
        is FeedbackEvent.AnswerCorrect,
        is FeedbackEvent.Combo,
        is FeedbackEvent.StreakExtend,
        FeedbackEvent.LessonComplete -> HapticKind.Confirm
        FeedbackEvent.AnswerWrong -> HapticKind.Reject
    }
}
