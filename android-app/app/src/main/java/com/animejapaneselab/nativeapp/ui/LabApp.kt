package com.animejapaneselab.nativeapp.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animejapaneselab.nativeapp.ui.screens.AiHistoryScreen
import com.animejapaneselab.nativeapp.ui.screens.LessonHubScreen
import com.animejapaneselab.nativeapp.ui.screens.LessonScreen
import com.animejapaneselab.nativeapp.ui.screens.LibraryScreen
import com.animejapaneselab.nativeapp.ui.screens.LoginGateScreen
import com.animejapaneselab.nativeapp.ui.screens.ReadAirSessionScreen
import com.animejapaneselab.nativeapp.ui.screens.ReadAirScreen
import com.animejapaneselab.nativeapp.ui.screens.ReviewScreen
import com.animejapaneselab.nativeapp.ui.screens.SearchScreen
import com.animejapaneselab.nativeapp.ui.screens.SettingsScreen
import com.animejapaneselab.nativeapp.ui.screens.SmartReviewQueueScreen
import com.animejapaneselab.nativeapp.ui.screens.SubtitleBrowserScreen
import com.animejapaneselab.nativeapp.ui.screens.TodayScreen
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackSettings
import com.animejapaneselab.nativeapp.ui.feedback.ProvideFeedbackEngine
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.platform.LearningSessionNotifier
import com.animejapaneselab.nativeapp.update.AppUpdateRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect

@Composable
fun LabApp() {
    LabAppContent()
}

