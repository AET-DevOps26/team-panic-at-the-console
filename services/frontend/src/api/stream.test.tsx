import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { renderHook } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { useIncidentStream, STREAM_URL } from "./stream";

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  url: string;
  onmessage: ((ev: MessageEvent) => void) | null = null;
  onopen: ((ev: Event) => void) | null = null;
  close = vi.fn();

  constructor(url: string) {
    this.url = url;
    FakeEventSource.instances.push(this);
  }
}

function setup() {
  const queryClient = new QueryClient();
  const invalidate = vi.spyOn(queryClient, "invalidateQueries").mockResolvedValue();
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  const hook = renderHook(() => useIncidentStream(), { wrapper });
  return { invalidate, hook, source: FakeEventSource.instances[FakeEventSource.instances.length - 1] };
}

describe("useIncidentStream", () => {
  beforeEach(() => {
    FakeEventSource.instances = [];
    vi.stubGlobal("EventSource", FakeEventSource);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("opens the stream and closes it on unmount", () => {
    const { hook, source } = setup();
    expect(source.url).toBe(STREAM_URL);

    hook.unmount();
    expect(source.close).toHaveBeenCalled();
  });

  it("invalidates incident queries on every event", () => {
    const { invalidate, source } = setup();

    source.onmessage?.(new MessageEvent("message", { data: '{"type":"incident.comment.added","incidentId":"abc"}' }));
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["incidents"] });
  });

  it("skips the initial open but invalidates after a reconnect", () => {
    const { invalidate, source } = setup();

    source.onopen?.(new Event("open"));
    expect(invalidate).not.toHaveBeenCalled();

    source.onopen?.(new Event("open"));
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["incidents"] });
  });
});
