package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.platform.DeviceCapabilitySnapshot
import com.animejapaneselab.nativeapp.platform.formatRefreshRates
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackState
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterArtwork
import com.animejapaneselab.nativeapp.ui.components.CourseCharacterRole
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.PrimaryButton
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.LabSpacing
import com.animejapaneselab.nativeapp.ui.theme.LabTheme

private val AiModels = listOf(
    "gemini-3.1-flash-lite",
    "gemini-3.5-flash",
    "deepseek-v4-flash",
    "deepseek-v4-pro",
    "grok-4.3",
)

private val AiModelLabels = mapOf(
    "gemini-3.1-flash-lite" to "Gemini 3.1 Flash Lite",
    "gemini-3.5-flash" to "Gemini 3.5 Flash",
    "deepseek-v4-flash" to "DeepSeek V4 Flash",
    "deepseek-v4-pro" to "DeepSeek V4 Pro",
    "grok-4.3" to "Grok 4.3",
)

private val ReasoningEfforts = listOf("low", "medium", "high")

private val ReasoningEffortLabels = mapOf(
    "low" to "低",
    "medium" to "中",
    "high" to "高",
)

@Composable
private fun LoginWelcomePanel(
    workSlug: String,
    episode: Int,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        contentColor = LabTheme.colors.onHero,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .background(LabTheme.heroBrush())
                .padding(
                    start = LabSpacing.XLarge,
                    top = LabSpacing.XLarge,
                    end = LabSpacing.Medium,
                    bottom = LabSpacing.Large,
                ),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            ) {
                Surface(
                    color = LabTheme.colors.onHero.copy(alpha = 0.16f),
                    contentColor = LabTheme.colors.onHero,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = "NIHONGO LAB · ようこそ",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    text = "跟着动漫学日语",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "登录后同步课程、连胜和复盘进度。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LabTheme.colors.onHero.copy(alpha = 0.85f),
                )
            }
            CourseCharacterArtwork(
                workSlug = workSlug,
                role = CourseCharacterRole.Today,
                motionEnabled = motionEnabled,
                stableSeed = episode,
                modifier = Modifier.size(112.dp),
            )
        }
    }
}

