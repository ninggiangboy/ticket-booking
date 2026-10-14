import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router";
import { useMe } from "../api/session";
import { loginPath } from "../lib/returnTo";
import { QueryGate } from "./errors/QueryGate";

/** Signed-in routes: a guest goes to sign-in and comes back to the same page (UXP-10). */
export function RequireAuth({ children }: { children: ReactNode }) {
  const me = useMe();
  const { pathname, search } = useLocation();
  return (
    <QueryGate query={me}>
      {(user) => (user ? children : <Navigate to={loginPath(pathname + search)} replace />)}
    </QueryGate>
  );
}
