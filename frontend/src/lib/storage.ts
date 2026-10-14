// Browser storage can throw (private mode, blocked site data): every access is best effort
// and failures only lose the convenience the value provided (DOC-38 §5).

type Area = "local" | "session";

function area(kind: Area): Storage | null {
  try {
    return kind === "local" ? window.localStorage : window.sessionStorage;
  } catch {
    return null;
  }
}

export function readItem(key: string, kind: Area = "local"): string | null {
  try {
    return area(kind)?.getItem(key) ?? null;
  } catch {
    return null;
  }
}

export function writeItem(key: string, value: string, kind: Area = "local"): void {
  try {
    area(kind)?.setItem(key, value);
  } catch {
    // ignored on purpose
  }
}

export function removeItem(key: string, kind: Area = "local"): void {
  try {
    area(kind)?.removeItem(key);
  } catch {
    // ignored on purpose
  }
}
