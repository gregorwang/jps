# 単語卡「常一起出现」补数据 · 交接

这份文档写给**专门补单词搭配数据的新会话**。开工前先读 `CLAUDE.md` 第 2、3 节，再读 `VOCAB_CARD_HANDOFF.md` 第 4 节（单词数据现状），然后读这里。

## 1. 要做什么

画布 `TangoBack.dc.html`（単語卡背面）的「常一起出现」一栏，每条是**日文搭配 + 中文意思**：

```
常一起出现
約束を守る   守约
約束を破る   违约、食言
約束する     名词＋する＝动词
```

App 现在（0.21.1）只能显示日文，而且大约 43% 的词一条都没有。原因是没有现成数据：`TangoRules.collocations`（`ui/words/Tango.kt`）只是从 `note` 里抽「…」片段。抽出来的片段只有日文，还经常是台词里的词，不是真正的搭配。

**目标**：给単語会推的每个词补 0–3 条常见搭配，每条带一个简短的中文意思。装进一个新 asset，卡片背面照画布显示。

## 2. 数据现状（2026-10-01 实测）

- 単語只推 `assets/vocab_lines.json` 里有台词的词：**3285 个**，而且都是 keep、非 easy、有释义的。只给这 3285 个补，其余 vocab_cards 里的词不用管。
- 其中 **1874 个**现在能从 note 里抽出片段（只有日文，质量参差），**1411 个**一条都没有。
- 单词卡数据：`assets/vocab_cards.json`，`cards[id] = [keep, reading, lemma, lemmaReading, pos, meaning, note, easy?]`，词头取 `heads[id]`，没有的话取 id 里 `-vocab-` 后面的部分，统一走 `KnowledgeRules.headOf`。`pos` 用中文标签。
- 例：`根負け`，note 是「常搭配「根負けする」表示僵持后认输。」。现有逻辑会抽出「根負けする」，但不带中文。

## 3. 定好的数据格式（新 asset，不改 vocab_cards.json 的列）

`assets/vocab_collocations.json`：

```json
{ "version": 1,
  "items": { "<vocabId>": [["約束を守る", "守约"], ["約束を破る", "违约、食言"], ["約束する", "名词＋する＝动词"]] } }
```

- 每个词 0–3 条。**写不出真正常用的搭配就不收这个词**（不要凑），卡上就不显示这一栏。
- 估算：约 2500 词 × 2.5 条 ≈ 200KB，可以进仓库（只有搭配，没有字幕原文）。
- 不单独加列，原因：vocab_cards 的第 8 列已经被 `easy` 占用，`check.py --install` 的流程也不用动。

## 4. 内容规矩（写进给 Antigravity 的提示词）

- 读者：中文母语初学者，汉字读音基本不会。
- **日文**：必须包含这个词本身或它的活用形（守る → 守って 不行，搭配写原形），长度 ≤ 12 字。要的是日语里真实高频的组合：名词 + 助词 + 动词（声をかける）、副词修饰（すっかり忘れる）、名词 + する、固定说法（お約束）。**不要抄动漫台词**，不要写整句。
- **中文**：≤ 10 个字。写这个组合的意思，不要只是复述单词的释义。语法提示（「名词＋する＝动词」）每个词最多一条。
- 拒收：模板话术（「常用于…」「表示…的意思」）、搭配里不含词头、中文和释义完全一样、一个词超过 3 条、半角括号混用。
- 用户的原则：**重复没关系，讲错不行**。拿不准的宁可不写。

## 5. 流程（照 `vocab-cards-v1` 的老套路）

工作目录 `../archive-content-sources/vocab-cards-v1/`（gitignore，**不能提交**）。可以参照的现成文件：`notes3_export.py`（分批导出 md）、`ANTIGRAVITY_NOTES3_PROMPT.md`（提示词的写法）、`check.py`（校验 + `--install`）。

1. 写 `colloc_export.py`：从 3285 个词里导出 `colloc/colloc_NN.md`，每批 100 词。每个词给出：词头、读音、词性、释义、现有 note（供参考）。按「note 里抽不出片段的」优先排。
2. 写 `ANTIGRAVITY_COLLOC_PROMPT.md`（第 4 节的规矩 + 输出格式：每批一个 `colloc_NN.json`，`{id: [[ja, zh], …]}`）。**先把提示词交给用户**，让 Antigravity 开始跑，再写下面的代码，两边并行（CLAUDE.md 0.7.0 的规矩）。
3. 写 `colloc_check.py`：按第 4 节逐条校验，打印拒收原因和覆盖率；加 `--install` 合并所有批次，写 `android-app/app/src/main/assets/vocab_collocations.json`。
4. 交回后：check 全过，再**人工抽查 40 个词**（看搭配是不是真常用、中文对不对），有问题的整批返工。
5. 是否改成主会话（强模型）自己写，或者 Antigravity 写完再由主会话复核全部条目，开工前问用户一句。知识卡那边用户定的是「强模型理解更深」，但搭配是 3285 个词的批量活，成本差很多。

## 6. App 这边要改的代码（数据装进去之后，约 1 个文件）

- `ui/words/Tango.kt`：新增 `TangoCollocations`（照同文件的 `TangoLines`：进程级缓存 + `peek()` + `load(context)`），在 `LabApplication` 的预读线程里一起读。`TangoRules.collocations(word)` 改成返回 `List<Pair<String, String>>`：先查新 asset，没有的话退回从 note 抽的片段（中文为空）。
- `ui/screens/review/TangoScreen.kt`，`TangoWordPage` 里「常一起出现」那一段：每条一行，日文用 `jpTitle` 17sp SemiBold，后面跟中文 13sp `ink2`，`verticalAlignment = Alignment.Bottom`（画布是 baseline 对齐）。
- 只做编译验证（`design/v3build.ps1 -Mode compile`），commit 并 push main。装入 asset 单独 commit 一次。全部做完后 `-Mode full`，然后发版（CLAUDE.md 第 6 节）并写 CHANGELOG。
- 做完在 `VOCAB_CARD_HANDOFF.md` 开头「还没做 ①」那条划掉，经验写回本文件。
