# 単語卡重做 + 帳面重排 · 交接

> **2026-09-30 · 0.19.0 已做完主体**（用户看过画布，说「照画布来」，第 0 节三个问题按提议默认通过）：単語标签（`ui/words/Tango.kt` + `screens/review/TangoScreen.kt`）、帳面重排、错题 / 期日の課題 / 苦手搬进「復習の順番」页（`SmartReviewQueueScreen`，練習 → 課程 場面 06 打开）、`assets/vocab_lines.json`（生成脚本 `archive-content-sources/vocab-cards-v1/fetch_subtitles.py` + `build_lines.py`）。
> **还没做**：① 「常一起出现」目前只从 note 里抽「…」片段（4.3 的 a），没有中文释义；要做 b 就交给 Antigravity 补 `collocations` 列。② 排序的「出现次数」用的是字幕里含这个词的行数（`freq`），不是 `learning_vocab_items.total_occurrences`。③ 用户手机上看后的反馈。下面是原始交接，供查。

这份文档写给**专门做单词卡的新会话**。先读 `CLAUDE.md`（第 2、3、4 节和第 8 节 0.18.0 那条），再读这里。

## 0. 开工前先问用户这 3 件事

画布上的方案用户还**没确认**。这三件事会改变要做什么，所以要先问：

1. **单词卡照画布这个方向做行不行？** 方向是：先想再看、5 个一组、组末一次小测。
2. **错题本、期日の課題、苦手 移出帳面，放到「練習 → 課程」里，可以吗？** 这几项本来就是做题时产生的。
3. **单词按什么顺序推？** 提议：先推知识卡例句里出现过的词，再推自習学过的课里的词，最后按出现次数排，不再随机。

照 memory 的规矩，方向上的修改直接画在画布上给用户看，不要写长段文字方案。

## 1. 用户的原话和意图（2026-09-30）

> 账面这里怎么还有之前过期的题目各种错误的题目……而且我不知道这里为什么还有词汇卡片，词汇卡片这里我觉得应该得重构一下，或者说词汇卡片这里后续得专门有一个开关来刷词汇，目前因为我们做了很多专门的知识点卡片，我们应该以专门的知识点卡片为主。

- 知識流以知识卡为主。单词要有一个专门的开关，想刷的时候才进去刷。
- 帳面里不应该出现过期题目和错题。
- 以前的单词卡就是一张摊开的词典条目，没有「想一下」的过程。2400 个词随机推（布石、紫紺、全力走行……），基本都是噪音。

## 2. 0.18.0 已经做了什么（不要重做）

- 知識流默认只推知识卡（`FeedRules.stream` 里 `deck == null` 的分支）。单词只在「帳面 → 单词」里刷：伪合集 `KnowledgeRules.VocabDeck = "@vocab"`，卡面还是旧的 `VocabBody`。
- 知识卡：♥ 掌握 = 不再推（30/90 天回来考）；收藏 = 每天回来一次（`KnowMark.starDay`）；两者互斥。伪合集 `StarDeck = "@star"` 用来只刷收藏的卡。
- 翻页手感：`screens/review/FeedSwipe.kt`。单词卡如果有卡内滚动，也要走 `rememberFeedPageHandoff`（Pager 已经统一套上了，不用另外处理）。

## 3. 画布上的方案

画布：https://claude.ai/artifact/9x3RkMeAtAYTN64i8T8HN4 ，页面「知識 · 単語 重构 / 帳面（预览）」，page id 是 `tango`。只读下面这 4 张：

| 画板 | 内容 |
|---|---|
| `TangoFront.dc.html` | 正面：顶部标签 知識 · **単語** · 帳面；罗马音 → 假名 → 汉字（大）；含这个词的台词（词下划线）+ 罗马音 + 原声；底部网点遮挡「意思？」，点一下翻开。右侧栏：♥ 掌握、收藏。顶部 5 段小进度。 |
| `TangoBack.dc.html` | 背面：意思、「常一起出现」搭配 2–3 条（約束を守る／約束を破る／約束する）、一句备注、台词 + 原声。右侧栏多一个「再来」。 |
| `TangoCheck.dc.html` | 组末小测：顶部 5 个词和对错点；听台词，选意思（4 选 1）；唯一的墨色主按钮「次の 5 個」。 |
| `LedgerNew.dc.html` | 帳面重排：合集 / 收藏的知识点 / 掌握了的 / 単語（已会·在学·斩，今日 5）/ 活用。错题本、期日の課題、苦手 都不在这里了。 |

