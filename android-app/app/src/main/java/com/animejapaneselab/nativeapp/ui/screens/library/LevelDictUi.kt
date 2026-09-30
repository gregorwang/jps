package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.Conjugator
import com.animejapaneselab.nativeapp.data.LevelDict
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

// ---------------------------------------------------------------------------
// 词类: the drawer under the tabs (名词 / 动词 五段·一段·サ变 / 形容词 …)
// ---------------------------------------------------------------------------

/** Part-of-speech filter of the level dictionary. The selection is one of the strings below. */
internal object PosCat {
    const val All = "全部"
    const val Verb = "动词"
    const val Godan = "动词·五段"
    const val Ichidan = "动词·一段"
    const val Suru = "动词·サ变"

    /** Top-level chips, in the order the drawer shows them. */
    val Top = listOf("名词", Verb, "い形容词", "な形容词", "副词", "感叹词", "连语", "其他")
    val VerbKinds = listOf(Godan, Ichidan, Suru)

    /** category (a [Top] entry) and, for verbs, the conjugation kind (a [VerbKinds] entry or null). */
    data class Key(val cat: String, val verbKind: String?)

    fun keyOf(item: VocabItem): Key {
        val raw = item.partOfSpeech.trim()
        val label = partOfSpeechLabel(raw)
        return when {
            raw == "连语" || raw == "連語" || raw == "表达" -> Key("连语", null)
            raw == "感叹词" || raw == "感動詞" -> Key("感叹词", null)
            label == "名词" -> Key("名词", null)
            label == "い形容词" || label == "な形容词" || label == "副词" -> Key(label, null)
            label.endsWith("动词") && label != "助动词" -> Key(Verb, verbKind(item, label))
            else -> Key("其他", null)
        }
    }

    private fun verbKind(item: VocabItem, label: String): String? {
        when (label) {
            "五段动词" -> return Godan
            "一段动词" -> return Ichidan
            "サ变动词" -> return Suru
        }
        val type = Conjugator.tableFor(item.surface, item.reading, item.partOfSpeech)?.typeLabel.orEmpty()
        return when {
            type.startsWith("五段") -> Godan
            type.startsWith("一段") -> Ichidan
            type.startsWith("サ变") -> Suru
            else -> null
        }
    }

    fun index(items: List<VocabItem>): Map<String, Key> = items.associate { it.id to keyOf(it) }

    fun matches(key: Key?, selected: String): Boolean = when {
        selected == All -> true
        key == null -> false
        selected in VerbKinds -> key.verbKind == selected
        else -> key.cat == selected
    }

    fun counts(index: Map<String, Key>): Map<String, Int> {
        val out = HashMap<String, Int>()
        out[All] = index.size
        index.values.forEach { key ->
            out[key.cat] = (out[key.cat] ?: 0) + 1
            key.verbKind?.let { out[it] = (out[it] ?: 0) + 1 }
        }
        return out
    }
}

/** The bar under the tabs: 词类 · current · ⌄. Tap or pull down to open, tap ✕ to clear. */
@Composable
internal fun PosHandle(
    selected: String,
    open: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    Box(modifier.fillMaxWidth().height(40.dp)) {
        Row(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var pulled = 0f
                    detectVerticalDragGestures(
                        onDragStart = { pulled = 0f },
                        onVerticalDrag = { change, dy ->
                            pulled += dy
                            if (pulled > 24.dp.toPx()) {
                                pulled = 0f
                                onOpen()
                            }
                            change.consume()
                        },
                    )
                }
                .clickableNoRipple(onClick = onToggle)
                .semantics { contentDescription = if (open) "收起词类抽屉" else "拉下词类抽屉，当前：$selected" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("词类", style = AjlTheme.type.caption.copy(fontSize = 13.sp), color = colors.ink3)
            Text(
                selected,
                style = AjlTheme.type.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = if (selected == PosCat.All) colors.ink else AjlTheme.work.accent,
            )
            if (selected != PosCat.All) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "清除词类",
                    tint = colors.ink3,
                    modifier = Modifier.size(28.dp).padding(6.dp).clickableNoRipple(onClick = onClear),
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = colors.ink3, modifier = Modifier.size(18.dp))
        }
        // The grabber, so the bar reads as something to pull.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.line2),
        )
        Hairline(Modifier.align(Alignment.BottomStart))
    }
}

