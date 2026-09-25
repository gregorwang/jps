# Linguistic Batch 7 交叉复核与冻结报告

复核日期：2026-07-27  
复核范围：

- `regen/batch7_vocab_linguistic.json`：40 条词汇卡
- `regen/batch7_grammar_sentence_linguistic.json`：20 条语法卡、20 条句子卡
- 合计：80 条

## 冻结结论

| 类型 | 总数 | 原稿通过 | 修订后通过 | 拒绝 |
| --- | ---: | ---: | ---: | ---: |
| 词汇 | 40 | 33 | 7 | 0 |
| 语法 | 20 | 19 | 1 | 0 |
| 句子 | 20 | 17 | 3 | 0 |
| **合计** | **80** | **69** | **11** | **0** |

80 条均已完成独立交叉复核并通过，可以作为本批最终写库候选。11 条发生改动，其中 8 条修正实质性语言学、说话人、语境或证据边界，3 条仅补足复核说明中的当前字幕命中及 occurrence 非穷尽性说明；没有条目因证据不足而拒绝。

本报告冻结下列最终文件：

- `regen/batch7_vocab_linguistic.json`
- `regen/batch7_grammar_sentence_linguistic.json`

生成阶段报告 `regen/generation_qa_linguistic_batch7.md` 未被改动，继续作为生成者自检快照保留。该报告中的两个候选文件哈希属于交叉审校前版本；最终写库应以下文“冻结哈希”为准。

## 人工交叉审校修订

### 词汇卡

| source_id | 修订要点 |
| --- | --- |
| `re-zero-vocab-買い取る` | 将“所有权转移／取得处分权”收窄为买方从对方手中接收、接手物品。EP2 的对象是失窃徽章，`買い取る` 本身不能证明卖方具有合法处分权，也不能单独保证法律所有权有效转移。 |
| `re-zero-vocab-持ち主` | 将一般词义与本段人物判断分开：`持ち主` 可指话语中认定的物主或通常持有人；EP2 中 Felt 的眼前持有与昴认定的 `元の持ち主` 分离，但该词本身不是法律判定术语。 |
| `re-zero-vocab-身柄` | 删除抽象“人身处置权／暂时拥有权限”的过强表述，改为当事人本身及其安置、看管、保护、拘束、移交或接手状态。EP3 lines 421、460 的焦点是“由谁接手、如何安排”。 |
| `re-zero-vocab-前提` | 收窄跨集概括：三处台词分别把失败、重来或牺牲设为起点；只有 `犠牲前提` 直接揭露方案把牺牲正常化，不再把三处都笼统写成同一种批判。 |
| `vocab-re-zero-maigo` | 在复核说明中补入当前 EP12 line 279 的直接命中，并明确 occurrence 聚合不是当前字幕的穷尽索引。 |
| `vocab-re-zero-haafu-erufu` | 在复核说明中补入当前 EP24 lines 55、386 的直接命中，并区分 occurrence 记录数与当前字幕逐字检索结果。 |
| `vocab-re-zero-hatsugen` | 在复核说明中补入当前 EP16 line 93；该卡正文已经分析 `発言を顧みて`，现在证据说明与正文一致。 |

### 语法卡

| source_id | 修订要点 |
| --- | --- |
| `re-zero-s01e04-grammar-016` | 明确一段动词 `確かめる` 的可能形与被动形同为 `確かめられる`。本句没有明说检查者，形态表面存在同形性；结合检查情节，核心结果是“无敌意得以确认／已被确认”，不再使用非形态学术语“结果形”。 |

### 句子卡

| source_id | 修订要点 |
| --- | --- |
| `re-zero-s01e03-sentence-010` | 根据 EP3 lines 27–32 校正说话人和互动链：Felt 先指控昴设圈套，爱蜜莉雅在 line 31 问 `どういうこと？`，随后由爱蜜莉雅在 line 32 向昴和 Felt 确认两人是否为同伴。 |
| `re-zero-s01e03-sentence-031` | 根据 EP3 lines 199–201 校正威胁范围：艾尔莎说发动后 `私以外は誰も残らない`，并非双方同归于尽；昴上一句以 `自爆` 戏称该杀招，再用柔软请求阻止她贸然发动。 |
| `re-zero-s01e03-sentence-033` | 将泛化的 `Vところだった` 术语说明改为本句实际形态 `危ないところだった`，明确 `ところ` 表示刚才逼近危险结果的阶段，不是地点。 |

## 来源、occurrence 与日语字幕

### 词汇

