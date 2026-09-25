# Linguistic batch8 生成侧 QA

## 结论

本批共 80 条：40 条 vocab、20 条 grammar、20 条 sentence。全部教学文本均在逐卡读取 source、occurrence 和当前日文字幕后重新手写；脚本只用于候选筛选、集合映射、结构校验、去重、相似度和模板泄漏检查，没有生成任何教学文字，也没有写数据库。

候选文件：

- `regen/batch8_vocab_linguistic.json`
- `regen/batch8_grammar_sentence_linguistic.json`

## 候选资格与数据库映射

筛选口径：

1. `linguistic_prompt_version = 'linguistic-card-v1'`。
2. 旧 `linguistic_payload` 非空。
3. 排除 batch1–7 的全部 ID；同时排除 `maintenance.regen_20260727_manifest` 中已经应用的 enrichment/source。
4. 展平旧 payload 的字符串叶，只统计长度不低于 12 的文本。
5. 该字符串叶至少跨 3 张仍然合格的 enrichment 卡重复。
6. 每张候选至少命中 5 个上述长重复叶，再人工确认这些重复文本确属套话。

排除 batch1–7 与已应用 manifest 后，合格池为 vocab 329 条、grammar 241 条、sentence 113 条。本批最终选定项的只读 SQL 复算结果：

| source_type | 数量 | enrichment 精确映射 | 旧提示版本 / payload | manifest 已应用 | 每卡重复长叶命中数 | 每卡最广命中叶跨卡数 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| vocab | 40 | 40 | 40 / 40 | 0 | 7–10 | 60–246 |
| grammar | 20 | 20 | 20 / 20 | 0 | 12–17 | 7–383 |
| sentence | 20 | 20 | 20 / 20 | 0 | 16 | 383 |
| **合计** | **80** | **80** | **80 / 80** | **0** | **≥7** | **≥7** |

人工确认的代表性旧套话包括：

- vocab：`形态构成 (Morphology)`、`结合上下文精确定位词义，拒绝机械对译。`、`词汇积累重在听说直觉与例句复现。`
- grammar：`形态学：词类构成与活用分析`、`句法管辖 (Syntactic Scope)`、`学习卡级别的语言学解析重在建立语言直觉，建议结合原声跟读。`
- sentence：`意群切分 (Chunking)`、`信息结构 (Information Structure)`、`句子卡片旨在培养影音语感与流利跟读能力。`

这些长句跨大量无关词项和台词原样重复，不能充当逐项语言学分析。

## Source、occurrence 与字幕

### vocab

- 40/40 精确映射到 `public.learning_vocab_items`。
- 40/40 均有 `public.learning_vocab_occurrences`。
- 合计 57 个分集 occurrence 记录，记录内 `occurrence_count` 合计 59。
- 单项覆盖 1–3 个分集记录、1–3 次 occurrence。
- 每项均另读真实日文字幕和相邻话轮；对应分集、当前行号与语境写入各卡 `review_note`，没有只相信汇总字段或旧 occurrence 行号。
- 候选初筛后还剔除了 `危うい`、`困る`、`嫉妬の魔女`、`届く`、`届ける`：其记录窗口未能支持预期词面，故没有为了凑数保留。

### grammar 与 sentence

- grammar 20/20 映射 `public.learning_grammar_points`。
- sentence 20/20 映射 `public.learning_sentences`。
- 对 source 日文去空白和标点后，在当前 EP4/EP6 日文字幕至多连续 5 行的窗口中复核：grammar 20/20 命中，sentence 20/20 命中。
- `review_note` 使用按日文文本重新定位的当前字幕行号，不把已有 `source_line_no` 当作唯一证据；跨行残句按连续话轮解释。

## 逐项复核中处理的源问题

### grammar

- `re-zero-s01e04-grammar-029`：真实句是「感謝しても したりない」；核心为「し足りない」的口语分段，不是列举用的「〜たり」。
- `re-zero-s01e04-grammar-030`：「触らせてくれ」在本场景是请求对方允许接触，不能机械解释成强迫对方做动作。
- `re-zero-s01e04-grammar-031`：完整范围包括「しても惜しくない」；这是用极端假设衡量价值，不是已经发生的交换。
- `re-zero-s01e04-grammar-041`：source pattern 标为「〜とは思います」，但字幕实际是「その可能性は かなり低いと思います」；「は」是给「その可能性」设主题，不属于「とは」结构。

### sentence

- `re-zero-s01e06-sentence-006`：「襲撃者がいた」本身没有“第二次”；第二次的信息来自相邻行「だが ２度目は」，卡片明确限制了证据边界。
- `re-zero-s01e06-sentence-007`：source 是「説明できる証拠も 未然に防ぐ手段も―」这一悬置残句，否定谓语在下一行「俺の手の中にはねえ」。
- `re-zero-s01e06-sentence-011`：「大丈夫って聞いていい？」是询问“可不可以问你‘没事吗’”的元语用许可，不只是直接问候。
- `re-zero-s01e06-sentence-017`：「お戻りになられました」是字幕中的实际双重敬语；说明同时区分了真实用例与规范上更简洁的「お戻りになりました」。
- `re-zero-s01e06-sentence-019`：「かねない」表示不希望的结果有发生可能，不表示“不可能”。
- `re-zero-s01e06-sentence-020`：「てみせる」表达把行动实现给对方看的决意，不等于单纯“试试看”。

## 结构、去重与泄漏检查

运行：

```text
node regen/validate_candidates.mjs regen/batch8_vocab_linguistic.json regen/batch8_grammar_sentence_linguistic.json
```

结果：

- files：2
- rows：80
- unique IDs：80
- errors：0
- warnings：0
- 80/80 均含非空 `headlineZh`、3 个 `domains`、2 个 `terms`、`historicalNoteZh`、`cautionZh` 和 `review_note`
- 批内 ID 重复：0；批内 `source_type + source_id` 重复：0
- 与 batch1–7 共 560 张已有语言学卡相比，ID 重复：0；source 重复：0
- batch8 内长度不低于 12 的完全相同教学字符串叶跨卡重复组：0
- batch8 与 batch1–7 的完全相同长教学字符串叶重复组：0
- 单个字符串叶内归一化 28 字符连续片段跨卡复用组：0
- 已知旧模板短语命中：0
- payload 内 source ID / source type / prompt version / 模板元信息泄漏：0
- 归一化整卡字符 4-gram Jaccard `>= 0.30`：0
- 最大相似度：0.0573，发生在 vocab「危うく」与 grammar `re-zero-s01e04-grammar-028`；两卡共享该场景真实表达，分析对象分别是副词与险些发生构式，不是模板复写

## 文件哈希

- `batch8_vocab_linguistic.json`：`4A676C1AB935939746113FFF2277B93112EBD6A806B6E3709C453CB73B349484`
- `batch8_grammar_sentence_linguistic.json`：`9ED737253C9DE82F1E6BD4C87DBC0BD7B1A54F8B87FFF5E4AC8661948140DDAE`

## 状态

生成侧校验通过，两个 JSON 已按上述 SHA-256 冻结，可进入独立交叉审校。本报告只记录生成侧结果，不代表 reviewer 的结论；本轮没有执行任何数据库写入。
