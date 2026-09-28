# android-app · Claude 工作手册

Anime Japanese Lab 的原生 Android App（Kotlin + Jetpack Compose）。**私人自用、非商业、不对外开放**。
用户只用中文沟通。本文是给下一次做功能扩展的会话看的：先看这里，按需再翻其他文档。

## 1. 按需阅读，不要全读

| 要做什么 | 读什么 |
|---|---|
| 环境、工具链、构建变体、服务地址 | `ANDROID_ENVIRONMENT.md`（环境以它为准；README 的功能列表是 v2 时期的，已过时） |
| 挑下一个功能 | `ANDROID_PRODUCT_ROADMAP.md`（后端已有、客户端没接的能力清单，含优先级） |
| 界面长什么样、用哪些规则 | 本文第 4 节；细节看 `design/V3_IMPLEMENTATION_PLAN.md` 第 2 节 |
| 动效 | `design/MOTION_SPEC.md` |
| 已知缺数据、还能补的字段 | `design/v3-requests/*.md` |
| 发布更新 | 本文第 6 节；细节看 `APP_UPDATE_GUIDE.md` |
| 以前每版改了什么 | `CHANGELOG.md`（面向用户的详细更新日志） |
| 活用道場（第三巻 活用）继续做 P3 | `CONJUGATION_DRILL_HANDOFF.md` |
| 自習（学习台）的产品逻辑 | 本文第 3 节「产品主线」；画布「自習 · 学习台（预览）」页 |

设计画布：https://claude.ai/artifact/9x3RkMeAtAYTN64i8T8HN4 （用 Artifact 工具的 `read` 读取，只看 `V3*`、`X*` 开头的画板）。**只在要实现画布上某一屏时才读，且只读那一屏**：`path=project/<画板>.dc.html`。

## 2. 省额度的工作方式（v3 重写的教训）

v3 重写派了 6 个页面包、7 个子代理，合计约 120 万 token。钱主要花在：每个子代理都从零开始，重新读计划、画布、MOTION_SPEC 和大文件，还要排队等 Gradle。做功能扩展时按下面的规矩来：

- **默认主会话自己做，不开子代理。** 一个功能通常只涉及 1–3 个文件，派子代理反而更贵。只有同时重写 3 屏以上、而且彼此独立时，才考虑并行，并且一次最多开 2 个。
- **不写新测试，不截图，不开模拟器。** 用户会自己在手机上测。已有的测试坏了，能顺手修就修，修不了就删掉。
- **验证只做编译**：用 `design/v3build.ps1 -Mode compile`，约 1 分钟。发布前才跑一次 `-Mode full`。
- **大文件只用 grep 定位、分段读。** `ui/LabViewModel.kt` 约 2800 行，绝对不要整读。
- **小事自己拍板，不要问。** 文案、次要入口留不留、和画布的小偏差都自己定，做完在汇报里一句带过。只有删数据、改后端、偏离设计主线这类事才需要问用户。
- **用户说"推送更新"，就是跑发布脚本**（第 6 节），用户会在手机设置页点"检查更新"安装。不要让用户自己打包、装 APK。**一批改动做完、编译和 `-Mode full` 都过了，默认直接发布**，不用再问要不要推。
- **额度可能中途用完。** 做完一块能编译的内容就 commit 一次，这样中断了也能从 git 里接着做。
- **直接在 main 上提交并 push，不开分支、不开 PR、不用 worktree。** 用户一个人开发，分支是多余的。

## 3. 代码地图（v3，0.4.0 起）

包路径 `app/src/main/java/com/animejapaneselab/nativeapp/`：

