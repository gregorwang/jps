# 单词卡数据重写 · 交接文档

> 给「Antigravity 写完单词卡之后」开的新会话看。先读 `CLAUDE.md`，再读本文，其他不用读。

## 1. 背景（一句话版）

`learning_vocab_items`（3770 行）的数据是早期脚本生成的：读音错（`大丈夫`=`だよ`，`戻れ`=原形`もどる`），释义乱，备注全是套话（「EP17筛选：学习价值优先于机械词频…」）。
0.9.0 的「単語練習」（辞書 → 選ぶ / この語を練習）要教读音、逐假名罗马音、拼写，所以数据必须改对。
重写交给了 Antigravity（用户手动操作），我们只做工程：校验、安装、发布。

## 2. 文件在哪

目录：`C:\Users\汪家俊\jps\archive-content-sources\vocab-cards-v1\`（已 gitignore，含字幕原文，**绝不能提交**）

| 文件 | 作用 |
|---|---|
| `ANTIGRAVITY_PROMPT.md` | 给 Antigravity 的任务说明（字段、质量要求） |
| `export.py` | 从 Supabase 只读导出 `vocab.json`、`subtitles.json`，生成 `batches/batch_00..37.md`（每批 100 词）和 `ids.json`。**不要重跑**，否则批次会被覆盖（加 `--offline` 才只重写 md） |
| `batches/batch_NN.json` | Antigravity 的产出 |
| `check.py` | 校验 + 合并成 `cards.json`；`--install` 写入 App 资源 |
| `cards.json` | 合并结果（check.py 生成） |

App 这边已经做好的（0.9.0 已发布）：
- `ui/words/VocabCards.kt`：读 `assets/vocab_cards.json`，格式 `{"version":1,"cards":{id:[keep, reading, lemma, lemma_reading, pos, meaning, note]}}`。文件不存在时什么都不做。
- 用到它的地方：`WordRules.card(..., fix)`（单词练习）、`LibraryScreen.VocabEntry`（辞書词条的读音/释义/备注）、`VocabPage`（`keep=false` 的词不在辞書列出）。

## 3. 新会话要做的事（按顺序）

1. **看校验结果**：
   ```
   cd C:\Users\汪家俊\jps\archive-content-sources\vocab-cards-v1
   python check.py
   ```
   目标 `ok=3770 missing=0 errors=0`。不到的话，把 errors 列表整理给用户，让 Antigravity 返工。**不要自己批量改 JSON 凑数**。
2. **抽查质量**（校验只能挡格式和套话，挡不住错读音）：
   - 随机抽 40 个 `keep=true` 的词，自己核对 `reading`（连浊、促音、长音）、`lemma` 是否真是辞书形、`meaning` 是否符合台词。
   - 看 `keep=false` 的比例：超过 20% 就列出来给用户看，可能删多了。
   - 搜 `note` 里的空话（「语气」「表示」开头的一大串、和释义重复的）。
   - 把抽查结果（有几处错、例子）告诉用户，由用户决定是否返工。
3. **安装**：`python check.py --install` → 生成 `android-app/app/src/main/assets/vocab_cards.json`。看一下文件大小（预计 300–500KB），可以接受。
4. **顺手把同一份数据用到课程流程**（0.9.0 没做）：
   - `ui/screens/session/StudyCardQuestion.kt` 的学习卡（課程里「先学这个词」）仍然显示原始 `reading`，比如 `戻れ / もどる`。改成：`sourceKind == "vocab"` 时用 `VocabCards.get(context, node.sourceId)` 覆盖读音、释义，`note` 放进 notes。
   - 不要改 `data/`（CLAUDE.md 规定）。
5. **编译**：`design/v3build.ps1 -Mode compile`，过了就 commit。
6. **发布**：`-Mode full` 通过 → `app/build.gradle.kts` 的 versionCode +1 → 跑 `scripts/publish-app-update.ps1`（见 CLAUDE.md 第 6 节）。更新说明写：「辞書单词数据重写：读音、释义改正，去掉套话备注和不成词的碎片」。
7. **commit + push main**，在 CLAUDE.md 第 8 节补两三条经验。

## 4. 可选：写回 Supabase

App 用本地 asset 已经够了。如果用户希望网页端和服务端题目（課程里的词汇题）也用上新数据，需要把 `cards.json` 写回 `learning_vocab_items` 的 `reading` / `romaji` / `meaning_zh` / `real_world_note`。
这需要 service role key（`wrangler.toml` 里只有只读的 publishable key），**先问用户要，别自己找**。改后端数据属于「需要问用户」的事。

## 5. 相关的坑

- Supabase 用 Python 请求要带浏览器 User-Agent，否则 Cloudflare 返回 403。
- `subtitle_lines` 的中文 `zh_text` 经常和日文错位，不能当译文用。
- `WordRules.fits` 和 `check.py` 的 `fits` 是同一条规则（汉字每个字读 1–4 个假名），改一边要同步另一边。
