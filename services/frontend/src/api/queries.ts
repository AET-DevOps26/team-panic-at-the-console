import { useQuery, useQueries, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./client";
import type { components } from "@openapi/schema";

// Re-export domain types from generated schema
export type Incident = components["schemas"]["Incident"];
export type IncidentStatus = components["schemas"]["IncidentStatus"];
export type Severity = components["schemas"]["Severity"];
export type IncidentEvent = components["schemas"]["IncidentEvent"];
export type Comment = components["schemas"]["Comment"];
export type CreateIncidentRequest = components["schemas"]["CreateIncidentRequest"];
export type UpdateStatusRequest = components["schemas"]["UpdateStatusRequest"];
export type UpdateDescriptionRequest = components["schemas"]["UpdateDescriptionRequest"];
export type EscalateSeverityRequest = components["schemas"]["EscalateSeverityRequest"];
export type AssignIncidentRequest = components["schemas"]["AssignIncidentRequest"];
export type CreateCommentRequest = components["schemas"]["CreateCommentRequest"];
export type LoginRequest = components["schemas"]["LoginRequest"];
export type RegisterRequest = components["schemas"]["RegisterRequest"];
export type UpdateProfileRequest = components["schemas"]["UpdateProfileRequest"];
export type ChangePasswordRequest = components["schemas"]["ChangePasswordRequest"];
export type User = components["schemas"]["User"];
export type Notification = components["schemas"]["Notification"];
export type NotificationListResponse = components["schemas"]["NotificationListResponse"];
export type WebhookSource = components["schemas"]["WebhookSource"];
export type WebhookSourceWithSecret = components["schemas"]["WebhookSourceWithSecret"];
export type CreateWebhookSourceRequest = components["schemas"]["CreateWebhookSourceRequest"];
export type ExternalEventSummary = components["schemas"]["ExternalEventSummary"];
export type ExternalEventDetail = components["schemas"]["ExternalEventDetail"];
export type ExternalEventListResponse = components["schemas"]["ExternalEventListResponse"];

const MOCK = import.meta.env.VITE_MOCK === "true";

/** Error thrown by query functions, carrying the HTTP status when known. */
export class ApiError extends Error {
  readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

/** True when the error means the resource cannot exist (bad id or not found). */
export function isNotFound(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 400 || error.status === 404);
}

// ── Mock data ─────────────────────────────────────────────────────────────────

const MOCK_INCIDENTS: Incident[] = [
  {
    id: "018e2c5f-1234-7abc-8def-000000000001",
    title: "Checkout service 5xx spike",
    description: "High error rate on checkout API after deploy v2.4.1. Payment service latency rose to 12% and checkout error rate crossed 5%.",
    status: "investigating",
    severity: "SEV2",
    createdAt: new Date(Date.now() - 3 * 3600_000).toISOString(),
    resolvedAt: null,
    summary: "Checkout API error rate spiked to **5%** after deploy `v2.4.1`; payment-service latency is the suspected driver.",
    summaryGeneratedAt: new Date(Date.now() - 2.5 * 3600_000).toISOString(),
    severitySuggestion: "SEV1: Checkout is revenue-critical and error rate exceeds the 5% SLO.",
    severitySuggestionGeneratedAt: new Date(Date.now() - 2.5 * 3600_000).toISOString(),
    solutions: "Roll back deploy v2.4.1\nScale payment-service replicas\nEnable checkout circuit breaker",
    solutionsGeneratedAt: new Date(Date.now() - 45 * 60_000).toISOString(),
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
  { timestamp: new Date(Date.now() - 2.5 * 3600_000).toISOString(), type: "status_changed", description: "status: open → investigating", newValue: "investigating" },
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
    // Recheck quickly while the gateway is down so recovery shows promptly.
    refetchInterval: (query) => (query.state.status === "error" ? 15_000 : 30_000),
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
      if (error || !data) throw new Error("Failed to fetch incidents");
      return data.items;
    },
  });
}

async function fetchIncident(id: string): Promise<Incident> {
  if (MOCK) {
    const found = MOCK_INCIDENTS.find((i) => i.id === id);
    if (!found) throw new ApiError("Incident not found", 404);
    return found;
  }
  const { data, error, response } = await apiClient.GET("/incidents/{incidentId}", {
    params: { path: { incidentId: id } },
  });
  if (error || !data) throw new ApiError("Failed to fetch incident", response.status);
  return data;
}

// A missing or malformed id will not start existing on retry.
function retryUnlessNotFound(failureCount: number, error: unknown) {
  return !isNotFound(error) && failureCount < 3;
}

