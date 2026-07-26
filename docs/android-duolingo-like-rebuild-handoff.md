# Android 多邻国-like 重构交接文档

## 文档作用

这份文档是后续重构 Android App 训练体验时的长期交接和复盘基准文档，不是一次性的任务说明。

后续每一轮如果在多邻国-like 改造中发现新的成功经验、失败原因、产品判断、数据库事实、视觉验收结论、素材接入方式，都应该继续补充到这份文档里。新会话开始前应先阅读本文档，再阅读素材说明和项目代码，避免重复犯同样的错误。

本文档只沉淀项目上下文、经验教训、设计原则和验收标准；具体给下一轮 Agent 的提示词不要写进本文档，提示词应在当前会话中单独给出，方便按当时目标调整。

## 背景

本项目是一个用于动漫日语学习的 Android App。当前目标不是做一个普通训练菜单，而是把训练主流程重构成 Duolingo-like 的课程路径体验。

这轮会话已经验证出一个关键结论：多邻国-like 不是单纯换皮，不是把卡片改成圆形按钮，而是一个完整产品结构：

- 课程/作品选择
- 集数目录
- 当前集学习路径
- 节点练习
- 答题反馈
- 复盘回炉
- 奖励/XP/能量/连胜
- 公共素材音效、动画、按钮手感

下一轮需要按产品结构重建，而不是继续在旧训练菜单上贴样式。

## 本轮成功点

1. 训练页方向已经从“模式列表”转为“路径地图”
   - 顶部有 HUD：当前课程、火焰/XP/能量等。
   - 中间是纵向路径节点。
   - 节点包括快速复盘、综合练习、词汇练习、语法练习、跟读训练、读空气、奖励宝箱。

2. 节点开始绑定真实数据
   - Re:Zero EP01 通过数据库/远程数据确认有：
     - 计划词汇 40
     - 语法 25
     - 跟读 25
     - 练习 40
     - 语感题 14
   - K-ON EP01 也确认了真实材料池。
   - 词汇节点已经能拆成 `词汇练习 1 / 词汇练习 2 / ...`，点击后进入对应练习。

3. 右侧书本入口修正为“选集目录”
   - 之前错误地跳到资料库。
   - 正确产品语义应该是当前作品/季度/集数目录。
   - 目录应显示 `EP01 / EP02 / EP03...`，不是“第 1 阶段 / 第 2 阶段”。

4. 答题反馈有所改善
   - 正确反馈：绿色底栏、XP、继续按钮。
   - 错误反馈：红色底栏、正确答案、错误/正确选项高亮。
   - “复制题目”降级为小型次操作。

5. 公共素材已进入 APK
   - Debug APK 约 109 MB。
   - APK 内确认包含 `flash_cards_shuffle_correct.mp3`、`flash_cards_incorrect_try_again.mp3`、`node_complete.mp3` 等音效。

## 本轮失败点

1. 一开始做成了“贴皮”
   - 只是把 UI 改成绿色/粉色/圆形节点。
   - 没先改产品逻辑，所以看起来和多邻国割裂。

2. 没有第一时间把 Supabase 数据库作为事实源
   - App 的真实结构是 `作品 -> 集数 -> 当前集材料池`。
   - 不是固定“第 4 阶段、第 41 部分、参加游戏之夜”。
   - 这些阶段标题不能硬套，必须由数据库里的作品、集数、材料数量推导。

3. 对“多邻国-like”的理解太表面
   - 多邻国核心不是圆形按钮，而是路径系统、课程切换、节点弹窗、选集目录、即时反馈、声音/触觉/动效编排。
   - 下一轮必须先建路径模型，再做表现层。

4. 素材没有做到场景级融合
   - 素材虽然进了 APK，但还没有形成完整资产索引。
   - 应该按场景映射：
     - 节点
     - 当前节点脉冲
     - 宝箱
     - 角色
     - 按钮
     - 答对
     - 答错
     - 连击
     - 完成页

5. 验收不能只靠编译
   - 必须用模拟器逐屏截图验收。
   - 本轮后期通过 Computer Use + ADB 才真正发现可做节点灰色像锁定的问题。

## 数据库事实源

下一轮必须先查 Supabase，不能猜。

项目 Supabase：

- Project name: `jps`
- Project ref/id: `qoatvdvbuleamyzsaldp`

重点表：

- `works`
- `episodes`
- `episode_learning_plans`
- `learning_vocab_items`
- `learning_grammar_points`
- `learning_sentences`
- `learning_exercises`
- `linguistic_exercise_drafts`
- `subtitle_lines`
- `subtitle_chunks`
- `user_progress`

重要事实：

- `episode_learning_plans` 的数量不是标量字段，而是 JSONB 数组长度：
  - `vocab_item_ids`
  - `grammar_point_ids`
  - `shadowing_sentence_ids`
  - `exercise_ids`
- `learning_vocab_items` 是全作品词汇表，没有直接 `episode` 字段，具体集数词汇要通过 plan 数组或其他关联推导。
- `learning_grammar_points`、`learning_sentences`、`learning_exercises`、`linguistic_exercise_drafts` 有 `work_slug` / `episode`。

已核验示例：

