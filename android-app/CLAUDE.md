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
| 活用道場（第三巻 活用）继续做 P3 | `CONJUGATION_DRILL_HANDOFF.md` |

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
- `ui/LabApp.kt`：底部 4 个标签 Today / Learn / Library / Review、二级页面路由、登录门、命令面板入口。
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
- `ui/study/StudyLog.kt`：全局学习日志（每天答题数、正确数、时长），喂给 Today 的「最近 12 週」格点（`screens/today/StudyHeatmap.kt`）。**新增任何答题型 session，判定对错的地方都要调 `StudyLog.record(...)`**，否则格点和时长不计。
- `platform/LearningSessionNotifier.kt`：学习中的常驻通知（Android 16 ProgressStyle 分段、作品色、角色头像、计时）；内容来自 `ui/LearningSessionStatus.kt`。
- `platform/StudyReminder.kt`：放課後チャイム，每天定时（非精确闹钟）检查 StudyLog，当天没答题才发通知；开机/更新/换时区时重新排程。
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

