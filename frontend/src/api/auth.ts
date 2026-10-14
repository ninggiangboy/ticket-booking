import type { Locale } from "../lib/locale";
import { authStore } from "./authStore";
import { api, call } from "./client";
import { ApiError } from "./errors";
import type { components } from "./schema";

export type Me = components["schemas"]["Me"];
export type VerifyResult = components["schemas"]["VerifyResponse"];

export interface MagicLinkResult {
  email: string;
  throttled: boolean;
}

/** E-01. Throttling still answers 202, marked by X-Magic-Link-Throttled: 1 (DR-21). */
export async function requestMagicLink(body: {
  email: string;
  returnTo: string;
  locale: Locale;
}): Promise<MagicLinkResult> {
  const { data, response } = await call(api.POST("/auth/magic-link", { body }));
  return {
    email: data.email,
    throttled:
      response.headers.get("X-Magic-Link-Throttled") === "1" || data.status === "THROTTLED",
  };
}

// One POST /auth/verify per token, even when StrictMode mounts the callback twice: a second call
// would get 401 because the token is single use (DR-114). Failed calls without an answer are
// forgotten so "Try again" can send the same token again.
const verifications = new Map<string, Promise<VerifyResult>>();

export function verifyMagicLink(token: string): Promise<VerifyResult> {
  let pending = verifications.get(token);
  if (!pending) {
    pending = call(api.POST("/auth/verify", { body: { token } })).then(({ data }) => {
      authStore.setCsrfToken(data.csrfToken);
      return data;
    });
    pending.catch((error: unknown) => {
      if (!(error instanceof ApiError) || error.isTransient || error.status === 500) {
        verifications.delete(token);
      }
    });
    verifications.set(token, pending);
  }
  return pending;
}

/** E-04. Returns null for a guest (401), which is not an error for the app. */
export async function fetchMe(): Promise<Me | null> {
  try {
    const { data } = await call(api.GET("/me"));
    authStore.setCsrfToken(data.csrfToken);
    return data;
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      authStore.clear();
      return null;
    }
    throw error;
  }
}

/** E-05. */
export async function updateLocale(locale: Locale): Promise<Me> {
  const { data } = await call(api.PATCH("/me", { body: { locale } }));
  return data;
}

/** E-03. Signing out never fails for the user: any error still clears the local session. */
export async function logout(): Promise<void> {
  try {
    await call(api.POST("/auth/logout"));
  } catch {
    // expired or unreachable session: the server forgets it on its own (FL-03 E1, E4)
  } finally {
    authStore.clear();
  }
}
