package com.animejapaneselab.nativeapp.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.screens.today.TodayRules
import com.animejapaneselab.nativeapp.ui.theme.normalizeWorkSlug
import java.time.LocalDate
import kotlin.math.min

/**
 * Paints the app's surfaces outside the app — home-screen widget, reminder notifications, the
 * live card's icons — in the v3 language: 1.5px ink frame, vertical serif line, rotated
 * screentone, work-colour seal. RemoteViews can't do vertical text or screentone, so these are
 * bitmaps. Colours mirror AjlTheme tokens (see design canvas「通知 · 超级岛」).
 */
internal class LabArt(context: Context, private val scale: Float, private val dark: Boolean) {
    private val surface = if (dark) 0xFF1C1C1A.toInt() else 0xFFFFFFFF.toInt()
    private val ink = if (dark) 0xFFEDEDE8.toInt() else 0xFF1B1B19.toInt()
    private val ink2 = if (dark) 0xFFA9A8A1.toInt() else 0xFF55544F.toInt()
    private val ink3 = if (dark) 0xFF86857E.toInt() else 0xFF6E6D67.toInt()
    private val line = if (dark) 0xFF34342F.toInt() else 0xFFE4E3DD.toInt()
    private val mono: Typeface = runCatching { ResourcesCompat.getFont(context, R.font.ibm_plex_mono_regular) }
        .getOrNull() ?: Typeface.MONOSPACE
    private val serifBold: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)

    private fun px(dp: Float) = dp * scale

    fun accentFor(workSlug: String): Int = when (normalizeWorkSlug(workSlug)) {
        "k-on" -> if (dark) 0xFFE58AA7.toInt() else 0xFFC4466F.toInt()
        "re-zero" -> if (dark) 0xFFA894F2.toInt() else 0xFF6A4FC4.toInt()
        else -> if (dark) 0xFF8C9BF2.toInt() else 0xFF3A4FCB.toInt()
    }

    private fun softFor(workSlug: String): Int = when (normalizeWorkSlug(workSlug)) {
        "k-on" -> if (dark) 0xFF3A2430.toInt() else 0xFFF8E6EC.toInt()
        "re-zero" -> if (dark) 0xFF2C2640.toInt() else 0xFFEEEAFA.toInt()
        else -> if (dark) 0xFF252A40.toInt() else 0xFFECEEFB.toInt()
    }

    private fun canvasOf(widthDp: Int, heightDp: Int): Pair<Bitmap, Canvas> {
        val bitmap = Bitmap.createBitmap(px(widthDp.toFloat()).toInt().coerceAtLeast(1), px(heightDp.toFloat()).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        return bitmap to Canvas(bitmap)
    }

    /** Frame + screentone band in the bottom-left corner. */
    private fun panel(canvas: Canvas, w: Float, h: Float, radiusDp: Float, accent: Int) {
        val inset = px(0.75f)
        val rect = RectF(inset, inset, w - inset, h - inset)
        val radius = px(radiusDp)
        canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surface })
        canvas.save()
        canvas.clipRect(rect)
        canvas.rotate(-12f, 0f, h)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; alpha = if (dark) 84 else 70 }
        val step = px(7f)
        var y = h - px(62f)
        while (y < h + px(40f)) {
            var x = -px(30f)
            while (x < w * 0.52f) {
                canvas.drawCircle(x, y, px(1.2f), dot)
                x += step
            }
            y += step
        }
        canvas.restore()
        canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = px(1.5f)
            color = ink
        })
    }

    private fun eyebrow() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = mono
        textSize = px(10.5f)
        color = ink3
        letterSpacing = 0.06f
    }

    /** Right-aligned vertical line; glyphs shrink until the columns fit [maxWidth]. Returns the left edge used. */
    private fun verticalLine(canvas: Canvas, text: String, right: Float, top: Float, bottom: Float, maxWidth: Float, maxGlyphDp: Float): Float {
        val availH = bottom - top
        var glyph = min(px(maxGlyphDp), availH / 4.5f)
        var columns: List<String>
        while (true) {
            val perColumn = (availH / (glyph * 1.1f)).toInt().coerceIn(3, 14)
            columns = TodayRules.verticalLayout(text, perColumn).split('\n')
            if (columns.size * glyph * 1.28f <= maxWidth || glyph <= px(12f)) break
            glyph *= 0.9f
        }
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serifBold; textSize = glyph; color = ink }
        val fm = paint.fontMetrics
        val cell = glyph * 1.1f
        val colW = glyph * 1.28f
        var colRight = right
        columns.forEach { column ->
            val cx = colRight - colW / 2f
            column.forEachIndexed { i, ch ->
                val g = TextRules.verticalGlyph(ch).toString()
                val cy = top + cell * i + cell / 2f
                canvas.drawText(g, cx - paint.measureText(g) / 2f, cy - (fm.ascent + fm.descent) / 2f, paint)
            }
            colRight -= colW
        }
        return colRight
    }

    private fun seal(canvas: Canvas, text: String, left: Float, top: Float, size: Float, color: Int) {
        canvas.save()
        canvas.rotate(-6f, left + size / 2f, top + size / 2f)
        canvas.drawRoundRect(RectF(left, top, left + size, top + size), px(5f), px(5f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = px(2f)
            this.color = color
        })
        val glyphs = text.split('\n')
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serifBold
            textSize = size * (if (glyphs.size > 1) 0.34f else 0.5f)
            this.color = color
        }
        val fm = paint.fontMetrics
        glyphs.forEachIndexed { i, g ->
            val cy = top + size * (i + 1) / (glyphs.size + 1)
            canvas.drawText(g, left + (size - paint.measureText(g)) / 2f, cy - (fm.ascent + fm.descent) / 2f, paint)
        }
        canvas.restore()
    }

    /** Bottom-left block: an accent meta line under up to [maxLines] lines of translation. */
    private fun footnote(canvas: Canvas, zh: String, meta: String, left: Float, bottom: Float, width: Int, maxLines: Int, accent: Int) {
        var y = bottom
        if (meta.isNotBlank()) {
            val metaPaint = TextPaint(eyebrow()).apply { color = accent }
            canvas.drawText(TextUtils.ellipsize(meta, metaPaint, width.toFloat(), TextUtils.TruncateAt.END).toString(), left, y - metaPaint.descent(), metaPaint)
            y -= px(16f)
        }
        if (zh.isNotBlank()) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = px(12.5f); color = ink2 }
            val layout = StaticLayout.Builder.obtain(zh, 0, zh.length, paint, width.coerceAtLeast(1))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(px(2f), 1f)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
            canvas.save()
            canvas.translate(left, y - layout.height)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    // ------------------------------------------------------------------ widget

    /** 今日の一句 widget; narrow sizes (2×2) keep only the line, the seal and the date. */
    fun widget(line: TodayWidgetLine?, widthDp: Int, heightDp: Int, due: Int): Bitmap {
        val (bitmap, canvas) = canvasOf(widthDp, heightDp)
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val accent = accentFor(line?.workSlug.orEmpty())
        panel(canvas, w, h, 18f, accent)
        val pad = px(14f)
        val compact = widthDp < 220
        val date = dateLabel(line?.date)
        val eye = eyebrow()
        canvas.drawText(if (compact) date else listOf("今日の一句", date).filter { it.isNotBlank() }.joinToString(" · "), pad, pad - eye.ascent(), eye)
        if (line == null) {
            val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = px(14f); color = ink2 }
            canvas.drawText("打开 App，翻开今天的一句", pad, h / 2f, body)
            return bitmap
        }
        val sealText = WorkIdentity.sealText(line.workSlug)
        if (compact) {
            seal(canvas, sealText, pad, h - pad - px(30f), px(30f), accent)
            verticalLine(canvas, line.ja, w - pad, pad, h - pad, w - pad * 2 - px(40f), 22f)
            return bitmap
        }
        seal(canvas, sealText, pad + px(1f), pad + px(22f), px(34f), accent)
        if (due > 0) dueChip(canvas, due, pad + px(44f), pad + px(26f), accent, softFor(line.workSlug))
        val left = verticalLine(canvas, line.ja, w - pad - px(2f), pad + px(2f), h - pad, w * 0.6f, 27f)
        val textWidth = (left - pad - px(10f)).toInt().coerceAtLeast(px(60f).toInt())
        footnote(canvas, line.zh, metaFor(line), pad, h - pad, textWidth, if (heightDp >= 150) 3 else 2, accent)
        return bitmap
    }

    private fun dueChip(canvas: Canvas, due: Int, left: Float, top: Float, accent: Int, soft: Int) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = px(11f); color = accent }
        val text = "復習 $due ›"
        val rect = RectF(left, top, left + paint.measureText(text) + px(16f), top + px(24f))
        canvas.drawRoundRect(rect, px(4f), px(4f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = soft })
        canvas.drawRoundRect(rect, px(4f), px(4f), Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = px(1f); color = accent })
        val fm = paint.fontMetrics
        canvas.drawText(text, left + px(8f), rect.centerY() - (fm.ascent + fm.descent) / 2f, paint)
    }

    /** 「スバル · Re:ゼロ 第一話」: the speaker when known, then work and episode (no 第 N 行 filler). */
    private fun metaFor(line: TodayWidgetLine): String {
        val first = line.attribution.substringBefore(" · ").trim()
        val speaker = first.takeUnless { it.isBlank() || it.any(Char::isDigit) }
        val work = listOf(WorkIdentity.displayName(line.workSlug), line.episodeLabel).filter { it.isNotBlank() }.joinToString(" ")
        return listOfNotNull(speaker, work.ifBlank { null }).joinToString(" · ")
    }

    private fun dateLabel(iso: String?): String = runCatching {
        val day = LocalDate.parse(iso ?: return "")
        "${day.monthValue}.${day.dayOfMonth} ${"月火水木金土日"[day.dayOfWeek.value - 1]}"
    }.getOrDefault("")

    // ------------------------------------------------------------------ notifications

    /** 朝の一句 panel for the expanded reminder (manga frame, 4px corners). */
    fun linePanel(ja: String, zh: String, eyebrowText: String, sealText: String, formula: String, workSlug: String, widthDp: Int, heightDp: Int): Bitmap {
        val (bitmap, canvas) = canvasOf(widthDp, heightDp)
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        val accent = accentFor(workSlug)
        panel(canvas, w, h, 4f, accent)
        val pad = px(12f)
        val eye = eyebrow().apply { textSize = px(10f) }
        canvas.drawText(eyebrowText, pad, pad - eye.ascent(), eye)
        seal(canvas, sealText, pad + px(1f), pad + px(20f), px(34f), accent)
        val left = verticalLine(canvas, ja, w - pad - px(2f), pad, h - pad, w * 0.42f, 24f)
        val textWidth = (left - pad - px(8f)).toInt().coerceAtLeast(px(60f).toInt())
        footnote(canvas, zh, formula, pad, h - pad, textWidth, 2, accent)
        return bitmap
    }

    /** 復習 card: stamp with the count, what is fading, then 時間割 rows per deck. Transparent ground. */
    fun reviewCard(drill: Int, shiori: Int, point: String?, gapDays: Int, workSlug: String, widthDp: Int): Bitmap {
        val rows = listOfNotNull(
            drill.takeIf { it > 0 }?.let { Triple("活用", point?.let { p -> "活用 · $p" } ?: "活用", "$it 句") },
            shiori.takeIf { it > 0 }?.let { Triple("收藏", "收藏 · 台词和生词", "$it 张") },
        )
        val heightDp = 60 + 8 + rows.size * 36
        val (bitmap, canvas) = canvasOf(widthDp, heightDp)
        val w = bitmap.width.toFloat()
        val accent = accentFor(workSlug)
        stampAt(canvas, (drill + shiori).toString(), "枚", px(2f), px(4f), px(50f), accent)
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serifBold; textSize = px(16f); color = ink }
        val sub = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = px(12f); color = ink2 }
        val textLeft = px(66f)
        val titleText = point?.let { "「$it」が薄れてきた" } ?: "復習の時間"
        canvas.drawText(TextUtils.ellipsize(titleText, title, w - textLeft, TextUtils.TruncateAt.END).toString(), textLeft, px(26f), title)
        canvas.drawText(if (gapDays >= 2) "$gapDays 天没复习" else "今天到期 ${drill + shiori} 张", textLeft, px(46f), sub)
        var y = px(68f)
        canvas.drawRect(0f, y - px(1.5f), w, y, Paint().apply { color = ink })
        val period = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serifBold; textSize = px(13f) }
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = px(13f); color = ink }
        val count = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = px(12f) }
        rows.forEachIndexed { i, (_, label, amount) ->
            val mid = y + px(18f)
            period.color = if (i == 0) accent else ink
            count.color = if (i == 0) accent else ink3
            val fm = body.fontMetrics
            val base = mid - (fm.ascent + fm.descent) / 2f
            canvas.drawText(if (i == 0) "一限" else "二限", 0f, base, period)
            val amountW = count.measureText(amount)
            canvas.drawText(TextUtils.ellipsize(label, body, w - px(46f) - amountW - px(12f), TextUtils.TruncateAt.END).toString(), px(46f), base, body)
            canvas.drawText(amount, w - amountW, base, count)
            y += px(36f)
            canvas.drawRect(0f, y - px(1f), w, y, Paint().apply { color = line })
        }
        return bitmap
    }

    private fun stampAt(canvas: Canvas, big: String, unit: String, left: Float, top: Float, size: Float, color: Int) {
        canvas.save()
        canvas.rotate(-6f, left + size / 2f, top + size / 2f)
        canvas.drawRoundRect(RectF(left, top, left + size, top + size), px(6f), px(6f), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = px(2f)
            this.color = color
        })
        val bigPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serifBold; textSize = size * (if (big.length > 2) 0.34f else 0.42f); this.color = color }
        val unitPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = size * 0.18f; this.color = color }
        canvas.drawText(big, left + (size - bigPaint.measureText(big)) / 2f, top + size * 0.56f, bigPaint)
        canvas.drawText(unit, left + (size - unitPaint.measureText(unit)) / 2f, top + size * 0.82f, unitPaint)
        canvas.restore()
    }

    /** Count stamp on its own (collapsed 復習 reminder). */
    fun stamp(count: Int, workSlug: String, sizeDp: Int): Bitmap {
        val (bitmap, canvas) = canvasOf(sizeDp, sizeDp)
        val inset = px(4f)
        stampAt(canvas, count.toString(), "枚", inset, inset, bitmap.width - inset * 2, accentFor(workSlug))
        return bitmap
    }

    /** Kanji name plate: soft circle, accent ring, one serif glyph. */
    fun nameplate(mark: String, workSlug: String, sizeDp: Int): Bitmap {
        val (bitmap, canvas) = canvasOf(sizeDp, sizeDp)
        val r = bitmap.width / 2f
        val accent = accentFor(workSlug)
        canvas.drawCircle(r, r, r - px(1f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = softFor(workSlug) })
        canvas.drawCircle(r, r, r - px(1f), Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = px(1.5f); color = accent })
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = serifBold; textSize = r * 0.9f; color = accent }
        val glyph = mark.take(1)
        val fm = paint.fontMetrics
        canvas.drawText(glyph, r - paint.measureText(glyph) / 2f, r - (fm.ascent + fm.descent) / 2f, paint)
        return bitmap
    }

    /** Face cropped to a circle with an accent ring — the live card's progress head. */
    fun faceBadge(face: Bitmap?, mark: String, workSlug: String, sizeDp: Int): Bitmap {
        if (face == null) return nameplate(mark, workSlug, sizeDp)
        val (bitmap, canvas) = canvasOf(sizeDp, sizeDp)
        val size = bitmap.width.toFloat()
        val r = size / 2f
        val shader = BitmapShader(face, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply { setScale(size / face.width, size / face.height) })
        }
        canvas.drawCircle(r, r, r - px(1f), Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        canvas.drawCircle(r, r, r - px(1f), Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = px(1.5f); color = accentFor(workSlug) })
        return bitmap
    }

    /** Four voice bars for the 原声 button. */
    fun voiceBars(workSlug: String): Bitmap {
        val (bitmap, canvas) = canvasOf(12, 14)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentFor(workSlug) }
        listOf(5f, 11f, 8f, 14f).forEachIndexed { i, hDp ->
            val x = px(i * 3.3f)
            canvas.drawRect(x, bitmap.height - px(hDp), x + px(2f), bitmap.height.toFloat(), paint)
        }
        return bitmap
    }

    /** Thin accent ramp under the 復習する button. */
    fun ramp(workSlug: String, widthDp: Int): Bitmap {
        val (bitmap, canvas) = canvasOf(widthDp, 3)
        val accent = accentFor(workSlug)
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, bitmap.width.toFloat(), 0f, softFor(workSlug), accent, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        return bitmap
    }

    companion object {
        fun forContext(context: Context, maxScale: Float = 3f): LabArt =
            LabArt(context, context.resources.displayMetrics.density.coerceAtMost(maxScale), isDark(context))

        fun isDark(context: Context) = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        /** White 学 seal on transparent, for the status bar / island small icon (alpha only). */
        fun sealIcon(context: Context): Bitmap {
            val size = (24 * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val s = size.toFloat()
            val box = s * 0.74f
            val left = (s - box) / 2f
            canvas.save()
            canvas.rotate(-6f, s / 2f, s / 2f)
            canvas.drawRoundRect(RectF(left, left, left + box, left + box), s * 0.1f, s * 0.1f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = s * 0.08f
                color = 0xFFFFFFFF.toInt()
            })
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textSize = box * 0.62f
                color = 0xFFFFFFFF.toInt()
            }
            val fm = paint.fontMetrics
            canvas.drawText("学", s / 2f - paint.measureText("学") / 2f, s / 2f - (fm.ascent + fm.descent) / 2f, paint)
            canvas.restore()
            return bitmap
        }
    }
}
