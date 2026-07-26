# Android 产品优化与扩展路线图

生成日期:**2026-07-26**
适用范围:`android-app/` 原生 Android 项目
数据来源:Supabase 项目 `jps`(ref `qoatvdvbuleamyzsaldp`,PG17,ap-southeast-1)实测 + Worker `src/worker.ts` 路由审计 + Android 传输层(`RemoteLabClient.kt`/`LearningModels.kt`)对照

## 0. 这份文档怎么用

- 每一条机会都是独立可执行的最小单元,按「价值 → 数据/接口 → Android 落点 → 步骤 → 测试 → 工作量」写。
- 标签含义:
  - **[仅Android]** 后端接口/数据已就绪,只改 `android-app/`,当前会话即可授权执行。
  - **[需后端]** 需要改 Worker 或数据库,按 AGENTS.md 需用户明确授权后才能动 `android-app/` 以外的代码。
  - **[需决策]** 涉及产品边界(如手写禁区的邻近功能),先问用户再做。
- 硬约束不变(见 `AGENTS.md` / `ANDROID_ENVIRONMENT.md`):不做手写/书写练习;Web 前端不是 Android 规范;默认只写 `android-app/`;保留脏工作区。
- 每完成一步跑:`.\gradlew.bat testDebugUnitTest --no-daemon --console=plain --max-workers=2`,UI 改动另跑 `lintDebug`、`assembleLocalSlim`。
- **安全纪律:Supabase access token 只存在于会话/环境变量中,绝不写进源码、Gradle、文档或提交历史。** Android 客户端只与 Worker 通信,不直连 Supabase。

## 1. 数据资产盘点(实测 2026-07-26)

### 1.1 内容规模

