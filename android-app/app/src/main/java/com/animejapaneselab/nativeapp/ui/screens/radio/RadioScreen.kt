package com.animejapaneselab.nativeapp.ui.screens.radio

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.PortraitPanel
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.TextTabs
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.VoiceBars
import com.animejapaneselab.nativeapp.ui.design.VoiceWave
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.radio.RadioCatalog
import com.animejapaneselab.nativeapp.ui.radio.RadioMode
import com.animejapaneselab.nativeapp.ui.radio.RadioPlayer
import com.animejapaneselab.nativeapp.ui.radio.RadioSeg
import com.animejapaneselab.nativeapp.ui.radio.RadioShelf
import com.animejapaneselab.nativeapp.ui.radio.RadioState
import com.animejapaneselab.nativeapp.ui.radio.RadioTrack
import com.animejapaneselab.nativeapp.ui.radio.SegKind
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.reading.ReadingLineText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.theme.ProvideWorkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** 知識电台: 節目表 (shelves of tracks) → 再生中 → 午睡 (dark). Playback lives in [RadioPlayer]. */
@Composable
fun RadioScreen(settings: LabSettings, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    remember { RadioPlayer.init(context) }
    val shelves by produceState(RadioCatalog.peek()) { if (value == null) value = withContext(Dispatchers.IO) { RadioCatalog.load(context) } }
    val state by RadioPlayer.state.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var nap by rememberSaveable { mutableStateOf(false) }
    BackHandler(expanded || nap) { if (nap) nap = false else expanded = false }

    ProvideWorkTheme("re-zero") {
        Box(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
            val all = shelves
            if (all == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingDots() }
            } else {
                Programme(all, state, onBack, onOpen = { expanded = true })
            }
            AnimatedVisibility(
                visible = expanded && state.active,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                Playing(state, settings, onCollapse = { expanded = false }, onNap = { nap = true })
            }
            if (nap && state.active) NapScreen(state, onClose = { nap = false })
        }
    }
}

// ---------------------------------------------------------------- 節目表

@Composable
private fun Programme(shelves: List<RadioShelf>, state: RadioState, onBack: () -> Unit, onOpen: () -> Unit) {
    val colors = AjlTheme.colors
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(0) }
    val inTab = shelves.filter { it.word == (tab == 1) }
    var shelfId by rememberSaveable(tab) { mutableStateOf(inTab.getOrNull(if (tab == 0 && inTab.size > 1) 1 else 0)?.id) }
    val shelf = inTab.firstOrNull { it.id == shelfId } ?: inTab.firstOrNull()
    val list = rememberLazyListState()

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "電台", onNav = onBack)
        TextTabs(listOf("知識", "単語"), tab, { tab = it }, Modifier.padding(horizontal = 20.dp))
        Hairline(Modifier.padding(top = 6.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            inTab.forEach { sh ->
                val on = sh.id == shelf?.id
                Box(
                    Modifier
                        .heightIn(min = 36.dp)
                        .background(if (on) colors.ink else Color.Transparent, AjlShape.Panel)
                        .border(1.5.dp, if (on) colors.ink else colors.line2, AjlShape.Panel)
                        .clickable { shelfId = sh.id }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(sh.name, style = AjlTheme.type.label, color = if (on) colors.onInk else colors.ink2)
                }
            }
        }
        if (shelf != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.Bottom) {
                Text(shelf.name, style = AjlTheme.type.jpTitle.copy(fontSize = 17.sp), color = colors.ink, modifier = Modifier.weight(1f))
                Text("${shelf.tracks.size} 回 · ${shelf.tracks.sumOf { it.seconds } / 60} 分", style = AjlTheme.type.meta, color = colors.ink3)
            }
            LazyColumn(
                state = list,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.active) 96.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shelf.tracks, key = { it.id }) { t ->
                    val ids = shelf.tracks.map { it.id }
                    TrackRow(
                        track = t,
                        current = state.trackId == t.id,
                        heard = t.id in state.heard,
                        queued = t.id in state.queue.drop(state.index + 1),
                        onPlay = {
                            if (state.trackId == t.id) onOpen() else {
                                RadioPlayer.play(context, t.id, ids.drop(ids.indexOf(t.id) + 1))
                                onOpen()
                            }
                        },
                        onQueue = { RadioPlayer.enqueue(context, t.id) },
                    )
                }
            }
        }
    }
    if (state.active) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { MiniBar(state, onOpen) }
    }
}

