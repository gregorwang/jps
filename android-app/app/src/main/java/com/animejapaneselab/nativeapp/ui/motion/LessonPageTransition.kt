package com.animejapaneselab.nativeapp.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LessonPageTransition(
    targetIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    AnimatedContent(
        targetState = targetIndex,
        modifier = modifier,
        transitionSpec = {
            if (reducedMotion) {
                fadeIn(tween(1)) togetherWith fadeOut(tween(1))
            } else {
                val direction = if (targetState >= initialState) 1 else -1
                ContentTransform(
                    targetContentEnter = slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = MotionTokens.Duration.PageTransition,
                            easing = MotionTokens.Curve.Decelerate,
                        ),
                        initialOffsetX = { fullWidth -> direction * fullWidth / 3 },
                    ) + fadeIn(tween(MotionTokens.Duration.PageTransition)),
                    initialContentExit = slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = MotionTokens.Duration.PageTransition,
                            easing = MotionTokens.Curve.Standard,
                        ),
                        targetOffsetX = { fullWidth -> -direction * fullWidth / 3 },
                    ) + fadeOut(tween(MotionTokens.Duration.PageTransition)),
                    sizeTransform = SizeTransform(clip = false),
                )
            }
        },
        label = "lesson-page-transition",
    ) { index ->
        content(index)
    }
}
