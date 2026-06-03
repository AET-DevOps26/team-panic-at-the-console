import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./client";
import type { components } from "@openapi/schema";

// Re-export domain types from generated schema
export type Incident = components["schemas"]["Incident"];
export type IncidentStatus = components["schemas"]["IncidentStatus"];
export type Severity = components["schemas"]["Severity"];
export type IncidentEvent = components["schemas"]["IncidentEvent"];
export type Comment = components["schemas"]["Comment"];
export type CreateIncidentRequest = components["schemas"]["CreateIncidentRequest"];
export type UpdateIncidentRequest = components["schemas"]["UpdateIncidentRequest"];
export type CreateCommentRequest = components["schemas"]["CreateCommentRequest"];

const MOCK = import.meta.env.VITE_MOCK === "true";

// ── Mock data ─────────────────────────────────────────────────────────────────

const MOCK_INCIDENTS: Incident[] = [
  {
    id: "018e2c5f-1234-7abc-8def-000000000001",
    title: "Checkout service 5xx spike",
    description: "High error rate on checkout API after deploy v2.4.1. Payment service latency rose to 12% and checkout error rate crossed 5%.",
    status: "investigating",
    severity: "SEV1",
    createdAt: new Date(Date.now() - 3 * 3600_000).toISOString(),
    resolvedAt: null,
  },
  {
    id: "018e2c5f-1234-7abc-8def-000000000002",
    title: "Payment service slow response",
    description: null,
    status: "open",
    severity: "SEV2",
    createdAt: new Date(Date.now() - 1 * 3600_000).toISOString(),
    resolvedAt: null,
  },
  {
    id: "018e2c5f-1234-7abc-8def-000000000003",
    title: "Database connection pool exhausted",
    description: "Connection pool hit max capacity during peak traffic",
    status: "resolved",
    severity: "SEV3",
    createdAt: new Date(Date.now() - 24 * 3600_000).toISOString(),
    resolvedAt: new Date(Date.now() - 20 * 3600_000).toISOString(),
  },
  {
    id: "018e2c5f-1234-7abc-8def-000000000004",
    title: "CDN cache miss rate elevated",
    description: null,
    status: "open",
    severity: "SEV4",
    createdAt: new Date(Date.now() - 30 * 60_000).toISOString(),
    resolvedAt: null,
  },
];

const MOCK_EVENTS: IncidentEvent[] = [
  { timestamp: new Date(Date.now() - 3 * 3600_000).toISOString(), type: "incident_created", description: "Incident created manually" },
  { timestamp: new Date(Date.now() - 2.5 * 3600_000).toISOString(), type: "status_changed", description: "status: open → investigating" },
  { timestamp: new Date(Date.now() - 2 * 3600_000).toISOString(), type: "assigned", description: "Assigned to alice@example.com" },
  { timestamp: new Date(Date.now() - 1 * 3600_000).toISOString(), type: "comment_added", description: "Possible root cause: connection pool misconfiguration in v2.4.1" },
];

// ── Health ────────────────────────────────────────────────────────────────────

export function useHealth() {
  return useQuery({
    queryKey: ["health"],
    queryFn: async () => {
      const { data, error } = await apiClient.GET("/health");
      if (error) throw new Error("Gateway unreachable");
      return data;
    },
    retry: false,
    refetchInterval: 60_000,
  });
}

// ── Incidents ─────────────────────────────────────────────────────────────────

export function useIncidents(params?: { status?: IncidentStatus; severity?: Severity }) {
  return useQuery({
    queryKey: ["incidents", params],
    queryFn: async () => {
      if (MOCK) return MOCK_INCIDENTS;
      const { data, error } = await apiClient.GET("/incidents", {
        params: { query: params },
      });
      if (error) throw new Error("Failed to fetch incidents");
      return data;
    },
  });
}

export function useIncident(id: string) {
  return useQuery({
    queryKey: ["incidents", id],
    queryFn: async () => {
      if (MOCK) return MOCK_INCIDENTS.find((i) => i.id === id) ?? undefined;
      const { data, error } = await apiClient.GET("/incidents/{incidentId}", {
        params: { path: { incidentId: id } },
      });
      if (error) throw new Error("Failed to fetch incident");
      return data;
    },
    enabled: Boolean(id),
  });
}

export function useCreateIncident() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CreateIncidentRequest) => {
      const { data, error } = await apiClient.POST("/incidents", { body });
      if (error) throw new Error("Failed to create incident");
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["incidents"] });
    },
  });
}

export function useUpdateIncident(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: UpdateIncidentRequest) => {
      const { data, error } = await apiClient.PATCH("/incidents/{incidentId}", {
        params: { path: { incidentId: id } },
        body,
      });
      if (error) throw new Error("Failed to update incident");
      return data;
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(["incidents", id], updated);
      void queryClient.invalidateQueries({ queryKey: ["incidents"] });
    },
  });
}

// ── Event log ─────────────────────────────────────────────────────────────────

export function useIncidentEvents(incidentId: string) {
  return useQuery({
    queryKey: ["incidents", incidentId, "events"],
    queryFn: async () => {
      if (MOCK) return MOCK_EVENTS;
      const { data, error } = await apiClient.GET("/incidents/{incidentId}/events", {
        params: { path: { incidentId } },
      });
      if (error) throw new Error("Failed to fetch events");
      return data;
    },
    enabled: Boolean(incidentId),
  });
}

// ── Comments ──────────────────────────────────────────────────────────────────

export function useComments(incidentId: string) {
  return useQuery({
    queryKey: ["incidents", incidentId, "comments"],
    queryFn: async () => {
      if (MOCK) return [] as Comment[];
      const { data, error } = await apiClient.GET("/incidents/{incidentId}/comments", {
        params: { path: { incidentId } },
      });
      if (error) throw new Error("Failed to fetch comments");
      return data;
    },
    enabled: Boolean(incidentId),
  });
}

export function useAddComment(incidentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CreateCommentRequest) => {
      const { data, error } = await apiClient.POST("/incidents/{incidentId}/comments", {
        params: { path: { incidentId } },
        body,
      });
      if (error) throw new Error("Failed to add comment");
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["incidents", incidentId, "comments"] });
    },
  });
}

// ── GenAI regeneration ────────────────────────────────────────────────────────

export function useRegenerateSummary(incidentId: string) {
  return useMutation({
    mutationFn: async () => {
      const { data, error } = await apiClient.POST("/incidents/{incidentId}/genai/summary", {
        params: { path: { incidentId } },
      });
      if (error) throw new Error("Regeneration failed");
      return data;
    },
  });
}

export function useRegenerateSeverity(incidentId: string) {
  return useMutation({
    mutationFn: async () => {
      const { data, error } = await apiClient.POST("/incidents/{incidentId}/genai/severity", {
        params: { path: { incidentId } },
      });
      if (error) throw new Error("Regeneration failed");
      return data;
    },
  });
}

export function useRegenerateSolutions(incidentId: string) {
  return useMutation({
    mutationFn: async () => {
      const { data, error } = await apiClient.POST("/incidents/{incidentId}/genai/solutions", {
        params: { path: { incidentId } },
      });
      if (error) throw new Error("Regeneration failed");
      return data;
    },
  });
}

export function useRegeneratePostmortem(incidentId: string) {
  return useMutation({
    mutationFn: async () => {
      const { data, error } = await apiClient.POST("/incidents/{incidentId}/genai/postmortem", {
        params: { path: { incidentId } },
      });
      if (error) throw new Error("Regeneration failed");
      return data;
    },
  });
}
