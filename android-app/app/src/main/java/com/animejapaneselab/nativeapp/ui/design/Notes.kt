package com.animejapaneselab.nativeapp.ui.design

import androidx.compose.foundation.border
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/** Small 1px tag above a headword: 語法 / N3 / 动词. [accent] = work colour (the kind tag). */
@Composable
fun TagChip(text: String, modifier: Modifier = Modifier, accent: Boolean = false, japanese: Boolean = false) {
    val colors = AjlTheme.colors
    val shape = RoundedCornerShape(4.dp)
    Text(
        text,
        modifier = modifier
            .border(AjlStroke.Hair, colors.line2, shape)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        style = (if (japanese) AjlTheme.type.jpLabel else AjlTheme.type.caption).copy(fontSize = 11.sp, lineHeight = 18.sp),
        color = if (accent) AjlTheme.work.accent else colors.ink3,
        maxLines = 1,
    )
}

/** A labelled note: short bold label in a fixed column (意思 / 语气 / 易错), the note beside it. */
@Composable
fun LabeledNote(label: String, text: String, modifier: Modifier = Modifier, labelColor: Color = AjlTheme.colors.ink) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label,
            style = AjlTheme.type.caption.copy(fontSize = 13.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
            color = labelColor,
            modifier = Modifier.width(30.dp),
        )
        NoteText(text, Modifier.weight(1f))
    }
}

/**
 * Word blocks joined by ＋: each part large in serif over a rule with its gloss underneath; the
 * part at [accentIndex] in the work colour (the point being taught).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordBlocks(parts: List<Pair<String, String>>, modifier: Modifier = Modifier, accentIndex: Int = parts.lastIndex) {
    val colors = AjlTheme.colors
    val accent = AjlTheme.work.accent
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        parts.forEachIndexed { i, (word, gloss) ->
            if (i > 0) Text("＋", style = AjlTheme.type.meta.copy(fontSize = 18.sp), color = accent, modifier = Modifier.padding(top = 4.dp))
            val on = i == accentIndex
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val rule = if (on) 2.dp else AjlStroke.Ink
                val ruleColor = if (on) accent else colors.ink
                Text(
                    word,
                    style = AjlTheme.type.jpBody.copy(fontSize = 21.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
                    color = if (on) accent else colors.ink,
                    modifier = Modifier
                        .drawBehind {
                            val y = size.height + 1.dp.toPx()
                            drawLine(ruleColor, Offset(0f, y), Offset(size.width, y), rule.toPx())
                        }
                        .padding(bottom = 2.dp),
                )
                if (gloss.isNotBlank()) Text(gloss, style = AjlTheme.type.caption.copy(fontSize = 12.sp, lineHeight = 17.sp), color = colors.ink3)
            }
        }
    }
}

/** 遮る: a screentoned slot standing in for the translation; tap to show it. */
@Composable
fun CoveredLine(modifier: Modifier = Modifier, label: String = "轻点看中文", onReveal: () -> Unit) {
    val colors = AjlTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.surface)
            .border(1.dp, colors.line2, RoundedCornerShape(4.dp))
            .clickableNoRipple(onReveal)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Screentone(Modifier.fillMaxSize(), color = colors.ink.copy(alpha = 0.08f), spacing = 6.dp, dotRadius = 1.1.dp)
        Text(label, style = AjlTheme.type.caption, color = colors.ink3)
    }
}
