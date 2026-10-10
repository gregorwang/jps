# 第四巻 造語 · 交接

自習书架上的第四本教科書，内容来自用户的《日语词汇与构词法》（`Downloads/日语词汇与构词法 (1).md`，13 章 + 附录）。和知識卡不同，它不是让人看的：构词就是「拆」和「拼」，所以做成动手的交互。画布「自習 · 第四巻 造語（预览）」页是用户认可的样子（`ZgIndex` / `ZgFuseGuess` / `ZgFuse` / `ZgMatrix`）。

## 1. 现在有什么（0.25.0）

| 组 | 玩法 | 课 | 文档章节 |
|---|---|---|---|
| 一 音が変わる | 拼合台：两块零件 → 先猜怎么读 → 合起来，变了的音亮起来 | 01 連濁 · 02 母音が変わる · 03 あいだに s · 04 「っ」が入る | 第 6 章（连浊、被覆形、音韵添加）、第 10 章（促音便） |
| 二 動詞をつなぐ | 组合矩阵：前项 × 后项，点列头看后项的核心义，点格子看词 | 05 〜出す・込む・直す・合う · 06 〜切る・抜く・返す・続ける · 07 取り〜・引き〜・打ち〜・差し〜 | 第 7 章 |

- 每课学完（拼合台做完最后一组；矩阵把每个词都点开过后做 6 题小测）在つづく页记为已学，才进練習。
- 練習 → 言語学 → 「造語」（VolumeSwitch 第 4 格；为了放下 4 格，标签改成了 台詞 / 基礎 / 活用 / 造語）：从已学的课里出 10 题，读音题（拼合）+ 意思题（矩阵），答错多的、没答过的先出。
- 声音：词点一下念（エミリア 语音包 → TTS）；台词卡的波形就是播放键，有原声的放原声，没有的走全局的声音偏好（エミリア / TTS）。

## 2. 代码

- `ui/zougo/ZougoBook.kt`：asset 模型和解析（`SegKind` = 罗马音片段变了什么：Voiced 连浊/半浊、Vowel 元音交替、Insert 插入的 s、Gem 促音、Already 挡住连浊的浊音）。
- `ui/zougo/Zougo.kt`：进程级 holder（已学的课、点开过的格子、每个词的对错记录，存 `LocalLabStore.readZougo`）+ `ZougoRules`（小测 / 練習出题）。判对错的地方都走 `Zougo.answer`，里面调 `StudyLog.record`。
- `ui/screens/zougo/`：`ZougoScreen`（目次、一课的流程、練習的外壳）、`FuseSitting`（拼合台，`FuseStage` 小测也用）、`MatrixSitting`、`ZougoQuiz`、`ZougoParts`（`SegText` 上色罗马音、`PartTile`、`LineCard`、`WordHead`、`ZougoHeader`）。
- 接线：`JishuScreen`（书架多一本、`zougo.bookOpen` 时整页交给 `ZougoStudy`）、`LabApp`（上课 / 練習时隐藏底栏）、`LearnScreen` + `LinguisticsScreen`（練習第 4 卷）、`LinguisticsTrack.WordBuilding`。

## 3. 数据：`archive-content-sources/zougo/`（gitignore，本地）

- `build_zougo.py`：**课程内容就写在这个文件里**（主会话按文档写，不交给 Antigravity）。`python build_zougo.py` 校验（选项里有正解、矩阵的词以前项开头以后项结尾、台词里有目标词）并写 `assets/zougo_lessons.json` 和 `voice_items.json`；`--ro` 打印每句的罗马音供检查。
- 台词：`find_lines.py 词 词 …` 在两部番的字幕缓存（`vocab-cards-v1/cache`，38002 行 + 4290 句带原声）里找候选。和 `learning_sentences.ja_text` 一字不差的句子自动挂原声；字幕里有但没原声的标「原作」；都没有的自己写例句，标「例句」。现在 65 句：原作 34（原声 20）、例句 31。
- 罗马音：`romaji.py`（fugashi 切词 + 和 App `Kana.romaji` 同一套写法）。自动切词常错（复合动词被拆开、一本→ichipon、秋雨→shuuu），**每次加句子都要 `--ro` 看一遍**，错的写进 `RO_FIX`。
- 文档里的错处（已在数据里避开）：汉语前缀表里 元素、低迷、初期、対策、脱退 其实不是「前缀 + 词」；月夜 现代读 つきよ；足早 不是复合い形容词；静かさ 一般说 静けさ。

## 4. 爱蜜莉亚语音

- `voice_items.json`（词 + 没原声的句子，key = sha1(原文.strip())[:16]）→ 追加进 `emilia-voice/batch_items.json`，类别 `zword`（已加进 `batch.py` 的 `WORDLIKE`：只用 SBV2、不转音色、录 3 次按读音挑）和 `zgram`（转音色、录 2 次）。
- **句子里要教的词换成假名再交给模型**（`say` 字段：雨傘を… → あまがさを…，另有 一日→いちにち、辛くて→つらくて 等）：SBV2 看汉字会念错，而 Whisper 回听按汉字比对查不出来，偏偏这里教的就是读音。
- 跑法：`python -m modal run batch.py::retake --kinds zword,zgram --word-takes 3 --sent-takes 2 --containers 2`，然后 `python pack_tools.py build`，把 zip 复制到用户「下载」，手机上 设置 → 语音包 → 重新导入。

## 5. 还没做（按文档的计划）

- 三 対になる（成对开关）：08 開く／開ける（自他，が ↔ を）、09 ころころ／ごろごろ（清浊音象征）。还有 広がる／広まる、高まる／高める。
- 四 わな（翻牌：你以为 / 其实）：10 中日同形（大丈夫 144 次、約束 86 次、邪魔、迷惑，在 Re:ゼロ 里都很多）、11 和製英語（英语只当锚点）。
- 五 カタカナを戻す（还原：罗马音里多余的元音淡出）：12 ストライク → strike。
- 文档里适合做知識卡、不适合交互的部分（语种比例、音符、入声→ク/ツ、r→ニ/ジ、呉音/漢音、同音汉语词、和汉对应）还没做卡。
- 第二期：原作页点字幕里的复合词弹出拆解（文档 11.8 想法四）。
- 每加一种玩法：`ZgKind` 加一项、`ZougoBook.parse` 认新字段、`ZougoScreen.LessonFlow` 加分支、`ZougoRules.practice` 出对应的题；画布上先画一张新玩法给用户确认。

## 6. 经验

- 两部番里几乎没有文档里的传统例词（雨傘、春雨、酒屋 一个都没有），拼合台优先挑台词里真出现的词（友達、心強い、胸騒ぎ、上着、絶対、失敗……），文档的经典例子用例句补。挑词前先 `find_lines.py` 看一眼覆盖。
- 拟态词在对白里很少（ABAB 型大多是笑声和感叹），做拟态词课时别指望配原作台词。
