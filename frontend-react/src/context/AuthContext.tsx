"use client";

import { createContext, useContext, useEffect, useState, useCallback } from "react";
import { type User, type Session } from "@supabase/supabase-js";
import { createClient } from "@/utils/supabase/client";
import { apiFetch } from "@/lib/api/client";

interface PlayerProfile {
  playerId: string;
  displayName: string | null;
  avatarUrl: string | null;
  gamesPlayed: number;
  gamesWon: number;
  bestScore: number | null;
}

interface AuthState {
  user: User | null;
  session: Session | null;
  loading: boolean;
  /** True when the backend confirms it sees the same JWT identity as Supabase. */
  backendConfirmed: boolean;
  /** Player profile from the backend (null for anonymous users). */
  profile: PlayerProfile | null;
  signInWithGoogle: () => Promise<void>;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthState>({
  user: null,
  session: null,
  loading: true,
  backendConfirmed: false,
  profile: null,
  signInWithGoogle: async () => {},
  signOut: async () => {},
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [backendConfirmed, setBackendConfirmed] = useState(false);
  const [profile, setProfile] = useState<PlayerProfile | null>(null);
  const supabase = createClient();

  // Confirm backend auth state whenever the Supabase session changes
  useEffect(() => {
    if (!session?.access_token) {
      setBackendConfirmed(false);
      setProfile(null);
      return;
    }

    let cancelled = false;

    async function verifyBackend() {
      try {
        const res = await apiFetch("/api/solo/profile");
        if (!cancelled) {
          if (res.ok) {
            const data = await res.json();
            setProfile(data);
            setBackendConfirmed(true);
          } else {
            // 404 = backend doesn't recognise this JWT (expired, missing secret, etc.)
            setProfile(null);
            setBackendConfirmed(false);
          }
        }
      } catch {
        // Network error — don't flip to false on transient failures
        if (!cancelled) {
          // Keep previous state on transient errors
        }
      }
    }

    verifyBackend();
    return () => { cancelled = true; };
  }, [session?.access_token]);

  useEffect(() => {
    // Load initial session
    supabase.auth.getSession().then(({ data }) => {
      setSession(data.session);
      setUser(data.session?.user ?? null);
      setLoading(false);
    });

    // Listen for auth state changes
    const { data: subscription } = supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session);
      setUser(session?.user ?? null);
    });

    return () => subscription.subscription.unsubscribe();
  }, []);

  const signInWithGoogle = useCallback(async () => {
    await supabase.auth.signInWithOAuth({
      provider: "google",
      options: {
        redirectTo: `${window.location.origin}/auth/callback`,
      },
    });
  }, []);

  const signOut = useCallback(async () => {
    await supabase.auth.signOut();
  }, []);

  return (
    <AuthContext.Provider value={{ user, session, loading, backendConfirmed, profile, signInWithGoogle, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