export function useIncident(id: string) {
  return useQuery({
    queryKey: ["incidents", id],
    queryFn: () => fetchIncident(id),
    enabled: Boolean(id),
    retry: retryUnlessNotFound,
  });
}

/**
 * Incident titles keyed by incident id, resolved from the same per-incident
 * cache entries as {@link useIncident}. Ids whose incident is still loading or
 * failed to load are absent from the map, so callers can degrade gracefully.
 */
export function useIncidentTitles(ids: string[]): Record<string, string> {
  const uniqueIds = [...new Set(ids)];
  const results = useQueries({
    queries: uniqueIds.map((id) => ({
      queryKey: ["incidents", id],
      queryFn: () => fetchIncident(id),
      retry: retryUnlessNotFound,
    })),
  });
  const titles: Record<string, string> = {};
  results.forEach((result, index) => {
    if (result.data) titles[uniqueIds[index]] = result.data.title;
  });
  return titles;
}

export function useCreateIncident() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CreateIncidentRequest) => {
      if (MOCK) return {} as Incident;
      const { data, error } = await apiClient.POST("/incidents", { body });
      if (error) throw new Error("Failed to create incident");
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["incidents"] });
    },
  });
}

function invalidateIncident(queryClient: ReturnType<typeof useQueryClient>, id: string, updated: Incident) {
  queryClient.setQueryData(["incidents", id], updated);
  void queryClient.invalidateQueries({ queryKey: ["incidents"] });
}

export function useUpdateIncidentStatus(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: UpdateStatusRequest) => {
      if (MOCK) return {} as Incident;
      const { data, error } = await apiClient.PATCH("/incidents/{incidentId}/status", {
        params: { path: { incidentId: id } },
        body,
      });
      if (error) throw new Error("Failed to update incident status");
      return data;
    },
    onSuccess: (updated) => {
      if (updated) invalidateIncident(queryClient, id, updated);
    },
  });
}

export function useSetIncidentSeverity(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: EscalateSeverityRequest) => {
      if (MOCK) return {} as Incident;
      const { data, error } = await apiClient.PATCH("/incidents/{incidentId}/severity", {
        params: { path: { incidentId: id } },
        body,
      });
      if (error) throw new Error("Failed to update incident severity");
      return data;
    },
    onSuccess: (updated) => {
      if (updated) invalidateIncident(queryClient, id, updated);
    },
  });
}

export function useAssignIncident(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: AssignIncidentRequest) => {
      if (MOCK) return {} as Incident;
      const { data, error } = await apiClient.PATCH("/incidents/{incidentId}/assign", {
        params: { path: { incidentId: id } },
        body,
      });
      if (error) throw new Error("Failed to assign incident");
      return data;
    },
    onSuccess: (updated) => {
      if (updated) invalidateIncident(queryClient, id, updated);
    },
  });
}

export function useUpdateIncidentDescription(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: UpdateDescriptionRequest) => {
      if (MOCK) return {} as Incident;
      const { data, error } = await apiClient.PATCH("/incidents/{incidentId}/description", {
        params: { path: { incidentId: id } },
        body,
      });
      if (error) throw new Error("Failed to update incident description");
      return data;
    },
    onSuccess: (updated) => {
      if (updated) invalidateIncident(queryClient, id, updated);
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
      if (error || !data) throw new Error("Failed to fetch comments");
      return data.items;
    },
    enabled: Boolean(incidentId),
  });
}

