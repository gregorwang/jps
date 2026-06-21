# Learning Card Content Enrichment Plan

## Goal

学习卡现在能做到“先学再练”，但内容仍偏薄。下一步不要把 AI Gateway 接成实时生成，因为实时生成会带来等待、成本和不稳定输出。正确方向是离线内容增强：用脚本提前生成结构化学习卡内容，写回 Supabase，前端学习卡只读取缓存字段。

这个方案不做 AI 出题审核后台，也不做可视化审核后台。它是一个开发侧/数据侧的内容补全流程。

## Current Problem

当前学习卡主要依赖已有字段：

- 词汇：`surface`、`reading`、`romaji`、`meaning_zh`、`pos`、`jlpt_level`、`anime_tone_note`、`real_world_note`
- 语法：`pattern`、`function_zh`、`ja_example`、`explanation_zh`、`pragmatics_note`、`real_world_note`
- 句子：`ja_text`、`romaji`、`meaning_zh`、`tone_tags`

这些字段足够做基础卡片，但不够支撑“学完立刻做题”的理解成本。尤其是语法卡，用户需要更清楚地知道：

- 这个结构到底是什么意思
- 为什么这个例句里这么用
- 它和相近表达有什么差别
- 动漫台词里是什么语气
- 现实对话能不能这样说
- 做题时最容易错在哪里

## Recommended Architecture

采用四层结构：

1. Source tables

   继续使用现有学习材料表，例如 `learning_vocab_items`、`learning_grammar_points`、`learning_sentences`。

2. Enrichment table

   新增一张结构化增强表，按 `source_type + source_id + model + prompt_version` 缓存 AI 生成结果。

3. Offline generation script

   本地或 CI 手动运行脚本，调用 AI Gateway 批量生成内容，写入 Supabase。

4. Frontend read path

   lesson 构建时读取增强内容，学习卡优先展示增强字段；如果没有增强内容，回退到现有字段。

## Proposed Supabase Table

建议新增表：`learning_card_enrichments`

字段：

```sql
create table learning_card_enrichments (
  id text primary key,
  source_type text not null check (source_type in ('vocab', 'grammar', 'sentence')),
  source_id text not null,
  work_slug text not null,
  episode integer,
  model text not null,
  prompt_version text not null,
  quality_score integer not null default 0,
  status text not null default 'ready' check (status in ('ready', 'needs_regen', 'disabled')),
  payload jsonb not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (source_type, source_id, model, prompt_version)
);
```

`payload` 里放结构化内容，不要放一整段散文。

## Payload Shape

### Vocab

```json
{
  "headlineZh": "日常里表示“没事/没关系/不要紧”的高频表达。",
  "coreMeaningZh": "表示状态没有问题，也可以回应道歉、关心或确认。",
  "usageScenes": [
    "别人问你有没有事",
    "回应对方的道歉",
    "确认某件事是否没问题"
  ],
  "animeToneZh": "台词里常用于关心、安抚或轻松回应，语气取决于上下文。",
  "realWorldUseZh": "现实中非常常用，口语和日常消息都可以用。",
  "commonMistakes": [
    "不要把它只理解成身体没事，也可以表示安排、状态、关系没问题。",
    "正式场合可换成更完整的表达。"
  ],
  "exampleBreakdown": [
    {
      "ja": "大丈夫？",
      "zh": "你没事吧？",
      "pointZh": "升调时多是关心或确认。"
    }
  ],
  "microQuizHintZh": "看到中文“没关系/没事吧”时优先联想到这个词。"
}
```

### Grammar

