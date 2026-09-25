from pathlib import Path

README = """# Anime Japanese Lab — Vectorize Ingest Worker

日文字幕主语料（`chunks.jsonl`）入库 Cloudflare Vectorize 的极简 Worker。

```txt
chunks.jsonl → POST /admin/upsert → Workers AI embedding → Vectorize upsert
```

**重要：**

- `chunks.jsonl` 的 `text` 必须是**日文主语料**
- **中文字幕不能**导入这个 Vectorize 主库
- staff credit 应先在 `preprocess.py` 过滤；Worker 会做**二次过滤**
- 不接 Supabase、不做网页 UI、不做读空气 LLM

## 安装

```bash
cd vector-ingest
npm install
```

## Wrangler 绑定

| Binding | 名称 |
|---------|------|
| `AI` | Workers AI |
| `VECTORIZE` | `anime-japanese-lab-subtitles` |

Worker 名称：`anime-japanese-lab-vector-ingest`

## 确认 Embedding 维度

不要硬猜维度。`@cf/baai/bge-m3` 通常为 **1024**，请实测：

```powershell
$env:CLOUDFLARE_ACCOUNT_ID = "your-account-id"
$env:CLOUDFLARE_API_TOKEN = "your-api-token"
npm run probe:embedding
```

## 创建 Vectorize Index

```bash
npx wrangler vectorize list
```

若不存在（将 `1024` 换成探测结果）：

```bash
npx wrangler vectorize create anime-japanese-lab-subtitles --dimensions=1024 --metric=cosine
```

Metadata 索引（`/query` filter 需要）：

```bash
npx wrangler vectorize create-metadata-index anime-japanese-lab-subtitles --property-name=work --type=string
npx wrangler vectorize create-metadata-index anime-japanese-lab-subtitles --property-name=episode --type=number
npx wrangler vectorize create-metadata-index anime-japanese-lab-subtitles --property-name=language --type=string
```

## ADMIN_TOKEN

本地：`.dev.vars.example` → `.dev.vars`

生产：`npx wrangler secret put ADMIN_TOKEN`

## 本地运行 / 部署

```bash
npm run dev
npm run deploy
curl http://127.0.0.1:8787/health
```

远程 AI/Vectorize：`npx wrangler dev --remote`

## 上传 chunks

```powershell
$env:INGEST_WORKER_URL = "https://anime-japanese-lab-vector-ingest.<subdomain>.workers.dev"
$env:ADMIN_TOKEN = "your-secret"
npm run upload:chunks
```

读取 `../output/vectorize/chunks.jsonl`，每批 50 条。

## 测试 `/query`

```powershell
Invoke-RestMethod -Method POST `
  -Uri "$env:INGEST_WORKER_URL/query" `
  -ContentType "application/json" `
  -Body '{"query":"唯が天然な場面","topK":5,"filter":{"work":"k-on","language":"ja"}}'
```

## API

| Method | Path | Auth |
|--------|------|------|
| GET | `/health` | 无 |
| POST | `/admin/upsert` | `Bearer ADMIN_TOKEN` |
| POST | `/query` | 无 |

更换模型：修改 `src/index.ts` 的 `EMBEDDING_MODEL`，重建 index 并重新上传。

## 常见错误

| 现象 | 处理 |
|------|------|
| 401 | 检查 `ADMIN_TOKEN` |
| dimension mismatch | 按探测维度重建 index |
| filter 无结果 | 创建 metadata index 后重新 upsert |
| staff credit rejected | 重跑 `preprocess.py` |

```bash
npm run typecheck
```
"""

Path(__file__).with_name("README.md").write_text(README, encoding="utf-8", newline="\n")
print("README written")
