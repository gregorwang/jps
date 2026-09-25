#!/usr/bin/env python3
"""Merge S01 SC.ass zh into re-zero subtitle_lines with multi-pass 1:1 alignment."""

from __future__ import annotations

import argparse
import csv
import json
import re
import sys
import urllib.request
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts" / "video_pipeline"))

from ass_parser import (  # noqa: E402
    RawCue,
    cues_overlap,
    detect_line_lang,
    parse_ass_cues,
    style_lang_hint,
)

PROJECT_REF = "qoatvdvbuleamyzsaldp"
S01_DIR = ROOT / "S01"
JA_CSV = ROOT / "rezero_s1_ep06_ep20_subtitle_lines_import" / "subtitle_lines.csv"
REPORT_CSV = ROOT / "rezero_s1_ep06_ep20_subtitle_lines_import" / "zh_merge_review.csv"
SKIP_STYLES = {"Staff", "Comment", "Title"}
JP_STYLES = {"OPJP", "EDJP", "OPJ", "EDJ"}
RE_KANA = re.compile(r"[\u3040-\u309f\u30a0-\u30ff]")
RE_SFX = re.compile(r"^[\s（(][^）)]*[）)]\s*$|^[～~♪\.…・]+$")


@dataclass
class JaLine:
    episode: int
    line_no: int
    start_ms: int
    end_ms: int
    mid_ms: int
    ja_text: str
    usable: bool


@dataclass
class Match:
    episode: int
    line_no: int
    zh_text: str
    confidence: str
    delta_ms: int


def srt_time_to_ms(t: str) -> int:
    t = t.strip().replace(",", ".")
    parts = t.split(":")
    if len(parts) == 3:
        h, m, s = parts
        return int(round((int(h) * 3600 + int(m) * 60 + float(s)) * 1000))
    if len(parts) == 2:
        m, s = parts
        return int(round((int(m) * 60 + float(s)) * 1000))
    return 0


def load_ja_lines(ep_from: int, ep_to: int) -> list[JaLine]:
    rows: list[JaLine] = []
    with JA_CSV.open(encoding="utf-8-sig", newline="") as f:
        for r in csv.DictReader(f):
            ep = int(r["episode"])
            if ep < ep_from or ep > ep_to:
                continue
            start = srt_time_to_ms(r["start_time"])
            end = srt_time_to_ms(r["end_time"])
            rows.append(
                JaLine(
                    episode=ep,
                    line_no=int(r["line_no"]),
                    start_ms=start,
                    end_ms=end,
                    mid_ms=(start + end) // 2,
                    ja_text=r["ja_text"],
                    usable=r.get("usable_for_analysis", "true") == "true",
                )
            )
    return rows


def find_sc_ass(episode: int) -> Path | None:
    hits = sorted(S01_DIR.glob(f"*S01E{episode:02d}*.SC.ass"))
    return hits[0] if hits else None


def extract_zh_cues(ass_path: Path) -> list[RawCue]:
    zh: list[RawCue] = []
    for cue in parse_ass_cues(ass_path):
        style = cue.style.strip()
        if style in SKIP_STYLES or style in JP_STYLES:
            continue
        hint = style_lang_hint(style)
        lang = detect_line_lang(cue.text)
        if hint == "ja" or (lang == "ja" and hint != "zh"):
            continue
        if hint == "zh" or style in {"Default", "OPCN", "EDCN"} or lang == "zh":
            zh.append(cue)
    zh.sort(key=lambda c: (c.start_ms, c.end_ms))
    return zh


def is_likely_dialogue(ja: JaLine) -> bool:
    t = ja.ja_text.strip()
    if not t or t in {"～♪", "~♪"}:
        return False
    if RE_SFX.match(t) and not RE_KANA.search(t.replace("（", "").replace("）", "")):
        return False
    return bool(RE_KANA.search(t) or len(t) >= 4)


def overlap_ms(a: JaLine, z: RawCue) -> int:
    return min(a.end_ms, z.end_ms) - max(a.start_ms, z.start_ms)


