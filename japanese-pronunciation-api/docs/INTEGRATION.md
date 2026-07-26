# 日语跟读真实评测 API 接入文档

版本：`v1.0`  
环境：Cloudflare Production  
API 根地址：`https://ajl-pronunciation-api.ishallnotwant123.workers.dev`

## 1. 先说明“30 条真人黄金样本”

30 条真人黄金样本不是 API 接入前置条件，也不是说现在的 API 不能用。

当前生产 API 已经可以录音、识别、对齐、评分并返回错误片段。30 条样本用于接入后校准目前的暂定阈值，例如：

- 正常发音不应被大量误判为错误。
- 故意漏读、错读时应能降低相应分项。
- 静音、过小和严重噪声应返回 `re_record`，而不是一个假低分。
- 模型意见冲突会降低可靠度，但只要仍有可评分音拍就返回练习分；完全没有识别证据时才返回 `uncertain`。
- `excellent`、`good`、`fair`、`retry` 的分界是否符合真人听感。

因此现在可以直接接入和手动测试。校准完成前，建议在界面上把分数标记为“Beta”或“体验评分”，暂时不要把它当成考试成绩。

## 2. 当前可测范围

生产服务在线，健康检查为：

```http
GET /health
```

生产 KV 已覆盖 `learning_sentences` 中可由 App 生成跟读题的全部 8,560 个课程句子，其中包含学习计划显式引用的 4,942 个唯一句子。App 必须直接使用课程接口返回的稳定 `sentence.id`，例如 `k-on-ep01-sent-00046`；不要再按日文文本、集数或行号硬编码单句白名单。

参考句可通过下面的命令从 Supabase 学习计划重新同步并批量写入 KV：

```powershell
pnpm seed:sync:remote
```

同步脚本会为每个跟读题生成假名、实际发音层、词级拍序列和节奏参考。字幕时间轴缺失时，响应中的 `reference.timingSource` 为 `estimated`，Android 会把该分项显示成“节奏参考”；准确度、完整度和清晰度仍来自本次真实录音识别。

## 3. 接口总览

### 3.1 健康检查

```http
GET https://ajl-pronunciation-api.ishallnotwant123.workers.dev/health
```

不需要鉴权。正常返回 HTTP 200。

### 3.2 发音评测

```http
POST https://ajl-pronunciation-api.ishallnotwant123.workers.dev/v1/pronunciation/evaluate
Authorization: Bearer <60 秒短票据>
Content-Type: multipart/form-data; boundary=...
```

multipart 字段：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `sentenceId` | string | 是 | 当前跟读题的课程 `sentence.id` |
| `attemptId` | UUID | 是 | 每次新录音生成一个；网络重试复用原值 |
| `mode` | string | 是 | `pronunciation` 或 `shadowing` |
| `clientVersion` | string | 否 | 例如 `android-0.1.0` |
| `audio` | file | 是 | PCM16、16 kHz、单声道 WAV |

不要把标准答案文本发给 API。服务端会根据 `sentenceId` 从 KV 读取经过审核的标准发音层。

## 4. 音频硬性要求

- 容器：RIFF/WAVE
- 编码：signed PCM16 little-endian
- 采样率：16,000 Hz
- 声道：单声道
- 时长：0.4–15 秒，建议 2–10 秒
- 音频文件：不超过 1.5 MB
- 完整 multipart 请求：不超过 1.5625 MB

不能直接上传 Android 默认的 AAC/M4A，也不能直接上传浏览器生成的 WebM/Opus。必须先编码或转换成上述 WAV。

## 5. 鉴权设计

评测 Worker 不直接接受 App 的登录 Cookie。正确调用链是：

```text
已登录 Android / Web App
  -> anime-japanese-lab 主站 Worker
  -> POST /api/pronunciation/ticket
  -> 返回只适用于某一句、60 秒有效的短票据
  -> App 直传 WAV 到评测 Worker
  -> scored / uncertain / re_record
```

