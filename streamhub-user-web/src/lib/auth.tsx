"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { api } from "./api";
import type { MemberInfo } from "./types";

// Trade-off: the member token is kept in localStorage for simplicity (no backend session/cookie
// plumbing on this read-only public site). It is XSS-exposed; a production build would move to an
// httpOnly, SameSite cookie issued by the API. The admin console already does the cookie-backed
// approach via NextAuth — see streamhub-web.
const STORAGE_KEY = "streamhub_member_token";

interface AuthState {
  member: MemberInfo | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

/** Member auth state backed by a token in localStorage; validated against /me on load. */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [member, setMember] = useState<MemberInfo | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = typeof window !== "undefined" ? localStorage.getItem(STORAGE_KEY) : null;
    if (!token) {
      setLoading(false);
      return;
    }
    api
      .me(token)
      .then(setMember)
      .catch(() => localStorage.removeItem(STORAGE_KEY)) // expired/invalid → drop it
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await api.login(email, password);
    localStorage.setItem(STORAGE_KEY, res.token);
    setMember(res.member);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setMember(null);
  }, []);

  return <AuthContext.Provider value={{ member, loading, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
