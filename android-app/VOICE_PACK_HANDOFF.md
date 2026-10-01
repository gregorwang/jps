# 爱蜜莉亚语音包 · 修 bug 交接

0.16.0 上线了「设置 → 接続 → 语音包」：用户导入一次 `emilia-voice.zip`，之后凡是走合成语音（TTS）的地方，只要这句话在包里就播爱蜜莉亚的声音。用户装好后试用发现了一些 bug，这份文档是给**专门修这些 bug 的新会话**看的。

**先让用户说清楚 bug**：哪一屏、点了什么、哪个词或哪句话、听到了什么（念错？没声音？还是微软的声音？），有截图最好（放 `android-app/photo/`）。下面的「已知问题」是写这份文档时已经发现的，用户说的很可能就在里面。

## 1. 它是怎么工作的

```
Supabase 文本 ──batch_items.py──> batch_items.json（要念的清单，key = sha1(原文.strip())[:16]）
          ──batch.py（Modal）──> SBV2 生成 → 句子再过 Seed-VC 音色转换 → Whisper 回听 → Opus
          ──> out/manifest.json + out/opus/<key>.ogg ──pack_tools.py build──> emilia-voice.zip
手机：设置 → 语音包 → 选 zip → 解压到 filesDir/voice-pack/
播放：LessonAudioController.playTts(text) → VoicePack.fileFor(text) 命中就播 ogg，否则走本机 / 远程 TTS
```

- **App 代码**（都在 `app/src/main/java/com/animejapaneselab/nativeapp/`）：
  - `ui/voicepack/VoicePack.kt`：导入（SAF 选 zip，先解到 `voice-pack.part` 再替换）、`fileFor(context, text)` 查找、`keyOf`（`text.trim { isWhitespace }` 取 SHA-1 前 16 位，和 Python `str.strip()` 一致）。
  - `ui/audio/AudioControllers.kt`：`playTts` 最开始查语音包，命中就调 `playFile`（本来是远程 TTS 播缓存文件用的，抽出来共用）。
  - `ui/screens/settings/SettingsScreen.kt`：「接続」组里的「语音包」一栏（导入 / 重新导入 / 移除）。
- **生成端**：`../archive-content-sources/emilia-voice/`（gitignore，只在本机）。整个声线项目的来龙去脉在同目录的 `HANDOFF.md`；这里只需要：
  - `batch_items.json`：清单，每条 `key / text（App 会念的原文）/ say（模型实际念的）/ kind / emo`。`kind` 有 `word`（词头）、`form`（词卡变形，当单词处理）、`grammar`、`sentence`。
  - `forms_in.tsv` → `forms_out.tsv`：词卡变形的来源（见第 2 节 0 条），`batch_items.py` 读 `forms_out.tsv`。
  - `batch_checks.json`：每条的 Whisper 回听结果 `hyp / cer / ok`。
  - `out/manifest.json` + `out/opus/*.ogg`：已生成、已通过的音频（13162 条，打包时再减去 `pack_drop.json`）。
  - `recheck_words.py` / `recheck_words.json`：按读音复查单词和变形（第 2 节 1 条），json 里分三组 `rescue`（收回）/ `misread_in_pack`（在包里但读音不对，已剔除）/ `still_rejected`（从没进包）。
  - **`problem_words.tsv`：人能直接看的问题清单**（Excel 可开）：状态 / 类型 / 原文 / 该念 / Whisper 听成，共 2615 行（单词 978、变形 1391、语法例句回听不过 246）。
  - `pack_drop.json`：打包时排除的 key（只有 key，要看是哪个词查上面的 tsv 或 `pack_tools.py lookup`）。
  - `pack_tools.py`：**本地工具，不花钱**（见第 3 节）。
- 包里现在是（**2026-10-01 第四版，28525 条，207MB**）：第三版 + 有原声的台词 `line` 3864（`learning_sentences.ja_text`）+ 原作页挂原声的字幕行 `subline` 1188 + K-ON 全部字幕 `kon` 4310。这三类**不做 Whisper 回听**（`batch.py::lines`），App 的声音按钮能切回原声 / TTS（`ui/voicepack/VoiceChoice.kt`，三档：原声 / エミリア / TTS，TTS 档跳过语音包）。实际约 2 万 L4 GPU 秒。
- 第三版是（2026-10-01，19163 条，127MB）：单词 3006 / 3466、词卡变形 7753 / 8239、语法例句 2685 / 2705、台词 4717 / 4832、语法句型 1004 / 1226（`kind=pattern`，第三版新增）。（2026-09-29 第二版是 11755 条。）清单来源：单词 `learning_vocab_items.surface`（念的是 `vocab_cards.json` / `reading` 的假名 +「。」）、语法 `learning_grammar_points.ja_example`、台词 `learning_sentences.ja_text`（只取没原声的，还没批量生成）。

