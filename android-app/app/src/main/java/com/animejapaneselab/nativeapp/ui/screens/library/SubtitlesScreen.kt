package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.SubtitleLine
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.Avatar
import com.animejapaneselab.nativeapp.ui.design.EmptyNote
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.TextRules
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.reading.DeepDiveTarget
import com.animejapaneselab.nativeapp.ui.reading.RubyText
import com.animejapaneselab.nativeapp.ui.reading.rememberCharacterProfile
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.reading.rememberSentenceDeepDive
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/** Subtitle browser (secondary screen): speaker avatars, scene groups, focus-line jump. */
@Composable
fun SubtitlesScreen(
    uiState: LabUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onFocusConsumed: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val workSlug = uiState.selection.workSlug
    val episode = uiState.selection.episode
    val lines = uiState.subtitles
    var query by rememberSaveable(workSlug, episode) { mutableStateOf("") }
    var collapsed by remember(workSlug, episode) { mutableStateOf(emptySet<Int>()) }
    var selectedKey by rememberSaveable(workSlug, episode) { mutableStateOf<String?>(null) }
    var furiganaOn by rememberSaveable { mutableStateOf(false) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }

    val audio = rememberLessonAudioController()
    val deepDive = rememberSentenceDeepDive(uiState.settings)
    val characterProfile = rememberCharacterProfile(uiState.settings, workSlug)
    val annotator = rememberFuriganaAnnotator(uiState.settings)
    val reduced = rememberReducedMotion()
    val listState = rememberLazyListState()
    val focusLeadPx = with(LocalDensity.current) { 96.dp.toPx() }

    val scenes = remember(lines) { splitSubtitleScenes(lines) }
    val visible = remember(lines, query) { filterSubtitles(lines, query) }
    val grouped = query.isBlank() && scenes.size > 1
    val showNotice = uiState.subtitleStatus == SyncStatus.Loading || uiState.subtitleStatus == SyncStatus.Error
    val rows = remember(lines, visible, scenes, grouped, collapsed, showNotice) {
        buildSubtitleRows(lines.isNotEmpty(), visible, scenes, grouped, collapsed, showNotice)
    }
    // Lines whose speaker repeats the previous line in the same scene drop their avatar.
    val continued = remember(scenes) {
        buildSet {
            scenes.forEach { scene ->
                var previous: String? = null
                scene.lines.forEach { line ->
                    val speaker = parseSpokenLine(line.jaText).speaker
                    if (speaker != null && speaker == previous) add(SubtitleRow.Line(line).key)
                    previous = speaker
                }
            }
        }
    }
    val selectedLine = remember(lines, selectedKey) { lines.firstOrNull { SubtitleRow.Line(it).key == selectedKey } }

    LaunchedEffect(furiganaOn, rows) {
        if (!furiganaOn) return@LaunchedEffect
        annotator.request("subtitle", rows.mapNotNull { (it as? SubtitleRow.Line)?.line?.let { line -> parseSpokenLine(line.jaText).text } })
    }

    // Focus jump (search result / review card → this line): clear find, open its scene, scroll.
    LaunchedEffect(uiState.subtitleFocusLineNo, lines) {
        val target = uiState.subtitleFocusLineNo ?: return@LaunchedEffect
        if (lines.isEmpty()) return@LaunchedEffect
        val line = lines.firstOrNull { it.lineNo == target }
        if (line == null) {
            onFocusConsumed()
            return@LaunchedEffect
        }
        query = ""
        val scene = scenes.firstOrNull { s -> s.lines.any { it.lineNo == target } }
        val nextCollapsed = if (scene != null) collapsed - scene.number else collapsed
        collapsed = nextCollapsed
        selectedKey = SubtitleRow.Line(line).key
        val jumpGrouped = scenes.size > 1
        val jumpRows = buildSubtitleRows(true, lines, scenes, jumpGrouped, nextCollapsed, showNotice)
        val index = jumpRows.indexOfFirst { it is SubtitleRow.Line && it.line.lineNo == target }
        withFrameNanos { }
        if (index >= 0) {
            if (reduced) {
                listState.scrollToItem(index)
                listState.scrollBy(-focusLeadPx)
            } else {
                listState.animateScrollToItem(index)
                listState.animateScrollBy(-focusLeadPx)
            }
        }
        onFocusConsumed()
    }

    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(
            nav = TopBarNav.Back,
            onNav = onBack,
            center = {
                Column(Modifier.weight(1f)) {
                    Text("字幕", style = AjlTheme.type.jpTitle.copy(fontSize = 17.sp, lineHeight = 22.sp), color = colors.ink)
                    val meta = listOfNotNull(
                        workTitle(uiState).takeIf { it.isNotBlank() },
                        lines.size.takeIf { it > 0 }?.let { "$it 行" },
                        scenes.size.takeIf { it > 1 }?.let { "$it 場面" },
                    ).joinToString(" · ")
                    if (meta.isNotEmpty()) Text(meta, style = AjlTheme.type.metaSmall, color = colors.ink3, maxLines = 1)
                }
            },
            actions = {
                EpisodeChip(episodeTitle(episode), onClick = { pickerOpen = true })
                IconButton44(Icons.Rounded.Search, "搜索字幕", onOpenSearch)
            },
        )
        Hairline()
        Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = if (selectedLine != null) 96.dp else 28.dp),
            ) {
                items(rows, key = { it.key }, contentType = { it.contentType }) { row ->
                    when (row) {
                        SubtitleRow.Tools -> SubtitleTools(
                            query = query,
                            onQueryChange = { query = it },
                            furiganaAvailable = uiState.settings.showFurigana,
                            furiganaOn = furiganaOn,
                            onToggleFurigana = { furiganaOn = !furiganaOn },
                        )
                        SubtitleRow.Notice -> SubtitleNotice(uiState, onRefresh)
                        SubtitleRow.Empty -> if (uiState.subtitleStatus != SyncStatus.Loading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                EmptyNote("この話の字幕はまだない")
                                OutlineButton("再読み込み", onRefresh, compact = true)
                            }
                        }
                        SubtitleRow.NoMatch -> EmptyNote("見つからない", gloss = query)
                        is SubtitleRow.SceneHead -> SceneHead(
                            scene = row.scene,
                            open = row.scene.number !in collapsed,
                            onToggle = {
                                collapsed = if (row.scene.number in collapsed) collapsed - row.scene.number else collapsed + row.scene.number
                            },
                        )
                        is SubtitleRow.Line -> {
                            val spoken = remember(row.line.jaText) { parseSpokenLine(row.line.jaText) }
                            SubtitleLineRow(
                                line = row.line,
                                speaker = spoken.speaker,
                                text = spoken.text,
                                showSpeaker = row.key !in continued,
                                selected = row.key == selectedKey,
                                furigana = if (furiganaOn) annotator.resultFor(spoken.text) else null,
                                onClick = { selectedKey = if (selectedKey == row.key) null else row.key },
                                onAvatar = {
                                    characterOptionFor(workSlug, spoken.speaker)?.let { option ->
                                        characterProfile.open()
                                        characterProfile.select(option)
                                    }
                                },
                            )
                        }
                    }
                }
            }
            if (selectedLine != null) {
                val spoken = remember(selectedLine.jaText) { parseSpokenLine(selectedLine.jaText) }
                PlayerDock(
                    text = spoken.text,
                    meta = listOfNotNull(clockLabel(selectedLine.startTime).takeIf { it.isNotBlank() }, spoken.speaker).joinToString(" · "),
                    loading = audio.playbackState.phase == AudioPlaybackPhase.Loading,
                    onPlay = { audio.speakText(spoken.text, uiState.settings.ttsWorkerUrl) },
                    onDeepDive = {
                        deepDive.request(
                            DeepDiveTarget(
                                workSlug = workSlug,
                                episode = episode,
                                lineNo = selectedLine.lineNo,
                                jaText = selectedLine.jaText,
                                zhText = selectedLine.zhText,
                            ),
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
                )
            }
        }
    }

    if (pickerOpen) {
        EpisodePickerSheet(
            works = uiState.works,
            episodes = uiState.episodes,
            selectedWork = workSlug,
            selectedEpisode = episode,
            onWorkSelected = onWorkSelected,
            onEpisodeSelected = {
                pickerOpen = false
                onEpisodeSelected(it)
            },
            onDismiss = { pickerOpen = false },
        )
    }
    DeepDiveSheet(deepDive)
    CharacterSheet(characterProfile)
}

