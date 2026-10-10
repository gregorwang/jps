# 学习台（授業）设计 · 先学后练

2026-09-27。起因：言語学三卷都是「点教科書 → 直接做题」，知识只在答错后的反馈里出现。App 只有测验，没有教学。

## 1. 问题

以第三巻 活用 为例，现在的流程：

```
言語学 → 第三巻 → 8 本教科書（A 动词活用形 … H 敬语）→ 点封面 → 立刻 15 题
```

- 72 个语法点没有任何「讲」的环节。第一次见到「促音便」就是在选择题里。
- 选题 `pickSession` 把没学过的点直接混进来（新句按点轮换），等于用测验代替教学。
- 教科書封面下面没有目录。看不到「这本书有哪几课、我学到哪一课」。
- 讲解素材其实已经有了，只是藏在答题之后：每句的 `formula`（拆解）、`sense`、`note`，基础题库 topic 的 `beginnerExplanationZh`、`cautionNoteZh`，以及 `data/Conjugator.kt` 能推出任意动词的完整活用表。

## 2. 学习逻辑

一课 = 一个语法点。每课分四步，顺序固定：

| 步 | 名称（界面） | 做什么 | 素材 |
|---|---|---|---|
| 1 | 板書 | 讲规则：一句话、公式、行×段表里高亮变化的格子、2 个变形推导（書く→書か→書かない）、易错点 | 新增的每课讲义（见第 4 节）+ `Conjugator` |
| 2 | 用例 | 3–5 句原声台词，高亮目标部分，点一下播原声，展开拆解。只看、只听，不答题 | 已有 `conjugation_drill_items`（`formula`/`sense`/`note`/`audioUrl`） |
| 3 | 練習 | 5 题，只考这一课，答错马上给板書里对应的那条规则 | 已有 4 种题型，范围限定为本课 |
| 4 | 小テスト结果 | 本课标记为「已学」，句子进入 Leitner 复习 | 本地进度 |

规则：

1. **先学后练**：只有「已学」的课，句子才进入复习池和混合练习。没学过的点不再混进来。
2. **复习和新课分开**：
   - 复習 tab 和教科書页只放到期复习（只抽已学的课）。
   - 教科書页的墨色主按钮是「继续 · 第 N 課 <标题>」，也就是下一节没学的课。
3. **不锁课**：目录可以随便跳，只是用「推荐下一课」引导。已经会的课，可以在板書页点「跳过，直接小测」。
4. **答错回到板書**：复习时答错，反馈里的「深入讲解」改成「回到第 N 課 板書」，一点就打开那一课的讲义。
5. **一课 5–10 分钟**：板書一屏、用例 3–5 句、練習 5 题。不要又变回 15 题一组。

## 3. 界面结构

```
言語学
 └ 第三巻 活用
    └ 教科書 A 动词活用形          ← 点封面进入目次，不再直接开题
       目次（第 1–10 課，每课状态：未学 / 学习中 / 已学 ·掌握度）
       [继续 · 第 3 課 连用形＋ます]   ← 唯一的 InkButton
       [复习 · 到期 12 句]             ← OutlineButton，没有到期的就不显示
        └ 第 3 課（授業页，番剧语法：場面 01 板書 → 場面 02 用例 → 場面 03 練習 → つづく）
```

- 目次页：新屏 `ui/screens/learn/TextbookIndexScreen.kt`，用 `TimetableRow` 列课（学园物件）。
- 授業页：新 session `ui/screens/session/ConjugationLessonSession.kt`，复用 `Eyecatch`、`TsuzukuScreen`、`OptionRow`、`FeedbackSheet`。
- 板書：新组件 `ui/design/Blackboard.kt`（行×段格子 + 高亮变化格 + 推导箭头），第二巻也能用。
- 常驻工具「活用表」：授業页右上角图标，底部弹出当前例句动词的 `Conjugator` 完整表。

## 4. 数据

- **每课讲义（新增，72 条）**：`app/src/main/assets/conjugation_lessons.json`，按 `point_id` 索引。
  - 字段：`rule`（一句话规则）、`formula`（公式）、`gridHighlight`（行×段里要高亮的段）、`examples`（2 个推导，辞书形→结果）、`pitfall`（易错点）、`prereq`（前置课）。
  - 讲义是原创语法说明，不含台词，可以提交到公开仓库。
  - 由 Claude 直接编写，不走后端，也不改 worker。
- **课程进度（新增）**：`LocalLabStore` 加 `lessonStatus: Map<pointId, Learned>`。现有 `DrillProgress` 不动。
- **选题改动**：`ConjugationDrillRules.pickSession` 增加参数 `learnedPoints`；新增 `pickLesson(pointId)`，从本课抽 5 题，干扰项可以来自已学的课。
- **老用户迁移**：已经有 Leitner 记录的点，自动算「已学」，进度不丢。

## 5. 推广到另外两卷

- **第二巻 基礎**：
  - 结构一样，一个 topic 一课。
  - 板書直接用 topic 的 `shortDefinitionZh` / `beginnerExplanationZh` / `cautionNoteZh`，不用新写。
  - 練習用该 topic 的题。
  - 改动最小，第三巻做完就接。
- **第一巻 台詞（读空气）**：
  - 按领域开课，板書讲这个领域的「读法」。需要新写讲义，优先级最低。

## 6. 分阶段

| 阶段 | 内容 | 落点 |
|---|---|---|
| S1 | 目次页、授業页（板書 / 用例 / 練習）、先学后练选题、72 课讲义 | `ui/drill/`、`ui/screens/learn/`、`ui/screens/session/`、`assets/` |
| S2 | 复习答错「回到板書」、活用表工具、Review 页只抽已学 | `ConjugationSession.kt`、`review/` |
| S3 | 第二巻 基礎 套用同一结构 | `FoundationSession.kt`、`LinguisticsScreen.kt` |
| S4 | 第一巻 读空气 领域讲义 | 待定 |

验证：`design/v3build.ps1 -Mode compile`，用户手测。
