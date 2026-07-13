import { describe, it, expect } from "vitest";
import { AUTO_GENERATION_WINDOW_MS, isAutoGenerating, solutionsToMarkdown, suggestedSeverity } from "./genai";

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

describe("solutionsToMarkdown", () => {
  it("turns bare lines into list items", () => {
    expect(solutionsToMarkdown("Roll back deploy\nScale replicas")).toBe("- Roll back deploy\n- Scale replicas");
  });

  it("keeps existing markdown list markers", () => {
    expect(solutionsToMarkdown("- Roll back deploy\n* Scale replicas\n1. Enable breaker")).toBe("- Roll back deploy\n* Scale replicas\n1. Enable breaker");
  });

  it("drops empty lines and trims whitespace", () => {
    expect(solutionsToMarkdown("  Roll back deploy  \n\n\nScale replicas\n")).toBe("- Roll back deploy\n- Scale replicas");
  });

  it("does not treat a leading dash without a space as a marker", () => {
    expect(solutionsToMarkdown("-verbose flag fix")).toBe("- -verbose flag fix");
  });
});

describe("suggestedSeverity", () => {
  it("parses the severity prefix", () => {
    expect(suggestedSeverity("SEV2: Checkout is degraded for all users")).toBe("SEV2");
  });

  it("returns null for a missing suggestion", () => {
    expect(suggestedSeverity(null)).toBe(null);
    expect(suggestedSeverity(undefined)).toBe(null);
    expect(suggestedSeverity("")).toBe(null);
  });

  it("returns null when the prefix is not a valid severity", () => {
    expect(suggestedSeverity("SEV5: out of range")).toBe(null);
    expect(suggestedSeverity("High: free-form severity")).toBe(null);
  });

  it("requires the prefix at the start of the string", () => {
    expect(suggestedSeverity("Suggested SEV1: total outage")).toBe(null);
  });

  it("requires the colon separator", () => {
    expect(suggestedSeverity("SEV1 total outage")).toBe(null);
  });
});
