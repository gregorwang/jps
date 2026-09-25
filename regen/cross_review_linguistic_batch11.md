# Linguistic Batch11 独立交叉审校

## 结论

本轮从磁盘恢复时，`review_overlay_linguistic_batch11.json` 与 `cross_review_linguistic_batch11.md` 均不存在，因此没有把崩溃前的口头进度当作完成结果，而是由未参与生成的 reviewer 从头独立复核全部 80 条候选。

- 审校对象：
  - `batch11_vocab_linguistic.json`：40 条 vocab
  - `batch11_grammar_sentence_linguistic.json`：20 条 grammar
  - `batch11_sentence_linguistic_overlay.json`：20 条 sentence
- 基础稿：80 条，唯一 ID 80，唯一 `source_type + source_id` 80
- ordered newline-ID MD5：`565962f3fc8972495aea7427755a5ccd`
- 独立审校结果：**原样保留 74，人工修订 6，拒绝 0**
- 修订分布：vocab 2；grammar 3；sentence 1
- 数据库操作：0；本轮只制作审校 overlay 与报告
- 教学文字处理：逐卡人工判断；脚本只用于读取、计数、按 ID 模拟合并、结构校验与 validator，没有生成或拼接教学内容

| source_type | 总数 | 原稿保留 | 审校修订 | 拒绝 |
| --- | ---: | ---: | ---: | ---: |
| vocab | 40 | 38 | 2 | 0 |
| grammar | 20 | 17 | 3 | 0 |
| sentence | 20 | 19 | 1 | 0 |
| 合计 | 80 | 74 | 6 | 0 |

冻结方式为三份生成稿加一份 6 条完整对象的审校 overlay：

- 基础稿保持生成侧冻结哈希不变。
- `review_overlay_linguistic_batch11.json` 只能按 `id` 替换基础稿中的同 ID 完整对象。
- 禁止把 overlay 追加为第 81–86 条；这样会制造重复 ID。

## 独立复核口径

生成侧 QA 只用于定位 source 与字幕范围，没有代替独立判断。80/80 均重新检查：

- 日语形态：活用、使役、被动、可能、愿望、否定、敬语、口语缩约及固定表达；
- 句法：格关系、话题、引用、补语、条件、名词化、述语省略与跨行接续；
- 语义：义项选择、否定范围、时态／体、施事与受事、词汇化意义；
- 语用：说话人立场、礼貌外形、毒舌／威胁／誓言、断言强度与证据边界；
- 指代与上下文：省略主语、`自分`、`それ／そう／こう`、前指／回指、相邻话轮及跨字幕后件；
- 教学边界：不把推测写成事实，不补出字幕不存在的施事、原话、宾语或隐藏语法成分。

## 逐 ID 审计清单

下表逐一列出基础稿全部 80 个完整 enrichment ID。`修订` 必须且只能对应 review overlay 中的 6 个 ID；其余 74 个 ID 均为独立复核后原样保留。

