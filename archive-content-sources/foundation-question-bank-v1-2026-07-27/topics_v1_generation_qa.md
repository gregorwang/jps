# 日语基础语言学主题目录 V1：生成阶段 QA

- 文件：`archive-content-sources/foundation-question-bank-v1-2026-07-27/topics_v1_generation.json`
- 生成者：`/root/foundation_topics_generator`
- 日期：2026-07-27
- 阶段：生成自检完成，等待不同 Agent 独立复核
- 范围：60 个 canonical 学习主题；本文件不包含可作答题目，也不包含数据库写入

## 创作约束

1. 60 项由生成者逐条撰写，没有用 Python、JavaScript、SQL 或文本模板批量拼接教学内容。
2. 每项包含唯一 `topic_key`、领域、模块、中日名称、短定义、入门解释、深入解释、风险提醒、先修项、标签、F1–F4 四级目标及原创例式规范。
3. `original_example_spec` 只规定未来原创例题应采用的结构、对照和限制，没有复用动漫字幕，也没有把作品角色、剧情或台词写成基础概念。
4. F1–F4 分别承担识别、解析、对比诊断和迁移应用，具体目标按每个概念单独撰写。
5. 先修关系只指向目录中更早出现的主题，形成可用于课程排序的有向无环结构。

## 领域配额

| domain | 要求 | 实际 | 结果 |
|---|---:|---:|---|
| `phonology_writing` | 8 | 8 | 通过 |
| `morphology` | 10 | 10 | 通过 |
| `syntax` | 13 | 13 | 通过 |
| `semantics` | 8 | 8 | 通过 |
| `pragmatics_discourse` | 12 | 12 | 通过 |
| `sociolinguistics` | 5 | 5 | 通过 |
| `historical_grammaticalization` | 4 | 4 | 通过 |
| 合计 | 60 | 60 | 通过 |

## 逐条生成自检

下表只记录生成者自检，不能替代独立复核。每项均检查了概念边界、日语事实、先修关系、F1–F4 递进和原创例式的可执行性。

