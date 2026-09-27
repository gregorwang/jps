package com.animejapaneselab.nativeapp.ui.screens.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.ReviewState
import com.animejapaneselab.nativeapp.data.StudyDay
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Numbers for one day or one week; [accuracy] only counts answers logged on this phone. */
data class StudyStats(
    val finished: Int = 0,
    val answers: Int = 0,
    val studied: Int = 0,
    val accuracy: Int? = null,
    val seconds: Int = 0,
)

/**
 * One square. [level] 1–3 = sessions finished that day (3 = three or more); a day with answers but
 * nothing finished stays unlit and only gets a dot ([touched]).
 */
data class StudyHeatCell(val date: LocalDate, val level: Int, val touched: Boolean, val stats: StudyStats)

data class StudyHeatmapModel(
    /** 84 cells, column-major (week by week, Mon→Sun); null = a future day. */
    val cells: List<StudyHeatCell?>,
    val gridStart: LocalDate,
    val todayIndex: Int,
    val week: StudyStats,
    /** Lifetime study time. */
    val totalSeconds: Long,
    val mastered: Int,
)

object StudyHeatmapRules {
    const val Weeks = 12

    fun build(log: Map<String, StudyDay>, progress: List<ProgressItem>, today: LocalDate, totalSeconds: Long = 0): StudyHeatmapModel {
        // Days before the local log existed still show up (as a dot), from each item's last review.
        val backfill = progress.mapNotNull { localDate(it.lastReviewedAt) }.groupingBy { it.toString() }.eachCount()
        fun statsOf(days: List<LocalDate>): StudyStats {
            val logged = days.mapNotNull { log[it.toString()] }
            val answered = logged.sumOf { it.answers }
            return StudyStats(
                finished = logged.sumOf { it.finished },
                answers = days.sumOf { day -> maxOf(log[day.toString()]?.answers ?: 0, backfill[day.toString()] ?: 0) },
                studied = logged.sumOf { it.studied },
                accuracy = if (answered > 0) logged.sumOf { it.correct } * 100 / answered else null,
                seconds = logged.sumOf { it.seconds },
            )
        }

        val weekStart = today.with(DayOfWeek.MONDAY)
        val gridStart = weekStart.minusWeeks((Weeks - 1).toLong())
        val cells = (0 until Weeks * 7).map { i ->
            val day = gridStart.plusDays(i.toLong())
            if (day.isAfter(today)) return@map null
            val stats = statsOf(listOf(day))
            val level = level(stats.finished)
            StudyHeatCell(day, level, touched = level == 0 && (stats.answers > 0 || stats.studied > 0), stats = stats)
        }
        return StudyHeatmapModel(
            cells = cells,
            gridStart = gridStart,
            todayIndex = (Weeks - 1) * 7 + (today.dayOfWeek.value - 1),
            week = statsOf((0..6).map { weekStart.plusDays(it.toLong()) }.filter { !it.isAfter(today) }),
            totalSeconds = maxOf(totalSeconds, log.values.sumOf { it.seconds.toLong() }),
            mastered = progress.count { it.state == ReviewState.Known || it.state == ReviewState.Good },
        )
    }

    fun level(finished: Int): Int = finished.coerceIn(0, 3)

