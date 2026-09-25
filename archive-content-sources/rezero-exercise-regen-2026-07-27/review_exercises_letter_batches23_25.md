# Letter exercises batches 23–25 独立交叉审校

## 结论

本轮独立审校了 EP19 exercise 169–240 与 EP20 exercise 001–048 的 120 条候选。审校重新读取当前数据库原题、相关词汇／句子／语法记录，并逐条对照 EP19–20 真实日文字幕及连续话轮；没有把生成侧报告当作复核结论。

| 批次 | 范围 | 条数 | 原稿保留 | 审校修订 | 拒绝 |
| --- | --- | ---: | ---: | ---: | ---: |
| batch23 | EP19 exercise 169–208 | 40 | 33 | 7 | 0 |
| batch24 | EP19 exercise 209–240、EP20 exercise 001–008 | 40 | 34 | 6 | 0 |
| batch25 | EP20 exercise 009–048 | 40 | 35 | 5 | 0 |
| **合计** | **EP19 exercise 169–240、EP20 exercise 001–048** | **120** | **102** | **18** | **0** |

三份 JSON 已按本报告末尾哈希冻结，可进入写库阶段。本轮数据库操作全部为只读 `SELECT`，没有写数据库。

## 原稿锁定

审校开始前锁定的生成稿 SHA-256：

- `batch23_exercises_letter.json`：`C96AB773412E22D62B92A90AE1BE3F616A213145E433482ED8A6521AED3EB074`
- `batch24_exercises_letter.json`：`807962932501DE9AA230B23C9126AB401772EA0CA73DA007020A175976BDBD38`
- `batch25_exercises_letter.json`：`8C3833CF7F8408ED53AFB71858E842C626CC39417BE2A66D35D28C173F2367F8`

## 数据库映射与真实来源

- 120/120 个候选 ID 精确映射当前 `public.learning_exercises`，没有缺失、额外、重复或位置错配；EP19 为 72 条，EP20 为 48 条。
- 在排除已应用 manifest 项及本地 batches 20–22 后，独立重算的下一段 120 个可用 ID 与三份候选逐位置 120/120 一致；按换行拼接 ID 得到的本地与数据库 MD5 均为 `3949961cd8660747a1748def9d2c8cdd`。
- 原题类型为 `grammar_meaning` 32 条、`sentence_understanding` 40 条、`vocab_reading` 24 条、`vocab_meaning` 24 条。
- 去除空白、标点、引号和括号注音后，120/120 条数据库原题 source 均命中当前 `subtitle_lines.ja_text`，也均命中 `learning_sentences.ja_text`；共覆盖 54 个不同 source。
- 32 条语法题中，14 条 source 与 `learning_grammar_points.ja_example` 精确匹配；其余按真实字幕结构和连续话轮重建，没有套用旧模板标签。
- 48 条词汇题全部命中 `learning_vocab_items`，也全部命中对应集数的 `learning_vocab_occurrences`；共覆盖 22 个不同目标表层。
- 本批 120 个 ID 在 `maintenance.regen_20260727_manifest` 中为 0 条，已应用与未应用记录均为 0；数据库中的 120 个旧答案在最终复核时仍全部是单字母。

## 读音、形态与句法复核

