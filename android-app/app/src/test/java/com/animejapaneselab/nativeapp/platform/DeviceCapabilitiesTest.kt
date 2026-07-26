package com.animejapaneselab.nativeapp.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceCapabilitiesTest {
    @Test
    fun androidVersionIncludesMinorApiWhenPresent() {
        assertEquals("Android 16 · API 36.1", formatAndroidVersion("16", 36, 1))
        assertEquals("Android 16 · API 36", formatAndroidVersion("16", 36, 0))
    }

    @Test
    fun refreshRatesAreFormattedForDiagnostics() {
        assertEquals("60 Hz / 90 Hz / 120 Hz", formatRefreshRates(listOf(60f, 90f, 120f)))
        assertEquals("59.9 Hz", formatRefreshRates(listOf(59.94f)))
        assertEquals("系统未报告", formatRefreshRates(emptyList()))
    }
}
