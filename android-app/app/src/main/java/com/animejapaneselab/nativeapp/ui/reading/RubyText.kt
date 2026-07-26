package com.animejapaneselab.nativeapp.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.FuriganaResult

/**
 * Japanese text with furigana rendered above kanji spans.
 *
 * Falls back to a plain [Text] when [furigana] is null, has no annotation, or does not
 * reconstruct [text] exactly (the AI contract guarantees concatenated spans equal the
 * original sentence; anything else is treated as unusable).
 *
 * Un-annotated spans are split per character so long kana runs wrap like normal
 * Japanese text; annotated spans stay atomic so a reading never detaches from its kanji.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RubyText(
    text: String,
    furigana: FuriganaResult?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    rubyStyle: TextStyle = MaterialTheme.typography.labelSmall,
    color: Color = Color.Unspecified,
    rubyColor: Color = Color.Unspecified,
) {
    val resolvedColor = color.takeOrElse { LocalContentColor.current }
    val annotation = furigana?.takeIf { it.hasAnnotation && it.plainText == text }
    if (annotation == null) {
        Text(text = text, modifier = modifier, style = style, color = resolvedColor)
        return
    }
    val resolvedRubyColor = rubyColor.takeOrElse { resolvedColor.copy(alpha = 0.72f) }
    val units = remember(annotation) { annotation.toRubyUnits() }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        units.forEach { unit ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = unit.reading.ifEmpty { " " },
                    style = rubyStyle,
                    color = if (unit.reading.isEmpty()) Color.Transparent else resolvedRubyColor,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = unit.text,
                    style = style,
                    color = resolvedColor,
                    softWrap = false,
                )
            }
        }
    }
}

private data class RubyUnit(
    val text: String,
    val reading: String,
)

private fun FuriganaResult.toRubyUnits(): List<RubyUnit> {
    return buildList {
        segments.forEach { segment ->
            if (segment.reading.isBlank()) {
                segment.text.forEach { character ->
                    add(RubyUnit(text = character.toString(), reading = ""))
                }
            } else {
                add(RubyUnit(text = segment.text, reading = segment.reading))
            }
        }
    }
}
