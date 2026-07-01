package com.animejapaneselab.nativeapp.ui.feedback

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

fun View.performAnswerFeedbackHaptic(correct: Boolean) {
    val feedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (correct) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
    } else {
        if (correct) HapticFeedbackConstants.CONTEXT_CLICK else HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(feedback)
}

fun View.performCompletionFeedbackHaptic() {
    val rewardPulse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.CONTEXT_CLICK
    }
    performHapticFeedback(rewardPulse)
    postDelayed({ performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }, 90L)
    postDelayed({ performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }, 180L)
}
