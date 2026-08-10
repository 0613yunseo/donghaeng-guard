import { useState } from "react";
import { toast } from "sonner";
import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";
import { Card, CardHead, Toggle } from "../components/ui";

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
  const [toggles, setToggles] = useState<Record<string, boolean>>(INIT_TOGGLES);

  const setToggle = (key: string, val: boolean) => {
    setToggles(prev => ({ ...prev, [key]: val }));
    toast.success(`${key} ${val ? "활성화" : "비활성화"}되었습니다.`);
  };

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <p className="font-bold text-[15px]" style={{ color: t.text }}>설정</p>
          <p className="text-xs mt-0.5" style={{ color: t.muted }}>서비스 운영 환경과 알림 조건을 설정합니다.</p>
        </div>
        <button
          className="h-8 px-4 rounded-xl text-xs font-semibold text-white transition-opacity hover:opacity-80"
          style={{ background: C.blue }}>
          변경사항 저장
        </button>
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

      {/* Account info */}
      <Card>
        <CardHead title="👤  계정 정보" />
        <div className="p-5 grid grid-cols-3 gap-6">
          {[
            { label: "관리자 이름",   value: "홍길동" },
            { label: "이메일",        value: "admin@donghaegg.kr" },
            { label: "권한",          value: "슈퍼관리자" },
            { label: "소속 기관",     value: "서울특별시 복지정책실" },
            { label: "마지막 로그인", value: "2024-06-30 14:20" },
            { label: "계정 생성일",   value: "2024-01-15" },
          ].map(f => (
            <div key={f.label}>
              <p className="text-[10px] font-semibold uppercase tracking-wider mb-1" style={{ color: t.muted }}>{f.label}</p>
              <p className="text-[13px] font-semibold" style={{ color: t.text }}>{f.value}</p>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
