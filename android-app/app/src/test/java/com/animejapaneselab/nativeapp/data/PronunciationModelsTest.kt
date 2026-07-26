package com.animejapaneselab.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PronunciationModelsTest {
    private val targetSentence = ShadowingSentence(
        id = FirstEnabledPronunciationSentenceId,
        ja = "正確には廃部寸前ね",
        reading = "せいかくにはいぶすんぜんね",
        meaningZh = "正确来说是即将废部",
        sourceLabel = "EP01 第 46 行",
        audioKind = AudioKind.Tts,
        sourceLineNo = 46,
    )

    @Test
    fun everyPersistedShadowingSentenceGetsItsStablePronunciationId() {
        assertEquals(
            targetSentence.id,
            pronunciationSentenceId(targetSentence),
        )
        assertEquals(
            "re-zero-s02e18-sentence-094",
            pronunciationSentenceId(
                targetSentence.copy(id = "re-zero-s02e18-sentence-094", sourceLineNo = 94),
            ),
        )
    }

    @Test
    fun scoreBandMapsToExistingLessonRatings() {
        fun score(band: String) = PronunciationScore(80, 80, 80, 80, 80, 80, band)

        assertEquals("像原声", score("excellent").lessonRating)
        assertEquals("像原声", score("good").lessonRating)
        assertEquals("大致跟上", score("fair").lessonRating)
        assertEquals("还要再练", score("retry").lessonRating)
    }
}