- `data/`、`domain/`：数据层和 Worker 协议。**做界面需求时不要改。** 本地持久化统一放进 `data/LocalLabStore.kt`（SharedPreferences）。
- `ui/LabViewModel.kt`：唯一的大 ViewModel（各种回调）。状态类型（`LabUiState`、`ReadAirTrainingState`、`LabTab`、`SecondaryScreen` 等）在 `ui/LabUiState.kt`，纯辅助函数在 `ui/LabViewModelSupport.kt`。**新功能要新建独立的 state holder，不要继续往这个文件里塞。** 只有需要复用它的现有状态时，才在这里加字段。
- 独立 state holder 的范例：`ui/study/StudyLog.kt`、`ui/notebook/Notebook.kt`（进程级 `object` + `StateFlow` + `LocalLabStore` 持久化，界面用 `collectAsState`）。
- `ui/LabApp.kt`：底部 5 个标签 今日 / 自習 / 練習（`LabTab.Learn`）/ 辞書 / 復習、二级页面路由、登录门、命令面板入口。自習学习中隐藏底栏。
- **产品主线：自習 → 練習 → 復習。** 自習学（按知识点，材料是带原声的动漫台词），練習测（只出自習学过的课），復習巩固。
  - `ui/jishu/Jishu.kt`（`JishuViewModel`）：一次学习（板書 + 8 张场景句卡）、「覚えた」记录（`pointId::sentenceId`）、场景上下文（按集拉 `/subtitles` 取前后句）、`parseFormula` 把拆解拆成词块。
  - `ui/screens/jishu/`：首页（今日の自習 + 教科書书架）、目次、`JishuSittingScreen`（板書页、场景句卡、遮る）、つづく（小テスト 就地跑本课練習）。卡片上**不显示出处**（集数、时间、说话人），用户明确不要。
  - 72 课讲义在 `assets/conjugation_lessons.json`，由 Antigravity 写、`archive-content-sources/conjugation-drill-p1/lessons/check.py --install` 装入；课列表、台词、已学（`learned`）仍在 `ConjugationDrillViewModel`。一次学习结束就 `markLearned`，这课才进練習。
- `ui/screens/<区域>/`：`today`、`learn`（課程 / 言語学 / 选番面板）、`session`（アイキャッチ、各题型、つづく、读空气、基础题库）、`library`（辞書、字幕）、`review`、`settings`（学生証、AI 历史）、`login`、`search`（命令面板）。`V3Contracts.kt` 里放的是回调合集。
- `ui/design/`：v3 组件库。**写新 UI 之前先 grep 这里有没有现成的**：
  - `Primitives`：Hairline、MangaPanel、Screentone、ProgressLine
  - `Buttons`：InkButton、OutlineButton、QuietButton、IconButton44、WordTile
  - `Navigation`：TopBar、BottomTabBar、TextTabs、VolumeSwitch
  - `Gakuen`：Seal、StampMark、StudentCard、TimetableRow、TextbookCover、AttendanceCard
  - `Anime`：DialogueBox、SpeechBubble、Avatar、OptionRow、FeedbackSheet、PortraitPanel（`speaking` 时头像点头）
  - `Voice`：VoiceBars（声波条）、`Modifier.speechLines`（漫画效果线）、`rememberVoicePhase`；「正在说话」类动效都从这里取
  - `Text`：VerticalText、EmphasisText
  - `WorkIdentity`：`workSlug` 到作品色、角色和头像的映射
