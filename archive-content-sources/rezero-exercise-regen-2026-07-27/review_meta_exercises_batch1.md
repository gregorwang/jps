# Meta 练习交叉复核报告（Batch 1）

## 复核范围与结论

- 文件：`regen/batch1_meta_exercises.json`
- 数量：80 条
- 来源：`learning_exercises` 80 条；主学习项为 grammar 1 条、sentence 79 条
- 字幕证据：EP27、EP38、EP39 的日文 `subtitle_lines`
- 数据库操作：仅执行只读查询，写入 0

| 结果 | 数量 |
| --- | ---: |
| 原样通过 | 68 |
| 修正后通过 | 12 |
| 驳回 | 0 |
| **合计** | **80** |

所有题目最终均能保留。复核时以日文字幕为准；数据库中若有 `source_line_no` 偏移、句子摘录截断或中文字幕错位，不把这些问题带进答案。

## 已修正的 12 条

| ID | 原问题 | 修正 |
| --- | --- | --- |
| `25d946d9-ec77-5669-befe-fca14c2a9c6b` | 把「報われない」的一个释义写成“感情不值得”，混淆“没有得到回应”与“没有价值”。 | 改为莎缇拉的心意没有得到理解和回应，并明确不是“不值得”。 |
| `84df8544-88bd-5726-a3d2-3dc850c6cb73` | 把「泣かされる」直接写成「泣かせる」再被动化，漏掉规则形「泣かせられる」与缩约关系。 | 改为规则使役被动「泣かせられる」在口语中缩为「泣かされる」，并重写提示。 |
| `7db5c4a2-1fa4-5cac-9652-bfd3f6e24844` | 把父亲的「期待してるぜ、息子」误归到第二场试炼。 | 改为昴回想起第一场试炼中的父亲。 |
| `2672ffde-26b9-597a-b7f1-fea4460a2baf` | 真实字幕写「てばぁ」，题目却不解释便直接称作「ってば」。 | 明确「てばぁ」对应常见形式「ってば」，再分析拉长音和语用。 |
| `7ea65e27-fc14-5885-8015-feef5aa3abf3` | 把「そう」只说成回指事件，未准确说明它直接代替前句判断「やはり面白い」；对「かい」的描述也容易误解为男性说话人专用。 | 补足命题回指，并改成“略带男性化或老派色彩”，不把形式和说话人性别绑定。 |
| `ecd05628-cd23-5535-bfd6-429c9ae6b2f3` | 题面说笑声出现在“一连串感谢后”，但实际是第一句感谢后发笑，昴随后继续道谢。 | 按 EP38 lines 205–216 校正事件顺序。 |
| `f7f53bf3-ff78-5c80-bae9-9bab09dda60d` | 中文误写“粗略口语”。 | 改为“粗口语”。 |
| `76410362-d80d-5370-9548-81f163122aae` | 日文引文开头多一个无意义空格。 | 删除空格。 |
| `9d6d5e10-e663-525e-b68b-e9051989cf8e` | 将「足元」解释成不自然的“眼前的基础”，也弱化了现场扭打中的字面脚下／防守义。 | 同时说明现场的脚下、防守义及“忽略眼前人和事”的语境讽刺。 |
| `d3e7f2e8-6b32-5827-93e7-00c1fa28b154` | 中文误写“粗体「かよ」”。 | 改为“粗硬的「かよ」”。 |
| `9d3c22cc-b0b6-5040-a9a4-594c258d3e50` | 答案把字幕仅写作「本」的物品直接定名为“福音书”，超出本题所需证据。 | 收紧为“手中的那本书”。 |
| `5dedd40e-17b7-505b-96a3-0002cb731a1f` | 把女性角色的「王様」直译成中文“国王”，不够自然。 | 改为“成为王、登上王位”。 |

## 精确 ID / source 映射

只读联表验证结果：

- 候选 ID 命中 `learning_exercises`：80/80
- review note 中主 source 命中真实学习项：80/80
- exercise 与 source 的 episode 一致：80/80
- 重复 ID：0
- 映射失败：0

