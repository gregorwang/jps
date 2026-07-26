package com.animejapaneselab.nativeapp.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine

enum class AnswerOptionState {
    Idle,
    Selected,
    Correct,
    Wrong,
    RevealedCorrect,
    Disabled,
}

@Composable
fun AnimatedAnswerOption(
    text: String,
    state: AnswerOptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    enabled: Boolean = true,
    xp: Int = 1,
) {
    val feedback = LocalFeedbackEngine.current
    val reducedMotion = rememberReducedMotion()
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state, reducedMotion) {
        if (state == AnswerOptionState.Wrong && !reducedMotion) {
            shake.snapTo(0f)
            repeat(3) {
                shake.animateTo(-10f, tween(45, easing = MotionTokens.Curve.Shake))
                shake.animateTo(10f, tween(90, easing = MotionTokens.Curve.Shake))
            }
            shake.animateTo(0f, tween(45))
        }
    }

    val selected = state == AnswerOptionState.Selected
    val correct = state == AnswerOptionState.Correct || state == AnswerOptionState.RevealedCorrect
    val wrong = state == AnswerOptionState.Wrong
    val disabled = state == AnswerOptionState.Disabled
    val container = when {
        correct -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        wrong -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        disabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surface
    }
    val content = when {
        correct -> MaterialTheme.colorScheme.primary
        wrong -> MaterialTheme.colorScheme.error
        disabled -> MaterialTheme.colorScheme.onSurfaceVariant
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val border = when {
        correct -> MaterialTheme.colorScheme.primary
        wrong -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.46f)
    }
    val scale by animateFloatAsState(
        targetValue = when {
            selected && !reducedMotion -> MotionTokens.Scale.OptionPressed
            correct && !reducedMotion -> 1.01f
            else -> 1f
        },
        animationSpec = MotionTokens.popSpring(reducedMotion),
        label = "answer-option-scale",
    )
    val iconScale by animateFloatAsState(
        targetValue = if ((correct || wrong) && !reducedMotion) MotionTokens.Scale.PopOvershoot else 1f,
        animationSpec = MotionTokens.popSpring(reducedMotion),
        label = "answer-option-icon-scale",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected || correct || wrong) 1.dp else 5.dp,
        label = "answer-option-elevation",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected || correct || wrong) 2.dp else 1.dp,
        label = "answer-option-border-width",
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = {
                feedback?.emit(FeedbackEvent.OptionSelect)
                onClick()
            },
            enabled = enabled && !disabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = shake.value
                },
            shape = MaterialTheme.shapes.large,
            color = container,
            contentColor = content,
            border = BorderStroke(borderWidth, border),
            shadowElevation = elevation,
            tonalElevation = if (selected || correct || wrong) 1.dp else 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = text, fontWeight = FontWeight.Black, textAlign = TextAlign.Start)
                    detail?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = content.copy(alpha = 0.78f),
                        )
                    }
                }
                Icon(
                    imageVector = when {
                        correct -> Icons.Rounded.CheckCircle
                        wrong -> Icons.Rounded.Close
                        else -> Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                    tint = if (correct || wrong || selected) content else Color.Transparent,
                )
            }
        }
        XpFlyout(
            xp = xp,
            visible = state == AnswerOptionState.Correct,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp),
        )
    }
}
