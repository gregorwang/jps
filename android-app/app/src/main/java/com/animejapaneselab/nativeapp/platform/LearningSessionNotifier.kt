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
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import com.animejapaneselab.nativeapp.MainActivity
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.widget.LabArt
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
        val (rawMode, topic) = splitTitle(status.title)
        val mode = japaneseMode(rawMode)
        val unit = japaneseUnit(status.unit)
        val chip = "$unit ${status.position}/${status.total}"
        val header = listOfNotNull(
            WorkIdentity.displayName(status.workSlug).ifBlank { null },
            status.episode.takeIf { it > 0 }?.let(TextRules::episodeLabel),
        ).joinToString(" ").ifBlank { status.subtitle }
        val startedAt = preferences.getLong(StartedAtKey, 0L).takeIf { it > 0 } ?: System.currentTimeMillis()
        val builder = Notification.Builder(appContext, ChannelId)
            .setSmallIcon(StudyReminder.sealIcon(appContext))
            .setContentTitle(topic ?: mode)
            .setContentText(listOfNotNull(mode.takeIf { topic != null }, "第 ${status.position} / ${status.total} $unit").joinToString(" · "))
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
        val face = CharacterFaces.face(appContext, status.workSlug, status.episode)
        face?.let { builder.setLargeIcon(it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            // One segment per question (a 場面 strip): answered ones in the work colour, the rest soft;
            // the character's face walks along as the progress head.
            val soft = WorkThemes.of(WorkIdentity.hue(status.workSlug), dark = false).soft.toArgb()
            val perQuestion = status.total in 2..MaxSegments
            val mark = WorkIdentity.representative(status.workSlug.ifBlank { "re-zero" }, status.episode.coerceAtLeast(1))?.mark ?: "学"
            val tracker = LabArt.forContext(appContext).faceBadge(face, mark, status.workSlug, 24)
            val style = Notification.ProgressStyle()
                .setStyledByProgress(false)
                .setProgressTrackerIcon(Icon.createWithBitmap(tracker))
                .setProgress(status.completed)
            if (perQuestion) {
                repeat(status.total) { i ->
                    style.addProgressSegment(Notification.ProgressStyle.Segment(1).setColor(if (i < status.completed) accent else soft))
                }
            } else {
                style.addProgressSegment(Notification.ProgressStyle.Segment(status.completed.coerceAtLeast(1)).setColor(accent))
                if (status.total > status.completed) {
                    style.addProgressSegment(Notification.ProgressStyle.Segment(status.total - status.completed).setColor(soft))
                }
            }
            builder.setStyle(style)
        } else {
            builder.setProgress(status.total, status.completed, false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            // Live Update request by extras too: HyperOS 3 is Android 16.0, where the builder
            // setters below don't exist yet, but the island reads these keys.
            builder.extras.putBoolean(ExtraRequestPromotedOngoing, true)
            builder.extras.putCharSequence(ExtraShortCriticalText, chip)
            if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
                builder
                    .setRequestPromotedOngoing(true)
                    .setShortCriticalText(chip)
            }
        }
        if (hyperOsFocusAllowed) addHyperOsIsland(builder, status, topic ?: mode, mode, face)
        return builder.build()
    }

    /** Xiaomi's own island protocol; only honoured for apps Xiaomi has granted 焦点通知. */
    private val hyperOsFocusAllowed: Boolean by lazy {
        runCatching {
            android.provider.Settings.System.getInt(appContext.contentResolver, "notification_focus_protocol", 0) >= 3 &&
                DeviceCapabilityReader.queryHyperOsFocusPermission(appContext)
        }.getOrDefault(false)
    }

    private fun addHyperOsIsland(builder: Notification.Builder, status: LearningSessionStatus, title: String, mode: String, face: Bitmap?) {
        val pic = "miui.focus.pic_face"
        val island = org.json.JSONObject().put(
            "param_v2",
            org.json.JSONObject()
                .put("protocol", 1)
                .put("business", "study")
                .put("updatable", true)
                .put("ticker", "${status.chipText} · $title")
                .put("aodTitle", status.chipText)
                .put(
                    "param_island",
                    org.json.JSONObject()
                        .put("islandProperty", 1)
                        .put(
                            "bigIslandArea",
                            org.json.JSONObject().put(
                                "imageTextInfoLeft",
                                org.json.JSONObject()
                                    .put("type", 1)
                                    .put("picInfo", org.json.JSONObject().put("type", 1).put("pic", pic))
                                    .put("textInfo", org.json.JSONObject().put("title", status.chipText).put("content", mode)),
                            ),
                        )
                        .put("smallIslandArea", org.json.JSONObject().put("picInfo", org.json.JSONObject().put("type", 1).put("pic", pic))),
                )
                .put("baseInfo", org.json.JSONObject().put("title", title).put("content", "第 ${status.position} / ${status.total} ${status.unit}").put("type", 2)),
        )
        builder.extras.putString("miui.focus.param", island.toString())
        if (face != null) {
            builder.extras.putBundle("miui.focus.pics", android.os.Bundle().apply { putParcelable(pic, Icon.createWithBitmap(face)) })
        }
    }

    /** Session labels in the app's school vocabulary (the island and card speak Japanese). */
    private fun japaneseMode(mode: String): String = when {
        "复习" in mode || "复盘" in mode -> "復習"
        "单点" in mode -> "練習"
        "读空气" in mode -> "空気を読む"
        "跟读" in mode -> "シャドーイング"
        "词汇" in mode -> "語彙"
        "语法" in mode -> "文法"
        "综合" in mode -> "総合"
        else -> mode
    }

    private fun japaneseUnit(unit: String): String = when (unit) {
        "问" -> "問"
        "页" -> "頁"
        else -> unit
    }

    /** 「单点训练 · やばい…」 → (单点训练, やばい…); a title without a topic stays whole. */
    private fun splitTitle(title: String): Pair<String, String?> {
        val parts = title.split(" · ", limit = 2)
        return if (parts.size == 2 && parts[1].isNotBlank()) parts[0] to parts[1] else title to null
    }

    private companion object {
        const val MaxSegments = 24
        const val ExtraRequestPromotedOngoing = "android.requestPromotedOngoing"
        const val ExtraShortCriticalText = "android.shortCriticalText"
        const val StartedAtKey = "session-started-at"
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
