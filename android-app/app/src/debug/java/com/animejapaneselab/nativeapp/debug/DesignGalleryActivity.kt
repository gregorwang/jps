package com.animejapaneselab.nativeapp.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.animejapaneselab.nativeapp.ui.design.AttendanceCard
import com.animejapaneselab.nativeapp.ui.design.AttendanceCell
import com.animejapaneselab.nativeapp.ui.design.AttendanceState
import com.animejapaneselab.nativeapp.ui.design.Avatar
import com.animejapaneselab.nativeapp.ui.design.BottomTabBar
import com.animejapaneselab.nativeapp.ui.design.BroadcastLine
import com.animejapaneselab.nativeapp.ui.design.CharacterLine
import com.animejapaneselab.nativeapp.ui.design.DialogueBox
import com.animejapaneselab.nativeapp.ui.design.EmphasisText
import com.animejapaneselab.nativeapp.ui.design.Eyebrow
import com.animejapaneselab.nativeapp.ui.design.FeedbackSheet
import com.animejapaneselab.nativeapp.ui.design.FilterPill
import com.animejapaneselab.nativeapp.ui.design.IconButton44
import com.animejapaneselab.nativeapp.ui.design.InkButton
import com.animejapaneselab.nativeapp.ui.design.InkSwitch
import com.animejapaneselab.nativeapp.ui.design.LineRow
import com.animejapaneselab.nativeapp.ui.design.LoadingDots
import com.animejapaneselab.nativeapp.ui.design.MangaPanel
import com.animejapaneselab.nativeapp.ui.design.OptionRow
import com.animejapaneselab.nativeapp.ui.design.OptionState
import com.animejapaneselab.nativeapp.ui.design.OutlineButton
import com.animejapaneselab.nativeapp.ui.design.ProgressLine
import com.animejapaneselab.nativeapp.ui.design.QuietButton
import com.animejapaneselab.nativeapp.ui.design.Screentone
import com.animejapaneselab.nativeapp.ui.design.Seal
import com.animejapaneselab.nativeapp.ui.design.SectionHeading
import com.animejapaneselab.nativeapp.ui.design.SlotState
import com.animejapaneselab.nativeapp.ui.design.SpeechBubble
import com.animejapaneselab.nativeapp.ui.design.StampMark
import com.animejapaneselab.nativeapp.ui.design.StickyTab
import com.animejapaneselab.nativeapp.ui.design.StudentCard
import com.animejapaneselab.nativeapp.ui.design.TabItem
import com.animejapaneselab.nativeapp.ui.design.TextTabs
import com.animejapaneselab.nativeapp.ui.design.TextbookCover
import com.animejapaneselab.nativeapp.ui.design.TimetableRow
import com.animejapaneselab.nativeapp.ui.design.TopBar
import com.animejapaneselab.nativeapp.ui.design.TopBarNav
import com.animejapaneselab.nativeapp.ui.design.UnderlineTextField
import com.animejapaneselab.nativeapp.ui.design.VerticalText
import com.animejapaneselab.nativeapp.ui.design.VolumeSwitch
import com.animejapaneselab.nativeapp.ui.design.WordTile
import com.animejapaneselab.nativeapp.ui.design.WorkIdentity
import com.animejapaneselab.nativeapp.ui.design.emphasisRanges
import com.animejapaneselab.nativeapp.ui.theme.AjlTheme
import com.animejapaneselab.nativeapp.ui.theme.AnimeJapaneseLabTheme
import com.animejapaneselab.nativeapp.ui.theme.ProvideWorkTheme

/**
 * Debug-only v3 component gallery for screenshot comparison with the design canvas.
 *
 * adb shell am start -n com.animejapaneselab.nativeapp/.debug.DesignGalleryActivity \
 *   --es work re-zero --ez dark true
 */
class DesignGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialWork = intent.getStringExtra("work") ?: "k-on"
        val initialDark = intent.getBooleanExtra("dark", false)
        setContent {
            var dark by remember { mutableStateOf(initialDark) }
            var work by remember { mutableStateOf(initialWork) }
            AnimeJapaneseLabTheme(darkTheme = dark) {
                ProvideWorkTheme(work) {
                    Gallery(
                        dark = dark,
                        work = work,
                        onToggleDark = { dark = !dark },
                        onCycleWork = { work = when (work) { "k-on" -> "re-zero"; "re-zero" -> ""; else -> "k-on" } },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Gallery(dark: Boolean, work: String, onToggleDark: () -> Unit, onCycleWork: () -> Unit) {
    val colors = AjlTheme.colors
    var tab by remember { mutableIntStateOf(0) }
    var textTab by remember { mutableIntStateOf(0) }
    var volume by remember { mutableIntStateOf(0) }
    var toggle by remember { mutableStateOf(true) }
    var option by remember { mutableStateOf(OptionState.Default) }
    var stampKey by remember { mutableIntStateOf(0) }
    var used by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
        ) {
            BroadcastLine()
            TopBar(
                title = "組件 · ${AjlTheme.work.name}",
                nav = TopBarNav.None,
                actions = {
                    QuietButton(if (dark) "浅色" else "深色", onToggleDark)
                    QuietButton("换作品", onCycleWork)
                },
            )
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Eyebrow("01 · TYPE")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                    VerticalText("第三話", revealStaggerMillis = 60)
                    VerticalText("お姉ちゃん、\nそろそろ起きないと。", style = AjlTheme.type.jpDisplay.copy(fontSize = AjlTheme.type.jpTitle.fontSize * 1.4f))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("アニメの文法", style = AjlTheme.type.jpDisplay, color = colors.ink)
                        Text("本日の時間割", style = AjlTheme.type.jpTitle, color = colors.ink)
                        Text("听一遍，替憂把台词说完", style = AjlTheme.type.title, color = colors.ink)
                        Text("姐姐，差不多该起床了。", style = AjlTheme.type.body, color = colors.ink2)
                        Eyebrow("場面 03 · 平沢家の朝 · 12:31")
                    }
                }
                EmphasisText("お姉ちゃん、そろそろ起きないと。", emphasisRanges("お姉ちゃん、そろそろ起きないと。", "そろそろ"))

                Eyebrow("02 · PANELS")
                MangaPanel(Modifier.fillMaxWidth().height(120.dp)) {
                    Screentone(Modifier.align(Alignment.BottomStart).size(220.dp, 90.dp).graphicsLayer { rotationZ = -12f })
                    Seal(WorkIdentity.sealText(work), Modifier.align(Alignment.TopStart).padding(16.dp))
                    Text("今日の一句", style = AjlTheme.type.meta, color = colors.ink3, modifier = Modifier.align(Alignment.TopEnd).padding(14.dp))
                }
                ProgressLine(0.58f)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    LoadingDots(delayMillis = 0)
                    StickyTab()
                    StampMark(animateIn = stampKey > 0, rotation = -10f)
                    QuietButton("盖章", { stampKey++ })
                }

                Eyebrow("03 · BUTTONS")
                InkButton("继续 · 替憂把台词说完", {}, caption = "听音拼句 · 7/12 · 约 4 分钟", trailingArrow = true, progress = 0.58f)
                InkButton("登录", {}, jpText = "登校する", trailingArrow = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlineButton("换一部番", {}, compact = true)
                    OutlineButton("再听一遍", {})
                    QuietButton("跳过", {})
                    IconButton44(Icons.Rounded.Search, "搜索", {})
                    InkSwitch(toggle, { toggle = it })
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WordTile("起きないと", { used = !used }, used = used)
                    WordTile("もう", {})
                    WordTile("寝ないで", {})
                    FilterPill("N3", true, {})
                    FilterPill("词汇", false, {})
                }

                Eyebrow("04 · NAV")
                TextTabs(listOf("課程", "言語学"), textTab, { textTab = it })
                VolumeSwitch(listOf("第一巻 アニメの台詞", "第二巻 基礎"), volume, { volume = it })
                SectionHeading("本日の時間割", meta = "还剩 3 节")
                TimetableRow("一限", "词汇 · 第三話", "済", SlotState.Done, {})
                TimetableRow("二限", "复习 · 快忘的卡片", "18 枚", SlotState.Current, {})
                TimetableRow("三限", "读空气 · 澪没说出口的话", "3 问", SlotState.Upcoming, {})
                LineRow(trailing = { Icon44Placeholder() }) { Text("题目自动读音", style = AjlTheme.type.body, color = colors.ink) }
                UnderlineTextField(email, { email = it }, label = "学籍", gloss = "邮箱", placeholder = "you@example.com")

                Eyebrow("05 · GAKUEN")
                StudentCard(listOf("氏名" to "Gregor", "所属" to "けいおん！ 第三話", "入学" to "2026.07.02", "出席" to "74 日"))
                AttendanceCard(
                    cells = (1..13).map { n ->
                        AttendanceCell(n, when { n <= 2 -> AttendanceState.Done; n == 3 -> AttendanceState.Now; else -> AttendanceState.Todo })
                    },
                    meta = "けいおん！ · 2 済 · 1 いま",
                    stampingEpisode = if (stampKey > 0) 2 else null,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextbookCover("VOL.1", "空気を読む", "今日はちょっと……", "读空气 · 32 问", 0.4f, true, {}, Modifier.weight(1f))
                    TextbookCover("VOL.2", "終助詞", "かわいいよね", "句末语气 · 24 问", 0.25f, false, {}, Modifier.weight(1f))
                }

                Eyebrow("06 · ANIME")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("唯", "憂", "澪", "エミリア", "ベアトリス", "スバル", "相手").forEach { Avatar(it) }
                }
                DialogueBox(speaker = "憂", text = "お姉ちゃん、そろそろ起きないと。", onAdvance = {})
                CharacterLine(WorkIdentity.character("澪"), "今日はちょっと……", gloss = "今天有点……", unfinished = true)
                SpeechBubble("正解！お姉ちゃんより早いね。", gloss = "答对了！比姐姐起得还快呢。")
                OptionRow("差不多该……了（轻轻提醒）", option, { option = when (option) { OptionState.Default -> OptionState.Selected; OptionState.Selected -> OptionState.Correct; OptionState.Correct -> OptionState.Wrong; else -> OptionState.Default } })
                OptionRow("马上、立刻（催促）", OptionState.Dimmed, {})
                FeedbackSheet(
                    correct = true,
                    onContinue = {},
                    character = WorkIdentity.character("憂"),
                    line = "正解！お姉ちゃんより早いね。",
                    lineGloss = "答对了！比姐姐起得还快呢。",
                    explanation = "「そろそろ」表示时机快到了，和「～ないと」连用是提醒，不是命令。",
                )
                Spacer(Modifier.height(24.dp))
            }
        }
        BottomTabBar(
            items = listOf(TabItem("今日", "今日"), TabItem("学ぶ", "学习"), TabItem("辞書", "资料"), TabItem("復習", "复盘")),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun Icon44Placeholder() {
    IconButton44(Icons.Rounded.Tune, "设置", {})
}
