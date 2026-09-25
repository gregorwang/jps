# Letter exercises batches 29–31 独立交叉审校

## 结论

本轮独立审校了 EP20 exercise 169–230 与 EP21 exercise 001–056、061–062 的 120 条候选。审校重新读取当前数据库原题、相关词汇／句子／语法记录，并逐条对照 EP20–21 真实日文字幕及连续话轮；没有把生成侧 QA 当作复核结论，也没有用脚本或模板生成教学文本。

| 批次 | 范围 | 条数 | 原稿保留 | 审校修订 | 拒绝 |
| --- | --- | ---: | ---: | ---: | ---: |
| batch29 | EP20 exercise 169–208 | 40 | 36 | 4 | 0 |
| batch30 | EP20 exercise 209–230、EP21 exercise 001–018 | 40 | 37 | 3 | 0 |
| batch31 | EP21 exercise 019–056、061–062 | 40 | 37 | 3 | 0 |
| **合计** | **EP20 62 条、EP21 58 条** | **120** | **110** | **10** | **0** |

EP21 exercise 057–060 在当前表中不存在，因此从 056 续到 061 不是候选漏项。三份 JSON 已按本报告末尾哈希冻结，可进入后续写库审议；本轮数据库操作全部为只读，没有写数据库。

## 原稿锁定

审校开始前锁定的生成稿 SHA-256：

- `batch29_exercises_letter.json`：`5C3DABF66FF845122BA95A4EC8145D8C8973D05E2D3D05985BEA4717523BE148`
- `batch30_exercises_letter.json`：`71F314C237C13EC8B4C66774F168194ADC872B2EA9368F7934497018CB582405`
- `batch31_exercises_letter.json`：`01DD9C57CFDD1A9E1C26E451B4CA1967A24C08A07DAF2E2505CA768310F644D4`

## 数据库映射、备份与真实来源

- 120/120 个候选 ID 精确映射当前 `public.learning_exercises`，没有缺失、额外、重复或位置错配；EP20 为 62 条，EP21 为 58 条。
- 三个本地文件与数据库有序 ID 以 `|` 连接后的 MD5 均为 `5b0792b5ce784b913a178d65730d7e38`；首尾为 `re-zero-s01e20-exercise-169` → `re-zero-s01e21-exercise-062`。
- 数据库原题型为 `grammar_meaning` 17 条、`sentence_understanding` 45 条、`vocab_reading` 29 条、`vocab_meaning` 29 条。
- 120/120 条原题 source 经规范化后精确命中当前 `subtitle_lines.ja_text`，共覆盖 66 个不同字幕源句。
- 104/120 条 source 精确命中 `learning_sentences.ja_text`；其中 45/45 条 sentence understanding 全部命中。
- 17 条语法题只有 4 条 source 与 `learning_grammar_points.ja_example` 完全匹配，因此复核以真实字幕结构和连续话轮为主，没有用稀疏标签反推答案。
- 58/58 条词汇题均唯一映射 `learning_vocab_items`，并命中对应集数的 `learning_vocab_occurrences`，覆盖 28 个目标表层；其中 44/58 条 source 同时精确命中学习句。
- 只读比对 `maintenance.regen_20260727_learning_exercises_backup`：选集 120 条、备份 120 条、内容精确一致 120 条；prompt、answer、hint、`updated_at` 漂移均为 0。
- 本批 ID 在 `maintenance.regen_20260727_manifest` 中为 0 条；最终只读扫描时，数据库旧答案仍为单字母的记录为 120/120。
- 安全顾问另行提示 3 张 `maintenance` 表未启用 RLS；现有权限核对显示 `public / anon / authenticated` 均无表权限，仅 `postgres` 保留权限。本任务未改变 schema、RLS 或授权。

## 读音、形态与句法复核

- 29 条读音题逐项核对目标表层、假名、拍数、长音、促音、拨音及助词边界，没有发现需要拒绝的错误。
- `切ったった` 按关西口语的 `切ってやった` 缩约解析；前置 `切れる` 则是可能形，不能把两处表面相近的形式混作同一活用。
- `落ちん` 还原为 `落ちない`，`ちゅう` 还原为引用判断的 `という`；`見てるしかねえってのか` 依次恢复 `見ている／しかない／というのか`。
- `飲み込ませるな` 是使役形加禁止，`飲み込まれる前` 是被动态加时间界限；`助け出せる` 是复合动词 `助け出す` 的可能形。
- `似合わねえ`、`消されちゃいねえ`、`軽うなっとる` 分别按 `似合わない`、`消されてはいない`、`軽くなっている` 复原，未把方言表面当成独立词义。
- `放つんだ` 读作 `はなつんだ`，五拍；`んだ` 属于后接说明形式 `のだ` 的缩约，不并入 `放つ` 词干，也不编码“唯一”这一限定。

## 说话人、指代与证据边界