- `ui/theme/Theme.kt`：`AjlTheme.colors`、`.type`、`.shape`、`WorkTheme`（按作品切换颜色）。`ui/motion/MotionTokens.kt`：Ease、Dur 等动效令牌。
- `ui/study/StudyLog.kt`：全局学习日志（每天答题数、正确数、时长、自習句数 `recordStudy`），喂给 Today 的「最近 12 週」格点（`screens/today/StudyHeatmap.kt`）。**新增任何答题型 session，判定对错的地方都要调 `StudyLog.record(...)`**，否则格点和时长不计。
- `platform/LearningSessionNotifier.kt`：学习中的常驻通知（Android 16 ProgressStyle 分段、作品色、角色头像、计时）；内容来自 `ui/LearningSessionStatus.kt`。
- `platform/StudyReminder.kt`：放課後チャイム，每天定时（非精确闹钟）检查 StudyLog，当天没答题才发通知；开机/更新/换时区时重新排程。
- **知識（底栏第 5 个，0.12.0 起，原「復習」）**：无限刷的知识点卡片流。卡片数据 `assets/knowledge_cards.json`（每份学习文档一个合集），模型和解析在 `ui/knowledge/KnowledgeCards.kt`，♥ / 读过 / 30·90 天检查存在 `ui/knowledge/Knowledge.kt`（`KnowledgeRules.pick` 是推送顺序），卡面在 `screens/review/KnowledgeCardBody.kt`。**新文档交给 Antigravity**：`archive-content-sources/knowledge-cards/`（`ANTIGRAVITY_PROMPT.md`、`check.py --install`、范例 `decks/inf.json`，gitignore），装入后发版即可。卡上内容全部直接摊开，**不要再加展开面板或「相关」跳转**（用户明确不要）。
- `ui/review/ReviewFeed.kt` + `screens/review/ReviewFeedScreen.kt`：知識流的外壳（0.11.0 的復習刷卡流改造而来）。流 = 知识卡（2 张）+ 资料单词卡（1 张）循环，快到底时 `settle` 自动接一批；到期卡（活用/收藏/错题/苦手）用 `FeedRules.weave` 穿插。`FeedRules.due` 把活用到期句、收藏、本地错题（合并同 id 的服务端任务）轮流排成一条流，`ReviewFeed`（进程级 object）存当天卡序和位置；判定经 `ReviewSinks` 写回 `ConjugationDrillViewModel.gradeLine` / `Notebook.grade|master` / `markMistakeReviewed` / `LabViewModel.gradeReviewTask`。卡片本身只加新的 `FeedKind`，不要再做单独的复习入口。
- `ui/notebook/`：栞（跨集生词本）。`data/NotebookModels.kt` 是模型、Leitner 规则（1/2/4/8/16 天）和编码；辞書第 4 个标签、翻卡复习（辞書和復習都有入口）、今日の一句和字幕页的栞按钮都在这里汇总。
- `widget/TodayWidget.kt`：桌面小组件「今日の一句」。RemoteViews 不支持竖排，所以整块画成位图；数据由 Today 页写入 `LocalLabStore`。
- `data/Conjugator.kt`：按规则推导动词/形容词活用表（不需要数据），辞書词条展开时显示。
- `data/CardEnrichment.kt`：解析 worker 下发的 AI 增强卡 `cardPayload`，**解析时会过滤模板话术和对不上词头的卡**；新发现的模板句加进 `FillerMarkers`。
- 登录：`LocalLabStore.readCachedUser()` 有值就直接进 App，`refreshAuthState()` 在后台校验，遇到 401 才退回登录页。不要改回「先等网络再放行」。
- `update/`：App 自更新，包括下载、校验、交给系统安装器。
- 调试时 `src/debug` 里有 `DesignGalleryActivity`（组件画廊）。

## 4. v3 设计规则速查

- 世界观：**制度用学园物件**（登校、学生証、時間割、出席カード、教科書），**内容用番剧语法**（第X話、場面 01–06、アイキャッチ、会話窓、つづく、次回予告）。
- 颜色只用 `AjlTheme.colors` 的令牌，不写硬编码色，浅色和深色都要能用。作品色（K-ON 桜、Re:ゼロ 菫、默认 藍）只用在播出线、进度线、当前项、印章、网点、名牌上。
- 形状：剧情和内容用 **1.5px 墨线、4px 圆角**（漫画框）；工具和设置用 **1px 细线、12px 圆角**。不用阴影，只有字块和当前教科書允许 2–3px 墨色实投影。
- **每屏最多一个墨色主按钮（InkButton）**。不写问候语，不写解释性小字。
- **顶部作品色「播出线」（BroadcastLine）已按用户要求去掉**，别再加回主页面。
- **重做界面前，先对照旧版画布（v2 的 `Today`、`TodayDark` 等画板）列出用户喜欢的元素**，不能默默删掉。v3 重写就丢了「最近 12 周」格点和学习时长，用户专门要回来。
- 字体：日语原文用 `FontFamily.Serif`，界面文字用系统黑体，元数据（集数、计数、时间码）用 IBM Plex Mono。
- XP 和连胜只从界面上去掉了，`learningXp` / `learningStreakDays` 的计算还在，不要删。
- 已移除、不要再引回来：DuolingoSans、Rive、Lottie、hla 触觉、吉祥物、路径节点。音效只在判定对错时响，用的是 Kenney CC0 音效。

## 5. 加一个功能的标准流程

1. 从 `ANDROID_PRODUCT_ROADMAP.md` 或用户描述里确定范围。后端已经有的接口，直接在 `data/` 里找 client 调用。
2. 界面放在对应的 `ui/screens/<区域>/`，只用 `ui/design` 的组件，缺什么就在 `ui/design` 里补一个通用的。
3. 状态放进新的 state holder，本地持久化用 `LocalLabStore`。
4. 编译通过（`v3build.ps1 -Mode compile`）后 commit。
5. 需要发给手机时，按第 6 节发布。

