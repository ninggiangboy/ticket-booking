import { expect, test as base, type Page } from "@playwright/test";
import { readFileSync, readdirSync } from "node:fs";
import { fileURLToPath } from "node:url";

// Shared helpers: strings come from the real catalogs so each test runs in `vi` and `en`
// without copying text; magic links come from the mock API or from Mailpit.

export const REAL = !!process.env.E2E_BASE_URL;
const MAILPIT = process.env.E2E_MAILPIT_URL ?? "http://localhost:8025";

export type Lang = "vi" | "en";
export type Text = (key: string, values?: Record<string, string | number>) => string;

function catalog(lang: Lang): Record<string, string> {
  const dir = fileURLToPath(new URL(`../src/locales/${lang}/`, import.meta.url));
  return Object.assign(
    {},
    ...readdirSync(dir).map((file) => JSON.parse(readFileSync(dir + file, "utf8"))),
  );
}

/** Strings of one locale, with simple `{name}` placeholders filled in. */
export function textFor(lang: Lang): Text {
  const strings = catalog(lang);
  return (key, values = {}) => {
    const message = strings[key];
    if (message === undefined) throw new Error(`no string ${key}`);
    return message.replace(/\{(\w+)\}/g, (_, name: string) => String(values[name] ?? `{${name}}`));
  };
}

export const test = base.extend<{ lang: Lang; text: Text }>({
  // eslint-disable-next-line no-empty-pattern -- Playwright fixtures take an object pattern
  lang: async ({}, use, info) => {
    await use((info.project.metadata.lang as Lang) ?? "vi");
  },
  text: async ({ lang }, use) => {
    await use(textFor(lang));
  },
});

export { expect };

export function uniqueEmail(tag = ""): string {
  const id = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`;
  return `e2e-${id}${tag}@example.com`;
}

/** Fills the sign-in form and waits for "Check your inbox". */
export async function requestLink(page: Page, text: Text, email: string) {
  await page.getByRole("textbox", { name: text("auth.form.email.label") }).fill(email);
  await page.getByRole("button", { name: text("auth.form.submit") }).click();
  await expect(page.getByRole("heading", { name: text("auth.sent.title") })).toBeVisible();
}

/** The newest sign-in link sent to `email`. */
export async function latestLink(page: Page, email: string): Promise<string> {
  if (!REAL) {
    const link = await page.evaluate(() => localStorage.getItem("tb.mock.lastLink"));
    if (!link) throw new Error("mock API issued no link");
    return link;
  }
  let found: string | null = null;
  await expect
    .poll(
      async () => {
        const search = await fetch(
          `${MAILPIT}/api/v1/search?query=${encodeURIComponent(`to:"${email}"`)}&limit=1`,
        );
        const { messages } = (await search.json()) as { messages: { ID: string }[] };
        if (!messages[0]) return null;
        const message = await fetch(`${MAILPIT}/api/v1/message/${messages[0].ID}`);
        const { Text } = (await message.json()) as { Text: string };
        found = /https?:\/\/\S+\/auth\/callback\?token=[A-Za-z0-9_-]{43}/.exec(Text)?.[0] ?? null;
        return found;
      },
      { timeout: 15_000 },
    )
    .not.toBeNull();
  return found!;
}

/** Opens `link` keeping only its path, so the mock and the real stack behave alike. */
export function pathOf(link: string): string {
  const url = new URL(link);
  return url.pathname + url.search;
}
