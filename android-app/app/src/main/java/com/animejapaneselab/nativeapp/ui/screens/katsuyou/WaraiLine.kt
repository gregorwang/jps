package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.design.VoiceBars
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.katsuyou.KyPassage
import com.animejapaneselab.nativeapp.ui.katsuyou.KySent
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.voicepack.lineCue

/*
 * 第十三巻 笑い: the whole book is one line. Straight = common sense, a step off = ボケ, pulled back = ツッコミ,
 * and the laugh sits where it snaps back (a filled dot in the work colour with 「笑」 on it). Everything here is
 * drawn by hand on a Canvas: no avatars, bubbles or manga effects.
 */

/** The laugh: a filled work-colour dot with 笑 on it. */
internal fun DrawScope.laughDot(center: Offset, radius: Float, measurer: TextMeasurer, fill: Color, on: Color, size: TextUnit = 16.sp) {
    drawCircle(fill, radius, center)
    val t = measurer.measure("笑", TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = size, color = on))
    drawText(t, topLeft = Offset(center.x - t.size.width / 2f, center.y - t.size.height / 2f))
}

/** [text] centred on [center]. */
internal fun DrawScope.centeredText(text: String, center: Offset, measurer: TextMeasurer, style: TextStyle) {
    val t = measurer.measure(text, style)
    drawText(t, topLeft = Offset(center.x - t.size.width / 2f, center.y - t.size.height / 2f))
}

/** The small wave beside a speaker that has a clip from the anime; tap = play / stop (under the 原声 / エミリア / TTS choice). */
@Composable
internal fun SpeakerWave(s: KySent, audio: LessonAudioController, tts: String, tint: Color) {
    if (s.audioUrl.isBlank()) return
    val context = LocalContext.current
    val cue = remember(s.ja, s.audioUrl) { lineCue(context, s.ja, s.audioUrl) }
    Row(
        Modifier.clickableNoRipple({ audio.toggle(cue, tts) }).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        VoiceBars(active = audio.isSounding(cue), color = AjlTheme.work.accent)
        Text("原声", style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = tint)
    }
}

private val TrackOn = 29.dp
private val TrackOff = 66.dp
private val NodeDrop = 22.dp

/**
 * 对话段 as a track: before the pick a straight dashed line with numbered nodes; after it the line bends out to
 * the sentences that stray (they slide right), comes back in a thick work-colour stroke to the one that pulls
 * it back, and that node turns into 笑.
 */
