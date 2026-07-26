package com.animejapaneselab.nativeapp.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.CharacterProfile
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.LocalLabStore
import com.animejapaneselab.nativeapp.data.RemoteLabClient
import com.animejapaneselab.nativeapp.ui.components.SecondaryButton
import com.animejapaneselab.nativeapp.ui.components.StructuredAiResultCard
import com.animejapaneselab.nativeapp.ui.theme.LabTheme
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

/** Shared character-profile bottom sheet; render once per screen. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CharacterProfileSheet(controller: CharacterProfileController) {
    val state = controller.state
    if (state is CharacterProfileState.Hidden) return
    ModalBottomSheet(
        onDismissRequest = controller::dismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "角色语言画像",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            when (state) {
                is CharacterProfileState.Picking -> {
                    Text(
                        text = "选一位角色，看 TA 的口癖、句末倾向、礼貌度和情绪表达是怎么构成的。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val characters = CharacterCatalog.charactersFor(controller.workSlug)
                    if (characters.isEmpty()) {
                        Text(
                            text = "当前作品还没有预置角色。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            characters.forEach { option ->
                                Surface(
                                    onClick = { controller.select(option) },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.heightIn(min = 44.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = option.nameJa,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (option.nameZh.isNotBlank() && option.nameZh != option.nameJa) {
                                            Text(
                                                text = option.nameZh,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is CharacterProfileState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "正在分析「${state.option.nameJa}」的说话方式…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is CharacterProfileState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = "画像获取失败：${state.message}",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(
                            text = "重试",
                            onClick = { controller.select(state.option) },
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            text = "换个角色",
                            onClick = controller::backToPicker,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                is CharacterProfileState.Ready -> {
                    val profile = state.profile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "「${state.option.nameJa}」· 基于 ${profile.sourceCount} 段字幕检索",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = controller::backToPicker) {
                            Text(text = "换角色", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (profile.cacheWarning.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = LabTheme.colors.warningContainer,
                            contentColor = LabTheme.colors.onWarningContainer,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(
                                text = profile.cacheWarning,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    StructuredAiResultCard(
                        result = profile.result,
                        fallbackText = profile.result.text,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryButton(
                        text = "重新生成画像",
                        onClick = { controller.select(state.option, regenerate = true) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                CharacterProfileState.Hidden -> Unit
            }
        }
    }
}
