package com.animejapaneselab.nativeapp.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.design.MarkedLine
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/**
 * A Japanese line with reading aids: kana over each kanji word ([showRuby]) and romaji under
 * every unit ([showRomaji]), so each sound sits right under what spells it. The [mark] range
 * (the point of the line) is bold, work-coloured and underlined, its romaji too.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingLineText(
    reading: LineReading,
    mark: IntRange?,
    showRuby: Boolean,
    showRomaji: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = AjlTheme.type.jpBody,
    color: Color = AjlTheme.colors.ink,
) {
    if (!showRuby && !showRomaji) {
        MarkedLine(reading.text, mark, modifier, style = style, color = color)
        return
    }
    val accent = AjlTheme.work.accent
    val ink3 = AjlTheme.colors.ink3
    val size = style.fontSize.takeIf { it.isSpecified }?.value ?: 18f
    val rubyStyle = AjlTheme.type.jpBody.copy(fontSize = (size * 0.42f).coerceAtLeast(9f).sp, lineHeight = (size * 0.55f).coerceAtLeast(11f).sp)
    val romajiStyle = AjlTheme.type.meta.copy(fontSize = (size * 0.4f).coerceIn(9f, 11f).sp, lineHeight = 13.sp)
    val bodyStyle = style.copy(lineHeight = (size * 1.3f).sp)
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(0.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        reading.units.forEach { unit ->
            val marked = mark != null && unit.start <= mark.last && unit.end > mark.first
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showRuby) {
                    Text(unit.ruby.ifEmpty { " " }, style = rubyStyle, color = if (unit.ruby.isEmpty()) Color.Transparent else if (marked) accent else ink3, maxLines = 1, softWrap = false)
                }
                Text(
                    buildAnnotatedString {
                        append(unit.text)
                        if (mark != null) {
                            val from = (mark.first - unit.start).coerceAtLeast(0)
                            val to = (mark.last + 1 - unit.start).coerceAtMost(unit.text.length)
                            if (from < to) addStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline), from, to)
                        }
                    },
                    style = bodyStyle,
                    color = color,
                    softWrap = false,
                )
                if (showRomaji) {
                    Text(
                        unit.romaji.ifEmpty { " " },
                        style = romajiStyle,
                        color = if (marked) accent else ink3,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 1.dp),
                    )
                }
            }
        }
    }
}
