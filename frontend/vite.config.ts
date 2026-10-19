/// <reference types="vitest/config" />
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// Product name for <title> and the logo (UXP-15); compose passes APP_NAME at build time.
process.env.VITE_APP_NAME ||= "ticket";

// `make dev` runs the API on 8081 (DOC-61 §2); nginx does the same routing in compose.
const apiTarget = process.env.VITE_API_PROXY ?? "http://localhost:8081";

export default defineConfig({
  plugins: [react()],
  build: { manifest: true },
  server: {
    proxy: {
      "/api": { target: apiTarget, changeOrigin: false },
      "/media": { target: apiTarget, changeOrigin: false },
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test-setup.ts"],
    include: ["src/**/*.test.{ts,tsx}", "scripts/**/*.test.mjs"],
    restoreMocks: true,
  },
});
