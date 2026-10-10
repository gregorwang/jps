package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.katsuyou.KyDecide
import com.animejapaneselab.nativeapp.ui.katsuyou.KyFig
import com.animejapaneselab.nativeapp.ui.katsuyou.KyJoint
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.voicepack.LineVoicePill

/*
 * 第十六巻 接続: the head of Japanese is on the right, and the piece that arrives decides the one before it: what form
 * it takes (たい → たく), what word it is (おもい before んだ is 重い), what it means (電話 before する is a call).
 * One device for the whole book, the 接缝条: a line in blocks, each with a tooth on its right edge that is its form
 * (square 未然, round 連用, point 原形, slant 仮定, flat = a noun); the deciding block is ink, and the notch on its left
 * is the tooth it wants. Two plays: joint (a seam is open, pick what the left block becomes) and decider (tap the
 * block that decides; an arc runs back to what it decided).
 */

internal enum class BlockLook { Plain, Ink, Open, Target, Wrong, Faded }

internal enum class Seam { Lit, Strong }

private val ToothW = 9.dp
private val ToothH = 9.dp

/** The block outline: a notch on the left that fits [left] (the block before's tooth), a tooth on the right ([right]). */
private fun blockPath(w: Float, h: Float, left: String?, right: String?, tw: Float, th: Float, grow: Float): Path {
    val p = Path()
    val m = h / 2f
    p.moveTo(0f, 0f)
    p.lineTo(w, 0f)
    if (!right.isNullOrEmpty() && grow > 0f) {
        p.lineTo(w, m - th)
        tooth(p, w, m, right, tw * grow, th)
    }
    p.lineTo(w, h)
    p.lineTo(0f, h)
    if (!left.isNullOrEmpty()) {
        p.lineTo(0f, m + th)
        notch(p, m, left, tw, th)
    }
    p.close()
    return p
}

/** From (e, m - th) down to (e, m + th), bulging right. */
private fun tooth(p: Path, e: Float, m: Float, kind: String, tw: Float, th: Float) {
    when (kind) {
        "q" -> { p.lineTo(e + tw, m - th); p.lineTo(e + tw, m + th); p.lineTo(e, m + th) }
        "r" -> p.arcTo(Rect(e - tw, m - th, e + tw, m + th), -90f, 180f, false)
        "p" -> { p.lineTo(e + tw, m); p.lineTo(e, m + th) }
        "k" -> { p.lineTo(e + tw, m - th); p.lineTo(e, m + th) }
        else -> p.lineTo(e, m + th)
    }
}

/** From (0, m + th) up to (0, m - th), cut into the block: the same shape as [tooth], walked backwards. */
private fun notch(p: Path, m: Float, kind: String, tw: Float, th: Float) {
    when (kind) {
        "q" -> { p.lineTo(tw, m + th); p.lineTo(tw, m - th); p.lineTo(0f, m - th) }
        "r" -> p.arcTo(Rect(-tw, m - th, tw, m + th), 90f, -180f, false)
        "p" -> { p.lineTo(tw, m); p.lineTo(0f, m - th) }
        "k" -> { p.lineTo(tw, m - th); p.lineTo(0f, m - th) }
        else -> p.lineTo(0f, m - th)
    }
}

/** Just the seam on a block's left edge (to light it up). */
private fun seamPath(h: Float, left: String, tw: Float, th: Float): Path {
    val p = Path()
    val m = h / 2f
    if (left.isEmpty()) {
        p.moveTo(0f, 0f); p.lineTo(0f, h)
    } else {
        p.moveTo(0f, m + th); notch(p, m, left, tw, th)
    }
    return p
}

private fun stripFont(blocks: List<String>): TextUnit {
    val n = blocks.sumOf { it.length }
    return when {
        n <= 7 -> 26.sp
        n <= 10 -> 22.sp
        n <= 14 -> 19.sp
        else -> 16.sp
    }
}

/**
 * One 接缝条. [texts] / [romaji] per block (what shows now), [looks] per block, [teeth] = the tooth drawn on each block's
 * right (null = none; the notch of the next block always follows it). [seams] lights a seam (s = between s and s + 1).
 * [grow] scales the tooth of block [growAt] (the open block on reveal). [arrow] = (from block, to block, to the seam
 * on to's right instead of its middle). [under] = a small label under a block.
 */
