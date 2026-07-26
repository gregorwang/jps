package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.ClozeNode
import com.animejapaneselab.nativeapp.data.LessonNode
import com.animejapaneselab.nativeapp.data.PairMatchNode
import com.animejapaneselab.nativeapp.data.ShadowingNode
import com.animejapaneselab.nativeapp.data.SingleChoiceNode
import com.animejapaneselab.nativeapp.data.StudyCardNode
import com.animejapaneselab.nativeapp.data.TileOrderNode
import com.animejapaneselab.nativeapp.data.japaneseReadingSegments
import com.animejapaneselab.nativeapp.ui.components.JuicyLessonTone
import com.animejapaneselab.nativeapp.ui.theme.LabPalette

internal data class FusionLessonVisualStyle(
    val label: String,
    val accent: Color,
    val accentDark: Color,
    val softContainer: Color,
    val icon: ImageVector,
    val actionTone: JuicyLessonTone,
)

internal data class FusionCoursePalette(
    val accent: Color,
    val accentDark: Color,
    val softContainer: Color,
    val actionTone: JuicyLessonTone,
)

internal fun lessonCoursePalette(workSlug: String): FusionCoursePalette {
    return when (workSlug.lowercase().replace('_', '-')) {
        "k-on", "kon" -> FusionCoursePalette(
            accent = LabPalette.Sakura,
            accentDark = LabPalette.SakuraDark,
            softContainer = LabPalette.SakuraSoft,
            actionTone = JuicyLessonTone.Pink,
        )

        "re-zero", "rezero" -> FusionCoursePalette(
            accent = LabPalette.Violet,
            accentDark = LabPalette.VioletDark,
            softContainer = LabPalette.VioletPanel,
            actionTone = JuicyLessonTone.Purple,
        )

        else -> FusionCoursePalette(
            accent = LabPalette.Violet,
            accentDark = LabPalette.VioletDark,
            softContainer = LabPalette.VioletPanel,
            actionTone = JuicyLessonTone.Purple,
        )
    }
}

internal fun LessonNode.fusionVisualStyle(workSlug: String): FusionLessonVisualStyle {
    val palette = lessonCoursePalette(workSlug)
    val (label, icon) = when (this) {
        is StudyCardNode -> when (sourceKind) {
            "grammar" -> "语法卡" to Icons.Rounded.Bolt
            "sentence" -> "台词卡" to Icons.Rounded.FormatQuote
            else -> "词汇卡" to Icons.Rounded.AutoStories
        }

        is PairMatchNode -> "听音配对" to Icons.AutoMirrored.Rounded.CompareArrows
        is SingleChoiceNode -> when (sourceKind) {
            "vocab" -> "单词学习" to Icons.Rounded.AutoStories
            "grammar" -> "语法理解" to Icons.Rounded.Bolt
            "sentence" -> "台词理解" to Icons.Rounded.FormatQuote
            else -> "选择练习" to Icons.Rounded.AutoStories
        }
        is ClozeNode -> "语法补帧" to Icons.Rounded.Bolt
        is TileOrderNode -> (if (audioTile) "听音剪辑" else "翻译拼句") to Icons.Rounded.GridView
        is ShadowingNode -> "声线模仿" to Icons.AutoMirrored.Rounded.VolumeUp
    }
    return FusionLessonVisualStyle(
        label = label,
        accent = palette.accent,
        accentDark = palette.accentDark,
        softContainer = palette.softContainer,
        icon = icon,
        actionTone = palette.actionTone,
    )
}

@Composable
internal fun FusionLessonStage(
    prompt: String,
    style: FusionLessonVisualStyle,
    modifier: Modifier = Modifier,
    promptMaxLines: Int = 4,
    headerAction: (@Composable () -> Unit)? = null,
    heroContent: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val showHeader = prompt.isNotBlank() || headerAction != null || heroContent != null
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.5.dp, style.accent.copy(alpha = 0.28f)),
        shadowElevation = 7.dp,
    ) {
        Column {
            if (showHeader) {
                Surface(
                    color = style.softContainer.copy(alpha = 0.72f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, top = 15.dp, end = 14.dp, bottom = 15.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (prompt.isNotBlank()) {
                            Text(
                                text = prompt,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                maxLines = promptMaxLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        headerAction?.invoke()
                        heroContent?.let { hero ->
                            Box(
                                modifier = Modifier.size(84.dp),
                                contentAlignment = Alignment.Center,
                                content = hero,
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

@Composable
internal fun FusionLessonFocusCard(
    label: String,
    text: String,
    style: FusionLessonVisualStyle,
    modifier: Modifier = Modifier,
    showLeadingIcon: Boolean = true,
    supportingContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        color = style.softContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.5.dp, style.accent.copy(alpha = 0.42f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (showLeadingIcon) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    color = style.accent,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(style.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = style.accentDark,
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                supportingContent?.invoke(this)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FusionStudyFlashcard(
    japanese: String,
    reading: String,
    meaningZh: String,
    notes: List<String>,
    style: FusionLessonVisualStyle,
    modifier: Modifier = Modifier,
    onPlayAudio: (() -> Unit)? = null,
) {
    val readingSegments = japaneseReadingSegments(japanese, reading)
    val phraseStyle = if (readingSegments.size == 1 && japanese.length <= 8) {
        MaterialTheme.typography.displaySmall
    } else {
        MaterialTheme.typography.headlineLarge
    }
    val visibleNotes = notes
        .asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .filterNot { note -> note == japanese || note == reading || note == meaningZh }
        .distinct()
        .take(2)
        .toList()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = style.softContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(2.dp, style.accent.copy(alpha = 0.52f)),
            shadowElevation = 3.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 9.dp,
                            alignment = Alignment.CenterHorizontally,
                        ),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        readingSegments.forEach { segment ->
                            Text(
                                text = segment,
                                style = phraseStyle,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    if (reading.isNotBlank() && reading != japanese) {
                        Text(
                            text = reading,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        onPlayAudio?.let { playAudio ->
                            Surface(
                                modifier = Modifier.size(48.dp),
                                color = style.accent.copy(alpha = 0.14f),
                                contentColor = style.accentDark,
                                shape = CircleShape,
                            ) {
                                IconButton(onClick = playAudio) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = "播放语音",
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                            }
                        }
                        Surface(
                            color = style.accent,
                            contentColor = Color.White,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = meaningZh,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                    visibleNotes.firstOrNull()?.let { primaryNote ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(
                                text = primaryNote,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                    visibleNotes.getOrNull(1)?.let { supportingNote ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = supportingNote,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
