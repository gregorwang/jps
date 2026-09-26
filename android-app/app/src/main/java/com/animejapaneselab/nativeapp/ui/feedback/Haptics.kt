package com.animejapaneselab.nativeapp.ui.feedback

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

enum class HapticKind {
    Light,
    Confirm,
    Reject,
}

class Haptics(private val view: View) {
    fun perform(kind: HapticKind?) {
        if (kind == null) return
        val feedback = when (kind) {
            HapticKind.Light -> HapticFeedbackConstants.CLOCK_TICK
            HapticKind.Confirm -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.CONTEXT_CLICK
            }
            HapticKind.Reject -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
        }
        runCatching { view.performHapticFeedback(feedback) }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
