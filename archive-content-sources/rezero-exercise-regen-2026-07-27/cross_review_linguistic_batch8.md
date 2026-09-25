# Linguistic Batch 8 独立交叉复核与冻结报告

复核日期：2026-07-27  
复核范围：

- `regen/batch8_vocab_linguistic.json`：40 条词汇卡
- `regen/batch8_grammar_sentence_linguistic.json`：20 条语法卡、20 条句子卡
- 合计：80 条

## 冻结结论

| 类型 | 总数 | 原稿通过 | 修订后通过 | 拒绝 |
| --- | ---: | ---: | ---: | ---: |
| 词汇 | 40 | 36 | 4 | 0 |
| 语法 | 20 | 13 | 7 | 0 |
| 句子 | 20 | 16 | 4 | 0 |
| **合计** | **80** | **65** | **15** | **0** |

80 条均已完成独立逐卡复核并通过。15 条发生修订：7 条修正说话人、话轮边界、语境或语言学术语，8 条清除学习者正文中的 `source`、pattern、行号或旧释义等内部审校措辞；没有条目因证据不足而拒绝。

本报告冻结下列最终文件：

- `regen/batch8_vocab_linguistic.json`
- `regen/batch8_grammar_sentence_linguistic.json`

生成阶段报告 `regen/generation_qa_linguistic_batch8.md` 保留未改。该报告记录的是交叉审校前哈希；最终写库必须以下文“冻结哈希”为准。

## 人工交叉审校修订

### 词汇卡

| source_id | 修订要点 |
| --- | --- |
| `re-zero-vocab-嘆く` | EP3 lines 395–398 是昴的连续内心独白，不是帕克回顾昴。改正说话人，并把实际词面校正为「あれだけ嘆いて」。 |
| `re-zero-vocab-報酬` | 「その報酬が…」与下一句「割に合わねえ」同属昴的内心独白；改掉“帕克以交易框架计算”的错误归属。 |
| `re-zero-vocab-客人` | EP3 lines 421–422 中询问昴身柄、提出由「当家」按宾客接收的都是莱因哈鲁特，不是罗姆或艾米莉雅的随从；随后才由艾米莉雅决定自己带走昴。 |
| `re-zero-vocab-怪しい` | 明确 EP3 line 30 的「怪しいとは思ってたんだ」由菲鲁特说出，而 lines 31–32 的关系追问改由艾米莉雅说出，避免把相邻话轮并给同一说话人。 |

### 语法卡

| source_id | 修订要点 |
| --- | --- |
| `re-zero-s01e04-grammar-029` | 将学习者提示中的“源 pattern”改为直接说明全假名「したりない」容易被误切；继续按「し足りない」而非列举「たり」教学。 |
| `re-zero-s01e04-grammar-031` | 删除“源例”式审校话语，直接提醒必须把「と引き換えに」与后续「しても惜しくない」连读，才能看出极端假设而非已发生交易。 |
| `re-zero-s01e04-grammar-033` | 从学习者正文移除内部 `source_line_no` 和当前行号，改为说明完整表达中的「相当に」与「かしら」各自承担程度和人物语气。 |
| `re-zero-s01e04-grammar-039` | 将容易误导的“敬体进行形”改为「得意としています」中的「ている」在此描述稳定属性，并非正在进行的动作。 |
| `re-zero-s01e04-grammar-041` | 不再把数据库 pattern 错配写进学习者正文；直接教授「その可能性は｜かなり低い｜と思います」的主题「は」与引用「と」，并提醒不要误切为「とは」。 |
| `re-zero-s01e04-grammar-042` | EP4 lines 375–376 的说话人是罗兹瓦尔，不是艾米莉雅。改为说明他从自身身份出发承认似应干预，却用「だろう」「けど」悬置行动。 |
| `re-zero-s01e04-grammar-043` | EP4 line 377 是蕾姆回应罗兹瓦尔的上一句，不是回应艾米莉雅；保留「放っておいても」的让步分析并修正话轮关系。 |

### 句子卡

| source_id | 修订要点 |
| --- | --- |
| `re-zero-s01e06-sentence-005` | 删除“源中文”式内部比较，直接说明「ってとこか」使死因判断保持推测性。 |
| `re-zero-s01e06-sentence-006` | 删除“源释义”措辞；明确「襲撃者がいた」只断定存在袭击者，第二轮信息来自前一行「だが ２度目は」。 |
| `re-zero-s01e06-sentence-007` | 删除 `source`、当前字幕等内部审校措辞；仍明确目标是悬置残句，否定极性由下一行「俺の手の中にはねえ」提供。 |
| `re-zero-s01e06-sentence-011` | 删除“源中文”措辞，直接说明若只译“你没事吧”，会遗漏「って聞いていい」对发问是否得体的元语用询问。 |

## Source、occurrence、字幕与证据边界

### 词汇

