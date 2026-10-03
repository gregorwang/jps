package com.animejapaneselab.nativeapp.ui.voicepack

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.animejapaneselab.nativeapp.data.AudioReliability
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.design.VoiceSwitchPill

/** The clip of a line from the anime, or null when it has none. */
fun lineSource(text: String, sourceUrl: String): PromptAudio.Source? = sourceUrl.takeIf { it.isNotEmpty() }?.let {
    PromptAudio.Source(it, autoPlay = false, reliability = AudioReliability.Verified, fallbackTtsText = text)
}

/** What a line plays right now under the remembered 原声 / エミリア / TTS choice (for auto-play, outside a pill). */
fun lineCue(context: Context, text: String, sourceUrl: String): PromptAudio {
    VoiceChoice.init(context)
    val kinds = VoiceChoice.available(context, text.trim(), sourceUrl.isNotEmpty())
    return VoiceOptions(kinds, VoiceChoice.pick(kinds, VoiceChoice.preferred.value)) {}.cue(lineSource(text, sourceUrl), text)
}

/** Tap = play / stop; swipe = 原声 / エミリア / TTS, remembered for every line in the App, and plays the new voice. */
@Composable
fun LineVoicePill(
    text: String,
    sourceUrl: String,
    audio: LessonAudioController,
    ttsWorkerUrl: String,
    modifier: Modifier = Modifier,
) {
    val voice = rememberVoiceOptions(text, hasSource = sourceUrl.isNotEmpty())
    val cue = remember(text, sourceUrl, voice.selected) { voice.cue(lineSource(text, sourceUrl), text) }
    var heard by remember(text) { mutableStateOf(voice.selected) }
    LaunchedEffect(voice.selected) {
        if (voice.selected != heard) {
            heard = voice.selected
            audio.play(cue, ttsWorkerUrl)
        }
    }
    VoiceSwitchPill(
        playing = audio.isSounding(cue),
        options = voice.labels,
        selected = voice.index,
        onSelect = voice.onSelect,
        onClick = { audio.toggle(cue, ttsWorkerUrl) },
        modifier = modifier,
    )
}
