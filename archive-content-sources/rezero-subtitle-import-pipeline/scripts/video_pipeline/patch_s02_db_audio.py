#!/usr/bin/env python3
"""One-shot: unify S2 episode_id + write audio_url/storage_path (uses SUPABASE_ACCESS_TOKEN)."""

from __future__ import annotations

import json
import os
import sys
import urllib.request
from pathlib import Path

PROJECT_REF = "qoatvdvbuleamyzsaldp"
ROOT = Path(__file__).resolve().parents[2]
SENTENCES_OUT = ROOT / "output" / "audio" / "re-zero" / "sentences_s02e26-e50.json"

PATCH_SQL = """
SET session_replication_role = replica;

UPDATE public.learning_sentences
SET episode_id = 're-zero-s02e' || lpad((episode - 25)::text, 2, '0')
WHERE work_slug = 're-zero' AND episode BETWEEN 26 AND 50;

UPDATE public.learning_exercises
SET episode_id = 're-zero-s02e' || lpad((episode - 25)::text, 2, '0')
WHERE work_slug = 're-zero' AND episode BETWEEN 26 AND 50;

UPDATE public.learning_grammar_points
SET episode_id = 're-zero-s02e' || lpad((episode - 25)::text, 2, '0')
WHERE work_slug = 're-zero' AND episode BETWEEN 26 AND 50;

UPDATE public.learning_vocab_occurrences
SET episode_id = 're-zero-s02e' || lpad((episode - 25)::text, 2, '0')
WHERE work_slug = 're-zero' AND episode BETWEEN 26 AND 50;

UPDATE public.episode_learning_plans
SET episode_id = 're-zero-s02e' || lpad((episode - 25)::text, 2, '0')
WHERE work_slug = 're-zero' AND episode BETWEEN 26 AND 50;

UPDATE public.episodes
SET id = 're-zero-s02e' || lpad((episode - 25)::text, 2, '0')
WHERE work_slug = 're-zero' AND episode BETWEEN 26 AND 50;

UPDATE public.learning_sentences
SET
  storage_path = 'rezeroS2/s02e' || lpad((episode - 25)::text, 2, '0') || '/' || id || '.mp3',
  audio_url = 'https://cdn.xn--cckl9nsb.com/rezeroS2/s02e' || lpad((episode - 25)::text, 2, '0') || '/' || id || '.mp3',
  updated_at = now()
WHERE work_slug = 're-zero'
  AND episode BETWEEN 26 AND 50
  AND recommended_shadowing = true
  AND id LIKE 're-zero-s02e%';

SET session_replication_role = DEFAULT;
"""

EXPORT_SQL = """
SELECT id, episode_id, episode, sort_order, ja_text, source_line_no, audio_url, storage_path
FROM public.learning_sentences
WHERE work_slug = 're-zero'
  AND episode BETWEEN 26 AND 50
  AND recommended_shadowing = true
ORDER BY episode, sort_order, id;
"""

VERIFY_SQL = """
SELECT
  (SELECT count(*) FROM learning_sentences WHERE work_slug='re-zero' AND episode BETWEEN 26 AND 50
     AND recommended_shadowing AND episode_id LIKE 're-zero-s02e%') AS episode_id_ok,
  (SELECT count(*) FROM learning_sentences WHERE work_slug='re-zero' AND episode BETWEEN 26 AND 50
     AND recommended_shadowing AND audio_url IS NOT NULL) AS audio_url_ok,
  (SELECT count(*) FROM episodes WHERE work_slug='re-zero' AND episode BETWEEN 26 AND 50
     AND id LIKE 're-zero-s02e%') AS episodes_ok;
"""


def run_sql(token: str, query: str):
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
    with urllib.request.urlopen(req, timeout=300) as resp:
        body = resp.read().decode()
        if not body.strip():
            return None
        return json.loads(body)


def main() -> int:
    token = os.environ.get("SUPABASE_ACCESS_TOKEN", "").strip()
    if not token:
        sys.exit("Set SUPABASE_ACCESS_TOKEN")

    print("Applying S2 episode_id + audio_url patch...")
    run_sql(token, PATCH_SQL)

    print("Verifying...")
    verify = run_sql(token, VERIFY_SQL)
    print(json.dumps(verify, ensure_ascii=False, indent=2))

    print("Exporting sentences JSON...")
    rows = run_sql(token, EXPORT_SQL) or []
    SENTENCES_OUT.parent.mkdir(parents=True, exist_ok=True)
    SENTENCES_OUT.write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {len(rows)} rows -> {SENTENCES_OUT}")

    sample = run_sql(
        token,
        "SELECT id, episode_id, audio_url FROM learning_sentences "
        "WHERE id IN ('re-zero-s02e15-sentence-001','re-zero-s02e15-sentence-165');",
    )
    print("Sample:", json.dumps(sample, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
