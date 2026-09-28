package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.NotebookKind
import com.animejapaneselab.nativeapp.data.NotebookRules
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.toNotebookEntry
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.AjlBottomSheet
import com.animejapaneselab.nativeapp.ui.design.Avatar
import com.animejapaneselab.nativeapp.ui.design.EmptyNote
import com.animejapaneselab.nativeapp.ui.design.FilterPill
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.SwipeAction
import com.animejapaneselab.nativeapp.ui.design.SwipeActionRow
import com.animejapaneselab.nativeapp.ui.design.UndoBar
import com.animejapaneselab.nativeapp.ui.design.VoiceBars
import com.animejapaneselab.nativeapp.ui.design.VoiceSwitchPill
import com.animejapaneselab.nativeapp.ui.design.WordTile
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.notebook.Notebook
import com.animejapaneselab.nativeapp.ui.notebook.NotebookMark
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.reading.ReadingLineText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.words.KnownWords
import kotlinx.coroutines.delay

/** What the 遮る pills hide in the 台词 list. */
private enum class LineMask(val label: String) { None("都显示"), Zh("遮中文"), Ja("遮日文") }

/**
 * 台词 tab: the episode's lines like a script. Tap the line = hear it (原声 when there is one),
 * tap anywhere else on the row = 台词卡, swipe right = 斩, swipe left = 收藏. 遮る hides the
 * Chinese (or the Japanese) until the row is tapped, for listening first.
 */