生成脚本在上一个会话的 scratchpad 里（`gen_tango.py`），已经不在了。要改画布，就直接 `read` 画板文件来改。

## 4. 数据现状

### 4.1 `assets/vocab_cards.json`（约 490KB，可以进仓库）

```json
{ "version": 1,
  "cards": { "<vocabId>": [keep, reading, lemma, lemmaReading, pos, meaning, note, easy?] },
  "heads": { "<vocabId>": "词头" } }
```

- 共 3770 条。`heads` 有 2383 条，专给 id 是 UUID 或编号、从 id 里看不出词头的词条用。取词头统一走 `KnowledgeRules.headOf`。
- `keep=0` 的不用（例：`世界` 是 `[0,'',…]`）。`easy=1` 的是看番的人都认识的词。进流的条件在 `KnowledgeRules.vocabPool`：keep、非 easy、有意思、有词头、没被斩，按词头去重。
- `pos` 是中文标签（名词、动词、い形容词、な形容词、副词……），`Conjugator` 两种标签都认。
- **`note` 大约一半是空的**。有内容的常常带搭配，比如「常搭配「打つ」表示提前布局：「布石を打つ」」。画布背面的「常一起出现」目前**没有现成字段**。
- 来源和校验工具在 `../archive-content-sources/vocab-cards-v1/`（gitignore，只在本机）：`export.py`、`check.py --install`、`push_cloud.py`（写回 Supabase），以及几份 Antigravity 提示词。

### 4.2 台词（卡面上「含这个词的台词 + 原声」）

- **本地 asset 里没有单词对应的台词。** 辞書的单词练习（`screens/library/WordStudy.kt` 的「原作里」）是运行时用 `WordDrill.examples(item, lines, drill)` 现找的：先在当前集的 `/sentences`（`ShadowingSentence`，最多 30 句）里找，再到活用练习的台词里找。先整词匹配，匹配不到再退到词干。知識流里没有「当前集」这个概念，所以这条路用不了。
- 要做，就得**离线预先配好**：写个脚本放在 `vocab-cards-v1/` 旁边，从 Supabase 的 `learning_sentences`（`ja_text`、音频路径）里给每个词挑 1–2 句（整词出现、句子短的优先，有原声的优先），结果写进一个新 asset（比如 `vocab_lines.json`：`id → [ja, audioUrl]`）。数据来源：
  - 只读查询：PostgREST + `wrangler.toml` 里的 publishable key 就够了（见 CLAUDE.md 0.6.0 那条）。请求 worker 时要带浏览器 User-Agent。
  - 原声只有 Re:ゼロ 第 26–50 话有。其余台词用 TTS，或者语音包（`VoicePack` 按原文的 sha1 查）。
  - **字幕的中文经常和日文错位，不要当译文用**。卡上只显示日文台词和逐词罗马音，不配中文。
  - 卡片上**不显示出处**（集数、时间、说话人），这是用户明确要求的。
- 装进去之前先算一下 asset 的大小（比如 3000 词 × 1 句 ≈ 200–300KB，可以接受）。

### 4.3 搭配（背面的「常一起出现」）

有两个办法，需要让用户选（或者自己拍板，汇报时一句带过）：
- a) 先从 `note` 里抽「…を…」这类「」片段来用，抽不到就不显示这一块。零成本，覆盖率低。
- b) 按 `vocab-cards-v1` 的老流程，交给 Antigravity 补 `collocations` 列（提示词 + 分批 md + `check.py` 校验）。**先把提示词交给用户，再写代码**，两边并行。

## 5. 要改的代码

包路径 `app/src/main/java/com/animejapaneselab/nativeapp/`：

- **单词的状态**：新建一个独立的 state holder（例如 `ui/words/Tango.kt`，照 `ui/knowledge/Knowledge.kt` 的写法：进程级 `object` + `StateFlow` + `LocalLabStore`），负责：
  - 每个词的 Leitner（记得 → 1/3/7/14/30 天，「再来」→ 当组末尾再来一次）；
  - 今天这组 5 个是哪几个、做到第几个、小测结果。
  - ♥ 掌握 = 斩：沿用 `KnownWords.setWord`，和辞書的「已斩」是同一份数据。收藏 = 进收藏本（`Notebook.toggle`，`NotebookKind.Vocab`），这部分 0.18.0 已经是这样了。
  - 每次判定和每道小测题都要调 `StudyLog.record(...)`；一组做完调 `StudyLog.finishSession` 点亮格子（规矩见 CLAUDE.md 0.8.4 / 0.14.0）。
