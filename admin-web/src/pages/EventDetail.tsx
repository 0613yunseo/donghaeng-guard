import { useState } from "react";
import { AlertTriangle, ArrowLeft, ChevronRight, Navigation2, Cpu, Crosshair } from "lucide-react";
import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";
import { Card, CardHead, PrimaryBtn, OutlineBtn } from "../components/ui";
import { RiskBadge } from "../components/badges/RiskBadge";
import { EVENTS } from "../mocks/adminMockData";

export function EventDetail() {
  const { t, selId, onNav } = useApp();
  const e = EVENTS.find(x => x.id === selId) ?? EVENTS[0];
  const isDanger = e.rl === "DANGER";
  const [zoom, setZoom] = useState(1);

  const fields: Array<{ l: string; v: React.ReactNode }> = [
    { l: "이벤트 ID",  v: e.id      },
    { l: "주행 ID",    v: e.trip    },
    { l: "사용자 ID",  v: e.user    },
    { l: "디바이스 ID",v: e.dev     },
    { l: "센서 종류",  v: e.sensor  },
    { l: "위험 유형",  v: e.rt      },
    { l: "위험 단계",  v: <RiskBadge level={e.rl} /> },
    { l: "감지 거리",  v: `${e.mm} mm` },
    { l: "위도",        v: e.lat.toFixed(6) },
    { l: "경도",        v: e.lng.toFixed(6) },
    { l: "감지 시각",  v: e.at      },
  ];

  return (
    <div className="space-y-4 max-w-5xl">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-xs" style={{ color: t.muted }}>
        <button onClick={() => onNav("events")} className="flex items-center gap-1 hover:opacity-70 transition-opacity">
          <ArrowLeft size={13} />목록으로
        </button>
        <ChevronRight size={12} style={{ color: t.border }} />
        <span className="font-semibold" style={{ color: t.text }}>{e.id}</span>
        <RiskBadge level={e.rl} />
      </div>

      {/* Danger alert */}
      {isDanger && (
        <div className="flex items-center gap-3 p-4 rounded-2xl"
          style={{ background: `${C.danger}0f`, border: `1px solid ${C.danger}33` }}>
          <div className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0" style={{ background: C.danger }}>
            <AlertTriangle size={16} color="#fff" />
          </div>
          <div>
            <p className="text-sm font-bold" style={{ color: C.danger }}>DANGER 등급 이벤트</p>
            <p className="text-xs mt-0.5" style={{ color: "#c0202d" }}>즉각적인 조치가 필요합니다. 현장 점검을 권장합니다.</p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-2 gap-4">
        {/* Event details */}
        <Card>
          <CardHead title="이벤트 상세 정보" />
          <div className="p-5 grid grid-cols-2 gap-x-6 gap-y-5">
            {fields.map(f => (
              <div key={f.l}>
                <p className="text-[10px] font-semibold uppercase tracking-wider mb-1" style={{ color: t.muted }}>{f.l}</p>
                <div className="text-[13px] font-semibold" style={{ color: t.text }}>{f.v}</div>
              </div>
            ))}
          </div>
        </Card>

        {/* Map */}
        <Card>
          <CardHead title="위치 정보" sub={`${e.lat.toFixed(6)}, ${e.lng.toFixed(6)}`} />
          <div className="p-4">
            <div className="relative rounded-xl overflow-hidden"
              style={{ height: 290, background: "linear-gradient(150deg,#0d1e38,#071424)" }}>
              <svg className="absolute inset-0 w-full h-full" style={{ opacity: 0.11 }} viewBox="0 0 440 290">
                {[36,72,108,144,180,216,252].map(y =>
                  <line key={y} x1="0" y1={y} x2="440" y2={y} stroke="#3A7BD5" strokeWidth="1" />
                )}
                {[55,110,165,220,275,330,385].map(x =>
                  <line key={x} x1={x} y1="0" x2={x} y2="290" stroke="#3A7BD5" strokeWidth="1" />
                )}
                <path d="M20 180 Q90 120 160 150 Q230 180 300 110 Q360 50 430 80" stroke="#3A7BD5" strokeWidth="3" fill="none" />
                <rect x="60" y="60" width="100" height="65" rx="6" fill="#1e3a6a" opacity="0.5" />
                <rect x="200" y="110" width="80" height="50" rx="6" fill="#1e3a6a" opacity="0.4" />
                <rect x="310" y="170" width="110" height="70" rx="6" fill="#1e3a6a" opacity="0.4" />
              </svg>

              <div className="absolute inset-0 transition-transform duration-300 origin-center"
                style={{ transform: `scale(${zoom})` }}>
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-full flex flex-col items-center">
                  <div className="relative w-12 h-12 rounded-full flex items-center justify-center shadow-2xl"
                    style={{ background: isDanger ? C.danger : C.warn, boxShadow: `0 0 24px ${isDanger ? C.danger : C.warn}60` }}>
                    <Crosshair size={22} color="#fff" />
                    <span className="absolute top-0 right-0 w-3 h-3 rounded-full border-2 border-white animate-ping"
                      style={{ background: isDanger ? C.danger : C.warn }} />
                  </div>
                  <div className="mt-1.5 px-3 py-0.5 rounded-full text-[11px] font-bold text-white shadow"
                    style={{ background: isDanger ? C.danger : C.warn }}>{e.rl}</div>
                </div>
              </div>

              <div className="absolute bottom-3 left-3 rounded-lg px-2.5 py-1.5 text-[10px] font-mono"
                style={{ background: "rgba(0,0,0,0.6)", color: "rgba(255,255,255,0.85)" }}>
                {e.lat.toFixed(6)}, {e.lng.toFixed(6)}
              </div>
              <div className="absolute bottom-3 right-3 flex flex-col gap-1">
                <button onClick={() => setZoom(z => Math.min(3, +(z + 0.5).toFixed(1)))}
                  className="w-6 h-6 rounded flex items-center justify-center text-sm font-bold hover:opacity-80"
                  style={{ background: "rgba(0,0,0,0.55)", color: "rgba(255,255,255,0.8)" }}>+</button>
                <button onClick={() => setZoom(z => Math.max(1, +(z - 0.5).toFixed(1)))}
                  className="w-6 h-6 rounded flex items-center justify-center text-sm font-bold hover:opacity-80"
                  style={{ background: "rgba(0,0,0,0.55)", color: "rgba(255,255,255,0.8)" }}>−</button>
              </div>
            </div>
          </div>
        </Card>
      </div>

      {/* Action buttons */}
      <div className="flex items-center gap-3">
        <PrimaryBtn icon={Navigation2} onClick={() => onNav("trips")}>연결된 주행 보기</PrimaryBtn>
        <OutlineBtn icon={Cpu} onClick={() => onNav("devices")}>연결된 디바이스 보기</OutlineBtn>
        <OutlineBtn icon={ArrowLeft} onClick={() => onNav("events")}>목록으로 돌아가기</OutlineBtn>
      </div>
    </div>
  );
}
