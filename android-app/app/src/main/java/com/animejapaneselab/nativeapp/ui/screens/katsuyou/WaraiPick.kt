package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.katsuyou.KyPick
import com.animejapaneselab.nativeapp.ui.katsuyou.KySplit
import com.animejapaneselab.nativeapp.ui.reading.Kana
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/** 前情: what happened just before the line, on a light-blue strip above the line card; it stays after the pick. */
@Composable
internal fun ContextStrip(text: String) {
    val colors = AjlTheme.colors
    Column(
        Modifier.fillMaxWidth().background(colors.infoSoft, RoundedCornerShape(4.dp)).padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("前情", style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp), color = colors.info)
        Text(
            buildAnnotatedString {
                // 「…」 holding Japanese: serif and blue, like a quoted line
                var rest = text
                while (rest.isNotEmpty()) {
                    val a = rest.indexOf('「')
                    val b = if (a >= 0) rest.indexOf('」', a) else -1
                    if (a < 0 || b < 0) { append(rest); break }
                    append(rest.substring(0, a + 1))
                    val inner = rest.substring(a + 1, b)
                    if (inner.any(Kana::isKana)) withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, color = colors.info)) { append(inner) }
                    else append(inner)
                    append('」')
                    rest = rest.substring(b + 1)
                }
            },
            style = AjlTheme.type.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
            color = colors.ink,
        )
    }
}

/**
 * The four kinds of ボケ as line shapes (64×40): 天然 slides off in a soft arc, とぼけ steps across at a right
 * angle on purpose, 勘違い takes the wrong fork at one word, 暴走 speeds up and runs out of the frame.
 */
