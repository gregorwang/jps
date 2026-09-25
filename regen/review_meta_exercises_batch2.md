# Meta 练习交叉复核报告（Batch 2）

## 复核范围与结论

- 文件：`regen/batch2_meta_exercises.json`
- 数量：80 条
- 主来源：`learning_sentences` 79 条；EP39 真实字幕直取 1 条
- 辅助来源：`learning_grammar_points` 2 条
- 字幕证据：EP39 日文字幕 lines 102–254；另回查 EP32 lines 264–265 识别席玛回忆引语
- 数据库操作：仅执行只读查询，写入 0

| 结果 | 数量 |
| --- | ---: |
| 原样通过 | 66 |
| 修正后通过 | 14 |
| 驳回 | 0 |
| **合计** | **80** |

所有题目最终均可保留。主要实质问题有两组：一是把拉姆给出的提示以及「本人」连续误指为罗兹瓦尔；二是把 EP39 中重现的席玛旧台词误当成当前对话中新作出的承诺。其余修正涉及形态切分、指示词范围、语义角色、证据边界和中文自然度。

## 已修正的 14 条

| ID | 原问题 | 修正 |
| --- | --- | --- |
| `e0cbc199-7a1f-59cd-b0ca-ebe895c9199c` | 答案出现“词义题答案”这类模板流程措辞，影响学习者阅读。 | 直接说明「スバル君」是独立呼语及其注意管理、语气转折功能。 |
| `84c32c98-d527-5288-a0b4-0364c8d79c7c` | 中文“粗口语缩约”不自然。 | 改为“粗硬口语缩约”。 |
| `07d7ad8c-66cb-5b4d-bb51-9f52bfe50cbe` | 把「気付けずにいる」不严谨地切成「気付けず＋にいる」。 | 明确「気付けず」相当于「気付けない」，「ずにいる」表示否定状态持续。 |
| `28d6b1f8-b138-53bd-b264-92984b69ca31` | 把「アドバイスしてくれる」的施事者「あいつ」错认成罗兹瓦尔，并使用男性代词。 | 按 EP39 lines 130–137 改为拉姆；她刚指出爱蜜莉雅受挫的原因。 |
| `4e7310b2-0dff-5686-ad94-7012d6ae9673` | 把奥托所指的旧玩笑写成“罗兹瓦尔喜欢我”。 | 改为昴又在说“拉姆也喜欢我”。 |
| `25aaadf5-b6a0-51ee-b44c-983496f341a6` | 把「本人」及两个被动句的动作发出者错认成罗兹瓦尔。 | 明确「本人」是拉姆；若被她听见，昴会遭她严厉斥责。 |
| `22845df6-bbb1-5ded-a233-1a30325b68c3` | 把奥托的应答写成“允许昴先处理”，凭空增加上下级许可关系。 | 改为奥托理解并接受顺序调整，随后按既定分工行动。 |
| `aa3c9dcf-e3ad-55b1-9f5c-1e603910e851` | 将「ここの目的」直接扩成“圣域及复制体系统的目的”，指称范围过满。 | 收紧为眼前圣域这一场所为何设立、承担何种功能；复制体信息只是相关内幕。 |
| `90f22a09-4a45-562e-81eb-8a40be830766` | 题面称昴“逐一询问”四人姓名，超出当前字幕证据。 | 改为先自报全名，再询问眼前这一位的姓名。 |
| `7d5355f7-f5ca-5122-84f7-8c7841dd2638` | 把「口外はせん」当作 EP39 当前对话的新承诺，漏掉跨集回忆和说话人。 | 回查 EP32 lines 264–265，明确这是席玛先前的承诺在 EP39 被昴回想起来。 |
| `8b6e57bf-89a3-5911-b6e7-a11c5b4d3663` | 未说明「名に懸けて誓おう」同样属于席玛旧话，也未利用“本人以本人姓名起誓”的识别作用。 | 明确说话人为席玛、担保物是自己的姓名与名誉，并说明昴凭此认出席玛。 |
| `0cbe4fb4-f80b-5081-bc02-5c04d7cf8ac5` | 题面把「問題を起こしてな」末尾笼统标成「てな」，容易误导成一个固定形式。 | 分开说明「起こして」的连接作用与句末「な」的长者口吻。 |
| `2cd780b6-1d55-565b-9b37-8f8125876ff8` | 「会っておらん」的还原不够精确。 | 明确助动性「おる＝いる」「おらん＝いない」，整段等于「会っていない」。 |
| `79453985-1fe0-58aa-ac0a-f6c8f985a15a` | 「共有する」只写席玛“拥有记忆”，没有补全共同者及记忆内容。 | 明确席玛与嘉飞尔共享他在墓所试炼中被迫看到的过去，由此推出两人私下见面并不奇怪。 |

