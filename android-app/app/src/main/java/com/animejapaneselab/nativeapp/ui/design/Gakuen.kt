package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * 判子 — identity seal (this work / this account). Square with [text] stacked (use `\n`), slightly
 * crooked. At most one per screen. [round] gives the circular 印 used on the student card.
 */
@Composable
fun Seal(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AjlTheme.work.accent,
    size: Dp = 44.dp,
    rotation: Float = -6f,
    round: Boolean = false,
) {
    val shape = if (round) CircleShape else RoundedCornerShape(5.dp)
    val lines = text.split('\n')
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation; alpha = if (round) 0.85f else 1f }
            .border(2.dp, color, shape)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            lines.forEach { line ->
                Text(
                    line,
                    style = AjlTheme.type.jpTitle.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = if (lines.size > 1) (size.value * 0.32f).sp else (size.value * 0.46f).sp,
                        lineHeight = if (lines.size > 1) (size.value * 0.37f).sp else (size.value * 0.5f).sp,
                    ),
                    color = color,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Deterministic resting angle for a stamp in -14°..+8°, so a card never reshuffles. */
fun stampRotationFor(seed: Int): Float {
    val h = (seed * 2654435761L).toInt() ushr 8
    return -14f + (Math.floorMod(h, 23)).toFloat()
}

/**
 * 「済」stamp. With [animateIn] it plays the one celebration in the app: scale 1.8→0.92→1,
 * rotation -24°→-10°→[rotation], alpha 0→1 in 220ms; [onLanded] fires on impact (play the
 * CONFIRM haptic + stamp sound there). Without it the stamp simply sits there — the course
 * switcher must NOT replay it.
 */
@Composable
fun StampMark(
    modifier: Modifier = Modifier,
    text: String = "済",
    size: Dp = 28.dp,
    rotation: Float = -12f,
    color: Color = AjlTheme.colors.stamp,
    animateIn: Boolean = false,
    onLanded: () -> Unit = {},
) {
    val reduced = rememberReducedMotion()
    val t = remember { Animatable(if (animateIn && !reduced) 0f else 1f) }
    LaunchedEffect(animateIn, reduced) {
        if (animateIn) {
            if (reduced) {
                t.snapTo(1f)
                onLanded()
            } else {
                t.snapTo(0f)
                t.animateTo(0.6f, tween((MotionTokens.Dur.Stamp * 0.6f).toInt(), easing = MotionTokens.Ease.Accelerate))
                onLanded()
                t.animateTo(1f, tween((MotionTokens.Dur.Stamp * 0.4f).toInt(), easing = MotionTokens.Ease.Decelerate))
            }
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val p = t.value
                if (p < 0.6f) {
                    val k = p / 0.6f
                    val s = 1.8f + (0.92f - 1.8f) * k
                    scaleX = s; scaleY = s
                    rotationZ = -24f + 14f * k
                    alpha = k
                } else {
                    val k = (p - 0.6f) / 0.4f
                    val s = 0.92f + 0.08f * k
                    scaleX = s; scaleY = s
                    rotationZ = -10f + (rotation + 10f) * k
                    alpha = 0.9f
                }
            }
            .border(AjlStroke.Ink, color, CircleShape)
            .clearAndSetSemantics { contentDescription = "已学完" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AjlTheme.type.jpTitle.copy(fontWeight = FontWeight.Black, fontSize = (size.value * 0.46f).sp, lineHeight = (size.value * 0.5f).sp),
            color = color,
        )
    }
}

/** 「いま」sticky tab in work colour — marks the current episode / textbook. */
@Composable
fun StickyTab(
    modifier: Modifier = Modifier,
    text: String = "いま",
    height: Dp = 18.dp,
) {
    val work = AjlTheme.work
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(work.accent)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AjlTheme.type.jpLabel.copy(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold),
            color = work.onAccent,
            maxLines = 1,
        )
    }
}

/** 在籍中 — the chip on the currently enrolled work in the course switcher. */
@Composable
fun EnrolledChip(modifier: Modifier = Modifier, text: String = "在籍中") {
    StickyTab(modifier = modifier, text = text, height = 24.dp)
}

/**
 * 学生証 — the account card at the top of 設定. [photo] draws inside the 72×88 frame (defaults
 * to an ink screentone placeholder). Rows are (label, value) pairs such as 氏名 / 所属 / 入学 / 出席.
 */
