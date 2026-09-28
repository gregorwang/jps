# 内容数据清洗 · 交接文档（2026-09-28）

> 给「Antigravity 交回语法点和台词之后」开的新会话看。先读 `CLAUDE.md`，再读本文。
> 单词卡那一轮见 `VOCAB_CARDS_HANDOFF.md`，流程相同。

## 1. 审计结果（2026-09-28，Supabase 只读导出）

App 课程每集只取前 30 个语法点、30 句台词（worker `order=sort_order&limit=30`），下面的「可见」指这部分。

| 数据 | 问题 | 处理 |
|---|---|---|
| `learning_grammar_points` 2966 条（可见 2397） | 1198 条解释 ≤10 字；208 条是「此处使用「〜」，表示…。 对照：…」模板；`pragmatics_note` 是流水线备注（「V9：语法例句来自原始 SRT 短句」240 条），课程学习卡会显示出来；句型标错（`なって` 标成引用「って」）；500 条句型不是形式（`とっくの昔に`、`请求表达`）；同一集同一句型重复约 450 条 | **Antigravity 本轮** |
| `learning_sentences` 8560 句（可见 2340） | 译文是剧情概述（「爱蜜莉雅愤怒地说…」）或对不上原文；190 句繁体；287 句用双空格断句；拼句题直接切这些译文 | **Antigravity 本轮**（只做可见的 2340 句） |
| 同上 | `reading` 7710 句为空或等于原文、`romaji` 8148 句为空、291 句罗马音没空格且错 | 不用数据：App 用 furigana + `ui/reading/Kana.kt` 本地算 |
| 同上 | `tone_tags` 里混进 441 行流水线字段（`v`、`speaker`、`clip_policy`…）；115 句原文带括号注音 `逢瀬(おうせ)` | 脚本修（push 时一起做） |
| `learning_exercises` 14427 题 | 2020 题的 `hint` 是流水线备注或通用话（「V10 recommended_shadowing=true…」「感情・相手・状況をまとめる。」）；664 题答案只有一个字母；248 道 sentence_understanding 答案是概述；`kana_to_kanji` / `meaning_to_japanese` 83 题按旧的错读音出题（「假名：だよ → 大丈夫」） | 本轮数据回来后**用脚本派生**：grammar_meaning 答案换成新 `meaning`，sentence 类答案换成新 `zh`，清掉流水线 hint，按 `vocab_cards` 重出或删掉假名题 |
| `learning_card_enrichments` 1585 张 | `payload` 大量模板（同一句用在 300+ 张卡上，882 张标题带内部 id）；App 已靠 `CardEnrichment.FillerMarkers` 过滤。`linguistic_payload` 质量尚可 | 本轮不做。语法点重写后，考虑 App 不再显示 grammar / vocab 的 `payload`，只留 `linguistic_payload` |
| `linguistic_phenomena` 426 条 | 42 条 deep / examples 为空 | 不显示，不做 |
| `conjugation_drill_items`、`vocab_cards` | 已经人工整理过 | 不做 |

## 2. 文件在哪

