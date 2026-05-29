"use client";

import { useAuth } from "@/context/AuthContext";

export default function LoginButton() {
  const { user, loading, signInWithGoogle, signOut } = useAuth();

  if (loading) {
    return (
      <button className="h-auth-btn font-plex text-[11px] tracking-wider uppercase px-4 py-2 opacity-50" disabled>
        Loading…
      </button>
    );
  }

  if (user) {
    return (
      <div className="h-auth-group flex items-center gap-3">
        {user.user_metadata?.avatar_url && (
          <img
            src={user.user_metadata.avatar_url}
            alt=""
            className="w-7 h-7 rounded-full"
          />
        )}
        <span className="font-plex text-[11px] tracking-wider text-h-dim uppercase max-w-32 truncate">
          {user.user_metadata?.full_name || user.email}
        </span>
        <button
          onClick={signOut}
          className="h-auth-btn font-plex text-[11px] tracking-wider uppercase text-h-dim hover:text-h-fg transition-colors"
        >
          Sign out
        </button>
      </div>
    );
  }

  return (
    <button
      onClick={signInWithGoogle}
      className="h-auth-btn font-plex text-[11px] tracking-wider uppercase border border-h-rule px-4 py-2 rounded-sm hover:border-h-fg hover:bg-white/5 transition-all"
    >
      Sign in with Google
    </button>
  );
}
