import crypto from "node:crypto";
import fs from "node:fs";

const configs = {
  meta9: {
    kind: "exercise",
    expectedRows: 80,
    expectedBaseFileRows: [80],
    chunkSize: 20,
    baseFiles: ["archive-content-sources/rezero-exercise-regen-2026-07-27/batch9_meta_exercises.json"],
    generationOverlay: "archive-content-sources/rezero-exercise-regen-2026-07-27/generation_overlay_meta_exercises_batch9.json",
    reviewOverlay: "archive-content-sources/rezero-exercise-regen-2026-07-27/review_overlay_meta_exercises_batch9.json",
    reviewReport: "archive-content-sources/rezero-exercise-regen-2026-07-27/cross_review_meta_exercises_batch9.md",
  },
  meta10: {
    kind: "exercise",
    expectedRows: 42,
    expectedBaseFileRows: [42],
    chunkSize: 21,
    baseFiles: ["archive-content-sources/rezero-exercise-regen-2026-07-27/batch10_meta_exercises.json"],
    generationOverlay: "archive-content-sources/rezero-exercise-regen-2026-07-27/generation_overlay_meta_exercises_batch10.json",
    reviewOverlay: "archive-content-sources/rezero-exercise-regen-2026-07-27/review_overlay_meta_exercises_batch10.json",
    reviewReport: "archive-content-sources/rezero-exercise-regen-2026-07-27/cross_review_meta_exercises_batch10.md",
  },
  linguistic11: {
    kind: "linguistic",
    expectedRows: 80,
    expectedBaseFileRows: [40, 20, 20],
    chunkSize: 10,
    baseFiles: [
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch11_vocab_linguistic.json",
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch11_grammar_sentence_linguistic.json",
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch11_sentence_linguistic_overlay.json",
    ],
    reviewOverlay: "archive-content-sources/rezero-exercise-regen-2026-07-27/review_overlay_linguistic_batch11.json",
    reviewReport: "archive-content-sources/rezero-exercise-regen-2026-07-27/cross_review_linguistic_batch11.md",
  },
  linguistic12: {
    kind: "linguistic",
    expectedRows: 80,
    expectedBaseFileRows: [40, 20, 20],
    chunkSize: 10,
    baseFiles: [
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch12_vocab_linguistic.json",
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch12_grammar_linguistic.json",
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch12_sentence_linguistic.json",
    ],
    reviewOverlay: "archive-content-sources/rezero-exercise-regen-2026-07-27/review_overlay_linguistic_batch12.json",
    reviewReport: "archive-content-sources/rezero-exercise-regen-2026-07-27/cross_review_linguistic_batch12.md",
  },
  letter35_37: {
    kind: "exercise",
    expectedRows: 120,
    expectedBaseFileRows: [40, 40, 40],
    chunkSize: 40,
    baseFiles: [
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch35_exercises_letter.json",
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch36_exercises_letter.json",
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch37_exercises_letter.json",
    ],
    generationOverlay: "archive-content-sources/rezero-exercise-regen-2026-07-27/generation_overlay_exercises_letter_batches35_37.json",
    reviewOverlay: "archive-content-sources/rezero-exercise-regen-2026-07-27/review_overlay_exercises_letter_batches35_37.json",
    reviewReport: "archive-content-sources/rezero-exercise-regen-2026-07-27/cross_review_exercises_letter_batches35_37.md",
  },
  letter38_40: {
    kind: "exercise",
    expectedRows: 120,
    expectedBaseFileRows: [40, 40, 40],
    chunkSize: 40,
    baseFiles: [
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch38_exercises_letter.json",
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch39_exercises_letter.json",
      "archive-content-sources/rezero-exercise-regen-2026-07-27/batch40_exercises_letter.json",
    ],
    generationOverlay: "archive-content-sources/rezero-exercise-regen-2026-07-27/generation_overlay_exercises_letter_batches38_40.json",
    reviewOverlay: "archive-content-sources/rezero-exercise-regen-2026-07-27/review_overlay_exercises_letter_batches38_40.json",
    reviewReport: "archive-content-sources/rezero-exercise-regen-2026-07-27/cross_review_exercises_letter_batches38_40.md",
  },
};

function fail(message) {
  throw new Error(message);
}

function readArray(file) {
  if (!fs.existsSync(file)) fail(`missing required file: ${file}`);
  const value = JSON.parse(fs.readFileSync(file, "utf8"));
  if (!Array.isArray(value)) fail(`${file}: top level must be an array`);
  return value;
}

function assertUniqueIds(rows, label) {
  const ids = rows.map((row) => row?.id);
  if (ids.some((id) => typeof id !== "string" || id.length === 0)) {
    fail(`${label}: every row must have a non-empty string id`);
  }
  const unique = new Set(ids);
  if (unique.size !== rows.length) {
    fail(`${label}: expected unique IDs, got ${rows.length}/${unique.size}`);
  }
}

