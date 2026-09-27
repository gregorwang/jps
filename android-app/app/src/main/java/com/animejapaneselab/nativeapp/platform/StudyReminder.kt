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
import com.animejapaneselab.nativeapp.ui.notebook.Notebook
import com.animejapaneselab.nativeapp.widget.TodayWidget
import com.animejapaneselab.nativeapp.widget.LabArt
import android.graphics.drawable.Icon
import android.widget.RemoteViews
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
    const val ActionPlayLine = "com.animejapaneselab.nativeapp.action.PLAY_TODAY_LINE"
    const val ActionSaveLine = "com.animejapaneselab.nativeapp.action.SAVE_TODAY_LINE"
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
        val morning = if (settings.morningLine) ReminderPlanner.morningMinute(habit) else null
        val (at, slot) = nextRing(habit, ReminderPlanner.reviewMinute(habit), morning)
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

    internal fun nextRing(
        habit: Int,
        review: Int?,
        morning: Int?,
        now: LocalDateTime = LocalDateTime.now(),
    ): Pair<LocalDateTime, ReminderSlot> {
        fun day(offset: Long) = listOfNotNull(
            morning?.let { it to ReminderSlot.Morning },
            habit to ReminderSlot.Habit,
            review?.let { it to ReminderSlot.Review },
        ).map { (minute, slot) -> now.toLocalDate().plusDays(offset).atStartOfDay().plusMinutes(minute.toLong()) to slot }
        return (day(0) + day(1)).first { it.first.isAfter(now.plusMinutes(1)) }
    }

    /** 栞 + 活用 cards due today (the widget's 復習 count). */
    fun dueCount(context: Context): Int {
        val store = LocalLabStore(context.applicationContext)
        val epochDay = LocalDate.now().toEpochDay()
        return NotebookRules.dueCount(store.readNotebook(), epochDay) + store.readDrillProgress().values.count { it.dueDay <= epochDay }
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
        val today = LocalDate.now()
        if (store.readSettings().studyReminder && store.readReminderPostedCount(today.toString()) < ReminderPlanner.DailyCap) {
            val message = ReminderPlanner.plan(slot, input(store, today))
            if (message != null && post(app, message)) {
                store.writeReminderPosted(message.template, today.toString(), habitPostedOn = today.toString().takeIf { slot == ReminderSlot.Habit })
            }
        }
        TodayWidget.refreshAll(app)
        sync(app)
    }

    /** First study of the day: drop the nudge that is no longer needed and re-arm on the new habit. */
    fun onStudyStarted(context: Context) {
        val app = context.applicationContext
        app.getSystemService(NotificationManager::class.java)?.cancel(NotificationId)
        TodayWidget.refreshAll(app)
        sync(app)
    }

    /** 設定's test button: posts today's 朝の一句 right now; false when notifications are blocked. */
    fun postTest(context: Context): Boolean {
        val app = context.applicationContext
        val source = morningSource(LocalLabStore(app))
        val body = source?.let {
            buildString {
                append("「").append(it.ja).append("」")
                if (it.zh.isNotBlank()) append("\n").append(it.zh)
            }
        } ?: "提醒能正常送达。"
        return post(app, ReminderMessage("morning", ReminderChannel.Study, "放課後チャイム · 测试", body, ReminderTarget.Today))
    }

    /** 朝の一句 → 挟む: saves the line to 栞 and re-posts the notification without that action. */
    internal fun saveTodayLine(context: Context) {
        val app = context.applicationContext
        val source = morningSource(LocalLabStore(app)) ?: return
        NotebookRules.decode(source.entry).firstOrNull()?.let { Notebook.save(app, it) }
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        if (manager.activeNotifications.none { it.id == NotificationId }) return
        val body = buildString {
            append("「").append(source.ja).append("」")
            if (source.zh.isNotBlank()) append("\n").append(source.zh)
        }
        post(app, ReminderMessage("morning", ReminderChannel.Study, "今日の一句 · 已挟入栞", body, ReminderTarget.Today), saved = true)
    }

    internal fun morningClip(context: Context) = morningSource(LocalLabStore(context))?.let { TodayLineAudio.cached(context, it.ja) }

    private data class MorningSource(
        val ja: String,
        val zh: String,
        val entry: String,
        val eyebrow: String,
        val seal: String,
        /** Accent line under the translation: the 拆解 for a lesson line, who / where for an episode line. */
        val note: String,
        val workSlug: String,
    )

    /** What 朝の一句 shows today: the main-line pick when fresh, else the episode's line. */
    private fun morningSource(store: LocalLabStore): MorningSource? {
        MorningPick.decode(store.readMorningPick())?.takeIf { MorningPick.isFor(it, LocalDate.now()) }?.let {
            val short = it.point.substringBefore('（').substringBefore(' ').trim()
            return MorningSource(
                ja = it.ja,
                zh = it.zh,
                entry = it.entry,
                eyebrow = listOf("朝の一句", short).filter(String::isNotBlank).joinToString(" · "),
                seal = short.take(2).let { s -> if (s.length == 2) "${s[0]}\n${s[1]}" else s.ifBlank { "学" } },
                note = it.formula,
                workSlug = "",
            )
        }
        return TodayWidgetLine.decode(store.readTodayWidgetLine())?.let { lineSource(it) }
    }

    private fun lineSource(line: TodayWidgetLine): MorningSource {
        val work = listOf(WorkIdentity.displayName(line.workSlug), line.episodeLabel).filter(String::isNotBlank).joinToString(" ")
        val speaker = line.attribution.substringBefore(" · ").trim().takeUnless { it.isBlank() || it.any(Char::isDigit) }
        return MorningSource(
            ja = line.ja,
            zh = line.zh,
            entry = line.notebookEntry,
            eyebrow = listOf("今日の一句", work).filter(String::isNotBlank).joinToString(" · "),
            seal = WorkIdentity.sealText(line.workSlug),
            note = listOfNotNull(speaker, work.ifBlank { null }).joinToString(" · "),
            workSlug = line.workSlug,
        )
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
            morningPick = MorningPick.decode(store.readMorningPick())?.takeIf { MorningPick.isFor(it, today) },
        )
    }

    private fun post(context: Context, message: ReminderMessage, saved: Boolean = false): Boolean {
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
        val store = LocalLabStore(context)
        val line = TodayWidgetLine.decode(store.readTodayWidgetLine())
        val content = runCatching { contentViews(context, store, message, line, saved) }.getOrNull()
        val accent = WorkThemes.of(WorkIdentity.hue(content?.workSlug ?: line?.workSlug.orEmpty()), dark = false).accent.toArgb()
        val builder = Notification.Builder(context, if (message.channel == ReminderChannel.Review) ReviewChannelId else StudyChannelId)
            .setSmallIcon(sealIcon(context))
            .setContentTitle(message.title)
            .setContentText(message.body.lineSequence().first())
            .setColor(accent)
            .setContentIntent(openIntent(context, message.target))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_REMINDER)
        if (content != null) {
            // System header (app, 朝の一句 / 復習到期, time) on top; everything below is ours.
            builder.setSubText(content.header)
                .setStyle(Notification.DecoratedCustomViewStyle())
                .setCustomContentView(content.small)
            content.big?.let { builder.setCustomBigContentView(it) }
        } else {
            builder.setStyle(Notification.BigTextStyle().bigText(message.body))
        }
        manager.notify(NotificationId, builder.build())
        return true
    }

    private class ReminderViews(val header: String, val small: RemoteViews, val big: RemoteViews?, val workSlug: String)

    private const val ContentWidthDp = 295

    /**
     * The reminder's content in the canvas design (「通知 · 超级岛」): a manga panel for line
     * reminders, a stamp + 時間割 card for 復習, a name plate + line when collapsed.
     */
    private fun contentViews(context: Context, store: LocalLabStore, message: ReminderMessage, line: TodayWidgetLine?, saved: Boolean): ReminderViews {
        val art = LabArt.forContext(context)
        val pkg = context.packageName
        if (message.channel == ReminderChannel.Review) {
            val today = LocalDate.now()
            val epochDay = today.toEpochDay()
            val shiori = NotebookRules.dueCount(store.readNotebook(), epochDay)
            val drill = store.readDrillProgress().values.count { it.dueDay <= epochDay }
            val due = shiori + drill
            val fading = store.readDrillPointDue().filterValues { it <= epochDay }.minByOrNull { it.value }?.key
                ?.substringBefore('（')?.trim()
            val lastStudy = store.readStudyLog().filter { (day, d) -> day < today.toString() && d.activity > 0 }.keys.maxOrNull()
            val gap = lastStudy?.let { runCatching { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(it), today).toInt() }.getOrNull() } ?: 0
            val workSlug = line?.workSlug.orEmpty()
            val small = RemoteViews(pkg, R.layout.notif_small).apply {
                setImageViewBitmap(R.id.notif_badge, art.stamp(due, workSlug, 40))
                setTextViewText(R.id.notif_title, fading?.let { "「$it」が薄れてきた" } ?: "復習の時間 · $due 枚")
                setTextViewText(R.id.notif_meta, listOfNotNull(drill.takeIf { it > 0 }?.let { "活用 $it" }, shiori.takeIf { it > 0 }?.let { "栞 $it" }).joinToString(" · "))
            }
            val big = RemoteViews(pkg, R.layout.notif_review_big).apply {
                setImageViewBitmap(R.id.notif_card, art.reviewCard(drill, shiori, fading, gap, workSlug, ContentWidthDp))
                setTextViewText(R.id.notif_go_meta, "约 ${((due * 10 + 59) / 60).coerceAtLeast(1)} 分钟")
                setImageViewBitmap(R.id.notif_ramp, art.ramp(workSlug, ContentWidthDp))
                setOnClickPendingIntent(R.id.notif_go, openIntent(context, ReminderTarget.Review))
            }
            return ReminderViews("復習到期", small, big, workSlug)
        }

        val morning = message.template == "morning"
        val source = (if (morning) morningSource(store) else line?.let { lineSource(it) })?.takeIf { message.body.startsWith("「") }
        if (source == null) {
            val workSlug = line?.workSlug.orEmpty()
            val small = RemoteViews(pkg, R.layout.notif_small).apply {
                setImageViewBitmap(R.id.notif_badge, art.nameplate(markFor(workSlug), workSlug, 40))
                setTextViewText(R.id.notif_title, message.title)
                setTextViewText(R.id.notif_meta, message.body.lineSequence().first())
            }
            return ReminderViews("放課後チャイム", small, null, workSlug)
        }
        val small = RemoteViews(pkg, R.layout.notif_small).apply {
            setImageViewBitmap(R.id.notif_badge, art.nameplate(markFor(source.workSlug), source.workSlug, 40))
            setTextViewText(R.id.notif_title, source.ja)
            setTextViewText(R.id.notif_meta, if (saved) "已挟入栞" else if (morning) source.eyebrow else message.title)
        }
        val big = RemoteViews(pkg, R.layout.notif_line_big).apply {
            setImageViewBitmap(
                R.id.notif_panel,
                art.linePanel(source.ja, source.zh, source.eyebrow, source.seal, source.note, source.workSlug, ContentWidthDp, 170),
            )
            val canPlay = morning && TodayLineAudio.cached(context, source.ja) != null
            val canSave = morning && !saved && source.entry.isNotBlank()
            setViewVisibility(R.id.notif_actions, if (canPlay || canSave) android.view.View.VISIBLE else android.view.View.GONE)
            setViewVisibility(R.id.notif_play, if (canPlay) android.view.View.VISIBLE else android.view.View.GONE)
            setViewVisibility(R.id.notif_save, if (canSave) android.view.View.VISIBLE else android.view.View.GONE)
            if (canPlay) {
                setImageViewBitmap(R.id.notif_play_bars, art.voiceBars(source.workSlug))
                setOnClickPendingIntent(R.id.notif_play, actionIntent(context, ActionPlayLine))
            }
            if (canSave) {
                val label = android.text.SpannableString("挟む · 存进栞").apply {
                    setSpan(android.text.style.TypefaceSpan("serif"), 0, 2, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, 2, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                setTextViewText(R.id.notif_save, label)
                setOnClickPendingIntent(R.id.notif_save, actionIntent(context, ActionSaveLine))
            }
        }
        return ReminderViews(if (morning) "朝の一句" else "放課後チャイム", small, big, source.workSlug)
    }

    private fun markFor(workSlug: String): String = WorkIdentity.representative(workSlug.ifBlank { "re-zero" })?.mark ?: "学"

    private var sealIconCache: Icon? = null

    /** The 学 seal: status bar, island and card header all show this instead of a generic glyph. */
    fun sealIcon(context: Context): Icon =
        sealIconCache ?: Icon.createWithBitmap(LabArt.sealIcon(context.applicationContext)).also { sealIconCache = it }

    private fun actionIntent(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, StudyReminderReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

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
            StudyReminder.ActionSaveLine -> StudyReminder.saveTodayLine(context)
            StudyReminder.ActionPlayLine -> {
                val clip = StudyReminder.morningClip(context) ?: return
                val pending = goAsync()
                TodayLineAudio.play(clip) { pending.finish() }
            }
        }
    }
}