- 40/40 个 `source_id` 在 `public.learning_vocab_items` 中存在。
- 40/40 均有 `public.learning_vocab_occurrences`；合计 60 个分集 occurrence 记录，记录内 `occurrence_count` 合计 108。
- 对 40 个词逐项检索当前日语字幕并阅读相邻行，不只读取 occurrence 的示例行。
- occurrence 表是已有聚合证据，不是当前字幕的穷尽 concordance。`迷子`、`発言`、`ハーフエルフ` 等词在当前字幕中的直接命中多于 occurrence 记录所列示例；卡片只把 occurrence 数写成数据库记录统计，不把它宣称为完整语料计数。
- 对 `買い取る`、`持ち主`、`身柄`、`前提`、`感覚`、`目立つ`、`黙る`、`免じる`、`呼び出す`、`抜く` 等容易受论元、语境或词形影响的项目另行连读上下文，教学结论均限制在真实证据范围内。

### 语法

- 20/20 个 `source_id` 在 `public.learning_grammar_points` 中存在。
- 20/20 的 source 日文可在当前 EP3/EP4 日语字幕至多连续 5 行窗口中命中。
- 已逐项复核形态、句法、语义和语用。重点确认：`見つけなきゃ出れない` 是否定条件产生的义务推导；`私室かしら` 在本处是人物化的柔化断言；`敵意がないのは確かめられた` 的实际谓语是动词；`助けてくれたじゃない` 是提醒共享的肯定事实。

### 句子

- 20/20 个 `source_id` 在 `public.learning_sentences` 中存在。
- 20/20 的 source 日文可在当前 EP3 日语字幕至多连续 5 行窗口中命中。
- 每条均连读前后对话，核对说话人、听者、照应、条件与因果范围。特别修正了 sentence-010 的说话人和 sentence-031 的威胁边界，并确认 sentence-022、023、027 的 source 截断不被误教为完整句法证据。

## Enrichment 映射与数据库状态

只读 SQL 复算结果：

- 候选 80 条，`public.learning_card_enrichments` 命中 80/80。
- `id + source_type + source_id` 三元组精确匹配 80/80。
- source 表命中 80/80：vocab 40、grammar 20、sentence 20。
- 80/80 当前仍为旧提示版本 `linguistic-card-v1`。
- 本地映射集合与数据库映射集合的 MD5 均为 `002cdad43ed4350e99bd46d43907aa1c`。
- 与 `maintenance.regen_20260727_manifest` 的非空 `applied_at` 重叠：0。
- 与 manifest 中 `review_status = 'applied'` 的重叠：0。
- 本轮只执行 `SELECT` 类查询；未执行 `INSERT`、`UPDATE`、`DELETE`、DDL、RPC 写入或迁移。

## 结构、去模板化与合并校验

最终候选校验：

```text
node regen/validate_candidates.mjs regen/batch7_vocab_linguistic.json regen/batch7_grammar_sentence_linguistic.json
```

- 2 个文件、80 条、80 个唯一 ID。
- errors：0；warnings：0。
- 80/80 均含非空 `headlineZh`、3 个 `domains`、2 个 `terms`、`historicalNoteZh`、`cautionZh` 和 `review_note`。
- 批内重复 ID：0；批内重复 `source_type + source_id`：0。

与既有候选合并校验：

- batch1–batch7：14 个文件、560 条、560 个唯一 ID。
- errors：0；warnings：0。
- batch7 与 batch1–6 的 ID 重叠：0；source 映射重叠：0。

去模板和泄漏检查：

- 用于跨卡比较的长度不低于 12 的唯一教学文本叶：904。
- batch7 内完全相同长文本叶跨卡重复组：0。
- batch7 与 batch1–6 的完全相同长文本叶重复组：0。
- 单个文本叶内归一化 28 字符连续片段跨卡复用组：0。
- 已知旧模板短语命中：0。
- payload 内部 enrichment ID、旧 prompt version 或 manifest 元信息泄漏：0。
- 归一化整卡字符 4-gram Jaccard `>= 0.30` 的卡对：0；最大值为 0.1592，出现在 `家事` 与 `全般`，二者因同段字幕中的上下位语义关系共享少量词汇，但教学焦点不同。

上述脚本只用于 JSON 结构、集合映射、重复文本、相似度和泄漏检查；教学文本、日语分析、说话人及证据边界均由人工逐条复核，不由脚本生成。

## 冻结哈希

| 文件 | SHA-256 |
| --- | --- |
| `regen/batch7_vocab_linguistic.json` | `2307887DBBE6D1FC34B043340EBA1657B1AD6D40EC4CCB7C1EF9641ADF93369A` |
| `regen/batch7_grammar_sentence_linguistic.json` | `9E5E8C0C868DD946AD2B1965207DD205FB61ECD462638E07EBF8EAF55BBBE2C5` |
| `regen/generation_qa_linguistic_batch7.md`（保留未改） | `BBF43B2C763EE5F734DBB64F59C97E3F36989686B016878555502544E941C000` |

## 最终状态

本批 80/80 已通过独立交叉复核，两个候选 JSON 自本报告所列最终校验与哈希起冻结，不再编辑。**可以进入数据库写入阶段。** 本复核任务本身没有执行任何数据库写入。
