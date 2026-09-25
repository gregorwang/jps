#!/usr/bin/env python3
"""Episode-by-episode manual pass: fill remaining empty dialogue from ASS only."""
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
S01 = ROOT / "S01"
JA_CSV = ROOT / "rezero_s1_ep06_ep20_subtitle_lines_import" / "subtitle_lines.csv"
AUDIT_CSV = ROOT / "rezero_s1_ep06_ep20_subtitle_lines_import" / "zh_audit_full.csv"
MANUAL_CSV = ROOT / "rezero_s1_ep06_ep20_subtitle_lines_import" / "zh_manual_final.csv"
PROJECT_REF = "qoatvdvbuleamyzsaldp"
RE_KANA = re.compile(r"[\u3040-\u309f\u30a0-\u30ff]")
SKIP = {"Staff", "Comment", "Title", "OPJP", "EDJP", "OPJ", "EDJ"}
ZH_STYLES = {"Default", "OPCN", "EDCN"}


@dataclass
class Ja:
    episode: int
    line_no: int
    start_ms: int
    end_ms: int
    mid_ms: int
    ja_text: str


@dataclass
class ZhCue:
    start_ms: int
    end_ms: int
    mid_ms: int
    text: str


@dataclass
class Canon:
    text: str
    method: str
    delta_ms: int


def srt_ms(t: str) -> int:
    t = t.strip().replace(",", ".")
    p = t.split(":")
    if len(p) == 3:
        return int(round((int(p[0]) * 3600 + int(p[1]) * 60 + float(p[2])) * 1000))
    return int(round((int(p[0]) * 60 + float(p[1])) * 1000))


def ass_ms(t: str) -> int:
    t = t.strip().replace(",", ".")
    p = t.split(":")
    return int(round((int(p[0]) * 3600 + int(p[1]) * 60 + float(p[2])) * 1000))


def clean_ass(s: str) -> str:
    s = re.sub(r"\{[^}]*\}", "", s)
    return s.replace("\\N", "\n").replace("\\n", "\n").replace("\\h", " ").strip()


