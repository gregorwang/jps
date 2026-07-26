import { createHmac, randomBytes } from "node:crypto";
import { readFile } from "node:fs/promises";

const args = new Map();
for (let index = 2; index < process.argv.length; index += 2) {
  const key = process.argv[index];
  const value = process.argv[index + 1];
  if (!key?.startsWith("--") || !value) throw new Error("Use --sentence ID [--subject USER] [--ttl SECONDS]");
  args.set(key.slice(2), value);
}
const sentenceId = args.get("sentence");
if (!sentenceId) throw new Error("--sentence is required");
const subject = args.get("subject") ?? "local-smoke-user";
const ttl = Number(args.get("ttl") ?? "60");
if (!Number.isInteger(ttl) || ttl < 1 || ttl > 120) throw new Error("--ttl must be an integer from 1 to 120");

const devVars = await readFile(new URL("../.dev.vars", import.meta.url), "utf8");
const secretLine = devVars.split(/\r?\n/u).find((line) => line.startsWith("AUTH_HMAC_SECRET="));
const secret = secretLine?.slice("AUTH_HMAC_SECRET=".length).trim();
if (!secret || secret.length < 32) throw new Error("Run pnpm secrets:bootstrap first");

const now = Math.floor(Date.now() / 1000);
const claims = {
  v: 1,
  sub: subject,
  sentenceId,
  iat: now,
  exp: now + ttl,
  jti: randomBytes(18).toString("base64url"),
};
const payload = Buffer.from(JSON.stringify(claims), "utf8").toString("base64url");
const signature = createHmac("sha256", secret).update(payload).digest("base64url");
console.log(`${payload}.${signature}`);
