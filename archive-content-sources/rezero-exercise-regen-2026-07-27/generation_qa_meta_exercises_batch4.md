# Meta 练习生成侧 QA（Batch 4，交叉复核前快照）

> 本文保留生成者提交时的只读映射与结构校验快照。独立交叉复核后的修正、复跑结果和最终文件哈希见 `regen/cross_review_meta_exercises_batch4.md`。

## 结论

- 候选文件：`regen/batch4_meta_exercises.json`
- 候选数量：80 条
- 数据范围：EP40，原练习 `sort_order` 218–302（按当前待处理选集排序，中间自然跳过已不在选集的记录）
- 练习类型：`sentence_understanding`
- 生成方式：逐条读取真实日文学习句与前后字幕后人工撰写；脚本仅用于选集、结构、唯一性和泄漏检查
- 数据库操作：只读查询，写入 0

本批把原先仅复述“用于慢读跟读”的元信息答案改成可独立学习的日语理解题。题目覆盖粗口缩约、否定与禁止、使役/被动、话题与对照、回声问句、跨行省略、语用反问、角色称呼、战斗呼声和情绪对话等真实语言点。

## 选集核对

按当前数据库执行以下只读口径：

```sql
select id
from public.learning_exercises
where answer like '用于%'
order by episode, sort_order, id
limit 80;
```

核对结果：

- 数据库当前选集：80 条
- 候选文件：80 条
- 顺序 ID MD5（以 `|` 连接）：数据库与本地均为 `5081241cc7a0eadf3076b429f050aab8`
- 位置级匹配：80/80
- 多余 ID：0
- 缺失 ID：0
- 全部属于 EP40

## 学习句与字幕来源

每条 `review_note` 均明确给出一个主 `learning_sentences` 来源：

- 来源引用：80 条
- 非空来源：80/80
- 唯一来源：80/80
- 来源按顺序的 MD5：数据库推导与本地引用均为 `8744f5e1763a8a1581513158793400a0`
- 来源范围：
  - `re-zero-s02e15-sentence-080` ～ `104`
  - `re-zero-s02e15-sentence-106` ～ `134`
  - `re-zero-s02e15-sentence-136` ～ `151`
  - `re-zero-s02e15-sentence-154` ～ `156`
  - `re-zero-s02e15-sentence-158` ～ `164`

对真实 EP40 日文 `subtitle_lines` 的文本核对：

- 去除空格后整行精确一致：77/80
- 真实字幕完整子片段：3/80
- 无来源匹配：0

三个子片段均是数据库已有学习句对真实字幕行的完整截取，不是补写：

- `sentence-095` 的「エル･フーラ！」来自 line 204「ラム！ エル･フーラ！」
- `sentence-129` 的「やっと見つけた」来自 line 262「やっと見つけた あっ…」
- `sentence-137` 的「安心しただけ？」来自 line 274「安心しただけ？ ん？」

数据库 `learning_sentences.source_line_no` 在本段存在系统性偏移，因此本批没有机械采用该字段，而是按标准化日文文本在真实字幕中重新定位；实际核对范围为 EP40 lines 178–321。

## 学习质量检查

- 自足性：80/80。题面包含目标日语、必要场景和明确问题。
- 人工针对性：80/80。答案分别解释该句的具体形态、语用或上下文，不复用占位模板。
- 提示独立性：80/80。提示只给观察路径，不直接复述完整结论。
- `answer` 与 `hint` 去除空格、标点后的最长连续公共片段：最大 7 字，未达到 8 字泄漏阈值。
- 学习者可见字段中未出现 `用于`、模板、生成器、模型、UUID、source ID、`review_note` 或 `source_line` 等内部元数据。
- 本批精确重复：prompt 0、answer 0、hint 0。
- 与 Batch 1–3 的逐字段精确重复：prompt 0、answer 0、hint 0。

## 结构校验

执行：

```text
node regen\validate_candidates.mjs regen\batch4_meta_exercises.json
```

结果：

```json
{
  "files": 1,
  "rows": 80,
  "uniqueIds": 80,
  "errors": [],
  "warnings": []
}
```

附加检查：

- 每条且仅含 `id`、`prompt`、`answer`、`hint`、`review_note`
- 五个字段均为非空字符串
- 重复 ID：0
- 数据库写入：0

最终候选文件 SHA-256：

```text
D8D78068A38925E04F6ABBCBCC6AC867B005F48B8ECE65FAC0A9A56C615A3DC2
```