| # | 完整 ID | source_type | 处置 | 简短复核结论 |
| ---: | --- | --- | --- | --- |
| 1 | `enrich-vocab-re-zero-vocab-死因-v1` | vocab | 保留 | 确认是昴对第一次衰弱死的暂定归因，`ってとこか` 没有被写成医学确证。 |
| 2 | `enrich-vocab-re-zero-vocab-襲撃者-v1` | vocab | 保留 | 未知袭击者从不定存在逐步成为调查对象，没有擅自补定动机、阵营或人数。 |
| 3 | `enrich-vocab-re-zero-vocab-狙う-v1` | vocab | 保留 | `狙われていた` 的宅邸众人是受事，未知施事和疑问语气均保留。 |
| 4 | `enrich-vocab-re-zero-vocab-未然に防ぐ-v1` | vocab | 保留 | 确认为袭击发生前的预防，证据与手段两项都受后续否定管辖。 |
| 5 | `enrich-vocab-re-zero-vocab-追っ払う-v1` | vocab | 保留 | 口语缩约与主动驱逐义正确，“虫”到后文才揭示为昴。 |
| 6 | `enrich-vocab-re-zero-vocab-震え-v1` | vocab | 保留 | `震える` 的名词形和身体线索解释成立；停抖不等于恐惧已经消失。 |
| 7 | `enrich-vocab-re-zero-vocab-頃合い-v1` | vocab | 保留 | `頃合い` 依据手不抖、能掩饰恐惧等状态判断合适时机，不是精确钟点。 |
| 8 | `enrich-vocab-re-zero-vocab-花壇-v1` | vocab | 保留 | `なかったら` 的反事实方向正确，花坛存在与昴幸存的现实关系未倒置。 |
| 9 | `enrich-vocab-re-zero-vocab-肥料-v1` | vocab | 保留 | `ふんを` 是被撒材料，`肥料に` 标用途，没有倒成给粪便施肥。 |
| 10 | `enrich-vocab-re-zero-vocab-挽回-v1` | vocab | 保留 | 名词独语表达昴的完成式自我计分，只是修复恶印象而非彻底恢复信任。 |
| 11 | `enrich-vocab-re-zero-vocab-愛称-v1` | vocab | 保留 | 爱称预设亲密但未获对方共同认可，命名权与关系边界说明准确。 |
| 12 | `enrich-vocab-re-zero-vocab-表現-v1` | vocab | 保留 | `愛情表現` 与 `その表現` 分别指行为类别和措辞，两个义项没有混同。 |
| 13 | `enrich-vocab-vocab-re-zero-aidagara-v1` | vocab | 保留 | `間柄` 指双方构成的人际关系，不是单个人的性格或一般物理关系。 |
| 14 | `enrich-vocab-vocab-re-zero-shitashii-v1` | vocab | 保留 | `親しい` 连体与 `親しくしたい` 连用均正确，现状被修正为未来亲近愿望。 |
| 15 | `enrich-vocab-re-zero-vocab-解説-v1` | vocab | 保留 | `解説のパックさん` 是戏谑的临时角色标签，请求解释心理背景而非逐字翻译。 |
| 16 | `enrich-vocab-re-zero-vocab-汚い-v1` | vocab | 保留 | 花坛和清洗语境选择物理污秽义，没有误教成“手段卑鄙”。 |
| 17 | `enrich-vocab-vocab-re-zero-toushu-v1` | vocab | 保留 | `当主` 正确指家门与宅邸权力结构的首领，不只是房屋所有人。 |
| 18 | `enrich-vocab-re-zero-vocab-疑う-v1` | vocab | 保留 | 被动方向、`と` 所标身份和 `かねない` 的不利可能均正确。 |
| 19 | `enrich-vocab-re-zero-vocab-背格好-v1` | vocab | 保留 | 词义限于身高与整体体格轮廓，没有扩张成五官、发型或完整长相。 |
| 20 | `enrich-vocab-vocab-re-zero-abaku-v1` | vocab | 保留 | `正体を暴く` 的揭露宾语和 `てみせる` 的强意志均无倒置。 |
| 21 | `enrich-vocab-re-zero-vocab-褒美-v1` | vocab | 保留 | 上位者奖赏关系成立；任昴提出愿望不等于任何要求已自动生效。 |
| 22 | `enrich-vocab-re-zero-vocab-食客-v1` | vocab | 保留 | 受供养寄居身份与拉姆后续 `食客という名の居候` 的讽刺重命名一致。 |
| 23 | `enrich-vocab-re-zero-vocab-聞き込み-v1` | vocab | 保留 | `みんなから` 标信息来源，`それとなく` 保留隐藏真实调查目的的策略。 |
| 24 | `vocab-card-v1:vocab-re-zero-suki` | vocab | 保留 | `隙` 是插话机会，`もねえ` 表示连最低空当也没有，未与 `好き` 混同。 |
| 25 | `enrich-vocab-re-zero-vocab-異常-v1` | vocab | 保留 | 结论只覆盖昴对坠落地点的当前观察，没有推成袭击风险已消失。 |
| 26 | `enrich-vocab-re-zero-vocab-気安く-v1` | vocab | 保留 | 不拘礼既可亲近也可冒失，拉姆回收昴原话为自己态度辩护的链条正确。 |
| 27 | `enrich-vocab-re-zero-vocab-無礼極まりない-v1` | vocab | 保留 | `極まりない` 是极度强化而非普通否定，评价依据限定为不敲门闯入。 |
| 28 | `enrich-vocab-re-zero-vocab-身のため-v1` | vocab | 保留 | `身` 的受益者是昴，表面忠告与碧翠丝的吹飞威胁同时保留。 |
| 29 | `enrich-vocab-re-zero-vocab-衰弱させる-v1` | vocab | 保留 | 使役表示让对方逐渐虚弱，不额外保证施事具有主观杀意或案发手段已确认。 |
| 30 | `enrich-vocab-re-zero-vocab-発祥-v1` | vocab | 修订 | 将本义校正为“兴起／起源”，地点义只由古斯提科与句内结构共同形成。 |
| 31 | `enrich-vocab-re-zero-vocab-出来損ない-v1` | vocab | 修订 | 省略话题续接 `呪術師は`，评价的是不成器的咒术师而非失败术法。 |
| 32 | `enrich-vocab-re-zero-vocab-身をもって-v1` | vocab | 保留 | 惯用副词组表示以亲身经历获得知识，不是“拿着身体”或主动自残。 |
| 33 | `enrich-vocab-re-zero-vocab-生命力-v1` | vocab | 保留 | `そのものでもある` 的同一性与追加 `も` 正确，并明确只属作品内玛那设定。 |
| 34 | `enrich-vocab-vocab-re-zero-giri-v1` | vocab | 保留 | `義理はない` 否认关系义务，同时语用上泄露碧翠丝确实参与救治。 |
| 35 | `enrich-vocab-re-zero-vocab-突破-v1` | vocab | 保留 | `突破できた` 回顾王都第四次循环成功，没有误写成当前宅邸危机已解决。 |
| 36 | `enrich-vocab-re-zero-vocab-曖昧-v1` | vocab | 保留 | 信息不充分不等于撒谎，帕克仍认可昴担忧艾米莉雅的动机。 |
| 37 | `enrich-vocab-re-zero-vocab-案じる-v1` | vocab | 保留 | `リアを案じてる` 选择挂念、担忧义，`確かみたい` 的推断强度保留。 |
| 38 | `enrich-vocab-re-zero-vocab-認識-v1` | vocab | 保留 | `そう` 回指完整身份分类，`認識している` 是持续认定而非初次认出人物。 |
| 39 | `enrich-vocab-re-zero-vocab-要望-v1` | vocab | 保留 | `ご要望に応える` 的礼貌外形和拉姆把责任归还给昴的讽刺功能一致。 |
| 40 | `enrich-vocab-re-zero-vocab-異文化交流-v1` | vocab | 保留 | 童话阅读只被轻松概括为交流体验，句末 `感じ？` 没有被升级为全面理解。 |
| 41 | `grammar-card-v1:re-zero-s01e06-grammar-001` | grammar | 保留 | `戻ってこれた` 是会话可能形，`と言うべきか` 用于措辞修正而非道德义务。 |
| 42 | `grammar-card-v1:re-zero-s01e06-grammar-002` | grammar | 保留 | 悬空 `って` 保持转念断裂，`忘れられてる` 的被动持续状态没有倒置。 |
| 43 | `grammar-card-v1:re-zero-s01e06-grammar-003` | grammar | 保留 | 固定郑重道歉、敬语前缀与过去式承认已造成影响的分析正确。 |
| 44 | `grammar-card-v1:re-zero-s01e06-grammar-004` | grammar | 保留 | `今って` 的话题化及 `何日の何時` 的日期、时刻双重询问完整保留。 |
| 45 | `grammar-card-v1:re-zero-s01e06-grammar-005` | grammar | 保留 | `襲撃がある` 经 `という` 成为 `事実` 内容，并限定为昴的循环知识。 |
| 46 | `grammar-card-v1:re-zero-s01e06-grammar-006` | grammar | 保留 | 可能形、假定 `ば` 和未尽逆接 `が` 共同表明理想方案尚不可行。 |
| 47 | `grammar-card-v1:re-zero-s01e06-grammar-007` | grammar | 修订 | 改为固定表达 `下手をすると` 省略 `を` 并词汇化，禁止 `下手だ＋する＋と`。 |
| 48 | `grammar-card-v1:re-zero-s01e06-grammar-008` | grammar | 保留 | `疑われかねない` 是被动连用形加负面可能，末尾 `ない` 不表示“不可能”。 |
| 49 | `grammar-card-v1:re-zero-s01e06-grammar-009` | grammar | 保留 | 连读 `せめて`、`だけでも`、`分かれば` 与后件 `話も違う`，最低门槛清楚。 |
| 50 | `grammar-card-v1:re-zero-s01e06-grammar-010` | grammar | 保留 | `情報収集に徹する` 正确表示收窄行动范围，て形连接调查手段与揭露目标。 |
| 51 | `grammar-card-v1:re-zero-s01e06-grammar-011` | grammar | 保留 | `暴いてみせる` 是完成承诺与自我激励，不与试做的 `暴いてみる` 混同。 |
| 52 | `grammar-card-v1:re-zero-s01e06-grammar-013` | grammar | 保留 | `とはいえ` 承认调查计划后补执行困难，没有被写成彻底反驳前案。 |
| 53 | `grammar-card-v1:re-zero-s01e06-grammar-014` | grammar | 保留 | `それとなく` 是有目的地隐藏意图，不是漫无目的或“没有任何意图”。 |
| 54 | `grammar-card-v1:re-zero-s01e06-grammar-015` | grammar | 保留 | `無礼極まりない` 的固定极度构式与行为根据、后续驱逐威胁均正确。 |
| 55 | `grammar-card-v1:re-zero-s01e06-grammar-016` | grammar | 保留 | `吹き飛ばされる前に` 保留昴受害的被动方向和主动作的事前期限。 |
| 56 | `grammar-card-v1:re-zero-s01e06-grammar-017` | grammar | 修订 | `身のため` 直接作名词述语，由 `かしら` 收束，不再虚构省略的 `いい`。 |
| 57 | `grammar-card-v1:re-zero-s01e06-grammar-018` | grammar | 保留 | `あるかないかで言えば ある` 只在有无尺度作最低肯定，未覆盖后续分类修正。 |
| 58 | `grammar-card-v1:re-zero-s01e06-grammar-019` | grammar | 保留 | `魔法というより 呪い` 是更准确标签的相对修正，不是绝对否定魔法。 |
| 59 | `grammar-card-v1:re-zero-s01e06-grammar-020` | grammar | 保留 | `呪いのほうに近い` 的方向比较、抽象距离与 `かしら` 判断强度均准确。 |
| 60 | `grammar-card-v1:re-zero-s01e06-grammar-021` | grammar | 修订 | `もっとも` 是限制性连接词，省略话题仍为咒术师，非“术法都是失败品”。 |
| 61 | `sentence-card-v1:re-zero-s01e06-sentence-023` | sentence | 保留 | 句首 `って` 是来不及完成的回应，未伪造不存在的完整引语。 |
| 62 | `sentence-card-v1:re-zero-s01e06-sentence-044` | sentence | 保留 | 句末 `けど` 跨到下一行负面评价，青鬼的帅气与徒劳同时成立。 |
| 63 | `sentence-card-v1:re-zero-s01e06-sentence-045` | sentence | 保留 | `頑張った分だけ` 建立投入与回报对应，`タイプだし` 将立场个人化。 |
| 64 | `sentence-card-v1:re-zero-s01e06-sentence-046` | sentence | 保留 | `自分` 指被评论的赤鬼，`青鬼を` 是被卷入者，后行继续说明受损结果。 |
| 65 | `sentence-card-v1:re-zero-s01e06-sentence-047` | sentence | 保留 | 两鬼都想亲近被拉姆戏谑重构为花心，不作为真实恋爱背叛教学。 |
| 66 | `sentence-card-v1:re-zero-s01e06-sentence-048` | sentence | 保留 | 书中盟约是 `書いてあった` 的内容，`し` 只列举一项历史传说判据。 |
| 67 | `sentence-card-v1:re-zero-s01e06-sentence-049` | sentence | 保留 | `神のみぞ` 经 `いえ` 即时改为 `竜のみぞ`，古典焦点与文化改写均正确。 |
| 68 | `sentence-card-v1:re-zero-s01e06-sentence-050` | sentence | 保留 | `聞かせないで` 的使役方向是不要让雷姆听见，不是不要听雷姆说。 |
| 69 | `sentence-card-v1:re-zero-s01e06-sentence-051` | sentence | 保留 | `嫉妬の魔女と` 是后置命名补语，宾语、`こう` 与 `呼んだ` 均从上一行恢复。 |
| 70 | `sentence-card-v1:re-zero-s01e06-sentence-052` | sentence | 保留 | `息災で` 是古雅的省略式临别祝愿，句尾 `で` 没有被误作原因助词。 |
| 71 | `sentence-card-v1:re-zero-s01e06-sentence-053` | sentence | 修订 | 两句校正为保密承诺与龙誓担保的关系，不再称作因果承接。 |
| 72 | `sentence-card-v1:re-zero-s01e06-sentence-054` | sentence | 保留 | `ゆめゆめ` 与否定呼应，`それ` 回指龙誓分量而非伴手礼。 |
| 73 | `sentence-card-v1:re-zero-s01e06-sentence-055` | sentence | 保留 | `ってば` 的责备性提题与敬称、敬体包裹的 `下手クソ` 毒舌一致。 |
| 74 | `sentence-card-v1:re-zero-s01e06-sentence-056` | sentence | 保留 | `いろいろ` 概括多项帮助并省略授受动作，感谢主体与告别语境正确。 |
| 75 | `sentence-card-v1:re-zero-s01e06-sentence-057` | sentence | 保留 | 单独 `食い止める` 从前行恢复袭击事态和昴主语，表示介入阻止而非进食。 |
| 76 | `sentence-card-v1:re-zero-s01e06-sentence-058` | sentence | 保留 | `死にたくねえ` 与 `死なせたくねえ` 的使役最小对正确扩展到不让他人死。 |
| 77 | `sentence-card-v1:re-zero-s01e06-sentence-059` | sentence | 保留 | 本行先确认成功看见，宾语 `鎖の音の正体` 到下一话轮才揭示。 |
| 78 | `sentence-card-v1:re-zero-s01e06-sentence-060` | sentence | 保留 | `踊らされた` 的使役被动和掌上起舞操控隐喻均保持昴为受控方。 |
| 79 | `sentence-card-v1:re-zero-s01e06-sentence-061` | sentence | 保留 | 敌意语境中的 `てもらえる` 只反映雷姆所求结果，过去方案和杀意没有被礼貌化抹掉。 |
| 80 | `sentence-card-v1:re-zero-s01e06-sentence-062` | sentence | 保留 | `ウソだろ レム` 表达不愿相信袭击者身份，不是指控雷姆刚刚撒谎。 |

