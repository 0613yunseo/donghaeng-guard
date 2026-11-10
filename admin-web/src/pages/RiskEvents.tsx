import { useState } from "react";
import { toast } from "sonner";
import { Download } from "lucide-react";
import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";
import { Card, CardHead, FilterBar, SelectInput, PrimaryBtn, OutlineBtn } from "../components/ui";
import { RiskEventTable } from "../components/tables/RiskEventTable";
import { useTripDetail } from "../hooks/useTripDetail";
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
  const { events, loading, error } = useTripDetail();
  const transitionEvents = getRiskTransitions(events);
  const [rl, setRl] = useState("");
  const [sensor, setSensor] = useState("");
  const [rt, setRt] = useState("");
  const [dev, setDev] = useState("");
  const [applied, setApplied] = useState({ rl: "", sensor: "", rt: "", dev: "" });

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

  const apply = () => {
    setApplied({ rl, sensor, rt, dev });
    toast.success("필터가 적용되었습니다.");
  };
  const reset = () => {
    setRl(""); setSensor(""); setRt(""); setDev("");
    setApplied({ rl: "", sensor: "", rt: "", dev: "" });
    toast.info("필터가 초기화되었습니다.");
  };

  return (
    <div className="space-y-4">
      <FilterBar>
        <SelectInput value={rl} onChange={setRl}>
          <option value="">위험 단계 전체</option>
          {(["DANGER", "WARNING", "SAFE"] as RiskLevel[]).map(v => <option key={v} value={v}>{v}</option>)}
        </SelectInput>
        <SelectInput value={sensor} onChange={setSensor}>
          <option value="">센서 종류 전체</option>
          {Array.from(new Set(events.map(e => e.sensor))).map(v => <option key={v} value={v}>{v}</option>)}
        </SelectInput>

        <SelectInput value={dev} onChange={setDev}>
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
          title={`위험 단계 변경 이력 (주행 ID: ${events[0]?.trip ?? "—"})`}
          sub={`${filtered.length}건 / 변경 이벤트 ${transitionEvents.length}건 · 전체 측정 ${events.length}건`}
          right={
            <OutlineBtn icon={Download} onClick={() => toast.success("CSV 파일이 다운로드되었습니다.")}>
              내보내기
            </OutlineBtn>
          }
        />
        {loading ? <p role="status" className="p-5 text-sm" style={{ color: t.muted }}>센서 이벤트를 불러오는 중입니다.</p>
          : error ? <p role="alert" className="p-5 text-sm">{error}</p>
            : <RiskEventTable events={filtered} onRowClick={(id) => onNav("event-detail", id)} />}
      </Card>
    </div>
  );
}