| 资产 | 数量 | 说明 |
| --- | ---: | --- |
| 作品 | 2 | K-ON!(14 集)、Re:Zero(13 集) |
| 字幕行 `subtitle_lines` | 38,002 | 日中双语 + 逐行时间戳(start/end_time) |
| 字幕分块 `subtitle_chunks` | 1,030 | 按场景分块,带时间范围与行范围 |
| 句子 `learning_sentences` | 8,560 | 带 kana reading、romaji、中文释义、**tone_tags**(礼貌体/日常体/语气助词)、**difficulty(N5-N1)**、**recommended_shadowing**、audio_url |
| ├ Re:Zero 句子 | 8,148 | **4,290 句带原声音频**,6,552 句推荐跟读 |
| └ K-ON 句子 | 412 | 无原声音频(0),全部推荐跟读 |
| 词汇 `learning_vocab_items` | 3,770 | **JLPT 全分布:N5=387 / N4=584 / N3=683 / N2=1238 / N1=864**,含动漫语气注记 + 现实用法注记 |
| 词汇出现足迹 `learning_vocab_occurrences` | 7,765 | 词 → 集数 → 出现次数 + 例句行号(example_line_nos) |
| 语法点 `learning_grammar_points` | 2,966 | pattern/功能/例句/讲解/**语用注记 pragmatics_note**/现实注记/难度/**来源行号** |
| 常规练习 `learning_exercises` | 14,427 | 10 种题型:sentence_understanding=5351、vocab_meaning=3448、vocab_reading=3391、grammar_meaning=2049、grammar_short_answer=70、kana_to_kanji=53、meaning_to_japanese=30、reading_air_tone=25、sentence_meaning=5、meaning_to_vocab=5 |
| 语言学题(已发布)`linguistic_exercise_drafts` | 494 | 6 领域:syntax=195、pragmatics=147、historical=68、morphology=54、phonology=28、sociolinguistics=2,全部 status=published |
| 语言现象库 `linguistic_phenomena` | 426 | 现象名(中/日)、定义、入门/深度讲解、注意事项、例句模式、tags(部分字段待补全,如 name_ja/beginner_explanation 有空值) |
| 字幕行现象标注 `subtitle_line_phenomena` | 385 | 行号 + 现象 + 置信度 + 证据说明,**全部 status=draft** |
| **AI 增强卡 `learning_card_enrichments`** | **1,585(全部 ready)** | vocab=613、grammar=536、sentence=436;payload 含 headlineZh/structureZh/**commonMistakes[]**/coreFunctionZh/realWorldUseZh/microQuizHintZh/**exampleBreakdown(逐词拆解)**;另有 linguistic_payload(nerd-lite 语言学术语加餐) |
| 每集学习计划 `episode_learning_plans` | 235 | 词汇/跟读/语法/练习 ID 清单 + plan_slot + notes(如「第1集学习计划:高频词20、手写10、跟读5、语法5」) |
| 角色语言画像 `character_language_profiles` | 4 | 例:スバル画像 = 口癖/句末倾向/礼貌度/情绪表达(中文 markdown) |
| 学习进度 `user_progress` | 320 | **服务端 SRS 字段:state/ease/review_count/next_review_on** + payload(题面/所选/答案/domain) |
| AI 结果缓存 `ai_result_cache` | 149 | kinds:furigana=85、rag_air=22、explain:vocab=18、explain:sentence=6、sentence_deep_dive=6、sentence_correction=4、explain:grammar=4、explain:linguistic=4 |
| AI 交互历史 `ai_interaction_history` | 66 | device/user + cache_key + result_payload |
| 句子批改历史 `sentence_correction_history` | 4 | 打字造句 → AI 批改结果 |
| 手写练习 `writing_practice_*` | 23+3 | **Android 禁区,不接**(笔画/覆盖率指标 = canvas 手写) |

### 1.2 数据卫生检查项(供内容管线参考,不阻塞 Android)

- `episodes` 表 80 行 vs works.episode_count 合计 27 —— 疑似存在按来源文件/语言的重复行,建议核对 `usable_as_main_corpus` 的消费方式。
- `learning_sentences.romaji` 无分词(如 `koredayone`),直接展示可读性差;kana `reading` 大多与原文相同(缺少汉字注音价值)→ 汉字注音应走 furigana 接口(见 P0-1)。
- `linguistic_phenomena` 的 name_ja / beginner_explanation_zh / example_patterns 存在空值(chatgpt-seed 批次),做「现象图鉴」前需要一轮内容补全。
- `subtitle_line_phenomena` 385 条全部 draft,消费前需要审核发布流。

## 2. 后端接口面 vs Android 使用现状

Worker(`src/worker.ts`)全部业务路由与 Android(`RemoteLabClient.kt`)对照:

| Worker 端点 | Android | 说明 |
| --- | --- | --- |
| POST /api/auth/login, /logout, GET /me, POST /claim-device | ✅ 已用 | |
| POST /api/auth/change-password | ❌ 未用 | 设置页可补「修改密码」 |
| POST /api/auth/register-owner | ➖ 不需要 | 单用户产品,注册走 Web |
| GET /api/works, /works/{slug}/episodes | ✅ 已用 | |
| GET /works/{slug}/episodes/{ep}/vocab, /grammar, /sentences, /exercises, /plan, /subtitles | ✅ 已用 | **响应已含 linguisticPayload(见 §3.7),Android 未解析** |
| GET /works/{slug}/episodes/{ep}(单集详情) | ❌ 未用 | 低价值,忽略 |
| GET /api/linguistic-exercises | ✅ 已用 | |
| GET/POST /api/progress, GET /api/review/today | ✅ 已用 | **ease/review_count 字段被 Android 丢弃**(见 §4.5) |
| POST /api/ai/explain | ✅ 已用 | |
| **POST /api/ai/furigana, /furigana/batch** | ❌ 未用 | 汉字→假名注音,已有 85 条缓存 |
| **POST /api/ai/sentence-deep-dive** | ❌ 未用 | 句子深挖精读 |
| **POST /api/ai/character-profile** | ❌ 未用 | 角色语言画像(DB 已有 4 份成品) |
| **POST /api/ai/correct-sentence** | ❌ 未用 | 打字造句批改 |
| **POST /api/rag/search** | ❌ 未用 | Vectorize 全字幕语义搜索 |
| GET /api/history, /api/history/detail | ❌ 未用 | AI 精讲历史回看 |
| GET /api/config, /api/ai/models | ❌ 未用 | 模型列表可用于设置页下拉 |
| POST /api/pronunciation/ticket + 发音 API /v1/pronunciation/evaluate | ✅ 已用 | |
| POST /api/rag/suggest-training-query, /generate-question(s), /save-question(s) | ➖ 不接 | 内容生产管线,属 Web 管理台 |
| GET /api/writing/stats, POST /api/writing/submit | 🚫 禁区 | 手写练习,Android 明确排除 |

**核心结论:后端已经领先客户端一个版本。** 至少 6 个现成端点 + 3 类现成数据字段在 Android 完全没有入口,大多数扩展**不需要动后端**。

## 3. P0 扩展:后端现成、纯 Android 接入(优先做)

### 3.1 假名注音阅读模式(furigana)[仅Android]

- **价值**:这是「日语阅读」产品的基本功。字幕、跟读句、词汇例句里的汉字当前无注音,romaji 又不可读;接入后阅读体验质变。
- **接口**:`POST /api/ai/furigana`、`POST /api/ai/furigana/batch`(服务端已带 `ai_result_cache` 缓存,85 条命中记录;请求/响应结构可参照 `src/components/JapaneseRubyText.tsx` 与 `src/routes/SentencesPage.tsx` 的调用——仅作传输契约参考,不复刻其 UI)。
- **Android 落点**:
  1. `data/RemoteLabClient.kt`:新增 `fetchFurigana(texts: List<String>): Map<String, FuriganaResult>`(batch 端点,分块 ≤50 条/次)。
  2. `data/LearningModels.kt`:新增 `FuriganaResult`(原文 + 注音分段列表)。
  3. `data/LocalLabStore.kt`:注音结果本地缓存(text hash → json),避免重复请求。
  4. UI:`SubtitleBrowserScreen`(逐行注音开关)、`LessonScreen` 跟读节点、`LibraryScreen` 句子卡。注音渲染建议新建 `ui/components/RubyText.kt`(在汉字上方画小号 kana,用 `Layout` 或 inline content 实现),**不要**塞进现有巨型文件。
  5. `LabSettings` 增 `showFurigana: Boolean`(默认开)。
- **测试**:JVM 解析测试(响应 JSON→模型);分块逻辑测试;缓存命中测试。
- **工作量**:M(2-4 个提交)。**风险**:AI 注音可能出错 → UI 标注「AI 注音」,长按可关。

### 3.2 增强学习卡(linguisticPayload 先行)[仅Android,完整版需后端]

- **价值**:1,585 条 ready 的 AI 增强内容一行代码都没用上,是最大的「白拿」体验升级。
- **现状事实**:Worker 已把 `linguisticPayload`(nerd-lite 语言学加餐:术语 + 通俗解释 + domains)合并进 `/vocab`、`/grammar`、`/sentences` 每行响应(`worker.ts:3061/3077/3094`);但主 `payload`(headlineZh/structureZh/commonMistakes/exampleBreakdown/realWorldUseZh/microQuizHintZh)**未下发**(`worker.ts:2707` 的 select 未包含 payload 列)。
- **Android 落点(第一步,零后端)**:
  1. `RemoteLabClient.fetchVocab/fetchGrammar/fetchSentences` 解析 `linguisticPayload` → 模型加可空字段。
  2. `LibraryScreen` 词汇/语法卡加「语言学加餐」折叠区(TagChip 术语 + 通俗解释)。
  3. `LessonScreen` 学习卡节点(StudyCardNode)答完后的讲解区追加该内容。
- **第二步 [需后端]**:`attachLinguisticPayloads` 的 select 加上 `payload` 列并在 mapVocab/mapGrammar/mapSentence 透传 → Android 学习卡直接获得「结构 + 常见错误 + 逐词例句拆解 + 现实用法」完整讲解。后端改动约 5 行。
- **测试**:JVM 解析测试(含 payload 缺失/畸形容错)。
- **工作量**:Android S,后端 S。

### 3.3 句子深挖精读 [仅Android]

- **价值**:阅读中遇到难句 → 一键「精读拆解」,把 explain 从泛泛讲解升级为逐词/语法/语气三层。
- **接口**:`POST /api/ai/sentence-deep-dive`(缓存 kind=sentence_deep_dive 已有记录;请求体参照 `src/server/repositories/animeRepository.ts:433` 附近契约)。
- **Android 落点**:`RemoteLabClient` 新方法;入口放在 `SubtitleBrowserScreen` 行长按菜单 + `LibraryScreen` 句子卡按钮;结果复用 `StructuredAiResultCard`。状态放独立 state-holder(如 `ui/screens` 内新 `SentenceDeepDiveState.kt`),**不要**再进 `LabViewModel`(见 §6.1)。
- **工作量**:S-M。

### 3.4 全库语义搜索(RAG)[仅Android]

- **价值**:「这句台词在哪集?」「日语怎么表达 XX?」——38,002 行字幕 + Vectorize 索引已就绪,Android 却没有任何搜索框。
- **接口**:`POST /api/rag/search`(Vectorize index `anime-japanese-lab-subtitles`)。
- **Android 落点**:
  1. `RemoteLabClient.searchSubtitles(query): List<RagHit>`。
  2. 新 `SecondaryScreen.Search` 枚举 + `ui/screens/SearchScreen.kt`(新文件,自带小型 state-holder);入口:资料库顶部搜索框 + 字幕页搜索图标。
  3. 结果项 → 跳转对应作品/集数字幕定位行。
- **测试**:响应解析 JVM 测试;空结果/网络失败状态。
- **工作量**:M。**注意**:这是新增页面,严格走「feature-owned state contract」,别扩 `LabUiState` 超过一个字段。

### 3.5 角色语言画像 [仅Android]

- **价值**:「スバル说话为什么这么冲?」——口癖/句末/礼貌度/情绪的角色画像已是成品(4 份,中文 markdown),动漫学习产品的差异化卖点。
- **接口**:`POST /api/ai/character-profile`(参照 `animeRepository.ts:449` 契约;服务端有 DB 缓存)。
- **Android 落点**:`LibraryScreen` 作品区新「角色」入口 → 底部弹层展示画像(markdown 轻渲染,复用 `Common.kt` 的 MarkdownLikeText 思路);`RemoteLabClient` 新方法。
- **工作量**:S-M。

### 3.6 AI 精讲历史 [仅Android]

- **价值**:问过的 AI 讲解现在阅后即焚(66 条历史在服务器上睡觉);复习时最需要回看。
- **接口**:`GET /api/history`、`GET /api/history/detail`。
- **Android 落点**:`ReviewScreen` 或设置页入口「AI 讲解历史」;列表 + 详情复用 `StructuredAiResultCard`;`RemoteLabClient` 新方法。
- **工作量**:S。

### 3.7 造句批改(打字输入)[仅Android][需决策]

- **价值**:输出练习闭环——学完词/语法后用键盘造句,AI 批改(`sentence_correction_history` 已有 4 条 Web 端记录)。
- **边界说明**:手写禁区禁止的是 canvas 笔画书写练习;**键盘打字造句是另一种交互**,不触碰 `writing_*` 表。但因贴近禁区,**实施前需用户一句话确认**。
- **接口**:`POST /api/ai/correct-sentence`(契约参照 `animeRepository.ts:468`)。
- **Android 落点**:词汇/语法学习卡完成后的可选「造个句」入口 + 独立批改结果卡;新文件 `ui/screens/SentenceCorrectionSheet.kt` + 独立 state-holder。
- **工作量**:M。

## 4. P1 优化:用已有数据把现有功能做深(多数是解析层小改)

| # | 优化 | 现状 → 目标 | 落点 | 量 |
| --- | --- | --- | --- | --- |
| 4.1 | **JLPT 难度贯穿** | vocab.level 已进模型但基本没用于筛选/分组 → 资料库词汇按 N5-N1 分组/筛选 + 等级徽章;课程 Hub 显示本集难度分布 | `LibraryScreen`(UI 已有 TagChip)、`SampleLearningRepository` 分组 | S |
| 4.2 | **句子难度与语气标签** | `learning_sentences.difficulty/tone_tags` 未解析 → `ShadowingSentence` 补两字段,跟读选句按难度渐进、卡片显示「礼貌体/日常体」标签 | `RemoteLabClient.fetchSentences`、`LearningModels`、`LibraryScreen`/`LessonScreen` | S |
| 4.3 | **kana 读音优先** | `fetchSentences` 现在 `reading = romaji 优先`(RemoteLabClient.kt:356),kana 被丢 → 改为 kana 优先,romaji 移到设置开关(「显示罗马音」默认关) | `RemoteLabClient`、`LabSettings`、句子卡 UI | S |
| 4.4 | **原声徽章与筛选** | 4,290 句带原声但 UI 无区分 → 跟读列表「原声」徽章(audioUrl 非空)+「只看原声」筛选;K-ON 全 TTS 如实标注 | `LibraryScreen`、`LessonScreen` 跟读节点(AudioReliability 已有模型) | S |
| 4.5 | **SRS 字段透传** | `ProgressItem` 丢弃 ease/review_count → 补解析;`SmartReviewPlanner` 用 next_review_on+ease 精确排序与到期分桶(今天/明天/本周) | `RemoteLabClient.progressItem`、`LearningModels`、`domain/SmartReviewPlanner` + JVM 测试 | M |
| 4.6 | **错题→原句定位** | 错题与语言学题 payload 里已有 sourceId/行号 → 错题卡加「查看原场景」跳字幕页并滚动定位该行 | `ReviewScreen`、`SubtitleBrowserScreen`(接收定位参数)、`LabViewModel` 已有 openSubtitles | M |
| 4.7 | **练习题型审计** | 10 种 exercise_type 中低频题型(kana_to_kanji=53、grammar_short_answer=70、meaning_to_japanese=30)是否被 `SampleLearningRepository` 映射?审计后:能映射的接入六种节点,不能的明确丢弃并记录 | `SampleLearningRepository` + JVM 测试 | S |
| 4.8 | **字幕页场景分块** | 1,030 个 subtitle_chunks 未用 → 字幕页按场景折叠(需后端在 /subtitles 响应或新端点带 chunk 信息)**[需后端]**;先行版:客户端按时间间隔聚类分段 **[仅Android]** | `SubtitleBrowserScreen` | M |
| 4.9 | **每集计划展示** | `fetchEpisodePlan` 已拉到 plan_slot/notes 但 notes 不展示 → 课程 Hub「本集计划」卡显示计划说明与四类数量 | `LessonScreen` Hub 区 | S |
| 4.10 | **修改密码** | 设置页缺账号管理 → 接 `POST /api/auth/change-password`(密码仅存内存,沿用登录表单纪律) | `SettingsScreen`、`RemoteLabClient` | S |
| 4.11 | **AI 模型下拉** | 设置页模型名手输 → `GET /api/ai/models` 拉列表做下拉选择 | `SettingsScreen`、`RemoteLabClient` | S |

## 5. P2 产品级扩展(需要内容/后端配合,规划层)

- **5.1 语言现象图鉴 [需后端+内容]**:426 条 `linguistic_phenomena` 做成可浏览的「读空气图鉴」(按 6 领域分类、搜索、关联例题)。前置:新 Worker 端点 + name_ja/beginner_explanation 空值补全(内容管线跑一轮)。这是把 ReadAir 从「刷题」升级为「体系」的关键。
- **5.2 带注释阅读模式 [需后端+审核]**:`subtitle_line_phenomena`(385 条,draft)审核发布后,字幕页在含现象的行上打小圆点,点击弹出现象解释 + 证据。真正的「阅读理解层」。
- **5.3 真实连胜/XP [需核对+可仅Android]**:`EpisodeFocus.xp/streakDays` 当前来自本地拼装(`SampleLearningRepository`),与 `user_progress` 真实数据脱钩 → 用本地进度 + 服务端 progress 聚合真实连胜/每日 XP(本地可先算,后端聚合端点更准)。今日屏已用 streak/XP 语义色展示,数据真实化后价值翻倍。
- **5.4 离线内容包 [仅Android,架构级]**:当前每次进集都拉全量 JSON(HttpURLConnection 无缓存)→ 按集落盘 JSON 缓存(`filesDir/content-cache/{work}/{ep}.json` + ETag/updated_at),弱网/离线可学。保持「本地进度先行」语义不变。
- **5.5 新作品扩容 [内容管线]**:`linguistic_generation_batches`(26 批)管线已在运转;K-ON 句子无音频(0/412)→ 若要跟读体验一致,跑一轮 TTS 预生成或原声切片。Android 侧无需改动(音频选择规则已按 audioUrl 兜底)。

## 6. 工程与架构优化(支撑以上一切的地基)

按 `ANDROID_PROJECT_GUIDE.md` §12-13 的既有判断,落成执行顺序:

1. **新功能一律 feature-owned state**:本文档所有 P0 新页面/新弹层(搜索、深挖、历史、角色、批改)都建独立 `XxxState.kt` + 小 state-holder,只通过窄接口用 `RemoteLabClient`;`LabViewModel`/`LabUiState` 最多加一个入口字段。这是不再膨胀的执行标准。
2. **死代码处置**:`MineScreen.kt`(224 行,无路由引用)+ 四屏中 32 个未引用私有函数(约 1,496 行,guide §9.3)→ 建一次「删除 PR」:先 `testDebugUnitTest` + `assembleLocalSlim` + 模拟器冒烟,绿了就删。**逐文件删,不批量 reset。**
3. **解析层可测化**:`RemoteLabClient` 的 JSON 解析已是纯函数风格,新增解析(furigana/深挖/搜索/历史)延续「internal fun parseXxx(json)」+ JVM 测试的模式(现有 `RemoteLabClientTest` 就是模板)。
4. **SharedPreferences → DataStore**:审慎迁移,仅当 4.5/5.3 需要更复杂的本地聚合时再做;迁移必须保持「正确进度先落本地、云失败进 pending 队列」语义与备份排除规则。
5. **Compose 性能基线**:2026-07-26 的 UI 美化轮已补:Lazy 列表 key/contentType、重计算 remember 化(Today/Library/LessonHub)、语义色替换硬编码。新代码沿用此基线。
6. **测试缺口**:目前 148 个 JVM 测试、0 个 androidTest。P0 每项至少配:解析 JVM 测试 + 状态机 JVM 测试;字幕定位/搜索跳转这类导航行为补 Compose UI 测试(首个 androidTest 从 SearchScreen 开始最划算)。

## 7. 红线与合规(执行时逐条自检)

1. Supabase token / 会话 Cookie / 密码:不入源码、不入日志、不入文档。
2. `writing_practice_*` 与手写交互:**不接、不移植、不"顺手恢复"**;3.7 造句批改是键盘输入,实施前仍需用户确认。
3. Web 前端(`src/routes/**`、components)只作为**传输契约参考**,不是 Android 的 UI/流程规范。
4. 内部参考资产(`local-fusion-assets/`)的许可边界与 release 门禁(签名变量 + `AJL_PUBLIC_ASSETS_CLEARED`)保持不变。
5. 备份排除规则(auth cookie 在 SharedPreferences)不得放松。

## 8. 建议执行顺序(两周一个闭环)

```text
第一周 · 阅读闭环:
  4.3 kana 优先 → 3.1 furigana 注音 → 4.4 原声徽章 → 4.2 难度/语气标签
  (每步一个提交 + testDebugUnitTest;周末 assembleLocalSlim 装机验收)

第二周 · 讲解闭环:
  3.2 linguisticPayload 接入 → 3.3 句子深挖 → 3.6 AI 历史
  (可选:后端 5 行改动放出完整增强 payload)

第三周起 · 检索与身份:
  3.4 RAG 搜索(含首个 androidTest)→ 3.5 角色画像 → 4.5 SRS 透传 → 4.6 错题定位
  之后按 P2 逐项立项(现象图鉴优先)。
```

## 9. 验证命令速查

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
Set-Location 'C:\Users\汪家俊\jps\android-app'
.\gradlew.bat testDebugUnitTest --no-daemon --console=plain --max-workers=2
.\gradlew.bat lintDebug --no-daemon --console=plain --max-workers=2
.\gradlew.bat assembleLocalSlim --no-daemon --console=plain --max-workers=2
# APK: app\build\outputs\apk\localSlim\app-localSlim.apk
```

数据核对(需要时,token 走环境变量):

```powershell
# 例:核对某表行数(把 <TOKEN> 放入环境变量,勿写进文件)
curl -s -X POST -H "Authorization: Bearer $env:SUPABASE_TOKEN" -H "Content-Type: application/json" `
  -d '{"query":"select count(*) from learning_card_enrichments where status=''ready''"}' `
  https://api.supabase.com/v1/projects/qoatvdvbuleamyzsaldp/database/query
```

---

维护说明:本文档是决策快照。每完成一项,把对应条目移入文末「已完成」区并附提交号;后端/数据侧变化(新端点、新表、行数量级变化)时更新 §1/§2。

## 已完成

2026-07-26 第三批(P1 收尾 + 真实数据 + 工程清理;`testDebugUnitTest` 全绿 + `lintDebug` 0 错误 + `assembleLocalSlim` 成功):

- **P1-1(原 4.1)JLPT 难度贯穿**:资料库词汇区 N5→N1 筛选 chips(带当前集词数、rememberSaveable 记忆、与搜索叠加过滤),词汇卡等级标签按难度语义着色(N5/N4 绿、N3 蓝、N2 橙、N1 红容器色)。
- **P1-9(原 4.9)每集计划展示**:课程中心新增「本集计划」卡——planSlot 期数 chip + 词汇/跟读/语法/练习四色数量胶囊(只显示 >0 项)+ 策展 notes 两行摘要。
- **P1-10(原 4.10)修改密码**:`RemoteLabClient.changePassword` + 设置页「账号安全」内联表单(旧/新/确认三字段、内存态密码、401/400 错误映射);服务端保留当前会话并踢掉其他设备。
- **P1-11(原 4.11)AI 模型在线下拉**:`fetchAiModels()` 拉取 `{id,label}` 列表,设置页模型 chips 改在线数据源(失败回退硬编码、未知当前值补显「当前」chip)。
- **P2-真实连胜/XP(原 5.3,Android 侧)**:连胜此前已真实;本批把硬编码 XP(820/1260)替换为 `learningXp(progressItems)`(熟练10/可复习6/模糊4/答错2),与连胜同源同步于 7 个状态更新点,新增 `LearningXpTest`。
- **工程清理(§6.2)**:删除无路由引用的 MineScreen.kt(229 行)+ 四大屏幕 44 个无引用顶层私有函数,合计约 **2,142 行死代码**;ReviewScreen 经审计零死代码(旧清单过时)。遗留第三轮候选 4 个(见清理报告),下次清理处理。

2026-07-26 第二批(检索与身份 + SRS;`testDebugUnitTest` 全绿 + `lintDebug` 0 错误 + `assembleLocalSlim` 成功):

- **P0-4(原 3.4)RAG 语义搜索**:`RemoteLabClient.searchSubtitles`(`re-zero↔rezero` slug 双向映射)+ `RagSearchResult/Source/Analysis` 模型;全新 `SearchScreen`(AI 解读卡 + 场景命中卡 + 逐行跳转);入口:资料库伪搜索框 + 字幕页顶栏图标;`SecondaryScreen.Search` 路由。
- **P0-5(原 3.5)角色语言画像**:`fetchCharacterProfile` + `ui/reading/CharacterProfileController.kt`(选人/加载/画像/重新生成完整弹层)+ Android-owned 角色目录(re-zero 6 人 / k-on 5 人);入口在资料库作品区(按作品条件显示)。
- **P1-5(原 4.5)SRS 透传**:`ProgressItem.ease/reviewCount` 解析;`SmartReviewPlanner` 优先级公式追加 ease 权重(1..2→+14、3→+6)与"反复复习仍不熟"权重(reviewCount≥3 且 Bad/Fuzzy→+8);新增 `ReviewDueBucket` 四档到期分桶 + `overdueCount/dueTodayCount` 统计;队列屏按桶分组渲染 + SRS 元信息 chips;测试按新公式全量更新并新增 4 个用例。
- **P1-6(原 4.6)错题→原句定位**:`LabUiState.subtitleFocusLineNo` + `openSubtitlesAt(work, ep, line)`(跨作品跳转);字幕页滚动定位 + tertiary 脉冲高亮(等两帧再滚、只消费一次);复习屏错题卡与到期任务行"查看原句/查看本集台词"入口(行号从 sourceLabel 正则/payload 提取)。

2026-07-26 第一批(阅读闭环 + 讲解闭环,一次落地;`testDebugUnitTest` 全绿 + `lintDebug` 0 错误 + `assembleLocalSlim` 成功):

- **P0-1 假名注音**:`RemoteLabClient.fetchFuriganaBatch` + `FuriganaCache`(本地缓存,FIFO 600 条)+ `ui/reading/RubyText.kt`(汉字上方注音,注音块不跨行断开)+ `FuriganaAnnotator`(缓存优先、40 条/批、失败静默)。接入:字幕页(注音开关 chip,按作品/集记忆,只请求可见行——成本护栏)、资料库跟读卡、课程跟读题。设置总开关 `showFurigana`。
- **P0-2 增强学习卡(linguisticPayload)**:`/vocab` `/grammar` `/sentences` 的 `linguisticPayload` 解析进 `VocabItem/GrammarPoint/ShadowingSentence.linguistic`;仓库管道把它送进三类 `StudyCardNode`;资料库三类卡 + 课程学习卡渲染"语言学加餐"折叠区(headline/terms/domains/caution/historical)。**第二步(后端下发主 payload)仍待做,见 §3.2。**
- **P0-3 句子深挖**:`fetchSentenceDeepDive` + `ui/reading/SentenceDeepDiveController.kt`(feature-owned 状态机 + 共享 ModalBottomSheet)。入口:字幕页每行、资料库每句跟读卡。
- **P0-6 AI 精讲历史**:`fetchAiHistory/fetchAiHistoryDetail` + 全新 `AiHistoryScreen`(分组列表 + 详情弹层,数据自取不经 LabViewModel)+ `SecondaryScreen.AiHistory` 路由 + 设置页"学习记录"入口。
- **P1-2 难度/语气标签**:`ShadowingSentence.difficulty/toneTags` 解析 + 资料库/课程跟读卡 chips。
- **P1-3 kana 优先**:`fetchSentences` 改为假名 reading 优先,`romaji` 独立字段;设置新增 `showRomaji`(默认关)控制罗马音行显示。
- **P1-4 原声徽章与筛选**:`hasSourceAudio` 计算属性 + 资料库"原声"success 徽章 + "只看原声"筛选(带隐藏计数)+ 课程跟读卡原声徽章。
- 测试:新增 20 个 JVM 测试(`ReadingLayerParsingTest`、`LinguisticCardPipingTest`)覆盖全部新解析器与仓库管道。
- 架构纪律执行情况:注音/深挖/历史全部走 `ui/reading/` feature-owned 控制器;`LabViewModel` 仅新增 `SecondaryScreen.AiHistory` 枚举 + `openAiHistory()` 一个方法。
