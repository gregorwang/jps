import { randomBytes } from "node:crypto";
import { access, writeFile } from "node:fs/promises";

const path = new URL("../.dev.vars", import.meta.url);
try {
  await access(path);
  console.log(".dev.vars already exists; no changes made.");
} catch {
  const secret = randomBytes(48).toString("base64url");
  await writeFile(path, `AUTH_HMAC_SECRET=${secret}\n`, { encoding: "utf8", mode: 0o600 });
  console.log("Created .dev.vars with a new HMAC secret (value not printed).");
}
