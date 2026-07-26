package com.animejapaneselab.nativeapp.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.AiExplainResult
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.components.StructuredAiResultCard
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

/** Shared bottom sheet so every deep-dive entry point looks identical; render once per screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceDeepDiveSheet(controller: SentenceDeepDiveController) {
    val state = controller.state
    if (state is DeepDiveState.Hidden) return
    ModalBottomSheet(
        onDismissRequest = controller::dismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val target = when (state) {
                is DeepDiveState.Loading -> state.target
                is DeepDiveState.Ready -> state.target
                is DeepDiveState.Error -> state.target
                DeepDiveState.Hidden -> return@Column
            }
            Text(
                text = "单句精读",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = target.jaText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (target.zhText.isNotBlank()) {
                        Text(
                            text = target.zhText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            when (state) {
                is DeepDiveState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "正在逐词拆解这句台词…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is DeepDiveState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = "精读失败：${state.message}",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    SecondaryButton(
                        text = "重试",
                        onClick = { controller.request(target) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is DeepDiveState.Ready -> {
                    StructuredAiResultCard(
                        result = state.result,
                        fallbackText = state.result.text,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                DeepDiveState.Hidden -> Unit
            }
        }
    }
}
