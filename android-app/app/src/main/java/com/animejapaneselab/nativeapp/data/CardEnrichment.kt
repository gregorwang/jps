package com.animejapaneselab.nativeapp.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * The AI study-card payload (`cardPayload`) the Worker attaches to vocab / grammar / sentence
 * rows. A good share of the stored payloads is template filler (and some leak internal ids), so
 * [parseCardEnrichment] keeps only lines that actually say something about this item.
 */
data class CardEnrichment(
    /** Core meaning / function when it adds to the row's own gloss. */
    val coreZh: String = "",
    /** Grammar: how it attaches (名词/句子 + と仮定して). */
    val structureZh: String = "",
    val usageScenes: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val breakdown: List<CardBreakdownPart> = emptyList(),
    /** Sentence: 意群 split for shadowing. */
    val chunks: List<String> = emptyList(),
    /** Sentence: natural translation when it differs from the row's meaning. */
    val naturalZh: String = "",
    /** Sentence: the tone label (设定说明、定义). */
    val toneZh: String = "",
) {
    val hasContent: Boolean
        get() = coreZh.isNotBlank() || structureZh.isNotBlank() || usageScenes.isNotEmpty() ||
            mistakes.isNotEmpty() || breakdown.isNotEmpty() || chunks.size > 1 || naturalZh.isNotBlank() || toneZh.isNotBlank()
}

data class CardBreakdownPart(val ja: String, val zh: String, val noteZh: String = "")

/** Phrases that only appear in template-generated payloads; any line containing one is dropped. */
private val FillerMarkers = listOf(
    "re-zero-", "k-on-", "rezero-", "【",
    "不要孤立背诵", "区分近义词差异", "进行自然", "第一时间联想", "快速联想", "优先联想", "优先回想",
    "强化词形", "先把这个词作为一个整体认出", "剧中高频", "切忌与近义表达硬套", "而非孤立翻译",
    "对比中日文原句", "注意台词中的省略与停顿", "适合跟读记忆", "动漫高频台词", "日常口语通用",
    "在台词中识别它的基本意思", "结合前后助词或谓语", "复习时把读音", "先单独读顺", "意群，", "前半句，",
    "后半句，", "声优台词语气自然流畅", "语法核心功能结构", "上下文例句载体", "不仅要背单词释义",
    "场景对话", "在真实日常", "最好整块记", "不要只看中文译文", "注意台词句尾的省略", "不要只认原形",
    "要看它在句子里是在命名", "在台词里要判断它是在说明事实",
)

private fun String.isFiller(): Boolean = FillerMarkers.any { contains(it) }

private fun String.clean(): String = trim().takeUnless { it.isBlank() || it.isFiller() }.orEmpty()

private fun JSONObject.text(vararg keys: String): String =
    keys.firstNotNullOfOrNull { key -> optString(key).takeIf { it.isNotBlank() && it != "null" } }.orEmpty()

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).clean().ifBlank { null } }

private val QuotedLabel = Regex("""[“"「]([^”"」]{1,24})[”"」]""")

/**
 * [ownGloss] is the row's own meaning / function; a core line that merely repeats it is dropped.
 * [headword] is the vocab surface or grammar pattern; breakdown rows that just restate it go too.
 */
fun parseCardEnrichment(payload: JSONObject?, ownGloss: String = "", headword: String = ""): CardEnrichment? {
    if (payload == null) return null
    val gloss = ownGloss.trim()
    val core = payload.text("coreMeaningZh", "coreFunctionZh").clean()
        .takeUnless { it == gloss || gloss.contains(it) }
        .orEmpty()
    val breakdown = when (val raw = payload.opt("exampleBreakdown")) {
        is JSONArray -> (0 until raw.length()).mapNotNull { raw.optJSONObject(it) }
        is JSONObject -> listOf(raw)
        else -> emptyList()
    }.mapNotNull { part ->
        val ja = part.text("ja").clean()
        val zh = part.text("zh").clean()
        if (ja.isBlank() || zh.isBlank() || ja == headword || zh == gloss) return@mapNotNull null
        CardBreakdownPart(ja = ja, zh = zh, noteZh = part.text("pointZh", "noteZh").clean())
    }
    // Some cards were generated for the wrong word; a breakdown that never mentions the headword
    // marks the whole card as suspect, so its breakdown and mistakes are dropped.
    val stem = headword.filterNot { it == '〜' || it == '～' || it.isWhitespace() }.take(2)
    val consistent = breakdown.isEmpty() || stem.isEmpty() || headword.length > 12 || breakdown.any { it.ja.contains(stem) }
    val chunks = (payload.optJSONArray("chunks") ?: JSONArray()).let { array ->
        (0 until array.length()).mapNotNull { array.optJSONObject(it)?.text("ja")?.trim()?.ifBlank { null } }
    }
    val natural = payload.text("naturalZh").clean().takeUnless { it == gloss }.orEmpty()
    val tone = QuotedLabel.find(payload.text("toneZh"))?.groupValues?.get(1)?.trim().orEmpty()
    val parsed = CardEnrichment(
        coreZh = core,
        structureZh = payload.text("structureZh").trim().takeUnless { it == headword || it.isFiller() }.orEmpty(),
        usageScenes = payload.optJSONArray("usageScenes").strings().filterNot { it == gloss },
        mistakes = if (consistent) payload.optJSONArray("commonMistakes").strings() else emptyList(),
        breakdown = if (consistent) breakdown else emptyList(),
        chunks = chunks,
        naturalZh = natural,
        toneZh = tone,
    )
    return parsed.takeIf { it.hasContent }
}
