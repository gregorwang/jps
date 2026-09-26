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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.animejapaneselab.nativeapp.platform.LearningSessionNotifier
import com.animejapaneselab.nativeapp.ui.design.BottomTabBar
import com.animejapaneselab.nativeapp.ui.design.TabItem
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackSettings
import com.animejapaneselab.nativeapp.ui.feedback.ProvideFeedbackEngine
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.pageTransform
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.screens.FoundationActions
import com.animejapaneselab.nativeapp.ui.screens.ReadAirHomeActions
import com.animejapaneselab.nativeapp.ui.screens.learn.LearnScreen
import com.animejapaneselab.nativeapp.ui.screens.library.LibraryScreen
import com.animejapaneselab.nativeapp.ui.screens.library.SubtitlesScreen
import com.animejapaneselab.nativeapp.ui.screens.login.LoginScreen
import com.animejapaneselab.nativeapp.ui.screens.review.ReviewScreen
import com.animejapaneselab.nativeapp.ui.screens.review.SmartReviewQueueScreen
import com.animejapaneselab.nativeapp.ui.screens.search.CommandPalette
import com.animejapaneselab.nativeapp.ui.screens.session.LessonSessionScreen
import com.animejapaneselab.nativeapp.ui.screens.session.ReadAirSessionScreen
import com.animejapaneselab.nativeapp.ui.screens.settings.AiHistoryScreen
import com.animejapaneselab.nativeapp.ui.screens.settings.SettingsScreen
import com.animejapaneselab.nativeapp.ui.screens.today.TodayScreen
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.theme.ProvideWorkTheme
import com.animejapaneselab.nativeapp.update.AppUpdateRoute
import kotlinx.coroutines.CancellationException

@Composable
fun LabApp() {
    LabAppContent()
}

/**
 * What the shell is showing underneath the command palette. [depth] orders the page stack so
 * [pageTransform] slides forward (deeper) or back: tabs 0 → secondary pages 1 → AI history and
 * answering sessions 2. Tab ↔ tab switches do not animate (only the tab indicator moves).
 */
internal sealed interface ShellRoute {
    val depth: Int

    data class Main(val tab: LabTab) : ShellRoute {
        override val depth: Int get() = 0
    }

    data class Secondary(val screen: SecondaryScreen) : ShellRoute {
        override val depth: Int get() = if (screen == SecondaryScreen.AiHistory) 2 else 1
    }

    data class Session(val kind: TrainingSessionKind) : ShellRoute {
        override val depth: Int get() = 2
    }
}

/**
 * The page under the shell. The search palette is an overlay, so while it is open the page
 * below stays whatever was showing before ([underlay]).
 */
internal fun shellRouteOf(
    secondary: SecondaryScreen?,
    session: TrainingSessionKind?,
    tab: LabTab,
    underlay: SecondaryScreen?,
): ShellRoute {
    val page = if (secondary == SecondaryScreen.Search) underlay else secondary
    return when {
        page != null && page != SecondaryScreen.Search -> ShellRoute.Secondary(page)
        session != null -> ShellRoute.Session(session)
        else -> ShellRoute.Main(tab)
    }
}

