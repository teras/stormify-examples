import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

interface ApiBaseUrlContextValue {
  baseUrl: string;
  setBaseUrl: (value: string) => void;
}

const storageKey = "rest-server-api-base-url";
const defaultUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

const ApiBaseUrlContext = createContext<ApiBaseUrlContextValue | null>(null);

export function ApiBaseUrlProvider({ children }: { children: ReactNode }) {
  const [baseUrl, setBaseUrlState] = useState(() => localStorage.getItem(storageKey) ?? defaultUrl);

  useEffect(() => {
    localStorage.setItem(storageKey, baseUrl);
  }, [baseUrl]);

  const value = useMemo<ApiBaseUrlContextValue>(() => ({
    baseUrl,
    setBaseUrl: (next) => setBaseUrlState(next.trim()),
  }), [baseUrl]);

  return <ApiBaseUrlContext.Provider value={value}>{children}</ApiBaseUrlContext.Provider>;
}

export function useApiBaseUrl() {
  const value = useContext(ApiBaseUrlContext);
  if (!value) {
    throw new Error("useApiBaseUrl must be used within ApiBaseUrlProvider");
  }
  return value;
}
