package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.components.PrimaryButton
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton

/**
 * 设置与登录相关输入框的统一配色：圆角 shapes.small、聚焦色 primary、
 * 标签/占位/帮助文本按弱层级递减。同包内其他屏幕可复用。
 */
@Composable
internal fun labFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
internal fun AccountLoginForm(
    email: String,
    password: String,
    isLoggingIn: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onRefreshAuth: () -> Unit,
) {
    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val canLogin = email.isNotBlank() && password.isNotBlank() && !isLoggingIn

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        enabled = !isLoggingIn,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp),
        label = { Text("邮箱") },
        placeholder = { Text("you@example.com") },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = labFieldColors(),
        leadingIcon = {
            Icon(Icons.Rounded.AlternateEmail, contentDescription = null)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { passwordFocusRequester.requestFocus() },
        ),
        trailingIcon = {
            if (email.isNotBlank() && !isLoggingIn) {
                IconButton(onClick = { onEmailChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = "清空邮箱")
                }
            }
        },
    )
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        enabled = !isLoggingIn,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .focusRequester(passwordFocusRequester),
        label = { Text("密码") },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = labFieldColors(),
        leadingIcon = {
            Icon(Icons.Rounded.Lock, contentDescription = null)
        },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                if (canLogin) {
                    focusManager.clearFocus()
                    onLogin()
                }
            },
        ),
        trailingIcon = {
            if (password.isNotBlank() && !isLoggingIn) {
                IconButton(onClick = { onPasswordChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = "清空密码")
                }
            }
        },
    )
    PrimaryButton(
        text = if (isLoggingIn) "登录中…" else "登录",
        onClick = {
            focusManager.clearFocus()
            onLogin()
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = canLogin,
    )
    SecondaryButton(
        text = "刷新登录状态",
        onClick = onRefreshAuth,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoggingIn,
    )
}

@Composable
internal fun AdvancedConnectionFields(
    settings: LabSettings,
    onSettingsChange: (LabSettings) -> Unit,
) {
    OutlinedTextField(
        value = settings.apiBaseUrl,
        onValueChange = { onSettingsChange(settings.copy(apiBaseUrl = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("学习服务地址") },
        placeholder = { Text("https://…") },
        supportingText = { Text("云端课程与同步接口的根地址。") },
        leadingIcon = {
            Icon(Icons.Rounded.Cloud, contentDescription = null)
        },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = labFieldColors(),
    )
    OutlinedTextField(
        value = settings.ttsWorkerUrl,
        onValueChange = { onSettingsChange(settings.copy(ttsWorkerUrl = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("语音服务地址") },
        placeholder = { Text("https://…") },
        supportingText = { Text("日语发音合成服务的地址。") },
        leadingIcon = {
            Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null)
        },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = labFieldColors(),
    )
}
