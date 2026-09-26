package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.ui.graphics.Color
import com.animejapaneselab.nativeapp.ui.theme.WorkHue
import com.animejapaneselab.nativeapp.ui.theme.WorkThemes
import com.animejapaneselab.nativeapp.ui.theme.normalizeWorkSlug
import com.animejapaneselab.nativeapp.ui.theme.progressRamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignRulesTest {
    // ---- progress colour interpolation --------------------------------------------------

    @Test
    fun progressRampHitsBothEnds() {
        val light = Color(0xFFEBC3D0)
        val deep = Color(0xFFAA3C64)
        assertEquals(light, progressRamp(light, deep, 0f))
        assertEquals(deep, progressRamp(light, deep, 1f))
    }

    @Test
    fun progressRampClampsOutOfRange() {
        val light = Color(0xFFEBC3D0)
        val deep = Color(0xFFAA3C64)
        assertEquals(light, progressRamp(light, deep, -0.5f))
        assertEquals(deep, progressRamp(light, deep, 3f))
    }

    @Test
    fun progressRampMidpointSitsBetween() {
        val mid = progressRamp(Color.White, Color.Black, 0.5f)
        assertTrue(mid.red in 0.3f..0.8f)
        assertEquals(mid.red, mid.green, 0.001f)
    }

    @Test
    fun workProgressColorGetsDeeperAsProgressGrows() {
        val theme = WorkThemes.forSlug("k-on", dark = false)
        val a = theme.progressColor(0.2f)
        val b = theme.progressColor(0.9f)
        assertTrue(luminance(b) < luminance(a))
    }

    // ---- vertical punctuation ------------------------------------------------------------

    @Test
    fun verticalGlyphMapsSpecPunctuation() {
        assertEquals('︑', TextRules.verticalGlyph('、'))
        assertEquals('︒', TextRules.verticalGlyph('。'))
        assertEquals('丨', TextRules.verticalGlyph('ー'))
        assertEquals('﹁', TextRules.verticalGlyph('「'))
        assertEquals('﹂', TextRules.verticalGlyph('」'))
        assertEquals('︙', TextRules.verticalGlyph('…'))
    }

    @Test
    fun verticalGlyphLeavesKanaAndKanjiAlone() {
        "第三話お姉ちゃん".forEach { assertEquals(it, TextRules.verticalGlyph(it)) }
    }

    @Test
    fun verticalColumnsSplitOnNewlineAndMapEachGlyph() {
        val columns = TextRules.verticalColumns("お姉ちゃん、\nそろそろ。")
        assertEquals(2, columns.size)
        assertEquals(listOf("お", "姉", "ち", "ゃ", "ん", "︑"), columns[0])
        assertEquals("︒", columns[1].last())
    }

    // ---- typewriter ----------------------------------------------------------------------

    @Test
    fun typewriterPausesAfterCommaAndPeriod() {
        val delays = TextRules.typewriterDelays("あ、い。う")
        assertEquals(0, delays[0])
        assertEquals(35, delays[1]) // 、 itself
        assertEquals(35 + 120, delays[2]) // after 、
        assertEquals(35 + 200, delays[4]) // after 。
    }

    @Test
    fun typewriterScalesToAudioLength() {
        val delays = TextRules.typewriterDelaysFor("お姉ちゃん、そろそろ起きないと。", 900)
        assertTrue(delays.sum() in 850..900)
    }

    // ---- numbers -------------------------------------------------------------------------

    @Test
    fun kanjiEpisodeNumbers() {
        assertEquals("第三話", TextRules.episodeLabel(3))
        assertEquals("第十話", TextRules.episodeLabel(10))
        assertEquals("第十三話", TextRules.episodeLabel(13))
        assertEquals("第二十五話", TextRules.episodeLabel(25))
        assertEquals("第六十六話", TextRules.episodeLabel(66))
        assertEquals("場面 03", TextRules.sceneLabel(3))
    }

    // ---- workSlug → theme ---------------------------------------------------------------

    @Test
    fun workSlugSelectsHue() {
        assertEquals(WorkHue.Sakura, WorkThemes.hueFor("k-on"))
        assertEquals(WorkHue.Sakura, WorkThemes.hueFor("K_ON"))
        assertEquals(WorkHue.Sumire, WorkThemes.hueFor("re-zero"))
        assertEquals(WorkHue.Sumire, WorkThemes.hueFor("rezero"))
        assertEquals(WorkHue.Sumire, WorkThemes.hueFor("re-zero-s3"))
        assertEquals(WorkHue.Ai, WorkThemes.hueFor(null))
        assertEquals(WorkHue.Ai, WorkThemes.hueFor("some-new-work"))
    }

    @Test
    fun specAccentsMatchThePlan() {
        assertEquals(Color(0xFFC4466F), WorkThemes.forSlug("k-on", false).accent)
        assertEquals(Color(0xFFE58AA7), WorkThemes.forSlug("k-on", true).accent)
        assertEquals(Color(0xFF6A4FC4), WorkThemes.forSlug("re-zero", false).accent)
        assertEquals(Color(0xFFA894F2), WorkThemes.forSlug("re-zero", true).accent)
        assertEquals(Color(0xFF3A4FCB), WorkThemes.forSlug("", false).accent)
        assertEquals(Color(0xFF8C9BF2), WorkThemes.forSlug("", true).accent)
    }

    @Test
    fun normalizeFoldsAliases() {
        assertEquals("re-zero", normalizeWorkSlug(" ReZero "))
        assertEquals("k-on", normalizeWorkSlug("k_on"))
        assertEquals("", normalizeWorkSlug(null))
    }

    // ---- work identity -------------------------------------------------------------------

    @Test
    fun reZeroSplitsIntoThreeSeasonsWithTheirFaces() {
        val seasons = WorkIdentity.seasons("re-zero", 66)
        assertEquals(listOf("エミリア", "ベアトリス", "スバル"), seasons.map { it.character.name })
        assertEquals(1..25, seasons[0].firstEpisode..seasons[0].lastEpisode)
        assertEquals(26..50, seasons[1].firstEpisode..seasons[1].lastEpisode)
        assertEquals(51..66, seasons[2].firstEpisode..seasons[2].lastEpisode)
        assertEquals("ベアトリス", WorkIdentity.seasonFor("re-zero", 66, 30)?.character?.name)
    }

    @Test
    fun characterAliasesResolveToPortraits() {
        val yui = WorkIdentity.character("平沢唯")
        assertEquals("唯", yui?.name)
        assertNotEquals(null, yui?.drawable)
        assertEquals(WorkIdentity.character("エミリア")?.drawable, WorkIdentity.drawableFor("Emilia"))
    }

    @Test
    fun unknownSpeakerFallsBackToKanjiCircle() {
        val ref = WorkIdentity.character("相手")
        assertEquals("相", ref?.mark)
        assertNull(ref?.drawable)
        assertNull(WorkIdentity.character("  "))
    }

    @Test
    fun stampRotationStaysInSpecRange() {
        (1..200).forEach { assertTrue(stampRotationFor(it) in -14f..8f) }
    }

    private fun luminance(c: Color) = 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
}
