# Anime Japanese Lab Android 项目详解

最后核对：**2026-07-13（Asia/Hong_Kong）**

适用范围：`android-app/` 原生 Android 项目。

目标读者：熟悉网页、React 或后端开发，但第一次维护 Kotlin、Jetpack Compose 和 Android 生命周期的开发者。

这份文档解释四件事：

1. 这个 App 当前到底由哪些部分组成。
2. 一次点击怎样从 UI 流到状态、网络、本地存储和系统能力。
3. 为什么功能数量看起来不多，主 Kotlin 源码却已经达到约 2.69 万行。
4. 哪些复杂度来自 Android 平台，哪些是当前项目自身的架构和历史债务。

环境、构建命令、签名和模拟器状态另见 [`ANDROID_ENVIRONMENT.md`](ANDROID_ENVIRONMENT.md)。未来代理的硬约束另见 [`AGENTS.md`](AGENTS.md)。

## 1. 最重要的产品边界

### 1.1 Android 是独立产品，不是 Web 的移植版

Android 端的行为、页面结构、交互、学习流程和视觉设计，不能以仓库根目录的 Web/PWA 实现为规范，也不能以“Web 对齐”为验收目标。

Android 的产品事实来源按优先级排列如下：

1. 用户对 Android 产品的明确要求。
2. Android 项目内已经确认的产品文档和测试。
3. Android 当前代码中仍然有效、且经过验证的行为。
4. 后端正式接口的请求/响应契约，仅用于数据交换。

后端接口与 Web 前端使用同一服务，不代表两个客户端必须具有相同的 UI、状态机、回退策略或学习流程。需要确认接口时，应读取后端 schema、接口测试、实际响应或后端文档，不应从 Web 页面逻辑反推 Android 产品行为。

### 1.2 仍然有效的 Android 边界

- Android 当前要求登录后才能学习。
- 不实现、迁移或恢复手写/书写练习功能。
- Android 的本机语音、原声音频、录音、触觉、通知、返回手势和生命周期应按 Android 产品需要独立设计。
- 默认只修改 `android-app/`。修改后端、Web、Supabase 或其他根目录项目需要用户明确授权。

## 2. 一句话理解这个项目

主应用是一个单 Activity、Compose-first 的原生 Android 学习应用；`debug` source set 另有一个仅供内部视觉预览的 `FusionPreviewActivity`，不属于正式产品导航。

- `MainActivity` 只负责启动 Compose。
- `LabApp` 负责整个应用外壳、登录门禁、底部导航和当前页面选择。
- 一个全局 `LabViewModel` 当前掌管几乎所有业务状态和动作。
- `SampleLearningRepository` 把远程内容、本地后备内容和课程题型转换成统一的 `LessonNode`。
- `RemoteLabClient` 手写 HTTP 和 JSON 解析。
- `LocalLabStore` 使用 SharedPreferences 保存设置、登录 Cookie、选择、错题和进度。
- Compose 屏幕渲染课程、语言学训练、资料库、复习、设置和字幕等 UI。
- Android 平台层另外处理 TTS、原声音频、麦克风录音、触觉、通知、权限和设备能力。

当前代码的基本数据流是：

```text
用户操作
  -> Composable 回调
  -> LabViewModel 方法
  -> Repository / RemoteLabClient / LocalLabStore / Android 平台能力
  -> MutableStateFlow<LabUiState> 更新
  -> Compose 读取新状态并重组 UI
```

这个方向本身是单向数据流，但“所有功能共享一个 ViewModel 和一个 UiState”让边界逐渐消失了。

## 3. 从 Web 开发切换到 Android 的概念翻译

