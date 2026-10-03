# 第五巻起的教科書（助詞・口語・類義）· 交接

用户 2026-10-03 定：新来的日语文档**不再做知識卡**，做成自習书架上的交互教科書（照造語 / VOL.A–H 活用书的做法：先猜、再揭晓）。三本一起在 0.29.0 发布。

| 巻 | 来源文档（Downloads） | 課 | 玩法 |
|---|---|---|---|
| 第五巻 助詞 | 《日语助词与同义词辨析》第 1–10 章 | 10 | 台词挖空选助词、を/で・に/で・まで/までに・だけ/しか・ね/よ 分拣、场景里选 は/が 和 ね/よ、还原省掉的助词、**找错**（新） |
| 第六巻 口語 | 《日语语素词典与动漫口语词汇》第 5–13 章 | 7 | 能说 / 听懂就好 分拣、这句是哪一层、**听原声猜语气**（新）、すげえ→すごい 还原台、ぶっ〜/やがる、命令阶梯 + 换个人说、称呼 |
| 第七巻 類義 | 《助词与同义词辨析》第 11–15 章 +《语素词典》第 3 章 | 8 | 挖空、分拣、连线（汉语一个「看/戴」→ 日语一组）、程度阶梯 |
| 第四巻 造語 三組 | 《语素词典》第 1–2 章 | +3（z08–z10） | 前缀矩阵（不・未・非・無）、后缀矩阵（者・家・的・化）、呉音/漢音拼合台 |

和 VOL.C/D/E/H 重复的章节（条件四兄弟、から/ので、そう/よう/らしい、授受、敬语、缩约）没有重做；ため/おかげで/せいで、ために/ように、ことにする/なる 放进了 類義 第 7 課。同音异字（あける/かわる/はかる）辞書「同音」已有，没做。

## 1. 代码

- `ui/kyoka/Kyoka.kt`：asset 模型（`KkBook`/`KkLesson`，每课的玩法就是活用书的 `KyLesson`，用 `KatsuyouBook.lessonOf/tableOf` 解析）+ 进程级 holder `Kyoka`（已学的课 id，存 `LocalLabStore.readKyoka`；开着的书、课、まとめ）。**不走 ConjugationDrillViewModel**：这些课不是活用 point，没有题库台词。
- `ui/screens/kyoka/KyokaScreen.kt`：目次（`BookCover` 从造語借来，改成 internal 带参数）、一课的流程（課前の一眼 → steps → つづく，复用 `StepSitting`/`PeekScreen`）、まとめ（`KyTableScreen`，加了 `onBack` 参数）。
- 接线：`JishuScreen`（书架多三本，`kyoka.book != null` 时整页交给 `KyokaStudy`）、`LabApp`（上课时隐藏底栏）。
- 新玩法：
  - **找错** `KyStep.Spot` / `KySpot` → `screens/katsuyou/SpotSitting.kt`：句子拆成块，点错的那块；揭晓后错块划掉、上面浮出正确的（`fix` 为空 = 「去掉」）。
  - **听原声** `PickSitting` 的 head `listen`：选之前只有声音胶囊（`LineVoicePill`，跟全局的原声/エミリア/TTS 选择走，进来自动放一遍），选完才出字。

## 2. 数据：`archive-content-sources/kyoka/`（gitignore，本地）

- `kk_common.py`：在活用书的 `ky_common.py` 上加了 `LA`（同一个助词出现多次时指定挖哪一个）、`G`/`CTX`（挖空 / 带场景挖空，选项 `(词, 为什么[, mark])`，不写 mark 的错项自动记 `no`）、`SPOT`、`LISTEN`、`KL`（课：id、section、title、gloss）。
- `book_joshi.py` / `book_kougo.py` / `book_ruigi.py`：各一本，导出 `BOOK`（id、volume、title、sub、sections、table、lessons）。
- `python build_kyoka.py [--ro] [--book joshi]`：校验 + 写 `assets/kyoka_books.json` 和 `voice_items.json`。台词照活用书的规矩挂原声（`grep_lines.py` 在 `../conjugation-rebuild/`）。
- 造語第三组在 `../zougo/build_zougo.py`（`M08`、`M09`、`L10`），照旧 `python build_zougo.py --ro`。

## 3. 文档里讲错、已在数据里改正的地方

- 友達 不是熟字训（とも＋たち 连浊）；認 读 ニン 是呉音不是漢音；情 读 セイ 的例子（表情、情勢）都不对；定食 读 ていしょく；「思う 后面接から句」应是「と」。这几处都没上卡，讲解里按正确的写。

## 4. 还没做

- 練習 tab 还没有这三本的题（造語有）。要做的话照 `ZougoRules.practice`：从已学课的 steps 里抽题混出。
- 用户真机试玩后的反馈：找错的块大小、听原声默认放エミリア是否合适（语气题建议原声，胶囊上有提示）。
- 爱蜜莉亚语音：见 `VOICE_BACKLOG.md`。

## 5. 经验

- `PEEK` 的结果字（30sp）加上左边几个方块，一行放不下就被截断：结果最多 4–5 个字，方块最多 3–4 个。
- bash heredoc 塞长 Python 会被截断，长内容直接用 Edit 改 `book_*.py` / `build_zougo.py`。
- 罗马音里的 `'`（nan'youbi）要用双引号字符串。
