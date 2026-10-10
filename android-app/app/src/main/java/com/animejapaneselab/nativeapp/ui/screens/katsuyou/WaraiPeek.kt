package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.animejapaneselab.nativeapp.ui.design.NoteText
import com.animejapaneselab.nativeapp.ui.katsuyou.KyGlMora
import com.animejapaneselab.nativeapp.ui.katsuyou.KyGlNode
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * 課前の一眼 (第十三巻), the line version: common sense along the bottom, the ボケ stepping off, a 乗る that strays
 * along (dashed), the turn on its word (って) and the drop back to 笑; a numbered legend of the lines underneath.
 */
@Composable
internal fun TrackHero(nodes: List<KyGlNode>) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val measurer = rememberTextMeasurer()
    val num = AjlTheme.type.meta.copy(fontSize = 10.sp, color = colors.ink)
    val label = AjlTheme.type.caption.copy(fontSize = 11.sp, color = colors.ink3)
    val pivotStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 15.sp, color = work.accent)
    val snapAt = nodes.indexOfFirst { it.at == "snap" }.takeIf { it >= 0 } ?: nodes.lastIndex
    val before = nodes.take(snapAt)
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(4.dp))
            .border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp))
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val w = size.width
            val base = 140.dp.toPx()
            fun yOf(at: String) = when (at) { "off" -> 94.dp.toPx(); "ride" -> 50.dp.toPx(); else -> base }
            val k = before.size.coerceAtLeast(1)
            val xs = before.indices.map { w * (0.14f + 0.54f * (it + 1) / k) }
            val sx = w * 0.84f
            drawText(measurer.measure("常理", label), topLeft = Offset(0f, base + 8.dp.toPx()))
            val ink = Stroke(AjlStroke.Ink.toPx(), join = StrokeJoin.Round)
            val dashed = Stroke(AjlStroke.Ink.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
            var px = 2.dp.toPx()
            var py = base
            var leftAt = -1f
            val solid = Path().apply { moveTo(px, py) }
            before.forEachIndexed { i, n ->
                val x = xs[i]
                val y = yOf(n.at)
                if (n.at == "on") {
                    solid.lineTo(x, y)
                } else if (py == base) {
                    val d = x - px
                    solid.lineTo(px + d * 0.3f, base)
                    leftAt = px + d * 0.3f
                    solid.cubicTo(px + d * 0.6f, base, px + d * 0.7f, y + 6.dp.toPx(), x, y)
                } else {
                    val seg = Path().apply { moveTo(px, py); cubicTo(px + (x - px) * 0.4f, py - 4.dp.toPx(), x - (x - px) * 0.4f, y + 10.dp.toPx(), x, y) }
                    drawPath(seg, colors.ink, style = if (n.at == "ride") dashed else ink)
                }
                px = x
                py = y
            }
            if (leftAt >= 0f) drawLine(colors.line2, Offset(leftAt, base), Offset(sx, base), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
            drawPath(solid, colors.ink, style = ink)
            // back to common sense: a curl on the turning word when there is one, then the thick drop
            val pivot = nodes.getOrNull(snapAt)?.pivot.orEmpty()
            var fx = px
            var fy = py
            if (pivot.isNotBlank() && py != base) {
                val curl = Path().apply { moveTo(px, py); cubicTo(px + 16.dp.toPx(), py - 14.dp.toPx(), px + 34.dp.toPx(), py - 4.dp.toPx(), px + 26.dp.toPx(), py + 8.dp.toPx()) }
                drawPath(curl, work.accent, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
                fx = px + 26.dp.toPx()
                fy = py + 8.dp.toPx()
                val t = measurer.measure(pivot.trimStart('…'), pivotStyle)
                drawText(t, topLeft = Offset((px + 36.dp.toPx()).coerceAtMost(w - t.size.width), (py - 30.dp.toPx()).coerceAtLeast(0f)))
            }
            if (fy != base) drawLine(work.accent, Offset(fx, fy), Offset(sx, base), 3.dp.toPx(), cap = StrokeCap.Round)
            else drawLine(colors.ink, Offset(fx, fy), Offset(sx, base), AjlStroke.Ink.toPx())
            drawLine(colors.ink, Offset(sx, base), Offset(w - 2.dp.toPx(), base), AjlStroke.Ink.toPx())
            before.forEachIndexed { i, n ->
                val c = Offset(xs[i], yOf(n.at))
                drawCircle(colors.surface, 10.dp.toPx(), c)
                drawCircle(colors.ink, 10.dp.toPx(), c, style = Stroke(AjlStroke.Ink.toPx()))
                centeredText("${i + 1}", c, measurer, num)
            }
            laughDot(Offset(sx, base), 15.dp.toPx(), measurer, work.accent, work.onAccent)
        }
        nodes.forEachIndexed { i, n ->
            val snap = i == snapAt
            Row(
                Modifier.fillMaxWidth().drawBehind { drawLine(colors.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(if (snap) "笑" else "${i + 1}", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = if (snap) work.accent else colors.ink3, modifier = Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(n.romaji, style = AjlTheme.type.meta.copy(fontSize = 10.sp, lineHeight = 13.sp), color = colors.ink3)
                    Text(
                        buildAnnotatedString {
                            val p = n.pivot.takeIf { snap && it.isNotBlank() && it in n.ja }
                            if (p == null) append(n.ja) else {
                                val at = n.ja.indexOf(p)
                                append(n.ja.substring(0, at))
                                withStyle(SpanStyle(color = work.accent)) { append(p) }
                                append(n.ja.substring(at + p.length))
                            }
                        },
                        style = AjlTheme.type.jpBody.copy(fontSize = if (snap) 17.sp else 15.sp, lineHeight = 24.sp, fontWeight = if (snap) FontWeight.Bold else FontWeight.Medium),
                        color = colors.ink,
                    )
                }
                Text(n.tag, style = AjlTheme.type.meta.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = if (snap) work.accent else colors.ink3)
            }
        }
    }
}

/**
 * 課前の一眼, beat by beat: the shorter word's beats sit over the ones they match in the longer word and are
 * joined to them; an extra beat in the middle is a dashed green cell with its note, extra beats at the ends are grey.
 */
@Composable
internal fun MoraHero(m: KyGlMora) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val measurer = rememberTextMeasurer()
    val ro = AjlTheme.type.meta.copy(fontSize = 10.sp, color = colors.ink3)
    val word = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.ink)
    val note = AjlTheme.type.caption.copy(fontSize = 12.sp, color = colors.ok)
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(4.dp))
            .border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().height(184.dp)) {
            val left = 70.dp.toPx()
            val step = minOf(50.dp.toPx(), (size.width - left) / m.b.size.coerceAtLeast(1))
            val box = step - 6.dp.toPx()
            val topY = 12.dp.toPx()
            val botY = 112.dp.toPx()
            val kana = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = (box / density / 2f).coerceAtMost(22f).sp)
            fun cx(col: Int) = left + col * step + box / 2
            val r = CornerRadius(4.dp.toPx())
            drawText(measurer.measure(m.aRomaji, ro), topLeft = Offset(0f, topY + 2.dp.toPx()))
            drawText(measurer.measure(m.aWord, word), topLeft = Offset(0f, topY + 16.dp.toPx()))
            drawText(measurer.measure(m.bRomaji, ro), topLeft = Offset(0f, botY + 2.dp.toPx()))
            drawText(measurer.measure(m.bWord, word), topLeft = Offset(0f, botY + 16.dp.toPx()))
            m.bKinds.forEachIndexed { k, kind ->
                if (kind == "extra") drawLine(colors.line2, Offset(cx(k), topY + 18.dp.toPx()), Offset(cx(k), botY), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
            }
            m.aCols.forEach { c -> drawLine(work.accent, Offset(cx(c), topY + box), Offset(cx(c), botY), 2.dp.toPx()) }
            m.a.forEachIndexed { i, k ->
                val x = left + m.aCols[i] * step
                drawRoundRect(colors.surface, Offset(x, topY), Size(box, box), r)
                drawRoundRect(colors.ink, Offset(x, topY), Size(box, box), r, style = Stroke(AjlStroke.Ink.toPx()))
                centeredText(k, Offset(x + box / 2, topY + box / 2), measurer, kana.copy(color = colors.ink))
            }
            m.b.forEachIndexed { k, s ->
                val x = left + k * step
                val kind = m.bKinds[k]
                when (kind) {
                    "same" -> {
                        drawRoundRect(work.tone(0.10f), Offset(x, botY), Size(box, box), r)
                        drawRoundRect(work.accent, Offset(x, botY), Size(box, box), r, style = Stroke(AjlStroke.Ink.toPx()))
                    }
                    "extra" -> {
                        drawRoundRect(colors.surface, Offset(x, botY), Size(box, box), r)
                        drawRoundRect(colors.ok, Offset(x, botY), Size(box, box), r, style = Stroke(AjlStroke.Ink.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
                    }
                    else -> {
                        drawRoundRect(colors.surface, Offset(x, botY), Size(box, box), r)
                        drawRoundRect(colors.line2, Offset(x, botY), Size(box, box), r, style = Stroke(1.dp.toPx()))
                    }
                }
                centeredText(s, Offset(x + box / 2, botY + box / 2), measurer, kana.copy(color = when (kind) { "same" -> work.accent; "extra" -> colors.ok; else -> colors.faint }))
            }
            val extra = m.bKinds.indexOf("extra")
            if (extra >= 0 && m.note.isNotBlank()) centeredText(m.note, Offset(cx(extra), botY + box + 14.dp.toPx()), measurer, note)
        }
        if (m.zh.isNotBlank()) NoteText(m.zh, style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center), color = colors.ink2, modifier = Modifier.fillMaxWidth())
    }
}
