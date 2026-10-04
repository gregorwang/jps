package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.katsuyou.KyJudge
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * 模擬問題 / 毒を見抜く (第九巻). The passage is a collapsible card (open on the first question of a passage); under
 * it the question. Without types: pick the right one of the options, after which every option says what is wrong
 * with it. With types: one option is shown alone and you say which kind of poison it carries; the poisoned words
 * are underlined after. The sentences that are the evidence are quoted under the verdict.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JudgeSitting(
    items: List<KyJudge>,
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
    var srcOpen by rememberSaveable(key, index) { mutableStateOf(item.open) }
    val revealed = picked >= 0
    val last = index == items.lastIndex

    fun pick(i: Int) {
        if (picked >= 0) return
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
        button = if (revealed) (if (last) lastLabel else "下一题") else null,
        onButton = {
            audio.stop()
            if (last) onDone(right, items.size, missed.map { items[it].let { j -> TsuzukuLine(j.stem, true, j.options.getOrNull(if (j.claim) 0 else j.answer)?.why.orEmpty()) } })
            else { index++; picked = -1 }
        },
        arrow = !last,
        modifier = modifier,
    ) {
        // the passage
        Column(
            Modifier
                .fillMaxWidth()
                .border(AjlStroke.Hair, colors.line2, RoundedCornerShape(4.dp))
                .background(colors.surface, RoundedCornerShape(4.dp))
                .clickableNoRipple({ srcOpen = !srcOpen })
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("原文 ${item.srcLabel}", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3, modifier = Modifier.weight(1f))
                Text(if (srcOpen) "收起" else "展开", style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = work.accent)
            }
            if (srcOpen) {
                item.src.forEachIndexed { i, s ->
                    val evidence = revealed && i in item.ev
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(circled(i), style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = if (evidence) work.accent else colors.ink3)
                        Text(
                            s,
                            style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 23.sp, fontWeight = if (evidence) FontWeight.Bold else FontWeight.Normal),
                            color = colors.ink,
                        )
                    }
                }
            }
        }
        Text(item.stem, style = AjlTheme.type.body.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)

        if (item.claim) {
            val claim = item.options.first()
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(AjlStroke.Ink, colors.ink, RoundedCornerShape(4.dp))
                    .background(colors.surface, RoundedCornerShape(4.dp))
                    .padding(14.dp),
            ) {
                Text(
                    buildAnnotatedString {
                        val at = if (revealed && claim.poison.isNotBlank()) claim.text.indexOf(claim.poison) else -1
                        if (at >= 0) {
                            append(claim.text.substring(0, at))
                            withStyle(SpanStyle(color = colors.bad, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) { append(claim.poison) }
                            append(claim.text.substring(at + claim.poison.length))
                        } else append(claim.text)
                    },
                    style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
                    color = colors.ink,
                )
            }
            if (!revealed) Text("这个选项毒在哪？选一种", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.types.forEachIndexed { i, t ->
                    val state = when {
                        !revealed -> TileState.Idle
                        i == item.answer -> TileState.Right
                        i == picked -> TileState.Wrong
                        else -> TileState.Dim
                    }
                    PickTile(state, { pick(i) }) { fg ->
                        Text(t, style = AjlTheme.type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = fg)
                    }
                }
            }
            if (revealed) {
                val ok = picked == item.answer
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Verdict(if (ok) "✓ ${item.types[item.answer]}" else "你选 ${item.types[picked]} · 正解 ${item.types[item.answer]}", ok)
                    Text(claim.why, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 23.sp), color = colors.ink)
                    Evidence(item)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item.options.forEachIndexed { i, o ->
                    val state = when {
                        !revealed -> TileState.Idle
                        i == item.answer -> TileState.Right
                        i == picked -> TileState.Wrong
                        else -> TileState.Dim
                    }
                    PickTile(state, { pick(i) }, Modifier.fillMaxWidth()) { fg ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${i + 1}.", style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = fg.copy(alpha = 0.7f))
                            Text(o.text, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium), color = fg, modifier = Modifier.weight(1f))
                            if (revealed) Text(if (o.ok) "✓" else o.type, style = AjlTheme.type.meta.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = if (state == TileState.Right) fg else colors.bad)
                        }
                        if (revealed && o.why.isNotBlank()) {
                            Text(o.why, style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 19.sp), color = fg.copy(alpha = 0.85f), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            if (revealed) {
                val ok = picked == item.answer
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Verdict(if (ok) "✓ ${item.answer + 1}" else "你选 ${picked + 1} · 正解 ${item.answer + 1}", ok)
                    if (item.rule.isNotBlank()) Text(item.rule, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 23.sp), color = colors.ink)
                    Evidence(item)
                }
            }
        }
    }
}

/** 依据: the sentences of the passage the answer rests on, quoted so you need not scroll back. */
@Composable
private fun Evidence(item: KyJudge) {
    if (item.ev.isEmpty()) return
    val colors = AjlTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(AjlTheme.work.tone(0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .heightIn(min = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("依据 ${item.ev.joinToString("") { circled(it) }}", style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = AjlTheme.work.accent)
        item.ev.forEach { i ->
            item.src.getOrNull(i)?.let { Text("${circled(i)} $it", style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, lineHeight = 21.sp), color = colors.ink) }
        }
    }
}
