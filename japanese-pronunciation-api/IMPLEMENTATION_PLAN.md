# Plan

在独立 Cloudflare Worker 项目中交付真实日语跟读评测 API：先以 Nova-3 提供主识别和词级证据，低置信时调用 Whisper 复核，再由可版本化的确定性拍对齐与音频规则输出分数、可靠度和问题时间片。项目按隐私优先原则不保存原始录音，并以 KV、D1、原生限流和 HMAC 短票据形成可上线的 P0 + 关键 P1 基线。

## Scope

- In: 16 kHz/单声道/PCM16 WAV 门控；Nova 主识别；Whisper 条件复核；日语课程词典、助词发音层和拍级加权对齐；总分/分项/可靠度/逐词时间片；KV 参考句；D1 幂等与版本；Cloudflare 原生限流；CORS；短票据鉴权；测试、部署和运维文档。
- Out: 现有 Web/Android 前端改造；R2 调试录音；实时 WebSocket 字幕；F0/语调和声线相似度；GOP/forced alignment 音素服务；真人黄金样本的最终阈值校准。

## Action items

[x] 对照 Markdown 与 Word 方案，采用 Word 版的完整 API、目录、类型和验收清单，并复核当前 Cloudflare 模型 schema。
[x] 建立 `ajl-pronunciation-api` 独立 Worker 工程，生成 Wrangler 绑定类型并启用日志、追踪、定时清理和生产配置。
[x] 实现 WAV 解析/质量门控、日语规范化、课程词典、助词发音层、拍级动态规划和确定性评分器。
[x] 接通 Nova-3 与条件 Whisper，处理模型冲突、空转写、未知文本、`uncertain` 和 `re_record`，且不把完整答案作为提示。
[x] 增加 HMAC 60 秒票据、严格 CORS、1.5 MB/15 秒限制、Cloudflare Rate Limiting、D1 幂等和 7 天结构化结果保留。
[x] 建立 KV/D1 生产资源、迁移/种子脚本、共享票据签发器、OpenAPI 和主项目接入说明。
[x] 运行 lint、严格类型检查、17 项单元测试、Wrangler dry-run/startup 检查及线上健康/评分/幂等/静音冒烟验证。
[ ] 收集并双人标注至少 30 条真人黄金样本，按 `docs/CALIBRATION.md` 调整置信度、fallback、reliability 和分数区间。
[ ] 在现有主项目新增受会话保护的票据签发端点，并将旧人工 1/2/3 自评 UI 替换成录音、上传、逐词高亮和片段回放。
[ ] 完成 iPad Safari/PWA、Windows Chrome/Edge 与 Android 客户端的真人实机验收后，再进入 Beta/Production 阈值冻结。

## Open questions

- 正式 Web 域名确定后，是否需要替换或追加 `ALLOWED_ORIGINS` 中的 workers.dev 域名？
- 真人黄金样本由哪些母语者/学习者提供，以及谁负责第二标注人？
- D1 结构化结果当前保留 7 天；接入现有学习历史后是否缩短为 24 小时或完全关闭？
