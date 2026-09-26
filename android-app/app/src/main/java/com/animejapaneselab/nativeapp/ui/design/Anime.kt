package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Manga image filter (ASSET_REMIX.md fallback): desaturate + contrast, then work-colour tone.
// ---------------------------------------------------------------------------

/** Greyscale + contrast so every source sheet reads as black-and-white manga. */
fun mangaColorFilter(contrast: Float = 1.35f): ColorFilter {
    val grey = ColorMatrix().apply { setToSaturation(0f) }
    val t = 128f * (1f - contrast)
    val boost = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, t,
            0f, contrast, 0f, 0f, t,
            0f, 0f, contrast, 0f, t,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
    grey.timesAssign(boost)
    return ColorFilter.colorMatrix(grey)
}

/**
 * Character portrait cropped to the face. With [manga] the image is filtered through
 * [mangaColorFilter] and multiplied with a work-colour screentone; missing images fall back to
 * a kanji circle (soft fill + tone + the character's mark).
 */
@Composable
fun Avatar(
    character: CharacterRef?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    highlighted: Boolean = false,
    manga: Boolean = true,
    contentDescription: String? = character?.name,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val ring = if (highlighted) work.accent else colors.line2
    val ringWidth = if (highlighted) AjlStroke.Ink else AjlStroke.Hair
    val base = modifier
        .size(size)
        .clip(CircleShape)
        .semantics { if (contentDescription != null) this.contentDescription = contentDescription }
    val drawable = character?.drawable
    if (drawable == null) {
        Box(
            base
                .background(work.soft)
                .screentone(work.tone(0.26f), angleDegrees = 0f, spacing = 6.dp, dotRadius = 1.1.dp)
                .border(if (highlighted) AjlStroke.Ink else AjlStroke.Hair, if (highlighted) work.accent else colors.line2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                character?.mark ?: "？",
                style = AjlTheme.type.jpTitle.copy(fontWeight = FontWeight.Black, fontSize = (size.value * 0.38f).sp, lineHeight = (size.value * 0.42f).sp),
                color = work.accent,
            )
        }
        return
    }
    val image = ImageBitmap.imageResource(drawable)
    val face = character.face
    val filter = remember(manga) { if (manga) mangaColorFilter() else null }
    val tone = work.tone(0.30f)
    val surface = colors.surface
    Canvas(base.border(ringWidth, ring, CircleShape)) {
        drawRect(surface)
        val side = (image.width * face.size).coerceAtMost(minOf(image.width, image.height).toFloat())
        val left = (image.width * face.cx - side / 2f).coerceIn(0f, image.width - side)
        val top = (image.height * face.cy - side / 2f).coerceIn(0f, image.height - side)
        drawImage(
            image = image,
            srcOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            srcSize = IntSize(side.roundToInt(), side.roundToInt()),
            dstSize = IntSize(this.size.width.roundToInt(), this.size.height.roundToInt()),
            colorFilter = filter,
        )
        if (manga) {
            val step = 5.dp.toPx()
            val r = 0.9.dp.toPx()
            var y = step / 2
            while (y < this.size.height) {
                var x = step / 2
                while (x < this.size.width) {
                    drawCircle(tone, r, Offset(x, y), blendMode = BlendMode.Multiply)
                    x += step
                }
                y += step
            }
        }
    }
}

/** Convenience: resolve a speaker name through [WorkIdentity]. */
@Composable
fun Avatar(
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    highlighted: Boolean = false,
) = Avatar(WorkIdentity.character(name), modifier, size, highlighted)

// ---------------------------------------------------------------------------
// Speech bubble
// ---------------------------------------------------------------------------

/** Reaction bubble: pointed top-left corner towards the avatar. */
val BubbleShape: Shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)

/**
 * 吹き出し — a character's line. Solid 1.5px ink outline; [unfinished] draws it dashed (the
 * line trails off / not said out loud — 読空気). Optional Chinese [gloss] under the line.
 */