@Composable
internal fun JointStrip(
    texts: List<String>,
    romaji: List<String>,
    looks: List<BlockLook>,
    teeth: List<String?>,
    modifier: Modifier = Modifier,
    seams: Map<Int, Seam> = emptyMap(),
    grow: Float = 1f,
    growAt: Int = -1,
    arrow: Triple<Int, Int, Boolean>? = null,
    under: Map<Int, Pair<String, Color>> = emptyMap(),
    post: String = "",
    fontSize: TextUnit = stripFont(texts),
    onTap: ((Int) -> Unit)? = null,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work.accent
    val measurer = rememberTextMeasurer()
    val coords = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
    var row by remember { androidx.compose.runtime.mutableStateOf<LayoutCoordinates?>(null) }
    val labelStyle = AjlTheme.type.meta.copy(fontSize = 10.sp)
    // many blocks (u18's whole sentence): tighter so it mostly fits a 360dp screen; the row still scrolls if not
    val pad = if (texts.size > 6) 5.dp else 10.dp
    Box(modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Row(
            Modifier
                .padding(start = 2.dp, end = 12.dp, top = if (arrow != null) 26.dp else 4.dp, bottom = if (under.isNotEmpty()) 22.dp else 4.dp)
                .onGloballyPositioned { row = it }
                .drawWithContent {
                    drawContent()
                    val r = row ?: return@drawWithContent
                    if (!r.isAttached) return@drawWithContent
                    fun box(i: Int): Rect? = coords[i]?.takeIf { it.isAttached }?.let { r.localBoundingBoxOf(it) }
                    arrow?.let { (from, to, toSeam) ->
                        val a = box(from)
                        val b = box(to)
                        if (a != null && b != null) {
                            val sx = a.center.x
                            val ex = if (toSeam) b.right + 2.dp.toPx() else b.center.x
                            val y = a.top - 4.dp.toPx()
                            val p = Path().apply {
                                moveTo(sx, y)
                                quadraticTo((sx + ex) / 2f, y - 26.dp.toPx() - (kotlin.math.abs(sx - ex) * 0.05f), ex, y - 2.dp.toPx())
                            }
                            drawPath(p, work, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                            val h = 5.dp.toPx()
                            val dir = if (ex < sx) 1f else -1f
                            drawLine(work, Offset(ex, y - 2.dp.toPx()), Offset(ex - h * 0.9f * -dir, y - 2.dp.toPx() - h), 1.5.dp.toPx(), StrokeCap.Round)
                            drawLine(work, Offset(ex, y - 2.dp.toPx()), Offset(ex + h * dir, y - 2.dp.toPx() - h * 0.3f), 1.5.dp.toPx(), StrokeCap.Round)
                        }
                    }
                    under.forEach { (i, lc) ->
                        val b = box(i) ?: return@forEach
                        val m = measurer.measure(lc.first, labelStyle.copy(color = lc.second))
                        drawText(m, topLeft = Offset(b.center.x - m.size.width / 2f, b.bottom + 4.dp.toPx()))
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            texts.forEachIndexed { i, t ->
                val look = looks[i]
                val left = if (i > 0) teeth[i - 1] else null
                val right = if (i < texts.lastIndex) teeth[i] else null
                val seam = if (i > 0) seams[i - 1] else null
                val fill = when (look) {
                    BlockLook.Ink -> colors.ink
                    BlockLook.Open, BlockLook.Target -> colors.bg
                    else -> colors.surface
                }
                val line = when (look) {
                    BlockLook.Wrong -> colors.bad
                    BlockLook.Faded -> colors.line2
                    else -> colors.ink
                }
                val dashed = look == BlockLook.Open || look == BlockLook.Target
                val fg = when (look) {
                    BlockLook.Ink -> colors.onInk
                    BlockLook.Open -> colors.ink3
                    BlockLook.Faded -> colors.ink3
                    BlockLook.Wrong -> colors.bad
                    else -> colors.ink
                }
                val g = if (i == growAt) grow else 1f
                Box(
                    Modifier
                        .heightIn(min = 58.dp)
                        .onGloballyPositioned { coords[i] = it }
                        .drawBehind {
                            val path = blockPath(size.width, size.height, left, right, ToothW.toPx(), ToothH.toPx(), g)
                            drawPath(path, fill)
                            drawPath(
                                path, line,
                                style = Stroke(
                                    AjlStroke.Ink.toPx(), join = StrokeJoin.Round,
                                    pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())) else null,
                                ),
                            )
                            if (seam != null && left != null) {
                                drawPath(
                                    seamPath(size.height, left, ToothW.toPx(), ToothH.toPx()), work,
                                    style = Stroke((if (seam == Seam.Strong) 3.dp else 1.5.dp).toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round),
                                )
                            }
                        }
                        .then(if (onTap != null) Modifier.clickableNoRipple(onClick = { onTap(i) }).semantics { role = Role.Button } else Modifier)
                        .padding(start = if (left.isNullOrEmpty()) pad else ToothW + pad * 0.6f, end = pad, top = 6.dp, bottom = 6.dp)
                        .widthIn(min = 22.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            romaji.getOrElse(i) { "" },
                            style = AjlTheme.type.meta.copy(fontSize = 10.sp),
                            color = if (look == BlockLook.Ink) colors.onInk2 else colors.ink3,
                            maxLines = 1,
                        )
                        Text(t, style = AjlTheme.type.jpBody.copy(fontSize = fontSize, lineHeight = fontSize * 1.3f, fontWeight = FontWeight.Bold), color = fg, maxLines = 1)
                    }
                }
            }
            if (post.isNotBlank()) {
                Text(
                    post, style = AjlTheme.type.jpBody.copy(fontSize = fontSize * 0.85f, fontWeight = FontWeight.Bold), color = colors.ink3,
                    modifier = Modifier.padding(start = 10.dp), maxLines = 1,
                )
            }
        }
    }
}

