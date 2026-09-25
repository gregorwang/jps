import { createHash } from "node:crypto";
import { readdir, readFile } from "node:fs/promises";
import { basename, join } from "node:path";

const regenDir = new URL("./", import.meta.url);
const targetNames = [
  "batch32_exercises_letter.json",
  "batch33_exercises_letter.json",
  "batch34_exercises_letter.json",
];
const exactKeys = ["id", "prompt", "answer", "hint", "review_note"];
const normalize = (value) =>
  value
    .normalize("NFKC")
    .replace(/[\s\p{P}\p{S}]+/gu, "")
    .toLowerCase();
const sha256 = (value) =>
  createHash("sha256").update(value).digest("hex").toUpperCase();
const md5 = (value) => createHash("md5").update(value).digest("hex");

const targetFiles = [];
for (const name of targetNames) {
  const raw = await readFile(new URL(name, regenDir), "utf8");
  targetFiles.push({ name, raw, rows: JSON.parse(raw) });
}
const rows = targetFiles.flatMap((file) => file.rows);
const targetIdSet = new Set(rows.map((row) => row.id));
const errors = [];
const warnings = [];

for (const [index, row] of rows.entries()) {
  const keys = Object.keys(row).sort();
  if (JSON.stringify(keys) !== JSON.stringify([...exactKeys].sort())) {
    errors.push(`row ${index + 1}: schema ${keys.join(",")}`);
  }
  for (const key of exactKeys) {
    if (typeof row[key] !== "string" || row[key].trim() === "") {
      errors.push(`row ${index + 1}: empty/non-string ${key}`);
    }
  }
}
if (rows.length !== 120) errors.push(`row count ${rows.length}`);
if (targetIdSet.size !== rows.length) errors.push("duplicate target ID");

const expectedIds = [
  ...Array.from(
    { length: 102 - 63 + 1 },
    (_, index) => `re-zero-s01e21-exercise-${String(63 + index).padStart(3, "0")}`,
  ),
  ...Array.from(
    { length: 142 - 103 + 1 },
    (_, index) => `re-zero-s01e21-exercise-${String(103 + index).padStart(3, "0")}`,
  ),
  ...[143, 144, ...Array.from({ length: 164 - 145 + 1 }, (_, i) => 145 + i),
    ...Array.from({ length: 183 - 166 + 1 }, (_, i) => 166 + i)]
    .map((number) => `re-zero-s01e21-exercise-${String(number).padStart(3, "0")}`),
];
const actualIds = rows.map((row) => row.id);
if (JSON.stringify(actualIds) !== JSON.stringify(expectedIds)) {
  errors.push("target ID order differs from frozen selection");
}

const learnerFields = ["prompt", "answer", "hint"];
const forbidden = /(?:数据库|manifest|模板|源题|选项答案|内部\s*ID|learning_exercises|review_note)/iu;
const zeroWidth = /[\u200B-\u200D\u2060\uFEFF]/u;
for (const row of rows) {
  if (/^[A-D]$/u.test(row.answer.trim())) {
    errors.push(`${row.id}: single-letter answer remains`);
  }
  for (const field of learnerFields) {
    if (forbidden.test(row[field])) {
      errors.push(`${row.id}: internal/meta leakage in ${field}`);
    }
    if (zeroWidth.test(row[field])) {
      errors.push(`${row.id}: zero-width/BOM in ${field}`);
    }
  }
}

const duplicateCounts = {};
for (const field of learnerFields) {
  const seen = new Map();
  for (const row of rows) {
    const value = normalize(row[field]);
    seen.set(value, (seen.get(value) ?? 0) + 1);
  }
  duplicateCounts[field] = [...seen.values()].filter((count) => count > 1).length;
  if (duplicateCounts[field] > 0) {
    errors.push(`normalized duplicate ${field}: ${duplicateCounts[field]}`);
  }
}

const allNames = (await readdir(regenDir))
  .filter((name) => name.endsWith(".json") && !targetNames.includes(name));
const otherRows = [];
for (const name of allNames) {
  try {
    const parsed = JSON.parse(await readFile(new URL(name, regenDir), "utf8"));
    if (Array.isArray(parsed)) {
      for (const row of parsed) {
        if (row && typeof row === "object" && typeof row.id === "string") {
          otherRows.push({ ...row, __file: name });
        }
      }
    }
  } catch {
    warnings.push(`could not parse ${name}`);
  }
}
const overlappingIds = otherRows.filter((row) => targetIdSet.has(row.id));
if (overlappingIds.length > 0) {
  errors.push(`ID overlap with other JSON candidates: ${overlappingIds.length}`);
}
const externalExactDuplicates = {};
for (const field of learnerFields) {
  const otherValues = new Set(
    otherRows
      .filter((row) => typeof row[field] === "string")
      .map((row) => normalize(row[field])),
  );
  externalExactDuplicates[field] = rows.filter((row) =>
    otherValues.has(normalize(row[field])),
  ).length;
  if (externalExactDuplicates[field] > 0) {
    errors.push(
      `normalized ${field} duplicates outside target: ${externalExactDuplicates[field]}`,
    );
  }
}

let hintsContainingAnswer = 0;
let maxCommonRun = 0;
let commonRunAtLeast8 = 0;
const longestCommonSubstring = (left, right) => {
  const previous = new Uint16Array(right.length + 1);
  let best = 0;
  for (let i = 1; i <= left.length; i += 1) {
    let diagonal = 0;
    for (let j = 1; j <= right.length; j += 1) {
      const saved = previous[j];
      previous[j] = left[i - 1] === right[j - 1] ? diagonal + 1 : 0;
      if (previous[j] > best) best = previous[j];
      diagonal = saved;
    }
  }
  return best;
};
for (const row of rows) {
  const answer = normalize(row.answer);
  const hint = normalize(row.hint);
  if (answer.length > 0 && hint.includes(answer)) hintsContainingAnswer += 1;
  const commonRun = longestCommonSubstring(answer, hint);
  maxCommonRun = Math.max(maxCommonRun, commonRun);
  if (commonRun >= 8) commonRunAtLeast8 += 1;
}

const result = {
  files: targetFiles.map((file) => ({
    name: file.name,
    rows: file.rows.length,
    sha256: sha256(file.raw),
  })),
  rows: rows.length,
  uniqueIds: targetIdSet.size,
  orderedCommaMd5: md5(actualIds.join(",")),
  duplicateCounts,
  otherJsonFilesScanned: allNames.length,
  otherRowsScanned: otherRows.length,
  overlappingIds: overlappingIds.length,
  externalExactDuplicates,
  hintsContainingAnswer,
  answerHintLongestNormalizedCommonRun: maxCommonRun,
  answerHintCommonRunAtLeast8: commonRunAtLeast8,
  errors,
  warnings,
};
console.log(JSON.stringify(result, null, 2));