- 年轻威尔海姆的初遇台词能证明粗硬语域、戒备和不讲社交圆融；不能再补成他在守护某种“私人生活边界”。
- 特蕾西亚的 `おいで` 能证明她主动把对峙改成轻松接触；字幕没有编码具体招手动作，因此不把画面假设写进语言题答案。
- `夜払い` 是把夜间战场骤然照亮的军用强光照明，友军闭眼是为了规避闪光；它不是该句所宣称的直接对白鲸伤害。该机制也与 [Episode 20 梗概](https://rezero.fandom.com/wiki/Episode_20) 及 [Night Banisher 条目说明](https://rezero.fandom.com/wiki/Magic) 一致。
- 紧接 `夜払い` 的 `聞いてたとおりだ` 确认的是预先听说的照明效果，不是对联合军整体火力规模或对白鲸战果的确认。
- `かなり効いた感じがする` 只给出现场观感和主观乐观；后续高度未降才是较可靠的客观指标。
- `初っぱなに切れる手札は全部切ったった` 表示开局可立即打出的这一组手牌耗尽、选择收窄；并不等于全军完全失去战力，也没有证明所谓“低成本后备方案”。
- 战斗呼声只能支持持续交锋和节奏未停；不能仅凭音声固定为近身肉搏、特定招式、命中或伤亡。
- EP21 的 `レムの奮戦しだい` 承接紧邻的 `ヴィルヘルムは？`，所系的是威尔海姆能否获救，不是泛指整场战斗胜负。
- “若天然群居，传闻理应存在”在当下只推出三头现象背后另有机制；分裂／分身结论要到 EP21 lines 85–100 的后续观察才能验证。
- 所有 learner-facing `prompt / answer / hint` 均能脱离数据库 ID、内部行号、旧选项和模板信息独立作答；交叉证据只保留在 `review_note`。

## 本轮 10 条修订

### batch29（4 条）

1. `re-zero-s01e20-exercise-187`：删除字幕未证明的“保护自己的生活边界”，只按粗硬语域刻画戒备、好斗和不圆融。
2. `re-zero-s01e20-exercise-188`：以文本可见的 `おいで` 邀请说明互动策略，删除未被字幕编码的具体招手动作并收窄关系推断。
3. `re-zero-s01e20-exercise-193`：纠正 `夜払い` 的机制为战场强光照明、友军同步与视力保护；明确该句没有声称直接伤害白鲸。
4. `re-zero-s01e20-exercise-194`：把 `聞いてたとおり` 接回紧邻的 `夜払い` 照明效果，删除对联合攻击规模和火力表现的越界解释。

### batch30（3 条）

1. `re-zero-s01e20-exercise-217`：把无依据的“视觉和体感”收窄为现场观感，保留 `感じがする` 的主观证据等级。
2. `re-zero-s01e20-exercise-221`：删除“低成本后备方案”的臆造资源属性，改为开场可用手牌耗尽、后续选择收窄。
3. `re-zero-s01e20-exercise-228`：将无法由呼声证明的“肉搏”改为持续交锋，并明确不固定攻击距离或伤亡。

### batch31（3 条）

1. `re-zero-s01e21-exercise-030`：用上一问 `ヴィルヘルムは？` 锁定指代，明确雷姆奋战所决定的是威尔海姆的救援／生还。
2. `re-zero-s01e21-exercise-034`：区分当下反证与后续揭示；缺少群居传闻只证明另有机制，分身结论由后文验证。
3. `re-zero-s01e21-exercise-045`：保留 `んだ` 的强制命令语气，删除该形式并不编码的“唯一”限定。

## 自足性、重复与泄漏 QA

正式 validator：

```text
node regen\validate_candidates.mjs regen\batch29_exercises_letter.json regen\batch30_exercises_letter.json regen\batch31_exercises_letter.json
```

结果：

- files：3
- rows：120
- unique IDs：120
- errors：0
- warnings：0

全量 letter 候选兼容性校验：

- files：27
- rows：1,080
- unique IDs：1,080
- errors：0
- warnings：0

附加检查：

- 精确五字段 schema、非空字段：120/120。
- 预期 ID 缺失、额外、顺序错位、重复：均为 0。
- 单字母答案、零宽字符及 BOM 残留：均为 0。
- learner-facing `prompt / answer / hint` 中数据库、manifest、模板、source ID、候选 ID、内部行号、旧选项答案等元话语泄漏：0。
- 本批归一化后的重复 prompt、answer、hint：0 / 0 / 0；三字段整组重复与 prompt 等于 answer：均为 0。
- 与其余 34 个本地 exercise 候选文件、1,349 条记录相比，归一化后的 prompt、answer、hint 精确重复：0 / 0 / 0。
- 对归一化文本检查长度 28 的连续片段：本批内部及与上述 1,349 条候选之间的 prompt、answer、hint 复用命中均为 0。
- hint 完整包含 answer、answer 完整包含 hint：0 / 0。
- answer 与 hint 的最长归一化公共连续片段最大为 6 个字符（`のではないか`）；达到 8 个字符的记录为 0。

## 最终文件哈希

- `batch29_exercises_letter.json`：`60DFA8485C113151C80BC95A474E1E92A35FA0B2FD2C730914A0CC699893B352`
- `batch30_exercises_letter.json`：`A98681E4A51D793F31D09FFF7539665EAC4D0E92EF1745FDE649926B6CC00EF6`
- `batch31_exercises_letter.json`：`541B63D1A9D620C70F480E8EEDAC475AE669E221538726EAFC48E63C3FC381D9`

## 冻结状态

独立审校通过：原稿保留 110 条、人工修订 10 条、拒绝 0 条。三份 JSON 自上述最终校验与 SHA-256 起冻结；冻结后不得再编辑，除非重新开启审校并重新计算哈希。本复核任务没有执行任何数据库写入。