| Web/React 常见概念 | 当前 Android 对应物 | 关键差异 |
| --- | --- | --- |
| 浏览器启动页面 | `MainActivity` | Activity 可能因旋转、配置变化或系统回收而重建。 |
| 应用进程初始化 | `LabApplication` | 生命周期通常比 Activity 长，但进程仍可能被系统杀死。 |
| `createRoot(...).render()` | `activity.setContent { ... }` | Compose 从这里建立 UI 组合树。 |
| React Function Component | `@Composable fun` | Compose 编译器跟踪状态读取，并决定重组哪些调用点。 |
| Props | Composable 参数 | 推荐传不可变状态和事件回调。 |
| Redux/Zustand 页面 Store | `ViewModel + StateFlow<UiState>` | ViewModel 通常应按屏幕或导航范围存在，而不是天然全局。 |
| `useState` | `remember` / `rememberSaveable` | `remember` 不保证跨 Activity 重建；`rememberSaveable` 可保存可序列化 UI 状态。 |
| `useEffect` | `LaunchedEffect` / `DisposableEffect` | 必须正确选择 key，并在离开组合时清理资源。 |
| CSS class / style | `Modifier`、Theme、参数 | Modifier 有严格顺序；`padding().clickable()` 与 `clickable().padding()` 语义可能不同。 |
| React Router | 当前是 `LabUiState` + `when` 手工路由 | 目前没有正式 NavHost/back stack，页面关系由状态字段表达。 |
| `fetch` / Axios | `RemoteLabClient` + `HttpURLConnection` | 当前项目自己处理线程、超时、Cookie、状态码、断开连接和 JSON。 |
| localStorage | SharedPreferences | Android 还要考虑备份、进程重启、敏感信息和同步提交。 |
| 静态资源目录 | `res/drawable`、`res/raw`、`res/values` | 资源在构建期生成 `R` ID，并参与密度、语言和资源裁剪。 |
| `.env` + bundler config | Gradle build type、BuildConfig、环境变量 | Release 还涉及签名、R8、资源收缩和 APK/AAB。 |
| 浏览器权限 | Manifest + 运行时权限 | 麦克风、通知等既要声明，也可能要在运行时请求。 |

### 3.1 Compose 的“重组”不是重新加载页面

当 Composable 读取的 State 变化时，Compose 会重新执行相关 Composable 调用，计算新的 UI 描述。它不等于浏览器刷新，也不等于把整个 Activity 重新创建。

因此有几个重要规则：

- Composable 函数体应尽量是纯 UI 描述。
- 网络请求、写存储、启动录音、播放器初始化不能直接散落在组合过程里。
- 业务状态应由 ViewModel 或独立状态持有者负责。
- 滚动、焦点、动画进度等纯 UI 状态可以留在 UI。
- 传入一个过大的全局 `LabUiState` 会让依赖范围和修改影响范围很难判断。

### 3.2 Android 有多层“活多久”的状态

```text
一次重组
  < 当前 Composable 留在组合中的时间
  < 当前 Activity 实例
  < ViewModel 所属导航/Activity 范围
  < 应用进程
  < 磁盘持久化数据
```

网页里经常只需要区分组件状态、全局 Store 和服务端数据；Android 还必须考虑 Activity 重建、后台、进程死亡、权限回调、播放器释放和硬件资源独占。

## 4. 应用怎样启动

启动链路很短：

```text
Android Launcher
  -> AndroidManifest.xml
  -> LabApplication.onCreate()
  -> MainActivity.onCreate()
  -> ComposeHost.install(activity)
  -> AnimeJapaneseLabTheme
  -> LabApp
  -> LabAppContent + LabViewModel
```

### `AndroidManifest.xml`

声明：

- App 的 Application 和启动 Activity。
- 网络、录音、震动、通知权限。
- 学习通知被用户清除时使用的 BroadcastReceiver。
- 备份关闭、网络安全配置、主题和预测返回支持。

### `LabApplication.kt`

进程启动时清理无法恢复的旧学习通知。它没有加载课程或动画运行时，目的是保持冷启动轻量。

### `MainActivity.kt`

安装 Android SplashScreen、启用 edge-to-edge、调整导航栏对比度，然后交给 `ComposeHost`。正式产品是单 Activity 架构，没有 Fragment 页面树；debug 预览 Activity 是开发工具入口。

### `ComposeHost.kt`

相当于 UI 根挂载点：安装主题、Material Surface 和 `LabApp()`。

## 5. 当前页面和路由模型

用户登录后，`LabApp.kt` 根据三个字段选择页面：

```text
secondaryScreen != null  -> 设置 / 字幕 / 智能复习队列
activeSession != null    -> 正在进行课程 / ReadAir 训练
否则                      -> 当前底部 Tab
```

### 5.1 底部 Tab

