"use client";

import { createClient } from "@/utils/supabase/client";

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
    const supabase = createClient();
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
