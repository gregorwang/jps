import type { WhisperTranscription } from "../contracts";
import type { NormalizedTranscript } from "../japanese/normalize";
import type { MoraAlignment } from "./align";
import { clamp, mean } from "../utils/math";

export function hasScoreableEvidence(normalized: NormalizedTranscript, alignment: MoraAlignment): boolean {
  const knownMoraCount = normalized.morae.filter((mora) => !mora.unknown && mora.value !== "�").length;
  return knownMoraCount >= 2 || alignment.matches >= 2;
}

export function whisperConfidence(transcription: WhisperTranscription): number {
  if (transcription.words.length > 0) return clamp(mean(transcription.words.map((word) => word.confidence)), 0, 1);
  if (transcription.meanLogProbability !== null) return clamp(Math.exp(transcription.meanLogProbability), 0, 1);
  return 0.65;
}
