package com.animejapaneselab.nativeapp.ui.screens.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.data.LabSettings
import com.animejapaneselab.nativeapp.data.SyncStatus
import com.animejapaneselab.nativeapp.ui.LabUiState
import com.animejapaneselab.nativeapp.ui.design.AjlBottomSheet
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.UnderlineTextField
import com.animejapaneselab.nativeapp.ui.design.VerticalText
import com.animejapaneselab.nativeapp.ui.design.screentone
import com.animejapaneselab.nativeapp.ui.motion.rememberReducedMotion
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/** Illustration viewport, in the canvas (V3Login) coordinate system. */
private const val GateWidth = 390f
private const val GateHeight = 430f

/**
 * 校门 — the v3 login gate (V3Login). Login stays mandatory; there is no anonymous entry.
 *
 * [exitProgress] 0→1 is driven by the shell after a successful login: the gate illustration
 * grows to 1.15× while the whole screen fades (MOTION_SPEC「其他细节」, 400ms).
 */
@Composable
fun LoginScreen(
    uiState: LabUiState,
    onSettingsChange: (LabSettings) -> Unit,
    onLogin: (String, String) -> Unit,
    onRefreshAuth: () -> Unit,
    modifier: Modifier = Modifier,
    exitProgress: () -> Float = { 0f },
) {
    val colors = AjlTheme.colors
    val type = AjlTheme.type
    val focusManager = LocalFocusManager.current
    val passwordFocus = remember { FocusRequester() }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var connectionOpen by rememberSaveable { mutableStateOf(false) }
    val loggingIn = uiState.auth.status == SyncStatus.Loading && uiState.auth.user == null
    val error = uiState.auth.message.takeIf { uiState.auth.status == SyncStatus.Error && it.isNotBlank() }
    val clock = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("a hh:mm", Locale.US)) }
    val submit = {
        focusManager.clearFocus()
        onLogin(email, password)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .graphicsLayer { alpha = 1f - exitProgress().coerceIn(0f, 1f) },
    ) {
        val viewportHeight = maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(min = viewportHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth()) {
                        SchoolGate(
                            modifier = Modifier.graphicsLayer {
                                val s = 1f + 0.15f * exitProgress().coerceIn(0f, 1f)
                                scaleX = s
                                scaleY = s
                            },
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 8.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                clock,
                                style = type.meta,
                                color = colors.ink2,
                                modifier = Modifier
                                    .background(colors.bg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                            IconButton44(
                                icon = Icons.Rounded.Tune,
                                contentDescription = "连接设置",
                                onClick = { connectionOpen = true },
                                tint = colors.ink2,
                                modifier = Modifier.background(colors.bg, androidx.compose.foundation.shape.CircleShape),
                            )
                        }
                    }
                    Text(
                        "ようこそ、学園へ。",
                        style = type.jpDisplay,
                        color = colors.ink,
                        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    UnderlineTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "学籍",
                        gloss = "邮箱",
                        placeholder = "you@example.com",
                        enabled = !loggingIn,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                    )
                    UnderlineTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "合言葉",
                        gloss = "密码",
                        placeholder = "········",
                        enabled = !loggingIn,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (!loggingIn) submit() }),
                        error = error,
                        modifier = Modifier.focusRequester(passwordFocus),
                    )
                    InkButton(
                        text = "登录",
                        jpText = "登校する",
                        trailingArrow = true,
                        loading = loggingIn,
                        onClick = submit,
                        height = 56.dp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }

    if (connectionOpen) {
        ConnectionSheet(
            settings = uiState.settings,
            refreshing = uiState.auth.status == SyncStatus.Loading,
            onSettingsChange = onSettingsChange,
            onRefreshAuth = onRefreshAuth,
            onDismiss = { connectionOpen = false },
        )
    }
}

