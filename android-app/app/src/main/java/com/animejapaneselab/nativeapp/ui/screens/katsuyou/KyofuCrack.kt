package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.katsuyou.KyCrack
import com.animejapaneselab.nativeapp.ui.katsuyou.KyDiffTable
import com.animejapaneselab.nativeapp.ui.katsuyou.KyFig
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.voicepack.LineVoicePill
import kotlin.math.abs

/*
 * 第十五巻 恐怖: fear is one zone of an almost normal line that cracks, and the crack is empty. Every question is one
 * card in fixed zones (场所, 说话的, the line in blocks, 回话); you tap the zone that is wrong. It turns to ink, a hairline
 * crack runs from it across the card, and the zone says only what is missing ("名字 · 没给"): no answer is given,
 * which is the point (the opposite of 找错, where the fix floats up).
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CrackSitting(
    items: List<KyCrack>,
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
    val feedback = LocalFeedbackEngine.current
    val audio = rememberLessonAudioController()
    val reduced = rememberReducedMotion()
    var index by rememberSaveable(key) { mutableIntStateOf(0) }
    var picked by rememberSaveable(key) { mutableStateOf("") }
    var right by rememberSaveable(key) { mutableIntStateOf(0) }
    val missed = remember(key) { mutableStateListOf<Int>() }
    val item = items.getOrNull(index) ?: return
    val revealed = picked.isNotEmpty()
    val last = index == items.lastIndex
    var card by remember(key, index) { mutableStateOf<LayoutCoordinates?>(null) }
    var hit by remember(key, index) { mutableStateOf<LayoutCoordinates?>(null) }
    val crack = remember(key, index) { Animatable(0f) }
    LaunchedEffect(revealed, key, index) {
        if (revealed) { if (reduced) crack.snapTo(1f) else crack.animateTo(1f, tween(360)) } else crack.snapTo(0f)
    }

    fun pick(zone: String) {
        if (revealed) return
        val ok = zone == item.at
        feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
        onAnswer(ok)
        if (ok) right++ else missed.add(index)
        picked = zone
    }

    // the zone that takes the void: the socket when there is one, else the cracked zone itself
    val voidAtSocket = item.socket.isNotBlank()
    fun state(zone: String): ZoneState = when {
        !revealed -> ZoneState.Open
        zone == item.at && !voidAtSocket -> ZoneState.Void
        zone == item.at -> ZoneState.Hit
        zone == picked -> ZoneState.Wrong
        zone in item.also -> ZoneState.Also
        else -> ZoneState.Quiet
    }
    fun Modifier.hitAnchor(zone: String) = if (zone == item.at && !voidAtSocket) onGloballyPositioned { hit = it } else this

    KySitting(
        eyebrow = eyebrow,
        title = title,
        counter = "${index + 1} / ${items.size}",
        progress = (index + if (revealed) 1 else 0).toFloat() / items.size,
        onClose = onClose,
        button = if (revealed) (if (last) lastLabel else "下一个") else null,
        onButton = {
            audio.stop()
            if (last) onDone(right, items.size, missed.map { items[it].let { c -> TsuzukuLine(c.ja, true, c.void) } })
            else { index++; picked = "" }
        },
        arrow = !last,
        modifier = modifier,
    ) {
        if (item.context.isNotBlank()) ContextStrip(item.context)
        Column(
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { card = it }
                .background(colors.surface, RoundedCornerShape(4.dp))
                .border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp))
                .drawWithContent {
                    drawContent()
                    val c = card
                    val h = hit
                    if (crack.value > 0f && c != null && h != null && c.isAttached && h.isAttached) {
                        drawCrack(c.localBoundingBoxOf(h), crack.value, item.ja.hashCode(), colors.ink, colors.onInk)
                    }
                }
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (item.place.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("场所")
                    TagZone(item.place, state("place"), item.void, item.also["place"], Modifier.weight(1f, fill = false).hitAnchor("place")) { pick("place") }
                    Box(Modifier.weight(1f))
                    if (revealed) CrackCount(item)
                }
            }
            if (item.who.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("说话的")
                    TagZone(item.who, state("who"), item.void, item.also["who"], Modifier.weight(1f, fill = false).hitAnchor("who")) { pick("who") }
                    if (item.place.isBlank() && revealed) { Box(Modifier.weight(1f)); CrackCount(item) }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item.blocks.forEachIndexed { i, (t, ro) ->
                    if (revealed && voidAtSocket && i == item.socketAt) SocketZone(item.socket, item.void, Modifier.onGloballyPositioned { hit = it })
                    BlockZone(t, ro, state("b$i"), item.void, item.also["b$i"], Modifier.hitAnchor("b$i")) { pick("b$i") }
                }
                if (revealed && voidAtSocket && item.socketAt == item.blocks.size) SocketZone(item.socket, item.void, Modifier.onGloballyPositioned { hit = it })
            }
            if (item.zh.isNotBlank()) Text(item.zh, style = AjlTheme.type.caption.copy(fontSize = 13.sp, lineHeight = 19.sp), color = colors.ink2)
            if (item.reply != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZoneLabel("回话")
                    ReplyZone(item.reply, state("reply"), item.void, Modifier.weight(1f).hitAnchor("reply")) { pick("reply") }
                }
            }
        }
        LineVoicePill(item.ja, item.audioUrl, audio, settings.ttsWorkerUrl, Modifier.align(Alignment.CenterHorizontally))
        if (!revealed && item.ask.isNotBlank()) {
            Text(item.ask, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        AnimatedVisibility(revealed, enter = fadeIn(tween(360)) + expandVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item.fig?.let { FigPanel(it) }
                val ok = picked == item.at
                Verdict(if (ok) "✓ ${item.label(item.at)}" else "你选 ${item.label(picked)} · 裂的是 ${item.label(item.at)}", ok)
                if (item.why.isNotBlank()) Text(item.why, style = AjlTheme.type.body, color = colors.ink)
                if (item.rule.isNotBlank()) Text(item.rule, style = AjlTheme.type.body, color = colors.ink2)
                item.table?.let { CrackTablePanel(it) }
            }
        }
    }
}

internal enum class ZoneState { Open, Void, Hit, Wrong, Also, Quiet }

@Composable
private fun ZoneLabel(text: String) {
    Text(text, style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp), color = AjlTheme.colors.ink3, modifier = Modifier.width(40.dp))
}

/** "正常 ×3 · 裂 ×1": how almost-normal the card was. */
@Composable
private fun CrackCount(item: KyCrack) {
    val cracked = 1 + item.also.size
    Text("正常 ×${item.zones.size - cracked} · 裂 ×$cracked", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = AjlTheme.colors.ink3, maxLines = 1)
}

