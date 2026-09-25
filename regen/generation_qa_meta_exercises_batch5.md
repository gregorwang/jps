# Meta 练习生成侧 QA（Batch 5，交叉复核前快照）

## 结论

- 候选文件：`regen/batch5_meta_exercises.json`
- 候选数量：80 条
- 数据范围：
  - EP40：55 条，原练习 `sort_order` 306–393
  - EP41：25 条，原练习 `sort_order` 138–182
- 练习类型：全部为 `sentence_understanding`
- 生成方式：逐条读取 `learning_exercises`、对应日文学习句及真实前后字幕后人工撰写；程序仅用于选集、结构、来源存在性、重复和泄漏校验，没有生成 `prompt`、`answer`、`hint` 或 `review_note`
- 数据库操作：只读 SQL，写入 0

本批把原有 `answer LIKE '用于%'` 的元信息答案改写为可独立学习的日语理解题。内容覆盖口语缩约、条件与让步、可能形、否定范围、名词化、引用、授受视角、省略、回声问句、角色语气、隐喻、跨行话轮及语料转写异常。

## 选集核对

按当前数据库执行只读选集：

```sql
select id, episode, sort_order
from public.learning_exercises
where answer like '用于%'
order by episode, sort_order, id
limit 80;
```

核对结果：

- 数据库当前选集：80 条
- 候选文件：80 条
- 位置级 ID 匹配：80/80
- 多余 ID：0
- 缺失 ID：0
- EP40 / EP41 分布：55 / 25
- 首 ID：`12bc1cfa-9195-526d-a46a-ec3b7d03f4fb`
- 末 ID：`re-zero-ex-ep041-182`
- 以 `|` 连接顺序 ID 的 MD5：
  - 数据库：`5d31a70cd3da7ff653e88945d50ac631`
  - 本地候选：`5d31a70cd3da7ff653e88945d50ac631`

## 学习句与字幕来源

### 来源存在性

- 79 条练习有直接对应的 `learning_sentences` 主来源：
  - EP40：54 条
  - EP41：25 条
- 1 条 EP40 练习 `0881a5f2-d3ba-521f-8c47-838ca562ade0` 的目标「ウソつき！」未作为独立学习句收录，直接定位到真实字幕 line 392；同一字幕前半「好きだ！」对应 `sentence-208`，已在 `review_note` 中作为相邻来源说明。
- 全部 `review_note` 共引用 80 个唯一 sentence ID（79 个主来源加上述 1 个相邻来源）；只读存在性查询返回 80/80，缺失 0。

### 真实字幕核对

- EP40 实际核对范围：lines 319–464。
- EP41 按目标话轮核对的主要范围：lines 5–22、46–110、153–187、213–246、293–342、401–426、452–505。
- 80/80 均定位到真实日文字幕。解释以 `subtitle_lines.ja_text` 与连续话轮为准，没有机械采用可能偏移的 `learning_sentences.source_line_no`，也没有把同一行错位的中文字段当作语义证据。

EP41 有若干学习句是把多行台词整理、删节或跨话轮合并后的学习版本，候选答案与 `review_note` 已明确按真实顺序复原，典型包括：

- `sentence-003`：实际分布于 lines 57、61。
- `sentence-004`：实际相关话语在 lines 66–68，学习句省略了中间的进门脱鞋铺垫。
- `sentence-016`：两部分在 lines 165、167，中间另有 line 166。
- `sentence-018`：实际分布于 lines 183、185–186。
- `sentence-019`：lines 216、219 之间有嘉飞尔对措辞的抗议；第二句是昴改口后的说法。
- `sentence-025`：拉姆的两句分别在 lines 308、310，中间有嘉飞尔插话。
- `sentence-026`：学习句整理了 lines 332、335–337，真实字幕措辞更完整。
- `sentence-031`：两部分在 lines 404、406。
- `sentence-037`：实际话语分布于 lines 471、473–474、479–482。
- `sentence-040`：两部分在 lines 496、499，中间两行仍是艾姬多娜的讥讽。

### 语料异常处理

本批发现并在学习者可见答案中明确校读两处疑似转写错误，未把错误形式包装成可模仿语法：

1. EP41 lines 49–50 / `sentence-002`
   - 语料：`分かれてない`
   - 语境要求：`分かってない`
   - 理由：前者通常是否定「分かれる」（没有分开），无法与“尚不清楚必须做什么”及后句「見つけなくちゃいけない」衔接。
2. EP41 line 410 / `sentence-032`
   - 语料：`区切りはつけてっきたつもりだ`
   - 语境要求：`区切りはつけてきたつもりだ`
   - 理由：固定搭配为「区切りをつける」，再接表示截至当前过程的「てくる」；现有促音位置不构成可教学的正常缩约。

## 学习质量与泄漏

- 题面自足：80/80。每条均含目标日语、必要场景和明确问题。
- 人工针对性：80/80。答案逐句处理真实形态、语义、语用或上下文，没有套用固定答案骨架。
- 上下文边界：对说话人、指代、省略和角色辱骂均区分“字幕可支持的结论”与“不能过度断言的内容”。
- 提示独立性：80/80。提示给出观察路径，不直接复述完整分析。
- 学习者可见字段中的 `用于`、模板、生成器、模型、UUID、内部 source 标识、`review_note` 等泄漏：0。
- `answer` 与 `hint` 去除空格、标点后的最长连续公共片段：最大 7 字。
- 达到 8 字泄漏阈值：0。
- 本批精确重复：prompt 0、answer 0、hint 0。
- 与 Batch 1–4 的逐字段精确重复：prompt 0、answer 0、hint 0。

## 结构与 validator

每条且仅含以下五个非空字符串字段：

```text
id
prompt
answer
hint
review_note
```

执行：

```text
node regen\validate_candidates.mjs regen\batch5_meta_exercises.json
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

附加结构结果：

- 非法字段组合：0
- 空字段：0
- 重复 ID：0
- 数据库写入：0

最终候选文件 SHA-256：

```text
01050c80ba9c96a8d4e345cf0eb230005685f10322c1e3c7b77a629cf69f7b8b
```