function assertSchema(rows, kind, label) {
  const expected =
    kind === "exercise"
      ? ["answer", "hint", "id", "prompt", "review_note"]
      : ["id", "linguistic_payload", "review_note", "source_id", "source_type"];
  const expectedKey = JSON.stringify(expected);
  for (const row of rows) {
    const actual = JSON.stringify(Object.keys(row).sort());
    if (actual !== expectedKey) {
      fail(`${label}:${row.id}: schema ${actual} does not match ${expectedKey}`);
    }
    if (kind === "exercise") {
      for (const field of ["prompt", "answer", "hint", "review_note"]) {
        if (typeof row[field] !== "string" || row[field].trim().length === 0) {
          fail(`${label}:${row.id}: ${field} must be a non-empty string`);
        }
      }
      if (/^[A-D]$/iu.test(row.answer.trim()) || row.answer.startsWith("用于")) {
        fail(`${label}:${row.id}: reviewed answer is still a placeholder`);
      }
    } else {
      for (const field of ["source_id", "review_note"]) {
        if (typeof row[field] !== "string" || row[field].trim().length === 0) {
          fail(`${label}:${row.id}: ${field} must be a non-empty string`);
        }
      }
      if (
        !row.linguistic_payload ||
        typeof row.linguistic_payload !== "object" ||
        Array.isArray(row.linguistic_payload)
      ) {
        fail(`${label}:${row.id}: linguistic_payload must be a plain object`);
      }
      if (!["vocab", "grammar", "sentence"].includes(row.source_type)) {
        fail(`${label}:${row.id}: unexpected source_type ${row.source_type}`);
      }
    }
  }
}

function applyOverlay(effective, overlayFile, kind, label) {
  if (!overlayFile) return { rows: effective, replacements: 0, overlayRows: 0 };
  const overlay = readArray(overlayFile);
  assertUniqueIds(overlay, label);
  assertSchema(overlay, kind, label);
  const byId = new Map(overlay.map((row) => [row.id, row]));
  const baseById = new Map(effective.map((row) => [row.id, row]));
  for (const id of byId.keys()) {
    if (!baseById.has(id)) fail(`${label}: overlay ID is absent from base: ${id}`);
  }
  if (kind === "linguistic") {
    for (const row of overlay) {
      const base = baseById.get(row.id);
      for (const field of ["source_type", "source_id"]) {
        if (row[field] !== base[field]) {
          fail(`${label}:${row.id}: overlay must not change immutable ${field}`);
        }
      }
    }
  }
  let replacements = 0;
  const rows = effective.map((row) => {
    const replacement = byId.get(row.id);
    if (!replacement) return row;
    replacements += 1;
    return { ...replacement, __source_file: row.__source_file };
  });
  if (replacements !== overlay.length) {
    fail(`${label}: replacement count ${replacements} != overlay rows ${overlay.length}`);
  }
  return { rows, replacements, overlayRows: overlay.length };
}

function assertReviewCoverage(rows, kind, reportFile) {
  const report = fs.readFileSync(reportFile, "utf8");
  if (report.trim().length === 0) fail(`${reportFile}: review report is empty`);
  const markers = rows.map((row) => (kind === "exercise" ? row.id : row.source_id));
  if (new Set(markers).size !== rows.length) {
    fail(`${reportFile}: review coverage markers must be unique per candidate`);
  }
  const missing = markers.filter((marker) => !report.includes(marker));
  if (missing.length > 0) {
    fail(
      `${reportFile}: independent review report is missing ${missing.length} candidate markers; first missing: ${missing.slice(0, 5).join(", ")}`,
    );
  }
  return {
    markers: markers.length,
    sha256: crypto.createHash("sha256").update(report).digest("hex").toUpperCase(),
  };
}

const [group, mode = "--summary", modeValue] = process.argv.slice(2);
const config = configs[group];
if (!config) {
  fail(`usage: node archive-content-sources/rezero-exercise-regen-2026-07-27/prepare_reviewed_transport.mjs <${Object.keys(configs).join("|")}> [--summary|--chunk N|--write-effective PATH]`);
}
if (!fs.existsSync(config.reviewReport)) fail(`missing review report: ${config.reviewReport}`);

const baseRows = config.baseFiles.flatMap((file, index) => {
  const rows = readArray(file);
  const expectedFileRows = config.expectedBaseFileRows[index];
  if (rows.length !== expectedFileRows) {
    fail(`${group}:${file}: expected ${expectedFileRows} rows, got ${rows.length}`);
  }
  return rows.map((row) => ({
    ...row,
    __source_file: file.replaceAll("\\", "/"),
  }));
});
assertUniqueIds(baseRows, `${group}:base`);
assertSchema(
  baseRows.map(({ __source_file, ...row }) => row),
  config.kind,
  `${group}:base`,
);
if (baseRows.length !== config.expectedRows) {
  fail(`${group}: expected ${config.expectedRows} base rows, got ${baseRows.length}`);
}

