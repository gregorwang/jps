package com.animejapaneselab.nativeapp.ui.design

import androidx.annotation.DrawableRes
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.ui.theme.WorkHue
import com.animejapaneselab.nativeapp.ui.theme.WorkThemes
import com.animejapaneselab.nativeapp.ui.theme.normalizeWorkSlug

/**
 * Where the face sits on a character sheet, as fractions of the image: centre ([cx], [cy]) and
 * square side ([size], fraction of the image width). Sheets differ (K-ON! close-ups on the left,
 * Re:ゼロ on the right), so avatars crop per drawable.
 */
data class FaceCrop(val cx: Float, val cy: Float, val size: Float)

/** A character as the UI needs it: display name, kanji-circle mark and (optional) portrait. */
data class CharacterRef(
    /** Japanese display name, e.g. 唯 / エミリア. */
    val name: String,
    /** One glyph for the kanji-circle fallback avatar. */
    val mark: String,
    @DrawableRes val drawable: Int?,
    val face: FaceCrop = FaceCrop(0.5f, 0.3f, 0.5f),
)

/** A season (課程) of a work as shown in the course switcher. */
data class WorkSeason(
    val title: String,
    val firstEpisode: Int,
    val lastEpisode: Int,
    val character: CharacterRef,
) {
    val episodeCount: Int get() = lastEpisode - firstEpisode + 1
    operator fun contains(episode: Int): Boolean = episode in firstEpisode..lastEpisode
}

/**
 * Work identity: workSlug → colour family, display name, representative characters and
 * character-name → portrait. Portraits are the existing low-res sheets in drawable-nodpi; draw
 * them through [Avatar] (which applies [mangaImageFilter]).
 */
object WorkIdentity {
    private val Yui = CharacterRef("唯", "唯", R.drawable.k_on_yui_character, FaceCrop(0.39f, 0.27f, 0.45f))
    private val Mio = CharacterRef("澪", "澪", R.drawable.k_on_mio_character, FaceCrop(0.65f, 0.49f, 0.40f))
    private val Ritsu = CharacterRef("律", "律", R.drawable.k_on_ritsu_character, FaceCrop(0.67f, 0.78f, 0.40f))
    private val Mugi = CharacterRef("紬", "紬", R.drawable.k_on_mugi_character, FaceCrop(0.30f, 0.22f, 0.42f))
    private val Azusa = CharacterRef("梓", "梓", R.drawable.k_on_azusa_character_v2, FaceCrop(0.28f, 0.55f, 0.48f))
    private val Ui = CharacterRef("憂", "憂", R.drawable.k_on_ui_character, FaceCrop(0.30f, 0.30f, 0.50f))
    private val Sawako = CharacterRef("さわ子", "さ", R.drawable.k_on_sawako_character, FaceCrop(0.27f, 0.36f, 0.45f))
    private val Jun = CharacterRef("純", "純", R.drawable.k_on_jun_character, FaceCrop(0.49f, 0.15f, 0.26f))

    private val Emilia = CharacterRef("エミリア", "エ", R.drawable.rezero_emilia_character, FaceCrop(0.70f, 0.40f, 0.35f))
    private val Beatrice = CharacterRef("ベアトリス", "ベ", R.drawable.rezero_beatrice_character, FaceCrop(0.69f, 0.37f, 0.33f))
    private val Subaru = CharacterRef("スバル", "ス", R.drawable.rezero_subaru_character, FaceCrop(0.65f, 0.40f, 0.38f))
    private val Rem = CharacterRef("レム", "レ", R.drawable.rezero_rem_character, FaceCrop(0.61f, 0.30f, 0.35f))
    private val Ram = CharacterRef("ラム", "ラ", R.drawable.rezero_ram_character, FaceCrop(0.61f, 0.30f, 0.35f))
    private val Puck = CharacterRef("パック", "パ", R.drawable.rezero_puck_character, FaceCrop(0.46f, 0.35f, 0.45f))
    private val Echidna = CharacterRef("エキドナ", "エ", R.drawable.rezero_echidna_character, FaceCrop(0.72f, 0.38f, 0.30f))
    private val Otto = CharacterRef("オットー", "オ", R.drawable.rezero_otto_character, FaceCrop(0.70f, 0.47f, 0.30f))
    private val Frederica = CharacterRef("フレデリカ", "フ", R.drawable.rezero_frederica_character, FaceCrop(0.62f, 0.33f, 0.35f))
    private val Petelgeuse = CharacterRef("ペテルギウス", "ペ", R.drawable.rezero_petelgeuse_character, FaceCrop(0.70f, 0.33f, 0.35f))

