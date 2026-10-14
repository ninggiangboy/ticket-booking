/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_NAME?: string;
  readonly VITE_API_MOCK?: string;
  readonly VITE_MAILPIT_URL?: string;
  readonly VITE_PAYMENTS?: "fake" | "stripe";
  readonly VITE_STRIPE_PUBLISHABLE_KEY?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