### 清单反向解析校验

校验程序从本节 Markdown 表格反向解析 ID、类型和处置，不读取报告中的汇总数字作为答案；随后与三份基础稿及 review overlay 做集合比较：

```json
{
  "rows": 80,
  "unique": 80,
  "missing": 0,
  "extra": 0,
  "duplicates": 0,
  "wrongStatus": 0,
  "wrongType": 0,
  "wrongIndex": 0,
  "emptyConclusion": 0,
  "statuses": {
    "retained": 74,
    "revised": 6
  },
  "types": {
    "vocab": 40,
    "grammar": 20,
    "sentence": 20
  }
}
```

### Transport 守卫：逐 source_id 清单

以下 80 个 `source_id` 与上表一一对应，供 reviewed transport 在传输前做显式独立复核覆盖检查：

| # | source_type | source_id |
| ---: | --- | --- |
| 1 | vocab | `re-zero-vocab-死因` |
| 2 | vocab | `re-zero-vocab-襲撃者` |
| 3 | vocab | `re-zero-vocab-狙う` |
| 4 | vocab | `re-zero-vocab-未然に防ぐ` |
| 5 | vocab | `re-zero-vocab-追っ払う` |
| 6 | vocab | `re-zero-vocab-震え` |
| 7 | vocab | `re-zero-vocab-頃合い` |
| 8 | vocab | `re-zero-vocab-花壇` |
| 9 | vocab | `re-zero-vocab-肥料` |
| 10 | vocab | `re-zero-vocab-挽回` |
| 11 | vocab | `re-zero-vocab-愛称` |
| 12 | vocab | `re-zero-vocab-表現` |
| 13 | vocab | `vocab-re-zero-aidagara` |
| 14 | vocab | `vocab-re-zero-shitashii` |
| 15 | vocab | `re-zero-vocab-解説` |
| 16 | vocab | `re-zero-vocab-汚い` |
| 17 | vocab | `vocab-re-zero-toushu` |
| 18 | vocab | `re-zero-vocab-疑う` |
| 19 | vocab | `re-zero-vocab-背格好` |
| 20 | vocab | `vocab-re-zero-abaku` |
| 21 | vocab | `re-zero-vocab-褒美` |
| 22 | vocab | `re-zero-vocab-食客` |
| 23 | vocab | `re-zero-vocab-聞き込み` |
| 24 | vocab | `vocab-re-zero-suki` |
| 25 | vocab | `re-zero-vocab-異常` |
| 26 | vocab | `re-zero-vocab-気安く` |
| 27 | vocab | `re-zero-vocab-無礼極まりない` |
| 28 | vocab | `re-zero-vocab-身のため` |
| 29 | vocab | `re-zero-vocab-衰弱させる` |
| 30 | vocab | `re-zero-vocab-発祥` |
| 31 | vocab | `re-zero-vocab-出来損ない` |
| 32 | vocab | `re-zero-vocab-身をもって` |
| 33 | vocab | `re-zero-vocab-生命力` |
| 34 | vocab | `vocab-re-zero-giri` |
| 35 | vocab | `re-zero-vocab-突破` |
| 36 | vocab | `re-zero-vocab-曖昧` |
| 37 | vocab | `re-zero-vocab-案じる` |
| 38 | vocab | `re-zero-vocab-認識` |
| 39 | vocab | `re-zero-vocab-要望` |
| 40 | vocab | `re-zero-vocab-異文化交流` |
| 41 | grammar | `re-zero-s01e06-grammar-001` |
| 42 | grammar | `re-zero-s01e06-grammar-002` |
| 43 | grammar | `re-zero-s01e06-grammar-003` |
| 44 | grammar | `re-zero-s01e06-grammar-004` |
| 45 | grammar | `re-zero-s01e06-grammar-005` |
| 46 | grammar | `re-zero-s01e06-grammar-006` |
| 47 | grammar | `re-zero-s01e06-grammar-007` |
| 48 | grammar | `re-zero-s01e06-grammar-008` |
| 49 | grammar | `re-zero-s01e06-grammar-009` |
| 50 | grammar | `re-zero-s01e06-grammar-010` |
| 51 | grammar | `re-zero-s01e06-grammar-011` |
| 52 | grammar | `re-zero-s01e06-grammar-013` |
| 53 | grammar | `re-zero-s01e06-grammar-014` |
| 54 | grammar | `re-zero-s01e06-grammar-015` |
| 55 | grammar | `re-zero-s01e06-grammar-016` |
| 56 | grammar | `re-zero-s01e06-grammar-017` |
| 57 | grammar | `re-zero-s01e06-grammar-018` |
| 58 | grammar | `re-zero-s01e06-grammar-019` |
| 59 | grammar | `re-zero-s01e06-grammar-020` |
| 60 | grammar | `re-zero-s01e06-grammar-021` |
| 61 | sentence | `re-zero-s01e06-sentence-023` |
| 62 | sentence | `re-zero-s01e06-sentence-044` |
| 63 | sentence | `re-zero-s01e06-sentence-045` |
| 64 | sentence | `re-zero-s01e06-sentence-046` |
| 65 | sentence | `re-zero-s01e06-sentence-047` |
| 66 | sentence | `re-zero-s01e06-sentence-048` |
| 67 | sentence | `re-zero-s01e06-sentence-049` |
| 68 | sentence | `re-zero-s01e06-sentence-050` |
| 69 | sentence | `re-zero-s01e06-sentence-051` |
| 70 | sentence | `re-zero-s01e06-sentence-052` |
| 71 | sentence | `re-zero-s01e06-sentence-053` |
| 72 | sentence | `re-zero-s01e06-sentence-054` |
| 73 | sentence | `re-zero-s01e06-sentence-055` |
| 74 | sentence | `re-zero-s01e06-sentence-056` |
| 75 | sentence | `re-zero-s01e06-sentence-057` |
| 76 | sentence | `re-zero-s01e06-sentence-058` |
| 77 | sentence | `re-zero-s01e06-sentence-059` |
| 78 | sentence | `re-zero-s01e06-sentence-060` |
| 79 | sentence | `re-zero-s01e06-sentence-061` |
| 80 | sentence | `re-zero-s01e06-sentence-062` |

