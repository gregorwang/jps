package com.animejapaneselab.nativeapp.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parsing contract for the reading loop (furigana ruby batches) and the explanation loop
 * (AI explain payload, AI history list and AI history detail).
 *
 * Pure JVM test: only org.json + JUnit4, no Android runtime and no network.
 */
class ReadingLayerParsingTest {

    // ---------------------------------------------------------------- furigana batch

    @Test
    fun furiganaBatchParsesRubySegmentsForEveryTargetId() {
        val results = parseFuriganaBatchJson(
            """
            {
              "results": {
                "t0": {
                  "ruby_segments": [
                    {"text": "軽音", "reading": "けいおん"},
                    {"text": "部", "reading": "ぶ"},
                    {"text": "って", "reading": ""}
                  ]
                },
                "t1": {
                  "rubySegments": [
                    {"text": "そろそろ", "reading": ""},
                    {"text": "起", "reading": "お"},
                    {"text": "きないと。", "reading": ""}
                  ]
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals(setOf("t0", "t1"), results.keys)

        val keionbu = requireNotNull(results["t0"])
        assertEquals(3, keionbu.segments.size)
        assertEquals("軽音部って", keionbu.plainText)
        assertEquals(FuriganaSegment("軽音", "けいおん"), keionbu.segments.first())
        assertEquals("", keionbu.segments.last().reading)
        assertTrue(keionbu.hasAnnotation)

        val okinaito = requireNotNull(results["t1"])
        assertEquals("そろそろ起きないと。", okinaito.plainText)
        assertTrue(okinaito.hasAnnotation)
    }

    @Test
    fun furiganaBatchKeepsKanaOnlySpansWithoutAnnotation() {
        val results = parseFuriganaBatchJson(
            """{"results":{"t0":{"ruby_segments":[{"text":"ありがとう"}]}}}""",
        )

        val kanaOnly = requireNotNull(results["t0"])
        assertEquals(1, kanaOnly.segments.size)
        assertEquals("ありがとう", kanaOnly.plainText)
        assertEquals("", kanaOnly.segments.single().reading)
        assertFalse(kanaOnly.hasAnnotation)
    }

    @Test
    fun furiganaBatchDegradesToEmptyMapForMalformedOrUnusablePayloads() {
        val empty = emptyMap<String, FuriganaResult>()

        assertEquals(empty, parseFuriganaBatchJson(""))
        assertEquals(empty, parseFuriganaBatchJson("not json at all"))
        assertEquals(empty, parseFuriganaBatchJson("[]"))
        assertEquals(empty, parseFuriganaBatchJson("""{"ok":true}"""))
        assertEquals(empty, parseFuriganaBatchJson("""{"results":{}}"""))
        // Entry without a segment array, entry that is not an object, and blank-text segments
        // are all skipped instead of producing half-built annotations.
        assertEquals(
            empty,
            parseFuriganaBatchJson(
                """
                {
                  "results": {
                    "t0": {"reading": "けいおん"},
                    "t1": "broken",
                    "t2": {"ruby_segments": [{"text": "", "reading": "よみ"}]}
                  }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun furiganaBatchKeepsUsableEntriesWhileDroppingBrokenNeighbours() {
        val results = parseFuriganaBatchJson(
            """
            {
              "results": {
                "t0": {"ruby_segments": []},
                "t1": {"ruby_segments": [{"text": "日本語", "reading": "にほんご"}]}
              }
            }
            """.trimIndent(),
        )

        assertEquals(setOf("t1"), results.keys)
        assertEquals("日本語", requireNotNull(results["t1"]).plainText)
    }

    // ---------------------------------------------------------------- ai explain

    @Test
    fun aiExplainResultKeepsWorkerTitleSummaryTextAndSections() {
        val result = parseAiExplainResult(
            JSONObject(
                """
                {
                  "title": "AI 精讲",
                  "summary": "先看语气。",
                  "text": "完整讲解正文。",
                  "sections": [
                    {"title": "判断线索", "body": "关系压力来自省略。"},
                    {"title": "可迁移方法", "body": "先看句尾再看助词。"}
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals("AI 精讲", result.title)
        assertEquals("先看语气。", result.summary)
        assertEquals("完整讲解正文。", result.text)
        assertEquals(
            listOf(
                AiExplainSection("判断线索", "关系压力来自省略。"),
                AiExplainSection("可迁移方法", "先看句尾再看助词。"),
            ),
            result.sections,
        )
    }

    @Test
    fun aiExplainResultDropsEmptySectionsAndSynthesizesTextFromSummary() {
        val result = parseAiExplainResult(
            JSONObject(
                """
                {
                  "summary": "先看语气。",
                  "text": "",
                  "sections": [
                    {"title": "判断线索", "body": "关系压力来自省略。"},
                    {"title": "", "body": ""},
                    {"title": "", "body": "没有标题的正文也要保留。"}
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals("智能精讲", result.title)
        assertEquals(
            listOf(
                AiExplainSection("判断线索", "关系压力来自省略。"),
                AiExplainSection("说明", "没有标题的正文也要保留。"),
            ),
            result.sections,
        )
        assertEquals(
            "先看语气。\n\n判断线索：关系压力来自省略。\n\n说明：没有标题的正文也要保留。",
            result.text,
        )
    }

    @Test
    fun aiExplainResultFallsBackToDefaultsForEmptyResponse() {
        val result = parseAiExplainResult(JSONObject())

        assertEquals("智能精讲", result.title)
        assertEquals("智能讲解已返回，但没有结构化摘要。", result.summary)
        assertEquals("智能讲解已返回，但没有结构化摘要。", result.text)
        assertTrue(result.sections.isEmpty())
    }

    // ---------------------------------------------------------------- ai history list

    @Test
    fun aiHistoryMergesAiCorrectionAndProfileGroupsWithFieldFallbacks() {
        val snapshot = parseAiHistoryJson(
            """
            {
              "generatedAt": "2026-07-20T10:00:00Z",
              "ai": [
                {
                  "id": "ai-1",
                  "kind": "linguistic",
                  "model": "gemini-3.1-flash-lite",
                  "workSlug": "k-on",
                  "episode": 1,
                  "sourceId": "line-12",
                  "title": "AI 精讲 · そろそろ起きないと。",
                  "summary": "先看语气。",
                  "updatedAt": "2026-07-20T09:00:00Z"
                }
              ],
              "corrections": [
                {
                  "id": "correction-1",
                  "targetType": "sentence",
                  "targetId": "sentence-7",
                  "model": "gemini-3.1-flash-lite",
                  "workSlug": "k-on",
                  "episode": 2,
                  "promptText": "请帮我批改这句话。",
                  "title": "",
                  "summary": "",
                  "createdAt": "2026-07-19T08:00:00Z"
                }
              ],
              "profiles": [
                {
                  "id": "profile-1",
                  "workSlug": "re-zero",
                  "characterKey": "emilia",
                  "model": "gemini-3.1-pro",
                  "title": "",
                  "summary": "角色语气总结。",
                  "updatedAt": "2026-07-18T07:00:00Z"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("2026-07-20T10:00:00Z", snapshot.generatedAt)
        assertEquals(3, snapshot.entries.size)
        assertEquals(
            listOf(AiHistoryGroup.Ai, AiHistoryGroup.Correction, AiHistoryGroup.Profile),
            snapshot.entries.map { it.group },
        )

        val ai = snapshot.entries[0]
        assertEquals("ai-1", ai.id)
        assertEquals("linguistic", ai.kind)
        assertEquals("gemini-3.1-flash-lite", ai.model)
        assertEquals("k-on", ai.workSlug)
        assertEquals(1, ai.episode)
        assertEquals("line-12", ai.sourceId)
        assertEquals("AI 精讲 · そろそろ起きないと。", ai.title)
        assertEquals("先看语气。", ai.summary)
        assertEquals("2026-07-20T09:00:00Z", ai.timestamp)

        // Corrections carry targetType/targetId instead of kind/sourceId, and fall back to the
        // group label plus the raw prompt when the row has no title/summary.
        val correction = snapshot.entries[1]
        assertEquals("correction-1", correction.id)
        assertEquals("sentence", correction.kind)
        assertEquals("sentence-7", correction.sourceId)
        assertEquals(AiHistoryGroup.Correction.label, correction.title)
        assertEquals("请帮我批改这句话。", correction.summary)
        assertEquals("2026-07-19T08:00:00Z", correction.timestamp)

        val profile = snapshot.entries[2]
        assertEquals("profile-1", profile.id)
        assertEquals("", profile.kind)
        assertEquals("emilia", profile.sourceId)
        assertEquals(AiHistoryGroup.Profile.label, profile.title)
        assertEquals("角色语气总结。", profile.summary)
        assertEquals("2026-07-18T07:00:00Z", profile.timestamp)
    }

    @Test
    fun aiHistoryDropsRowsWithoutIdAndDegradesForMalformedJson() {
        val snapshot = parseAiHistoryJson(
            """
            {
              "generatedAt": "",
              "ai": [
                {"id": "", "title": "空 id 行"},
                {"title": "缺少 id 的行"},
                "not-an-object",
                {"id": "ai-2", "title": "保留行", "createdAt": "2026-07-01T00:00:00Z"}
              ],
              "corrections": [],
              "profiles": []
            }
            """.trimIndent(),
        )

        assertEquals(listOf("ai-2"), snapshot.entries.map { it.id })
        // `ai` rows prefer updatedAt, then fall back to createdAt.
        assertEquals("2026-07-01T00:00:00Z", snapshot.entries.single().timestamp)
        assertEquals("", snapshot.generatedAt)

        assertEquals(AiHistorySnapshot(), parseAiHistoryJson("not json"))
        assertEquals(AiHistorySnapshot(), parseAiHistoryJson(""))
        assertEquals(AiHistorySnapshot(), parseAiHistoryJson("[]"))
        assertTrue(parseAiHistoryJson("{}").entries.isEmpty())
    }

    // ---------------------------------------------------------------- ai history detail

    @Test
    fun aiHistoryDetailParsesCachedResultAndSnakeCaseFields() {
        val detail = parseAiHistoryDetailJson(
            """
            {
              "id": "ai-1",
              "title": "AI 精讲 · そろそろ起きないと。",
              "summary": "先看语气。",
              "model": "gemini-3.1-flash-lite",
              "cache_kind": "linguistic",
              "work_slug": "k-on",
              "episode": 1,
              "source_id": "line-12",
              "prompt_text": "为什么我会选错？",
              "updatedAt": "2026-07-20T09:00:00Z",
              "result": {
                "title": "AI 精讲",
                "summary": "先看语气。",
                "text": "完整讲解正文。",
                "sections": [{"title": "判断线索", "body": "关系压力来自省略。"}]
              }
            }
            """.trimIndent(),
        )

        assertNotNull(detail)
        requireNotNull(detail)
        assertEquals("ai-1", detail.id)
        assertEquals("AI 精讲 · そろそろ起きないと。", detail.title)
        assertEquals("先看语气。", detail.summary)
        assertEquals("gemini-3.1-flash-lite", detail.model)
        assertEquals("linguistic", detail.cacheKind)
        assertEquals("k-on", detail.workSlug)
        assertEquals(1, detail.episode)
        assertEquals("line-12", detail.sourceId)
        assertEquals("为什么我会选错？", detail.promptText)
        // createdAt is absent, so updatedAt is used as the record timestamp.
        assertEquals("2026-07-20T09:00:00Z", detail.createdAt)

        val result = requireNotNull(detail.result)
        assertEquals("AI 精讲", result.title)
        assertEquals("完整讲解正文。", result.text)
        assertEquals(listOf(AiExplainSection("判断线索", "关系压力来自省略。")), result.sections)
    }

    @Test
    fun aiHistoryDetailKeepsNullResultWhenPayloadHasNoCachedResult() {
        val detail = parseAiHistoryDetailJson(
            """{"id":"ai-2","summary":"仅列表摘要","promptText":"提问原文","createdAt":"2026-07-02T00:00:00Z"}""",
        )

        assertNotNull(detail)
        requireNotNull(detail)
        assertEquals("ai-2", detail.id)
        assertEquals("AI 记录", detail.title)
        assertEquals("仅列表摘要", detail.summary)
        assertEquals("提问原文", detail.promptText)
        assertEquals("2026-07-02T00:00:00Z", detail.createdAt)
        assertEquals(0, detail.episode)
        assertNull(detail.result)
    }

    @Test
    fun aiHistoryDetailReturnsNullForNullBlankErrorAndMalformedPayloads() {
        assertNull(parseAiHistoryDetailJson("null"))
        assertNull(parseAiHistoryDetailJson("  null  "))
        assertNull(parseAiHistoryDetailJson(""))
        assertNull(parseAiHistoryDetailJson("   "))
        assertNull(parseAiHistoryDetailJson("""{"error":"not_found"}"""))
        assertNull(parseAiHistoryDetailJson("""{"id":"ai-3","error":"expired"}"""))
        assertNull(parseAiHistoryDetailJson("{broken"))
        assertNull(parseAiHistoryDetailJson("[]"))
    }
}
