import { type NextRequest, NextResponse } from "next/server";
import { createClient } from "@/utils/supabase/middleware";

const ADMIN_ROUTES = /^\/admin(?:\/.*)?$/;

export async function middleware(request: NextRequest) {
  const { supabase, supabaseResponse } = createClient(request);

  const {
    data: { user },
  } = await supabase.auth.getUser();

  const pathname = request.nextUrl.pathname;
  const isAdmin = ADMIN_ROUTES.test(pathname);

  if (isAdmin) {
    if (!user) {
      const redirectUrl = new URL("/", request.url);
      redirectUrl.searchParams.set("auth_required", "1");
      return NextResponse.redirect(redirectUrl);
    }
    // Defense-in-depth: also require the admin claim from Supabase app_metadata.
    // The backend enforces ROLE_ADMIN via @PreAuthorize, but blocking here avoids
    // showing the admin UI skeleton to non-admin authenticated users.
    if (user.app_metadata?.role !== "admin") {
      return NextResponse.redirect(new URL("/", request.url));
    }
  }

  return supabaseResponse;
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)",
  ],
};
