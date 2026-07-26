package com.animejapaneselab.nativeapp.ui.feedback

data class FeedbackVariant(
    val animationTimeMs: Long,
    val sound: SoundAsset? = null,
    val haptic: HapticAsset? = null,
    val visual: VisualAsset? = null,
)

data class SoundAsset(
    val rawName: String,
    val volume: Float = 0.55f,
    val rate: Float = 1f,
)

data class HapticAsset(
    val l1RawName: String,
    val l2RawName: String? = null,
    val l3RawName: String? = null,
)

sealed interface VisualAsset {
    val rawName: String

    data class Rive(
        override val rawName: String,
        val artboard: String? = null,
        val stateMachine: String? = null,
        val animationName: String? = null,
        val triggerMap: Map<String, String> = emptyMap(),
    ) : VisualAsset

    data class Lottie(
        override val rawName: String,
        val iterations: Int = 1,
    ) : VisualAsset
}

internal fun android.content.Context.rawResourceId(rawName: String): Int? {
    val id = resources.getIdentifier(rawName, "raw", packageName)
    return id.takeIf { it != 0 }
}
