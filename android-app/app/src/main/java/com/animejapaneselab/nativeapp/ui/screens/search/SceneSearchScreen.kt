package com.animejapaneselab.nativeapp.ui.screens.search

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.AudioKind
import com.animejapaneselab.nativeapp.data.NotebookEntry
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.data.RagSearchSource
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.SubtitleLine
import com.animejapaneselab.nativeapp.data.promptAudioForSentence
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.audio.LessonAudioController
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.AjlBottomSheet
import com.animejapaneselab.nativeapp.ui.design.EmptyNote
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.ToolPanel
import com.animejapaneselab.nativeapp.ui.design.VoiceWave
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.design.rememberVoicePhase
import com.animejapaneselab.nativeapp.ui.notebook.Notebook
import com.animejapaneselab.nativeapp.ui.notebook.rememberNotebookEntries
import com.animejapaneselab.nativeapp.ui.reading.DeepDiveTarget
import com.animejapaneselab.nativeapp.ui.reading.rememberSentenceDeepDive
import com.animejapaneselab.nativeapp.ui.screens.library.DeepDiveSheet
import com.animejapaneselab.nativeapp.ui.screens.library.parseSpokenLine
import com.animejapaneselab.nativeapp.ui.search.SceneRules
import com.animejapaneselab.nativeapp.ui.search.SceneSearch
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.theme.ProvideWorkTheme
import kotlinx.coroutines.flow.first
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 场景搜索 page (canvas 「原作 · 场景搜索」): the query on top, 「日语里常这么说」 (the AI's own
 * lines for the scene), then 原作里最像的一句 — one manga panel per hit line, every work mixed.
 * When nothing fits it says so, the AI lines lead, and the nearest misses fold away. A panel opens
 * the scene sheet; every play control is a [VoiceWave], never a round ▶ button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SceneSearchScreen(
    uiState: LabUiState,
    onBack: () -> Unit,
    onOpenLine: (workSlug: String, episode: Int, lineNo: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    val context = LocalContext.current
    val state by SceneSearch.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var query by rememberSaveable { mutableStateOf(state.query) }
    val audio = rememberLessonAudioController()
    var playingKey by remember { mutableStateOf<String?>(null) }
    val sounding = audio.playbackState.phase == AudioPlaybackPhase.Loading || audio.playbackState.phase == AudioPlaybackPhase.Playing
    LaunchedEffect(sounding) { if (!sounding) playingKey = null }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var foldOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.focusId, state.result) {
        val id = state.focusId ?: return@LaunchedEffect
        if (state.result?.sources?.any { it.id == id } == true) {
            openId = id
            SceneSearch.focus(null)
        }
    }
    LaunchedEffect(Unit) { if (state.query.isBlank()) runCatching { focusRequester.requestFocus() } }

    fun submit() {
        focusManager.clearFocus()
        foldOpen = false
        SceneSearch.search(context, query)
    }
    fun speak(key: String, text: String) {
        if (playingKey == key && sounding) {
            audio.stop()
        } else {
            playingKey = key
            audio.speakText(text, uiState.settings.ttsWorkerUrl)
        }
    }
    fun playLine(key: String, source: RagSearchSource, line: SubtitleLine) {
        if (playingKey == key && sounding) {
            audio.stop()
        } else {
            playingKey = key
            playSubtitleLine(audio, source, line, uiState.settings.ttsWorkerUrl)
        }
    }

    Column(modifier.fillMaxSize().background(colors.bg)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton44(Icons.AutoMirrored.Rounded.ArrowBack, "返回", onBack)
            ToolPanel(Modifier.weight(1f).height(42.dp)) {
                Row(
                    Modifier.fillMaxSize().padding(start = 12.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.Search, null, tint = colors.ink3, modifier = Modifier.size(16.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text("描述一个场景，或输入台词", style = AjlTheme.type.body.copy(fontSize = 15.sp), color = colors.faint, maxLines = 1)
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = AjlTheme.type.jpBody.copy(fontSize = 16.sp, color = colors.ink),
                            cursorBrush = SolidColor(colors.ink),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { submit() }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).semantics { contentDescription = "按场景找台词" },
                        )
                    }
                    if (query.isNotEmpty()) IconButton44(Icons.Rounded.Close, "清空", { query = "" }, tint = colors.ink3, iconSize = 16.dp)
                }
            }
        }

        val result = state.result
        val shown = remember(result) { result?.let(SceneRules::shown).orEmpty() }
        val nearest = remember(result) { result?.let(SceneRules::nearest).orEmpty() }
        val weak = result != null && (result.weak || shown.isEmpty())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { LoadingDots(delayMillis = 0) }
                }
                state.error != null -> item(key = "error") {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error.orEmpty(), style = AjlTheme.type.body, color = colors.ink2)
                        QuietButton("重新搜索", onClick = { SceneSearch.retry(context) }, color = AjlTheme.work.accent)
                    }
                }
                result == null -> item(key = "idle") { EmptyNote("描述一个场景", gloss = "比如：傲娇地拒绝别人") }
                else -> {
                    if (result.examples.isNotEmpty()) {
                        item(key = "ex-head") { SectionHead("日语里常这么说", tag = "AI 例句") }
                        if (weak) {
                            items(result.examples, key = { "ex-${it.ja}" }) { ex ->
                                ExampleRow(ex.ja, ex.zh, playing = playingKey == "ex-${ex.ja}" && sounding, onPlay = { speak("ex-${ex.ja}", ex.ja) })
                            }
                        } else {
                            item(key = "ex-chips") {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    result.examples.forEach { ex ->
                                        ExampleChip(ex.ja, playing = playingKey == "ex-${ex.ja}" && sounding, onClick = { speak("ex-${ex.ja}", ex.ja) })
                                    }
                                }
                            }
                        }
                    }
                    if (!weak) {
                        item(key = "hits-head") { SectionHead("原作里最像的一句", count = shown.size) }
                        items(shown, key = { "hit-${it.id}" }) { source ->
                            val line = SceneRules.hitLine(source) ?: return@items
                            ProvideWorkTheme(source.workSlug) {
                                HitPanel(
                                    source = source,
                                    line = line,
                                    previous = SceneRules.previous(source),
                                    playing = playingKey == "hit-${source.id}" && sounding,
                                    onPlay = { playLine("hit-${source.id}", source, line) },
                                    onOpen = { openId = source.id },
                                )
                            }
                        }
                    } else {
                        item(key = "weak") {
                            Row(
                                Modifier.fillMaxWidth().border(AjlStroke.Hair, colors.line2, AjlShape.Tool).padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("原作里没有很像的场景", style = AjlTheme.type.body, color = colors.ink)
                            }
                        }
                        if (nearest.isNotEmpty()) {
                            item(key = "fold") {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp)
                                        .clickable(role = Role.Button, onClickLabel = if (foldOpen) "收起" else "展开") { foldOpen = !foldOpen },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text("最接近的 ${nearest.size} 句", style = AjlTheme.type.caption, color = colors.ink2)
                                    Icon(if (foldOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = colors.ink2, modifier = Modifier.size(16.dp))
                                }
                            }
                            if (foldOpen) {
                                items(nearest, key = { "near-${it.id}" }) { source ->
                                    val line = SceneRules.hitLine(source) ?: return@items
                                    NearRow(source, line, onOpen = { openId = source.id })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val open = state.result?.sources?.firstOrNull { it.id == openId }
    if (open != null) {
        ProvideWorkTheme(open.workSlug) {
            SceneSheet(
                uiState = uiState,
                source = open,
                audio = audio,
                onOpenLine = { lineNo ->
                    openId = null
                    onOpenLine(open.workSlug, open.episode, lineNo)
                },
                onDismiss = { openId = null },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------- parts

@Composable
private fun SectionHead(text: String, tag: String? = null, count: Int? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, style = AjlTheme.type.label.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = AjlTheme.colors.ink)
        if (tag != null) {
            Text(
                tag,
                style = AjlTheme.type.metaSmall,
                color = AjlTheme.colors.info,
                modifier = Modifier.background(AjlTheme.colors.infoSoft, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        if (count != null) {
            Spacer(Modifier.weight(1f))
            Text("$count 句", style = AjlTheme.type.meta, color = AjlTheme.colors.ink3)
        }
    }
}

@Composable
private fun ExampleChip(ja: String, playing: Boolean, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    Row(
        Modifier
            .heightIn(min = 40.dp)
            .background(if (playing) AjlTheme.work.soft else colors.surface, AjlShape.Tool)
            .border(AjlStroke.Hair, if (playing) AjlTheme.work.accent else colors.line2, AjlShape.Tool)
            .clickable(role = Role.Button, onClickLabel = "朗读", onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(ja, style = AjlTheme.type.jpBody.copy(fontSize = 15.sp), color = colors.ink)
    }
}

@Composable
private fun ExampleRow(ja: String, zh: String, playing: Boolean, onPlay: () -> Unit) {
    val colors = AjlTheme.colors
    Row(
        Modifier.fillMaxWidth().border(width = 0.dp, color = colors.line).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(ja, style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, lineHeight = 27.sp), color = colors.ink)
            if (zh.isNotBlank()) Text(zh, style = AjlTheme.type.caption, color = colors.ink2)
        }
        VoiceWave(playing = playing, onClick = onPlay, synthetic = true)
    }
}

/** One hit: where it is from, the line before (faint), the line with its key words marked, the AI's Chinese and why. */
@Composable
private fun HitPanel(
    source: RagSearchSource,
    line: SubtitleLine,
    previous: SubtitleLine?,
    playing: Boolean,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    val match = source.match
    MangaPanel(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClickLabel = "打开这个场景", onClick = onOpen),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sourceLabel(source, line), style = AjlTheme.type.meta, color = accent, modifier = Modifier.weight(1f))
                VoiceWave(playing = playing, onClick = onPlay, synthetic = !line.hasSourceAudio)
            }
            if (previous != null) {
                Text(parseSpokenLine(previous.jaText).text, style = AjlTheme.type.jpBody.copy(fontSize = 13.sp, lineHeight = 20.sp), color = colors.ink3, maxLines = 1)
            }
            Text(
                marked(parseSpokenLine(line.jaText).text, match?.mark.orEmpty(), accent),
                style = AjlTheme.type.jpTitle.copy(fontSize = 21.sp, lineHeight = 30.sp),
                color = colors.ink,
                modifier = Modifier.padding(end = 8.dp),
            )
            if (!match?.zh.isNullOrBlank()) Text(match?.zh.orEmpty(), style = AjlTheme.type.body.copy(fontSize = 15.sp), color = colors.ink2, modifier = Modifier.padding(end = 8.dp))
            if (!match?.why.isNullOrBlank()) Text(match?.why.orEmpty(), style = AjlTheme.type.caption, color = colors.ink2, modifier = Modifier.padding(end = 8.dp))
        }
    }
}

@Composable
private fun NearRow(source: RagSearchSource, line: SubtitleLine, onOpen: () -> Unit) {
    val colors = AjlTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "打开这个场景", onClick = onOpen)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(sourceLabel(source, line), style = AjlTheme.type.meta, color = colors.ink3)
        Text(parseSpokenLine(line.jaText).text, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp), color = colors.ink2)
        source.match?.zh?.takeIf { it.isNotBlank() }?.let { Text(it, style = AjlTheme.type.caption, color = colors.ink3) }
    }
}

