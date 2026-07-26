package com.animejapaneselab.nativeapp.ui.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PronunciationWavRecorderTest {
    @Test
    fun encodesPcmAsMono16KhzPcm16Wav() {
        val pcm = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val wav = encodePcm16MonoWav(pcm)
        val littleEndian = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)

        assertEquals("RIFF", wav.copyOfRange(0, 4).toString(Charsets.US_ASCII))
        assertEquals(40, littleEndian.getInt(4))
        assertEquals("WAVE", wav.copyOfRange(8, 12).toString(Charsets.US_ASCII))
        assertEquals(1, littleEndian.getShort(20).toInt())
        assertEquals(1, littleEndian.getShort(22).toInt())
        assertEquals(PronunciationSampleRate, littleEndian.getInt(24))
        assertEquals(PronunciationSampleRate * 2, littleEndian.getInt(28))
        assertEquals(16, littleEndian.getShort(34).toInt())
        assertEquals("data", wav.copyOfRange(36, 40).toString(Charsets.US_ASCII))
        assertEquals(pcm.size, littleEndian.getInt(40))
        assertArrayEquals(pcm, wav.copyOfRange(44, wav.size))
    }

    @Test
    fun dropsIncompleteTrailingPcmSample() {
        val wav = encodePcm16MonoWav(byteArrayOf(1, 2, 3))

        assertEquals(46, wav.size)
        assertEquals(2, ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN).getInt(40))
    }
}