App、浏览器和 APK 中都不能保存 `AUTH_HMAC_SECRET`、Cloudflare API Token 或其他长期密钥。

### 5.1 给主站 Worker 配置同源密钥

主站 Worker 可以使用变量名 `PRONUNCIATION_AUTH_HMAC_SECRET`。它的值必须和评测项目 `.dev.vars` 中的 `AUTH_HMAC_SECRET` 完全相同，但变量名不必相同。

在主项目目录执行：

```powershell
cd C:\Users\汪家俊\jps
pnpm exec wrangler secret put PRONUNCIATION_AUTH_HMAC_SECRET
```

按提示粘贴同一个密钥。不要把密钥写进 `wrangler.toml`、TypeScript、Kotlin、Git 或聊天记录。主站本地开发时，在主项目 `.dev.vars` 中加入同名变量。

### 5.2 主站 Worker 签发票据

可直接复用项目中的 [`integration/ticket-signer.ts`](../integration/ticket-signer.ts)。给主站 `Env` 增加：

```ts
PRONUNCIATION_AUTH_HMAC_SECRET: string
```

在主站 Worker 的 `/api/` 路由中增加：

```ts
if (url.pathname === '/api/pronunciation/ticket' && request.method === 'POST') {
  return handlePronunciationTicket(request, env)
}
```

示例处理函数：

```ts
import {
  createEvaluationTicket,
  normalizePronunciationSentenceId,
  pronunciationSentenceLookupPath,
} from './server/pronunciationTicket'

async function handlePronunciationTicket(request: Request, env: Env) {
  const auth = await getAuthContext(request, env)
  if (!auth.user) {
    return Response.json(
      { error: { message: 'Authentication required' } },
      { status: 401, headers: { 'Cache-Control': 'no-store' } },
    )
  }

  const body = await request.json().catch(() => null) as { sentenceId?: unknown } | null
  const sentenceId = normalizePronunciationSentenceId(body?.sentenceId)
  if (!sentenceId) {
    return Response.json({ error: { message: 'Invalid pronunciation sentence ID' } }, { status: 400 })
  }
  const sentences = await supabase<Array<{ id: string }>>(env, pronunciationSentenceLookupPath(sentenceId))
  if (sentences.length === 0) {
    return Response.json(
      { error: { message: 'Sentence is not enabled for pronunciation evaluation' } },
      { status: 404, headers: { 'Cache-Control': 'no-store' } },
    )
  }

  const ticket = await createEvaluationTicket({
    subject: auth.user.id,
    sentenceId,
    secret: env.PRONUNCIATION_AUTH_HMAC_SECRET,
    ttlSeconds: 60,
  })

  return Response.json(
    { ticket, expiresIn: 60 },
    { headers: { 'Cache-Control': 'no-store' } },
  )
}
```

将 `integration/ticket-signer.ts` 复制到主项目时，只复制代码，不复制或硬编码密钥。主站必须确认 `sentenceId` 存在于课程句库，不能让客户端为任意字符串签票。

## 6. Android 原生 App 接入

你的 Android 工程当前使用 `HttpURLConnection` 和 `org.json`，无需为了这一个接口引入 Retrofit 或 OkHttp。

### 6.1 权限

在 `android-app/app/src/main/AndroidManifest.xml` 增加：

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Android 运行时还要申请 `Manifest.permission.RECORD_AUDIO`。用户拒绝时停留在待录音状态，不要发送空文件。

### 6.2 申请短票据

在现有 `RemoteLabClient` 中增加一个公开方法。它会自动携带现有 `ajl_session` Cookie：

```kotlin
fun createPronunciationTicket(sentenceId: String): String {
    val body = JSONObject().put("sentenceId", sentenceId)
    val response = JSONObject(post("/api/pronunciation/ticket", body))
    return response.optString("ticket").ifBlank {
        error("Pronunciation ticket response did not include a ticket")
    }
}
```