@Composable
private fun LabAppContent(viewModel: LabViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeSession = uiState.activeSession
    val secondaryScreen = uiState.secondaryScreen
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotion()
    val density = LocalDensity.current
    val backProgress = remember { Animatable(0f) }
    val backDirection = remember { mutableFloatStateOf(1f) }
    val maxBackTranslationPx = with(density) { 24.dp.toPx() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshDeviceCapabilities() }
    val promotionSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshDeviceCapabilities() }

    // The page the palette floats above; Search itself never becomes a page.
    var underlay by remember { mutableStateOf<SecondaryScreen?>(null) }
    LaunchedEffect(secondaryScreen) {
        if (secondaryScreen != SecondaryScreen.Search) underlay = secondaryScreen
    }
    val paletteOpen = secondaryScreen == SecondaryScreen.Search
    val route = shellRouteOf(secondaryScreen, activeSession, uiState.selectedTab, underlay)
    val closePalette: () -> Unit = {
        // Closing search returns to the page it was opened from (only 字幕 has an entry).
        if (underlay == SecondaryScreen.Subtitles) viewModel.openSubtitles() else viewModel.closeSecondaryScreen()
    }
    val goBack: () -> Unit = {
        when {
            secondaryScreen == SecondaryScreen.AiHistory -> viewModel.openSettings()
            secondaryScreen != null -> viewModel.closeSecondaryScreen()
            activeSession != null -> viewModel.exitTrainingSession()
        }
    }

    TrainingFrameRateEffect(highFrameRate = activeSession != null)
    LearningSessionNotificationEffect(
        enabled = uiState.settings.learningLiveUpdates,
        status = uiState.learningSessionStatus(),
    )

    // Predictive back for pages and sessions; the palette registers its own (later = on top).
    PredictiveBackHandler(enabled = !paletteOpen && (activeSession != null || secondaryScreen != null)) { events ->
        try {
            events.collect { event ->
                backDirection.floatValue = if (event.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                backProgress.snapTo(event.progress)
            }
            goBack()
            backProgress.snapTo(0f)
        } catch (cancelled: CancellationException) {
            backProgress.animateTo(0f, animationSpec = tween(durationMillis = 140))
            throw cancelled
        }
    }

    // Login gate. After a successful login the gate stays on top for 400ms while its
    // illustration grows to 1.15× and the screen fades onto 今日 (MOTION_SPEC「其他细节」).
    val loggedIn = uiState.auth.user != null
    var gateVisible by remember { mutableStateOf(!loggedIn) }
    val gateExit = remember { Animatable(0f) }
    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            if (gateVisible && !reducedMotion) {
                gateExit.animateTo(1f, tween(400, easing = MotionTokens.Ease.Accelerate))
            }
            gateVisible = false
        } else {
            gateExit.snapTo(0f)
            gateVisible = true
        }
    }

    ProvideWorkTheme(uiState.selection.workSlug) {
        Box(Modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
            if (loggedIn) {
                ProvideFeedbackEngine(
                    settings = FeedbackSettings(
                        soundEnabled = uiState.settings.feedbackSounds,
                        hapticsEnabled = uiState.settings.hapticsEnabled,
                        richAnimationsEnabled = uiState.settings.richAnimationsEnabled,
                    ),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Scaffold(
                            containerColor = AjlTheme.colors.bg,
                            contentWindowInsets = WindowInsets.safeDrawing,
                            bottomBar = {
                                if (route is ShellRoute.Main) {
                                    BottomTabBar(
                                        items = LabTab.entries.map { TabItem(it.jp, it.label) },
                                        selectedIndex = LabTab.entries.indexOf(uiState.selectedTab),
                                        onSelect = { viewModel.selectTab(LabTab.entries[it]) },
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
                                AnimatedContent(
                                    targetState = route,
                                    transitionSpec = {
                                        val from = initialState.depth
                                        val to = targetState.depth
                                        if (from == to && from == 0) {
                                            ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)
                                        } else {
                                            pageTransform(forward = to >= from, reducedMotion = reducedMotion, density = density)
                                        }
                                    },
                                    label = "shell-page",
                                ) { page ->
                                    ShellPage(
                                        route = page,
                                        uiState = uiState,
                                        viewModel = viewModel,
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
                                                runCatching { promotionSettingsLauncher.launch(intent) }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        CommandPalette(
                            visible = paletteOpen,
                            uiState = uiState,
                            onDismiss = closePalette,
                            onOpenSubtitleLine = viewModel::openSubtitlesAt,
                            onOpenLibrary = { viewModel.selectTab(LabTab.Library) },
                        )
                    }
                }
            }
            if (gateVisible) {
                LoginScreen(
                    uiState = uiState,
                    onSettingsChange = viewModel::updateSettings,
                    onLogin = viewModel::loginOwner,
                    onRefreshAuth = viewModel::refreshAuthState,
                    exitProgress = { gateExit.value },
                    modifier = Modifier
                        .background(AjlTheme.colors.bg.copy(alpha = 0f))
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                )
            }
        }
    }
}

@Composable
private fun ShellPage(
    route: ShellRoute,
    uiState: LabUiState,
    viewModel: LabViewModel,
    onRequestNotificationPermission: () -> Unit,
    onOpenPromotedNotificationSettings: () -> Unit,
) {
    when (route) {
        is ShellRoute.Secondary -> when (route.screen) {
            SecondaryScreen.Settings -> SettingsScreen(
                uiState = uiState,
                onSettingsChange = viewModel::updateSettings,
                onRefresh = viewModel::refreshFromServer,
                onLogout = viewModel::logoutOwner,
                onRefreshAuth = viewModel::refreshAuthState,
                onRefreshDeviceCapabilities = viewModel::refreshDeviceCapabilities,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenPromotedNotificationSettings = onOpenPromotedNotificationSettings,
                appUpdateContent = { AppUpdateRoute() },
                onOpenAiHistory = viewModel::openAiHistory,
                onBack = viewModel::closeSecondaryScreen,
            )

            SecondaryScreen.AiHistory -> AiHistoryScreen(
                uiState = uiState,
                onBack = viewModel::openSettings,
            )

            SecondaryScreen.Subtitles -> SubtitlesScreen(
                uiState = uiState,
                onBack = viewModel::closeSecondaryScreen,
                onRefresh = viewModel::refreshSubtitleLines,
                onWorkSelected = viewModel::selectWork,
                onEpisodeSelected = viewModel::selectEpisode,
                onFocusConsumed = viewModel::clearSubtitleFocus,
                onOpenSearch = viewModel::openSearch,
            )

            SecondaryScreen.SmartReviewQueue -> SmartReviewQueueScreen(
                plan = uiState.smartReviewPlan,
                onBack = viewModel::closeSecondaryScreen,
                onStartItem = viewModel::startSmartReviewItem,
            )

            // Never a page: the palette is drawn as an overlay by the shell.
            SecondaryScreen.Search -> Unit
        }

        is ShellRoute.Session -> when (route.kind) {
            TrainingSessionKind.Lesson -> LessonSessionScreen(
                uiState = uiState,
                onExit = viewModel::exitTrainingSession,
                onSubmitAnswer = viewModel::submitAnswer,
                onContinue = viewModel::continueLesson,
                onRestart = viewModel::restartLesson,
                onNextBatch = viewModel::startNextLessonBatch,
                onEvaluatePronunciation = viewModel::evaluatePronunciation,
                onRetryPronunciation = viewModel::retryPronunciationEvaluation,
                onResetPronunciation = viewModel::resetPronunciationEvaluation,
                onSkip = viewModel::skipLessonNode,
                onEyecatchPlayed = viewModel::markEyecatchPlayed,
            )

            TrainingSessionKind.ReadAir -> ReadAirSessionScreen(
                uiState = uiState,
                onExit = viewModel::exitTrainingSession,
                onAnswerSelected = viewModel::selectReadAirAnswer,
                onNext = viewModel::nextReadAirExercise,
                onRestart = viewModel::restartReadAirSession,
                onSkip = viewModel::skipReadAirExercise.takeIf { uiState.readAir.filteredExercises.size > 1 },
            )
        }

        is ShellRoute.Main -> when (route.tab) {
            LabTab.Today -> TodayScreen(
                uiState = uiState,
                onStartLesson = viewModel::startLessonFromCurrentTab,
                onStartModeLesson = { mode -> viewModel.startLessonModeFromCurrentTab(mode) },
                onStartReadAir = viewModel::startReadAirForCurrentEpisode,
                onStartReview = viewModel::openSmartReviewQueue,
                onOpenLearn = { viewModel.selectLearnSection(LearnSection.Course) },
                onOpenSubtitles = viewModel::openSubtitles,
                onOpenSearch = viewModel::openSearch,
                onOpenSettings = viewModel::openSettings,
                onTodayLineRevealed = viewModel::markTodayLineRevealed,
            )

            LabTab.Learn -> LearnScreen(
                uiState = uiState,
                onSectionSelected = viewModel::selectLearnSection,
                onStartLesson = viewModel::startLesson,
                onStartModeLesson = { mode, batch, pathNodeKey ->
                    viewModel.startLessonModeFromCurrentTab(mode, batch, pathNodeKey)
                },
                onStartExercise = viewModel::startExerciseLab,
                onStartExerciseMix = viewModel::startExerciseLabMix,
                onStartReadAirBatch = viewModel::startReadAirPathBatch,
                onStartReview = viewModel::openSmartReviewQueue,
                onWorkSelected = viewModel::selectWork,
                onEpisodeSelected = viewModel::selectEpisode,
                onTrackSelected = viewModel::selectLinguisticsTrack,
                readAir = ReadAirHomeActions(
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
                ),
                foundation = FoundationActions(
                    onRefresh = viewModel::refreshFoundationCatalog,
                    onPackSelected = viewModel::selectFoundationPack,
                    onDomainSelected = viewModel::selectFoundationDomain,
                    onTopicSelected = viewModel::selectFoundationTopic,
                    onStageSelected = viewModel::selectFoundationStage,
                    onAnswerSelected = viewModel::submitFoundationAnswer,
                    onPrevious = viewModel::previousFoundationQuestion,
                    onNext = viewModel::nextFoundationQuestion,
                    onRestart = viewModel::restartFoundationQuestions,
                ),
                onOpenSearch = viewModel::openSearch,
            )

            LabTab.Library -> LibraryScreen(
                uiState = uiState,
                onWorkSelected = viewModel::selectWork,
                onEpisodeSelected = viewModel::selectEpisode,
                onStartLesson = viewModel::startLessonFromCurrentTab,
                onStartModeLesson = { mode -> viewModel.startLessonModeFromCurrentTab(mode) },
                onStartReadAir = { viewModel.selectLearnSection(LearnSection.Linguistics) },
                onOpenSubtitles = viewModel::openSubtitles,
                onOpenSettings = viewModel::openSettings,
                onTargetLesson = viewModel::startTargetLesson,
                onAskAi = viewModel::askAiAboutLibraryItem,
                onOpenSearch = viewModel::openSearch,
            )

            LabTab.Review -> ReviewScreen(
                uiState = uiState,
                onOpenLesson = { viewModel.selectLearnSection(LearnSection.Course) },
                onOpenSmartReviewQueue = viewModel::openSmartReviewQueue,
                onMistakeReviewed = viewModel::markMistakeReviewed,
                onPracticeMistake = viewModel::practiceLocalMistake,
                onPracticeRemoteTask = viewModel::practiceReviewTask,
                onExplainMistake = viewModel::askAiAboutMistake,
                onViewSource = viewModel::openSubtitlesAt,
                onOpenSearch = viewModel::openSearch,
                onOpenSettings = viewModel::openSettings,
            )
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
