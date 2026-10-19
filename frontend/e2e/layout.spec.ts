import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "./support";

// LGN-19, LGN-20, ERP-01, ERP-20, ERP-21, DS-07, DS-10.

test("error-404-unknown-route: E1 keeps the URL and leads back to events", async ({
  page,
  text,
}) => {
  await page.goto("/khong-co-trang-nay");
  await expect(
    page.getByRole("heading", { name: text("errors.page.notFound.title") }),
  ).toBeFocused();
  await expect(page).toHaveTitle(new RegExp(text("errors.page.notFound.title")));
  await page.getByRole("link", { name: text("errors.page.notFound.cta") }).click();
  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole("heading", { name: text("events.list.title") })).toBeVisible();
});

test("login keyboard order and axe (LGN-20)", async ({ page, text }) => {
  await page.goto("/login?returnTo=%2Fme%2Ftickets");
  const email = page.getByRole("textbox", { name: text("auth.form.email.label") });
  await expect(email).toBeFocused();
  // Tab around the whole page once, then read the order from the skip link onwards.
  const visited: string[] = [];
  for (let i = 0; i < 12; i += 1) {
    await page.keyboard.press("Tab");
    visited.push(
      await page.evaluate(() => {
        const el = document.activeElement as HTMLElement | null;
        return (
          el?.getAttribute("aria-label") ??
          el?.getAttribute("name") ??
          el?.textContent?.trim() ??
          ""
        );
      }),
    );
  }
  const order = visited.slice(visited.indexOf(text("common.nav.skipToContent")));
  const at = (label: string) => {
    const index = order.indexOf(label);
    expect(index, `${label} in ${order.join(" | ")}`).toBeGreaterThanOrEqual(0);
    return index;
  };
  expect(at(text("common.lang.vi"))).toBeLessThan(at(text("common.nav.toEvents")));
  expect(at(text("common.nav.toEvents"))).toBeLessThan(at("email"));
  expect(at("email")).toBeLessThan(at(text("auth.form.submit")));

  await email.fill("an@example.com");
  await email.press("Enter");
  await expect(page.getByRole("heading", { name: text("auth.sent.title") })).toBeFocused();
  const results = await new AxeBuilder({ page }).analyze();
  expect(results.violations).toEqual([]);
});

test("pages pass axe: sign-in form, expired link, E1", async ({ page }) => {
  for (const path of ["/login?reason=session_expired", "/auth/callback?token=abc", "/nope"]) {
    await page.goto(path);
    await expect(page.locator("main h1")).toBeVisible();
    const results = await new AxeBuilder({ page }).analyze();
    expect(results.violations, path).toEqual([]);
  }
});

test.describe("phone, 390 px (LGN-19, ERP-20)", () => {
  test.use({ viewport: { width: 390, height: 844 } });

  for (const path of ["/login", "/nope"]) {
    test(`no horizontal scroll and 44 px targets on ${path}`, async ({ page }) => {
      await page.goto(path);
      await expect(page.locator("main h1")).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
        390,
      );
      const small = await page.evaluate(() =>
        Array.from(document.querySelectorAll<HTMLElement>("button, a, input"))
          .filter((el) => el.offsetParent !== null && !el.closest("p"))
          .map((el) => ({ el: el.outerHTML.slice(0, 80), box: el.getBoundingClientRect() }))
          .filter(({ box }) => box.height < 44 || box.width < 44)
          .map(({ el }) => el),
      );
      expect(small).toEqual([]);
    });
  }

  test("the primary button spans the card", async ({ page, text }) => {
    await page.goto("/login");
    const button = await page.getByRole("button", { name: text("auth.form.submit") }).boundingBox();
    const field = await page
      .getByRole("textbox", { name: text("auth.form.email.label") })
      .boundingBox();
    expect(Math.round(button!.width)).toBe(Math.round(field!.width));
  });
});

test("language switch updates <html lang> without reloading, fonts stay local (DS-07, DS-10)", async ({
  page,
}) => {
  const external: string[] = [];
  page.on("request", (request) => {
    if (/fonts\.(googleapis|gstatic)\.com/.test(request.url())) external.push(request.url());
  });
  await page.goto("/");
  const marker = await page.evaluate(
    () => ((window as unknown as { marker: number }).marker = Math.random()),
  );
  const target = (await page.evaluate(() => document.documentElement.lang)) === "vi" ? "en" : "vi";
  await page
    .getByRole("button", { name: target === "en" ? "English" : "Tiếng Việt", exact: true })
    .click();
  await expect(page.locator("html")).toHaveAttribute("lang", target);
  expect(await page.evaluate(() => (window as unknown as { marker: number }).marker)).toBe(marker);
  const loaded = await page.evaluate(
    async () => (await document.fonts.load('16px "Be Vietnam Pro"')).length,
  );
  expect(loaded).toBeGreaterThan(0);
  expect(external).toEqual([]);
});