票据只有 60 秒有效期，所以必须在录音结束后、准备上传前申请，不要在进入页面时提前申请。

### 6.3 录制 WAV

Android 建议使用 `AudioRecord`，参数固定为：

```kotlin
val sampleRate = 16_000
val channelConfig = AudioFormat.CHANNEL_IN_MONO
val audioFormat = AudioFormat.ENCODING_PCM_16BIT
```

`AudioRecord` 读出的是裸 PCM 字节。停止录音后必须加上 44 字节 RIFF/WAVE 头，再作为 `audio/wav` 上传。WAV 头至少应包含：

```text
RIFF + fileSize + WAVE
fmt  + PCM(1) + mono(1) + sampleRate(16000) + byteRate(32000)
blockAlign(2) + bitsPerSample(16)
data + pcmByteLength
```

录音状态建议限制在 15 秒内自动停止；低于 0.4 秒时在本地提示重新录制，不必消耗一次 API 请求。

### 6.4 Android multipart 上传

以下代码风格与现有 `RemoteLabClient` 一致。HTTP 200 和 HTTP 422 都要读取 JSON；422 是正常的 `re_record` 业务结果，不是崩溃条件。

```kotlin
private const val PRONUNCIATION_BASE =
    "https://ajl-pronunciation-api.ishallnotwant123.workers.dev"

fun evaluatePronunciation(
    ticket: String,
    sentenceId: String,
    attemptId: String,
    wavBytes: ByteArray,
): JSONObject {
    val boundary = "ajl-${UUID.randomUUID()}"
    val connection = (
        URL("$PRONUNCIATION_BASE/v1/pronunciation/evaluate")
            .openConnection() as HttpURLConnection
        ).apply {
        requestMethod = "POST"
        connectTimeout = 15_000
        readTimeout = 60_000
        doOutput = true
        setChunkedStreamingMode(16 * 1024)
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", "Bearer $ticket")
        setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
    }

    fun textPart(name: String, value: String): ByteArray = (
        "--$boundary\r\n" +
            "Content-Disposition: form-data; name=\"$name\"\r\n\r\n" +
            "$value\r\n"
        ).toByteArray(StandardCharsets.UTF_8)

    connection.outputStream.use { output ->
        output.write(textPart("sentenceId", sentenceId))
        output.write(textPart("attemptId", attemptId))
        output.write(textPart("mode", "shadowing"))
        output.write(textPart("clientVersion", "android-0.1.0"))
        output.write(
            (
                "--$boundary\r\n" +
                    "Content-Disposition: form-data; name=\"audio\"; filename=\"attempt.wav\"\r\n" +
                    "Content-Type: audio/wav\r\n\r\n"
                ).toByteArray(StandardCharsets.UTF_8),
        )
        output.write(wavBytes)
        output.write("\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8))
    }

    val status = connection.responseCode
    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
    val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
    connection.disconnect()

    val json = JSONObject(body)
    if (status == 200 || (status == 422 && json.optString("assessmentStatus") == "re_record")) {
        return json
    }
    error("Pronunciation API HTTP $status: ${body.take(240)}")
}
```

需要的 imports：

```kotlin
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
```

每次用户重新录音时使用新的 `UUID.randomUUID().toString()`。只有网络超时、断网或 HTTP 502 后重发同一段录音时，才复用原 `attemptId`。

## 7. Web / PWA 接入

### 7.1 浏览器录音转 PCM16 WAV

浏览器 `MediaRecorder` 通常生成 WebM/Opus，不能直接上传。可在停止录音后用 Web Audio API 解码、降采样并编码：

