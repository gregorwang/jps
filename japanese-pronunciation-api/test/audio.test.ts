import { describe, expect, it } from "vitest";
import { parsePcm16MonoWav } from "../src/audio/wav";
import { analyzeAudioQuality } from "../src/audio/quality";
import { makePcm16Wav, speechLikeSamples } from "./helpers/audio";

describe("PCM16 WAV parsing and quality gating", () => {
  it("parses a 16 kHz mono PCM16 WAV", () => {
    const wav = parsePcm16MonoWav(makePcm16Wav(speechLikeSamples(1)));
    expect(wav.sampleRate).toBe(16_000);
    expect(wav.channels).toBe(1);
    expect(wav.durationMs).toBeCloseTo(1000, 3);
  });

  it("rejects unsupported sample rate", () => {
    const input = makePcm16Wav(new Int16Array(8_000), { sampleRate: 8_000 });
    expect(() => parsePcm16MonoWav(input)).toThrow("16 kHz");
  });

  it("accepts speech-like audio", () => {
    const quality = analyzeAudioQuality(parsePcm16MonoWav(makePcm16Wav(speechLikeSamples())));
    expect(quality.scorable).toBe(true);
    expect(quality.status).toBe("ok");
    expect(quality.speechDurationMs).toBeGreaterThan(500);
  });

  it("returns silent instead of a fake low score", () => {
    const quality = analyzeAudioQuality(parsePcm16MonoWav(makePcm16Wav(new Int16Array(16_000))));
    expect(quality.scorable).toBe(false);
    expect(quality.status).toBe("silent");
  });

  it("rejects heavily clipped audio", () => {
    const samples = new Int16Array(16_000);
    for (let index = 0; index < samples.length; index += 1) samples[index] = index % 2 === 0 ? 32_767 : -32_768;
    const quality = analyzeAudioQuality(parsePcm16MonoWav(makePcm16Wav(samples)));
    expect(quality.scorable).toBe(false);
    expect(quality.status).toBe("clipped");
  });
});
