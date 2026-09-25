# 基础语言学 V1 Wave 2：生成阶段 QA

- 生成文件：`archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave2_generation.json`
- 题包：`foundation-v1-wave-2`
- 生成者：`/root/foundation_topics_reviewer`
- 日期：2026-07-27
- 原始文件 SHA-256：`e59bdf68731fd513f0a481d2f1dadeb55b6e9a3bb416006aa8377ff3582fd4c0`
- 状态：生成阶段冻结，等待不同 Agent 完成 80 / 80 独立复核
- 数据库：未写入

## 生成约束

1. 80 道题均由生成者逐条人工原创，没有使用 Python、JavaScript、SQL 或文本模板拼接教学内容。
2. 每个主题恰有 F1 识别、F2 解析、F3 对比诊断、F4 迁移应用各一题。
3. 所有例句、最小对照和对话均为中性构造材料，不包含作品、集数、角色、字幕、来源行或动漫专名。
4. 每题有四个同层级选项、一个正确答案以及三个针对本题语料的错误项解释。
5. 对话题显式标出必要说话人；零论元、引用、情态、证据性和会话含意题均核对跨话轮指代与语境。
6. 逐题按适用范围检查了形态、句法、语义、语用、否定、时制、体、语态、说话人和指称关系。

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

本表是生成者自检，不能替代独立复核。

