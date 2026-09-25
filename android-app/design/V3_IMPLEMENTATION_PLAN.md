# v3「アニメの文法」UI 重写 · 实施计划（交接文档）

写于 2026-09-25，给下一个会话直接照着执行。设计阶段已经结束，本文只讲**怎么把设计落成代码**。

## 0. 新会话开场（按顺序做）

1. 读这些文件：`ANDROID_ENVIRONMENT.md` → 本文 → `design/MOTION_SPEC.md`。`design/ASSET_REMIX.md` 已暂停，只需要看最上面的兜底方案。
2. 读设计画布：用 Artifact 工具的 `read` 读 https://claude.ai/artifact/9x3RkMeAtAYTN64i8T8HN4 。只看 **v3 页**，文件名以 `V3*`、`X*` 开头；v2 页只是对照，不要照着实现。
   需要看某一屏的细节时，读 `project/<文件名>.dc.html`。
3. Git：分支 `feat/ui-v3` 已经建好，`design/` 也已提交并推送到 GitHub。切到这个分支上干活即可。`android-app/local-anime-assets/` 是用户用 Gemini 做的试验图，放着别动，也不要提交。
4. 跑一次基线（命令见第 7 节），确认改之前测试是绿的，并记下测试数量。
5. 用户偏好：中文回复；大任务用多个子代理并行（fable 模型）；**要大胆，不要在旧结构上换皮**。

## 1. 目标与边界

**要做**
- 全部界面按 v3 重写：主题、组件库、所有页面、动效，以及触觉和音效。
- 把多邻国遗留的东西全部移出 App：DuolingoSans 字体、Rive/Lottie、`.hla` 触觉文件、吉祥物、XP/连胜的界面、路径节点。

**不做**
- 不改数据层和网络协议（`data/`、`domain/`、Worker 接口），也不改后端。
- 不重构 `LabViewModel` 的业务逻辑。页面只能用现有的 state 和回调；确实缺字段时，按第 5 节的规则找主代理加。
- 不做素材改造（已暂停），不做手写功能，登录仍然必须。
- XP 和连胜**只从界面上去掉**；`learningXp` / `learningStreakDays` 的计算和对应测试保留。

## 2. 已经拍板的设计决定

### 2.1 世界观
你是这所学园的学生，课上看的是番。
- **制度用学园物件**：校门登录「登校する」、学生証、時間割、出席カード（「済」章 +「いま」便签）、教科書。
- **内容用番剧语法**：一部番 → 第X話 → 場面 01–06、アイキャッチ、会話窓、つづく、次回予告。

### 2.2 颜色（浅色 / 深色）
| 令牌 | 浅色 | 深色 |
|---|---|---|
| bg | `#F7F7F4` | `#141413` |
| surface | `#FFFFFF` | `#1C1C1A` |
| sunken | `#EFEEE9` | `#232320` |
| line | `#E4E3DD` | `#2E2E2A` |
| line2 | `#D3D2CB` | `#3A3A36` |
| ink（正文、主按钮底） | `#1B1B19` | `#EDEDE8` |
| ink2 | `#55544F` | `#A9A8A1` |
| ink3 | `#6E6D67` | `#86857E` |
| ok / okSoft | `#2C7A55` / `#E6F1EA` | `#6FC79C` / `#1E3328` |
| bad / badSoft | `#B8432F` / `#F8E9E5` | `#F08E7A` / `#3A2420` |
| stamp（朱印） | `#B8432F` | `#F08E7A` |

**作品色**：`WorkTheme` 用 CompositionLocal 实现，按 `workSlug` 切换。

| 作品 | 名称 | 浅色 accent / soft | 深色 accent / soft |
|---|---|---|---|
| k-on | 桜 | `#C4466F` / `#F8E6EC` | `#E58AA7` / `#3A2230` |
| re-zero（全季） | 菫 | `#6A4FC4` / `#EEEAFA` | `#A894F2` / `#2A2446` |
| 默认 / 全局页 | 藍 | `#3A4FCB` / `#ECEEFB` | `#8C9BF2` / `#262B45` |

作品色只用在这些地方：顶部 3px 播出线、进度线、到期数、当前项、印章、网点、名牌。

