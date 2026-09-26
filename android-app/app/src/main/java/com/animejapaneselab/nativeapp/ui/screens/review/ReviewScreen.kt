package com.animejapaneselab.nativeapp.ui.screens.review

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.ui.LabUiState

// STAGE-2 STUB (task package B): the signature is the contract LabApp calls. Replace the body
// with the v3 implementation; keep the signature (new params only with defaults).

/** 復習 tab: 翻卡堆 + 苦手なところ + 错题本. */
@Composable
fun ReviewScreen(
    uiState: LabUiState,
    onOpenLesson: () -> Unit,
    onOpenSmartReviewQueue: () -> Unit,
    onMistakeReviewed: (String) -> Unit,
    onPracticeMistake: (String) -> Unit,
    onPracticeRemoteTask: (ProgressItem) -> Unit,
    onExplainMistake: (String) -> Unit,
    /** Opens subtitles at the source line; lineNo = 0 opens the episode without focusing a line. */
    onViewSource: (workSlug: String, episode: Int, lineNo: Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.animejapaneselab.nativeapp.ui.screens.ReviewScreen(
        uiState = uiState,
        onOpenLesson = onOpenLesson,
        onOpenSmartReviewQueue = onOpenSmartReviewQueue,
        onMistakeReviewed = onMistakeReviewed,
        onPracticeMistake = onPracticeMistake,
        onPracticeRemoteTask = onPracticeRemoteTask,
        onExplainMistake = onExplainMistake,
        onViewSource = onViewSource,
        modifier = modifier,
    )
}
