# 基础日语语言学题库 V1 — Wave 3 独立交叉复核

## 审核身份与冻结输入

- 生成 Agent：`/root/foundation_api_v1`
- 独立复核 Agent：`/root/foundation_schema_v1`
- 生成文件：`foundation/questions_wave3_generation.json`
- 生成文件 SHA-256：`23122d0c5339a2670bdbfc782603bbe2fa7fdafc014ea62eaaa809656628b881`
- 生成内容规范化 SHA-256：`01615a40eb995724cc8d61332a284aba1c48c05cc66e7dc043e8bfc7be187bf3`
- 复核 overlay：`foundation/questions_wave3_review_overlay.json`
- 复核 overlay SHA-256：`bbfedad8b139f4031a45cff9d3ab8661e8611f40eecd59d41152508791728176`
- overlay 生效后规范化 SHA-256：`02d6ce0d99c169bde95858b61d24fdbb2ba762d2b57173095ad4d94922a10039`

本次逐条复核 80 题。复核者与生成者不同；每题均重新检查日语形态、句法、语义、语用、说话人和动作角色、指代、否定、时态、语态，以及题面内部的跨话轮或跨材料上下文。复核不把形式标签当作充分解释，并专门检查了社会语言学断言是否过度本质化、历史语言学断言是否越过题面证据。

## 结果

| 项目 | 数量 |
|---|---:|
| 基础题目 | 80 |
| 唯一 ID | 80 |
| 覆盖 topic | 20 |
| F1 / F2 / F3 / F4 | 20 / 20 / 20 / 20 |
| keep | 67 |
| revise | 13 |
| reject | 0 |
| 完整同 ID replacements | 13 |
| 最低单题质量分 | 97 |
| A / B / C / D 正确答案 | 20 / 20 / 20 / 20 |
| validator errors / warnings | 0 / 0 |

13 个 `revise` 均已在 overlay 中以完整同 ID 对象替换，未追加新题、未改变题目总数。所有原先需要修复的问题均已在交付前消解，因此最终无 `reject`。

## 关键修订

1. `prag-speech-acts-f2`：明确「入口を」是「広くする」的变化对象，避免把后句论元误写成移动事件主语。
2. `prag-speech-acts-f4`：把出题说明从角色话轮移出，改成自然的延期协商；同时锁定原期限周二和新期限周三。
3. `prag-deixis-f2`：将周一、仙台和同步电话写入语境，角色只说自然话轮，使「明日／ここ」共享同一发话中心。
4. `prag-deixis-f4`：改为元语言迁移题，排除角色在会话里直接朗读任务说明。
5. `prag-ellipsis-fragments-f4`：改为元语言问答材料，保留「赤い箱を」对宾语槽和格关系的最小恢复。
6. `prag-sentence-final-particles-f4`：改成两人共同查看日程表的真实会话，使「ね」的共享确认条件由上下文而非说明性假话轮承担。
7. `prag-politeness-honorifics-f4`：改成自然店员受理序列，并依据《敬語の指針》把「確認いたします」中的「いたす」精确处理为谦让语Ⅱ／丁重语。
8. `prag-turn-taking-backchannels-f3`：把第一组改为能投射后续的未完叙述，使中途「そうですか」作为接收反馈更自然。
9. `prag-turn-taking-backchannels-f4`：保留未完接续位置，但把听者的教学目标移入元语言描述。
10. `soc-style-shifting-community-f2`：把团队局部规范写入共同经历语境，新人只作自然接收回应。
11. `soc-uchi-soto-power-f2`：把“己方部长稍后联系”的角色自我说明移入元语言场景，保持动作方向清楚。
12. `soc-uchi-soto-power-f4`：把已确认事实移入题面描述，保留买方真实问句并锁定明日、肯定和己方部长主语。
13. `soc-contact-borrowing-f1`：将不规范的“元音配置”改为“元音插入与音段适配”，明确「ス・ト・ラ・イ・ク」五拍。

## 80 题逐条复核台账

