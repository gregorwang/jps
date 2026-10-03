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
import com.animejapaneselab.nativeapp.ui.foundation.LinguisticsTrack
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillViewModel
import com.animejapaneselab.nativeapp.ui.drill.DrillMode
import com.animejapaneselab.nativeapp.ui.jishu.JishuState
import com.animejapaneselab.nativeapp.ui.jishu.JishuViewModel
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillState
import com.animejapaneselab.nativeapp.ui.screens.jishu.splitTitle
import com.animejapaneselab.nativeapp.ui.screens.today.TodayMainLine
import com.animejapaneselab.nativeapp.ui.screens.jishu.JishuScreen
import com.animejapaneselab.nativeapp.platform.LearningSessionNotifier
import com.animejapaneselab.nativeapp.platform.LaunchRequests
import com.animejapaneselab.nativeapp.platform.ReminderTarget
import com.animejapaneselab.nativeapp.widget.TodayWidget
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.animejapaneselab.nativeapp.data.LocalLabStore
import androidx.core.app.NotificationManagerCompat
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
import com.animejapaneselab.nativeapp.ui.screens.review.ReviewFeedActions
import com.animejapaneselab.nativeapp.ui.screens.review.ReviewFeedScreen
import com.animejapaneselab.nativeapp.ui.review.ReviewSinks
import com.animejapaneselab.nativeapp.ui.screens.review.ReviewRules
import com.animejapaneselab.nativeapp.ui.screens.review.SmartReviewQueueScreen
import com.animejapaneselab.nativeapp.ui.screens.search.CommandPalette
import com.animejapaneselab.nativeapp.ui.screens.search.SearchScope
import com.animejapaneselab.nativeapp.ui.screens.search.SceneSearchScreen
import com.animejapaneselab.nativeapp.ui.search.SceneSearch
import com.animejapaneselab.nativeapp.ui.screens.library.DictEntrySheet
import com.animejapaneselab.nativeapp.ui.screens.library.DictTarget
import com.animejapaneselab.nativeapp.ui.review.ReviewFeed
import com.animejapaneselab.nativeapp.ui.screens.session.LessonSessionScreen
import com.animejapaneselab.nativeapp.ui.screens.session.ReadAirSessionScreen
import com.animejapaneselab.nativeapp.ui.screens.settings.AiHistoryScreen
import com.animejapaneselab.nativeapp.ui.screens.settings.SettingsScreen
import com.animejapaneselab.nativeapp.ui.screens.today.TodaySample
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
    // Each page searches its own things (原作 the lines, 辞書 its entries, 知識 the cards); 今日 searches everything.
    // Fixed when it opens, so a hit that changes page does not swap the panel while it fades out.
    val lastScope = remember { arrayOf(SearchScope.All) }
    val paletteScope = remember(paletteOpen) {
        if (!paletteOpen) return@remember lastScope[0]
        when {
            uiState.selectedTab == LabTab.Library -> SearchScope.Dict
            uiState.selectedTab == LabTab.Review -> SearchScope.Knowledge
            else -> SearchScope.All
        }.also { lastScope[0] = it }
    }
    // A 辞書 entry picked in search opens right here, over the page.
    var dictTarget by remember { mutableStateOf<DictTarget?>(null) }
    val route = shellRouteOf(secondaryScreen, activeSession, uiState.selectedTab, underlay)
    // A 自習 sitting (or its 小テスト) takes the whole screen, like a lesson.
    val jishu: JishuViewModel = viewModel()
    val jishuSitting by jishu.state.collectAsStateWithLifecycle()
    val drillForJishu: ConjugationDrillViewModel = viewModel()
    val drillSession by drillForJishu.state.collectAsStateWithLifecycle()
    // 第四巻 造語: a 課 in 自習, a practice in 練習 — also full screen.
    val zougoState by com.animejapaneselab.nativeapp.ui.zougo.Zougo.state.collectAsStateWithLifecycle()
    val katsuState by com.animejapaneselab.nativeapp.ui.katsuyou.Katsuyou.state.collectAsStateWithLifecycle()
    val jishuImmersive = (uiState.selectedTab == LabTab.Jishu &&
        (jishuSitting.sitting != null || zougoState.lesson != null || katsuState.lesson != null || (drillSession.mode == DrillMode.Lesson && drillSession.session.isNotEmpty()))) ||
        (uiState.selectedTab == LabTab.Learn && zougoState.practice != null)
    val closePalette: () -> Unit = {
        // Closing search returns to the page it was opened from (only 字幕 has an entry).
        if (underlay == SecondaryScreen.Subtitles) viewModel.openSubtitles() else viewModel.closeSecondaryScreen()
    }
    // 场景搜索 opened from 原作 goes back to 原作; from the palette, back to the tab.
    var scenesFromGensaku by remember { mutableStateOf(false) }
    val backFromScenes: () -> Unit = {
        if (scenesFromGensaku) viewModel.openSubtitles() else viewModel.closeSecondaryScreen()
    }
    val goBack: () -> Unit = {
        when {
            secondaryScreen == SecondaryScreen.AiHistory -> viewModel.openSettings()
            secondaryScreen == SecondaryScreen.SceneSearch -> backFromScenes()
            secondaryScreen != null -> viewModel.closeSecondaryScreen()
            activeSession != null -> viewModel.exitTrainingSession()
        }
    }

    TrainingFrameRateEffect(highFrameRate = activeSession != null)
    LearningSessionNotificationEffect(
        enabled = uiState.settings.learningLiveUpdates,
        // 自習 and 活用 are the main line now; they get the live card (and the island) too.
        status = uiState.learningSessionStatus()
            ?: buildJishuSessionStatus(
                jishuSitting.sitting,
                jishuSitting.sitting?.pointId?.let(drillSession::titleOf).orEmpty(),
                uiState.selection.workSlug,
            )
            ?: buildDrillSessionStatus(drillSession, uiState.selection.workSlug),
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

    // 放課後チャイム is on by default, so ask for the permission once instead of waiting for a toggle.
    LaunchedEffect(loggedIn) {
        if (!loggedIn || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val store = LocalLabStore(context)
        if (uiState.settings.studyReminder && !store.readNotificationPermissionAsked() &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            store.writeNotificationPermissionAsked()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Drill items feed the reminders (fading 課, tomorrow's 朝の一句), so load them at sign-in.
    LaunchedEffect(loggedIn) { if (loggedIn) drillForJishu.ensureLoaded() }

    // Leaving the app is when 復習 counts have usually changed: keep the widget honest.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { TodayWidget.refreshIfDueChanged(context) }

    // A tapped reminder opens its tab, unless a lesson is running.
    val launchTarget by LaunchRequests.target.collectAsStateWithLifecycle()
    LaunchedEffect(launchTarget, loggedIn) {
        val target = launchTarget ?: return@LaunchedEffect
        if (!loggedIn) return@LaunchedEffect
        if (activeSession == null) {
            if (secondaryScreen != null) viewModel.closeSecondaryScreen()
            viewModel.selectTab(
                when (target) {
                    ReminderTarget.Review -> LabTab.Review
                    ReminderTarget.Jishu -> LabTab.Jishu
                    ReminderTarget.Today -> LabTab.Today
                },
            )
        }
        LaunchRequests.consume()
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
                                if (route is ShellRoute.Main && !jishuImmersive) {
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
                                        onFindScenes = { query ->
                                            SceneSearch.search(context, query)
                                            scenesFromGensaku = true
                                            viewModel.openSceneSearch()
                                        },
                                        onBackFromScenes = backFromScenes,
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
                            scope = paletteScope,
                            uiState = uiState,
                            onDismiss = closePalette,
                            onOpenEntry = {
                                closePalette()
                                dictTarget = it
                            },
                            onOpenScenes = {
                                scenesFromGensaku = false
                                viewModel.openSceneSearch()
                            },
                            onOpenKnowledge = { card ->
                                ReviewFeed.show(context, card, java.time.LocalDate.now().toEpochDay())
                                closePalette()
                                viewModel.selectTab(LabTab.Review)
                            },
                        )
                        dictTarget?.let { target ->
                            DictEntrySheet(
                                target = target,
                                uiState = uiState,
                                onAskAi = viewModel::askAiAboutLibraryItem,
                                onTargetLesson = viewModel::startTargetLesson,
                                onDismiss = { dictTarget = null },
                            )
                        }
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
    onFindScenes: (query: String) -> Unit,
    onBackFromScenes: () -> Unit,
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
                onFindScenes = onFindScenes,
            )

            SecondaryScreen.SceneSearch -> SceneSearchScreen(
                uiState = uiState,
                onBack = onBackFromScenes,
                onOpenLine = viewModel::openSubtitlesAt,
            )

            SecondaryScreen.SmartReviewQueue -> SmartReviewQueueScreen(
                plan = uiState.smartReviewPlan,
                onBack = viewModel::closeSecondaryScreen,
                onStartItem = viewModel::startSmartReviewItem,
                weak = remember(uiState.progressItems) { ReviewRules.weakSpots(uiState.progressItems, java.time.LocalDate.now()) },
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
            LabTab.Today -> {
                val drill: ConjugationDrillViewModel = viewModel()
                val jishu: JishuViewModel = viewModel()
                val drillState by drill.state.collectAsStateWithLifecycle()
                val jishuState by jishu.state.collectAsStateWithLifecycle()
                TodayScreen(
                uiState = uiState,
                mainLine = remember(drillState, jishuState) { todayMainLine(drillState, jishuState) },
                onStartJishu = { point ->
                    jishu.requestStart(point)
                    viewModel.selectTab(LabTab.Jishu)
                },
                onStartPractice = {
                    viewModel.selectLinguisticsTrack(LinguisticsTrack.Conjugation)
                    viewModel.selectLearnSection(LearnSection.Linguistics)
                    drill.startReview()
                },
                onStartLesson = viewModel::startLessonFromCurrentTab,
                onStartModeLesson = { mode -> viewModel.startLessonModeFromCurrentTab(mode) },
                onStartReadAir = viewModel::startReadAirForCurrentEpisode,
                onStartReview = { viewModel.selectTab(LabTab.Review) },
                onOpenLearn = { viewModel.selectLearnSection(LearnSection.Course) },
                onOpenSubtitles = viewModel::openSubtitles,
                onOpenSearch = viewModel::openSearch,
                onOpenSettings = viewModel::openSettings,
                onTodayLineRevealed = viewModel::markTodayLineRevealed,
            )
            }

            LabTab.Jishu -> JishuScreen(ttsWorkerUrl = uiState.settings.ttsWorkerUrl, settings = uiState.settings)

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
                onViewSource = viewModel::openSubtitlesAt,
            )

            LabTab.Review -> {
                val drill: ConjugationDrillViewModel = viewModel()
                val drillState by drill.state.collectAsStateWithLifecycle()
                ReviewFeedScreen(
                    uiState = uiState,
                    drill = drillState,
                    actions = remember(viewModel, drill) {
                        ReviewFeedActions(
                            sinks = ReviewSinks(
                                gradeLine = drill::gradeLine,
                                markMistakeReviewed = viewModel::markMistakeReviewed,
                                gradeTask = viewModel::gradeReviewTask,
                            ),
                            ensureDrill = drill::ensureLoaded,
                            askAi = viewModel::askAiAboutLibraryItem,
                            practiceMistake = viewModel::practiceLocalMistake,
                            practiceTask = viewModel::practiceReviewTask,
                            practiceWeak = { category ->
                                when (category) {
                                    "语感 · 读空气" -> viewModel.startReadAirForCurrentEpisode()
                                    "基础题库" -> {
                                        viewModel.selectLinguisticsTrack(LinguisticsTrack.Foundation)
                                        viewModel.selectLearnSection(LearnSection.Linguistics)
                                    }
                                    else -> viewModel.selectLearnSection(LearnSection.Course)
                                }
                            },
                            viewSource = viewModel::openSubtitlesAt,
                            openKnownWords = { viewModel.selectTab(LabTab.Library) },
                            openSearch = viewModel::openSearch,
                            done = { viewModel.selectTab(LabTab.Today) },
                        )
                    },
                )
            }
        }
    }
}

/** 今日's 自習 / 練習 periods: the 課 to continue with its next line, and the 活用 lines due (null before any 課 is learned). */
private fun todayMainLine(drill: ConjugationDrillState, jishu: JishuState): TodayMainLine {
    val point = jishu.currentPoint(drill)
    val (headline, gloss) = point?.let { splitTitle(drill.titleOf(it)) } ?: ("" to "")
    val next = point?.let { p ->
        val lines = drill.linesOf(p).sortedBy { it.sortOrder }.distinctBy { it.sentenceId }
        lines.firstOrNull { JishuState.key(p, it.sentenceId) !in jishu.studied } ?: lines.firstOrNull()
    }
    val today = java.time.LocalDate.now().toEpochDay()
    val dueLine = drill.items.firstOrNull { it.pointId in drill.learned && (drill.progress[it.id]?.dueDay ?: Long.MAX_VALUE) <= today }
    fun sample(item: com.animejapaneselab.nativeapp.data.ConjugationDrillItem) = TodaySample(
        ja = item.jaText,
        zh = item.zh,
        mark = (item.spanStart until item.spanEnd).takeIf { item.spanStart in 0 until item.spanEnd && item.spanEnd <= item.jaText.length },
        audioUrl = item.audioUrl,
    )
    return TodayMainLine(
        jishuPoint = point,
        jishuLesson = point?.let { "第 ${drill.lessonNumber(it)} 課" },
        jishuHeadline = headline,
        jishuGloss = gloss,
        jishuStudied = point?.let { jishu.studiedIn(it, drill.linesOf(it)) } ?: 0,
        jishuTotal = point?.let { jishu.totalIn(drill, it) } ?: 0,
        jishuSample = next?.let(::sample),
        practiceDue = if (drill.learned.isEmpty()) null else drill.dueToday,
        practiceLessons = drill.learned.size,
        practiceSample = dueLine?.let(::sample),
    )
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
