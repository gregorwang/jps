import type { NovaTranscription } from "../contracts";
import { UpstreamAsrError } from "../errors";
import { parseNovaResponse } from "./parse";

export interface AsrMetadata {
  attemptId: string;
  sentenceId: string;
  userHash: string;
}

export async function runNova(
  bytes: ArrayBuffer,
  contentType: string,
  env: Env,
  metadata: AsrMetadata,
): Promise<NovaTranscription> {
  const audioBody = new Response(bytes).body;
  if (audioBody === null) throw new UpstreamAsrError("无法读取录音数据");
  let response: Response;
  try {
    response = await env.AI.run(
      "@cf/deepgram/nova-3",
      {
        audio: { body: audioBody, contentType },
        language: "ja",
        punctuate: false,
        smart_format: false,
        numerals: false,
        utterances: true,
        mip_opt_out: true,
      },
      {
        returnRawResponse: true,
      },
    );
  } catch (error) {
    console.error(JSON.stringify({ message: "nova_request_failed", error: errorMessage(error), attemptId: metadata.attemptId }));
    throw new UpstreamAsrError();
  }
  if (!response.ok) {
    console.error(JSON.stringify({ message: "nova_upstream_error", status: response.status, attemptId: metadata.attemptId }));
    throw new UpstreamAsrError();
  }
  let payload: unknown;
  try {
    payload = await response.json();
  } catch {
    throw new UpstreamAsrError("Nova-3 返回了无效 JSON");
  }
  return parseNovaResponse(payload, env.AI.aiGatewayLogId ?? undefined);
}

export function gatewayMetadata(metadata: AsrMetadata, stage: "nova" | "whisper"): Record<string, string> {
  return {
    attemptId: metadata.attemptId,
    sentenceId: metadata.sentenceId,
    userHash: metadata.userHash.slice(0, 24),
    stage,
  };
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}