| # | ID | 判定 | 分数 | 独立复核结论 |
|---:|---|---|---:|---|
| 1 | `foundation-v1-prag-speech-acts-f1` | keep | 98 | 字面能力问与关窗请求由下一话轮消歧，仍保留真实能力调查边界。 |
| 2 | `foundation-v1-prag-speech-acts-f2` | revise | 98 | 行为力量成立；已修正「入口を」的句法角色说明。 |
| 3 | `foundation-v1-prag-speech-acts-f3` | keep | 98 | 同形可能疑问随预约和现场序列分别实现确认与请求。 |
| 4 | `foundation-v1-prag-speech-acts-f4` | revise | 98 | 已重写为自然延期协商并固定周二到周三的时间关系。 |
| 5 | `foundation-v1-prag-deixis-f1` | keep | 98 | 电话发话地点与「ここ」一致，空间中心清楚。 |
| 6 | `foundation-v1-prag-deixis-f2` | revise | 98 | 已把周一、仙台、同步通话写入语境，时空锚点唯一。 |
| 7 | `foundation-v1-prag-deixis-f3` | keep | 97 | 普通间接转述改用地点名可避免中心错配，同时承认直接引用例外。 |
| 8 | `foundation-v1-prag-deixis-f4` | revise | 98 | 已用元语言场景重写移动后的旧地点指称。 |
| 9 | `foundation-v1-prag-reference-anaphora-f1` | keep | 98 | 红箱先行语由话题连续性和贴标签事件共同支持。 |
| 10 | `foundation-v1-prag-reference-anaphora-f2` | keep | 99 | 主语显著性与线性邻近形成真实竞争，保留歧义最稳妥。 |
| 11 | `foundation-v1-prag-reference-anaphora-f3` | keep | 98 | 同指与部分—整体桥接区分准确，未无限放宽桥接。 |
| 12 | `foundation-v1-prag-reference-anaphora-f4` | keep | 98 | 明示助手和「その記録」同时稳定人物与受事指称。 |
| 13 | `foundation-v1-prag-information-focus-f1` | keep | 98 | 「誰」建立替代集合，「木村さんが」填补人物焦点槽。 |
| 14 | `foundation-v1-prag-information-focus-f2` | keep | 98 | 「何を」问答锁定对象焦点，格角色和信息结构未混淆。 |
| 15 | `foundation-v1-prag-information-focus-f3` | keep | 97 | 同一回答随当前问题改变适切度，未臆造书面材料的韵律。 |
| 16 | `foundation-v1-prag-information-focus-f4` | keep | 98 | 分裂句更正人物，否定仍作用于提交事件。 |
| 17 | `foundation-v1-prag-ellipsis-fragments-f1` | keep | 98 | 「三時から」只填时间起点，其他命题由问题最小恢复。 |
| 18 | `foundation-v1-prag-ellipsis-fragments-f2` | keep | 98 | 人物片段填补「誰」槽，过去事件及受事由问题继承。 |
| 19 | `foundation-v1-prag-ellipsis-fragments-f3` | keep | 98 | 同一地点片段随前问恢复不同事件，格与时态均一致。 |
| 20 | `foundation-v1-prag-ellipsis-fragments-f4` | revise | 98 | 已移除教学目标假话轮，保留宾语片段与格恢复。 |
| 21 | `foundation-v1-prag-sentence-final-particles-f1` | keep | 98 | 共享视觉证据支持「ね」的共同确认，命题与立场分层清楚。 |
| 22 | `foundation-v1-prag-sentence-final-particles-f2` | keep | 98 | 知识不对称支持「よ」的定向告知，不把形式固化为责备。 |
| 23 | `foundation-v1-prag-sentence-final-particles-f3` | keep | 97 | 提醒目标支持「よ」，同时承认无终助词陈述也可告知。 |
| 24 | `foundation-v1-prag-sentence-final-particles-f4` | revise | 98 | 已改成共同看表的自然对话，使共享证据条件显式。 |
| 25 | `foundation-v1-prag-politeness-honorifics-f1` | keep | 98 | 「いらっしゃる」尊敬部长主语，「ます」承担听者礼貌。 |
| 26 | `foundation-v1-prag-politeness-honorifics-f2` | keep | 98 | 「私が」锁定施事，「お持ちする」的谦让语Ⅰ方向正确。 |
| 27 | `foundation-v1-prag-politeness-honorifics-f3` | keep | 99 | 对内和对外的内外重排准确，部长主语始终不变。 |
| 28 | `foundation-v1-prag-politeness-honorifics-f4` | revise | 99 | 已改为真实服务序列，并把「いたす」定为谦让语Ⅱ／丁重语。 |
| 29 | `foundation-v1-prag-register-style-f1` | keep | 98 | 公共设施和不特定受众支持制度化礼貌告示语体。 |
| 30 | `foundation-v1-prag-register-style-f2` | keep | 98 | 朋友消息与设施通知保留命题，按媒介整体切换语体。 |
| 31 | `foundation-v1-prag-register-style-f3` | keep | 98 | 正式行政文本中的无动机亲密形式诊断成立，并保留引用例外。 |
| 32 | `foundation-v1-prag-register-style-f4` | keep | 98 | 机构式改写保留明日前提交与防遗漏要求，时态未漂移。 |
| 33 | `foundation-v1-prag-turn-taking-backchannels-f1` | keep | 98 | 未完接续、短促「はい」和原说话人续讲共同锁定相槌功能。 |
| 34 | `foundation-v1-prag-turn-taking-backchannels-f2` | keep | 98 | 完整是非问句后的「はい」是肯定答复并导向保存行动。 |
| 35 | `foundation-v1-prag-turn-taking-backchannels-f3` | revise | 98 | 已使第一叙述自然投射后续，序列功能对比更可靠。 |
| 36 | `foundation-v1-prag-turn-taking-backchannels-f4` | revise | 98 | 已改为元语言刺激，保留「て」形未完位置与最小反馈。 |
| 37 | `foundation-v1-prag-connectives-coherence-f1` | keep | 98 | 「そのため」准确编码雨势增强到停工的题设因果。 |
| 38 | `foundation-v1-prag-connectives-coherence-f2` | keep | 98 | 「そこで」组织情势—有意志应对关系，逻辑方向清楚。 |
| 39 | `foundation-v1-prag-connectives-coherence-f3` | keep | 98 | 测试成功与延期并不矛盾，「しかし」标记预期受阻。 |
| 40 | `foundation-v1-prag-connectives-coherence-f4` | keep | 98 | 「そこで／その結果」分别承担应对与结果，事件链完整。 |
| 41 | `foundation-v1-prag-viewpoint-empathy-f1` | keep | 98 | 同事施事、说话人受益与「くれる」视点相容，过去时正确。 |
| 42 | `foundation-v1-prag-viewpoint-empathy-f2` | keep | 98 | 说话人为受益取得者，前辈为教导施事，未加入强迫义。 |
| 43 | `foundation-v1-prag-viewpoint-empathy-f3` | keep | 98 | 「くれる／もらう」只改变主句组织中心，实际角色不变。 |
| 44 | `foundation-v1-prag-viewpoint-empathy-f4` | keep | 98 | 「てもらった」保持同事组装、说话人受益和过去完成。 |
| 45 | `foundation-v1-soc-standard-regional-variation-f1` | keep | 97 | 限定关西部分变体后，「休みや／休みだ」对应审慎成立。 |
| 46 | `foundation-v1-soc-standard-regional-variation-f2` | keep | 99 | 「ない／へん」均可系统表达否定，并保留词汇、音韵和场合制约。 |
| 47 | `foundation-v1-soc-standard-regional-variation-f3` | keep | 98 | 同一人按家庭和全国场合切换，不能反推出生地或人格。 |
| 48 | `foundation-v1-soc-standard-regional-variation-f4` | keep | 97 | 近似改写保留否定、非过去和说明性收束，未宣称全域等价。 |
| 49 | `foundation-v1-soc-gender-age-indexing-f1` | keep | 99 | 「僕／ね」只作概率性风格资源，不据单句判定身份。 |
| 50 | `foundation-v1-soc-gender-age-indexing-f2` | keep | 98 | 同一成年人按场合切换自称和礼貌体，不蕴涵身份更换。 |
| 51 | `foundation-v1-soc-gender-age-indexing-f3` | keep | 98 | 广告角色风格依时代、媒介和表演传统解释，未建排他规则。 |
| 52 | `foundation-v1-soc-gender-age-indexing-f4` | keep | 98 | 改写保留当前否定并弱化强势索引，不把「私」绝对无标化。 |
| 53 | `foundation-v1-soc-style-shifting-community-f1` | keep | 98 | 同一工作人员随公众说明和团队协调切换语体，身份连续。 |
| 54 | `foundation-v1-soc-style-shifting-community-f2` | revise | 98 | 已把团队局部指称写入共同经历，并恢复新人自然回应。 |
| 55 | `foundation-v1-soc-style-shifting-community-f3` | keep | 98 | 持续参与和内部简称习得直接支持实践共同体成员化。 |
| 56 | `foundation-v1-soc-style-shifting-community-f4` | keep | 98 | 对外通知与对内协调保留当天停止的决定及权限边界。 |
| 57 | `foundation-v1-soc-uchi-soto-power-f1` | keep | 98 | 「弊社」和「伺う」保持己方施事与朝外方的谦让方向。 |
| 58 | `foundation-v1-soc-uchi-soto-power-f2` | revise | 98 | 已移除角色教学说明，保持己方部长联系交易方的方向。 |
| 59 | `foundation-v1-soc-uchi-soto-power-f3` | keep | 99 | 内部职级轴与对外内外轴区分准确，主语角色稳定。 |
| 60 | `foundation-v1-soc-uchi-soto-power-f4` | revise | 98 | 已将已确认事实移入描述，肯否、明日和敬语方向一致。 |
| 61 | `foundation-v1-soc-contact-borrowing-f1` | revise | 98 | 已改为“元音插入与音段适配”，并明确五拍切分。 |
| 62 | `foundation-v1-soc-contact-borrowing-f2` | keep | 98 | 「スマートフォン→スマホ」体现日语内部截短与词汇化。 |
| 63 | `foundation-v1-soc-contact-borrowing-f3` | keep | 98 | 「マンション」的同步常用义分化准确，并保留制度差异。 |
| 64 | `foundation-v1-soc-contact-borrowing-f4` | keep | 97 | 仿拟借形仅作为音系候选，没有冒充现实既定借词。 |
| 65 | `foundation-v1-hist-classical-modern-grammar-f1` | keep | 98 | 「知ら＋ず」与固定「いざ知らず」分析准确，未假设在线生成古典范式。 |
| 66 | `foundation-v1-hist-classical-modern-grammar-f2` | keep | 99 | 古典ク活用连体形「高き」与现代「高い」对照正确。 |
| 67 | `foundation-v1-hist-classical-modern-grammar-f3` | keep | 98 | 固定「いざ知らず」与较可扩展「Vずに」按替换性区分。 |
| 68 | `foundation-v1-hist-classical-modern-grammar-f4` | keep | 99 | 「けり」到「た」保留过去核心，同时明确语气不可完全复制。 |
| 69 | `foundation-v1-hist-sound-change-orthography-f1` | keep | 99 | 权威现代假名遣资料支持「けふ→きょう」，未越界重建全链。 |
| 70 | `foundation-v1-hist-sound-change-orthography-f2` | keep | 98 | 「てふ→てう→ちょう」资料依据充分，中央方言限定明确。 |
| 71 | `foundation-v1-hist-sound-change-orthography-f3` | keep | 98 | 要求分期拼写、重复对应和语义路径，且保留借用等反例。 |
| 72 | `foundation-v1-hist-sound-change-orthography-f4` | keep | 98 | 仿真规则只在指定环境执行，并明确不等同真实词源。 |
| 73 | `foundation-v1-hist-grammaticalization-auxiliaries-f1` | keep | 98 | 独立存在动词与体貌辅助结构在连接和论元上区分清楚。 |
| 74 | `foundation-v1-hist-grammaticalization-auxiliaries-f2` | keep | 98 | 固定连接、抽象体貌和地点论元弱化共同支持辅助性。 |
| 75 | `foundation-v1-hist-grammaticalization-auxiliaries-f3` | keep | 98 | 同期实义与辅助用法可共存，并可由格结构和连接诊断。 |
| 76 | `foundation-v1-hist-grammaticalization-auxiliaries-f4` | keep | 99 | 资料支持存在动词「いる」的辅助动词化方向，假说保持最小。 |
| 77 | `foundation-v1-hist-lexicalization-construction-change-f1` | keep | 98 | 「手を焼く」整体特殊义与有限替换支持渐变词汇化。 |
| 78 | `foundation-v1-hist-lexicalization-construction-change-f2` | keep | 98 | 「目玉焼き」兼有约定义、部分透明性和替换限制。 |
| 79 | `foundation-v1-hist-lexicalization-construction-change-f3` | keep | 98 | 项目词汇化与开放「Vてしまう」辅助构式的对照准确。 |
| 80 | `foundation-v1-hist-lexicalization-construction-change-f4` | keep | 98 | 分期材料只支持可检验最小假说，并保留竞争解释。 |

## 权威材料核对

- 文化审议会国语分科会《敬語の指針》：用于区分谦让语Ⅰ与谦让语Ⅱ／丁重语，并核对「いたす」的听者导向。
- 文化厅《現代仮名遣い》及其历史假名对照：用于核对「けふ／きょう」「てふ／ちょう」。
- 国立国语研究所关于关西否定辞的调查资料：用于确认「ン／ヘン／ナイ」存在词汇、音韵、位置和场合条件，避免把地域变体写成无条件替换。
- 国立国语研究所关于存在动词「いる」及「ている」历时发展的研究：用于核对辅助动词化方向。
- 国立国语研究所关于标准语、共通语和性别语言使用的研究说明：用于控制社会语言学题目的概率性和非本质主义表述。

## 最终结论

Wave 3 的 80 题在 overlay 生效后满足 authoring contract；逐题复核未发现仍需拒绝的候选。结构校验、自测、唯一性、题目覆盖、正确答案平衡、生成者/复核者分离和完整 replacement 守卫均通过。该复核结果可交给后续打包与数据库前置检查，但本复核任务未执行任何数据库写入。
