import { readFile } from "node:fs/promises";
import { describe, expect, it } from "vitest";
import { splitMorae, toHiragana } from "../src/japanese/mora";
import { parseSentenceReference } from "../src/storage/sentences";

describe("generated course references", () => {
  it("parses every generated KV value and keeps its mora sequence consistent", async () => {
    const raw = await readFile(new URL("../.generated/sentences-kv.json", import.meta.url), "utf8");
    const entries = JSON.parse(raw) as Array<{ key: string; value: string }>;
    expect(entries.length).toBeGreaterThan(0);

    for (const entry of entries) {
      const reference = parseSentenceReference(JSON.parse(entry.value));
      expect(reference, entry.key).not.toBeNull();
      if (reference === null) continue;
      expect(entry.key).toBe(`sentence:${reference.sentenceId}`);
      const expected = reference.tokens.flatMap((token) => token.morae).map(toHiragana).join("");
      expect(expected, reference.sentenceId).toBe(splitMorae(reference.phoneticReading).join(""));
    }
  });
});
