"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import * as api from "@/lib/api";
import type { AuthUser, PlanCode } from "@/lib/api";
import { clearStoredToken, readStoredToken, writeStoredToken } from "@/lib/auth-storage";

type Status = "loading" | "signed-out" | "signed-in";

type RegisterInput = {
  email: string;
  password: string;
  fullName: string;
  planCode: PlanCode;
};

type AuthContextValue = {
  status: Status;
  user: AuthUser | null;
  login: (email: string, password: string) => Promise<AuthUser>;
  register: (input: RegisterInput) => Promise<AuthUser>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

/** Sesión global: carga el token guardado al arrancar y lo valida contra GET /auth/me. */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<Status>("loading");
  const [user, setUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    const stored = readStoredToken();
    if (!stored) {
      setStatus("signed-out");
      return;
    }
    api
      .fetchMe(stored)
      .then((me) => {
        setUser(me);
        setStatus("signed-in");
      })
      .catch(() => {
        clearStoredToken();
        setStatus("signed-out");
      });
  }, []);

  const applySession = useCallback((auth: api.AuthResponse) => {
    const { token, ...me } = auth;
    writeStoredToken(token);
    setUser(me);
    setStatus("signed-in");
    return me;
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      const auth = await api.login({ email, password });
      return applySession(auth);
    },
    [applySession],
  );

  const register = useCallback(
    async (input: RegisterInput) => {
      const auth = await api.register(input);
      return applySession(auth);
    },
    [applySession],
  );

  const logout = useCallback(() => {
    clearStoredToken();
    setUser(null);
    setStatus("signed-out");
  }, []);

  const value = useMemo(
    () => ({ status, user, login, register, logout }),
    [status, user, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth debe usarse dentro de <AuthProvider>");
  return ctx;
}
