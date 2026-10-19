import { screen, waitFor } from "@testing-library/react";
import { delay, http } from "msw";
import { problem } from "../../mocks/problem";
import { server } from "../../test-setup";
import { issueMockLink, renderApp, TOKEN } from "../../test/renderApp";

// LGN-09…LGN-12 and FLA-37 (DOC-44 §9, DOC-83 FL-02, FL-04).

function countRequests(path: string) {
  const calls: string[] = [];
  server.events.on("request:start", ({ request }) => {
    if (new URL(request.url).pathname === `/api/v1${path}`) calls.push(request.method);
  });
  return calls;
}

afterEach(() => server.events.removeAllListeners());

describe("AuthCallback", () => {
  it("LGN-09/10: drops the token from the URL, verifies once, loads /me and replaces to returnTo", async () => {
    const verify = countRequests("/auth/verify");
    const me = countRequests("/me");
    const token = await issueMockLink("an@example.com", "/me/tickets");
    const { router } = await renderApp(`/auth/callback?token=${token}`);
    expect(
      await screen.findByRole("heading", { name: "Đang xác minh đường dẫn" }),
    ).toBeInTheDocument();
    await waitFor(() => expect(router.state.location.pathname).toBe("/me/tickets"));
    expect(verify).toEqual(["POST"]);
    expect(me.filter((method) => method === "GET").length).toBeGreaterThanOrEqual(1);
    expect(router.state.historyAction).toBe("REPLACE");
    // the mock API keeps its own server-side state under tb.mock.*; the app stores nothing
    const appKeys = (storage: Storage) =>
      Object.keys(storage).filter((key) => !key.startsWith("tb.mock."));
    for (const storage of [localStorage, sessionStorage]) {
      expect(
        appKeys(storage)
          .map((key) => storage.getItem(key))
          .join(),
      ).not.toMatch(/csrf|Qm9Z/i);
    }
  });

  it("LGN-10: a guest probe still in flight when verify succeeds does not count as the answer", async () => {
    // The boot GET /me leaves before the session exists and answers 401 after verify succeeded.
    let probe = true;
    server.use(
      http.get("*/api/v1/me", async () => {
        if (!probe) return undefined;
        probe = false;
        await delay(150);
        return problem(401, "UNAUTHENTICATED");
      }),
    );
    const token = await issueMockLink("an@example.com", "/me/tickets");
    const { router } = await renderApp(`/auth/callback?token=${token}`);
    await waitFor(() => expect(router.state.location.pathname).toBe("/me/tickets"));
    expect(await screen.findByRole("heading", { name: "Vé của tôi" })).toBeInTheDocument();
  });

  it("FLA-37: with a slow guest probe the cookie language still wins after sign-in", async () => {
    let probe = true;
    server.use(
      http.get("*/api/v1/me", async () => {
        if (!probe) return undefined;
        probe = false;
        await delay(150);
        return problem(401, "UNAUTHENTICATED");
      }),
    );
    const token = await issueMockLink("an@example.com", "/me/tickets");
    document.cookie = "tb_lang=en; Path=/";
    await renderApp(`/auth/callback?token=${token}`, { locale: "en" });
    expect(await screen.findByRole("heading", { name: "My tickets" })).toBeInTheDocument();
    await new Promise((resolve) => setTimeout(resolve, 300));
    expect(document.documentElement.lang).toBe("en");
    expect(document.cookie).toContain("tb_lang=en");
  });

  it("LGN-11: a used link shows the expired variant with a new-link button", async () => {
    const token = await issueMockLink("an@example.com");
    await fetch(`${location.origin}/api/v1/auth/verify`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token }),
    });
    await renderApp(`/auth/callback?token=${token}`);
    expect(
      await screen.findByRole("heading", { name: "Đường dẫn không còn dùng được" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Gửi đường dẫn mới" })).toHaveAttribute(
      "href",
      "/login",
    );
  });

  it.each(["", "?token=abc"])(
    "LGN-12: %s shows the expired variant without calling verify",
    async (query) => {
      const verify = countRequests("/auth/verify");
      await renderApp(`/auth/callback${query}`);
      await screen.findByRole("heading", { name: "Đường dẫn không còn dùng được" });
      expect(verify).toHaveLength(0);
    },
  );

  it("LGN-11: an unknown well-formed token is invalid too", async () => {
    await renderApp(`/auth/callback?token=${TOKEN}`);
    expect(
      await screen.findByRole("heading", { name: "Đường dẫn không còn dùng được" }),
    ).toBeInTheDocument();
  });

  it("FLA-37: a tb_lang cookie that differs from the account wins and is saved", async () => {
    const patches: string[] = [];
    server.events.on("request:start", async ({ request }) => {
      if (request.method === "PATCH") patches.push(await request.clone().text());
    });
    const token = await issueMockLink("an@example.com");
    document.cookie = "tb_lang=en; Path=/";
    const { router } = await renderApp(`/auth/callback?token=${token}`, { locale: "en" });
    await waitFor(() => expect(router.state.location.pathname).toBe("/"));
    await waitFor(() => expect(patches).toEqual(['{"locale":"en"}']));
    expect(document.documentElement.lang).toBe("en");
  });
});
