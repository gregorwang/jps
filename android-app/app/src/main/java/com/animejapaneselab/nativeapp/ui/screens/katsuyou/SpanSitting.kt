package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.katsuyou.KySpan
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.screens.zougo.StagePanel
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlin.math.abs

/**
 * 括る / は的地盘 (第九巻): the sentence in blocks with one block fixed. Drag from it (or tap the block where
 * you think the stretch ends) and the blocks between light up; letting go judges. Then the right stretch is
 * bracketed ［ ］, the 补洞 (the noun put back into its clause) and the why show, and the sentence is read out.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpanSitting(
    items: List<KySpan>,
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
    var live by remember(key, index) { mutableIntStateOf(-1) }
    val missed = remember(key) { mutableStateListOf<Int>() }
    val item = items.getOrNull(index) ?: return
    val rects = remember(key, index) { HashMap<Int, Rect>() }
    val revealed = picked >= 0
    val last = index == items.lastIndex
    val start = item.dir == "start"
    val lo = if (start) 0 else item.anchor
    val hi = if (start) item.anchor - 1 else item.toks.lastIndex

    /** The block under [at]; blocks outside what can be picked are clamped when [clamp], else -1. */
    fun hit(at: Offset, clamp: Boolean): Int {
        val under = rects.entries.firstOrNull { it.value.contains(at) }?.key
            ?: rects.entries.minByOrNull { (_, r) -> abs(r.center.x - at.x) + abs(r.center.y - at.y) * 3 }?.key
            ?: return -1
        if (!clamp && under !in lo..hi) return -1
        return under.coerceIn(lo, hi)
    }

    fun judge(i: Int) {
        if (picked >= 0 || i < 0) return
        val ok = i == item.answer
        feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
        onAnswer(ok)
        if (ok) right++ else missed.add(index)
        picked = i
        audio.speakText(item.text, settings.ttsWorkerUrl)
    }

    /** First and last block of the stretch for a pick of [i]. */
    fun stretch(i: Int): IntRange = if (start) i until item.anchor else item.anchor..i
    val shown = when {
        revealed -> stretch(item.answer)
        live >= 0 -> stretch(live)
        else -> IntRange.EMPTY
    }
    val mine = if (revealed) stretch(picked) else IntRange.EMPTY
    val clause = stretch(item.answer).joinToString("") { item.toks[it].first }

    KySitting(
        eyebrow = eyebrow,
        title = title,
        counter = "${index + 1} / ${items.size}",
        progress = (index + if (revealed) 1 else 0).toFloat() / items.size,
        onClose = onClose,
        button = if (revealed) (if (last) lastLabel else "下一句") else null,
        onButton = {
            audio.stop()
            if (last) onDone(right, items.size, missed.map { items[it].let { s -> TsuzukuLine(s.text, true, s.why) } })
            else { index++; picked = -1 }
        },
        arrow = !last,
        modifier = modifier,
    ) {
        StagePanel {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (item.zh.isNotBlank()) Text(item.zh, style = AjlTheme.type.caption, color = colors.ink3)
                FlowRow(
                    modifier = Modifier
                        .pointerInput(item, revealed) {
                            if (!revealed) detectTapGestures { judge(hit(it, clamp = false)) }
                        }
                        .pointerInput(item, revealed) {
                            if (!revealed) detectDragGestures(
                                onDragStart = { live = hit(it, clamp = true) },
                                onDrag = { change, _ -> change.consume(); live = hit(change.position, clamp = true) },
                                onDragEnd = { judge(live) },
                                onDragCancel = { live = -1 },
                            )
                        },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    itemVerticalAlignment = Alignment.Bottom,
                ) {
                    item.toks.forEachIndexed { i, (text, ro) ->
                        val isAnchor = i == item.anchor
                        val inShown = i in shown
                        val wrongMine = revealed && i in mine && i !in shown
                        val missedMine = revealed && i in shown && i !in mine
                        val bg: Color = when {
                            isAnchor -> colors.ink
                            inShown -> work.tone(0.22f)
                            else -> colors.surface
                        }
                        val fg: Color = when {
                            isAnchor -> colors.bg
                            wrongMine -> colors.bad
                            revealed && !inShown -> colors.ink2
                            else -> colors.ink
                        }
                        val edge: Color = when {
                            isAnchor -> colors.ink
                            wrongMine -> colors.bad
                            inShown -> work.accent
                            revealed -> colors.line2
                            else -> colors.ink
                        }
                        Row(
                            Modifier
                                .onGloballyPositioned { rects[i] = Rect(it.positionInParent(), it.size.toSize()) }
                                .background(bg, RoundedCornerShape(4.dp))
                                .border(if (revealed && !inShown && !wrongMine) AjlStroke.Hair else AjlStroke.Ink, edge, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (revealed && i == shown.first) Bracket("［")
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(ro, style = AjlTheme.type.meta.copy(fontSize = 9.sp), color = fg.copy(alpha = 0.7f))
                                Text(
                                    text,
                                    style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, fontWeight = if (isAnchor || inShown) FontWeight.Bold else FontWeight.Medium),
                                    color = fg,
                                )
                                if (missedMine) Text("▲", style = AjlTheme.type.meta.copy(fontSize = 8.sp), color = work.accent)
                            }
                            if (revealed && i == shown.last) Bracket("］")
                        }
                    }
                }
            }
        }
        if (!revealed) {
            val anchor = item.toks[item.anchor].first
            val line = item.ask.ifBlank {
                if (start) "「$anchor」前面有一段在修饰它，从哪一块开始？拖过去，或点那一块。"
                else "「$anchor」管到哪一块为止？拖过去，或点最后一块。"
            }
            Text(line, style = AjlTheme.type.body, color = colors.ink2)
        } else {
            val ok = picked == item.answer
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Verdict(if (ok) "✓ 括对了：$clause" else "应该是：$clause", ok)
                if (item.gap.isNotBlank() || item.tag.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (item.tag.isNotBlank()) Text(item.tag, style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = work.accent)
                        if (item.gap.isNotBlank()) Text("补洞 → ${item.gap}", style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                    }
                }
                Text(item.why, style = AjlTheme.type.body, color = colors.ink)
            }
        }
    }
}

@Composable
private fun Bracket(text: String) {
    Text(text, style = AjlTheme.type.jpDisplay.copy(fontSize = 24.sp, fontWeight = FontWeight.Light), color = AjlTheme.work.accent, modifier = Modifier.padding(horizontal = 2.dp))
}