/** The note under a zone after the pick: the void line in the cracked zone, "你选的" / the also-note elsewhere. */
@Composable
private fun ZoneNote(state: ZoneState, void: String, also: String?) {
    val colors = AjlTheme.colors
    when (state) {
        ZoneState.Void -> Text(void, style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.3.sp), color = colors.onInk2)
        ZoneState.Wrong -> Text("你选的", style = AjlTheme.type.meta.copy(fontSize = 9.sp), color = colors.bad)
        ZoneState.Also -> if (also != null) Text(also, style = AjlTheme.type.meta.copy(fontSize = 9.sp), color = colors.info, maxLines = 1)
        else -> Unit
    }
}

private fun Modifier.zoneFrame(state: ZoneState, open: Color, line2: Color, ink: Color, bad: Color, accent: Color, hairWhenOpen: Boolean): Modifier {
    val shape = RoundedCornerShape(4.dp)
    return when (state) {
        ZoneState.Void -> background(ink, shape).border(AjlStroke.Ink, ink, shape)
        ZoneState.Hit -> background(open, shape).border(AjlStroke.Ink, ink, shape)
        ZoneState.Wrong -> background(open, shape).border(AjlStroke.Ink, bad, shape)
        ZoneState.Also -> background(open, shape).border(AjlStroke.Ink, accent, shape)
        ZoneState.Quiet -> background(open, shape).border(AjlStroke.Hair, line2, shape)
        ZoneState.Open -> background(open, shape).border(if (hairWhenOpen) AjlStroke.Hair else AjlStroke.Ink, if (hairWhenOpen) line2 else ink, shape)
    }
}

/** 场所 / 说话的: a quiet tag; it can crack like any block. */
@Composable
private fun TagZone(text: String, state: ZoneState, void: String, also: String?, modifier: Modifier, onTap: () -> Unit) {
    val colors = AjlTheme.colors
    Column(
        modifier
            .heightIn(min = 44.dp)
            .zoneFrame(state, colors.surface, colors.line2, colors.ink, colors.bad, colors.info, hairWhenOpen = true)
            .clickableNoRipple(onTap)
            .semantics { role = Role.Button }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text,
            style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = if (state == ZoneState.Void) FontWeight.SemiBold else FontWeight.Medium),
            color = when (state) { ZoneState.Void -> colors.onInk; ZoneState.Quiet -> colors.ink3; else -> colors.ink2 },
        )
        ZoneNote(state, void, also)
    }
}