```ts
export async function toPcm16MonoWav(input: Blob): Promise<Blob> {
  const context = new AudioContext()
  try {
    const decoded = await context.decodeAudioData(await input.arrayBuffer())
    const sampleRate = 16_000
    const frameCount = Math.max(1, Math.ceil(decoded.duration * sampleRate))
    const offline = new OfflineAudioContext(1, frameCount, sampleRate)
    const source = offline.createBufferSource()
    source.buffer = decoded
    source.connect(offline.destination)
    source.start()
    const rendered = await offline.startRendering()
    const samples = rendered.getChannelData(0)
    const buffer = new ArrayBuffer(44 + samples.length * 2)
    const view = new DataView(buffer)

    const ascii = (offset: number, value: string) => {
      for (let index = 0; index < value.length; index += 1) {
        view.setUint8(offset + index, value.charCodeAt(index))
      }
    }

    ascii(0, 'RIFF')
    view.setUint32(4, 36 + samples.length * 2, true)
    ascii(8, 'WAVE')
    ascii(12, 'fmt ')
    view.setUint32(16, 16, true)
    view.setUint16(20, 1, true)
    view.setUint16(22, 1, true)
    view.setUint32(24, sampleRate, true)
    view.setUint32(28, sampleRate * 2, true)
    view.setUint16(32, 2, true)
    view.setUint16(34, 16, true)
    ascii(36, 'data')
    view.setUint32(40, samples.length * 2, true)

    for (let index = 0; index < samples.length; index += 1) {
      const sample = Math.max(-1, Math.min(1, samples[index] ?? 0))
      view.setInt16(44 + index * 2, sample < 0 ? sample * 0x8000 : sample * 0x7fff, true)
    }
    return new Blob([buffer], { type: 'audio/wav' })
  } finally {
    await context.close()
  }
}
```

正式版本优先用 `AudioWorklet` 直接采集和降采样；上面的方法适合先把手动联调跑通。

### 7.2 Web 请求

```ts
const sentenceId = currentShadowingSentence.id
const attemptId = crypto.randomUUID()

const ticketResponse = await fetch('/api/pronunciation/ticket', {
  method: 'POST',
  credentials: 'include',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ sentenceId }),
})
if (!ticketResponse.ok) throw new Error(await ticketResponse.text())
const { ticket } = await ticketResponse.json() as { ticket: string }

const form = new FormData()
form.set('sentenceId', sentenceId)
form.set('attemptId', attemptId)
form.set('mode', 'shadowing')
form.set('clientVersion', 'web-0.1.0')
form.set('audio', wavBlob, 'attempt.wav')

const response = await fetch(
  'https://ajl-pronunciation-api.ishallnotwant123.workers.dev/v1/pronunciation/evaluate',
  {
    method: 'POST',
    headers: { Authorization: `Bearer ${ticket}` },
    body: form,
  },
)

const result = await response.json()
if (response.status !== 200 && !(response.status === 422 && result.assessmentStatus === 're_record')) {
  throw new Error(JSON.stringify(result))
}
```

不要手动设置 multipart 的 `Content-Type`；浏览器会自动补上 boundary。

生产 CORS 当前允许：

- `https://anime-japanese-lab.ishallnotwant123.workers.dev`
- `http://localhost:5173`
- `http://127.0.0.1:5173`

原生 Android 请求通常不携带 `Origin`，不受浏览器 CORS 限制。如果 Web 域名或开发端口发生变化，需要更新评测项目 `wrangler.jsonc` 的 `ALLOWED_ORIGINS` 后重新部署。

## 8. 成功响应

`assessmentStatus: "scored"` 时返回 HTTP 200。下面是结构示例；实际文字、分数和时间戳由录音决定：

