import type { ReactNode } from "react";
import { Sidebar } from "./Sidebar";
import { Header } from "./Header";
import { useApp } from "../../contexts/AppContext";

export function Layout({ children }: { children: ReactNode }) {
  const { t, sidebarWidth } = useApp();

  return (
    <div className="min-h-screen" style={{
      fontFamily: "'Noto Sans KR','Inter',system-ui,sans-serif",
      background: t.bg,
      color: t.text,
    }}>
      <Sidebar />
      <Header />
      <main
        className="transition-all duration-200"
        style={{ marginLeft: sidebarWidth, paddingTop: 56 }}>
        <div className="p-6">{children}</div>
      </main>
    </div>
  );
}
