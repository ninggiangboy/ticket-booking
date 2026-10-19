import { describe, expect, it } from "vitest";
import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { checkCatalogs, placeholders } from "./i18n-check.mjs";

function fixture(files) {
  const root = mkdtempSync(join(tmpdir(), "i18n-"));
  for (const [path, content] of Object.entries(files)) {
    const full = join(root, path);
    mkdirSync(join(full, ".."), { recursive: true });
    writeFileSync(full, typeof content === "string" ? content : JSON.stringify(content));
  }
  return { localesDir: join(root, "src/locales"), srcDir: join(root, "src") };
}

const ok = {
  "src/locales/vi/checkout.json": { "checkout.hold.expired.title": "Hết giờ giữ vé {time}" },
  "src/locales/en/checkout.json": { "checkout.hold.expired.title": "Your hold expired {time}" },
  "src/app.tsx": 't("checkout.hold.expired.title")',
};

describe("i18n:check (DOC-31 §9)", () => {
  it("passes matching catalogs", () => {
    expect(checkCatalogs(fixture(ok)).errors).toEqual([]);
  });

  it("fails when a key exists only in vi (I18N-11, UX-14)", () => {
    const { errors } = checkCatalogs(fixture({ ...ok, "src/locales/en/checkout.json": {} }));
    expect(errors).toContain("checkout: missing in en: checkout.hold.expired.title");
  });

  it("fails on placeholder mismatch, empty values and unknown keys", () => {
    const { errors } = checkCatalogs(
      fixture({
        "src/locales/vi/events.json": {
          "events.list.count": "{count} sự kiện",
          "events.list.title": "",
        },
        "src/locales/en/events.json": {
          "events.list.count": "events",
          "events.list.title": "Events",
        },
        "src/page.tsx": 't("events.list.missing")',
      }),
    );
    expect(errors).toEqual(
      expect.arrayContaining([
        "events: placeholder mismatch events.list.count: vi {count}, en (none)",
        "events: empty value in vi: events.list.title",
        "page.tsx: unknown key events.list.missing",
      ]),
    );
  });

  it("reads top-level ICU arguments only", () => {
    expect(placeholders("{count, plural, one {# vé} other {# vé}} · {name}")).toEqual([
      "count",
      "name",
    ]);
  });
});
