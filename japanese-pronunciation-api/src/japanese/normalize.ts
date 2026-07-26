import type { AsrWord, MoraEvidence, SentenceReference } from "../contracts";
import { toKana } from "wanakana";
import { isKana, splitMorae, stripJapanesePunctuation, toHiragana } from "./mora";

interface LexiconEntry {
  surface: string;
  reading: string;
  tokenIndex?: number;
  contextualParticle?: boolean;
}

export interface NormalizedTranscript {
  kana: string;
  morae: MoraEvidence[];
  unknownRatio: number;
  unknownCharacters: string[];
}

interface SpannedMora extends MoraEvidence {
  sourceStart: number;
  sourceEnd: number;
}

interface InternalNormalizedTranscript extends Omit<NormalizedTranscript, "morae"> {
  morae: SpannedMora[];
}

export function expectedMorae(reference: SentenceReference): MoraEvidence[] {
  return reference.tokens.flatMap((token, tokenIndex) => {
    const values = token.morae.length > 0 ? token.morae : splitMorae(token.reading);
    return values.map((value) => ({ value: toHiragana(value), tokenIndex }));
  });
}

export function normalizeTranscript(
  transcript: string,
  words: readonly AsrWord[],
  reference: SentenceReference,
): NormalizedTranscript {
  const lexicon = buildLexicon(reference);
  if (words.length === 0) return withoutSourceSpans(normalizeText(transcript, lexicon));

  let joined = "";
  const wordSpans: Array<{ sourceStart: number; sourceEnd: number; startMs: number; endMs: number; confidence: number }> = [];
  for (const word of words) {
    const cleanWord = stripJapanesePunctuation(normalizeAsrScript(word.word));
    if (cleanWord === "") continue;
    const sourceStart = joined.length;
    joined += cleanWord;
    wordSpans.push({
      sourceStart,
      sourceEnd: joined.length,
      startMs: word.start * 1000,
      endMs: word.end * 1000,
      confidence: word.confidence,
    });
  }

  if (joined === "" && transcript.trim() !== "") return withoutSourceSpans(normalizeText(transcript, lexicon));
  const normalized = normalizeText(joined, lexicon);
  const morae = normalized.morae.map((item): MoraEvidence => {
    const overlaps = wordSpans.filter((span) => span.sourceEnd > item.sourceStart && span.sourceStart < item.sourceEnd);
    const evidence = withoutSourceSpan(item);
    if (overlaps.length === 0) return evidence;
    return {
      ...evidence,
      startMs: Math.min(...overlaps.map((span) => span.startMs)),
      endMs: Math.max(...overlaps.map((span) => span.endMs)),
      confidence: overlaps.reduce((sum, span) => sum + span.confidence, 0) / overlaps.length,
    };
  });
  return {
    kana: normalized.kana,
    morae,
    unknownRatio: normalized.unknownRatio,
    unknownCharacters: normalized.unknownCharacters,
  };
}

function normalizeText(text: string, lexicon: readonly LexiconEntry[]): InternalNormalizedTranscript {
  const clean = stripJapanesePunctuation(normalizeAsrScript(text));
  const morae: SpannedMora[] = [];
  const unknownCharacters: string[] = [];
  let unknownCount = 0;
  let index = 0;
  let lastMatchedTokenIndex = -1;

  while (index < clean.length) {
    const entry = lexicon.find(
      (candidate) =>
        clean.startsWith(candidate.surface, index) &&
        (!candidate.contextualParticle || candidate.tokenIndex === lastMatchedTokenIndex + 1),
    );
    if (entry) {
      const values = splitMorae(entry.reading);
      for (let moraIndex = 0; moraIndex < values.length; moraIndex += 1) {
        const value = values[moraIndex];
        if (!value) continue;
        morae.push({
          value,
          sourceStart: index + Math.floor((moraIndex * entry.surface.length) / values.length),
          sourceEnd: index + Math.max(1, Math.ceil(((moraIndex + 1) * entry.surface.length) / values.length)),
          ...(entry.tokenIndex === undefined ? {} : { tokenIndex: entry.tokenIndex }),
        });
      }
      if (entry.tokenIndex !== undefined) lastMatchedTokenIndex = entry.tokenIndex;
      index += entry.surface.length;
      continue;
    }

    const character = clean[index] ?? "";
    if (isKana(character)) {
      let end = index + 1;
      while (end < clean.length && isKana(clean[end] ?? "")) end += 1;
      const values = splitMorae(clean.slice(index, end));
      for (let moraIndex = 0; moraIndex < values.length; moraIndex += 1) {
        const value = values[moraIndex];
        if (!value) continue;
        morae.push({
          value,
          sourceStart: index + Math.floor((moraIndex * (end - index)) / values.length),
          sourceEnd: index + Math.max(1, Math.ceil(((moraIndex + 1) * (end - index)) / values.length)),
        });
      }
      index = end;
      continue;
    }

    unknownCount += 1;
    unknownCharacters.push(character);
    morae.push({ value: "�", unknown: true, sourceStart: index, sourceEnd: index + 1 });
    index += 1;
  }

  return {
    kana: morae.map((mora) => mora.value).join(""),
    morae,
    unknownRatio: clean.length === 0 ? 1 : unknownCount / Array.from(clean).length,
    unknownCharacters: [...new Set(unknownCharacters)],
  };
}

