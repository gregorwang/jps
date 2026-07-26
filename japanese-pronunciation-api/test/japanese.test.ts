import { describe, expect, it } from "vitest";
import { splitMorae, toHiragana } from "../src/japanese/mora";
import { expectedMorae, normalizeAsrScript, normalizeTranscript } from "../src/japanese/normalize";
import { reference } from "./helpers/reference";

describe("Japanese normalization", () => {
  it("normalizes katakana and groups contracted sounds as one mora", () => {
    expect(toHiragana("キャット")).toBe("きゃっと");
    expect(splitMorae("きゃっと")).toEqual(["きゃ", "っ", "と"]);
  });

  it("maps the reference surface text to phonetic kana including particle は -> わ", () => {
    const normalized = normalizeTranscript(reference.text, [], reference);
    expect(normalized.kana).toBe(reference.phoneticReading);
    expect(normalized.unknownRatio).toBe(0);
  });

  it("keeps a known wrong-word reading for deterministic comparison", () => {
    const normalized = normalizeTranscript("正確には配布寸前ね", [], reference);
    expect(normalized.kana).toContain("はいふ");
    expect(normalized.kana).not.toBe(reference.phoneticReading);
  });

  it("reassembles compounds split across Nova word boundaries", () => {
    const words = ["正確", "に", "は", "配", "布", "寸", "前", "ね"].map((word, index) => ({
      word,
      start: index * 0.2,
      end: (index + 1) * 0.2,
      confidence: 0.9,
    }));
    const normalized = normalizeTranscript("正確 には 配 布 寸 前 ね", words, reference);
    expect(normalized.kana).toBe("せいかくにわはいふすんぜんね");
    expect(normalized.unknownRatio).toBe(0);
  });

  it("builds token-indexed expected morae", () => {
    const morae = expectedMorae(reference);
    expect(morae.map((item) => item.value).join("")).toBe(reference.phoneticReading);
    expect(morae.find((item) => item.value === "ぶ")?.tokenIndex).toBe(3);
  });

  it("converts a Latin-script Japanese ASR result into scoreable kana", () => {
    expect(normalizeAsrScript("yabai korewa honki de yabai")).toBe("やばい これわ ほんき で やばい");
  });

  it("normalizes the mixed-script shape observed in production", () => {
    const reZeroReference = {
      ...reference,
      text: "やばい… これは本気でやばい",
      displayReading: "やばい これ わ ほんき で やばい",
      phoneticReading: "やばいこれわほんきでやばい",
      tokens: [
        { surface: "やばい", reading: "やばい", morae: ["や", "ば", "い"], pos: "adjective" },
        { surface: "これ", reading: "これ", morae: ["こ", "れ"], pos: "noun" },
        { surface: "は", reading: "わ", morae: ["わ"], pos: "particle" },
        { surface: "本気", reading: "ほんき", morae: ["ほ", "ん", "き"], pos: "noun" },
        { surface: "で", reading: "で", morae: ["で"], pos: "particle" },
        { surface: "やばい", reading: "やばい", morae: ["や", "ば", "い"], pos: "adjective" },
      ],
      lexicon: { 本気: "ほんき" },
    };
    const normalized = normalizeTranscript("yabai これは 本 気で yabai", [], reZeroReference);
    expect(normalized.kana).toBe(reZeroReference.phoneticReading);
    expect(normalized.unknownRatio).toBe(0);
  });

  it("keeps Latin phonetic mistakes visible to the mora scorer", () => {
    const normalized = normalizeTranscript("yapai korewa honkite yapai", [], reference);
    expect(normalized.unknownRatio).toBe(0);
    expect(normalized.kana).toContain("やぱい");
  });
});
