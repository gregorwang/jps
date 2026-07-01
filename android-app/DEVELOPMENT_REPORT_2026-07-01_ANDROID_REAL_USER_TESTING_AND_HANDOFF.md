# 2026-07-01 Android App 开发与真实用户测试报告

## 结论先说

这 13 小时主要不是在做一个单点功能，而是在把 Android App 从“能看起来像移动端”继续推向“能按真实用户路径使用”。实际完成的东西集中在四块：

1. 读空气大题库体验继续对齐网页端，补了浏览/训练两种模式、完整筛选维度、队列数量展示、空队列重练入口，并用 Re:Zero EP56 这种非第一集场景完整做题验证。
2. 作品库/资料页继续往原生入口页靠，验证了作品 tab、episode 列表、句子数/chunk 数/字幕数、按 episode 进入不同训练入口。
3. 账号同步页面和真实登录态做了端到端可视验证，确认 App 识别当前账号、读取账号范围进度、支持合并当前设备进度。
4. 底部导航命中区域修了一轮又一轮，最后用模拟器确认 icon 和文字区域都能切换页面。

这轮确实有不少时间消耗在重复安装、启动、点同一个区域、截图确认上。原因不是为了刷时间，而是 Android 底部导航和冷启动在模拟器上出现了“视觉上按到了，但页面不切”的真实问题；如果只看 Compose 代码会误判已经修好，所以反复用模拟器验证了不同 y 坐标、icon 区域、label 区域。

## 本次主要改动

### 1. 读空气：训练模式 / 浏览全部题目模式

相关文件：

- `app/src/main/java/com/animejapaneselab/nativeapp/ui/screens/ReadAirScreen.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/ui/LabViewModel.kt`
- `app/src/test/java/com/animejapaneselab/nativeapp/ui/ReadAirTrainingStateTest.kt`

完成内容：

- 读空气入口页支持 `单题训练` 和 `浏览全部题目` 两种模式。
- 顶部和模式卡片展示当前筛选范围：
  - 全题库数量
  - 当前筛选数量
  - 队列待练数量
  - 已答数量
- 筛选维度继续对齐网页端大题库：
  - 作品
  - 集数
  - 领域
  - 语言现象
  - 题型
  - 难度
- 浏览模式能显示当前筛选结果列表。
- 训练模式只取当前筛选范围内“待练队列”。
- 当前筛选范围题目都做完时，不再只给一个含糊空态，而是显示：
  - `重置队列再练`
  - `清空筛选`
- `清空筛选` 文案补全，避免只显示“清空”导致用户不知道清空什么。

真实模拟器验证：

- 默认读空气全题库显示 `160/160`，队列 `148`，已答 `12`。
- 切到浏览模式后筛选面板展开正常。
- 选择 `Re:Zero` 后浏览结果变为 `132`。
- 切到集数页 `EP49-60`，选择 `EP56`。
- `Re:Zero EP56` 浏览结果为 `2`，已答 `2`。
- 切回单题训练后显示 `队列 0 · 待练 0/2`，并出现 `重置队列再练`。
- 点击 `重置队列再练` 后队列恢复为 `2/2`。
- 进入 EP56 训练，完成两题：
  - 第 1 题：关系确认，Line 1。
  - 第 2 题：省略与沉默，Line 11。
- 完成页显示 `2` 题、`100%`、`+16 XP`。
- 返回读空气入口后，EP56 重新显示待练 `0`、已答 `2`。

### 2. 读空气从错题页/复习页进入的匹配修复

相关文件：

- `app/src/main/java/com/animejapaneselab/nativeapp/ui/LabViewModel.kt`
- `app/src/test/java/com/animejapaneselab/nativeapp/ui/ReadAirTrainingStateTest.kt`

完成内容：

- 复习页进入读空气训练时，不再只依赖脆弱的文本匹配。
- 匹配顺序补强为：
  - remote review task id
  - sourceId
  - work / episode / line number
  - 文本兜底
- 复习页进入的读空气 session 保留来源 tab，完成后能回到错题页。

