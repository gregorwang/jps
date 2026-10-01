package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.animejapaneselab.nativeapp.data.GrammarPoint
import com.animejapaneselab.nativeapp.data.LessonTarget
import com.animejapaneselab.nativeapp.data.LevelDict
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.data.promptAudioForSentence
import com.animejapaneselab.nativeapp.data.toNotebookEntry
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.notebook.Notebook
import com.animejapaneselab.nativeapp.ui.notebook.rememberNotebookEntries
import com.animejapaneselab.nativeapp.ui.words.KnownWords
import com.animejapaneselab.nativeapp.ui.words.TangoLines

/** One dictionary entry to pull up from outside the 辞書 lists (a search hit). */
sealed interface DictTarget {
    data class Word(val item: VocabItem) : DictTarget
    data class Pattern(val item: GrammarPoint) : DictTarget
}

/**
 * The 辞書 word card / grammar card on its own, over whatever page is showing: search hits open
 * here instead of switching tabs. 收藏 / 斩 / AI / 練習 behave as in the lists.
 */
@Composable
fun DictEntrySheet(
    target: DictTarget,
    uiState: LabUiState,
    onAskAi: (targetKey: String, kind: String, text: String, context: String) -> Unit,
    onTargetLesson: (LessonTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val audio = rememberLessonAudioController()
    val notebook = rememberNotebookEntries()
    val savedKeys = remember(notebook) { notebook.mapTo(HashSet()) { it.key } }
    remember { KnownWords.init(appContext) }
    val known by KnownWords.words.collectAsState()
    val workSlug = uiState.selection.workSlug
    val episode = uiState.selection.episode
    val episodeLabel = uiState.focus.episodeLabel.ifBlank { episodeTitle(episode) }
    val busy = audio.playbackState.phase == AudioPlaybackPhase.Loading || audio.playbackState.phase == AudioPlaybackPhase.Playing
    var speaking by remember { mutableStateOf(false) }
    LaunchedEffect(busy) { if (!busy) speaking = false }
    fun speak(text: String) {
        speaking = true
        audio.speakText(text, uiState.settings.ttsWorkerUrl)
    }

    when (target) {
        is DictTarget.Word -> {
            val item = target.item
            var studying by remember(item.id) { mutableStateOf(false) }
            val example = remember(item.id, uiState.shadowing) {
                findExampleLine(item.surface, item.reading, uiState.shadowing)
                    ?: offlineExample(TangoLines.load(appContext)[item.id], item.id)
            }
            val cut = KnownWords.key(item) in known
            val entry = item.toNotebookEntry(workSlug, episode, example)
            if (studying) {
                val pool = remember(item.id) {
                    LevelDict.peek()?.vocab?.filter { it.level == item.level }?.takeIf { it.size >= 4 } ?: uiState.vocab
                }
                WordStudyDialog(
                    words = listOf(item),
                    pool = pool,
                    lines = uiState.shadowing,
                    settings = uiState.settings,
                    workSlug = workSlug,
                    episode = episode,
                    onDismiss = onDismiss,
                )
            } else {
                WordCardSheet(
                    item = item,
                    example = example,
                    saved = NotebookRules.key(NotebookKind.Vocab, item.id) in savedKeys,
                    known = cut,
                    speaking = speaking && busy,
                    uiState = uiState,
                    onSpeak = ::speak,
                    onPlayExample = {
                        if (example != null) {
                            speaking = false
                            audio.play(promptAudioForSentence(workSlug, example, autoPlay = false), uiState.settings.ttsWorkerUrl)
                        }
                    },
                    onToggleSave = { Notebook.toggle(appContext, entry) },
                    onCut = {
                        if (cut) KnownWords.restore(appContext, item) else KnownWords.cut(appContext, listOf(item))
                        onDismiss()
                    },
                    onAsk = { onAskAi(item.aiKey(), "vocab", item.surface, item.aiContext(episodeLabel)) },
                    onLearn = { studying = true },
                    onDismiss = onDismiss,
                )
            }
        }

        is DictTarget.Pattern -> {
            val item = target.item
            val key = KnownWords.grammarKey(item.pattern)
            val cut = key in known
            GrammarCardSheet(
                points = listOf(item),
                openId = item.id,
                uiState = uiState,
                savedKeys = savedKeys,
                known = cut,
                speaking = speaking && busy,
                onMove = {},
                onSpeak = { speak(it.pattern.trim('〜', '～')) },
                onPlayExample = { point ->
                    val line = uiState.shadowing.firstOrNull { point.sourceLineNo > 0 && it.sourceLineNo == point.sourceLineNo }
                    if (line != null) {
                        speaking = false
                        audio.play(promptAudioForSentence(workSlug, line, autoPlay = false), uiState.settings.ttsWorkerUrl)
                    } else {
                        speak(point.exampleJa)
                    }
                },
                onToggleSave = { Notebook.toggle(appContext, it.toNotebookEntry(workSlug, episode)) },
                onCut = {
                    if (cut) KnownWords.restoreKey(appContext, key) else KnownWords.cutKey(appContext, key, NotebookRules.key(NotebookKind.Grammar, it.id))
                    onDismiss()
                },
                onAsk = { onAskAi(it.aiKey(), "grammar", it.pattern, it.aiContext(episodeLabel)) },
                onLearn = {
                    onDismiss()
                    onTargetLesson(LessonTarget.Grammar(it.id))
                },
                onDismiss = onDismiss,
            )
        }
    }
}
