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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
    // 场景分组是纯本地视图偏好：默认开，跨作品/集保留用户的选择。
    var sceneGroupingRequested by rememberSaveable { mutableStateOf(true) }
    // 已折叠的场景序号；换作品/集自动清空，跟其余每集状态同一套 key。
    var collapsedScenes by rememberSaveable(
        uiState.selection.workSlug,
        uiState.selection.episode,
        stateSaver = CollapsedScenesSaver,
    ) {
        mutableStateOf(emptySet<Int>())
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
    // 按台词间的静默切段；只切一次，结果跟着这一集的字幕缓存。
    val scenes = remember(uiState.subtitles) { splitSubtitleScenes(uiState.subtitles) }
    val furiganaToggleVisible = uiState.settings.showFurigana && uiState.subtitles.isNotEmpty()
    // 只有真的切出两段以上才值得给开关：单场景（含时间全解析失败）等同于平铺，chip 藏起来。
    val sceneToggleVisible = scenes.size > 1
    val toolbarVisible = furiganaToggleVisible || sceneToggleVisible
    // 搜索命中天然跨场景，分组没有意义，一旦有搜索词就退化为平铺。
    val groupingActive = sceneGroupingRequested && sceneToggleVisible && normalizedQuery.isBlank()

    // 整个 LazyColumn 的 item 序列先算成一份描述列表：渲染和“目标行下标”共用它，
    // 于是头部有几项、场景头插在哪里、哪些行因为折叠而消失，都不需要再手工数。
    val listItems = remember(
        uiState.subtitles,
        visibleSubtitles,
        scenes,
        groupingActive,
        collapsedScenes,
        toolbarVisible,
    ) {
        buildSubtitleItems(
            hasSubtitles = uiState.subtitles.isNotEmpty(),
            visibleLines = visibleSubtitles,
            scenes = scenes,
            grouped = groupingActive,
            collapsedScenes = collapsedScenes,
            toolbarVisible = toolbarVisible,
        )
    }

    // 只在用户主动打开注音后，为“当前这批可见台词”排队请求；重复文本由 annotator 内部去重。
    // 分组模式下折叠掉的行不在 listItems 里，也就不会被请求。
    LaunchedEffect(furiganaOn, listItems) {
        if (!furiganaOn) return@LaunchedEffect
        annotator.request(
            "subtitle",
            listItems.mapNotNull { item -> (item as? SubtitleItemSpec.Line)?.line?.jaText },
        )
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
        if (uiState.subtitles.none { it.lineNo == target }) {
            // 目标行不在这一集里：直接消费掉，别让焦点一直挂着。
            consumedFocusLineNo = target
            onFocusConsumed()
            return@LaunchedEffect
        }
        // 定位优先：本地搜索词可能把目标行过滤掉，先清空搜索框再滚。
        if (query.isNotEmpty()) query = ""
        // 分组开着时目标行所在场景若是折叠的，先展开——否则它压根没有对应的 item。
        if (sceneGroupingRequested && scenes.size > 1) {
            val scene = scenes.firstOrNull { candidate ->
                candidate.lines.any { line -> line.lineNo == target }
            }
            if (scene != null && scene.number in collapsedScenes) {
                collapsedScenes = collapsedScenes - scene.number
            }
        }
        // 等列表按“搜索已清空 + 目标场景已展开”的新状态重新组合、测量完再滚，否则下标会落空。
        withFrameNanos { }
        withFrameNanos { }
        // 用和渲染同一个构建函数、按滚动这一刻的状态重演一遍 item 序列，直接查目标行的真实下标。
        // 搜索已清空所以可见行就是整集；分组开关与折叠集合都读最新值，和刚组合出来的列表逐项一致。
        val itemIndex = buildSubtitleItems(
            hasSubtitles = true,
            visibleLines = uiState.subtitles,
            scenes = scenes,
            grouped = sceneGroupingRequested && scenes.size > 1,
            collapsedScenes = collapsedScenes,
            toolbarVisible = toolbarVisible,
        ).indexOfFirst { item -> item is SubtitleItemSpec.Line && item.line.lineNo == target }
        if (itemIndex < 0) {
            consumedFocusLineNo = target
            onFocusConsumed()
            return@LaunchedEffect
        }
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
            items(
                items = listItems,
                key = { item -> item.key },
                contentType = { item -> item.contentType },
            ) { item ->
                when (item) {
                    is SubtitleItemSpec.Fixed -> when (item.slot) {
                        SubtitleFixedSlot.TopBar -> SubtitleTopBar(
                            title = uiState.focus.episodeLabel.ifBlank { uiState.focus.workTitle },
                            onBack = onBack,
                            onRefresh = onRefresh,
                            onOpenSearch = onOpenSearch,
                            loading = uiState.subtitleStatus == SyncStatus.Loading,
                        )

                        SubtitleFixedSlot.Scope -> SubtitleScopeSelector(
                            uiState = uiState,
                            onWorkSelected = onWorkSelected,
                            onEpisodeSelected = onEpisodeSelected,
                        )

                        SubtitleFixedSlot.Search -> OutlinedTextField(
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

                        SubtitleFixedSlot.Toolbar -> SubtitleToolbar(
                            furiganaVisible = furiganaToggleVisible,
                            furiganaEnabled = furiganaRequested,
                            onFuriganaToggle = { furiganaRequested = !furiganaRequested },
                            sceneGroupingVisible = sceneToggleVisible,
                            sceneGroupingEnabled = sceneGroupingRequested,
                            onSceneGroupingToggle = { sceneGroupingRequested = !sceneGroupingRequested },
                        )

                        SubtitleFixedSlot.Status -> {
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
                                    text = when {
                                        normalizedQuery.isNotBlank() -> "找到 ${visibleSubtitles.size} 行"
                                        groupingActive -> "共 ${uiState.subtitles.size} 行 · ${scenes.size} 个场景"
                                        else -> "共 ${uiState.subtitles.size} 行"
                                    },
                                    modifier = Modifier.padding(horizontal = LabSpacing.XXSmall),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        SubtitleFixedSlot.Empty -> LabCard {
                            Column(verticalArrangement = Arrangement.spacedBy(LabSpacing.XXSmall)) {
                                Text("暂无台词", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Text(
                                    "点右上角刷新，或换一集查看。",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        SubtitleFixedSlot.NoSearchResult -> Surface(
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

                    is SubtitleItemSpec.SceneHeader -> SubtitleSceneHeader(
                        scene = item.scene,
                        collapsed = item.scene.number in collapsedScenes,
                        onToggle = {
                            val number = item.scene.number
                            collapsedScenes = if (number in collapsedScenes) {
                                collapsedScenes - number
                            } else {
                                collapsedScenes + number
                            }
                        },
                    )

                    is SubtitleItemSpec.Line -> {
                        val line = item.line
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
        }
        SentenceDeepDiveSheet(deepDive)
    }
}

// ---------------------------------------------------------------------------
// 场景切分（纯逻辑，不碰 Compose）
// ---------------------------------------------------------------------------

/** 一段连续对白。[number] 从 1 开始，同时兼作折叠状态与 item key 的标识。 */
private data class SubtitleScene(
    val number: Int,
    val lines: List<SubtitleLine>,
    val startMillis: Long?,
    val endMillis: Long?,
) {
    /** "MM:SS–MM:SS"；整段时间戳都解析不出来时为 null，场景头就只显示行数。 */
    val timeRangeLabel: String? = if (startMillis == null || endMillis == null) {
        null
    } else {
        "${formatSceneClock(startMillis)}–${formatSceneClock(maxOf(startMillis, endMillis))}"
    }
}

/**
 * 把 "HH:MM:SS,mmm" 这类时间戳解析成毫秒。
 *
 * 防御式：容忍 "MM:SS" / "SS" 的短写、用 '.' 分隔毫秒、毫秒不足或超过 3 位、
 * 以及后面跟着多余内容（只取第一个空格前的片段）。任何解析不出来的形态一律返回
 * null，由调用方决定退化行为，绝不抛异常。
 */
private fun parseSubtitleTimeMillis(raw: String?): Long? {
    val token = raw?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() } ?: return null
    val separator = token.indexOfLast { it == ',' || it == '.' }
    val clock = if (separator >= 0) token.substring(0, separator) else token
    val fraction = if (separator >= 0) token.substring(separator + 1) else ""
    val millis = when {
        fraction.isEmpty() -> 0L
        !fraction.all(Char::isDigit) -> return null
        else -> fraction.take(3).padEnd(3, '0').toLongOrNull() ?: return null
    }
    val parts = clock.split(':')
    if (parts.isEmpty() || parts.size > 3) return null
    val numbers = parts.map { part ->
        val cleaned = part.trim()
        if (cleaned.isEmpty() || !cleaned.all(Char::isDigit)) return null
        cleaned.toLongOrNull() ?: return null
    }
    val seconds = when (numbers.size) {
        3 -> numbers[0] * 3600 + numbers[1] * 60 + numbers[2]
        2 -> numbers[0] * 60 + numbers[1]
        else -> numbers[0]
    }
    return seconds * 1000 + millis
}

/**
 * 按静默间隔把整集台词切成场景：
 * 「本行开始时间 − 上一行结束时间（结束解析失败就退回它自己的开始时间）」超过
 * [SceneGapMillis] 就开新场景。
 *
 * 时间解析失败的行不产生边界、也不更新比较基准，直接跟着当前场景走；
 * 整集一个时间戳都解析不出来时自然只剩一个场景，调用方据此退化成平铺。
 */
private fun splitSubtitleScenes(lines: List<SubtitleLine>): List<SubtitleScene> {
    if (lines.isEmpty()) return emptyList()
    val starts = lines.map { parseSubtitleTimeMillis(it.startTime) }
    val ends = lines.mapIndexed { index, line ->
        parseSubtitleTimeMillis(line.endTime) ?: starts[index]
    }
    val boundaries = mutableListOf(0)
    var previousEnd: Long? = null
    for (index in lines.indices) {
        val start = starts[index]
        val prev = previousEnd
        if (index > 0 && start != null && prev != null && start - prev > SceneGapMillis) {
            boundaries += index
        }
        val end = ends[index]
        if (end != null) previousEnd = end
    }
    boundaries += lines.size
    return boundaries.zipWithNext().mapIndexed { sceneIndex, (from, to) ->
        SubtitleScene(
            number = sceneIndex + 1,
            lines = lines.subList(from, to).toList(),
            startMillis = (from until to).mapNotNull { starts[it] }.minOrNull(),
            endMillis = (from until to).mapNotNull { ends[it] }.maxOrNull(),
        )
    }
}

/** 毫秒 → "MM:SS"。纯 Kotlin 补零，不受 Locale 影响（阿拉伯语环境也还是 ASCII 数字）。 */
private fun formatSceneClock(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

// ---------------------------------------------------------------------------
// LazyColumn item 序列
// ---------------------------------------------------------------------------

/** 固定头部/占位槽位；[key] 同时用作 LazyColumn 的 key 与 contentType。 */
private enum class SubtitleFixedSlot(val key: String) {
    TopBar("subtitle-top-bar"),
    Scope("subtitle-scope"),
    Search("subtitle-search"),
    Toolbar("subtitle-toolbar"),
    Status("subtitle-status"),
    Empty("subtitle-empty"),
    NoSearchResult("subtitle-no-search-result"),
}

/**
 * LazyColumn 里一条 item 的描述。渲染与“目标行在列表中的下标”都由同一份序列驱动，
 * 所以不再需要按头部数量手算偏移。
 */
private sealed interface SubtitleItemSpec {
    val key: String
    val contentType: String

    data class Fixed(val slot: SubtitleFixedSlot) : SubtitleItemSpec {
        override val key: String get() = slot.key
        override val contentType: String get() = slot.key
    }

    data class SceneHeader(val scene: SubtitleScene) : SubtitleItemSpec {
        override val key: String get() = "scene-${scene.number}"
        override val contentType: String get() = "subtitle-scene-header"
    }

    data class Line(val line: SubtitleLine) : SubtitleItemSpec {
        override val key: String get() = "${line.lineNo}-${line.startTime}"
        override val contentType: String get() = "subtitle-line"
    }
}

/**
 * 生成整个列表的 item 序列。纯函数：给定同样的入参必然得到同样的顺序，
 * 因此行定位可以拿“定位后的入参”预演一遍，直接算出目标行的下标。
 */
private fun buildSubtitleItems(
    hasSubtitles: Boolean,
    visibleLines: List<SubtitleLine>,
    scenes: List<SubtitleScene>,
    grouped: Boolean,
    collapsedScenes: Set<Int>,
    toolbarVisible: Boolean,
): List<SubtitleItemSpec> {
    val items = mutableListOf<SubtitleItemSpec>()
    items += SubtitleItemSpec.Fixed(SubtitleFixedSlot.TopBar)
    items += SubtitleItemSpec.Fixed(SubtitleFixedSlot.Scope)
    items += SubtitleItemSpec.Fixed(SubtitleFixedSlot.Search)
    if (toolbarVisible) items += SubtitleItemSpec.Fixed(SubtitleFixedSlot.Toolbar)
    items += SubtitleItemSpec.Fixed(SubtitleFixedSlot.Status)
    when {
        !hasSubtitles -> items += SubtitleItemSpec.Fixed(SubtitleFixedSlot.Empty)
        visibleLines.isEmpty() -> items += SubtitleItemSpec.Fixed(SubtitleFixedSlot.NoSearchResult)
        // 分组只在“无搜索词”时开启，此时 visibleLines 就是整集，scenes 正好把它切完。
        grouped -> scenes.forEach { scene ->
            items += SubtitleItemSpec.SceneHeader(scene)
            if (scene.number !in collapsedScenes) {
                scene.lines.forEach { line -> items += SubtitleItemSpec.Line(line) }
            }
        }

        else -> visibleLines.forEach { line -> items += SubtitleItemSpec.Line(line) }
    }
    return items
}

// ---------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------

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
 * 工具区：同一行里并排放「假名注音」和「场景分组」两枚同款 chip。
 * 注音要走 AI，所以默认关闭，只有用户点亮才会为当前列表请求假名；
 * 总开关（设置里的 showFurigana）关掉时 [furiganaVisible] 为 false，chip 直接不出现。
 * 场景分组是纯本地视图开关，默认开，只有切得出两段以上时才显示。
 */
@Composable
private fun SubtitleToolbar(
    furiganaVisible: Boolean,
    furiganaEnabled: Boolean,
    onFuriganaToggle: () -> Unit,
    sceneGroupingVisible: Boolean,
    sceneGroupingEnabled: Boolean,
    onSceneGroupingToggle: () -> Unit,
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
            if (furiganaVisible) {
                FilterChip(
                    selected = furiganaEnabled,
                    onClick = onFuriganaToggle,
                    label = {
                        Text(
                            text = "假名注音",
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = subtitleChipColors(),
                    border = subtitleChipBorder(furiganaEnabled),
                )
            }
            if (sceneGroupingVisible) {
                FilterChip(
                    selected = sceneGroupingEnabled,
                    onClick = onSceneGroupingToggle,
                    label = {
                        Text(
                            text = "场景分组",
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = subtitleChipColors(),
                    border = subtitleChipBorder(sceneGroupingEnabled),
                )
            }
        }
        AnimatedVisibility(
            visible = furiganaVisible && furiganaEnabled,
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

/**
 * 场景头：一条 surfaceContainerHigh 小胶囊，点一下折叠/展开整段。
 * 折叠时该场景的行根本不进 item 序列，所以不需要额外的进出场动画，只让箭头转一下。
 */
@Composable
private fun SubtitleSceneHeader(
    scene: SubtitleScene,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberReducedMotion()
    val arrowRotation by animateFloatAsState(
        targetValue = if (collapsed) 0f else 180f,
        animationSpec = tween(
            durationMillis = MotionTokens.duration(MotionTokens.Duration.Micro, reducedMotion),
            easing = MotionTokens.Curve.Standard,
        ),
        label = "subtitle-scene-arrow",
    )
    val detail = listOfNotNull(scene.timeRangeLabel, "${scene.lines.size} 行").joinToString(" · ")
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .heightIn(min = 44.dp)
                .padding(horizontal = LabSpacing.Small, vertical = LabSpacing.XXSmall),
            horizontalArrangement = Arrangement.spacedBy(LabSpacing.XSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "场景 ${scene.number}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = detail,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = if (collapsed) "展开场景 ${scene.number}" else "折叠场景 ${scene.number}",
                modifier = Modifier
                    .size(20.dp)
                    .rotate(arrowRotation),
                tint = MaterialTheme.colorScheme.primary,
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

/** 折叠场景序号的持久化：Bundle 存不了 Set，落成 List<Int> 再还原。 */
private val CollapsedScenesSaver = listSaver<Set<Int>, Int>(
    save = { collapsed -> collapsed.toList() },
    restore = { saved -> saved.toSet() },
)

/** 相邻两行之间静默超过这个时长就判为换场。 */
private const val SceneGapMillis = 9_000L

/** 定位时给目标行留的顶部余量（像素，负值=多往上滚一点），免得它顶死在列表最上沿。 */
private const val FocusScrollOffsetPx = -100

/** 高亮脉冲驻留时长；到点后由行内部动画把颜色渐渐收回常态。 */
private const val FocusPulseHoldMillis = 1000L
