import { LogOut, PanelLeftClose, PanelLeftOpen, Shield } from "lucide-react";
import { useApp } from "../../contexts/AppContext";
import { NAV } from "../../constants/menu";
import { C } from "../../constants/colors";

export function Sidebar() {
  const { t, page, onNav, collapsed, toggleCollapsed } = useApp();
  const w = collapsed ? 64 : 220;

  return (
    <aside className="fixed inset-y-0 left-0 z-40 flex flex-col transition-all duration-200"
      style={{ width: w, background: t.sidebar, borderRight: `1px solid ${t.sidebarBorder}` }}>

      {/* Logo area */}
      <div
        className={`flex items-center h-16 shrink-0 ${collapsed ? "justify-center px-2" : "gap-3 px-4"}`}
        style={{ borderBottom: `1px solid ${t.sidebarBorder}` }}>
        {!collapsed && (
          <>
            <div className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0"
              style={{ background: C.blue }}>
              <Shield size={18} color="#fff" />
            </div>
            <div className="flex-1 min-w-0 overflow-hidden">
              <p className="font-bold text-[14px] leading-tight" style={{ color: t.sidebarText }}>동행가드</p>
              <p className="text-[10px] leading-tight tracking-wide" style={{ color: t.sidebarMuted }}>Admin Portal</p>
            </div>
          </>
        )}
        <button
          onClick={toggleCollapsed}
          className="flex items-center justify-center rounded-lg transition-all hover:opacity-90"
          style={{ width: 28, height: 28, background: "rgba(255,255,255,0.12)", color: t.sidebarText, flexShrink: 0 }}>
          {collapsed ? <PanelLeftOpen size={16} /> : <PanelLeftClose size={16} />}
        </button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-3 px-2 space-y-0.5">
        {!collapsed && (
          <p className="px-2 pt-1 pb-2 text-[10px] font-semibold uppercase tracking-widest" style={{ color: t.sidebarMuted }}>
            메뉴
          </p>
        )}
        {NAV.map(({ key, label, Icon }) => {
          const active = page === key || (page === "event-detail" && key === "events");
          return (
            <button
              key={key}
              title={collapsed ? label : undefined}
              onClick={() => onNav(key)}
              className="w-full flex items-center gap-2.5 px-2.5 py-2 rounded-xl text-[13px] font-medium transition-all"
              style={{
                color: active ? "#fff" : t.sidebarText,
                background: active ? t.sidebarActive : "transparent",
                borderLeft: `3px solid ${active ? C.blue : "transparent"}`,
              }}>
              <Icon size={16} className="shrink-0" />
              {!collapsed && <span className="truncate">{label}</span>}
            </button>
          );
        })}
      </nav>

      {/* User footer */}
      <div className="px-3 py-3 shrink-0" style={{ borderTop: `1px solid ${t.sidebarBorder}` }}>
        <div className="flex items-center gap-2.5">
          <div className="w-7 h-7 rounded-full shrink-0 flex items-center justify-center text-[11px] font-bold text-white"
            style={{ background: C.blue }}>관</div>
          {!collapsed && (
            <>
              <div className="flex-1 min-w-0">
                <p className="text-[11px] font-semibold truncate" style={{ color: t.sidebarText }}>admin@donghaegg.kr</p>
                <p className="text-[10px]" style={{ color: t.sidebarMuted }}>슈퍼관리자</p>
              </div>
              <LogOut size={13} style={{ color: t.sidebarMuted }} className="cursor-pointer hover:opacity-70 transition-opacity shrink-0" />
            </>
          )}
        </div>
      </div>
    </aside>
  );
}
