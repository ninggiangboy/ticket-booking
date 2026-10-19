import type { components } from "./schema";

export type Problem = components["schemas"]["Problem"] & {
  errors?: components["schemas"]["FieldError"][];
  [extension: string]: unknown;
};

/**
 * Failed API call. `status` is null when no response arrived (network error). The UI picks
 * messages from `code` (DR-136), never from the server's `title`/`detail`.
 */
export class ApiError extends Error {
  readonly status: number | null;
  readonly code: string | null;
  readonly requestId: string | null;
  readonly retryAfterSeconds: number | null;
  readonly problem: Problem | null;

  constructor(init: {
    status: number | null;
    code?: string | null;
    requestId?: string | null;
    retryAfterSeconds?: number | null;
    problem?: Problem | null;
    cause?: unknown;
  }) {
    super(init.code ?? (init.status === null ? "NETWORK_ERROR" : `HTTP_${init.status}`), {
      cause: init.cause,
    });
    this.name = "ApiError";
    this.status = init.status;
    this.code = init.code ?? null;
    this.requestId = init.requestId ?? null;
    this.retryAfterSeconds = init.retryAfterSeconds ?? null;
    this.problem = init.problem ?? null;
  }

  get isNetwork(): boolean {
    return this.status === null;
  }

  /** Overload, rate limit or no response: the cases that retry on their own (DOC-38 §8.2). */
  get isTransient(): boolean {
    return this.status === null || this.status === 429 || this.status === 503;
  }

  static fromResponse(response: Response, body: unknown): ApiError {
    const problem = isProblem(body) ? body : null;
    const header = response.headers.get("Retry-After");
    const fromHeader = header !== null && /^\d+$/.test(header) ? Number(header) : null;
    return new ApiError({
      status: response.status,
      code: problem?.code ?? null,
      requestId: problem?.requestId ?? response.headers.get("X-Request-Id"),
      retryAfterSeconds:
        fromHeader ??
        (typeof problem?.retryAfterSeconds === "number" ? problem.retryAfterSeconds : null),
      problem,
    });
  }
}

function isProblem(body: unknown): body is Problem {
  return typeof body === "object" && body !== null && typeof (body as Problem).code === "string";
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}
