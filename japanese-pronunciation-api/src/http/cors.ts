import { ApiError } from "../errors";

export function assertAllowedOrigin(request: Request, allowedOrigins: ReadonlySet<string>): string | null {
  const origin = request.headers.get("Origin");
  if (origin === null) return null;
  if (!allowedOrigins.has(origin)) throw new ApiError(403, "origin_not_allowed", "请求来源不被允许");
  return origin;
}

export function corsHeaders(origin: string | null): Headers {
  const headers = new Headers({
    "Access-Control-Allow-Headers": "Authorization, Content-Type",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Max-Age": "86400",
    Vary: "Origin",
  });
  if (origin !== null) headers.set("Access-Control-Allow-Origin", origin);
  return headers;
}

export function withCors(response: Response, origin: string | null): Response {
  const headers = new Headers(response.headers);
  for (const [key, value] of corsHeaders(origin)) headers.set(key, value);
  return new Response(response.body, { status: response.status, statusText: response.statusText, headers });
}
