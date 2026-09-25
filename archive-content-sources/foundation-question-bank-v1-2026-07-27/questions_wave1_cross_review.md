# 基础语言学 V1 Wave 1：独立逐题复核

- 生成文件：`archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave1_generation.json`
- Review overlay：`archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave1_review_overlay.json`
- 生成 QA：`archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave1_generation_qa.md`
- 主题契约：`archive-content-sources/foundation-question-bank-v1-2026-07-27/topics_v1_effective.json`
- 生成者：`/root/foundation_schema_v1`
- 独立复核者：`/root/audit_foundation_wave2_publish`
- 生成文件 raw SHA-256：`de56068d9be648ae0f83dbd74b31e8b24b9053f620290a5f56ec03929a44a9f6`
- 日期：2026-07-27
- 数据库：未写入

## 复核结论

| 检查项 | 结果 |
|---|---:|
| 基础题 | 80 |
| 唯一题目 ID | 80 |
| 覆盖主题 | 20 |
| F1 / F2 / F3 / F4 | 20 / 20 / 20 / 20 |
| keep / revise / reject | 77 / 3 / 0 |
| 完整同 ID replacements | 3 |
| Overlay 总体质量分 | 96 |
| 单题最低质量分 | 97 |
| 正确答案 A / B / C / D | 20 / 20 / 20 / 20 |
| Validator errors / warnings | 0 / 0 |

逐题复核覆盖了适用的日语音系与形态音系、拍与音节、词汇重音、元音清化、连浊、文字与正字法、形态切分、词类、活用、派生、复合、自动／他动、句法成分、格关系、语义、语用、说话人、指代、否定、时制、体、情态、语态和上下文。题包全部为独立的元语言基础题，不依赖动漫字幕；复核同时确认题干没有暗中要求不存在的跨字幕信息。所有 `revise` 均在 overlay 中提供完整同 ID 题目，按 ID 替换而非追加。

## 三项修订

1. `foundation-v1-phw_vowel_devoicing-f1`
   - 原题以「くさ」作为典型清化候选，但东京式词汇重音可能干扰目标元音是否清化，题面没有排除该因素。
   - 改为首拍不承载重音的「くつ」，目标 /u/ 位于清辅音 /k/ 与 /ts/ 之间；仍明确清化受语速、重音、说话人和地域影响，不把倾向写成绝对规则。
2. `foundation-v1-morph_auxiliary_chain-f1`
   - 原题只给孤立形式「読ませられなかった」，却把「られ」无条件分析为被动；孤立形式也可能允许使役谓语的可能读法。
   - 现明确指定“当时没有被要求阅读”的使役被动读法，再考查使役、被动、否定、过去的外向层级；边界说明保留孤立形式的可能歧义。
3. `foundation-v1-morph_auxiliary_chain-f4`
   - 原「使われていなかった」容易优先解释为“当时没有在使用”，不足以唯一诊断结果状态。
   - 改为完成点明确的「この装置はまだ設置されていなかった」，由「設置する」、检查时基准和「まだ」共同支持被动＋结果状态＋否定过去。

## 80 题逐条台账

