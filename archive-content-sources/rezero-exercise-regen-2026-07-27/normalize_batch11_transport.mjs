import { readFile, writeFile } from "node:fs/promises";

const readJsonWithTrailingCommaCleanup = async (path) => {
  const raw = await readFile(path, "utf8");
  return JSON.parse(raw.replace(/,\s*([}\]])/g, "$1"));
};

const vocabPath = "archive-content-sources/rezero-exercise-regen-2026-07-27/batch11_vocab_linguistic.json";
const grammarPath = "archive-content-sources/rezero-exercise-regen-2026-07-27/batch11_grammar_sentence_linguistic.json";
const sentencePath = "archive-content-sources/rezero-exercise-regen-2026-07-27/batch11_sentence_linguistic_overlay.json";

const vocab = await readJsonWithTrailingCommaCleanup(vocabPath);
const grammar = await readJsonWithTrailingCommaCleanup(grammarPath);
const sentencePart1 = await readJsonWithTrailingCommaCleanup(
  "archive-content-sources/rezero-exercise-regen-2026-07-27/batch11_sentence_part1.overlay.json",
);
const sentencePart2 = await readJsonWithTrailingCommaCleanup(
  "archive-content-sources/rezero-exercise-regen-2026-07-27/batch11_sentence_part2.overlay.json",
);
const sentences = [...sentencePart1, ...sentencePart2];

if (vocab.length !== 40 || grammar.length !== 20 || sentences.length !== 20) {
  throw new Error(
    `Unexpected Batch11 counts: vocab=${vocab.length}, grammar=${grammar.length}, sentence=${sentences.length}`,
  );
}

await writeFile(vocabPath, `${JSON.stringify(vocab, null, 2)}\n`, "utf8");
await writeFile(grammarPath, `${JSON.stringify(grammar, null, 2)}\n`, "utf8");
await writeFile(sentencePath, `${JSON.stringify(sentences, null, 2)}\n`, "utf8");

console.log(
  JSON.stringify({
    vocab: vocab.length,
    grammar: grammar.length,
    sentence: sentences.length,
  }),
);
