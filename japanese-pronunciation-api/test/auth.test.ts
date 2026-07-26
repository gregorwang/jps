import { describe, expect, it } from "vitest";
import { signEvaluationTicket, verifyEvaluationTicket } from "../src/auth/ticket";

const secret = "test-secret-that-is-longer-than-thirty-two-characters";

describe("evaluation tickets", () => {
  it("signs and verifies a sentence-scoped short-lived ticket", async () => {
    const claims = { v: 1 as const, sub: "user-123", sentenceId: "sentence-1", iat: 1000, exp: 1060, jti: "ticket_123456" };
    const token = await signEvaluationTicket(claims, secret);
    await expect(verifyEvaluationTicket(`Bearer ${token}`, secret, "sentence-1", 1020)).resolves.toEqual(claims);
  });

  it("rejects a ticket for another sentence", async () => {
    const claims = { v: 1 as const, sub: "user-123", sentenceId: "sentence-1", iat: 1000, exp: 1060, jti: "ticket_123456" };
    const token = await signEvaluationTicket(claims, secret);
    await expect(verifyEvaluationTicket(`Bearer ${token}`, secret, "sentence-2", 1020)).rejects.toMatchObject({ code: "ticket_scope_mismatch" });
  });

  it("rejects expired tickets", async () => {
    const claims = { v: 1 as const, sub: "user-123", sentenceId: "sentence-1", iat: 1000, exp: 1060, jti: "ticket_123456" };
    const token = await signEvaluationTicket(claims, secret);
    await expect(verifyEvaluationTicket(`Bearer ${token}`, secret, "sentence-1", 1060)).rejects.toMatchObject({ code: "ticket_expired" });
  });
});
