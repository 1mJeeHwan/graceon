"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { api } from "./api";
import type { MemberAuthResponse, MemberInfo } from "./types";

// Trade-off: the member token is kept in localStorage for simplicity (no backend session/cookie
// plumbing on this read-only public site). It is XSS-exposed; a production build would move to an
// httpOnly, SameSite cookie issued by the API. The admin console already does the cookie-backed
// approach via NextAuth — see streamhub-web.
//
// Two things narrow the blast radius until that move happens: the token now lives 2h rather than 8h,
// and logging out revokes it server-side (jti denylist) instead of merely forgetting it here — so a
// stolen copy stops working the moment the real user signs out.
const STORAGE_KEY = "streamhub_member_token";

interface AuthState {
  member: MemberInfo | null;
  /** Raw member token (Bearer) for authenticated calls like order creation. Null when logged out. */
  token: string | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  /** Applies an auth response (token + profile) to the session — used after sign-up. */
  applySession: (res: MemberAuthResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

/** Member auth state backed by a token in localStorage; validated against /me on load. */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [member, setMember] = useState<MemberInfo | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const stored = typeof window !== "undefined" ? localStorage.getItem(STORAGE_KEY) : null;
    if (!stored) {
      setLoading(false);
      return;
    }
    api
      .me(stored)
      .then((m) => {
        setMember(m);
        setToken(stored);
      })
      .catch(() => localStorage.removeItem(STORAGE_KEY)) // expired/invalid → drop it
      .finally(() => setLoading(false));
  }, []);

  const applySession = useCallback((res: MemberAuthResponse) => {
    localStorage.setItem(STORAGE_KEY, res.token);
    setToken(res.token);
    setMember(res.member);
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      applySession(await api.login(email, password));
    },
    [applySession],
  );

  const logout = useCallback(() => {
    const current = localStorage.getItem(STORAGE_KEY);
    // Clear locally first so the UI never waits on the network to sign out, then tell the server to
    // revoke the token. A failed call is swallowed: the user is logged out of this browser either
    // way, and the token expires on its own.
    localStorage.removeItem(STORAGE_KEY);
    setToken(null);
    setMember(null);
    if (current) {
      api.logout(current).catch(() => undefined);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ member, token, loading, login, applySession, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}

/**
 * Reads the stored member token synchronously. Used by the PG redirect landing pages
 * (/checkout/success|fail), which run before the AuthProvider has revalidated /me and so
 * cannot rely on {@link useAuth}().token being populated yet.
 */
export function getStoredToken(): string | null {
  return typeof window !== "undefined" ? localStorage.getItem(STORAGE_KEY) : null;
}
