package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.VoiceBars
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.katsuyou.KyDiff
import com.animejapaneselab.nativeapp.ui.katsuyou.KyDiffTable
import com.animejapaneselab.nativeapp.ui.katsuyou.KyFig
import com.animejapaneselab.nativeapp.ui.katsuyou.KyPick
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.voicepack.lineCue

/*
 * 第十四巻 涙: the tear is where this moment differs from how the person usually talks. Every question is two
 * cards: a grey 平时 negative behind (what they always say, how many times), the line of this moment in front.
 * You tap the block that is different; it lights up, a line runs back to the negative, the counts sit side by side.
 * The small figure under it takes the shape of the point itself (stairs, two floors, an arc over the years).
 */

/** A small wave + 原声 that plays [text] under the 原声 / エミリア / TTS choice; nothing when there is no clip. */
@Composable
internal fun MiniVoice(text: String, audioUrl: String, audio: LessonAudioController, tts: String, tint: Color) {
    if (audioUrl.isBlank()) return
    val context = LocalContext.current
    val cue = remember(text, audioUrl) { lineCue(context, text, audioUrl) }
    Row(
        Modifier.clickableNoRipple({ audio.toggle(cue, tts) }).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        VoiceBars(active = audio.isSounding(cue), color = AjlTheme.work.accent)
        Text("原声", style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = tint)
    }
}

