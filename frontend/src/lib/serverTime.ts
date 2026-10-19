// Server clock offset from X-Server-Time (DR-66): offset = serverTime − (t0 + t1) / 2,
// median of the last 5 samples. Countdowns use serverNow(), never Date.now() alone.

const SAMPLES = 5;
const samples: number[] = [];
let offset = 0;

export function recordServerTime(serverTime: number, sentAt: number, receivedAt: number): void {
  if (!Number.isFinite(serverTime)) return;
  samples.push(serverTime - (sentAt + receivedAt) / 2);
  if (samples.length > SAMPLES) samples.shift();
  const sorted = [...samples].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);
  offset = sorted.length % 2 === 1 ? sorted[mid]! : (sorted[mid - 1]! + sorted[mid]!) / 2;
}

export function serverOffset(): number {
  return offset;
}

export function serverNow(): number {
  return Date.now() + offset;
}

export function resetServerTime(): void {
  samples.length = 0;
  offset = 0;
}
