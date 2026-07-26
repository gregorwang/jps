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
import android.graphics.Color
import android.os.Build
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import com.animejapaneselab.nativeapp.MainActivity
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.ui.LearningSessionStatus

class LearningSessionNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)

    fun beginSession(status: LearningSessionStatus) {
        preferences.edit { putBoolean(DismissedKey, false) }
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
        val builder = Notification.Builder(appContext, ChannelId)
            .setSmallIcon(R.drawable.ic_learning_notification)
            .setContentTitle(status.title)
            .setContentText(status.subtitle)
            .setSubText("已完成 ${status.completed}/${status.total}")
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setLocalOnly(true)
            .setColor(Color.rgb(88, 204, 2))
            .setTimeoutAfter(MaxSessionDurationMs)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setStyle(
                Notification.ProgressStyle()
                    .addProgressSegment(
                        Notification.ProgressStyle.Segment(status.total)
                            .setColor(Color.rgb(88, 204, 2)),
                    )
                    .setProgress(status.completed)
                    .setStyledByProgress(true),
            )
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

    private companion object {
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
