import { ApiError } from "./errors";

export interface RuntimeConfig {
  maxAudioBytes: number;
  maxRequestBytes: number;
  attemptTtlDays: number;
  scorerVersion: string;
  authRequired: boolean;
  allowedOrigins: Set<string>;
  thresholds: {
    novaConfidence: number;
    fallbackAlignmentMin: number;
    fallbackAlignmentMax: number;
    unknownRatio: number;
    reliability: number;
  };
}

export function loadConfig(env: Env): RuntimeConfig {
  return {
    maxAudioBytes: integerSetting(env.MAX_AUDIO_BYTES, "MAX_AUDIO_BYTES", 1, 5_000_000),
    maxRequestBytes: integerSetting(env.MAX_REQUEST_BYTES, "MAX_REQUEST_BYTES", 1, 6_000_000),
    attemptTtlDays: integerSetting(env.ATTEMPT_TTL_DAYS, "ATTEMPT_TTL_DAYS", 1, 30),
    scorerVersion: env.SCORER_VERSION,
    authRequired: env.AUTH_REQUIRED === "true",
    allowedOrigins: new Set(
      env.ALLOWED_ORIGINS.split(",")
        .map((origin) => origin.trim())
        .filter(Boolean),
    ),
    thresholds: {
      novaConfidence: numberSetting(env.NOVA_CONFIDENCE_THRESHOLD, "NOVA_CONFIDENCE_THRESHOLD", 0, 1),
      fallbackAlignmentMin: numberSetting(env.FALLBACK_ALIGNMENT_MIN, "FALLBACK_ALIGNMENT_MIN", 0, 100),
      fallbackAlignmentMax: numberSetting(env.FALLBACK_ALIGNMENT_MAX, "FALLBACK_ALIGNMENT_MAX", 0, 100),
      unknownRatio: numberSetting(env.UNKNOWN_RATIO_THRESHOLD, "UNKNOWN_RATIO_THRESHOLD", 0, 1),
      reliability: numberSetting(env.RELIABILITY_THRESHOLD, "RELIABILITY_THRESHOLD", 0, 1),
    },
  };
}

function integerSetting(value: string, name: string, min: number, max: number): number {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < min || parsed > max) {
    throw new ApiError(500, "invalid_server_configuration", `${name} 配置无效`);
  }
  return parsed;
}

function numberSetting(value: string, name: string, min: number, max: number): number {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed < min || parsed > max) {
    throw new ApiError(500, "invalid_server_configuration", `${name} 配置无效`);
  }
  return parsed;
}