def read_ass(path: Path) -> list[ZhCue]:
    raw = path.read_text(encoding="utf-8-sig", errors="replace")
    cues: list[ZhCue] = []
    in_events = False
    for line in raw.splitlines():
        s = line.strip()
        if s == "[Events]":
            in_events = True
            continue
        if not in_events or not s.startswith("Dialogue:"):
            continue
        body = s[len("Dialogue:") :].strip()
        parts = body.split(",")
        if len(parts) < 10:
            continue
        style = parts[3].strip()
        text = clean_ass(",".join(parts[9:]))
        if not text or style in SKIP:
            continue
        if style not in ZH_STYLES and not re.search(r"[\u4e00-\u9fff]", text):
            continue
        st, en = ass_ms(parts[1]), ass_ms(parts[2])
        cues.append(ZhCue(st, en, (st + en) // 2, text))
    cues.sort(key=lambda c: (c.start_ms, c.end_ms))
    return cues


def overlap(a0, a1, b0, b1, slack=0):
    return not (a1 + slack < b0 or b1 + slack < a0)


def ov_ms(ja: Ja, zh: ZhCue) -> int:
    return min(ja.end_ms, zh.end_ms) - max(ja.start_ms, zh.start_ms)


def is_dialogue(text: str) -> bool:
    t = text.strip()
    if not t or t in {"～♪", "~♪"}:
        return False
    if re.match(r"^[（(][^）)]*[）)]\s*$", t) and not RE_KANA.search(t.replace("（", "").replace("）", "")):
        return False
    return bool(RE_KANA.search(t) or len(t) >= 4)


def in_ass_hole(mid_ms: int, cues: list[ZhCue], min_gap: int = 3500) -> bool:
    for i in range(len(cues) - 1):
        a, b = cues[i], cues[i + 1]
        if a.end_ms < mid_ms < b.start_ms and (b.start_ms - a.end_ms) >= min_gap:
            return True
    return False


def after_ass_end(ja: Ja, cues: list[ZhCue], pad_ms: int = 2500) -> bool:
    if not cues:
        return True
    return ja.start_ms > cues[-1].end_ms + pad_ms


def is_scream_only(text: str) -> bool:
    t = re.sub(r"[（(][^）)]*[）)]", "", text).strip()
    if not t:
        return True
    if re.fullmatch(r"[あアぁァうウえエおオー～~♪.…！!？?\s]+", t):
        return True
    if len(t) <= 8 and not re.search(r"[\u4e00-\u9fff\u3040-\u30ff]{3,}", t):
        return True
    return False


def zh_cue_for_ja(ja: Ja, cues: list[ZhCue]) -> ZhCue | None:
    best = None
    best_ov = -1
    for zh in cues:
        if not overlap(ja.start_ms, ja.end_ms, zh.start_ms, zh.end_ms, 1200):
            continue
        o = ov_ms(ja, zh)
        if o > best_ov:
            best_ov = o
            best = zh
    if best:
        return best
    for zh in cues:
        if ja.mid_ms >= zh.start_ms - 800 and ja.mid_ms <= zh.end_ms + 800:
            d = abs(ja.mid_ms - zh.mid_ms)
            if best is None or d < abs(ja.mid_ms - best.mid_ms):
                best = zh
    return best


def canonical_relaxed(ja: Ja, cues: list[ZhCue]) -> Canon | None:
    if after_ass_end(ja, cues) or in_ass_hole(ja.mid_ms, cues) or is_scream_only(ja.ja_text):
        return None
    best = None
    best_ov = -1
    for zh in cues:
        if not overlap(ja.start_ms, ja.end_ms, zh.start_ms, zh.end_ms, 1200):
            continue
        o = ov_ms(ja, zh)
        if o > best_ov:
            best_ov = o
            best = Canon(zh.text, "overlap_relaxed", abs(ja.mid_ms - zh.mid_ms))
    if best:
        return best
    span = None
    for zh in cues:
        if ja.mid_ms >= zh.start_ms - 1200 and ja.mid_ms <= zh.end_ms + 1200:
            d = abs(ja.mid_ms - zh.mid_ms)
            if span is None or d < span.delta_ms:
                span = Canon(zh.text, "span_relaxed", d)
    if span and span.delta_ms <= 3500:
        return span
    return None


def empty_reason(ja: Ja, cues: list[ZhCue]) -> str:
    if not is_dialogue(ja.ja_text):
        return "not_dialogue"
    if is_scream_only(ja.ja_text):
        return "scream_sfx"
    if after_ass_end(ja, cues):
        return "netflix_only_after_ass"
    if in_ass_hole(ja.mid_ms, cues):
        return "ass_time_hole"
    return "no_confident_match"


def load_ja(ep_from: int, ep_to: int) -> list[Ja]:
    rows = []
    with JA_CSV.open(encoding="utf-8-sig", newline="") as f:
        for r in csv.DictReader(f):
            ep = int(r["episode"])
            if ep < ep_from or ep > ep_to:
                continue
            st, en = srt_ms(r["start_time"]), srt_ms(r["end_time"])
            rows.append(Ja(ep, int(r["line_no"]), st, en, (st + en) // 2, r["ja_text"]))
    return rows


def load_audit() -> list[dict]:
    with AUDIT_CSV.open(encoding="utf-8-sig", newline="") as f:
        return list(csv.DictReader(f))


def manual_pass_ep(ep_ja: list[Ja], cues: list[ZhCue], zh_map: dict[tuple[int, int], str], meta: dict) -> int:
    added = 0
    by_no = {j.line_no: j for j in ep_ja}
    nos = sorted(by_no)

    for ln in nos:
        key = (by_no[ln].episode, ln)
        if zh_map.get(key):
            continue
        ja = by_no[ln]
        if not is_dialogue(ja.ja_text):
            continue
        c = canonical_relaxed(ja, cues)
        if c:
            zh_map[key] = c.text
            meta[key] = c.method
            added += 1

    for i, ln in enumerate(nos):
        key = (by_no[ln].episode, ln)
        if zh_map.get(key):
            continue
        ja = by_no[ln]
        if not is_dialogue(ja.ja_text) or is_scream_only(ja.ja_text):
            continue
        if after_ass_end(ja, cues) or in_ass_hole(ja.mid_ms, cues):
            continue
        prev_ja = next_ja = None
        prev_zh = next_zh = None
        for j in range(i - 1, -1, -1):
            p = nos[j]
            pk = (by_no[p].episode, p)
            if zh_map.get(pk):
                prev_ja, prev_zh = by_no[p], zh_map[pk]
                break
        for j in range(i + 1, len(nos)):
            n = nos[j]
            nk = (by_no[n].episode, n)
            if zh_map.get(nk):
                next_ja, next_zh = by_no[n], zh_map[nk]
                break
        if prev_zh and next_zh and prev_zh == next_zh and prev_ja and next_ja:
            pc = zh_cue_for_ja(prev_ja, cues)
            if pc and ja.mid_ms >= pc.start_ms - 800 and ja.mid_ms <= pc.end_ms + 800:
                zh_map[key] = prev_zh
                meta[key] = "split_cue"
                added += 1
    return added


def sql_lit(v: str) -> str:
    return "'" + v.replace("'", "''") + "'"


def run_sql(token: str, q: str) -> str:
    req = urllib.request.Request(
        f"https://api.supabase.com/v1/projects/{PROJECT_REF}/database/query",
        data=json.dumps({"query": q}).encode(),
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "User-Agent": "Mozilla/5.0",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        return resp.read().decode()


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--from-ep", type=int, default=6)
    ap.add_argument("--to-ep", type=int, default=20)
    ap.add_argument("--run", action="store_true")
    ap.add_argument("--token", default="")
    args = ap.parse_args()

    if not AUDIT_CSV.exists():
        sys.exit(f"missing {AUDIT_CSV}; run audit_rezero_zh.py first")

    audit_rows = load_audit()
    ja_lines = load_ja(args.from_ep, args.to_ep)
    ja_by_key = {(j.episode, j.line_no): j for j in ja_lines}

    zh_map: dict[tuple[int, int], str] = {}
    meta: dict[tuple[int, int], str] = {}
    for r in audit_rows:
        ep, ln = int(r["episode"]), int(r["line_no"])
        if ep < args.from_ep or ep > args.to_ep:
            continue
        zh = (r.get("audit_zh") or "").strip()
        if zh:
            zh_map[(ep, ln)] = zh
            meta[(ep, ln)] = r.get("method") or "audit"

    total_added = 0
    final_rows: list[dict] = []
    cues_cache: dict[int, list] = {}

    for ep in range(args.from_ep, args.to_ep + 1):
        ass = sorted(S01.glob(f"*S01E{ep:02d}*.SC.ass"))
        if not ass:
            print(f"EP{ep:02d}: missing ass")
            continue
        cues = read_ass(ass[0])
        cues_cache[ep] = cues
        ep_ja = [j for j in ja_lines if j.episode == ep]
        before = sum(1 for j in ep_ja if zh_map.get((j.episode, j.line_no)))
        added = manual_pass_ep(ep_ja, cues, zh_map, meta)
        total_added += added
        after = sum(1 for j in ep_ja if zh_map.get((j.episode, j.line_no)))
        dlg = [j for j in ep_ja if is_dialogue(j.ja_text)]
        dlg_with = sum(1 for j in dlg if zh_map.get((j.episode, j.line_no)))
        print(f"EP{ep:02d}: manual +{added}, zh {before}->{after}/{len(ep_ja)}, dialogue {dlg_with}/{len(dlg)}")

    for r in audit_rows:
        ep, ln = int(r["episode"]), int(r["line_no"])
        if ep < args.from_ep or ep > args.to_ep:
            continue
        key = (ep, ln)
        prev_zh = (r.get("audit_zh") or "").strip()
        new_zh = zh_map.get(key, "")
        if new_zh and not prev_zh:
            action = "manual_fill"
            method = meta.get(key, "manual")
        elif new_zh and prev_zh and new_zh != prev_zh:
            action = "manual_fix"
            method = meta.get(key, "manual")
        elif prev_zh and not new_zh:
            action = r.get("action", "empty")
            method = r.get("method", "")
        elif prev_zh:
            action = r.get("action", "kept")
            method = r.get("method", "")
        else:
            action = "empty_no_ass"
            method = ""
        row = dict(r)
        row["manual_zh"] = new_zh
        row["manual_action"] = action
        row["manual_method"] = method if new_zh and not prev_zh else (method if action.startswith("manual") else r.get("method", ""))
        ja = ja_by_key.get(key)
        if ja and not new_zh and r.get("is_dialogue") == "yes" and ep in cues_cache:
            row["empty_reason"] = empty_reason(ja, cues_cache[ep])
        else:
            row["empty_reason"] = ""
        final_rows.append(row)

    fields = list(final_rows[0].keys()) if final_rows else []
    with MANUAL_CSV.open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(final_rows)

    final = [(ep, ln, zh) for (ep, ln), zh in sorted(zh_map.items()) if args.from_ep <= ep <= args.to_ep]
    empty_dlg = sum(
        1
        for r in final_rows
        if r.get("is_dialogue") == "yes" and not (r.get("manual_zh") or "").strip()
    )
    print(f"Report: {MANUAL_CSV}")
    print(f"Manual added={total_added}, final with zh={len(final)}/{len(ja_lines)}, empty dialogue={empty_dlg}")

    if not args.run:
        print("Dry run")
        return

    token = args.token or __import__("os").environ.get("SUPABASE_ACCESS_TOKEN", "")
    if not token:
        sys.exit("missing token")

    stmts = [
        f"UPDATE public.subtitle_lines SET zh_text = NULL WHERE work_slug = 're-zero' "
        f"AND episode BETWEEN {args.from_ep} AND {args.to_ep};"
    ]
    for i in range(0, len(final), 150):
        batch = final[i : i + 150]
        vals = ",\n".join(f"({e},{ln},{sql_lit(zh)})" for e, ln, zh in batch)
        stmts.append(
            "UPDATE public.subtitle_lines AS s SET zh_text = v.zh_text, source = 'bilingual_subtitle' "
            f"FROM (VALUES {vals}) AS v(episode, line_no, zh_text) "
            "WHERE s.work_slug = 're-zero' AND s.episode = v.episode AND s.line_no = v.line_no;"
        )
    ep_list = ",".join(str(e) for e in range(args.from_ep, args.to_ep + 1))
    stmts.append(
        f"UPDATE public.episodes e SET zh_lines = sub.cnt FROM ("
        f"SELECT episode, count(*)::int AS cnt FROM public.subtitle_lines "
        f"WHERE work_slug = 're-zero' AND episode IN ({ep_list}) AND coalesce(zh_text, '') <> '' "
        f"GROUP BY episode) sub WHERE e.work_slug = 're-zero' AND e.episode = sub.episode;"
    )
    stmts.append(
        f"SELECT episode, count(*)::int AS total, count(*) FILTER (WHERE coalesce(zh_text, '') <> '')::int AS with_zh "
        f"FROM public.subtitle_lines WHERE work_slug = 're-zero' AND episode BETWEEN {args.from_ep} AND {args.to_ep} "
        f"GROUP BY episode ORDER BY episode;"
    )
    for i, q in enumerate(stmts, 1):
        print(f"[{i}/{len(stmts)}]...", flush=True)
        out = run_sql(token, q)
        if q.startswith("SELECT"):
            print(out)


if __name__ == "__main__":
    main()
