import { render } from "@testing-library/react";
import i18next from "i18next";
import { createMemoryRouter } from "react-router";
import { App, createQueryClient, wireSessionExpiry } from "../app/App";
import { initI18n } from "../app/i18n";
import { routes } from "../app/router";
import type { Locale } from "../lib/locale";

let ready: Promise<unknown> | null = null;

/** i18next with the real catalogs, every namespace loaded. */
export async function setupI18n(locale: Locale = "vi") {
  ready ??= initI18n(locale);
  await ready;
  await i18next.changeLanguage(locale);
  await i18next.loadNamespaces(["common", "errors", "auth", "events", "tickets"]);
}

/** Renders the whole app (router, providers, i18n with the real catalogs) at `path`. */
export async function renderApp(path: string, options: { locale?: Locale } = {}) {
  await setupI18n(options.locale);
  const queryClient = createQueryClient();
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  wireSessionExpiry(router, queryClient);
  const view = render(<App router={router} queryClient={queryClient} />);
  return { ...view, router, queryClient };
}

/** A valid 43-character token shape. */
export const TOKEN = "q3J2k0wYb7nQ1u8Z5cXfHh3aT9mEoVd4LpS6iNgR2yA";

/** Issues a mock link like E-01 would, and returns its token. */
export async function issueMockLink(email: string, returnTo = "/"): Promise<string> {
  const response = await fetch(`${location.origin}/api/v1/auth/magic-link`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, returnTo, locale: "vi" }),
  });
  if (response.status !== 202) throw new Error(`magic link ${response.status}`);
  const link = localStorage.getItem("tb.mock.lastLink")!;
  return new URL(link).searchParams.get("token")!;
}
