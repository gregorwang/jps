package com.animejapaneselab.nativeapp.ui.reading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.animejapaneselab.nativeapp.data.CharacterProfile
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CharacterOption(
    val key: String,
    val nameJa: String,
    val nameZh: String,
)

/**
 * Android-owned character catalog per work. The backend accepts any key/name pair
 * and caches by (workSlug, characterKey, model); these curated entries just give
 * the picker sensible defaults.
 */
object CharacterCatalog {
    fun charactersFor(workSlug: String): List<CharacterOption> = when (workSlug) {
        "re-zero", "rezero" -> listOf(
            CharacterOption("subaru", "スバル", "菜月昴"),
            CharacterOption("emilia", "エミリア", "爱蜜莉雅"),
            CharacterOption("rem", "レム", "雷姆"),
            CharacterOption("ram", "ラム", "拉姆"),
            CharacterOption("beatrice", "ベアトリス", "碧翠丝"),
            CharacterOption("puck", "パック", "帕克"),
        )

        "k-on" -> listOf(
            CharacterOption("yui", "平沢唯", "平泽唯"),
            CharacterOption("mio", "秋山澪", "秋山澪"),
            CharacterOption("ritsu", "田井中律", "田井中律"),
            CharacterOption("tsumugi", "琴吹紬", "琴吹䌷"),
            CharacterOption("azusa", "中野梓", "中野梓"),
        )

        else -> emptyList()
    }
}

sealed interface CharacterProfileState {
    data object Hidden : CharacterProfileState
    data object Picking : CharacterProfileState
    data class Loading(val option: CharacterOption) : CharacterProfileState
    data class Ready(val option: CharacterOption, val profile: CharacterProfile) : CharacterProfileState
    data class Error(val option: CharacterOption, val message: String) : CharacterProfileState
}

/** Feature-owned state holder for the character-language-profile flow. */
@Stable
class CharacterProfileController internal constructor(
    private val scope: CoroutineScope,
    private val store: LocalLabStore,
    private val settingsProvider: () -> LabSettings,
    private val workSlugProvider: () -> String,
) {
    var state by mutableStateOf<CharacterProfileState>(CharacterProfileState.Hidden)
        private set

    val workSlug: String get() = workSlugProvider()

    fun open() {
        if (state is CharacterProfileState.Hidden) state = CharacterProfileState.Picking
    }

    fun backToPicker() {
        state = CharacterProfileState.Picking
    }

    fun dismiss() {
        state = CharacterProfileState.Hidden
    }

    fun select(option: CharacterOption, regenerate: Boolean = false) {
        val current = state
        if (current is CharacterProfileState.Loading && current.option == option) return
        state = CharacterProfileState.Loading(option)
        val requestWorkSlug = workSlug
        scope.launch {
            val settings = settingsProvider()
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    RemoteLabClient(settings.apiBaseUrl, store.readSessionCookie()).fetchCharacterProfile(
                        workSlug = requestWorkSlug,
                        characterKey = option.key,
                        characterName = option.nameJa,
                        model = settings.aiModel,
                        reasoningEffort = settings.reasoningEffort,
                        regenerate = regenerate,
                    )
                }
            }
            val latest = state
            if (latest !is CharacterProfileState.Loading || latest.option != option) return@launch
            state = outcome.fold(
                onSuccess = { CharacterProfileState.Ready(option, it) },
                onFailure = { failure ->
                    if (failure is CancellationException) throw failure
                    CharacterProfileState.Error(option, failure.message ?: "画像请求失败")
                },
            )
        }
    }
}

@Composable
fun rememberCharacterProfile(settings: LabSettings, workSlug: String): CharacterProfileController {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val store = remember(context) { LocalLabStore(context) }
    val latestSettings = rememberUpdatedState(settings)
    val latestWorkSlug = rememberUpdatedState(workSlug)
    return remember(scope, store) {
        CharacterProfileController(
            scope = scope,
            store = store,
            settingsProvider = { latestSettings.value },
            workSlugProvider = { latestWorkSlug.value },
        )
    }
}