@Composable
private fun TrackRow(track: RadioTrack, current: Boolean, heard: Boolean, queued: Boolean, onPlay: () -> Unit, onQueue: () -> Unit) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    MangaPanel(Modifier.fillMaxWidth().clickable(onClick = onPlay), borderColor = if (current) colors.ink else colors.line2) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(track.no, style = AjlTheme.type.meta, color = if (current) accent else colors.ink3, modifier = Modifier.width(56.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(track.title, style = AjlTheme.type.jpTitle.copy(fontSize = 16.sp), color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (track.gloss.isNotEmpty()) {
                    Text(track.gloss, style = AjlTheme.type.caption, color = colors.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(clock(track.seconds), style = AjlTheme.type.metaSmall, color = colors.ink2)
                    if (track.hand) Text("新稿", style = AjlTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = colors.ink2)
                    if (track.voiceCount > 0) Text("原声 ${track.voiceCount}", style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = accent)
                    val tag = when { current -> "正在听"; queued -> "排队中"; heard -> "听过"; else -> "" }
                    if (tag.isNotEmpty()) Text(tag, style = AjlTheme.type.caption.copy(fontSize = 11.sp), color = colors.faint)
                }
            }
            IconButton44(Icons.Rounded.Add, "加入队列", onQueue, tint = colors.ink2)
        }
    }
}

@Composable
private fun MiniBar(state: RadioState, onOpen: () -> Unit) {
    val colors = AjlTheme.colors
    val track = RadioPlayer.track() ?: return
    Row(
        Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .background(colors.ink, AjlShape.Panel)
            .clickable(onClick = onOpen)
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        com.animejapaneselab.nativeapp.ui.design.Avatar(WorkIdentity.character("エミリア"), size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(
                if (state.recap) "${track.no} · 只念日语" else "${track.no} · ${state.seg + 1}/${track.segs.size}",
                style = AjlTheme.type.metaSmall,
                color = colors.onInk2,
            )
            Text(track.title, style = AjlTheme.type.jpTitle.copy(fontSize = 15.sp), color = colors.onInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        VoiceBars(active = state.playing, color = AjlTheme.work.accent)
        IconButton44(
            if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            if (state.playing) "暂停" else "播放",
            RadioPlayer::toggle,
            tint = colors.onInk,
        )
    }
}

// ---------------------------------------------------------------- 再生中

@Composable
private fun Playing(state: RadioState, settings: LabSettings, onCollapse: () -> Unit, onNap: () -> Unit) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    val track = RadioPlayer.track() ?: return
    val seg = RadioPlayer.seg()
    val now by rememberNow(state.playing)
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton44(Icons.Rounded.KeyboardArrowDown, "收起", onCollapse)
            Text(
                "${track.shelf} · ${track.no}",
                style = AjlTheme.type.meta,
                color = colors.ink3,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.mode == RadioMode.Nap) IconButton44(Icons.Rounded.Bedtime, "午睡画面", onNap) else Spacer(Modifier.size(44.dp))
        }
        var script by rememberSaveable { mutableStateOf(false) }
        TextTabs(listOf("播放", "文案"), if (script) 1 else 0, { script = it == 1 }, Modifier.padding(horizontal = 20.dp))
        Hairline(Modifier.padding(top = 6.dp))
        if (script) {
            ScriptView(track, state, Modifier.weight(1f))
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(12.dp))
                PortraitPanel(WorkIdentity.character("エミリア"), Modifier.fillMaxWidth().height(200.dp), speaking = state.playing)
                Spacer(Modifier.height(14.dp))
                Text(track.title, style = AjlTheme.type.jpTitle, color = colors.ink)
                Spacer(Modifier.height(12.dp))
                if (seg != null) SegCard(seg, settings)
                Spacer(Modifier.height(18.dp))
                ChapterLine(track, state)
                if (state.notice.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(state.notice, style = AjlTheme.type.caption, color = colors.bad)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton44(Icons.Rounded.SkipPrevious, "上一段", RadioPlayer::previous, iconSize = 26.dp)
            MangaPanel(Modifier.weight(1f).height(64.dp)) {
                VoiceWave(
                    playing = state.playing,
                    onClick = RadioPlayer::toggle,
                    modifier = Modifier.fillMaxSize(),
                    synthetic = seg?.kind != SegKind.Orig,
                )
            }
            IconButton44(Icons.Rounded.SkipNext, "下一段", RadioPlayer::next, iconSize = 26.dp)
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.border(1.dp, colors.line2, AjlShape.Tool)) {
                ModeChip("午睡", state.mode == RadioMode.Nap, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)) { RadioPlayer.setMode(RadioMode.Nap) }
                ModeChip("散歩", state.mode == RadioMode.Walk, RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)) { RadioPlayer.setMode(RadioMode.Walk) }
            }
            if (state.mode == RadioMode.Nap) {
                val left = if (state.napEndsAt > 0) clock(((state.napEndsAt - now) / 1000).toInt().coerceAtLeast(0)) else "${state.napMinutes} 分"
                Row(
                    Modifier
                        .heightIn(min = 40.dp)
                        .border(1.dp, colors.line2, AjlShape.Tool)
                        .clickable(onClick = RadioPlayer::cycleNapMinutes)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.Timer, contentDescription = "午睡定时", tint = colors.ink2, modifier = Modifier.size(16.dp))
                    Text(left, style = AjlTheme.type.meta, color = colors.ink2)
                }
            }
            Spacer(Modifier.weight(1f))
            QuietButton("再听这句", RadioPlayer::repeat)
        }
    }
}