验证：

- 从复习页进入 K-ON EP01 line 25 的读空气题，能匹配到 cloud ReadAir exercise。
- 完成后按钮显示 `返回错题页`，返回路径正确。

### 3. 作品库 / 资料页继续对齐网页端

相关文件：

- `app/src/main/java/com/animejapaneselab/nativeapp/ui/screens/LibraryScreen.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/ui/screens/LessonScreen.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/ui/LabViewModel.kt`

完成/验证内容：

- 资料页能看到作品 tab：
  - `K-ON!`
  - `Re:Zero`
- Re:Zero 作品概览显示 `66/66` 集。
- episode 选择不是藏到底部，而是以列表/网格入口方式在资料页中出现。
- 选择不同 episode 后，卡片统计会随 episode 更新。

真实模拟器验证过的 episode：

- `Re:Zero EP04`
  - 显示 444 句、11 chunks、516 字幕。
  - 从资料页进入综合训练，训练来源是 EP04。
- `Re:Zero EP05`
  - 从资料页进入读空气专项。
  - 显示 `Re:Zero EP05`，队列为 `3/16`。
- `Re:Zero EP56`
  - 显示 756 句、28 chunks、1244 字幕。
  - 从资料页进入语法专项。
  - Lesson 顶部标题补上 episode label，避免只看到行号不知道当前是哪一集。

### 4. 底部导航点击命中区域

相关文件：

- `app/src/main/java/com/animejapaneselab/nativeapp/ui/LabApp.kt`

问题现象：

- 旧实现里，底部导航看起来能按到 icon，但页面有时不切换。
- label 区域相对更容易切换。
- 这不是单纯视觉问题，是可点击区域和 icon 绘制位置不一致。

处理过程：

1. 先尝试 Material3 `NavigationBarItem`，但模拟器上 icon 区域仍有不稳定点击。
2. 去掉固定高度，交给 Material3 处理底部栏高度，仍然不能彻底解决。
3. 改成自定义底部 tab，每个 tab 外层统一接管 click。
4. 继续发现 icon 内部 indicator/Surface 可能影响事件路径，于是改成纯 `Box + background` 绘制选中胶囊。
5. 最后用外层 `Box` 作为唯一 clickable 区域，icon 和文字只是绘制内容。

最终模拟器验证：

- 从今日页点击 `设置` icon 中心，能进入设置页。
- 从设置页点击 `读空气` icon 中心，能进入读空气页。
- 点击 `设置` 文字区域，也能进入设置页。

### 5. 账号同步和真实账号状态

相关文件：

- `app/src/main/java/com/animejapaneselab/nativeapp/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/ui/LabViewModel.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/data/RemoteLabClient.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/data/LocalLabStore.kt`

完成内容：

- 设置页显示账号同步卡片。
- 登录后能看到当前账号邮箱。
- 显示 deviceId。
- 显示账号范围读取进度。
- 支持：
  - 刷新账号状态
  - 合并当前设备进度
  - 退出登录

真实模拟器验证：

- App 当前处于已登录状态。
- 设置页显示账号邮箱。
- 显示 `账号范围进度 273 条，复习 21 条`。
- 点击刷新后状态文案更新。
- 点击合并当前设备进度后显示账号状态刷新完成。

安全说明：

- 没有把账号密码写入代码。
- 没有把账号密码写入报告。
- 没有把 Supabase service role 或 AI Gateway token 放入 Android。
- Android 仍走 Worker API，Worker 负责服务端密钥。

### 6. 启动体验

相关文件：

- `app/src/main/java/com/animejapaneselab/nativeapp/ComposeHost.kt`
- `app/src/main/java/com/animejapaneselab/nativeapp/SplashActivity.kt`

完成内容：

- 将 ComposeHost 内部启动占位等待从较长等待缩短到 `600ms`。
- 减少冷启动空白/假死体感。

验证现象：

- 正常情况下主界面约 2 秒左右出现。
- 多次重新安装后模拟器会出现一次较长 JIT/首帧卡顿，logcat 中能看到主线程跳帧，但没有崩溃或 ANR。
- 这仍然是后续可优化项，不应算完全解决。

