export type AssessmentStatus = "scored" | "uncertain" | "re_record";

export type SegmentStatus =
  | "correct"
  | "unclear"
  | "substitution"
  | "omission"
  | "extra"
  | "pause"
  | "unscored";

export type EvaluationMode = "pronunciation" | "shadowing";

export interface ReferenceAlias {
  surface: string;
  reading: string;
}

export interface ReferenceToken {
  surface: string;
  reading: string;
  morae: string[];
  pos?: string;
  aliases?: ReferenceAlias[];
}

export interface NativeTimingReference {
  durationMs: number;
  speechStartMs: number;
  speechEndMs: number;
  pauseBoundaries: number[];
  source?: "native" | "subtitle" | "estimated";
}

export interface SentenceReference {
  sentenceId: string;
  text: string;
  displayReading: string;
  phoneticReading: string;
  romaji?: string;
  tokens: ReferenceToken[];
  lexicon?: Record<string, string>;
  native: NativeTimingReference;
  scorerVersion: string;
}

export interface AudioQuality {
  status: "ok" | "too_short" | "too_long" | "silent" | "too_quiet" | "clipped" | "noisy";
  scorable: boolean;
  durationMs: number;
  speechDurationMs: number;
  speechStartMs: number | null;
  speechEndMs: number | null;
  speechRatio: number;
  clippingRatio: number;
  rms: number;
  peak: number;
  noiseFloor: number;
  snrDb: number | null;
  qualityScore: number;
  reasons: string[];
  speechSegments: Array<{ startMs: number; endMs: number }>;
}

export interface EvaluationSegment {
  tokenIndex: number;
  surface: string;
  expected: string;
  heard?: string;
  status: SegmentStatus;
  score?: number;
  message?: string;
  startMs?: number;
  endMs?: number;
}

export interface ScoreBreakdown {
  overall: number;
  accuracy: number;
  completeness: number;
  clarity: number;
  fluency: number;
  rhythm: number;
  band: "excellent" | "good" | "fair" | "retry";
}

export interface PronunciationEvaluation {
  ok: true;
  attemptId: string;
  sentenceId: string;
  assessmentStatus: AssessmentStatus;
  scorerVersion: string;
  reference: {
    timingSource: "native" | "subtitle" | "estimated";
  };
  engine: {
    primary: "@cf/deepgram/nova-3";
    secondary: "@cf/openai/whisper-large-v3-turbo";
    selected: "primary" | "secondary";
    fallbackUsed: boolean;
    reliability: number;
    agreement?: number;
  };
  recognized?: {
    text: string;
    phoneticKana?: string;
    durationMs: number;
  };
  score?: ScoreBreakdown;
  audioQuality: AudioQuality;
  segments: EvaluationSegment[];
  feedback: string[];
}

export interface ApiErrorBody {
  ok: false;
  error: {
    code: string;
    message: string;
    requestId: string;
  };
}

export interface AsrWord {
  word: string;
  start: number;
  end: number;
  confidence: number;
}

export interface NovaTranscription {
  transcript: string;
  confidence: number;
  words: AsrWord[];
  gatewayLogId?: string;
}

export interface WhisperTranscription {
  transcript: string;
  words: AsrWord[];
  meanLogProbability: number | null;
  noSpeechProbability: number | null;
  gatewayLogId?: string;
}

export interface MoraEvidence {
  value: string;
  tokenIndex?: number;
  startMs?: number;
  endMs?: number;
  confidence?: number;
  unknown?: boolean;
}

export function isPronunciationEvaluation(value: unknown): value is PronunciationEvaluation {
  if (!isRecord(value) || value.ok !== true) return false;
  if (typeof value.attemptId !== "string" || typeof value.sentenceId !== "string") return false;
  if (!isAssessmentStatus(value.assessmentStatus) || typeof value.scorerVersion !== "string") return false;
  return isRecord(value.engine) && typeof value.engine.reliability === "number" && Array.isArray(value.segments);
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isAssessmentStatus(value: unknown): value is AssessmentStatus {
  return value === "scored" || value === "uncertain" || value === "re_record";
}
