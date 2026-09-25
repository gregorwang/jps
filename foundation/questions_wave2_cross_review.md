# 基础语言学 V1 Wave 2：独立逐题复核

- 生成文件：`foundation/questions_wave2_generation.json`
- Review overlay：`foundation/questions_wave2_review_overlay.json`
- 生成者：`/root/foundation_topics_reviewer`
- 独立复核者：`/root/foundation_topics_generator`
- 生成文件 raw SHA-256：`e59bdf68731fd513f0a481d2f1dadeb55b6e9a3bb416006aa8377ff3582fd4c0`
- 日期：2026-07-27
- 数据库：未写入

## 复核结论

| 检查项 | 结果 |
|---|---:|
| 基础题 | 80 |
| 唯一题目 ID | 80 |
| 覆盖主题 | 20 |
| F1 / F2 / F3 / F4 | 20 / 20 / 20 / 20 |
| keep / revise / reject | 74 / 6 / 0 |
| 完整同 ID replacements | 6 |
| 最低质量分 | 96 |
| 正确答案 A / B / C / D | 20 / 20 / 20 / 20 |
| Validator errors / warnings | 0 / 0 |

逐题复核覆盖了适用的日语形态、活用、词类、句法成分、格关系、配价、从句边界、零论元、词义、作用域、时制、体、情态、证据性、语态、说话人、指代、敬语方向、会话含意和跨话轮上下文。所有 `revise` 均在 overlay 中提供完整同 ID 题目，按 ID 替换而非追加。

## 六项修订

1. `foundation-v1-syn-relative-clauses-f3`
   - 原外关系例「雨が降った音」在孤立形式下自然度不足。
   - 改为「雨が屋根を打つ音」：从句已含「雨が」「屋根を」，中心名词「音」通过事件产生关系连接，不能补成「打つ」的普通格论元。
2. `foundation-v1-syn-passive-f2`
   - 原提示误残留另一题的「ぬらす／荷物」。
   - 改为本题实际的「捨てる／資料」，与所有者被动「私は同僚に資料を捨てられた」完全一致。
3. `foundation-v1-sem-compositionality-polysemy-f2`
   - 原「窓を開く」会引入「開ける」偏好及「開く」读法干扰。
   - 改为自然且明确读作「ひらく」的「本を開く／会議を開く」。
4. `foundation-v1-sem-compositionality-polysemy-f3`
   - 原题把「古い工場の写真」分析为「古い」也可宽域修饰整个照片短语，句法依据不足。
   - 改为真实的并列修饰范围歧义「古い机と椅子」：可为「[古い机]と[椅子]」或「古い[机と椅子]」。
5. `foundation-v1-sem-compositionality-polysemy-f4`
   - 原「箱を開く」同样存在搭配自然度干扰。
   - 改为「本を開く／展示会を開く」，词形与读法保持一致，两个对象提供清楚选义线索。
6. `foundation-v1-prag-context-implicature-f4`
   - 原提示及 A 项错因把下午说明会之前的另一场会议误写成“上午会议”。
   - 改为“会前另有一场会议／另一场会议”，保留 13:50 结束、14:00 开始、会场相邻的完整时间链。

## 80 题逐条台账

