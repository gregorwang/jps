package com.animejapaneselab.nativeapp.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AiModelOption
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.platform.DeviceCapabilitySnapshot
import com.animejapaneselab.nativeapp.platform.formatRefreshRates
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.design.FilterPill
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.InkSwitch
import com.animejapaneselab.nativeapp.ui.design.LineRow
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.UnderlineTextField
import com.animejapaneselab.nativeapp.ui.feedback.FeedbackEvent
import com.animejapaneselab.nativeapp.ui.feedback.LocalFeedbackEngine
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val FallbackAiModels = listOf(
    AiModelOption("gemini-3.1-flash-lite", "Gemini 3.1 Flash Lite"),
    AiModelOption("gemini-3.5-flash", "Gemini 3.5 Flash"),
    AiModelOption("deepseek-v4-flash", "DeepSeek V4 Flash"),
    AiModelOption("deepseek-v4-pro", "DeepSeek V4 Pro"),
    AiModelOption("grok-4.3", "Grok 4.3"),
)

private const val ReasoningModel = "grok-4.3"
private val ReasoningEfforts = listOf("low" to "低", "medium" to "中", "high" to "高")

/** Which inline editor under a row is open (one at a time). */
private enum class Open { None, Model, Api, Voice, SoundTest, Device, Password }

private data class PasswordFeedback(val message: String, val isError: Boolean)

/**
 * 設定 (V3Settings): 学生証 on top, then Japanese-headed groups of 1px hairline rows. Every v2
 * setting survives; the copy is v3 (no greetings, no explanatory lines).
 */
