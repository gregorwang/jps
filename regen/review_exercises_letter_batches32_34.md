# Letter exercises batches 32–34 独立交叉审校

## 结论

本轮独立复核 EP21 exercise 063–183（数据库无 exercise 165）的 120 条候选。复核逐条对照 EP21 真实日文字幕、连续话轮、数据库原题及关联 vocab／grammar／sentence；生成侧 QA 仅作线索，没有代替独立判断。教学文本没有通过 Python、JavaScript 或 SQL 模板生成。

由于软件崩溃后的 Windows 沙箱不能原位修改既有文件，本轮以三份生成稿加 11 条完整审校 overlay 的方式冻结：

- 基础稿：`batch32_exercises_letter.json`、`batch33_exercises_letter.json`、`batch34_exercises_letter.json`
- 审校覆盖：`review_overlay_exercises_letter_batches32_34.json`
- 合并规则：按 `id` 以 overlay 的完整五字段对象覆盖基础稿同 ID；其余对象保持基础稿

| 批次 | 条数 | 原稿保留 | 审校修订 | 拒绝 |
| --- | ---: | ---: | ---: | ---: |
| batches 32–34 | 120 | 109 | 11 | 0 |

## 主要修订

1. `exercise-114`：删除“高风险必然等于失败概率过半”的概率过推。
2. `exercise-122`：删除字幕文字不能证明的“带笑意”，保留反问的论证功能。
3. `exercise-128`、`exercise-180`：结合上一句 `我々`，明确库珥修的意向形答复同时代表她所率一方，不硬判为单人决定或单纯号召。
4. `exercise-142`：保留被动态的施受方向，删除原稿无依据补出的有生施事“人”。
5. `exercise-149`：纳入紧接的 `誰にでも`，把 `俺も／お前も／誰にでも` 作为后置强调列举，不机械唯一化省略助词。
6. `exercise-161`：把 `すっげえ` 细化为 `すごい → すげえ` 的元音变化加促音强化。
7. `exercise-166`：将关西话 `何や` 准确收窄为副词性 `なんや ≈ 何だか／どうも`，并补明 `なっとる < なっておる`，避免误写成不定代词 `何か`。
8. `exercise-169`：把 `頼めるか` 说明为缓和请求的可能性疑问，不误说成单纯测试听者能力。
9. `exercise-177`：将句末 `けど` 接回下一句 `乗るか`，不把拖尾本身直接等同于听者回应。
10. `exercise-182`：补明惯用比喻 `吹けば飛ぶ` 中 `吹く` 通常省略风这一主语，消除施事歧义。

## 结构与内容校验

overlay 正式 validator 结果：

- rows：11
- unique IDs：11
- errors：0
- warnings：0

合并后的 120 条候选结论为：

- 原稿保留 109、审校修订 11、拒绝 0。
- 读音、拍数、长音、拨音、促音、拗音及目标词界逐项复核。
- 使役／被动／可能／意向／否定命令、否定范围、引用、话题、方言、说话人、指代和证据等级逐项复核。
- learner-facing `prompt / answer / hint` 不包含数据库、manifest、内部 ID、旧选项答案或模板元话语。

## 数据库只读预检

审校冻结时的只读结果：

- targets：120；unique IDs：120。
- 首尾：`re-zero-s01e21-exercise-063` → `re-zero-s01e21-exercise-183`；exercise 165 不存在。
- ordered comma-MD5：`24c1ffd15608a3c348a2e040b7e8bf79`。
- 维护备份命中：120/120。
- 对 `work_slug / episode_id / episode / exercise_type / prompt / answer / hint / difficulty / vocab_item_id / sort_order / updated_at` 做空值安全全字段比较：120/120 未漂移。
- 当前单字母答案：120/120。
- manifest 任意状态重叠：0；已应用重叠：0。
- 本独立复核没有写数据库。

## 冻结 SHA-256

- `batch32_exercises_letter.json`：`3DE4B1FAC002420DAF9BBFE49C82A3535811275C933CEAC59E90915202937C30`
- `batch33_exercises_letter.json`：`48CCC880499E4E3EF73C8E1309EEA5628054AA750057165D330A5A2E0795824E`
- `batch34_exercises_letter.json`：`A35077948C045E256229E7B3E96D2035B7E9A03FD9A4A268D3D643D575C7A160`
- `review_overlay_exercises_letter_batches32_34.json`：`99802367A1D47DBF98CD5041243483838DC1FD747347D2FD805ABA9A265CC3A9`

上述四个文件及“按 ID 以 overlay 覆盖基础稿”的合并规则共同构成本轮冻结候选。
