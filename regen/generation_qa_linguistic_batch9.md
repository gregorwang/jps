# Linguistic batch9 生成侧 QA

## 结论

本批共 80 条：40 条 vocab、20 条 grammar、20 条 sentence。全部教学内容均在逐条读取数据库 source、vocab occurrence、当前日文字幕及相邻话轮后重新手写；JavaScript 只用于读取候选、结构校验、去重、相似度和泄漏检查，没有生成任何教学文字。本轮只执行 Supabase 只读 SQL，没有写数据库。

候选文件：

- `regen/batch9_vocab_linguistic.json`
- `regen/batch9_grammar_sentence_linguistic.json`

## 候选资格与数据库映射

筛选口径：

1. `linguistic_prompt_version = 'linguistic-card-v1'`。
2. 旧 `linguistic_payload` 非空。
3. 排除 `maintenance.regen_20260727_manifest` 中所有 `applied_at is not null` 的 enrichment；Batch8 已包含在本次排除集合。
4. 展平仍合格旧 payload 的字符串叶，只统计长度不低于 12 的文本。
5. 字符串叶至少跨 3 张仍未应用的 enrichment 重复；每张候选须命中不少于 5 个此类长重复叶，再人工确认内容确属套话。

排除全部已应用记录后，高置信重复模板池为：

| source_type | 合格池 |
| --- | ---: |
| vocab | 289 |
| grammar | 219 |
| sentence | 93 |

最终 80 条的只读复算：

| source_type | 数量 | enrichment 三元组精确映射 | 旧提示版本 / payload | source 存在 | manifest 已应用 | 每卡重复长叶命中 | 命中叶最大跨卡数 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| vocab | 40 | 40 | 40 / 40 | 40 | 0 | 10 | 230 |
| grammar | 20 | 20 | 20 / 20 | 20 | 0 | 17 | 348 |
| sentence | 20 | 20 | 20 / 20 | 20 | 0 | 16 | 348 |
| **合计** | **80** | **80** | **80 / 80** | **80** | **0** | **≥10** | **≥230** |

旧内容中跨大量无关学习项复现的代表性套话仍包括：

- vocab：`形态构成 (Morphology)`、`结合上下文精确定位词义，拒绝机械对译。`、`词汇积累重在听说直觉与例句复现。`
- grammar：`形态学：词类构成与活用分析`、`句法管辖 (Syntactic Scope)`、`学习卡级别的语言学解析重在建立语言直觉，建议结合原声跟读。`
- sentence：`意群切分 (Chunking)`、`信息结构 (Information Structure)`、`句子卡片旨在培养影音语感与流利跟读能力。`

这些句子原样跨数十至数百张卡复现，不能作为具体学习项的语言学分析。

## Source、occurrence 与当前字幕

### vocab

- 40/40 精确映射 `public.learning_vocab_items`。
- 40/40 均存在 `public.learning_vocab_occurrences`。
- 共 40 个 occurrence 分集记录，记录内 `occurrence_count` 合计 49；每项恰有 1 个分集记录。
- 当前日文字幕支持 40/40：EP5 18 项、EP6 20 项、EP7 2 项。
- 实际核验使用词面或可靠活用词干，而非机械相信旧 `example_line_nos`。例如：
  - `執念深い` 的字幕实际形态是 EP5 line 85「執念深さ」。
  - `手のひらの上で踊る` 的字幕实际形态是 EP6 line 457「手のひらの上で 踊らされた」。
  - `務まる`、`寄る`、`惜しい`、`憎む`、`手を抜く` 均按真实活用定位。
- 每张卡的 `review_note` 记录当前字幕分集、行号和必要的相邻话轮。

### grammar

- 20/20 精确映射 `public.learning_grammar_points`，均来自 EP5。
- 对 source `ja_example` 去除空白和标点后，在当前日文字幕至多连续 5 行窗口中命中 20/20；本批 20 项实际都可在单行内定位。
- 逐条重新判断真实结构，不接受旧 pattern 与表面形式冲突：
  - `grammar-001` 的「動かれて」在客人护理语境中是尊敬形，再接「てはいけません」，不是普通受害被动。
  - `grammar-002` 的真实台词是「どうして戻ったんだ…」；核心是疑问词与解释性「んだ」，旧 pattern「〜のか」未出现在 source。
  - `grammar-011` 的「前回とは条件が違う」是比较对象「と」叠加对比「は」，不是“所谓”的定义句。
  - `grammar-019` 的「って」需要连读下一行双胞胎大慌张奔走，不能在破折号处截断证据。

