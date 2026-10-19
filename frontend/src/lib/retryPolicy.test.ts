import { retryWaitSeconds } from "./retryPolicy";

describe("retryWaitSeconds (DOC-38 §8.2, ERP-09, ERP-10)", () => {
  it("backs off 8, 16, 32 then stays at 32 without Retry-After", () => {
    expect([1, 2, 3, 4, 9].map((n) => retryWaitSeconds(n))).toEqual([8, 16, 32, 32, 32]);
  });

  it("clamps Retry-After to 1–32 seconds", () => {
    expect(retryWaitSeconds(1, 6)).toBe(6);
    expect(retryWaitSeconds(1, 120)).toBe(32);
    expect(retryWaitSeconds(1, 0)).toBe(1);
  });
});