```json
{
  "headlineZh": "表示“不做某事就不行”，后半句常省略。",
  "structureZh": "动词ない形 + と",
  "coreFunctionZh": "用于提醒、催促、表达必须做某事。",
  "exampleBreakdown": {
    "ja": "そろそろ起きないと...",
    "zh": "差不多该起床了，不然...",
    "parts": [
      {
        "text": "起きない",
        "roleZh": "不起床"
      },
      {
        "text": "と",
        "roleZh": "如果这样的话"
      }
    ]
  },
  "animeToneZh": "家人或熟人之间提醒时很自然，省略后半句会显得更口语。",
  "realWorldUseZh": "现实口语常用。完整说法可以是「起きないと遅れる」。",
  "nearbyExpressions": [
    {
      "pattern": "なければならない",
      "differenceZh": "更正式，更像规则或义务。"
    },
    {
      "pattern": "ないといけない",
      "differenceZh": "意思接近，但说得更完整。"
    }
  ],
  "commonMistakes": [
    "不要把省略号后的内容当成缺失错误，这是口语省略。",
    "选择题里看到提醒、催促、否则的语境时优先考虑这个结构。"
  ],
  "microQuizHintZh": "如果句子表达“不做就糟了”，答案常和「ないと」有关。"
}
```

### Sentence

```json
{
  "headlineZh": "这句话是在吐槽某种状态会逐渐形成。",
  "literalZh": "像这样，家里蹲就会形成了呢。",
  "naturalZh": "这样下去就会变成家里蹲了。",
  "chunks": [
    {
      "ja": "こうやって",
      "zh": "像这样"
    },
    {
      "ja": "ニートが",
      "zh": "家里蹲/无业状态"
    },
    {
      "ja": "出来上がっていくのね",
      "zh": "就会逐渐形成呢"
    }
  ],
  "toneZh": "带一点吐槽和总结语气，句末「のね」让判断更像感叹。",
  "shadowingTipZh": "先把「こうやって」和「出来上がっていくのね」分成两段跟读。",
  "commonMistakes": [
    "不要把「出来上がる」只理解成做完物品，这里是状态形成。",
    "「のね」不是核心事实信息，而是语气。"
  ]
}
```

## Prompt Design

每类内容单独 prompt，不要用一个大 prompt 兼容所有类型。

### Shared Requirements

模型必须输出 JSON，不能输出 Markdown。

要求：

- 使用简体中文解释。
- 不要编造作品剧情。
- 只基于输入字段解释。
- 不确定时写短句，不要扩展。
- 每个数组最多 3 项。
- 每个字段短而具体。
- 不生成题目，只生成学习卡说明。

### Vocab Prompt Sketch

```text
你是日语学习卡内容生成器。请根据输入词汇生成结构化学习说明。
只输出 JSON，字段必须符合 schema。
不要生成练习题，不要生成长篇文章。

输入：
surface: ...
reading: ...
romaji: ...
meaningZh: ...
pos: ...
jlptLevel: ...
animeToneNote: ...
realWorldNote: ...

输出 schema:
{
  "headlineZh": string,
  "coreMeaningZh": string,
  "usageScenes": string[],
  "animeToneZh": string,
  "realWorldUseZh": string,
  "commonMistakes": string[],
  "exampleBreakdown": [{"ja": string, "zh": string, "pointZh": string}],
  "microQuizHintZh": string
}
```

### Grammar Prompt Sketch

```text
你是日语语法学习卡内容生成器。请根据输入语法点生成结构化学习说明。
只输出 JSON，字段必须符合 schema。
不要生成练习题，不要生成长篇文章。
不要编造例句之外的剧情。

输入：
pattern: ...
functionZh: ...
jaExample: ...
explanationZh: ...
pragmaticsNote: ...
realWorldNote: ...
difficulty: ...

输出 schema:
{
  "headlineZh": string,
  "structureZh": string,
  "coreFunctionZh": string,
  "exampleBreakdown": {
    "ja": string,
    "zh": string,
    "parts": [{"text": string, "roleZh": string}]
  },
  "animeToneZh": string,
  "realWorldUseZh": string,
  "nearbyExpressions": [{"pattern": string, "differenceZh": string}],
  "commonMistakes": string[],
  "microQuizHintZh": string
}
```

## Script Workflow

建议新增脚本：

```text
scripts/enrich-learning-cards.ts
```

参数：

```bash
pnpm tsx scripts/enrich-learning-cards.ts --work k-on --episode 1 --type grammar --limit 20
pnpm tsx scripts/enrich-learning-cards.ts --work re-zero --episode 7 --type vocab --limit 80
```

流程：

