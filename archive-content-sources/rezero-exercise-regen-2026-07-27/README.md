# Re:Zero 学习数据人工重生记录（2026-07-27）

## 数据库实测状态

- `maintenance.regen_20260727_manifest`：3,554 行
  - `applied`：3,551
  - `rolled_back`：3
  - 其他未完成状态：0
- 已应用到 `public.learning_exercises`：2,591 条，数据库与 manifest 候选逐字段精确匹配 2,591 / 2,591。
- 已应用到 `public.learning_card_enrichments`：960 条，数据库与 manifest 候选逐字段精确匹配 960 / 960。
- 960 条语言学卡全部固定为：
  - `linguistic_prompt_version = agent-jp-regenerated-v1`
  - `linguistic_quality_score = 95`
  - `linguistic_status = ready`
- applied 记录中的占位答案残留：0；applied 目标缺失：0；重复 applied 身份：0。

本次继续处理以前的实测基线为 applied 3,029 条；本次新增并核验 522 条：

- 练习新增 362 条：2,229 → 2,591。
- 语言学卡新增 160 条：800 → 960。

## 本次继续处理的人工批次

| 分组 | 数量 | 独立复核结果 | 事务粒度 |
| --- | ---: | --- | --- |
| Meta exercises Batch9 | 80 | 保留 76、按 ID 修订 4、拒绝 0 | 4 × 20 |
| Linguistic Batch11 | 80 | 保留 74、按 ID 修订 6、拒绝 0 | 8 × 10 |
| Letter exercises Batch35–37 | 120 | 保留 116、按 ID 修订 4、拒绝 0 | 3 × 40 |
| Meta exercises Batch10 | 42 | 保留 40、按 ID 修订 2、拒绝 0 | 2 × 21 |
| Letter exercises Batch38–40 | 120 | 保留 112、按 ID 修订 8、拒绝 0 | 3 × 40 |
| Linguistic Batch12 | 80 | 保留 70、按 ID 修订 10、拒绝 0 | 8 × 10 |

生成者和独立复核者始终为不同 Agent。复核逐条检查了日语形态、句法、语义、语用、说话人、指代、否定、时态、语态和连续字幕上下文。典型修正包括：

- `re-zero-ex-ep043-177` 明确为碧翠丝在掩饰，消除说话人指代歧义。
- `grammar-007` 按固定表达「下手をすると」中的助词省略或词汇化解释，不再误拆为「下手だ＋する＋と」。
- Batch38–40 修正了菲利斯、威尔海姆、康伍德等说话人归属，以及「声をかける」的对象、`その辺` 的回指和“不死不等于无人受伤”等边界。
- Linguistic Batch12 修正了说话人、`てもらう` 的受益方、句末省略谓语、否定范围、缩约边界及未被字幕支持的目的推断。

JavaScript 仅用于选取、结构校验、按 ID 合并 overlay、分块和数据库运输，没有用模板拼接教学内容。每份最终候选都经过显式逐 ID ledger、唯一性检查和 validator；本次各有效集合均为 0 errors / 0 warnings。

## 原子写入守卫

每个事务都在同一 `DO` 中完成并核验：

1. 候选数量、唯一 ID 数量和固定分块大小。
2. 目标表与备份表存在全部 ID。
3. 当前目标整行仍与备份逐字段完全一致。
4. 当前答案或语言学版本仍属于本批目标占位类型。
5. batch ID 和 `(table_name, row_id)` 均无任何 manifest 历史重叠。
6. 插入 `approved` manifest、更新目标、逐字段核验、标记 `applied`。
7. 对全局所有 applied manifest 与数据库重新做逐字段精确匹配。

任一守卫异常都会使该事务整体回滚。一次长 SQL 的终端回传被截断，数据库在解码候选载荷前即拒绝执行；复核确认该次产生 0 manifest、0 目标写入、0 未完成状态，随后改用带长度与省略符检查的分片重组运输完成正式事务。

## 已应用数据构成

| 类别 | 已应用 |
| --- | ---: |
| EP15 练习 | 229 |
| 字母占位练习批次 | 1,600 |
| 元信息答案练习 Batch1–10 | 762 |
| 语言学卡 Batch1–12 | 960 |
| 合计 | 3,551 |

## 当前剩余

- 字母占位答案：664
  - EP22：25
  - EP23：212
  - EP24：216
  - EP25：211
- `用于…` 元信息答案：0。
- 当前仍为旧 `linguistic-card-v1` 且有 payload 的卡：625。
- 可复现的宽口径“至少 12 字符、跨至少 3 卡重复叶”审计：445 张旧卡、121 个重复长叶。该宽口径不等同于历史固定模板 cohort，不与旧算术余量混用。

## 备份与安全状态

- `maintenance.regen_20260727_learning_exercises_backup`：3,255 行。
- `maintenance.regen_20260727_card_enrichments_backup`：1,585 行。
- `maintenance.regen_20260727_manifest`：3,551 条 applied、3 条既有 rolled_back。
- 用户提供的一次性凭据未写入 `regen`、脚本或本 README。
- Supabase Advisor 仍提示三张 `maintenance` 表未启用 RLS。本次未擅自改变访问策略；在没有配套 policy 时直接启用 RLS 可能阻断维护访问。

## 可复现校验

全库只读审计见 `archive-content-sources/rezero-exercise-regen-2026-07-27/audit_remaining.sql`。

候选结构校验：

```powershell
node regen\validate_candidates.mjs <candidate.json> [...]
```

复核 overlay 的按 ID 合并、覆盖数量、ledger 覆盖和分块摘要：

```powershell
node regen\prepare_reviewed_transport.mjs <group>
```

最终数据库审计：applied 3,551 / 3,551 精确匹配；占位残留 0；目标缺失 0；未完成 manifest 0。