@Composable
internal fun TrackPassage(item: KyPassage, picked: Int, onPick: (Int) -> Unit, audio: LessonAudioController, tts: String) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val revealed = picked >= 0
    val n = item.sents.size
    val tops = remember(item) { mutableStateListOf<Float>().apply { repeat(n) { add(-1f) } } }
    val measurer = rememberTextMeasurer()
    val labelStyle = AjlTheme.type.caption.copy(fontSize = 11.sp, color = colors.ink3)
    val numStyle = AjlTheme.type.meta.copy(fontSize = 11.sp, color = colors.ink)
    Column(
        Modifier.fillMaxWidth().drawBehind {
            if (tops.any { it < 0f }) return@drawBehind
            val xOn = TrackOn.toPx()
            val xOff = TrackOff.toPx()
            val ys = tops.map { it + NodeDrop.toPx() }
            val top = 18.dp.toPx()
            val end = size.height - 4.dp.toPx()
            centeredText("常理", Offset(xOn, 7.dp.toPx()), measurer, labelStyle)
            val ink = Stroke(AjlStroke.Ink.toPx())
            if (!revealed) {
                drawLine(colors.ink, Offset(xOn, top), Offset(xOn, end), AjlStroke.Ink.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())))
                ys.forEachIndexed { i, y ->
                    drawCircle(colors.bg, 12.dp.toPx(), Offset(xOn, y))
                    drawCircle(colors.ink, 12.dp.toPx(), Offset(xOn, y), style = ink)
                    centeredText(circled(i), Offset(xOn, y), measurer, numStyle)
                }
                return@drawBehind
            }
            val path = Path().apply { moveTo(xOn, top) }
            var x = xOn
            var y = top
            val snaps = mutableListOf<Pair<Offset, Offset>>()
            val ghosts = mutableListOf<Pair<Float, Float>>()
            var ghostFrom = -1f
            item.sents.forEachIndexed { i, s ->
                val yi = ys[i]
                if (s.at == "off") {
                    if (x == xOn) {
                        val yd = maxOf(y + 14.dp.toPx(), yi - 44.dp.toPx())
                        path.lineTo(xOn, yd)
                        val h = yi - yd
                        path.cubicTo(xOn, yd + h * 0.6f, xOff, yi - h * 0.5f, xOff, yi)
                        ghostFrom = yd
                    } else path.lineTo(xOff, yi)
                    x = xOff
                } else {
                    if (x == xOff) {
                        val turn = minOf(y + 16.dp.toPx(), yi - 16.dp.toPx()).coerceAtLeast(y)
                        path.lineTo(xOff, turn)
                        if (s.at == "snap") snaps += Offset(xOff, turn) to Offset(xOn, yi)
                        else path.lineTo(xOn, yi)
                        path.moveTo(xOn, yi)
                        ghosts += ghostFrom to yi
                    } else path.lineTo(xOn, yi)
                    x = xOn
                }
                y = yi
            }
            path.lineTo(x, end)
            ghosts.forEach { (a, b) ->
                drawLine(colors.line2, Offset(xOn, a), Offset(xOn, b), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
            }
            drawPath(path, colors.ink, style = Stroke(AjlStroke.Ink.toPx(), join = StrokeJoin.Round))
            snaps.forEach { (a, b) -> drawLine(work.accent, a, b, 3.dp.toPx(), cap = StrokeCap.Round) }
            var after = false
            item.sents.forEachIndexed { i, s ->
                val yi = ys[i]
                when {
                    s.at == "snap" -> { laughDot(Offset(xOn, yi), 15.dp.toPx(), measurer, work.accent, work.onAccent); after = true }
                    s.at == "off" -> {
                        drawCircle(colors.surface, 6.dp.toPx(), Offset(xOff, yi))
                        drawCircle(colors.ink, 6.dp.toPx(), Offset(xOff, yi), style = ink)
                    }
                    after -> drawCircle(colors.ink, 5.dp.toPx(), Offset(xOn, yi))
                    else -> {
                        drawCircle(colors.surface, 6.dp.toPx(), Offset(xOn, yi))
                        drawCircle(colors.ink, 6.dp.toPx(), Offset(xOn, yi), style = ink)
                    }
                }
            }
        },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        item.sents.forEachIndexed { i, s ->
            val off = revealed && s.at == "off"
            val start by animateDpAsState(if (off) 88.dp else 56.dp, tween(420), label = "track-shift")
            val answer = revealed && i == item.answer
            val wrong = revealed && i == picked && i != item.answer
            Column(
                Modifier
                    .padding(start = start)
                    .fillMaxWidth()
                    .onGloballyPositioned { c -> c.positionInParent().y.let { if (tops[i] != it) tops[i] = it } }
                    .background(colors.surface, RoundedCornerShape(4.dp))
                    .border(
                        if (answer || wrong) AjlStroke.Ink else AjlStroke.Hair,
                        when { answer -> colors.ink; wrong -> colors.bad; else -> colors.line2 },
                        RoundedCornerShape(4.dp),
                    )
                    .clickableNoRipple({ if (!revealed) onPick(i) })
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val who = listOf(if (revealed) circled(i) else "", s.who).filter { it.isNotBlank() }.joinToString(" ")
                    Text(who, style = AjlTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 16.sp), color = colors.ink3)
                    SpeakerWave(s, audio, tts, colors.ink3)
                    Spacer(Modifier.weight(1f))
                    if (revealed) RoleTag(s, answer, wrong)
                }
                if (s.ro.isNotBlank()) Text(s.ro, style = AjlTheme.type.meta.copy(fontSize = 10.sp, lineHeight = 14.sp), color = colors.ink3)
                Text(
                    markedText(s.ja, if (revealed) s.mark else "", work.accent),
                    style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 24.sp),
                    color = colors.ink,
                )
                AnimatedVisibility(revealed && s.zh.isNotBlank(), enter = fadeIn()) {
                    Text(s.zh, style = AjlTheme.type.body.copy(fontSize = 12.sp, lineHeight = 17.sp), color = colors.ink2)
                }
            }
        }
    }
}

/** ボケ · 正解 / ツッコミ · 你选的 on the right of a sentence after the pick. */
@Composable
private fun RoleTag(s: KySent, answer: Boolean, wrong: Boolean, onDark: Boolean = false) {
    val colors = AjlTheme.colors
    val base = when {
        onDark -> colors.onInk
        answer -> colors.ink
        s.at == "snap" -> AjlTheme.work.accent
        else -> colors.ink3
    }
    val tail = when { answer -> "正解"; wrong -> "你选的"; else -> "" }
    if (s.role.isBlank() && tail.isBlank()) return
    Text(
        buildAnnotatedString {
            append(s.role)
            if (tail.isNotBlank()) {
                if (s.role.isNotBlank()) append(" · ")
                if (wrong) withStyle(SpanStyle(color = colors.bad)) { append(tail) } else append(tail)
            }
        },
        style = AjlTheme.type.meta.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
        color = base,
        maxLines = 1,
    )
}

/**
 * 緊張と緩和: the sentences as compact rows; after the pick a tension curve sits above them, rising sentence by
 * sentence, cracking where the character breaks, and dropping to the floor where the laugh is.
 */
