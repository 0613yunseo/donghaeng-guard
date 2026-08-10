import { useState } from "react";
import { toast } from "sonner";
import { MapPin } from "lucide-react";
import { useApp } from "../contexts/AppContext";
import { C, gradeColor } from "../constants/colors";
import { Card, CardHead, FilterBar, SelectInput, PrimaryBtn, OutlineBtn } from "../components/ui";
import { GradeBadge, ZsBadge } from "../components/badges/RiskBadge";
import { MapPlaceholder } from "../components/map/MapPlaceholder";
import { ZONES } from "../mocks/adminMockData";
import type { RiskGrade, ZoneStatus } from "../types/admin";

const PIN_POSITIONS = [
  { zoneId: "ZONE-001", x: "38%", y: "34%" },
  { zoneId: "ZONE-002", x: "60%", y: "46%" },
  { zoneId: "ZONE-003", x: "27%", y: "60%" },
  { zoneId: "ZONE-004", x: "54%", y: "22%" },
  { zoneId: "ZONE-005", x: "70%", y: "65%" },
];

const MIN_ZOOM = 1;
const MAX_ZOOM = 3;

export function RiskZones() {
  const { t } = useApp();
  const [zoom,    setZoom]    = useState(1);
  const [grade,   setGrade]   = useState("");
  const [status,  setStatus]  = useState("");
  const [applied, setApplied] = useState({ grade: "", status: "" });

  const filteredZones = ZONES.filter(z =>
    (!applied.grade  || z.grade === applied.grade) &&
    (!applied.status || z.st    === applied.status)
  );

  const filteredPins = PIN_POSITIONS
    .map(p => ({ ...p, zone: ZONES.find(z => z.id === p.zoneId) }))
    .filter(p => p.zone &&
      (!applied.grade  || p.zone.grade === applied.grade) &&
      (!applied.status || p.zone.st    === applied.status)
    ) as Array<{ zoneId: string; x: string; y: string; zone: typeof ZONES[0] }>;

  const apply = () => {
    setApplied({ grade, status });
    toast.success("필터가 적용되었습니다.");
  };
  const reset = () => {
    setGrade(""); setStatus("");
    setApplied({ grade: "", status: "" });
    toast.info("필터가 초기화되었습니다.");
  };

  return (
    <div className="space-y-4">
      <FilterBar>
        <SelectInput value={grade} onChange={setGrade}>
          <option value="">위험도 전체</option>
          {(["HIGH","MEDIUM","LOW"] as RiskGrade[]).map(g => <option key={g} value={g}>{g}</option>)}
        </SelectInput>
        <SelectInput value={status} onChange={setStatus}>
          <option value="">상태 전체</option>
          {(["확인 전","검토 중","조치 완료"] as ZoneStatus[]).map(s => <option key={s} value={s}>{s}</option>)}
        </SelectInput>
        <PrimaryBtn onClick={apply}>적용</PrimaryBtn>
        <OutlineBtn onClick={reset}>초기화</OutlineBtn>
        <span className="text-xs ml-auto" style={{ color: t.muted }}>{filteredZones.length}개 구간 표시 중</span>
      </FilterBar>

      <div className="grid grid-cols-3 gap-4" style={{ height: "calc(100vh - 280px)", minHeight: 480 }}>
        {/* Map panel */}
        <Card className="col-span-2 flex flex-col overflow-hidden">
          <CardHead
            title="위험 구간 지도"
            sub="서울특별시 현황"
            right={
              <div className="flex items-center gap-4 text-[11px]" style={{ color: t.muted }}>
                {(["HIGH","MEDIUM","LOW"] as RiskGrade[]).map(g => (
                  <span key={g} className="flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full" style={{ background: gradeColor(g) }} />{g}
                  </span>
                ))}
              </div>
            }
          />
          <div className="flex-1 m-4 mt-0" style={{ minHeight: 0 }}>
            <MapPlaceholder label="서울특별시 위험 구간 현황" style={{ height: "100%" }}>
              {/* Zoomable layer */}
              <div
                className="absolute inset-0 transition-transform duration-300 origin-center"
                style={{ transform: `scale(${zoom})` }}>
                {filteredPins.map(({ zoneId, x, y, zone }) => (
                  <div key={zoneId}
                    className="absolute -translate-x-1/2 -translate-y-1/2 group cursor-pointer z-10"
                    style={{ left: x, top: y }}>
                    <div className="relative">
                      <div className="w-10 h-10 rounded-full flex items-center justify-center shadow-xl"
                        style={{ background: gradeColor(zone.grade), boxShadow: `0 0 18px ${gradeColor(zone.grade)}50` }}>
                        <MapPin size={17} color="#fff" />
                      </div>
                      <div className="group-hover:block absolute bottom-full left-1/2 -translate-x-1/2 mb-2.5 z-20 whitespace-nowrap rounded-xl px-3 py-2 shadow-2xl"
                        style={{ background: t.card, border: `1px solid ${t.border}`, boxShadow: t.shadowLg }}>
                        <p className="text-[12px] font-bold" style={{ color: t.text }}>{zone.id}</p>
                        <p className="text-[11px] mt-0.5" style={{ color: t.muted }}>점수 {zone.score} · {zone.cnt}건</p>
                        <p className="text-[11px]" style={{ color: t.muted }}>{zone.loc}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* Zoom controls */}
              <div className="absolute bottom-3 right-3 flex flex-col gap-1.5" style={{ zIndex: 30 }}>
                <button
                  onClick={() => setZoom(z => Math.min(MAX_ZOOM, +(z + 0.5).toFixed(1)))}
                  disabled={zoom >= MAX_ZOOM}
                  className="w-7 h-7 rounded-lg flex items-center justify-center text-sm font-bold transition-opacity"
                  style={{ background: "rgba(0,0,0,0.6)", color: zoom >= MAX_ZOOM ? "rgba(255,255,255,0.3)" : "rgba(255,255,255,0.85)" }}>
                  +
                </button>
                <button
                  onClick={() => setZoom(z => Math.max(MIN_ZOOM, +(z - 0.5).toFixed(1)))}
                  disabled={zoom <= MIN_ZOOM}
                  className="w-7 h-7 rounded-lg flex items-center justify-center text-sm font-bold transition-opacity"
                  style={{ background: "rgba(0,0,0,0.6)", color: zoom <= MIN_ZOOM ? "rgba(255,255,255,0.3)" : "rgba(255,255,255,0.85)" }}>
                  −
                </button>
              </div>

              <div className="absolute bottom-3 left-16 rounded-lg px-2 py-1 text-[10px] font-mono"
                style={{ background: "rgba(0,0,0,0.5)", color: "rgba(255,255,255,0.6)", zIndex: 30 }}>
                {Math.round(zoom * 100)}%
              </div>
            </MapPlaceholder>
          </div>
        </Card>

        {/* List panel */}
        <Card className="flex flex-col overflow-hidden">
          <CardHead title="위험 구간 목록" sub={`${filteredZones.length}개 구간`} />
          <div className="flex-1 overflow-y-auto p-3 space-y-2">
            {filteredZones.length === 0 ? (
              <div className="py-10 text-center text-xs" style={{ color: t.muted }}>조건에 맞는 구간이 없습니다.</div>
            ) : filteredZones.map(z => (
              <div key={z.id}
                className="p-3.5 rounded-xl cursor-pointer transition-colors"
                style={{ border: `1px solid ${t.border}`, background: t.bgSub }}
                onMouseEnter={e => (e.currentTarget.style.background = t.cardHover)}
                onMouseLeave={e => (e.currentTarget.style.background = t.bgSub)}>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-[12px] font-bold" style={{ color: t.text }}>{z.id}</span>
                  <GradeBadge g={z.grade} />
                </div>
                <p className="text-[11px] truncate mb-2" style={{ color: t.muted }}>{z.loc}</p>
                <div className="flex items-center gap-2 mb-2">
                  <div className="flex-1 h-1.5 rounded-full overflow-hidden" style={{ background: t.border }}>
                    <div className="h-full rounded-full" style={{ width: `${z.score}%`, background: gradeColor(z.grade) }} />
                  </div>
                  <span className="text-[11px] font-bold shrink-0" style={{ color: t.text }}>{z.score}점</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-[10px]" style={{ color: t.muted }}>{z.cnt}건 · {z.last}</span>
                  <ZsBadge s={z.st} />
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
