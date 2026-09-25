#!/usr/bin/env python3
"""Upload Re:Zero S2 shadowing mp3 files to Cloudflare R2 (gregorwang bucket)."""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_AUDIO_ROOT = ROOT / "output" / "audio" / "re-zero"
BUCKET = "gregorwang"
CDN_BASE = "https://cdn.xn--cckl9nsb.com"
R2_PREFIX = "rezeroS2"

VALID_NAME = re.compile(r"^re-zero-s02e\d{2}-sentence-\d{3}\.mp3$")


def find_wrangler() -> str:
    for name in ("wrangler.cmd", "wrangler"):
        p = shutil.which(name)
        if p:
            return p
    npm = Path.home() / "AppData" / "Roaming" / "npm" / "wrangler.cmd"
    if npm.exists():
        return str(npm)
    raise RuntimeError("wrangler not found")


def upload_file(wrangler: str, local: Path, r2_key: str, *, dry_run: bool) -> tuple[bool, str]:
    if dry_run:
        return True, "dry_run"
    cmd = [wrangler, "r2", "object", "put", f"{BUCKET}/{r2_key}", "--file", str(local.resolve()), "--remote"]
    proc = subprocess.run(cmd, capture_output=True, text=True, check=False)
    if proc.returncode != 0:
        return False, (proc.stderr or proc.stdout or "upload failed")[-500:]
    return True, ""


def main() -> int:
    p = argparse.ArgumentParser(description="Upload S2 shadowing audio to R2")
    p.add_argument("--audio-root", default=str(DEFAULT_AUDIO_ROOT))
    p.add_argument("--from-ep", type=int, default=1)
    p.add_argument("--to-ep", type=int, default=25)
    p.add_argument("--workers", type=int, default=8)
    p.add_argument("--dry-run", action="store_true")
    args = p.parse_args()

    wrangler = find_wrangler()
    audio_root = Path(args.audio_root)
    jobs: list[tuple[Path, str, str, str]] = []
    skip_n = 0

    for ep in range(args.from_ep, args.to_ep + 1):
        ep_dir = audio_root / f"s02e{ep:02d}"
        if not ep_dir.is_dir():
            print(f"[SKIP] missing {ep_dir}", flush=True)
            continue
        for mp3 in sorted(ep_dir.glob("*.mp3")):
            if not VALID_NAME.match(mp3.name):
                skip_n += 1
                print(f"SKIP bad name: {mp3.name}", flush=True)
                continue
            slug = f"s02e{ep:02d}"
            r2_key = f"{R2_PREFIX}/{slug}/{mp3.name}"
            jobs.append((mp3, slug, r2_key, mp3.stem))

    print(f"Uploading {len(jobs)} files with {args.workers} workers...", flush=True)
    urls: list[dict] = []
    ok_n = 0
    fail_n = 0

    def _run(job: tuple[Path, str, str, str]) -> dict:
        mp3, slug, r2_key, sid = job
        ok, err = upload_file(wrangler, mp3, r2_key, dry_run=args.dry_run)
        return {
            "sentence_id": sid,
            "episode": slug,
            "r2_key": r2_key,
            "cdn_url": f"{CDN_BASE}/{r2_key}",
            "ok": ok,
            "error": err,
        }

    with ThreadPoolExecutor(max_workers=max(1, args.workers)) as pool:
        futures = [pool.submit(_run, job) for job in jobs]
        for i, fut in enumerate(as_completed(futures), 1):
            row = fut.result()
            urls.append(row)
            if row["ok"]:
                ok_n += 1
            else:
                fail_n += 1
                print(f"FAIL {row['sentence_id']}: {row['error']}", flush=True)
            if i % 200 == 0 or i == len(jobs):
                print(f"  progress {i}/{len(jobs)} ok={ok_n} fail={fail_n}", flush=True)

    urls.sort(key=lambda x: (x["episode"], x["sentence_id"]))
    out = audio_root / "r2_urls_s02.json"
    out.write_text(
        json.dumps({"bucket": BUCKET, "prefix": R2_PREFIX, "cdn_base": CDN_BASE, "items": urls}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"\nDone upload ok={ok_n} fail={fail_n} skip_bad_name={skip_n}", flush=True)
    print(f"URL list: {out}", flush=True)
    return 0 if fail_n == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
