package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LinguisticCardPayload
import com.animejapaneselab.nativeapp.data.StudyCardNode
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.ToolPanel
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.reading.RubyText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import androidx.compose.runtime.LaunchedEffect

/**
 * 学习卡 — not a question: one word / pattern / line as a manga panel (serif headword with
 * furigana, reading, meaning, notes), an optional folded 语言学 note, and one ink 「记住了」.
 */
@Composable
internal fun StudyCardQuestion(
    env: LessonQuestionEnv,
    node: StudyCardNode,
    settings: LabSettings,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val furigana = rememberFuriganaAnnotator(settings)
    LaunchedEffect(node.japanese, settings.showFurigana) {
        if (settings.showFurigana) furigana.request("sentence", listOf(node.japanese))
    }
    var submitted by remember(node.id) { mutableStateOf(false) }
    LessonQuestionScaffold(
        env = env,
        modifier = modifier,
        bottomBar = {
            LessonActionBar(
                primaryLabel = "记住了",
                onPrimary = {
                    if (!submitted) {
                        submitted = true
                        env.onSubmit(node.expectedAnswer)
                    }
                },
            )
        },
    ) {
        MangaPanel(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(width = 96.dp, height = 72.dp)
                    .screentone(work.tone(0.26f)),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f))
                    AudioChip(node.audio, env.playback, env.onPlay)
                }
                RubyText(
                    text = node.japanese,
                    furigana = if (settings.showFurigana) furigana.resultFor(node.japanese) else null,
                    style = AjlTheme.type.jpDisplay.copy(
                        fontSize = if (node.japanese.length > 8) 26.sp else 34.sp,
                        lineHeight = if (node.japanese.length > 8) 38.sp else 46.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    rubyStyle = AjlTheme.type.jpLabel,
                    color = colors.ink,
                    rubyColor = colors.ink3,
                )
                if (node.reading.isNotBlank() && node.reading != node.japanese) {
                    Text(node.reading, style = AjlTheme.type.jpLabel.copy(fontSize = 14.sp, lineHeight = 20.sp), color = colors.ink3)
                }
                Text(
                    node.meaningZh,
                    style = AjlTheme.type.body.copy(fontSize = 17.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium),
                    color = colors.ink,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        val notes = node.notes.filter { it.isNotBlank() && it != node.meaningZh }.distinct()
        if (notes.isNotEmpty()) {
            Column {
                notes.forEach { note ->
                    Text(
                        note,
                        style = AjlTheme.type.body.copy(fontSize = 14.sp),
                        color = colors.ink2,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                    Hairline()
                }
            }
        }
        LinguisticNote(node.linguistic)
    }
}

/** 言語学 addendum, folded by default (a tool panel: 1px line, 12dp corners). */
@Composable
private fun LinguisticNote(payload: LinguisticCardPayload?) {
    if (payload == null || !payload.hasContent) return
    val colors = AjlTheme.colors
    var open by rememberSaveable(payload) { mutableStateOf(false) }
    ToolPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button) { open = !open }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("言語学", style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp), color = colors.ink)
                Text(
                    payload.headlineZh.ifBlank { payload.level },
                    style = AjlTheme.type.caption,
                    color = colors.ink3,
                    maxLines = if (open) 3 else 1,
                    modifier = Modifier.weight(1f),
                )
                Text(if (open) "−" else "+", style = AjlTheme.type.meta.copy(fontSize = 15.sp), color = colors.ink2)
            }
            if (open) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Hairline()
                    payload.terms.filter { it.termZh.isNotBlank() }.forEach { term ->
                        Text(
                            listOf(term.termZh, term.plainZh).filter { it.isNotBlank() }.joinToString(" — "),
                            style = AjlTheme.type.body.copy(fontSize = 14.sp),
                            color = colors.ink2,
                        )
                    }
                    payload.domains.forEach { domain ->
                        val title = domain.titleZh.ifBlank { domain.domain }
                        Text(
                            listOf(title, domain.takeawayZh).filter { it.isNotBlank() }.joinToString(" · "),
                            style = AjlTheme.type.body.copy(fontSize = 14.sp),
                            color = colors.ink2,
                        )
                    }
                    if (payload.cautionZh.isNotBlank()) {
                        Text(payload.cautionZh, style = AjlTheme.type.caption, color = colors.ink3)
                    }
                    if (payload.historicalNoteZh.isNotBlank()) {
                        Text(payload.historicalNoteZh, style = AjlTheme.type.caption, color = colors.ink3)
                    }
                }
            }
        }
    }
}
