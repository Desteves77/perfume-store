"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { apiClient, ApiError } from "./api";
import type { User } from "./types";

type SessionContextValue = { user: User | null; loading: boolean; refresh: () => Promise<void>; logout: () => Promise<void> };
const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const refresh = async () => {
    try { setUser(await apiClient.get<User>("/api/v1/auth/me")); }
    catch (error) { if (error instanceof ApiError && error.status === 401) setUser(null); }
    finally { setLoading(false); }
  };
  useEffect(() => { void refresh(); }, []);
  const value = useMemo(() => ({
    user, loading, refresh,
    logout: async () => { await apiClient.post("/api/v1/auth/logout"); setUser(null); }
  }), [user, loading]);
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}
export function useSession() {
  const value = useContext(SessionContext);
  if (!value) throw new Error("useSession precisa estar dentro de SessionProvider");
  return value;
}
