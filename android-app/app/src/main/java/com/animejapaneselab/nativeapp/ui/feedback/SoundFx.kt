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
        .setMaxStreams(4)
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
    private var released = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (released) return@setOnLoadCompleteListener
            val pending = pendingBySample.remove(sampleId)
            if (status == 0) {
                loadedSamples += sampleId
                pending?.let { playLoaded(sampleId, it) }
            }
        }
    }

    fun play(asset: SoundAsset) {
        if (released) return
        val rawId = rawIdFor(asset.rawName)
        val sample = sampleByRawId.getOrPut(rawId) {
            soundPool.load(appContext, rawId, 1)
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

    fun release() {
        if (released) return
        released = true
        soundPool.setOnLoadCompleteListener(null)
        sampleByRawId.clear()
        loadedSamples.clear()
        pendingBySample.clear()
        soundPool.release()
    }
}

private fun rawIdFor(rawName: String): Int {
    return when (rawName) {
        SoundAsset.Error -> R.raw.feedback_error
        else -> R.raw.feedback_success
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
