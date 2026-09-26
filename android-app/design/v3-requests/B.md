# B · 今日与复习 — requests

1. **今日の一句「首次打开」标记要持久化**
   - 页面：TodayScreen（一句逐字浮现，每天只播一次）
   - 建议：`LabUiState.todayLineRevealedOn: String?` + `LabViewModel.markTodayLineRevealed(date)`（写 DataStore）
   - 现状：`TodayLineRevealMemory` 只在进程内记，冷启动当天会再播一次。

2. **今日の一句的说话人与时间码**
   - 页面：TodayScreen
   - 建议：`ShadowingSentence` 带 `speaker`、`startTime`
   - 现状：从同集读空气 sceneLines 按 lineNo 找说话人，从已加载字幕找时间码；都没有时显示「第 N 行」。

3. **苦手なところ的分类**
   - 页面：ReviewScreen
   - 建议：`ProgressItem` 带语法点/知识点标签（如「助词 は・が」）
   - 现状：按 `itemType` 粗分类（词汇 / 语法 / 听力 …），最近 7 天正确率由 `ProgressItem.state` + 更新时间推算。

4. **时間割里课程的预计时长**
   - 页面：TodayScreen 主按钮副标题（画布「约 4 分钟」）
   - 建议：`LessonState.estimatedMinutes`
   - 现状：不显示时长，只显示 `模式 · 已做/总数`。
