# Letter-answer exercises batches 20–22：生成侧 QA

## 结论

本批按指定规则，从当前仍为单字母答案的练习中排除已应用 manifest，再按 `episode, sort_order, id` 取下一组 120 条。batches 17–19 已写入数据库，因此没有再叠加本地排除集。每题均逐条读取原记录、相关学习项、真实日文字幕及连续话轮后人工原创 `prompt / answer / hint / review_note`；脚本只用于只读选集、精确映射和 QA，没有用 Python、循环模板或批量拼接生成教学文本。全程未写数据库。

| 文件 | 条数 | 范围 |
|---|---:|---|
| `batch20_exercises_letter.json` | 40 | EP19 exercise 049–088 |
| `batch21_exercises_letter.json` | 40 | EP19 exercise 089–128 |
| `batch22_exercises_letter.json` | 40 | EP19 exercise 129–168 |
| **合计** | **120** | **EP19 exercise 049–168** |

题型分布：

- `vocab_reading`：56
- `vocab_meaning`：56
- `grammar_meaning`：8

## 选集冻结与精确映射

筛选与排序：

1. 当前 `public.learning_exercises.answer` 去空白后匹配单个 `A/B/C/D`。
2. 排除 `maintenance.regen_20260727_manifest` 中同 ID 且 `applied_at is not null` 的记录。
3. 按 `episode, sort_order, id` 排序，取前 120。
4. batches 17–19 已经写库，故不再作额外的本地 ID 排除。

最终只读复查：

- 数据库选集：120 条、120 个唯一 ID，连续范围为 `re-zero-s01e19-exercise-049` → `re-zero-s01e19-exercise-168`。
- 三个候选文件：120 条、120 个唯一 ID；缺失 ID 0，额外 ID 0，逐位置映射一致。
- 数据库与候选有序 ID MD5（以 `|` 连接）均为 `12b460282bf66db3fb7afffc33fe588d`。
- 与已应用 manifest 重叠：0。
- 候选 ID 在 manifest 中的未应用记录：0。
- 与最终扫描时其余 49 个本地 JSON 候选文件的 ID 重叠：0。

## 来源、学习项与字幕证据

- 120/120 条原记录的 `source` 经仅去除空白、标点、引号及括号注音的归一化后，均能命中 EP19 真实字幕。
- 112/112 条词汇题都把目标表层形式精确映射到 `learning_vocab_items`，并在 EP19 的 `learning_vocab_occurrences` 中找到对应真实字幕位置；共覆盖 47 个唯一词汇表层。
- 46 条原记录、18 个不同源句与 `learning_sentences` 精确匹配。
- 8 条语法题中，6 条有直接的语法例句匹配；另外 2 条分别位于真实字幕 line 5 与 line 18，均由相邻字幕和句子记录交叉支持，没有据标签猜写。
- 教学解释以 EP19 真实字幕及连续话轮为最终依据，没有机械复述词项的泛化备注。

## 人工重写与源题修正

### 表层读音、缩约与活用

- `exercise-057` 的实际表层是 `済まされない`，读作 `すまされない`，不是只回答基本形。
- `exercise-063` 的实际表层是 `引っかかった`，读作 `ひっかかった`，并明确促音位置。
- `exercise-079` 处理字幕中的缩约 `たくらんでる`，同时说明其展开形式和基本形。
- `exercise-133`、`exercise-137` 对 `呼び出された` 回答实际表层读音 `よびだされた`。
- `exercise-135` 对字幕表层 `交ぜたって` 回答 `まぜたって`，并结合关西口语解释其请求语气。

### 避免“题面直接泄露答案”

- 原先以纯假名或片假名本身询问“怎样读”的项目改为真正可学习的任务：
  - `たくらんでる`：识别缩约、展开形式与基本形。
  - `ミーティア`：判断拍数及长音符、小写 `ィ` 的作用。
  - 两条 `ウソ`：分别练习平假名转换、书写与拍数，不再让题面原样给出读音。

