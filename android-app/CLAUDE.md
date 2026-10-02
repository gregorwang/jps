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
| 语法点 / 台词译文 / 练习题的数据清洗（Antigravity 交回后） | `CONTENT_CLEAN_HANDOFF.md` |
| 罗马音 / 读音显示逐屏修正（用户发截图指导） | `ROMAJI_FIX_HANDOFF.md` |
| 爱蜜莉亚声线 TTS（批量预生成音频，替掉微软 TTS） | `../archive-content-sources/emilia-voice/HANDOFF.md`（本地，gitignore） |
| 语音包（设置 → 语音包）的 bug、念错的词、重新打包 | `VOICE_PACK_HANDOFF.md` |
| 単語卡（5 个一组、先想再看，0.19.0 已做）、帳面重排、错题搬去練習 | `VOCAB_CARD_HANDOFF.md` |
| 単語卡背面「常一起出现」补搭配数据（日文 + 中文） | `COLLOCATION_HANDOFF.md` |
| 辞書「高频补充」（动漫高频 JLPT N5–N2 单词 / 语法，也进単語）的数据和脚本 | `FREQ_WORDS_HANDOFF.md` |
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
- `ui/screens/<区域>/`：`today`、`learn`（課程 / 言語学 / 选番面板）、`session`（アイキャッチ、各题型、つづく、读空气、基础题库）、`library`（辞書、原作 = `SubtitlesScreen`）、`review`、`settings`（学生証、AI 历史）、`login`、`search`（命令面板，按 `SearchScope` 分范围，见 0.22.0 经验）。`V3Contracts.kt` 里放的是回调合集。
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
- **知識（底栏第 5 个，0.12.0 起，原「復習」）**：无限刷的知识点卡片流。卡片数据 `assets/knowledge_cards.json`（每份学习文档一个合集），模型和解析在 `ui/knowledge/KnowledgeCards.kt`，♥ / 读过 / 30·90 天检查存在 `ui/knowledge/Knowledge.kt`（`KnowledgeRules.pick` 是推送顺序），卡面在 `screens/review/KnowledgeCardBody.kt`。**知识卡由主会话（强模型）自己读完文档来做，不再交给 Antigravity**（用户 2026-09-30 定：理解得越深，卡越好）。卡片数据写在 `archive-content-sources/knowledge-cards/decks/`（`s2.json`、`s4.json`、`threads.json`，卡片内容 `canvas-gen/decks_data.py`，24 种卡型的画法 `canvas-gen/gen_decks.py`（路径常量指向当时的 scratchpad，重跑前改掉）），画布「知識 · 第二篇 / 第四篇（卡片）」「系列线索」三页是预览。**0.17.0 起 asset 是格式 2**（`version: 2`，deck 带 `order`/`short`，卡片字段原样放在 `KnowledgeCard.data`），24 种卡型画在 `screens/review/KnowledgeKinds.kt`（【x】= 目标词，［］= 括号）；新增卡型 = `KnowKind` 加一项 + 这里加一个画法。装入：在 `canvas-gen` 里把 decks 写成 `{version:2, decks:[…]}` 覆盖 `assets/knowledge_cards.json`。**0.16.1 起 App 里没有知识卡合集**：唯一的 `inf` 被用户否了（整张卡在讲英语本身）。英语**可以当锚点**（一行小字「英语里是 wait for」），卡的主角必须是日语；专讲英语的章节不做卡。用户的文档是一个系列（不定式→影山→词性→补语定语→介词…），后篇反复回收前篇的知识点（連用形、に、の/こと、ように），**这是承上启下，不是重复，不要去重**。卡上内容全部直接摊开，**不要再加展开面板或「相关」跳转**（用户明确不要）。
- `ui/review/ReviewFeed.kt` + `screens/review/ReviewFeedScreen.kt`：知識流的外壳（0.11.0 的復習刷卡流改造而来）。流 = 知识卡（2 张）+ 资料单词卡（1 张）循环，快到底时 `settle` 自动接一批；到期卡（活用/收藏/苦手）用 `FeedRules.weave` 穿插；**错题不进知識流**（0.16.1，用户不要），只在帳面 → 错题本里刷。资料单词卡的词头来自 `vocab_cards.json` 的 `heads`（id 是 UUID/编号的词条，约 2400 个），不能从 id 里切。`FeedRules.due` 把活用到期句、收藏、本地错题（合并同 id 的服务端任务）轮流排成一条流，`ReviewFeed`（进程级 object）存当天卡序和位置；判定经 `ReviewSinks` 写回 `ConjugationDrillViewModel.gradeLine` / `Notebook.grade|master` / `markMistakeReviewed` / `LabViewModel.gradeReviewTask`。卡片本身只加新的 `FeedKind`，不要再做单独的复习入口。
- `ui/notebook/`：栞（跨集生词本）。`data/NotebookModels.kt` 是模型、Leitner 规则（1/2/4/8/16 天）和编码；辞書第 4 个标签、翻卡复习（辞書和復習都有入口）、今日の一句和字幕页的栞按钮都在这里汇总。
- `widget/TodayWidget.kt`：桌面小组件「今日の一句」。RemoteViews 不支持竖排，所以整块画成位图；数据由 Today 页写入 `LocalLabStore`。
- `data/Conjugator.kt`：按规则推导动词/形容词活用表（不需要数据），辞書词条展开时显示。
- `data/CardEnrichment.kt`：解析 worker 下发的 AI 增强卡 `cardPayload`，**解析时会过滤模板话术和对不上词头的卡**；新发现的模板句加进 `FillerMarkers`。
- 登录：`LocalLabStore.readCachedUser()` 有值就直接进 App，`refreshAuthState()` 在后台校验，遇到 401 才退回登录页。不要改回「先等网络再放行」。
- `ui/voicepack/VoicePack.kt`：爱蜜莉亚语音包（设置 → 接続 → 语音包，导入一次 zip 到 `filesDir/voice-pack`）。`LessonAudioController.playTts` 最先按 sha1(原文.strip())[:16] 查它，没有再走本机 / 远程 TTS。语音包由 `archive-content-sources/emilia-voice/batch.py` 生成，**音频不进仓库**。
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

