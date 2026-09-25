# Meta 练习独立交叉复核与冻结报告（Batch 7）

复核日期：2026-07-27

## 结论

- 复核文件：`regen/batch7_meta_exercises.json`
- 逐条复核：80/80
- 原稿通过：73
- 修订后通过：7
- 拒绝：0
- 最终状态：80/80 已冻结，可进入写库阶段
- 数据库操作：仅执行只读查询，写入 0

本轮以独立 reviewer 身份逐条读取候选的 `prompt`、`answer`、`hint`、`review_note`，并对照原 `learning_exercises`、对应 `learning_sentences`、当前 `subtitle_lines` 连续话轮及本地 Netflix 日文 CC。复核范围包括正确选集、ID/source 映射、目标句、说话人、指代、省略、形态还原、语义角色、语用功能、证据边界、题面自足性、hint 泄漏和跨批重复。

程序只用于只读选集、集合与哈希比对、结构、重复、公共片段和 validator 检查，没有生成或改写教学文本。七条修订均由 reviewer 根据真实语料逐条重写。

生成者释放时的快照 `regen/generation_qa_meta_exercises_batch7.md` 保持不变；本文记录交叉复核后的最终状态。

## 选集与数据库状态

独立只读复算采用以下口径：

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

结果：

- 数据库当前选集：80 条，唯一 ID 80。
- 本地候选：80 条，唯一 ID 80。
- 数据库与本地位置级 ID 匹配：80/80。
- EP41：30 条，原 `sort_order` 271–301；其中 292 已由既有 manifest 排除，所以本批不含该 ID。
- EP42：50 条，原 `sort_order` 138–213。
- `sentence_understanding`：80/80。
- 已应用 manifest 重叠：0。
- 数据库与本地按顺序拼接 ID 的 MD5 均为：

```text
e76e5e46d5ec7b80f145103fa8ebf788
```

这说明交叉复核期间目标集合没有漂移，也没有发生并发写入。

## Learning sentence、目标句与真实字幕

- `review_note` 引用 `learning_sentences`：80/80。
- 引用的 source ID 唯一：80/80；数据库存在：80/80。
- source episode 与练习 episode 一致：80/80。
- 数据库与本地按顺序拼接 source ID 的 MD5 均为：

```text
ed0f92941fd877c96704499b4d28e17e
```

- 原练习目标日文与所引 `learning_sentences.ja_text` 逐字一致：80/80。
- 每个最终 `prompt` 均逐字包含对应目标日文：80/80。
- 顺序目标日文的 MD5 为：

```text
17367ba8497ff73d34b25af2dc35dcab
```

- 77/80 个目标日文在当前 `subtitle_lines.ja_text` 中逐字命中，共命中 78 行。
- 多出的 1 行来自「ひゃ～！ ささっ」在 EP42 current lines 82、205 各出现一次；`re-zero-ex-ep042-168` 依据 source 顺序与连续潜行动作取首次场景。
- 另外 3 条只有字幕排版符号差异，词形与语义一致：
  1. `re-zero-ex-ep042-152`：source `ここはお姫様部屋`；current line 46 为 `ここは“お姫様部屋”`。
  2. `re-zero-ex-ep042-177`：current line 107 在句尾另有跨行破折号 `ーー`。
  3. `re-zero-ex-ep042-179`：current line 109 给被引用的 `このような場所` 加了引号。

交叉复核连续读取了 EP41 current lines 345–509、EP42 current lines 1–215，并复查对应本地 Netflix 日文 CC。CC 的说话人标记用于确认琉兹／席玛、嘉飞尔／昴、爱蜜莉雅／艾姬多娜、佛尔特娜／裘斯／亚齐等换人边界；没有按孤立字幕行或旧中文释义猜测说话人。

## 交叉复核修订