/** The 平时 negative: sunken, dashed, a little turned. */
internal fun Modifier.ghostCard(line: Color, fill: Color): Modifier = this
    .rotate(-1.5f)
    .background(fill, RoundedCornerShape(4.dp))
    .drawBehind {
        drawRoundRect(
            line, cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(AjlStroke.Ink.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
        )
    }

/** [back] on top, [front] laid over its lower edge by [overlap], shifted right by [inset]; the back card leaves [inset] free on the right. */
@Composable
private fun Overlap(overlap: Dp, inset: Dp, back: @Composable () -> Unit, front: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Layout(contents = listOf(back, front), modifier = modifier) { (b, f), c ->
        val ins = inset.roundToPx()
        val w = c.maxWidth
        val bp = b.first().measure(c.copy(minWidth = 0, maxWidth = (w - ins).coerceAtLeast(0), minHeight = 0))
        val fp = f.first().measure(c.copy(minWidth = 0, maxWidth = (w - ins).coerceAtLeast(0), minHeight = 0))
        val y = (bp.height - overlap.roundToPx()).coerceAtLeast(0)
        layout(w, y + fp.height) {
            bp.place(0, 0)
            fp.place(ins, y)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DiffSitting(
    items: List<KyDiff>,
    key: String,
    eyebrow: String,
    title: String,
    settings: LabSettings,
    onClose: () -> Unit,
    onAnswer: (Boolean) -> Unit,
    onDone: (right: Int, asked: Int, missed: List<TsuzukuLine>) -> Unit,
    lastLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val feedback = LocalFeedbackEngine.current
    val audio = rememberLessonAudioController()
    var index by rememberSaveable(key) { mutableIntStateOf(0) }
    var picked by rememberSaveable(key) { mutableIntStateOf(-1) }
    var right by rememberSaveable(key) { mutableIntStateOf(0) }
    val missed = remember(key) { mutableStateListOf<Int>() }
    val item = items.getOrNull(index) ?: return
    val revealed = picked >= 0
    val last = index == items.lastIndex
    var box by remember(key, index) { mutableStateOf<LayoutCoordinates?>(null) }
    var hit by remember(key, index) { mutableStateOf<LayoutCoordinates?>(null) }
    val glow by animateFloatAsState(if (revealed) 1f else 0f, tween(420), label = "diff-glow")

    fun pick(i: Int) {
        if (revealed) return
        val ok = i == item.answer
        feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
        onAnswer(ok)
        if (ok) right++ else missed.add(index)
        picked = i
    }

    KySitting(
        eyebrow = eyebrow,
        title = title,
        counter = "${index + 1} / ${items.size}",
        progress = (index + if (revealed) 1 else 0).toFloat() / items.size,
        onClose = onClose,
        button = if (revealed) (if (last) lastLabel else "下一个") else null,
        onButton = {
            audio.stop()
            if (last) onDone(right, items.size, missed.map { items[it].let { d -> TsuzukuLine(d.ja, true, d.why) } })
            else { index++; picked = -1 }
        },
        arrow = !last,
        modifier = modifier,
    ) {
        if (item.context.isNotBlank()) ContextStrip(item.context)
        Overlap(
            overlap = 18.dp,
            inset = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .onGloballyPositioned { box = it }
                .drawWithContent {
                    drawContent()
                    // after the pick: a dashed line from the negative down to the block that changed
                    val b = box
                    val h = hit
                    if (glow > 0f && b != null && h != null && h.isAttached && b.isAttached) {
                        val r: Rect = b.localBoundingBoxOf(h)
                        val start = Offset(36.dp.toPx(), 40.dp.toPx())
                        val end = Offset(r.center.x, r.top)
                        val p = Path().apply {
                            moveTo(start.x, start.y)
                            cubicTo(start.x, end.y - 10.dp.toPx(), end.x - 40.dp.toPx(), start.y + 30.dp.toPx(), end.x, end.y)
                        }
                        drawPath(p, work.accent.copy(alpha = glow), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))))
                        drawCircle(work.accent.copy(alpha = glow), 3.5.dp.toPx(), start)
                    }
                },
            back = {
                Column(
                    Modifier.fillMaxWidth().ghostCard(colors.line2, colors.sunken).padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.ghostLabel.ifBlank { "平时" }, style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp), color = colors.ink3, modifier = Modifier.weight(1f))
                        if (item.ghostCount.isNotBlank()) Text(item.ghostCount, style = AjlTheme.type.meta.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium), color = colors.faint)
                    }
                    item.ghostLines.forEachIndexed { i, l ->
                        Text(
                            markedGhost(l, if (revealed) item.ghostMark else "", work.accent),
                            style = AjlTheme.type.jpBody.copy(fontSize = if (i == 0) 16.sp else 14.sp, lineHeight = if (i == 0) 24.sp else 21.sp, fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Medium),
                            color = if (i == 0) colors.ink2 else colors.ink3,
                        )
                    }
                }
            },
            front = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(4.dp))
                        .border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp))
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            listOf(item.nowLabel.ifBlank { "这一刻" }, item.who).filter { it.isNotBlank() }.joinToString(" · "),
                            style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp),
                            color = work.accent,
                        )
                        MiniVoice(item.ja, item.audioUrl, audio, settings.ttsWorkerUrl, colors.ink3)
                        Box(Modifier.weight(1f))
                        if (revealed && item.nowCount.isNotBlank()) Text(item.nowCount, style = AjlTheme.type.meta.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium), color = work.accent)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.blocks.forEachIndexed { i, (t, ro) ->
                            val ans = revealed && i == item.answer
                            val wrong = revealed && i == picked && i != item.answer
                            val also = revealed && i in item.also
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Column(
                                    Modifier
                                        .heightIn(min = 48.dp)
                                        .then(if (ans) Modifier.onGloballyPositioned { hit = it } else Modifier)
                                        .background(if (ans) work.accent else colors.surface, RoundedCornerShape(4.dp))
                                        .border(
                                            if (revealed && !ans && !wrong) AjlStroke.Hair else AjlStroke.Ink,
                                            when { ans -> work.accent; wrong -> colors.bad; revealed -> colors.line2; else -> colors.ink },
                                            RoundedCornerShape(4.dp),
                                        )
                                        .clickableNoRipple({ pick(i) })
                                        .semantics { role = Role.Button }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    if (ro.isNotBlank()) Text(ro, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = if (ans) work.onAccent.copy(alpha = 0.8f) else colors.ink3, maxLines = 1)
                                    Text(
                                        t,
                                        style = AjlTheme.type.jpBody.copy(fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
                                        color = when { ans -> work.onAccent; wrong -> colors.bad; revealed && !also -> colors.ink2; else -> colors.ink },
                                    )
                                }
                                if (also) Text(item.also.getValue(i), style = AjlTheme.type.meta.copy(fontSize = 9.sp), color = colors.info, maxLines = 1)
                                else if (wrong) Text("你选的", style = AjlTheme.type.meta.copy(fontSize = 9.sp), color = colors.bad)
                            }
                        }
                    }
                    if (item.zh.isNotBlank()) Text(item.zh, style = AjlTheme.type.caption.copy(fontSize = 13.sp, lineHeight = 19.sp), color = colors.ink2)
                }
            },
        )
        if (!revealed && item.ask.isNotBlank()) {
            Text(item.ask, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        AnimatedVisibility(revealed, enter = fadeIn(tween(360)) + expandVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item.fig?.let { FigPanel(it) }
                val ok = picked == item.answer
                Verdict(if (ok) "✓ ${item.blocks[item.answer].first}" else "你选 ${item.blocks.getOrNull(picked)?.first.orEmpty()} · 正解 ${item.blocks[item.answer].first}", ok)
                if (item.why.isNotBlank()) Text(item.why, style = AjlTheme.type.body, color = colors.ink)
                if (item.rule.isNotBlank()) Text(item.rule, style = AjlTheme.type.body, color = colors.ink2)
            }
        }
    }
}

