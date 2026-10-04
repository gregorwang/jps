package com.animejapaneselab.nativeapp.ui.screens.review

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.knowledge.KnowKind
import com.animejapaneselab.nativeapp.ui.knowledge.KnowPrev
import com.animejapaneselab.nativeapp.ui.knowledge.KnowQuiz
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeCard
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import org.json.JSONArray
import org.json.JSONObject

/*
 * v2 知識 cards (deck format 2): one drawing per kind of content — a tree, a duel, a flow chart,
 * nested CAUSE boxes, a timeline… The fields live in [KnowledgeCard.data] as the decks wrote them
 * (`archive-content-sources/knowledge-cards/decks/`). In the text, 【x】 marks the target and ［］
 * are drawn brackets.
 */

private val R4 = RoundedCornerShape(4.dp)

/** 【x】 → [tint] + underline; ［ ］ → bold [tint]. */
internal fun marked(s: String, tint: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < s.length) {
        val c = s[i]
        val end = if (c == '【') s.indexOf('】', i + 1) else -1
        when {
            end > 0 -> {
                withStyle(SpanStyle(color = tint, textDecoration = TextDecoration.Underline)) { append(s.substring(i + 1, end)) }
                i = end + 1
            }
            c == '［' || c == '］' -> {
                withStyle(SpanStyle(color = tint, fontWeight = FontWeight.Bold)) { append(c) }
                i++
            }
            else -> {
                append(c)
                i++
            }
        }
    }
}

private fun JSONObject.objs(name: String): List<JSONObject> = optJSONArray(name)?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }.orEmpty()
private fun JSONObject.arrs(name: String): List<JSONArray> = optJSONArray(name)?.let { a -> (0 until a.length()).mapNotNull { a.optJSONArray(it) } }.orEmpty()
private fun JSONObject.strs(name: String): List<String> = optJSONArray(name)?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
private fun JSONArray.s(i: Int): String = optString(i)
private fun String.hasKana(): Boolean = any { it in '぀'..'ヿ' }

@Composable
private fun Ja(text: String, size: Int = 16, weight: FontWeight = FontWeight.Medium, color: Color = AjlTheme.colors.ink, modifier: Modifier = Modifier) {
    Text(
        marked(text, AjlTheme.work.accent),
        style = AjlTheme.type.jpBody.copy(fontSize = size.sp, lineHeight = (size * 1.5f).sp, fontWeight = weight),
        color = color,
        modifier = modifier,
    )
}

@Composable
private fun Zh(text: String, size: Int = 13, color: Color = AjlTheme.colors.ink2, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier) {
    Text(
        marked(text, AjlTheme.work.accent),
        style = AjlTheme.type.body.copy(fontSize = size.sp, lineHeight = (size * 1.5f).sp, fontWeight = weight),
        color = color,
        modifier = modifier,
    )
}

@Composable
private fun Mono(text: String, size: Int = 11, color: Color = AjlTheme.colors.ink3) {
    Text(text, style = AjlTheme.type.meta.copy(fontSize = size.sp, lineHeight = (size * 1.4f).sp), color = color)
}

/** Japanese or Chinese, whichever the text is. */
@Composable
private fun Cell(text: String, size: Int = 13, modifier: Modifier = Modifier) {
    if (text.hasKana()) Ja(text, size + 1, modifier = modifier) else Zh(text, size, AjlTheme.colors.ink, modifier = modifier)
}

