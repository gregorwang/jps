import { spawn } from "node:child_process";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const devVars = await readFile(new URL("../.dev.vars", import.meta.url), "utf8");
const secretLine = devVars.split(/\r?\n/u).find((line) => line.startsWith("AUTH_HMAC_SECRET="));
const secret = secretLine?.slice("AUTH_HMAC_SECRET=".length).trim();
if (!secret || secret.length < 32) throw new Error("Run pnpm secrets:bootstrap first");

const wrangler = fileURLToPath(new URL("../node_modules/wrangler/bin/wrangler.js", import.meta.url));
const child = spawn(process.execPath, [wrangler, "secret", "put", "AUTH_HMAC_SECRET"], {
  cwd: fileURLToPath(new URL("..", import.meta.url)),
  stdio: ["pipe", "inherit", "inherit"],
});
child.stdin.end(secret);
const exitCode = await new Promise((resolve, reject) => {
  child.once("error", reject);
  child.once("exit", (code) => resolve(code));
});
if (exitCode !== 0) throw new Error(`wrangler secret put failed with exit code ${exitCode}`);
