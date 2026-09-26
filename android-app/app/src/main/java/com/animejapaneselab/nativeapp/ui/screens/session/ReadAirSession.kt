package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.animejapaneselab.nativeapp.ui.LabUiState

// STAGE-2 STUB (task package D2): the signature is the contract LabApp calls. Replace the body
// with the v3 implementation; keep the signature (new params only with defaults).

/** 読空気 session in the manga-dialogue language (XReadAirManga). */
@Composable
fun ReadAirSessionScreen(
    uiState: LabUiState,
    onExit: () -> Unit,
    onAnswerSelected: (String) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.animejapaneselab.nativeapp.ui.screens.ReadAirSessionScreen(uiState, onExit, onAnswerSelected, onNext, onRestart, modifier)
}