@Composable
private fun Chip(text: String, fg: Color, bg: Color, border: Color) {
    Text(
        marked(text, AjlTheme.work.accent),
        style = AjlTheme.type.jpBody.copy(fontSize = 13.sp, lineHeight = 20.sp),
        color = fg,
        modifier = Modifier.clip(R4).background(bg).border(1.dp, border, R4).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
internal fun PrevStrip(prev: KnowPrev) {
    val colors = AjlTheme.colors
    Column(
        Modifier.fillMaxWidth().clip(R4).background(colors.infoSoft).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Mono("前回のあらすじ · ${prev.from}", 10, colors.info)
        Text(marked(prev.text, colors.info), style = AjlTheme.type.caption, color = colors.ink2)
    }
}

@Composable
internal fun KnowKindBody(card: KnowledgeCard, onAnswer: (KnowQuiz, Boolean) -> Unit) {
    val d = card.data
    when (card.kind) {
        KnowKind.Tree -> Tree(d)
        KnowKind.Duel -> Duel(d)
        KnowKind.Bins -> Bins(d)
        KnowKind.Flow -> Flow(d)
        KnowKind.Boxes -> Boxes(d)
        KnowKind.Table -> Table(d)
        KnowKind.Rules -> Rules(d)
        KnowKind.Steps -> Stations(d.arrs("steps").mapIndexed { i, s -> Station("", s.s(0), s.s(1), i == d.arrs("steps").lastIndex, headIsZh = true) })
        KnowKind.Pattern -> Pattern(d)
        KnowKind.Radial -> Radial(d)
        KnowKind.Blocks -> Blocks(d)
        KnowKind.Quiz -> ItemsQuiz(card, onAnswer)
        KnowKind.Quote -> Quote(d)
        KnowKind.Floors -> Floors(d)
        KnowKind.Lanes -> Lanes(d)
        KnowKind.Putback -> Putback(d)
        KnowKind.Uses -> Uses(d)
        KnowKind.Nest -> Nest(d)
        KnowKind.Arcs -> Arcs(d)
        KnowKind.Parse2 -> Parse2(d)
        KnowKind.Reorder -> Reorder(d)
        KnowKind.Segment -> Segment(d)
        KnowKind.Ladder -> Ladder(d)
        KnowKind.Thread -> {
            val stops = d.arrs("stops")
            Stations(stops.mapIndexed { i, s -> Station(s.s(0), s.s(1), s.s(2), i == stops.lastIndex) }, labelWidth = 40)
        }
        else -> Unit
    }
}

// ------------------------------------------------------------------ kinds

@Composable
private fun Tree(d: JSONObject) {
    val colors = AjlTheme.colors
    val lineColor = colors.line2
    val branches = d.objs("branches")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.border(1.5.dp, colors.ink, R4).padding(horizontal = 8.dp, vertical = 12.dp)) {
            Text(d.optString("root"), style = AjlTheme.type.jpTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.Black), color = colors.ink)
        }
        Box(Modifier.width(10.dp).height(1.5.dp).background(lineColor))
        Column(Modifier.weight(1f)) {
            branches.forEachIndexed { i, b ->
                val first = i == 0
                val last = i == branches.lastIndex
                Row(
                    Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val w = 1.5.dp.toPx()
                            val mid = size.height / 2
                            drawLine(lineColor, Offset(0f, mid), Offset(10.dp.toPx(), mid), w)
                            drawLine(lineColor, Offset(0f, if (first) mid else 0f), Offset(0f, if (last) mid else size.height), w)
                        }
                        .padding(start = 14.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(b.optString("label"), style = AjlTheme.type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = colors.ink, modifier = Modifier.width(32.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        b.arrs("leaves").forEach { leaf ->
                            Column {
                                Text(leaf.s(0), style = AjlTheme.type.body.copy(fontSize = 11.sp), color = AjlTheme.work.accent)
                                Ja(leaf.s(1), 14)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Duel(d: JSONObject) {
    val colors = AjlTheme.colors
    val a = d.optJSONObject("a") ?: JSONObject()
    val b = d.optJSONObject("b") ?: JSONObject()
    Column {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(44.dp))
            listOf(a, b).forEach { side ->
                val ja = side.optString("ja")
                val n = ja.count { it != '【' && it != '】' }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Mono(side.optString("romaji"), 10)
                    Ja(ja, if (n <= 4) 24 else if (n <= 6) 19 else 15, FontWeight.Bold)
                    Text(
                        side.optString("sub"),
                        style = AjlTheme.type.body.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = colors.ink2,
                        modifier = Modifier.border(1.dp, colors.line2, R4).padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
        Box(Modifier.padding(top = 10.dp)) { Hairline(strong = true) }
        d.arrs("rows").forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Zh(r.s(0), 11, colors.ink3, modifier = Modifier.width(44.dp))
                Cell(r.s(1), modifier = Modifier.weight(1f))
                Cell(r.s(2), modifier = Modifier.weight(1f))
            }
            Hairline()
        }
    }
}

@Composable
private fun Bins(d: JSONObject) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("left", "right").forEach { key ->
            val side = d.optJSONObject(key) ?: return@forEach
            val tone = side.optString("tone")
            val (fg, bg, bd) = when (tone) {
                "ok" -> Triple(colors.ok, colors.okSoft, colors.ok)
                "work" -> Triple(work.accent, work.tone(0.12f), work.accent)
                else -> Triple(colors.ink, Color.Transparent, colors.line2)
            }
            Column(
                Modifier.fillMaxWidth().border(1.5.dp, if (tone == "ink") colors.line2 else colors.ink, R4).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Zh(side.optString("label"), 12, colors.ink2, FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    side.strs("items").forEach { Chip(it, fg, bg, bd) }
                }
            }
        }
        d.optString("note").takeIf { it.isNotBlank() }?.let { Zh(it) }
        d.arrs("ex").forEach { e -> Example(e.s(0), e.s(1), e.s(2), 16) }
    }
}

@Composable
private fun Example(ja: String, romaji: String, zh: String, size: Int = 17) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        if (romaji.isNotBlank()) Mono(romaji)
        Ja(ja, size)
        if (zh.isNotBlank()) Zh(zh)
    }
}

@Composable
private fun Flow(d: JSONObject) {
    val colors = AjlTheme.colors
    val ink = colors.ink
    Column {
        Zh(
            d.optString("ask"), 15, ink, FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().border(1.5.dp, ink, R4).padding(horizontal = 12.dp, vertical = 10.dp),
        )
        Canvas(Modifier.fillMaxWidth().height(24.dp)) {
            val w = 1.5.dp.toPx()
            val l = size.width * 0.25f
            val r = size.width * 0.75f
            val y = size.height * 0.45f
            drawLine(ink, Offset(size.width / 2, 0f), Offset(size.width / 2, y), w)
            drawLine(ink, Offset(l, y), Offset(r, y), w)
            drawLine(ink, Offset(l, y), Offset(l, size.height), w)
            drawLine(ink, Offset(r, y), Offset(r, size.height), w)
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("yes" to colors.ok, "no" to AjlTheme.work.accent).forEach { (key, tint) ->
                val side = d.optJSONObject(key) ?: JSONObject()
                Column(
                    Modifier.weight(1f).fillMaxHeight().border(1.5.dp, tint, R4).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Zh(side.optString("label"), 12, tint, FontWeight.Bold)
                    side.strs("items").forEach { Ja(it, 14) }
                    Zh(side.optString("note"), 12)
                }
            }
        }
    }
}

@Composable
private fun Boxes(d: JSONObject) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        d.objs("pair").forEachIndexed { i, p ->
            if (i > 0) Hairline()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.width(76.dp)) {
                    Mono(p.optString("romaji"), 10)
                    Ja(p.optString("ja"), 23, FontWeight.Bold)
                    Zh(p.optString("zh"), 12)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NestedBox(p.strs("layers"))
                    Ja(p.optString("ex"), 14)
                }
            }
        }
    }
}

