import { describe, expect, it } from "vitest";
import type { AudioQuality, NovaTranscription } from "../src/contracts";
import { expectedMorae, normalizeTranscript } from "../src/japanese/normalize";
import { alignMorae } from "../src/scoring/align";
import { makeScoredEvaluation } from "../src/scoring/score";
import { reference } from "./helpers/reference";

describe("score response invariants", () => {
  it("clamps model timestamps to the uploaded audio duration", () => {
    const words = ["正確", "に", "は", "廃部", "寸前", "ね"].map((word, index) => ({
      word,
      start: index * 0.8,
      end: (index + 1) * 0.8,
      confidence: 0.95,
    }));
    const normalized = normalizeTranscript(reference.text, words, reference);
    const nova: NovaTranscription = { transcript: reference.text, confidence: 0.95, words };
    const quality: AudioQuality = {
      status: "ok",
      scorable: true,
      durationMs: 3024,
      speechDurationMs: 2400,
      speechStartMs: 200,
      speechEndMs: 2600,
      speechRatio: 0.79,
      clippingRatio: 0,
      rms: 0.12,
      peak: 0.6,
      noiseFloor: 0.002,
      snrDb: 35,
      qualityScore: 98,
      reasons: [],
      speechSegments: [{ startMs: 200, endMs: 2600 }],
    };
    const evaluation = makeScoredEvaluation({
      attemptId: "11111111-1111-4111-8111-111111111111",
      reference,
      mode: "pronunciation",
      quality,
      nova,
      recognizedKana: normalized.kana,
      alignment: alignMorae(expectedMorae(reference), normalized.morae),
      reliability: 0.9,
      fallbackUsed: false,
      agreement: null,
      selectedModel: "primary",
      lowConfidence: false,
    });
    expect(evaluation.segments.every((segment) => (segment.endMs ?? 0) <= quality.durationMs)).toBe(true);
    expect(evaluation.reference.timingSource).toBe("native");
  });
});
