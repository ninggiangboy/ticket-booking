import createClient, { type Middleware } from "openapi-fetch";
import { recordServerTime } from "../lib/serverTime";
import { authStore } from "./authStore";
import { ApiError } from "./errors";
import type { paths } from "./schema";

// Every API call goes through this client (DOC-12 §6). Interceptors: Accept-Language, X-CSRF-Token
// with one retry on CSRF_TOKEN_INVALID (DR-85), X-Server-Time offset (DR-66) and the global
// 401 UNAUTHENTICATED handler (DOC-38 §8.5).

const SAFE_METHODS = new Set(["GET", "HEAD"]);

/** Calls whose 401 is an answer, not an expired session (guest probe, sign-in, sign-out). */
const OWN_401 = new Set([
  "POST /auth/verify",
  "POST /auth/magic-link",
  "POST /auth/logout",
  "GET /me",
]);

let currentLocale: () => string = () => "vi";
let onUnauthenticated: () => void = () => {};

export function configureApi(options: {
  locale: () => string;
  onUnauthenticated: () => void;
}): void {
  currentLocale = options.locale;
  onUnauthenticated = options.onUnauthenticated;
}

const sentAt = new Map<string, number>();
const retryable = new Map<string, Request>();

const interceptors: Middleware = {
  onRequest({ request, id }) {
    request.headers.set("Accept-Language", currentLocale());
    if (!SAFE_METHODS.has(request.method)) {
      const token = authStore.csrfToken();
      if (token) request.headers.set("X-CSRF-Token", token);
      retryable.set(id, request.clone());
    }
    sentAt.set(id, Date.now());
    return request;
  },

  async onResponse({ request, response, schemaPath, id }) {
    const t0 = sentAt.get(id);
    sentAt.delete(id);
    const original = retryable.get(id);
    retryable.delete(id);
    const serverTime = Number(response.headers.get("X-Server-Time"));
    if (t0 !== undefined && serverTime > 0) recordServerTime(serverTime, t0, Date.now());

    if (response.ok) return response;
    const code = await problemCode(response);

    if (response.status === 403 && code === "CSRF_TOKEN_INVALID" && original) {
      return retryWithFreshCsrf(original);
    }
    if (
      response.status === 401 &&
      code === "UNAUTHENTICATED" &&
      !OWN_401.has(`${request.method} ${schemaPath}`)
    ) {
      onUnauthenticated();
    }
    return response;
  },
};

async function problemCode(response: Response): Promise<string | null> {
  try {
    const body: unknown = await response.clone().json();
    return typeof body === "object" && body !== null && "code" in body
      ? String((body as { code: unknown }).code)
      : null;
  } catch {
    return null;
  }
}

/** DR-85: fetch a new token with GET /me and send the same request once more. */
async function retryWithFreshCsrf(original: Request): Promise<Response> {
  const me = await api.GET("/me");
  if (me.data) authStore.setCsrfToken(me.data.csrfToken);
  const retry = new Request(original);
  const token = authStore.csrfToken();
  if (token) retry.headers.set("X-CSRF-Token", token);
  return fetch(retry);
}

export const api = createClient<paths>({
  baseUrl: `${globalThis.location?.origin ?? ""}/api/v1`,
  // Read fetch at call time so tests and MSW can replace it.
  fetch: (request) => globalThis.fetch(request),
});
api.use(interceptors);

type Result<T> = { data?: T; error?: unknown; response: Response };

/** Unwraps an openapi-fetch result; throws ApiError for problem responses and network failures. */
export async function call<T>(
  pending: Promise<Result<T>>,
): Promise<{ data: T; response: Response }> {
  let result: Result<T>;
  try {
    result = await pending;
  } catch (cause) {
    throw new ApiError({ status: null, cause });
  }
  if (!result.response.ok) throw ApiError.fromResponse(result.response, result.error);
  return { data: result.data as T, response: result.response };
}
