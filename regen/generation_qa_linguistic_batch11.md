# Linguistic Batch11 生成侧 QA

## 结论

本批有效候选共 80 条：40 条 vocab、20 条 grammar、20 条 sentence。每张卡均在读取 source、vocab occurrence、当前 EP6 日文字幕及相邻话轮后逐条人工重写；JavaScript 只用于 JSON 传输、结构校验、去重、泄漏与相似度检查，没有生成教学文字。本轮对 Supabase 只执行只读 SQL，没有写数据库。

冻结候选由三个文件组成：

- `regen/batch11_vocab_linguistic.json`：40 条 vocab
- `regen/batch11_grammar_sentence_linguistic.json`：20 条 grammar
- `regen/batch11_sentence_linguistic_overlay.json`：20 条 sentence

sentence 使用单独 overlay 是文件追加阶段 Windows sandbox helper 反复拒绝读取既有 grammar 文件后的恢复方案；overlay 本身包含完整对象，不依赖模板补全。最终 validator 直接读取上述三个文件。

## 候选资格与锁集

筛选口径：

1. `linguistic_prompt_version = 'linguistic-card-v1'`。
2. 旧 `linguistic_payload` 非空。
3. 排除 `maintenance.regen_20260727_manifest` 中已应用的 enrichment。
4. 展平剩余旧 payload 的字符串叶，只统计长度不低于 12 的文本。
5. 字符串叶至少跨 3 张旧卡重复；每张候选至少命中 5 个此类长重复叶。

排除 Batch1–10 已应用记录后，高置信重复模板池为：

| source_type | 合格池 | 每卡命中范围 | 最广重复叶跨卡数 |
| --- | ---: | ---: | ---: |
| vocab | 209 | 5–10 | 268 |
| grammar | 179 | 13–17 | 268 |
| sentence | 53 | 14–16 | 268 |

最终选择：

- vocab：EP6 occurrence 按首个记录行号排序的前 40 项。
- grammar：EP6 sort 1–11、13–21，共 20 项。跳过 grammar-012，因为其完整例句已由 Batch10 sentence-021 精细覆盖。
- sentence：EP6 sentence-023 与 sentence-044–062，共 20 项。
- ordered ID MD5：`565962f3fc8972495aea7427755a5ccd`

数据库只读复算：

| 检查 | 结果 |
| --- | ---: |
| incoming / unique | 80 / 80 |
| enrichment 三元组精确映射 | 80 |
| source 存在 | 80 |
| backup 存在 | 80 |
| 仍为旧 prompt 且 payload 非空 | 80 |
| 相对 backup 未漂移 | 80 |
| applied manifest overlap | 0 |
| 入选卡重复长叶命中范围 | 7–17 |
| 入选卡最广重复叶跨卡数 | 268 |

## Source、occurrence 与当前字幕

### vocab

- 40/40 精确映射 `public.learning_vocab_items`。
- 40/40 均有 EP6 `public.learning_vocab_occurrences`；记录内 `occurrence_count` 合计 47。
- 当前 EP6 日文字幕对词条 surface 直接命中 35/40。
- 其余 5 项按真实活用逐条核实：
  - `狙う → 狙われていた`
  - `疑う → 疑われかねない`
  - `暴く → 暴いてみせる`
  - `衰弱させる → 衰弱させて`
  - `案じる → 案じてる`
- 旧 occurrence 行号存在局部偏移，未机械采信；每张卡的 `review_note` 记录当前字幕证据和必要话轮。

关键边界包括：

- `未然に防ぐ` 的宾语承接袭击，表示事前阻止，不是事后补救。
- `追っ払う` 所在“赶虫”话轮以虫影射昴，直到后文才揭晓。
- `汚い` 在破抹布语境选择物理污秽义，不是“手段卑鄙”。
- `疑われかねない` 的昴是被怀疑者；`かねない` 表示不利可能，并非“不可能”。
- `義理はない` 是否认关系义务，却同时泄露碧翠丝确实参与救治。
- `案じる` 在本句是担忧艾米莉雅，不是制定方案。

### grammar

