# Meta 练习生成侧 QA（Batch 9）

## 结论

- 基础候选：`regen/batch9_meta_exercises.json`，80 条
- 生成修正 overlay：`regen/generation_overlay_meta_exercises_batch9.json`，1 条
- 有效冻结版本：按基础候选原顺序，以 `id` 为键用 overlay 替换同 ID 对象；最终仍为 80 条
- 数据范围：
  - EP42：14 条，原练习 `sort_order` 332–355
  - EP43：66 条，原练习 `sort_order` 141–260
- 首 ID：`re-zero-ex-ep042-332`
- 末 ID：`re-zero-ex-ep043-260`
- 练习类型：80/80 为 `sentence_understanding`
- 数据库操作：只读查询，写入 0

本批逐条读取原 `learning_exercises`、唯一对应的 `learning_sentences`、EP42 与 EP43 当前日文字幕及连续上下文，然后人工撰写 `prompt`、`answer`、`hint`、`review_note`。程序只用于选集、映射、JSON 结构、重复、泄漏、哈希和数据库漂移 QA；没有用 Python、JavaScript、SQL 循环或语言模板生成教学内容。

## Base + overlay 冻结规则

基础文件写入后，QA 发现 `re-zero-ex-ep043-170` 的题面虽然保留了目标句义，却漏掉原学习句中的读音注记 `(むすめご)`。恢复后的 Windows sandbox helper 一度不允许修改既有文件，因此没有绕过 `apply_patch` 直接改写基础文件，而是新增一个仅含该完整修正对象的 overlay。

有效候选定义：

1. 读取基础文件 80 条并保留原顺序。
2. 读取 overlay 1 条。
3. 遇到相同 `id` 时以 overlay 完整对象替换基础对象。
4. 不追加第二条同 ID 记录。

只读合并验证：

| 检查项 | 结果 |
| --- | ---: |
| base rows | 80 |
| overlay rows | 1 |
| overlay ID 在 base 中命中 | 1 |
| effective rows | 80 |
| effective unique IDs | 80 |
| 五字段结构异常 | 0 |
| 空字段 | 0 |

有效版本中 `re-zero-ex-ep043-170` 的题面完整保留：

```text
その後 魔女様は 娘御(むすめご)を連れて たびたび集落を訪れた
```

## 选集与数据库漂移

只读选集：

```sql
select e.*
from public.learning_exercises e
where e.answer like '用于%'
order by e.episode, e.sort_order, e.id
limit 80;
```

本地有效候选与数据库锁定选集的顺序 ID 复算：

```text
55b97bc3622528249abca3a77531d7f4
```

两边的 ordered comma-MD5 完全相同。最终只读预检：

| 检查项 | 结果 |
| --- | ---: |
| incoming | 80 |
| unique IDs | 80 |
| 当前目标存在 | 80 |
| 备份存在 | 80 |
| 当前答案仍为 `用于…` | 80 |
| 当前整行与备份一致（undrifted） | 80 |
| manifest overlap | 0 |

因此生成和 QA 期间没有原题漂移、提前写入或审计清单冲突。

## Learning sentence 与真实字幕

- 80/80 个 `review_note` 含明确的 `learning_sentences` 来源 ID。
- 来源 ID 共 80 个且全部唯一。
- 数据库中 80/80 来源存在，来源 episode 与练习 episode 一致。
- 原练习引号中的目标日文与对应 `learning_sentences.ja_text`：80/80 精确一致。
- 有效题面完整包含各自目标日文：80/80。

对当前 `subtitle_lines` 做两层核对：

1. 原样或只去空格、常见引号和字幕破折号；
2. 对字幕中额外的读音括注作排版级归一化。

结果：

| 当前字幕匹配 | 数量 |
| --- | ---: |
| 唯一匹配目标 | 78 |
| 因当前字幕重复而命中两行 | 2 |
| 无匹配 | 0 |
| 总命中字幕行 | 82 |

两组重复均是同一目标在当前字幕中连续复现：

- `re-zero-ex-ep043-197`：EP43 lines 123–124
- `re-zero-ex-ep043-250`：EP43 lines 266–267

额外排版差异包括：

