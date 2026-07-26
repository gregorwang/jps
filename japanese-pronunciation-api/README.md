# AJL 日语跟读真实评测 API

这是一个可独立部署的 Cloudflare Worker 服务。客户端提交课程句子 ID 与 16 kHz、单声道、PCM16 WAV；服务先做录音质量门控，再使用 Nova-3 日语识别、课程词典与拍级对齐，必要时调用 Whisper 复核，最后返回确定性分数、系统可靠度、逐词状态和回放时间片。

生产地址：`https://ajl-pronunciation-api.ishallnotwant123.workers.dev`

## 已实现

- `POST /v1/pronunciation/evaluate`
- `GET /health`
- 0.4–15 秒、最大 1.5 MB 的严格 WAV 校验
- 静音、过短、过小、削波、严重噪声门控；质量失败返回 `re_record`，不伪造低分
- Nova-3 `language=ja` 主识别；不传完整标准答案提示
- Whisper Large v3 Turbo 条件复核
- NFKC、平/片假名、助词发音层、课程有限词典和拍级加权动态规划
- Accuracy / Completeness / Clarity / Fluency / Rhythm 与独立 Reliability
- `scored` / `uncertain` / `re_record`
- HMAC 短票据、Origin 白名单、Cloudflare 原生限流
- KV 参考句、D1 幂等结果与定时过期清理
- 原始录音不落盘、不写 D1、不写 R2

## 关键目录

```text
src/
  asr/             Nova / Whisper 调用与响应校验
  audio/           PCM16 WAV 解析和质量门控
  auth/            HMAC 评测票据
  japanese/        假名、课程词典和拍序列
  scoring/         动态规划、模型一致性和评分
  storage/         KV 参考句与 D1 幂等结果
  routes/          /v1/pronunciation/evaluate
data/sentences/    已预处理课程句子
integration/       主项目可复制的票据签发器
migrations/        D1 schema
scripts/           密钥、KV 种子和本地票据工具
test/              确定性单元测试
```

## 本地准备

```powershell
pnpm install
pnpm secrets:bootstrap
pnpm types
pnpm db:migrate:local
pnpm seed:sync
pnpm seed:local
pnpm dev
```

`.dev.vars` 由脚本生成并已被 Git 忽略。不要把 `AUTH_HMAC_SECRET` 放进浏览器或 Wrangler `vars`。

## 验证

```powershell
pnpm lint
pnpm typecheck
pnpm test
pnpm deploy:dry
pnpm exec wrangler check startup
```

当前基线为 17 项通过的单元测试；另已完成生产健康检查、真实双模型评分、D1 幂等重放和静音 `re_record` 冒烟测试。

## 生产发布

Cloudflare 资源已经建好并写入 `wrangler.jsonc`。后续发布顺序：

```powershell
pnpm db:migrate:remote
pnpm seed:sync:remote
pnpm secrets:deploy
pnpm deploy
```

`secrets:deploy` 通过 Wrangler 标准输入写入密钥，不把值放在命令参数或日志中。

## 模型与 AI Gateway 的当前边界

Nova-3 的音频输入目前要求 `ReadableStream`，而 AI Gateway 的 Workers 绑定会拒绝流输入。因此生产实现让 Nova 直接走同账号 Workers AI binding；Whisper 的 base64 条件复核继续走 `my-ai-gateway`，并设置 `skipCache=true`、`collectLog=false`，避免 AI Gateway 保存用户音频载荷。延迟、状态、fallback 与分数元数据由 Workers 结构化日志记录。

如果 Cloudflare 后续为 Gateway binding 增加流支持和“仅记录元数据、不记录 payload”的类型化选项，可在不改变公开 API 的前提下把 Nova 重新接入 Gateway。

## 下一步

先按 [App/API 接入说明](docs/INTEGRATION.md) 用已写入 KV 的测试句跑通录音、鉴权、上传和结果 UI，再根据 [校准指南](docs/CALIBRATION.md) 收集真人样本并调整暂定阈值。完整执行顺序见 [实施计划](IMPLEMENTATION_PLAN.md)，生产资源与验证记录见 [部署清单](docs/DEPLOYMENT.md)。
