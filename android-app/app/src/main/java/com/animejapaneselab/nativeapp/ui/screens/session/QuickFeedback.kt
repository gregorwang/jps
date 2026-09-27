package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the drill knows about a wrong answer, sent to /api/ai/quick-feedback. */
internal data class QuickFeedbackRequest(
    val prompt: String,
    val sentence: String,
    val chosen: String,
    val answer: String,
    val point: String,
    val formula: String,
    val learned: List<String>,
)

internal sealed interface QuickFeedbackState {
    data object Loading : QuickFeedbackState
    data class Ready(val text: String) : QuickFeedbackState
    data object Unavailable : QuickFeedbackState
}

/** Fetches the one-line AI note once per [request]; null request means nothing to explain. */
@Composable
internal fun rememberQuickFeedback(request: QuickFeedbackRequest?): QuickFeedbackState {
    val context = LocalContext.current.applicationContext
    val state by produceState<QuickFeedbackState>(if (request == null) QuickFeedbackState.Unavailable else QuickFeedbackState.Loading, request) {
        if (request == null) return@produceState
        value = QuickFeedbackState.Loading
        val store = LocalLabStore(context)
        value = runCatching {
            withContext(Dispatchers.IO) {
                RemoteLabClient(store.readSettings().apiBaseUrl, store.readSessionCookie()).fetchQuickFeedback(
                    prompt = request.prompt,
                    sentence = request.sentence,
                    chosen = request.chosen,
                    answer = request.answer,
                    point = request.point,
                    formula = request.formula,
                    learned = request.learned,
                )
            }
        }.fold(
            onSuccess = { text -> if (text.isBlank()) QuickFeedbackState.Unavailable else QuickFeedbackState.Ready(text) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                QuickFeedbackState.Unavailable
            },
        )
    }
    return state
}

internal fun quickFeedbackLabel(chosen: String) = "AI · 为什么不是「$chosen」"

/** Placeholder row while the note is on its way. */
@Composable
internal fun QuickFeedbackLoading(chosen: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow(quickFeedbackLabel(chosen))
        LoadingDots(delayMillis = 0)
    }
}
