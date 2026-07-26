import { Buffer } from "node:buffer";
import type { WhisperTranscription } from "../contracts";
import { UpstreamAsrError } from "../errors";
import type { AsrMetadata } from "./nova";
import { errorMessage, gatewayMetadata } from "./nova";
import { parseWhisperResponse } from "./parse";

export async function runWhisper(
  bytes: ArrayBuffer,
  env: Env,
  metadata: AsrMetadata,
): Promise<WhisperTranscription> {
  let payload: unknown;
  try {
    payload = await env.AI.run(
      "@cf/openai/whisper-large-v3-turbo",
      {
        audio: Buffer.from(bytes).toString("base64"),
        language: "ja",
        task: "transcribe",
        vad_filter: true,
        beam_size: 5,
        condition_on_previous_text: false,
        no_speech_threshold: 0.6,
      },
      {
        gateway: {
          id: env.AI_GATEWAY_ID,
          skipCache: true,
          collectLog: false,
          metadata: gatewayMetadata(metadata, "whisper"),
        },
      },
    );
  } catch (error) {
    console.warn(JSON.stringify({ message: "whisper_request_failed", error: errorMessage(error), attemptId: metadata.attemptId }));
    throw new UpstreamAsrError("Whisper 复核暂时不可用");
  }
  return parseWhisperResponse(payload, env.AI.aiGatewayLogId ?? undefined);
}
