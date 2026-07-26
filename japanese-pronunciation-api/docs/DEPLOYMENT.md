# 生产部署清单

部署日期：2026-07-10（Asia/Hong_Kong）

## Cloudflare 资源

| 资源 | 名称 / ID | 用途 |
|---|---|---|
| Worker | `ajl-pronunciation-api` | API、音频门控、模型编排、评分 |
| Worker version | `438ad044-2fd3-4959-8ec7-2f4b56dd4c63` | 当前 100% 流量版本 |
| URL | `https://ajl-pronunciation-api.ishallnotwant123.workers.dev` | 生产入口 |
| KV | `ajl-pronunciation-sentences-prod` | 句子参考数据 |
| KV ID | `314d5324649343b980a57547a5a84e85` | `SENTENCES` binding |
| D1 | `ajl-pronunciation-prod` | 幂等结果、评分版本 |
| D1 ID | `e9154e90-38e3-4ce8-90fe-368e61033557` | `DB` binding，APAC |
| AI Gateway | `my-ai-gateway` | Whisper 条件复核；关闭本请求日志 |
| Rate Limit | namespace `38471` | 每 key 20 次/60 秒 |
| Cron | `17 3 * * *` | 清理过期 D1 attempts |

生产密钥 `AUTH_HMAC_SECRET` 已通过 Wrangler Secret 设置，不存在于 `wrangler.jsonc`、源码或 Git。

## 已执行验证

- `pnpm lint`：通过，无 warning。
- `pnpm typecheck`：通过。
- `pnpm test`：5 个文件、17 项测试通过。
- `pnpm deploy:dry`：通过；bundle gzip 约 12 KB。
- `wrangler check startup`：通过；线上部署启动时间 4–6 ms。
- `/health`：HTTP 200。
- 缺票据：HTTP 401。
- 合法 PCM16 WAV（TTS 冒烟）的真实 Nova + Whisper 链：HTTP 200 `scored`。
- 同一 subject/sentence/attempt 重放：HTTP 200，`X-Idempotent-Replay: true`。
- 1 秒静音 WAV：HTTP 422 `re_record`，未调用 ASR。
- KV 远端读取：种子句 `kon-ep01-046` 存在。
- D1 远端查询：当前评分版本存在，attempts 可写入。

## 隐私决定

- 不创建 R2 bucket；原始录音只存在于单次 Worker 请求内存。
- D1 只保留结构化响应和散列 user ID，默认 7 天。
- Workers 日志不写 Authorization、原始音频或完整 user ID。
- Nova 直接使用 Workers AI binding，并设置 Deepgram `mip_opt_out=true`。
- Whisper 通过 AI Gateway，但 `collectLog=false`、`skipCache=true`，避免保存 base64 音频 payload。

## 回滚

```powershell
pnpm exec wrangler deployments list --name ajl-pronunciation-api
pnpm exec wrangler rollback --name ajl-pronunciation-api
```

回滚 Worker 不会自动回滚 KV/D1。数据库变更必须使用向后兼容迁移；不要手动删除生产 D1 或 KV。