/** One block of the line: romaji over the word. */
@Composable
private fun BlockZone(text: String, romaji: String, state: ZoneState, void: String, also: String?, modifier: Modifier, onTap: () -> Unit) {
    val colors = AjlTheme.colors
    Column(
        modifier
            .heightIn(min = 48.dp)
            .zoneFrame(state, colors.surface, colors.line2, colors.ink, colors.bad, colors.info, hairWhenOpen = false)
            .clickableNoRipple(onTap)
            .semantics { role = Role.Button }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val void_ = state == ZoneState.Void
        if (romaji.isNotBlank()) Text(romaji, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = if (void_) colors.onInk2 else colors.ink3, maxLines = 1)
        Text(
            text,
            style = AjlTheme.type.jpBody.copy(fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
            color = when (state) { ZoneState.Void -> colors.onInk; ZoneState.Quiet -> colors.ink2; else -> colors.ink },
        )
        ZoneNote(state, void, also)
    }
}

/** The slot that has no place in the sentence (「？に」), put in on reveal: ink, dashed inside, holding the void line. */
@Composable
private fun SocketZone(text: String, void: String, modifier: Modifier) {
    val colors = AjlTheme.colors
    Column(
        modifier
            .heightIn(min = 48.dp)
            .background(colors.ink, RoundedCornerShape(4.dp))
            .drawBehind {
                val inset = 3.dp.toPx()
                drawRoundRect(
                    colors.onInk2, topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold), color = colors.onInk)
        Text(void, style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.3.sp), color = colors.onInk2)
    }
}

