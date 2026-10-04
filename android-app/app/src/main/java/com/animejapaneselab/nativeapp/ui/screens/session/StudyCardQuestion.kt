package com.animejapaneselab.nativeapp.ui.screens.session

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.PromptAudio
import com.animejapaneselab.nativeapp.data.StudyCardNode
import com.animejapaneselab.nativeapp.data.StudyFact
import com.animejapaneselab.nativeapp.ui.audio.AudioPlaybackPhase
import com.animejapaneselab.nativeapp.ui.design.CoveredLine
import com.animejapaneselab.nativeapp.ui.design.LabeledNote
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.TagChip
import com.animejapaneselab.nativeapp.ui.design.VoiceSwitchPill
import com.animejapaneselab.nativeapp.ui.voicepack.rememberVoiceOptions
import com.animejapaneselab.nativeapp.ui.design.WordBlocks
import com.animejapaneselab.nativeapp.ui.design.rememberVoicePhase
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.motion.MotionTokens
import com.animejapaneselab.nativeapp.ui.reading.FuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.reading.Kana
import com.animejapaneselab.nativeapp.ui.reading.LineReading
import com.animejapaneselab.nativeapp.ui.reading.ReadingLineText
import com.animejapaneselab.nativeapp.ui.reading.RubyText
import com.animejapaneselab.nativeapp.ui.reading.rememberFuriganaAnnotator
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.words.KnownWords
import com.animejapaneselab.nativeapp.ui.words.VocabCards
import com.animejapaneselab.nativeapp.ui.words.WordRules

/**
 * 学习卡 — not a question. One thing to learn, and the card is titled by it: the pattern
 * (〜てみせる), the word (諦める) or the line itself. Fixed order: what it is → its parts as word
 * blocks → the anime line (reading aids, target marked, 原声 / TTS pill) → at most a few
 * labelled notes. Unlabelled lines and the 言語学 addendum stay off the card.
 */
@Composable
internal fun StudyCardQuestion(
    env: LessonQuestionEnv,
    node: StudyCardNode,
    settings: LabSettings,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val furigana = rememberFuriganaAnnotator(settings)
    val aided = settings.showFurigana || settings.showRomaji
    LaunchedEffect(node.japanese, aided) {
        if (aided) furigana.request("sentence", listOf(node.japanese))
    }
    val hasSource = node.audio is PromptAudio.Source
    val voices = rememberVoiceOptions(node.japanese, hasSource)
    val cue = voices.cue(node.audio.takeIf { hasSource }, node.japanese)
    val voice = Voice(
        playing = env.playback.phase == AudioPlaybackPhase.Playing || env.playback.phase == AudioPlaybackPhase.Loading,
        options = voices.labels,
        selected = voices.index,
        onSelect = voices.onSelect,
        onPlay = { env.onPlay(cue) },
    )
    val kind = when (node.sourceKind) {
        "grammar" -> CardKind.Grammar
        "vocab" -> CardKind.Word
        else -> CardKind.Line
    }
    var submitted by remember(node.id) { mutableStateOf(false) }
    val submit = {
        if (!submitted) {
            submitted = true
            env.onSubmit(node.expectedAnswer)
        }
    }
    LessonQuestionScaffold(
        env = env,
        modifier = modifier,
        heading = null,
        eyebrow = "",
        bottomBar = {
            LessonActionBar(
                primaryLabel = if (kind == CardKind.Line) "听懂了" else "记住了",
                onPrimary = submit,
                quietLabel = if (kind == CardKind.Word) "认识 · 斩" else null,
                onQuiet = if (kind == CardKind.Word) {
                    {
                        KnownWords.setWord(context, node.japanese, true)
                        submit()
                    }
                } else {
                    null
                },
            )
        },
    ) {
        when (kind) {
            CardKind.Grammar -> GrammarCard(node, settings, furigana, voice)
            CardKind.Word -> WordCard(node, settings, furigana, voice)
            CardKind.Line -> LineCard(node, settings, furigana, voice)
        }
    }
}

private enum class CardKind { Grammar, Word, Line }

private class Voice(
    val playing: Boolean,
    val options: List<String>,
    val selected: Int,
    val onSelect: (Int) -> Unit,
    val onPlay: () -> Unit,
)

@Composable
private fun VoiceControl(voice: Voice, modifier: Modifier = Modifier) {
    VoiceSwitchPill(
        playing = voice.playing,
        options = voice.options,
        selected = voice.selected,
        onSelect = voice.onSelect,
        onClick = voice.onPlay,
        modifier = modifier,
    )
}

