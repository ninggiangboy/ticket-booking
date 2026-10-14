// Locale selection shared with the backend LocaleResolver (DOC-31 §2):
// account locale (from GET /me) > cookie tb_lang > browser languages > vi.

export const SUPPORTED_LOCALES = ["vi", "en"] as const;
export type Locale = (typeof SUPPORTED_LOCALES)[number];
export const DEFAULT_LOCALE: Locale = "vi";

const COOKIE = "tb_lang";
const ONE_YEAR_SECONDS = 31_536_000;

export function isLocale(value: unknown): value is Locale {
  return typeof value === "string" && (SUPPORTED_LOCALES as readonly string[]).includes(value);
}

export function readLocaleCookie(): Locale | null {
  const match = document.cookie.match(/(?:^|;\s*)tb_lang=([^;]*)/);
  const value = match?.[1] ? decodeURIComponent(match[1]) : null;
  return isLocale(value) ? value : null;
}

export function writeLocaleCookie(locale: Locale): void {
  document.cookie = `${COOKIE}=${locale}; Path=/; SameSite=Lax; Max-Age=${ONE_YEAR_SECONDS}`;
}

/** Prefix match in the browser's preference order, like Accept-Language q-weights. */
export function matchBrowserLocale(languages: readonly string[]): Locale | null {
  for (const tag of languages) {
    const prefix = tag.toLowerCase().split("-")[0];
    if (isLocale(prefix)) return prefix;
  }
  return null;
}

export function resolveGuestLocale(): Locale {
  return readLocaleCookie() ?? matchBrowserLocale(navigator.languages ?? []) ?? DEFAULT_LOCALE;
}