@Composable
fun SettingsScreen(
    uiState: LabUiState,
    onSettingsChange: (LabSettings) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onRefreshAuth: () -> Unit,
    onRefreshDeviceCapabilities: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenPromotedNotificationSettings: () -> Unit,
    appUpdateContent: @Composable () -> Unit,
    onOpenAiHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val store = remember(appContext) { LocalLabStore(appContext) }
    val scope = rememberCoroutineScope()
    val feedback = LocalFeedbackEngine.current
    val settings = uiState.settings
    val apiBaseUrl = settings.apiBaseUrl
    val colors = AjlTheme.colors

    var open by rememberSaveable { mutableStateOf(Open.None) }
    fun toggle(target: Open) {
        open = if (open == target) Open.None else target
    }

    var audioController by remember { mutableStateOf<LessonAudioController?>(null) }
    DisposableEffect(Unit) { onDispose { audioController?.release() } }

    // 讲解模型：拉一次在线列表，失败或为空时回退到内置列表。
    var remoteModels by remember { mutableStateOf<List<AiModelOption>>(emptyList()) }
    var modelsLoading by remember { mutableStateOf(false) }
    LaunchedEffect(apiBaseUrl) {
        modelsLoading = true
        val outcome = runCatching {
            withContext(Dispatchers.IO) { RemoteLabClient(apiBaseUrl, store.readSessionCookie()).fetchAiModels() }
        }
        val failure = outcome.exceptionOrNull()
        if (failure is CancellationException) throw failure
        remoteModels = outcome.getOrNull().orEmpty()
        modelsLoading = false
    }
    val modelOptions = remember(remoteModels) { remoteModels.ifEmpty { FallbackAiModels } }
    val modelLabel = modelOptions.firstOrNull { it.id == settings.aiModel }?.label ?: settings.aiModel

    // 修改密码：字段只留在内存里，换账号或离开设置即丢弃。
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordSubmitting by remember { mutableStateOf(false) }
    var passwordFeedback by remember { mutableStateOf<PasswordFeedback?>(null) }
    LaunchedEffect(uiState.auth.user?.email) {
        if (open == Open.Password) open = Open.None
        oldPassword = ""
        newPassword = ""
        confirmPassword = ""
        passwordFeedback = null
    }
    val canSubmitPassword = oldPassword.isNotBlank() && newPassword.length >= 6 &&
        newPassword == confirmPassword && !passwordSubmitting
    val submitPassword: () -> Unit = {
        if (canSubmitPassword) {
            passwordSubmitting = true
            passwordFeedback = null
            scope.launch {
                val outcome = runCatching {
                    withContext(Dispatchers.IO) {
                        RemoteLabClient(apiBaseUrl, store.readSessionCookie()).changePassword(oldPassword, newPassword)
                    }
                }
                val failure = outcome.exceptionOrNull()
                if (failure is CancellationException) throw failure
                passwordSubmitting = false
                passwordFeedback = when {
                    failure != null -> PasswordFeedback(failure.changePasswordMessage(), isError = true)
                    outcome.getOrNull() == true -> {
                        oldPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                        open = Open.None
                        PasswordFeedback("密码已更新，其他设备已退出登录", isError = false)
                    }
                    else -> PasswordFeedback("网络异常，请稍后再试", isError = true)
                }
            }
        }
    }

    val capabilities = uiState.deviceCapabilities

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item(key = "top") { TopBar(title = "設定", onNav = onBack) }
        item(key = "card") {
            StudentIdCard(uiState = uiState, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp))
        }

        item(key = "manabi") {
            SettingsGroup("学び方") {
                ToggleRow("题目自动读音", settings.autoSpeak) { onSettingsChange(settings.copy(autoSpeak = it)) }
                ToggleRow("答题音效", settings.feedbackSounds) { onSettingsChange(settings.copy(feedbackSounds = it)) }
                ToggleRow("触觉反馈", settings.hapticsEnabled) { onSettingsChange(settings.copy(hapticsEnabled = it)) }
                ToggleRow("进场アイキャッチ", settings.richAnimationsEnabled) {
                    onSettingsChange(settings.copy(richAnimationsEnabled = it))
                }
                ToggleRow(
                    "放課後チャイム",
                    settings.studyReminder,
                    value = if (settings.studyReminder) "${settings.studyReminderHour}:00" else null,
                ) { enabled ->
                    onSettingsChange(settings.copy(studyReminder = enabled))
                    if (enabled && capabilities?.notificationsEnabled != true) onRequestNotificationPermission()
                }
                if (settings.studyReminder) {
                    LineRow(minHeight = 52.dp) {
                        RowLabel("提醒时间", Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReminderHours.forEach { hour ->
                                FilterPill(
                                    text = "$hour",
                                    selected = settings.studyReminderHour == hour,
                                    onClick = { onSettingsChange(settings.copy(studyReminderHour = hour)) },
                                )
                            }
                        }
                    }
                }
                DisclosureRow("试听音效", open == Open.SoundTest, onClick = { toggle(Open.SoundTest) })
                if (open == Open.SoundTest) {
                    InlinePanel {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlineButton("答对", onClick = { feedback?.emit(FeedbackEvent.AnswerCorrect(xp = 0)) }, compact = true)
                            OutlineButton("答错", onClick = { feedback?.emit(FeedbackEvent.AnswerWrong) }, compact = true)
                            OutlineButton("学完", onClick = { feedback?.emit(FeedbackEvent.LessonComplete) }, compact = true)
                        }
                    }
                }
            }
        }

        item(key = "yomi") {
            SettingsGroup("読み方") {
                ToggleRow("假名注音", settings.showFurigana) { onSettingsChange(settings.copy(showFurigana = it)) }
                ToggleRow("罗马音", settings.showRomaji) { onSettingsChange(settings.copy(showRomaji = it)) }
            }
        }

        item(key = "ai") {
            SettingsGroup("AI") {
                val modelValue = if (settings.aiModel == ReasoningModel) {
                    "$modelLabel · ${ReasoningEfforts.firstOrNull { it.first == settings.reasoningEffort }?.second ?: settings.reasoningEffort}"
                } else {
                    modelLabel
                }
                DisclosureRow("默认模型", open == Open.Model, value = modelValue, onClick = { toggle(Open.Model) })
                if (open == Open.Model) {
                    InlinePanel {
                        if (modelsLoading) LoadingDots()
                        val options = if (settings.aiModel.isNotBlank() && modelOptions.none { it.id == settings.aiModel }) {
                            modelOptions + AiModelOption(settings.aiModel, settings.aiModel)
                        } else {
                            modelOptions
                        }
                        options.forEach { option ->
                            ChoiceRow(
                                text = option.label,
                                selected = option.id == settings.aiModel,
                                onClick = { onSettingsChange(settings.copy(aiModel = option.id)) },
                            )
                        }
                    }
                }
                if (settings.aiModel == ReasoningModel) {
                    LineRow(minHeight = 52.dp) {
                        RowLabel("推理强度", Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReasoningEfforts.forEach { (id, label) ->
                                FilterPill(
                                    text = label,
                                    selected = settings.reasoningEffort == id,
                                    onClick = { onSettingsChange(settings.copy(reasoningEffort = id)) },
                                )
                            }
                        }
                    }
                }
                NavRow("讲解历史", onClick = onOpenAiHistory)
            }
        }

        item(key = "setsuzoku") {
            SettingsGroup("接続") {
                DisclosureRow("学习服务", open == Open.Api, value = hostOf(settings.apiBaseUrl), onClick = { toggle(Open.Api) })
                if (open == Open.Api) {
                    InlinePanel {
                        UnderlineTextField(
                            value = settings.apiBaseUrl,
                            onValueChange = { onSettingsChange(settings.copy(apiBaseUrl = it)) },
                            label = "学習サーバー",
                            gloss = "地址",
                            placeholder = "https://…",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        )
                    }
                }
                DisclosureRow("语音", open == Open.Voice, value = hostOf(settings.ttsWorkerUrl), onClick = { toggle(Open.Voice) })
                if (open == Open.Voice) {
                    InlinePanel {
                        UnderlineTextField(
                            value = settings.ttsWorkerUrl,
                            onValueChange = { onSettingsChange(settings.copy(ttsWorkerUrl = it)) },
                            label = "音声サーバー",
                            gloss = "地址",
                            placeholder = "https://…",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlineButton(
                                text = "试听发音",
                                compact = true,
                                onClick = {
                                    val controller = audioController ?: LessonAudioController(context).also { audioController = it }
                                    controller.speakText("これは日本語の音声テストです。", settings.ttsWorkerUrl)
                                },
                            )
                            val playback = audioController?.playbackState
                            if (playback != null && playback.message.isNotBlank()) {
                                Text(
                                    playback.message,
                                    style = AjlTheme.type.caption,
                                    color = if (playback.phase == AudioPlaybackPhase.Error) colors.bad else colors.ink3,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                ToggleRow("云同步", settings.cloudSync) { onSettingsChange(settings.copy(cloudSync = it)) }
                LineRow(onClick = if (uiState.sync.status == SyncStatus.Loading) null else onRefresh, minHeight = 48.dp) {
                    RowLabel("同步资料", Modifier.weight(1f))
                    when (uiState.sync.status) {
                        SyncStatus.Loading -> LoadingDots(delayMillis = 0)
                        SyncStatus.Error -> RowValue("失败 · 重试", color = colors.bad)
                        else -> RowValue(syncTimeLabel(uiState.sync.lastSyncedAt).ifBlank { "立即同步" })
                    }
                }
                ToggleRow("实时活动", settings.learningLiveUpdates, value = liveUpdateValue(capabilities)) { enabled ->
                    onSettingsChange(settings.copy(learningLiveUpdates = enabled))
                    if (enabled && capabilities?.notificationsEnabled != true) onRequestNotificationPermission()
                }
                if (capabilities?.notificationsEnabled == false) {
                    NavRow("授予通知权限", onClick = onRequestNotificationPermission)
                } else if (capabilities?.supportsPromotedOngoingRuntime == true && !capabilities.promotedNotificationsAllowed) {
                    NavRow("允许 Live Update", onClick = onOpenPromotedNotificationSettings)
                }
                if (uiState.auth.user != null) {
                    LineRow(onClick = if (uiState.auth.status == SyncStatus.Loading) null else onRefreshAuth) {
                        RowLabel("学籍", Modifier.weight(1f))
                        if (uiState.auth.status == SyncStatus.Loading) {
                            LoadingDots(delayMillis = 0)
                        } else {
                            RowValue(uiState.auth.user.email, color = if (uiState.auth.status == SyncStatus.Error) colors.bad else colors.ink2)
                        }
                    }
                    DisclosureRow("修改密码", open == Open.Password, onClick = {
                        toggle(Open.Password)
                        passwordFeedback = null
                    })
                    passwordFeedback?.let { fb ->
                        Text(
                            fb.message,
                            style = AjlTheme.type.caption,
                            color = if (fb.isError) colors.bad else colors.ok,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    if (open == Open.Password) {
                        InlinePanel {
                            PasswordForm(
                                oldPassword = oldPassword,
                                newPassword = newPassword,
                                confirmPassword = confirmPassword,
                                submitting = passwordSubmitting,
                                canSubmit = canSubmitPassword,
                                onOldChange = { oldPassword = it },
                                onNewChange = { newPassword = it },
                                onConfirmChange = { confirmPassword = it },
                                onSubmit = submitPassword,
                            )
                        }
                    }
                }
                LineRow(onClick = onLogout, divider = false) {
                    Text("退出登录", style = AjlTheme.type.body, color = colors.bad)
                }
            }
        }

        item(key = "tanmatsu") {
            SettingsGroup("端末") {
                DisclosureRow(
                    "本机",
                    open == Open.Device,
                    value = capabilities?.deviceName ?: "…",
                    onClick = {
                        toggle(Open.Device)
                        if (capabilities == null) onRefreshDeviceCapabilities()
                    },
                    divider = open != Open.Device,
                )
                if (open == Open.Device) {
                    DeviceRows(
                        capabilities = capabilities,
                        refreshing = uiState.deviceCapabilitiesRefreshing,
                        onRefresh = onRefreshDeviceCapabilities,
                    )
                }
            }
        }

        item(key = "update") {
            SettingsGroup("更新") { appUpdateContent() }
        }
    }
}

// ---------------------------------------------------------------------------
// 学生証
// ---------------------------------------------------------------------------

@Composable
private fun StudentIdCard(uiState: LabUiState, modifier: Modifier = Modifier) {
    val card = remember(uiState.auth.user, uiState.selection, uiState.works, uiState.progressItems) {
        studentCardInfo(uiState)
    }
    com.animejapaneselab.nativeapp.ui.design.StudentCard(
        rows = card.rows,
        number = card.number,
        modifier = modifier,
    )
}

// ---------------------------------------------------------------------------
// Rows (private; proposed for ui/design in v3-requests/A.md)
// ---------------------------------------------------------------------------

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 22.dp),
    ) {
        Text(
            title,
            style = AjlTheme.type.jpTitle.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = AjlTheme.colors.ink,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        content()
    }
}

@Composable
private fun RowLabel(text: String, modifier: Modifier = Modifier, color: Color = AjlTheme.colors.ink) {
    Text(text, style = AjlTheme.type.body, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun RowValue(text: String, color: Color = AjlTheme.colors.ink2) {
    Text(
        text,
        style = AjlTheme.type.meta.copy(fontSize = 12.sp),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 12.dp),
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    value: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowLabel(label, Modifier.weight(1f))
            if (value != null) {
                RowValue(value)
                androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
            }
            InkSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
        Hairline()
    }
}

@Composable
private fun DisclosureRow(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    value: String? = null,
    divider: Boolean = !expanded,
) {
    LineRow(onClick = onClick, divider = divider) {
        RowLabel(label, Modifier.weight(1f))
        if (value != null) RowValue(value)
        Icon(
            if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "收起" else "展开",
            tint = AjlTheme.colors.ink3,
            modifier = Modifier.padding(start = 8.dp).size(18.dp),
        )
    }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit, value: String? = null) {
    LineRow(onClick = onClick) {
        RowLabel(label, Modifier.weight(1f))
        if (value != null) RowValue(value)
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = AjlTheme.colors.ink3,
            modifier = Modifier.padding(start = 8.dp).size(18.dp),
        )
    }
}

@Composable
private fun ChoiceRow(text: String, selected: Boolean, onClick: () -> Unit) {
    LineRow(onClick = onClick, minHeight = 44.dp, divider = false) {
        Text(
            text,
            style = AjlTheme.type.body.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) AjlTheme.colors.ink else AjlTheme.colors.ink2,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = "已选", tint = AjlTheme.work.accent, modifier = Modifier.size(18.dp))
        }
    }
}

/** Inline editor under an expanded row: no box, just an indent and the closing hairline. */
@Composable
private fun InlinePanel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        Hairline()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceRows(
    capabilities: DeviceCapabilitySnapshot?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
) {
    InlinePanel {
        if (capabilities == null) {
            if (refreshing) LoadingDots(delayMillis = 0) else Text("读取失败", style = AjlTheme.type.caption, color = AjlTheme.colors.ink3)
        } else {
            CapabilityLine("系统", capabilities.androidVersion)
            CapabilityLine(
                "显示",
                buildString {
                    append(capabilities.resolution)
                    append(" · %.0f Hz".format(capabilities.currentRefreshRateHz))
                    if (capabilities.adaptiveRefreshRate) append(" · ARR")
                    append(" · ")
                    append(formatRefreshRates(capabilities.supportedRefreshRatesHz))
                },
            )
            CapabilityLine(
                "触觉",
                when {
                    capabilities.envelopeHaptics -> "振动包络"
                    capabilities.amplitudeControl -> "振幅控制"
                    capabilities.hasVibrator -> "基础振动"
                    else -> "不可用"
                },
            )
            CapabilityLine(
                "超级岛",
                when {
                    !capabilities.supportsHyperOsIsland -> "未检测到"
                    capabilities.hyperOsFocusPermission == true -> "协议 3 · 已获焦点权限"
                    else -> "协议 3 · 未获焦点权限"
                },
            )
            CapabilityLine(
                "实时更新",
                when {
                    capabilities.supportsPromotedOngoingRuntime && capabilities.promotedNotificationsAllowed -> "API 36.1 · 已允许"
                    capabilities.supportsPromotedOngoingRuntime -> "API 36.1 · 待允许"
                    else -> "需要 API 36.1"
                },
            )
            CapabilityLine(
                "通知",
                "${if (capabilities.notificationsEnabled) "已启用" else "已关闭"} · ${capabilities.primaryAbi}",
            )
        }
        OutlineButton(
            text = if (refreshing) "读取中…" else "重新读取",
            onClick = onRefresh,
            enabled = !refreshing,
            compact = true,
        )
    }
}

@Composable
private fun CapabilityLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(label, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3, modifier = Modifier.width(64.dp))
        Text(value, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = AjlTheme.colors.ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PasswordForm(
    oldPassword: String,
    newPassword: String,
    confirmPassword: String,
    submitting: Boolean,
    canSubmit: Boolean,
    onOldChange: (String) -> Unit,
    onNewChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val mismatch = confirmPassword.isNotEmpty() && confirmPassword != newPassword
    val tooShort = newPassword.isNotEmpty() && newPassword.length < 6
    UnderlineTextField(
        value = oldPassword,
        onValueChange = onOldChange,
        label = "当前密码",
        enabled = !submitting,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
    )
    UnderlineTextField(
        value = newPassword,
        onValueChange = onNewChange,
        label = "新密码",
        placeholder = "至少 6 位",
        enabled = !submitting,
        error = if (tooShort) "至少 6 位" else null,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
    )
    UnderlineTextField(
        value = confirmPassword,
        onValueChange = onConfirmChange,
        label = "确认新密码",
        enabled = !submitting,
        error = if (mismatch) "两次输入不一致" else null,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            if (canSubmit) {
                focusManager.clearFocus()
                onSubmit()
            }
        }),
    )
    InkButton(
        text = "确认修改",
        onClick = {
            focusManager.clearFocus()
            onSubmit()
        },
        enabled = canSubmit || submitting,
        loading = submitting,
    )
}

