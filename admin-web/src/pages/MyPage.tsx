import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";
import { Card, CardHead } from "../components/ui";

const STATS = [
  { label: "담당 이벤트 검토", value: "128건", sub: "이번 달 처리" },
  { label: "조치 완료 구간",   value: "34개",  sub: "누적 처리"   },
  { label: "로그인 횟수",      value: "62회",  sub: "이번 달"     },
  { label: "생성 리포트",      value: "8건",   sub: "이번 달"     },
];

const LOGS = [
  { action: "위험 이벤트 EVT-001 검토 완료",  time: "2024-06-30 14:25" },
  { action: "ZONE-002 상태 → 검토 중 변경",   time: "2024-06-30 12:10" },
  { action: "6월 종합 리포트 다운로드",        time: "2024-06-30 09:30" },
  { action: "디바이스 DG-ESP32-005 오류 확인", time: "2024-06-29 17:45" },
  { action: "위험 이벤트 EVT-004 검토 완료",  time: "2024-06-29 15:20" },
];

export function MyPage() {
  const { t, onNav } = useApp();

  return (
    <div className="space-y-5 max-w-4xl">
      {/* Profile */}
      <Card>
        <div className="p-6 flex items-center gap-6">
          <div className="w-20 h-20 rounded-2xl flex items-center justify-center text-3xl font-bold text-white shrink-0"
            style={{ background: `linear-gradient(135deg, ${C.blue}, ${C.mint})` }}>관</div>
          <div className="flex-1">
            <div className="flex items-center gap-3 mb-1">
              <p className="text-xl font-bold" style={{ color: t.text }}>홍길동</p>
              <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold"
                style={{ background: `${C.blue}15`, color: C.blue }}>슈퍼관리자</span>
            </div>
            <p className="text-sm mb-3" style={{ color: t.muted }}>admin@donghaegg.kr</p>
            <div className="flex items-center gap-5 text-xs" style={{ color: t.muted }}>
              <span>📍 서울특별시 복지정책실</span>
              <span>📅 가입일 2024-01-15</span>
              <span>🕐 마지막 로그인 2024-06-30 14:20</span>
            </div>
          </div>
          <div className="flex gap-2 shrink-0">
            <button className="h-8 px-4 rounded-xl text-xs font-semibold transition-opacity hover:opacity-80 text-white"
              style={{ background: C.blue }}>프로필 수정</button>
            <button onClick={() => onNav("settings")}
              className="h-8 px-4 rounded-xl text-xs font-medium transition-opacity hover:opacity-80"
              style={{ border: `1px solid ${t.border}`, color: t.muted }}>설정 이동</button>
          </div>
        </div>
      </Card>

      {/* Stats */}
      <div className="grid grid-cols-4 gap-4">
        {STATS.map(s => (
          <Card key={s.label} className="p-5 text-center">
            <p className="text-2xl font-bold mb-1" style={{ color: t.text }}>{s.value}</p>
            <p className="text-[12px] font-medium" style={{ color: t.text }}>{s.label}</p>
            <p className="text-[11px] mt-0.5" style={{ color: t.muted }}>{s.sub}</p>
          </Card>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-5">
        {/* Account info */}
        <Card>
          <CardHead title="계정 정보" />
          <div className="p-5 space-y-4">
            {[
              { label: "이름",      value: "홍길동" },
              { label: "이메일",    value: "admin@donghaegg.kr" },
              { label: "권한",      value: "슈퍼관리자" },
              { label: "소속 기관", value: "서울특별시 복지정책실" },
              { label: "연락처",    value: "010-1234-5678" },
              { label: "계정 상태", value: "활성" },
            ].map(f => (
              <div key={f.label} className="flex items-center justify-between py-1"
                style={{ borderBottom: `1px solid ${t.border}` }}>
                <span className="text-[12px]" style={{ color: t.muted }}>{f.label}</span>
                <span className="text-[13px] font-semibold" style={{ color: t.text }}>{f.value}</span>
              </div>
            ))}
          </div>
        </Card>

        {/* Activity log */}
        <Card>
          <CardHead title="최근 활동 로그" />
          <div>
            {LOGS.map((l, i) => (
              <div key={i} className="px-5 py-3.5 flex items-start gap-3"
                style={{ borderBottom: i < LOGS.length - 1 ? `1px solid ${t.border}` : undefined }}>
                <div className="w-1.5 h-1.5 rounded-full mt-1.5 shrink-0" style={{ background: C.blue }} />
                <div className="flex-1 min-w-0">
                  <p className="text-[12px] font-medium" style={{ color: t.text }}>{l.action}</p>
                  <p className="text-[11px] mt-0.5" style={{ color: t.muted }}>{l.time}</p>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      {/* Password change */}
      <Card>
        <CardHead title="비밀번호 변경" />
        <div className="p-5 grid grid-cols-3 gap-4">
          {["현재 비밀번호","새 비밀번호","새 비밀번호 확인"].map(label => (
            <div key={label}>
              <p className="text-[11px] font-medium mb-1.5" style={{ color: t.muted }}>{label}</p>
              <input type="password" placeholder="••••••••"
                className="w-full h-9 px-3 rounded-xl text-sm outline-none"
                style={{ background: t.input, border: `1px solid ${t.border}`, color: t.text }} />
            </div>
          ))}
        </div>
        <div className="px-5 pb-5">
          <button className="h-8 px-5 rounded-xl text-xs font-semibold text-white transition-opacity hover:opacity-80"
            style={{ background: C.blue }}>비밀번호 변경</button>
        </div>
      </Card>
    </div>
  );
}
