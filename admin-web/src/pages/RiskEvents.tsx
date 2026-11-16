import { useState } from "react";
import { toast } from "sonner";
import { Download } from "lucide-react";
import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";
import { Card, CardHead, FilterBar, SelectInput, PrimaryBtn, OutlineBtn } from "../components/ui";
import { RiskEventTable } from "../components/tables/RiskEventTable";
import { useTripDetail } from "../hooks/useTripDetail";
import { useTripList } from "../hooks/useTripList";
import type { RiskEvent, RiskLevel } from "../types/admin";

function getRiskTransitions(events: RiskEvent[]): RiskEvent[] {
  const sorted = [...events].sort((a, b) => {
    const timeA = new Date(a.at.replace(" ", "T")).getTime();
    const timeB = new Date(b.at.replace(" ", "T")).getTime();

    return timeA - timeB || Number(a.id) - Number(b.id);
  });

  const previousRiskBySensor = new Map<string, RiskLevel>();

  return sorted.filter(event => {
    const key = `${event.trip}:${event.dev}:${event.sensor}`;
    const previousRisk = previousRiskBySensor.get(key);

    previousRiskBySensor.set(key, event.rl);

    // 첫 번째 측정값은 비교 대상이 없으므로 제외
    if (previousRisk === undefined) {
      return false;
    }

    return previousRisk !== event.rl;
  });
}

export function RiskEvents() {
  const { t, onNav, searchQuery } = useApp();

  // ─── 주행 목록 ──────────────────────────────────────────────
  const { trips: tripList, loading: tripsLoading } = useTripList();

  // ─── 드래프트 필터 (적용 전) ────────────────────────────────
  const [draftTripId, setDraftTripId] = useState("");
  const [draftRl, setDraftRl] = useState("");
  const [draftSensor, setDraftSensor] = useState("");
  const [draftDev, setDraftDev] = useState("");

  // ─── 적용된 필터 ────────────────────────────────────────────
  const [appliedTripId, setAppliedTripId] = useState("");
  const [applied, setApplied] = useState({ rl: "", sensor: "", rt: "", dev: "" });

  // ─── Trip 상세 데이터 (적용된 tripId 기준) ──────────────────
  const { events, loading, error } = useTripDetail(appliedTripId || null);

  const transitionEvents = getRiskTransitions(events);

  // ─── 이벤트 필터링 ──────────────────────────────────────────
  const filtered = transitionEvents.filter(e => {
    const q = searchQuery.toLowerCase();
    const matchSearch = !q
      || e.id.toLowerCase().includes(q)
      || e.dev.toLowerCase().includes(q)
      || e.sensor.toLowerCase().includes(q)
      || e.rt.toLowerCase().includes(q)
      || e.trip.toLowerCase().includes(q);
    return matchSearch
      && (!applied.rl || e.rl === applied.rl)
      && (!applied.sensor || e.sensor === applied.sensor)
      && (!applied.rt || e.rt === applied.rt)
      && (!applied.dev || e.dev === applied.dev);
  });

  // ─── 섹션 헤딩 ──────────────────────────────────────────────
  const sectionTitle = appliedTripId
    ? `위험 단계 변경 이력 (주행 ID: ${appliedTripId})`
    : `위험 단계 변경 이력 (주행 미선택)`;

  // ─── 필터 이벤트 핸들러 ─────────────────────────────────────
  const apply = () => {
    setAppliedTripId(draftTripId);
    setApplied({ rl: draftRl, sensor: draftSensor, rt: "", dev: draftDev });
    toast.success("필터가 적용되었습니다.");
  };

  const reset = () => {
    setDraftTripId("");
    setDraftRl("");
    setDraftSensor("");
    setDraftDev("");
    setAppliedTripId("");
    setApplied({ rl: "", sensor: "", rt: "", dev: "" });
    toast.info("필터가 초기화되었습니다.");
  };

  // ─── 센서 목록 (드래프트 드롭다운용) ──────────────────────────
  const sensorOptions = Array.from(new Set(events.map(e => e.sensor)));

  return (
    <div className="space-y-4">
      <FilterBar>
        {/* 주행 선택 드롭다운 */}
        <SelectInput value={draftTripId} onChange={setDraftTripId}>
          <option value="">주행 선택...</option>
          {tripsLoading ? (
            <option value="" disabled>주행 목록 불러오는 중...</option>
          ) : (
            tripList.map(trip => (
              <option key={trip.tripId} value={String(trip.tripId)}>
                주행 #{trip.tripId}
              </option>
            ))
          )}
        </SelectInput>

        {/* 위험 단계 필터 */}
        <SelectInput value={draftRl} onChange={setDraftRl}>
          <option value="">위험 단계 전체</option>
          {(["DANGER", "WARNING", "SAFE"] as RiskLevel[]).map(v => <option key={v} value={v}>{v}</option>)}
        </SelectInput>

        {/* 센서 종류 필터 */}
        <SelectInput value={draftSensor} onChange={setDraftSensor}>
          <option value="">센서 종류 전체</option>
          {sensorOptions.map(v => <option key={v} value={v}>{v}</option>)}
        </SelectInput>

        {/* 디바이스 필터 */}
        <SelectInput value={draftDev} onChange={setDraftDev}>
          <option value="">디바이스 전체</option>
          <option value="" disabled>디바이스 정보 미제공</option>
        </SelectInput>

        <PrimaryBtn onClick={apply}>적용</PrimaryBtn>
        <OutlineBtn onClick={reset}>초기화</OutlineBtn>

        {searchQuery && (
          <span className="text-xs px-2 py-0.5 rounded-full" style={{ background: `${C.blue}15`, color: C.blue }}>
            검색: "{searchQuery}"
          </span>
        )}
      </FilterBar>

      <Card>
        <CardHead
          title={sectionTitle}
          sub={
            !appliedTripId
              ? "주행을 선택하고 [적용]을 클릭하면 이벤트를 조회합니다."
              : `${filtered.length}건 / 변경 이벤트 ${transitionEvents.length}건 · 전체 측정 ${events.length}건`
          }
          right={
            <OutlineBtn icon={Download} onClick={() => toast.success("CSV 파일이 다운로드되었습니다.")}>
              내보내기
            </OutlineBtn>
          }
        />
        {!appliedTripId ? (
          <p className="p-5 text-sm" style={{ color: t.muted }}>주행을 선택하고 적용 버튼을 누르세요.</p>
        ) : loading ? (
          <p role="status" className="p-5 text-sm" style={{ color: t.muted }}>센서 이벤트를 불러오는 중입니다.</p>
        ) : error ? (
          <p role="alert" className="p-5 text-sm">{error}</p>
        ) : events.length > 0 && transitionEvents.length === 0 ? (
          <p className="p-5 text-sm" style={{ color: t.muted }}>
            센서 측정 데이터는 존재하지만 위험 단계 변경 이력은 없습니다. (전체 측정 {events.length}건)
          </p>
        ) : (
          <RiskEventTable events={filtered} onRowClick={(id) => onNav("event-detail", id, appliedTripId)} />
        )}
      </Card>
    </div>
  );
}
