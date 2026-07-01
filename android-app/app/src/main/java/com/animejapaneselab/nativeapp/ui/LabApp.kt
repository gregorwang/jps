package com.animejapaneselab.nativeapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animejapaneselab.nativeapp.ui.screens.LessonHubScreen
import com.animejapaneselab.nativeapp.ui.screens.LessonScreen
import com.animejapaneselab.nativeapp.ui.screens.LibraryScreen
import com.animejapaneselab.nativeapp.ui.screens.ReadAirSessionScreen
import com.animejapaneselab.nativeapp.ui.screens.ReadAirScreen
import com.animejapaneselab.nativeapp.ui.screens.ReviewScreen
import com.animejapaneselab.nativeapp.ui.screens.SettingsScreen
import com.animejapaneselab.nativeapp.ui.screens.TodayScreen

@Composable
fun LabApp() {
    LabAppContent()
}

@Composable
private fun LabAppContent(viewModel: LabViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeSession = uiState.activeSession

    BackHandler(enabled = activeSession != null) {
        viewModel.exitTrainingSession()
    }

    Scaffold(
        bottomBar = {
            if (activeSession == null) {
                BottomNavigation(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = viewModel::selectTab,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (activeSession ?: uiState.selectedTab) {
                TrainingSessionKind.Lesson -> LessonScreen(
                    uiState = uiState,
                    onExit = viewModel::exitTrainingSession,
                    onSubmitAnswer = viewModel::submitAnswer,
                    onContinue = viewModel::continueLesson,
                    onRestart = viewModel::restartLesson,
                    onNextBatch = viewModel::startNextLessonBatch,
                    onAskAi = viewModel::askAiAboutCurrentNode,
                    onAiQuestionChange = viewModel::updateAiQuestion,
                )

                TrainingSessionKind.ReadAir -> ReadAirSessionScreen(
                    uiState = uiState,
                    onExit = viewModel::exitTrainingSession,
                    onAnswerSelected = viewModel::selectReadAirAnswer,
                    onNext = viewModel::nextReadAirExercise,
                    onRestart = viewModel::restartReadAirSession,
                    onAskAi = viewModel::askAiAboutReadAirExercise,
                )

                LabTab.Today -> TodayScreen(
                    uiState = uiState,
                    onStartLesson = viewModel::startLessonFromCurrentTab,
                    onWorkSelected = viewModel::selectWork,
                    onEpisodeSelected = viewModel::selectEpisode,
                    onOpenLibrary = { viewModel.selectTab(LabTab.Library) },
                    onStartReadAir = viewModel::startReadAirForCurrentEpisode,
                    onRefresh = viewModel::refreshFromServer,
                    onLessonModeSelected = viewModel::selectLessonMode,
                )

                LabTab.Lesson -> LessonHubScreen(
                    uiState = uiState,
                    onStartLesson = viewModel::startLesson,
                    onLessonModeSelected = viewModel::selectLessonMode,
                )

                LabTab.Library -> LibraryScreen(
                    uiState = uiState,
                    onWorkSelected = viewModel::selectWork,
                    onEpisodeSelected = viewModel::selectEpisode,
                    onStartLesson = viewModel::startLessonFromCurrentTab,
                    onStartModeLesson = viewModel::startLessonModeFromCurrentTab,
                    onStartReadAir = viewModel::startReadAirForCurrentEpisode,
                    onTargetLesson = viewModel::startTargetLesson,
                    onAskAi = viewModel::askAiAboutLibraryItem,
                )

                LabTab.ReadAir -> ReadAirScreen(
                    uiState = uiState,
                    onRefresh = viewModel::refreshReadAirExercises,
                    onWorkSelected = viewModel::selectReadAirWork,
                    onDomainSelected = viewModel::selectReadAirDomain,
                    onPhenomenonSelected = viewModel::selectReadAirPhenomenon,
                    onQuestionTypeSelected = viewModel::selectReadAirQuestionType,
                    onDifficultySelected = viewModel::selectReadAirDifficulty,
                    onEpisodeSelected = viewModel::selectReadAirEpisode,
                    onModeSelected = viewModel::selectReadAirMode,
                    onResetFilters = viewModel::resetReadAirFilters,
                    onResetQueue = viewModel::resetReadAirQueue,
                    onStartSession = viewModel::startReadAirSession,
                    onBrowseAnswer = viewModel::selectReadAirBrowseAnswer,
                )

                LabTab.Review -> ReviewScreen(
                    uiState = uiState,
                    onOpenLesson = { viewModel.selectTab(LabTab.Lesson) },
                    onPracticeWeakest = viewModel::practiceWeakestReviewItem,
                    onMistakeReviewed = viewModel::markMistakeReviewed,
                    onPracticeMistake = viewModel::practiceLocalMistake,
                    onPracticeRemoteTask = viewModel::practiceReviewTask,
                    onSync = viewModel::syncProgressNow,
                )

                LabTab.Settings -> SettingsScreen(
                    uiState = uiState,
                    onSettingsChange = viewModel::updateSettings,
                    onRefresh = viewModel::refreshFromServer,
                    onSync = viewModel::syncProgressNow,
                    onLogin = viewModel::loginOwner,
                    onLogout = viewModel::logoutOwner,
                    onClaimDevice = viewModel::claimCurrentDevice,
                    onRefreshAuth = viewModel::refreshAuthState,
                )
            }
        }
    }
}

@Composable
private fun BottomNavigation(
    selectedTab: LabTab,
    onTabSelected: (LabTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clickable(
                            role = Role.Tab,
                            onClickLabel = tab.label,
                            onClick = { onTabSelected(tab) },
                        )
                        .semantics(mergeDescendants = true) {
                            selected = isSelected
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .width(48.dp)
                            .height(30.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Text(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 0.dp),
                        text = tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

private val LabTab.icon: ImageVector
    get() = when (this) {
        LabTab.Today -> Icons.Rounded.Home
        LabTab.Lesson -> Icons.Rounded.School
        LabTab.Library -> Icons.Rounded.AutoStories
        LabTab.ReadAir -> Icons.Rounded.Psychology
        LabTab.Review -> Icons.Rounded.BarChart
        LabTab.Settings -> Icons.Rounded.Settings
    }
