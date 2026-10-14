import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError, isApiError } from "../../api/errors";
import { MAX_AUTO_RETRIES, retryWaitSeconds } from "../../lib/retryPolicy";

// Countdown and automatic retry of E4 (DOC-52 §6.2): wait per Retry-After or 8 → 16 → 32 s,
// pause while the tab is hidden, stop after 10 consecutive failures.

export type RetryPhase = "countdown" | "running" | "stopped";

export interface AutoRetry {
  phase: RetryPhase;
  wait: number;
  remaining: number;
  lastError: ApiError;
  retryNow: () => void;
}

export function useAutoRetry(initialError: ApiError, retry: () => Promise<unknown>): AutoRetry {
  const failures = useRef(1);
  const retryRef = useRef(retry);
  const mounted = useRef(true);
  const [lastError, setLastError] = useState(initialError);
  const [wait, setWait] = useState(() => retryWaitSeconds(1, initialError.retryAfterSeconds));
  const [remaining, setRemaining] = useState(wait);
  const remainingRef = useRef(wait);
  const [round, setRound] = useState(0);
  const [phase, setPhaseState] = useState<RetryPhase>("countdown");
  // The ticker reads the phase from a ref: a retry that fails fast would otherwise batch
  // "running" → "countdown" into no visible change and the countdown would stall.
  const phaseRef = useRef<RetryPhase>("countdown");
  const setPhase = useCallback((next: RetryPhase) => {
    phaseRef.current = next;
    setPhaseState(next);
  }, []);

  useEffect(() => {
    retryRef.current = retry;
  }, [retry]);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const attempt = useCallback(async () => {
    setPhase("running");
    try {
      await retryRef.current();
    } catch (cause) {
      if (!mounted.current) return;
      const error = isApiError(cause) ? cause : new ApiError({ status: null, cause });
      failures.current += 1;
      setLastError(error);
      if (failures.current >= MAX_AUTO_RETRIES) {
        setPhase("stopped");
        return;
      }
      const next = retryWaitSeconds(failures.current, error.retryAfterSeconds);
      remainingRef.current = next;
      setWait(next);
      setRemaining(next);
      setPhase("countdown");
      setRound((value) => value + 1);
    }
  }, [setPhase]);

  // One tick per second, restarted for each round; a hidden tab does not count down (ERP-13).
  useEffect(() => {
    const timer = window.setInterval(() => {
      if (phaseRef.current !== "countdown" || document.hidden) return;
      remainingRef.current = Math.max(0, remainingRef.current - 1);
      setRemaining(remainingRef.current);
      if (remainingRef.current === 0) void attempt();
    }, 1000);
    return () => window.clearInterval(timer);
  }, [attempt, round]);

  const retryNow = useCallback(() => {
    if (phaseRef.current !== "running") void attempt();
  }, [attempt]);

  return { phase, wait, remaining, lastError, retryNow };
}
