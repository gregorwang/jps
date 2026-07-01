# Android App Feature Parity Handoff - 2026-06-30

## 背景

当前目标不是把网页端简单套皮成移动端，而是把网页端已经存在的学习功能迁移成原生 Android 体验，并用 Android Emulator / Computer Use 像真实用户一样操作验证。用户明确指出：之前过度集中在 UI 微调，导致读空气题库筛选、账号同步、模型配置、作品选择等核心功能没有对齐网页端。

本阶段的判断原则：

- 功能完整性优先于单点 UI 微调。
- Android 必须是原生移动交互，不是网页布局缩小版。
- UI 可以参考 Duolingo 的清晰层级、路径感、绿色主视觉和即时反馈，但不能牺牲网页端已有功能。
- 测试时不能只固定测几道题，要覆盖多题库、多筛选、多入口、多状态。

## 本轮已完成

### 1. 今日页作品 / 单集选择前置

已把作品和单集选择从页面底部移到今日页上方，用户进入 App 后能更早确认当前学习作品和 episode。

关键文件：

- `app/src/main/java/com/animejapaneselab/nativeapp/ui/screens/TodayScreen.kt`

关键位置：

- `CourseSelector` 已移动到 `TodayHero` 后面。
- 区块标题改为 `作品 / 单集`。

模拟器验证：

- 今日页首屏能看到绿色学习状态卡。
- 下方能看到作品 / 单集选择卡。
- 能看到作品 chip 和 episode chip。

### 2. 读空气筛选维度扩展

已把读空气筛选从较窄的 episode / 类型筛选扩展到接近网页端维度：

- 作品
- 领域
- 语言现象
- 题型
- 难度
- 集数

同时新增：

- `重置队列`
- `清空筛选`

关键文件：

- `app/src/main/java/com/animejapaneselab/nativeapp/ui/screens/ReadAirScreen.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/ui/LabViewModel.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/ui/LabApp.kt`

关键 ViewModel 方法：

- `selectReadAirWork`
- `selectReadAirPhenomenon`
- `selectReadAirDifficulty`
- `resetReadAirFilters`
- `resetReadAirQueue`

模拟器验证：

- 读空气页可进入筛选区域。
- 已看到 `作品`、`领域`、`语言现象`、`题型`、`难度` 分组。
- 当前本地样例数据下 episode 选项有限，`集数` 是否显示依赖数据量。

### 3. 账号同步基础接入

已补上账号同步 UI 和客户端基础能力：

- 未登录状态展示。
- deviceId 展示。
- 邮箱 / 密码登录入口。
- 刷新状态。
- 登录后合并当前设备进度。
- 退出登录。
- session cookie 本地保存。
- 已登录后 progress / AI 等请求通过带 cookie 的 `RemoteLabClient` 发出。

关键文件：

- `app/src/main/java/com/animejapaneselab/nativeapp/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/ui/LabViewModel.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/data/RemoteLabClient.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/data/LocalLabStore.kt`

关键方法：

- `fetchAuthMe`
- `loginOwner`
- `logoutOwner`
- `claimCurrentDevice`
- `readSessionCookie`
- `writeSessionCookie`
- `clearSessionCookie`
- `refreshAuthState`

模拟器验证：

- 设置页能看到 `账号同步`。
- 能看到未登录状态、deviceId、邮箱输入、密码输入、登录按钮、刷新状态按钮。
- 未提交真实登录，因为没有密码，且提交会触发真实账号请求。

### 4. 模型设置改回真实模型名

已把模型配置从抽象的“快速 / 强大”改回网页端风格的真实模型名。

当前模型名：

- `Gemini 3.1 Flash Lite`
- `Gemini 3.5 Flash`
- `DeepSeek V4 Flash`
- `DeepSeek V4 Pro`
- `Grok 4.3`

当选择 `Grok 4.3` 时显示：

- `Grok 推理强度`
- `低`
- `中`
- `高`

关键文件：

- `app/src/main/java/com/animejapaneselab/nativeapp/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/model/LearningModels.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/data/LocalLabStore.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/data/RemoteLabClient.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/ui/LabViewModel.kt`

模拟器验证：

- 设置页智能讲解区能看到真实模型名。
- 点选 `Grok 4.3` 后能看到推理强度设置。

### 5. 构建验证

已执行：

