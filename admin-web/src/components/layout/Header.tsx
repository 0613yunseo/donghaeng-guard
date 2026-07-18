import { useState } from "react";
import { Bell, Search, Sun, Moon, LogOut, CalendarDays } from "lucide-react";
import { useApp } from "../../contexts/AppContext";
import { C } from "../../constants/colors";
import { NOTIFICATIONS } from "../../mocks/adminMockData";
import type { Page } from "../../types/admin";

export function Header() {
  const { t, dark, toggleDark, onNav, sidebarWidth, searchQuery, setSearchQuery } = useApp();
  const [dropOpen,  setDropOpen] = useState(false);
  const [bellOpen,  setBellOpen] = useState(false);
  const [notifs,    setNotifs]   = useState(NOTIFICATIONS);
  const unread = notifs.filter(n => !n.read).length;

  const markAllRead = () => setNotifs(n => n.map(x => ({ ...x, read: true })));
  const closeAll = () => { setDropOpen(false); setBellOpen(false); };

  return (
    <header
      className="fixed top-0 right-0 z-30 h-14 flex items-center gap-4 px-6 transition-all duration-200"
      style={{ left: sidebarWidth, background: t.header, borderBottom: `1px solid ${t.border}` }}
      onClick={closeAll}>

      {/* Search bar */}
      <div className="relative flex-1" style={{ maxWidth: 480 }}>
        <Search size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" style={{ color: t.muted }} />
        <input
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          placeholder="이벤트 ID, 디바이스, 위험 유형 검색…"
          className="w-full h-9 pl-10 pr-10 rounded-xl text-xs outline-none transition-all"
          style={{ background: t.input, border: `1px solid ${t.border}`, color: t.text }}
          onFocus={e => { e.currentTarget.style.border = `1px solid ${C.blue}`; e.currentTarget.style.boxShadow = `0 0 0 3px ${C.blue}18`; }}
          onBlur={e  => { e.currentTarget.style.border = `1px solid ${t.border}`; e.currentTarget.style.boxShadow = "none"; }}
        />
        {searchQuery
          ? <button onClick={() => setSearchQuery("")}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[11px] hover:opacity-70"
              style={{ color: t.muted }}>✕</button>
          : <span className="absolute right-3 top-1/2 -translate-y-1/2 text-[10px] px-1.5 py-0.5 rounded font-medium"
              style={{ background: t.border, color: t.muted }}>⌘K</span>
        }
      </div>

      <div className="ml-auto flex items-center gap-2">
        {/* Date chip */}
        <div className="hidden lg:flex items-center gap-1.5 h-8 px-3 rounded-xl text-xs"
          style={{ background: t.input, border: `1px solid ${t.border}`, color: t.muted }}>
          <CalendarDays size={13} /> Jun 1 – Jun 30
        </div>

        {/* Risk filter */}
        <select className="h-8 px-2.5 rounded-xl text-xs outline-none hidden lg:block"
          style={{ background: t.input, border: `1px solid ${t.border}`, color: t.text }}>
          <option>전체 위험단계</option>
          <option>DANGER</option>
          <option>WARNING</option>
          <option>SAFE</option>
        </select>

        {/* Dark mode toggle */}
        <button
          onClick={e => { e.stopPropagation(); toggleDark(); }}
          className="w-8 h-8 rounded-xl flex items-center justify-center hover:opacity-70 transition-opacity"
          style={{ background: t.input, border: `1px solid ${t.border}`, color: t.muted }}>
          {dark ? <Sun size={14} /> : <Moon size={14} />}
        </button>

        {/* Bell */}
        <div className="relative" onClick={e => e.stopPropagation()}>
          <button
            onClick={() => { setBellOpen(o => !o); setDropOpen(false); }}
            className="w-8 h-8 rounded-xl flex items-center justify-center hover:opacity-70 transition-opacity"
            style={{ background: t.input, border: `1px solid ${t.border}`, color: t.muted }}>
            <Bell size={14} />
          </button>
          {unread > 0 && (
            <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full text-[9px] font-bold text-white flex items-center justify-center"
              style={{ background: C.danger }}>{unread}</span>
          )}
          {bellOpen && (
            <div className="absolute right-0 top-full mt-2 w-80 rounded-2xl overflow-hidden z-50"
              style={{ background: t.card, border: `1px solid ${t.border}`, boxShadow: t.shadowLg }}>
              <div className="flex items-center justify-between px-4 py-3" style={{ borderBottom: `1px solid ${t.border}` }}>
                <p className="text-[13px] font-bold" style={{ color: t.text }}>
                  알림{unread > 0 && (
                    <span className="ml-1.5 px-1.5 py-0.5 rounded-full text-[10px] text-white" style={{ background: C.danger }}>{unread}</span>
                  )}
                </p>
                <button onClick={markAllRead} className="text-[11px] hover:opacity-70" style={{ color: C.blue }}>모두 읽음</button>
              </div>
              <div>
                {notifs.map(n => (
                  <div
                    key={n.id}
                    onClick={() => setNotifs(prev => prev.map(x => x.id === n.id ? { ...x, read: true } : x))}
                    className="px-4 py-3 flex items-start gap-3 cursor-pointer transition-colors"
                    style={{ background: n.read ? "transparent" : `${n.color}08`, borderBottom: `1px solid ${t.border}` }}
                    onMouseEnter={e => (e.currentTarget.style.background = t.bgSub)}
                    onMouseLeave={e => (e.currentTarget.style.background = n.read ? "transparent" : `${n.color}08`)}>
                    <div className="w-2 h-2 rounded-full mt-1.5 shrink-0" style={{ background: n.read ? t.border : n.color }} />
                    <div>
                      <p className="text-[12px] font-semibold" style={{ color: t.text }}>{n.title}</p>
                      <p className="text-[11px] mt-0.5" style={{ color: t.muted }}>{n.body}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Avatar + dropdown */}
        <div className="relative" onClick={e => e.stopPropagation()}>
          <button
            onClick={() => setDropOpen(o => !o)}
            className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold text-white transition-opacity hover:opacity-80"
            style={{ background: C.blue }}>관</button>

          {dropOpen && (
            <div className="absolute right-0 top-full mt-2 w-52 rounded-2xl overflow-hidden z-50"
              style={{ background: t.card, border: `1px solid ${t.border}`, boxShadow: t.shadowLg }}>
              <div className="px-4 py-3.5" style={{ borderBottom: `1px solid ${t.border}` }}>
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold text-white shrink-0"
                    style={{ background: C.blue }}>관</div>
                  <div>
                    <p className="text-[13px] font-bold" style={{ color: t.text }}>홍길동</p>
                    <p className="text-[11px]" style={{ color: t.muted }}>admin@donghaegg.kr</p>
                  </div>
                </div>
              </div>
              <div className="py-1.5">
                {([
                  { label: "마이페이지", sub: "프로필 및 계정 정보", icon: "👤", page: "mypage"   as Page },
                  { label: "설정",       sub: "알림·시스템 설정",    icon: "⚙️", page: "settings" as Page },
                ] as const).map(item => (
                  <button
                    key={item.label}
                    onClick={() => { onNav(item.page); setDropOpen(false); }}
                    className="w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors"
                    onMouseEnter={e => (e.currentTarget.style.background = t.bgSub)}
                    onMouseLeave={e => (e.currentTarget.style.background = "transparent")}>
                    <span className="text-base">{item.icon}</span>
                    <div>
                      <p className="text-[13px] font-medium" style={{ color: t.text }}>{item.label}</p>
                      <p className="text-[11px]" style={{ color: t.muted }}>{item.sub}</p>
                    </div>
                  </button>
                ))}
              </div>
              <div className="py-1.5" style={{ borderTop: `1px solid ${t.border}` }}>
                <button
                  className="w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors"
                  onMouseEnter={e => (e.currentTarget.style.background = t.bgSub)}
                  onMouseLeave={e => (e.currentTarget.style.background = "transparent")}>
                  <LogOut size={15} style={{ color: C.danger }} />
                  <p className="text-[13px] font-medium" style={{ color: C.danger }}>로그아웃</p>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
