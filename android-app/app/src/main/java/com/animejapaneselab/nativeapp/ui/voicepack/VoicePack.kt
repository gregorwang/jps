package com.animejapaneselab.nativeapp.ui.voicepack

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class VoicePackState(
    /** Clips in the imported pack; 0 = no pack. */
    val clips: Int = 0,
    val importing: Boolean = false,
    val message: String = "",
)

/**
 * Pre-generated Emilia-voice clips, imported once from a zip the user copies to the phone
 * (`manifest.json` + `<key>.ogg`, built by archive-content-sources/emilia-voice/batch.py).
 * Lives in app storage, so App updates never touch it. [LessonAudioController] asks [fileFor]
 * before any TTS; texts not in the pack keep using the phone / Microsoft TTS.
 */
object VoicePack {
    private const val Dir = "voice-pack"
    private val _state = MutableStateFlow(VoicePackState())
    val state: StateFlow<VoicePackState> = _state.asStateFlow()
    @Volatile private var files: Map<String, String>? = null

    fun init(context: Context) {
        if (files == null) load(context)
    }

    /** The clip for exactly this text (the batch keyed sha1(text.strip())[:16]), or null. */
    fun fileFor(context: Context, text: String): File? {
        init(context)
        val name = files?.get(keyOf(text)) ?: return null
        return File(dir(context), name).takeIf { it.isFile }
    }

    suspend fun import(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(importing = true, message = "")
        val app = context.applicationContext
        val staging = File(app.filesDir, "$Dir.part").apply { deleteRecursively(); mkdirs() }
        val result = runCatching {
            app.contentResolver.openInputStream(uri)!!.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        val name = entry.name.substringAfterLast('/')
                        if (entry.isDirectory || !(name.endsWith(".ogg") || name == "manifest.json")) continue
                        File(staging, name).outputStream().use { zip.copyTo(it) }
                    }
                }
            }
            check(parse(staging).isNotEmpty()) { "压缩包里没有 manifest.json 或音频" }
            dir(app).deleteRecursively()
            check(staging.renameTo(dir(app))) { "无法保存语音包" }
        }
        staging.deleteRecursively()
        load(app)
        _state.value = _state.value.copy(
            importing = false,
            message = result.fold({ "已导入 ${_state.value.clips} 条" }, { "导入失败：${it.message ?: "未知错误"}" }),
        )
    }

    fun remove(context: Context) {
        dir(context.applicationContext).deleteRecursively()
        load(context)
        _state.value = _state.value.copy(message = "")
    }

    private fun load(context: Context) {
        val map = parse(dir(context.applicationContext))
        files = map
        _state.value = _state.value.copy(clips = map.size)
    }

    private fun parse(folder: File): Map<String, String> {
        val manifest = File(folder, "manifest.json").takeIf { it.isFile } ?: return emptyMap()
        val json = runCatching { JSONObject(manifest.readText()) }.getOrNull() ?: return emptyMap()
        return json.keys().asSequence().mapNotNull { key ->
            val file = json.optJSONObject(key)?.optString("file").orEmpty()
            if (file.isNotBlank() && File(folder, file).isFile) key to file else null
        }.toMap()
    }

    private fun dir(context: Context) = File(context.applicationContext.filesDir, Dir)

    private fun keyOf(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.trim { it.isWhitespace() }.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }
}
