# Linguistic Batch 9 独立交叉复核与冻结报告

复核日期：2026-07-27  
复核范围：

- `regen/batch9_vocab_linguistic.json`：40 条词汇卡
- `regen/batch9_grammar_sentence_linguistic.json`：20 条语法卡、20 条句子卡
- 合计：80 条

## 冻结结论

| 类型 | 总数 | 原稿通过 | 修订后通过 | 拒绝 |
| --- | ---: | ---: | ---: | ---: |
| 词汇 | 40 | 33 | 7 | 0 |
| 语法 | 20 | 15 | 5 | 0 |
| 句子 | 20 | 14 | 6 | 0 |
| **合计** | **80** | **62** | **18** | **0** |

80 条均已逐卡复核并通过。18 条发生最小修订，范围包括说话人、指称与场景边界，形态／句法术语，语义蕴含，以及学习者正文中的内部审校措辞；没有条目因证据不足而拒绝。

本报告冻结下列最终文件：

- `regen/batch9_vocab_linguistic.json`
- `regen/batch9_grammar_sentence_linguistic.json`

生成阶段报告 `regen/generation_qa_linguistic_batch9.md` 保留未改；其中哈希对应交叉审校前原稿。最终写库必须以下文“冻结哈希”为准。

## 人工交叉审校修订

### 词汇卡

| source_id | 修订要点 |
| --- | --- |
| `re-zero-vocab-務まる` | `務まる` 是自动词表达的胜任判断，不是形态上的可能形；将“よく＋可能”改为“よく＋胜任判断”的反预期评价。 |
| `re-zero-vocab-所有物` | EP5 浴场话轮只有昴与罗兹瓦尔。三次 `所有物` 均由罗兹瓦尔说出，line 124 的 `ガブリ` 是昴直接反击，随后罗兹瓦尔评价 `躊躇ないな`；原稿误写成拉姆咬人，已修正正文与 review note。 |
| `re-zero-vocab-吸い出す` | `吸い出す` 编码从内部向外抽离，但不自行蕴含“取空／取尽”；完成程度仍由数量与上下文决定。 |
| `re-zero-vocab-寄る` | 原句只支持告别时邀请“以后有事随时再来”，没有足够证据把目的地限定为艾米莉雅自己的房间附近，已收紧证据边界。 |
| `re-zero-vocab-寒気` | 真实词组是 `あの寒気`，不是 `あと寒気`；`あの` 才是把寒意回指到先前身体体验的指示词。同步清理“学习卡给出读音”的内部措辞。 |
| `re-zero-vocab-得物` | 将“字幕汉字”改为词形本身与 `相手の面` 的并列证据，继续明确本句是武器义 `得物`，不是猎物义 `獲物`。 |
| `re-zero-vocab-手のひらの上で踊る` | 去掉“字幕实际／真实台词”等内部审校口吻，直接说明本场景使用使役被动 `踊らされた`，不可误读为主动 `踊った`。 |

### 语法卡

| source_id | 修订要点 |
| --- | --- |
| `re-zero-s01e05-grammar-002` | 删除旧 pattern、source、字幕等内部元信息；直接说明本句是 `戻ったんだ`，疑问性由 `どうして` 与语调承担，不能擅自补成 `のか`。 |
| `re-zero-s01e05-grammar-004` | `ノックもしないで入り込んで―` 与后续 `随分と無礼なやつ` 属于碧翠丝同一连续发话；把错误的“下一话轮”改为后续评价补全。 |
| `re-zero-s01e05-grammar-013` | `死ぬだの生きるだの` 的总评由碧翠丝自己的后续话语承接，不是切换到下一说话人；修正话轮术语。 |
| `re-zero-s01e05-grammar-014` | 将“前两行／前面话轮”改为“前文／前述内容”，准确描述 `あげくに` 的跨句回指，不把字幕切行误当话轮切换。 |
| `re-zero-s01e05-grammar-019` | 将“字幕行结束”改为表面停顿后的句法悬置；仍要求把后续双胞胎奔走的结果一并读入。 |

### 句子卡

| source_id | 修订要点 |
| --- | --- |
| `re-zero-s01e04-sentence-005` | `見つけなきゃ 出れない` 在本句是 `見つけなければ` 引出的否定必要条件，不是独立义务表达；标题改为“否定条件缩约”。 |
| `re-zero-s01e04-sentence-013` | 将“字幕标点”改为停顿、断句与语调，避免把内部字幕载体写成语言学证据本身。 |
| `re-zero-s01e04-sentence-017` | 精确重写关系从句论元：省略主语是昴，`私のこと` 填补 `知らない` 的宾语空位；整个 `私のことを` 又是外层 `助けてくれた` 的宾语。 |
| `re-zero-s01e04-sentence-028` | 保留 EP4 lines 176–177 的同一话轮和罗兹瓦尔拉长音证据，去掉 `source`、文本匹配等内部审校措辞，改为直接教授跨段同句与表演性韵律。 |
| `re-zero-s01e04-sentence-031` | `ので` 是雷姆在前一句结论后追加的理由，不是“数据库漏行”，也不是切到前一话轮；已改为同一说话人的后置原因说明。 |
| `re-zero-s01e04-sentence-038` | `雇う` 不是使役结构；`俺を` 是 `雇う` 的直接宾语／被雇者，`この屋敷で` 是工作地点。 |

## Source、occurrence、字幕与证据边界

### 数据库映射

最终候选的只读 SQL 复算结果：