internal fun DrawScope.bokeGlyph(kind: String, line: Color, ghost: Color) {
    val s = size.width / 64f
    fun o(x: Float, y: Float) = Offset(x * s, y * s)
    val w = 2.dp.toPx()
    val dash = PathEffect.dashPathEffect(floatArrayOf(3 * s, 3 * s))
    when (kind) {
        "tennen" -> {
            val p = Path().apply { moveTo(4 * s, 34 * s); lineTo(20 * s, 34 * s); cubicTo(34 * s, 34 * s, 44 * s, 26 * s, 58 * s, 8 * s) }
            drawPath(p, line, style = Stroke(w, cap = StrokeCap.Round))
            drawCircle(line, 3.5f * s, o(58f, 8f), style = Stroke(1.5.dp.toPx()))
        }
        "toboke" -> {
            drawLine(ghost, o(26f, 34f), o(60f, 34f), 1.2.dp.toPx(), pathEffect = dash)
            val p = Path().apply { moveTo(4 * s, 34 * s); lineTo(26 * s, 34 * s); lineTo(26 * s, 10 * s); lineTo(60 * s, 10 * s) }
            drawPath(p, line, style = Stroke(w, join = StrokeJoin.Miter))
        }
        "kanchigai" -> {
            drawLine(ghost, o(24f, 34f), o(60f, 34f), 1.2.dp.toPx(), pathEffect = dash)
            drawLine(line, o(4f, 34f), o(24f, 34f), w)
            drawLine(line, o(24f, 34f), o(58f, 8f), w, cap = StrokeCap.Round)
            drawCircle(line, 3.5f * s, o(24f, 34f))
        }
        // 第十四巻 涙: three ways of not finishing
        "iisashi" -> {
            drawLine(line, o(4f, 20f), o(28f, 20f), w)
            drawLine(ghost, o(32f, 20f), o(48f, 20f), 1.5.dp.toPx(), pathEffect = dash)
            drawCircle(line, 6f * s, o(55f, 20f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * s, 2 * s))))
        }
        "zekku" -> {
            val p = Path().apply { moveTo(4 * s, 20 * s); lineTo(30 * s, 20 * s); lineTo(35 * s, 13 * s); lineTo(39 * s, 27 * s); lineTo(44 * s, 16 * s) }
            drawPath(p, line, style = Stroke(w, join = StrokeJoin.Round))
        }
        "enryo" -> {
            val p = Path().apply { moveTo(4 * s, 20 * s); lineTo(30 * s, 20 * s); cubicTo(40 * s, 20 * s, 44 * s, 26 * s, 40 * s, 32 * s) }
            drawPath(p, line, style = Stroke(w, cap = StrokeCap.Round))
        }
        "bousou" -> {
            // the frame it runs out of, then a line that keeps getting steeper and leaves with an arrow
            drawLine(ghost, o(50f, 2f), o(50f, 38f), 1.2.dp.toPx(), pathEffect = dash)
            val p = Path().apply { moveTo(4 * s, 34 * s); lineTo(12 * s, 34 * s); cubicTo(28 * s, 34 * s, 40 * s, 28 * s, 48 * s, 16 * s); lineTo(60 * s, 3 * s) }
            drawPath(p, line, style = Stroke(w, cap = StrokeCap.Round))
            val head = Path().apply { moveTo(53 * s, 4 * s); lineTo(61 * s, 2 * s); lineTo(59 * s, 10 * s) }
            drawPath(head, line, style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Options that carry a glyph: 3 (or 2×2) square tiles, the line shape over the name; after the pick each tile says why. */
@Composable
internal fun GlyphOptions(item: KyPick, picked: Int, onPick: (Int) -> Unit) {
    val colors = AjlTheme.colors
    val revealed = picked >= 0
    val perRow = if (item.options.size <= 3) item.options.size else 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item.options.indices.chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { i ->
                    val o = item.options[i]
                    val right = revealed && i == item.answer
                    val soso = revealed && o.mark == "ok"
                    val dim = revealed && !right
                    val fg = when { right -> colors.onInk; dim -> colors.ink2; else -> colors.ink }
                    val line = when { right -> colors.onInk; dim -> colors.faint; else -> colors.ink }
                    val ghost = if (right) colors.ink2 else colors.line2
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 112.dp)
                                .background(if (right) colors.ink else colors.surface, RoundedCornerShape(4.dp))
                                .border(
                                    if (dim) AjlStroke.Hair else AjlStroke.Ink,
                                    when { revealed && i == picked && !right && !soso -> colors.bad; dim -> colors.line2; else -> colors.ink },
                                    RoundedCornerShape(4.dp),
                                )
                                .clickableNoRipple({ if (!revealed) onPick(i) })
                                .semantics { role = Role.Button }
                                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Canvas(Modifier.size(64.dp, 40.dp)) { bokeGlyph(o.glyph, line, ghost) }
                            Text(
                                o.text + if (right) " ✓" else "",
                                style = AjlTheme.type.jpTitle.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
                                color = fg,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        AnimatedVisibility(revealed, enter = fadeIn(tween(300)) + expandVertically()) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val tag = when {
                                    right -> "正解"
                                    soso -> if (i == picked) "你选的 · 也说得通" else "也说得通"
                                    i == picked -> "你选的 · 不对"
                                    else -> "不对"
                                }
                                Text(tag, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = when { right -> colors.ok; soso -> colors.info; else -> colors.bad })
                                if (o.why.isNotBlank()) Text(
                                    o.why,
                                    style = AjlTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = if (right) FontWeight.SemiBold else FontWeight.Normal),
                                    color = if (right) colors.ink else colors.ink2,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                repeat(perRow - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * ダジャレ after the pick: the sound cut into its beats, then two arcs out of it to the two words it can be;
 * the one the joke takes is in the work colour and gets the 笑 dot.
 */
@Composable
internal fun SplitPanel(split: KySplit) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val measurer = rememberTextMeasurer()
    val morae = Kana.morae(split.sound)
    val kanaStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = colors.ink)
    val wordStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    val gloss = AjlTheme.type.caption.copy(fontSize = 12.sp)
    val meta = AjlTheme.type.meta.copy(fontSize = 10.sp, color = colors.ink3)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(colors.surface, RoundedCornerShape(4.dp))
            .border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp)),
    ) {
        val box = 34.dp.toPx()
        val step = 37.dp.toPx()
        val x0 = 10.dp.toPx()
        val cy = 100.dp.toPx()
        morae.forEachIndexed { i, m ->
            val x = x0 + i * step
            drawRoundRect(colors.surface, Offset(x, cy - box / 2), Size(box, box), CornerRadius(4.dp.toPx()))
            drawRoundRect(colors.ink, Offset(x, cy - box / 2), Size(box, box), CornerRadius(4.dp.toPx()), style = Stroke(AjlStroke.Ink.toPx()))
            centeredText(m, Offset(x + box / 2, cy), measurer, kanaStyle)
        }
        val boxesEnd = x0 + morae.size * step - (step - box)
        val mid = (x0 + boxesEnd) / 2
        if (split.romaji.isNotBlank()) centeredText(split.romaji, Offset(mid, cy - box / 2 - 10.dp.toPx()), measurer, meta)
        centeredText("同一个音", Offset(mid, cy + box / 2 + 14.dp.toPx()), measurer, gloss.copy(color = colors.ink3))
        val arcEnd = boxesEnd + 36.dp.toPx()
        val up = 44.dp.toPx()
        val down = 156.dp.toPx()
        fun arc(toY: Float) = Path().apply {
            moveTo(boxesEnd, cy)
            cubicTo(boxesEnd + 20.dp.toPx(), cy, arcEnd - 20.dp.toPx(), toY, arcEnd, toY)
        }
        val hitA = split.hit == "a"
        drawPath(arc(up), if (hitA) work.accent else colors.ink, style = Stroke(if (hitA) 2.5.dp.toPx() else AjlStroke.Ink.toPx()))
        drawPath(arc(down), if (!hitA) work.accent else colors.ink, style = Stroke(if (!hitA) 2.5.dp.toPx() else AjlStroke.Ink.toPx()))
        val wx = arcEnd + 8.dp.toPx()
        fun word(w: Pair<String, String>, y: Float, hit: Boolean, label: String) {
            val c = if (hit) work.accent else colors.ink
            drawText(measurer.measure(label, meta.copy(color = if (hit) work.accent else colors.ink3)), topLeft = Offset(wx, y - 40.dp.toPx()))
            val t = measurer.measure(w.first, wordStyle.copy(color = c))
            drawText(t, topLeft = Offset(wx, y - t.size.height / 2f - 4.dp.toPx()))
            val g = measurer.measure(w.second, gloss.copy(color = if (hit) work.accent else colors.ink2), constraints = androidx.compose.ui.unit.Constraints(maxWidth = (size.width - wx - 4.dp.toPx()).toInt().coerceAtLeast(1)))
            drawText(g, topLeft = Offset(wx, y + 12.dp.toPx()))
            if (hit) laughDot(Offset(size.width - 22.dp.toPx(), y - 4.dp.toPx()), 14.dp.toPx(), measurer, work.accent, work.onAccent, 15.sp)
        }
        word(split.a, up, hitA, if (hitA) "笑点" else "本来的话")
        word(split.b, down, !hitA, if (!hitA) "笑点" else "本来的话")
    }
}

