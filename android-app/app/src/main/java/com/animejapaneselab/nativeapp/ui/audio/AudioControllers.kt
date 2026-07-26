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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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
    private var mediaPlayer: MediaPlayer? = null
    private var ttsJob: Job? = null
    private var localTtsInitTimeoutJob: Job? = null
    private var localTts: TextToSpeech? = null
    private var localTtsInitialized = false
    private var localTtsReady = false
    private var pendingTtsRequest: TtsRequest? = null
    private val localFallbacks = ConcurrentHashMap<String, TtsRequest>()
    private var failedSourceUrl: String? = null
    private var failedSourceFallback: TtsRequest? = null
    var playbackState by mutableStateOf(AudioPlaybackState())
        private set

    private val localTtsListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
            postPlaybackState(AudioPlaybackPhase.Playing, "本机语音播放中")
        }

        override fun onDone(utteranceId: String) {
            localFallbacks.remove(utteranceId)
            postPlaybackState(AudioPlaybackPhase.Idle, "")
        }

        @Deprecated("Deprecated in Android")
        override fun onError(utteranceId: String) {
            retryWithConfiguredWorker(utteranceId)
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            retryWithConfiguredWorker(utteranceId)
        }

        private fun retryWithConfiguredWorker(utteranceId: String) {
            val request = localFallbacks.remove(utteranceId) ?: return
            scope.launch { playRemoteTts(request.text, request.workerUrl) }
        }
    }

    init {
        localTts = TextToSpeech(appContext) { status ->
            scope.launch {
                val engine = localTts
                localTtsInitTimeoutJob?.cancel()
                localTtsReady = status == TextToSpeech.SUCCESS &&
                    engine != null &&
                    engine.setLanguage(Locale.JAPAN) >= TextToSpeech.LANG_AVAILABLE
                if (localTtsReady) {
                    engine?.setOnUtteranceProgressListener(localTtsListener)
                }
                localTtsInitialized = true
                pendingTtsRequest?.also {
                    pendingTtsRequest = null
                    playTts(it.text, it.workerUrl)
                }
            }
        }
        localTtsInitTimeoutJob = scope.launch {
            delay(2_000L)
            if (!localTtsInitialized) {
                localTtsInitialized = true
                localTtsReady = false
                pendingTtsRequest?.also {
                    pendingTtsRequest = null
                    playRemoteTts(it.text, it.workerUrl)
                }
            }
        }
    }

    fun play(cue: PromptAudio, ttsWorkerUrl: String, autoAttempt: Boolean = false) {
        when (cue) {
            PromptAudio.None -> Unit
            is PromptAudio.Tts -> playTts(cue.text, ttsWorkerUrl)
            is PromptAudio.Source -> {
                val fallback = failedSourceFallback
                if (!autoAttempt && failedSourceUrl == cue.url && fallback != null) {
                    failedSourceUrl = null
                    failedSourceFallback = null
                    playTts(fallback.text, fallback.workerUrl)
                } else {
                    playSource(cue, ttsWorkerUrl, autoAttempt)
                }
            }
        }
    }

    fun speakText(text: String, ttsWorkerUrl: String) {
        playTts(text, ttsWorkerUrl)
    }

    private fun playSource(cue: PromptAudio.Source, ttsWorkerUrl: String, autoAttempt: Boolean) {
        failedSourceUrl = null
        failedSourceFallback = null
        ttsJob?.cancel()
        ttsJob = null
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
                failedSourceUrl = null
                failedSourceFallback = null
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
                failedSourceUrl = cue.url
                failedSourceFallback = cue.fallbackTtsText.takeIf(String::isNotBlank)?.let { TtsRequest(it, ttsWorkerUrl) }
                val suffix = if (failedSourceFallback == null) "" else "；再次点击可改用本机/配置语音"
                postPlaybackState(AudioPlaybackPhase.Error, if (autoAttempt) "原声自动播放失败$suffix" else "原声播放失败$suffix")
                true
            }
            player.prepareAsync()
        }.onFailure {
            failedSourceUrl = cue.url
            failedSourceFallback = cue.fallbackTtsText.takeIf(String::isNotBlank)?.let { TtsRequest(it, ttsWorkerUrl) }
            val suffix = if (failedSourceFallback == null) "" else "；再次点击可改用本机/配置语音"
            postPlaybackState(AudioPlaybackPhase.Error, if (autoAttempt) "原声自动加载失败$suffix" else "原声加载失败$suffix")
        }
    }

    private fun playTts(text: String, ttsWorkerUrl: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        stopMedia()
        ttsJob?.cancel()
        localTts?.stop()
        val request = TtsRequest(clean, ttsWorkerUrl)
        if (!localTtsInitialized) {
            pendingTtsRequest = request
            postPlaybackState(AudioPlaybackPhase.Loading, "正在准备本机日语语音")
            return
        }
        if (localTtsReady && speakWithLocalTts(request)) return
        playRemoteTts(clean, ttsWorkerUrl)
    }

    private fun speakWithLocalTts(request: TtsRequest): Boolean {
        val engine = localTts ?: return false
        val utteranceId = UUID.randomUUID().toString()
        localFallbacks[utteranceId] = request
        val status = engine.speak(request.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (status == TextToSpeech.SUCCESS) {
            postPlaybackState(AudioPlaybackPhase.Loading, "本机语音准备中")
            return true
        }
        localFallbacks.remove(utteranceId)
        return false
    }

    private fun playRemoteTts(text: String, ttsWorkerUrl: String) {
        ttsJob?.cancel()
        ttsJob = scope.launch {
            postPlaybackState(AudioPlaybackPhase.Loading, "语音加载中")
            val file = try {
                withContext(Dispatchers.IO) { fetchRemoteTts(text, ttsWorkerUrl) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                postPlaybackState(AudioPlaybackPhase.Error, "语音请求失败：${error.message ?: "未知错误"}")
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
                    postPlaybackState(AudioPlaybackPhase.Playing, "语音播放中")
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
                    postPlaybackState(AudioPlaybackPhase.Error, "语音播放失败")
                    true
                }
                player.prepareAsync()
            }.onFailure { error ->
                postPlaybackState(AudioPlaybackPhase.Error, "语音播放失败：${error.message ?: "未知错误"}")
            }
        }
    }

    private fun fetchRemoteTts(text: String, ttsWorkerUrl: String): File {
        val normalizedBase = ttsWorkerUrl.trim().trimEnd('/')
        var lastError: Throwable? = null
        if (normalizedBase.isNotBlank()) {
            for (voice in JapaneseTtsVoices) {
                val cacheFile = File(ttsCacheDir, "${sha256("$normalizedBase|$voice|$text")}.mp3")
                if (cacheFile.exists() && cacheFile.length() > 0) return cacheFile
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
        throw lastError ?: IllegalStateException("未配置可用的语音 Worker")
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
        val tempFile = File.createTempFile(cacheFile.nameWithoutExtension, ".part", ttsCacheDir)
        val connection = (URL("$normalizedBase/tts").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 25_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "audio/mpeg,application/octet-stream")
            setRequestProperty("User-Agent", RemoteTtsUserAgent)
        }
        try {
            connection.outputStream.use { stream ->
                stream.write(body.toByteArray(StandardCharsets.UTF_8))
            }
            val status = connection.responseCode
            if (status !in 200..299) error("语音请求失败：HTTP $status")
            BufferedInputStream(connection.inputStream).use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            check(tempFile.length() > 0) { "语音服务返回空音频" }
            if (!tempFile.renameTo(cacheFile)) {
                tempFile.copyTo(cacheFile, overwrite = true)
                tempFile.delete()
            }
            return cacheFile
        } catch (error: Throwable) {
            tempFile.delete()
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
        ttsJob?.cancel()
        localTtsInitTimeoutJob?.cancel()
        ttsJob = null
        scope.cancel()
        pendingTtsRequest = null
        localFallbacks.clear()
        localTts?.stop()
        localTts?.shutdown()
        localTts = null
        stopMedia()
    }

    private fun postPlaybackState(phase: AudioPlaybackPhase, message: String) {
        scope.launch {
            playbackState = AudioPlaybackState(phase = phase, message = message)
        }
    }
}

private data class TtsRequest(
    val text: String,
    val workerUrl: String,
)

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

private fun sha256(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}
