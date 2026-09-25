# Linguistic batch6 生成侧 QA

## 范围

- `batch6_vocab_linguistic.json`：40 条 vocab。
- `batch6_grammar_sentence_linguistic.json`：20 条 grammar、20 条 sentence。
- 本轮只执行只读 SQL、文件结构检查、映射、去重和泄漏检查；没有写数据库。

## 候选资格与映射

| source_type | 数量 | enrichment 精确映射 | manifest 已应用 | 重复长叶命中范围 |
| --- | ---: | ---: | ---: | ---: |
| vocab | 40 | 40 | 0 | 5–10 |
| grammar | 20 | 20 | 0 | 14 |
| sentence | 20 | 20 | 0 | 13–16 |

80 条候选均满足：

- `linguistic_prompt_version = linguistic-card-v1`
- `linguistic_payload is not null`
- 未出现在 `maintenance.regen_20260727_manifest` 的已应用记录中
- 每卡至少命中 5 个长度不低于 12、且跨至少 3 卡重复的字符串叶

输出文件中的 `id + source_type + source_id` 与 `public.learning_card_enrichments` 精确匹配 80/80。

## Source、occurrence 与字幕

- 40/40 vocab 均能映射 `learning_vocab_items`。
- 40/40 vocab 均有 `learning_vocab_occurrences`；共 93 个分集记录、232 次 occurrence。
- 20/20 grammar 的 `ja_example` 均在当前 EP3 日文字幕中命中；跨行例用最多连续 5 行归一化复核。
- 20/20 sentence 的 `ja_text` 均在当前 EP5 日文字幕中命中。
- `source_line_no` 存在系统性旧偏移，因此 `review_note` 使用全文重新定位后的当前字幕行，不把旧行号直接当证据。

## 内容结构

- 总数 80，类型分布 40/20/20。
- 80/80 均含 `headlineZh`、至少 3 个 `domains`、至少 2 个 `terms`、`historicalNoteZh`、`cautionZh` 和 `review_note`。
- ID、source ID 均无批内重复。
- 与 batch1–5 共 400 张既有语言学卡相比，ID 重复 0、source ID 重复 0。

## 去模板与泄漏

- batch6 内长度不低于 12 的完全相同字符串叶跨卡重复：0。
- batch6 与 batch1–5 之间的完全相同长字符串叶：0。
- 已知旧模板短语命中：0，包括 `形态构成 (Morphology)`、`句法管辖 (Syntactic Scope)`、`语用情态 (Pragmatic Force)`、`意群切分 (Chunking)` 及旧版通用学习建议。
- 28 字符连续片段的批内复用抽查：0。

## 生成时处理的源项问题

- `vocab-re-zero-sawaru`：源卡把「触る」误写成“别碰／命令表达”；候选恢复为五段动词“触摸”，把禁止义限定在「触るな」。
- `re-zero-s01e03-grammar-48`：pattern 是「ということは」，存储例句却是「ということです」；候选明确列出 EP3 连续两句的前提与结论结构。
- `re-zero-s01e03-grammar-50`：源 pattern 将条件形和工具格机械合并为「ば〜で」；候选恢复牙→爪→骨→命的省略谓语链。
- `re-zero-s01e03-grammar-60`：源例「そうみたいなもんだ」是角色犹疑口语；候选连读后续「爺ちゃんみてえなもんだ」校准比较对象。
- 多条 sentence 的中文源义包含相邻字幕行内容；候选在 `review_note` 中显式标注跨行来源，不将补入内容伪装为逐字直译。

## 结论

batch6 两个 JSON 文件通过生成侧结构、资格、映射、字幕、去重与模板泄漏检查，可进入独立交叉复核；本报告不代表复核者结论。
