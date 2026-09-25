/**
 * Anime Japanese Lab — Vectorize ingest Worker
 * 日文主语料 chunks → Workers AI embedding → Vectorize upsert
 */

export const EMBEDDING_MODEL = "@cf/baai/bge-m3";

const MAX_CHUNKS_PER_REQUEST = 50;
const EMBEDDING_BATCH_SIZE = 10;
const METADATA_TEXT_MAX = 6000;

const STAFF_META_KEYWORDS = [
  "字幕",
  "字幕组",
  "制作",
  "工作人员",
  "感谢",
  "翻译",
  "校对",
  "时间轴",
  "压制",
  "片源",
  "招募",
] as const;

const RE_KANA = /[\u3040-\u309f\u30a0-\u30ff]/;
const RE_CHINESE_INDICATORS =
  /[的吗嘛吧呢]|动画|感谢|工作人员|轻音|就此|完结|特此|华盟|字幕|制作|翻译|校对|压制|片源|招募|[，；]/;

export interface ChunkMetadata {
  work: string;
  episode: number;
  chunk_no: number;
  start_time: string;
  end_time: string;
  language: string;
  source: string;
  text?: string;
  [key: string]: string | number | boolean | undefined;
}

export interface IngestChunk {
  id: string;
  text: string;
  metadata: ChunkMetadata;
}

interface EmbeddingResponse {
  shape?: number[];
  data?: number[][];
}

export interface Env {
  AI: Ai;
  VECTORIZE: VectorizeIndex;
  ADMIN_TOKEN: string;
}

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });
}

function isStaffMetaLine(text: string): boolean {
  return STAFF_META_KEYWORDS.some((kw) => text.includes(kw));
}

function isMainlyChineseLowKana(text: string): boolean {
  if (!RE_CHINESE_INDICATORS.test(text)) return false;
  const kanaCount = (text.match(/[\u3040-\u309f\u30a0-\u30ff]/g) ?? []).length;
  const cjkCount = (text.match(/[\u4e00-\u9fff]/g) ?? []).length;
  if (cjkCount < 4) return false;
  if (kanaCount === 0) return true;
  if (cjkCount >= 6 && kanaCount <= 2) return true;
  const total = cjkCount + kanaCount;
  return total > 0 && kanaCount / total < 0.08;
}

export function validateChunk(chunk: IngestChunk): string | null {
  if (!chunk.id || typeof chunk.id !== "string" || !chunk.id.trim()) {
    return "Rejected: missing or empty id";
  }
  if (!chunk.text || typeof chunk.text !== "string" || !chunk.text.trim()) {
    return "Rejected: missing or empty text";
  }
  if (!chunk.metadata || typeof chunk.metadata !== "object" || Array.isArray(chunk.metadata)) {
    return "Rejected: metadata must be an object";
  }
  if (chunk.metadata.language !== "ja") {
    return 'Rejected: metadata.language must be "ja"';
  }
  if (!RE_KANA.test(chunk.text)) {
    return "Rejected: text must contain Japanese kana";
  }
  if (isStaffMetaLine(chunk.text)) {
    return "Rejected: staff credit text detected";
  }
  if (isMainlyChineseLowKana(chunk.text)) {
    return "Rejected: mainly Chinese text";
  }
  return null;
}

function extractEmbeddings(result: unknown): number[][] {
  const r = result as EmbeddingResponse;
  if (r?.data && Array.isArray(r.data)) return r.data;
  if (Array.isArray(result)) return result as number[][];
  throw new Error("Unexpected embedding response shape");
}

async function embedTexts(ai: Ai, texts: string[]): Promise<number[][]> {
  const result = await ai.run(EMBEDDING_MODEL, { text: texts });
  return extractEmbeddings(result);
}

function buildVectorizeMetadata(chunk: IngestChunk): Record<string, string | number | boolean> {
  const meta: Record<string, string | number | boolean> = {};
  for (const [key, value] of Object.entries(chunk.metadata)) {
    if (key === "text") continue;
    if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
      meta[key] = value;
    }
  }
  meta.text = chunk.text.slice(0, METADATA_TEXT_MAX);
  return meta;
}

function checkAuth(request: Request, env: Env): Response | null {
  const header = request.headers.get("Authorization") ?? "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!env.ADMIN_TOKEN || token !== env.ADMIN_TOKEN) {
    return json({ ok: false, error: "Unauthorized" }, 401);
  }
  return null;
}