@Composable
private fun SubtitleTools(
    query: String,
    onQueryChange: (String) -> Unit,
    furiganaAvailable: Boolean,
    furiganaOn: Boolean,
    onToggleFurigana: () -> Unit,
) {
    val colors = AjlTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FindField(query, onQueryChange, placeholder = "引く · 台词、中文", modifier = Modifier.weight(1f))
        if (furiganaAvailable) {
            val shape = RoundedCornerShape(6.dp)
            Box(
                Modifier
                    .height(40.dp)
                    .background(if (furiganaOn) colors.ink else colors.surface, shape)
                    .border(AjlStroke.Hair, if (furiganaOn) colors.ink else colors.line2, shape)
                    .clickableNoRipple(onToggleFurigana)
                    .semantics { contentDescription = if (furiganaOn) "关闭注音" else "显示注音" }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("ふりがな", style = AjlTheme.type.jpLabel.copy(fontSize = 12.sp), color = if (furiganaOn) colors.onInk else colors.ink)
            }
        }
    }
}

@Composable
private fun SubtitleNotice(uiState: LabUiState, onRefresh: () -> Unit) {
    val colors = AjlTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (uiState.subtitleStatus == SyncStatus.Loading) {
            LoadingDots()
        } else {
            Text(uiState.subtitleMessage, style = AjlTheme.type.caption, color = colors.bad, modifier = Modifier.weight(1f))
            OutlineButton("再読み込み", onRefresh, compact = true)
        }
    }
}

