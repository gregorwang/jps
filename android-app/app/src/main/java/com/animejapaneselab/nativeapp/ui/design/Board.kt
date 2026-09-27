package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/** 五段 rows in 行×段 order, each with its 辞书形 ending (う段). ワ行 is the 買う row: わ い う え お. */
private val GodanRows = listOf(
    Triple("カ", "く", listOf("か", "き", "く", "け", "こ")),
    Triple("ガ", "ぐ", listOf("が", "ぎ", "ぐ", "げ", "ご")),
    Triple("サ", "す", listOf("さ", "し", "す", "せ", "そ")),
    Triple("タ", "つ", listOf("た", "ち", "つ", "て", "と")),
    Triple("ナ", "ぬ", listOf("な", "に", "ぬ", "ね", "の")),
    Triple("バ", "ぶ", listOf("ば", "び", "ぶ", "べ", "ぼ")),
    Triple("マ", "む", listOf("ま", "み", "む", "め", "も")),
    Triple("ラ", "る", listOf("ら", "り", "る", "れ", "ろ")),
    Triple("ワ", "う", listOf("わ", "い", "う", "え", "お")),
)

private val Dans = listOf("a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お")

/**
 * 板書 行×段 grid for 五段 verbs. The [dan] column (a/i/u/e/o) takes the work colour; rows
 * whose 辞书形 ending is in [endings] stay ink while the others fade, so 音便 lessons show
 * which rows they cover. Draws nothing when both are empty.
 */
@Composable
fun DanGrid(dan: String, endings: List<String>, modifier: Modifier = Modifier) {
    if (dan.isBlank() && endings.isEmpty()) return
    val column = Dans.indexOfFirst { it.first == dan }
    MangaPanel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 6.dp)) {
            GridRow(
                label = "",
                cells = Dans.map { it.second + "段" },
                highlight = column,
                active = true,
                header = true,
            )
            Hairline()
            GodanRows.forEach { (row, ending, kana) ->
                val active = endings.isEmpty() || ending in endings
                GridRow(label = "${row}行", cells = kana, highlight = column, active = active, header = false)
            }
        }
    }
}

@Composable
private fun GridRow(label: String, cells: List<String>, highlight: Int, active: Boolean, header: Boolean) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Row(Modifier.fillMaxWidth().height(if (header) 30.dp else 26.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = AjlTheme.type.meta.copy(fontSize = 11.sp),
            color = if (active) colors.ink2 else colors.faint,
            modifier = Modifier.width(44.dp).padding(start = 12.dp),
        )
        cells.forEachIndexed { i, cell ->
            val on = i == highlight
            Box(
                Modifier
                    .weight(1f)
                    .height(if (header) 30.dp else 26.dp)
                    .background(if (on && active) work.soft else colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    cell,
                    style = if (header) AjlTheme.type.meta.copy(fontSize = 11.sp) else AjlTheme.type.jpBody.copy(fontSize = 15.sp, lineHeight = 20.sp),
                    fontWeight = if (on && active) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        !active -> colors.faint
                        on -> work.accent
                        header -> colors.ink3
                        else -> colors.ink
                    },
                )
            }
        }
    }
}

/**
 * One 板書 derivation: 書く → 書か → 書かない in serif with work-colour arrows, the result in
 * bold, its Chinese meaning underneath.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DerivationLine(steps: List<String>, gloss: String, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
            steps.forEachIndexed { i, step ->
                if (i > 0) {
                    Text("→", style = AjlTheme.type.meta.copy(fontSize = 14.sp), color = AjlTheme.work.accent, modifier = Modifier.align(Alignment.CenterVertically))
                }
                Text(
                    step,
                    style = AjlTheme.type.jpBody.copy(fontSize = 19.sp, lineHeight = 28.sp),
                    fontWeight = if (i == steps.lastIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (i == steps.lastIndex) colors.ink else colors.ink2,
                )
            }
        }
        if (gloss.isNotBlank()) Text(gloss, style = AjlTheme.type.caption, color = colors.ink3)
    }
}