/**
 * Japanese ASR occasionally emits a phonetic Latin transcription even when
 * language=ja. Treat that output as pronunciation evidence instead of marking
 * every Latin character unknown. The substitutions cover common acoustic
 * spellings produced by multilingual recognizers (l/r, v/b and hard c/k).
 */
export function normalizeAsrScript(value: string): string {
  return value.replace(/[A-Za-z\u00c0-\u024f]+/gu, (run) => {
    const ascii = run
      .normalize("NFKD")
      .replace(/[\u0300-\u036f]/gu, "")
      .toLowerCase()
      .replaceAll("l", "r")
      .replaceAll("v", "b")
      .replace(/c(?=[aou])/gu, "k")
      .replace(/c(?=[ei])/gu, "s")
      .replace(/qu/gu, "k");
    return toHiragana(toKana(ascii));
  });
}

function withoutSourceSpans(value: InternalNormalizedTranscript): NormalizedTranscript {
  return { ...value, morae: value.morae.map(withoutSourceSpan) };
}

function withoutSourceSpan(value: SpannedMora): MoraEvidence {
  return {
    value: value.value,
    ...(value.tokenIndex === undefined ? {} : { tokenIndex: value.tokenIndex }),
    ...(value.startMs === undefined ? {} : { startMs: value.startMs }),
    ...(value.endMs === undefined ? {} : { endMs: value.endMs }),
    ...(value.confidence === undefined ? {} : { confidence: value.confidence }),
    ...(value.unknown === undefined ? {} : { unknown: value.unknown }),
  };
}

function buildLexicon(reference: SentenceReference): LexiconEntry[] {
  const bySurface = new Map<string, LexiconEntry>();
  const add = (surface: string, reading: string, tokenIndex?: number, contextualParticle = false): void => {
    const normalizedSurface = stripJapanesePunctuation(surface);
    if (!normalizedSurface || !reading) return;
    const entry: LexiconEntry = {
      surface: normalizedSurface,
      reading: toHiragana(reading),
      ...(tokenIndex === undefined ? {} : { tokenIndex }),
      ...(contextualParticle ? { contextualParticle: true } : {}),
    };
    const existing = bySurface.get(normalizedSurface);
    if (!existing || (existing.tokenIndex === undefined && tokenIndex !== undefined)) bySurface.set(normalizedSurface, entry);
  };

  reference.tokens.forEach((token, tokenIndex) => {
    const contextualParticle = token.pos === "particle" && token.surface !== token.reading && Array.from(token.surface).every(isKana);
    add(token.surface, token.reading, tokenIndex, contextualParticle);
    add(token.reading, token.reading, tokenIndex);
    for (const alias of token.aliases ?? []) add(alias.surface, alias.reading, tokenIndex);
  });
  for (const [surface, reading] of Object.entries(reference.lexicon ?? {})) add(surface, reading);
  return [...bySurface.values()].sort((left, right) => right.surface.length - left.surface.length);
}