@Composable
private fun LabAppContent(viewModel: LabViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeSession = uiState.activeSession
    val secondaryScreen = uiState.secondaryScreen
    val context = LocalContext.current
    val backProgress = remember { Animatable(0f) }
    val backDirection = remember { mutableFloatStateOf(1f) }
    val maxBackTranslationPx = with(LocalDensity.current) { 24.dp.toPx() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshDeviceCapabilities() }
    val promotionSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshDeviceCapabilities() }

    TrainingFrameRateEffect(highFrameRate = activeSession != null)
    LearningSessionNotificationEffect(
        enabled = uiState.settings.learningLiveUpdates,
        status = uiState.learningSessionStatus(),
    )

    PredictiveBackHandler(enabled = activeSession != null || secondaryScreen != null) { events ->
        try {
            events.collect { event ->
                backDirection.floatValue = if (event.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                backProgress.snapTo(event.progress)
            }
            if (activeSession != null) {
                viewModel.exitTrainingSession()
            } else {
                viewModel.closeSecondaryScreen()
            }
            backProgress.snapTo(0f)
        } catch (cancelled: CancellationException) {
            backProgress.animateTo(0f, animationSpec = tween(durationMillis = 140))
            throw cancelled
        }
    }

    if (uiState.auth.user == null) {
        LoginGateScreen(
            uiState = uiState,
            onSettingsChange = viewModel::updateSettings,
            onLogin = viewModel::loginOwner,
            onRefreshAuth = viewModel::refreshAuthState,
        )
        return
    }

    ProvideFeedbackEngine(
        settings = FeedbackSettings(
            soundEnabled = uiState.settings.feedbackSounds,
            hapticsEnabled = uiState.settings.hapticsEnabled,
            richAnimationsEnabled = uiState.settings.richAnimationsEnabled,
        ),
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (activeSession == null && secondaryScreen == null) {
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
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .graphicsLayer {
                        val progress = backProgress.value
                        scaleX = 1f - progress * 0.04f
                        scaleY = 1f - progress * 0.04f
                        translationX = backDirection.floatValue * progress * maxBackTranslationPx
                        alpha = 1f - progress * 0.08f
                    },
            ) {
                when {
                secondaryScreen == SecondaryScreen.Settings -> SettingsScreen(
                    uiState = uiState,
                    onSettingsChange = viewModel::updateSettings,
                    onRefresh = viewModel::refreshFromServer,
                    onLogin = viewModel::loginOwner,
                    onLogout = viewModel::logoutOwner,
                    onRefreshAuth = viewModel::refreshAuthState,
                    onRefreshDeviceCapabilities = viewModel::refreshDeviceCapabilities,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.refreshDeviceCapabilities()
                        }
                    },
                    onOpenPromotedNotificationSettings = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
                                data = "package:${context.packageName}".toUri()
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            runCatching {
                                promotionSettingsLauncher.launch(intent)
                            }
                        }
                    },
                    appUpdateContent = { AppUpdateRoute() },
                    onOpenAiHistory = viewModel::openAiHistory,
                    onBack = viewModel::closeSecondaryScreen,
                )

                secondaryScreen == SecondaryScreen.AiHistory -> AiHistoryScreen(
                    uiState = uiState,
                    onBack = viewModel::closeSecondaryScreen,
                )

                secondaryScreen == SecondaryScreen.Search -> SearchScreen(
                    uiState = uiState,
                    onBack = viewModel::closeSecondaryScreen,
                    onOpenSubtitleLine = viewModel::openSubtitlesAt,
                )

                secondaryScreen == SecondaryScreen.Subtitles -> SubtitleBrowserScreen(
                    uiState = uiState,
                    onBack = viewModel::closeSecondaryScreen,
                    onRefresh = viewModel::refreshSubtitleLines,
                    onWorkSelected = viewModel::selectWork,
                    onEpisodeSelected = viewModel::selectEpisode,
                    onFocusConsumed = viewModel::clearSubtitleFocus,
                    onOpenSearch = viewModel::openSearch,
                )

                secondaryScreen == SecondaryScreen.SmartReviewQueue -> SmartReviewQueueScreen(
                    plan = uiState.smartReviewPlan,
                    onBack = viewModel::closeSecondaryScreen,
                    onStartItem = viewModel::startSmartReviewItem,
                )

                activeSession == TrainingSessionKind.Lesson -> LessonScreen(
                    uiState = uiState,
                    onExit = viewModel::exitTrainingSession,
                    onSubmitAnswer = viewModel::submitAnswer,
                    onContinue = viewModel::continueLesson,
                    onRestart = viewModel::restartLesson,
                    onNextBatch = viewModel::startNextLessonBatch,
                    onEvaluatePronunciation = viewModel::evaluatePronunciation,
                    onRetryPronunciation = viewModel::retryPronunciationEvaluation,
                    onResetPronunciation = viewModel::resetPronunciationEvaluation,
                )

                activeSession == TrainingSessionKind.ReadAir -> ReadAirSessionScreen(
                    uiState = uiState,
                    onExit = viewModel::exitTrainingSession,
                    onAnswerSelected = viewModel::selectReadAirAnswer,
                    onNext = viewModel::nextReadAirExercise,
                    onRestart = viewModel::restartReadAirSession,
                )

                else -> when (uiState.selectedTab) {
                    LabTab.Today -> TodayScreen(
                        uiState = uiState,
                        onStartLesson = viewModel::startLessonFromCurrentTab,
                        onStartReadAir = viewModel::startReadAirForCurrentEpisode,
                        onStartReview = viewModel::openSmartReviewQueue,
                        onOpenSubtitles = viewModel::openSubtitles,
                    )

                    LabTab.Lesson -> LessonHubScreen(
                        uiState = uiState,
                        onStartLesson = viewModel::startLesson,
                        onStartModeLesson = { mode, batch, pathNodeKey ->
                            viewModel.startLessonModeFromCurrentTab(mode, batch, pathNodeKey)
                        },
                        onStartExercise = viewModel::startExerciseLab,
                        onStartExerciseMix = viewModel::startExerciseLabMix,
                        onStartReadAir = viewModel::startReadAirPathBatch,
                        onOpenReadAir = { viewModel.selectTab(LabTab.Linguistics) },
                        onStartReview = viewModel::openSmartReviewQueue,
                        onWorkSelected = viewModel::selectWork,
                        onEpisodeSelected = viewModel::selectEpisode,
                    )

                    LabTab.Linguistics -> ReadAirScreen(
                        uiState = uiState,
                        onRefresh = viewModel::refreshReadAirExercises,
                        onWorkSelected = viewModel::selectReadAirWork,
                        onDomainSelected = viewModel::selectReadAirDomain,
                        onQuestionTypeSelected = viewModel::selectReadAirQuestionType,
                        onDifficultySelected = viewModel::selectReadAirDifficulty,
                        onTopicSelected = viewModel::selectReadAirTopic,
                        onEpisodeSelected = viewModel::selectReadAirEpisode,
                        onModeSelected = viewModel::selectReadAirMode,
                        onResetFilters = viewModel::resetReadAirFilters,
                        onResetQueue = viewModel::resetReadAirQueue,
                        onStartSession = viewModel::startReadAirSession,
                        onBrowseAnswer = viewModel::selectReadAirBrowseAnswer,
                    )

                    LabTab.Library -> LibraryScreen(
                        uiState = uiState,
                        onWorkSelected = viewModel::selectWork,
                        onEpisodeSelected = viewModel::selectEpisode,
                        onStartLesson = viewModel::startLessonFromCurrentTab,
                        onStartModeLesson = { mode -> viewModel.startLessonModeFromCurrentTab(mode) },
                        onStartReadAir = { viewModel.selectTab(LabTab.Linguistics) },
                        onOpenSubtitles = viewModel::openSubtitles,
                        onOpenSettings = viewModel::openSettings,
                        onTargetLesson = viewModel::startTargetLesson,
                        onAskAi = viewModel::askAiAboutLibraryItem,
                        onOpenSearch = viewModel::openSearch,
                    )

                    LabTab.Review -> ReviewScreen(
                        uiState = uiState,
                        onOpenLesson = { viewModel.selectTab(LabTab.Lesson) },
                        onOpenSmartReviewQueue = viewModel::openSmartReviewQueue,
                        onMistakeReviewed = viewModel::markMistakeReviewed,
                        onPracticeMistake = viewModel::practiceLocalMistake,
                        onPracticeRemoteTask = viewModel::practiceReviewTask,
                        onExplainMistake = viewModel::askAiAboutMistake,
                        onViewSource = viewModel::openSubtitlesAt,
                    )

                }
                }
            }
        }
    }
}

