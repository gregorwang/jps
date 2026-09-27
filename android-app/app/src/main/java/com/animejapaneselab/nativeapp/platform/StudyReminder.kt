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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 放課後チャイム — a chain of inexact alarms, one per check ([ReminderSlot]). Each ring asks
 * [ReminderPlanner] whether to post, then arms the next check. Everything is computed on the
 * phone from StudyLog, 栞 and 活用 progress; no server, no push service.
 */
object StudyReminder {
    const val ExtraOpen = "com.animejapaneselab.nativeapp.extra.OPEN"
    private const val ExtraSlot = "slot"
    private const val StudyChannelId = "study-reminder"
    private const val ReviewChannelId = "review-due"
    private const val NotificationId = 4107
    private const val ActionRing = "com.animejapaneselab.nativeapp.action.STUDY_REMINDER"
    private const val WindowMillis = 10 * 60 * 1000L

    /** Arms or cancels the next check to match settings and habit. Safe to call often. */
    fun sync(context: Context) {
        val app = context.applicationContext
        val store = LocalLabStore(app)
        val settings = store.readSettings()
        val alarms = app.getSystemService(AlarmManager::class.java) ?: return
        alarms.cancel(ringIntent(app, ReminderSlot.Habit))
        if (!settings.studyReminder) return
        val habit = habitMinute(store)
        val (at, slot) = nextRing(habit, ReminderPlanner.reviewMinute(habit))
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), WindowMillis, ringIntent(app, slot))
    }

    /** The Habit check time as "HH:mm", for the settings row. */
    fun plannedTime(context: Context): String {
        val minute = habitMinute(LocalLabStore(context.applicationContext))
        return "%d:%02d".format(minute / 60, minute % 60)
    }

    private fun habitMinute(store: LocalLabStore): Int {
        val settings = store.readSettings()
        return ReminderPlanner.habitMinute(store.readStudyStarts(), settings.studyReminderAuto, settings.studyReminderHour)
    }

    internal fun nextRing(habit: Int, review: Int?, now: LocalDateTime = LocalDateTime.now()): Pair<LocalDateTime, ReminderSlot> {
        val today = now.toLocalDate()
        val candidates = listOfNotNull(
            today.atStartOfDay().plusMinutes(habit.toLong()) to ReminderSlot.Habit,
            review?.let { today.atStartOfDay().plusMinutes(it.toLong()) to ReminderSlot.Review },
            today.plusDays(1).atStartOfDay().plusMinutes(habit.toLong()) to ReminderSlot.Habit,
        )
        return candidates.first { it.first.isAfter(now.plusMinutes(1)) }
    }

    private fun ringIntent(context: Context, slot: ReminderSlot): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, StudyReminderReceiver::class.java).setAction(ActionRing).putExtra(ExtraSlot, slot.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun ring(context: Context, slotName: String?) {
        val app = context.applicationContext
        val store = LocalLabStore(app)
        val slot = ReminderSlot.entries.firstOrNull { it.name == slotName } ?: ReminderSlot.Habit
        if (store.readSettings().studyReminder) {
            val today = LocalDate.now()
            val message = ReminderPlanner.plan(slot, input(store, today))
            if (message != null && post(app, message)) {
                store.writeReminderPosted(message.template, habitPostedOn = today.toString().takeIf { slot == ReminderSlot.Habit })
            }
        }
        sync(app)
    }

    /** First study of the day: drop the nudge that is no longer needed and re-arm on the new habit. */
    fun onStudyStarted(context: Context) {
        val app = context.applicationContext
        app.getSystemService(NotificationManager::class.java)?.cancel(NotificationId)
        sync(app)
    }

    private fun input(store: LocalLabStore, today: LocalDate): ReminderInput {
        val log = store.readStudyLog()
        val todayKey = today.toString()
        val lastStudy = log.filter { (day, d) -> day < todayKey && d.activity > 0 }.keys.maxOrNull()
        val epochDay = today.toEpochDay()
        val line = TodayWidgetLine.decode(store.readTodayWidgetLine())
        return ReminderInput(
            today = today,
            studiedToday = (log[todayKey]?.activity ?: 0) > 0,
            lastStudyDay = lastStudy?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            shioriDue = NotebookRules.dueCount(store.readNotebook(), epochDay),
            drillDue = store.readDrillProgress().values.count { it.dueDay <= epochDay },
            fadingPoint = store.readDrillPointDue().filterValues { it <= epochDay }.minByOrNull { it.value }?.key,
            lineJa = line?.ja,
            lineZh = line?.zh,
            habitPostedToday = store.readReminderHabitPostedOn() == todayKey,
            lastTemplate = store.readReminderLastTemplate(),
        )
    }

    private fun post(context: Context, message: ReminderMessage): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        manager.createNotificationChannel(
            NotificationChannel(StudyChannelId, "放課後チャイム", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "当天还没学习时提醒，时间跟着你的学习习惯走"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(ReviewChannelId, "復習到期", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "栞和活用按遗忘曲线到期时提醒"
            },
        )
        val line = TodayWidgetLine.decode(LocalLabStore(context).readTodayWidgetLine())
        val accent = WorkThemes.of(WorkIdentity.hue(line?.workSlug.orEmpty()), dark = false).accent.toArgb()
        val notification = Notification.Builder(context, if (message.channel == ReminderChannel.Review) ReviewChannelId else StudyChannelId)
            .setSmallIcon(R.drawable.ic_learning_notification)
            .setContentTitle(message.title)
            .setContentText(message.body.lineSequence().first())
            .setStyle(Notification.BigTextStyle().bigText(message.body))
            .setColor(accent)
            .setContentIntent(openIntent(context, message.target))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .build()
        manager.notify(NotificationId, notification)
        return true
    }

    private fun openIntent(context: Context, target: ReminderTarget): PendingIntent = PendingIntent.getActivity(
        context,
        10 + target.ordinal,
        Intent(context, MainActivity::class.java)
            .putExtra(ExtraOpen, target.key)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Tab requests from notification taps, consumed by LabApp once the user is signed in. */
object LaunchRequests {
    private val _target = MutableStateFlow<ReminderTarget?>(null)
    val target: StateFlow<ReminderTarget?> = _target.asStateFlow()

    fun handle(intent: Intent?) {
        ReminderTarget.of(intent?.getStringExtra(StudyReminder.ExtraOpen))?.let { _target.value = it }
    }

    fun consume() {
        _target.value = null
    }
}

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> StudyReminder.sync(context)
            "com.animejapaneselab.nativeapp.action.STUDY_REMINDER" -> StudyReminder.ring(context, intent.getStringExtra("slot"))
        }
    }
}
