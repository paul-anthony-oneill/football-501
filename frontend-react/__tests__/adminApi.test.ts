import { describe, it, expect, vi, beforeEach } from "vitest";

// ── Mock apiFetch ──────────────────────────────────────────────────────────
// vi.mock is hoisted — use vi.hoisted() for the reference inside the factory

const { mockApiFetch } = vi.hoisted(() => ({ mockApiFetch: vi.fn() }));

vi.mock("@/lib/api/client", () => ({
  apiFetch: mockApiFetch,
}));

import { adminApi } from "@/lib/api/admin";

// ── Helpers ────────────────────────────────────────────────────────────────

function mockResponse(status: number, body: unknown = {}) {
  return new Response(status === 204 ? null : JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

beforeEach(() => {
  mockApiFetch.mockReset();
});

// ── URL construction ───────────────────────────────────────────────────────

describe("adminApi — URL construction", () => {
  it("listCategories calls GET /api/admin/categories", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, [{ id: "1", name: "Football" }]));
    await adminApi.listCategories();
    expect(mockApiFetch).toHaveBeenCalledWith("/api/admin/categories", expect.anything());
  });

  it("getCategory calls GET /api/admin/categories/{id}", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, { id: "abc", name: "Film" }));
    await adminApi.getCategory("abc");
    expect(mockApiFetch).toHaveBeenCalledWith("/api/admin/categories/abc", expect.anything());
  });

  it("createCategory calls POST with body", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(201, { id: "1" }));
    await adminApi.createCategory({ name: "Test", slug: "test" });
    const [, init] = mockApiFetch.mock.calls[0];
    expect(mockApiFetch.mock.calls[0][0]).toBe("/api/admin/categories");
    const requestInit = init as RequestInit;
    expect(requestInit.method).toBe("POST");
    expect(JSON.parse(requestInit.body as string)).toEqual({ name: "Test", slug: "test" });
  });

  it("updateCategory calls PUT with body", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, { id: "1" }));
    await adminApi.updateCategory("1", { name: "Updated" });
    const [, init] = mockApiFetch.mock.calls[0];
    expect(mockApiFetch.mock.calls[0][0]).toBe("/api/admin/categories/1");
    expect((init as RequestInit).method).toBe("PUT");
  });

  it("deleteCategory calls DELETE", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(204, null));
    await adminApi.deleteCategory("1");
    const [, init] = mockApiFetch.mock.calls[0];
    expect(mockApiFetch.mock.calls[0][0]).toBe("/api/admin/categories/1");
    expect((init as RequestInit).method).toBe("DELETE");
  });

  it("listQuestions builds query string with all params", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, { content: [], totalElements: 0 }));
    await adminApi.listQuestions("cat-1", "active", 2, 20);
    expect(mockApiFetch.mock.calls[0][0]).toBe(
      "/api/admin/questions?categoryId=cat-1&status=active&page=2&size=20",
    );
  });

  it("listQuestions omits optional params when not provided", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, { content: [], totalElements: 0 }));
    await adminApi.listQuestions();
    expect(mockApiFetch.mock.calls[0][0]).toBe("/api/admin/questions?page=0&size=10");
  });
});

// ── Error handling ─────────────────────────────────────────────────────────

describe("adminApi — error handling", () => {
  it("throws with JSON error message on non-OK response", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(400, { message: "Bad request" }));
    await expect(adminApi.listCategories()).rejects.toThrow("Bad request");
  });

  it("throws with fallback message when error body is not JSON", async () => {
    mockApiFetch.mockResolvedValue(new Response("Internal Server Error", { status: 500 }));
    await expect(adminApi.listCategories()).rejects.toThrow("An error occurred");
  });

  it("throws on 404", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(404, { message: "Not found" }));
    await expect(adminApi.getCategory("nonexistent")).rejects.toThrow("Not found");
  });
});

// ── 204 No Content ─────────────────────────────────────────────────────────

describe("adminApi — 204 handling", () => {
  it("returns undefined for 204 DELETE", async () => {
    mockApiFetch.mockResolvedValue(new Response(null, { status: 204 }));
    const result = await adminApi.deleteCategory("1");
    expect(result).toEqual({});
  });

  it("returns undefined for 204 bulk delete", async () => {
    mockApiFetch.mockResolvedValue(new Response(null, { status: 204 }));
    const result = await adminApi.deleteAnswers(["a", "b"]);
    expect(result).toEqual({});
  });
});