@Composable
fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    gloss: String? = null,
    unfinished: Boolean = false,
    compact: Boolean = false,
    shape: Shape = if (compact) RoundedCornerShape(10.dp) else BubbleShape,
    background: Color = AjlTheme.colors.surface,
) {
    val colors = AjlTheme.colors
    val ink = colors.ink
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .drawBehind {
                val w = (if (compact) 1.2.dp else AjlStroke.Ink).toPx()
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(
                    outline,
                    ink,
                    style = Stroke(
                        width = w * 2,
                        pathEffect = if (unfinished) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null,
                    ),
                )
            }
            .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = if (compact) 6.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text,
            style = if (compact) AjlTheme.type.jpLabel.copy(fontSize = 12.sp, lineHeight = 17.sp) else AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp),
            color = colors.ink,
        )
        if (gloss != null) Text(gloss, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
    }
}

/** Avatar + bubble, the character's reaction (答对时的角色反应, 読空気の対白). */
@Composable
fun CharacterLine(
    character: CharacterRef?,
    line: String,
    modifier: Modifier = Modifier,
    gloss: String? = null,
    unfinished: Boolean = false,
    avatarSize: Dp = 44.dp,
    bubbleVisible: Boolean = true,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Avatar(character, size = avatarSize, highlighted = true)
        Box(Modifier.weight(1f).graphicsLayer { alpha = if (bubbleVisible) 1f else 0f }) {
            SpeechBubble(line, gloss = gloss, unfinished = unfinished)
        }
    }
}

// ---------------------------------------------------------------------------
// Dialogue box (visual-novel window)
// ---------------------------------------------------------------------------

/**
 * Typewriter progress for a [DialogueBox]. `visibleChars` advances per [TextRules.typewriterDelays];
 * [skip] reveals the whole line (first tap), [isComplete] tells the caller the second tap should
 * advance.
 */
@Stable
class TypewriterState internal constructor(val text: String, private val targetTotalMs: Int?) {
    var visibleChars by mutableIntStateOf(0)
        internal set
    val isComplete: Boolean get() = visibleChars >= text.length
    fun skip() {
        visibleChars = text.length
    }

    internal suspend fun run() {
        val delays = TextRules.typewriterDelaysFor(text, targetTotalMs)
        for (i in text.indices) {
            if (isComplete) return
            delay(delays[i].toLong())
            if (visibleChars <= i) visibleChars = i + 1
        }
    }
}

/**
 * [animate] false (or reduced motion) shows the whole line immediately. [targetTotalMs] aligns
 * typing to source audio: audio length × 0.9.
 */
@Composable
fun rememberTypewriterState(text: String, animate: Boolean = true, targetTotalMs: Int? = null): TypewriterState {
    val reduced = rememberReducedMotion()
    val state = remember(text, targetTotalMs) { TypewriterState(text, targetTotalMs) }
    LaunchedEffect(state, animate, reduced) {
        if (!animate || reduced) state.skip() else state.run()
    }
    return state
}

/**
 * 会話窓 — the visual-novel dialogue box: work-colour name plate (slides in 10dp/120ms), line in
 * serif typed out 35ms/char, a breathing ▼ once complete, 3dp solid ink shadow.
 *
 * Tap handling follows the spec: first tap completes the line, the next tap calls [onAdvance]
 * (pass null when the box is not tappable). Supply [body] to render something other than plain
 * text (e.g. a blank slot inside the line); it receives the currently visible prefix.
 */
