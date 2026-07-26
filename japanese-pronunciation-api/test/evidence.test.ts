import { describe, expect, it } from "vitest";
import type { AudioQuality, SentenceReference } from "../src/contracts";
import { expectedMorae, normalizeTranscript } from "../src/japanese/normalize";
import { alignMorae } from "../src/scoring/align";
import { hasScoreableEvidence } from "../src/scoring/evidence";
import { makeScoredEvaluation } from "../src/scoring/score";

const difficultReference: SentenceReference = {
  sentenceId: "re-zero-s01e01-sentence-005",
  text: "ひょっとして 異世界召喚ってやつ～!?",
  displayReading: "ひょっと し て い せかい しょーかん って やつ",
  phoneticReading: "ひょっとしていせかいしょーかんってやつ",
  tokens: [
    { surface: "ひょっと", reading: "ひょっと", morae: ["ひょ", "っ", "と"] },
    { surface: "し", reading: "し", morae: ["し"] },
    { surface: "て", reading: "て", morae: ["て"] },
    { surface: "異", reading: "い", morae: ["い"] },
    { surface: "世界", reading: "せかい", morae: ["せ", "か", "い"] },
    { surface: "召喚", reading: "しょーかん", morae: ["しょ", "ー", "か", "ん"] },
    { surface: "って", reading: "って", morae: ["っ", "て"] },
    { surface: "やつ", reading: "やつ", morae: ["や", "つ"] },
  ],
  lexicon: { 異: "い", 世界: "せかい", 召喚: "しょーかん" },
  native: { durationMs: 1251, speechStartMs: 50, speechEndMs: 1201, pauseBoundaries: [1140], source: "subtitle" },
  scorerVersion: "ja-mora-v1.1.0",
};

describe("scoreable recognition evidence", () => {
  it("keeps a difficult but partially recognized production attempt scoreable", () => {
    const transcript = "ひ よ として 遺 は 雑 和 で やる";
    const normalized = normalizeTranscript(transcript, [], difficultReference);
    const alignment = alignMorae(expectedMorae(difficultReference), normalized.morae);
    expect(hasScoreableEvidence(normalized, alignment)).toBe(true);

    const quality: AudioQuality = {
      status: "ok",
      scorable: true,
      durationMs: 4220,
      speechDurationMs: 2880,
      speechStartMs: 340,
      speechEndMs: 3960,
      speechRatio: 0.682,
      clippingRatio: 0,
      rms: 0.04149,
      peak: 0.45474,
      noiseFloor: 0.00311,
      snrDb: 24.1,
      qualityScore: 100,
      reasons: [],
      speechSegments: [
        { startMs: 340, endMs: 660 },
        { startMs: 800, endMs: 1200 },
        { startMs: 1360, endMs: 1820 },
        { startMs: 2140, endMs: 3540 },
        { startMs: 3660, endMs: 3960 },
      ],
    };
    const evaluation = makeScoredEvaluation({
      attemptId: "11111111-1111-4111-8111-111111111111",
      reference: difficultReference,
      mode: "shadowing",
      quality,
      nova: { transcript, confidence: 0.55, words: [] },
      recognizedKana: normalized.kana,
      alignment,
      reliability: 0.592,
      fallbackUsed: true,
      agreement: 0.35,
      selectedModel: "primary",
      lowConfidence: true,
    });
    expect(evaluation.score?.overall).toBeGreaterThanOrEqual(20);
    expect(evaluation.score?.overall).toBeLessThan(70);
    expect(evaluation.feedback[0]).toContain("仅供练习参考");
  });

  it("still refuses to invent a score from no recognition evidence", () => {
    const normalized = normalizeTranscript("", [], difficultReference);
    const alignment = alignMorae(expectedMorae(difficultReference), normalized.morae);
    expect(hasScoreableEvidence(normalized, alignment)).toBe(false);
  });
});
