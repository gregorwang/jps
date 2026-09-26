package com.animejapaneselab.nativeapp.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AiCoachState
import com.animejapaneselab.nativeapp.data.MistakeRecord
import com.animejapaneselab.nativeapp.data.ProgressItem
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.domain.SmartReviewEntry
import com.animejapaneselab.nativeapp.domain.buildSmartReviewPlan
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.BroadcastLine
import com.animejapaneselab.nativeapp.ui.design.EmptyNote
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.ToolPanel
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import java.time.LocalDate
import kotlin.math.roundToInt

/** 復習 tab: 翻卡堆 + 苦手なところ + 错题本. */
@Composable
fun ReviewScreen(
    uiState: LabUiState,
    onOpenLesson: () -> Unit,
    onOpenSmartReviewQueue: () -> Unit,
    onMistakeReviewed: (String) -> Unit,
    onPracticeMistake: (String) -> Unit,
    onPracticeRemoteTask: (ProgressItem) -> Unit,
    onExplainMistake: (String) -> Unit,
    /** Opens subtitles at the source line; lineNo = 0 opens the episode without focusing a line. */
    onViewSource: (workSlug: String, episode: Int, lineNo: Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val colors = AjlTheme.colors
    val reviewTasks = remember(uiState.reviewTasks) { uiState.reviewTasks.distinctBy(ReviewRules::taskIdentity) }
    val mistakes = remember(uiState.mistakes) { uiState.mistakes.distinctBy(ReviewRules::mistakeIdentity) }
    val plan = remember(reviewTasks, mistakes, today) { buildSmartReviewPlan(reviewTasks, mistakes, today) }
    val mistakesById = remember(mistakes) { mistakes.associateBy { it.itemId } }
    val deck = remember(plan, mistakesById, today) {
        ReviewRules.deck(plan).map { it.toCard(mistakesById, today) }
    }
    val weak = remember(uiState.progressItems, today) { ReviewRules.weakSpots(uiState.progressItems, today) }
    val dismissed = remember { mutableStateListOf<String>() }
    val remaining = deck.filterNot { it.key in dismissed }
    val dueCount = ReviewRules.dueCount(plan)
    var notebookOpen by rememberSaveable { mutableStateOf(false) }
    val entryByKey = remember(plan) { plan.entries.associateBy { it.key } }

    fun practice(card: ReviewCard) {
        val entry = entryByKey[card.key] ?: return
        val mistakeId = entry.localMistakeId
        if (mistakeId != null) onPracticeMistake(mistakeId) else entry.remoteTask?.let(onPracticeRemoteTask)
    }

    Column(modifier.fillMaxSize().background(colors.bg)) {
        BroadcastLine()
        TopBar(
            nav = TopBarNav.None,
            title = "復習",
            actions = { IconButton44(Icons.Rounded.Search, "搜索", onOpenSearch) },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            item(key = "deck", contentType = "deck") {
                Column {
                    when {
                        deck.isEmpty() -> EmptyNote("今日の復習はなし", gloss = "没有到期的卡片")
                        remaining.isEmpty() -> Column(
                            Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            EmptyNote("ひと通りめくった", modifier = Modifier.padding(bottom = 0.dp))
                            QuietButton("再翻一遍", onClick = { dismissed.clear() })
                        }
                        else -> ReviewDeck(
                            cards = remaining,
                            onSwiped = { card, direction ->
                                dismissed += card.key
                                if (direction == SwipeDirection.Right) card.localMistakeId?.let(onMistakeReviewed)
                            },
                            onPractice = ::practice,
                        )
                    }
                    if (deck.isNotEmpty()) {
                        DueCount(
                            count = dueCount,
                            caption = if (dueCount > 0) {
                                "今天到期 · 约 ${plan.estimatedMinutes} 分钟"
                            } else {
                                "近日 ${plan.entries.size} 枚"
                            },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (plan.entries.isNotEmpty()) {
                        InkButton(
                            text = "开始复习",
                            jpText = "めくる",
                            trailingArrow = true,
                            onClick = onOpenSmartReviewQueue,
                        )
                    } else {
                        InkButton(text = "去学ぶ", trailingArrow = true, onClick = onOpenLesson)
                    }
                }
            }
            if (weak.isNotEmpty()) {
                item(key = "weak", contentType = "weak") {
                    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeading(title = "苦手なところ", meta = "最近 7 天")
                        weak.forEach { spot -> WeakSpotRow(spot) }
                    }
                }
            }
            val notebookTotal = mistakes.size + reviewTasks.size
            if (notebookTotal > 0) {
                item(key = "notebook-toggle", contentType = "toggle") {
                    Row(
                        Modifier
                            .padding(top = if (weak.isEmpty()) 20.dp else 8.dp)
                            .heightIn(min = 44.dp)
                            .clickableNoRipple({ notebookOpen = !notebookOpen }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("错题本 · ${mistakes.size} 题", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink2)
                        if (reviewTasks.isNotEmpty()) {
                            Text("· 到期任务 ${reviewTasks.size}", style = AjlTheme.type.body.copy(fontSize = 14.sp), color = colors.ink3)
                        }
                        Icon(
                            if (notebookOpen) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                            contentDescription = if (notebookOpen) "收起错题本" else "展开错题本",
                            tint = colors.ink2,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            if (notebookOpen) {
                if (mistakes.isNotEmpty()) {
                    item(key = "mistakes-heading", contentType = "heading") {
                        SectionHeading(
                            title = "間違いノート",
                            gloss = "错题本",
                            meta = "${mistakes.size} 题",
                            modifier = Modifier.padding(top = 12.dp).semantics { heading() },
                        )
                    }
                    items(mistakes, key = { "m-" + ReviewRules.mistakeIdentity(it) }, contentType = { "mistake" }) { mistake ->
                        MistakeItem(
                            mistake = mistake,
                            today = today,
                            dueOn = plan.entries.firstOrNull { it.localMistakeId == mistake.itemId }?.remoteTask?.nextReviewOn,
                            ai = uiState.aiCoach.takeIf { uiState.reviewAiTargetId == mistake.itemId },
                            onPractice = { onPracticeMistake(mistake.itemId) },
                            onExplain = { onExplainMistake(mistake.itemId) },
                            onReviewed = { onMistakeReviewed(mistake.itemId) },
                            onViewSource = { onViewSource(mistake.workSlug, mistake.episode, ReviewRules.sourceLineNo(mistake)) },
                        )
                    }
                }
                if (reviewTasks.isNotEmpty()) {
                    item(key = "tasks-heading", contentType = "heading") {
                        SectionHeading(
                            title = "期日の課題",
                            gloss = "到期任务",
                            meta = "${reviewTasks.size} 项",
                            modifier = Modifier.padding(top = 20.dp).semantics { heading() },
                        )
                    }
                    items(reviewTasks, key = { "t-" + ReviewRules.taskIdentity(it) }, contentType = { "task" }) { task ->
                        TaskItem(
                            task = task,
                            today = today,
                            onPractice = { onPracticeRemoteTask(task) },
                            onViewSource = { onViewSource(task.workSlug, task.episode, ReviewRules.sourceLineNo(task)) },
                        )
                    }
                }
            }
        }
    }
}

private fun SmartReviewEntry.toCard(mistakesById: Map<String, MistakeRecord>, today: LocalDate): ReviewCard {
    val mistake = localMistakeId?.let(mistakesById::get)
    val task = remoteTask
    return ReviewCard(
        key = key,
        meta = ReviewRules.cardMeta(
            type = mistake?.typeLabel ?: itemType,
            episode = episode,
            attempts = mistake?.attempts ?: 0,
        ),
        due = ReviewRules.dueLabel(task?.nextReviewOn, today) ?: if (mistake != null) "今天" else null,
        dueNow = ReviewRules.isDueNow(this),
        front = if (mistake != null) title else ReviewRules.taskLabel(title),
        answer = mistake?.expected?.takeIf { it.isNotBlank() },
        yourAnswer = mistake?.selected?.takeIf { it.isNotBlank() },
        note = mistake?.explanation?.takeIf { it.isNotBlank() }
            ?: task?.let { "上次 · ${it.state.label}" + if (it.reviewCount > 0) " · 已复习 ${it.reviewCount} 次" else "" },
        localMistakeId = mistake?.itemId,
    )
}

/** 18 枚 · 今天到期 · 约 9 分钟 — the work-colour due count under the deck. */
@Composable
private fun DueCount(count: Int, caption: String) {
    Row(
        Modifier.padding(start = 10.dp, top = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            count.toString(),
            style = AjlTheme.type.meta.copy(fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Medium),
            color = if (count > 0) AjlTheme.work.accent else AjlTheme.colors.ink,
        )
        Text("枚", style = AjlTheme.type.jpLabel.copy(fontSize = 15.sp, lineHeight = 22.sp), color = AjlTheme.colors.ink, modifier = Modifier.padding(bottom = 6.dp))
        Text(caption, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3, modifier = Modifier.padding(bottom = 7.dp))
    }
}

@Composable
private fun WeakSpotRow(spot: ReviewRules.WeakSpot) {
    val pct = (spot.accuracy * 100).roundToInt()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(spot.name, style = AjlTheme.type.body.copy(fontSize = 14.sp), color = AjlTheme.colors.ink, modifier = Modifier.weight(1f))
        Box(Modifier.width(96.dp)) {
            ProgressLine(progress = spot.accuracy, contentDescription = "${spot.name} 正确率 $pct%")
        }
        Text(
            "$pct%",
            style = AjlTheme.type.meta.copy(fontSize = 12.sp),
            color = AjlTheme.colors.ink2,
            modifier = Modifier.width(36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MistakeItem(
    mistake: MistakeRecord,
    today: LocalDate,
    dueOn: String?,
    ai: AiCoachState?,
    onPractice: () -> Unit,
    onExplain: () -> Unit,
    onReviewed: () -> Unit,
    onViewSource: () -> Unit,
) {
    val colors = AjlTheme.colors
    val canViewSource = mistake.workSlug.isNotBlank() && mistake.episode > 0
    val lineNo = ReviewRules.sourceLineNo(mistake)
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Eyebrow(
                    ReviewRules.cardMeta(mistake.typeLabel, mistake.episode, mistake.attempts) + " · " + mistake.lastState.label,
                    modifier = Modifier.weight(1f),
                )
                ReviewRules.dueLabel(dueOn, today)?.let { Eyebrow(it, color = AjlTheme.work.accent) }
            }
            Text(
                mistake.prompt,
                style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp),
                color = colors.ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            AnswerPair("你答", mistake.selected.ifBlank { "未作答" }, colors.bad)
            AnswerPair("正解", mistake.expected, colors.ok)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlineButton("再练", onClick = onPractice, compact = true)
                OutlineButton("AI 讲解", onClick = onExplain, compact = true)
                if (canViewSource) OutlineButton(ReviewRules.viewSourceLabel(lineNo), onClick = onViewSource, compact = true)
                QuietButton("已掌握", onClick = onReviewed, color = colors.ok)
            }
            if (ai != null && (ai.answer.isNotBlank() || ai.status == SyncStatus.Loading || ai.status == SyncStatus.Error || ai.result != null)) {
                AiExplainPanel(ai)
            }
        }
        Hairline()
    }
}

@Composable
private fun AnswerPair(label: String, value: String, tint: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.Top) {
        Text(label, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3, modifier = Modifier.width(40.dp).padding(top = 2.dp))
        Text(value, style = AjlTheme.type.body, color = tint, modifier = Modifier.weight(1f))
    }
}

/** AI 讲解 result: a tool panel (1px hairline, 12px corners) — structured sections when present. */
@Composable
private fun AiExplainPanel(ai: AiCoachState) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    ToolPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val result = ai.result
            when {
                ai.status == SyncStatus.Loading && result == null -> LoadingDots(delayMillis = 0)
                result != null -> {
                    if (result.title.isNotBlank()) Text(result.title, style = type.title.copy(fontSize = 16.sp, lineHeight = 22.sp), color = colors.ink)
                    if (result.summary.isNotBlank()) Text(result.summary, style = type.body, color = colors.ink2)
                    result.sections.forEach { section ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (section.title.isNotBlank()) Text(section.title, style = type.caption.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
                            Text(section.body, style = type.body.copy(fontSize = 14.sp), color = colors.ink2)
                        }
                    }
                    if (result.sections.isEmpty() && result.summary.isBlank() && result.text.isNotBlank()) {
                        Text(result.text, style = type.body, color = colors.ink2)
                    }
                }
                else -> Text(
                    ai.answer,
                    style = type.body,
                    color = if (ai.status == SyncStatus.Error) colors.bad else colors.ink2,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskItem(
    task: ProgressItem,
    today: LocalDate,
    onPractice: () -> Unit,
    onViewSource: () -> Unit,
) {
    val colors = AjlTheme.colors
    val canViewSource = task.workSlug.isNotBlank() && task.episode > 0
    val due = ReviewRules.dueLabel(task.nextReviewOn, today)
    val dueNow = ReviewRules.parseDate(task.nextReviewOn)?.let { !it.isAfter(today) } ?: true
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Eyebrow(
                    ReviewRules.cardMeta(task.itemType, task.episode, 0) + " · " + task.state.label,
                    modifier = Modifier.weight(1f),
                )
                if (due != null) Eyebrow(due, color = if (dueNow) AjlTheme.work.accent else colors.ink3)
            }
            Text(
                ReviewRules.taskLabel(task.label).ifBlank { "待复习内容" },
                style = AjlTheme.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlineButton("练习", onClick = onPractice, compact = true)
                if (canViewSource) {
                    OutlineButton(ReviewRules.viewSourceLabel(ReviewRules.sourceLineNo(task)), onClick = onViewSource, compact = true)
                }
            }
        }
        Hairline()
    }
}