目录：`C:\Users\汪家俊\jps\archive-content-sources\content-clean-v1\`（已 gitignore，含字幕原文，**绝不能提交**）

| 文件 | 作用 |
|---|---|
| `ANTIGRAVITY_PROMPT.md` | 给 Antigravity 的任务说明 |
| `export.py` | 只读导出可见的语法点 / 台词 / 字幕上下文，生成 `batches/g_00..47.md`（每批 50 条）、`s_00..23.md`（每批 100 句）。**不要重跑**（`--offline` 只重写 md） |
| `batches/g_NN.json`、`s_NN.json` | Antigravity 的产出 |
| `check.py` | 校验 + 合并成 `grammar_cards.json`、`sentence_cards.json` |

## 3. 数据回来后要做的事

1. `python check.py`：目标语法点 `ok=2397`、台词 `ok=2340`，`missing=0 errors=0`。不到就把 errors 给用户让 Antigravity 返工，**不要自己改 JSON 凑数**。
2. 抽查：语法点随机 30 条（句型判断对不对、`keep=false` 的理由站不站得住），台词随机 30 句（对照原文）。结果告诉用户。
3. 写 `push_cloud.py`（仿 `vocab-cards-v1/push_cloud.py`，Management API + 用户给的 `sbp_` token）：
   - 先备份：`create table learning_grammar_points_backup_2026MMDD as …`、sentences、exercises 同理，**建完立刻 `enable row level security`**。
   - 语法点：`pattern / function_zh(=meaning) / explanation_zh(=explain) / real_world_note(=note) / difficulty(=level)`，`pragmatics_note` 清空；`keep=false` 的行怎么下线要看 worker（加 `is_active` 列或把 `sort_order` 挪到 30 之后，改 worker 属于要问用户的事）。例句译文 `example_zh` 目前没有列，先加列或放进 `explanation_zh` 之外的新字段。
   - 台词：`meaning_zh(=zh)`；`keep=false` 同上；顺手清 `tone_tags` 和括号注音。
   - 练习题：按第 1 节表里的规则派生。
4. 迁移记录写进 `supabase/migrations/`。App 端如需显示 `example_zh`，改 `data/` 解析（这属于数据层改动，按需做）。
5. 编译、发版，CHANGELOG 写清楚改了哪些数据。

## 4. 进度

- 2026-09-28 上午：包已交给 Antigravity 开工（先台词后语法点）。
- 2026-09-28 下午：第一轮交回，`check.py` 全过（语法点 2397，keep=false 854 = 35%；台词 2340，keep=false 11）。
  - 抽查台词 30 句：意思全对，3–4 句有润色或添字（「小さな王国」→「虚妄的王国」），没有概述式译文。
  - 抽查语法点 20 条：改标准确，1 处错译（ハーフエルフ→「半年精灵」，已在 `g_25.json` 改成「半精灵」）；`explain` 偏华丽、爱复述剧情，不影响教学。
  - **问题出在我的提示词**：句型标错就删、不改标，外加约 250 条以「太初级」删。K-ON! 每集 15 条只剩 1–5 条。用户同意**返工**：`export_rework.py` 生成 `batches/rw_00..15.md`（795 条，同集重复的不返工），提示词 `ANTIGRAVITY_REWORK_PROMPT.md`；`check.py` 让 `rw_NN.json` 覆盖同 id 的 g 行，返工里 keep=false 只能以「重复：」「碎片：」开头。
- 下线方式（用户定）：三张表加 `is_active`。worker 仍取前 30 条（按 sort_order），**在这 30 条里**滤掉 is_active=false，避免第 31 条以后没清洗的行补上来；练习题直接 `is_active=eq.true`。已提交（cd63600），**worker 还没部署**：必须先跑 `push_cloud.py` 建好列再部署，否则练习题查询会报错。
- `push_cloud.py`：幂等，每次从 `grammar_cards.json`、`sentence_cards.json`、`exercises_orig.json`（首次写回前的练习题快照，不要删）重新计算，返工回来后直接重跑。`--dry` 看统计。练习题派生：匹配到清洗行的语法题 / 台词题换答案（语法题 hint 换成 explain、题干换新句型）；删掉的行对应的题下线；单字母答案（A/B，选项没存）下线；非学习词的词汇题下线；vocab_meaning 答案同步云端释义；假名题按云端单词重出；流水线 / 高频套话 hint 清空（词汇题的标签式 hint 保留，那是释义或读音）。
- App：`exampleZh` 改为读 worker 的 `exampleZh`（以前错把 realWorldNote 当译文）；学习卡 note 的标签从「语气」改为「用法」；辞書语法条目不再显示旧的 AI 增强 `payload`（和新句型对不上）。
- 没做：练习题里对应**窗口外**（第 31 条以后）语法点 / 台词的约 4500 题仍是旧答案，没清洗（不在本轮范围）。

- 2026-09-28 晚：返工交回，`交回 795/795，改标保留 700，错误 0`；语法点 keep=false 降到 154（6%，碎片 73 + 重复 22 + 第一轮同集重复）。K-ON! 每集恢复到 11–15 条。抽查 30 条句型全对，偶有挑了次要句型（「ならともかく…から」挑了「から」）。`push_cloud.py --dry`：语法题改写 1519、下线 83，练习题共下线 686。

### 下一步

1. 用户给 `sbp_` token → `SUPABASE_ACCESS_TOKEN=... python push_cloud.py` → 仓库根目录 `npx vite build && npx wrangler deploy` → 发版。
2. ~~返工~~：已交回并通过，第 1 步直接用现在的 json。