/** 回话: what came back, or an empty dashed box when nothing did. */
@Composable
private fun ReplyZone(text: String, state: ZoneState, void: String, modifier: Modifier, onTap: () -> Unit) {
    val colors = AjlTheme.colors
    val empty = text.isBlank() && state != ZoneState.Void
    Column(
        modifier
            .heightIn(min = 48.dp)
            .then(
                if (empty && state != ZoneState.Wrong) Modifier.drawBehind {
                    drawRoundRect(
                        if (state == ZoneState.Open) colors.ink3 else colors.line2, cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(AjlStroke.Ink.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                    )
                } else Modifier.zoneFrame(state, colors.surface, colors.line2, colors.ink, colors.bad, colors.info, hairWhenOpen = false),
            )
            .clickableNoRipple(onTap)
            .semantics { role = Role.Button }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (text.isNotBlank()) Text(text, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = if (state == ZoneState.Void) colors.onInk else colors.ink)
        ZoneNote(state, void, null)
    }
}

/**
 * The crack: from the right edge of [zone] a hairline zigzags across the card to its far edge, with two short
 * branches; inside the (ink) zone it is drawn in [onInk]. [t] 0–1 draws it from its start.
 */
private fun DrawScope.drawCrack(zone: Rect, t: Float, seed: Int, ink: Color, onInk: Color) {
    val step = 22.dp.toPx()
    val down = zone.center.y < size.height / 2
    var x = zone.left + zone.width * 0.25f
    var y = zone.center.y
    val pts = mutableListOf(Offset(x, y))
    var k = 0
    var s = abs(seed)
    fun rnd(): Float { s = (s * 1103515245 + 12345) and 0x7fffffff; return (s % 1000) / 1000f }
    while (x < size.width && k < 40) {
        x += step * (0.6f + rnd() * 0.6f)
        y += (if (k % 2 == 0) -1 else 1) * 5.dp.toPx() * rnd() + (if (down) 1 else -1) * 7.dp.toPx() * rnd()
        y = y.coerceIn(2f, size.height - 2f)
        pts += Offset(x.coerceAtMost(size.width), y)
        k++
    }
    val n = ((pts.size - 1) * t).toInt().coerceAtLeast(1)
    val main = Path().apply { moveTo(pts[0].x, pts[0].y); for (i in 1..n) lineTo(pts[i].x, pts[i].y) }
    val stroke = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(main, ink, style = stroke)
    clipRect(zone.left, zone.top, zone.right, zone.bottom) { drawPath(main, onInk, style = stroke) }
    // branches off the 3rd and 6th points
    listOf(3, 6).filter { it < n }.forEach { i ->
        val p = pts[i]
        val dir = if (i == 3) 1 else -1
        val b = Path().apply {
            moveTo(p.x, p.y)
            lineTo(p.x + 8.dp.toPx(), p.y + dir * 14.dp.toPx())
            lineTo(p.x + 2.dp.toPx(), p.y + dir * 26.dp.toPx())
        }
        drawPath(b, ink, style = Stroke(1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** まとめ: the scene line by line against the kinds of crack ([KyDiffTable.cols]); the cracked ones are ink squares. */
@Composable
internal fun CrackTablePanel(t: KyDiffTable) {
    val colors = AjlTheme.colors
    Column(
        Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(4.dp)).border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(t.title, style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp), color = colors.ink3, modifier = Modifier.weight(1f))
            t.cols.forEach { c ->
                Box(Modifier.width(34.dp), contentAlignment = Alignment.BottomCenter) {
                    Text(c.chunked(2).joinToString("\n"), style = AjlTheme.type.caption.copy(fontSize = 10.sp, lineHeight = 12.sp), color = colors.ink3, textAlign = TextAlign.Center)
                }
            }
        }
        t.rows.forEach { (ja, zh, flags) ->
            Row(
                Modifier.fillMaxWidth().drawBehind { drawLine(colors.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(ja, style = AjlTheme.type.jpBody.copy(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                    if (zh.isNotBlank()) Text(zh, style = AjlTheme.type.caption.copy(fontSize = 11.sp, lineHeight = 15.sp), color = colors.ink3)
                }
                flags.forEach { on ->
                    Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(18.dp).then(
                                if (on) Modifier.background(colors.ink, RoundedCornerShape(3.dp))
                                else Modifier.drawBehind {
                                    drawRoundRect(colors.line2, cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))))
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

// ------------------------------------------------------------------ 第十五巻 的小图（每个都是那个知识点本来的形状）

/** Dispatch for the 恐怖 figures; [KyFig.rows] mean something different for each kind. */
@Composable
internal fun KyofuFig(fig: KyFig) {
    when (fig.kind) {
        "box" -> BoxFig(fig)
        "slots" -> SlotsFig(fig)
        "same" -> SameFig(fig)
        "pins" -> PinsFig(fig)
        "count" -> CountFig(fig)
        "pairs" -> PairsFig(fig)
        "layers" -> LayersFig(fig)
        "frame3" -> Frame3Fig(fig)
        "arrows" -> ArrowsFig(fig)
        else -> SetsuzokuFig(fig)
    }
}

private val jpFig @Composable get() = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
private val noteFig @Composable get() = AjlTheme.type.caption.copy(fontSize = 11.sp, lineHeight = 15.sp)

/** 命名 = 关进格子: rows [word, note, closed|open]; closed is a solid box with a label, open has no edge at all. */
@Composable
private fun BoxFig(fig: KyFig) {
    val colors = AjlTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        fig.rows.forEach { r ->
            val closed = r.getOrNull(2) == "closed"
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .then(
                            if (closed) Modifier.border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp))
                            else Modifier.drawBehind {
                                // no box: a few scattered dots where an edge would be
                                for (i in 0 until 14) {
                                    val a = i / 14f * 6.283f
                                    drawCircle(colors.line2, 1.5.dp.toPx(), Offset(center.x + kotlin.math.cos(a) * size.width * 0.42f, center.y + kotlin.math.sin(a) * size.height * 0.4f))
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(r.getOrElse(0) { "" }, style = jpFig.copy(fontSize = 18.sp), color = if (closed) colors.ink else colors.ink2)
                }
                Text(r.getOrElse(1) { "" }, style = noteFig, color = colors.ink3)
            }
        }
    }
}

/** The places of a sentence: rows [text, what it is, note, ghost|void|hit]. */
@Composable
private fun SlotsFig(fig: KyFig) {
    val colors = AjlTheme.colors
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        fig.rows.forEach { r ->
            val kind = r.getOrElse(3) { "" }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                val shape = RoundedCornerShape(3.dp)
                Text(
                    r.getOrElse(0) { "" },
                    style = jpFig,
                    color = when (kind) { "void" -> colors.onInk; "ghost" -> colors.ink3; else -> colors.ink },
                    modifier = Modifier
                        .then(
                            when (kind) {
                                "void" -> Modifier.background(colors.ink, shape)
                                "ghost" -> Modifier.drawBehind { drawRoundRect(colors.faint, cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))) }
                                else -> Modifier.border(AjlStroke.Ink, colors.ink, shape)
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Text(r.getOrElse(1) { "" }, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = if (kind == "void") colors.ink else colors.ink3, textAlign = TextAlign.Center)
                Text(r.getOrElse(2) { "" }, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = colors.ink3, textAlign = TextAlign.Center)
            }
        }
    }
}

/** 同一句话 · 两个地方: [KyFig.top] once, then rows [ep, where · who → whom, ""|void]; the void row's place is ink. */
@Composable
private fun SameFig(fig: KyFig) {
    val colors = AjlTheme.colors
    if (fig.top.isNotBlank()) Text(fig.top, style = jpFig.copy(fontSize = 17.sp, lineHeight = 26.sp), color = colors.ink)
    fig.rows.forEach { r ->
        Row(
            Modifier.fillMaxWidth().drawBehind { drawLine(colors.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(r.getOrElse(0) { "" }, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = colors.ink3, modifier = Modifier.width(34.dp))
            val text = r.getOrElse(1) { "" }
            if (r.getOrNull(2) == "void") {
                val (where, to) = text.split(" → ").let { it[0] to it.getOrElse(1) { "" } }
                Text(where, style = AjlTheme.type.body.copy(fontSize = 13.sp), color = colors.onInk, modifier = Modifier.background(colors.ink, RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                if (to.isNotBlank()) Text("→ $to", style = AjlTheme.type.body.copy(fontSize = 13.sp), color = colors.ink)
            } else Text(text, style = AjlTheme.type.body.copy(fontSize = 13.sp), color = colors.ink)
        }
    }
    if (fig.note.isNotBlank()) Text(fig.note, style = noteFig.copy(fontSize = 12.sp), color = colors.ink3)
}

/** 几个地方的口音: a north–south line ([KyFig.labels]), rows [word, region, 0–1 position] pinned on it. */
@Composable
private fun PinsFig(fig: KyFig) {
    val colors = AjlTheme.colors
    val measurer = rememberTextMeasurer()
    val word = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.ink)
    val region = AjlTheme.type.meta.copy(fontSize = 10.sp, color = colors.ink3)
    Canvas(Modifier.fillMaxWidth().height(86.dp)) {
        val y = 50.dp.toPx()
        val x0 = 18.dp.toPx()
        val x1 = size.width - 18.dp.toPx()
        drawLine(colors.line2, Offset(x0, y), Offset(x1, y), 1.5.dp.toPx())
        fig.labels.getOrNull(0)?.let { val t = measurer.measure(it, region); drawText(t, topLeft = Offset(0f, y - t.size.height / 2)) }
        fig.labels.getOrNull(1)?.let { val t = measurer.measure(it, region); drawText(t, topLeft = Offset(size.width - t.size.width, y - t.size.height / 2)) }
        fig.rows.forEach { r ->
            val p = r.getOrNull(2)?.toFloatOrNull() ?: return@forEach
            val x = x0 + (x1 - x0) * p
            drawCircle(colors.ink, 4.dp.toPx(), Offset(x, y))
            val w = measurer.measure(r[0], word)
            drawText(w, topLeft = Offset((x - w.size.width / 2f).coerceIn(0f, size.width - w.size.width), y - w.size.height - 8.dp.toPx()))
            val g = measurer.measure(r.getOrElse(1) { "" }, region)
            drawText(g, topLeft = Offset((x - g.size.width / 2f).coerceIn(0f, size.width - g.size.width), y + 8.dp.toPx()))
        }
    }
}

/** 数一个，少一个: rows [number, words, on|gone|cut]; gone is struck through, cut ends in an empty box. */
@Composable
private fun CountFig(fig: KyFig) {
    val colors = AjlTheme.colors
    fig.rows.forEach { r ->
        val kind = r.getOrElse(2) { "" }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(min = 32.dp)) {
            Text(r.getOrElse(0) { "" }, style = jpFig, color = colors.ink, modifier = Modifier.width(44.dp))
            if (kind == "cut") {
                Box(Modifier.size(84.dp, 24.dp).background(colors.ink, RoundedCornerShape(3.dp)))
            } else {
                Text(
                    r.getOrElse(1) { "" },
                    style = jpFig.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = if (kind == "gone") colors.ink3 else colors.ink,
                    modifier = if (kind == "gone") Modifier.drawWithContent {
                        drawContent()
                        drawLine(colors.ink3, Offset(0f, size.height * 0.55f), Offset(size.width, size.height * 0.55f), 1.dp.toPx())
                    } else Modifier,
                )
            }
        }
    }
}

/** 一来一回: rows [who, call, reply, ok|dash|void]; the line from call to reply is solid, dashed, or never arrives. */
@Composable
private fun PairsFig(fig: KyFig) {
    val colors = AjlTheme.colors
    fig.rows.forEach { r ->
        val kind = r.getOrElse(3) { "" }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 40.dp).drawBehind { drawLine(colors.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(r.getOrElse(0) { "" }, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = colors.ink3, modifier = Modifier.width(56.dp))
            Text(r.getOrElse(1) { "" }, style = jpFig.copy(fontSize = 13.sp), color = colors.ink, modifier = Modifier.weight(1f, fill = false))
            Canvas(Modifier.weight(1f).height(12.dp).widthIn(min = 12.dp)) {
                val y = size.height / 2
                when (kind) {
                    "ok" -> drawLine(colors.ink, Offset(0f, y), Offset(size.width, y), AjlStroke.Ink.toPx())
                    "dash" -> drawLine(colors.faint, Offset(0f, y), Offset(size.width, y), AjlStroke.Ink.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
                }
            }
            val reply = r.getOrElse(2) { "" }
            val shape = RoundedCornerShape(3.dp)
            Box(
                Modifier
                    .widthIn(min = 72.dp)
                    .height(26.dp)
                    .then(
                        when (kind) {
                            "void" -> Modifier.background(colors.ink, shape)
                            "dash" -> Modifier.drawBehind { drawRoundRect(colors.faint, cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(AjlStroke.Ink.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))) }
                            else -> Modifier.border(AjlStroke.Ink, colors.ink, shape)
                        },
                    )
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (reply.isNotBlank()) Text(reply, style = jpFig.copy(fontSize = 13.sp), color = if (kind == "dash") colors.faint else colors.ink)
            }
        }
    }
}

/** 两层信息: rows [layer, what it knows, lit|dim]; the lit layer is ink, the other only a hairline. */
@Composable
private fun LayersFig(fig: KyFig) {
    val colors = AjlTheme.colors
    fig.rows.forEach { r ->
        val lit = r.getOrNull(2) == "lit"
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .then(if (lit) Modifier.background(colors.ink, RoundedCornerShape(4.dp)) else Modifier.border(AjlStroke.Hair, colors.line2, RoundedCornerShape(4.dp)))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(r.getOrElse(0) { "" }, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = if (lit) colors.onInk2 else colors.ink3, modifier = Modifier.width(32.dp))
            Text(r.getOrElse(1) { "" }, style = AjlTheme.type.body.copy(fontSize = 13.sp), color = if (lit) colors.onInk else colors.ink2)
        }
    }
}

/** 祝词的骨架: three parts down the page, rows [part, words, hit]; the hit one is ink. */
@Composable
private fun Frame3Fig(fig: KyFig) {
    val colors = AjlTheme.colors
    fig.rows.forEachIndexed { i, r ->
        val hit = r.getOrNull(2) == "hit"
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .then(if (hit) Modifier.background(colors.ink, RoundedCornerShape(4.dp)) else Modifier.border(AjlStroke.Hair, colors.line2, RoundedCornerShape(4.dp)))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("${i + 1} ${r.getOrElse(0) { "" }}", style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = if (hit) colors.onInk2 else colors.ink3, modifier = Modifier.width(48.dp))
            Text(r.getOrElse(1) { "" }, style = jpFig.copy(fontSize = 14.sp), color = if (hit) colors.onInk else colors.ink2)
        }
        if (i < fig.rows.lastIndex) Text("↓", style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = colors.faint, modifier = Modifier.padding(start = 18.dp))
    }
}

/** 敬语的方向: rows [word, up|down, what it should do, what it was made to do]. */
@Composable
private fun ArrowsFig(fig: KyFig) {
    val colors = AjlTheme.colors
    fig.rows.forEach { r ->
        val up = r.getOrNull(1) == "up"
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).drawBehind { drawLine(colors.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(r.getOrElse(0) { "" }, style = jpFig, color = colors.ink, modifier = Modifier.width(96.dp))
            Text(if (up) "↑" else "↓", style = AjlTheme.type.body.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = colors.ink)
            Column(Modifier.weight(1f)) {
                Text(r.getOrElse(2) { "" }, style = AjlTheme.type.body.copy(fontSize = 13.sp), color = colors.ink2)
                Text(r.getOrElse(3) { "" }, style = AjlTheme.type.body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = colors.bad)
            }
        }
    }
}
