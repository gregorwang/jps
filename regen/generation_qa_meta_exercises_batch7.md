# Meta 练习生成侧 QA（Batch 7，交叉复核前快照）

## 结论

- 候选文件：`regen/batch7_meta_exercises.json`
- 候选数量：80 条
- 数据范围：
  - EP41：30 条，原练习 `sort_order` 271–301
  - EP42：50 条，原练习 `sort_order` 138–213
- 首 ID：`re-zero-ex-ep041-271`
- 末 ID：`re-zero-ex-ep042-213`
- 练习类型：80/80 为 `sentence_understanding`
- 生成方式：逐条读取原 `learning_exercises`、对应 `learning_sentences`、当前 `subtitle_lines` 连续话轮及本地 Netflix 日文 CC 后，人工撰写 `prompt`、`answer`、`hint`、`review_note`
- 程序用途：仅用于只读选集、位置与 source 映射、JSON 结构、重复、连续片段、提示泄漏和哈希校验；没有生成或改写教学文本
- 数据库操作：仅执行只读 SQL，写入 0

本批把两类不能作为学习材料的占位答案改写为可独立作答的日语理解题：

- EP41 的 30 条原答案为「用于理解本集人物关系、情绪和剧情推进的关键句。」
- EP42 的 50 条原答案为「用于本集慢读跟读的完整台词。」

内容覆盖嘉飞尔结束旧创伤、与同伴和解、爱蜜莉雅进入新试炼、幼年记忆、佛尔特娜与裘斯的交谈，以及艾姬多娜对记忆空间的说明。题目逐条处理否定辖域、回声问句、话题化、缩约、复合谓语、引用、名词化、使役被动、零主语、拟声拟态、固定搭配、跨行句法和语用错位。

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

- 数据库当前选集：80 条，唯一 ID 80 个。
- 本地候选：80 条，唯一 ID 80 个。
- 位置级 ID 匹配：80/80。
- EP41：30 条，`sort_order` 271–301。
- EP42：50 条，`sort_order` 138–213。
- 已应用 manifest 重叠：0。
- 以 `|` 连接顺序 ID 的 MD5：
  - 数据库：`e76e5e46d5ec7b80f145103fa8ebf788`
  - 本地：`e76e5e46d5ec7b80f145103fa8ebf788`

这次没有在 manifest 已排除 Batch 6 的基础上再加 `offset`。此前错误的全 EP42 偏移预选未写入候选文件，正式候选自第一条起即使用上述当前数据库前 80 条。

## Learning sentence 与真实字幕

### 来源映射

- 80/80 条 `review_note` 含明确的 `learning_sentences` 来源 ID。
- 来源 ID 共 80 个且全部唯一。
- 数据库存在性：80/80。
- 来源 episode 与练习 episode 一致：80/80。
- 原练习目标日文与所选 `learning_sentences.ja_text` 逐字匹配：80/80。
- 新 `prompt` 逐字包含原练习目标日文：80/80。

### 当前日语字幕

- 77/80 个目标日文在当前 `subtitle_lines.ja_text` 中逐字命中。
- 逐字命中字幕行共 78 行；行数多 1 是因为「ひゃ～！ ささっ」在 EP42 当前 lines 82、205 出现两次。
- 本题 `re-zero-ex-ep042-168` 依据原练习顺序、来源句 `sentence-032` 和相邻潜行动作选择 line 82 的首次场景，`review_note` 已明确记录另一命中。
- 另外 3 条仅有字幕排版符号差异，日文词形和语义一致：
  1. `re-zero-ex-ep042-152`
     - source：`ここはお姫様部屋`
     - current line 46 / CC block 47：`ここは“お姫様部屋”`
  2. `re-zero-ex-ep042-177`
     - source：`あなた方は このような場所で 不便を強いられるなど`
     - current line 107 / CC block 109 在末尾另有跨行破折号 `ーー`
  3. `re-zero-ex-ep042-179`
     - source：`このような場所なんて 言わないの`
     - current line 109 / CC block 111 给被引用的「このような場所」加了引号

除数据库字幕外，还连续复查：

- EP41 当前 lines 350–509，以及 Netflix CC blocks 365–525。
- EP42 当前 lines 1–200，以及 Netflix CC blocks 1–195。

