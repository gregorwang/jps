import { describe, expect, it } from "vitest";
import { expectedMorae, normalizeTranscript } from "../src/japanese/normalize";
import { alignMorae } from "../src/scoring/align";
import { reference } from "./helpers/reference";

describe("weighted mora alignment", () => {
  it("scores an exact reading at 100", () => {
    const heard = normalizeTranscript(reference.text, [], reference);
    const alignment = alignMorae(expectedMorae(reference), heard.morae);
    expect(alignment.score).toBe(100);
    expect(alignment.substitutions).toBe(0);
    expect(alignment.deletions).toBe(0);
  });

  it("localizes ぶ -> ふ to the 廃部 token", () => {
    const heard = normalizeTranscript("せいかくにわはいふすんぜんね", [], reference);
    const alignment = alignMorae(expectedMorae(reference), heard.morae);
    const substitution = alignment.operations.find((operation) => operation.type === "substitution");
    expect(substitution?.expected?.value).toBe("ぶ");
    expect(substitution?.heard?.value).toBe("ふ");
    expect(substitution?.expected?.tokenIndex).toBe(3);
  });

  it("detects omissions separately from substitutions", () => {
    const heard = normalizeTranscript("せいかくにわはいぶね", [], reference);
    const alignment = alignMorae(expectedMorae(reference), heard.morae);
    expect(alignment.deletions).toBeGreaterThanOrEqual(4);
    expect(alignment.score).toBeLessThan(100);
  });
});
