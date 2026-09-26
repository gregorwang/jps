package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.data.LessonTarget
import com.animejapaneselab.nativeapp.ui.LabUiState

// STAGE-2 STUB (task package E): the signature is the contract LabApp calls. Replace the body
// with the v3 implementation; keep the signature (new params only with defaults).

/** 辞書 tab: 词汇 / 语法 / 台词, dictionary entries, 五十音 index. */
@Composable
fun LibraryScreen(
    uiState: LabUiState,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onStartLesson: () -> Unit,
    onStartModeLesson: (LessonMode) -> Unit,
    onStartReadAir: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenSettings: () -> Unit,
    onTargetLesson: (LessonTarget) -> Unit,
    onAskAi: (targetKey: String, kind: String, text: String, context: String) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.animejapaneselab.nativeapp.ui.screens.LibraryScreen(
        uiState = uiState,
        onWorkSelected = onWorkSelected,
        onEpisodeSelected = onEpisodeSelected,
        onStartLesson = onStartLesson,
        onStartModeLesson = onStartModeLesson,
        onStartReadAir = onStartReadAir,
        onOpenSubtitles = onOpenSubtitles,
        onOpenSettings = onOpenSettings,
        onTargetLesson = onTargetLesson,
        onAskAi = onAskAi,
        onOpenSearch = onOpenSearch,
        modifier = modifier,
    )
}
