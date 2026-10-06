package com.animejapaneselab.nativeapp.ui.radio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.platform.RadioService
import com.animejapaneselab.nativeapp.ui.study.StudyLog
import com.animejapaneselab.nativeapp.ui.voicepack.VoicePack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class RadioMode { Nap, Walk }

data class RadioState(
    /** Track ids in play order; empty = nothing loaded. */
    val queue: List<String> = emptyList(),
    val index: Int = 0,
    val seg: Int = 0,
    val playing: Boolean = false,
    /** Nap mode after the queue ran out: only the Japanese of the queue, slower, until the timer. */
    val recap: Boolean = false,
    val recapSeg: RadioSeg? = null,
    val mode: RadioMode = RadioMode.Nap,
    val napMinutes: Int = 20,
    /** Wall clock the nap timer stops at; 0 = not running. */
    val napEndsAt: Long = 0,
    val heard: Set<String> = emptySet(),
    val notice: String = "",
) {
    val active: Boolean get() = queue.isNotEmpty()
    val trackId: String? get() = queue.getOrNull(index)
}

/**
 * 知識电台的播放器 (process-wide, like the other holders). Plays a track segment by segment:
 * the voice pack first (Emilia; keyed by the text, so her Chinese clips slot in later without code),
 * else the original clip for anime lines, else the phone's TTS in the segment's language.
 * [RadioService] keeps it alive with the screen off and puts it on the lock screen.
 */
object RadioPlayer {
    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state.asStateFlow()

    private lateinit var app: Context
    private var store: LocalLabStore? = null
    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var playerPaused = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsInit = false
    private var pendingSpeak: (() -> Unit)? = null
    /** Bumped at every segment start / stop; callbacks of an older segment are ignored. */
    private var token = 0
    private var recapSegs: List<RadioSeg> = emptyList()
    private var recapPos = 0
    private var napLeftMs = 0L
    private var lastRecord = 0L
    private var focusRequest: AudioFocusRequest? = null
    private var pausedByFocus = false

    fun init(context: Context) {
        if (store != null) return
        app = context.applicationContext
        val s = LocalLabStore(app)
        store = s
        runCatching { JSONObject(s.readRadio() ?: return) }.getOrNull()?.let { j ->
            _state.update {
                it.copy(
                    heard = j.optJSONArray("heard")?.let { a -> List(a.length()) { i -> a.getString(i) }.toSet() }.orEmpty(),
                    mode = if (j.optString("mode") == "walk") RadioMode.Walk else RadioMode.Nap,
                    napMinutes = j.optInt("nap", 20),
                )
            }
        }
    }

    fun track(): RadioTrack? = _state.value.trackId?.let(RadioCatalog::track)

    /** The segment sounding (or paused on) now. */
    fun seg(): RadioSeg? {
        val s = _state.value
        return if (s.recap) s.recapSeg else track()?.segs?.getOrNull(s.seg)
    }

    /** Plays [trackId], then the rest of [then] (the shelf after it). */
    fun play(context: Context, trackId: String, then: List<String> = emptyList()) {
        init(context)
        halt()
        _state.update { it.copy(queue = listOf(trackId) + then.filter { id -> id != trackId }, index = 0, seg = 0, recap = false, recapSeg = null, notice = "") }
        start()
    }

    fun enqueue(context: Context, trackId: String) {
        init(context)
        if (!_state.value.active) return play(context, trackId)
        _state.update { if (trackId in it.queue.drop(it.index + 1)) it else it.copy(queue = it.queue + trackId) }
    }

    fun toggle() = if (_state.value.playing) pause() else resume()

    fun pause() {
        if (!_state.value.playing) return
        token++
        main.removeCallbacksAndMessages(null)
        player?.let { if (it.isPlaying) { it.pause(); playerPaused = true } else release() }
        tts?.stop()
        val s = _state.value
        napLeftMs = if (s.napEndsAt > 0) (s.napEndsAt - System.currentTimeMillis()).coerceAtLeast(0) else 0
        _state.update { it.copy(playing = false, napEndsAt = 0) }
        RadioService.sync(app)
    }

    fun resume() {
        val s = _state.value
        if (!s.active || s.playing) return
        if (s.mode == RadioMode.Nap) armNap(if (napLeftMs > 0) napLeftMs else s.napMinutes * 60_000L)
        _state.update { it.copy(playing = true) }
        requestFocus()
        val p = player
        if (p != null && playerPaused) {
            playerPaused = false
            val t = ++token
            p.setOnCompletionListener { done(t) }
            p.start()
            tick()
            RadioService.sync(app)
        } else {
            startSeg()
        }
    }

    fun next() = jump(+1)

    fun previous() = jump(-1)

    /** 再听这句. */
    fun repeat() {
        if (!_state.value.active) return
        halt()
        _state.update { it.copy(playing = true) }
        startSeg()
    }

