package com.animejapaneselab.nativeapp.ui.completion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.LabTheme

@Composable
fun MistakeSummaryCard(
    result: LessonResultUiState,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val mistakeCount = (result.completedCount - result.correctCount).coerceAtLeast(0)
    val errorRatio = if (result.completedCount == 0) 0f else mistakeCount.toFloat() / result.completedCount
    val progress by animateFloatAsState(
        targetValue = if (reducedMotion) errorRatio else errorRatio.coerceAtLeast(0.06f),
        animationSpec = MotionTokens.softSpring(reducedMotion),
        label = "mistake-summary-progress",
    )
    LabCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                color = LabTheme.colors.warningContainer,
                contentColor = LabTheme.colors.warning,
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = "本次主要错因",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = result.primaryMistakeType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            Surface(
                color = LabTheme.colors.warningContainer,
                contentColor = LabTheme.colors.onWarningContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = "$mistakeCount 题",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = LabTheme.colors.warning,
            trackColor = LabTheme.colors.warningContainer,
            strokeCap = StrokeCap.Round,
            gapSize = (-1).dp,
            drawStopIndicator = {},
        )
    }
}
