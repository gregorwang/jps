# Letter-answer exercises batches 23–25：生成侧 QA

## 结论

本批从当前仍为单字母答案的练习中排除已应用 manifest，并显式排除已冻结但尚未写库的 batches 20–22 共 120 个 ID，再按 `episode, sort_order, id` 取下一组 120 条。每条都读取原题、相关学习记录、真实日文字幕及连续话轮后人工原创 `prompt / answer / hint / review_note`；脚本只用于只读选集、映射和 QA，没有用 Python 模板或循环拼接教学文本。全程未写数据库。

| 文件 | 条数 | 范围 |
|---|---:|---|
| `batch23_exercises_letter.json` | 40 | EP19 exercise 169–208 |
| `batch24_exercises_letter.json` | 40 | EP19 exercise 209–240；EP20 exercise 001–008 |
| `batch25_exercises_letter.json` | 40 | EP20 exercise 009–048 |
| **合计** | **120** | **EP19 72；EP20 48** |

题型分布：

- `grammar_meaning`：32
- `sentence_understanding`：40
- `vocab_reading`：24
- `vocab_meaning`：24

## 选集冻结与精确映射

筛选步骤：

1. 当前 `public.learning_exercises.answer` 去空白后匹配单个 `A/B/C/D`。
2. 排除 `maintenance.regen_20260727_manifest` 中同 ID 且 `applied_at is not null` 的记录。
3. 从本地 `batch20/21/22_exercises_letter.json` 读取 120 个 ID，并显式排除该集合。
4. 按 `episode, sort_order, id` 排序，取前 120。

只读复查结果：

- batches 20–22 排除集：120 条、120 个唯一 ID，范围 EP19 exercise 049–168，连续性检查通过。
- 排除集有序 ID MD5：`12b460282bf66db3fb7afffc33fe588d`。
- 新选集：120 条、120 个唯一 ID；数据库首尾为 `re-zero-s01e19-exercise-169` → `re-zero-s01e20-exercise-048`。
- 三个候选文件的 ID 顺序与数据库逐位置一致，缺失 0、额外 0。
- 数据库与候选有序 ID MD5 均为 `8154c839836f2feaacf698fe66d0ef5f`。
- 新选集与 batches 20–22 重叠：0。
- 与已应用 manifest 重叠：0；与未应用 manifest 重叠：0。
- 与最终扫描时其余 53 个本地 JSON 候选文件的 ID 重叠：0。

## 来源、学习项与真实字幕

- 120/120 条原记录的 source 经仅去除空白、标点、引号及括号注音的归一化后，均精确命中真实字幕。
- 120/120 条 source 同样命中 `learning_sentences`；共覆盖 54 个不同源句。
- 32 条语法题中，14 条有完全相同的 `learning_grammar_points.ja_example`；其余 18 条均由对应 sentence、真实字幕和连续话轮支持，没有根据错误标签猜写。
- 48 条 EP20 词汇题原记录虽然 `vocab_item_id` 为空，仍可按题面目标表层全部映射到 `learning_vocab_items` 和 EP20 `learning_vocab_occurrences`：48/48，覆盖 22 个唯一表层。
- `learning_sentences.source_line_no` 和 `learning_vocab_occurrences.example_line_nos` 存在成段错位。本批以 `subtitle_lines.line_no` 的实际日文文本为准，例如：
  - EP19 `交渉は成立ですね`：句表标 99，真实字幕为 98。
  - EP19 `討伐が済んだら…`：句表标 105，真实字幕为 104。
  - EP20 `総員 あのバカどもに続け！`：学习记录标 14，真实字幕为 12。
  - EP20 `夜払いが来ます…`：学习记录标 16，真实字幕为 14。
  - EP20 `よそ見とは…` 与 `射程を無視した…`：词汇 occurrence 标 37、38，真实字幕分别为 31、32。

## 人工修正：EP19 语法与剧情理解

- `exercise-170`：实际核心为引用「って」、缩约 `知ってる` 和原因「から」，不是笼统的て形连接。
- `exercise-171`：把外层认知主体 `卿が` 与内层命题主体 `白鯨が` 分层，并说明引用助词「と」。
- `exercise-175`：破折号后的 `と` 与下一行共同并列“是否结盟”和“是否相信情报”，不能孤立成引用。
- `exercise-177`：关西语 `ホンマやったら` 相当于“如果是真的”，不是“讨伐完成以后”。
- `exercise-179`：`のですが` 用于铺设背景并温和转向魔矿石采掘权，不是简单断定。
- `exercise-181`：真实结构是正式场合表达 `同盟に際して`；原句没有理由接续助词 `し`。
- `exercise-182`：恢复上一行 `こっちから出せるのは`，区分外层两项筹码与情报内部的“时间和地点”。
- `exercise-188`：`ってのが` 展开为 `というもの／というのが`，把“推进交涉的时机”名词化；旧标签中的 `し` 不存在。
- `exercise-190`：`整えていた` 表示会谈前已完成铺垫，句末 `か` 是领悟后的自语式确认。
- `exercise-192`：义务来自 `なきゃならない＝なければならない`，不是条件助词 `なら`。
- `exercise-193`：`公爵へ` 是献上感谢的方向，句末 `と` 与下一行 `同等` 构成比较标准，不是引用。
- `exercise-196`：`末席を汚した身` 是自谦惯用表达，不是字面弄脏座位，也不存在旧 hint 所称理由接续。
- `exercise-199`：`与えてくださる` 同时表达敬意和受益，两个 `に` 分别关联给予对象与感谢事由。
- `exercise-200`：把破折号后的 `思ってたわけで` 接回，区分引用「って」、口语 `乗っかってくる` 与库珥修随后揭穿的事后装懂。