// ---------------------------------------------------------------------------
// Pure helpers
// ---------------------------------------------------------------------------

internal data class StudentCardInfo(val rows: List<Pair<String, String>>, val number: String)

/** 学生証 rows from what the app already knows: account, current work/episode, progress dates. */
internal fun studentCardInfo(uiState: LabUiState): StudentCardInfo {
    val user = uiState.auth.user
    val name = user?.email?.substringBefore('@')?.ifBlank { null } ?: "—"
    val slug = uiState.selection.workSlug
    val workName = com.animejapaneselab.nativeapp.ui.design.WorkIdentity.displayName(
        slug,
        uiState.works.firstOrNull { it.slug == slug }?.displayName ?: slug,
    )
    val affiliation = listOf(
        workName,
        uiState.selection.episode.takeIf { it > 0 }?.let { com.animejapaneselab.nativeapp.ui.design.TextRules.episodeLabel(it) }.orEmpty(),
    ).filter { it.isNotBlank() }.joinToString(" ")
    val days = uiState.progressItems.mapNotNull { dayOf(it.lastReviewedAt) }.toSortedSet()
    val rows = buildList {
        add("氏名" to name)
        if (affiliation.isNotBlank()) add("所属" to affiliation)
        days.firstOrNull()?.let { add("入学" to it.replace('-', '.')) }
        if (days.isNotEmpty()) add("出席" to "${days.size} 日")
    }
    val digits = user?.id.orEmpty().filter { it.isLetterOrDigit() }.takeLast(4).uppercase()
    return StudentCardInfo(rows, "No. ${digits.ifBlank { "0001" }}")
}

