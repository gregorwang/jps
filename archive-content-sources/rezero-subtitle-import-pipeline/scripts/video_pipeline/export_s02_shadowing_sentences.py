#!/usr/bin/env python3
"""Export Re:Zero S2 shadowing sentences JSON for the clip pipeline (S1-style IDs)."""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.request
from collections import defaultdict
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parents[1]
PROJECT_REF = "qoatvdvbuleamyzsaldp"
DEFAULT_OUT = ROOT / "output" / "audio" / "re-zero" / "sentences_s02e26-e50.json"
DEFAULT_BACKUP = (
    ROOT
    / "backups"
    / "rezero_s02e26_e50_before_import_20260626T053651Z"
    / "learning_sentences.json"
)


def db_episode_to_s2(episode: int) -> int:
    return episode - 25


def episode_id_from_db(episode: int) -> str:
    return f"re-zero-s02e{db_episode_to_s2(episode):02d}"


def sentence_id_from_db(episode: int, seq: int) -> str:
    return f"{episode_id_from_db(episode)}-sentence-{seq:03d}"


def run_sql(token: str, query: str) -> list[dict]:
    req = urllib.request.Request(
        f"https://api.supabase.com/v1/projects/{PROJECT_REF}/database/query",
        data=json.dumps({"query": query}).encode(),
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "User-Agent": "Mozilla/5.0",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        return json.loads(resp.read().decode())


def load_from_supabase(ep_min: int, ep_max: int) -> list[dict]:
    token = os.environ.get("SUPABASE_ACCESS_TOKEN", "").strip()
    if not token:
        return []
    q = (
        "SELECT id, episode, sort_order, ja_text, source_line_no "
        f"FROM learning_sentences WHERE work_slug = 're-zero' "
        f"AND episode BETWEEN {ep_min} AND {ep_max} "
        "AND recommended_shadowing = true "
        "ORDER BY episode, sort_order, id"
    )
    return run_sql(token, q)


def load_from_backup(path: Path, ep_min: int, ep_max: int) -> list[dict]:
    rows = json.loads(path.read_text(encoding="utf-8"))
    return [
        r
        for r in rows
        if ep_min <= int(r["episode"]) <= ep_max and r.get("recommended_shadowing")
    ]


def build_export(rows: list[dict]) -> list[dict]:
    by_ep: dict[int, list[dict]] = defaultdict(list)
    for row in rows:
        by_ep[int(row["episode"])].append(row)

    out: list[dict] = []
    for episode in sorted(by_ep):
        items = sorted(by_ep[episode], key=lambda r: (int(r["sort_order"]), str(r["id"])))
        ep_id = episode_id_from_db(episode)
        for seq, row in enumerate(items, start=1):
            out.append(
                {
                    "id": sentence_id_from_db(episode, seq),
                    "episode_id": ep_id,
                    "db_id": row["id"],
                    "episode": episode,
                    "sort_order": int(row["sort_order"]),
                    "ja_text": row["ja_text"],
                    "source_line_no": int(row["source_line_no"]) if row.get("source_line_no") else None,
                }
            )
    return out


def main() -> int:
    p = argparse.ArgumentParser(description="Export S2 shadowing sentences JSON (S1-style naming)")
    p.add_argument("--from-ep", type=int, default=26, help="DB episode min (26=S2E01)")
    p.add_argument("--to-ep", type=int, default=50, help="DB episode max (50=S2E25)")
    p.add_argument("--out", default=str(DEFAULT_OUT))
    p.add_argument("--backup", default=str(DEFAULT_BACKUP))
    args = p.parse_args()

    rows = load_from_supabase(args.from_ep, args.to_ep)
    source = "supabase"
    if not rows:
        backup = Path(args.backup)
        if not backup.exists():
            sys.exit("No SUPABASE_ACCESS_TOKEN and no backup JSON found")
        rows = load_from_backup(backup, args.from_ep, args.to_ep)
        source = f"backup:{backup.name}"

    exported = build_export(rows)
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(exported, ensure_ascii=False, indent=2), encoding="utf-8")

    by_ep = defaultdict(int)
    for item in exported:
        by_ep[item["episode"]] += 1
    print(f"source={source} episodes={len(by_ep)} sentences={len(exported)} -> {out_path}")
    for ep in sorted(by_ep):
        print(f"  DB ep{ep} (s02e{db_episode_to_s2(ep):02d}): {by_ep[ep]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
