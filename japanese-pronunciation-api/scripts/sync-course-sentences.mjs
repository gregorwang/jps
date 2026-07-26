import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { mkdir, readdir, readFile, writeFile } from "node:fs/promises";
import kuromoji from "kuromoji";

const require = createRequire(import.meta.url);
const projectDirectory = fileURLToPath(new URL("../", import.meta.url));
const repositoryDirectory = fileURLToPath(new URL("../../", import.meta.url));
const outputDirectory = join(projectDirectory, ".generated");
const outputFile = join(outputDirectory, "sentences-kv.json");
const reportFile = join(outputDirectory, "sentences-report.json");
const manualDataDirectory = join(projectDirectory, "data", "sentences");
const scorerVersion = "ja-mora-v1.1.0";
const pageSize = 1000;
const tokenReadingOverrides = {
  "危": "あぶ",
  "貌": "ぼう",
  "癒": "いや",
  "虐": "ぎゃく",
};
const sentenceReadingOverrides = {
  "k-on-ep10-sent-00072": "いえわたしことぶきけのしつじでございます",
  "re-zero-s01e13-sentence-080": "これよりきしのほこりをけがしたふていのやからにちゅうをくだす",
  "re-zero-s01e15-sentence-090": "このばにいないあるじにかわりれむがちゅうをくだします",
  "re-zero-sentence-ep050-382": "ねむりにおちるせかいをみまもるほしぼしに",
  "re-zero-sentence-ep050-393": "たいようにほしぼしにせいれいにせかいにほこりに",
};

const { url: supabaseUrl, key: supabaseKey } = await loadSupabaseConfig();
const tokenizer = await buildTokenizer();
const plans = await fetchAll(
  "/rest/v1/episode_learning_plans?select=work_slug,episode,shadowing_sentence_ids&order=work_slug.asc,episode.asc,plan_slot.asc",
);
const plannedIds = new Set(
  plans.flatMap((plan) => (Array.isArray(plan.shadowing_sentence_ids) ? plan.shadowing_sentence_ids : []))
    .filter((id) => typeof id === "string" && id.trim() !== ""),
);
if (plannedIds.size === 0) throw new Error("No shadowing sentence IDs were returned by Supabase");

const sentenceRows = await fetchAll(
  "/rest/v1/learning_sentences?select=id,work_slug,episode,ja_text,reading,romaji,source_line_no,audio_url&order=id.asc",
);
const courseSentences = sentenceRows.filter((sentence) =>
  typeof sentence.id === "string" && sentence.id.trim() !== "" &&
  typeof sentence.ja_text === "string" && sentence.ja_text.trim() !== "",
);
const foundIds = new Set(courseSentences.map((sentence) => sentence.id));
const missingIds = [...plannedIds].filter((id) => !foundIds.has(id));
if (missingIds.length > 0) {
  throw new Error(`Supabase did not return ${missingIds.length} planned sentence(s): ${missingIds.slice(0, 5).join(", ")}`);
}

const episodeKeys = new Set(courseSentences.map((sentence) => `${sentence.work_slug}:${sentence.episode}`));
const subtitleRows = (
  await mapWithConcurrency([...episodeKeys], 8, async (episodeKey) => {
    const separator = episodeKey.lastIndexOf(":");
    const workSlug = episodeKey.slice(0, separator);
    const episode = Number(episodeKey.slice(separator + 1));
    return fetchAll(
      `/rest/v1/subtitle_lines?select=work_slug,episode,line_no,start_time,end_time&work_slug=eq.${encodeURIComponent(workSlug)}&episode=eq.${episode}&order=line_no.asc`,
    );
  })
).flat();
const timings = new Map(
  subtitleRows.map((row) => [`${row.work_slug}:${row.episode}:${row.line_no}`, row]),
);

const generatedReferences = [];
const unresolved = [];
let databaseReadingFallbacks = 0;
let subtitleTimings = 0;
let estimatedTimings = 0;
for (const sentence of courseSentences) {
  try {
    const built = buildSentenceReference(sentence, timings, tokenizer);
    generatedReferences.push(built.reference);
    if (built.usedDatabaseReadingFallback) databaseReadingFallbacks += 1;
    if (built.reference.native.source === "subtitle") subtitleTimings += 1;
    else estimatedTimings += 1;
  } catch (error) {
    unresolved.push({
      sentenceId: sentence.id,
      text: sentence.ja_text,
      message: error instanceof Error ? error.message : String(error),
    });
  }
}

