package com.animejapaneselab.nativeapp.ui.voicepack

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.PromptAudio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VoiceKind(val label: String) { Original("原声"), Emilia("エミリア"), Tts("TTS") }

/**
 * One voice preference for every line in the App: 原声 / エミリア (voice pack) / TTS, swiped on any
 * voice pill and remembered. A line only offers the voices it has; when the preferred one is missing
 * it falls back to エミリア, then to whatever is first (原声, else TTS). Default エミリア.
 */
object VoiceChoice {
    // default エミリア (user 2026-10-03); switched on any voice pill and remembered
    private val _preferred = MutableStateFlow(VoiceKind.Emilia)
    val preferred: StateFlow<VoiceKind> = _preferred.asStateFlow()
    @Volatile private var loaded = false

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val store = LocalLabStore(context)
        _preferred.value = store.readVoiceChoice()?.let { name -> VoiceKind.entries.firstOrNull { it.name == name } }
            ?: VoiceKind.Emilia
    }

    fun set(context: Context, kind: VoiceKind) {
        _preferred.value = kind
        LocalLabStore(context).writeVoiceChoice(kind.name)
    }

    fun available(context: Context, text: String, hasSource: Boolean): List<VoiceKind> = buildList {
        if (hasSource) add(VoiceKind.Original)
        if (text.isNotBlank() && VoicePack.fileFor(context, text) != null) add(VoiceKind.Emilia)
        add(VoiceKind.Tts)
    }

    fun pick(available: List<VoiceKind>, preferred: VoiceKind): VoiceKind = when {
        preferred in available -> preferred
        VoiceKind.Emilia in available -> VoiceKind.Emilia
        else -> available.first()
    }
}

/** The voices one line offers, which one is on, and how to switch (an index into [kinds], as the pill reports it). */
class VoiceOptions(val kinds: List<VoiceKind>, val selected: VoiceKind, val onSelect: (Int) -> Unit) {
    val labels: List<String> get() = kinds.map { it.label }
    val index: Int get() = kinds.indexOf(selected).coerceAtLeast(0)

    /** What to play: the clip for 原声, the pack (by [text]) for エミリア, plain TTS for TTS. */
    fun cue(source: PromptAudio?, text: String): PromptAudio = when (selected) {
        VoiceKind.Original -> source ?: PromptAudio.Tts(text, autoPlay = false)
        VoiceKind.Emilia -> PromptAudio.Tts(text, autoPlay = false)
        VoiceKind.Tts -> PromptAudio.Tts(text, autoPlay = false, voicePack = false)
    }
}

@Composable
fun rememberVoiceOptions(text: String, hasSource: Boolean): VoiceOptions {
    val context = LocalContext.current.applicationContext
    remember(context) { VoiceChoice.init(context) }
    val pack by VoicePack.state.collectAsState()
    val preferred by VoiceChoice.preferred.collectAsState()
    val kinds = remember(text, hasSource, pack.clips) { VoiceChoice.available(context, text.trim(), hasSource) }
    return VoiceOptions(kinds, VoiceChoice.pick(kinds, preferred)) { i ->
        kinds.getOrNull(i)?.let { VoiceChoice.set(context, it) }
    }
}
