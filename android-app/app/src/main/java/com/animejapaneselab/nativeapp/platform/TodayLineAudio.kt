package com.animejapaneselab.nativeapp.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Keeps today's line as a local clip so 朝の一句's「▶ 原声」plays instantly from the notification,
 * without opening the app or waiting on the network. Source clip when there is one, else TTS.
 */
object TodayLineAudio {
    private const val Dir = "today-line"
    private const val Voice = "ja-JP-NanamiNeural"
    private const val UserAgent = "Mozilla/5.0"
    private const val MaxPlayMillis = 9_000L

    private fun file(context: Context, ja: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(ja.toByteArray()).joinToString("") { "%02x".format(it) }.take(24)
        return File(File(context.cacheDir, Dir), "$hash.mp3")
    }

    fun cached(context: Context, ja: String): File? = file(context, ja).takeIf { it.exists() && it.length() > 0 }

    /** Blocking download; call off the main thread. Keeps only today's clip. */
    fun prepare(context: Context, ja: String, sourceUrl: String, ttsWorkerUrl: String) {
        if (ja.isBlank() || cached(context, ja) != null) return
        val target = file(context, ja)
        target.parentFile?.apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val part = File(target.parentFile, target.name + ".part")
        runCatching {
            val connection = if (sourceUrl.isNotBlank()) {
                (URL(sourceUrl).openConnection() as HttpURLConnection).apply { setRequestProperty("User-Agent", UserAgent) }
            } else {
                val base = ttsWorkerUrl.trim().trimEnd('/').ifBlank { return }
                (URL("$base/tts").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", UserAgent)
                    outputStream.use { it.write(JSONObject().put("text", ja).put("voice", Voice).toString().toByteArray()) }
                }
            }
            connection.connectTimeout = 15_000
            connection.readTimeout = 25_000
            try {
                check(connection.responseCode in 200..299)
                connection.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
                check(part.length() > 0)
                part.renameTo(target)
            } finally {
                connection.disconnect()
            }
        }.onFailure { part.delete() }
    }

    /** Plays [clip] once; [done] runs when playback ends, fails, or hits the time cap. */
    fun play(clip: File, done: () -> Unit) {
        val player = MediaPlayer()
        val handler = Handler(Looper.getMainLooper())
        var finished = false
        val finish = {
            if (!finished) {
                finished = true
                handler.removeCallbacksAndMessages(null)
                runCatching { player.release() }
                done()
            }
        }
        runCatching {
            player.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
            )
            player.setDataSource(clip.path)
            player.setOnCompletionListener { finish() }
            player.setOnErrorListener { _, _, _ -> finish(); true }
            player.prepare()
            player.start()
            handler.postDelayed({ finish() }, MaxPlayMillis)
        }.onFailure { finish() }
    }
}
