# Letter exercises batches 14–16 独立交叉审校

## 结论

本轮独立审校了 EP18 的 120 条候选：

- `regen/batch14_exercises_letter.json`：exercise 049–088
- `regen/batch15_exercises_letter.json`：exercise 089–128
- `regen/batch16_exercises_letter.json`：exercise 129–168

审校者没有依据生成者的结论直接放行，而是重新读取当前数据库原题、vocab occurrence、可用的 `learning_sentences` / `learning_grammar_points`，并逐条对照 EP18 日文字幕和连续话轮。

| 批次 | 条数 | 原稿保留 | 审校修订 | 拒绝 |
| --- | ---: | ---: | ---: | ---: |
| batch14 | 40 | 38 | 2 | 0 |
| batch15 | 40 | 36 | 4 | 0 |
| batch16 | 40 | 35 | 5 | 0 |
| **合计** | **120** | **109** | **11** | **0** |

三份 JSON 已按本报告末尾哈希冻结，可进入写库阶段。本轮只执行 SELECT 和本地候选修订，没有写数据库。

## 数据库映射与字幕证据

### exercise 与 source

- 120/120 个候选 ID 精确映射 `public.learning_exercises`。
- 120/120 均为 `re-zero` EP18，ID 后缀连续覆盖 049–168。
- 原题类型为：
  - `vocab_reading`：56
  - `vocab_meaning`：56
  - `grammar_meaning`：8
- 120 条原题共涉及 49 条不同的日文 source line。
- 去空白、标点并仅移除字幕中的括号注音后，120/120 个 source line 均精确命中当前 `subtitle_lines.ja_text`。其中 118 条可直接逐字归一化命中；另外 2 条是 `寵愛にあずかる`，当前字幕原文为 `寵愛(ちょうあい)にあずかる`。
- 12 条不同 source line、共 37 个 exercise 行还精确命中 `public.learning_sentences`。其余词汇题以 `learning_vocab_occurrences`、词项和真实字幕共同取证，并非 source 缺失。

### vocab

- 112/112 个 vocab exercise 的原目标均能映射到 EP18 occurrence；56 个 reading 目标中有 55 个不同词面，`約束`因两个真实用例重复出现。
- 不能只凭 occurrence 映射放行：exercise 105/106 的 DB 目标为 `死ね`，但原题所附真实句和字幕都是 `死ぬほど憎んだ男の声`，并不存在命令形 `死ね`。候选已经改为实际出现的 `死ぬ`及程度构式，本轮重新核对后确认该修复正确。
- 多个原题 hint 把基本形错当作句中实际读音。候选对 `果たされた`、`尊き`、`破った`、`足りない`、`死なせた`、`従い`、`疲れて`、`焦って`、`逃げよう`等逐项给出实际词形，再说明基本形；本轮均按字幕复核。

### grammar

- 8 个 grammar exercise 中，7 个原 source line 能精确匹配现有 `learning_grammar_points.ja_example`。
- exercise 161 对应的旧 grammar row 虽能匹配原句，却把 `黙って`内部的て形误切成引用助词「って」。候选按 `黙る → 黙って ＋ いる`纠正，本轮确认正确。
- exercise 168 的旧题把 `あってはならない`误绑为条件「〜なら」，数据库中也没有与该 source line 对应的正确 grammar row。候选按完整禁止／强烈不可接受结构 `Vてはならない`解释，本轮确认正确。
- exercise 162–167 的条件主体、愿望主体、宾语和命令执行者均重新按说话人关系复核，未发现角色倒置。

## 本轮 11 条修订

### 实质内容修订

1. `re-zero-s01e18-exercise-102`
   - 原候选把 `自分も命を落として`中的 `自分`解释成帕克。
   - EP18 lines 59–62 是帕克连续责备昴：唤起白鲸、让爱蜜莉雅死去、自己也丧命，随后以呼语 `君`收束。
   - 已改为昴，并补充代词恢复证据。

