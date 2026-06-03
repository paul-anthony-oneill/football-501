"use client";

import { createClient } from "@/utils/supabase/client";

const supabase = createClient();

/**
 * Wraps {@link fetch} with automatic Supabase Bearer token injection
 * for {@code /api/*} calls.
 *
 * <p>When the user has an active Supabase session (Google sign-in or
 * anonymous), the access token is attached.  When there is no session
 * the request is made without auth — the backend assigns an anonymous
 * session UUID via the {@code X-Anonymous-Id} cookie.
 *
 * <p>External URLs and non-API paths are passed through unchanged.
 */
export async function apiFetch(
  input: RequestInfo | URL,
  init?: RequestInit,
): Promise<Response> {
  const url =
    typeof input === "string"
      ? input
      : input instanceof URL
        ? input.href
        : input.url;

  // Only inject auth for local API calls
  if (url.startsWith("/api/")) {
    const {
      data: { session },
    } = await supabase.auth.getSession();
    if (session?.access_token) {
      init = {
        ...init,
        headers: {
          ...init?.headers,
          Authorization: `Bearer ${session.access_token}`,
        },
      };
    }
  }

  return fetch(input, init);
}
