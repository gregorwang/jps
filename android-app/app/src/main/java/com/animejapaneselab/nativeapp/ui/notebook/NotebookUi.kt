package com.animejapaneselab.nativeapp.ui.notebook

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.animejapaneselab.nativeapp.data.NotebookEntry
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.data.asShadowingSentence
import com.animejapaneselab.nativeapp.data.promptAudioForSentence
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.EmptyNote
import com.animejapaneselab.nativeapp.ui.design.FilterPill
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.Screentone
import com.animejapaneselab.nativeapp.ui.design.StampMark
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.theme.ProvideWorkTheme
import java.time.LocalDate

/** Collects the notebook, initialising it from disk on first use. */
@Composable
fun rememberNotebookEntries(): List<NotebookEntry> {
    val context = LocalContext.current
    remember { Notebook.init(context) }
    val entries by Notebook.entries.collectAsState()
    return entries
}

/** Small 栞 toggle for the action rows of dictionary entries. */
@Composable
fun NotebookToggleButton(entry: () -> NotebookEntry, saved: Boolean) {
    val context = LocalContext.current
    OutlineButton(
        text = if (saved) "栞を外す" else "栞に挟む",
        onClick = { Notebook.toggle(context, entry()) },
        compact = true,
        ink = false,
    )
}

