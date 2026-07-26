package com.animejapaneselab.nativeapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.FuriganaResult
import com.animejapaneselab.nativeapp.data.SubtitleLine
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.components.LabCard
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.reading.DeepDiveTarget
import com.animejapaneselab.nativeapp.ui.reading.RubyText
import com.animejapaneselab.nativeapp.ui.reading.SentenceDeepDiveSheet
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.reading.rememberSentenceDeepDive
import com.animejapaneselab.nativeapp.ui.theme.LabSpacing
import com.animejapaneselab.nativeapp.ui.theme.LabTheme
import kotlinx.coroutines.delay

@Composable
fun SubtitleBrowserScreen(
    uiState: LabUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onFocusConsumed: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
) {
    var query by rememberSaveable(uiState.selection.workSlug, uiState.selection.episode) {
        mutableStateOf("")
    }
    var copiedLineNo by rememberSaveable(uiState.selection.workSlug, uiState.selection.episode) {
        mutableIntStateOf(-1)
    }
    // 注音是 AI 调用：默认关闭，换作品/换集也归零，避免整集台词被自动送去注音。
    var furiganaRequested by rememberSaveable(uiState.selection.workSlug, uiState.selection.episode) {
        mutableStateOf(false)
    }
    val normalizedQuery = query.trim()
    val visibleSubtitles = remember(uiState.subtitles, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            uiState.subtitles
        } else {
            uiState.subtitles.filter { line ->
                line.jaText.contains(normalizedQuery, ignoreCase = true) ||
                    line.zhText.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }
    val clipboard = LocalClipboardManager.current
    val annotator = rememberFuriganaAnnotator(uiState.settings)
    val deepDive = rememberSentenceDeepDive(uiState.settings)
    val furiganaOn = uiState.settings.showFurigana && furiganaRequested
    val reducedMotion = rememberReducedMotion()
    val listState = rememberLazyListState()
    // 外部跳转（搜索结果 / 错题卡）落到某一行后，让这行背景闪一下再熄灭。
    var focusPulseLineNo by remember(uiState.selection.workSlug, uiState.selection.episode) {
        mutableIntStateOf(-1)
    }
    // 已经定位过的行号：防止字幕列表刷新时对同一个目标反复滚动。
    var consumedFocusLineNo by remember(uiState.selection.workSlug, uiState.selection.episode) {
        mutableIntStateOf(-1)
    }
    val furiganaToggleVisible = uiState.settings.showFurigana && uiState.subtitles.isNotEmpty()
    // LazyColumn 固定头部：顶栏 / 范围选择 / 搜索框 /（可选）注音开关 / 状态行。
    // 所以台词行下标 = 头部项数 + 该行在未过滤列表里的位置。
    val headerItemCount = if (furiganaToggleVisible) 5 else 4

    // 只在用户主动打开注音后，为“当前这批可见台词”排队请求；重复文本由 annotator 内部去重。
    LaunchedEffect(furiganaOn, visibleSubtitles) {
        if (!furiganaOn) return@LaunchedEffect
        annotator.request("subtitle", visibleSubtitles.map(SubtitleLine::jaText))
    }

    // 行定位：带着目标行号进来时滚过去并高亮，滚完通知外部把焦点清掉。
    LaunchedEffect(uiState.subtitleFocusLineNo, uiState.subtitles) {
        val target = uiState.subtitleFocusLineNo
        if (target == null) {
            // 焦点已被清空：解锁标记，之后再跳同一行也能重新定位。
            consumedFocusLineNo = -1
            return@LaunchedEffect
        }
        if (target == consumedFocusLineNo) return@LaunchedEffect
        // 台词还没加载完，等这一集的字幕到位后本效果会以新列表重跑。
        if (uiState.subtitles.isEmpty()) return@LaunchedEffect
        val lineIndex = uiState.subtitles.indexOfFirst { it.lineNo == target }
        if (lineIndex < 0) {
            // 目标行不在这一集里：直接消费掉，别让焦点一直挂着。
            consumedFocusLineNo = target
            onFocusConsumed()
            return@LaunchedEffect
        }
        // 定位优先：本地搜索词可能把目标行过滤掉，先清空搜索框再滚。
        if (query.isNotEmpty()) query = ""
        // 等列表按未过滤内容重新组合、测量完再滚，否则下标会落空。
        withFrameNanos { }
        withFrameNanos { }
        val itemIndex = headerItemCount + lineIndex
        if (reducedMotion) {
            listState.scrollToItem(itemIndex, FocusScrollOffsetPx)
        } else {
            listState.animateScrollToItem(itemIndex, FocusScrollOffsetPx)
        }
        focusPulseLineNo = target
        consumedFocusLineNo = target
        onFocusConsumed()
    }

    // 高亮脉冲驻留一小会儿后自动熄灭（渐隐由行内部动画完成）。
    LaunchedEffect(focusPulseLineNo) {
        if (focusPulseLineNo < 0) return@LaunchedEffect
        delay(FocusPulseHoldMillis)
        focusPulseLineNo = -1
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = LabSpacing.Screen, vertical = LabSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "subtitle-top-bar") {
                SubtitleTopBar(
                    title = uiState.focus.episodeLabel.ifBlank { uiState.focus.workTitle },
                    onBack = onBack,
                    onRefresh = onRefresh,
                    onOpenSearch = onOpenSearch,
                    loading = uiState.subtitleStatus == SyncStatus.Loading,
                )
            }
            item(key = "subtitle-scope") {
                SubtitleScopeSelector(
                    uiState = uiState,
                    onWorkSelected = onWorkSelected,
                    onEpisodeSelected = onEpisodeSelected,
                )
            }
            item(key = "subtitle-search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "清除搜索")
                            }
                        }
                    } else {
                        null
                    },
                    placeholder = { Text("搜索日文或中文台词") },
                    shape = MaterialTheme.shapes.large,
                    colors = labFieldColors(),
                )
            }
            if (furiganaToggleVisible) {
                item(key = "subtitle-furigana-toggle") {
                    FuriganaToggle(
                        enabled = furiganaRequested,
                        onToggle = { furiganaRequested = !furiganaRequested },
                    )
                }
            }
            item(key = "subtitle-status") {
                if (uiState.subtitleMessage.isNotBlank()) {
                    Text(
                        text = uiState.subtitleMessage,
                        modifier = Modifier.padding(horizontal = LabSpacing.XXSmall),
                        color = if (uiState.subtitleStatus == SyncStatus.Error) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                } else if (uiState.subtitles.isNotEmpty()) {
                    Text(
                        text = if (normalizedQuery.isBlank()) {
                            "共 ${uiState.subtitles.size} 行"
                        } else {
                            "找到 ${visibleSubtitles.size} 行"
                        },
                        modifier = Modifier.padding(horizontal = LabSpacing.XXSmall),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (uiState.subtitles.isEmpty()) {
                item(key = "subtitle-empty") {
                    LabCard {
                        Column(verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall)) {
                            Text("暂无台词", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text(
                                "点右上角刷新，或换一集查看。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            } else if (visibleSubtitles.isEmpty()) {
                item(key = "subtitle-no-search-result") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = LabTheme.colors.infoContainer,
                        contentColor = LabTheme.colors.onInfoContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = "没有找到“$normalizedQuery”",
                            modifier = Modifier.padding(LabSpacing.Medium),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                items(
                    items = visibleSubtitles,
                    key = { line -> "${line.lineNo}-${line.startTime}" },
                    contentType = { "subtitle-line" },
                ) { line ->
                    SubtitleLineRow(
                        line = line,
                        copied = copiedLineNo == line.lineNo,
                        focused = focusPulseLineNo == line.lineNo,
                        furigana = if (furiganaOn) annotator.resultFor(line.jaText) else null,
                        onCopy = {
                            clipboard.setText(
                                AnnotatedString(
                                    listOf(line.jaText, line.zhText)
                                        .filter(String::isNotBlank)
                                        .joinToString("\n"),
                                ),
                            )
                            copiedLineNo = line.lineNo
                        },
                        onDeepDive = {
                            deepDive.request(
                                DeepDiveTarget(
                                    workSlug = uiState.selection.workSlug,
                                    episode = uiState.selection.episode,
                                    lineNo = line.lineNo,
                                    jaText = line.jaText,
                                    zhText = line.zhText,
                                ),
                            )
                        },
                    )
                }
            }
        }
        SentenceDeepDiveSheet(deepDive)
    }
}

@Composable
private fun SubtitleTopBar(
    title: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("台词浏览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onOpenSearch) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "全局搜索",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onRefresh, enabled = !loading) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            } else {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "刷新台词",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SubtitleScopeSelector(
    uiState: LabUiState,
    onWorkSelected: (String) -> Unit,
    onEpisodeSelected: (Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedWork = uiState.works.firstOrNull { it.slug == uiState.selection.workSlug }
    val selectedEpisode = uiState.episodes.firstOrNull { it.episode == uiState.selection.episode }
    LabCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = LabSpacing.Small)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { expanded = !expanded }
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(LabSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("EP", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = selectedWork?.displayName ?: uiState.focus.workTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = selectedEpisode?.label ?: uiState.focus.episodeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "收起范围" else "切换作品或选集",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            ExpandableScopeContent(expanded = expanded) {
                Text(
                    "作品",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall)) {
                    items(uiState.works, key = { it.slug }) { work ->
                        val selected = work.slug == uiState.selection.workSlug
                        FilterChip(
                            selected = selected,
                            onClick = { onWorkSelected(work.slug) },
                            label = {
                                Text(
                                    text = work.displayName,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            shape = MaterialTheme.shapes.small,
                            colors = subtitleChipColors(),
                            border = subtitleChipBorder(selected),
                        )
                    }
                }
                Text(
                    "选集",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall)) {
                    items(uiState.episodes, key = { it.id }) { episode ->
                        val selected = episode.episode == uiState.selection.episode
                        FilterChip(
                            selected = selected,
                            onClick = { onEpisodeSelected(episode.episode) },
                            label = {
                                Text(
                                    text = episode.label,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            shape = MaterialTheme.shapes.small,
                            colors = subtitleChipColors(),
                            border = subtitleChipBorder(selected),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 注音开关：注音要走 AI，所以默认关闭，只有用户点亮这枚 chip 才会为当前列表请求假名。
 * 总开关（设置里的 showFurigana）关掉时调用方根本不会渲染它。
 */
@Composable
private fun FuriganaToggle(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = enabled,
                onClick = onToggle,
                label = {
                    Text(
                        text = "假名注音",
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                    )
                },
                shape = MaterialTheme.shapes.small,
                colors = subtitleChipColors(),
                border = subtitleChipBorder(enabled),
            )
        }
        AnimatedVisibility(
            visible = enabled,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                    easing = MotionTokens.Curve.Decelerate,
                ),
            ) + expandVertically(
                animationSpec = tween(
                    durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                    easing = MotionTokens.Curve.Standard,
                ),
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
                    easing = MotionTokens.Curve.Standard,
                ),
            ) + shrinkVertically(
                animationSpec = tween(
                    durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
                    easing = MotionTokens.Curve.Standard,
                ),
            ),
        ) {
            Text(
                text = "注：当前列表的假名由 AI 生成，会陆续标注上来；关掉即停止请求。",
                modifier = Modifier.padding(horizontal = LabSpacing.XXSmall),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SubtitleLineRow(
    line: SubtitleLine,
    copied: Boolean,
    focused: Boolean,
    furigana: FuriganaResult?,
    onCopy: () -> Unit,
    onDeepDive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val containerColor by animateColorAsState(
        targetValue = if (copied) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(
            durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
            easing = MotionTokens.Curve.Standard,
        ),
        label = "subtitle-line-container",
    )
    val borderColor by animateColorAsState(
        targetValue = if (copied) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
        },
        animationSpec = tween(
            durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
            easing = MotionTokens.Curve.Standard,
        ),
        label = "subtitle-line-border",
    )
    // 定位脉冲：进场快、退场慢；reducedMotion 下 MotionTokens.duration 会压成 1ms，
    // 于是变成“静态高亮一秒再恢复”，不会有闪动。
    val highlight by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(
            durationMillis = MotionTokens.duration(
                if (focused) MotionTokens.Duration.Micro else MotionTokens.Duration.AnswerFeedback,
                reducedMotion,
            ),
            easing = MotionTokens.Curve.Standard,
        ),
        label = "subtitle-line-focus",
    )
    // 不在脉冲中时原样用复制态的颜色，既有复制高亮完全不受影响。
    val surfaceColor = if (highlight > 0f) {
        lerp(containerColor, MaterialTheme.colorScheme.tertiaryContainer, highlight)
    } else {
        containerColor
    }
    val strokeColor = if (highlight > 0f) {
        lerp(borderColor, MaterialTheme.colorScheme.tertiary, highlight)
    } else {
        borderColor
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = surfaceColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, strokeColor),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                start = LabSpacing.Small,
                top = LabSpacing.Small,
                end = LabSpacing.XXSmall,
                bottom = LabSpacing.Small,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                color = if (copied) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (copied) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = line.lineNo.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall),
            ) {
                Text(
                    text = "${line.startTime} – ${line.endTime}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                )
                RubyText(
                    text = line.jaText,
                    furigana = furigana,
                    style = MaterialTheme.typography.titleMedium.copy(
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                if (line.zhText.isNotBlank()) {
                    Text(
                        text = line.zhText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = if (copied) "已复制" else "复制这条台词",
                        tint = if (copied) LabTheme.colors.success else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onDeepDive) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = "精读这条台词",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** 作品/选集折叠区：动画走 MotionTokens 并尊重系统减弱动效。 */
@Composable
private fun ExpandableScopeContent(
    expanded: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                easing = MotionTokens.Curve.Decelerate,
            ),
        ) + expandVertically(
            animationSpec = tween(
                durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                easing = MotionTokens.Curve.Standard,
            ),
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
                easing = MotionTokens.Curve.Standard,
            ),
        ) + shrinkVertically(
            animationSpec = tween(
                durationMillis = MotionTokens.duration(MotionTokens.Duration.CardEnter, reducedMotion),
                easing = MotionTokens.Curve.Standard,
            ),
        ),
    ) {
        Column(
            modifier = Modifier.padding(top = LabSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            content = content,
        )
    }
}

@Composable
private fun subtitleChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurface,
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
)

@Composable
private fun subtitleChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = MaterialTheme.colorScheme.outline,
    selectedBorderColor = MaterialTheme.colorScheme.primary,
)

/** 定位时给目标行留的顶部余量（像素，负值=多往上滚一点），免得它顶死在列表最上沿。 */
private const val FocusScrollOffsetPx = -100

/** 高亮脉冲驻留时长；到点后由行内部动画把颜色渐渐收回常态。 */
private const val FocusPulseHoldMillis = 1000L
