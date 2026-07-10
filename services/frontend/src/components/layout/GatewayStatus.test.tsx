import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { GatewayStatus } from "./GatewayStatus";

vi.mock("@/api/client", () => ({
  apiClient: { GET: vi.fn() },
}));

import { apiClient } from "@/api/client";

function renderWithClient() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <GatewayStatus />
    </QueryClientProvider>,
  );
}

describe("GatewayStatus", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("shows a checking state while the first health check is in flight", () => {
    vi.mocked(apiClient.GET).mockReturnValue(new Promise(() => {}) as never);
    renderWithClient();
    expect(screen.getByRole("status")).toHaveTextContent("Checking API…");
  });

  it("shows connected when the gateway responds", async () => {
    vi.mocked(apiClient.GET).mockResolvedValue({ data: { status: "ok" }, error: undefined } as never);
    renderWithClient();
    expect(await screen.findByText("API connected")).toBeInTheDocument();
    expect(apiClient.GET).toHaveBeenCalledWith("/health");
  });

  it("shows unreachable when the health check fails", async () => {
    vi.mocked(apiClient.GET).mockResolvedValue({ data: undefined, error: { message: "boom" } } as never);
    renderWithClient();
    expect(await screen.findByText("API unreachable")).toBeInTheDocument();
  });
});
