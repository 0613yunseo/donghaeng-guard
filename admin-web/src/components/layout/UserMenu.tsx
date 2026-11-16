import { LogOut, Settings, User } from "lucide-react";
import { useApp } from "../../contexts/AppContext";
import { useLogout } from "../../hooks/useLogout";
import { useCurrentUser } from "../../hooks/useCurrentUser";
import { C } from "../../constants/colors";
import type { Page } from "../../types/admin";

interface UserMenuProps {
  onClose: () => void;
  /** 메뉴 위치 클래스 (헤더: 아래로, 사이드바: 위로) */
  className: string;
}

/** 프로필 메뉴(마이페이지·설정·로그아웃). 헤더와 사이드바 하단이 같이 쓴다. */
export function UserMenu({ onClose, className }: UserMenuProps) {
  const { t, onNav } = useApp();
  const user = useCurrentUser();

  const items = [
    { label: "마이페이지", sub: "프로필 및 계정 정보", Icon: User, page: "mypage" as Page },
    { label: "설정", sub: "알림·시스템 설정", Icon: Settings, page: "settings" as Page },
  ];

  const doLogout = useLogout();
  const logout = () => { onClose(); doLogout(); };

  return (
    <div
      role="menu"
      onClick={e => e.stopPropagation()}
      className={`absolute w-60 rounded-2xl overflow-hidden z-50 ${className}`}
      style={{ background: t.card, border: `1px solid ${t.border}`, boxShadow: t.shadowLg }}>
      <div className="px-4 py-3.5" style={{ borderBottom: `1px solid ${t.border}` }}>
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold text-white shrink-0"
            style={{ background: C.blue }}><User size={18} /></div>
          <div className="min-w-0 flex-1">
            <p className="font-bold truncate" style={{ color: t.text, fontSize: 13 }}>{user.name}</p>
            <p className="truncate" style={{ color: t.muted, fontSize: 11 }}>{user.email ?? "계정 연동 전"}</p>
          </div>
        </div>
      </div>

      <div className="py-1.5">
        {items.map(({ label, sub, Icon, page }) => (
          <button
            key={label}
            role="menuitem"
            onClick={() => { onNav(page); onClose(); }}
            className="w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors"
            onMouseEnter={e => (e.currentTarget.style.background = t.bgSub)}
            onMouseLeave={e => (e.currentTarget.style.background = "transparent")}>
            <Icon size={16} className="shrink-0" style={{ color: t.muted }} />
            <div className="min-w-0">
              <p className="font-medium truncate" style={{ color: t.text, fontSize: 13 }}>{label}</p>
              <p className="truncate" style={{ color: t.muted, fontSize: 11 }}>{sub}</p>
            </div>
          </button>
        ))}
      </div>

      <div className="py-1.5" style={{ borderTop: `1px solid ${t.border}` }}>
        <button
          role="menuitem"
          onClick={logout}
          className="w-full flex items-center gap-3 px-4 py-2.5 text-left transition-colors"
          onMouseEnter={e => (e.currentTarget.style.background = t.bgSub)}
          onMouseLeave={e => (e.currentTarget.style.background = "transparent")}>
          <LogOut size={15} className="shrink-0" style={{ color: C.danger }} />
          <p className="font-medium" style={{ color: C.danger, fontSize: 13 }}>로그아웃</p>
        </button>
      </div>
    </div>
  );
}
