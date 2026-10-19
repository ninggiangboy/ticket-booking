import { eventIdOf, loginPath, pickerEventIdOf, sanitizeReturnTo } from "./returnTo";

const EVENT = "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35";

describe("sanitizeReturnTo (LGN-13, UX-17)", () => {
  it.each([
    ["/me/tickets", "/me/tickets"],
    [`/events/${EVENT}/seats?x=1`, `/events/${EVENT}/seats?x=1`],
    ["//evil.example", "/"],
    ["/\\evil", "/"],
    ["https://evil.example/", "/"],
    ["events", "/"],
    ["", "/"],
    [null, "/"],
    [`/${"a".repeat(512)}`, "/"],
  ])("%s → %s", (input, expected) => {
    expect(sanitizeReturnTo(input)).toBe(expected);
  });

  it("accepts exactly 512 characters", () => {
    const value = `/${"a".repeat(511)}`;
    expect(sanitizeReturnTo(value)).toBe(value);
  });
});

describe("event ids in returnTo", () => {
  it("finds the event of any event page for the back link", () => {
    expect(eventIdOf(`/events/${EVENT}`)).toBe(EVENT);
    expect(eventIdOf(`/events/${EVENT}/seats`)).toBe(EVENT);
    expect(eventIdOf("/events/abc")).toBeNull();
  });

  it("only treats the seat and queue pages as pickers", () => {
    expect(pickerEventIdOf(`/events/${EVENT}/seats`)).toBe(EVENT);
    expect(pickerEventIdOf(`/events/${EVENT}/queue`)).toBe(EVENT);
    expect(pickerEventIdOf(`/events/${EVENT}`)).toBeNull();
  });

  it("builds the sign-in path with an encoded returnTo and a reason", () => {
    expect(loginPath("/checkout/x?y=1", "session_expired")).toBe(
      "/login?returnTo=%2Fcheckout%2Fx%3Fy%3D1&reason=session_expired",
    );
    expect(loginPath("//evil")).toBe("/login?returnTo=%2F");
  });
});
