# Linguistic batch10 生成侧 QA

## 结论

本批共 80 条：40 条 vocab、20 条 grammar、20 条 sentence。每张卡均在逐条读取数据库 source、vocab occurrence、当前日文字幕与相邻话轮后重新手写；JavaScript 仅用于结构校验、去重、泄漏与相似度检查，没有生成教学文字。本轮对 Supabase 只执行只读 SQL，没有写数据库。

候选文件：

- `regen/batch10_vocab_linguistic.json`
- `regen/batch10_grammar_sentence_linguistic.json`

## 候选资格与数据库映射

筛选口径：

1. `linguistic_prompt_version = 'linguistic-card-v1'`。
2. 旧 `linguistic_payload` 非空。
3. 排除 `maintenance.regen_20260727_manifest` 中 `applied_at is not null` 或 `review_status = 'applied'` 的 enrichment；Batch9 已包含在排除集合。
4. 展平仍合格旧 payload 的字符串叶，只统计长度不低于 12 的文本。
5. 字符串叶至少跨 3 张仍未应用的 enrichment 重复；每张候选至少命中 5 个此类长重复叶，再人工确认确属模板污染。

排除全部已应用记录后，高置信重复模板池为：

| source_type | 合格池 | 池内每卡命中范围 | 重复叶最大跨卡数 |
| --- | ---: | ---: | ---: |
| vocab | 249 | 5–10 | 308 |
| grammar | 199 | 13–17 | 308 |
| sentence | 73 | 16 | 308 |

最终 80 条只读复算：

| source_type | 数量 | enrichment 三元组精确映射 | 旧提示版本 / payload | source 存在 | manifest 已应用 | 入选卡命中范围 | 入选卡最大跨卡数 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| vocab | 40 | 40 | 40 / 40 | 40 | 0 | 7–10 | 208 |
| grammar | 20 | 20 | 20 / 20 | 20 | 0 | 16–17 | 308 |
| sentence | 20 | 20 | 20 / 20 | 20 | 0 | 16 | 308 |
| **合计** | **80** | **80** | **80 / 80** | **80** | **0** | **≥7** | **≥208** |

旧 payload 中跨大量无关卡片复现的代表性套话包括：

- vocab：`形态构成 (Morphology)`、`结合上下文精确定位词义，拒绝机械对译。`、`词汇积累重在听说直觉与例句复现。`
- grammar：`形态学：词类构成与活用分析`、`句法管辖 (Syntactic Scope)`、`学习卡级别的语言学解析重在建立语言直觉，建议结合原声跟读。`
- sentence：`意群切分 (Chunking)`、`信息结构 (Information Structure)`、`句子卡片旨在培养影音语感与流利跟读能力。`

这些文本原样跨数十至数百张卡出现，不能充当具体学习项的分析。

## Source、occurrence 与当前字幕

### vocab

- 40/40 精确映射 `public.learning_vocab_items`。
- 40/40 均有 EP7 的 `public.learning_vocab_occurrences` 记录；所选词在全表合计 50 个分集 occurrence 记录，`occurrence_count` 合计 56，跨 9 个分集。
- 当前 EP7 日文字幕支持 40/40。21 项可按词条 surface 直接命中，19 项按真实活用或可靠词干定位：
  - `疑わしい→疑わしき`、`罰する→罰せよ`、`突き立てる→突き立てて`。
  - `漂う→漂わせて`、`耐える→耐えられない`、`装う→装って`。
  - `落ち込む→落ち込んで`、`辛気くさい→辛気くさく`、`持ち込む→持ち込まれる`。
  - `担ぎ込む→担ぎ込んだ`、`引っ込む→引っ込んで`、`思い込む→思い込んでた`。
  - `身を張る→身を張って`、`気に入る→気に入った`、`投げ捨てる→投げ捨てた`。
  - `騒ぎ立てる→騒ぎ立ててた`、`放っておく→放っておけん`、`拾う→拾った`、`悲しませる→悲しませた`。
- 旧 `example_line_nos` 存在整体偏移，未被机械采信。每张卡的 `review_note` 都记录当前 EP7 行号和必要的相邻话轮。
- 同词多次出现时保留说话人边界：`行儀` 是碧翠丝要求后被昴复述；`洗いざらい` 先属昴内心愿望、后属拉姆命令；`身の安全` 两次都回到碧翠丝的契约。

### grammar

