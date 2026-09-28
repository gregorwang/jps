package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.delay

/**
 * ♥ 掌握 on a feed card (double tap or the rail): a big heart pops in slightly tilted, holds, and
 * fades; [onDone] fires when it has gone. Reduced motion: it just shows for a moment.
 */
@Composable
fun HeartBurst(modifier: Modifier = Modifier, size: Dp = 132.dp, onDone: () -> Unit = {}) {
    val reduced = rememberReducedMotion()
    val scale = remember { Animatable(if (reduced) 1f else 0.4f) }
    val alpha = remember { Animatable(if (reduced) 0.92f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            alpha.animateTo(0.92f, tween(MotionTokens.Dur.Press))
            scale.animateTo(1.12f, tween(MotionTokens.Dur.Stamp, easing = MotionTokens.Ease.Decelerate))
            scale.animateTo(1f, tween(MotionTokens.Dur.Press))
        }
        delay(320)
        alpha.animateTo(0f, tween(MotionTokens.Dur.State, easing = MotionTokens.Ease.Accelerate))
        onDone()
    }
    Icon(
        Icons.Rounded.Favorite,
        contentDescription = null,
        tint = AjlTheme.colors.heart,
        modifier = modifier
            .size(size)
            .clearAndSetSemantics {}
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = -8f
                this.alpha = alpha.value
            },
    )
}