### sentence

- 20/20 精确映射 `public.learning_sentences`，均来自 EP4。
- 19/20 对 source 日文去除空白和标点后，在当前字幕至多连续 5 行窗口中精确命中。
- `sentence-028` 是唯一的机械归一化例外，但人工证据完整：source「私が この屋敷の当主 ロズワール･Ｌ･メイザースというわけだよ」对应当前 EP4 lines 176–177「私が この屋敷の当主― / ロズワール･Ｌ･メイザース というわぁーけだよ」。差异仅为字幕跨行与罗兹瓦尔角色拉长音，语义和句法连续，故当前字幕证据仍为 20/20。
- 生成阶段主动放弃了初选的 `sentence-018` 和 `sentence-025`：两句真实台词已经分别被旧批次中的另一 source 类型或另一分集同句覆盖，保留会提高跨批内容复写风险；最终改选 `sentence-038`、`sentence-039`。
- 关键指称和边界：
  - `sentence-020` 中帕克先说「おはよう リア」，所以「君を失う」的「君」是艾米莉雅；下一行才切换到感谢昴。
  - `sentence-023` 由帕克评价昴“竟然是真心说的”，下一行才转向「リア」。
  - `sentence-031` 以「ので」结尾，但后件在前一句「当家の食卓は レムが預かっています」，属于后置理由，不是无意义残句。

## 人工重写中的语言学边界

- `惜しい` 在「手間も惜しい」中是连杀人的工夫都不值得花，并非“差一点、可惜”。
- `得物` 在战斗语境指武器，需与同读的「獲物」区分。
- `寒気` 本项读「さむけ」，不能误取气象词的「かんき」。
- `心外` 包含被误解或冒犯后的不服，不是中性的“意外”。
- `手を抜かない` 在战斗中表示不放水；不能只套用工作上的“偷工减料”。
- `sentence-005` 保留真实口语「出れない」，同时明确它是「出られない」的ら抜き形。
- `sentence-012` 中句末「姉様」是雷姆对拉姆的呼语，醒来的人是昴。
- `sentence-030` 的「食卓を預かる」是负责伙食，不是保管实体餐桌。

## 结构、去重与泄漏检查

运行：

```text
node regen/validate_candidates.mjs regen/batch9_vocab_linguistic.json regen/batch9_grammar_sentence_linguistic.json
```

结果：

- files：2
- rows：80
- unique IDs：80
- errors：0
- warnings：0
- 80/80 均含 3 个 `domains`、2 个 `terms`、非空 `headlineZh`、`historicalNoteZh`、`cautionZh` 与 `review_note`
- 批内 `id` 重复：0；批内 `source_type + source_id` 重复：0
- 与 batch1–8 共 640 张已有语言学卡相比，ID 重复：0；source 重复：0
- batch9 内长度不低于 12 的完全相同教学字符串叶：0
- batch9 与 batch1–8 的完全相同长教学字符串叶：0
- 归一化 28 字符连续片段在 batch9 批内或跨 batch1–8 复用：0
- 已知旧模板短语命中：0
- payload 内 source ID、source type、prompt version、行号或模板元信息泄漏：0
- 归一化整卡字符 4-gram Jaccard `>= 0.30`：0
- 最大跨批相似度：0.1963，发生在本批 `sentence-031` 与旧 `grammar-038`；两卡共享同一场景的后置理由，分别分析整句与语法点，没有复写教学文字

全量复核：

```text
node regen/validate_candidates.mjs regen/batch{1..9}_vocab_linguistic.json regen/batch{1..9}_grammar_sentence_linguistic.json
```

- files：18
- rows：720
- unique IDs：720
- errors：0
- warnings：0

## 文件哈希

- `batch9_vocab_linguistic.json`：`2911374E73949B45E29FEB150744576AD08BBB9B788503C8D745F8909CD21645`
- `batch9_grammar_sentence_linguistic.json`：`3AE2B816C484479684A35A24B94C8F05E045457F47B2BC9FD1FC77B7AB71E354`

## 状态

生成侧 QA 通过，两个 JSON 已按上述 SHA-256 冻结，可释放给独立 reviewer。该结论不代表交叉审校已经完成；本轮没有执行任何数据库写入。
