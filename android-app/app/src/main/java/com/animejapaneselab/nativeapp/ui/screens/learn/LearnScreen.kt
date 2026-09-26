package com.animejapaneselab.nativeapp.ui.screens.learn

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.animejapaneselab.nativeapp.data.LessonExerciseKind
import com.animejapaneselab.nativeapp.data.LessonMode
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.LearnSection
import com.animejapaneselab.nativeapp.ui.foundation.LinguisticsTrack
import com.animejapaneselab.nativeapp.ui.foundation.LinguisticsTrackScreen
import com.animejapaneselab.nativeapp.ui.screens.FoundationActions
import com.animejapaneselab.nativeapp.ui.screens.LessonHubScreen
import com.animejapaneselab.nativeapp.ui.screens.ReadAirHomeActions
import com.animejapaneselab.nativeapp.ui.screens.ReadAirScreen
import com.animejapaneselab.nativeapp.ui.screens.session.FoundationSession

// STAGE-2 STUB (task package C): the signature is the contract LabApp calls. Replace the body
// with the v3 implementation; keep the signature (new params only with defaults).

/**
 * 学ぶ tab with 課程 / 言語学 text tabs ([LabUiState.learnSection]). 言語学 = 第一巻 アニメの台詞
 * (linguisticsTrack == AnimeCorpus) / 第二巻 基礎 (Foundation). Answering foundation questions
 * happens in [FoundationSession] (package D2); this screen decides when to show it.
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
    when (uiState.learnSection) {
        LearnSection.Course -> LessonHubScreen(
            uiState = uiState,
            onStartLesson = onStartLesson,
            onStartModeLesson = onStartModeLesson,
            onStartExercise = onStartExercise,
            onStartExerciseMix = onStartExerciseMix,
            onStartReadAir = onStartReadAirBatch,
            onStartReview = onStartReview,
            onWorkSelected = onWorkSelected,
            onEpisodeSelected = onEpisodeSelected,
            modifier = modifier,
        )
        LearnSection.Linguistics -> LinguisticsTrackScreen(
            selectedTrack = uiState.linguisticsTrack,
            onTrackSelected = onTrackSelected,
            animeCorpusContent = {
                ReadAirScreen(
                    uiState = uiState,
                    onRefresh = readAir.onRefresh,
                    onWorkSelected = readAir.onWorkSelected,
                    onDomainSelected = readAir.onDomainSelected,
                    onQuestionTypeSelected = readAir.onQuestionTypeSelected,
                    onDifficultySelected = readAir.onDifficultySelected,
                    onTopicSelected = readAir.onTopicSelected,
                    onEpisodeSelected = readAir.onEpisodeSelected,
                    onModeSelected = readAir.onModeSelected,
                    onResetFilters = readAir.onResetFilters,
                    onResetQueue = readAir.onResetQueue,
                    onStartSession = readAir.onStartSession,
                    onBrowseAnswer = readAir.onBrowseAnswer,
                )
            },
            foundationContent = {
                FoundationSession(state = uiState.foundation, actions = foundation, onExit = {})
            },
            modifier = modifier,
        )
    }
}