@Composable
fun StudentCard(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    number: String = "No. 0001",
    monoValueLabels: Set<String> = setOf("入学", "出席"),
    photo: (@Composable () -> Unit)? = null,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    MangaPanel(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(work.accent),
        )
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().height(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("学 生 証", style = AjlTheme.type.jpLabel.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = work.onAccent)
                Text(number, style = AjlTheme.type.metaSmall.copy(fontSize = 10.sp), color = work.onAccent)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier
                        .size(width = 72.dp, height = 88.dp)
                        .background(colors.sunken)
                        .border(AjlStroke.Hair, colors.line2),
                ) {
                    if (photo != null) {
                        photo()
                    } else {
                        Box(Modifier.fillMaxSize().screentone(colors.ink.copy(alpha = 0.16f), angleDegrees = 0f, spacing = 6.dp, dotRadius = 1.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { (label, value) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, style = AjlTheme.type.jpLabel.copy(fontSize = 13.sp), color = colors.ink3, modifier = Modifier.width(44.dp))
                            Text(
                                value,
                                style = if (label in monoValueLabels) AjlTheme.type.meta.copy(fontSize = 12.sp) else AjlTheme.type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        Seal(
            text = "印",
            round = true,
            color = colors.stamp,
            size = 42.dp,
            rotation = -10f,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 14.dp),
        )
    }
}

enum class SlotState { Done, Current, Upcoming }

/**
 * 時間割 row: period (一限 / 二限 / 放課後) · title · mono meta. Done rows are faint and struck
 * through with a green 済; the current row's period and meta take the work colour.
 */
@Composable
fun TimetableRow(
    period: String,
    title: String,
    meta: String,
    state: SlotState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val fg = if (state == SlotState.Done) colors.faint else colors.ink
    LineRow(modifier = modifier, onClick = onClick, minHeight = 48.dp) {
        Text(
            period,
            style = AjlTheme.type.jpLabel.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            color = when (state) {
                SlotState.Done -> colors.faint
                SlotState.Current -> work.accent
                SlotState.Upcoming -> colors.ink
            },
            modifier = Modifier.width(44.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = AjlTheme.type.body.copy(textDecoration = if (state == SlotState.Done) TextDecoration.LineThrough else null),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            meta,
            style = if (state == SlotState.Done) AjlTheme.type.jpLabel.copy(fontSize = 12.sp) else AjlTheme.type.meta.copy(fontSize = 12.sp),
            color = when (state) {
                SlotState.Done -> colors.ok
                SlotState.Current -> work.accent
                SlotState.Upcoming -> colors.ink3
            },
        )
    }
}

/**
 * 教科書 cover (212dp tall): spine, rotated screentone band, vertical serif title, a speech
 * bubble with a sample line, Chinese gloss and a progress line. The [current] book gets the
 * work-soft cover, work spine, いま tab and a 3dp solid ink shadow.
 */
@Composable
fun TextbookCover(
    volume: String,
    title: String,
    sampleLine: String,
    gloss: String,
    progress: Float,
    current: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val shape = RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 6.dp, bottomEnd = 6.dp)
    Box(
        modifier = modifier
            .height(212.dp)
            .solidShadow(if (current) AjlStroke.SolidShadowLarge else 0.dp, colors.ink, shape)
            .clip(shape)
            .background(if (current) work.soft else colors.surface)
            .border(AjlStroke.Ink, colors.ink, shape)
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$volume $title $gloss" },
    ) {
        Screentone(
            color = if (current) work.tone(0.34f) else colors.ink.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = (-30).dp)
                .size(width = 130.dp, height = 70.dp)
                .graphicsLayer { rotationZ = -12f },
        )
        // Spine.
        Box(
            Modifier
                .width(12.dp)
                .fillMaxHeight()
                .background(if (current) work.accent else colors.sunken)
                .drawBehind {
                    val w = AjlStroke.Ink.toPx()
                    drawRect(colors.ink, topLeft = Offset(size.width - w, 0f), size = androidx.compose.ui.geometry.Size(w, size.height))
                },
        )
        VerticalText(
            text = title,
            style = AjlTheme.type.jpDisplay.copy(fontSize = 24.sp, lineHeight = 28.sp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
        )
        Text(
            volume,
            style = AjlTheme.type.metaSmall.copy(fontSize = 10.sp),
            color = colors.ink3,
            modifier = Modifier.padding(start = 22.dp, top = 14.dp),
        )
        SpeechBubble(
            text = sampleLine,
            modifier = Modifier
                .padding(start = 22.dp, top = 40.dp)
                .widthIn(max = 88.dp),
            compact = true,
        )
        if (current) {
            StickyTab(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 44.dp))
        }
        Text(
            gloss,
            style = AjlTheme.type.caption.copy(fontSize = 12.sp),
            color = colors.ink2,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, bottom = 26.dp),
        )
        ProgressLine(
            progress = progress,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, end = 12.dp, bottom = 14.dp),
        )
    }
}



