package com.animejapaneselab.nativeapp.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.design.Hairline
import com.animejapaneselab.nativeapp.ui.design.NoteText
import com.animejapaneselab.nativeapp.ui.knowledge.KnowExample
import com.animejapaneselab.nativeapp.ui.knowledge.KnowKind
import com.animejapaneselab.nativeapp.ui.knowledge.KnowQuiz
import com.animejapaneselab.nativeapp.ui.knowledge.KnowledgeCard
import com.animejapaneselab.nativeapp.ui.knowledge.VocabWord
import com.animejapaneselab.nativeapp.ui.reading.FuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.reading.Kana
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.reading.ReadingLineText
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme

/*
 * 知識 card bodies: everything the point has is laid out on the card itself — no sheet, no jump to
 * a related card (the related cards simply come next in the feed). Long cards scroll inside.
 */

/** Room on the right for the rail of buttons. */
private val RailRoom = 54.dp

@Composable
internal fun ColumnScope.KnowBody(card: KnowledgeCard, romaji: FuriganaAnnotator?, onAnswer: (quiz: KnowQuiz, right: Boolean) -> Unit) {
    val colors = AjlTheme.colors
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 18.dp, end = RailRoom, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        card.prev?.let { PrevStrip(it) }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (card.title.isNotBlank() && (card.v2 || card.kind != KnowKind.Quiz)) {
                Text(
                    marked(card.title, AjlTheme.work.accent),
                    style = AjlTheme.type.jpTitle.copy(fontSize = if (card.v2) 23.sp else 25.sp, lineHeight = if (card.v2) 32.sp else 35.sp, fontWeight = FontWeight.Bold),
                    color = colors.ink,
                )
            }
            if (card.en.isNotBlank()) Text(card.en, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink3)
        }
        if (card.v2) {
            KnowKindBody(card, onAnswer)
            if (card.rule.isNotBlank()) Rule(card.rule.replace("【", "").replace("】", ""))
        } else {
        when (card.kind) {
            KnowKind.Pattern -> {
                if (card.parts.isNotEmpty()) Parts(card)
                card.examples.forEach { Example(it, romaji) }
            }
            KnowKind.Contrast -> Contrast(card, romaji)
            KnowKind.Trap -> Trap(card, romaji)
            KnowKind.Origin -> Origin(card)
            KnowKind.Map -> MapRows(card)
            KnowKind.Quiz -> card.quiz?.let { Quiz(card.id, it, onAnswer) }
            else -> Unit
        }
        if (card.rule.isNotBlank()) Rule(card.rule)
        card.limit?.let { limit ->
            val tint = if (limit.mark == "✕") colors.bad else colors.ink2
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(limit.mark, style = AjlTheme.type.body.copy(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold), color = tint, modifier = Modifier.width(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(limit.ja, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 24.sp), color = tint)
                    Text(limit.why, style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 20.sp), color = colors.ink2)
                }
            }
        }
        card.notes.forEach { note ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(note.h, style = AjlTheme.type.body.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = AjlTheme.work.accent)
                NoteText(note.b)
            }
        }
        }
    }
}

@Composable
private fun Parts(card: KnowledgeCard) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        card.parts.forEachIndexed { i, part ->
            if (i > 0) Text("＋", style = AjlTheme.type.meta.copy(fontSize = 16.sp), color = work.accent, modifier = Modifier.padding(top = 4.dp))
            val tint = if (part.hl) work.accent else colors.ink
            Column(Modifier.width(IntrinsicSize.Max), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(part.w, style = AjlTheme.type.jpTitle.copy(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold), color = tint)
                Box(Modifier.fillMaxWidth().height(2.dp).background(tint))
                if (part.l1.isNotBlank()) Text(part.l1, style = AjlTheme.type.body.copy(fontSize = 12.sp, lineHeight = 16.sp), color = colors.ink2)
                if (part.l2.isNotBlank()) Text(part.l2, style = AjlTheme.type.body.copy(fontSize = 12.sp, lineHeight = 16.sp), color = colors.ink3)
            }
        }
    }
}