## 人工修订明细

### 1. `re-zero-vocab-発祥`

生成稿把 `発祥` 本身释成“最初兴起的地方”，混淆了“起源”与“发祥地”。

修订后明确：

- `発祥` 表示文化、技法或制度的兴起／起源；
- 地点义来自 `国が発祥`、`発祥の地`、`日本発祥` 等组合；
- 本句把古斯提科标成相关诅咒体系的发祥地，但没有说该体系只存在于该国。

### 2. `re-zero-vocab-出来損ない`

生成稿把 `出来損ないばかり` 错指为“现存诅咒术法都是失败品”。连续话轮为：

`呪術師？` → `…呪術師は` → `もっとも 出来損ないばかりで` → `とてもまともに扱えたもんじゃない`

省略话题延续的是前句 `呪術師は`。因此 `出来損ない` 贬低的是一群咒术师“不成器”，下一句说明他们难以正常驾驭相关术法；不是把术法本身叫作残次品。overlay 已同步修正 headline、三项 domain、terms、caution 与 review note。

### 3. `re-zero-s01e06-grammar-007`

生成稿明确写成错误分析 `下手だ＋する＋と`。

修订后固定为：

- `下手すると` 来自固定表达 `下手をすると` 的助词 `を` 省略；
- `下手をする` 表示处置不当、情势走偏，再接条件 `と`；
- 整体已词汇化为“弄不好／搞不好”，本句引出昴可能被当成刺客同伙的不利后果；
- caution 明文禁止再分析成 `下手だ＋する＋と`。

