import { ApiError } from "../errors";

export async function parseBoundedFormData(request: Request, maxBytes: number): Promise<FormData> {
  const contentType = request.headers.get("Content-Type") ?? "";
  if (!contentType.toLowerCase().startsWith("multipart/form-data")) {
    throw new ApiError(400, "bad_request", "Content-Type 必须为 multipart/form-data");
  }

  const declaredLength = request.headers.get("Content-Length");
  if (declaredLength !== null) {
    const parsed = Number(declaredLength);
    if (!Number.isFinite(parsed) || parsed < 0) throw new ApiError(400, "bad_request", "Content-Length 无效");
    if (parsed > maxBytes) throw new ApiError(413, "audio_too_large", "上传内容超过大小限制");
  }
  if (request.body === null) throw new ApiError(400, "bad_request", "请求体为空");

  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let total = 0;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > maxBytes) {
      await reader.cancel("request body too large");
      throw new ApiError(413, "audio_too_large", "上传内容超过大小限制");
    }
    chunks.push(value);
  }

  const body = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    body.set(chunk, offset);
    offset += chunk.byteLength;
  }
  const bounded = new Request(request.url, { method: request.method, headers: request.headers, body });
  try {
    return await bounded.formData();
  } catch {
    throw new ApiError(400, "bad_request", "无法解析 multipart 请求体");
  }
}