// ---------------------------------------------------------------------------------- scene sheet

/**
 * 点开一个场景: the hit with two lines either side as a script timeline, the hit in a manga panel
 * under a 「この一言」 name plate, then the scene's waveform — one segment per line, tap it and the
 * lines play in order (原声 where there is a clip, synthesis otherwise), the sounding segment bounces.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SceneSheet(
    uiState: LabUiState,
    source: RagSearchSource,
    audio: LessonAudioController,
    onOpenLine: (lineNo: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    val context = LocalContext.current
    val lines = remember(source) { SceneRules.window(source) }
    val hit = remember(source) { SceneRules.hitLine(source) } ?: return
    val notebook = rememberNotebookEntries()
    val deepDive = rememberSentenceDeepDive(uiState.settings)
    val entry = remember(source, hit) {
        NotebookEntry(
            key = NotebookRules.key(NotebookKind.Line, "${source.workSlug}-${source.episode}-${hit.lineNo}"),
            kind = NotebookKind.Line,
            headline = parseSpokenLine(hit.jaText).text,
            meaning = source.match?.zh.orEmpty(),
            workSlug = source.workSlug,
            episode = source.episode,
            lineNo = hit.lineNo,
            audioUrl = hit.audioUrl,
            storagePath = hit.storagePath,
        )
    }
    val saved = notebook.any { it.key == entry.key }
    // Sequential playback of the whole scene; -1 = stopped. A single tapped line plays alone.
    var playingIndex by remember { mutableIntStateOf(-1) }
    var sequence by remember { mutableStateOf(false) }
    LaunchedEffect(playingIndex, sequence) {
        if (playingIndex < 0) return@LaunchedEffect
        playSubtitleLine(audio, source, lines[playingIndex], uiState.settings.ttsWorkerUrl)
        snapshotFlow { audio.playbackState.phase }.first { it == AudioPlaybackPhase.Loading || it == AudioPlaybackPhase.Playing }
        snapshotFlow { audio.playbackState.phase }.first { it == AudioPlaybackPhase.Idle || it == AudioPlaybackPhase.Error }
        playingIndex = if (sequence && playingIndex + 1 < lines.size) playingIndex + 1 else -1
    }

    AjlBottomSheet(onDismissRequest = {
        audio.stop()
        onDismiss()
    }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("第${source.episode}話", style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = colors.ink)
                Text(
                    "${WorkIdentity.displayName(source.workSlug, source.workSlug)} · ${SceneRules.clock(hit.startTime)}",
                    style = AjlTheme.type.meta,
                    color = colors.ink3,
                    modifier = Modifier.weight(1f),
                )
                IconButton44(
                    if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    if (saved) "取消收藏" else "收藏这一句",
                    { Notebook.toggle(context, entry) },
                    tint = if (saved) accent else colors.ink,
                )
            }

            Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                lines.forEachIndexed { index, line ->
                    val isHit = line.lineNo == hit.lineNo
                    val last = index == lines.lastIndex
                    if (isHit) {
                        HitPlate(source, line, accent, lineAbove = index > 0, lineBelow = !last)
                    } else {
                        TimelineRow(
                            text = parseSpokenLine(line.jaText).text,
                            sounding = playingIndex == index,
                            lineAbove = index > 0,
                            lineBelow = !last,
                            onClick = {
                                sequence = false
                                playingIndex = if (playingIndex == index) -1 else index
                                if (playingIndex < 0) audio.stop()
                            },
                        )
                    }
                }
            }

            SceneWaveform(
                segments = lines.size,
                hitIndex = lines.indexOfFirst { it.lineNo == hit.lineNo },
                playingIndex = playingIndex,
                originalCount = lines.count { it.hasSourceAudio },
                onClick = {
                    if (playingIndex >= 0) {
                        playingIndex = -1
                        audio.stop()
                    } else {
                        sequence = true
                        playingIndex = 0
                    }
                },
            )

            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton("精读", onClick = {
                    deepDive.request(
                        DeepDiveTarget(
                            workSlug = source.workSlug,
                            episode = source.episode,
                            lineNo = hit.lineNo,
                            jaText = hit.jaText,
                            zhText = source.match?.zh.orEmpty(),
                        ),
                    )
                }, modifier = Modifier.height(52.dp))
                InkButton("在原作里接着看", onClick = {
                    audio.stop()
                    onOpenLine(hit.lineNo)
                }, modifier = Modifier.weight(1f))
            }
        }
    }
    DeepDiveSheet(deepDive)
}

@Composable
private fun TimelineRow(text: String, sounding: Boolean, lineAbove: Boolean, lineBelow: Boolean, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClickLabel = "朗读这一句", onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.width(11.dp).heightIn(min = 44.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val x = size.width / 2
                val line = colors.line2
                if (lineAbove) drawLine(line, Offset(x, 0f), Offset(x, 16.dp.toPx()), strokeWidth = 1.dp.toPx())
                if (lineBelow) drawLine(line, Offset(x, 16.dp.toPx()), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                drawCircle(if (sounding) accent else colors.surface, radius = 3.5.dp.toPx(), center = Offset(x, 16.dp.toPx()))
                drawCircle(if (sounding) accent else colors.faint, radius = 3.5.dp.toPx(), center = Offset(x, 16.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            }
        }
        Text(
            text,
            style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 24.sp),
            color = if (sounding) colors.ink else colors.ink3,
            modifier = Modifier.weight(1f).padding(top = 4.dp, bottom = 10.dp),
        )
    }
}

/** The hit in the timeline: a big work-colour dot, a manga panel with a 3dp ink shadow and a 「この一言」 plate. */
@Composable
private fun HitPlate(source: RagSearchSource, line: SubtitleLine, accent: androidx.compose.ui.graphics.Color, lineAbove: Boolean, lineBelow: Boolean) {
    val colors = AjlTheme.colors
    val match = source.match
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.width(11.dp).heightIn(min = 120.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val x = size.width / 2
                if (lineAbove) drawLine(colors.line2, Offset(x, 0f), Offset(x, 34.dp.toPx()), strokeWidth = 1.dp.toPx())
                if (lineBelow) drawLine(colors.line2, Offset(x, 34.dp.toPx()), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                drawCircle(accent, radius = 5.5.dp.toPx(), center = Offset(x, 34.dp.toPx()))
            }
        }
        Box(Modifier.weight(1f).padding(top = 12.dp, bottom = 18.dp, end = 3.dp)) {
            MangaPanel(Modifier.fillMaxWidth(), shadow = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        marked(parseSpokenLine(line.jaText).text, match?.mark.orEmpty(), accent),
                        style = AjlTheme.type.jpTitle.copy(fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
                        color = colors.ink,
                    )
                    if (!match?.zh.isNullOrBlank()) Text(match?.zh.orEmpty(), style = AjlTheme.type.body.copy(fontSize = 15.sp), color = colors.ink2)
                    if (!match?.why.isNullOrBlank()) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                        Text(match?.why.orEmpty(), style = AjlTheme.type.caption, color = colors.ink2)
                    }
                }
            }
            Text(
                "この一言",
                style = AjlTheme.type.jpLabel.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                color = colors.bg,
                modifier = Modifier
                    .offset(x = 12.dp, y = (-12).dp)
                    .background(colors.ink)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

/** One segment of bars per line; the hit's segment is work colour, the sounding one bounces. The waveform is the play control. */
@Composable
private fun SceneWaveform(segments: Int, hitIndex: Int, playingIndex: Int, originalCount: Int, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    val phase by rememberVoicePhase(playingIndex >= 0, periodMillis = 900)
    val heights = remember(segments) { List(segments) { s -> List(5 + (s * 3) % 3) { b -> 0.3f + ((s * 7 + b * 5) % 10) / 14f } } }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .clickable(role = Role.Button, onClickLabel = if (playingIndex >= 0) "停止" else "播放这一段", onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Canvas(Modifier.fillMaxWidth().height(40.dp).padding(top = 8.dp)) {
            val gapSeg = 8.dp.toPx()
            val totalBars = heights.sumOf { it.size }
            val barGap = 2.dp.toPx()
            val usable = size.width - gapSeg * (segments - 1)
            val barW = ((usable - barGap * (totalBars - segments)) / totalBars).coerceAtLeast(1f)
            var x = 0f
            heights.forEachIndexed { s, bars ->
                val color = when {
                    s == playingIndex -> accent
                    s == hitIndex -> accent.copy(alpha = 0.55f)
                    else -> colors.line2
                }
                bars.forEachIndexed { b, r ->
                    val live = if (s == playingIndex) 0.35f + 0.65f * abs(sin(2 * PI * (phase + b / bars.size.toFloat()))).toFloat() else r
                    val h = size.height * live.coerceIn(0.15f, 1f)
                    drawRoundRect(color, Offset(x, (size.height - h) / 2), Size(barW, h), CornerRadius(barW / 2))
                    x += barW + barGap
                }
                x += gapSeg - barGap
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (originalCount > 0) "原声 · $segments 句" else "朗读 · $segments 句",
                style = AjlTheme.type.meta,
                color = colors.ink3,
            )
        }
    }
}

// --------------------------------------------------------------------------------------- helpers

private fun sourceLabel(source: RagSearchSource, line: SubtitleLine): String =
    "${WorkIdentity.displayName(source.workSlug, source.workSlug)} 第${source.episode}話 · ${SceneRules.clock(line.startTime)}"

/** The key words of the line in work colour, underlined. */
private fun marked(text: String, mark: String, accent: androidx.compose.ui.graphics.Color): AnnotatedString {
    if (mark.isBlank() || !text.contains(mark)) return AnnotatedString(text)
    val at = text.indexOf(mark)
    return buildAnnotatedString {
        append(text.substring(0, at))
        withStyle(SpanStyle(color = accent, textDecoration = TextDecoration.Underline)) { append(mark) }
        append(text.substring(at + mark.length))
    }
}

/** 原声 when the worker found a clip for the line, otherwise synthesis of the spoken text. */
private fun playSubtitleLine(audio: LessonAudioController, source: RagSearchSource, line: SubtitleLine, ttsWorkerUrl: String) {
    val text = parseSpokenLine(line.jaText).text
    if (line.hasSourceAudio) {
        val sentence = ShadowingSentence(
            id = "${source.workSlug}-${source.episode}-${line.lineNo}",
            ja = text,
            reading = "",
            meaningZh = "",
            sourceLabel = "",
            audioKind = AudioKind.Source,
            sourceLineNo = line.lineNo,
            audioUrl = line.audioUrl,
            storagePath = line.storagePath,
        )
        audio.play(promptAudioForSentence(source.workSlug, sentence, autoPlay = false), ttsWorkerUrl)
    } else {
        audio.speakText(text, ttsWorkerUrl)
    }
}