/** A work-colour bookmark notch shown on saved rows. */
@Composable
fun NotebookMark(modifier: Modifier = Modifier) {
    Text(
        "栞",
        style = AjlTheme.type.metaSmall.copy(fontSize = 10.sp),
        color = AjlTheme.work.onAccent,
        modifier = modifier
            .background(AjlTheme.work.accent, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

internal fun speakEntry(entry: NotebookEntry, audio: LessonAudioController, ttsWorkerUrl: String) {
    when (entry.kind) {
        NotebookKind.Line -> audio.play(
            promptAudioForSentence(entry.workSlug, entry.asShadowingSentence(), autoPlay = false),
            ttsWorkerUrl,
        )
        NotebookKind.Vocab -> audio.speakText(entry.headline, ttsWorkerUrl)
        NotebookKind.Grammar -> audio.speakText(entry.example.ifBlank { entry.headline }, ttsWorkerUrl)
    }
}

private fun sourceLabel(entry: NotebookEntry): String = listOfNotNull(
    WorkIdentity.displayName(entry.workSlug).takeIf { it.isNotBlank() },
    entry.episode.takeIf { it > 0 }?.let { TextRules.episodeLabel(it) },
    entry.lineNo.takeIf { it > 0 }?.let { "L$it" },
).joinToString(" · ")

// ---------------------------------------------------------------------------
// 栞 page (辞書 4th tab)
// ---------------------------------------------------------------------------

private enum class KindFilter(val label: String, val kind: NotebookKind?) {
    All("全部", null),
    Vocab("词", NotebookKind.Vocab),
    Grammar("文型", NotebookKind.Grammar),
    Line("台词", NotebookKind.Line),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotebookPage(
    ttsWorkerUrl: String,
    onViewSource: (workSlug: String, episode: Int, lineNo: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val entries = rememberNotebookEntries()
    val audio = rememberLessonAudioController()
    val today = LocalDate.now().toEpochDay()
    var filter by rememberSaveable { mutableStateOf(KindFilter.All) }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewing by rememberSaveable { mutableStateOf(false) }
    val shown = remember(entries, filter) { entries.filter { filter.kind == null || it.kind == filter.kind } }
    val due = remember(entries, today) { NotebookRules.dueCount(entries, today) }
    val colors = AjlTheme.colors
    val type = AjlTheme.type

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
    ) {
        item(key = "tools", contentType = "tools") {
            Column(Modifier.padding(top = 14.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (entries.isNotEmpty()) {
                    OutlineButton(
                        text = if (due > 0) "めくる · 到期 $due 枚" else "今日の栞は全部めくった",
                        onClick = { reviewing = true },
                        enabled = due > 0,
                        ink = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KindFilter.entries.forEach { f ->
                        val count = if (f.kind == null) entries.size else entries.count { it.kind == f.kind }
                        FilterPill("${f.label} $count", selected = filter == f, onClick = { filter = f })
                    }
                }
            }
        }
        if (shown.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                EmptyNote(
                    if (entries.isEmpty()) "栞はまだない" else "この種類の栞はない",
                    gloss = if (entries.isEmpty()) "在词汇、语法、台词或今日の一句里点「栞に挟む」" else null,
                )
            }
        }
        items(shown, key = { it.key }, contentType = { "entry" }) { entry ->
            val open = expanded == entry.key
            Column(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickableNoRipple(onClick = { expanded = if (open) null else entry.key })
                        .padding(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            entry.kind.label,
                            style = type.metaSmall.copy(fontSize = 10.sp),
                            color = colors.ink2,
                            modifier = Modifier
                                .border(AjlStroke.Hair, colors.line2, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                        Text(sourceLabel(entry), style = type.metaSmall, color = colors.ink3, modifier = Modifier.weight(1f), maxLines = 1)
                        if (NotebookRules.isMastered(entry)) {
                            Text("覚えた", style = type.metaSmall, color = colors.ok)
                        } else if (NotebookRules.isDue(entry, today)) {
                            Text("到期", style = type.metaSmall, color = AjlTheme.work.accent)
                        }
                    }
                    val big = entry.kind != NotebookKind.Line
                    Text(
                        entry.headline,
                        style = if (big) {
                            type.jpDisplay.copy(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                        } else {
                            type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp)
                        },
                        color = colors.ink,
                    )
                    if (entry.reading.isNotBlank()) Text(entry.reading, style = type.jpBody.copy(fontSize = 13.sp), color = colors.ink3)
                    if (entry.meaning.isNotBlank()) {
                        Text(entry.meaning, style = type.body.copy(fontSize = 14.sp, lineHeight = 21.sp), color = colors.ink2)
                    }
                }
                if (open) {
                    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (entry.example.isNotBlank()) {
                            Text("「${entry.example}」", style = type.jpBody.copy(fontSize = 14.sp, lineHeight = 22.sp), color = colors.ink2)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlineButton("聞く", { speakEntry(entry, audio, ttsWorkerUrl) }, compact = true, leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp)
                            if (entry.workSlug.isNotBlank() && entry.episode > 0) {
                                OutlineButton("原場面", { onViewSource(entry.workSlug, entry.episode, entry.lineNo) }, compact = true)
                            }
                            OutlineButton("栞を外す", {
                                expanded = null
                                Notebook.remove(context, entry.key)
                            }, compact = true)
                        }
                    }
                }
                Hairline()
            }
        }
    }

    if (reviewing) {
        NotebookReviewDialog(ttsWorkerUrl = ttsWorkerUrl, onDismiss = { reviewing = false })
    }
}

// ---------------------------------------------------------------------------
// 栞をめくる — flashcards
// ---------------------------------------------------------------------------

/**
 * Full-screen flashcard run over the due 栞 (max 20): front shows the headline, めくる reveals
 * reading / meaning / example and plays the audio, then まだ / 覚えた move it along the ladder.
 */
@Composable
fun NotebookReviewDialog(ttsWorkerUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val audio = rememberLessonAudioController()
    // The queue is frozen when the dialog opens so grading doesn't reshuffle it underfoot.
    val queue = remember {
        Notebook.init(context)
        NotebookRules.dueQueue(Notebook.entries.value, LocalDate.now().toEpochDay())
    }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var remembered by remember { mutableIntStateOf(0) }
    val colors = AjlTheme.colors
    val type = AjlTheme.type

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(colors.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            TopBar(
                nav = TopBarNav.Close,
                onNav = onDismiss,
                center = {
                    ProgressLine(
                        progress = if (queue.isEmpty()) 1f else index.toFloat() / queue.size,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${index.coerceAtMost(queue.size)}/${queue.size}",
                        style = type.meta,
                        color = colors.ink3,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                },
            )
            val card = queue.getOrNull(index)
            if (card == null) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    StampMark(text = "済", size = 64.dp, animateIn = true)
                    Spacer(Modifier.height(20.dp))
                    Text("おつかれ", style = type.jpTitle.copy(fontSize = 22.sp), color = colors.ink)
                    Text(
                        "覚えた $remembered · まだ ${queue.size - remembered}",
                        style = type.meta,
                        color = colors.ink3,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Spacer(Modifier.height(28.dp))
                    InkButton("閉じる", onClick = onDismiss)
                }
            } else ProvideWorkTheme(card.workSlug) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                ) {
                    MangaPanel(Modifier.fillMaxWidth().weight(1f).clickableNoRipple(onClick = {
                        if (!revealed) {
                            revealed = true
                            speakEntry(card, audio, ttsWorkerUrl)
                        }
                    })) {
                        Screentone(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .fillMaxWidth(0.55f)
                                .height(120.dp),
                        )
                        Text(
                            sourceLabel(card),
                            style = type.meta,
                            color = colors.ink3,
                            modifier = Modifier.padding(start = 16.dp, top = 14.dp),
                        )
                        AnimatedContent(
                            targetState = revealed,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
                            label = "notebook-card",
                        ) { back ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                val big = card.kind != NotebookKind.Line
                                Text(
                                    card.headline,
                                    style = type.jpDisplay.copy(
                                        fontSize = if (big) 34.sp else 22.sp,
                                        lineHeight = if (big) 44.sp else 34.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = colors.ink,
                                    textAlign = TextAlign.Center,
                                )
                                if (back) {
                                    if (card.reading.isNotBlank()) {
                                        Text(card.reading, style = type.jpBody.copy(fontSize = 15.sp), color = colors.ink3, textAlign = TextAlign.Center)
                                    }
                                    if (card.meaning.isNotBlank()) {
                                        Text(card.meaning, style = type.body.copy(fontSize = 16.sp, lineHeight = 24.sp), color = colors.ink, textAlign = TextAlign.Center)
                                    }
                                    if (card.example.isNotBlank()) {
                                        Text("「${card.example}」", style = type.jpBody.copy(fontSize = 14.sp, lineHeight = 22.sp), color = colors.ink2, textAlign = TextAlign.Center)
                                    }
                                    QuietButton("もう一度聞く", onClick = { speakEntry(card, audio, ttsWorkerUrl) })
                                } else {
                                    Text("点一下翻面", style = type.caption, color = colors.ink3)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    fun grade(ok: Boolean) {
                        Notebook.grade(context, card.key, ok)
                        StudyLog.record(context, answers = 1, correct = if (ok) 1 else 0)
                        if (ok) remembered++
                        revealed = false
                        index++
                    }
                    if (revealed) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlineButton("まだ", { grade(false) }, modifier = Modifier.weight(1f))
                            InkButton("覚えた", onClick = { grade(true) }, modifier = Modifier.weight(1f))
                        }
                    } else {
                        InkButton("めくる", onClick = {
                            revealed = true
                            speakEntry(card, audio, ttsWorkerUrl)
                        })
                    }
                }
            }
        }
    }
}
