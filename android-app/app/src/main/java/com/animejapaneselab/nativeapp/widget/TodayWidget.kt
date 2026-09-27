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

    /** 2:1 manga-panel art for notifications (BigPictureStyle), matching the widget. */
    fun lineArt(context: Context, line: TodayWidgetLine): Bitmap =
        TodayWidgetRenderer(context, artScale(context), isDark(context)).render(line, ArtWidthDp, ArtHeightDp, due = 0)

    fun dueArt(context: Context, shiori: Int, drill: Int, point: String?): Bitmap =
        TodayWidgetRenderer(context, artScale(context), isDark(context)).renderDue(shiori, drill, point, ArtWidthDp, ArtHeightDp)

    private const val ArtWidthDp = 360
    private const val ArtHeightDp = 180

    private fun artScale(context: Context) = context.resources.displayMetrics.density.coerceAtMost(2.5f)

    private fun isDark(context: Context) = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

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
        val bitmap = TodayWidgetRenderer(context, scale, dark).render(read(context), widthDp, heightDp, due)
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

/**
 * Paints the widget as one bitmap so it can keep the app's manga-panel look (vertical serif
 * line, screentone, seal) — RemoteViews has no vertical text. Colours mirror AjlTheme tokens.
 */
internal class TodayWidgetRenderer(context: Context, private val scale: Float, dark: Boolean) {
    private val surface = if (dark) 0xFF1C1C1A.toInt() else 0xFFFFFFFF.toInt()
    private val ink = if (dark) 0xFFEDEDE8.toInt() else 0xFF1B1B19.toInt()
    private val ink2 = if (dark) 0xFFA9A8A1.toInt() else 0xFF55544F.toInt()
    private val ink3 = if (dark) 0xFF86857E.toInt() else 0xFF6E6D67.toInt()
    private val dark = dark
    private val mono: Typeface = runCatching { ResourcesCompat.getFont(context, R.font.ibm_plex_mono_regular) }
        .getOrNull() ?: Typeface.MONOSPACE
    private val serifBold: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)

    private fun px(dp: Float) = dp * scale

    private fun accentFor(workSlug: String): Int = when (normalizeWorkSlug(workSlug)) {
        "k-on" -> if (dark) 0xFFE58AA7.toInt() else 0xFFC4466F.toInt()
        "re-zero" -> if (dark) 0xFFA894F2.toInt() else 0xFF6A4FC4.toInt()
        else -> if (dark) 0xFF8C9BF2.toInt() else 0xFF3A4FCB.toInt()
    }

    fun render(line: TodayWidgetLine?, widthDp: Int, heightDp: Int, due: Int): Bitmap {
        val w = px(widthDp.toFloat()).toInt().coerceAtLeast(1)
        val h = px(heightDp.toFloat()).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val accent = accentFor(line?.workSlug.orEmpty())
        val radius = px(18f)
        val inset = px(0.75f)
        val panel = RectF(inset, inset, w - inset, h - inset)

        canvas.drawRoundRect(panel, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surface })

        // Screentone band, bottom-left, tilted like the in-app hero.
        canvas.save()
        canvas.clipRect(panel)
        canvas.rotate(-12f, 0f, h.toFloat())
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; alpha = 77 }
        val step = px(7f)
        var y = h - px(70f)
        while (y < h + px(40f)) {
            var x = -px(30f)
            while (x < w * 0.55f) {
                canvas.drawCircle(x, y, px(1.2f), dot)
                x += step
            }
            y += step
        }
        canvas.restore()

        canvas.drawRoundRect(
            panel,
            radius,
            radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = px(1.5f)
                color = ink
            },
        )

        val pad = px(14f)
        val eyebrow = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = mono
            textSize = px(10.5f)
            color = ink3
            letterSpacing = 0.06f
        }
        val header = listOfNotNull(
            "今日の一句",
            line?.let { WorkIdentity.displayName(it.workSlug).ifBlank { null } },
            line?.episodeLabel?.ifBlank { null },
        ).joinToString(" · ")
        canvas.drawText(header, pad, pad - eyebrow.ascent(), eyebrow)
        // 復習 count under the eyebrow; the overlay view on top of it opens 復習.
        val dueShift = if (due > 0) px(20f) else 0f
        if (due > 0) {
            val duePaint = TextPaint(eyebrow).apply { color = accent; textSize = px(12f) }
            canvas.drawText("復習 $due ›", pad, pad + px(18f) - duePaint.ascent(), duePaint)
        }

        if (line == null) {
            val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = px(14f); color = ink2 }
            canvas.drawText("打开 App，翻开今天的一句", pad, h / 2f, body)
            return bitmap
        }

        // Seal under the eyebrow when there is room.
        if (heightDp >= 150 + (if (due > 0) 20 else 0)) {
            drawSeal(canvas, WorkIdentity.sealText(line.workSlug), pad + px(2f), pad + px(26f) + dueShift, px(34f), accent)
        }

        // Vertical line on the right: shrink the glyphs until the columns fit ~62% of the width.
        val top = pad + px(6f)
        val availH = h - top - pad
        var glyph = min(px(30f), availH / 4.5f)
        var columns: List<String>
        while (true) {
            val perColumn = (availH / (glyph * 1.1f)).toInt().coerceIn(3, 14)
            columns = TodayRules.verticalLayout(line.ja, perColumn).split('\n')
            if (columns.size * glyph * 1.28f <= w * 0.62f || glyph <= px(13f)) break
            glyph *= 0.9f
        }
        val jp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serifBold
            textSize = glyph
            color = ink
        }
        val fm = jp.fontMetrics
        val cell = glyph * 1.1f
        val colW = glyph * 1.28f
        var colRight = w - pad - px(4f)
        columns.forEach { column ->
            val cx = colRight - colW / 2f
            column.forEachIndexed { i, ch ->
                val g = TextRules.verticalGlyph(ch).toString()
                val cy = top + cell * i + cell / 2f
                canvas.drawText(g, cx - jp.measureText(g) / 2f, cy - (fm.ascent + fm.descent) / 2f, jp)
            }
            colRight -= colW
        }
        val textLeftLimit = colRight

        // Translation + attribution, bottom-left, never under the vertical columns.
        val zhWidth = (textLeftLimit - pad - px(10f)).toInt().coerceAtLeast(px(60f).toInt())
        var bottom = h - pad
        if (line.attribution.isNotBlank()) {
            val meta = TextPaint(eyebrow).apply { color = accent }
            canvas.drawText(
                TextUtils.ellipsize(line.attribution, meta, zhWidth.toFloat(), TextUtils.TruncateAt.END).toString(),
                pad,
                bottom - meta.descent(),
                meta,
            )
            bottom -= px(16f)
        }
        if (line.zh.isNotBlank()) {
            val zhPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = px(13f); color = ink2 }
            val maxLines = if (heightDp >= 150) 3 else 2
            val layout = StaticLayout.Builder.obtain(line.zh, 0, line.zh.length, zhPaint, zhWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(px(2f), 1f)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
            canvas.save()
            canvas.translate(pad, bottom - layout.height)
            layout.draw(canvas)
            canvas.restore()
        }
        return bitmap
    }

    /**
     * 復習 card for reminders: the due count set large in serif, what it is made of above it, and
     * the fading 課 running down the right edge — same panel, tone and seal as the line card.
     */
    fun renderDue(shiori: Int, drill: Int, point: String?, widthDp: Int, heightDp: Int): Bitmap {
        val w = px(widthDp.toFloat()).toInt().coerceAtLeast(1)
        val h = px(heightDp.toFloat()).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val accent = accentFor("")
        val radius = px(18f)
        val inset = px(0.75f)
        val panel = RectF(inset, inset, w - inset, h - inset)
        canvas.drawRoundRect(panel, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surface })
        canvas.save()
        canvas.clipRect(panel)
        canvas.rotate(-12f, 0f, h.toFloat())
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; alpha = 77 }
        val step = px(7f)
        var y = h - px(60f)
        while (y < h + px(40f)) {
            var x = -px(30f)
            while (x < w * 0.5f) {
                canvas.drawCircle(x, y, px(1.2f), dot)
                x += step
            }
            y += step
        }
        canvas.restore()
        canvas.drawRoundRect(panel, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = px(1.5f)
            color = ink
        })

        val pad = px(16f)
        val eyebrow = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = px(10.5f); color = ink3; letterSpacing = 0.06f }
        val parts = listOfNotNull(shiori.takeIf { it > 0 }?.let { "栞 $it" }, drill.takeIf { it > 0 }?.let { "活用 $it" })
        canvas.drawText((listOf("復習") + parts).joinToString(" · "), pad, pad - eyebrow.ascent(), eyebrow)

        val big = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serifBold; textSize = px(58f); color = ink }
        val count = (shiori + drill).toString()
        val baseline = h * 0.68f
        canvas.drawText(count, pad, baseline, big)
        val unit = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = px(12f); color = accent }
        canvas.drawText("枚到期", pad + big.measureText(count) + px(8f), baseline, unit)

        // Fading 課 down the right edge.
        val title = point?.takeIf { it.isNotBlank() }?.substringBefore('（')?.substringBefore(' ')?.take(8) ?: "復習"
        val glyph = min(px(24f), (h - pad * 2) / (title.length.coerceAtLeast(3) * 1.1f))
        val jp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serifBold; textSize = glyph; color = ink }
        val fm = jp.fontMetrics
        val cx = w - pad - glyph * 0.7f
        title.forEachIndexed { i, ch ->
            val g = TextRules.verticalGlyph(ch).toString()
            val cy = pad + glyph * 1.1f * i + glyph * 0.55f
            canvas.drawText(g, cx - jp.measureText(g) / 2f, cy - (fm.ascent + fm.descent) / 2f, jp)
        }
        drawSeal(canvas, "復\n習", w - pad - glyph * 1.6f - px(44f), h - pad - px(36f), px(34f), accent)
        return bitmap
    }

    private fun drawSeal(canvas: Canvas, text: String, left: Float, top: Float, size: Float, accent: Int) {
        canvas.save()
        canvas.rotate(-6f, left + size / 2f, top + size / 2f)
        val rect = RectF(left, top, left + size, top + size)
        canvas.drawRoundRect(
            rect,
            px(5f),
            px(5f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = px(1.5f)
                color = accent
            },
        )
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serifBold
            textSize = size * 0.36f
            color = accent
        }
        val glyphs = text.split('\n')
        val fm = paint.fontMetrics
        glyphs.forEachIndexed { i, g ->
            val cy = top + size * (i + 1) / (glyphs.size + 1)
            canvas.drawText(g, left + (size - paint.measureText(g)) / 2f, cy - (fm.ascent + fm.descent) / 2f, paint)
        }
        canvas.restore()
    }
}