## 6. 构建与发布

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
# 编译（在任意目录都能跑，有全局锁）
powershell -ExecutionPolicy Bypass -File C:\Users\汪家俊\jps\android-app\design\v3build.ps1 -Mode compile
# 发布前：单测 + debug 包
powershell -ExecutionPolicy Bypass -File C:\Users\汪家俊\jps\android-app\design\v3build.ps1 -Mode full
```

发布更新，也就是用户说的"推送更新"：
1. 在 `app/build.gradle.kts` 里给 `versionCode` 加 1，`versionName` 按需改。版本号不高于线上的话，脚本会拒绝发布。
   **同时在 `CHANGELOG.md` 最上面加一节**（用户要求：每次更新都要详细记录改了什么、为什么、在哪能看到），`-ReleaseNotes` 用这一节的摘要。
2. 跑发布脚本：
   ```powershell
   $env:Path = "C:\Program Files\nodejs;$env:Path"   # PowerShell 默认找不到 node，wrangler 会失败
   Set-Location C:\Users\汪家俊\jps\android-app
   powershell -ExecutionPolicy Bypass -File .\scripts\publish-app-update.ps1 -ReleaseNotes '<中文更新说明>'
   ```
   脚本会打 `localSlim` 包，上传到 R2，再更新 `latest.json`。如果包已经打好，可以加 `-SkipBuild` 跳过打包。
3. 验证：`curl https://anime-japanese-lab-android-updates.ishallnotwant123.workers.dev/v1/latest`，看 versionCode 是不是新版本。

## 7. 坑

- **这个 GitHub 仓库是公开的。** 多邻国来源和参考用的素材只能本地使用，靠 `.gitignore` 挡在仓库外，不能提交。`archive-content-sources/` 里的归档素材同样如此。
- **素材只归档，不删除**，统一移到仓库根目录的 `archive-content-sources/`。`android-app/local-anime-assets/` 是用户的试验图，不要动，也不要提交。
- `res/drawable-nodpi/`（角色头像）被 gitignore 了，但 App 在用。新开的 git worktree 里没有这个目录，`v3build.ps1` 会自动用目录链接（junction）把它补上。
- **删除 worktree 之前，先用 `cmd /c rmdir` 拆掉里面的目录链接。** 否则递归删除会顺着链接，把主仓库的素材也删掉。Windows 路径过长删不掉时，用 `Remove-Item -LiteralPath "\\?\<完整路径>" -Recurse -Force`。
- 素材许可证记录写在仓库根目录的 `docs/assets-license.md`。改动素材打包方式时，要同步更新这里。
- 登录是必须的，不做手写功能，Web 前端不是规范；默认只改 `android-app/` 下的文件。

## 8. 经验记录（每次会话结束补几条）

**2026-09-28 · 0.13.0（辞書手势化）**
- 用户嫌「一排按钮 + 10 格表」死板：手机就该用点、滑、长按。规矩：**点词 = 发音，点行 = 拉起词卡，右滑 = 斩，左滑 = 收藏，长按 = 多选**；破坏性手势（斩）都要过线才生效、震一下，并给 4 秒撤销。新做列表类界面照这个来。
- 通用组件：`ui/design/Gestures.kt`（`SwipeActionRow`、`UndoBar`）。词卡是 `screens/library/WordCard.kt`，变形规则是 `ui/words/FormDial.kt`（在 `Conjugator` 的表上切出词干 / 变的部分 / 五段那一列），`data/Conjugator.kt` 没改。
- 右侧 あかさたな 是五十音索引，用户误以为是五段。词少于一屏时不显示。
- 「栞」这个字用户不认识，0.12.0 漏改了辞書、通知、小组件，这次全部改成「收藏」。新文案一律用「收藏」。

**2026-09-28 · 0.12.0（復習 → 知識 无限流）**
- 用户的本意：復習应该是「知识点卡片流」而不是「到期错题流」——到期的东西刷几张就没了，达不到抖音效果。现在知识卡是主体、到期卡穿插；没有おわり卡，也没有おかわり。
- 用户读卡是「直接看」：画布上的「展开」「相关」按钮被否了，内容一律摊在卡上（卡内可滚动）。「栞」这个字用户不认识，界面上改叫「收藏」，新文案别再用 栞。
- ♥ = 掌握；和抖音一样双击也是 ♥。知识卡 ♥ 后 30/90 天用 `quiz.tests` 指向它的自测题回来考，答错自动取消。
- 推荐算法的结论：单用户不做云端推荐（没有数据可学），排序规则留在本地；云端以后只做卡片内容下发和 ♥ 同步（还没做）。
- Pager 的 key 必须唯一：同一张卡再次出现时 `ReviewFeed.more` 给它加 `#n`。
- 还没做：知识卡和 ♥ 状态上云；把 0.9.x 以来积压的本地数据（notes3 备注等）推到 Supabase（用户说以后再解决）。

