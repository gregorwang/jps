# 同音の部屋 · 交接（0.28.0）

辞書 词汇页的第三档「同音」+ 同音の部屋 + 听原声猜字 + 词卡底部「同じ音」一行。画布：「辞書 · 同音の部屋（预览）」页。

## 数据
- 本地脚本 `archive-content-sources/homophones/`（gitignore）：`dump.py` 从 `dict_vocab.json` + `dict_freq_vocab.json` 找读音相同、且不是纯假名的词，写 `candidates.txt`；**`groups.txt` 是手工整理的结果**（一行一组：`读音 | 字 中文 cluster cut kana | … || 说明`）；`build.py` 校验后写 `assets/homophones.json`。
- `cluster`：同一个非零数字 = 同一个日语词的不同写法（取る/撮る）；0 = 只是同音。`cut` = 这个词在 `vocab_lines.json` 台词里要挖空的原文片段，`kana` = 它的读音；台词对不上这个词就写 `-`，不出题。
- 台词来自 `vocab_lines*.json`（按词 id），App 运行时按 id 查。辞書加了新词，就重跑 `dump.py`，把新出现的组手工补进 `groups.txt`，再跑 `build.py`。

## 代码
- `ui/words/Homophones.kt`：模型、`HomophoneRules`（按级别筛、出题）、`Homophones`（asset 加载）、`HomophoneRoom`（哪个屋子开着，进程级 StateFlow）。
- `ui/screens/library/HomophoneScreens.kt`：`HomophonePage`（辞書列表）、`SameSoundRow`（词卡一行）、`HomophoneRoomHost`（在 `LabApp` 里，全屏 Dialog：屋子 + 测验）。
- `LevelDictUi.kt` 的 `SourceSwitch` 改成 `DictSource` 三档。

## 坑（这次遇到的）
- `vocab_lines` 是按「词形出现在台词里」自动配的，复合词会误配：放す→解放する、悪→気持ち悪い、空く→あいてっ、奥→奥さん、額 的词条其实是 ひたい。新加组时逐句看一遍，别直接信。
- 近义的一对（意志/意思）不出题：答案不唯一。
- 没做：音调（箸/橋/端）、只差一个音的「最小对立」（かし/がし、来て/切手）听力。

## 0.28.1 修的两个声音问题
- **原声对不上台词**：`build_lines.py`（vocab-cards-v1）和 `build_tango.py`（jlpt-freq）原来按「作品 + 集 + `source_line_no`」去对 `subtitle_lines.line_no`，但这两个不是一套编号，4183 条里只有 347 条真对得上，単語和同音页的大部分「原声」是别人的声音。现在按**台词文字**对（去空白后完全相等），带原声的整句也直接当候选台词。以后谁再用 `audio_sentences.json`，都别按行号对。
- 重跑后挑句会选到复合词（上げる→積み上げる、深い→興味深い），所以 `homophones/pinned_lines.json` 钉住了同音题每个词手工核过的那句（原声按文字重配），`build.py` 先把钉住的写回两个 `vocab_lines*.json` 再校验。**重跑 `build_lines.py` / `build_tango.py` 之后一定要再跑一次 `homophones/build.py`。**
- **爱蜜莉亚语音**：大字读音（あげる 这种纯假名）语音包里 197 个只有 10 个，点了就走 TTS。现在点读音念组里第一个有语音包的字（同音，听起来一样）；测验里没原声、有语音包的句子标「エミリア」。缺的 557 条登记在 `VOICE_BACKLOG.md`。
