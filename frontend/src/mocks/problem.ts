import { HttpResponse } from "msw";

// Problem Details as the API sends them (DOC-36 §5) plus the headers every response carries.

let sequence = 0;

export function requestId(): string {
  sequence += 1;
  return `${Date.now().toString(16).padStart(16, "0")}${sequence.toString(16).padStart(16, "0")}`.slice(
    -32,
  );
}

export function apiHeaders(extra?: Record<string, string>): Headers {
  const headers = new Headers({
    "X-Request-Id": requestId(),
    "X-Server-Time": String(Date.now()),
    "Cache-Control": "no-store",
  });
  for (const [name, value] of Object.entries(extra ?? {})) headers.set(name, value);
  return headers;
}

export function problem(
  status: number,
  code: string,
  extension: Record<string, unknown> = {},
  headers: Record<string, string> = {},
) {
  const all = apiHeaders({ "Content-Type": "application/problem+json", ...headers });
  return HttpResponse.json(
    {
      type: `https://errors.ticket.dev/${code}`,
      title: code,
      status,
      detail: code,
      code,
      requestId: all.get("X-Request-Id"),
      ...extension,
    },
    { status, headers: all },
  );
}
