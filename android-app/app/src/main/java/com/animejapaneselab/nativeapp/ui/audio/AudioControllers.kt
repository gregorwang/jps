package com.animejapaneselab.nativeapp.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.data.PromptAudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

private val JapaneseTtsVoices = listOf("ja-JP-NanamiNeural", "ja-JP-KeitaNeural")
private const val RemoteTtsUserAgent = "Mozilla/5.0"

enum class AudioPlaybackPhase {
    Idle,
    Loading,
    Playing,
    Error,
}

data class AudioPlaybackState(
    val phase: AudioPlaybackPhase = AudioPlaybackPhase.Idle,
    val message: String = "",
)

class LessonAudioController(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ttsCacheDir = File(appContext.cacheDir, "lesson-tts").apply { mkdirs() }
    private var ttsReady = false
    private var ttsSupported = false
    private var pendingTts: PendingTts? = null
    private var activeLocalTts: PendingTts? = null
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    var playbackState by mutableStateOf(AudioPlaybackState())
        private set

    fun play(cue: PromptAudio, ttsWorkerUrl: String, autoAttempt: Boolean = false) {
        when (cue) {
            PromptAudio.None -> Unit
            is PromptAudio.Tts -> playTts(cue.text, ttsWorkerUrl)
            is PromptAudio.Source -> playSource(cue, ttsWorkerUrl, autoAttempt)
        }
    }

    fun speakText(text: String, ttsWorkerUrl: String) {
        playTts(text, ttsWorkerUrl)
    }

    private fun playSource(cue: PromptAudio.Source, ttsWorkerUrl: String, autoAttempt: Boolean) {
        stopMedia()
        postPlaybackState(AudioPlaybackPhase.Loading, "原声加载中")
        runCatching {
            val player = MediaPlayer()
            mediaPlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            player.setDataSource(cue.url)
            player.setOnPreparedListener { prepared ->
                postPlaybackState(AudioPlaybackPhase.Playing, "原声播放中")
                prepared.start()
            }
            player.setOnCompletionListener { completed ->
                if (mediaPlayer === completed) mediaPlayer = null
                completed.release()
                postPlaybackState(AudioPlaybackPhase.Idle, "")
            }
            player.setOnErrorListener { failed, _, _ ->
                if (mediaPlayer === failed) mediaPlayer = null
                failed.release()
                if (!autoAttempt && cue.fallbackTtsText.isNotBlank()) {
                    postPlaybackState(AudioPlaybackPhase.Error, "原声播放失败，正在尝试标准语音")
                    playTts(cue.fallbackTtsText, ttsWorkerUrl)
                } else if (cue.fallbackTtsText.isNotBlank()) {
                    postPlaybackState(AudioPlaybackPhase.Error, "原声自动播放失败，可手动点标准语音兜底")
                } else {
                    postPlaybackState(AudioPlaybackPhase.Error, "原声播放失败")
                }
                true
            }
            player.prepareAsync()
        }.onFailure {
            if (!autoAttempt && cue.fallbackTtsText.isNotBlank()) {
                postPlaybackState(AudioPlaybackPhase.Error, "原声加载失败，正在尝试标准语音")
                playTts(cue.fallbackTtsText, ttsWorkerUrl)
            } else if (cue.fallbackTtsText.isNotBlank()) {
                postPlaybackState(AudioPlaybackPhase.Error, "原声加载失败，可手动点标准语音兜底")
            } else {
                postPlaybackState(AudioPlaybackPhase.Error, "原声加载失败")
            }
        }
    }

    private fun ensureLocalTts() {
        if (tts != null || ttsReady) return
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                scope.launch {
                    configureTts(status)
                }
            }
        }.onFailure {
            ttsReady = true
            ttsSupported = false
        }
    }

    private fun configureTts(status: Int) {
        val engine = tts ?: run {
            ttsReady = true
            ttsSupported = false
            return
        }
        val languageResult = engine.setLanguage(Locale.JAPAN)
        ttsSupported = status == TextToSpeech.SUCCESS &&
            languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED
        ttsReady = true
        engine.setSpeechRate(0.94f)
        engine.setPitch(1.0f)
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    postPlaybackState(AudioPlaybackPhase.Playing, "系统标准语音播放中")
                }

                override fun onDone(utteranceId: String?) {
                    activeLocalTts = null
                    postPlaybackState(AudioPlaybackPhase.Idle, "")
                }

                @Deprecated("Deprecated in Android framework")
                override fun onError(utteranceId: String?) {
                    handleLocalTtsError()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    handleLocalTtsError()
                }
            },
        )
        pendingTts?.let { pending ->
            pendingTts = null
            playTts(pending.text, pending.ttsWorkerUrl)
        }
    }

    private fun playTts(text: String, ttsWorkerUrl: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        if (!ttsReady) {
            pendingTts = PendingTts(clean, ttsWorkerUrl)
            ensureLocalTts()
            if (ttsReady) {
                pendingTts = null
                playRemoteTts(clean, ttsWorkerUrl)
                return
            }
            postPlaybackState(AudioPlaybackPhase.Loading, "系统标准语音初始化中")
            return
        }
        stopMedia()
        if (ttsSupported) {
            runCatching { tts?.stop() }
            activeLocalTts = PendingTts(clean, ttsWorkerUrl)
            postPlaybackState(AudioPlaybackPhase.Loading, "系统标准语音准备中")
            val result = runCatching {
                tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "lesson-${sha256(clean).take(12)}")
            }.getOrNull()
            if (result == TextToSpeech.SUCCESS) return
            activeLocalTts = null
        }
        playRemoteTts(clean, ttsWorkerUrl)
    }

    private fun playRemoteTts(text: String, ttsWorkerUrl: String) {
        scope.launch {
            postPlaybackState(AudioPlaybackPhase.Loading, "云端标准语音加载中")
            val file = runCatching {
                withContext(Dispatchers.IO) { fetchRemoteTts(text, ttsWorkerUrl) }
            }.getOrElse { error ->
                postPlaybackState(AudioPlaybackPhase.Error, "云端标准语音请求失败：${error.message ?: "未知错误"}")
                return@launch
            }
            stopMedia()
            runCatching {
                val player = MediaPlayer()
                mediaPlayer = player
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                player.setDataSource(file.absolutePath)
                player.setOnPreparedListener { prepared ->
                    postPlaybackState(AudioPlaybackPhase.Playing, "云端标准语音播放中")
                    prepared.start()
                }
                player.setOnCompletionListener { completed ->
                    if (mediaPlayer === completed) mediaPlayer = null
                    completed.release()
                    postPlaybackState(AudioPlaybackPhase.Idle, "")
                }
                player.setOnErrorListener { failed, _, _ ->
                    if (mediaPlayer === failed) mediaPlayer = null
                    failed.release()
                    postPlaybackState(AudioPlaybackPhase.Error, "云端标准语音播放失败")
                    true
                }
                player.prepareAsync()
            }.onFailure { error ->
                postPlaybackState(AudioPlaybackPhase.Error, "云端标准语音播放失败：${error.message ?: "未知错误"}")
            }
        }
    }

    private fun handleLocalTtsError() {
        val fallback = activeLocalTts
        activeLocalTts = null
        if (fallback != null) {
            postPlaybackState(AudioPlaybackPhase.Error, "系统标准语音播放失败，正在尝试云端语音")
            playRemoteTts(fallback.text, fallback.ttsWorkerUrl)
        } else {
            postPlaybackState(AudioPlaybackPhase.Error, "系统标准语音播放失败")
        }
    }

    private fun fetchRemoteTts(text: String, ttsWorkerUrl: String): File {
        val normalizedBase = ttsWorkerUrl.trim().trimEnd('/')
        val cacheFile = File(ttsCacheDir, "${sha256(text)}.mp3")
        if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile

        var lastError: Throwable? = null
        if (normalizedBase.isNotBlank()) {
            for (voice in JapaneseTtsVoices) {
                runCatching {
                    fetchRemoteTtsWorkerVoice(
                        normalizedBase = normalizedBase,
                        text = text,
                        voice = voice,
                        cacheFile = cacheFile,
                    )
                }.onSuccess {
                    return it
                }.onFailure { error ->
                    lastError = error
                    cacheFile.delete()
                }
            }
        }
        runCatching {
            fetchGoogleTranslateTts(text = text, cacheFile = cacheFile)
        }.onSuccess {
            return it
        }.onFailure { error ->
            cacheFile.delete()
            lastError = error
        }
        throw lastError ?: IllegalStateException("标准语音请求失败")
    }

    private fun fetchRemoteTtsWorkerVoice(
        normalizedBase: String,
        text: String,
        voice: String,
        cacheFile: File,
    ): File {
        val body = JSONObject()
            .put("text", text)
            .put("voice", voice)
            .toString()
        val connection = (URL("$normalizedBase/tts").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 25_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "audio/mpeg,application/octet-stream")
            setRequestProperty("User-Agent", RemoteTtsUserAgent)
            outputStream.use { stream ->
                stream.write(body.toByteArray(StandardCharsets.UTF_8))
            }
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) error("标准语音请求失败：HTTP $status")
            BufferedInputStream(connection.inputStream).use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            return cacheFile
        } catch (error: Throwable) {
            cacheFile.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchGoogleTranslateTts(text: String, cacheFile: File): File {
        val connection = (URL(buildGoogleTranslateTtsUrl(text)).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("Accept", "audio/mpeg,application/octet-stream")
            setRequestProperty("User-Agent", RemoteTtsUserAgent)
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) error("备用标准语音请求失败：HTTP $status")
            BufferedInputStream(connection.inputStream).use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            check(cacheFile.length() > 0) { "备用标准语音返回空音频" }
            return cacheFile
        } catch (error: Throwable) {
            cacheFile.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun stopMedia() {
        mediaPlayer?.let { player ->
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        mediaPlayer = null
    }

    fun release() {
        playbackState = AudioPlaybackState()
        scope.cancel()
        stopMedia()
        val textToSpeech = tts
        tts = null
        runCatching { textToSpeech?.shutdown() }
        ttsReady = false
        ttsSupported = false
        pendingTts = null
        activeLocalTts = null
    }

    private fun postPlaybackState(phase: AudioPlaybackPhase, message: String) {
        scope.launch {
            playbackState = AudioPlaybackState(phase = phase, message = message)
        }
    }
}

class FeedbackSoundController(context: Context) {
    private val rewardHandler = Handler(Looper.getMainLooper())
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val success = soundPool.load(context, R.raw.feedback_success, 1)
    private val error = soundPool.load(context, R.raw.feedback_error, 1)
    private var successLoaded = false
    private var errorLoaded = false
    private var pendingFeedback: Boolean? = null
    private var pendingCompletion = false
    private var released = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (released) return@setOnLoadCompleteListener
            if (status == 0) {
                if (sampleId == success) successLoaded = true
                if (sampleId == error) errorLoaded = true
                if (sampleId == success && pendingCompletion) {
                    pendingCompletion = false
                    playCompletion()
                }
                pendingFeedback?.let { pending ->
                    val pendingLoaded = if (pending) successLoaded else errorLoaded
                    if (pendingLoaded) {
                        pendingFeedback = null
                        play(pending)
                    }
                }
            } else if ((sampleId == success && pendingFeedback == true) || (sampleId == error && pendingFeedback == false)) {
                pendingFeedback = null
            } else if (sampleId == success && pendingCompletion) {
                pendingCompletion = false
            }
        }
    }

    fun play(correct: Boolean) {
        if (released) return
        val loaded = if (correct) successLoaded else errorLoaded
        if (!loaded) {
            pendingFeedback = correct
            return
        }
        val sampleId = if (correct) success else error
        val volume = if (correct) 0.78f else 0.72f
        val rate = if (correct) 1.06f else 0.92f
        playSample(sampleId, volume, rate)
    }

    fun playCompletion() {
        if (released) return
        if (!successLoaded) {
            pendingCompletion = true
            return
        }
        rewardHandler.removeCallbacksAndMessages(null)
        playSample(success, volume = 0.50f, rate = 1.06f)
        postCompletionTone(delayMillis = 86L, volume = 0.44f, rate = 1.24f)
        postCompletionTone(delayMillis = 174L, volume = 0.36f, rate = 1.42f)
    }

    private fun playSample(sampleId: Int, volume: Float, rate: Float) {
        soundPool.play(sampleId, volume, volume, 1, 0, rate)
    }

    private fun postCompletionTone(delayMillis: Long, volume: Float, rate: Float) {
        rewardHandler.postDelayed(
            {
                if (!released) {
                    playSample(success, volume, rate)
                }
            },
            delayMillis,
        )
    }

    fun release() {
        released = true
        rewardHandler.removeCallbacksAndMessages(null)
        pendingFeedback = null
        pendingCompletion = false
        soundPool.release()
    }
}

@Composable
fun rememberLessonAudioController(): LessonAudioController {
    val context = LocalContext.current
    val controller = remember(context) { LessonAudioController(context) }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}

@Composable
fun rememberFeedbackSoundController(): FeedbackSoundController {
    val context = LocalContext.current
    val controller = remember(context) { FeedbackSoundController(context) }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}

private data class PendingTts(
    val text: String,
    val ttsWorkerUrl: String,
)

private fun sha256(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}
