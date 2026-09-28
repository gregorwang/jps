# 罗马音修正 · 交接文档

用户会一张一张发手机截图（红字标出问题），逐屏指导「罗马音该怎么放」。这份文档是为那一轮准备的：规矩、流程、工具、每个显示读音的地方在哪个文件。**接手时先读这份，不用再全仓 grep。**

状态截至 0.15.0（2026-09-28）。

---

## 1. 已定的规矩

- **罗马音在最上面，假名在它下面，然后才是字**（用户原话：「像多邻国一样，罗马音在字的上面、在假名的上面」）。
- **逐词对齐**：每个词的罗马音压在这个词正上方，换行时三层一起走。**不要再出现「整句一条罗马音」放在句子上面或下面。**
- 罗马音、假名注音分别受设置里「罗马音」「假名注音」两个开关控制（`LabSettings.showRomaji` / `showFurigana`）。
  **`showRomaji` 默认是关的**（`data/LocalLabStore.kt` 第 33 行）。用户截图里「没有罗马音」时，先确认是开关没开还是代码没接。

## 2. 还没定、等用户在截图里拍板的

1. **单词读音并排的写法**要不要也改成上下叠：现在是「いどむ idomu」横着排（见第 5 节 ⚠ 那一类）。
2. **纯假名的句子**要不要也加罗马音（现在逐词组件对纯假名也会加，但有些地方根本没接组件）。
3. **选项、字块、配对卡片**这类答题元素上要不要罗马音（现在只有单词练习的拼句字块有）。
4. `showRomaji` 默认值要不要改成开。

用户定了哪条，就把它补进第 1 节，并在 `CLAUDE.md` 第 8 节记一句。

## 3. 流程（照 0.5.1 那次的做法）

1. 用户把截图放进 `android-app/photo/`（不提交），图上用红字标问题。
2. **先读完全部截图**，每张图在第 5 节的表里找到对应位置（找不到就按截图上的文字 grep）。
3. 一张图对应一处改动。改法优先用第 4 节的现成组件，不要每屏各写一套。
4. 每改完一批：`design/v3build.ps1 -Mode compile` → commit（直接 main）。
5. 全部改完：bump 版本 + `CHANGELOG.md` 加一节 → `-Mode full` → 发布脚本（`CLAUDE.md` 第 6 节）。
6. 汇报时逐张图对上「这张图 → 改了哪里」。

## 4. 工具箱

| 要做什么 | 用什么 | 在哪 |
|---|---|---|
| 一句日语，逐词叠罗马音 / 假名 | `ReadingLineText(reading, mark, showRuby, showRomaji, style)` | `ui/reading/ReadingLineText.kt` |
| 拿到 `reading` | `val furigana = rememberFuriganaAnnotator(settings)`；`LaunchedEffect(text) { furigana.request("sentence", listOf(text)) }`；`LineReading.build(text, furigana.resultFor(text))` | `ui/reading/FuriganaController.kt`、`Kana.kt` |
| 句子里要标出的目标词 | `ReadingLineText` 的 `mark: IntRange?` | 同上 |
| 句型卡那种逐字浮现 | `ReadingLineText(..., revealed = 进度)`，或直接用 `RevealLine` | `screens/library/DictCards.kt` |
| 单个假名串 → 罗马音 | `Kana.romaji(kana)`（片假名先 `Kana.toHiragana`） | `ui/reading/Kana.kt` |
| 公式词块（活用拆解） | `FormulaRow(..., readings, showRuby, showRomaji)`，罗马音已在最上 | `screens/jishu/Formula.kt` |
| 带罗马音的字块 | `WordTile(text, sub = 罗马音)`，sub 已在字上面 | `ui/design/Buttons.kt` |

**转换要点**：把 `RubyText(...)` + 下面单独一行 `Text(romaji)` 换成一个 `ReadingLineText`。它在罗马音、假名都关着时会退回普通的 `MarkedLine`，所以不用自己判断开关。

**坑**：
- 汉字词的罗马音来自 Worker 的 furigana。没网或还没返回时，汉字单元的罗马音是空的，只有假名部分有。这不是 bug，别在客户端硬猜读音。
- 罗马音比字宽时会把这个词撑宽（`tsuka` 比「捕」宽），行距看起来松。要收紧就调 `ReadingLineText` 里的 `romajiStyle` 字号，所有地方一起变。
- `ReadingLineText` 不会自动换成竖排，也不能用在通知、小组件里（那两处是 RemoteViews / 位图）。

## 5. 每个显示读音的地方

✅ 已经是「罗马音 → 假名 → 字、逐词对齐」　⚠ 单词读音横着并排　❌ 没有罗马音（只有假名注音或什么都没有）

