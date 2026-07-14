import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { BASE_URL } from "./client";

const MOCK = import.meta.env.VITE_MOCK === "true";

export const STREAM_URL = `${BASE_URL}/incidents/stream`;

// Live cache invalidation: the gateway relays incident.* NATS events over SSE
// as {type, incidentId} envelopes. Any message means some incident data changed
// on the server, so mounted incident queries refetch. Uses the same wholesale
// ["incidents"] prefix invalidation the mutations already use; the envelope is
// just a doorbell, the data itself still comes from the REST endpoints.
// Notifications are derived from the same incident events, so the doorbell
// also refreshes the bell badge.
export function useIncidentStream(): void {
  const queryClient = useQueryClient();

  useEffect(() => {
    // The Prism mock has no SSE endpoint, and jsdom has no EventSource.
    if (MOCK || typeof EventSource === "undefined") return;

    const source = new EventSource(STREAM_URL);
    const invalidate = () => {
      void queryClient.invalidateQueries({ queryKey: ["incidents"] });
      void queryClient.invalidateQueries({ queryKey: ["notifications"] });
    };

    source.onmessage = invalidate;

    // EventSource auto-reconnects after a drop, but events sent during the gap
    // are lost: refetch on every open except the first (mount fetches fresh anyway).
    let opened = false;
    source.onopen = () => {
      if (opened) invalidate();
      opened = true;
    };

    return () => source.close();
  }, [queryClient]);
}