## 精确 ID / source 映射

只读联表与字幕匹配结果：

- 候选 exercise ID 命中 `learning_exercises`：80/80
- 候选 exercise 的 episode 为 EP39：80/80
- review note 中主 sentence 命中 `learning_sentences`：79/79
- 79 条 sentence 的日文经去空格、引号和破折号规范化后命中 EP39 真实日文字幕：79/79
- 无独立 sentence 的 `分かりました` 命中 EP39 subtitle line 151：1/1
- 两个辅助 grammar source 命中且 episode 为 EP39：2/2
- 已应用审计清单重叠：0
- 本地与数据库排序后 ID 集合 MD5 均为 `12479576cd9e1126f18dad7d92e21ba3`

| # | Exercise ID | 主来源 |
| ---: | --- | --- |
| 01 | `5d0150fa-f415-502a-a889-a91546ad10e6` | `re-zero-s02e14-sentence-056` |
| 02 | `2437f275-ac41-5ec4-baf4-d63c3599c82a` | `re-zero-s02e14-sentence-057` |
| 03 | `a79e6480-0331-5101-bcd7-cd7293e0fe25` | `re-zero-s02e14-sentence-058` |
| 04 | `e0cbc199-7a1f-59cd-b0ca-ebe895c9199c` | `re-zero-s02e14-sentence-060` |
| 05 | `a278566a-a735-595f-9205-d8d17a04c4a3` | `re-zero-s02e14-sentence-061` |
| 06 | `dfe4c601-4396-5228-9523-48d3b38a1701` | `re-zero-s02e14-sentence-062` |
| 07 | `84c32c98-d527-5288-a0b4-0364c8d79c7c` | `re-zero-s02e14-sentence-063` |
| 08 | `edde6363-9e4c-5b23-9536-29cef93d81cc` | `re-zero-s02e14-sentence-064` |
| 09 | `45bc84e0-f765-560c-ae70-4f250397f90a` | `re-zero-s02e14-sentence-065` |
| 10 | `f0ece553-39b8-54da-9611-779dbecfa5b8` | `re-zero-s02e14-sentence-066` |
| 11 | `d591d328-195c-531f-8b24-eca9b88167ae` | `re-zero-s02e14-sentence-067` |
| 12 | `e81efe3b-9f72-5293-a210-5ff5ac0fc287` | `re-zero-s02e14-sentence-070` |
| 13 | `a4de9d22-da6f-5c83-974b-c7c115a49709` | `re-zero-s02e14-sentence-071` |
| 14 | `4b4cc34a-4d87-5094-8863-2ab1e20965c2` | `re-zero-s02e14-sentence-072` |
| 15 | `07d7ad8c-66cb-5b4d-bb51-9f52bfe50cbe` | `re-zero-s02e14-sentence-073` |
| 16 | `1ad3a149-e9eb-551e-a8f3-3ed4360f70e8` | `re-zero-s02e14-sentence-075` |
| 17 | `0a49797b-6ad3-5ec3-b184-07c3e6d7c326` | `re-zero-s02e14-sentence-076` |
| 18 | `28d6b1f8-b138-53bd-b264-92984b69ca31` | `re-zero-s02e14-sentence-077` |
| 19 | `4e7310b2-0dff-5686-ad94-7012d6ae9673` | `re-zero-s02e14-sentence-078` |
| 20 | `25aaadf5-b6a0-51ee-b44c-983496f341a6` | `re-zero-s02e14-sentence-079` |
| 21 | `925244e8-b36c-5a26-808c-525089758149` | `re-zero-s02e14-sentence-080` |
| 22 | `16799282-733f-5e04-ac7f-875d66a5c31a` | `re-zero-s02e14-sentence-081` |
| 23 | `a53ca9bb-3ab1-5f2a-a42f-071d5e7276b8` | `re-zero-s02e14-sentence-082` |
| 24 | `9b7eba85-3eba-583e-862b-f2b895a78ac1` | `re-zero-s02e14-sentence-084` |
| 25 | `a9570f33-c9a4-55af-bc63-993c55daa907` | `re-zero-s02e14-sentence-085` |
| 26 | `22845df6-bbb1-5ded-a233-1a30325b68c3` | EP39 subtitle line 151 |
| 27 | `2a343229-0d68-5c03-a14b-89763df7d5d4` | `re-zero-s02e14-sentence-088` |
| 28 | `bf931597-fd18-5a2a-b7d6-a498ea50f3f5` | `re-zero-s02e14-sentence-091` |
| 29 | `a22db27e-4b9b-54cc-951e-a7fdcbee9806` | `re-zero-s02e14-sentence-092` |
| 30 | `5a51809e-bd89-5b8f-b635-b71131624d10` | `re-zero-s02e14-sentence-093` |
| 31 | `698e238b-4aa2-5fb6-8993-6ffcc074bda7` | `re-zero-s02e14-sentence-094` |
| 32 | `8388953b-ff0d-5ea6-9129-81d8b084b7b4` | `re-zero-s02e14-sentence-095` |
| 33 | `12957316-5373-5a6d-bb45-7cf321ddb2c2` | `re-zero-s02e14-sentence-096` |
| 34 | `4f8371da-4f9d-5e77-9a00-8be496c778fd` | `re-zero-s02e14-sentence-097` |
| 35 | `7eb8deab-a5b5-525d-900b-1583833eaa3f` | `re-zero-s02e14-sentence-098` |
| 36 | `989586f9-fdf5-5f98-94ba-df985e605d74` | `re-zero-s02e14-sentence-099` |
| 37 | `5aedcc87-7a03-5d19-8271-2c0dd8dd357e` | `re-zero-s02e14-sentence-100` |
| 38 | `aa3c9dcf-e3ad-55b1-9f5c-1e603910e851` | `re-zero-s02e14-sentence-101` |
| 39 | `db5a7f1e-fd7d-5773-9d55-88554170b8c0` | `re-zero-s02e14-sentence-102` |
| 40 | `24272166-d25a-596e-b5fc-65180aaf3666` | `re-zero-s02e14-sentence-104` |
| 41 | `5b44894c-07f1-5a52-8975-530ac1cef38d` | `re-zero-s02e14-sentence-106` |
| 42 | `85f6cfaf-f22d-5372-b75d-756eea3db6c7` | `re-zero-s02e14-sentence-107` |
| 43 | `7d00c1c8-76f0-5b94-b316-55eb5d95beb4` | `re-zero-s02e14-sentence-108` |
| 44 | `9c05d3a7-991c-5895-8de9-b52617cdcf3c` | `re-zero-s02e14-sentence-109` |
| 45 | `f3ba144c-69bd-5c8f-9eb3-31236c3b9183` | `re-zero-s02e14-sentence-110` |
| 46 | `cd50e382-5910-5dcd-b14e-33cf8628b8c6` | `re-zero-s02e14-sentence-111` |
| 47 | `18dfa555-7430-5224-a062-51a5f901bd9b` | `re-zero-s02e14-sentence-112` |
| 48 | `3508a79f-d058-5e00-b863-3ef15e7e4a14` | `re-zero-s02e14-sentence-113` |
| 49 | `d4ac6836-2893-5916-bf09-da167e6b5522` | `re-zero-s02e14-sentence-114` |
| 50 | `8827b413-9817-57b6-b5a6-76ee1fe86281` | `re-zero-s02e14-sentence-115` |
| 51 | `c1238e0d-4121-516f-ba4a-f2a7709646cf` | `re-zero-s02e14-sentence-116` |
| 52 | `2f0c72ab-9aff-5de1-afbf-bcc82913912a` | `re-zero-s02e14-sentence-117` |
| 53 | `8bf43278-22ab-5481-8a5a-bd0a6434e278` | `re-zero-s02e14-sentence-118` |
| 54 | `d0a710d7-5639-55e1-807a-500b14a5acbc` | `re-zero-s02e14-sentence-119` |
| 55 | `a1bd541a-ba41-5186-84eb-601f9ff7370f` | `re-zero-s02e14-sentence-120` |
| 56 | `d23b8fd3-29f7-5e38-b31e-d79da6546d6c` | `re-zero-s02e14-sentence-121` |
| 57 | `42e866ba-9fff-5e8d-8df1-14acb62b5eb1` | `re-zero-s02e14-sentence-122` |
| 58 | `6b2aee89-ffea-5b23-b6f4-ed9612d74bc4` | `re-zero-s02e14-sentence-123` |
| 59 | `57a869f4-de92-52ba-be2d-00fddefd07cd` | `re-zero-s02e14-sentence-124` |
| 60 | `90f22a09-4a45-562e-81eb-8a40be830766` | `re-zero-s02e14-sentence-125` |
| 61 | `17e086db-05d6-572b-8a07-78b9a028768a` | `re-zero-s02e14-sentence-126` |
| 62 | `53111518-e619-5161-935f-60825c30d4c6` | `re-zero-s02e14-sentence-127` |
| 63 | `34231764-2634-5e12-84e0-f6712220c2e8` | `re-zero-s02e14-sentence-128` |
| 64 | `995a8855-7153-58c7-ba47-10fcd8c262d2` | `re-zero-s02e14-sentence-129` |
| 65 | `2f32e266-943d-5c1a-9931-0e950216b35a` | `re-zero-s02e14-sentence-130` |
| 66 | `c3e848fc-a920-543c-b933-6e007d0b19f4` | `re-zero-s02e14-sentence-131` |
| 67 | `fece3b36-6758-5bc3-b45b-7b5e24aafe4e` | `re-zero-s02e14-sentence-132` |
| 68 | `d49dc0e5-0dad-5671-bb04-0549be3c2939` | `re-zero-s02e14-sentence-133` |
| 69 | `57a717fa-218d-56a4-8244-314c1dfff45f` | `re-zero-s02e14-sentence-134` |
| 70 | `8d71d809-e3c7-53e9-953b-aaaf7160d510` | `re-zero-s02e14-sentence-135` |
| 71 | `7d5355f7-f5ca-5122-84f7-8c7841dd2638` | `re-zero-s02e14-sentence-136` |
| 72 | `8b6e57bf-89a3-5911-b6e7-a11c5b4d3663` | `re-zero-s02e14-sentence-137` |
| 73 | `0cbe4fb4-f80b-5081-bc02-5c04d7cf8ac5` | `re-zero-s02e14-sentence-138` |
| 74 | `76f89166-3fd3-528a-87f5-20a002b27a4f` | `re-zero-s02e14-sentence-139` |
| 75 | `d698bf20-8fe4-5f64-bf1e-2b4aadaecd31` | `re-zero-s02e14-sentence-140` |
| 76 | `3a9f35b5-5238-5ecf-ad47-81834c341295` | `re-zero-s02e14-sentence-141` |
| 77 | `34e47143-1589-5321-8849-fc8fccb30031` | `re-zero-s02e14-sentence-142` |
| 78 | `76a1537d-5133-5fbd-ba01-3c3ee320f91e` | `re-zero-s02e14-sentence-143` |
| 79 | `2cd780b6-1d55-565b-9b37-8f8125876ff8` | `re-zero-s02e14-sentence-144` |
| 80 | `79453985-1fe0-58aa-ac0a-f6c8f985a15a` | `re-zero-s02e14-sentence-145` |