@Composable
fun DialogueBox(
    speaker: String?,
    state: TypewriterState,
    modifier: Modifier = Modifier,
    onAdvance: (() -> Unit)? = null,
    body: @Composable ColumnScope.(visible: String) -> Unit = { visible ->
        Text(visible, style = AjlTheme.type.jpBody.copy(fontSize = 21.sp, lineHeight = 34.sp), color = AjlTheme.colors.ink)
    },
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val reduced = rememberReducedMotion()
    val plate = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(speaker) {
        if (!reduced) {
            plate.snapTo(0f)
            plate.animateTo(1f, tween(120, easing = MotionTokens.Ease.Decelerate))
        }
    }
    val transition = rememberInfiniteTransition(label = "dialogue-caret")
    val caretY by if (state.isComplete && !reduced) {
        transition.animateFloat(0f, 3f, infiniteRepeatable(tween(400, easing = MotionTokens.Ease.Standard), RepeatMode.Reverse), label = "caret")
    } else {
        remember { mutableStateOf(0f) }
    }
    val shape = AjlShape.Tile
    Box(modifier.fillMaxWidth().padding(top = 14.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .solidShadow(AjlStroke.SolidShadowLarge, colors.ink, shape)
                .clip(shape)
                .background(colors.surface)
                .border(AjlStroke.Ink, colors.ink, shape)
                .then(
                    if (onAdvance != null) {
                        Modifier.clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                            if (!state.isComplete) state.skip() else onAdvance()
                        }
                    } else {
                        Modifier
                    },
                )
                .semantics(mergeDescendants = true) { contentDescription = listOfNotNull(speaker, state.text).joinToString("：") }
                .padding(start = 16.dp, end = 32.dp, top = 20.dp, bottom = 14.dp),
        ) {
            body(state.text.take(state.visibleChars))
        }
        if (!speaker.isNullOrBlank()) {
            Box(
                Modifier
                    .padding(start = 12.dp)
                    .offset(y = (-14).dp)
                    .graphicsLayer {
                        alpha = plate.value
                        translationX = (1f - plate.value) * -10.dp.toPx()
                    }
                    .height(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(work.accent)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(speaker, style = AjlTheme.type.jpLabel.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold), color = work.onAccent)
            }
        }
        if (state.isComplete) {
            Canvas(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 12.dp)
                    .size(width = 12.dp, height = 10.dp)
                    .graphicsLayer { translationY = caretY * density }
                    .clearAndSetSemantics {},
            ) {
                val p = Path().apply {
                    moveTo(1.dp.toPx(), 1.dp.toPx())
                    lineTo(size.width - 1.dp.toPx(), 1.dp.toPx())
                    lineTo(size.width / 2f, size.height - 1.dp.toPx())
                    close()
                }
                drawPath(p, work.accent)
            }
        }
    }
}

/** Short form: type out [text] spoken by [speaker]. */
@Composable
fun DialogueBox(
    speaker: String?,
    text: String,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    targetTotalMs: Int? = null,
    onAdvance: (() -> Unit)? = null,
) {
    val state = rememberTypewriterState(text, animate, targetTotalMs)
    DialogueBox(speaker, state, modifier, onAdvance)
}

// ---------------------------------------------------------------------------
// Answer option
// ---------------------------------------------------------------------------

enum class OptionState { Default, Selected, Correct, Wrong, Dimmed }

/**
 * Answer option row (≥52dp). Default: 1px hairline, 10dp corners. Selected: 1.5px ink (180ms).
 * Correct/Wrong: ok/bad outline + soft fill + ✓/✕; Wrong also nudges ±6dp twice. Dimmed: the
 * options not chosen after the answer is revealed. Selecting emits [FeedbackEvent.OptionSelect]
 * (CLOCK_TICK); answer haptics/sounds stay with the caller that knows the verdict.
 */
@Composable
fun OptionRow(
    text: String,
    state: OptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    japanese: Boolean = false,
    enabled: Boolean = true,
    leading: String? = null,
) {
    val colors = AjlTheme.colors
    val reduced = rememberReducedMotion()
    val feedback = LocalFeedbackEngine.current
    var wrongCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(state) { if (state == OptionState.Wrong) wrongCount++ }
    val spec = MotionTokens.standard<Color>(MotionTokens.Dur.State, reduced)
    val border by animateColorAsState(
        when (state) {
            OptionState.Default, OptionState.Dimmed -> colors.line
            OptionState.Selected -> colors.ink
            OptionState.Correct -> colors.ok
            OptionState.Wrong -> colors.bad
        },
        spec, label = "option-border",
    )
    val fill by animateColorAsState(
        when (state) {
            OptionState.Correct -> colors.okSoft
            OptionState.Wrong -> colors.badSoft
            else -> colors.surface
        },
        spec, label = "option-fill",
    )
    val width by animateDpAsState(
        if (state == OptionState.Default || state == OptionState.Dimmed) AjlStroke.Hair else AjlStroke.Ink,
        MotionTokens.standard(MotionTokens.Dur.State, reduced), label = "option-width",
    )
    val fg = when (state) {
        OptionState.Correct -> colors.ok
        OptionState.Wrong -> colors.bad
        OptionState.Dimmed -> colors.ink3
        else -> colors.ink
    }
    val icon: ImageVector? = when (state) {
        OptionState.Correct -> Icons.Rounded.Check
        OptionState.Wrong -> Icons.Rounded.Close
        else -> null
    }
    val shape = AjlShape.Button
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shakeOnWrong(wrongCount)
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(fill)
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outline, border, style = Stroke(width.toPx() * 2))
            }
            .clickable(
                remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled && (state == OptionState.Default || state == OptionState.Selected),
                role = Role.RadioButton,
            ) {
                feedback?.emit(FeedbackEvent.OptionSelect)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = if (state == OptionState.Correct) "正确" else "错误", tint = fg, modifier = Modifier.size(22.dp))
        if (leading != null && icon == null) Text(leading, style = AjlTheme.type.meta, color = colors.ink3)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text,
                style = if (japanese) AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp) else AjlTheme.type.body.copy(fontWeight = if (state == OptionState.Correct || state == OptionState.Wrong) FontWeight.Medium else FontWeight.Normal),
                color = fg,
            )
            if (detail != null) Text(detail, style = AjlTheme.type.caption, color = colors.ink3)
        }
    }
}

