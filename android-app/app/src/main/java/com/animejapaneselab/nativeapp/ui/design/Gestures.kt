package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * What a row does when swiped past the line. [icon] draws the mark (hollow until armed, then
 * filled); [dismiss] slides the row out before [onCommit] (斩), otherwise it springs back (收藏).
 */
class SwipeAction(
    val label: String,
    val armedLabel: String,
    val background: Color,
    val content: Color,
    val dismiss: Boolean,
    val icon: @Composable (armed: Boolean) -> Unit,
    val onCommit: () -> Unit,
)

/**
 * A list row that can be swiped sideways: [right] is revealed dragging right, [left] dragging
 * left. Past 96dp the mark fills in and the phone ticks once; letting go there commits, short of
 * it the row springs back. Vertical scrolling of the list is untouched.
 */
@Composable
fun SwipeActionRow(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    right: SwipeAction? = null,
    left: SwipeAction? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val threshold = with(density) { 96.dp.toPx() }
    val limit = with(density) { 190.dp.toPx() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var armed by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = with(density) { maxWidth.toPx() }
        val x = offset.value
        val action = if (x > 0f) right else if (x < 0f) left else null
        if (action != null) {
            Row(
                Modifier.matchParentSize().background(action.background).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (x > 0f) Arrangement.Start else Arrangement.End,
            ) {
                val scale by animateFloatAsState(if (armed) 1.08f else 1f, tween(MotionTokens.Dur.Press), label = "swipe-mark")
                Row(
                    Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    action.icon(armed)
                    Text(
                        if (armed) action.armedLabel else action.label,
                        style = AjlTheme.type.label.copy(fontWeight = FontWeight.SemiBold),
                        color = action.content,
                    )
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), 0) }
                .background(AjlTheme.colors.bg)
                .draggable(
                    enabled = enabled && (right != null || left != null),
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val raw = offset.value + delta
                        val allowed = when {
                            raw > 0f && right == null -> 0f
                            raw < 0f && left == null -> 0f
                            abs(raw) > limit -> sign(raw) * (limit + (abs(raw) - limit) * 0.25f)
                            else -> raw
                        }
                        scope.launch { offset.snapTo(allowed) }
                        val nowArmed = abs(allowed) >= threshold
                        if (nowArmed != armed) {
                            armed = nowArmed
                            if (nowArmed) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                    },
                    onDragStopped = {
                        val committed = if (armed) (if (offset.value > 0f) right else left) else null
                        armed = false
                        if (committed != null && committed.dismiss) {
                            offset.animateTo(sign(offset.value) * width, tween(MotionTokens.Dur.State))
                            committed.onCommit()
                            offset.snapTo(0f)
                        } else {
                            offset.animateTo(0f, tween(MotionTokens.Dur.Release))
                            committed?.onCommit()
                        }
                    },
                ),
        ) { content() }
    }
}

/** Ink bar above the tab bar: what just happened, and 撤销. */
@Composable
fun UndoBar(text: String, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colors.ink, AjlShape.Tool)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = AjlTheme.type.label, color = colors.onInk, modifier = Modifier.weight(1f), maxLines = 1)
        QuietButton("撤销", onUndo, color = colors.onInk)
    }
}
