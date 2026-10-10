# C · 学ぶ（課程 / 選番 / 言語学）— requests

1. **話タイトル（ViewModel / data）**
   - 页面：CourseScreen 話列表、当前話面板标题
   - 缺：`EpisodeOption` / `EpisodePlan` 没有集标题。
   - 建议：`EpisodeOption.title`（或 `EpisodePlan.title`）。
   - 现状：`CourseModel.episodeTitle` 内置 けいおん！ 1–14、Re:ゼロ 1–26 的标题表；其余集标题为空（只显示「第N話」）。

2. **场面名的"番味"副标题（data）**
   - 页面：CourseScreen 場面 03 / 04（画布：「听音拼句 · 给镜头配字幕」「跟读 · 憂的台词」）
   - 缺：跟读句没有说话人（同 B.md #2 的 `ShadowingSentence.speaker`）。
   - 现状：显示「听音拼句 · 综合」「跟读 · N 句」。

3. **非当前話的场面进度（ViewModel）**
   - 页面：話列表进度线、出席カード「済」
   - 缺：未选中的话只有 progress 行，没有素材清单。
   - 现状：按 `pathNodeKey` 前缀推导（词汇/语法/听音拼句/跟读 各占 1/4，四个都有才算済）；读空气不计入。
   - 建议：服务端给每话一个 `episode_progress { scenesDone, scenesTotal }`。

4. **AjlBottomSheet 内容可滚动（component）**
   - 页面：CourseSwitcherSheet（Re:ゼロ 一期 25 格出席卡 + 4 行季度，小屏可能超高）
   - 建议：`AjlBottomSheet(scrollable = true)` 或在内部 Column 加 `verticalScroll`。
   - 现状：未加滚动；言語学的筛选面板在自己的内容里加了 `verticalScroll`。

5. **读空气「浏览全部题目」模式的去留（产品）**
   - 旧 ReadAirScreen 首页有 单题训练 / 浏览 切换；v3 言語学页只保留「教科書 → 读空气 · N 问」的训练入口。
   - `ReadAirHomeActions.onModeSelected / onBrowseAnswer / onResetQueue` 目前没有入口。
   - 建议：确认是否还要浏览模式；要的话放进 D2 的 ReadAirSession（或辞書）。

6. **基礎题的作答记录持久化（ViewModel）**
   - 页面：LinguisticsScreen 第二巻 教科書进度线
   - 现状：进度用 `FoundationTrainingState.selectedAnswers`（进程内），冷启动归零。
   - 建议：从 progressItems 回填 `selectedAnswers`，或提供 `answeredQuestionIds`。

## 留给其他包 / 阶段 3 的死代码
- `screens/ReadAirScreen.kt` 里旧的 `ReadAirSessionScreen` 及其私有函数已无人调用（LabApp 用的是 `session.ReadAirSessionScreen`）。按分工 C 没动，由 D2 / 阶段 3 删除。
- `screens/TrainingPathModels.kt`（+ `TrainingPathModelsTest`）只剩测试在用，Hub 已删；阶段 3 可一起删。
- `screens/LessonScreen.kt` 删 Hub 后留下若干未用 import（rive/fusion/Duolingo 组件等），D1 重写题型时一并清理。
