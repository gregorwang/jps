package com.animejapaneselab.nativeapp.ui.screens.katsuyou

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.ui.audio.rememberLessonAudioController
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.katsuyou.Katsuyou
import com.animejapaneselab.nativeapp.ui.katsuyou.KyCell
import com.animejapaneselab.nativeapp.ui.katsuyou.KyTable
import com.animejapaneselab.nativeapp.ui.screens.zougo.LineCard
import com.animejapaneselab.nativeapp.ui.screens.zougo.segColor
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.zougo.SegKind

/**
 * まとめ as a table: a header row, rows in sections (a rail when the order matters), cells as kana pieces /
 * text / ✓△✗. Tabs pick a column (A 段, E 条件), a tapped row shows its lines (C, H), 遮る hides columns (F, G).
 */
@Composable
internal fun KyTableScreen(table: KyTable, eyebrow: String, settings: LabSettings, modifier: Modifier = Modifier, onBack: () -> Unit = Katsuyou::exit) {
    BackHandler(onBack = onBack)
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val audio = rememberLessonAudioController()
    var selCol by rememberSaveable { mutableIntStateOf(0) }
    var selRow by rememberSaveable { mutableStateOf("") }
    var cover by rememberSaveable { mutableStateOf(false) }
    val shown = remember { mutableStateListOf<String>() }
    val byCol = table.select == "col"
    val cellCols = table.cols.size

    Column(modifier.fillMaxSize().background(colors.bg)) {
        TopBar(
            nav = TopBarNav.Back,
            onNav = onBack,
            center = {
                Column(Modifier.weight(1f)) {
                    Text(eyebrow, style = AjlTheme.type.meta.copy(fontSize = 11.sp), color = colors.ink3, maxLines = 1)
                    Text(table.title, style = AjlTheme.type.jpTitle.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = colors.ink)
                }
            },
            actions = {
                if (table.cover.isNotEmpty()) {
                    Text(
                        "遮る",
                        style = AjlTheme.type.body.copy(fontSize = 13.sp),
                        color = if (cover) colors.bg else colors.ink2,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(if (cover) colors.ink else Color.Transparent, RoundedCornerShape(12.dp))
                            .border(AjlStroke.Hair, if (cover) colors.ink else colors.line2, RoundedCornerShape(12.dp))
                            .clickableNoRipple({ cover = !cover; shown.clear() })
                            .semantics { role = Role.Button; contentDescription = "遮住答案" }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            },
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (byCol) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    table.cols.forEachIndexed { k, (t, ro) ->
                        val on = k == selCol
                        Column(
                            Modifier
                                .heightIn(min = 44.dp)
                                .background(if (on) colors.ink else Color.Transparent, RoundedCornerShape(12.dp))
                                .border(AjlStroke.Hair, if (on) colors.ink else colors.line2, RoundedCornerShape(12.dp))
                                .clickableNoRipple({ selCol = k })
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            if (ro.isNotBlank()) Text(ro, style = AjlTheme.type.meta.copy(fontSize = 9.sp), color = if (on) colors.bg else colors.ink3)
                            Text(t, style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = if (on) colors.bg else colors.ink)
                        }
                    }
                }
            }
            MangaPanel(Modifier.fillMaxWidth()) {
                Column {
                    // header
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
                        Spacer(Modifier.width(LabelWidth))
                        table.cols.forEachIndexed { k, (t, ro) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (ro.isNotBlank()) Text(ro, style = AjlTheme.type.meta.copy(fontSize = 9.sp), color = colors.ink3)
                                Text(
                                    t,
                                    style = AjlTheme.type.caption.copy(fontSize = 12.sp, fontWeight = if (byCol && k == selCol) FontWeight.Bold else FontWeight.Normal),
                                    color = if (byCol && k == selCol) work.accent else colors.ink3,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(AjlStroke.Ink).background(colors.ink))
                    table.sections.forEachIndexed { si, sec ->
                        if (sec.title.isNotBlank()) {
                            Text(sec.title, style = AjlTheme.type.caption.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = colors.ink2,
                                modifier = Modifier.fillMaxWidth().background(colors.sunken).padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                        sec.rows.forEachIndexed { ri, row ->
                            val id = "$si-$ri"
                            val rowOn = table.select == "row" && selRow == id
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .background(if (rowOn) work.tone(0.18f) else Color.Transparent)
                                    .then(if (table.select == "row") Modifier.clickableNoRipple({ selRow = if (rowOn) "" else id }) else Modifier)
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(Modifier.width(LabelWidth).heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (sec.rail) {
                                        Box(Modifier.width(14.dp).heightIn(min = 52.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                            if (ri < sec.rows.lastIndex) Box(Modifier.width(1.5.dp).height(52.dp).padding(top = 26.dp).background(colors.ink))
                                            Box(Modifier.size(9.dp).background(if (ri == 0) colors.ink else colors.surface, CircleShape).border(AjlStroke.Ink, colors.ink, CircleShape))
                                        }
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Column {
                                        Text(row.label, style = AjlTheme.type.jpBody.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                                        if (row.labelSub.isNotBlank()) Text(row.labelSub, style = AjlTheme.type.caption.copy(fontSize = 10.sp), color = colors.ink3)
                                    }
                                }
                                row.cells.forEachIndexed { ci, cell ->
                                    val key = "$id-$ci"
                                    val hidden = cover && ci in table.cover && key !in shown
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .heightIn(min = 44.dp)
                                            .padding(2.dp)
                                            .background(
                                                when {
                                                    hidden -> colors.sunken
                                                    byCol && ci == selCol -> work.tone(0.18f)
                                                    else -> Color.Transparent
                                                },
                                                RoundedCornerShape(2.dp),
                                            )
                                            .then(if (hidden) Modifier.clickableNoRipple({ shown.add(key) }) else Modifier),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (!hidden) TableCell(cell, dim = byCol && ci != selCol && cellCols > 2)
                                    }
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(AjlStroke.Hair).background(colors.line))
                        }
                    }
                }
            }
            if (byCol) {
                table.colNotes.getOrNull(selCol)?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = AjlTheme.type.jpBody.copy(fontSize = 15.sp), color = colors.ink)
                }
                table.colLines.getOrNull(selCol)?.let { LineCard(it, audio, settings.ttsWorkerUrl) }
            }
            if (table.select == "row") {
                val parts = selRow.split('-').mapNotNull { it.toIntOrNull() }
                val row = parts.takeIf { it.size == 2 }?.let { table.sections.getOrNull(it[0])?.rows?.getOrNull(it[1]) }
                if (row == null) {
                    Text("点一行，听原作里的说法", style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
                } else {
                    row.lines.forEach { LineCard(it, audio, settings.ttsWorkerUrl) }
                    if (row.lines.isEmpty()) Text("这一行原作里没有现成的台词", style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
                }
            }
            table.notes.forEach { (label, text, note) ->
                Row(
                    Modifier.fillMaxWidth().border(AjlStroke.Hair, colors.line2, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(label, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
                    Column(Modifier.weight(1f)) {
                        Text(text, style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = colors.ink)
                        if (note.isNotBlank()) Text(note, style = AjlTheme.type.caption.copy(fontSize = 12.sp), color = colors.ink3)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private val LabelWidth = 84.dp

@Composable
private fun TableCell(cell: KyCell, dim: Boolean) {
    val colors = AjlTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            cell.mark.isNotBlank() -> {
                val (t, c) = when (cell.mark) {
                    "yes" -> "✓" to colors.ok
                    "ok" -> "△" to colors.info
                    else -> "✗" to colors.bad
                }
                Text(t, style = AjlTheme.type.body.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold), color = c)
            }
            cell.segs.isNotEmpty() -> Text(
                buildAnnotatedString {
                    cell.segs.forEach { s ->
                        val sc = segColor(s.kind)
                        val c = if (sc == Color.Unspecified || s.kind == SegKind.Already) (if (dim) colors.ink3 else colors.ink) else sc
                        withStyle(SpanStyle(color = c)) { append(s.text) }
                    }
                },
                style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
            else -> Text(cell.text, style = AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold), color = if (dim) colors.ink3 else colors.ink, textAlign = TextAlign.Center)
        }
        if (cell.sub.isNotBlank()) Text(cell.sub, style = AjlTheme.type.caption.copy(fontSize = 10.sp, lineHeight = 13.sp), color = colors.ink3, textAlign = TextAlign.Center)
    }
}
