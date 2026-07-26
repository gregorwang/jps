import type { PronunciationEvaluation } from "../contracts";
import { isPronunciationEvaluation } from "../contracts";

interface AttemptRow {
  attempt_id: string;
  user_hash: string;
  sentence_id: string;
  response_json: string;
}

export type CachedAttempt =
  | { status: "miss" }
  | { status: "conflict" }
  | { status: "hit"; evaluation: PronunciationEvaluation };

export async function findCachedAttempt(
  db: D1Database,
  attemptId: string,
  userHash: string,
  sentenceId: string,
): Promise<CachedAttempt> {
  const row = await db
    .prepare("SELECT attempt_id, user_hash, sentence_id, response_json FROM attempts WHERE attempt_id = ?1 AND expires_at > unixepoch()")
    .bind(attemptId)
    .first<AttemptRow>();
  if (row === null) return { status: "miss" };
  if (row.user_hash !== userHash || row.sentence_id !== sentenceId) return { status: "conflict" };
  let parsed: unknown;
  try {
    parsed = JSON.parse(row.response_json);
  } catch {
    return { status: "miss" };
  }
  return isPronunciationEvaluation(parsed) ? { status: "hit", evaluation: parsed } : { status: "miss" };
}

export async function saveAttempt(
  db: D1Database,
  evaluation: PronunciationEvaluation,
  userHash: string,
  ttlDays: number,
): Promise<void> {
  const overall = evaluation.score?.overall ?? null;
  await db
    .prepare(
      `INSERT INTO attempts (
        attempt_id, user_hash, sentence_id, assessment_status, overall_score,
        scorer_version, response_json, created_at, expires_at
      ) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, unixepoch(), unixepoch() + ?8)
      ON CONFLICT(attempt_id) DO NOTHING`,
    )
    .bind(
      evaluation.attemptId,
      userHash,
      evaluation.sentenceId,
      evaluation.assessmentStatus,
      overall,
      evaluation.scorerVersion,
      JSON.stringify(evaluation),
      ttlDays * 86_400,
    )
    .run();
}

export async function cleanupExpiredAttempts(db: D1Database): Promise<number> {
  const result = await db.prepare("DELETE FROM attempts WHERE expires_at <= unixepoch()").run();
  return result.meta.changes;
}
