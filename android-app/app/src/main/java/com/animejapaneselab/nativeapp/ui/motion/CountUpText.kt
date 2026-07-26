package com.animejapaneselab.nativeapp.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlin.math.roundToInt

@Composable
fun CountUpText(
    target: Int,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    onTick: ((Int) -> Unit)? = null,
) {
    val reducedMotion = rememberReducedMotion()
    val value = remember(target) { Animatable(if (reducedMotion) target.toFloat() else 0f) }
    val latestTick by rememberUpdatedState(onTick)
    LaunchedEffect(target, reducedMotion) {
        if (reducedMotion) {
            value.snapTo(target.toFloat())
        } else {
            var lastTick = 0
            value.animateTo(
                targetValue = target.toFloat(),
                animationSpec = tween(
                    durationMillis = MotionTokens.Duration.XpCount,
                    easing = MotionTokens.Curve.Decelerate,
                ),
            ) {
                val current = this.value.roundToInt()
                if (current / 5 > lastTick / 5) {
                    lastTick = current
                    latestTick?.invoke(current)
                }
            }
        }
    }
    Text(
        text = "$prefix${value.value.roundToInt()}$suffix",
        modifier = modifier,
        color = color,
        style = style,
    )
}