/** 说话的 + the part of the line before the strip, the strip, then the zh once revealed. */
@Composable
private fun StripCard(
    who: String,
    pre: String,
    preRomaji: String,
    zh: String,
    showZh: Boolean,
    strip: @Composable () -> Unit,
) {
    val colors = AjlTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(4.dp))
            .border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp))
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (who.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("说话的", style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp), color = colors.ink3)
                Text(who, style = AjlTheme.type.label.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
            }
        }
        if (pre.isNotBlank()) {
            Column {
                if (preRomaji.isNotBlank()) Text(preRomaji, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = colors.ink3)
                Text(pre.trim(), style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold), color = colors.ink2)
            }
        }
        strip()
        if (showZh && zh.isNotBlank()) Text(zh, style = AjlTheme.type.body.copy(fontSize = 14.sp, lineHeight = 20.sp), color = colors.ink)
    }
}

// ------------------------------------------------------------------ joint：左边那节变成什么？

@Composable
internal fun JointSitting(
    items: List<KyJoint>,
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
    var picked by rememberSaveable(key) { mutableIntStateOf(-1) }
    var right by rememberSaveable(key) { mutableIntStateOf(0) }
    val missed = remember(key) { mutableStateListOf<Int>() }
    val item = items.getOrNull(index) ?: return
    val revealed = picked >= 0
    val last = index == items.lastIndex
    val grow = remember(key, index) { Animatable(0f) }
    LaunchedEffect(revealed, key, index) {
        if (revealed) { if (reduced) grow.snapTo(1f) else grow.animateTo(1f, tween(220)) } else grow.snapTo(0f)
    }

    fun pick(k: Int) {
        if (revealed) return
        val ok = k == item.answer
        feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
        onAnswer(ok)
        if (ok) right++ else missed.add(index)
        picked = k
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
            if (last) onDone(right, items.size, missed.map { items[it].let { j -> TsuzukuLine(j.ja, true, j.blocks[j.at].text) } })
            else { index++; picked = -1 }
        },
        arrow = !last,
        modifier = modifier,
    ) {
        if (item.context.isNotBlank()) ContextStrip(item.context)
        val texts = item.blocks.mapIndexed { i, b -> if (i == item.at && !revealed) "？" else b.text }
        val romaji = item.blocks.mapIndexed { i, b -> if (i == item.at && !revealed) "" else b.romaji }
        val looks = item.blocks.indices.map { i ->
            when {
                i == item.ink -> BlockLook.Ink
                i == item.at && !revealed -> BlockLook.Open
                else -> BlockLook.Plain
            }
        }
        // the open block has no tooth until it is filled; then it grows the right one
        val teeth = item.blocks.mapIndexed { i, b -> if (i == item.at && !revealed) null else b.edge }
        val seams = buildMap {
            item.lit.forEach { put(it, Seam.Lit) }
            if (revealed) put(item.at, Seam.Strong)
        }
        StripCard(item.who, item.pre, item.preRomaji, item.zh, revealed) {
            JointStrip(
                texts = texts, romaji = romaji, looks = looks, teeth = teeth,
                seams = seams, grow = grow.value, growAt = item.at,
                arrow = Triple(item.ink, item.at, true),
                under = if (revealed) mapOf(item.at to (formName(item.blocks[item.at].edge) + " ✓" to AjlTheme.work.accent)) else emptyMap(),
                post = item.post,
            )
        }
        LineVoicePill(item.ja, item.audioUrl, audio, settings.ttsWorkerUrl, Modifier.align(Alignment.CenterHorizontally))
        if (!revealed && item.ask.isNotBlank()) {
            Text(item.ask, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, modifier = Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item.options.forEachIndexed { k, o ->
                val state = when {
                    !revealed -> TileState.Idle
                    k == item.answer -> TileState.Right
                    k == picked -> TileState.Wrong
                    else -> TileState.Dim
                }
                PickTile(state = state, onClick = { pick(k) }, modifier = Modifier.weight(1f)) { fg ->
                    Text(o.romaji, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = fg.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.CenterHorizontally), maxLines = 1)
                    Text(o.text, style = AjlTheme.type.jpBody.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), color = fg, modifier = Modifier.align(Alignment.CenterHorizontally), maxLines = 1)
                }
            }
        }
        AnimatedVisibility(revealed, enter = fadeIn(tween(360)) + expandVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (item.forms.isNotEmpty()) FormsPanel(item.forms)
                item.fig?.let { FigPanel(it) }
                val ok = picked == item.answer
                val chosen = item.options.getOrNull(picked)
                Verdict(if (ok) "✓ ${item.options[item.answer].text}" else "你选 ${chosen?.text.orEmpty()} · 要 ${item.options[item.answer].text}", ok)
                if (!ok && chosen != null && chosen.why.isNotBlank()) Text(chosen.why, style = AjlTheme.type.body, color = colors.bad)
                if (item.why.isNotBlank()) Text(item.why, style = AjlTheme.type.body, color = colors.ink)
                if (item.rule.isNotBlank()) Text(item.rule, style = AjlTheme.type.body, color = colors.ink2)
            }
        }
    }
}

