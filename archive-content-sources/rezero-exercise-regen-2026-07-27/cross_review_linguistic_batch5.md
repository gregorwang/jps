# Linguistic Batch 5 交叉复核与冻结报告

## 冻结结论

本次对 batch5 的 80 张语言学卡逐项复核了源记录、词汇 occurrence、日文字幕上下文、语义角色、搭配、语用、证据边界与去模板化程度。

| 类型 | 条数 | 原样通过 | 交叉修正 | 拒绝 |
|---|---:|---:|---:|---:|
| vocab | 40 | 39 | 1 | 0 |
| grammar | 20 | 19 | 1 | 0 |
| sentence | 20 | 20 | 0 | 0 |
| **合计** | **80** | **78** | **2** | **0** |

冻结候选：

- `regen/batch5_vocab_linguistic.json`
- `regen/batch5_grammar_sentence_linguistic.json`

生成侧原始 QA 已原样保留并重命名为 `regen/generation_qa_linguistic_batch5.md`。本报告记录交叉复核后的最终状态。全程没有写数据库。

## 两处交叉修正

### 1. `vocab-card-v1:vocab-re-zero-wasureru`

- 原问题：把 EP1 line 888 的「言い忘れていたけど…」解释为“临死前补说”。
- 证据：EP1 lines 884–890 显示说话人是遭受重创后准备反击的罗姆；后接「せめて相打ちに…」，并非已经成立的临死遗言。
- 修正：改为“罗姆重伤后准备拼死反击时，补出先前漏说的信息”，同时把“最后行动”收窄为“拼死一搏”，避免把风险错误写成既成死亡。

### 2. `grammar-card-v1:re-zero-s01e03-grammar-24`

- 原问题：把「〜てね」称为“礼貌愿望”。
- 证据：EP3 line 86 是「名前だけでも覚えて… 逝ってね！」；「てね」在此是柔和、亲切表面的请求形式，不是敬体，也不是愿望形。
- 修正：改为“柔和请求形式与死亡威胁形成冷酷反差”。其余关于「だけでも」最低阈值和语用反差的分析成立。

## 逐项复核清单

### Vocab（40/40）

`集数/次数`来自 `learning_vocab_occurrences` 聚合；`字幕证据`是本次实际连读的代表上下文，不只依赖 occurrence 行号。