| # | Exercise ID | 类型 | Source ID |
| ---: | --- | --- | --- |
| 01 | `22945b06-ba67-5b72-a07e-531fb0f6d047` | grammar | `2bfb5697-f011-585f-9a33-2dda952bbe28` |
| 02 | `d50a8797-befd-5cb4-af3c-62c28ffc1bc1` | sentence | `re-zero-s02e13-sentence-001` |
| 03 | `26dc64b4-33a2-52cb-8fd0-36880190ad5f` | sentence | `re-zero-s02e13-sentence-006` |
| 04 | `1ad03eed-8cde-5045-a25d-d2d43de97c28` | sentence | `re-zero-s02e13-sentence-008` |
| 05 | `59d947e4-d33e-57a1-8fe0-fc70cfee21c8` | sentence | `re-zero-s02e13-sentence-013` |
| 06 | `25d946d9-ec77-5669-befe-fca14c2a9c6b` | sentence | `re-zero-s02e13-sentence-019` |
| 07 | `51d7a194-541d-5297-8633-55617ed8857d` | sentence | `re-zero-s02e13-sentence-024` |
| 08 | `0c79ec8a-d973-5c4d-b113-8a8953da5dab` | sentence | `re-zero-s02e13-sentence-025` |
| 09 | `8114b9ca-e790-5ac1-a6c6-853b42bde9fb` | sentence | `re-zero-s02e13-sentence-029` |
| 10 | `3e544549-988d-5ce3-9864-2c774ee94c8e` | sentence | `re-zero-s02e13-sentence-033` |
| 11 | `3375138b-1315-54a8-aed4-a8995f65f443` | sentence | `re-zero-s02e13-sentence-035` |
| 12 | `84df8544-88bd-5726-a3d2-3dc850c6cb73` | sentence | `re-zero-s02e13-sentence-036` |
| 13 | `2da6db9e-c2ce-5535-80f5-4f93e04e0519` | sentence | `re-zero-s02e13-sentence-037` |
| 14 | `c5b7b42a-97c6-57ea-a15c-c31481982c2b` | sentence | `re-zero-s02e13-sentence-041` |
| 15 | `376c5753-ca6c-5f55-b797-a06b09f3da63` | sentence | `re-zero-s02e13-sentence-042` |
| 16 | `b331baf2-9bd3-5e78-bdb0-a22e53ec324b` | sentence | `re-zero-s02e13-sentence-044` |
| 17 | `7db5c4a2-1fa4-5cac-9652-bfd3f6e24844` | sentence | `re-zero-s02e13-sentence-049` |
| 18 | `6001392d-58d0-5044-b4e3-1190ba85a1a6` | sentence | `re-zero-s02e13-sentence-050` |
| 19 | `67d81899-eba9-5a05-9db2-6f53b5c0e361` | sentence | `re-zero-s02e13-sentence-051` |
| 20 | `e3992ce1-4ca0-59c9-9387-8ead826ced2b` | sentence | `re-zero-s02e13-sentence-052` |
| 21 | `486f3142-0e3d-5db8-aec2-5b03af14ba3c` | sentence | `re-zero-s02e13-sentence-053` |
| 22 | `4df90042-c069-5d63-8192-a0175c0c2986` | sentence | `re-zero-s02e13-sentence-060` |
| 23 | `3a6f4996-3fcd-5839-8a3d-a6f11af3afc2` | sentence | `re-zero-s02e13-sentence-061` |
| 24 | `2672ffde-26b9-597a-b7f1-fea4460a2baf` | sentence | `re-zero-s02e13-sentence-062` |
| 25 | `793d00f5-cbfb-592b-8e62-0d6ab7d50f9a` | sentence | `re-zero-s02e13-sentence-063` |
| 26 | `3c5e885a-8090-5818-9e2a-91c674ea0f0b` | sentence | `re-zero-s02e13-sentence-064` |
| 27 | `b9731939-67e9-5eec-8cd7-f91b86e445df` | sentence | `re-zero-s02e13-sentence-066` |
| 28 | `7979bb49-b859-5d7c-b6f7-5ebc2fb728a4` | sentence | `re-zero-s02e13-sentence-067` |
| 29 | `fe0a4334-9070-5e2e-b3a9-adf0fc879c16` | sentence | `re-zero-s02e13-sentence-068` |
| 30 | `7ea65e27-fc14-5885-8015-feef5aa3abf3` | sentence | `re-zero-s02e13-sentence-070` |
| 31 | `f99765cd-e6b8-50e2-bf3b-fe08d591b41e` | sentence | `re-zero-s02e13-sentence-074` |
| 32 | `1bd04ef5-48d1-5f4f-9b84-af68bed90118` | sentence | `re-zero-s02e13-sentence-076` |
| 33 | `eb28106e-7989-5bdc-9b62-21b79a5f3732` | sentence | `re-zero-s02e13-sentence-077` |
| 34 | `c3de0d7b-0d4b-51ef-b5fb-8ce5de1b33d4` | sentence | `re-zero-s02e13-sentence-082` |
| 35 | `3b45c350-bcf0-5e70-8d10-aa1917a540d5` | sentence | `re-zero-s02e13-sentence-086` |
| 36 | `1b47519c-43ce-5f3e-b371-74215068c9ff` | sentence | `re-zero-s02e13-sentence-087` |
| 37 | `15c59f39-9003-5dcd-9bbc-99a84a262157` | sentence | `re-zero-s02e13-sentence-091` |
| 38 | `1383f7f3-e42e-57d1-be3f-67e3db5fc87d` | sentence | `re-zero-s02e13-sentence-093` |
| 39 | `747f2262-bf65-5b50-b886-44a53a80a363` | sentence | `re-zero-s02e13-sentence-095` |
| 40 | `aad59699-de5a-5809-a3fe-3e3851fe312a` | sentence | `re-zero-s02e13-sentence-096` |
| 41 | `ecd05628-cd23-5535-bfd6-429c9ae6b2f3` | sentence | `re-zero-s02e13-sentence-100` |
| 42 | `413b1138-db6f-5565-a668-b83ca5787edd` | sentence | `re-zero-s02e13-sentence-104` |
| 43 | `cb2caa9c-1467-55ef-be8e-f3737ab8bf3e` | sentence | `re-zero-s02e13-sentence-108` |
| 44 | `f7f53bf3-ff78-5c80-bae9-9bab09dda60d` | sentence | `re-zero-s02e13-sentence-109` |
| 45 | `192bab5a-a436-58b3-9491-778140abfd2c` | sentence | `re-zero-s02e13-sentence-112` |
| 46 | `76410362-d80d-5370-9548-81f163122aae` | sentence | `re-zero-s02e13-sentence-113` |
| 47 | `ae19e3a1-9a4f-5d56-a5d0-49d51bbe0ee3` | sentence | `re-zero-s02e13-sentence-114` |
| 48 | `5d45433c-40d9-54bd-902d-933573f14886` | sentence | `re-zero-s02e13-sentence-115` |
| 49 | `682eaeb1-dae8-5f1d-9144-5524f0da4cdf` | sentence | `re-zero-s02e13-sentence-116` |
| 50 | `7521f45e-cca9-571b-aa8f-962bf279c341` | sentence | `re-zero-s02e13-sentence-117` |
| 51 | `81ee164a-626a-50e4-8d8d-06a7d74117bf` | sentence | `re-zero-s02e13-sentence-118` |
| 52 | `11e847cf-e780-5701-9b20-11b13f75ebd1` | sentence | `re-zero-s02e14-sentence-004` |
| 53 | `bc4783e9-79e9-53de-9c97-bc0b62074814` | sentence | `re-zero-s02e14-sentence-005` |
| 54 | `ea3da312-b23a-5ef2-8a9d-bcd05f2559d3` | sentence | `re-zero-s02e14-sentence-007` |
| 55 | `9d6d5e10-e663-525e-b68b-e9051989cf8e` | sentence | `re-zero-s02e14-sentence-008` |
| 56 | `c615151c-e6e2-5fb4-aa49-fecc7d0a3e01` | sentence | `re-zero-s02e14-sentence-009` |
| 57 | `d3e7f2e8-6b32-5827-93e7-00c1fa28b154` | sentence | `re-zero-s02e14-sentence-011` |
| 58 | `f689f4d5-ac0d-5670-8a8e-064f19e8056d` | sentence | `re-zero-s02e14-sentence-014` |
| 59 | `c34ec3fd-84c2-5a6a-b736-510a4f17a19a` | sentence | `re-zero-s02e14-sentence-015` |
| 60 | `76decf6f-b803-52f1-9432-3610d6aea160` | sentence | `re-zero-s02e14-sentence-020` |
| 61 | `8f0895bc-3532-51a7-9da7-d7ab200e1a86` | sentence | `re-zero-s02e14-sentence-023` |
| 62 | `5fd1c337-ea44-50f6-a15d-37ab4a931290` | sentence | `re-zero-s02e14-sentence-024` |
| 63 | `68e187da-81b7-5023-8980-5d0b4703feb3` | sentence | `re-zero-s02e14-sentence-025` |
| 64 | `0d629479-6aad-5769-b429-4446260d756c` | sentence | `re-zero-s02e14-sentence-026` |
| 65 | `93998110-8fba-56f7-b6be-47943127ad0d` | sentence | `re-zero-s02e14-sentence-028` |
| 66 | `98948e26-7a26-599f-9c81-74590b591253` | sentence | `re-zero-s02e14-sentence-029` |
| 67 | `b1588946-3b23-52e9-8144-42d8f6fbaf41` | sentence | `re-zero-s02e14-sentence-032` |
| 68 | `d30d7b9a-84d6-59dd-a257-13575b2f801f` | sentence | `re-zero-s02e14-sentence-033` |
| 69 | `f65a3887-5b12-5217-bec7-3546b65ada14` | sentence | `re-zero-s02e14-sentence-034` |
| 70 | `9d3c22cc-b0b6-5040-a9a4-594c258d3e50` | sentence | `re-zero-s02e14-sentence-035` |
| 71 | `5dedd40e-17b7-505b-96a3-0002cb731a1f` | sentence | `re-zero-s02e14-sentence-037` |
| 72 | `666add15-ea99-50ce-9013-7ea7c6c9ccb2` | sentence | `re-zero-s02e14-sentence-039` |
| 73 | `4465c905-46b1-5529-858f-7915333e4206` | sentence | `re-zero-s02e14-sentence-042` |
| 74 | `9be23f22-6487-5753-9bf0-d6a131dd007d` | sentence | `re-zero-s02e14-sentence-043` |
| 75 | `9faf6db1-c08a-532e-9703-f17c7346d4c9` | sentence | `re-zero-s02e14-sentence-044` |
| 76 | `0d917cb8-3e27-516e-81c7-0a12a095ccc3` | sentence | `re-zero-s02e14-sentence-046` |
| 77 | `469b477f-6c4a-57b1-9446-62f6fdcef7c7` | sentence | `re-zero-s02e14-sentence-047` |
| 78 | `3d347d27-77df-514d-ae6f-190e82cf77be` | sentence | `re-zero-s02e14-sentence-048` |
| 79 | `3358f299-a79d-515c-88e0-1b84c649f6f2` | sentence | `re-zero-s02e14-sentence-049` |
| 80 | `f5fa1fa4-1792-52d3-a89a-f2dee7268b3a` | sentence | `re-zero-s02e14-sentence-050` |

