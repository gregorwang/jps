# Meta 练习生成侧 QA（Batch 10）

## 结论

- 基础候选：`regen/batch10_meta_exercises.json`，42 条
- 生成修正 overlay：`regen/generation_overlay_meta_exercises_batch10.json`，1 条
- 有效冻结版本：保持基础文件原顺序，以 `id` 为键用 overlay 完整替换同 ID 对象；最终仍为 42 条
- 数据范围：EP43，原练习 `sort_order` 262–352 的锁定选集
- 首 ID：`re-zero-ex-ep043-262`
- 末 ID：`re-zero-ex-ep043-352`
- 练习类型：42/42 为 `sentence_understanding`
- 数据库操作：只读查询，写入 0
- 独立复核状态：尚未进行；本报告仅记录生成与机械 QA，不能替代不同 Agent 的逐条复核

本批逐 ID 读取当前 `learning_exercises` 原题、精确对应的 `learning_sentences`、EP43 当前日文字幕连续话轮，并用本地 Netflix 日文 CC 的说话人标注交叉确认。42 条 `prompt`、`answer`、`hint`、`review_note` 均由 Agent 逐条人工撰写。PowerShell、SQL 与 Node 仅用于选取、映射、计数、重复／泄漏检查、哈希和 validator，没有用于生成或拼接教学文字。

## Base + overlay 冻结规则

基础文件落盘后的机械元话语扫描发现 `re-zero-ex-ep043-313` 的答案含有“教学重点”这一学习者不需要看到的表述。按冻结规则，没有回写或追加基础记录，而是建立一个只含该 ID 完整对象的 generation overlay。

有效候选定义：

1. 读取基础文件 42 条并保持原顺序。
2. 读取 overlay 1 条。
3. 相同 `id` 以 overlay 的完整对象替换基础对象。
4. overlay 不能作为第 43 条追加。

只读合并结果：

| 检查项 | 结果 |
| --- | ---: |
| base rows | 42 |
| overlay rows | 1 |
| overlay unique IDs | 1 |
| overlay ID 在 base 中命中 | 1 |
| effective rows | 42 |
| effective unique IDs | 42 |
| 学习者可见内部元话语 | 0 |

## 选集与只读数据库状态

锁定选集来自 `regen/selection_next_meta_batch10.json`。有效候选与选集比较：

| 检查项 | 结果 |
| --- | ---: |
| selection rows | 42 |
| candidate rows | 42 |
| candidate unique IDs | 42 |
| selection missing | 0 |
| candidate extras | 0 |
| ordered ID MD5 | `48fcd05c324b7c4eccc37ed06187f57f` |

ordered ID MD5 与 `regen/selection_qa_next_round.md` 的数据库锁集指纹完全相同。

生成前只读查询再次确认当前 42/42 原题存在、42/42 `answer` 仍以「用于」开头。选集 QA 已确认这 42 条在备份表中存在、当前整行与备份完全一致，且 `maintenance.regen_20260727_manifest` 任意状态重叠为 0。生成阶段没有执行 INSERT、UPDATE、DELETE、DDL 或 RPC 写入。

## Learning sentence 映射

- 原题引号中的目标日文与 EP43 `learning_sentences.ja_text` 精确匹配：42/42。
- 每条原题的精确 sentence 匹配数：1；没有零匹配或多 sentence 匹配。
- 42 个 `review_note` 均含明确 `learning_sentences` ID。
- source 引用数：42；唯一 source ID：42；缺失引用：0。
- 首 source：`re-zero-s02e18-sentence-126`
- 末 source：`re-zero-s02e18-sentence-216`
- ordered source ID MD5：`9d65a4e7b8c5a3cff018fc1b5051155f`

部分旧 `learning_sentences.source_line_no` 与当前 `subtitle_lines.line_no` 存在后段偏移。本批只把旧行号当定位线索，最终以目标日文命中、连续日文话轮和 Netflix CC 的说话人边界为准。

## 当前字幕与跨行上下文

对 42 个目标做以下排版级归一化后，与整集当前 `subtitle_lines.ja_text` 比较：

- 去空白；
- 把半角中点 `･` 与全角中点 `・` 统一；
- 去掉字幕行尾续接破折号 `―`。

结果：

| 当前字幕匹配 | 数量 |
| --- | ---: |
| 零匹配目标 | 0 |
| 唯一匹配目标 | 37 |
| 多行重复目标 | 5 |
| 总命中字幕行 | 48 |

