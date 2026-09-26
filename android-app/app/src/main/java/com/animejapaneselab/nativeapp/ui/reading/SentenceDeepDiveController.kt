package com.animejapaneselab.nativeapp.ui.reading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.animejapaneselab.nativeapp.data.AiExplainResult
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DeepDiveTarget(
    val workSlug: String,
    val episode: Int,
    val lineNo: Int,
    val jaText: String,
    val zhText: String = "",
)

sealed interface DeepDiveState {
    data object Hidden : DeepDiveState
    data class Loading(val target: DeepDiveTarget) : DeepDiveState
    data class Ready(val target: DeepDiveTarget, val result: AiExplainResult) : DeepDiveState
    data class Error(val target: DeepDiveTarget, val message: String) : DeepDiveState
}

/**
 * Feature-owned state holder for the sentence deep-dive flow. Screens call
 * [request] with a line and render [SentenceDeepDiveSheet] once; nothing goes
 * through LabViewModel.
 */
@Stable
class SentenceDeepDiveController internal constructor(
    private val scope: CoroutineScope,
    private val store: LocalLabStore,
    private val settingsProvider: () -> LabSettings,
) {
    var state by mutableStateOf<DeepDiveState>(DeepDiveState.Hidden)
        private set

    fun request(target: DeepDiveTarget) {
        if (target.jaText.isBlank()) return
        val current = state
        if (current is DeepDiveState.Loading && current.target == target) return
        state = DeepDiveState.Loading(target)
        scope.launch {
            val settings = settingsProvider()
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    RemoteLabClient(settings.apiBaseUrl, store.readSessionCookie()).fetchSentenceDeepDive(
                        workSlug = target.workSlug,
                        episode = target.episode,
                        lineNo = target.lineNo,
                        jaText = target.jaText,
                        zhText = target.zhText,
                        model = settings.aiModel,
                        reasoningEffort = settings.reasoningEffort,
                        deviceId = store.deviceId(),
                    )
                }
            }
            val latest = state
            if (latest !is DeepDiveState.Loading || latest.target != target) return@launch
            state = outcome.fold(
                onSuccess = { DeepDiveState.Ready(target, it) },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    DeepDiveState.Error(target, error.message ?: "精读请求失败")
                },
            )
        }
    }

    fun dismiss() {
        state = DeepDiveState.Hidden
    }
}

@Composable
fun rememberSentenceDeepDive(settings: LabSettings): SentenceDeepDiveController {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val store = remember(context) { LocalLabStore(context) }
    val latestSettings = rememberUpdatedState(settings)
    return remember(scope, store) {
        SentenceDeepDiveController(scope, store) { latestSettings.value }
    }
}
