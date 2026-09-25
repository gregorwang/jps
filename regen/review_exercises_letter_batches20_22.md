# Letter exercises batches 20–22 独立交叉审校

## 结论

本轮独立审校了 EP19 exercise 049–168 的 120 条候选。审校重新读取当前数据库原题、相关 `learning_vocab_items` / `learning_vocab_occurrences` / `learning_sentences` / `learning_grammar_points`，并逐条对照 EP19 真实日文字幕及连续话轮；没有把生成侧报告当作复核结论。

| 批次 | 范围 | 条数 | 原稿保留 | 审校修订 | 拒绝 |
| --- | --- | ---: | ---: | ---: | ---: |
| batch20 | EP19 exercise 049–088 | 40 | 40 | 0 | 0 |
| batch21 | EP19 exercise 089–128 | 40 | 37 | 3 | 0 |
| batch22 | EP19 exercise 129–168 | 40 | 38 | 2 | 0 |
| **合计** | **EP19 exercise 049–168** | **120** | **115** | **5** | **0** |

三份 JSON 已按本报告末尾哈希冻结，可进入写库阶段。本轮数据库操作全部为只读 `SELECT`，没有写数据库。

## 原稿锁定

审校开始前锁定的生成稿 SHA-256：

- `batch20_exercises_letter.json`：`CBA3606A4D605E7619C97E02FA4BEC813842406EA95DBC4E090AFA70EC619043`
- `batch21_exercises_letter.json`：`C0FBAC30EC2CB13FB0E0DDDC7DF05A5A7E2872DAE6F03EE4E88AFEC191837B89`
- `batch22_exercises_letter.json`：`2A9638DB8DACB9039FE6EF76E9C145C46F496A4EC3C9338A2A8EA4CDAB8AB7FD`

## 数据库映射与真实来源

- 120/120 个候选 ID 精确映射当前 `public.learning_exercises`，有序范围为 `re-zero-s01e19-exercise-049` 至 `re-zero-s01e19-exercise-168`，没有缺失、额外、重复或位置错配。
- 本地候选与数据库有序 ID MD5 均为 `12b460282bf66db3fb7afffc33fe588d`。
- 原题类型为 `vocab_reading` 56 条、`vocab_meaning` 56 条、`grammar_meaning` 8 条。
- 去除空白、标点、引号和括号注音后，120/120 条数据库原题 source 均精确命中 EP19 当前 `subtitle_lines.ja_text`。
- 112/112 条词汇题均精确提取目标表层并映射 `learning_vocab_items`；共 47 个不同表层。112/112 条同时命中 EP19 `learning_vocab_occurrences.example_line_nos` 所记录的真实字幕行。
- 46 条原题 source 与 `learning_sentences.ja_text` 精确匹配。8 条语法题中 6 条有直接语法例句记录；另外 2 条按跨行句法与真实字幕重建，不沿用错误模板标签。
- 本批 120 个 ID 在 `maintenance.regen_20260727_manifest` 中为 0 条，已应用与未应用记录均为 0；数据库中的 120 个旧答案在复核时仍全部是单字母，确认本任务没有提前写库。

## 读音、形态与句法复核

