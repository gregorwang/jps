package com.animejapaneselab.nativeapp.data

private const val ReZeroShadowingCdnBase = "https://cdn.xn--cckl9nsb.com"
private const val FlaggedSuffix = ".unmatched_or_ineligible.mp3"

data class ShadowingAudioSource(
    val url: String,
    val isFlagged: Boolean,
)

fun buildReZeroShadowingAudio(
    sentenceId: String,
    audioUrl: String,
    storagePath: String,
): ShadowingAudioSource? {
    buildExplicitShadowingAudio(audioUrl, storagePath)?.let { return it }

    buildModernSeason3ShadowingAudio(sentenceId)?.let { return it }

    val episodeSlug = sentenceId.split("-").getOrNull(2) ?: return null
    val episode = parseEpisodeSlug(episodeSlug) ?: return null
    return when (episode.season) {
        1, 2 -> {
            val prefix = if (episode.season == 1) "rezeroS1" else "rezeroS2"
            val isFlagged = episode.season == 1 && flaggedSentenceIds.contains(sentenceId)
            val flaggedPart = if (isFlagged) ".unmatched_or_ineligible" else ""
            ShadowingAudioSource(
                url = "$ReZeroShadowingCdnBase/$prefix/$episodeSlug/$sentenceId$flaggedPart.mp3",
                isFlagged = isFlagged,
            )
        }

        3 -> {
            val sentenceNumber = Regex("""sentence-(\d+)$""", RegexOption.IGNORE_CASE)
                .find(sentenceId)
                ?.groupValues
                ?.getOrNull(1)
                ?.padStart(3, '0')
                ?: return null
            val globalEpisode = 50 + episode.episode
            val seasonPadded = episode.season.toString().padStart(2, '0')
            val filename = "rezero_s${seasonPadded}e${globalEpisode}_v9_sent_$sentenceNumber.mp3"
            ShadowingAudioSource(
                url = "$ReZeroShadowingCdnBase/rezeroS3/$episodeSlug/$filename",
                isFlagged = false,
            )
        }

        else -> null
    }
}

fun buildExplicitShadowingAudio(
    audioUrl: String,
    storagePath: String,
): ShadowingAudioSource? {
    val dbUrl = normalizeDbAudioValue(audioUrl) ?: normalizeDbAudioValue(storagePath)
    return dbUrl?.let {
        ShadowingAudioSource(
            url = it,
            isFlagged = it.contains(FlaggedSuffix),
        )
    }
}

fun promptAudioForSentence(
    workSlug: String,
    sentence: ShadowingSentence,
    autoPlay: Boolean,
): PromptAudio {
    val explicitSource = buildExplicitShadowingAudio(
        audioUrl = sentence.audioUrl,
        storagePath = sentence.storagePath,
    )
    if (explicitSource != null) {
        return promptSourceAudio(explicitSource, sentence, autoPlay)
    }

    if (workSlug == "re-zero") {
        val source = buildReZeroShadowingAudio(
            sentenceId = sentence.id,
            audioUrl = "",
            storagePath = "",
        )
        if (source != null) {
            return promptSourceAudio(source, sentence, autoPlay)
        }
    }
    return PromptAudio.Tts(sentence.ja, autoPlay = autoPlay, label = "播放标准语音")
}

private fun promptSourceAudio(
    source: ShadowingAudioSource,
    sentence: ShadowingSentence,
    autoPlay: Boolean,
): PromptAudio.Source {
    val reliability = if (source.isFlagged) AudioReliability.Flagged else AudioReliability.Verified
    return PromptAudio.Source(
        url = source.url,
        autoPlay = autoPlay && !source.isFlagged,
        reliability = reliability,
        fallbackTtsText = sentence.ja,
    )
}

private fun buildModernSeason3ShadowingAudio(sentenceId: String): ShadowingAudioSource? {
    val match = Regex("""^rezero_s(\d+)e(\d+)_v\d+_sent_\d+$""", RegexOption.IGNORE_CASE).find(sentenceId)
        ?: return null
    val season = match.groupValues[1].toIntOrNull() ?: return null
    val globalEpisode = match.groupValues[2].toIntOrNull() ?: return null
    if (season != 3) return null
    val episodeInSeason = globalEpisode - 50
    if (episodeInSeason < 1) return null
    val episodeSlug = "s${season.toString().padStart(2, '0')}e${episodeInSeason.toString().padStart(2, '0')}"
    return ShadowingAudioSource(
        url = "$ReZeroShadowingCdnBase/rezeroS3/$episodeSlug/$sentenceId.mp3",
        isFlagged = false,
    )
}

private data class EpisodeSlug(val season: Int, val episode: Int)