private fun formName(edge: String) = when (edge) {
    "q" -> "未然"
    "r" -> "連用"
    "p" -> "原形"
    "k" -> "仮定"
    else -> "名词"
}

/** 同一节 · 右边换了，它就换形: (form, right, word, zh[, now]). */
@Composable
private fun FormsPanel(rows: List<List<String>>) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column(
        Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(12.dp)).border(AjlStroke.Hair, colors.line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text("同一节 · 右边换了，它就换形", style = AjlTheme.type.meta.copy(fontSize = 10.sp, letterSpacing = 0.4.sp), color = colors.ink3, modifier = Modifier.padding(bottom = 4.dp))
        rows.forEach { r ->
            val now = r.getOrNull(4) == "now"
            Row(
                Modifier.fillMaxWidth().heightIn(min = 40.dp).then(if (now) Modifier.background(work.soft, RoundedCornerShape(4.dp)) else Modifier).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(r.getOrElse(0) { "" }, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = if (now) work.accent else colors.ink, modifier = Modifier.width(56.dp))
                Text(r.getOrElse(1) { "" }, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = if (now) work.accent else colors.ink3, modifier = Modifier.width(64.dp))
                Text(r.getOrElse(2) { "" }, style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, modifier = Modifier.weight(1f), maxLines = 1)
                Text(r.getOrElse(3) { "" }, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3, maxLines = 1)
            }
        }
    }
}

// ------------------------------------------------------------------ decider：谁说了算？

