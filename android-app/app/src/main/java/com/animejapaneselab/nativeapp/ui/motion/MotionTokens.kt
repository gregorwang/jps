package com.animejapaneselab.nativeapp.ui.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object MotionTokens {
    object Duration {
        const val TapDown = 70
        const val TapUp = 140
        const val Micro = 160
        const val CardEnter = 240
        const val PageTransition = 280
        const val AnswerFeedback = 420
        const val AnswerWrongShake = 360
        const val NodeUnlock = 700
        const val XpCount = 900
        const val LessonComplete = 1600
    }

    object Scale {
        const val ButtonPressed = 0.97f
        const val OptionPressed = 0.98f
        const val PopOvershoot = 1.12f
        const val NodeActive = 1.04f
    }

    object Curve {
        val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val Decelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
        val Shake: Easing = CubicBezierEasing(0.36f, 0f, 0.66f, -0.56f)
    }

    fun duration(baseMillis: Int, reducedMotion: Boolean): Int {
        return if (reducedMotion) 1 else baseMillis
    }

    fun microSpec(reducedMotion: Boolean) = tween<Float>(
        durationMillis = duration(Duration.Micro, reducedMotion),
        easing = Curve.Standard,
    )

    fun softSpring(reducedMotion: Boolean) = if (reducedMotion) {
        tween<Float>(durationMillis = 1)
    } else {
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    }

    fun popSpring(reducedMotion: Boolean) = if (reducedMotion) {
        tween<Float>(durationMillis = 1)
    } else {
        spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMedium)
    }
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