### 同词不同语境

`情報`、`出入り`、`現れる`、`ウソ`、`判断`、`同盟`、`呼び出された`、`討伐` 等重复目标均按各自字幕位置设计不同任务，没有复制同一问答模板。

### 语法标签的证据性修正

- `exercise-161`：line 3 是由语境补全的名词片段，不把省略内容伪装成完整句。
- `exercise-162`：line 4 的关键是表示目的的 `そのために` 和悬置话题 `情報は―`；`差し出す` 内部不存在可独立分析的接续助词 `し`。
- `exercise-163`：line 5 的名词片段承接上一话题，以连续话轮解释其功能。
- `exercise-164`：`とは` 在此标记被提出的名称／话题并带意外感，不按普通定义句处理。
- `exercise-165`：`に当たって` 表示临近并着手重要事项时的时间背景。
- `exercise-166`：真实结构是 `討伐に役立つ`；`に` 标记用途／目标领域，句内没有被旧提示误认的 `し`。
- `exercise-167`：`はずだ` 表示依据现有线索作出的强预期，不写成百分之百保证。
- `exercise-168`：`出てきた` 在该句中是线索／来源“浮现出来”的比喻性出现，不套用泛化的“从过去持续到现在”说明。

## 角色、指代与语用

- EP19 lines 18–57 按昴与库珥修的正式交涉读取：昴提出白鲸情报，库珥修质询依据；“测谎未发现虚假”不被夸大为客观事实已经证实，同盟决定也与真假判断分开处理。
- lines 58–75 按安娜塔西亚、拉塞尔加入交涉后的连续话轮读取；`ウチらも交ぜたって` 结合关西方言语境解释为“也让我们加入”，不是让步条件“即使混入”。该用法另参考[大阪方言研究资料](https://minpaku.repo.nii.ac.jp/record/1927/files/SER39_015.pdf)与 [MIT Kansai Dialect：TE-form](https://web.mit.edu/kansai/3.BasicGrammar/2.TE-form/2.Grammar/4G.html)交叉核对。
- lines 76–80 区分魔矿石资源本身、采掘权及商人组成的商业组织，没有把三者混成同一概念。

## 最终 QA

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

- 精确五字段 schema：120/120；空字段：0。
- 预期 ID 缺失、额外、重复：均为 0。
- 单字母答案残留：0。
- 学习侧 `prompt / answer / hint` 中的数据库、manifest、模板、源题、选项答案或内部 source ID 等元话语泄漏：0。
- 本批归一化后的重复 prompt、answer、hint：均为 0；prompt 与 answer 完全相同：0。
- 与最终扫描时其余 49 个本地 JSON 候选文件、2,169 条记录相比，归一化后的 prompt、answer、hint 精确重复：均为 0。
- 对归一化文本检查长度 28 的连续片段：本批内部及与其他候选之间的 prompt、answer、hint 复用命中均为 0。
- hint 完整包含 answer：0。
- answer 与 hint 的最长归一化公共连续片段最大为 5 个字符；达到 8 个字符的记录：0。

SHA-256：

- `batch20_exercises_letter.json`：`CBA3606A4D605E7619C97E02FA4BEC813842406EA95DBC4E090AFA70EC619043`
- `batch21_exercises_letter.json`：`C0FBAC30EC2CB13FB0E0DDDC7DF05A5A7E2872DAE6F03EE4E88AFEC191837B89`
- `batch22_exercises_letter.json`：`2A9638DB8DACB9039FE6EF76E9C145C46F496A4EC3C9338A2A8EA4CDAB8AB7FD`

## 冻结与释放

三个候选文件及本报告已完成生成侧 QA，现冻结释放给独立 reviewer。释放后生成者不再编辑，除非 reviewer 明确退回具体问题。本报告不是交叉复核结论，数据库仍保持未写入状态。
