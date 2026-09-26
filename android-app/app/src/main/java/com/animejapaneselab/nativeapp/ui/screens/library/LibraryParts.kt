package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AiExplainResult
import com.animejapaneselab.nativeapp.data.EpisodeOption
import com.animejapaneselab.nativeapp.data.CardEnrichment
import com.animejapaneselab.nativeapp.data.LinguisticCardPayload
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.data.WorkOption
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.AjlBottomSheet
import com.animejapaneselab.nativeapp.ui.design.Avatar
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LineRow
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.design.ToolPanel
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.reading.CharacterCatalog
import com.animejapaneselab.nativeapp.ui.reading.CharacterOption
import com.animejapaneselab.nativeapp.ui.reading.CharacterProfileController
import com.animejapaneselab.nativeapp.ui.reading.CharacterProfileState
import com.animejapaneselab.nativeapp.ui.reading.DeepDiveState
import com.animejapaneselab.nativeapp.ui.reading.SentenceDeepDiveController
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer

// ---------------------------------------------------------------------------
// Dictionary tabs (词汇 48 · 语法 9 · 台词 26) — V3Library header row.
// ---------------------------------------------------------------------------

internal data class DictTab(val label: String, val count: Int)

/**
 * Small sans tabs with a mono count, sitting on a 1.5px ink rule; the current tab gets a 3px
 * work-colour underline. [trailing] hangs at the right end of the rule (第三話 ▾).
 * Candidate for promotion to ui/design (see design/v3-requests/E.md).
 */
@Composable
internal fun DictTabs(
    tabs: List<DictTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        )
                        .drawBehind {
                            if (selected) {
                                val h = 3.dp.toPx()
                                drawRect(accent, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        tab.label,
                        style = AjlTheme.type.body.copy(
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = if (selected) colors.ink else colors.ink3,
                    )
                    Text(tab.count.toString(), style = AjlTheme.type.meta, color = colors.ink3)
                }
            }
            Spacer(Modifier.weight(1f))
            if (trailing != null) trailing()
        }
        Hairline(Modifier.align(Alignment.BottomStart), strong = true)
    }
}

/** 第三話 ▾ — the episode picker trigger: a 1px tool outline, serif label. */
@Composable
internal fun EpisodeChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .height(32.dp)
            .background(colors.surface, shape)
            .border(AjlStroke.Hair, colors.line2, shape)
            .clickableNoRipple(onClick)
            .semantics { contentDescription = "切换作品或集数：$label" }
            .padding(start = 8.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = AjlTheme.type.jpLabel.copy(fontSize = 13.sp, lineHeight = 18.sp), color = colors.ink, maxLines = 1)
        Icon(Icons.Rounded.ExpandMore, null, tint = colors.ink2, modifier = Modifier.size(14.dp))
    }
}

// ---------------------------------------------------------------------------
// 五十音 index rail
// ---------------------------------------------------------------------------

/**
 * Vertical あかさたなはまやらわ(他) rail. Rows without entries are faint and inert; the row in
 * view is work-colour 700. Tap or drag along the rail to jump.
 */
