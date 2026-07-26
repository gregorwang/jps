import type {
  AudioQuality,
  EvaluationMode,
  EvaluationSegment,
  NovaTranscription,
  PronunciationEvaluation,
  ScoreBreakdown,
  SentenceReference,
} from "../contracts";
import type { MoraAlignment } from "./align";
import { clamp, mean, round } from "../utils/math";

const PRIMARY_MODEL = "@cf/deepgram/nova-3" as const;
const SECONDARY_MODEL = "@cf/openai/whisper-large-v3-turbo" as const;

interface ScoringInput {
  attemptId: string;
  reference: SentenceReference;
  mode: EvaluationMode;
  quality: AudioQuality;
  nova: NovaTranscription;
  recognizedKana: string;
  alignment: MoraAlignment;
  reliability: number;
  fallbackUsed: boolean;
  agreement: number | null;
  selectedModel: "primary" | "secondary";
  lowConfidence: boolean;
}

export function computeReliability(input: {
  novaConfidence: number;
  qualityScore: number;
  unknownRatio: number;
  moraCount: number;
  fallbackUsed: boolean;
  agreement: number | null;
  fallbackFailed: boolean;
}): number {
  if (input.moraCount === 0) return 0;
  const confidence = clamp(input.novaConfidence, 0, 1);
  const quality = clamp(input.qualityScore / 100, 0, 1);
  const known = clamp(1 - input.unknownRatio, 0, 1);
  let reliability = input.fallbackUsed
    ? 0.4 * confidence + 0.2 * quality + 0.15 * known + 0.25 * (input.agreement ?? 0)
    : 0.5 * confidence + 0.2 * quality + 0.2 * known + 0.1 * 0.85;
  if (input.fallbackFailed) reliability -= 0.15;
  return clamp(reliability, 0, 1);
}

export function makeScoredEvaluation(input: ScoringInput): PronunciationEvaluation {
  const accuracy = clamp(100 * (1 - (input.alignment.substitutions + 0.35 * input.alignment.insertions) / expectedCount(input.alignment)), 0, 100);
  const completeness = clamp(100 * (1 - input.alignment.deletions / expectedCount(input.alignment)), 0, 100);
  const clarity = clarityScore(input.nova.confidence, input.quality.qualityScore, input.agreement);
  const fluency = fluencyScore(input.quality);
  const rhythm = rhythmScore(input.quality, input.reference, input.mode);
  const overall = 0.4 * accuracy + 0.2 * completeness + 0.15 * clarity + 0.15 * fluency + 0.1 * rhythm;
  const score: ScoreBreakdown = {
    overall: round(overall),
    accuracy: round(accuracy),
    completeness: round(completeness),
    clarity: round(clarity),
    fluency: round(fluency),
    rhythm: round(rhythm),
    band: scoreBand(overall),
  };
  const segments = buildSegments(input.reference, input.alignment, input.quality.durationMs);
  return {
    ok: true,
    attemptId: input.attemptId,
    sentenceId: input.reference.sentenceId,
    assessmentStatus: "scored",
    scorerVersion: input.reference.scorerVersion,
    reference: referenceMetadata(input.reference),
    engine: {
      primary: PRIMARY_MODEL,
      secondary: SECONDARY_MODEL,
      selected: input.selectedModel,
      fallbackUsed: input.fallbackUsed,
      reliability: round(input.reliability, 3),
      ...(input.agreement === null ? {} : { agreement: round(input.agreement, 3) }),
    },
    recognized: {
      text: input.nova.transcript,
      ...(input.recognizedKana.includes("�") ? {} : { phoneticKana: input.recognizedKana }),
      durationMs: input.quality.durationMs,
    },
    score,
    audioQuality: input.quality,
    segments,
    feedback: [
      ...(input.lowConfidence ? ["识别可靠度较低，本次低分仅供练习参考"] : []),
      ...buildFeedback(score, segments),
    ].slice(0, 3),
  };
}