@Composable
internal fun LinesPage(
    key: String,
    uiState: LabUiState,
    savedKeys: Set<String>,
    playingId: String?,
    onPlay: (ShadowingSentence, tts: Boolean) -> Unit,
    onSpeak: (String) -> Unit,
    onDeepDive: (ShadowingSentence) -> Unit,
    onAsk: (ShadowingSentence) -> Unit,
    onLearn: (ShadowingSentence) -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    var query by rememberSaveable(key) { mutableStateOf("") }
    var mask by rememberSaveable(key) { mutableStateOf(LineMask.None) }
    val revealed = remember(key, mask) { mutableStateListOf<String>() }
    var archive by rememberSaveable(key) { mutableStateOf(false) }
    var openId by rememberSaveable(key) { mutableStateOf<String?>(null) }
    var undo by remember(key) { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    remember { KnownWords.init(appContext) }
    val known by KnownWords.words.collectAsState()
    val all = uiState.shadowing
    val cut = remember(all, known) { all.filter { KnownWords.lineKey(it.id) in known } }
    val lines = remember(all, known, archive) { if (archive) cut else all.filterNot { KnownWords.lineKey(it.id) in known } }
    val filtered = remember(lines, query) { lines.filter { it.matches(query) } }
    LaunchedEffect(cut.isEmpty()) { if (cut.isEmpty()) archive = false }
    LaunchedEffect(undo) {
        if (undo != null) {
            delay(4000)
            undo = null
        }
    }
    val workSlug = uiState.selection.workSlug
    val episode = uiState.selection.episode
    val store = remember(appContext) { LocalLabStore(appContext) }
    var tts by remember { mutableStateOf(store.readJishuVoiceTts()) }
    fun toggleSave(line: ShadowingSentence) {
        val entry = line.toNotebookEntry(workSlug, episode)
        val wasSaved = NotebookRules.key(NotebookKind.Line, line.id) in savedKeys
        Notebook.toggle(appContext, entry)
        undo = (if (wasSaved) "已取消收藏这句" else "已收藏这句") to { Notebook.toggle(appContext, entry) }
    }
    fun cutLine(line: ShadowingSentence) {
        val k = KnownWords.lineKey(line.id)
        if (archive) {
            KnownWords.restoreKey(appContext, k)
        } else {
            KnownWords.cutKey(appContext, k, NotebookRules.key(NotebookKind.Line, line.id))
            undo = "已斩这句" to { KnownWords.restoreKey(appContext, k) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            item(key = "tools", contentType = "tools") {
                Column(Modifier.padding(top = 12.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FindField(query, { query = it }, placeholder = "引く · 台词、中文")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LineMask.entries.forEach { m -> FilterPill(m.label, mask == m, { mask = m }) }
                        if (cut.isNotEmpty()) FilterPill("已斩 ${cut.size}", archive, { archive = !archive })
                    }
                }
            }
            when {
                all.isEmpty() -> item(key = "empty") { EmptyNote("この話の台詞はまだない") }
                lines.isEmpty() -> item(key = "empty") { EmptyNote("全部斩了", gloss = "斩掉的在「已斩」里") }
                filtered.isEmpty() -> item(key = "empty") { EmptyNote("見つからない", gloss = query) }
            }
            items(filtered.size, key = { "s-${filtered[it].id}" }, contentType = { "line" }) { i ->
                val line = filtered[i]
                val open = line.id in revealed
                LineRow(
                    line = line,
                    saved = NotebookRules.key(NotebookKind.Line, line.id) in savedKeys,
                    archive = archive,
                    playing = playingId == line.id,
                    hideZh = mask == LineMask.Zh && !open,
                    hideJa = mask == LineMask.Ja && !open,
                    onReveal = { revealed += line.id },
                    onPlay = { onPlay(line, tts) },
                    onOpen = { openId = line.id },
                    onCut = { cutLine(line) },
                    onToggleSave = { toggleSave(line) },
                )
            }
        }
        undo?.let { (text, restore) ->
            UndoBar(
                text = text,
                onUndo = {
                    restore()
                    undo = null
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    openId?.let { id ->
        if (filtered.none { it.id == id }) {
            openId = null
        } else {
            LineCardSheet(
                lines = filtered,
                openId = id,
                uiState = uiState,
                savedKeys = savedKeys,
                known = archive,
                playing = playingId == id,
                tts = tts,
                onVoice = {
                    tts = it
                    store.writeJishuVoiceTts(it)
                },
                onMove = { openId = it },
                onPlay = { onPlay(it, tts) },
                onSpeak = onSpeak,
                onToggleSave = { toggleSave(it) },
                onCut = {
                    openId = null
                    cutLine(it)
                },
                onAsk = onAsk,
                onDeepDive = onDeepDive,
                onLearn = {
                    openId = null
                    onLearn(it)
                },
                onDismiss = { openId = null },
            )
        }
    }
}

@Composable
private fun LineRow(
    line: ShadowingSentence,
    saved: Boolean,
    archive: Boolean,
    playing: Boolean,
    hideZh: Boolean,
    hideJa: Boolean,
    onReveal: () -> Unit,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    onCut: () -> Unit,
    onToggleSave: () -> Unit,
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val work = AjlTheme.work
    val spoken = remember(line.ja) { parseSpokenLine(line.ja) }
    Column(Modifier.fillMaxWidth()) {
        SwipeActionRow(
            enabled = !archive,
            right = SwipeAction(
                label = "斩",
                armedLabel = "松手斩掉",
                background = colors.ink,
                content = colors.onInk,
                dismiss = true,
                icon = { armed -> SealMark(armed = armed, tint = colors.onInk, fill = colors.onInk, size = 36.dp) },
                onCommit = onCut,
            ),
            left = SwipeAction(
                label = if (saved) "取消收藏" else "收藏",
                armedLabel = if (saved) "松手取消" else "松手收藏",
                background = work.accent,
                content = work.onAccent,
                dismiss = false,
                icon = { armed ->
                    Icon(
                        if (armed != saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                        tint = work.onAccent,
                        modifier = Modifier.size(28.dp),
                    )
                },
                onCommit = onToggleSave,
            ),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colors.bg)
                    .clickableNoRipple(onClick = onOpen)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.width(38.dp).padding(top = 2.dp)) {
                    if (spoken.speaker != null) Avatar(WorkIdentity.character(spoken.speaker), size = 38.dp, highlighted = playing)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(spoken.speaker.orEmpty(), style = type.caption.copy(fontSize = 12.sp), color = colors.ink3)
                        if (playing) VoiceBars(active = true, color = work.accent)
                        Box(Modifier.weight(1f))
                        if (saved) NotebookMark()
                        if (line.hasSourceAudio) Text("原声", style = type.metaSmall, color = colors.ink3)
                    }
                    if (hideJa) {
                        ToneBar("点一下看日文 · 先听一遍", onClick = onReveal, height = 30)
                    } else {
                        Text(
                            spoken.text,
                            style = type.jpBody.copy(fontSize = 18.sp, lineHeight = 28.sp),
                            color = colors.ink,
                            modifier = Modifier.clickableNoRipple(onClick = onPlay).semantics { contentDescription = "${spoken.text}，点一下播放" },
                        )
                    }
                    if (line.meaningZh.isNotBlank()) {
                        if (hideZh) {
                            ToneBar("点一下看中文", onClick = onReveal, height = 26)
                        } else {
                            Text(line.meaningZh, style = type.body.copy(fontSize = 14.sp, lineHeight = 21.sp), color = colors.ink2)
                        }
                    }
                }
            }
        }
        Hairline()
    }
}

/** A screentone strip standing in for hidden text; tap to show it. */
@Composable
private fun ToneBar(label: String, onClick: () -> Unit, height: Int) {
    val colors = AjlTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.sunken)
            .screentone(colors.line2, spacing = 5.dp, dotRadius = 1.dp)
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
    }
}

/**
 * 台词卡: the line large with romaji and kana over every word (tap = play), the translation, the
 * 原声 / TTS pill, the line split into chunks to hear one at a time, and the words in it.
 * Swipe the top half for the previous / next line.
 */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LineCardSheet(
    lines: List<ShadowingSentence>,
    openId: String,
    uiState: LabUiState,
    savedKeys: Set<String>,
    known: Boolean,
    playing: Boolean,
    tts: Boolean,
    onVoice: (Boolean) -> Unit,
    onMove: (String) -> Unit,
    onPlay: (ShadowingSentence) -> Unit,
    onSpeak: (String) -> Unit,
    onToggleSave: (ShadowingSentence) -> Unit,
    onCut: (ShadowingSentence) -> Unit,
    onAsk: (ShadowingSentence) -> Unit,
    onDeepDive: (ShadowingSentence) -> Unit,
    onLearn: (ShadowingSentence) -> Unit,
    onDismiss: () -> Unit,
) {
    val index = lines.indexOfFirst { it.id == openId }.coerceAtLeast(0)
    val line = lines[index]
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val work = AjlTheme.work
    val settings = uiState.settings
    val spoken = remember(line.ja) { parseSpokenLine(line.ja) }
    val furigana = rememberFuriganaAnnotator(settings)
    LaunchedEffect(spoken.text) { furigana.request("sentence", listOf(spoken.text)) }
    val reading = remember(spoken.text, furigana.resultFor(spoken.text)) { LineReading.build(spoken.text, furigana.resultFor(spoken.text)) }
    val enrichment = line.enrichment
    val chunks = enrichment?.chunks.orEmpty().filter { it.isNotBlank() }.takeIf { it.size > 1 }.orEmpty()
    val words = enrichment?.breakdown.orEmpty().filter { it.ja.isNotBlank() }
    val saved = NotebookRules.key(NotebookKind.Line, line.id) in savedKeys

    AjlBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CardHeader(
                level = null,
                meta = listOfNotNull(spoken.speaker, enrichment?.toneZh?.takeIf { it.isNotBlank() }, "${index + 1} / ${lines.size}").joinToString(" · "),
                saved = saved,
                known = known,
                onToggleSave = { onToggleSave(line) },
                onCut = { onCut(line) },
                leading = { if (spoken.speaker != null) Avatar(WorkIdentity.character(spoken.speaker), size = 32.dp, highlighted = playing) },
            )
            SwipeStage(onStep = { step -> onMove(lines[(index + step + lines.size) % lines.size].id) }) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).clickableNoRipple(onClick = { onPlay(line) }).semantics { contentDescription = "播放这句" }) {
                        ReadingLineText(
                            reading,
                            null,
                            showRuby = settings.showFurigana,
                            showRomaji = settings.showRomaji,
                            style = type.jpBody.copy(fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                    VoiceBars(active = playing, color = work.accent)
                }
                val zh = enrichment?.naturalZh?.takeIf { it.isNotBlank() } ?: line.meaningZh
                if (zh.isNotBlank()) Text(zh, style = type.title.copy(fontSize = 19.sp, lineHeight = 28.sp), color = colors.ink)
            }
            VoiceSwitchPill(
                playing = playing,
                options = listOf("原声", "TTS"),
                selected = if (tts) 1 else 0,
                onSelect = { onVoice(it == 1) },
                onClick = { onPlay(line) },
            )
            if (chunks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("一块一块听", style = type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = work.accent)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        chunks.forEach { chunk -> WordTile(chunk, onClick = { onSpeak(chunk) }) }
                    }
                }
            }
            if (words.isNotEmpty()) {
                Column {
                    words.forEach { part ->
                        Hairline()
                        Row(
                            Modifier.fillMaxWidth().clickableNoRipple(onClick = { onSpeak(part.ja) }).padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(part.ja, style = type.jpBody.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = colors.ink)
                            Column(Modifier.weight(1f)) {
                                if (part.zh.isNotBlank()) Text(part.zh, style = type.body.copy(fontSize = 14.sp), color = colors.ink2)
                                if (part.noteZh.isNotBlank()) Text(part.noteZh, style = type.caption.copy(fontSize = 12.sp), color = colors.ink3)
                            }
                        }
                    }
                }
            }
            LibraryAiNote(line.aiKey(), uiState)
            if (lines.size > 1) {
                Text("‹ 左右滑换上一句 / 下一句 ›", style = type.metaSmall, color = colors.ink3, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton("讲解", { onAsk(line) }, modifier = Modifier.height(52.dp))
                if (words.isEmpty()) OutlineButton("精读", { onDeepDive(line) }, modifier = Modifier.height(52.dp))
                InkButton("跟读这句", { onLearn(line) }, modifier = Modifier.weight(1f))
            }
        }
    }
}