// ---------------------------------------------------------------------------
// Feedback sheet
// ---------------------------------------------------------------------------

/**
 * Answer feedback panel pinned to the bottom of a session screen: slides up 240ms; the
 * character's avatar grows 0.9→1 and fades in (180ms), its bubble follows 80ms later. Below:
 * verdict + [explanation] and one ink [continueLabel] button. [extra] sits between explanation
 * and button (e.g. "再听一遍" / "加入错题本" outline buttons).
 */
@Composable
fun FeedbackSheet(
    correct: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    character: CharacterRef? = null,
    line: String? = null,
    lineGloss: String? = null,
    verdict: String = if (correct) "正确" else "再想想",
    explanation: String? = null,
    continueLabel: String = "继续",
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = AjlTheme.colors
    val reduced = rememberReducedMotion()
    val enter = remember { Animatable(if (reduced) 1f else 0f) }
    val avatar = remember { Animatable(if (reduced) 1f else 0f) }
    var bubble by remember { mutableStateOf(reduced) }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        enter.animateTo(1f, tween(MotionTokens.Dur.Sheet, easing = MotionTokens.Ease.Decelerate))
    }
    LaunchedEffect(Unit) {
        if (reduced) return@LaunchedEffect
        delay(MotionTokens.Dur.Sheet / 2L)
        avatar.animateTo(1f, tween(180, easing = MotionTokens.Ease.Decelerate))
        delay(80)
        bubble = true
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = (1f - enter.value) * size.height }
            .clip(AjlShape.Sheet)
            .background(colors.surface)
            .drawBehind {
                drawRoundRect(
                    colors.line2,
                    size = Size(size.width, size.height + 16.dp.toPx()),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(AjlStroke.Hair.toPx()),
                )
            }
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp)
            .semantics(mergeDescendants = false) { },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (line != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Avatar(
                    character,
                    size = 44.dp,
                    highlighted = true,
                    modifier = Modifier.graphicsLayer {
                        val s = 0.9f + 0.1f * avatar.value
                        scaleX = s; scaleY = s; alpha = avatar.value
                    },
                )
                Box(Modifier.weight(1f).graphicsLayer { alpha = if (bubble) 1f else 0f }) {
                    SpeechBubble(line, gloss = lineGloss)
                }
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            Text(
                verdict,
                style = AjlTheme.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = if (correct) colors.ok else colors.bad,
            )
            if (explanation != null) {
                Text(" · ", style = AjlTheme.type.body, color = colors.ink3)
                Text(explanation, style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2, modifier = Modifier.weight(1f))
            }
        }
        if (extra != null) extra()
        InkButton(continueLabel, onContinue, height = 48.dp)
    }
}

/** Fills the box with a manga-filtered portrait (dialogue scene stand-in); kanji circle if none. */
@Composable
fun PortraitPanel(character: CharacterRef?, modifier: Modifier = Modifier) {
    val work = AjlTheme.work
    MangaPanel(modifier) {
        Box(Modifier.fillMaxSize().screentone(work.tone(0.22f)))
        Avatar(character, size = 120.dp, highlighted = false, modifier = Modifier.align(Alignment.Center))
    }
}
