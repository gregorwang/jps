package com.animejapaneselab.nativeapp.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity

/** Square face crops of the work's representative character, for notification large icons. */
object CharacterFaces {
    private const val SizePx = 192
    private val cache = mutableMapOf<Int, Bitmap?>()

    @Synchronized
    fun face(context: Context, workSlug: String, episode: Int = 1): Bitmap? {
        val character = WorkIdentity.representative(workSlug.ifBlank { "re-zero" }, episode.coerceAtLeast(1)) ?: return null
        val res = character.drawable ?: return null
        return cache.getOrPut(res) {
            runCatching {
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                val image = BitmapFactory.decodeResource(context.resources, res, options) ?: return@runCatching null
                val face = character.face
                val side = (image.width * face.size * 1.35f).coerceAtMost(minOf(image.width, image.height).toFloat()).toInt()
                val left = (image.width * face.cx - side / 2f).toInt().coerceIn(0, image.width - side)
                val top = (image.height * face.cy - side / 2f).toInt().coerceIn(0, image.height - side)
                Bitmap.createScaledBitmap(Bitmap.createBitmap(image, left, top, side, side), SizePx, SizePx, true)
            }.getOrNull()
        }
    }
}