async function handleUpsert(request: Request, env: Env): Promise<Response> {
  const authError = checkAuth(request, env);
  if (authError) return authError;

  let body: { chunks?: IngestChunk[] };
  try {
    body = (await request.json()) as { chunks?: IngestChunk[] };
  } catch {
    return json({ ok: false, error: "Invalid JSON body" }, 400);
  }

  const chunks = body.chunks;
  if (!Array.isArray(chunks)) {
    return json({ ok: false, error: "Request body must include chunks array" }, 400);
  }
  if (chunks.length === 0) {
    return json({ ok: false, error: "chunks array is empty" }, 400);
  }
  if (chunks.length > MAX_CHUNKS_PER_REQUEST) {
    return json(
      { ok: false, error: `At most ${MAX_CHUNKS_PER_REQUEST} chunks per request` },
      400,
    );
  }

  const received = chunks.length;
  const errors: { id: string; message: string }[] = [];
  const valid: IngestChunk[] = [];

  for (const chunk of chunks) {
    const id = chunk?.id ?? "(unknown)";
    const rejectReason = validateChunk(chunk);
    if (rejectReason) {
      errors.push({ id, message: rejectReason });
      continue;
    }
    valid.push(chunk);
  }

  let upserted = 0;

  for (let i = 0; i < valid.length; i += EMBEDDING_BATCH_SIZE) {
    const batch = valid.slice(i, i + EMBEDDING_BATCH_SIZE);
    try {
      const vectors = await embedTexts(
        env.AI,
        batch.map((c) => c.text),
      );
      if (vectors.length !== batch.length) {
        for (const c of batch) {
          errors.push({
            id: c.id,
            message: "Rejected: embedding count mismatch",
          });
        }
        continue;
      }

      const records: VectorizeVector[] = batch.map((chunk, idx) => ({
        id: chunk.id,
        values: vectors[idx]!,
        metadata: buildVectorizeMetadata(chunk),
      }));

      await env.VECTORIZE.upsert(records);
      upserted += batch.length;
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      for (const c of batch) {
        errors.push({ id: c.id, message: `Upsert failed: ${message}` });
      }
    }
  }

  const rejected = errors.length;
  const ok = rejected === 0;

  return json({
    ok,
    received,
    upserted,
    rejected,
    errors,
  });
}

function buildQueryFilter(
  filter?: Record<string, string | number | boolean>,
): VectorizeVectorMetadataFilter | undefined {
  if (!filter || Object.keys(filter).length === 0) return undefined;
  const out: VectorizeVectorMetadataFilter = {};
  for (const [key, value] of Object.entries(filter)) {
    if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
      out[key] = value;
    }
  }
  return Object.keys(out).length > 0 ? out : undefined;
}

async function handleQuery(request: Request, env: Env): Promise<Response> {
  let body: {
    query?: string;
    topK?: number;
    filter?: Record<string, string | number | boolean>;
  };
  try {
    body = (await request.json()) as typeof body;
  } catch {
    return json({ ok: false, error: "Invalid JSON body" }, 400);
  }

  const query = body.query?.trim();
  if (!query) {
    return json({ ok: false, error: "query is required" }, 400);
  }

  const topK = Math.min(Math.max(body.topK ?? 5, 1), 50);

  try {
    const [vector] = await embedTexts(env.AI, [query]);
    if (!vector) {
      return json({ ok: false, error: "Failed to generate query embedding" }, 500);
    }

    const filter = buildQueryFilter(body.filter);
    const result = await env.VECTORIZE.query(vector, {
      topK,
      returnMetadata: "all",
      ...(filter ? { filter } : {}),
    });

    const matches = (result.matches ?? []).map((m) => ({
      id: m.id,
      score: m.score,
      metadata: m.metadata ?? {},
    }));

    return json({ ok: true, matches });
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    return json({ ok: false, error: message }, 500);
  }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname.replace(/\/+$/, "") || "/";

    if (request.method === "GET" && path === "/health") {
      return json({ ok: true, service: "anime-japanese-lab-vector-ingest" });
    }

    if (request.method === "POST" && path === "/admin/upsert") {
      return handleUpsert(request, env);
    }

    if (request.method === "POST" && path === "/query") {
      return handleQuery(request, env);
    }

    return json({ ok: false, error: "Not found" }, 404);
  },
} satisfies ExportedHandler<Env>;
