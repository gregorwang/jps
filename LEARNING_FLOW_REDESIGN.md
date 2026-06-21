# 学习流与题型重构评审

## 这版文档的修正重点

这版按新的讨论修正方向：先不做全局设计语言、不做 CSS 大改、不把所有模块都往多邻国套。当前重点是业务逻辑和交互逻辑，也就是“什么数据生成什么题型，用户怎么做，系统怎么反馈，音频怎么播放，错题怎么回流”。

多邻国参考图只用来分析低阶训练题型的交互，不作为整个网站的视觉模板，也不适合套到语言学/读空气这种高阶题。

## 当前共识

1. 学习材料和做题训练要分层，但学习页不一定必须全屏。
2. 练习页可以隐藏左侧导航，但这是为了减少干扰，不是为了做成纯全屏游戏。
3. 平板/iPad 是重要使用场景，交互要适合触控、听音、点选和持续练习。
4. 暂时不要优先改全局 CSS。全局视觉重构收益低、风险高，容易牵连所有页面。
5. 手写模块不强行改成多邻国式题型。它本身是独立训练逻辑，保留现有方向。
6. 语言学/读空气题是高阶题，不和多邻国那类词汇/语法低阶题比较。
7. 对话完成题先搁置或降级，不要为了做题型硬从全局台词里拉一句来配。

## 音频逻辑必须先理清

之前文档把“发音/听音”讲得太粗。项目里实际存在三种音频来源，它们不能混用：

### 1. 原声音频

这是最重要的听力/跟读材料。当前 `LearningSentence` 类型里已有：

- `audioUrl`
- `storagePath`

`SentencesPage` 里也已经通过 `buildReZeroShadowingAudio` 判断 Re:Zero 跟读句原声，并用 `AudioButton` 播放。

原声适合：

- 跟读句
- 听音语块拼接
- 真实台词听力
- 音系/听感判断
- 读空气题里的语气证据

如果某条原声被标记为不匹配或不可靠，就不能自动当成听力题依据，只能显示为“可能不准，建议 TTS”或降级为辅助材料。

### 2. TTS 音频

当前 `TtsButton` 调 `speakJapanese`，适合生成标准辅助读音。

TTS 适合：

- 独立词汇发音
- 没有原声的语法例句
- 没有原声的普通句子辅助朗读
- 配对题中点击日文后的读音反馈

TTS 不适合当作：

- 音系学判断证据
- 角色语气判断证据
- 动漫原台词语调证据
- 高阶读空气题中的“真实发音”证据

### 3. 无音频题

有些题不需要音频，例如：

- 中文到日文选择
- 语法填空
- 词义配对的中文侧
- 纯文本语境判断

这些题不要为了像参考图而强行播放音频。

### 音频优先级

建议每道题都有明确音频策略：

```ts
type PromptAudio =
  | { kind: 'source'; url: string; autoPlay: boolean; reliability: 'verified' | 'flagged' }
  | { kind: 'tts'; text: string; autoPlay: boolean }
  | { kind: 'none' }
```

优先级：

1. 跟读/听力/语气判断：优先原声。
2. 原声可靠时，进入题目后可自动播放。
3. 原声不可靠时，不自动播放，只给手动播放和警告。
4. 没有原声但题目仍适合听音训练时，用 TTS，并明确这是标准辅助音，不是真实台词。
5. 音系和读空气中的听感证据题，没有可靠原声就不生成这种题。

iPad/Safari 可能限制页面加载后自动播放，所以实际实现要在用户点击“开始练习”后，把后续题目的自动播放视为同一次学习交互链的一部分。如果自动播放失败，显示大播放按钮，不要让题目卡死。

## 模块边界

### 普通低阶训练

包括：

- 词汇识别
- 词义配对
- 听音选义
- 语块拼接
- 语法填空
- 句意拼接
- 中文到日文选择

这部分可以参考多邻国的交互，因为目标是快速、短闭环、低负担。

### 语言学/读空气训练

这是高阶训练，不能按多邻国题型硬套。

它的核心不是“选哪个词”，而是：

- 识别语境线索
- 判断说话人的意图
- 比较选项背后的语用差异
- 根据台词证据解释为什么
- 必要时让 AI 解释错因