| 练习 ID | 原稿问题 | 最终处理 |
| --- | --- | --- |
| `re-zero-ex-ep041-287` | 把“十四岁通常对应初二”写成过宽泛的年龄—年级对应 | 改为日本中学二年级学生通常在该学年满十四岁，并明确昴依据 `今年でようやく14` 当场换算；保留 `中２` 与“中二病”形象的第二层笑点 |
| `re-zero-ex-ep041-300` | 把嘉飞尔看见的 `必死こいた結果` 误写成昴“拼命又难堪的样子” | 改为昴留在墓所里给爱蜜莉雅的鼓励文字；区分 `うるせえ` 顶回夸奖与 `忘れろ` 要嘉飞尔忘掉所见成果，不扩大成整场战斗 |
| `re-zero-ex-ep041-301` | 把 `どうか` 误解为指向“某种回应” | 改为请求／恳求的强化成分，明确它不是不定代词 `何か`；结合连续辱骂与 `魔女の娘`，把表面请求、提议判为讽刺性施压 |
| `re-zero-ex-ep042-138` | `review_note` 将说话人无必要地限定为“幼年爱蜜莉雅” | 删除当前字幕和 CC 无法支持的年龄标签，只保留“爱蜜莉雅复述帕克说法” |
| `re-zero-ex-ep042-151` | 将 `あった` 的主语模糊写成“对象或意图” | 明确悬空的 `ものが` 是 `あった` 的主语；封存意图由 `閉じ込めておきたい` 表达，`と見える` 管辖整个观察推断 |
| `re-zero-ex-ep042-195` | 将 `どうしよ` 绝对化为“长音未完全说出” | 改为 `どうしよう` 的口语缩略；本行的省略号与紧接惊叫又赋予它发话中断效果，不把两层现象混为唯一解释 |
| `re-zero-ex-ep042-205` | `それ` 混入“想说出口”，并把 `相手` 笼统写成所有被评价者 | 将 `それ` 限定为假设中的负面看法；`相手` 是传达接收者、此处首先为眼前的爱蜜莉雅。随后改写 hint，消除与答案的 11 字连续重合 |

七条修订都没有改变 ID、顺序、source 映射、目标日文或五字段结构。

## 重点保留判定

- `re-zero-ex-ep041-272`：`知らないはずない` 按双层否定解释为“不可能不知道”，结合下一话轮把省略主语判为芙蕾德莉卡。
- `re-zero-ex-ep041-275`、`276`：Netflix CC 分别标明琉兹与席玛，保留两人连续呼喊嘉飞尔的换人边界。
- `re-zero-ex-ep041-293`：`いま一歩` 按“还差一步”解释；`ギルティラウ` 保留为作品内名称，不伪造成普通日语成语。
- `re-zero-ex-ep041-298`：`してちゃ` 还原为 `していては`，`みい` 按带角色语色彩的命令形式处理，不扩写不可证实的方言来源。
- `re-zero-ex-ep042-153`：`遊ばされていた` 中爱蜜莉雅是被安排留在房内玩耍的一方，保留使役被动分析。
- `re-zero-ex-ep042-168`：同句两次出现，本题按 source 顺序和连续潜行动作采用首次场景，不机械取任一字符串命中。
- `re-zero-ex-ep042-177`、`178`：把 `不便を強いられるなど` 与下一行 `あってはならないことですよ` 作为跨行完整句处理。
- `re-zero-ex-ep042-194`–`197`：四条相邻 `どうしよう` 保持为同一场清理慌乱中的重复、缩略／中断、字幕分块与哭腔，不为每行杜撰新事件。

## 自足性、泄漏与重复

- 题面自足：80/80；每题含目标日语、必要场景和明确问题。
- 说话人、指代、形态、语义角色和证据边界：80/80 已按连续话轮复核。
- 每条且仅含 `id`、`prompt`、`answer`、`hint`、`review_note` 五个非空字符串字段。
- 学习者可见的 `prompt`、`answer`、`hint` 中，旧元信息答案、内部 source ID、UUID、`review_note`、batch/regen、数据库、生成器或模型信息泄漏：0。
- `answer` 与 `hint` 去除空格、标点后的最长连续公共片段：最大 7 字，出现在 `re-zero-ex-ep042-192`。
- 达到 8 字 hint 泄漏阈值：0；hint 完整包含答案：0。
- 不可见格式字符：0。

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
- Batch 1–7 合并：560 条，唯一 ID 560。
- 对 `prompt + answer + hint` 去除空格、标点后建立连续 20 字符窗口，涉及 Batch 7 的跨批复用组：0。

## Validator

本批：

```text
node regen\validate_candidates.mjs regen\batch7_meta_exercises.json
```

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

## 冻结哈希

生成者释放时的候选 SHA-256：

```text
05086C637F8F4B92108A1A9FAA050F7977F5780F8A71C487DA5B2BBA12BD97BD
```

交叉复核后的最终候选大小与 SHA-256：

```text
71655 bytes
D809C3996F73B41C4A17CE273488AEB622D4224C8F3F858355AE531477AB42A1
```

最终判定：原稿通过 73 条、修订后通过 7 条、拒绝 0 条。`regen/batch7_meta_exercises.json` 已冻结，可写库；本复核任务没有执行任何数据库写入。
