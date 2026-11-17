import type { ReactNode } from "react";
import { Sidebar } from "./Sidebar";
import { Header } from "./Header";
import { useApp } from "../../contexts/AppContext";

export function Layout({ children }: { children: ReactNode }) {
  const { t, sidebarWidth, isMobile, mobileNavOpen, setMobileNavOpen } = useApp();

  return (
    <div className="min-h-screen" style={{
      fontFamily: "'Noto Sans KR','Inter',system-ui,sans-serif",
      background: t.bg,
      color: t.text,
    }}>
      {isMobile && mobileNavOpen && (
        <div onClick={() => setMobileNavOpen(false)}
          style={{ position: "fixed", inset: 0, zIndex: 35, background: "rgba(0,0,0,0.4)" }} />
      )}
      <Sidebar />
      <Header />
      <main
        className="transition-all duration-200"
        style={{ marginLeft: sidebarWidth, paddingTop: 56 }}>
        <div style={{ padding: isMobile ? 12 : 24 }}>{children}</div>
      </main>
    </div>
  );
}
