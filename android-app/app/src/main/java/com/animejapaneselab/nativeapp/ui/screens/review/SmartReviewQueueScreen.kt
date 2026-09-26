package com.animejapaneselab.nativeapp.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.domain.ReviewDueBucket
import com.animejapaneselab.nativeapp.domain.SmartReviewPlan
import com.animejapaneselab.nativeapp.ui.design.EmptyNote
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LineRow
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import java.time.LocalDate

/**
 * Smart review queue (secondary screen, no artboard — derived from V3Review): the planner's
 * cards grouped by due bucket (期限切れ → 今日 → 未定 → 近日), priority order kept inside each
 * bucket. One ink button starts the first card; tapping any row starts that card.
 */
@Composable
fun SmartReviewQueueScreen(
    plan: SmartReviewPlan,
    onBack: () -> Unit,
    onStartItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val colors = AjlTheme.colors
    val groups = remember(plan.entries) { ReviewRules.queueGroups(plan.entries) }
    val first = remember(plan.entries) { ReviewRules.firstInQueue(plan.entries) }
    val dueCount = ReviewRules.dueCount(plan)

    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(title = "復習の順番", nav = TopBarNav.Back, onNav = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            item(key = "summary", contentType = "summary") {
                Column {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "${plan.entries.size}",
                            style = AjlTheme.type.meta.copy(fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Medium),
                            color = if (dueCount > 0) AjlTheme.work.accent else colors.ink,
                        )
                        Text("枚", style = AjlTheme.type.jpLabel.copy(fontSize = 15.sp, lineHeight = 22.sp), color = colors.ink, modifier = Modifier.padding(bottom = 6.dp))
                        Text(
                            listOfNotNull(
                                plan.overdueCount.takeIf { it > 0 }?.let { "逾期 $it" },
                                plan.dueTodayCount.takeIf { it > 0 }?.let { "今天 $it" },
                                plan.estimatedMinutes.takeIf { it > 0 }?.let { "约 $it 分钟" },
                            ).joinToString(" · "),
                            style = AjlTheme.type.caption,
                            color = colors.ink3,
                            modifier = Modifier.padding(bottom = 7.dp),
                        )
                    }
                    if (plan.entries.isNotEmpty() && plan.focusLabel.isNotBlank()) {
                        Text(
                            "重点 · ${plan.focusLabel}",
                            style = AjlTheme.type.meta,
                            color = colors.ink2,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (first != null) {
                        InkButton(
                            text = "从第一张开始",
                            caption = first.title,
                            trailingArrow = true,
                            onClick = { onStartItem(first.key) },
                        )
                    }
                }
            }
            if (groups.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    EmptyNote("今日の復習はなし", gloss = "没有到期的卡片")
                }
            }
            groups.forEach { group ->
                item(key = "h-${group.bucket.name}", contentType = "heading") {
                    SectionHeading(
                        title = ReviewRules.bucketTitle(group.bucket),
                        gloss = group.bucket.label,
                        meta = "${group.rows.size} 枚",
                        modifier = Modifier.padding(top = 24.dp).semantics { heading() },
                    )
                }
                items(group.rows, key = { "r-${group.bucket.name}-${it.entry.key}" }, contentType = { "row" }) { row ->
                    QueueRow(row, bucket = group.bucket, today = today, onClick = { onStartItem(row.entry.key) })
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    row: ReviewRules.QueueRow,
    bucket: ReviewDueBucket,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val colors = AjlTheme.colors
    val entry = row.entry
    val urgent = bucket == ReviewDueBucket.Overdue || bucket == ReviewDueBucket.DueToday
    val due = ReviewRules.dueLabel(entry.remoteTask?.nextReviewOn, today)
    LineRow(
        onClick = onClick,
        minHeight = 60.dp,
        leading = {
            Text(
                row.position.toString().padStart(2, '0'),
                style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                color = if (row.position == 1) AjlTheme.work.accent else colors.ink3,
                modifier = Modifier.width(22.dp),
            )
        },
        trailing = {
            if (due != null) {
                Text(
                    due,
                    style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                    color = if (urgent) AjlTheme.work.accent else colors.ink3,
                )
            }
        },
    ) {
        Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (entry.localMistakeId != null) entry.title else ReviewRules.taskLabel(entry.title),
                style = AjlTheme.type.body,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(
                    ReviewRules.cardMeta(entry.itemType, entry.episode, 0),
                    entry.reason,
                ).filter { it.isNotBlank() }.joinToString(" · "),
                style = AjlTheme.type.meta,
                color = colors.ink3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
