export interface EvaluationTicketInput {
  subject: string;
  sentenceId: string;
  secret: string;
  ttlSeconds?: number;
}

export async function createEvaluationTicket(input: EvaluationTicketInput): Promise<string> {
  const ttl = input.ttlSeconds ?? 60;
  if (!Number.isInteger(ttl) || ttl < 1 || ttl > 120) throw new Error("ttlSeconds must be from 1 to 120");
  if (input.secret.length < 32) throw new Error("HMAC secret is too short");
  const now = Math.floor(Date.now() / 1000);
  const payloadBytes = new TextEncoder().encode(
    JSON.stringify({
      v: 1,
      sub: input.subject,
      sentenceId: input.sentenceId,
      iat: now,
      exp: now + ttl,
      jti: crypto.randomUUID().replaceAll("-", ""),
    }),
  );
  const payload = base64Url(payloadBytes);
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(input.secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(payload));
  return `${payload}.${base64Url(new Uint8Array(signature))}`;
}

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/u, "");
}