```json
{
  "ok": true,
  "attemptId": "a48d40a9-6380-4ec6-a863-a01bef21aa7d",
  "sentenceId": "k-on-ep01-sent-00046",
  "assessmentStatus": "scored",
  "scorerVersion": "ja-mora-v1.1.0",
  "reference": { "timingSource": "subtitle" },
  "engine": {
    "primary": "@cf/deepgram/nova-3",
    "secondary": "@cf/openai/whisper-large-v3-turbo",
    "fallbackUsed": true,
    "reliability": 0.755
  },
  "recognized": {
    "text": "正確には配布寸前ね",
    "phoneticKana": "せいかくにわはいふすんぜんね",
    "durationMs": 3024
  },
  "score": {
    "overall": 83,
    "accuracy": 88,
    "completeness": 100,
    "clarity": 78,
    "fluency": 80,
    "rhythm": 82,
    "band": "good"
  },
  "audioQuality": {
    "status": "ok",
    "scorable": true,
    "durationMs": 3024,
    "speechDurationMs": 2610,
    "speechStartMs": 160,
    "speechEndMs": 2800,
    "speechRatio": 0.863,
    "clippingRatio": 0.001,
    "rms": 0.11,
    "peak": 0.76,
    "noiseFloor": 0.01,
    "snrDb": 20.8,
    "qualityScore": 91,
    "reasons": [],
    "speechSegments": [{ "startMs": 160, "endMs": 2800 }]
  },
  "segments": [
    {
      "tokenIndex": 3,
      "surface": "廃部",
      "expected": "はいぶ",
      "heard": "はいふ",
      "status": "unclear",
      "score": 67,
      "message": "「廃部」疑似不够清楚",
      "startMs": 1180,
      "endMs": 1670
    }
  ],
  "feedback": ["重点检查「廃部」这一段。"]
}
```

`score` 表示读得怎么样；`engine.reliability` 表示系统对这次判断有多确定。两者不能混成一个数字。

## 9. 三种业务结果如何显示

| HTTP | `assessmentStatus` | App 行为 |
|---:|---|---|
| 200 | `scored` | 显示总分、分项、可靠度、逐词高亮和问题时间片 |
| 200 | `uncertain` | 仅在完全没有可评分识别证据时不显示分数并提示重录 |
| 422 | `re_record` | 不显示低分；根据 `audioQuality.status` 提示重新录制 |

`audioQuality.status` 映射：

| 状态 | 建议文案 |
|---|---|
| `too_short` | 录音太短，请读完整句子 |
| `too_long` | 录音超过 15 秒，请重新录制 |
| `silent` | 没有检测到有效语音 |
| `too_quiet` | 音量太小，请靠近麦克风 |
| `clipped` | 音量过大出现爆音，请稍微远离麦克风 |
| `noisy` | 环境噪声太大，请换安静位置 |

segment 状态：

| 状态 | 含义 |
|---|---|
| `correct` | 与目标发音层一致 |
| `unclear` | 有疑点但证据不足以断言具体音素错误 |
| `substitution` | 疑似读成其他拍或词 |
| `omission` | 疑似漏读 |
| `extra` | 疑似多读或重复 |
| `pause` | 停顿明显异常 |
| `unscored` | 当前证据不足，不评分 |

只突出 1–3 个非 `correct` segment。`unclear` 和 `substitution` 的 UI 文案应保留“疑似”，不要宣传成舌位、口型或音素级确诊。

当 `startMs`、`endMs` 都存在且 `endMs > startMs` 时，可以回放用户录音中的对应时间片。它们只对应用户录音，不能直接套到原声时间轴。

## 10. HTTP 错误处理

错误响应格式：

```json
{
  "ok": false,
  "error": {
    "code": "ticket_expired",
    "message": "评测票据已过期",
    "requestId": "b8b03f6e-f788-4655-97ae-6fe7701482c4"
  }
}
```