| # | question_id | 答案 | 逐题核对结论 |
|---:|---|:---:|---|
| 01 | `foundation-v1-syn-topic-subject-f1` | A | 「この町は／冬が」清楚区分话语主题与主题内主格，不把「は」机械标作主语。 |
| 02 | `foundation-v1-syn-topic-subject-f2` | B | 「本は」可还原为「本を読む」的对象主题；格恢复与实际改写分开说明。 |
| 03 | `foundation-v1-syn-topic-subject-f3` | C | 前问「だれが」明确焦点，回答的「が／は」差异由问答语境而非旧新信息口诀解释。 |
| 04 | `foundation-v1-syn-topic-subject-f4` | D | 自动词「止まる」主格和未知机器焦点一致，题面排除了已建立主题的替代语境。 |
| 05 | `foundation-v1-syn-zero-arguments-f1` | B | 「申込書を」由前问主题和「出す」配价共同恢复，目标「受付に」已明示。 |
| 06 | `foundation-v1-syn-zero-arguments-f2` | C | 窗户由当前话题、开放状态和「閉める」选择限制三重支持，不按最近名词机械回指。 |
| 07 | `foundation-v1-syn-zero-arguments-f3` | D | 「ご覧になる」的敬语方向与前问部长主体一致，排除了说话人自我尊敬误析。 |
| 08 | `foundation-v1-syn-zero-arguments-f4` | A | 前轮颜色指令使「青い封筒を」唯一可恢复，红色封筒没有竞争资格。 |
| 09 | `foundation-v1-syn-valency-argument-structure-f1` | C | 「降る／開ける／渡す」的一、二、三价框架按核心参与者而非表面词数判断。 |
| 10 | `foundation-v1-syn-valency-argument-structure-f2` | D | 给予者、物品、接受者为核心角色；入口可替换，是场所附加语。 |
| 11 | `foundation-v1-syn-valency-argument-structure-f3` | A | 同形「かかる」的空间状态义与耗时义按选择限制给出不同配价。 |
| 12 | `foundation-v1-syn-valency-argument-structure-f4` | B | 新传递事件正确实现 Xが Yに Zを渡す，三个角色均未错位。 |
| 13 | `foundation-v1-syn-existential-constructions-f1` | D | 普通字面语境中猫用「いる」，不混入过去、礼貌或非法活用。 |
| 14 | `foundation-v1-syn-existential-constructions-f2` | A | 首次引入终端采用地点に＋存在物が＋ある，和已知主题所在句分开。 |
| 15 | `foundation-v1-syn-existential-constructions-f3` | B | 植物用「ある」、动物用「いる」按语法有生性解释，不等同生物学生命。 |
| 16 | `foundation-v1-syn-existential-constructions-f4` | C | 导览终端为无生新信息，地点、主格和存在动词配置自然。 |
| 17 | `foundation-v1-syn-relative-clauses-f1` | A | 连体从句在中心名词前闭合，主句「装置を使う」没有误划入从句。 |
| 18 | `foundation-v1-syn-relative-clauses-f2` | B | 内关系中心名词恢复为「机を使った」，从句对象与主句主题分层明确。 |
| 19 | `foundation-v1-syn-relative-clauses-f3` | C | 「読んだ本」可恢复对象格；「降った音」为伴随／内容外关系，不伪造格空位。 |
| 20 | `foundation-v1-syn-relative-clauses-f4` | D | 合并句同时保留制作人、地图对象和展示地点，中心名词只在主句显式带格。 |
| 21 | `foundation-v1-syn-nominalized-clauses-f1` | B | 「朝早く泳ぐの」边界完整，外层「が」附着整个名词化从句。 |
| 22 | `foundation-v1-syn-nominalized-clauses-f2` | C | 「こと」把搬迁计划作为「決める」的事件内容，词汇名词义未混入。 |
| 23 | `foundation-v1-syn-nominalized-clauses-f3` | D | 直接感知「の」与决定构式「こと」按主句谓语选择，未写成无例外互换。 |
| 24 | `foundation-v1-syn-nominalized-clauses-f4` | A | 内层儿童跑步与外层说话人看见的两个主体不混同，格关系完整。 |
| 25 | `foundation-v1-syn-complement-quotation-f1` | C | 直接引用边界、原请求语体和外层「と言った」清楚分开。 |
| 26 | `foundation-v1-syn-complement-quotation-f2` | D | 普通体间接补语使用「翌日／そこ」表达转述视点，外层发言者明确。 |
| 27 | `foundation-v1-syn-complement-quotation-f3` | A | 周一原发话与周二转述的第一人称、时间、地点和礼貌体转换一致。 |
| 28 | `foundation-v1-syn-complement-quotation-f4` | B | 直接礼貌原话转为无引号普通体补语，负责人仍控制内部省略主语。 |
| 29 | `foundation-v1-syn-te-clause-linking-f1` | D | 分句连接「開けて」与体貌内部「開いている」按后续结构区分。 |
| 30 | `foundation-v1-syn-te-clause-linking-f2` | A | 晚点与未赶上会议形成明确因果链，否定过去只作用于结果谓语。 |
| 31 | `foundation-v1-syn-te-clause-linking-f3` | B | 显式「利用者が」标主语切换；省略版默认延续负责人但保留语境可取消性。 |
| 32 | `foundation-v1-syn-te-clause-linking-f4` | C | 两个可控动作共享操作员零主语，对象格与先后顺序均正确。 |
| 33 | `foundation-v1-syn-conditionals-f1` | A | 「押すと開く」为可重复自动结果，后件不含命令或意志。 |
| 34 | `foundation-v1-syn-conditionals-f2` | B | 「なら」承接对方东京计划后给建议，跨话轮前提明确。 |
| 35 | `foundation-v1-syn-conditionals-f3` | C | 一次性到站后请求用「たら」自然；「と」与意志性请求后件的限制被准确说明。 |
| 36 | `foundation-v1-syn-conditionals-f4` | D | 「晴れたら」中的た不误判为过去，开放未来条件可接共同提议。 |
| 37 | `foundation-v1-syn-passive-f1` | B | 主动「担当者が資料を修正した」到直接被动的施受映射准确。 |
| 38 | `foundation-v1-syn-passive-f2` | C | 受影响者“我”、所属物“资料”和基础施事“同事”的所有者被动层级明确。 |
| 39 | `foundation-v1-syn-passive-f3` | D | 「見られる」的可能与被动同形通过地点条件、施事「人に」和主动还原消歧。 |
| 40 | `foundation-v1-syn-passive-f4` | A | 原对象提升、原施事改「に」、被动活用三步均正确，没有误写成使役。 |
| 41 | `foundation-v1-syn-causative-f1` | C | 「休む＋使役＋过去」形态切分正确，强制／许可留给语境判断。 |
| 42 | `foundation-v1-syn-causative-f2` | D | 外层指导者、内层学生主体和资料对象的两层论元映射完整。 |
| 43 | `foundation-v1-syn-causative-f3` | A | 请求、同意、第三方追问和“按本人希望”共同支撑许可使役，无说话人时序冲突。 |
| 44 | `foundation-v1-syn-causative-f4` | B | 他动基础句已有「箱を」，被使役者用「職員に」，致使者用「管理者は」。 |
| 45 | `foundation-v1-sem-compositionality-polysemy-f1` | D | 形容词修饰、场所「で」、对象「を」各自贡献被明确区分。 |
| 46 | `foundation-v1-sem-compositionality-polysemy-f2` | A | 「開く」的物理开放义与举行活动义由对象选择限制触发，并保留相关多义分析。 |
| 47 | `foundation-v1-sem-compositionality-polysemy-f3` | B | 「古い工場の写真」的两种理解归于修饰附着／范围，不误析为同音或时态。 |
| 48 | `foundation-v1-sem-compositionality-polysemy-f4` | C | 同一「開く」搭配箱子和展览会，形式不变且两个义项线索充分。 |
| 49 | `foundation-v1-sem-tense-reference-time-f1` | A | 事件时、说话时、基准时三个日期逐一对应，无交换。 |
| 50 | `foundation-v1-sem-tense-reference-time-f2` | B | 昨日会面与下周出发分层定位，从句非过去相对会面和发话均合理。 |
| 51 | `foundation-v1-sem-tense-reference-time-f3` | C | 未来提交提供基准，从句た形可表示提交前完成而晚于当前发话。 |
| 52 | `foundation-v1-sem-tense-reference-time-f4` | D | 8月10日发话、12日送达、15日查看的时间线由日期与主从句时制唯一编码。 |
| 53 | `foundation-v1-sem-lexical-aspect-f1` | B | 无终点步行活动与发现、制作完成、破裂达成正确区分。 |
| 54 | `foundation-v1-sem-lexical-aspect-f2` | C | 「一時間」量活动时长，「一時間で」量到达完成点所需时间。 |
| 55 | `foundation-v1-sem-lexical-aspect-f3` | D | 量化对象产生单次终点，「毎朝」建立习惯事件序列，短语层面分析完整。 |
| 56 | `foundation-v1-sem-lexical-aspect-f4` | A | 广场步行无路径终点或量化产物；另三项都有自然达成边界。 |
| 57 | `foundation-v1-sem-teiru-readings-f1` | C | 活动谓语加当前观察点得到进行读法，不误判结果状态。 |
| 58 | `foundation-v1-sem-teiru-readings-f2` | D | 瞬时变化「開く」加ている呈现当前开放结果，窗户未误作有意施事。 |
| 59 | `foundation-v1-sem-teiru-readings-f3` | A | 自动变化＋「今」和他动反复＋「毎朝」分别支持结果状态与习惯。 |
| 60 | `foundation-v1-sem-teiru-readings-f4` | B | 明示维护记录、去年和两次，建立经验／记录读法且不蕴涵当前停机。 |
| 61 | `foundation-v1-sem-modality-f1` | D | 规则牌使「てはいけない」具有明确道义禁止来源。 |
| 62 | `foundation-v1-sem-modality-f2` | A | 备用钥匙提供现实手段，可能形解释为能力／环境可能而非许可。 |
| 63 | `foundation-v1-sem-modality-f3` | B | 同形可能问句由工作人员的规则回应消歧为许可，跨话轮证据充分。 |
| 64 | `foundation-v1-sem-modality-f4` | C | 「かもしれない」对整个使用命题作认识概率判断，与能力、许可、义务平行对照。 |
| 65 | `foundation-v1-sem-evidentiality-f1` | A | 预报来源和终止形接续共同锁定传闻「そうだ」。 |
| 66 | `foundation-v1-sem-evidentiality-f2` | B | 焦味是嗅觉直接证据，内部过热是间接推断，未把样态证据限于视觉。 |
| 67 | `foundation-v1-sem-evidentiality-f3` | C | 「降るそうだ／降りそうだ」按接续和预报／天空证据双重消歧。 |
| 68 | `foundation-v1-sem-evidentiality-f4` | D | 广播内容由名词谓语终止形接传闻「そうです」，礼貌外层不改变来源。 |
| 69 | `foundation-v1-sem-negation-scope-f1` | B | NOT(ALL came) 与 ALL(NOT came) 明确分开，句子不虚构具体到场人数。 |
| 70 | `foundation-v1-sem-negation-scope-f2` | C | 「なければならない」按完整必要构式解释，形态否定与外层情态作用域准确。 |
| 71 | `foundation-v1-sem-negation-scope-f3` | D | 两交一未交的枚举事实只与部分否定相容，全有全无选项逐项排除。 |
| 72 | `foundation-v1-sem-negation-scope-f4` | A | 「だれも…ない」明确全无；较弱「わけではない」被指出只是不充分而非必假。 |
| 73 | `foundation-v1-sem-quantifier-scope-f1` | C | 浮游「三人」由人类量词与主格学生连接，不误作事件次数。 |
| 74 | `foundation-v1-sem-quantifier-scope-f2` | D | 「二台」计机器个体、「二回」计点检事件，真值差异和同机重复边界明确。 |
| 75 | `foundation-v1-sem-quantifier-scope-f3` | A | 三本中读两本支持 NOT(ALL)，不被错误升级为一本也没读。 |
| 76 | `foundation-v1-sem-quantifier-scope-f4` | B | 「三台の機械」让设备数量进入名词内部，排除只计三次事件。 |
| 77 | `foundation-v1-prag-context-implicature-f1` | D | 报告书完成是字面内容，另一准备可能未完成是可撤销数量／相关性含意。 |
| 78 | `foundation-v1-prag-context-implicature-f2` | A | 整体问题与局部回答的信息落差可计算出含意，不借身份、欺骗或词典第二义。 |
| 79 | `foundation-v1-prag-context-implicature-f3` | B | 强化句与取消句保持原字面真值，正确实施可撤销性测试。 |
| 80 | `foundation-v1-prag-context-implicature-f4` | C | 临近的另一会议产生潜在婉拒含意；隔壁会场和两点开始条件自然取消该含意。 |

## Validator 结果

运行：

```text
node scripts/validate-foundation-content.mjs pack archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave2_generation.json
```

结构与内容结果：

- `rows`: 80
- `uniqueIds`: 80
- `topicCount`: 20
- `answerCounts`: A=20、B=20、C=20、D=20
- `warnings`: 0
- generation canonical SHA-256：`817307ad8b7bd1cc1f5bd05364138232fd47793ef6ea568a193b981265c6eacf`

Validator 当前仅报告两类预期的生成阶段审计缺口：

1. 80 / 80 尚无 `reviewer_agent`；
2. 80 / 80 尚无独立复核后的 `quality_score`。

这两项必须由不同 Agent 的 review overlay 补齐；在此之前本题包不得发布或写入数据库。
