package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.katsuyou.KySpot
import com.animejapaneselab.nativeapp.ui.screens.session.TsuzukuLine
import com.animejapaneselab.nativeapp.ui.screens.zougo.LineCard
import com.animejapaneselab.nativeapp.ui.screens.zougo.StagePanel
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * 找错: the sentence in pieces, one piece is wrong. Tap the one you think it is; then the wrong piece is
 * struck, what it should be sits over it, and the why (and an anime line that uses it right) shows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpotSitting(
    items: List<KySpot>,
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
        button = if (revealed) (if (last) lastLabel else "下一句") else null,
        onButton = {
            audio.stop()
            if (last) onDone(right, items.size, missed.map { items[it].let { s -> TsuzukuLine(s.fixed, true, s.why) } })
            else { index++; picked = -1 }
        },
        arrow = !last,
        modifier = modifier,
    ) {
        StagePanel {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.zh.isNotBlank()) Text("想说：${item.zh}", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.Bottom) {
                    item.toks.forEachIndexed { i, (text, ro) ->
                        val isBad = i == item.bad
                        val state = when {
                            !revealed -> TileState.Idle
                            isBad -> TileState.Wrong
                            i == picked -> TileState.Wrong
                            else -> TileState.Dim
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedVisibility(revealed && isBad, enter = fadeIn() + scaleIn(initialScale = 0.9f)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 4.dp)) {
                                    Text(item.fix.second, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = work.accent)
                                    Text(item.fix.first.ifBlank { "去掉" }, style = AjlTheme.type.jpBody.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = work.accent)
                                }
                            }
                            PickTile(state, {
                                if (picked < 0) {
                                    val ok = isBad
                                    feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
                                    onAnswer(ok)
                                    if (ok) right++ else missed.add(index)
                                    picked = i
                                    audio.speakText(item.fixed, settings.ttsWorkerUrl)
                                }
                            }) { fg ->
                                Text(ro, style = AjlTheme.type.meta.copy(fontSize = 10.sp), color = fg.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text(
                                    text,
                                    style = AjlTheme.type.jpBody.copy(
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textDecoration = if (revealed && isBad) TextDecoration.LineThrough else null,
                                    ),
                                    color = fg,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )
                            }
                        }
                    }
                }
            }
        }
        if (!revealed) {
            Text("哪一块用错了？点它", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            val ok = picked == item.bad
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val fixLabel = item.fix.first.ifBlank { "去掉" }
                Verdict(if (ok) "✓ 找到了：${item.toks[item.bad].first} → $fixLabel" else "错的是「${item.toks[item.bad].first}」 → $fixLabel", ok)
                Text(item.fixed, style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                Text(item.why, style = AjlTheme.type.body, color = colors.ink)
            }
            item.line?.let { LineCard(it, audio, settings.ttsWorkerUrl) }
        }
    }
}