| Tab | 入口 Composable | 当前职责 |
| --- | --- | --- |
| 今日 | `TodayScreen` | 今日学习概览、继续课程、进入 ReadAir/复习/字幕。 |
| 课程 | `LessonHubScreen` | 课程路径、作品/集数、题型实验室和课程开始入口。 |
| 语言学 | `ReadAirScreen` | 语言学题库、筛选、浏览和训练入口。 |
| 资料库 | `LibraryScreen` | 作品/集数内容、词汇/语法/句子、定向学习和 AI 解释入口。 |
| 复习 | `ReviewScreen` | 本地错题、远程复习任务、智能复习队列。 |

### 5.2 覆盖底部 Tab 的页面

- 未登录时：`LoginGateScreen`。
- `SecondaryScreen.Settings`：设置页。
- `SecondaryScreen.Subtitles`：字幕浏览页。
- `SecondaryScreen.SmartReviewQueue`：智能复习队列。
- `TrainingSessionKind.Lesson`：正式课程会话。
- `TrainingSessionKind.ReadAir`：语言学训练会话。

### 5.3 当前导航的特点

当前没有 Navigation Compose/NavHost。页面历史不是一个独立 back stack，而是通过 `selectedTab`、`secondaryScreen` 和 `activeSession` 三个状态字段组合出来。

优点是早期实现直接；缺点是：

- 页面作用域与全局状态作用域绑定。
- 返回行为需要手工判断当前是哪类页面。
- 深链、状态恢复、页面级 ViewModel 和导航测试不自然。
- 新增页面经常意味着继续扩展 `LabUiState`、`LabViewModel` 和 `LabApp.when`。

## 6. 当前全局状态：`LabViewModel` 和 `LabUiState`

`LabViewModel.kt` 是当前项目的控制中心。它现在同时负责：

- 登录、退出、账号状态和设备认领。
- 当前作品、集数和上次选择。
- 远程课程内容刷新。
- 课程开始、答题、继续、重开和批次切换。
- ReadAir 题库、八类筛选、浏览答案和训练队列。
- 发音录制后的上传、评分、重试和错误恢复。
- 本地错题、复习任务和智能复习计划。
- AI 提问和不同入口的上下文组装。
- 本地优先进度写入、云同步和失败重试队列。
- 设置、设备能力和通知相关状态。

`LabUiState` 则把以上所有页面所需数据放进一个状态对象，包括：

- 导航与会话状态。
- 作品、集数、词汇、语法、句子、字幕和场景。
- 当前课程 Session、模式、批次、目标和 XP。
- ReadAir 完整训练状态。
- 错题、进度、复习计划。
- Auth、Sync、AI、发音评分和设备能力。

这不是“Kotlin 必须这样写”，而是早期为了快速贯通功能采用的全局状态方案。随着功能增加，任何一个页面动作都可能读取或复制整个 `LabUiState`，因此局部修改容易影响其他流程。

## 7. 数据层、领域层和平台层

### 7.1 `data/`

#### `LearningModels.kt`

集中定义内容、设置、同步和学习节点模型。正式课程目前有六种主要交互节点：

1. `StudyCardNode`：学习卡。
2. `PairMatchNode`：配对。
3. `SingleChoiceNode`：单选。
4. `ClozeNode`：语法填空。
5. `TileOrderNode`：拼句/听音排序。
6. `ShadowingNode`：跟读与发音评估。

一个用户眼中的“课程功能”，在代码里实际是六种交互机制、各自的渲染、答案状态、反馈、音频和进度规则。

#### `SampleLearningRepository.kt`

名字虽然是 Sample，但当前职责很重：

- 提供本地作品和后备内容。
- 把远程 vocab、grammar、sentence、exercise 组合成课程。
- 为不同 LessonMode 和 LessonExerciseKind 生成节点。
- 生成干扰项、配对、拼句 tile 和语法填空。
- 根据进度调整练习优先级。
- 处理批次、目标训练和课程平衡。
- 包含 K-ON、Re:Zero 等本地后备场景。

它实际上同时承担内容 Repository、课程组装器、题目生成器和 fallback catalog，因此达到约 1,814 行。

#### `RemoteLabClient.kt`

手写完成：

- 登录 Cookie 请求。
- 内容、题目、字幕、复习和进度接口。
- AI 请求。
- 发音 ticket、multipart WAV 上传和评分解析。
- camelCase/legacy 字段兼容和 JSON 映射。
- 超时、HTTP 状态、错误响应和断开连接。

因为没有使用 Retrofit/Ktor Client、序列化 DTO 框架或独立 data source，每一个接口都需要写较多传输和解析代码。

#### `LocalLabStore.kt`

