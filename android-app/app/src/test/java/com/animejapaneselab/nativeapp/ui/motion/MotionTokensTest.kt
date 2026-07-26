package com.animejapaneselab.nativeapp.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionTokensTest {
    @Test
    fun reducedMotionCollapsesDurations() {
        assertEquals(1, MotionTokens.duration(MotionTokens.Duration.PageTransition, reducedMotion = true))
    }

    @Test
    fun normalMotionKeepsTokenDurations() {
        assertEquals(280, MotionTokens.duration(MotionTokens.Duration.PageTransition, reducedMotion = false))
    }
}