def match_episode(ep_ja: list[JaLine], zh_cues: list[RawCue]) -> tuple[list[Match], list[JaLine]]:
    """One zh cue may cover multiple Netflix ja lines (split cues)."""
    matched_nos: set[int] = set()
    matches: list[Match] = []

    # Pass 1: span coverage — every ja whose midpoint falls inside a zh cue window
    for zh in zh_cues:
        zmid = (zh.start_ms + zh.end_ms) // 2
        zstart = zh.start_ms - 1200
        zend = zh.end_ms + 1200
        for ja in ep_ja:
            if ja.line_no in matched_nos:
                continue
            if ja.mid_ms < zstart or ja.mid_ms > zend:
                if not cues_overlap(ja.start_ms, ja.end_ms, zh.start_ms, zh.end_ms, slack_ms=1500):
                    continue
            delta = abs(ja.mid_ms - zmid)
            if delta <= 6000 or cues_overlap(ja.start_ms, ja.end_ms, zh.start_ms, zh.end_ms, slack_ms=1500):
                matches.append(Match(ja.episode, ja.line_no, zh.text, "span", delta))
                matched_nos.add(ja.line_no)

    # Pass 2: nearest zh for remaining dialogue (reuse zh allowed)
    for ja in sorted(ep_ja, key=lambda x: (x.start_ms, x.line_no)):
        if ja.line_no in matched_nos:
            continue
        if not is_likely_dialogue(ja):
            continue
        best: RawCue | None = None
        best_dist = 10**9
        for zh in zh_cues:
            zmid = (zh.start_ms + zh.end_ms) // 2
            dist = abs(ja.mid_ms - zmid)
            if dist <= 7000 and dist < best_dist:
                best_dist = dist
                best = zh
        if best is not None:
            matches.append(Match(ja.episode, ja.line_no, best.text, "nearest", best_dist))
            matched_nos.add(ja.line_no)

    # Pass 3: walk remaining dialogue + unused zh slots by order (recut drift)
    rem_ja = [j for j in sorted(ep_ja, key=lambda x: x.start_ms) if j.line_no not in matched_nos and is_likely_dialogue(j)]
    if rem_ja:
        for ja in rem_ja:
            best: RawCue | None = None
            best_dist = 10**9
            for zh in zh_cues:
                zmid = (zh.start_ms + zh.end_ms) // 2
                dist = abs(ja.mid_ms - zmid)
                if dist <= 12000 and dist < best_dist:
                    best_dist = dist
                    best = zh
            if best is not None:
                matches.append(Match(ja.episode, ja.line_no, best.text, "sequence", best_dist))
                matched_nos.add(ja.line_no)

    unmatched = [j for j in ep_ja if j.line_no not in matched_nos]
    return matches, unmatched


def validate_match(m: Match, ja: JaLine) -> bool:
    """Drop low-confidence pairings that are clearly wrong."""
    if m.confidence in {"sequence", "nearest"} and m.delta_ms > 8500:
        return False
    if m.confidence == "span" and m.delta_ms > 10000:
        return False
    return True


def sql_literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def build_update_sql(matches: list[Match], batch_size: int = 150) -> list[str]:
    stmts: list[str] = []
    for i in range(0, len(matches), batch_size):
        batch = matches[i : i + batch_size]
        values = ",\n".join(
            f"({m.episode}, {m.line_no}, {sql_literal(m.zh_text)})" for m in batch
        )
        stmts.append(
            "UPDATE public.subtitle_lines AS s\n"
            "SET zh_text = v.zh_text,\n"
            "    source = CASE WHEN coalesce(s.ja_text, '') <> '' THEN 'bilingual_subtitle' ELSE s.source END\n"
            f"FROM (VALUES\n{values}\n) AS v(episode, line_no, zh_text)\n"
            "WHERE s.work_slug = 're-zero'\n"
            "  AND s.episode = v.episode\n"
            "  AND s.line_no = v.line_no;"
        )
    return stmts