另有 8 个辅助引用也全部存在并与题面一致：

- grammar：`e1911187-cbfa-5dcd-aaf4-bc8dd6ade32a`（〜だけでよかった）
- grammar：`de130cb0-6a47-559c-b3eb-ac4c6e54aa2b`（〜てくれる）
- grammar：`476c4b44-8548-54b5-ab74-803f6147dd14`（〜として）
- grammar：`99fa913b-da48-5e58-a14a-67faed1dcada`（〜ということ）
- vocab：`a9686674-aa27-57fd-806b-bb28902d709b`（悪態）
- vocab：`vocab-re-zero-kyouryoku`（協力）
- vocab：`43e73176-b549-5749-a6fd-59144346028f`（友達）
- vocab：`823fc7d2-fd3c-59cc-b863-eb56c3c8e945`（王様）

## 字幕与内容检查

- EP27 的 grammar 例句实见于字幕 line 245；源语法项的 `source_line_no=247` 有偏移。
- EP38 的 50 条 sentence 主来源逐项与真实日文字幕核对，覆盖 lines 1–255。
- EP39 的 29 条 sentence 主来源逐项与真实日文字幕核对，覆盖 lines 3–97。
- 对跨行结构均恢复了必要前件或后件，但没有把推测内容伪装成原字幕。
- 题面均可脱离数据库字段独立理解；答案均回应题目提出的语言点；提示只给观察路径，不直接给出结论。

## 最终校验

执行：

```text
node regen\validate_candidates.mjs regen\batch1_meta_exercises.json
```

结果：

```json
{
  "files": 1,
  "rows": 80,
  "uniqueIds": 80,
  "errors": [],
  "warnings": []
}
```

附加检查：

- 每条且仅含 `id`、`prompt`、`answer`、`hint`、`review_note` 五个非空字符串字段。
- learner-facing 字段无内部 ID、模板字样、选项占位或答案元信息。
- 重复 prompt：0；重复 hint：0。
- answer 与 hint 的最长连续公共片段达到 8 字者：0。
- 数据库写入：0。
