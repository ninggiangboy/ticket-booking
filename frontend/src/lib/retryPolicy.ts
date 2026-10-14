// Automatic retry for overload and network errors (DOC-38 §8.2, DR-132).

export const MAX_AUTO_RETRIES = 10;
const BACKOFF_SECONDS = [8, 16, 32] as const;

/**
 * Seconds to wait before the next attempt. `failures` counts consecutive failures so far
 * (1 after the first error). With Retry-After the wait is clamped to 1–32 s; without it
 * the wait grows 8, 16, 32 and stays at 32.
 */
export function retryWaitSeconds(failures: number, retryAfterSeconds?: number | null): number {
  if (retryAfterSeconds != null && Number.isFinite(retryAfterSeconds)) {
    return Math.min(32, Math.max(1, Math.round(retryAfterSeconds)));
  }
  const index = Math.min(Math.max(failures, 1), BACKOFF_SECONDS.length) - 1;
  return BACKOFF_SECONDS[index]!;
}
