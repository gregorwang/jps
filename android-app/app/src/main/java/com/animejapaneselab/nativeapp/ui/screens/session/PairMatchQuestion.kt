package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.PairMatchNode
import com.animejapaneselab.nativeapp.ui.design.OptionRow
import com.animejapaneselab.nativeapp.ui.design.OptionState
import kotlinx.coroutines.delay

/**
 * 配对 (not drawn; derived from OptionRow + the 选择 layout): Japanese on the left (tapping it
 * reads it aloud), meanings on the right in another order. Pick one from each side; a match
 * turns both ok and locks them, a miss nudges both and clears. All matched → submitted.
 */
@Composable
internal fun PairMatchQuestion(
    env: LessonQuestionEnv,
    node: PairMatchNode,
    autoSpeak: Boolean,
    modifier: Modifier = Modifier,
) {
    var matched by rememberSaveable(node.id) { mutableStateOf(listOf<String>()) }
    var pickJa by rememberSaveable(node.id) { mutableStateOf<String?>(null) }
    var pickMeaning by rememberSaveable(node.id) { mutableStateOf<String?>(null) }
    var miss by remember(node.id) { mutableStateOf<Pair<String, String>?>(null) }
    var submitted by remember(node.id) { mutableStateOf(false) }
    val meanings = remember(node.id) { node.pairs.reversed() }
    val latestSubmit by rememberUpdatedState(env.onSubmit)
    val latestWrong by rememberUpdatedState(env.onWrongTap)

    LaunchedEffect(node.id) {
        val first = node.pairs.firstOrNull()
        if (autoSpeak && first != null && first.audioText.isNotBlank()) env.onSpeak(first.audioText)
    }
    LaunchedEffect(pickJa, pickMeaning) {
        val ja = pickJa ?: return@LaunchedEffect
        val meaning = pickMeaning ?: return@LaunchedEffect
        if (ja == meaning) {
            matched = matched + ja
            pickJa = null
            pickMeaning = null
        } else {
            miss = ja to meaning
            latestWrong()
            delay(520)
            miss = null
            pickJa = null
            pickMeaning = null
        }
    }
    LaunchedEffect(matched.size) {
        if (!submitted && node.pairs.isNotEmpty() && matched.size == node.pairs.size && !env.answered) {
            submitted = true
            delay(300)
            latestSubmit(node.expectedAnswer)
        }
    }

    fun stateFor(id: String, picked: String?, missId: String?): OptionState = when {
        id in matched || env.answered -> OptionState.Correct
        id == missId -> OptionState.Wrong
        id == picked -> OptionState.Selected
        else -> OptionState.Default
    }

    LessonQuestionScaffold(env = env, modifier = modifier, heading = node.prompt.ifBlank { "把日语和意思连起来" }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            node.pairs.forEachIndexed { index, pair ->
                val meaning = meanings[index]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OptionRow(
                        text = pair.right,
                        state = stateFor(pair.id, pickJa, miss?.first),
                        onClick = {
                            if (miss == null && pair.id !in matched) {
                                pickJa = pair.id
                                if (pair.audioText.isNotBlank()) env.onSpeak(pair.audioText)
                            }
                        },
                        japanese = true,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    OptionRow(
                        text = meaning.left,
                        state = stateFor(meaning.id, pickMeaning, miss?.second),
                        onClick = { if (miss == null && meaning.id !in matched) pickMeaning = meaning.id },
                        japanese = LessonRules.looksJapanese(meaning.left),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}
