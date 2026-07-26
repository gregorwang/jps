package com.animejapaneselab.nativeapp.ui.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun XpFlyout(
    xp: Int,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    var mounted by remember(visible, xp) { mutableStateOf(visible) }
    LaunchedEffect(visible, xp) {
        if (visible) {
            mounted = true
            delay(MotionTokens.duration(760, reducedMotion).toLong())
            mounted = false
        }
    }
    val offset by animateFloatAsState(
        targetValue = if (mounted && !reducedMotion) -28f else 0f,
        animationSpec = MotionTokens.softSpring(reducedMotion),
        label = "xp-flyout-offset",
    )
    AnimatedVisibility(
        visible = mounted,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.graphicsLayer { translationY = offset },
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = "+$xp XP",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