// ── JSON Content-Type header ───────────────────────────────────────────────

describe("adminApi — Content-Type header", () => {
  it("sets Content-Type: application/json on GET requests", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, []));
    await adminApi.listCategories();
    const [, init] = mockApiFetch.mock.calls[0];
    expect((init as RequestInit).headers).toEqual({ "Content-Type": "application/json" });
  });

  it("merges Content-Type with method and body", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(201, { id: "1" }));
    await adminApi.createCategory({ name: "X", slug: "x" });
    const [, init] = mockApiFetch.mock.calls[0];
    const req = init as RequestInit;
    expect(req.headers).toEqual({ "Content-Type": "application/json" });
    expect(req.method).toBe("POST");
    expect(req.body).toBeDefined();
  });
});

// ── Specific endpoint coverage ─────────────────────────────────────────────

describe("adminApi — specific endpoints", () => {
  it("updateQuestionStatus calls PATCH with status body", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, { id: "q1" }));
    await adminApi.updateQuestionStatus("q1", { status: "active" });
    const [url, init] = mockApiFetch.mock.calls[0];
    expect(url).toBe("/api/admin/questions/q1/status");
    expect((init as RequestInit).method).toBe("PATCH");
  });

  it("updateSuitableForDaily calls PATCH with suitable body", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, { id: "q1" }));
    await adminApi.updateSuitableForDaily("q1", true);
    const [url, init] = mockApiFetch.mock.calls[0];
    expect(url).toBe("/api/admin/questions/q1/suitable-for-daily");
    expect((init as RequestInit).method).toBe("PATCH");
    expect(JSON.parse((init as RequestInit).body as string)).toEqual({ suitable: true });
  });

  it("bulkCreateAnswers calls POST to bulk endpoint", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(201, { created: 5 }));
    await adminApi.bulkCreateAnswers("q1", { answers: [{ displayText: "Test Player", score: 42 }] });
    const [url, init] = mockApiFetch.mock.calls[0];
    expect(url).toBe("/api/admin/questions/q1/answers/bulk");
    expect((init as RequestInit).method).toBe("POST");
  });

  it("listAnswers calls GET for question answers", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, []));
    await adminApi.listAnswers("q1");
    expect(mockApiFetch.mock.calls[0][0]).toBe("/api/admin/questions/q1/answers");
  });

  it("listTemplates calls GET /api/admin/templates", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, []));
    await adminApi.listTemplates();
    expect(mockApiFetch.mock.calls[0][0]).toBe("/api/admin/templates");
  });

  it("generateAll calls POST /api/admin/templates/generate", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, { totalGenerated: 10 }));
    await adminApi.generateAll();
    const [url, init] = mockApiFetch.mock.calls[0];
    expect(url).toBe("/api/admin/templates/generate");
    expect((init as RequestInit).method).toBe("POST");
  });

  it("rematerializeQuestion calls POST with correct URL", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, { answersCreated: 50 }));
    await adminApi.rematerializeQuestion("q1");
    const [url, init] = mockApiFetch.mock.calls[0];
    expect(url).toBe("/api/admin/questions/q1/rematerialize");
    expect((init as RequestInit).method).toBe("POST");
  });

  it("backfillEntities calls POST", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, { created: 100 }));
    await adminApi.backfillEntities();
    const [url, init] = mockApiFetch.mock.calls[0];
    expect(url).toBe("/api/admin/entities/backfill-from-players");
    expect((init as RequestInit).method).toBe("POST");
  });

  it("bulkActivateQuestions calls POST with default limit", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, { activated: 10 }));
    await adminApi.bulkActivateQuestions();
    expect(mockApiFetch.mock.calls[0][0]).toBe("/api/admin/questions/bulk-activate?limit=100");
  });

  it("bulkActivateQuestions calls POST with custom limit", async () => {
    mockApiFetch.mockResolvedValue(mockResponse(200, { activated: 5 }));
    await adminApi.bulkActivateQuestions(50);
    expect(mockApiFetch.mock.calls[0][0]).toBe("/api/admin/questions/bulk-activate?limit=50");
  });
});