@Composable
private fun LearningSessionNotificationEffect(
    enabled: Boolean,
    status: LearningSessionStatus?,
) {
    val context = LocalContext.current
    val notifier = remember(context) { LearningSessionNotifier(context) }
    val sessionStarted = remember { mutableStateOf(false) }
    LaunchedEffect(enabled, status) {
        if (enabled && status != null) {
            if (sessionStarted.value) {
                notifier.update(status)
            } else {
                notifier.beginSession(status)
                sessionStarted.value = true
            }
        } else {
            notifier.endSession()
            sessionStarted.value = false
        }
    }
}

@Composable
private fun TrainingFrameRateEffect(highFrameRate: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
    val view = LocalView.current

    DisposableEffect(view, highFrameRate) {
        val requestedRate = if (highFrameRate) {
            View.REQUESTED_FRAME_RATE_CATEGORY_HIGH
        } else {
            View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT
        }
        runCatching { view.setRequestedFrameRate(requestedRate) }
        onDispose {
            if (highFrameRate) {
                runCatching { view.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_DEFAULT) }
            }
        }
    }
}

@Composable
private fun BottomNavigation(
    selectedTab: LabTab,
    onTabSelected: (LabTab) -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RectangleShape,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(68.dp)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LabTab.entries.forEach { tab ->
                    BottomNavigationItem(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        reducedMotion = reducedMotion,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationItem(
    tab: LabTab,
    isSelected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "nav-pill-color",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "nav-content-color",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = MotionTokens.popSpring(reducedMotion),
        label = "nav-icon-scale",
    )
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable(
                role = Role.Tab,
                onClickLabel = tab.label,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                selected = isSelected
            },
        color = Color.Transparent,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 30.dp)
                    .clip(CircleShape)
                    .background(pillColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                    tint = contentColor,
                )
            }
            Text(
                text = tab.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
            )
        }
    }
}

private val LabTab.icon: ImageVector
    get() = when (this) {
        LabTab.Today -> Icons.Rounded.Home
        LabTab.Lesson -> Icons.Rounded.School
        LabTab.Linguistics -> Icons.Rounded.Psychology
        LabTab.Library -> Icons.Rounded.AutoStories
        LabTab.Review -> Icons.Rounded.BarChart
    }
