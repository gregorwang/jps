#!/usr/bin/env python3
"""Upload Re:Zero S3 shadowing mp3 files to Cloudflare R2 (gregorwang bucket)."""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_AUDIO_ROOT = ROOT / "output" / "audio" / "re-zero"
BUCKET = "gregorwang"
CDN_BASE = "https://cdn.xn--cckl9nsb.com"
R2_PREFIX = "rezeroS3"


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
    p = argparse.ArgumentParser(description="Upload S3 shadowing audio to R2")
    p.add_argument("--audio-root", default=str(DEFAULT_AUDIO_ROOT))
    p.add_argument("--from-ep", type=int, default=1)
    p.add_argument("--to-ep", type=int, default=16)
    p.add_argument("--dry-run", action="store_true")
    args = p.parse_args()

    wrangler = find_wrangler()
    audio_root = Path(args.audio_root)
    urls: list[dict] = []
    ok_n = 0
    fail_n = 0

    for ep in range(args.from_ep, args.to_ep + 1):
        ep_dir = audio_root / f"s03e{ep:02d}"
        if not ep_dir.is_dir():
            print(f"[SKIP] missing {ep_dir}")
            continue
        mp3s = sorted(ep_dir.glob("*.mp3"))
        print(f"\n=== s03e{ep:02d} ({len(mp3s)} files) ===")
        for mp3 in mp3s:
            r2_key = f"{R2_PREFIX}/s03e{ep:02d}/{mp3.name}"
            ok, err = upload_file(wrangler, mp3, r2_key, dry_run=args.dry_run)
            cdn = f"{CDN_BASE}/{r2_key}"
            urls.append({"sentence_id": mp3.stem, "episode": f"s03e{ep:02d}", "r2_key": r2_key, "cdn_url": cdn, "ok": ok})
            if ok:
                ok_n += 1
                print(f"  ok {mp3.name}")
            else:
                fail_n += 1
                print(f"  FAIL {mp3.name}: {err}")

    out = audio_root / "r2_urls_s03.json"
    out.write_text(json.dumps({"bucket": BUCKET, "prefix": R2_PREFIX, "cdn_base": CDN_BASE, "items": urls}, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nDone upload ok={ok_n} fail={fail_n}")
    print(f"URL list: {out}")
    return 0 if fail_n == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