export function useAddComment(incidentId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CreateCommentRequest) => {
      if (MOCK) return {} as Comment;
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

// ── Notifications ─────────────────────────────────────────────────────────────

const MOCK_NOTIFICATIONS: NotificationListResponse = {
  items: [
    {
      id: "018e2c5f-1234-7abc-8def-00000000n001",
      incidentId: "018e2c5f-1234-7abc-8def-000000000001",
      type: "INCIDENT_ASSIGNED",
      recipientId: "018e2c5f-1234-7abc-8def-0000000000aa",
      message: "You were assigned to an incident.",
      read: false,
      createdAt: new Date(Date.now() - 20 * 60_000).toISOString(),
    },
    {
      id: "018e2c5f-1234-7abc-8def-00000000n002",
      incidentId: "018e2c5f-1234-7abc-8def-000000000002",
      type: "INCIDENT_CREATED",
      recipientId: null,
      message: "New incident: Payment service slow response (SEV2)",
      read: true,
      createdAt: new Date(Date.now() - 2 * 3600_000).toISOString(),
    },
  ],
  total: 2,
  page: 0,
  size: 10,
  unreadCount: 1,
};

export function useNotifications(params?: { unreadOnly?: boolean; size?: number }) {
  return useQuery({
    queryKey: ["notifications", params],
    queryFn: async (): Promise<NotificationListResponse> => {
      if (MOCK) return MOCK_NOTIFICATIONS;
      const { data, error } = await apiClient.GET("/notifications", {
        params: { query: params },
      });
      if (error || !data) throw new Error("Failed to fetch notifications");
      return data;
    },
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (notificationId: string) => {
      if (MOCK) return;
      const { error } = await apiClient.POST("/notifications/{notificationId}/read", {
        params: { path: { notificationId } },
      });
      if (error) throw new Error("Failed to mark notification read");
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      if (MOCK) return;
      const { error } = await apiClient.POST("/notifications/read-all");
      if (error) throw new Error("Failed to mark notifications read");
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });
}

// ── Webhook sources & external events ─────────────────────────────────────────

const MOCK_WEBHOOK_SOURCES: WebhookSource[] = [
  {
    slug: "github",
    createdAt: new Date(Date.now() - 14 * 24 * 3600_000).toISOString(),
    lastEventAt: new Date(Date.now() - 25 * 60_000).toISOString(),
  },
  {
    slug: "grafana",
    createdAt: new Date(Date.now() - 2 * 24 * 3600_000).toISOString(),
    secretRotatedAt: new Date(Date.now() - 1 * 24 * 3600_000).toISOString(),
  },
];

const MOCK_EXTERNAL_EVENTS: ExternalEventListResponse = {
  items: [
    {
      id: "018e2c5f-1234-7abc-8def-0000000000e1",
      source: "github",
      eventType: "ci_failure",
      deliveryId: "d1f9c1a0-mock",
      receivedAt: new Date(Date.now() - 25 * 60_000).toISOString(),
      publishedAt: new Date(Date.now() - 25 * 60_000 + 300).toISOString(),
    },
    {
      id: "018e2c5f-1234-7abc-8def-0000000000e2",
      source: "github",
      eventType: "ci_success",
      receivedAt: new Date(Date.now() - 3 * 3600_000).toISOString(),
      publishedAt: new Date(Date.now() - 3 * 3600_000 + 300).toISOString(),
    },
  ],
  total: 2,
  page: 0,
  size: 25,
};

export function useWebhookSources() {
  return useQuery({
    queryKey: ["webhook-sources"],
    queryFn: async (): Promise<WebhookSource[]> => {
      if (MOCK) return MOCK_WEBHOOK_SOURCES;
      const { data, error } = await apiClient.GET("/webhook-sources");
      if (error || !data) throw new Error("Failed to fetch webhook sources");
      return data.items;
    },
  });
}

export function useCreateWebhookSource() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CreateWebhookSourceRequest): Promise<WebhookSourceWithSecret> => {
      if (MOCK) return { slug: body.slug, secret: "f".repeat(64), createdAt: new Date().toISOString() };
      const { data, error, response } = await apiClient.POST("/webhook-sources", { body });
      if (error || !data) throw new ApiError("Failed to register source", response?.status);
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["webhook-sources"] });
    },
  });
}

export function useRotateWebhookSourceSecret() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (slug: string): Promise<WebhookSourceWithSecret> => {
      if (MOCK) return { slug, secret: "e".repeat(64), createdAt: new Date().toISOString(), secretRotatedAt: new Date().toISOString() };
      const { data, error, response } = await apiClient.POST("/webhook-sources/{source}/rotate-secret", {
        params: { path: { source: slug } },
      });
      if (error || !data) throw new ApiError("Failed to rotate secret", response?.status);
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["webhook-sources"] });
    },
  });
}

export function useDeleteWebhookSource() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (slug: string): Promise<void> => {
      if (MOCK) return;
      const { error, response } = await apiClient.DELETE("/webhook-sources/{source}", {
        params: { path: { source: slug } },
      });
      if (error) throw new ApiError("Failed to delete source", response?.status);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["webhook-sources"] });
    },
  });
}

export function useExternalEvents(params?: { source?: string; eventType?: string; page?: number; size?: number }) {
  return useQuery({
    queryKey: ["external-events", params],
    queryFn: async (): Promise<ExternalEventListResponse> => {
      if (MOCK) return MOCK_EXTERNAL_EVENTS;
      const { data, error } = await apiClient.GET("/external-events", {
        params: { query: params },
      });
      if (error || !data) throw new Error("Failed to fetch external events");
      return data;
    },
    // Keep the previous page on screen while filters/pages change, and poll so
    // a freshly configured webhook shows up without a manual refresh.
    placeholderData: (previous) => previous,
    refetchInterval: 15_000,
  });
}

