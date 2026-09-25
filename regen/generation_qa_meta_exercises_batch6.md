# Meta 练习生成侧 QA（Batch 6，交叉复核前快照）

## 结论

- 候选文件：`regen/batch6_meta_exercises.json`
- 候选数量：80 条
- 数据范围：全部为 EP41，原练习 `sort_order` 183–270
- 首 ID：`re-zero-ex-ep041-183`
- 末 ID：`re-zero-ex-ep041-270`
- 练习类型：80/80 为 `sentence_understanding`
- 生成方式：逐条读取原 `learning_exercises`、对应 `learning_sentences`、当前 `subtitle_lines` 日文及相邻话轮后人工撰写 `prompt`、`answer`、`hint`、`review_note`
- 程序用途：仅用于只读选集、ID/source 映射、JSON 结构、重复、连续片段、提示泄漏及哈希校验，没有生成任何教学文本
- 数据库操作：仅执行只读 SQL，写入 0

本批把原来完全相同的元信息答案「用于理解本集人物关系、情绪和剧情推进的关键句。」改写为能够独立作答的日语理解题。内容集中于嘉飞尔与众人的冲突、丧母创伤、战斗转折、拉姆的含蓄互动及试炼回忆，逐条处理口语缩约、条件与让步、使役、被动、引用、名词化、省略、回声、角色语、比喻、说话人转换和转写异常。

## 选集与数据库映射

只读选集口径：

```sql
select e.*
from public.learning_exercises e
where e.answer like '用于%'
  and not exists (
    select 1
    from maintenance.regen_20260727_manifest m
    where m.table_name = 'public.learning_exercises'
      and m.row_id = e.id
      and (m.applied_at is not null or m.review_status = 'applied')
  )
order by e.episode, e.sort_order, e.id
limit 80;
```

复算结果：

- 数据库当前选集：80 条。
- 本地候选：80 条、80 个唯一 ID。
- 位置级 ID 匹配：80/80。
- EP 分布：EP41 共 80 条。
- 原 `sort_order` 范围：183–270。
- 原 `answer` 精确等于旧元信息模板：80/80。
- 已应用 manifest 重叠：0。
- 以 `|` 连接顺序 ID 的 MD5：
  - 数据库：`639fd9edb3017daf8f0d09eb9069f9b4`
  - 本地：`639fd9edb3017daf8f0d09eb9069f9b4`

## Learning sentence 与真实字幕

### 来源映射

- 80/80 条 `review_note` 含明确的 `learning_sentences` 来源 ID。
- 来源 ID 共 80 个且全部唯一，数据库存在性 80/80。
- 原练习目标日文与对应 `learning_sentences.ja_text` 逐字匹配 80/80。
- 每个新 `prompt` 均逐字包含原练习目标日文，80/80；没有只写抽象题目而遗漏待分析台词。
- 来源句 episode 与练习 episode 一致 80/80。

### 当前日语字幕

- 80/80 个目标日文在 EP41 当前 `subtitle_lines.ja_text` 中精确命中。
- 精确命中字幕行共 81 行，范围为当前 lines 19–354。
- 多出的一行来自 `勝手に見限ってんじゃねえ！`：当前字幕在 lines 183、306 各出现一次。本题 `re-zero-ex-ep041-261` 依据来源句、候选顺序和相邻话轮选择后一次，并在 `review_note` 中明确记录两个命中，未机械采用首次匹配。
- 除数据库字幕外，还复查了本地同集 Netflix 日文 CC 源文件中的说话人标注，用于确认被 `subtitle_lines` 去掉的换人边界；教学结论仍以真实日文和连续话轮为依据。

### 重点证据边界

1. `re-zero-ex-ep041-205`
   - 当前 line 89 把「そうじゃなきゃ…」与「違う！」收在一行。
   - 源 CC 明确显示前半由昴继续、后半由嘉飞尔打断，候选没有把它误教成同一人的自我修正。
2. `re-zero-ex-ep041-260`
   - `subtitle_lines` 与 `learning_sentences` 都写作「否定したくっなる」。
   - 标准活用应为 `否定する → 否定したい → 否定したくなる`，中间促音没有可成立的形态来源；候选明确标为疑似转写异常，不把错误形式包装成口语规则。
3. `re-zero-ex-ep041-261`
   - 同一句在本集出现两次；候选按后一次拉姆—嘉飞尔话轮讲解，同时指出较早一次由昴说出。
4. `re-zero-ex-ep041-191`
   - 保留 source 中的读音标注 `悪(わり)い`，同时按前后决策范围解释 `それ以外の全部`，不扩大成“世间万物”。
5. `re-zero-ex-ep041-197`、`233`、`254`
   - 分别处理愤怒残句、无主句名词短语和不完整爆音；仅恢复上下文必需成分，不声称存在唯一省略谓语或唯一词首。
6. `re-zero-ex-ep041-245`、`246`
   - 把「通訳」解释为对动作和情绪的幽默拟译；引号中的「心配させないでよ」是人物推测的言外之意，不伪造为爱蜜莉雅实际说出的字幕。

## 人工原创与学习质量

- 题面自足：80/80。每条给出目标日语、必要场景和可操作的问题。
- 答案针对性：80/80。每条按真实词形、句法、语义、语用或前后照应作答，没有套用旧 `tone` 标签。
- 提示独立性：80/80。提示只给观察路径，不复述完整答案。
- 说话人、指代和省略均以连续话轮为界，不把心理推断、人物说法或幽默命名写成旁白事实。
- 学习者可见的 `prompt`、`answer`、`hint` 中，旧元信息模板、UUID、内部 source ID、`review_note`、模型、生成器、EP/line 定位等泄漏：0。
- `answer` 与 `hint` 去除空格、标点后的最长连续公共片段：最大 7 字。
- 达到 8 字提示泄漏阈值：0。

## 唯一性、去模板与结构

本批内部精确重复：

| 字段 | 重复组 |
| --- | ---: |
| `prompt` | 0 |
| `answer` | 0 |
| `hint` | 0 |
| `review_note` | 0 |

与 Meta Batch 1–5 共 400 条既有候选相比：

| 字段 | 精确重复 |
| --- | ---: |
| `prompt` | 0 |
| `answer` | 0 |
| `hint` | 0 |
| `review_note` | 0 |

附加结果：

- 与 Batch 1–5 的 ID 重叠：0。
- 对 `prompt + answer + hint` 去除空格、标点后建立连续 20 字符窗口，跨卡复用组：0。
- 每条且仅含 `id`、`prompt`、`answer`、`hint`、`review_note` 五个非空字符串字段：80/80。
- 空字段：0。
- review 来源缺失：0；review 来源重复：0。

## Validator

本批：

```text
node regen\validate_candidates.mjs regen\batch6_meta_exercises.json
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

Meta Batch 1–6 合并：

```json
{
  "files": 6,
  "rows": 480,
  "uniqueIds": 480,
  "errors": [],
  "warnings": []
}
```

## 文件哈希

`regen/batch6_meta_exercises.json`

```text
EE6547827F187702A76A7A7DD298C8EE69828227EA1B3D4948CF13251D8FAFE1
```

## 状态

生成侧 QA 已通过，`regen/batch6_meta_exercises.json` 自上述校验与哈希起冻结，不再编辑。**可以释放给独立 reviewer。** 本轮没有执行任何数据库写入。
