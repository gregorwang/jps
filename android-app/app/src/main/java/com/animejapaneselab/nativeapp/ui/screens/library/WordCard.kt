package com.animejapaneselab.nativeapp.ui.screens.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.ShadowingSentence
import com.animejapaneselab.nativeapp.data.VocabItem
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.AjlBottomSheet
import com.animejapaneselab.nativeapp.ui.design.FilterPill
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.MarkedLine
import com.animejapaneselab.nativeapp.ui.design.NoteText
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.VoiceBars
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.reading.Kana
import com.animejapaneselab.nativeapp.ui.theme.AjlShape
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.words.FormDial
import com.animejapaneselab.nativeapp.ui.words.ExtraConjugations
import com.animejapaneselab.nativeapp.ui.words.VocabCards
import com.animejapaneselab.nativeapp.ui.words.WordRules
import kotlinx.coroutines.launch

/** Reading that spells the headword (the stored one is sometimes the dictionary form's). */
@Composable
internal fun rememberShownReading(item: VocabItem): String {
    val appContext = LocalContext.current.applicationContext
    return remember(item.id) {
        val fix = VocabCards.get(appContext, item.id)
        WordRules.card(item, null, emptyList(), fix).reading ?: item.reading
    }
}

@Composable
internal fun rememberShownMeaning(item: VocabItem): String {
    val appContext = LocalContext.current.applicationContext
    return remember(item.id) {
        val fix = VocabCards.get(appContext, item.id)
        fix?.meaning?.takeIf { fix.keep && it.isNotBlank() } ?: item.meaningZh
    }
}

/**
 * 词卡: a word pulled up from the 词汇 list. The headword is live — the column on the right
 * (ま み む め も ん) turns it into the forms starting with that kana, a sideways swipe steps
 * through every form, tapping it reads the form shown. 收藏 and 斩 are the two icons up top.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun WordCardSheet(
    item: VocabItem,
    example: ShadowingSentence?,
    saved: Boolean,
    known: Boolean,
    speaking: Boolean,
    uiState: LabUiState,
    onSpeak: (String) -> Unit,
    onPlayExample: () -> Unit,
    onToggleSave: () -> Unit,
    onCut: () -> Unit,
    onAsk: () -> Unit,
    onLearn: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val work = AjlTheme.work
    val appContext = LocalContext.current.applicationContext
    val reading = rememberShownReading(item)
    val meaning = rememberShownMeaning(item)
    val fix = remember(item.id) { VocabCards.get(appContext, item.id) }
    val dial = remember(item.id, reading) {
        FormDial.of(ExtraConjugations.tableFor(appContext, item.surface, reading, item.partOfSpeech), item.surface, reading, meaning)
    }
    var at by rememberSaveable(item.id) { mutableIntStateOf(0) }
    val form = dial?.forms?.getOrNull(at)
    fun go(index: Int) {
        val target = dial?.forms?.getOrNull(index) ?: return
        at = index
        onSpeak(target.value)
    }

    AjlBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Level · type, then 收藏 and 斩 as icons.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val level = Jlpt.normalize(item.level).takeIf { it in Jlpt.Levels }
                if (level != null) {
                    Text(
                        level,
                        style = type.metaSmall.copy(fontSize = 11.sp),
                        color = work.accent,
                        modifier = Modifier.background(work.soft, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
                Text(
                    dial?.typeLabel ?: partOfSpeechLabel(item.partOfSpeech),
                    style = type.caption,
                    color = colors.ink2,
                    modifier = Modifier.weight(1f),
                )
                IconButton44(
                    if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    if (saved) "取消收藏" else "收藏",
                    onToggleSave,
                    tint = work.accent,
                    iconSize = 22.dp,
                )
                if (known) {
                    OutlineButton("恢复", onCut, compact = true)
                } else {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clickableNoRipple(onClick = onCut)
                            .semantics { contentDescription = "斩" },
                        contentAlignment = Alignment.Center,
                    ) { SealMark(armed = false, tint = colors.ink) }
                }
            }

            WordStage(
                dial = dial,
                at = at,
                surface = item.surface,
                reading = reading,
                meaning = meaning,
                speaking = speaking,
                onTapWord = { onSpeak(form?.value ?: item.surface) },
                onStep = { step -> dial?.let { go((at + step + it.forms.size) % it.forms.size) } },
                onCell = { cell -> dial?.let { go(it.cycle(cell, at)) } },
            )

            if (dial != null) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    dial.forms.forEachIndexed { index, f ->
                        FilterPill(f.label, selected = index == at, onClick = { go(index) })
                    }
                }
            }

            if (example != null) {
                val text = remember(example.id) { parseSpokenLine(example.ja).text }
                val mark = remember(text, dial) { exampleMark(text, item.surface, dial) }
                Hairline()
                Row(
                    Modifier.fillMaxWidth().clickableNoRipple(onClick = onPlayExample),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MarkedLine(text, mark, style = type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp))
                        if (example.meaningZh.isNotBlank()) Text(example.meaningZh, style = type.caption, color = colors.ink2)
                    }
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).border(AjlStroke.Hair, colors.line2, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.PlayArrow, contentDescription = "播放例句", tint = colors.ink, modifier = Modifier.size(18.dp)) }
                }
            }

            SameSoundRow(item.surface)

            // A checked card's empty note is deliberate: never fall back to the row's unreliable one.
            val note = if (fix?.keep == true) fix.note else item.realWorldNote
            if (!WordRules.isFiller(note)) NoteText(note)
            EnrichmentNote(item.enrichment)
            LinguisticNote(item.linguistic)
            LibraryAiNote(item.aiKey(), uiState)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlineButton("讲解", onAsk, modifier = Modifier.height(52.dp))
                InkButton("练这个词", onLearn, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 斩 as a small ink seal: hollow at rest, filled once a swipe is past the line. */