/** [text] with the [mark] words (｜-separated) outlined in [color]: the word in the negative that changed. */
@Composable
private fun markedGhost(text: String, mark: String, color: Color) = buildAnnotatedString {
    val words = mark.split('|').filter { it.isNotBlank() }
    var i = 0
    while (i < text.length) {
        val w = words.firstOrNull { text.startsWith(it, i) }
        if (w != null) {
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold, background = color.copy(alpha = 0.10f))) { append(w) }
            i += w.length
        } else {
            append(text[i]); i++
        }
    }
}

/** The small figure: a hairline card with a mono title over a drawing in the shape of the point. */
@Composable
internal fun FigPanel(fig: KyFig) {
    val colors = AjlTheme.colors
    Column(
        Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(12.dp)).border(AjlStroke.Hair, colors.line, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (fig.title.isNotBlank()) Text(fig.title, style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp), color = colors.ink3)
        when (fig.kind) {
            "stairs" -> StairsFig(fig)
            "floors" -> FloorsFig(fig)
            "arc" -> ArcFig(fig)
        }
    }
}

/** 称呼的台阶: each step a little higher; the jump is one curve from [KyFig.from] over the steps between to [KyFig.to]. */
@Composable
private fun StairsFig(fig: KyFig) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val measurer = rememberTextMeasurer()
    val n = fig.steps.size.coerceAtLeast(2)
    Canvas(Modifier.fillMaxWidth().height(112.dp)) {
        val sw = size.width / n
        val rise = 18.dp.toPx()
        val base = size.height - 8.dp.toPx()
        fun y(i: Int) = base - i * rise
        val p = Path().apply {
            moveTo(0f, y(0))
            for (i in 0 until n) { lineTo((i + 1) * sw, y(i)); if (i < n - 1) lineTo((i + 1) * sw, y(i + 1)) }
        }
        drawPath(p, colors.line2, style = Stroke(1.2.dp.toPx()))
        fig.steps.forEachIndexed { i, s ->
            val c = when (i) { fig.from -> colors.ink; fig.to -> work.accent; else -> colors.faint }
            val t = measurer.measure(s, TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = c))
            drawText(t, topLeft = Offset(i * sw + (sw - t.size.width) / 2f, y(i) - t.size.height - 3.dp.toPx()))
        }
        val a = Offset(fig.from * sw + sw / 2, y(fig.from) - 20.dp.toPx())
        val b = Offset(fig.to * sw + sw / 2 - 8.dp.toPx(), y(fig.to) - 22.dp.toPx())
        val arc = Path().apply { moveTo(a.x, a.y); cubicTo(a.x + sw, a.y - 60.dp.toPx(), b.x - sw, b.y - 40.dp.toPx(), b.x, b.y) }
        drawPath(arc, work.accent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        val head = Path().apply { moveTo(b.x - 9.dp.toPx(), b.y - 5.dp.toPx()); lineTo(b.x, b.y); lineTo(b.x - 9.dp.toPx(), b.y + 4.dp.toPx()) }
        drawPath(head, work.accent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** 语体 as two floors: [KyFig.top] on the upper one, the fall (accent curve) to the first of [KyFig.bottom], the rest stay below. */
@Composable
private fun FloorsFig(fig: KyFig) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val measurer = rememberTextMeasurer()
    val meta = AjlTheme.type.meta.copy(fontSize = 10.sp, color = colors.ink3)
    val dropStyle = AjlTheme.type.caption.copy(fontSize = 11.sp, color = work.accent)
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val upY = 34.dp.toPx()
        val loY = 96.dp.toPx()
        drawLine(colors.line, Offset(0f, upY), Offset(size.width, upY), 1.dp.toPx())
        drawLine(colors.line, Offset(0f, loY), Offset(size.width, loY), 1.dp.toPx())
        fig.labels.getOrNull(0)?.let { val t = measurer.measure(it, meta); drawText(t, topLeft = Offset(size.width - t.size.width, upY - t.size.height - 2.dp.toPx())) }
        fig.labels.getOrNull(1)?.let { val t = measurer.measure(it, meta); drawText(t, topLeft = Offset(size.width - t.size.width, loY + 3.dp.toPx())) }
        val h = 30.dp.toPx()
        val pad = 8.dp.toPx()
        val r = CornerRadius(4.dp.toPx())
        val topT = measurer.measure(fig.top, TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.ink))
        val topW = topT.size.width + pad * 2
        drawRoundRect(colors.surface, Offset(4.dp.toPx(), upY - h + 12.dp.toPx()), Size(topW, h), r)
        drawRoundRect(colors.ink, Offset(4.dp.toPx(), upY - h + 12.dp.toPx()), Size(topW, h), r, style = Stroke(AjlStroke.Ink.toPx()))
        drawText(topT, topLeft = Offset(4.dp.toPx() + pad, upY - h + 12.dp.toPx() + (h - topT.size.height) / 2))
        var x = 4.dp.toPx() + topW + 46.dp.toPx()
        val by = loY - h + 2.dp.toPx()
        val fall = Path().apply {
            moveTo(4.dp.toPx() + topW, upY - h / 2 + 12.dp.toPx())
            cubicTo(4.dp.toPx() + topW + 26.dp.toPx(), upY - h / 2 + 12.dp.toPx(), x - 20.dp.toPx(), by + h / 2, x, by + h / 2)
        }
        drawPath(fall, work.accent, style = Stroke(2.5.dp.toPx()))
        if (fig.drop.isNotBlank()) {
            val t = measurer.measure(fig.drop, dropStyle)
            drawText(t, topLeft = Offset(4.dp.toPx() + topW + 4.dp.toPx(), upY + 18.dp.toPx()))
        }
        fig.bottom.forEachIndexed { i, s ->
            val first = i == 0
            val t = measurer.measure(s, TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = if (first) 15.sp else 13.sp, color = if (first) work.onAccent else colors.ink2))
            val w = t.size.width + pad * 2
            if (x + w > size.width) return@forEachIndexed
            if (first) drawRoundRect(work.accent, Offset(x, by), Size(w, h), r)
            else {
                drawRoundRect(colors.surface, Offset(x, by), Size(w, h), r)
                drawRoundRect(colors.line2, Offset(x, by), Size(w, h), r, style = Stroke(1.2.dp.toPx()))
            }
            drawText(t, topLeft = Offset(x + pad, by + (h - t.size.height) / 2))
            x += w + 4.dp.toPx()
        }
    }
}

