# 基础语言学 V1 Wave 3：生成阶段 QA

- 生成文件：`archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave3_generation.json`
- 题包：`foundation-v1-wave-3`
- 生成者：`/root/foundation_api_v1`
- 日期：2026-07-27
- 原始文件 SHA-256：`23122d0c5339a2670bdbfc782603bbe2fa7fdafc014ea62eaaa809656628b881`
- 状态：生成阶段冻结，等待不同 Agent 完成 80 / 80 独立复核
- 数据库：未写入

## 生成约束

1. 80 道题均由生成者逐条人工原创，没有使用 Python、JavaScript、SQL 或文本模板拼接教学内容。
2. 每个主题恰有 F1 识别、F2 解析、F3 对比诊断、F4 迁移应用各一题。
3. 全部句子、最小对照、元语言材料和对话均为中性构造材料，不含作品、集数、角色、字幕行或来源 ID。
4. 每题有四个同层级选项、一个可辩护的正确答案，以及三个针对本题材料的错误项说明。
5. 对话题标明必要说话人；指示、照应、省略、敬语、视点、会话序列与社会索引题均核对跨话轮语境。
6. 逐题按适用范围检查了形态、句法、语义、语用、说话人、指称、否定、时制、体、语态与时代／地域边界。
7. 历史题只使用已明确标注的教材级事实或明示为教学假设的抽象规则，不把假设材料伪装成真实词源或年代。

## 数量与配额

| 检查项 | 结果 |
|---|---:|
| 题目数 | 80 |
| 唯一 ID | 80 |
| 唯一主题 | 20 |
| 每主题 F1–F4 | 20 × 4 |
| F1 / F2 / F3 / F4 | 20 / 20 / 20 / 20 |
| 正确答案 A / B / C / D | 20 / 20 / 20 / 20 |
| Validator 内容警告 | 0 |

## 逐题生成自检

本表是生成者逐题自检，不能替代不同 Agent 的独立复核。

