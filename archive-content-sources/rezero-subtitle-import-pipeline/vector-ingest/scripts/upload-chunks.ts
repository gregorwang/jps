/**
 * Upload output/vectorize/chunks.jsonl to the ingest Worker in batches of 50.
 */

import { createReadStream } from "node:fs";
import { createInterface } from "node:readline";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
interface IngestChunk {
  id: string;
  text: string;
  metadata: Record<string, unknown> & { language?: string };
}

const __dirname = dirname(fileURLToPath(import.meta.url));
const CHUNKS_PATH = resolve(
  process.env.CHUNKS_PATH?.trim() ||
    resolve(__dirname, "../../output/vectorize/chunks.jsonl"),
);
const BATCH_SIZE = 50;

interface UpsertResponse {
  ok: boolean;
  received: number;
  upserted: number;
  rejected: number;
  errors: { id: string; message: string }[];
  error?: string;
}

async function readAllChunks(path: string): Promise<IngestChunk[]> {
  const chunks: IngestChunk[] = [];
  const rl = createInterface({
    input: createReadStream(path, { encoding: "utf8" }),
    crlfDelay: Infinity,
  });

  let lineNo = 0;
  for await (const line of rl) {
    lineNo += 1;
    const trimmed = line.trim();
    if (!trimmed) continue;
    try {
      chunks.push(JSON.parse(trimmed) as IngestChunk);
    } catch {
      throw new Error(`Invalid JSON at line ${lineNo}`);
    }
  }
  return chunks;
}

async function upsertBatch(
  baseUrl: string,
  token: string,
  batch: IngestChunk[],
  batchNo: number,
): Promise<UpsertResponse> {
  const url = `${baseUrl.replace(/\/+$/, "")}/admin/upsert`;
  const res = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ chunks: batch }),
  });

  const text = await res.text();
  let body: UpsertResponse;
  try {
    body = JSON.parse(text) as UpsertResponse;
  } catch {
    throw new Error(
      `Batch ${batchNo}: non-JSON response (${res.status}): ${text.slice(0, 500)}`,
    );
  }

  if (!res.ok) {
    console.error(`Batch ${batchNo} HTTP ${res.status}:`, text);
    throw new Error(body.error ?? `HTTP ${res.status}`);
  }

  return body;
}

async function main(): Promise<void> {
  const baseUrl = process.env.INGEST_WORKER_URL?.trim();
  const token = process.env.ADMIN_TOKEN?.trim();

  if (!baseUrl) {
    console.error("Missing INGEST_WORKER_URL");
    process.exit(1);
  }
  if (!token) {
    console.error("Missing ADMIN_TOKEN");
    process.exit(1);
  }

  console.log(`Reading ${CHUNKS_PATH}`);
  const all = await readAllChunks(CHUNKS_PATH);
  console.log(`Total chunks: ${all.length}`);

  const totalBatches = Math.ceil(all.length / BATCH_SIZE);
  let totalUpserted = 0;
  let totalRejected = 0;
  const allErrors: { id: string; message: string }[] = [];

  for (let i = 0; i < all.length; i += BATCH_SIZE) {
    const batchNo = Math.floor(i / BATCH_SIZE) + 1;
    const batch = all.slice(i, i + BATCH_SIZE);

    console.log(`\n--- Batch ${batchNo}/${totalBatches} (${batch.length} chunks) ---`);

    const result = await upsertBatch(baseUrl, token, batch, batchNo);

    console.log(`  received: ${result.received}`);
    console.log(`  upserted: ${result.upserted}`);
    console.log(`  rejected: ${result.rejected}`);

    if (result.errors.length > 0) {
      for (const e of result.errors) {
        console.log(`  error [${e.id}]: ${e.message}`);
      }
      allErrors.push(...result.errors);
    }

    if (!result.ok) {
      console.warn(`  batch ok=false (partial failures)`);
    }

    totalUpserted += result.upserted;
    totalRejected += result.rejected;
  }

  console.log("\n=== Upload complete ===");
  console.log(`Total chunks: ${all.length}`);
  console.log(`Upserted:     ${totalUpserted}`);
  console.log(`Rejected:     ${totalRejected}`);
  if (allErrors.length > 0) {
    console.log(`Errors:       ${allErrors.length}`);
    process.exit(1);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
