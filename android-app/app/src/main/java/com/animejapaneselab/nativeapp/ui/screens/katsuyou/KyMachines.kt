package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.Screentone
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.katsuyou.KyDial
import com.animejapaneselab.nativeapp.ui.katsuyou.KySpeed
import com.animejapaneselab.nativeapp.ui.katsuyou.KyStep
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.screens.zougo.KindChip
import com.animejapaneselab.nativeapp.ui.screens.zougo.LineCard
import com.animejapaneselab.nativeapp.ui.screens.zougo.StagePanel
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private typealias Done = (right: Int, asked: Int, missed: List<TsuzukuLine>) -> Unit

// ------------------------------------------------------------------ G 语速档位

/** The same sentence from 书面 to 口语 on four stops; dial, listen, and say which one the anime uses. */
@Composable
internal fun SpeedSitting(
    items: List<KySpeed>, key: String, eyebrow: String, title: String, settings: LabSettings,
    onClose: () -> Unit, onAnswer: (Boolean) -> Unit, onDone: Done, lastLabel: String, modifier: Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val feedback = LocalFeedbackEngine.current
    val audio = rememberLessonAudioController()
    var index by rememberSaveable(key) { mutableIntStateOf(0) }
    var pos by rememberSaveable(key) { mutableIntStateOf(0) }
    var picked by rememberSaveable(key) { mutableIntStateOf(-1) }
    var right by rememberSaveable(key) { mutableIntStateOf(0) }
    val missed = remember(key) { mutableStateListOf<Int>() }
    val item = items.getOrNull(index) ?: return
    val revealed = picked >= 0
    val last = index == items.lastIndex
    val (text, ro) = item.stops[pos]
    val isOrig = revealed && pos == item.answer

    KySitting(
        eyebrow = eyebrow, title = title, counter = "${index + 1} / ${items.size}",
        progress = (index + if (revealed) 1 else 0).toFloat() / items.size, onClose = onClose,
        button = if (revealed) (if (last) lastLabel else "下一句") else null,
        onButton = {
            audio.stop()
            if (last) onDone(right, items.size, missed.map { items[it].let { s -> TsuzukuLine(s.stops[s.answer].first, true, s.stops[s.answer].second) } })
            else { index++; picked = -1; pos = 0 }
        },
        arrow = !last, modifier = modifier,
    ) {
        if (isOrig) {
            LineCard(item.line, audio, settings.ttsWorkerUrl)
        } else {
            StagePanel {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "▶ 听第 ${pos + 1} 档",
                            style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                            color = work.accent,
                            modifier = Modifier.clickableNoRipple({ audio.speakText(item.pre + text, settings.ttsWorkerUrl) }).padding(vertical = 8.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        Text("第 ${pos + 1} 档", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3)
                    }
                    Text(ro, style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = colors.ink3)
                    Text(
                        buildAnnotatedString { append(item.pre); withStyle(SpanStyle(color = work.accent)) { append(text) } },
                        style = AjlTheme.type.jpBody.copy(fontSize = 22.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.ink,
                    )
                    Text(item.line.zh, style = AjlTheme.type.body.copy(fontSize = 13.sp), color = colors.ink2)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("慢 · 书面", style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
                Spacer(Modifier.weight(1f))
                Text("快 · 口语", style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
            }
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                item.stops.indices.forEach { k ->
                    val on = k <= pos
                    val scale by animateFloatAsState(if (k == pos) 1.25f else 1f, tween(200), label = "stop")
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickableNoRipple({
                                pos = k
                                if (!(revealed && k == item.answer)) audio.speakText(item.pre + item.stops[k].first, settings.ttsWorkerUrl)
                            })
                            .semantics { role = Role.Button; contentDescription = "第 ${k + 1} 档" },
                        contentAlignment = Alignment.Center,
                    ) {
                        // the rail between stops
                        Row(Modifier.fillMaxWidth()) {
                            Box(Modifier.weight(1f).height(2.dp).background(if (k == 0) Color.Transparent else if (on) work.accent else colors.line2))
                            Box(Modifier.weight(1f).height(2.dp).background(if (k == item.stops.lastIndex) Color.Transparent else if (k < pos) work.accent else colors.line2))
                        }
                        Box(
                            Modifier
                                .size((22 * scale).dp)
                                .background(if (on) work.accent else colors.bg, CircleShape)
                                .border(AjlStroke.Ink, if (on) work.accent else colors.ink, CircleShape),
                        )
                        if (revealed && k == item.answer) {
                            Text(
                                "原作",
                                style = AjlTheme.type.caption.copy(fontSize = 10.sp, lineHeight = 14.sp),
                                color = work.accent,
                                modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp).rotate(6f)
                                    .background(colors.bg).border(AjlStroke.Ink, work.accent, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        if (!revealed) {
            Text("拨一拨，听一听：原作说到了第几档？", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2, modifier = Modifier.align(Alignment.CenterHorizontally))
            PickTile(TileState.Idle, {
                val ok = pos == item.answer
                feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
                onAnswer(ok)
                if (ok) right++ else missed.add(index)
                picked = pos
                pos = item.answer
            }, Modifier.fillMaxWidth()) { fg ->
                Text("就是这一档", style = AjlTheme.type.body.copy(fontSize = 15.sp), color = fg, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Verdict(if (picked == item.answer) "✓ 原作就是这么快" else "原作说到了第 ${item.answer + 1} 档", picked == item.answer)
                Text(item.note, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 23.sp), color = colors.ink)
            }
        }
    }
}

// ------------------------------------------------------------------ A 活用盘

private val Dan = listOf("あ段", "い段", "う段", "え段", "お段")

/** The verb on the left, its 行 as five cells on the right: tap the 段 the ending slides to. */
@Composable
internal fun DialSitting(
    items: List<KyDial>, key: String, eyebrow: String, title: String, settings: LabSettings,
    onClose: () -> Unit, onAnswer: (Boolean) -> Unit, onDone: Done, lastLabel: String, modifier: Modifier,
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
    val a = item.answer

    fun pick(k: Int) {
        if (revealed) return
        val ok = k == a
        feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
        onAnswer(ok)
        if (ok) right++ else missed.add(index)
        picked = k
        audio.speakText(item.word, settings.ttsWorkerUrl)
    }

    KySitting(
        eyebrow = eyebrow, title = title, counter = "${index + 1} / ${items.size}",
        progress = (index + if (revealed) 1 else 0).toFloat() / items.size, onClose = onClose,
        button = if (revealed) (if (last) lastLabel else "下一个") else null,
        onButton = {
            audio.stop()
            if (last) onDone(right, items.size, missed.map { items[it].let { d -> TsuzukuLine(d.word, true, d.verb) } }) else { index++; picked = -1 }
        },
        arrow = !last, modifier = modifier,
    ) {
        MangaPanel(Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
            Screentone(Modifier.align(Alignment.BottomStart).offset(x = (-30).dp, y = 14.dp).size(170.dp, 70.dp).rotate(-12f), color = work.tone(0.26f))
            Row(Modifier.fillMaxWidth().heightIn(min = 300.dp)) {
                Column(
                    Modifier.weight(1f).heightIn(min = 300.dp).padding(14.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("→ ${item.form}", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = work.accent)
                    Spacer(Modifier.height(10.dp))
                    val endK = if (revealed && a < 5) item.kana[a] else if (revealed) "" else item.kana[item.cur]
                    val endRo = if (revealed && a < 5) item.romaji[a] else if (revealed) "" else item.romaji[item.cur]
                    Text(
                        buildAnnotatedString {
                            append(item.stemRomaji)
                            withStyle(SpanStyle(color = if (revealed) colors.info else colors.ink2)) { append(endRo) }
                            if (revealed) withStyle(SpanStyle(color = work.accent)) { append(item.suffixRomaji) }
                        },
                        style = AjlTheme.type.meta.copy(fontSize = 14.sp),
                        color = colors.ink2,
                    )
                    Text(
                        buildAnnotatedString {
                            append(item.stem)
                            withStyle(SpanStyle(color = if (revealed) colors.info else colors.ink)) { append(endK) }
                            if (revealed) withStyle(SpanStyle(color = work.accent)) { append(item.suffix) }
                        },
                        style = AjlTheme.type.jpDisplay.copy(fontSize = 40.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink,
                        modifier = Modifier.clickableNoRipple({ audio.speakText(if (revealed) item.word else item.verb, settings.ttsWorkerUrl) }),
                    )
                    Text(if (revealed) "${item.verb} → ${item.word}" else item.verb, style = AjlTheme.type.caption.copy(fontSize = 13.sp), color = colors.ink3)
                }
                Column(
                    Modifier.width(96.dp).heightIn(min = 300.dp).background(colors.bg).padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(item.rowName, style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.ink3, modifier = Modifier.align(Alignment.CenterHorizontally))
                    item.kana.forEachIndexed { n, k ->
                        val bg = when {
                            revealed && n == a -> work.accent
                            !revealed && n == item.cur -> colors.sunken
                            else -> Color.Transparent
                        }
                        val fg = when {
                            revealed && n == a -> colors.bg
                            revealed && n == picked -> colors.bad
                            else -> colors.ink
                        }
                        val bgA by animateColorAsState(bg, tween(220), label = "dial-cell")
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(bgA, RoundedCornerShape(4.dp))
                                .border(if (revealed && n == picked && n != a) AjlStroke.Ink else 0.dp, if (revealed && n == picked && n != a) colors.bad else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickableNoRipple({ pick(n) })
                                .semantics { role = Role.Button; contentDescription = "${Dan[n]} $k" }
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (!revealed && n == item.cur) "原形" else Dan[n], style = AjlTheme.type.caption.copy(fontSize = 10.sp), color = fg.copy(alpha = 0.7f), modifier = Modifier.width(30.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(item.romaji[n], style = AjlTheme.type.meta.copy(fontSize = 9.sp, lineHeight = 10.sp), color = fg.copy(alpha = 0.7f))
                                Text(k, style = AjlTheme.type.jpBody.copy(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = if (n == 2) FontWeight.Normal else FontWeight.Bold), color = fg)
                            }
                        }
                    }
                }
            }
        }
        if (item.extra.isNotBlank()) {
            PickTile(
                when {
                    !revealed -> TileState.Idle
                    a == 5 -> TileState.Right
                    picked == 5 -> TileState.Wrong
                    else -> TileState.Dim
                },
                { pick(5) },
                Modifier.fillMaxWidth(),
            ) { fg -> Text(item.extra, style = AjlTheme.type.body.copy(fontSize = 15.sp), color = fg, modifier = Modifier.align(Alignment.CenterHorizontally)) }
        }
        if (!revealed) {
            Text("词尾滑到哪一段？", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            JudgedNote(if (picked == a) "✓ ${item.word}" else "正解 ${item.word}", picked == a, item.tags, item.rule)
            LineCard(item.line, audio, settings.ttsWorkerUrl)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JudgedNote(verdict: String, ok: Boolean, tags: List<Pair<String, com.animejapaneselab.nativeapp.ui.zougo.SegKind>>, rule: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Verdict(verdict, ok)
            tags.forEach { (t, k) -> KindChip(t, k) }
        }
        if (rule.isNotBlank()) Text(rule, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 23.sp), color = AjlTheme.colors.ink)
    }
}

// ------------------------------------------------------------------ A 分拣（左右划）

/** One card at a time: swipe (or tap) it left or right; it gets stamped with where it belongs. */
@Composable
internal fun SwipeSitting(
    step: KyStep.Swipe, key: String, eyebrow: String, settings: LabSettings,
    onClose: () -> Unit, onAnswer: (Boolean) -> Unit, onDone: Done, lastLabel: String, modifier: Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val feedback = LocalFeedbackEngine.current
    val audio = rememberLessonAudioController()
    val reduced = rememberReducedMotion()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val items = step.items
    var index by rememberSaveable(key) { mutableIntStateOf(0) }
    var picked by rememberSaveable(key) { mutableIntStateOf(-1) }
    var right by rememberSaveable(key) { mutableIntStateOf(0) }
    val missed = remember(key) { mutableStateListOf<Int>() }
    val drag = remember(key) { Animatable(0f) }
    val item = items.getOrNull(index) ?: return
    val revealed = picked >= 0
    val last = index == items.lastIndex

    fun pick(side: Int) {
        if (revealed) return
        val ok = side == item.answer
        feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
        onAnswer(ok)
        if (ok) right++ else missed.add(index)
        picked = side
        audio.speakText(item.word, settings.ttsWorkerUrl)
        scope.launch { drag.animateTo(if (reduced) 0f else with(density) { (if (side == 0) -18 else 18).dp.toPx() }, tween(220)) }
    }

    KySitting(
        eyebrow = eyebrow, title = step.title.ifBlank { "${step.left}？${step.right}？" }, counter = "${index + 1} / ${items.size}",
        progress = (index + if (revealed) 1 else 0).toFloat() / items.size, onClose = onClose,
        button = if (revealed) (if (last) lastLabel else "下一张") else null,
        onButton = {
            audio.stop()
            if (last) onDone(right, items.size, missed.map { items[it].let { s -> TsuzukuLine(s.word, true, s.result) } })
            else { index++; picked = -1; scope.launch { drag.snapTo(0f) } }
        },
        arrow = !last, modifier = modifier,
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("← ${step.left}", style = AjlTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.ink2)
            Spacer(Modifier.weight(1f))
            Text("${step.right} →", style = AjlTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.ink2)
        }
        val widthPx = with(density) { 300.dp.toPx() }
        Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
            MangaPanel(
                Modifier
                    .size(240.dp, 260.dp)
                    .offset { IntOffset(drag.value.roundToInt(), 0) }
                    .rotate(drag.value / widthPx * 20f)
                    .pointerInput(key, index, revealed) {
                        if (revealed) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val v = drag.value
                                if (abs(v) > widthPx * 0.28f) pick(if (v < 0) 0 else 1) else scope.launch { drag.animateTo(0f, tween(180)) }
                            },
                        ) { change, dx -> change.consume(); scope.launch { drag.snapTo(drag.value + dx) } }
                    },
            ) {
                Screentone(Modifier.align(Alignment.TopEnd).offset(x = 36.dp, y = (-20).dp).size(170.dp, 70.dp).rotate(-12f), color = work.tone(0.26f))
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.romaji, style = AjlTheme.type.meta.copy(fontSize = 14.sp), color = colors.ink3)
                    Text(item.word, style = AjlTheme.type.jpDisplay.copy(fontSize = 52.sp, lineHeight = 62.sp, fontWeight = FontWeight.Bold), color = colors.ink,
                        modifier = Modifier.clickableNoRipple({ audio.speakText(item.word, settings.ttsWorkerUrl) }))
                    AnimatedVisibility(revealed, enter = fadeIn()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(item.result, style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                            if (item.trap.isNotBlank()) Text(item.trap, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.bad)
                        }
                    }
                }
                if (revealed) {
                    val ok = picked == item.answer
                    Stamp(if (item.answer == 0) step.left else step.right, if (ok) colors.ok else colors.bad,
                        Modifier.align(if (item.answer == 0) Alignment.TopStart else Alignment.TopEnd).padding(14.dp))
                }
            }
        }
        if (!revealed) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickTile(TileState.Idle, { pick(0) }, Modifier.weight(1f)) { fg -> Text("← ${step.left}", style = AjlTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = fg, modifier = Modifier.align(Alignment.CenterHorizontally)) }
                PickTile(TileState.Idle, { pick(1) }, Modifier.weight(1f)) { fg -> Text("${step.right} →", style = AjlTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = fg, modifier = Modifier.align(Alignment.CenterHorizontally)) }
            }
            Text("往左右划卡片也行 · 还剩 ${items.size - index} 张", style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

/** A rubber stamp, slightly rotated, that drops in. */
@Composable
private fun Stamp(text: String, color: Color, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    var shown by remember { mutableStateOf(reduced) }
    LaunchedEffect(Unit) { shown = true }
    val s by animateFloatAsState(if (shown) 1f else 1.6f, tween(420), label = "stamp")
    Text(
        text,
        style = AjlTheme.type.jpTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        color = color,
        modifier = modifier
            .rotate(-8f)
            .alpha(if (shown) 1f else 0f)
            .size((56 * s).dp, (32 * s).dp)
            .border(AjlStroke.Ink, color, RoundedCornerShape(4.dp))
            .padding(top = 2.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

// ------------------------------------------------------------------ F 翻牌

/** A grid of cards: say い or な to yourself, then flip. The look-alikes get a work-colour frame. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FlipSitting(
    step: KyStep.Flip, key: String, eyebrow: String, settings: LabSettings,
    onClose: () -> Unit, onDone: Done, lastLabel: String, modifier: Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val audio = rememberLessonAudioController()
    val open = remember(key) { mutableStateListOf<Int>() }
    val traps = step.items.count { it.trap }
    val found = step.items.withIndex().count { (k, c) -> c.trap && k in open }
    KySitting(
        eyebrow = eyebrow, title = step.title.ifBlank { step.ask }, counter = "${open.size} / ${step.items.size}",
        progress = open.size.toFloat() / step.items.size, onClose = onClose,
        button = if (open.size == step.items.size) lastLabel else null,
        onButton = { audio.stop(); onDone(0, 0, emptyList()) },
        arrow = false, modifier = modifier,
    ) {
        Text(step.ask, style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 2) {
            step.items.forEachIndexed { k, c ->
                val o = k in open
                val bg by animateColorAsState(if (o) colors.surface else colors.sunken, tween(220), label = "flip")
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 118.dp)
                        .background(bg, RoundedCornerShape(4.dp))
                        .border(if (o && c.trap) 2.dp else AjlStroke.Ink, if (o && c.trap) work.accent else colors.ink, RoundedCornerShape(4.dp))
                        .clickableNoRipple({ if (!o) { open.add(k); audio.speakText(c.form.substringBefore(' '), settings.ttsWorkerUrl) } })
                        .semantics { role = Role.Button; contentDescription = "翻开 ${c.word}" }
                        .padding(10.dp),
                ) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(c.romaji, style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3)
                        Text(c.word, style = AjlTheme.type.jpDisplay.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold), color = colors.ink)
                        AnimatedVisibility(o, enter = fadeIn() + scaleIn(initialScale = 0.9f)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(c.form, style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = work.accent)
                                if (c.note.isNotBlank()) Text(c.note, style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.ink3)
                            }
                        }
                    }
                    if (o) Stamp(c.kind, if (c.trap) work.accent else colors.ink2, Modifier.align(Alignment.TopEnd).size(36.dp, 26.dp))
                }
            }
        }
        Text("伪装者 $found / $traps", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

// ------------------------------------------------------------------ D 叠积木

/** Stack 助動詞 blocks onto a stem in their only order; the joints change (たい → たく) and the meaning reads out. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StackSitting(
    step: KyStep.Stack, key: String, eyebrow: String, settings: LabSettings,
    onClose: () -> Unit, onAnswer: (Boolean) -> Unit, onDone: Done, lastLabel: String, modifier: Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val feedback = LocalFeedbackEngine.current
    val audio = rememberLessonAudioController()
    val st = step.stack
    var index by rememberSaveable(key) { mutableIntStateOf(0) }
    var ids by rememberSaveable(key) { mutableStateOf("") }
    var right by rememberSaveable(key) { mutableIntStateOf(0) }
    var slipped by rememberSaveable(key) { mutableStateOf(false) }
    val missed = remember(key) { mutableStateListOf<Int>() }
    val goal = st.goals.getOrNull(index) ?: return
    val last = index == st.goals.lastIndex
    val done = ids == goal.need
    val byId = st.blocks.associateBy { it.id }
    val lastRank = ids.lastOrNull()?.let { byId[it.toString()]?.rank } ?: 0
    val maxRank = st.blocks.maxOf { it.rank }
    // each block: its form here (mid before another block, past before た), and whether it changed
    val chain = ids.mapIndexed { n, c ->
        val b = byId.getValue(c.toString())
        val nxt = ids.getOrNull(n + 1)?.toString()
        val f = when {
            nxt == null -> b.fin
            nxt == "p" && b.past != null -> b.past
            else -> b.mid
        }
        Triple(f, f.first != b.fin.first, b.seam)
    }
    val word = st.base.first + chain.joinToString("") { it.first.first }
    LaunchedEffect(done) {
        if (done) {
            feedback?.emit(FeedbackEvent.AnswerCorrect(xp = 0))
            onAnswer(true)
            if (!slipped) right++
            audio.speakText(word, settings.ttsWorkerUrl)
        }
    }
    LaunchedEffect(ids) {
        if (!done && ids.length >= goal.need.length && !slipped) {
            slipped = true
            missed.add(index)
            feedback?.emit(FeedbackEvent.AnswerWrong)
            onAnswer(false)
        }
    }

    KySitting(
        eyebrow = eyebrow, title = step.title.ifBlank { "積み木" }, counter = "${index + 1} / ${st.goals.size}",
        progress = (index + if (done) 1 else 0).toFloat() / st.goals.size, onClose = onClose,
        button = if (done) (if (last) lastLabel else "下一个") else null,
        onButton = {
            audio.stop()
            if (last) onDone(right, st.goals.size, missed.map { st.goals[it].let { g -> TsuzukuLine(g.line.target, true, g.goal) } })
            else { index++; ids = ""; slipped = false }
        },
        arrow = !last, modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("拼出", style = AjlTheme.type.caption.copy(fontSize = 13.sp), color = colors.ink3)
            Text(goal.goal, style = AjlTheme.type.jpTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = colors.ink)
        }
        StagePanel {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StackBlock(st.base.first, st.base.second, st.baseNote, false, colors.sunken)
                    chain.forEach { (f, changed, seam) -> StackBlock(f.first, f.second, seam, changed, colors.surface) }
                }
                Text(word, style = AjlTheme.type.jpDisplay.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold), color = colors.ink,
                    modifier = Modifier.clickableNoRipple({ audio.speakText(word, settings.ttsWorkerUrl) }))
                Text(st.meanings[ids] ?: "", style = AjlTheme.type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = if (done) colors.ok else colors.ink)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            st.blocks.forEach { b ->
                val ok = !done && b.rank > lastRank && lastRank != maxRank
                PickTile(if (ok) TileState.Idle else TileState.Dim, { if (ok) ids += b.id }, Modifier.alpha(if (ok) 1f else 0.4f).widthIn(min = 92.dp)) { fg ->
                    Text(b.fin.first, style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = fg)
                    Text(b.gloss, style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = fg.copy(alpha = 0.7f))
                }
            }
        }
        if (!done) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("撤回一块", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2, modifier = Modifier.clickableNoRipple({ ids = ids.dropLast(1) }).padding(vertical = 10.dp))
                Text("清空", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2, modifier = Modifier.clickableNoRipple({ ids = "" }).padding(vertical = 10.dp))
            }
            if (slipped) Text("不是这个意思，撤回几块再试。", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.bad)
        } else {
            LineCard(goal.line, audio, settings.ttsWorkerUrl)
        }
    }
}

@Composable
private fun StackBlock(kana: String, romaji: String, note: String, changed: Boolean, bg: Color) {
    val colors = AjlTheme.colors
    Column(
        Modifier.background(bg, RoundedCornerShape(4.dp)).border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(romaji, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = if (changed) colors.info else colors.ink3)
        Text(kana, style = AjlTheme.type.jpBody.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold), color = if (changed) colors.info else colors.ink)
        Text(note, style = AjlTheme.type.caption.copy(fontSize = 9.sp), color = colors.ink3, maxLines = 1)
    }
}

// ------------------------------------------------------------------ E 连线

/** Tap a first half, then the second half it joins; a wrong pair says why it does not fit. */
@Composable
internal fun ConnectSitting(
    step: KyStep.Connect, key: String, eyebrow: String, settings: LabSettings,
    onClose: () -> Unit, onAnswer: (Boolean) -> Unit, onDone: Done, lastLabel: String, modifier: Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val feedback = LocalFeedbackEngine.current
    val audio = rememberLessonAudioController()
    val c = step.connect
    // done[left] = order number (1…)
    val done = remember(key) { mutableStateListOf<Int>().apply { repeat(c.left.size) { add(0) } } }
    val slipped = remember(key) { mutableStateListOf<Int>() }
    var sel by rememberSaveable(key) { mutableIntStateOf(0) }
    var msg by rememberSaveable(key) { mutableStateOf("") }
    var bad by rememberSaveable(key) { mutableStateOf(false) }
    val n = done.count { it > 0 }
    val all = n == c.left.size

    KySitting(
        eyebrow = eyebrow, title = step.title.ifBlank { "接得上的才是一句" }, counter = "$n / ${c.left.size}",
        progress = n.toFloat() / c.left.size, onClose = onClose,
        button = if (all) lastLabel else null,
        onButton = {
            audio.stop()
            onDone(c.left.size - slipped.size, c.left.size, slipped.map { TsuzukuLine(c.left[it].first + c.left[it].second, true, "") })
        },
        arrow = false, modifier = modifier,
    ) {
        Text("先点左边，再点能接上的右边", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.left.forEachIndexed { k, (pre, conn) ->
                    val d = done[k] > 0
                    PickTile(
                        when { d -> TileState.Dim; k == sel -> TileState.Current; else -> TileState.Idle },
                        { if (!d) { sel = k; msg = ""; bad = false } },
                        Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    ) { fg ->
                        Text(
                            buildAnnotatedString { append(pre); withStyle(SpanStyle(color = if (d || k == sel) fg else work.accent, fontWeight = FontWeight.Bold)) { append(conn) } },
                            style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 21.sp), color = fg,
                        )
                        if (d) Text("${done[k]}", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ok)
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.right.forEachIndexed { k, t ->
                    val owner = c.match[k]
                    val d = done[owner] > 0
                    PickTile(if (d) TileState.Dim else TileState.Idle, {
                        if (!d && sel >= 0) {
                            if (owner == sel) {
                                done[sel] = n + 1
                                feedback?.emit(FeedbackEvent.AnswerCorrect(xp = 0))
                                onAnswer(sel !in slipped)
                                msg = "✓ ${c.left[sel].first}${c.left[sel].second} $t"; bad = false
                                c.lines.getOrNull(sel)?.let { audio.speakText(it.ja, settings.ttsWorkerUrl) }
                                sel = done.indexOfFirst { it == 0 }
                            } else {
                                if (sel !in slipped) slipped.add(sel)
                                feedback?.emit(FeedbackEvent.AnswerWrong)
                                msg = "接不上：「${c.left[sel].second}」后面不是这个意思"; bad = true
                            }
                        }
                    }, Modifier.fillMaxWidth().heightIn(min = 64.dp)) { fg ->
                        Text(t, style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 21.sp), color = fg)
                        if (d) Text("${done[owner]}", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ok)
                    }
                }
            }
        }
        if (msg.isNotBlank()) Text(msg, style = AjlTheme.type.body.copy(fontSize = 14.sp), color = if (bad) colors.bad else colors.ok)
        if (all) c.lines.forEach { LineCard(it, audio, settings.ttsWorkerUrl) }
    }
}
