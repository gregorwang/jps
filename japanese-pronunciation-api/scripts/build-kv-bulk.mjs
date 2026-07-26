import { mkdir, readdir, readFile, writeFile } from "node:fs/promises";

const dataDirectory = new URL("../data/sentences/", import.meta.url);
const outputDirectory = new URL("../.generated/", import.meta.url);
const outputFile = new URL("sentences-kv.json", outputDirectory);
const filenames = (await readdir(dataDirectory)).filter((name) => name.endsWith(".json")).sort();
if (filenames.length === 0) throw new Error("No sentence JSON files found");

const seen = new Set();
const entries = [];
for (const filename of filenames) {
  const raw = await readFile(new URL(filename, dataDirectory), "utf8");
  const parsed = JSON.parse(raw);
  if (!parsed || typeof parsed !== "object" || typeof parsed.sentenceId !== "string") {
    throw new Error(`${filename}: sentenceId is required`);
  }
  if (!Array.isArray(parsed.tokens) || parsed.tokens.length === 0) {
    throw new Error(`${filename}: tokens must be a non-empty array`);
  }
  if (seen.has(parsed.sentenceId)) throw new Error(`Duplicate sentenceId: ${parsed.sentenceId}`);
  seen.add(parsed.sentenceId);
  entries.push({ key: `sentence:${parsed.sentenceId}`, value: JSON.stringify(parsed) });
}

await mkdir(outputDirectory, { recursive: true });
await writeFile(outputFile, `${JSON.stringify(entries, null, 2)}\n`, "utf8");
console.log(`Prepared ${entries.length} sentence reference(s) in .generated/sentences-kv.json`);
