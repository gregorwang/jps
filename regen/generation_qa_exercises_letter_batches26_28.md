# Letter-answer exercises batches 26–28：生成侧 QA

## 结论

本批从当前仍为单字母答案的练习中排除已应用 manifest，并显式排除已冻结但尚未写库的 batches 23–25 共 120 个 ID，再按 `episode, sort_order, id` 取下一组 120 条。每条都读取原题、相关词汇／语法记录、真实日文字幕及连续话轮后人工原创 `prompt / answer / hint / review_note`；脚本只用于只读选集、精确映射和 QA，没有用 Python 模板或循环拼接教学文本。全程未写数据库。

| 文件 | 条数 | 范围 | 题型 |
|---|---:|---|---|
| `batch26_exercises_letter.json` | 40 | EP20 exercise 049–088 | 20 reading；20 meaning |
| `batch27_exercises_letter.json` | 40 | EP20 exercise 089–128 | 20 reading；20 meaning |
| `batch28_exercises_letter.json` | 40 | EP20 exercise 129–168 | 6 reading；6 meaning；28 grammar |
| **合计** | **120** | **EP20 exercise 049–168** | **46 reading；46 meaning；28 grammar** |

数据库原题型分布：

- `vocab_reading`：46
- `vocab_meaning`：46
- `grammar_meaning`：28

## 选集冻结与精确映射

筛选步骤：

1. 当前 `public.learning_exercises.answer` 去空白后匹配单个 `A/B/C/D`。
2. 排除 `maintenance.regen_20260727_manifest` 中同 ID 且 `applied_at is not null` 的记录。
3. 从本地 `batch23/24/25_exercises_letter.json` 读取并显式排除其 120 个 ID。
4. 按 `episode, sort_order, id` 排序，取前 120。

只读复查结果：

- batches 23–25 排除集：120 条、120 个唯一 ID；范围为 EP19 exercise 169–240 与 EP20 exercise 001–048。
- 排除集首尾：`re-zero-s01e19-exercise-169` → `re-zero-s01e20-exercise-048`。
- 排除集有序 ID（以 `|` 连接）MD5：`8154c839836f2feaacf698fe66d0ef5f`。
- 新选集：120 条、120 个唯一 ID；数据库首尾为 `re-zero-s01e20-exercise-049` → `re-zero-s01e20-exercise-168`，`sort_order` 连续为 49–168。
- 三个候选文件的 ID 与数据库逐位置一致；缺失 0、额外 0、顺序错误 0。
- 数据库与候选有序 ID（以 `|` 连接）MD5 均为 `3022cbbcc57981a263fff3935ba78a96`。
- 新选集与 batches 23–25 重叠：0。
- 与已应用 manifest 重叠：0；与未应用 manifest 重叠：0。
- 与最终扫描时其余 56 个本地 JSON 候选文件、2,509 条记录的 ID 重叠：0。

## 来源、学习项与真实字幕

- 120/120 条原题 source 经仅去除空白、标点、引号及括号注音的归一化后，均精确命中 EP20 真实日文字幕；共覆盖 46 个不同源句。
- 100/120 条 source 同样精确命中 `learning_sentences`：
  - 28 条语法题：28/28。
  - 46 条词汇读音题：36/46。
  - 46 条词汇意义题：36/46。
- 92 条词汇题原记录的 `vocab_item_id` 虽全部为空，仍可按题面目标表层映射到 `learning_vocab_items` 与 EP20 `learning_vocab_occurrences`：92/92，覆盖 44 个唯一表层。
- 28 条语法题中，6 条有归一化后完全相同的 `learning_grammar_points.ja_example`；其余 22 条由对应 `learning_sentences`、真实字幕和连续话轮支持，没有根据缺失或错误标签猜写。
- 学习表中的来源行号存在系统性偏移，所以本批以 `subtitle_lines.line_no` 的实际文本为准。例如：
  - `無視` occurrence 标为 line 38，真实字幕「射程を無視した無形の剣」在 line 32。
  - `剣技` occurrence 标为 line 39，真实字幕在 line 33。
  - `攻撃手段` 与 `生命線` occurrence 分别标为 lines 84、86，真实字幕分别在 lines 73、75。
  - `夜払いが来ます…` 的 grammar 记录标为 line 16，真实字幕在 line 14。
  - `よそ見とは…` 的 grammar 记录标为 line 37，真实字幕在 line 31。