@Composable
private fun NestedBox(layers: List<String>) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    if (layers.size <= 1) {
        Text(layers.firstOrNull().orEmpty(), style = AjlTheme.type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
        return
    }
    val cause = "CAUSE" in layers.first()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(R4)
            .background(if (cause) work.tone(0.12f) else colors.surface)
            .border(1.5.dp, if (cause) work.accent else colors.ink, R4)
            .padding(start = 7.dp, end = 7.dp, top = 3.dp, bottom = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Mono(layers.first(), 11, if (cause) work.accent else colors.ink2)
        NestedBox(layers.drop(1))
    }
}

@Composable
private fun Table(d: JSONObject) {
    val colors = AjlTheme.colors
    val cols = d.strs("cols")
    val weights = when {
        cols.firstOrNull() == "英语" -> List(cols.size) { 1f }
        cols.size == 2 -> listOf(1.3f, 1f)
        cols.size == 3 -> listOf(0.8f, 1f, 1.4f)
        else -> List(cols.size) { 1f }
    }
    Column {
        Row(Modifier.padding(bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cols.forEachIndexed { i, c -> Zh(c, 11, colors.ink3, modifier = Modifier.weight(weights[i])) }
        }
        Hairline(strong = true)
        d.arrs("rows").forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cols.indices.forEach { i ->
                    val cell = r.s(i)
                    val m = Modifier.weight(weights[i])
                    if (cols[i] == "英语") Box(m) { Mono(cell, 11, colors.ink2) } else Cell(cell, modifier = m)
                }
            }
            Hairline()
        }
    }
}