/** A promise over the years: [KyFig.a] on the left, a long arc ([KyFig.mid] at its top) landing on [KyFig.b]; [KyFig.note] along the ground. */
@Composable
private fun ArcFig(fig: KyFig) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val measurer = rememberTextMeasurer()
    val midStyle = AjlTheme.type.caption.copy(fontSize = 11.sp, color = work.accent)
    val noteStyle = AjlTheme.type.caption.copy(fontSize = 11.sp, color = colors.ink3)
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val ground = 100.dp.toPx()
        val x0 = 28.dp.toPx()
        drawLine(colors.line, Offset(0f, ground), Offset(size.width, ground), 1.dp.toPx())
        val bT = measurer.measure(fig.b, TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = work.onAccent))
        val bw = bT.size.width + 12.dp.toPx()
        val x1 = size.width - bw / 2 - 2.dp.toPx()
        drawLine(colors.line2, Offset(x0 + 30.dp.toPx(), ground), Offset(x1 - 30.dp.toPx(), ground), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx())))
        drawCircle(colors.surface, 6.dp.toPx(), Offset(x0, ground))
        drawCircle(colors.ink, 6.dp.toPx(), Offset(x0, ground), style = Stroke(AjlStroke.Ink.toPx()))
        val arc = Path().apply { moveTo(x0, ground - 6.dp.toPx()); cubicTo(x0 + (x1 - x0) * 0.2f, 0f, x1 - (x1 - x0) * 0.2f, 0f, x1, ground - 10.dp.toPx()) }
        drawPath(arc, work.accent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawRoundRect(work.accent, Offset(x1 - bw / 2, ground - 8.dp.toPx()), Size(bw, bT.size.height + 4.dp.toPx()), CornerRadius(3.dp.toPx()))
        drawText(bT, topLeft = Offset(x1 - bw / 2 + 6.dp.toPx(), ground - 6.dp.toPx()))
        val aT = measurer.measure(fig.a, TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = colors.ink))
        drawText(aT, topLeft = Offset((x0 - aT.size.width / 2f).coerceAtLeast(0f), ground + 10.dp.toPx()))
        if (fig.mid.isNotBlank()) centeredText(fig.mid, Offset((x0 + x1) / 2, 34.dp.toPx()), measurer, midStyle)
        if (fig.note.isNotBlank()) centeredText(fig.note, Offset((x0 + x1) / 2, ground - 10.dp.toPx()), measurer, noteStyle)
    }
}