### 4. `re-zero-s01e06-grammar-017`

生成稿把 `消えたほうが 身のため` 讲成末尾省略了 `いい`，属于无依据补写。

修订后按实际句法说明：

- `消えたほう` 把“赶快离开”作为与不离开相比的方案；
- `身のため` 直接承担名词述语，说明该方案“对你自身有利”，句尾由 `かしら` 收束；
- 不必先虚构 `Vたほうがいい` 再说 `いい` 被删除；
- 前行 `吹き飛ばされる前に` 仍使这项表面建议成为最后通牒。

### 5. `re-zero-s01e06-grammar-021`

`もっとも` 的连接词判断本身正确，但生成稿沿用了 `出来損ない` 的错误指代，把保留项写成“术法多是失败品”。

修订后明确：

- 这里是连接词“不过／话虽如此”，不是最高级 `最も`；
- 省略话题延续 `呪術師は`，评价的是使用者能力；
- 保留项限制的是咒术师能否熟练驾驭术法，不撤回相关体系存在，也不证明诅咒没有危险。

### 6. `re-zero-s01e06-sentence-053`

生成稿把 `余計なことは言わない。ドラゴンに誓うぜ` 两个短句称为“因果般承接”。

实际关系是：

- 前句作出未来保密承诺；
- 后句以龙为见证，为前一承诺提供最高等级的担保；
- 两句是“承诺—起誓／担保”关系，不是原因与结果。

