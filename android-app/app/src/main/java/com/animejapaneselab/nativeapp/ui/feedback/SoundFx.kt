package com.animejapaneselab.nativeapp.ui.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.animejapaneselab.nativeapp.R

class SoundFx(context: Context) {
    private val appContext = context.applicationContext
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val sampleByRawId = mutableMapOf<Int, Int>()
    private val loadedSamples = mutableSetOf<Int>()
    private val pendingBySample = mutableMapOf<Int, SoundAsset>()
    private val fallbackBySample = mutableMapOf<Int, Int>()
    private var released = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (released) return@setOnLoadCompleteListener
            if (status == 0) {
                loadedSamples += sampleId
                fallbackBySample.remove(sampleId)
                pendingBySample.remove(sampleId)?.let { asset ->
                    playLoaded(sampleId, asset)
                }
            } else {
                val asset = pendingBySample.remove(sampleId)
                val fallbackRawId = fallbackBySample.remove(sampleId)
                if (asset != null && fallbackRawId != null) {
                    val fallbackSample = sampleByRawId.getOrPut(fallbackRawId) {
                        soundPool.load(appContext, fallbackRawId, 1)
                    }
                    if (fallbackSample in loadedSamples) {
                        playLoaded(fallbackSample, asset.copy(volume = asset.volume.coerceAtLeast(0.72f)))
                    } else {
                        pendingBySample[fallbackSample] = asset.copy(volume = asset.volume.coerceAtLeast(0.72f))
                    }
                }
            }
        }
    }

    fun play(asset: SoundAsset) {
        if (released) return
        val fallbackRawId = fallbackRawId(asset.rawName)
        val requestedRawId = appContext.soundRawResourceId(asset.rawName)
        val rawId = requestedRawId ?: fallbackRawId ?: return
        val sample = sampleByRawId.getOrPut(rawId) {
            soundPool.load(appContext, rawId, 1)
        }
        if (requestedRawId != null && fallbackRawId != null && requestedRawId != fallbackRawId) {
            fallbackBySample[sample] = fallbackRawId
        }
        if (sample in loadedSamples) {
            playLoaded(sample, asset)
        } else {
            pendingBySample[sample] = asset
        }
    }

    private fun playLoaded(sample: Int, asset: SoundAsset) {
        if (released) return
        soundPool.play(sample, asset.volume, asset.volume, 1, 0, asset.rate)
    }

    private fun fallbackRawId(rawName: String): Int? {
        return if (rawName.contains("incorrect") || rawName.contains("wrong")) {
            R.raw.feedback_error
        } else {
            R.raw.feedback_success
        }
    }

    fun release() {
        if (released) return
        released = true
        soundPool.setOnLoadCompleteListener(null)
        sampleByRawId.clear()
        loadedSamples.clear()
        pendingBySample.clear()
        fallbackBySample.clear()
        soundPool.release()
    }
}

private fun Context.soundRawResourceId(rawName: String): Int? {
    return when (rawName) {
        "right_answer" -> R.raw.right_answer
        "wrong_answer" -> R.raw.wrong_answer
        else -> rawResourceId(rawName)
    }
}

@Composable
fun rememberSoundFx(): SoundFx {
    val context = LocalContext.current
    val soundFx = remember(context) { SoundFx(context) }
    DisposableEffect(soundFx) {
        onDispose { soundFx.release() }
    }
    return soundFx
}
