package com.animejapaneselab.nativeapp.ui.search

import android.content.Context
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RagSearchResult
import com.animejapaneselab.nativeapp.data.RagSearchSource
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.data.SubtitleLine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 场景搜索: describe a scene (or type a line) and get the anime lines most like it, across every work.
 * One search feeds both the 今日 palette (first two) and the full 场景 page, so opening 「全部场景」
 * never waits twice. The worker grades each hit line (`explain`): its own translation (subtitle
 * Chinese is often misaligned), why it fits, the key words, and 0/1/2 relevance.
 */
object SceneSearch {
    data class State(
        val query: String = "",
        val loading: Boolean = false,
        val result: RagSearchResult? = null,
        val error: String? = null,
        /** A hit picked in the palette: the page opens its scene sheet. */
        val focusId: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    fun search(context: Context, query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        val now = _state.value
        if (q == now.query && (now.loading || now.result != null)) return
        run(context.applicationContext, q)
    }

    fun retry(context: Context) {
        val q = _state.value.query
        if (q.isNotBlank()) run(context.applicationContext, q)
    }

    fun focus(id: String?) {
        _state.value = _state.value.copy(focusId = id)
    }

    private fun run(context: Context, q: String) {
        job?.cancel()
        _state.value = State(query = q, loading = true)
        job = scope.launch {
            val store = LocalLabStore(context)
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    RemoteLabClient(store.readSettings().apiBaseUrl, store.readSessionCookie()).searchSubtitles(
                        query = q,
                        workSlug = "all",
                        deviceId = store.deviceId(),
                        topK = 6,
                        analyze = false,
                        explain = true,
                    )
                }
            }
            outcome.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            _state.value = _state.value.copy(
                loading = false,
                result = outcome.getOrNull(),
                error = outcome.exceptionOrNull()?.let { "没搜出来，再试一次" },
            )
        }
    }
}

/** What the page shows of one search. */
object SceneRules {
    /** The graded hits worth showing (relevance ≥ 1); without grading, the top few by score. */
    fun shown(result: RagSearchResult): List<RagSearchSource> {
        val graded = result.sources.filter { it.match != null }
        if (graded.isEmpty()) return result.sources.filter { hitLine(it) != null }.take(4)
        return graded.filter { (it.match?.relevance ?: 0) >= 1 }
    }

    /** Ungraded leftovers shown folded under 「最接近的」 when nothing fits. */
    fun nearest(result: RagSearchResult): List<RagSearchSource> =
        result.sources.filter { it.match != null && (it.match?.relevance ?: 0) == 0 && hitLine(it) != null }.take(3)

    fun hitLine(source: RagSearchSource): SubtitleLine? {
        val no = source.match?.lineNo ?: source.hitLineNos.minOrNull() ?: return null
        return source.lines.firstOrNull { it.lineNo == no }
    }

    /** The hit and up to [around] lines on each side, for the scene sheet's timeline. */
    fun window(source: RagSearchSource, around: Int = 2): List<SubtitleLine> {
        val hit = hitLine(source) ?: return emptyList()
        val at = source.lines.indexOf(hit)
        return source.lines.subList((at - around).coerceAtLeast(0), (at + around + 1).coerceAtMost(source.lines.size))
    }

    fun previous(source: RagSearchSource): SubtitleLine? {
        val hit = hitLine(source) ?: return null
        return source.lines.getOrNull(source.lines.indexOf(hit) - 1)
    }

    /** `00:28:37,507` → `28:37`. */
    fun clock(raw: String): String {
        val core = raw.substringBefore(',').substringBefore('.')
        val parts = core.split(':')
        return when {
            parts.size == 3 && parts[0].trimStart('0').isEmpty() -> "${parts[1]}:${parts[2]}"
            parts.size == 3 -> "${parts[0].trimStart('0')}:${parts[1]}:${parts[2]}"
            else -> core
        }
    }
}
