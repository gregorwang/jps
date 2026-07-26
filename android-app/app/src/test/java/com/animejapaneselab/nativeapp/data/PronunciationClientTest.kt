package com.animejapaneselab.nativeapp.data

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationClientTest {
    @Test
    fun multipartWriterFollowsProductionContract() {
        val output = ByteArrayOutputStream()
        writePronunciationMultipart(
            output = output,
            boundary = "ajl-test-boundary",
            sentenceId = FirstEnabledPronunciationSentenceId,
            attemptId = "a48d40a9-6380-4ec6-a863-a01bef21aa7d",
            wavBytes = byteArrayOf(
                'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            ),
        )
        val body = output.toByteArray().toString(StandardCharsets.ISO_8859_1)

        assertTrue(body.contains("name=\"sentenceId\"\r\n\r\n$FirstEnabledPronunciationSentenceId"))
        assertTrue(body.contains("name=\"attemptId\"\r\n\r\na48d40a9-6380-4ec6-a863-a01bef21aa7d"))
        assertTrue(body.contains("name=\"mode\"\r\n\r\nshadowing"))
        assertTrue(body.contains("name=\"clientVersion\"\r\n\r\nandroid-0.1.0"))
        assertTrue(body.contains("filename=\"attempt.wav\""))
        assertTrue(body.contains("Content-Type: audio/wav\r\n\r\nRIFF"))
        assertTrue(body.endsWith("\r\n--ajl-test-boundary--\r\n"))
    }

    @Test
    fun parsesReRecordAsBusinessResult() {
        val result = parsePronunciationEvaluation(
            JSONObject(
                """
                {
                  "ok":true,
                  "attemptId":"attempt",
                  "sentenceId":"kon-ep01-046",
                  "assessmentStatus":"re_record",
                  "scorerVersion":"ja-mora-v1.0.0",
                  "engine":{"primary":"nova","secondary":"whisper","fallbackUsed":false,"reliability":0.0},
                  "audioQuality":{"status":"silent","scorable":false,"durationMs":1000,"qualityScore":0,"reasons":["silent"]},
                  "segments":[],
                  "feedback":[]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(PronunciationAssessmentStatus.ReRecord, result.assessmentStatus)
        assertEquals("silent", result.audioQuality.status)
    }

    @Test
    fun parsesScoredResponseWithoutMixingScoreAndReliability() {
        val result = parsePronunciationEvaluation(
            JSONObject(
                """
                {
                  "attemptId":"attempt",
                  "sentenceId":"kon-ep01-046",
                  "assessmentStatus":"scored",
                  "scorerVersion":"ja-mora-v1.0.0",
                  "reference":{"timingSource":"estimated"},
                  "engine":{"primary":"nova","secondary":"whisper","fallbackUsed":true,"reliability":0.755},
                  "recognized":{"text":"正確には配布寸前ね","phoneticKana":"せいかくにわはいふすんぜんね","durationMs":3024},
                  "score":{"overall":83,"accuracy":88,"completeness":100,"clarity":78,"fluency":80,"rhythm":82,"band":"good"},
                  "audioQuality":{"status":"ok","scorable":true,"durationMs":3024,"qualityScore":91,"reasons":[]},
                  "segments":[{"tokenIndex":3,"surface":"廃部","expected":"はいぶ","heard":"はいふ","status":"unclear","score":67,"message":"疑似不够清楚"}],
                  "feedback":["重点检查「廃部」这一段。"]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(83, result.score?.overall)
        assertEquals(0.755, result.engine.reliability, 0.0001)
        assertTrue(result.reference.usesEstimatedTiming)
        assertEquals("像原声", result.score?.lessonRating)
        assertTrue(result.segments.single().needsAttention)
    }
}
