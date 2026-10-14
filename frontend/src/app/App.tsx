import { QueryClient, QueryClientProvider, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef } from "react";
import { createBrowserRouter, RouterProvider } from "react-router";
import { authStore } from "../api/authStore";
import { configureApi } from "../api/client";
import { meKey, meQuery } from "../api/session";
import { applyAccountLocale } from "../features/auth/localeSync";
import { currentLocale } from "../lib/i18n";
import { loginPath } from "../lib/returnTo";

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, refetchOnWindowFocus: false },
    },
  });
}

/**
 * Global 401 UNAUTHENTICATED (DOC-38 §8.5, FL-03 b): forget the account and open sign-in on the
 * "session expired" variant, returning to the current page. Only one redirect for a burst of 401s.
 */
export function wireSessionExpiry(
  router: ReturnType<typeof createBrowserRouter>,
  queryClient: QueryClient,
): void {
  let redirecting = false;
  configureApi({
    locale: currentLocale,
    onUnauthenticated() {
      const { pathname, search } = router.state.location;
      if (redirecting || pathname === "/login") return;
      redirecting = true;
      authStore.clear();
      queryClient.clear();
      queryClient.setQueryData(meKey, null);
      void router
        .navigate(loginPath(pathname + search, "session_expired"), { replace: true })
        .finally(() => {
          redirecting = false;
        });
    },
  });
}

/**
 * Applies the account locale and any unsaved language choice from the boot GET /me. Only that
 * request counts: if sign-in cancels it (AuthCallback), the callback applies the locale itself.
 */
function SessionBoot() {
  const queryClient = useQueryClient();
  const started = useRef(false);
  useEffect(() => {
    if (started.current) return;
    started.current = true;
    queryClient.fetchQuery(meQuery).then(
      (me) => {
        if (me) void applyAccountLocale(me, queryClient, { afterSignIn: false });
      },
      () => {
        // cancelled by sign-in, or the API is down: the page shows its own state
      },
    );
  }, [queryClient]);
  return null;
}

export function App({
  router,
  queryClient,
}: {
  router: ReturnType<typeof createBrowserRouter>;
  queryClient: QueryClient;
}) {
  return (
    <QueryClientProvider client={queryClient}>
      <SessionBoot />
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}