| # | question_id | 决定 | 分数 | 独立复核结论 |
|---:|---|:---:|---:|---|
| 01 | `foundation-v1-syn-topic-subject-f1` | keep | 98 | 主题框架与主题内主格分层准确。 |
| 02 | `foundation-v1-syn-topic-subject-f2` | keep | 98 | 「読む」对象主题可恢复为「本を」，不把分析还原当实际强制改写。 |
| 03 | `foundation-v1-syn-topic-subject-f3` | keep | 98 | 「だれが」问答焦点使「が」版本更直接，「は」差异保留语境条件。 |
| 04 | `foundation-v1-syn-topic-subject-f4` | keep | 98 | 自动词主格和未知机器焦点一致，仅 D 完整回答。 |
| 05 | `foundation-v1-syn-zero-arguments-f1` | keep | 98 | 「申込書を」由前问和提交配价共同恢复。 |
| 06 | `foundation-v1-syn-zero-arguments-f2` | keep | 98 | 窗户由话语显著性、开放状态与选择限制三重确定。 |
| 07 | `foundation-v1-syn-zero-arguments-f3` | keep | 99 | 部长主体同时符合前问和「ご覧になる」敬语方向。 |
| 08 | `foundation-v1-syn-zero-arguments-f4` | keep | 98 | 蓝色信封被前轮唯一指定，省略后的对象与目标均可恢复。 |
| 09 | `foundation-v1-syn-valency-argument-structure-f1` | keep | 98 | 一价、二价、三价按核心角色而非表面词数判断。 |
| 10 | `foundation-v1-syn-valency-argument-structure-f2` | keep | 98 | 三个传递论元与场所附加语划分准确。 |
| 11 | `foundation-v1-syn-valency-argument-structure-f3` | keep | 97 | 同形「かかる」按空间状态义和耗时义分别建立配价。 |
| 12 | `foundation-v1-syn-valency-argument-structure-f4` | keep | 98 | 「Xが Yに Zを渡す」三个角色格映射完整。 |
| 13 | `foundation-v1-syn-existential-constructions-f1` | keep | 96 | 普通字面语境中的猫用「いる」，唯一答案成立。 |
| 14 | `foundation-v1-syn-existential-constructions-f2` | keep | 98 | 新存在物与已知主题所在句的信息结构分开。 |
| 15 | `foundation-v1-syn-existential-constructions-f3` | keep | 98 | 植物／动物对照按语法有生性，不误用生物学二分。 |
| 16 | `foundation-v1-syn-existential-constructions-f4` | keep | 97 | 无生终端首次引入的地点、主格和「ある」配置正确。 |
| 17 | `foundation-v1-syn-relative-clauses-f1` | keep | 98 | 从句在中心名词前闭合，主句「使う」不越界。 |
| 18 | `foundation-v1-syn-relative-clauses-f2` | keep | 98 | 内关系「机」可恢复为「机を」，两个句法层级明确。 |
| 19 | `foundation-v1-syn-relative-clauses-f3` | revise | 98 | 改用自然的「雨が屋根を打つ音」，内外关系诊断无伪造格空位。 |
| 20 | `foundation-v1-syn-relative-clauses-f4` | keep | 98 | 制作人、地图和展示地点在合并后均保持原角色。 |
| 21 | `foundation-v1-syn-nominalized-clauses-f1` | keep | 98 | 「朝早く泳ぐの」边界及外层「が」附着正确。 |
| 22 | `foundation-v1-syn-nominalized-clauses-f2` | keep | 97 | 「こと」把搬迁计划包装为「決める」的事件内容。 |
| 23 | `foundation-v1-syn-nominalized-clauses-f3` | keep | 98 | 直接感知「の」与决定「こと」按典型谓语选择区分。 |
| 24 | `foundation-v1-syn-nominalized-clauses-f4` | keep | 98 | 内层儿童与外层说话人主体不混同。 |
| 25 | `foundation-v1-syn-complement-quotation-f1` | keep | 98 | 直接引用保留请求语体和指示词，边界清楚。 |
| 26 | `foundation-v1-syn-complement-quotation-f2` | keep | 97 | 普通体间接补语与转述者的时间地点指示相容。 |
| 27 | `foundation-v1-syn-complement-quotation-f3` | keep | 98 | 原发话到转述的人称、时地与礼貌体转换自洽。 |
| 28 | `foundation-v1-syn-complement-quotation-f4` | keep | 98 | 负责人控制内部零主语，间接补语采用普通体。 |
| 29 | `foundation-v1-syn-te-clause-linking-f1` | keep | 98 | 分句连接与「ている」体貌内部结构准确区分。 |
| 30 | `foundation-v1-syn-te-clause-linking-f2` | keep | 98 | 晚点原因、未赶上结果及否定过去关系清楚。 |
| 31 | `foundation-v1-syn-te-clause-linking-f3` | keep | 98 | 主语切换版显式，省略版只表达可取消的连续性偏好。 |
| 32 | `foundation-v1-syn-te-clause-linking-f4` | keep | 97 | 同一操作员共享零主语，两项他动对象格和顺序正确。 |
| 33 | `foundation-v1-syn-conditionals-f1` | keep | 98 | 「と」与设备自动结果配合，后件无意志性。 |
| 34 | `foundation-v1-syn-conditionals-f2` | keep | 98 | 「なら」承接甲的东京计划并由乙给建议。 |
| 35 | `foundation-v1-syn-conditionals-f3` | keep | 98 | 一次性到站后请求用「たら」自然，对「と」限制未绝对化。 |
| 36 | `foundation-v1-syn-conditionals-f4` | keep | 98 | 「晴れたら」是未来开放条件，た形未误判为过去事实。 |
| 37 | `foundation-v1-syn-passive-f1` | keep | 98 | 直接被动的受事提升和「に」施事者映射准确。 |
| 38 | `foundation-v1-syn-passive-f2` | revise | 98 | 修正错误提示后，所有者被动的我、同事、资料三角色完全一致。 |
| 39 | `foundation-v1-syn-passive-f3` | keep | 98 | 「見られる」的可能／被动由格框和主动还原消歧。 |
| 40 | `foundation-v1-syn-passive-f4` | keep | 98 | 对象提升、施事格与被动活用三步正确。 |
| 41 | `foundation-v1-syn-causative-f1` | keep | 98 | 「休む＋使役＋过去」形态切分准确。 |
| 42 | `foundation-v1-syn-causative-f2` | keep | 98 | 指导者、学生、资料形成外层致使与内层阅读结构。 |
| 43 | `foundation-v1-syn-causative-f3` | keep | 99 | 请求、同意与「本人の希望どおり」共同支持许可读法。 |
| 44 | `foundation-v1-syn-causative-f4` | keep | 98 | 他动基础句保留对象「箱を」，被使役者用「職員に」。 |
| 45 | `foundation-v1-sem-compositionality-polysemy-f1` | keep | 98 | 修饰语、场所格与对象格对句义的贡献逐项正确。 |
| 46 | `foundation-v1-sem-compositionality-polysemy-f2` | revise | 98 | 改为「本を開く／会議を開く」，消除读法和搭配干扰。 |
| 47 | `foundation-v1-sem-compositionality-polysemy-f3` | revise | 99 | 改为「古い机と椅子」的并列修饰范围歧义。 |
| 48 | `foundation-v1-sem-compositionality-polysemy-f4` | revise | 98 | 改为同读法「本を開く／展示会を開く」的多义迁移对照。 |
| 49 | `foundation-v1-sem-tense-reference-time-f1` | keep | 98 | 事件时、说话时、基准时三个日期无歧义。 |
| 50 | `foundation-v1-sem-tense-reference-time-f2` | keep | 97 | 昨日会面与下周出发的主从句时间定位相容。 |
| 51 | `foundation-v1-sem-tense-reference-time-f3` | keep | 96 | 明示未来提交基准支持完成先行读法，并提醒脱离语境的竞争解释。 |
| 52 | `foundation-v1-sem-tense-reference-time-f4` | keep | 97 | 10日发话、12日送达、15日查看的相对时间线明确。 |
| 53 | `foundation-v1-sem-lexical-aspect-f1` | keep | 98 | 步行活动与发现、量化制作、破裂达成分类准确。 |
| 54 | `foundation-v1-sem-lexical-aspect-f2` | keep | 98 | 时长与到达完成点所需时间的诊断正确。 |
| 55 | `foundation-v1-sem-lexical-aspect-f3` | keep | 97 | 单次有界对象与整体习惯序列分层说明。 |
| 56 | `foundation-v1-sem-lexical-aspect-f4` | keep | 98 | 广场步行无内在终点，其他候选均有达成边界。 |
| 57 | `foundation-v1-sem-teiru-readings-f1` | keep | 98 | 活动谓语和「今」锁定进行读法。 |
| 58 | `foundation-v1-sem-teiru-readings-f2` | keep | 98 | 瞬时变化「開く」加ている得到当前开放结果状态。 |
| 59 | `foundation-v1-sem-teiru-readings-f3` | keep | 98 | 「今」自动变化与「毎朝」反复活动分别支持结果、习惯。 |
| 60 | `foundation-v1-sem-teiru-readings-f4` | keep | 97 | 记录来源、去年和两次支持经验／记录读法，不蕴涵当前停机。 |
| 61 | `foundation-v1-sem-modality-f1` | keep | 98 | 规则牌明确提供道义禁止来源。 |
| 62 | `foundation-v1-sem-modality-f2` | keep | 98 | 备用钥匙提供现实手段，题面排除许可歧义。 |
| 63 | `foundation-v1-sem-modality-f3` | keep | 98 | 工作人员援引规则的回应把可能问句消歧为许可。 |
| 64 | `foundation-v1-sem-modality-f4` | keep | 98 | 「かもしれない」对核心命题取认识情态作用域。 |
| 65 | `foundation-v1-sem-evidentiality-f1` | keep | 98 | 预报来源与终止形接续锁定传闻「そうだ」。 |
| 66 | `foundation-v1-sem-evidentiality-f2` | keep | 98 | 焦味为嗅觉证据，内部过热为间接推断。 |
| 67 | `foundation-v1-sem-evidentiality-f3` | keep | 99 | 「降る／降り」接续与预报／天空证据双重消歧。 |
| 68 | `foundation-v1-sem-evidentiality-f4` | keep | 98 | 广播内容以名词谓语终止形接传闻「そうです」。 |
| 69 | `foundation-v1-sem-negation-scope-f1` | keep | 98 | NOT(ALL) 与 ALL(NOT) 分开，未虚构具体人数。 |
| 70 | `foundation-v1-sem-negation-scope-f2` | keep | 98 | 「なければならない」按完整必要构式和作用域解释。 |
| 71 | `foundation-v1-sem-negation-scope-f3` | keep | 98 | 两交一未交只与部分否定相容。 |
| 72 | `foundation-v1-sem-negation-scope-f4` | keep | 98 | 「だれも…ない」明确全无，「わけではない」仅为较弱相容项。 |
| 73 | `foundation-v1-sem-quantifier-scope-f1` | keep | 98 | 「三人」计学生个体而非三次到来事件。 |
| 74 | `foundation-v1-sem-quantifier-scope-f2` | keep | 98 | 「台」计机器，「回」计事件，真值边界明确。 |
| 75 | `foundation-v1-sem-quantifier-scope-f3` | keep | 98 | 三本中读两本支持 NOT(ALL)，不升级为全无。 |
| 76 | `foundation-v1-sem-quantifier-scope-f4` | keep | 98 | 「三台の機械」将数量直接限制对象名词。 |
| 77 | `foundation-v1-prag-context-implicature-f1` | keep | 98 | 字面只断言报告完成，另一准备未完成为可撤销含意。 |
| 78 | `foundation-v1-prag-context-implicature-f2` | keep | 98 | 整体问题与局部回答的信息差可计算出数量含意。 |
| 79 | `foundation-v1-prag-context-implicature-f3` | keep | 98 | 强化和取消追加句保留原字面真值，正确检验可撤销性。 |
| 80 | `foundation-v1-prag-context-implicature-f4` | revise | 98 | 修正会议时段残留后，13:50、14:00、隔壁会场的取消链一致。 |

## Validator

运行：

```text
node scripts/validate-foundation-content.mjs pack foundation/questions_wave2_generation.json foundation/questions_wave2_review_overlay.json
```

结果：

- `rows`: 80
- `uniqueIds`: 80
- `topicCount`: 20
- `replacementsApplied`: 6
- `answerCounts`: A=20、B=20、C=20、D=20
- `generatorAgent`: `/root/foundation_topics_reviewer`
- `reviewerAgent`: `/root/foundation_topics_generator`
- `qualityScore`: 96
- `errors`: 0
- `warnings`: 0
- effective canonical SHA-256：`79a35b9793bd9b91bc8094126f7a0847d6f21ed99e43a0182ceecf22485c05a6`

本复核未写数据库。只有在后续传输守卫再次确认生成 SHA、80 个唯一 ID、同 ID replacement、无 reject、最低分不低于 95 以及数据库无历史重叠后，题包才可进入发布事务。
