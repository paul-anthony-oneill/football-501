import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";

// ── Mocks ──────────────────────────────────────────────────────────────────

const mockGetSession = vi.fn();

vi.mock("@/utils/supabase/client", () => ({
  createClient: () => ({
    auth: {
      getSession: mockGetSession,
    },
  }),
}));

// Import after mocking so it picks up the mocked supabase client
import { apiFetch } from "@/lib/api/client";

// ── Tests ──────────────────────────────────────────────────────────────────

describe("apiFetch", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    mockGetSession.mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("passes through fetch for non-/api/ URLs without auth header", async () => {
    const mockFetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200 }),
    );

    await apiFetch("https://example.com/data");

    const [input, init] = mockFetch.mock.calls[0];
    expect(input).toBe("https://example.com/data");
    expect(init?.headers).toBeUndefined(); // no headers added
  });

  it("passes through fetch for /api/ URLs without auth when no session", async () => {
    mockGetSession.mockResolvedValue({ data: { session: null } });

    const mockFetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200 }),
    );

    await apiFetch("/api/categories");

    const [input, init] = mockFetch.mock.calls[0];
    expect(input).toBe("/api/categories");
    // Should not have Authorization header
    const headers = init?.headers as Record<string, string> | undefined;
    expect(headers?.Authorization).toBeUndefined();
  });

  it("injects Bearer token for /api/ URLs when session exists", async () => {
    mockGetSession.mockResolvedValue({
      data: { session: { access_token: "test-token-123" } },
    });

    const mockFetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200 }),
    );

    await apiFetch("/api/categories");

    const [input, init] = mockFetch.mock.calls[0];
    expect(input).toBe("/api/categories");
    const headers = init?.headers as Record<string, string> | undefined;
    expect(headers?.Authorization).toBe("Bearer test-token-123");
  });

  it("preserves existing headers when injecting auth", async () => {
    mockGetSession.mockResolvedValue({
      data: { session: { access_token: "token" } },
    });

    const mockFetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200 }),
    );

    await apiFetch("/api/admin/questions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: "{}",
    });

    const [, init] = mockFetch.mock.calls[0];
    const headers = init?.headers as Record<string, string>;
    expect(headers["Content-Type"]).toBe("application/json");
    expect(headers.Authorization).toBe("Bearer token");
  });

  it("does NOT inject auth for full URL objects (only relative /api/ paths)", async () => {
    mockGetSession.mockResolvedValue({
      data: { session: { access_token: "url-object-token" } },
    });

    const mockFetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200 }),
    );

    // A full URL object href is "https://host/api/..." — does not start with "/api/"
    await apiFetch(new URL("https://api.example.com/api/entities"));

    const [, init] = mockFetch.mock.calls[0];
    const headers = init?.headers as Record<string, string> | undefined;
    expect(headers?.Authorization).toBeUndefined();
  });


  it("returns the raw fetch response", async () => {
    mockGetSession.mockResolvedValue({ data: { session: null } });
    const expected = new Response('{"data":42}', { status: 200 });
    vi.spyOn(globalThis, "fetch").mockResolvedValue(expected);

    const result = await apiFetch("/api/test");
    expect(result).toBe(expected);
  });

  it("propagates fetch errors (network failure)", async () => {
    mockGetSession.mockResolvedValue({ data: { session: null } });
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new TypeError("Failed to fetch"));

    await expect(apiFetch("/api/test")).rejects.toThrow("Failed to fetch");
  });
});