@Composable
internal fun KanaIndexRail(
    present: Set<String>,
    current: String?,
    onJump: (String) -> Unit,
    modifier: Modifier = Modifier,
    cellHeight: Dp = 28.dp,
) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    val rows = Gojuon.RailRows
    val haptics = LocalHapticFeedback.current
    val cellPx = with(LocalDensity.current) { cellHeight.toPx() }
    val latestPresent by rememberUpdatedState(present)
    val latestJump by rememberUpdatedState(onJump)
    var lastRow by remember { mutableStateOf<String?>(null) }
    fun jumpAt(y: Float) {
        val index = (y / cellPx).toInt().coerceIn(0, rows.lastIndex)
        // Snap to the nearest row that has entries so a drag never lands on nothing.
        val target = (index downTo 0).map { rows[it] }.firstOrNull { it in latestPresent }
            ?: (index..rows.lastIndex).map { rows[it] }.firstOrNull { it in latestPresent }
            ?: return
        if (target != lastRow) {
            lastRow = target
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            latestJump(target)
        }
    }
    Column(
        modifier
            .width(28.dp)
            .semantics { contentDescription = "五十音索引" }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    lastRow = null
                    jumpAt(offset.y)
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        lastRow = null
                        jumpAt(offset.y)
                    },
                    onVerticalDrag = { change, _ -> jumpAt(change.position.y) },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            val isCurrent = row == current
            val has = row in present
            Box(Modifier.height(cellHeight).width(28.dp), contentAlignment = Alignment.Center) {
                Text(
                    row,
                    style = AjlTheme.type.jpLabel.copy(
                        fontSize = 12.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = when {
                        isCurrent -> accent
                        has -> colors.ink3
                        else -> colors.faint.copy(alpha = 0.5f)
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Find field (引く)
// ---------------------------------------------------------------------------

/** Compact find-in-page field: 1px tool outline, 12dp corners, 40dp. */
@Composable
internal fun FindField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    ToolPanel(modifier.fillMaxWidth().height(40.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 2.dp).align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.Search, null, tint = colors.ink3, modifier = Modifier.size(16.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(placeholder, style = AjlTheme.type.caption, color = colors.faint, maxLines = 1)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = AjlTheme.type.caption.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
                )
            }
            if (value.isNotEmpty()) {
                IconButton44(Icons.Rounded.Close, "清除", { onValueChange("") }, tint = colors.ink3, iconSize = 16.dp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AI result
// ---------------------------------------------------------------------------

/** A structured AI explanation on a sunken tool panel: eyebrow, title, summary, sections. */
@Composable
internal fun AiResultBlock(
    result: AiExplainResult?,
    fallback: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    eyebrow: String = "講解 · AI",
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    ToolPanel(modifier.fillMaxWidth(), background = colors.sunken) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow(eyebrow)
            if (result == null || isError) {
                Text(
                    fallback.ifBlank { "暂时没有讲解" },
                    style = type.body,
                    color = if (isError) colors.bad else colors.ink2,
                )
                return@Column
            }
            if (result.title.isNotBlank()) Text(result.title, style = type.title.copy(fontSize = 16.sp, lineHeight = 22.sp), color = colors.ink)
            if (result.summary.isNotBlank()) Text(result.summary, style = type.body, color = colors.ink2)
            if (result.sections.isEmpty() && result.summary.isBlank() && result.text.isNotBlank()) {
                Text(result.text, style = type.body, color = colors.ink2)
            }
            result.sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (section.title.isNotBlank()) {
                        Text(section.title, style = type.label.copy(fontSize = 14.sp, lineHeight = 20.sp), color = colors.ink)
                    }
                    if (section.body.isNotBlank()) Text(section.body, style = type.body, color = colors.ink2)
                }
            }
        }
    }
}

/** The library AI answer for [targetKey], if it is the one currently asked about. */
@Composable
internal fun LibraryAiNote(targetKey: String, uiState: LabUiState, modifier: Modifier = Modifier) {
    if (uiState.libraryAiTargetKey != targetKey) return
    when (uiState.aiCoach.status) {
        SyncStatus.Loading -> Row(
            modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Eyebrow("講解 · AI")
            LoadingDots()
        }
        SyncStatus.Success, SyncStatus.Error -> AiResultBlock(
            result = uiState.aiCoach.result,
            fallback = uiState.aiCoach.answer,
            isError = uiState.aiCoach.status == SyncStatus.Error,
            modifier = modifier,
        )
        SyncStatus.Idle -> Unit
    }
}

// ---------------------------------------------------------------------------
// 語言学 addendum
// ---------------------------------------------------------------------------

/** Linguistics addendum (语言学加餐) — shown inside an opened entry, never collapsible itself. */
@Composable
internal fun EnrichmentNote(card: CardEnrichment?, modifier: Modifier = Modifier) {
    if (card == null || !card.hasContent) return
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val accent = AjlTheme.work.accent
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(listOfNotNull("要点", card.toneZh.ifBlank { null }).joinToString(" · "))
        if (card.structureZh.isNotBlank()) {
            Text(card.structureZh, style = type.jpBody.copy(fontSize = 15.sp, lineHeight = 22.sp), color = accent)
        }
        if (card.coreZh.isNotBlank()) Text(card.coreZh, style = type.body, color = colors.ink)
        if (card.chunks.size > 1) {
            Text(card.chunks.joinToString("  ／  "), style = type.jpBody.copy(fontSize = 15.sp, lineHeight = 24.sp), color = colors.ink2)
        }
        if (card.naturalZh.isNotBlank()) Text("意译 · ${card.naturalZh}", style = type.caption, color = colors.ink2)
        if (card.usageScenes.isNotEmpty()) {
            Text("场面 · ${card.usageScenes.joinToString("、")}", style = type.caption, color = colors.ink2)
        }
        card.breakdown.forEach { part ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(part.ja, style = type.jpBody.copy(fontSize = 14.sp, lineHeight = 20.sp), color = colors.ink)
                Text(
                    listOf(part.zh, part.noteZh).filter { it.isNotBlank() }.joinToString(" · "),
                    style = type.caption,
                    color = colors.ink3,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        card.mistakes.forEach { Text("易错 · $it", style = type.caption, color = colors.bad) }
    }
}

@Composable
internal fun LinguisticNote(payload: LinguisticCardPayload?, modifier: Modifier = Modifier) {
    if (payload == null || !payload.hasContent) return
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow(listOf("言語学", payload.level).filter { it.isNotBlank() }.joinToString(" · "))
        if (payload.headlineZh.isNotBlank()) Text(payload.headlineZh, style = type.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
        payload.terms.forEach { term ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(term.termZh, style = type.caption.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
                if (term.plainZh.isNotBlank()) Text(term.plainZh, style = type.caption, color = colors.ink3, modifier = Modifier.weight(1f))
            }
        }
        payload.domains.forEach { domain ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(domain.titleZh.ifBlank { domain.domain }, style = type.caption.copy(fontWeight = FontWeight.SemiBold), color = colors.ink)
                if (domain.takeawayZh.isNotBlank()) Text(domain.takeawayZh, style = type.caption, color = colors.ink2)
                if (domain.explanationZh.isNotBlank()) Text(domain.explanationZh, style = type.caption, color = colors.ink3)
            }
        }
        if (payload.cautionZh.isNotBlank()) Text("注意 · ${payload.cautionZh}", style = type.caption, color = colors.bad)
        if (payload.historicalNoteZh.isNotBlank()) Text("源流 · ${payload.historicalNoteZh}", style = type.caption, color = colors.ink3)
    }
}

// ---------------------------------------------------------------------------
// Sheets: 精読 (sentence deep dive) and 登場人物 (character language profile)
// ---------------------------------------------------------------------------

/** v3 rendering of [SentenceDeepDiveController]; render once per screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeepDiveSheet(controller: SentenceDeepDiveController) {
    val state = controller.state
    val target = when (state) {
        is DeepDiveState.Loading -> state.target
        is DeepDiveState.Ready -> state.target
        is DeepDiveState.Error -> state.target
        DeepDiveState.Hidden -> return
    }
    val colors = AjlTheme.colors
    AjlBottomSheet(onDismissRequest = controller::dismiss, title = "精読", gloss = "单句精读") {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MangaPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(parseSpokenLine(target.jaText).text, style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp), color = colors.ink)
                    if (target.zhText.isNotBlank()) Text(target.zhText, style = AjlTheme.type.caption, color = colors.ink3)
                }
            }
            when (state) {
                is DeepDiveState.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    LoadingDots()
                }
                is DeepDiveState.Error -> {
                    Text(state.message, style = AjlTheme.type.body, color = colors.bad)
                    OutlineButton("重试", { controller.request(target) })
                }
                is DeepDiveState.Ready -> AiResultBlock(state.result, state.result.text, eyebrow = "精読 · AI")
                DeepDiveState.Hidden -> Unit
            }
        }
    }
}

/** Resolves a speaker name to its catalogue entry for the character-profile sheet. */
internal fun characterOptionFor(workSlug: String, speaker: String?): CharacterOption? {
    val ref = WorkIdentity.character(speaker) ?: return null
    return CharacterCatalog.charactersFor(workSlug).firstOrNull { option ->
        WorkIdentity.character(option.nameJa)?.name == ref.name
    }
}

/** v3 rendering of [CharacterProfileController]; render once per screen. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun CharacterSheet(controller: CharacterProfileController) {
    val state = controller.state
    if (state is CharacterProfileState.Hidden) return
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    AjlBottomSheet(onDismissRequest = controller::dismiss, title = "登場人物", gloss = "角色语言画像") {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (state) {
                CharacterProfileState.Picking -> {
                    val characters = CharacterCatalog.charactersFor(controller.workSlug)
                    if (characters.isEmpty()) {
                        Text("这部番还没有预置角色", style = type.body, color = colors.ink3)
                    }
                    Column {
                        characters.forEach { option ->
                            LineRow(
                                onClick = { controller.select(option) },
                                leading = { Avatar(WorkIdentity.character(option.nameJa), size = 36.dp) },
                            ) {
                                Text(option.nameJa, style = type.jpTitle, color = colors.ink)
                                if (option.nameZh.isNotBlank() && option.nameZh != option.nameJa) {
                                    Text("  ${option.nameZh}", style = type.caption, color = colors.ink3)
                                }
                            }
                        }
                    }
                }
                is CharacterProfileState.Loading -> {
                    CharacterHead(state.option, meta = null)
                    Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) { LoadingDots() }
                }
                is CharacterProfileState.Error -> {
                    CharacterHead(state.option, meta = null, onChange = controller::backToPicker)
                    Text(state.message, style = type.body, color = colors.bad)
                    OutlineButton("重试", { controller.select(state.option) })
                }
                is CharacterProfileState.Ready -> {
                    val profile = state.profile
                    CharacterHead(
                        state.option,
                        meta = if (profile.sourceCount > 0) "${profile.sourceCount} 段字幕" else null,
                        onChange = controller::backToPicker,
                    )
                    if (profile.cacheWarning.isNotBlank()) Text(profile.cacheWarning, style = type.caption, color = colors.ink3)
                    AiResultBlock(profile.result, profile.result.text, eyebrow = "画像 · AI")
                    OutlineButton("重新生成", { controller.select(state.option, regenerate = true) })
                }
                CharacterProfileState.Hidden -> Unit
            }
        }
    }
}

@Composable
private fun CharacterHead(option: CharacterOption, meta: String?, onChange: (() -> Unit)? = null) {
    val colors = AjlTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(WorkIdentity.character(option.nameJa), size = 44.dp, highlighted = true)
        Column(Modifier.weight(1f)) {
            Text(option.nameJa, style = AjlTheme.type.jpTitle.copy(fontSize = 19.sp, lineHeight = 26.sp), color = colors.ink)
            val sub = listOfNotNull(option.nameZh.takeIf { it.isNotBlank() && it != option.nameJa }, meta).joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = AjlTheme.type.meta, color = colors.ink3)
        }
        if (onChange != null) QuietButton("换角色", onChange)
    }
}

// ---------------------------------------------------------------------------
// Episode picker sheet (どの話？)
// ---------------------------------------------------------------------------

/** Episode label for headers: 第三話 (falls back to EP03 when out of kanji range). */
internal fun episodeTitle(episode: Int): String = TextRules.episodeLabel(episode)

/** Short work name for headers (けいおん！ / Re:ゼロ / catalogue name). */
internal fun workTitle(uiState: LabUiState): String {
    val work = uiState.works.firstOrNull { it.slug == uiState.selection.workSlug }
    return WorkIdentity.displayName(uiState.selection.workSlug, work?.displayName ?: uiState.focus.workTitle)
}

/**
 * Work + episode picker. Works as rows with the representative avatar; episodes grouped by
 * season (Re:ゼロ 一期/二期/三期) as mono number cells, the current one ink-filled.
 * [actions] renders under the grid (library: この話を学ぶ / 読空気 / 登場人物).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun EpisodePickerSheet(
    works: List<WorkOption>,
    episodes: List<EpisodeOption>,
    selectedWork: String,
    selectedEpisode: Int,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    actions: (@Composable () -> Unit)? = null,
) {
    val colors = AjlTheme.colors
    AjlBottomSheet(onDismissRequest = onDismiss, title = "どの話？", gloss = "换一集") {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (works.size > 1) {
                Column {
                    works.forEach { work ->
                        val selected = work.slug == selectedWork
                        LineRow(
                            onClick = { if (!selected) onWorkSelected(work.slug) },
                            leading = {
                                Avatar(WorkIdentity.representative(work.slug, 1, work.episodeCount), size = 36.dp, highlighted = selected)
                            },
                            trailing = { Text("${work.episodeCount} 話", style = AjlTheme.type.meta, color = colors.ink3) },
                        ) {
                            Text(
                                WorkIdentity.displayName(work.slug, work.displayName),
                                style = AjlTheme.type.jpTitle.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                                color = if (selected) colors.ink else colors.ink2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            val available = episodes.filter { it.workSlug.isBlank() || it.workSlug == selectedWork }.ifEmpty { episodes }
            val count = works.firstOrNull { it.slug == selectedWork }?.episodeCount
                ?: available.maxOfOrNull { it.episode } ?: 0
            val seasons = WorkIdentity.seasons(selectedWork, maxOf(count, available.maxOfOrNull { it.episode } ?: 0))
            seasons.forEach { season ->
                val inSeason = available.filter { it.episode in season }
                if (inSeason.isEmpty()) return@forEach
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (seasons.size > 1) {
                        Eyebrow("${season.title} · ${season.firstEpisode.pad2()}–${season.lastEpisode.pad2()}")
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        inSeason.forEach { episode ->
                            EpisodeCell(
                                number = episode.episode,
                                selected = episode.episode == selectedEpisode,
                                onClick = { onEpisodeSelected(episode.episode) },
                            )
                        }
                    }
                }
            }
            if (actions != null) {
                Hairline()
                actions()
            }
        }
    }
}

@Composable
private fun EpisodeCell(number: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    Box(
        Modifier
            .size(44.dp)
            .background(if (selected) colors.ink else colors.surface, AjlShape.Panel)
            .border(if (selected) AjlStroke.Ink else AjlStroke.Hair, if (selected) colors.ink else colors.line2, AjlShape.Panel)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { contentDescription = episodeTitle(number) },
        contentAlignment = Alignment.Center,
    ) {
        Text(number.pad2(), style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = if (selected) colors.onInk else colors.ink)
    }
}

internal fun Int.pad2(): String = toString().padStart(2, '0')

/** Chevron that turns 180° over [MotionTokens.Dur.State] (graphicsLayer only). */
@Composable
internal fun Chevron(open: Boolean, description: String, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = MotionTokens.standard(MotionTokens.Dur.State, reduced),
        label = "chevron",
    )
    Icon(
        Icons.Rounded.ExpandMore,
        contentDescription = description,
        tint = AjlTheme.colors.ink3,
        modifier = modifier.size(18.dp).graphicsLayer { rotationZ = rotation },
    )
}

/** Primary action for the picker sheet (the sheet's single ink button). */
@Composable
internal fun LearnEpisodeButton(episode: Int, onClick: () -> Unit) {
    InkButton("学这一话", onClick, jpText = "${episodeTitle(episode)}を学ぶ", trailingArrow = true)
}

/** Saveable helper for an optional String (expanded entry key). */
@Composable
internal fun rememberExpandedKey(vararg inputs: Any?) = rememberSaveable(*inputs) { mutableStateOf<String?>(null) }
