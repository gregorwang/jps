"""Match learning sentences to subtitle index."""

from __future__ import annotations

import csv
import re
import unicodedata
from dataclasses import dataclass
from difflib import SequenceMatcher
from pathlib import Path

from ass_parser import SubtitleRow

RE_ASS_TAGS = re.compile(r"\{[^}]*\}")
RE_SPEAKER = re.compile(r"^[\(（][^)）]{1,20}[\)）]")
RE_QUOTES = re.compile(r"^[「『\"'“”‘’]+|[」』\"'“”‘’]+$")
RE_SPACES = re.compile(r"[\s　]+")
RE_TRIM_PUNCT = re.compile(r"^[~～!?！？…\.。、，]+|[~～!?！？…\.。、，]+$")


def normalize_for_match(text: str) -> str:
    text = unicodedata.normalize("NFKC", text or "")
    text = RE_ASS_TAGS.sub("", text)
    text = text.replace("\\N", "").replace("\n", "")
    text = RE_SPEAKER.sub("", text)
    text = RE_QUOTES.sub("", text)
    text = RE_SPACES.sub("", text)
    text = text.replace("…", "").replace("...", "")
    text = RE_TRIM_PUNCT.sub("", text)
    return text.strip()


def match_core(text: str) -> str:
    return normalize_for_match(text)


@dataclass
class LearningSentence:
    sentence_id: str
    sort_order: int
    ja_text: str
    zh_text: str
    source_line_no: int | None


@dataclass
class MatchResult:
    subtitle_index: int | None
    subtitle_start_ms: int | None
    subtitle_end_ms: int | None
    matched_zh_text: str
    match_status: str
    match_confidence: str
    match_reason: str
    clip_eligible: bool
    matched_ja_text: str = ""
    candidate_count: int = 1
    used_chronological_fallback: bool = False
    suspicious_flags: tuple[str, ...] = ()


def load_learning_sentences(path: Path, episode_filter: str | None = None) -> list[LearningSentence]:
    rows: list[LearningSentence] = []
    with path.open(encoding="utf-8-sig", newline="") as f:
        for r in csv.DictReader(f):
            ep_id = r.get("episode_id", "")
            if episode_filter and ep_id and episode_filter not in ep_id and ep_id != episode_filter:
                # Also allow matching by episode_id param like re0_s3e07 vs rezero-ep57
                pass
            sort_order = int(r.get("sort_order") or 0)
            source_line_no = r.get("source_line_no")
            rows.append(
                LearningSentence(
                    sentence_id=r["id"],
                    sort_order=sort_order,
                    ja_text=(r.get("ja_text") or "").strip(),
                    zh_text=(r.get("meaning_zh") or r.get("zh_text") or "").strip(),
                    source_line_no=int(source_line_no) if source_line_no else None,
                )
            )
    rows.sort(key=lambda x: (x.sort_order, x.sentence_id))
    return rows


def exact_match(sent_norm: str, rows: list[SubtitleRow]) -> SubtitleRow | None:
    for row in rows:
        if normalize_for_match(row.ja_text) == sent_norm:
            return row
    return None


def substring_match(sent_norm: str, rows: list[SubtitleRow]) -> tuple[SubtitleRow | None, str]:
    best: SubtitleRow | None = None
    best_len = 0
    reason = ""
    for row in rows:
        row_norm = normalize_for_match(row.ja_text)
        if not row_norm:
            continue
        if sent_norm in row_norm:
            if len(row_norm) > best_len:
                best = row
                best_len = len(row_norm)
                reason = "sentence_in_subtitle"
        elif row_norm in sent_norm:
            if len(row_norm) > best_len:
                best = row
                best_len = len(row_norm)
                reason = "subtitle_in_sentence"
    return best, reason


def similarity_match(sent_norm: str, rows: list[SubtitleRow]) -> tuple[SubtitleRow | None, float]:
    best: SubtitleRow | None = None
    best_ratio = 0.0
    for row in rows:
        row_norm = normalize_for_match(row.ja_text)
        if not row_norm:
            continue
        ratio = SequenceMatcher(None, sent_norm, row_norm).ratio()
        if ratio > best_ratio:
            best_ratio = ratio
            best = row
    return best, best_ratio