## 其余 74 条的关键边界确认

### vocab

- `狙われていた` 的宅邸众人是受事，未知袭击者没有被补成确定施事。
- `未然に防ぐ` 承接袭击，表示事前阻止，不是事后处理。
- `追っ払う` 的“虫”后文才揭示为昴，保留延迟讽刺。
- `汚いボロ雑巾` 选择物理污秽义，没有误教为“手段卑鄙”。
- `刺客の一味と疑われかねない` 保留被动、身份补语与不利可能，未倒成昴怀疑别人或“不可能被怀疑”。
- `義理はない` 否认继续帮助的关系义务，同时保留碧翠丝已参与救治这一语用泄露。
- `案じる` 选择挂念／担忧义，不按汉字误写成制定方案。
- `無礼極まりない` 的表面 `ない` 没有被当成普通否定。

### grammar

- `戻ってこれた` 按会话中的ら抜き可能处理；`と言うべきか` 是措辞选择，不是道德义务。
- grammar-002 的悬空 `って` 没有被补写成不存在的完整引语。
- grammar-004 同时保留 `何日` 与 `何時`，没有把真实双重提问缩成只问日期。
- `疑われかねない` 保留被动方向及负面可能。
- grammar-009 连读上一话轮 `せめて`、最低项 `だけでも`、条件 `分かれば` 与后件 `話も違う`。
- `情報収集に徹して` 与 `正体を暴いてみせる` 分别保持专注手段与强意志结果。
- `あるかないかで言えば ある` 只作最低限度肯定，后续分类修正未被抹掉。
- `魔法というより 呪いのほうに近い` 保留相对比较，不写成绝对否定魔法。