export function makeUncertainEvaluation(input: {
  attemptId: string;
  reference: SentenceReference;
  quality: AudioQuality;
  novaTranscript: string;
  recognizedKana: string;
  reliability: number;
  fallbackUsed: boolean;
  selectedModel: "primary" | "secondary";
  agreement: number | null;
}): PronunciationEvaluation {
  return {
    ok: true,
    attemptId: input.attemptId,
    sentenceId: input.reference.sentenceId,
    assessmentStatus: "uncertain",
    scorerVersion: input.reference.scorerVersion,
    reference: referenceMetadata(input.reference),
    engine: {
      primary: PRIMARY_MODEL,
      secondary: SECONDARY_MODEL,
      selected: input.selectedModel,
      fallbackUsed: input.fallbackUsed,
      reliability: round(input.reliability, 3),
      ...(input.agreement === null ? {} : { agreement: round(input.agreement, 3) }),
    },
    recognized: {
      text: input.novaTranscript,
      ...(input.recognizedKana !== "" && !input.recognizedKana.includes("�") ? { phoneticKana: input.recognizedKana } : {}),
      durationMs: input.quality.durationMs,
    },
    audioQuality: input.quality,
    segments: input.reference.tokens.slice(0, 3).map((token, tokenIndex) => ({
      tokenIndex,
      surface: token.surface,
      expected: token.reading,
      status: "unscored" as const,
      message: "本次证据不足，未对该词作精确判断",
    })),
    feedback: ["本次识别证据不一致，未生成伪精确分数", "请在较安静环境中重新跟读一次"],
  };
}

export function makeReRecordEvaluation(input: {
  attemptId: string;
  reference: SentenceReference;
  quality: AudioQuality;
}): PronunciationEvaluation {
  return {
    ok: true,
    attemptId: input.attemptId,
    sentenceId: input.reference.sentenceId,
    assessmentStatus: "re_record",
    scorerVersion: input.reference.scorerVersion,
    reference: referenceMetadata(input.reference),
    engine: {
      primary: PRIMARY_MODEL,
      secondary: SECONDARY_MODEL,
      selected: "primary",
      fallbackUsed: false,
      reliability: 0,
    },
    audioQuality: input.quality,
    segments: [],
    feedback: input.quality.reasons.length > 0 ? input.quality.reasons : ["录音质量不足，请重新录音"],
  };
}

function buildSegments(reference: SentenceReference, alignment: MoraAlignment, durationMs: number): EvaluationSegment[] {
  const segments: EvaluationSegment[] = reference.tokens.map((token, tokenIndex) => {
    const operations = alignment.operations.filter((operation) => operation.expected?.tokenIndex === tokenIndex);
    const heard = operations.flatMap((operation) => (operation.heard ? [operation.heard] : []));
    const substitutions = operations.filter((operation) => operation.type === "substitution").length;
    const deletions = operations.filter((operation) => operation.type === "deletion").length;
    const confidences = heard.flatMap((item) => (item.confidence === undefined ? [] : [item.confidence]));
    const averageConfidence = confidences.length > 0 ? mean(confidences) : 1;
    const expectedMoraCount = Math.max(1, token.morae.length);
    const tokenScore = clamp(100 * (1 - (substitutions + deletions) / expectedMoraCount) * (0.75 + 0.25 * averageConfidence), 0, 100);
    let status: EvaluationSegment["status"] = "correct";
    let message: string | undefined;
    if (deletions === expectedMoraCount) {
      status = "omission";
      message = `「${token.surface}」疑似漏读`;
    } else if (substitutions > 0 || deletions > 0) {
      status = averageConfidence < 0.75 ? "unclear" : "substitution";
      message = `「${token.surface}」疑似读音不一致`;
    } else if (averageConfidence < 0.72) {
      status = "unclear";
      message = `「${token.surface}」疑似不够清楚`;
    }

    const startTimes = heard.flatMap((item) => (item.startMs === undefined ? [] : [item.startMs]));
    const endTimes = heard.flatMap((item) => (item.endMs === undefined ? [] : [item.endMs]));
    return {
      tokenIndex,
      surface: token.surface,
      expected: token.reading,
      ...(heard.length > 0 ? { heard: heard.map((item) => item.value).join("") } : {}),
      status,
      score: round(tokenScore),
      ...(message === undefined ? {} : { message }),
      ...(startTimes.length > 0 ? { startMs: round(clamp(Math.min(...startTimes), 0, durationMs)) } : {}),
      ...(endTimes.length > 0 ? { endMs: round(clamp(Math.max(...endTimes), 0, durationMs)) } : {}),
    };
  });

  const extras = alignment.operations.flatMap((operation) => (operation.type === "insertion" && operation.heard ? [operation.heard] : []));
  if (extras.length > 0) {
    const starts = extras.flatMap((item) => (item.startMs === undefined ? [] : [item.startMs]));
    const ends = extras.flatMap((item) => (item.endMs === undefined ? [] : [item.endMs]));
    segments.push({
      tokenIndex: -1,
      surface: "",
      expected: "",
      heard: extras.map((item) => item.value).join(""),
      status: "extra",
      message: "疑似多读了一个片段",
      ...(starts.length > 0 ? { startMs: round(clamp(Math.min(...starts), 0, durationMs)) } : {}),
      ...(ends.length > 0 ? { endMs: round(clamp(Math.max(...ends), 0, durationMs)) } : {}),
    });
  }
  return segments;
}

