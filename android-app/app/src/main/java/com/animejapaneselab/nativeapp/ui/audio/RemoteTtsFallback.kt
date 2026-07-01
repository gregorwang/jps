package com.animejapaneselab.nativeapp.ui.audio

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val GoogleTranslateTtsBaseUrl = "https://translate.googleapis.com/translate_tts"
internal const val GoogleTranslateTtsMaxChars = 180

internal fun buildGoogleTranslateTtsUrl(text: String): String {
    val clean = text.trim()
    require(clean.isNotBlank()) { "语音文本为空" }
    require(clean.length <= GoogleTranslateTtsMaxChars) {
        "语音文本过长：${clean.length}"
    }
    val encodedText = URLEncoder.encode(clean, StandardCharsets.UTF_8.name())
    return "$GoogleTranslateTtsBaseUrl?client=gtx&ie=UTF-8&tl=ja&q=$encodedText"
}
