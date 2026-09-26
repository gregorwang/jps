package com.animejapaneselab.nativeapp.ui.screens.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LineRow
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.theme.normalizeWorkSlug

/**
 * 課程: the work's 主視覚 (+ 换一部番 → [CourseSwitcherSheet]), then the 話 list with the selected
 * episode opened as a manga panel of 場面 01–06 and the single ink button, then 題型別 drills.
 */
@Composable
internal fun CourseScreen(
    uiState: LabUiState,
    actions: CourseActions,
    modifier: Modifier = Modifier,
) {
    val model = remember(
        uiState.selection,
        uiState.works,
        uiState.episodes,
        uiState.progressItems,
        uiState.vocab,
        uiState.grammar,
        uiState.shadowing,
        uiState.exercises,
        uiState.episodePlan,
        uiState.readAir.exercises,
        uiState.reviewTasks,
        uiState.mistakes,
        uiState.lesson,
    ) { uiState.courseScreenModel() }
    var switcherOpen by rememberSaveable { mutableStateOf(false) }

    val startScene: (CourseScene) -> Unit = { scene ->
        when (scene.kind) {
            SceneKind.Review -> actions.onStartReview()
            SceneKind.ReadAir -> actions.onStartReadAirBatch(scene.nextBatch?.batch ?: 1)
            else -> {
                val mode = scene.kind.lessonMode
                if (mode == null) {
                    actions.onStartLesson()
                } else {
                    val batch = scene.nextBatch
                    actions.onStartModeLesson(mode, batch?.batch ?: 1, batch?.key ?: "${scene.kind.keyPrefix}-1")
                }
            }
        }
    }

    val before = model.rows.filter { it.number < model.selectedEpisode }
    val after = model.rows.filter { it.number > model.selectedEpisode }

    LazyColumn(modifier.fillMaxSize()) {
        item(key = "hero") {
            CourseHero(model, onSwitch = { switcherOpen = true }, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
            Spacer(Modifier.height(16.dp))
        }
        before.forEach { row ->
            item(key = "ep-${row.number}") {
                EpisodeLine(row, onClick = { actions.onEpisodeSelected(row.number) })
            }
        }
        item(key = "current") {
            EpisodePanel(
                model = model,
                onScene = startScene,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
        after.forEach { row ->
            item(key = "ep-${row.number}") {
                EpisodeLine(row, onClick = { actions.onEpisodeSelected(row.number) })
            }
        }
        item(key = "drills") {
            DrillSection(
                drills = model.drills,
                onDrill = { drill -> drill.kind?.let(actions.onStartExercise) ?: actions.onStartExerciseMix() },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
            )
        }
    }

    if (switcherOpen) {
        CourseSwitcherSheet(
            model = model,
            onDismiss = { switcherOpen = false },
            onSeasonSelected = { row ->
                val sameWork = normalizeWorkSlug(row.workSlug) ==
                    normalizeWorkSlug(model.workSlug)
                if (!sameWork) actions.onWorkSelected(row.workSlug)
                row.jumpTo?.let(actions.onEpisodeSelected)
                switcherOpen = false
            },
            onEpisodeSelected = { episode ->
                actions.onEpisodeSelected(episode)
                switcherOpen = false
            },
        )
    }
}

@Composable
private fun CourseHero(model: CourseScreenModel, onSwitch: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    MangaPanel(modifier.fillMaxWidth().height(118.dp)) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .width(118.dp)
                    .fillMaxHeight()
                    .background(work.soft)
                    .screentone(work.tone(0.35f), angleDegrees = 0f)
                    .drawBehind {
                        val w = AjlStroke.Ink.toPx()
                        drawRect(colors.ink, topLeft = Offset(size.width - w, 0f), size = Size(w, size.height))
                    },
            ) {
                model.heroArt?.let { art ->
                    Image(
                        painter = painterResource(art),
                        contentDescription = model.workName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().padding(end = 1.5.dp),
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        model.workName,
                        style = AjlTheme.type.jpDisplay.copy(fontSize = 24.sp, lineHeight = 30.sp),
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(model.heroMeta, style = AjlTheme.type.meta, color = colors.ink3, maxLines = 1)
                }
                OutlineButton(
                    text = "换一部番",
                    onClick = onSwitch,
                    compact = true,
                    trailingIcon = Icons.Rounded.ExpandMore,
                )
            }
        }
    }
}

/** A collapsed 話 row: done / past ones above the panel, upcoming ones below (faint). */
@Composable
private fun EpisodeLine(row: EpisodeRow, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val upcoming = row.state == EpisodeRowState.Upcoming && row.progress <= 0f
    val labelColor = if (upcoming) colors.faint else colors.ink3
    val titleColor = if (upcoming) colors.faint else colors.ink2
    LineRow(
        modifier = Modifier.padding(horizontal = 20.dp),
        onClick = onClick,
        minHeight = 46.dp,
    ) {
        Text(
            row.label,
            style = AjlTheme.type.jpLabel.copy(fontSize = 14.sp, lineHeight = 18.sp),
            color = labelColor,
            maxLines = 1,
            modifier = Modifier.width(64.dp),
        )
        Text(
            row.title,
            style = AjlTheme.type.jpLabel.copy(fontSize = 16.sp, lineHeight = 22.sp),
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (row.progress > 0f) {
            ProgressLine(row.progress, modifier = Modifier.width(48.dp).padding(start = 12.dp))
        }
        if (row.state == EpisodeRowState.Done) {
            Text(
                "済",
                style = AjlTheme.type.jpLabel.copy(fontSize = 12.sp),
                color = colors.ok,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/** The selected 話 as a manga panel: header, 場面 01–06, one ink button. */
@Composable
private fun EpisodePanel(
    model: CourseScreenModel,
    onScene: (CourseScene) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val course = model.course
    val current = course.current
    val title = model.rows.firstOrNull { it.number == model.selectedEpisode }?.title.orEmpty()
    MangaPanel(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    TextRules.episodeLabel(model.selectedEpisode),
                    style = AjlTheme.type.jpLabel.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
                    color = work.accent,
                    maxLines = 1,
                )
                Text(
                    title,
                    style = AjlTheme.type.jpTitle.copy(fontSize = 20.sp, lineHeight = 26.sp),
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (course.done) "済" else "場面 ${current?.number ?: 1}/6",
                    style = AjlTheme.type.meta,
                    color = if (course.done) colors.ok else colors.ink3,
                )
            }
            Hairline()
            val allEmpty = course.scenes.none { it.startable }
            if (allEmpty) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    LoadingDots()
                }
            } else {
                course.scenes.forEach { scene -> SceneRow(scene, model.materials, onClick = { onScene(scene) }) }
            }
            val primary = current ?: course.scenes.firstOrNull { it.startable && it.kind != SceneKind.Review }
            if (primary != null) {
                InkButton(
                    text = if (current != null) "继续场面 ${primary.number.toString().padStart(2, '0')}" else "再看一遍",
                    onClick = { onScene(primary) },
                    trailingArrow = true,
                    height = 44.dp,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun SceneRow(scene: CourseScene, materials: EpisodeMaterials, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val now = scene.status == SceneStatus.Current
    val quiet = scene.status == SceneStatus.Done || scene.status == SceneStatus.Empty
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 38.dp)
            .background(if (now) work.soft else colors.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = scene.startable,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            scene.number.toString().padStart(2, '0'),
            style = AjlTheme.type.meta,
            color = if (now) work.accent else colors.faint,
            modifier = Modifier.width(20.dp),
        )
        Text(
            CourseModel.sceneName(scene, materials),
            style = AjlTheme.type.body.copy(fontSize = 14.sp, fontWeight = if (now) FontWeight.SemiBold else FontWeight.Normal),
            color = if (quiet) colors.faint else colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            CourseModel.sceneMeta(scene, materials),
            style = AjlTheme.type.meta,
            color = when (scene.status) {
                SceneStatus.Done -> colors.ok
                SceneStatus.Current -> work.accent
                SceneStatus.Empty -> colors.faint
                SceneStatus.Todo -> colors.ink3
            },
            maxLines = 1,
        )
    }
}

/** 題型別 — the always-open drill lab, one quiet row per exercise kind. */
@Composable
private fun DrillSection(drills: List<DrillRow>, onDrill: (DrillRow) -> Unit, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    Column(modifier.fillMaxWidth()) {
        SectionHeading(title = "題型別", gloss = "按题型练")
        drills.forEach { drill ->
            LineRow(onClick = { onDrill(drill) }, minHeight = 44.dp) {
                Text(
                    drill.label,
                    style = AjlTheme.type.body.copy(fontSize = 14.sp),
                    color = colors.ink,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (drill.total > 0) "${drill.covered}/${drill.total}" else "—",
                    style = AjlTheme.type.meta,
                    color = colors.ink3,
                )
            }
        }
    }
}