**2026-10-02 · 0.24.0（内容改成本地包）**
- 用户以为数据都在本地，实际上課程 / 辞書 / 自習 / 字幕每次都现拉 worker，所以页面转圈。现在 `scripts/build-content-pack.py` 把所有只读内容接口（`isCacheableContentPath` 那一组，两部作品全部话）抓成 `assets/content/<sha1(path)>.json`，`RemoteLabClient.get` 先查 `data/ContentPack`，命中就不联网。**这个目录有字幕原文，已 gitignore**；发布脚本每次先重抓一遍（`-SkipContentPack` 可跳过），新 worktree 里没有它时自动退回联网。
- App 新加只读内容接口时：路径加进 `isCacheableContentPath`，并在 `build-content-pack.py` 里照 App 的拼法加上，两边路径字符串必须一字不差（URLEncoder 编码）。
- 组题（`buildLessonNodes`）原来在主线程的 `_uiState.update` 里跑，会卡住アイキャッチ；已挪到 `Dispatchers.Default`。别再把重计算塞进 `update { }`。

**2026-10-01 · 场景搜索页（进 0.23.0，和另一个会话的改动一起发，发版等用户点头）**
- 代码：`ui/search/SceneSearch.kt`（进程级 holder + `SceneRules`），`screens/search/SceneSearchScreen.kt`（整页 + 场景面板），路由 `SecondaryScreen.SceneSearch`。今日搜索回车只给 2 个场景 + 「全部场景」，原作页一个搜索框 + 「在全部原作里按意思找」一行。worker `/api/rag/search` 收 `workSlug:"all"` + `explain:true`：两部作品一起搜，Flash-Lite 给命中句打 0/1/2 相关度、现翻中文（字幕中文常错位）、`why`、`mark`，并按 `learning_sentences` 附原声。一次约 9 秒。
- 用户定的界面规矩（画布评论）：**播放不用圆形 ▶ 按钮，波形本身就是播放键**（`ui/design/Voice.kt` 的 `VoiceWave`）；**搜索不按作品分开**。
- 向量库保持 30 句一段（1182 条）。按单句或 5 句重建索引都被否了：用户是 Workers 付费版但**不想触发超出包含额度的按量计费**，收益也不大。
- **多个会话同时在改项目时不发版**（用户明确说过），只 commit；提交前看 `git status`，别人的改动不混进自己的提交；会话之间用 SendMessage 对齐谁的文件、谁发版。

