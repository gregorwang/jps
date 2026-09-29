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
  - `batch_items.json`：清单，每条 `key / text（App 会念的原文）/ say（模型实际念的）/ kind / emo`。
  - `batch_checks.json`：每条的 Whisper 回听结果 `hyp / cer / ok`。
  - `out/manifest.json` + `out/opus/*.ogg`：已生成、已通过的音频（5113 条）。
  - `pack_tools.py`：**本地工具，不花钱**（见第 3 节）。
- 包里现在是：单词 2694、语法例句 2415、台词 4。清单来源：单词 `learning_vocab_items.surface`（念的是 `vocab_cards.json` / `reading` 的假名 +「。」）、语法 `learning_grammar_points.ja_example`、台词 `learning_sentences.ja_text`（只取没原声的，还没批量生成）。

## 2. 已知问题（写文档时已经发现）

1. **单词校验太松，有念错的混进包里**。单词的通过线是「回听结果和词头或假名的字错率 ≤ 0.5」，因为 Whisper 对单个词会随意写成汉字或假名。结果 603 个通过的单词字错率 > 0，里面有真念错的：齟齬→「そこ」、高校→「ここ」、黙る→「黙れ」、魔女因子→「魔女陰死」；也有只是同音字的（龍剣→龍拳，读音一样，没问题）。
   - 修法：把 Whisper 结果转成假名再和 `say` 比（本机装 `pyopenjtalk` 或 `pykakasi` 做汉字→读音），读音不一致的 `pack_tools.py drop`，然后 `build` 重新打包。最省事的临时办法：`python pack_tools.py build --word-cer 0`，只留完全一致的单词（会少 600 个左右，不在包里的会退回原来的语音，不会出错）。
2. **有些地方播的文字和清单对不上，所以没用上语音包**（听到的还是微软 / 手机的声音）。语音包按「App 传给 `playTts` 的原文」精确匹配，下面这些原文不在清单里：
   - 辞書语法页点句型，念的是 `pattern` 去掉「〜」（`GrammarPage.speak`），清单里只有例句。
   - 辞書台词、字幕页念的是 `parseSpokenLine(line.ja).text`（`LibraryScreen.kt`、`SubtitlesScreen.kt`），台词还没批量生成。
   - 学习卡 `StudyCardQuestion` 念 `node.japanese`，今日页念 `sample.ja`，自習场景句卡念 `item.jaText`：多数是有原声的台词或拼出来的句子，要逐个确认。
   - 用 `pack_tools.py lookup "<原文>"` 一查就知道：`listed=NO` 就是清单里没有这句。
   - 修法二选一：（a）把这些原文补进 `batch_items.py` 的清单，再生成（要花 Modal 的钱或在本机跑，见第 4 节）；（b）App 端查找时做规范化（比如去掉句末「。」、全半角空格统一），两边算 key 前做同样的规范化。**改 key 算法要两边一起改并重新打包**，否则全部查不到。
3. **没有办法在手机上看出这次播的是不是语音包**。排查时很难确认。建议在 `playTts` 命中 / 未命中时打一行 `Log.d("VoicePack", "hit|miss key text")`，用户连电脑时用 `adb logcat -s VoicePack` 看；或者在播放状态文字里区分「爱蜜莉亚语音」和「合成语音」（`postPlaybackState` 的 message）。
4. **约 40 句语法例句没做音色转换**（Modal 预算到了），它们不在包里，照旧用原来的语音，不算 bug。

## 3. 本地工具 `pack_tools.py`（在 `archive-content-sources/emilia-voice/`，Git Bash 里加 `PYTHONIOENCODING=utf-8`）

```bash
python pack_tools.py stats                              # 清单 / 包里各多少条
python pack_tools.py lookup 齟齬 "それなら 私の出番じゃないかしら？"   # key、在不在清单、Whisper 听成什么、在不在包里
python pack_tools.py play 齟齬                          # 用电脑默认播放器放这条
python pack_tools.py drop 齟齬 高校                      # 记进 pack_drop.json，以后打包都排除
python pack_tools.py build [--word-cer 0] [--sent-cer 0.3]   # 按排除表和阈值重打 emilia-voice.zip
```

重打包后把 `emilia-voice.zip` 复制到用户「下载」，让用户在手机上「设置 → 语音包 → 重新导入」。**只改语音包不用发版**；改了 App 代码才走 `CLAUDE.md` 第 6 节发版。

## 4. 要重新生成音频时（花钱，先问）

- Modal 已经花到用户定的上限（约 14.7 / 15 美元）。**任何 `modal run` 之前先报预估花费，并得到用户同意**。按实测：SBV2 每条约 0.3 GPU 秒，Seed-VC 音色转换每句约 2 GPU 秒，Whisper 每条约 0.4 GPU 秒；L4 约 0.8 美元 / 小时，再加每个容器 1 分钟左右的模型加载。
- 生成入口：`modal run --detach batch.py::run --kinds word,grammar,sentence [--limit N]`（已完成的 key 自动跳过，只做新的；每阶段有 GPU 秒上限参数 `--sbv2-s --vc-s --asr-s`）；只回听不生成：`batch.py::check_rest`；取回：`batch.py::fetch`。
- 免费的办法是在本机 RTX 2060 跑推理（SBV2 约 1 亿参数、Seed-VC 约 2 亿，6GB 够），模型文件在 `emilia-voice/final/`，但 Windows 上要先装两套推理环境（SBV2 的 `pyopenjtalk` 在 Windows 上编译常出问题）。
- 音频是声优的声音：**不能提交进这个公开仓库**，只在 `archive-content-sources/`（gitignore）和用户手机上。

## 5. 做完之后

- 在本文第 2 节把修掉的问题划掉，或者写明怎么修的。
- 改了 App 代码：`v3build.ps1 -Mode compile` → commit → 需要时按 `CLAUDE.md` 第 6 节发版，`CHANGELOG.md` 写清楚。
- 在 `CLAUDE.md` 第 8 节补几条经验。