| # | source_id / 词形 | 集数/次数 | 字幕证据 | 结论 |
|---:|---|---:|---|---|
| 1 | `vocab-re-zero-wasureru` / 忘れる | 7/9 | EP1 884–890 | 修正 |
| 2 | `re-zero-vocab-恩人` / 恩人 | 6/8 | EP1 448 | 通过 |
| 3 | `vocab-re-zero-joukyou` / 状況 | 6/8 | EP1 129 | 通过 |
| 4 | `vocab-re-zero-ganbaru` / 頑張る | 6/8 | EP1 480 | 通过 |
| 5 | `vocab-re-zero-gokai` / 誤解 | 6/7 | EP2 22 | 通过 |
| 6 | `re-zero-vocab-足止め` / 足止め | 6/7 | EP2 18 | 通过 |
| 7 | `re-zero-vocab-銀髪` / 銀髪 | 5/9 | EP1 641–642 | 通过 |
| 8 | `re-zero-vocab-気付く` / 気付く | 5/8 | EP1 414 | 通过 |
| 9 | `vocab-re-zero-genkai` / 限界 | 5/8 | EP1 477 | 通过 |
| 10 | `vocab-re-zero-shitsurei` / 失礼 | 5/7 | EP1 864 | 通过 |
| 11 | `re-zero-vocab-機嫌` / 機嫌 | 5/7 | EP4 472 | 通过 |
| 12 | `vocab-re-zero-chiryou` / 治療 | 5/7 | EP2 108 | 通过 |
| 13 | `vocab-re-zero-mitomeru` / 認める | 5/7 | EP1 639 | 通过 |
| 14 | `re-zero-vocab-誓う` / 誓う | 5/7 | EP6 406 | 通过 |
| 15 | `vocab-re-zero-jijou` / 事情 | 5/6 | EP1 815 | 通过 |
| 16 | `vocab-re-zero-anshin` / 安心 | 5/6 | EP1 786 | 通过 |
| 17 | `vocab-re-zero-taido` / 態度 | 5/6 | EP1 413 | 通过 |
| 18 | `re-zero-vocab-手段` / 手段 | 5/6 | EP2 194 | 通过 |
| 19 | `re-zero-vocab-時間稼ぎ` / 時間稼ぎ | 5/6 | EP3 80 | 通过 |
| 20 | `vocab-re-zero-jitai` / 事態 | 5/5 | EP7 271 | 通过 |
| 21 | `vocab-re-zero-abunai` / 危ない | 4/8 | EP1 30 | 通过 |
| 22 | `vocab-re-zero-sunao` / 素直 | 4/8 | EP1 219 | 通过 |
| 23 | `re-zero-vocab-覚える` / 覚える | 4/8 | EP1 235 | 通过 |
| 24 | `re-zero-vocab-確かめる` / 確かめる | 4/7 | EP4 42–43 | 通过 |
| 25 | `re-zero-vocab-方法` / 方法 | 4/6 | EP2 155 | 通过 |
| 26 | `vocab-re-zero-youken` / 用件 | 4/6 | EP2 331 | 通过 |
| 27 | `re-zero-vocab-説得力` / 説得力 | 4/6 | EP7 400 | 通过 |
| 28 | `vocab-re-zero-ryouchi` / 領地 | 4/6 | EP7 344 | 通过 |
| 29 | `re-zero-vocab-大切` / 大切 | 4/5 | EP1 116 | 通过 |
| 30 | `re-zero-vocab-幸い` / 幸い | 4/5 | EP3 264 | 通过 |
| 31 | `re-zero-vocab-成果` / 成果 | 4/5 | EP4 444 | 通过 |
| 32 | `re-zero-vocab-敵対` / 敵対 | 4/5 | EP7 28 | 通过 |
| 33 | `vocab-re-zero-dangen` / 断言 | 4/5 | EP7 196 | 通过 |
| 34 | `re-zero-vocab-根拠` / 根拠 | 4/5 | EP2 270 | 通过 |
| 35 | `re-zero-vocab-混乱` / 混乱 | 4/5 | EP4 212–214 | 通过 |
| 36 | `vocab-re-zero-muda` / 無駄 | 4/5 | EP1 460 | 通过 |
| 37 | `re-zero-vocab-証拠` / 証拠 | 4/5 | EP2 80 | 通过 |
| 38 | `vocab-re-zero-inochigake` / 命懸け | 4/4 | EP3 395 | 通过 |
| 39 | `re-zero-vocab-変わる` / 変わる | 4/4 | EP1 41–42 | 通过 |
| 40 | `vocab-re-zero-daisuki` / 大好き | 4/4 | EP3 67–69 | 通过 |

### Grammar（20/20）

| source_id | 实际字幕 | 结论 |
|---|---|---|
| `re-zero-s01e03-grammar-24` | EP3 86 | 修正 |
| `re-zero-s01e03-grammar-25` | EP3 89 | 通过 |
| `re-zero-s01e03-grammar-26` | EP3 95–96 | 通过 |
| `re-zero-s01e03-grammar-27` | EP3 97–98 | 通过 |
| `re-zero-s01e03-grammar-28` | EP3 99 | 通过 |
| `re-zero-s01e03-grammar-29` | EP3 108 | 通过 |
| `re-zero-s01e03-grammar-30` | EP3 118 | 通过 |
| `re-zero-s01e03-grammar-31` | EP3 121 | 通过 |
| `re-zero-s01e03-grammar-32` | EP3 122–123 | 通过 |
| `re-zero-s01e03-grammar-33` | EP3 127–129 | 通过 |
| `re-zero-s01e03-grammar-34` | EP3 129 | 通过 |
| `re-zero-s01e03-grammar-35` | EP3 161 | 通过 |
| `re-zero-s01e03-grammar-36` | EP3 175 | 通过 |
| `re-zero-s01e03-grammar-37` | EP3 179 | 通过 |
| `re-zero-s01e03-grammar-38` | EP3 198 | 通过 |
| `re-zero-s01e03-grammar-39` | EP3 201 | 通过 |
| `re-zero-s01e03-grammar-40` | EP3 208 | 通过 |
| `re-zero-s01e03-grammar-41` | EP3 214 | 通过 |
| `re-zero-s01e03-grammar-42` | EP3 228 | 通过 |
| `re-zero-s01e03-grammar-43` | EP3 228 | 通过 |

20/20 的 source example 经去空白、标点归一后均能在对应日文字幕中命中。重点确认了跨行后件、缩约、否定范围、施受角色和 `grammar-42` 的源标签误导；候选分析没有把「危ないところだった」误套成典型 `Vるところだった`。