/**
 * 留白 after the pick: the line with an empty dashed slot where it stops, and a few ways to go on. Picking one is
 * not graded and never fills the slot: the anime left it empty.
 */
@Composable
internal fun FillPanel(line: String, fills: List<Pair<String, String>>) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    var chosen by remember(line) { mutableIntStateOf(-1) }
    Column(
        Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(12.dp)).border(AjlStroke.Hair, colors.line, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("那个空槽 · 你会怎么接？（不算分）", style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp), color = colors.ink3)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(line, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = colors.ink, modifier = Modifier.weight(1f, fill = false))
            Box(
                Modifier.size(72.dp, 26.dp).drawBehind {
                    drawRoundRect(work.accent, cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(AjlStroke.Ink.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
                },
            )
        }
        fills.forEachIndexed { i, (ja, zh) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .border(if (i == chosen) AjlStroke.Ink else AjlStroke.Hair, if (i == chosen) work.accent else colors.line2, RoundedCornerShape(12.dp))
                    .clickableNoRipple({ chosen = i })
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(ja, style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, modifier = Modifier.weight(1f))
                Text(zh, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
            }
        }
        if (chosen >= 0) Text("你选的这句不会填进槽里：原作没说完，槽就一直空着。每个人往里放的不一样，这就是它重的原因。", style = AjlTheme.type.caption.copy(fontSize = 13.sp, lineHeight = 20.sp), color = colors.ink2)
    }
}

/**
 * 道别 as line ends: each option a farewell; after the pick each row draws where its line goes: on (dashed, they meet
 * again), cut (said again-soon, never did), sealed (the end). A [KyPick] option marked "base" is the 平时 negative.
 */
@Composable
internal fun EndsOptions(item: KyPick, picked: Int, onPick: (Int) -> Unit) {
    val colors = AjlTheme.colors
    val revealed = picked >= 0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item.options.forEachIndexed { i, o ->
            val right = revealed && i == item.answer
            val wrong = revealed && i == picked && !right
            val base = o.mark == "base"
            val fg = if (right) colors.onInk else colors.ink
            val sub = if (right) colors.onInk2 else colors.ink3
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(if (base) Modifier.ghostCard(colors.line2, colors.sunken) else Modifier)
                    .background(if (right) colors.ink else if (base) Color.Transparent else colors.surface, RoundedCornerShape(4.dp))
                    .then(if (base) Modifier else Modifier.border(if (right || wrong) AjlStroke.Ink else AjlStroke.Hair, when { right -> colors.ink; wrong -> colors.bad; else -> colors.line2 }, RoundedCornerShape(4.dp)))
                    .clickableNoRipple({ if (!revealed) onPick(i) })
                    .semantics { role = Role.Button }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row {
                    Text(o.group, style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp), color = sub, modifier = Modifier.weight(1f))
                    Text("ABCD".getOrNull(i)?.toString().orEmpty() + if (right) " ✓" else if (wrong) " · 你选的" else "", style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = if (wrong) colors.bad else sub)
                }
                if (o.romaji.isNotBlank()) Text(o.romaji, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = sub)
                Text(o.text, style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), color = if (base && !right) colors.ink2 else fg)
                if (revealed) {
                    LineEnd(o.glyph, if (right) colors.onInk else colors.ink, if (right) colors.onInk else colors.bad)
                    if (o.why.isNotBlank()) Text(o.why, style = AjlTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 18.sp), color = if (right) colors.onInk else colors.ink2)
                }
            }
        }
    }
}