function clarityScore(novaConfidence: number, qualityScore: number, agreement: number | null): number {
  return agreement === null
    ? clamp(80 * novaConfidence + 0.2 * qualityScore, 0, 100)
    : clamp(65 * novaConfidence + 0.2 * qualityScore + 15 * agreement, 0, 100);
}

function fluencyScore(quality: AudioQuality): number {
  const internalPauseMs = quality.speechSegments.slice(1).reduce((sum, segment, index) => {
    const previous = quality.speechSegments[index];
    return sum + Math.max(0, segment.startMs - (previous?.endMs ?? segment.startMs) - 250);
  }, 0);
  const ratioPenalty = Math.abs(quality.speechRatio - 0.75) * 40;
  return clamp(100 - internalPauseMs / 20 - ratioPenalty, 0, 100);
}

function rhythmScore(quality: AudioQuality, reference: SentenceReference, mode: EvaluationMode): number {
  const nativeSpeechMs = Math.max(1, reference.native.speechEndMs - reference.native.speechStartMs);
  const ratio = Math.max(0.1, quality.speechDurationMs / nativeSpeechMs);
  const durationPenalty = Math.abs(Math.log(ratio)) * (mode === "shadowing" ? 70 : 55);
  const pauseCountPenalty = Math.abs(Math.max(0, quality.speechSegments.length - 1) - reference.native.pauseBoundaries.length) * 7;
  return clamp(100 - durationPenalty - pauseCountPenalty, 0, 100);
}

function scoreBand(score: number): ScoreBreakdown["band"] {
  if (score >= 90) return "excellent";
  if (score >= 80) return "good";
  if (score >= 65) return "fair";
  return "retry";
}

function expectedCount(alignment: MoraAlignment): number {
  return Math.max(1, alignment.matches + alignment.substitutions + alignment.deletions);
}

function buildFeedback(score: ScoreBreakdown, segments: readonly EvaluationSegment[]): string[] {
  const issues = segments
    .filter((segment) => segment.status !== "correct")
    .sort((left, right) => (left.score ?? 0) - (right.score ?? 0))
    .slice(0, 3)
    .flatMap((segment) => (segment.message ? [segment.message] : []));
  if (issues.length === 0) {
    return score.overall >= 90 ? ["整体内容与节奏都很接近参考"] : ["整体内容正确", "继续保持清晰、连贯的节奏"];
  }
  return issues;
}

function referenceMetadata(reference: SentenceReference): PronunciationEvaluation["reference"] {
  return { timingSource: reference.native.source ?? "native" };
}
