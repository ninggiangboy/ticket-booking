import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter } from "react-router";
import { App, createQueryClient, wireSessionExpiry } from "./app/App";
import { initI18n } from "./app/i18n";
import { routes } from "./app/router";
import { resolveGuestLocale } from "./lib/locale";
import "./styles/fonts.css";
import "./styles/tokens.css";
import "./styles/base.css";

async function boot() {
  // Mock API for frontend work without a backend (master plan §7.4). Statically removed from
  // builds without VITE_API_MOCK=1.
  if (import.meta.env.VITE_API_MOCK === "1") {
    const { startMockApi } = await import("./mocks/browser");
    await startMockApi();
  }
  await initI18n(resolveGuestLocale());
  const queryClient = createQueryClient();
  const router = createBrowserRouter(routes);
  wireSessionExpiry(router, queryClient);
  createRoot(document.getElementById("root")!).render(
    <StrictMode>
      <App router={router} queryClient={queryClient} />
    </StrictMode>,
  );
}

void boot();
