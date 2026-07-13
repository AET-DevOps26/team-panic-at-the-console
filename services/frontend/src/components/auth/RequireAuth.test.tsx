import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import RequireAuth from "./RequireAuth";

vi.mock("@/api/client", () => ({
  apiClient: { GET: vi.fn(), POST: vi.fn(), PATCH: vi.fn() },
}));

import { apiClient } from "@/api/client";

function renderGuarded() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/incidents"]}>
        <Routes>
          <Route path="/login" element={<p>Login page</p>} />
          <Route element={<RequireAuth />}>
            <Route path="/incidents" element={<p>Incident list</p>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("RequireAuth", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the protected page when /users/me succeeds", async () => {
    vi.mocked(apiClient.GET).mockResolvedValue({
      data: {
        id: "018e2c5f-1234-7abc-8def-0000000000aa",
        email: "alex@example.com",
        displayName: "Alex",
        role: "MEMBER",
        createdAt: "2026-05-08T10:00:00Z",
      },
      error: undefined,
    } as never);

    renderGuarded();

    await waitFor(() => expect(screen.getByText("Incident list")).toBeInTheDocument());
  });

  it("redirects to /login when the session check returns 401", async () => {
    vi.mocked(apiClient.GET).mockResolvedValue({
      data: undefined,
      error: { message: "Not authenticated" },
      response: { status: 401 },
    } as never);

    renderGuarded();

    await waitFor(() => expect(screen.getByText("Login page")).toBeInTheDocument());
    expect(screen.queryByText("Incident list")).not.toBeInTheDocument();
  });
});