@Composable
internal fun SealMark(armed: Boolean, tint: androidx.compose.ui.graphics.Color, fill: androidx.compose.ui.graphics.Color = tint, size: androidx.compose.ui.unit.Dp = 28.dp) {
    val colors = AjlTheme.colors
    Box(
        Modifier
            .size(size)
            .background(if (armed) fill else androidx.compose.ui.graphics.Color.Transparent, AjlShape.Panel)
            .border(AjlStroke.Ink, tint, AjlShape.Panel),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "斩",
            style = AjlTheme.type.label.copy(fontSize = (size.value * 0.54f).sp, fontWeight = FontWeight.Bold),
            color = if (armed) (if (fill == colors.onInk) colors.ink else colors.onInk) else tint,
        )
    }
}

@Composable
private fun WordStage(
    dial: FormDial?,
    at: Int,
    surface: String,
    reading: String,
    meaning: String,
    speaking: Boolean,
    onTapWord: () -> Unit,
    onStep: (Int) -> Unit,
    onCell: (Int) -> Unit,
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val work = AjlTheme.work
    val form = dial?.forms?.getOrNull(at)
    val reduced = rememberReducedMotion()
    val swipe = with(LocalDensity.current) { 40.dp.toPx() }
    val drift = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .pointerInput(dial) {
                if (dial == null) return@pointerInput
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total <= -swipe) onStep(1) else if (total >= swipe) onStep(-1)
                        scope.launch { drift.animateTo(0f, tween(MotionTokens.Dur.Release)) }
                    },
                    onDragCancel = { scope.launch { drift.animateTo(0f) } },
                ) { change, delta ->
                    change.consume()
                    total += delta
                    scope.launch { drift.snapTo(total * 0.35f) }
                }
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.weight(1f).heightIn(min = if (dial?.cells?.isNotEmpty() == true) 264.dp else 0.dp).graphicsLayer { translationX = drift.value },
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val shown = form?.value ?: surface
                val big = if (shown.length > 5) 42.sp else 52.sp
                val style = type.jpDisplay.copy(fontSize = big, lineHeight = big * 1.15f, fontWeight = FontWeight.Bold)
                Row(Modifier.clickableNoRipple(onClick = onTapWord).semantics { contentDescription = "发音 $shown" }) {
                    if (form == null) {
                        Text(surface, style = style, color = colors.ink, maxLines = 1)
                    } else {
                        Text(form.stem, style = style, color = colors.ink, maxLines = 1)
                        AnimatedContent(
                            targetState = form.ending,
                            transitionSpec = {
                                if (reduced) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                                else (fadeIn(tween(MotionTokens.Dur.State)) + slideInVertically(tween(MotionTokens.Dur.State)) { it / 4 }) togetherWith fadeOut(tween(90))
                            },
                            label = "ending",
                        ) { ending ->
                            Text(ending, style = style.copy(textDecoration = TextDecoration.Underline), color = work.accent, maxLines = 1)
                        }
                    }
                }
                VoiceBars(active = speaking, color = work.accent)
            }
            val kana = form?.reading ?: reading
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                if (kana.isNotBlank()) Text(kana, style = type.jpBody.copy(fontSize = 17.sp), color = colors.ink2)
                val roma = Kana.romaji(kana)
                if (roma.isNotBlank()) Text(roma, style = type.meta, color = colors.ink3)
            }
            val gloss = if (form == null || form.label == FormDial.Dictionary) meaning else form.gloss.ifBlank { meaning }
            if (gloss.isNotBlank()) Text(gloss, style = type.title.copy(fontSize = 20.sp, lineHeight = 28.sp), color = colors.ink)
            if (form != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    Text(form.label, style = type.label.copy(fontWeight = FontWeight.SemiBold), color = work.accent)
                    if (form.rule.isNotBlank()) Text(form.rule, style = type.meta, color = colors.ink3)
                }
            }
        }
        if (dial != null && dial.cells.isNotEmpty()) {
            Column(
                Modifier
                    .width(52.dp)
                    .fillMaxHeight()
                    .clip(AjlShape.Panel)
                    .border(AjlStroke.Ink, colors.ink, AjlShape.Panel),
            ) {
                dial.cells.forEachIndexed { index, cell ->
                    val on = form?.cell == index
                    if (index > 0) Box(Modifier.fillMaxWidth().height(AjlStroke.Hair).background(colors.line))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .heightIn(min = if (dial.cells.size > 7) 34.dp else 44.dp)
                            .background(if (on) work.accent else colors.surface)
                            .clickableNoRipple(onClick = { onCell(index) })
                            .semantics { contentDescription = "${cell.kana} 段" },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            cell.kana,
                            style = type.jpTitle.copy(fontSize = 20.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
                            color = if (on) work.onAccent else colors.ink,
                        )
                        if (cell.tag.isNotEmpty()) {
                            Text(cell.tag, style = type.metaSmall.copy(fontSize = 9.sp, lineHeight = 10.sp), color = if (on) work.onAccent else colors.ink3)
                        }
                    }
                }
            }
        }
    }
}

/** Where in the example line the word (in whichever form it appears) sits. */
private fun exampleMark(text: String, surface: String, dial: FormDial?): IntRange? {
    val needles = buildList {
        dial?.forms?.map { it.value }?.sortedByDescending { it.length }?.let { addAll(it) }
        add(surface.trim())
        dial?.forms?.firstOrNull()?.stem?.takeIf { it.isNotEmpty() }?.let { add(it) }
    }
    needles.forEach { needle ->
        if (needle.isEmpty()) return@forEach
        val start = text.indexOf(needle)
        if (start >= 0) return start until start + needle.length
    }
    return null
}
