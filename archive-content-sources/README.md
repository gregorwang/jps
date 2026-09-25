# archive-content-sources — 内容生产资料归档（不是应用代码）

> **给 AI Agent 的一句话**：这个目录里是**生产学习内容时用过的原始素材、导入流水线和生成/复核记录**。
> 这些内容**已经写进 Supabase 数据库**，Android App、Web 前端和 Worker **运行时都不读这里的任何文件**。
> 做功能开发或 UI 改版时**可以完全忽略此目录**；只有在需要“重新导入字幕 / 追溯某条题目来源 / 新增一批内容”时才进来。

归档时间：2026-09-25（原本散落在仓库根目录的 `srt/`、`regen/`、`foundation/` 和 `public/` 下的字幕文件）。

## 目录一览

| 子目录 | 内容 | 来源（原路径） | 大小 | 是否已入库 |
| --- | --- | --- | ---: | --- |
| `rezero-subtitles-ass-backup-S00-S03/` | Re:Zero 全季 BDRip 中文字幕（`.SC.ass` 简体 / `.TC.ass` 繁体），按 `S00`–`S03` 分季 | `public/字幕备份/`（`srt/jpv/` 下原有一份完全相同的副本，已去重删除） | 115M | 是，字幕句子与翻译已导入 |
| `rezero-subtitles-netflix-ja-srt/` | Re:Zero 新编集版 Netflix 日文 CC 字幕（`.srt`），`S02/` = E01–E25，`S03/` = E51–E62 | `public/Re_Zero kara Hajimeru Isekai Seikatsu 2nd Season (1)/`、`public/…3rd Season/` | 1.9M | 是 |
| `rezero-subtitle-import-pipeline/` | 字幕 → 数据库的导入流水线：`scripts/`（Node/Python 导入、合并、审计脚本）、`scripts/generate-learning-materials/`（词汇/语法/例句/练习生成器）、`scripts/video_pipeline/`（跟读音频切片）、`vector-ingest/`（Cloudflare Vectorize 字幕向量入库 Worker）、`input/subtitles/`（K-ON!、双语 ass 输入） | `srt/`（此前被 gitignore，现在纳入版本控制；`node_modules`、`__pycache__` 已清除） | 2.7M | 是 |
| `rezero-exercise-regen-2026-07-27/` | 2026-07-27 对 Re:Zero 练习题（2,591 条）和语言学卡片（960 条）做的人工重新生成：分批的生成 JSON、复核 overlay、交叉复核报告、QA 报告，以及入库用的 `.mjs` 脚本 | `regen/` | 5.1M | 是，详见该目录 `README.md` |
| `foundation-question-bank-v1-2026-07-27/` | 基础日语语言学题库 V1（60 主题、3 题包、240 题）：主题/题目的生成稿、复核 overlay、最终生效版 `*_effective.json`、出题契约 | `foundation/` | 1.5M | 是，详见该目录 `README.md` |

## 和仓库其他部分的关系

- **仍然有用、不在这里的脚本**：`scripts/validate-foundation-content.mjs`、`scripts/merge-foundation-reviewed.mjs`、`scripts/prepare-foundation-transport.mjs`、`scripts/build-foundation-publish-sql.mjs` 是 Foundation 题库的通用校验/发布工具，通过命令行参数接收文件路径，例如：
  ```powershell
  node scripts/validate-foundation-content.mjs pack archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave1_generation.json archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave1_review_overlay.json
  ```
- **路径已经改过**：搬家时同步更新了
  - `rezero-exercise-regen-2026-07-27/*.mjs` 中硬编码的 `regen/…` → `archive-content-sources/rezero-exercise-regen-2026-07-27/…`（需在仓库根目录运行）；
  - `rezero-subtitle-import-pipeline/scripts/` 下 3 个 Python 脚本对 ASS 字幕目录的引用 → `../rezero-subtitles-ass-backup-S00-S03/S02|S03`；
  - 两个 README 和各份复核报告里的文件路径。
- **已知失效的默认值**：`video_pipeline/batch_kamigami_shadowing.py`、`retry_best_effort.py` 默认从 `jpv/` 找 `.mkv` 视频源，视频从未入库，使用时需用 `--jpv-dir` 指定本地视频目录。
- **唯一的代码依赖**：`src/worker.foundation.test.ts` 把 `foundation-question-bank-v1-2026-07-27/questions_wave{1,2,3}_effective.json` 当测试夹具 import。移动或删除这三个文件前先改这个测试。
- `vitest.config.ts` 已排除整个 `archive-content-sources/**`，这里的文件不会被当成测试跑。
- `rezero-subtitle-import-pipeline/vector-ingest/.dev.vars` 含密钥，已被 gitignore，不要提交。

## 新增内容时怎么做

1. 原始字幕放进对应的 `rezero-subtitles-*` 目录（或为新作品新建 `<作品>-subtitles-<格式>/`）。
2. 用 `rezero-subtitle-import-pipeline/scripts/` 里的导入脚本写入 Supabase。
3. 大批量生成/复核的中间产物，按 `<主题>-<YYYY-MM-DD>/` 新建子目录放在这里，并附 `README.md` 写明数据库实测状态，不要再散落到仓库根目录。