**2026-10-01 · 0.22.1（联网实测：AI 500/400、场景搜索、模型更新）**
- **0.22.0 没做任何联网测试就发了**，用户在手机上发现 AI 接口报 500/400、场景搜索结果全不相关。规矩：**动到联网功能（worker、AI、搜索）时，发版前必须跑 `scripts/probe-ai.py`**（`AJL_EMAIL` / `AJL_PASSWORD` 环境变量，账号直接问用户要，别自己翻文件或造会话），每行都要 200；搜索类功能还要看结果内容对不对，不只看状态码。
- 查出来的坑：worker 的 `allowedCacheKinds` 漏了 `quick_feedback`（AI 写好了，存缓存时抛错变 500）；App 调 worker 的 `readTimeout` 原来只有 25 秒（AI/RAG 现在 120 秒）；向量库 metadata 是 `rezero`、字幕表是 `re-zero`，且 `subtitle_chunks` 里没有 Re:ゼロ，按时间窗取行。
- 模型清单别凭记忆：用 `.dev.vars` 的 `CF_AIG_TOKEN` 走网关列 `google-ai-studio/v1beta/models` 和 `grok/v1/models`（Python 要带浏览器 UA，否则 1010）。Gemini 3.x 的思考 token 算在 `max_tokens` 里（会把正文截断），3.8 Flash 不支持 `minimal`。非默认模型失败一律退回 Flash-Lite（`callAiGateway`）。
- 场景搜索：中文描述先由 Flash-Lite 改写成日语台词再搜（`expandSceneQuery`），块内逐句用 bge-m3 打分标 `hit`（`rankSourceLines`）。剩下的瓶颈是索引粒度（30 行一块，召回不到），要按单句重建索引，**改后端数据，先问用户**。

**2026-10-01 · 0.22.0（搜索分范围 + 辞書去掉台词）**
- 搜索还是一个 `CommandPalette`，范围由打开它的页面决定（`LabApp` 的 `paletteScope`，打开那一刻定死）：原作 → `Scenes`（向量搜索 + 「AI 挑一个场景」`/api/rag/suggest-training-query`），辞書 → `Dict`（整本 `LevelDict` + 当前话，`SearchIndex.kt` 的 `rank`），知識 → `Knowledge`（知识卡全文），其余 → `All`。練習没有搜索入口。新页面要搜索就加一个 scope，别再让所有页面共用一个范围。
- 搜索结果要**直接打开目标**：词 / 语法用 `DictEntrySheet`（任何页面上弹词卡、语法卡），知识卡用 `ReviewFeed.show`（插到当前卡后面并翻过去，知識流还没建好时先挂起）。只切 tab 不算「打开」。
- 辞書只剩 词汇 / 语法 / 收藏；台词全部在「原作」（原字幕页），`LinesPage` 已删。用户的判断：台词是整个素材库，不该在辞書里放一份每话 30 句的切片。
- 网页版还没搬的只剩 `/api/rag/generate-question(s)`、`save-question(s)`（出题写进草稿表，属于出题后台，不搬）和手写练习（不做）。

**2026-10-01 · 0.21.0（12 篇文档全部做成知識卡，共 370 张）**
- 新卡数据在 `archive-content-sources/knowledge-cards/canvas-gen/decks_more.py`（S2/S4 仍在 `decks_data.py`），`python build_asset.py` 直接校验（kind、【】配对、quiz 答案、tests 指向）并写 `assets/knowledge_cards.json`，不再经过画布。一篇做完就 build + commit asset，中断了能从 git 接着做。
- 用户定的原则：**重复没关系，讲错不行**。原文档（AI 写的）错处不少（失敗な、母的归属、作り方接原形、妹=み、蹴る 古典是下一段……），卡上一律改正，并在汇报里列出来。
- 字形微差卡只能放 Unicode 不同的字（歩/步）：器、内 这类中日同码，手机上显示一样。
- 长内容别用 bash heredoc 塞 python（会截断），直接用 Edit 往 `decks_more.py` 里加。