- 候选 80 条，`public.learning_card_enrichments` 三元组 `id + source_type + source_id` 精确命中 80/80。
- source 表命中 80/80：`learning_vocab_items` 40、`learning_grammar_points` 20、`learning_sentences` 20。
- 80/80 当前仍为旧提示版本 `linguistic-card-v1`。
- 本地与数据库映射集合按 UTF-8／`C` 顺序计算的 MD5 均为 `fd994453b2b667d4998d87675ed087ff`。
- 与 `maintenance.regen_20260727_manifest` 的非空 `applied_at` 重叠：0。
- 与 manifest 中 `review_status = 'applied'` 的重叠：0。

### 词汇 occurrence 与实际词形

- 40/40 词汇均有 `learning_vocab_occurrences`。
- 共 40 个分集 occurrence 记录，记录内 `occurrence_count` 合计 49；每个词项恰有 1 个分集记录。
- 当前日语字幕支持 40/40：EP5 18 项、EP6 20 项、EP7 2 项。
- occurrence 的旧 `example_line_nos` 与当前 `subtitle_lines` 存在多处偏移，因此没有机械采用旧行号，而是按真实词面、可靠活用词干与连续上下文重新定位。
- 形态变化边界另行核对：`執念深い` 实际出现为 `執念深さ`，`手のひらの上で踊る` 实际出现为 `踊らされた`；`務まる`、`寄る`、`惜しい`、`憎む`、`手を抜く` 均按真实活用与搭配解释。

### 语法、句子与说话人

- grammar 20/20 精确映射 EP5 source；去除空白与标点后，20/20 source 日文均能作为同集当前字幕单行中的连续文本命中。
- sentence 20/20 精确映射 EP4 source；19/20 可按同样方式在单行内命中。
- 唯一单行例外 `sentence-028` 的证据完整：source 标准写法对应 EP4 lines 176–177 `私が この屋敷の当主― / ロズワール･Ｌ･メイザース というわぁーけだよ`。差异只是跨行显示与角色拉长音。
- 说话人判断以连续日文话轮、呼语、应答链、授受方向和角色语尾共同确定，不按单行字符串或旧 source 行号机械归属。
- 重点边界复核包括：
  - EP5 lines 115–124 的浴场场景是昴与罗兹瓦尔，`ガブリ` 的反击者不是拉姆。
  - EP4 line 124 `君を失う` 的 `君` 由前一行 `おはよう リア` 锁定为艾米莉雅；下一行才转向感谢昴。
  - EP4 line 136 是帕克评价昴的真心发言；下一行才转向 `リア`。
  - EP4 line 186 的 `ので` 回接雷姆自己上一句的结论，不是缺失主句。
  - EP5 lines 39–41 的 `だの`、总评和 `あげくに` 均属于碧翠丝连续发话，字幕切行不等于话轮切换。

## 结构、去模板化与合并校验

最终候选校验：

```text
node regen/validate_candidates.mjs regen/batch9_vocab_linguistic.json regen/batch9_grammar_sentence_linguistic.json
```

- files：2
- rows：80
- unique IDs：80
- errors：0
- warnings：0
- 类型数量：vocab 40、grammar 20、sentence 20
- 80/80 个唯一 `source_type + source_id`

全量候选校验：

```text
node regen/validate_candidates.mjs regen/batch{1..9}_vocab_linguistic.json regen/batch{1..9}_grammar_sentence_linguistic.json
```

- files：18
- rows：720
- unique IDs：720
- errors：0
- warnings：0
- batch9 与 batch1–8 的 ID 重叠：0
- batch9 与 batch1–8 的 source 映射重叠：0

去模板、泄漏和相似度检查：

- 排除固定 `domain` 枚举值后，batch9 长度不低于 12 的教学文本叶为 1,011 个，1,011 个均唯一。
- batch9 内完全相同长教学文本叶跨卡重复组：0。
- batch9 与 batch1–8 的完全相同长教学文本叶重复：0。
- 归一化 28 字符连续片段在 batch9 内复用：0；与 batch1–8 交叉复用：0。
- 已知旧模板短语命中：0。
- 学习者 payload 中 source ID、source type、prompt version、数据库、候选、模板或行号等内部审校元信息命中：0。
- 归一化整卡字符 4-gram Jaccard `>= 0.30`：0。
- 最大相似度为 `0.1984`，发生在本批 `sentence-031` 与既有 `grammar-038`；两卡共享同一场景的后置理由结构，但教学文字没有复写。

上述脚本只用于选择、结构、集合映射、重复文本、泄漏与相似度 QA；所有教学文字修订以及说话人、形态、句法、语义和语用判断均由人工逐卡完成，没有使用 Python、JavaScript 或 SQL 模板生成教学内容。

## 冻结哈希

交叉审校前生成稿哈希：

| 文件 | SHA-256 |
| --- | --- |
| `regen/batch9_vocab_linguistic.json` | `2911374E73949B45E29FEB150744576AD08BBB9B788503C8D745F8909CD21645` |
| `regen/batch9_grammar_sentence_linguistic.json` | `3AE2B816C484479684A35A24B94C8F05E045457F47B2BC9FD1FC77B7AB71E354` |

独立交叉审校后的最终冻结哈希：

| 文件 | SHA-256 |
| --- | --- |
| `regen/batch9_vocab_linguistic.json` | `6AC296DFAB7F042D2D3A19B404A4C72826D8008A2A77CC263F66FE3D5082E07F` |
| `regen/batch9_grammar_sentence_linguistic.json` | `A06C3BA22E265ED24968280508DF443E3A0888BFC9E2ECA67B06BC7536E9F2C1` |

## 数据库写入状态

本轮仅执行 Supabase `SELECT` 类只读查询；未执行 `INSERT`、`UPDATE`、`DELETE`、DDL、RPC 写入或迁移，数据库写入数为 0。

## 最终状态

**独立交叉审校通过。上述两个 JSON 已按最终 SHA-256 冻结，可进入写库阶段；本报告本身没有执行写库。**