## 2. 已知问题（写文档时已经发现）

0. ~~**词卡的变形（ます形 / 意志 / 假定 …）全都没有**~~：已补（2026-09-29）。变形是 App 用 `Conjugator` + `FormDial` 现场推的，Supabase 里没有，所以第一版清单漏了。做法：`forms_in.tsv`（词头 / 读音 / 词性，从 Supabase + `vocab_cards.json` 导出）→ 临时写一个 JVM 单测调 `FormDial.of(Conjugator.tableFor(...))` 输出 `forms_out.tsv`（跑完删掉测试，不提交）→ `batch_items.py` 把它加成 `kind=form`（当单词处理：不做音色转换）→ `modal run batch.py::run --kinds form`。**改了 `Conjugator` / `FormDial` 的输出，就要重导 `forms_out.tsv` 再补生成**。实际花费约 1.3 美元（SBV2 1488 GPU 秒、Whisper 3833 GPU 秒）。
   - 顺带发现的 App bug：词性标成动词的「戻りましょう」会推出「戻りましょおう」之类的怪形（词表数据问题）。
1. ~~**单词校验太松，有念错的混进包里**~~：已改成按读音判定（2026-09-29，`recheck_words.py`）。Whisper 结果和 `say` / 原文都转成平假名（pykakasi + MeCab/unidic-lite 两种读法，任一一致就算对），读音一致才进包：同音字被误删的收回 336 条，读音对不上的 1407 条记进 `pack_drop.json`。还缺 978 个单词、1391 个变形，逐条在 `problem_words.tsv`。
   - 问题的类型（抽样 60 条看的）：**多数是模型真念错**，① 吞尾音（愛されよう→愛されよ、守り抜こう→まもりぬこ、夢見ます→ゆめみま），② 近音替换（告げます→継ぎます、秘めろ→決めろ、頼る→頼れ、善意→戦意），③ 清浊 / 促音错（罰しない→はしない、強欲→こうよく、絶頂→せっちょ）；**少数是 Whisper 对孤立短词听错、其实念对了**（聡明→ソメイ 丢长音）。宁可退回原来的语音也不教错音，所以全剔；误剔的那部分可以人工 `pack_tools.py play` 听了再从 `pack_drop.json` 里拿出来。
   - **2026-10-01 已重录**（`batch.py::retake`，见第 3 节）：单词 / 变形 / 句型每条重录 3 + 3 次，按读音判，挑最好的一次；剩下的约 460 词、490 变形、220 句型（`retake_checks.json` 里能看到每次 Whisper 听成什么）多数是 Whisper 听孤立短词不准（制御塔→制御と、発作→ほさ），也有模型确实念不出（野蛮→やば）。再录收益很小，不建议再花钱。
   - 原来的计划：给这约 2400 条各重录一遍再按读音判（约 0.4 美元）。**注意 `batch.py` 的 `sbv2_chunk` 见到 `raw/<key>.wav` 已存在就跳过**，重录要先让它写到另一个目录（如 `raw2/`）或换 key 后缀，Whisper / encode 也要跟着读新目录；SBV2 每次推理本身有随机性，不用改参数。吞尾音类可以试着在 `say` 末尾多给一点停顿（「。」换成「。。」或加「…」），先 `--limit 30` 小批验证。
   - 语法例句缺的 290 句：246 句是回听字错率 > 0.3 没过（在 tsv 里），44 句是当时预算到了没做音色转换（第 4 条）。
   - 以下是原来的记录（已按上面的方法修掉）：单词的通过线是「回听结果和词头或假名的字错率 ≤ 0.5」，因为 Whisper 对单个词会随意写成汉字或假名。结果 603 个通过的单词字错率 > 0，里面有真念错的：齟齬→「そこ」、高校→「ここ」、黙る→「黙れ」、魔女因子→「魔女陰死」；也有只是同音字的（龍剣→龍拳，读音一样，没问题）。
   - 修法：把 Whisper 结果转成假名再和 `say` 比（本机装 `pyopenjtalk` 或 `pykakasi` 做汉字→读音），读音不一致的 `pack_tools.py drop`，然后 `build` 重新打包。最省事的临时办法：`python pack_tools.py build --word-cer 0`，只留完全一致的单词（会少 600 个左右，不在包里的会退回原来的语音，不会出错）。
