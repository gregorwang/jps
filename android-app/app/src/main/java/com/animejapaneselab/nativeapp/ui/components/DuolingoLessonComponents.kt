package com.animejapaneselab.nativeapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.LabPalette
import com.animejapaneselab.nativeapp.ui.theme.LabTheme
import kotlin.math.sin

enum class JuicyLessonTone(
    val face: Color,
    val lip: Color,
    val content: Color = Color.White,
) {
    Green(face = LabPalette.Green, lip = LabPalette.GreenDark),
    Blue(face = LabPalette.Blue, lip = lerp(LabPalette.Blue, Color.Black, 0.26f)),
    Red(face = LabPalette.Coral, lip = lerp(LabPalette.Coral, Color.Black, 0.24f)),
    Pink(face = LabPalette.Sakura, lip = LabPalette.SakuraDark),
    Purple(face = LabPalette.Violet, lip = LabPalette.VioletDark),
    Orange(face = LabPalette.Orange, lip = lerp(LabPalette.Orange, Color.Black, 0.24f)),
    White(face = Color.White, lip = LabPalette.Outline, content = LabPalette.Ink),
}

@Composable
fun JuicyLessonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: JuicyLessonTone = JuicyLessonTone.Green,
    contentColorOverride: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val feedbackEngine = LocalFeedbackEngine.current
    val reducedMotion = rememberReducedMotion()
    val colorScheme = MaterialTheme.colorScheme
    val face = if (enabled) tone.face else colorScheme.surfaceContainerHighest
    val lip = if (enabled) tone.lip else colorScheme.outlineVariant
    val content = if (enabled) {
        contentColorOverride ?: tone.content
    } else {
        colorScheme.onSurface.copy(alpha = 0.38f)
    }
    val shape = MaterialTheme.shapes.large
    val lipDrop = 3.dp
    val pressOffset by animateDpAsState(
        targetValue = if (pressed && enabled && !reducedMotion) lipDrop else 0.dp,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 1)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "juicy-button-press",
    )

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = lipDrop)
                .clip(shape)
                .background(lip),
        )
        Surface(
            onClick = {
                feedbackEngine?.emit(FeedbackEvent.TapPrimary)
                onClick()
            },
            enabled = enabled,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .offset { IntOffset(x = 0, y = pressOffset.roundToPx()) },
            color = face,
            contentColor = content,
            shape = shape,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon?.let {
                    Icon(it, contentDescription = null, tint = content, modifier = Modifier.padding(end = 8.dp))
                }
                Text(
                    text = text,
                    color = content,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                trailingIcon?.let {
                    Icon(it, contentDescription = null, tint = content, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
fun JuicyLessonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    heightDp: Int = 14,
    milestoneVisible: Boolean = false,
    pulsing: Boolean = false,
    progressColor: Color = LabPalette.Green,
    trackColor: Color = Color(0xFFE5E5E5),
) {
    val reducedMotion = rememberReducedMotion()
    val target = progress.coerceIn(0f, 1f)
    val animated = remember { Animatable(0f) }
    LaunchedEffect(target, reducedMotion) {
        if (reducedMotion) {
            animated.snapTo(target)
        } else {
            animated.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = MotionTokens.Duration.AnswerFeedback,
                    easing = MotionTokens.Curve.Standard,
                ),
            )
        }
    }
    val shape = RoundedCornerShape((heightDp / 2).dp)

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .clip(shape)
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated.value)
                    .clip(shape)
                    .background(progressColor),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 3.dp)
                        .fillMaxWidth(0.65f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.32f)),
                )
            }
        }
        if (milestoneVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(width = 18.dp, height = 22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, progressColor, RoundedCornerShape(6.dp)),
            )
        }
        if (pulsing && !reducedMotion) {
            PulseRing(color = progressColor, modifier = Modifier.align(Alignment.CenterEnd).size(26.dp))
        }
    }
}

