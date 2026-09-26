package com.animejapaneselab.nativeapp.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * v3 page change (MOTION_SPEC §2): the new page slides in from 24dp to the right and fades in
 * over 260ms; going back reverses. Reduced motion swaps instantly.
 */
fun pageTransform(forward: Boolean, reducedMotion: Boolean, density: Density): ContentTransform {
    if (reducedMotion) return ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)
    val shift = with(density) { 24.dp.roundToPx() }
    val dir = if (forward) 1 else -1
    val d = MotionTokens.Dur.Page
    return ContentTransform(
        targetContentEnter = slideInHorizontally(tween(d, easing = MotionTokens.Ease.Decelerate)) { dir * shift } +
            fadeIn(tween(d, easing = MotionTokens.Ease.Decelerate)),
        initialContentExit = slideOutHorizontally(tween(d, easing = MotionTokens.Ease.Accelerate)) { -dir * shift } +
            fadeOut(tween(d / 2, easing = MotionTokens.Ease.Accelerate)),
        sizeTransform = SizeTransform(clip = false),
    )
}

@Composable
fun LessonPageTransition(
    targetIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val density = androidx.compose.ui.platform.LocalDensity.current
    AnimatedContent(
        targetState = targetIndex,
        modifier = modifier,
        transitionSpec = { pageTransform(targetState >= initialState, reducedMotion, density) },
        label = "lesson-page-transition",
    ) { index ->
        content(index)
    }
}
