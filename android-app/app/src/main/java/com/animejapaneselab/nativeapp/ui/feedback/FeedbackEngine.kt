package com.animejapaneselab.nativeapp.ui.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import com.animejapaneselab.nativeapp.ui.rive.RiveMascotController

class FeedbackEngine(
    private val settings: FeedbackSettings,
    private val soundFx: SoundFx,
    private val haptics: Haptics,
    private val mascot: RiveMascotController,
) {
    fun emit(event: FeedbackEvent) {
        val variant = LearningFeedbackRegistry.variantFor(event)
        if (processFeedbackSoundPlaybackGate.shouldPlay(event, settings.soundEnabled)) {
            variant.sound?.let(soundFx::play)
        }
        if (settings.hapticsEnabled) haptics.perform(event.hapticKind(), variant.haptic)
        mascot.trigger(
            name = event.mascotTrigger() ?: "idle",
            visual = variant.visual.takeIf { settings.richAnimationsEnabled },
        )
    }
}

val LocalFeedbackEngine = staticCompositionLocalOf<FeedbackEngine?> { null }
val LocalRiveMascotController = staticCompositionLocalOf<RiveMascotController?> { null }

@Composable
fun rememberFeedbackEngine(settings: FeedbackSettings): FeedbackEngine {
    val soundFx = rememberSoundFx()
    val haptics = rememberHaptics()
    val mascot = remember { RiveMascotController() }
    return remember(settings, soundFx, haptics, mascot) {
        FeedbackEngine(settings, soundFx, haptics, mascot)
    }
}

@Composable
fun ProvideFeedbackEngine(
    settings: FeedbackSettings,
    content: @Composable () -> Unit,
) {
    val soundFx = rememberSoundFx()
    val haptics = rememberHaptics()
    val mascot = remember { RiveMascotController() }
    val engine = remember(settings, soundFx, haptics, mascot) {
        FeedbackEngine(settings, soundFx, haptics, mascot)
    }
    DisposableEffect(Unit) {
        mascot.trigger("idle")
        onDispose { }
    }
    CompositionLocalProvider(
        LocalFeedbackEngine provides engine,
        LocalRiveMascotController provides mascot,
        content = content,
    )
}

private fun FeedbackEvent.hapticKind(): HapticKind? {
    return when (this) {
        FeedbackEvent.TapPrimary,
        FeedbackEvent.TapSecondary,
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

private fun FeedbackEvent.mascotTrigger(): String? {
    return when (this) {
        is FeedbackEvent.AnswerCorrect -> "correct"
        FeedbackEvent.AnswerWrong -> "wrong"
        is FeedbackEvent.Combo -> "combo"
        FeedbackEvent.LessonNodeUnlock -> "unlock"
        is FeedbackEvent.StreakExtend -> "streak"
        FeedbackEvent.LessonComplete -> "complete"
        else -> null
    }
}
