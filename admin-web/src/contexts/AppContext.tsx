import { createContext, useContext, useState, useCallback, useSyncExternalStore, type ReactNode } from "react";
import { LIGHT, DARK } from "../constants/colors";
import type { Page, ThemeTokens } from "../types/admin";

interface AppContextValue {
  t: ThemeTokens;
  dark: boolean;
  toggleDark: () => void;
  page: Page;
  selId: string;
  selTripId: string;
  onNav: (p: Page, id?: string, tripId?: string) => void;
  collapsed: boolean;
  toggleCollapsed: () => void;
  searchQuery: string;
  setSearchQuery: (q: string) => void;
  sidebarWidth: number;
  isMobile: boolean;                 // 폰 화면(767px 이하): 사이드바는 열고 닫는 서랍으로 바뀐다
  mobileNavOpen: boolean;
  setMobileNavOpen: (open: boolean) => void;
}

const MOBILE_QUERY = "(max-width: 767px)";

function useIsMobile(): boolean {
  return useSyncExternalStore(
    cb => {
      const m = window.matchMedia(MOBILE_QUERY);
      m.addEventListener("change", cb);
      return () => m.removeEventListener("change", cb);
    },
    () => window.matchMedia(MOBILE_QUERY).matches,
    () => false,
  );
}

const AppContext = createContext<AppContextValue | null>(null);

export function AppProvider({ children }: { children: ReactNode }) {
  const [page, setPage] = useState<Page>("dashboard");
  const [selId, setSelId] = useState("");
  const [selTripId, setSelTripId] = useState("");
  const [dark, setDark] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const isMobile = useIsMobile();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const t = dark ? DARK : LIGHT;
  const sidebarWidth = isMobile ? 0 : collapsed ? 64 : 220;

  const onNav = useCallback((p: Page, id?: string, tripId?: string) => {
    setMobileNavOpen(false);
    setPage(p);
    if (id !== undefined) setSelId(id);
    if (tripId !== undefined) setSelTripId(tripId);
  }, []);

  return (
    <AppContext.Provider value={{
      t, dark, toggleDark: () => setDark(d => !d),
      page, selId, selTripId, onNav,
      collapsed, toggleCollapsed: () => setCollapsed(c => !c),
      searchQuery, setSearchQuery,
      sidebarWidth, isMobile, mobileNavOpen, setMobileNavOpen,
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