const manualReferences = await loadManualReferences();
const referencesById = new Map(manualReferences.map((reference) => [reference.sentenceId, reference]));
for (const reference of generatedReferences) referencesById.set(reference.sentenceId, reference);
const references = [...referencesById.values()].sort((left, right) => left.sentenceId.localeCompare(right.sentenceId));
const entries = references.map((reference) => ({
  key: `sentence:${reference.sentenceId}`,
  value: JSON.stringify(reference),
}));
const report = {
  generatedAt: new Date().toISOString(),
  courseSentences: courseSentences.length,
  plannedUniqueSentences: plannedIds.size,
  generatedCourseReferences: generatedReferences.length,
  manualReferences: manualReferences.length,
  kvEntries: entries.length,
  databaseReadingFallbacks,
  subtitleTimings,
  estimatedTimings,
  unresolved,
};

await mkdir(outputDirectory, { recursive: true });
await writeFile(reportFile, `${JSON.stringify(report, null, 2)}\n`, "utf8");
if (unresolved.length > 0) {
  throw new Error(
    `Could not build ${unresolved.length} pronunciation reference(s). See .generated/sentences-report.json`,
  );
}
await writeFile(outputFile, `${JSON.stringify(entries)}\n`, "utf8");
console.log(
  `Prepared ${entries.length} KV reference(s): ${generatedReferences.length} course sentences, ` +
    `${subtitleTimings} subtitle timings, ${estimatedTimings} estimated timings`,
);

function buildSentenceReference(sentence, timingRows, activeTokenizer) {
  const text = requiredText(sentence.ja_text, `${sentence.id}.ja_text`);
  const spokenText = text.replace(/([\p{Script=Han}々]+)[(（][ぁ-ゖァ-ヺー]+[)）]/gu, "$1");
  const databaseReading = sentenceReadingOverrides[sentence.id] || normalizedKanaReading(sentence.reading);
  const rawTokens = activeTokenizer.tokenize(spokenText);
  const tokens = [];
  const unresolvedSurfaces = [];

  for (const rawToken of rawTokens) {
    const surface = typeof rawToken.surface_form === "string" ? rawToken.surface_form.normalize("NFKC") : "";
    if (surface === "" || isOnlyPunctuation(surface)) continue;
    const reading = tokenReadingOverrides[surface] ||
      normalizedKanaReading(rawToken.pronunciation) ||
      normalizedKanaReading(rawToken.reading) ||
      fallbackSymbolReading(surface);
    if (reading === "") {
      unresolvedSurfaces.push(surface);
      continue;
    }
    const morae = splitMorae(reading);
    if (morae.length === 0) {
      unresolvedSurfaces.push(surface);
      continue;
    }
    tokens.push({
      surface,
      reading,
      morae,
      ...(typeof rawToken.pos === "string" && rawToken.pos !== "記号" ? { pos: normalizePartOfSpeech(rawToken.pos) } : {}),
    });
  }

  let finalTokens = tokens;
  let usedDatabaseReadingFallback = false;
  if (unresolvedSurfaces.length > 0 || tokens.length === 0) {
    if (databaseReading === "") {
      throw new Error(`unresolved reading for: ${unresolvedSurfaces.join(" / ") || text}`);
    }
    finalTokens = [{ surface: text, reading: databaseReading, morae: splitMorae(databaseReading) }];
    usedDatabaseReadingFallback = true;
  }

  const phoneticReading = finalTokens.map((token) => token.reading).join("");
  if (phoneticReading === "") throw new Error("phonetic reading is empty");
  const timingKey = `${sentence.work_slug}:${sentence.episode}:${sentence.source_line_no}`;
  const native = buildTimingReference(text, splitMorae(phoneticReading).length, timingRows.get(timingKey));
  const lexiconEntries = finalTokens
    .filter((token) => !isOnlyKana(token.surface) && token.surface.length <= 24)
    .map((token) => [token.surface, token.reading]);

  return {
    usedDatabaseReadingFallback,
    reference: {
      sentenceId: requiredText(sentence.id, "sentence.id"),
      text: spokenText,
      displayReading: databaseReading || finalTokens.map((token) => token.reading).join(" "),
      phoneticReading,
      ...(typeof sentence.romaji === "string" && sentence.romaji.trim() !== "" ? { romaji: sentence.romaji.trim() } : {}),
      tokens: finalTokens,
      ...(lexiconEntries.length > 0 ? { lexicon: Object.fromEntries(lexiconEntries) } : {}),
      native,
      scorerVersion,
    },
  };
}