/** 場面 07 · speaker faces · 12:10 – 12:48. Tap folds the scene. */
@Composable
private fun SceneHead(scene: SubtitleScene, open: Boolean, onToggle: () -> Unit) {
    val colors = AjlTheme.colors
    val speakers = remember(scene) {
        scene.lines.mapNotNull { parseSpokenLine(it.jaText).speaker }.distinct().take(4)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickableNoRipple(onToggle)
            .padding(start = 10.dp, end = 6.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            TextRules.sceneLabel(scene.number),
            style = AjlTheme.type.label.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = colors.ink,
        )
        if (speakers.isNotEmpty()) {
            Box(Modifier.width((18 + (speakers.size - 1) * 12).dp).height(18.dp)) {
                speakers.forEachIndexed { index, name ->
                    Avatar(WorkIdentity.character(name), size = 18.dp, modifier = Modifier.offset(x = (index * 12).dp))
                }
            }
        }
        if (!open) Text("${scene.lines.size} 行", style = AjlTheme.type.metaSmall, color = colors.ink3)
        Spacer(Modifier.weight(1f))
        scene.timeRangeLabel?.let { Text(it, style = AjlTheme.type.metaSmall, color = colors.ink3) }
        Chevron(open, if (open) "收起场面" else "展开场面")
    }
}

@Composable
private fun SubtitleLineRow(
    line: SubtitleLine,
    speaker: String?,
    text: String,
    showSpeaker: Boolean,
    selected: Boolean,
    furigana: com.animejapaneselab.nativeapp.data.FuriganaResult?,
    onClick: () -> Unit,
    onAvatar: () -> Unit,
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AjlTheme.work.soft else colors.bg)
            .clickableNoRipple(onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.width(44.dp).padding(top = 3.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (speaker != null && showSpeaker) {
                Avatar(WorkIdentity.character(speaker), size = 32.dp, modifier = Modifier.clickableNoRipple(onAvatar))
            }
            Text(clockLabel(line.startTime), style = type.metaSmall, color = colors.ink3, maxLines = 1)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (speaker != null && showSpeaker) Text(speaker, style = type.caption, color = colors.ink2)
            RubyText(
                text = text,
                furigana = furigana,
                style = type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp),
                rubyStyle = type.metaSmall.copy(fontSize = 9.sp),
                color = colors.ink,
                rubyColor = colors.ink3,
            )
            if (line.zhText.isNotBlank()) {
                Text(line.zhText, style = type.caption.copy(fontSize = 13.sp, lineHeight = 19.sp), color = colors.ink3)
            }
        }
    }
}

/** Floating tool dock for the selected line: ink play (the screen's one primary), text, 精読. */
@Composable
private fun PlayerDock(
    text: String,
    meta: String,
    loading: Boolean,
    onPlay: () -> Unit,
    onDeepDive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(colors.surface, shape)
            .border(AjlStroke.Hair, colors.line2, shape)
            .padding(start = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.ink)
                .clickableNoRipple(onPlay)
                .semantics { contentDescription = "播放这一行" },
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                LoadingDots(delayMillis = 0, color = colors.onInk, dotSize = 4.dp)
            } else {
                Icon(Icons.Rounded.PlayArrow, null, tint = colors.onInk, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text,
                style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotBlank()) Text(meta, style = AjlTheme.type.metaSmall, color = colors.ink3, maxLines = 1)
        }
        OutlineButton("精読", onDeepDive, compact = true)
    }
}
