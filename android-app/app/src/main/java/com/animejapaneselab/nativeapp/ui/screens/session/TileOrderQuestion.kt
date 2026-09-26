package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.TileOrderNode
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.WordTile
import com.animejapaneselab.nativeapp.ui.design.rememberTypewriterState
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * 拼句 (V3LessonScene). 听音: finish the character's line inside the 会話窓 from word tiles.
 * 翻译: the line is typed out in the 会話窓 and the Chinese tiles go into the subtitle strip under
 * the shot. Tiles fly in / back (MOTION §3-04); one ink 检查.
 */
@Composable
internal fun TileOrderQuestion(
    env: LessonQuestionEnv,
    node: TileOrderNode,
    modifier: Modifier = Modifier,
) {
    val reduced = rememberReducedMotion()
    val flight = rememberTileFlightState(node.id, animate = !reduced)
    var placed by rememberSaveable(node.id) { mutableStateOf(listOf<Int>()) }
    var checking by remember(node.id) { mutableStateOf(false) }
    val bank = node.bankTiles

    fun place(index: Int) {
        if (env.answered || index in placed || flight.busy(index)) return
        placed = placed + index
        flight.launchIn(index, bank[index])
    }

    fun takeBack(index: Int) {
        if (env.answered || flight.busy(index)) return
        flight.launchBack(index, bank[index])
        placed = placed - index
    }

    val heading = when {
        !node.audioTile -> "给这句台词配上字幕"
        env.speaker != null -> "听一遍，替${env.speaker}把台词说完"
        else -> "听一遍，把这句台词拼出来"
    }
    val typewriter = rememberTypewriterState(if (node.audioTile) "" else node.displayText)

    @Composable
    fun PlacedTiles(emptyWidth: androidx.compose.ui.unit.Dp) {
        val feedback = env.feedback
        TileFlow {
            if (feedback != null) {
                placed.forEach { index ->
                    VerdictChip(bank[index], feedback.correct, Modifier.align(Alignment.CenterVertically))
                }
            } else if (placed.isEmpty()) {
                BlankGap(Modifier.align(Alignment.CenterVertically), width = emptyWidth)
            } else {
                placed.forEach { index ->
                    key(index) {
                        WordTile(
                            bank[index],
                            onClick = { takeBack(index) },
                            modifier = flight.slot(index)
                                .align(Alignment.CenterVertically)
                                .graphicsLayer { alpha = if (flight.flyingIn(index)) 0f else 1f },
                        )
                    }
                }
            }
        }
    }

    LessonQuestionScaffold(
        env = env,
        modifier = modifier,
        heading = heading,
        bottomBar = {
            LessonActionBar(
                primaryLabel = "检查",
                enabled = placed.isNotEmpty() && !checking,
                onPrimary = {
                    if (placed.isNotEmpty() && !checking) {
                        checking = true
                        env.onSubmit(placed.joinToString("") { bank[it] })
                    }
                },
            )
        },
        overlay = { TileFlightOverlay(flight) },
    ) {
        SceneDialogue(env.character, env.speaker, node.audio, env, typewriter) { visible ->
            if (node.audioTile) {
                PlacedTiles(emptyWidth = 96.dp)
            } else {
                Text(visible, style = dialogueStyle(), color = AjlTheme.colors.ink)
            }
        }
        if (!node.audioTile) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(AjlShape.Panel)
                    .background(AjlTheme.colors.sunken)
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("字幕", style = AjlTheme.type.meta, color = AjlTheme.colors.ink3)
                PlacedTiles(emptyWidth = 160.dp)
            }
        }
        Hairline(Modifier.padding(top = 4.dp))
        TileFlow {
            bank.forEachIndexed { index, tile ->
                WordTile(
                    tile,
                    onClick = { place(index) },
                    modifier = flight.bank(index).widthIn(min = 48.dp),
                    used = index in placed || flight.flyingBack(index),
                    enabled = !env.answered,
                )
            }
        }
    }
}
