# 基础语言学 V1 出题契约

本目录的基础语言学题目独立于动漫语料。题目不得伪造作品、集数、角色、
字幕行或来源 ID，也不得复用动漫台词。脚本只能用于选取、校验、哈希、
合并审核 overlay 和数据库传输；所有教学文本必须由 Agent 逐条人工创作。

## 发布单位

- 共 3 个 wave；每个 wave 固定 20 个主题、80 道题。
- 每个主题恰有 `F1`、`F2`、`F3`、`F4` 各一道。
- `F1` 为识别，`F2` 为解析，`F3` 为对比诊断，`F4` 为迁移应用。
- 每题 4 个互相平行且不重叠的选项，恰有一个可辩护的正确答案。
- 每个 wave 的正确答案位置必须平衡为 A/B/C/D 各 20 道。
- 生成者与独立复核者必须是不同 Agent。未完成 80/80 逐题复核的 wave
  不得进入数据库。

## Generation JSON

```json
{
  "pack": {
    "id": "foundation-v1-wave-1",
    "curriculum_version": "foundation-v1",
    "batch_no": 1,
    "title_zh": "题包标题",
    "description_zh": "题包说明",
    "topic_count": 20,
    "question_count": 80,
    "generator_agent": "/root/example_generator"
  },
  "topic_ids": ["topic_id"],
  "questions": [
    {
      "id": "foundation-v1-topic-id-f1",
      "topic_id": "topic_id",
      "stage": "F1",
      "question_type": "single_choice",
      "source_kind": "original_sentence",
      "stimulus": {
        "kind": "sentence",
        "ja_text": "原创日语例句",
        "zh_context": "必要时提供的中性语境"
      },
      "prompt_zh": "题干",
      "options": [
        {"id": "A", "text": "选项 A"},
        {"id": "B", "text": "选项 B"},
        {"id": "C", "text": "选项 C"},
        {"id": "D", "text": "选项 D"}
      ],
      "answer": {"option_id": "A"},
      "hint_zh": "不泄露答案的提示",
      "explanation_zh": "直接解释本题结论",
      "deep_explanation_zh": "说明形态、句法、语义或语用机制",
      "caution_note_zh": "指出边界条件或常见误判",
      "wrong_explanations": {
        "B": "B 错误的具体原因",
        "C": "C 错误的具体原因",
        "D": "D 错误的具体原因"
      },
      "transfer_example_ja": "可选的迁移例句",
      "transfer_explanation_zh": "与迁移例句成对出现的说明",
      "difficulty": 1,
      "tags": ["topic_tag"],
      "sort_order": 0
    }
  ]
}
```

`question_type` 只允许：

- `single_choice`
- `morphology_analysis`
- `syntax_relation`
- `contrast_choice`
- `kuuki_yomi`

`source_kind` 只允许：

- `original_sentence`
- `minimal_pair`
- `constructed_dialogue`
- `metalinguistic`

`stimulus.kind` 只允许 `sentence`、`dialogue`、`contrast`、`metalinguistic`。
对话使用 `turns` 数组，并明确必要的说话人；对照使用 `items` 数组。禁止用空
字符串或虚构来源占位。

## 逐题质量要求

每题必须结合适用范围核对：

- 日语形态切分、活用和词类归属；
- 句法结构、论元、格关系、从句边界和省略恢复；
- 词义、组合意义、作用域、时制、体、情态与语态；
- 会话含意、言语行为、礼貌、语域和社会索引；
- 说话人、听话人、指代对象、视点与跨话轮上下文；
- 否定的形式与作用域；
- 原创例句的自然度、语用成立条件和方言/时代边界。

选项应保持同一分析层级，不得通过长度、术语格式或明显褒贬暴露答案。
错误选项说明必须针对本题语料解释“为什么错”，不能只写“与题意不符”。
音高重音、方言、性别/年龄语言等主题必须明确描述体系和适用范围，避免把
概率性倾向写成所有说话人的固定规则。

## 独立复核 overlay

复核文件必须覆盖 80 个唯一 ID：

```json
{
  "reviewer_agent": "/root/example_reviewer",
  "reviewed_count": 80,
  "generation_sha256": "生成 JSON 的 SHA-256",
  "decisions": [
    {
      "id": "foundation-v1-topic-id-f1",
      "decision": "keep",
      "quality_score": 96,
      "note": "逐题复核结论"
    }
  ],
  "replacements": []
}
```

- `decision` 只允许 `keep`、`revise`、`reject`。
- `revise` 必须在 `replacements` 中提供同 ID 的完整题目；overlay 按 ID
  替换，绝不能追加。
- `keep` 不得提供 replacement。
- 出现 `reject`、缺失 ID、额外 ID、重复 ID、质量分低于 95，整包不得发布。
- 复核 Markdown 必须含 80 行逐题台账，并记录所有修订的理由。