@Composable
internal fun DecideSitting(
    items: List<KyDecide>,
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
    var index by rememberSaveable(key) { mutableIntStateOf(0) }
    var picked by rememberSaveable(key) { mutableIntStateOf(-1) }
    var right by rememberSaveable(key) { mutableIntStateOf(0) }
    val missed = remember(key) { mutableStateListOf<Int>() }
    val item = items.getOrNull(index) ?: return
    val revealed = picked >= 0
    val last = index == items.lastIndex

    fun pick(k: Int) {
        if (revealed) return
        val ok = k == item.answer
        feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
        onAnswer(ok)
        if (ok) right++ else missed.add(index)
        picked = k
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
            if (last) onDone(right, items.size, missed.map { items[it].let { d -> TsuzukuLine(d.ja, true, d.blocks[d.answer].text) } })
            else { index++; picked = -1 }
        },
        arrow = !last,
        modifier = modifier,
    ) {
        if (item.context.isNotBlank()) ContextStrip(item.context)
        val work = AjlTheme.work.accent
        val texts = item.blocks.mapIndexed { i, b -> if (i == item.target && !revealed) item.kana.ifBlank { b.text } else b.text }
        val looks = item.blocks.indices.map { i ->
            when {
                revealed && i == item.answer -> BlockLook.Ink
                revealed && i == picked -> BlockLook.Wrong
                i == item.target && !revealed -> BlockLook.Target
                else -> BlockLook.Plain
            }
        }
        // the word classes (teeth) only show once the decider is found
        val teeth = item.blocks.map { if (revealed) it.edge else null }
        val under = buildMap {
            if (revealed) {
                val at = if (item.target >= 0) item.target else item.answer
                if (item.tag.isNotBlank()) put(at, item.tag to work)
                if (picked != item.answer) put(picked, "你选的" to colors.bad)
            }
        }
        StripCard(item.who, item.pre, item.preRomaji, item.zh, revealed) {
            JointStrip(
                texts = texts, romaji = item.blocks.map { it.romaji }, looks = looks, teeth = teeth,
                seams = if (revealed && item.target >= 0 && item.answer == item.target + 1) mapOf(item.target to Seam.Strong) else emptyMap(),
                arrow = if (revealed && item.target >= 0) Triple(item.answer, item.target, false) else null,
                under = under,
                post = item.post,
                onTap = { pick(it) },
            )
            if (!revealed && item.hint.isNotBlank()) {
                Text(item.hint, style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.ink3)
            }
        }
        LineVoicePill(item.ja, item.audioUrl, audio, settings.ttsWorkerUrl, Modifier.align(Alignment.CenterHorizontally))
        if (!revealed && item.ask.isNotBlank()) {
            Text(item.ask, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, modifier = Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
        }
        AnimatedVisibility(revealed, enter = fadeIn(tween(360)) + expandVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item.fig?.let { FigPanel(it) }
                val ok = picked == item.answer
                Verdict(if (ok) "✓ ${item.blocks[item.answer].text}" else "你点了 ${item.blocks.getOrNull(picked)?.text.orEmpty()} · 说了算的是 ${item.blocks[item.answer].text}", ok)
                if (!ok && item.left.isNotBlank()) Text(item.left, style = AjlTheme.type.body, color = colors.bad)
                if (item.why.isNotBlank()) Text(item.why, style = AjlTheme.type.body, color = colors.ink)
                if (item.rule.isNotBlank()) Text(item.rule, style = AjlTheme.type.body, color = colors.ink2)
            }
        }
    }
}

// ------------------------------------------------------------------ 小图

@Composable
internal fun SetsuzokuFig(fig: KyFig) {
    when (fig.kind) {
        "rails2" -> Rails2Fig(fig)
        "fold" -> FoldFig(fig)
        "rails" -> RailsFig(fig)
        "nest" -> NestFig(fig, fromRight = false)
        "chain" -> NestFig(fig, fromRight = true)
        "grid" -> GridFig(fig)
        "scale" -> ScaleFig(fig)
        "bounds" -> BoundsFig(fig)
        "sound" -> SoundFig(fig)
        "dangle" -> DangleFig(fig)
        "joints" -> JointsFig(fig)
    }
    if (fig.note.isNotBlank()) Text(fig.note, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = AjlTheme.colors.ink3)
}