使用一个 SharedPreferences 文件保存：

- device ID。
- 设置。
- session Cookie。
- 当前作品/集数和每个作品上次集数。
- 错题。
- 已提交进度。
- 等待上传的进度。

复杂对象被手工编码/解码为 JSON。备份被关闭，避免登录 Cookie 跟随系统备份迁移。

### 7.2 `domain/`

这是当前最小、也相对纯净的一层：

- `LessonEngine.kt`：课程 Session、答题判定、反馈后继续、重开。
- `LessonProgress.kt`：根据已有进度恢复课程位置。
- `SmartReviewPlanner.kt`：把错题和远程进度转为复习优先级。

这些文件主要是纯 Kotlin，不依赖 Compose UI，因而容易进行 JVM 单元测试。

### 7.3 `platform/`

- `DeviceCapabilities.kt`：Android 版本、刷新率、触觉和特定设备能力检查。
- `LearningSessionNotifier.kt`：通知渠道、Android 16 ProgressStyle、ongoing/promoted 状态和清除通知处理。

这部分是 Android 真正特有的复杂度，在网页项目里通常没有直接对应实现。

## 8. 一次课程怎样运行

课程主链路大致如下：

```text
选择作品/集数/模式
  -> LabViewModel 请求或读取内容
  -> SampleLearningRepository 生成 List<LessonNode>
  -> LessonEngine.start/resume 得到 LessonSession
  -> LessonScreen 根据具体 LessonNode 类型选择渲染器
  -> 用户提交答案
  -> LessonEngine.answer 生成 AnswerFeedback
  -> UI 播放视觉/声音/触觉反馈
  -> 本地写入错题与进度
  -> 开启云同步时尝试上传
  -> 上传失败则保留 pending progress
  -> 继续到下一节点或完成页
```

本地进度先提交、云端后同步，是当前重要的数据安全语义：网络失败不能让正确答题在本机消失。

### 8.1 音频

`LessonAudioController` 同时管理：

- 原声 URL 的异步 MediaPlayer 播放。
- Android 本机日语 TextToSpeech 初始化、超时和回调。
- 配置的远程 TTS Worker。
- TTS 文件缓存和临时文件原子替换。
- 播放 Loading/Playing/Error 状态。
- 原声失败后的显式再次点击回退。
- 离开组合时释放播放器、TTS 和协程。

这些不是一行 `<audio>` 可以完整替代的 Android 资源生命周期。

### 8.2 发音评估

`PronunciationWavRecorder` 直接使用 `AudioRecord`：

- 16 kHz、单声道、16-bit PCM。
- 独立采集线程。
- 最短/最长录音时间。
- PCM 转标准 WAV header。
- 停止、取消、线程 join 和硬件释放。

随后 `RemoteLabClient` 申请短期 ticket，将 WAV 作为 multipart 上传，并把评分结果映射为发音 UI 状态。

### 8.3 反馈和视觉系统

当前项目不仅有静态 Compose UI，还同时包含：

- 正误反馈音效。
- Android 振动与 HLA 触觉 pattern。
- Compose 动画和统一 MotionTokens。
- Rive 状态机绑定。
- Lottie 播放。
- Fusion 视觉资源解析、许可状态和 fallback。
- 完成页、角色插画、课程路径和 XP 动画。

用户可能把这些都看成“课程页面”，但实现上它们已经是数个独立子系统。

## 9. 为什么会有 26,864 行 Kotlin

以下数字是 2026-07-13 对 `app/src/main/java/**/*.kt` 的静态统计，不含测试、XML 和资源文件。

### 9.1 按区域拆账

| 区域 | 行数 | 占比 | 主要内容 |
| --- | ---: | ---: | --- |
| App Shell + `ui/screens` | 16,662 | 62.0% | 全局状态/路由和全部主要页面。 |
| components/completion/feedback/fusion/motion/rive/theme | 4,873 | 18.1% | 组件、完成页、视觉、动画、Rive/Lottie、声音触觉映射。 |
| `data/` | 4,055 | 15.1% | 模型、手写网络/JSON、本地存储、课程生成和 fallback 内容。 |
| `ui/audio` | 648 | 2.4% | 原声、TTS、反馈音和 WAV 录音。 |
| `domain/` + `platform/` + 启动文件 | 626 | 2.3% | 课程引擎、复习计划、通知、设备能力和启动。 |
| 合计 | 26,864 | 100% | 69 个主 Kotlin 文件。 |

