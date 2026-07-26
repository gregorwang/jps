package com.animejapaneselab.nativeapp.ui.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine

enum class LessonNodeVisualState {
    Locked,
    Available,
    Active,
    Completed,
}

@Composable
fun AnimatedLessonNode(
    title: String,
    subtitle: String,
    state: LessonNodeVisualState,
    unlockReason: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalFeedbackEngine.current
    val reducedMotion = rememberReducedMotion()
    var appeared by remember(title, state) { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state == LessonNodeVisualState.Available && !appeared) {
            appeared = true
            feedback?.emit(FeedbackEvent.LessonNodeUnlock)
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (state == LessonNodeVisualState.Active && !reducedMotion) MotionTokens.Scale.NodeActive else 1f,
        animationSpec = MotionTokens.popSpring(reducedMotion),
        label = "lesson-node-scale",
    )
    val container = when (state) {
        LessonNodeVisualState.Locked -> MaterialTheme.colorScheme.surfaceVariant
        LessonNodeVisualState.Available -> MaterialTheme.colorScheme.secondaryContainer
        LessonNodeVisualState.Active -> MaterialTheme.colorScheme.primaryContainer
        LessonNodeVisualState.Completed -> MaterialTheme.colorScheme.primary
    }
    val content = when (state) {
        LessonNodeVisualState.Locked -> MaterialTheme.colorScheme.onSurfaceVariant
        LessonNodeVisualState.Available -> MaterialTheme.colorScheme.onSecondaryContainer
        LessonNodeVisualState.Active -> MaterialTheme.colorScheme.onPrimaryContainer
        LessonNodeVisualState.Completed -> MaterialTheme.colorScheme.onPrimary
    }
    Surface(
        onClick = onClick,
        enabled = state != LessonNodeVisualState.Locked,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = if (state == LessonNodeVisualState.Active) 2.dp else 1.dp,
            color = if (state == LessonNodeVisualState.Locked) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
            },
        ),
        shadowElevation = if (state == LessonNodeVisualState.Active) 5.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                color = content.copy(alpha = 0.14f),
                contentColor = content,
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    imageVector = when (state) {
                        LessonNodeVisualState.Locked -> Icons.Rounded.Lock
                        LessonNodeVisualState.Completed -> Icons.Rounded.Check
                        LessonNodeVisualState.Active -> Icons.Rounded.PlayArrow
                        LessonNodeVisualState.Available -> icon
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(11.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    text = if (state == LessonNodeVisualState.Locked) unlockReason else subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedVisibility(visible = state == LessonNodeVisualState.Completed, enter = fadeIn()) {
                Icon(Icons.Rounded.Check, contentDescription = null)
            }
        }
    }
}