function buildTimingReference(text, moraCount, row) {
  const startMs = parseTimestamp(row?.start_time);
  const endMs = parseTimestamp(row?.end_time);
  const cueDuration = startMs !== null && endMs !== null ? endMs - startMs : 0;
  const hasSubtitleTiming = cueDuration >= 400 && cueDuration <= 15_000;
  const durationMs = hasSubtitleTiming
    ? Math.round(cueDuration)
    : Math.round(Math.min(14_000, Math.max(700, 380 + moraCount * 165)));
  const speechStartMs = Math.min(100, Math.round(durationMs * 0.04));
  const speechEndMs = Math.max(speechStartMs + 1, durationMs - speechStartMs);
  return {
    durationMs,
    speechStartMs,
    speechEndMs,
    pauseBoundaries: punctuationBoundaries(text, speechStartMs, speechEndMs),
    source: hasSubtitleTiming ? "subtitle" : "estimated",
  };
}

function punctuationBoundaries(text, speechStartMs, speechEndMs) {
  const characters = [...text];
  return characters.flatMap((character, index) => {
    if (!/[、，,。！？!?…]/u.test(character) || index === characters.length - 1) return [];
    const ratio = (index + 1) / Math.max(1, characters.length);
    return [Math.round(speechStartMs + (speechEndMs - speechStartMs) * ratio)];
  });
}

function parseTimestamp(value) {
  if (typeof value !== "string") return null;
  const match = value.trim().match(/^(\d{1,3}):(\d{2}):(\d{2})[,.](\d{1,3})$/u);
  if (!match) return null;
  const [, hours, minutes, seconds, fraction] = match;
  const milliseconds = Number(fraction.padEnd(3, "0").slice(0, 3));
  return ((Number(hours) * 60 + Number(minutes)) * 60 + Number(seconds)) * 1000 + milliseconds;
}

function normalizedKanaReading(value) {
  if (typeof value !== "string") return "";
  const compact = value.normalize("NFKC").replace(/[\p{P}\p{S}\p{Z}\s]/gu, "");
  if (compact === "" || !/^[ぁ-ゖァ-ヺー]+$/u.test(compact)) return "";
  return toHiragana(compact);
}

function fallbackSymbolReading(surface) {
  const normalized = surface.normalize("NFKC");
  const kanaReading = normalizedKanaReading(normalized);
  if (kanaReading !== "") return kanaReading;
  if (/^\d+$/u.test(normalized)) {
    const digits = {
      "0": "ぜろ", "1": "いち", "2": "に", "3": "さん", "4": "よん",
      "5": "ご", "6": "ろく", "7": "なな", "8": "はち", "9": "きゅう",
    };
    return [...normalized].map((digit) => digits[digit] ?? "").join("");
  }
  if (/^[A-Za-z]+$/u.test(normalized)) {
    const letters = {
      A: "えー", B: "びー", C: "しー", D: "でぃー", E: "いー", F: "えふ", G: "じー",
      H: "えいち", I: "あい", J: "じぇー", K: "けー", L: "える", M: "えむ", N: "えぬ",
      O: "おー", P: "ぴー", Q: "きゅー", R: "あーる", S: "えす", T: "てぃー", U: "ゆー",
      V: "ぶい", W: "だぶりゅー", X: "えっくす", Y: "わい", Z: "ぜっと",
    };
    return [...normalized.toUpperCase()].map((letter) => letters[letter] ?? "").join("");
  }
  return isOnlyKana(normalized) ? toHiragana(normalized) : "";
}

