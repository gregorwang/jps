# 活用道場 交接（2026-09-26）

用动漫原声台词练基础日语：动词活用、音便、て形补助、助动词、条件接续、形容词、口语缩约、敬语。
起因是用户的学习笔记（`~/Downloads/gyo_dan.md`，讲行×段、て形拆解）：死记硬背的东西，用番剧原句记得更牢。

## 已完成

| 阶段 | 内容 | 位置 |
|---|---|---|
| P1 数据 | 只用**带原声**的句子（Re:ゼロ S2，`learning_sentences.audio_url`，共 4290 句）→ fugashi + UniDic 形态素分析 → 72 个语法点规则抽取 → DeepSeek 校对（有效性、重译中文、拆解公式、用法） | 脚本和中间数据在 `archive-content-sources/conjugation-drill-p1/`（已 gitignore，含台词原文，不能提交） |
| P2 后端 | 表 `conjugation_drill_items`，线上 **906 条已发布**；Worker `GET /api/conjugation-drill/items` 已部署 | `supabase/migrations/20260926120000_*.sql`、`src/worker.ts` |
| P2 App | 言語学 第三巻 活用：8 本教科書（按大类）、筛选到语法点、每组 15 题（先复习后新句）、4 种题型（行＋活用形 / 原形倒推 / 语法点辨认 / 句意）、答后播原声并显示拆解；进度是本地 Leitner（1/2/4/8/16 天） | `ui/drill/`、`ui/screens/session/ConjugationSession.kt`、`data/ConjugationDrill.kt`；commit `8b324e1`、`a5b11e4` |

| P3 数据 | `CAP` 20→40、`PER_EP` 2→3；修了 7 条误判规则（见下）；放宽 のに／ながら／てから／らしい／ければ／なくては；重跑时**已发布的句子优先保留、被拒过的不再重选**，所以 Leitner 进度不丢 | `extract.py`（旧版和 P2 数据备份在 `p2_backup/`） |
| P3 App | 答题反馈里的「深入讲解 · <主题>」：切换显示 `linguistic_foundation_topics` 的讲解，映射在 `ConjugationDrillRules.topicIdFor`；Review 页「活用 復習 · 到期 N 句」，点了直接跳到第三巻开一组 | commit `4fb1738` |

**线上 1734 句（2026-09-27 导入，待审 0）**：P2 的 905 句 + DeepSeek/Gemini 审的 199 句 + Antigravity 两轮审的 630 句。App 不用发版，运行时从 Worker 拉。

Antigravity 审核记录（都在存档目录）：
1. 第一轮造假（脚本照抄字幕中文、形态素原样当公式、402 句 note 同一句占位），全部撤下，隔离在 `antigravity_rejected/`。
2. 第二轮（723 句）：batch_000–015 合格，**从 batch_016 起变敷衍**——公式换成半角简写 `比べた(下一段动词连用形) + ら(…)`、中文越写越长越华丽（zh/ja 字数比中位数从 0.9 涨到 1.9，旧数据是 0.75）。只收了前 16 批里 zh ≤ 1.8 倍的 269 句，其余退回；原始产出在 `antigravity_round2/`，过滤前的备份是 `verified.before_round2_filter.json`。
3. 第三轮（392 句）：用加严后的 `ANTIGRAVITY_PROMPT.md`，质量稳定（zh/ja 比 0.90，前后无漂移），keep 363。只有最后一批的 12 句敬语 sense 为空、公式不规范，已手工改好，其中 2 句改判不收（お世話になる 是寒暄套语；司教様がお連れした 是尊者主语误用谦让语）。

**交给外部代理审核的经验**：
- 代理会在后半段偷懒。抽查要分前、中、后三段比较：zh/ja 字数比、公式格式、sense/note 是否为空；汇报里的样本要它从最后几批里取。
- `merge.py` 的 `lazy()` 现在会拒收以下几种：zh 照抄字幕、zh 超过日文 1.6 倍、公式里有形态素原样、公式用半角括号或半角 ` + `、同批 note 重复。
- zh 同时是句意题的正确选项，写得太长太华丽等于泄题，所以要求直译。

