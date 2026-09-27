package com.animejapaneselab.nativeapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 活用道場 板書: the hand-written lesson for one grammar point, bundled as
 * `assets/conjugation_lessons.json` (written offline, validated by the lessons `check.py`).
 * A point without a lesson still teaches from its drill lines' 拆解 (see ConjugationLessonScreen).
 */
data class ConjugationLesson(
    val pointId: String,
    val title: String,
    val group: String,
    val rule: String,
    val formula: String,
    /** 段 the change lands on: a / i / u / e / o, or blank when the point is not a 段 shift. */
    val dan: String,
    /** 辞书形 endings the point touches (う・つ・る for 促音便), highlighted in the 行×段 grid. */
    val endings: List<String>,
    val points: List<String>,
    val derivations: List<LessonDerivation>,
    val pitfall: String,
    val contrast: LessonContrast?,
    val prereq: List<String>,
    val practice: List<LessonPractice>,
)

data class LessonDerivation(val base: String, val steps: List<String>, val zh: String)

data class LessonContrast(val pointId: String, val text: String)

data class LessonPractice(val prompt: String, val answer: String, val distractors: List<String>, val why: String)

private const val LessonsAsset = "conjugation_lessons.json"

/** Lessons in curriculum order; empty when the asset is missing or malformed. */
fun loadConjugationLessons(context: Context): List<ConjugationLesson> = runCatching {
    context.assets.open(LessonsAsset).bufferedReader(Charsets.UTF_8).use { parseConjugationLessons(it.readText()) }
}.getOrElse { emptyList() }

internal fun parseConjugationLessons(body: String): List<ConjugationLesson> {
    val array = JSONObject(body).optJSONArray("lessons") ?: return emptyList()
    return (0 until array.length()).mapNotNull { i ->
        val row = array.optJSONObject(i) ?: return@mapNotNull null
        val pointId = row.optString("point_id").ifBlank { return@mapNotNull null }
        ConjugationLesson(
            pointId = pointId,
            title = row.optString("title"),
            group = row.optString("group"),
            rule = row.optString("rule"),
            formula = row.optString("formula"),
            dan = row.optString("dan"),
            endings = row.optJSONArray("endings").strings(),
            points = row.optJSONArray("points").strings(),
            derivations = row.optJSONArray("derivations").objects().map {
                LessonDerivation(it.optString("base"), it.optJSONArray("steps").strings(), it.optString("zh"))
            }.filter { it.steps.size >= 2 },
            pitfall = row.optString("pitfall"),
            contrast = row.optJSONObject("contrast")?.let { c ->
                LessonContrast(c.optString("point_id"), c.optString("text")).takeIf { it.pointId.isNotBlank() && it.text.isNotBlank() }
            },
            prereq = row.optJSONArray("prereq").strings(),
            practice = row.optJSONArray("practice").objects().map {
                LessonPractice(it.optString("prompt"), it.optString("answer"), it.optJSONArray("distractors").strings(), it.optString("why"))
            }.filter { it.prompt.isNotBlank() && it.answer.isNotBlank() && it.distractors.isNotEmpty() },
        )
    }
}

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
