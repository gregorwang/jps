package com.animejapaneselab.nativeapp.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTokensTest {
    @Test
    fun reducedMotionCollapsesDurations() {
        assertEquals(0, MotionTokens.duration(MotionTokens.Dur.Page, reducedMotion = true))
    }

    @Test
    fun normalMotionKeepsTokenDurations() {
        assertEquals(260, MotionTokens.duration(MotionTokens.Dur.Page, reducedMotion = false))
    }

    @Test
    fun everydayDurationsStayInTheQuietBand() {
        listOf(
            MotionTokens.Dur.Press,
            MotionTokens.Dur.State,
            MotionTokens.Dur.Sheet,
            MotionTokens.Dur.Stamp,
        ).forEach { assertTrue("$it outside 90..240", it in 90..240) }
    }
}
