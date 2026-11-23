import { useState } from "react";
import { toast } from "sonner";
import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";
import { Card, CardHead, Toggle } from "../components/ui";
import { useCurrentUser } from "../hooks/useCurrentUser";

const INIT_TOGGLES: Record<string, boolean> = {
  "DANGER 이벤트 즉시 알림":    true,
  "주간 통계 리포트 자동 발송":  true,
  "디바이스 오류 발생 시 알림":  true,
  "위험 구간 신규 감지 알림":    false,
  "AI 분석 자동 실행":           true,
  "위험 구간 재분류 주기":       true,
  "분석 신뢰도 임계값":          false,
  "데이터 보존 기간":            true,
  "API 키 관리":                 true,
  "관리자 계정 권한 관리":       true,
  "감사 로그 활성화":            false,
};

const STORAGE_KEY = "donghaeng-admin-settings";

const SECTIONS = [
  {
    title: "알림 설정", icon: "🔔",
    items: [
      { label: "DANGER 이벤트 즉시 알림",   sub: "Email / SMS 동시 발송" },
      { label: "주간 통계 리포트 자동 발송", sub: "매주 월요일 오전 9시" },
      { label: "디바이스 오류 발생 시 알림", sub: "연결 끊김·ERROR 상태 감지 시" },
      { label: "위험 구간 신규 감지 알림",   sub: "AI 분석 후 HIGH 등급 구간 생성 시" },
    ],
  },
  {
    title: "AI 분석 설정", icon: "🧠",
    items: [
      { label: "AI 분석 자동 실행",     sub: "매일 오전 06:00 자동 스케줄" },
      { label: "위험 구간 재분류 주기", sub: "현재 설정: 7일" },
      { label: "분석 신뢰도 임계값",    sub: "현재 설정: 75점 이상" },
    ],
  },
  {
    title: "시스템 설정", icon: "⚙️",
    items: [
      { label: "데이터 보존 기간",       sub: "현재 설정: 90일" },
      { label: "API 키 관리",            sub: "외부 연동용 API 키 발급·폐기" },
      { label: "관리자 계정 권한 관리",  sub: "슈퍼관리자·일반관리자 구분" },
      { label: "감사 로그 활성화",       sub: "모든 관리자 액션 기록" },
    ],
  },
];

export function Settings() {
  const { t } = useApp();
  const user = useCurrentUser();
  // 서버에 설정 API가 없어 이 브라우저(localStorage)에만 저장한다. 서버 동작에는 영향 없음.
  const [toggles, setToggles] = useState<Record<string, boolean>>(() => {
    try {
      return { ...INIT_TOGGLES, ...JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "{}") };
    } catch {
      return INIT_TOGGLES;
    }
  });

  const setToggle = (key: string, val: boolean) => {
    const next = { ...toggles, [key]: val };
    setToggles(next);
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(next)); } catch { /* 저장 불가 환경 */ }
    toast.success(`${key} ${val ? "켜짐" : "꺼짐"} (이 브라우저에만 저장)`);
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <p className="font-bold text-[15px]" style={{ color: t.text }}>설정</p>
          <p className="text-xs mt-0.5" style={{ color: t.muted }}>서비스 운영 환경과 알림 조건을 설정합니다.</p>
        </div>
      </div>

      <div className="px-4 py-3 rounded-xl text-xs" style={{ background: `${C.blue}12`, color: t.muted, border: `1px solid ${t.border}` }}>
        설정 API가 아직 없어 이 화면의 토글은 이 브라우저에만 저장되며 서버 동작에는 영향을 주지 않습니다.
      </div>

      {SECTIONS.map(sec => (
        <Card key={sec.title}>
          <CardHead title={`${sec.icon}  ${sec.title}`} />
          <div>
            {sec.items.map((item, idx) => (
              <div key={idx} className="flex items-center justify-between gap-6 px-5 py-4"
                style={{ borderBottom: idx < sec.items.length - 1 ? `1px solid ${t.border}` : undefined }}>
                <div className="flex-1 min-w-0">
                  <p className="text-[13px] font-medium" style={{ color: t.text }}>{item.label}</p>
                  <p className="text-[11px] mt-0.5" style={{ color: t.muted }}>{item.sub}</p>
                </div>
                <Toggle on={toggles[item.label] ?? false} onChange={v => setToggle(item.label, v)} />
              </div>
            ))}
          </div>
        </Card>
      ))}

      {/* Account info: 계정 API가 없어 서버에서 받을 수 있는 값만 표시한다 */}
      <Card>
        <CardHead title="👤  계정 정보" sub="계정 API 연동 전" />
        <div className="p-5 grid grid-cols-3 gap-6">
          {[
            { label: "사용자 ID", value: user.userId },
            { label: "이름",      value: user.name },
            { label: "이메일",    value: user.email ?? "—" },
            { label: "권한",      value: user.role ?? "—" },
          ].map(f => (
            <div key={f.label}>
              <p className="font-semibold uppercase tracking-wider mb-1" style={{ color: t.muted, fontSize: 10 }}>{f.label}</p>
              <p className="font-semibold" style={{ color: t.text, fontSize: 13 }}>{f.value}</p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