private val figJp @Composable get() = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)

/** たい 和 高い: rows of [stem, tails…], the last row the labels; stems right-aligned so the tails line up. */
@Composable
private fun Rails2Fig(fig: KyFig) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work.accent
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        fig.rows.forEachIndexed { ri, r ->
            val label = ri == fig.rows.lastIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.firstOrNull().orEmpty(), style = figJp, color = colors.ink2, textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
                r.drop(1).forEach { c ->
                    Box(
                        Modifier.weight(1f).padding(horizontal = 2.dp).heightIn(min = if (label) 20.dp else 36.dp)
                            .then(if (label) Modifier else Modifier.border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp))),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (label) Text(c, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = colors.ink3)
                        else Text(c, style = figJp, color = work)
                    }
                }
            }
        }
    }
}

/** く＋あっ → かっ: rows in pairs, the long way then the short; the middle piece is the one that merged. */
@Composable
private fun FoldFig(fig: KyFig) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work.accent
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        fig.rows.chunked(2).forEach { pair ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val a = pair[0]
                Text(a.filter { it.isNotBlank() }.joinToString(" ＋ "), style = figJp.copy(fontWeight = FontWeight.Medium), color = colors.ink3)
                pair.getOrNull(1)?.let { b ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("→ ", style = AjlTheme.type.meta, color = colors.ink3)
                        b.forEachIndexed { i, c ->
                            if (c.isNotBlank()) Text(c, style = figJp, color = if (i == 1) work else colors.ink)
                        }
                    }
                }
            }
        }
    }
}

/** 两条轨: rows [text, v | a | bridge]; verbs on the top rail, adjectives on the bottom one, a bridge between. */
@Composable
private fun RailsFig(fig: KyFig) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work.accent
    val measurer = rememberTextMeasurer()
    val style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
    val meta = AjlTheme.type.meta.copy(fontSize = 9.sp, color = colors.ink3)
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val topY = 30.dp.toPx()
        val botY = 112.dp.toPx()
        val midY = (topY + botY) / 2f
        drawLine(colors.ink, Offset(0f, topY), Offset(size.width, topY), 1.5.dp.toPx())
        drawLine(colors.ink, Offset(0f, botY - 2.dp.toPx()), Offset(size.width, botY - 2.dp.toPx()), 1.dp.toPx())
        drawLine(colors.ink, Offset(0f, botY + 2.dp.toPx()), Offset(size.width, botY + 2.dp.toPx()), 1.dp.toPx())
        drawText(measurer.measure("动词", meta), topLeft = Offset(0f, topY + 4.dp.toPx()))
        drawText(measurer.measure("形容词", meta), topLeft = Offset(0f, botY + 6.dp.toPx()))
        val gap = 10.dp.toPx()
        val ms = fig.rows.map { measurer.measure(it[0], style) }
        val total = ms.sumOf { it.size.width + 16.dp.toPx().toDouble() }.toFloat() + gap * (ms.size - 1)
        var x = ((size.width - total) / 2f).coerceAtLeast(30.dp.toPx())
        var prev: Pair<Float, Float>? = null
        fig.rows.forEachIndexed { i, r ->
            val m = ms[i]
            val w = m.size.width + 16.dp.toPx()
            val h = 26.dp.toPx()
            val cy = when (r.getOrNull(1)) { "a" -> botY - h / 2f - 6.dp.toPx(); "bridge" -> midY; else -> topY - h / 2f - 2.dp.toPx() }
            val bridge = r.getOrNull(1) == "bridge"
            prev?.let { (px, py) ->
                val p = Path().apply { moveTo(px, py); cubicTo(px + gap, py, x - gap, cy, x, cy) }
                drawPath(p, if (bridge || fig.rows.getOrNull(i - 1)?.getOrNull(1) == "bridge") work else colors.ink3, style = Stroke(if (bridge) 2.5.dp.toPx() else 1.5.dp.toPx()))
            }
            drawRect(if (bridge) work.copy(alpha = 0.12f) else colors.surface, Offset(x, cy - h / 2f), androidx.compose.ui.geometry.Size(w, h))
            drawRect(if (bridge) work else colors.ink, Offset(x, cy - h / 2f), androidx.compose.ui.geometry.Size(w, h), style = Stroke(1.3.dp.toPx()))
            drawText(m, color = if (bridge) work else colors.ink, topLeft = Offset(x + 8.dp.toPx(), cy - m.size.height / 2f))
            prev = (x + w) to cy
            x += w + gap
        }
    }
}

