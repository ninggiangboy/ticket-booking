import { act, fireEvent, screen, waitFor } from "@testing-library/react";
import { http } from "msw";
import { problem } from "../../mocks/problem";
import { server } from "../../test-setup";
import { issueMockLink, renderApp } from "../../test/renderApp";

// Acceptance criteria LGN-xx of DOC-44 §9, against the MSW mock of E-01…E-07.

const EVENT = "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35";

function recordMagicLinkRequests() {
  const bodies: Record<string, unknown>[] = [];
  server.events.on("request:start", async ({ request }) => {
    if (request.method === "POST" && request.url.endsWith("/auth/magic-link")) {
      bodies.push((await request.clone().json()) as Record<string, unknown>);
    }
  });
  return bodies;
}

function emailInput() {
  return screen.getByRole("textbox", { name: "Email" });
}

function submit() {
  fireEvent.click(screen.getByRole("button", { name: "Gửi đường dẫn đăng nhập" }));
}

afterEach(() => server.events.removeAllListeners());

describe("Login form", () => {
  it("LGN-01: empty email shows the field error, focuses the field and sends nothing", async () => {
    const sent = recordMagicLinkRequests();
    await renderApp("/login");
    await screen.findByRole("heading", { name: "Đăng nhập để mua vé" });
    submit();
    expect(await screen.findByRole("alert")).toHaveTextContent("Email chưa đúng định dạng.");
    expect(emailInput()).toHaveFocus();
    expect(emailInput()).toHaveAttribute("aria-invalid", "true");
    expect(sent).toHaveLength(0);
  });

  it("LGN-02: rejects an@example and sends a trimmed, lower-cased address", async () => {
    const sent = recordMagicLinkRequests();
    await renderApp("/login");
    fireEvent.change(await screen.findByRole("textbox", { name: "Email" }), {
      target: { value: "an@example" },
    });
    submit();
    expect(await screen.findByText("Email chưa đúng định dạng.")).toBeInTheDocument();
    fireEvent.change(emailInput(), { target: { value: " An@Example.com " } });
    expect(screen.queryByText("Email chưa đúng định dạng.")).not.toBeInTheDocument();
    submit();
    await screen.findByRole("heading", { name: "Kiểm tra hộp thư" });
    expect(sent).toEqual([{ email: "an@example.com", returnTo: "/", locale: "vi" }]);
  });

  it("LGN-03: 202 shows the sent state with the address, focus on h1, resend and other email", async () => {
    await renderApp("/login");
    fireEvent.change(await screen.findByRole("textbox", { name: "Email" }), {
      target: { value: "an@example.com" },
    });
    submit();
    const heading = await screen.findByRole("heading", { name: "Kiểm tra hộp thư" });
    await waitFor(() => expect(heading).toHaveFocus());
    expect(screen.getByTestId("sent-body")).toHaveTextContent("an@example.com");
    expect(screen.getByRole("button", { name: "Gửi lại" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Dùng email khác" })).toBeInTheDocument();
  });

  it("LGN-04: throttled 202 shows sent plus the limit notice", async () => {
    await renderApp("/login");
    fireEvent.change(await screen.findByRole("textbox", { name: "Email" }), {
      target: { value: "an+throttled@example.com" },
    });
    submit();
    await screen.findByRole("heading", { name: "Kiểm tra hộp thư" });
    expect(screen.getByText("Bạn đã yêu cầu quá nhiều lần")).toBeInTheDocument();
    expect(screen.getByText(/tối đa 3 đường dẫn trong 15 phút/)).toBeInTheDocument();
  });

  it("LGN-05: 503 EMAIL_PROVIDER_UNAVAILABLE keeps the form and the address", async () => {
    await renderApp("/login");
    fireEvent.change(await screen.findByRole("textbox", { name: "Email" }), {
      target: { value: "an+smtpdown@example.com" },
    });
    submit();
    expect(await screen.findByText("Chưa gửi được email")).toBeInTheDocument();
    expect(emailInput()).toHaveValue("an+smtpdown@example.com");
    expect(screen.getByRole("button", { name: "Gửi đường dẫn đăng nhập" })).toBeEnabled();
  });

  it("LGN-06: 429 counts down with the button disabled and never resends on its own", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const sent = recordMagicLinkRequests();
    await renderApp("/login");
    fireEvent.change(await screen.findByRole("textbox", { name: "Email" }), {
      target: { value: "an+ratelimit@example.com" },
    });
    submit();
    expect(await screen.findByText(/sau 6 giây/)).toBeInTheDocument();
    const button = screen.getByRole("button", { name: "Gửi đường dẫn đăng nhập" });
    expect(button).toBeDisabled();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000);
    });
    expect(screen.getByText(/sau 4 giây/)).toBeInTheDocument();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(4500);
    });
    expect(button).toBeEnabled();
    expect(sent).toHaveLength(1);
    vi.useRealTimers();
  });

  it("LGN-07: use a different email clears the field and focuses it", async () => {
    await renderApp("/login");
    fireEvent.change(await screen.findByRole("textbox", { name: "Email" }), {
      target: { value: "an@example.com" },
    });
    submit();
    fireEvent.click(await screen.findByRole("button", { name: "Dùng email khác" }));
    expect(emailInput()).toHaveValue("");
    expect(emailInput()).toHaveFocus();
  });

  it("LGN-08: open inbox points to Mailpit in dev, in a new tab without opener", async () => {
    await renderApp("/login");
    fireEvent.change(await screen.findByRole("textbox", { name: "Email" }), {
      target: { value: "an@gmail.com" },
    });
    submit();
    const link = await screen.findByRole("link", { name: "Mở hộp thư" });
    expect(link).toHaveAttribute("href", "http://localhost:8025");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });

  it("LGN-13: an outside returnTo is sent as /", async () => {
    const sent = recordMagicLinkRequests();
    await renderApp(`/login?returnTo=${encodeURIComponent("//evil.example")}`);
    fireEvent.change(await screen.findByRole("textbox", { name: "Email" }), {
      target: { value: "an@example.com" },
    });
    submit();
    await screen.findByRole("heading", { name: "Kiểm tra hộp thư" });
    expect(sent[0]?.returnTo).toBe("/");
  });

  it("LGN-14: reason=signed_out shows the success notice, title again and the tickets note", async () => {
    await renderApp("/login?reason=signed_out");
    expect(await screen.findByRole("heading", { name: "Đăng nhập lại" })).toHaveFocus();
    expect(screen.getByRole("status")).toHaveTextContent("Bạn đã đăng xuất khỏi trình duyệt này.");
    expect(screen.getByTestId("after-sign-in")).toHaveTextContent(
      "Vé đã mua vẫn nằm ở trang Vé của tôi.",
    );
  });

  it("LGN-15: reason=session_expired shows the warning as an alert and the page note", async () => {
    await renderApp(`/login?returnTo=${encodeURIComponent("/checkout/x")}&reason=session_expired`);
    await screen.findByRole("heading", { name: "Đăng nhập lại" });
    expect(screen.getByRole("alert")).toHaveTextContent("Phiên đăng nhập đã hết hạn.");
    expect(screen.getByTestId("after-sign-in")).toHaveTextContent(
      "Bạn quay lại đúng trang đang xem dở.",
    );
  });

  it("LGN-16: with a saved selection the note names the event in bold", async () => {
    localStorage.setItem(
      `tb.selection.${EVENT}`,
      JSON.stringify({ seatIds: [], savedAt: Date.now() }),
    );
    await renderApp(`/login?returnTo=${encodeURIComponent(`/events/${EVENT}/seats`)}`);
    const name = await screen.findByText("Hòa nhạc Giao Mùa");
    expect(name.tagName).toBe("STRONG");
    expect(screen.getByRole("link", { name: "Quay lại sự kiện" })).toHaveAttribute(
      "href",
      `/events/${EVENT}`,
    );
  });

  it("LGN-16: when E-07 fails the note falls back and the form still shows", async () => {
    server.use(http.get("*/api/v1/events/:eventId", () => problem(500, "INTERNAL_ERROR")));
    localStorage.setItem(`tb.selection.${EVENT}`, "{}");
    await renderApp(`/login?returnTo=${encodeURIComponent(`/events/${EVENT}/seats`)}`);
    await waitFor(() =>
      expect(screen.getByTestId("after-sign-in")).toHaveTextContent(
        "Bạn quay lại đúng trang đang xem dở.",
      ),
    );
    expect(emailInput()).toBeInTheDocument();
  });

  it("LGN-17: a signed-in user goes straight to returnTo", async () => {
    const token = await issueMockLink("an@example.com");
    await fetch(`${location.origin}/api/v1/auth/verify`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token }),
    });
    const { router } = await renderApp(`/login?returnTo=${encodeURIComponent("/me/tickets")}`);
    await screen.findByRole("heading", { name: "Vé của tôi" });
    expect(router.state.location.pathname).toBe("/me/tickets");
  });

  it("LGN-22: in English every string is English and E-01 carries locale en", async () => {
    const sent = recordMagicLinkRequests();
    await renderApp("/login", { locale: "en" });
    await screen.findByRole("heading", { name: "Sign in to buy tickets" });
    fireEvent.change(screen.getByRole("textbox", { name: "Email" }), {
      target: { value: "an@example.com" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Send sign-in link" }));
    await screen.findByRole("heading", { name: "Check your inbox" });
    expect(sent[0]?.locale).toBe("en");
  });

  it("LGN-18: the sent tab follows a sign-in finished in another tab", async () => {
    const { router } = await renderApp(`/login?returnTo=${encodeURIComponent("/me/tickets")}`);
    fireEvent.change(await screen.findByRole("textbox", { name: "Email" }), {
      target: { value: "an@example.com" },
    });
    submit();
    await screen.findByRole("heading", { name: "Kiểm tra hộp thư" });
    // the other tab opens the link
    const token = new URL(localStorage.getItem("tb.mock.lastLink")!).searchParams.get("token");
    await fetch(`${location.origin}/api/v1/auth/verify`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token }),
    });
    act(() => {
      document.dispatchEvent(new Event("visibilitychange"));
    });
    await waitFor(() => expect(router.state.location.pathname).toBe("/me/tickets"));
  });
});
