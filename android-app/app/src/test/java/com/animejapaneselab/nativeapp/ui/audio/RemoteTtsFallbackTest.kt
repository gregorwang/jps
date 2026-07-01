package com.animejapaneselab.nativeapp.ui.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteTtsFallbackTest {
    @Test
    fun buildsJapaneseFallbackTtsUrl() {
        val url = buildGoogleTranslateTtsUrl("  こんにちは 世界  ")

        assertTrue(url.startsWith("https://translate.googleapis.com/translate_tts?"))
        assertTrue(url.contains("client=gtx"))
        assertTrue(url.contains("tl=ja"))
        assertTrue(url.contains("q=%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF+%E4%B8%96%E7%95%8C"))
    }

    @Test
    fun rejectsBlankFallbackTtsText() {
        assertThrows(IllegalArgumentException::class.java) {
            buildGoogleTranslateTtsUrl("  ")
        }
    }

    @Test
    fun rejectsOverlongFallbackTtsText() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            buildGoogleTranslateTtsUrl("あ".repeat(GoogleTranslateTtsMaxChars + 1))
        }

        assertEquals("语音文本过长：181", error.message)
    }
}