- `シー婆(ばあ)` 的字幕读音括注；
- `〝試練〞`、`〝聖域〞`、`“場面を切り替える”` 的引号；
- 跨行尾部破折号；
- EP43 后段 `learning_sentences.source_line_no` 与当前字幕行号存在已知偏移，`review_note` 使用当前字幕实际位置复核，未把旧行号当作内容证据。

## 人工语言学边界

本批重点处理了以下不能靠模板安全生成的边界：

- 跨行未完句：`とき`、`ことのある`、属格 `の`、时间名词 `間`、内嵌疑问 `のか` 等均向相邻字幕恢复主句或中心名词。
- 同形判别：`来られた` 结合 `ご本人` 判作尊敬形；`ていただいてもいい` 按受益请求而非说话人许可解释。
- 角色口语：嘉飞尔的 `気になんな`、`つけてっきた`、`ねえ`，席玛的 `語れん／じゃ／のう`，碧翠丝的角色句尾 `かしら` 均结合现场功能解释，没有推广成标准模板。
- 片假名与长音：儿童说 `セーレージュツシ` 时不因片假名自动判作外来语或误听。
- 语用：反问、放任、威胁、嘴硬、恭敬告退、惊叫和重复恳求分别根据前后行动判定，没有只按句末形式机械贴标签。
- 指代与说话人：`彼ら`、`その人`、`彼`、`それ`、`ご本人` 以及省略主语均由连续话轮约束。
- 不过度推断：没有把 `つきあい` 自动写成恋爱，没有把非词汇叫声伪造为语法，也没有把角色的表面托词当成全知旁白事实。

## 唯一性、泄漏与提示

有效 80 条内部：

| 字段 | 精确重复组 |
| --- | ---: |
| `prompt` | 0 |
| `answer` | 0 |
| `hint` | 0 |
| `review_note` | 0 |

附加检查：

- 学习者可见 `prompt`、`answer`、`hint` 中，旧占位答案、旧 V10 提示、内部 source ID、数据库、manifest、review 定位、模型或生成器信息泄漏：0。
- review 来源缺失：0；review 来源重复：0。
- 不可见空字段与多余字段：0。
- `answer` 与 `hint` 去空格、标点后的最长连续公共片段最大为 8 字，仅 1 条：`re-zero-ex-ep043-193`。重合片段是题目本身的目标结构 `会ったことがない`，提示要求比较其经历时间范围，没有复述答案中的场景结论或人物事实；现有 validator 不报泄漏 warning。

## Validator

基础候选：

```json
{
  "files": 1,
  "rows": 80,
  "uniqueIds": 80,
  "errors": [],
  "warnings": []
}
```

overlay：

```json
{
  "files": 1,
  "rows": 1,
  "uniqueIds": 1,
  "errors": [],
  "warnings": []
}
```

Meta Batch 1–9 基础文件合并校验：

```json
{
  "files": 9,
  "rows": 720,
  "uniqueIds": 720,
  "errors": [],
  "warnings": []
}
```

overlay 是替换而非追加；有效合并后的 80 条再次确认唯一 ID 80、字段精确重复 0、学习者元信息泄漏 0。

## 冻结哈希

基础文件 `regen/batch9_meta_exercises.json`：

```text
932FFD61A59E9ADCF1EE75C9854F7D75FF8A0BA49682645158E92A2EFBE3C891
```

修正文件 `regen/generation_overlay_meta_exercises_batch9.json`：

```text
ED23771DA781ACBC3315229471879B7094585B369BBAABB0659000D3A1AB8762
```

按 `id + NUL + prompt + NUL + answer + NUL + hint + NUL + review_note`、逐行保持基础顺序拼接所得的有效内容指纹 SHA-256：

```text
B316D504F55BA299EF85FC6CC3B196D32D7A77072BCFA3131158F94D5557D5FA
```

以上三个值共同冻结 Batch 9。消费方必须按本报告的 base + overlay 替换规则读取，不能把 overlay 当作第 81 条追加记录。

## 状态

Meta Batch 9 生成人工稿与 QA 已完成，80/80 有效候选冻结。未写数据库，等待独立 reviewer。