Netflix CC 的说话人标注用于确认芙蕾德莉卡／嘉飞尔、琉兹／席玛、昴／爱蜜莉雅、艾姬多娜／爱蜜莉雅、佛尔特娜／裘斯／亚齐之间的换人边界。候选没有根据单行旧中文释义猜说话人，也没有把相邻人物的话拼成同一人的句子。

### 重点证据边界

1. `re-zero-ex-ep041-272`
   - 「知らないはずない」按双层否定解释为“不可能不知道”。
   - 结合下一句复述与祖母告知的论证，省略主语按芙蕾德莉卡处理，不误教成“你不应该知道”。
2. `re-zero-ex-ep041-287`
   - 「中２」先按十四岁对应的中学二年级解释，再说明它与「中二病」形象形成双关；没有只保留译文笑点。
3. `re-zero-ex-ep041-293`
   - 「いま一歩」按“还差一步”解释；`ギルティラウ` 保留为作品内名称和嘉飞尔的格言式角色语，没有伪造普通日语固定成语。
4. `re-zero-ex-ep042-153`
   - 「遊ばされていた」按受安排的一方解释，并结合被锁在房内的连续场景说明使役被动含义。
5. `re-zero-ex-ep042-168`
   - 同一句在本集出现两次；候选按前一次幼年爱蜜莉雅潜行话轮讲解，未机械取后一次。
6. `re-zero-ex-ep042-177`、`178`
   - 把「不便を強いられるなど」与下一行「あってはならないことですよ」作为跨行完整句处理，没有把前半残句伪装成独立命题。
7. `re-zero-ex-ep042-194`–`197`
   - 四条相邻「どうしよう」分别按连续焦虑中的重复、截断、字幕分块和哭腔解释；没有为每一字幕行杜撰新的剧情事件。

## 人工原创与学习质量

- 题面自足：80/80。每条包含目标日语、必要场景和明确可操作的问题。
- 答案针对性：80/80。每条回答真实词形、句法、语义、语用或前后照应问题。
- 提示独立性：80/80。提示只给观察路径，不复述完整答案。
- 学习者可见的 `prompt`、`answer`、`hint` 中，旧占位答案、内部 source ID、UUID、review 定位、模型或生成器信息泄漏：0。
- `answer` 与 `hint` 去除空格、标点后的最长连续公共片段：
  - 最大 7 字：`めっそうもない`
  - 达到 8 字提示泄漏阈值：0。
- 不可见格式字符：0。

## 唯一性、去模板与结构

本批内部精确重复：

| 字段 | 重复组 |
| --- | ---: |
| `prompt` | 0 |
| `answer` | 0 |
| `hint` | 0 |
| `review_note` | 0 |

与 Meta Batch 1–6 共 480 条既有候选相比：

| 字段 | 精确重复 |
| --- | ---: |
| `prompt` | 0 |
| `answer` | 0 |
| `hint` | 0 |
| `review_note` | 0 |

附加结果：

- 与 Batch 1–6 的 ID 重叠：0。
- Batch 1–7 合并：560 条，唯一 ID 560 个。
- 对 `prompt + answer + hint` 去除空格、标点后建立连续 20 字符窗口，涉及本批的跨卡复用组：0。
- 每条且仅含 `id`、`prompt`、`answer`、`hint`、`review_note` 五个非空字符串字段：80/80。
- 空字段：0。
- review 来源缺失：0；review 来源重复：0。

## Validator

本批：

```text
node regen\validate_candidates.mjs regen\batch7_meta_exercises.json
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

Meta Batch 1–7 合并：

```json
{
  "files": 7,
  "rows": 560,
  "uniqueIds": 560,
  "errors": [],
  "warnings": []
}
```

## 文件哈希

`regen/batch7_meta_exercises.json`

```text
05086C637F8F4B92108A1A9FAA050F7977F5780F8A71C487DA5B2BBA12BD97BD
```

## 状态

生成侧 QA 已通过，`regen/batch7_meta_exercises.json` 自上述校验与哈希起冻结，不再编辑。**可以释放给独立 reviewer。** 本轮没有执行任何数据库写入。
