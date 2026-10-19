import { http, HttpResponse } from "msw";
import { apiHeaders, problem } from "./problem";

// Mock of E-01…E-05 (master plan §7.4). It replays the DOC-37 examples; the only state is which
// links were issued and whether this browser is signed in, kept in localStorage so a link opened in
// another tab works like the real flow. Scenarios are chosen by the email address:
//   …+throttled@…   202 with X-Magic-Link-Throttled: 1
//   …+smtpdown@…    503 EMAIL_PROVIDER_UNAVAILABLE
//   …+ratelimit@…   429 RATE_LIMITED, Retry-After 6
//   …+invalid@…     422 VALIDATION_FAILED (invalid_email)
//   …+servererror@… 500 INTERNAL_ERROR
//   …+organizer@…   account with an organizer profile
// The latest link is printed to the console and stored in localStorage["tb.mock.lastLink"].

const STATE = "tb.mock.auth";
export const LAST_LINK = "tb.mock.lastLink";

interface MockState {
  links: Record<string, { email: string; returnTo: string; locale: string; used: boolean }>;
  latest: Record<string, string>;
  accounts: Record<string, { locale: string }>;
  session: { email: string; csrfToken: string } | null;
}

function load(): MockState {
  try {
    const raw = localStorage.getItem(STATE);
    if (raw) return JSON.parse(raw) as MockState;
  } catch {
    // fall through
  }
  return { links: {}, latest: {}, accounts: {}, session: null };
}

function save(state: MockState): void {
  localStorage.setItem(STATE, JSON.stringify(state));
}

export function resetMockAuth(): void {
  localStorage.removeItem(STATE);
  localStorage.removeItem(LAST_LINK);
}

function randomToken(bytes: number): string {
  const data = crypto.getRandomValues(new Uint8Array(bytes));
  return btoa(String.fromCharCode(...data))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

function me(state: MockState) {
  const session = state.session!;
  const organizer = session.email.includes("+organizer@")
    ? { organizerId: "0199a000-2222-7333-8444-555566667777", name: "Giao Mùa Studio" }
    : null;
  return {
    userId: "0199a000-1111-7222-8333-444455556666",
    email: session.email,
    locale: state.accounts[session.email]?.locale ?? "vi",
    roles: organizer ? ["BUYER", "ORGANIZER"] : ["BUYER"],
    organizer,
    csrfToken: session.csrfToken,
    serverTime: Date.now(),
  };
}

function safeReturnTo(value: unknown): string {
  return typeof value === "string" &&
    value.length <= 512 &&
    value.startsWith("/") &&
    !value.startsWith("//") &&
    !value.startsWith("/\\")
    ? value
    : "/";
}

function csrfOk(request: Request, state: MockState): boolean {
  return request.headers.get("X-CSRF-Token") === state.session?.csrfToken;
}

export const authHandlers = [
  http.post("*/api/v1/auth/magic-link", async ({ request }) => {
    const body = (await request.json()) as { email?: string; returnTo?: string; locale?: string };
    const email = String(body.email ?? "")
      .trim()
      .toLowerCase();
    if (email.includes("+invalid@") || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      return problem(422, "VALIDATION_FAILED", {
        errors: [{ field: "email", rule: "invalid_email", params: null }],
      });
    }
    if (email.includes("+ratelimit@")) {
      return problem(429, "RATE_LIMITED", { retryAfterSeconds: 6 }, { "Retry-After": "6" });
    }
    if (email.includes("+servererror@")) return problem(500, "INTERNAL_ERROR");
    if (email.includes("+throttled@")) {
      return HttpResponse.json(
        { status: "THROTTLED", email, expiresInSeconds: 0 },
        { status: 202, headers: apiHeaders({ "X-Magic-Link-Throttled": "1" }) },
      );
    }
    const state = load();
    const token = randomToken(32);
    state.links[token] = {
      email,
      returnTo: safeReturnTo(body.returnTo),
      locale: body.locale === "en" ? "en" : "vi",
      used: false,
    };
    state.latest[email] = token;
    save(state);
    if (email.includes("+smtpdown@")) {
      return problem(
        503,
        "EMAIL_PROVIDER_UNAVAILABLE",
        { retryAfterSeconds: 30 },
        { "Retry-After": "30" },
      );
    }
    const link = `${location.origin}/auth/callback?token=${token}`;
    localStorage.setItem(LAST_LINK, link);
    console.info(`[mock api] magic link for ${email}: ${link}`);
    return HttpResponse.json(
      { status: "SENT", email, expiresInSeconds: 900 },
      { status: 202, headers: apiHeaders() },
    );
  }),

  http.post("*/api/v1/auth/verify", async ({ request }) => {
    const { token } = (await request.json()) as { token?: string };
    const state = load();
    const link = token ? state.links[token] : undefined;
    if (!token || !link || link.used || state.latest[link.email] !== token) {
      return problem(401, "LOGIN_LINK_INVALID");
    }
    link.used = true;
    state.accounts[link.email] ??= { locale: link.locale };
    state.session = { email: link.email, csrfToken: randomToken(32) };
    save(state);
    return HttpResponse.json({ ...me(state), returnTo: link.returnTo }, { headers: apiHeaders() });
  }),

  http.post("*/api/v1/auth/logout", ({ request }) => {
    const state = load();
    if (state.session && !csrfOk(request, state)) return problem(403, "CSRF_TOKEN_INVALID");
    state.session = null;
    save(state);
    return new HttpResponse(null, { status: 204, headers: apiHeaders() });
  }),

  http.get("*/api/v1/me", () => {
    const state = load();
    if (!state.session) return problem(401, "UNAUTHENTICATED");
    return HttpResponse.json(me(state), { headers: apiHeaders() });
  }),

  http.patch("*/api/v1/me", async ({ request }) => {
    const state = load();
    if (!state.session) return problem(401, "UNAUTHENTICATED");
    if (!csrfOk(request, state)) return problem(403, "CSRF_TOKEN_INVALID");
    const { locale } = (await request.json()) as { locale?: string };
    if (locale !== "vi" && locale !== "en") {
      return problem(422, "VALIDATION_FAILED", {
        errors: [{ field: "locale", rule: "invalid_locale", params: null }],
      });
    }
    state.accounts[state.session.email] = { locale };
    save(state);
    return HttpResponse.json(me(state), { headers: apiHeaders() });
  }),
];
