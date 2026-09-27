package com.animejapaneselab.nativeapp.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.AjlBottomSheet
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.StudentCard
import com.animejapaneselab.nativeapp.ui.design.UnderlineTextField
import com.animejapaneselab.nativeapp.ui.design.clickableNoRipple
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.profile.StudentProfile
import com.animejapaneselab.nativeapp.ui.theme.AjlStroke
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 学生証 with the user's own 氏名 / 所属 / photo laid over the account defaults. Tapping it opens
 * the editor; 入学 and 出席 come from the study record and stay as they are.
 */
@Composable
internal fun EditableStudentCard(uiState: LabUiState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    remember { StudentProfile.init(context) }
    val profile by StudentProfile.state.collectAsState()
    val base = remember(uiState.auth.user, uiState.selection, uiState.works, uiState.progressItems) { studentCardInfo(uiState) }
    val defaults = base.rows.toMap()
    val rows = base.rows.map { (label, value) ->
        when (label) {
            "氏名" -> label to profile.name.ifBlank { value }
            "所属" -> label to profile.affiliation.ifBlank { value }
            else -> label to value
        }
    }.let { list -> if (list.none { it.first == "所属" } && profile.affiliation.isNotBlank()) listOf(list.first(), "所属" to profile.affiliation) + list.drop(1) else list }
    val photo = rememberStudentPhoto(profile.photoVersion)
    var editing by rememberSaveable { mutableStateOf(false) }

    StudentCard(
        rows = rows,
        number = base.number,
        modifier = modifier,
        photo = photo?.let { bitmap -> { Image(bitmap, contentDescription = "学生証照片", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) } },
        onEdit = { editing = true },
    )

    if (editing) {
        StudentCardEditor(
            photo = photo,
            name = profile.name,
            affiliation = profile.affiliation,
            defaultName = defaults["氏名"].orEmpty(),
            defaultAffiliation = defaults["所属"].orEmpty(),
            fixedRows = base.rows.filter { it.first == "入学" || it.first == "出席" },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun rememberStudentPhoto(version: Long): ImageBitmap? {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(null, version) {
        value = if (version == 0L) null else withContext(Dispatchers.IO) { StudentProfile.loadPhoto(context)?.asImageBitmap() }
    }
    return photo
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentCardEditor(
    photo: ImageBitmap?,
    name: String,
    affiliation: String,
    defaultName: String,
    defaultAffiliation: String,
    fixedRows: List<Pair<String, String>>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = AjlTheme.colors
    val scope = rememberCoroutineScope()
    var nameDraft by rememberSaveable { mutableStateOf(name) }
    var affiliationDraft by rememberSaveable { mutableStateOf(affiliation) }
    var loading by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            loading = true
            scope.launch {
                StudentProfile.setPhoto(context, uri)
                loading = false
            }
        }
    }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    AjlBottomSheet(onDismissRequest = onDismiss, title = "学生証", gloss = "编辑") {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(width = 96.dp, height = 118.dp)
                        .background(colors.sunken)
                        .border(AjlStroke.Hair, colors.line2)
                        .clickableNoRipple(::pick),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        loading -> LoadingDots(delayMillis = 0)
                        photo != null -> Image(photo, contentDescription = "学生証照片", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else -> {
                            Box(Modifier.fillMaxSize().screentone(colors.ink.copy(alpha = 0.16f), angleDegrees = 0f, spacing = 6.dp, dotRadius = 1.dp))
                            Text("写真", style = AjlTheme.type.jpLabel.copy(fontSize = 13.sp), color = colors.ink3)
                        }
                    }
                }
                QuietButton(if (photo != null) "换照片" else "选照片", ::pick)
                if (photo != null) QuietButton("移除", { StudentProfile.clearPhoto(context) })
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                UnderlineTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it.take(24) },
                    label = "氏名",
                    placeholder = defaultName,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                UnderlineTextField(
                    value = affiliationDraft,
                    onValueChange = { affiliationDraft = it.take(32) },
                    label = "所属",
                    placeholder = defaultAffiliation.ifBlank { "—" },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                fixedRows.forEach { (label, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = AjlTheme.type.jpLabel.copy(fontSize = 13.sp), color = colors.ink3, modifier = Modifier.width(44.dp))
                        Text(value, style = AjlTheme.type.meta.copy(fontSize = 12.sp), color = colors.ink2)
                    }
                }
            }
        }
        InkButton(
            "保存",
            onClick = {
                StudentProfile.update(context, nameDraft, affiliationDraft)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            height = 48.dp,
        )
    }
}
