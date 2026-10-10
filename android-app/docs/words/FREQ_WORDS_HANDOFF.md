# 辞書 高频补充 · 交接

2026-10-01 做的（进 0.23.0）。用户要「动漫里的高频词，取 JLPT N5–N2（N1 先不做），3000 个够了；语法 500 个左右；都要有爱蜜莉亚的读音」，放在**辞書按级别**（原作 / 高频补充 切换），能配上原作台词的再进**単語**。

## 数据在哪

生成目录 `archive-content-sources/jlpt-freq/`（gitignore，只在本机），按顺序：

| 脚本 | 做什么 |
|---|---|
| `pick_words.py` | `jiten_anime.csv`（Jiten 动漫词频，CC BY-SA）× `openjlpt.sqlite`（OpenJLPT 的 N5–N2 词，CC BY-SA）→ 按动漫频率取前 3000 → 去掉 `dict_vocab.json` 已有 → `pick_words.json`（2123） |
| `zh_*.tsv` | 主会话手写：`序号 \t 中文 \t [常用写法] \t [读音]`，中文为 `-` = 删掉 |
| `build_words.py` | → `words.json`（2116，词性用 App 的标签：动词 / 名词 / な形容词…） |
| `pick_grammar.py` → `merge_grammar.py` | Hanabira（CC BY）N5–N2 + OpenJLPT 补充，去掉辞书已有 → `grammar_src.json`（445） |
| `gz_*.tsv` → `build_grammar.py` | 主会话逐条重写：`序号 \t 句型 \t 意思 \t 接续 \t 例句 \t 例句中文` → `grammar.json`（342） |
| `build_assets.py` | → `assets/dict_freq_vocab.json`、`dict_freq_grammar.json`（worker JSON 形状，`LevelDict` 读） |
| `build_tango.py` | 给每个词从两部番字幕里挑 1 句（fugashi 原形 + 读音都对上，单字汉字要单独出现，有原声优先）→ `assets/vocab_cards_freq.json`、`vocab_lines_freq.json`（1486 个词，640 句有原声） |

App：`LevelDict.Loaded.freqVocab/freqGrammar`，`LibraryScreen` 的 `SourceSwitch`（`LevelDictUi.kt`），`VocabCards` / `TangoLines` 把 `*_freq.json` 合并在主文件下面（主文件重新生成也不会丢）。搜索（`CommandPalette` Dict 范围）也索引了这些词条。许可证记录在 `docs/assets-license.md`。

语音：`emilia-voice/batch_items.py` 的 `jword`（词，按读音回听，录 4 次取最好）、`jgram`（语法例句，音色转换，不回听）。词 1761 / 2059 在包里，例句 341 / 341。

## 经验 / 坑

- **源数据错得多，讲错不行**：Hanabira 的例句有大量错误（静かく話す、降っているだから、止むそうにない、待ってあげく），不少条目其实是单词（どこ、なに、もう）。所以每条语法都重写，只留 342 条，没凑 500。OpenJLPT 的英文释义有同音词配错（どうか→銅貨、せめて→攻め手、いち→市），中文按动漫用法写。
- 按读音匹配词频会把单字汉字匹配到助词（野→の、葉→は）：汉字词只按写法匹配，假名词才按读音。
- 读音取动漫里最常见的那个（私 わたくし→わたし），但词表本身的读音也有离谱的（後 ご、四 し、七 しち），在 zh 表第 4 列改；**改了读音的词，语音包里旧读音的录音要先清掉回听记录再重录**（`batch.py::retake --only 後,四 --redo`），不然会挑回旧录音。四、七 最后没录成，退回微软语音。
- 字幕匹配：只认字面会配错（辛い 配到「言いづらい」，輪 配到「レムりん」，刀 配到「彫刻刀」，違い 配到「違います」），改成原形 + 读音都对。unidic-lite 自己也会读错（あの方 → ほう），少量，没管。
- 生成 `zh_*.tsv` 时直接看着 `words_src.tsv` 写，250 个一段，一段一个文件；中断了从缺的序号接着写（`build_words.py` 会报缺哪些）。

## 还没做

- 単語排序没区分原作词和补充词（`TangoRules.rank` 按出现次数，补充的高频词会排得靠前）。用户觉得太基础的话，可以把 `jf-` 开头的词放到同一档的后面。
- 辞書卡片上没标「补」：靠切换就分开了。
- 298 个词 Whisper 一直判不过，不再花钱重录。
