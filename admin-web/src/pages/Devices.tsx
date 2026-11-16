import { useState } from "react";
import { toast } from "sonner";
import { Download, RefreshCw, CheckCircle2, Clock, XCircle } from "lucide-react";
import { useApp } from "../contexts/AppContext";
import { C } from "../constants/colors";
import { Card, CardHead, FilterBar, SelectInput, PrimaryBtn, OutlineBtn } from "../components/ui";
import { StatusCard } from "../components/cards/StatusCard";
import { DeviceTable } from "../components/tables/DeviceTable";
import { useApiList } from "../hooks/useApiList";
import type { Device, DeviceStatus } from "../types/admin";

interface DeviceApiItem { deviceId: string; deviceStatus: DeviceStatus }

export function Devices() {
  const { t } = useApp();
  const [stF,     setStF]     = useState("");
  const [applied, setApplied] = useState({ st: "" });

  // 백엔드는 deviceId/deviceStatus만 제공하므로 나머지 항목은 "—"로 표시한다
  const { items, loading, error, refresh } = useApiList<DeviceApiItem>("/api/devices?userId=1", "디바이스 목록");
  const DEVICES: Device[] = items.map(d => ({
    id: d.deviceId, user: "1", name: "—", sensors: [], st: d.deviceStatus, last: "—",
  }));

  const filtered = DEVICES.filter(d => !applied.st || d.st === applied.st);

  const conn = DEVICES.filter(d => d.st === "CONNECTED").length;
  const disc = DEVICES.filter(d => d.st === "DISCONNECTED").length;
  const err  = DEVICES.filter(d => d.st === "ERROR").length;

  const applyStatusFilter = (val: DeviceStatus) => {
    setStF(val);
    setApplied({ st: val });
  };

  const apply = () => {
    setApplied({ st: stF });
    toast.success("필터가 적용되었습니다.");
  };
  const reset = () => {
    setStF("");
    setApplied({ st: "" });
    toast.info("필터가 초기화되었습니다.");
  };

  const handleRefresh = () => {
    refresh();
    toast.success("디바이스 상태를 다시 조회합니다.");
  };

  return (
    <div className="space-y-4">
      <FilterBar>
        <SelectInput value={stF} onChange={setStF}>
          <option value="">상태 전체</option>
          {(["CONNECTED","DISCONNECTED","ERROR"] as DeviceStatus[]).map(v => <option key={v} value={v}>{v}</option>)}
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
                <span className={loading ? "inline-block animate-spin" : ""}>
                  {loading ? "⟳" : "새로고침"}
                </span>
              </OutlineBtn>
              <OutlineBtn icon={Download} onClick={() => toast.success("CSV 파일이 다운로드되었습니다.")}>
                내보내기
              </OutlineBtn>
            </div>
          }
        />
        {error ? (
          <p role="alert" className="p-5 text-sm">{error}</p>
        ) : loading ? (
          <p role="status" className="p-5 text-sm" style={{ color: t.muted }}>디바이스를 불러오는 중입니다.</p>
        ) : filtered.length === 0 ? (
          <p className="p-5 text-sm" style={{ color: t.muted }}>등록된 디바이스가 없습니다.</p>
        ) : (
          <DeviceTable devices={filtered} />
        )}
      </Card>
    </div>
  );
}
