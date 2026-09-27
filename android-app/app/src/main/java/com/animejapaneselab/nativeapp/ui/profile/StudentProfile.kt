package com.animejapaneselab.nativeapp.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.animejapaneselab.nativeapp.data.LocalLabStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** What the user wrote on their 学生証; blank fields fall back to the account / current work. */
data class StudentProfileData(
    val name: String = "",
    val affiliation: String = "",
    /** Bumped on every photo change so the card reloads it; 0 = no photo. */
    val photoVersion: Long = 0,
)

/**
 * Process-wide 学生証 profile: 氏名 / 所属 and a photo, kept on this phone ([LocalLabStore] +
 * one JPEG in app storage). 入学 and 出席 are not editable; they come from the study record.
 */
object StudentProfile {
    private const val PhotoFile = "student-photo.jpg"
    private const val PhotoWidth = 360
    private const val PhotoHeight = 440

    private val _state = MutableStateFlow(StudentProfileData())
    val state: StateFlow<StudentProfileData> = _state.asStateFlow()
    private var store: LocalLabStore? = null

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        store = LocalLabStore(context.applicationContext).also { _state.value = it.readStudentProfile() }
    }

    fun photoFile(context: Context): File = File(context.applicationContext.filesDir, PhotoFile)

    fun update(context: Context, name: String, affiliation: String) {
        init(context)
        save(_state.value.copy(name = name.trim().take(24), affiliation = affiliation.trim().take(32)))
    }

    /** Center-crops the picked image to the card's 72×88 frame and stores it; false if unreadable. */
    suspend fun setPhoto(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        init(context)
        val source = runCatching { decode(context, uri) }.getOrNull() ?: return@withContext false
        val cropped = cropToFrame(source)
        val ok = runCatching {
            photoFile(context).outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        }.isSuccess
        if (ok) save(_state.value.copy(photoVersion = System.currentTimeMillis()))
        ok
    }

    fun clearPhoto(context: Context) {
        init(context)
        photoFile(context).delete()
        save(_state.value.copy(photoVersion = 0))
    }

    /** The stored photo, or null. Call off the main thread. */
    fun loadPhoto(context: Context): Bitmap? {
        val file = photoFile(context)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    private fun save(next: StudentProfileData) {
        _state.value = next
        store?.writeStudentProfile(next)
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder applies EXIF rotation; downsample big photos on the way in.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val scale = maxOf(1, minOf(info.size.width / (PhotoWidth * 2), info.size.height / (PhotoHeight * 2)))
                decoder.setTargetSize(info.size.width / scale, info.size.height / scale)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val sample = maxOf(1, minOf(bounds.outWidth / (PhotoWidth * 2), bounds.outHeight / (PhotoHeight * 2)))
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        }
    }

    private fun cropToFrame(source: Bitmap): Bitmap {
        val target = PhotoWidth.toFloat() / PhotoHeight
        val w = source.width
        val h = source.height
        val (cw, ch) = if (w.toFloat() / h > target) ((h * target).toInt() to h) else (w to (w / target).toInt())
        // Faces sit in the upper part of most portraits: bias the vertical crop upward.
        val x = (w - cw) / 2
        val y = ((h - ch) * 0.3f).toInt()
        val cropped = Bitmap.createBitmap(source, x, y, cw.coerceAtMost(w - x), ch.coerceAtMost(h - y))
        return Bitmap.createScaledBitmap(cropped, PhotoWidth, PhotoHeight, true)
    }
}