RE_META_CUE = re.compile(r"^[\(（][^)）]{1,40}[\)）]\s*$")
MAX_BACK_GAP_MS = 12000
MAX_FWD_GAP_MS = 2500


def is_meta_cue(text: str) -> bool:
    raw = (text or "").strip()
    if RE_META_CUE.match(raw):
        return True
    return not normalize_for_match(raw)


def is_rhetorical_prefix(text: str) -> bool:
    if is_meta_cue(text):
        return False
    raw = text.strip()
    norm = normalize_for_match(raw)
    if not norm:
        return False
    if re.search(r"^(フフ|ふふ|はっ|ハァ|あっ)", norm):
        return False
    if raw.endswith("\u2026") or raw.endswith("..."):
        return True
    if len(norm) <= 5 and not re.search(r"[!！?？]$", raw):
        return True
    return False


def combined_norm_rows(rows: list[SubtitleRow], start_idx: int, end_idx: int) -> str:
    parts: list[str] = []
    for i in range(start_idx, end_idx + 1):
        row = rows[i - 1]
        parts.append(normalize_for_match(row.ja_text))
    return "".join(parts)


def _ngram_overlap(sent_core: str, row_core: str, n: int = 4) -> float:
    if len(sent_core) < n:
        return 1.0 if sent_core and sent_core in row_core else 0.0
    windows = [sent_core[i : i + n] for i in range(len(sent_core) - n + 1)]
    if not windows:
        return 0.0
    hits = sum(1 for w in windows if w in row_core)
    return hits / len(windows)


def _longest_run_overlap(sent_core: str, row_core: str) -> float:
    if not sent_core or not row_core:
        return 0.0
    sm = SequenceMatcher(None, sent_core, row_core)
    match = sm.find_longest_match(0, len(sent_core), 0, len(row_core))
    return match.size / len(sent_core)


def enumerate_candidates(
    sent_norm: str, subtitles: list[SubtitleRow], *, best_effort: bool = False
) -> list[tuple[SubtitleRow, str, str, str, float]]:
    sent_core = match_core(sent_norm) if sent_norm else ""
    candidates: list[tuple[SubtitleRow, str, str, str, float]] = []
    seen: set[int] = set()
    for row in subtitles:
        row_norm = normalize_for_match(row.ja_text)
        row_core = match_core(row.ja_text)
        if not row_norm:
            continue
        if row.subtitle_index in seen:
            continue
        if sent_norm == row_norm or (sent_core and sent_core == row_core):
            candidates.append((row, "matched", "high", "exact_match", 1.0))
            seen.add(row.subtitle_index)
            continue
        if sent_core and row_core:
            shorter, longer = (sent_core, row_core) if len(sent_core) <= len(row_core) else (row_core, sent_core)
            if len(shorter) >= 4 and shorter in longer and len(shorter) >= len(longer) * 0.35:
                candidates.append((row, "matched", "high", "core_substring", 0.97))
                seen.add(row.subtitle_index)
                continue
        if sent_norm in row_norm:
            candidates.append((row, "matched", "medium", "sentence_in_subtitle", 0.95))
            seen.add(row.subtitle_index)
            continue
        if row_norm in sent_norm and len(row_norm) >= max(6, len(sent_norm) * 0.35):
            candidates.append((row, "matched", "medium", "subtitle_in_sentence", 0.90))
            seen.add(row.subtitle_index)
            continue
        ratio = SequenceMatcher(None, sent_core or sent_norm, row_core or row_norm).ratio()
        if ratio >= 0.90:
            candidates.append((row, "matched", "high", f"similarity:{ratio:.3f}", ratio))
            seen.add(row.subtitle_index)
        elif ratio >= 0.78:
            candidates.append((row, "matched", "medium", f"similarity:{ratio:.3f}", ratio))
            seen.add(row.subtitle_index)
        elif best_effort and ratio >= 0.55:
            candidates.append((row, "matched", "low", f"best_effort_similarity:{ratio:.3f}", ratio))
            seen.add(row.subtitle_index)
        elif best_effort:
            overlap = _ngram_overlap(sent_core or sent_norm, row_core or row_norm)
            if overlap >= 0.45:
                candidates.append((row, "matched", "low", f"best_effort_ngram:{overlap:.3f}", overlap))
                seen.add(row.subtitle_index)
            else:
                run = _longest_run_overlap(sent_core or sent_norm, row_core or row_norm)
                if run >= 0.40:
                    candidates.append((row, "matched", "low", f"best_effort_lcs:{run:.3f}", run))
                    seen.add(row.subtitle_index)
    candidates.sort(
        key=lambda item: (-item[4], item[0].start_ms, -len(match_core(item[0].ja_text)), item[0].subtitle_index)
    )
    return candidates