@Composable
fun JuicyLessonChoiceCard(
    text: String,
    detail: String? = null,
    selected: Boolean,
    correct: Boolean,
    wrong: Boolean,
    answered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionColor: Color = LabPalette.Blue,
    selectionContainer: Color = selectionColor.copy(alpha = 0.12f),
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val feedbackEngine = LocalFeedbackEngine.current
    val reducedMotion = rememberReducedMotion()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colorScheme = MaterialTheme.colorScheme
    val extended = LabTheme.colors
    val colorSpec = remember(reducedMotion) {
        tween<Color>(
            durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
            easing = MotionTokens.Curve.Standard,
        )
    }
    val face by animateColorAsState(
        targetValue = when {
            correct -> extended.successContainer
            wrong -> colorScheme.errorContainer
            selected -> selectionContainer
            else -> colorScheme.surface
        },
        animationSpec = colorSpec,
        label = "choice-face",
    )
    val border by animateColorAsState(
        targetValue = when {
            correct -> extended.success
            wrong -> colorScheme.error
            selected -> selectionColor
            else -> colorScheme.outline
        },
        animationSpec = colorSpec,
        label = "choice-border",
    )
    val content by animateColorAsState(
        targetValue = when {
            correct -> extended.onSuccessContainer
            wrong -> colorScheme.onErrorContainer
            selected -> lerp(selectionColor, colorScheme.onSurface, 0.24f)
            else -> colorScheme.onSurface
        },
        animationSpec = colorSpec,
        label = "choice-content",
    )
    val lipColor = when {
        correct -> extended.success
        wrong -> colorScheme.error
        selected -> selectionColor
        else -> colorScheme.outline
    }
    val shape = MaterialTheme.shapes.small
    val pressOffset by animateDpAsState(
        targetValue = if (pressed && !answered && !reducedMotion) 4.dp else 0.dp,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 1)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "choice-press-offset",
    )

    Box(
        modifier = modifier
            .heightIn(min = 58.dp)
            .then(if (wrong) Modifier.shakeOnTrigger(text.hashCode()) else Modifier),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .clip(shape)
                .background(lipColor.copy(alpha = if (answered || selected) 0.75f else 0.55f)),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = 0, y = pressOffset.roundToPx()) }
                .clip(shape)
                .clickable(
                    enabled = !answered,
                    interactionSource = interaction,
                    indication = null,
                    onClick = {
                        feedbackEngine?.emit(FeedbackEvent.OptionSelect)
                        onClick()
                    },
                ),
            shape = shape,
            color = face,
            contentColor = content,
            border = BorderStroke(if (selected || correct || wrong) 2.dp else 1.5.dp, border),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                leadingContent?.invoke()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = content,
                    )
                    detail?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = content.copy(alpha = 0.72f),
                        )
                    }
                }
                ChoiceStatusDot(
                    correct = correct,
                    wrong = wrong,
                    selected = selected,
                    selectionColor = selectionColor,
                )
            }
        }
    }
}

@Composable
private fun ChoiceStatusDot(
    correct: Boolean,
    wrong: Boolean,
    selected: Boolean,
    selectionColor: Color,
) {
    when {
        correct || wrong -> {
            val badge = if (correct) LabTheme.colors.success else MaterialTheme.colorScheme.error
            val onBadge = if (correct) LabTheme.colors.onSuccess else MaterialTheme.colorScheme.onError
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(badge),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (correct) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    tint = onBadge,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        selected -> {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(2.dp, selectionColor, CircleShape),
            )
        }
    }
}

fun Modifier.shakeOnTrigger(trigger: Int): Modifier = composed {
    val reducedMotion = rememberReducedMotion()
    val anim = remember { Animatable(0f) }
    LaunchedEffect(trigger, reducedMotion) {
        if (trigger != 0 && !reducedMotion) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(durationMillis = MotionTokens.Duration.AnswerWrongShake))
        }
    }
    val density = LocalDensity.current
    val offsetPx = with(density) { (sin(anim.value * Math.PI * 6).toFloat() * 8.dp.toPx() * (1f - anim.value)) }
    offset { IntOffset(offsetPx.toInt(), 0) }
}

@Composable
private fun BoxScope.PulseRing(color: Color, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "pulse-ring")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "pulse-ring-scale",
    )
    val alpha by pulse.animateFloat(
        initialValue = 0.38f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "pulse-ring-alpha",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}
