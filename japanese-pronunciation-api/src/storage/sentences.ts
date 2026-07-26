import type { NativeTimingReference, ReferenceAlias, ReferenceToken, SentenceReference } from "../contracts";
import { isRecord } from "../contracts";
import { ApiError } from "../errors";
import { splitMorae, toHiragana } from "../japanese/mora";

const KEY_PREFIX = "sentence:";

export async function loadSentenceReference(env: Env, sentenceId: string): Promise<SentenceReference> {
  const raw = await env.SENTENCES.get(`${KEY_PREFIX}${sentenceId}`);
  if (raw === null) throw new ApiError(404, "sentence_not_found", "评测参考句不存在");
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    console.error(JSON.stringify({ message: "invalid_sentence_json", sentenceId }));
    throw new ApiError(500, "invalid_reference_data", "参考句数据损坏");
  }
  const reference = parseSentenceReference(parsed);
  if (reference === null || reference.sentenceId !== sentenceId) {
    console.error(JSON.stringify({ message: "invalid_sentence_reference", sentenceId }));
    throw new ApiError(500, "invalid_reference_data", "参考句数据无效");
  }
  const expected = reference.tokens.flatMap((token) => token.morae).map(toHiragana).join("");
  const declared = splitMorae(reference.phoneticReading).join("");
  if (expected !== declared) {
    console.error(JSON.stringify({ message: "sentence_mora_mismatch", sentenceId }));
    throw new ApiError(500, "invalid_reference_data", "参考句拍序列与发音读音不一致");
  }
  return reference;
}

export function parseSentenceReference(value: unknown): SentenceReference | null {
  if (!isRecord(value)) return null;
  if (
    typeof value.sentenceId !== "string" ||
    typeof value.text !== "string" ||
    typeof value.displayReading !== "string" ||
    typeof value.phoneticReading !== "string" ||
    typeof value.scorerVersion !== "string" ||
    !Array.isArray(value.tokens)
  ) return null;
  const tokens = value.tokens.map(parseToken);
  if (tokens.some((token) => token === null)) return null;
  const native = parseNative(value.native);
  if (native === null) return null;
  const lexicon = parseLexicon(value.lexicon);
  if (lexicon === null) return null;
  const reference: SentenceReference = {
    sentenceId: value.sentenceId,
    text: value.text,
    displayReading: value.displayReading,
    phoneticReading: value.phoneticReading,
    tokens: tokens.filter((token): token is ReferenceToken => token !== null),
    native,
    scorerVersion: value.scorerVersion,
    ...(typeof value.romaji === "string" ? { romaji: value.romaji } : {}),
    ...(lexicon === undefined ? {} : { lexicon }),
  };
  return reference;
}

function parseToken(value: unknown): ReferenceToken | null {
  if (!isRecord(value) || typeof value.surface !== "string" || typeof value.reading !== "string") return null;
  if (!Array.isArray(value.morae) || !value.morae.every((mora) => typeof mora === "string" && mora.length > 0)) return null;
  let aliases: ReferenceAlias[] | undefined;
  if (value.aliases !== undefined) {
    if (!Array.isArray(value.aliases)) return null;
    aliases = [];
    for (const alias of value.aliases) {
      if (!isRecord(alias) || typeof alias.surface !== "string" || typeof alias.reading !== "string") return null;
      aliases.push({ surface: alias.surface, reading: alias.reading });
    }
  }
  return {
    surface: value.surface,
    reading: value.reading,
    morae: value.morae,
    ...(typeof value.pos === "string" ? { pos: value.pos } : {}),
    ...(aliases === undefined ? {} : { aliases }),
  };
}

function parseNative(value: unknown): NativeTimingReference | null {
  if (!isRecord(value)) return null;
  const durationMs = finiteNonNegative(value.durationMs);
  const speechStartMs = finiteNonNegative(value.speechStartMs);
  const speechEndMs = finiteNonNegative(value.speechEndMs);
  if (durationMs === null || speechStartMs === null || speechEndMs === null || speechEndMs < speechStartMs) return null;
  if (!Array.isArray(value.pauseBoundaries) || !value.pauseBoundaries.every((item) => finiteNonNegative(item) !== null)) return null;
  return {
    durationMs,
    speechStartMs,
    speechEndMs,
    pauseBoundaries: value.pauseBoundaries.filter((item): item is number => typeof item === "number"),
    ...(value.source === "native" || value.source === "subtitle" || value.source === "estimated"
      ? { source: value.source }
      : {}),
  };
}

function parseLexicon(value: unknown): Record<string, string> | undefined | null {
  if (value === undefined) return undefined;
  if (!isRecord(value)) return null;
  const entries = Object.entries(value);
  if (!entries.every(([surface, reading]) => surface.length > 0 && typeof reading === "string" && reading.length > 0)) return null;
  return Object.fromEntries(entries.filter((entry): entry is [string, string] => typeof entry[1] === "string"));
}

function finiteNonNegative(value: unknown): number | null {
  return typeof value === "number" && Number.isFinite(value) && value >= 0 ? value : null;
}