- 24 条读音题逐项核对目标表层、拍数、长音、拗音、拨音及助词边界。
- EP20 的 `夜払い` 按动画实际语音改为 `よるばらい`（五拍），保留 `はらい→ばらい` 的连浊说明。旧数据库词项所记 `よばらい` 没有音频证据支持；2016 年播出当时的逐句听写也转写为 `Yoru-barai`（[Japanese in Anime 的 EP20 逐句转写](https://japaneseinanime.blogspot.com/2016/08/)）。
- `無形の剣` 在该话语音中读 `むけいのけん`，与数据库词项及同一份逐句听写的 `mukei no ken` 一致，没有误改成姓名等其他读法。
- `聞いてた` 先还原为 `聞いていた`，再解释实际句型 `Vたとおり`；不把缩约后的表面片段当成完整接续。
- `総員` 与 `怖い` 的重复基础读音题分别改为词界／拍数任务，避免与同批既有学习目标实质重复。
- `末席を汚す` 按谦逊惯用表达解释为“忝列末席／承蒙加入”，不是字面上的“给家族或席位抹黑”（参见[《デジタル大辞泉》「汚す」第 3 义](https://dictionary.goo.ne.jp/word/%E6%B1%9A%E3%81%99_%28%E3%81%91%E3%81%8C%E3%81%99%29/)）。

## 说话人、指代与证据边界

- 昴提出的是艾利奥尔大森林魔矿石“部分采掘权”，不是同时交付“矿石实物＋采掘权”两项独立筹码。
- 库珥修接受同盟时依据的是昴表现出的精神以及她自己的观察判断；这不等于她对昴作无条件、全领域的相信。
- 安娜塔西亚与拉塞尔的到场建立在昴事先铺垫之上，但介入和打断谈判的时机由来访者自己选择，不能全部归功于昴安排。
- `与えてくださる` 的施事虽省略，敬语方向和直接称谢对象指向昴；台词没有明说“库珥修与昴共同给予”。
- 尤里乌斯说 `怖いね` 后，下一句把“害怕”的对象幽默地转向自己击败白鲸后过于耀眼的未来；不能先当作他字面承认害怕白鲸，再补一个反转。
- 商人对白鲸的态度只被台词界定为关系生计与存续的 `死活問題`；未补造具体商路、损失机制、伤亡或时间尺度。
- 所有 learner-facing `prompt / answer / hint` 均能脱离内部行号、source ID、旧选项和模板独立作答；行号与数据库交叉证据仅保留在 `review_note`。

## 本轮 18 条修订

### batch23（7 条）

1. `re-zero-s01e19-exercise-178`：删除“商路受阻”等未被对白明确给出的受害机制，只保留商人群体的生计与存续利害。
2. `re-zero-s01e19-exercise-185`：把“相信昴”收窄为相信他表现出的精神并依自己的眼睛判断；不把结盟扩大为无条件相信。
3. `re-zero-s01e19-exercise-193`：移除 learner-facing 内部行号，直接给出需要比较的两句台词。
4. `re-zero-s01e19-exercise-196`：纠正 `末席を汚す` 的惯用义，删除“给家族蒙羞”的字面误释。
5. `re-zero-s01e19-exercise-199`：按敬语方向和直接称谢对象解释省略施事，不再声称台词明说库珥修与昴是共同主语。
6. `re-zero-s01e19-exercise-203`：移除 learner-facing 内部行号，重写为仅依题面话轮即可使用的提示。
7. `re-zero-s01e19-exercise-205`：明确提案对象是“部分采掘权”，不是“矿石与采掘权”两项；同时移除内部行号提示。

### batch24（6 条）

1. `re-zero-s01e19-exercise-216`：移除 learner-facing 内部行号，保留自足的语句对照。
2. `re-zero-s01e19-exercise-221`：移除 learner-facing 内部行号并重写提示，避免泄漏证据索引。
3. `re-zero-s01e19-exercise-224`：移除 learner-facing 内部行号，直接锚定题中出现的人物和表述。
4. `re-zero-s01e19-exercise-230`：纠正行动归属：昴负责事前铺垫，来访者自己选择进入和打断的时机。
5. `re-zero-s01e19-exercise-236`：纠正 `末席を汚す` 的谦逊惯用义。
6. `re-zero-s01e19-exercise-239`：按省略施事、敬语方向和直接称谢对象重写，不补造“库珥修＋昴”的显式共同主语。

### batch25（5 条）

1. `re-zero-s01e20-exercise-013`：把与同批重复的 `総員` 基础读音题改为 `そういん｜あの` 的词界与拍数分析。
2. `re-zero-s01e20-exercise-015`：把 `夜払い` 从无音频支持的 `よばらい` 四拍纠正为动画实际读音 `よるばらい` 五拍，并记录连浊。
3. `re-zero-s01e20-exercise-020`：先还原 `聞いてた→聞いていた`，再按真实结构 `聞いていたとおり` 解释。
4. `re-zero-s01e20-exercise-025`：把与同批重复的 `怖い` 基础读音题改为 `こわい｜ね` 的词界与三拍加一拍分析。
5. `re-zero-s01e20-exercise-026`：删除“先字面承认害怕白鲸”的过度推断，按下一句确定玩笑实际指向他设想中的耀眼未来。

## 自足性、重复与泄漏 QA

正式 validator：

```text
node regen\validate_candidates.mjs regen\batch23_exercises_letter.json regen\batch24_exercises_letter.json regen\batch25_exercises_letter.json
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
- learner-facing `prompt / answer / hint` 中的数据库、manifest、模板、源题、选项答案、内部 source ID 或行号等元话语泄漏：0。
- 本批归一化后的重复 prompt、answer、hint：0 / 0 / 0；prompt 与 answer 完全相同：0。
- 与审校时已存在的其他本地候选相比，归一化后的 prompt、answer、hint 精确重复：0 / 0 / 0。
- 对归一化文本检查长度 28 的连续片段：本批内部及与其他候选之间的 prompt、answer、hint 复用命中均为 0。
- hint 完整包含 answer、answer 完整包含 hint：0 / 0。
- answer 与 hint 的最长归一化公共连续片段最大为 5 个字符；达到 8 个字符的记录为 0。
- 120 条已完成两轮逐题人工复核；题面所需的日文片段、人物或话轮锚点均足以定位任务。

## 最终文件哈希

- `batch23_exercises_letter.json`：`2D1D9CBAB6AFC7C7B021258DF8267277F7CD306644F396C38380CE2993AC1231`
- `batch24_exercises_letter.json`：`2E9AB2B82E1B544A64F924CD29FF26CFAE1B0714AFF3E72E4FB35513F4120F0F`
- `batch25_exercises_letter.json`：`AEFA3407A82A4AFDD6D03C9A3CEFF3D1763E5185AFD74EF52B013A0470AF2C30`

## 冻结状态

独立审校通过：原稿保留 102 条、修订 18 条、拒绝 0 条。三份 JSON 自上述最终校验与 SHA-256 起冻结，可写库；冻结后不得再编辑，除非重新开启审校并重新计算哈希。本复核任务本身没有执行任何数据库写入。