40 条 `sentence_understanding` 没有复述语法题答案，而是分别训练指代、举证责任、跨行省略、谈判边界、商业筹码、预先布局、人物动机和说话可靠性。

## 人工修正：EP20 表层读音与语用

- `exercise-009`：字幕实际是命令形 `ぶちかませ`，读作 `ぶちかませ`；基本形才是 `ぶちかます`。源词项和旧 hint 把二者混为同一读音。
- `exercise-019`：字幕实际表层为 `聞いてたとおり`，读作 `きいてたとおり`，由 `聞いていたとおり` 缩约；不是旧资料写的 `きいたとおり`。
- `exercise-017`、`035`、`039` 分别按真实活用表层讲解 `目をつむってください`、`逃げまくって`、`安く見られた`，没有只回答词典形。
- 两次 `総員`、两次 `怖い` 均设计成不同学习任务：第一次练整词拍数／问答主语，第二次练汉字构成／假名化和跨行笑点。
- `夜払い` 只按证据充分的范围解释为需要提前闭眼应对的战术／招式名称，不编造完整术式机制。
- `ああ 怖いね` 必须连到下一行“害怕自己受赞赏后的耀眼未来”，保留昴以自吹玩笑化解压力的语用。
- `逃げまくってやろうぜ` 按主动诱敌、持续牵制的作战方案解释，不误写成临阵弃战。
- `よそ見とは…安く見られた` 按敌人分神与库珥修遭到轻视的责难解释；`安く見る` 不按价格义。
- `射程を無視した無形の剣` 按库珥修远距离、无可见固定形体的斩击说明，区分实体剑、有效距离与攻击形态。

## 角色、指代与证据边界

- EP19 lines 20–57：昴从人员物资、装备采购和威尔海姆悲愿推测库珥修的计划；他承认没有确证。库珥修只确认未发现谎言，并明确把情报可信度与政治同盟分开。
- lines 58–117：安娜塔西亚和拉塞尔加入后，商人利益、佣兵援助、矿石流通与合辛商会采购构成额外筹码；“押し时”解释的是她选择介入谈判的时机。
- lines 122–147：`それ` 回指魔女教针对艾米莉娅的行动；威尔海姆公开与特蕾西亚的关系及杀妻之仇，昴随后假装早已全部算准，并被测谎能力当场揭穿。
- EP20 lines 1–8 是人物相遇的回忆；lines 9–32 转入白鲸战斗。词义和语气均按各自场景处理，没有把回忆中的日常词汇套入战术模板。

## 最终 QA

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

- 精确五字段 schema：120/120；空字段：0。
- 预期 ID 顺序完全一致；缺失、额外、重复：均为 0。
- 单字母答案残留：0。
- 学习侧 `prompt / answer / hint` 中的数据库、manifest、模板、源题、选项答案、内部 ID 等元话语泄漏：0。
- 本批归一化后的重复 prompt、answer、hint：均为 0；prompt 与 answer 完全相同：0。
- 与最终扫描时其余 53 个本地 JSON 候选文件、2,389 条记录相比，归一化 prompt、answer、hint 精确重复：均为 0。
- 对归一化文本检查长度 28 的连续片段：本批内部及与其他候选之间的 prompt、answer、hint 复用命中均为 0。
- hint 完整包含 answer：0。
- answer 与 hint 的最长归一化公共连续片段最大为 5 个字符；达到 8 个字符的记录：0。

SHA-256：

- `batch23_exercises_letter.json`：`C96AB773412E22D62B92A90AE1BE3F616A213145E433482ED8A6521AED3EB074`
- `batch24_exercises_letter.json`：`807962932501DE9AA230B23C9126AB401772EA0CA73DA007020A175976BDBD38`
- `batch25_exercises_letter.json`：`8C3833CF7F8408ED53AFB71858E842C626CC39417BE2A66D35D28C173F2367F8`

## 冻结与释放

三个候选文件及本报告已完成生成侧 QA，现冻结释放给独立 reviewer。释放后生成者不再编辑，除非 reviewer 明确退回具体问题。本报告不是交叉复核结论，数据库仍保持未写入状态。
