import { useMemo, useState } from "react";
import { toast } from "sonner";
import { useApp } from "../contexts/AppContext";
import { gradeColor } from "../constants/colors";
import { Card, CardHead, FilterBar, SelectInput, PrimaryBtn, OutlineBtn } from "../components/ui";
import { GradeBadge } from "../components/badges/RiskBadge";
import { ZoneMap } from "../components/map/ZoneMap";
import { useApiList } from "../hooks/useApiList";
import type { RiskGrade } from "../types/admin";

interface ZoneApiItem {
  zoneId: number;
  latitude: number | null;
  longitude: number | null;
  riskScore: number | null;
  riskGrade: RiskGrade | null;
  zoneEventCount: number | null;
  reason: string | null;
  updatedAt: string | null;
}

interface ZoneView {
  id: string;
  grade: RiskGrade;
  score: number;
  cnt: number;
  last: string;
  loc: string;
  lat: number | null;
  lng: number | null;
}

export function RiskZones() {
  const { t } = useApp();
  const [grade, setGrade] = useState("");
  const [applied, setApplied] = useState({ grade: "" });

  const { items, loading, error } = useApiList<ZoneApiItem>("/api/risk-zones", "위험 구간");
  const ZONES: ZoneView[] = useMemo(() => items.map(z => ({
    id: `ZONE-${z.zoneId}`,
    grade: z.riskGrade ?? "LOW",
    score: z.riskScore ?? 0,
    cnt: z.zoneEventCount ?? 0,
    last: z.updatedAt?.replace("T", " ") ?? "—",
    loc: z.reason?.trim() || (z.latitude != null && z.longitude != null
      ? `${z.latitude.toFixed(4)}, ${z.longitude.toFixed(4)}` : "위치 정보 없음"),
    lat: z.latitude,
    lng: z.longitude,
  })), [items]);

  const filteredZones = useMemo(() => ZONES.filter(z => !applied.grade || z.grade === applied.grade), [ZONES, applied.grade]);

  const apply = () => {
    setApplied({ grade });
    toast.success("필터가 적용되었습니다.");
  };
  const reset = () => {
    setGrade("");
    setApplied({ grade: "" });
    toast.info("필터가 초기화되었습니다.");
  };

  return (
    <div className="space-y-4">
      <FilterBar>
        <SelectInput value={grade} onChange={setGrade}>
          <option value="">위험도 전체</option>
          {(["HIGH", "MEDIUM", "LOW"] as RiskGrade[]).map(g => <option key={g} value={g}>{g}</option>)}
        </SelectInput>
        <PrimaryBtn onClick={apply}>적용</PrimaryBtn>
        <OutlineBtn onClick={reset}>초기화</OutlineBtn>
        <span className="text-xs ml-auto" style={{ color: t.muted }}>{filteredZones.length}개 구간 표시 중</span>
      </FilterBar>

      <div className="grid grid-cols-3 gap-4 stack-sm" style={{ height: "calc(100vh - 280px)", minHeight: 480 }}>
        {/* Map panel */}
        <Card className="col-span-2 flex flex-col overflow-hidden">
          <CardHead
            title="위험 구간 지도"
            sub="OpenStreetMap · 구간 좌표 기준"
            right={
              <div className="flex items-center gap-4 text-[11px]" style={{ color: t.muted }}>
                {(["HIGH", "MEDIUM", "LOW"] as RiskGrade[]).map(g => (
                  <span key={g} className="flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full" style={{ background: gradeColor(g) }} />{g}
                  </span>
                ))}
              </div>
            }
          />
          <div className="flex-1 m-4 mt-0" style={{ minHeight: 320 }}>
            <ZoneMap zones={filteredZones} />
          </div>
        </Card>

        {/* List panel */}
        <Card className="flex flex-col overflow-hidden">
          <CardHead title="위험 구간 목록" sub={`${filteredZones.length}개 구간`} />
          <div className="flex-1 overflow-y-auto p-3 space-y-2">
            {error ? (
              <div role="alert" className="py-10 text-center text-xs">{error}</div>
            ) : loading ? (
              <div role="status" className="py-10 text-center text-xs" style={{ color: t.muted }}>불러오는 중입니다.</div>
            ) : filteredZones.length === 0 ? (
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
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}