def sort_subtitles_chronological(subtitles: list[SubtitleRow]) -> list[SubtitleRow]:
    return sorted(subtitles, key=lambda row: (row.start_ms, row.end_ms, row.subtitle_index))


def expand_ass_range(
    subtitles: list[SubtitleRow],
    anchor_idx: int,
    sent_norm: str,
) -> tuple[int, int, str]:
    sorted_rows = sort_subtitles_chronological(subtitles)
    anchor_pos = next(i for i, row in enumerate(sorted_rows) if row.subtitle_index == anchor_idx)
    start_pos = end_pos = anchor_pos
    reason_parts = [f"anchor:{anchor_idx}"]

    while start_pos > 0:
        prev = sorted_rows[start_pos - 1]
        curr = sorted_rows[start_pos]
        gap = curr.start_ms - prev.end_ms
        if gap > MAX_BACK_GAP_MS:
            break
        if is_meta_cue(prev.ja_text):
            break
        if is_rhetorical_prefix(prev.ja_text):
            start_pos -= 1
            reason_parts.append(f"back:{sorted_rows[start_pos].subtitle_index}")
        else:
            break

    combo = combined_norm_rows_chrono(sorted_rows, start_pos, end_pos)
    if sent_norm in combo or combo in sent_norm or SequenceMatcher(None, sent_norm, combo).ratio() >= 0.92:
        return (
            sorted_rows[start_pos].subtitle_index,
            sorted_rows[end_pos].subtitle_index,
            ";".join(reason_parts),
        )

    fwd_extra = 0
    while fwd_extra < 4:
        combo = combined_norm_rows_chrono(sorted_rows, start_pos, end_pos)
        if sent_norm in combo or combo in sent_norm:
            break
        if SequenceMatcher(None, sent_norm, combo).ratio() >= 0.92:
            break
        next_pos = end_pos + 1
        if next_pos >= len(sorted_rows):
            break
        curr = sorted_rows[end_pos]
        nxt = sorted_rows[next_pos]
        gap = nxt.start_ms - curr.end_ms
        if gap > MAX_FWD_GAP_MS:
            break
        nxt_norm = normalize_for_match(nxt.ja_text)
        if not nxt_norm or (nxt_norm not in sent_norm and sent_norm not in nxt_norm):
            break
        end_pos = next_pos
        fwd_extra += 1
        reason_parts.append(f"fwd:{sorted_rows[end_pos].subtitle_index}")

    return (
        sorted_rows[start_pos].subtitle_index,
        sorted_rows[end_pos].subtitle_index,
        ";".join(reason_parts),
    )


def combined_norm_rows_chrono(rows: list[SubtitleRow], start_pos: int, end_pos: int) -> str:
    parts = [normalize_for_match(rows[i].ja_text) for i in range(start_pos, end_pos + 1)]
    return "".join(parts)


def _row_span(subtitles: list[SubtitleRow], start_idx: int, end_idx: int) -> tuple[int, int]:
    rows_by_idx = {row.subtitle_index: row for row in subtitles}
    start_row = rows_by_idx[start_idx]
    end_row = rows_by_idx[end_idx]
    start_ms = min(start_row.start_ms, end_row.start_ms)
    end_ms = max(start_row.end_ms, end_row.end_ms)
    if end_ms - start_ms > 30000:
        return start_row.start_ms, start_row.end_ms
    return start_ms, end_ms


