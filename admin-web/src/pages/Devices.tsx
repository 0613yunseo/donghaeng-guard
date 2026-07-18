import { useState } from "react";
import { toast } from "sonner";
import { Download, RefreshCw, CheckCircle2, Clock, XCircle } from "lucide-react";
import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";
import { Card, CardHead, FilterBar, SelectInput, PrimaryBtn, OutlineBtn } from "../components/ui";
import { StatusCard } from "../components/cards/StatusCard";
import { DeviceTable } from "../components/tables/DeviceTable";
import { DEVICES } from "../mocks/adminMockData";
import type { DeviceStatus } from "../types/admin";

export function Devices() {
  const { t } = useApp();
  const [stF,     setStF]     = useState("");
  const [sensor,  setSensor]  = useState("");
  const [applied, setApplied] = useState({ st: "", sensor: "" });
  const [spinning, setSpinning] = useState(false);

  const filtered = DEVICES.filter(d =>
    (!applied.st     || d.st === applied.st) &&
    (!applied.sensor || d.sensors.includes(applied.sensor))
  );

  const conn = DEVICES.filter(d => d.st === "CONNECTED").length;
  const disc = DEVICES.filter(d => d.st === "DISCONNECTED").length;
  const err  = DEVICES.filter(d => d.st === "ERROR").length;

  const applyStatusFilter = (val: DeviceStatus) => {
    setStF(val);
    setApplied({ st: val, sensor: applied.sensor });
  };

  const apply = () => {
    setApplied({ st: stF, sensor });
    toast.success("필터가 적용되었습니다.");
  };
  const reset = () => {
    setStF(""); setSensor("");
    setApplied({ st: "", sensor: "" });
    toast.info("필터가 초기화되었습니다.");
  };

  const handleRefresh = () => {
    setSpinning(true);
    setTimeout(() => {
      setSpinning(false);
      toast.success("디바이스 상태가 새로고침되었습니다.");
    }, 1200);
  };

  return (
    <div className="space-y-4">
      <FilterBar>
        <SelectInput value={stF} onChange={setStF}>
          <option value="">상태 전체</option>
          {(["CONNECTED","DISCONNECTED","ERROR"] as DeviceStatus[]).map(v => <option key={v} value={v}>{v}</option>)}
        </SelectInput>
        <SelectInput value={sensor} onChange={setSensor}>
          <option value="">센서 전체</option>
          {["TOF","ULTRASONIC"].map(v => <option key={v} value={v}>{v}</option>)}
        </SelectInput>
        <PrimaryBtn onClick={apply}>적용</PrimaryBtn>
        <OutlineBtn onClick={reset}>초기화</OutlineBtn>
        <span className="text-xs ml-auto" style={{ color: t.muted }}>{filtered.length}개 표시 중</span>
      </FilterBar>

      {/* Status summary cards */}
      <div className="grid grid-cols-3 gap-4">
        <StatusCard label="연결됨"    count={conn} Icon={CheckCircle2} accent={C.mint}   onClick={() => applyStatusFilter("CONNECTED")}    />
        <StatusCard label="연결 끊김" count={disc} Icon={Clock}        accent={t.muted}  onClick={() => applyStatusFilter("DISCONNECTED")} />
        <StatusCard label="오류"      count={err}  Icon={XCircle}      accent={C.danger} onClick={() => applyStatusFilter("ERROR")}         />
      </div>

      <Card>
        <CardHead
          title="디바이스 목록"
          sub={`${filtered.length}개 / 전체 ${DEVICES.length}개`}
          right={
            <div className="flex gap-2">
              <OutlineBtn icon={RefreshCw} onClick={handleRefresh}>
                <span className={spinning ? "inline-block animate-spin" : ""}>
                  {spinning ? "⟳" : "새로고침"}
                </span>
              </OutlineBtn>
              <OutlineBtn icon={Download} onClick={() => toast.success("CSV 파일이 다운로드되었습니다.")}>
                내보내기
              </OutlineBtn>
            </div>
          }
        />
        <DeviceTable devices={filtered} />
      </Card>
    </div>
  );
}
