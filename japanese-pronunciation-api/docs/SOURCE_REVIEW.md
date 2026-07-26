# 两份方案文档的实施映射

已完整读取：

- `C:/Users/汪家俊/Downloads/日语跟读真实评测API方案_Cloudflare优先_v1.0.md`
- `C:/Users/汪家俊/Downloads/日语跟读真实评测API方案_Cloudflare优先_v1.0.docx`

Markdown 是 13 节精简实现基线；Word 版提供了更完整的 39 个表格、响应 segment 示例、推荐目录、前端状态机、共享 TypeScript 类型和逐项验收清单。因此实现以 Word 版为主、Markdown 为交叉核对。

## 直接落地的要求

| 文档要求 | 实现位置 |
|---|---|
| 单一 `POST /v1/pronunciation/evaluate` | `src/routes/evaluate.ts` |
| 16 kHz / mono / PCM16 WAV | `src/audio/wav.ts` |
| 质量差返回 `re_record` | `src/audio/quality.ts`、`src/scoring/score.ts` |
| Nova `language=ja` | `src/asr/nova.ts` |
| Whisper 条件复核 | `src/asr/whisper.ts`、`src/routes/evaluate.ts` |
| 不传完整答案提示 | 两个 ASR 调用均未使用 keyterm/initial_prompt |
| 两套读音和助词发音层 | `data/sentences/*.json`、`src/japanese/normalize.ts` |
| 拍级加权对齐 | `src/scoring/align.ts` |
| 五项分数与可靠度分离 | `src/scoring/score.ts` |
| KV 参考句 | `src/storage/sentences.ts` |
| D1 版本/幂等 | `migrations/0001_init.sql`、`src/storage/attempts.ts` |
| 原音默认不保存 | 没有 R2 binding；代码不持久化 audio bytes |
| 限流、CORS、短票据 | `wrangler.jsonc`、`src/auth`、`src/http` |
| 最多 1–3 个主要问题 | `buildFeedback()` |

## 按当前 Cloudflare 状态修订的地方

1. Whisper 当前输出 schema 已包含 segment 内的 words 时间信息，因此复核器会解析这些字段；但 Nova 仍是权威的主时间证据。
2. AI Gateway Workers binding 当前不支持 Nova 所需的 `ReadableStream`。生产中 Nova 直接走 Workers AI binding；Whisper 的 base64 输入仍走 Gateway。
3. AI Gateway 默认会保存请求/响应 payload；当前 binding 类型没有 `collectLogPayload:false`，所以本项目对音频请求使用 `collectLog:false`，改由 Workers 结构化日志记录非敏感元数据。
4. 最新 Workers Vitest 集成在本机 Windows/workerd 组合中无法加载 `cloudflare:test-internal`。确定性测试改用标准 Vitest，Worker 运行时则用 Wrangler dry-run、startup check 和真实线上冒烟覆盖。

这些修订不改变公开 API，也不扩大文档所禁止的能力声明。