所以 `LinguisticTrainingPage` 和 `EpisodeLinguisticsPage` 应保留高阶题形态。可以借用“单题推进”和“明确反馈”，但不应该改成配对、语块拼接、词汇选择这类低阶题。

### 手写训练

手写是独立模块，不强行变成多邻国式。

当前 `WritingPracticePage` 的方向是合理的：

- 目标字词
- 读音和释义
- Canvas 描红/书写
- 提交
- 通过就进入下一个
- 可跳过/重写

这一块后续最多做交互细节优化，不进入这轮题型重构主线。

旧的 `PracticePage` 需要重新评估，因为它现在和 `WritingPracticePage` 职责重叠，而且 API 层还有混用题库的问题。

### 资料学习页

词汇页、语法页、句子页可以保留资料浏览能力。

但它们不应该承担正式训练主路径。更合理的是：

- 资料页用于查、看、AI 精讲、复习来源。
- 正式做题进入 lesson/练习队列。
- 做错后再回到具体资料卡查看解释，而不是先让用户浏览 30 个解释。

## 当前代码和数据问题

### `ChoiceTrainer` 太窄

`ChoiceTrainer` 只能表达：

- prompt
- body
- choices
- answer
- explanation
- listenText

它适合单选题，但不适合：

- 词义配对
- 语块排序
- 句子拼接
- 填空
- 点击日文自动发音
- 悬停解释
- 错误撤销
- 底部反馈
- 原声/TTS 音频策略

不要继续把所有题型塞进 `ChoiceTrainer`。它可以保留为旧单选组件，新的训练应该有 `LessonPlayer` 或类似的题型引擎。

### `/practice` API 混用了题库

`src/worker.ts` 里 `/api/works/:work/episodes/:episode/exercises` 当前读的是 `linguistic_exercise_drafts`，然后映射成普通 `LearningExercise`。

这和 `agent.MD` 里的规则冲突：普通 `learning_exercises` 和 `linguistic_exercise_drafts` 应该分开。

这件事要在实现新练习队列时修掉：

- 普通练习不要从 `linguistic_exercise_drafts` 取。
- 高阶语言学题继续走 `/api/linguistic-exercises`。
- 手写走 `learning_vocab_items` 的 handwriting 数据。
- 新 lesson 队列由 Worker adapter 从可靠学习材料生成。

### `learning_exercises` 需要审计

MCP 样本显示 `learning_exercises` 里存在明显不可靠数据，例如某些假名提示和答案对应不上。

因此第一阶段不要把 `learning_exercises` 当主题库直接展示。更稳的做法是从更可靠的材料表生成题：

- `learning_vocab_items`
- `learning_grammar_points`
- `learning_sentences`
- `learning_vocab_occurrences`

`learning_exercises` 后面单独做清洗/审计，再决定哪些题型可以纳入。

### 每集材料量不是一次课

数据库里每集材料很多。比如 Re:Zero 某些集有上百个词、上百个语法点和两百多练习项。这个数量不能直接暴露给用户。

要把 episode 当素材来源，不要当一次 lesson。

正确粒度应该是：

- episode source
- lesson queue
- exercise node
- attempt/progress

## 推荐的新练习引擎

新增一个题型引擎，不先大改 CSS。

概念结构：

```ts
type LessonNode =
  | ConceptNode
  | ExerciseNode
  | FeedbackNode

type ExerciseNode =
  | VocabPairMatchExercise
  | AudioTileExercise
  | TranslationTileExercise
  | SingleChoiceExercise
  | ClozeChoiceExercise
```

暂时不把对话完成放进第一批。

每个 `ExerciseNode` 至少要有：

```ts
type ExerciseBase = {
  id: string
  source: {
    kind: 'vocab' | 'grammar' | 'sentence' | 'linguistic'
    sourceId: string
    workSlug?: string
    episode?: number
    lineNo?: number
  }
  prompt: string
  audio: PromptAudio
  explanation?: string
  difficulty?: string
}
```

答题结果要记录：

```ts
type ExerciseAttempt = {
  exerciseId: string
  exerciseType: string
  selected?: unknown
  correct: boolean
  answer: unknown
  durationMs?: number
  sourceId: string
}
```

