import { ApiError } from "../errors";

export interface ParsedWav {
  sampleRate: number;
  channels: 1;
  bitsPerSample: 16;
  samples: Int16Array;
  durationMs: number;
}

export function parsePcm16MonoWav(bytes: ArrayBuffer): ParsedWav {
  if (bytes.byteLength < 44) throw invalidWav("WAV 文件过短");
  const view = new DataView(bytes);
  if (readFourCc(view, 0) !== "RIFF" || readFourCc(view, 8) !== "WAVE") {
    throw invalidWav("只接受 RIFF/WAVE 音频");
  }

  let offset = 12;
  let format: { audioFormat: number; channels: number; sampleRate: number; byteRate: number; blockAlign: number; bits: number } | null = null;
  let dataOffset = -1;
  let dataLength = -1;

  while (offset + 8 <= view.byteLength) {
    const chunkId = readFourCc(view, offset);
    const chunkLength = view.getUint32(offset + 4, true);
    const contentOffset = offset + 8;
    if (contentOffset + chunkLength > view.byteLength) throw invalidWav("WAV chunk 长度无效");

    if (chunkId === "fmt ") {
      if (chunkLength < 16) throw invalidWav("WAV fmt chunk 无效");
      format = {
        audioFormat: view.getUint16(contentOffset, true),
        channels: view.getUint16(contentOffset + 2, true),
        sampleRate: view.getUint32(contentOffset + 4, true),
        byteRate: view.getUint32(contentOffset + 8, true),
        blockAlign: view.getUint16(contentOffset + 12, true),
        bits: view.getUint16(contentOffset + 14, true),
      };
    } else if (chunkId === "data" && dataOffset < 0) {
      dataOffset = contentOffset;
      dataLength = chunkLength;
    }
    offset = contentOffset + chunkLength + (chunkLength % 2);
  }

  if (format === null || dataOffset < 0 || dataLength <= 0) throw invalidWav("WAV 缺少 fmt 或 data chunk");
  if (format.audioFormat !== 1) throw invalidWav("只接受未压缩 PCM WAV");
  if (format.channels !== 1) throw invalidWav("录音必须为单声道");
  if (format.sampleRate !== 16_000) throw invalidWav("录音采样率必须为 16 kHz");
  if (format.bits !== 16 || format.blockAlign !== 2 || format.byteRate !== 32_000) {
    throw invalidWav("录音必须为 PCM16");
  }
  if (dataLength % 2 !== 0 || dataOffset % 2 !== 0) throw invalidWav("PCM16 数据未正确对齐");

  const samples = new Int16Array(bytes, dataOffset, dataLength / 2);
  return {
    sampleRate: format.sampleRate,
    channels: 1,
    bitsPerSample: 16,
    samples,
    durationMs: (samples.length / format.sampleRate) * 1000,
  };
}

function readFourCc(view: DataView, offset: number): string {
  return String.fromCharCode(
    view.getUint8(offset),
    view.getUint8(offset + 1),
    view.getUint8(offset + 2),
    view.getUint8(offset + 3),
  );
}

function invalidWav(message: string): ApiError {
  return new ApiError(400, "bad_request", message);
}
