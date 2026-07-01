package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.components.PrimaryButton
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.components.TagChip

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

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        PrimaryButton(
            text = if (isLoggingIn) "登录中" else "登录",
            onClick = {
                focusManager.clearFocus()
                onLogin()
            },
            modifier = Modifier.weight(1f),
            enabled = canLogin,
        )
        SecondaryButton(
            text = "刷新状态",
            onClick = onRefreshAuth,
            modifier = Modifier.weight(1f),
            enabled = !isLoggingIn,
        )
    }
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        enabled = !isLoggingIn,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp),
        label = { Text("邮箱") },
        singleLine = true,
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
}

@Composable
internal fun AdvancedConnectionFields(
    deviceId: String,
    settings: LabSettings,
    onSettingsChange: (LabSettings) -> Unit,
) {
    TagChip(deviceId)
    OutlinedTextField(
        value = settings.apiBaseUrl,
        onValueChange = { onSettingsChange(settings.copy(apiBaseUrl = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("学习服务地址") },
        singleLine = true,
    )
    OutlinedTextField(
        value = settings.ttsWorkerUrl,
        onValueChange = { onSettingsChange(settings.copy(ttsWorkerUrl = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("语音服务地址") },
        singleLine = true,
    )
}