这比现在只存 `itemId + state` 更适合后续错题回炉。

## 第一批题型

### 1. 词义配对

参考 `public/1.png`，但用自己的数据和交互。

来源：

- `learning_vocab_items.surface`
- `learning_vocab_items.meaningZh`
- `reading` 用于辅助显示或发音

交互：

- 左侧中文，右侧日文。
- 用户点中文，再点日文。
- 点日文后播放 TTS。
- 匹配成功变成完成态。
- 匹配失败显示短暂错误，然后撤销本次配对。

用途：

- 普通词汇入门。
- 复习词义。

### 2. 中文到日文选择

参考 `public/5.png`。

来源：

- `learning_vocab_items`
- 少量 `learning_sentences`

交互：

- 给中文。
- 选择正确日文。
- 日文选项可点击后播放 TTS 或原声片段。

注意：

- 干扰项不能继续随便 `slice(index, index + 3)`。
- 应优先同词性、相近长度、相近难度。

### 3. 听音语块拼接

参考 `public/2.png`。

来源：

- 首选有可靠原声的 `learning_sentences`。
- 无原声时可用 TTS，但题目应标为普通听句/拼句，不当作真实台词听辨。

交互：

- 进入题后自动播放，失败则显示大播放按钮。
- 下方给语块。
- 用户拼成日文句子。
- 可重听，可慢速。

用途：

- 跟读前置训练。
- 句子理解。
- 真实语流熟悉。

### 4. 日文到中文语块拼接

参考 `public/4.png`。

来源：

- `learning_sentences`
- `learning_grammar_points.jaExample`

交互：

- 给日文。
- 日文可播放，优先原声，其次 TTS。
- 下方给中文语块，用户拼意思。
- 反馈时显示自然中文和关键结构。

用途：

- 检查是否真的理解句子。
- 连接语法解释和真实句意。

### 5. 语法选词/填空

参考 `public/6.png`。

来源：

- `learning_grammar_points`
- `learning_sentences`

交互：

- 给句子，挖掉一个助词、活用、表达。
- 选项支持悬停/长按解释。
- 反馈时讲“为什么这个可以，为什么另一个不自然”。

用途：

- 语法训练主力。
- 比单纯问“这个语法是什么意思”更有效。

### 6. 普通单选

保留，但降低比例。

来源：

- 词汇、语法、句子、语言学题都可以。

用途：

- 快速确认概念。
- 高阶题的选项判断。

单选不能继续作为全部训练的主形态。

## 暂缓题型

### 对话完成

先不做第一批。

原因：

- 如果只是从全局台词拉相邻句做补全，题目质量可能很差。
- 对话自然性、角色意图、前后文都需要人工或 AI 校验。
- 多邻国这种对话题频率本来也不高。

替代思路：

- 后续做“下一句意图判断”而不是硬补台词。
- 给一段上下文，问“下一句更可能表达什么意图”。
- 或在读空气高阶题里做“回应功能选择”，不放入普通低阶 lesson。

## 题型比例

题型比例不能固定到所有内容都一样，要按训练轨道分。

### 普通综合 lesson

一轮 8 到 10 个练习节点，建议：

| 类型 | 比例 | 10 题示例 |
| --- | --- | --- |
| 词义配对 | 20% | 2 |
| 中文到日文选择 | 10% | 1 |
| 听音语块拼接 | 20% | 2 |
| 日文到中文语块拼接 | 15% | 1-2 |
| 语法填空/选词 | 25% | 2-3 |
| 普通单选 | 10% | 1 |

原则：

- 单选不超过 30%。
- 有可靠原声时，至少 20% 题目绑定听音。
- 每轮至少有 2 道需要“拼/配/填”，不能全是点选。
- 错题回炉会覆盖比例，优先重复用户错过的类型。

### 词汇 lesson

适合新词学习，建议：

| 类型 | 比例 |
| --- | --- |
| 词义配对 | 35% |
| 中文到日文选择 | 25% |
| 听音选义/听音识词 | 20% |
| 句中识别/轻量填空 | 10% |
| 普通单选 | 10% |

词汇不应该只问“这个词什么意思”，要加听音、配对和句中识别。

### 语法 lesson