2. **有些地方播的文字和清单对不上，所以没用上语音包**（听到的还是微软 / 手机的声音）。语音包按「App 传给 `playTts` 的原文」精确匹配，下面这些原文不在清单里：
   - ~~辞書语法页点句型，念的是 `pattern` 去掉「〜」（`GrammarPage.speak`），清单里只有例句。~~ 2026-10-01 已加进清单（`kind=pattern`，只收纯日文的句型，带 N / V / ＋ / 動詞… 占位的念不出来，跳过）。
   - 辞書台词、字幕页念的是 `parseSpokenLine(line.ja).text`（`LibraryScreen.kt`、`SubtitlesScreen.kt`）。2026-10-01：无原声的 `learning_sentences` 已全部生成，`batch_items.py` 同时收原文和去掉说话人标记后的文本；**有原声的台词、字幕页的全部字幕行没有生成**（量大，切 TTS 时退回原来的语音）。
   - 2026-10-01：単語卡里没有原声的台词（`vocab_lines.json` 里 audio 为空的，`TangoScreen.playTangoLine`）也加进了清单。
   - 学习卡 `StudyCardQuestion` 念 `node.japanese`，今日页念 `sample.ja`，自習场景句卡念 `item.jaText`：多数是有原声的台词或拼出来的句子，要逐个确认。
   - 用 `pack_tools.py lookup "<原文>"` 一查就知道：`listed=NO` 就是清单里没有这句。
   - 修法二选一：（a）把这些原文补进 `batch_items.py` 的清单，再生成（要花 Modal 的钱或在本机跑，见第 4 节）；（b）App 端查找时做规范化（比如去掉句末「。」、全半角空格统一），两边算 key 前做同样的规范化。**改 key 算法要两边一起改并重新打包**，否则全部查不到。
3. **没有办法在手机上看出这次播的是不是语音包**。排查时很难确认。建议在 `playTts` 命中 / 未命中时打一行 `Log.d("VoicePack", "hit|miss key text")`，用户连电脑时用 `adb logcat -s VoicePack` 看；或者在播放状态文字里区分「爱蜜莉亚语音」和「合成语音」（`postPlaybackState` 的 message）。
4. ~~**约 40 句语法例句没做音色转换**~~：2026-10-01 已补。

## 3. 本地工具 `pack_tools.py`（在 `archive-content-sources/emilia-voice/`，Git Bash 里加 `PYTHONIOENCODING=utf-8`）

```bash
python pack_tools.py stats                              # 清单 / 包里各多少条
python pack_tools.py lookup 齟齬 "それなら 私の出番じゃないかしら？"   # key、在不在清单、Whisper 听成什么、在不在包里
python pack_tools.py play 齟齬                          # 用电脑默认播放器放这条
python pack_tools.py drop 齟齬 高校                      # 记进 pack_drop.json，以后打包都排除
python pack_tools.py build [--word-cer 0] [--sent-cer 0.3]   # 按排除表和阈值重打 emilia-voice.zip
python -m modal run batch.py::retake [--kinds word,form] [--word-takes 3] [--sent-takes 1] [--check-only]
                                                       # 花钱：给还不在包里的条目录新的一次（raw/<key>__tN.wav），按读音 / 字错率挑最好的，
                                                       # 直接下载进 out/、写 manifest、从 pack_drop.json 拿掉；--check-only 只补回听
python recheck_words.py [--apply]                      # 按读音复查单词 / 变形：报告 → recheck_words.json；--apply 从 Modal volume 下载收回的 wav（免费）、本地转 opus、写 pack_drop.json
```

`recheck_words.py` 需要 `python -m pip install pykakasi fugashi unidic-lite`（已装）；下载 Modal volume 里的文件不花 GPU 钱，Git Bash 里要加 `MSYS_NO_PATHCONV=1`。

重打包后把 `emilia-voice.zip` 复制到用户「下载」，让用户在手机上「设置 → 语音包 → 重新导入」。**只改语音包不用发版**；改了 App 代码才走 `CLAUDE.md` 第 6 节发版。

## 4. 要重新生成音频时（花钱）

- 2026-10-01 用户说余额更新到 30 美元，重录类的任务不用先报价，直接跑。第三版共用约 2.8 万 L4 GPU 秒（约 6–7 美元）。**`modal` CLI 是独立安装的，没有 fugashi，`retake` 要用 `python -m modal run`**。按实测：SBV2 每条约 0.3 GPU 秒，Seed-VC 音色转换每句约 2 GPU 秒，Whisper 每条约 0.4 GPU 秒；L4 约 0.8 美元 / 小时，再加每个容器 1 分钟左右的模型加载。
- 生成入口：`modal run --detach batch.py::run --kinds word,grammar,sentence [--limit N]`（已完成的 key 自动跳过，只做新的；每阶段有 GPU 秒上限参数 `--sbv2-s --vc-s --asr-s`）；只回听不生成：`batch.py::check_rest`；取回：`batch.py::fetch`。
- 免费的办法是在本机 RTX 2060 跑推理（SBV2 约 1 亿参数、Seed-VC 约 2 亿，6GB 够），模型文件在 `emilia-voice/final/`，但 Windows 上要先装两套推理环境（SBV2 的 `pyopenjtalk` 在 Windows 上编译常出问题）。
- 音频是声优的声音：**不能提交进这个公开仓库**，只在 `archive-content-sources/`（gitignore）和用户手机上。

