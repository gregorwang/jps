export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

export class UpstreamAsrError extends ApiError {
  constructor(message = "语音识别服务暂时不可用") {
    super(502, "asr_unavailable", message);
    this.name = "UpstreamAsrError";
  }
}