@Composable
fun LoginGateScreen(
    uiState: LabUiState,
    onSettingsChange: (LabSettings) -> Unit,
    onLogin: (String, String) -> Unit,
    onRefreshAuth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var loginEmail by rememberSaveable { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var advancedOpen by rememberSaveable { mutableStateOf(false) }
    val isLoggingIn = uiState.auth.status == SyncStatus.Loading
    val reducedMotion = rememberReducedMotion()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(horizontal = LabSpacing.Screen, vertical = LabSpacing.XLarge),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
    ) {
        item(key = "login-hero") {
            LoginWelcomePanel(
                workSlug = uiState.selection.workSlug,
                episode = uiState.selection.episode,
                motionEnabled = uiState.settings.richAnimationsEnabled && !reducedMotion,
            )
        }
        item(key = "login-form") {
            LabCard(contentPadding = PaddingValues(LabSpacing.Large)) {
                Column(verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall)) {
                    Text(
                        text = "登录账号",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "保存你的学习路径和复盘进度。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                AuthStatusMessage(
                    message = uiState.auth.message,
                    isError = uiState.auth.status == SyncStatus.Error,
                )
                AccountLoginForm(
                    email = loginEmail,
                    password = loginPassword,
                    isLoggingIn = isLoggingIn,
                    onEmailChange = { loginEmail = it },
                    onPasswordChange = { loginPassword = it },
                    onLogin = { onLogin(loginEmail, loginPassword) },
                    onRefreshAuth = onRefreshAuth,
                )
            }
        }
        item(key = "login-connection") {
            SettingsSection {
                Column {
                    DisclosureHeader(
                        title = "连接设置",
                        subtitle = "服务地址异常时再调整。",
                        expanded = advancedOpen,
                        onToggle = { advancedOpen = !advancedOpen },
                    )
                    ExpandableSettingsContent(expanded = advancedOpen) {
                        AdvancedConnectionFields(
                            settings = uiState.settings,
                            onSettingsChange = onSettingsChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: LabUiState,
    onSettingsChange: (LabSettings) -> Unit,
    onRefresh: () -> Unit,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    onRefreshAuth: () -> Unit,
    onRefreshDeviceCapabilities: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenPromotedNotificationSettings: () -> Unit,
    appUpdateContent: @Composable () -> Unit,
    onOpenAiHistory: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val feedbackEngine = LocalFeedbackEngine.current
    var audioController by remember { mutableStateOf<LessonAudioController?>(null) }
    var advancedOpen by rememberSaveable { mutableStateOf(false) }
    var loginEmail by rememberSaveable { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            audioController?.release()
        }
    }

    LaunchedEffect(uiState.auth.user?.email) {
        if (uiState.auth.user != null) {
            loginEmail = ""
            loginPassword = ""
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(horizontal = LabSpacing.Screen, vertical = LabSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
    ) {
        item(key = "settings-top-bar") {
            SettingsTopBar(onBack = onBack)
        }
        item(key = "settings-hero") {
            SettingsHero(
                uiState = uiState,
                onRefresh = onRefresh,
            )
        }
        item(key = "app-update") {
            appUpdateContent()
        }
        item(key = "settings-device") {
            DeviceCapabilitySection(
                capabilities = uiState.deviceCapabilities,
                refreshing = uiState.deviceCapabilitiesRefreshing,
                onRefresh = onRefreshDeviceCapabilities,
            )
        }
        item(key = "settings-live-update") {
            LiveUpdateSettingsSection(
                enabled = uiState.settings.learningLiveUpdates,
                capabilities = uiState.deviceCapabilities,
                onEnabledChange = { enabled ->
                    onSettingsChange(uiState.settings.copy(learningLiveUpdates = enabled))
                    if (enabled && uiState.deviceCapabilities?.notificationsEnabled != true) {
                        onRequestNotificationPermission()
                    }
                },
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenPromotedNotificationSettings = onOpenPromotedNotificationSettings,
            )
        }
        item(key = "settings-account") {
            AccountSection(
                uiState = uiState,
                email = loginEmail,
                password = loginPassword,
                onEmailChange = { loginEmail = it },
                onPasswordChange = { loginPassword = it },
                onLogin = { onLogin(loginEmail, loginPassword) },
                onLogout = onLogout,
                onRefreshAuth = onRefreshAuth,
            )
        }
        item(key = "settings-ai-history") {
            SettingsSection(title = "学习记录") {
                SettingsNavigationRow(
                    icon = Icons.Rounded.History,
                    title = "AI 讲解历史",
                    subtitle = "回看问过的精讲、批改与角色画像。",
                    onClick = onOpenAiHistory,
                )
            }
        }
        item(key = "settings-experience") {
            SettingsSection(title = "学习体验") {
                SettingSwitch(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    title = "题目自动读音",
                    subtitle = "进入听力或跟读题时自动播放。",
                    checked = uiState.settings.autoSpeak,
                    onCheckedChange = { onSettingsChange(uiState.settings.copy(autoSpeak = it)) },
                )
                SettingSwitch(
                    icon = Icons.Rounded.GraphicEq,
                    title = "答题反馈音效",
                    subtitle = "答对、答错和完成训练时播放短反馈音。",
                    checked = uiState.settings.feedbackSounds,
                    onCheckedChange = { onSettingsChange(uiState.settings.copy(feedbackSounds = it)) },
                )
                SettingSwitch(
                    icon = Icons.Rounded.GraphicEq,
                    title = "触觉反馈",
                    subtitle = "按钮、答题、节点和结算使用编排震动。",
                    checked = uiState.settings.hapticsEnabled,
                    onCheckedChange = { onSettingsChange(uiState.settings.copy(hapticsEnabled = it)) },
                )
                SettingSwitch(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "富动画",
                    subtitle = "使用本地 Rive/Lottie 角色、节点和奖励动画。",
                    checked = uiState.settings.richAnimationsEnabled,
                    onCheckedChange = { onSettingsChange(uiState.settings.copy(richAnimationsEnabled = it)) },
                )
                SettingSwitch(
                    icon = Icons.Rounded.CloudSync,
                    title = "云端同步",
                    subtitle = "关闭后答案仍会保存在本机，重新开启时再补传。",
                    checked = uiState.settings.cloudSync,
                    onCheckedChange = { onSettingsChange(uiState.settings.copy(cloudSync = it)) },
                )
            }
        }
        item(key = "settings-reading") {
            SettingsSection(title = "阅读偏好") {
                SettingSwitch(
                    icon = Icons.Rounded.Translate,
                    title = "假名注音",
                    subtitle = "在字幕、跟读和例句里为汉字标注假名读音（AI 生成）。",
                    checked = uiState.settings.showFurigana,
                    onCheckedChange = { onSettingsChange(uiState.settings.copy(showFurigana = it)) },
                )
                SettingSwitch(
                    icon = Icons.Rounded.Abc,
                    title = "罗马音",
                    subtitle = "在假名读音之外额外显示罗马字。",
                    checked = uiState.settings.showRomaji,
                    onCheckedChange = { onSettingsChange(uiState.settings.copy(showRomaji = it)) },
                )
            }
        }
        item(key = "settings-sound") {
            SettingsSection(title = "声音") {
                PrimaryButton(
                    text = "测试云端日语发音",
                    onClick = {
                        val controller = audioController ?: LessonAudioController(context).also {
                            audioController = it
                        }
                        controller.speakText("これは日本語の音声テストです。", uiState.settings.ttsWorkerUrl)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TtsPlaybackStatus(audioController?.playbackState ?: AudioPlaybackState())
                FeedbackSoundTestPanel(
                    onPlayCorrect = { feedbackEngine?.emit(FeedbackEvent.AnswerCorrect(xp = 12)) },
                    onPlayWrong = { feedbackEngine?.emit(FeedbackEvent.AnswerWrong) },
                    onPlayCompletion = { feedbackEngine?.emit(FeedbackEvent.LessonComplete) },
                    onPlayNode = { feedbackEngine?.emit(FeedbackEvent.LessonNodeUnlock) },
                )
            }
        }
        item(key = "settings-ai") {
            SettingsSection(title = "智能讲解") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SettingIcon(Icons.Rounded.Psychology, containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("默认讲解模型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("错题复习和纠错统一使用这里的模型。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall)) {
                    AiModels.forEach { model ->
                        val selected = model == uiState.settings.aiModel
                        FilterChip(
                            selected = selected,
                            onClick = { onSettingsChange(uiState.settings.copy(aiModel = model)) },
                            label = {
                                Text(
                                    text = model.learningModelLabel(),
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            } else {
                                null
                            },
                            shape = MaterialTheme.shapes.small,
                            colors = settingsChipColors(),
                            border = settingsChipBorder(selected),
                        )
                    }
                }
                if (uiState.settings.aiModel == "grok-4.3") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Grok 推理强度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        Text("仅 Grok 4.3 生效；高强度更稳，低强度响应更轻。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall), modifier = Modifier.fillMaxWidth()) {
                            ReasoningEfforts.forEach { effort ->
                                val selected = effort == uiState.settings.reasoningEffort
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSettingsChange(uiState.settings.copy(reasoningEffort = effort)) },
                                    label = {
                                        Text(
                                            text = effort.reasoningEffortLabel(),
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            fontWeight = FontWeight.Bold,
                                        )
                                    },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    shape = MaterialTheme.shapes.small,
                                    colors = settingsChipColors(),
                                    border = settingsChipBorder(selected),
                                )
                            }
                        }
                    }
                }
            }
        }
        item(key = "settings-advanced") {
            SettingsSection {
                Column {
                    DisclosureHeader(
                        title = "高级连接",
                        subtitle = "云端地址和本机编号。正常学习不需要改。",
                        expanded = advancedOpen,
                        onToggle = { advancedOpen = !advancedOpen },
                    )
                    ExpandableSettingsContent(expanded = advancedOpen) {
                        AdvancedConnectionFields(
                            settings = uiState.settings,
                            onSettingsChange = onSettingsChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveUpdateSettingsSection(
    enabled: Boolean,
    capabilities: DeviceCapabilitySnapshot?,
    onEnabledChange: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenPromotedNotificationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(modifier = modifier, title = "学习实时状态") {
        SettingSwitch(
            icon = Icons.Rounded.NotificationsActive,
            title = "锁屏与状态栏学习进度",
            subtitle = "只在你主动进行训练时显示，退出训练后立即移除。",
            checked = enabled,
            onCheckedChange = onEnabledChange,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = when {
                    capabilities == null -> "正在检查通知权限。"
                    !capabilities.notificationsEnabled -> "普通通知权限尚未开启。"
                    capabilities.supportsPromotedOngoingRuntime && capabilities.promotedNotificationsAllowed ->
                        "Android 16 Live Update 已允许，可显示锁屏进度和状态栏胶囊。"
                    capabilities.supportsPromotedOngoingRuntime ->
                        "ProgressStyle 已可用；还需在系统设置中允许提升为 Live Update。"
                    else -> "当前系统使用普通进度通知；API 36.1 设备可进一步显示胶囊。"
                },
                modifier = Modifier.padding(horizontal = LabSpacing.Small, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (capabilities?.notificationsEnabled == false) {
            PrimaryButton(
                text = "授予通知权限",
                onClick = onRequestNotificationPermission,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (capabilities?.supportsPromotedOngoingRuntime == true &&
            !capabilities.promotedNotificationsAllowed
        ) {
            SecondaryButton(
                text = "打开 Live Update 权限",
                onClick = onOpenPromotedNotificationSettings,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DeviceCapabilitySection(
    capabilities: DeviceCapabilitySnapshot?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSection(modifier = modifier, title = "设备能力中心") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingIcon(Icons.Rounded.PhoneAndroid)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = capabilities?.deviceName ?: "正在读取当前设备",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = capabilities?.androidVersion ?: "检查 Android 与 HyperOS 能力",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(onClick = onRefresh, enabled = !refreshing) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                } else {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "刷新设备能力",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (capabilities == null) {
            Text(
                text = if (refreshing) "正在检查显示、触觉和系统通知能力……" else "暂时无法读取设备能力。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            return@SettingsSection
        }

        CapabilityRow(
            label = "显示",
            value = buildString {
                append(capabilities.resolution)
                append(" · 当前 %.0f Hz".format(capabilities.currentRefreshRateHz))
                if (capabilities.adaptiveRefreshRate) append(" · ARR")
            },
            detail = "可用刷新率：${formatRefreshRates(capabilities.supportedRefreshRatesHz)}；训练期间已请求高刷新率。",
        )
        CapabilityRow(
            label = "丰富触觉",
            value = when {
                capabilities.envelopeHaptics -> "支持 Android 16 振动包络"
                capabilities.amplitudeControl -> "支持振幅控制"
                capabilities.hasVibrator -> "基础振动"
                else -> "不可用"
            },
            detail = if (capabilities.envelopeHaptics) {
                "答对与答错可使用更细腻的强度、锐度曲线。"
            } else {
                "继续使用现有 HLA 波形和系统触觉回退。"
            },
        )
        CapabilityRow(
            label = "小米超级岛",
            value = when {
                !capabilities.supportsHyperOsIsland -> "未检测到 HyperOS 3 岛协议"
                capabilities.hyperOsFocusPermission == true -> "协议 3 · 当前应用已获焦点权限"
                else -> "协议 3 · 当前应用未获焦点权限"
            },
            detail = "超级岛仍受小米开发者平台、签名和白名单约束。",
        )
        CapabilityRow(
            label = "Android 实时更新",
            value = if (capabilities.supportsPromotedOngoingRuntime && capabilities.promotedNotificationsAllowed) {
                "API 36.1 · Live Update 已允许"
            } else if (capabilities.supportsPromotedOngoingRuntime) {
                "API 36.1 · 等待用户允许提升显示"
            } else {
                "需要 Android 16 QPR2 / API 36.1"
            },
            detail = if (capabilities.supportsPromotedOngoingRuntime) {
                "训练通知已使用 ProgressStyle，并请求 promoted ongoing 与状态栏短文本。"
            } else {
                "Android 16 基础版本使用 ProgressStyle，胶囊提升能力暂不可用。"
            },
        )
        CapabilityRow(
            label = "通知与架构",
            value = "通知${if (capabilities.notificationsEnabled) "已启用" else "已关闭"} · ${capabilities.primaryAbi}",
            detail = "设备能力页只读取状态，不会自行申请通知或超级岛权限。",
        )
    }
}

@Composable
private fun CapabilityRow(
    label: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = LabSpacing.Small, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                "账号、声音和讲解偏好",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SettingsHero(
    uiState: LabUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        contentColor = LabTheme.colors.onHero,
        shape = MaterialTheme.shapes.extraLarge,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .background(LabTheme.heroBrush())
                .padding(LabSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    color = LabTheme.colors.onHero.copy(alpha = 0.16f),
                    contentColor = LabTheme.colors.onHero,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "偏好",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = LabTheme.colors.onHero.copy(alpha = 0.8f),
                    )
                    Text("学习偏好", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        "声音、讲解和资料更新",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LabTheme.colors.onHero.copy(alpha = 0.85f),
                    )
                }
            }
            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LabTheme.colors.onHero,
                    contentColor = LabTheme.colors.heroGradientStart,
                ),
            ) {
                Text("更新资料", fontWeight = FontWeight.Black)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LabTheme.colors.onHero.copy(alpha = 0.14f),
                contentColor = LabTheme.colors.onHero,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    listOf(
                        if (uiState.settings.feedbackSounds) "音效已开启" else "音效已关闭",
                        if (uiState.settings.hapticsEnabled) "触觉已开启" else "触觉已关闭",
                        if (uiState.settings.richAnimationsEnabled) "富动画已开启" else "富动画已关闭",
                        "讲解 ${uiState.settings.aiModel.learningModelShortLabel()}",
                        uiState.sync.message,
                    ).joinToString(" · "),
                    modifier = Modifier.padding(horizontal = LabSpacing.Small, vertical = LabSpacing.XSmall),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AccountSection(
    uiState: LabUiState,
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onRefreshAuth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoggingIn = uiState.auth.status == SyncStatus.Loading
    val loggedIn = uiState.auth.user != null

    SettingsSection(modifier = modifier, title = "账号同步") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingIcon(
                Icons.Rounded.Key,
                containerColor = if (loggedIn) LabTheme.colors.successContainer else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (loggedIn) LabTheme.colors.onSuccessContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = uiState.auth.user?.email ?: "未登录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("进度按账号保存", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AuthStatusMessage(
            message = uiState.auth.message,
            isError = uiState.auth.status == SyncStatus.Error,
        )
        if (uiState.auth.user == null) {
            AccountLoginForm(
                email = email,
                password = password,
                isLoggingIn = isLoggingIn,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onLogin = onLogin,
                onRefreshAuth = onRefreshAuth,
            )
        } else {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("退出登录", fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun String.learningModelLabel(): String = AiModelLabels[this] ?: this

private fun String.learningModelShortLabel(): String {
    return learningModelLabel()
}

private fun String.reasoningEffortLabel(): String = ReasoningEffortLabels[this] ?: this

@Composable
private fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LabCard(modifier = modifier) {
        title?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 5.dp, height = 18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
        }
        content()
    }
}

/** 折叠区标题行：整行可点，展开状态用箭头指示。 */
@Composable
private fun DisclosureHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(
            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "收起" else "展开",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** 折叠内容容器：展开/收起动画统一走 MotionTokens，并尊重系统减弱动效。 */
@Composable
private fun ExpandableSettingsContent(
    expanded: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                easing = MotionTokens.Curve.Decelerate,
            ),
        ) + expandVertically(
            animationSpec = tween(
                durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                easing = MotionTokens.Curve.Standard,
            ),
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
                easing = MotionTokens.Curve.Standard,
            ),
        ) + shrinkVertically(
            animationSpec = tween(
                durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                easing = MotionTokens.Curve.Standard,
            ),
        ),
    ) {
        Column(
            modifier = Modifier.padding(top = LabSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.Small),
            content = content,
        )
    }
}

/** 账号状态提示：错误走 errorContainer 卡片，普通信息保持弱层级文本。 */
@Composable
private fun AuthStatusMessage(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    if (message.isBlank()) return
    if (isError) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = LabSpacing.Small, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    } else {
        Text(
            text = message,
            modifier = modifier,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun settingsChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurface,
    iconColor = MaterialTheme.colorScheme.primary,
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun settingsChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = MaterialTheme.colorScheme.outline,
    selectedBorderColor = MaterialTheme.colorScheme.primary,
)

@Composable
private fun TtsPlaybackStatus(
    playbackState: AudioPlaybackState,
    modifier: Modifier = Modifier,
) {
    if (playbackState.message.isBlank()) return
    if (playbackState.phase == AudioPlaybackPhase.Error) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = playbackState.message,
                modifier = Modifier.padding(horizontal = LabSpacing.Small, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    } else {
        Text(
            text = playbackState.message,
            modifier = modifier,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FeedbackSoundTestPanel(
    onPlayCorrect: () -> Unit,
    onPlayWrong: () -> Unit,
    onPlayCompletion: () -> Unit,
    onPlayNode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "反馈音试听",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "试听答对、答错、节点和完成的反馈音。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(
                text = "答对",
                onClick = onPlayCorrect,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "答错",
                onClick = onPlayWrong,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(
                text = "节点",
                onClick = onPlayNode,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "完成",
                onClick = onPlayCompletion,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** 设置内的跳转行：整行可点，右侧箭头表示进入二级页面。 */
@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(
            icon,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SettingIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(
        modifier = modifier.size(42.dp),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null)
        }
    }
}