结论：体积主要不是 Manifest、Gradle 或 Android 模板代码造成的，**22,183 行（82.6%）都是 UI 及其视觉/反馈系统**。

### 9.2 最大的五个文件

| 文件 | 行数 | 占全部主代码 | 为什么大 |
| --- | ---: | ---: | --- |
| `LessonScreen.kt` | 5,496 | 20.5% | 课程首页、课程切换、目录、路径、六类题型、录音评分、反馈和完成入口全部在一起。 |
| `LabViewModel.kt` | 3,228 | 12.0% | 所有页面状态、登录、网络、课程、复习、AI、发音和同步总控。 |
| `ReadAirScreen.kt` | 2,262 | 8.4% | 首页、筛选 Sheet、路径、训练、浏览、答题反馈和完成。 |
| `SampleLearningRepository.kt` | 1,814 | 6.8% | 内容 fallback、课程生成、题型转换、干扰项和优先级。 |
| `LibraryScreen.kt` | 1,444 | 5.4% | 作品/集数选择、内容概览、词汇语法句子和多个动作区。 |

这五个文件合计 14,244 行，占主 Kotlin 代码约 53%。这说明问题不是“项目有 69 个文件所以自然很大”，而是少数文件持续吸收历次需求。

### 9.3 “功能不多”为什么仍然会膨胀

#### 一个功能名下面有很多状态组合

以“课程”为例，它不是一个页面，而是：

- 课程 Hub。
- 作品/集数目录与切换器。
- 学习路径与节点状态。
- 六种题型。
- 每种题型的未答、已选、正确、错误、反馈和继续状态。
- 原声、TTS、播放失败和手动回退。
- 录音权限、录音中、评分中、成功、失败和重试。
- 动画开启/关闭、内部资源允许/禁用。
- 中途退出、进度通知和完成结算。

产品列表里写一项“课程”，代码却是大量状态的乘积。

#### Compose 把布局和视觉明确写在 Kotlin 中

React 项目通常把 JSX、CSS、图标库和浏览器默认行为分散在不同文件或依赖中。当前 Compose 项目大量使用显式 `Row/Column/Box`、Modifier、颜色、尺寸、动画和语义，因此一张视觉复杂的卡片经常需要几十行 Kotlin。

但是，这只能解释部分差异，不能解释 5,496 行的单文件。

#### 历次设计方案没有完全退出代码库

静态审计发现：

- `LessonScreen.kt`、`ReadAirScreen.kt`、`LibraryScreen.kt`、`ReviewScreen.kt` 中共有 32 个顶层私有函数，名称只在自己的声明处出现。
- 按相邻顶层声明粗略估算，这些候选代码约覆盖 1,496 行。
- `MineScreen.kt` 有 229 行，但当前应用路由没有引用 `MineScreen`。
- 项目同时存在 Duolingo-like、Fusion、Rive、Lottie、纯 Compose fallback 和 legacy completion 等多套视觉路径。

这是一份清理候选清单，不应在没有构建和 UI 验证的情况下整批删除；但它足以说明代码量中确实包含改版残留，而不是全部对应当前可见功能。

#### 手写基础设施增加了重复代码

当前没有依赖注入框架、类型化 HTTP Client、JSON 序列化框架、Room 或 DataStore。轻量项目这样做可以减少依赖，但随着接口和状态增多，Cookie、JSON、错误映射、SharedPreferences 编解码和对象创建都由项目自己维护，代码会明显变长。

### 9.4 当前规模并不是 Android 的必然规模

同样的功能用 Kotlin/Compose 可以比现在更小、更容易理解。当前体积由四类因素共同造成：

1. Android 音频、录音、权限、通知和生命周期的真实平台复杂度。
2. 高视觉密度和多套动画/反馈系统。
3. 全局 ViewModel、全局 UiState 和巨型屏幕导致职责无法自然分文件。
4. AI 多轮改版保留旧方案、局部追加新实现而缺少持续删除和架构整理。

第 1 类不能凭空消失；第 2 类是产品取舍；第 3、4 类是当前最应该治理的部分。

## 10. 构建、资源和发布

### 10.1 主要工具链