- 20/20 精确映射 `public.learning_grammar_points`，均来自 EP6。
- source `ja_example` 去除空白和标点后，在当前 EP6 日文字幕中命中 20/20。
- 逐条重新判断真实形式，不照搬旧 pattern：
  - grammar-001 的 `戻ってこれた` 是会话中的ら抜き可能，`と言うべきか` 用于措辞选择，不是道德义务。
  - grammar-002 只有悬空 `…って`，没有足够证据伪造完整引语。
  - grammar-004 的真实台词同时问 `何日` 与 `何時`，旧 pattern 只保留日期并不完整。
  - grammar-009 必须连读上一话轮 `せめて` 和后件 `話も違う`。
  - grammar-018 的 `あるかないかで言えば ある` 只作最低限度肯定，后文仍修正分类。
  - grammar-019/020 要连读 `魔法というより 呪いのほうに近い`，保留相对比较，而非绝对否定魔法。
  - grammar-021 的 `もっとも` 是“不过”的连接词，不是最高级 `最も`。

### sentence

- 20/20 精确映射 `public.learning_sentences`，均来自 EP6。
- source `ja_text` 去除空白和标点后，在当前 EP6 日文字幕中命中 20/20。
- 对跨行、省略与指称重新划界：
  - sentence-023 的开头 `…って` 是被截断回应，不能补写不存在的原话。
  - sentence-044 的 `けど` 落点在下一行；只保留“很帅”会漏掉同时存在的负面评价。
  - sentence-046 的 `自分` 属于被评论的赤鬼，不是拉姆或昴。
  - sentence-047 把“两鬼都想亲近”戏谑重构为花心，不是事实性的恋爱背叛。
  - sentence-049 必须连读前一行从 `神のみぞ` 到 `竜のみぞ` 的即时自我修正。
  - sentence-051 的 `嫉妬の魔女と` 是后置命名补语；主语、宾语与 `呼んだ` 位于上一话轮。
  - sentence-058 与前一句构成 `死にたくねえ / 死なせたくねえ` 的使役最小对。
  - sentence-059 先强调“看见成功”，所见对象 `鎖の音の正体` 在下一话轮才揭示。
  - sentence-061 的 `てもらう` 只反映雷姆所求的结果；礼貌授受外形不使暗杀变成对昴有利的帮助。
  - sentence-062 的 `ウソだろ` 表达不愿相信，并非指控雷姆刚才撒谎。

## 结构、重复、泄漏与相似度

运行：

```text
node regen/validate_candidates.mjs \
  regen/batch11_vocab_linguistic.json \
  regen/batch11_grammar_sentence_linguistic.json \
  regen/batch11_sentence_linguistic_overlay.json
```

结果：

- files：3
- rows：80
- unique IDs：80
- errors：0
- warnings：0
- 80/80 均有 3 个 `domains`、2 个 `terms`、非空 `headlineZh`、`historicalNoteZh`、`cautionZh` 与 `review_note`
- 批内 ID 重复：0；批内 `source_type + source_id` 重复：0
- 与 Batch1–10 共 800 张既有语言学卡相比，ID 重复：0；source 重复：0
- Batch11 内长度不低于 12 的完全相同教学字符串叶：0
- Batch11 与 Batch1–10 的完全相同长教学字符串叶：0
- 归一化 28 字符连续片段在 Batch11 批内或跨 Batch1–10 复用：0
- 已知旧模板短语命中：0
- payload 内 source 标记、内部 ID、prompt version、行号、数据库或模板元信息泄漏：0
- 最大跨批整卡字符 4-gram Jaccard：`0.03709949409780776`
  - 本批：`enrich-vocab-re-zero-vocab-疑う-v1`
  - 既有：`sentence-card-v1:re-zero-s01e06-sentence-019`
  - 两卡共享同集“被怀疑”情境，但教学文字未复写

全量复核：

```text
node regen/validate_candidates.mjs \
  regen/batch{1..10}_vocab_linguistic.json \
  regen/batch{1..10}_grammar_sentence_linguistic.json \
  regen/batch11_vocab_linguistic.json \
  regen/batch11_grammar_sentence_linguistic.json \
  regen/batch11_sentence_linguistic_overlay.json
```

- files：23
- rows：880
- unique IDs：880
- errors：0
- warnings：0

## 冻结哈希

- `batch11_vocab_linguistic.json`：`5A37915CDF0DF0EAEF5DD4F7F2722D4E3FE4BFD31DF37EED4698699CC5D71123`
- `batch11_grammar_sentence_linguistic.json`：`576132E30C1FE11F92562B4BEEDC68F0043DF508A1154925DDED8EFE61716226`
- `batch11_sentence_linguistic_overlay.json`：`0AAEF58ABC18C9CD32EFA431959933D7441F4920450B6461209188384D718279`

## 状态

生成侧 QA 通过，三文件已按上述 SHA-256 冻结，可释放给独立 reviewer。该结论不代表交叉审校已经完成；本轮没有执行任何数据库写入。