```powershell
$env:JAVA_HOME="$env:ProgramFiles\Android\Android Studio\jbr"
$env:PATH="$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

结果：

- `BUILD SUCCESSFUL`
- `:app:testDebugUnitTest` 通过
- `:app:assembleDebug` 通过

## 明确未完成事项

### 1. 读空气还没有完全等同网页端

已补齐核心筛选维度，但还没有完整实现网页端的 `浏览全部题目` 体验。下一步需要补：

- 单题训练 / 浏览全部题目模式切换。
- 浏览模式下的大题库列表。
- 当前队列数量更明确展示。
- 清空筛选后恢复全部题库。
- 筛选组合在大量题目数据下的真实验证。
- 不要只测固定几道题，要用不同 work / episode / phenomenon / difficulty 反复验证。

### 2. 账号同步还需要端到端验证

目前完成了 UI 和客户端基础接入，但还没有用真实账号完成完整链路验证。下一步需要：

- 确认 Android 调用的 auth endpoint 与网页端完全一致。
- 使用用户提供的测试账号密码验证登录。
- 验证登录后进度读取是否切到 userId。
- 验证 `合并当前设备进度` 后网页端和 Android 端数据一致。
- 验证退出登录后回到 deviceId 本地状态。
- 验证 session 过期 / 网络失败的提示。

注意：没有用户明确给密码时，不要替用户提交登录。

### 3. 作品库 / 资料页仍需继续对齐网页端

这轮修了今日页作品选择前置，但作品库页本身还需要按网页端继续对齐：

- 作品 tab，例如 `K-ON!` / `Re:ゼロから始める異世界生活`。
- 作品导入集数统计。
- episode 网格 / 列表。
- 每集句子数、chunk 数。
- 点击 episode 后能进入对应词表、语法、跟读、读空气等训练入口。
- 不要把作品选择藏在某个页面底部。

### 4. 底部导航命中区域仍需修

模拟器测试中发现：点击底部导航 icon 通常有效，但点击文字 label 有时不触发切换。下一步需要：

- 扩大每个 bottom nav item 的整体 hit target。
- 确保 icon 和 label 都属于同一个 clickable 区域。
- 用模拟器逐个点击 `今日 / 训练 / 资料 / 读空气 / 错题 / 设置` 验证。

### 5. Duolingo 风格还需要系统化视觉回归

当前已有绿色主视觉、路径感和卡片反馈，但还没有完成系统化 Duolingo 对齐。下一步不要只凭感觉改颜色，需要：

- 对照 Duolingo 的移动端首屏信息密度。
- 对照路径节点、当前关卡、锁定态、完成态、反馈态。
- 统一主按钮、次按钮、chip、卡片、底部导航的半径和阴影。
- 检查字体大小是否适合手机，不要把网页大标题照搬到移动端。
- 用 Android Emulator 截图逐屏检查，不只看 Compose Preview。

### 6. 声音 / TTS 设置还需跟网页端核对

设置页已有声音相关控制，但还需要确认是否完全对应网页端：

- 默认日语人声。
- 播放按钮默认是否使用该人声。
- 答对 / 答错 / 完成训练音效。
- 进入听力或跟读时自动播放。
- Android 端和网页端的配置字段是否一致。

### 7. 自动化测试还不够

目前主要依靠手动模拟器操作和单元测试构建。下一步建议补：

- Compose UI test：底部导航点击。
- Compose UI test：读空气筛选组合。
- ViewModel unit test：ReadAir queue/filter。
- ViewModel unit test：login / logout / claim device 状态流转。

## 当前已知工作区状态

`android-app/` 当前是未跟踪目录。仓库里还存在一些与本轮 Android 工作无关的修改，不要随意 revert。

已看到的非 Android 改动包括：

- `agent.MD`
- `scripts/import-rezero-ep62-subtitles.mjs`
- `scripts/import-rezero-missing-subtitles.mjs`
- `scripts/repair-rezero-sentences-from-subtitles.mjs`
- `src/worker.ts`
- `public/sw.js`
- `supabase/`

处理原则：

- 继续 Android 工作时只改 `android-app/`，除非明确需要对齐后端接口。
- 不要重置或覆盖用户已有改动。

## 下一会话建议优先级

1. 先打开这个文档和 `DEVELOPMENT_SUMMARY_2026-06-30_TRAINING_SESSION_REDESIGN.md`。
2. 用模拟器重新跑一遍真实用户路径，不要只看代码。
3. 优先补读空气的 `浏览全部题目` 模式和大题库筛选验证。
4. 修底部导航 hit target。
5. 再继续作品库 / 资料页与网页端对齐。
6. 最后做系统化 Duolingo 视觉回归。

## 新会话提示词

```text
我们继续开发 C:\Users\汪家俊\jps\android-app 这个原生 Android App。

请先阅读：
1. C:\Users\汪家俊\jps\android-app\DEVELOPMENT_SUMMARY_2026-06-30_ANDROID_FEATURE_PARITY_HANDOFF.md
2. C:\Users\汪家俊\jps\android-app\DEVELOPMENT_SUMMARY_2026-06-30_TRAINING_SESSION_REDESIGN.md

重点：不要只做 UI 微调。当前目标是把网页端已有功能迁移成真正原生 Android 体验，不是网页套皮。需要使用 Android Emulator / Computer Use 像真实用户一样操作 App，边测试边修改。

这次优先继续：
1. 读空气要对齐网页端：单题训练 / 浏览全部题目模式、完整大题库筛选、队列数量、重置队列、清空筛选，并用不同作品、集数、语言现象、题型、难度测试，不能只固定测几道题。
2. 修底部导航点击命中区域，icon 和文字都必须能点。
3. 继续对齐作品库 / 资料页：作品选择不能藏在底部，要有类似网页端的作品 tab、episode 列表、句子数 / chunk 数、进入各训练入口。
4. 账号同步已经有基础 UI 和 client 接入，但还需要端到端验证；没有密码时不要提交真实登录。
5. UI 继续参考 Duolingo，但功能完整性优先。每次改完都要构建并在模拟器里真实操作验证。

构建命令：
$env:JAVA_HOME="$env:ProgramFiles\Android\Android Studio\jbr"
$env:PATH="$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```
