# 爱蜜莉亚语音 · 待生成清单

新做的题目、课程里有新词、新例句时，App 先用 TTS 念。等这里攒够一批，再一次性用爱蜜莉亚声线生成，然后重新打语音包。用户 2026-10-03 定：**攒着，后面批量跑**。

**预算**：Modal 余额约 **10 美元**（用户 2026-10-03 说的）。每次 `modal run` 前先报预估花费；能在本机 RTX 2060 跑的就在本机跑。
参考：2026-10-02 补 16450 条变形，约花 3–3.5 美元，相当于每 1000 条 0.2 美元左右。小批量时，容器启动的固定开销占大头，所以要攒着一起跑。

## 待生成

| 加入日期 | 来源 | 清单文件 | 条数 | 类别 | 状态 |
|---|---|---|---|---|---|
| 2026-10-03 | 0.26.0 VOL.B 音便（拼合台的词、词尾地图的て形、没原声的例句） | `archive-content-sources/conjugation-rebuild/voice_items.json` | 34 | `zword`（词）/ `zgram`（句） | 待生成 |
| 2026-10-03 | 0.27.0 VOL.G 縮約（语速档位每一档整句、没原声的例句） | 同上，`book: "G"` | 29 | `zgram` | 待生成 |
| 2026-10-03 | 0.27.0 VOL.A 活用形（活用盘的原形和变形、分拣的词、没原声的例句） | 同上，`book: "A"` | 62 | `zword` / `zgram` | 待生成 |
| 2026-10-03 | 0.27.0 VOL.F 形容词（拼合台的词、翻牌的词、没原声的例句） | 同上，`book: "F"` | 46 | `zword` / `zgram` | 待生成 |
| 2026-10-03 | 0.27.0 VOL.C 补助动词（换词的变体句、没原声的例句） | 同上，`book: "C"` | 93 | `zgram` | 待生成 |
| 2026-10-03 | 0.27.0 VOL.D 助动词（積み木拼出的词、没原声的例句） | 同上，`book: "D"` | 22 | `zword` / `zgram` | 待生成 |

**规矩**：以后任何内容脚本（`build_katsuyou.py`、`build_zougo.py`、知识卡……）只要写出 `voice_items.json`，或者有新的 App 会念的固定文字，就在上表加一行。生成完把状态改成「已入包 + 日期 + 包版本」。

## 生成一批的步骤

细节看 `VOICE_PACK_HANDOFF.md` 和 `../archive-content-sources/emilia-voice/HANDOFF.md`，这里只列顺序：

1. 把表里「待生成」的各个 `voice_items.json` 追加进 `emilia-voice/batch_items.json`。**不要跑 `batch_items.py`**，它会从 Supabase 重建整份清单。key 已存在的跳过。
2. 要教读音的词，`say` 换成假名（SBV2 看汉字会念错，Whisper 回听也查不出来）。
3. 先报价，再跑：`python -m modal run batch.py::retake --kinds zword,zgram --word-takes 3 --sent-takes 2 --containers 2`（新类别名要先加进 `batch.py` 的 `WORDLIKE`）。
4. `python pack_tools.py build`，把 zip 复制到用户的「下载」，提醒用户在 设置 → 语音包 → 重新导入。
5. 回到这里更新状态。

## 已入包

| 日期 | 内容 | 条数 | 包 |
|---|---|---|---|
| 2026-10-02 | 0.25.0 第四巻 造語（词 + 无原声例句） | 78 | 第六版之后的补包 |
