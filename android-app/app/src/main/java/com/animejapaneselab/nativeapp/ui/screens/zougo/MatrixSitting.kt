package com.animejapaneselab.nativeapp.ui.screens.zougo

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.zougo.ZgCell
import com.animejapaneselab.nativeapp.ui.zougo.ZgLesson
import com.animejapaneselab.nativeapp.ui.zougo.Zougo
import com.animejapaneselab.nativeapp.ui.zougo.ZougoRules
import com.animejapaneselab.nativeapp.ui.zougo.ZougoState

/**
 * 组合矩阵: front parts down the side, back parts across the top. A header shows what that part
 * adds (〜込む = 进去 · 深入) and lights its line; a cell opens the word below — how it splits, what
 * it means, a line. Once every word has been opened, the 小テスト asks the meanings ([onTest]).
 */
@Composable
internal fun MatrixSitting(
    lesson: ZgLesson,
    settings: LabSettings,
    onClose: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    val m = lesson.matrix ?: return
    val context = LocalContext.current
    val audio = rememberLessonAudioController()
    val state by Zougo.state.collectAsState()
    val first = m.words.firstOrNull()
    var focusRow by rememberSaveable(lesson.id) { mutableStateOf(m.focusRow) }
    var row by rememberSaveable(lesson.id) { mutableIntStateOf(first?.first ?: 0) }
    var col by rememberSaveable(lesson.id) { mutableIntStateOf(first?.second ?: 0) }
    val total = m.words.size
    val seen = state.seenIn(lesson)

    fun open(r: Int, c: Int) {
        row = r; col = c
        audio.stop()
        if (m.cells[r][c] is ZgCell.Word) {
            Zougo.see(context, lesson.id, r, c)
            if (settings.autoSpeak) audio.speakText((m.cells[r][c] as ZgCell.Word).word, settings.ttsWorkerUrl)
        }
    }
    // The first word counts as opened as soon as the 課 opens.
    androidx.compose.runtime.LaunchedEffect(lesson.id) { first?.let { (r, c, _) -> Zougo.see(context, lesson.id, r, c) } }

    val head = if (focusRow) m.rows[row] else m.cols[col]
    Column(modifier.fillMaxSize().background(AjlTheme.colors.bg)) {
        ZougoHeader(
            eyebrow = "第四巻 造語 · 第 ${lesson.number} 課",
            title = lesson.gloss.ifBlank { lesson.title },
            counter = "$seen / $total",
            progress = if (total == 0) 0f else seen.toFloat() / total,
            onClose = onClose,
        )
        Column(Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // what the focused part adds
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (focusRow) "${head.text}〜" else "〜${head.text}",
                    style = AjlTheme.type.jpDisplay.copy(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
                    color = AjlTheme.work.accent,
                )
                Text(if (focusRow) "${head.romaji}-" else "-${head.romaji}", style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = AjlTheme.colors.ink3)
                Spacer(Modifier.weight(1f))
                head.core.forEach { core ->
                    Text(
                        core,
                        style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 26.sp),
                        color = AjlTheme.colors.ink,
                        maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(13.dp)).background(AjlTheme.colors.sunken).padding(horizontal = 10.dp),
                    )
                }
            }
            Grid(
                lesson = lesson,
                state = state,
                row = row,
                col = col,
                focusRow = focusRow,
                onCol = { c -> focusRow = false; open(m.words.firstOrNull { it.second == c }?.first ?: row, c) },
                onRow = { r -> focusRow = true; open(r, m.words.firstOrNull { it.first == r }?.second ?: col) },
                onCell = ::open,
            )
            Detail(lesson, row, col, audio, settings.ttsWorkerUrl, Modifier.weight(1f))
        }
        val left = total - seen
        InkButton(
            text = "小テスト · ${minOf(ZougoRules.LessonQuizSize, total)} 题",
            onClick = { audio.stop(); onTest() },
            enabled = left <= 0,
            caption = if (left > 0) "还有 $left 个词没点开" else null,
            trailingArrow = left <= 0,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun Grid(
    lesson: ZgLesson,
    state: ZougoState,
    row: Int,
    col: Int,
    focusRow: Boolean,
    onCol: (Int) -> Unit,
    onRow: (Int) -> Unit,
    onCell: (Int, Int) -> Unit,
) {
    val m = lesson.matrix ?: return
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GridRow {
            Spacer(Modifier.width(62.dp))
            m.cols.forEachIndexed { c, h ->
                val on = !focusRow && c == col
                HeadButton(h.text, h.romaji, on, Modifier.weight(1f).height(44.dp), underline = true) { onCol(c) }
            }
        }
        m.rows.forEachIndexed { r, h ->
            GridRow {
                HeadButton(h.text, h.romaji, focusRow && r == row, Modifier.width(62.dp).height(48.dp), underline = false, start = true) { onRow(r) }
                m.cells[r].forEachIndexed { c, cell ->
                    val selected = r == row && c == col
                    val inLine = if (focusRow) r == row else c == col
                    val opened = ZougoState.cellKey(lesson.id, r, c) in state.seen
                    val shape = RoundedCornerShape(4.dp)
                    val base = Modifier.weight(1f).height(48.dp).clip(shape)
                        .semantics { role = Role.Button; this.selected = selected; contentDescription = h.text + m.cols[c].text }
                        .clickableNoRipple({ onCell(r, c) })
                    when (cell) {
                        is ZgCell.Word -> {
                            val fill by animateColorAsState(if (selected) work.accent else colors.surface, tween(200), label = "cell")
                            Box(
                                base
                                    .background(fill)
                                    .border(if (inLine || selected) AjlStroke.Ink else AjlStroke.Hair, if (selected) work.accent else if (inLine) colors.ink else colors.line2, shape)
                                    .drawBehind {
                                        if (opened && !selected) drawRect(work.tone(0.30f), topLeft = Offset(0f, size.height - 3.dp.toPx()))
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    cell.word,
                                    style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = if (selected || inLine) FontWeight.Bold else FontWeight.Normal),
                                    color = if (selected) work.onAccent else if (inLine) colors.ink else colors.ink2,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        ZgCell.Rare -> Box(
                            base.drawBehind {
                                val w = (if (selected) 1.5f else 1f).dp.toPx()
                                drawRoundRect(
                                    if (selected) work.accent else colors.line2,
                                    topLeft = Offset(w / 2, w / 2),
                                    size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                                    cornerRadius = CornerRadius(4.dp.toPx()),
                                    style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                                )
                            },
                            contentAlignment = Alignment.Center,
                        ) { Text("少", style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = if (selected) work.accent else colors.ink3) }
                        ZgCell.None -> Box(
                            base
                                .background(colors.sunken)
                                .screentone(colors.line2, spacing = 6.dp, dotRadius = 1.dp)
                                .then(if (selected) Modifier.border(AjlStroke.Ink, colors.ink3, shape) else Modifier),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridRow(content: @Composable RowScope.() -> Unit) =
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, content = content)

@Composable
private fun HeadButton(text: String, romaji: String, on: Boolean, modifier: Modifier, underline: Boolean, start: Boolean = false, onClick: () -> Unit) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val c = if (on) work.accent else if (underline) colors.ink2 else colors.ink
    Column(
        modifier
            .clickableNoRipple(onClick)
            .semantics { role = Role.Tab; selected = on }
            .drawBehind {
                if (underline) drawRect(if (on) work.accent else colors.line, topLeft = Offset(0f, size.height - 2.dp.toPx()))
                else if (on) drawRect(work.accent, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
            }
            .padding(start = if (start) 8.dp else 0.dp),
        horizontalAlignment = if (start) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, style = AjlTheme.type.jpTitle.copy(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold), color = c, maxLines = 1)
        Text(romaji, style = AjlTheme.type.meta.copy(fontSize = 10.sp, lineHeight = 13.sp), color = if (on) work.accent else colors.ink3, maxLines = 1)
    }
}

@Composable
private fun Detail(lesson: ZgLesson, row: Int, col: Int, audio: com.animejapaneselab.nativeapp.ui.audio.LessonAudioController, ttsWorkerUrl: String, modifier: Modifier) {
    val m = lesson.matrix ?: return
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    MangaPanel(modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = row to col,
            transitionSpec = { (fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 12 }).togetherWith(fadeOut(tween(100))) },
            label = "matrix-detail",
        ) { (r, c) ->
            val cell = m.cells[r][c]
            val pair = m.rows[r].text + m.cols[c].text
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (cell) {
                    is ZgCell.Word -> {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WordHead(cell.romaji, cell.kana, cell.word, onSpeak = { audio.speakText(cell.word, ttsWorkerUrl) })
                            Spacer(Modifier.weight(1f))
                            Text(cell.meaning, style = AjlTheme.type.body.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, textAlign = TextAlign.End, modifier = Modifier.padding(bottom = 6.dp).weight(1f, fill = false))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PieceChip(m.rows[r].text, cell.front, colors.line2, colors.ink)
                            Text("+", style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = colors.ink3)
                            PieceChip(m.cols[c].text, cell.back, work.accent, work.accent)
                        }
                        Divider()
                        LineCard(cell.line, audio, ttsWorkerUrl)
                    }
                    ZgCell.Rare -> {
                        Text(pair, style = AjlTheme.type.jpDisplay.copy(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold), color = colors.ink2)
                        Text("词典里有，平时很少说。", style = AjlTheme.type.body, color = colors.ink2)
                    }
                    ZgCell.None -> {
                        Text(pair, style = AjlTheme.type.jpDisplay.copy(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.LineThrough), color = colors.ink3)
                        Text("不成词。", style = AjlTheme.type.body, color = colors.ink2)
                    }
                }
            }
        }
    }
}

@Composable
private fun PieceChip(part: String, gloss: String, border: androidx.compose.ui.graphics.Color, partColor: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.border(AjlStroke.Hair, border, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(part, style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, lineHeight = 18.sp), color = partColor)
        Text(gloss, style = AjlTheme.type.caption, color = AjlTheme.colors.ink3)
    }
}
