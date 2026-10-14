import { setupWorker } from "msw/browser";
import { handlers } from "./handlers";

export async function startMockApi(): Promise<void> {
  await setupWorker(...handlers).start({ onUnhandledRequest: "bypass", quiet: true });
  console.info("[mock api] MSW is serving /api/v1 (VITE_API_MOCK=1)");
}