    fun duration(seconds: Long): String {
        val minutes = seconds / 60
        return when {
            minutes <= 0 -> "0m"
            minutes < 60 -> "${minutes}m"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    private val Weekdays = listOf("月", "火", "水", "木", "金", "土", "日")

    fun dayLabel(date: LocalDate): String = "${date.monthValue}.${date.dayOfMonth} ${Weekdays[date.dayOfWeek.value - 1]}"

    /** Column → 「9月」 on each column whose week ends in a new month. */
    fun monthLabels(gridStart: LocalDate): Map<Int, String> {
        var last = -1
        return (0 until Weeks).mapNotNull { col ->
            val month = gridStart.plusWeeks(col.toLong()).plusDays(6).monthValue
            if (month == last) null else (col to "${month}月").also { last = month }
        }.toMap()
    }

    private fun localDate(iso: String): LocalDate? {
        if (iso.isBlank()) return null
        return runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate() }
            .recoverCatching { LocalDate.parse(iso.take(10)) }
            .getOrNull()
    }
}

/**
 * 最近 12 週: one square a day, lit only by a finished session (deeper for 2, 3+), a dot for a day
 * touched but not finished. Tap a square for that day's numbers; tap it again for the week.
 */
@Composable
fun StudyHeatmapSection(model: StudyHeatmapModel, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val shades = listOf(
        colors.sunken,
        lerp(colors.sunken, work.accent, 0.32f),
        lerp(colors.sunken, work.accent, 0.62f),
        work.accent,
    )
    val dot = lerp(colors.sunken, work.accent, 0.62f)
    val cell = 13.dp
    val gap = 3.dp
    val gridWidth = cell * StudyHeatmapRules.Weeks + gap * (StudyHeatmapRules.Weeks - 1)
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val picked = selected?.let { model.cells.getOrNull(it) }
    val months = remember(model.gridStart) { StudyHeatmapRules.monthLabels(model.gridStart) }
    val labelStyle = AjlTheme.type.meta.copy(fontSize = 9.sp, lineHeight = 11.sp)

    Column(modifier.fillMaxWidth()) {
        SectionHeading(
            title = "最近 12 週",
            meta = "累计 ${StudyHeatmapRules.duration(model.totalSeconds)} · 已掌握 ${model.mastered}",
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Top) {
            // 月 水 金 日 beside the rows.
            Column(Modifier.padding(top = 14.dp).width(14.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
                listOf("月", "", "水", "", "金", "", "日").forEach { label ->
                    Box(Modifier.height(cell), contentAlignment = Alignment.CenterStart) {
                        if (label.isNotEmpty()) Text(label, style = labelStyle, color = colors.ink3)
                    }
                }
            }
            Column {
                Box(Modifier.width(gridWidth).height(14.dp)) {
                    months.forEach { (col, label) ->
                        Text(label, style = labelStyle, color = colors.ink3, modifier = Modifier.offset(x = (cell + gap) * col))
                    }
                }
                Canvas(
                    Modifier
                        .size(width = gridWidth, height = cell * 7 + gap * 6)
                        .pointerInput(model.cells) {
                            detectTapGestures { pos ->
                                val step = (cell + gap).toPx()
                                val col = (pos.x / step).toInt()
                                val row = (pos.y / step).toInt()
                                if (col !in 0 until StudyHeatmapRules.Weeks || row !in 0..6) return@detectTapGestures
                                val index = col * 7 + row
                                if (model.cells.getOrNull(index) == null) return@detectTapGestures
                                selected = if (selected == index) null else index
                            }
                        }
                        .semantics { contentDescription = "最近 12 周学习记录，本周学完 ${model.week.finished} 次" },
                ) {
                    val c = cell.toPx()
                    val step = c + gap.toPx()
                    val radius = CornerRadius(3.dp.toPx())
                    model.cells.forEachIndexed { i, day ->
                        if (day == null) return@forEachIndexed
                        val topLeft = Offset((i / 7) * step, (i % 7) * step)
                        drawRoundRect(shades[day.level], topLeft, Size(c, c), radius)
                        if (day.touched) drawCircle(dot, radius = 2.dp.toPx(), center = topLeft + Offset(c / 2, c / 2))
                        // Ink ring on the picked day (today when none); today keeps a thin ring either way.
                        val ring = i == selected || (i == model.todayIndex && selected == null)
                        if (ring || i == model.todayIndex) {
                            val w = (if (ring) 1.5.dp else 1.dp).toPx()
                            drawRoundRect(
                                if (ring) colors.ink else colors.ink3,
                                topLeft + Offset(w / 2, w / 2),
                                Size(c - w, c - w),
                                radius,
                                style = Stroke(w),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            val stats = picked?.stats ?: model.week
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    picked?.let { StudyHeatmapRules.dayLabel(it.date) } ?: "本周",
                    style = AjlTheme.type.meta.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    color = work.accent,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                StatRow("学完", "${stats.finished} 回")
                StatRow("答题", "${stats.answers}")
                StatRow("自習", "${stats.studied} 句")
                StatRow("正确率", stats.accuracy?.let { "$it%" } ?: "—")
                StatRow("时长", StudyHeatmapRules.duration(stats.seconds.toLong()))
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = AjlTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 17.sp), color = AjlTheme.colors.ink3)
        Text(value, style = AjlTheme.type.meta.copy(fontSize = 12.sp, lineHeight = 17.sp), color = AjlTheme.colors.ink)
    }
}