/** A Japanese line with its target underlined in [tint] (romaji over each word when on), then Chinese. */
@Composable
private fun Example(ex: KnowExample, romaji: FuriganaAnnotator?, tint: Color = AjlTheme.work.accent, size: Int = 19, struck: Boolean = false) {
    val colors = AjlTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val range = ex.targetRange
        val text = remember(ex, tint, struck) {
            buildAnnotatedString {
                if (range == null) {
                    append(ex.ja)
                } else {
                    append(ex.ja.substring(0, range.first))
                    withStyle(SpanStyle(color = tint, textDecoration = if (struck) TextDecoration.LineThrough else TextDecoration.Underline)) {
                        append(ex.ja.substring(range.first, range.last + 1))
                    }
                    append(ex.ja.substring(range.last + 1))
                }
            }
        }
        val lineStyle = AjlTheme.type.jpBody.copy(fontSize = size.sp, lineHeight = (size * 1.6f).sp, fontWeight = FontWeight.Medium)
        if (romaji != null) {
            LaunchedEffect(ex.ja) { romaji.request("sentence", listOf(ex.ja)) }
            val reading = remember(ex.ja, romaji.resultFor(ex.ja)) { LineReading.build(ex.ja, romaji.resultFor(ex.ja)) }
            ReadingLineText(reading, range, showRuby = false, showRomaji = true, style = lineStyle)
        } else {
            Text(text, style = lineStyle, color = colors.ink)
        }
        if (ex.zh.isNotBlank()) Text(ex.zh, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 22.sp), color = colors.ink)
    }
}

@Composable
private fun Rule(text: String) {
    NoteText(
        text,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(AjlTheme.colors.sunken).padding(horizontal = 14.dp, vertical = 12.dp),
        style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 24.sp),
        color = AjlTheme.colors.ink,
    )
}

@Composable
private fun Contrast(card: KnowledgeCard, romaji: FuriganaAnnotator?) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    Column {
        Hairline(strong = true)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(56.dp))
            Text(card.a, style = AjlTheme.type.jpTitle.copy(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), color = colors.ink, modifier = Modifier.weight(1f))
            Text(card.b, style = AjlTheme.type.jpTitle.copy(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold), color = work.accent, modifier = Modifier.weight(1f))
        }
        card.rows.forEach { row ->
            Hairline()
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.label, style = AjlTheme.type.body.copy(fontSize = 12.sp, lineHeight = 19.sp), color = colors.ink3, modifier = Modifier.width(56.dp))
                Text(row.a, style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 19.sp), color = colors.ink, modifier = Modifier.weight(1f))
                Text(row.b, style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 19.sp), color = colors.ink, modifier = Modifier.weight(1f))
            }
        }
        Hairline()
    }
    card.examples.forEachIndexed { i, ex ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val side = if (i == 0) card.a else card.b
            if (side.isNotBlank()) Text(side, style = AjlTheme.type.jpBody.copy(fontSize = 12.sp), color = if (i == 0) colors.ink3 else work.accent)
            Example(ex, romaji, size = 17)
        }
    }
}

@Composable
private fun Trap(card: KnowledgeCard, romaji: FuriganaAnnotator?) {
    val colors = AjlTheme.colors
    card.examples.forEachIndexed { i, ex ->
        val bad = i == 0
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (bad) "✕" else "○",
                style = AjlTheme.type.body.copy(fontSize = 18.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
                color = if (bad) colors.bad else colors.ok,
                modifier = Modifier.width(20.dp),
            )
            Example(ex, romaji = romaji.takeIf { !bad }, tint = if (bad) colors.bad else colors.ok, size = 18, struck = bad)
        }
    }
}

@Composable
private fun Origin(card: KnowledgeCard) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    card.glyph?.let { g ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(g.text, style = AjlTheme.type.jpTitle.copy(fontSize = if (g.text.length <= 1) 76.sp else 48.sp, lineHeight = 88.sp, fontWeight = FontWeight.Black), color = colors.ink)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(g.reading, style = AjlTheme.type.jpBody.copy(fontSize = 19.sp), color = work.accent)
                Text(g.meaning, style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 19.sp), color = colors.ink2)
            }
        }
    }
    Column {
        card.steps.forEachIndexed { i, step ->
            val last = i == card.steps.lastIndex
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.width(12.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (last) work.accent else colors.surface)
                            .border(1.5.dp, if (last) work.accent else colors.ink, CircleShape),
                    )
                    if (!last) Box(Modifier.weight(1f).width(1.5.dp).background(colors.line2))
                }
                Column(Modifier.padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(step.head, style = AjlTheme.type.jpBody.copy(fontSize = 17.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium), color = if (last) work.accent else colors.ink)
                    NoteText(step.text, style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 20.sp))
                }
            }
        }
    }
}