| # | question_id | 决定 | 分数 | 独立复核结论 |
|---:|---|:---:|---:|---|
| 01 | `foundation-v1-phw_mora_syllable-f1` | keep | 98 | 「きゃ」作为拗音占一拍，「く」另占一拍；未把字符数、词数与拍数混同。 |
| 02 | `foundation-v1-phw_mora_syllable-f2` | keep | 97 | 在题面声明的分析体系中，拨音作前一音节特殊拍，四拍、两音节结论一致。 |
| 03 | `foundation-v1-phw_mora_syllable-f3` | keep | 98 | 「きょ」不拆拍，「う」作为长音拍增加一拍，两项差异的诊断唯一。 |
| 04 | `foundation-v1-phw_mora_syllable-f4` | keep | 98 | 「りゅ｜う｜が｜く」保留拗音单位与长音拍，朗读提示和四拍计数准确。 |
| 05 | `foundation-v1-phw_special_morae-f1` | keep | 99 | 促音均占一拍，并按后续塞音／摩擦音区分闭塞或摩擦时长。 |
| 06 | `foundation-v1-phw_special_morae-f2` | keep | 98 | /p/ 前拨音的双唇同化、三拍结构与规范拼写三个层次区分清楚。 |
| 07 | `foundation-v1-phw_special_morae-f3` | keep | 98 | 「かこ／かっこ」以促音拍形成时长和词形对立，未用词义代替语音证据。 |
| 08 | `foundation-v1-phw_special_morae-f4` | keep | 98 | 「コ｜ー｜チ」三拍与长音符功能说明准确，未机械改写片假名长音。 |
| 09 | `foundation-v1-phw_pitch_accent-f1` | keep | 98 | 在东京式抽象标注下，第二拍后的下降对应第二拍重音核，不等同于响度。 |
| 10 | `foundation-v1-phw_pitch_accent-f2` | keep | 98 | 三拍词至后接助词均无下降支持平板型，助词高音被正确用于诊断。 |
| 11 | `foundation-v1-phw_pitch_accent-f3` | keep | 98 | 音高下降与单纯响度突出被放在同一比较层级，甲才实现目标有核轮廓。 |
| 12 | `foundation-v1-phw_pitch_accent-f4` | keep | 98 | 头高型第一拍后立即下降，后续拍及助词保持低音，其他选项对应不同核型。 |
| 13 | `foundation-v1-phw_vowel_devoicing-f1` | revise | 98 | 改用首拍非重音的「くつ」，排除原「くさ」的词汇重音干扰并保留概率性限制。 |
| 14 | `foundation-v1-phw_vowel_devoicing-f2` | keep | 98 | 句末「です」的 /u/ 清化只改变语音实现，不删除系词、拍位、礼貌体或拼写。 |
| 15 | `foundation-v1-phw_vowel_devoicing-f3` | keep | 97 | 同一「きく」在自然语速与纠正性慢读中的差异可由语速和突出度解释。 |
| 16 | `foundation-v1-phw_vowel_devoicing-f4` | keep | 98 | 「すこし」首拍 /u/ 的局部清辅音环境判断准确，并保留重音、边界等变异因素。 |
| 17 | `foundation-v1-phw_rendaku-f1` | keep | 98 | 「ほん＋たな→ほんだな」将连浊定位于后部词基词首 /t/→/d/。 |
| 18 | `foundation-v1-phw_rendaku-f2` | keep | 97 | 给定「て＋かみ」后，复合中的 /k/→/g/ 与整体词汇化说明相容。 |
| 19 | `foundation-v1-phw_rendaku-f3` | keep | 98 | 后部已有浊阻音作为莱曼定律类阻断线索，而非被误写成绝对算法。 |
| 20 | `foundation-v1-phw_rendaku-f4` | keep | 98 | 后部「ふくろ」词首 /f/→/b/ 的预测有规则依据，也明确要求词典核对。 |
| 21 | `foundation-v1-phw_kana_orthography-f1` | keep | 98 | 主题助词「は」与方向助词「へ」的固定拼写、读音和词内同形边界准确。 |
| 22 | `foundation-v1-phw_kana_orthography-f2` | keep | 98 | 借词、格助词与动词礼貌过去形被正确区分，文字种未被当作句法角色。 |
| 23 | `foundation-v1-phw_kana_orthography-f3` | keep | 98 | 中性正字法保留长音、促音和助词拼写，特殊表音文体未被误判为规范形式。 |
| 24 | `foundation-v1-phw_kana_orthography-f4` | keep | 97 | 片假名借词、平假名普通词和方向助词「へ」组合自然，也承认汉字替代方案。 |
| 25 | `foundation-v1-phw_script_choice-f1` | keep | 98 | 汉字词汇部分与假名送假名、助词、礼貌成分的分工准确且非绝对化。 |
| 26 | `foundation-v1-phw_script_choice-f2` | keep | 98 | 汉字版与假名版的命题、活用和句法一致，差异限于可读性、柔和度和受众。 |
| 27 | `foundation-v1-phw_script_choice-f3` | keep | 98 | 「柔らかい／やわらかい」均可自然修饰「布」，未由文字种虚构义项分工。 |
| 28 | `foundation-v1-phw_script_choice-f4` | keep | 97 | 儿童向假名提示的否定请求、现场指示和礼貌度一致，未声称是唯一标识形式。 |
| 29 | `foundation-v1-phw_kanji_reading_okurigana-f1` | keep | 98 | 「書」取训读「か」，送假名为「いた」；未混淆形态切分与正字法边界。 |
| 30 | `foundation-v1-phw_kanji_reading_okurigana-f2` | keep | 98 | 「高＋く＋なかっ＋た」保留词干、连接、否定和过去层级。 |
| 31 | `foundation-v1-phw_kanji_reading_okurigana-f3` | keep | 98 | 「行く→行った」的特殊促音便与送假名「った」准确，且未过度推广。 |
| 32 | `foundation-v1-phw_kanji_reading_okurigana-f4` | keep | 98 | 指定规则一段动词后，「深めた」及送假名「めた」均无音便或词干错误。 |
| 33 | `foundation-v1-morph_word_morpheme_classes-f1` | keep | 98 | い形容词、名词、格助词和动词由活用与分布诊断，不依赖中文词性。 |
| 34 | `foundation-v1-morph_word_morpheme_classes-f2` | keep | 98 | 「小さ＋さ」正确区分词基内「さ」与派生名词化成分，整体名词分布成立。 |
| 35 | `foundation-v1-morph_word_morpheme_classes-f3` | keep | 98 | 「必要」与五段动词「要る」通过系词、否定和过去范式清楚区分。 |
| 36 | `foundation-v1-morph_word_morpheme_classes-f4` | keep | 97 | 「読み＋手」具有替换、组合意义和整体名词分布证据，也保留能产性限制。 |
| 37 | `foundation-v1-morph_verb_conjugation-f1` | keep | 98 | 五段 /g/ 结尾「泳いで」与サ变「確認して」的范式和词汇核心正确。 |
| 38 | `foundation-v1-morph_verb_conjugation-f2` | keep | 98 | 「急いだ」正确还原为「急ぐ」，/g/ 结尾た形使用「いだ」。 |
| 39 | `foundation-v1-morph_verb_conjugation-f3` | keep | 98 | 「見る」一段与「切る」五段虽同以「る」结尾，て形诊断仍清楚。 |
| 40 | `foundation-v1-morph_verb_conjugation-f4` | keep | 98 | 「運ばない／運んだ」展示五段 a 行和鼻音便，可能形干扰项被正确排除。 |
| 41 | `foundation-v1-morph_adjective_inflection-f1` | keep | 98 | 「静か」作な形容词词干，连体位置用「な」，地点格由后续成分承担。 |
| 42 | `foundation-v1-morph_adjective_inflection-f2` | keep | 98 | い形容词连接、否定、过去逐层组合，时态和否定作用域无错置。 |
| 43 | `foundation-v1-morph_adjective_inflection-f3` | keep | 98 | 「きれい」表面末尾「い」未被当作词类充分条件，「きれいな水」唯一自然。 |
| 44 | `foundation-v1-morph_adjective_inflection-f4` | keep | 98 | 「便利な」与「便利ではありませんでした」属于同一な形容词／系词系列。 |
| 45 | `foundation-v1-morph_copula-f1` | keep | 98 | 「でした」是名词性判断的礼貌过去系词，不是地点格「で」或进行标记。 |
| 46 | `foundation-v1-morph_copula-f2` | keep | 97 | 「休館＋で＋は＋なかっ＋た」保留名词谓语、否定和过去层级。 |
| 47 | `foundation-v1-morph_copula-f3` | keep | 98 | 「だ／です／である」命题内容一致，语体差异被写成倾向而非禁令。 |
| 48 | `foundation-v1-morph_copula-f4` | keep | 98 | 三种系词形式依次满足礼貌肯定非过去、否定非过去和肯定过去。 |
| 49 | `foundation-v1-morph_auxiliary_chain-f1` | revise | 98 | 明确指定使役被动读法后再考层级，消除孤立「られ」可能读法的歧义。 |
| 50 | `foundation-v1-morph_auxiliary_chain-f2` | keep | 98 | 钥匙损坏、箱子对象和尝试者共同消歧「開けられ」为可能，外层否定过去准确。 |
| 51 | `foundation-v1-morph_auxiliary_chain-f3` | keep | 97 | 题面明确目标为否定使役被动整体，也保留孤立形式的可能歧义警告。 |
| 52 | `foundation-v1-morph_auxiliary_chain-f4` | revise | 99 | 改为完成点明确的「まだ設置されていなかった」，稳定诊断结果状态。 |
| 53 | `foundation-v1-morph_negation_forms-f1` | keep | 98 | 动词、い形容词和名词性谓语分别匹配现代普通否定形式。 |
| 54 | `foundation-v1-morph_negation_forms-f2` | keep | 98 | 「静かではありませんでした」准确实现礼貌否定过去，否定未错落在主题名词。 |
| 55 | `foundation-v1-morph_negation_forms-f3` | keep | 97 | 简单性质句用「おもしろくない」，同时保留引用／元语言「Xではない」的边界。 |
| 56 | `foundation-v1-morph_negation_forms-f4` | keep | 98 | 名词「休み」的系词否定过去与动词「休む」的否定过去被严格区分。 |
| 57 | `foundation-v1-morph_tense_forms-f1` | keep | 98 | 「歩く」形态为非过去，「毎朝」建立习惯框架，未误解为进行或未来专用。 |
| 58 | `foundation-v1-morph_tense_forms-f2` | keep | 98 | 「来週」把自动变化事件定位于未来，非过去形与计划解释相容。 |
| 59 | `foundation-v1-morph_tense_forms-f3` | keep | 98 | 相同非过去形由「毎日／明日」分别获得习惯和未来解释。 |
| 60 | `foundation-v1-morph_tense_forms-f4` | keep | 98 | 叙事发现构式「ドアを開けると、猫がいた」的前后时间关系自然。 |
| 61 | `foundation-v1-morph_derivation_nominalization-f1` | keep | 98 | 「再＋利用」透明表达重复并形成名词／サ变词干，能产性未过度推广。 |
| 62 | `foundation-v1-morph_derivation_nominalization-f2` | keep | 98 | 「静か＋さ」形成可带格助词的名词，派生词类及性质／程度语义准确。 |
| 63 | `foundation-v1-morph_derivation_nominalization-f3` | keep | 98 | 词汇名词「休み」与保留谓语小句结构的「休むこと」层级不同。 |
| 64 | `foundation-v1-morph_derivation_nominalization-f4` | keep | 98 | 「細かい→細かさ」形态规则正确，并明确合法性不保证所有搭配同等自然。 |
| 65 | `foundation-v1-morph_compounding-f1` | keep | 98 | 「木＋箱」以后部为名词中心、前部给材料关系，后部中心倾向未绝对化。 |
| 66 | `foundation-v1-morph_compounding-f2` | keep | 97 | 「木箱」先成完整被放物，再与「置き場」组合，括号与释义一致。 |
| 67 | `foundation-v1-morph_compounding-f3` | keep | 97 | 「紙」限制「容器」而非最外层箱子，材料范围和目标括号一致。 |
| 68 | `foundation-v1-morph_compounding-f4` | keep | 97 | 「予備鍵保管箱」仅作结构透明候选，并明确要求核对真实通行度。 |
| 69 | `foundation-v1-morph_transitivity_pairs-f1` | keep | 98 | 自动词「開く」与他动词「開ける」的参与者和格框架映射正确。 |
| 70 | `foundation-v1-morph_transitivity_pairs-f2` | keep | 98 | 「消える」只断言变化，「消す」增加施事，未从自动句虚构特定致使者。 |
| 71 | `foundation-v1-morph_transitivity_pairs-f3` | keep | 98 | 他动「閉める」与使役「閉めさせる」的参与者层级准确，强制／许可留给语境。 |
| 72 | `foundation-v1-morph_transitivity_pairs-f4` | keep | 97 | 「風でドアが開いた」前景化结果并表达非意志原因，也承认其他视角构式。 |
| 73 | `foundation-v1-syn_constituent_order-f1` | keep | 98 | 带助词名词短语与句末谓语划分正确，未把词内活用当主句法边界。 |
| 74 | `foundation-v1-syn_constituent_order-f2` | keep | 98 | 两个修饰语先在名词短语内部组合，再由「を／で」接入小句，无跨挂。 |
| 75 | `foundation-v1-syn_constituent_order-f3` | keep | 97 | 乙完整前置宾语但依赖话语语境，丙拆散定语短语；语法性与中性度分开。 |
| 76 | `foundation-v1-syn_constituent_order-f4` | keep | 98 | A 保持三个格成分完整并以有限谓语收尾，未宣称顺序唯一。 |
| 77 | `foundation-v1-syn_case_particles-f1` | keep | 98 | 放置者、被放物和结果位置分别由「が／を／に」实现。 |
| 78 | `foundation-v1-syn_case_particles-f2` | keep | 97 | 在“查验词典”义下，施事、对象和活动地点的格标记准确。 |
| 79 | `foundation-v1-syn_case_particles-f3` | keep | 98 | 「公園で」活动场所与「公園を」穿行路径区分准确，路径「を」不改变及物性。 |
| 80 | `foundation-v1-syn_case_particles-f4` | keep | 98 | 施事、起点、工具、受事和方向目标依次由「が／から／で／を／へ」标记。 |

## Validator

运行：

```text
node scripts/validate-foundation-content.mjs pack archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave1_generation.json archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave1_review_overlay.json
```

结果：

- `rows`: 80
- `uniqueIds`: 80
- `topicCount`: 20
- `replacementsApplied`: 3
- `answerCounts`: A=20、B=20、C=20、D=20
- `generatorAgent`: `/root/foundation_schema_v1`
- `reviewerAgent`: `/root/audit_foundation_wave2_publish`
- `qualityScore`: 96
- `errors`: 0
- `warnings`: 0
- generation canonical SHA-256：`2949003b514edde3e33e41a15f8664516dca6de8c639cb61a4e7882936ac2478`
- effective canonical SHA-256：`34072c6871073f53db5946c8f775b7077f8ad1e5301140b22c32e2f619e2a93f`

本复核未写数据库。只有在后续传输守卫再次确认生成 raw SHA、80 个唯一 ID、完整同 ID replacement、无 reject、所有单题不低于 95、主题处于有效课程范围、数据库无历史重叠且现有行无漂移后，题包才可进入发布事务。