**2026-09-30 · 0.19.0（単語重做 + 帳面重排）**
- 用户看完画布说「照那个来」，就不必再逐条确认交接文档里的问题，直接做。
- 単語是**独立的 pager**（`TangoScreen`），不走 `FeedRules` / `ReviewFeed`：5 张词卡 + 1 张小测卡，`Tango`（`ui/words/Tango.kt`）存 Leitner 箱和当前组，小测答完才动箱；♥ 掌握 = `KnownWords.setWord`（和辞書的已斩是同一份），收藏 = `Notebook.toggle`。旧的 `VocabDeck` 伪合集和 `VocabBody` 已经没有入口，可以删。
- 单词的原作台词是**离线配好的**：`fetch_subtitles.py` 从 Supabase 拉 `subtitle_lines`（Re:ゼロ 的 `usable_for_analysis` 是 null，别加这个过滤）+ `learning_sentences` 里有原声的，`build_lines.py` 每词挑 1 句（有原声 > 整词命中 > 长度适中），写 `assets/vocab_lines.json`（音频路径去掉公共前缀）。没有台词的词不进単語。缓存在 `vocab-cards-v1/cache/`（gitignore 的 archive 里）。
- 错题 / 期日の課題 / 苦手不再在知識页：入口是 練習 → 課程 場面 06 →「復習の順番」（`openSmartReviewQueue`），从那里开始的练习做完回 練習 tab（`LabViewModel` 里复习流程的 `selectedTab` 都改成了 `Learn`）。今日的三限 復習 仍指向知識 tab。
- `v3build.ps1` 的日志是 UTF-16，`grep` 要先用 `tr -d` 去掉 NUL 字节；编译错误在日志里的行号会被折行。
- 待用户手机验证：単語 卡的翻页手感（没翻开不能划走）、小测里「挖空 + 听」够不够答、5 个一组的节奏。

**2026-09-30 · 0.18.0（知識流手感 + 掌握/收藏 + 单词移出）**
- 翻页卡顿的根因：卡内 `verticalScroll` 抢走手势，Pager 的 page nested-scroll connection 在 `onPostFling` 吞掉剩余速度，只能靠拖过半张翻页。修法在 `screens/review/FeedSwipe.kt`：`feedFlingBehavior`（18% 就翻）+ `rememberFeedPageHandoff`（卡已经带动 Pager 时，在 `onPreFling` 自己翻页）；`KnowBody` 内容放得下时关掉卡内滚动。以后 Pager 里放可滚内容都照这个做。
- 用户对两个按钮的理解（已照此实现）：**♥ 掌握 = 不再推**（30/90 天回来考），**收藏 = 重要、每天回来一次**，两者互斥。知识卡的收藏存在 `KnowMark.starDay`，不再进收藏本；伪合集 `KnowledgeRules.StarDeck` / `VocabDeck` 让帳面只刷收藏 / 只刷单词。
- 知識流默认只有知识卡；单词只在「帳面 → 单词」。单词卡重做（5 个一组、先想再看、组末小测）和帳面重排（错题 / 期日の課題 / 苦手 移出帳面）画在画布「知識 · 単語 重构 / 帳面（预览）」页，**等用户确认后再做**。单词卡要显示原作台词，得先从 Supabase 把每个词的出处句 + 音频拉进 asset。

**2026-09-30 · 0.17.0（知識卡重做）**
- 用户的日语文档是一个系列（不定式 → 影山 → 词性 → 补语定语 → 介词），后篇反复回收前篇的点；**读文档必须通读，不能看目录下结论**（我只看目录就说「重复要去重」，被用户指出是承上启下）。
- 知识卡由主会话自己做（用户明确：强模型理解更深，卡更好）；英语只当一行锚点。第一、三、五篇还没做成格式 2 的卡，第三篇只有画布上的 12 张样例（`canvas-gen/gen_forms.py`）。
- **做卡流程（用户认可，固定下来）**：主会话通读原文 → 在 `canvas-gen/decks_data.py` 写卡片数据（一张卡一个点、挑画法、「前回」、英语只当锚点、例句逐条核对）→ 脚本生成 asset → 编译发版 → 用户手机上看、截图反馈。画布只在出现新画法时画一张确认，不逐张预览；便宜模型（Sonnet）只做「照样式加一种新画法」的编码。
- 用户说「等会还有任务」时只 commit，不发版；发版前看一眼 `git diff`，上次被打断的命令其实已经改了版本号。

