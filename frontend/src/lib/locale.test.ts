import {
  matchBrowserLocale,
  readLocaleCookie,
  resolveGuestLocale,
  writeLocaleCookie,
} from "./locale";

describe("guest locale (DOC-31 §2)", () => {
  it("matches browser languages by prefix in preference order", () => {
    expect(matchBrowserLocale(["en-US", "en"])).toBe("en");
    expect(matchBrowserLocale(["vi-VN"])).toBe("vi");
    expect(matchBrowserLocale(["fr-FR", "fr"])).toBeNull();
    expect(matchBrowserLocale(["de", "en-GB", "vi"])).toBe("en");
  });

  it("prefers the tb_lang cookie over the browser (I18N-02)", () => {
    writeLocaleCookie("vi");
    vi.spyOn(navigator, "languages", "get").mockReturnValue(["en-US"]);
    expect(resolveGuestLocale()).toBe("vi");
  });

  it("falls back to vi for unsupported browsers (I18N-03)", () => {
    vi.spyOn(navigator, "languages", "get").mockReturnValue(["fr-FR", "fr"]);
    expect(resolveGuestLocale()).toBe("vi");
  });

  it("ignores an unsupported cookie value", () => {
    document.cookie = "tb_lang=de; Path=/";
    expect(readLocaleCookie()).toBeNull();
  });
});