@Composable
private fun ModeChip(text: String, on: Boolean, shape: RoundedCornerShape, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    Box(
        Modifier
            .heightIn(min = 40.dp)
            .background(if (on) colors.ink else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AjlTheme.type.label, color = if (on) colors.onInk else colors.ink2)
    }
}

/** The segment now sounding: Japanese with romaji / kana over it, Chinese as plain text. */
@Composable
private fun SegCard(seg: RadioSeg, settings: LabSettings) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    MangaPanel(Modifier.fillMaxWidth().heightIn(min = 120.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val label = when (seg.kind) { SegKind.Orig -> "原声"; SegKind.Ja -> "日本語"; SegKind.Zh -> "エミリア" }
            Text(
                label,
                style = AjlTheme.type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = if (seg.kind == SegKind.Zh) colors.ink3 else accent,
                modifier = Modifier.border(1.dp, if (seg.kind == SegKind.Zh) colors.line2 else accent, RoundedCornerShape(2.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
            )
            if (seg.kind == SegKind.Zh) {
                Text(seg.text, style = AjlTheme.type.body.copy(fontSize = 16.sp, lineHeight = 26.sp), color = colors.ink)
            } else {
                val furigana = rememberFuriganaAnnotator(settings)
                LaunchedEffect(seg.text, settings.showFurigana, settings.showRomaji) {
                    if (settings.showFurigana || settings.showRomaji) furigana.request("sentence", listOf(seg.text))
                }
                val reading = remember(seg.text, furigana.resultFor(seg.text)) { LineReading.build(seg.text, furigana.resultFor(seg.text)) }
                ReadingLineText(
                    reading,
                    mark = null,
                    showRuby = settings.showFurigana,
                    showRomaji = settings.showRomaji,
                    style = AjlTheme.type.jpBody.copy(fontSize = 20.sp, lineHeight = 32.sp),
                )
                if (seg.caption.isNotEmpty()) Text(seg.caption, style = AjlTheme.type.body, color = colors.ink2)
            }
        }
    }
}

/** The whole script of the track: chapter heads, one paragraph per sentence; the part sounding is marked and kept in view. Tap = play from there. */
@Composable
private fun ScriptView(track: RadioTrack, state: RadioState, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    val list = rememberLazyListState()
    val heads = remember(track.id) { track.chapters.toMap() }
    // paragraphs: a sentence cut into z / j pieces is one paragraph; an original line or a captioned Japanese line stands alone
    val paras = remember(track.id) {
        val out = mutableListOf<IntRange>()
        var from = 0
        for (i in 1..track.segs.size) {
            if (i == track.segs.size || !track.segs[i].cont) { out += from until i; from = i }
        }
        out
    }
    val now = if (state.recap) -1 else state.seg
    val at = paras.indexOfFirst { now in it }
    LaunchedEffect(track.id, at) {
        if (at >= 0) list.animateScrollToItem(at, scrollOffset = -160)
    }
    val jpFont = AjlTheme.type.jpBody.fontFamily
    LazyColumn(
        state = list,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 48.dp),
    ) {
        items(paras.size, key = { paras[it].first }) { p ->
            val range = paras[p]
            val first = track.segs[range.first]
            val on = now in range
            Column {
                heads[range.first]?.let { label ->
                    Text(
                        label,
                        style = AjlTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.ink3,
                        modifier = Modifier.padding(top = if (p == 0) 0.dp else 16.dp, bottom = 6.dp),
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clickable { RadioPlayer.seekTo(if (on) now else range.first) }
                        .padding(vertical = 6.dp),
                ) {
                    Box(Modifier.width(3.dp).heightIn(min = 20.dp).fillMaxHeight().background(if (on) accent else Color.Transparent))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (range.first == range.last && first.kind != SegKind.Zh) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (first.kind == SegKind.Orig) {
                                    Text(
                                        "原声",
                                        style = AjlTheme.type.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                        color = accent,
                                        modifier = Modifier.border(1.dp, accent, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp),
                                    )
                                }
                                Text(first.text, style = AjlTheme.type.jpBody.copy(fontSize = 18.sp, lineHeight = 28.sp), color = if (on) accent else colors.ink)
                            }
                            if (first.caption.isNotEmpty()) Text(first.caption, style = AjlTheme.type.caption, color = colors.ink3)
                        } else {
                            val text = buildAnnotatedString {
                                for (i in range) {
                                    val seg = track.segs[i]
                                    val color = when {
                                        i == now -> accent
                                        on || seg.kind != SegKind.Zh -> colors.ink
                                        else -> colors.ink2
                                    }
                                    if (seg.kind == SegKind.Zh) {
                                        withStyle(SpanStyle(color = color)) { append(seg.text) }
                                    } else {
                                        // Japanese inside a Chinese sentence: put back the 「」 the script had
                                        val body = seg.text.trimEnd { it in TRAILING }
                                        withStyle(SpanStyle(color = color, fontFamily = jpFont)) { append("「"); append(body); append("」") }
                                        withStyle(SpanStyle(color = color)) { append(seg.text.substring(body.length)) }
                                    }
                                }
                            }
                            Text(text, style = AjlTheme.type.body.copy(fontSize = 16.sp, lineHeight = 26.sp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterLine(track: RadioTrack, state: RadioState) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    val chapters = track.chapters.ifEmpty { listOf(0 to "") }
    val at = if (state.recap) chapters.lastIndex else track.chapterAt(state.seg)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            chapters.forEachIndexed { i, (start, _) ->
                val end = chapters.getOrNull(i + 1)?.first ?: track.segs.size
                val inside = when {
                    i < at -> 1f
                    i > at -> 0f
                    else -> ((state.seg - start + 1f) / (end - start).coerceAtLeast(1)).coerceIn(0f, 1f)
                }
                Box(Modifier.weight((end - start).coerceAtLeast(1).toFloat()).height(3.dp).background(colors.line)) {
                    Box(Modifier.fillMaxWidth(inside).height(3.dp).background(accent))
                }
            }
        }
        Row {
            Text(
                if (state.recap) "只念日语" else chapters[at].second,
                style = AjlTheme.type.caption,
                color = colors.ink3,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (state.recap) "" else "${state.seg + 1} / ${track.segs.size}",
                style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                color = colors.ink3,
            )
        }
    }
}

// ---------------------------------------------------------------- 午睡

@Composable
private fun NapScreen(state: RadioState, onClose: () -> Unit) {
    val now by rememberNow(true)
    val dark = Color(0xFF141413)
    val dim = Color(0xFF5E5D58)
    val mid = Color(0xFF86857E)
    val accent = AjlTheme.work.accent
    val seg = RadioPlayer.seg()
    val track = RadioPlayer.track()
    val total = state.napMinutes * 60_000f
    val left = if (state.napEndsAt > 0) (state.napEndsAt - now).coerceAtLeast(0) else 0L
    Column(
        Modifier
            .fillMaxSize()
            .background(dark)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                listOfNotNull("午睡", track?.let { "${it.shelf} ${it.no}" }).joinToString(" · "),
                style = AjlTheme.type.meta.copy(fontSize = 12.sp),
                color = mid,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton44(Icons.Rounded.Close, "退出午睡画面", onClose, tint = mid)
        }
        Spacer(Modifier.weight(0.8f))
        Text("あと", style = AjlTheme.type.meta, color = mid)
        Text(clock((left / 1000).toInt()), style = AjlTheme.type.meta.copy(fontSize = 64.sp, lineHeight = 70.sp), color = Color(0xFFA9A8A1))
        Spacer(Modifier.height(48.dp))
        // Volume over the nap: full while she talks, then the Japanese-only murmur, fading out.
        val elapsed = if (state.napEndsAt > 0) 1f - left / total else 0f
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val n = 24
            repeat(n) { i ->
                val x = i / (n - 1f)
                val fadeFrom = 1f - 6 * 60_000f / total
                val h = if (x < fadeFrom) 1f else (1f - (x - fadeFrom) / (1f - fadeFrom)).coerceAtLeast(0.12f)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(h)
                        .background(if (x <= elapsed) accent else Color(0xFF2E2E2A), RoundedCornerShape(1.dp)),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("讲课", style = AjlTheme.type.caption, color = dim, modifier = Modifier.weight(1f))
            Text("只念日语", style = AjlTheme.type.caption, color = dim, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("渐弱 · 停", style = AjlTheme.type.caption, color = dim, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(64.dp))
        if (seg != null && seg.kind != SegKind.Zh) {
            Text(seg.text, style = AjlTheme.type.jpBody.copy(fontSize = 22.sp, lineHeight = 34.sp), color = dim, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
        Text("点一下回到播放", style = AjlTheme.type.caption, color = dim, modifier = Modifier.padding(bottom = 32.dp))
    }
}

@Composable
private fun rememberNow(running: Boolean): androidx.compose.runtime.State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running) {
        while (running) {
            now.longValue = System.currentTimeMillis()
            delay(1000)
        }
    }
    return now
}

private const val TRAILING = "。！？、，：；…!?"

private fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