### 2.3 字体
- 日语原文用 `FontFamily.Serif`：Android 系统的 Noto Serif CJK，不打包。
- 界面文字用系统默认黑体。
- 元数据（集数、计数、时间码）用 IBM Plex Mono：**OFL 授权，打包进 `res/font/`**。需要下载 Regular 和 Medium 两个字重。许可证记录要写进仓库根目录的 `docs/assets-license.md`，这个位置在 android-app 外，先征得用户同意。
- 竖排：Compose 没有竖排文字，自己写 `VerticalText`，一个字一行叠起来。标点要换成竖排专用字形：`、`→`︑`，`。`→`︒`，`ー`→`丨`，`「`→`﹁`，`」`→`﹂`，`…`→`︙`。这部分写成纯函数，并补单测。

### 2.4 形状与层级
- **1.5px 墨线、4px 圆角 = 剧情和内容**（漫画框）；**1px 细线、12px 圆角 = 工具和设置**。不用阴影。唯一的例外是字块和当前教科书上的 2–3px 墨色实投影。
- 墨色主按钮每屏最多一个。
- 进度线是连续的 3px 细线，颜色从作品色浅色插值到深色；对错不影响进度线的颜色。

### 2.5 导航
- 底部 4 个标签，日文字加中文小字：今日（今日）· 学ぶ（学习）· 辞書（资料）· 復習（复盘）。当前标签字重 700，顶部有一条作品色 2px 指示线。
- 「学ぶ」顶部用文字标签切换「課程 / 言語学」。
- 全局搜索是命令面板：浮层 + 遮罩；没有"取消"按钮，点遮罩或用返回手势关闭；**不放**键盘快捷键提示。

## 3. 页面对照表（旧代码 → 新实现 → 画布参考）

| 旧文件 / 入口 | 新文件（建议） | 画布画板 |
|---|---|---|
| `screens/SettingsScreen.kt` 里的 `LoginGateScreen` | `screens/login/LoginScreen.kt` | V3Login（校门线稿、「登校する」） |
| `screens/TodayScreen.kt` | `screens/today/TodayScreen.kt` | V3Today（今日の一句 + 本日の時間割） |
| `screens/LessonScreen.kt` 里的 `LessonHubScreen` 和课程切换部分 | `screens/learn/LearnScreen.kt`、`CourseSwitcherSheet.kt` | V3Learn、V3CourseSwitcher（出席カード + 角色头像） |
| `foundation/LinguisticsTrackScreen.kt`、`screens/ReadAirScreen.kt` 的首页部分 | `screens/learn/LinguisticsScreen.kt` | V3Linguistics（四本教科書 + 第一巻/第二巻） |
| `screens/LessonScreen.kt` 里的 `LessonScreen` 和各题型 | `screens/session/*`（每个题型一个文件） | V3LessonScene（会話窓）、XCorrect、LessonFeedback（v2 可参考结构） |
| `LessonComplete`、`completion/*` | `screens/session/TsuzukuScreen.kt` | XTsuzuku |
| （新增）进场转场 | `screens/session/Eyecatch.kt` | XEyecatch |
| `ReadAirSessionScreen` | `screens/session/ReadAirSession.kt` | XReadAirManga |
| `foundation/FoundationTrainingScreen.kt` 的答题部分 | `screens/session/FoundationSession.kt` | 与 XReadAirManga 同一套语言 |
| `screens/LibraryScreen.kt` | `screens/library/LibraryScreen.kt` | V3Library（辞書词条 + 五十音索引） |
| `screens/SubtitleBrowserScreen.kt` | `screens/library/SubtitlesScreen.kt` | v2 Subtitles 的结构 + v3 语言（说话人头像、场面分组） |
| `screens/ReviewScreen.kt`、`SmartReviewQueueScreen.kt` | `screens/review/*` | V3Review（翻卡堆 + 苦手なところ） |
| `screens/SettingsScreen.kt` 的其余部分 | `screens/settings/SettingsScreen.kt` | V3Settings（学生証） |
| `screens/SearchScreen.kt` | `screens/search/CommandPalette.kt` | Search（v2，已经按 Cursor 风格改过） |
| `screens/AiHistoryScreen.kt` | `screens/settings/AiHistoryScreen.kt` | AiHistory（v2 结构，换成 v3 字体和框线） |

没有画出来的页面（发音评测细节、配对、智能复习队列、基础题库筛选），按第 2 节的规则和最接近的画板自行推导，不要回头参照旧 UI。

## 4. 阶段与任务包

