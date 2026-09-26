package com.animejapaneselab.nativeapp.ui.screens.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
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

data class StudyHeatmapModel(
    /** 84 cells, column-major (week by week, Mon→Sun); null = a future day. */
    val levels: List<Int?>,
    val todayIndex: Int,
    val weekAnswers: Int,
    val weekAccuracy: Int?,
    val weekSeconds: Int,
    val mastered: Int,
)

object StudyHeatmapRules {
    const val Weeks = 12

    fun build(log: Map<String, StudyDay>, progress: List<ProgressItem>, today: LocalDate): StudyHeatmapModel {
        // Days before the local log existed still show up, from each item's last review.
        val backfill = progress.mapNotNull { localDate(it.lastReviewedAt) }.groupingBy { it.toString() }.eachCount()
        fun answersOn(day: LocalDate): Int = maxOf(log[day.toString()]?.answers ?: 0, backfill[day.toString()] ?: 0)

        val weekStart = today.with(DayOfWeek.MONDAY)
        val gridStart = weekStart.minusWeeks((Weeks - 1).toLong())
        val levels = (0 until Weeks * 7).map { i ->
            val day = gridStart.plusDays(i.toLong())
            if (day.isAfter(today)) null else level(answersOn(day))
        }
        val week = (0..6).map { weekStart.plusDays(it.toLong()) }.filter { !it.isAfter(today) }
        val weekLog = week.mapNotNull { log[it.toString()] }
        val logged = weekLog.sumOf { it.answers }
        return StudyHeatmapModel(
            levels = levels,
            todayIndex = (Weeks - 1) * 7 + (today.dayOfWeek.value - 1),
            weekAnswers = week.sumOf(::answersOn),
            weekAccuracy = if (logged > 0) weekLog.sumOf { it.correct } * 100 / logged else null,
            weekSeconds = weekLog.sumOf { it.seconds },
            mastered = progress.count { it.state == ReviewState.Known || it.state == ReviewState.Good },
        )
    }

    fun level(answers: Int): Int = when {
        answers <= 0 -> 0
        answers < 10 -> 1
        answers < 30 -> 2
        else -> 3
    }

    fun duration(seconds: Int): String {
        val minutes = seconds / 60
        return when {
            minutes <= 0 -> "0m"
            minutes < 60 -> "${minutes}m"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    private fun localDate(iso: String): LocalDate? {
        if (iso.isBlank()) return null
        return runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate() }
            .recoverCatching { LocalDate.parse(iso.take(10)) }
            .getOrNull()
    }
}

/** 最近 12 週: v2's dot grid (one square a day, work-colour by volume) with the week's numbers beside it. */
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
    val cell = 13.dp
    val gap = 3.dp
    Column(modifier.fillMaxWidth()) {
        SectionHeading(title = "最近 12 週", meta = "本周 ${model.weekAnswers} 题")
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Canvas(
                Modifier
                    .size(width = cell * StudyHeatmapRules.Weeks + gap * (StudyHeatmapRules.Weeks - 1), height = cell * 7 + gap * 6)
                    .semantics { contentDescription = "最近 12 周学习记录，本周 ${model.weekAnswers} 题" },
            ) {
                val c = cell.toPx()
                val step = c + gap.toPx()
                val radius = CornerRadius(3.dp.toPx())
                model.levels.forEachIndexed { i, level ->
                    if (level == null) return@forEachIndexed
                    val topLeft = Offset((i / 7) * step, (i % 7) * step)
                    drawRoundRect(shades[level], topLeft, Size(c, c), radius)
                    if (i == model.todayIndex) {
                        val w = 1.5.dp.toPx()
                        drawRoundRect(
                            colors.ink,
                            topLeft + Offset(w / 2, w / 2),
                            Size(c - w, c - w),
                            radius,
                            style = Stroke(w),
                        )
                    }
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow("正确率", model.weekAccuracy?.let { "$it%" } ?: "—")
                StatRow("时长", StudyHeatmapRules.duration(model.weekSeconds))
                StatRow("已掌握", "${model.mastered}")
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3)
        Text(value, style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = AjlTheme.colors.ink)
    }
}
