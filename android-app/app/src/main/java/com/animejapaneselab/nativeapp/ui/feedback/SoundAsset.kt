package com.animejapaneselab.nativeapp.ui.feedback

data class SoundAsset(
    val rawName: String,
    val volume: Float = 0.55f,
    val rate: Float = 1f,
) {
    companion object {
        const val Success = "feedback_success"
        const val Error = "feedback_error"
    }
}
