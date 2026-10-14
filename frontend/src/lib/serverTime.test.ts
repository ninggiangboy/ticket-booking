import { recordServerTime, resetServerTime, serverOffset } from "./serverTime";

describe("server clock offset (DR-66)", () => {
  beforeEach(() => resetServerTime());

  it("uses the midpoint of the round trip", () => {
    recordServerTime(10_500, 1_000, 2_000);
    expect(serverOffset()).toBe(9_000);
  });

  it("takes the median of the last five samples", () => {
    for (const offset of [100, 5_000, 120, 110, 90, 105])
      recordServerTime(1_000 + offset, 1_000, 1_000);
    // last five: 5000, 120, 110, 90, 105 → median 110
    expect(serverOffset()).toBe(110);
  });
});
