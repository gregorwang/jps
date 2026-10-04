package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.katsuyou.KyPassage
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/** ①②③… for the [i]th (0-based) sentence of a passage. */
internal fun circled(i: Int): String = if (i in 0..19) (0x2460 + i).toChar().toString() else "${i + 1}"

/**
 * 主张はどこ (第九巻): a passage in numbered sentences and one question about it (which one is the author's claim,
 * the target, the concession, the one that tells you the mood changed). Tap a sentence; then every sentence gets
 * its job (一般论 / 让步 / 转折 / 主张 …), the signal words are bold and the Chinese shows under each.
 */
@Composable
internal fun PassageSitting(
    items: List<KyPassage>,
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
    val revealed = picked >= 0
    val last = index == items.lastIndex

    KySitting(
        eyebrow = eyebrow,
        title = title,
        counter = "${index + 1} / ${items.size}",
        progress = (index + if (revealed) 1 else 0).toFloat() / items.size,
        onClose = onClose,
        button = if (revealed) (if (last) lastLabel else "下一题") else null,
        onButton = {
            audio.stop()
            if (last) onDone(right, items.size, missed.map { items[it].let { p -> TsuzukuLine(p.sents[p.answer].ja, true, p.why) } })
            else { index++; picked = -1 }
        },
        arrow = !last,
        modifier = modifier,
    ) {
        Text(item.ask, style = AjlTheme.type.body.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item.sents.forEachIndexed { i, s ->
                val state = when {
                    !revealed -> TileState.Idle
                    i == item.answer -> TileState.Right
                    i == picked -> TileState.Wrong
                    else -> TileState.Dim
                }
                PickTile(state, {
                    if (picked < 0) {
                        val ok = i == item.answer
                        feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
                        onAnswer(ok)
                        if (ok) right++ else missed.add(index)
                        picked = i
                    }
                }, Modifier.fillMaxWidth()) { fg ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(circled(i), style = AjlTheme.type.meta.copy(fontSize = 14.sp), color = fg.copy(alpha = 0.8f), modifier = Modifier.width(22.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (s.ro.isNotBlank()) Text(s.ro, style = AjlTheme.type.meta.copy(fontSize = 10.sp, lineHeight = 14.sp), color = fg.copy(alpha = 0.7f))
                            Text(
                                markedText(s.ja, if (revealed) s.mark else "", if (state == TileState.Right) fg else work.accent),
                                style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 24.sp),
                                color = fg,
                            )
                            if (revealed && s.zh.isNotBlank()) {
                                Text(s.zh, style = AjlTheme.type.body.copy(fontSize = 12.sp, lineHeight = 17.sp), color = fg.copy(alpha = 0.8f), modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        if (revealed && s.role.isNotBlank()) {
                            Text(s.role, style = AjlTheme.type.meta.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = if (state == TileState.Right) fg else work.accent)
                        }
                    }
                }
            }
        }
        if (revealed) {
            val ok = picked == item.answer
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Verdict(if (ok) "✓ ${circled(item.answer)}" else "正解は ${circled(item.answer)}", ok)
                if (item.why.isNotBlank()) Text(item.why, style = AjlTheme.type.body, color = colors.ink)
                if (item.rule.isNotBlank()) Text(item.rule, style = AjlTheme.type.body, color = colors.ink2)
            }
        }
    }
}

/** [text] with every occurrence of the words in [marks] (separated by "|") bold, underlined and in [color]. */
@Composable
internal fun markedText(text: String, marks: String, color: androidx.compose.ui.graphics.Color) = buildAnnotatedString {
    val words = marks.split('|').filter { it.isNotBlank() }
    var i = 0
    while (i < text.length) {
        val w = words.firstOrNull { text.startsWith(it, i) }
        if (w != null) {
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) { append(w) }
            i += w.length
        } else {
            append(text[i])
            i++
        }
    }
}
