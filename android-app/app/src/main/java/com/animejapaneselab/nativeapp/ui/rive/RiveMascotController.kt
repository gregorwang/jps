package com.animejapaneselab.nativeapp.ui.rive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.animejapaneselab.nativeapp.ui.feedback.VisualAsset

class RiveMascotController {
    var lastTrigger by mutableStateOf("idle")
        private set
    var visualAsset by mutableStateOf<VisualAsset?>(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var isSpeaking by mutableStateOf(false)
        private set

    fun trigger(name: String, visual: VisualAsset? = null) {
        lastTrigger = name
        visualAsset = visual
    }

    fun setNumber(name: String, value: Float) {
        if (name == "progress") progress = value
    }

    fun setBoolean(name: String, value: Boolean) {
        if (name == "isSpeaking") isSpeaking = value
    }
}
