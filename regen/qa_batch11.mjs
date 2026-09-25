import fs from "node:fs";

const batch11Files = [
  "regen/batch11_vocab_linguistic.json",
  "regen/batch11_grammar_sentence_linguistic.json",
  "regen/batch11_sentence_linguistic_overlay.json",
];
const priorFiles = Array.from({ length: 10 }, (_, index) => index + 1).flatMap(
  (batch) => [
    `regen/batch${batch}_vocab_linguistic.json`,
    `regen/batch${batch}_grammar_sentence_linguistic.json`,
  ],
);

const loadRows = (files) =>
  files.flatMap((file) =>
    JSON.parse(fs.readFileSync(file, "utf8")).map((row) => ({ ...row, _file: file })),
  );
const current = loadRows(batch11Files);
const prior = loadRows(priorFiles);
const normalize = (text) =>
  text
    .normalize("NFKC")
    .replace(/[\s\p{P}\p{S}]+/gu, "")
    .toLowerCase();
const walkStrings = (value, output = []) => {
  if (typeof value === "string") output.push(value);
  else if (Array.isArray(value)) value.forEach((item) => walkStrings(item, output));
  else if (value && typeof value === "object")
    Object.entries(value).forEach(([key, item]) => {
      if (key !== "domain" && key !== "level") walkStrings(item, output);
    });
  return output;
};
const cardStrings = (row) => walkStrings(row.linguistic_payload);
const cardText = (row) => normalize(cardStrings(row).join(""));
const ngrams = (text, size) => {
  const result = new Set();
  for (let index = 0; index + size <= text.length; index += 1) {
    result.add(text.slice(index, index + size));
  }
  return result;
};
const jaccard = (left, right) => {
  let intersection = 0;
  for (const value of left) if (right.has(value)) intersection += 1;
  return intersection / (left.size + right.size - intersection || 1);
};

const priorIds = new Set(prior.map((row) => row.id));
const priorSources = new Set(prior.map((row) => `${row.source_type}\0${row.source_id}`));
const duplicatePriorIds = current.filter((row) => priorIds.has(row.id)).map((row) => row.id);
const duplicatePriorSources = current
  .filter((row) => priorSources.has(`${row.source_type}\0${row.source_id}`))
  .map((row) => `${row.source_type}:${row.source_id}`);

const currentLong = new Map();
const priorLong = new Map();
for (const row of current) {
  for (const text of cardStrings(row)) {
    const trimmed = text.trim();
    if (trimmed.length >= 12) {
      const owners = currentLong.get(trimmed) ?? new Set();
      owners.add(row.id);
      currentLong.set(trimmed, owners);
    }
  }
}
for (const row of prior) {
  for (const text of cardStrings(row)) {
    const trimmed = text.trim();
    if (trimmed.length >= 12) {
      const owners = priorLong.get(trimmed) ?? new Set();
      owners.add(row.id);
      priorLong.set(trimmed, owners);
    }
  }
}
const repeatedLongWithin = [...currentLong]
  .filter(([, owners]) => owners.size > 1)
  .map(([text, owners]) => ({ text, count: owners.size }));
const repeatedLongAcross = [...currentLong]
  .filter(([text]) => priorLong.has(text))
  .map(([text, owners]) => ({
    text,
    current: owners.size,
    prior: priorLong.get(text).size,
  }));

const chunkOwners = new Map();
for (const row of [...prior, ...current]) {
  const group = current.some((candidate) => candidate.id === row.id) ? "current" : "prior";
  for (const text of cardStrings(row)) {
    const normalized = normalize(text);
    for (const chunk of ngrams(normalized, 28)) {
      const owners = chunkOwners.get(chunk) ?? [];
      owners.push({ group, id: row.id });
      chunkOwners.set(chunk, owners);
    }
  }
}
const repeatedChunks = [...chunkOwners]
  .map(([chunk, owners]) => ({
    chunk,
    owners: [...new Map(owners.map((owner) => [`${owner.group}\0${owner.id}`, owner])).values()],
  }))
  .filter(
    ({ owners }) =>
      owners.some((owner) => owner.group === "current") &&
      new Set(owners.map((owner) => owner.id)).size > 1,
  );

const leakPattern =
  /re-zero-|source[_ ]?id|prompt[_ ]?version|\bsource\b|\bcurrent\b|\bEP\d+\b|\blines?\s*\d+\b|数据库|模板生成/i;
const leaks = current.flatMap((row) =>
  cardStrings(row)
    .filter((text) => leakPattern.test(text))
    .map((text) => ({ id: row.id, text })),
);
const oldTemplatePatterns = [
  "形态构成 (Morphology)",
  "结合上下文精确定位词义，拒绝机械对译。",
  "词汇积累重在听说直觉与例句复现。",
  "形态学：词类构成与活用分析",
  "句法管辖 (Syntactic Scope)",
  "学习卡级别的语言学解析重在建立语言直觉",
  "意群切分 (Chunking)",
  "信息结构 (Information Structure)",
  "句子卡片旨在培养影音语感与流利跟读能力。",
];
const oldTemplateHits = current.flatMap((row) => {
  const serialized = JSON.stringify(row.linguistic_payload);
  return oldTemplatePatterns
    .filter((pattern) => serialized.includes(pattern))
    .map((pattern) => ({ id: row.id, pattern }));
});

const priorNgrams = prior.map((row) => ({ id: row.id, grams: ngrams(cardText(row), 4) }));
let maximumSimilarity = { score: 0, currentId: null, priorId: null };
for (const row of current) {
  const grams = ngrams(cardText(row), 4);
  for (const previous of priorNgrams) {
    const score = jaccard(grams, previous.grams);
    if (score > maximumSimilarity.score) {
      maximumSimilarity = { score, currentId: row.id, priorId: previous.id };
    }
  }
}

console.log(
  JSON.stringify(
    {
      currentRows: current.length,
      priorRows: prior.length,
      currentTypes: Object.fromEntries(
        ["vocab", "grammar", "sentence"].map((type) => [
          type,
          current.filter((row) => row.source_type === type).length,
        ]),
      ),
      duplicatePriorIds,
      duplicatePriorSources,
      repeatedLongWithin,
      repeatedLongAcross,
      repeated28CharChunks: repeatedChunks,
      leaks,
      oldTemplateHits,
      maximumCrossBatchFourGramJaccard: maximumSimilarity,
    },
    null,
    2,
  ),
);
