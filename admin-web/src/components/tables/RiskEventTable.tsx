import { useApp } from "../../contexts/AppContext";
import { C } from "../../constants/colors";
import { RiskBadge } from "../badges/RiskBadge";
import type { RiskEvent } from "../../types/admin";

interface Props {
  events: RiskEvent[];
  onRowClick: (id: string) => void;
}

export function RiskEventTable({ events, onRowClick }: Props) {
  const { t } = useApp();

  if (events.length === 0) {
    return (
      <div className="py-16 text-center text-sm" style={{ color: t.muted }}>
        조건에 맞는 이벤트가 없습니다.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs">
        <thead>
          <tr style={{ borderBottom: `1px solid ${t.border}`, background: t.bgSub }}>
            {["이벤트 ID","감지 시각","위험단계","센서","위험유형","거리(mm)","위도","경도","디바이스","주행 ID"].map(h => (
              <th key={h} className="px-4 py-3 text-left font-medium whitespace-nowrap" style={{ color: t.muted }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {events.map(e => (
            <tr key={e.id}
              onClick={() => onRowClick(e.id)}
              className="cursor-pointer transition-colors"
              style={{ borderBottom: `1px solid ${t.border}`, background: e.rl === "DANGER" ? `${C.danger}07` : "transparent" }}
              onMouseEnter={ev => (ev.currentTarget.style.background = e.rl === "DANGER" ? `${C.danger}12` : t.bgSub)}
              onMouseLeave={ev => (ev.currentTarget.style.background = e.rl === "DANGER" ? `${C.danger}07` : "transparent")}>
              <td className="px-4 py-3 font-mono font-semibold" style={{ color: C.blue }}>{e.id}</td>
              <td className="px-4 py-3 whitespace-nowrap" style={{ color: t.muted }}>{e.at}</td>
              <td className="px-4 py-3"><RiskBadge level={e.rl} /></td>
              <td className="px-4 py-3 font-mono" style={{ color: t.textSub }}>{e.sensor}</td>
              <td className="px-4 py-3" style={{ color: t.muted }}>{e.rt}</td>
              <td className="px-4 py-3 font-semibold" style={{ color: t.text }}>{e.mm}</td>
              <td className="px-4 py-3 font-mono" style={{ color: t.muted }}>{e.lat.toFixed(4)}</td>
              <td className="px-4 py-3 font-mono" style={{ color: t.muted }}>{e.lng.toFixed(4)}</td>
              <td className="px-4 py-3 font-mono text-[11px]" style={{ color: t.muted }}>{e.dev}</td>
              <td className="px-4 py-3" style={{ color: t.muted }}>{e.trip}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