**2026-09-27 · 0.11.0（復習刷卡流）**
- 用户想要的是「像抖音一样好玩」：全屏竖滑、上划即记得、双击盖章、右侧按钮栏、底部面板像评论区。先在画布画了「復習 · 刷卡流（预览）」页（`Reel*.dc.html`，一个可交互模板生成多块画板），用户确认后一次实现。
- 復習的规矩：**判定一次、写回原处**，不要再出现「翻了卡但不记排程」的入口。本地错题没有排程，用 `review-mistake-due` 记「记得一次 → 2 天后再来，再记得就移出」。只有标题没内容的服务端任务做不成卡，留在帳面。
- 这次会话中途，`vocab_cards.json` 被另一边（Antigravity 第三轮备注）装入了新版；提交前用 `git status` 看清楚，别人的改动单独提交，不要混进功能提交。

**2026-09-27 · 0.10.1（产品方向：不改成 JLPT 大纲驱动）**
- 用户考虑过把学习改成按 N5 → N1 组织，讨论后**决定不改**：App 的核心就是动漫素材，按作品 / 话组织的辞書、課程保留，JLPT 只作筛选标签。不要再主动提议大改；完整讨论和当时的数据现状在 `CHANGELOG.md` 的 0.10.1 一节。
- 单词卡 note 由 `VocabCards.tidyNote` 去掉开头抄的台词；校对过的卡片 note 为空就是空，**不要**退回 `realWorldNote`（`WordCard.checked`）。

**2026-09-27 · 0.10.0（产品逻辑大扫除 + 斩）**
- 用户最烦的是**不合常理的产品逻辑**，不只是 bug。规矩：结算页的主按钮是「完成」（回到进来的地方），「再来一组」最多当次要按钮，并且只在有意义时出现（还有到期、有错题）；只有「下一课 / 下一話 / 続き」这种往前走的动作可以当主按钮。新做 session 结算页照这个来。
- 斩：`ui/words/KnownWords.kt`（按词头记，本地存 `known-words`），辞書词汇页有「已斩」档案可以恢复。`vocab_cards.json` 每行可以带第 8 列 `easy`（Antigravity 任务 `ANTIGRAVITY_EASY_PROMPT.md`，`check.py --install` 写入），在那之前用 `KnownWords.Obvious` 里内置的约 30 个词。課程（按话的 `SampleLearningRepository`）还没有跳过已斩的词，因为那在 data 层。
- 今日時間割 = 一限 自習 / 二限 練習（活用到期）/ 三限 復習（卡片 + 栞）/ 之后是按话的課程、读空气、跟读。今日点自習用 `JishuViewModel.requestStart` 直接开课；「接着学哪课」的规则是 `JishuState.currentPoint`，自習首页和今日共用。
- grep 找调用方时要连函数引用一起搜（`::topicFor`），审计里「没人用」的判断就是这么错的。
- 没做、留给下次：活用练习的选项加罗马音（AUDIT C1/C4，需要 furigana）；課程「先学这个词」学习卡换成 `WordStudy` 的版本（C3）；練習 tab 的按话「課程」去留（A3，要用户定）。

**2026-09-27 · 0.9.1（单词卡数据装入）**
- Antigravity 的单词卡：`check.py` 全过（3770，keep=false 6.7%），抽查 40 词读音全对；问题只在措辞（「微缩微景」式叠词）和约 15% 复述释义的空备注，不影响教学，直接装了。以后精修就针对这两类返工。
- `vocab_cards.json` 约 490KB，只含读音/释义/备注，不含字幕原文，可以进仓库；`archive-content-sources/vocab-cards-v1/` 仍不能提交。
- 课程学习卡（`StudyCardQuestion`）在 `sourceKind == "vocab"` 时用 `VocabCards` 覆盖读音、释义，备注换成「词性 / 辞书形 / note」，旧的 `realWorldNote` 和出现次数不再显示。服务端词汇题仍是旧数据，写回 Supabase 要 service role key（交接文档第 4 节）。
- 0.9.2：用户给了 Supabase 个人 access token（`sbp_`），走 Management API（`/v1/projects/<ref>/database/query`）可以直接跑 SQL 和 DDL，不需要 service role key。数据已写回云端（`push_cloud.py`），worker 只下发 `is_study_word=true`。**用 `create table … as` 在 public 下建的表默认不开 RLS，匿名 key 能读到，建完立刻 `enable row level security`。**
- 云端的 `pos` 现在是中文标签（`な形容词`、`动词`），`Conjugator` 已兼容；新代码判断词性时两种标签都要认。
- 用户以为 App 是纯本地、不联网的；其实登录、課程、辞書、AI 都走 worker，只有 `vocab_cards.json` 是本地覆盖。以后云端逻辑要当作唯一数据源来做，本地 asset 只是兜底。

