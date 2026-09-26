package com.animejapaneselab.nativeapp.ui.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.LessonExerciseKind
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.LearnSection
import com.animejapaneselab.nativeapp.ui.design.BroadcastLine
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.TextTabs
import com.animejapaneselab.nativeapp.ui.foundation.LinguisticsTrack
import com.animejapaneselab.nativeapp.ui.screens.FoundationActions
import com.animejapaneselab.nativeapp.ui.screens.ReadAirHomeActions
import com.animejapaneselab.nativeapp.ui.screens.session.FoundationSession
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * 学ぶ tab: 課程 / 言語学 text tabs ([LabUiState.learnSection]). 課程 = [CourseScreen]; 言語学 =
 * [LinguisticsScreen] (第一巻 アニメの台詞 / 第二巻 基礎). Foundation answering happens in
 * [FoundationSession] (package D2) — shown here in place of the page while open; its ×/back
 * returns to the 教科書.
 */
@Composable
fun LearnScreen(
    uiState: LabUiState,
    onSectionSelected: (LearnSection) -> Unit,
    // 課程
    onStartLesson: () -> Unit,
    onStartModeLesson: (LessonMode, Int, String) -> Unit,
    onStartExercise: (LessonExerciseKind) -> Unit,
    onStartExerciseMix: () -> Unit,
    onStartReadAirBatch: (Int) -> Unit,
    onStartReview: () -> Unit,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    // 言語学
    onTrackSelected: (LinguisticsTrack) -> Unit,
    readAir: ReadAirHomeActions,
    foundation: FoundationActions,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var foundationOpen by rememberSaveable { mutableStateOf(false) }
    val onFoundationVolume = uiState.learnSection == LearnSection.Linguistics &&
        uiState.linguisticsTrack == LinguisticsTrack.Foundation
    LaunchedEffect(onFoundationVolume) { if (!onFoundationVolume) foundationOpen = false }
    if (foundationOpen && onFoundationVolume) {
        FoundationSession(
            state = uiState.foundation,
            actions = foundation,
            onExit = { foundationOpen = false },
            modifier = modifier,
        )
        return
    }

    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val course = uiState.learnSection == LearnSection.Course
    Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
        BroadcastLine()
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextTabs(
                items = listOf("課程", "言語学"),
                selectedIndex = if (course) 0 else 1,
                onSelect = { onSectionSelected(if (it == 0) LearnSection.Course else LearnSection.Linguistics) },
            )
            Spacer(Modifier.weight(1f))
            if (course) {
                IconButton44(Icons.Rounded.Search, "搜索", onOpenSearch)
            } else {
                IconButton44(Icons.Rounded.Tune, "筛选", { filtersOpen = true })
            }
        }
        if (course) {
            CourseScreen(
                uiState = uiState,
                actions = CourseActions(
                    onStartLesson = onStartLesson,
                    onStartModeLesson = onStartModeLesson,
                    onStartExercise = onStartExercise,
                    onStartExerciseMix = onStartExerciseMix,
                    onStartReadAirBatch = onStartReadAirBatch,
                    onStartReview = onStartReview,
                    onWorkSelected = onWorkSelected,
                    onEpisodeSelected = onEpisodeSelected,
                ),
                modifier = Modifier.weight(1f),
            )
        } else {
            LinguisticsScreen(
                uiState = uiState,
                onTrackSelected = onTrackSelected,
                readAir = readAir,
                foundation = foundation,
                onOpenFoundation = { foundationOpen = true },
                filtersOpen = filtersOpen,
                onFiltersDismiss = { filtersOpen = false },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

internal data class CourseActions(
    val onStartLesson: () -> Unit,
    val onStartModeLesson: (LessonMode, Int, String) -> Unit,
    val onStartExercise: (LessonExerciseKind) -> Unit,
    val onStartExerciseMix: () -> Unit,
    val onStartReadAirBatch: (Int) -> Unit,
    val onStartReview: () -> Unit,
    val onWorkSelected: (String) -> Unit,
    val onEpisodeSelected: (Int) -> Unit,
)
