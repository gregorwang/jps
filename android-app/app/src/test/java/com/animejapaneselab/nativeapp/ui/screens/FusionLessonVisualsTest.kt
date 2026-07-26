package com.animejapaneselab.nativeapp.ui.screens

import com.animejapaneselab.nativeapp.data.ClozeChoice
import com.animejapaneselab.nativeapp.data.ClozeNode
import com.animejapaneselab.nativeapp.data.MatchPair
import com.animejapaneselab.nativeapp.data.PairMatchNode
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonTone
import com.animejapaneselab.nativeapp.ui.theme.LabPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FusionLessonVisualsTest {
    @Test
    fun kOnQuestionTypesShareOneCoursePalette() {
        val pair = PairMatchNode(
            id = "pair",
            title = "",
            prompt = "",
            explanation = "",
            sourceLabel = "",
            pairs = listOf(MatchPair("1", "中文", "日语")),
        )
        val cloze = ClozeNode(
            id = "cloze",
            title = "",
            prompt = "",
            explanation = "",
            sourceLabel = "",
            before = "起き",
            after = "",
            choices = listOf(ClozeChoice("ないと", "")),
            answer = "ないと",
        )

        val pairStyle = pair.fusionVisualStyle("k-on")
        val clozeStyle = cloze.fusionVisualStyle("k-on")

        assertNotEquals(pairStyle.label, clozeStyle.label)
        assertEquals(LabPalette.Sakura, pairStyle.accent)
        assertEquals(pairStyle.accent, clozeStyle.accent)
        assertEquals(LabPalette.SakuraDark, clozeStyle.accentDark)
        assertEquals(LabPalette.SakuraSoft, pairStyle.softContainer)
        assertEquals(JuicyLessonTone.Pink, pairStyle.actionTone)
        assertEquals(pairStyle.actionTone, clozeStyle.actionTone)
    }

    @Test
    fun reZeroUsesASeparateCoursePalette() {
        val palette = lessonCoursePalette("re-zero")

        assertEquals(LabPalette.Violet, palette.accent)
        assertEquals(LabPalette.VioletDark, palette.accentDark)
        assertEquals(JuicyLessonTone.Purple, palette.actionTone)
    }
}
