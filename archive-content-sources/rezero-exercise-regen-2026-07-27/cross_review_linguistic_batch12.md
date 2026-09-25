# Linguistic Batch12 独立复核

## 结论

本批 80 条已由未参与生成的 reviewer 逐条独立复核，覆盖 40 条 vocab、20 条 grammar、20 条 sentence。复核以 source 记录、vocab occurrence、当前日文字幕和连续话轮为准；生成侧 QA 只用于定位，不作为正确性结论。逐条检查了日语形态、句法、语义、语用、说话人、指代、否定、时态、语态及跨字幕上下文。

- 保留：70 条
- 修订：10 条
- review overlay：10 条、唯一 ID 10，均按 ID 完整替换原候选，不追加
- 数据库写入：0

主要修订包括两处说话人误判（「消耗戦」「確率」）、一处句末「と」跨字幕关系误判、一处把“听取条件”扩大为“听从条件”、一处缩约中「の」数量漏算，以及一处虚构“带走调查”目的。另对「貴重品」「鑑定」「紙一重」「ことになる」的术语、受益方、指代和非意志性作了收紧。

## 逐条复核台账

| # | enrichment_id | source_type | 结论 | 独立复核结论 |
| ---: | --- | --- | --- | --- |
| 01 | `vocab-card-v1:vocab-re-zero-shitto-no-majo` | vocab | 保留 | 艾米莉雅是在抗议禁忌称呼，并非承认魔女身份；说话人与否定性语用均正确。 |
| 02 | `vocab-card-v1:re-zero-vocab-趣味` | vocab | 保留 | 「趣味が悪い」取“品味／做法恶劣”，批评对象是昴的称呼行为，不是业余爱好。 |
| 03 | `vocab-card-v1:re-zero-vocab-暇` | vocab | 保留 | 「暇じゃない」只说明当下无暇继续交谈，没有误造过去时或“以前很闲”。 |
| 04 | `vocab-card-v1:re-zero-vocab-能力` | vocab | 保留 | 触发主体与死亡条件交代准确；「能力」没有被误写成可主动发动的技能。 |
| 05 | `vocab-card-v1:re-zero-vocab-恩義` | vocab | 保留 | 否定结果需跨到下一行读取；本轮尚未受救与“恩义消失”的时间线关系正确。 |
| 06 | `vocab-card-v1:re-zero-vocab-無関係` | vocab | 保留 | 「無関係の役立たず」是昴的自嘲，修饰对象与艾米莉雅无关这一歧义已排除。 |
| 07 | `vocab-card-v1:re-zero-vocab-自己満足` | vocab | 保留 | 自我满足的行动者与所有格均指昴，没有把评价错安到艾米莉雅身上。 |
| 08 | `vocab-card-v1:re-zero-vocab-繰り返す` | vocab | 保留 | 「何度～ても」的让步范围仅指循环中的关键事件，没有扩大成世界绝对不变。 |
| 09 | `vocab-card-v1:vocab-re-zero-misugosu` | vocab | 保留 | 「見過ごす」在已知死亡后果的语境是坐视不管，不是无意漏看。 |
| 10 | `vocab-card-v1:re-zero-vocab-質問` | vocab | 保留 | 「とか」承担不满式举例／提示，店主讽刺“只问不买”的语用判断准确。 |
| 11 | `vocab-card-v1:re-zero-vocab-阻止` | vocab | 保留 | 「阻止すれば」是未实现的条件设想；前句「出遅れた」已否定成功阻止。 |
| 12 | `vocab-card-v1:re-zero-vocab-困る` | vocab | 保留 | 「困ったこと」是眼下面临的麻烦事，形式过去不等于事件已经结束。 |
| 13 | `vocab-card-v1:re-zero-vocab-届ける` | vocab | 保留 | 找到并送还徽章的行动者是昴；莱因哈鲁特仅负责转达保证，施受关系正确。 |
| 14 | `vocab-card-v1:re-zero-vocab-怒る` | vocab | 保留 | 「怒ってもいる」描述菲鲁特持续对艾尔莎生气，不是正在出声训斥。 |
| 15 | `vocab-card-v1:re-zero-vocab-隠す` | vocab | 保留 | 条件形没有被误写成菲鲁特已经隐藏敌意，艾尔莎的嘲弄语气也已保留。 |
| 16 | `vocab-card-v1:vocab-re-zero-jiman` | vocab | 保留 | 「自慢か」是昴对偷窃习惯的讽刺反问，不是真诚询问特长。 |
| 17 | `vocab-card-v1:re-zero-vocab-用意` | vocab | 保留 | 「用意してる」表示手机已经备妥、可供交易，不只是正在准备。 |
| 18 | `vocab-card-v1:re-zero-vocab-貴重品` | vocab | 修订 | 将内部术语“说话人承诺”改为“说话人主张”；昴是在议价中断言稀缺性，并未作出承诺。 |
| 19 | `vocab-card-v1:re-zero-vocab-鑑定` | vocab | 修订 | 罗姆爷是鉴定执行者；省略的受益／委托方不应硬限为菲鲁特一方，公平性服务双方交易。 |
| 20 | `vocab-card-v1:re-zero-vocab-焦る` | vocab | 保留 | 发问者菲鲁特、焦急者昴及「焦ってん」的持续状态与说明语气均正确。 |
| 21 | `vocab-card-v1:re-zero-vocab-目標` | vocab | 保留 | 只确认菲鲁特想改善未来处境，没有虚构目标的金额、职业或其他精确内容。 |
| 22 | `vocab-card-v1:re-zero-vocab-注意` | vocab | 保留 | 「扱いには注意」是操作手机时的省略式警告，不是“注意到线索”。 |
| 23 | `vocab-card-v1:re-zero-vocab-危うい` | vocab | 保留 | 昴针对菲鲁特当下想法发出危险警告，没有扩大成对人格的永久判断。 |
| 24 | `vocab-card-v1:re-zero-vocab-明るい` | vocab | 保留 | 「明るい時間」指天色尚亮，并以循环记忆形成“艾尔莎来得更早”的反预期。 |
| 25 | `vocab-card-v1:re-zero-vocab-負ける` | vocab | 保留 | 「負け」是「負けを認める」中省略助词的宾语，不是命令形或终止谓语。 |
| 26 | `vocab-card-v1:re-zero-vocab-急に` | vocab | 保留 | 「急に」修饰态度突然变化，不是催促“赶紧”；艾米莉雅缺少循环记忆的视角正确。 |
| 27 | `vocab-card-v1:re-zero-vocab-親身` | vocab | 保留 | 行动者昴、受益者艾米莉雅及「くれる」的授受方向均未倒置。 |
| 28 | `vocab-card-v1:re-zero-vocab-釈然` | vocab | 保留 | 固定否定「釈然としない」正确解释为不释然，没有误译成如释重负。 |
| 29 | `vocab-card-v1:re-zero-vocab-防ぐ` | vocab | 保留 | 艾米莉雅点名帕克并下达即时防御命令；省略宾语由眼前攻击恢复。 |
| 30 | `vocab-card-v1:re-zero-vocab-紙一重` | vocab | 修订 | 收紧为防御时机险险赶上；「助かったよ」不支持扩大成“众人都侥幸得救”。 |
| 31 | `vocab-card-v1:re-zero-vocab-達者` | vocab | 保留 | 「口ばかり達者」是只会嘴上逞能，不是身体健康义。 |
| 32 | `vocab-card-v1:vocab-re-zero-hinmingai` | vocab | 保留 | 正确标明艾尔莎借「しょせん」与出身标签贬低菲鲁特，未把偏见当旁白事实。 |
| 33 | `vocab-card-v1:re-zero-vocab-正義感` | vocab | 保留 | 「思わぬ」保留昴连自己也意外的正义冲动，自嘲语气与说话人均准确。 |
| 34 | `vocab-card-v1:re-zero-vocab-自己紹介` | vocab | 保留 | 帕克的自报姓名与随后死亡威胁连读，战斗中的黑色幽默没有被友好化。 |
| 35 | `vocab-card-v1:re-zero-vocab-慣れる` | vocab | 保留 | 「戦い慣れしてる」是长期实战习惯；昴的性别刻板预期未被包装成客观事实。 |
| 36 | `vocab-card-v1:re-zero-vocab-消耗戦` | vocab | 修订 | 提案者应为昴；罗姆爷在下一行以精灵显现时限反驳，原稿把两人及帕克的话轮串错。 |
| 37 | `vocab-card-v1:re-zero-vocab-無目的` | vocab | 保留 | 「わけじゃない」完整否定“毫无目的”，帕克是在暗示散冰另有战术用途。 |
| 38 | `vocab-card-v1:re-zero-vocab-感心` | vocab | 保留 | 「感心しない」是帕克对残酷行为的不赞成，不只是技术评价。 |
| 39 | `vocab-card-v1:re-zero-vocab-眠い` | vocab | 保留 | 体验者是帕克，困倦需跨行解释为魔力耗尽、即将消失的征兆。 |
| 40 | `vocab-card-v1:re-zero-vocab-確率` | vocab | 修订 | 连续发言者是昴而非罗姆爷；昴比较年龄后说明为何让菲鲁特选择存活率最高的逃生方案。 |
| 41 | `grammar-card-v1:re-zero-s01e04-grammar-050` | grammar | 保留 | 「てから」的先后顺序、两个前置条件及句末「だからね」的提醒作用均准确。 |
| 42 | `grammar-card-v1:re-zero-s01e05-grammar-003` | grammar | 修订 | 主分析正确；将矛盾术语“结果性决定”改为“非意志结果”，与「ことになる」无主动决策者一致。 |
| 43 | `grammar-card-v1:re-zero-s01e05-grammar-020` | grammar | 保留 | 「つかまっていた」缩约及「に」的施事／来源角色正确，句中不存在授受补助。 |
| 44 | `grammar-card-v1:re-zero-s01e05-grammar-022` | grammar | 保留 | 正确切分为「感謝し＋足りない」，没有把连读误判成列举「たり」。 |
| 45 | `grammar-card-v1:re-zero-s01e05-grammar-024` | grammar | 保留 | 「じゃねえか」是否定外形的肯定确认，符合昴看到房间的惊喜语气。 |
| 46 | `grammar-card-v1:re-zero-s01e05-grammar-028` | grammar | 保留 | 「てやる」在独白中强化挑战性决意；约定内容与时间线指代均正确。 |
| 47 | `grammar-card-v1:re-zero-s01e05-grammar-046` | grammar | 保留 | 「かねる」表示基于判断难以理解，不是尚未习得的能力不足。 |
| 48 | `grammar-card-v1:re-zero-s01e05-grammar-047` | grammar | 修订 | 句末「と」标雷姆所想内容，省略认知／注视谓语；下一行拉姆的「だそうよ」是独立转述。 |
| 49 | `grammar-card-v1:re-zero-s01e05-grammar-048` | grammar | 保留 | 两个「嫌」分别是条件谓语与被引用拒绝词，功能和作用域没有混淆。 |
| 50 | `grammar-card-v1:re-zero-s01e05-grammar-049` | grammar | 保留 | 被动施受方向、愿望否定范围及未尽「けど」均正确。 |
| 51 | `grammar-card-v1:re-zero-s01e05-grammar-050` | grammar | 保留 | 「と思い」以连用中止连接下一行道歉，发言者与所指事件准确。 |
| 52 | `grammar-card-v1:re-zero-s01e05-grammar-051` | grammar | 保留 | 「同僚といっても」先承认分类再限制其推论，昴与雷姆的身份关系正确。 |
| 53 | `grammar-card-v1:re-zero-s01e05-grammar-052` | grammar | 保留 | 前件行动者雷姆、后件决定者昴及主动「ことにする」均准确。 |
| 54 | `grammar-card-v1:re-zero-s01e05-grammar-053` | grammar | 修订 | 「お聞きします」只承诺郑重听取未知条件；动词本身不表示已同意听从或无条件执行。 |
| 55 | `grammar-card-v1:re-zero-s01e05-grammar-054` | grammar | 保留 | 两层否定、预防目的和「ないと」省略义务后件均准确。 |
| 56 | `grammar-card-v1:re-zero-s01e05-grammar-055` | grammar | 保留 | 「着々と」的谓语省略及「答えておこう」的暂定作答语气均正确。 |
| 57 | `grammar-card-v1:re-zero-s01e05-grammar-056` | grammar | 保留 | 使役对象、跨行主句及雷姆对拉姆意图的主观归因均准确。 |
| 58 | `grammar-card-v1:re-zero-s01e05-grammar-057` | grammar | 修订 | 补全「今のところの」中的两处属格「の」；连同「なるの」共涉及三个不同边界。 |
| 59 | `grammar-card-v1:re-zero-s01e05-grammar-058` | grammar | 保留 | 「聞いてくれる？」是提出约会前的预请求，不等于艾米莉雅已经同意。 |
| 60 | `grammar-card-v1:re-zero-s01e05-grammar-059` | grammar | 保留 | 非本意结果、不确定情态、受影响者和未来指向均准确。 |
| 61 | `sentence-card-v1:re-zero-s01e03-sentence-035` | sentence | 保留 | 礼貌劝降、句末悬置及持剑语境中的实际强制性均正确。 |
| 62 | `sentence-card-v1:re-zero-s01e03-sentence-037` | sentence | 保留 | 可能否定描述剑的既定机制，不是莱因哈鲁特临时拒绝拔剑。 |
| 63 | `sentence-card-v1:re-zero-s01e03-sentence-039` | sentence | 保留 | 「てほしい」的施事、信息宾语及重置后的关系语境均准确。 |
| 64 | `sentence-card-v1:re-zero-s01e03-sentence-041` | sentence | 保留 | 后置感谢理由、救助施受方向和说话人均正确。 |
| 65 | `sentence-card-v1:re-zero-s01e03-sentence-042` | sentence | 保留 | 跨行成本—回报框架和自嘲语用准确，没有误写成后悔救人。 |
| 66 | `sentence-card-v1:re-zero-s01e03-sentence-043` | sentence | 保留 | 治疗操作完成与完全康复严格区分，时态和结果状态均正确。 |
| 67 | `sentence-card-v1:re-zero-s01e03-sentence-044` | sentence | 保留 | 「はず」只反映艾米莉雅当前记忆，循环造成的双方记忆不对称处理正确。 |
| 68 | `sentence-card-v1:re-zero-s01e03-sentence-045` | sentence | 保留 | 礼貌条件、负责处理者及可被拒绝的安置方案均准确。 |
| 69 | `sentence-card-v1:re-zero-s01e03-sentence-046` | sentence | 保留 | 正确区分职责上的规范性不能与休班状态下的现场通融。 |
| 70 | `sentence-card-v1:re-zero-s01e03-sentence-047` | sentence | 保留 | 菲鲁特的受恩视角、道德性不能及后续归还行为均准确。 |
| 71 | `sentence-card-v1:re-zero-s01e03-sentence-049` | sentence | 修订 | 强制随行／带离性质成立，但本段没有说透具体处置目的，删除虚构的“带走调查”。 |
| 72 | `sentence-card-v1:re-zero-s01e03-sentence-050` | sentence | 保留 | 拒绝权的话题化、当下强制及菲鲁特并非表面语法主语均准确。 |
| 73 | `sentence-card-v1:re-zero-s01e04-sentence-018` | sentence | 保留 | 「なんて」谦抑自身付出，并重构为互惠关系，发言目的准确。 |
| 74 | `sentence-card-v1:re-zero-s01e04-sentence-025` | sentence | 保留 | 返回者、完成时态与现实中存在的敬语重叠均解释准确。 |
| 75 | `sentence-card-v1:re-zero-s01e04-sentence-033` | sentence | 保留 | 机构被动、连用形跨字幕和临时治理背景均准确。 |
| 76 | `sentence-card-v1:re-zero-s01e04-sentence-035` | sentence | 保留 | 正确区分 source 的常规重排与真实字幕中的右置补充。 |
| 77 | `sentence-card-v1:re-zero-s01e04-sentence-036` | sentence | 保留 | 王位候选资格、集合成员关系和零断定均准确。 |
| 78 | `sentence-card-v1:re-zero-s01e04-sentence-042` | sentence | 保留 | 「ここまで」的话语范围、谓语省略和流程切换均准确。 |
| 79 | `sentence-card-v1:re-zero-s01e04-sentence-045` | sentence | 保留 | 「その」回指间谍假设，“可能性很低”未被误写成零，判断主体准确。 |
| 80 | `sentence-card-v1:re-zero-s01e04-sentence-046` | sentence | 保留 | 「お世辞でも」是假设性最低情形，不是艾米莉雅断定昴撒谎。 |

## 合并与机器校验

按 ID 将 review overlay 覆盖三份冻结候选后：

- effective rows：80
- unique IDs：80
- unique `source_type + source_id`：80
- vocab / grammar / sentence：40 / 20 / 20
- 与选集按顺序逐项匹配：80 / 80
- overlay ID 不在基础候选中：0
- overlay source 不一致：0
- `validate_candidates.mjs`：errors 0，warnings 0
- payload 内 source ID／模板元信息泄漏：0
- 批内重复长教学字符串警告：0
- 与 Batch1–11 ID／source 重叠：0 / 0
- 批内及跨 Batch1–11 完全相同长教学字符串叶：0 / 0
- 批内或跨 Batch1–11 的归一化 28 字符连续片段复用：0
- 旧模板短语命中：0
- 最大跨批整卡字符 4-gram Jaccard：`0.058265582655826556`（与生成侧冻结审计一致）

本报告与 overlay 仅完成独立复核和候选修订，不授权或执行数据库写入。