// ------------------------------------------------------------------ 語法

@Composable
private fun GrammarCard(node: StudyCardNode, settings: LabSettings, furigana: FuriganaAnnotator, voice: Voice) {
    val colors = AjlTheme.colors
    val pattern = node.reading.ifBlank { node.prompt }
    Tags(listOf("語法") + node.tags)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(pattern, style = AjlTheme.type.jpDisplay.copy(fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold), color = colors.ink)
        if (node.meaningZh.isNotBlank()) {
            Text(node.meaningZh, style = AjlTheme.type.body.copy(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium), color = colors.ink)
        }
    }
    val parts = remember(node.enrichment) { StudyCardRules.structureParts(node.enrichment?.structureZh.orEmpty()) }
    if (parts.size > 1) WordBlocks(parts.map { it to "" })
    if (node.japanese != pattern) {
        val zh = node.exampleZh.ifBlank { StudyCardRules.translationOf(node) }
        ScenePanel(node.japanese, StudyCardRules.markOf(node.japanese, pattern), settings, furigana, voice) {
            if (zh.isNotBlank()) Translation(zh)
        }
    } else {
        VoiceControl(voice)
    }
    Facts(node, exclude = node.meaningZh)
}

// ------------------------------------------------------------------ 単語

@Composable
private fun WordCard(node: StudyCardNode, settings: LabSettings, furigana: FuriganaAnnotator, voice: Voice) {
    val colors = AjlTheme.colors
    val context = LocalContext.current.applicationContext
    // Vocab rows carry unreliable readings / meanings / notes: the hand-checked card wins.
    val fix = remember(node.id) { VocabCards.get(context, node.sourceId) }
    val reading = fix?.reading?.ifBlank { null } ?: node.reading
    val meaning = fix?.meaning?.ifBlank { null } ?: node.meaningZh
    val tags = if (fix != null) listOfNotNull(fix.pos.ifBlank { null }) + node.tags.filter { Regex("N[1-5]").matches(it) } else node.tags
    Tags(listOf("単語") + tags)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            RubyText(
                text = node.japanese,
                furigana = if (settings.showFurigana) furigana.resultFor(node.japanese) else null,
                style = AjlTheme.type.jpDisplay.copy(fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold),
                rubyStyle = AjlTheme.type.jpLabel,
                color = colors.ink,
                rubyColor = colors.ink3,
            )
            val sound = listOfNotNull(
                reading.takeIf { it.isNotBlank() && it != node.japanese },
                reading.takeIf { settings.showRomaji && it.isNotBlank() }?.let { Kana.romaji(Kana.toHiragana(it)) },
            ).joinToString(" · ")
            if (sound.isNotBlank()) Text(sound, style = AjlTheme.type.meta.copy(fontSize = 13.sp), color = colors.ink3)
        }
        VoiceControl(voice, Modifier.padding(bottom = 4.dp))
    }
    Text(meaning, style = AjlTheme.type.body.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium), color = colors.ink)
    val facts = if (fix == null) {
        node.facts
    } else {
        listOfNotNull(
            fix.lemma.takeIf { it.isNotBlank() && it != node.japanese }?.let { lemma ->
                StudyFact("辞书形", lemma + fix.lemmaReading.takeIf { it.isNotBlank() && it != lemma }?.let { "（$it）" }.orEmpty())
            },
            fix.note.takeIf { it.isNotBlank() }?.let { StudyFact("注意", it) },
        )
    }
    FactList(facts.filter { it.text != meaning && !WordRules.isFiller(it.text) })
}

// ------------------------------------------------------------------ 台詞

@Composable
private fun LineCard(node: StudyCardNode, settings: LabSettings, furigana: FuriganaAnnotator, voice: Voice) {
    val enrichment = node.enrichment
    Tags(listOf("台詞") + listOfNotNull(enrichment?.toneZh?.ifBlank { null }))
    var revealed by rememberSaveable(node.id) { mutableStateOf(false) }
    ScenePanel(node.japanese, null, settings, furigana, voice, big = true) {}
    // Listen first: the translation stays covered until tapped.
    if (node.meaningZh.isNotBlank()) {
        if (revealed) {
            Text(node.meaningZh, style = AjlTheme.type.body.copy(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium), color = AjlTheme.colors.ink)
        } else {
            CoveredLine(onReveal = { revealed = true }, label = "先听，再轻点看中文")
        }
    }
    val parts = remember(enrichment) {
        enrichment?.breakdown?.filter { it.ja.isNotBlank() }?.map { it.ja to it.zh }?.takeIf { it.size > 1 }
            ?: enrichment?.chunks?.filter { it.isNotBlank() }?.map { it to "" }?.takeIf { it.size > 1 }
            .orEmpty()
    }
    if (parts.isNotEmpty()) WordBlocks(parts, accentIndex = -1)
    val natural = enrichment?.naturalZh?.takeIf { it.isNotBlank() && it != node.meaningZh }
    FactList(listOfNotNull(natural?.let { StudyFact("意译", it) }) + node.facts)
    enrichment?.mistakes?.filter { it.isNotBlank() }?.forEach { LabeledNote("易错", it, labelColor = AjlTheme.colors.bad) }
}

