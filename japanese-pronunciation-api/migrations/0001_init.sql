CREATE TABLE IF NOT EXISTS scorer_versions (
  version TEXT PRIMARY KEY,
  description TEXT NOT NULL,
  created_at INTEGER NOT NULL DEFAULT (unixepoch())
);

INSERT OR IGNORE INTO scorer_versions (version, description)
VALUES ('ja-mora-v1.0.0', 'Nova-3 + conditional Whisper, deterministic Japanese mora scoring');

CREATE TABLE IF NOT EXISTS attempts (
  attempt_id TEXT PRIMARY KEY,
  user_hash TEXT NOT NULL,
  sentence_id TEXT NOT NULL,
  assessment_status TEXT NOT NULL CHECK (assessment_status IN ('scored', 'uncertain', 're_record')),
  overall_score INTEGER,
  scorer_version TEXT NOT NULL,
  response_json TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_attempts_expires_at ON attempts (expires_at);
CREATE INDEX IF NOT EXISTS idx_attempts_sentence_created ON attempts (sentence_id, created_at DESC);