| # | topic_key | 本条重点核对 | 生成自检 |
|---:|---|---|---|
| 01 | `phw_mora_syllable` | 拍、音节、假名数三者不混同；拗音计拍明确 | 通过 |
| 02 | `phw_special_morae` | 拨音、促音、长音均占拍但语音实现不同 | 通过 |
| 03 | `phw_pitch_accent` | 以东京式下降位置为限定，不把音高重音写成强弱重读 | 通过 |
| 04 | `phw_vowel_devoicing` | 清化作为受环境制约的表层实现，不写成元音删除 | 通过 |
| 05 | `phw_rendaku` | 连浊为形态音系倾向，莱曼定律不被夸大为全覆盖规则 | 通过 |
| 06 | `phw_kana_orthography` | 两套假名功能与现代假名遣分开说明，助词固定拼写保留 | 通过 |
| 07 | `phw_script_choice` | 文字种选择同时覆盖词界、读者与语体，不把合法变体判错 | 通过 |
| 08 | `phw_kanji_reading_okurigana` | 读法按完整词判断，送假名与活用边界相连 | 通过 |
| 09 | `morph_word_morpheme_classes` | 词、语素、词类使用形态句法证据，不按中文翻译分类 | 通过 |
| 10 | `morph_verb_conjugation` | 五段、一段、不规则与音便边界清楚，避免「る」结尾误判 | 通过 |
| 11 | `morph_adjective_inflection` | い形容词和な形容词的定语、谓语及否定系统分别处理 | 通过 |
| 12 | `morph_copula` | 「だ／です／である」按系列和语体分析，不做任意片段替换 | 通过 |
| 13 | `morph_auxiliary_chain` | 助动成分的线性次序与语义作用域形成层级对应 | 通过 |
| 14 | `morph_negation_forms` | 动词、い形容词、名词性谓语的否定形态不交叉套用 | 通过 |
| 15 | `morph_tense_forms` | 非过去／过去是形态标签，不直接等同固定现实时间 | 通过 |
| 16 | `morph_derivation_nominalization` | 词汇派生与「の／こと」从句名词化明确分层 | 通过 |
| 17 | `morph_compounding` | 中心语、括号结构、语义透明度与连浊接口均有覆盖 | 通过 |
| 18 | `morph_transitivity_pairs` | 词汇性自他对应不简化为单一后缀，也不凭「を」单独分类 | 通过 |
| 19 | `syn_constituent_order` | 谓语居后是基本倾向，成分移动与逐词乱序严格区分 | 通过 |
| 20 | `syn_case_particles` | 格由谓语配价与构式共同许可，助词不做一词一译 | 通过 |
| 21 | `syn_topic_subject` | 主题和主格不等同，「は／が」解释依赖问答与语境 | 通过 |
| 22 | `syn_zero_arguments` | 零论元恢复同时核对配价、指代、视点、语态和敬语方向 | 通过 |
| 23 | `syn_valency_argument_structure` | 核心论元与附加语分开，表面省略不改变配价需求 | 通过 |
| 24 | `syn_existential_constructions` | 存在、所在、所有三类结构及有生性概念化边界清楚 | 通过 |
| 25 | `syn_relative_clauses` | 连体从句边界、内关系与外关系均纳入，不假设统一格空位 | 通过 |
| 26 | `syn_nominalized_clauses` | 「の／こと」选择与主句谓语、事件类型和视点关联 | 通过 |
| 27 | `syn_complement_quotation` | 直接、间接、命题补语和拟态内容按指示中心区分 | 通过 |
| 28 | `syn_te_clause_linking` | 顺序、并列、方式、原因读法由事件结构和语境决定 | 通过 |
| 29 | `syn_conditionals` | 「と・ば・たら・なら」写成重叠但受制约的系统，不绝对化 | 通过 |
| 30 | `syn_passive` | 直接、所有者、间接受影响被动及同形可能表达均有诊断 | 通过 |
| 31 | `syn_causative` | 致使者、被使役者和基础事件分层；命令／许可由语境判定 | 通过 |
| 32 | `sem_compositionality_polysemy` | 词义、结构义、多义和同音四者不以中文译词代替分析 | 通过 |
| 33 | `sem_tense_reference_time` | 事件时、说话时、基准时三点关系覆盖主句与相对时制 | 通过 |
| 34 | `sem_lexical_aspect` | 状态、活动、终结性作为谓语短语组合结果而非永久词标 | 通过 |
| 35 | `sem_teiru_readings` | 进行、结果状态、习惯、经验读法均由事件类型与语境核对 | 通过 |
| 36 | `sem_modality` | 动力、道义、认识情态与否定作用域分开 | 通过 |
| 37 | `sem_evidentiality` | 传闻「そうだ」、样态「そうだ」、推定形式按接续和证据区分 | 通过 |
| 38 | `sem_negation_scope` | 全否定、部分否定、情态内外范围可用事实条件检验 | 通过 |
| 39 | `sem_quantifier_scope` | 浮游量词的宿主、个体数量、事件次数及否定范围分别检查 | 通过 |
| 40 | `prag_context_implicature` | 字面内容、可撤销含意和语义蕴涵有明确测试 | 通过 |
| 41 | `prag_speech_acts` | 表面句式与提问、请求等行为力量分开，预期回应作为证据 | 通过 |
| 42 | `prag_deixis` | 人称、空间、时间、话语指示均锚定到明确指示中心 | 通过 |
| 43 | `prag_reference_anaphora` | 先行语不按最近名词机械选择，桥接关系单独标示 | 通过 |
| 44 | `prag_information_focus` | 背景、信息焦点、对比焦点由当前问题和替代集合确定 | 通过 |
| 45 | `prag_ellipsis_fragments` | 句法恢复内容与礼貌性含意严格分开，不擅自补否定 | 通过 |
| 46 | `prag_sentence_final_particles` | 「ね／よ」按知识分布和互动立场分析，不作固定翻译 | 通过 |
| 47 | `prag_politeness_honorifics` | 对听者礼貌、尊敬、谦让方向与事件角色逐一对应 | 通过 |
| 48 | `prag_register_style` | 语域按受众、媒介、任务整体调整，不只替换句尾 | 通过 |
| 49 | `prag_turn_taking_backchannels` | 附和的倾听、接收和同意功能按位置及序列区分 | 通过 |
| 50 | `prag_connectives_coherence` | 因果方向、转折、续接、话题转换及推论层级明确 | 通过 |
| 51 | `prag_viewpoint_empathy` | 授受、移动、语态、零论元围绕同一视点中心核对 | 通过 |
| 52 | `soc_standard_regional_variation` | 标准化与结构优劣分开，方言不被错误化或娱乐化 | 通过 |
| 53 | `soc_gender_age_indexing` | 性别年龄关联只写概率性索引，禁止由单一形式断定身份 | 通过 |
| 54 | `soc_style_shifting_community` | 个体内风格变化结合受众、任务与实践共同体解释 | 通过 |
| 55 | `soc_uchi_soto_power` | 内外边界、制度权力和亲疏作为可变且可能竞争的维度 | 通过 |
| 56 | `soc_contact_borrowing` | 音系适配、形态整合、语义变化与临时转写地位分开 | 通过 |
| 57 | `hist_classical_modern_grammar` | 古典与现代范式按时代标注，不用影视古风角色语作史料 | 通过 |
| 58 | `hist_sound_change_orthography` | 历史音变要求记录链，不从现代相似音捏造同源关系 | 通过 |
| 59 | `hist_grammaticalization_auxiliaries` | 实义与辅助义可共存，语法化证据不简化为形式变短 | 通过 |
| 60 | `hist_lexicalization_construction_change` | 词汇化、重新分析、能产性扩展和替代假说分别说明 | 通过 |

## 机器校验结果

机器命令只解析和核验已人工创作的内容，没有生成或改写任何教学文本。

| 检查项 | 结果 |
|---|---:|
| JSON 可解析 | 通过 |
| `topic_count` 与数组长度 | 60 / 60 |
| 唯一 `topic_key` | 60 / 60 |
| `topic_key` 命名规则 | 0 个异常 |
| 领域配额 | 8 / 10 / 13 / 8 / 12 / 5 / 4 |
| 必填字段缺失 | 0 |
| F1–F4 缺失 | 0 |
| 原创例式三字段缺失 | 0 |
| 不存在的先修引用 | 0 |
| 指向后置主题的先修引用 | 0 |
| 核心文本字段精确重复 | 0 |
| 少于 3 个标签的主题 | 0 |
| 动漫作品名或字幕原文命中 | 0 |

## 交接给独立复核者

独立复核者应逐条检查：

1. 中日名称是否为该概念的恰当术语，且没有把教学语法和理论术语错误混合。
2. 入门解释是否可懂，深入解释是否准确，提醒项是否覆盖最常见误判。
3. 先修关系是否足以支撑 F2–F4，是否存在应拆分或合并的主题。
4. F1–F4 是否真正递进，而非仅改变措辞。
5. 原创例式规范是否能产出不依赖作品语料、人物身份或隐藏剧情的题目。
6. 历史语言学条目在进入题目生成前，真实实例和年代必须再由权威资料核查；本目录目前只规定概念和例式边界。

未完成不同 Agent 的独立复核前，本生成文件不得作为 approved 数据写入数据库。
