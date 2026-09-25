# 基础日语语言学题库 V1（2026-07-27）

这套题库独立于动漫字幕语料，用原创例句、最小对立、构造对话和元语言材料讲授日语基础语言学。动漫语料训练仍保留为另一条训练轨道，两者不共用题目身份或伪造作品、集数、角色来源。

## 数据库实测状态

- 课程版本：`foundation-v1`
- 已发布主题：60
- 已发布题包：3
- 已发布题目：240
- `maintenance.linguistic_foundation_v1_manifest`：303 条，全部 `applied`
- Manifest 与数据库逐字段精确匹配：
  - 主题：60 / 60
  - 题包：3 / 3
  - 题目：240 / 240
- 生成者与复核者相同的记录：0
- 最低质量分：主题 96、题包 96、题目 96
- 匿名角色可见：60 个主题、3 个题包、240 题，均为已发布记录

每个题包含 20 个主题、80 题；每个主题固定 F1 识别、F2 解析、F3 对比诊断、F4 迁移应用四阶。每包 F1–F4 各 20 题，正确答案 A/B/C/D 各 20 题。

| 领域 | 主题数 |
| --- | ---: |
| 语音与文字 | 8 |
| 形态学 | 10 |
| 句法学 | 13 |
| 语义学 | 8 |
| 语用与话语 | 12 |
| 社会语言学 | 5 |
| 历史语言学与语法化 | 4 |

## 多 Agent 创作与独立复核

| 集合 | 数量 | 生成者 | 独立复核者 | 复核结果 | Candidate SHA-256 |
| --- | ---: | --- | --- | --- | --- |
| 主题目录 | 60 | `/root/foundation_topics_generator` | `/root/foundation_topics_reviewer` | 最低 96 | `a8c0cd35e48a083d01ac38c462eb1c1a5add239658cdcd6ada0d12ffe1b210d6` |
| Wave 1 | 80 | `/root/foundation_schema_v1` | `/root/audit_foundation_wave2_publish` | 保留 77、修订 3、拒绝 0 | `d7b1eb74a51d3d6eb1ec3bad4f64cd837e8e31f557a5f15252a12b787c7aed0b` |
| Wave 2 | 80 | `/root/foundation_topics_reviewer` | `/root/foundation_topics_generator` | 保留 74、修订 6、拒绝 0 | `8e550a04e22fe662f1c9f743ed6230b78fccef0153b5512137c6cd09f7afeb19` |
| Wave 3 | 80 | `/root/foundation_api_v1` | `/root/foundation_schema_v1` | 保留 67、修订 13、拒绝 0 | `415da6de202ace24c473af71095e70a996cba139af12c054208d9e78b2392488` |

复核逐题检查形态、句法、语义、语用、说话人、指代、否定、时态、语态和跨话轮语境。Review overlay 只能完整替换既有 ID，不能追加；未获独立复核的候选不能进入发布事务。

脚本只负责结构校验、按 ID 合并、哈希、事务 SQL 构建与数据库运输，没有用模板生成教学内容。四份有效文件的 SHA-256：

- `topics_v1_effective.json`：`a3ac016034ce68a5d613cdfee6200bff5e8b9ad823f858353e742e7e8fa617af`
- `questions_wave1_effective.json`：`f328e1657f8c396c317d164d4f85f80bf636ce55b0299836f471870d32e18d97`
- `questions_wave2_effective.json`：`598642cd811d66143333fb1720e09d81e5f390a7ab1ac0290d391ebe0967951a`
- `questions_wave3_effective.json`：`5f80698e14e7ec0649a6aee76b505264d7d2a3423ac7c17879ec3a63a15d1f03`

## 数据与客户端契约

数据库表：

- `public.linguistic_foundation_topics`
- `public.linguistic_foundation_question_packs`
- `public.linguistic_foundation_questions`
- `public.linguistic_phenomenon_topic_map`

只读 API：

- `GET /api/linguistics/foundation/topics`
- `GET /api/linguistics/foundation/packs`
- `GET /api/linguistics/foundation/questions`

API 使用已绑定资源和筛选条件的不透明游标，只返回 `published` 数据。Worker 会把数据库中的四类已复核刺激材料规范化为稳定 DTO。全量回归覆盖 240 / 240 题：句子 69、对话 30、对照 58、元语言 83；23 条对话/对照顶层语境、2 条释义标注和 2 条结构标注均保留。

Web 与 Android 都提供“基础语言学 / 动漫语料分析”双轨入口。基础题进度只保存 `track`、题包、主题、阶段、题型和题目身份，不伪造 `workSlug` 或 `episode`；复习入口会定位到原题。

## 发布守卫

每个发布事务同时验证候选数量和唯一 ID、生成/复核独立性、质量、主题版本和状态、领域配额、F1–F4 覆盖、答案平衡、选项与错因解释、禁用动漫来源键、目标及 Manifest 历史重叠、候选哈希和逐字段一致性。

事务先插入 `approved` Manifest 与目标记录，逐字段核验后才发布并标记 `applied`；最后再检查全局所有 applied Manifest。任何异常都会使整批回滚。正式写入前每波都执行同一 SQL 的回滚干跑，并确认数据库计数没有变化。

## 可复现校验

```powershell
node scripts/validate-foundation-content.mjs topics archive-content-sources/foundation-question-bank-v1-2026-07-27/topics_v1_generation.json archive-content-sources/foundation-question-bank-v1-2026-07-27/topics_v1_review_overlay.json
node scripts/validate-foundation-content.mjs pack archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave1_generation.json archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave1_review_overlay.json
node scripts/validate-foundation-content.mjs pack archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave2_generation.json archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave2_review_overlay.json
node scripts/validate-foundation-content.mjs pack archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave3_generation.json archive-content-sources/foundation-question-bank-v1-2026-07-27/questions_wave3_review_overlay.json
npm test
npm run build
```

数据库安全 Advisor 为 0 条安全告警。性能 Advisor 仅有历史未使用索引的提示；没有为消除提示而删除索引。
