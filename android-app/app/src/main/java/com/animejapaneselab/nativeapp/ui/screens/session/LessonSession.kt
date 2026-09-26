package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.screens.LessonScreen

// STAGE-2 STUB (task package D1): the signature is the contract LabApp calls. Replace the body
// with the v3 implementation; keep the signature (new params only with defaults).

/** Lesson session: アイキャッチ → questions (会話窓 …) → feedback → つづく. */
@Composable
fun LessonSessionScreen(
    uiState: LabUiState,
    onExit: () -> Unit,
    onSubmitAnswer: (String) -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onNextBatch: () -> Unit,
    onEvaluatePronunciation: (String, String, ByteArray, Long) -> Unit,
    onRetryPronunciation: () -> Unit,
    onResetPronunciation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LessonScreen(
        uiState = uiState,
        onExit = onExit,
        onSubmitAnswer = onSubmitAnswer,
        onContinue = onContinue,
        onRestart = onRestart,
        onNextBatch = onNextBatch,
        onEvaluatePronunciation = onEvaluatePronunciation,
        onRetryPronunciation = onRetryPronunciation,
        onResetPronunciation = onResetPronunciation,
        modifier = modifier,
    )
}
