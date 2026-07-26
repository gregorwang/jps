import type { ApiErrorBody } from "./contracts";
import { loadConfig } from "./config";
import { ApiError } from "./errors";
import { assertAllowedOrigin, corsHeaders, withCors } from "./http/cors";
import { handleEvaluate } from "./routes/evaluate";
import { cleanupExpiredAttempts } from "./storage/attempts";

export default {
  async fetch(request, env): Promise<Response> {
    const requestId = crypto.randomUUID();
    let origin: string | null = null;
    try {
      const config = loadConfig(env);
      origin = assertAllowedOrigin(request, config.allowedOrigins);
      if (request.method === "OPTIONS") {
        return new Response(null, { status: 204, headers: corsHeaders(origin) });
      }
      const url = new URL(request.url);
      let response: Response;
      if (request.method === "GET" && url.pathname === "/health") {
        response = Response.json(
          {
            ok: true,
            service: "ajl-pronunciation-api",
            environment: env.ENVIRONMENT,
            scorerVersion: env.SCORER_VERSION,
            primaryModel: "@cf/deepgram/nova-3",
            secondaryModel: "@cf/openai/whisper-large-v3-turbo",
            timestamp: new Date().toISOString(),
          },
          { headers: { "Cache-Control": "no-store" } },
        );
      } else if (request.method === "POST" && url.pathname === "/v1/pronunciation/evaluate") {
        response = await handleEvaluate(request, env, config, requestId);
      } else {
        throw new ApiError(404, "not_found", "接口不存在");
      }
      return withCors(response, origin);
    } catch (error) {
      const apiError = error instanceof ApiError ? error : new ApiError(500, "internal_error", "服务器内部错误");
      console.error(
        JSON.stringify({
          message: "request_failed",
          requestId,
          method: request.method,
          path: new URL(request.url).pathname,
          status: apiError.status,
          code: apiError.code,
          error: error instanceof Error ? error.message : String(error),
        }),
      );
      const body: ApiErrorBody = {
        ok: false,
        error: { code: apiError.code, message: apiError.message, requestId },
      };
      return withCors(Response.json(body, { status: apiError.status, headers: { "Cache-Control": "no-store" } }), origin);
    }
  },

  async scheduled(_controller, env): Promise<void> {
    const deleted = await cleanupExpiredAttempts(env.DB);
    console.log(JSON.stringify({ message: "expired_attempts_cleaned", deleted }));
  },
} satisfies ExportedHandler<Env>;