private fun markColor(m: String, ok: Color, bad: Color, ink3: Color) = when (m) {
    "✕" -> bad
    "○" -> ok
    else -> ink3
}

@Composable
private fun Rules(d: JSONObject) {
    val colors = AjlTheme.colors
    Column {
        Hairline(strong = true)
        d.objs("items").forEach { item ->
            val m = item.optString("mark")
            val tint = markColor(m, colors.ok, colors.bad, colors.ink3)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(m, style = AjlTheme.type.body.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold), color = tint, modifier = Modifier.width(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (item.optBoolean("zhline")) {
                        Text(
                            item.optString("ja"),
                            style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 22.sp, textDecoration = if (m == "✕") TextDecoration.LineThrough else null),
                            color = if (m == "✕") colors.bad else colors.ink,
                        )
                    } else {
                        Ja(item.optString("ja"), 16, color = if (m == "✕") colors.bad else colors.ink)
                    }
                    item.optString("fix").takeIf { f -> f.isNotBlank() }?.let { f ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (m == "✕") "→" else "／", style = AjlTheme.type.body, color = colors.ink3)
                            Ja(f, 16, FontWeight.SemiBold, if (m == "✕") colors.ok else colors.ink)
                        }
                    }
                    item.optString("zh").takeIf { z -> z.isNotBlank() }?.let { z -> Zh(z, 12) }
                }
            }
            Hairline()
        }
    }
}

private class Station(val label: String, val head: String, val sub: String, val end: Boolean, val headIsZh: Boolean = false)

@Composable
private fun Stations(items: List<Station>, labelWidth: Int = 0) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column {
        items.forEachIndexed { i, st ->
            val last = i == items.lastIndex
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (labelWidth > 0) Zh(st.label, 11, colors.ink3, modifier = Modifier.width(labelWidth.dp).padding(top = 2.dp))
                Column(Modifier.width(12.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .padding(top = 5.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (st.end) work.accent else colors.surface)
                            .border(2.dp, work.accent, CircleShape),
                    )
                    if (!last) Box(Modifier.weight(1f).width(2.dp).background(work.tone(0.3f)))
                }
                Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    if (st.headIsZh) Zh(st.head, 15, colors.ink, FontWeight.SemiBold) else Ja(st.head, 15, if (st.end) FontWeight.Bold else FontWeight.Medium)
                    if (st.sub.isNotBlank()) Zh(st.sub, 12)
                }
            }
        }
    }
}

@Composable
private fun Pattern(d: JSONObject) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            d.objs("parts").forEachIndexed { i, p ->
                if (i > 0) Text("＋", style = AjlTheme.type.meta.copy(fontSize = 15.sp), color = work.accent, modifier = Modifier.padding(top = 4.dp))
                val tint = if (p.optBoolean("hl")) work.accent else colors.ink
                val w = p.optString("w")
                Column(Modifier.width(IntrinsicSize.Max), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(w, style = AjlTheme.type.jpTitle.copy(fontSize = if (w.length <= 6) 19.sp else 16.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold), color = tint)
                    Box(Modifier.fillMaxWidth().height(2.dp).background(tint))
                    Zh(p.optString("l1"), 11)
                }
            }
        }
        Hairline()
        d.objs("examples").forEach { Example(it.optString("ja"), it.optString("romaji"), it.optString("zh")) }
        d.optJSONObject("limit")?.let { l ->
            val tint = markColor(l.optString("mark"), colors.ok, colors.bad, colors.ink3)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(l.optString("mark"), style = AjlTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = tint, modifier = Modifier.width(16.dp))
                Column {
                    Ja(l.optString("ja"), 15, color = tint)
                    Zh(l.optString("why"), 12)
                }
            }
        }
    }
}

