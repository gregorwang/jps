# 活用书重构（VOL.A–H，照造語）· 交接

用户 2026-10-03 定：自習书架上 A–H 八本活用书（72 课，`conjugation_lessons.json`）照第四巻 造語的思路重做成动手的交互，**一本做完发一版，用户在手机上试过再做下一本**。内容由主会话自己写，不交给 Antigravity。

## 1. 画布（都已经过用户确认）

画布页（只读要做的那一屏）：
- 「自習 · VOL.B 音便」 `OnFuseGuess` / `OnFuse` / `OnMap`
- 「VOL.G 縮約」 `ShukuGuess` / `Shuku`（还原台）
- 「VOL.A 活用盘 / VOL.F 形容词」 `DialGuess` / `Dial` / `AdjFuse*`
- 「VOL.C 换词 / VOL.D 叠积木」 `SwapGuess` / `Swap` / `Stack*`
- 「VOL.E 接续 / VOL.H 敬语」 `JoinGuess` / `Join` / `KeigoGuess` / `Keigo`
- 「八本重构：入口 / まとめ / 練習」：`RbHome` `RbIndex` `RbTsuzuku` `RbPractice`，各本总结 `SumA` `SumF` `SumG` `SumC` `SumD` `SumE` `SumH`
- 「八本重构：新题型」：`Peek`（課前の一眼）、`BackTrack`（B）、`SpeedDial`（G）、`SwipeSort`（A）、`FlipTrap`（F）、`TimeLine`（C）、`Evidence`（D）、`Connect`（E）、`Speaker`（H）

画板的生成脚本在 `archive-content-sources/conjugation-rebuild/canvas-gen/`（gitignore，里面有原作台词）。

**顺序**：B → G → A+F → C/D/E/H。**G 只讲「缩约 ⇄ 还原」**，不重复 C/D 里的意思。书架上叫 VOL.X，「巻」是練習那边的编号，别混用。

## 2. 现在有什么（0.26.0：VOL.B 音便）

一课 = 課前の一眼 → 拼合台 → 倒推 → つづく（`markLearned`）。目次每行有玩法标签，最下面「まとめ」= 词尾地图（て / た 切换）。

- 数据：`assets/katsuyou_lessons.json`，由 `archive-content-sources/conjugation-rebuild/build_katsuyou.py` 生成（`--ro` 打印罗马音检查）。**按 point_id 挂课**，所以已学、練習（活用题库）、今日時間割 都还走 `ConjugationDrillViewModel`，不用改。asset 里没有的课自动走旧的 板書 + 台詞。
- 代码：
  - `ui/katsuyou/KatsuyouBook.kt`：asset 模型（`KyLesson` = peek + fuse + back；`KyBook` = 玩法名、まとめ 行）。
  - `ui/katsuyou/Katsuyou.kt`：进程级路由（正在上的课 / 开着的 まとめ），答题调 `StudyLog.record`。
  - `ui/screens/katsuyou/KatsuyouScreens.kt`：`KatsuyouLesson`（流程）、`PeekScreen`、`BackTrackSitting`、`KatsuyouMap`。
  - 拼合台和造語共用：`screens/zougo/FuseSitting.kt` 现在收 `items/key/eyebrow/title/onAnswer`；`ZgFuse.ask` 是结果框里的问题；`SegKind` 多了 `Gone`（掉的词尾：揭晓前虚线、揭晓后划掉）和 `Bad`（例外，红）。
  - 接线：`JishuScreen` 的 `start()` 先查 `KatsuyouBook.lesson(point)`；`TextbookIndex` 的玩法标签和 まとめ 行；`LabApp` 上课时隐藏底栏。

## 3. 下一本怎么做

1. 在 `build_katsuyou.py` 的 `BOOKS` 里加一本（group = 组字母），写课（台词先用 zougo 的 `find_lines.py` 或直接在字幕缓存里搜，原声优先）。
2. 新玩法：`KyLesson` 加字段 + `KatsuyouBook.parse` 认它 + `KatsuyouLesson` 的 `Phase` 加一步 + `KatsuyouScreens` 加界面。画布上的画板就是规格。
3. G 的还原台、A 的活用盘 都是新组件；E/H 的总结表是新的 まとめ 形式（现在 `KatsuyouMap` 只会画 B 那种词尾地图）。

## 4. 还没做

- 爱蜜莉亚语音：`build_katsuyou.py` 写了 `voice_items.json`（34 条：词 + 没原声的句子），还没跑 `emilia-voice/batch.py`（要 Modal GPU，**先报价**），现在走 TTS。
- 練習里的新题型（画布 `RbPractice`：各本的「先猜」混在一起出）还没接，練習 仍是原来的活用题库。

## 5. 经验

- 两部番里て形台词很多（言って 248 句、聞いて 112 句），原声也够；た形少一些（急いだ、脱いだ、休んだ 一句都没有）。
- `romaji.py` 自动切词会把补助动词粘在一起（kaitearu、nanda、kiitehoshii），每句都要人看一遍写进 `RO_FIX`。
- 字幕里有全角空格（`とんでもない！　信じて…`），`learning_sentences` 匹配前先把 `　` 归一，显示用原文。
