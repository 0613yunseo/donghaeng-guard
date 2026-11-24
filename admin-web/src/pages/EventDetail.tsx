import { useMemo } from "react";
import { AlertTriangle, ArrowLeft, BrainCircuit, ChevronRight, Navigation2, Cpu } from "lucide-react";
import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";
import { Card, CardHead, PrimaryBtn, OutlineBtn } from "../components/ui";
import { RiskBadge } from "../components/badges/RiskBadge";
import { useTripDetail } from "../hooks/useTripDetail";
import { imageSrc } from "../lib/imageSrc";
import { ZoneMap } from "../components/map/ZoneMap";

export function EventDetail() {
  const { t, selId, selTripId, onNav } = useApp();
  const { events, loading, error } = useTripDetail(selTripId || null);
  const e = events.find(x => x.id === selId);
  // 이벤트 1건을 지도 마커로 쓴다(ZoneMap 재사용: DANGER→HIGH, WARNING→MEDIUM, SAFE→LOW)
  const mapZones = useMemo(() => (e ? [{
    id: `이벤트 ${e.id}`,
    grade: e.rl === "DANGER" ? "HIGH" as const : e.rl === "WARNING" ? "MEDIUM" as const : "LOW" as const,
    score: 0, cnt: 0, loc: "", label: `이벤트 ${e.id} · ${e.rl} · ${e.at}`,
    lat: e.lat ?? null, lng: e.lng ?? null,
  }] : []), [e]);

  if (loading || error || !e) {
    return <Card>
      <CardHead title="이벤트 상세 정보" />
      <p role={error ? "alert" : "status"} className="p-5 text-sm" style={{ color: t.muted }}>
        {loading ? "센서 이벤트를 불러오는 중입니다." : error ?? "해당 이벤트가 주행 1번에 없습니다."}
      </p>
      <div className="p-5"><OutlineBtn icon={ArrowLeft} onClick={() => onNav("events")}>목록으로 돌아가기</OutlineBtn></div>
    </Card>;
  }
  const isDanger = e.rl === "DANGER";
  const isCamera = e.sensor === "CAMERA";

  const fields: Array<{ l: string; v: React.ReactNode }> = [
    { l: "이벤트 ID", v: e.id },
    { l: "주행 ID", v: e.trip },
    { l: "사용자 ID", v: e.user },
    { l: "디바이스 ID", v: e.dev },
    { l: "센서 종류", v: e.sensor },
    { l: "위험 유형", v: e.rt },
    { l: "위험 단계", v: <RiskBadge level={e.rl} /> },
    { l: "감지 거리", v: e.mm == null ? "—" : `${e.mm} mm` },
    { l: "위도", v: (e.lat?.toFixed(6) ?? "—") },
    { l: "경도", v: (e.lng?.toFixed(6) ?? "—") },
    { l: "감지 시각", v: e.at },
  ];

  return (
    <div className="space-y-4">
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

      <div className="grid grid-cols-2 gap-4 stack-sm">
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

        {/* Map: 이벤트 위치를 실제 지도에 표시 */}
        <Card>
          <CardHead title="위치 정보" sub={`${(e.lat?.toFixed(6) ?? "—")}, ${(e.lng?.toFixed(6) ?? "—")}`} />
          <div className="p-4" style={{ height: 322 }}>
            {e.lat != null && e.lng != null ? (
              <ZoneMap zones={mapZones} />
            ) : (
              <p className="text-sm" style={{ color: t.muted }}>이 이벤트에는 위치 좌표가 없습니다.</p>
            )}
          </div>
        </Card>
      </div>

      {/* Camera event details */}
      {isCamera && (
        <Card>
          <CardHead
            title="카메라 감지 상세"
            sub="실제 센서 이벤트의 객체 인식 결과 및 이미지"
          />

          <div className="p-5 space-y-4">
            <div className="grid grid-cols-3 gap-4">
              <div>
                <p className="text-xs mb-1" style={{ color: t.muted }}>
                  감지 객체
                </p>
                <p className="text-sm font-semibold" style={{ color: t.text }}>
                  {e.detectedObject ?? "—"}
                </p>
              </div>

              <div>
                <p className="text-xs mb-1" style={{ color: t.muted }}>
                  탐지 신뢰도
                </p>
                <p className="text-sm font-semibold" style={{ color: t.text }}>
                  {e.confidence != null
                    ? `${(e.confidence * 100).toFixed(1)}%`
                    : "—"}
                </p>
              </div>

              <div>
                <p className="text-xs mb-1" style={{ color: t.muted }}>
                  표준편차
                </p>
                <p className="text-sm font-semibold" style={{ color: t.text }}>
                  {e.stdDev ?? "—"}
                </p>
              </div>
            </div>

            {e.imageUrl ? (
              <div>
                <p className="text-xs font-semibold mb-2" style={{ color: t.muted }}>
                  감지 이미지
                </p>

                <img
                  src={imageSrc(e.imageUrl)}
                  alt={`${e.detectedObject ?? "객체"} 감지 이미지`}
                  className="w-full max-h-96 object-contain rounded-xl"
                  style={{ background: t.bgSub }}
                />
              </div>
            ) : (
              <p className="text-xs" style={{ color: t.muted }}>
                해당 이벤트에 연결된 이미지가 없습니다.
              </p>
            )}
          </div>
        </Card>
      )}

      {/* Action buttons */}
      <div className="flex items-center gap-3">
        <PrimaryBtn icon={Navigation2} onClick={() => onNav("trips")}>연결된 주행 보기</PrimaryBtn>
        <OutlineBtn icon={Cpu} onClick={() => onNav("devices")}>연결된 디바이스 보기</OutlineBtn>
        <OutlineBtn icon={BrainCircuit} onClick={() => onNav("ai")}>AI 분석 보기</OutlineBtn>
        <OutlineBtn icon={ArrowLeft} onClick={() => onNav("events")}>목록으로 돌아가기</OutlineBtn>
      </div>
    </div>
  );
}