@Composable
internal fun TensionPassage(item: KyPassage, picked: Int, onPick: (Int) -> Unit, audio: LessonAudioController, tts: String) {
    val colors = AjlTheme.colors
    val revealed = picked >= 0
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedVisibility(revealed, enter = fadeIn(tween(420))) { TensionCurve(item) }
        Column {
            item.sents.forEachIndexed { i, s ->
                val answer = revealed && i == item.answer
                val wrong = revealed && i == picked && i != item.answer
                val fg = if (answer) colors.onInk else colors.ink
                val sub = if (answer) colors.onInk2 else colors.ink3
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (answer) colors.ink else Color.Transparent, RoundedCornerShape(4.dp))
                        .then(if (answer) Modifier else Modifier.drawBehind {
                            drawLine(colors.line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                        })
                        .then(if (wrong) Modifier.border(AjlStroke.Ink, colors.bad, RoundedCornerShape(4.dp)) else Modifier)
                        .clickableNoRipple({ if (!revealed) onPick(i) })
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(circled(i), style = AjlTheme.type.meta.copy(fontSize = 13.sp, lineHeight = 22.sp), color = if (wrong) colors.bad else sub, modifier = Modifier.width(20.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (s.who.isNotBlank()) Text(s.who, style = AjlTheme.type.caption.copy(fontSize = 11.sp, lineHeight = 14.sp), color = sub)
                            SpeakerWave(s, audio, tts, sub)
                        }
                        if (s.ro.isNotBlank()) Text(s.ro, style = AjlTheme.type.meta.copy(fontSize = 10.sp, lineHeight = 14.sp), color = sub)
                        Text(
                            markedText(s.ja, if (revealed) s.mark else "", if (answer) fg else AjlTheme.work.accent),
                            style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 22.sp),
                            color = fg,
                        )
                        if (revealed && s.zh.isNotBlank()) Text(s.zh, style = AjlTheme.type.body.copy(fontSize = 12.sp, lineHeight = 16.sp), color = if (answer) colors.onInk2 else colors.ink2)
                    }
                    if (revealed) Box(Modifier.padding(top = 4.dp)) { RoleTag(s, answer, wrong, onDark = answer) }
                }
            }
        }
    }
}

/** The curve: one point per sentence at its lv (0–3); 崩了 cracks; the biggest drop is drawn thick and ends in 笑. */
@Composable
private fun TensionCurve(item: KyPassage) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val measurer = rememberTextMeasurer()
    val lv = item.sents.map { it.lv.coerceIn(0f, 3f) }
    val drop = (1 until lv.size).maxByOrNull { lv[it - 1] - lv[it] } ?: -1
    val crack = item.sents.indexOfFirst { it.role == "崩了" }
    val label = AjlTheme.type.meta.copy(fontSize = 10.sp, color = colors.ink3)
    val num = AjlTheme.type.meta.copy(fontSize = 11.sp, color = colors.ink3)
    val crackStyle = AjlTheme.type.caption.copy(fontSize = 11.sp, color = colors.ink)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(176.dp)
            .background(colors.surface, RoundedCornerShape(4.dp))
            .border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp)),
    ) {
        val left = 28.dp.toPx()
        val right = size.width - 28.dp.toPx()
        val base = 140.dp.toPx()
        fun px(i: Int) = if (lv.size == 1) left else left + (right - left) * i / (lv.size - 1)
        fun py(i: Int) = 136.dp.toPx() - lv[i] / 3f * 106.dp.toPx()
        drawText(measurer.measure("紧张", label), topLeft = Offset(10.dp.toPx(), 8.dp.toPx()))
        drawLine(colors.line, Offset(10.dp.toPx(), base), Offset(size.width - 10.dp.toPx(), base), 1.dp.toPx())
        if (drop > 0) drawLine(colors.line2, Offset(px(drop - 1), py(drop - 1)), Offset(px(drop - 1), base), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
        val path = Path()
        lv.indices.forEach { i ->
            val x = px(i)
            val y = py(i)
            when {
                i == 0 -> path.moveTo(x, y)
                i == drop -> path.moveTo(x, y)
                i == crack -> {
                    // a jagged crack across the peak
                    val d = 6.dp.toPx()
                    path.lineTo(x - 2 * d, y + 10.dp.toPx())
                    path.lineTo(x - d, y)
                    path.lineTo(x, y + 14.dp.toPx())
                    path.lineTo(x + d, y - 4.dp.toPx())
                    path.lineTo(x + 2 * d, y + 10.dp.toPx())
                }
                else -> path.lineTo(x, y)
            }
        }
        drawPath(path, colors.ink, style = Stroke(1.8.dp.toPx(), join = StrokeJoin.Round))
        if (drop > 0) drawLine(work.accent, Offset(px(drop - 1), py(drop - 1)), Offset(px(drop), py(drop)), 3.dp.toPx(), cap = StrokeCap.Round)
        lv.indices.forEach { i ->
            if (i != drop && i != crack) drawCircle(colors.ink, 3.5.dp.toPx(), Offset(px(i), py(i)))
            centeredText(circled(i), Offset(px(i), 160.dp.toPx()), measurer, if (i == drop) num.copy(color = work.accent) else num)
        }
        if (crack >= 0) centeredText("崩了一下", Offset(px(crack), (py(crack) - 14.dp.toPx()).coerceAtLeast(10.dp.toPx())), measurer, crackStyle)
        if (drop > 0) laughDot(Offset(px(drop), py(drop)), 14.dp.toPx(), measurer, work.accent, work.onAccent, 15.sp)
    }
}