let effective = baseRows;
const generation = applyOverlay(
  effective,
  config.generationOverlay,
  config.kind,
  `${group}:generation-overlay`,
);
effective = generation.rows;
const review = applyOverlay(
  effective,
  config.reviewOverlay,
  config.kind,
  `${group}:review-overlay`,
);
effective = review.rows;

assertUniqueIds(effective, `${group}:effective`);
if (effective.length !== config.expectedRows) {
  fail(`${group}: expected ${config.expectedRows} effective rows, got ${effective.length}`);
}

if (group === "meta9") {
  const row = effective.find((candidate) => candidate.id === "re-zero-ex-ep043-177");
  if (!row || !/碧翠丝.*掩饰|掩饰.*碧翠丝/u.test(row.prompt)) {
    fail("meta9: re-zero-ex-ep043-177 must explicitly identify 碧翠丝 as the one concealing her motive");
  }
}

if (group === "linguistic11") {
  const row = effective.find(
    (candidate) => candidate.source_id === "re-zero-s01e06-grammar-007",
  );
  const serialized = JSON.stringify(row?.linguistic_payload ?? {});
  if (
    !serialized.includes("下手をすると") ||
    !/(省略|词汇化)/u.test(serialized) ||
    serialized.includes("表面由「下手だ＋する＋と」构成")
  ) {
    fail("linguistic11: grammar-007 fixed-expression analysis has not been corrected");
  }
}

const reviewCoverage = assertReviewCoverage(
  effective,
  config.kind,
  config.reviewReport,
);

const transportRows = effective.map((row) => {
  if (config.kind === "exercise") {
    return {
      id: row.id,
      prompt: row.prompt,
      answer: row.answer,
      hint: row.hint,
      source_file: row.__source_file,
    };
  }
  return {
    id: row.id,
    source_type: row.source_type,
    source_id: row.source_id,
    linguistic_payload: row.linguistic_payload,
    source_file: row.__source_file,
  };
});

const chunks = [];
for (let start = 0; start < transportRows.length; start += config.chunkSize) {
  chunks.push(transportRows.slice(start, start + config.chunkSize));
}
if (chunks.some((chunk) => chunk.length !== config.chunkSize)) {
  fail(`${group}: a transport chunk does not contain exactly ${config.chunkSize} rows`);
}

const canonical = JSON.stringify(
  effective.map(({ __source_file, ...row }) => row),
  null,
  2,
);
const summary = {
  group,
  kind: config.kind,
  baseRows: baseRows.length,
  uniqueIds: new Set(effective.map((row) => row.id)).size,
  generationOverlayRows: generation.overlayRows,
  generationReplacements: generation.replacements,
  reviewOverlayRows: review.overlayRows,
  reviewReplacements: review.replacements,
  reviewCoverageMarkers: reviewCoverage.markers,
  reviewReportSha256: reviewCoverage.sha256,
  effectiveRows: effective.length,
  chunkSize: config.chunkSize,
  chunks: chunks.length,
  firstId: effective.at(0).id,
  lastId: effective.at(-1).id,
  effectiveSha256: crypto
    .createHash("sha256")
    .update(`${canonical}\n`)
    .digest("hex")
    .toUpperCase(),
};

if (mode === "--summary") {
  process.stdout.write(`${JSON.stringify(summary, null, 2)}\n`);
} else if (mode === "--chunk") {
  const index = Number(modeValue);
  if (!Number.isInteger(index) || index < 1 || index > chunks.length) {
    fail(`${group}: chunk index must be between 1 and ${chunks.length}`);
  }
  const rows = chunks[index - 1];
  process.stdout.write(
    `${JSON.stringify({
      group,
      chunk: index,
      rows: rows.length,
      firstId: rows.at(0).id,
      lastId: rows.at(-1).id,
      base64: Buffer.from(JSON.stringify(rows), "utf8").toString("base64"),
    })}\n`,
  );
} else if (mode === "--write-effective") {
  if (!modeValue) fail(`${group}: --write-effective requires a path`);
  fs.writeFileSync(
    modeValue,
    `${JSON.stringify(
      effective.map(({ __source_file, ...row }) => row),
      null,
      2,
    )}\n`,
    "utf8",
  );
  process.stdout.write(`${JSON.stringify({ ...summary, output: modeValue }, null, 2)}\n`);
} else {
  fail(`${group}: unsupported mode ${mode}`);
}
