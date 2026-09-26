package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.data.SingleChoiceNode
import com.animejapaneselab.nativeapp.ui.design.EmphasisText
import com.animejapaneselab.nativeapp.ui.design.OptionRow
import com.animejapaneselab.nativeapp.ui.design.OptionState
import com.animejapaneselab.nativeapp.ui.design.ToolPanel
import com.animejapaneselab.nativeapp.ui.design.emphasisRanges
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/** Option state before / after answering; shared by 选择, 填空 and 自评. */
internal fun optionStateFor(option: String, pending: String?, committed: String?, correctAnswer: String?): OptionState = when {
    committed == null -> if (option == pending) OptionState.Selected else OptionState.Default
    correctAnswer != null && option == correctAnswer -> OptionState.Correct
    option == committed -> OptionState.Wrong
    else -> OptionState.Dimmed
}

/**
 * 选择 (XCorrect): the quoted line in a tool frame with the prompt's 「keyword」 getting 着重号
 * once answered correctly, numbered options, one ink 检查.
 */
@Composable
internal fun ChoiceQuestion(
    env: LessonQuestionEnv,
    node: SingleChoiceNode,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    var pending by rememberSaveable(node.id) { mutableStateOf<String?>(null) }
    var checking by remember(node.id) { mutableStateOf(false) }
    val committed = env.feedback?.selected
    val keyword = remember(node.prompt) { LessonRules.quotedKeyword(node.prompt) }
    LessonQuestionScaffold(
        env = env,
        modifier = modifier,
        bottomBar = {
            LessonActionBar(
                primaryLabel = "检查",
                enabled = pending != null && !checking,
                onPrimary = {
                    val choice = pending ?: return@LessonActionBar
                    if (!checking) {
                        checking = true
                        env.onSubmit(choice)
                    }
                },
            )
        },
    ) {
        val body = node.body?.takeIf { it.isNotBlank() }
        if (body != null) {
            ToolPanel(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EmphasisText(
                        text = body,
                        emphasis = if (keyword != null) emphasisRanges(body, keyword) else emptyList(),
                        emphasisVisible = env.feedback?.correct == true,
                        style = AjlTheme.type.jpBody.copy(fontSize = 22.sp, lineHeight = 36.sp),
                        color = colors.ink,
                    )
                    if (node.audio != PromptAudio.None) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f))
                            AudioChip(node.audio, env.playback, env.onPlay)
                        }
                    }
                }
            }
        } else if (node.audio != PromptAudio.None) {
            AudioChip(node.audio, env.playback, env.onPlay)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            node.choices.forEachIndexed { index, choice ->
                OptionRow(
                    text = choice,
                    state = optionStateFor(choice, pending, committed, node.answer),
                    onClick = { if (!env.answered) pending = choice },
                    japanese = LessonRules.looksJapanese(choice),
                    leading = "${index + 1}",
                )
            }
        }
    }
}