/** `2026-09-24T12:00:00Z` → `2026-09-24`; anything unparseable → null. */
/** 放課後チャイム hours on offer: after school through late evening. */
private val ReminderHours = listOf(18, 20, 21, 22, 23)

internal fun dayOf(timestamp: String): String? {
    val day = timestamp.trim().take(10)
    return day.takeIf { Regex("""\d{4}-\d{2}-\d{2}""").matches(it) }
}

internal fun hostOf(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return "未设置"
    return trimmed.substringAfter("://").substringBefore('/').ifBlank { trimmed }
}

private fun syncTimeLabel(lastSyncedAt: String): String {
    val t = lastSyncedAt.trim()
    if (t.length < 16) return ""
    return runCatching {
        java.time.Instant.parse(t).atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault("")
}

private fun liveUpdateValue(capabilities: DeviceCapabilitySnapshot?): String? = when {
    capabilities == null -> null
    !capabilities.notificationsEnabled -> "通知未开"
    capabilities.supportsPromotedOngoingRuntime && capabilities.promotedNotificationsAllowed -> "胶囊"
    else -> null
}

/** changePassword 的 HTTP 异常翻成中文。 */
private fun Throwable.changePasswordMessage(): String {
    val raw = message.orEmpty()
    return when {
        raw.contains("401") -> "旧密码不正确"
        raw.contains("400") -> "新密码至少 6 位"
        else -> "网络异常，请稍后再试"
    }
}
