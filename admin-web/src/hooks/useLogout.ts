import { toast } from "sonner";
import { useApp } from "../contexts/AppContext";

/** 로그아웃 동작. 헤더 프로필 메뉴와 사이드바 하단이 같이 쓴다. */
export function useLogout() {
  const { onNav } = useApp();

  // shortcut: 관리자 로그인 기능이 아직 없어 대시보드로 이동만 한다, 인증 연동 시 세션 종료로 교체
  return () => {
    onNav("dashboard");
    toast.info("로그아웃했습니다. (로그인 기능 연동 전이라 대시보드로 이동합니다)");
  };
}
