const SMALL_KANA = new Set(["ゃ", "ゅ", "ょ", "ぁ", "ぃ", "ぅ", "ぇ", "ぉ", "ゎ", "ゕ", "ゖ"]);

export function toHiragana(value: string): string {
  const normalized = value.normalize("NFKC");
  let result = "";
  for (const character of normalized) {
    const code = character.codePointAt(0) ?? 0;
    if (code >= 0x30a1 && code <= 0x30f6) {
      result += String.fromCodePoint(code - 0x60);
    } else {
      result += character;
    }
  }
  return result;
}

export function stripJapanesePunctuation(value: string): string {
  return value.normalize("NFKC").replace(/[\p{P}\p{S}\p{Z}\s]/gu, "");
}

export function splitMorae(reading: string): string[] {
  const hiragana = toHiragana(stripJapanesePunctuation(reading));
  const morae: string[] = [];
  for (const character of hiragana) {
    if (SMALL_KANA.has(character) && morae.length > 0) {
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

export function isKana(character: string): boolean {
  return /^[ぁ-ゖァ-ヺー]$/u.test(character);
}
