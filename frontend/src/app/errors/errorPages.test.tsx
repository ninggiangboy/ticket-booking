import { act, fireEvent, screen, waitFor } from "@testing-library/react";
import { useQuery } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { createMemoryRouter, RouterProvider } from "react-router";
import { QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import { getEvent } from "../../api/events";
import { MOCK_EVENT_ID } from "../../mocks/events";
import { problem } from "../../mocks/problem";
import { server } from "../../test-setup";
import { renderApp, setupI18n } from "../../test/renderApp";
import { createQueryClient } from "../App";
import { QueryGate } from "./QueryGate";
import { RouteErrorBoundary } from "./RouteErrorBoundary";

// ERP-xx of DOC-52 §9.

function EventProbe() {
  const query = useQuery({ queryKey: ["probe"], queryFn: () => getEvent(MOCK_EVENT_ID) });
  return <QueryGate query={query}>{(event) => <h1 tabIndex={-1}>{event.name}</h1>}</QueryGate>;
}

async function renderProbe() {
  await setupI18n();
  const queryClient = createQueryClient();
  const router = createMemoryRouter([
    {
      path: "/",
      element: (
        <main>
          <EventProbe />
        </main>
      ),
      errorElement: <RouteErrorBoundary />,
    },
  ]);
  return render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

describe("error pages", () => {
  it("ERP-01: an unknown route shows E1 and keeps the URL", async () => {
    const { router } = await renderApp("/khong-co-trang-nay");
    expect(await screen.findByRole("heading", { name: "Trang này không tồn tại" })).toHaveFocus();
    expect(screen.getByTestId("error-404")).toHaveAttribute("role", "alert");
    expect(document.title).toContain("Trang này không tồn tại");
    expect(document.querySelector('meta[name="robots"]')).toHaveAttribute("content", "noindex");
    expect(router.state.location.pathname).toBe("/khong-co-trang-nay");
  });

  it("ERP-03: a 404 of the main data shows E1", async () => {
    server.use(http.get("*/api/v1/events/:eventId", () => problem(404, "NOT_FOUND")));
    await renderProbe();
    expect(await screen.findByTestId("error-404")).toBeInTheDocument();
  });

  it("ERP-08: 503 with Retry-After 6 counts down and retries exactly once, then recovers", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    let calls = 0;
    server.use(
      http.get("*/api/v1/events/:eventId", () => {
        calls += 1;
        return calls === 1
          ? problem(503, "OVERLOADED", { retryAfterSeconds: 6 }, { "Retry-After": "6" })
          : HttpResponse.json({ name: "Hòa nhạc Giao Mùa" });
      }),
    );
    await renderProbe();
    expect(await screen.findByText("Tự thử lại sau 6 giây")).toBeInTheDocument();
    expect(screen.getByTestId("error-503")).toHaveAttribute("role", "status");
    expect(screen.getByText("503")).toBeInTheDocument();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(screen.getByText("Tự thử lại sau 1 giây")).toBeInTheDocument();
    expect(calls).toBe(1);
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1000);
    });
    await waitFor(() =>
      expect(screen.getByRole("heading", { name: "Hòa nhạc Giao Mùa" })).toHaveFocus(),
    );
    expect(calls).toBe(2);
    vi.useRealTimers();
  });

  it("ERP-09/12/15: no response backs off 8, 16, 32 and gives up after 10 failures", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    let calls = 0;
    server.use(
      http.get("*/api/v1/events/:eventId", () => {
        calls += 1;
        return HttpResponse.error();
      }),
    );
    await renderProbe();
    expect(await screen.findByText("Tự thử lại sau 8 giây")).toBeInTheDocument();
    expect(screen.getByText("···")).toBeInTheDocument();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(8000);
    });
    expect(await screen.findByText("Tự thử lại sau 16 giây")).toBeInTheDocument();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(16000);
    });
    expect(await screen.findByText("Tự thử lại sau 32 giây")).toBeInTheDocument();
    for (let i = 0; i < 7; i += 1) {
      await act(async () => {
        await vi.advanceTimersByTimeAsync(32000);
      });
    }
    expect(calls).toBe(10);
    expect(screen.queryByTestId("auto-retry")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Thử lại" })).toBeInTheDocument();
    vi.useRealTimers();
  });

  it("ERP-11: retry now skips the wait", async () => {
    let calls = 0;
    server.use(
      http.get("*/api/v1/events/:eventId", () => {
        calls += 1;
        return problem(429, "RATE_LIMITED", { retryAfterSeconds: 20 }, { "Retry-After": "20" });
      }),
    );
    await renderProbe();
    expect(await screen.findByText("429")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Thử lại ngay" }));
    await waitFor(() => expect(calls).toBe(2));
  });

  it("ERP-17: 500 shows E5 with the full request id", async () => {
    server.use(
      http.get("*/api/v1/events/:eventId", () =>
        HttpResponse.json(
          {
            type: "https://errors.ticket.dev/INTERNAL_ERROR",
            title: "x",
            status: 500,
            detail: "x",
            code: "INTERNAL_ERROR",
            requestId: "9f2c1d7e4b3a4e6f8a0b1c2d3e4f5a6b",
          },
          { status: 500, headers: { "Content-Type": "application/problem+json" } },
        ),
      ),
    );
    await renderProbe();
    expect(await screen.findByTestId("request-id")).toHaveTextContent(
      "9f2c1d7e4b3a4e6f8a0b1c2d3e4f5a6b",
    );
  });

  it("ERP-18: a render error shows E5 without a request id", async () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    function Broken(): never {
      throw new Error("boom");
    }
    await setupI18n();
    const router = createMemoryRouter([
      { path: "/", element: <Broken />, errorElement: <RouteErrorBoundary /> },
    ]);
    render(<RouterProvider router={router} />);
    expect(await screen.findByTestId("error-500")).toBeInTheDocument();
    expect(screen.queryByTestId("request-id")).not.toBeInTheDocument();
  });
});
