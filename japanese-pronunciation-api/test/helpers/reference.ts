import type { SentenceReference } from "../../src/contracts";

export const reference: SentenceReference = {
  sentenceId: "kon-ep01-046",
  text: "正確には廃部寸前ね",
  displayReading: "せいかくにはいぶすんぜんね",
  phoneticReading: "せいかくにわはいぶすんぜんね",
  romaji: "seikaku ni wa haibu sunzen ne",
  tokens: [
    { surface: "正確", reading: "せいかく", morae: ["せ", "い", "か", "く"] },
    { surface: "に", reading: "に", morae: ["に"] },
    { surface: "は", reading: "わ", morae: ["わ"], pos: "particle" },
    { surface: "廃部", reading: "はいぶ", morae: ["は", "い", "ぶ"] },
    { surface: "寸前", reading: "すんぜん", morae: ["す", "ん", "ぜ", "ん"] },
    { surface: "ね", reading: "ね", morae: ["ね"] },
  ],
  lexicon: { 配布: "はいふ", 正確: "せいかく", 廃部: "はいぶ", 寸前: "すんぜん" },
  native: { durationMs: 2860, speechStartMs: 120, speechEndMs: 2740, pauseBoundaries: [1030], source: "native" },
  scorerVersion: "ja-mora-v1.0.0",
};