### sentence

- sentence-023 的句首 `って` 保持被截断回应，不伪造昴没说出口的话。
- sentence-044 的 `けど` 连到下一行负面评价，未把青鬼评价截成单向赞美。
- sentence-046 的 `自分` 属于被评论的赤鬼，不属于拉姆或昴；后行继续给出青鬼受损结果。
- sentence-047 的“花心”是拉姆把两鬼都想亲近戏谑重构成恋爱框架，不是事实性恋爱背叛。
- sentence-049 连读 `神のみぞ` 到 `竜のみぞ` 的即时自我修正。
- sentence-051 的 `嫉妬の魔女と` 是后置命名补语；宾语和 `呼んだ` 位于上一话轮。
- sentence-058 保留 `死にたくねえ／死なせたくねえ` 的使役最小对与保护范围。
- sentence-059 先强调“看见成功”，所见对象 `鎖の音の正体` 到下一话轮才揭示。
- sentence-061 的 `てもらう` 只反映雷姆期待取得的结果，礼貌授受外形没有把暗杀变成对昴有利的帮助。
- sentence-062 的 `ウソだろ` 是不愿相信袭击者身份，不是指控雷姆刚才撒谎。

## Overlay 与按 ID 合并检查

`review_overlay_linguistic_batch11.json`：