@Composable
private fun Radial(d: JSONObject) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val spokes = d.arrs("spokes")
    @Composable
    fun spoke(r: JSONArray?, modifier: Modifier) {
        Column(modifier.border(1.dp, colors.line2, R4).padding(horizontal = 8.dp, vertical = 6.dp)) {
            Zh(r?.s(0).orEmpty(), 11, work.accent, FontWeight.SemiBold)
            Zh(r?.s(1).orEmpty(), 14, colors.ink)
        }
    }
    val center = d.optJSONObject("center") ?: JSONObject()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            spoke(spokes.getOrNull(0), Modifier.weight(1f))
            spoke(spokes.getOrNull(1), Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.size(64.dp).clip(CircleShape).border(1.5.dp, colors.ink, CircleShape),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Mono(center.optString("romaji"), 9)
                Text(center.optString("ja"), style = AjlTheme.type.jpTitle.copy(fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black), color = colors.ink)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            spoke(spokes.getOrNull(2), Modifier.weight(1f))
            spoke(spokes.getOrNull(3), Modifier.weight(1f))
        }
        d.arrs("reads").forEach { r ->
            Column {
                Ja(r.s(0), 15, FontWeight.Bold)
                Zh(r.s(1))
            }
        }
    }
}

@Composable
private fun Blocks(d: JSONObject) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column {
        Hairline(strong = true)
        d.objs("rows").forEach { r ->
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    r.arrs("blocks").forEachIndexed { i, b ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                b.s(0),
                                style = AjlTheme.type.jpTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                                color = colors.ink,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .widthIn(min = 34.dp)
                                    .clip(R4)
                                    .background(if (i == 0) colors.surface else work.tone(0.12f))
                                    .border(1.5.dp, if (i == 0) colors.ink else work.accent, R4)
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                            )
                            Zh(b.s(1), 10, colors.ink3)
                        }
                    }
                }
                Column {
                    Mono(r.optString("romaji"), 10)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Ja(r.optString("whole"), 19, FontWeight.Bold)
                        Zh(r.optString("zh"))
                    }
                }
            }
            Hairline()
        }
        d.optString("note").takeIf { it.isNotBlank() }?.let { Box(Modifier.padding(top = 10.dp)) { Ja(it, 16) } }
    }
}

@Composable
private fun Quote(d: JSONObject) {
    val colors = AjlTheme.colors
    Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text(
            marked(d.strs("lines").joinToString("\n"), AjlTheme.work.accent),
            style = AjlTheme.type.jpTitle.copy(fontSize = 32.sp, lineHeight = 48.sp, fontWeight = FontWeight.Black),
            color = colors.ink,
        )
        Zh(d.optString("try"), 15)
    }
}

@Composable
private fun Floors(d: JSONObject) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val ink = colors.ink
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(16.dp)) {
            drawPath(
                Path().apply { moveTo(4f, size.height); lineTo(size.width / 2, 1f); lineTo(size.width - 4f, size.height) },
                ink,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        d.objs("floors").forEach { f ->
            val ground = f.optString("n") == "1F"
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(R4)
                    .background(if (ground) work.tone(0.12f) else colors.surface)
                    .border(1.5.dp, ink, R4),
            ) {
                Box(Modifier.width(36.dp).fillMaxHeight().drawBehind { drawLine(ink, Offset(size.width, 0f), Offset(size.width, size.height), 1.5.dp.toPx()) }, contentAlignment = Alignment.Center) {
                    Mono(f.optString("n"), 13, ink)
                }
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Zh(f.optString("name"), 14, ink, FontWeight.Bold)
                    Mono(f.optString("en"), 10)
                    Text(f.optString("tools"), style = AjlTheme.type.jpBody.copy(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold), color = work.accent)
                    Ja(f.optString("ex"), 15)
                }
            }
        }
    }
}

@Composable
private fun Lanes(d: JSONObject) {
    val head = d.optJSONObject("head") ?: JSONObject()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Ja(head.optString("ja"), 36, FontWeight.Black)
            Column {
                Mono(head.optString("romaji"))
                Zh(head.optString("zh"))
            }
        }
        d.objs("lanes").forEach { lane ->
            val stops = lane.arrs("stops")
            Stations(stops.map { s -> Station(s.s(0), s.s(1), s.s(2), s.optBoolean(3)) }, labelWidth = 32)
        }
    }
}

