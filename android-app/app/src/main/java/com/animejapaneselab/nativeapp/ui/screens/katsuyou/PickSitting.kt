package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.voicepack.LineVoicePill
import com.animejapaneselab.nativeapp.ui.voicepack.lineCue
import com.animejapaneselab.nativeapp.ui.zougo.ZgLine
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.katsuyou.KyOpt
import com.animejapaneselab.nativeapp.ui.katsuyou.KyPick
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.screens.zougo.KindChip
import com.animejapaneselab.nativeapp.ui.screens.zougo.LineCard
import com.animejapaneselab.nativeapp.ui.screens.zougo.SegText
import com.animejapaneselab.nativeapp.ui.screens.zougo.StagePanel
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.zougo.ZgSeg

/**
 * One answer out of a few. What sits on top is [KyPick.head] (the line with a gap, 縮約 short → full, a time
 * line, a context, a speaker); the options are laid out by [KyPick.layout]. After the pick, 换词 / 敬语 let
 * you tap the other options to see the line change; 接续 / 换个人说 show every option's why.
 */
@Composable
internal fun PickSitting(
    items: List<KyPick>,
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
    val feedback = LocalFeedbackEngine.current
    val audio = rememberLessonAudioController()
    var index by rememberSaveable(key) { mutableIntStateOf(0) }
    var picked by rememberSaveable(key) { mutableIntStateOf(-1) }
    var cur by rememberSaveable(key) { mutableIntStateOf(-1) }
    var right by rememberSaveable(key) { mutableIntStateOf(0) }
    val missed = remember(key) { mutableStateListOf<Int>() }
    val item = items.getOrNull(index) ?: return
    val revealed = picked >= 0
    val last = index == items.lastIndex
    val explore = item.options.any { it.line != null }
    val shown = item.options.getOrNull(cur) ?: item.right

    fun pick(i: Int) {
        if (revealed) {
            if (explore) { cur = i; item.options[i].line?.let { audio.speakText(it.ja, settings.ttsWorkerUrl) } }
            return
        }
        val ok = i == item.answer || item.options[i].mark == "ok"
        feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
        onAnswer(ok)
        if (ok) right++ else missed.add(index)
        picked = i
        cur = item.answer
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
            if (last) onDone(right, items.size, missed.map { items[it].let { p -> TsuzukuLine(p.right.text, true, p.right.romaji) } })
            else { index++; picked = -1; cur = -1 }
        },
        arrow = !last,
        modifier = modifier,
    ) {
        if (item.head == "show" && item.context.isNotBlank()) ContextStrip(item.context)
        PickHead(item, revealed, shown, audio, settings.ttsWorkerUrl)
        if (revealed) item.split?.let { SplitPanel(it) }
        if (!revealed && item.ask.isNotBlank() && !item.askOnly) {
            Text(item.ask, style = AjlTheme.type.body.copy(fontSize = 14.sp), color = AjlTheme.colors.ink2, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        when {
            item.options.any { it.glyph.isNotBlank() } -> GlyphOptions(item, picked, ::pick)
            item.layout == "floors" -> FloorOptions(item, picked, ::pick)
            item.layout == "ends" -> EndsOptions(item, picked, ::pick)
            else -> PickOptions(item, picked, cur, revealed, ::pick)
        }
        if (revealed) {
            if (item.fills.isNotEmpty()) FillPanel(item.line?.ja ?: item.ask, item.fills)
            PickExplain(item, picked, shown)
            item.table?.let { DiffTablePanel(it) }
            when {
                item.layout == "ladder" -> if (shown != item.right) shown.line?.let { LineCard(it, audio, settings.ttsWorkerUrl) }
                item.head == "timeline" || item.head == "speaker" ->
                    item.line?.let { LineCard(it, audio, settings.ttsWorkerUrl) }
            }
        }
    }
}

@Composable
private fun PickHead(item: KyPick, revealed: Boolean, shown: KyOpt, audio: LessonAudioController, tts: String) {
    val colors = AjlTheme.colors
    val line = item.line
    when (item.head) {
        "shuku" -> {
            if (line != null) LineCard(line, audio, tts)
            StagePanel {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    SegText(item.shortRomaji, 15.sp, lit = revealed, base = colors.ink2)
                    KanaLine(item.short, 30, revealed)
                    Text("↓", style = AjlTheme.type.meta.copy(fontSize = 18.sp), color = colors.ink3)
                    AnimatedVisibility(revealed, enter = fadeIn() + scaleIn(initialScale = 0.92f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SegText(item.fullRomaji, 15.sp, lit = true)
                            KanaLine(item.full, 30, true)
                        }
                    }
                    if (!revealed) Text("还原成完整的说法？", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        "timeline" -> TimelinePanel(item)
        "speaker" -> SpeakerPanel(item)
        "show" -> if (line != null) LineCard(line, audio, tts) else AskPanel(item.ask)
        "listen" -> if (line != null) {
            if (revealed) LineCard(line, audio, tts) else ListenPanel(line, audio, tts)
        }
        else -> if (line != null) {
            // 换词: another option swaps its own version of the line in; 敬语阶梯 keeps the question and shows the level's line below
            val variant = revealed && item.layout != "ladder" && shown != item.right && shown.line != null
            val l = if (variant) shown.line!! else line
            SlotLine(
                line = l,
                // the right one shows the line's own surface (そう in 話したいそうよ under the option そうだ)
                fill = if (shown == item.right || item.layout == "ladder") line.target else shown.text,
                filled = revealed,
                audio = audio,
                ttsWorkerUrl = tts,
                romaji = l.romaji,
                zh = l.zh,
                label = if (variant) "换了一个词" else null,
                context = item.context,
            )
        }
    }
}

/** No line, only the sentence to put into Japanese (an English / Chinese 原句): it stays up after the pick. */
@Composable
private fun AskPanel(ask: String) {
    val colors = AjlTheme.colors
    val english = ask.none { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
    StagePanel {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (english) "原句 · 英语" else "原句 · 中文", style = AjlTheme.type.meta.copy(fontSize = 11.sp, letterSpacing = 0.6.sp), color = colors.ink3)
            Text(ask, style = AjlTheme.type.body.copy(fontSize = 17.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium), color = colors.ink)
            Text("换成日语，哪句最自然？", style = AjlTheme.type.caption, color = colors.ink2)
        }
    }
}

private val KyPick.askOnly: Boolean get() = head == "show" && line == null

/** Whole sentences as options (ASK, 换个人说): name them by letter in the verdict and on the tiles. */
private val KyPick.lettered: Boolean get() = askOnly || head == "speaker"

/** Options that are all Chinese (no kana): set in the body face, not the Japanese serif. */
private val KyPick.chineseOptions: Boolean
    get() = options.all { o -> o.text.none { Character.UnicodeScript.of(it.code) in setOf(Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA) } }

/** 听原声: only the voice (it plays once by itself, switch 原声 / エミリア / TTS on the pill); the words show after the pick. */
@Composable
private fun ListenPanel(line: ZgLine, audio: LessonAudioController, tts: String) {
    val colors = AjlTheme.colors
    val context = LocalContext.current
    LaunchedEffect(line.ja) {
        val cue = lineCue(context, line.ja, line.audioUrl)
        if (!audio.isSounding(cue)) audio.toggle(cue, tts)
    }
    StagePanel {
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LineVoicePill(line.ja, line.audioUrl, audio, tts)
            Text("猜语气听原声最准：左右滑可以换", style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
        }
    }
}

/** Kana in their kind's colour; a [SegKind.Gone] piece is dotted before and struck after. */
@Composable
private fun KanaLine(segs: List<ZgSeg>, size: Int, lit: Boolean) {
    SegText(segs, size.sp, lit, base = AjlTheme.colors.ink)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickOptions(item: KyPick, picked: Int, cur: Int, revealed: Boolean, onPick: (Int) -> Unit) {
    val colors = AjlTheme.colors
    fun stateOf(i: Int): TileState = when {
        !revealed -> TileState.Idle
        item.layout == "chips" || item.layout == "ladder" -> when {
            i == cur -> TileState.Current
            i == picked && picked != item.answer && item.options[picked].mark != "ok" -> TileState.Wrong
            else -> TileState.Idle
        }
        i == item.answer -> TileState.Right
        i == picked -> if (item.options[i].mark == "ok") TileState.Idle else TileState.Wrong
        else -> TileState.Dim
    }
    when (item.layout) {
        "chips" -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item.options.forEachIndexed { i, o ->
                Box {
                    PickTile(stateOf(i), { onPick(i) }) { fg ->
                        if (o.romaji.isNotBlank()) Text(o.romaji, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = fg.copy(alpha = 0.7f))
                        Text(o.text, style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold), color = fg)
                    }
                    if (revealed && i == item.answer) OrigBadge(Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-8).dp))
                }
            }
        }
        "columns" -> {
            val groups = item.options.map { it.group }.distinct()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { g ->
                    val has = revealed && item.right.group == g
                    Column(
                        Modifier
                            .weight(1f)
                            .background(if (has) AjlTheme.work.tone(0.18f) else colors.surface, RoundedCornerShape(4.dp))
                            .border(AjlStroke.Hair, colors.line2, RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(g, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink2, maxLines = 1)
                        item.options.forEachIndexed { i, o ->
                            if (o.group == g) PickTile(stateOf(i), { onPick(i) }, Modifier.fillMaxWidth()) { fg ->
                                Text(o.text, style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), color = fg, modifier = Modifier.align(Alignment.CenterHorizontally))
                            }
                        }
                    }
                }
            }
        }
        "ladder" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val n = item.options.size
            // よろしく…申し上げます: a long rung gets its label on a line of its own and the whole width for the words
            val long = item.options.any { it.text.length > 8 }
            val step = if (long) 12 else 22
            item.options.forEachIndexed { i, o ->
                PickTile(stateOf(i), { onPick(i) }, Modifier.fillMaxWidth().padding(start = (i * step).dp, end = ((n - 1 - i) * step).dp)) { fg ->
                    if (long) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(o.group, style = AjlTheme.type.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = fg)
                            if (o.why.isNotBlank()) Text(o.why, style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = fg.copy(alpha = 0.7f))
                        }
                        Text(o.romaji, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = fg.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
                        Text(o.text, style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = fg)
                    } else Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.width(76.dp)) {
                            Text(o.group, style = AjlTheme.type.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = fg)
                            if (o.why.isNotBlank()) Text(o.why, style = AjlTheme.type.caption.copy(fontSize = 10.sp), color = fg.copy(alpha = 0.7f))
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(o.romaji, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = fg.copy(alpha = 0.7f))
                            Text(o.text, style = AjlTheme.type.jpBody.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold), color = fg)
                        }
                    }
                }
            }
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item.options.forEachIndexed { i, o ->
                PickTile(stateOf(i), { onPick(i) }, Modifier.fillMaxWidth()) { fg ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val face = if (item.chineseOptions) AjlTheme.type.body.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold) else AjlTheme.type.jpBody.copy(fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Text(o.text, style = face, color = fg, modifier = Modifier.weight(1f))
                        Text(markLabel(o, i, item, revealed), style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = markColor(o, i, item, revealed, fg))
                    }
                    if (revealed && o.why.isNotBlank()) {
                        Text(o.why, style = AjlTheme.type.caption, color = fg.copy(alpha = 0.85f), modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

private fun markLabel(o: KyOpt, i: Int, item: KyPick, revealed: Boolean): String = when {
    !revealed -> "ABCD".getOrNull(i)?.toString().orEmpty()
    item.lettered -> "ABCD".getOrNull(i)?.toString().orEmpty() + when {
        o.mark == "ok" -> " · 也说得通"
        o.mark == "no" -> " · 不行"
        i == item.answer -> " ✓"
        else -> ""
    }
    o.mark == "ok" -> "也说得通"
    o.mark == "no" -> "不行"
    i == item.answer -> if (item.line?.fromAnime == true && item.head in setOf("line", "context", "listen")) "原作 ✓" else "✓"
    else -> ""
}

@Composable
private fun markColor(o: KyOpt, i: Int, item: KyPick, revealed: Boolean, fg: androidx.compose.ui.graphics.Color) = when {
    !revealed -> AjlTheme.colors.ink3
    i == item.answer -> fg
    o.mark == "ok" -> AjlTheme.colors.info
    else -> AjlTheme.colors.bad
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickExplain(item: KyPick, picked: Int, shown: KyOpt) {
    val colors = AjlTheme.colors
    val ok = picked == item.answer
    val soso = !ok && item.options[picked].mark == "ok"
    // 原句 → which Japanese: the options are whole sentences, so name them by letter instead of copying both out again
    fun name(i: Int) = if (item.lettered) "ABCD".getOrNull(i)?.toString().orEmpty() else item.options[i].text
    val verdict = when {
        ok -> item.verdictRight.ifBlank { "✓ ${name(item.answer)}" }
        soso -> "也说得通 · ${if (item.line?.fromAnime == true) "原作用的是" else "最自然的是"} ${name(item.answer)}"
        else -> item.verdictWrong.ifBlank { "你选 ${name(picked)} · 正解 ${name(item.answer)}" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Verdict(verdict, ok || soso)
            item.tags.forEach { (t, k) -> KindChip(t, k) }
        }
        // rows show every why on the options themselves; the others explain the one being looked at.
        val note = if (item.layout == "rows" || item.layout == "floors" || item.layout == "ends") "" else shown.why.takeIf { item.layout != "ladder" }.orEmpty()
        if (note.isNotBlank()) Text(note, style = AjlTheme.type.body, color = colors.ink)
        if (item.rule.isNotBlank()) Text(item.rule, style = AjlTheme.type.body, color = if (note.isBlank()) colors.ink else colors.ink2)
    }
}

@Composable
private fun OrigBadge(modifier: Modifier = Modifier) {
    val work = AjlTheme.work
    Text(
        "原作",
        style = AjlTheme.type.caption.copy(fontSize = 10.sp, lineHeight = 14.sp),
        color = work.accent,
        modifier = modifier
            .background(AjlTheme.colors.bg, RoundedCornerShape(2.dp))
            .border(AjlStroke.Ink, work.accent, RoundedCornerShape(2.dp))
            .padding(horizontal = 4.dp),
    )
}

/** 时间轴: 过去 — 现在 — 以后 with dots (a moment), bars (a stretch), a flag (for later) and a cross (no way back). */
@Composable
private fun TimelinePanel(item: KyPick) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    MangaPanel(Modifier.fillMaxWidth().height(150.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 18.dp)) {
            val w = maxWidth
            val axisY = 84.dp
            Box(Modifier.offset(y = axisY).fillMaxWidth().height(2.dp).background(colors.line2))
            Box(Modifier.offset(x = w * 0.5f - 1.dp, y = axisY - 10.dp).size(2.dp, 22.dp).background(colors.ink))
            Text("过去", style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.ink3, modifier = Modifier.offset(y = axisY + 18.dp))
            Text("现在", style = AjlTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = colors.ink, modifier = Modifier.offset(x = w * 0.5f - 12.dp, y = axisY + 18.dp))
            Text("以后", style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.ink3, modifier = Modifier.align(Alignment.TopEnd).offset(y = axisY + 18.dp))
            item.marks.forEach { m ->
                val x = w * (m.at / 100f)
                when (m.kind) {
                    "bar" -> {
                        val x2 = w * (m.until / 100f)
                        Box(Modifier.offset(x = x, y = axisY - 5.dp).size(x2 - x, 12.dp).background(work.tone(0.22f)).border(AjlStroke.Ink, work.accent))
                        MarkLabel(m.label, x + (x2 - x) / 2 - 30.dp, axisY - 34.dp)
                    }
                    "flag" -> {
                        Box(Modifier.offset(x = x, y = axisY - 28.dp).size(2.dp, 30.dp).background(work.accent))
                        Box(Modifier.offset(x = x + 2.dp, y = axisY - 28.dp).size(14.dp, 10.dp).background(work.accent))
                        MarkLabel(m.label, x - 40.dp, axisY - 52.dp)
                    }
                    "cross" -> {
                        Box(Modifier.offset(x = x - 10.dp, y = axisY - 9.dp).size(20.dp).background(colors.bad, CircleShape), contentAlignment = Alignment.Center) {
                            Text("×", style = AjlTheme.type.meta.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold), color = colors.bg)
                        }
                        MarkLabel(m.label, x - 34.dp, axisY - 36.dp)
                    }
                    else -> {
                        Box(Modifier.offset(x = x - 7.dp, y = axisY - 6.dp).size(14.dp).background(work.accent, CircleShape))
                        MarkLabel(m.label, x - 30.dp, axisY - 34.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkLabel(text: String, x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp) {
    Text(text, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = AjlTheme.colors.ink, maxLines = 1, modifier = Modifier.offset(x = x.coerceAtLeast(0.dp), y = y))
}

/** 换个人说: who is talking, to whom, and what they want to say. */
@Composable
private fun SpeakerPanel(item: KyPick) {
    val colors = AjlTheme.colors
    StagePanel {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).border(AjlStroke.Ink, colors.ink, CircleShape).background(colors.bg, CircleShape), contentAlignment = Alignment.Center) {
                Text(item.who.take(1), style = AjlTheme.type.jpDisplay.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = colors.ink)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.who, style = AjlTheme.type.jpTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), color = colors.ink)
                Text(item.rel, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
                Text(item.context, style = AjlTheme.type.body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = AjlTheme.work.accent)
            }
        }
    }
}