辅助 grammar：

- `f9fc1a9b-8593-5332-b363-2b2511da42ba`：`〜させたらどうじゃ`
- `23362b9e-495f-5a77-9c37-960a8c079455`：`〜ないと分からない`

## 字幕与内容检查

- `learning_sentences.source_line_no` 与真实字幕在本段多处偏移约 2–4 行；复核时以日文字幕内容匹配为准，review note 使用真实字幕行号。
- `sentence-060`、`097`、`098` 等学习句中文发生跨行错接，题目没有沿用错位中文。
- `sentence-080` 合并奥托和昴两位说话人的发言，题面已明确拆分双方功能。
- EP39 lines 236–237 是 EP32 lines 264–265 的回忆重现；最终题面已明确说话人为席玛。
- `sentence-130` 的「リューズさんに」存在切分或助词歧义，答案只解释可确定的「つもりはなかった」，没有强指省略主语。
- 所有跨行结构均恢复必要前件或后件，没有把推测内容写成原字幕明说的事实。
- 题面均可脱离数据库字段独立理解；答案回应所问语言点；hint 只给分析路径，不直接给出结论。

## 最终校验

执行：

```text
node regen\validate_candidates.mjs regen\batch2_meta_exercises.json
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
- learner-facing 字段中的内部 ID、模板字样、答案元信息：0。
- 重复 prompt：0；重复 hint：0；完整答案泄漏：0。
- answer 与 hint 有 3 处最长连续公共片段为 8 字，经人工确认均只是题目本身要求分析的日语形式（`何かしておく`、`笑っていいか`、`かっこをつけた…`），不含分析结论。
- 数据库写入：0。