### 阶段 1 · 地基（1 个子代理，串行，其他人等它做完）
在 `ui/design/` 下建组件库，重写 `ui/theme/Theme.kt` 和 `ui/motion/MotionTokens.kt`：
- **主题**：`AjlColors`、`WorkTheme`、`AjlType`（jpDisplay / jpBody / title / body / caption / meta），`AnimeJapaneseLabTheme` 对外签名保持不变。
- **基础**：`Hairline`、`MangaPanel`、`Screentone`（用 Canvas 画点阵，可设角度、颜色、密度）、`ProgressLine`、`LoadingDots`。
- **文字**：`VerticalText`、`EmphasisText`（着重号，根据 `TextLayoutResult` 在字上方画点）。现有的 `ui/reading/RubyText.kt` 继续用。
- **按钮**：`InkButton`（主）、`OutlineButton`、`QuietButton`（文字）、`IconButton44`、`WordTile`（带实投影、按下下沉）。
- **导航**：`TopBar`、`BottomTabBar`、`TextTabs`、`VolumeSwitch`（第一巻/第二巻）。
- **学园物件**：`Seal`（印章）、`StampMark`（「済」）、`StickyTab`（「いま」）、`StudentCard`、`TimetableRow`、`TextbookCover`、`AttendanceCard`。
- **番剧物件**：`DialogueBox`（名牌 + ▼ + 打字机）、`SpeechBubble`（实线；虚线表示话没说完）、`Avatar`（有图用图并加 `MangaImageFilter`，没图显示汉字圆）、`OptionRow`（默认 / 选中 / 对 / 错四种状态）、`FeedbackSheet`。
- **作品身份**：`ui/design/WorkIdentity.kt`，负责 `workSlug` → 作品色、代表角色、角色名 → drawable 的映射。drawable 用 `drawable-nodpi` 里现有的角色图；出席卡里 Re:ゼロ 三季分别用 エミリア / ベアトリス / スバル。
- **Debug 组件画廊**：放在 `src/debug`，把所有组件摆出来，方便截图对照。
- **单测**：进度色插值、竖排标点映射、`workSlug` → 主题。

**完成标准**：`testDebugUnitTest` 通过；旧页面此时可能显示错乱，但**必须能编译**。旧页面引用的旧令牌先留一层 `@Deprecated` 的兼容别名，阶段 3 再删。

### 阶段 2 · 页面（5 个子代理并行，每人只动自己名下的文件）
| 包 | 负责范围 |
|---|---|
| **A 外壳** | `ui/LabApp.kt`（4 个标签、页面过渡、命令面板入口）、登录、设置、学生証、AI 历史、搜索、`update/AppUpdateSettingsSection.kt` 的样式；`LabViewModel.kt` 里的 `LabTab` 枚举由 A 统一改成 `Today / Learn / Library / Review`，把原来的 `Lesson`、`Linguistics` 合并成 `Learn` 加一个子状态 |
| **B 今日与复习** | 今日（一句、時間割）、復習（翻卡堆、苦手、错题本）、智能复习队列 |
| **C 学ぶ** | `LearnScreen`（話 / 場面列表、主视觉）、选番面板（出席卡 + 头像）、言語学（教科書、巻）；从 `LessonScreen.kt` 里拆出 `LessonHubScreen` 相关代码 |
| **D 答题** | `screens/session/*`：アイキャッチ、会話窓各题型（学习卡、配对、选择、填空、拼句、跟读 + 发音评测）、反馈面板 + 角色反应、つづく、读空气答题、基础题库答题。**D 的量最大**，可以再拆成 D1（`LessonScreen` 各题型 + つづく + アイキャッチ）和 D2（读空气 + 基础题库） |
| **E 辞書** | 辞書（词汇、语法、台词三个标签，词条格式，五十音索引）、字幕浏览 |

**并行规则**
- 每个包只能新建或修改自己名下的文件。`ui/design/*` 和 `Theme.kt` 在阶段 2 只读；缺组件时找主代理加。
- `LabViewModel.kt` 只有 A 能改，而且只改 `LabTab`。别的包缺字段时，把需求写进 `design/V3_VIEWMODEL_REQUESTS.md`，由主代理统一加。
- 旧文件改完后，旧的入口函数要删掉，不能留两套实现。
- 动效按 `MOTION_SPEC.md` 做；日常动效直接用组件库里的，别自己写一套。
- 每个包交付前都要自己跑一遍 `testDebugUnitTest` 和 `assembleDebug`。