2. `re-zero-s01e18-exercise-124`
   - 原候选只把 `へこまされたり`写成单一路径的使役被动。
   - 已改为保留两种可行分析：他动词 `へこます`的被动，或 `へこませられたり`的口语使役被动缩约；两者的语义角色均为外界使昴消沉。

### 题面自答与 hint 泄漏修订

- `exercise-055`：题面原样展示字幕括号注音 `ちょうあい`，等于直接给出答案；已隐藏注音，保留长音与拗音任务。
- `exercise-057`、`129`、`133`、`145`、`151`：hint 原先直接写出完整读音，或把各字读音完整拼出；已改为同音词、构词或对比线索。
- `exercise-123`、`125`、`153`：全假名词若只问“怎样读”会由题面自答；已分别改成活用还原、促音拍数、促音与拗音辨析任务，并同步收紧 hint。

修订后，所有 reading 题的完整答案读音在 hint 中直接出现的条数为 0。

## 说话人、指代与证据边界

逐条连读了目标字幕及前后话轮；目标主要位于 EP18 lines 1–112，并继续读取 lines 119–175，以核对昴后续反复要求雷姆选择、共同逃亡等语用结论。重点结论包括：

- lines 1–25：帕克、培提其乌斯和昴的角色方向已分别核实；`僕を殺したい`中的被杀者是帕克，愿望主体是听者培提其乌斯。
- lines 35–62：培提其乌斯退场后由帕克说 `勝ち逃げされたな`并责备昴；`自分も命を落として`仍指昴。
- lines 63–70：笑声被昴认作自己的声音，`怠惰だね スバル`按自我审判与敌人惯用语的重叠解释。
- lines 72–100：雷姆先把异常归因为人群疲劳，这只是人物当时的体贴推测；答案没有把它写成客观真相。
- lines 104–175：`逃げよう`是把雷姆纳入的共同提议，随后多次出现 `選んでくれ`和 `俺と生きてくれ`，足以支持“提议带有强压力”，但不把意向形误称为命令形。
- `呼び寄せられた`的施事未明说；`楽な旅路には…`的结尾未说完。候选均保留省略边界，没有硬补唯一施事或固定谓语。

## 结构、重复与泄漏 QA

运行：

```text
node regen/validate_candidates.mjs regen/batch14_exercises_letter.json regen/batch15_exercises_letter.json regen/batch16_exercises_letter.json
```

结果：

- files：3
- rows：120
- unique IDs：120
- errors：0
- warnings：0
- 120/120 均含非空 `id`、`prompt`、`answer`、`hint`、`review_note`
- 批内重复 ID：0
- 批内归一化 prompt / answer / hint 完全重复：0 / 0 / 0
- prompt、answer、hint 内归一化 28 字符连续片段跨卡复用组：0
- 与既有 letter batch5–13 共 360 条相比，ID / prompt / answer / hint 完全重复：0 / 0 / 0 / 0
- 完整 reading 答案在 hint 中直接出现：0
- answer 与 hint 的最长归一化连续公共片段为 7 字符；达到 16 字符的条目：0

## 最终文件哈希

- `batch14_exercises_letter.json`：`CACB390AF0936AD51D58704BC7C380B287BFF467E0C6FDDC1F588C65DE260F13`
- `batch15_exercises_letter.json`：`E56507EFD817EE3F3D529DEE402D4D4604342C5733C1C414F4976B5A6002C645`
- `batch16_exercises_letter.json`：`67F50DA66ACA1BCB6387D81629C67ECFB1500C34819243CA5E0B3D62E9765F21`

## 冻结状态

独立审校通过：原稿保留 109 条、修订 11 条、拒绝 0 条。三份 JSON 已按上述 SHA-256 冻结，可写库；冻结后不得再编辑，除非重新开启审校并重新计算哈希。
