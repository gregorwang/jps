# Letter-answer exercises batches 11–13：生成侧 QA

## 结论

本批从数据库当前未应用的单字母答案练习中，严格按 `episode, sort_order, id` 取前 120 条，逐题读取学习项与真实日文字幕后人工重写。JavaScript 仅用于 JSON、ID、重复和泄漏校验，没有生成教学文本；全程没有写数据库。

| 文件 | 条数 | 范围 |
|---|---:|---|
| `batch11_exercises_letter.json` | 40 | EP17 exercise 168–208（原库无 198） |
| `batch12_exercises_letter.json` | 40 | EP17 exercise 209–240；EP18 exercise 001–008 |
| `batch13_exercises_letter.json` | 40 | EP18 exercise 009–048 |
| **合计** | **120** | EP17 72；EP18 48 |

## 选集冻结

筛选条件：

1. `work_slug = 're-zero'`。
2. 原 `answer` 去空白后为单个 `A/B/C/D` 字母。
3. 排除 `maintenance.regen_20260727_manifest` 中同 ID 且 `applied_at is not null` 的练习。
4. 按 `episode, sort_order, id` 排序，取前 120。

核对结果：

- 数据库选集：120 条、120 个唯一 ID。
- 候选文件：120 条、120 个唯一 ID。
- 数据库有序 ID MD5：`4da421a9fc61ad58c59a18a38f8ea6a1`。
- 候选有序 ID MD5：`4da421a9fc61ad58c59a18a38f8ea6a1`。
- 首条：`re-zero-s01e17-exercise-168`。
- 末条：`re-zero-s01e18-exercise-048`。

## Source 与字幕证据

### EP17

- 40 条 grammar exercise 涉及的 30 个真实 grammar source 均存在且带日文例句。
- 32 条 sentence/tone exercise 均按 EP17 连续字幕话轮重写。
- 原练习所引日文 72/72 均能在 EP17 字幕中定位。
- 全部 120 条原引句中，117 条经去空白与标点后精确命中；另 3 条只因字幕增加括号注音而表面不同：
  - `風除(よ)け`
  - `地竜(ちりゅう)`
  - `剣聖(けんせい)`
- 上述 3 条已逐字人工确认，非内容错配。

### EP18

- 实际使用的 19 个 vocab source 全部存在，且 19/19 具备非空读音和中文释义。
- 目标字幕集中在 EP18 lines 3–35，均逐行连读上下文。
- 对重复出现的 `脳`、`震える`、`精霊`、`信仰`，每题分别绑定自身搭配、角色态度或句法位置，不复制同一空泛答案。

## 源题错误的证据性修正

本批没有机械继承源题标签，明确修正了以下错误：

- `exercise-184`
  - 源题把陈述理由的 `ないんですから` 误标成疑问 `んですか`。
  - 候选按 `のだ + から` 的说明理由结构重写。
- `exercise-193`
  - 源题把 `おかしくなったんじゃないですか` 误标成禁止式 `〜ないで`。
  - 候选按负面确认／责问重写。
- `exercise-197`
  - 源 source 把 `どれだけ` 错释为限定“只有／至少”。
  - 候选按程度疑问“究竟有多……”重写。
- `exercise-204`
  - 源题从接续词 `だけど` 中误匹配限定助词 `だけ`。
  - 候选按转折“但是”及前后论证关系重写。
- `exercise-205`
  - 源题把副词 `幸いにも` 中的 `にも` 泛释为追加“也”。
  - 候选明确其评价对象是昴与奥托的暂时逃脱，并说明为何该措辞激怒昴。
- EP18 `exercise-005`
  - 问题目标是句中 `黙って`，源提示却只给基本形读音 `だまる`。
  - 候选回答实际活用读音 `だまって`，再补基本形。
- EP18 `exercise-015`
  - 问题目标是句中 `怠った`，源提示却只给基本形读音 `おこたる`。
  - 候选回答实际活用读音 `おこたった`，再补基本形。
- EP18 `exercise-035`、`exercise-036`
  - 原题目标写 `生きてくれ`，但所引字幕实际为 `悠久の時を生きるがゆえに`。
  - 候选改为真实词形 `生きる（いきる）`，并排除原题虚构的“请活下去／和我一起活”语义。

## 角色与指代核对

- EP17 `分からないんですか` 至 `白鯨です` 的说话人为奥托。
- 雷姆下车迎击；昴与奥托继续乘车。`僕らの竜車を逃がすために` 的受益方是车上两人。
- `今は幸いにも逃げきったはず` 的省略主语是昴与奥托的龙车一方，不是雷姆。
- `レムって誰のことです？` 是奥托真实的记忆空缺，不是嘲讽。
- EP18 `黙っていなよ、スバル`、`本気で僕を殺したいなら`、`精霊相手に時間を語るな`、`死が罰にすらならない` 的说话人为帕克。
- EP18 对帕克提出 `即座にしとめるべきだった`、主张 `信仰の深さに時間など関係ない`、说 `試練は果たされた` 的是培提其乌斯。

## 最终 QA

运行：

```text
node regen\validate_candidates.mjs regen\batch11_exercises_letter.json regen\batch12_exercises_letter.json regen\batch13_exercises_letter.json
```

结果：

- files：3
- rows：120
- unique IDs：120
- errors：0
- warnings：0

附加检查：

- 精确五字段 schema：120/120。
- 空字段：0。
- 单字母答案残留：0。
- `用于…`、选项、答案元话语或内部 source ID 泄漏：0。
- 归一化重复 prompt：0。
- 归一化重复 prompt-answer：0。
- 与既有 exercise 候选的精确 prompt 重复：0。
- 与既有 exercise 候选的精确 prompt-answer 重复：0。
- hint 完整包含 answer：0。
- 归一化 hint-answer 最长公共连续片段达到 8 字符：0。

SHA-256：

- `batch11_exercises_letter.json`：`202EE6682C08B8A398332895FE38ABC9E0533438C5C4D3D22F8E838CD4276CB7`
- `batch12_exercises_letter.json`：`255DE58684C118CF72638D3B5C168DCBB5EAABD317904B77052BCC357C13E855`
- `batch13_exercises_letter.json`：`4E81F2EBDA5E8CB18D110BD161F36EB4EB64B7F2675C03C027F176C9F6B1D455`

以上三个文件已完成生成侧 QA，可释放给独立复核者；本报告不代表交叉复核结论。
