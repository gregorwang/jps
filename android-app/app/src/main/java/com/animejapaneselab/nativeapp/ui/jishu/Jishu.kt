package com.animejapaneselab.nativeapp.ui.jishu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animejapaneselab.nativeapp.data.ConjugationDrillItem
import com.animejapaneselab.nativeapp.data.DrillProgress
import com.animejapaneselab.nativeapp.data.EpisodeContentCache
import com.animejapaneselab.nativeapp.data.EpisodeSelection
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.data.SubtitleLine
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One page of a 自習 sitting: the 課's 板書, then one card per anime line. */
sealed interface JishuPage {
    data object Board : JishuPage

    /** [item] is the line's annotation for this 課; [others] are other 課 found in the same line. */
    data class Card(val item: ConjugationDrillItem, val others: List<ConjugationDrillItem>) : JishuPage
}

/** The subtitle lines just before and after a card's line: the scene it sits in. */
data class SceneContext(val before: String, val after: String)

data class JishuSitting(
    val pointId: String,
    val pages: List<JishuPage>,
    val index: Int = 0,
    /** Cards sent to the back with もう一回 (sentence ids); each goes back at most once. */
    val requeued: Set<String> = emptySet(),
    val remembered: Int = 0,
) {
    val current: JishuPage? get() = pages.getOrNull(index)
    val isComplete: Boolean get() = index >= pages.size
    val cardCount: Int get() = pages.count { it is JishuPage.Card }
}

data class JishuState(
    /** Sentence ids marked 覚えた, per 課: "pointId::sentenceId". */
    val studied: Set<String> = emptySet(),
    val openBook: String? = null,
    val sitting: JishuSitting? = null,
    /** Scene context by sentence id, filled as episodes' subtitles arrive. */
    val context: Map<String, SceneContext> = emptyMap(),
    /** 遮る: hide the translation until tapped (second pass). */
    val cover: Boolean = false,
) {
    fun studiedIn(pointId: String, lines: List<ConjugationDrillItem>): Int =
        lines.distinctBy { it.sentenceId }.count { key(pointId, it.sentenceId) in studied }

    companion object {
        fun key(pointId: String, sentenceId: String) = "$pointId::$sentenceId"
    }
}

/**
 * 自習 state holder: which anime lines of each 課 have been learned (覚えた), the sitting in
 * progress, and the scene around each line (neighbouring subtitle lines, fetched per episode).
 * The 課 list, lines and 板書 come from ConjugationDrillViewModel.
 */
class JishuViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalLabStore(application)
    private val contentCache = EpisodeContentCache(application.filesDir)
    private val _state = MutableStateFlow(JishuState(studied = store.readJishuStudied(), cover = store.readJishuCover()))
    val state: StateFlow<JishuState> = _state.asStateFlow()
    private val loadingEpisodes = mutableSetOf<EpisodeSelection>()

    fun openBook(group: String) = _state.update { it.copy(openBook = group) }

    fun closeBook() = _state.update { it.copy(openBook = null) }

    fun toggleCover() = _state.update { it.copy(cover = !it.cover).also { s -> store.writeJishuCover(s.cover) } }

    /**
     * Starts a sitting of [pointId]: its 板書, then up to [SittingSize] lines not yet learned (in
     * curriculum order); once every line is learned, the weakest ones by drill progress.
     */
    fun startSitting(
        pointId: String,
        lines: List<ConjugationDrillItem>,
        allItems: List<ConjugationDrillItem>,
        progress: Map<String, DrillProgress>,
        hasBoard: Boolean,
    ) {
        val studied = _state.value.studied
        val unique = lines.sortedBy { it.sortOrder }.distinctBy { it.sentenceId }
        val fresh = unique.filter { JishuState.key(pointId, it.sentenceId) !in studied }
        val picked = fresh.ifEmpty { unique.sortedBy { progress[it.id]?.box ?: 0 } }.take(SittingSize)
        val bySentence = allItems.groupBy { it.sentenceId }
        val cards = picked.map { item ->
            JishuPage.Card(item, bySentence[item.sentenceId].orEmpty().filter { it.pointId != pointId }.distinctBy { it.pointId })
        }
        val pages = (if (hasBoard) listOf(JishuPage.Board) else emptyList()) + cards
        _state.update { it.copy(sitting = JishuSitting(pointId, pages)) }
        cards.map { it.item }.forEach(::loadContext)
    }

    fun next() = _state.update { s -> s.copy(sitting = s.sitting?.let { it.copy(index = it.index + 1) }) }

    /** 覚えた: this line of this 課 is learned. */
    fun remember() {
        val sitting = _state.value.sitting ?: return
        val card = sitting.current as? JishuPage.Card ?: return
        val studied = _state.value.studied + JishuState.key(sitting.pointId, card.item.sentenceId)
        store.writeJishuStudied(studied)
        StudyLog.recordStudy(getApplication())
        _state.update { it.copy(studied = studied, sitting = sitting.copy(index = sitting.index + 1, remembered = sitting.remembered + 1)) }
    }

    /** もう一回: the card goes to the back of the sitting once; a second もう一回 just moves on. */
    fun again() {
        if (_state.value.sitting?.current is JishuPage.Card) StudyLog.recordStudy(getApplication())
        requeue()
    }

    private fun requeue() = _state.update { s ->
        val sitting = s.sitting ?: return@update s
        val card = sitting.current as? JishuPage.Card ?: return@update s
        val id = card.item.sentenceId
        if (id in sitting.requeued) {
            s.copy(sitting = sitting.copy(index = sitting.index + 1))
        } else {
            s.copy(sitting = sitting.copy(pages = sitting.pages + card, index = sitting.index + 1, requeued = sitting.requeued + id))
        }
    }

    fun back() = _state.update { s ->
        val sitting = s.sitting ?: return@update s
        s.copy(sitting = sitting.copy(index = (sitting.index - 1).coerceAtLeast(0)))
    }

    fun endSitting() = _state.update { it.copy(sitting = null) }

    private fun loadContext(item: ConjugationDrillItem) {
        if (item.sentenceId in _state.value.context) return
        val selection = episodeOf(item.sentenceId) ?: return
        synchronized(loadingEpisodes) { if (!loadingEpisodes.add(selection)) return }
        viewModelScope.launch {
            val client = RemoteLabClient(store.readSettings().apiBaseUrl, store.readSessionCookie(), contentCache = contentCache)
            val lines = withContext(Dispatchers.IO) { runCatching { client.fetchSubtitleLines(selection) }.getOrNull() }
            synchronized(loadingEpisodes) { loadingEpisodes.remove(selection) }
            if (lines.isNullOrEmpty()) return@launch
            val inEpisode = _state.value.sitting?.pages.orEmpty().filterIsInstance<JishuPage.Card>()
                .map { it.item }.filter { episodeOf(it.sentenceId) == selection }
            val found = sceneContexts(lines, inEpisode)
            _state.update { it.copy(context = it.context + found) }
        }
    }

    companion object {
        const val SittingSize = 8

        private val EpisodePattern = Regex("""^(.+?)-s(\d+)e(\d+)-""")

        /** `re-zero-s02e15-sentence-012` → re-zero, episode 40 (S2 counts on from S1's 25). */
        fun episodeOf(sentenceId: String): EpisodeSelection? {
            val match = EpisodePattern.find(sentenceId) ?: return null
            val (slug, season, episode) = match.destructured
            val offset = when (season.toInt()) {
                1 -> 0
                2 -> 25
                3 -> 50
                else -> return null
            }
            return EpisodeSelection(slug, offset + episode.toInt())
        }

        /** Neighbouring lines of each item that belongs to [lines]' episode, matched by start time, then by text. */
        fun sceneContexts(lines: List<SubtitleLine>, items: List<ConjugationDrillItem>): Map<String, SceneContext> {
            fun norm(s: String) = s.filterNot { it.isWhitespace() }
            val byTime = lines.withIndex().associateBy({ it.value.startTime.trim() }, { it.index })
            val byText = lines.withIndex().associateBy({ norm(it.value.jaText) }, { it.index })
            return items.mapNotNull { item ->
                val timed = byTime[item.startTime.trim()]
                val at = timed?.takeIf { norm(lines[it].jaText) == norm(item.jaText) }
                    ?: byText[norm(item.jaText)]
                    ?: timed
                    ?: return@mapNotNull null
                item.sentenceId to SceneContext(
                    before = lines.getOrNull(at - 1)?.jaText?.trim().orEmpty(),
                    after = lines.getOrNull(at + 1)?.jaText?.trim().orEmpty(),
                )
            }.toMap()
        }
    }
}

/** One segment of a line's 拆解: 「言っ」 with 「言う · ワ行五段 · 连用形促音便」. */
data class FormulaPart(val word: String, val note: String)

/**
 * Splits a drill 拆解 like `言っ（言う·ワ行五段·连用形促音便）＋ちゃっ（ちゃう←てしまう）＋た（过去）`
 * into word + note parts (＋ outside brackets separates parts). Returns empty when the formula
 * does not follow that shape, so the caller can show it as plain text.
 */
fun parseFormula(formula: String): List<FormulaPart> {
    val parts = mutableListOf<String>()
    val current = StringBuilder()
    var depth = 0
    formula.trim().forEach { c ->
        when (c) {
            '（', '(' -> { depth++; current.append(c) }
            '）', ')' -> { depth = (depth - 1).coerceAtLeast(0); current.append(c) }
            '＋', '+' -> if (depth == 0) { parts += current.toString(); current.clear() } else current.append(c)
            else -> current.append(c)
        }
    }
    parts += current.toString()
    val parsed = parts.map { it.trim() }.filter { it.isNotEmpty() }.map { part ->
        val open = part.indexOfFirst { it == '（' || it == '(' }
        if (open <= 0) FormulaPart(part, "") else {
            val close = part.indexOfLast { it == '）' || it == ')' }.takeIf { it > open } ?: part.length
            FormulaPart(
                word = part.substring(0, open).trim(),
                note = part.substring(open + 1, close).split('·', '・').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · "),
            )
        }
    }
    return if (parsed.isEmpty() || parsed.all { it.note.isEmpty() }) emptyList() else parsed
}
