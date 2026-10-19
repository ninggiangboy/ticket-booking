import {
  expect,
  latestLink,
  pathOf,
  REAL,
  requestLink,
  test,
  textFor,
  uniqueEmail,
  type Lang,
} from "./support";

// DOC-44 §10 and the P1-10 acceptance: email → link → back to returnTo; a used link shows
// "This link no longer works"; both locales.

test("login-happy-path: the link opens in a new tab, returns to returnTo, the first tab follows", async ({
  page,
  context,
  text,
}) => {
  const email = uniqueEmail();
  await page.goto("/login?returnTo=%2Fme%2Ftickets");
  await expect(page.getByRole("heading", { name: text("auth.form.title") })).toBeVisible();
  await requestLink(page, text, email);
  await expect(page.getByTestId("sent-body")).toContainText(email);

  const link = await latestLink(page, email);
  const tab = await context.newPage();
  await tab.goto(pathOf(link));
  await expect(tab).toHaveURL(/\/me\/tickets$/);
  await expect(tab.getByRole("heading", { name: text("tickets.title") })).toBeVisible();
  await expect(
    tab.getByRole("button", { name: new RegExp(text("common.nav.account")) }),
  ).toBeVisible();

  // LGN-18: back on the first tab, it notices the session and moves on.
  await page.bringToFront();
  await page.evaluate(() => document.dispatchEvent(new Event("visibilitychange")));
  await expect(page).toHaveURL(/\/me\/tickets$/);
});

test("login-link-single-use: opening the same link twice shows the expired variant", async ({
  page,
  text,
}) => {
  const email = uniqueEmail();
  await page.goto("/login");
  await requestLink(page, text, email);
  const link = await latestLink(page, email);

  await page.goto(pathOf(link));
  await expect(page).toHaveURL(/\/$/);
  await page.getByRole("button", { name: new RegExp(text("common.nav.account")) }).click();
  await page.getByRole("menuitem", { name: text("common.nav.signOut") }).click();
  await expect(page.getByText(text("auth.signedOut.notice"))).toBeVisible();

  await page.goto(pathOf(link));
  await expect(page.getByRole("heading", { name: text("auth.invalid.title") })).toBeVisible();
  await expect(page).toHaveURL(/\/auth\/callback$/);
  await page.getByRole("link", { name: text("auth.invalid.cta") }).click();
  await expect(page).toHaveURL(/\/login$/);
});

test("login-link-superseded: only the newest link works", async ({ page, text }) => {
  const email = uniqueEmail();
  await page.goto("/login");
  await requestLink(page, text, email);
  const first = await latestLink(page, email);
  await page.getByRole("button", { name: text("auth.sent.resend") }).click();
  await expect.poll(() => latestLink(page, email)).not.toBe(first);
  const second = await latestLink(page, email);

  await page.goto(pathOf(first));
  await expect(page.getByRole("heading", { name: text("auth.invalid.title") })).toBeVisible();
  await page.goto(pathOf(second));
  await expect(
    page.getByRole("button", { name: new RegExp(text("common.nav.account")) }),
  ).toBeVisible();
});

test("login-throttle: too many requests show the limit notice", async ({ page, text }) => {
  await page.goto("/login");
  if (REAL) {
    const email = uniqueEmail();
    for (let i = 0; i < 3; i += 1) {
      await requestLink(page, text, email);
      await page.getByRole("button", { name: text("auth.sent.otherEmail") }).click();
    }
    await requestLink(page, text, email);
  } else {
    await requestLink(page, text, uniqueEmail("+throttled"));
  }
  await expect(page.getByText(text("auth.throttled.title"))).toBeVisible();
});

test("login-smtp-down: the form stays with the address and an error", async ({ page, text }) => {
  test.skip(REAL, "needs Mailpit stopped; run by hand against the real stack");
  const email = uniqueEmail("+smtpdown");
  await page.goto("/login");
  await page.getByRole("textbox", { name: text("auth.form.email.label") }).fill(email);
  await page.getByRole("button", { name: text("auth.form.submit") }).click();
  await expect(page.getByText(text("errors.email_provider_unavailable.title"))).toBeVisible();
  await expect(page.getByRole("textbox", { name: text("auth.form.email.label") })).toHaveValue(
    email,
  );
});

test("logout: signing out lands on the signed-out variant and forgets the account", async ({
  page,
  text,
}) => {
  const email = uniqueEmail();
  await page.goto("/login");
  await requestLink(page, text, email);
  await page.goto(pathOf(await latestLink(page, email)));
  await page.getByRole("button", { name: new RegExp(text("common.nav.account")) }).click();
  await page.getByRole("menuitem", { name: text("common.nav.signOut") }).click();
  await expect(page).toHaveURL(/\/login\?reason=signed_out$/);
  await expect(page.getByRole("heading", { name: text("auth.form.title.again") })).toBeVisible();
  await expect(page.getByTestId("after-sign-in")).toContainText(text("auth.after.tickets"));
  await page.goto("/me/tickets");
  await expect(page).toHaveURL(/\/login\?returnTo=%2Fme%2Ftickets$/);
});

test("session-expired: a 401 mid-session returns to the same page after signing in again", async ({
  page,
  lang,
  text,
}) => {
  test.skip(REAL, "revoking a session needs psql; FLA-27 covers it against the real stack");
  const email = uniqueEmail();
  await page.goto("/login?returnTo=%2Fme%2Ftickets");
  await requestLink(page, text, email);
  await page.goto(pathOf(await latestLink(page, email)));
  await expect(page).toHaveURL(/\/me\/tickets$/);

  // The server forgets the session (like UPDATE session SET revoked_at = now()); the next call
  // that needs it is the language switch, which then answers 401.
  await page.evaluate(() => {
    const state = JSON.parse(localStorage.getItem("tb.mock.auth")!);
    state.session = null;
    localStorage.setItem("tb.mock.auth", JSON.stringify(state));
  });
  const other: Lang = lang === "vi" ? "en" : "vi";
  const otherText = textFor(other);
  await page.getByRole("button", { name: otherText(`common.lang.${other}`), exact: true }).click();
  await expect(page).toHaveURL(/\/login\?returnTo=%2Fme%2Ftickets&reason=session_expired$/);
  await expect(page.getByRole("alert")).toContainText(otherText("auth.sessionOver.notice"));

  await requestLink(page, otherText, email);
  await page.goto(pathOf(await latestLink(page, email)));
  await expect(page).toHaveURL(/\/me\/tickets$/);
  await expect(page.getByRole("heading", { name: otherText("tickets.title") })).toBeVisible();
});
