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

## 2. 现在有什么

### 进度（0.27.0 一次发：G → A+F → C → D → E → H 全做完再发）

| 本 | 課数 | 状态 | 玩法 | まとめ |
|---|---|---|---|---|
| B 音便 | 5 | 0.26.0 已发 | 拼合台 + 倒推 | 词尾地图（`KatsuyouMap`） |
| G 縮約 | 6 | 内容已写（`book_g.py`） | 還原台 + 语速档位 | 缩约对照（表，遮る，点行听台词） |
| A 活用形 | 10 | 未做 | 活用盘 + 分拣 | 五段活用表 |
| F 形容词 | 7 | 未做 | 拼合台 + 翻牌 | 两列表 |
| C 补助动词 | 17 | 未做 | 换词 + 时间轴 | 补助动词地图 |
| D 助动词 | 12 | 未做 | 叠积木 + 证据 | 接续顺序 |
| E 接续 | 10 | 未做 | 接续 + 连线 | 条件四兄弟 |
| H 敬语 | 5 | 未做 | 敬语阶梯 + 换个人说 | 敬语一览 |

### 数据（`archive-content-sources/conjugation-rebuild/`，gitignore）

- `ky_common.py`：共用工具。`line(ja, target, zh)` 自动挂原声（`AUD_ALIAS` 处理原声字幕里的错字），`RO_FIX` 手写罗马音，`S()` 片段，`O()` 选项，`PICK()` / `SHUKU()` / `SPEED()` / `step()` / `lesson()`。
- `book_<x>.py`：一本一个文件，导出 `BOOK`。`build_katsuyou.py` 按 `ORDER` 把存在的都装进 asset（`version: 2`），`--ro --book G` 打印这本的罗马音。
- `grep_lines.py <正则>`：在两部番字幕里找台词，★ = 有原声。
- `voice_items.json` 每条带 `book` 字段（语音积压按本登记）。

### 代码

一课 = 課前の一眼 → `steps`（按顺序）→ つづく（`markLearned`）。asset 里一课是 `{point, peek, steps:[{type, title, items…}]}`。

- `ui/katsuyou/KatsuyouBook.kt`：模型。`KyStep` = `Fuse`（拼合台，和造語共用）/ `Back`（倒推）/ `Pick` / `Speed` / `Dial` / `Swipe` / `Flip` / `Stack` / `Connect`；`KyBook.table`（表格まとめ）或 `rows`（B 的词尾地图）。
- `ui/screens/katsuyou/`：
  - `KatsuyouScreens.kt`：`KatsuyouLesson`（按 steps 走）、`StepSitting` 分发、`PeekScreen`（按钮文字 = `peek.go`）、`BackTrackSitting`、`KatsuyouMap`（有 `table` 就转给 `KyTableScreen`）。
  - `PickSitting.kt`：**通用「几选一」**。`head` = `line`（台词挖空）/ `shuku`（缩约 → 完整）/ `timeline` / `context` / `speaker`；`layout` = `rows`（揭晓后每项显示 why 和 原作✓/也说得通/不行）/ `chips`（揭晓后可点别的选项换句子，选项带 `line`）/ `columns`（按 `group` 分栏）/ `ladder`（敬语阶梯）。`mark == "ok"` 算答对。
  - `KyMachines.kt`：`SpeedSitting`（语速档位）、`DialSitting`（活用盘，answer 5 = 下面那个额外按钮）、`SwipeSitting`、`FlipSitting`（不判对错）、`StackSitting`（积木，`meanings` 按已叠的 id 串查）、`ConnectSitting`。
  - `KyParts.kt`：`KySitting`（页头 + 滚动 + 唯一墨色按钮）、`PickTile`、`SlotLine`（带空格的台词卡）。
  - `KyTableScreen.kt`：表格まとめ，`select` = `col`（标签切列，显示 `colNotes`/`colLines`）/ `row`（点行显示台词）；`cover` 列可遮；`sections[].rail` 画顺序轨道；`notes` 脚注。
- 目次 / 自習首页的数量显示用 `KyLesson.count`（所有 step 的题数），玩法标签用 `KyBook.play`。

## 3. 下一本怎么做

1. 写 `book_<x>.py`（照 `book_g.py`），在 `build_katsuyou.py` 的 `ORDER` 里已经有它的字母，跑 `python build_katsuyou.py --ro --book X`，罗马音不对的写进 `RO_FIX`（这本文件里 `RO_FIX.update`）。
2. 需要新画法时：`KyStep` 加一种 + `stepOf` 解析 + `StepSitting` 分发 + 在 `KyMachines.kt` 写界面。能用 `PickSitting` 的就加 `head` / `layout`，不要新开一个界面。
3. 编译 → commit → 在本文件进度表改状态，在 `VOICE_BACKLOG.md` 加一行。

## 4. 还没做

- 爱蜜莉亚语音：见 `VOICE_BACKLOG.md`，现在都走 TTS。
- 練習里的新题型（画布 `RbPractice`：各本的「先猜」混在一起出）还没接，練習 仍是原来的活用题库。

## 5. 经验

- 两部番里て形台词很多（言って 248 句、聞いて 112 句），原声也够；た形少一些（急いだ、脱いだ、休んだ 一句都没有）。
- `romaji.py` 自动切词会把补助动词粘在一起（kaitearu、nanda、kiitehoshii），每句都要人看一遍写进 `RO_FIX`。
- 字幕里有全角空格（`とんでもない！　信じて…`），`learning_sentences` 匹配前先把 `　` 归一，显示用原文。
- G 的台词：缩约形在两部番里很多，原声也够（`grep_lines.py "なきゃ|なくちゃ"`）。原声字幕偶有错字（「無駄っだぜ」），显示改正、音频靠 `AUD_ALIAS` 对上。
- っち 的罗马音统一写 tch（itchau、okotcha），和片段 `S('it', ('cha', …))` 拼起来一致。
- 0.26.0 发布时 `build-content-pack.py` 连续两次 SSL EOF（抓 worker 时断开）；这次没改联网内容接口，就用 `-SkipContentPack` 沿用上一版的内容包发了。改了内容接口时不能这样跳过。
