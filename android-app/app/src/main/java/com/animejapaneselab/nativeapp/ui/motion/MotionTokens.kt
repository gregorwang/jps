package com.animejapaneselab.nativeapp.ui.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * v3 motion tokens — design/MOTION_SPEC.md §1. Everyday motion is 90–240ms with no bounce;
 * only the eight anime moments (§3) perform. Reduced motion (animator scale 0) snaps to the
 * final state while haptics and sound still fire.
 */
object MotionTokens {
    object Ease {
        /** Most state changes. */
        val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        /** Entrances, sheets sliding in. */
        val Decelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
        /** Exits. */
        val Accelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    }

    object Dur {
        const val Press = 90
        const val Release = 180
        const val State = 180
        const val Sheet = 240
        const val Scrim = 200
        const val Page = 260
        const val Stamp = 220
        const val Progress = 300
        const val PaletteOpen = 160
        const val PaletteClose = 120
        const val TypeChar = 35
        const val TypeCommaPause = 120
        const val TypePeriodPause = 200
        const val TileFly = 240
        const val TileReturn = 180
        const val CardFlip = 320
        const val CardRise = 200
        const val LoadingDotStep = 300
        const val LoadingDelay = 2_000
    }

    /** Critically damped settle after a drag release — never overshoots. */
    fun <T> settle(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 600f)

    fun <T> standard(durationMillis: Int, reducedMotion: Boolean = false): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationMillis, easing = Ease.Standard)

    fun <T> decelerate(durationMillis: Int, reducedMotion: Boolean = false): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationMillis, easing = Ease.Decelerate)

    fun <T> accelerate(durationMillis: Int, reducedMotion: Boolean = false): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationMillis, easing = Ease.Accelerate)

    /** Duration collapsed to 0 under reduced motion. */
    fun duration(baseMillis: Int, reducedMotion: Boolean): Int = if (reducedMotion) 0 else baseMillis
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.isReducedMotionEnabled() }
}

fun Context.isReducedMotionEnabled(): Boolean {
    val animatorScale = runCatching {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    val transitionScale = runCatching {
        Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    }.getOrDefault(1f)
    return animatorScale == 0f || transitionScale == 0f
}
