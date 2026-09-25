# Linguistic batch7 生成侧 QA

## 结论

本批共 80 条：40 条 vocab、20 条 grammar、20 条 sentence。全部教学文本均在逐卡读取 source、occurrence 和当前日文字幕后手写；脚本仅用于候选筛选、JSON 结构、映射、去重、相似度与模板泄漏检查，没有生成任何教学内容，也没有写数据库。

候选文件：

- `regen/batch7_vocab_linguistic.json`
- `regen/batch7_grammar_sentence_linguistic.json`

## 候选资格与数据库映射

筛选口径：

1. `linguistic_prompt_version = 'linguistic-card-v1'`。
2. 旧 `linguistic_payload` 非空。
3. 排除 `maintenance.regen_20260727_manifest` 中已经应用的 enrichment/source。
4. 展平旧 payload 的字符串叶，仅保留长度不低于 12 的文本。
5. 该字符串叶至少跨 3 张 enrichment 卡重复。
6. 每张候选至少命中 5 个上述长重复叶，并人工确认这些重复内容是明确套话。

只读 SQL 复算结果：

| source_type | 数量 | enrichment 精确映射 | 旧提示版本 | manifest 已应用 | 每卡重复长叶命中数 | 所命中叶的最大跨卡数 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| vocab | 40 | 40 | 40 | 0 | 5–10 | 79–422 |
| grammar | 20 | 20 | 20 | 0 | 12–17 | 12–422 |
| sentence | 20 | 20 | 20 | 0 | 16 | 422 |
| **合计** | **80** | **80** | **80** | **0** | **≥5** | **≥12** |

人工确认的代表性旧套话包括：

- vocab：`形态构成 (Morphology)`、`结合上下文精确定位词义，拒绝机械对译。`、`词汇积累重在听说直觉与例句复现。`
- grammar：`形态学：词类构成与活用分析`、`句法管辖 (Syntactic Scope)`、`学习卡级别的语言学解析重在建立语言直觉，建议结合原声跟读。`
- sentence：`意群切分 (Chunking)`、`信息结构 (Information Structure)`、`句子卡片旨在培养影音语感与流利跟读能力。`

这些文本跨大量无关词项和台词完全重复，不能作为逐项语言学分析。

## Source、occurrence 与字幕

### vocab

- 40/40 均精确映射到 `public.learning_vocab_items`。
- 40/40 均有 `public.learning_vocab_occurrences`。
- 合计 60 个分集 occurrence 记录、108 次 occurrence。
- 单项覆盖 1–3 个分集记录、1–4 次 occurrence。
- 每项均另读实际日文字幕及相邻行；对应分集、行号和语境写入各卡 `review_note`。

### grammar 与 sentence

- grammar 20/20 映射 `public.learning_grammar_points`。
- sentence 20/20 映射 `public.learning_sentences`。
- 对 source 日文去空白和标点后，在当前 EP3/EP4 字幕至多连续 5 行窗口中复核：grammar 20/20 命中，sentence 20/20 命中。
- `review_note` 使用当前字幕重新定位后的行号，不把旧 `source_line_no` 当作唯一证据。

## 逐项复核中处理的源问题

- `re-zero-s01e04-grammar-007`：真实句为「見つけなきゃ出れない」，先是缩约条件“找不到就出不去”，义务只是由后果推导，不能机械标成既定义务句。
- `re-zero-s01e04-grammar-013`：「私室かしら」在自我介绍书库兼卧室时接近人物化的柔化断言，并非真正向听者求知。
- `re-zero-s01e04-grammar-016`：源 pattern 误标为「〜のは確か」；字幕实际是「敵意がないのは 確かめられた」，核心谓语为动词「確かめられた」。
- `re-zero-s01e04-grammar-024`：「助けてくれたじゃない」是提醒双方承认肯定事实，不是否定“没有救”。
- `re-zero-s01e03-sentence-022`：「マナ切れで消えちゃう」陈述当前因果，不含显式假设条件。
- `re-zero-s01e03-sentence-023`：source 只收「僕は契約に従う」；当前字幕完整行还含条件「君に何かあれば」。
- `re-zero-s01e03-sentence-027`：source 只收前半；当前字幕完整行以「から」连接「その間に お前は全力で逃げろ」。
- `re-zero-s01e03-sentence-033`：「危ないところだった」表示刚才处在险境临界点，不是“危险的地点”。

## 结构、去重与泄漏检查

运行：

```text
node regen/validate_candidates.mjs regen/batch7_vocab_linguistic.json regen/batch7_grammar_sentence_linguistic.json
```

结果：

- files：2
- rows：80
- unique IDs：80
- errors：0
- warnings：0
- 80/80 均含非空 `headlineZh`、3 个 `domains`、2 个 `terms`、`historicalNoteZh`、`cautionZh` 和 `review_note`
- 批内 ID 重复：0；批内 `source_type + source_id` 重复：0
- 与 batch1–6 共 480 张既有语言学卡相比，ID 重复：0；source 重复：0
- batch7 内长度不低于 12 的完全相同教学字符串叶跨卡重复组：0
- batch7 与 batch1–6 的完全相同长教学字符串叶重复组：0
- 单个字符串叶内归一化 28 字符连续片段跨卡复用组：0
- 已知旧模板短语命中：0
- payload 内部 source ID / 模板元信息泄漏：0
- 归一化整卡字符 4-gram Jaccard `>= 0.30`：0
- 最大相似度：0.2042，发生在「家事」与「全般」；两卡因同一字幕明确构成上下位语义关系而共享少量词汇，但分析焦点分别是家务总类和范围覆盖，并非模板复写

## 文件哈希

- `batch7_vocab_linguistic.json`：`B2D2909AA92C47928F7B92879E0AC3943BF38126EA5C509FFB3DFA283AF5B37C`
- `batch7_grammar_sentence_linguistic.json`：`19F315D4670C6F00D4768B037D4A2EECDF8AB1B08F155279C8BB2F6D3850F1E8`

## 状态

生成侧校验通过，两个 JSON 已冻结，可进入独立交叉审校。本报告只记录生成侧结果，不代表审校者结论；本轮没有执行任何数据库写入。
