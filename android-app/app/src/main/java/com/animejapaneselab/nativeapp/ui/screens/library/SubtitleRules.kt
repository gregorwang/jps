package com.animejapaneselab.nativeapp.ui.screens.library

import com.animejapaneselab.nativeapp.data.SubtitleLine

/**
 * Pure rules behind the subtitle browser: speaker markers, scene splitting and the LazyColumn
 * row sequence (render and focus-jump share it, so the target index is never counted by hand).
 */

/** A subtitle line split into its speaker marker and the spoken text. */
internal data class SpokenLine(val speaker: String?, val text: String)

private val AssTag = Regex("""\{\\[^}]*\}""")
private val SpeakerMarker = Regex("""^[（(]([^（）()「」\s]{1,12})[）)]\s*""")

/**
 * Netflix CC style lines start with the speaker in brackets: `（エミリア）フフッ`. The marker is
 * lifted into [SpokenLine.speaker]; ASS override tags (`{\an8}`) are dropped. A bracket with
 * nothing after it is a sound cue (（拍手）), not a speaker, so the line is kept whole.
 */
internal fun parseSpokenLine(raw: String): SpokenLine {
    val cleaned = raw.replace(AssTag, "").trim()
    val match = SpeakerMarker.find(cleaned) ?: return SpokenLine(null, cleaned.ifEmpty { raw.trim() })
    val rest = cleaned.substring(match.range.last + 1).trim()
    if (rest.isEmpty()) return SpokenLine(null, cleaned)
    return SpokenLine(match.groupValues[1].trim(), rest)
}

/** One run of continuous dialogue. [number] starts at 1 and doubles as the collapse key. */
internal data class SubtitleScene(
    val number: Int,
    val lines: List<SubtitleLine>,
    val startMillis: Long?,
    val endMillis: Long?,
) {
    /** "MM:SS – MM:SS", or null when no timestamp in the scene parsed. */
    val timeRangeLabel: String?
        get() = if (startMillis == null || endMillis == null) {
            null
        } else {
            "${formatSceneClock(startMillis)} – ${formatSceneClock(maxOf(startMillis, endMillis))}"
        }
}

/** Silence longer than this between two lines starts a new scene. */
internal const val SceneGapMillis = 9_000L

/**
 * "HH:MM:SS,mmm" → millis. Tolerates MM:SS / SS, '.' before the fraction, 1–n fraction digits
 * and trailing junk after a space. Anything unparseable returns null; never throws.
 */
internal fun parseSubtitleTimeMillis(raw: String?): Long? {
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

/** Millis → "MM:SS", locale-independent ASCII. */
internal fun formatSceneClock(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

/** Start time as "12:31"; the raw string when it does not parse. */
internal fun clockLabel(raw: String): String =
    parseSubtitleTimeMillis(raw)?.let(::formatSceneClock) ?: raw.trim()

/**
 * Splits an episode into scenes on silences: (this start − previous end) > [SceneGapMillis].
 * Lines whose time does not parse never open a scene and never move the baseline; an episode
 * with no parseable time is one scene (the caller then shows a flat list).
 */
internal fun splitSubtitleScenes(lines: List<SubtitleLine>): List<SubtitleScene> {
    if (lines.isEmpty()) return emptyList()
    val starts = lines.map { parseSubtitleTimeMillis(it.startTime) }
    val ends = lines.mapIndexed { index, line -> parseSubtitleTimeMillis(line.endTime) ?: starts[index] }
    val boundaries = mutableListOf(0)
    var previousEnd: Long? = null
    for (index in lines.indices) {
        val start = starts[index]
        val prev = previousEnd
        if (index > 0 && start != null && prev != null && start - prev > SceneGapMillis) {
            boundaries += index
        }
        ends[index]?.let { previousEnd = it }
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

/** Case-insensitive find over Japanese and Chinese text; blank query keeps every line. */
internal fun filterSubtitles(lines: List<SubtitleLine>, query: String): List<SubtitleLine> {
    val q = query.trim()
    if (q.isEmpty()) return lines
    return lines.filter { it.jaText.contains(q, ignoreCase = true) || it.zhText.contains(q, ignoreCase = true) }
}

/** Row descriptor for the subtitle LazyColumn. */
internal sealed interface SubtitleRow {
    val key: String
    val contentType: String

    data object Tools : SubtitleRow {
        override val key = "tools"
        override val contentType = "tools"
    }

    data object Notice : SubtitleRow {
        override val key = "notice"
        override val contentType = "notice"
    }

    data object Empty : SubtitleRow {
        override val key = "empty"
        override val contentType = "empty"
    }

    data object NoMatch : SubtitleRow {
        override val key = "no-match"
        override val contentType = "no-match"
    }

    data class SceneHead(val scene: SubtitleScene) : SubtitleRow {
        override val key: String get() = "scene-${scene.number}"
        override val contentType: String get() = "scene"
    }

    data class Line(val line: SubtitleLine) : SubtitleRow {
        override val key: String get() = "${line.lineNo}-${line.startTime}"
        override val contentType: String get() = "line"
    }
}

/**
 * The whole row sequence. Pure: the focus jump replays it with the post-jump inputs (search
 * cleared, target scene expanded) and reads the target's real index.
 */
internal fun buildSubtitleRows(
    hasSubtitles: Boolean,
    visibleLines: List<SubtitleLine>,
    scenes: List<SubtitleScene>,
    grouped: Boolean,
    collapsedScenes: Set<Int>,
    showNotice: Boolean,
): List<SubtitleRow> = buildList {
    add(SubtitleRow.Tools)
    if (showNotice) add(SubtitleRow.Notice)
    when {
        !hasSubtitles -> add(SubtitleRow.Empty)
        visibleLines.isEmpty() -> add(SubtitleRow.NoMatch)
        grouped -> scenes.forEach { scene ->
            add(SubtitleRow.SceneHead(scene))
            if (scene.number !in collapsedScenes) scene.lines.forEach { add(SubtitleRow.Line(it)) }
        }
        else -> visibleLines.forEach { add(SubtitleRow.Line(it)) }
    }
}
