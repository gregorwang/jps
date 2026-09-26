package com.animejapaneselab.nativeapp.ui.screens.review

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** What one flip card shows. Built from a [com.animejapaneselab.nativeapp.domain.SmartReviewEntry]. */
data class ReviewCard(
    val key: String,
    /** 拼句 · 第一話 · 错 2 次 */
    val meta: String,
    /** 今天 / 逾期 3 天 / 10.2 */
    val due: String?,
    val dueNow: Boolean,
    /** Front: the prompt / line. */
    val front: String,
    /** Back: 正解. */
    val answer: String?,
    /** Back: what the learner answered last time. */
    val yourAnswer: String?,
    /** Back: a short note (explanation, or the last review state for remote tasks). */
    val note: String?,
    /** Set for local mistakes — a right swipe marks them reviewed. */
    val localMistakeId: String?,
)

enum class SwipeDirection { Left, Right }

private const val FlyOutFraction = 0.35f
private const val DegreesPer100Dp = 4f

/**
 * 翻卡堆 (MOTION_SPEC §3-07). Tap the top card to flip it 180° around Y (320ms, camera distance
 * 12×density, work-soft back). Drag sideways: it follows the finger and tilts 4° per 100dp;
 * past 35% of the width it flies out, otherwise it settles back critically damped. The next
 * card rises from the stack (0.96→1, 8dp→0, 200ms). No haptics — §4 lists none for the deck.
 */
@Composable
fun ReviewDeck(
    cards: List<ReviewCard>,
    onSwiped: (ReviewCard, SwipeDirection) -> Unit,
    onPractice: (ReviewCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(196.dp),
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val visible = cards.take(3)
        visible.indices.reversed().forEach { depth ->
            val card = visible[depth]
            key(card.key) {
                DeckCard(
                    card = card,
                    depth = depth,
                    containerWidthPx = widthPx,
                    onSwiped = onSwiped,
                    onPractice = onPractice,
                )
            }
        }
    }
}

/** Resting tilt of a card in the stack: top 0°, second -3°, third +4° (V3Review). */
private fun restRotation(depth: Float): Float = when {
    depth <= 1f -> -3f * depth
    else -> -3f + 7f * (depth - 1f).coerceAtMost(1f)
}