private fun parseEpisodeSlug(slug: String): EpisodeSlug? {
    val match = Regex("""^s(\d+)e(\d+)$""", RegexOption.IGNORE_CASE).find(slug) ?: return null
    return EpisodeSlug(
        season = match.groupValues[1].toIntOrNull() ?: return null,
        episode = match.groupValues[2].toIntOrNull() ?: return null,
    )
}

private fun normalizeDbAudioValue(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    return "$ReZeroShadowingCdnBase/${trimmed.trimStart('/')}"
}

private val flaggedSentenceIds = setOf(
    "re-zero-s01e03-sentence-003",
    "re-zero-s01e03-sentence-004",
    "re-zero-s01e03-sentence-010",
    "re-zero-s01e03-sentence-021",
    "re-zero-s01e03-sentence-023",
    "re-zero-s01e03-sentence-025",
    "re-zero-s01e04-sentence-016",
    "re-zero-s01e04-sentence-028",
    "re-zero-s01e04-sentence-036",
    "re-zero-s01e05-sentence-002",
    "re-zero-s01e05-sentence-007",
    "re-zero-s01e05-sentence-009",
    "re-zero-s01e05-sentence-017",
    "re-zero-s01e05-sentence-023",
    "re-zero-s01e05-sentence-033",
    "re-zero-s01e05-sentence-041",
    "re-zero-s01e05-sentence-054",
    "re-zero-s01e06-sentence-007",
    "re-zero-s01e06-sentence-009",
    "re-zero-s01e06-sentence-010",
    "re-zero-s01e06-sentence-023",
    "re-zero-s01e06-sentence-047",
    "re-zero-s01e06-sentence-050",
    "re-zero-s01e06-sentence-054",
    "re-zero-s01e06-sentence-059",
    "re-zero-s01e06-sentence-062",
    "re-zero-s01e07-sentence-001",
    "re-zero-s01e07-sentence-021",
    "re-zero-s01e07-sentence-025",
    "re-zero-s01e07-sentence-030",
    "re-zero-s01e07-sentence-038",
    "re-zero-s01e07-sentence-044",
    "re-zero-s01e07-sentence-056",
    "re-zero-s01e07-sentence-058",
    "re-zero-s01e08-sentence-010",
    "re-zero-s01e08-sentence-015",
    "re-zero-s01e08-sentence-018",
    "re-zero-s01e08-sentence-024",
    "re-zero-s01e08-sentence-025",
    "re-zero-s01e08-sentence-031",
    "re-zero-s01e08-sentence-035",
    "re-zero-s01e08-sentence-036",
    "re-zero-s01e08-sentence-037",
    "re-zero-s01e08-sentence-047",
    "re-zero-s01e08-sentence-056",
    "re-zero-s01e08-sentence-063",
    "re-zero-s01e08-sentence-070",
    "re-zero-s01e08-sentence-072",
    "re-zero-s01e08-sentence-073",
    "re-zero-s01e08-sentence-076",
    "re-zero-s01e08-sentence-081",
    "re-zero-s01e08-sentence-084",
    "re-zero-s01e08-sentence-085",
    "re-zero-s01e08-sentence-089",
    "re-zero-s01e08-sentence-091",
    "re-zero-s01e08-sentence-093",
    "re-zero-s01e08-sentence-094",
    "re-zero-s01e09-sentence-002",
    "re-zero-s01e09-sentence-008",
    "re-zero-s01e09-sentence-050",
    "re-zero-s01e09-sentence-055",
    "re-zero-s01e09-sentence-056",
    "re-zero-s01e09-sentence-059",
    "re-zero-s01e09-sentence-063",
    "re-zero-s01e10-sentence-002",
    "re-zero-s01e10-sentence-012",
    "re-zero-s01e10-sentence-013",
    "re-zero-s01e10-sentence-038",
    "re-zero-s01e10-sentence-063",
    "re-zero-s01e10-sentence-073",
    "re-zero-s01e10-sentence-074",
    "re-zero-s01e10-sentence-082",
    "re-zero-s01e10-sentence-092",
    "re-zero-s01e11-sentence-023",
    "re-zero-s01e11-sentence-029",
    "re-zero-s01e11-sentence-030",
    "re-zero-s01e11-sentence-059",
    "re-zero-s01e11-sentence-061",
    "re-zero-s01e11-sentence-062",
    "re-zero-s01e11-sentence-084",
    "re-zero-s01e11-sentence-086",
    "re-zero-s01e11-sentence-096",
    "re-zero-s01e12-sentence-006",
    "re-zero-s01e12-sentence-014",
    "re-zero-s01e12-sentence-020",
    "re-zero-s01e12-sentence-028",
    "re-zero-s01e12-sentence-031",
    "re-zero-s01e12-sentence-039",
    "re-zero-s01e12-sentence-050",
    "re-zero-s01e12-sentence-058",
    "re-zero-s01e12-sentence-062",
    "re-zero-s01e12-sentence-063",
    "re-zero-s01e12-sentence-075",
    "re-zero-s01e12-sentence-084",
    "re-zero-s01e13-sentence-002",
    "re-zero-s01e13-sentence-003",
    "re-zero-s01e13-sentence-010",
    "re-zero-s01e13-sentence-012",
    "re-zero-s01e13-sentence-016",
    "re-zero-s01e13-sentence-019",
    "re-zero-s01e13-sentence-023",
    "re-zero-s01e13-sentence-024",
    "re-zero-s01e13-sentence-025",
    "re-zero-s01e13-sentence-045",
    "re-zero-s01e13-sentence-055",
    "re-zero-s01e13-sentence-057",
    "re-zero-s01e13-sentence-069",
    "re-zero-s01e13-sentence-080",
    "re-zero-s01e14-sentence-031",
    "re-zero-s01e14-sentence-035",
    "re-zero-s01e14-sentence-046",
    "re-zero-s01e14-sentence-057",
    "re-zero-s01e14-sentence-059",
    "re-zero-s01e14-sentence-063",
    "re-zero-s01e14-sentence-077",
    "re-zero-s01e14-sentence-083",
    "re-zero-s01e14-sentence-096",
    "re-zero-s01e15-sentence-033",
    "re-zero-s01e15-sentence-040",
    "re-zero-s01e15-sentence-043",
    "re-zero-s01e15-sentence-050",
    "re-zero-s01e15-sentence-053",
    "re-zero-s01e15-sentence-055",
    "re-zero-s01e15-sentence-056",
    "re-zero-s01e15-sentence-059",
    "re-zero-s01e15-sentence-066",
    "re-zero-s01e15-sentence-074",
    "re-zero-s01e15-sentence-079",
    "re-zero-s01e15-sentence-081",
    "re-zero-s01e15-sentence-085",
    "re-zero-s01e15-sentence-086",
    "re-zero-s01e15-sentence-089",
    "re-zero-s01e15-sentence-094",
    "re-zero-s01e16-sentence-003",
    "re-zero-s01e16-sentence-006",
    "re-zero-s01e16-sentence-024",
    "re-zero-s01e16-sentence-028",
    "re-zero-s01e16-sentence-029",
    "re-zero-s01e16-sentence-032",
    "re-zero-s01e16-sentence-034",
    "re-zero-s01e16-sentence-037",
    "re-zero-s01e16-sentence-041",
    "re-zero-s01e16-sentence-043",
    "re-zero-s01e16-sentence-050",
    "re-zero-s01e16-sentence-054",
    "re-zero-s01e16-sentence-057",
    "re-zero-s01e16-sentence-059",
    "re-zero-s01e16-sentence-060",
    "re-zero-s01e16-sentence-062",
    "re-zero-s01e16-sentence-071",
    "re-zero-s01e16-sentence-073",
    "re-zero-s01e16-sentence-081",
    "re-zero-s01e17-sentence-002",
    "re-zero-s01e17-sentence-011",
    "re-zero-s01e17-sentence-024",
    "re-zero-s01e17-sentence-028",
    "re-zero-s01e17-sentence-029",
    "re-zero-s01e17-sentence-032",
    "re-zero-s01e17-sentence-042",
    "re-zero-s01e17-sentence-065",
    "re-zero-s01e17-sentence-069",
    "re-zero-s01e17-sentence-091",
    "re-zero-s01e17-sentence-094",
    "re-zero-s01e17-sentence-095",
    "re-zero-s01e18-sentence-002",
    "re-zero-s01e18-sentence-008",
    "re-zero-s01e18-sentence-012",
    "re-zero-s01e18-sentence-015",
    "re-zero-s01e18-sentence-021",
    "re-zero-s01e18-sentence-047",
    "re-zero-s01e18-sentence-048",
    "re-zero-s01e18-sentence-050",
    "re-zero-s01e18-sentence-058",
    "re-zero-s01e18-sentence-061",
    "re-zero-s01e18-sentence-064",
    "re-zero-s01e18-sentence-065",
    "re-zero-s01e18-sentence-089",
    "re-zero-s01e19-sentence-027",
    "re-zero-s01e19-sentence-029",
    "re-zero-s01e19-sentence-033",
    "re-zero-s01e19-sentence-042",
    "re-zero-s01e19-sentence-046",
    "re-zero-s01e19-sentence-047",
    "re-zero-s01e19-sentence-058",
    "re-zero-s01e19-sentence-071",
    "re-zero-s01e19-sentence-074",
    "re-zero-s01e19-sentence-082",
    "re-zero-s01e19-sentence-093",
    "re-zero-s01e19-sentence-096",
    "re-zero-s01e20-sentence-009",
    "re-zero-s01e20-sentence-010",
    "re-zero-s01e20-sentence-014",
    "re-zero-s01e20-sentence-017",
    "re-zero-s01e20-sentence-020",
    "re-zero-s01e20-sentence-025",
    "re-zero-s01e20-sentence-026",
    "re-zero-s01e20-sentence-036",
    "re-zero-s01e20-sentence-043",
    "re-zero-s01e20-sentence-044",
    "re-zero-s01e20-sentence-045",
    "re-zero-s01e20-sentence-046",
    "re-zero-s01e20-sentence-047",
    "re-zero-s01e20-sentence-048",
    "re-zero-s01e20-sentence-049",
    "re-zero-s01e20-sentence-052",
    "re-zero-s01e20-sentence-053",
    "re-zero-s01e20-sentence-054",
    "re-zero-s01e20-sentence-058",
    "re-zero-s01e20-sentence-069",
    "re-zero-s01e20-sentence-076",
    "re-zero-s01e20-sentence-082",
    "re-zero-s01e20-sentence-083",
    "re-zero-s01e21-sentence-005",
    "re-zero-s01e21-sentence-015",
    "re-zero-s01e21-sentence-017",
    "re-zero-s01e21-sentence-022",
    "re-zero-s01e21-sentence-030",
    "re-zero-s01e21-sentence-035",
    "re-zero-s01e21-sentence-042",
    "re-zero-s01e21-sentence-043",
    "re-zero-s01e21-sentence-046",
    "re-zero-s01e21-sentence-049",
    "re-zero-s01e21-sentence-053",
    "re-zero-s01e21-sentence-060",
    "re-zero-s01e21-sentence-063",
    "re-zero-s01e21-sentence-068",
    "re-zero-s01e21-sentence-086",
    "re-zero-s01e21-sentence-093",
    "re-zero-s01e21-sentence-096",
    "re-zero-s01e22-sentence-020",
    "re-zero-s01e22-sentence-023",
    "re-zero-s01e22-sentence-031",
    "re-zero-s01e22-sentence-036",
    "re-zero-s01e22-sentence-055",
    "re-zero-s01e22-sentence-069",
    "re-zero-s01e22-sentence-071",
    "re-zero-s01e22-sentence-073",
    "re-zero-s01e22-sentence-076",
    "re-zero-s01e22-sentence-079",
    "re-zero-s01e22-sentence-083",
    "re-zero-s01e23-sentence-028",
    "re-zero-s01e23-sentence-029",
    "re-zero-s01e23-sentence-030",
    "re-zero-s01e23-sentence-043",
    "re-zero-s01e23-sentence-045",
    "re-zero-s01e23-sentence-061",
    "re-zero-s01e23-sentence-064",
    "re-zero-s01e23-sentence-069",
    "re-zero-s01e23-sentence-071",
    "re-zero-s01e23-sentence-078",
    "re-zero-s01e23-sentence-081",
    "re-zero-s01e23-sentence-082",
    "re-zero-s01e24-sentence-001",
    "re-zero-s01e24-sentence-012",
    "re-zero-s01e24-sentence-015",
    "re-zero-s01e24-sentence-031",
    "re-zero-s01e24-sentence-035",
    "re-zero-s01e24-sentence-038",
    "re-zero-s01e24-sentence-046",
    "re-zero-s01e24-sentence-047",
    "re-zero-s01e24-sentence-056",
    "re-zero-s01e24-sentence-061",
    "re-zero-s01e24-sentence-062",
    "re-zero-s01e24-sentence-063",
    "re-zero-s01e24-sentence-068",
    "re-zero-s01e24-sentence-069",
    "re-zero-s01e24-sentence-076",
    "re-zero-s01e24-sentence-077",
    "re-zero-s01e24-sentence-083",
    "re-zero-s01e25-sentence-004",
    "re-zero-s01e25-sentence-006",
    "re-zero-s01e25-sentence-022",
    "re-zero-s01e25-sentence-035",
    "re-zero-s01e25-sentence-041",
    "re-zero-s01e25-sentence-047",
    "re-zero-s01e25-sentence-050",
    "re-zero-s01e25-sentence-056",
    "re-zero-s01e25-sentence-073",
    "re-zero-s01e25-sentence-074",
    "re-zero-s01e25-sentence-078",
    "re-zero-s01e25-sentence-080",
    "re-zero-s01e25-sentence-086",
    "re-zero-s01e25-sentence-087",
    "re-zero-s01e25-sentence-088",
    "re-zero-s01e25-sentence-089",
    "re-zero-s01e25-sentence-090",
    "re-zero-s01e25-sentence-091",
    "re-zero-s01e25-sentence-092",
    "re-zero-s01e25-sentence-093",
    "re-zero-s01e25-sentence-094",
    "re-zero-s01e25-sentence-095",
    "re-zero-s01e25-sentence-096",
)
