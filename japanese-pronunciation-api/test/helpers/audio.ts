export function makePcm16Wav(
  samples: Int16Array,
  options: { sampleRate?: number; channels?: number } = {},
): ArrayBuffer {
  const sampleRate = options.sampleRate ?? 16_000;
  const channels = options.channels ?? 1;
  const blockAlign = channels * 2;
  const buffer = new ArrayBuffer(44 + samples.byteLength);
  const view = new DataView(buffer);
  writeFourCc(view, 0, "RIFF");
  view.setUint32(4, 36 + samples.byteLength, true);
  writeFourCc(view, 8, "WAVE");
  writeFourCc(view, 12, "fmt ");
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, channels, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * blockAlign, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, 16, true);
  writeFourCc(view, 36, "data");
  view.setUint32(40, samples.byteLength, true);
  new Int16Array(buffer, 44).set(samples);
  return buffer;
}

export function speechLikeSamples(durationSeconds = 1.2): Int16Array {
  const sampleRate = 16_000;
  const length = Math.round(durationSeconds * sampleRate);
  const samples = new Int16Array(length);
  for (let index = 0; index < length; index += 1) {
    const time = index / sampleRate;
    const active = time >= 0.18 && time <= durationSeconds - 0.18;
    if (!active) continue;
    const envelope = 0.12 + 0.08 * Math.sin(2 * Math.PI * 3.2 * time) ** 2;
    const value = envelope * (Math.sin(2 * Math.PI * 180 * time) + 0.32 * Math.sin(2 * Math.PI * 360 * time));
    samples[index] = Math.round(Math.max(-0.95, Math.min(0.95, value)) * 32_767);
  }
  return samples;
}

function writeFourCc(view: DataView, offset: number, value: string): void {
  for (let index = 0; index < 4; index += 1) view.setUint8(offset + index, value.charCodeAt(index));
}