适合一个语法点或少量相关语法，建议：

| 类型 | 比例 |
| --- | --- |
| 迷你概念卡 | 10% |
| 语法填空/选词 | 35% |
| 句意拼接 | 25% |
| 功能单选 | 20% |
| 错题变式 | 10% |

这里的关键是“看一个点，马上做一个点”。不要先列 30 个语法卡。

### 跟读/句子 lesson

适合有原声的句子，建议：

| 类型 | 比例 |
| --- | --- |
| 原声听音语块拼接 | 40% |
| 日文到中文拼接 | 20% |
| 跟读播放/自评 | 25% |
| 句中词汇/语法小题 | 15% |

如果没有可靠原声，就不要伪装成原声听力课，改成 TTS 辅助句子理解课。

### 复习 lesson

复习不按固定题型比例，按错误历史来。

建议：

- 70% 来自用户错过/模糊的来源。
- 30% 是同源变式题。
- 连续答对后降低出现频率。
- 连续答错时切到更简单的题型，比如从语块拼接降级到单选或迷你讲解。

### 语言学/读空气高阶题

不纳入普通 lesson 的比例。

建议单独一轮 3 到 5 题，或作为专题训练：

- 60% 语境/意图判断。
- 20% 证据定位。
- 20% 选项对比/错因解释。

这类题可以单题推进，但不做词义配对、语块拼接那一套。

## 学习页和练习页怎么分

### 学习页

保留页面式结构，适合 iPad 阅读和查资料。

功能：

- 查看词汇/语法/句子。
- AI 精讲。
- 播放原声或 TTS。
- 从资料卡发起“练这个点”。

不做：

- 不把整页资料和正式训练混在一个滚动流里。
- 不要求用户先读完所有语法卡。

### 练习页

练习页可以隐藏左侧导航，也可以做成更沉浸的练习容器。

但重点不是“全屏视觉”，而是：

- 当前只有一个任务。
- 音频策略明确。
- 选择/拼接/填空动作明确。
- 反馈明确。
- 下一题推进明确。

CSS 只做局部组件，不改全局设计系统。

## 推荐实现顺序

### 第一阶段：不改全局样式，先建题型引擎

新增：

- `LessonPlayer`
- `LessonFeedback`
- `VocabPairMatch`
- `SingleChoiceExercise`
- `AudioTileExercise`
- `TranslationTileExercise`
- `ClozeChoiceExercise`

先使用局部 class，不重写 `styles.css` 的全局框架。

### 第二阶段：新增 lesson adapter

Worker 或前端 adapter 从现有可靠表生成题：

- `learning_vocab_items` -> 词义配对、中文到日文选择。
- `learning_sentences` -> 原声/TTS 语块拼接、中文语块拼接。
- `learning_grammar_points` -> 语法填空、功能单选、句意拼接。

第一版可以先不建新表，等交互验证后再固化 lesson 数据模型。

### 第三阶段：修清楚 API 边界

- `/api/works/:work/episodes/:episode/exercises` 不再混用 `linguistic_exercise_drafts`。
- 高阶语言学题继续走 `/api/linguistic-exercises`。
- 手写继续走 `WritingPracticePage` 和 handwriting vocab。
- 新增 `/api/works/:work/episodes/:episode/lesson` 或类似接口。

### 第四阶段：接进度和错题

当前 `saveReviewState` 可以继续用，但新练习需要额外记录 attempt 细节。

优先记录：

- 题型
- 题源
- 是否正确
- 用户答案
- 正确答案
- 音频来源
- 耗时

这些数据以后决定复习题型比例。

## 下一轮代码改动建议

最小可执行范围：

1. 不动全局 CSS。
2. 不动手写模块。
3. 不动语言学/读空气高阶题页面。
4. 新增一个普通 lesson route。
5. 先接 K-ON EP01 fallback 或 API 数据生成 8 到 10 个普通训练节点。
6. 第一批只做三种题型：词义配对、单选、语法填空或语块拼接。
7. 音频抽象先写清楚，原声优先，TTS 辅助。
8. 首页“开始今日训练”后续再切到这个 lesson route。

这样可以先验证交互逻辑，不会因为视觉重构把全站弄乱。