1. 从 Supabase 读取目标材料。
2. 查询 `learning_card_enrichments`，跳过已有 `ready` 且 prompt_version 相同的记录。
3. 调用 AI Gateway。
4. 解析 JSON。
5. 用本地规则打 `quality_score`。
6. 写入 Supabase。
7. 输出命令行报告。

## Quality Rules

不需要后台审核，但脚本必须做机器检查。

基础规则：

- JSON 必须可解析。
- 必填字段不能为空。
- 字段长度不能过长。
- 数组长度不能超过 3。
- 不能出现“作为一个 AI”。
- 不能出现明显英文解释大段。
- `jaExample` 不能被模型改写成别的句子。
- `source_id` 必须能对应原表记录。

建议评分：

```text
100 起
-20 缺少关键字段
-15 字段太长
-20 JSON 修复后才可解析
-30 疑似编造例句
-10 commonMistakes 为空
-10 animeToneZh 为空
低于 70 标记 needs_regen
```

## Frontend Integration

前端不要直接调用 AI Gateway。

接口方案：

- `/api/works/:work/episodes/:episode/lesson-materials` 可以未来合并返回材料和 enrichment。
- 或者先加独立接口 `/api/learning-card-enrichments?workSlug=&episode=`

前端 lesson 构建时传入：

```ts
enrichments?: Record<string, LearningCardEnrichment>
```

key 建议：

```ts
`${sourceType}:${sourceId}`
```

学习卡展示优先级：

1. enrichment payload
2. 原始字段
3. 最小 fallback

例如语法学习卡：

- 标题：`payload.headlineZh`，fallback `functionZh`
- 结构：`payload.structureZh`，fallback `pattern`
- 例句拆解：`payload.exampleBreakdown`
- 易错点：`payload.commonMistakes`
- 语气：`payload.animeToneZh`，fallback `pragmaticsNote`
- 现实使用：`payload.realWorldUseZh`，fallback `realWorldNote`

## Why Not Real-Time AI

实时 AI 不适合学习卡首屏：

- 首屏会等待模型，学习节奏被打断。
- 同一个词每次生成可能不一致。
- 成本不可控。
- 出错时会影响练习主流程。
- iPad 网络环境下体验更差。

离线增强的优势：

- 前端读取快。
- 内容稳定。
- 可以批量重跑。
- 可以通过脚本规则过滤低质量输出。
- 以后想换 prompt/model，只需要提高 `prompt_version` 后重跑。

## Implementation Phases

### Phase 1: Data Schema

新增 `learning_card_enrichments` 表。

只做表和类型，不改 UI。

### Phase 2: Offline Script

实现 `scripts/enrich-learning-cards.ts`。

先支持 grammar，因为当前语法学习卡最薄。

### Phase 3: API Read Path

Worker 增加读取 enrichment 的接口或合并到 lesson material fetch。

### Phase 4: Lesson Card Rendering

`StudyLessonNode` 增加 `enrichment` 字段。

`LessonPlayer` 的 `StudyCard` 展示结构化内容。

### Phase 5: Batch Backfill

按优先级跑：

1. 当前主学作品当前集。
2. Re:Zero 已经在练的集数。
3. K-ON 全集语法。
4. 高频词汇。
5. 跟读句。

## Recommended First Cut

第一版只做 grammar enrichment。

原因：

- 语法卡最需要解释。
- 语法点数量少，成本低。
- 输出结构更稳定。
- 对学习体验提升最大。

第一版字段：

```json
{
  "headlineZh": "...",
  "structureZh": "...",
  "coreFunctionZh": "...",
  "exampleBreakdown": {
    "ja": "...",
    "zh": "...",
    "parts": []
  },
  "animeToneZh": "...",
  "realWorldUseZh": "...",
  "commonMistakes": [],
  "microQuizHintZh": "..."
}
```

暂时不做 `nearbyExpressions`，避免模型乱类比。

## Open Decisions

后续新会话需要确认：

- enrichment 表是否新建，还是先复用 `ai_result_cache`。
- 是否允许脚本写 Supabase service role。
- prompt_version 命名，例如 `grammar-card-v1`.
- 模型选择：建议 mini 做批量初稿，高质量模型只用于重跑低分项。
- 是否把增强内容也用于资料页，而不只用于 lesson 学习卡。
