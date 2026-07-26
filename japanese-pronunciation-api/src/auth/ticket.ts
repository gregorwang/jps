import { ApiError } from "../errors";
import { base64UrlDecode, base64UrlEncode, utf8 } from "../utils/crypto";
import { isRecord } from "../contracts";

export interface EvaluationTicketClaims {
  v: 1;
  sub: string;
  sentenceId: string;
  iat: number;
  exp: number;
  jti: string;
}

export async function verifyEvaluationTicket(
  authorization: string | null,
  secret: string,
  sentenceId?: string,
  nowSeconds = Math.floor(Date.now() / 1000),
): Promise<EvaluationTicketClaims> {
  if (secret.length < 32) throw new ApiError(500, "invalid_server_configuration", "鉴权密钥配置无效");
  if (!authorization?.startsWith("Bearer ")) {
    throw new ApiError(401, "unauthorized", "缺少评测票据");
  }

  const token = authorization.slice("Bearer ".length).trim();
  const parts = token.split(".");
  if (parts.length !== 2) throw new ApiError(401, "unauthorized", "评测票据格式无效");
  const [payloadPart, signaturePart] = parts;
  if (!payloadPart || !signaturePart || payloadPart.length > 2048) {
    throw new ApiError(401, "unauthorized", "评测票据格式无效");
  }

  let payloadBytes: Uint8Array<ArrayBuffer>;
  let signatureBytes: Uint8Array<ArrayBuffer>;
  try {
    payloadBytes = base64UrlDecode(payloadPart);
    signatureBytes = base64UrlDecode(signaturePart);
  } catch {
    throw new ApiError(401, "unauthorized", "评测票据格式无效");
  }

  const key = await importHmacKey(secret, ["verify"]);
  const validSignature = await crypto.subtle.verify("HMAC", key, signatureBytes, utf8(payloadPart));
  if (!validSignature) throw new ApiError(401, "unauthorized", "评测票据签名无效");

  let parsed: unknown;
  try {
    parsed = JSON.parse(new TextDecoder().decode(payloadBytes));
  } catch {
    throw new ApiError(401, "unauthorized", "评测票据内容无效");
  }
  if (!isTicketClaims(parsed)) throw new ApiError(401, "unauthorized", "评测票据内容无效");
  if (sentenceId !== undefined && parsed.sentenceId !== sentenceId) {
    throw new ApiError(403, "ticket_scope_mismatch", "票据不适用于该句子");
  }
  if (parsed.exp <= nowSeconds || parsed.iat > nowSeconds + 5) throw new ApiError(401, "ticket_expired", "评测票据已过期");
  if (parsed.exp - parsed.iat > 120) throw new ApiError(401, "unauthorized", "评测票据有效期过长");
  return parsed;
}

export async function signEvaluationTicket(
  claims: EvaluationTicketClaims,
  secret: string,
): Promise<string> {
  if (secret.length < 32) throw new Error("AUTH_HMAC_SECRET must contain at least 32 characters");
  const payload = base64UrlEncode(utf8(JSON.stringify(claims)));
  const key = await importHmacKey(secret, ["sign"]);
  const signature = await crypto.subtle.sign("HMAC", key, utf8(payload));
  return `${payload}.${base64UrlEncode(new Uint8Array(signature))}`;
}

function importHmacKey(secret: string, usages: KeyUsage[]): Promise<CryptoKey> {
  return crypto.subtle.importKey("raw", utf8(secret), { name: "HMAC", hash: "SHA-256" }, false, usages);
}

function isTicketClaims(value: unknown): value is EvaluationTicketClaims {
  if (!isRecord(value)) return false;
  return (
    value.v === 1 &&
    typeof value.sub === "string" &&
    value.sub.length >= 1 &&
    value.sub.length <= 160 &&
    typeof value.sentenceId === "string" &&
    typeof value.iat === "number" &&
    Number.isInteger(value.iat) &&
    typeof value.exp === "number" &&
    Number.isInteger(value.exp) &&
    typeof value.jti === "string" &&
    /^[A-Za-z0-9_-]{8,128}$/u.test(value.jti)
  );
}