export function useExternalEvent(id: string | null) {
  return useQuery({
    queryKey: ["external-events", "detail", id],
    queryFn: async (): Promise<ExternalEventDetail> => {
      if (MOCK) {
        return { ...MOCK_EXTERNAL_EVENTS.items[0], rawPayload: { action: "completed", workflow_run: { name: "CI", conclusion: "failure" } } };
      }
      const { data, error, response } = await apiClient.GET("/external-events/{externalEventId}", {
        params: { path: { externalEventId: id as string } },
      });
      if (error || !data) throw new ApiError("Failed to fetch event", response?.status);
      return data;
    },
    enabled: Boolean(id),
  });
}

// ── Auth ──────────────────────────────────────────────────────────────────────

const MOCK_USER: User = {
  id: "018e2c5f-1234-7abc-8def-0000000000aa",
  email: "demo@example.com",
  displayName: "Demo Responder",
  role: "MEMBER",
  createdAt: new Date(Date.now() - 30 * 24 * 3600_000).toISOString(),
};

export function useCurrentUser() {
  return useQuery({
    queryKey: ["users", "me"],
    queryFn: async (): Promise<User> => {
      if (MOCK) return MOCK_USER;
      const { data, error, response } = await apiClient.GET("/users/me");
      if (error || !data) throw new ApiError("Not authenticated", response?.status);
      return data;
    },
    retry: false,
    staleTime: 5 * 60_000,
  });
}

export function useUsers() {
  return useQuery({
    queryKey: ["users", "directory"],
    queryFn: async (): Promise<User[]> => {
      if (MOCK) return [MOCK_USER];
      const { data, error } = await apiClient.GET("/users", {
        params: { query: { limit: 100, offset: 0 } },
      });
      if (error || !data) throw new Error("Failed to fetch users");
      return data.items;
    },
    staleTime: 60_000,
  });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: UpdateProfileRequest): Promise<User | undefined> => {
      if (MOCK) {
        return {
          ...MOCK_USER,
          displayName: body.displayName ?? MOCK_USER.displayName,
          email: body.email ?? MOCK_USER.email,
        };
      }
      const { data, error, response } = await apiClient.PATCH("/users/me", { body });
      if (error) throw new ApiError(error.message ?? "Profile update failed", response?.status);
      return data;
    },
    onSuccess: (user) => {
      if (user) queryClient.setQueryData(["users", "me"], user);
      // The directory (assignment pickers, mention lists) shows the same fields.
      void queryClient.invalidateQueries({ queryKey: ["users", "directory"] });
    },
  });
}

export function useChangePassword() {
  return useMutation({
    mutationFn: async (body: ChangePasswordRequest): Promise<void> => {
      if (MOCK) return;
      const { error, response } = await apiClient.POST("/users/me/password", { body });
      if (error) throw new ApiError(error.message ?? "Password change failed", response?.status);
    },
  });
}

export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: LoginRequest): Promise<User | undefined> => {
      if (MOCK) return MOCK_USER;
      // Frontend and gateway share a host behind the edge/ingress in every real
      // deployment, so the default (same-origin) credentials mode already sends and
      // stores the httpOnly `session` cookie. We deliberately do not force `include`:
      // it would break the cross-origin Prism mock, whose wildcard CORS rejects
      // credentialed requests.
      const { data, error } = await apiClient.POST("/auth/login", { body });
      if (error) throw new Error(error.message ?? "Invalid email or password");
      return data;
    },
    onSuccess: (user) => {
      if (user) queryClient.setQueryData(["users", "me"], user);
    },
  });
}

export function useRegister() {
  return useMutation({
    mutationFn: async (body: RegisterRequest): Promise<User | undefined> => {
      if (MOCK) return MOCK_USER;
      const { data, error, response } = await apiClient.POST("/auth/register", { body });
      if (error) throw new ApiError(error.message ?? "Registration failed", response?.status);
      return data;
    },
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      if (MOCK) return;
      const { error } = await apiClient.POST("/auth/logout");
      if (error) throw new Error("Logout failed");
    },
    onSuccess: () => {
      // Drop every cached query: the next login may be a different user.
      queryClient.clear();
    },
  });
}
