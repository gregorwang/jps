# 日语基础语言学主题目录 V1：独立交叉复核

- 生成文件：`foundation/topics_v1_generation.json`
- 生成者：`/root/foundation_topics_generator`
- 独立复核者：`/root/foundation_topics_reviewer`
- 生成文件原始 SHA-256：`4557208fad1ef1b3e024fd24b852c054c68007f66d64d01cc4bcb0de1bec7d1a`
- 复核范围：60 / 60 个 `topic_key`
- 结论：`keep` 47，`revise` 13，`reject` 0
- 修订方式：`foundation/topics_v1_review_overlay.json` 中按 `topic_key` 整条替换；没有追加 ID
- 评分口径：下表质量分评估“生成记录应用本次 replacement 后”的候选；全体最低分 96，可进入题目生成阶段

## 总结

60 个主题的领域配额、模块层级和 F1–F4 递进总体成立。复核确认：

1. 领域配额仍为 8 / 10 / 13 / 8 / 12 / 5 / 4，主题总数和唯一 ID 均为 60。
2. 所有先修项都指向已存在且排序更早的主题；没有后向引用、缺失引用或环。
3. 形态、句法、语义与语用层级没有被中文翻译机械替代。形态时态与语义时制、主题与焦点、零论元与照应、词汇性自他对应与句法使役等相邻主题的边界可操作。
4. 否定、时态、语态、说话人、指代、敬语方向与跨话轮信息在对应主题中均有明确诊断；没有依赖动漫角色、剧情或字幕上下文。
5. 13 项原记录含会误导后续出题的过强概括、分类问题或术语瑕疵，已全部以同 ID 完整记录修订；没有未解决的拒绝项。

## 关键修订

- `phw_special_morae`：促音不再一律描述为“阻塞”，改为随闭塞音、摩擦音等后续辅音类别实现。
- `phw_kana_orthography`：不再把假名体系说成简单“音节表”，改为以拍为基础的音形对应。
- `morph_word_morpheme_classes`：移除“词必可独立使用”的充分条件，改用独立性、黏着性、分布等多项诊断。
- `morph_transitivity_pairs`：修正“自词／他词”为“自动词／他动词”。
- `syn_case_particles`：移除把「まで」直接列作格助词的分类错误，并说明其作为副助词与格成分组合。
- `syn_existential_constructions`：将「ある／いる」从生物学有生命改为语法有生性和主体性概念化，明确植物通常用「ある」。
- `syn_relative_clauses`：区分可恢复格关系的内关系和不存在该格空位的外关系。
- `syn_complement_quotation`：明确普通体主要约束间接引用和命题补语；直接引用可保留原发话语体、句末形式和指示中心。
- `sem_teiru_readings`：把经验／记录用法与结果状态及任意过去事件分开，并补足基准时和主体视角条件。
- `sem_evidentiality`：样态「そうだ」的证据不再限于视觉，改为当下可感知的外观、声音等迹象。
- `prag_politeness_honorifics`：按尊敬语、谦让语Ⅰ、谦让语Ⅱ（丁重语）和礼貌语拆分敬意方向，移除“谦让就是降低自己”的单一解释。
- `soc_standard_regional_variation`：区分共同语、标准语和东京地域变体，保留历史联系但不画等号。
- `hist_sound_change_orthography`：把历史假名遣这一记录体系与音变过程分开，并限制共同语音变结论的地域范围。

敬语分类复核参考了文化厅《敬語の指針》所采用的尊敬语、谦让语Ⅰ、谦让语Ⅱ（丁重语）、礼貌语与美化语体系；连体修饰复核参考了国立国语研究所关于内关系、外关系和内容补充关系的研究框架。`ている` 的经验／记录用法按学术文献中的独立用法处理，不把任意过去事件直接归入该类。

## 逐条复核台账

