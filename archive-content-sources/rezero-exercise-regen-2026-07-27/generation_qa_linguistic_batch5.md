# Linguistic Batch 5 生成侧 QA（交叉复核前快照）

> 本报告保留生成侧提交时的 QA 结果与文件哈希；交叉复核后的结论、修正和新哈希见 `cross_review_linguistic_batch5.md`。

## 结论

本批共 80 条，均为逐卡读取源项、occurrence 与真实日文字幕上下文后手写；没有使用 Python 或文本模板生成教学内容，也没有写入数据库。

| 类型 | 条数 | 旧提示版本 | manifest 已应用 | 源项存在 | enrichment ID 精确匹配 |
|---|---:|---:|---:|---:|---:|
| vocab | 40 | 40 | 0 | 40 | 40 |
| grammar | 20 | 20 | 0 | 20 | 20 |
| sentence | 20 | 20 | 0 | 20 | 20 |
| **合计** | **80** | **80** | **0** | **80** | **80** |

候选文件：

- `regen/batch5_vocab_linguistic.json`
- `regen/batch5_grammar_sentence_linguistic.json`

## 候选与重复证据

筛选口径：

1. `linguistic_prompt_version = 'linguistic-card-v1'`。
2. 排除 `maintenance.regen_20260727_manifest` 中 `public.learning_card_enrichments` 已有非空 `applied_at` 的记录。
3. 展平旧 `linguistic_payload` 的字符串叶；叶长至少 12 字符。
4. 同一叶至少跨 3 张不同 enrichment 卡出现。
5. 每张候选卡至少命中 5 个不同的上述长重复叶。

最终证据范围：

| 类型 | 每卡重复长叶种类 | 该卡所命中叶的最大跨卡数 |
|---|---:|---:|
| vocab | 5–10 | 78–497 |
| grammar | 14 | 497 |
| sentence | 13–16 | 174–497 |

人工抽查并确认这些叶确属套话，而非多个词条合理共享的知识。代表例：

- grammar：`形态学：词类构成与活用分析`、`其核心语义在于传达说话人对动作、状态或因果关系的心理评价与判断。`、`学习卡级别的语言学解析重在建立语言直觉，建议结合原声跟读。`
- sentence：`意群切分 (Chunking)`、`句子卡片旨在培养影音语感与流利跟读能力。`、`结合《Re:Zero》当集情境，体会角色在特定心理高潮期的表达特色。`
- vocab：`结合上下文精确定位词义，拒绝机械对译。`、`词汇积累重在听说直觉与例句复现。`、`学习时把形式、语义和使用场景一起记。`

## ID / source 映射

### grammar

- enrichment：`grammar-card-v1:re-zero-s01e03-grammar-24` ～ `grammar-card-v1:re-zero-s01e03-grammar-43`
- source：`re-zero-s01e03-grammar-24` ～ `re-zero-s01e03-grammar-43`
- 20/20 按固定前缀一一映射。

### sentence

- enrichment：`sentence-card-v1:re-zero-s01e05-sentence-010` ～ `sentence-card-v1:re-zero-s01e05-sentence-029`
- source：`re-zero-s01e05-sentence-010` ～ `re-zero-s01e05-sentence-029`
- 20/20 按固定前缀一一映射。

### vocab

40/40 的 `source_id` 均存在于 `learning_vocab_items`，且每项都有 occurrence。32 项使用标准映射 `vocab-card-v1:<source_id>`；8 项沿用数据库现有的非标准 enrichment ID：

| enrichment ID | source ID |
|---|---|
| `enrich-vocab-re-zero-vocab-誓う-v1` | `re-zero-vocab-誓う` |
| `enrich-vocab-vocab-re-zero-jitai-vocab-card-v1` | `vocab-re-zero-jitai` |
| `enrich-vocab-re-zero-vocab-説得力-vocab-card-v1` | `re-zero-vocab-説得力` |
| `enrich-vocab-vocab-re-zero-ryouchi-vocab-card-v1` | `vocab-re-zero-ryouchi` |
| `enrich-vocab-re-zero-vocab-敵対-vocab-card-v1` | `re-zero-vocab-敵対` |
| `enrich-vocab-vocab-re-zero-dangen-vocab-card-v1` | `vocab-re-zero-dangen` |
| `enrich-vocab-re-zero-vocab-証拠-v1` | `re-zero-vocab-証拠` |
| `enrich-vocab-vocab-re-zero-daisuki-vocab-card-v1` | `vocab-re-zero-daisuki` |

词汇项的 occurrence 下界为 4 次、4 行记录；所有 40 项还另以实际 `subtitle_lines.ja_text` 命中及相邻行核对，避免只相信 occurrence 的行号。

## 字幕核对备注

- grammar 20 项均核对 EP3 实际字幕，特别处理跨行后件、源摘录截断与角色缩约。
- sentence 20 项均以全文日文命中为准。数据库 `source_line_no` 普遍有偏移：
  - `010–020` 的实际命中通常比存储值小 1；
  - `021–029` 存在约 3–4 行偏移。
- `sentence-014` 的源中文把下一行「へこむのは そのあとにするわ」并入本句，候选已按真实日文边界拆清。
- `grammar-42` 的「危ないところだった」是“处在危险关头”，不是典型 `Vるところだった` 的“差点做成某事”；候选明确排除该误析。

## 最终机械 QA

运行：

```text
node regen\validate_candidates.mjs regen\batch5_vocab_linguistic.json regen\batch5_grammar_sentence_linguistic.json
```

结果：

- files：2
- rows：80
- unique IDs：80
- errors：0
- warnings：0
- 与 batch1–batch4 的长教学叶完全重复组：0
- 归一化整卡字符 4-gram Jaccard `>= 0.30`：0
- 最大相似度：0.0691
- payload 内部 source ID / 模板元信息泄漏：0

文件 SHA-256：

- `batch5_vocab_linguistic.json`：`2C0E5646CE03A6D6C5969F25D38E124DF51A82037D1FD6BA3ADA2EDB7C392789`
- `batch5_grammar_sentence_linguistic.json`：`8FF61539CAA838237E20793F847746457DD0D1DCF4494729868CE6CA7C748FB6`
