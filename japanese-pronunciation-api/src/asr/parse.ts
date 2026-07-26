import type { AsrWord, NovaTranscription, WhisperTranscription } from "../contracts";
import { isRecord } from "../contracts";
import { clamp, mean } from "../utils/math";
import { UpstreamAsrError } from "../errors";

export function parseNovaResponse(value: unknown, gatewayLogId?: string): NovaTranscription {
  if (!isRecord(value) || !isRecord(value.results) || !Array.isArray(value.results.channels)) {
    throw new UpstreamAsrError("Nova-3 返回了无法解析的结果");
  }
  const firstChannel = value.results.channels[0];
  if (!isRecord(firstChannel) || !Array.isArray(firstChannel.alternatives)) {
    return { transcript: "", confidence: 0, words: [], ...(gatewayLogId ? { gatewayLogId } : {}) };
  }
  const alternative = firstChannel.alternatives[0];
  if (!isRecord(alternative)) {
    return { transcript: "", confidence: 0, words: [], ...(gatewayLogId ? { gatewayLogId } : {}) };
  }
  const words = parseNovaWords(alternative.words);
  const alternativeConfidence = finiteNumber(alternative.confidence);
  return {
    transcript: typeof alternative.transcript === "string" ? alternative.transcript.trim() : "",
    confidence: clamp(words.length > 0 ? mean(words.map((word) => word.confidence)) : (alternativeConfidence ?? 0), 0, 1),
    words,
    ...(gatewayLogId ? { gatewayLogId } : {}),
  };
}

export function parseWhisperResponse(value: unknown, gatewayLogId?: string): WhisperTranscription {
  if (!isRecord(value) || typeof value.text !== "string") {
    throw new UpstreamAsrError("Whisper 返回了无法解析的结果");
  }
  const segments = Array.isArray(value.segments) ? value.segments.filter(isRecord) : [];
  const logProbabilities = segments.flatMap((segment) => {
    const number = finiteNumber(segment.avg_logprob);
    return number === null ? [] : [number];
  });
  const noSpeechProbabilities = segments.flatMap((segment) => {
    const number = finiteNumber(segment.no_speech_prob);
    return number === null ? [] : [number];
  });
  const words: AsrWord[] = [];
  for (const segment of segments) {
    const segmentLogProbability = finiteNumber(segment.avg_logprob) ?? -0.4;
    const confidence = clamp(Math.exp(segmentLogProbability), 0, 1);
    if (Array.isArray(segment.words)) {
      for (const rawWord of segment.words) {
        if (!isRecord(rawWord) || typeof rawWord.word !== "string") continue;
        const start = finiteNumber(rawWord.start);
        const end = finiteNumber(rawWord.end);
        if (start === null || end === null || start < 0 || end < start) continue;
        words.push({ word: rawWord.word.trim(), start, end, confidence });
      }
    }
  }
  return {
    transcript: value.text.trim(),
    words,
    meanLogProbability: logProbabilities.length > 0 ? mean(logProbabilities) : null,
    noSpeechProbability: noSpeechProbabilities.length > 0 ? mean(noSpeechProbabilities) : null,
    ...(gatewayLogId ? { gatewayLogId } : {}),
  };
}

function parseNovaWords(value: unknown): AsrWord[] {
  if (!Array.isArray(value)) return [];
  const words: AsrWord[] = [];
  for (const rawWord of value) {
    if (!isRecord(rawWord) || typeof rawWord.word !== "string") continue;
    const start = finiteNumber(rawWord.start);
    const end = finiteNumber(rawWord.end);
    const confidence = finiteNumber(rawWord.confidence);
    if (start === null || end === null || confidence === null || start < 0 || end < start) continue;
    words.push({ word: rawWord.word.trim(), start, end, confidence: clamp(confidence, 0, 1) });
  }
  return words;
}

function finiteNumber(value: unknown): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}