## 5. 经验（每次做完补几条）

**2026-10-01 · 第三版（重录 + 补清单，19163 条）**
- **促音判得太严，白花了一轮重录的钱**：第一轮和补回听时，句型按读音判对错，而「ってば」「ってもんだ」这类开头是「っ」的，Whisper 根本听不出开头的促音（ってば→てば），念对了也被判错。第二轮前我才加了规则（句型开头的「っ」不计），旧录音按新规则重判就过了。但第二轮仍给这些句型各重录了 3 次，那部分钱白花。**教训：改了判定规则，先在本地用 `retake_checks.json` 把已有录音重判一遍（免费），只给重判后仍不过的条目重录。**
- **报价低估**：按「GPU 秒 × L4 0.8 美元/时」报 5–6 美元，实际约 9 美元。原因是没算容器的 CPU / 内存、模型加载、试跑和补跑。以后报价 = GPU 秒估算 × 1.5，试跑和补跑都算进去。`modal billing report` 当天查不到，以用户看到的账单为准。
- **GPU 秒上限太紧**：回听上限 9000 秒平分到 8 个容器，有 3 个提前停了，只好再开一轮只回听（`--check-only`），又付一次加载费。上限是防失控用的，设成估算的 2 倍。
- **`modal` 命令是单独装的，没有本机的 fugashi / pykakasi**：`retake` 的本地部分要 import `recheck_words`，第一次试跑直接报错。用 `python -m modal run`。
- **判定**：Whisper 听孤立短词很不准（促音、长音、同音字），单词 / 变形 / 句型一律按读音比，不按字比；句子也加了读音比对（字错率 ≤0.3 或读音错率 ≤0.2）。录 6 次还过不了的基本是 Whisper 听错或模型确实念不出（野蛮→やば），别再花钱。
- **重录的做法**：新录音用新文件名 `<key>__tN.wav`，不覆盖旧的；每次回听记进 `retake_checks.json`，下一轮只加新的 take；成品先编码到卷上单独的 `opus_new/` 再整目录下载。**不要拉卷上的 manifest**，本地 manifest 里有只在本地编码的条目，会被覆盖。
- 换音色那一步有 335 句没出结果，日志里没有报错，原因没查（第二轮重录补上了）。下次先查清楚 `vc_chunk` 跳过的是「源文件不存在」还是别的。
- 写脚本时别在 bash heredoc 里写正则（`\{\\[` 被吃掉一层，去说话人标记的正则写错了），改用 Edit 或先写成文件；大列表先建索引，别每条都扫一遍全部日志。
- 别把猜测说成结论：我说 dots.tts「可能念错孤立单词」只是推测，用户据此就放弃了。推测要明说「没实测」，并给出最便宜的验证办法。

**2026-10-01 · 第四版（台词全量、不回听）**
- 字幕页一行的文字和它挂的原声句（`learning_sentences.ja_text`）经常不一样（worker 按 `source_line_no` = 字幕 `line_no` 挂，句子常是好几行拼起来的），所以两种文字都要进清单：`line` 和 `subline`。原作页播エミリア时先找这行文字，找不到再用句子的文字（`SubtitlesScreen` 的 `packText`）。
- 句子的音色转换实测每句约 2 GPU 秒、SBV2 约 0.3，和第三版一致；6 个容器跑 5500 句约 40 分钟。
- 两个 `lines` 不要同时下载：都会删本地 `out/opus_new` 再拉，并写 `out/manifest.json`。现在写清单前会重读，但下载目录仍共用，排队跑。
- **`import batch_items` 会把整个脚本跑一遍**（拉 Supabase、重写 `batch_items.json`），要用里面的函数就复制出来，别 import。

## 6. 做完之后

- 在本文第 2 节把修掉的问题划掉，或者写明怎么修的。
- 改了 App 代码：`v3build.ps1 -Mode compile` → commit → 需要时按 `CLAUDE.md` 第 6 节发版，`CHANGELOG.md` 写清楚。
- 在本文第 5 节补几条经验（语音包的经验只写这里，不写进 `CLAUDE.md`）。
