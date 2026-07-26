import type { AudioQuality } from "../contracts";
import type { ParsedWav } from "./wav";
import { clamp, percentile, round } from "../utils/math";

const FRAME_MS = 20;
const MIN_DURATION_MS = 400;
const MAX_DURATION_MS = 15_000;
const MIN_SPEECH_MS = 360;

export function analyzeAudioQuality(wav: ParsedWav): AudioQuality {
  const samplesPerFrame = Math.round((wav.sampleRate * FRAME_MS) / 1000);
  const frameEnergies: number[] = [];
  let sumSquares = 0;
  let peak = 0;
  let clipped = 0;

  for (let frameStart = 0; frameStart < wav.samples.length; frameStart += samplesPerFrame) {
    const frameEnd = Math.min(wav.samples.length, frameStart + samplesPerFrame);
    let frameSquares = 0;
    for (let index = frameStart; index < frameEnd; index += 1) {
      const sample = (wav.samples[index] ?? 0) / 32_768;
      const absolute = Math.abs(sample);
      sumSquares += sample * sample;
      frameSquares += sample * sample;
      peak = Math.max(peak, absolute);
      if (absolute >= 0.995) clipped += 1;
    }
    frameEnergies.push(Math.sqrt(frameSquares / Math.max(1, frameEnd - frameStart)));
  }

  const rms = Math.sqrt(sumSquares / Math.max(1, wav.samples.length));
  const clippingRatio = clipped / Math.max(1, wav.samples.length);
  const noiseFloor = Math.max(0.0001, percentile(frameEnergies, 0.2));
  const speechThreshold = Math.max(0.012, Math.min(0.08, noiseFloor * 2.8));
  const speechMask = frameEnergies.map((energy) => energy >= speechThreshold);
  bridgeShortGaps(speechMask, 5);
  removeShortRuns(speechMask, 3);

  const speechFrameIndexes = speechMask.flatMap((isSpeech, index) => (isSpeech ? [index] : []));
  const speechSegments = maskToSegments(speechMask, FRAME_MS, wav.durationMs);
  const speechStartMs = speechFrameIndexes.length > 0 ? (speechFrameIndexes[0] ?? 0) * FRAME_MS : null;
  const lastSpeechFrame = speechFrameIndexes.at(-1);
  const speechEndMs = lastSpeechFrame === undefined ? null : Math.min(wav.durationMs, (lastSpeechFrame + 1) * FRAME_MS);
  const speechDurationMs = speechFrameIndexes.length * FRAME_MS;
  const speechRatio = speechDurationMs / Math.max(1, wav.durationMs);
  const speechEnergies = frameEnergies.filter((_, index) => speechMask[index]);
  const speechRms = speechEnergies.length > 0 ? Math.sqrt(speechEnergies.reduce((sum, value) => sum + value * value, 0) / speechEnergies.length) : 0;
  const snrDb = speechRms > 0 ? 20 * Math.log10(speechRms / noiseFloor) : null;

  const reasons: string[] = [];
  let status: AudioQuality["status"] = "ok";
  if (wav.durationMs < MIN_DURATION_MS) {
    status = "too_short";
    reasons.push("录音短于 0.4 秒");
  } else if (wav.durationMs > MAX_DURATION_MS) {
    status = "too_long";
    reasons.push("录音长于 15 秒");
  } else if (rms < 0.003 || speechDurationMs === 0) {
    status = "silent";
    reasons.push("没有检测到可评分语音");
  } else if (rms < 0.008 || speechDurationMs < MIN_SPEECH_MS) {
    status = "too_quiet";
    reasons.push("有效语音过短或音量过低");
  } else if (clippingRatio > 0.08) {
    status = "clipped";
    reasons.push("录音削波过多");
  } else if (snrDb !== null && snrDb < 4 && noiseFloor > 0.02) {
    status = "noisy";
    reasons.push("背景噪声过高");
  }

  const qualityScore = clamp(
    100 - clippingRatio * 280 - Math.max(0, 0.012 - rms) * 1200 - Math.max(0, 8 - (snrDb ?? 0)) * 2.5,
    0,
    100,
  );
  return {
    status,
    scorable: status === "ok",
    durationMs: round(wav.durationMs),
    speechDurationMs: round(speechDurationMs),
    speechStartMs: speechStartMs === null ? null : round(speechStartMs),
    speechEndMs: speechEndMs === null ? null : round(speechEndMs),
    speechRatio: round(speechRatio, 3),
    clippingRatio: round(clippingRatio, 5),
    rms: round(rms, 5),
    peak: round(peak, 5),
    noiseFloor: round(noiseFloor, 5),
    snrDb: snrDb === null ? null : round(snrDb, 1),
    qualityScore: round(qualityScore),
    reasons,
    speechSegments,
  };
}

function bridgeShortGaps(mask: boolean[], maxGapFrames: number): void {
  let index = 0;
  while (index < mask.length) {
    if (mask[index]) {
      index += 1;
      continue;
    }
    const start = index;
    while (index < mask.length && !mask[index]) index += 1;
    const boundedBySpeech = start > 0 && index < mask.length && mask[start - 1] === true && mask[index] === true;
    if (boundedBySpeech && index - start <= maxGapFrames) {
      for (let fill = start; fill < index; fill += 1) mask[fill] = true;
    }
  }
}

function removeShortRuns(mask: boolean[], minRunFrames: number): void {
  let index = 0;
  while (index < mask.length) {
    if (!mask[index]) {
      index += 1;
      continue;
    }
    const start = index;
    while (index < mask.length && mask[index]) index += 1;
    if (index - start < minRunFrames) {
      for (let clear = start; clear < index; clear += 1) mask[clear] = false;
    }
  }
}

function maskToSegments(mask: readonly boolean[], frameMs: number, durationMs: number): Array<{ startMs: number; endMs: number }> {
  const segments: Array<{ startMs: number; endMs: number }> = [];
  let index = 0;
  while (index < mask.length) {
    if (!mask[index]) {
      index += 1;
      continue;
    }
    const start = index;
    while (index < mask.length && mask[index]) index += 1;
    segments.push({ startMs: start * frameMs, endMs: Math.min(durationMs, index * frameMs) });
  }
  return segments;
}
