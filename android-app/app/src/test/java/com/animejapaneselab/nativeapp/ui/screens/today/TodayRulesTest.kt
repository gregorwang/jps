package com.animejapaneselab.nativeapp.ui.screens.today

import com.animejapaneselab.nativeapp.data.AudioKind
import com.animejapaneselab.nativeapp.data.LinguisticExercise
import com.animejapaneselab.nativeapp.data.LinguisticExerciseAnswer
import com.animejapaneselab.nativeapp.data.LinguisticSceneLine
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.SubtitleLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TodayRulesTest {
    private fun sentence(ja: String, zh: String = "", lineNo: Int = 0) = ShadowingSentence(
        id = "s-$lineNo-$ja",
        ja = ja,
        reading = "",
        meaningZh = zh,
        sourceLabel = "",
        audioKind = AudioKind.None,
        sourceLineNo = lineNo,
    )

    private fun exercise(workSlug: String, episode: Int, vararg lines: LinguisticSceneLine) = LinguisticExercise(
        id = "e-$workSlug-$episode",
        workSlug = workSlug,
        episode = episode,
        jaText = "",
        sceneLines = lines.toList(),
        domain = "pragmatics",
        phenomenonKey = "",
        questionType = "",
        prompt = "",
        options = emptyList(),
        answer = LinguisticExerciseAnswer(answerZh = ""),
    )

    // ---- 今日の一句 ---------------------------------------------------------------------

    @Test
    fun pickIsStableWithinADay() {
        val lines = (1..20).map { TodayLine("台詞その$it です", "第 $it 句", lineNo = it) }
        val day = LocalDate.of(2026, 9, 25)
        val a = TodayRules.pickLine(day, "k-on", 3, lines)
        val b = TodayRules.pickLine(day, "k-on", 3, lines.toList())
        assertEquals(a, b)
    }

    @Test
    fun pickChangesAcrossDays() {
        val lines = (1..20).map { TodayLine("台詞その$it です", "第 $it 句", lineNo = it) }
        val picks = (0L until 14L).map { TodayRules.pickLine(LocalDate.of(2026, 9, 1).plusDays(it), "k-on", 3, lines) }.toSet()
        assertTrue("two weeks should not all pick the same line", picks.size > 3)
    }

    @Test
    fun pickPrefersFittingLinesWithTranslation() {
        val long = TodayLine("これはとても長い台詞なので縦書きの枠には到底収まりきらないはずです", "长")
        val untranslated = TodayLine("おはよう、みんな")
        val good = TodayLine("お姉ちゃん、そろそろ起きないと。", "姐姐，差不多该起床了。")
        repeat(30) { offset ->
            val pick = TodayRules.pickLine(LocalDate.of(2026, 1, 1).plusDays(offset.toLong()), "k-on", 3, listOf(long, untranslated, good))
            assertEquals(good, pick)
        }
    }

    @Test
    fun pickFallsBackToShortestWhenNothingFits() {
        val a = TodayLine("これはとても長い台詞なので縦書きの枠には収まりきらない一文です")
        val b = TodayLine("これも長い台詞なので縦書きの枠には到底収まりきらないはずの一文です")
        assertEquals(a, TodayRules.pickLine(LocalDate.of(2026, 9, 25), "re-zero", 1, listOf(b, a)))
        assertNull(TodayRules.pickLine(LocalDate.of(2026, 9, 25), "re-zero", 1, emptyList()))
    }

    @Test
    fun candidatesMergeSpeakerAndTimeCodeByLineNumber() {
        val lines = TodayRules.candidates(
            workSlug = "k-on",
            episode = 3,
            shadowing = listOf(sentence("お姉ちゃん、そろそろ起きないと。", "姐姐，差不多该起床了。", lineNo = 16)),
            subtitles = listOf(SubtitleLine(16, "00:12:31.200", "00:12:33.000", "お姉ちゃん、そろそろ起きないと。")),
            readAirExercises = listOf(
                exercise("k_on", 3, LinguisticSceneLine(lineNo = 16, speaker = "憂", jaText = "お姉ちゃん、そろそろ起きないと。")),
                exercise("k-on", 4, LinguisticSceneLine(lineNo = 16, speaker = "澪", jaText = "別の話")),
            ),
        )
        assertEquals(1, lines.size)
        val line = lines.single()
        assertEquals("憂", line.speaker)
        assertEquals("12:31", line.timeCode)
        assertEquals("憂 · 12:31", line.attribution)
    }

    @Test
    fun candidatesTakeInlineSpeakerFromSubtitles() {
        val lines = TodayRules.candidates(
            workSlug = "k-on",
            episode = 1,
            shadowing = emptyList(),
            subtitles = listOf(SubtitleLine(3, "", "", "（唯）軽音部って何？", "轻音部是什么？")),
            readAirExercises = emptyList(),
        )
        assertEquals(TodayLine("軽音部って何？", "轻音部是什么？", speaker = "唯", timeCode = null, lineNo = 3), lines.single())
    }

    @Test
    fun attributionNeverShowsARowNumber() {
        assertNull(TodayLine("あ", lineNo = 7).attribution)
        assertNull(TodayLine("あ").attribution)
    }

    @Test
    fun timeCodes() {
        assertEquals("12:31", TodayRules.formatTimeCode("00:12:31.200"))
        assertEquals("1:02:03", TodayRules.formatTimeCode("01:02:03,000"))
        assertEquals("04:05", TodayRules.formatTimeCode("4:5"))
        assertNull(TodayRules.formatTimeCode("soon"))
        assertNull(TodayRules.formatTimeCode(null))
    }

    @Test
    fun verticalLayoutBreaksAfterPunctuation() {
        assertEquals("お姉ちゃん、\nそろそろ起きないと。", TodayRules.verticalLayout("お姉ちゃん、そろそろ起きないと。", 10))
    }

    @Test
    fun verticalLayoutHardBreaksLongRuns() {
        val out = TodayRules.verticalLayout("あいうえおかきくけこさしすせそ", 6)
        assertEquals(listOf("あいうえおか", "きくけこさし", "すせそ"), out.split('\n'))
    }

    @Test
    fun verticalLayoutKeepsFinalPeriodWithItsColumn() {
        val out = TodayRules.verticalLayout("あいうえおか。", 6)
        assertFalse(out.split('\n').any { it == "。" })
    }

    @Test
    fun dateMetaUsesKanjiWeekday() {
        assertEquals("9.25 金", TodayRules.dateMeta(LocalDate.of(2026, 9, 25)))
    }

    // ---- 本日の時間割 -------------------------------------------------------------------

    @Test
    fun dominantSpeakerCountsOnlyGivenLines() {
        val lines = listOf(
            TodayLine("a", speaker = "憂", lineNo = 1),
            TodayLine("b", speaker = "唯", lineNo = 2),
            TodayLine("c", speaker = "憂", lineNo = 3),
            TodayLine("d", speaker = "唯", lineNo = 4),
            TodayLine("e", speaker = "唯", lineNo = 5),
        )
        assertEquals("憂", TodayRules.dominantSpeaker(lines, setOf(1, 2, 3)))
        assertNull(TodayRules.dominantSpeaker(lines, setOf(99)))
    }
}
