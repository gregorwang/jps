/**
 * Probe Workers AI embedding dimensions for EMBEDDING_MODEL.
 * Run after `wrangler dev` with remote AI, or deploy and call with credentials.
 *
 * Usage (remote Workers AI via wrangler):
 *   npm run probe:embedding
 */

import { readFileSync } from "node:fs";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
const EMBEDDING_MODEL = "@cf/baai/bge-m3";

const __dirname = dirname(fileURLToPath(import.meta.url));

function loadAccountId(): string {
  const envId = process.env.CLOUDFLARE_ACCOUNT_ID?.trim();
  if (envId) return envId;

  try {
    const out = readFileSync(resolve(__dirname, "../.wrangler/deploy/config.json"), "utf8");
    const cfg = JSON.parse(out) as { account_id?: string };
    if (cfg.account_id) return cfg.account_id;
  } catch {
    // ignore
  }

  throw new Error(
    "Set CLOUDFLARE_ACCOUNT_ID or run `wrangler whoami` and export the account id.",
  );
}

async function main(): Promise<void> {
  const accountId = loadAccountId();
  const token =
    process.env.CLOUDFLARE_API_TOKEN?.trim() ??
    process.env.WRANGLER_API_TOKEN?.trim();

  if (!token) {
    console.error(
      "Set CLOUDFLARE_API_TOKEN (API token with Workers AI read) or run via OAuth:",
    );
    console.error(
      "  $env:CLOUDFLARE_API_TOKEN = (wrangler whoami does not print token; create API token in dashboard)",
    );
    process.exit(1);
  }

  const url = `https://api.cloudflare.com/client/v4/accounts/${accountId}/ai/run/${EMBEDDING_MODEL}`;
  const res = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ text: ["次元テスト"] }),
  });

  const raw = await res.text();
  if (!res.ok) {
    console.error("Workers AI request failed:", res.status, raw);
    process.exit(1);
  }

  const parsed = JSON.parse(raw) as {
    result?: { shape?: number[]; data?: number[][] };
  };
  const result = parsed.result ?? parsed;
  const data = (result as { data?: number[][] }).data;
  const shape = (result as { shape?: number[] }).shape;

  let dims: number | undefined;
  if (shape && shape.length >= 2) {
    dims = shape[1];
  } else if (data?.[0]) {
    dims = data[0].length;
  }

  if (!dims) {
    console.error("Could not read embedding dimensions:", raw.slice(0, 500));
    process.exit(1);
  }

  console.log(`Model: ${EMBEDDING_MODEL}`);
  console.log(`Embedding dimensions: ${dims}`);
  console.log("");
  console.log("Create Vectorize index (if not exists):");
  console.log(
    `  npx wrangler vectorize create anime-japanese-lab-subtitles --dimensions=${dims} --metric=cosine`,
  );
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
