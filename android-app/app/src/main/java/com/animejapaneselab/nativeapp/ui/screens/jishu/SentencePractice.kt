package com.animejapaneselab.nativeapp.ui.screens.jishu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AiExplainResult
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.UnderlineTextField
import com.animejapaneselab.nativeapp.ui.screens.library.AiResultBlock
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface Correction {
    data object Idle : Correction
    data object Loading : Correction
    data class Done(val result: AiExplainResult) : Correction
    data class Failed(val message: String) : Correction
}

/**
 * つづく: write one sentence with the lesson's grammar and let AI correct it — from reading
 * anime lines to producing one. Optional; never blocks moving on.
 */
@Composable
internal fun SentencePractice(pointId: String, title: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var text by rememberSaveable(pointId) { mutableStateOf("") }
    var state by remember(pointId) { mutableStateOf<Correction>(Correction.Idle) }
    val submit: () -> Unit = submit@{
        val sentence = text.trim()
        if (sentence.isBlank() || state is Correction.Loading) return@submit
        state = Correction.Loading
        scope.launch {
            val store = LocalLabStore(context)
            val settings = store.readSettings()
            state = runCatching {
                withContext(Dispatchers.IO) {
                    RemoteLabClient(settings.apiBaseUrl, store.readSessionCookie()).correctSentence(
                        targetId = pointId,
                        targetLabel = title,
                        sentence = sentence,
                        model = settings.aiModel,
                        deviceId = store.deviceId(),
                    )
                }
            }.fold(
                onSuccess = { Correction.Done(it) },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    Correction.Failed("批改失败，稍后再试")
                },
            )
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "用这一课造一句",
            style = AjlTheme.type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = AjlTheme.colors.ink,
        )
        UnderlineTextField(
            value = text,
            onValueChange = {
                text = it
                if (state !is Correction.Loading) state = Correction.Idle
            },
            label = "作文",
            gloss = title,
            placeholder = "日本語で一文",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            trailing = {
                if (state is Correction.Loading) {
                    LoadingDots(delayMillis = 0)
                } else {
                    OutlineButton("批改", submit, enabled = text.isNotBlank(), compact = true)
                }
            },
        )
        when (val s = state) {
            is Correction.Done -> AiResultBlock(result = s.result, fallback = "", eyebrow = "批改 · AI")
            is Correction.Failed -> AiResultBlock(result = null, fallback = s.message, isError = true, eyebrow = "批改 · AI")
            else -> Unit
        }
    }
}
