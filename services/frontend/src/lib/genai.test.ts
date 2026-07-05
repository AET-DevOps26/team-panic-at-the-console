import { describe, it, expect } from "vitest";
import { AUTO_GENERATION_WINDOW_MS, isAutoGenerating } from "./genai";

describe("isAutoGenerating", () => {
  const now = Date.parse("2026-06-26T12:00:00Z");
  const secondsAgo = (s: number) => new Date(now - s * 1000).toISOString();

  it("is true for a null field shortly after the trigger", () => {
    expect(isAutoGenerating(secondsAgo(10), null, now)).toBe(true);
    expect(isAutoGenerating(secondsAgo(10), undefined, now)).toBe(true);
  });

  it("is false once the field has a value", () => {
    expect(isAutoGenerating(secondsAgo(10), "generated text", now)).toBe(false);
  });

  it("is false after the auto-generation window has passed", () => {
    const past = new Date(now - AUTO_GENERATION_WINDOW_MS - 1000).toISOString();
    expect(isAutoGenerating(past, null, now)).toBe(false);
  });

  it("is false without a trigger timestamp", () => {
    expect(isAutoGenerating(null, null, now)).toBe(false);
    expect(isAutoGenerating(undefined, null, now)).toBe(false);
  });

  it("tolerates small clock skew (trigger slightly in the future)", () => {
    const future = new Date(now + 5_000).toISOString();
    expect(isAutoGenerating(future, null, now)).toBe(true);
  });

  it("is false for an unparsable timestamp", () => {
    expect(isAutoGenerating("not-a-date", null, now)).toBe(false);
  });
});
