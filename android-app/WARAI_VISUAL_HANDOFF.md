# 第十三巻 笑い · 画布新画法落地 · 交接

用户 2026-10-10 看了画布第二版，说「这不就好多了」。**画布即方案**：https://claude.ai/artifact/Jg2qJ6cRYYDrBms1GmKijm （Artifact `read` 读；每屏一个 `project/<名>.dc.html`，便签在 `project/canvas.json` 的 notes 里）。照画布实现，小偏差自己定。背景和数据见 `KYOKA_HANDOFF.md` 第 9 节。

## 规矩（用户定的）

- 视觉语言是全书**一根线**：直线 = 常理，偏出去 = ボケ，折回来 = ツッコミ，笑落在折点（工作色实心圆 + 白色「笑」）。**不要用组件库的素材**（Avatar、SpeechBubble、speechLines 都不要），**不要漫画风**；用 Compose `Canvas` 自己画线（1.5dp 墨线，折回那段 3dp 工作色）。
- 颜色只用 `AjlTheme.colors` / `AjlTheme.work`，浅色、深色都要能看。
- 只跑 `design/v3build.ps1 -Mode compile`。**不 commit、不发版**（用户说「提交」才 commit，说「推送更新」才发版）。工作区里 `RADIO_HANDOFF.md`、`radio_tracks.json` 等别的会话的改动不要动。
- 数据在 `archive-content-sources/kyoka/book_warai.py`，改完跑 `python build_kyoka.py --ro --book warai`；`kk_common.py` 的 `SENT` 要加字段。其他书的 asset 不能变：改前备份 `kyoka_books.json`，改完逐本比对。

## 要做的（按顺序，每块编译过了再做下一块）

1. **对话段 track 画法**（画布 `W01Before` / `W01After`；w01、w05、w08、w09、w16 的 passage）
   - 数据：`SENT` 加 `who`（说话人，可空）；passage 加 `style`（`track` / `tension` / 空 = 旧样子，读解几本不受影响）。线上位置由 role 推：ボケ、乗る = 偏出去；ツッコミ、翻る 的第一句 = 折点；其余在线上。
   - `KySent` / `KyPassage` 加字段（`ui/katsuyou/KatsuyouBook.kt` 的 `passage` 解析）。
   - `PassageSitting`：左边 56dp 宽一栏画线。作答前是直的虚线 + 编号圆圈；揭晓后，偏出去的句子整块右移 32dp，线弯过去，从它斜着折回到下一句的节点，节点换成「笑」圆点。每句上方一行小字说话人。题干改成「哪一句偏出去了？」。
   - 有原声的句子（奥托 ep32 那三句）：说话人旁放小波形，点了念（`lineCue`）。
2. **紧张曲线 tension**（`W13After`；w13 的 3 题）：每句加 `lv`（0–3：①1 ②2 ③2.5 ④3 带裂纹 ⑤2 ⑥0 ⑦0.5）。揭晓后在台词列表上方画 176dp 高的折线，崩下来那一格放「笑」圆点；列表变紧凑行（序号、说话人、日文、中文、右侧标签）。题干改成「紧张在哪一句一下崩掉？」。
3. **「前情」条 + 选项线形**（`W02Before` / `W02After`）：
   - show 题加 `ctx` 字段，画成台词卡上方浅蓝底（`colors.infoSoft`）的「前情」条，选完也留着。w02 现在写在 ask 里的场景挪进 ctx，ask 只留「她刚才是哪一种偏法？」。
   - 选项加可选 `glyph`（`tennen` / `toboke` / `kanchigai` / `bousou`）。有 glyph 的题，选项排成 3–4 格的方块，每格是线形 + 名字，揭晓后每格下面写 why。四个线形的画法照画布 SVG。
