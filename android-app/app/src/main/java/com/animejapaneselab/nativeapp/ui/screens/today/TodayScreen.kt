package com.animejapaneselab.nativeapp.ui.screens.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.ui.LabUiState

// STAGE-2 STUB (task package B): the signature is the contract LabApp calls. Replace the body
// with the v3 implementation; keep the signature (new params only with defaults).

/** 今日 tab: 今日の一句 + 本日の時間割. */
@Composable
fun TodayScreen(
    uiState: LabUiState,
    onStartLesson: () -> Unit,
    onStartModeLesson: (LessonMode) -> Unit,
    onStartReadAir: () -> Unit,
    onStartReview: () -> Unit,
    onOpenLearn: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.animejapaneselab.nativeapp.ui.screens.TodayScreen(
        uiState = uiState,
        onStartLesson = onStartLesson,
        onStartReadAir = onStartReadAir,
        onStartReview = onStartReview,
        onOpenSubtitles = onOpenSubtitles,
        modifier = modifier,
    )
}
