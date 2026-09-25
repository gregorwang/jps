#!/usr/bin/env python3
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
PREV_CSV = ROOT / "rezero_s1_ep06_ep20_subtitle_lines_import" / "zh_merge_review.csv"
REPORT = ROOT / "rezero_s1_ep06_ep20_subtitle_lines_import" / "zh_audit_full.csv"
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


def canonical(ja: Ja, cues: list[ZhCue]) -> Canon | None:
    best = None
    best_ov = -1
    for zh in cues:
        if not overlap(ja.start_ms, ja.end_ms, zh.start_ms, zh.end_ms, 800):
            continue
        o = ov_ms(ja, zh)
        if o > best_ov:
            best_ov = o
            best = Canon(zh.text, "overlap", abs(ja.mid_ms - zh.mid_ms))
    if best:
        return best
    span = None
    for zh in cues:
        if ja.mid_ms >= zh.start_ms - 1500 and ja.mid_ms <= zh.end_ms + 1500:
            d = abs(ja.mid_ms - zh.mid_ms)
            if span is None or d < span.delta_ms:
                span = Canon(zh.text, "span", d)
    if span and span.delta_ms <= 4500:
        return span
    if not is_dialogue(ja.ja_text):
        return None
    near = None
    for zh in cues:
        d = abs(ja.mid_ms - zh.mid_ms)
        if d <= 2800 and (near is None or d < near.delta_ms):
            near = Canon(zh.text, "nearest", d)
    return near


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


def load_prev() -> dict[tuple[int, int], str]:
    if not PREV_CSV.exists():
        return {}
    out = {}
    with PREV_CSV.open(encoding="utf-8-sig", newline="") as f:
        for r in csv.DictReader(f):
            if r.get("status") == "matched" and r.get("zh_text"):
                out[(int(r["episode"]), int(r["line_no"]))] = r["zh_text"]
    return out


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

    ja_lines = load_ja(args.from_ep, args.to_ep)
    prev = load_prev()
    audit: list[dict] = []
    final: list[tuple[int, int, str]] = []
    stats = {"kept": 0, "fixed": 0, "removed": 0, "added": 0}

    for ep in range(args.from_ep, args.to_ep + 1):
        ass = sorted(S01.glob(f"*S01E{ep:02d}*.SC.ass"))
        if not ass:
            print(f"EP{ep:02d}: missing ass")
            continue
        cues = read_ass(ass[0])
        ep_ja = [j for j in ja_lines if j.episode == ep]
        for ja in ep_ja:
            c = canonical(ja, cues)
            new_zh = c.text if c else ""
            old = prev.get((ja.episode, ja.line_no), "")
            if new_zh:
                final.append((ja.episode, ja.line_no, new_zh))
                if not old:
                    stats["added"] += 1
                    act = "added"
                elif old == new_zh:
                    stats["kept"] += 1
                    act = "kept"
                else:
                    stats["fixed"] += 1
                    act = "fixed"
            else:
                act = "removed_wrong" if old else "empty"
                if old:
                    stats["removed"] += 1
            audit.append(
                {
                    "episode": ja.episode,
                    "line_no": ja.line_no,
                    "action": act,
                    "method": c.method if c else "",
                    "delta_ms": c.delta_ms if c else "",
                    "ja_text": ja.ja_text,
                    "prev_zh": old,
                    "audit_zh": new_zh,
                    "is_dialogue": "yes" if is_dialogue(ja.ja_text) else "no",
                }
            )
        ep_a = [a for a in audit if a["episode"] == ep]
        dlg = [a for a in ep_a if a["is_dialogue"] == "yes"]
        print(
            f"EP{ep:02d}: audit {sum(1 for a in ep_a if a['audit_zh'])}/{len(ep_ja)}, "
            f"dialogue {sum(1 for d in dlg if d['audit_zh'])}/{len(dlg)}, "
            f"removed={sum(1 for a in ep_a if a['action'] == 'removed_wrong')}"
        )

    with REPORT.open("w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(audit[0].keys()))
        w.writeheader()
        w.writerows(audit)
    print(f"Report: {REPORT}")
    print(
        f"Summary kept={stats['kept']} fixed={stats['fixed']} removed={stats['removed']} "
        f"added={stats['added']} final={len(final)}/{len(ja_lines)}"
    )
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