## P3 做了什么

- **误判修正**：`d_sou_youtai` 要求词干和そう紧贴且是连用形/语干；`e_to` 排除 だと、といい、ようと、引用动词，并改名「终止形＋と（一…就・条件）」；`h_sonkei` 排除 なさい 命令形；`c_tewa_dame` 排除 ない＋て（归 `g_nakereba`，后者新增 なくては＋ならない/いけない/だめ）；`a_trap_ru` 排除接头辞「お」后的；`e_noni` 的 の＋に 只认 助動詞/形容詞 后、且后面不是动词/邪魔/必要；`d_kanou_doushi` 要求 lemma 本身是五段（用 fugashi 查）、排除命令形和句末的连用形。
- **单一动词的点**（する、来る、行く、ございます、なさい）按活用形轮换，不再被 lemma 上限卡住。
- **语料确实少**：ので 1、そうだ(传闻) 2、てある 3、ございます 3、てはいけない 4——带原声的 Re:ゼロ S2 就这么多，接受现状。要更多只能等别的作品有原声。
- **校对模型**：DeepSeek 余额用完（402 Insufficient Balance），`verify.mjs` 支持 `MODEL=gemini-3.5-flash`（走 AI Gateway 的 `compat` 端点）。Gemini 偏宽松，几乎全 keep，拆解偶有小错。
- **基础题库映射**：大类默认 A/B→`morph_verb_conjugation`、C→`syn_te_clause_linking`、D/G→`morph_auxiliary_chain`、E→`prag_connectives_coherence`、F→`morph_adjective_inflection`、H→`prag_politeness_honorifics`；按点覆盖：ている→`sem_teiru_readings`、授受→`prag_viewpoint_empathy`、可能/义务/许可/推测→`sem_modality`、そう/よう/らしい→`sem_evidentiality`、れる→`syn_passive`、せる→`syn_causative`、ず→`morph_negation_forms`、条件→`syn_conditionals`、って→`syn_complement_quotation`、ちゃう/とく→`prag_register_style`。

## 怎么跑（都在 `archive-content-sources/conjugation-drill-p1/`）

- **准备素材**：`audio_sentences.json` 已存档在该目录。要重新导出，不用 token：learning_sentences 和 subtitle_lines 用 `.dev.vars` 的 `SUPABASE_PUBLISHABLE_KEY` 走 PostgREST 就能读（分页 1000）。等价 SQL：
  ```sql
  select s.id, s.episode, s.source_line_no, s.ja_text, s.reading, s.meaning_zh, s.difficulty, s.audio_url, l.start_time, l.end_time
  from learning_sentences s
  left join subtitle_lines l
    on l.work_slug = s.work_slug and l.episode = s.episode and l.line_no = s.source_line_no
  where s.audio_url <> ''
  ```
- **执行 SQL 的方式**：调 Supabase Management API，`POST https://api.supabase.com/v1/projects/qoatvdvbuleamyzsaldp/database/query`，请求体是 `{"query": ...}`，带上 `Authorization: Bearer <sbp_ token>`。token 由用户临时提供，24 小时后过期。
- **执行顺序**：
  1. `python extract.py`：生成 `selected.json`。
  2. `node verify.mjs`：做 AI 校对。
     - 走项目自己的 AI Gateway，默认 `deepseek-v4-pro`（余额已用完），可用 `MODEL=gemini-3.5-flash node verify.mjs`；token 从 `jps/.dev.vars` 的 `CF_AIG_TOKEN` 读取。
     - 运行时会跳过已经校对过的条目，所以中断后直接重跑就能接着来。
     - `max_tokens` 必须设大（24000），否则推理过程会把 token 吃光，返回空内容。
  3. `python build_preview.py <输出目录>`：生成 `drill_items.json` 和 `preview.md`。
  4. `SBP=<token> node import.mjs drill_items.json`：按 id upsert，这次没出现的条目会被自动 archived。
- 注意：脚本里的 `HERE` 指脚本所在目录，所以在存档目录里直接运行就行。