### 阶段 3 · 清理（1 个子代理，串行）
- 删除 `ui/rive/*`、`ui/fusion/*`、`ui/completion/FusionLessonCompleteScreen.kt`、`ui/components/DuolingoLessonComponents.kt`、`ui/feedback/DuolingoLikeAssetRegistry.kt`、`ui/motion/XpFlyout.kt`、`ui/motion/CountUpText.kt`、`ui/motion/AnimatedLessonNode.kt`、`screens/FusionLessonVisuals.kt`，以及 `Haptics.kt` 里 hla 相关的部分（`HlaPlayer`）；`FeedbackEngine.mascotTrigger()` 也删掉。
- 对应删除或改写的测试：`DuolingoLikeAssetRegistryTest`、`HlaPatternParserTest`、`FusionVisualCatalogTest`、`FusionLessonVisualsTest`、`MotionTokensTest`（按新令牌重写）；`TrainingPathModelsTest` 要看路径模型是否还在用，还在用就保留。
- `app/build.gradle.kts`：删掉 `rive-android`、`lottie-compose` 两个依赖，以及第 96 行的 `res.srcDir("../local-fusion-assets/res")`。
- 用 `git mv` 把 `local-fusion-assets/` 移到 `../archive-content-sources/local-fusion-assets/`。用户的规矩是**素材归档，不删除**；这一步在 android-app 外，执行前先跟用户确认。
- `res/drawable/` 里多邻国来源的矢量图（`duo_*`、`path_bea_*`、`common_level_chest_*`、`in_challenge_junior`、`guidebook_white`、`listen_match_wave_*`、`duoradio_*`）：确认没有引用后，一起归档。
- 全局搜一遍 `Duolingo|duo_|Rive|Lottie|Fusion|mascot|xp|streak`，确认界面层没有残留（数据层的 xp/streak 计算保留）。

### 阶段 4 · 验收（主代理）
- 第 7 节的完整验证命令全部通过；测试数量 ≥ 基线数量减去被删掉的测试数量。
- 在模拟器上逐屏截图，和画布 v3 画板并排对照：浅色和深色各一套；K-ON! 和 Re:ゼロ 各切一次作品色。
- 按 `MOTION_SPEC.md` 第 6 节的验收清单过一遍。
- 在 `design/` 下写一页《v3 落地记录》：和设计稿的差异、已知问题。

## 5. 子代理任务模板（分派时复制）

```text
你负责 Anime Japanese Lab Android 的 v3 UI 重写 · 任务包 <X>。
先读：android-app/design/V3_IMPLEMENTATION_PLAN.md（第 2、3、4 节）、android-app/design/MOTION_SPEC.md。
画布参考：用 Artifact 工具 read https://claude.ai/artifact/9x3RkMeAtAYTN64i8T8HN4，只看 path=project/<画板>.dc.html：<列出画板>。
你名下的文件：<列表>。只能改这些；ui/design/*、Theme.kt、LabViewModel.kt 只读。
缺组件或缺 ViewModel 字段时，写进 android-app/design/V3_VIEWMODEL_REQUESTS.md 后继续做其他部分。
原则：大胆按 v3 实现，不要沿用旧页面的结构和文案；不写问候语和解释性小字；每屏只有一个墨色主按钮。
完成前运行：.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain --max-workers=2（先设置 JAVA_HOME）。
最后汇报：改了哪些文件、和画布有哪些偏差、还有什么没做完。
```

## 6. 风险与应对

| 风险 | 应对 |
|---|---|
| 有的机型没有 CJK 衬线字体（`FontFamily.Serif` 会退回黑体） | 能接受；竖排和字号照样成立 |
| 5 个包一起改，`LessonScreen.kt` 这个 4679 行的大文件容易冲突 | C 只拆 Hub 部分，D 负责题型部分；由 C 先完成拆分并提交，D 在 C 的提交之上开始 |
| 页面缺状态（比如一话里各场面的完成情况） | 先用现有数据推导；推不出来就写进 REQUESTS，不要自己改 ViewModel |
| 额度中途用完 | 每个阶段结束都要**提交一次、保证能编译**；阶段 2 的每个包各自提交 |
| 角色头像只有低分辨率原图 | 用 `MangaImageFilter` 统一处理；图片缺失时显示汉字圆 |

## 7. 命令

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
Set-Location 'C:\Users\汪家俊\jps\android-app'
# 快速：编译 + 单测
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain --max-workers=2
# 完整：再加 lint 和个人安装包
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug assembleLocalSlim --no-daemon --console=plain --max-workers=2
```
Kotlin 编译加 lint 通常要 3–6 分钟，timeout 要设够；控制台长时间没有输出不代表卡住了。

## 8. 完成的定义
- 所有页面都是 v3，App 里找不到多邻国的痕迹，`rive` 和 `lottie` 依赖已经移除。
- 第 7 节的完整命令全部通过，localSlim 安装包能装到真机上用。
- 浅色和深色、两部番的作品色都截图对照过。
- `design/` 下有落地记录，画布上三条开着的评论（出席卡、教科书、会話窓）可以关掉。
