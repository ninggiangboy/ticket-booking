import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll } from "vitest";

import { handlers } from "./mocks/handlers";

// jsdom brings its own AbortSignal, which Node's Request (undici) rejects. Tests drop the signal;
// the browser build is unaffected.
const NodeRequest = globalThis.Request;
globalThis.Request = class extends NodeRequest {
  constructor(input: RequestInfo | URL, init?: RequestInit) {
    super(input, init?.signal ? { ...init, signal: undefined } : init);
  }
} as typeof Request;

// ScrollRestoration calls it; jsdom does not implement scrolling.
window.scrollTo = () => {};

export const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => {
  cleanup();
  server.resetHandlers();
  localStorage.clear();
  sessionStorage.clear();
  document.cookie = "tb_lang=; Path=/; Max-Age=0";
});
afterAll(() => server.close());