- 56 条读音题逐项核对了词项读音和真实表层。长音、拗音、促音、拨音、正常大小的 `つ` 及助词边界均未发现未修正错误。
- `済まされない`、`引っかかった`、`たくらんでる`、`呼び出された` 均按字幕实际词形回答，并分别保留否定、过去、口语缩约、被动过去的形态信息。
- `交ぜたって` 在 lines 66–68 的关西话轮中表示“也让我们加入”，不是标准语让步条件“即使混入”。其与大阪方言授受形式 `タル < テヤル` 的关系另以大阪大学论文《大阪方言における授受表現》（[DOI 10.18910/100651](https://doi.org/10.18910/100651)）交叉确认。
- 8 条语法题对真实结构的判断成立：line 3 和 line 5 是由上下文承接的名词片段；line 4 的 `し` 只存在于 `差し出す` 内部；lines 18–19 构成 `討伐に役立つ`；`とは`、`に当たって`、`はずだ`、`出てきた` 均按当前语境解释，没有套用旧模板泛义。

## 说话人、指代与证据边界

- lines 18–57 按昴、库珥修和菲利斯的连续交涉复核。库珥修判断“没有说谎”不被扩大成对白鲸情报的客观验证；相信情报与缔结同盟也保持为两个判断。
- lines 58–75 区分安娜塔西亚的关西话、库珥修对召集者的追问，以及安娜塔西亚从商人立场提出的援助。没有把 `交ぜたって` 误作让步，也没有把 `死活問題` 写成具体伤亡预言。
- lines 76–84 按拉塞尔的商业立场复核，并严格区分魔矿石、矿脉、采掘权、部分权利转让与商业组织。
- 对 `発想`、`情報`、`判断`、`直前`、`悲願` 等指代均回看前后话轮；破折号和省略号处没有补造唯一未说出的谓语、秒数或合同细节。

## 本轮 5 条修订

1. `re-zero-s01e19-exercise-103`
   - 原候选与 exercise-099 都只让学习者把 `ウソ` 转成平假名并数拍，学习任务实质重复。
   - 已改为分析 `ウソは` 的词界：名词 `うそ` 两拍，主题助词书写 `は`、实际读 `わ`，整段语音三拍。

2. `re-zero-s01e19-exercise-121`
   - 原候选再次只问 `情報` 的基本读音，与 exercise-053 高度重复。
   - 已改为分析 `情報を` 的构词和格助词边界，并把 `情＝じょ・う`、`報＝ほ・う` 与四拍结构对应起来。

3. `re-zero-s01e19-exercise-123`
   - 原候选再次只问 `判断` 的基本读音，与 exercise-101 高度重复。
   - 已改为分析 `判断だ`：名词 `はんだん` 四拍，断定助动词 `だ` 独立一拍，名词末尾拨音不脱落。

4. `re-zero-s01e19-exercise-146`
   - 原答案把白鲸“长期威胁交通与活动”写成已给证据，但 lines 73–75 只明确说白鲸在不在对商人是 `死活問題`。
   - 已删除具体威胁方式，只保留商人群体利害攸关及台词没有给出受害方式、时间和伤亡数字的边界。

5. `re-zero-s01e19-exercise-150`
   - 原答案说转让比例不能由 `割譲` 一词确定，这个词义判断本身正确，却遗漏同一提案 lines 16、84 已明说转让的是采掘权的 `一部`。
   - 已明确区分词汇本身不编码比例与本段语境已经给出“部分转让”，避免把已知条件写成未知。

## 自足性、重复与泄漏 QA

正式 validator：

```text
node regen\validate_candidates.mjs regen\batch20_exercises_letter.json regen\batch21_exercises_letter.json regen\batch22_exercises_letter.json
```

结果：

- files：3
- rows：120
- unique IDs：120
- errors：0
- warnings：0

附加检查：

- 精确五字段 schema、非空字段：120/120。
- 预期 ID 缺失、额外、顺序错位、重复：均为 0。
- 单字母答案残留：0。
- 学习侧 `prompt / answer / hint` 中的数据库、manifest、模板、源题、选项答案或内部 source ID 等元话语泄漏：0。
- 本批归一化后的重复 prompt、answer、hint：0 / 0 / 0；prompt 与 answer 完全相同：0。
- 与当前其余 52 个本地 JSON 中 1,629 条练习格式记录相比，归一化后的 prompt、answer、hint 精确重复：0 / 0 / 0。
- 对归一化文本检查长度 28 的连续片段：本批内部及与其他候选之间的 prompt、answer、hint 复用命中均为 0。
- hint 完整包含 answer、answer 完整包含 hint：0 / 0。
- answer 与 hint 的最长归一化公共连续片段最大为 5 个字符；达到 8 个字符的记录为 0。
- 120 条逐题人工复核后，题面所需的日文片段、人物或话轮锚点均足以定位任务；没有依赖内部 ID、旧选项或未提供模板才能作答的项目。

## 最终文件哈希

- `batch20_exercises_letter.json`：`CBA3606A4D605E7619C97E02FA4BEC813842406EA95DBC4E090AFA70EC619043`
- `batch21_exercises_letter.json`：`47BBABDEBB8E896E04194B27C1693D52E9C3FDEC262B52D415D5EE0BA6C81ADA`
- `batch22_exercises_letter.json`：`152C6A9AC7366C81AF2ABE9434DF4BAC7F11DACF24775AEA07331826B47F8B3A`

## 冻结状态

独立审校通过：原稿保留 115 条、修订 5 条、拒绝 0 条。三份 JSON 自上述最终校验与 SHA-256 起冻结，可写库；冻结后不得再编辑，除非重新开启审校并重新计算哈希。本复核任务本身没有执行任何数据库写入。