## 人工修正：词汇表层、读音与场景义

- 所有读音题以字幕实际表层为准，不把词典形冒充现场读法：
  - `さらせ` 读 `さらせ`，基本形为 `さらす`。
  - `離れろ` 读 `はなれろ`，基本形为 `離れる`。
  - `放て` 读 `はなて`，基本形为 `放つ`。
  - `効いた` 读 `きいた`；`効いてねえ` 展开为 `効いていない`；`効いていません` 保留完整礼貌否定。
- 对 `ひたすら`、`タフ`、`マナ`、`おとり`、`こなす` 等全假名或片假名词，没有设计成照抄题面的伪读音题，而是分别训练拍数、词性、名词化、书写变体、活用和实际词形。
- `無視` 按无视射程限制解释，不套用人际“故意不理人”；`手札を切る` 按投入战术资源解释，不写成撕纸牌。
- `威力を殺す` 按抑制、抵消力量的引申义；`マナを散らす` 只解释为打散能量，没有编造落点或额外术式机制。
- `おとり` 按吸引白鲸的战术诱饵解释；`邪魔になる` 是此时闯入友军火力范围会造成妨碍，不是同伴嫌弃昴。
- `攻撃手段` 与 `回復特化` 形成明确能力对照；`生命線` 是维系队伍生存的比喻，不是掌纹。
- `役割だけはこなしてくれ／頼むからよ` 按治疗职责、受益方向请求和恳求语气解释，没有泛化成冷静命令。

## 人工修正：28 条语法题

- `exercise-141`：`見ない` 是否定连体形修饰 `顔`，`だな` 表示观察确认；旧 grammar 标签“命令形”错误。
- `exercise-142`：把 `してやがんだ` 展开为 `してやがるんだ`，说明 `てやがる` 的粗鲁、轻蔑评价和 `んだ` 的盘问口气。
- `exercise-143`：区分重复的 `いいから`（消除顾虑、推进话题）与 `おいで`（亲近的邀请／催促）。
- `exercise-144`：`花は好き？` 由 `は` 设定花的话题，喜欢者从面对面对话恢复；不是强补字幕没有的人称。
- `exercise-145`、`147`：分别解析 `ぶちかませ` 和 `続け` 的实际命令形；`あのバカどもに` 是跟随目标。
- `exercise-146`：`アル・ヒューマ` 作为喊出即施术的口令可独立成话；没有为它捏造唯一省略动词。
- `exercise-148`：前句危险预告为 `目をつむってください` 提供理由；礼貌形式仍具有临战紧迫性。
- `exercise-149`：实际表层 `聞いてたとおり` 来自 `聞いていたとおり`，表示现场观感与事前传闻一致。
- `exercise-150`、`165`：`くっ／あっ` 与 `てやあああ／ふん` 是非命题战斗呼声；尤其没有把 `てやあああ` 硬拆成て形或 `てやる`。
- `exercise-151`：`あれが…白鯨…` 是震惊下的识别片段，后置名词给指示对象命名，判断成分可由语境恢复。
- `exercise-152`：`でかさ` 是 `でかい＋さ` 的程度名词化，前置 `なんて` 构成超预期感叹。
- `exercise-153`：`ですか` 形成礼貌疑问，感受者由相邻问答恢复为昴。
- `exercise-154`、`155`：把 `ああ 怖いね` 接到下一行；`称賛される` 被动态修饰 `俺`，整个长名词短语以 `が` 补足 `怖い` 的对象，形成昴自夸式反转笑点。
- `exercise-156`：分开解析 `逃げまくる` 的反复／彻底、`てやろう` 的挑战性意志和 `ぜ` 的号召；结合诱敌计划，不误判为弃战。
- `exercise-157`：`とは` 标出敌人分心这一意外行为，`安く見られた` 是“遭到小看”的被动惯用语，`ものだ` 收束感叹评价。
- `exercise-158`：`射程を無視した` 是修饰 `無形の剣` 的连体从句，整段为由画面和后句补足的名词片段。
- `exercise-159`：按相邻顺序解析两层修饰：`百人一太刀で有名な` 修饰 `クルシュ様`，再由 `クルシュ様の` 限定中心名词 `剣技`。
- `exercise-160`：动作性名词 `散開` 在军令中直接充当口令，重复表示紧迫，不是客观叙述。
- `exercise-161`：`夢見てきた` 的 `てくる` 表示十四年来一路持续至当前，不是空间移动。
- `exercise-162`：`落ち` 是连接后项的连用形，`さらせ` 是命令形，两者构成连续的战斗宣告。
- `exercise-163`：`N風情` 是“区区 N 之流”的贬称；末尾 `が` 随省略谓语悬止并在怒吼中完成斥骂，不按“风景”义。
- `exercise-164`：`斬られに来る` 是被动加目的移动，`とは` 标意外；`協力的で結構` 是把敌人靠近反讽成主动送上门。
- `exercise-166`：`やべえ` 是 `やばい` 的粗口语元音变化，结合突发受袭表示局势不妙。
- `exercise-167`：`すんな` 展开为禁止形 `するな`，并分开说明粗犷句尾 `や`、辱称 `ダボ` 和悬止 `が`。
- `exercise-168`：`いっくぞ～` 是 `行くぞ` 的表演性促音化与拖长，不作为标准活用教授；`今だよ` 是全员行动的时机信号。

