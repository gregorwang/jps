# 活用道場 交接（2026-09-26）

用动漫原声台词练基础日语：动词活用、音便、て形补助、助动词、条件接续、形容词、口语缩约、敬语。
起因是用户的学习笔记（`~/Downloads/gyo_dan.md`，讲行×段、て形拆解）：死记硬背的东西，用番剧原句记得更牢。

## 已完成

| 阶段 | 内容 | 位置 |
|---|---|---|
| P1 数据 | 只用**带原声**的句子（Re:ゼロ S2，`learning_sentences.audio_url`，共 4290 句）→ fugashi + UniDic 形态素分析 → 72 个语法点规则抽取 → DeepSeek 校对（有效性、重译中文、拆解公式、用法） | 脚本和中间数据在 `archive-content-sources/conjugation-drill-p1/`（已 gitignore，含台词原文，不能提交） |
| P2 后端 | 表 `conjugation_drill_items`，线上 **906 条已发布**；Worker `GET /api/conjugation-drill/items` 已部署 | `supabase/migrations/20260926120000_*.sql`、`src/worker.ts` |
| P2 App | 言語学 第三巻 活用：8 本教科書（按大类）、筛选到语法点、每组 15 题（先复习后新句）、4 种题型（行＋活用形 / 原形倒推 / 语法点辨认 / 句意）、答后播原声并显示拆解；进度是本地 Leitner（1/2/4/8/16 天） | `ui/drill/`、`ui/screens/session/ConjugationSession.kt`、`data/ConjugationDrill.kt`；commit `8b324e1`、`a5b11e4` |

**还没推送到手机**。用户要测试时先"推送更新"（CLAUDE.md 第 6 节）。

## P3 待做（按性价比排序）

1. **多出题**：用户要"多找点"。抽取阶段每个语法点上限是 20 句（`extract.py` 里的 `CAP`），而候选有 6577 条：する 602、促音便 506、ている 484、んだ 396、可能动词 342……把 `CAP` 提到 40，再跑校对和导入，大概能到 1800 条。
2. **修误判规则**（`extract.py`），这些是校对时发现的：
   - `d_sou_youtai`：会误抓副词「そう」
   - `e_to`：会误抓引用的「だと」
   - `h_sonkei`：会误抓「ごめんなさい」
   - `c_tewa_dame`：混进了「なくてはならない」，这应该归到 `g_nakereba`
   - `a_trap_ru`：会误抓「お帰り」
   - `e_noni`：会误抓表目的的「のに」
   - `d_kanou_doushi`：偶尔误判
3. **补条数少的语法点**：ので、のに、ながら、てから、てある 各只有 4 句，らしい 2 句。原因可能是规则写窄了，也可能是带原声的语料确实少。先放宽规则试试，不够就接受现状。
4. **关联基础题库**：每个语法点链接到 `linguistic_foundation_topics` 对应的主题，在答题反馈里加"深入讲解"。对应关系：
   - 動詞活用：`morph_verb_conjugation`
   - て形：`syn_te_clause_linking`
   - ている：`sem_teiru_readings`
   - 助動成分：`morph_auxiliary_chain`
   - 条件：`syn_conditionals`
   - 被动：`syn_passive`
   - 使役：`syn_causative`
   - 敬语：`prag_politeness_honorifics`
   - 形容词：`morph_adjective_inflection`
5. （可选）在 Today 或 Review 页显示「活用 复习 N」的入口。
6. 有 40 条没校对到，用户说不用补了。

## 怎么跑（都在 `archive-content-sources/conjugation-drill-p1/`）

- **准备素材**：`audio_sentences.json` 当时放在 scratchpad，没有存档。重新导出用这条 SQL：
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
     - 走项目自己的 AI Gateway，调 `deepseek-v4-pro`，token 从 `jps/.dev.vars` 的 `CF_AIG_TOKEN` 读取。
     - 运行时会跳过已经校对过的条目，所以中断后直接重跑就能接着来。
     - `max_tokens` 必须设大（24000），否则推理过程会把 token 吃光，返回空内容。
  3. `python build_preview.py <输出目录>`：生成 `drill_items.json` 和 `preview.md`。
  4. `SBP=<token> node import.mjs drill_items.json`：按 id upsert，这次没出现的条目会被自动 archived。
- 注意：脚本里的 `HERE` 指脚本所在目录，所以在存档目录里直接运行就行。
