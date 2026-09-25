# Meta 练习独立交叉复核与冻结报告（Batch 6）

复核日期：2026-07-27

## 结论

- 复核文件：`regen/batch6_meta_exercises.json`
- 逐条复核：80/80
- 原稿通过：74
- 修订后通过：6
- 拒绝：0
- 最终状态：80/80 已冻结，可进入写库阶段
- 数据库操作：仅执行只读查询，写入 0

本轮以独立 reviewer 身份逐条读取候选题面、答案、提示和审校备注，并对照原 `learning_exercises`、对应 `learning_sentences`、EP41 当前 `subtitle_lines` 及本地 Netflix 日文 CC 连续话轮。复核范围包括 ID/source 映射、目标句、说话人、听者、指代、省略、形态还原、语义角色、语用功能、证据边界、题面自足性、hint 泄漏和重复文本。

程序只用于只读选集、集合/哈希比对、结构、重复、公共片段和 validator 检查，没有生成或改写任何教学内容。六处教学文本均由 reviewer 依据真实语料直接修订。

生成者释放时的快照 `regen/generation_qa_meta_exercises_batch6.md` 保持不变；本文记录交叉复核后的最终状态。

## 选集与数据库状态

独立只读复算继续采用以下口径：

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
- EP41：80/80。
- `sentence_understanding`：80/80。
- 原 `sort_order`：183–270。
- 原答案精确等于旧元信息模板：80/80。
- 已应用 manifest 重叠：0。
- 数据库与本地按顺序拼接 ID 的 MD5 均为：

```text
639fd9edb3017daf8f0d09eb9069f9b4
```

这说明交叉复核期间目标集合没有漂移，也没有发生并发写入。

## Learning sentence、目标句与真实字幕

- `review_note` 引用 `learning_sentences`：80/80。
- 引用的 source ID 唯一：80/80；数据库存在：80/80。
- 顺序 source ID 的 MD5，数据库与本地均为：

```text
155a661d6c5be93e130f2bf624524448
```

- 原练习目标日文与所引 `learning_sentences.ja_text` 逐字一致：80/80。
- 每个最终 `prompt` 均逐字包含对应目标日文：80/80。
- 顺序目标日文的 MD5，原练习、learning sentence 与本地题面均为：

```text
dbc7f55bbd9d051d89c68cda00442eb7
```

- 80 个目标日文在 EP41 当前 `subtitle_lines.ja_text` 中全部精确命中。
- 精确命中字幕共 81 行，当前行号范围 19–354。
- 唯一多重命中为 `re-zero-s02e16-sentence-125` 的「勝手に見限ってんじゃねえ！」，位于 current lines 183、306；本批 `re-zero-ex-ep041-261` 取后一次拉姆—嘉飞尔话轮，并已明确记录两个命中。

交叉复核还逐段读取了本地 Netflix 日文 CC 的说话人标注。`subtitle_lines` 将换人标记清理掉时，以 CC 标注、连续话轮、形态和语义共同判定，不按字符串首次命中或学习句的旧 `source_line_no` 机械归属说话人。

## 交叉复核修订

