# Linguistic Batch 10 独立交叉审校

## 结论

- 审校对象：
  - `regen/batch10_vocab_linguistic.json`：40 条 vocab
  - `regen/batch10_grammar_sentence_linguistic.json`：20 条 grammar、20 条 sentence
- 独立审校结果：**原样保留 77，人工修订 3，拒绝 0**
- 修订分布：vocab 3；grammar 0；sentence 0
- 数据库操作：只读 SQL，写入 0
- 文本处理方式：逐卡核对 source、当前连续字幕、形态、句法、语义、语用、说话人、指代、否定、施受、时态和证据边界；没有用模板或循环生成教学文字

80 条均保留为合格候选。三个问题不需要整卡拒绝，但会导致学习者误解相邻表达或指代，因此已在原 JSON 中人工精修。`batch10_grammar_sentence_linguistic.json` 经独立复核后未改动。

## 修订明细

### 1. `re-zero-vocab-特別扱い`

生成稿问题：

- 把后一句「ぞっとしねえな」笼统写成昴“感到恐惧”，并把它称为“恐惧框架”。
- 「ぞっとしない」在这里是惯用负面评价，核心为“不怎么样、不令人满意、不值得高兴”，不能按普通组合机械理解成“并不可怕”。

修订：

- headline 改为未知对象给予的特殊待遇“让人感觉不妙”。
- pragmatics 明确「ぞっとしない」的惯用义及其对“优待”联想的撤销。
- caution 增加不得按普通否定反向理解的边界。
- review_note 明确 current EP7 line 142 的目标和 line 143 的评价关系。

外部词义复证：

