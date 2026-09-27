package com.animejapaneselab.nativeapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import com.animejapaneselab.nativeapp.MainActivity
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.NotebookEntry
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.platform.ReminderTarget
import com.animejapaneselab.nativeapp.platform.StudyReminder
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.screens.today.TodayLine
import com.animejapaneselab.nativeapp.ui.screens.today.TodayRules
import com.animejapaneselab.nativeapp.ui.theme.normalizeWorkSlug
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.min

/** What the home-screen 今日の一句 shows: the last line the 今日 tab picked. */
data class TodayWidgetLine(
    val ja: String,
    val zh: String,
    val attribution: String,
    val workSlug: String,
    val episodeLabel: String,
    val date: String,
    /** Source clip for the line, blank when only TTS exists. */
    val audioUrl: String = "",
    /** The line as a 栞 entry (NotebookRules encoding), for the notification's 挟む action. */
    val notebookEntry: String = "",
) {
    fun encode(): String = JSONObject()
        .put("ja", ja).put("zh", zh).put("at", attribution)
        .put("w", workSlug).put("ep", episodeLabel).put("d", date)
        .put("au", audioUrl).put("nb", notebookEntry)
        .toString()

    companion object {
        /**
         * Stored as a queue of upcoming days (JSON array) so the widget and 朝の一句 move on at
         * midnight without the app; picks [today]'s entry, else the latest earlier one. A single
         * object (pre-0.8.2) still decodes.
         */
        fun decode(raw: String?, today: LocalDate = LocalDate.now()): TodayWidgetLine? {
            val text = raw?.trim() ?: return null
            if (!text.startsWith("[")) return decodeOne(text)
            val lines = runCatching {
                val array = org.json.JSONArray(text)
                (0 until array.length()).mapNotNull { decodeOne(array.optJSONObject(it)?.toString()) }
            }.getOrDefault(emptyList())
            val key = today.toString()
            return lines.firstOrNull { it.date == key }
                ?: lines.filter { it.date < key }.maxByOrNull { it.date }
                ?: lines.firstOrNull()
        }

        fun encodeQueue(lines: List<TodayWidgetLine>): String =
            org.json.JSONArray().apply { lines.forEach { put(JSONObject(it.encode())) } }.toString()

        private fun decodeOne(raw: String?): TodayWidgetLine? = runCatching {
            val o = JSONObject(raw ?: return null)
            TodayWidgetLine(
                ja = o.optString("ja"),
                zh = o.optString("zh"),
                attribution = o.optString("at"),
                workSlug = o.optString("w"),
                episodeLabel = o.optString("ep"),
                date = o.optString("d"),
                audioUrl = o.optString("au"),
                notebookEntry = o.optString("nb"),
            ).takeIf { it.ja.isNotBlank() }
        }.getOrNull()
    }
}

object TodayWidget {
    /** Called by the 今日 tab whenever its line changes; repaints any placed widgets. */
    /** One upcoming day's line for [publish]. */
    fun queueEntry(line: TodayLine, workSlug: String, episodeLabel: String, day: LocalDate, audioUrl: String, entry: NotebookEntry?) =
        TodayWidgetLine(
            ja = line.ja,
            zh = line.zh,
            attribution = line.attribution.orEmpty(),
            workSlug = workSlug,
            episodeLabel = episodeLabel,
            date = day.toString(),
            audioUrl = audioUrl,
            notebookEntry = entry?.let { NotebookRules.encode(listOf(it)) }.orEmpty(),
        )

    /** Stores the next days' lines (today first) and redraws the widget if anything changed. */
    fun publish(context: Context, queue: List<TodayWidgetLine>) {
        if (queue.isEmpty()) return
        val store = LocalLabStore(context.applicationContext)
        val encoded = TodayWidgetLine.encodeQueue(queue)
        if (store.readTodayWidgetLine() == encoded) return
        store.writeTodayWidgetLine(encoded)
        refreshAll(context)
    }

    private var renderedDue = -1

    /** Re-renders only when the 復習 count on the widget is stale (cheap to call on app stop). */
    fun refreshIfDueChanged(context: Context) {
        val due = StudyReminder.dueCount(context)
        if (due != renderedDue) refreshAll(context)
    }

    fun read(context: Context): TodayWidgetLine? =
        TodayWidgetLine.decode(LocalLabStore(context.applicationContext).readTodayWidgetLine())

    /** Asks the launcher to pin the widget; false when the launcher does not support it. */
    fun requestPin(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return runCatching {
            manager.requestPinAppWidget(ComponentName(context, TodayWidgetProvider::class.java), null, null)
        }.getOrDefault(false)
    }

    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = runCatching {
            manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
        }.getOrDefault(IntArray(0))
        ids.forEach { update(context, manager, it) }
    }

    internal fun update(context: Context, manager: AppWidgetManager, id: Int) {
        val options = manager.getAppWidgetOptions(id)
        val density = context.resources.displayMetrics.density
        val widthDp = options.dp(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val heightDp = options.dp(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180)
        var scale = density
        // Keep the bitmap well under the RemoteViews memory cap.
        while (widthDp * scale * heightDp * scale > 1_100_000f && scale > 1f) scale *= 0.85f
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val due = StudyReminder.dueCount(context)
        renderedDue = due
        val bitmap = LabArt(context, scale, dark).widget(read(context), widthDp, heightDp, due)
        val review = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java)
                .putExtra(StudyReminder.ExtraOpen, ReminderTarget.Review.key)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val views = RemoteViews(context.packageName, R.layout.today_widget).apply {
            setImageViewBitmap(R.id.today_widget_image, bitmap)
            setContentDescription(R.id.today_widget_image, read(context)?.let { "今日の一句 ${it.ja} ${it.zh}" } ?: "今日の一句")
            setOnClickPendingIntent(R.id.today_widget_image, open)
            setViewVisibility(R.id.today_widget_review, if (due > 0) android.view.View.VISIBLE else android.view.View.GONE)
            setOnClickPendingIntent(R.id.today_widget_review, review)
            setContentDescription(R.id.today_widget_review, "復習 $due")
        }
        manager.updateAppWidget(id, views)
    }

    private fun Bundle?.dp(key: String, fallback: Int): Int =
        this?.getInt(key, 0)?.takeIf { it > 40 } ?: fallback
}

class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { TodayWidget.update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        TodayWidget.update(context, manager, id)
    }
}