**2026-09-27 · 0.9.0（读音辅助 + 学生証 + 单词练习）**
- 读音辅助统一走 `ui/reading/Kana.kt`（假名→罗马音、按音拍切分、`LineReading` 把一句拆成「汉字词+读音 / 单个音拍」）和 `ReadingLineText`（上注假名、下注罗马音、目标词标色）。读音来自 Worker 的 furigana（`rememberFuriganaAnnotator`），受设定「假名注音 / 罗马音」开关控制。用户不熟假名，**凡是显示日语的学习界面都要考虑罗马音**。
- 进度统一用 `ProgressLine`，不要再做分段进度条。声音按钮：`VoiceSwitchPill`（左右滑切原声/TTS）+ `VoiceTone`（播放时网点像声波扩散），在 `ui/design/VoiceSwitch.kt`。
- 学生証编辑：`ui/profile/StudentProfile.kt`（氏名/所属/照片，照片存在 filesDir）。入学、出席不可改。
- 单词练习：`ui/words/WordDrill.kt`（纯规则）+ `screens/library/WordStudy.kt`（全屏 Dialog）。`learning_vocab_items` 的读音/释义/备注很多是错的（大丈夫=だよ、戻れ=もどる、「EP17筛选…」），重写交给 Antigravity：`archive-content-sources/vocab-cards-v1/`（`export.py` 导出批次、`check.py` 校验、`--install` 写入 `assets/vocab_cards.json`，App 用 `VocabCards` 自动覆盖）。字幕的中文和日文经常错位，别当译文用。

**2026-09-27 · 0.8.4（最近 12 週 + 活用讲解排版）**
- 格子的规矩（用户定的）：**学完一次才点亮**。`StudyDay.finished` 在 `TsuzukuScreen` 首次出现时、栞翻卡复习做完时各 +1；只答了题没学完的那天只画小点。新 session 结束页不走 `TsuzukuScreen` 的，要自己调 `StudyLog.finishSession`。时长是「答题间隔（≤180s）」估算的学习时间，不是 App 前台时间；累计值存 `study-total-seconds`。
- 讲解类文字统一用 `ui/design` 的 `NoteText`（「」内衬线加粗、`**…**` 加粗、→ 作品色）和 `MarkedLine`（目标词加粗、作品色、下划线）；拆解词块是 `screens/jishu/Formula.kt` 的 `FormulaRow`。用户很讨厌通用教科书段落和「深入/回到」来回切换的按钮，不要再加回来。

**2026-09-27 · 0.8.0（智能提醒 + AI 进主线）**
- 提醒：`platform/ReminderPlanner.kt`（纯规则：习惯时间 = 近 14 天首次学习时刻中位数 −15 分钟；朝 8:30 / 习惯 / 復習 三个检查点，每天最多 2 条；断更 1–7 天每天、8–14 天隔 3 天、第 15 天说「先不提醒了」后静默）+ `StudyReminder.kt`（闹钟链、发通知、通知按钮）+ `ReminderHealth.kt`（通知权限 / 小米自启动 / 省电无限制）+ `MorningPick.kt`（朝の一句从「最久没复习的已学課」挑原作台词）+ `TodayLineAudio.kt`（台词音频缓存，通知里直接播）。
- 之前的提醒从没生效：默认开着，却只在手动拨开关时才申请通知权限。**新加任何通知功能，都要确认权限在默认路径上会被申请**；小米还必须开自启动和省电无限制，否则闹钟不响。
- AI：默认 `gemini-3.5-flash-lite`（另有 `gemini-3.6-flash`），旧 id 在 worker 和 `LocalLabStore` 里自动映射。Gemini 3.x 弃用了 `temperature`，思考深度用 `reasoning_effort`（按任务在 worker 的 `effortFor` 里定），分栏讲解走 `callAiSections`（JSON schema 结构化输出）。`.dev.vars` 里有 `CF_AIG_TOKEN`，改 AI 参数前可以先直接调网关实测。
- AI 用在判断和生成上（答错点评 `/api/ai/quick-feedback`、自習つづく的作文批改），数据里已有的东西（活用拆解、原作台词）不要硬塞给 AI。

