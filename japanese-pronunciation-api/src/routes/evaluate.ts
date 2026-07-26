import type { EvaluationMode, PronunciationEvaluation } from "../contracts";
import type { RuntimeConfig } from "../config";
import { parseBoundedFormData } from "../http/body";
import { ApiError, UpstreamAsrError } from "../errors";
import { verifyEvaluationTicket } from "../auth/ticket";
import { sha256Hex } from "../utils/crypto";
import { loadSentenceReference } from "../storage/sentences";
import { findCachedAttempt, saveAttempt } from "../storage/attempts";
import { parsePcm16MonoWav } from "../audio/wav";
import { analyzeAudioQuality } from "../audio/quality";
import { runNova } from "../asr/nova";
import { runWhisper } from "../asr/whisper";
import { expectedMorae, normalizeTranscript } from "../japanese/normalize";
import { alignMorae } from "../scoring/align";
import { modelAgreement } from "../scoring/consensus";
import { hasScoreableEvidence, whisperConfidence } from "../scoring/evidence";
import {
  computeReliability,
  makeReRecordEvaluation,
  makeScoredEvaluation,
  makeUncertainEvaluation,
} from "../scoring/score";

export async function handleEvaluate(
  request: Request,
  env: Env,
  config: RuntimeConfig,
  requestId: string,
): Promise<Response> {
  const startedAt = Date.now();
  const claims = config.authRequired
    ? await verifyEvaluationTicket(request.headers.get("Authorization"), env.AUTH_HMAC_SECRET)
    : null;
  const subject = claims?.sub ?? "anonymous-local-user";
  const userHash = await sha256Hex(subject);
  const ip = request.headers.get("CF-Connecting-IP") ?? "unknown";
  if (claims !== null) await enforceRateLimit(env, userHash, ip, claims.sentenceId);

  const form = await parseBoundedFormData(request, config.maxRequestBytes);
  const sentenceId = requiredText(form, "sentenceId", /^[A-Za-z0-9._:-]{1,100}$/u);
  if (claims !== null && claims.sentenceId !== sentenceId) {
    throw new ApiError(403, "ticket_scope_mismatch", "票据不适用于该句子");
  }
  const attemptId = requiredText(
    form,
    "attemptId",
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu,
  );
  const modeValue = requiredText(form, "mode", /^(?:pronunciation|shadowing)$/u);
  const mode: EvaluationMode = modeValue === "shadowing" ? "shadowing" : "pronunciation";
  const audio = form.get("audio");
  if (!(audio instanceof File)) throw new ApiError(400, "bad_request", "缺少 audio 文件");
  if (audio.size > config.maxAudioBytes) throw new ApiError(413, "audio_too_large", "音频超过 1.5 MB 限制");
  const contentType = audio.type.toLowerCase() || "audio/wav";
  if (!new Set(["audio/wav", "audio/wave", "audio/x-wav"]).has(contentType)) {
    throw new ApiError(400, "bad_request", "首版只接受 PCM16 WAV 音频");
  }

  if (claims === null) await enforceRateLimit(env, userHash, ip, sentenceId);

  const cached = await findCachedAttempt(env.DB, attemptId, userHash, sentenceId);
  if (cached.status === "conflict") throw new ApiError(409, "attempt_conflict", "attemptId 已被其他请求使用");
  if (cached.status === "hit") {
    return evaluationResponse(cached.evaluation, cached.evaluation.assessmentStatus === "re_record" ? 422 : 200, true);
  }

  const storedReference = await loadSentenceReference(env, sentenceId);
  const reference = { ...storedReference, scorerVersion: config.scorerVersion };
  const bytes = await audio.arrayBuffer();
  const wav = parsePcm16MonoWav(bytes);
  const quality = analyzeAudioQuality(wav);
  if (!quality.scorable) {
    const evaluation = makeReRecordEvaluation({ attemptId, reference, quality });
    await persistAttempt(env, evaluation, userHash, config.attemptTtlDays, requestId);
    logEvaluation({ requestId, attemptId, sentenceId, startedAt, evaluation });
    return evaluationResponse(evaluation, 422, false);
  }

  const asrMetadata = { attemptId, sentenceId, userHash };
  const nova = await runNova(bytes, contentType, env, asrMetadata);
  const normalizedNova = normalizeTranscript(nova.transcript, nova.words, reference);
  const expected = expectedMorae(reference);
  const novaAlignment = alignMorae(expected, normalizedNova.morae);
  const shouldFallback =
    nova.confidence < config.thresholds.novaConfidence ||
    (novaAlignment.score >= config.thresholds.fallbackAlignmentMin && novaAlignment.score <= config.thresholds.fallbackAlignmentMax) ||
    normalizedNova.unknownRatio > config.thresholds.unknownRatio ||
    normalizedNova.morae.length === 0;

  let selectedTranscription = nova;
  let selectedNormalized = normalizedNova;
  let selectedAlignment = novaAlignment;
  let selectedModel: "primary" | "secondary" = "primary";
  let agreement: number | null = null;
  let fallbackFailed = false;
  if (shouldFallback) {
    try {
      const whisper = await runWhisper(bytes, env, asrMetadata);
      const normalizedWhisper = normalizeTranscript(whisper.transcript, whisper.words, reference);
      const whisperAlignment = alignMorae(expected, normalizedWhisper.morae);
      agreement = modelAgreement(normalizedNova.morae, normalizedWhisper.morae);
      if (whisperAlignment.score > novaAlignment.score) {
        selectedTranscription = {
          transcript: whisper.transcript,
          confidence: whisperConfidence(whisper),
          words: whisper.words,
          ...(whisper.gatewayLogId === undefined ? {} : { gatewayLogId: whisper.gatewayLogId }),
        };
        selectedNormalized = normalizedWhisper;
        selectedAlignment = whisperAlignment;
        selectedModel = "secondary";
      }
    } catch (error) {
      if (!(error instanceof UpstreamAsrError)) throw error;
      fallbackFailed = true;
    }
  }

  const reliability = computeReliability({
    novaConfidence: selectedTranscription.confidence,
    qualityScore: quality.qualityScore,
    unknownRatio: selectedNormalized.unknownRatio,
    moraCount: selectedNormalized.morae.length,
    fallbackUsed: shouldFallback,
    agreement,
    fallbackFailed,
  });
  const mustReturnUncertain = !hasScoreableEvidence(selectedNormalized, selectedAlignment);
  const evaluation = mustReturnUncertain
    ? makeUncertainEvaluation({
        attemptId,
        reference,
        quality,
        novaTranscript: selectedTranscription.transcript,
        recognizedKana: selectedNormalized.kana,
        reliability,
        fallbackUsed: shouldFallback,
        selectedModel,
        agreement,
      })
    : makeScoredEvaluation({
        attemptId,
        reference,
        mode,
        quality,
        nova: selectedTranscription,
        recognizedKana: selectedNormalized.kana,
        alignment: selectedAlignment,
        reliability,
        fallbackUsed: shouldFallback,
        agreement,
        selectedModel,
        lowConfidence: reliability < config.thresholds.reliability,
      });

  await persistAttempt(env, evaluation, userHash, config.attemptTtlDays, requestId);
  logEvaluation({ requestId, attemptId, sentenceId, startedAt, evaluation });
  return evaluationResponse(evaluation, 200, false);
}

