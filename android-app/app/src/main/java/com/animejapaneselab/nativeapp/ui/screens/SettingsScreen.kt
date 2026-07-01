package com.animejapaneselab.nativeapp.ui.screens

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackState
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.audio.rememberFeedbackSoundController
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.PrimaryButton
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.components.SectionTitle
import com.animejapaneselab.nativeapp.ui.feedback.performAnswerFeedbackHaptic
import com.animejapaneselab.nativeapp.ui.feedback.performCompletionFeedbackHaptic

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
fun SettingsScreen(
    uiState: LabUiState,
    onSettingsChange: (LabSettings) -> Unit,
    onRefresh: () -> Unit,
    onSync: () -> Unit,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    onClaimDevice: () -> Unit,
    onRefreshAuth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val feedbackView = LocalView.current
    val feedbackSoundController = rememberFeedbackSoundController()
    var audioController by remember { mutableStateOf<LessonAudioController?>(null) }
    var advancedOpen by rememberSaveable { mutableStateOf(false) }
    var loginEmail by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }

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
        modifier = modifier.imePadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SettingsHero(
                uiState = uiState,
                onRefresh = onRefresh,
                onSync = onSync,
            )
        }
        item {
            AccountSection(
                uiState = uiState,
                email = loginEmail,
                password = loginPassword,
                onEmailChange = { loginEmail = it },
                onPasswordChange = { loginPassword = it },
                onLogin = { onLogin(loginEmail, loginPassword) },
                onLogout = onLogout,
                onClaimDevice = onClaimDevice,
                onRefreshAuth = onRefreshAuth,
            )
        }
        item {
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
                    icon = Icons.Rounded.CloudSync,
                    title = "同步学习进度",
                    subtitle = "答题后自动保存学习进度和复习队列。",
                    checked = uiState.settings.cloudSync,
                    onCheckedChange = { onSettingsChange(uiState.settings.copy(cloudSync = it)) },
                )
            }
        }
        item {
            SettingsSection(title = "声音") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PrimaryButton(
                        text = "测试日语发音",
                        onClick = {
                            val controller = audioController ?: LessonAudioController(context).also {
                                audioController = it
                            }
                            controller.speakText("これは日本語の音声テストです。", uiState.settings.ttsWorkerUrl)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = "安装语音",
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                TtsPlaybackStatus(audioController?.playbackState ?: AudioPlaybackState())
                FeedbackSoundTestPanel(
                    onPlayCorrect = {
                        feedbackSoundController.play(true)
                        feedbackView.performAnswerFeedbackHaptic(true)
                    },
                    onPlayWrong = {
                        feedbackSoundController.play(false)
                        feedbackView.performAnswerFeedbackHaptic(false)
                    },
                    onPlayCompletion = {
                        feedbackSoundController.playCompletion()
                        feedbackView.performCompletionFeedbackHaptic()
                    },
                )
            }
        }
        item {
            SettingsSection(title = "智能讲解") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SettingIcon(Icons.Rounded.Psychology, containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("默认讲解模型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text("词汇、语法、读空气和纠错统一使用这里的模型。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AiModels.forEach { model ->
                        FilterChip(
                            selected = model == uiState.settings.aiModel,
                            onClick = { onSettingsChange(uiState.settings.copy(aiModel = model)) },
                            label = { Text(model.learningModelLabel(), fontWeight = FontWeight.Bold) },
                        )
                    }
                }
                if (uiState.settings.aiModel == "grok-4.3") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Grok 推理强度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                        Text("仅 Grok 4.3 生效；高强度更稳，低强度响应更轻。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ReasoningEfforts.forEach { effort ->
                                FilterChip(
                                    selected = effort == uiState.settings.reasoningEffort,
                                    onClick = { onSettingsChange(uiState.settings.copy(reasoningEffort = effort)) },
                                    label = { Text(effort.reasoningEffortLabel(), fontWeight = FontWeight.Bold) },
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            SettingsSection {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("高级连接", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("云端地址和本机编号。正常学习不需要改。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { advancedOpen = !advancedOpen }) {
                        Icon(if (advancedOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
                    }
                }
                if (advancedOpen) {
                    AdvancedConnectionFields(
                        deviceId = uiState.deviceId,
                        settings = uiState.settings,
                        onSettingsChange = onSettingsChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHero(
    uiState: LabUiState,
    onRefresh: () -> Unit,
    onSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("偏好", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("学习偏好", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("同步、声音和讲解偏好", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HeroStatusPill(label = "同步", value = uiState.sync.status.settingsLabel(), modifier = Modifier.weight(1f))
                HeroStatusPill(label = "音效", value = if (uiState.settings.feedbackSounds) "已开启" else "已关闭", modifier = Modifier.weight(1f))
                HeroStatusPill(label = "讲解", value = uiState.settings.aiModel.learningModelShortLabel(), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSync,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
                ) {
                    Text("同步进度", fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("更新资料", fontWeight = FontWeight.Black)
                }
            }
            Text(
                uiState.sync.message,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
    onClaimDevice: () -> Unit,
    onRefreshAuth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoggingIn = uiState.auth.status == SyncStatus.Loading

    SettingsSection(title = "账号同步") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SettingIcon(Icons.Rounded.Key, containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = uiState.auth.user?.email ?: "未登录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("deviceId: ${uiState.deviceId}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            text = uiState.auth.message,
            color = if (uiState.auth.status == SyncStatus.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(text = "合并当前设备进度", onClick = onClaimDevice, modifier = Modifier.weight(1f))
                SecondaryButton(text = "退出登录", onClick = onLogout, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStatusPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.16f),
        contentColor = Color.White,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

private fun SyncStatus.settingsLabel(): String {
    return when (this) {
        SyncStatus.Idle -> "待同步"
        SyncStatus.Loading -> "同步中"
        SyncStatus.Success -> "已同步"
        SyncStatus.Error -> "需重试"
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
            Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        content()
    }
}

@Composable
private fun TtsPlaybackStatus(
    playbackState: AudioPlaybackState,
    modifier: Modifier = Modifier,
) {
    if (playbackState.message.isBlank()) return
    Text(
        text = playbackState.message,
        modifier = modifier,
        color = if (playbackState.phase == AudioPlaybackPhase.Error) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun FeedbackSoundTestPanel(
    onPlayCorrect: () -> Unit,
    onPlayWrong: () -> Unit,
    onPlayCompletion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "反馈音试听",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
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
        PrimaryButton(
            text = "测试完成音",
            onClick = onPlayCompletion,
            modifier = Modifier.fillMaxWidth(),
        )
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