    fun stop() {
        if (store == null) return
        halt()
        abandonFocus()
        _state.update { it.copy(queue = emptyList(), index = 0, seg = 0, playing = false, recap = false, recapSeg = null, napEndsAt = 0) }
        napLeftMs = 0
        RadioService.sync(app)
    }

    fun setMode(mode: RadioMode) {
        _state.update {
            it.copy(mode = mode, napEndsAt = if (mode == RadioMode.Nap && it.playing) System.currentTimeMillis() + it.napMinutes * 60_000L else 0)
        }
        napLeftMs = 0
        if (mode == RadioMode.Walk) setVolume(1f)
        save()
    }

    /** Cycles the nap timer 20 → 30 → 45 → 15 minutes and restarts it. */
    fun cycleNapMinutes() {
        val next = when (_state.value.napMinutes) { 15 -> 20; 20 -> 30; 30 -> 45; else -> 15 }
        _state.update { it.copy(napMinutes = next, napEndsAt = if (it.playing && it.mode == RadioMode.Nap) System.currentTimeMillis() + next * 60_000L else 0) }
        napLeftMs = 0
        save()
    }

    // ------------------------------------------------------------------ sequencing

    private fun start() {
        val s = _state.value
        if (s.mode == RadioMode.Nap) armNap(s.napMinutes * 60_000L)
        _state.update { it.copy(playing = true) }
        requestFocus()
        startSeg()
    }

    private fun armNap(ms: Long) {
        napLeftMs = 0
        _state.update { it.copy(napEndsAt = System.currentTimeMillis() + ms) }
    }

    private fun jump(delta: Int) {
        val s = _state.value
        val t = track() ?: return
        halt()
        if (s.recap) {
            recapPos = (recapPos + delta).coerceAtLeast(0)
            _state.update { it.copy(playing = true) }
            return startSeg()
        }
        val target = s.seg + delta
        when {
            target < 0 && s.index > 0 -> _state.update { it.copy(index = it.index - 1, seg = 0) }
            target < 0 -> _state.update { it.copy(seg = 0) }
            target >= t.segs.size -> return advanceTrack()
            else -> _state.update { it.copy(seg = target) }
        }
        _state.update { it.copy(playing = true) }
        startSeg()
    }

    private fun startSeg() {
        val s = _state.value
        val seg = if (s.recap) recapSegs.getOrNull(recapPos % recapSegs.size.coerceAtLeast(1)) else track()?.segs?.getOrNull(s.seg)
        if (seg == null) return if (s.recap) stop() else advanceTrack()
        if (s.recap) _state.update { it.copy(recapSeg = seg) }
        val t = ++token
        record()
        tick()
        RadioService.sync(app)
        // Anime lines: the original clip is the point. Everything else: Emilia's pack, then the phone.
        val pack = VoicePack.fileFor(app, seg.text)
        val fromPack = { if (pack != null) playMedia(pack.absolutePath, t) { speak(seg, t) } else speak(seg, t) }
        when {
            seg.kind == SegKind.Orig && seg.audioUrl.isNotEmpty() -> playMedia(seg.audioUrl, t, fromPack)
            else -> fromPack()
        }
    }

    /** A segment finished: wait a breath, then the next one. */
    private fun done(t: Int) {
        if (t != token || !_state.value.playing) return
        val s = _state.value
        val seg = seg()
        val gap = when {
            s.recap -> 1800L
            seg?.kind == SegKind.Orig -> 700L
            seg?.kind == SegKind.Ja -> 450L
            else -> 280L
        }
        main.postDelayed({
            if (t != token || !_state.value.playing) return@postDelayed
            if (_state.value.recap) {
                recapPos++
                startSeg()
            } else {
                val track = track()
                if (track != null && _state.value.seg + 1 < track.segs.size) {
                    _state.update { it.copy(seg = it.seg + 1) }
                    startSeg()
                } else {
                    advanceTrack()
                }
            }
        }, gap)
    }

    private fun advanceTrack() {
        val s = _state.value
        s.trackId?.let { id -> _state.update { it.copy(heard = it.heard + id) }; save() }
        if (s.index + 1 < s.queue.size) {
            _state.update { it.copy(index = it.index + 1, seg = 0) }
            return startSeg()
        }
        // Queue done. Nap: keep murmuring the Japanese until the timer; walk: stop.
        if (s.mode == RadioMode.Nap && s.napEndsAt > System.currentTimeMillis()) {
            recapSegs = s.queue.mapNotNull(RadioCatalog::track).flatMap { tr -> tr.segs.filter { it.kind != SegKind.Zh } }
            recapPos = 0
            if (recapSegs.isNotEmpty()) {
                _state.update { it.copy(recap = true) }
                return startSeg()
            }
        }
        stop()
    }

    private fun halt() {
        token++
        main.removeCallbacksAndMessages(null)
        release()
        tts?.stop()
    }