// ------------------------------------------------------------------ parts

@Composable
private fun Tags(tags: List<String>) {
    val shown = tags.filter { it.isNotBlank() }.distinct()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        shown.forEachIndexed { i, tag -> TagChip(tag, accent = i == 0, japanese = i == 0) }
    }
}

/**
 * The anime line as a manga panel: reading aids, the target marked, the 原声 / TTS pill; the
 * corner screentone swells while it speaks.
 */
@Composable
private fun ScenePanel(
    text: String,
    mark: IntRange?,
    settings: LabSettings,
    furigana: FuriganaAnnotator,
    voice: Voice,
    big: Boolean = false,
    below: @Composable () -> Unit,
) {
    val work = AjlTheme.work
    val pulse by rememberVoicePhase(voice.playing, periodMillis = 700)
    val swell by animateFloatAsState(if (voice.playing) 1f else 0f, tween(MotionTokens.Dur.Progress, easing = MotionTokens.Ease.Standard), label = "tone-swell")
    val line = remember(text, furigana.resultFor(text)) { LineReading.build(text, furigana.resultFor(text)) }
    val size = if (big) 25 else if (text.length > 16) 21 else 24
    MangaPanel(Modifier.fillMaxWidth()) {
        val beat = kotlin.math.sin(pulse * 2 * Math.PI).toFloat() * 0.5f + 0.5f
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(width = 140.dp + 60.dp * swell, height = 64.dp + 36.dp * swell)
                .screentone(work.tone(0.22f + swell * (0.08f + 0.12f * beat))),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ReadingLineText(
                line,
                mark,
                showRuby = settings.showFurigana,
                showRomaji = settings.showRomaji,
                style = AjlTheme.type.jpBody.copy(fontSize = size.sp, lineHeight = (size * 1.6f).sp, fontWeight = FontWeight.Medium),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { below() }
                VoiceControl(voice)
            }
        }
    }
}

@Composable
private fun Translation(zh: String) {
    Text(zh, style = AjlTheme.type.body, color = AjlTheme.colors.ink)
}

@Composable
private fun Facts(node: StudyCardNode, exclude: String) {
    FactList(node.facts.filter { it.text != exclude && !WordRules.isFiller(it.text) })
    node.enrichment?.mistakes?.filter { it.isNotBlank() }?.forEach { LabeledNote("易错", it, labelColor = AjlTheme.colors.bad) }
}

@Composable
private fun FactList(facts: List<StudyFact>) {
    if (facts.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        facts.distinctBy { it.text }.forEach { LabeledNote(it.label, it.text, labelColor = if (it.label == "场景") AjlTheme.colors.ink3 else AjlTheme.colors.ink) }
    }
}

internal object StudyCardRules {
    /** 「动词て形 + みせる」 → [动词て形, みせる]; one part when it does not split. */
    fun structureParts(structure: String): List<String> =
        structure.split('+', '＋').map { it.trim() }.filter { it.isNotBlank() }

    /** Where the pattern (〜てみせる → てみせる) sits in the line; shorter tails as a fallback. */
    fun markOf(line: String, pattern: String): IntRange? {
        val core = pattern.trim().trimStart('〜', '～', '~').substringBefore('／').substringBefore('/').trim()
        if (core.isEmpty()) return null
        var probe = core
        while (probe.length >= 2) {
            val at = line.indexOf(probe)
            if (at >= 0) return at until at + probe.length
            probe = probe.drop(1)
        }
        return null
    }

    /** The line's Chinese from the enrichment: natural translation, or the breakdown row that is the whole line. */
    fun translationOf(node: StudyCardNode): String {
        val e = node.enrichment ?: return ""
        return e.naturalZh.ifBlank { e.breakdown.firstOrNull { it.ja.trim() == node.japanese.trim() }?.zh.orEmpty() }
    }
}
