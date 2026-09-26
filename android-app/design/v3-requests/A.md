# A · 外壳（登录、设置、搜索、AI 历史）— requests

1. **设置行组件进组件库**
   - 页面：SettingsScreen（将来 AI 历史、更新区也能用）
   - 建议：`ui/design` 加 `SettingsGroup`（日文小标题）、`ToggleRow`、`NavRow`、`DisclosureRow`、`ChoiceRow`、`LineRow`（1px 细线行，min 48dp）
   - 现状：都写成 `screens/settings/SettingsScreen.kt` 里的 private 函数；`AppUpdateSettingsSection` 手写了同样的行。

2. **学生証的真实字段**
   - 页面：SettingsScreen · 学生証
   - 建议：`AuthUser.displayName`、`AuthUser.enrolledAt`（注册日），`LabUiState.attendanceDays: Int`（有学习记录的天数）
   - 现状：氏名取邮箱 @ 前；入学取 `progressItems.lastReviewedAt` 最早一天；出席 = 有 `lastReviewedAt` 的不同日期数；学号取 user.id 后 4 位。头像为网点占位。

3. **讲解历史条数**
   - 页面：SettingsScreen「AI · 讲解历史」行（画布右侧显示「46」）
   - 建议：`LabUiState.aiHistoryCount: Int?`（登录后或进设置时拉一次）
   - 现状：不显示数字，只有 ›。

4. **登录页时钟文案「始業まで N 分」**
   - 页面：LoginScreen 左上角
   - 建议：确定"始業"时间来源（例如用户的每日提醒时间 `LabSettings.reminderTime`）
   - 现状：只显示当前时间 `AM 08:25`。

5. **命令面板的全局词条搜索**
   - 页面：CommandPalette「词汇」分组
   - 建议：`RemoteLabClient.searchEntries(query)` 或本地全作品词表索引；点命中后能定位到辞書的具体词条（`LabViewModel.openLibraryEntry(kind, id)`）
   - 现状：只在当前话已加载的 `vocab` / `grammar` 里做字面匹配，点击切到辞書标签，不定位到词条。画布里的「操作」分组（用这个词出专项练习 / 问 AI）没做：缺按任意查询词出题和问 AI 的入口。