    // ------------------------------------------------------------------ nap timer / volume / time studied

    private fun tick() {
        main.removeCallbacks(ticker)
        main.postDelayed(ticker, 5_000)
        setVolume(volumeNow())
    }

    private val ticker: Runnable = Runnable {
        val s = _state.value
        if (!s.playing) return@Runnable
        if (s.napEndsAt > 0 && System.currentTimeMillis() >= s.napEndsAt) {
            stop()
            return@Runnable
        }
        setVolume(volumeNow())
        main.postDelayed(ticker, 5_000)
    }

    /** Full until the last 6 minutes of the nap timer, then down to a whisper. */
    private fun volumeNow(): Float {
        val s = _state.value
        if (s.mode != RadioMode.Nap || s.napEndsAt == 0L) return 1f
        val left = s.napEndsAt - System.currentTimeMillis()
        val fade = 6 * 60_000f
        return (left / fade).coerceIn(0.12f, 1f)
    }

    private fun setVolume(v: Float) {
        runCatching { player?.setVolume(v, v) }
    }

    /** Listening counts as study time (same ≤180 s gap rule as answers); one prefs write per 30 s. */
    private fun record() {
        val now = System.currentTimeMillis()
        if (now - lastRecord < 30_000) return
        lastRecord = now
        StudyLog.recordRead(app)
    }

    private fun save() {
        val s = _state.value
        store?.writeRadio(
            JSONObject()
                .put("heard", JSONArray(s.heard.toList()))
                .put("mode", if (s.mode == RadioMode.Walk) "walk" else "nap")
                .put("nap", s.napMinutes)
                .toString(),
        )
    }

    // ------------------------------------------------------------------ sound

    private fun playMedia(source: String, t: Int, fallback: () -> Unit) {
        release()
        val p = MediaPlayer()
        player = p
        playerPaused = false
        runCatching {
            p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            p.setWakeMode(app, PowerManager.PARTIAL_WAKE_LOCK)
            p.setDataSource(source)
            p.setOnPreparedListener { if (t == token && _state.value.playing) { it.setVolume(volumeNow(), volumeNow()); it.start() } }
            p.setOnCompletionListener { done(t) }
            p.setOnErrorListener { _, _, _ -> if (t == token) { release(); fallback() }; true }
            p.prepareAsync()
        }.onFailure { if (t == token) { release(); fallback() } }
    }

    private fun release() {
        player?.let { runCatching { it.reset(); it.release() } }
        player = null
        playerPaused = false
    }

    private fun speak(seg: RadioSeg, t: Int) {
        val engine = tts ?: TextToSpeech(app) { status ->
            main.post {
                ttsInit = true
                ttsReady = status == TextToSpeech.SUCCESS
                if (!ttsReady) _state.update { it.copy(notice = "手机的语音合成用不了") }
                pendingSpeak?.invoke()
                pendingSpeak = null
            }
        }.also { e ->
            tts = e
            e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) { utteranceId?.toIntOrNull()?.let { id -> main.post { done(id) } } }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { utteranceId?.toIntOrNull()?.let { id -> main.post { done(id) } } }
                override fun onError(utteranceId: String?, errorCode: Int) { utteranceId?.toIntOrNull()?.let { id -> main.post { done(id) } } }
            })
        }
        val go = go@{
            if (t != token) return@go
            if (!ttsReady) { main.postDelayed({ done(t) }, 400); return@go }
            val zh = seg.kind == SegKind.Zh
            val ok = engine.setLanguage(if (zh) Locale.SIMPLIFIED_CHINESE else Locale.JAPANESE)
            if (ok == TextToSpeech.LANG_MISSING_DATA || ok == TextToSpeech.LANG_NOT_SUPPORTED) {
                _state.update { it.copy(notice = if (zh) "手机没有中文语音，去系统设置 → 文字转语音 里装一个" else "手机没有日语语音") }
                main.postDelayed({ done(t) }, 400)
                return@go
            }
            engine.setSpeechRate(if (zh) 1.0f else 0.92f)
            val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeNow()) }
            engine.speak(seg.text, TextToSpeech.QUEUE_FLUSH, params, t.toString())
        }
        if (ttsInit) go() else pendingSpeak = go
    }

    // ------------------------------------------------------------------ audio focus (a call pauses the radio)

    private fun requestFocus() {
        val am = app.getSystemService(AudioManager::class.java) ?: return
        val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener({ change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        if (_state.value.playing) { pausedByFocus = change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT; pause() }
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> if (pausedByFocus) { pausedByFocus = false; resume() }
                }
            }, main)
            .build().also { focusRequest = it }
        am.requestAudioFocus(req)
    }

    private fun abandonFocus() {
        val am = app.getSystemService(AudioManager::class.java) ?: return
        focusRequest?.let { am.abandonAudioFocusRequest(it) }
    }
}
