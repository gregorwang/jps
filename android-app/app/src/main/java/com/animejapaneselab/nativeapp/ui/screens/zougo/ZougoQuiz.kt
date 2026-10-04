package com.animejapaneselab.nativeapp.ui.screens.zougo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.OptionRow
import com.animejapaneselab.nativeapp.ui.design.OptionState
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.zougo.ZgQuestion
import com.animejapaneselab.nativeapp.ui.zougo.Zougo

/**
 * 造語's 小テスト / 練習: reading questions (two parts → romaji) and meaning questions (word →
 * Chinese). Every answer is judged once ([Zougo.answer] → record + 学習記録); [onDone] gets the
 * words answered wrong.
 */
@Composable
internal fun ZougoQuiz(
    questions: List<ZgQuestion>,
    eyebrow: String,
    title: String,
    settings: LabSettings,
    onClose: () -> Unit,
    onDone: (right: Int, missed: List<ZgQuestion>) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val feedback = LocalFeedbackEngine.current
    val audio = rememberLessonAudioController()
    var index by rememberSaveable(questions) { mutableIntStateOf(0) }
    var picked by rememberSaveable(questions) { mutableIntStateOf(-1) }
    var right by rememberSaveable(questions) { mutableIntStateOf(0) }
    val missed = remember(questions) { mutableStateListOf<ZgQuestion>() }
    val q = questions.getOrNull(index) ?: return
    val answered = picked >= 0
    val answer = when (q) {
        is ZgQuestion.Reading -> q.answer
        is ZgQuestion.Meaning -> q.answer
    }
    val options = when (q) {
        is ZgQuestion.Reading -> q.options
        is ZgQuestion.Meaning -> q.options
    }
    LaunchedEffect(index) {
        if (q is ZgQuestion.Meaning && settings.autoSpeak) audio.speakText(q.cell.word, settings.ttsWorkerUrl)
    }
    LaunchedEffect(index, answered) {
        if (answered && q is ZgQuestion.Reading && settings.autoSpeak) audio.speakText(q.item.word, settings.ttsWorkerUrl)
    }

    Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
        ZougoHeader(eyebrow, title, "${index + 1} / ${questions.size}", (index + if (answered) 1 else 0).toFloat() / questions.size, onClose)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (q) {
                is ZgQuestion.Reading -> FuseStage(q.item, answered, onSpeak = { audio.speakText(q.item.word, settings.ttsWorkerUrl) })
                is ZgQuestion.Meaning -> StagePanel(Modifier.height(170.dp)) {
                    WordHead(q.cell.romaji, q.cell.kana, q.cell.word, onSpeak = { audio.speakText(q.cell.word, settings.ttsWorkerUrl) }, size = 44.sp, center = true)
                    Text("是什么意思？", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = AjlTheme.colors.ink2, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEachIndexed { i, option ->
                    OptionRow(
                        text = option,
                        state = when {
                            !answered -> OptionState.Default
                            i == answer -> OptionState.Correct
                            i == picked -> OptionState.Wrong
                            else -> OptionState.Dimmed
                        },
                        leading = "ABCD".getOrNull(i)?.toString(),
                        enabled = !answered,
                        onClick = {
                            if (picked < 0) {
                                val ok = i == answer
                                feedback?.emit(if (ok) FeedbackEvent.AnswerCorrect(xp = 0) else FeedbackEvent.AnswerWrong)
                                Zougo.answer(context, q.word, ok)
                                if (ok) right++ else missed.add(q)
                                picked = i
                            }
                        },
                    )
                }
            }
            if (answered) {
                when (q) {
                    is ZgQuestion.Reading -> Text(q.item.rule, style = AjlTheme.type.body, color = AjlTheme.colors.ink)
                    is ZgQuestion.Meaning -> LineCard(q.cell.line, audio, settings.ttsWorkerUrl)
                }
            }
        }
        if (answered) {
            val last = index == questions.lastIndex
            InkButton(
                text = if (last) "完成" else "下一题",
                onClick = {
                    audio.stop()
                    if (last) onDone(right, missed.toList()) else { index++; picked = -1 }
                },
                trailingArrow = !last,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}