多行重复均通过上下文、旧 source 线索和 Netflix CC block 定位，没有按字符串任取一行：

| exercise ID | 当前字幕 lines | 说明 |
| --- | --- | --- |
| `re-zero-ex-ep043-262` | 294, 297 | 取首次道歉及其后「私たちをかくまって」；第二次道歉已进入后续话轮 |
| `re-zero-ex-ep043-291` | 354, 355 | 同一问句的连续排版复现 |
| `re-zero-ex-ep043-310` | 396, 452, 453 | 取潘多拉初次劝阻雷古勒斯后的呼语；后两行属于另一场处置话轮 |
| `re-zero-ex-ep043-339` | 462, 463 | 同一替代现实陈述的连续排版复现 |
| `re-zero-ex-ep043-341` | 473, 474 | 同一因果说明的连续排版复现 |

Netflix CC 还用于确认数据库字幕中没有保存的说话人边界，包括：

- 佛尔特娜的两次「ごめんなさい」；
- 阿齐对佛尔特娜的「ひきょうだ」及护送爱蜜莉雅后的连续话轮；
- 阿齐遭黑蛇侵蚀后的术式、安抚与最后阻断；
- 雷古勒斯、潘多拉、裘斯、佛尔特娜在战斗段的换人；
- 幼年爱蜜莉雅跟随妖精抵达封印后，潘多拉说「ようこそ／お待ちしておりました」。

## 人工生成覆盖范围

生成稿逐条处理了以下边界，供独立 reviewer 重新核验：

- 形态与活用：可能形、使役形、被动态、否定命令、过去判断、持续体及口语缩约。
- 句法：跨字幕未完句、名词化「こと／の」、引用「って／と」、并列「も」、反问式「誰が～ものか」和长连体修饰。
- 语义与语用：表面请求和实际指令、表面建议和嘲弄、反事实自责、受害者归责、策略性赞美及敬语包装的强制行为。
- 说话人与指代：`あなた`、`彼ら`、`彼`、`自分`、`そいつ`、`あの２人`、`それ／そう` 均由连续话轮约束。
- 否定、时态与语态：`誰も～ない`、`別に～ない`、`はずがない`、`ところだった`、`書き換えません`、`囲まれて過ごしている` 等未作机械直译。
- 作品内词汇：`ヒューマ／アル・ヒューマ` 作为术式呼喊处理；儿童片假名 `フーイン` 按上下文恢复为「封印」，没有伪造普通日语词源。

以上是生成侧覆盖清单，不构成复核通过结论。

## 结构、唯一性与泄漏

有效 42 条：

| 检查项 | 结果 |
| --- | ---: |
| 五个必需字段空值 | 0 |
| 单字母答案 | 0 |
| 以「用于」开头的答案 | 0 |
| `prompt` 精确重复组 | 0 |
| `answer` 精确重复组 | 0 |
| `hint` 精确重复组 | 0 |
| `review_note` 精确重复组 | 0 |
| 学习者字段中的 V10、source ID、manifest、数据库、生成器／模型或答案元话语 | 0 |

## Validator

基础候选：

```json
{
  "files": 1,
  "rows": 42,
  "uniqueIds": 42,
  "errors": [],
  "warnings": []
}
```

generation overlay：

```json
{
  "files": 1,
  "rows": 1,
  "uniqueIds": 1,
  "errors": [],
  "warnings": []
}
```

overlay 是替换而非追加。只读有效合并再次确认 42 条、42 个唯一 ID，四个教学／审计文本字段精确重复组均为 0。

## 冻结哈希

基础文件 SHA-256：

```text
1743AB9A03D5E3223BD50B0476886C40939F7816B39B5BE6EA8122DB9308F423
```

generation overlay SHA-256：

```text
00638B6BC479C8A36F2D0A2A8CC9F51773BC134E860FEA4427C2AF75621ED63F
```

按基础顺序，以 overlay 替换后，将每条 `id + NUL + prompt + NUL + answer + NUL + hint + NUL + review_note` 用换行连接所得的有效内容指纹 SHA-256：

```text
05769B49739CADC3DD394E0780B351FCE1B721F944CF1326AFDE8BDC06B0AD7B
```

## 状态

Meta Batch10 生成人工稿与机械 QA 已完成并冻结；未写数据库。下一步必须交给未参与本批生成的独立 Agent 逐条复核，在独立复核完成前不得进入数据库传输。