    /** Every alias we have seen in content (kanji, full names, kana, romaji) → character. */
    private val Aliases: Map<String, CharacterRef> = buildMap {
        fun add(ref: CharacterRef, vararg names: String) = names.forEach { put(it.lowercase(), ref) }
        add(Yui, "唯", "平沢唯", "平沢 唯", "ゆい", "ユイ", "yui")
        add(Mio, "澪", "秋山澪", "秋山 澪", "みお", "ミオ", "mio")
        add(Ritsu, "律", "田井中律", "田井中 律", "りっちゃん", "りつ", "リツ", "ritsu")
        add(Mugi, "紬", "琴吹紬", "琴吹 紬", "ムギ", "むぎ", "ムギちゃん", "mugi", "tsumugi")
        add(Azusa, "梓", "中野梓", "中野 梓", "あずにゃん", "あずさ", "azusa")
        add(Ui, "憂", "平沢憂", "平沢 憂", "うい", "ui")
        add(Sawako, "さわ子", "山中さわ子", "さわちゃん", "さわ子先生", "sawako")
        add(Jun, "純", "鈴木純", "じゅん", "jun")
        add(Emilia, "エミリア", "エミリアたん", "emilia")
        add(Beatrice, "ベアトリス", "ベア子", "beatrice")
        add(Subaru, "スバル", "菜月昴", "ナツキ・スバル", "昴", "subaru")
        add(Rem, "レム", "rem")
        add(Ram, "ラム", "ram")
        add(Puck, "パック", "puck")
        add(Echidna, "エキドナ", "echidna")
        add(Otto, "オットー", "otto")
        add(Frederica, "フレデリカ", "frederica")
        add(Petelgeuse, "ペテルギウス", "ペテルギウス・ロマネコンティ", "petelgeuse", "betelgeuse")
    }

    fun hue(workSlug: String?): WorkHue = WorkThemes.hueFor(workSlug)

    /** けいおん！ / Re:ゼロ — short display names used in headers and the switcher. */
    fun displayName(workSlug: String?, fallback: String = ""): String = when (normalizeWorkSlug(workSlug)) {
        "k-on" -> "けいおん！"
        "re-zero" -> "Re:ゼロ"
        else -> fallback
    }

    /** Two stacked glyphs for the work seal (軽/音, 異/世). */
    fun sealText(workSlug: String?): String = when (normalizeWorkSlug(workSlug)) {
        "k-on" -> "軽\n音"
        "re-zero" -> "異\n世"
        else -> "学\n園"
    }

    /** Character by speaker name (any alias). Unknown names get a kanji-circle-only ref. */
    fun character(name: String?): CharacterRef? {
        val key = name?.trim().orEmpty()
        if (key.isEmpty()) return null
        return Aliases[key.lowercase()] ?: CharacterRef(key, key.take(1), null)
    }

    @DrawableRes
    fun drawableFor(name: String?): Int? = character(name)?.drawable

    /**
     * Seasons shown in the course switcher. K-ON! is one season; Re:ゼロ's single catalogue work
     * (66 episodes) splits into 一期 1–25 / 二期 26–50 / 三期 51+ led by エミリア / ベアトリス / スバル.
     */
    fun seasons(workSlug: String?, episodeCount: Int): List<WorkSeason> = when (normalizeWorkSlug(workSlug)) {
        "k-on" -> listOf(WorkSeason("けいおん！", 1, episodeCount.coerceAtLeast(1), Yui))
        "re-zero" -> buildList {
            val total = episodeCount.coerceAtLeast(1)
            add(WorkSeason("Re:ゼロ 一期", 1, minOf(25, total), Emilia))
            if (total > 25) add(WorkSeason("Re:ゼロ 二期", 26, minOf(50, total), Beatrice))
            if (total > 50) add(WorkSeason("Re:ゼロ 三期", 51, total, Subaru))
        }
        else -> listOf(WorkSeason(displayName(workSlug, workSlug.orEmpty()), 1, episodeCount.coerceAtLeast(1), CharacterRef("学", "学", null)))
    }

    fun seasonFor(workSlug: String?, episodeCount: Int, episode: Int): WorkSeason? =
        seasons(workSlug, episodeCount).firstOrNull { episode in it }

    /** The face of a work/episode (switcher avatar, Today seal fallback). */
    fun representative(workSlug: String?, episode: Int = 1, episodeCount: Int = 66): CharacterRef? =
        seasonFor(workSlug, episodeCount, episode)?.character
            ?: seasons(workSlug, episodeCount).firstOrNull()?.character
}
