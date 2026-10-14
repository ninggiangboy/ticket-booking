import { fireEvent, screen, waitFor } from "@testing-library/react";
import { http } from "msw";
import { problem } from "../../mocks/problem";
import { server } from "../../test-setup";
import { issueMockLink, renderApp } from "../../test/renderApp";

// Account menu, sign-out and session expiry (FL-03), language switch (FL-04).

async function signIn(email = "an@example.com") {
  const token = await issueMockLink(email);
  await fetch(`${location.origin}/api/v1/auth/verify`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token }),
  });
}

function recordPatches() {
  const bodies: string[] = [];
  server.events.on("request:start", async ({ request }) => {
    if (request.method === "PATCH") bodies.push(await request.clone().text());
  });
  return bodies;
}

afterEach(() => server.events.removeAllListeners());

describe("session", () => {
  it("guests see a sign-in link that returns to the current page", async () => {
    await renderApp("/");
    expect(await screen.findByRole("link", { name: "Đăng nhập" })).toHaveAttribute(
      "href",
      "/login?returnTo=%2F",
    );
  });

  it("a guest opening a signed-in page goes to sign-in with returnTo", async () => {
    const { router } = await renderApp("/me/tickets");
    await screen.findByRole("heading", { name: "Đăng nhập để mua vé" });
    expect(router.state.location.search).toBe("?returnTo=%2Fme%2Ftickets");
  });

  it("account menu: items by role, Esc closes and returns focus", async () => {
    await signIn();
    await renderApp("/");
    const trigger = await screen.findByRole("button", { name: /Tài khoản/ });
    fireEvent.click(trigger);
    expect(screen.getByTestId("account-email")).toHaveTextContent("an@example.com");
    expect(screen.getByRole("menuitem", { name: "Vé của tôi" })).toHaveFocus();
    expect(screen.getByRole("menuitem", { name: "Tạo hồ sơ tổ chức" })).toBeInTheDocument();
    fireEvent.keyDown(screen.getByRole("menu"), { key: "ArrowDown" });
    expect(screen.getByRole("menuitem", { name: "Tạo hồ sơ tổ chức" })).toHaveFocus();
    fireEvent.keyDown(screen.getByRole("menu"), { key: "Escape" });
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  it("organizers get Studio instead of creating a profile", async () => {
    await signIn("an+organizer@example.com");
    await renderApp("/");
    fireEvent.click(await screen.findByRole("button", { name: /Tài khoản/ }));
    expect(screen.getByRole("menuitem", { name: "Studio của bạn" })).toHaveAttribute(
      "href",
      "/studio",
    );
  });

  it("FL-03 (a): sign out sends the CSRF token and lands on the signed-out variant", async () => {
    const headers: (string | null)[] = [];
    server.events.on("request:start", ({ request }) => {
      if (request.url.endsWith("/auth/logout")) headers.push(request.headers.get("X-CSRF-Token"));
    });
    await signIn();
    const { router } = await renderApp("/");
    fireEvent.click(await screen.findByRole("button", { name: /Tài khoản/ }));
    fireEvent.click(screen.getByRole("menuitem", { name: "Đăng xuất" }));
    await screen.findByText("Bạn đã đăng xuất khỏi trình duyệt này.");
    expect(router.state.location.search).toBe("?reason=signed_out");
    expect(headers[0]).toBeTruthy();
  });

  it("FL-03 (b) / FLA-27: a 401 mid-session opens sign-in on session expired with returnTo", async () => {
    await signIn();
    const { router } = await renderApp("/me/tickets");
    await screen.findByRole("heading", { name: "Vé của tôi" });
    server.use(http.patch("*/api/v1/me", () => problem(401, "UNAUTHENTICATED")));
    fireEvent.click(screen.getByRole("button", { name: "English" }));
    await screen.findByText(/Your session has expired/);
    expect(router.state.location.pathname).toBe("/login");
    expect(router.state.location.search).toBe("?returnTo=%2Fme%2Ftickets&reason=session_expired");
  });

  it("FLA-30: a guest switching language sets the cookie, no PATCH, no reload", async () => {
    const patches = recordPatches();
    await renderApp("/");
    fireEvent.click(await screen.findByRole("button", { name: "English" }));
    await screen.findByRole("heading", { name: "Upcoming events" });
    expect(document.cookie).toContain("tb_lang=en");
    expect(document.documentElement.lang).toBe("en");
    expect(screen.getByRole("button", { name: "English" })).toHaveAttribute("aria-pressed", "true");
    expect(patches).toHaveLength(0);
  });

  it("FLA-31: signed in, switching language saves it with PATCH /me", async () => {
    const patches = recordPatches();
    await signIn();
    await renderApp("/");
    await screen.findByRole("button", { name: /Tài khoản/ });
    fireEvent.click(screen.getByRole("button", { name: "English" }));
    await screen.findByRole("heading", { name: "Upcoming events" });
    await waitFor(() => expect(patches).toEqual(['{"locale":"en"}']));
  });

  it("FLA-36: a failed PATCH keeps the new language quietly and is retried on the next load", async () => {
    await signIn();
    server.use(http.patch("*/api/v1/me", () => problem(500, "INTERNAL_ERROR")));
    await renderApp("/");
    await screen.findByRole("button", { name: /Tài khoản/ });
    fireEvent.click(screen.getByRole("button", { name: "English" }));
    await screen.findByRole("heading", { name: "Upcoming events" });
    await waitFor(() => expect(localStorage.getItem("tb.pendingLocale")).toBe("en"));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();

    server.resetHandlers();
    const patches = recordPatches();
    await renderApp("/");
    await waitFor(() => expect(patches).toEqual(['{"locale":"en"}']));
    await waitFor(() => expect(localStorage.getItem("tb.pendingLocale")).toBeNull());
  });
});
