import { useState } from "react";
import { toast } from "sonner";
import { Download } from "lucide-react";
import { useApp } from "../contexts/AppContext";
import { Card, CardHead, FilterBar, SelectInput, PrimaryBtn, OutlineBtn } from "../components/ui";
import { TripTable } from "../components/tables/TripTable";
import { TRIPS, DEVICES } from "../mocks/adminMockData";
import type { RiskLevel, TripStatus } from "../types/admin";

export function Trips() {
  const { t } = useApp();
  const [st,      setSt]      = useState("");
  const [rl,      setRl]      = useState("");
  const [dev,     setDev]     = useState("");
  const [applied, setApplied] = useState({ st: "", rl: "", dev: "" });

  const filtered = TRIPS.filter(trip =>
    (!applied.st  || trip.st  === applied.st) &&
    (!applied.rl  || trip.rl  === applied.rl) &&
    (!applied.dev || trip.dev === applied.dev)
  );

  const apply = () => {
    setApplied({ st, rl, dev });
    toast.success("필터가 적용되었습니다.");
  };
  const reset = () => {
    setSt(""); setRl(""); setDev("");
    setApplied({ st: "", rl: "", dev: "" });
    toast.info("필터가 초기화되었습니다.");
  };

  return (
    <div className="space-y-4">
      <FilterBar>
        <SelectInput value={st} onChange={setSt}>
          <option value="">주행 상태 전체</option>
          {(["STARTED","ENDED"] as TripStatus[]).map(v => <option key={v} value={v}>{v}</option>)}
        </SelectInput>
        <SelectInput value={rl} onChange={setRl}>
          <option value="">위험 단계 전체</option>
          {(["DANGER","WARNING","SAFE"] as RiskLevel[]).map(v => <option key={v} value={v}>{v}</option>)}
        </SelectInput>
        <SelectInput value={dev} onChange={setDev}>
          <option value="">디바이스 전체</option>
          {DEVICES.map(d => <option key={d.id} value={d.id}>{d.id}</option>)}
        </SelectInput>
        <PrimaryBtn onClick={apply}>적용</PrimaryBtn>
        <OutlineBtn onClick={reset}>초기화</OutlineBtn>
        <span className="text-xs ml-auto" style={{ color: t.muted }}>{filtered.length}건 표시 중</span>
      </FilterBar>

      <Card>
        <CardHead
          title="주행 기록 목록"
          sub={`${filtered.length}건 / 전체 ${TRIPS.length}건`}
          right={
            <OutlineBtn icon={Download} onClick={() => toast.success("CSV 파일이 다운로드되었습니다.")}>
              내보내기
            </OutlineBtn>
          }
        />
        <TripTable trips={filtered} />
      </Card>
    </div>
  );
}
