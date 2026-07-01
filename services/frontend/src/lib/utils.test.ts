import { describe, it, expect, beforeEach, vi } from "vitest";
import { cn, formatRelativeTime, formatDateTime } from "./utils";

describe("cn", () => {
  it("merges class names", () => {
    expect(cn("foo", "bar")).toBe("foo bar");
  });

  it("resolves tailwind conflicts — last wins", () => {
    expect(cn("p-2", "p-4")).toBe("p-4");
  });

  it("ignores falsy values", () => {
    expect(cn("foo", false, undefined, null, "bar")).toBe("foo bar");
  });
});

describe("formatRelativeTime", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-06-26T12:00:00Z"));
  });

  it("returns 'just now' for timestamps under a minute ago", () => {
    expect(formatRelativeTime("2026-06-26T11:59:30Z")).toBe("just now");
  });

  it("returns minutes for timestamps under an hour ago", () => {
    expect(formatRelativeTime("2026-06-26T11:45:00Z")).toBe("15m ago");
  });

  it("returns hours for timestamps under a day ago", () => {
    expect(formatRelativeTime("2026-06-26T09:00:00Z")).toBe("3h ago");
  });

  it("returns days for older timestamps", () => {
    expect(formatRelativeTime("2026-06-24T12:00:00Z")).toBe("2d ago");
  });
});

describe("formatDateTime", () => {
  it("formats ISO string as a locale date-time string", () => {
    const result = formatDateTime("2026-06-26T12:00:00Z");
    // locale output varies by environment — just check it contains the year
    expect(result).toContain("2026");
  });
});
