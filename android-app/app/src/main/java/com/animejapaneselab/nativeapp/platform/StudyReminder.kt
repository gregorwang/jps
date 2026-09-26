package com.animejapaneselab.nativeapp.platform

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.animejapaneselab.nativeapp.MainActivity
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.theme.WorkThemes
import com.animejapaneselab.nativeapp.widget.TodayWidgetLine
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 放課後チャイム — one inexact alarm a day at the chosen hour. When it rings and nothing was
 * answered today (StudyLog), it posts a quiet nudge carrying today's line and the 栞 due count.
 * Studied already → it stays silent and just re-arms for tomorrow.
 */
object StudyReminder {
    private const val ChannelId = "study-reminder"
    private const val NotificationId = 4107
    private const val ActionRing = "com.animejapaneselab.nativeapp.action.STUDY_REMINDER"
    private const val WindowMillis = 20 * 60 * 1000L

    /** Arms or cancels the alarm to match the stored settings. Safe to call often. */
    fun sync(context: Context) {
        val app = context.applicationContext
        val settings = LocalLabStore(app).readSettings()
        val alarms = app.getSystemService(AlarmManager::class.java) ?: return
        val pending = ringIntent(app)
        alarms.cancel(pending)
        if (!settings.studyReminder) return
        alarms.setWindow(AlarmManager.RTC_WAKEUP, nextTrigger(settings.studyReminderHour), WindowMillis, pending)
    }

    internal fun nextTrigger(hour: Int, now: LocalDateTime = LocalDateTime.now(), zone: ZoneId = ZoneId.systemDefault()): Long {
        var at = now.toLocalDate().atTime(hour.coerceIn(0, 23), 0)
        if (!at.isAfter(now.plusMinutes(1))) at = at.plusDays(1)
        return at.atZone(zone).toInstant().toEpochMilli()
    }

    private fun ringIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, StudyReminderReceiver::class.java).setAction(ActionRing),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun ring(context: Context) {
        val app = context.applicationContext
        val store = LocalLabStore(app)
        val today = LocalDate.now()
        val studied = (store.readStudyLog()[today.toString()]?.answers ?: 0) > 0
        if (!studied && store.readSettings().studyReminder) post(app, store, today)
        sync(app)
    }

    private fun post(context: Context, store: LocalLabStore, today: LocalDate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(ChannelId, "放課後チャイム", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "当天还没学习时，在设定的时间提醒一次"
            },
        )
        val line = TodayWidgetLine.decode(store.readTodayWidgetLine())
        val shioriDue = NotebookRules.dueCount(store.readNotebook(), today.toEpochDay())
        val body = buildString {
            if (line != null) {
                append("「").append(line.ja).append("」")
                if (line.zh.isNotBlank()) append("\n").append(line.zh)
            } else {
                append("今天的一句还在等你。")
            }
            if (shioriDue > 0) append("\n栞到期 ").append(shioriDue).append(" 枚")
        }
        val open = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val accent = WorkThemes.of(WorkIdentity.hue(line?.workSlug.orEmpty()), dark = false).accent.toArgb()
        val notification = Notification.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_learning_notification)
            .setContentTitle("放課後チャイム · 今天还没开课")
            .setContentText(body.lineSequence().first())
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setSubText(line?.let { listOf(WorkIdentity.displayName(it.workSlug), it.episodeLabel).filter(String::isNotBlank).joinToString(" ") })
            .setColor(accent)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        manager.notify(NotificationId, notification)
    }
}

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> StudyReminder.sync(context)
            "com.animejapaneselab.nativeapp.action.STUDY_REMINDER" -> StudyReminder.ring(context)
        }
    }
}