- Kotlin 2.0.21。
- Jetpack Compose + Material 3。
- Gradle 8.14.5、Android Gradle Plugin 8.13.2。
- JVM 17 bytecode，由 Android Studio JBR 21 构建。
- `minSdk 26`、`targetSdk 36`、`compileSdk 36.1`。
- Rive 11.7.1、Lottie 6.7.1。

### 10.2 Build variants

| Variant | 用途 |
| --- | --- |
| `debug` | 开发、预览和模拟器验证，允许内部参考资源。 |
| `localSlim` | 个人安装版，经过 R8 和资源收缩，使用 debug 签名，允许内部资源。 |
| `release` | 对外发布版；要求正式签名和资源许可开关，否则主动失败。 |

### 10.3 为什么 Android 构建比网页多几个概念

Android 产物不是服务器上的 JS bundle，而是安装到设备的签名 APK/AAB。构建还必须处理：

- SDK/API 兼容。
- DEX 字节码。
- R8 混淆、优化和 keep rule。
- 多 ABI 原生库。
- 资源编译与收缩。
- Manifest 合并。
- 签名证书。
- 安装和系统权限。

## 11. 测试现在能证明什么

当前有 23 个 JVM `*Test.kt` 文件，约 3,602 行测试代码。2026-07-13 的环境记录是 144 个测试全部通过；源码继续变化后仍必须重跑。

已有测试主要覆盖：

- 课程引擎和课程生成。
- 远程 JSON 解析。
- 音频 URL/发音模型/WAV 编码。
- 进度合并和智能复习。
- ReadAir 筛选状态。
- 视觉资源 catalog、反馈映射和 motion token。
- 设备能力的纯函数部分。

当前没有 `app/src/androidTest`，因此 JVM 测试不能证明：

- 实际页面能否正确点击和返回。
- Activity 重建后的状态。
- Compose 语义、布局和可访问性。
- 真机录音权限和麦克风资源释放。
- MediaPlayer/TTS 在设备上的完整生命周期。
- 通知权限、通知样式和系统设置跳转。
- 多页面组合后的用户主流程。

“单元测试全绿但 App 仍有很多问题”在当前测试结构下完全可能发生。

## 12. 当前架构的核心问题

### 12.1 状态作用域过大

登录、课程、语言学、资料库、复习、设置和设备能力共享一个 `LabUiState`。页面无法只依赖自己的小状态合同。

### 12.2 ViewModel 同时是 Controller、Use Case、Coordinator 和 Service Locator

`LabViewModel` 直接创建 Repository 和 Store，并决定远程请求、本地持久化、课程流程、同步和 UI 消息。它很难被独立构造，也很难只测试一个页面状态。

### 12.3 UI 状态持有与 UI 渲染没有充分分开

理想的 Compose 屏幕应有一层小型 Route/State-holder 负责收集 ViewModel 状态，再把专用 `ScreenUiState` 和 callbacks 交给纯 UI Composable。当前屏幕虽然大多接收回调，但普遍直接接收完整 `LabUiState`。

### 12.4 文件按“所有屏幕”和技术类型组织，而不是完整 feature

课程相关代码分散在 `data`、`domain`、`ui/screens`、`ui/audio`、`ui/completion`、`ui/feedback`、`ui/fusion`、`ui/motion` 和 `ui/rive`；同时 `LessonScreen.kt` 又装入多个实际页面。修改课程时很难看到完整边界。

### 12.5 缺少 UI 回归护栏

视觉改版主要依赖人工截图和模拟器观察，旧 Composable 很容易留在文件中，新代码也缺少页面级自动化测试证明旧入口已经退出。

## 13. 建议的目标结构：不换语言、不推倒重写

这不是立即执行的迁移清单，而是理解未来边界的参考结构：

```text
app/
  App.kt                    只负责应用根、全局主题和导航
  navigation/               明确 destination/back stack

feature/
  auth/
    AuthRoute.kt
    AuthScreen.kt
    AuthViewModel.kt
    AuthUiState.kt
  lesson/
    LessonHub...
    LessonSession...
    node/study/
    node/pairmatch/
    node/choice/
    node/cloze/
    node/tileorder/
    node/shadowing/
  linguistics/
  library/
  review/
  settings/

domain/
  lesson/
  progress/
  review/

data/
  auth/
  catalog/
  progress/
  pronunciation/

platform/
  audio/
  recording/
  notification/
  haptics/
```

核心原则：

