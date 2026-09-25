# 下一轮只读选集 QA

## 结论

本轮只做机器选集与 ID 清单，没有生成或复制任何教学文字，没有写数据库，也没有修改 A/B/C 已冻结候选。

生成的只读文件：

- `regen/selection_next_meta_batch10.json`：42 条
- `regen/selection_next_letter_batches38_40.json`：120 条，按 40 条分为 Batch38–40
- `regen/selection_next_linguistic_batch12.json`：80 张，40 vocab + 20 grammar + 20 sentence

选集把下列冻结候选视为即将应用并提前排除：

- Meta Batch9：80 个唯一 ID
- Linguistic Batch11：80 个唯一 ID
- Letter Batch35–37：120 个唯一 ID

generation overlay 只替换同 ID，不增加锁集数量。

## 共同数据库守卫

只读复算使用项目 `qoatvdvbuleamyzsaldp` 的实际状态，并对最终文件中的精确 ID 再次核验。

| 检查 | exercises 选集 | linguistic 选集 |
| --- | ---: | ---: |
| incoming | 162 | 80 |
| unique IDs | 162 | 80 |
| 目标表存在 | 162 | 80 |
| 备份表存在 | 162 | 80 |
| 当前整行与备份完全一致 | 162 | 80 |
| 当前仍为目标占位类型 | 162 | 80 |
| source 表存在 | 不适用 | 80 |
| manifest 任意状态重叠 | 0 | 0 |
| A/B/C pending ID 重叠 | 0 | 0 |

整行比较口径：

- `public.learning_exercises` 对 `maintenance.regen_20260727_learning_exercises_backup`，备份侧仅去掉 `backed_up_at`
- `public.learning_card_enrichments` 对 `maintenance.regen_20260727_card_enrichments_backup`，备份侧仅去掉 `backed_up_at`

## Meta Batch10

筛选条件：

1. 当前 `answer LIKE '用于%'`。
2. 排除 Meta Batch9 与 Letter Batch35–37 的 pending exercise ID。
3. 目标与备份存在，当前整行未漂移。
4. `maintenance.regen_20260727_manifest` 任意状态均不得出现同 ID。
5. 按 `episode, sort_order, id` 排序，取全部剩余项。

只读结果：

| 指标 | 数量 |
| --- | ---: |
| 当前「用于%」 | 122 |
| Meta Batch9 pending | 80 |
| manifest 重叠 | 0 |
| 缺备份 | 0 |
| 漂移 | 0 |
| 最终选入 | 42 |

42 条全部来自 episode 43。

- ordered ID MD5（UTF-8，以换行连接且末尾无换行）：`48fcd05c324b7c4eccc37ed06187f57f`
- 文件 SHA-256：`A1CEFE11FF745F14DD04ED6A30D178BFAA230E4DC30038FFBC2D8C6F68D036ED`

## Letter Batch38–40

筛选条件：

1. 当前答案匹配单个 ASCII 字母 `^[A-Za-z]$`；数据库实测只出现 A/B/C/D。
2. 排除 Meta Batch9 与 Letter Batch35–37 的 pending exercise ID。
3. 目标与备份存在，当前整行未漂移。
4. manifest 任意状态零重叠。
5. 按 `episode, sort_order, id` 排序取前 120 条，再依次切为三批，每批 40 条。

只读结果：

| 指标 | 数量 |
| --- | ---: |
| 当前单字母答案 | 904 |
| Letter Batch35–37 pending | 120 |
| 排除后合格池 | 784 |
| manifest 重叠 | 0 |
| 缺备份 | 0 |
| 漂移 | 0 |
| 最终选入 | 120 |

当前单字母值分布为 A=285、B=200、C=214、D=205。

| 计划批次 | 数量 | 首 ID | 末 ID | episode |
| --- | ---: | --- | --- | ---: |
| 38 | 40 | `re-zero-s01e22-exercise-065` | `re-zero-s01e22-exercise-105` | 22 |
| 39 | 40 | `re-zero-s01e22-exercise-106` | `re-zero-s01e22-exercise-145` | 22 |
| 40 | 40 | `re-zero-s01e22-exercise-146` | `re-zero-s01e22-exercise-185` | 22 |

- ordered ID MD5（UTF-8，以换行连接且末尾无换行）：`24ce47db676dbcfc33499b204de7c652`
- 文件 SHA-256：`8AA52EEA58AB8E3C92DFFCAD47D8B8C0DCD1AD7D9CBF2153418100617E6B57F2`

## Linguistic Batch12

高置信旧模板口径沿用 `generation_qa_linguistic_batch11.md`：

1. `linguistic_prompt_version = 'linguistic-card-v1'`。
2. 旧 `linguistic_payload` 非空。
3. 先排除 manifest 任意状态记录与 Linguistic Batch11 pending ID。
4. 目标、备份与对应 source 均存在，当前整行未漂移。
5. 展平旧 payload 的字符串叶，只统计长度不低于 12 的文本。
6. 字符串叶至少跨 3 张守卫后旧卡重复。
7. 每张候选至少命中 5 个这类长重复叶。

排序口径：

- vocab：按最早 occurrence 的 `episode, first_line_no, source_id, id`
- grammar / sentence：按 source 的 `episode, sort_order, source_id, id`

只读结果：

| 指标 | 数量 |
| --- | ---: |
| 当前旧 prompt 且 payload 非空 | 785 |
| Linguistic Batch11 pending | 80 |
| 共同守卫后 | 705 |
| 高置信重复模板池 | 361 |
| 最终选入 | 80 |

| source_type | 高置信池 | 选入 | 高置信池重复长叶命中范围 | 最广重复叶跨卡数 |
| --- | ---: | ---: | ---: | ---: |
| vocab | 169 | 40 | 5–10 | 228 |
| grammar | 159 | 20 | 13–17 | 228 |
| sentence | 33 | 20 | 14–16 | 228 |

三类池均足够，因此无需降配。入选 vocab 40/40 均存在 occurrence。

| source_type | 首 ID | 末 ID | episode 范围 |
| --- | --- | --- | --- |
| vocab | `vocab-card-v1:vocab-re-zero-shitto-no-majo` | `vocab-card-v1:re-zero-vocab-確率` | 1–3 |
| grammar | `grammar-card-v1:re-zero-s01e04-grammar-050` | `grammar-card-v1:re-zero-s01e05-grammar-059` | 4–5 |
| sentence | `sentence-card-v1:re-zero-s01e03-sentence-035` | `sentence-card-v1:re-zero-s01e04-sentence-046` | 3–4 |

- ordered ID MD5（UTF-8，以换行连接且末尾无换行）：`6d0ab59bedb023fe0b4ab204edb8ad11`
- 文件 SHA-256：`3946FBB7DDD3D713393D49D84E4D3BFD1211045FEB0F29FC359165E1C7F67D1D`

## 文件结构与禁限检查

- 三个 JSON 分别为 42 / 120 / 80 条，唯一 ID 数分别为 42 / 120 / 80。
- 文件 ID 顺序与数据库只读查询结果逐项一致。
- Meta 项仅含：`id/table_name/source_type/source_id/episode/sort_order/exercise_type/placeholder_type`。
- Letter 项仅额外含：`selection_order/planned_batch`。
- Linguistic 项仅额外含旧 prompt 版本、重复叶命中计数和类型内顺序等非教学诊断元数据。
- 文件不含 prompt、answer、hint、payload、linguistic_payload 或任何教学字段。
- 本轮未执行 INSERT、UPDATE、DELETE、DDL 或 RPC 写入。