| # | topic_key | 决定 | 复核后质量分 | 独立复核结论 |
|---:|---|---|---:|---|
| 01 | `phw_mora_syllable` | keep | 98 | 拍、音节与假名数边界准确；拗音和特殊拍计数没有混同。 |
| 02 | `phw_special_morae` | revise | 97 | 原稿把促音说成后续辅音“阻塞延长”，未覆盖摩擦音；replacement 已按辅音类别修正。 |
| 03 | `phw_pitch_accent` | keep | 97 | 明确限定东京式、重音核和下降位置，不把音高重音等同强弱重读。 |
| 04 | `phw_vowel_devoicing` | keep | 98 | 清化与音位删除、底层元音与表层听感、概率条件与绝对规则均区分清楚。 |
| 05 | `phw_rendaku` | keep | 97 | 连浊作为形态音系倾向处理；莱曼定律未被写成无例外规则。 |
| 06 | `phw_kana_orthography` | revise | 97 | 原稿“共享音节表”会与拍／音节主题冲突；replacement 改为以拍为基础的音形清单。 |
| 07 | `phw_script_choice` | keep | 97 | 汉字、平假名和片假名选择同时覆盖词界、读者、风格和合法变体。 |
| 08 | `phw_kanji_reading_okurigana` | keep | 96 | 音读、训读、熟字读及送假名均按完整词项判断，没有单字猜读。 |
| 09 | `morph_word_morpheme_classes` | revise | 97 | 原短定义把词等同于可独立单位，条件过强；replacement 改为多证据边界诊断。 |
| 10 | `morph_verb_conjugation` | keep | 98 | 五段、一段、不规则和音便范围准确，「る」结尾误判风险已覆盖。 |
| 11 | `morph_adjective_inflection` | keep | 98 | い形容词、な形容词的定语、谓语、否定和过去连接分层准确。 |
| 12 | `morph_copula` | keep | 97 | 「だ／です／である」系列和语体、时态、否定限制清楚，不做残片替换。 |
| 13 | `morph_auxiliary_chain` | keep | 96 | 线性连接、层级和作用域关系可操作，并承认传统与现代分析不一一对应。 |
| 14 | `morph_negation_forms` | keep | 97 | 三类谓语的否定形态不交叉套用；「じゃない」确认用法被单独提醒。 |
| 15 | `morph_tense_forms` | keep | 96 | 非过去／过去作为形态标签，与实际时间读法和发现用法分开。 |
| 16 | `morph_derivation_nominalization` | keep | 97 | 词汇派生、词汇性名词和「の／こと」从句名词化层级明确。 |
| 17 | `morph_compounding` | keep | 97 | 中心语、括号结构、透明度、连浊和重音重组边界充分。 |
| 18 | `morph_transitivity_pairs` | revise | 98 | 概念分析正确，但 F4 有“自词／他词”术语瑕疵；replacement 已改为规范术语。 |
| 19 | `syn_constituent_order` | keep | 97 | 谓语居后作为倾向，成分移动与逐词乱序严格区分。 |
| 20 | `syn_case_particles` | revise | 97 | 原短定义把「まで」纳入格助词列举；replacement 已区分格助词与可叠加副助词。 |
| 21 | `syn_topic_subject` | keep | 98 | 主题、主格、对比、穷尽和问答语境关系准确，不作旧／新信息二分。 |
| 22 | `syn_zero_arguments` | keep | 99 | 配价、说话人、候选指代、视点、语态、敬语方向和跨句连续性覆盖最完整。 |
| 23 | `syn_valency_argument_structure` | keep | 98 | 核心论元、附加语和表面省略不混同；多义谓语的配价变化可检验。 |
| 24 | `syn_existential_constructions` | revise | 97 | 原稿“有生命”概括会误判植物；replacement 已改为日语语法有生性概念化。 |
| 25 | `syn_relative_clauses` | revise | 98 | 原入门解释暗示所有中心名词都在从句中有角色；replacement 已区分内外关系。 |
| 26 | `syn_nominalized_clauses` | keep | 97 | 「の／こと」的选择限制、视点、事实性及多功能性说明准确。 |
| 27 | `syn_complement_quotation` | revise | 98 | 原稿可能让人以为直接引用也须普通体；replacement 已按直接／间接引用分别说明。 |
| 28 | `syn_te_clause_linking` | keep | 98 | 顺序、并列、方式、原因读法由事件结构和语境确定，主语切换也有约束。 |
| 29 | `syn_conditionals` | keep | 97 | 「と・ば・たら・なら」写成重叠且受制约的系统，命令和意志限制未绝对化。 |
| 30 | `syn_passive` | keep | 98 | 直接、所有者、间接受影响被动及「られる」同形歧义均可诊断。 |
| 31 | `syn_causative` | keep | 97 | 致使者、被使役者与基础事件两层结构准确；命令、许可来自语境。 |
| 32 | `sem_compositionality_polysemy` | keep | 97 | 词义、结构义、多义、同音和语用充实边界清楚。 |
| 33 | `sem_tense_reference_time` | keep | 96 | 事件时、说话时和基准时三点关系适用于主句与连体从句比较。 |
| 34 | `sem_lexical_aspect` | keep | 98 | 状态、活动、终结性作为谓语短语组合结果，而非永久动词词典标签。 |
| 35 | `sem_teiru_readings` | revise | 97 | 原稿把经验写成“已获得的经历状态”且条件不足；replacement 补足经验／记录、基准时和限制。 |
| 36 | `sem_modality` | keep | 97 | 动力、道义、认识情态与否定内外作用域分开。 |
| 37 | `sem_evidentiality` | revise | 97 | 原稿把样态判断写得过度视觉化；replacement 扩为当下可感知迹象并保留接续诊断。 |
| 38 | `sem_negation_scope` | keep | 98 | 全否定、部分否定、情态和焦点范围可用枚举事实判真。 |
| 39 | `sem_quantifier_scope` | keep | 97 | 浮游量词的宿主、个体数量、事件次数、量词类别和否定范围均分开。 |
| 40 | `prag_context_implicature` | keep | 98 | 字面内容、会话含意、可撤销测试和语义蕴涵严格区分。 |
| 41 | `prag_speech_acts` | keep | 98 | 表面句式、行为力量、社会成本和预期回应构成完整诊断链。 |
| 42 | `prag_deixis` | keep | 98 | 说话人、听话人、时空、话语位置和转述中心转换均明确。 |
| 43 | `prag_reference_anaphora` | keep | 98 | 最近名词不被机械选作先行语；桥接、显著性和竞争候选处理准确。 |
| 44 | `prag_information_focus` | keep | 97 | 当前问题、替代集合、背景、信息焦点和对比焦点边界可执行。 |
| 45 | `prag_ellipsis_fragments` | keep | 98 | 句法恢复内容与拒绝、保留等礼貌含意分开，不擅补主语、否定或态度。 |
| 46 | `prag_sentence_final_particles` | keep | 97 | 「ね／よ」按知识分布和回应期待解释，不作固定翻译或性别专属判断。 |
| 47 | `prag_politeness_honorifics` | revise | 98 | 原稿“降低己方行为”过度简化谦让语；replacement 已拆分谦让语Ⅰ、Ⅱ及敬意方向。 |
| 48 | `prag_register_style` | keep | 98 | 语域按受众、媒介、任务整体组织；有动机语体切换不被误判。 |
| 49 | `prag_turn_taking_backchannels` | keep | 97 | 附和的持续倾听、接收和同意功能按序列位置区分，并保留无韵律时的不确定性。 |
| 50 | `prag_connectives_coherence` | keep | 98 | 因果方向、转折、话题转换及事实／推论／言语行为层级准确。 |
| 51 | `prag_viewpoint_empathy` | keep | 97 | 授受、移动、语态、零论元和敬语方向围绕同一视点中心核对。 |
| 52 | `soc_standard_regional_variation` | revise | 98 | 原稿易把共同语直接等同东京地区语言；replacement 已区分共同语、标准语和地域变体。 |
| 53 | `soc_gender_age_indexing` | keep | 99 | 性别、年龄只作概率性社会索引，禁止由单一人称或句末形式推断身份。 |
| 54 | `soc_style_shifting_community` | keep | 98 | 同一说话人的受众设计、任务变化和实践共同体规范均有覆盖。 |
| 55 | `soc_uchi_soto_power` | keep | 97 | 内外边界、制度权力和亲疏是动态且可能竞争的维度，不作文化本质化。 |
| 56 | `soc_contact_borrowing` | keep | 97 | 音系适配、形态整合、语义变化、和制形式和临时转写地位清楚。 |
| 57 | `hist_classical_modern_grammar` | keep | 96 | 古典与现代范式按时代和语体区分；现代残留的生产性与固定性分开。 |
| 58 | `hist_sound_change_orthography` | revise | 97 | 原稿把历史假名遣和音变并列成“过程”且未限定地域；replacement 已修正。 |
| 59 | `hist_grammaticalization_auxiliaries` | keep | 96 | 实义与辅助义可共存，语义漂白、黏着增强等证据不被写成同步必然。 |
| 60 | `hist_lexicalization_construction_change` | keep | 96 | 词汇化、重新分析、构式扩展、频率和替代假说均分层说明。 |