**2026-09-29 · 0.16.0（爱蜜莉亚语音包）**
- 声线模型全流程在 `../archive-content-sources/emilia-voice/HANDOFF.md`：最终方案 SBV2 + 情绪参考 + Seed-VC 音色转换（单词不转换，会念糊）。**Modal 一天花了约 14.7 美元，用户很在意**：跑任何 GPU 任务前先报预估花费，推理优先用本机 2060。
- 字幕切出来的原声片段常混着前后别人的台词，训练前必须用 Whisper 逐词时间戳裁一遍（`align_cut.py`），这是第一轮模型句尾乱念的根因。
- 让用户听 / 标东西之前，先说要多久、多少就够；标注都做成页面上能点的（A/B 选择 + 问题标签 + 一键复制），别让用户口述。
- 语音包按「App 实际传给 `playTts` 的原文」做 key，所以清单要从 Supabase 拉（单词 `surface`、语法 `ja_example`、台词 `ja_text`），不是从本地 assets 猜。APK 更新是整包下载，大文件（语音包 35MB）别打进 APK，做成一次性导入。

**2026-09-28 · 0.15.0（辞書 语法 / 台词重做 + 罗马音在上）**
- 语法、台词两页照词汇页的手势做（点听、点行拉卡、右滑斩、左滑收藏），代码在 `screens/library/GrammarPage.kt`、`LinesPage.kt`，两张卡共用的零件（`SwipeStage` 左右滑换条目、`CardHeader`、`ColoredNote`、`FormulaChips`、`RevealLine`）在 `DictCards.kt`。语法 / 台词的斩存在 `KnownWords` 里，带前缀键（`文型:` / `台詞:`），不会和词头冲突。
- **罗马音一律在最上面，假名在它下面，然后才是字**（用户要「像多邻国」）。`ReadingLineText`、公式词块、`WordTile` 的 sub、音拍格都已改；新界面不要再放「整句一条罗马音」，一律用 `ReadingLineText` 逐词对齐。
- 用户说辞書是「用来看的」：句型卡不要遮挡 / 挖空，改成打开卡片时句型逐字浮现（`ReadingLineText` 的 `revealed` 参数）；播放音频不重放浮现。
- 笔记标签颜色：讲解 / 语气 / 实际 / 场景 用浅蓝（新令牌 `colors.info` / `infoSoft`），易错用 `bad`。
- `AjlBottomSheet` 的描边不能加在 `ModalBottomSheet` 的 modifier 上（会画在未滑动的位置，出现横穿屏幕的黑框），已改成在弹窗内容里画。
- 画布「辞書 · 语法 / 台词（预览）」页（`JitenGram*`、`JitenLine*`）由 scratchpad 里的生成脚本产出；画布默认打开这一页。

**2026-09-28 · 0.14.0（今日页重排 + 学习卡重做）**
- 今日 = 本日の時間割（固定三节：一限 自習 / 二限 練習 / 三限 復習，`PeriodRules` 在 `screens/today/TodayModels.kt`）+ 学習記録。当前节展开成卡（这课的下一句台词 + 原声/TTS + 唯一黑按钮），数据来自 `LabApp.todayMainLine`。没到期 = `Idle`「无到期」，不盖済。按话课程 / 读空气 / 跟读不再上今日。今日の一句只剩给小组件和朝の一句喂数据。
- 学習記録格子按当天 `seconds` 分 5 档（`StudyHeatmapRules.LevelMinutes` = 1/10/20/40 分钟），用户明确要「颜色深浅 = 学习时长」，取代 0.8.4 的「学完才点亮 + 小点」。
- 学习卡（`StudyCardQuestion`）：标题就是语法点 / 词 / 台词；补充只用带标签的 `StudyFact`（在 `SampleLearningRepository` 里组装），言語学不上卡。新通用组件在 `ui/design/Notes.kt`：`TagChip`、`LabeledNote`、`WordBlocks`、`CoveredLine`（遮る）。声音一律 `VoiceSwitchPill`（原声/TTS 可滑），别再用单按钮。
- 设计教训：用户嫌保守时，我把今日改成「上划一条流 + 底栏 4 个」、学习卡加 SVG 线稿角色，被说「还不如上一版」立刻回退。别为了「大胆」推翻结构；方向提议直接画在画布上给他看，别写长段文字方案。

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