**2026-09-27 · 0.7.0（自習 tab）**
- 用户的产品逻辑：**素材来自动漫（字幕、原声），学习融进场景**，但**自習按知识点组织，不按剧集**；出处信息都不要。先学后练：学过的才进練習。
- 做大界面前**先在设计画布上画预览**，用户确认后再写代码；这次在画布加了「自習」页 5 块画板。
- 讲义这类批量内容交给 Antigravity：仿照 `conjugation-drill-p1/ANTIGRAVITY_PROMPT.md` 写提示词 + 分批 md + 校验脚本（拒收模板话术、半角公式、抄台词），我只做工程。**先把提示词交给用户，再写代码**，两边并行。
- 画布发布：scratchpad 用长路径（`C:\Users\汪家俊\AppData\Local\Temp\...`），短路径 `6058~1` 会被读取权限规则拦；发布前先 `read` 一次整个画布（不带 path）。

**2026-09-26 · 0.6.0（用户放权的大改动轮）**
- 工具链：Kotlin 2.3.21、Compose BOM 2026.06.01（Compose 1.11）。**Compose 1.12 起要求 compileSdk 37**，本机只有 36.1，要升得先装 SDK 37 并换 AGP 9。
- 后端（`src/worker.ts`，部署：仓库根目录 `npx vite build` 后 `npx wrangler deploy`；PATH 里要先加 `C:\Program Files\nodejs`）：
  - 会话改成滑动续期（活跃时最多每天续一次，续满 30 天），不再每月被踢出。
  - `/vocab` `/grammar` `/sentences` 下发 `cardPayload`（AI 增强卡）。
  - `/subtitles` 每行附带对应句子的 `audioUrl`/`storagePath`，字幕页可逐行播原声（原声只在 Re:ゼロ 第 26–50 话）。
- **worker 的 `/sentences` 固定 `limit=30`，课程进度按这 30 句计算**，不要直接调大，否则每集进度会被稀释。要更多原声就走 `/subtitles` 的 audio 字段。
- Cloudflare 会拦 Python 默认 UA（403），脚本里请求 worker 要带浏览器 User-Agent。
- 用 PostgREST + `wrangler.toml` 里的 publishable key 就能只读查内容表（`learning_card_enrichments` 等），不需要管理 token。

**2026-09-26 · 0.5.1（按用户手机截图的红字批注逐条修）**
- 用户的反馈方式是：手机截图，用红字标出问题，放进 `android-app/photo/`（不提交）。按「读完全部截图 → 每张图对应一处改动 → 汇报里逐条对上」的流程处理。
- **题目构造的通病：先打乱再截断会把正确答案截掉，`.distinct()` 会吞掉重复的正确词块**（例如「やばい…これは本気でやばい」里有两个「やばい」）。在 `SampleLearningRepository` 里构造选项或词块时：正确项一个不少、重复的也保留，干扰项只用来补足空位。新题型也照这个规矩写。
- 「点了 A 还要再点 B 才能开始」这种两步操作，用户会认为交互不合理：点入口就应该直接开始。
- 动效要让人看出「有东西在发生」：用户对静态的播放按钮不满意。播放、录音、加载这类状态都要有可见的动效，同时照顾 reduced motion。
- 工具坑：
  - PowerShell 5.1 的 `Set-Content -Encoding UTF8` 会给文件加 BOM（`build.gradle.kts` 首行被污染）。改版本号用 Edit 工具或 `sed`。
  - Bash 里用 heredoc 塞「Python 脚本 + 含引号和正则的 Kotlin」容易引号失配而报错；长替换脚本先写进 scratchpad 的 `.py` 文件再运行。
- 待办（本次没做）：
  - ~~辞書词条补动词活用形~~：0.6.0 已用规则推导实现（`data/Conjugator.kt`）。
  - ~~服务端会话滑动续期~~：0.6.0 已上线。

