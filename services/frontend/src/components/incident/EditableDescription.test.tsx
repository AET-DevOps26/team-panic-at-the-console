import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { EditableDescription } from "./EditableDescription";
import type { Incident } from "@/api/queries";

vi.mock("@/api/client", () => ({
  apiClient: { GET: vi.fn(), POST: vi.fn(), PATCH: vi.fn() },
}));

import { apiClient } from "@/api/client";

const incident: Incident = {
  id: "018e2c5f-1234-7abc-8def-000000000001",
  title: "Checkout 5xx spike",
  description: "Initial description",
  status: "open",
  severity: "SEV2",
  createdAt: "2026-06-26T12:00:00Z",
  resolvedAt: null,
};

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("EditableDescription", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the current description", () => {
    renderWithClient(<EditableDescription incident={incident} />);
    expect(screen.getByText("Initial description")).toBeInTheDocument();
  });

  it("shows a placeholder when there is no description", () => {
    renderWithClient(<EditableDescription incident={{ ...incident, description: null }} />);
    expect(screen.getByText("No description")).toBeInTheDocument();
  });

  it("saves an edited description via PATCH and leaves edit mode", async () => {
    const user = userEvent.setup();
    vi.mocked(apiClient.PATCH).mockResolvedValue({
      data: { ...incident, description: "Updated description" },
      error: undefined,
    } as never);

    renderWithClient(<EditableDescription incident={incident} />);

    await user.click(screen.getByRole("button", { name: "Edit description" }));
    const textarea = screen.getByPlaceholderText("Describe the incident…");
    expect(textarea).toHaveValue("Initial description");

    await user.clear(textarea);
    await user.type(textarea, "Updated description");
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(apiClient.PATCH).toHaveBeenCalledWith("/incidents/{incidentId}/description", {
      params: { path: { incidentId: incident.id } },
      body: { description: "Updated description" },
    });
    expect(screen.queryByRole("button", { name: "Save" })).not.toBeInTheDocument();
  });

  it("sends an empty string when the description is cleared", async () => {
    const user = userEvent.setup();
    vi.mocked(apiClient.PATCH).mockResolvedValue({
      data: { ...incident, description: null },
      error: undefined,
    } as never);

    renderWithClient(<EditableDescription incident={incident} />);

    await user.click(screen.getByRole("button", { name: "Edit description" }));
    await user.clear(screen.getByPlaceholderText("Describe the incident…"));
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(apiClient.PATCH).toHaveBeenCalledWith("/incidents/{incidentId}/description", {
      params: { path: { incidentId: incident.id } },
      body: { description: "" },
    });
  });
});