- 一个页面只观察自己的 `UiState`。
- 一个 ViewModel 只协调一个屏幕或清晰导航范围。
- UI Composable 只接收状态和 callback，不接收 Repository、Client 或全局 Controller。
- 课程节点各自拥有渲染器和测试。
- Android 平台能力通过小接口进入业务层。
- 每次迁移一个垂直切片，保持 App 始终可构建、可安装和可回退。
- 旧实现只有在新实现通过验证后才删除，但不能无限期并存。

## 14. 第一次修改这个项目时怎么定位代码

### 要改启动、主题、底部导航

- `MainActivity.kt`
- `ComposeHost.kt`
- `ui/LabApp.kt`
- `ui/theme/Theme.kt`

### 要改课程结构或答题规则

- 模型：`data/LearningModels.kt`
- 节点生成：`data/SampleLearningRepository.kt`
- Session 状态机：`domain/LessonEngine.kt`
- 进度恢复：`domain/LessonProgress.kt`
- 总控动作：`ui/LabViewModel.kt`
- UI：`ui/screens/LessonScreen.kt`

### 要改 ReadAir/语言学训练

- 模型：`data/LearningModels.kt`
- 解析：`data/RemoteLabClient.kt`
- 状态筛选：`ui/LabViewModel.kt` 中的 `ReadAirTrainingState`
- UI：`ui/screens/ReadAirScreen.kt`

### 要改音频或发音

- 音频选择模型：`data/LearningModels.kt` 的 `PromptAudio`
- 原声规则：`data/ReZeroShadowingAudio.kt`
- 播放/TTS：`ui/audio/AudioControllers.kt`
- 录音/WAV：`ui/audio/PronunciationWavRecorder.kt`
- 远程评分：`data/RemoteLabClient.kt`
- 发音 UI：当前仍在 `ui/screens/LessonScreen.kt`

### 要改进度、错题和复习

- 持久化：`data/LocalLabStore.kt`
- 进度模型：`data/LearningModels.kt`
- 智能排序：`domain/SmartReviewPlanner.kt`
- 同步协调：`ui/LabViewModel.kt`
- 页面：`ui/screens/ReviewScreen.kt`、`SmartReviewQueueScreen.kt`

### 要改动画、插画、声音或触觉反馈

- `ui/feedback/`
- `ui/fusion/`
- `ui/motion/`
- `ui/rive/`
- `ui/components/CourseCharacterArtwork.kt`

先判断需求属于业务状态、纯 UI、平台能力还是资源映射，再决定文件；不要默认继续往 `LessonScreen.kt` 或 `LabViewModel.kt` 追加。

## 15. 修改和验证的推荐顺序

1. 阅读 `AGENTS.md`、`ANDROID_ENVIRONMENT.md` 和本文档。
2. 确认需求是 Android 自身需求，不使用 Web 页面作为规范。
3. 写清楚用户动作、状态变化、失败状态和持久化语义。
4. 找到最小垂直切片，不在无关大文件中顺手改版。
5. 优先补纯 Kotlin 状态/规则测试。
6. 涉及 UI 时补 Compose UI 测试；涉及系统能力时再做设备验证。
7. 运行 `testDebugUnitTest`，必要时运行 `lintDebug` 和 `assembleLocalSlim`。
8. 最后在模拟器/真机验证权限、返回、音频、录音和通知。
9. 明确删除被替代的旧入口，避免又留下一套不可达 UI。

## 16. 对当前项目规模的最终判断

Android 确实比普通网页多出生命周期、系统权限、硬件、签名和设备兼容等概念；音频、录音、通知和触觉也是真实工作量。

但当前 2.69 万行并不能简单归因于“Android 就这么复杂”。更准确的判断是：

- 约五分之一代码是课程单文件。
- 约三分之一代码集中在 `LessonScreen`、`LabViewModel` 和 `ReadAirScreen` 三个文件。
- 82.6% 是 UI 及视觉反馈，而不是平台模板。
- 存在至少一批明确的未接入或无显式调用 UI 候选。
- 同一个全局状态和总控类吸收了过多功能。
- 多轮 AI 迭代更擅长“追加一个可见方案”，没有持续完成“替换后删除旧方案”和“重新建立边界”。

因此，正确方向不是改成 Java、Flutter 或 React Native，也不是继续接受当前复杂度；正确方向是保留 Kotlin/Compose 和已经工作的原生能力，逐步把状态、页面、题型和平台服务拆回可理解、可测试的边界。
