import { useState } from "react";
import { LogOut, PanelLeftClose, Settings, User } from "lucide-react";
import { useApp } from "../../contexts/AppContext";
import { NAV } from "../../constants/menu";
import { C } from "../../constants/colors";
import { useLogout } from "../../hooks/useLogout";
import { useCurrentUser } from "../../hooks/useCurrentUser";

export function Sidebar() {
  const { t, page, onNav, collapsed, toggleCollapsed } = useApp();
  const w = collapsed ? 64 : 220;
  const [menuOpen, setMenuOpen] = useState(false);
  const logout = useLogout();
  const user = useCurrentUser();

  return (
    <aside className="fixed inset-y-0 left-0 z-40 flex flex-col transition-all duration-200"
      style={{ width: w, background: t.sidebar, borderRight: `1px solid ${t.sidebarBorder}` }}>

      {/* Logo area */}
      <div
        className={`flex items-center h-16 shrink-0 ${collapsed ? "justify-center px-2" : "gap-2.5 px-3"}`}
        style={{ borderBottom: `1px solid ${t.sidebarBorder}` }}>
        {collapsed ? (
          // 접힌 상태: 로고 아이콘만 표시하고 누르면 펼친다
          <button onClick={toggleCollapsed} title="사이드바 펼치기" className="shrink-0 hover:opacity-90 transition-opacity">
            <img src="/logo-mark.png" alt="동행가드" width={40} height={40} className="rounded-xl" />
          </button>
        ) : (
          <>
            {/* 펼친 상태: 접힌 상태와 같은 로고 타일 + 선명한 텍스트(이미지 글씨는 작게 줄이면 읽기 어려워 텍스트로 표시) */}
            <img src="/logo-mark.png" alt="" width={36} height={36} className="rounded-xl shrink-0" />
            <div className="flex-1 min-w-0">
              <p className="font-bold leading-tight truncate" style={{ color: t.sidebarText, fontSize: 15 }}>동행가드</p>
              <p className="leading-tight truncate" style={{ color: t.sidebarMuted, fontSize: 10 }}>Admin Portal</p>
            </div>
            <button
              onClick={toggleCollapsed}
              title="사이드바 접기"
              className="flex items-center justify-center rounded-lg transition-all hover:opacity-90"
              style={{ width: 28, height: 28, background: "rgba(255,255,255,0.12)", color: t.sidebarText, flexShrink: 0 }}>
              <PanelLeftClose size={16} />
            </button>
          </>
        )}
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

      {/* User footer: 프로필을 누르면 사이드바 안에서 마이페이지·설정·로그아웃이 펼쳐진다(팝업 없음) */}
      <div className="px-2 py-2 shrink-0" style={{ borderTop: `1px solid ${t.sidebarBorder}` }}>
        {menuOpen && (
          <div role="menu" className="pb-1 space-y-0.5">
            {[
              { label: "마이페이지", Icon: User, color: t.sidebarText, onClick: () => { onNav("mypage"); setMenuOpen(false); } },
              { label: "설정", Icon: Settings, color: t.sidebarText, onClick: () => { onNav("settings"); setMenuOpen(false); } },
              { label: "로그아웃", Icon: LogOut, color: "#ff8a8a", onClick: () => { setMenuOpen(false); logout(); } },
            ].map(({ label, Icon, color, onClick }) => (
              <button
                key={label}
                role="menuitem"
                title={collapsed ? label : undefined}
                onClick={onClick}
                className={`w-full flex items-center gap-2.5 py-2 rounded-lg transition-colors ${collapsed ? "justify-center" : "px-2.5"}`}
                style={{ color, fontSize: 13 }}
                onMouseEnter={e => (e.currentTarget.style.background = "rgba(255,255,255,0.08)")}
                onMouseLeave={e => (e.currentTarget.style.background = "transparent")}>
                <Icon size={16} className="shrink-0" />
                {!collapsed && <span className="truncate">{label}</span>}
              </button>
            ))}
          </div>
        )}

        <div className={`flex items-center gap-2 ${collapsed ? "justify-center" : "px-1"}`}>
          <button
            onClick={() => setMenuOpen(o => !o)}
            title="프로필 메뉴"
            aria-expanded={menuOpen}
            className="flex items-center gap-2.5 flex-1 min-w-0 text-left rounded-lg py-1 hover:opacity-90 transition-opacity">
            <div className="w-7 h-7 rounded-full shrink-0 flex items-center justify-center font-bold text-white"
              style={{ background: C.blue }}><User size={14} /></div>
            {!collapsed && (
              <div className="flex-1 min-w-0">
                <p className="font-semibold truncate" style={{ color: t.sidebarText, fontSize: 11 }}>{user.email ?? user.name}</p>
                <p style={{ color: t.sidebarMuted, fontSize: 10 }}>{user.role ?? "계정 연동 전"}</p>
              </div>
            )}
          </button>
          {!collapsed && (
            <button
              onClick={logout}
              title="로그아웃"
              className="shrink-0 p-1 rounded-md hover:opacity-70 transition-opacity"
              style={{ color: t.sidebarMuted }}>
              <LogOut size={14} />
            </button>
          )}
        </div>
      </div>
    </aside>
  );
}
