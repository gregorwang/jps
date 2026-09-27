package com.animejapaneselab.nativeapp.ui.screens.jishu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.drill.ConjugationDrillRules
import com.animejapaneselab.nativeapp.ui.jishu.parseFormula
import com.animejapaneselab.nativeapp.ui.reading.Kana
import com.animejapaneselab.nativeapp.ui.reading.WordReading
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/** Groups whose grammar point is the stem's own form (活用形 / 音便 / 形容词), not what follows it. */
private val StemGroups = setOf("A", "B", "F")

/**
 * 拆解 as word blocks joined by ＋: each word large in serif over a rule, its notes underneath; the
 * block carrying the grammar point (the stem for 活用形 groups, what follows it otherwise) in the
 * work colour. Falls back to the plain formula when it does not parse.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormulaRow(
    formula: String,
    group: String,
    modifier: Modifier = Modifier,
    wordSize: TextUnit = 24.sp,
    readings: ((List<String>) -> List<WordReading?>)? = null,
    showRuby: Boolean = false,
    showRomaji: Boolean = false,
) {
    val parts = remember(formula) { parseFormula(formula) }
    val sounds = remember(parts, readings) { readings?.invoke(parts.map { it.word }).orEmpty() }
    if (parts.isEmpty()) {
        Text(formula, modifier = modifier, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 26.sp), color = AjlTheme.colors.ink)
        return
    }
    val stemPoint = ConjugationDrillRules.groupKey(group) in StemGroups
    val rubyRow = showRuby && parts.indices.any { sounds.getOrNull(it) != null && Kana.hasKanji(parts[it].word) }
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        parts.forEachIndexed { i, part ->
            if (i > 0) Text("＋", style = AjlTheme.type.meta.copy(fontSize = 18.sp), color = AjlTheme.work.accent, modifier = Modifier.padding(top = if (rubyRow) 20.dp else 6.dp))
            FormulaBlock(
                part.word,
                part.note,
                accent = if (stemPoint) i == 0 else i > 0,
                wordSize = wordSize,
                ruby = if (rubyRow) sounds.getOrNull(i)?.kana?.takeIf { Kana.hasKanji(part.word) }.orEmpty() else null,
                romaji = sounds.getOrNull(i)?.romaji?.takeIf { showRomaji },
            )
        }
    }
}

@Composable
private fun FormulaBlock(word: String, note: String, accent: Boolean, wordSize: TextUnit, ruby: String?, romaji: String?) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val notes = note.split(" · ").filter { it.isNotBlank() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.width(IntrinsicSize.Max)) {
            // Kana over a kanji word; an empty line keeps blocks aligned when only some have it.
            if (ruby != null) {
                Text(ruby.ifEmpty { " " }, style = AjlTheme.type.jpBody.copy(fontSize = 11.sp, lineHeight = 14.sp), color = if (accent) work.accent else colors.ink3, maxLines = 1)
            }
            Text(
                word,
                style = AjlTheme.type.jpBody.copy(fontSize = wordSize, lineHeight = wordSize * 1.33f, fontWeight = FontWeight.SemiBold),
                color = if (accent) work.accent else colors.ink,
            )
            Box(Modifier.fillMaxWidth().height(if (accent) 2.dp else 1.5.dp).background(if (accent) work.accent else colors.ink))
            if (romaji != null) {
                Text(
                    romaji,
                    style = AjlTheme.type.meta.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = if (accent) work.accent else colors.ink2,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        if (notes.isNotEmpty()) {
            Column {
                Text(notes.first(), style = AjlTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 17.sp), color = colors.ink2)
                if (notes.size > 1) {
                    Text(notes.drop(1).joinToString(" · "), style = AjlTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 17.sp), color = colors.ink3)
                }
            }
        }
    }
}
