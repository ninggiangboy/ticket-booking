// CSRF token lives only in memory (LGN-10): never in localStorage or sessionStorage.
// A reload gets a fresh one from GET /me (DR-85).

let csrfToken: string | null = null;

export const authStore = {
  csrfToken: (): string | null => csrfToken,
  setCsrfToken(token: string): void {
    csrfToken = token;
  },
  clear(): void {
    csrfToken = null;
  },
};
