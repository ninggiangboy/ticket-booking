// `returnTo` only accepts internal paths (DR-21, DR-131): starts with one "/", not "//" or "/\",
// at most 512 characters. Anything else falls back to "/". The server checks again (E-01, E-02).

const MAX_LENGTH = 512;

export function sanitizeReturnTo(value: string | null | undefined): string {
  if (!value || value.length > MAX_LENGTH) return "/";
  if (!value.startsWith("/") || value.startsWith("//") || value.startsWith("/\\")) return "/";
  return value;
}

const UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
const EVENT_PAGE = new RegExp(`^/events/(${UUID})(?:/|$|\\?)`, "i");
const EVENT_PICKER = new RegExp(`^/events/(${UUID})/(?:seats|queue)$`, "i");

/** Event id when `returnTo` is any page of one event (back link on the login header). */
export function eventIdOf(returnTo: string): string | null {
  return EVENT_PAGE.exec(returnTo)?.[1] ?? null;
}

/** Event id when `returnTo` is the seat or queue page (note "after you sign in", DOC-44 §6.4). */
export function pickerEventIdOf(returnTo: string): string | null {
  return EVENT_PICKER.exec(returnTo)?.[1] ?? null;
}

export function isUuid(value: string | undefined): value is string {
  return value !== undefined && new RegExp(`^${UUID}$`, "i").test(value);
}

export function loginPath(returnTo: string, reason?: "signed_out" | "session_expired"): string {
  const params = new URLSearchParams({ returnTo: sanitizeReturnTo(returnTo) });
  if (reason) params.set("reason", reason);
  return `/login?${params.toString()}`;
}