/** 接続設定: server addresses and a manual auth re-check (everything the v2 gate offered). */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun ConnectionSheet(
    settings: LabSettings,
    refreshing: Boolean,
    onSettingsChange: (LabSettings) -> Unit,
    onRefreshAuth: () -> Unit,
    onDismiss: () -> Unit,
) {
    AjlBottomSheet(onDismissRequest = onDismiss, title = "接続設定", gloss = "连接设置") {
        UnderlineTextField(
            value = settings.apiBaseUrl,
            onValueChange = { onSettingsChange(settings.copy(apiBaseUrl = it)) },
            label = "学習サーバー",
            gloss = "学习服务地址",
            placeholder = "https://…",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        )
        UnderlineTextField(
            value = settings.ttsWorkerUrl,
            onValueChange = { onSettingsChange(settings.copy(ttsWorkerUrl = it)) },
            label = "音声サーバー",
            gloss = "语音服务地址",
            placeholder = "https://…",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        )
        OutlineButton(
            text = if (refreshing) "检查中…" else "刷新登录状态",
            onClick = onRefreshAuth,
            enabled = !refreshing,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------------
// 校門 line art (V3Login SVG, redrawn on Canvas) + falling petals
// ---------------------------------------------------------------------------

private data class Petal(
    val x: Float,
    val startY: Float,
    val periodMs: Long,
    val phase: Float,
    val sway: Float,
    val spin: Float,
    /** Resting pose under reduced motion (canvas positions). */
    val restX: Float,
    val restY: Float,
    val restAngle: Float,
)

private val PetalSpecs = listOf(
    Petal(x = 110f, startY = -20f, periodMs = 9_000, phase = 0.10f, sway = 18f, spin = 160f, restX = 96f, restY = 182f, restAngle = 30f),
    Petal(x = 280f, startY = -30f, periodMs = 11_000, phase = 0.55f, sway = 22f, spin = -140f, restX = 262f, restY = 160f, restAngle = 50f),
    Petal(x = 160f, startY = -10f, periodMs = 12_000, phase = 0.80f, sway = 14f, spin = 120f, restX = 128f, restY = 120f, restAngle = -20f),
    Petal(x = 320f, startY = -40f, periodMs = 8_500, phase = 0.30f, sway = 16f, spin = -180f, restX = 310f, restY = 96f, restAngle = 10f),
    Petal(x = 70f, startY = -25f, periodMs = 10_000, phase = 0.68f, sway = 20f, spin = 150f, restX = 84f, restY = 340f, restAngle = -40f),
)

@Composable
private fun SchoolGate(modifier: Modifier = Modifier) {
    val colors = AjlTheme.colors
    val work = AjlTheme.work
    val reduced = rememberReducedMotion()
    BoxWithConstraints(modifier.fillMaxWidth().clearAndSetSemantics { }) {
        val s = maxWidth.value / GateWidth
        val heightDp = (GateHeight * s).dp
        Box(Modifier.fillMaxWidth().height(heightDp)) {
            // 網点の空 — the only texture, work colour, 12°.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height((250f * s).dp)
                    .screentone(work.tone(0.30f), angleDegrees = 12f, spacing = (7f * s).dp, dotRadius = (1.3f * s).dp),
            )
            val ink = colors.ink
            val paper = colors.surface
            val faintLine = colors.line2
            val windowTone = colors.ink.copy(alpha = 0.18f)
            Canvas(Modifier.fillMaxWidth().height(heightDp)) {
                scale(s, pivot = Offset.Zero) {
                    drawGate(ink = ink, paper = paper, faintLine = faintLine, windowTone = windowTone)
                }
            }
            // 門牌「アニメ日本語学園」 on the right pillar.
            val density = LocalDensity.current
            val plateFont = with(density) { (9f * s).dp.toSp() }
            VerticalText(
                text = "アニメ日本語学園",
                style = AjlTheme.type.jpLabel.copy(fontSize = plateFont, lineHeight = plateFont, fontWeight = FontWeight.Bold),
                color = colors.ink,
                glyphSpacing = 1.08f,
                modifier = Modifier.offset(x = ((352f - 6.5f) * s).dp, y = (310f * s).dp),
            )
            FallingPetals(k = s, color = work.accent.copy(alpha = 0.85f), animate = !reduced, height = heightDp)
        }
    }
}

@Composable
private fun FallingPetals(k: Float, color: Color, animate: Boolean, height: androidx.compose.ui.unit.Dp) {
    val time = remember { mutableLongStateOf(0L) }
    if (animate) {
        LaunchedEffect(Unit) {
            val start = withFrameMillis { it }
            while (true) {
                withFrameMillis { time.longValue = it - start }
            }
        }
    }
    Canvas(Modifier.fillMaxWidth().height(height)) {
        scale(k, pivot = Offset.Zero) {
            PetalSpecs.forEach { p ->
                val (x, y, angle) = if (!animate) {
                    Triple(p.restX, p.restY, p.restAngle)
                } else {
                    val t = ((time.longValue.toFloat() / p.periodMs) + p.phase) % 1f
                    val fallY = p.startY + (GateHeight + 40f - p.startY) * t
                    val swayX = p.x + p.sway * sin(2f * PI.toFloat() * t * 2f)
                    Triple(swayX, fallY, p.restAngle + p.spin * t)
                }
                rotate(angle, pivot = Offset(x, y)) {
                    drawOval(color, topLeft = Offset(x - 4f, y - 2.4f), size = Size(8f, 4.8f))
                }
            }
        }
    }
}

private fun DrawScope.drawGate(ink: Color, paper: Color, faintLine: Color, windowTone: Color) {
    val stroke = Stroke(width = 1.5f, join = StrokeJoin.Round)
    val thin = Stroke(width = 1f)
    fun box(x: Float, y: Float, w: Float, h: Float, style: Stroke = stroke) {
        drawRect(paper, Offset(x, y), Size(w, h))
        drawRect(ink, Offset(x, y), Size(w, h), style = style)
    }
    fun toneBox(x: Float, y: Float, w: Float, h: Float) {
        drawRect(paper, Offset(x, y), Size(w, h))
        var dy = y + 3f
        while (dy < y + h) {
            var dx = x + 3f
            while (dx < x + w) {
                drawCircle(windowTone, 1f, Offset(dx, dy))
                dx += 6f
            }
            dy += 6f
        }
        drawRect(ink, Offset(x, y), Size(w, h), style = thin)
    }

    // 校舎 + 時計塔
    box(58f, 206f, 274f, 150f)
    box(166f, 136f, 58f, 220f)
    val roof = Path().apply {
        moveTo(160f, 136f); lineTo(195f, 108f); lineTo(230f, 136f); close()
    }
    drawPath(roof, paper)
    drawPath(roof, ink, style = stroke)
    // 時計
    drawCircle(paper, 15f, Offset(195f, 168f))
    drawCircle(ink, 15f, Offset(195f, 168f), style = stroke)
    drawLine(ink, Offset(195f, 168f), Offset(195f, 158f), strokeWidth = 1.5f, cap = StrokeCap.Round)
    drawLine(ink, Offset(195f, 168f), Offset(203f, 172f), strokeWidth = 1.5f, cap = StrokeCap.Round)
    // 窓
    listOf(74f, 106f, 138f, 230f, 262f, 294f).forEach { x ->
        toneBox(x, 222f, 22f, 26f)
        toneBox(x, 266f, 22f, 26f)
    }
    toneBox(180f, 200f, 30f, 36f)
    // 玄関
    box(178f, 300f, 34f, 56f)
    // 地面と参道
    drawLine(ink, Offset(0f, 398f), Offset(GateWidth, 398f), strokeWidth = 1.5f)
    drawLine(faintLine, Offset(70f, 398f), Offset(150f, 330f), strokeWidth = 1f)
    drawLine(faintLine, Offset(320f, 398f), Offset(240f, 330f), strokeWidth = 1f)
    // 門柱
    box(18f, 300f, 40f, 98f)
    box(332f, 300f, 40f, 98f)
    box(12f, 292f, 52f, 10f)
    box(326f, 292f, 52f, 10f)
    // 門牌
    box(340f, 306f, 24f, 88f, thin)
    // 桜の木
    val trunk = Path().apply {
        moveTo(30f, 300f)
        cubicTo(10f, 250f, 60f, 210f, 30f, 170f)
    }
    drawPath(trunk, ink, style = stroke)
    listOf(Triple(18f, 170f, 26f), Triple(52f, 150f, 24f), Triple(36f, 200f, 20f)).forEach { (cx, cy, r) ->
        drawCircle(paper, r, Offset(cx, cy))
        drawCircle(ink, r, Offset(cx, cy), style = stroke)
    }
}