### Sentence（20/20）

| source_id | 实际字幕 | 结论 |
|---|---|---|
| `re-zero-s01e05-sentence-010` | EP5 33 | 通过 |
| `re-zero-s01e05-sentence-011` | EP5 38 | 通过 |
| `re-zero-s01e05-sentence-012` | EP5 42 | 通过 |
| `re-zero-s01e05-sentence-013` | EP5 45 | 通过 |
| `re-zero-s01e05-sentence-014` | EP5 49 | 通过 |
| `re-zero-s01e05-sentence-015` | EP5 54 | 通过 |
| `re-zero-s01e05-sentence-016` | EP5 63 | 通过 |
| `re-zero-s01e05-sentence-017` | EP5 71 | 通过 |
| `re-zero-s01e05-sentence-018` | EP5 73 | 通过 |
| `re-zero-s01e05-sentence-019` | EP5 86 | 通过 |
| `re-zero-s01e05-sentence-020` | EP5 90 | 通过 |
| `re-zero-s01e05-sentence-021` | EP5 94–95 | 通过 |
| `re-zero-s01e05-sentence-022` | EP5 107 | 通过 |
| `re-zero-s01e05-sentence-023` | EP5 109 | 通过 |
| `re-zero-s01e05-sentence-024` | EP5 112–113 | 通过 |
| `re-zero-s01e05-sentence-025` | EP5 128 | 通过 |
| `re-zero-s01e05-sentence-026` | EP5 133 | 通过 |
| `re-zero-s01e05-sentence-027` | EP5 134 | 通过 |
| `re-zero-s01e05-sentence-028` | EP5 150 | 通过 |
| `re-zero-s01e05-sentence-029` | EP5 168 | 通过 |

20/20 的 source Japanese 经归一化后与真实字幕精确匹配。数据库 `source_line_no` 的系统性偏移没有被当作真实上下文；每卡均按全文命中重新定位。`sentence-014` 的中文跨行误并、`sentence-015` 的反问否定、`sentence-022` 的执行者／受益视角及 `sentence-026–027` 的理想互助与实际单向补位均已核实。

## Occurrence 上游异常

40/40 词汇都有 occurrence；共 187 条集记录、254 次 occurrence、244 个 `example_line_nos` 引用。聚合次数与 40 张卡的 `review_note` 全部一致。

其中 4/244 个示例行号在当前 `subtitle_lines` 中不存在，属于 occurrence 的旧字幕偏移，不是候选卡新增的断言：

| source_id | occurrence 引用 | 当前字幕中的词形命中 |
|---|---|---|
| `re-zero-vocab-混乱` | EP4 500 | EP4 493（另有 213） |
| `re-zero-vocab-誓う` | EP10 422、424 | EP10 403、405 |
| `vocab-re-zero-daisuki` | EP7 421 | EP7 408 |

三张候选卡引用的代表例分别是 EP4 212–214、EP6 406、EP3 67–69，均已直接核对。因此该上游偏移不阻断候选，但将来清理 occurrence 时应修正这 4 个行号。

## 去模板化与证据边界

- 80/80 均围绕各自字幕中的真实构式、论元、角色关系或话轮功能展开，没有用同一段“形态—语义—语用”空壳替换词面。
- 断言均限制在字幕可支持的范围；不把中文错并行、存储行号附近的无关台词或作品外设定当成证据。
- `linguistic_payload` 内部 source ID／模板元信息泄漏为 0。
- batch1–batch5 共 400 张卡一起运行长教学字符串重复检查，重复告警为 0。

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

再将 batch1–batch5 的 10 个语言学候选文件合并校验：

- rows：400
- unique IDs：400
- errors：0
- warnings：0

交叉复核后 SHA-256：

- `batch5_vocab_linguistic.json`：`0C3F6B2D6ACD1F699B715DBD2D1E570FE9F4405C2649981B51FCA664645E5FD6`
- `batch5_grammar_sentence_linguistic.json`：`5C589DE21DA8AEC18C0077082D4FD1FE6C0B65DC8F36904F08715306296ED77C`

## 数据库状态

- enrichment/source 映射：80/80 精确一致。
- 旧提示版本：80/80。
- 已应用 manifest 重叠：0。
- 本次执行：只读 SQL；无 INSERT、UPDATE、DELETE、DDL 或权限改动。

以上两个候选文件现已完成交叉复核并冻结，可交由根任务按既定事务写入。