- **入口（开关）**：`screens/review/ReviewFeedScreen.kt` 顶部的 `TextTabs(listOf("知識", "帳面"))` 改成 知識 · 単語 · 帳面。単語 标签可以直接复用 `FeedPager`（用 `VocabDeck` 那条流），也可以另写一个 `TangoPager`。如果卡片序列是「5 张词卡 + 1 张小测」这种固定结构，另写一个更清楚。
- **卡面**：`screens/review/KnowledgeCardBody.kt` 里的 `VocabBody` 换成正面 / 背面两态。遮挡可以用现成的 `ui/design/Notes.kt` 的 `CoveredLine`，或者用 `ReviewFeedScreen.kt` 里的私有 `RevealBox`。逐词罗马音用 `ReadingLineText`，读音来自 `rememberFuriganaAnnotator`。声音用 `VoicePill` 或 `VoiceSwitchPill`（原声 / TTS 可滑）。
- **小测卡**：新加 `FeedKind`，或者在 Tango 自己的 pager 里单独写一种卡。构造选项的规矩见 CLAUDE.md 0.5.1：正确项一个不能少，干扰项只用来补空位。干扰项从同词性的词里挑。
- **排序**：`KnowledgeRules.pickVocab` 现在是 `shuffled(seed)`。改成：知识卡 `examples` 里出现过的词 → 自習学过的课（`ConjugationDrillState.learned` 对应的台词）里出现过的词 → 其余的（没有出现次数字段的话，就按 `learning_vocab_items.occurrence`，要从云端拉）。
- **帳面**：`ReviewFeedScreen.kt` 里的 `Ledger(...)`。「単語」那一行显示 已会 / 在学 / 斩 和今日数量。要删掉的：`苦手なところ`、`間違いノート · 错题本` 那一行、`期日の課題` 列表。「收藏 · 生词和句子」这一行留不留，自己拍板。
- **错题等搬去練習**（等用户确认）：`screens/learn/CourseScreen.kt`。場面 06（`SceneKind.Review`）现在跳到復習 tab（`LabApp.kt` 里 `onStartReview = { viewModel.selectTab(LabTab.Review) }`，两处），要改成打开错题本。需要的动作已经有了：`viewModel::practiceLocalMistake`、`viewModel::practiceReviewTask`（现在经 `ReviewFeedActions` 传给帳面）。错题卡片流 `FeedRules.due(only = FeedSource.Mistake)` 也可以搬过去复用。
- **今日 三限**用的是 `FeedSession.remaining`（到期卡），和单词无关，不要动它。

## 6. 规矩（容易踩的）

- 罗马音在最上面，下面是假名，再下面才是汉字（CLAUDE.md 0.15.0）。每屏最多一个墨色主按钮。不写解释性小字。颜色只用 `AjlTheme.colors` 的令牌。
- 结算页的主按钮只能是往前走的动作（「次の 5 個」可以）。「再来一组」最多当次要按钮（CLAUDE.md 0.10.0）。
- 斩这类破坏性操作要过线才生效、震一下，并给 4 秒撤销（`ui/design/Gestures.kt` 的 `UndoBar`）。
- Pager 的 key 必须唯一，同一张卡再次出现时加 `#n`。
- 只做编译验证（`design/v3build.ps1 -Mode compile`），不写新测试、不截图。每做完一块能编译的就 commit 并 push main。全部做完后跑 `-Mode full`，然后按 CLAUDE.md 第 6 节发版，同时写 CHANGELOG。
- 结束时在 CLAUDE.md 第 8 节补经验，把本文件加进第 1 节的表（已经加了），做完的条目在这里划掉。

## 7. 建议顺序

1. 问用户第 0 节的 3 件事，按回答改画布或直接开做。
2. 帳面重排 + 错题搬去練習（纯界面，不依赖新数据），编译，commit。
3. 写 `vocab_lines.json` 的生成脚本，装入 asset，commit。
4. 做 Tango state holder、単語 标签、正面 / 背面卡、小测卡，编译，commit。
5. 搭配（4.3 的 a 或 b）。
6. `-Mode full`，发版。
