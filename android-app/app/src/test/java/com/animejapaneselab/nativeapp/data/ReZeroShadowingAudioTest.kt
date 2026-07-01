package com.animejapaneselab.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReZeroShadowingAudioTest {
    @Test
    fun buildsSeasonOneSourceAudioFromSentenceId() {
        val source = buildReZeroShadowingAudio(
            sentenceId = "re-zero-s01e01-sentence-001",
            audioUrl = "",
            storagePath = "",
        )

        requireNotNull(source)
        assertEquals(
            "https://cdn.xn--cckl9nsb.com/rezeroS1/s01e01/re-zero-s01e01-sentence-001.mp3",
            source.url,
        )
        assertFalse(source.isFlagged)
    }

    @Test
    fun buildsModernSeasonThreeSourceAudioFromSentenceId() {
        val source = buildReZeroShadowingAudio(
            sentenceId = "rezero_s03e62_v9_sent_001",
            audioUrl = "",
            storagePath = "",
        )

        requireNotNull(source)
        assertEquals(
            "https://cdn.xn--cckl9nsb.com/rezeroS3/s03e12/rezero_s03e62_v9_sent_001.mp3",
            source.url,
        )
        assertFalse(source.isFlagged)
    }

    @Test
    fun flagsLateSeasonOneUnmatchedAudioLikeWeb() {
        val source = buildReZeroShadowingAudio(
            sentenceId = "re-zero-s01e25-sentence-096",
            audioUrl = "",
            storagePath = "",
        )

        requireNotNull(source)
        assertTrue(source.url.endsWith("/re-zero-s01e25-sentence-096.unmatched_or_ineligible.mp3"))
        assertTrue(source.isFlagged)
    }

    @Test
    fun kOnSentenceUsesTtsAudio() {
        val sentence = ShadowingSentence(
            id = "k-on-ep01-sent-00056",
            ja = "このプリントをみんなに配っておいてね。",
            reading = "",
            meaningZh = "去把这些资料发给大家。",
            sourceLabel = "EP01 第 56 行",
            audioKind = AudioKind.Tts,
        )

        val audio = promptAudioForSentence("k-on", sentence, autoPlay = true)

        assertTrue(audio is PromptAudio.Tts)
        assertTrue(audio.autoPlay)
    }

    @Test
    fun nonReZeroSentenceUsesExplicitSourceAudioWithTtsFallback() {
        val sentence = ShadowingSentence(
            id = "k-on-ep01-sent-00056",
            ja = "このプリントをみんなに配っておいてね。",
            reading = "",
            meaningZh = "去把这些资料发给大家。",
            sourceLabel = "EP01 第 56 行",
            audioKind = AudioKind.Source,
            audioUrl = "https://cdn.example.test/k-on/ep01/sent-056.mp3",
        )

        val audio = promptAudioForSentence("k-on", sentence, autoPlay = true)

        require(audio is PromptAudio.Source)
        assertEquals("https://cdn.example.test/k-on/ep01/sent-056.mp3", audio.url)
        assertEquals("このプリントをみんなに配っておいてね。", audio.fallbackTtsText)
        assertTrue(audio.autoPlay)
        assertEquals(AudioReliability.Verified, audio.reliability)
    }

    @Test
    fun reZeroSentenceUsesSourceAudioWithTtsFallback() {
        val sentence = ShadowingSentence(
            id = "re-zero-s01e01-sentence-001",
            ja = "やばい…これは本気でやばい。",
            reading = "",
            meaningZh = "糟了……这是真的不妙。",
            sourceLabel = "EP01 第 3 行",
            audioKind = AudioKind.Source,
        )

        val audio = promptAudioForSentence("re-zero", sentence, autoPlay = true)

        require(audio is PromptAudio.Source)
        assertTrue(audio.url.endsWith("/re-zero-s01e01-sentence-001.mp3"))
        assertEquals("やばい…これは本気でやばい。", audio.fallbackTtsText)
        assertTrue(audio.autoPlay)
    }

    @Test
    fun flaggedReZeroSourceAudioDoesNotAutoPlay() {
        val sentence = ShadowingSentence(
            id = "re-zero-s01e25-sentence-096",
            ja = "これは本気でやばい。",
            reading = "",
            meaningZh = "这是真的不妙。",
            sourceLabel = "EP25 第 96 行",
            audioKind = AudioKind.Source,
        )

        val audio = promptAudioForSentence("re-zero", sentence, autoPlay = true)

        require(audio is PromptAudio.Source)
        assertFalse(audio.autoPlay)
        assertEquals(AudioReliability.Flagged, audio.reliability)
    }
}
