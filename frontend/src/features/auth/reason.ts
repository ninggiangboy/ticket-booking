// Why the sign-in screen opened (DR-138): `?reason=` with `signed_out` or `session_expired`;
// the navigation state of DR-115 is accepted too. Any other value is ignored.

export type LoginReason = "signed_out" | "session_expired";

export function parseReason(query: string | null, state: unknown): LoginReason | null {
  const fromState =
    typeof state === "object" && state !== null && "reason" in state
      ? (state as { reason: unknown }).reason
      : null;
  for (const value of [query, fromState]) {
    if (value === "signed_out" || value === "session_expired") return value;
  }
  return null;
}
