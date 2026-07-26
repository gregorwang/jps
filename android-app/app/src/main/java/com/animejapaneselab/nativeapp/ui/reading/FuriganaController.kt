package com.animejapaneselab.nativeapp.ui.reading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.animejapaneselab.nativeapp.data.FuriganaCache
import com.animejapaneselab.nativeapp.data.FuriganaResult
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen-scoped furigana loader: local cache first, then batched Worker requests.
 * Failures stay silent — callers keep rendering plain text and a later [request]
 * for the same texts retries.
 *
 * Call [request] from a LaunchedEffect (not straight from composition); read
 * [resultFor] during composition — it is snapshot-backed and triggers recomposition
 * when annotations arrive.
 */
@Stable
class FuriganaAnnotator internal constructor(
    private val scope: CoroutineScope,
    private val cache: FuriganaCache,
    private val clientProvider: () -> RemoteLabClient,
) {
    private val results = mutableStateMapOf<String, FuriganaResult>()
    private val requested = mutableSetOf<String>()

    fun resultFor(text: String): FuriganaResult? = results[text]

    fun request(targetType: String, texts: List<String>) {
        val pending = mutableListOf<String>()
        texts.forEach { text ->
            if (text.isBlank() || text.length > MaxTextLength) return@forEach
            if (results.containsKey(text) || !requested.add(text)) return@forEach
            val cached = cache.read(text)
            if (cached != null) {
                results[text] = cached
            } else {
                pending += text
            }
        }
        if (pending.isEmpty()) return
        scope.launch {
            pending.chunked(BatchSize).forEach { chunk ->
                val items = chunk.withIndex().associate { (index, text) -> "t$index" to text }
                val fetched = withContext(Dispatchers.IO) {
                    runCatching { clientProvider().fetchFuriganaBatch("sentence", items) }
                        .getOrElse { emptyMap() }
                }
                chunk.forEachIndexed { index, text ->
                    val result = fetched["t$index"]
                    if (result != null && result.plainText == text) {
                        results[text] = result
                        cache.write(text, result)
                    } else {
                        requested.remove(text)
                    }
                }
            }
        }
    }

    private companion object {
        const val BatchSize = 40
        const val MaxTextLength = 500
    }
}

@Composable
fun rememberFuriganaAnnotator(settings: LabSettings): FuriganaAnnotator {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val store = remember(context) { LocalLabStore(context) }
    val cache = remember(context) { FuriganaCache(context) }
    val apiBaseUrl = settings.apiBaseUrl
    return remember(scope, cache, store, apiBaseUrl) {
        FuriganaAnnotator(scope, cache) { RemoteLabClient(apiBaseUrl, store.readSessionCookie()) }
    }
}