function requiredText(form: FormData, name: string, pattern: RegExp): string {
  const value = form.get(name);
  if (typeof value !== "string" || !pattern.test(value)) throw new ApiError(400, "bad_request", `${name} 字段无效`);
  return value;
}

async function enforceRateLimit(env: Env, userHash: string, ip: string, sentenceId: string): Promise<void> {
  const rateLimit = await env.EVALUATION_RATE_LIMITER.limit({
    key: `${userHash.slice(0, 24)}:${ip}:${sentenceId}`,
  });
  if (!rateLimit.success) throw new ApiError(429, "rate_limited", "评测请求过于频繁，请稍后再试");
}

async function persistAttempt(
  env: Env,
  evaluation: PronunciationEvaluation,
  userHash: string,
  ttlDays: number,
  requestId: string,
): Promise<void> {
  try {
    await saveAttempt(env.DB, evaluation, userHash, ttlDays);
  } catch (error) {
    console.warn(JSON.stringify({ message: "attempt_persistence_failed", requestId, error: error instanceof Error ? error.message : String(error) }));
  }
}

function evaluationResponse(evaluation: PronunciationEvaluation, status: number, idempotentReplay: boolean): Response {
  return Response.json(evaluation, {
    status,
    headers: {
      "Cache-Control": "no-store",
      "X-Idempotent-Replay": idempotentReplay ? "true" : "false",
    },
  });
}

function logEvaluation(input: {
  requestId: string;
  attemptId: string;
  sentenceId: string;
  startedAt: number;
  evaluation: PronunciationEvaluation;
}): void {
  console.log(
    JSON.stringify({
      message: "pronunciation_evaluated",
      requestId: input.requestId,
      attemptId: input.attemptId,
      sentenceId: input.sentenceId,
      assessmentStatus: input.evaluation.assessmentStatus,
      fallbackUsed: input.evaluation.engine.fallbackUsed,
      selectedModel: input.evaluation.engine.selected,
      reliability: input.evaluation.engine.reliability,
      agreement: input.evaluation.engine.agreement ?? null,
      overall: input.evaluation.score?.overall ?? null,
      durationMs: Date.now() - input.startedAt,
    }),
  );
}
