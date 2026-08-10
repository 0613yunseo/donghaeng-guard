import { MoreHorizontal } from "lucide-react";
import { useApp } from "../../contexts/AppContext";
import { C } from "../../constants/colors";
import { DevBadge } from "../badges/RiskBadge";
import type { Device } from "../../types/admin";

interface Props {
  devices: Device[];
}

export function DeviceTable({ devices }: Props) {
  const { t } = useApp();

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs">
        <thead>
          <tr style={{ borderBottom: `1px solid ${t.border}`, background: t.bgSub }}>
            {["디바이스 ID","사용자 ID","디바이스명","센서 종류","상태","마지막 연결","액션"].map(h => (
              <th key={h} className="px-4 py-3 text-left font-medium" style={{ color: t.muted }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {devices.map(d => (
            <tr key={d.id}
              className="transition-colors"
              style={{ borderBottom: `1px solid ${t.border}`, background: d.st === "ERROR" ? `${C.danger}07` : "transparent" }}
              onMouseEnter={e => (e.currentTarget.style.background = d.st === "ERROR" ? `${C.danger}12` : t.bgSub)}
              onMouseLeave={e => (e.currentTarget.style.background = d.st === "ERROR" ? `${C.danger}07` : "transparent")}>
              <td className="px-4 py-3 font-mono font-semibold" style={{ color: C.blue }}>{d.id}</td>
              <td className="px-4 py-3" style={{ color: t.muted }}>{d.user}</td>
              <td className="px-4 py-3 font-semibold" style={{ color: t.text }}>{d.name}</td>
              <td className="px-4 py-3">
                <div className="flex flex-wrap gap-1">
                  {d.sensors.map(s => (
                    <span key={s} className="px-2 py-0.5 rounded text-[10px] font-semibold"
                      style={{ background: `${C.blue}14`, color: C.blue }}>{s}</span>
                  ))}
                </div>
              </td>
              <td className="px-4 py-3"><DevBadge s={d.st} /></td>
              <td className="px-4 py-3 whitespace-nowrap" style={{ color: t.muted }}>{d.last}</td>
              <td className="px-4 py-3">
                <button className="p-1.5 rounded-lg hover:opacity-70 transition-opacity" style={{ color: t.muted }}>
                  <MoreHorizontal size={15} />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