- [文化庁《平成28年度「国語に関する世論調査」》](https://www.bunka.go.jp/tokei_hakusho_shuppan/tokeichosa/kokugo_yoronchosa/pdf/h28_chosa_kekka.pdf)
- [国立国会图书馆协同参考数据库：「ぞっとしねえな」的意义调查](https://crd.ndl.go.jp/reference/entry/index.php?id=1000192756&page=ref_view)

### 2. `re-zero-vocab-投げ捨てる`

生成稿问题：

- 把 line 334 的「それ」扩大成“整段关系与说明机会”，并给出“宽域指代”标签。
- 连续字幕中，line 333 先明确提出「あの姉に弁明する機会」，line 334 才说「それを投げ捨てた」；最邻近、最可证的照应对象是这项弁明机会。

修订：

- headline 与 semantics 将直接指代收回到“向拉姆说明的机会”。
- 后续双胞胎关系破裂保留为丢弃机会造成的语用后果，不再塞入「それ」的字面范围。
- terms 从“宽域指代”改为“邻接照应”。
- review_note 记录 lines 333–334 的证据顺序。

### 3. `vocab-re-zero-hanatte_oku`

生成稿问题：

- 对「俺は放っておけんのか？」的概括基本方向正确，但把表层负极性问句写成“问句答案显然是否定”，容易让学习者把答句极性倒置。

修订：

- 明确表层是「放っておけないのか」：“我是不是终究无法放任她们”。
- 语境倾向的回答改为“是，我无法放任”，并说明这是自问确认，不是对他人的命令。
- 区分 line 353 的内心倾向、lines 354–358 的打断，以及 lines 359–366 才完成的生存选择。
- caution 增加不能倒成肯定可能“能把她们放着吗”。

## 数据库资格、备份与漂移

只读复算以两个候选文件的 80 个 `source_type + source_id` 为目标：

| 检查 | 结果 |
| --- | ---: |
| 当前 enrichment | 80/80 |
| 唯一 enrichment ID | 80/80 |
| 候选确定性 ID 映射 | 80/80 |
| `work_slug = 're-zero'` | 80/80 |
| `linguistic_prompt_version = 'linguistic-card-v1'` 且旧 payload 非空 | 80/80 |
| maintenance 备份存在 | 80/80 |
| 当前旧 payload、版本、质量分、状态与备份一致 | 80/80 |
| manifest 记录重叠 | 0 |
| 已应用 manifest 重叠 | 0 |

本地候选与数据库 source 的类型分布一致：

| source_type | 本地 | 数据库 source | 分集 |
| --- | ---: | ---: | --- |
| vocab | 40 | 40 | EP7 |
| grammar | 20 | 20 | EP5 |
| sentence | 20 | 20 | EP6 |

本轮没有发现生成后数据库漂移，也没有在已应用记录上重复审校。

## Source 与真实字幕复核

### vocab

- 40/40 精确映射 `learning_vocab_items`。
- 40/40 有 EP7 `learning_vocab_occurrences`。
- 当前 EP7 字幕：
  - 21/40 可由词条 surface 直接命中。
  - 19/40 由真实活用或复合形式命中。
- 复核范围覆盖当前 EP7 lines 2–405；重点连续话轮包括：
  - 「疑わしきは罰せよ」及女仆准则；
  - 魔女气味、假装照料与嫉妒；
  - 禁书库契约、罗兹瓦尔与拉姆对峙；
  - 悬崖独白、双胞胎记忆与生存选择。
- 使役、被动、可能、使役情绪动词和古风命令均与实际词形一致。
- 修订后的三项分别校正惯用义、代词照应和否定疑问极性；其余 37 项未发现说话人或论元倒置。

### grammar

- 20/20 精确映射 `learning_grammar_points`，均为 EP5。
- 对 source `ja_example` 去除空白、标点后作单行包含匹配：19/20。
- 唯一例外 `grammar-040`：
  - source：`やれることが増えれば それだけ仕事が減る`
  - current line 182：`バルスのやれることが増えれば それだけラムの仕事が減る`
  - 字幕只补明两处所属；「ば＋それだけ」结构一致。
- 独立复核确认：
  - `grammar-025` 是嵌入疑问，不把旧说明“不管原因”冒充字面语法。
  - `grammar-029` 的条件后件跨行，不把重复字幕块伪造为两个例句。
  - `grammar-030` 的预期内容和落空证据分属前后行。
  - `grammar-037` 的「任せられない」按不能托付解释，未误教为被动。
  - `grammar-040` 未把 source pattern 中未出现的「ほど」添入原句。
  - `grammar-043` 的「とか」按模糊引用处理，不误作必须并列两项。

### sentence

- 20/20 精确映射 `learning_sentences`，均为 EP6。
- source `ja_text` 在当前日文字幕中逐字单行命中：20/20。
- 独立复核确认：
  - `sentence-021` 的使役许可中，获准行动者是「俺」。
  - `sentence-027` 的「それ」保持依赖前文的边界，没有伪造单一定义。
  - `sentence-028` 的「でもある」为判断成分加追加「も」，不是让步。
  - `sentence-031` 只把「なら」前件当本行字面，后续推理仍标作跨行照应。
  - `sentence-033` 的「確かみたい」保留推断余地。
  - `sentence-039`–`042` 的嵌套童话说话层、愿望主体、决定主体和哭泣主体均正确。
  - `sentence-043` 让“悲伤”与“温柔”并存，没有写成昴彻底否定拉姆。

## 结构、重复与泄漏

独立复算：

- Batch10：80 条，唯一 ID 80，唯一 `source_type + source_id` 80。
- Batch1–10 合并：800 条，唯一 ID 800。
- 与 Batch1–9 的 ID 重叠：0。
- 与 Batch1–9 的 source 重叠：0。
- 80/80 均有 3 个 `domains`、2 个 `terms`、非空 `headlineZh`、`historicalNoteZh`、`cautionZh`、`review_note`。
- Batch10 内长度不少于 12 的完全相同教学字符串：0。
- Batch10 与 Batch1–9 的完全相同长教学字符串：0。
- 归一化 28 字符连续窗口，涉及 Batch10 的跨卡复用：0。
- 已知旧模板短语命中：0。
- learner-facing payload 中 source ID、prompt version、source 行号或内部字段名泄漏：0。
- 不可见格式字符：0。
- 归一化整卡 4-gram Jaccard `>= 0.30`：0。
- 最大相似度：`0.04647887323943662`，位于本批 `grammar-037` 与旧 `sentence-030`；属于同集读写工作语境，不构成教学文案复写。

## Validator

Batch10：

```text
node regen\validate_candidates.mjs regen\batch10_vocab_linguistic.json regen\batch10_grammar_sentence_linguistic.json
```

```json
{
  "files": 2,
  "rows": 80,
  "uniqueIds": 80,
  "errors": [],
  "warnings": []
}
```

Batch1–10 全量：

```json
{
  "files": 20,
  "rows": 800,
  "uniqueIds": 800,
  "errors": [],
  "warnings": []
}
```

## 最终文件哈希

`regen/batch10_vocab_linguistic.json`

```text
385784B0F5F303B0A58B5B2ABB12BAD9784E6CC727BE719667C5B4E0385B52B4
```

`regen/batch10_grammar_sentence_linguistic.json`

```text
6D87FCEEE86040806773E90159CBB244B696339BCB5F93A42DB2BA9B377EC570
```

生成前快照中 vocab 文件的 SHA-256 为 `169B359FDA10ABF36FDE4CC2226E11D02A5097230853C31ADDC219A4DC172442`；哈希变化仅来自上列 3 张 vocab 卡的人工修订。grammar/sentence 文件哈希未变。

## 状态

独立交叉审校完成。最终处置为：**保留 77、修订 3、拒绝 0**。两个候选文件自上述 validator 与 SHA-256 起冻结，可进入后续应用审查。本轮没有执行任何数据库写入。