/**
 * 四层楼: a roof and four floors, 画面 on top down to 文化梗; the arrow on the right says the lower, the more
 * Japanese you need. After the pick the line moves into the floor it belongs to.
 */
@Composable
internal fun FloorOptions(item: KyPick, picked: Int, onPick: (Int) -> Unit) {
    val colors = AjlTheme.colors
    val revealed = picked >= 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Canvas(Modifier.fillMaxWidth().height(22.dp)) {
                    val p = Path().apply { moveTo(4.dp.toPx(), size.height - 1.dp.toPx()); lineTo(size.width / 2, 2.dp.toPx()); lineTo(size.width - 4.dp.toPx(), size.height - 1.dp.toPx()) }
                    drawPath(p, colors.ink, style = Stroke(AjlStroke.Ink.toPx(), join = StrokeJoin.Round))
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.options.forEachIndexed { i, o ->
                        val right = revealed && i == item.answer
                        val soso = revealed && i == picked && o.mark == "ok"
                        val wrong = revealed && i == picked && !right && !soso
                        val edge = when { right -> colors.ink; soso -> colors.info; wrong -> colors.bad; else -> colors.ink }
                        val fg = if (right) colors.onInk else colors.ink
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .heightIn(min = 62.dp)
                                .background(if (right) colors.ink else colors.surface, RoundedCornerShape(4.dp))
                                .border(AjlStroke.Ink, edge, RoundedCornerShape(4.dp))
                                .clickableNoRipple({ if (!revealed) onPick(i) })
                                .semantics { role = Role.Button },
                        ) {
                            Box(
                                Modifier.width(40.dp).fillMaxHeight().drawBehind {
                                    drawLine(if (right) colors.onInk else edge, Offset(size.width, 0f), Offset(size.width, size.height), AjlStroke.Ink.toPx())
                                },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${i + 1}", style = AjlTheme.type.meta.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = if (soso || wrong) edge else fg)
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    buildAnnotatedString {
                                        append(o.text)
                                        if (right) append(" ✓")
                                        val tag = when { soso -> "你选的 · 也说得通"; wrong -> "你选的 · 不对"; else -> "" }
                                        if (tag.isNotBlank()) {
                                            append("  ")
                                            withStyle(SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, color = edge, fontFamily = AjlTheme.type.meta.fontFamily)) { append(tag) }
                                        }
                                    },
                                    style = AjlTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                    color = fg,
                                )
                                if (o.why.isNotBlank()) Text(o.why, style = AjlTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 17.sp), color = if (right) colors.onInk2 else colors.ink3)
                                val line = item.line
                                if (right && line != null) Text(
                                    line.ja,
                                    style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
                                    color = colors.onInk,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
            Canvas(Modifier.padding(top = 30.dp).width(22.dp).height(260.dp)) {
                val x = size.width / 2
                val c = colors.ink3
                drawLine(c, Offset(x, 4.dp.toPx()), Offset(x, size.height - 2.dp.toPx()), 1.3.dp.toPx())
                val p = Path().apply { moveTo(x - 6.dp.toPx(), size.height - 10.dp.toPx()); lineTo(x, size.height - 1.dp.toPx()); lineTo(x + 6.dp.toPx(), size.height - 10.dp.toPx()) }
                drawPath(p, c, style = Stroke(1.3.dp.toPx(), join = StrokeJoin.Round))
            }
        }
        Text("越往下，越要懂日语", style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.ink3, modifier = Modifier.align(Alignment.End))
    }
}