## 最终 QA

正式 validator：

```text
node regen\validate_candidates.mjs regen\batch26_exercises_letter.json regen\batch27_exercises_letter.json regen\batch28_exercises_letter.json
```

结果：

- files：3
- rows：120
- unique IDs：120
- errors：0
- warnings：0

附加检查：

- 精确五字段 schema：120/120；空字段：0。
- 预期 ID 顺序完全一致；缺失、额外、重复：均为 0。
- 单字母答案残留：0。
- 学习侧 `prompt / answer / hint` 中的数据库、manifest、模板、源题、选项答案、内部 ID 等元话语泄漏：0。
- 零宽字符及 BOM 残留：0。
- 本批归一化后的重复 prompt、answer、hint：均为 0；prompt 与 answer 完全相同：0。
- 与其余 56 个本地 JSON 候选文件、2,509 条记录相比，归一化 prompt、answer、hint 精确重复：均为 0。
- 对归一化文本检查长度 28 的连续片段：本批内部及与其他候选之间的 prompt、answer、hint 复用命中均为 0。
- hint 完整包含 answer：0。
- answer 与 hint 的最长归一化公共连续片段最大为 6 个字符；达到 8 个字符的记录：0。

SHA-256：

- `batch26_exercises_letter.json`：`E8BBFADB8D3300FDEBF3EE3EA46AFE30525C3CC8CCD7E47664FC3FDD8B43A97A`
- `batch27_exercises_letter.json`：`DEFE6C10662E9B9E23ED73D6956964FFA3A151EFA017F6AB4480FE49749299A1`
- `batch28_exercises_letter.json`：`3A08407DF83DC9BD25548CF47C43429004513B614EEC16F1C148A057FE2BD944`

## 数据库安全附带告警

本次只读检查同时收到 Supabase advisory：`maintenance.regen_20260727_learning_exercises_backup`、`maintenance.regen_20260727_card_enrichments_backup`、`maintenance.regen_20260727_manifest` 三张表当前未启用 RLS。此问题与本批内容生成无关，本次没有自动修改；启用 RLS 前需要先决定访问策略，否则直接启用会阻断现有访问。

## 冻结与释放

三个候选文件及本报告已完成生成侧 QA，现冻结释放给独立 reviewer。释放后生成者不再编辑，除非 reviewer 明确退回具体问题。数据库仍保持未写入状态。