| # | question_id | 答案 | 逐题核对结论 |
|---:|---|:---:|---|
| 01 | `foundation-v1-prag-speech-acts-f1` | A | 请求形式、关窗宾语与职员随即执行的下一话轮一致，排除纯能力询问、承诺和完成报告。 |
| 02 | `foundation-v1-prag-speech-acts-f2` | B | 移桌理由及同僚行动回应共同支持间接请求；「少し」不改变时制，入口也不是施事。 |
| 03 | `foundation-v1-prag-speech-acts-f3` | C | 相同可能形式分别由预约时点与即时行动序列约束为出席确认和当场请求，不以年龄作无证据推断。 |
| 04 | `foundation-v1-prag-speech-acts-f4` | D | 高负担延期请求同时给出迟到原因、新期限和疑问缓和，未擅自决定，也保留上司拒绝空间。 |
| 05 | `foundation-v1-prag-deixis-f1` | B | 无叙事投射时「ここ」以说话人所在的一楼前台为中心，电话另一端不自动成为指示原点。 |
| 06 | `foundation-v1-prag-deixis-f2` | C | 发话日为周一故「明日」是周二；「ここ」锚定发信者当时所在的仙台事务所。 |
| 07 | `foundation-v1-prag-deixis-f3` | D | 间接转述改用明确地名保持京都指称；只有保留直接引语框架时原发话「ここ」才能原样出现。 |
| 08 | `foundation-v1-prag-deixis-f4` | A | 发信地点已移至咖啡店，明确说「さっきいた図書館」避免把「ここ」误解为当前地点，并保持过去遗忘事件。 |
| 09 | `foundation-v1-prag-reference-anaphora-f1` | C | 第二句重提并持续显著化箱子，使「それ」优先回指红箱；袋子、桌子和架子均缺少同等话语连续性。 |
| 10 | `foundation-v1-prag-reference-anaphora-f2` | D | 研究员与助手都满足有生性和行为能力，主语显著性与线性邻近相互竞争，现有材料不足以唯一消歧。 |
| 11 | `foundation-v1-prag-reference-anaphora-f3` | A | 「その家」与先行项共指；「玄関」依赖房屋—玄关的部分关系作桥接，两者不是同一种直接照应。 |
| 12 | `foundation-v1-prag-reference-anaphora-f4` | B | 重述「助手」并以「その記録」保持对象，明确谁确认什么；代词和零主语仍保留人物歧义。 |
| 13 | `foundation-v1-prag-information-focus-f1` | D | 「誰」建立人物替代集合，「木村さんが」填补该槽；名词身份和句末位置本身不决定焦点。 |
| 14 | `foundation-v1-prag-information-focus-f2` | A | 前问已给小林与修理事件，回答以「プリンターを」填补对象槽；格标记与焦点解释分层处理。 |
| 15 | `foundation-v1-prag-information-focus-f3` | B | 同一表面回答可因当前问题而有不同焦点适配；问答二需要语调或改写突出“会议”，不存在时态对立。 |
| 16 | `foundation-v1-prag-information-focus-f4` | C | 分裂句把森设为人物更正焦点，同时「提出していない」保持提交事件上的非过去否定作用域。 |
| 17 | `foundation-v1-prag-ellipsis-fragments-f1` | A | 「三時から」继承会议主题和名词谓语框架，保留起点「から」，不改成施事、终点或否定。 |
| 18 | `foundation-v1-prag-ellipsis-fragments-f2` | B | 回答只明示人物，关窗谓词、窗户宾语与过去时由完整问题恢复，不补入拒绝态度或未来时间。 |
| 19 | `foundation-v1-prag-ellipsis-fragments-f3` | C | 两处「駅前で」均为地点成分；不同当前问题分别提供“会见”和“买票”的谓词及论元结构。 |
| 20 | `foundation-v1-prag-ellipsis-fragments-f4` | D | 「赤い箱を」唯一填补二选一宾语槽，格助词与省略的「倉庫へ運ぶ」对齐，未改变肯否与时制。 |
| 21 | `foundation-v1-prag-sentence-final-particles-f1` | B | 双方共同看见雨势变化，「ね」组织共同确认而不改变「降ってきた」的时体或引入引用。 |
| 22 | `foundation-v1-prag-sentence-final-particles-f2` | C | 来访者请求地点，引导员用「よ」定向提供其尚不掌握的二楼信息；回答不是疑问或第三方转述。 |
| 23 | `foundation-v1-prag-sentence-final-particles-f3` | D | 只有引导员知道路况且目标是提醒，四项中「よ」最直接编码非共享信息向驾驶者的更新。 |
| 24 | `foundation-v1-prag-sentence-final-particles-f4` | A | 双方共同读取明确日程，「ね」适合共同确认；「よ」、传闻式和不确定式分别误设信息方向或证据状态。 |
| 25 | `foundation-v1-prag-politeness-honorifics-f1` | C | 「いらっしゃる」替换实义「いる」抬高部长这一主语，「ます」另行提供丁宁性，不含使役被动。 |
| 26 | `foundation-v1-prag-politeness-honorifics-f2` | D | 「私が」明示职员为搬运者，「お持ちする」谦逊表达己方服务，资料宾语从顾客请求中恢复。 |
| 27 | `foundation-v1-prag-politeness-honorifics-f3` | A | 两句主体均为部长；对内尊敬、对外把己方纳入内方后用「伺う」谦逊，变化不来自主语消失。 |
| 28 | `foundation-v1-prag-politeness-honorifics-f4` | B | 店员以「確認いたします」表达己方行动，再礼貌请求顾客等候；其他项或颠倒服务角色，或语体过于直接。 |
| 29 | `foundation-v1-prag-register-style-f1` | D | 公共设施范围框架与「ご遠慮ください」共同构成面向不特定读者的制度化礼貌告示。 |
| 30 | `foundation-v1-prag-register-style-f2` | A | 两句保留到达后联系的命题，分别用口语条件句与机构式名词化、礼貌请求适配受众和媒介。 |
| 31 | `foundation-v1-prag-register-style-f3` | B | 正式行政文本从「してください」无动机切到亲密「だよ」，问题在社会关系索引而非时制或论元。 |
| 32 | `foundation-v1-prag-register-style-f4` | C | 两句均正式化并保留明日前提交与避免遗漏的要求；强硬命令、半套改写和过去事实都不合目标。 |
| 33 | `foundation-v1-prag-turn-taking-backchannels-f1` | A | 「はい」位于未完的「次に—」之后且原说话人随即继续，序列位置支持接收反馈而非承诺答复。 |
| 34 | `foundation-v1-prag-turn-taking-backchannels-f2` | B | 完整是非问句后「はい、保存します」重述谓词，明确落实肯定选择与非过去行动决定。 |
| 35 | `foundation-v1-prag-turn-taking-backchannels-f3` | C | 同一「そうですか」在未完叙述中支持继续，在完整通知后关闭接收并导向行动，差异来自序列环境。 |
| 36 | `foundation-v1-prag-turn-taking-backchannels-f4` | D | 接续形投射后续阶段，最小「はい」显示跟随且不夺话轮；结案、反驳和宣告完成均破坏序列。 |
| 37 | `foundation-v1-prag-connectives-coherence-f1` | B | 「そのため」回指雨势增强并引出停工结果；逆接、举例和换言均不符合给定因果。 |
| 38 | `foundation-v1-prag-connectives-coherence-f2` | C | 「そこで」承接入口关闭的具体情势并引出主体决定走南门，适配问题—应对结构。 |
| 39 | `foundation-v1-prag-connectives-coherence-f3` | D | 测试成功建立可公开预期，手册未完成却导致延期；「しかし」标记推论层面的预期逆转而非逻辑矛盾。 |
| 40 | `foundation-v1-prag-connectives-coherence-f4` | A | 「そこで」引出补纸行动，「その結果」引出恢复打印，分别编码情势—应对与行动—结果。 |
| 41 | `foundation-v1-prag-viewpoint-empathy-f1` | C | 「同僚が／私に」保留发送与接收角色，「送ってくれた」增加朝说话人一方的受益视点和过去定位。 |
| 42 | `foundation-v1-prag-viewpoint-empathy-f2` | D | 主句主题「私」取得帮助，「先輩に」标示嵌入教导动作的施事；不存在使役、被动或角色倒转。 |
| 43 | `foundation-v1-prag-viewpoint-empathy-f3` | A | 两句可指同一发送事件，但「くれる」与「もらう」分别以给予行动和受益取得组织句法中心。 |
| 44 | `foundation-v1-prag-viewpoint-empathy-f4` | B | 「私は同僚に…てもらった」同时保持同事为组装者、说话人为受益者与过去完成，不添加强迫。 |
| 45 | `foundation-v1-soc-standard-regional-variation-f1` | D | 在明确限定的关西部分会话变体中，名词谓语后「や」对应共通语断定「だ」，不编码格、否定或过去。 |
| 46 | `foundation-v1-soc-standard-regional-variation-f2` | A | 「ない／へん」在各自变体中系统表达非过去否定；标准化地位不能转化为结构优劣判断。 |
| 47 | `foundation-v1-soc-standard-regional-variation-f3` | B | 同一人按家庭与全国发言场合切换形式，支持语体适配，却不足以从单例断定出生地或性格。 |
| 48 | `foundation-v1-soc-standard-regional-variation-f4` | C | 「行かないんだ」同时保留非过去、否定和说明性收束；其他项改变极性、时态或言语行为。 |
| 49 | `foundation-v1-soc-gender-age-indexing-f1` | A | 「僕／ね」可参与风格化自我呈现，但不在形态语义上蕴含唯一性别或年龄，主语仍是说话人。 |
| 50 | `foundation-v1-soc-gender-age-indexing-f2` | B | 题面控制同一说话人，两句都为非过去肯定；自称、词汇与礼貌体随公开／亲近场景切换。 |
| 51 | `foundation-v1-soc-gender-age-indexing-f3` | C | 脚本可调用社会风格线索，却不能由单句推出配音者或现实使用者的排他性人口身份。 |
| 52 | `foundation-v1-soc-gender-age-indexing-f4` | D | 「私は知りません」保留第一人称当前否定，去除「俺／知らねえ／ぞ」的强势索引并采用较中性礼貌体。 |
| 53 | `foundation-v1-soc-style-shifting-community-f1` | B | 同一人以「本日／ご説明します」和「きょう／段取り／しよう」适配公众与团队，命题领域保持一致。 |
| 54 | `foundation-v1-soc-style-shifting-community-f2` | C | 团队重复实践赋予「二番」局部指称并许可宾语省略；「回そう」是意向形，不是过去否定。 |
| 55 | `foundation-v1-soc-style-shifting-community-f3` | D | 参与经历与内部简称习得是直接证据，支持成员化风格适应；年龄、性别和出生地均未被观测。 |
| 56 | `foundation-v1-soc-style-shifting-community-f4` | A | 对外制度通知与对内行动协调都维持当天停工，语体差异由受众设计而非身份变化或命题冲突解释。 |
| 57 | `foundation-v1-soc-uchi-soto-power-f1` | C | 「弊社」把本公司标作内方，「担当者が」仍为来访施事，「伺う」谦逊组织己方面向交易方的行动。 |
| 58 | `foundation-v1-soc-uchi-soto-power-f2` | D | 对外不抬高己方部长，以「弊社／ご連絡いたします」保持己方主语与正确联系方向。 |
| 59 | `foundation-v1-soc-uchi-soto-power-f3` | A | 内部职级轴支持尊敬部长，对外内外轴支持谦逊己方部长；两句主语均未改变。 |
| 60 | `foundation-v1-soc-uchi-soto-power-f4` | B | 回答保持己方部长、明日、肯定联系买方四项事实，并以谦让形式实现外部待遇方向。 |
| 61 | `foundation-v1-soc-contact-borrowing-f1` | D | 「ストライク」展示辅音连缀经元音配置等手段进入日语拍序列，不涉及动词活用或纯字体替换。 |
| 62 | `foundation-v1-soc-contact-borrowing-f2` | A | 「スマホ」是借入后在日语内部进一步缩略并词汇化的形式，缩略不编码被动、过去或否定。 |
| 63 | `foundation-v1-soc-contact-borrowing-f3` | B | 日语「マンション」的常用住宅义与英语来源词发生分化，音形来源不能保证同步语义一一对应。 |
| 64 | `foundation-v1-soc-contact-borrowing-f4` | C | 虚构 KLEN 的「クレン（仮）」只作为音系可行候选，明确区分临时转写与共同体已经确立的借词。 |
| 65 | `foundation-v1-hist-classical-modern-grammar-f1` | A | 固定构式中「知ら＋ず」保留古典否定层次；同步整体化不等于现代使用者每次生成完整古典范式。 |
| 66 | `foundation-v1-hist-classical-modern-grammar-f2` | B | 教材标注的古典ク活用连体形「高き」与现代定语形「高い」构成范式对照，命题均为肯定属性。 |
| 67 | `foundation-v1-hist-classical-modern-grammar-f3` | C | 「いざ知らず」高度固定，而「Vずに」仍有可替换动词槽；两者共享历史层次但同步能产性不同。 |
| 68 | `foundation-v1-hist-classical-modern-grammar-f4` | D | 「花が咲いた」保持花开、肯定与过去／已察觉的最小命题，明确说明无法完整保存「けり」的发现或感叹色彩。 |
| 69 | `foundation-v1-hist-sound-change-orthography-f1` | B | 「けふ／きょう」显示历史拼写与现代音形不透明，旧写法不能逐假名套用现代独立音值。 |
| 70 | `foundation-v1-hist-sound-change-orthography-f2` | C | 严格沿题面链条解释 /teɸu/→/teu/ 的辅音变化及后续 /tɕoː/ 合流，并限定中央方言系范围。 |
| 71 | `foundation-v1-hist-sound-change-orthography-f3` | D | 分期资料与重复音对应可检验；单个现代听感相似不足以排除偶然、借用或其他来源。 |
| 72 | `foundation-v1-hist-sound-change-orthography-f4` | A | 仿真输入满足 V_V 环境，单步 /ɸ/→/w/ 得 /awa/；题面明示不把结果当真实词源或跨地域事实。 |
| 73 | `foundation-v1-hist-grammaticalization-auxiliaries-f1` | C | 独立「いる」支配存在地点，辅助「寝ている」与主动作形成睡眠持续体貌；两句均为肯定非过去。 |
| 74 | `foundation-v1-hist-grammaticalization-auxiliaries-f2` | D | て形固定连接、抽象体貌贡献与地点论元独立性降低共同支持辅助性，句长和有生主语不构成诊断。 |
| 75 | `foundation-v1-hist-grammaticalization-auxiliaries-f3` | A | 同期可共存实义存在和辅助体貌用法，须按格结构与连接区分；同步差异不否认可能的历时联系。 |
| 76 | `foundation-v1-hist-grammaticalization-auxiliaries-f4` | B | 相对系列与结果状态向体貌扩展、音形缩减的假说相容，但未虚构年份、唯一因果或全地域同步。 |
| 77 | `foundation-v1-hist-lexicalization-construction-change-f1` | D | 「手を焼く」在机器语境中整体表示难以应付，意义特殊化且内部替换受限；仍不宣称内部结构全失。 |
| 78 | `foundation-v1-hist-lexicalization-construction-change-f2` | A | 「目玉焼き」具有稳定食品义并保留可辨成分，恰好展示整体约定化与部分内部透明并存。 |
| 79 | `foundation-v1-hist-lexicalization-construction-change-f3` | B | 特定词组「手を焼く」侧重词汇义特殊化，开放「Vてしまう」侧重抽象辅助功能与能产槽位。 |
| 80 | `foundation-v1-hist-lexicalization-construction-change-f4` | C | 抽象系列支持“项目词汇化后经类推扩展构式”的最小假说，同时列出借用、独立创新和材料偏差等替代解释。 |

## Validator 结果

运行：

```text
node scripts/validate-foundation-content.mjs pack archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave3_generation.json
```

结构与内容结果：

- `rows`: 80
- `uniqueIds`: 80
- `topicCount`: 20
- `answerCounts`: A=20、B=20、C=20、D=20
- `warnings`: 0
- generation canonical SHA-256：`01615a40eb995724cc8d61332a284aba1c48c05cc66e7dc043e8bfc7be187bf3`

Validator 当前仅报告两类预期的生成阶段审计缺口：

1. 80 / 80 尚无 `reviewer_agent`；
2. 80 / 80 尚无独立复核后的 `quality_score`。

这两项必须由不同 Agent 的 review overlay 补齐；在此之前本题包不得发布或写入数据库。