@Composable
private fun MapRows(card: KnowledgeCard) {
    val colors = AjlTheme.colors
    Column {
        Hairline(strong = true)
        card.rows.forEach { row ->
            Column(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(row.label, style = AjlTheme.type.body.copy(fontSize = 12.sp, lineHeight = 18.sp), color = colors.ink3)
                    Text(row.a, style = AjlTheme.type.jpBody.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium), color = colors.ink, modifier = Modifier.weight(1f, fill = false))
                }
                if (row.b.isNotBlank()) Text(row.b, style = AjlTheme.type.body.copy(fontSize = 13.sp, lineHeight = 19.sp), color = colors.ink2)
            }
            Hairline()
        }
    }
}

@Composable
private fun Quiz(cardId: String, quiz: KnowQuiz, onAnswer: (KnowQuiz, Boolean) -> Unit) {
    val colors = AjlTheme.colors
    var picked by rememberSaveable(cardId) { mutableIntStateOf(-1) }
    val answered = picked >= 0
    val prompt = remember(quiz, answered) {
        buildAnnotatedString {
            append(quiz.before)
            if (answered) {
                withStyle(SpanStyle(color = colors.ok, textDecoration = TextDecoration.Underline)) { append(quiz.options[quiz.answer]) }
            } else {
                append("（　　）")
            }
            append(quiz.after)
        }
    }
    Text(prompt, style = AjlTheme.type.jpBody.copy(fontSize = 23.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium), color = colors.ink)
    if (quiz.zh.isNotBlank()) Text(quiz.zh, style = AjlTheme.type.body.copy(fontSize = 15.sp, lineHeight = 22.sp), color = colors.ink2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        quiz.options.chunked(2).forEachIndexed { r, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { c, option ->
                    val i = r * 2 + c
                    val isAnswer = i == quiz.answer
                    val tint = when {
                        answered && isAnswer -> colors.ok
                        answered && i == picked -> colors.bad
                        else -> colors.ink
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 60.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (answered && isAnswer) colors.okSoft else colors.surface)
                            .border(1.5.dp, tint, RoundedCornerShape(4.dp))
                            .clickable(enabled = !answered, role = Role.Button, onClickLabel = option) {
                                picked = i
                                onAnswer(quiz, i == quiz.answer)
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(option, style = AjlTheme.type.jpTitle.copy(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold), color = tint)
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
    if (answered && quiz.why.isNotBlank()) Rule(quiz.why)
}

/** A word from 資料: headword, reading, meaning and the checked note. */
@Composable
internal fun ColumnScope.VocabBody(word: VocabWord, romaji: Boolean) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val fix = word.fix
    val head = word.head
    val headSize = when {
        head.length <= 3 -> 72
        head.length <= 6 -> 48
        else -> 30
    }
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(end = RailRoom),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Text(head, style = AjlTheme.type.jpTitle.copy(fontSize = headSize.sp, lineHeight = (headSize * 1.25f).sp, fontWeight = FontWeight.Bold), color = colors.ink)
        if (fix.reading.isNotBlank() && fix.reading != head) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(fix.reading, style = AjlTheme.type.jpBody.copy(fontSize = 22.sp), color = work.accent)
                if (romaji) Text(Kana.romaji(fix.reading), style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = colors.ink3, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        Text(fix.meaning, style = AjlTheme.type.title.copy(fontSize = 21.sp, lineHeight = 30.sp), color = colors.ink, modifier = Modifier.padding(top = 8.dp))
        val tags = listOf(fix.pos, fix.lemma.takeIf { it.isNotBlank() && it != head }?.let { "辞书形 $it" }).filterNotNull().filter { it.isNotBlank() }
        if (tags.isNotEmpty()) {
            Text(
                tags.joinToString(" · "),
                style = AjlTheme.type.caption.copy(fontSize = 12.sp),
                color = colors.ink2,
                modifier = Modifier.border(1.dp, colors.line2, RoundedCornerShape(4.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
        if (fix.note.isNotBlank()) NoteText(fix.note, modifier = Modifier.padding(top = 10.dp))
    }
}