4. **ダジャレ 揭晓面板**（`W10After`）：pick 题加可选 `split = {sound: 'いって', a: ['行って','去 · 记住名字再走'], b: ['逝って','去世 · 记住名字再死'], hit: 'b'}`，揭晓后在台词卡和选项之间画：拍格 → 两条弧 → 两个词，命中那条用工作色加「笑」。w10 两题都配上（友人 / ユージン 那题：sound ゆうじん，a 友人，b ユージン）。
5. **課前の一眼两种新版式**（`Peek05`、`Peek10`）：`glance` 加 `kind`：`track`（一根线 + 编号节点 + 图例三行，w05 用；w01、w03 也可以用）和 `mora`（两行拍格对齐，连线标相同的拍，多出的拍用 ok 色虚框，w10 用）。`PeekGlance.kt` 按 kind 分支，旧的照旧。w05 第 3 条 beat 改成「比直接否定多偏出去一段，**摔回来更重**」。
6. **四层楼选项**（`W16After`）：LAYER 题（w16）的选项画成楼层：屋顶 + 四格，左边写层号，右边一个向下的箭头「越往下，越要懂日语」。揭晓后这句台词显示在正确那一层里。数据加 `layout: 'floors'` 即可。
7. **连线提示**：`ConnectSitting` 左边没有接头（pre 为空）时，连错提示写「对不上：「X」不是这个」。

## 做完

- 每块做完都编译；全部做完更新 `CHANGELOG.md`「未发布」第十三巻那一节（写这本的新画法）、`KYOKA_HANDOFF.md` 第 9 节「画布和待定的代码改动」改成「已做」。
- 只能实现一部分时，在本文档列出做到哪一步。

## 状态（2026-10-10）

7 块全部做完，每块 `-Mode compile` 过了。**没 commit、没发版**。其他八本 asset 逐本比对，一字未变；`voice_items.json` 里第十三巻还是 87 条（没加新的要念的句子）。

- 代码：`screens/katsuyou/WaraiLine.kt`（track、tension、「笑」圆点、说话人旁的小波形）、`WaraiPick.kt`（前情条、四种ボケ线形方块、ダジャレ 揭晓面板、四层楼）、`WaraiPeek.kt`（課前の一眼 track / mora）；`PassageSitting` / `PickSitting` / `PeekGlance` 只加了分支；`KatsuyouBook.kt` 加字段（都有默认值，旧数据照旧）；连线提示在 `KyMachines.kt`。
- 数据：`kk_common.py` 的 `SENT`（who / lv / at，有 who 的句子按原文查原声）、`PASSAGE`（style，track 按 role 推 at：ボケ・乗る・オチ = off，紧跟着的 ツッコミ・翻る・フォロー = snap）、`GLANCE`（track= / mora=）、`TRACK`、`MORA`；`book_warai.py` 的 `BOKE`、`SPLIT`、`LAYER`（layout floors）。

自己定的小偏差：
- w01 奥托第④句的 role 从 ツッコミ 改成 フォロー（照画布）。w09 雷姆・拉姆的「一」「二」两句手动标 off（同一个句式偏了两次），w08 的 オチ 当作偏出去、フォロー 当作折点。
- 说话人没核实的不写名字：「有人」「提名字的人」「接话的人」，w08 的 100 円例句写 A / B；さわ子那段 ⑤ 紬、⑦ 轻音部（照画布）。昴妈妈那段的紧张度是我定的（1.5 / 2.5 / 0 / 0.3 / 0.5）。
- 暴走 的线形画布上没有，自己画的：越来越陡、冲过一条虚线框、带箭头。
- tension 作答前也用紧凑行（画布只画了揭晓后）；課前の一眼「再看一个」那一行还是旧样子（画布上配了小线形，没做）；w03 没换成线（要配律自夸那几句，数据里没有）。

只能真机看的：track 的线和节点是按每句的实际位置画的，句子换行多的时候节点会不会贴边；ダジャレ 面板在窄屏（360dp）上 ユージン 那题右边的字够不够放；四层楼右边箭头是固定 260dp 高，楼层文字换行时会不会短了一截；mora 拍格在窄屏上按宽度缩小（最大 44dp）。
