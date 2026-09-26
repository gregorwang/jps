package com.animejapaneselab.nativeapp.platform

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import com.animejapaneselab.nativeapp.MainActivity
import com.animejapaneselab.nativeapp.R
import androidx.compose.ui.graphics.toArgb
import com.animejapaneselab.nativeapp.ui.LearningSessionStatus
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.theme.WorkThemes

class LearningSessionNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)

    fun beginSession(status: LearningSessionStatus) {
        preferences.edit {
            putBoolean(DismissedKey, false)
            putLong(StartedAtKey, System.currentTimeMillis())
        }
        update(status)
    }

    fun update(status: LearningSessionStatus) {
        if (preferences.getBoolean(DismissedKey, false)) return
        if (!canPost()) return
        ensureChannel()
        manager?.notify(NotificationId, buildNotification(status))
    }

    fun endSession() {
        manager?.cancel(NotificationId)
        preferences.edit { putBoolean(DismissedKey, false) }
    }

    internal fun markDismissed() {
        preferences.edit { putBoolean(DismissedKey, true) }
    }

    private val preferences by lazy {
        appContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    }

    private fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            ChannelId,
            "学习实时状态",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "显示当前训练进度、锁屏状态和 Android 实时更新"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager?.createNotificationChannel(channel)
    }

    private fun buildNotification(status: LearningSessionStatus): Notification {
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val deleteIntent = PendingIntent.getBroadcast(
            appContext,
            1,
            Intent(appContext, LearningSessionNotificationDismissReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Re:ゼロ 第一話 in the header, the line being studied as the title, 「单点训练 · 第 2 / 4 问」 below;
        // work colour + character face so it reads as this app rather than a generic progress bar.
        val accent = WorkThemes.of(WorkIdentity.hue(status.workSlug), dark = false).accent.toArgb()
        val (mode, topic) = splitTitle(status.title)
        val header = listOfNotNull(
            WorkIdentity.displayName(status.workSlug).ifBlank { null },
            status.episode.takeIf { it > 0 }?.let(TextRules::episodeLabel),
        ).joinToString(" ").ifBlank { status.subtitle }
        val startedAt = preferences.getLong(StartedAtKey, 0L).takeIf { it > 0 } ?: System.currentTimeMillis()
        val builder = Notification.Builder(appContext, ChannelId)
            .setSmallIcon(R.drawable.ic_learning_notification)
            .setContentTitle(topic ?: mode)
            .setContentText(listOfNotNull(mode.takeIf { topic != null }, "第 ${status.position} / ${status.total} 问").joinToString(" · "))
            .setSubText(header)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setWhen(startedAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setLocalOnly(true)
            .setColor(accent)
            .setTimeoutAfter(MaxSessionDurationMs)
        characterFace(status)?.let { builder.setLargeIcon(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            // One segment per question (a 場面 strip), dimmed until answered.
            val perQuestion = status.total in 2..MaxSegments
            val style = Notification.ProgressStyle()
                .setStyledByProgress(true)
                .setProgressTrackerIcon(Icon.createWithResource(appContext, R.drawable.ic_learning_notification).setTint(accent))
                .setProgress(status.completed)
            if (perQuestion) {
                repeat(status.total) { style.addProgressSegment(Notification.ProgressStyle.Segment(1).setColor(accent)) }
            } else {
                style.addProgressSegment(Notification.ProgressStyle.Segment(status.total).setColor(accent))
            }
            builder.setStyle(style)
        } else {
            builder.setProgress(status.total, status.completed, false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
            Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1
        ) {
            builder
                .setRequestPromotedOngoing(true)
                .setShortCriticalText(status.chipText)
        }
        return builder.build()
    }

    /** 「单点训练 · やばい…」 → (单点训练, やばい…); a title without a topic stays whole. */
    private fun splitTitle(title: String): Pair<String, String?> {
        val parts = title.split(" · ", limit = 2)
        return if (parts.size == 2 && parts[1].isNotBlank()) parts[0] to parts[1] else title to null
    }

    private fun characterFace(status: LearningSessionStatus): Bitmap? {
        val character = WorkIdentity.representative(status.workSlug, status.episode.coerceAtLeast(1)) ?: return null
        val res = character.drawable ?: return null
        return faceCache.getOrPut(res) {
            runCatching {
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                val image = BitmapFactory.decodeResource(appContext.resources, res, options) ?: return@runCatching null
                val face = character.face
                val side = (image.width * face.size).coerceAtMost(minOf(image.width, image.height).toFloat()).toInt()
                val left = (image.width * face.cx - side / 2f).toInt().coerceIn(0, image.width - side)
                val top = (image.height * face.cy - side / 2f).toInt().coerceIn(0, image.height - side)
                Bitmap.createScaledBitmap(Bitmap.createBitmap(image, left, top, side, side), FaceSizePx, FaceSizePx, true)
            }.getOrNull()
        }
    }

    private companion object {
        const val MaxSegments = 24
        const val FaceSizePx = 192
        const val StartedAtKey = "session-started-at"
        val faceCache = mutableMapOf<Int, Bitmap?>()
        const val ChannelId = "learning-session-live-update"
        const val NotificationId = 1601
        const val MaxSessionDurationMs = 2 * 60 * 60 * 1000L
        const val PreferencesName = "learning-live-update-state"
        const val DismissedKey = "dismissed-current-session"
    }
}

class LearningSessionNotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        LearningSessionNotifier(context).markDismissed()
    }
}