### 自習
| 位置 | 状态 | 代码 |
|---|---|---|
| 板書的原作台词 | ✅ | `screens/jishu/JishuSitting.kt:361` |
| 板書的公式词块 | ✅ | `JishuSitting.kt:389` → `Formula.kt` |
| 场景句卡 | ✅ | `JishuSitting.kt` 同一段（`aids.romaji`） |

### 今日
| 位置 | 状态 | 代码 |
|---|---|---|
| 今日课程卡里的那句台词 | ✅ | `screens/today/TodayScreen.kt:393` |

### 練習 / 学习 session
| 位置 | 状态 | 代码 |
|---|---|---|
| 学习卡 · 台词 / 语法例句 | ✅ | `screens/session/StudyCardQuestion.kt:283` |
| 学习卡 · 单词大字 | ❌ 只有假名注音 + 下面一行「读音 · 罗马音」⚠ | `StudyCardQuestion.kt:183`（`RubyText`）、`:193` |
| 跟读题 | ✅（0.15.0 从整句改的） | `screens/session/ShadowingQuestion.kt:105` |
| 活用练习 · 答完的「拆解」 | ❌ `FormulaRow` 没传 `showRomaji` | `screens/session/ConjugationSession.kt:315` |
| 选择题、完形、拼句、配对、读空气、基础题库、对话框打字机 | ❌ **未逐个核实**，大概率都没有罗马音 | `screens/session/ChoiceQuestion.kt`、`ClozeQuestion.kt`、`TileOrderQuestion.kt`、`PairMatchQuestion.kt`、`ReadAir*.kt`、`Foundation*.kt`、`ui/design/Anime.kt`（`DialogueBox`） |

### 辞書
| 位置 | 状态 | 代码 |
|---|---|---|
| 词汇列表的词头「いどむ idomu」 | ⚠ | `screens/library/LibraryScreen.kt:752` |
| 词卡大字下面的读音 | ⚠ | `screens/library/WordCard.kt:332` |
| 词卡里的例句 | ❌ 只有标色，没有注音 | `WordCard.kt`（`MarkedLine`，例句那段） |
| 语法列表每行的例句 | ❌（单行省略，放注音会很挤，要问用户） | `screens/library/GrammarPage.kt` `GrammarRow` |
| 句型卡「原作里」 | ✅ | `GrammarPage.kt` → `DictCards.kt` `RevealLine` |
| 台词列表 | ❌ 列表里只有字 | `screens/library/LinesPage.kt` `LineRow` |
| 台词卡大字 | ✅ | `LinesPage.kt:381` |
| 台词卡的意群字块、词列表 | ❌ | `LinesPage.kt`（`WordTile` 没传 sub） |
| 字幕浏览 | ❌ `RubyText`，只有假名 | `screens/library/SubtitlesScreen.kt:447` |
| 单词练习 · 读音行 / 答案 | ⚠ | `screens/library/WordStudy.kt:338`、`:355`、`:454`、`:505` |
| 单词练习 · 音拍格、拼句字块 | ✅ | `WordStudy.kt:343`（`MoraCell`）、`:472` |
| 单词练习 · 例句 | ✅ | `WordStudy.kt:314`、`:547` |

### 知識 / 復習流
| 位置 | 状态 | 代码 |
|---|---|---|
| 知识卡例句 | ✅（0.15.0 从整句改的；只开罗马音，不带假名） | `screens/review/KnowledgeCardBody.kt:154` |
| 知识卡里的单词卡 | ⚠ | `KnowledgeCardBody.kt:347` |
| 活用卡（翻开后） | ✅ | `screens/review/ReviewFeedScreen.kt:669`，拆解 `:714` |
| 收藏 / 错题台词卡 | ✅ | `ReviewFeedScreen.kt:819` |
| 收藏单词卡 | ⚠ | `ReviewFeedScreen.kt:766` |

### 系统界面（不能用 Compose 组件）
| 位置 | 状态 | 代码 |
|---|---|---|
| 通知、超级岛、桌面小组件「今日の一句」 | ❌ | `platform/LearningSessionNotifier.kt`、`platform/StudyReminder.kt`、`widget/TodayWidget.kt`（位图，要单独画） |

## 6. 改完后要同步的地方

- `CLAUDE.md` 第 8 节：记下用户在第 2 节拍板的结论。
- 画布「辞書 · 语法 / 台词（预览）」页的台词卡示意还是「假名在上、罗马音在下」，只在用户要看画布时再改。
- 这份文档第 5 节：改完一处把状态改成 ✅，下次接手就不用重查。
