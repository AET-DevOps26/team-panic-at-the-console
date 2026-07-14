import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NotificationBell } from "./NotificationBell";

vi.mock("@/api/client", () => ({
  apiClient: { GET: vi.fn(), POST: vi.fn() },
}));

import { apiClient } from "@/api/client";

const LIST_RESPONSE = {
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

function renderBell() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <NotificationBell />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("NotificationBell", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(apiClient.GET).mockResolvedValue({ data: LIST_RESPONSE, error: undefined } as never);
    vi.mocked(apiClient.POST).mockResolvedValue({ data: undefined, error: undefined } as never);
  });

  it("shows the unread count as a badge", async () => {
    renderBell();
    expect(await screen.findByLabelText("Notifications (1 unread)")).toBeInTheDocument();
    expect(screen.getByText("1")).toBeInTheDocument();
    expect(apiClient.GET).toHaveBeenCalledWith("/notifications", {
      params: { query: { size: 10 } },
    });
  });

  it("hides the badge when everything is read", async () => {
    vi.mocked(apiClient.GET).mockResolvedValue({
      data: { ...LIST_RESPONSE, unreadCount: 0 },
      error: undefined,
    } as never);
    renderBell();
    expect(await screen.findByLabelText("Notifications")).toBeInTheDocument();
  });

  it("lists notifications and marks one read on click", async () => {
    const user = userEvent.setup();
    renderBell();

    await user.click(await screen.findByLabelText("Notifications (1 unread)"));
    await user.click(await screen.findByText("You were assigned to an incident."));

    expect(apiClient.POST).toHaveBeenCalledWith("/notifications/{notificationId}/read", {
      params: { path: { notificationId: "018e2c5f-1234-7abc-8def-00000000n001" } },
    });
  });

  it("does not mark already-read notifications again", async () => {
    const user = userEvent.setup();
    renderBell();

    await user.click(await screen.findByLabelText("Notifications (1 unread)"));
    await user.click(await screen.findByText("New incident: Payment service slow response (SEV2)"));

    expect(apiClient.POST).not.toHaveBeenCalled();
  });

  it("marks all notifications read", async () => {
    const user = userEvent.setup();
    renderBell();

    await user.click(await screen.findByLabelText("Notifications (1 unread)"));
    await user.click(await screen.findByText("Mark all read"));

    expect(apiClient.POST).toHaveBeenCalledWith("/notifications/read-all");
  });
});