def run_sql(token: str, query: str) -> str:
    req = urllib.request.Request(
        f"https://api.supabase.com/v1/projects/{PROJECT_REF}/database/query",
        data=json.dumps({"query": query}).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "User-Agent": "Mozilla/5.0",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        return resp.read().decode("utf-8")


def write_report(all_matches: list[Match], ja_by_key: dict[tuple[int, int], JaLine], unmatched: list[JaLine]) -> None:
    REPORT_CSV.parent.mkdir(parents=True, exist_ok=True)
    with REPORT_CSV.open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(["episode", "line_no", "confidence", "delta_ms", "ja_text", "zh_text", "status"])
        for m in sorted(all_matches, key=lambda x: (x.episode, x.line_no)):
            ja = ja_by_key[(m.episode, m.line_no)]
            w.writerow([m.episode, m.line_no, m.confidence, m.delta_ms, ja.ja_text, m.zh_text, "matched"])
        for ja in sorted(unmatched, key=lambda x: (x.episode, x.line_no)):
            status = "sfx_or_no_zh" if not is_likely_dialogue(ja) else "needs_review"
            w.writerow([ja.episode, ja.line_no, "", "", ja.ja_text, "", status])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--from-ep", type=int, default=6)
    parser.add_argument("--to-ep", type=int, default=20)
    parser.add_argument("--run", action="store_true")
    parser.add_argument("--token", default="")
    args = parser.parse_args()

    ja_lines = load_ja_lines(args.from_ep, args.to_ep)
    ja_by_key = {(j.episode, j.line_no): j for j in ja_lines}
    all_matches: list[Match] = []
    all_unmatched: list[JaLine] = []

    for ep in range(args.from_ep, args.to_ep + 1):
        ass_path = find_sc_ass(ep)
        if not ass_path:
            print(f"EP{ep:02d}: missing SC.ass", file=sys.stderr)
            continue
        ep_ja = [j for j in ja_lines if j.episode == ep]
        zh_cues = extract_zh_cues(ass_path)
        matches, unmatched = match_episode(ep_ja, zh_cues)
        matches = [m for m in matches if validate_match(m, ja_by_key[(m.episode, m.line_no)])]
        matched_nos = {m.line_no for m in matches}
        unmatched = [j for j in ep_ja if j.line_no not in matched_nos]
        all_matches.extend(matches)
        all_unmatched.extend(unmatched)
        dialogue = sum(1 for j in ep_ja if is_likely_dialogue(j))
        matched_dialogue = sum(1 for m in matches if is_likely_dialogue(ja_by_key[(m.episode, m.line_no)]))
        print(
            f"EP{ep:02d}: {len(matches)}/{len(ep_ja)} total, "
            f"dialogue {matched_dialogue}/{dialogue}, zh_cues={len(zh_cues)}, review={sum(1 for u in unmatched if is_likely_dialogue(u))}"
        )

    write_report(all_matches, ja_by_key, all_unmatched)
    print(f"Report: {REPORT_CSV}")

    needs_review = [u for u in all_unmatched if is_likely_dialogue(u)]
    print(f"Total matched: {len(all_matches)}/{len(ja_lines)}")
    print(f"Dialogue still unmatched: {len(needs_review)}")

    if not args.run:
        print(f"Dry run: would apply {len(all_matches)} updates")
        return

    token = args.token or __import__("os").environ.get("SUPABASE_ACCESS_TOKEN", "")
    if not token:
        print("Missing token", file=sys.stderr)
        sys.exit(1)

    ep_list = ",".join(str(ep) for ep in range(args.from_ep, args.to_ep + 1))
    stmts = [
        f"UPDATE public.subtitle_lines SET zh_text = NULL "
        f"WHERE work_slug = 're-zero' AND episode BETWEEN {args.from_ep} AND {args.to_ep};",
    ]
    stmts.extend(build_update_sql(all_matches))
    stmts.append(
        f"UPDATE public.episodes e SET zh_lines = sub.cnt FROM ("
        f"SELECT episode, count(*)::int AS cnt FROM public.subtitle_lines "
        f"WHERE work_slug = 're-zero' AND episode IN ({ep_list}) AND coalesce(zh_text, '') <> '' GROUP BY episode"
        f") sub WHERE e.work_slug = 're-zero' AND e.episode = sub.episode;"
    )
    stmts.append(
        f"SELECT episode, count(*)::int AS total, count(*) FILTER (WHERE coalesce(zh_text,'') <> '')::int AS with_zh "
        f"FROM public.subtitle_lines WHERE work_slug = 're-zero' AND episode BETWEEN {args.from_ep} AND {args.to_ep} "
        f"GROUP BY episode ORDER BY episode;"
    )

    for i, sql in enumerate(stmts, 1):
        print(f"[{i}/{len(stmts)}] running...", flush=True)
        result = run_sql(token, sql)
        if sql.startswith("SELECT"):
            print(result)


if __name__ == "__main__":
    main()
