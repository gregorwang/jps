package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.ClozeNode
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.ui.design.CharacterRef
import com.animejapaneselab.nativeapp.ui.design.DialogueBox
import com.animejapaneselab.nativeapp.ui.design.TypewriterState
import com.animejapaneselab.nativeapp.ui.design.WordTile
import com.animejapaneselab.nativeapp.ui.design.rememberTypewriterState
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/** The scene shot with the 会話窓 overlapping its lower edge (V3LessonScene). */
@Composable
internal fun SceneDialogue(
    character: CharacterRef?,
    speaker: String?,
    audio: PromptAudio,
    env: LessonQuestionEnv,
    typewriter: TypewriterState,
    modifier: Modifier = Modifier,
    panelHeight: Dp = 190.dp,
    body: @Composable ColumnScope.(visible: String) -> Unit,
) {
    Box(modifier.fillMaxWidth()) {
        ScenePanel(character, audio, env.playback, env.onPlay, height = panelHeight)
        DialogueBox(
            speaker = speaker,
            state = typewriter,
            modifier = Modifier.padding(top = panelHeight - 40.dp),
            onAdvance = {},
            body = body,
        )
    }
}

/** Dialogue-line text style (21sp serif). */
@Composable
internal fun dialogueStyle() = AjlTheme.type.jpBody.copy(fontSize = 21.sp, lineHeight = 34.sp)

/** The dashed empty gap in a line (「お姉ちゃん、そろそろ＿＿＿。」). */
@Composable
internal fun BlankGap(modifier: Modifier = Modifier, width: Dp = 64.dp) {
    val line2 = AjlTheme.colors.line2
    Box(
        modifier
            .width(width)
            .height(30.dp)
            .drawBehind {
                val y = size.height - 3.dp.toPx()
                drawLine(
                    line2,
                    Offset(0f, y),
                    Offset(size.width, y),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                )
            },
    )
}

/** A placed piece after checking: ok / bad outline and soft fill, no shadow. */
@Composable
internal fun VerdictChip(text: String, correct: Boolean, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    val shape = AjlShape.Tile
    Box(
        modifier
            .height(40.dp)
            .clip(shape)
            .background(if (correct) colors.okSoft else colors.badSoft)
            .border(AjlStroke.Ink, if (correct) colors.ok else colors.bad, shape)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AjlTheme.type.jpBody, color = if (correct) colors.ok else colors.bad, maxLines = 1)
    }
}

/**
 * 填空 — the line sits in the 会話窓 with a dashed gap; the choices are word tiles below that fly
 * into the gap (a second choice sends the first one back). One ink 检查.
 */
@Composable
internal fun ClozeQuestion(
    env: LessonQuestionEnv,
    node: ClozeNode,
    modifier: Modifier = Modifier,
) {
    val reduced = rememberReducedMotion()
    val flight = rememberTileFlightState(node.id, animate = !reduced)
    var chosen by rememberSaveable(node.id) { mutableStateOf<Int?>(null) }
    var checking by remember(node.id) { mutableStateOf(false) }
    val gap = "　"
    val typewriter = rememberTypewriterState(node.before + gap + node.after)
    val choices = node.choices.map { it.value }

    fun place(index: Int) {
        if (env.answered || flight.busy(index)) return
        chosen?.let { old ->
            if (old == index) return
            flight.launchBack(old, choices[old])
        }
        chosen = index
        flight.launchIn(index, choices[index])
    }

    fun takeBack() {
        val current = chosen ?: return
        if (env.answered || flight.busy(current)) return
        flight.launchBack(current, choices[current])
        chosen = null
    }

    LessonQuestionScaffold(
        env = env,
        modifier = modifier,
        bottomBar = {
            LessonActionBar(
                primaryLabel = "检查",
                enabled = chosen != null && !checking,
                onPrimary = {
                    val index = chosen ?: return@LessonActionBar
                    if (!checking) {
                        checking = true
                        env.onSubmit(choices[index])
                    }
                },
                quietLabel = "跳过".takeIf { env.onSkip != null },
                onQuiet = env.onSkip,
                quietEnabled = !checking,
            )
        },
        overlay = { TileFlightOverlay(flight) },
    ) {
        SceneDialogue(env.character, env.speaker, node.audio, env, typewriter) { visible ->
            val style = dialogueStyle()
            if (visible.length <= node.before.length) {
                Text(visible, style = style, color = AjlTheme.colors.ink)
            } else {
                TileFlow(Modifier.padding(top = 2.dp)) {
                    if (node.before.isNotEmpty()) Text(node.before, style = style, color = AjlTheme.colors.ink, modifier = Modifier.align(Alignment.CenterVertically))
                    val feedback = env.feedback
                    val index = chosen
                    when {
                        feedback != null -> VerdictChip(feedback.selected, feedback.correct, Modifier.align(Alignment.CenterVertically))
                        index != null -> key(index) {
                            WordTile(
                                choices[index],
                                onClick = ::takeBack,
                                modifier = flight.slot(index)
                                    .align(Alignment.CenterVertically)
                                    .graphicsLayer { alpha = if (flight.flyingIn(index)) 0f else 1f },
                            )
                        }
                        else -> BlankGap(Modifier.align(Alignment.CenterVertically))
                    }
                    val afterVisible = node.after.take((visible.length - node.before.length - gap.length).coerceAtLeast(0))
                    if (afterVisible.isNotEmpty()) Text(afterVisible, style = style, color = AjlTheme.colors.ink, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }
        TileFlow(Modifier.padding(top = 8.dp)) {
            choices.forEachIndexed { index, choice ->
                WordTile(
                    choice,
                    onClick = { place(index) },
                    modifier = flight.bank(index).widthIn(min = 48.dp),
                    used = chosen == index || flight.flyingBack(index),
                    enabled = !env.answered,
                )
            }
        }
    }
}