enum class AttendanceState { Done, Now, Todo, Empty }

data class AttendanceCell(
    val number: Int,
    val state: AttendanceState,
    val label: String = "",
)

/**
 * 出席カード: a 7-column grid of episodes. 済 stamps on finished ones, the いま strip on the current
 * one (work-soft cell). [stampingEpisode] plays the stamp animation on that one cell only — pass
 * it exclusively at the moment an episode is completed.
 */
@Composable
fun AttendanceCard(
    cells: List<AttendanceCell>,
    modifier: Modifier = Modifier,
    title: String = "出席カード",
    meta: String? = null,
    columns: Int = 7,
    stampingEpisode: Int? = null,
    onStampLanded: () -> Unit = {},
    onSelect: ((Int) -> Unit)? = null,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    MangaPanel(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(title, style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp, letterSpacing = 3.sp), color = colors.ink)
                if (meta != null) Text(meta, style = AjlTheme.type.meta, color = colors.ink3)
            }
            val padded = cells + List((columns - cells.size % columns) % columns) { AttendanceCell(0, AttendanceState.Empty) }
            Column(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val w = AjlStroke.Hair.toPx()
                        drawRect(colors.line2, size = androidx.compose.ui.geometry.Size(size.width, w))
                        drawRect(colors.line2, size = androidx.compose.ui.geometry.Size(w, size.height))
                    },
            ) {
                padded.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { cell ->
                            AttendanceCellView(
                                cell = cell,
                                stamping = stampingEpisode != null && cell.number == stampingEpisode,
                                onStampLanded = onStampLanded,
                                onSelect = onSelect,
                                modifier = Modifier.weight(1f),
                                lineColor = colors.line2,
                                soft = work.soft,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceCellView(
    cell: AttendanceCell,
    stamping: Boolean,
    onStampLanded: () -> Unit,
    onSelect: ((Int) -> Unit)?,
    modifier: Modifier,
    lineColor: Color,
    soft: Color,
) {
    val colors = AjlTheme.colors
    val bg = when (cell.state) {
        AttendanceState.Now -> soft
        AttendanceState.Empty -> colors.bg
        else -> colors.surface
    }
    val clickable = onSelect != null && cell.state != AttendanceState.Empty
    Box(
        modifier = modifier
            .height(48.dp)
            .background(bg)
            .drawBehind {
                val w = AjlStroke.Hair.toPx()
                drawRect(lineColor, topLeft = Offset(size.width - w, 0f), size = androidx.compose.ui.geometry.Size(w, size.height))
                drawRect(lineColor, topLeft = Offset(0f, size.height - w), size = androidx.compose.ui.geometry.Size(size.width, w))
            }
            .then(
                if (clickable) {
                    Modifier.clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button) { onSelect?.invoke(cell.number) }
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = cell.label.ifEmpty {
                    when (cell.state) {
                        AttendanceState.Done -> "第${cell.number}話 已学完"
                        AttendanceState.Now -> "第${cell.number}話 正在学"
                        AttendanceState.Todo -> "第${cell.number}話 未开始"
                        AttendanceState.Empty -> "空"
                    }
                }
            },
    ) {
        if (cell.state != AttendanceState.Empty) {
            Text(
                cell.number.toString(),
                style = AjlTheme.type.metaSmall,
                color = colors.ink3,
                modifier = Modifier.padding(start = 4.dp, top = 3.dp),
            )
        }
        when (cell.state) {
            AttendanceState.Done -> StampMark(
                modifier = Modifier.align(Alignment.Center).offset(y = 2.dp),
                rotation = stampRotationFor(cell.number),
                animateIn = stamping,
                onLanded = onStampLanded,
            )
            AttendanceState.Now -> StickyTab(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 4.dp, end = 4.dp, bottom = 5.dp)
                    .fillMaxWidth(),
            )
            else -> Unit
        }
    }
}