def match_sentence_chronological(
    sentence: LearningSentence,
    subtitles: list[SubtitleRow],
    min_start_ms: int = 0,
    *,
    best_effort: bool = False,
) -> MatchResult:
    sent_norm = normalize_for_match(sentence.ja_text)
    if not sent_norm:
        return MatchResult(
            None, None, None, "", "unmatched", "none", "empty_sentence", False
        )

    candidates = enumerate_candidates(sent_norm, subtitles, best_effort=best_effort)
    if not candidates:
        return MatchResult(
            None, None, None, "", "unmatched", "none", "no_candidate", False,
            candidate_count=0,
        )

    chronological = [c for c in candidates if c[0].start_ms >= min_start_ms - 300]
    used_fallback = False
    if not chronological:
        chronological = candidates
        used_fallback = True

    chronological.sort(
        key=lambda item: (-item[4], item[0].start_ms, -len(match_core(item[0].ja_text)), item[0].subtitle_index)
    )

    video_duration_ms = max((row.end_ms for row in subtitles), default=0)
    recap_cutoff_ms = int(video_duration_ms * 0.55) if video_duration_ms else 0
    early_pool = [c for c in chronological if c[0].start_ms < recap_cutoff_ms]
    late_pool = [c for c in chronological if c[0].start_ms >= recap_cutoff_ms]
    if early_pool and late_pool:
        best_early = max(early_pool, key=lambda item: item[4])
        best_late = max(late_pool, key=lambda item: item[4])
        if best_late[4] - best_early[4] <= 0.15:
            anchor_row, status, confidence, reason, _ratio = min(
                early_pool, key=lambda item: (-item[4], item[0].start_ms, item[0].subtitle_index)
            )
        else:
            anchor_row, status, confidence, reason, _ratio = chronological[0]
    else:
        anchor_row, status, confidence, reason, _ratio = chronological[0]
    start_idx, end_idx, expand_reason = expand_ass_range(subtitles, anchor_row.subtitle_index, sent_norm)
    if start_idx > end_idx:
        start_idx, end_idx = end_idx, start_idx
    start_ms, end_ms = _row_span(subtitles, start_idx, end_idx)
    rows_by_idx = {row.subtitle_index: row for row in subtitles}
    start_row = rows_by_idx[start_idx]
    end_row = rows_by_idx[end_idx]
    sorted_rows = sort_subtitles_chronological(subtitles)
    start_pos = next(i for i, row in enumerate(sorted_rows) if row.subtitle_index == start_idx)
    end_pos = next(i for i, row in enumerate(sorted_rows) if row.subtitle_index == end_idx)
    lo_pos, hi_pos = min(start_pos, end_pos), max(start_pos, end_pos)
    matched_ja = "".join(normalize_for_match(sorted_rows[i].ja_text) for i in range(lo_pos, hi_pos + 1))
    combo = combined_norm_rows_chrono(sorted_rows, lo_pos, hi_pos)

    suspicious: list[str] = []
    if len(candidates) > 1 and reason not in {"exact_match", "core_substring"}:
        suspicious.append("duplicate_candidates")
    if used_fallback:
        suspicious.append("chronological_fallback")
    if confidence == "medium" and "similarity" in reason:
        suspicious.append("low_confidence")
    if best_effort:
        suspicious.append("best_effort_match")
    if confidence == "low":
        suspicious.append("low_confidence")
    if start_idx != end_idx:
        suspicious.append("multi_cue_merge")
    if sent_norm not in combo and combo not in sent_norm:
        ratio = SequenceMatcher(None, sent_norm, combo).ratio()
        if ratio < 0.92:
            suspicious.append("text_coverage_gap")
    duration_ms = end_ms - start_ms
    if duration_ms > 30000:
        suspicious.append("clip_too_long")
    if duration_ms < 800:
        suspicious.append("clip_too_short")

    full_reason = f"{reason};{expand_reason}" if expand_reason else reason
    if best_effort and candidates:
        clip_eligible = status == "matched"
    else:
        clip_eligible = status == "matched" and confidence in {"high", "medium"}

    return MatchResult(
        anchor_row.subtitle_index,
        start_ms,
        end_ms,
        end_row.zh_text or anchor_row.zh_text,
        status,
        confidence,
        full_reason,
        clip_eligible,
        matched_ja_text=matched_ja,
        candidate_count=len(candidates),
        used_chronological_fallback=used_fallback,
        suspicious_flags=tuple(suspicious),
    )


