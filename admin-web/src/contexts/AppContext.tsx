import { createContext, useContext, useState, useCallback, type ReactNode } from "react";
import { LIGHT, DARK } from "../constants/colors";
import type { Page, ThemeTokens } from "../types/admin";

interface AppContextValue {
  t: ThemeTokens;
  dark: boolean;
  toggleDark: () => void;
  page: Page;
  selId: string;
  onNav: (p: Page, id?: string) => void;
  collapsed: boolean;
  toggleCollapsed: () => void;
  searchQuery: string;
  setSearchQuery: (q: string) => void;
  sidebarWidth: number;
}

const AppContext = createContext<AppContextValue | null>(null);

export function AppProvider({ children }: { children: ReactNode }) {
  const [page, setPage] = useState<Page>("dashboard");
  const [selId, setSelId] = useState("");
  const [dark, setDark] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  const t = dark ? DARK : LIGHT;
  const sidebarWidth = collapsed ? 64 : 220;

  const onNav = useCallback((p: Page, id?: string) => {
    setPage(p);
    if (id !== undefined) setSelId(id);
  }, []);

  return (
    <AppContext.Provider value={{
      t, dark, toggleDark: () => setDark(d => !d),
      page, selId, onNav,
      collapsed, toggleCollapsed: () => setCollapsed(c => !c),
      searchQuery, setSearchQuery,
      sidebarWidth,
    }}>
      {children}
    </AppContext.Provider>
  );
}

export function useApp(): AppContextValue {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error("useApp must be used within AppProvider");
  return ctx;
}
