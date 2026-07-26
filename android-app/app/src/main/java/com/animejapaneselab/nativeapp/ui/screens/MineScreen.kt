package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.components.SectionTitle
import com.animejapaneselab.nativeapp.ui.components.TagChip
import com.animejapaneselab.nativeapp.ui.theme.LabPalette

@Composable
fun MineScreen(
    uiState: LabUiState,
    onOpenSettings: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenLibrary: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            MineHero(
                email = uiState.auth.user?.email.orEmpty(),
                reviewCount = uiState.reviewTasks.size + uiState.mistakes.size,
                masteredCount = uiState.progressItems.count { it.state.remoteValue == "known" || it.state.remoteValue == "good" },
            )
        }
        item {
            LabCard {
                SectionTitle(
                    eyebrow = "账户",
                    title = "学习数据按账号保存",
                    accentColor = LabPalette.Violet,
                )
                Text(
                    text = "当前作品：${uiState.focus.episodeLabel}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TagChip("云端进度")
                    TagChip("自动复习")
                    TagChip("外部 AI 提示词")
                }
            }
        }
        item {
            MineActionCard(
                title = "学习设置",
                subtitle = "声音、讲解模型、资料更新和退出登录。",
                icon = Icons.Rounded.Settings,
                onClick = onOpenSettings,
            )
        }
        item {
            MineActionCard(
                title = "复盘中心",
                subtitle = "查看错因、弱点和到期复习。",
                icon = Icons.Rounded.BarChart,
                onClick = onOpenReview,
            )
        }
        item {
            MineActionCard(
                title = "资料库",
                subtitle = "浏览作品、单集、台词、词汇、语法和跟读材料。",
                icon = Icons.Rounded.AutoStories,
                onClick = onOpenLibrary,
            )
        }
        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, LabPalette.Violet.copy(alpha = 0.32f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LabPalette.Violet),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Text("退出登录", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun MineHero(
    email: String,
    reviewCount: Int,
    masteredCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = LabPalette.VioletPanel,
        contentColor = LabPalette.Ink,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, LabPalette.Violet.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    color = Color.White,
                    contentColor = LabPalette.Violet,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.padding(14.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("我的", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = email.ifBlank { "已登录账号" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MineMetric("待复盘", reviewCount.toString(), Modifier.weight(1f))
                MineMetric("已掌握", masteredCount.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MineMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.88f),
        contentColor = LabPalette.Ink,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, LabPalette.Violet.copy(alpha = 0.10f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                label,
                color = LabPalette.Muted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MineActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
        tonalElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = LabPalette.VioletPanel,
                contentColor = LabPalette.Violet,
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