/** 一层包一层 (or, [fromRight], hung on from the right: の 链): words in a row, brackets underneath, one per layer. */
@Composable
private fun NestFig(fig: KyFig, fromRight: Boolean) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work.accent
    val measurer = rememberTextMeasurer()
    val style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.ink)
    val zh = AjlTheme.type.caption.copy(fontSize = 10.sp, color = colors.ink3)
    val n = fig.rows.size
    Canvas(Modifier.fillMaxWidth().height((40 + 16 * n).dp)) {
        val ms = fig.rows.map { measurer.measure(it[0], style) }
        val pad = 6.dp.toPx()
        val total = ms.sumOf { it.size.width + pad.toDouble() * 2 }.toFloat()
        val scale = if (total > size.width) size.width / total else 1f
        var x = 0f
        val spans = ms.map { m -> val w = (m.size.width + pad * 2) * scale; val s = x to x + w; x += w; s }
        ms.forEachIndexed { i, m -> drawText(m, topLeft = Offset(spans[i].first + pad * scale, 0f)) }
        val y0 = ms.maxOf { it.size.height } + 6.dp.toPx()
        val step = 14.dp.toPx()
        for (layer in 1 until n) {
            val (a, b) = if (!fromRight) 0 to layer else (n - 1 - layer) to (n - 1)
            val y = y0 + step * layer
            val l = spans[a].first + 2.dp.toPx()
            val r = spans[b].second - 2.dp.toPx()
            val c = if (layer == n - 1) work else colors.ink3
            drawLine(c, Offset(l, y - 6.dp.toPx()), Offset(l, y), 1.2.dp.toPx())
            drawLine(c, Offset(l, y), Offset(r, y), 1.2.dp.toPx())
            drawLine(c, Offset(r, y), Offset(r, y - 6.dp.toPx()), 1.2.dp.toPx())
            val label = fig.rows[if (!fromRight) b else a].getOrElse(1) { "" }
            val lm = measurer.measure(label, zh)
            val lx = if (!fromRight) r + 4.dp.toPx() else l - lm.size.width - 4.dp.toPx()
            drawText(lm, topLeft = Offset(lx.coerceIn(0f, size.width - lm.size.width), y - lm.size.height / 2f))
        }
    }
}

/** A small table; an empty cell is a dashed box: the form that does not exist. Row 0 = the headers. */
@Composable
private fun GridFig(fig: KyFig) {
    val colors = AjlTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        fig.rows.forEachIndexed { ri, r ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                r.forEachIndexed { ci, c ->
                    val head = ri == 0 || ci == 0
                    Box(
                        Modifier.weight(1f).heightIn(min = if (head) 24.dp else 40.dp)
                            .then(
                                when {
                                    head -> Modifier
                                    c.isBlank() -> Modifier.drawBehind {
                                        drawRect(colors.bad, style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
                                    }
                                    else -> Modifier.border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp))
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            head -> Text(c, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = colors.ink3, textAlign = TextAlign.Center)
                            c.isBlank() -> Text("（没有）", style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.bad)
                            else -> Text(c, style = figJp, color = colors.ink)
                        }
                    }
                }
            }
        }
    }
}

/** 有刻度的变化: rows [word, what grows, ok | no]; ok runs to an end mark, no is short and dashed. */
@Composable
private fun ScaleFig(fig: KyFig) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work.accent
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        fig.rows.forEach { r ->
            val ok = r.getOrNull(2) == "ok"
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(r[0], style = figJp, color = if (ok) colors.ink else colors.bad, modifier = Modifier.width(64.dp))
                Column(Modifier.weight(1f)) {
                    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                        val y = size.height / 2f
                        if (ok) {
                            val n = 6
                            for (k in 0..n) drawLine(colors.ink3, Offset(size.width * k / n, y - 4.dp.toPx()), Offset(size.width * k / n, y + 4.dp.toPx()), 1.dp.toPx())
                            drawLine(colors.ink, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx())
                            drawLine(work, Offset(size.width - 1.dp.toPx(), 0f), Offset(size.width - 1.dp.toPx(), size.height), 3.dp.toPx())
                        } else {
                            drawLine(colors.bad, Offset(0f, y), Offset(size.width * 0.35f, y), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                        }
                    }
                    Text(r.getOrElse(1) { "" }, style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.ink3)
                }
            }
        }
    }
}