- 20/20 精确映射 `public.learning_grammar_points`，均来自 EP5。
- source `ja_example` 去除空白和标点后，在当前字幕单行内精确命中 19/20。
- 唯一机械例外是 `grammar-040`：source 为「やれることが増えれば それだけ仕事が減る」，当前 EP5 line 182 为「バルスのやれることが増えれば それだけラムの仕事が減る」。字幕只增加两处所属成分，`ば＋それだけ` 的对应程度结构完整。
- `grammar-040` 的旧 pattern 写成 `〜れば〜ほど/それだけ`，但真实字幕没有「ほど」；新卡明确拒绝把未出现的形式当原句证据。
- `grammar-030` 的「はずだったんだけど…」必须连读 lines 109–111：上一行给出照旧推进的计划，下一行才说明与前回完全不同。
- `grammar-037` 的后果还延续到下一行的“不能写便条”；卡片区分当前例句与跨行补充。

### sentence

- 20/20 精确映射 `public.learning_sentences`，均来自 EP6。
- source `ja_text` 去除空白和标点后，在当前日文字幕单行内命中 20/20。
- 旧 `source_line_no` 与当前字幕有偏移，所有卡均以当前行号和上下文重新定位。
- `sentence-027` 的「それ」依赖前文简便方法及风险说明，后文才补充玛那与生命力关系；新卡未把代词武断收窄为一句定义。
- `sentence-031` 只给出「なら」条件前件，破折号后的推理跨行展开；未伪造一个逐字后件。
- `sentence-039` 至 `sentence-042` 位于嵌套童话叙事：愿望主体是红鬼，计划引语来自青鬼，出走决定也属于青鬼信件，哭泣主体再回到红鬼。
- `sentence-043` 的「そうも思う」允许“悲伤”与“温柔”两种评价并存，不是昴彻底否定拉姆。

## 人工重写中的证据边界

- `疑わしきは罰せよ` 中「疑わしき」是文语色彩形式，「罰せよ」是命令，不是条件。
- `漂わせて` 是「漂う」的使役形；只据词形可确认“使气味散发”，不能额外断言是否出于昴主观意图。
- `白々しい` 评价说法虚假，`辛気くさい` 评价神情或气氛阴沉；二者都不应被邻近的「臭い」误导成嗅觉词。
- `重きを置く` 所在台词有省略，新卡只保留固定搭配与应然义，不虚构唯一施事者。
- `崖…` 单独证明地点识别；跨出一步的设想来自后续话轮，未压进词义本身。
- `投げ捨てる` 的宾语「それ」范围较宽，新卡不把它强制限定为某一个具体承诺。
- `放っておけんのか` 是否定可能加反问式自语，并非普通的肯定许可问句。
- `食客という名の居候` 是拉姆的讥讽性重命名，不是宅邸制度的正式定义。

## 结构、去重与泄漏检查

运行：

```text
node regen/validate_candidates.mjs regen/batch10_vocab_linguistic.json regen/batch10_grammar_sentence_linguistic.json
```

结果：

- files：2
- rows：80
- unique IDs：80
- errors：0
- warnings：0
- 80/80 均有 3 个 `domains`、2 个 `terms`、非空 `headlineZh`、`historicalNoteZh`、`cautionZh` 与 `review_note`
- 批内 ID 重复：0；批内 `source_type + source_id` 重复：0
- 与 batch1–9 共 720 张既有语言学卡相比，ID 重复：0；source 重复：0
- batch10 内长度不低于 12 的完全相同教学字符串叶：0
- batch10 与 batch1–9 的完全相同长教学字符串叶：0
- 归一化 28 字符连续片段在 batch10 批内或跨 batch1–9 复用：0
- 已知旧模板短语命中：0
- payload 内 source ID、source type、prompt version、行号或模板元信息泄漏：0
- 归一化整卡字符 4-gram Jaccard `>= 0.30`：0
- 最大跨批相似度：0.0465，发生在本批 `grammar-037` 与旧 `sentence-030`；两者涉及同一 EP5 读写与工作语境，但教学文字未复写

全量复核：

```text
node regen/validate_candidates.mjs regen/batch{1..10}_vocab_linguistic.json regen/batch{1..10}_grammar_sentence_linguistic.json
```

- files：20
- rows：800
- unique IDs：800
- errors：0
- warnings：0

## 文件哈希

- `batch10_vocab_linguistic.json`：`169B359FDA10ABF36FDE4CC2226E11D02A5097230853C31ADDC219A4DC172442`
- `batch10_grammar_sentence_linguistic.json`：`6D87FCEEE86040806773E90159CBB244B696339BCB5F93A42DB2BA9B377EC570`

## 状态

生成侧 QA 通过，两个 JSON 已按上述 SHA-256 冻结，可释放给独立 reviewer。该结论不代表交叉审校已经完成；本轮没有执行任何数据库写入。