## 结构与覆盖复核

### 先修关系

- 不存在的先修引用：0
- 指向后置主题的先修引用：0
- 环：0
- 需要改动的先修关系：0

当前 DAG 从书写／音系单位，经形态、句法、语义，进入语用、社会语言学和历史语言学。某些主题并不理论上必须学习完所有先修项才能接触，但这些边在 V1 中表达的是课程依赖而非语言学因果，未发现会阻断合理教学顺序的边。

### 相邻主题边界

- `morph_tense_forms` 负责形态形式，`sem_tense_reference_time` 负责时间解释。
- `morph_transitivity_pairs` 负责词汇性自他对应，`syn_causative` 负责句法使役。
- `syn_topic_subject` 负责主题和主格，`prag_information_focus` 负责当前问题和替代集合。
- `syn_zero_arguments` 负责未发音论元，`prag_reference_anaphora` 负责跨话语指称链。
- `syn_relative_clauses` 负责连体修饰，`syn_nominalized_clauses` 负责从句进入名词性位置。
- `prag_politeness_honorifics` 负责敬意方向，`prag_register_style` 负责整段表达的媒介与语域一致性。
- `hist_grammaticalization_auxiliaries` 负责语法功能形成，`hist_lexicalization_construction_change` 负责词汇固化、重新分析和构式能产性。

### 后续出题硬约束

1. 历史语言学题中的真实形式、年代和变化链必须逐题由权威资料核实；目录通过不等于具体历史例句自动通过。
2. 音高重音题必须明确采用东京式等具体体系；无音频题应提供可读的音高标注，不要求学习者凭文字猜音。
3. 样态、引用、指示、零论元、敬语和话轮题必须显式给出说话人、听话人、基准时、必要前文和角色关系。
4. 任何真实借词、连浊词、地域变体或古典形式都不能把规则预测冒充词典／史料事实。
5. 题目生成者不得复用本次目录复核者作为同一批题目的独立复核者。

## 校验结果

运行：

```text
node scripts/validate-foundation-content.mjs topics foundation/topics_v1_generation.json foundation/topics_v1_review_overlay.json
```

结果：

- 有效主题：60
- 唯一 ID：60
- 应用 replacement：13
- 独立复核覆盖：60 / 60
- 生成者与复核者不同：通过
- 最低复核后质量分：96
- 错误：0
- 警告：0
- 合并后 canonical SHA-256：`39246816c3d9df5749020d06eda91dd43ae78c68dee13443d110b1bdad7578ba`