| 练习 ID | 原稿问题 | 最终处理 |
| --- | --- | --- |
| `re-zero-ex-ep041-229` | 把「何度でも」误写成爱蜜莉雅本人刚刚表明的内容 | 区分发话者：爱蜜莉雅本人以恐惧经历劝说；“即使失败也会反复挑战”由昴在前文说出。保留点名盟友作为现场见证的语用分析 |
| `re-zero-ex-ep041-231` | 将「ここまでされちゃ認めるしかねえ」误归嘉飞尔，并误接成他战后认输 | 按 CC 连续话轮改为昴发言；结合「どんだけタフ」说明他被迫承认嘉飞尔的强悍与韧性。下一话轮嘉飞尔仍继续进攻，不能接到战后「負けたのは認める」 |
| `re-zero-ex-ep041-232` | 把内心独白中的「あの」限定成依赖“双方共享经历” | 改为依赖昴自己的既往经历和前文记忆；明确「あの」在没有现场听者的独白中也能锁定艾姬多娜 |
| `re-zero-ex-ep041-245` | 将发出动作/叫声的帕特拉修误写成爱蜜莉雅，并否认现场存在语言差异 | 按紧邻的「パトラッシュの鳴き声」改正对象；说明奥托把动物表达转成人话，现场确有跨物种沟通差异，引号内容仍是翻译而非帕特拉修直接说出的日文字幕 |
| `re-zero-ex-ep041-246` | 将「心配させないでよ」中被迫担心者误判为爱蜜莉雅 | 改为帕特拉修：昴使帕特拉修担心；引号加「だろ？」表示昴先猜她的意思，奥托随后追加译文 |
| `re-zero-ex-ep041-269` | 将试炼中的嘉飞尔发声武断写成幼年角色当场对白 | 改为嘉飞尔面对童年场景发声、重现当时孩子的心情；日文 CC 对婴儿声音另标「赤ん坊のガーフィール」，故不把完整台词强归给画面中的婴幼儿 |

六处修订均已写入对应 `review_note`，没有改变 ID、顺序、source 映射、目标日文或五字段结构。

## 重点保留判定

- `re-zero-ex-ep041-205`：当前字幕把「そうじゃなきゃ… 違う！」合在一行，但日文 CC 明确显示昴的未完条件被嘉飞尔「違う！」打断；保留两个话轮的分析。
- `re-zero-ex-ep041-210`：「放り捨ててって」按语境切为「放り捨てていって」的口语收缩，承接母亲抛下负担后离开；不把末尾误当成独立引用句。
- `re-zero-ex-ep041-254`：只可靠还原「ざっけんな」为「ふざけんな」；对单音残片「ばっ！」继续保持证据边界，不擅补唯一词。
- `re-zero-ex-ep041-260`：`subtitle_lines` 与 `learning_sentences` 均写「否定したくっなる」，标准活用只能是「否定したくなる」；继续明确标为疑似转写异常，不将错误促音包装成口语规则。
- `re-zero-ex-ep041-261`：同句两次出现，本题按来源顺序和相邻话轮取 current line 306，不采用首次字符串命中。
- `re-zero-ex-ep041-268`、`270`：对角色粗口、残句和夸张促音只说明可证实的会话功能，不虚构唯一词源或心理事实。

## 自足性、泄漏与重复

- 题面自足：80/80；每题含目标日语、必要场景和明确问题。
- 说话人、指代、形态、语义角色和证据边界：80/80 已按连续话轮复核。
- 每条且仅含 `id`、`prompt`、`answer`、`hint`、`review_note` 五个非空字符串字段。
- 学习者可见的 `prompt`、`answer`、`hint` 中，旧元信息答案、内部 source ID、UUID、`review_note`、batch/regen、生成器或模型信息泄漏：0。
- `answer` 与 `hint` 去除空格、标点后的最长连续公共片段：最大 7 字。
- 达到 8 字 hint 泄漏阈值：0。

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

对 `prompt + answer + hint` 去除空格、标点后建立连续 20 字符窗口，Batch 6 与 Batch 1–5 的跨卡复用组：0。

## Validator

本批：

```text
node regen\validate_candidates.mjs regen\batch6_meta_exercises.json
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

## 冻结哈希

生成者释放时的候选 SHA-256：

```text
EE6547827F187702A76A7A7DD298C8EE69828227EA1B3D4948CF13251D8FAFE1
```

交叉复核后的最终 SHA-256：

```text
2D0CA76F1A81A5B6621DFA8326381C7C12721EC18EBE3EF37518C07BBCAA8333
```

最终判定：原稿通过 74 条、修订后通过 6 条、拒绝 0 条。`regen/batch6_meta_exercises.json` 已冻结，可写库；本复核任务没有执行任何数据库写入。