@Composable
private fun Putback(d: JSONObject) {
    val colors = AjlTheme.colors
    Column {
        Hairline(strong = true)
        d.arrs("items").forEach { r ->
            val phrase = r.s(0)
            val back = r.s(1)
            val noun = back.substringBefore('【')
            val shown = if (noun.isNotBlank() && phrase.endsWith(noun)) phrase.dropLast(noun.length) + "【" + noun + "】" else phrase
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(marked(shown, colors.ink), style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, lineHeight = 21.sp), color = colors.ink, modifier = Modifier.weight(1f))
                Text("→", style = AjlTheme.type.caption, color = colors.ink3)
                Ja(back, 14, modifier = Modifier.weight(1f))
                Zh(r.s(2), 11, colors.ink3, modifier = Modifier.width(30.dp))
            }
            Hairline()
        }
    }
}

@Composable
private fun Uses(d: JSONObject) {
    val colors = AjlTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        d.arrs("items").chunked(2).forEach { pair ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { u ->
                    Column(Modifier.weight(1f).fillMaxHeight().border(1.dp, colors.line2, R4).padding(horizontal = 9.dp, vertical = 7.dp)) {
                        Zh(u.s(0), 11, colors.ink3)
                        Ja(u.s(1), 15)
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Nest(d: JSONObject) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Ja(d.optString("ja"), 17)
        Column {
            d.arrs("layers").forEachIndexed { i, l ->
                Row(
                    Modifier.padding(start = (i * 12).dp).padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(l.s(0), style = AjlTheme.type.jpTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = work.accent)
                    Text("←", style = AjlTheme.type.caption, color = colors.ink3)
                    Cell(l.s(1))
                }
            }
        }
    }
}

@Composable
private fun Arcs(d: JSONObject) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val subjects = d.strs("subjects")
    val preds = d.strs("preds")
    val toks = subjects + preds.mapIndexed { i, p -> if (i < preds.lastIndex) p + "と" else p }
    val n = subjects.size
    val centers = remember { mutableStateMapOf<Int, Float>() }
    val ink2 = colors.ink2
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Ja(d.optString("ja"), 16)
        Column {
            Canvas(Modifier.fillMaxWidth().height(66.dp)) {
                val base = size.height - 2f
                for (k in 0 until n) {
                    val xa = centers[n - 1 - k] ?: continue
                    val xb = centers[n + k] ?: continue
                    val top = base - (k + 1) * 20.dp.toPx()
                    drawPath(
                        Path().apply { moveTo(xa, base); cubicTo(xa, top, xb, top, xb, base) },
                        if (k == 0) work.accent else ink2,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                toks.forEachIndexed { i, t ->
                    Text(
                        t,
                        style = AjlTheme.type.jpBody.copy(fontSize = 12.sp, fontWeight = if (i < n) FontWeight.Bold else FontWeight.Medium),
                        color = if (i < n) colors.ink else work.accent,
                        modifier = Modifier.onGloballyPositioned { centers[i] = it.positionInParent().x + it.size.width / 2f },
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Zh("主语  " + subjects.joinToString(" → ") { it.dropLast(1) }, 13, colors.ink)
            Zh("谓语  " + preds.joinToString(" → "), 13, colors.ink)
        }
    }
}

@Composable
private fun Parse2(d: JSONObject) {
    val colors = AjlTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Ja(d.optString("text"), 21, FontWeight.Bold)
        d.arrs("readings").forEachIndexed { i, r ->
            Column(Modifier.fillMaxWidth().border(1.dp, colors.line2, R4).padding(horizontal = 10.dp, vertical = 8.dp)) {
                Mono("切法 ${i + 1}", 10)
                Ja(r.s(0), 17)
                Zh(r.s(1), 13, colors.ink)
            }
        }
        d.optJSONArray("fix")?.let { f ->
            Column(Modifier.fillMaxWidth().clip(R4).background(colors.okSoft).padding(horizontal = 10.dp, vertical = 8.dp)) {
                Ja(f.s(0), 14, FontWeight.SemiBold, colors.ok)
                Zh(f.s(1), 12)
            }
        }
    }
}

@Composable
private fun Reorder(d: JSONObject) {
    val colors = AjlTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Triple("难读：长块夹在中间", d.optString("bad"), false), Triple("好读：长块放前面", d.optString("good"), true)).forEach { (label, s, good) ->
            Column(
                Modifier.fillMaxWidth().border(1.5.dp, if (good) colors.ok else colors.line2, R4).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Zh(label, 12, if (good) colors.ok else colors.ink3, FontWeight.Bold)
                Ja(s, 16)
            }
        }
        Zh(d.optString("zh"))
    }
}

@Composable
private fun Segment(d: JSONObject) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work.accent
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            d.arrs("chunks").forEach { c ->
                Row(Modifier.drawBehind { drawLine(work, Offset(0f, size.height), Offset(size.width, size.height), 2.dp.toPx()) }.padding(horizontal = 2.dp)) {
                    Text(c.s(0), style = AjlTheme.type.jpTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = colors.ink)
                    Text(c.s(1), style = AjlTheme.type.jpTitle.copy(fontSize = 18.sp), color = colors.ink3)
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(R4).background(colors.sunken).padding(horizontal = 10.dp, vertical = 8.dp)) {
            Zh("全假名：自己拆", 11, colors.ink3)
            Ja(d.optString("kana"), 14, FontWeight.Normal, colors.ink2)
        }
        Zh(d.optString("note"), 12, colors.ink3)
    }
}

@Composable
private fun Ladder(d: JSONObject) {
    val colors = AjlTheme.colors
    Column {
        Hairline(strong = true)
        d.arrs("steps").forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(20.dp).padding(top = 4.dp)) { Mono("%02d".format(i + 1), 11, AjlTheme.work.accent) }
                Column {
                    Ja(s.s(0), 15)
                    Zh(s.s(1), 11, colors.ink3)
                }
            }
            Hairline()
        }
    }
}

