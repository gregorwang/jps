"""Japanese text helpers — stdlib only."""

from __future__ import annotations

import re
import unicodedata

RE_HIRAGANA = re.compile(r"[\u3040-\u309f]")
RE_KATAKANA = re.compile(r"[\u30a0-\u30ff]")
RE_KANA = re.compile(r"[\u3040-\u309f\u30a0-\u30ff]")
RE_CJK = re.compile(r"[\u4e00-\u9fff]")
RE_JAPANESE = re.compile(r"[\u3040-\u309f\u30a0-\u30ff\u4e00-\u9fff\u30fc\u30fb]")
RE_LATIN = re.compile(r"[A-Za-z]")
RE_ASS_NOISE = re.compile(r"[\\{}＜＞<>♪♫…・\s　]+")

_HIRA_ROMAJI: dict[str, str] = {}
_KATA_ROMAJI: dict[str, str] = {}


def _init_romaji() -> None:
    if _HIRA_ROMAJI:
        return
    base = {
        "あ": "a", "い": "i", "う": "u", "え": "e", "お": "o",
        "か": "ka", "き": "ki", "く": "ku", "け": "ke", "こ": "ko",
        "さ": "sa", "し": "shi", "す": "su", "せ": "se", "そ": "so",
        "た": "ta", "ち": "chi", "つ": "tsu", "て": "te", "と": "to",
        "な": "na", "に": "ni", "ぬ": "nu", "ね": "ne", "の": "no",
        "は": "ha", "ひ": "hi", "ふ": "fu", "へ": "he", "ほ": "ho",
        "ま": "ma", "み": "mi", "む": "mu", "め": "me", "も": "mo",
        "や": "ya", "ゆ": "yu", "よ": "yo",
        "ら": "ra", "り": "ri", "る": "ru", "れ": "re", "ろ": "ro",
        "わ": "wa", "を": "wo", "ん": "n",
        "が": "ga", "ぎ": "gi", "ぐ": "gu", "げ": "ge", "ご": "go",
        "ざ": "za", "じ": "ji", "ず": "zu", "ぜ": "ze", "ぞ": "zo",
        "だ": "da", "ぢ": "ji", "づ": "zu", "で": "de", "ど": "do",
        "ば": "ba", "び": "bi", "ぶ": "bu", "べ": "be", "ぼ": "bo",
        "ぱ": "pa", "ぴ": "pi", "ぷ": "pu", "ぺ": "pe", "ぽ": "po",
        "ゃ": "ya", "ゅ": "yu", "ょ": "yo",
        "っ": "", "ー": "-", "ゔ": "vu",
    }
    for k, v in base.items():
        _HIRA_ROMAJI[k] = v
        _KATA_ROMAJI[chr(ord(k) + 0x60)] = v


def normalize_ja(text: str) -> str:
    text = unicodedata.normalize("NFKC", text or "")
    text = RE_ASS_NOISE.sub("", text)
    return text.strip()


def is_japanese_line(text: str) -> bool:
    text = normalize_ja(text)
    if not text or len(text) < 2:
        return False
    ja = len(RE_JAPANESE.findall(text))
    latin = len(RE_LATIN.findall(text))
    if ja == 0:
        return False
    return latin <= ja


def kana_ratio(text: str) -> float:
    chars = [c for c in text if not c.isspace()]
    if not chars:
        return 0.0
    return len(RE_KANA.findall(text)) / len(chars)


def has_kanji(text: str) -> bool:
    return bool(RE_CJK.search(text))


def is_mostly_katakana(text: str) -> bool:
    chars = [c for c in text if RE_JAPANESE.match(c)]
    if not chars:
        return False
    kata = sum(1 for c in chars if RE_KATAKANA.match(c))
    return kata / len(chars) >= 0.8


def to_romaji(text: str) -> str:
    _init_romaji()
    out: list[str] = []
    i = 0
    while i < len(text):
        ch = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ""
        if ch == "っ" and nxt:
            nxt_rom = _HIRA_ROMAJI.get(nxt) or _KATA_ROMAJI.get(nxt, "")
            if nxt_rom:
                out.append(nxt_rom[0])
            i += 1
            continue
        if ch in _HIRA_ROMAJI:
            out.append(_HIRA_ROMAJI[ch])
            i += 1
            continue
        if ch in _KATA_ROMAJI:
            out.append(_KATA_ROMAJI[ch])
            i += 1
            continue
        if ch in "、。！？…":
            out.append(" ")
        i += 1
    return re.sub(r"\s+", " ", "".join(out)).strip()


def guess_reading(surface: str, contexts: list[str]) -> str:
    if RE_KANA.fullmatch(surface or ""):
        return surface
    if not has_kanji(surface):
        return surface
    for ctx in contexts:
        ctx = normalize_ja(ctx)
        if surface not in ctx:
            continue
        idx = ctx.find(surface)
        after = ctx[idx + len(surface):]
        m = re.match(r"[\u3040-\u309f\u30a0-\u30ffー]+", after)
        if m:
            return m.group(0)
    return ""


def infer_pos(surface: str) -> str:
    if surface.endswith(("する", "した", "して", "しない", "できる")):
        return "動詞"
    if surface.endswith(("い", "く", "しい", "たい")) and has_kanji(surface):
        return "形容詞/形容動詞"
    if surface.endswith(("よ", "ね", "か", "な", "わ", "ぞ", "ぜ", "さ")):
        return "終助詞/語気"
    if is_mostly_katakana(surface):
        return "外来語/擬音"
    if len(surface) <= 2 and RE_KANA.search(surface):
        return "副詞/感嘆"
    return "名詞/表現"


def estimate_jlpt(surface: str, count: int) -> str:
    if count >= 20 or len(surface) <= 2:
        return "N5"
    if count >= 8 or len(surface) <= 4:
        return "N4"
    if len(surface) >= 6:
        return "N3"
    return "N4"