- rows：6
- unique IDs：6
- source_type：vocab 2、grammar 3、sentence 1
- 6/6 ID 均在 80 条基础稿中恰好命中
- 6/6 `source_type + source_id` 与对应基础对象一致
- overlay 内重复 ID：0
- overlay 以完整对象替换，不依赖字段级追加

在内存中按 ID 替换后的最终候选：

- rows：80
- unique IDs：80
- unique `source_type + source_id`：80
- replacements：6
- 类型分布：vocab 40、grammar 20、sentence 20
- 结构 errors：0
- 重复长教学字符串 warnings：0
- learner-facing payload 内部 ID／source 字段泄漏：0

## Validator

三份生成侧基础稿：

```json
{
  "files": 3,
  "rows": 80,
  "uniqueIds": 80,
  "errors": [],
  "warnings": []
}
```

审校 overlay：

```json
{
  "files": 1,
  "rows": 6,
  "uniqueIds": 6,
  "errors": [],
  "warnings": []
}
```

按 ID 内存合并复算：

```json
{
  "baseRows": 80,
  "overlayRows": 6,
  "replacements": 6,
  "mergedRows": 80,
  "mergedUniqueIds": 80,
  "mergedUniqueSources": 80,
  "errors": [],
  "warnings": []
}
```

## 冻结 SHA-256

- `batch11_vocab_linguistic.json`：`5A37915CDF0DF0EAEF5DD4F7F2722D4E3FE4BFD31DF37EED4698699CC5D71123`
- `batch11_grammar_sentence_linguistic.json`：`576132E30C1FE11F92562B4BEEDC68F0043DF508A1154925DDED8EFE61716226`
- `batch11_sentence_linguistic_overlay.json`：`0AAEF58ABC18C9CD32EFA431959933D7441F4920450B6461209188384D718279`
- `review_overlay_linguistic_batch11.json`：`A775B6FE0CD521A8F84C13770701EA3FBEE7DCF8EA2E8F91D9D247E3706DFE4C`

## 状态

独立交叉审校完成。最终处置为：**保留 74、修订 6、拒绝 0**。

后续写库前必须按 `id` 合并 overlay，重新执行数据库目标／备份／未漂移／manifest 零重叠守卫，并在写入时固定：

- `linguistic_prompt_version = agent-jp-regenerated-v1`
- `linguistic_quality_score = 95`
- `linguistic_status = ready`

本轮没有执行任何数据库写入。
