import { User } from "lucide-react";
import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";
import { Card, CardHead } from "../components/ui";
import { useCurrentUser } from "../hooks/useCurrentUser";
import { useTripList } from "../hooks/useTripList";
import { useApiList } from "../hooks/useApiList";
import type { DeviceStatus } from "../types/admin";

interface DeviceApiItem { deviceId: string; deviceStatus: DeviceStatus }

const NOT_LINKED = "계정 API 연동 전";

export function MyPage() {
  const { t, onNav } = useApp();
  const user = useCurrentUser();
  const { trips, loading: tripsLoading, error: tripsError } = useTripList();
  const { items: devices, loading: devicesLoading } =
    useApiList<DeviceApiItem>(`/api/devices?userId=${user.userId}`, "디바이스 목록");

  const loading = tripsLoading || devicesLoading;
  const totalEvents = trips.reduce((sum, trip) => sum + (trip.eventCount ?? 0), 0);

  // 서버 데이터만 사용한다. 계정 정보를 주는 API가 없는 항목은 "—"로 표시한다
  const stats = [
    { label: "주행 기록", value: trips.length, sub: "전체 주행" },
    { label: "센서 이벤트", value: totalEvents.toLocaleString(), sub: "전체 주행 합계" },
    { label: "등록 디바이스", value: devices.length, sub: "내 계정에 등록됨" },
    { label: "연결된 디바이스", value: devices.filter(d => d.deviceStatus === "CONNECTED").length, sub: "현재 연결 상태" },
  ];

  const fields = [
    { label: "사용자 ID", value: user.userId },
    { label: "이름", value: user.name },
    { label: "이메일", value: user.email ?? "—" },
    { label: "권한", value: user.role ?? "—" },
  ];

  // 최근 활동: 주행 기록(실제 데이터)으로 보여준다
  const recent = trips.slice(0, 6).map(trip => ({
    id: trip.tripId,
    action: `주행 #${trip.tripId} ${trip.status === "ENDED" ? "종료" : "진행 중"} · 센서 이벤트 ${trip.eventCount ?? 0}건`,
    time: trip.startedAt.replace("T", " ").slice(0, 16),
  }));

  const small = (px: number) => ({ fontSize: px });

  return (
    <div className="space-y-5">
      {/* Profile */}
      <Card>
        <div className="p-6 flex items-center gap-6">
          <div className="w-20 h-20 rounded-2xl flex items-center justify-center shrink-0"
            style={{ background: `linear-gradient(135deg, ${C.blue}, ${C.mint})` }}>
            <User size={36} color="#fff" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xl font-bold mb-1 truncate" style={{ color: t.text }}>{user.name}</p>
            <p className="mb-1" style={{ color: t.muted, ...small(13) }}>
              {user.email ?? "이메일 정보 없음"}
            </p>
            <p style={{ color: t.muted, ...small(12) }}>
              로그인 기능이 연동되기 전이라 계정 정보(이름·이메일·권한)는 표시할 수 없습니다.
            </p>
          </div>
          <button onClick={() => onNav("settings")}
            className="h-8 px-4 rounded-xl text-xs font-medium transition-opacity hover:opacity-80 shrink-0"
            style={{ border: `1px solid ${t.border}`, color: t.muted }}>설정 이동</button>
        </div>
      </Card>

      {tripsError && (
        <p role="alert" className="text-xs rounded-xl px-3 py-2"
          style={{ background: t.bgSub, color: C.danger, border: `1px solid ${t.border}` }}>{tripsError}</p>
      )}

      {/* Stats */}
      <div className="grid grid-cols-4 gap-4">
        {stats.map(s => (
          <Card key={s.label} className="p-5 text-center">
            <p className="text-2xl font-bold mb-1" style={{ color: t.text }}>{loading ? "…" : s.value}</p>
            <p className="font-medium" style={{ color: t.text, ...small(12) }}>{s.label}</p>
            <p className="mt-0.5" style={{ color: t.muted, ...small(11) }}>{s.sub}</p>
          </Card>
        ))}
      </div>

      <div className="grid grid-cols-2 gap-5">
        {/* Account info */}
        <Card>
          <CardHead title="계정 정보" sub={NOT_LINKED} />
          <div className="p-5 space-y-4">
            {fields.map(f => (
              <div key={f.label} className="flex items-center justify-between py-1"
                style={{ borderBottom: `1px solid ${t.border}` }}>
                <span style={{ color: t.muted, ...small(12) }}>{f.label}</span>
                <span className="font-semibold" style={{ color: t.text, ...small(13) }}>{f.value}</span>
              </div>
            ))}
          </div>
        </Card>

        {/* Recent activity (주행 기록 기준) */}
        <Card>
          <CardHead title="최근 활동" sub="최근 주행 기록 기준" />
          <div>
            {recent.length === 0 && (
              <p className="px-5 py-10 text-center" style={{ color: t.muted, ...small(12) }}>
                {tripsLoading ? "불러오는 중입니다." : "아직 주행 기록이 없습니다."}
              </p>
            )}
            {recent.map((l, i) => (
              <div key={l.id} className="px-5 py-3.5 flex items-start gap-3"
                style={{ borderBottom: i < recent.length - 1 ? `1px solid ${t.border}` : undefined }}>
                <div className="w-1.5 h-1.5 rounded-full mt-1.5 shrink-0" style={{ background: C.blue }} />
                <div className="flex-1 min-w-0">
                  <p className="font-medium" style={{ color: t.text, ...small(12) }}>{l.action}</p>
                  <p className="mt-0.5" style={{ color: t.muted, ...small(11) }}>{l.time}</p>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