/** Several short questions on one card, each answered on the spot; each answer counts. */
@Composable
private fun ItemsQuiz(card: KnowledgeCard, onAnswer: (KnowQuiz, Boolean) -> Unit) {
    val colors = AjlTheme.colors
    val d = card.data
    val options = d.strs("options")
    val items = d.arrs("items")
    // "i:j,i:j" — which option each question got.
    var picked by rememberSaveable(card.id) { mutableStateOf("") }
    val chosen = remember(picked) {
        picked.split(',').mapNotNull { p -> p.split(':').takeIf { it.size == 2 }?.let { (a, b) -> a.toIntOrNull()?.let { k -> b.toIntOrNull()?.let { k to it } } } }.toMap()
    }
    Column {
        items.forEachIndexed { i, q ->
            val answer = q.optInt(1)
            val got = chosen[i]
            Column(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Ja(q.s(0), 16)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEachIndexed { j, o ->
                        val on = got == j
                        val isAns = got != null && j == answer
                        val tint = when {
                            on && j == answer -> colors.ok
                            on -> colors.bad
                            isAns -> colors.ok
                            else -> colors.ink
                        }
                        Text(
                            o,
                            style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            color = tint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .widthIn(min = 56.dp)
                                .heightIn(min = 40.dp)
                                .clip(R4)
                                .background(if (on) (if (j == answer) colors.okSoft else colors.badSoft) else Color.Transparent)
                                .border(1.5.dp, if (on || isAns) tint else colors.line2, R4)
                                .clickable(enabled = got == null, role = Role.Button, onClickLabel = o) {
                                    picked = (picked.split(',').filter { it.isNotBlank() } + "$i:$j").joinToString(",")
                                    onAnswer(KnowQuiz("", "", "", options, answer, q.s(2), card.testsId), j == answer)
                                }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                        )
                    }
                    if (got != null) {
                        Text(
                            if (got == answer) "○" else "✕",
                            style = AjlTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            color = if (got == answer) colors.ok else colors.bad,
                        )
                    }
                }
                if (got != null) Zh(q.s(2), 12, colors.ink3)
            }
            Hairline()
        }
    }
}
