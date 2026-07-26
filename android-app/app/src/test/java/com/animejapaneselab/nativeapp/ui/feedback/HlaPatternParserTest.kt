package com.animejapaneselab.nativeapp.ui.feedback

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HlaPatternParserTest {
    @Test
    fun parsesDuolingoLikeHlaShape() {
        val pattern = HlaPatternParser.parse(
            """
            {
              "ProjectName": "CTA Button_V3",
              "Duration": 615,
              "Timings": [30, 104, 30, 451],
              "Amplitudes": [179, 0, 166, 77],
              "Repeat": -1
            }
            """.trimIndent(),
        )

        requireNotNull(pattern)
        assertArrayEquals(longArrayOf(30, 104, 30, 451), pattern.timings)
        assertArrayEquals(intArrayOf(179, 0, 166, 77), pattern.amplitudes)
        assertEquals(-1, pattern.repeat)
    }

    @Test
    fun rejectsMismatchedHlaArrays() {
        val pattern = HlaPatternParser.parse("""{"Timings":[30],"Amplitudes":[120,0]}""")

        assertNull(pattern)
    }

    @Test
    fun clampsInvalidHlaValues() {
        val pattern = HlaPatternParser.parse("""{"Timings":[-20,40],"Amplitudes":[-1,300]}""")

        requireNotNull(pattern)
        assertArrayEquals(longArrayOf(0, 40), pattern.timings)
        assertArrayEquals(intArrayOf(0, 255), pattern.amplitudes)
    }
}
