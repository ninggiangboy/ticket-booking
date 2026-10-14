// Build-time configuration (Vite `VITE_*`). APP_NAME is the product name (UXP-15, DR-54).

export const APP_NAME: string = import.meta.env.VITE_APP_NAME || "ticket";

export const API_MOCK: boolean = import.meta.env.VITE_API_MOCK === "1";

/** Mailpit web UI, shown as "Open inbox" for every domain in dev builds (DOC-44 §6.1). */
export const MAILPIT_URL: string | null =
  import.meta.env.VITE_MAILPIT_URL || (import.meta.env.DEV ? "http://localhost:8025" : null);
