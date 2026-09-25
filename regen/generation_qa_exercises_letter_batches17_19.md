# Letter-answer exercises batches 17–19：生成侧 QA

## 结论

本批按用户指定规则，从当前仍为单字母答案的练习中排除已应用 manifest，并额外显式排除尚未写库的 batches 14–16 共 120 个 ID，再按 `episode, sort_order, id` 取下一组 120 条。每题均读取原记录、相关学习项、真实日文字幕及上下话轮后人工重写；脚本只用于选行和 QA，没有生成教学文本。全程未写数据库。

| 文件 | 条数 | 范围 |
|---|---:|---|
| `batch17_exercises_letter.json` | 40 | EP18 exercise 169–208 |
| `batch18_exercises_letter.json` | 40 | EP18 exercise 209–240；EP19 exercise 001–008 |
| `batch19_exercises_letter.json` | 40 | EP19 exercise 009–048 |
| **合计** | **120** | **EP18 72；EP19 48** |

## 选集冻结与精确映射

筛选条件：

1. `work_slug = 're-zero'`。
2. 原 `answer` 去空白后为单个 `A/B/C/D`。
3. 排除 `maintenance.regen_20260727_manifest` 中同 ID 且 `applied_at is not null` 的记录。
4. 从本地 `batch14/15/16_exercises_letter.json` 读取并核验 120 个唯一 ID，再显式排除该集合。
5. 按 `episode, sort_order, id` 排序，取前 120。

最终只读复查：

- batches 14–16 排除集：120 条、120 个唯一 ID，范围 EP18 049–168，MD5 `396201f26c032d9979416362e4cb8714`。
- 数据库新选集：120 条、120 个唯一 ID；EP18 72、EP19 48。
- 新选集与 batches 14–16 重叠：0。
- 新选集与已应用 manifest 重叠：0。
- 数据库首尾：`re-zero-s01e18-exercise-169` → `re-zero-s01e19-exercise-048`。
- 候选首尾一致，逐位置 ID 差异：0。
- 数据库与候选有序 ID MD5（以 `|` 连接）均为 `d36dadf3ac34d1439ae64b4d6d8932a5`。

## 来源与字幕证据

- EP18 grammar、sentence 和语气题逐条对照真实字幕，目标覆盖实际字幕 lines 6–138。
- EP19 词汇题的全部目标句集中在真实字幕 lines 3–18；同时核对相关 `learning_sentences` 和 21 个唯一词汇项。
- `learning_sentences.source_line_no` 中存在已知错位，例如 sentence-007 标为 29、实际字幕为 24，sentence-025 标为 119、实际为 104，sentence-028 标为 137、实际为 122，sentence-031 标为 153、实际为 138。本批以实际 `subtitle_lines.line_no` 和连续日文话轮为准，没有机械继承错位行号。
- EP19 部分词汇项沿用其他集数的泛化备注；重写只采用词项读音、词义与 EP19 当前交涉语境，不复制泛化模板说明。

## 源题误标的证据性修正

- `exercise-169`：`あって` 是 `ある` 的て形，内部没有引用助词 `って`。
- `exercise-171`：`一緒にしないで` 是“不要混为一谈”，不是“不要一起做”。
- `exercise-172`：按完整 `Vてもらいたい` 分析愿望主体与执行者，不按简单 `Vたい` 处理。
- `exercise-173`：`罰にならない` 是 `なる` 的否定，不是条件 `〜なら`。
- `exercise-180`：`戻ってきた` 按 `戻る＋てくる` 处理，句中无引用。
- `exercise-187`：`迫ってくる` 按接近说话人视点的 `Vてくる` 处理，句中无引用。
- `exercise-189`：`みたいだ` 是完整推测形式，不能截出愿望 `たい`。
- `exercise-194`：`憎んだ` 是 `憎む` 的过去形，内部没有说明语法 `んだ`。
- `exercise-195`：`笑ってる` 是 `笑っている` 的缩约，内部没有引用 `って`。
- `exercise-201`：`焦ってた` 是 `焦っていた` 的缩约，内部没有引用 `って`。
- `exercise-206`：`レムにだって` 应切为 `レム＋に＋だって`，不能截出引用 `って`。
- `exercise-208`：`嫌いじゃありません` 的 `じゃ` 是 `では` 的口语缩约，不是条件。
- `exercise-209`：`へこんだり` 中的字面 `んだ` 属于过去形，真实结构为 `Vたり、Vたりで`。
- `exercise-218`：现存字幕前半 `サテラの半分 千は` 切分不清；候选只解释证据充分的后半命令，不编造精确数量关系。
- EP19 的 `白鯨` 统一按规范读音 `はくげい`，没有继承源提示中带空格的罗马字 `hak gei`。

## 角色、指代与语用

- EP18 lines 8–11 的断罪和“本应立即了结我”由培提其乌斯对帕克说。
- lines 13–14 的条件挑战由帕克提出，`僕を` 的受事是帕克。
- lines 26–27 是帕克以精灵寿命反驳；lines 28–31 是培提其乌斯以信仰深度反击并拒绝被相提并论。
- lines 43–62 是帕克对昴列罪、说明契约后果并评价昴。
- lines 63–70 的笑声最终被昴识别为自身的自我责难。
- lines 72–92 的关心、道歉与委婉抱怨按雷姆—昴连续话轮解释。
- EP19 lines 3–19 按正式同盟交涉读取：昴拿出白鲸现身情报，交换条件涉及魔矿石采掘权与讨伐计划；没有把词汇脱离为一般字典释义。

## 最终 QA

运行：

```text
node regen\validate_candidates.mjs regen\batch17_exercises_letter.json regen\batch18_exercises_letter.json regen\batch19_exercises_letter.json
```

结果：

- files：3
- rows：120
- unique IDs：120
- errors：0
- warnings：0

附加检查：

- 精确五字段 schema：120/120。
- 空字段、单字母答案残留：0。
- 学习侧 `prompt / answer / hint` 中的选项元话语、模板语言、数据库／源题措辞或内部 source ID 泄漏：0。
- 本批归一化重复 prompt、answer、hint、prompt-answer：均为 0。
- 与当时既有 28 个 exercise 候选文件、1,349 条记录的归一化 prompt 重复：0。
- 与既有候选的归一化 prompt-answer 重复：0。
- hint 完整包含 answer：0。
- answer 完整包含长度至少 8 的 hint：0。
- 归一化 answer-hint 最长公共连续片段达到 8 字符：0。

SHA-256：

- `batch17_exercises_letter.json`：`66675728A69E3D426E8CF112F3A908D93800BBAB2B1E8DD0A72ECB9396699085`
- `batch18_exercises_letter.json`：`8C950D00DE3A8EA1B807600C02F7148AA2A388C9A54A5DF3CA0ECFDDBCA89AFF`
- `batch19_exercises_letter.json`：`5743166D976362D16EC4B470EBCF6AD0358D841C9204C87741CEC9530CAF1C47`

## 冻结与释放

三个候选文件及本报告已完成生成侧 QA，现冻结释放给独立 reviewer。释放后生成者不再编辑，除非 reviewer 明确退回具体问题。本报告不是交叉复核结论，数据库仍保持未写入状态。