- 40/40 个 `source_id` 精确映射到 `public.learning_vocab_items`。
- 40/40 均有 `public.learning_vocab_occurrences`；共 57 个分集 occurrence 记录，记录内 `occurrence_count` 合计 59。
- 每个词有 1–3 个分集 occurrence 记录，记录内合计次数范围也是 1–3。
- 逐项检索当前日语字幕并阅读相邻话轮，40/40 至少有一个真实词形命中；没有把 occurrence 的旧示例行号当作唯一证据。
- occurrence 表不是当前字幕的穷尽 concordance。例如「勘弁」的 EP15 记录行号发生漂移，但当前字幕可在更早的话轮直接命中；卡片没有把漂移行误作词面证据。
- 对说话人易混的 EP3 结尾独白、身柄安排和「怪しい」对话另行连读，修正见上表。

### 语法与句子

- grammar 20/20 精确映射 `public.learning_grammar_points`；sentence 20/20 精确映射 `public.learning_sentences`。
- 将 source 日文去除空白和标点后，在同集当前日语字幕至多连续 5 行窗口中复核：grammar 20/20 命中，sentence 20/20 命中。
- EP4 lines 122–125 连读确认：帕克先以「おはよう リア」对艾米莉雅说话，因此 line 124「君を失う」中的「君」确指艾米莉雅；line 125 才转到对昴的感谢。生成稿在此处原本正确，交叉复核没有误改。
- EP4 lines 362–377 连读确认观察、间谍评估、应否干预均属于罗兹瓦尔与蕾姆的话轮；据此修正 grammar-042、043。
- EP6 的残句、前句轮次、否定辖域和元语用问句均按连续字幕解释，不把数据库中的完整中文释义伪装成单行日文本身包含的信息。

## Enrichment 映射与数据库状态

只读 SQL 复算结果：

- 候选 80 条，`public.learning_card_enrichments` 命中 80/80。
- `id + source_type + source_id` 三元组精确匹配 80/80。
- source 表命中 80/80：vocab 40、grammar 20、sentence 20。
- 80/80 当前仍为旧提示版本 `linguistic-card-v1`。
- 按 UTF-8 二进制顺序计算，本地与数据库映射集合 MD5 均为 `1f8b2dd34656f29d902918904f30f827`。
- 与 `maintenance.regen_20260727_manifest` 的非空 `applied_at` 重叠：0。
- 与 manifest 中 `review_status = 'applied'` 的重叠：0。
- 本轮仅执行 `SELECT` 类查询；未执行 `INSERT`、`UPDATE`、`DELETE`、DDL、RPC 写入或迁移。

## 结构、去模板化与合并校验

最终候选校验：

```text
node regen/validate_candidates.mjs regen/batch8_vocab_linguistic.json regen/batch8_grammar_sentence_linguistic.json
```

- 2 个文件、80 条、80 个唯一 ID。
- errors：0；warnings：0。
- 类型数量为 vocab 40、grammar 20、sentence 20。
- 80/80 个唯一 `source_type + source_id`，每卡均有 3 个 domains、2 个 terms。

与既有候选合并校验：

- batch1–batch8：16 个文件、640 条、640 个唯一 ID。
- errors：0；warnings：0。
- batch8 与 batch1–7 的 ID 重叠：0；source 映射重叠：0。

去模板、泄漏和相似度检查：

- 用于比较的长度不低于 12 的教学文本叶：934。
- batch8 内完全相同长文本叶跨卡重复组：0。
- batch8 与 batch1–7 的完全相同长文本叶重复组：0。
- 归一化 28 字符连续片段跨卡复用组：0。
- 已知旧模板短语命中：0。
- 学习者 payload 内部 source、行号、prompt version、数据库、候选或模板审校元信息命中：0。
- 归一化整卡字符 4-gram Jaccard `>= 0.30`：0。
- 最大相似度为 0.1752，发生在本批 grammar-030 与既有 EP4 sentence-022；两卡共享「触らせてくれ」同一真实场景，分别分析许可使役与整句互动，不是模板复写。

上述脚本只用于 JSON 结构、集合映射、重复文本、相似度和泄漏检查；教学结论、说话人、语境、形态、句法、语义和语用均由人工逐卡复核。

## 冻结哈希

交叉审校前生成稿哈希：

| 文件 | SHA-256 |
| --- | --- |
| `regen/batch8_vocab_linguistic.json` | `4A676C1AB935939746113FFF2277B93112EBD6A806B6E3709C453CB73B349484` |
| `regen/batch8_grammar_sentence_linguistic.json` | `9ED737253C9DE82F1E6BD4C87DBC0BD7B1A54F8B87FFF5E4AC8661948140DDAE` |

最终冻结哈希：

| 文件 | SHA-256 |
| --- | --- |
| `regen/batch8_vocab_linguistic.json` | `4084CCB58B7BC435D29AF4A7E2C0E250C5BDB30C3C22311BAD6E63848FB6E6D0` |
| `regen/batch8_grammar_sentence_linguistic.json` | `77909522D1EDC57CBDB89A4CC23CDAD8E6011A9B5CB1BEC370088C6BB1A726D9` |
| `regen/generation_qa_linguistic_batch8.md`（保留未改） | `BE0B25E37E263BA8B2F8F389EB5D09E1DA84A917E264C0503E10F5C87BD1C181` |

## 最终状态

本批 80/80 已通过独立交叉复核，两个候选 JSON 自本报告所列最终校验与哈希起冻结，不再编辑。**可以进入数据库写入阶段。** 本复核任务本身没有执行任何数据库写入。