## 构建与测试

执行命令：

```powershell
$env:JAVA_HOME="$env:ProgramFiles\Android\Android Studio\jbr"
$env:PATH="$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

结果：

- `:app:testDebugUnitTest` 通过。
- `:app:assembleDebug` 通过。
- debug APK 生成成功。

APK 本机路径：

```text
C:\Users\汪家俊\jps\android-app\app\build\outputs\apk\debug\app-debug.apk
```

本次另复制一份便于手动测试：

```text
C:\Users\汪家俊\jps\android-app\apk-handoff\AnimeJapaneseLab-debug-2026-07-01.apk
```

## 为什么看起来有些地方在重复做

重复主要发生在底部导航。

原因：

- 第一次看代码，`NavigationBarItem` 理论上应该整块可点。
- 但模拟器真实点击显示：icon 有按压反馈，却不一定触发页面切换。
- 如果只跑单测或看 Compose Preview，会误以为已经修好。
- 所以我反复做了：
  - 构建
  - 安装
  - 启动
  - 点 icon
  - 点 label
  - 换 y 坐标
  - 看页面是否真的切换
  - 再改布局

这部分时间消耗比较大，但它确实抓到了一个真实用户会遇到的问题。

## 当前仍未完成 / 仍需继续

### 1. 真正完整的账号登录流程还要再压测

当前已经验证了已登录态、账号进度读取、合并设备进度。但完整的：

- 退出登录
- 输入邮箱
- 输入密码
- 提交登录
- 登录后自动读取账号进度
- 再退出并回到 deviceId 进度

这一整条链路还需要专门再测一次。原因是这会真实改变当前 App session 和账号状态，最好作为单独验证任务做，并记录每一步截图。

### 2. Duolingo 风格还只是方向，不是系统化完成

已有：

- 绿色主视觉
- XP / 连续天数 / 能量
- 训练入口卡
- 单题训练 session
- 答题反馈 dock

但还需要继续系统化：

- 节点路径更像 Duolingo。
- 锁定态、完成态、当前态更明确。
- 各训练入口统一视觉语言。
- 字体层级和按钮半径进一步统一。

### 3. 作品库还可以继续深化

现在资料页已经能选择作品和 episode，也能进入不同训练入口，但还可以继续：

- 让 episode 列表更像移动端课程目录。
- 增加每集训练完成度。
- 增加每集读空气/词汇/语法/跟读的完成态。
- 对 K-ON 和 Re:Zero 做更一致的入口呈现。

### 4. 冷启动性能还需要专项处理

多次重新安装后，模拟器冷启动偶尔较慢。当前没有崩溃，但体验不够好。后续建议：

- 减少首屏同步初始化。
- 延后不必要的数据加载。
- 给首屏更明确的 loading 状态。
- 用 Android Studio profiler 或 Perfetto 做一次启动专项分析。

## 本次交付内容

- Android 源码。
- 单元测试。
- 截图验证材料。
- 本报告。
- debug APK。

## 建议你手动测试的路径

1. 安装 APK。
2. 打开 App，先看今日页是否进入 Re:Zero EP56。
3. 点底部 `设置` icon 和文字，确认都能进设置页。
4. 在设置页确认账号状态和同步文案。
5. 点底部 `读空气` icon。
6. 切浏览模式。
7. 选择 Re:Zero -> EP49-60 -> EP56。
8. 切回单题训练。
9. 点 `重置队列再练`。
10. 完成 EP56 两道读空气题。
11. 回到读空气入口，看待练/已答数量是否正确。
12. 去资料页，切 Re:Zero EP04、EP05、EP56，分别进入不同训练入口。

## 一句话总结

这轮真正推进的是“真实用户路径可用性”：读空气非第一集筛选和重练、资料页按 episode 进入训练、账号同步状态、底部导航命中区域。还没完成的是完整账号重新登录压测、系统化 Duolingo 视觉回归和启动性能专项。