def match_sentence(sentence: LearningSentence, subtitles: list[SubtitleRow]) -> MatchResult:
    sent_norm = normalize_for_match(sentence.ja_text)
    if not sent_norm:
        return MatchResult(
            None, None, None, "", "unmatched", "none", "empty_sentence", False
        )

    row = exact_match(sent_norm, subtitles)
    if row:
        return MatchResult(
            row.subtitle_index,
            row.start_ms,
            row.end_ms,
            row.zh_text,
            "matched",
            "high",
            "exact_match",
            True,
        )

    row, sub_reason = substring_match(sent_norm, subtitles)
    if row:
        return MatchResult(
            row.subtitle_index,
            row.start_ms,
            row.end_ms,
            row.zh_text,
            "matched",
            "medium",
            f"substring_match:{sub_reason}",
            True,
        )

    row, ratio = similarity_match(sent_norm, subtitles)
    if row and ratio >= 0.90:
        return MatchResult(
            row.subtitle_index,
            row.start_ms,
            row.end_ms,
            row.zh_text,
            "matched",
            "high",
            f"similarity:{ratio:.3f}",
            True,
        )
    if row and ratio >= 0.78:
        return MatchResult(
            row.subtitle_index,
            row.start_ms,
            row.end_ms,
            row.zh_text,
            "skipped",
            "medium",
            f"similarity_low:{ratio:.3f}",
            False,
        )

    return MatchResult(
        row.subtitle_index if row else None,
        row.start_ms if row else None,
        row.end_ms if row else None,
        row.zh_text if row else "",
        "unmatched",
        "none",
        f"similarity:{ratio:.3f}" if row else "no_candidate",
        False,
    )


def build_manifest_rows(
    episode_id: str,
    sentences: list[LearningSentence],
    subtitles: list[SubtitleRow],
    pre_roll_ms: int,
    post_roll_ms: int,
    video_duration_ms: int | None,
) -> list[dict]:
    manifest: list[dict] = []
    for sent in sentences:
        m = match_sentence(sent, subtitles)
        zh_text = sent.zh_text or m.matched_zh_text
        clip_start = clip_end = clip_duration = ""
        clip_path = ""

        if m.subtitle_start_ms is not None and m.subtitle_end_ms is not None:
            clip_start_ms = max(0, m.subtitle_start_ms - pre_roll_ms)
            clip_end_ms = m.subtitle_end_ms + post_roll_ms
            if video_duration_ms is not None:
                clip_end_ms = min(clip_end_ms, video_duration_ms)
            if clip_end_ms > clip_start_ms:
                clip_start = clip_start_ms
                clip_end = clip_end_ms
                clip_duration = clip_end_ms - clip_start_ms

        if m.clip_eligible and clip_start != "":
            clip_path = f"clips/{episode_id}_sentence_{sent.sort_order:03d}.mp4"

        manifest.append(
            {
                "sentence_id": sent.sentence_id,
                "episode_id": episode_id,
                "sort_order": sent.sort_order,
                "ja_text": sent.ja_text,
                "zh_text": zh_text,
                "subtitle_start_ms": m.subtitle_start_ms if m.subtitle_start_ms is not None else "",
                "subtitle_end_ms": m.subtitle_end_ms if m.subtitle_end_ms is not None else "",
                "clip_start_ms": clip_start,
                "clip_end_ms": clip_end,
                "clip_duration_ms": clip_duration,
                "clip_path": clip_path if m.clip_eligible else "",
                "match_status": m.match_status,
                "match_confidence": m.match_confidence,
                "match_reason": m.match_reason,
            }
        )
    return manifest
