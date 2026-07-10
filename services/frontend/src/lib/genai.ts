import { useEffect, useState } from "react";

// Creating an incident auto-generates summary, severity suggestion, and solution
// suggestions; resolving one auto-generates the postmortem. Generation runs async
// behind NATS + the LLM, so the backend exposes no "in progress" flag: a still-null
// field shortly after its trigger timestamp is the only signal that generation is
// underway. After this window we assume it failed or was skipped and show the
// regular empty state instead of a spinner.
export const AUTO_GENERATION_WINDOW_MS = 2 * 60_000;

// How long to keep showing progress after an explicit Regenerate click before
// giving up on the result.
export const REGEN_WATCH_MS = 60_000;

export function isAutoGenerating(
  triggeredAt: string | null | undefined,
  value: string | null | undefined,
  now: number = Date.now(),
): boolean {
  if (value != null || !triggeredAt) return false;
  // Math.abs tolerates small client/server clock skew (trigger slightly in the future).
  return Math.abs(now - Date.parse(triggeredAt)) < AUTO_GENERATION_WINDOW_MS;
}

// Solution suggestions arrive as plain lines (one step per line). Some LLM
// output already uses markdown list markers; keep those, and turn bare lines
// into list items so everything renders as one bulleted list.
export function solutionsToMarkdown(solutions: string): string {
  return solutions
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .map((line) => (/^(?:[-*+]|\d+[.)])\s/.test(line) ? line : `- ${line}`))
    .join("\n");
}

// Re-renders the component periodically while `active`, so the time-window checks
// above expire even when no new query data arrives (refetches that return unchanged
// data don't cause a re-render on their own).
export function useIntervalRerender(active: boolean, intervalMs = 3_000): void {
  const [, setTick] = useState(0);
  useEffect(() => {
    if (!active) return;
    const timer = setInterval(() => setTick((n) => n + 1), intervalMs);
    return () => clearInterval(timer);
  }, [active, intervalMs]);
}