/** 假名的边界 vs 词素的边界: row 0 kana, row 1 their romaji, row 2 the morphemes; widths by letters so the cuts disagree. */
@Composable
private fun BoundsFig(fig: KyFig) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work.accent
    val kana = fig.rows.getOrNull(0).orEmpty()
    val ro = fig.rows.getOrNull(1).orEmpty()
    val mor = fig.rows.getOrNull(2).orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            kana.forEachIndexed { i, k ->
                Column(Modifier.weight(ro.getOrElse(i) { "x" }.length.toFloat().coerceAtLeast(1f)).border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp)).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(ro.getOrElse(i) { "" }, style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3)
                    Text(k, style = figJp, color = if (i == kana.lastIndex) work else colors.ink)
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            mor.forEachIndexed { i, m ->
                Box(Modifier.weight(m.length.toFloat().coerceAtLeast(1f)).padding(horizontal = 1.dp).background(if (i == mor.lastIndex) AjlTheme.work.soft else colors.sunken, RoundedCornerShape(4.dp)).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(m, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = if (i == mor.lastIndex) work else colors.ink2)
                }
            }
        }
        Text("上：假名　下：词素（词干 · 他动后缀 · 语尾）", style = AjlTheme.type.caption.copy(fontSize = 10.sp), color = colors.ink3)
    }
}

/** 同一个音 · 看右边: rows [left, word, right, class, edge]; the right one inked, the class beside it. */
@Composable
private fun SoundFig(fig: KyFig) {
    val colors = AjlTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        fig.rows.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    JointStrip(
                        texts = listOf(r[0], r[1], r[2]), romaji = listOf("", "", ""),
                        looks = listOf(BlockLook.Faded, BlockLook.Plain, BlockLook.Ink), teeth = listOf(null, r.getOrElse(4) { "" }, null),
                        fontSize = 14.sp,
                    )
                }
                Text(r.getOrElse(3) { "" }, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink2, modifier = Modifier.width(64.dp))
            }
        }
    }
}

/** ために 右边空着: the blocks of [rows] ([text, tooth]) and a dashed empty block at the end. */
@Composable
private fun DangleFig(fig: KyFig) {
    val texts = fig.rows.map { it[0] } + "？"
    JointStrip(
        texts = texts, romaji = texts.map { "" },
        looks = fig.rows.map { BlockLook.Plain } + BlockLook.Open,
        teeth = fig.rows.map { it.getOrElse(1) { "" } } + null,
        fontSize = 17.sp,
    )
}

/** 这本书的接缝: rows [tooth, right pieces, the form they want, example]. */
@Composable
private fun JointsFig(fig: KyFig) {
    val colors = AjlTheme.colors
    Column {
        fig.rows.forEach { r ->
            Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToothGlyph(r[0])
                Column(Modifier.weight(1f)) {
                    Text(r.getOrElse(1) { "" }, style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = colors.ink)
                    Text(r.getOrElse(2) { "" }, style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.ink3)
                }
                Text(r.getOrElse(3) { "" }, style = AjlTheme.type.jpBody.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = colors.ink2)
            }
        }
    }
}

/** A block silhouette with one tooth, for legends. */
@Composable
internal fun ToothGlyph(edge: String, size: Dp = 30.dp) {
    val colors = AjlTheme.colors
    Canvas(Modifier.size(width = size, height = size * 0.7f)) {
        val w = this.size.width * 0.72f
        val p = blockPath(w, this.size.height, null, edge.ifEmpty { null }, this.size.width * 0.24f, this.size.height * 0.24f, 1f)
        drawPath(p, colors.surface)
        drawPath(p, colors.ink, style = Stroke(1.3.dp.toPx(), join = StrokeJoin.Round))
    }
}