@Composable
private fun DeckCard(
    card: ReviewCard,
    depth: Int,
    containerWidthPx: Float,
    onSwiped: (ReviewCard, SwipeDirection) -> Unit,
    onPractice: (ReviewCard) -> Unit,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val reduced = rememberReducedMotion()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isTop = depth == 0
    val depthAnim by animateFloatAsState(
        targetValue = depth.toFloat(),
        animationSpec = MotionTokens.decelerate(MotionTokens.Dur.CardRise, reduced),
        label = "deck-rise",
    )
    val offsetX = remember { Animatable(0f) }
    val flip = remember { Animatable(0f) }
    val stepPx = with(density) { 8.dp.toPx() }
    val per100Px = with(density) { 100.dp.toPx() }
    val threshold = containerWidthPx * FlyOutFraction

    fun toggleFlip() {
        scope.launch {
            val target = if (flip.targetValue < 90f) 180f else 0f
            flip.animateTo(target, MotionTokens.standard(MotionTokens.Dur.CardFlip, reduced))
        }
    }

    fun release() {
        scope.launch {
            val x = offsetX.value
            if (abs(x) > threshold) {
                val direction = if (x > 0) SwipeDirection.Right else SwipeDirection.Left
                offsetX.animateTo(sign(x) * containerWidthPx * 1.4f, MotionTokens.accelerate(MotionTokens.Dur.CardRise, reduced))
                onSwiped(card, direction)
            } else {
                if (reduced) offsetX.snapTo(0f) else offsetX.animateTo(0f, MotionTokens.settle())
            }
        }
    }

    fun flyOut(direction: SwipeDirection) {
        scope.launch {
            val s = if (direction == SwipeDirection.Right) 1f else -1f
            offsetX.animateTo(s * containerWidthPx * 1.4f, MotionTokens.accelerate(MotionTokens.Dur.CardRise, reduced))
            onSwiped(card, direction)
        }
    }

    val rightLabel = if (card.localMistakeId != null) "覚えた" else "次へ"
    val showBack by remember { derivedStateOf { flip.value >= 90f } }
    val dragRight by remember { derivedStateOf { offsetX.value >= 0f } }
    val gesture = if (isTop) {
        Modifier
            .pointerInput(card.key) {
                detectHorizontalDragGestures(
                    onDragEnd = { release() },
                    onDragCancel = { release() },
                ) { change, amount ->
                    change.consume()
                    scope.launch { offsetX.snapTo(offsetX.value + amount) }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "翻面",
                onClick = ::toggleFlip,
            )
            .semantics {
                contentDescription = listOfNotNull(card.meta, card.front, card.answer?.let { "正解 $it" }).joinToString("，")
                stateDescription = if (flip.targetValue >= 90f) "背面" else "正面"
                customActions = listOf(
                    CustomAccessibilityAction(rightLabel) { flyOut(SwipeDirection.Right); true },
                    CustomAccessibilityAction("まだ · 下一张") { flyOut(SwipeDirection.Left); true },
                )
            }
    } else {
        Modifier
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 26.dp, top = 10.dp)
            .height(170.dp)
            .graphicsLayer {
                val d = depthAnim
                val s = 1f - 0.04f * d
                scaleX = s
                scaleY = s
                translationY = stepPx * d
                translationX = offsetX.value
                rotationZ = restRotation(d) + offsetX.value / per100Px * DegreesPer100Dp
                rotationY = flip.value
                cameraDistance = 12f * this.density
            }
            .then(gesture),
    ) {
        MangaPanel(
            modifier = Modifier.fillMaxSize(),
            background = if (showBack) work.soft else colors.surface,
        ) {
            // Stack decoration: the card right under the top one is work-soft with screentone.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val d = depthAnim
                        alpha = if (showBack) 0f else if (d <= 1f) d else (2f - d).coerceIn(0f, 1f)
                    }
                    .background(work.soft)
                    .screentone(work.tone(0.28f), angleDegrees = 0f),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = (1f - depthAnim).coerceIn(0f, 1f)
                        if (showBack) rotationY = 180f
                    }
                    .padding(16.dp),
            ) {
                if (showBack) CardBack(card, onPractice = { onPractice(card) }) else CardFront(card)
            }
            if (isTop) {
                // Swipe hint that fades in with the drag distance.
                Text(
                    text = if (dragRight) rightLabel else "まだ",
                    style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp),
                    color = if (dragRight && card.localMistakeId != null) colors.ok else colors.ink3,
                    modifier = Modifier
                        .align(if (dragRight) Alignment.BottomStart else Alignment.BottomEnd)
                        .padding(16.dp)
                        .graphicsLayer {
                            alpha = (abs(offsetX.value) / threshold).coerceIn(0f, 1f)
                            if (showBack) rotationY = 180f
                        },
                )
            }
        }
    }
}

@Composable
private fun CardFront(card: ReviewCard) {
    val colors = AjlTheme.colors
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Eyebrow(card.meta, modifier = Modifier.weight(1f))
            if (card.due != null) {
                Eyebrow(card.due, color = if (card.dueNow) AjlTheme.work.accent else colors.ink3)
            }
        }
        Text(
            card.front,
            style = AjlTheme.type.jpDisplay.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
            color = colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text("轻点翻面", style = AjlTheme.type.caption, color = colors.ink3)
    }
}

@Composable
private fun CardBack(card: ReviewCard, onPractice: () -> Unit) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (card.answer != null) {
            AnswerLine("正解", card.answer, colors.ok, emphasize = true)
        }
        if (card.yourAnswer != null) {
            AnswerLine("你答", card.yourAnswer, colors.bad, emphasize = false)
        }
        if (card.note != null) {
            Text(
                card.note,
                style = type.caption,
                color = colors.ink2,
                maxLines = if (card.answer != null) 2 else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Box(Modifier.weight(1f))
        OutlineButton(text = "再练", onClick = onPractice, compact = true)
    }
}

@Composable
private fun AnswerLine(label: String, value: String, tint: androidx.compose.ui.graphics.Color, emphasize: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Text(label, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3, modifier = Modifier.width(36.dp).padding(top = 3.dp))
        Text(
            value,
            style = if (emphasize) AjlTheme.type.jpBody.copy(fontSize = 18.sp, lineHeight = 26.sp) else AjlTheme.type.body,
            color = tint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
