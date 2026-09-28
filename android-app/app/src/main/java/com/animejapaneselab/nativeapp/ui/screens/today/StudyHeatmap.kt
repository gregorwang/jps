package com.animejapaneselab.nativeapp.ui.screens.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
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

/** One square. [level] 0–4 = how long was studied that day (0, <10, <20, <40, 40+ minutes). */
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
            StudyHeatCell(day, level(stats.seconds), touched = false, stats = stats)
        }
        return StudyHeatmapModel(
            cells = cells,
            gridStart = gridStart,
            todayIndex = (Weeks - 1) * 7 + (today.dayOfWeek.value - 1),
            week = statsOf((0..6).map { today.minusDays(it.toLong()) }),
            totalSeconds = maxOf(totalSeconds, log.values.sumOf { it.seconds.toLong() }),
            mastered = progress.count { it.state == ReviewState.Known || it.state == ReviewState.Good },
        )
    }

    /** Minutes that start each shade: <10, <20, <40, 40+. */
    val LevelMinutes = listOf(1, 10, 20, 40)

    fun level(seconds: Int): Int {
        val minutes = seconds / 60
        return LevelMinutes.count { minutes >= it }
    }

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
 * 学習記録: one square a day, shaded by how long was studied (the progress line's ramp: the work
 * colour from pale to full). Tap a square for that day's numbers; tap it again for the last 7 days.
 */
@Composable
fun StudyHeatmapSection(model: StudyHeatmapModel, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val shades = listOf(
        colors.sunken,
        lerp(colors.sunken, work.accent, 0.22f),
        lerp(colors.sunken, work.accent, 0.45f),
        lerp(colors.sunken, work.accent, 0.72f),
        work.accent,
    )
    val gap = 4.dp
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val picked = selected?.let { model.cells.getOrNull(it) }
    val months = remember(model.gridStart) { StudyHeatmapRules.monthLabels(model.gridStart) }
    val labelStyle = AjlTheme.type.meta.copy(fontSize = 10.sp, lineHeight = 12.sp)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading(
            title = "学習記録",
            meta = "12 週 · 累计 ${StudyHeatmapRules.duration(model.totalSeconds)}",
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val labelWidth = 20.dp
            val cell = ((maxWidth - labelWidth - gap * (StudyHeatmapRules.Weeks - 1)) / StudyHeatmapRules.Weeks).coerceAtMost(26.dp)
            val gridWidth = cell * StudyHeatmapRules.Weeks + gap * (StudyHeatmapRules.Weeks - 1)
            Row(verticalAlignment = Alignment.Top) {
                // 月 水 金 日 beside the rows.
                Column(Modifier.padding(top = 16.dp).width(labelWidth), verticalArrangement = Arrangement.spacedBy(gap)) {
                    listOf("月", "", "水", "", "金", "", "日").forEach { label ->
                        Box(Modifier.height(cell), contentAlignment = Alignment.CenterStart) {
                            if (label.isNotEmpty()) Text(label, style = labelStyle, color = colors.ink3)
                        }
                    }
                }
                Column {
                    Box(Modifier.width(gridWidth).height(16.dp)) {
                        months.forEach { (col, label) ->
                            Text(label, style = labelStyle, color = colors.ink3, modifier = Modifier.offset(x = (cell + gap) * col))
                        }
                    }
                    Canvas(
                        Modifier
                            .size(width = gridWidth, height = cell * 7 + gap * 6)
                            .pointerInput(model.cells, cell) {
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
                            .semantics { contentDescription = "最近 12 周学习记录，最近 7 天学了 ${StudyHeatmapRules.duration(model.week.seconds.toLong())}" },
                    ) {
                        val c = cell.toPx()
                        val step = c + gap.toPx()
                        val radius = CornerRadius(4.dp.toPx())
                        model.cells.forEachIndexed { i, day ->
                            if (day == null) return@forEachIndexed
                            val topLeft = Offset((i / 7) * step, (i % 7) * step)
                            drawRoundRect(shades[day.level.coerceIn(0, 4)], topLeft, Size(c, c), radius)
                            if (i == model.todayIndex) {
                                val w = 1.5.dp.toPx()
                                drawRoundRect(colors.ink, topLeft + Offset(w / 2, w / 2), Size(c - w, c - w), radius, style = Stroke(w))
                            }
                            if (i == selected) {
                                val w = 2.dp.toPx()
                                val out = 2.dp.toPx()
                                drawRoundRect(work.accent, topLeft - Offset(out, out), Size(c + out * 2, c + out * 2), CornerRadius(5.dp.toPx()), style = Stroke(w))
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            Text("按学习时长  少", style = labelStyle, color = colors.ink3, modifier = Modifier.padding(end = 2.dp))
            shades.forEach { shade -> Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(shade)) }
            Text("多", style = labelStyle, color = colors.ink3, modifier = Modifier.padding(start = 2.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                picked?.let { StudyHeatmapRules.dayLabel(it.date) } ?: "最近 7 天",
                style = AjlTheme.type.jpLabel.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = work.accent,
            )
            if (picked != null) {
                Text(
                    "回到最近 7 天",
                    style = AjlTheme.type.meta.copy(fontSize = 11.sp, textDecoration = TextDecoration.Underline),
                    color = colors.ink3,
                    modifier = Modifier.clickableNoRipple(onClick = { selected = null }),
                )
            }
        }
        val stats = picked?.stats ?: model.week
        Row(Modifier.fillMaxWidth()) {
            Stat("时长", StudyHeatmapRules.duration(stats.seconds.toLong()), Modifier.weight(1f))
            Stat("学完", "${stats.finished} 回", Modifier.weight(1f))
            Stat("答题", "${stats.answers}", Modifier.weight(1f))
            Stat("正确率", stats.accuracy?.let { "$it%" } ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = AjlTheme.type.meta.copy(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium), color = AjlTheme.colors.ink)
        Text(label, style = AjlTheme.type.caption.copy(fontSize = 11.sp, lineHeight = 14.sp), color = AjlTheme.colors.ink3)
    }
}