- `k-on EP01`
  - plan vocab 20
  - plan grammar 5
  - plan shadowing 5
  - plan exercises 17
  - actual grammar 15
  - actual shadowing 30
  - actual exercises 35
  - read-air published 11

- `re-zero EP01`
  - plan vocab 40
  - plan grammar 25
  - plan shadowing 25
  - plan exercises 40
  - actual grammar 25
  - actual shadowing 25
  - actual exercises 40
  - read-air published 14

- `re-zero EP26`
  - 有多个 `plan_slot`
  - slot 1: vocab 32, grammar 12, shadowing 15, exercises 76
  - slot 2: vocab 28, grammar 8, shadowing 15, exercises 15
  - slot 3: vocab 54, grammar 26, shadowing 15, exercises 149
  - actual grammar 26
  - actual shadowing 33
  - actual exercises 149
  - read-air published 1

## 推荐产品结构

### Tab 职责

- 今日
  - 总入口
  - 到期复习
  - 今日建议
  - 当前作品继续学习

- 训练
  - Duolingo-like 路径主界面
  - 不是训练模式卡片菜单

- 资料
  - 作品、集数、台词、词汇、语法、跟读材料浏览

- 复盘
  - 错题
  - 到期复习
  - 弱点分类

- 我的
  - 账号
  - 设置
  - 同步状态

### 课程模型

建议新增或重构为以下纯 UI 模型：

```kotlin
data class CourseTrack(
    val workSlug: String,
    val title: String,
    val seasons: List<CourseSeason>,
)

data class CourseSeason(
    val title: String,
    val episodes: List<CourseEpisode>,
)

data class CourseEpisode(
    val episode: Int,
    val title: String?,
    val totalCues: Int,
    val chunkCount: Int,
    val plan: EpisodePlan?,
)

data class EpisodePath(
    val workSlug: String,
    val episode: Int,
    val title: String,
    val units: List<PathUnit>,
)

data class PathUnit(
    val id: String,
    val title: String,
    val nodes: List<PathNode>,
)

data class PathNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: PathNodeKind,
    val state: PathNodeState,
    val action: PathNodeAction,
    val batch: Int,
    val countLabel: String,
)
```

### 节点拆分规则

不要固定 7 个节点。

建议规则：

- 词汇：每 20 个词一个节点
  - `词汇练习 1`
  - `词汇练习 2`
  - `词汇练习 3`

- 语法：每 6 个点一个节点
  - `语法练习 1`
  - `语法练习 2`

- 跟读：每 6 句一个节点
  - `跟读训练 1`
  - `跟读训练 2`

- 读空气：每 7 题一个节点
  - `读空气 1`
  - `读空气 2`

- 综合练习：每 10 题一个节点
  - `综合练习 1`
  - `综合练习 2`

- 奖励宝箱：每 4 个可做节点插入一个

- 复盘节点：
  - 如果有到期复习或本地错题，显示为黄色当前/待处理节点
  - 如果没有，不要抢占首屏主路径

## 视觉验收标准

下一轮不要只说“编译通过”，必须截图验收。

至少验收这些截图：

1. 今日 Tab
2. 训练 Tab 首屏
3. 训练路径向下滚动后
4. 顶部课程切换器
5. 右侧书本选集目录
6. 点击词汇节点后的第一题
7. 答对反馈
8. 答错反馈
9. 完成页
10. 设置页音效/触觉/动画开关

视觉判断：

- 可做节点必须是绿色或明确可点击状态。
- 锁定节点才是灰色。
- 当前节点可以粉色/黄色并带脉冲或高亮。
- 宝箱应明显是奖励，不应像普通练习。
- 底部不能再出现旧的“只练语感/只练语法/未做过的题”筛选胶囊区域。
- 选集目录必须显示 EP，不显示“第 1 阶段/第 2 阶段”。

## 素材融合建议

素材目录：

```text
C:\Users\汪家俊\proui\duolingo-like-assets
```

重点文档：

```text
C:\Users\汪家俊\proui\duolingo-like-assets\AI-USAGE-GUIDE.md
```

下一轮应该先读该文档，再读 Android 项目。

素材融合不要散写资源名。应该集中维护：

```kotlin
object LearningAssetRegistry {
    fun soundFor(event: FeedbackEvent): SoundAsset?
    fun hapticFor(event: FeedbackEvent): HapticAsset?
    fun visualFor(event: FeedbackEvent): VisualAsset?
    fun nodeVisual(kind: PathNodeKind, state: PathNodeState): NodeVisualAsset
}
```

反馈事件至少包括：

- `TapPrimary`
- `TapSecondary`
- `OptionSelect`
- `AnswerCorrect`
- `AnswerWrong`
- `Combo`
- `LessonNodeUnlock`
- `XpGain`
- `StreakExtend`
- `LessonComplete`
- `ReviewScheduled`

## 下一轮执行顺序

1. 读素材使用文档
2. 读 Android 项目结构
3. 用 Supabase 只读查询确认数据结构和目标集数材料数量
4. 设计路径 UI model
5. 先实现数据派生和节点拆分测试
6. 再改 Compose UI
7. 接入素材 registry
8. 接入反馈音效/触觉/动画
9. 构建 APK
10. 用模拟器逐屏截图验收
11. 根据截图修视觉问题