/** go: the line runs on, dashed (明天照样见); cut: dashed, then a cross (被剪断); seal: a bar closes it (封口). */
@Composable
private fun LineEnd(kind: String, ink: Color, cross: Color) {
    val measurer = rememberTextMeasurer()
    val label = when (kind) { "go" -> "还会见"; "cut" -> "被剪断"; "seal" -> "封口"; else -> "" }
    val labelColor = if (kind == "cut") cross else ink
    val style = AjlTheme.type.caption.copy(fontSize = 10.sp, color = labelColor)
    Canvas(Modifier.fillMaxWidth().height(22.dp)) {
        val y = size.height / 2
        val w = size.width
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        when (kind) {
            "go" -> {
                drawLine(ink, Offset(0f, y), Offset(w * 0.6f, y), AjlStroke.Ink.toPx())
                drawLine(ink, Offset(w * 0.6f, y), Offset(w, y), AjlStroke.Ink.toPx(), pathEffect = dash)
            }
            "cut" -> {
                drawLine(ink, Offset(0f, y), Offset(w * 0.45f, y), AjlStroke.Ink.toPx())
                drawLine(ink, Offset(w * 0.45f, y), Offset(w * 0.7f, y), AjlStroke.Ink.toPx(), pathEffect = dash)
                val c = Offset(w * 0.72f, y); val d = 5.dp.toPx()
                drawLine(cross, Offset(c.x - d, c.y - d), Offset(c.x + d, c.y + d), 1.6.dp.toPx())
                drawLine(cross, Offset(c.x + d, c.y - d), Offset(c.x - d, c.y + d), 1.6.dp.toPx())
            }
            "seal" -> {
                drawLine(ink, Offset(0f, y), Offset(w * 0.6f, y), AjlStroke.Ink.toPx())
                drawLine(ink, Offset(w * 0.6f, 2.dp.toPx()), Offset(w * 0.6f, size.height - 2.dp.toPx()), 3.dp.toPx())
            }
        }
        if (label.isNotBlank()) {
            val t = measurer.measure(label, style)
            val x = when (kind) { "go" -> w * 0.62f; "cut" -> w * 0.72f + 10.dp.toPx(); else -> w * 0.6f + 8.dp.toPx() }
            drawText(t, topLeft = Offset(x, (size.height - t.size.height) / 2 - if (kind == "go") 8.dp.toPx() else 0f))
        }
    }
}

/** まとめ: the scene line by line, a square per axis; the ones the line turns over are filled. */
@Composable
internal fun DiffTablePanel(t: KyDiffTable) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column(
        Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(4.dp)).border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(t.title, style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp), color = colors.ink3, modifier = Modifier.weight(1f))
            t.cols.forEach { c ->
                Box(Modifier.width(30.dp), contentAlignment = Alignment.BottomCenter) {
                    Text(c.toList().joinToString("\n"), style = AjlTheme.type.caption.copy(fontSize = 11.sp, lineHeight = 13.sp), color = colors.ink3)
                }
            }
        }
        t.rows.forEach { (ja, zh, flags) ->
            Row(
                Modifier.fillMaxWidth().drawBehind { drawLine(colors.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(ja, style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                    if (zh.isNotBlank()) Text(zh, style = AjlTheme.type.caption.copy(fontSize = 11.sp, lineHeight = 15.sp), color = colors.ink3)
                }
                flags.forEach { on ->
                    Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(22.dp).then(
                                if (on) Modifier.background(work.accent, RoundedCornerShape(4.dp))
                                else Modifier.drawBehind {
                                    drawRoundRect(colors.line2, cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))))
                                },
                            ),
                        )
                    }
                }
            }
        }
        if (t.note.isNotBlank()) Text(t.note, style = AjlTheme.type.body.copy(fontSize = 14.sp, lineHeight = 22.sp), color = colors.ink2, modifier = Modifier.padding(top = 8.dp))
    }
}
