import { defineConfig, devices } from "@playwright/test";

// E2E (DOC-44 §10, DOC-52 §10). Two modes (master plan §7.4):
//  - default: Vite dev server with the MSW mock API (VITE_API_MOCK=1), no backend needed;
//  - E2E_BASE_URL=http://localhost:8080 (after `make up`): real stack, links read from Mailpit.
// E2E_CHROMIUM_CHANNEL=chrome runs Chromium projects on an installed Google Chrome.

const realBaseUrl = process.env.E2E_BASE_URL;
const mockPort = 5174;
const channel = process.env.E2E_CHROMIUM_CHANNEL;

const locales = [
  { name: "vi", locale: "vi-VN" },
  { name: "en", locale: "en-US" },
];
const browsers = [
  { name: "chromium", device: devices["Desktop Chrome"], channel },
  { name: "webkit", device: devices["Desktop Safari"] },
];

export default defineConfig({
  testDir: "e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: realBaseUrl ?? `http://localhost:${mockPort}`,
    trace: "retain-on-failure",
  },
  projects: browsers.flatMap((browser) =>
    locales.map((l) => ({
      name: `${browser.name}-${l.name}`,
      use: { ...browser.device, channel: browser.channel, locale: l.locale },
      metadata: { lang: l.name },
    })),
  ),
  webServer: realBaseUrl
    ? undefined
    : {
        command: `pnpm exec vite --port ${mockPort} --strictPort`,
        url: `http://localhost:${mockPort}`,
        env: { VITE_API_MOCK: "1" },
        reuseExistingServer: !process.env.CI,
        timeout: 60_000,
      },
});