/** The opened drawer: a scrim over the list, chips of the 词类 with counts, verbs split below. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PosPanel(
    selected: String,
    counts: Map<String, Int>,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AjlTheme.colors
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(colors.ink.copy(alpha = 0.38f)).clickableNoRipple(onClick = onClose))
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .border(AjlStroke.Ink, colors.ink, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        ) {
            val verbShown = selected == PosCat.Verb || selected in PosCat.VerbKinds
            FlowRow(
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val tops = listOf(PosCat.All) + PosCat.Top.filter { (counts[it] ?: 0) > 0 }
                tops.forEach { key ->
                    PosChip(
                        label = key,
                        count = counts[key] ?: 0,
                        on = if (key == PosCat.Verb) verbShown else selected == key,
                        onClick = { onSelect(key) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Pad the last row so the chips keep their column width.
                repeat((3 - tops.size % 3) % 3) { Spacer(Modifier.weight(1f)) }
            }
            if (verbShown) {
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("动词", style = AjlTheme.type.caption.copy(fontSize = 13.sp), color = colors.ink3)
                    Hairline(Modifier.weight(1f))
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosChip("全部动词", counts[PosCat.Verb] ?: 0, selected == PosCat.Verb, { onSelect(PosCat.Verb) }, Modifier.weight(1f))
                    PosCat.VerbKinds.filter { (counts[it] ?: 0) > 0 }.forEach { kind ->
                        PosChip(kind.removePrefix("动词·"), counts[kind] ?: 0, selected == kind, { onSelect(kind) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PosChip(label: String, count: Int, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .height(52.dp)
            .background(if (on) work.soft else colors.surface, shape)
            .border(AjlStroke.Hair, if (on) work.accent else colors.line2, shape)
            .clickableNoRipple(onClick = onClick)
            .semantics { contentDescription = "$label $count 个" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = AjlTheme.type.body.copy(fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal), color = if (on) work.accent else colors.ink, maxLines = 1)
        Text(count.toString(), style = AjlTheme.type.metaSmall, color = colors.ink3)
    }
}

// ---------------------------------------------------------------------------
// 级别: N5 ▾ at the right end of the tab rule
// ---------------------------------------------------------------------------

internal data class LevelOption(val key: String, val label: String, val count: Int)

@Composable
internal fun LevelChip(selected: String, options: List<LevelOption>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val label = options.firstOrNull { it.key == selected }?.label ?: selected
    Box(modifier) {
        Row(
            Modifier
                .height(32.dp)
                .background(colors.surface, shape)
                .border(AjlStroke.Hair, colors.line2, shape)
                .clickableNoRipple(onClick = { open = true })
                .semantics { contentDescription = "切换级别：$label" }
                .padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = AjlTheme.type.meta.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = colors.ink, maxLines = 1)
            Icon(Icons.Rounded.ExpandMore, null, tint = colors.ink2, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(12.dp),
        ) {
            options.forEach { option ->
                val on = option.key == selected
                DropdownMenuItem(
                    text = {
                        Row(Modifier.padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Text(
                                option.label,
                                style = AjlTheme.type.meta.copy(fontSize = 15.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                                color = if (on) work.accent else colors.ink,
                                modifier = Modifier.weight(1f),
                            )
                            Text(option.count.toString(), style = AjlTheme.type.metaSmall, color = colors.ink3)
                        }
                    },
                    onClick = {
                        open = false
                        onSelect(option.key)
                    },
                    modifier = Modifier.background(if (on) work.soft else colors.surface),
                )
            }
        }
    }
}

/** 级外 is shown as 級外; the other keys are shown as they are. */
internal fun levelLabel(key: String): String = if (key == LevelDict.Outside) "級外" else key