| HTTP | 常见 code | 客户端处理 |
|---:|---|---|
| 400 | `bad_request` | 检查 multipart 字段、WAV 格式和 UUID |
| 401 | `unauthorized` / `ticket_expired` | 重新向主站申请票据后重试一次 |
| 403 | `ticket_scope_mismatch` / `origin_not_allowed` | 检查票据句子或 Web Origin |
| 404 | `not_found` | 当前句子不在课程句库或评测 KV 同步失败 |
| 409 | `attempt_conflict` | 生成新 `attemptId`；不要跨用户或跨句复用 |
| 413 | `audio_too_large` | 缩短录音；不要压成其他不支持格式 |
| 429 | `rate_limited` | 保留录音，稍后再试 |
| 502 | `asr_unavailable` | 保留录音和原 `attemptId`，允许稍后重试 |

当前限流是同一用户、IP、句子组合每 60 秒 20 次。

## 11. 不改 App 的命令行手测

准备一段符合要求的 WAV 后，可以先绕过主站票据端点做一次调用。短票据从本机被 Git 忽略的 `.dev.vars` 生成，120 秒后自动失效。

```powershell
cd C:\Users\汪家俊\jps\japanese-pronunciation-api
$ticket = node .\scripts\create-ticket.mjs --sentence k-on-ep01-sent-00046 --subject manual-test --ttl 120
$attemptId = [guid]::NewGuid().ToString()

curl.exe `
  -X POST `
  "https://ajl-pronunciation-api.ishallnotwant123.workers.dev/v1/pronunciation/evaluate" `
  -H "Authorization: Bearer $ticket" `
  -F "sentenceId=k-on-ep01-sent-00046" `
  -F "attemptId=$attemptId" `
  -F "mode=shadowing" `
  -F "clientVersion=manual-curl" `
  -F "audio=@C:\你的路径\test.wav;type=audio/wav"
```

如果手头录音不是目标 WAV 格式，可先转换：

```powershell
ffmpeg -i .\input.m4a -ac 1 -ar 16000 -c:a pcm_s16le .\test.wav
```

## 12. 推荐的第一次人工测试

按下面顺序测试，最容易定位是录音、鉴权还是评测问题：

1. 调 `/health`，确认 HTTP 200。
2. 正常读 `正確には廃部寸前ね`，确认得到 `scored` 或诚实的 `uncertain`。
3. 录 1 秒静音，确认 HTTP 422 + `re_record` + `silent`。
4. 故意把「廃部（はいぶ）」读成「配布（はいふ）」，观察 `廃部` segment 是否被标记。
5. 故意漏读「寸前」，观察 completeness 和 omission。
6. 对同一个 `attemptId` 重发完全相同请求，确认响应头 `X-Idempotent-Replay: true`。
7. 新录音必须换新 `attemptId`，否则会拿到旧结果。

记录每次的 `attemptId`、真人主观判断、API 状态、overall、reliability 和问题 segment。这批记录之后就能逐步形成黄金样本。

## 13. 接入验收清单

- [ ] `/health` 返回 200。
- [ ] APK 已声明并运行时申请 `RECORD_AUDIO`。
- [ ] 客户端只上传 PCM16、16 kHz、单声道 WAV。
- [ ] 主站已配置与评测 Worker 相同值的 HMAC secret。
- [ ] 票据只在服务端签发，APK 和前端没有长期密钥。
- [ ] 所有跟读题都直接使用课程接口返回的稳定 `sentence.id`。
- [ ] 生产 KV 条目数覆盖课程句库中的全部 8,560 个可出题句子。
- [ ] 录音结束后才申请 60 秒票据。
- [ ] 每段新录音使用新的 UUID `attemptId`。
- [ ] 422 被解析为 `re_record`，不是通用网络错误。
- [ ] `uncertain` 不展示分数。
- [ ] `score` 与 `reliability` 分开展示。
- [ ] UI 不把疑似识别问题宣传成音素级确诊。
- [ ] 原始录音不写日志、不进 D1、不上传 R2。

完整机器可读契约见 [`openapi.yaml`](../openapi.yaml)，共享 TypeScript 类型见 [`src/contracts.ts`](../src/contracts.ts)。