function splitMorae(reading) {
  const smallKana = new Set(["ゃ", "ゅ", "ょ", "ぁ", "ぃ", "ぅ", "ぇ", "ぉ", "ゎ", "ゕ", "ゖ"]);
  const hiragana = toHiragana(reading.normalize("NFKC").replace(/[\p{P}\p{S}\p{Z}\s]/gu, ""));
  const morae = [];
  for (const character of hiragana) {
    if (smallKana.has(character) && morae.length > 0) {
      const previous = morae.at(-1) ?? "";
      if (previous !== "っ" && previous !== "ん" && previous !== "ー") {
        morae[morae.length - 1] = previous + character;
        continue;
      }
    }
    morae.push(character);
  }
  return morae;
}

function toHiragana(value) {
  let result = "";
  for (const character of value.normalize("NFKC")) {
    const code = character.codePointAt(0) ?? 0;
    result += code >= 0x30a1 && code <= 0x30f6 ? String.fromCodePoint(code - 0x60) : character;
  }
  return result;
}

function isOnlyPunctuation(value) {
  return value !== "" && /^[\p{P}\p{S}\p{Z}\s]+$/u.test(value);
}

function isOnlyKana(value) {
  return value !== "" && /^[ぁ-ゖァ-ヺー]+$/u.test(value.normalize("NFKC"));
}

function normalizePartOfSpeech(value) {
  const positions = { 助詞: "particle", 助動詞: "auxiliary", 動詞: "verb", 名詞: "noun", 形容詞: "adjective" };
  return positions[value] ?? value;
}

function requiredText(value, field) {
  if (typeof value !== "string" || value.trim() === "") throw new Error(`${field} is required`);
  return value.trim();
}

async function buildTokenizer() {
  const dictionaryPath = join(dirname(require.resolve("kuromoji")), "..", "dict");
  return new Promise((resolve, reject) => {
    kuromoji.builder({ dicPath: dictionaryPath }).build((error, instance) => {
      if (error) reject(error);
      else resolve(instance);
    });
  });
}

async function loadManualReferences() {
  const filenames = (await readdir(manualDataDirectory)).filter((name) => name.endsWith(".json")).sort();
  return Promise.all(filenames.map(async (filename) => JSON.parse(await readFile(join(manualDataDirectory, filename), "utf8"))));
}

async function fetchAll(path) {
  const rows = [];
  for (let from = 0; ; from += pageSize) {
    const response = await fetch(`${supabaseUrl}${path}`, {
      headers: {
        apikey: supabaseKey,
        Authorization: `Bearer ${supabaseKey}`,
        Range: `${from}-${from + pageSize - 1}`,
      },
    });
    if (!response.ok) throw new Error(`Supabase ${response.status} for ${path}: ${await response.text()}`);
    const page = await response.json();
    if (!Array.isArray(page)) throw new Error(`Supabase returned a non-array for ${path}`);
    rows.push(...page);
    if (page.length < pageSize) return rows;
  }
}

async function loadSupabaseConfig() {
  const configuredUrl = process.env.SUPABASE_URL?.trim();
  const configuredKey = process.env.SUPABASE_PUBLISHABLE_KEY?.trim();
  if (configuredUrl && configuredKey) return { url: configuredUrl.replace(/\/$/u, ""), key: configuredKey };
  const wrangler = await readFile(join(repositoryDirectory, "wrangler.toml"), "utf8");
  const url = wrangler.match(/^SUPABASE_URL\s*=\s*"([^"]+)"/mu)?.[1]?.trim();
  const key = wrangler.match(/^SUPABASE_PUBLISHABLE_KEY\s*=\s*"([^"]+)"/mu)?.[1]?.trim();
  if (!url || !key) throw new Error("Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY before syncing references");
  return { url: url.replace(/\/$/u, ""), key };
}

async function mapWithConcurrency(items, concurrency, operation) {
  const results = Array.from({ length: items.length });
  let nextIndex = 0;
  await Promise.all(Array.from({ length: Math.min(concurrency, items.length) }, async () => {
    while (nextIndex < items.length) {
      const index = nextIndex;
      nextIndex += 1;
      results[index] = await operation(items[index], index);
    }
  }));
  return results;
}
