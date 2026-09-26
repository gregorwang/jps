package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.screens.SubtitleBrowserScreen

// STAGE-2 STUB (task package E): the signature is the contract LabApp calls. Replace the body
// with the v3 implementation; keep the signature (new params only with defaults).

/** Subtitle browser (secondary screen): speaker avatars, scene groups, focus-line jump. */
@Composable
fun SubtitlesScreen(
    uiState: LabUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onFocusConsumed: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubtitleBrowserScreen(
        uiState = uiState,
        onBack = onBack,
        onRefresh = onRefresh,
        onWorkSelected = onWorkSelected,
        onEpisodeSelected = onEpisodeSelected,
        modifier = modifier,
        onFocusConsumed = onFocusConsumed,
        onOpenSearch = onOpenSearch,
    )
}
