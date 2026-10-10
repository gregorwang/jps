# docs · 文档目录

文档里写的路径都相对 `android-app/`。入口仍是根目录的 `CLAUDE.md`（第 1 节按任务查该读哪份）；`CHANGELOG.md`、`README.md` 留在根目录。新写的文档按下面分类放，不要再堆在根目录。

| 文件夹 | 放什么 | 文档 |
|---|---|---|
| `setup/` | 环境、构建、发布 | `ANDROID_ENVIRONMENT`、`APP_UPDATE_GUIDE` |
| `product/` | 路线图、审计 | `ANDROID_PRODUCT_ROADMAP`、`AUDIT_2026-09-27` |
| `design/` | 设计规则、动效、**新文档做教科書的做法** | `TEXTBOOK_UI_PLAYBOOK`（必读）、`MOTION_SPEC`、`V3_IMPLEMENTATION_PLAN`、`ASSET_REMIX`、`STUDY_DESK_PLAN`、`v3-requests/` |
| `textbooks/` | 自習书架上各本教科書的交接 | `KYOKA_HANDOFF`（第五巻起）、`WARAI_VISUAL_HANDOFF`（笑い 画布落地，做新书的范例）、`CONJUGATION_REBUILD_HANDOFF`（VOL.A–H）、`CONJUGATION_DRILL_HANDOFF`、`ZOUGO_HANDOFF`、`DOKKAI_HANDOFF` |
| `words/` | 单词、辞書 | `VOCAB_CARD_HANDOFF`（単語卡）、`VOCAB_CARDS_HANDOFF`（单词卡数据）、`COLLOCATION_HANDOFF`、`FREQ_WORDS_HANDOFF`、`HOMOPHONE_HANDOFF` |
| `voice/` | 语音包、电台、配音待办 | `VOICE_PACK_HANDOFF`、`VOICE_BACKLOG`、`RADIO_HANDOFF`、`SHADOWING_SCORING_FAILED`（不做） |
| `content/` | 内容数据清洗、读音修正 | `CONTENT_CLEAN_HANDOFF`、`ROMAJI_FIX_HANDOFF` |

脚本没有挪：`design/v3build.ps1`、`design/peek_overview.py` 还在原处。
