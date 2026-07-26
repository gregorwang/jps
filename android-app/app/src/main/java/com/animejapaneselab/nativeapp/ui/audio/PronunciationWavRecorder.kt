package com.animejapaneselab.nativeapp.ui.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal const val PronunciationSampleRate = 16_000
internal const val PronunciationMinimumDurationMs = 400L
internal const val PronunciationMaximumDurationMs = 15_000L

internal data class RecordedPronunciationAudio(
    val wavBytes: ByteArray,
    val durationMs: Long,
)

internal class PronunciationWavRecorder {
    private val lock = Any()

    @Volatile
    private var recording = false

    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    private var pcmOutput: ByteArrayOutputStream? = null
    private var captureFailure: Throwable? = null

    val isRecording: Boolean
        get() = recording

    @SuppressLint("MissingPermission")
    fun start() {
        check(!recording) { "A pronunciation recording is already active" }
        releaseCurrentRecording()

        val minimumBuffer = AudioRecord.getMinBufferSize(
            PronunciationSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minimumBuffer > 0) { "This device cannot record 16 kHz mono PCM audio" }
        val bufferSize = maxOf(minimumBuffer, 4_096)
        val nextRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            PronunciationSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        check(nextRecord.state == AudioRecord.STATE_INITIALIZED) {
            nextRecord.release()
            "Unable to initialize the microphone"
        }

        val output = ByteArrayOutputStream()
        audioRecord = nextRecord
        pcmOutput = output
        captureFailure = null
        recording = true
        try {
            nextRecord.startRecording()
        } catch (error: Throwable) {
            recording = false
            releaseCurrentRecording()
            throw error
        }
        captureThread = Thread(
            {
                val buffer = ByteArray(bufferSize)
                val maximumPcmBytes = PronunciationSampleRate * 2 *
                    (PronunciationMaximumDurationMs / 1_000L).toInt()
                try {
                    while (recording) {
                        val read = nextRecord.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                        if (read > 0) {
                            synchronized(lock) {
                                val remaining = maximumPcmBytes - output.size()
                                if (remaining > 0) {
                                    output.write(buffer, 0, minOf(read, remaining))
                                }
                                if (output.size() >= maximumPcmBytes) recording = false
                            }
                        } else if (read < 0) {
                            error("Microphone capture failed with code $read")
                        }
                    }
                } catch (error: Throwable) {
                    if (recording) captureFailure = error
                }
            },
            "ajl-pronunciation-capture",
        ).apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop(): RecordedPronunciationAudio {
        val currentRecord = audioRecord ?: error("No pronunciation recording is active")
        recording = false
        runCatching { currentRecord.stop() }
        captureThread?.join(1_500)
        currentRecord.release()
        audioRecord = null
        captureThread = null

        captureFailure?.let { failure ->
            captureFailure = null
            pcmOutput = null
            throw failure
        }
        val pcm: ByteArray = synchronized(lock) {
            pcmOutput?.toByteArray() ?: ByteArray(0)
        }
        pcmOutput = null
        val durationMs = pcm.size.toLong() * 1_000L / (PronunciationSampleRate * 2L)
        return RecordedPronunciationAudio(
            wavBytes = encodePcm16MonoWav(pcm),
            durationMs = durationMs,
        )
    }

    fun cancel() {
        recording = false
        releaseCurrentRecording()
    }

    private fun releaseCurrentRecording() {
        val currentRecord = audioRecord
        runCatching { currentRecord?.stop() }
        captureThread?.join(750)
        runCatching { currentRecord?.release() }
        audioRecord = null
        captureThread = null
        pcmOutput = null
        captureFailure = null
    }
}

internal fun encodePcm16MonoWav(pcmBytes: ByteArray): ByteArray {
    val pcmSize = pcmBytes.size - (pcmBytes.size % 2)
    val buffer = ByteBuffer.allocate(44 + pcmSize).order(ByteOrder.LITTLE_ENDIAN)
    buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
    buffer.putInt(36 + pcmSize)
    buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
    buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
    buffer.putInt(16)
    buffer.putShort(1.toShort())
    buffer.putShort(1.toShort())
    buffer.putInt(PronunciationSampleRate)
    buffer.putInt(PronunciationSampleRate * 2)
    buffer.putShort(2.toShort())
    buffer.putShort(16.toShort())
    buffer.put("data".toByteArray(Charsets.US_ASCII))
    buffer.putInt(pcmSize)
    buffer.put(pcmBytes, 0, pcmSize)
    return buffer.array()
}
